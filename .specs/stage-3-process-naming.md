# Stage 3 — Feature: process naming/PID labeling

## Status
done — implemented, analyzed, converged

## Co
ROADMAP: "Add specific name for process ID" — obecnie `killScheduler.sh`
(po Stage 2) robi `pgrep -f "$jar"` po nazwie jara. Jeśli dwie instancje
(dwa różne configi) odpalone z dwóch różnych katalogów, ale jar ma tę samą
nazwę w obu — `pgrep -f` złapie **oba** PID-y i `kill $pid` zabije obie
instancje naraz, nie tylko tę, o którą chodziło. To już jest zaznaczone w
README (Stop program): "Attention, when you run more than one program then
kill all processes by name of app" — czyli znana wada, nie tylko teoria.

## Po co
Root cause: identyfikacja procesu po nazwie jara w `ps`/`pgrep` nie
odróżnia instancji. Standardowe rozwiązanie Unix: PID file per instancja,
nie grep po nazwie procesu.

## Proponowane podejście (root-cause fix)
- `runScheduler.sh` po starcie zapisuje `$!` (PID właśnie odpalonego
  procesu w tle) do pliku `scheduler.pid` w katalogu skryptu.
- `killScheduler.sh` czyta PID z `scheduler.pid` w swoim katalogu i zabija
  dokładnie ten PID (nie grep po nazwie jara) — usuwa plik po zabiciu.
- Multi-instancja rozwiązana przez fakt, że każda instancja żyje w swoim
  katalogu (już obecny wzorzec z Setup: kopiujesz jar+properties do
  osobnego folderu) → osobny `scheduler.pid` per katalog, zero kolizji.
- Odrzucone: nadawanie javie custom nazwy procesu widocznej w `ps` —
  niestandardowe, zależne od JVM/OS, więcej kodu niż plik z PID-em.

## Zakres
- `runScheduler.sh`, `killScheduler.sh`
- README: krótka wzmianka o `scheduler.pid` w sekcji Stop program

## Poza zakresem
- Zmiana architektury na systemd/scheduled task (to już Stage 5, tam PID
  management robi sam OS/init system, ten problem znika inaczej)
- GUI/CLI do listowania działających instancji

## Clarifications (odpowiedzi)
1. `killScheduler.sh` sprawdza `kill -0 $pid` przed właściwym kill; jeśli
   PID nie żyje, usuwa plik bez błędu (stale pidfile guard).
2. `runScheduler.sh` sprawdza `scheduler.pid` przed startem; jeśli PID w
   nim wskazuje żywy proces — komunikat, exit bez startu duplikatu.

## Acceptance criteria
- [x] `runScheduler.sh` zapisuje PID do `scheduler.pid`
- [x] `killScheduler.sh` zabija dokładnie ten PID, nie inne instancje
- [x] Dwie instancje w dwóch katalogach: kill jednej nie rusza drugiej (E2E)
- [x] README Stop program zaktualizowany

## Converge
Zgodne ze specem i clarifications, brak odchyleń.

Pełny E2E test (realny jar `1.0.1`, dwa izolowane katalogi w scratchpadzie,
osobne kopie `testDb.db`):
- obie instancje wystartowane, różne PID-y w osobnych `scheduler.pid`
- `runScheduler.sh` drugi raz w katalogu A → odmowa startu duplikatu
  (`already running (pid ...)`, exit 1) — potwierdzone
- `killScheduler.sh` w katalogu A → zabity tylko PID A (potwierdzone przez
  `/proc/<pid>`), instancja B nadal żyje, jej `scheduler.pid` nietknięty
- symulacja crasha instancji B (`kill -9` z zewnątrz, pidfile zostaje) →
  `killScheduler.sh` wykrywa martwy PID, komunikat "not running (stale
  pidfile)", sprząta plik bez błędu

Wszystkie 4 scenariusze z acceptance criteria + oba clarify guardy
zweryfikowane na żywo, nie tylko statycznie.
