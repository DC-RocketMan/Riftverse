---
navigation:
  parent: grundlagen/index.md
  title: Kabel und Leitungen
  icon: riftverse:kupferkabel
  position: 20
item_ids:
  - riftverse:kupferkabel
  - riftverse:kupferkabel_isoliert
  - riftverse:flux_leitung
  - riftverse:flux_leitung_isoliert
  - riftverse:gravitit_leitung
  - riftverse:gravitit_leitung_isoliert
  - riftverse:gravitit_leitung_doppelt
  - riftverse:null_leitung
  - riftverse:isolierfolie
---

# Kabel und Leitungen

Ein Kabel ist in Riftverse kein voller Block, sondern ein dünner Strang wie bei IndustrialCraft: ein Kern in der
Blockmitte und Arme nur zu den Seiten, an denen etwas angeschlossen ist. Ein einzelnes Kabel ohne Nachbarn ist deshalb
nur ein kleiner Würfel – erst in einer Reihe wird es zum Strang.

<GameScene zoom="4" interactive={true}>
  <Block id="riftverse:kurbel_dynamo" x="0" y="0" z="0" p:facing="north" />
  <Block id="riftverse:kupferkabel" x="1" y="0" z="0" p:west="true" p:east="true" />
  <Block id="riftverse:kupferkabel" x="2" y="0" z="0" p:west="true" p:east="true" />
  <Block id="riftverse:kupferkabel" x="3" y="0" z="0" p:west="true" p:east="true" />
  <Block id="riftverse:erzmuehle" x="4" y="0" z="0" p:facing="north" />
  <BlockAnnotation x="0" y="0" z="0">Kurbel-Dynamo: liefert Stufe I</BlockAnnotation>
  <BlockAnnotation x="2" y="0" z="0">Kupferkabel, blank: 0,5 FE Verlust pro Block, gibt einen Schlag</BlockAnnotation>
  <BlockAnnotation x="4" y="0" z="0">Erzmühle: verträgt Stufe I</BlockAnnotation>
</GameScene>

## Alle Leitungen nebeneinander

Jede Stufe hat ihre eigene Leitung. Jede Isolierschicht macht sie dicker; die Null-Leitung der höchsten Stufe ist
wieder dünn, wie bei IC2 die Glasfaser.

<GameScene zoom="3" interactive={true}>
  <Block id="riftverse:kupferkabel" x="0" y="0" z="0" p:north="true" p:south="true" />
  <Block id="riftverse:kupferkabel_isoliert" x="1" y="0" z="0" p:north="true" p:south="true" />
  <Block id="riftverse:flux_leitung" x="2" y="0" z="0" p:north="true" p:south="true" />
  <Block id="riftverse:flux_leitung_isoliert" x="3" y="0" z="0" p:north="true" p:south="true" />
  <Block id="riftverse:gravitit_leitung" x="4" y="0" z="0" p:north="true" p:south="true" />
  <Block id="riftverse:gravitit_leitung_isoliert" x="5" y="0" z="0" p:north="true" p:south="true" />
  <Block id="riftverse:gravitit_leitung_doppelt" x="6" y="0" z="0" p:north="true" p:south="true" />
  <Block id="riftverse:null_leitung" x="7" y="0" z="0" p:north="true" p:south="true" />
</GameScene>

| Leitung | Stufe | Dicke | Verlust pro Block | Anfassen |
|---|---|---|---|---|
| <ItemLink id="kupferkabel" /> | I | 4 px | 0,5 FE | Schlag |
| <ItemLink id="kupferkabel_isoliert" /> | I | 6 px | 0,5 FE | sicher |
| <ItemLink id="flux_leitung" /> | II | 4 px | 1 FE | Schlag |
| <ItemLink id="flux_leitung_isoliert" /> | II | 6 px | 1 FE | sicher |
| <ItemLink id="gravitit_leitung" /> | III | 6 px | 2 FE | starker Schlag |
| <ItemLink id="gravitit_leitung_isoliert" /> | III | 8 px | 2 FE | Schlag |
| <ItemLink id="gravitit_leitung_doppelt" /> | III | 10 px | 2 FE | sicher |
| <ItemLink id="null_leitung" /> | IV | 4 px | 0,05 FE | sicher, braucht keine Hülle |

Ein Paket, das größer ist als die Stufe der Leitung, lässt sie schmelzen.

## Isolieren und färben

Jede Leitung wird erst blank gebaut. In der Garagen-Werkbank kommt pro Schicht eine <ItemLink id="isolierfolie" />
dazu. Isolierfolie presst die Garagenpresse aus Polymerfetzen, die der Recycler aus Alien-Schrott holt.

Isolierte Leitungen lassen sich mit **Farbstoff** einfärben (Rechtsklick). Unterschiedlich gefärbte Leitungen
verbinden sich nicht – so laufen zwei Netze direkt nebeneinander.

<GameScene zoom="4" interactive={true}>
  <Block id="riftverse:kupferkabel_isoliert" x="0" y="0" z="0" p:east="true" p:color="15" />
  <Block id="riftverse:kupferkabel_isoliert" x="1" y="0" z="0" p:west="true" p:east="true" p:color="15" />
  <Block id="riftverse:kupferkabel_isoliert" x="2" y="0" z="0" p:west="true" p:color="15" />
  <Block id="riftverse:kupferkabel_isoliert" x="0" y="0" z="1" p:east="true" p:color="12" />
  <Block id="riftverse:kupferkabel_isoliert" x="1" y="0" z="1" p:west="true" p:east="true" p:color="12" />
  <Block id="riftverse:kupferkabel_isoliert" x="2" y="0" z="1" p:west="true" p:color="12" />
</GameScene>

<RecipesFor id="isolierfolie" />

## Rezepte

<Row>
  <RecipeFor id="kupferkabel" />
  <RecipeFor id="kupferkabel_isoliert" />
  <RecipeFor id="flux_leitung" />
  <RecipeFor id="flux_leitung_isoliert" />
  <RecipeFor id="gravitit_leitung" />
  <RecipeFor id="gravitit_leitung_isoliert" />
  <RecipeFor id="gravitit_leitung_doppelt" />
  <RecipeFor id="null_leitung" />
</Row>
