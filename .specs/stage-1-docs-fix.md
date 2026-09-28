# Stage 1 — Docs fix

## Status
done — implemented, analyzed, converged

## Co
Poprawki w `README.MD`, bez zmian w kodzie:
1. Badge CI (workflow `.github/workflows/build.yaml` już istnieje)
2. Link/wzmianka o licencji + plik `LICENSE` w repo
3. Fix broken linku: `[cron-utils docs]()` w sekcji Features (pusty href)
4. Fix sekcji Contact: obecnie link do GitLab, sprawdzić który remote kanoniczny (patrz Open questions)
5. Collapse: "install Java 17" powtórzone 3x w Setup → jedna sekcja Prerequisites
6. Przykładowy snippet `app.properties` inline w README
7. Krótka sekcja o logowaniu (slf4j-simple — gdzie/jak logi)

## Po co
README ma braki wykryte w analizie projektu (broken link, brak licencji, brak
widoczności istniejącego CI, powtórzenia w Setup utrudniające onboarding).

## Zakres
Tylko `README.MD` + nowy plik `LICENSE`. Brak zmian w kodzie/testach/CI configu.

## Poza zakresem
- Zmiana samego workflow CI (Stage 1 tylko dodaje badge, nie modyfikuje `build.yaml`)
- Naprawa driftu wersji jara w Setup (`-1.0.jar` vs realny `-1.0.1.jar`) — to Stage 2
- Diagram architektury / rozbudowa General Info — nie było w oryginalnym findings jako Stage 1 item

## Clarifications (odpowiedzi)
1. **Remote/repo kanoniczny**: GitHub (`github_new`) —
   `github.com/jagieloadrian/DtatabaseSchedulerExecutor` (literówka w nazwie repo,
   zostaje jak jest, to nie jest w zakresie Stage 1). Badge CI + link Contact → GitHub.
2. **Licencja**: MIT.
3. **Logging**: opisać istniejące przekierowanie z `runScheduler.sh`
   (`> output.log 2>&1`) jako część flow uruchomienia, nie tylko domyślne
   zachowanie slf4j-simple.

## Acceptance criteria
- [x] README zawiera badge CI wskazujący na działający workflow
- [x] `LICENSE` istnieje w repo, README linkuje do niego
- [x] Link do cron-utils docs działa (niepusty href)
- [x] Sekcja Contact wskazuje poprawny/potwierdzony remote (GitHub)
- [x] Setup ma jedną sekcję Prerequisites zamiast 3x powtórzonego Java 17
- [x] README ma przykładowy `app.properties`
- [x] README ma sekcję Logging opisującą gdzie/jak trafiają logi

## Converge
Zgodne ze specem i clarifications, brak odchyleń. Jedna uwaga poza zakresem:
Download link (linia 85) wskazuje na inny wariant nazwy repo
(`DatabaseSchedulerExecutor`) niż CI badge (`DtatabaseSchedulerExecutor`,
literówka w realnej nazwie repo na GitHub) — nie ruszane, poza zakresem Stage 1.
