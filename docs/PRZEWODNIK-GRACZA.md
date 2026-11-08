# 🌍 KABINA NARODÓW 2.0 — PRZEWODNIK GRACZA

**Zasady i instrukcja gry · EarthSMP-Core 1.8.13 · 8 listopada 2026**

> **Zacznij tutaj, nawet jeśli nigdy nie grałeś na EarthSMP.** Ten poradnik opisuje to, co powinien wiedzieć gracz: państwa, GUI, rdzenie, mapę, wojny, handel, enchanty i najważniejsze zakazy. Nie wymaga znajomości komend administracyjnych.
>
> **Główna komenda serwera: `/esmp`.** Państwami, granicami, wojnami i rdzeniami zarządzasz klikając ikony w GUI. Powrót do stolicy i zakładanie konta mapy korzystają z **podkomend `/esmp`**. **Wyjątek: sklepy graczy korzystają z osobnego pluginu Shopkeepers i polecenia `/shopkeeper`** — instrukcja w §12. Nie musisz używać osobnych komend do obsługi państw i wojen.
>
> **Przedpremierowo:** serwer działa, ale edycja nadal jest na **dniu 0**. Czas rozpoczęcia i ewentualne zmiany adresów ogłosi administracja na Discordzie.

---

## 1. Pierwsze 5 minut

1. Zdobądź dostęp do **whitelisty** od administracji.
2. Uruchom **Minecraft Java Edition**. Aktualny adres serwera: **`91.90.81.190:25565`** (jeśli zostanie zmieniony, obowiązuje adres podany na Discordzie).
3. Po wejściu na serwer wpisz w czacie **`/esmp`**.
4. Kliknij ikonę **Główka gracza — „Państwo”**. Załóż państwo lub przyjmij zaproszenie do istniejącego.
5. Jeśli jesteś liderem, przejdź do **Magnetytu — „Rdzenie”** i odbierz **darmowy rdzeń**.
6. Wejdź na **[mapę polityczną](https://91-90-81-190.sslip.io)**, aby zobaczyć geografię, granice, rdzenie i WorldBorder.

**Najważniejsza różnica względem zwykłego SMP:** kolorowe terytorium to **granica polityczna**, nie ochrona przed kopaniem. Budynki zabezpieczasz **Rdzeniem Ochronnym**, który osłania **65 × 65 bloków**.

## 2. Menu `/esmp` — co kliknąć?

| Ikona w Minecraft | Nazwa w menu | Do czego służy |
|:---:|---|---|
| 🧑 **Główka gracza** | **Państwo** | Zakładanie kraju, członkowie, zaproszenia, rangi |
| 🗺️ **Wypełniona mapa** | **Terytoria** | Sprawdzanie chunków, wnioski o granice |
| 🧭 **Magnetyt (lodestone)** | **Rdzenie** | Darmowy rdzeń, stan, przenoszenie, stolica |
| 🪖 **Złoty hełm** | **Ranking państw** | Ranking według **postawionych** rdzeni własnych i zdobycznych |
| 🧭 **Kompas** | **Przełącz panel boczny** | Pokazanie/ukrycie danych o dniu, państwie, wojnie i limicie powrotów |
| 📄 **Papier** | **Zaproszenia** | Otrzymane zaproszenia do państw |
| ⚔️ **Żelazny miecz** | **Dyplomacja** | Wojny, sojusznicy, propozycje pokoju |
| ⛏️ **Żelazny kilof** | **Oblężenia** | Status i zasady zdobywania baz |
| ⚙️ **Komparator** | **Ustawienia** | Obecnie ekran „W przygotowaniu” |
| 🚪 **Drzwi / Bariera** | **Powrót / Zamknij** | Nawigacja w GUI |

*Emoji pomagają rozpoznać kategorię; rzeczywistą ikoną jest przedmiot Minecrafta wskazany pogrubioną nazwą. W każdym menu czytaj też podpowiedź po najechaniu myszką.*

## 3. Państwo: utworzenie, dołączenie i rangi

### Chcę założyć państwo

**`/esmp` → 🧑 „Państwo” → 💚 szmaragd „Załóż państwo”**.

Po kliknięciu wpisz **samą nazwę** na czacie (to nie jest komenda). Dopuszczalne są **3–32 znaki**: litery, cyfry, spacja, `_` i `-`. Masz **2 minuty**, a wiadomość z nazwą **nie jest publikowana na czacie**. Wpisz `anuluj`, aby zrezygnować.

Zostajesz **liderem**, a nowe państwo ma prawo do **jednego darmowego rdzenia**. Musisz go jeszcze odebrać w zakładce **Rdzenie**.

### Chcę dołączyć do państwa

Poproś lidera lub zastępcę o zaproszenie. Następnie:

**`/esmp` → 📄 „Zaproszenia” → wybierz państwo → 🟩 „Przyjmij”**.

Jeden gracz może należeć tylko do **jednego** państwa. Państwo jednoosobowe jest dozwolone, a przynależność do jakiegoś państwa jest wymagana regulaminowo.

### Rangi i zarządzanie

| Ranga | Możliwości |
|---|---|
| 👑 **Lider** (1 osoba) | Zarządzanie państwem, członkami, dyplomacją i rdzeniami; **jako jedyny może rozwiązać państwo** |
| ⭐ **Zastępca** (maks. 2) | Zaproszenia, członkowie, wojny, propozycje granic, rdzenie i zmiana stolicy |
| 👤 **Członek** | Normalna gra, budowanie i walka; bez zarządzania krajem |

**Lider i zastępca:** 🧑 **Państwo → „Zaproś gracza”** (zielony barwnik) → wybierz gracza online bez państwa → potwierdź. W **„Członkowie”** (główka gracza) można wybrać osobę i ją awansować, zdegradować lub wyrzucić; lider może przekazać przywództwo. **„Wysłane zaproszenia”** (książka z piórem) pozwala wycofać zaproszenie.

**Odejście i rozwiązanie:** 🧑 **Państwo → „Opuść państwo” / „Rozwiąż państwo”** (czerwony barwnik) → dodatkowe potwierdzenie. **Rozwiązanie jest możliwe tylko liderowi i tylko w pokoju**. Usuwa **wszystkie aktualnie posiadane rdzenie — również zdobyczne** — oraz państwo i jego granice. Budynki pozostają bez państwowej ochrony. Uważaj: to nie jest zwykłe opuszczenie drużyny!

## 4. Terytoria — jak zdobyć obszar na mapie?

Granice są tworzone z **chunków 16 × 16**, które administracja zatwierdza po otrzymaniu wniosku. **Nie ma automatycznego claima przez samo chodzenie**.

### Zalecane: mapa WWW

1. Wejdź na **[mapę](https://91-90-81-190.sslip.io)** i zaloguj się. Instrukcja pierwszego powiązania konta jest w **§13**.
2. Jako **lider lub zastępca** wybierz **„Włącz zaznaczanie na mapie”** w panelu **„Terytoria — edytor chunków”**.
3. Na komputerze: **LPM** zaznacza (także przeciąganiem), **PPM** wymazuje zaznaczenie, **przytrzymana ŚPM** przesuwa mapę, **kółko** przybliża/oddala. Na telefonie użyj widocznych przycisków **„Zaznaczaj” / „Wymazuj” / „Przesuwaj mapę”**.
4. Wybierz **rodzaj wniosku: dołączenie albo oddanie terenu**, sprawdź zaznaczenie i kliknij **„Wyślij wniosek”**.
5. **Dopiero po akceptacji administratora** teren otrzyma kolor Twojego państwa. Możesz też przeglądać własne wnioski.

### Alternatywa: menu Minecraft

**`/esmp` → 🗺️ „Terytoria”**. Użyj **czerwonego banera „Róg 1 — tutaj”** i **zielonego banera „Róg 2 — tutaj”** w dwóch punktach w świecie; następnie **„Podsumowanie zaznaczenia”** (zwykła mapa) i **„Wniosek: dołącz teren”** (zielona wełna) lub **„Wniosek: oddaj teren”** (czerwona wełna). Własne wnioski znajdziesz pod ikoną **papieru**.

**Pamiętaj:** terytorium polityczne **nie blokuje wejścia, kopania ani stawiania bloków**. Nie jest też pozwoleniem na griefing w pokoju. **Ochrona rzeczywistych baz wymaga rdzenia.**

## 5. Rdzenie ochronne — najważniejszy przedmiot państwa

Każde państwo może mieć:
- **1 darmowy rdzeń**, odebrany w GUI po założeniu państwa.
- **2 dodatkowe rdzenie własnej produkcji** (limit **3 własnych** łącznie, również tych trzymanych jako przedmioty).
- **Dowolną liczbę rdzeni zdobytych w wojnach** — nie wliczają się do limitu trzech.

**Odbiór pierwszego:** `/esmp` → **magnetyt „Rdzenie”** → **magnetyt „Odbierz darmowy rdzeń”** → **zielona wełna „Potwierdź”**. W tym menu jest również **gwiazda Netheru „Stan rdzeni”** i **kompas „Czy jestem chroniony?”**.

### Gdzie i jak postawić rdzeń?

- Lider lub zastępca stawia **specjalny rdzeń-przedmiot** (wygląda jak magnetyt / lodestone) **na zatwierdzonym terytorium własnego państwa**.
- Każdy chroni kwadrat **65 × 65 bloków**, od dna świata do limitu wysokości. Obszary nie mogą na siebie nachodzić; mogą bezpośrednio sąsiadować.
- Cały obszar ochrony musi mieścić się wewnątrz **WorldBorder**.
- Na chronionym terenie obcy nie mogą normalnie rozwalać budowli, stawiać bloków ani otwierać skrzyń.
- **Nie da się wykopać postawionego rdzenia zwykłym kilofem**, nawet oblężniczym. Własny rdzeń podnosi się poprzez GUI.

### Crafting 1 — dodatkowy Rdzeń Ochronny

Otwórz zwykły **stół rzemieślniczy 3 × 3**. Ułóż **dokładnie** te przedmioty:

| Lewy slot | Środkowy slot | Prawy slot |
|:---:|:---:|:---:|
| ⬛ **64× Węgiel** | ⬜ **64× Sztabka żelaza** | 🟧 **64× Sztabka miedzi** |
| 🟥 **64× Redstone** | 🟫 **1× Szafa grająca** (*Jukebox*) | 🟦 **64× Lapis lazuli** |
| 🟨 **64× Sztabka złota** | 🩵 **64× Diament** | 🟪 **64× Odłamek ametystu** |

**Razem: 512 surowców + 1 Jukebox.** Kolorowy kwadrat oznacza **rodzaj surowca**, a nie blok, który należy włożyć do craftingu. Użyj **surowców/sztabek**, nie bloków rud i nie skompresowanych bloków materiałów.

**Jak odebrać wynik:** zwykły **lewy klik pustym kursorem**, **bez Shifta**. Potrzebujesz pełnych stacków po 64 w ośmiu polach. To specjalny crafting: serwer zużywa **wszystkie** wymagane surowce, a nie po jednym. Tylko lider lub zastępca może wykonać rdzeń; limit to dwa crafted na państwo.

### Podnoszenie i zmiana stolicy

**`/esmp` → magnetyt „Rdzenie”**:
- **Żelazny kilof „Podnieś własny rdzeń”** — popatrz na jego lodestone z odległości do **6 bloków** i potwierdź. Tylko w pokoju. **Zdobytych rdzeni nie można podnosić i przenosić**.
- **Złoty blok „Ustaw stolicę”** — popatrz na własny postawiony rdzeń i potwierdź. Działa podczas pokoju; **6 godzin cooldownu dla całego państwa**. Podniesienie aktualnej stolicy wymaga wcześniejszego wyznaczenia innej.
- Jeśli stolica zostanie zdobyta, a państwo wciąż ma inne rdzenie, serwer automatycznie wybiera nową.

### Strefa 9 × 9 wokół rdzenia

W poziomie **4 bloki w każdą stronę od rdzenia**, pionowo od **Y rdzenia −5 do +10**:
- Nie stawiaj **łóżek, obsydianu, płaczącego obsydianu, bloków netherite, ancient debris, reinforced deepslate**, bedrocka i innych bloków praktycznie niemożliwych do przebicia.
- To właśnie w tej strefie przeciwnicy **przejmują rdzeń podczas oblężenia**.

## 6. Dyplomacja i wojny — wszystko w GUI

**`/esmp` → ⚔️ żelazny miecz „Dyplomacja”**

| Chcesz… | Kliknij |
|---|---|
| Zobaczyć konflikty | 🗺️ **„Aktywne wojny”** (mapa) → wybierz wojnę |
| Wypowiedzieć wojnę | 🗡️ **„Wypowiedz wojnę”** (miecz netherite) → wybierz państwo (czerwony baner) → potwierdź zieloną wełną |
| Dodać powód wojny | Na ekranie potwierdzenia kliknij **książkę z piórem „Dodaj powód”**, a następnie napisz tekst na czacie (opcjonalne) |
| Sprawdzić strony konfliktu | W szczegółach wojny kliknij **„Uczestnicy”** (główka gracza) |
| Zaprosić sojusznika | W szczegółach wojny **„Zaproś sojusznika”** (zielony baner) → wybierz państwo |
| Przyjąć zaproszenie do wojny | **„Zaproszenia do wojen”** (papier) → wybierz zaproszenie → **„Przyjmij”** |
| Zaproponować / przyjąć pokój | W szczegółach wojny **biały baner „Zaproponuj pokój” / „Zaakceptuj pokój”** |
| Wycofać się z cudzej wojny | Jako sojusznik: w szczegółach wojny **bariera „Opuść wojnę”** |

**Ważne zasady:**
- Wojnę wypowiada lider lub zastępca. Nie trzeba przedstawiać *casus belli*; opcjonalna wiadomość jest ogłaszana wraz z wojną.
- Państwo może uczestniczyć w **wielu wojnach naraz**.
- **Dołączenie sojuszników/najemników do wojny: tylko przez pierwsze 4 godziny** od jej pierwotnego wypowiedzenia. Dołączone państwo może potem **samodzielnie opuścić konflikt**.
- **Pokój wymaga zgody obu pierwotnych stron**, a nie wszystkich sojuszników. Reparacje i zmiany granic są ustalane pomiędzy graczami, a nie automatycznie ściągane przez plugin.
- Wojna pozwala na **normalne PvP pomiędzy jej stronami**, ale nie oznacza automatycznego prawa do rozbierania chronionych baz. Do tego są **legalne oblężenia**.

## 7. Oblężenia i zdobywanie baz

**`/esmp` → ⛏️ żelazny kilof „Oblężenia”**. Zobaczysz ikonę **książki „Status oblężenia”**, **zegara „Reguły oblężeń”** i **diamentowego kilofa „Kilof Oblężniczy — receptura”**.

**Szturm uruchamia się automatycznie po wejściu atakującego na teren wrogiej chronionej bazy** — nie trzeba wpisywać komendy ani naciskać przycisku „Start”.

| Warunek | Zasada |
|---|---|
| Formalna wojna | Musi istnieć między uprawnionymi stronami |
| Godziny szturmu | **10:00–00:00** czasu polskiego |
| Obrońcy online | Co najmniej **20% pełnej liczby członków broniącego państwa**, zaokrąglając w górę; min. **1** |
| Gdy obrońców zrobi się za mało | Już rozpoczęty szturm ma **30 minut tolerancji**; nowego nie rozpoczniesz |
| O północy albo po zakończeniu szturmu | Postęp zostaje wyzerowany |
| Wejście wroga do chronionej bazy podczas legalnego oblężenia | **Nikt nie może tam stawiać bloków**, również obrońcy |

### Crafting 2 — Kilof Oblężniczy

**Stół rzemieślniczy 3 × 3**: surowce w tym samym układzie co rdzeń, ale **po 16 sztuk**, a w środku **zwykły diamentowy kilof**.

| Lewy slot | Środkowy slot | Prawy slot |
|:---:|:---:|:---:|
| ⬛ **16× Węgiel** | ⬜ **16× Sztabka żelaza** | 🟧 **16× Sztabka miedzi** |
| 🟥 **16× Redstone** | ⛏️ **1× Zwykły diamentowy kilof** | 🟦 **16× Lapis lazuli** |
| 🟨 **16× Sztabka złota** | 🩵 **16× Diament** | 🟪 **16× Odłamek ametystu** |

**Razem: 128 surowców + diamentowy kilof** (czyli **19 diamentów**, jeśli kilof wytwarzasz od zera). Kilof musi być **zwykły, bez modyfikacji**. Wynik odbierz **zwykłym LPM z pustym kursorem, bez Shifta**.

**Kilof Oblężniczy:**
- Ma **15 legalnych zniszczeń** i znika po ostatnim.
- Służy tylko do **bloków postawionych przez graczy** w chronionej bazie przeciwnika **podczas aktywnego, legalnego oblężenia**. Naturalnego kamienia/ziemi tym sposobem nie przebijesz.
- **Nie niszczy samego rdzenia**. Ma wygląd diamentowego kilofa, ale celowo niższą szybkość kopania.
- Poza legalnym szturmem **nie jest zwykłym kilofem górniczym**.

### Zdobywanie rdzenia — 480 sekund

Po wejściu do strefy **9 × 9** wokół wrogiego rdzenia (Y **−5…+10** względem rdzenia) działa licznik:

| Kto jest w strefie? | Zmiana licznika |
|---|---|
| 🔴 Tylko uprawnieni atakujący | **+1 sekunda** postępu na sekundę |
| ⚔️ Atakujący i obrońcy | **Pauza** |
| 🟢 Tylko obrońcy | **−2 sekundy** postępu na sekundę |

**480 / 480 sekund = zdobycie** (8 minut nieprzerwanego ataku bez obrony). Pierwsze państwo koalicji, które rozpocznie naliczanie, zostaje wskazane jako przyszły właściciel. Jeśli licznik spadnie do zera lub szturm się zakończy, przypisanie i postęp się resetują.

**Łupy:** przed zdobyciem rdzenia **nie wolno otwierać ani lootować skrzyń chronionej bazy**, nawet w trakcie oblężenia. **Po skutecznym przejęciu rdzenia** nowy właściciel bazy może otworzyć skrzynie. Nie obchodź zakazu przez lejki i inne mechanizmy.

**Ostatni rdzeń:** obowiązuje zasada **`x − 1`**. Jeśli państwo ma `x` rdzeni, można przejąć `x − 1`; **ostatni znika**, a całe państwo **upada**. Jego polityczne granice stają się neutralne. Gracze nie są usuwani z serwera i mogą założyć nowe państwo lub dołączyć do innego.

## 8. PvP, śmierć i Combat Log

- **Podczas wojny** strony konfliktu mogą walczyć normalnie. Inne państwa nie stają się automatycznie wrogami tylko dlatego, że gdzieś trwa wojna. **Friendly fire jest włączony.**
- **W pokoju, we własnej bazie chronionej rdzeniem:** nie można zostać zabitym przez innego gracza ani bezkarnie strzelać z ochrony do innych.
- **W pokoju, na własnym terytorium politycznym poza rdzeniem:** pierwszy wrogi cios zostaje anulowany, dostajesz **8 sekund ochrony**; ponownie można ją otrzymać dopiero po **5 minutach**. Twój własny atak przerywa ochronę. Na neutralnym terenie nie ma tej dodatkowej tarczy.
- **Combat Tag trwa 60 sekund.** Wylogowanie w trakcie walki powoduje karę systemową — **nie wylogowuj się, dopóki licznik nie zniknie**. Dokładnych konsekwencji nie próbuj obchodzić.
- **Po śmierci wypada CAŁY ekwipunek** (`keepInventory=false`).

### Odrodzenie w trakcie wojny

Po kliknięciu **Respawn** pojawia się odliczanie na ekranie. Liczy się **liczba graczy online w Twoim własnym państwie** w chwili śmierci:

| Twoi członkowie online | Blokada po odrodzeniu |
|---:|---:|
| 1–2 | **15 s** |
| 3–5 | **20 s** |
| 6–10 | **30 s** |
| 11–15 | **40 s** |
| 16 i więcej | **50 s** |

Podczas licznika **nie możesz się poruszać, walczyć ani używać przedmiotów**, ale jesteś nietykalny. **Po upływie czasu ochrona kończy się natychmiast**, więc wróg może od razu Cię zaatakować. W czasie pokoju nie ma dodatkowego timera.

**Łóżka:** w chronionej bazie łóżko ustawione jako punkt odrodzenia gracza i bloki bezpośrednio pod nim są chronione przed zniszczeniem podczas oblężenia (również gdy właściciel jest offline). Nie wszystkie łóżka są niezniszczalne. Łóżek nie wolno ustawiać w strefie 9 × 9 rdzenia.

## 9. Powrót do stolicy

To **jedyny istotny skrót w grze, którego nie ma obecnie jako przycisku GUI**. Wpisz:

```mcfunction
/esmp home
```

- Teleportuje **do aktualnej stolicy Twojego państwa**, a nie do dowolnego rdzenia ani prywatnego `/sethome`.
- Działa **również podczas wojny**.
- Ma zawsze **30 sekund przygotowania z licznikiem na ekranie**. **Nie ruszaj się i nie otrzymuj obrażeń** — w przeciwnym razie teleportacja zostanie anulowana. Sam obrót kamery nie jest ruchem.
- **Dni 1–2:** nieograniczona liczba powrotów. **Od dnia 3:** **4 skuteczne teleportacje dziennie na gracza**; reset limitu o **06:00 czasu polskiego**.
- Nieudana lub anulowana próba **nie zabiera wykorzystania**. Pozostały limit widać w panelu bocznym.

## 10. Limity przedmiotów — obowiązują zawsze

Limity liczone są **razem w całym ekwipunku** (plecak + hotbar + druga ręka), również poza wojną. **W skrzyniach można mieć więcej.**

| Przedmiot | Maksimum przy sobie |
|---|---:|
| 🍎 Zwykłe złote jabłka | **8** |
| 🟣 Perły Endu | **3** |
| 🕸️ Pajęczyny | **16** |
| 🗿 Totemy nieśmiertelności | **3** |
| ✨ Zaklęte złote jabłka | **0 — zakazane** |
| 💠 End Crystal | **0 — zakazane** |
| ⚓ Respawn Anchor | **0 — zakazane** |

Serwer ogranicza m.in. podnoszenie przedmiotów i wyjmowanie ich z magazynów. Celowe obchodzenie limitów jest zabronione.

## 11. Enchanty i odblokowywanie wymiarów

**Dzień serwera zmienia się codziennie o 06:00 czasu polskiego**, nie co 24 godziny od premiery.

### Najważniejsze daty

| Dzień edycji | Co się zmienia? |
|---|---|
| **1–6** | Fortune niedostępny; standardowe enchanty maksymalnie **I** (poza wyjątkami) |
| **7** | Odblokowuje się **Fortune I** |
| **10, godz. 06:00** | **Otwiera się Nether**; zwykłe enchanty maksymalnie **II**; Quick Charge III |
| **16, godz. 06:00** | Można używać **Fortune III**; Fortune II nadal zabroniony |
| **Zawsze** | **End jest wyłączony. Mending i Multishot zabronione.** |

### Tabela limitów enchantów

| Enchant / grupa | Dni 1–9 | Od dnia 10 |
|---|:---:|:---:|
| **Mending** | ❌ | ❌ |
| **Multishot** (kusza) | ❌ | ❌ |
| **Fortune** | ❌ dni 1–6, **I** dni 7–9 | **I** do dnia 15; od dnia 16 **I lub III** |
| **Quick Charge** (kusza) | **II** | **III** |
| **Piercing** (kusza) | **I** | **II** |
| **Unbreaking** (wszystkie przedmioty, w tym kusza) | **III** | **III** |
| **Efficiency** | **V** | **V** |
| **Looting** | **III** | **III** |
| **Silk Touch, Aqua Affinity** | **I** | **I** |
| **Respiration** | **III** | **III** |
| **Wszystkie standardowe enchanty bojowe i ochronne** (np. Protection, Sharpness, Power, Thorns, Knockback, Fire Aspect, Feather Falling) | **I** | **II** |
| **Pozostałe enchanty** (np. Depth Strider, Soul Speed, Swift Sneak, Riptide, Lure, Density, Wind Burst) | **I** | **II** |
| **Enchantmenty naturalnie jednopoziomowe** (np. Flame, Infinity, Channeling) | **I** | **I** |

**Ważne:** standardowy limit I/II obowiązuje także na łupach, książkach, gotowych przedmiotach od villagerów i przedmiotach łączonych na kowadle. **Fortune II jest zawsze zakazane**, nawet gdy Fortune III jest już dostępne. Nielegalny enchant może zostać zablokowany, usunięty albo obniżony przez serwer.

### Nether, End i villagery

- **Nether:** otwiera się od **06:00 dnia 10**. Wcześniejsze próby wejścia są blokowane.
- **End:** zamknięty przez **całą edycję** — nie planuj Ender Dragona ani zwykłego dostępu do shulkerów.
- Świat to **Ziemia ok. 1:1500**, z nietypowo ograniczonymi jaskiniami i **bez naturalnych struktur/wiosek**. Pierwszych villagerów można uzyskać np. przez **leczenie zombie villagerów**, potem normalnie rozmnażać.
- W Netherze wolno używać łóżek do wydobywania ancient debris, **ale nie do PvP**.

## 12. Handel i gospodarka — jak założyć sklep Shopkeepers?

**Nie ma pieniędzy, banków ani wirtualnych kont.** Gospodarka EarthSMP opiera się na **barterze: przedmiot za przedmiot**.

Sklepy obsługuje osobny plugin **Shopkeepers**, więc **wyjątkowo używamy też komendy `/shopkeeper`** (alias `/shopkeepers`). Zwykły sklep gracza **pobiera towar z przypisanej skrzyni**; nie ma nieskończonych zapasów. Nielimitowane sklepy administracji są zarezerwowane dla obsługi serwera.

### 12.1. Jak stworzyć własny sklep?

**Przygotuj:** skrzynię, towary i miejsce na NPC (np. villagera). Postaw skrzynię w bezpiecznym miejscu, najlepiej w swojej bazie chronionej rdzeniem. Shopkeepers obsługuje dwa sposoby zakładania sklepu — dostępny zależy od uprawnień skonfigurowanych przez administrację.

**Sposób A — komendą, jeśli administracja włączyła tworzenie sklepów poleceniem:**

1. Ustaw celownik na **skrzyni**, z którą sklep ma współpracować.
2. Wpisz:

   ```mcfunction
   /shopkeeper trading villager
   ```

3. Powstanie sklep typu **Trading Shop** przy wskazanej skrzyni. Możesz potem przestawić NPC przez edytor, jeśli miejsce na to pozwala.

**Ważne:** samo `/shopkeeper` bez podania typu, szczególnie gdy nie patrzysz na skrzynię, może próbować utworzyć **sklep administracyjny**, a nie zwykły sklep gracza. Do tworzenia przez komendę potrzeba uprawnienia `shopkeeper.create` (domyślnie mają je tylko operatorzy). **Jeżeli komenda odmawia dostępu, to nie błąd gry — użyj sposobu B albo zgłoś się do administracji.**

**Sposób B — specjalnym przedmiotem Shopkeepers (domyślna metoda dla zwykłych graczy):**

1. Uzyskaj od administracji specjalny **przedmiot do tworzenia sklepów** (domyślnie jajo przywołania villagera o nazwie *Shopkeeper*). To nie musi być zwykłe jajo z Minecrafta.
2. Trzymając przedmiot i **nie celując w blok**, klikaj **PPM**, aż wybierzesz typ **Trading**. **Shift + PPM** przełącza wygląd sklepikarza, np. villager / tabliczka, jeśli jest dostępny.
3. **PPM na skrzyni**, by ją przypisać do sklepu.
4. **PPM na wybranym bloku**, by umieścić sklepikarza. Zwróć uwagę na dozwoloną odległość od skrzyni.

**Dla EarthSMP polecam „Trading”** — ten typ umożliwia barter za *dowolny przedmiot*. Typy **Selling** i **Buying** opierają się na przedmiotach skonfigurowanych jako waluta, dlatego nie są domyślnym wyborem do naszej gospodarki.

### 12.2. Jak ustawić wymiany? (NPC i GUI)

1. Włóż **towar na sprzedaż** do powiązanej skrzyni.
2. Podejdź do swojego sklepikarza i wykonaj **Shift + PPM**, aby otworzyć **edytor sklepu** (zwykłe PPM to okno handlu klienta).
3. W **Trading Shop** górne sloty pokazują sprzedawane towary. W slotach ceny umieszczasz **wzory przedmiotów**, które chcesz otrzymywać za towar: weź je kursorem ze swojego ekwipunku i kliknij odpowiednie pole ceny w GUI. Przedmiot jest tam **kopiowany jako wzór**, a nie sprzedawany podczas konfiguracji.
4. Klikaj **LPM / PPM** na pozycjach edytora, aby zwiększać lub zmniejszać ilości. Dostosuj liczbę sztuk produktu i zapłaty, np. **32 brukowane łupki za 32 sztuki bruku**.
5. Zamknij edytor, aby zapisać ofertę, i sprawdź transakcję z perspektywy klienta.

| Co chcesz zrobić? | Jak? |
|---|---|
| Otworzyć sklep jak klient | **PPM** na NPC |
| Edytować swój sklep | **Shift + PPM** na NPC |
| Sprawdzić / uzupełnić zapasy | Skrzynia sklepu; w edytorze także **ikona skrzyni** |
| Tymczasowo zamknąć / otworzyć sklep | W edytorze **ikona banera** |
| Przenieść NPC | W edytorze **ikona perły Endu**, następnie PPM na nowe miejsce |
| Usunąć sklep na stałe | W edytorze **ikona kości** — **brak cofnięcia** |

**Jak działa handel:** po zakupie z przypisanej skrzyni znika sprzedany towar, a do skrzyni trafia zapłata. **Brak towaru lub miejsca na zapłatę = transakcja nie dojdzie do skutku.** Uzupełniaj magazyn, jeśli chcesz prowadzić sklep bez przerwy.

### 12.3. Komendy Shopkeepers przydatne graczowi

| Komenda | Zastosowanie |
|---|---|
| `/shopkeeper help` | Pomoc i dostępne dla Ciebie komendy |
| `/shopkeeper list` | Lista **Twoich** sklepów |
| `/shopkeeper trading villager` | Tworzenie sklepu barterowego przy wskazanej skrzyni (**jeśli masz uprawnienie tworzenia przez komendę**) |
| `/shopkeeper notify trades` | Włącza/wyłącza powiadomienia o transakcjach |
| `/shopkeeper remove` | Próbuje usunąć sklep, na który patrzysz (**wymaga uprawnień; najprościej użyć ikony kości w edytorze**) |

**Nie używaj `/shopkeeper give` do zdobywania przedmiotów na własną rękę** — to polecenie administracyjne, nie sklep gracza. Przedmiot do tworzenia sklepów udostępnia administracja.

### 12.4. Gdzie można ustawić sklep?

- Sklep gracza może działać **w dowolnym dozwolonym miejscu**, nie tylko w Makau. Najbezpieczniej ustawić **skrzynię wewnątrz chronionej bazy**, a NPC dostępnego dla klientów na zewnątrz, jeśli pozwala na to aktualna konfiguracja maksymalnej odległości.
- **Sam NPC nie zastępuje Rdzenia Ochronnego**: niechroniona skrzynia może być narażona na dostęp lub zniszczenie zgodnie z innymi mechanikami świata. W pokoju kradzież/grief pozostają zabronione regulaminowo.
- **Sklepy administracyjne na spawnie Makau** mają oferować nieograniczone wymiany podstawowych surowców budowlanych, np. bruk ↔ brukowany łupek 1:1. Asortyment określi administracja; nie będzie automatycznej sprzedaży strategicznych surowców, takich jak diamenty czy netherite.
- **Naturalni villagerzy** są oddzielną mechaniką Minecrafta — nie myl ich z NPC Shopkeepers. Zakazy enchantów i limity przedmiotów obowiązują przy każdym handlu.

> **Uwaga dotycząca premiery:** Shopkeepers jest zainstalowany i podstawowo przetestowany, ale nie potwierdzono jeszcze produkcyjnego sposobu udostępnienia graczom **tworzenia sklepów** (komenda z uprawnieniem czy specjalny przedmiot) oraz odległości NPC od skrzyni. Administracja ogłosi tę decyzję przed startem. Bez tego gracz bez OP może handlować, ale niekoniecznie może od razu stworzyć sklep.

**Dokumentacja pluginu:** [Creating Shops](https://github-wiki-see.page/m/Shopkeepers/Shopkeepers-Wiki/wiki/Creating-Shops), [Player Shop Setup](https://github-wiki-see.page/m/Shopkeepers/Shopkeepers-Wiki/wiki/Player-Shop-Setup), [Editing Shops](https://github-wiki-see.page/m/Shopkeepers/Shopkeepers-Wiki/wiki/Editing-Shops).

## 13. Mapa polityczna i konto WWW

**Mapa:** **https://91-90-81-190.sslip.io** (jeśli adres się zmieni, nowy będzie na Discordzie).

**Bez logowania** zobaczysz geografię, kolorowe państwa, WorldBorder oraz **wszystkie postawione rdzenie**. Publiczny znacznik każdego rdzenia to **lodestone i współrzędne X/Y/Z**, ale bez wskazania, który jest stolicą.

### Pierwsze powiązanie konta

**Wyjątek techniczny od samego GUI `/esmp`:** w obecnym wydaniu generowanie kodu mapy odbywa się przez podkomendę:

```mcfunction
/esmp map code
```

1. W grze otrzymasz **jednorazowy kod ważny 5 minut**.
2. Otwórz mapę, wybierz **„Pierwsze powiązanie”**.
3. Wprowadź kod oraz własne hasło (**12–128 znaków**). **Nie używaj hasła do konta Microsoft.**
4. Później wybierasz **„Mam konto”** i logujesz się nickiem Minecraft oraz ustalonym hasłem. Nie musisz być wtedy zalogowany w Minecraft.

**Po zalogowaniu** możesz zobaczyć pozycje **własne i graczy z tego samego państwa**, aktualizowane na żywo. **Nie widzisz przeciwników ani nawet sojuszników**. W Netherze znaczniki na mapie Ziemi znikają.

W razie utraty hasła podkomenda **`/esmp map reset`** usuwa powiązanie konta, więc można je utworzyć ponownie. **`/esmp map revoke`** wylogowuje wszystkie sesje, ale nie kasuje konta. **Nie podawaj nikomu kodu ani hasła.**

## 14. Zasady zachowania i najważniejsze zakazy

**Light RP:** traktujemy wojny i dyplomację jako część gry; konfliktów nie przenosimy na Discorda i do życia prywatnego.

**Zakazane:** cheaty/X-ray i nieuczciwe automatyzacje (w tym wykorzystywanie Baritone do oszukiwania), duplikowanie, exploity, omijanie ochrony baz i limitów, toksyczność, podgląd dokładnego HP/serc innych graczy, **nemosowanie** (zabudowywanie wroga tak, by nie mógł się bronić), spam strzałami z **Instant Damage**, **PvP wagonikami TNT**, **wybuchające łóżka jako broń PvP** oraz **portal trapping**.

**Pokój:** to, że coś nie jest chronione technicznie, **nie oznacza zgody na kradzież i griefing**. **Wojna:** wolno atakować majątek wrogiego państwa poza chronionymi bazami zgodnie z regułami; chronione bazy podlegają osobnym zasadom oblężeń. Nie niszcz neutralnych państw pod pretekstem cudzej wojny.

**Simple Voice Chat** jest częścią serwera; do korzystania z głosu może być potrzebny zgodny mod klienta. Gra i wszystkie mechaniki państwowe działają także na czystym Minecraft Java, bez obowiązkowej paczki modów.

W razie problemu z mechaniką, niespodziewanej blokady lub podejrzenia exploita **zgłoś problem administracji** zamiast próbować go wykorzystywać.

## 15. Miniprzypominajka przed pierwszą wojną

- **Państwa i rangi:** `/esmp` → główka gracza.
- **Rdzenie 65×65:** `/esmp` → lodestone. 1 darmowy + 2 craftowane; zdobyczne bez limitu.
- **Mapa/claim:** strona WWW + akceptacja admina. Claim polityczny **nie zabezpiecza bloków**.
- **Wojna:** `/esmp` → żelazny miecz. Koalicja dołącza w ciągu 4 godzin.
- **Oblężenie:** automatyczne, **10:00–00:00**, minimum **20% obrońców online**.
- **Przejmowanie:** **9×9, 480 sekund**. Nie lootuj skrzyń przed zdobyciem.
- **Kilof Oblężniczy:** **15 bloków postawionych przez graczy**, tylko legalny szturm.
- **PvP:** Combat Tag **60 s**, wszystkie rzeczy wypadają po śmierci.
- **Powrót do stolicy:** `/esmp home`, **30 s bez ruchu i obrażeń**; od dnia 3 **4 użycia dziennie**.
- **Dzień 10:** Nether i enchanty II. **Dzień 16:** Fortune III.
- **Najważniejsze zakazy:** Mending, Fortune II, Multishot, enchanted gapple, End Crystal, Respawn Anchor.

---

**Miłej edycji i powodzenia w budowie własnego państwa.** 🌍

*Źródło: aktualna [specyfikacja](docs.md) oraz interfejsy w kodzie EarthSMP-Core 1.8.13. W sytuacjach spornych nadrzędne są zasady zatwierdzone przez administrację. Przewodnik jest przeznaczony dla graczy, dlatego nie zawiera komend operatorskich i informacji o infrastrukturze.*
