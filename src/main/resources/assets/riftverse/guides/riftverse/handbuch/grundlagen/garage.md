---
navigation:
  parent: grundlagen/index.md
  title: Garagen-Werkbank
  icon: riftverse:garagen_werkbank
  position: 0
item_ids:
  - riftverse:garagen_werkbank
  - riftverse:garagen_hammer
  - riftverse:loetkolben
  - riftverse:loetzinn
---

# Garagen-Werkbank

<BlockImage id="garagen_werkbank" scale="2" float="left" />

Holzwerkbank, Schraubstock, Kaffeefleck: Hier entsteht fast jedes Riftverse-Teil. Sie funktioniert wie eine normale
Werkbank mit 3 × 3-Raster, hat aber drei Werkzeugslots daneben:

- **Hammer:** Manche Rezepte schlagen etwas zurecht und kosten den <ItemLink id="garagen_hammer" /> Haltbarkeit.
- **Lötkolben und Lötzinn:** Elektronik wird gelötet. Der <ItemLink id="loetkolben" /> verliert dabei 1 Haltbarkeit,
  das <ItemLink id="loetzinn" /> wird verbraucht.

Was ein Rezept braucht, steht unter dem Rezept („Hammer: 1 Haltbarkeit“, „Lötzinn: 2“). Das Raster leert sich beim
Schließen ins Inventar.

<br clear="all" />

<RecipeFor id="garagen_werkbank" />

## Werkzeuge

Der Hammer macht nebenbei auch Platten aus Barren (2 → 1), solange du noch keine Presse hast.

<Row>
  <RecipeFor id="garagen_hammer" />
  <RecipeFor id="loetkolben" />
</Row>

Lötzinn gibt es von Hand aus einem Zinnbarren oder günstiger aus der Walzbank:

<RecipesFor id="loetzinn" />

> **Garagen-Notiz:** Die ersten Maschinen brauchen nur den Hammer. Gelötet wird erst ab der Bastlerwerkstatt.
