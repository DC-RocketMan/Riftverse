---
navigation:
  parent: rohstoffe/index.md
  title: Staub, Platten, Draht
  icon: riftverse:kupferdraht
  position: 10
item_ids:
  - riftverse:eisen_staub
  - riftverse:kupfer_staub
  - riftverse:gold_staub
  - riftverse:zinn_staub
  - riftverse:eisen_platte
  - riftverse:kupfer_platte
  - riftverse:zinn_platte
  - riftverse:gold_platte
  - riftverse:bronze_platte
  - riftverse:stahl_platte
  - riftverse:kupferdraht
  - riftverse:golddraht
  - riftverse:zinn_huelse
  - riftverse:eisen_stange
  - riftverse:bronze_zahnrad
---

# Staub, Platten, Draht

Aus Barren werden in Riftverse erst Zwischenprodukte: Platten, Drähte, Hülsen, Stangen. Jedes Rezept fragt nur nach
der Form („eine Kupferplatte“), nicht nach der Maschine – Platten aus Create oder Mekanism passen also auch.

<ItemGrid>
  <ItemIcon id="eisen_platte" />
  <ItemIcon id="kupfer_platte" />
  <ItemIcon id="zinn_platte" />
  <ItemIcon id="gold_platte" />
  <ItemIcon id="bronze_platte" />
  <ItemIcon id="stahl_platte" />
  <ItemIcon id="kupferdraht" />
  <ItemIcon id="golddraht" />
  <ItemIcon id="zinn_huelse" />
  <ItemIcon id="eisen_stange" />
  <ItemIcon id="bronze_zahnrad" />
</ItemGrid>

## Staub: Erz verdoppeln

Die <ItemLink id="erzmuehle" /> macht aus 1 Erz 2 Staub. Im Ofen wird daraus wieder ein Barren – du bekommst also
doppelt so viel Metall wie beim direkten Schmelzen.

<Row>
  <RecipesFor id="eisen_staub" />
  <RecipesFor id="kupfer_staub" />
  <RecipesFor id="gold_staub" />
  <RecipesFor id="zinn_staub" />
</Row>

## Platten

Drei Wege, du wählst: Hammer in der Werkbank (2 Barren → 1 Platte), die Garagenpresse (1 → 1, kurbeln) oder eine Presse
aus einem anderen Mod.

<RecipesFor id="eisen_platte" />
<RecipesFor id="kupfer_platte" />
<RecipesFor id="zinn_platte" />
<RecipesFor id="gold_platte" />
<RecipesFor id="bronze_platte" />
<RecipesFor id="stahl_platte" />

## Walzbank: Draht, Hülsen, Stangen

Die <ItemLink id="walzbank" /> hat drei Modi: **Walzen** (Platte → 3 Draht), **Schneiden** (Platte → 2 Hülsen) und
**Ziehen** (Barren → 2 Stangen). Kupferdraht fällt außerdem manchmal im Recycler aus Alien-Schrott ab.

<Row>
  <RecipesFor id="kupferdraht" />
  <RecipeFor id="golddraht" />
  <RecipeFor id="zinn_huelse" />
  <RecipeFor id="eisen_stange" />
</Row>

## Zahnräder

<RecipeFor id="bronze_zahnrad" />
