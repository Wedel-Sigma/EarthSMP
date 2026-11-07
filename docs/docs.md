---
title: "Kabina Narodów 2.0 — dokumentacja i specyfikacja v1.8.4"
version: "1.8.4"
date: "2026-11-07"
status: "main: EarthSMP-Core 1.8.8; mechaniki kuszy Multishot/Quick Charge zatwierdzone regulaminowo, kod do zmiany w następnym etapie; Unbreaking III zawsze na wszystkich przedmiotach; 1.8.7-1.8.8 czekają na pełne testy"
language: "pl"
---

# KABINA NARODÓW 2.0 — DOKUMENTACJA I SPECYFIKACJA v1.8.4

**Stan ustaleń: 7 listopada 2026 r.**  
**Przeznaczenie:** aktualny zapis zasad, postępu implementacji oraz informacji dla testujących. Dokument nie jest potwierdzeniem wdrożenia każdej wymienionej funkcji.  
**Skala:** prywatny serwer około 30 znajomych, zwykle około 5 osób w państwie; light roleplay, polityka, gospodarka i wojny.

> **Zasada pierwszeństwa:** wszystkie uzgodnione poniżej zasady nowej edycji mają pierwszeństwo przed regulaminem poprzednich edycji. Ze starego regulaminu obowiązuje wszystko, co nie koliduje bezpośrednio z nowymi zasadami. Nie przedstawiać propozycji jako decyzji zatwierdzonych.


## OFICJALNA NAZWA SERWERA — Kabina Narodów 2.0

- **Nazwa prezentowana graczom:** **Kabina Narodów 2.0**. Dotyczy nagłówków w portalu, wiadomości na czacie, Minecraft GUI, sidebaru i etykiet mapy.
- **Nazwy techniczne bez zmian:** `EarthSMP`, `EarthSMPCore`, `EarthSMP-Core`, repozytorium, pakiety i zmienne Javy, identyfikatory w SQLite, nazwy kluczy w squaremap, uprawnienia oraz polecenia `/esmp` i `/earthsmp`.
- **Konfiguracja:** `server.display-name: "Kabina Narodów 2.0"` w `plugins/EarthSMPCore/config.yml` (na nowych instalacjach wpis jest w domyślnym `config.yml`, a na starszych plikach działa wartość domyślna). Zmiana wymaga restartu pluginu/serwera, nie zmiany plików źródłowych.
- **Wydanie:** `1.8.6 R2` wprowadza nazwy widoczne w interfejsie, a `1.8.6 R3` usuwa błąd kompilacji w `SiegeManager.say(...)`. Nie zmieniają mechanik gry ani schematu SQLite v10. Pełny build R3 po poprawce wymaga jeszcze potwierdzenia przez użytkownika.
- **Elementy historyczne:** dawniej wydane przedmioty mogą przechowywać starą nazwę w metadanych przedmiotu aż do ponownego uzyskania lub ręcznej aktualizacji — nie są bezpiecznie masowo nadpisywane.

## STATUS IMPLEMENTACJI — aktualizacja 7 listopada 2026 (1.8.8)

> **Zamknięty etap funkcjonalny mapy i GUI, ale nie zakończone testy produkcyjne.** Rozdzielamy potwierdzone działanie przez użytkownika, poprawki w kodzie i funkcje wymagające jeszcze testów wielu graczy.

**Repozytorium:** [Wedel-Sigma/EarthSMP](https://github.com/Wedel-Sigma/EarthSMP) · **`main`: `1.8.8`** (7.11.2026, commit `ee632e49dbf1e79543c5a11564456569cb9122eb`) · **1.8.7 R3:** użytkownik potwierdził start serwera, ale nie testy PvP; **1.8.8:** kod wypchnięty na GitHub, pełny build i testy funkcjonalne jeszcze niepotwierdzone · **środowisko:** Paper 26.3 / Java 25 / SQLite v10 / squaremap.

### Historia wydań i stan potwierdzenia

| Wersja | Główne zmiany | Weryfikacja |
| --- | --- | --- |
| **1.8.2** | Centralny zegar od 06:00 `Europe/Warsaw`, `/home` do stolicy, 4 teleportacje dziennie od dnia 3, cooldown zmiany stolicy 6 h; SQLite v8. | Testy lokalne, poprawki Gradle/JUnit; pozostały testy multiplayer. |
| **1.8.3** | Publiczne znaczniki wszystkich postawionych rdzeni, stale widoczne współrzędne XYZ, odświeżanie squaremap. | Użytkownik potwierdził działanie. |
| **1.8.4 R2–R3** | Jednorazowe powiązanie Minecraft → konto portalu, logowanie hasłem, sesje do 30 dni, pozycje tylko własnego państwa; naprawione kafelki. SQLite v9. | Portal i mapa działają w testach użytkownika; nadal trzeba sprawdzić izolację wielu państw i surowe API. |
| **1.8.5** | Interaktywny edytor nieregularnych chunków, wnioski, administracyjne zatwierdzanie, SQLite v10. | **Potwierdzone:** edytor działa, a zatwierdzone zmiany granic są widoczne również w Minecrafcie. Kod znajduje się na `main`. |
| **1.8.6** | `/esmp tierlist`, stronicowany ranking postawionych rdzeni (również zdobycznych), sidebar z danymi państwa, dnia, wojny i `/home`; przełącznik `/esmp sidebar`. | **Użytkownik potwierdził prawidłowe działanie rankingu i sidebaru.** Nie jest to pełna walidacja sytuacji wojennych i kilku państw. |
| **1.8.6 R2** | Rebranding widoczny dla graczy do **Kabina Narodów 2.0**, konfiguracja `server.display-name`, bez ruszania `/esmp` i nazw technicznych. | Pierwsza próba kompilacji ujawniła błąd statycznego odwołania w `SiegeManager.say(...)`. Nie traktować R2 jako wydania poprawnie zbudowanego. |
| **1.8.6 R3** | Hotfix: `SiegeManager.say(...)` zmienione ze `static` na metodę instancyjną, ponieważ używa `plugin`. | Kontrolna analiza kodu potwierdza poprawność zmiany; **brak przekazanego wyniku pełnego `./gradlew clean build` i testu R3 na serwerze**. |
| **1.8.7 R3** | PvP pokojowe/tarcza, wojenny respawn z centralnym Title, combat log 60 s, ochrona przypisanych łóżek. Stan kary i timerów utrwalany w plikach pluginu. | **Użytkownik potwierdził, że serwer się uruchamia**, jednak mechaniki PvP, kary za logout i ochrony łóżek **czekają na zbiorcze testy multiplayer**. |
| **1.8.8 — kod na `main`** | Limity ekwipunku, zakaz 3 przedmiotów, reguły Mending/Fortune/enchantów oraz Nether/End. Bez migracji SQLite. | **Potwierdzone:** push na GitHub. **Niepotwierdzone:** pełny build i działanie wszystkich reguł w Paper; wymagana osobna sesja testowa. |

**Stan obecny:** systemy państw, rdzeni, wojen, mapa i GUI działają w zakresach potwierdzonych przez użytkownika. **1.8.7 R3** uruchamia się, ale testy multiplayer jeszcze nie były przeprowadzone. **1.8.8** jest już na `main`, lecz bez przekazanego wyniku pełnego buildu Java 25 i testu funkcjonalnego; nie uznajemy etapu za zweryfikowany. Projekt nie jest gotowy produkcyjnie.

**Zależność mapy:** portal `127.0.0.1:8766` wciąż pobiera publiczne kafelki i znaczniki z wewnętrznego squaremap `http://127.0.0.1:8080` (`map-portal.squaremap-internal-url`). **Nie wyłączać backendu HTTP 8080**. Oryginalnego GUI squaremap nie trzeba udostępniać graczom; 8080 powinno pozostać prywatne.

**Bezpieczeństwo:** publiczna squaremap nie może publikować współrzędnych graczy; portal wymaga filtrowania po stronie serwera. Przed udostępnieniem poza LAN/Tailscale trzeba skonfigurować HTTPS, poprawić obsługę `Host`/`Origin` i przetestować logowanie, sesje oraz izolację państw.

### Historia testów: 1.8.0 i 1.8.1

*Poniższe punkty opisują stan historyczny z okresu 1.8.1; nowsze moduły zestawiono w tabeli powyżej.*

- **EarthSMP-Core 1.8.0 — pierwsza seria multiplayer:** 40/49 testów oznaczono jako zaliczone. Pozostałych 9 nie należy interpretować jako dziewięciu awarii — część była niewykonana lub dotyczyła funkcji niewdrożonych.
- **EarthSMP-Core 1.8.0 — końcowa seria multiplayer:** **12/12 zaliczonych**: blokada demontażu rdzenia na wojnie, reset przejmowania po restarcie, pełne 480 s, zmiana właściciela zdobytego rdzenia, dostęp do zdobytej bazy, brak zmiany granic, zdobycie ostatniego rdzenia, upadek państwa, czyszczenie powiązanych danych oraz trwałość po restarcie.
- Z wcześniejszych testów: państwa i uprawnienia, zaproszenia i role, wnioski o terytoria, mapa polityczna, stawianie i ochrona rdzeni, blokada TNT/pistonów, crafting i 15 użyć Kilofa Oblężniczego, deklaracja wojny, automatyczne oblężenie, BossBar, kontestowanie i cofanie postępu, kontrola obecności obrońcy, `/esmp siege check`, zatrzymanie szturmu i pokój.
- **Dostęp do skrzyń:** przed zdobyciem chronionej bazy zwykły gracz nie mógł otworzyć skrzyni; po prawidłowym zdobyciu test dostępu do zdobytej bazy zaliczono. Przechwytywanie przedmiotów przez mechanizmy techniczne (np. hoppery) wymaga osobnych testów.
- Po testach przywrócono normalne **godziny oblężeń 10:00–00:00 Europe/Warsaw**. Tymczasowe obejście godzin służyło wyłącznie do testów.
- **EarthSMP-Core 1.8.1 (6.11.2026):** zbudowano JAR, uruchomiono na serwerze i **zaliczono dostępne testy solo** (m.in. czwarty licznik rdzeni i zmianę stolicy). Kod 1.8.1 został scalony do `main`. **Nie wykonano jeszcze końcowej sesji multiplayer dla 1.8.1**; zaplanowano trzech testerów (dwóch zwykłych graczy + administrator).
- **Stan implementacji 1.8.1:** automatyczna sukcesja stolicy po zdobyciu, ręczne wskazanie nowej stolicy w pokoju przez lidera/zastępcę, dobrowolne rozwiązanie państwa przez lidera w pokoju z potwierdzeniem oraz licznik wszystkich kontrolowanych rdzeni (własne + zdobyczne) w GUI i `/esmp core stock`. Rozwiązanie państwa i sukcesja po zdobyciu nadal wymagają pełnych testów multiplayer.


### Historyczne ustalenia 1.8.1 i zaległe testy

1. **Stolica:** automatyczna sukcesja po zdobyciu starej jest zaimplementowana; lider/zastępca może podczas pokoju wskazać inny własny, już postawiony rdzeń. Podniesienie aktualnej stolicy pozostaje zablokowane — najpierw trzeba przenieść status stolicy na inny rdzeń. **W 1.8.1 cooldown nie istniał; od 1.8.2 został wdrożony.**
2. **Rozwiązanie państwa:** dostępne dla lidera w pokoju przez GUI lub `/esmp nation disband`, po potwierdzeniu; dotyczy wszystkich aktualnie należących do niego rdzeni (własnych i zdobycznych), członkostw oraz granic. Budynki pozostają. **Test destrukcyjny wykonywać wyłącznie na kopii świata i bazy.**
3. **GUI rdzeni:** czwarty licznik pokazuje łączną liczbę aktualnie postawionych rdzeni kontrolowanych przez państwo, w tym zdobyczne; dotychczasowy licznik postawionych własnych rdzeni pozostaje.
4. **Backup:** aktualny `EarthSMP-Backup.ps1` tworzy spójny backup **offline po zatrzymaniu Paper**, obejmuje `plugins/EarthSMPCore/earthsmp.db` i sprawdza jego obecność w ZIP. Nie jest to dawny wariant backupu online przez RCON. Pełny test przywrócenia kopii z SQLite nadal warto przeprowadzić.
5. **Testy zaległe:** wojenne ograniczenia i zmiana stolicy z drugim graczem, fizyczne usuwanie własnych i zdobytych rdzeni przy rozwiązaniu państwa, trwałość SQLite po restarcie, próby obejścia ochrony i testy kilku państw.

### Dalsze prace / niepotwierdzone w pełnych testach multiplayer

- **Interfejs terytoriów (1.8.5):** claimowanie chunków przypomina **FTB Chunks**, ale odbywa się na mapie WWW bez obowiązkowego moda. Lider/zastępca zaznacza chunki myszką i wysyła wniosek, który zatwierdza administrator. **Użytkownik potwierdził działanie edytora oraz odzwierciedlenie zaakceptowanych zmian w grze.**
- **Mody klienta są opcjonalne**; zamiast obowiązkowego instalatora-wizarda planujemy paczkę modów i krótki tutorial dla chętnych. Czysty klient Minecrafta ma pozostać wystarczający do gry.
- Prywatne pozycje graczy na mapie (tylko własne państwo, bez sojuszników, wrogów oraz Netheru/Endu) — **zaimplementowane w portalu 1.8.4**, jednak wymagają pełnego testu z wieloma państwami i weryfikacji publicznych endpointów.
- **Publiczne pozycje wszystkich postawionych rdzeni** (niezależnie od właściciela, granic i dyplomacji) — **wdrożone i potwierdzone na testowym serwerze w 1.8.3**; ikona lodestone i współrzędne `X/Y/Z`, bez numerów oraz oznaczenia stolicy.
- **Centralny zegar edycji i `/home`:** wdrożone od **1.8.2** — zmiana dnia o 06:00 `Europe/Warsaw`, komendy zegara, teleportacja także podczas wojny, bez limitu w dniach 1–2, następnie 4 na gracza dziennie; cooldown ręcznej zmiany stolicy 6 h na państwo. Wciąż potrzebny test integracyjny wielu graczy, granicy dnia i restartu.
- **Interfejs gracza (1.8.6 — podstawowe testy zaliczone):** `/esmp tierlist` — ranking wszystkich państw według aktualnie kontrolowanych postawionych rdzeni (własnych i zdobycznych); scoreboard/sidebar z dniem serwera, nazwą państwa, rdzeniami, liczbą członków online, pokojem/wojną i `/home`. **Użytkownik potwierdził działanie rankingu i sidebaru na serwerze.** Osobno pozostają testy zmian członkostwa, kilku państw, wojny i restartu.
- Testy większych koalicji, jednoczesnych wojen i obciążenia serwera, omijania ochrony skrzyń (hoppery, wagoniki), niespodziewanych zdarzeń bloków, exploitów i odporności na awarie.
- **PvP i odrodzenie (1.8.7 R3):** zaimplementowane, potwierdzono jedynie uruchomienie na Paper; brak testów wielu graczy. **Limity, enchanty, wymiary (1.8.8):** zaimplementowane w paczce testowej, nadal wymagają pełnego buildu i weryfikacji interakcji. **Barterowe sklepy i pozostałe dodatki:** w planie.

### Feedback od testerów

Przy zgłaszaniu uwag podaj: **wersję pluginu**, **rolę i czy miałeś OP**, **kroki odtworzenia**, **wynik oczekiwany**, **wynik faktyczny** oraz (jeśli są) screenshot i istotny fragment logów. Nie publikuj publicznie adresu IP, danych Tailscale ani pełnych logów zawierających dane prywatne.

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
- **Nether** dostępny od **06:00 (Europe/Warsaw) dnia serwera 10**; **End** wyłączony na stałe. Pierwszych **dwóch villagerów** do rozpoczęcia hodowli trzeba zdobyć przez leczenie zombie villagerów; późniejsze rozmnażanie jest dozwolone. Zasada pochodzenia villagerów jest na razie **regulaminowa, nieegzekwowana automatycznie przez plugin**. Mending zakazany. Fortune: brak do dnia 6, I od dnia 7, III od dnia 16, Fortune II nigdy. Pozostałe enchanty dokładnie według reguł w 1.8.8, bez rozszerzania wyjątków QoL.
- Barter **przedmiot za przedmiot**, bez wirtualnej waluty. Gracze: sklepy skrzynkowe. Admin: nieskończone wymiany podstawowych materiałów (np. cobblestone ↔ cobbled deepslate 1:1). Docelowa mapa przeglądarkowa koloruje chunki państw, pokazuje **graczy tylko członkom tego samego państwa**, a **wszystkie postawione rdzenie publicznie** z ikoną lodestone i współrzędnymi.
- **Zegar edycji:** centralny numer dnia zmienia się codziennie o **06:00 czasu polskiego**. Docelowe `/home` działa także podczas wojny: dni 1–2 bez limitu, od dnia 3 maksymalnie **4 razy dziennie na gracza** (reset 06:00). Ręczna zmiana stolicy: **6 godzin cooldownu na państwo** (plan).
- **Interfejs 1.8.6:** `/esmp tierlist` — państwa sortowane według kontrolowanych postawionych rdzeni (w tym zdobycznych); indywidualny panel boczny serwera/państwa. Podstawowe działanie obu funkcji zostało potwierdzone w grze.

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
- **Aktualizacja 2026-11-07:** na GitHub `main` jest **1.8.6**, a użytkownik potwierdził start `1.8.7 R3`. Testy multiplayer PvP, respawnu, combat logu i łóżek są zaplanowane; kod `1.8.8` stanowi **wydanie testowe**, nie wynik testu na Paper. Pozostałe rozdziały zawierają również zasady **planowane**, a nie wdrożone.

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
- **Implementacja 1.8.8 (jeszcze nietestowana na Paper):** kontrole podnoszenia, przenoszenia ze skrzyń, craftingu, Creative, drag, kliknięć wielokrotnych i okresowe przeglądy ekwipunku. W przypadkach nietypowych nadwyżka jest **odkładana na ziemię**, nie usuwana. Przed akceptacją wymagane testy zabezpieczeń, zwłaszcza z innymi pluginami sklepów i niestandardowymi interfejsami. Produkcyjny sposób obsługi nieprzewidzianych operacji do potwierdzenia.
- End jest niedostępny, więc nie przewidujemy naturalnego źródła Shulker Boxów ani osobnego systemu wyjątków; administracja nie powinna dystrybuować ich wbrew tej zasadzie.
- Pozostałe odziedziczone zasady PvP z dawnego regulaminu opisano w sekcji 14.

## 11. Zaklęcia (enchanty) i progresja

- Pierwszych **dwóch villagerów** pozyskuje się **przez leczenie zombie villagerów**. Po zdobyciu tej pary można **normalnie rozmnażać villagerów**. Nie ma adminowskiego sklepu z enchantami. **Uwaga implementacyjna:** plugin 1.8.8 sprawdza legalność ofert enchantów, lecz nie weryfikuje pochodzenia pierwszej pary — obecnie jest to zasada regulaminowa.
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

### 11.2. Kusze — wyjątki zatwierdzone 7 listopada 2026 (implementacja do wykonania)

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
- **Status techniczny:** powyższe zasady są **zatwierdzoną specyfikacją**, ale **nie są jeszcze zaimplementowane w 1.8.8**. Obecny `EditionEnchantRules` nie rozróżnia rodzaju przedmiotu, pozwala na Multishot I i Quick Charge I/II. **Unbreaking III na kuszy jest już prawidłowo dozwolony** i nie wymaga zmiany. **Następna sesja programistyczna:** dodać walidację zależną od typu przedmiotu i testy obejść z użyciem gotowych kusz, w tym ofert villagerów; nie oznaczać funkcji jako przetestowanej przed buildem na Paper.

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
- **Status:** mechanika wdrożona od **1.8.2**, z zapisem limitów w SQLite i resetem o 06:00. Klasyczny respawn przy łóżku pozostaje niezależny od `/home`.

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
- **Stan od 1.8.5:** publiczna mapa polityczna, interaktywny edytor chunków, mapowe wnioski oraz panel admina WWW są zaimplementowane i przeszły test użytkownika z potwierdzeniem efektu w Minecrafcie. Obsługa konflikujących wniosków wymaga testów z wieloma graczami.

### 16.2. Publiczne znaczniki rdzeni — wdrożone w 1.8.3

- Na mapie WWW każdy obserwator, **również niezalogowany, z innego państwa, sojusznik lub wróg**, widzi **wszystkie postawione rdzenie**. Nie ma ukrywania według dyplomacji ani granic politycznych.
- Znak graficzny to **lodestone**. **Nad nim koordynaty `X`, `Y`, `Z`** rzeczywistego bloku rdzenia. Nie podajemy numeru („rdzeń 1/2”), nazwy, właściciela ani statusu **STOLICA** przy znaczniku. Polityczne kolory terytoriów są osobną warstwą.
- Po postawieniu lub przeniesieniu rdzenia marker pojawia się/zmienia lokalizację; po zdobyciu pozostaje w tym samym miejscu (bez publicznego oznaczania zmiany właściciela); po demontażu, zniszczeniu ostatniego rdzenia lub rozwiązaniu państwa znika. Dane synchronizowane z zatwierdzonym stanem świata/SQLite.
- **Publiczność znaczników rdzeni NIE oznacza upublicznienia pozycji graczy**. Pozycje graczy nadal widoczne tylko dla członków ich własnego państwa (oraz samego gracza), z wykluczeniem sojuszników, wrogów i publicznej mapy.
- **Stan od 1.8.3:** znaczniki są publikowane przez squaremap, użytkownik potwierdził działanie na serwerze testowym. Komenda administracyjna: `/esmp core maprefresh`.


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
- **Dostęp zewnętrzny:** lokalne HTTP jest tylko do testów. Na serwerze produkcyjnym konieczne jest HTTPS oraz prawidłowa konfiguracja origin, proxy, ciasteczek i uprawnień. **Obecne `MapPortalServer.validateOrigin` wymaga jawnego numeru portu, a `hostAllowed` porównuje dokładnie nagłówek Host**; przed użyciem ładnego adresu `https://map...` bez `:443` należy tę logikę dostosować i przetestować z reverse proxy. Nie traktować aktualnego 1.8.4 R3 jako gotowego do wystawienia wprost do Internetu.

## 17. Administracja i wyjątkowe przypadki

- Administratorzy mają **pełną kontrolę nad wszystkimi państwami, rdzeniami, członkostwem, wojnami, granicami i ochroną**. Błędy lub nietypowe zakończenia konfliktów można ręcznie naprawić.
- W trakcie wojny dobrowolne rozwiązanie państwa jest zablokowane. Jeśli państwo **przymusowo upada** po utracie ostatniego rdzenia, likwidację równoległych wojen i relacji koalicyjnych może przeprowadzić administrator.
- **Stan 1.8.4 R3:** zdobycie stolicy państwa posiadającego kolejne rdzenie wyznacza nową stolicę automatycznie; komenda zmiany stolicy jest dostępna dla lidera/zastępcy podczas pokoju. **Sześciogodzinny cooldown na państwo działa od 1.8.2**.
- W czasie pokoju gracz bez państwa (np. bezpośrednio po upadku) powinien jak najszybciej dołączyć do innego albo założyć nowe; obowiązek jest regulaminowy, nie trzeba budować skomplikowanego systemu kar za chwilowy stan przejściowy.
- System rozpoznawania pochodzenia bloków należy uruchomić **zanim gracze zaczną budować** na docelowym serwerze; trzeba uwzględnić tłoki, grawitację, płyny, automatyczne farmy, budowanie zniszczonych struktur i zmiany właściciela.

## 18. Rozwój projektu i wstępny plan wdrożenia produkcyjnego

> **Status: plan roboczy, bez zakupu hostingu/domeny i bez daty premiery.** Docelowo prywatna edycja na około 30 znajomych; finansowanie hostingu ma zostać zorganizowane przez **zbiórkę wśród graczy**. Nie wpisujemy cen jako stałych, ponieważ oferty i warunki mogą się zmieniać. Decyzję o dostawcy podejmujemy po teście technicznym, nie na podstawie samego hasła „dodatkowe porty”.

### 18.1. Etapy prac nad pluginem — stan obecny

- **3.1 — specyfikacja zasad:** ukończona i aktualizowana po większych zmianach.
- **3.2 — dobór pluginów uzupełniających:** LuckPerms, WorldGuard/WorldEdit, CoreProtect, ewentualne Chest Shops, voice chat — ostateczny zestaw i wersje wymagają osobnej kontroli.
- **3.3 — EarthSMP-Core:** na GitHub `main` jest `1.8.6` (zweryfikowane 7.11.2026). **1.8.5 przeszła test edytora i synchronizacji granic**, **1.8.6 przeszła podstawowy test GUI/HUD**, **1.8.7 R3 wystartowała na serwerze**, a **1.8.8 jest paczką do testów**. Pełne testy multiplayer pozostają do wykonania.
- **3.4 — zakończony test podstawowy 1.8.5:** interaktywny edytor chunków, wnioski WWW, panel admina i poprawna synchronizacja granic z Minecraftem. Testy wieloosobowe i konkurencyjne wnioski pozostają do wykonania.
- **3.5 — interfejs i branding 1.8.6:** ranking `/esmp tierlist` i sidebar potwierdzone w podstawowym teście; nazwa graczy „Kabina Narodów 2.0” w R2/R3. Weryfikacja ostatecznej paczki R3 oraz pełnych scenariuszy multiplayer pozostaje otwarta.
- **3.6 — najbliższa sesja testowa:** 1.8.7 R3 (dwa państwa, PvP, combat log, respawn i łóżka) oraz 1.8.8 (limity wyposażenia, enchanty, wymiary). Dopiero po wynikach zamkniemy te etapy.

### 18.2. Zakładana architektura po wdrożeniu

```text
Gracz Minecraft Java ────────> adres serwera Minecraft (hosting / subdomena)
Gracz w przeglądarce ─HTTPS─> stabilny adres portalu EarthSMP
                                         |
                              reverse proxy / tunel HTTPS
                                         |
                         EarthSMP-Core: portal HTTP :8766
                                  |                 |
                         sesje, konta,         publiczne zasoby
                         widoczność graczy     squaremap :8080
                                  |                 |
                                  +------ Paper + pluginy
                                            |
                                      SQLite + świat Earth
                                      + wygenerowane kafelki
```

- **Jedna usługa gry + portal to preferowany wariant**, jeżeli hosting dopuszcza porty WWW i ich bezpieczną publikację. Cloudflare Pages **nie jest wymagane**: obecny frontend jest serwowany przez plugin Java, a logowanie i pozycje są dynamiczne.
- **squaremap pozostaje na serwerze** jako generator kafelków i warstw politycznych. Jej oryginalne GUI nie będzie linkowane graczom. **W 1.8.4 R3 `:8080` jest nadal konieczne jako wewnętrzny upstream**; może zostać niewidoczne publicznie, ale nie można go po prostu wyłączyć. Późniejszy refaktor może zastąpić HTTP dostępem bezpośrednio do wygenerowanych plików.
- **`:8766`** to portal dostępny dziś lokalnie. Produkcyjny użytkownik ma korzystać ze stabilnego **HTTPS**, bez ręcznego wpisywania portu. Adresy typu `mc.earthsmp.pl`, `map.earthsmp.pl` są wyłącznie ilustracją, **nie oznaczają posiadania domeny**.
- Tylko publiczne granice, geografia i rdzenie są dostępne anonimowo. Pozycje graczy są zwracane przez chronione API, z filtrowaniem aktualnego państwa na backendzie. Nie proxy'ować publicznego `players.json` i nie udostępniać pełnego katalogu squaremap bez kontroli.

### 18.3. Wybór hostingu — wymagania przed zakupem

**Wymagane / do pisemnego potwierdzenia u dostawcy:**

1. Minecraft Java z **Paper 26.3 i Java 25** oraz możliwość wrzucenia własnych JAR-ów (w tym shaded `EarthSMP-Core`) i konfiguracji pluginów.
2. Wystarczające **RAM/CPU** dla świata Ziemi, squaremap i około 30 zarejestrowanych graczy (szczytowa jednoczesna liczba do ustalenia); miejsce na mapę, kafelki, logi, SQLite i zapas na backupy. Nie szacować zasobów wyłącznie po liczbie graczy.
3. Możliwość uruchomienia **HTTP portalu `8766` w pluginie** oraz **wewnętrznego HTTP squaremap `8080`** na tym samym serwerze/kontenerze; niekolidujące porty, obsługa połączeń HTTP. Samo „dodatkowe porty UDP” nie wystarczą.
4. Sposób udostępnienia **HTTPS z poprawnym Host/Origin**: reverse proxy hostingu, możliwość uruchomienia `cloudflared`/własnej usługi, bądź zewnętrzne proxy/tunel. **Dodatkowy port nie oznacza automatycznie TLS ani prawa do uruchamiania usług pomocniczych.**
5. Dostęp do plików, wygodny upload dużego świata, możliwość pełnego zatrzymania procesu, harmonogram zadań/backupu i bezpieczny eksport SQLite. Sprawdzić politykę hostingu wobec dodatkowego HTTP w pluginie.
6. Opcja próbna / okres testowy i warunki zmiany pakietu. Przed płatnością przetestować uruchomienie **tego konkretnego pluginu i portalu**, nie tylko pustego Paper.

**Kandydaci do weryfikacji:** BedrockHost, Astroworld, EpicHost — rozważani podczas rozmowy ze względu na oferty portów; **to nie jest jeszcze wybór dostawcy ani gwarancja obsługi naszej konfiguracji**. Sprawdzić aktualne regulaminy, wersje Javy, realny dostęp HTTPS i możliwość uruchomienia procesów.

**Alternatywa awaryjna:** jeśli hosting Minecraft blokuje prawidłowy dostęp do portalu WWW, użyć osobnego małego **VPS/reverse proxy** albo hostować cały Paper i WWW na VPS z administracją własną. Jest to bardziej elastyczne, ale wymaga utrzymania systemu, aktualizacji, zapory i monitoringu. Nie zakładać automatycznie dwóch płatnych hostingów.

### 18.4. Domena, darmowe adresy i Cloudflare

- **Zakup własnej domeny nie jest obowiązkowy do uruchomienia serwera.** Minecraft może działać pod subdomeną otrzymaną od hostingu. Dla portalu WWW potrzebny jest jednak **stabilny adres HTTPS** z certyfikatem — może to być darmowa subdomena usługodawcy, jeżeli rzeczywiście ją udostępnia dla naszej aplikacji.
- **Wariant własnej domeny (opcjonalny):** płatna domena, np. `earthsmp.pl`, plus dwa rekordy: `mc.` dla gry i `map.` dla portalu. Zakup i odnowienie domeny to osobne koszty; subdomeny zazwyczaj nie wymagają osobnego zakupu. Wysokość kosztu ustalić przed zbiórką.
- **Cloudflare Tunnel:** stały publiczny hostname wymaga domeny skonfigurowanej w Cloudflare i działającego procesu `cloudflared`, do czego hosting musi zapewnić możliwość techniczną. **Darmowy Quick Tunnel `trycloudflare.com` nadaje się do testów, a nie jako stały adres edycji** (adres może się zmieniać; brak gwarancji działania). Sam DNS/proxy Cloudflare nie zastępuje hostingu świata, pluginu ani bazy.
- Jeśli nie kupimy domeny, najpierw sprawdzić, czy hosting zapewnia **bezpłatny, stały adres HTTPS** dla niestandardowego portalu — nie obiecywać go bez potwierdzenia. W przeciwnym razie rozważyć zakup domeny lub inny mechanizm publikacji.
- **Minecraft i WWW to dwa różne protokoły.** Cloudflare HTTP Tunnel do mapy nie zapewnia automatycznie publicznego połączenia Minecraft; gra korzysta z adresu i portu serwera od hostingu (ewentualnie DNS/SRV).

### 18.5. Plan migracji z lokalnego serwera — kolejność

1. **Dokończyć mechaniki i zamrozić wersję kandydującą do wydania.** Zakończyć testy 1.8.4, edytor chunków, niezbędne funkcje PvP/wojen, zabezpieczenia i interfejs; odłożyć niekrytyczne dodatki, jeśli nie blokują startu.
2. **Zebrać środki na hosting** od chętnych graczy; przed wpłatami ustalić koszt całkowity na edycję, okres, sposób rozliczania, zapas, zasady zwrotów i osobę opłacającą usługę. Domena, jeśli będzie kupiona, jest osobnym punktem budżetu.
3. **Wybrać i przetestować hosting** według §18.3: najpierw Paper+Java 25, następnie `:8080` → `:8766`, potem HTTPS z realnego urządzenia spoza sieci serwera.
4. **Zatrzymać lokalny Paper**; zachować nieruszony backup **`MAIN`**, dodatkowo utworzyć nową kopię migracyjną *offline* (świat Earth, Nether jeśli występuje, `plugins/` i konfiguracje, SQLite z ewentualnymi plikami `-wal`/`-shm` w spójnym stanie, folder danych squaremap, server.properties, lista wersji pluginów). Hasła i sekrety przekazać wyłącznie prywatnym kanałem.
5. **Przenieść pliki na hosting**, zgodnie z układem oczekiwanym przez usługodawcę: świat i pliki konfiguracyjne, wymagane pluginy, **tylko shaded JAR EarthSMP-Core (nie `-plain.jar`)**, bazę `plugins/EarthSMPCore/earthsmp.db`, dane mapy. Gradle i źródła nie muszą być na produkcyjnym serwerze.
6. **Przygotować i ustawić konfigurację**: `level-name=Earth`, `world-name: Earth`, identyfikator Overworldu, wewnętrzny upstream squaremap `http://127.0.0.1:8080`, bind portalu, `map-portal.public-origin`, rekordy DNS/TLS i ewentualne reguły reverse proxy. **Przed publicznym HTTPS poprawić walidację `public-origin`/`Host` w `MapPortalServer`**, która obecnie nie akceptuje standardowego adresu HTTPS bez jawnego portu; dodać test z proxy, nagłówkiem Host, cookies Secure i logowaniem.
7. **Uruchomić testowo, ale jeszcze nie otwierać edycji**: sprawdzić konsolę, schemat SQLite (**v10 również dla 1.8.7/1.8.8**), spójność państw, rdzeni, zegara, `/home`, wojen, kafelków, kont WWW oraz uprawnień. Pamiętać, że uruchomienie `/esmp clock start` jest decyzją startową edycji — nie wykonywać jej podczas migracyjnej próby na bazie produkcyjnej.
8. **Skonfigurować oficjalny adres** gry i portalu oraz w razie potrzeby dodać klikalne `/esmp map` (przyszły etap). Ograniczyć dostęp do 8080 i 8766, by ruch publiczny przechodził wyłącznie przez kontrolowaną warstwę HTTPS.
9. **Wykonać próbę obciążeniową i test bezpieczeństwa**, w tym równoległe państwa i przeglądarki, login/revoke/reset, zmiany członkostwa, Nether, brak wycieków przez API i przez oryginalną squaremap. Sprawdzić odtwarzanie kopii. Dopiero potem ogłosić datę edycji i rozpocząć zegar.

### 18.6. Backup i odtwarzanie po uruchomieniu

- Backup **`MAIN` jest osobną, nietykalną kopią stanu bazowego**; nie nadpisywać go nowymi backupami ani eksperymentami. Po migracji zachować drugi, nazwany punkt przywracania „przed startem produkcji”.
- Wykonywać **automatyczne kopie** świata, danych pluginów i **spójnej bazy SQLite**. Najprostszy sprawdzony model projektu: backup po zatrzymaniu Paper. Jeśli dostawca proponuje snapshoty online, zweryfikować spójność SQLite (checkpoint/WAL lub backup API) — nie kopiować bez zastanowienia tylko aktywnego `.db`.
- Kopie przechowywać **poza serwerem hostującym** (minimum jedna lokalizacja zewnętrzna), z rotacją i testem odtworzenia na osobnej instancji; nadzorować wykorzystanie dysku przez squaremap i logi.
- Po rozpoczęciu edycji nie resetować centralnego zegara i nie podmieniać samodzielnie samej bazy bez zgodnego świata; wersja schematu oraz pliki świata tworzą jeden stan gry.

### 18.7. Kryteria gotowości do startu (checklista)

- [ ] Hosting sprawdzony na rzeczywistych Java 25 + Paper 26.3 + EarthSMP-Core i squaremap; dostęp do danych i portów potwierdzony.
- [ ] Portal dostępny pod **stabilnym adresem HTTPS** z drugiego urządzenia/sieci; certyfikat i właściwe Host/Origin/Secure cookie działają.
- [ ] **8080 nie jest publicznie widoczne**, lecz działa jako wewnętrzny upstream do czasu refaktoru; publiczna squaremap nie publikuje pozycji graczy.
- [ ] Jednorazowe powiązanie konta, logowanie z innego urządzenia, trwałość sesji po restarcie, revoke i reset — sprawdzone.
- [ ] Bez zalogowania i z konta obcego państwa **nie da się pobrać cudzych współrzędnych**; weryfikacja zarówno GUI, jak i surowego HTTP/API.
- [ ] Granice, rdzenie i kafelki mapy odświeżają się po zmianach; mapa nie ujawnia stolicy jako specjalnego statusu.
- [ ] Edytor chunków i zatwierdzanie administracyjne działają lub są świadomie wyłączone przed startem, z komendami jako planem awaryjnym.
- [ ] Zegar 06:00, limity `/home`, cooldown 6 h, oblężenia 10:00–00:00, upadek państwa i backupy mają zaliczone testy na docelowym środowisku.
- [ ] Istnieje działający **backup i przetestowane odtworzenie** świata + SQLite; zapas dyskowy potwierdzony.
- [ ] Regulamin, sposób zgłaszania błędów, link do mapy i koszt utrzymania zostały podane graczom.

### 18.8. Etapy 1.8.7–1.8.8 i następne kroki

**1.8.7 R3 — implementacja gotowa do testów multiplayer:** PvP pokojowe i tarcza; combat tag 60 s z karą za wyjście; wojenny respawn 15–50 s z dużym centralnym odliczaniem; ochrona przypisanych łóżek. Potwierdzony jest tylko start pluginu, nie przypadki bojowe. Zbiorcza sesja z 3–4 graczami i minimum dwoma państwami jest obowiązkowa przed akceptacją.

**1.8.8 — paczka do lokalnego builda i testów:**

- Limity plecak + hotbar + offhand (i wyposażenie gracza): 8 zwykłych złotych jabłek, 3 perły, 16 pajęczyn, 3 totemy; zakaz posiadania zaklętych złotych jabłek, End Crystals i Respawn Anchors. Częściowe pobieranie przedmiotów ma pozostawiać resztę w źródle. Nietypowy nadmiar jest upuszczany na ziemię, nie kasowany.
- Mending zakazany; Fortune I od 7. dnia, III od 16., II nigdy. Zaklęcia walki I do dnia 9, II od dnia 10. **Wyjątki QoL pozostają jak w 1.8.8 z nowym, przedmiotowym wyjątkiem dla kusz (§11.2), do wdrożenia w następnym kodzie.**
- End zawsze zamknięty. Nether od **06:00 dnia serwera 10** (`progression.nether-unlock-day: 10`) — **decyzja ostateczna**, zgodna z implementacją.
- Brak nowego schematu SQLite: nadal **v10**. Nowa logika nie wymaga resetu danych, lecz pełnego backupu i testu ze starymi przedmiotami.
- **Kryterium akceptacji:** `./gradlew clean build` na Java 25, start pluginu i seria testów: częściowe podnoszenie, shift-click, drag, Creative, `/give`, enderchest, craft, villagery, kowadło, Mending, granice dni 6/7/9/10/15/16 oraz portale End/Nether i próby /tp.

**Przed sesją testową — kolejna zmiana kodu:** wdrożyć zaakceptowane zasady kusz z §11.2 (Multishot 0, Quick Charge II/III, Piercing I/II; Unbreaking III bez zmian także na kuszy), weryfikując gotowe przedmioty i handel. Następnie pełny build i testy Paper.

**Po sesji testowej:** poprawki, aktualizacja stanu dokumentacji, audyt publicznego portalu, instalacja na hostingu z HTTPS, test backupu i obciążenia; gospodarka barterowa / Chest Shops to osobny etap. Nie planujemy obecnie rozszerzać specyfikacji o kolejne nieuzgodnione mechaniki.

## 19. Szczegóły do rozstrzygnięcia podczas projektowania technicznego (NIE nowe reguły)

Dla przejrzystości poniższe elementy nie są ukrytymi „zatwierdzonymi zasadami”; to techniczne wyjątki wymagające rozpisania implementacji:

1. **Techniczny wygląd blokady respawnu (reguły rozstrzygnięte):** wybrać ekranowy sposób prezentacji 15–50 s, zabezpieczyć pełny brak ruchu i interakcji oraz nietykalność *podczas* odliczania. Po odliczaniu brak ochrony. „Zbrojownia” nie jest specjalną strefą.
2. **Wyjątki enchantów — rozstrzygnięte:** obowiązuje lista i poziomy z 1.8.8. Nie dodawać dalszych wyjątków dla mobilności lub przeżywalności bez nowej decyzji.
3. **Odporność systemu ochrony:** pełna lista wyjątków fizyki, tłoków, eksplozji, zastępowania bloków, redstone oraz obrony łóżek i ich podpór.
4. **Precyzyjna mechanika startu oblężenia:** rozróżnienie obecności wrogiego gracza w chronionej bazie, startu ataku przy progu online i prowadzenia licznika; nie rozszerzać blokad na pokój lub neutralnych.
5. **Mapa prywatnych pozycji i jawnych rdzeni:** od 1.8.4 istnieje uwierzytelnienie do pozycji współobywateli i filtrowanie po stronie serwera; potrzebne są testy wielu państw oraz audyt publicznych endpointów. **Pozycje wszystkich rdzeni są publiczne, ale pozycje graczy pozostają prywatne.**
6. **Edytor terytoriów WWW (kierunek zatwierdzony):** autoryzacja konta Minecraft, narzędzia zaznaczania wielu chunków (również mobilnie), podgląd wniosków, panel admina i walidacja backendowa. Sam wybór mapy WWW zamiast moda jest już rozstrzygnięty.
7. **Zegar i `/home`:** wdrożone od 1.8.2; nadal przetestować strefę `Europe/Warsaw`, zmianę czasu, restarty i czytelność komunikatów. Zachować ustalone 06:00, dni 1–2 bez limitu i 4 użycia od dnia 3.
8. **Nether — rozstrzygnięte:** start o **06:00 dnia 10** (`Europe/Warsaw`), zgodnie z aktualnym kodem. Dawne sformułowania „po 10 dniach” są nieaktualne.
9. **Interfejsy i konkretne pluginy:** wygląd ikony państwa, szczegóły bezpiecznego miejsca przy stolicy, współdziałanie sidebaru z innymi scoreboardami oraz adminowskie komendy ręcznej korekty. Sam przełącznik `/esmp sidebar` jest już zaimplementowany. To decyzje techniczne, nie nowe zasady.

**Koniec dokumentacji v1.8.4.** Decyzje administracyjne z 7 listopada 2026 r. rozstrzygają próg Netheru, wyjątki enchantów oraz pozyskiwanie pierwszej pary villagerów; mechanizmy PvP i 1.8.8 wymagają dalszych testów.


<!-- Historia aktualizacji v1.7: GUI 1.8.6, rebranding R2, hotfix R3; informacje o main uaktualniono w v1.8. -->

<!-- Aktualizacja v1.8: 1.8.7 R3 start bez testów PvP; 1.8.8 przygotowana do builda; limity/enchanty/wymiary, próg Netheru do potwierdzenia; audyt dzienny w osobnym pliku. -->


<!-- Aktualizacja v1.8.1: potwierdzone 06:00 dnia 10, enchanty bez zmian względem 1.8.8, pierwsza para villagerów z leczenia (później rozmnażanie), main 1.8.8 oraz procedura porządkowania repo. -->

<!-- Aktualizacja v1.8.2: wyjątki dla kuszy (Multishot zakaz, Quick Charge II/III, Piercing I/II); zmiany w kodzie odłożone na następną sesję. -->
<!-- Korekta v1.8.3: Unbreaking III zawsze i na wszystkich przedmiotach, również kuszach; kod 1.8.8 już spełnia tę regułę. -->

<!-- Aktualizacja v1.8.4: zbiorcza tabela limitów enchantów (§11.3), trzy momenty zmian: dni 7/10/16; bez zmian w kodzie Javy. -->
