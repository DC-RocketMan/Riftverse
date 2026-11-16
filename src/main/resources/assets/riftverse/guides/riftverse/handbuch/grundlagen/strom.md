---
navigation:
  parent: grundlagen/index.md
  title: Strom und Spannung
  icon: riftverse:einspeiser
  position: 10
item_ids:
  - riftverse:einspeiser
  - riftverse:ausspeiser
  - riftverse:kreativ_quelle
---

# Strom und Spannung

Riftverse rechnet in FE wie fast jeder Technik-Mod. Aber Riftverse-Strom fließt in einem **eigenen Netz mit den
Regeln von IndustrialCraft**:

- **Strom fließt in Paketen mit einer Spannungsstufe.** Ein Generator schickt Pakete seiner Stufe los.
- **Ist ein Paket zu groß für eine Maschine, explodiert sie sofort.** Keine Warnung, kein Rauch vorher.
- **Ist ein Paket zu groß für eine Leitung, schmilzt die Leitung.**
- **Leitungen verlieren unterwegs etwas Strom** – pro Block und Paket ein kleiner fester Betrag.

| Stufe | Größtes Paket | Leitung |
|---|---|---|
| I Niederspannung | 128 FE/t | Kupferkabel |
| II Laborspannung | 1.024 FE/t | Flux-Leitung |
| III Industriespannung | 8.192 FE/t | Gravitit-Leitung |
| IV Rift-Spannung | unbegrenzt | Null-Leitung, Rift-Kabelknoten |

Jede Maschine verträgt höchstens die Stufe ihres Tiers. Eine T1-Maschine direkt an einem T2-Generator fliegt also in
die Luft. Dazwischen gehört ein [Umspanner](umspanner.md).

## Was hilft

- **Rechtsklick mit leerer Hand auf ein Kabel** zeigt das Teilnetz: Quellen, Verbraucher, Leitungen, übertragener
  Strom und die Stufe.
- **Jade** (falls installiert) zeigt beim Hinsehen, welche Stufe eine Maschine verträgt oder abgibt.
- Der **Transformator-Chip** lässt eine Maschine eine Stufe mehr vertragen. Siehe [Upgrades](upgrades.md).
- Wie hart Überspannung bestraft wird, stellt die Config ein: explodieren, nur durchbrennen oder aus.

## Andere Mods anschließen

Riftverse-Leitungen verbinden sich nur mit Riftverse-Blöcken. Fremder Strom kommt über zwei Übergänge:

<Row>
  <BlockImage id="einspeiser" scale="1.5" />
  <BlockImage id="ausspeiser" scale="1.5" />
</Row>

- <ItemLink id="einspeiser" />: nimmt FE von jedem Mod an und speist es mit Stufe I ins Riftverse-Netz.
- <ItemLink id="ausspeiser" />: gibt Riftverse-Strom als normales FE ab. Fremde Maschinen explodieren nie.

Items wie Energiezellen, Gadgets und Rüstung sind reines FE und laden in jedem Ladegerät, auch aus anderen Mods.

<Row>
  <RecipeFor id="einspeiser" />
  <RecipeFor id="ausspeiser" />
</Row>

## Zum Ausprobieren

Die <ItemLink id="kreativ_quelle" /> gibt es nur im Kreativmodus: eine unendliche Quelle, bei der ein Rechtsklick die
Stufe von I bis IV wechselt. Gut, um vor dem Ernstfall zu testen, was eine Maschine aushält.
