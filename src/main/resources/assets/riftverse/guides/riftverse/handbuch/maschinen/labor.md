---
navigation:
  parent: maschinen/index.md
  title: Rift-Labor (T2)
  icon: riftverse:praezisions_montierer
  position: 20
item_ids:
  - riftverse:praezisions_montierer
  - riftverse:quantenzentrifuge
  - riftverse:gravitationspresse
  - riftverse:dimensions_scanner
  - riftverse:chrono_beschleuniger
  - riftverse:portalfluid_raffinerie
---

# Rift-Labor (T2)

Labor-Weiß mit abgeschrägten Kanten und grünen Sichtfenstern. Die T2-Geräte stecken in Labor-Gehäusen und laufen auf
**Stufe II** – an einem Stufe-I-Netz brauchen sie einen [Umspanner](../grundlagen/umspanner.md), sonst bekommen sie
nichts; umgekehrt fliegen T1-Maschinen an einem Stufe-II-Netz in die Luft.

Präzisions-Montierer und Quantenzentrifuge kommen noch mit Basis-Schaltkreisen aus. Damit lassen sich die
Rift-Schaltkreise für den Rest bauen.

## Präzisions-Montierer

<BlockImage id="praezisions_montierer" float="right" />

Ein Block mit Roboterarm, der Teile **Schritt für Schritt** zusammensetzt. Die Zutaten gehören in feste Slots, oben
links beginnend – die Reihenfolge zählt. Die GUI zeigt den aktuellen Schritt. Verbraucht wird erst am Ende; eine
abgebrochene Montage verliert nichts. **200 FE/t.**

Baut Rift-Schaltkreise, Kanonenkern, Kanone Mk II und III, die Rüstungsrohlinge und viele Gadgets.

<br clear="all" />

<RecipeFor id="praezisions_montierer" />

## Quantenzentrifuge

<BlockImage id="quantenzentrifuge" float="right" />

Eine weiße Trommel mit Bullauge für die Chemie: Säuregel → Ätzlösung, Biomasse-Pulpe → Nährserum, Fluxkristall +
Säuregel → Flux-Stabilisator. Fast immer fallen Nebenprodukte ab. **300 FE/t.**

<br clear="all" />

<RecipeFor id="quantenzentrifuge" />

## Gravitationspresse

<BlockImage id="gravitationspresse" float="right" />

Zwei Platten, die sich ohne Berührung zusammenpressen. Schwebstein-Brocken → Antigrav-Platte, später Gravitit →
Gravitit-Rahmen und Kohleblock → Graphitplatten. **350 FE/t.**

<br clear="all" />

<RecipeFor id="gravitationspresse" />

## Dimensions-Scanner

<BlockImage id="dimensions_scanner" float="right" />

Ein Tischgerät mit Hologramm-Globus. Eine gefüllte Universumsprobe und Papier ergeben eine **Koordinatenkarte** mit
Typ und Modifikatoren des Universums; die Probe wird dabei verbraucht. **150 FE/t.**

<br clear="all" />

<Row>
  <Recipe id="dimensions_scanner/koordinatenkarte" />
  <RecipeFor id="dimensions_scanner" />
</Row>

## Chrono-Beschleuniger

<BlockImage id="chrono_beschleuniger" float="right" />

Ein Würfel mit Uhrwerk-Front. Die **Rückseite** zeigt zur Maschine, Rechtsklick wechselt die Stufe: **2×, 4× oder 8×
so schnell** für 100, 400 oder 800 FE/t. Pflanzen wachsen auch schneller; Generatoren lassen sich nicht beschleunigen.
Er hat keine GUI und deshalb keinen Akku-Slot.

<br clear="all" />

<RecipeFor id="chrono_beschleuniger" />

## Portalfluid-Raffinerie

Ein Labor-Turm mit grünen Glasrohren, **3 × 3 × 4**. Aus Roh-Portalfluid wird hier

- **stabiles Portalfluid** (mit Säuregel und Fluxstaub) – zerfällt nicht mehr, für Kanone Mk I und II,
- **Portalfluid-Konzentrat** (mit Chronoglas-Scheibe und Flux-Stabilisator) – für Kanone Mk II und III.

Füttern mit Kartuschen oder per Rohr direkt aus dem Fluid-Mixer. **250 FE/t, Stufe II.**

Boden und Deckel ganz aus Labor-Hülle; in den zwei Lagen dazwischen Labor-Hülle an den Ecken, Glas an den Seiten, die
Mitte bleibt frei. Die Raffinerie lässt sich komplett aus T1-Material bauen.

<GameScene zoom="3" interactive={true}>
  <Block id="riftverse:portalfluid_raffinerie" x="0" y="0" z="0" p:facing="north" />
  <Block id="riftverse:labor_huelle" x="-1" y="0" z="0" />
  <Block id="riftverse:labor_huelle" x="1" y="0" z="0" />
  <Block id="riftverse:labor_huelle" x="-1" y="0" z="1" />
  <Block id="riftverse:labor_huelle" x="0" y="0" z="1" />
  <Block id="riftverse:labor_huelle" x="1" y="0" z="1" />
  <Block id="riftverse:labor_huelle" x="-1" y="0" z="2" />
  <Block id="riftverse:labor_huelle" x="0" y="0" z="2" />
  <Block id="riftverse:labor_huelle" x="1" y="0" z="2" />
  <Block id="riftverse:labor_huelle" x="-1" y="1" z="0" />
  <Block id="minecraft:glass" x="0" y="1" z="0" />
  <Block id="riftverse:labor_huelle" x="1" y="1" z="0" />
  <Block id="minecraft:glass" x="-1" y="1" z="1" />
  <Block id="minecraft:glass" x="1" y="1" z="1" />
  <Block id="riftverse:labor_huelle" x="-1" y="1" z="2" />
  <Block id="minecraft:glass" x="0" y="1" z="2" />
  <Block id="riftverse:labor_huelle" x="1" y="1" z="2" />
  <Block id="riftverse:labor_huelle" x="-1" y="2" z="0" />
  <Block id="minecraft:glass" x="0" y="2" z="0" />
  <Block id="riftverse:labor_huelle" x="1" y="2" z="0" />
  <Block id="minecraft:glass" x="-1" y="2" z="1" />
  <Block id="minecraft:glass" x="1" y="2" z="1" />
  <Block id="riftverse:labor_huelle" x="-1" y="2" z="2" />
  <Block id="minecraft:glass" x="0" y="2" z="2" />
  <Block id="riftverse:labor_huelle" x="1" y="2" z="2" />
  <Block id="riftverse:labor_huelle" x="-1" y="3" z="0" />
  <Block id="riftverse:labor_huelle" x="0" y="3" z="0" />
  <Block id="riftverse:labor_huelle" x="1" y="3" z="0" />
  <Block id="riftverse:labor_huelle" x="-1" y="3" z="1" />
  <Block id="riftverse:labor_huelle" x="0" y="3" z="1" />
  <Block id="riftverse:labor_huelle" x="1" y="3" z="1" />
  <Block id="riftverse:labor_huelle" x="-1" y="3" z="2" />
  <Block id="riftverse:labor_huelle" x="0" y="3" z="2" />
  <Block id="riftverse:labor_huelle" x="1" y="3" z="2" />
  <BlockAnnotation x="0" y="0" z="0">Steuerblock</BlockAnnotation>
</GameScene>

<Row>
  <Recipe id="raffinerie/portalfluid" />
  <Recipe id="raffinerie/portalfluid_konzentrat" />
</Row>

<RecipeFor id="portalfluid_raffinerie" />
