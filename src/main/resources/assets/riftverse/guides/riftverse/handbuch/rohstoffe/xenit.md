---
navigation:
  parent: rohstoffe/index.md
  title: Xenit
  icon: riftverse:xenit_ingot
  position: 20
item_ids:
  - riftverse:xenit_ore
  - riftverse:deepslate_xenit_ore
  - riftverse:raw_xenit
  - riftverse:xenit_ingot
  - riftverse:xenit_block
  - riftverse:xenit_staub
  - riftverse:xenit_platte
  - riftverse:xenit_stange
  - riftverse:meteoritenkern
  - riftverse:meteoritenpeiler
---

# Xenit

<ItemImage id="xenit_ingot" scale="2" float="right" />

Xenit ist ein Alien-Metall, das mit Meteoriten auf die Erde gekommen ist. Es steckt im Kern fast jeder Maschine ab der
Bastlerwerkstatt: Spulenkerne, Schaltkreis-Träger, Gehäusepanzerung, Kanone. Die Garage kommt noch ganz ohne aus.

<br clear="all" />

## Wo es liegt

Alle 900–1.100 Blöcke ist ein **Meteoritenkrater** eingeschlagen (in jedem Biom außer Flüssen; im Ozean liegt er
versunken am Meeresgrund).

- **Am Krater** liegen 8–12 Xenit-Erze offen, in der Mitte der <ItemLink id="meteoritenkern" />.
- **Darunter** steckt eine trichterförmige Einschlagsader mit 20–30 Erz.
- **Rundherum** liegt das meiste Xenit: im Eisen-Band (Y −24 bis 56, am dichtesten um Y 16) bis 200 Blöcke um den
  Krater, nach außen immer seltener. Nahe am Krater bringt eine Stunde Strip-Mining rund 20–40 Erz.
- **Später:** Die Wrackfelder der Schrottwelt sind voll davon, und der Universums-Destillator holt es aus einer Probe.

<GameScene zoom="3" interactive={true}>
  <Block id="riftverse:meteoritenkern" x="1" y="0" z="1" />
  <Block id="riftverse:xenit_ore" x="0" y="0" z="1" />
  <Block id="riftverse:xenit_ore" x="2" y="0" z="0" />
  <Block id="riftverse:deepslate_xenit_ore" x="1" y="-1" z="1" />
  <Block id="minecraft:stone" x="0" y="-1" z="0" />
  <Block id="minecraft:stone" x="2" y="-1" z="2" />
  <BlockAnnotation x="1" y="0" z="1">Meteoritenkern: in der Erzmühle gibt er 6 Xenit-Staub</BlockAnnotation>
</GameScene>

## Der Meteoritenpeiler

<ItemImage id="meteoritenpeiler" float="left" />

Ein Kompass, dessen Nadel zum nächsten Krater zeigt. In der Xenit-Zone (200 Blöcke) wird sie grün – dann lohnt es
sich, im Eisen-Band zu graben. Funktioniert nur in der Oberwelt. Von einem beliebigen Punkt ist der nächste Krater
im Schnitt rund 380 Blöcke weit weg.

<br clear="all" />

<RecipeFor id="meteoritenpeiler" />

## Verarbeiten

Zwei Wege, du wählst:

- **Ofen:** 1 Erz → 1 Barren. Geht sofort.
- **Erzmühle und Xenit-Schmelze:** 1 Erz → 2 Xenit-Staub → 2 Barren. Doppelt so viel, aber Xenit-Staub schmilzt nur
  in der Xenit-Schmelze, nicht im Ofen.

Platten und Stangen entstehen wie bei den anderen Metallen.

<RecipesFor id="xenit_ingot" />

<Row>
  <RecipesFor id="xenit_staub" />
  <RecipesFor id="xenit_platte" />
  <RecipeFor id="xenit_stange" />
  <RecipeFor id="xenit_block" />
</Row>
