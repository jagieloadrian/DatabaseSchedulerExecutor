# Stage 4 — Feature: multi-DB support

## Status
done — implemented, analyzed, converged

## Co
ROADMAP: rozszerzyć poza SQLite → Oracle, MySQL, Postgres.

## Obecny stan (przeczytany kod)
- `DriverManagerConnectionProvider(url)` hardcode'uje `jdbc:sqlite:$url`
  (`DatabaseConnectionProvider.kt:13`)
- `PropertiesConfig` czyta `filepath` (ścieżka do pliku SQLite), `statement`, `cron`
- `Validator.validatePath` wymaga, żeby `filepath` wskazywał na istniejący
  plik z rozszerzeniem `.db`/`.properties` — założenie specyficzne dla SQLite
  (plik lokalny), nie pasuje do MySQL/Postgres/Oracle (host:port/dbname)
- `DbExecutor`/`runSqlStatement` już są DB-agnostyczne — używają zwykłego
  JDBC `Connection`/`Statement`, żadnej zmiany nie potrzebują

## Po co
Room for Improvement z ROADMAP, JDBC abstrakcja już częściowo gotowa
(DbExecutor), brakuje tylko: dobry connection provider + config + walidacja
dla nie-plikowych baz.

## Clarifications (odpowiedzi)
1. **Config**: osobne properties (`dbtype`, `host`, `port`, `database`), nie
   pojedynczy `dburl`. URL budowany w kodzie per vendor.
2. **Zakres baz**: MySQL, SQLite, PostgreSQL, Oracle + inne popularne. Konkretna
   lista przyjęta do implementacji: **SQLite (istniejące), PostgreSQL, MySQL,
   MariaDB, Oracle, SQL Server (MSSQL)** — sześć najpopularniejszych silników,
   pokrywa "MySQL/SQLite/Postgres/Oracle plus inne popularne". Zaprojektowane
   rozszerzalnie (jeden wpis w mapie + jedna zależność Gradle = kolejna baza),
   więc dodanie np. DB2 później jest tanie.
3. **Credentials**: obie opcje wspierane, użytkownik wybiera. Priorytet:
   zmienne środowiskowe `DB_USER`/`DB_PASSWORD` jeśli ustawione, inaczej
   `dbuser`/`dbpassword` z `app.properties` (plaintext, jak reszta configu).
   Jedna linijka fallbacku per getter (`System.getenv(...) ?: properties[...]`),
   zero nowej zależności. Opisane w README jako dwie opcje z przykładami.

## Projekt (po decyzjach)
Nowe properties w `app.properties` (opcjonalne, tryb sieciowy):
```properties
dbtype=postgresql
host=localhost
port=5432
database=mydb
dbuser=postgres
dbpassword=secret
```
- `dbtype` brak lub `sqlite` + obecny `filepath` → stary tryb bez zmian
  (`jdbc:sqlite:$filepath`), zero regresji dla istniejących configów.
- `dbtype` z listy sieciowej → `PropertiesConfig` czyta `host`/`port`/`database`/
  `dbuser`/`dbpassword`, `port` opcjonalny (domyślny per vendor jeśli brak).
- `dbuser`/`dbpassword`: `PropertiesConfig` najpierw sprawdza zmienne
  środowiskowe `DB_USER`/`DB_PASSWORD`, dopiero potem properties z pliku
  (`System.getenv("DB_USER") ?: properties["dbuser"]`) — obie opcje działają,
  użytkownik wybiera, env ma priorytet gdy ustawione.
- URL builder: mapa `DbType -> (host, port, database) -> jdbcUrl`, np.
  - postgresql: `jdbc:postgresql://host:port/database`
  - mysql: `jdbc:mysql://host:port/database`
  - mariadb: `jdbc:mariadb://host:port/database`
  - mssql: `jdbc:sqlserver://host:port;databaseName=database`
  - oracle: `jdbc:oracle:thin:@host:port:database` (SID-style, najprostszy format)
- `DriverManagerConnectionProvider` przyjmuje URL + opcjonalne user/password,
  woła `DriverManager.getConnection(url, user, password)` gdy podane, inaczej
  `DriverManager.getConnection(url)` (SQLite, bez credentiali).
- `Validator`: plik-na-dysku check tylko w trybie SQLite; w trybie sieciowym
  walidacja że `host`/`database` niepuste, `dbtype` rozpoznany.
- `build.gradle.kts`: dopisać drivery — `org.postgresql:postgresql`,
  `com.mysql:mysql-connector-j`, `org.mariadb.jdbc:mariadb-java-client`,
  `com.microsoft.sqlserver:mssql-jdbc`, `com.oracle.database.jdbc:ojdbc11`.

## Zakres
- `DatabaseConnectionProvider.kt`, `PropertiesConfig.kt`, `Validator.kt`,
  `Main.kt` (wiring), `build.gradle.kts` (5 nowych driverów)
- Testy jednostkowe: URL builder per vendor, validator w nowym trybie
- E2E: Docker dostępny w środowisku (`docker info` OK) — realne kontenery
  dla przynajmniej Postgres i MySQL (lekkie, szybki pull). Oracle/MSSQL
  obrazy są ciężkie/wolne do ściągnięcia — E2E dla nich best-effort, jeśli
  pull się nie uda w rozsądnym czasie, zostaje tylko unit test URL-buildera
  + walidacji (bez odpalenia realnego kontenera), zaznaczone w Converge.
- README: nowa sekcja o `dbtype`/`host`/`port`/`database`/`dbuser`/`dbpassword`

## Poza zakresem
- Connection pooling (HikariCP itp.) — YAGNI, jedna długo działająca
  aplikacja z jednym połączeniem na wykonanie, nie serwer obsługujący ruch
- GUI/CLI do zarządzania wieloma bazami naraz w jednym configu
- SSL/TLS connection options, connection string tuning params — jeśli
  potrzebne, użytkownik dopisze je ręcznie do przyszłego rozszerzenia URL-a

## Acceptance criteria
- [x] Istniejący `app.properties` z `filepath` (SQLite) działa bez zmian (regresja = 0)
- [x] `dbtype=postgresql` + realny kontener Docker: connect + SQL execute działa
- [x] `dbtype=mysql` + realny kontener Docker: connect + SQL execute działa
- [x] `dbtype=mariadb`/`mssql`/`oracle`: pełne E2E przez realny kontener Docker (patrz Follow-up niżej — best-effort limit zamknięty)
- [x] Walidacja configu rozróżnia tryb SQLite vs sieciowy, sensowny błąd gdy brak wymaganych pól
- [x] README udokumentowane

## Converge
Zgodne ze specem i clarifications, brak odchyleń.

Zaimplementowane:
- `DbType` enum (SQLITE, POSTGRESQL, MYSQL, MARIADB, MSSQL, ORACLE) +
  `buildJdbcUrl` per vendor + `DriverManagerConnectionProvider.sqlite()`/`.network()`
- `PropertiesConfig`: nowe opcjonalne gettery `dbtype`/`host`/`port`/`database`,
  `dbuser`/`dbpassword` z priorytetem `DB_USER`/`DB_PASSWORD` env nad plikiem
- `Validator`: rozgałęzienie SQLite vs sieciowy tryb, nowy `validateNetworkDbConfig`
- `Main.kt`: `buildConnectionProvider` wybiera tryb na podstawie `dbtype`
- `build.gradle.kts`: 5 nowych driverów (aktualne wersje z Maven Central na
  dzień pisania: postgresql 42.7.7, mysql-connector-j 9.3.0, mariadb-java-client
  3.5.10, mssql-jdbc 13.6.0.jre11, ojdbc11 23.8.0.25.04) + testcontainers 1.21.4
  (pinowane na ostatniej stabilnej gałęzi 1.x, nie 2.0.x — zbyt świeży major
  release, mniejsze ryzyko niekompatybilności)

Weryfikacja:
- `./gradlew clean test` — pełny zielony build, wszystkie testy (unit +
  E2E) przechodzą razem
- **Realne E2E przez Docker** (potwierdzone w logu testów): kontener
  `postgres:16-alpine` i `mysql:8.4` odpalone przez Testcontainers, connection
  zbudowany przez `DriverManagerConnectionProvider.network(DbType.X, ...)`
  (czyli dokładnie tą samą ścieżkę co produkcyjny `Main.kt`), realny
  `SELECT` przez `executeOperation` z `DbExecutor` zwrócił poprawny wynik
- SQLite: regresja zero — istniejący tryb `filepath` przechodzi przez
  dokładnie tę samą ścieżkę kodu co przed Stage 4 (`DriverManagerConnectionProvider.sqlite()`
  robi to samo co stary jednoargumentowy konstruktor)

## Follow-up: pełne E2E dla MariaDB/MSSQL/Oracle
Best-effort limit z Zakresu zamknięty na żądanie użytkownika — dodane
kontenery `org.testcontainers:mariadb`/`mssqlserver`/`oracle-free` (wszystkie
1.21.4, pasują do już przypiętej wersji). `NetworkDatabaseE2ETest.kt`
rozszerzony do 5 testów, **wszystkie 5 baz z README/tej specyfikacji
realnie przetestowane E2E przez Docker**, nie tylko unit testem URL-buildera.

Dwa prawdziwe bugi w kodzie produkcyjnym znalezione przez to testowanie
(nie tylko problemy testu — realne, odtwarzalne na żywych kontenerach):

1. **Oracle URL był zły format.** `buildJdbcUrl` dla `ORACLE` używał
   starego stylu SID (`jdbc:oracle:thin:@host:port:SID`). Realny kontener
   (i większość dzisiejszych wdrożeń Oracle, łącznie z Oracle Free/XE PDB)
   używa service name, nie SID → `ORA-12505: SID ... is not registered
   with the listener`. Naprawione na slash-style
   (`jdbc:oracle:thin:@//host:port/serviceName`) —
   `DatabaseConnectionProvider.kt:33`. README dopisane: `database` dla
   `oracle` to service name.
2. **MSSQL default łamał się na self-signed cert.** Nowszy
   `mssql-jdbc` domyślnie wymaga `encrypt=true` z walidacją CA. Kontener
   testowy (i typowy on-prem SQL Server bez własnego CA) ma self-signed
   cert → `SSLHandshakeException`. Zapytałem użytkownika wprost (decyzja
   security-relevant, nie moja do cichego podjęcia) — wybrał dodanie
   `trustServerCertificate=true` na stałe do domyślnego URL-a (kanał
   nadal szyfrowany, bez walidacji CA). `DatabaseConnectionProvider.kt:32`,
   README dopisane.

Test-side fix (nie produkcyjny bug): `MSSQLServerContainer.getDatabaseName()`
rzuca `UnsupportedOperationException` w testcontainers — test używa
`"master"` (domyślna baza kontenera) zamiast tego gettera.

Zweryfikowane: `./gradlew clean test` — pełny zielony build, `bash`
potwierdzenie `tests="5" skipped="0" failures="0" errors="0"` dla
`NetworkDatabaseE2ETest` przed i po fixach (pierwszy przebieg złapał oba
bugi realnie, nie hipotetycznie).
