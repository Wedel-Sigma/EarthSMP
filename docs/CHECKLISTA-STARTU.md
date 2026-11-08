# Kabina Narodów 2.0 — checklista przed premierą

> Stan na **8 listopada 2026**, EarthSMP-Core 1.8.13. Projekt dla znajomych, nie publiczny serwer konkurencyjny. **Serwer infrastrukturalnie działa, ale edycja NIE wystartowała (dzień 0).** Zasady: [docs.md](docs.md). Obsługa VPS i backupów: [OPERACJE-VPS.md](OPERACJE-VPS.md).

## Potwierdzone wykonanie

- [x] EarthSMP-Core 1.8.13 zbudowany i uruchomiony na Paper 26.3 / Java 25.
- [x] Podstawowe funkcje państw, rdzeni, granic i mapy przetestowane.
- [x] Testy oblężeń i zdobywania rdzeni, w tym pełne 480 s, strefy, Kilof Oblężniczy, loot po zdobyciu, ostatni rdzeń, upadek państwa i trwałość po restarcie (historyczna seria 1.8.0: 12/12).
- [x] PvP pokojowe/wojenne, Combat Tag 60 s, Combat Log, respawn wojenny, zakończenie wojny: **8/8 OK** w sesji 2 graczy na 1.8.13.
- [x] Shopkeepers: podstawowe działanie pluginu i handlu (sklepy na spawnie jeszcze nieutworzone).
- [x] GrimAC i Better Anti-Dupe: zainstalowane, podstawowe działanie sprawdzone.
- [x] Paper Anti-Xray tryb 2 w Overworldzie i Netherze: administrator przetestował i potwierdził działanie.
- [x] Czysta mapa z oryginalnego **MAIN.zip** przeniesiona na VPS, nowa SQLite, dzień 0.
- [x] Contabo VPS, Ubuntu 24.04, Java 25, oddzielne konta systemowe, SSH przez klucz, usługa systemd dla Paper.
- [x] Publiczne połączenie Minecraft bez Tailscale; whitelist utrzymana na etapie budowy.
- [x] Caddy + Let's Encrypt HTTPS; działają mapa polityczna i logowanie do portalu po ponownym powiązaniu konta.
- [x] Jeden udany backup offline przez skrypt Linuksa; Paper po nim wrócił; timer na **07:00 Europe/Warsaw** aktywny.
- [x] Jedną kopię pobrano na Windows i pozytywnie sprawdzono integralność archiwum oraz podstawowe pliki.

## Do wykonania przed pierwszą edycją

- [ ] **Makau:** zbudować spawn, punkty informacyjne, zabezpieczyć docelowy teren WorldGuardem; sprawdzić działanie z konta bez OP.
- [ ] **Handel:** postawić sklepy administracyjne Shopkeepers, skonfigurować barter i faktycznie wykonać transakcje na docelowym świecie.
- [ ] **Gracze:** przed premierą wyczyścić tymczasowy ekwipunek i postępy budowniczych, zachowując budynki i regiony; potwierdzić właściwy spawn.
- [ ] **PRE-LAUNCH:** utworzyć odrębną kopię kompletnego stanu ze spawnem, nadal z dniem 0. Nie ruszać oryginalnego MAIN.zip.
- [ ] **Odtwarzanie:** uruchomić pełny test odtworzenia backupu świata + SQLite + pluginów na osobnym środowisku (samo poprawne `tar -tzf` nie wystarcza).
- [ ] **Offsite:** ustalić i włączyć regularny eksport archiwów poza VPS albo harmonogram ręcznego pobierania; obecnie jeden backup na Windows jest, ale eksport nie jest automatyczny.
- [ ] **Mapa/prywatność:** potwierdzić dla co najmniej dwóch państw, że `/api/positions` nie udostępnia pozycji obcych graczy, także bez logowania lub po zmianie członkostwa. Osobno kontrola braku dostępu do bazowego squaremap z zewnątrz.
- [ ] **Voice Chat:** po migracji sprawdzić połączenie między dwoma graczami na publicznym serwerze, port UDP zgodny z konfiguracją.
- [ ] **Wydajność:** przeprowadzić próbę z kilkoma graczami, sprawdzić TPS/MSPT/RAM/FPS mapy. VPS ma współdzielone vCPU, więc liczba rdzeni nie gwarantuje wydajności jednowątkowej.
- [ ] **Koalicja:** w miarę dostępności testerów sprawdzić 3 państwa w jednej wojnie, wybór strony, prawo do zdobycia rdzenia. Dotąd brak odpowiedniej liczby graczy.
- [ ] **Dane kontaktowe/regulamin:** opublikować dla znajomych adres Minecrafta, link mapy, zasady, sposób zgłaszania błędów i informację o składce (jeśli będzie).
- [ ] **Premiera:** jako ostatni krok po snapshot/backup i akceptacji administratora użyć `/esmp clock start`. **Nie wcześniej.**

## Znane ryzyka — nie są potwierdzonymi błędami

- WorldGuard lub inne pluginy mogą teoretycznie anulować stawianie rdzenia po wcześniejszym zapisie do SQLite w `CoreItems.onPlace`. Warto skontrolować podczas aktualizacji.
- Awaria SQLite w szczególnym momencie `/home` może pozostawić wykonaną teleportację bez naliczenia limitu (teleport przed zapisem).
- Sam GrimAC nie gwarantuje wykrycia Baritone; osobno włączony Paper Anti-Xray ogranicza pozyskanie wiedzy o zakrytych rudach.
- Nietypowe łączenie hopperów, wagoników i innych pluginów wymaga obserwacji po starcie. Poważne błędy zgłaszać i naprawiać; nie zakładać z góry złej woli znajomych.
- Brak niezależnego testu pełnego odzyskania po awarii całego VPS-a.

## Zasady aktualizacji dokumentacji

**Potwierdzone** = administrator wykonał test i podał wynik. **Wdrożone** = mechanika obecna w kodzie, może wymagać testu. **Do zrobienia** = brak potwierdzenia. Po każdym większym etapie aktualizować status, nie po pojedynczej sesji.

Zobacz: [główna specyfikacja](docs.md) · [runbook VPS](OPERACJE-VPS.md).
