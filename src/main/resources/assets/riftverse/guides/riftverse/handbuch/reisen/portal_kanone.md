---
navigation:
  parent: reisen/index.md
  title: Portal-Kanone
  icon: riftverse:portal_kanone_mk1
  position: 0
item_ids:
  - riftverse:portal_kanone_mk1
  - riftverse:portal_kanone_mk2
  - riftverse:portal_kanone_mk3
  - riftverse:portal_kanone_mk4
  - riftverse:kanonengehaeuse
  - riftverse:kanonenkern
  - riftverse:chronoglas_griff
  - riftverse:fluid_kartusche
  - riftverse:fluid_kartusche_voll
  - riftverse:fluid_kartusche_stabil
  - riftverse:fluid_kartusche_konzentrat
  - riftverse:fluid_kartusche_essenz
---

# Portal-Kanone

<Row>
  <ItemImage id="portal_kanone_mk1" scale="1.5" />
  <ItemImage id="portal_kanone_mk2" scale="1.5" />
  <ItemImage id="portal_kanone_mk3" scale="1.5" />
  <ItemImage id="portal_kanone_mk4" scale="1.5" />
</Row>

Die Kanone ist das Rückgrat von Riftverse: Jede Stufe braucht die vorige als Zutat und Teile aus dem jeweils neuen Tier.

| Kanone | Gebaut an | Fluid | pro Schuss | Kann |
|---|---|---|---|---|
| <ItemLink id="portal_kanone_mk1" /> | Garagen-Werkbank | roh oder stabil | 250 mB | zufällige Sprünge |
| <ItemLink id="portal_kanone_mk2" /> | Präzisions-Montierer | stabil oder Konzentrat | 100 mB | + gezielt mit Koordinatenkarte |
| <ItemLink id="portal_kanone_mk3" /> | Präzisions-Montierer | Konzentrat oder Essenz | 50 mB | + 8 gespeicherte Ziele, Kurzstrecken-Portale |
| <ItemLink id="portal_kanone_mk4" /> | Rift-Schmiede | nur Essenz | 25 mB | + Leere Welt, gezüchtete Universen |

## Welches Fluid wohin führt

Jede Kanone nimmt ihr eigenes Fluid und das nächstbessere. Mit dem besseren springt sie **zufällig auch eine Stufe
weiter**. Du kommst also an neue Rohstoffe, bevor du die nächste Kanone hast – nur gezielt geht es erst mit ihr.

| Fluid | Kartusche | Zufällige Ziele |
|---|---|---|
| Roh-Portalfluid (zerfällt nach 10 min) | <ItemLink id="fluid_kartusche_voll" /> | Schrott-, Kristall-, toxische Welt |
| stabiles Portalfluid | <ItemLink id="fluid_kartusche_stabil" /> | + Niedriggravitation, Zeitdilatation, organische Welt |
| Portalfluid-Konzentrat | <ItemLink id="fluid_kartusche_konzentrat" /> | + Hochgravitation, Spiegelwelt |
| Weltenformer-Essenz | <ItemLink id="fluid_kartusche_essenz" /> | + Leere Welt |

Kartuschen fassen 1.000 mB. Die <ItemLink id="fluid_kartusche" /> füllst du im Fluid-Mixer (roh) oder in der
Raffinerie (stabil, Konzentrat); Essenz macht nur der Weltenformer.

<RecipeFor id="fluid_kartusche" />

## Mk I

Kanonengehäuse, Fluxlinse, 2 Basis-Schaltkreise und eine Kartusche. Die Kanone startet leer und lädt volle Kartuschen
aus dem Inventar nach.

<Row>
  <RecipeFor id="kanonengehaeuse" />
  <RecipeFor id="portal_kanone_mk1" />
</Row>

## Mk II

Im Präzisions-Montierer in 5 Schritten. Mit einer **Koordinatenkarte in der anderen Hand** führt der Schuss genau in
dieses Universum.

<Row>
  <RecipeFor id="kanonenkern" />
  <RecipeFor id="chronoglas_griff" />
</Row>

<RecipeFor id="portal_kanone_mk2" />

## Mk III

Merkt sich **8 Ziele**: Mit Koordinatenkarte in der anderen Hand schießen speichert das Ziel. Taste
<KeyBind id="key.riftverse.ziel" /> wechselt zwischen Zufall, Kurzstrecke und den gespeicherten Zielen,
Schleichen + <KeyBind id="key.riftverse.ziel" /> löscht das gewählte. **Kurzstrecken-Portale** führen bis 96 Blöcke
weit innerhalb derselben Welt. Braucht ein [Story-Fragment](../bauteile/fragmente.md).

<RecipeFor id="portal_kanone_mk3" />

## Mk IV

Aus der Rift-Schmiede, mit Nullmaterie-Kern und 3 Story-Fragmenten. Nur sie erreicht gezüchtete Universen aus dem
Weltenformer.

<RecipeFor id="portal_kanone_mk4" />
