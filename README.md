# Riftverse – Multiversum-Mod für NeoForge 1.21.1

Baue eine Rift-Kanone, schieß Portale in zufällig generierte Universen mit eigener
Physik und triff dort auf Bosse, deren Geschichte in Echtzeit erzeugt wird.

## Was drin ist

| Bereich | Inhalt |
|---|---|
| **Erz** | Quantenerz in der Oberwelt (Y −56 bis 40, Eisenspitzhacke) und in Rift-Universen |
| **Maschinen** | Quanten-Raffinerie, Fluid-Synthesizer, Rift-Assembler (Ofen-Brennstoff, Trichter/Rohre kompatibel) |
| **Rift-Kanone** | 3 Modi: Zufall, Heimat, Letztes Universum; Mk-II-Upgrade mit 4 Universums-Splittern |
| **Universen** | ~360.000 zufällige Universen in 4 Dimensionen (Sonne, Ewige Nacht, Inferno, Leere) |
| **Physik** | Gravitation 0,3–1,8 g, dünne/dichte/giftige Atmosphäre, Hitze/Kälte, Kreaturengröße |
| **Bosse** | 7 Persönlichkeiten, 6 Fähigkeiten, 5 Plot-Twists, Gnade-Entscheidung, Chronik-Buch |

## Starten (Windows)

1. **JDK 21** installieren, z. B. [Eclipse Temurin 21](https://adoptium.net/).
2. ZIP entpacken, in den Ordner wechseln und in PowerShell ausführen:
   ```
   .\gradlew.bat runClient
   ```
   Beim ersten Mal lädt Gradle Minecraft + NeoForge herunter (dauert ein paar Minuten).
   Danach startet Minecraft direkt mit dem Mod.
3. **Neue Welt** erstellen (empfohlen), Kreativ- oder Überlebensmodus.

Zum Testen im Kreativmodus: Reiter „Riftverse“ im Inventar, oder
`/give @s riftverse:rift_gun` und `/give @s riftverse:rift_fluid_cell 32`.

## Als Teil eines Modpacks

```
.\gradlew.bat build
```
Die fertige Datei liegt unter `build/libs/riftverse-1.0.0.jar`. Diese in den `mods`-Ordner
einer **NeoForge-1.21.1**-Instanz legen (CurseForge-App, Prism Launcher, Modrinth App).
Dort kannst du weitere NeoForge-1.21.1-Mods dazupacken.

## Spielablauf (Überleben)

1. **Quantenerz** abbauen → Rohe Quantensplitter.
2. **Quanten-Raffinerie** bauen (7 Eisen um einen Ofen, Redstone unten in der Mitte).
   Splitter + Redstone (oder Glowstone-Staub für doppelte Ausbeute) → Quantenbarren.
3. **Fluid-Synthesizer**: Barren + Glas → 2 Kartuschen, Barren + Schleimball → 4 Kartuschen.
4. **Rift-Assembler**:
   * 4 Barren + Enderauge → Rift-Kern (oder 6 Barren + Diamant ohne Nether)
   * Kanonen-Rahmen (Werkbank) + Rift-Kern → **Rift-Kanone**
   * Rift-Kanone + 4 Universums-Splitter → **Rift-Kanone Mk II**
5. Fährst du mit der Maus über den Pfeil in einer Maschine, zeigt sie dir ihre Rezepte.

### Rift-Kanone
* **Rechtsklick**: Portal öffnen (1 Kartusche). Reinlaufen = Reise. Mobs und Items können mit.
* **Schleichen + Rechtsklick**: Modus wechseln.
* Beim Abflug aus der Oberwelt merkt sich die Kanone den Ort als **Heimat**.
* Am Ziel öffnet sich kurz ein **orangefarbenes Rückportal**.

### Universums-Scanner
Rechtsklick zeigt Gravitation, Atmosphäre, Temperatur, Gelände, Rohstoffe, Gefahr und den
Story-Status des aktuellen Universums.

## Physik der Universen
Bewusst an echte Physik angelehnt:
* **Gravitation** ändert die Fallbeschleunigung. Weil die Sprungkraft gleich bleibt, springst du
  bei 0,5 g doppelt so hoch. Fallschaden skaliert mit g, die sichere Fallhöhe mit 1/g.
* **Kreaturen** sind bei niedriger Schwerkraft größer, bei hoher kleiner (Quadrat-Kubik-Gesetz).
* **Dichte Luft** bremst Bewegung, **dünne Luft** macht Sprinten anstrengend (Hunger).
* **Giftige Luft** vergiftet dich ohne Helm.
* **Kälte** lässt dich erfrieren, außer du trägst Leder. Gewässer frieren zu.
* **Hitze** erhöht den Hunger; statt Wasser gibt es Lava. In der Inferno-Dimension verdampft Wasser
  und Betten explodieren.
* Zwischen Universen liegt ein **Abgrund aus Leere** – das Ende des Universums.

## Story-Generator
Kein KI-Modell nötig: `story/StoryEngine.java` setzt Handlungen aus Bausteinen zusammen –
Persönlichkeit × Ereignis × Kontext (Physik, deine Lebenspunkte, wie oft du gestorben bist,
dein Item, Kampfdauer) × Zufall. Ablauf:

1. Du betrittst ein neues Universum → nach ~6 s kommt ein **Rift-Funkspruch** mit Auftrag.
2. Ein Lichtstrahl markiert die **Boss-Arena** 40–70 Blöcke entfernt.
3. Der Boss redet: Begrüßung, Reaktionen, Phasenwechsel, **Plot-Twist** bei 33 %.
4. Manche Bosse **betteln** bei 20 % um Gnade: Rechtsklick = verschonen, Angriff = beenden.
5. Epilog, Spitzname bei den Bewohnern und eine **Chronik** als Buch.

Neue Sätze, Titel oder Motive einfach in `StoryEngine.java` bzw. `NameGen.java` ergänzen.

## Projektstruktur

```
src/main/java/dev/riftverse/
  Riftverse.java          Einstieg, Registrierung
  registry/               Blöcke, Items, Entities, Menüs, Weltgenerierung
  machine/                Maschinen (Block, BlockEntity, Menü, Rezepte)
  item/                   Rift-Kanone, Scanner
  portal/                 Portal-Entity, Reise-Logik
  universe/               Universums-Profile, Chunk-Generator, Biome, Physik
  boss/                   Boss-Entity, Fähigkeiten
  story/                  Story-Generator, Regie, Speicherstand
  client/                 GUI, Portal- und Boss-Renderer
src/main/resources/
  data/riftverse/         Dimensionen, Biome, Rezepte, Loot, Erzgenerierung
  assets/riftverse/       Texturen, Modelle, Sprachen (de/en)
```

## Hinweise
* Der Code wurde ohne Testlauf geschrieben. Falls beim Bauen Fehler kommen: Fehlermeldung
  kopieren und mitschicken, dann wird's gezielt behoben.
* Namen, Texturen und Figuren sind eigene Entwürfe, inspiriert vom Multiversum-Genre.

## Lizenz
Dieses Projekt ist unter der [CC BY-NC-SA 4.0](https://creativecommons.org) lizenziert.
