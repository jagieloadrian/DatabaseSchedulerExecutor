# Stage 6 — Relocate shell/install scripts + bats test coverage

## Status
done — implemented, analyzed, converged

## Co
- Przenieść `runScheduler.sh`, `killScheduler.sh`, `install-service.sh`,
  `install-service.ps1` z root repo do `src/test/resources/` (dosłownie,
  potwierdzone w clarify)
- Zaktualizować README (ścieżki/instrukcje kopiowania)
- Dodać bats testy formalizujące manualną weryfikację z tej sesji
  (duplicate-start guard, stale pidfile, missing jar/config guard, glob
  resolution)

## Kontekst odkryty przy specify
Sprawdzone: te skrypty **nie są dziś częścią budowanej dystrybucji**
(`build/distributions/*.zip` z `application` plugin nie ma customowego
`distributions { }` blocku w `build.gradle.kts`, więc `distZip` nie
pakuje root-level plików jak `runScheduler.sh`). README Download/Usage
sugeruje że powinny tam być, ale w praktyce to były zawsze pliki do
ręcznego skopiowania z repo, nie zweryfikowany automatyczny release
artifact. Przeniesienie do `src/test/resources/` nie psuje więc realnego
packagingu (bo tego packagingu i tak nie było) — tylko wymaga update
instrukcji "skąd skopiować" w README.

## Clarifications (3 rundy pytań, odpowiedzi)
1. Nie usuwać starych skryptów — zostają obok `install-service.*` jako
   opcja bez roota/Administratora (dev/quick test).
2. Docelowa lokalizacja: **`src/test/resources/` dosłownie** (obok
   istniejących fixture'ów jak `app.properties`, `testDb.db`), nie nowy
   `scripts/` folder — potwierdzone wprost mimo zaznaczonego ryzyka
   (README ścieżki do update'u).
3. Testy dla skryptów: **bats** (Bash Automated Testing System), nie
   plain-assert script.

## Projekt
- `git mv` czterech plików do `src/test/resources/`
- Testy bats w `src/test/scripts/` (osobno od fixture'ów, mirror
  `src/test/kotlin` jako "kod testowy" vs `src/test/resources` jako dane) —
  `.ps1` bez testu bats (PowerShell), pokryty już przez CI Windows job
  ze Stage 5
- README: wszystkie miejsca odwołujące się do ścieżki skryptów
  zaktualizowane na `src/test/resources/<script>`
- CI: krok instalujący `bats` (apt, standardowy pakiet Ubuntu) + uruchamiający
  testy w istniejącym `build` jobie (`.github/workflows/build.yaml`)
- Lokalnie: `bats` niedostępny jako pakiet (brak passwordless sudo) —
  odpalę przez `git clone` bats-core do scratchpada (nie wymaga roota),
  realnie uruchomię testy przed commitem

## Zakres
- 4 pliki skryptów (przeniesienie)
- README.MD (aktualizacja ścieżek)
- Nowe pliki bats: `src/test/scripts/run_kill.bats`,
  `src/test/scripts/install_service.bats`
- `.github/workflows/build.yaml` (krok instalacji + odpalenia bats)

## Poza zakresem
- Testy dla `install-service.ps1` w bats (niemożliwe, PowerShell) —
  pokryte przez istniejący `test-windows-install` job ze Stage 5
- Zmiana faktycznego packagingu dystrybucji (`distZip` customization) —
  osobny temat, nie proszony teraz

## Acceptance criteria
- [x] 4 skrypty fizycznie w `src/test/resources/`, root repo bez nich
- [x] README nie ma martwych ścieżek do starych lokalizacji
- [x] bats testy pokrywają: duplicate-start guard, stale pidfile cleanup,
      kill izolowany do instancji, missing jar/config guard w
      install-service.sh
- [x] Testy realnie odpalone lokalnie (nie tylko napisane) i zielone
- [x] CI ma krok bats (nawet jeśli nie zweryfikowany live na GH Actions —
      ten sam limit co Stage 5 Windows job, branch nie wypchnięty)

## Converge
Zgodne z clarifications (3 rundy), brak niezamierzonych odchyleń.

Zaimplementowane:
- `git mv` 4 skryptów do `src/test/resources/`
- README: Setup krok 3, "Install as a service" zaktualizowane —
  dopisana instrukcja skąd skopiować skrypty (`src/test/resources/`)
- `src/test/scripts/run_kill.bats` — 5 testów: glob resolution + pidfile
  write, duplicate-start guard, kill izolowany + pidfile cleanup, stale
  pidfile graceful cleanup, no-pidfile no-op. `java` podmieniony na fake
  (sleep) w testach — szybkie/deterministyczne, bez realnego JVM/DB (real
  E2E z prawdziwym jarem już zrobiony manualnie w Stage 2/3)
- `src/test/scripts/install_service.bats` — 3 testy: missing jar, missing
  config, brak roota
- `.github/workflows/build.yaml`: `build` job dostał krok `apt-get install
  bats` + `bats src/test/scripts/*.bats`

Odchylenie znalezione i naprawione przy okazji: `test-windows-install` job
(Stage 5) odwoływał się do `install-service.ps1` w root repo — po
przenosinach ta ścieżka już nie istnieje, poprawione na
`src\test\resources\install-service.ps1` w kroku "Stage install folder".
Złapane przez przegląd workflow po zmianie, nie przez uruchomienie (Windows
job i tak nie jest jeszcze zweryfikowany live, patrz Stage 5 Converge).

Weryfikacja:
- `bats-core` sklonowany do scratchpada (bez roota, `git clone`, `apt`
  wymagałby hasła) — **realnie odpalone lokalnie**: `bin/bats
  src/test/scripts/*.bats` → 8/8 `ok`
- YAML workflow zweryfikowany syntaktycznie (`python3 -c "import yaml..."` OK)
- `git status` czysty po każdym kroku, żadnych przypadkowych plików w commicie

Nie zweryfikowane live: krok CI `bats` na GitHub Actions (branch nie
wypchnięty, ten sam limit co reszta CI w tej sesji) — jeśli
`ubuntu-latest` runner ma inny stan `apt` cache niż oczekiwany, `apt-get
update` powinien to pokryć, ale to ostatecznie potwierdzi dopiero pierwszy
PR/push.

## Korekta (po fakcie)
Clarify #2 wyżej ("`src/test/resources/` dosłownie") okazał się
nieporozumieniem — użytkownik doprecyzował, że chodziło o osobny folder
`scripts/` (dokładnie ta opcja, którą sam rekomendowałem w pierwszej
rundzie pytań, zanim user wybrał inaczej). Finalna, aktualna lokalizacja:

- `scripts/runScheduler.sh`, `scripts/killScheduler.sh`,
  `scripts/install-service.sh`, `scripts/install-service.ps1`
- `scripts/test/run_kill.bats`, `scripts/test/install_service.bats`
  (przeniesione razem ze skryptami, osobno od `src/test/` Kotlina)

Wszystkie powyższe wzmianki `src/test/resources/<script>` /
`src/test/scripts/<test>.bats` w tym dokumencie opisują stan przejściowy,
nie finalny. Zaktualizowane przy korekcie:
- README (dwie linie ze ścieżką: Setup krok 3, "Install as a service")
- `scripts/test/*.bats` — `SCRIPT_SRC_DIR` przeliczony na nową względną ścieżkę
- `.github/workflows/build.yaml` — krok `bats` + Windows job (kopiowanie
  `install-service.ps1`)

Zweryfikowane ponownie po korekcie: `bats scripts/test/*.bats` → 8/8
`ok` (bats-core ponownie sklonowany do scratchpada), `python3 -c "import
yaml..."` na workflow → OK, `grep` na README dla
`runScheduler|killScheduler|install-service` → wszystkie 11 wystąpień
sprawdzone, tylko te dwie miały ścieżkę, obie poprawne po korekcie.
