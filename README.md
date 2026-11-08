# Kabina Narodów 2.0 — EarthSMP

Prywatny serwer **Minecraft Java** dla znajomych: państwa, kolorowane terytoria, rdzenie ochronne, dyplomacja, wojny i oblężenia oraz własny portal polityczny z mapą Ziemi. Kod pluginu w **Java 25 / Paper 26.3**, baza **SQLite**.

**Wersja projektu:** EarthSMP-Core **1.8.13**. **Stan na 8 listopada 2026:** Paper i autorska mapa WWW działają na VPS z Ubuntu 24.04; udane połączenie z serwerem Minecraft i logowanie do portalu przez HTTPS. Trwa przygotowanie spawnu Makau, sklepów i ostatecznych testów. **Oficjalna edycja jeszcze nie wystartowała (dzień 0).**

## Dokumentacja

| Plik | Co zawiera |
| --- | --- |
| [**Regulamin serwera**](docs/REGULAMIN.md) | Obowiązujące zasady gry, PvP, wojny, ochrony baz, handlu, fair play i zachowania |
| [**Przewodnik gracza**](docs/PRZEWODNIK-GRACZA.md) | **Od pierwszego wejścia po wojny: GUI, craftingi 3×3, limity, mapa i zasady** |
| [Specyfikacja zasad i mechanik](docs/docs.md) | Kompletny zestaw reguł edycji, status 1.8.13, świat, rdzenie, państwa, PvP, oblężenia, progresja |
| [Checklista przed premierą](docs/CHECKLISTA-STARTU.md) | Faktycznie wykonane testy i lista otwartych działań, bez duplikowania już zaliczonych mechanik |
| [Runbook VPS i backupów](docs/OPERACJE-VPS.md) | Systemd, SSH, Caddy, porty, logi, ręczne backupy, odtwarzanie i aktualizacja pluginu |
| [Karta awaryjna](docs/AWARYJNE.md) | Krótkie komendy: Minecraft nie działa, mapa padła, problemy z SQLite, backupami i SSH |
| [Archiwum rozwoju](docs/HISTORIA-ROZWOJU.md) | Starsze statusy, próby i plany od 1.8.0 — materiał historyczny |
| [Historia porządkowania repo](docs/REPO-PORZADKI.md) | Uwagi o prywatnych plikach i historycznych sekretach, nie instrukcja bieżącej migracji |

## Układ projektu

- `EarthSMP-Core/` — źródła autorskiego pluginu Paper, testy i Gradle Kotlin DSL.
- `docs/` — specyfikacja, utrzymanie, testy i historia decyzji.
- `EarthSMP-Backup.ps1` — starszy skrypt backupu **lokalnego Windowsa**, nie dla bieżącego VPS.
- `start.bat` — lokalny start testowy (Windows).
- Produkcyjny świat, dane Squaremap, bazy SQLite, hasła, archiwa i ustawienia serwera **nie są wersjonowane**.

## Budowanie

Wymagana **Java 25**.

```bash
cd EarthSMP-Core
./gradlew clean build
```

Na Windows analogicznie: `gradlew.bat clean build`. Na serwer wdrażaj **wyłącznie shaded JAR**, nie `*-plain.jar`. Przed aktualizacją wykonaj spójny backup i wyłącz Paper, potem sprawdź logi i stan pluginu. Pełna procedura w [runbooku](docs/OPERACJE-VPS.md).

## Gdzie zgłaszać problem?

Przy zgłoszeniu podaj wersję, rolę/uprawnienia gracza, kroki odtworzenia, oczekiwany i rzeczywisty rezultat oraz fragment logów **bez sekretów**. Nie publikuj danych kont, kluczy ani archiwów produkcyjnych.

## Stan premiery

Zegar uruchamia administrator wyłącznie w chwili rozpoczęcia nowej edycji poleceniem `/esmp clock start`. Przed tym: spawn, handel, backup `PRE-LAUNCH`, test odtworzenia kopii i podstawowa próba generalna. Szczegóły w [checkliście](docs/CHECKLISTA-STARTU.md).
