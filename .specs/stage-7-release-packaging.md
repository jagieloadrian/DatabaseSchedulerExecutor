# Stage 7 — Fix release packaging gap

## Status
done — implemented, analyzed, converged

## Co
README Download/Usage sekcja mówi: pobierz zip z GitHub Release, unzip,
`sh runScheduler.sh`. W praktyce zbudowany i publikowany zip
(`build/distributions/*.zip`, dołączany przez `release` job w CI) **nie
zawiera** `scripts/` ani przykładowego `app.properties` — użytkownik
pobierający realny release nie ma czym odpalić.

## Zweryfikowany aktualny stan (`./gradlew distZip` + `unzip -l`)
Zip zawiera:
- `lib/` — fat jar (`DatabaseScheduleExecutor-1.0.1.jar`, ma wszystkie
  zależności shade'owane w środku, ~34MB) **PLUS** każdą zależność jeszcze
  raz jako osobny jar (postgresql, mysql-connector, mariadb, mssql-jdbc,
  ojdbc11, itd.) — bo `application`/`distributions` plugin nie wie nic o
  customowym fat-jar mergingu w `tasks.withType<Jar>`, po prostu bundluje
  cały `runtimeClasspath` osobno. Efekt: ~69MB zip, połowa zawartości
  zduplikowana (to samo jest już w środku fat jara).
- `bin/DatabaseScheduleExecutor` (+`.bat`) — wygenerowany przez
  `application` plugin standardowy launcher (Apache-2.0 Gradle
  boilerplate), odpala przez classpath z `lib/`, **nigdy nieużywany i
  niewspomniany nigdzie w README** — cały udokumentowany workflow
  (Setup/Usage) zakłada ręczne `java -jar fatjar.jar --config x` albo
  `sh runScheduler.sh`, nie ten launcher.

Brakuje: `scripts/*.sh`/`*.ps1`, przykładowy `app.properties`.

## Po co
Realna niespójność dokumentacja↔artifact — Download/Usage w README
opisuje flow który dziś nie zadziała z faktycznym release zipem.

## Dodatkowo odkryte: `release` job nigdy nic nie builduje
`.github/workflows/build.yaml` `release` job: checkout → setup JDK → cache
→ "Get version" → `ncipollo/release-action` z `files: build/distributions/*`.
**Brak jakiegokolwiek kroku `./gradlew build`/`assemble`/`distZip`** w tym
jobie — świeży checkout na osobnym runnerze (`needs: build` to tylko
kolejność, nie współdzielony filesystem między jobami bez explicit
upload/download-artifact). `build/distributions/*` nigdy by tam nie
istniało → release zawsze publikowałby się bez załączonych plików (albo
`ncipollo/release-action` błądzi na pustym globie). To część tej samej
luki "packaging", nie osobny temat — fix wymaga też dodania kroku builda
w `release` jobie.

## Clarifications (odpowiedzi)
1. **Zakres fixu**: pełny (b) — usunąć `application` plugin całkowicie
   (jego jedyny efekt to nieużywany `bin/` launcher + zduplikowany `lib/`;
   `Main-Class` manifest już ustawiany niezależnie przez
   `tasks.withType<Jar>`, `./gradlew run` nigdzie nieudokumentowany/nieużywany).
   Zamiast tego: własny `Zip` task budujący dokładnie to czego oczekuje
   README (fat jar + `app.properties` + `scripts/*`, flat, bez `scripts/test/`).

## Projekt
- Usunąć `application` plugin z `plugins {}` i blok `application { mainClass.set(...) }`
- Nowy task `releaseZip` (`Zip` type): `from(tasks.jar)`,
  `from("src/main/resources/app.properties")`, `from("scripts") { exclude("test/**") }`,
  wszystko flat w root zipa (bez podfolderów) — dokładnie zgodne z Usage
  ("unzip", `sh runScheduler.sh` bez ścieżki)
- `archiveBaseName` = `rootProject.name`, `archiveVersion` = `version` →
  ta sama nazwa pliku co dotychczasowy `distZip` (`DatabaseScheduleExecutor-1.0.1.zip`),
  zero zmian po stronie README/Download linku
- `assemble`/`build` dostaje `dependsOn(releaseZip)` — normalny
  `gradle clean build` (Setup krok 2) dalej produkuje fat jar jak dziś,
  plus teraz przy okazji gotowy release zip
- CI `release` job: dodany krok `./gradlew releaseZip` przed
  "Create GitHub Release" (jedyny sposób żeby `build/distributions/*`
  faktycznie tam istniało)

## Zakres (wstępny, do potwierdzenia w clarify)
- `build.gradle.kts` — customowa dystrybucja
- CI (`release` job) — bez zmian jeśli nadal `build/distributions/*`
- README — bez zmian merytorycznych (obecny opis Usage już zgodny z
  intencją, tylko artifact go nie spełniał)

## Poza zakresem
- Zmiana samego mechanizmu release (np. przejście na GitHub Actions
  `actions/upload-artifact` zamiast `ncipollo/release-action`) — nie proszone

## Acceptance criteria
- [x] Release zip zawiera: fat jar, `app.properties`, 4 skrypty — flat, bez `scripts/test/`
- [x] Zip nie ma duplikacji zależności (sam fat jar, bez osobnego `lib/`)
- [x] Nieużywany `bin/` launcher zniknął
- [x] `./gradlew build`/`clean build` (Setup krok 2) nadal produkuje fat jar jak dotychczas
- [x] CI `release` job faktycznie builduje przed próbą załączenia plików
- [x] Pełna suita testów zielona po usunięciu `application` pluginu

## Converge
Zgodne z clarifications, brak odchyleń.

Zaimplementowane:
- `build.gradle.kts`: usunięty `application` plugin + blok, nowy task
  `releaseZip` (`Zip` type) — `from(tasks.jar)` + `app.properties` +
  `scripts/` (bez `test/**`), spięty z `assemble` przez `dependsOn`
- `.github/workflows/build.yaml`: `release` job dostał krok
  `./gradlew releaseZip` przed "Create GitHub Release" — bez tego
  `build/distributions/*` nigdy by tam nie istniało

Zweryfikowane realnie:
- `./gradlew clean releaseZip` → zip **32MB** (było ~69MB) — jeden fat
  jar, `app.properties`, 4 skrypty, flat, zero `scripts/test/`, zero
  `bin/`/duplikatów `lib/`. Sprawdzone `unzip -l`.
- `./gradlew clean test` → pełna suita zielona po usunięciu
  `application` pluginu, nic się nie posypało
- `./gradlew build` → `releaseZip` odpala się automatycznie przez
  `assemble`, dokładnie tak jak `build` job w CI go teraz wywoła
- `python3 -c "import yaml..."` na workflow → OK
- `grep` po repo za wzmiankami usuniętego pluginu (`application`,
  `gradlew run`, `distZip`, `distTar`) → tylko dwa niezwiązane
  wystąpienia słowa "application" w README (opis projektu, nie plugin)

Nie zweryfikowane live: sam `release` job na GitHub Actions (branch nie
wypchnięty, wymaga push na `main` żeby się odpalić — ten sam limit co
reszta CI w tej sesji).
