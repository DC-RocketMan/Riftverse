---
navigation:
  parent: energie/index.md
  title: Singularitätskern
  icon: riftverse:singularitaetskern
  position: 70
item_ids:
  - riftverse:singularitaetskern
  - riftverse:eindaemmungsring
---

# Singularitätskern

<BlockImage id="singularitaetskern" scale="2" float="right" />

Ein schwarzes Loch im Keller. **100.000 FE/t auf Stufe IV.** Dafür frisst es **64 beliebige Items pro Sekunde** –
am besten über Rohre. Zu wenig Futter heißt anteilig weniger Strom.

<br clear="all" />

## Wenn er hungert

Nach 5 Sekunden ohne Futter wird er unruhig und wächst alle 10 Sekunden eine Stufe (bis 6):

- Er saugt Gegenstände, Mobs und Spieler an und verletzt alles direkt am Kern.
- Er frisst Blöcke im Umkreis von 1 + Stufe – keine Maschinen, keine Kisten, nichts Unzerstörbares.
- Gefressenes zählt als Futter.

Die **Notabschaltung** (Knopf in der GUI oder Redstone-Signal) hält ihn sofort an und setzt das Wachstum zurück;
danach kühlt er eine Minute ab.

## Eindämmungsring

Wer sich nicht traut, baut den <ItemLink id="eindaemmungsring" /> drumherum: drei Ringe wie bei einem
Kreiselkompass, jeder der Rand eines 5 × 5-Quadrats durch den Kern (42 Blöcke). Steht der Ring, schaltet der Kern
nach 5 Sekunden Hunger sicher ab, statt zu wachsen.

<GameScene zoom="3" interactive={true}>
  <Block id="riftverse:singularitaetskern" x="0" y="0" z="0" p:facing="north" />
  <Block id="riftverse:eindaemmungsring" x="-2" y="-2" z="0" />
  <Block id="riftverse:eindaemmungsring" x="-2" y="-1" z="0" />
  <Block id="riftverse:eindaemmungsring" x="-2" y="0" z="-2" />
  <Block id="riftverse:eindaemmungsring" x="-2" y="0" z="-1" />
  <Block id="riftverse:eindaemmungsring" x="-2" y="0" z="0" />
  <Block id="riftverse:eindaemmungsring" x="-2" y="0" z="1" />
  <Block id="riftverse:eindaemmungsring" x="-2" y="0" z="2" />
  <Block id="riftverse:eindaemmungsring" x="-2" y="1" z="0" />
  <Block id="riftverse:eindaemmungsring" x="-2" y="2" z="0" />
  <Block id="riftverse:eindaemmungsring" x="-1" y="-2" z="0" />
  <Block id="riftverse:eindaemmungsring" x="-1" y="0" z="-2" />
  <Block id="riftverse:eindaemmungsring" x="-1" y="0" z="2" />
  <Block id="riftverse:eindaemmungsring" x="-1" y="2" z="0" />
  <Block id="riftverse:eindaemmungsring" x="0" y="-2" z="-2" />
  <Block id="riftverse:eindaemmungsring" x="0" y="-2" z="-1" />
  <Block id="riftverse:eindaemmungsring" x="0" y="-2" z="0" />
  <Block id="riftverse:eindaemmungsring" x="0" y="-2" z="1" />
  <Block id="riftverse:eindaemmungsring" x="0" y="-2" z="2" />
  <Block id="riftverse:eindaemmungsring" x="0" y="-1" z="-2" />
  <Block id="riftverse:eindaemmungsring" x="0" y="-1" z="2" />
  <Block id="riftverse:eindaemmungsring" x="0" y="0" z="-2" />
  <Block id="riftverse:eindaemmungsring" x="0" y="0" z="2" />
  <Block id="riftverse:eindaemmungsring" x="0" y="1" z="-2" />
  <Block id="riftverse:eindaemmungsring" x="0" y="1" z="2" />
  <Block id="riftverse:eindaemmungsring" x="0" y="2" z="-2" />
  <Block id="riftverse:eindaemmungsring" x="0" y="2" z="-1" />
  <Block id="riftverse:eindaemmungsring" x="0" y="2" z="0" />
  <Block id="riftverse:eindaemmungsring" x="0" y="2" z="1" />
  <Block id="riftverse:eindaemmungsring" x="0" y="2" z="2" />
  <Block id="riftverse:eindaemmungsring" x="1" y="-2" z="0" />
  <Block id="riftverse:eindaemmungsring" x="1" y="0" z="-2" />
  <Block id="riftverse:eindaemmungsring" x="1" y="0" z="2" />
  <Block id="riftverse:eindaemmungsring" x="1" y="2" z="0" />
  <Block id="riftverse:eindaemmungsring" x="2" y="-2" z="0" />
  <Block id="riftverse:eindaemmungsring" x="2" y="-1" z="0" />
  <Block id="riftverse:eindaemmungsring" x="2" y="0" z="-2" />
  <Block id="riftverse:eindaemmungsring" x="2" y="0" z="-1" />
  <Block id="riftverse:eindaemmungsring" x="2" y="0" z="0" />
  <Block id="riftverse:eindaemmungsring" x="2" y="0" z="1" />
  <Block id="riftverse:eindaemmungsring" x="2" y="0" z="2" />
  <Block id="riftverse:eindaemmungsring" x="2" y="1" z="0" />
  <Block id="riftverse:eindaemmungsring" x="2" y="2" z="0" />
</GameScene>

<Row>
  <RecipeFor id="singularitaetskern" />
  <RecipeFor id="eindaemmungsring" />
</Row>
