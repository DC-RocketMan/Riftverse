# Riftverse

Ein Minecraft-Modpack im Stil von Feed The Beast, inspiriert von Rick and Mortys Portalreisen.
Dieses Repo enthält den Riftverse-Mod für **NeoForge 1.21.1**. Die Anleitung für alles Weitere steht im
Design-Dokument („Riftverse – Design-Dokument“), gebaut wird Meilenstein für Meilenstein.

## Stand: Meilenstein 1 – Fundament

| Bereich | Inhalt |
|---|---|
| **Metalle** | Zinnerz (Oberwelt, per Config abschaltbar), Rohes Zinn, Zinnbarren und -block; Staub, Platten, Drähte, Hülsen, Stangen; Bronze, Stahl, Mischmetall; Xenit-Erz, -Barren, -Platte, -Staub, -Stange (Krater-Generierung folgt in Meilenstein 2) |
| **T0 Garage** | Garagen-Werkbank mit Werkzeugslots für Hammer, Lötkolben und Lötzinn; Garagenpresse und Kurbel-Dynamo mit der Handkurbel |
| **T1 Maschinen** | Erzmühle (1 Erz → 2 Staub), Walzbank (Walzen, Schneiden, Ziehen), Legierungsofen (Bronze, Stahl), Tonnenreaktor (verbrennt jedes Item) |
| **Energienetz Stufe I** | Eigenes Riftverse-Netz in FE mit IC2-Regeln: Pakete mit Spannungsstufe, Kabelverlust 0,5 FE pro Block, Überspannung (explodieren, durchbrennen oder aus), blanke Kabel geben einen Schlag |
| **Kabel** | Kupferkabel blank (4 px) und isoliert (6 px), isolierte Kabel mit Farbstoff einfärbbar; unterschiedlich gefärbte Kabel verbinden sich nicht |
| **Kompatibilität** | Einspeiser (FE von jedem Mod rein) und Ausspeiser (normales FE raus); Maschinen zeigen ihren Füllstand nur lesbar an |
| **Akku** | Energiezelle (40.000 FE) als reines FE-Item; jede Maschine und jeder Generator hat einen Akku-Slot |
| **Test** | Kreativ-Quelle (nur Kreativmodus): unendliche Quelle, Rechtsklick wechselt Stufe I–IV, um Überspannung zu testen |

Der Prototyp aus dem ersten Entwurf (Quantenerz, Rift-Kanone, Bosse, Story-Generator) ist noch drin
und wird in Meilenstein 2 durch Xenit, Kanone Mk I und friedliche Universen ersetzt.

### So spielt sich Meilenstein 1

1. **Garagen-Werkbank** in der normalen Werkbank bauen (Eisen, Kupfer, Holzstämme, Werkbank).
2. In der Garagen-Werkbank **Bronzestaub** von Hand mischen (3 Kupfer + 1 Zinn → 2), im Ofen zu Bronze schmelzen.
3. **Garagen-Hammer** bauen und in den Werkzeugslot legen: 2 Barren → 1 Platte.
4. **Garagenpresse**, **Handkurbel** und **Kurbel-Dynamo** bauen. Mit der Handkurbel Rechtsklick gedrückt
   halten: Die Presse macht 1 Barren → 1 Platte, der Dynamo liefert 8 FE/t.
5. **Lötkolben** und **Lötzinn** (Zinnbarren + Hammer) in die Werkzeugslots legen – die T1-Maschinen werden gelötet.
6. T1-Maschinen brauchen das **Xenit-Gehäuse**. Xenit gibt es im Überleben erst ab Meilenstein 2
   (Meteoritenkrater); bis dahin im Kreativmodus testen.

Rechtsklick mit leerer Hand auf ein Kabel zeigt das Teilnetz: Quellen, Verbraucher, Leitungen und übertragene FE/t.

### Entscheidungen in Meilenstein 1

Wo das Design-Dokument nichts festlegt oder sich widerspricht, gilt bis auf Weiteres:

- **Bronze in T0:** Der Legierungsofen ist T1, Bronze wird aber schon in T0 gebraucht. Deshalb gibt es eine
  Handmischung in der Garagen-Werkbank mit halber Ausbeute (3 Kupfer + 1 Zinn → 2 Bronzestaub);
  der Legierungsofen macht 3 + 1 → 4.
- **Akku-Slot:** Maschinen ziehen Strom aus der Energiezelle, Generatoren laden sie.
- **Lötzinn:** Zinnbarren + Hammer → 1 Lötzinn, in der Walzbank Zinnplatte → 3 Lötzinn.
- **Schaltkreise:** Basis-Schaltkreise kommen mit dem Platinen-Ätzer erst in Meilenstein 2. Die T1-Maschinen
  aus Meilenstein 1 werden deshalb gelötet statt mit Schaltkreis gebaut; Meilenstein 2 kann die Rezepte nachschärfen.
- **Isolierfolie:** Polymerfetzen kommen eigentlich aus dem Recycler (Meilenstein 2). Bis dahin wirft der
  Tonnenreaktor gelegentlich welche als Kleinteil aus.
- **Tonnenreaktor:** jedes Item brennt 50 Ticks, Schrott 100 Ticks, Brennstoffe die Hälfte ihrer Ofen-Brenndauer;
  Kohle bringt so 32.000 FE.
- **Garagenpresse:** braucht 40 Kurbel-Ticks pro Platte (2 Sekunden Kurbeln). Create-Rotation folgt in Phase 2.
- **Kabel verkleiden** (Rechtsklick mit einem Block) ist noch nicht drin.

## Config

`config/riftverse-common.toml`:

| Option | Standard | Wirkung |
|---|---|---|
| `energie.overvoltage` | `EXPLODE` | `EXPLODE`, `BURNOUT` oder `OFF` |
| `energie.cable_shock` | `true` | Schlag an blanken Kabeln unter Strom |
| `welt.tin_ore` | `true` | Zinnerz generieren (aus, wenn ein anderer Mod Zinn liefert) |

## Bauen und Starten

Voraussetzung: **JDK 21**, z. B. [Eclipse Temurin 21](https://adoptium.net/).

```
.\gradlew.bat runClient     # Windows: Testclient starten
./gradlew build             # Jar unter build/libs/
```

Bei jedem Push baut GitHub Actions den Mod (`.github/workflows/build.yml`). Ein Meilenstein ist erst fertig,
wenn dieser Check grün ist; das fertige Jar liegt als Artefakt am Workflow-Lauf.

## Projektstruktur

```
src/main/java/dev/riftverse/
  energy/       Riftverse-Netz: Graph, Pakete, Spannungsstufen, Überspannung, Ein-/Ausspeiser
  cable/        Kupferkabel blank und isoliert
  tech/         Maschinen, Generatoren, Garagen-Werkbank (+ menu/ für die GUIs)
  recipe/       Rezepttypen der Maschinen und der Garagen-Werkbank
  worldgen/     per Config abschaltbare Erz-Generierung
  client/       Screens (ohne Texturdateien gezeichnet), Kabelfarben
  registry/     Registrierung aller Blöcke, Items, Rezepttypen …
  machine/, item/, portal/, universe/, boss/, story/   Prototyp aus dem ersten Entwurf
src/main/resources/
  assets/riftverse/   Texturen, Modelle, Blockstates, Sprachen
  data/riftverse/     Rezepte (pro Maschine ein Ordner), Loot, Worldgen, Tags
tools/
  texturen/   Texturgenerator (Python, Pillow + NumPy); meilenstein1.py ergänzt die neuen Texturen
  daten/      generate.py erzeugt Modelle, Blockstates, Loot, Tags, Rezepte und Sprachen für Meilenstein 1
```

Rezepte ändern: in `tools/daten/generate.py` eintragen und `python3 tools/daten/generate.py` laufen lassen.
Rezepte sind JSON im Datapack, jede Maschine hat ihren eigenen Rezepttyp, z. B.

```json
{
  "type": "riftverse:legierungsofen",
  "ingredients": [
    {"ingredient": {"tag": "c:ingots/copper"}, "count": 3},
    {"ingredient": {"tag": "c:ingots/tin"}}
  ],
  "result": {"id": "riftverse:bronze_ingot", "count": 4},
  "time": 200
}
```

Rezepte der Garagen-Werkbank haben die Typen `riftverse:werkbank_shaped` und `riftverse:werkbank_shapeless`
mit den Zusatzfeldern `hammer` (Haltbarkeit pro Craft) und `loetzinn` (verbrauchtes Lötzinn).

## Lizenz

Dieses Projekt ist unter der [CC BY-NC-SA 4.0](https://creativecommons.org/licenses/by-nc-sa/4.0/) lizenziert.

Sie sind frei und berechtigt:
- Teilen: Das Material in jedwedem Format oder Medium zu vervielfältigen und weiterzuverbreiten.
- Bearbeiten: Das Material zu remixen, zu verändern und darauf aufzubauen.

Unter folgenden Bedingungen:
1. Namensnennung (Attribution): Sie müssen angemessene Urheber- und Rechteangaben machen, einen Link zur Lizenz beifügen und angeben, ob Änderungen vorgenommen wurden.
2. Nicht-kommerziell (Non-Commercial): Sie dürfen das Material nicht für kommerzielle Zwecke nutzen.
3. Weitergabe unter gleichen Bedingungen (ShareAlike): Wenn Sie das Material remixen, verändern oder anderweitig direkt darauf aufbauen, müssen Sie Ihre Beiträge unter derselben Lizenz wie das Original veröffentlichen.
