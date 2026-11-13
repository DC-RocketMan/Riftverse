# Riftverse

Ein Minecraft-Modpack im Stil von Feed The Beast, inspiriert von Rick and Mortys Portalreisen.
Dieses Repo enthält den Riftverse-Mod für **NeoForge 1.21.1**. Die Anleitung für alles Weitere steht im
Design-Dokument („Riftverse – Design-Dokument“), gebaut wird Meilenstein für Meilenstein.

## Stand: Meilenstein 3 – T2 Rift-Labor

Mit dem Rift-Labor kommen Stufe II, die sechs T2-Maschinen, vier neue Generatoren, drei neue Universumstypen
und die Portal-Kanone Mk II mit Koordinatenkarten dazu.

| Bereich | Inhalt |
|---|---|
| **Stufe II** | Flux-Leitung (blank mit Schlag, isoliert, färbbar; 1 FE Verlust pro Block, verträgt 1.024 FE/t), Umspanner I ↔ II (Front = Hochspannung, Redstone schaltet von herunter- auf hochspannen) |
| **Flux-Zelle** | 16 Mio. FE, Stufe II, Füllstand als leuchtendes Kristallband, behält die Ladung beim Abbauen |
| **Gehäuse** | Labor-Gehäuse (Verbundplatten um ein Xenit-Gehäuse) für alle T2-Maschinen, Labor-Hülle für die Multiblöcke |
| **T2-Maschinen** | Präzisions-Montierer (sequenzielle Montage, die Reihenfolge der Slots zählt), Quantenzentrifuge, Gravitationspresse, Dimensions-Scanner, Chrono-Beschleuniger (2× / 4× / 8×), Portalfluid-Raffinerie (Multiblock 3 × 3 × 4) |
| **Generatoren** | Biomasse-Kessel (T1, 80 FE/t, Nährserum als Nebenprodukt), Zeitschleifen-Solarpanel (120 FE/t, Lichtschuld), Fluxkristall-Resonator (wachsendes Raster wie ein IC2-Reaktor, Hitze, Wasserkühlung), Gravitations-Pendel (Multiblock 3 × 5 × 3, 400 FE/t, Gravitation zählt) |
| **Zwischenprodukte** | Verbundplatte, Flux-Spule, Rift-Schaltkreis, Stabilisierte Linse, Kanonenkern, Chronoglas-Griff, Antigrav-Platte, Ätzlösung, Toxinrest, Flux-Stabilisator, Nährserum |
| **Stabilisiertes Portalfluid** | Aus der Raffinerie (Roh-Portalfluid + Säuregel + Fluxstaub), zerfällt nicht mehr, kommt in stabile Kartuschen |
| **Kanone Mk II** | Montiert in 5 Schritten aus Mk I, Kanonenkern, Chronoglas-Griff, 2 Verbundplatten und Flux-Spule; nur stabiles Fluid, 100 mB pro Schuss; Koordinatenkarte in der anderen Hand = gezielter Sprung |
| **Universen** | Niedriggravitationswelt (0,3 g, Schwebeinseln über dem Nichts, Schwebstein), Zeitdilatationswelt (4× Zufallsticks, Chronoglas-Sand fällt in Zeitlupe, Zeitverzerrungen), organische Welt (Biomasse, Blöcke wachsen nach, fressende Flora) |

### So spielt sich Meilenstein 3

1. **Verbundplatten** in der Garagenpresse pressen (Mischmetall + Xenit-Staub + Fluxstaub), daraus **Labor-Gehäuse** bauen.
2. Zuerst **Präzisions-Montierer** und **Quantenzentrifuge** bauen – sie kommen mit Basis-Schaltkreisen aus.
   Die Zentrifuge macht aus Säuregel **Ätzlösung**, der Montierer daraus **Rift-Schaltkreise**
   (Slot 1 Basis-Schaltkreis, Slot 2 zwei Golddrähte, Slot 3 Ätzlösung, Slot 4 Fluxstaub).
3. **Flux-Leitungen** und einen **Umspanner** legen: Stufe-II-Generatoren grillen sonst die T1-Maschinen.
4. Die **Portalfluid-Raffinerie** als Multiblock aufbauen (siehe unten) und mit Roh-Portalfluid aus Kartuschen
   oder per Rohr direkt aus dem Fluid-Mixer füttern. Sie füllt **stabile Kartuschen**.
5. Mit stabilem Fluid springt die **Kanone Mk I** zufällig auch in die drei T2-Welten: dort Schwebstein,
   Chronoglas-Sand und Biomasse holen.
6. **Antigrav-Platten** in der Gravitationspresse, **Chronoglas-Scheiben** in der Xenit-Schmelze,
   **Flux-Stabilisator** in der Zentrifuge, dann **Stabilisierte Linse**, **Kanonenkern** und **Kanone Mk II** im Montierer.
7. **Universumsprobe** in einem Universum füllen, im **Dimensions-Scanner** mit Papier zur **Koordinatenkarte**
   machen und mit der Mk II gezielt zurückspringen.

### Multiblöcke

Der Steuerblock sitzt unten in der Mitte der Vorderseite, der Bau geht 3 Blöcke breit und 3 tief nach hinten.
Ein Rechtsklick auf den Steuerblock nennt das erste fehlende Teil mit Koordinaten.

- **Portalfluid-Raffinerie (3 × 3 × 4):** Boden und Deckel ganz aus Labor-Hülle; in den zwei Lagen dazwischen
  Labor-Hülle an den vier Ecken, Glas an den vier Seitenmitten, die Mitte bleibt frei.
- **Gravitations-Pendel (3 × 5 × 3):** Boden und Deckel aus Labor-Hülle, vier Eckpfosten aus Labor-Hülle
  (3 hoch), in der Mitte auf halber Höhe der Schwebstein-Kern.
- **Fluxkristall-Resonator:** Kern mit 3 Spalten à 3 Plätzen, jede angrenzende Resonator-Kammer (bis 6) gibt eine
  Spalte dazu. Stäbe schwingen mit ihren Nachbarn (20 FE/t pro Puls, 4 Hitze pro Puls), Kühlkörper führen 12 Hitze/t ab,
  Wasser bringt 1,5-fache Leistung. Bei 10.000 Hitze explodiert der Kristall; Redstone schaltet ab.

### Entscheidungen in Meilenstein 3

- **Henne-Ei-Problem gelöst:** Kanone Mk II braucht Material aus T2-Welten, die laut Dokument erst Mk II erreicht.
  Deshalb springt die Mk I mit **stabilisiertem Portalfluid** zufällig auch in die T2-Typen; gezielt geht es erst
  mit Mk II. Die Raffinerie lässt sich komplett aus T1-Material bauen.
- **Stabilisator als Kompat-Tag:** Die Stabilisierte Linse fragt `riftverse:compat/stabilisator` ab. Ohne Pack steht
  dort der Flux-Stabilisator (Quantenzentrifuge: Fluxkristall + Säuregel), im Pack kann KubeJS den Source-Gem eintragen.
- **Umspanner:** spannt herunter bis zu 4 Pakete à 128 FE/t pro Tick, hoch ein Paket à 1.024 FE/t. Die Seiten haben
  im Netz feste Rollen, Strom fließt also nie „rückwärts“ durch.
- **Präzisions-Montierer:** Die Schritte sind die Slots in fester Reihenfolge (oben links beginnend); die GUI zeigt
  den aktuellen Schritt. Verbraucht wird alles am Ende, abgebrochene Montagen verlieren nichts.
- **Chrono-Beschleuniger:** Die Rückseite zeigt zur Maschine, Rechtsklick wechselt die Stufe. Generatoren lassen sich
  nicht beschleunigen, Pflanzen schon. Er hat keine GUI und deshalb keinen Akku-Slot.
- **Labor-Hülle statt Labor-Gehäuse** in den Multiblöcken, weil 25–29 Labor-Gehäuse pro Bau zu teuer wären.
- **Schwebeinseln:** Um jede Zellmitte liegt eine große Heimatinsel, auf der die Rifts landen; der Rest sind
  verstreute Inseln über dem Nichts.
- **Zeitdilatation:** Drei zusätzliche Runden Zufallsticks rund um jeden Spieler (Radius 3 Chunks). Etwa alle
  anderthalb Minuten springen Spieler und Kreaturen in der Nähe auf ihre Position von vor bis zu fünf Sekunden zurück.
- **Organische Welt:** Nur Geländeblöcke (Tag `riftverse:waechst_nach`) wachsen nach 1–2 Minuten nach, die Liste
  übersteht einen Neustart.

## Meilenstein 2 – Spielbarer Prototyp

Ab jetzt lässt sich Riftverse im Überleben von der Garage bis zum ersten Sprung durchspielen.

| Bereich | Inhalt |
|---|---|
| **Meteoritenkrater** | Alle 900–1.100 Blöcke ein Krater in der Overworld (alle Biome außer Flüssen, im Ozean versunken am Meeresgrund): Schüssel mit Auswurfwall, 8–12 sichtbare Xenit-Erze, Meteoritenkern in der Mitte, darunter eine trichterförmige Einschlagsader aus 20–30 Erz |
| **Xenit-Zone** | Xenit-Erz im Eisen-Band (Y −24 bis 56) rund um jeden Krater; Dichte fällt linear von 100 % am Krater auf 0 bei 200 Blöcken |
| **Meteoritenpeiler** | Kompass, dessen Nadel zum nächsten Krater zeigt und in der Xenit-Zone grün wird |
| **Neue Rohstoffe** | Fluxkristall-Erz (Overworld unter Y 0, selten), Alien-Wracks mit Schrotthaufen (Overworld selten), Meteoritenkern |
| **Restliche T1-Maschinen** | Xenit-Schmelze (Xenit-Staub → Barren), Platinen-Ätzer (→ Basis-Schaltkreis), Kristall-Schleifer (Fluxkristall → Fluxlinse + Fluxstaub), Recycler (Alien-Schrott → Polymerfetzen, Drähte, Federn, Mini-Motoren), Fluid-Mixer (Fluxstaub + Wasser → Roh-Portalfluid) |
| **Xenit-Zelle** | Akku mit 1 Mio. FE auf Stufe I; behält ihre Ladung beim Abbauen, lädt und entlädt Items |
| **Portal-Kanone Mk I** | Rein zufällige Sprünge in die drei T1-Universumstypen, 250 mB Roh-Portalfluid pro Schuss aus Fluid-Kartuschen (1.000 mB = 4 Schüsse); Rückkehr zum Heimanker mit Taste R oder Schleichen + Rechtsklick |
| **Roh-Portalfluid** | Zerfällt 10 Minuten nach der Herstellung, im Tank wie in der Kartusche |
| **Universen** | Schrottwelt (Nebel, Müllberge, Wrackfelder mit Xenit, Schrott-Golems), Kristallwelt (immer hell, Fluxkristall-Cluster wachsen nach, Sprinten lässt sie zerspringen), toxische Welt (Gift ohne Helm, Säureseen mit Säuregel, Säureregen frisst Rüstung). Jedes Universum würfelt 1–3 Modifikatoren |
| **Rift-Anker und Aufräumen** | Universen ohne Bauten und ohne Rift-Anker, die 10 Ingame-Tage nicht besucht wurden, werden beim Serverstart zurückgesetzt; der Universums-Scanner zeigt den Zustand |
| **Prototyp** | Quantenerz, alte Rift-Kanone und Quanten-Maschinen sind entfernt. Bosse und Story-Generator bleiben für Phase 3 im Code, starten aber nicht mehr bei der Ankunft |

### So spielt sich Meilenstein 2

1. **Meteoritenpeiler** in der Garagen-Werkbank bauen (Kompass, 4 Kupferplatten, 2 Redstone) und dem
   grünen Zeiger folgen. Am Krater liegt Xenit offen, das meiste steckt im Eisen-Band darunter.
2. **Xenit-Gehäuse** bauen und damit die T1-Maschinen aus Meilenstein 1. Xenit-Erz mit der Erzmühle
   verdoppeln, den Staub in der **Xenit-Schmelze** zu Barren schmelzen.
3. **Alien-Wracks** suchen (rostige Rümpfe mit Schrotthaufen). Alien-Schrott in der Garagenpresse zu
   Schrottplatten pressen, im **Platinen-Ätzer** mit Redstone und Kupferdraht zu **Basis-Schaltkreisen** ätzen.
4. **Fluxkristall-Erz** unter Y 0 abbauen. Der **Kristall-Schleifer** macht daraus eine Fluxlinse und Fluxstaub.
5. Im **Fluid-Mixer** Fluxstaub mit Wasser zu Roh-Portalfluid mischen und **Fluid-Kartuschen** füllen.
   Das Fluid zerfällt nach 10 Minuten – erst füllen, wenn es losgehen soll.
6. **Portal-Kanone Mk I** an der Garagen-Werkbank bauen: Kanonengehäuse, Fluxlinse, 2 Basis-Schaltkreise,
   Fluid-Kartusche. Rechtsklick öffnet ein Rift in ein zufälliges Universum; volle Kartuschen im Inventar
   lädt die Kanone selbst nach. Zurück geht es mit **R** (oder Schleichen + Rechtsklick).
7. Ein **Rift-Anker** (4 Eisenplatten, Xenit-Stange, Fluxlinse) hält ein Universum, das man behalten will.

### Entscheidungen in Meilenstein 2

- **Universen als Raster:** Jeder Universumstyp ist eine Dimension (`riftverse:schrottwelt`, `riftverse:kristallwelt`,
  `riftverse:toxische_welt`), aufgeteilt in Zellen von 4.096 × 4.096 Blöcken mit Leere dazwischen. Jede Zelle ist ein
  Universum mit eigenem Seed, Gelände und Modifikatoren. Eine Zelle umfasst genau 8 × 8 Region-Dateien, deshalb kann
  das Aufräumen ein Universum löschen, ohne die Nachbarn zu berühren.
- **Modifikatoren:** Riesen- oder Mini-Mobs, doppelte Erze, keine Tiere, Dauergewitter (Blitze in der Nähe),
  supraleitend oder hochohmig (Kabelverlust 0 bzw. doppelt) und als Kuriosität hüpfende oder leuchtende Kreaturen.
  „Ewige Nacht“ fehlt noch, weil sich alle Universen eines Typs eine Dimension und damit eine Tageszeit teilen.
- **Atemschutz:** Bis das Atemfilter-Modul (Meilenstein 6) kommt, schützt jeder Helm vor der giftigen Luft.
  Säureregen fällt, wenn es in der Overworld regnet.
- **Säuregel:** Glasflasche an einer Säurequelle füllen. Gebraucht wird es ab dem Rift-Labor (Meilenstein 3).
- **Kristall-Schleifer:** 1 Fluxkristall → 1 Fluxlinse + 2 Fluxstaub, damit ein Kristall zwei Kartuschen füllt.
- **Kanone Mk I:** Das Rezept nimmt eine leere oder volle Kartusche; die Kanone startet leer und lädt volle
  Kartuschen aus dem Inventar nach (die leere kommt zurück). Auch der Rücksprung kostet einen Schuss. Ein
  kostenloses Rückportal bei der Ankunft gibt es nicht mehr. Der Heimanker ist der Ort, von dem man zuletzt aus
  der Heimat-Realität gesprungen ist; ohne Anker geht es zum Weltspawn.
- **Schaltkreise in T1:** Die neuen Maschinen brauchen einen Basis-Schaltkreis, außer dem Platinen-Ätzer, der sie
  erst herstellt. Die gelöteten Rezepte aus Meilenstein 1 bleiben, damit niemand festhängt.
- **Isolierfolie:** Polymerfetzen kommen jetzt aus dem Recycler; der Tonnenreaktor wirft keine mehr aus.
- **Als „gebaut“ zählt:** eine Block-Entity oder mehr als 64 dauerhaft platzierte Blöcke (Fackeln, Gerüste und
  Leitern zählen nicht, Tag `riftverse:zaehlt_nicht_als_bau`). Wer einmal über die Schwelle kommt, bleibt bebaut.
- **Xenit-Zelle:** gibt Energie nur an Verbraucher ab, nicht an andere Speicher; so pumpen zwei Zellen keine Energie
  im Kreis.

## Meilenstein 1 – Fundament

| Bereich | Inhalt |
|---|---|
| **Metalle** | Zinnerz (Oberwelt, per Config abschaltbar), Rohes Zinn, Zinnbarren und -block; Staub, Platten, Drähte, Hülsen, Stangen; Bronze, Stahl, Mischmetall; Xenit-Erz, -Barren, -Platte, -Staub, -Stange |
| **T0 Garage** | Garagen-Werkbank mit Werkzeugslots für Hammer, Lötkolben und Lötzinn; Garagenpresse und Kurbel-Dynamo mit der Handkurbel |
| **T1 Maschinen** | Erzmühle (1 Erz → 2 Staub), Walzbank (Walzen, Schneiden, Ziehen), Legierungsofen (Bronze, Stahl), Tonnenreaktor (verbrennt jedes Item) |
| **Energienetz Stufe I** | Eigenes Riftverse-Netz in FE mit IC2-Regeln: Pakete mit Spannungsstufe, Kabelverlust 0,5 FE pro Block, Überspannung (explodieren, durchbrennen oder aus), blanke Kabel geben einen Schlag |
| **Kabel** | Kupferkabel blank (4 px) und isoliert (6 px), isolierte Kabel mit Farbstoff einfärbbar; unterschiedlich gefärbte Kabel verbinden sich nicht |
| **Kompatibilität** | Einspeiser (FE von jedem Mod rein) und Ausspeiser (normales FE raus); Maschinen zeigen ihren Füllstand nur lesbar an |
| **Akku** | Energiezelle (40.000 FE) als reines FE-Item; jede Maschine und jeder Generator hat einen Akku-Slot |
| **Test** | Kreativ-Quelle (nur Kreativmodus): unendliche Quelle, Rechtsklick wechselt Stufe I–IV, um Überspannung zu testen |

### So spielt sich Meilenstein 1

1. **Garagen-Werkbank** in der normalen Werkbank bauen (Eisen, Kupfer, Holzstämme, Werkbank).
2. In der Garagen-Werkbank **Bronzestaub** von Hand mischen (3 Kupfer + 1 Zinn → 2), im Ofen zu Bronze schmelzen.
3. **Garagen-Hammer** bauen und in den Werkzeugslot legen: 2 Barren → 1 Platte.
4. **Garagenpresse**, **Handkurbel** und **Kurbel-Dynamo** bauen. Mit der Handkurbel Rechtsklick gedrückt
   halten: Die Presse macht 1 Barren → 1 Platte, der Dynamo liefert 8 FE/t.
5. **Lötkolben** und **Lötzinn** (Zinnbarren + Hammer) in die Werkzeugslots legen – die T1-Maschinen werden gelötet.
6. T1-Maschinen brauchen das **Xenit-Gehäuse** aus Xenit vom Meteoritenkrater (siehe Meilenstein 2).

Rechtsklick mit leerer Hand auf ein Kabel zeigt das Teilnetz: Quellen, Verbraucher, Leitungen und übertragene FE/t.

### Entscheidungen in Meilenstein 1

Wo das Design-Dokument nichts festlegt oder sich widerspricht, gilt bis auf Weiteres:

- **Bronze in T0:** Der Legierungsofen ist T1, Bronze wird aber schon in T0 gebraucht. Deshalb gibt es eine
  Handmischung in der Garagen-Werkbank mit halber Ausbeute (3 Kupfer + 1 Zinn → 2 Bronzestaub);
  der Legierungsofen macht 3 + 1 → 4.
- **Akku-Slot:** Maschinen ziehen Strom aus der Energiezelle, Generatoren laden sie.
- **Lötzinn:** Zinnbarren + Hammer → 1 Lötzinn, in der Walzbank Zinnplatte → 3 Lötzinn.
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
| `universen.aufraeumen` | `true` | Unbebaute, lange nicht besuchte Universen beim Serverstart zurücksetzen |
| `universen.tage` | `10` | Ingame-Tage ohne Besuch bis zum Zurücksetzen |
| `universen.bloecke` | `64` | Ab mehr als so vielen platzierten Blöcken gilt ein Universum als bebaut |
| `story.boss_bei_ankunft` | `false` | Prototyp: Boss kurz nach der Ankunft starten (bis zum Story-Modus aus) |

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
  energy/       Riftverse-Netz: Graph, Pakete, Spannungsstufen, Überspannung, Ein-/Ausspeiser, Speicher
  cable/        Kupferkabel blank und isoliert
  tech/         Maschinen, Generatoren, Fluid-Mixer, Xenit-Zelle, Garagen-Werkbank (+ menu/ für die GUIs)
  recipe/       Rezepttypen der Maschinen, des Fluid-Mixers und der Garagen-Werkbank
  universe/     Universumstypen, Modifikatoren, Gelände, Physik und Gefahren, Aufräumen
  worldgen/     Meteoritenkrater, Xenit-Zone, Alien-Wracks, per Config abschaltbare Erze
  item/         Portal-Kanone, Fluid-Kartusche, Meteoritenpeiler, Universums-Scanner
  block/        Meteoritenkern, Fluxkristalle, Säure, Rift-Anker
  fluid/        Zerfall von Roh-Portalfluid, Säuregel aus der Glasflasche
  portal/       Rift-Portal, Reise, Heimanker
  network/      Rückkehr-Taste
  entity/       Schrott-Golem
  client/       Screens (ohne Texturdateien gezeichnet), Kabelfarben, Peiler-Nadel, Nebel, Tasten
  registry/     Registrierung aller Blöcke, Items, Fluids, Rezepttypen …
  boss/, story/ Bosse und Story-Generator aus dem ersten Entwurf (ruhen bis Phase 3)
src/main/resources/
  assets/riftverse/   Texturen, Modelle, Blockstates, Sprachen
  data/riftverse/     Rezepte (pro Maschine ein Ordner), Loot, Worldgen, Dimensionen, Biome, Tags
tools/
  texturen/   Texturgenerator (Python, Pillow + NumPy); meilenstein1.py und meilenstein2.py ergänzen die neuen Texturen
  daten/      generate.py erzeugt Modelle, Blockstates, Loot, Tags, Rezepte, Worldgen, Dimensionen und Sprachen
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

Maschinen mit mehreren Ausgängen lesen `byproducts`, der Fluid-Mixer hat eigene Fluid-Felder:

```json
{
  "type": "riftverse:kristall_schleifer",
  "ingredients": [{"ingredient": {"tag": "c:gems/fluxkristall"}}],
  "result": {"id": "riftverse:fluxlinse"},
  "byproducts": [{"result": {"id": "riftverse:fluxstaub", "count": 2}, "chance": 1.0}],
  "time": 160
}
```

```json
{
  "type": "riftverse:fluid_mixer",
  "ingredients": [{"ingredient": {"tag": "c:dusts/fluxkristall"}}],
  "fluid": {"fluid": "minecraft:water", "amount": 1000},
  "result": {"id": "riftverse:roh_portalfluid", "amount": 1000},
  "time": 200
}
```
## Made By David Chapman

## Lizenz

Dieses Projekt ist unter der [CC BY-NC-SA 4.0](https://creativecommons.org/licenses/by-nc-sa/4.0/) lizenziert.

Sie sind frei und berechtigt:
- Teilen: Das Material in jedwedem Format oder Medium zu vervielfältigen und weiterzuverbreiten.
- Bearbeiten: Das Material zu remixen, zu verändern und darauf aufzubauen.

Unter folgenden Bedingungen:
1. Namensnennung (Attribution): Sie müssen angemessene Urheber- und Rechteangaben machen, einen Link zur Lizenz beifügen und angeben, ob Änderungen vorgenommen wurden.
2. Nicht-kommerziell (Non-Commercial): Sie dürfen das Material nicht für kommerzielle Zwecke nutzen.
3. Weitergabe unter gleichen Bedingungen (ShareAlike): Wenn Sie das Material remixen, verändern oder anderweitig direkt darauf aufbauen, müssen Sie Ihre Beiträge unter derselben Lizenz wie das Original veröffentlichen.
