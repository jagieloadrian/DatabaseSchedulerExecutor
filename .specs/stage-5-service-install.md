# Stage 5 — Easier launch (systemd/scheduled task installers)

## Status
done — implemented, analyzed, converged

## Co
ROADMAP Stage 5, częściowo nieaktualny (mówi o driftowanej wersji jara i
`pgrep -f` — oba już naprawione w Stage 2/3). Aktualny stan
`runScheduler.sh`/`killScheduler.sh`: glob po jarze, `scheduler.pid` per
katalog, guard na duplikat startu, stale-pidfile cleanup. Działa, ale to
nadal `nohup` w tle — brak auto-restart po crashu, brak startu przy boot,
kill wymaga ręcznego wywołania skryptu.

Dodać:
- `install-service.sh` (Linux) — instaluje jako systemd unit
- `install-service.ps1` (Windows) — rejestruje scheduled task (ONSTART/SYSTEM)

## Po co
Room for Improvement / dyskusja wcześniej w tej sesji: `nohup`+`pgrep` nie
daje auto-restart, nie startuje przy boot, wymaga pamiętania o dwóch
osobnych skryptach do stop/start. systemd/scheduled task to standardowe
mechanizmy OS-owe robiące to samo bez pisania własnej logiki retry/boot.

## Projekt
- `install-service.sh`: znajduje jar (`DatabaseScheduleExecutor-*.jar`) w
  katalogu skryptu, kopiuje jar+`app.properties` do katalogu docelowego
  (domyślnie `/opt/db-scheduler`, nadpisywalne arg1), generuje
  `/etc/systemd/system/db-scheduler.service` (`ExecStart=java -jar ...`,
  `Restart=on-failure`, `WantedBy=multi-user.target`), `daemon-reload`,
  `enable --now`. Wymaga roota.
- `install-service.ps1`: to samo dla Windows, `schtasks /Create ... /SC
  ONSTART /RU SYSTEM /RL HIGHEST`. Wymaga Administratora.
- Systemd/scheduled task **nie korzystają** z `runScheduler.sh`/
  `killScheduler.sh`/`scheduler.pid` — mają własny PID-tracking (to inny,
  równoległy tryb wdrożenia, nie zamiennik dla dev/ręcznego użycia tych
  skryptów, oba tryby mogą współistnieć w README).
- Uninstall: jedna komenda w komentarzu na górze skryptu (jak wcześniej w
  tej sesji ustalone), bez osobnego pliku.

## Zakres
- `install-service.sh`, `install-service.ps1`
- README: sekcja o instalacji jako usługa (alternatywa dla
  `runScheduler.sh`), z uninstall-command w komentarzu

## Poza zakresem
- Dedykowany user/service account zamiast root/SYSTEM (ponytail note z
  ROADMAP — hardening na później)
- Automatyczne odpalenie w tym środowisku jako część weryfikacji — patrz
  Open questions, brak passwordless sudo w tej sesji

## Clarifications (odpowiedzi)
1. Weryfikacja statyczna: `bash -n`, treść wygenerowanego unit file,
   `systemd-analyze verify` na samym pliku bez `enable --now`. Bez realnej
   instalacji (brak passwordless sudo w środowisku).
2. Install dir zostaje: `/opt/db-scheduler` (Linux), `%ProgramFiles%\...`
   (Windows), nadpisywalne arg/param.

## Acceptance criteria
- [x] `install-service.sh`: syntax OK, poprawnie generuje unit file z
      właściwą ścieżką do jara i configu
- [x] `install-service.ps1`: poprawnie generuje komendę `schtasks`
- [x] Oba: znajdują jar przez glob, nie hardcode'ują wersji
- [x] README ma sekcję o instalacji jako usługa + uninstall command
- [x] Weryfikacja zgodna z odpowiedzią na Open question #1

## Converge
Zgodne ze specem i clarifications, jedno realne odchylenie znalezione i
naprawione w trakcie: pierwsza wersja `install-service.sh` używała
bashizmów (`${BASH_SOURCE[0]}`, `$EUID`). Projekt woła te skrypty przez
`sh scriptname.sh` (konwencja z `runScheduler.sh`), a `sh` na tym systemie
to dash — bashizmy wysypywały się z "Bad substitution" / "Illegal number".
Przepisane na POSIX sh (`$0`, `$(id -u)`), przetestowane pod `sh` (nie tylko
`bash -n`).

Weryfikacja wykonana (zgodnie z clarify — statyczna, brak passwordless
sudo w środowisku):
- `install-service.sh` pod `sh`: 3 scenariusze — brak jara (exit 1,
  komunikat), brak `app.properties` (exit 1, komunikat), oba obecne bez
  roota (exit 1, "run as root") — wszystkie przeszły po POSIX-fixie
- Treść generowanego unit file wyrenderowana z przykładowymi wartościami
  i zweryfikowana `systemd-analyze verify` — exit 0, zero ostrzeżeń
- `install-service.ps1`: brak `pwsh` w tym środowisku (Linux, bez
  Windows) — zweryfikowany tylko manualnym przeglądem (nawiasy/cudzysłowy
  zbalansowane, standardowe cmdlety, poprawny idiom sprawdzania admina).
  **Nie uruchomiony ani razu** — jeśli chcesz pewność, przetestuj na
  Windows albo powiedz, to poszukam innego sposobu weryfikacji.
