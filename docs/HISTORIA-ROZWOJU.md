# EarthSMP — archiwum historii rozwoju i planów (do 7 listopada 2026)

> **MATERIAŁ ARCHIWALNY, NIE OBOWIĄZUJĄCY STATUS PRODUKCYJNY.** Zapis wcześniejszych testów, założeń i planów (także tych, które później wykonano lub zmieniono). Przechowywany dla ciągłości projektu. Aktualne informacje w [docs.md](docs.md), [OPERACJE-VPS.md](OPERACJE-VPS.md) i [CHECKLISTA-STARTU.md](CHECKLISTA-STARTU.md).

## STATUS IMPLEMENTACJI — aktualizacja 7 listopada 2026 (1.8.13)

> **Zamknięty etap funkcjonalny mapy i GUI, ale nie zakończone testy produkcyjne.** Rozdzielamy potwierdzone działanie przez użytkownika, poprawki w kodzie i funkcje wymagające jeszcze testów wielu graczy.

**Repozytorium:** [Wedel-Sigma/EarthSMP](https://github.com/Wedel-Sigma/EarthSMP) · **`main`: `1.8.13`** (sprawdzone 7.11.2026; ostatni merge kodu `e0ac475b6290`) · **środowisko:** Paper 26.3 / Java 25 / SQLite v10 / squaremap. **Potwierdzone:** pełny `gradlew clean build` i podstawowe działanie kodu 1.8.13 na serwerze przez użytkownika. Publiczny marker spawnu Makau (sam domek) jest obecny w repozytorium; jego osobny test nie został wyraźnie potwierdzony. **Nadal wymagane:** pełne testy multiplayer, bezpieczeństwo WWW pod HTTPS, graniczne przypadki mechanik oraz odtworzenie backupu świata i SQLite.

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
| **1.8.8** | Limity ekwipunku, zakaz 3 przedmiotów, reguły Mending/Fortune/enchantów oraz Nether/End. | Podstawowe zachowania sprawdzone, przypadki graniczne w audycie. |
| **1.8.9–1.8.11** | Monitor alertów bezpieczeństwa, blokady portalu Host/Origin, WorldBorder, reguły kusz. | Implementacja obecna w `main`, wymagane testy ataków i integracji. |
| **1.8.12** | `/home` 30 s odliczania przez Title, anulowanie przy ruchu lub otrzymaniu obrażeń. | Podstawowe działanie potwierdzone w grze. |
| **1.8.13 — aktualny `main`** | Desktop LPM/PPM/ŚPM, mobilne tryby, selektywne Title/ActionBar, odświeżanie ikon rdzeni po zmianie + self-healing co 10 min, marker spawnu Makau jako sam domek. | **Udany build i podstawowy test użytkownika**, testy skrajne jeszcze przed nami. |

**Stan obecny:** `1.8.13` jest na `main` i przeszedł podstawowy build/test użytkownika. **Rozpoczynamy audyt kodu oraz kontrolowane testy exploitów**, a dopiero później pełną sesję multiplayer. Sam start Paper nie potwierdza poprawności wojny, zdobywania rdzeni, izolacji portalu, odporności na dupe ani backupów.

**Zależność mapy:** portal `127.0.0.1:8766` wciąż pobiera publiczne kafelki i znaczniki z wewnętrznego squaremap `http://127.0.0.1:8080` (`map-portal.squaremap-internal-url`). **Nie wyłączać backendu HTTP 8080**. Oryginalnego GUI squaremap nie trzeba udostępniać graczom; 8080 powinno pozostać prywatne.

**Bezpieczeństwo:** publiczna squaremap nie może publikować współrzędnych graczy; portal wymaga filtrowania po stronie serwera. Kontrole `Host`/`Origin` i sesji **są już w kodzie 1.8.13**. Przed udostępnieniem poza LAN/Tailscale trzeba skonfigurować HTTPS i zweryfikować działanie reverse proxy, logowania, sesji oraz izolacji państw surowym HTTP/API.

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


---

## 18. Rozwój projektu i wstępny plan wdrożenia produkcyjnego

> **Status: plan roboczy, bez zakupu hostingu/domeny i bez daty premiery.** Docelowo prywatna edycja na około 30 znajomych; finansowanie hostingu ma zostać zorganizowane przez **zbiórkę wśród graczy**. Nie wpisujemy cen jako stałych, ponieważ oferty i warunki mogą się zmieniać. Decyzję o dostawcy podejmujemy po teście technicznym, nie na podstawie samego hasła „dodatkowe porty”.

### 18.1. Etapy prac nad pluginem — stan obecny

- **3.1 — specyfikacja zasad:** ukończona i aktualizowana po większych zmianach.
- **3.2 — pluginy uzupełniające:** istnieją już m.in. LuckPerms, WorldGuard/WorldEdit, Shopkeepers, voice chat i squaremap; trzeba potwierdzić ich aktualne wersje i zgodność z Paper 26.3. **Nowe zadania przed startem:** antycheat oraz przeciwdziałanie duplikacji, test kompatybilności między pluginami i kontrola logów. CoreProtect pozostaje kandydatem do instalacji, jeśli nie jest już dostępny.
- **3.3 — EarthSMP-Core:** na GitHub `main` jest już **1.8.8** (zweryfikowane 7.11.2026). **1.8.5** przeszła podstawowy test edytora i synchronizacji granic; **1.8.6** test GUI/HUD; **1.8.7 R3** wystartowała na Paper, lecz mechaniki multiplayer wciąż czekają na testy; **1.8.8** zawiera reguły ekwipunku/enchantów/wymiarów, wymagające potwierdzenia buildu i funkcji na serwerze. Poprawka wyjątków kusz (Multishot i Quick Charge) pozostaje do wdrożenia.
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
- [x] **WorldBorder na naszej mapie WWW** wdrożony w 1.8.10 i potwierdzony w podstawowym teście użytkownika; dodatkowe sprawdzenie zoomu i zmian granicy podczas próby generalnej.
- [ ] Edytor chunków i zatwierdzanie administracyjne działają lub są świadomie wyłączone przed startem, z komendami jako planem awaryjnym.
- [x] **30-sekundowe `/home` przez Title** — implementacja, build i podstawowy test w grze zaliczone.
- [ ] **Testy brzegowe `/home`:** anulowanie przy obrażeniach/ruchu, restart, brak zapisu po błędzie SQLite, limit 4/4 oraz przekroczenie 06:00 w trakcie odliczania.
- [x] **PvP/Combat Log:** osiem podstawowych scenariuszy zaliczonych w teście z 2 graczami 7.11.2026 (szczegóły §18.13).
- [ ] Zegar 06:00, limity `/home`, cooldown 6 h, oblężenia 10:00–00:00, upadek państwa i backupy mają zaliczone testy na docelowym środowisku.
- [ ] **Antycheat i antydupe** są dobrane do konkretnej wersji Paper/Java, skonfigurowane, przetestowane pod kątem fałszywych alarmów i znanych metod duplikacji; logi/alerty dostępne dla administracji. Bez automatycznych banów przed weryfikacją trafności wykryć.
- [ ] **Anti-Xray i Baritone:** na Earth włączyć i przetestować wbudowany Paper Anti-Xray (zalecany tryb 2 po kontroli FPS/TPS), skonfigurować osobno Nether pod ancient debris, zweryfikować mod/resource pack X-ray i automatyczne kopanie Baritone. Sam GrimAC nie gwarantuje rozpoznania wszystkich działań Baritone.
- [ ] **Monitor prób obejścia zabezpieczeń** zapisuje szczegóły zdarzenia, informuje administrację i przy uzasadnionym podejrzeniu nadużycia wyświetla graczowi komunikat ostrzegawczy. Nie oznacza zwykłych, przypadkowo niedozwolonych transakcji jako oszustwa.
- [x] **Shopkeepers:** użytkownik potwierdził działanie i testy systemu handlu. Pozostaje ustawienie sklepów administracyjnych na spawnie i ich asortymentu; próby odporności na dupe i przeciążenie będą częścią końcowych testów bezpieczeństwa.
- [ ] **Spawn Makau** przygotowany i chroniony; działają informacyjne tablice/zasady, sklepiki administracyjne, strefy ruchu graczy i zabezpieczenia przed griefingiem.
- [ ] Istnieje działający **backup i przetestowane odtworzenie** świata + SQLite; zapas dyskowy potwierdzony.
- [ ] Regulamin, sposób zgłaszania błędów, link do mapy i koszt utrzymania zostały podane graczom.

### 18.8. Etapy 1.8.7–1.8.8 i następne kroki

**1.8.7 R3 — implementacja gotowa do testów multiplayer:** PvP pokojowe i tarcza; combat tag 60 s z karą za wyjście; wojenny respawn 15–50 s z dużym centralnym odliczaniem; ochrona przypisanych łóżek. Potwierdzony jest tylko start pluginu, nie przypadki bojowe. Zbiorcza sesja z 3–4 graczami i minimum dwoma państwami jest obowiązkowa przed akceptacją.

**1.8.8 — paczka do lokalnego builda i testów:**

- Limity plecak + hotbar + offhand (i wyposażenie gracza): 8 zwykłych złotych jabłek, 3 perły, 16 pajęczyn, 3 totemy; zakaz posiadania zaklętych złotych jabłek, End Crystals i Respawn Anchors. Częściowe pobieranie przedmiotów ma pozostawiać resztę w źródle. Nietypowy nadmiar jest upuszczany na ziemię, nie kasowany.
- Mending zakazany; Fortune I od 7. dnia, III od 16., II nigdy. Zaklęcia walki I do dnia 9, II od dnia 10. **Wyjątki kusz z §11.2 są już w kodzie 1.8.13**, ale wymagają testów granicznych w grze.
- End zawsze zamknięty. Nether od **06:00 dnia serwera 10** (`progression.nether-unlock-day: 10`) — **decyzja ostateczna**, zgodna z implementacją.
- Brak nowego schematu SQLite: nadal **v10**. Nowa logika nie wymaga resetu danych, lecz pełnego backupu i testu ze starymi przedmiotami.
- **Kryterium akceptacji:** `./gradlew clean build` na Java 25, start pluginu i seria testów: częściowe podnoszenie, shift-click, drag, Creative, `/give`, enderchest, craft, villagery, kowadło, Mending, granice dni 6/7/9/10/15/16 oraz portale End/Nether i próby /tp.

**Przed sesją testową — zasady kusz już wdrożone w 1.8.13:** Multishot 0, Quick Charge II/III zależnie od dnia, Piercing I/II i Unbreaking III są w kodzie. Pozostało sprawdzenie gotowych przedmiotów, handlu i progów edycji na Paper.

**Po sesji testowej:** poprawki, aktualizacja stanu dokumentacji, audyt publicznego portalu, instalacja na hostingu z HTTPS, test backupu i obciążenia. **Gospodarka opiera się już na Shopkeepers**, wymaga jednak konfiguracji i testów; spawn Makau należy przygotować przed rozpoczęciem edycji. Nie planujemy pisać nowego pluginu sklepowego.

### 18.9. Najbliższe etapy do startu — antycheat, antydupe, sklepy i spawn (plan 7.11.2026)

Poniższa lista to **plan wdrożenia, nie potwierdzenie ukończenia**. Termin startu (rozważana niedziela o 18:00) pozostaje **warunkowy**, bez oficjalnej deklaracji, dopóki nie przejdą testy bezpieczeństwa, gry i hostingu.

1. **Zasady kusz wdrożone w 1.8.13:** Multishot zabroniony, Quick Charge II w pierwszej fazie / III od dnia 10, Piercing I/II i Unbreaking III na wszystkich przedmiotach. Build Java 25 zaliczony; pozostaje test na Paper dla gotowych przedmiotów z handlu, kowadła i starych zaklęć.
2. **Testy solo 1.8.8:** limity ekwipunku i nadwyżki, magazyny, handel z villagerami, kowadło, enchanting, losowe łupy, mechaniki Mending/Fortune, portale i teleportacje Nether/End; odblokowanie progów dni 7/10/16.
3. **Zbiorcze testy multiplayer 1.8.7–1.8.8:** minimum dwa państwa, PvP pokojowe i wojenne, tarcze, Combat Log 60 sekund i Alt+F4, opóźniony respawn, ochrona łóżek, oblężenia, przejmowanie rdzeni; oddzielnie kontrola widoczności pozycji graczy na portalu i obsługi jednoczesnych wniosków terytorialnych.
4. **Antycheat GrimAC — zainstalowany i podstawowo przetestowany:** ostrzeżenia/logowanie i ręczna ocena bez pochopnego automatycznego bana; nadal sprawdzić ping/lagi, elytry, perły, walkę, `/home`, fałszywe alerty i współdziałanie z pluginem.
5. **Better Anti-Dupe — zainstalowany i podstawowo przetestowany:** sprawdzić ataki z hopperami, kontenerami, restartami, Shopkeepers i lagiem; **instalacja nie gwarantuje braku dupe**. Testy destrukcyjne tylko na kopii świata i SQLite.
6. **Sklepy Shopkeepers — testy działania zaliczone:** nie wracamy do implementacji ani podstawowej konfiguracji systemu. Pozostało **na spawnie Makau** postawić NPC sklepów administracyjnych oraz ustawić konkretny asortyment i nielimitowane podstawowe wymiany barterowe. Sklepy graczy mogą działać poza spawnem, w dowolnym legalnym miejscu. Scenariusze dupe i dużego obciążenia sprawdzić podczas próby generalnej.
7. **Spawn Makau:** zbudować reprezentacyjny i czytelny spawn, oznaczyć zasady i ważne punkty, przygotować podstawowe sklepy administracyjne oraz ochronę WorldGuard/EarthSMP; zrobić spacer testowy z graczem bez uprawnień operatora.
8. **Mapa WWW i WorldBorder — wdrożone w 1.8.10:** obrys granicy i zabezpieczenia portalu są w kodzie; użytkownik potwierdził działanie. Dodatkowe testy zoomu, proxy HTTPS i separacji danych państw nadal wykonać przed publicznym udostępnieniem.
9. **Hosting i bezpieczeństwo WWW:** sprawdzić dostęp do portów, HTTPS, autoryzację mapy oraz brak możliwości pobrania pozycji obcych państw; zweryfikować zabezpieczenia `Host`/`Origin`, filtrowanie zasobów squaremap i integrację reverse proxy. Dodatkowo przejrzeć historię Git pod kątem przypadkowo publikowanych sekretów (nie zakładać ich wystąpienia bez potwierdzenia).
10. **Próba generalna przed otwarciem:** test z większą grupą, profilowanie wydajności/spark, kopia bezpieczeństwa **po wyłączeniu Paper**, sprawdzenie odzyskiwania danych i ustalenie procedury na krytyczne exploity. Dopiero po zaliczeniu tych testów administrator ogłasza termin i uruchamia zegar edycji; w razie błędu blokującego start przesuwamy, zamiast ryzykować utratę świata lub ekonomii.

**Aktualna kolejność prac (7.11.2026):** (1) GrimAC, Better Anti-Dupe oraz Shopkeepers działają w testach podstawowych; (2) EarthSMP-Core **1.8.13** przeszedł build i próbę działania, a spawn Makau ma w repo znacznik domku; (3) **audyt kodu i bezpieczeństwa** — weryfikacja scenariuszy poniżej; (4) **giga-testy multiplayer** i test odtwarzania backupu; (5) budowa i ochrona spawnu Makau, sklepy administracyjne oraz hosting z HTTPS. Nie mylić zaliczonego testu podstawowego z certyfikacją bezpieczeństwa.

**Warunki blokujące start:** nieprzetestowane lub niemożliwe do odtworzenia backupy, wykryty działający dupe/obejście ograniczeń bez poprawki, wyciek prywatnych pozycji lub haseł, niepoprawny Combat Log/war PvP, brak działającego serwera i portalu HTTPS, krytyczne problemy wydajnościowe. Kosmetyka spawnu może być skromniejsza, ale nie kosztem ochrony i czytelności.

### 18.10. Powiadomienia o próbach obejścia zabezpieczeń (moduł wdrożony od 1.8.11; wymagane testy)

System bezpieczeństwa powinien **rejestrować wykryte próby obejścia zabezpieczeń**, w szczególności przy handlu niedozwolonymi przedmiotami, manipulacji limitami ekwipunku, ochroną terytorium i podejrzanych operacjach mogących prowadzić do duplikacji. W razie wykrycia takiego zdarzenia **administracja otrzymuje powiadomienie z informacją o graczu, rodzaju i czasie zdarzenia, jego kontekście oraz wyniku blokady**, a odpowiedni wpis pozostaje w dzienniku bezpieczeństwa.

**Gracz może otrzymać czytelny komunikat ekranowy informujący o wykryciu próby oszukiwania lub obejścia zasad serwera.** Forma i treść komunikatu są elementem interfejsu, ustalanym na etapie implementacji; nie wymagają osobnego opisu w regulaminie.

- **Rozróżnić przypadkową operację od podejrzanego działania:** sam fakt, że naturalna oferta villagera zawiera niedozwolone zaklęcie, nie oznacza winy gracza. Jednorazową, zablokowaną transakcję można jedynie zalogować i objaśnić. Wyraźniejsze ostrzeżenia przewidzieć dla świadomych, ponawianych lub nietypowych prób.
- **Ograniczyć spam:** powiadomienia grupować i stosować krótkie ograniczenie częstości dla tego samego gracza i rodzaju wykrycia; pełne dane zachować w logach.
- **Bez automatycznej kary za sam alert:** blokada nielegalnej operacji pozostaje podstawową reakcją; karę nakłada administrator po analizie kontekstu, z uwzględnieniem fałszywych alarmów. W przyszłości można dodać widok administracyjny historii zdarzeń, np. `/esmp security`.
- **Stan 1.8.13:** moduł `SecurityAlerts` jest zaimplementowany, prowadzi zapis zdarzeń i informuje uprawnionych administratorów, a `/esmp security` pozwala obejrzeć historię. Testy jakości alertów i odporności na spam pozostają częścią audytu.

### 18.11. Dopieszczenia komunikatów gracza — wdrożone na `main` 1.8.13, podstawowy test zaliczony

**Zasada nadrzędna: nie zasłaniać ekranu podczas PvP i nie spamować pełnoekranowymi powiadomieniami.** `Title` to krótkie, okazjonalne podkreślenie istotnych wydarzeń, **nie zamiennik całego czatu**. Czatu nie usuwamy dla ważnych ogłoszeń: zachowuje nazwy państw, numery wojny, powody, instrukcje i historię zdarzeń. Krótkie limity/błędy to **ActionBar**, nie Title. Szczegółowe dane administracyjne, kody logowania do WWW, instrukcje komend i błędy SQLite pozostają na czacie/logach.

- **Duże Title:** wypowiedzenie wojny, zawarcie pokoju lub administracyjne zakończenie wojny; rozpoczęcie i zakończenie oblężenia, ostrzeżenie o 30 minutach tolerancji przy zbyt małej liczbie obrońców; zdobycie rdzenia i upadek państwa. `Title` oblężeń **zatwierdzone mimo ryzyka rozpraszania w PvP** — zostają do pierwszych rozgrywek, a jeżeli gracze będą narzekać, zmienimy je po starcie. BossBar postępu oblężenia nadal działa niezależnie.
- **Osobiste / lokalne Title:** założenie i rozwiązanie państwa, zaproszenie do państwa i wojny, dołączenie/wycofanie sojusznika, propozycja pokoju, zmiana rangi/przywództwa, wyznaczenie stolicy oraz postawienie rdzenia. Tylko adresaci, których dotyczy powiadomienie; bez globalnego informowania o zwykłych operacjach w GUI.
- **ActionBar:** pozostałe użycia Kilofa Oblężniczego, jego wytworzenie, odmowa zakazanego enchantu, blokada Netheru/Endu, limity ekwipunku, stan Combat Tag i tarczy PvP. Nieliczne ważne błędy lub polecenia z większą ilością szczegółów nadal w czacie.
- **Ochrona ekranu:** nie wyświetlać nowych ogólnych Title graczowi z aktywnym PvP Combat Tagiem (60 sekund), trwającym 30-sekundowym `/home` lub blokadą odrodzenia wojennego (15–50 sekund); nie nadpisywać ich liczników. Bez kolejki zaległych pełnoekranowych powiadomień. Jeśli Title zostało pominięte, **ważne wydarzenie nadal jest na czacie**. W testowym kodzie czas zwykłego Title to około 2 s i maks. jedno takie Title co 6 s na gracza. Nie generować powiadomień co sekundę.
- **Poprawiony komunikat:** po utworzeniu państwa kod podaje instrukcję `/esmp core free` zamiast nieaktualnej zapowiedzi wdrożenia rdzeni.
- **Stan:** zmiany już są na `main` 1.8.13; użytkownik potwierdził poprawny build i podstawowe działanie. `EventTitles` ogranicza zwykłe powiadomienia do jednego na 6 sekund i blokuje je podczas Combat Taga, `/home` i wojennego respawnu. Osobne przypadki wojenne sprawdzimy w audycie/giga-testach.

### 18.12. AUDYT EarthSMP-Core 1.8.13 — rozpoczęty 7.11.2026

**Zakres wstępnego przeglądu statycznego:** `MapPortalServer`, `MapPortalAuth`, `MapPortalSecurity`, `MapPortalPublicMarkers`, `MapTerritoryWeb`, `CoreProtection`, `CoreItems`, `SiegePickaxe`, `WarRepository`, `NationDatabase`, `HomeCommand`, `CombatTagListener`, `EventTitles` i `SquaremapCoreLayer`. To **nie jest pełny audyt penetracyjny ani wynik testów na żywym serwerze**.

- **Potwierdzone w kodzie:** HTTP `Origin`/`Host` kontrolowany dla mutacji, HttpOnly/SameSite dla sesji, hasła z PBKDF2, ograniczenie prób loginu, autoryzacja terytoriów po stronie Paper i transakcyjne zatwierdzanie w SQLite, filtrowanie publicznych warstw squaremap, brak wrażliwych pozycji w publicznych markerach. Sprawdzić to jeszcze aktywnie przez surowe API, konta z różnych państw i bez sesji.
- **Priorytet 1 (integracja ochrony):** `CoreItems.onPlace` zatwierdza zmianę w SQLite już w obsłudze zdarzenia `BlockPlaceEvent` na priorytecie HIGHEST; trzeba przetestować, czy WorldGuard/inny plugin może anulować ustawienie bloku po zapisaniu rdzenia, tworząc niespójność między bazą a światem. To **ryzyko integracyjne do odtworzenia**, a nie potwierdzony exploit.
- **Priorytet 2 (spójność `/home`):** `HomeCommand.complete` teleportuje gracza przed `recordHomeUse(...)`; przy wyjątkowej awarii zapisu SQLite teleportacja może być udana, ale bez pobrania limitu. Sprawdzić zachowanie i logi w kontrolowanym teście awarii; nie zmieniać zasady „opłata tylko po udanym teleportowaniu” bez osobnego projektu transakcji.
- **Priorytet 3 (magazyny i antydupe):** próby otwierania podwójnych skrzyń przez granicę rdzenia, hoppery/hopper minecarty, podnoszenie przedmiotów, inne GUI pluginów, transakcje Shopkeepers, klonowanie tokenu rdzenia i kilofa oraz przerwanie serwera między aktualizacją stanu świata i SQLite. Obecność handlerów w kodzie nie jest dowodem odporności na wszystkie obejścia.
- **Priorytet 4 (wojna):** obrona 20% online, czas 10:00–00:00 Europe/Warsaw, utrata ostatniego rdzenia, warstwy chronione podczas aktywnego szturmu, lista stron wojny po wycofaniu sojuszników, reset postępu po restarcie, opóźniony respawn i kara za Combat Log.
- **Priorytet 5 (publiczny portal):** niezalogowany użytkownik, członek innego państwa i administrator; sprawdzić `/api/positions`, `/api/territory/*`, ograniczenia liczby prób, reset sesji, właściwe `Host`/`Origin`, HTTPS reverse proxy i brak bezpośredniego dostępu do squaremap :8080. Test spawnu: sam domek w Makau, bez podpisu i bez wpływu na zaznaczanie chunków.
- **Priorytet 6 (operacyjny):** backup offline i pełne odtworzenie `world` + SQLite, uprawnienia plików, brak ujawnienia sekretów, test startu po restarcie, `gradlew clean build`, obserwacja TPS oraz test GrimAC/Better Anti-Dupe bez fałszywych blokad.

**Status audytu:** rozpoczęty od analizy `main`, bez potwierdzonych krytycznych exploitów na tym etapie. Wynik pozytywny dopiero po sprawdzeniu testów w grze i naprawieniu rzeczywistych usterek. Kodu nie zmieniamy w ramach samego aktualizowania dokumentacji.

### 18.14. Anti-Xray i Baritone — otwarte zadanie przed startem (7.11.2026)

**Status: do wdrożenia i przetestowania — jeszcze NIE potwierdzone.** X-ray w modzie i resource packu zdradza lokalizacje rud, a Baritone automatyzuje chodzenie i kopanie. Instalacja GrimAC ani Better Anti-Dupe nie zastępuje obfuskacji danych bloków wysyłanych do klienta.

- **Paper 26.3:** ma wbudowany Anti-Xray. W `config/paper-world-defaults.yml` (lub override `paper-world.yml` dla Earth) ustawić `anticheat.anti-xray.enabled: true` i rozważyć `engine-mode: 2`. Zachować poprawne `hidden-blocks` i `replacement-blocks`, dopasować `max-block-height` do faktycznej wysokości złóż w customowej mapie Earth. Wariant z `air` w `hidden-blocks` może obniżyć FPS klienta, więc testować przed użyciem produkcyjnym.
- **Nether:** osobna konfiguracja maskowania zwłaszcza `ancient_debris`, `nether_gold_ore` i `nether_quartz_ore`; End jest zamknięty regulaminowo.
- **Baritone:** Anti-Xray może odebrać wiedzę o ukrytych rudach, ale nie uniemożliwia automatycznego legalnego ruchu/kopania. GrimAC nie może być uznany za stuprocentowy detektor. Ewentualne alerty do ręcznej weryfikacji, bez automatycznych kar za same regularne działania.
- **Test z osobnym klientem bez OP:** uruchomić X-ray resource pack i mod po pełnym restarcie Paper; sprawdzić ukryte i odsłonięte rudy oraz zakres wysokości; w Netherze ancient debris; zweryfikować FPS/TPS i zachowanie Baritone. Sprawdzić, czy publiczny seed lub znane współrzędne mapy nie osłabiają ochrony.
- **Brak zmiany źródeł EarthSMP-Core:** konfiguracja Paper i testy serwera są osobnym działaniem.

### 18.13. Raport z testów multiplayer PvP / Combat Log — 7.11.2026

**Wersja: EarthSMP-Core 1.8.13. Testerzy: 2 osoby, dwa państwa. Źródło: raport administratora z gry. Wynik: 8/8 OK, bez zgłoszonych uwag.**

1. PvP podczas pokoju — **OK** (brak uwag).
2. Wypowiedzenie wojny — **OK** (brak uwag).
3. Combat Tag 60 sekund — **OK** (brak uwag).
4. Combat Log: wyjście podczas walki — **OK** (brak uwag).
5. Combat Log: ponowne wejście — **OK** (brak uwag).
6. Bezpieczne wyjście po wygaśnięciu Combat Taga — **OK** (brak uwag).
7. Wojenny respawn przy 1–2 osobach online — **OK** (brak uwag).
8. Zawarcie pokoju i przywrócenie reguł PvP — **OK** (brak uwag).

**Zakres potwierdzenia:** podstawowa ścieżka PvP oraz Combat Log. To nie oznacza jeszcze zaliczenia testów granicznych (awarie, przeciążenie, ponad dwie osoby, restart w środku walki) ani pełnego audytu bezpieczeństwa. **Następny test: legalne oblężenie, spór o strefę 9×9, wymuszanie blokad stawiania bloków, loot dopiero po zdobyciu i przejęcie/rozpad państwa. Testy niszczące świat lub bazę wykonać na kopii.**

## 19. Szczegóły do rozstrzygnięcia podczas projektowania technicznego (NIE nowe reguły)

Dla przejrzystości poniższe elementy nie są ukrytymi „zatwierdzonymi zasadami”; to techniczne wyjątki wymagające rozpisania implementacji:

1. **Techniczny wygląd blokady respawnu (reguły rozstrzygnięte):** wybrać ekranowy sposób prezentacji 15–50 s, zabezpieczyć pełny brak ruchu i interakcji oraz nietykalność *podczas* odliczania. Po odliczaniu brak ochrony. „Zbrojownia” nie jest specjalną strefą.
2. **Wyjątki enchantów — rozstrzygnięte:** obowiązuje lista i poziomy z 1.8.8. Nie dodawać dalszych wyjątków dla mobilności lub przeżywalności bez nowej decyzji.
3. **Odporność systemu ochrony:** pełna lista wyjątków fizyki, tłoków, eksplozji, zastępowania bloków, redstone oraz obrony łóżek i ich podpór.
4. **Precyzyjna mechanika startu oblężenia:** rozróżnienie obecności wrogiego gracza w chronionej bazie, startu ataku przy progu online i prowadzenia licznika; nie rozszerzać blokad na pokój lub neutralnych.
5. **Mapa prywatnych pozycji i jawnych rdzeni:** od 1.8.4 istnieje uwierzytelnienie do pozycji współobywateli i filtrowanie po stronie serwera; potrzebne są testy wielu państw oraz audyt publicznych endpointów. **Pozycje wszystkich rdzeni są publiczne, ale pozycje graczy pozostają prywatne.**
6. **Edytor terytoriów WWW (kierunek zatwierdzony):** autoryzacja konta Minecraft, narzędzia zaznaczania wielu chunków (również mobilnie), podgląd wniosków, panel admina i walidacja backendowa. Sam wybór mapy WWW zamiast moda jest już rozstrzygnięty.
7. **Zegar i `/home`:** podstawowa teleportacja i limity wdrożone od 1.8.2; **nowy TODO: 30 sekund odliczania na ekranie przed teleportacją**, anulowanie po rzeczywistym ruchu/obrażeniach, bez naliczania nieudanych prób (szczegóły §15.2). Nadal przetestować strefę `Europe/Warsaw`, zmianę czasu, restarty i czytelność komunikatów. Zachować 06:00, dni 1–2 bez limitu i 4 użycia od dnia 3.
8. **Nether — rozstrzygnięte:** start o **06:00 dnia 10** (`Europe/Warsaw`), zgodnie z aktualnym kodem. Dawne sformułowania „po 10 dniach” są nieaktualne.
9. **Interfejsy i konkretne pluginy:** wygląd ikony państwa, szczegóły bezpiecznego miejsca przy stolicy, współdziałanie sidebaru z innymi scoreboardami oraz adminowskie komendy ręcznej korekty. Sam przełącznik `/esmp sidebar` jest już zaimplementowany. To decyzje techniczne, nie nowe zasady.

**Koniec dokumentacji v1.8.8.** Decyzje administracyjne z 7 listopada 2026 r. rozstrzygają próg Netheru, wyjątki enchantów oraz pozyskiwanie pierwszej pary villagerów; mechanizmy PvP i 1.8.8 wymagają dalszych testów.


<!-- Historia aktualizacji v1.7: GUI 1.8.6, rebranding R2, hotfix R3; informacje o main uaktualniono w v1.8. -->

<!-- Aktualizacja v1.8: 1.8.7 R3 start bez testów PvP; 1.8.8 przygotowana do builda; limity/enchanty/wymiary, próg Netheru do potwierdzenia; audyt dzienny w osobnym pliku. -->


<!-- Aktualizacja v1.8.1: potwierdzone 06:00 dnia 10, enchanty bez zmian względem 1.8.8, pierwsza para villagerów z leczenia (później rozmnażanie), main 1.8.8 oraz procedura porządkowania repo. -->

<!-- Aktualizacja v1.8.2: wyjątki dla kuszy (Multishot zakaz, Quick Charge II/III, Piercing I/II); zmiany w kodzie odłożone na następną sesję. -->
<!-- Korekta v1.8.3: Unbreaking III zawsze i na wszystkich przedmiotach, również kuszach; kod 1.8.8 już spełnia tę regułę. -->

<!-- Aktualizacja v1.8.4: zbiorcza tabela limitów enchantów (§11.3), trzy momenty zmian: dni 7/10/16; bez zmian w kodzie Javy. -->

<!-- Aktualizacja v1.8.5: potwierdzony plan sklepów Shopkeepers i spawnu Makau, dobór antycheat/antydupe do weryfikacji, kolejność testów przed warunkowym startem; bez zmian w kodzie. -->

<!-- Aktualizacja v1.8.6: TODO obrys WorldBorder na autorskiej mapie WWW; granica widoczna w squaremap :8080, lecz brak jej na naszym portalu. Bez zmian w kodzie. -->

<!-- Korekta v1.8.7: villagery z leczenia to konsekwencja mapy bez wiosek, nie ograniczenie regulaminowe/kodowe; main ma 1.8.9, potwierdzono lokalnie start, limity EQ, enchanty w ekwipunku i wymiary; test ofert villagerów w toku. -->

<!-- Aktualizacja v1.8.8: monitor podejrzanych prób obejścia zabezpieczeń, alerty admina i komunikaty dla gracza; bez zmian w kodzie. -->

<!-- Aktualizacja planu 7.11.2026: po instalacji antycheata i antydupe zaplanowano odświeżanie ikon rdzeni on-demand + lekki self-healing; Shopkeepers przetestowany, do wykonania tylko sklepy administracyjne na spawnie. Bez zmian w kodzie Core. -->

<!-- 7.11.2026: zatwierdzony TODO /home: 30 s przygotowania z ekranowym licznikiem, anulowanie po ruchu i otrzymaniu obrażeń; brak implementacji w bieżącym HomeCommand. -->

<!-- 7.11.2026: zatwierdzone dopieszczenia 1.8.13: desktop LPM/PPM/ŚPM, mobile przełączniki; selektywne Title bez zakrywania PvP/home/respawnu, część powiadomień jako ActionBar; mniejsza częstotliwość squaremap. Kod wyłącznie ZIP testowy. -->
