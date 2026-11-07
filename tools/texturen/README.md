# Riftverse – Platzhalter-Texturen

Vorübergehende 16×16-Texturen für alle Blöcke, Items, Gadgets und die Rüstung aus dem Riftverse-Design-Dokument. Farben und Optik folgen dem Design-Leitfaden (Portal-Grün, Xenit-Petrol, Labor-Weiß, Graphit, Null-Schwarz).

## Einbau

Den Ordner `assets/` direkt nach `src/main/resources/` deines NeoForge-1.21.1-Projekts kopieren. Namespace ist `riftverse`.

```
assets/riftverse/
├── textures/block/          Erze, Materialblöcke, Gehäuse T0–T4, Maschinenfronten, Generatoren, Fluids
├── textures/item/           Materialien, Bauteile, Gadgets, Rüstungs-Icons, Module
├── textures/models/armor/   multiversal_<stufe>_layer_1.png / _layer_2.png (64×32)
├── models/block, models/item
├── blockstates/
└── lang/de_de.json, en_us.json
```

## Was du wissen solltest

- **Animiert** (vertikaler Frame-Streifen + `.mcmeta`): Fluxkristall-Block, Chronoglas, Nullmaterie-Block und -Item, alle vier Portalfluids, Gehäuse T4, Rift-Schmiede, Weltenformer, Singularitätskern, Universums-Kern, Story-Fragment, Portal-Kanone Mk IV.
- **Fluids**: `<name>_still` (16×16) und `<name>_flow` (32×32) für Roh-Portalfluid, Portalfluid, Konzentrat und Weltenformer-Essenz. Die Texturen sind schon farbig, also im `IClientFluidTypeExtensions` den Tint auf `0xFFFFFFFF` lassen.
- **Maschinen**: jede Maschine hat `<id>_front` und `<id>_front_on`. Seiten und Deckel nutzen die Gehäuse `casing_t<n>` und `casing_t<n>_top` ihres Tiers.
- **Rüstung**: drei Ausbaustufen (`rohling`, `kalibriert`, `perfektioniert`) mit je vier Item-Icons und zwei Layern. In 1.21.1 zeigt `new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath("riftverse", "multiversal_perfektioniert"))` automatisch auf die beiden Layer-Dateien.
- **Modelle und Blockstates** sind Starthilfe mit diesen Annahmen:
  - Maschinen und Generatoren mit Front: Properties `facing` (north/east/south/west) und `active` (true/false), Modell-Parent `minecraft:block/orientable`.
  - Rift-Schmiede, Weltenformer, Singularitätskern: nur `facing`.
  - Tonnenreaktor, Mikrokosmos-Zelle: nur `active`.
  - Klon-Tank: zwei Blöcke hoch, Property `half` (lower/upper), translucent.
  - Chronoglas: translucent.
  Wenn deine Properties anders heißen, nur die Blockstate-Dateien anpassen.
- **Sprachdateien**: `en_us.json` enthält vorerst die deutschen Namen, damit nirgends rohe Schlüssel stehen.

## Neu generieren

Alles ist prozedural und deterministisch erzeugt. Im Ordner `generator/` liegt das Python-Skript (Pillow + NumPy):

```
python3 generator/main.py <zielordner>
```

Farben änderst du zentral in `generator/lib.py` (`_RAMPS`), einzelne Texturen in `materials.py`, `machines.py`, `gadgets.py` und `armor.py`.
