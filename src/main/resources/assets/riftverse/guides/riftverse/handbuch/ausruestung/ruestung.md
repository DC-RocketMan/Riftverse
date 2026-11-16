---
navigation:
  parent: ausruestung/index.md
  title: Multiversal-Rüstung
  icon: riftverse:kernmantel_perfektioniert
  position: 20
item_ids:
  - riftverse:analyse_visier_rohling
  - riftverse:analyse_visier_kalibriert
  - riftverse:analyse_visier_perfektioniert
  - riftverse:kernmantel_rohling
  - riftverse:kernmantel_kalibriert
  - riftverse:kernmantel_perfektioniert
  - riftverse:exo_rahmen_rohling
  - riftverse:exo_rahmen_kalibriert
  - riftverse:exo_rahmen_perfektioniert
  - riftverse:gravi_stiefel_rohling
  - riftverse:gravi_stiefel_kalibriert
  - riftverse:gravi_stiefel_perfektioniert
  - riftverse:kalibrier_satz
  - riftverse:multiversum_legierung
---

# Multiversal-Rüstung

<Row>
  <ItemImage id="analyse_visier_perfektioniert" scale="1.5" />
  <ItemImage id="kernmantel_perfektioniert" scale="1.5" />
  <ItemImage id="exo_rahmen_perfektioniert" scale="1.5" />
  <ItemImage id="gravi_stiefel_perfektioniert" scale="1.5" />
</Row>

Graphit-Panzerplatten mit Petrol-Kanten über einem langen Labormantel. Die Rüstung hat **keine Haltbarkeit – sie läuft
mit FE.** Jedes Teil hat einen eigenen Speicher; was leer ist, zieht aus dem Kernmantel nach. Laufende Kosten werden
einmal pro Sekunde abgebucht. Die eigentliche Stärke kommt aus den [Modulen](module.md).

| Teil | Slots | Speicher | Rüstung / Härte | Grundeigenschaft |
|---|---|---|---|---|
| Analyse-Visier (Helm) | 3 | 500.000 FE | 3 / 2 → 5 / 4 | HUD mit Universum und Kanonenfluid |
| Kernmantel (Brust) | 4 | 2 Mio. FE | 8 / 3 → 10 / 5 | versorgt alle Teile, Kanonentank 4.000 mB |
| Exo-Rahmen (Hose) | 3 | 500.000 FE | 6 / 2 → 8 / 4 | +10 % Abbautempo |
| Gravi-Stiefel | 2 | 250.000 FE | 3 / 2 → 4 / 4 | kein Fallschaden bis 10 Blöcke |

## Drei Ausbaustufen

Module, Ladung und Brusttank wandern bei jeder Stufe mit.

1. **Rohling** im Präzisions-Montierer: 6 Schritte mit Graphitplatten, Gravitit-Rahmen, Rift-Schaltkreisen,
   Flux-Spulen, einem Teil-Bauteil und Wolle für den Labormantel.
2. **Kalibriert** im Quanten-Lithografen: Rohling + 2 Quanten-Schaltkreise + <ItemLink id="kalibrier_satz" />
   (Material aus drei Universen und ein Story-Fragment).
3. **Perfektioniert** in der Rift-Schmiede: 2 Minuten bei 20.000 FE/t mit 4 Nullmaterie, 2 Multiversal-Prozessoren,
   <ItemLink id="multiversum_legierung" /> (Material aus sechs Universen) und 3 Story-Fragmenten.

<Row>
  <RecipeFor id="kalibrier_satz" />
  <RecipeFor id="multiversum_legierung" />
</Row>

## Analyse-Visier

<RecipeFor id="analyse_visier_rohling" />
<Row>
  <RecipeFor id="analyse_visier_kalibriert" />
  <RecipeFor id="analyse_visier_perfektioniert" />
</Row>

## Kernmantel

Den Brusttank füllst du mit angelegtem Kernmantel: Schleichen + Rechtsklick mit einer vollen Kartusche. Die Kanone
tankt zuerst aus dem Brusttank nach, dann aus Kartuschen.

<RecipeFor id="kernmantel_rohling" />
<Row>
  <RecipeFor id="kernmantel_kalibriert" />
  <RecipeFor id="kernmantel_perfektioniert" />
</Row>

## Exo-Rahmen

<RecipeFor id="exo_rahmen_rohling" />
<Row>
  <RecipeFor id="exo_rahmen_kalibriert" />
  <RecipeFor id="exo_rahmen_perfektioniert" />
</Row>

## Gravi-Stiefel

<RecipeFor id="gravi_stiefel_rohling" />
<Row>
  <RecipeFor id="gravi_stiefel_kalibriert" />
  <RecipeFor id="gravi_stiefel_perfektioniert" />
</Row>

## Set-Bonus „Multiversale Resonanz“

Alle vier Teile perfektioniert und ein Universums-Kern im Kernmantel: **halber Kanonenverbrauch** und Phasenschritt
ohne Abklingzeit in fremden Universen.
