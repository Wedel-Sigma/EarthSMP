---
title: "Earth SMP — specyfikacja projektu v1.0"
version: "1.0"
date: "2026-11-05"
status: "Zamknięcie etapu projektowania mechanik 3.1; bez implementacji pluginów gameplayowych"
language: "pl"
---

# EARTH SMP — KOMPLETNA SPECYFIKACJA v1.0

**Stan ustaleń: 5 listopada 2026 r.**  
**Przeznaczenie:** dokument źródłowy dla administratorów, programowania pluginu *EarthSMP Core*, wyboru pluginów gotowych oraz odtworzenia kontekstu w następnej rozmowie z ChatGPT.  
**Skala:** prywatny serwer około 30 znajomych, zwykle około 5 osób w państwie; light roleplay, polityka, gospodarka i wojny.

> **Zasada pierwszeństwa:** wszystkie uzgodnione poniżej zasady nowej edycji mają pierwszeństwo przed regulaminem poprzednich edycji. Ze starego regulaminu obowiązuje wszystko, co nie koliduje bezpośrednio z nowymi zasadami. Nie przedstawiać propozycji jako decyzji zatwierdzonych.

## 0. Skrót kluczowych reguł

- Każdy gracz należy do jednego państwa; nawet jednoosobowego. Granice polityczne to kolorowe, zatwierdzane przez administratora **chunki**, ale same granice nie chronią bloków.
- Każde państwo dostaje **1 darmowy rdzeń**, może wykonać **2 dodatkowe**; maksimum **3 rdzenie własne**. Rdzenie zdobyte w wojnach **nie liczą się do limitu**.
- Każdy rdzeń chroni **65 × 65 bloków**, w całej wysokości Overworldu (**Y −64…319**). Obszary ochronne nie mogą na siebie nachodzić, ale mogą sąsiadować. Nowy rdzeń można postawić tylko na zatwierdzonym własnym terytorium; zdobyty rdzeń jest wyjątkiem od wymogu stawiania.
- Formalna wojna pozwala na oblężenia między **10:00 a 00:00 Europe/Warsaw**, gdy online jest co najmniej **20% całego składu broniącego państwa** (zaokrąglenie w górę; min. 1). Spadek poniżej progu w trakcie daje **30 minut** na dokończenie; o północy koniec bez wyjątku.
- Atakujący w chronionej bazie mogą niszczyć **wyłącznie bloki postawione przez graczy**, specjalnym **Kilofem Oblężniczym**. Jeden kilof niszczy **15 bloków** i znika. Gdy podczas legalnego oblężenia wróg przebywa w chronionym regionie, **nikt nie może tam stawiać bloków**.
- Strefa przejmowania wokół rdzenia: **9 × 9 bloków**, Y rdzenia **−5…+10**. Wymagane **480 s**; atakujący bez obrońcy +1 s/s; obie strony w strefie: pauza; sami obrońcy: −2 s/s. Koniec oblężenia lub postęp 0 resetuje przypisanie zdobywcy; przerwanie oblężenia resetuje też cały postęp.
- Z państwa mającego **x rdzeni można zdobyć maksymalnie x−1**; ostatni rdzeń **znika**, a państwo **upada**. Całe jego terytorium staje się neutralne; granice później przydziela admin zgodnie z ustaleniami graczy.
- W czasie pokoju ochrona PvP we własnej bazie; na własnym terytorium politycznym po pierwszej próbie ataku **8 s obronnej nietykalności, cooldown 5 min**, pierwszy cios anulowany. W wojnie PvP działa normalnie; neutralne państwa zachowują pokojowe zabezpieczenia.
- Podczas wojny czas odrodzenia zależy od członków **własnego państwa online**: 1–2 = 15 s; 3–5 = 20 s; 6–10 = 30 s; 11–15 = 40 s; 16+ = 50 s. W pokoju brak timera. **Nie ma bezpiecznego respawnu w opanowanej zbrojowni: przeciwnicy mogą zabić odradzającego się natychmiast; brak nietykalności przy łóżku.**
- Limity w **całym ekwipunku przez cały czas**: zwykłe złote jabłka 8; perły Endu 3; pajęczyny 16; totemy 3. Zaklęte złote jabłka, End Crystals i Respawn Anchors — całkowicie wyłączone. Po śmierci **cały ekwipunek wypada**.
- **Nether** dostępny po 10 dniach; **End** wyłączony na stałe. Zaklęcia przez handel z villagerami uzyskanymi przez leczenie zombie villagerów; Mending zakazany. Fortune: brak do dnia 6, I od dnia 7, III od dnia 16, Fortune II nigdy. Enchanty wpływające na PvP: maks. I przez pierwsze 9 dni, następnie maks. II.
- Barter **przedmiot za przedmiot**, bez wirtualnej waluty. Gracze: sklepy skrzynkowe. Admin: nieskończone wymiany podstawowych materiałów (np. cobblestone ↔ cobbled deepslate 1:1). Mapa przeglądarkowa koloruje chunki państw; pokazuje lokalizację na żywo **tylko własnego państwa**; rdzenie nigdy nie są zaznaczone.

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

### 1.2. Backupy

- Gotowy skrypt **EarthSMP-Backup.ps1**, PowerShell 5.1 z RCON/mcrcon: powiadomienie graczy, `save-off`, `save-all flush`, kopia, `save-on`, archiwum ZIP i kontrola poprawności; powiadomienia o rezultacie.
- Backup testowy: około **427,4 MB**, **14,6 s**; sprawdzono rozpakowanie oraz **uruchomienie osobnego serwera z kopii**.
- Harmonogram co 2 godziny został przetestowany, a następnie **usunięty celowo** na czas testowania; regularny backup wróci po ustaleniu hostingu. Backup ręczny pozostaje dostępny.
- Żadne gameplayowe pluginy nie zostały jeszcze wdrożone. Ten dokument jest projektem do zrealizowania, a nie opisem uruchomionych funkcji.

## 2. Państwa i terytoria

- Członkostwo w państwie jest **obowiązkowe regulaminowo**. Dopuszczone państwa jednoosobowe, grupy i organizacje najemnicze. Jeden gracz może należeć do tylko **jednego** państwa.
- Rangi: **1 przywódca**, do **2 zastępców**, dowolni członkowie. Przywódca i zastępcy mają **identyczne uprawnienia funkcjonalne**, np. zaproszenia, wyrzucanie, dyplomacja i zarządzanie państwem.
- Państwo można zakładać poprzez system serwera/GUI; otrzymuje jeden darmowy rdzeń.
- Terytorium polityczne jest zbiorem **chunków** oznaczonych kolorem państwa. Państwo proponuje zmianę granic, jeden z administratorów ją zatwierdza. Nie ma rozbudowanych punktów ekspansji czy sztywnych limitów terytorium. Granice nie powinny nakładać się na inny zatwierdzony obszar bez zgody/rozstrzygnięcia administracji.
- **Granice polityczne nie chronią bloków** i same w sobie nie blokują wejścia. Ochronę fizyczną dają tylko rdzenie i obszary publiczne.
- Brak automatycznego wykrywania casus belli i kar za samo wejście na cudze terytorium. Spory o granice są podstawą dyplomacji/RP.
- Nie wolno **dobrowolnie rozwiązać państwa w czasie wojny**. Administrator może zarządzać wszystkimi jednostkami i dokonać ręcznej korekty, gdy mechanika wyjątkowego przypadku wymaga interwencji.
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
- Co najmniej jeden rdzeń państwa jest wyznaczony jako **stolica** (gdy istnieje). Zmiana wymaga decyzji administratora. Jeśli stolica zostanie zdobyta, `/home` nie działa aż do wyznaczenia nowej stolicy przez administratora.

### 3.1. Strefa bezpośrednio przy rdzeniu

- Strefa **9 × 9 bloków w X/Z**, od **Y rdzenia −5 do Y rdzenia +10** włącznie, jest zarówno obszarem kontroli przejmowania, jak i obszarem ograniczeń budowlanych.
- Nie wolno w niej stawiać **łóżek**, **obsydianu**, **płaczącego obsydianu**, **bloków netherite**, **reinforced deepslate**, **ancient debris** i podobnych skrajnie odpornych/niemożliwych do normalnego zniszczenia bloków (np. bedrock i bloki administracyjne, gdyby zostały uzyskane).
- Materiały wokół rdzenia mają pozwalać na uczciwe przebicie obrony; poza tą strefą można budować zwykłe, nawet rozległe systemy murów i fortyfikacji. Nie wolno obejść zasady, zamykając rdzeń w naturalnej nieprzebijalnej skale.

## 4. Rdzenie — crafting i koszty

### 4.1. Dodatkowy Rdzeń Ochronny

Własny crafting o układzie **3 × 3**; w **centrum 1 Jukebox (szafa grająca)**, a w ośmiu pozostałych miejscach po **64 sztuki**:

| Górny rząd | Górny rząd | Górny rząd |
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
- Działa do przełamywania ochrony tylko przy **formalnej wojnie, aktywnym legalnym oblężeniu i na wrogim chronionym terenie**, wyłącznie na **blokach postawionych przez graczy**.
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
- Podczas legalnego oblężenia atakujący mogą **otwierać i opróżniać skrzynie** przeciwnika w chronionej bazie.
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

- W okresie blokady odradzający się gracz nie może się przemieszczać, walczyć, używać ekwipunku ani liczyć się do przejmowania rdzenia. Czas nie zmienia się wskutek późniejszego wejścia/wyjścia innych członków państwa.
- **Najnowsza, nadrzędna decyzja:** gdy gracz odradza się przy łóżku w zbrojowni zajętej przez wroga, **można go zabić natychmiast — zero ochronnej nieśmiertelności przy respawnie**. Wcześniejsza propozycja bezwzględnej nietykalności respawnującego się gracza **nie obowiązuje w takim przypadku**. Implementacja nie może dodawać dodatkowego bezpiecznego okresu po timerze. Techniczny moment pojawienia się fizycznej postaci podczas odliczania wymaga spójnej implementacji, bez podważania tej decyzji.
- Nie wolno stawiać łóżek w strefie **9 × 9, Y rdzenia −5…+10**.
- W chronionej **własnej** bazie można posiadać dowolną liczbę łóżek (nawet setki). **Niezniszczalne podczas oblężenia są tylko łóżka przypisane jako punkt odrodzenia konkretnego gracza oraz bloki bezpośrednio pod nimi**. Ochrona trwa także, gdy przypisany gracz jest offline. Pozostałe łóżka nie otrzymują specjalnego immunitetu.
- Obce łóżka poza chronionym regionem nie mają tej ochrony; nie można stworzyć niezniszczalnej wysuniętej bazy łóżkami. Tłoki, eksplozje i usuwanie podparcia nie mogą obchodzić ochrony przypisanych łóżek.
- Zbrojownia to strategiczny punkt: podczas legalnego oblężenia napastnicy mogą otwierać i zabierać zawartość skrzyń, blokować dojście do ekwipunku i eliminować odradzających się obrońców.

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
- **Zakaz wykorzystywania braku claima** do griefingu i kradzieży w czasie pokoju. W wojnie dopuszczalne są działania opisane w specyfikacji, również rabunek wrogich skrzyń podczas legalnego oblężenia.
- **Nemosowanie zakazane**: zamykanie/zabudowywanie przeciwnika blokami w sposób uniemożliwiający obronę, by bić go np. po nogach. Normalne mury i barykady nie są automatycznie tym samym.
- **Zakaz BowDamagePvP**: spamowania strzałami z efektem Instant Damage.
- **Zakaz TNT Minecart PvP**, pułapek opartych na wagonikach TNT; **zakaz wybuchających łóżek jako PvP** w Netherze (wydobywanie jest dozwolone); **zakaz pułapek portalowych**.
- **Zakaz modów pokazujących serca / dokładne HP innych graczy**.
- Limity ekwipunku ze starego regulaminu przyjęto jako **maksymalne sumy w całym EQ**: 8 zwykłych złotych jabłek, 3 perły, 16 pajęczyn; dodatkowo nowe max 3 totemy.
- **Zmiana względem starej edycji:** stary całkowity zakaz PvP bez wypowiedzenia wojny **nie obowiązuje**; zastępuje go nowy system PvP pokojowego. Stary bezwzględny zakaz każdej kradzieży i griefu ma wyjątki działań wojennych uzgodnione wyżej. Stary zakaz eksplozji łóżek w Endzie jest bezprzedmiotowy, bo End nie działa.
- Zakazy czysto społeczne, takie jak nemosowanie, portal trapping i TNT Minecart PvP, pozostają **regulaminowe**, nie trzeba ich automatycznie kodować.

## 15. Teleportacja i stolice

- W czasie pokoju gracz należący do państwa posiadającego rdzeń może użyć **`/home`** (alternatywnie alias w stylu `/tp home`) w celu przeniesienia się do bezpiecznego miejsca przy **jednym rdzeniu stołecznym** tego państwa.
- Pozostałe rdzenie **nie są** punktami fast travel. Nie ma wyboru dowolnej twierdzy.
- Gdy państwo uczestniczy w **jakiejkolwiek wojnie**, `/home` zostaje **całkowicie wyłączone** do końca udziału w wojnie.
- **Zmianę stolicy zatwierdza administrator**; przywódca nie może zmieniać jej co chwilę sam, aby teleportować armię po świecie.
- Po zdobyciu stolicy, dopóki admin nie wyznaczy nowej, `/home` jest nieczynne. Klasyczny respawn przy łóżku pozostaje niezależny od `/home`.

## 16. Mapa polityczna i widoczność graczy

- Przeglądarkowa mapa świata w stylu dawnych Earth SMP: geografia + **półprzezroczyste kolorowe zaznaczenie chunków** należących do poszczególnych państw. Każde państwo ma kolor; obszar neutralny jest bez koloru. Po kliknięciu terenu pojawia się nazwa państwa.
- Polityczne granice i ochrona 65×65 rdzeni to **dwie odrębne warstwy systemu**.
- Zatwierdzona przez administratora zmiana granic trafia na mapę.
- **Lokalizacji rdzeni nigdy nie pokazujemy** na publicznej mapie, żeby zwiad miał znaczenie.
- Pozycje graczy są widoczne **na żywo** tylko dla **członków tego samego państwa**, po autoryzacji konta. Nie pokazujemy innych państw, wrogów, neutralnych ani nawet sojuszników. Na mapie publicznej nie pokazujemy nikogo.
- Lokalizacje odświeżają się na bieżąco, bez sztucznego opóźnienia (praktycznie co kilka sekund według możliwości integracji).
- Po wejściu do **Netheru** znacznik gracza znika z mapy Overworldu i wraca po powrocie. **Nie robimy osobnej mapy politycznej Netheru.**
- Technicznie widoczność musi wynikać z uwierzytelnienia i serwerowego filtrowania danych — ukrycie markerów w JavaScripcie przeglądarki nie zabezpiecza współrzędnych wroga.
- Narzędzie typu Dynmap/BlueMap z polityczną warstwą i kontrolą dostępu zostanie wybrane i zweryfikowane dopiero na etapie 3.2.

## 17. Administracja i wyjątkowe przypadki

- Administratorzy mają **pełną kontrolę nad wszystkimi państwami, rdzeniami, członkostwem, wojnami, granicami i ochroną**. Błędy lub nietypowe zakończenia konfliktów można ręcznie naprawić.
- W trakcie wojny dobrowolne rozwiązanie państwa jest zablokowane. Jeśli państwo **przymusowo upada** po utracie ostatniego rdzenia, likwidację równoległych wojen i relacji koalicyjnych może przeprowadzić administrator.
- Jeżeli jeden rdzeń jest stołeczny i zostanie zdobyty, nie ustawia się nowej stolicy automatycznie — admin wybiera ją ręcznie.
- W czasie pokoju gracz bez państwa (np. bezpośrednio po upadku) powinien jak najszybciej dołączyć do innego albo założyć nowe; obowiązek jest regulaminowy, nie trzeba budować skomplikowanego systemu kar za chwilowy stan przejściowy.
- System rozpoznawania pochodzenia bloków należy uruchomić **zanim gracze zaczną budować** na docelowym serwerze; trzeba uwzględnić tłoki, grawitację, płyny, automatyczne farmy, budowanie zniszczonych struktur i zmiany właściciela.

## 18. Wdrożenie — plan i zakres przyszłych pluginów

**Etap 3.1 (ten dokument):** opis mechanik. Nie oznacza funkcji już zainstalowanych.

**Etap 3.2:** dobór pluginów gotowych, np. do uprawnień i ochrony obszarów administracyjnych (LuckPerms, WorldGuard/WorldEdit), logów i cofania griefu (np. CoreProtect), barterowych Chest Shops i komunikacji głosowej. Dokładną zgodność z aktualnym Paper zweryfikować przed instalacją.

**Etap 3.3:** własny plugin **EarthSMP Core** obejmujący państwa i członkostwo, granice, dyplomację, wojny, rdzenie i obszary ochrony, pobierający stacki custom crafting, kilof 15 bloków, śledzenie pochodzenia bloków, oblężenia 20%/10–00/30 min, przejmowanie rdzeni, respawn, PvP terytorialne, limity ekwipunku, enchanty i progresję, bezpieczne `/home`, mapę polityczną / API. Trzeba zaplanować trwałą bazę danych, uprawnienia adminów, logi i system odtwarzania.

**Kolejne etapy:** pregenerowanie mapy/LOD dla Distant Horizons, modpack QoL/wizualny zgodny z AMD i NVIDIA, własny **instalator Windows w C#** umożliwiający łatwe przygotowanie klienta, docelowy hosting i automatyczne backupy.

## 19. Szczegóły do rozstrzygnięcia podczas projektowania technicznego (NIE nowe reguły)

Dla przejrzystości poniższe elementy nie są ukrytymi „zatwierdzonymi zasadami”; to techniczne wyjątki wymagające rozpisania implementacji:

1. **Respawn w okupowanej zbrojowni:** jak dokładnie pokazać 15–50 s bezruchu bez przyznawania zabronionej przez użytkownika nietykalności po respawnie; gracz przy łóżku może zostać zabity od razu.
2. **Dokładna lista zaklęć QoL:** pełne wyjątki dla butów/hełmu, zwłaszcza enchanty jednocześnie poprawiające mobilność w PvP. Pozostałe zaklęcia podlegają ogólnemu limitowi I/II.
3. **Odporność systemu ochrony:** pełna lista wyjątków fizyki, tłoków, eksplozji, zastępowania bloków, redstone oraz obrony łóżek i ich podpór.
4. **Precyzyjna mechanika startu oblężenia:** rozróżnienie obecności wrogiego gracza w chronionej bazie, startu ataku przy progu online i prowadzenia licznika; nie rozszerzać blokad na pokój lub neutralnych.
5. **Mapa prywatnych pozycji:** system logowania i autoryzacji do współobywateli; aktualizacja „na żywo” bez publikacji ukrytych pozycji klientowi.
6. **Interfejsy i konkretne pluginy:** wybór narzędzi oraz adminowskie komendy ręcznej korekty; to decyzje implementacyjne.

**Koniec specyfikacji v1.0.** Dokument można wysłać jako załącznik `.md` na Discordzie lub wgrać do nowej rozmowy, aby odtworzyć ustalenia. Wszelkie późniejsze zmiany należy dopisywać w nowej wersji dokumentu, a nie traktować dawnych propozycji jako obowiązujących.
