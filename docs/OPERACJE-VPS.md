# EarthSMP — operacje i utrzymanie VPS

> **Stan na 8 listopada 2026.** Instrukcja dla administratorów **Kabiny Narodów 2.0**, przed oficjalnym startem edycji. Nie zawiera adresów prywatnych, haseł, tokenów ani kluczy. Źródło zasad rozgrywki: [docs.md](docs.md). Najważniejsze decyzje przed premierą: [CHECKLISTA-STARTU.md](CHECKLISTA-STARTU.md).

## 1. Środowisko i zakres potwierdzenia

| Element | Wdrożenie / status |
|---|---|
| Hosting | Contabo Cloud VPS 6, region EU; 6 współdzielonych vCPU, 12 GB RAM, 200 GB SSD |
| System | Ubuntu 24.04.5 LTS (x86_64) |
| JVM | Eclipse Temurin Java 25, sprawdzona wersja 25.0.5 |
| Gra | Paper 26.3 + EarthSMP-Core 1.8.13; sprawdzono wejście przez Internet bez Tailscale |
| Systemd | `earthsmp.service` — automatyczny start Paper na koncie systemowym `minecraft` |
| Katalog gry | `/opt/earthsmp`; świat `/opt/earthsmp/Earth` |
| Baza | `/opt/earthsmp/plugins/EarthSMPCore/earthsmp.db` (SQLite schema v10) |
| JVM | `-Xms2G -Xmx8G -jar paper.jar nogui` |
| Portal | Caddy → `127.0.0.1:8766` (EarthSMP-Core); wewnętrzna squaremap `127.0.0.1:8080` |
| WWW | Publiczny HTTPS, certyfikat Let's Encrypt — działanie mapy i ponowne powiązanie/logowanie konta potwierdzone |
| Backup | `earthsmp-backup` + timer systemd, 07:00 `Europe/Warsaw`; jeden udany backup i jeden pobrany/zweryfikowany na Windows |
| Wydanie | **Dzień 0: edycja jeszcze nie rozpoczęta. Nie używać `/esmp clock start` podczas budowy spawnu.** |

Założenie projektu: **prywatny serwer dla znajomych**. Nie trzeba wdrażać nadmiernie skomplikowanej infrastruktury, ale warto mieć sprawdzony dostęp, spójne backupy i bezpieczne uprawnienia.

## 2. Dostęp i konta

- Konto `esmpadmin` loguje się przez SSH kluczem i ma `sudo`; test logowania wykonany.
- Bezpośrednie logowanie SSH jako `root` i logowanie SSH hasłem zostały wyłączone. Zostaw konfigurację SSH działającą; przed każdą przyszłą zmianą dostępu przetestuj **drugą** sesję.
- `minecraft` to odrębne konto systemowe bez loginu interaktywnego, właściciel `/opt/earthsmp`. Nie uruchamiać Paper jako root.
- Klucze prywatne, hasła oraz pełny `server.properties` i kopie produkcyjne pozostają poza Git. Klucz prywatny bez passphrase warto dodatkowo zabezpieczyć na komputerze administratora.
- Archiwa i konfiguracje mogą zawierać sekrety (np. RCON). **Nie publikować backupów, logów z wrażliwymi danymi ani danych testowych na GitHubie lub Discordzie.**

Z własnego komputera:

```powershell
ssh esmpadmin@<PUBLICZNE_IP_VPS>
```

Na VPS:

```bash
id minecraft
java -version
sudo systemctl status earthsmp --no-pager
```

## 3. Porty i mapa WWW

| Port | Dostęp z Internetu | Cel |
|---|---|---|
| TCP 22 | Tak, administracyjny | SSH (zalecane dalsze ograniczenie do zaufanych adresów, jeśli praktyczne) |
| TCP 25565 | Tak | Minecraft Java; `online-mode=true`, obecnie whitelist |
| UDP 24454 | Tak, **jeżeli** to port skonfigurowany dla Simple Voice Chat | Czat głosowy; osobny test działania po migracji pozostał |
| TCP 80 i 443 | Tak | Caddy, HTTP→HTTPS i certyfikat |
| TCP 8766 | **Nie** | Backend portalu wyłącznie na `127.0.0.1` |
| TCP 8080 | **Nie** | Backend kafelków squaremap wyłącznie na `127.0.0.1` |

Konfiguracje produkcyjne (pliki prywatne, niecommitowane):
- `/etc/caddy/Caddyfile`: publiczna nazwa HTTPS, `reverse_proxy 127.0.0.1:8766`.
- `/opt/earthsmp/plugins/EarthSMPCore/config.yml`: `map-portal.bind-address: "127.0.0.1"`, `port: 8766`, `public-origin: "https://<HOST_MAPY>"`, `squaremap-internal-url: "http://127.0.0.1:8080"`.
- Domyślny `config.yml` w zasobach JAR może różnić się od istniejącego pliku użytkownika. Nie podmieniaj produkcyjnej konfiguracji nowym plikiem domyślnym bez porównania.

Szybka kontrola:

```bash
sudo ufw status verbose
sudo ss -lntp | grep -E ':(8080|8766|25565|443)\b'
systemctl is-active earthsmp caddy
sudo journalctl -u caddy -n 50 --no-pager
curl -sS -o /dev/null -w '%{http_code}\n' -H 'Host: <HOST_MAPY>' http://127.0.0.1:8766/
curl -sS -o /dev/null -w '%{http_code}\n' https://<HOST_MAPY>/
```

Dwa ostatnie żądania powinny dać 200 dla strony głównej. Nie wyłączaj 8080, bo portal pobiera z niego kafelki. **Nie wystawiaj 8080 ani 8766 bezpośrednio do Internetu.** Certyfikat HTTPS i wstępne działanie strony zostały sprawdzone; testy surowych endpointów oraz izolacji pozycji pomiędzy państwami pozostają osobnym punktem przed premierą.

## 4. Paper — uruchamianie, zatrzymywanie, logi

```bash
sudo systemctl status earthsmp --no-pager
sudo journalctl -u earthsmp -n 100 --no-pager
sudo journalctl -u earthsmp -f
sudo systemctl restart earthsmp
sudo systemctl stop earthsmp
sudo systemctl start earthsmp
```

`Ctrl+C` kończy podgląd `journalctl -f`, a **nie** proces Paper. Po `start` w logach szukaj `Done (...)` oraz braku błędów pluginów; samo `active (running)` oznacza tylko działający proces.

Usługa: `/etc/systemd/system/earthsmp.service`; `User=minecraft`, `WorkingDirectory=/opt/earthsmp`, `Restart=on-failure`. Ustawienie limitu pamięci z konfiguracji usługi: do 8 GB dla JVM; nie przydzielaj całej pamięci VPS Javie.

Nie wywołuj `/reload` podczas aktualizacji pluginów; restartuj Paper przez systemd.

## 5. Backup offline na VPS

**Faktycznie uruchomione i sprawdzone:**
- Skrypt `/usr/local/sbin/earthsmp-backup` zatrzymuje usługę Paper, tworzy archiwum z `/opt/earthsmp` (pomija odtwarzalne `logs`, `cache`, `session.lock`), kontroluje odczyt archiwum i ponownie uruchamia Paper, także po niektórych błędach skryptu.
- Kopie: `/var/backups/earthsmp/EarthSMP_RRRR-MM-DD_GG-MM-SS.tar.gz`, uprawnienia root:esmpadmin i 640.
- Timer: `earthsmp-backup.timer`, **codziennie o 07:00 `Europe/Warsaw`**; `Persistent=false` (nie odrabia zaległego backupu w środku rozgrywki).
- Retencja: skrypt usuwa pliki pasujące do wzorca starsze niż określony próg `find -mtime +14`; to przybliżona retencja, nie ścisłe czternaście ostatnich kopii.
- Po pierwszym uruchomieniu backupu Paper ponownie był aktywny. Kopia została pobrana na Windows i kontrola `tar -tzf` zakończyła się powodzeniem.

Ręczne uruchomienie (chwilowa przerwa w działaniu gry i portalu; powiadom graczy):

```bash
sudo /usr/local/sbin/earthsmp-backup
sudo ls -lh /var/backups/earthsmp/
systemctl is-active earthsmp
```

Kontrola harmonogramu:

```bash
systemctl list-timers --all | grep earthsmp
sudo journalctl -u earthsmp-backup.service -n 80 --no-pager
```

**Ograniczenia:** backup wykonywany lokalnie na tym samym VPS nie chroni przed utratą całej maszyny. Pobranie jednej kopii na Windows zostało potwierdzone; **automatycznego eksportu poza VPS jeszcze nie ma**. Snapshoty hostingowe nie zastępują eksportowalnego backupu. Nie ma jeszcze potwierdzonego **pełnego odtworzenia** z produkcyjnego archiwum.

Pobranie backupu na Windows (w lokalnym PowerShellu; plik jest czytelny dla grupy `esmpadmin`):

```powershell
scp esmpadmin@<PUBLICZNE_IP_VPS>:/var/backups/earthsmp/<PLIK_BACKUPU>.tar.gz .
tar -tzf .\<PLIK_BACKUPU>.tar.gz > $null
```

Uwaga: oparcie kopii wyłącznie na lokalnym komputerze wymaga regularnego pobierania; komputer musi być włączony, jeśli w przyszłości zostanie skonfigurowana automatyzacja.

## 6. Odtworzenie backupu — procedura próbna

**Status na 8 listopada: NIEPRZETESTOWANE OD POCZĄTKU DO KOŃCA.** Nie próbuj przywracać kopii w działającym katalogu produkcyjnym.

1. Wybierz istniejące archiwum `EarthSMP_*.tar.gz`, sprawdź rozmiar i integralność `tar -tzf`.
2. Utwórz osobne środowisko testowe (osobny katalog / instancję, bez równoczesnego udostępnienia tych samych portów).
3. Rozpakuj backup do **pustego katalogu testowego**, nigdy wprost na działającym `/opt/earthsmp`.
4. Sprawdź obecność `Earth/level.dat`, `plugins/EarthSMPCore/earthsmp.db`, plików konfiguracyjnych i pluginów.
5. Uruchom odtworzony serwer testowo z właściwą wersją Java/JAR i osobnymi portami; potwierdź dane świata i bazy, działanie `/esmp clock`, mapy i przynajmniej jednego stanu pluginu.
6. Udokumentuj wynik testu. Dopiero przetestowane odtworzenie pozwala zaznaczyć ten punkt checklisty jako ukończony.

Przykład **samego rozpakowania do testów** (nie uruchamia Paper):

```bash
mkdir -p "$HOME/earthsmp-restore-test"
tar -xzf /ścieżka/do/backup.tar.gz -C "$HOME/earthsmp-restore-test"
test -f "$HOME/earthsmp-restore-test/Earth/level.dat"
test -f "$HOME/earthsmp-restore-test/plugins/EarthSMPCore/earthsmp.db"
```

Jeżeli odtwarzasz po awarii na produkcji: zatrzymaj Paper, zachowaj kopię uszkodzonego stanu, odtwórz **świat i SQLite z tego samego punktu czasowego**, sprawdź właściciela `minecraft` i uprawnienia, a następnie uruchom Paper i sprawdź logi. Nie zastępuj samej bazy z innej daty.

## 7. Aktualizacja EarthSMP-Core

Kod źródłowy jest w repo `EarthSMP-Core/`; serwer VPS używa wyłącznie skompilowanego JAR-a. Zachowaj tylko **shaded JAR** (z zależnościami), nie `-plain.jar`.

Przykładowy workflow:

1. Lokalnie zaktualizuj kod, uruchom testy i zbuduj JAR `cd EarthSMP-Core && ./gradlew clean build` (na Windows `gradlew.bat clean build`).
2. Zrób backup przy zatrzymanym Paper lub uruchom sprawdzony skrypt backupu; odnotuj wersję i ewentualne zmiany schematu SQLite.
3. Prześlij nowy JAR przez `scp` do katalogu domowego `esmpadmin`, potem **przy zatrzymanym Paper** przenieś go do `/opt/earthsmp/plugins/`, usuwając lub archiwizując poprzedni JAR tej samej wtyczki, aby nie załadować dwóch wersji.
4. Ustaw właściciela `minecraft:minecraft` i prawa pliku `640`.
5. Uruchom Paper, sprawdź `Done`, wersję w logach, SQLite, portal oraz podstawowe komendy. W razie błędu wróć do poprzedniego JAR-a i **odpowiedniego backupu** (szczególnie po migracji schematu).

Źródło pluginu i dokumentacja są w publicznym Git; produkcyjne `plugins/`, `config/`, świat i bazy SQLite są ignorowane w `.gitignore`.

## 8. Dzień 0 i otwarcie edycji

- Jedyna oryginalna kopia czystego świata to **`MAIN.zip`**, przechowywana lokalnie — **nie nadpisywać ani nie usuwać**. Świat został z niej przywrócony przed migracją.
- Obecnie: nowa SQLite, brak testowych państw, dzień 0. Budowa spawnu Makau i sklepów administracyjnych przed premierą.
- `/esmp clock`: kontrola stanu zegara. `/esmp clock start`: **jednorazowy start edycji**, zapisuje czas w SQLite i uruchamia progresję dni.
- Zegar jest niezależny od uptime Paper; następne dni zaczynają się o 06:00 czasu polskiego. Testy i aktualizacje VPS **nie** powinny uruchamiać zegara.
- Po zbudowaniu spawnu: wykonaj punkt odtworzenia **PRE-LAUNCH**, wyczyść przedpremierowe ekwipunki i postępy budowniczych, zweryfikuj WorldGuard oraz sklepy; dopiero potem wyznacz start.
- Koszt pierwszego miesiąca VPS poniósł administrator; ewentualna zbiórka na kolejne okresy jest planowana później, bez deklaracji konkretnej daty i warunków.

## 9. Rzeczy pozostałe przed premierą

Szczegółowy wykaz: [CHECKLISTA-STARTU.md](CHECKLISTA-STARTU.md). Najważniejsze: przygotowanie spawnu i sklepów, niezależne odtworzenie backupu, izolacja danych mapy pomiędzy państwami oraz surowe API, Simple Voice Chat, próba z większą grupą (w tym koalicja 3 państw, gdy znajdą się testerzy). Stan testów nie jest równoznaczny z gwarancją braku exploitów.
