---
navigation:
  parent: energie/index.md
  title: Parallelzapfer
  icon: riftverse:parallelzapfer
  position: 60
item_ids:
  - riftverse:parallelzapfer
---

# Parallelzapfer

Ein Antennengestell über einem kleinen grünen Riss, das ein energiereiches Paralleluniversum anzapft.
**10.000 FE/t – auf Stufe IV.** Ohne Umspanner III ↔ IV oder Null-Leitung grillt er alles, was du anschließt.

- Verbraucht **5 mB Portalfluid pro Tick** (stabiles Fluid oder Konzentrat; Konzentrat zählt doppelt).
- Jede Quelle hält **3–5 Stunden**. Dann muss er neu ausgerichtet werden (eine Minute).
- Manchmal bemerkt die andere Seite den Diebstahl: Wächter kommen durch den Riss, und der Zapfer setzt 30 Sekunden aus.

## Aufbau (3 × 3 × 3)

Boden aus Graphit-Hülle mit dem Steuerblock vorne in der Mitte, darüber vier Eckpfosten aus Graphit-Hülle und darauf
vier Blitzableiter. Über dem Steuerblock in der Mitte bleibt Luft – dort öffnet sich der Riss.

<GameScene zoom="4" interactive={true}>
  <Block id="riftverse:parallelzapfer" x="0" y="0" z="0" p:facing="north" />
  <Block id="riftverse:graphit_huelle" x="-1" y="0" z="0" />
  <Block id="riftverse:graphit_huelle" x="1" y="0" z="0" />
  <Block id="riftverse:graphit_huelle" x="-1" y="0" z="1" />
  <Block id="riftverse:graphit_huelle" x="0" y="0" z="1" />
  <Block id="riftverse:graphit_huelle" x="1" y="0" z="1" />
  <Block id="riftverse:graphit_huelle" x="-1" y="0" z="2" />
  <Block id="riftverse:graphit_huelle" x="0" y="0" z="2" />
  <Block id="riftverse:graphit_huelle" x="1" y="0" z="2" />
  <Block id="riftverse:graphit_huelle" x="-1" y="1" z="0" />
  <Block id="riftverse:graphit_huelle" x="1" y="1" z="0" />
  <Block id="riftverse:graphit_huelle" x="-1" y="1" z="2" />
  <Block id="riftverse:graphit_huelle" x="1" y="1" z="2" />
  <Block id="minecraft:lightning_rod" x="-1" y="2" z="0" />
  <Block id="minecraft:lightning_rod" x="1" y="2" z="0" />
  <Block id="minecraft:lightning_rod" x="-1" y="2" z="2" />
  <Block id="minecraft:lightning_rod" x="1" y="2" z="2" />
  <BlockAnnotation x="0" y="1" z="1">Hier bleibt Luft – der Riss</BlockAnnotation>
</GameScene>

<RecipeFor id="parallelzapfer" />
