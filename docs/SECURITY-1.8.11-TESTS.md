# EarthSMP-Core 1.8.11 — kandydat: alerty bezpieczeństwa

**Status:** gałąź testowa, nie wdrażać jako zweryfikowanego wydania bez `./gradlew.bat clean build` i testów na Paper 26.3 / Java 25.

## Zakres
- Core loguje zdarzenia typu `ITEM_LIMIT`, `INVENTORY_OVERFLOW`, `ENCHANT_DENIED`, `ENCHANT_RESULT`, `ENCHANT_NORMALIZED`, `CORE_ANCHOR`, `PROTECTED_BREAK`, `PROTECTED_PLACE` i `PROTECTED_CONTAINER`.
- Dzienny dziennik JSONL (UTC): `plugins/EarthSMPCore/security-events/YYYY-MM-DD.jsonl`. Zapisuje także pojedyncze odmowy (nawet bez alertu).
- Domyślnie próg alertu dla administratorów: 4 podobne zdarzenia tego samego gracza i typu w 60 sekund; cooldown 60 sekund. Poważne automatyczne korekty ekwipunku i atak na rdzeń zgłaszają się od razu, ale z cooldownem.
- `/esmp security [1-30]`: ostatnie zdarzenia od włączenia pluginu. Historia trwała jest w plikach JSONL.
- Gracz może zobaczyć ostrzeżenie po powtarzających się próbach; żadnych automatycznych banów/kicków.
- Opis `enforced` dotyczy blokady/korekty istniejącego Core, nie orzeczenia, że gracz świadomie oszukiwał.
- Powiadomienia widzą konta z `earthsmp.security.alerts` (domyślnie OP). Konsola rejestruje emitowane alerty.

## Konfiguracja
```yaml
security:
  enabled: true
  alert-threshold: 4
  alert-cooldown-seconds: 60
  warn-player-on-repeat: true
```

W przypadku serwera aktualizowanego z 1.8.10 dopisz sekcję ręcznie do istniejącego `plugins/EarthSMPCore/config.yml`, gdyż `saveDefaultConfig()` nie podmienia już istniejącego pliku. Przy braku kluczy używane są domyślne wartości z Javy.

## Integracja z antycheatem i antydupe
GrimAC oraz wybrany antydupe to **oddzielne pluginy**. Ich alerty, reguły karania i wersje trzeba zweryfikować we własnych konfiguracjach. Ten kod nie integruje ich API i nie twierdzi, że zatrzymuje duplikację itemów. Nie ustawiaj automatycznych banów, dopóki nie zostanie oceniony poziom false positives. Wtyczki należy pobierać z oficjalnych stron i sprawdzić zgodność z Paper 26.3 / Java 25 na testowym serwerze.

## Testy wymagane przed merge
1. `cd EarthSMP-Core` i `./gradlew.bat clean build` na JDK 25. Do instalacji użyj wyłącznie shaded JAR (bez `-plain`).
2. Wykonaj backup offline świata i SQLite, zainstaluj JAR na **kopii testowej**, sprawdź `/plugins`, `/version EarthSMPCore` i logi startowe.
3. Jako zwykły gracz spróbuj wejść w chronioną skrzynię, niszczyć blok bazy, wykopać rdzeń; sprawdź że zasady z 1.8.10 nadal działają.
4. Powtórz kilka zabronionych prób z nadmiarem perł/zakazanym przedmiotem. Sprawdź komunikaty, alert dla OP, cooldown i brak spamu; prawidłowe operacje nie powinny tworzyć alertów.
5. Wstaw niedozwolony enchant na testowy przedmiot i sprawdź normalizację. Oferta villagera z niedozwolonym enchantem sama w sobie **nie świadczy o oszustwie gracza** i nie jest przypisywana graczowi.
6. `/esmp security 10` w konsoli/OP i z konta zwykłego gracza. Sprawdź format `security-events/YYYY-MM-DD.jsonl` i trwałość po restarcie.
7. Zrób oddzielne testy kompatybilności z Shopkeepers, GrimAC i antydupe. Dopiero potem pełne testy multiplayer.

## Pozostałe działania
- Dobrać i ręcznie skonfigurować niezależny antycheat i antydupe; to **nie jest częścią** wydania Core 1.8.11.
- Osobny test odporności na eksploity na kopii świata/bazy, testowanie z różnym pingiem i obciążeniem.
- Nie edytować głównego `docs/docs.md` przed zamknięciem i przetestowaniem etapu.
