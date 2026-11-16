---
navigation:
  parent: maschinen/index.md
  title: Bastlerwerkstatt (T1)
  icon: riftverse:walzbank
  position: 10
item_ids:
  - riftverse:erzmuehle
  - riftverse:walzbank
  - riftverse:legierungsofen
  - riftverse:xenit_schmelze
  - riftverse:platinen_aetzer
  - riftverse:kristall_schleifer
  - riftverse:recycler
  - riftverse:fluid_mixer
---

# Bastlerwerkstatt (T1)

Lackiertes Xenit-Petrol mit Nieten: Alle T1-Maschinen stecken in einem Xenit-Gehäuse, laufen auf Stufe I und werden in
der Garagen-Werkbank gelötet. Die meisten brauchen einen Basis-Schaltkreis – bis auf den Platinen-Ätzer, der sie
erst herstellt. In welcher Reihenfolge du sie baust, hängt davon ab, was dir gerade fehlt.

<Row>
  <BlockImage id="erzmuehle" />
  <BlockImage id="walzbank" />
  <BlockImage id="legierungsofen" />
  <BlockImage id="xenit_schmelze" />
</Row>
<Row>
  <BlockImage id="platinen_aetzer" />
  <BlockImage id="kristall_schleifer" />
  <BlockImage id="recycler" />
  <BlockImage id="fluid_mixer" />
</Row>

## Erzmühle

1 Erz → 2 Staub für Eisen, Kupfer, Gold, Zinn und Xenit. Mahlt außerdem Kohle zu Kohlestaub, den Meteoritenkern zu
6 Xenit-Staub und einige Rohstoffe fremder Welten. **30 FE/t.**

<RecipeFor id="erzmuehle" />

## Walzbank

Drei Modi, umschaltbar in der GUI: **Walzen** (Platte → 3 Draht), **Schneiden** (Platte → 2 Hülsen), **Ziehen**
(Barren → 2 Stangen). **30 FE/t.**

<RecipeFor id="walzbank" />

## Legierungsofen

Zwei Eingänge: 3 Kupfer + 1 Zinn → 4 Bronze, Eisen + 2 Kohlestaub → Stahl. Geht mit Barren oder Staub. **40 FE/t.**

<RecipeFor id="legierungsofen" />

## Xenit-Schmelze

Schmilzt Xenit-Staub (nur hier!) und rohes Xenit zu Barren, später Chronoglas-Sand zu Scheiben. **40 FE/t.**

<RecipeFor id="xenit_schmelze" />

## Platinen-Ätzer

Schrottplatte + Redstone + 3 Kupferdraht → Basis-Schaltkreis. **30 FE/t.**

<RecipeFor id="platinen_aetzer" />

## Kristall-Schleifer

Fluxkristall → Fluxlinse + 2 Fluxstaub. **50 FE/t.**

<RecipeFor id="kristall_schleifer" />

## Recycler

Alien-Schrott → Polymerfetzen, dazu mit etwas Glück Kupferdraht, Federn und Mini-Motoren. **20 FE/t.**

<RecipeFor id="recycler" />

## Fluid-Mixer

Fluxstaub + 1.000 mB Wasser → 1.000 mB Roh-Portalfluid. Füllt Fluid-Kartuschen; Rohre können Wasser hinein- und
Portalfluid herauspumpen. **40 FE/t.** Roh-Portalfluid zerfällt nach 10 Minuten – erst mischen, wenn es losgeht.

<Recipe id="fluid_mixer/roh_portalfluid" />

<RecipeFor id="fluid_mixer" />
