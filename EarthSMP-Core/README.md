# EarthSMP Core — etap 3.3.1

Minimalny szkielet autorskiego pluginu Paper 26.3 (Java 25), przygotowany do umieszczenia w repozytorium `Wedel-Sigma/EarthSMP`.

**To jeszcze nie implementuje państw, wojen ani ochrony baz.** Pierwszy cel: skompilować plik JAR, załadować go na serwerze i wykonać `/earthsmp status`.

## Wymagania

- JDK 25 (ta sama główna wersja co serwer).
- Gradle 9.1+ (lub IDE wyposażone w obsługę Gradle; w archiwum **nie ma** wrappera Gradle).
- Dostęp do internetu przy pierwszym budowaniu, by pobrać `paper-api`.

## W VS Code / PowerShell

Otwórz katalog `EarthSMP-Core` jako projekt Java/Gradle. W terminalu:

```powershell
gradle build
```

JAR powstanie pod `build/libs/EarthSMP-Core-0.1.0.jar`.

1. Wyłącz serwer Paper (komenda `stop`).
2. Skopiuj JAR do katalogu serwera `plugins/`.
3. Uruchom serwer ponownie (bez `/reload`).
4. Sprawdź `/plugins` i `/earthsmp status` jako OP.
5. Powinien wyświetlić `EarthSMP Core 0.1.0 - plugin dziala.` i `Swiat Earth: zaladowany`.

Jeśli komenda jest niedostępna dla admina bez OP, nadaj w LuckPerms węzeł `earthsmp.admin` grupie `admin`.

## Co wprowadzamy później

Rozwój będzie etapowy: dane państw i członkostwo → terytoria → rdzenie → dyplomacja → oblężenia → ograniczenia PvP → integracja z mapą. Nie implementujemy wszystkiego na raz.

## Uwaga o repozytorium

`EarthSMP-Core/.gitignore` wyklucza katalogi `build/` i `.gradle/` tego modułu. Nie wrzucaj skompilowanego JAR-a ani baz danych pluginu do GitHuba.

Stan pluginu i jego komendy są niezależne od pluginów WorldGuard, LuckPerms czy squaremap. Integracje dodamy dopiero wtedy, gdy będą potrzebne.
