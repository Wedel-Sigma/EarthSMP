---
title: "Earth SMP — dokumentacja i specyfikacja v1.3"
version: "1.3"
date: "2026-11-06"
status: "EarthSMP-Core 1.8.1 na main — testy solo zaliczone; testy końcowe multiplayer oraz dalsze mechaniki w planie"
language: "pl"
---

# EARTH SMP — DOKUMENTACJA I SPECYFIKACJA v1.3

**Stan ustaleń: 6 listopada 2026 r.**  
**Przeznaczenie:** aktualny zapis zasad, postępu implementacji oraz informacji dla testujących. Dokument nie jest potwierdzeniem wdrożenia każdej wymienionej funkcji.  
**Skala:** prywatny serwer około 30 znajomych, zwykle około 5 osób w państwie; light roleplay, polityka, gospodarka i wojny.

> **Zasada pierwszeństwa:** wszystkie uzgodnione poniżej zasady nowej edycji mają pierwszeństwo przed regulaminem poprzednich edycji. Ze starego regulaminu obowiązuje wszystko, co nie koliduje bezpośrednio z nowymi zasadami. Nie przedstawiać propozycji jako decyzji zatwierdzonych.

## STATUS IMPLEMENTACJI — EarthSMP-Core 1.8.1 (6 listopada 2026)

> **To wersja do zamkniętych testów**, nie deklaracja gotowości na publiczny serwer. Poniżej odróżniamy potwierdzone zachowanie gry od planów oraz punktów wymagających poprawek.

**Kod:** [Wedel-Sigma/EarthSMP](https://github.com/Wedel-Sigma/EarthSMP) · **wersja pluginu:** `1.8.1` (scalona do `main`, PR #1) · **środowisko:** Paper 26.3 / Java 25 / SQLite / squaremap.

### Weryfikacja: dotychczasowe testy 1.8.0 i nowe testy 1.8.1

- **EarthSMP-Core 1.8.0 — pierwsza seria multiplayer:** 40/49 testów oznaczono jako zaliczone. Pozostałych 9 nie należy interpretować jako dziewięciu awarii — część była niewykonana lub dotyczyła funkcji niewdrożonych.
- **EarthSMP-Core 1.8.0 — końcowa seria multiplayer:** **12/12 zaliczonych**: blokada demontażu rdzenia na wojnie, reset przejmowania po restarcie, pełne 480 s, zmiana właściciela zdobytego rdzenia, dostęp do zdobytej bazy, brak zmiany granic, zdobycie ostatniego rdzenia, upadek państwa, czyszczenie powiązanych danych oraz trwałość po restarcie.
- Z wcześniejszych testów: państwa i uprawnienia, zaproszenia i role, wnioski o terytoria, mapa polityczna, stawianie i ochrona rdzeni, blokada TNT/pistonów, crafting i 15 użyć Kilofa Oblężniczego, deklaracja wojny, automatyczne oblężenie, BossBar, kontestowanie i cofanie postępu, kontrola obecności obrońcy, `/esmp siege check`, zatrzymanie szturmu i pokój.
- **Dostęp do skrzyń:** przed zdobyciem chronionej bazy zwykły gracz nie mógł otworzyć skrzyni; po prawidłowym zdobyciu test dostępu do zdobytej bazy zaliczono. Przechwytywanie przedmiotów przez mechanizmy techniczne (np. hoppery) wymaga osobnych testów.
- Po testach przywrócono normalne **godziny oblężeń 10:00–00:00 Europe/Warsaw**. Tymczasowe obejście godzin służyło wyłącznie do testów.
- **EarthSMP-Core 1.8.1 (6.11.2026):** zbudowano JAR, uruchomiono na serwerze i **zaliczono dostępne testy solo** (m.in. czwarty licznik rdzeni i zmianę stolicy). Kod 1.8.1 został scalony do `main`. **Nie wykonano jeszcze końcowej sesji multiplayer dla 1.8.1**; zaplanowano trzech testerów (dwóch zwykłych graczy + administrator).
- **Stan implementacji 1.8.1:** automatyczna sukcesja stolicy po zdobyciu, ręczne wskazanie nowej stolicy w pokoju przez lidera/zastępcę, dobrowolne rozwiązanie państwa przez lidera w pokoju z potwierdzeniem oraz licznik wszystkich kontrolowanych rdzeni (własne + zdobyczne) w GUI i `/esmp core stock`. Rozwiązanie państwa i sukcesja po zdobyciu nadal wymagają pełnych testów multiplayer.


### Stan 1.8.1 i rzeczy pozostałe do sprawdzenia

1. **Stolica:** automatyczna sukcesja po zdobyciu starej jest zaimplementowana; lider/zastępca może podczas pokoju wskazać inny własny, już postawiony rdzeń. Podniesienie aktualnej stolicy pozostaje zablokowane — najpierw trzeba przenieść status stolicy na inny rdzeń. **Nowy cooldown 6 h na państwo to ustalony plan, a nie funkcja 1.8.1.**
2. **Rozwiązanie państwa:** dostępne dla lidera w pokoju przez GUI lub `/esmp nation disband`, po potwierdzeniu; dotyczy wszystkich aktualnie należących do niego rdzeni (własnych i zdobycznych), członkostw oraz granic. Budynki pozostają. **Test destrukcyjny wykonywać wyłącznie na kopii świata i bazy.**
3. **GUI rdzeni:** czwarty licznik pokazuje łączną liczbę aktualnie postawionych rdzeni kontrolowanych przez państwo, w tym zdobyczne; dotychczasowy licznik postawionych własnych rdzeni pozostaje.
4. **Backup:** aktualny `EarthSMP-Backup.ps1` tworzy spójny backup **offline po zatrzymaniu Paper**, obejmuje `plugins/EarthSMPCore/earthsmp.db` i sprawdza jego obecność w ZIP. Nie jest to dawny wariant backupu online przez RCON. Pełny test przywrócenia kopii z SQLite nadal warto przeprowadzić.
5. **Testy zaległe:** wojenne ograniczenia i zmiana stolicy z drugim graczem, fizyczne usuwanie własnych i zdobytych rdzeni przy rozwiązaniu państwa, trwałość SQLite po restarcie, próby obejścia ochrony i testy kilku państw.

### Dalsze prace / niepotwierdzone w testach

- **Nowe, zatwierdzone założenie interfejsu terytoriów:** claimowanie chunków ma docelowo przypominać **FTB Chunks**, ale odbywać się **na internetowej mapie squaremap**, bez obowiązkowego moda. Gracz zaznacza chunki myszką, a wniosek o zmianę granic wciąż zatwierdza administrator. **W 1.8.1 działa tylko dotychczasowy system komend i wniosków, nie edytor mapowy.**
- **Mody klienta są opcjonalne**; zamiast obowiązkowego instalatora-wizarda planujemy paczkę modów i krótki tutorial dla chętnych. Czysty klient Minecrafta ma pozostać wystarczający do gry.
- Prywatne pozycje graczy na mapie (tylko własne państwo; żadnych pozycji wrogów, sojuszników ani w Netherze) — **plan**, nie funkcja 1.8.1.
- **Publiczne pozycje wszystkich postawionych rdzeni** (niezależnie od właściciela, granic i dyplomacji) — zaakceptowane przez głosowanie graczy, **jeszcze niewdrożone**; tylko ikona lodestone i współrzędne `X/Y/Z`, bez numerów oraz oznaczenia stolicy.
- **Centralny zegar edycji:** dzień serwera zmienia się o **06:00 Europe/Warsaw**. Planowana komenda admina `/esmp clock`, `/home` również w wojnie: bez limitu w dniach 1–2, od dnia 3 **4 teleportacje na gracza na dzień**, reset o 06:00. Planowany **cooldown 6 godzin na państwo** przy ręcznej zmianie stolicy. Żaden z tych modułów nie jest wdrożony w 1.8.1.
- **Końcowy polish UI:** `/esmp tierlist` — ranking wszystkich państw według liczby ich aktualnie postawionych rdzeni (własnych i zdobycznych); scoreboard/sidebar z dniem serwera, nazwą państwa, rdzeniami i przydatnym statusem. **Plan, nie funkcjonalność 1.8.1.**
- Testy większych koalicji, jednoczesnych wojen i obciążenia serwera, omijania ochrony skrzyń (hoppery, wagoniki), niespodziewanych zdarzeń bloków, exploitów i odporności na awarie.
- Pełne funkcje PvP pokojowego, timerów respawnu, progresji, limitów ekwipunku, enchantów, barterowych sklepów i pozostałych dodatków z dalszych rozdziałów — **zasady docelowe; bez osobnej weryfikacji nie oznaczamy jako ukończonych**.

### Feedback od testerów

Przy zgłaszaniu uwag podaj: **wersję pluginu**, **rolę i czy miałeś OP**, **kroki odtworzenia**, **wynik oczekiwany**, **wynik faktyczny** oraz (jeśli są) screenshot i istotny fragment logów. Nie publikuj publicznie adresu IP, danych Tailscale ani pełnych logów zawierających dane prywatne.

---

## 0. Skrót kluczowych reguł

- Każdy gracz należy do jednego państwa; nawet jednoosobowego. Granice polityczne to kolorowe, zatwierdzane przez administratora **chunki**, ale same granice nie chronią bloków. Docelowe zaznaczanie chunków odbywa się **myszką na internetowej mapie (interakcja w stylu FTB Chunks)**; obecnie 1.8.1 obsługuje wnioski przez komendy.
- Każde państwo dostaje **1 darmowy rdzeń**, może wykonać **2 dodatkowe**; maksimum **3 rdzenie własne**. Rdzenie zdobyte w wojnach **nie liczą się do limitu**.
- Każdy rdzeń chroni **65 × 65 bloków**, w całej wysokości Overworldu (**Y −64…319**). Obszary ochronne nie mogą na siebie nachodzić, ale mogą sąsiadować. Nowy rdzeń można postawić tylko na zatwierdzonym własnym terytorium; zdobyty rdzeń jest wyjątkiem od wymogu stawiania.
- Formalna wojna pozwala na oblężenia między **10:00 a 00:00 Europe/Warsaw**, gdy online jest co najmniej **20% całego składu broniącego państwa** (zaokrąglenie w górę; min. 1). Spadek poniżej progu w trakcie daje **30 minut** na dokończenie; o północy koniec bez wyjątku.
- Atakujący w chronionej bazie mogą niszczyć **wyłącznie bloki postawione przez graczy**, specjalnym **Kilofem Oblężniczym**. Jeden kilof niszczy **15 bloków** i znika. Gdy podczas legalnego oblężenia wróg przebywa w chronionym regionie, **nikt nie może tam stawiać bloków**.
- Strefa przejmowania wokół rdzenia: **9 × 9 bloków**, Y rdzenia **−5…+10**. Wymagane **480 s**; atakujący bez obrońcy +1 s/s; obie strony w strefie: pauza; sami obrońcy: −2 s/s. Koniec oblężenia lub postęp 0 resetuje przypisanie zdobywcy; przerwanie oblężenia resetuje też cały postęp.
- Z państwa mającego **x rdzeni można zdobyć maksymalnie x−1**; ostatni rdzeń **znika**, a państwo **upada**. Całe jego terytorium staje się neutralne; granice później przydziela admin zgodnie z ustaleniami graczy.
- W czasie pokoju ochrona PvP we własnej bazie; na własnym terytorium politycznym po pierwszej próbie ataku **8 s obronnej nietykalności, cooldown 5 min**, pierwszy cios anulowany. W wojnie PvP działa normalnie; neutralne państwa zachowują pokojowe zabezpieczenia.
- Podczas wojny po kliknięciu **Respawn** rozpoczyna się widoczne **na ekranie (nie na czacie)** odliczanie zależne od członków **własnego państwa online**: 1–2 = 15 s; 3–5 = 20 s; 6–10 = 30 s; 11–15 = 40 s; 16+ = 50 s. Przez ten czas gracz nie może się poruszać, z niczym wchodzić w interakcje ani zostać zabity. Po zakończeniu odliczania natychmiast wraca zwykła rozgrywka, w tym możliwość zaatakowania go przez wrogów. W pokoju brak dodatkowego timera.
- Limity w **całym ekwipunku przez cały czas**: zwykłe złote jabłka 8; perły Endu 3; pajęczyny 16; totemy 3. Zaklęte złote jabłka, End Crystals i Respawn Anchors — całkowicie wyłączone. Po śmierci **cały ekwipunek wypada**.
- **Nether** dostępny po 10 dniach; **End** wyłączony na stałe. Zaklęcia przez handel z villagerami uzyskanymi przez leczenie zombie villagerów; Mending zakazany. Fortune: brak do dnia 6, I od dnia 7, III od dnia 16, Fortune II nigdy. Enchanty wpływające na PvP: maks. I przez pierwsze 9 dni, następnie maks. II.
- Barter **przedmiot za przedmiot**, bez wirtualnej waluty. Gracze: sklepy skrzynkowe. Admin: nieskończone wymiany podstawowych materiałów (np. cobblestone ↔ cobbled deepslate 1:1). Docelowa mapa przeglądarkowa koloruje chunki państw, pokazuje **graczy tylko członkom tego samego państwa**, a **wszystkie postawione rdzenie publicznie** z ikoną lodestone i współrzędnymi.
- **Zegar edycji:** centralny numer dnia zmienia się codziennie o **06:00 czasu polskiego**. Docelowe `/home` działa także podczas wojny: dni 1–2 bez limitu, od dnia 3 maksymalnie **4 razy dziennie na gracza** (reset 06:00). Ręczna zmiana stolicy: **6 godzin cooldownu na państwo** (plan).
- **Etap końcowy:** `/esmp tierlist` — państwa sortowane według łącznej liczby postawionych rdzeni (w tym zdobycznych); indywidualny panel boczny serwera/państwa w grze (plan).

---

## 1. Świat, serwer i obecny stan techniczny

### 1.1. Środowisko

- Lokalne środowisko testowe: **Paper 26.3**, **Eclipse Temurin Java 25.0.5**, Windows, serwer uruchamiany przez `start.bat`, sterta JVM `-Xms2G -Xmx6G`.
- Mapa **Earth Diamond Edition — No Caves / No Structures**, skala około **1:1500**, z zachowanymi naturalnymi złożami, biomami i spawnami mobów; brak rozbudowanych jaskiń, mogą występować geody i małe naturalne pustki.
- Mapa przycięta w MCA Selector do około **6144 × 6656 bloków**; orientacyjny zachowany zakres X **4608…10752**, Z **−3584…3072**.
- Worldborder: środek **(7680, −256)**, średnica **5120** bloków (grywalny obszar X 5120…10240 i Z −2816…2304). Teren wykraczający poza border pozostaje jako margines widokowy.
- **Spawn: Makau (Macao)**. Spawn, działki sklepowe i pozostałe obszary publiczne mają otrzymać ochronę administracyjną.
- Główne ustawienia: `difficulty=hard`, `enforce-whitelist=true`, `generate-structures=false`, `spawn-protection=0` (ochrona pluginem), `online-mode=true`, whitelist, view-distance około 10; `respawn_radius=0`, `players_sleeping_percentage=50`.
- Planowanych dwóch administratorów z pełną kontrolą nad państwami, terytoriami, rdzeniami i wyjątkowymi sytuacjami.

### 1.2. Backupy i stan pluginu

- Aktualny skrypt **`EarthSMP-Backup.ps1`** tworzy backup **offline**: należy zatrzymać Paper (`stop`), następnie uruchomić skrypt z `-ConfirmStopped`. Sprawdza on m.in. port, blokadę świata, obecność `level.dat` oraz **`plugins/EarthSMPCore/earthsmp.db`**. Do ZIP trafiają świat, baza SQLite, dane pluginów (z wyłączeniem dużych odtwarzalnych danych squaremap) i konfiguracja. Nie należy traktować go jako działającego backupu online przez RCON.
- Historyczny test przywracania wcześniejszej kopii świata: około **427,4 MB**, **14,6 s**. **Nie potwierdza on jeszcze przywracania nowego ZIP z SQLite**; test odtworzenia świata razem z bazą pozostaje do wykonania.
- Harmonogram backupu co 2 godziny był przetestowany, a potem usunięty na czas testowania; backupy automatyczne i retencja na docelowym hostingu są osobnym zadaniem.
- **Aktualizacja 2026-11-06:** `EarthSMP-Core 1.8.1` działa w testach solo i znajduje się na `main`. Testy multiplayer mechanik wojennych bazują dotychczas na **1.8.0**, a pełny test końcowy 1.8.1 odbędzie się w trzyosobowym składzie. Pozostałe rozdziały tego dokumentu zawierają również zasady **planowane**, a nie wdrożone.

## 2. Państwa i terytoria

- Członkostwo w państwie jest **obowiązkowe regulaminowo**. Dopuszczone państwa jednoosobowe, grupy i organizacje najemnicze. Jeden gracz może należeć do tylko **jednego** państwa.
- Rangi: **1 przywódca**, do **2 zastępców**, dowolni członkowie. Przywódca i zastępcy mają **zbliżone uprawnienia funkcjonalne**, np. zaproszenia, wyrzucanie członków, dyplomacja i zmiana stolicy w pokoju. **Wyjątek:** dobrowolnie rozwiązać państwo może tylko przywódca.
- Państwo można zakładać poprzez system serwera/GUI; otrzymuje jeden darmowy rdzeń.
- Terytorium polityczne jest zbiorem **chunków** oznaczonych kolorem państwa. Państwo proponuje zmianę granic, jeden z administratorów ją zatwierdza. **Zatwierdzony docelowy interfejs** to zaznaczanie chunków na mapie przeglądarkowej podobnie jak w FTB Chunks, a nie tylko wskazywanie dwóch punktów w świecie. Nie ma rozbudowanych punktów ekspansji czy sztywnych limitów terytorium. Granice nie powinny nakładać się na inny zatwierdzony obszar bez zgody/rozstrzygnięcia administracji.
- **Granice polityczne nie chronią bloków** i same w sobie nie blokują wejścia. Ochronę fizyczną dają tylko rdzenie i obszary publiczne.
- Brak automatycznego wykrywania casus belli i kar za samo wejście na cudze terytorium. Spory o granice są podstawą dyplomacji/RP.
- **Stan 1.8.1 — rozwiązanie państwa:** decyzję podejmuje **lider** po potwierdzeniu, tylko gdy państwo **nie uczestniczy w wojnie**. Znikają wszystkie aktualnie należące do państwa postawione rdzenie (własne i zdobyczne), członkostwa, relacje i jego ochrona; terytorium staje się neutralne, a zwykłe budowle pozostają. Dostępne przez GUI i `/esmp nation disband`; pełny test destrukcyjny na kopii świata pozostaje do wykonania. Administrator może interweniować w wyjątkowych przypadkach.
- Po upadku/rozwiązaniu państwa jego **polityczne terytorium staje się neutralne**; administrator nadaje później nowe granice zgodnie z pokojem i ustaleniami graczy. Dawni członkowie mogą wstąpić do innego państwa albo założyć nowe i uzyskać nowy darmowy rdzeń.
- Ukryte obozy, magazyny i bunkry poza ochroną są dozwolone. Nie można traktować braku rdzenia/claima jako zgody na kradzież lub grief w czasie pokoju.

## 3. Rdzenie ochronne: liczba, teren i cykl życia

- Każde nowo założone państwo dostaje **1 darmowy Rdzeń Ochronny**.
- Państwo może dodatkowo **wyprodukować maksymalnie 2 rdzenie**, czyli mieć **maks. 3 rdzenie własnego pochodzenia** (łącznie przedmioty w magazynach/ekwipunkach i rdzenie postawione).
- **Zdobyte** rdzenie nie liczą się do limitu 3 i mogą podnosić łączną liczbę baz dowolnie wysoko.
- Każdy rdzeń tworzy kwadrat **65 × 65** bloków, współrzędne X/Z `rdzeń ±32`, na pełnej wysokości świata (**Y −64 do 319** w bieżącej mapie). Regiony mogą bezpośrednio sąsiadować, **nie mogą się nakładać**. Trzy obok siebie mogą utworzyć megatwierdzę.
- **Nowy własny rdzeń można postawić tylko na zatwierdzonym terytorium własnego państwa**. Zdobycie już istniejącego rdzenia w cudzym regionie to dopuszczony wyjątek — nie przenosi automatycznie granic politycznych.
- Rdzeń musi mieć unikalne ID i powiązanie z państwem, aby nie obchodzić limitów; usunięcie państwa unieważnia i usuwa jego rdzenie, także przedmioty w ekwipunkach i schowkach, a chronione regiony tracą ochronę.
- **Własne** rdzenie można zdjąć i przestawić wyłącznie, gdy państwo **nie uczestniczy w wojnie**, i tylko na własne terytorium, bez nakładania regionów.
- **Rdzeni zdobytych w wojnie nie można przenosić** ani demontować w celu ustawienia gdzie indziej.
- Jeden z aktualnie posiadanych rdzeni państwa jest wyznaczony jako **stolica** (gdy istnieje). **Stan 1.8.1:** po utracie stolicy system automatycznie wybiera nową spośród pozostałych rdzeni. Lider lub zastępca może ręcznie wskazać inny własny postawiony rdzeń **podczas pokoju**; aktywnej stolicy nie można podnieść, dopóki nie wyznaczy się następnej. Admin może wykonać korektę ręcznie. **Nowe ustalenie DO WDROŻENIA:** ręczna zmiana stolicy ma **6 godzin cooldownu na całe państwo** (wspólny dla lidera i zastępców), bez ograniczania automatycznej sukcesji i korekt admina. Oznaczenie stolicy nie będzie publicznie wyróżniane na mapie.

### 3.1. Strefa bezpośrednio przy rdzeniu

- Strefa **9 × 9 bloków w X/Z**, od **Y rdzenia −5 do Y rdzenia +10** włącznie, jest zarówno obszarem kontroli przejmowania, jak i obszarem ograniczeń budowlanych.
- Nie wolno w niej stawiać **łóżek**, **obsydianu**, **płaczącego obsydianu**, **bloków netherite**, **reinforced deepslate**, **ancient debris** i podobnych skrajnie odpornych/niemożliwych do normalnego zniszczenia bloków (np. bedrock i bloki administracyjne, gdyby zostały uzyskane).
- Materiały wokół rdzenia mają pozwalać na uczciwe przebicie obrony; poza tą strefą można budować zwykłe, nawet rozległe systemy murów i fortyfikacji. Nie wolno obejść zasady, zamykając rdzeń w naturalnej nieprzebijalnej skale.

## 4. Rdzenie — crafting i koszty

### 4.1. Dodatkowy Rdzeń Ochronny

Własny crafting o układzie **3 × 3**; w **centrum 1 Jukebox (szafa grająca)**, a w ośmiu pozostałych miejscach po **64 sztuki**:

| Lewy slot | Środkowy slot | Prawy slot |
|---|---|---|
| 64 × węgiel | 64 × sztabka żelaza | 64 × sztabka miedzi |
| 64 × redstone | **1 × Jukebox** | 64 × lapis lazuli |
| 64 × sztabka złota | 64 × diament | 64 × odłamek ametystu |

Koszt: **512 surowców + 1 Jukebox**. Bez szmaragdów, wymagane zwykłe surowce, **nie bloki rud**. Receptura dostosowana do średniego państwa około 5 osób; ma wymagać dłuższego wspólnego farmienia.

**Technika:** standardowy crafting Minecraft zużywa tylko 1 sztukę ze slotu, więc trzeba wdrożyć własne sprawdzanie i pobieranie **całych 64 sztuk** z każdego slotu.

### 4.2. Kilof Oblężniczy

Analogiczny crafting **3 × 3**: w centrum **1 zwykły diamentowy kilof**, wokół **po 16 sztuk tych samych ośmiu surowców**, w tym samym układzie co rdzeń.

- Koszt: **128 surowców + diamentowy kilof**; 16 diamentów w surowcach + 3 zużyte do wykonania bazowego kilofa = **19 diamentów** (jeśli kilof jest świeżo wykonany).
- Wygląd: specjalny kilof z wizualnym połyskiem zaklętego przedmiotu (*glint*), bez konieczności rzeczywistego enchantu.
- **Dokładnie 15 legalnie zniszczonych bloków**, potem kilof znika. Nie można zwiększać tej liczby zwykłą wytrzymałością ani zaklęciami.
- Działa do przełamywania ochrony tylko przy **formalnej wojnie, aktywnym legalnym oblężeniu i na wrogim chronionym terenie**, wyłącznie na **blokach postawionych przez graczy**. **Samego rdzenia nie da się wykopać kilofem**: można go zdobyć wyłącznie przez licznik przejmowania.
- Naturalne niezniszczone bloki świata (stone, dirt, deepslate itd.) są dla oblężniczego niszczenia chronione. Blok tego samego typu **postawiony przez gracza** może zostać zniszczony Kilofem Oblężniczym.

## 5. Dyplomacja, sojusze i wojny

- Własne menu serwerowe, robocza komenda `/diplomacy`; tworzenie państwa, skład, dyplomacja, wojny, sojusze, propozycje pokoju.
- Przywódca i zastępcy mogą wypowiedzieć **formalną wojnę** dowolnemu państwu przez GUI; nie ma przymusowego casus belli. Można dodać **dowolny opcjonalny tekst ogłoszenia**, emitowany serwerowi.
- Państwo może prowadzić **wiele wojen jednocześnie**.
- Sojusz można zawrzeć także podczas wojny. Sojusznicy i organizacje najemnicze mogą wejść do trwającego konfliktu po jednej ze stron tylko w ciągu **4 godzin od pierwotnego wypowiedzenia wojny**. Późniejszy nowy sojusz nie resetuje tego okna.
- Sojusznicy / najemnicy mogą **jednostronnie opuścić konflikt w dowolnej chwili**; wszystkie państwa uczestniczące dostają powiadomienie.
- Dwaj **główni pierwotni uczestnicy wojny** mogą zakończyć wojnę tylko za **wzajemną zgodą** przez GUI. Traktaty pokojowe i reparacje są umowami społecznymi, nie automatycznie ściąganymi świadczeniami; ewentualne zmiany granic/rdzeni administrator zatwierdza ręcznie.
- Informacje o wojnach, przystępowaniu, wycofaniu i pokoju są ogłaszane zainteresowanym / całemu serwerowi zgodnie z typem zdarzenia.
- W czasie wojny nie można dobrowolnie rozwiązać państwa. **Przymusowy upadek po utracie ostatniego rdzenia jest wyjątkiem**; skomplikowane skutki dla innych równoległych wojen może rozstrzygnąć administrator, który ma pełną kontrolę.

## 6. Kiedy wolno oblegać i jak chronimy bazę

- PvP wojenne i formalne oblężenia to różne stany: **walczyć można podczas wojny normalnie**, lecz niszczenie rdzenia / chronionych murów wymaga spełnienia dodatkowych warunków.
- Legalne okno oblężeń: codziennie **10:00–00:00 czasu Europe/Warsaw**.
- Wymagana obecność: online co najmniej **`max(1, ceil(0,2 × liczba wszystkich członków państwa broniącego))`**. Do mianownika wliczamy także nieaktywnych/offline członków; nie stosujemy automatycznego filtrowania aktywności.
- Jeżeli obrońców online będzie za mało **po rozpoczęciu aktywnego oblężenia**, trwa **30-minutowy okres ochronny** na dokończenie rozpoczętego oblężenia. Jeśli wymagana liczba wróci online, okres ten się resetuje. Poniżej progu nie można zaczynać **nowego** oblężenia.
- **O północy** oblężenia kończą się bez względu na pozostały czas 30-minutowej tolerancji. Koniec wojny również kończy oblężenie.
- Zakończenie/przerwanie oblężenia **zeruje całkowicie postęp przejmowania (0/480)** i przypisanie państwa zdobywającego.
- W czasie pokoju chronione bazy są zabezpieczone przed obcym niszczeniem, stawianiem bloków, otwieraniem skrzyń i rabunkiem.
- **Przed udanym zdobyciem rdzenia atakujący nie może otwierać ani opróżniać skrzyń w chronionej bazie**, nawet podczas legalnego oblężenia. **Dopiero po skutecznym przejęciu rdzenia** nowy właściciel może otwierać i lootować skrzynie zdobytej bazy. Ochrona przed wcześniejszym otwieraniem została sprawdzona z konta zwykłego gracza; obejścia przez lejki i podobne mechanizmy wymagają dodatkowego testu.
- Niszczenie w chronionym terenie: wyłącznie Kilofem Oblężniczym, bloków pochodzenia graczowego, z wyłączeniem specjalnie chronionych elementów (aktywne łóżka i podpory). Niedozwolone obchodzenie ochrony przez zwykłe narzędzia, TNT, eksplozje, ogień, wodę/lawę, tłoki, redstone, fizykę bloków itp.
- **Gdy wrogi gracz przebywa na obszarze chronionego regionu 65 × 65 podczas legalnego oblężenia, nie wolno stawiać tam bloków ANI ATAKUJĄCYM, ANI OBROŃCOM.** Po opuszczeniu regionu przez ostatniego przeciwnika zasada ta wygasa, a obowiązują zwykłe prawa do budowania.
- **Poza chronionymi bazami** podczas wojny wolno niszczyć/okraść majątek **wrogiego państwa**; w czasie pokoju takie działania są zakazane regulaminem, nawet bez technicznego claima. Majątek neutralnych państw pozostaje chroniony regulaminowo.

## 7. Przejmowanie i upadek państw

### 7.1. Przejmowanie

- Strefa kontroli: **9 × 9 bloków** wokół rdzenia, poziomo; pionowo od **Y −5 do Y +10** względem rdzenia.
- Licznik od **0 do 480 sekund**. Tylko atakujący w strefie: **+1 s na sekundę**. Atakujący i obrońcy: **pauza**. Tylko obrońcy: **−2 s na sekundę**. Obecność bez uprawnienia (neutralni) nie powinna przyznawać postępu.
- Pierwszy uprawniony atakujący, który **rozpocznie** postęp, wskazuje **państwo przyszłego właściciela**. Przy koalicji pomoc sojuszników nie zmienia przypisania. Pierwszy atakujący może zginąć lub wyjść ze strefy, a prawo tego państwa utrzymuje się, dopóki postęp pozostaje powyżej zera i oblężenie nie zakończy się.
- Gdy postęp spadnie do zera, następna skuteczna próba może wskazać inne państwo koalicji. Koniec oblężenia całkowicie resetuje stan.
- Po dojściu do **480 s** rdzeń przechodzi pod kontrolę zwycięskiego państwa (o ile nie jest ostatnim rdzeniem obrońcy); ochrona bazy przechodzi z nim. **Granice polityczne nie zmieniają się automatycznie**.

### 7.2. Zasada `x − 1`

- Gdy państwo ma aktualnie **x** rdzeni, **pierwsze x−1** może zostać przejęte przez wrogów. **Ostatni** rdzeń po zdobyciu **znika** zamiast przechodzić do atakującego, a państwo **automatycznie przestaje istnieć**.
- Przykłady: 1 rdzeń → 0 zdobyczy; 2 → 1 zdobycz; 8 → 7 zdobyczy, ósmy znika.
- Państwo z 1 rdzeniem nie daje napastnikowi darmowej kolejnej twierdzy. Państwo z wieloma rdzeniami może utracić większość jako łupy.
- Po upadku całe terytorium państwa staje się **neutralne**. Admin potem ustala granice według ustaleń graczy. Już przejęte rdzenie pozostają własnością zdobywców.
- Likwidacja państwa nie usuwa postaci ani przedmiotów jego graczy; mogą oni wstąpić do innych państw lub utworzyć nowe i dostać nowy rdzeń startowy.

## 8. PvP pokojowe i wojenne

- W stanie **wojny** między uczestnikami obowiązuje normalne PvP; mechanika ochrony pokojowej między wrogami nie działa.
- Gdy **państwo nie uczestniczy w danym konflikcie**, zachowuje ochronę pokojową względem neutralnych i państw niebędących jego wojennymi przeciwnikami. Cudza wojna nie wyłącza wszystkich zabezpieczeń na serwerze.
- **Na własnym chronionym terenie 65 × 65 w czasie pokoju** gracz nie może zostać zabity przez innego gracza. Nie może też z bezpiecznej bazy zadawać obrażeń innym (ochrona nie służy do bezkarnego atakowania).
- **Na własnym politycznym terytorium poza rdzeniem w czasie pokoju** pierwsza próba uderzenia przez innego gracza nie zadaje obrażeń, uruchamia **8 sekund ochrony** i ostrzeżenie o ataku. Ponowna aktywacja dopiero po **5 minutach**; okres liczony od pierwszego uruchomienia. W czasie ochrony nie wolno zadawać obrażeń; własny atak natychmiast kończy ochronę. Obrażenia środowiskowe i własne pułapki nie uruchamiają tarczy.
- Na cudzym i neutralnym terenie nie ma takiej dodatkowej pokojowej tarczy terytorialnej; zachowujemy jednak pozostałe zasady regulaminowe.
- **Friendly fire jest włączony**; reguły wojny i pokoju stosują się do relacji między państwami zgodnie z powyższym systemem.

## 9. Śmierć, łóżka i zbrojownie

- `keepInventory=false`: **cały ekwipunek wypada** po każdej śmierci. Może go podnieść inny gracz, także wróg, zgodnie z regułami wojny i limitami przedmiotów.
- **W czasie pokoju brak dodatkowego timera respawnu.**
- **Podczas wojny** czas blokady odrodzenia zależy od liczby **online członków państwa danego gracza** w chwili śmierci:

| Członkowie swojego państwa online | Czas |
|---:|---:|
| 1–2 | 15 sekund |
| 3–5 | 20 sekund |
| 6–10 | 30 sekund |
| 11–15 | 40 sekund |
| 16+ | 50 sekund |

- **Moment rozpoczęcia:** po kliknięciu przycisku **Respawn** gracz pojawia się w miejscu odrodzenia (np. przy swoim łóżku) i od tego momentu na jego ekranie widnieje licznik (np. jako overlay; dokładna forma UI do dobrania). **Nie jest to komunikat na czacie.**
- **W trakcie odliczania:** gracz ma pełną nietykalność (nie można go zabić), nie może się poruszać, atakować, otwierać skrzyń, używać przedmiotów, wykonywać interakcji ani wpływać na przejmowanie rdzenia. Techniczna implementacja powinna zapobiegać obejściom blokady przez inne mechaniki. Czas zostaje ustalony na podstawie liczby członków państwa online w chwili śmierci i nie zmienia się przez późniejsze wejście lub wyjście graczy.
- **Po upływie czasu:** blokady i nietykalność znikają jednocześnie; od tej chwili gracz może normalnie walczyć i zostać natychmiast zaatakowany, także gdy wrogowie okupują miejsce odrodzenia. **Nie ma dodatkowych sekund ochronnych po zakończeniu licznika.**
- **„Zbrojownia” nie jest osobną strefą ani mechanicznym obiektem:** to umowna nazwa pomieszczenia, w którym gracze zapewne ustawią łóżka i skrzynie z zapasowym wyposażeniem. Zasada odrodzenia działa tak samo poza tak nazwanym pomieszczeniem.
- **Stan implementacji:** mechanika blokady odrodzenia jest **zatwierdzonym planem**, nie potwierdzoną funkcją EarthSMP-Core 1.8.0.
- Nie wolno stawiać łóżek w strefie **9 × 9, Y rdzenia −5…+10**.
- W chronionej **własnej** bazie można posiadać dowolną liczbę łóżek (nawet setki). **Niezniszczalne podczas oblężenia są tylko łóżka przypisane jako punkt odrodzenia konkretnego gracza oraz bloki bezpośrednio pod nimi**. Ochrona trwa także, gdy przypisany gracz jest offline. Pozostałe łóżka nie otrzymują specjalnego immunitetu.
- Obce łóżka poza chronionym regionem nie mają tej ochrony; nie można stworzyć niezniszczalnej wysuniętej bazy łóżkami. Tłoki, eksplozje i usuwanie podparcia nie mogą obchodzić ochrony przypisanych łóżek.
- Pomieszczenie nazywane potocznie „zbrojownią” może być strategicznym miejscem ze względu na łóżka i skrzynie. Napastnicy mogą je okupować, ale **nie mogą zabić gracza w czasie jego odliczania po respawnie**; mogą zaatakować go bezpośrednio po jego zakończeniu. **Nie mogą też otwierać skrzyń w chronionej bazie przed zdobyciem jej rdzenia**. Po przejęciu rdzenia prawo do lootowania przechodzi na nowego właściciela.

## 10. Limity przedmiotów i zakazy

Wszystkie limity dotyczą **całego ekwipunku gracza łącznie** (plecak + hotbar + offhand), **przez cały czas**, nie tylko podczas wojny. Magazyny stacjonarne mogą mieć nieograniczone zapasy.

| Przedmiot | Maksymalna liczba w całym EQ |
|---|---:|
| Zwykłe złote jabłka | **8** (historycznie dodatkowo ustalony stack do 16, ale limit 8 całości jest ostrzejszy) |
| Perły Endu | **3** |
| Pajęczyny | **16** |
| Totemy nieśmiertelności | **3** |
| Zaklęte złote jabłka (enchanted golden apples) | **0 — zakazane** |
| End Crystals | **0 — zakazane** |
| Respawn Anchors | **0 — zakazane** |

- Plugin blokuje **podniesienie z ziemi** i **wyjęcie ze skrzyni** ponad limit; jeśli da się wziąć tylko część, nadmiar pozostaje w poprzednim miejscu. Bez kar i automatycznego kasowania legalnych zapasów.
- End jest niedostępny, więc nie przewidujemy naturalnego źródła Shulker Boxów ani osobnego systemu wyjątków; administracja nie powinna dystrybuować ich wbrew tej zasadzie.
- Pozostałe odziedziczone zasady PvP z dawnego regulaminu opisano w sekcji 14.

## 11. Zaklęcia (enchanty) i progresja

- Zwykłe villagery zdobywa się **wyłącznie przez leczenie zombie villagerów**, a zaklęcia pozyskuje się przez **handel z tak uzyskanymi villagerami**. Nie ma adminowskiego sklepu z enchantami.
- Ograniczenia obowiązują **na gotowym przedmiocie** i muszą obejmować łączenie książek w kowadle, oferty handlowe oraz inne potencjalne źródła. Nie wolno obejść progów przez łączenie.
- **Mending** — zabroniony przez całą edycję.
- **Fortune II** — zabroniony przez całą edycję, nawet gdy Fortune I i III są dopuszczone.
- **Fortune I** — dostępny od 7. dnia; **Fortune III** — od 16. dnia; przed dniem 7 Fortune niedostępny.
- **Enchantmenty dające przewagę w PvP** (np. Protection, Sharpness, Power, Knockback, Fire Aspect, Thorns, Piercing, Quick Charge oraz analogiczne) — maksymalnie **poziom I przez pierwsze 9 dni edycji**, a **poziom II od 10. dnia do końca** (zaklęcia o naturalnym maks. I pozostają na I).
- **Wyjątki dostępne na normalnych poziomach**: **Unbreaking III**, **Efficiency V**, **Looting III**, **Silk Touch I** oraz naprawdę użytkowe zaklęcia na buty/hełm, które nie zapewniają istotnej przewagi w PvP (np. Aqua Affinity I, Respiration III). Dla zaklęć wpływających na mobilność lub przetrwanie w walce nie wolno automatycznie przyjmować pełnego poziomu jako „QoL”; szczegółowa lista wyjątków jest jednym z drobnych punktów technicznego doprecyzowania.
- Nie ma mechaniki stopniowego odblokowywania wszystkich enchantów lub płatnego adminowskiego sklepu; jedyne fazy to zatwierdzone limity PvP i harmonogram Fortune.

### 11.1. Oś czasu

| Moment edycji | Zmiana |
|---|---|
| Dni 0–6 | Fortune niedostępny; PvP enchanty maks. I |
| Dni 7–9 | Fortune I; PvP enchanty maks. I |
| Od 10. dnia | PvP enchanty maks. II |
| Po 10 dniach od startu | **Otwarcie Netheru** |
| Dni 10–15 | Fortune I |
| Od 16. dnia | Fortune I albo III; II niedostępny |
| Cała edycja | Mending zakazany; End zamknięty |

## 12. Nether, End, eksplozje

- **Nether jest zablokowany przez pierwszych 10 dni** i otwierany po upływie 10 dni od startu edycji.
- **End jest na stałe wyłączony**. W konsekwencji brak naturalnego dostępu do shulkerów/Shulker Boxów.
- W Netherze **można wysadzać łóżka w celu wydobywania ancient debris**. **Nie wolno używać eksplozji łóżek do PvP.** Jest to **zasada pisemna**, nie osobna blokada kodowa.
- **TNT Minecart PvP** jest zakazane **regulaminowo**, w tym pułapki z wagonikami TNT. Zwykłe TNT może służyć do prac budowlanych/górnictwa, ale nie wolno nim obchodzić ochrony baz.
- **Portal trapping** (celowe blokowanie gracza w przejściu przy zmianie wymiaru) jest zabroniony regulaminowo, także podczas wojny. Normalne zasadzki w pobliżu portali nie są tym samym.
- End Crystals i Respawn Anchors są niedostępne w całości (silniejsza zasada niż stary zakaz wyłącznie ich PvP).

## 13. Gospodarka i handel

- **Wyłącznie barter** — wymiana przedmiot za przedmiot, **bez wirtualnej waluty, kont i banków**.
- Gracze tworzą sklepy na bazie **gotowego pluginu skrzynkowego (Chest Shops)**. Towar przechowywany jest fizycznie; sprzedawany w zamian za inne przedmioty, a sprzedaż kończy się po wyczerpaniu zasobów.
- **Makau / spawn:** administrowane, chronione działki handlowe dla sklepów graczy; ochrona przed nieuprawnioną kradzieżą i niszczeniem.
- Sklepy administracyjne mają **nieskończone zapasy** i realizują wyłącznie podstawowe wymiany materiałów budowlanych, np. **1 cobblestone ↔ 1 cobbled deepslate**, obustronnie 1:1.
- Admin nie sprzedaje w nieskończoność zasobów strategicznych typu diamenty czy netherite. Szczegółową listę wymian ustala administracja w trakcie edycji.
- Konkretny kompatybilny plugin (barter + nieskończone sklepy admina) wybierzemy w etapie 3.2, nie piszemy własnego sklepu bez potrzeby.

## 14. Zasady społeczne i regulamin odziedziczony

Pochodzenie: regulamin poprzedniej edycji pokazany na Discordzie; **zachowujemy wszystkie niekolidujące punkty**, przy czym nowe rozwiązania mają pierwszeństwo.

- Każdy gracz powinien znać regulamin; obowiązuje **light RP** i **Simple Voice Chat**.
- Zakazane: cheaty, exploity, duplikowanie przedmiotów, toksyczność na czacie i voice chacie, przenoszenie konfliktów z gry do życia.
- **Zakaz wykorzystywania braku claima** do griefingu i kradzieży w czasie pokoju. W wojnie dopuszczalne są działania opisane w specyfikacji; **skrzynie w chronionym regionie wolno lootować dopiero po zdobyciu rdzenia**, a nie w trakcie próby jego przejęcia. Poza chronionymi bazami nadal obowiązują odrębne reguły wojenne.
- **Nemosowanie zakazane**: zamykanie/zabudowywanie przeciwnika blokami w sposób uniemożliwiający obronę, by bić go np. po nogach. Normalne mury i barykady nie są automatycznie tym samym.
- **Zakaz BowDamagePvP**: spamowania strzałami z efektem Instant Damage.
- **Zakaz TNT Minecart PvP**, pułapek opartych na wagonikach TNT; **zakaz wybuchających łóżek jako PvP** w Netherze (wydobywanie jest dozwolone); **zakaz pułapek portalowych**.
- **Zakaz modów pokazujących serca / dokładne HP innych graczy**.
- Limity ekwipunku ze starego regulaminu przyjęto jako **maksymalne sumy w całym EQ**: 8 zwykłych złotych jabłek, 3 perły, 16 pajęczyn; dodatkowo nowe max 3 totemy.
- **Zmiana względem starej edycji:** stary całkowity zakaz PvP bez wypowiedzenia wojny **nie obowiązuje**; zastępuje go nowy system PvP pokojowego. Stary bezwzględny zakaz każdej kradzieży i griefu ma wyjątki działań wojennych uzgodnione wyżej. Stary zakaz eksplozji łóżek w Endzie jest bezprzedmiotowy, bo End nie działa.
- Zakazy czysto społeczne, takie jak nemosowanie, portal trapping i TNT Minecart PvP, pozostają **regulaminowe**, nie trzeba ich automatycznie kodować.

## 15. Teleportacja, stolice i centralny zegar edycji — NOWE USTALENIA, DO WDROŻENIA

### 15.1. Jeden zegar dla całej edycji

- Plugin ma utrzymywać **centralny numer dnia serwera**: dzień **1** zaczyna się w chwili oficjalnego uruchomienia edycji; kolejne dni zaczynają się **codziennie o 06:00 czasu polskiego**, w strefie `Europe/Warsaw` (z uwzględnieniem zmian czasu letniego/zimowego).
- Są to **dni kalendarzowe serwera z granicą o 06:00**, a nie kolejne 24-godzinne okna liczone od startu. Gdy edycja rozpocznie się wieczorem, dzień 1 będzie krótszy. Przestój Paper nie zatrzymuje upływu dni — po restarcie numer dnia ma być obliczany z daty startu i bieżącej daty w przyjętej strefie.
- Data/godzina początku edycji powinna być utrwalona trwale (np. SQLite) i nie zmieniać się wraz z restartami ani logowaniem poszczególnych graczy.
- Ten sam zegar jest podstawą **wszystkich mechanik opartych na numerze dnia**: limitów `/home`, odblokowania wymiarów, etapów zaklęć, przyszłych wydarzeń i podobnych reguł. Istniejące szczegółowe progi opisują rozdziały 11–12.
- Zatwierdzona **administracyjna komenda `/esmp clock`** ma pokazywać aktualny dzień serwera, datę/godzinę rozpoczęcia edycji oraz czas do kolejnej zmiany dnia o 06:00. **Nie jest zaimplementowana w 1.8.1.**

### 15.2. `/home`: także podczas wojny

- `/home` teleportuje gracza **wyłącznie w pobliże rdzenia będącego aktualną stolicą jego państwa**. Pozostałe rdzenie nie są dowolnymi punktami szybkiej podróży. Sposób wyznaczania bezpiecznego miejsca docelowego jest szczegółem technicznym.
- **Dozwolone zarówno w pokoju, jak i podczas wojny.** Poprzednia zasada całkowitej blokady `/home` podczas wojny **została uchylona**.
- **Dni serwera 1–2:** `/home` jest **nielimitowane** dla wszystkich graczy, by ułatwić start.
- **Od dnia serwera 3:** każdy gracz ma **4 udane teleportacje na dzień serwera**, niezależnie od wojny i od tego, jak długo jest na serwerze. Limit jest **na gracza**, nie na państwo.
- **Reset dokładnie o 06:00 Europe/Warsaw**, przy przeskoku centralnego dnia serwera. To nie jest przesuwające się 24-godzinne okno. Akceptujemy możliwość użycia **4 teleportacji tuż przed 06:00 i 4 tuż po 06:00**.
- Użycie jest pobierane **dopiero po pomyślnej teleportacji**; nieudane wywołanie nie zużywa limitu. Dane limitów pozostają w SQLite i nie resetują się przy restarcie lub wylogowaniu.
- Informacja o pozostałych użyciach i następnym resecie powinna być czytelna dla gracza (komunikat i docelowo sidebar). W dniach 1–2 interfejs pokazuje `∞`.
- **Status:** zatwierdzona mechanika na przyszły etap pluginu, **nieobecna w 1.8.1**. Klasyczny respawn przy łóżku pozostaje niezależny od `/home`.

### 15.3. Zmiana stolicy i cooldown 6 godzin

- **Stan obecny 1.8.1:** lider i zastępca mogą podczas pokoju przeznaczyć inny, już postawiony własny rdzeń na stolicę. W czasie wojny ręczna zmiana jest zabroniona. Wyznaczenie nowej stolicy pozwala spakować poprzednią (o ile jest własna, nie zdobyczna).
- **Nowe ograniczenie DO WDROŻENIA:** po udanej ręcznej zmianie stolicy obowiązuje **6 godzin rzeczywistego cooldownu dla całego państwa**, a nie osobno dla graczy. Obejmuje to także zmianę wykonaną naprzemiennie przez lidera i zastępcę.
- Cooldown zapisujemy trwale i **nie resetujemy go o 06:00**, podczas restartu ani po zmianie składu państwa. Komunikat ma podać czas pozostały do kolejnej możliwej zmiany.
- **Automatyczna sukcesja** po utracie stolicy nie może zostać zablokowana przez cooldown; **interwencja administratora** również może go ominąć.
- Sam status stolicy jest istotny dla mechaniki `/home` i możliwości demontażu rdzenia, ale **na publicznej mapie wszystkie rdzenie wyglądają identycznie**.

## 16. Mapa polityczna i widoczność graczy

- Przeglądarkowa mapa świata w stylu dawnych Earth SMP: geografia + **półprzezroczyste kolorowe zaznaczenie chunków** należących do poszczególnych państw. Każde państwo ma kolor; obszar neutralny jest bez koloru. Po kliknięciu terenu pojawia się nazwa państwa.
- Polityczne granice i ochrona 65×65 rdzeni to **dwie odrębne warstwy systemu**.
- Zatwierdzona przez administratora zmiana granic trafia na mapę.
- **Wszystkie aktualnie postawione rdzenie pokazujemy PUBLICZNIE**, niezależnie od właściciela, położenia na terenie innego państwa oraz relacji dyplomatycznych. Decyzja zatwierdzona w głosowaniu przyszłych graczy. Każdy znacznik: **ikonka lodestone i nad nią współrzędne `X`, `Y`, `Z`**. Bez numerów rdzeni, nazw, rangi ani oznaczenia stolicy. Rdzenie w przedmiotach/ekwipunku nie mają znaczników.
- Pozycje graczy są widoczne **na żywo** tylko dla **członków tego samego państwa**, po autoryzacji konta. Nie pokazujemy innych państw, wrogów, neutralnych ani nawet sojuszników. Na mapie publicznej nie pokazujemy nikogo.
- Lokalizacje odświeżają się na bieżąco, bez sztucznego opóźnienia (praktycznie co kilka sekund według możliwości integracji).
- Po wejściu do **Netheru** znacznik gracza znika z mapy Overworldu i wraca po powrocie. **Nie robimy osobnej mapy politycznej Netheru.**
- Technicznie widoczność musi wynikać z uwierzytelnienia i serwerowego filtrowania danych — ukrycie markerów w JavaScripcie przeglądarki nie zabezpiecza współrzędnych wroga.
- **Stan 1.8.1:** polityczne kolory chunków są widoczne przez squaremap; tracker graczy jest wyłączony; **prywatne znaczniki graczy, publiczne znaczniki rdzeni i edytor WWW nie są jeszcze wdrożone**. Mapa na `localhost` działa lokalnie, dopóki nie skonfiguruje się osobnego bezpiecznego dostępu.
- Używamy **squaremap** do istniejącej warstwy politycznej. Prywatne pozycje graczy wymagają uwierzytelnienia i filtrowania danych na backendzie; **publiczne rdzenie są osobną, jawną warstwą**. Obie należy aktualizować po zmianach właścicieli, zakładaniu, podnoszeniu, zdobyciu i likwidacji rdzeni.

### 16.1. Edytor terytoriów na mapie — zatwierdzony kierunek, DO WDROŻENIA

**Cel:** interakcja z chunkami **jak w FTB Chunks**, ale na **internetowej mapie squaremap**. To istotne doprecyzowanie pierwotnego pomysłu na „claimowanie” — chodzi o łatwe, wizualne zaznaczanie chunków, **nie o przeniesienie ochrony baz z rdzeni na claimy**.

**Docelowy przebieg dla gracza:**

1. Gracz otwiera mapę WWW i wchodzi w **tryb edycji terytorium** po uwierzytelnieniu kontem Minecraft (proponowana metoda: krótki, jednorazowy kod wydany komendą w grze; szczegóły do dopracowania).
2. Mapa pokazuje geografię, aktualne kolory państw oraz **siatkę chunków 16×16**. Gracz widzi, które chunki są neutralne, własne lub zajęte przez inne państwa.
3. **Kliknięciem i przeciąganiem myszki** zaznacza lub odznacza wiele chunków. Przykładowy interfejs: LPM dodaje do zaznaczenia, PPM usuwa; finalne skróty i wygoda na telefonach wymagają dopracowania.
4. Zmiany są najpierw **podglądem/wnioskiem**, bez natychmiastowego nadawania własności. Gracz przegląda zaznaczenie i wysyła wniosek ADD/REMOVE.
5. **Administrator zatwierdza albo odrzuca** wniosek, widząc dokładne chunki oraz możliwe konflikty. Dopiero po zatwierdzeniu zmieniają się granice w SQLite oraz publiczna warstwa polityczna.
6. Serwer sprawdza uprawnienia, własność i konflikty **po stronie backendu**; nie ufamy samemu interfejsowi przeglądarkowemu.

**Ważne ograniczenia i zgodność z istniejącym systemem:**

- Polityczny claim nadal **nie blokuje PvP, kopania, budowania ani wchodzenia na obszar**. Ochronę budowli zapewniają **wyłącznie rdzenie 65×65** oraz obszary administracyjne.
- **Zachowujemy istniejący proces zatwierdzania przez administrację, bazę SQLite i dane terytoriów.** Zmieniamy przede wszystkim UX wyboru chunków, nie zasady granic.
- Istniejące `/esmp territory pos1`, `pos2`, `propose` i narzędzia admina pozostają jako **alternatywa/tryb awaryjny**; edytor WWW będzie domyślnym sposobem dla zwykłych graczy.
- Mapa musi być osiągalna przez graczy **spoza komputera hosta**; sam `localhost` nie wystarczy. Zapewniamy bezpieczny dostęp WWW i autoryzację operacji, nie udostępniamy bez potrzeby prywatnych znaczników i danych.
- **Stan 1.8.1:** istnieje publiczna warstwa kolorowych terytoriów squaremap oraz komendowe składanie wniosków. **Brak jeszcze klikalnego edytora**, logowania do niego i panelu zatwierdzania w przeglądarce. To prace na **następny etap mapy**, nie już wydana funkcja.

### 16.2. Publiczne znaczniki rdzeni — zatwierdzone głosowaniem, DO WDROŻENIA

- Na mapie WWW każdy obserwator, **również niezalogowany, z innego państwa, sojusznik lub wróg**, widzi **wszystkie postawione rdzenie**. Nie ma ukrywania według dyplomacji ani granic politycznych.
- Znak graficzny to **lodestone**. **Nad nim koordynaty `X`, `Y`, `Z`** rzeczywistego bloku rdzenia. Nie podajemy numeru („rdzeń 1/2”), nazwy, właściciela ani statusu **STOLICA** przy znaczniku. Polityczne kolory terytoriów są osobną warstwą.
- Po postawieniu lub przeniesieniu rdzenia marker pojawia się/zmienia lokalizację; po zdobyciu pozostaje w tym samym miejscu (bez publicznego oznaczania zmiany właściciela); po demontażu, zniszczeniu ostatniego rdzenia lub rozwiązaniu państwa znika. Dane synchronizowane z zatwierdzonym stanem świata/SQLite.
- **Publiczność znaczników rdzeni NIE oznacza upublicznienia pozycji graczy**. Pozycje graczy nadal widoczne tylko dla członków ich własnego państwa (oraz samego gracza), z wykluczeniem sojuszników, wrogów i publicznej mapy.
- **Stan 1.8.1:** znaczniki rdzeni są planowane; dotychczas nie są publikowane przez squaremap.

### 16.3. `/esmp tierlist` — ranking państw (końcowy etap GUI)

- Komenda otwiera **minecraftowe GUI** wszystkich istniejących państw, posortowanych **malejąco według łącznej liczby ich aktualnie postawionych rdzeni**; liczą się **własne i zdobyczne**, lecz nie rdzenie-przedmioty w ekwipunku czy magazynie. Remisy mogą mieć stabilny porządek alfabetyczny (detal techniczny).
- Każde państwo ma ikonę (np. baner/ikona kraju); po **najechaniu kursorem** gracz widzi tooltip: **nazwa państwa, liczba jego członków i liczba kontrolowanych rdzeni**. Liczba członków online może być dodatkową przydatną informacją, jeśli nie będzie zaśmiecać interfejsu.
- Ranking jest czytany z aktualnego stanu państw i rdzeni, a nie ręcznie wprowadzany; po zdobyciu lub utracie rdzenia kolejność się zmienia.
- **Status:** zatwierdzony pomysł na końcowe upiększanie interfejsu; **brak w 1.8.1**.

### 16.4. Sidebar / scoreboard — informacja widoczna podczas gry (końcowy etap GUI)

- Po **prawej stronie ekranu**, w standardowym minecraftowym scoreboard sidebar, pokazujemy **aktualny dzień edycji**, **nazwę własnego państwa**, **łączną liczbę jego kontrolowanych rdzeni (własne + zdobyczne)**.
- Dodatkowe przydatne informacje 24/7: **liczba członków państwa online / łącznie**, **status pokój/wojna** oraz **pozostały limit `/home`** (`∞` w dniach 1–2; od dnia 3 np. `2/4`). To docelowy, zwięzły układ; kolejność i wygląd zostaną dopracowane w ostatniej fazie.
- Gracz bez państwa ma widzieć czytelne `Brak państwa` i sensowną informację o stanie serwera. Dane powinny być **indywidualne dla oglądającego**, aktualizowane po zmianach państwa, rdzeni, wojny i dnia serwera.
- Możliwość wyłączania HUD komendą `/esmp sidebar` to **propozycja techniczna**, nie wymóg zatwierdzony przez głosowanie.
- **Status:** plan na etap polish, niegotowe w 1.8.1.

### 16.5. Instalacja klienta — brak obowiązkowych modów i wizarda

- EarthSMP pozostaje **grywalny na czystym kliencie Minecraft Java**. Do claimowania przez mapę WWW **nie potrzeba moda klienta**.
- Własny instalator typu **wizard (Windows/C#) nie jest obecnie planowany jako wymagany element wdrożenia**. Rezygnujemy z przymusowej instalacji dodatkowego oprogramowania.
- Opcjonalnie opublikujemy **paczkę modów graficznych, wydajnościowych i QoL** oraz **krótki poradnik instalacji** dla zainteresowanych.
- Mody nie mogą być warunkiem udziału w rozgrywce ani dostępu do mechanik państwowych.

## 17. Administracja i wyjątkowe przypadki

- Administratorzy mają **pełną kontrolę nad wszystkimi państwami, rdzeniami, członkostwem, wojnami, granicami i ochroną**. Błędy lub nietypowe zakończenia konfliktów można ręcznie naprawić.
- W trakcie wojny dobrowolne rozwiązanie państwa jest zablokowane. Jeśli państwo **przymusowo upada** po utracie ostatniego rdzenia, likwidację równoległych wojen i relacji koalicyjnych może przeprowadzić administrator.
- **Stan 1.8.1:** zdobycie stolicy państwa posiadającego kolejne rdzenie wyznacza nową stolicę automatycznie; komenda zmiany stolicy jest dostępna dla lidera/zastępcy podczas pokoju. **Sześciogodzinny cooldown na państwo to nowy, jeszcze niewdrożony warunek.**
- W czasie pokoju gracz bez państwa (np. bezpośrednio po upadku) powinien jak najszybciej dołączyć do innego albo założyć nowe; obowiązek jest regulaminowy, nie trzeba budować skomplikowanego systemu kar za chwilowy stan przejściowy.
- System rozpoznawania pochodzenia bloków należy uruchomić **zanim gracze zaczną budować** na docelowym serwerze; trzeba uwzględnić tłoki, grawitację, płyny, automatyczne farmy, budowanie zniszczonych struktur i zmiany właściciela.

## 18. Wdrożenie — plan i zakres przyszłych pluginów

**Etap 3.1:** pierwotny opis mechanik (zakończony). Ten dokument został później rozszerzony o faktyczne wyniki testów 1.8.0.

**Etap 3.2:** dobór gotowych pluginów, np. LuckPerms, WorldGuard/WorldEdit, CoreProtect, barterowych Chest Shops i komunikacji głosowej. **Stan wdrożenia poszczególnych dodatków wymaga osobnej weryfikacji**.

**Etap 3.3 — w trakcie:** własny plugin **EarthSMP-Core 1.8.1** (Paper, Java 25, Gradle, SQLite) znajduje się na `main`. Mechaniki państw, terytoriów, mapy politycznej, rdzeni, ochrony, wojen, kilofa i przejmowania przeszły testy multiplayer w **1.8.0**; zmiany **1.8.1** przeszły dostępne testy solo, a końcowy test trzech osób pozostaje do wykonania. Backup offline obejmuje już bazę SQLite, ale jego pełne przywracanie wymaga testu. **Nie potwierdzono jeszcze ukończenia** timerów respawnu, PvP terytorialnego, limitów ekwipunku, enchantów, progresji, `/home`, zegara edycji, rankingów i znaczników graczy/rdzeni.

**Kolejne etapy (kierunek rozwoju, bez deklarowania dat):**

1. **Mechaniki przed kolejną dużą aktualizacją:** centralny zegar edycji ze zmianą dnia o **06:00 Europe/Warsaw** i `/esmp clock`; `/home` również podczas wojny (dni 1–2 bez limitu, od dnia 3 cztery użycia na gracza na dzień, reset 06:00); **6 h cooldownu ręcznej zmiany stolicy na państwo**.
2. **Mapa WWW squaremap:** edytor chunków w stylu FTB Chunks (autoryzacja gracza, zaznaczanie myszką, wnioski, panel admina); **prywatne pozycje członków tego samego państwa** na żywo; **publiczne znaczniki wszystkich postawionych rdzeni** z ikoną lodestone i współrzędnymi; bezpieczny dostęp spoza localhost.
3. **Końcowy polish:** `/esmp tierlist` według liczby postawionych własnych i zdobycznych rdzeni, tooltipy państw, indywidualny scoreboard/sidebar z dniem, państwem, liczbą rdzeni i przydatnymi statusami; dopracowanie interfejsu.
4. **Infrastruktura i inne mechaniki:** opcjonalny modpack QoL/wizualny z tutorialem, pregenerowanie mapy/LOD dla Distant Horizons, docelowy hosting i automatyczne backupy; pozostałe zasady rozdziałów 8–14 według osobnych etapów. **Kod kolejnej wersji będzie dostarczany użytkownikowi jako pełny ZIP projektu, nie wymaga przełączania gałęzi Git.**

## 19. Szczegóły do rozstrzygnięcia podczas projektowania technicznego (NIE nowe reguły)

Dla przejrzystości poniższe elementy nie są ukrytymi „zatwierdzonymi zasadami”; to techniczne wyjątki wymagające rozpisania implementacji:

1. **Techniczny wygląd blokady respawnu (reguły rozstrzygnięte):** wybrać ekranowy sposób prezentacji 15–50 s, zabezpieczyć pełny brak ruchu i interakcji oraz nietykalność *podczas* odliczania. Po odliczaniu brak ochrony. „Zbrojownia” nie jest specjalną strefą.
2. **Dokładna lista zaklęć QoL:** pełne wyjątki dla butów/hełmu, zwłaszcza enchanty jednocześnie poprawiające mobilność w PvP. Pozostałe zaklęcia podlegają ogólnemu limitowi I/II.
3. **Odporność systemu ochrony:** pełna lista wyjątków fizyki, tłoków, eksplozji, zastępowania bloków, redstone oraz obrony łóżek i ich podpór.
4. **Precyzyjna mechanika startu oblężenia:** rozróżnienie obecności wrogiego gracza w chronionej bazie, startu ataku przy progu online i prowadzenia licznika; nie rozszerzać blokad na pokój lub neutralnych.
5. **Mapa prywatnych pozycji i jawnych rdzeni:** uwierzytelnienie do pozycji współobywateli; aktualizacja na żywo i filtrowanie po stronie serwera. **Pozycje wszystkich rdzeni są publiczne, ale pozycje graczy pozostają prywatne.**
6. **Edytor terytoriów WWW (kierunek zatwierdzony):** autoryzacja konta Minecraft, narzędzia zaznaczania wielu chunków (również mobilnie), podgląd wniosków, panel admina i walidacja backendowa. Sam wybór mapy WWW zamiast moda jest już rozstrzygnięty.
7. **Zegar i `/home`:** prawidłowe obchodzenie się ze strefą `Europe/Warsaw`, zmianą czasu i restartami; czytelne komunikaty o resecie i cooldownie. Zachować ustalone 06:00, dni 1–2 bez limitu i 4 użycia od dnia 3.
8. **Istniejące zapisy „od dnia 10” a „po 10 dniach”:** rozdziały 11–12 historycznie używają obu sformułowań dla Netheru. Przed implementacją ustalić jednoznaczny próg wg centralnego numeru dnia, bez domyślnej reinterpretacji wcześniejszych reguł.
9. **Interfejsy i konkretne pluginy:** wygląd ikony państwa, szczegóły bezpiecznego miejsca przy stolicy, ewentualne ukrywanie sidebaru oraz adminowskie komendy ręcznej korekty. To decyzje techniczne, nie nowe zasady.

**Koniec dokumentacji v1.3.** Sekcja stanu implementacji oddziela to, co przetestowane, od zasad i planów. W razie kolizji obowiązują nowsze, wyraźnie oznaczone ustalenia (w szczególności §15–16 dotyczące `/home`, zegara i mapy).
