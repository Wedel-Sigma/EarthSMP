# EarthSMP — karta awaryjna (VPS)

> **Kabina Narodów 2.0 · Ubuntu 24.04 · Paper + Java 25.**
> Szybka ściąga dla administratora. Pełne procedury: [OPERACJE-VPS.md](OPERACJE-VPS.md).
> **Nie wykonuj `/esmp clock start` przed premierą.**

## 1. Najpierw sprawdź, co padło

Połącz się z VPS: `ssh esmpadmin@<IP_VPS>`. Następnie:

```bash
systemctl is-active earthsmp caddy
sudo systemctl status earthsmp --no-pager
sudo journalctl -u earthsmp -n 60 --no-pager
df -h /opt/earthsmp
free -h
```

**`active` to tylko działający proces, nie dowód pełnego uruchomienia Paper.** Szukaj w logach `Done (...)` i błędów.

## 2. Minecraft nie działa / gracze nie mogą wejść

```bash
sudo journalctl -u earthsmp -n 100 --no-pager
sudo systemctl restart earthsmp
sudo journalctl -u earthsmp -f
```

Restart **rozłączy graczy**. Jeśli problem wraca, sprawdź wolne miejsce (`df -h`), RAM (`free -h`) i błędy Java/SQLite/pluginów. Nie restartuj w pętli. **`Ctrl+C` kończy podgląd logu, nie Paper.**

## 3. Minecraft działa, ale mapa WWW nie

```bash
systemctl is-active caddy earthsmp
sudo journalctl -u caddy -n 60 --no-pager
sudo ss -lntp | grep -E ':(443|8766|8080)\b'
curl -sS -o /dev/null -w 'portal: %{http_code}\n' -H 'Host: <HOST_MAPY>' http://127.0.0.1:8766/
curl -sS -o /dev/null -w 'HTTPS: %{http_code}\n' https://<HOST_MAPY>/
```

- **Lokalny portal 200, HTTPS nie działa:** sprawdź Caddy, DNS/certyfikat i porty 80/443. Po sprawdzeniu logów ewentualnie `sudo systemctl restart caddy`.
- **Portal 8766 nie odpowiada:** to część EarthSMP-Core; sprawdź logi `earthsmp`, a w razie potrzeby zrestartuj **Paper**, uprzedzając graczy.
- **Brak kafelków:** sprawdź lokalną Squaremap na 8080. **Nie otwieraj portów 8080 i 8766 do Internetu!**

## 4. Lagi, brak RAM-u albo pełny dysk

```bash
uptime
free -h
df -h
top
```

`q` zamyka `top`. Jeśli dysk jest pełny, **nie usuwaj w ciemno** świata, SQLite ani backupów. Najpierw sprawdź, co zajmuje miejsce; jeśli Paper zgłasza błędy zapisu, zatrzymaj usługę i ustal przyczynę. W przypadku OOM/spadków TPS zachowaj logi.

## 5. Podejrzenie uszkodzenia świata / SQLite / duplikacji

**Najważniejsze: przerwij zapisy i nie pogarszaj stanu.**

```bash
sudo systemctl stop earthsmp
systemctl is-active earthsmp
sudo ls -lh /var/backups/earthsmp/
```

**Nie kasuj `earthsmp.db` ani folderu `Earth`. Nie podmieniaj samej bazy lub samego świata z innego dnia.** Zachowaj kopię obecnego (nawet uszkodzonego) stanu, żeby móc przeanalizować problem. Odtwarzanie wykonuj **ze spójnego backupu świata + SQLite**, zgodnie z [runbookiem](OPERACJE-VPS.md). Jeśli nie wiesz, co przywracać, **nie uruchamiaj Paper ponownie — napisz do mnie z logami**.

## 6. Backup nie zadziałał

```bash
systemctl list-timers --all | grep earthsmp
sudo journalctl -u earthsmp-backup.service -n 80 --no-pager
sudo ls -lh /var/backups/earthsmp/
```

Ręcznie (tylko kiedy Paper **normalnie działa**, a gracze wiedzą o przerwie):

```bash
sudo /usr/local/sbin/earthsmp-backup
```

Skrypt chwilowo zatrzymuje Paper i uruchamia go ponownie. **Nie uruchamiaj go jako narzędzia ratunkowego na podejrzanie uszkodzonej bazie lub po ręcznym zatrzymaniu serwera.** Kopie na VPS nie zastępują kopii poza VPS.

## 7. Nie działa SSH / straciłeś OP-a

- **SSH:** sprawdź lokalną sieć i IP. Jeśli nie możesz się dostać, korzystaj z **konsoli awaryjnej panelu Contabo**. Nie zmieniaj reguł UFW ani `sshd_config` w ciemno.
- **Brak OP:** nie trzeba resetować świata ani SQLite. Uprawnienia można przywrócić po zatrzymaniu Paper przez prawidłową edycję `ops.json` / zaufaną konsolę administracyjną. Zachowaj kopię `ops.json` przed zmianą.

## 8. Co mi wysłać przy awarii?

**Krótki opis:** co nie działa, od kiedy, co wydarzyło się wcześniej, czy gracze są online.

**Przydatne wyniki:**

```bash
sudo systemctl status earthsmp caddy --no-pager
sudo journalctl -u earthsmp -n 100 --no-pager
sudo journalctl -u caddy -n 50 --no-pager
df -h && free -h
```

**Nie wysyłaj** haseł, prywatnego klucza SSH, tokenów, całej bazy SQLite ani plików z sekretami.

---

**Złota zasada:** **najpierw logi → potem decyzja → dopiero restart lub odtwarzanie.** Żadnego `rm -rf`, `/reload` ani „resetu bazy na próbę”.
