# Timer (Fabric 1.21.4)

Mod kliencki. Odlicza czas do odrodzenia bloków.

## Budowanie
Potrzebujesz Javy 21 i Gradle (albo IntelliJ, który zrobi to sam).
    gradle wrapper --gradle-version 8.12   # jednorazowo
    ./gradlew build
Gotowy plik: build/libs/timer-1.0.0.jar -> wrzuć do .minecraft/mods (+ Fabric API).

## Użycie
- K  - menu moda (dodawanie dowolnego bloku po id / z ręki, opcjonalny ręczny czas)
- J  - dodaj/usuń blok, na który patrzysz
- Na serwerze z listy autoServers (domyślnie minestar.gg) mod śledzi WSZYSTKIE bloki,
  które znikają i wracają, sam mierzy czas regeneracji i potem odlicza.
- Konfiguracja: config/timer.json
- L  - wygląd HUD: wybór rogu ekranu, rozmiar (50%-300%) i margines, z podglądem na żywo
