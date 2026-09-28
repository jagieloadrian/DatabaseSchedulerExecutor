# Stage 2 — Correctness/consistency (jar version drift)

## Status
done — implemented, analyzed, converged

## Co
Jar version hardcoded `-1.0.jar` w trzech miejscach, realna zbudowana wersja to
`-1.0.1.jar` (`gradle.properties` → `version=1.0.1`, CI (`build.yaml`) taguje
release właśnie tą wartością). Rozjazd:
- `README.MD:55` — `java -jar DatabaseScheduleExecutor-1.0.jar ...`
- `runScheduler.sh` — `java -jar DatabaseScheduleExecutor-1.0.jar ...`
- `killScheduler.sh` — `pgrep -f DatabaseScheduleExecutor-1.0.jar`

## Po co
Root cause, nie tylko symptom w README: wersja zduplikowana w 3 miejscach,
jedno źródło prawdy to `gradle.properties`. Kopiowanie jara zgodnie z Setup
(krok 3) z literalną nazwą `-1.0.jar` faktycznie się wysypie — pliku o takiej
nazwie nie będzie w `build/libs`.

## Zakres (root-cause fix)
- `runScheduler.sh` / `killScheduler.sh`: zamiast hardcode wersji, znaleźć jar
  dynamicznie (glob `DatabaseScheduleExecutor-*.jar` w katalogu skryptu) —
  jedna zmiana, działa dla każdej przyszłej wersji bez ręcznej edycji.
- `README.MD`: komenda `java -jar ...` w Setup zaktualizowana do wzorca
  zgodnego ze skryptami (bez konkretnej wersji / z komentarzem "nazwa pliku
  zależy od wersji w `build/libs`").

## Poza zakresem
- Zmiana CI (`build.yaml`) — już poprawnie ciągnie wersję z `gradle.properties`
- Stage 5 (systemd/scheduled task installer) — osobny stage, tam też będzie
  potrzebny ten sam glob, ale to inny plik/inna praca

## Clarifications (odpowiedzi)
1. Scope poszerzony: fix `runScheduler.sh` + `killScheduler.sh` (root cause),
   nie tylko README.
2. `killScheduler.sh` robi własny glob na `DatabaseScheduleExecutor-*.jar` w
   katalogu skryptu (ten sam wzorzec co `runScheduler.sh`) i `pgrep -f`
   dopasowuje dokładną nazwę znalezionego pliku, nie goły prefix.

## Acceptance criteria
- [x] `runScheduler.sh` uruchamia poprawny jar niezależnie od wersji w nazwie pliku
      (shell glob `DatabaseScheduleExecutor-*.jar` w komendzie `java -jar`)
- [x] `killScheduler.sh` nadal zabija właściwy proces po zmianie
      (własny `ls`-glob → dokładna nazwa pliku → `pgrep -f "$jar"`)
- [x] README Setup krok 2 nie ma już literalnie błędnej wersji `-1.0.jar`
- [x] Zweryfikowano w scratchpadzie: katalog z jarem `-1.0.1.jar`, glob
      poprawnie rozwiązuje się do pełnej nazwy pliku; `bash -n` syntax OK dla
      obu skryptów

## Converge
Zgodne ze specem i clarifications, brak odchyleń. Pełny end-to-end test
(realne uruchomienie `sh runScheduler.sh` + `sh killScheduler.sh` na żywym
javie) nie wykonany — brak działającego configu/DB w tym środowisku;
zweryfikowano logikę (glob resolution + syntax), nie live process kill.
