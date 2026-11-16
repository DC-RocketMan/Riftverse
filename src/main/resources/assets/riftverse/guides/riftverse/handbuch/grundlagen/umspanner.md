---
navigation:
  parent: grundlagen/index.md
  title: Umspanner
  icon: riftverse:umspanner
  position: 30
item_ids:
  - riftverse:umspanner
  - riftverse:umspanner_ii
  - riftverse:umspanner_iii
---

# Umspanner

<BlockImage id="umspanner" scale="2" float="right" />

Ein Umspanner verbindet zwei Spannungsstufen. Die **Front** ist die hohe Stufe, die übrigen Seiten die niedrige.

- **Ohne Redstone spannt er herunter:** Aus einem großen Paket werden bis zu 4 kleine Pakete pro Tick.
- **Mit Redstone-Signal spannt er hoch:** Viele kleine Pakete werden zu einem großen.

Die Seiten haben im Netz feste Rollen, Strom fließt also nie rückwärts durch einen Umspanner.

<br clear="all" />

| Umspanner | Verbindet |
|---|---|
| <ItemLink id="umspanner" /> | I ↔ II |
| <ItemLink id="umspanner_ii" /> | II ↔ III |
| <ItemLink id="umspanner_iii" /> | III ↔ IV |

> **Garagen-Notiz:** Der Parallelzapfer liefert Stufe IV. Ohne Umspanner III ↔ IV davor grillt er deine eigenen
> T3-Maschinen.

<Row>
  <RecipeFor id="umspanner" />
  <RecipeFor id="umspanner_ii" />
  <RecipeFor id="umspanner_iii" />
</Row>
