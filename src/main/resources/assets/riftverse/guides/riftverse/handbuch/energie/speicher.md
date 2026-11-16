---
navigation:
  parent: energie/index.md
  title: Speicher
  icon: riftverse:xenit_zelle
  position: 80
item_ids:
  - riftverse:energiezelle
  - riftverse:garagen_akku
  - riftverse:xenit_zelle
  - riftverse:flux_zelle
  - riftverse:chrono_speicher
---

# Speicher

Vier Speicherblöcke, einer pro Stufe, und eine kleine Zelle für die Hosentasche. Alle Speicherblöcke **behalten ihre
Ladung beim Abbauen**, laden ein Item im linken Slot und entladen eins im rechten. Sie geben Strom nur an Verbraucher
ab, nicht an andere Speicher – so pumpen zwei Akkus keine Energie im Kreis.

<Row>
  <BlockImage id="garagen_akku" />
  <BlockImage id="xenit_zelle" />
  <BlockImage id="flux_zelle" />
  <BlockImage id="chrono_speicher" />
</Row>

| Speicher | Stufe | Fasst | Besonderheit |
|---|---|---|---|
| <ItemLink id="garagen_akku" /> | I | 100.000 FE | ganz ohne Xenit, schon in der Garage baubar |
| <ItemLink id="xenit_zelle" /> | I | 1 Mio. FE | der Akku der Bastlerwerkstatt |
| <ItemLink id="flux_zelle" /> | II | 16 Mio. FE | Füllstand leuchtet als Kristallband |
| <ItemLink id="chrono_speicher" /> | III | 256 Mio. FE | lädt 2.048 FE/t, gibt 4× so schnell ab |

## Garagen-Akku

Holzkiste, Zinn, Kupfer, Eisen und Redstone: Mehr braucht ein erster Speicher nicht. Er fängt auf, was der
Kurbel-Dynamo oder der Tonnenreaktor zu viel liefern, und gibt es ab, wenn die Maschinen laufen.

<RecipeFor id="garagen_akku" />

## Xenit-Zelle, Flux-Zelle, Chrono-Speicher

Chrono-Speicher, die sich berühren, bilden einen **Verbund** (bis 9 × 9 × 9): Sie gleichen ihre Ladung aus und wirken
wie ein großer Speicher.

<Row>
  <RecipeFor id="xenit_zelle" />
  <RecipeFor id="flux_zelle" />
  <RecipeFor id="chrono_speicher" />
</Row>

## Energiezelle

<ItemImage id="energiezelle" float="right" />

Die kleine Batterie (40.000 FE) passt in den **Akku-Slot** jeder Maschine und jedes Generators: Generatoren laden sie,
Maschinen ziehen Strom aus ihr. Sie ist reines FE und lädt auch in Ladegeräten anderer Mods. Gadgets und Speicher
brauchen sie als Bauteil.

<br clear="all" />

<RecipeFor id="energiezelle" />
