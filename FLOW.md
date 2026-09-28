# Flow pracy (per stage z ROADMAP.md)

Każdy stage z `ROADMAP.md` przechodzi przez 5 kroków:

## 1. Specify
Krótki spec dla stage'a w `.specs/stage-N-<nazwa>.md`:
- co ma powstać, po co (why)
- zakres / poza zakresem
- acceptance criteria

## 2. Clarify
Pytania o niejasne punkty specu (przez AskUserQuestion), odpowiedzi dopisane
do tego samego pliku specu — nie zostają tylko w rozmowie.

## 3. Implement
Kod wg specu. Testy dla logiki nietrywialnej. Atomic commit per jednostka pracy
(wymóg z CLAUDE.md — jeden commit, conventional message).

## 4. Analyze
Weryfikacja przed commitem:
- testy dotyczące zmiany zielone
- acceptance criteria ze specu spełnione
- lint / CI gates przechodzą
Braki → fix i re-run przed przejściem dalej.

## 5. Converge
Porównanie wyniku z ideą + clarifications ze specu. Rozjazdy zaznaczone w specu
(status: done / deviated + powód). Odznaczenie stage'a w `ROADMAP.md`,
aktualizacja `PROJECT-STATUS.md`.

---

## Uwaga: kompaktowanie kontekstu

CLAUDE.md ma ustawione: kontekst orchestratora poniżej 40%, auto-summarize
powyżej 35%. Oznacza to, że historia tej rozmowy może zniknąć w trakcie pracy
nad stage'em — kompaktowanie zostawia streszczenie, nie pełny zapis decyzji.

Konsekwencja dla flow: kroki 1–2 (specify, clarify) MUSZĄ trafić do pliku
specu, zanim przejdziemy do implementacji. Jeśli decyzja/odpowiedź istnieje
tylko w konwersacji, kompaktowanie może ją zgubić lub spłaszczyć. Plik specu
jest źródłem prawdy, nie wątek rozmowy.

Praktycznie: po każdym z kroków 1, 2 i 5 — zapis do pliku, nie poleganie na
pamięci sesji. Wznowienie pracy po kompaktowaniu = czytamy `ROADMAP.md` +
właściwy plik w `.specs/`, nie próbujemy odtwarzać z historii czatu.
