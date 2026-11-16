---
navigation:
  parent: energie/index.md
  title: Gravitations-Pendel
  icon: riftverse:gravitations_pendel
  position: 40
item_ids:
  - riftverse:gravitations_pendel
  - riftverse:schwebstein_kern
---

# Gravitations-Pendel

Ein Gestell, in dem eine schwebende Steinkugel zwischen zwei Polen pendelt. **400 FE/t auf Stufe II, ganz ohne
Brennstoff.**

Die Schwerkraft zählt mit: In der Hochgravitationswelt liefert es das Doppelte, in der Niedriggravitationswelt nur die
Hälfte. Eine Außenbasis in einem schweren Universum lohnt sich also – den Strom bringen Rift-Kabelknoten nach Hause.

## Aufbau (3 × 5 × 3)

Boden und Deckel aus Labor-Hülle, vier Eckpfosten aus Labor-Hülle (3 hoch), in der Mitte auf halber Höhe der
<ItemLink id="schwebstein_kern" />. Der Steuerblock sitzt vorne unten in der Mitte.

<GameScene zoom="3" interactive={true}>
  <Block id="riftverse:gravitations_pendel" x="0" y="0" z="0" p:facing="north" />
  <Block id="riftverse:labor_huelle" x="-1" y="0" z="0" />
  <Block id="riftverse:labor_huelle" x="1" y="0" z="0" />
  <Block id="riftverse:labor_huelle" x="-1" y="0" z="1" />
  <Block id="riftverse:labor_huelle" x="0" y="0" z="1" />
  <Block id="riftverse:labor_huelle" x="1" y="0" z="1" />
  <Block id="riftverse:labor_huelle" x="-1" y="0" z="2" />
  <Block id="riftverse:labor_huelle" x="0" y="0" z="2" />
  <Block id="riftverse:labor_huelle" x="1" y="0" z="2" />
  <Block id="riftverse:labor_huelle" x="-1" y="1" z="0" />
  <Block id="riftverse:labor_huelle" x="1" y="1" z="0" />
  <Block id="riftverse:labor_huelle" x="-1" y="1" z="2" />
  <Block id="riftverse:labor_huelle" x="1" y="1" z="2" />
  <Block id="riftverse:labor_huelle" x="-1" y="2" z="0" />
  <Block id="riftverse:labor_huelle" x="1" y="2" z="0" />
  <Block id="riftverse:schwebstein_kern" x="0" y="2" z="1" />
  <Block id="riftverse:labor_huelle" x="-1" y="2" z="2" />
  <Block id="riftverse:labor_huelle" x="1" y="2" z="2" />
  <Block id="riftverse:labor_huelle" x="-1" y="3" z="0" />
  <Block id="riftverse:labor_huelle" x="1" y="3" z="0" />
  <Block id="riftverse:labor_huelle" x="-1" y="3" z="2" />
  <Block id="riftverse:labor_huelle" x="1" y="3" z="2" />
  <Block id="riftverse:labor_huelle" x="-1" y="4" z="0" />
  <Block id="riftverse:labor_huelle" x="0" y="4" z="0" />
  <Block id="riftverse:labor_huelle" x="1" y="4" z="0" />
  <Block id="riftverse:labor_huelle" x="-1" y="4" z="1" />
  <Block id="riftverse:labor_huelle" x="0" y="4" z="1" />
  <Block id="riftverse:labor_huelle" x="1" y="4" z="1" />
  <Block id="riftverse:labor_huelle" x="-1" y="4" z="2" />
  <Block id="riftverse:labor_huelle" x="0" y="4" z="2" />
  <Block id="riftverse:labor_huelle" x="1" y="4" z="2" />
  <BlockAnnotation x="0" y="0" z="0">Steuerblock (Front nach vorne)</BlockAnnotation>
  <BlockAnnotation x="0" y="2" z="1">Schwebstein-Kern</BlockAnnotation>
</GameScene>

<Row>
  <RecipeFor id="gravitations_pendel" />
  <RecipeFor id="schwebstein_kern" />
</Row>
