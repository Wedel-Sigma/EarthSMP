---
title: "Kabina Narodów 2.0 — dokumentacja i specyfikacja v1.8.13"
version: "1.8.13"
date: "2026-11-08"
status: "EarthSMP-Core 1.8.13 na VPS; Minecraft, portal HTTPS i backupy działają; dzień 0, oczekiwanie na przygotowanie spawnu i test odtwarzania"
language: "pl"
---

# KABINA NARODÓW 2.0 — DOKUMENTACJA I SPECYFIKACJA v1.8.13

**Stan ustaleń: 8 listopada 2026 r.**  
**Przeznaczenie:** aktualny zapis zasad, postępu implementacji oraz informacji dla testujących. Dokument nie jest potwierdzeniem wdrożenia każdej wymienionej funkcji.  
**Skala:** prywatny serwer około 30 znajomych, zwykle około 5 osób w państwie; light roleplay, polityka, gospodarka i wojny.

> **Zasada pierwszeństwa:** wszystkie uzgodnione poniżej zasady nowej edycji mają pierwszeństwo przed regulaminem poprzednich edycji. Ze starego regulaminu obowiązuje wszystko, co nie koliduje bezpośrednio z nowymi zasadami. Nie przedstawiać propozycji jako decyzji zatwierdzonych.


## OFICJALNA NAZWA SERWERA — Kabina Narodów 2.0

- **Nazwa prezentowana graczom:** **Kabina Narodów 2.0**. Dotyczy nagłówków w portalu, wiadomości na czacie, Minecraft GUI, sidebaru i etykiet mapy.
- **Nazwy techniczne bez zmian:** `EarthSMP`, `EarthSMPCore`, `EarthSMP-Core`, repozytorium, pakiety i zmienne Javy, identyfikatory w SQLite, nazwy kluczy w squaremap, uprawnienia oraz polecenia `/esmp` i `/earthsmp`.
- **Konfiguracja:** `server.display-name: "Kabina Narodów 2.0"` w `plugins/EarthSMPCore/config.yml` (na nowych instalacjach wpis jest w domyślnym `config.yml`, a na starszych plikach działa wartość domyślna). Zmiana wymaga restartu pluginu/serwera, nie zmiany plików źródłowych.
- **Historia rebrandingu:** `1.8.6 R2` wprowadził nazwy widoczne dla graczy, a `1.8.6 R3` naprawił błąd kompilacji w `SiegeManager.say(...)`. Bieżący build **1.8.13** jest sprawdzony i uruchomiony na VPS.
- **Elementy historyczne:** dawniej wydane przedmioty mogą przechowywać starą nazwę w metadanych przedmiotu aż do ponownego uzyskania lub ręcznej aktualizacji — nie są bezpiecznie masowo nadpisywane.

## STAN AKTUALNY — 8 listopada 2026

**Wersja:** EarthSMP-Core **1.8.13**, Paper **26.3**, Java **25**, SQLite **schema v10**. Główne mechaniki gotowe i w większości sprawdzone. Serwer znajduje się na **VPS Contabo** (Ubuntu 24.04), nie na lokalnym Windowsie. Minecraft działa publicznie bez Tailscale; **Caddy + HTTPS + portal polityczny i logowanie WWW** zostały sprawdzone. **Edycja nie została jeszcze uruchomiona** — dzień 0; trwa etap przygotowania spawnu.

**Dokumenty projektu:**
- [Administracja VPS, aktualizacje, awarie i backupy](OPERACJE-VPS.md) — rzeczywiste ścieżki i komendy na Linuksie.
- [Checklista przed premierą](CHECKLISTA-STARTU.md) — potwierdzone testy i niewykonane zadania.
- [Historia rozwoju i starsze plany](HISTORIA-ROZWOJU.md) — archiwum poprzednich statusów, nie źródło informacji o bieżącym wdrożeniu.
- [Porządki w repo i uwagi do historycznych sekretów](REPO-PORZADKI.md) — materiał historyczny; produkcyjne konfiguracje i backupy pozostają poza Git.

### Potwierdzone testy i wdrożenia

| Obszar | Stan na 8.11.2026 |
| --- | --- |
| Państwa, rangi, rdzenie, claimy, GUI | Podstawowe scenariusze przetestowane. Edytor chunków i zatwierdzanie działały na środowisku testowym. |
| Oblężenia | Historyczna pierwsza seria **40/49 OK**, a końcowa seria **12/12 OK**, w tym 480 s, przejęcie rdzenia, dostęp do zdobytej bazy, upadek państwa i restart. Nie należy interpretować 9 pozostałych pozycji pierwszej serii jako 9 błędów. |
| PvP, Combat Tag i wojenny respawn | Sesja z 2 graczami na **1.8.13: 8/8 OK**, w tym wylogowanie podczas walki i pokój. |
| Shopkeepers | Plugin i podstawowy handel sprawdzone. Sklepy administracyjne na spawnie jeszcze niegotowe. |
| GrimAC / Better Anti-Dupe | Zainstalowane i sprawdzone podstawowo; brak gwarancji wykrywania wszystkich exploitów. |
| Paper Anti-Xray | **Włączony i sprawdzony** w Overworld i Netherze, `engine-mode: 2`. Baritone nie jest niezawodnie wykrywalny. |
| Portal polityczny | Publiczne HTTPS + strona + powiązanie/login działają po migracji. Szczegółowa weryfikacja izolacji graczy między państwami i API jeszcze otwarta. |
| VPS | Publiczne wejście Minecraft, systemd, SSH na klucz, konto `minecraft` i Caddy działają. |
| Backup | Pierwsza kopia Linux offline wykonana; systemd timer **07:00 Europe/Warsaw** aktywny, pierwsze archiwum pobrane na Windows i sprawdzone. Brak pełnego testu odtwarzania, brak automatycznego eksportu offsite. |
| Zegar | **Dzień 0** na nowej bazie. Uruchomienie tylko przez wyraźne `/esmp clock start` w dniu premiery. |

### Najważniejsze działania do premiery

1. Budowa fizycznego spawnu Makau, region WorldGuard i sklepy administracyjne Shopkeepers.
2. Próba odtworzenia pełnego backupu świata + SQLite w odseparowanym środowisku; zadbanie o regularną kopię poza VPS.
3. Test izolacji pozycji na WWW i surowych endpointów z dwoma państwami, próba Simple Voice Chat i wydajności przy większej liczbie znajomych.
4. Test koalicji trzech państw dopiero kiedy znajdzie się trzeci gracz, bez sztucznego blokowania przygotowań.
5. Po zakończeniu budowy: **PRE-LAUNCH** ze spawnem, nadal dzień 0; porządek w danych budowniczych; ogłoszenie daty i dopiero `/esmp clock start`.

**Zamrożenie funkcjonalne:** brak zgłoszonego krytycznego błędu wymagającego obecnie dopisania kolejnych funkcji. Dwa rzadkie ryzyka integracyjne pozostają w backlogu: zapis rdzenia w SQLite przed końcową finalizacją `BlockPlaceEvent` i wyjątkowy błąd SQLite przy rozliczaniu `/home`. Nie uznawać ich za potwierdzone exploity.

---

## 0. Skrót kluczowych reguł

- Każdy gracz należy do jednego państwa; nawet jednoosobowego. Granice polityczne to kolorowe, zatwierdzane przez administratora **chunki**, ale same granice nie chronią bloków. Zaznaczanie chunków **myszką na internetowej mapie (interakcja w stylu FTB Chunks)** działa od 1.8.5; komendy pozostają jako alternatywa. Użytkownik potwierdził, że zatwierdzone granice są odzwierciedlone w Minecrafcie.
- Każde państwo dostaje **1 darmowy rdzeń**, może wykonać **2 dodatkowe**; maksimum **3 rdzenie własne**. Rdzenie zdobyte w wojnach **nie liczą się do limitu**.
- Każdy rdzeń chroni **65 × 65 bloków**, w całej wysokości Overworldu (**Y −64…319**). Obszary ochronne nie mogą na siebie nachodzić, ale mogą sąsiadować. Nowy rdzeń można postawić tylko na zatwierdzonym własnym terytorium; zdobyty rdzeń jest wyjątkiem od wymogu stawiania.
- Formalna wojna pozwala na oblężenia między **10:00 a 00:00 Europe/Warsaw**, gdy online jest co najmniej **20% całego składu broniącego państwa** (zaokrąglenie w górę; min. 1). Spadek poniżej progu w trakcie daje **30 minut** na dokończenie; o północy koniec bez wyjątku.
- Atakujący w chronionej bazie mogą niszczyć **wyłącznie bloki postawione przez graczy**, specjalnym **Kilofem Oblężniczym**. Jeden kilof niszczy **15 bloków** i znika. Gdy podczas legalnego oblężenia wróg przebywa w chronionym regionie, **nikt nie może tam stawiać bloków**.
- Strefa przejmowania wokół rdzenia: **9 × 9 bloków**, Y rdzenia **−5…+10**. Wymagane **480 s**; atakujący bez obrońcy +1 s/s; obie strony w strefie: pauza; sami obrońcy: −2 s/s. Koniec oblężenia lub postęp 0 resetuje przypisanie zdobywcy; przerwanie oblężenia resetuje też cały postęp.
- Z państwa mającego **x rdzeni można zdobyć maksymalnie x−1**; ostatni rdzeń **znika**, a państwo **upada**. Całe jego terytorium staje się neutralne; granice później przydziela admin zgodnie z ustaleniami graczy.
- W czasie pokoju ochrona PvP we własnej bazie; na własnym terytorium politycznym po pierwszej próbie ataku **8 s obronnej nietykalności, cooldown 5 min**, pierwszy cios anulowany. W wojnie PvP działa normalnie; neutralne państwa zachowują pokojowe zabezpieczenia.
- Podczas wojny po kliknięciu **Respawn** rozpoczyna się widoczne **na ekranie (nie na czacie)** odliczanie zależne od członków **własnego państwa online**: 1–2 = 15 s; 3–5 = 20 s; 6–10 = 30 s; 11–15 = 40 s; 16+ = 50 s. Przez ten czas gracz nie może się poruszać, z niczym wchodzić w interakcje ani zostać zabity. Po zakończeniu odliczania natychmiast wraca zwykła rozgrywka, w tym możliwość zaatakowania go przez wrogów. W pokoju brak dodatkowego timera.
- Limity w **całym ekwipunku przez cały czas**: zwykłe złote jabłka 8; perły Endu 3; pajęczyny 16; totemy 3. Zaklęte złote jabłka, End Crystals i Respawn Anchors — całkowicie wyłączone. Po śmierci **cały ekwipunek wypada**.
- **Nether** dostępny od **06:00 (Europe/Warsaw) dnia serwera 10**; **End** wyłączony na stałe. Mapa **nie ma naturalnych wiosek**, dlatego pierwszych villagerów w praktyce zdobywa się przez **leczenie zombie villagerów**; następnie można je normalnie rozmnażać. **Nie jest to dodatkowy zakaz ani hardcoded reguła pluginu** — ograniczenie wynika z dostępnych źródeł mobów na mapie. Mending zakazany. Fortune: brak do dnia 6, I od dnia 7, III od dnia 16, Fortune II nigdy. Pozostałe enchanty dokładnie według reguł w 1.8.8, bez rozszerzania wyjątków QoL.
- Barter **przedmiot za przedmiot**, bez wirtualnej waluty. Gracze: sklepy skrzynkowe. Admin: nieskończone wymiany podstawowych materiałów (np. cobblestone ↔ cobbled deepslate 1:1). Docelowa mapa przeglądarkowa koloruje chunki państw, pokazuje **graczy tylko członkom tego samego państwa**, a **wszystkie postawione rdzenie publicznie** z ikoną lodestone i współrzędnymi.
- **Zegar edycji:** centralny numer dnia zmienia się codziennie o **06:00 czasu polskiego**. Docelowe `/home` działa także podczas wojny: dni 1–2 bez limitu, od dnia 3 maksymalnie **4 razy dziennie na gracza** (reset 06:00). Ręczna zmiana stolicy: **6 godzin cooldownu na państwo** (plan).
- **Interfejs 1.8.6:** `/esmp tierlist` — państwa sortowane według kontrolowanych postawionych rdzeni (w tym zdobycznych); indywidualny panel boczny serwera/państwa. Podstawowe działanie obu funkcji zostało potwierdzone w grze.

---

## 1. Świat, serwer i obecny stan techniczny

### 1.1. Środowisko

- **Bieżące środowisko VPS:** Paper 26.3, Eclipse Temurin Java 25.0.5, Ubuntu 24.04, usługa `earthsmp.service`, JVM `-Xms2G -Xmx8G`. Poprzednie lokalne środowisko testowe to Windows ze skryptem `start.bat`.
- Mapa **Earth Diamond Edition — No Caves / No Structures**, skala około **1:1500**, z zachowanymi naturalnymi złożami, biomami i spawnami mobów; brak rozbudowanych jaskiń, mogą występować geody i małe naturalne pustki.
- Mapa przycięta w MCA Selector do około **6144 × 6656 bloków**; orientacyjny zachowany zakres X **4608…10752**, Z **−3584…3072**.
- Worldborder: środek **(7680, −256)**, średnica **5120** bloków (grywalny obszar X 5120…10240 i Z −2816…2304). Teren wykraczający poza border pozostaje jako margines widokowy.
- **WorldBorder na własnym portalu mapy:** wdrożony w **1.8.10**. Autorski portal WWW rysuje publiczny obrys z bieżącej konfiguracji Paper; można go przełączać w warstwach. Użytkownik potwierdził działanie 1.8.10; sprawdzenie obrysu na różnych zoomach i zmianach konfiguracji pozostaje częścią testów końcowych.
- **Spawn: Makau (Macao)**. W kodzie autorskiej mapy WWW (`portal.js`) publiczny znacznik spawnu to **sam domek bez kółka i stałego podpisu**, pozycjonowany na podstawie `details.spawn.x/z` z squaremap. Upewnić się podczas testu, że te współrzędne wskazują rzeczywisty spawn Makau. Fizyczna budowa i ochrona spawnu, działek sklepowych oraz terenów publicznych pozostaje do wykonania.
- Główne ustawienia: `difficulty=hard`, `enforce-whitelist=true`, `generate-structures=false`, `spawn-protection=0` (ochrona pluginem), `online-mode=true`, whitelist, view-distance około 10; `respawn_radius=0`, `players_sleeping_percentage=50`.
- Planowanych dwóch administratorów z pełną kontrolą nad państwami, terytoriami, rdzeniami i wyjątkowymi sytuacjami.

### 1.2. Backupy i stan pluginu

**Bieżące backupy Ubuntu opisano w [OPERACJE-VPS.md](OPERACJE-VPS.md). Poniższe informacje o skrypcie PowerShell i kopiach z Windows są historyczne.**

- Aktualny skrypt **`EarthSMP-Backup.ps1`** tworzy backup **offline**: należy zatrzymać Paper (`stop`), następnie uruchomić skrypt z `-ConfirmStopped`. Sprawdza on m.in. port, blokadę świata, obecność `level.dat` oraz **`plugins/EarthSMPCore/earthsmp.db`**. Do ZIP trafiają świat, baza SQLite, dane pluginów (z wyłączeniem dużych odtwarzalnych danych squaremap) i konfiguracja. Nie należy traktować go jako działającego backupu online przez RCON.
- Historyczny test przywracania wcześniejszej kopii świata: około **427,4 MB**, **14,6 s**. **Nie potwierdza on jeszcze przywracania nowego ZIP z SQLite**; test odtworzenia świata razem z bazą pozostaje do wykonania.
- Harmonogram backupu co 2 godziny był przetestowany, a potem usunięty na czas testowania; backupy automatyczne i retencja na docelowym hostingu są osobnym zadaniem.
- **Aktualizacja 2026-11-08:** `main` zawiera **1.8.13**, działa na VPS. PvP i Combat Log mają potwierdzone 8/8 OK, oblężenia i zdobywanie rdzeni były już testowane. Pełne odtworzenie produkcyjnego archiwum oraz test koalicji 3 państw pozostają otwarte.

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
- Jeden z aktualnie posiadanych rdzeni państwa jest wyznaczony jako **stolica** (gdy istnieje). **Stan 1.8.1:** po utracie stolicy system automatycznie wybiera nową spośród pozostałych rdzeni. Lider lub zastępca może ręcznie wskazać inny własny postawiony rdzeń **podczas pokoju**; aktywnej stolicy nie można podnieść, dopóki nie wyznaczy się następnej. Admin może wykonać korektę ręcznie. **Wdrożone od 1.8.2:** ręczna zmiana stolicy ma **6 godzin cooldownu na całe państwo** (wspólny dla lidera i zastępców), bez ograniczania automatycznej sukcesji i korekt admina. Oznaczenie stolicy nie będzie publicznie wyróżniane na mapie.

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
- **Implementacja od 1.8.8 (przetestowano podstawowe zachowania, nie wszystkie graniczne):** kontrole podnoszenia, przenoszenia ze skrzyń, craftingu, Creative, drag, kliknięć wielokrotnych i okresowe przeglądy ekwipunku. W przypadkach nietypowych nadwyżka jest **odkładana na ziemię**, nie usuwana. Przed akceptacją wymagane testy zabezpieczeń, zwłaszcza z innymi pluginami sklepów i niestandardowymi interfejsami. Produkcyjny sposób obsługi nieprzewidzianych operacji do potwierdzenia.
- End jest niedostępny, więc nie przewidujemy naturalnego źródła Shulker Boxów ani osobnego systemu wyjątków; administracja nie powinna dystrybuować ich wbrew tej zasadzie.
- Pozostałe odziedziczone zasady PvP z dawnego regulaminu opisano w sekcji 14.

## 11. Zaklęcia (enchanty) i progresja

- **Nie ma naturalnych wiosek na mapie.** Leczenie zombie villagerów to praktyczna droga pozyskania pierwszych villagerów, po czym można je rozmnażać normalnie. **Nie wprowadzamy osobnego wymogu prawnego ani kontroli pochodzenia pierwszych dwóch villagerów w pluginie**; nie ma adminowskiego sklepu z enchantami. **Sprawdzenie poprawności oferowanych i kupowanych enchantów** (np. książek i gotowych zbroi) to odrębny test bezpieczeństwa handlu, jeszcze niepotwierdzony.
- Ograniczenia obowiązują **na gotowym przedmiocie** i muszą obejmować łączenie książek w kowadle, oferty handlowe oraz inne potencjalne źródła. Nie wolno obejść progów przez łączenie.
- **Mending** — zabroniony przez całą edycję.
- **Implementacja 1.8.8:** blokuje niedozwolone wyniki kowadła i craftingu, nowe nielegalne oferty villagerów, niedozwolone enchanty przy enchantowaniu oraz efekt Mending. Istniejące przedmioty z niedozwolonymi enchantami są automatycznie **normalizowane** (enchant usuwany lub obniżany; przedmiot pozostaje). Dopuszczona lista wyjątków QoL jest ścisła: Unbreaking III, Efficiency V, Looting III, Silk Touch I, Aqua Affinity I, Respiration III. **Wszystkie inne enchanty pozostają ograniczone tak, jak w kodzie 1.8.8** (w tym mobilność), bez dodatkowych wyjątków. Testy handlu i kowadła nadal do wykonania.
- **Fortune II** — zabroniony przez całą edycję, nawet gdy Fortune I i III są dopuszczone.
- **Fortune I** — dostępny od 7. dnia; **Fortune III** — od 16. dnia; przed dniem 7 Fortune niedostępny.
- **Enchantmenty dające przewagę w PvP** (np. Protection, Sharpness, Power, Knockback, Fire Aspect, Thorns, Piercing oraz analogiczne) — maksymalnie **poziom I przez pierwsze 9 dni edycji**, a **poziom II od 10. dnia do końca** (zaklęcia o naturalnym maks. I pozostają na I). **Wyjątki dla kuszy** (Multishot i Quick Charge) określa §11.2; **Unbreaking III wolno zawsze na wszystkich przedmiotach, także na kuszy**, chyba że zostanie wyraźnie postanowione inaczej.
- **Zatwierdzone wyjątki QoL:** **Unbreaking III na wszystkich przedmiotach, w tym na kuszach, w każdej fazie edycji** (chyba że administracja wyraźnie ustali wyjątek), Efficiency V, Looting III, Silk Touch I, Aqua Affinity I i Respiration III. Pozostałe enchanty, również wpływające na mobilność, podlegają dotychczasowym limitom; nie dodajemy innych wyjątków.
- Nie ma mechaniki stopniowego odblokowywania wszystkich enchantów lub płatnego adminowskiego sklepu; jedyne fazy to zatwierdzone limity PvP i harmonogram Fortune.

### 11.1. Oś czasu

| Moment edycji | Zmiana |
|---|---|
| Dni 0–6 | Fortune niedostępny; standardowe enchanty maks. I; Quick Charge II; wyjątki QoL według §11.3 |
| Dni 7–9 | Fortune I; standardowe enchanty maks. I; Quick Charge II |
| Od 10. dnia, 06:00 PL | Standardowe enchanty maks. II; Quick Charge III |
| Dzień 10, godz. 06:00 PL | **Otwarcie Netheru** |
| Dni 10–15 | Fortune I |
| Od 16. dnia, 06:00 PL | Fortune I albo III; Fortune II niedostępny |
| Cała edycja | Mending zakazany; End zamknięty |

### 11.2. Kusze — wyjątki zatwierdzone 7 listopada 2026 (wdrożone w 1.8.13)

Reguły w tej sekcji dotyczą **zaklęć na kuszy**. **Unbreaking III pozostaje globalnie dozwolony** (§11); jedynymi nowymi wyjątkami od ogólnego limitu są Multishot i Quick Charge. Rozróżnienie faz następuje według centralnego zegara edycji (zmiana dnia o 06:00 `Europe/Warsaw`).

| Zaklęcie na kuszy | Faza limitu I (dni 0–9) | Faza limitu II (od dnia 10) |
| --- | --- | --- |
| **Multishot** | **Zabroniony** | **Zabroniony** |
| **Quick Charge** | Maks. **II** | Maks. **III** |
| **Piercing** | Maks. **I** | Maks. **II** |
| **Unbreaking na kuszy** | Maks. **III** | Maks. **III** |
| **Inne zaklęcia na kuszy**, jeżeli przedmiot może je legalnie posiadać | Maks. **I** | Maks. **II** |

- **Mending pozostaje zabroniony** na wszystkich przedmiotach, w tym na kuszach.
- **Unbreaking III jest dozwolony zawsze i na każdym przedmiocie, również na kuszy**; odstępstwo wyłącznie po nowym, jednoznacznym ustaleniu administracyjnym.
- Zasada obejmuje **gotowe kusze** zdobyte przez handel z villagerami, loot, komendy, kowadło i przedmioty wcześniej pozyskane, nie tylko samo enchantowanie. Nie wolno zachować nielegalnego poziomu przez zdobycie gotowego przedmiotu.
- **Status techniczny 1.8.13:** Multishot zabroniony; Quick Charge II przed dniem 10 i III od dnia 10; Piercing I/II zgodnie z fazą; Unbreaking III zawsze dozwolony. Kod wdrożony i zbudowany. Testy graniczne z gotowymi przedmiotami, kowadłem i handlem nie mają osobnego kompletnego protokołu.

### 11.3. Pełna tabela limitów zaklęć — stan ustaleń na 7 listopada 2026

**Pierwszy próg** to dni **0–9** (przed dniem 10), a **drugi próg** zaczyna się **od 06:00 dnia 10** (`Europe/Warsaw`). Wartości to **maksymalny dozwolony poziom na gotowym przedmiocie**, nie poziom gwarantowany przez enchanting/handel. Naturalny maksymalny poziom w Minecraft zawsze obowiązuje, nawet jeżeli w tabeli widnieje „II” dla enchantu mającego tylko poziom I. **Fortune ma dodatkowe, niezależne progi dnia 7 i 16.**

| Enchant | Pierwszy próg (dni 0–9) | Drugi próg (od dnia 10) |
|---|---|---|
| **Mending** | **Zakazany** | **Zakazany** |
| **Multishot (kusza)** | **Zakazany** | **Zakazany** |
| **Fortune** | Dni 0–6: **zakaz**; dni 7–9: **I** | Dni 10–15: **I**; od dnia 16: **I lub III** (**II zakazane zawsze**) |
| **Quick Charge (kusza)** | **II** | **III** |
| **Piercing (kusza)** | **I** | **II** |
| **Unbreaking (wszystkie przedmioty, w tym kusza)** | **III** | **III** |
| Efficiency | **V** | **V** |
| Looting | **III** | **III** |
| Silk Touch | **I** | **I** |
| Aqua Affinity | **I** | **I** |
| Respiration | **III** | **III** |
| Protection | **I** | **II** |
| Fire Protection | **I** | **II** |
| Blast Protection | **I** | **II** |
| Projectile Protection | **I** | **II** |
| Feather Falling | **I** | **II** |
| Thorns | **I** | **II** |
| Depth Strider | **I** | **II** |
| Frost Walker | **I** | **II** |
| Soul Speed | **I** | **II** |
| Swift Sneak | **I** | **II** |
| Sharpness | **I** | **II** |
| Smite | **I** | **II** |
| Bane of Arthropods | **I** | **II** |
| Knockback | **I** | **II** |
| Fire Aspect | **I** | **II** |
| Sweeping Edge | **I** | **II** |
| Power | **I** | **II** |
| Punch | **I** | **II** |
| Flame | **I** (naturalny maks.) | **I** (naturalny maks.) |
| Infinity | **I** (naturalny maks.) | **I** (naturalny maks.) |
| Impaling | **I** | **II** |
| Loyalty | **I** | **II** |
| Riptide | **I** | **II** |
| Channeling | **I** (naturalny maks.) | **I** (naturalny maks.) |
| Luck of the Sea | **I** | **II** |
| Lure | **I** | **II** |
| Density | **I** | **II** |
| Breach | **I** | **II** |
| Wind Burst | **I** | **II** |
| Curse of Binding | **I** (naturalny maks.) | **I** (naturalny maks.) |
| Curse of Vanishing | **I** (naturalny maks.) | **I** (naturalny maks.) |
| **Pozostałe/nowe enchanty bez osobnej reguły** | **I** (nie wyżej niż naturalny maks.) | **II** (nie wyżej niż naturalny maks.) |

**Zasady odczytu tabeli:** zakaz i wyjątki (`Mending`, `Multishot`, `Fortune`, `Quick Charge`, `Unbreaking` i wymienione QoL) mają pierwszeństwo przed zasadą I/II; wszystkie pozostałe enchanty ograniczane są standardowym limitem I/II. Zaklęcia nie mogą trafić na przedmioty, na których są normalnie niedozwolone. Ograniczenia dotyczą również przedmiotów z ofert villagerów (także gotowej zbroi z Protection IV), książek, kowadła, łupów i przedmiotów wprowadzonych komendami.

**Status kodu:** tabela opisuje **zatwierdzone zasady**, a nie potwierdzenie ich pełnego działania. W `1.8.8` nadal **brakuje blokady Multishot i wyjątkowego limitu Quick Charge II/III**; te dwie zmiany kodu i testy na Paper są zaplanowane na kolejną sesję. Pozostałe wpisy odzwierciedlają reguły z `EditionEnchantRules` (naturalny maksymalny poziom uwzględnia dodatkowo `EditionEnchantListener`).

## 12. Nether, End, eksplozje

- **Nether pozostaje zablokowany do godziny 05:59:59 dnia 10; otwiera się o 06:00 (Europe/Warsaw) dnia serwera 10**. To ostateczna decyzja administracyjna, nadrzędna wobec dawnych sformułowań „po 10 pełnych dniach”.
- **End jest na stałe wyłączony**. W konsekwencji brak naturalnego dostępu do shulkerów/Shulker Boxów.
- **Implementacja testowa 1.8.8:** blokuje portale i teleportacje, przekierowuje respawn oraz wejście gracza z niedostępnego wymiaru na Overworld. Parametr `progression.nether-unlock-day: 10` jest **zatwierdzony**; nie zmieniamy go na 11. Pełny test granicznej godziny nadal do przeprowadzenia.
- W Netherze **można wysadzać łóżka w celu wydobywania ancient debris**. **Nie wolno używać eksplozji łóżek do PvP.** Jest to **zasada pisemna**, nie osobna blokada kodowa.
- **TNT Minecart PvP** jest zakazane **regulaminowo**, w tym pułapki z wagonikami TNT. Zwykłe TNT może służyć do prac budowlanych/górnictwa, ale nie wolno nim obchodzić ochrony baz.
- **Portal trapping** (celowe blokowanie gracza w przejściu przy zmianie wymiaru) jest zabroniony regulaminowo, także podczas wojny. Normalne zasadzki w pobliżu portali nie są tym samym.
- End Crystals i Respawn Anchors są niedostępne w całości (silniejsza zasada niż stary zakaz wyłącznie ich PvP).

## 13. Gospodarka i handel

- **Wyłącznie barter** — wymiana przedmiot za przedmiot, **bez wirtualnej waluty, kont i banków**.
- **Shopkeepers — przetestowany i działający** (potwierdzenie użytkownika 7.11.2026). Używamy barteru przedmiot za przedmiot, bez wirtualnej waluty; nie tworzymy własnego pluginu sklepowego. Sklep gracza działa na rzeczywistym towarze w przypisanej skrzyni lub magazynie, a sprzedaż kończy się po wyczerpaniu zapasów. Do wykonania pozostaje ustawienie NPC i konfiguracja asortymentu sklepów **administracyjnych na spawnie w Makau** (punkt 5 / spawn); testy odporności na dupe i obciążenie pozostają częścią próby generalnej, a nie niedokończoną implementacją Shopkeepers.
- **Makau / spawn:** przed startem trzeba zbudować czytelny i estetyczny punkt startowy z placem, informacjami, zasadami i sklepami administracyjnymi. Administracja może również udostępniać chronione działki handlowe, ale **sklepy graczy mogą stać w dowolnym legalnym miejscu**, także we własnych miastach i bazach. Ochronę spawnu, NPC i magazynów trzeba przetestować z WorldGuard/EarthSMP.
- Sklepy administracyjne mają **nieskończone zapasy** i realizują wyłącznie podstawowe wymiany materiałów budowlanych, np. **1 cobblestone ↔ 1 cobbled deepslate**, obustronnie 1:1.
- Admin nie sprzedaje w nieskończoność zasobów strategicznych typu diamenty czy netherite. Szczegółową listę wymian ustala administracja w trakcie edycji.
- **Sklepy w stylu Rust — zatwierdzony kierunek:** gracz może trzymać skrzynię z towarem **wewnątrz chronionej bazy**, a obsługującego ją NPC Shopkeepers (np. villager) wystawić **na zewnątrz**, żeby inni mogli handlować bez dostępu do skrzyni. Historyczna lokalna konfiguracja Shopkeepers zawierała `max-container-distance: 15`, `max-shops-per-player: -1`, `max-containers-per-player-shop: 9`; **wartości aktualnej konfiguracji i możliwość powiązania przez ścianę trzeba sprawdzić w grze**. Nie mylić NPC Shopkeepers z naturalnym villagerem używanym do handlu enchantami.
- **Przed startem:** sprawdzić obecną wersję Shopkeepers i poprawki bezpieczeństwa (szczególnie potencjalne duplikacje w transakcjach), ochronę skrzyni przed obcym otwarciem/zniszczeniem, zachowanie NPC przy oblężeniu, brak dostępu do zapasów z zewnątrz oraz równoczesne zakupy. Sklepy administracyjne muszą realizować planowany **barter z nieskończonymi podstawowymi materiałami** — potwierdzić obsługę tej funkcji przez aktualny plugin/konfigurację przed wydaniem.

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

## 15. Teleportacja, stolice i centralny zegar edycji — wdrożone od 1.8.2

### 15.1. Jeden zegar dla całej edycji

- Plugin ma utrzymywać **centralny numer dnia serwera**: dzień **1** zaczyna się w chwili oficjalnego uruchomienia edycji; kolejne dni zaczynają się **codziennie o 06:00 czasu polskiego**, w strefie `Europe/Warsaw` (z uwzględnieniem zmian czasu letniego/zimowego).
- Są to **dni kalendarzowe serwera z granicą o 06:00**, a nie kolejne 24-godzinne okna liczone od startu. Gdy edycja rozpocznie się wieczorem, dzień 1 będzie krótszy. Przestój Paper nie zatrzymuje upływu dni — po restarcie numer dnia ma być obliczany z daty startu i bieżącej daty w przyjętej strefie.
- Data/godzina początku edycji powinna być utrwalona trwale (np. SQLite) i nie zmieniać się wraz z restartami ani logowaniem poszczególnych graczy.
- Ten sam zegar jest podstawą **wszystkich mechanik opartych na numerze dnia**: limitów `/home`, odblokowania wymiarów, etapów zaklęć, przyszłych wydarzeń i podobnych reguł. Istniejące szczegółowe progi opisują rozdziały 11–12.
- Zatwierdzona **administracyjna komenda `/esmp clock`** ma pokazywać aktualny dzień serwera, datę/godzinę rozpoczęcia edycji oraz czas do kolejnej zmiany dnia o 06:00. **Wdrożona od 1.8.2** (historycznie nieobecna w 1.8.1).

### 15.2. `/home`: także podczas wojny

- `/home` teleportuje gracza **wyłącznie w pobliże rdzenia będącego aktualną stolicą jego państwa**. Pozostałe rdzenie nie są dowolnymi punktami szybkiej podróży. Sposób wyznaczania bezpiecznego miejsca docelowego jest szczegółem technicznym.
- **Dozwolone zarówno w pokoju, jak i podczas wojny.** Poprzednia zasada całkowitej blokady `/home` podczas wojny **została uchylona**.
- **Dni serwera 1–2:** `/home` jest **nielimitowane** dla wszystkich graczy, by ułatwić start.
- **Od dnia serwera 3:** każdy gracz ma **4 udane teleportacje na dzień serwera**, niezależnie od wojny i od tego, jak długo jest na serwerze. Limit jest **na gracza**, nie na państwo.
- **Reset dokładnie o 06:00 Europe/Warsaw**, przy przeskoku centralnego dnia serwera. To nie jest przesuwające się 24-godzinne okno. Akceptujemy możliwość użycia **4 teleportacji tuż przed 06:00 i 4 tuż po 06:00**.
- Użycie jest pobierane **dopiero po pomyślnej teleportacji**; nieudane wywołanie nie zużywa limitu. Dane limitów pozostają w SQLite i nie resetują się przy restarcie lub wylogowaniu.
- Informacja o pozostałych użyciach i następnym resecie powinna być czytelna dla gracza (komunikat i docelowo sidebar). W dniach 1–2 interfejs pokazuje `∞`.
- **WDROŻONE — 30 sekund przygotowania `/home` (1.8.12–1.8.13; podstawowy test w grze potwierdzony):** po poprawnej weryfikacji możliwości użycia `/home` teleportacja **nie wykonuje się natychmiast**. Uruchamia się 30-sekundowe odliczanie na środku ekranu przez natywny Minecraft **Title (`/title`, Adventure `player.showTitle`)**, analogicznie do odliczania po wojennym respawnie. **Nie używać BossBara ani ActionBara do licznika.** Duża liczba sekund znajduje się w tytule, a informacja o anulowaniu w podtytule, z czytelną informacją: **„Nie ruszaj się i nie otrzymuj obrażeń — inaczej teleportacja zostanie anulowana”**. Pokazywać pozostały czas i wynik (sukces/anulowano), bez zalewania czatu wiadomościami co sekundę.
- **Anulowanie przygotowania:** każda rzeczywista zmiana pozycji gracza (współrzędne X/Y/Z; sam obrót kamery bez przemieszczenia nie liczy się jako ruch) albo otrzymanie obrażeń przerywa licznik. Anulowanie ma być natychmiastowe i usuwać ekranowy licznik. Dodatkowo w implementacji bezpiecznie sprzątać oczekiwanie po wylogowaniu, śmierci, zmianie świata, innym teleportowaniu i wyłączeniu pluginu; ponowne `/home` w czasie odliczania nie może tworzyć równoległych zadań.
- **Limit i walidacja:** anulowane, nieskuteczne i niedokończone przygotowanie **nie zużywa** dziennego użycia. Limit należy rozliczać dopiero po faktycznie udanej teleportacji; w chwili zakończenia odliczania ponownie sprawdzić stan gracza, przynależność do państwa, aktualną stolicę, dostępność bezpiecznej pozycji i limit względem bieżącego dnia serwera (także przy zmianie dnia o 06:00). Czas 30 s dotyczy również dni 1–2 i okresu wojny — **nie zmienia** dotychczasowych uprawnień ani liczby dozwolonych użyć.
- **Status wdrożenia:** na `main` działa 30-sekundowy warmup przez natywny Title z anulowaniem przy rzeczywistym ruchu, obrażeniach, innej teleportacji i wylogowaniu. Stan jest ponownie sprawdzany przed teleportacją; dopiero udana teleportacja pobiera użycie. Potwierdzony build i podstawowe działanie. **Otwarte testy:** granica 06:00, awarie SQLite przy zapisie licznika po teleportacji, teleporter innego pluginu, restart podczas odliczania i odporność na obchodzenie limitu. Respawn wojenny i jego Title nie są zastępowane przez powiadomienia eventowe.

### 15.3. Zmiana stolicy i cooldown 6 godzin

- **Od 1.8.1:** lider i zastępca mogą podczas pokoju przeznaczyć inny, już postawiony własny rdzeń na stolicę. W czasie wojny ręczna zmiana jest zabroniona. Wyznaczenie nowej stolicy pozwala spakować poprzednią (o ile jest własna, nie zdobyczna).
- **Ograniczenie wdrożone od 1.8.2:** po udanej ręcznej zmianie stolicy obowiązuje **6 godzin rzeczywistego cooldownu dla całego państwa**, a nie osobno dla graczy. Obejmuje to także zmianę wykonaną naprzemiennie przez lidera i zastępcę.
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
- **Stan od 1.8.5:** kolorowe chunki squaremap, publiczne rdzenie i prywatne pozycje są dostępne przez portal `:8766`; **edytor chunków WWW z wnioskami i administracyjnym zatwierdzaniem został wdrożony**. Użytkownik potwierdził zgodność granic z Minecraftem. Nadal brakuje publicznego HTTPS i pełnych testów wieloosobowych. squaremap `:8080` pozostaje prywatnym upstreamem kafelków/warstw.
- Używamy **squaremap** do istniejącej warstwy politycznej. Prywatne pozycje graczy wymagają uwierzytelnienia i filtrowania danych na backendzie; **publiczne rdzenie są osobną, jawną warstwą**. Obie należy aktualizować po zmianach właścicieli, zakładaniu, podnoszeniu, zdobyciu i likwidacji rdzeni.

### 16.1. Edytor terytoriów na mapie — wdrożony od 1.8.5, wymagane testy multiplayer

**Cel:** interakcja z chunkami **jak w FTB Chunks**, ale na **internetowej mapie squaremap**. To istotne doprecyzowanie pierwotnego pomysłu na „claimowanie” — chodzi o łatwe, wizualne zaznaczanie chunków, **nie o przeniesienie ochrony baz z rdzeni na claimy**.

**Docelowy przebieg dla gracza:**

1. Gracz otwiera portal WWW i wchodzi w **tryb edycji terytorium** po zalogowaniu istniejącym kontem mapy. Powiązanie z Minecraftem jest jednorazowe przez `/esmp map code`, później wystarcza nick i hasło (od 1.8.4).
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
- **Stan od 1.8.5:** publiczna mapa polityczna, interaktywny edytor chunków, mapowe wnioski oraz panel admina WWW są zaimplementowane i przeszły test użytkownika z potwierdzeniem efektu w Minecrafcie. Obsługa konfliktujących wniosków wymaga testów z wieloma graczami.
- **WDROŻONE UX EDYTORA (7.11.2026), kod na `main` 1.8.13, podstawowy test użytkownika:** na komputerze **LPM = zaznaczanie chunków** (również przeciąganiem), **PPM = wymazywanie zaznaczenia**, **przytrzymana ŚPM = przesuwanie mapy**, kółko = zoom. **Nie przełączać trybu zaznaczania/przesuwania przyciskami na desktopie.** Programowa obsługa środkowego przycisku nie może uruchamiać auto-scrollu przeglądarki ani tworzyć przypadkowych zaznaczeń. Na telefonie / urządzeniu dotykowym **pozostają przyciski trybów** (zaznacz, wymaż, przesuń); pinch zoom bez zmian. Warto jeszcze zweryfikować działanie na telefonie, touchpadzie i różnych przeglądarkach.

### 16.2. Publiczne znaczniki rdzeni — wdrożone w 1.8.3

- Na mapie WWW każdy obserwator, **również niezalogowany, z innego państwa, sojusznik lub wróg**, widzi **wszystkie postawione rdzenie**. Nie ma ukrywania według dyplomacji ani granic politycznych.
- Znak graficzny to **lodestone**. **Nad nim koordynaty `X`, `Y`, `Z`** rzeczywistego bloku rdzenia. Nie podajemy numeru („rdzeń 1/2”), nazwy, właściciela ani statusu **STOLICA** przy znaczniku. Polityczne kolory terytoriów są osobną warstwą.
- Po postawieniu lub przeniesieniu rdzenia marker pojawia się/zmienia lokalizację; po zdobyciu pozostaje w tym samym miejscu (bez publicznego oznaczania zmiany właściciela); po demontażu, zniszczeniu ostatniego rdzenia lub rozwiązaniu państwa znika. Dane synchronizowane z zatwierdzonym stanem świata/SQLite.
- **Publiczność znaczników rdzeni NIE oznacza upublicznienia pozycji graczy**. Pozycje graczy nadal widoczne tylko dla członków ich własnego państwa (oraz samego gracza), z wykluczeniem sojuszników, wrogów i publicznej mapy.
- **Stan od 1.8.3:** znaczniki są publikowane przez squaremap, użytkownik potwierdził działanie na serwerze testowym. Komenda administracyjna: `/esmp core maprefresh`.


**WDROŻONE w 1.8.13 — optymalizacja ikon rdzeni squaremap:** `SquaremapCoreLayer.refresh()` porównuje fingerprint identyfikatorów oraz współrzędnych XYZ. Przy niezmienionym zestawie i poprawnie istniejącej warstwie pomija generowanie ikon i log INFO. Pełny refresh następuje na początku, po rzeczywistej zmianie rdzeni oraz w razie brakującej warstwy; kontrola samonaprawy działa **co 10 minut** zamiast co minutę. `/esmp core maprefresh` wymusza odświeżenie przez `refreshCoreMap(true)`.

- **Stan:** kompilacja i podstawowe działanie 1.8.13 potwierdzone. Należy dodatkowo sprawdzić restart, opóźniony start squaremap, większą liczbę markerów, ich usunięcie po upadku/rozwiązaniu państwa, ręczny force-refresh i koszt aktualizacji SQLite.

### 16.3. `/esmp tierlist` — ranking państw (końcowy etap GUI)

- Komenda otwiera **minecraftowe GUI** wszystkich istniejących państw, posortowanych **malejąco według łącznej liczby ich aktualnie postawionych rdzeni**; liczą się **własne i zdobyczne**, lecz nie rdzenie-przedmioty w ekwipunku czy magazynie. Remisy mogą mieć stabilny porządek alfabetyczny (detal techniczny).
- Każde państwo ma ikonę (np. baner/ikona kraju); po **najechaniu kursorem** gracz widzi tooltip: **nazwa państwa, liczba jego członków i liczba kontrolowanych rdzeni**. Liczba członków online może być dodatkową przydatną informacją, jeśli nie będzie zaśmiecać interfejsu.
- Ranking jest czytany z aktualnego stanu państw i rdzeni, a nie ręcznie wprowadzany; po zdobyciu lub utracie rdzenia kolejność się zmienia.
- **Status 1.8.6:** zaimplementowany jako paginowany minecraftowy GUI (45 państw na stronę), dostępny z `/esmp tierlist` i centrum `/esmp`; liczniki pobierane na bieżąco z SQLite. **Użytkownik potwierdził działanie w grze**; pozostaje test rankingu po zdobyciu i utracie rdzeni w kilku państwach.

### 16.4. Sidebar / scoreboard — informacja widoczna podczas gry (końcowy etap GUI)

- Po **prawej stronie ekranu**, w standardowym minecraftowym scoreboard sidebar, pokazujemy **aktualny dzień edycji**, **nazwę własnego państwa**, **łączną liczbę jego kontrolowanych rdzeni (własne + zdobyczne)**.
- Dodatkowe przydatne informacje 24/7: **liczba członków państwa online / łącznie**, **status pokój/wojna** oraz **pozostały limit `/home`** (`∞` w dniach 1–2; od dnia 3 np. `2/4`). To docelowy, zwięzły układ; kolejność i wygląd zostaną dopracowane w ostatniej fazie.
- Gracz bez państwa ma widzieć czytelne `Brak państwa` i sensowną informację o stanie serwera. Dane powinny być **indywidualne dla oglądającego**, aktualizowane po zmianach państwa, rdzeni, wojny i dnia serwera.
- **Od 1.8.6:** `/esmp sidebar` pozwala wyłączyć lub włączyć HUD; preferencja UUID jest zapamiętywana po restarcie. To wygodna opcja techniczna, nie dodatkowe ograniczenie zasad serwera.
- **Status 1.8.6:** funkcja zaimplementowana, a **użytkownik potwierdził jej podstawowe działanie w grze**. Odświeżanie co 2 s na głównym wątku Paper. Nadal wymaga próby z wieloma graczami, zmianami członkostwa, wojną, restartem i innymi pluginami scoreboard.

### 16.5. Instalacja klienta — brak obowiązkowych modów i wizarda

- EarthSMP pozostaje **grywalny na czystym kliencie Minecraft Java**. Do claimowania przez mapę WWW **nie potrzeba moda klienta**.
- Własny instalator typu **wizard (Windows/C#) nie jest obecnie planowany jako wymagany element wdrożenia**. Rezygnujemy z przymusowej instalacji dodatkowego oprogramowania.
- Opcjonalnie opublikujemy **paczkę modów graficznych, wydajnościowych i QoL** oraz **krótki poradnik instalacji** dla zainteresowanych.
- Mody nie mogą być warunkiem udziału w rozgrywce ani dostępu do mechanik państwowych.

### 16.6. Portal WWW EarthSMP — konto na całą edycję i prywatne pozycje (1.8.4 R3)

- **Jedyny docelowy interfejs dla graczy:** własny portal EarthSMP, a nie publiczna strona oryginalnej squaremap. Gracze otrzymają docelowo jeden stabilny link HTTPS; planowana komenda `/esmp map` będzie podawała klikalny adres. W 1.8.4 działają podkomendy `code`, `revoke`, `reset`, natomiast **samo wyświetlanie klikalnego linku przez `/esmp map` jest jeszcze planem**.
- **Jednorazowe powiązanie:** `/esmp map code` generuje kod ważny 5 minut, używany wyłącznie do pierwszego założenia konta na portalu i ustawienia własnego hasła (12–128 znaków). Później gracz loguje się **nickiem Minecraft i hasłem** również spoza domu i bez wchodzenia do gry.
- **Trwałość:** konta nie wygasają automatycznie przed zakończeniem edycji. Sesje przeglądarki do **30 dni**, ponowne logowanie bez kodu z gry, niezależnie od restartów Paper. `/esmp map revoke` wylogowuje wszystkie urządzenia, `/esmp map reset` usuwa konto i sesje, pozwalając ponownie powiązać konto. Tabele kont/sesji w **SQLite schema v9**, bez publikowania haseł (przechowywane są hashe).
- **Prywatność:** backend wylicza uprawnienia z **aktualnego członkostwa w państwie**, nawet gdy oglądający jest offline. Udostępnia jedynie aktualnie zalogowanych graczy tego samego państwa (oraz samego siebie); nie pokazuje obcych państw ani sojuszników, a Nether i End są ukryte. Pozycje odświeżane na bieżąco. **Przed publicznym uruchomieniem konieczny test dwóch państw i bezpośrednich zapytań HTTP bez zalogowania/po zmianie państwa**.
- **Przeglądarkowe GUI:** publiczne kafelki, kolory terytoriów i publiczne rdzenie są widoczne bez konta; prywatne pozycje wymagają konta. R3 naprawia układ kafelków i uszkodzone obrazki przy pustych kafelkach, dodaje odświeżanie.
- **Architektura obecnej wersji:** portal jest serwerem HTTP wewnątrz pluginu Paper, domyślnie `127.0.0.1:8766`. Publiczne kafelki i znaczniki pobiera z **wewnętrznego squaremap `127.0.0.1:8080`**. **Nie usuwać ani nie wyłączać backendu 8080 w tej wersji.** Portu 8080 nie trzeba jednak podawać graczom ani udostępniać z Internetu.
- **Dostęp zewnętrzny (stan 1.8.13):** portal działa przez Caddy i prawidłowy HTTPS bez jawnego portu 443. Kontrole `Host`/`Origin`, sesje oraz ciasteczka są zaimplementowane, a stronę i logowanie sprawdzono po migracji. Do zrobienia pozostaje weryfikacja prywatności surowych endpointów dla członków różnych państw oraz testy graniczne sesji. Historyczne uwagi o błędach wersji 1.8.4 nie obowiązują obecnego wdrożenia.

## 17. Administracja i wyjątkowe przypadki

- Administratorzy mają **pełną kontrolę nad wszystkimi państwami, rdzeniami, członkostwem, wojnami, granicami i ochroną**. Błędy lub nietypowe zakończenia konfliktów można ręcznie naprawić.
- W trakcie wojny dobrowolne rozwiązanie państwa jest zablokowane. Jeśli państwo **przymusowo upada** po utracie ostatniego rdzenia, likwidację równoległych wojen i relacji koalicyjnych może przeprowadzić administrator.
- **Stan 1.8.4 R3:** zdobycie stolicy państwa posiadającego kolejne rdzenie wyznacza nową stolicę automatycznie; komenda zmiany stolicy jest dostępna dla lidera/zastępcy podczas pokoju. **Sześciogodzinny cooldown na państwo działa od 1.8.2**.
- W czasie pokoju gracz bez państwa (np. bezpośrednio po upadku) powinien jak najszybciej dołączyć do innego albo założyć nowe; obowiązek jest regulaminowy, nie trzeba budować skomplikowanego systemu kar za chwilowy stan przejściowy.
- System rozpoznawania pochodzenia bloków należy uruchomić **zanim gracze zaczną budować** na docelowym serwerze; trzeba uwzględnić tłoki, grawitację, płyny, automatyczne farmy, budowanie zniszczonych struktur i zmiany właściciela.

## 18. Eksploatacja, wdrożenie i kryteria premiery

**Status z 8 listopada 2026:** migracja na VPS zakończona, serwer i mapa HTTPS działają, natomiast edycja jest celowo zatrzymana na **dniu 0**. Dawne plany hostingu i wersji 1.8.7–1.8.8 znajdują się w [archiwum](HISTORIA-ROZWOJU.md); nie są już aktualną listą prac.

### 18.1. Architektura działającej instalacji

```text
Minecraft Java + Simple Voice Chat
        |                  |
     TCP 25565         UDP 24454 (port do potwierdzenia testem)
        |                  |
      Ubuntu / Contabo Cloud VPS 6
          |
       Paper 26.3 + Java 25 + EarthSMP-Core 1.8.13
          |                     |
      Earth + SQLite       portal: 127.0.0.1:8766
                                 |
                           squaremap: 127.0.0.1:8080
                                 ^
                         Caddy + HTTPS :443
                                 ^
                       przeglądarka gracza
```

- **System:** Ubuntu 24.04 LTS, 6 współdzielonych vCPU, 12 GB RAM, 200 GB SSD. Paper działa jako `minecraft` z `-Xms2G -Xmx8G`. Konto `esmpadmin` służy do administracji, `root` nie loguje się bezpośrednio przez SSH.
- **Zabezpieczenia sieci:** UFW otwiera 22/TCP, 25565/TCP, 80/TCP, 443/TCP i UDP Voice Chat (jego działanie po migracji należy potwierdzić). Wewnętrzne porty **8080 i 8766 pozostają tylko na 127.0.0.1**, nie otwieramy ich w zaporze.
- **Mapa:** Caddy reverse proxy do `127.0.0.1:8766`; serwerowy portal sam pobiera publiczne kafelki z `127.0.0.1:8080`. Certyfikat Let's Encrypt został poprawnie wystawiony; lokalne i publiczne żądania strony głównej zwracały HTTP 200. Logowanie WWW po powiązaniu nowego konta działa.
- **Adresy:** obecnie używany publiczny IPv4 Minecraft oraz tymczasowa domena `sslip.io` dla portalu HTTPS. To infrastruktura testowo-przedpremierowa, a nie gwarancja niezmienności adresu po zmianie VPS lub DNS. Aktualne konkretne adresy i konta podaje administrator graczom przez zaufany kanał.
- **Stan danych:** przywrócono czysty świat z `MAIN.zip`, utworzono nową bazę SQLite i nie uruchomiono zegara. Oryginał `MAIN.zip` pozostaje nienaruszalny.
- **Historia finansowania:** pierwszy miesiąc hostingu opłacił administrator; ewentualna zbiórka wśród znajomych ma być zorganizowana później.

Szczegółowy runbook: **[OPERACJE-VPS.md](OPERACJE-VPS.md)**.

### 18.2. Backupy, odtwarzanie i aktualizacje

- **Windows (archiwalnie / lokalnie):** `EarthSMP-Backup.ps1` wymaga zatrzymanego Paper i `-ConfirmStopped`. Skrypt repozytoryjny nie obsługuje bieżącego VPS Linuksowego.
- **Linux (produkcyjny skrypt):** `/usr/local/sbin/earthsmp-backup`; zatrzymuje `earthsmp.service`, wykonuje `tar.gz` ze światem, pluginami i SQLite, waliduje plik, ponownie uruchamia usługę. Potwierdzono wykonanie pierwszego backupu i aktywność Paper po operacji.
- **Harmonogram:** `earthsmp-backup.timer` codziennie o **07:00 Europe/Warsaw**, pliki w `/var/backups/earthsmp`, retencja około 14 dni przez `find -mtime +14`.
- **Poza VPS:** jedna kopia pobrana przez SCP na Windows, przechodzi `tar -tzf` i kontrolę podstawowych plików. Eksport nie jest jeszcze zautomatyzowany, co oznacza ryzyko utraty najnowszego stanu przy awarii całej maszyny.
- **Odtwarzanie:** pełna próba restartu serwera odtworzonego z archiwum **jeszcze nieprzeprowadzona**. Nie mylić jej z prawidłowym odczytem listy plików; testować na oddzielnym katalogu/środowisku.
- **Aktualizacja pluginu:** lokalne `gradlew clean build`, przesłanie wyłącznie shaded JAR, backup, zatrzymanie Paper przez systemd, podmiana JAR, kontrola właściciela pliku, uruchomienie i sprawdzenie logów. Nie trzymać dwóch JAR-ów tego samego pluginu.

### 18.3. Ochrona, polityka bezpieczeństwa, znane granice

- Serwer jest dla zaufanej grupy znajomych. Priorytet to stabilność, spójny stan świata i ekonomii oraz łatwe rozwiązywanie zgłoszeń, nie agresywne automatyczne banowanie.
- **Anti-Xray** Paper włączony i przetestowany w Earth i Netherze (tryb 2); nie wymaga dodatkowego pluginu. **Baritone** może wykonywać dozwolone mechanicznie ruchy: GrimAC nie jest gwarantowanym detektorem, więc zakaz używania go egzekwujemy również regulaminowo.
- **GrimAC i Better Anti-Dupe** zainstalowane i podstawowo przetestowane; Shopkeepers działa, ale konfiguracja administracyjnych sklepów jeszcze przed nami.
- **Portal:** uwierzytelnianie, hashowanie haseł/sesji, kontrole Host/Origin i filtrowanie mapy są wdrożone. HTTPS i logowanie sprawdzone; nie oznacza to zaliczenia testu surowego API oraz wszystkich kombinacji przynależności państwowej.
- **Ryzyka kodu do obserwacji:** możliwy rozjazd SQLite ↔ świat, jeśli inny plugin anulowałby postawienie rdzenia po zapisaniu tokenu; możliwe niepobranie użycia `/home` przy awarii SQLite po teleportacji. To scenariusze brzegowe, nie potwierdzone błędy.
- **Dane w repo:** źródła, dokumentacja i skrypty. Niecommitowane dane świata, pluginów, archiwów, kont, hasła ani sekretne pliki. Sprawdzić historyczne publikacje plików prywatnych według [REPO-PORZADKI.md](REPO-PORZADKI.md).

### 18.4. Checklista go/no-go

Aktualny, oznaczany wynikami wykaz: **[CHECKLISTA-STARTU.md](CHECKLISTA-STARTU.md)**.

W skrócie: budowa Makau i sklepów; sprawdzony backup z pełnym odtworzeniem; kopia poza VPS; test prywatności WWW; Voice Chat; test stabilności; próba koalicji 3 państw przy dostępności testerów; reset przedpremierowych danych budowniczych; snapshot `PRE-LAUNCH`.

**Uruchomienie centralnego zegara jest czynnością jednorazową** i należy do administratora w dniu oficjalnego startu. Podczas prac technicznych dzień pozostaje 0.

### 18.5. Raport z testów PvP / Combat Log — 7 listopada 2026

**EarthSMP-Core 1.8.13, dwa państwa, 2 testerów: 8/8 OK, bez uwag.**

| Test | Wynik |
| --- | --- |
| PvP w pokoju | OK |
| Wypowiedzenie wojny | OK |
| Combat Tag 60 s | OK |
| Wylogowanie podczas Combat Tag | OK |
| Ponowne wejście bez podwójnej kary | OK |
| Bezpieczne wylogowanie po wygaśnięciu | OK |
| Respawn w wojnie | OK |
| Zawarcie pokoju | OK |

Historyczne testy oblężeń i listy starych wersji zachowano w [HISTORIA-ROZWOJU.md](HISTORIA-ROZWOJU.md). Nie oznacza to ukończenia testów wieloosobowych koalicji ani wszystkich przypadków awarii.

---

**Główna specyfikacja v1.8.13 — aktualizacja 8 listopada 2026.** Wcześniejsze komentarze i plany przeniesiono do historii, a bieżące operacje do runbooka VPS.
