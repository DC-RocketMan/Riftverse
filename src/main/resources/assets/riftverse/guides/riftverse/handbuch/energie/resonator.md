---
navigation:
  parent: energie/index.md
  title: Fluxkristall-Resonator
  icon: riftverse:fluxkristall_resonator
  position: 30
item_ids:
  - riftverse:fluxkristall_resonator
  - riftverse:resonator_kammer
  - riftverse:fluxkristall_stab
  - riftverse:kuehlkoerper
---

# Fluxkristall-Resonator

<BlockImage id="fluxkristall_resonator" scale="2" float="right" />

Ein Kristall in einer Stimmgabel-Halterung, der mit seinen Nachbarn mitschwingt. Funktioniert wie ein Reaktor aus
IndustrialCraft: Du legst ein Raster aus <ItemLink id="fluxkristall_stab" /> und <ItemLink id="kuehlkoerper" />
und hältst die Hitze im Griff. **Stufe II.**

<br clear="all" />

## So funktioniert das Raster

- Der Resonator hat **3 Spalten à 3 Plätzen**. Jede angrenzende <ItemLink id="resonator_kammer" /> (bis zu 6) gibt
  eine Spalte dazu.
- Jeder **Stab** schwingt mit seinen Nachbarstäben: ein Puls plus einer je Nachbarstab, **20 FE/t und 4 Hitze pro Puls**.
- Jeder **Kühlkörper** führt 12 Hitze pro Tick ab, die Hülle selbst 1.
- **Wasser** im Tank kühlt zusätzlich und bringt die **1,5-fache Leistung**.
- Bei **10.000 Hitze explodiert der Kristall.** Ein Redstone-Signal schaltet den Resonator ab.

Ein guter Aufbau schafft rund 300 FE/t ungekühlt und 450 FE/t gekühlt. Viele Stäbe dicht beieinander bringen viel
Strom und viel Hitze – wie viel Risiko du eingehst, entscheidest du.

<Row>
  <RecipeFor id="fluxkristall_resonator" />
  <RecipeFor id="resonator_kammer" />
</Row>

<Row>
  <RecipeFor id="fluxkristall_stab" />
  <RecipeFor id="kuehlkoerper" />
</Row>
