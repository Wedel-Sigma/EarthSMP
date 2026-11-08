# Porządkowanie repozytorium — 7 listopada 2026

> **MATERIAŁ HISTORYCZNY.** Opis dawnych zmian indeksu Git i ryzyka wcześniejszego pull/merge; bieżące porządki w repo zostały ukończone. Aktualna dokumentacja: [docs.md](docs.md), [OPERACJE-VPS.md](OPERACJE-VPS.md), [CHECKLISTA-STARTU.md](CHECKLISTA-STARTU.md). Nadal należy pamiętać, że dawne sekrety mogły pozostać w historii Git.

## Ważne przed scaleniem

Ta gałąź usuwa z **indeksu Git** wygenerowane katalogi `plugins/`, `config/` oraz lokalne pliki konfiguracyjne Paper. **Samo usunięcie z repo nie oznacza usunięcia danych z historii Git.** W historii nadal może być dostępny `plugins/LuckPerms/editor-keystore.json`.

**Krytyczne:** przy zwykłym `pull` po scaleniu Git może usunąć także **lokalne pliki wcześniej śledzone**, nie tylko przestać je wersjonować. Dlatego PR jest draftem, a nie automatycznym merge.

1. Zatrzymaj Paper poleceniem `stop` i zamknij konsolę.
2. Zachowaj niezmieniony backup **MAIN**. Wykonaj **NOWY**, odrębny, offline backup całego serwera, w tym `plugins/`, `config/`, `Earth/`, `server.properties`, SQLite i mapy (np. `.\EarthSMP-Backup.ps1 -ConfirmStopped`).
3. Upewnij się, że archiwum istnieje **poza folderem repo**, i że ma odpowiedni rozmiar oraz można je otworzyć. Dodatkowo skopiuj `plugins/` i `config/` poza repo, jeśli to wygodne.
4. Dopiero potem scal draft PR i wykonaj `pull` w GitHub Desktop. Jeśli Git usunął wcześniejsze pliki runtime, **przy zatrzymanym Paper** odtwórz je z nowego backupu.
5. Sprawdź świat, pluginy, SQLite i mapę; uruchom Paper. Następnie upewnij się, że `git status` nie pokazuje plików runtime.
6. `LuckPerms/editor-keystore.json` należy traktować jako **potencjalny sekret z publicznej historii**. Zweryfikuj oficjalny sposób unieważnienia/rotacji danych edytora LuckPerms. Wyczyszczenie bieżącego drzewa Git **nie usuwa starszych commitów**; ewentualne przepisywanie historii wymaga osobnej decyzji i koordynacji.

## Zawartość gałęzi

- Pozostaje: `EarthSMP-Core/`, `docs/`, `EarthSMP-Backup.ps1`, `start.bat`.
- Z repo usunięte: wygenerowane dane `plugins/`, `config/`, lokalne konfiguracje Paper i narzędzie `mcrcon.exe`.
- `.gitignore` zapobiega przypadkowemu ponownemu dodaniu tych plików.
- `docs/docs.md` v1.8.1 zapisuje zatwierdzone rozstrzygnięcia: Nether 06:00 dnia 10, enchanty jak w 1.8.8, pierwsze dwa villagery z leczenia, później rozmnażanie.
- Przypomnienie: reguła pochodzenia villagerów jest obecnie regulaminowa, a nie technicznie wymuszana.
