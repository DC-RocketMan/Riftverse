# Riftverse

Ein Minecraft-Modpack im Stil von Feed The Beast, inspiriert von Rick and Mortys Portalreisen.
Dieses Repo enthält den Riftverse-Mod für **NeoForge 1.21.1**. Die Anleitung für alles Weitere steht im
Design-Dokument („Riftverse – Design-Dokument“), gebaut wird Meilenstein für Meilenstein.

## Stand: Meilenstein 5 – T4 Weltenformer

Mit dem Weltenformer kommen Stufe IV mit der Null-Leitung, die Rift-Schmiede, der Weltenformer, der Singularitätskern,
die Portal-Kanone Mk IV, die Leere Welt und gezüchtete Universen dazu.

| Bereich | Inhalt |
|---|---|
| **Stufe IV** | Null-Leitung (4 px, sicher ohne Hülle, 0,05 FE Verlust pro Block, unbegrenzte Paketgröße) aus Nullmaterie-Faser |
| **Gehäuse** | Null-Hülle für den Weltenformer-Ring (ohne Nullmaterie), Null-Gehäuse (mit Nullmaterie-Faser), Eindämmungsring |
| **Rift-Schmiede** | Multiblock 5 × 5 × 5, 20.000 FE/t auf Stufe IV, sechs Eingänge: Multiversal-Prozessor, Singularitätslinse, Null-Hülle, alles aus Nullmaterie und die Kanone Mk IV |
| **Weltenformer** | Ring 7 × 7 × 3, 40.000 FE/t + Portalfluid-Konzentrat; macht Weltenformer-Essenz oder züchtet ein Universum nach Wahl |
| **Singularitätskern** | 100.000 FE/t auf Stufe IV, frisst 64 Items pro Sekunde; hungrig wächst er und frisst seine Umgebung; Notabschaltung per Knopf oder Redstone; der Eindämmungsring (5 × 5 × 5) schaltet ihn stattdessen sicher ab |
| **Zwischenprodukte (T4-Leitern)** | Multiversal-Prozessor, Singularitätslinse, Null-Spule, Weltenformer-Essenz, Null-Gehäuse, Null-Leitung; dazu Nullmaterie-Faser und Nullmaterie-Kern |
| **Kanone Mk IV** | Aus der Rift-Schmiede: Mk III, Multiversal-Prozessor, Singularitätslinse, Nullmaterie-Kern, 3 Story-Fragmente; nur Weltenformer-Essenz, 25 mB pro Schuss; erreicht die Leere Welt und gezüchtete Universen |
| **Leere Welt** | Ewige Sternennacht, fast keine Blöcke, schwebende Kerne aus Leerstein; seltene Nullkerne mit Nullmaterie-Erz, bewacht von Plagegeistern; die Leere frisst Licht, Betten explodieren, kein Klon-Tank |
| **Gezüchtete Universen** | Landschaft, Rohstoffe, Schwerkraft (0,3 / 1 / 2,5 g) und Gefahren (an/aus) frei gewählt; erreichbar nur über die Koordinatenkarte des Weltenformers |

### So spielt sich Meilenstein 5

1. Die **Rift-Schmiede** baut man noch aus T3-Teilen: Steuerblock (Werkbank: Quanten-Schaltkreise, Kanonenkern,
   Graphit-Gehäuse, Amboss, Gravitit-Spulen, Chrono-Linse) vorne in der Mitte eines Bodens aus 5 × 5 Graphit-Hülle,
   vier Eckpfosten (3 hoch) und ein Deckelrahmen aus Graphit-Hülle. In der Mitte steht ein Amboss, darüber bleibt Luft.
   Strom bis zur Null-Leitung: ein Parallelzapfer direkt am Steuerblock oder ein Umspanner III ↔ IV davor.
2. In der Schmiede **Multiversal-Prozessoren**, **Singularitätslinsen** und **Null-Hülle** (4 Graphit-Hülle +
   Gravitit + 2 Obsidian → 4) machen.
3. Den **Weltenformer** bauen: Steuerblock vorne in der Mitte eines Rings aus 7 × 7 Null-Hülle, darüber eine Lage mit
   Null-Hülle an den Ecken und Glas an den Seiten, oben wieder ein Ring aus Null-Hülle. Innen bleibt alles frei.
4. Im Modus **Essenz**: leere Kartusche + Inversionssplitter + 2.000 mB Konzentrat → **Weltenformer-Essenz**.
   Mit Essenz springt die **Kanone Mk III** zufällig auch in die **Leere Welt**.
5. In der Leeren Welt die **Nullkerne** suchen (weinender Obsidian auf der Oberseite), die Plagegeister besiegen und das
   **Nullmaterie-Erz** mit einer Diamantspitzhacke abbauen.
6. In der Schmiede Nullmaterie zu **Nullmaterie-Faser** (→ Null-Leitung, Null-Spule, Null-Gehäuse) und zum
   **Nullmaterie-Kern** verarbeiten, dann die **Kanone Mk IV** schmieden.
7. Im Modus **Welt**: zwei Universumsproben (links die Landschaft, rechts die Rohstoffe), Schwerkraft und Gefahren an
   den Knöpfen wählen, Papier einlegen – nach einer Minute kommt die **Koordinatenkarte** eines neuen Universums heraus.
   Mit Karte in der anderen Hand bringt die Mk IV einen dorthin.
8. Den **Singularitätskern** (Null-Spulen, Nullmaterie-Kern, Singularitätslinsen, Null-Gehäuse, Multiversal-Prozessor)
   zünden und füttern – 64 beliebige Items pro Sekunde, am besten über Rohre. Wer sich nicht traut, baut den
   **Eindämmungsring** drumherum.

### T4-Geräte

- **Weltenformer:** Slots von links: Probe Landschaft, Probe Rohstoffe, leere Kartusche oder Papier, Inversionssplitter,
  Ausgang; rechts Konzentrat-Kartusche rein und leere Kartusche raus (Rohre füllen Konzentrat direkt in den Tank,
  16.000 mB). Die Proben bleiben liegen. Proben aus gezüchteten Universen geben deren Wahl weiter; die Leere Welt taugt
  nicht als Vorlage. Im Ring schwebt ein Partikel-Planet in den Farben der Landschaft, plattgedrückt bei 2,5 g.
- **Singularitätskern:** neun Futterslots, Akku-Slot wird geladen. Zu wenig Futter heißt anteilig weniger Strom. Nach
  5 Sekunden Hunger wächst er alle 10 Sekunden eine Stufe (bis 6): Er saugt Gegenstände, Mobs und Spieler an, verletzt
  alles direkt am Kern und frisst Blöcke im Umkreis von 1 + Stufe (keine Maschinen, keine Kisten, nichts
  Unzerstörbares). Gefressenes zählt als Futter. Die Notabschaltung (Knopf oder Redstone-Signal) hält ihn sofort an
  und setzt das Wachstum zurück; danach kühlt er eine Minute ab.
- **Eindämmungsring:** drei Ringe wie bei einem Kreiselkompass, jeder der Rand eines 5 × 5-Quadrats durch den Kern
  (waagerecht, Nord-Süd, Ost-West; 42 Blöcke). Steht der Ring, schaltet der Kern nach 5 Sekunden Hunger sicher ab.

### Entscheidungen in Meilenstein 5

- **Henne-Ei-Problem wie in Meilenstein 3 und 4:** Die Mk IV braucht Nullmaterie aus der Leeren Welt. Deshalb kommen
  Rift-Schmiede und Weltenformer ohne Nullmaterie aus (Graphit-Hülle, Null-Hülle aus Graphit, Gravitit und Obsidian),
  und mit **Weltenformer-Essenz** springt die Mk III zufällig in die Leere Welt.
- **Kanonen-Fluide:** Jede Kanone nimmt ihr Fluid und das nächsthöhere: Mk I roh und stabil, Mk II stabil und Konzentrat,
  Mk III Konzentrat und Essenz, Mk IV nur Essenz.
- **Weltenformer-Essenz gibt es nur in Kartuschen,** nicht als eigenes Fluid in Rohren (sie entsteht direkt in der
  Kartusche).
- **Gezüchtete Universen** liegen in einer eigenen Dimension. Ihre Wahl steckt in der Zellnummer, deshalb braucht der
  Weltengenerator keine Speicherdaten und das Aufräumen funktioniert wie überall. Pro Kombination gibt es 50 Universen;
  danach landet man wieder in einem schon gezüchteten. Zufallssprünge führen nie in ein gezüchtetes Universum.
- **Was eine Landschaft mitbringt:** Gelände, Gestein, Deko, Biom, Nachwachsen (organisch), Kristallknospen und – wenn
  „Gefahr: an“ – Giftluft, Säureregen, Resonanz und Doppelgänger. Nicht übertragen werden die Effekte, die an der
  Dimension hängen: vertauschte Tageszeit der Spiegelwelt, 4× Zufallsticks und Zeitverzerrungen der
  Zeitdilatationswelt. Die Schwerkraft kommt immer vom Knopf, Fallschaden 3× ab 2,5 g.
- **Leere Welt:** Die Leere frisst Licht – was ein Spieler dort an Licht setzt, verschwindet nach 5 Sekunden (Riftverse-
  Maschinen ausgenommen). „Kein Respawn-Anker“ heißt: Betten explodieren und ein Klon-Tank lässt sich dort nicht
  aufstellen. Die Rifts landen auf einem großen Ankunftskern in der Zellmitte.
- **Nullmaterie ist selten:** 4–7 Erz pro Nullkern, ein Erz gibt genau eine Nullmaterie (kein Glück). Wächter sind
  Plagegeister aus einem Spawner in der Kammer des Kerns – sie fliegen und kommen durch Wände. Der
  Universums-Destillator holt aus der Leere nur Leerstein und Obsidian.
- **Rift-Schmiede-Hülle:** Die Schmiede nutzt Graphit-Hülle, damit sie vor der ersten Nullmaterie steht; Null-Schwarz
  bleibt dem Weltenformer.
- **Singularitätskern:** Die Notabschaltung ist sicher, kostet aber eine Minute Abkühlzeit. Gefährlich ist nur ein
  hungriger Kern ohne Ring.

## Meilenstein 4 – T3 Multiversum-Industrie

Mit der Multiversum-Industrie kommen Stufe III, alle T3-Maschinen und -Generatoren, der Chrono-Speicher, die
Portal-Kanone Mk III, zwei neue Universumstypen und ein Ersatz für die Story-Fragmente dazu.

| Bereich | Inhalt |
|---|---|
| **Stufe III** | Gravitit-Leitung blank (6 px, starker Schlag), isoliert (8 px, Schlag) und doppelt isoliert (10 px, sicher); 2 FE Verlust pro Block, verträgt 8.192 FE/t. Umspanner II ↔ III und III ↔ IV |
| **Chrono-Speicher** | 256 Mio. FE, Stufe III; lädt 2.048 FE/t und gibt 8.192 FE/t ab (4× schneller); behält die Ladung beim Abbauen; Speicher, die sich berühren, bilden einen Verbund |
| **Gehäuse** | Graphit-Gehäuse (Graphitplatten und Gravitit-Rahmen um ein Labor-Gehäuse) für alle T3-Geräte, Graphit-Hülle für den Parallelzapfer |
| **T3-Maschinen** | Quanten-Lithograf (1.200 FE/t), Materie-Rekonfigurator (2.000 FE/t + 50 mB Portalfluid), Klon-Tank (1.500 FE/t, 50 FE/t Erhalt), Universums-Destillator (3.500 FE/t), Rift-Kabelknoten (100 FE/t Grundlast, 5 % Verlust, Stufe IV) |
| **T3-Generatoren** | Mikrokosmos-Zelle (2.000–8.000 FE/t, Stufe III), Parallelzapfer (Multiblock 3 × 3 × 3, 10.000 FE/t, **Stufe IV**, 5 mB Portalfluid pro Tick) |
| **Zwischenprodukte** | Quanten-Schaltkreis, Chrono-Linse, Gravitit-Spule, Gravitit-Rahmen, Graphitplatte, Fragment-Replik |
| **Portalfluid-Konzentrat** | Aus der Raffinerie (1.000 mB Roh-Portalfluid + Chronoglas-Scheibe + Flux-Stabilisator → 500 mB), in Konzentrat-Kartuschen |
| **Kanone Mk III** | Montiert in 6 Schritten aus Mk II, 2 Quanten-Schaltkreisen, Gravitit-Rahmen, Inversionssplitter, Chrono-Linse und einem Story-Fragment; nur Konzentrat, 50 mB pro Schuss; merkt sich 8 Ziele, öffnet Kurzstrecken-Portale in derselben Welt |
| **Universen** | Hochgravitationswelt (2,5 g, plattgedrückte Ebenen, kaum Sprünge, Fallschaden 3×, Gravitit-Erz), Spiegelwelt (Tag und Nacht vertauscht, an der Mittelachse gespiegeltes Gelände, Spiegelkristall mit Inversionssplittern, Doppelgänger) |
| **Story-Fragmente** | Rezepte fragen das Tag `riftverse:story_fragmente` ab; bis zum Story-Modus füllt es die **Fragment-Replik** aus dem Quanten-Lithografen |

### So spielt sich Meilenstein 4

1. In der **Raffinerie** Roh-Portalfluid mit Chronoglas-Scheibe und Flux-Stabilisator zu **Portalfluid-Konzentrat**
   verdichten und Konzentrat-Kartuschen füllen.
2. Mit Konzentrat springt die **Kanone Mk II** zufällig auch in die T3-Welten. In der **Hochgravitationswelt**
   Gravitit-Erz mit einer Diamantspitzhacke abbauen, in der **Spiegelwelt** Spiegelkristall (gibt Inversionssplitter).
3. In der **Gravitationspresse** Gravitit zu **Gravitit-Rahmen** und Kohleblöcke zu **Graphitplatten** pressen,
   daraus **Graphit-Gehäuse** bauen.
4. Zuerst den **Quanten-Lithografen** bauen – er kommt mit Rift-Schaltkreisen aus und macht die
   **Quanten-Schaltkreise** (Rift-Schaltkreis + Chronoglas-Scheibe + 2 Golddraht) und **Chrono-Linsen**.
5. **Gravitit-Leitungen** legen und mit einem **Umspanner II ↔ III** an das Labor-Netz hängen.
6. **Fragment-Replik** im Lithografen (2 Inversionssplitter + Chronoglas-Scheibe + Nährserum), dann die
   **Kanone Mk III** im Präzisions-Montierer: Slot 1 Mk II, Slot 2 zwei Quanten-Schaltkreise, Slot 3 Gravitit-Rahmen,
   Slot 4 Inversionssplitter, Slot 5 Chrono-Linse, Slot 6 Fragment-Replik.
7. Mit Koordinatenkarte in der anderen Hand schießen: Die Mk III merkt sich das Ziel. **Taste V** wechselt zwischen
   Zufall, Kurzstrecke und den gespeicherten Zielen, **Schleichen + V** löscht das gewählte Ziel.

### T3-Geräte

- **Materie-Rekonfigurator:** Material in Slot 1, Musterstück in Slot 2 (wird nicht verbraucht), stabile oder
  Konzentrat-Kartusche rechts. Umgebaut wird nur innerhalb einer Wertstufe (`riftverse:wertstufe/*`): Erdmetalle
  (Eisen, Kupfer, Zinn), Rohstoffe der T1-, T2- und T3-Universen.
- **Klon-Tank:** zwei Blöcke hoch; wer ihn aufstellt, dem gehört er (Schleichen + Rechtsklick bindet ihn neu).
  Ein Klon braucht 16 Biomasse-Pulpe und 4 Nährserum und wächst 2 Minuten. Stirbt der Besitzer, wacht er vor dem Tank
  im Klon auf und hat alles dabei, was er bei sich trug; der Tank ist danach leer. Ohne 50 FE/t stirbt der Klon ab.
- **Universums-Destillator:** gefüllte Universumsprobe hinein, heraus kommt die Ressource ihres Typs (Schrott, Fluxkristall,
  Säuregel, Schwebstein, Chronoglas-Sand, Biomasse, Gravitit, Inversionssplitter). Eine Probe hält 16 Durchläufe.
- **Rift-Kabelknoten:** werden paarweise gebaut. Kopplungskarte erst an den sendenden, dann an den empfangenden Knoten
  halten; Rechtsklick zeigt den Zustand, Schleichen + Rechtsklick tauscht die Rollen. Trichter und Rohre füllen den
  Sender (Items, Fluids), Strom kommt über Leitungen. Der Empfänger gibt alles wieder heraus, Strom mit **Stufe IV**.
  Beide Knoten halten ihren Chunk geladen; ein Universum mit Kabelknoten wird nie zurückgesetzt.
- **Mikrokosmos-Zelle:** Die Bewohner essen alle 10 Sekunden so viel Nahrung, wie ihre Stufe hoch ist, und verbauen alle
  20 Sekunden Material (Barren, Platten, Bretter, Ziegel, Glas). Der Knopf „Abzug“ (25–100 %) regelt, wie viel Strom man
  abzweigt. Bei 90 % Zufriedenheit steigt die Zivilisation eine Stufe (bis 4 = 8.000 FE/t), bei 0 % wird gestreikt.
- **Parallelzapfer (3 × 3 × 3):** Boden aus Graphit-Hülle mit dem Steuerblock vorne in der Mitte, darüber vier
  Eckpfosten aus Graphit-Hülle, darauf vier Blitzableiter. Über dem Steuerblock in der Mitte bleibt Luft – dort öffnet
  sich der Riss. Jede Quelle hält 3–5 Stunden, dann „Neu ausrichten“ (1 Minute). Manchmal bemerkt die andere Seite den
  Diebstahl: Wächter kommen durch den Riss, der Zapfer setzt 30 Sekunden aus.

### Entscheidungen in Meilenstein 4

- **Henne-Ei-Problem wie in Meilenstein 3:** Mk III braucht Material aus T3-Welten. Deshalb springt die Mk II mit
  **Portalfluid-Konzentrat** zufällig auch dorthin; gezielt geht es erst mit Mk III. Konzentrat entsteht in der T2-Raffinerie.
- **Kanonen-Fluide:** Mk I nimmt Roh- und stabiles Portalfluid, Mk II stabiles Fluid und Konzentrat, Mk III nur Konzentrat.
  Geladen wird die erste passende Kartusche im Inventar.
- **Kanone Mk III:** Zur Liste aus dem Dokument kommt die **Chrono-Linse** (Linsen-Leiter T3). Kurzstrecken-Portale
  reichen 96 Blöcke weit und kosten einen normalen Schuss. Sind alle 8 Plätze belegt, fliegt das älteste Ziel raus.
- **Story-Fragmente:** Die Fragment-Replik füllt das Tag `riftverse:story_fragmente`, bis Story-Modus und Grindweg kommen.
- **Stufe IV ohne Leitung:** Die Null-Leitung kam erst in Meilenstein 5. Bis dahin sitzt der Parallelzapfer (und der
  empfangende Kabelknoten) direkt an der Front eines Umspanners III ↔ IV; jede Gravitit-Leitung dazwischen schmilzt.
- **Chrono-Speicher als Verbund statt Hülle:** Speicher, die sich berühren, gleichen ihre Ladung jeden Tick aus und
  wirken wie ein großer Speicher (bis 9 × 9 × 9). So zählt jeder Speicher mit, ohne eigene Multiblock-Hülle.
- **Universums-Destillator ist ein Einzelblock** (offene Frage im Dokument).
- **Konzentrat im Tank:** Rekonfigurator und Parallelzapfer nehmen stabiles Portalfluid und Konzentrat; Konzentrat zählt doppelt.
- **Hochgravitation:** Mit 2,5 g springt man nur noch gut einen halben Block hoch. Über einen vollen Block kommt man
  nur mit selbst gesetzten Stufen (halben Blöcken), die man mit der normalen Schritthöhe hochläuft. Das gilt auch für
  Mobs. Das Gelände ist plattgedrückt.
- **Spiegelwelt:** Alle Dimensionen teilen sich die Tageszeit der Overworld. Ein kleiner Mixin verschiebt sie für den
  Dimensionstyp der Spiegelwelt um einen halben Tag (Himmel, Licht und Mob-Spawns passen dazu). Das Gelände ist an der
  Nord-Süd-Achse durch die Mitte des Universums gespiegelt. Doppelgänger sind Wüstenzombies mit einer Kopie deiner
  Ausrüstung, die nichts fallen lassen.
- **Graphitplatte:** Der Graphit fürs T3-Gehäuse entsteht in der Gravitationspresse aus einem Kohleblock (2 Platten).

## Meilenstein 3 – T2 Rift-Labor

Mit dem Rift-Labor kommen Stufe II, die sechs T2-Maschinen, vier neue Generatoren, drei neue Universumstypen
und die Portal-Kanone Mk II mit Koordinatenkarten dazu.

| Bereich | Inhalt |
|---|---|
| **Stufe II** | Flux-Leitung (blank mit Schlag, isoliert, färbbar; 1 FE Verlust pro Block, verträgt 1.024 FE/t), Umspanner I ↔ II (Front = Hochspannung, Redstone schaltet von herunter- auf hochspannen) |
| **Flux-Zelle** | 16 Mio. FE, Stufe II, Füllstand als leuchtendes Kristallband, behält die Ladung beim Abbauen |
| **Gehäuse** | Labor-Gehäuse (Verbundplatten um ein Xenit-Gehäuse) für alle T2-Maschinen, Labor-Hülle für die Multiblöcke |
| **T2-Maschinen** | Präzisions-Montierer (sequenzielle Montage, die Reihenfolge der Slots zählt), Quantenzentrifuge, Gravitationspresse, Dimensions-Scanner, Chrono-Beschleuniger (2× / 4× / 8×), Portalfluid-Raffinerie (Multiblock 3 × 3 × 4) |
| **Generatoren** | Biomasse-Kessel (T1, 80 FE/t, Nährserum als Nebenprodukt), Zeitschleifen-Solarpanel (120 FE/t, Lichtschuld), Fluxkristall-Resonator (wachsendes Raster wie ein IC2-Reaktor, Hitze, Wasserkühlung), Gravitations-Pendel (Multiblock 3 × 5 × 3, 400 FE/t, Gravitation zählt) |
| **Zwischenprodukte** | Verbundplatte, Flux-Spule, Rift-Schaltkreis, Stabilisierte Linse, Kanonenkern, Chronoglas-Griff, Antigrav-Platte, Ätzlösung, Toxinrest, Flux-Stabilisator, Nährserum |
| **Stabilisiertes Portalfluid** | Aus der Raffinerie (Roh-Portalfluid + Säuregel + Fluxstaub), zerfällt nicht mehr, kommt in stabile Kartuschen |
| **Kanone Mk II** | Montiert in 5 Schritten aus Mk I, Kanonenkern, Chronoglas-Griff, 2 Verbundplatten und Flux-Spule; nur stabiles Fluid, 100 mB pro Schuss; Koordinatenkarte in der anderen Hand = gezielter Sprung |
| **Universen** | Niedriggravitationswelt (0,3 g, Schwebeinseln über dem Nichts, Schwebstein), Zeitdilatationswelt (4× Zufallsticks, Chronoglas-Sand fällt in Zeitlupe, Zeitverzerrungen), organische Welt (Biomasse, Blöcke wachsen nach, fressende Flora) |

### So spielt sich Meilenstein 3

1. **Verbundplatten** in der Garagenpresse pressen (Mischmetall + Xenit-Staub + Fluxstaub), daraus **Labor-Gehäuse** bauen.
2. Zuerst **Präzisions-Montierer** und **Quantenzentrifuge** bauen – sie kommen mit Basis-Schaltkreisen aus.
   Die Zentrifuge macht aus Säuregel **Ätzlösung**, der Montierer daraus **Rift-Schaltkreise**
   (Slot 1 Basis-Schaltkreis, Slot 2 zwei Golddrähte, Slot 3 Ätzlösung, Slot 4 Fluxstaub).
3. **Flux-Leitungen** und einen **Umspanner** legen: Stufe-II-Generatoren grillen sonst die T1-Maschinen.
4. Die **Portalfluid-Raffinerie** als Multiblock aufbauen (siehe unten) und mit Roh-Portalfluid aus Kartuschen
   oder per Rohr direkt aus dem Fluid-Mixer füttern. Sie füllt **stabile Kartuschen**.
5. Mit stabilem Fluid springt die **Kanone Mk I** zufällig auch in die drei T2-Welten: dort Schwebstein,
   Chronoglas-Sand und Biomasse holen.
6. **Antigrav-Platten** in der Gravitationspresse, **Chronoglas-Scheiben** in der Xenit-Schmelze,
   **Flux-Stabilisator** in der Zentrifuge, dann **Stabilisierte Linse**, **Kanonenkern** und **Kanone Mk II** im Montierer.
7. **Universumsprobe** in einem Universum füllen, im **Dimensions-Scanner** mit Papier zur **Koordinatenkarte**
   machen und mit der Mk II gezielt zurückspringen.

### Multiblöcke

Der Steuerblock sitzt unten in der Mitte der Vorderseite, der Bau geht 3 Blöcke breit und 3 tief nach hinten.
Ein Rechtsklick auf den Steuerblock nennt das erste fehlende Teil mit Koordinaten.

- **Portalfluid-Raffinerie (3 × 3 × 4):** Boden und Deckel ganz aus Labor-Hülle; in den zwei Lagen dazwischen
  Labor-Hülle an den vier Ecken, Glas an den vier Seitenmitten, die Mitte bleibt frei.
- **Gravitations-Pendel (3 × 5 × 3):** Boden und Deckel aus Labor-Hülle, vier Eckpfosten aus Labor-Hülle
  (3 hoch), in der Mitte auf halber Höhe der Schwebstein-Kern.
- **Fluxkristall-Resonator:** Kern mit 3 Spalten à 3 Plätzen, jede angrenzende Resonator-Kammer (bis 6) gibt eine
  Spalte dazu. Stäbe schwingen mit ihren Nachbarn (20 FE/t pro Puls, 4 Hitze pro Puls), Kühlkörper führen 12 Hitze/t ab,
  Wasser bringt 1,5-fache Leistung. Bei 10.000 Hitze explodiert der Kristall; Redstone schaltet ab.

### Entscheidungen in Meilenstein 3

- **Henne-Ei-Problem gelöst:** Kanone Mk II braucht Material aus T2-Welten, die laut Dokument erst Mk II erreicht.
  Deshalb springt die Mk I mit **stabilisiertem Portalfluid** zufällig auch in die T2-Typen; gezielt geht es erst
  mit Mk II. Die Raffinerie lässt sich komplett aus T1-Material bauen.
- **Stabilisator als Kompat-Tag:** Die Stabilisierte Linse fragt `riftverse:compat/stabilisator` ab. Ohne Pack steht
  dort der Flux-Stabilisator (Quantenzentrifuge: Fluxkristall + Säuregel), im Pack kann KubeJS den Source-Gem eintragen.
- **Umspanner:** spannt herunter bis zu 4 Pakete à 128 FE/t pro Tick, hoch ein Paket à 1.024 FE/t. Die Seiten haben
  im Netz feste Rollen, Strom fließt also nie „rückwärts“ durch.
- **Präzisions-Montierer:** Die Schritte sind die Slots in fester Reihenfolge (oben links beginnend); die GUI zeigt
  den aktuellen Schritt. Verbraucht wird alles am Ende, abgebrochene Montagen verlieren nichts.
- **Chrono-Beschleuniger:** Die Rückseite zeigt zur Maschine, Rechtsklick wechselt die Stufe. Generatoren lassen sich
  nicht beschleunigen, Pflanzen schon. Er hat keine GUI und deshalb keinen Akku-Slot.
- **Labor-Hülle statt Labor-Gehäuse** in den Multiblöcken, weil 25–29 Labor-Gehäuse pro Bau zu teuer wären.
- **Schwebeinseln:** Um jede Zellmitte liegt eine große Heimatinsel, auf der die Rifts landen; der Rest sind
  verstreute Inseln über dem Nichts.
- **Zeitdilatation:** Drei zusätzliche Runden Zufallsticks rund um jeden Spieler (Radius 3 Chunks). Etwa alle
  anderthalb Minuten springen Spieler und Kreaturen in der Nähe auf ihre Position von vor bis zu fünf Sekunden zurück.
- **Organische Welt:** Nur Geländeblöcke (Tag `riftverse:waechst_nach`) wachsen nach 1–2 Minuten nach, die Liste
  übersteht einen Neustart.

## Meilenstein 2 – Spielbarer Prototyp

Ab jetzt lässt sich Riftverse im Überleben von der Garage bis zum ersten Sprung durchspielen.

| Bereich | Inhalt |
|---|---|
| **Meteoritenkrater** | Alle 900–1.100 Blöcke ein Krater in der Overworld (alle Biome außer Flüssen, im Ozean versunken am Meeresgrund): Schüssel mit Auswurfwall, 8–12 sichtbare Xenit-Erze, Meteoritenkern in der Mitte, darunter eine trichterförmige Einschlagsader aus 20–30 Erz |
| **Xenit-Zone** | Xenit-Erz im Eisen-Band (Y −24 bis 56) rund um jeden Krater; Dichte fällt linear von 100 % am Krater auf 0 bei 200 Blöcken |
| **Meteoritenpeiler** | Kompass, dessen Nadel zum nächsten Krater zeigt und in der Xenit-Zone grün wird |
| **Neue Rohstoffe** | Fluxkristall-Erz (Overworld unter Y 0, selten), Alien-Wracks mit Schrotthaufen (Overworld selten), Meteoritenkern |
| **Restliche T1-Maschinen** | Xenit-Schmelze (Xenit-Staub → Barren), Platinen-Ätzer (→ Basis-Schaltkreis), Kristall-Schleifer (Fluxkristall → Fluxlinse + Fluxstaub), Recycler (Alien-Schrott → Polymerfetzen, Drähte, Federn, Mini-Motoren), Fluid-Mixer (Fluxstaub + Wasser → Roh-Portalfluid) |
| **Xenit-Zelle** | Akku mit 1 Mio. FE auf Stufe I; behält ihre Ladung beim Abbauen, lädt und entlädt Items |
| **Portal-Kanone Mk I** | Rein zufällige Sprünge in die drei T1-Universumstypen, 250 mB Roh-Portalfluid pro Schuss aus Fluid-Kartuschen (1.000 mB = 4 Schüsse); Rückkehr zum Heimanker mit Taste R oder Schleichen + Rechtsklick |
| **Roh-Portalfluid** | Zerfällt 10 Minuten nach der Herstellung, im Tank wie in der Kartusche |
| **Universen** | Schrottwelt (Nebel, Müllberge, Wrackfelder mit Xenit, Schrott-Golems), Kristallwelt (immer hell, Fluxkristall-Cluster wachsen nach, Sprinten lässt sie zerspringen), toxische Welt (Gift ohne Helm, Säureseen mit Säuregel, Säureregen frisst Rüstung). Jedes Universum würfelt 1–3 Modifikatoren |
| **Rift-Anker und Aufräumen** | Universen ohne Bauten und ohne Rift-Anker, die 10 Ingame-Tage nicht besucht wurden, werden beim Serverstart zurückgesetzt; der Universums-Scanner zeigt den Zustand |
| **Prototyp** | Quantenerz, alte Rift-Kanone und Quanten-Maschinen sind entfernt. Bosse und Story-Generator bleiben für Phase 3 im Code, starten aber nicht mehr bei der Ankunft |

### So spielt sich Meilenstein 2

1. **Meteoritenpeiler** in der Garagen-Werkbank bauen (Kompass, 4 Kupferplatten, 2 Redstone) und dem
   grünen Zeiger folgen. Am Krater liegt Xenit offen, das meiste steckt im Eisen-Band darunter.
2. **Xenit-Gehäuse** bauen und damit die T1-Maschinen aus Meilenstein 1. Xenit-Erz mit der Erzmühle
   verdoppeln, den Staub in der **Xenit-Schmelze** zu Barren schmelzen.
3. **Alien-Wracks** suchen (rostige Rümpfe mit Schrotthaufen). Alien-Schrott in der Garagenpresse zu
   Schrottplatten pressen, im **Platinen-Ätzer** mit Redstone und Kupferdraht zu **Basis-Schaltkreisen** ätzen.
4. **Fluxkristall-Erz** unter Y 0 abbauen. Der **Kristall-Schleifer** macht daraus eine Fluxlinse und Fluxstaub.
5. Im **Fluid-Mixer** Fluxstaub mit Wasser zu Roh-Portalfluid mischen und **Fluid-Kartuschen** füllen.
   Das Fluid zerfällt nach 10 Minuten – erst füllen, wenn es losgehen soll.
6. **Portal-Kanone Mk I** an der Garagen-Werkbank bauen: Kanonengehäuse, Fluxlinse, 2 Basis-Schaltkreise,
   Fluid-Kartusche. Rechtsklick öffnet ein Rift in ein zufälliges Universum; volle Kartuschen im Inventar
   lädt die Kanone selbst nach. Zurück geht es mit **R** (oder Schleichen + Rechtsklick).
7. Ein **Rift-Anker** (4 Eisenplatten, Xenit-Stange, Fluxlinse) hält ein Universum, das man behalten will.

### Entscheidungen in Meilenstein 2

- **Universen als Raster:** Jeder Universumstyp ist eine Dimension (`riftverse:schrottwelt`, `riftverse:kristallwelt`,
  `riftverse:toxische_welt`), aufgeteilt in Zellen von 4.096 × 4.096 Blöcken mit Leere dazwischen. Jede Zelle ist ein
  Universum mit eigenem Seed, Gelände und Modifikatoren. Eine Zelle umfasst genau 8 × 8 Region-Dateien, deshalb kann
  das Aufräumen ein Universum löschen, ohne die Nachbarn zu berühren.
- **Modifikatoren:** Riesen- oder Mini-Mobs, doppelte Erze, keine Tiere, Dauergewitter (Blitze in der Nähe),
  supraleitend oder hochohmig (Kabelverlust 0 bzw. doppelt) und als Kuriosität hüpfende oder leuchtende Kreaturen.
  „Ewige Nacht“ fehlt noch, weil sich alle Universen eines Typs eine Dimension und damit eine Tageszeit teilen.
- **Atemschutz:** Bis das Atemfilter-Modul (Meilenstein 6) kommt, schützt jeder Helm vor der giftigen Luft.
  Säureregen fällt, wenn es in der Overworld regnet.
- **Säuregel:** Glasflasche an einer Säurequelle füllen. Gebraucht wird es ab dem Rift-Labor (Meilenstein 3).
- **Kristall-Schleifer:** 1 Fluxkristall → 1 Fluxlinse + 2 Fluxstaub, damit ein Kristall zwei Kartuschen füllt.
- **Kanone Mk I:** Das Rezept nimmt eine leere oder volle Kartusche; die Kanone startet leer und lädt volle
  Kartuschen aus dem Inventar nach (die leere kommt zurück). Auch der Rücksprung kostet einen Schuss. Ein
  kostenloses Rückportal bei der Ankunft gibt es nicht mehr. Der Heimanker ist der Ort, von dem man zuletzt aus
  der Heimat-Realität gesprungen ist; ohne Anker geht es zum Weltspawn.
- **Schaltkreise in T1:** Die neuen Maschinen brauchen einen Basis-Schaltkreis, außer dem Platinen-Ätzer, der sie
  erst herstellt. Die gelöteten Rezepte aus Meilenstein 1 bleiben, damit niemand festhängt.
- **Isolierfolie:** Polymerfetzen kommen jetzt aus dem Recycler; der Tonnenreaktor wirft keine mehr aus.
- **Als „gebaut“ zählt:** eine Block-Entity oder mehr als 64 dauerhaft platzierte Blöcke (Fackeln, Gerüste und
  Leitern zählen nicht, Tag `riftverse:zaehlt_nicht_als_bau`). Wer einmal über die Schwelle kommt, bleibt bebaut.
- **Xenit-Zelle:** gibt Energie nur an Verbraucher ab, nicht an andere Speicher; so pumpen zwei Zellen keine Energie
  im Kreis.

## Meilenstein 1 – Fundament

| Bereich | Inhalt |
|---|---|
| **Metalle** | Zinnerz (Oberwelt, per Config abschaltbar), Rohes Zinn, Zinnbarren und -block; Staub, Platten, Drähte, Hülsen, Stangen; Bronze, Stahl, Mischmetall; Xenit-Erz, -Barren, -Platte, -Staub, -Stange |
| **T0 Garage** | Garagen-Werkbank mit Werkzeugslots für Hammer, Lötkolben und Lötzinn; Garagenpresse und Kurbel-Dynamo mit der Handkurbel |
| **T1 Maschinen** | Erzmühle (1 Erz → 2 Staub), Walzbank (Walzen, Schneiden, Ziehen), Legierungsofen (Bronze, Stahl), Tonnenreaktor (verbrennt jedes Item) |
| **Energienetz Stufe I** | Eigenes Riftverse-Netz in FE mit IC2-Regeln: Pakete mit Spannungsstufe, Kabelverlust 0,5 FE pro Block, Überspannung (explodieren, durchbrennen oder aus), blanke Kabel geben einen Schlag |
| **Kabel** | Kupferkabel blank (4 px) und isoliert (6 px), isolierte Kabel mit Farbstoff einfärbbar; unterschiedlich gefärbte Kabel verbinden sich nicht |
| **Kompatibilität** | Einspeiser (FE von jedem Mod rein) und Ausspeiser (normales FE raus); Maschinen zeigen ihren Füllstand nur lesbar an |
| **Akku** | Energiezelle (40.000 FE) als reines FE-Item; jede Maschine und jeder Generator hat einen Akku-Slot |
| **Test** | Kreativ-Quelle (nur Kreativmodus): unendliche Quelle, Rechtsklick wechselt Stufe I–IV, um Überspannung zu testen |

### So spielt sich Meilenstein 1

1. **Garagen-Werkbank** in der normalen Werkbank bauen (Eisen, Kupfer, Holzstämme, Werkbank).
2. In der Garagen-Werkbank **Bronzestaub** von Hand mischen (3 Kupfer + 1 Zinn → 2), im Ofen zu Bronze schmelzen.
3. **Garagen-Hammer** bauen und in den Werkzeugslot legen: 2 Barren → 1 Platte.
4. **Garagenpresse**, **Handkurbel** und **Kurbel-Dynamo** bauen. Mit der Handkurbel Rechtsklick gedrückt
   halten: Die Presse macht 1 Barren → 1 Platte, der Dynamo liefert 8 FE/t.
5. **Lötkolben** und **Lötzinn** (Zinnbarren + Hammer) in die Werkzeugslots legen – die T1-Maschinen werden gelötet.
6. T1-Maschinen brauchen das **Xenit-Gehäuse** aus Xenit vom Meteoritenkrater (siehe Meilenstein 2).

Rechtsklick mit leerer Hand auf ein Kabel zeigt das Teilnetz: Quellen, Verbraucher, Leitungen und übertragene FE/t.

### Entscheidungen in Meilenstein 1

Wo das Design-Dokument nichts festlegt oder sich widerspricht, gilt bis auf Weiteres:

- **Bronze in T0:** Der Legierungsofen ist T1, Bronze wird aber schon in T0 gebraucht. Deshalb gibt es eine
  Handmischung in der Garagen-Werkbank mit halber Ausbeute (3 Kupfer + 1 Zinn → 2 Bronzestaub);
  der Legierungsofen macht 3 + 1 → 4.
- **Akku-Slot:** Maschinen ziehen Strom aus der Energiezelle, Generatoren laden sie.
- **Lötzinn:** Zinnbarren + Hammer → 1 Lötzinn, in der Walzbank Zinnplatte → 3 Lötzinn.
- **Tonnenreaktor:** jedes Item brennt 50 Ticks, Schrott 100 Ticks, Brennstoffe die Hälfte ihrer Ofen-Brenndauer;
  Kohle bringt so 32.000 FE.
- **Garagenpresse:** braucht 40 Kurbel-Ticks pro Platte (2 Sekunden Kurbeln). Create-Rotation folgt in Phase 2.
- **Kabel verkleiden** (Rechtsklick mit einem Block) ist noch nicht drin.

## Config

`config/riftverse-common.toml`:

| Option | Standard | Wirkung |
|---|---|---|
| `energie.overvoltage` | `EXPLODE` | `EXPLODE`, `BURNOUT` oder `OFF` |
| `energie.cable_shock` | `true` | Schlag an blanken Kabeln unter Strom |
| `welt.tin_ore` | `true` | Zinnerz generieren (aus, wenn ein anderer Mod Zinn liefert) |
| `universen.aufraeumen` | `true` | Unbebaute, lange nicht besuchte Universen beim Serverstart zurücksetzen |
| `universen.tage` | `10` | Ingame-Tage ohne Besuch bis zum Zurücksetzen |
| `universen.bloecke` | `64` | Ab mehr als so vielen platzierten Blöcken gilt ein Universum als bebaut |
| `story.boss_bei_ankunft` | `false` | Prototyp: Boss kurz nach der Ankunft starten (bis zum Story-Modus aus) |

## Bauen und Starten

Voraussetzung: **JDK 21**, z. B. [Eclipse Temurin 21](https://adoptium.net/).

```
.\gradlew.bat runClient     # Windows: Testclient starten
./gradlew build             # Jar unter build/libs/
```

Bei jedem Push baut GitHub Actions den Mod (`.github/workflows/build.yml`). Ein Meilenstein ist erst fertig,
wenn dieser Check grün ist; das fertige Jar liegt als Artefakt am Workflow-Lauf.

## Projektstruktur

```
src/main/java/dev/riftverse/
  energy/       Riftverse-Netz: Graph, Pakete, Spannungsstufen, Überspannung, Ein-/Ausspeiser, Speicher
  cable/        Kupferkabel blank und isoliert
  tech/         Maschinen, Generatoren, Fluid-Mixer, Speicher, Garagen-Werkbank, T3-Geräte (+ menu/ für die GUIs)
  recipe/       Rezepttypen der Maschinen, des Fluid-Mixers und der Garagen-Werkbank
  universe/     Universumstypen, Modifikatoren, Gelände, Physik und Gefahren, Aufräumen
  worldgen/     Meteoritenkrater, Xenit-Zone, Alien-Wracks, per Config abschaltbare Erze
  item/         Portal-Kanone, Fluid-Kartusche, Meteoritenpeiler, Universums-Scanner
  block/        Meteoritenkern, Fluxkristalle, Säure, Rift-Anker
  fluid/        Zerfall von Roh-Portalfluid, Säuregel aus der Glasflasche
  portal/       Rift-Portal, Reise, Heimanker
  network/      Rückkehr-Taste, Ziel-Taste der Kanone Mk III
  mixin/        Vertauschte Tageszeit der Spiegelwelt
  entity/       Schrott-Golem
  client/       Screens (ohne Texturdateien gezeichnet), Kabelfarben, Peiler-Nadel, Nebel, Tasten
  registry/     Registrierung aller Blöcke, Items, Fluids, Rezepttypen …
  boss/, story/ Bosse und Story-Generator aus dem ersten Entwurf (ruhen bis Phase 3)
src/main/resources/
  assets/riftverse/   Texturen, Modelle, Blockstates, Sprachen
  data/riftverse/     Rezepte (pro Maschine ein Ordner), Loot, Worldgen, Dimensionen, Biome, Tags
tools/
  texturen/   Texturgenerator (Python, Pillow + NumPy); meilenstein1.py bis meilenstein4.py ergänzen die neuen Texturen
  daten/      generate.py erzeugt Modelle, Blockstates, Loot, Tags, Rezepte, Worldgen, Dimensionen und Sprachen
```

Rezepte ändern: in `tools/daten/generate.py` eintragen und `python3 tools/daten/generate.py` laufen lassen.
Rezepte sind JSON im Datapack, jede Maschine hat ihren eigenen Rezepttyp, z. B.

```json
{
  "type": "riftverse:legierungsofen",
  "ingredients": [
    {"ingredient": {"tag": "c:ingots/copper"}, "count": 3},
    {"ingredient": {"tag": "c:ingots/tin"}}
  ],
  "result": {"id": "riftverse:bronze_ingot", "count": 4},
  "time": 200
}
```

Rezepte der Garagen-Werkbank haben die Typen `riftverse:werkbank_shaped` und `riftverse:werkbank_shapeless`
mit den Zusatzfeldern `hammer` (Haltbarkeit pro Craft) und `loetzinn` (verbrauchtes Lötzinn).

Maschinen mit mehreren Ausgängen lesen `byproducts`, der Fluid-Mixer hat eigene Fluid-Felder:

```json
{
  "type": "riftverse:kristall_schleifer",
  "ingredients": [{"ingredient": {"tag": "c:gems/fluxkristall"}}],
  "result": {"id": "riftverse:fluxlinse"},
  "byproducts": [{"result": {"id": "riftverse:fluxstaub", "count": 2}, "chance": 1.0}],
  "time": 160
}
```

```json
{
  "type": "riftverse:fluid_mixer",
  "ingredients": [{"ingredient": {"tag": "c:dusts/fluxkristall"}}],
  "fluid": {"fluid": "minecraft:water", "amount": 1000},
  "result": {"id": "riftverse:roh_portalfluid", "amount": 1000},
  "time": 200
}
```
## Made By David Chapman

## Lizenz

Dieses Projekt ist unter der [CC BY-NC-SA 4.0](https://creativecommons.org/licenses/by-nc-sa/4.0/) lizenziert.

Sie sind frei und berechtigt:
- Teilen: Das Material in jedwedem Format oder Medium zu vervielfältigen und weiterzuverbreiten.
- Bearbeiten: Das Material zu remixen, zu verändern und darauf aufzubauen.

Unter folgenden Bedingungen:
1. Namensnennung (Attribution): Sie müssen angemessene Urheber- und Rechteangaben machen, einen Link zur Lizenz beifügen und angeben, ob Änderungen vorgenommen wurden.
2. Nicht-kommerziell (Non-Commercial): Sie dürfen das Material nicht für kommerzielle Zwecke nutzen.
3. Weitergabe unter gleichen Bedingungen (ShareAlike): Wenn Sie das Material remixen, verändern oder anderweitig direkt darauf aufbauen, müssen Sie Ihre Beiträge unter derselben Lizenz wie das Original veröffentlichen.
