# EarthSMP Core 1.0.0 — rdzenie ochronne (etap testowy)

Aktualizacja 0.9.0 → 1.0.0. **Nie kasuj** Gradle Wrappera, bazy SQLite, Polski ani terytoriów.

## Co zostało dodane

- Migracja SQLite v3 → v4: osobna tabela `protection_cores`, identyfikatory UUID, przypisanie państwa, świata, pozycji i pierwszej stolicy.
- Pierwszy rdzeń ochronny dla państwa może **testowo ustawić administrator** komendą `/esmp core place`.
- Blok rdzenia to na razie `LODESTONE` (tymczasowy placeholder). Ustawiany nad wskazanym pełnym blokiem podłoża, nie pod stopami gracza.
- Centrum rdzenia musi być wewnątrz **zatwierdzonego terytorium państwa administratora**. Cała powierzchnia 65×65 musi mieścić się w worldborderze. Strefy się nie nakładają, ale mogą sąsiadować.
- Obszar chroniony: X/Z ±32 od rdzenia, cała wysokość świata. Członkowie państwa mogą tam budować, inni nie, admin ma bypass poza samym blokiem rdzenia.
- Blok rdzenia nie może zostać zwyczajnie wykopany; `/esmp core remove` usuwa go administracyjnie.
- Ograniczenia obejmują standardowe break/place, prawy przycisk na blokach, wiadra, wybuchy, przepychanie pistonami, część zagrożeń środowiskowych oraz przesył z hopperów między regionami.
- Żadne dane rdzeni nie trafiają do warstwy squaremap.

**To nie jest jeszcze kompletna ochrona na produkcję.** Nietypowe obejścia (inne pluginy, encje, tryby redstone, interakcje z kontenerami/pojazdami) wymagają dalszych testów i zabezpieczeń. Piston dotykający chronionego regionu jest tymczasowo blokowany nawet właścicielowi. Ochrona PvP, wojny, oblężenia i przejmowanie rdzeni NIE działają w tej wersji.

## Polecenia

- `/esmp core here` — czy miejsce, w którym stoisz, jest chronione (dla każdego gracza).
- `/esmp core place` — admin: patrz na blok podłoża z odległości maksymalnie 6 kratek, nad nim musi być wolne miejsce. Państwo musi już posiadać zatwierdzony chunk.
- `/esmp core list` — admin: wyświetla pozycje wszystkich rdzeni (nigdy nie pokazujemy ich publicznie).
- `/esmp core remove` — admin: patrz bezpośrednio na blok rdzenia.

Tymczasowo można postawić **jeden darmowy rdzeń na państwo**. Produkcja dwóch dodatkowych, przedmioty PDC, wydawanie rdzenia w GUI podczas zakładania kraju, przenoszenie własnych oraz zdobywanie i limit pochodzenia wdrożymy później. Nie da się tej funkcji nadużyć zwykłym craftingiem — tylko admin może stawiać.

## Aktualizacja i test

1. `stop` serwera i skopiuj `plugins/EarthSMPCore/earthsmp.db` do bezpiecznego backupu.
2. Rozpakuj paczkę do głównego `EarthSMP`, **nadpisz tylko źródła** `EarthSMP-Core/`.
3. `cd EarthSMP-Core` i `./gradlew.bat clean build` w PowerShell: `.\gradlew.bat clean build`.
4. Usuń wcześniejszy JAR 0.9.0 z `plugins/`, wrzuć `build/libs/EarthSMP-Core-1.0.0.jar` (bez `-plain`).
5. Na terytorium Polski: `/esmp core place` patrząc na podłoże. Zobacz `LODESTONE`.
6. `/esmp core here` wewnątrz 65×65. Spróbuj wykopać sam blok rdzenia (powinno odmówić), zwykły blok powinien móc wykopać administrator/członek.
7. Zrestartuj Paper; `/esmp core list` i `/esmp core here` mają zachować dane.
8. `/esmp core remove` do ewentualnego posprzątania testu.

**Brak wymogu ponownego renderowania squaremap.** Pozycje rdzeni pozostają prywatne.

### Ważny detal

SQLite jest odtwarzane z pliku, a rdzeń jest też rzeczywistym blokiem w świecie. Kopię bazy i świata wykonuj z **tego samego momentu**, po zatrzymaniu serwera. Inaczej po przywróceniu można otrzymać osierocone rdzenie lub niespójny stan.

Pełny build na Paper 26.3 należy wykonać lokalnie. Kontrola syntaktyczna na atrapach API nie zastępuje kompilacji against Paper ani testu integracyjnego.


## 1.1.0 — rdzenie jako przedmioty

- `/esmp core free` — lider lub zastepca odbiera jeden darmowy rdzen na panstwo (jezeli nie byl juz wykorzystany).
- `/esmp core stock` — darmowe 0/1, stworzone 0/2 i ile zostalo postawionych.
- Stare rdzenie 1.0.0 sa przejmowane automatycznie przez migracje SQLite (schemat v5).
- Crafting **w stole rzemieslniczym**: 64 coal, iron, copper, redstone, lapis, gold, diamond, amethyst + 1 jukebox. Zwykly lewy klik w wynik pustym kursorem; bez Shift.
- Tokeny maja unikalne identyfikatory zapisane w SQLite, naleznosc do kraju i status `ITEM` lub `PLACED`. Nie da sie uzyskac dwoch baz z kopii jednego itemu.
- Postawienie przez lidera/zastepce na zatwierdzonym terenie, poza nakladajacymi sie obszarami i wewnatrz worldbordera.
- Tokeny `CRAFTED` licza sie do 2 nawet jesli leza w skrzynkach.
- **Ograniczenia obecnego etapu:** nie ma jeszcze przenoszenia rdzeni, wojny/zdobyczy, stref 9x9, auto-dostarczania darmowego przedmiotu w chwili zalozenia kraju ani pelnej ochrony wszystkich mozliwych obejsc; polecenie `/esmp core free` jest tymczasowym odbiorem startowego przedmiotu.
- Backup: kopiuj zarowno swiat `Earth`, jak i `plugins/EarthSMPCore/earthsmp.db` po zatrzymaniu serwera.


## 1.2.0 — przenoszenie własnych rdzeni i poprawka mapy

- `/esmp core pickup` — lider lub zastepca patrzy na blok swojego rdzenia (do 6 blokow). **Wolny slot w ekwipunku jest wymagany.** Rdzen znika ze swiata, wraca jako przedmiot **z tym samym UUID** i przestaje chronic dotychczasowa baze. Kolejne postawienie odbywa sie przez prawy przycisk myszy jak w 1.1.0. Token `ITEM` nie dodaje nowego rdzenia do limitu (przenoszenie = ten sam token).
- Stolicy nie mozna podniesc przez `/esmp core pickup`, dopoki admin nie wyznaczy innego **postawionego** rdzenia danego kraju komenda `/esmp core capital` (patrzac na niego). To wymusza spelnienie zasady, ze stolice zmienia administrator; dla kraju z tylko jedna baza przeniesienie jedynej stolicy wymaga osobnej interwencji admina w przyszlym narzedziu relokacji.
- Zdobyte rdzenie (`CONQUERED`) nie moga zostac podniesione. Sama mechanika wojny oraz flaga aktywnej wojny jeszcze NIE istnieja; po ich dodaniu `/pickup` bedzie dodatkowo blokowany, gdy kraj uczestniczy w wojnie.
- `/esmp core remove` pozostaje **administracyjnym, permanentnym usunieciem** z bazy i swiata — NIE sluzy do przenoszenia. Przenoszenie zawsze przez `pickup`.
- squaremap: prostokaty wypelnienia pozbawione wewnetrznych obwodek, a zewnetrzna granica kraju liczona z sasiadowania politycznych chunkow. Nie zmienia terytoriow w SQLite i nie pokazuje polozen rdzeni.
- Nie potrzeba nowej migracji SQLite (`user_version=5`). Rdzenie 1.0.0 i 1.1.0 pozostaja na miejscu; nie zmieniaj recznie danych bazy.
- **Konieczny spójny backup** swiata Earth i `plugins/EarthSMPCore/earthsmp.db` przy wylaczonym Paper. Przenoszenie dotyka jednoczesnie bloku i bazy danych — nie da sie ich polaczyc w jedna transakcje. Awaria serwera w trakcie komendy moze wymagac przywrocenia tych dwoch plikow razem.


## 1.3.0 — strefa przyszłego przejmowania 9×9×16 (bez oblężeń)

- Strefa X/Z `rdzeń ±4`, Y od `rdzeń −5` do `rdzeń +10` (włącznie), automatycznie wynika z aktualnego położenia rdzenia. Nie trzeba migracji SQLite (schemat v5).
- Zakaz stawiania wszelkich łóżek, obsydianu, crying obsidian, netherite block, reinforced deepslate, ancient debris oraz bloków administracyjnych/niezniszczalnych. Zakaz działa także na członków kraju i OP — uczciwe oblężenia muszą działać bez wyjątków. Poza strefą tych ograniczeń nie ma.
- Blokowanie drugiej połówki łóżka, jeśli pierwsza zostaje postawiona poza 9×9 (BlockMultiPlaceEvent).
- Blokowanie powstawania obsydianu z wody i lawy przez BlockFormEvent; wtyczka nie tylko blokuje zwykłe kliknięcie obsydianem.
- Nie można postawić NOWEGO rdzenia w przygotowanej wcześniej klatce z niedozwolonych bloków. Test przed zatwierdzeniem tokenu/zmianą SQLite skanuje 9×9×16. Istniejące rdzenie nie są przenoszone ani niszczone.
- `/esmp core zonecheck` — **tylko admin**: patrz na istniejący lodestone rdzenia do 6 kratek, otrzymasz liczbę nielegalnych bloków w 9×9×16 i współrzędne pierwszego. Jest to przydatne dla starszych rdzeni z wersji <= 1.2.0.
- Test: spróbuj postawić łóżko i obsydian w 4 blokach od istniejącego rdzenia na Y rdzenia (ma odmówić), a następnie w 5 blokach (ma się udać — jeśli nie blokuje inne zabezpieczenie). Zwykły kamień w 9×9 powinien być dozwolony. Sprawdź łóżko z jedną połówką na granicy strefy.
- **Nie ma jeszcze wojny, przejmowania przez 480 s, blokady budowania przy wrogu ani licznika obrońców.** To wyłącznie reguły budowlane strefy, których potem użyjemy w oblężeniach.
- Backup: przed wymianą JAR zatrzymaj Paper i wykonaj spójny backup świata i `plugins/EarthSMPCore/earthsmp.db`. Wymień wyłącznie JAR (bez `-plain`).


## 1.4.0 — centralne GUI `/esmp`

- `/esmp` lub `/esmp menu` otwiera **centrum EarthSMP**. `/esmp nation menu` zachowuje wcześniejsze menu narodowe.
- Zakładka **Państwo**: działające wcześniej zaproszenia, członkowie, role, transfer przywództwa, tworzenie kraju. Powrót z narodowego menu prowadzi do centrum.
- Zakładka **Terytoria**: punkt 1, punkt 2, podsumowanie zaznaczenia, propozycja ADD/REMOVE z potwierdzeniem, oczekujące wnioski własnego kraju z opcją anulowania. Administrator widzi oczekujące wnioski i może je zaakceptować/odrzucić. Wymagane uprawnienia i walidacja zakresu pozostają we wcześniejszej klasie TerritoryCommand/SQLite.
- Zakładka **Rdzenie**: informacja o stanie 0/1 + 0/2, sprawdzenie ochrony, odbiór darmowego rdzenia (z potwierdzeniem), przeniesienie rdzenia (z potwierdzeniem), instrukcja craftingu; admin ma strefę zonecheck i zmianę stolicy. Komendy rdzeni nadal sprawdzają prawa, miejsce, pełny ekwipunek i ważność rdzenia.
- **Dyplomacja / Historia / Ustawienia**: wyraźnie oznaczone jako niedostępne; żadnych fikcyjnych efektów.
- GUI używa wyłącznie Bukkit Inventory API (bez modów klienta), własnych InventoryHolderów i przełącza ekrany w następnym ticku. Nie da się wyciągać przedmiotów z menu.
- **Nie zmienia schematu SQLite**, posiadłości ani rdzeni. Żadnych lokalizacji rdzeni w publicznej mapie.
- Obecne komendy pozostają dostępne jako awaryjna i administracyjna ścieżka.

### Test

1. Po zatrzymaniu serwera wykonaj spójny backup świata Earth i earthsmp.db.
2. Rozpakuj patch do repo, zbuduj `.\gradlew.bat clean build`, wrzuć tylko `EarthSMP-Core-1.4.0.jar` (nie `-plain`).
3. `/esmp` => `Panstwo`, `Terytoria`, `Rdzenie`, `Dyplomacja`; menu kraju => `Powrot do centrum`.
4. `Terytoria`: zaznacz pos1 i pos2 w swiecie Earth, pokaż selection, złóż testowy wniosek o 1 neutralny chunk; admin: wnioski => zaakceptuj. Potwierdź zmianę na squaremap. **Uważaj, to realnie zmienia mapę!**
5. `Rdzenie`: sprawdź stan Polski, `Czy jestem chroniony?`, przejdź do instrukcji; **nie pobieraj ponownie darmowego rdzenia**, jeśli polski już został wykorzystany. Zobacz okno potwierdzenia pickup bez potwierdzania, aby nie przenosić rdzenia przypadkiem.
6. `Dyplomacja`: ma wyświetlić informację „jeszcze nieaktywna”, bez możliwości rozpoczęcia wojny.

Ważne: podgląd listy oczekujących wniosków pobiera najwyżej 50, zgodnie z istniejącym limitem bazy. W przypadku wielu wniosków rozszerzymy paginację SQL w przyszłym etapie.


## v1.5.0 — Dyplomacja

- `/esmp` → **Dyplomacja**: deklaracje wojny (z opcjonalnym opisem), zaproszenia do
  koalicji, uczestnicy, wycofanie sojuszników, obustronna zgoda na pokój.
- `/esmp war list` to komenda awaryjna do odczytu wojen.
- `/esmp war adminend <id>` — awaryjne zakończenie wojny przez operatora.
- Sojusznicy mogą wejść tylko w ciągu 4 godzin **od wypowiedzenia** wojny;
  mogą opuścić konflikt w dowolnym momencie.
- Podnoszenie i ponowne stawianie rdzeni jest zablokowane, jeśli kraj
  bierze udział w dowolnej aktywnej wojnie.
- Nie ma jeszcze walk wojennych, praw legalnego oblężenia ani podbijania rdzeni.
- Poprzednie państwa i rdzenie są zachowane: migracja SQLite v5 → v6.
- `gradlew build` uruchamia test składni `plugin.yml` oraz test transakcji dyplomacji.


## 1.6.0 — warunki oblezen

`/esmp` → `Oblezenia` (lub `/esmp siege menu`). Gracz musi stac w obszarze chronionym
65×65 *wrogiego* rdzenia i nalezec do strony wojny przeciw jego wlascicielowi.
Oblezenie wolno rozpoczac 10:00–00:00 czasu `Europe/Warsaw` z co najmniej
`max(1, ceil(0.2 * liczba wszystkich czlonkow panstwa broniacego))` obroncami online.
Jesli wymagana obecnosc spadnie podczas oblezenia, biegnie 30 minut tolerancji;
po powrocie obroncow licznik resetuje sie. Koniec wojny, wyjscie uczestnika koalicji
z konfliktu, znikniecie rdzenia, polnoc w Polsce lub koniec tolerancji przerywaja oblezenie.

Obecnie sesje oblezen sa **ulotne** i zawsze koncza sie przy restarcie serwera.
Nie ma jeszcze niszczenia blokow, pladrowania skrzyn, kilofa oblezniczego ani przejmowania
rdzeni — ochrona baz pozostaje bez zmian. W kolejnej wersji podlaczymy te mechaniki
do legalnego stanu oblezenia. Admin moze zatrzymac oblezenie bedac na danym obszarze:
`/esmp siege adminstop`. Brak ujawniania wspolrzednych rdzeni na mapie.
