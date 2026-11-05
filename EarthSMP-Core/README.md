# EarthSMP Core — 0.3.0 (etap 3.3.2a)

Aktualizacja **na istniejący projekt 0.2.0**. Rozpakuj do głównego katalogu repozytorium `EarthSMP` i nadpisz wskazane pliki.
Nie kasuj `EarthSMP-Core/gradle/`, `gradlew.bat`, `gradlew`, `settings.gradle.kts`, `config.yml` ani bazy danych.

## Co robi

- `/earthsmp status` — liczba państw z SQLite.
- `/earthsmp nation create <nazwa>` — tworzy państwo, z wykonawcy robi lidera (jedna osoba = jedno państwo).
- `/earthsmp nation info` — pokazuje własne państwo, rangę oraz UUID państwa.
- Nazwa: 3–32 znaki, litery/cyfry/spacja/myślnik/podkreślnik. Nazwy nie mogą się powtarzać.
- Tworzenie państwa i przypisanie lidera jest jedną transakcją SQLite.
- **Wszystkie komendy są tymczasowo tylko dla admina/OP** (`earthsmp.admin`). Docelowo gracze będą tworzyli państwa przez GUI.
- **Brak darmowego rdzenia w tej wersji** — wymaga osobnej implementacji. Używaj testowych nazw/państw; brak jeszcze komendy usuwania.

## Test na serwerze

1. Zatrzymaj Paper przez `stop`.
2. Opcjonalnie skopiuj `plugins/EarthSMPCore/earthsmp.db` jako backup.
3. W `EarthSMP-Core` wykonaj `./gradlew.bat clean build` (PowerShell: `.\gradlew.bat clean build`).
4. W `plugins/` usuń **stary** `EarthSMP-Core-0.2.0.jar`, wgraj `EarthSMP-Core-0.3.0.jar` z `build/libs` (nie `-plain`).
5. Włącz serwer; wpisz `/earthsmp nation create Polska`, `/earthsmp nation info`, `/earthsmp status`.
6. Zrestartuj Paper i ponów `/earthsmp nation info` oraz `/earthsmp status`.

Po prawidłowym restarcie nazwa, rola `LEADER` oraz licznik państw zostaną zachowane.

## Ważne

Istniejąca baza z 0.2.0 **nie wymaga migracji** (schemat SQLite v1 bez zmian).
`plugins/EarthSMPCore/` powinno być wykluczone w głównym `.gitignore`.
Z racji backupów państw **nie zapisuj bazy do publicznego Git**.
Nie używaj `/reload` do aktualizacji kodu.
