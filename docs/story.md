# Storylines schreiben

Storylines sind Geschichten in den **einzigartigen Universen** (Magisches Dorf, Lumora, Verlassene Minen …). Jede
Storyline ist eine JSON-Datei im Datapack. Du schreibst Texte, Orte und Abläufe, der Mod erledigt den Rest.

- **Ort der Dateien:** `src/main/resources/data/riftverse/riftverse_story/<welt>/<name>.json`
  (in einem eigenen Datapack: `data/<namespace>/riftverse_story/...`)
- **Id der Storyline:** `riftverse:<welt>/<name>`, z. B. `riftverse:test_einzigartig/kartograf`
- **Neu laden ohne Neustart:** `/reload`
- **Prüfen:** `python3 tools/story/pruefen.py` (läuft auch in GitHub Actions und meldet Tippfehler)
- **Beispiel zum Abschauen:** `data/riftverse/riftverse_story/test_einzigartig/kartograf.json`

## Aufbau

```json
{
  "welt": "magisches_dorf",
  "titel": "Der verlorene Lehrling",
  "beschreibung": "Steht im Rift-Tagebuch unter dem Titel.",
  "geltung": "welt",
  "fortschritt": "riftverse:story/lehrling",
  "figuren": { ... },
  "schritte": [ ... ]
}
```

| Feld | Pflicht | Bedeutung |
|---|---|---|
| `welt` | ja | Id eines einzigartigen Universums (`test_einzigartig`, später `magisches_dorf`, `lumora`, `verlassene_minen`) |
| `titel` | ja | Name der Geschichte (Tagebuch, Ziel-Anzeige, Abschluss) |
| `beschreibung` | nein | Kurzer Text im Tagebuch |
| `geltung` | nein | `welt` (Standard): Stand gilt für alle Spieler. `spieler`: jeder hat seinen eigenen Stand |
| `fortschritt` | nein | Advancement, das beim Abschluss vergeben wird (Datei unter `data/riftverse/advancement/…`, Kriterium `abgeschlossen` mit Trigger `minecraft:impossible`) |
| `start` | nein | Id des ersten Schritts (Standard: der erste in der Liste) |

## Figuren

```json
"figuren": {
  "mira": {
    "name": "Mira",
    "aussehen": "alex",
    "ort": [2132, 2032],
    "laufen": true,
    "radius": 5,
    "sofort": false,
    "standard": "Hast du schon eine leere Karte für mich?"
  }
}
```

| Feld | Bedeutung |
|---|---|
| `name` | Name über dem Kopf und im Dialog |
| `aussehen` | `steve`, `alex`, `ari`, `efe`, `kai`, `makena`, `noor`, `sunny`, `zuri` oder ein Texturpfad wie `riftverse:textures/entity/figur/mira.png` (64 × 64, Spieler-Skin-Format). Mit `gleeb` oder `gleeb:<beruf>` sieht die Figur aus wie ein Gleeb aus Bridge's Inn (`buerger`, `baeckerin`, `kraeuterhexe`, `alchemist`, `runenschmied`, `sternenkundlerin`) |
| `ort` | `[x, z]` (steht auf dem Boden) oder `[x, y, z]` |
| `laufen` | läuft im `radius` um ihren Platz herum (Standard: nein) |
| `sofort` | steht schon da, wenn die Welt zum ersten Mal betreten wird (Standard: ja). Mit `false` erscheint sie erst per Aktion `figur` |
| `standard` | Satz, wenn man sie anspricht und gerade nichts zu tun ist |

## Schritte

Die Storyline steht immer auf genau einem Schritt. Der wartet auf einen seiner **Auslöser**, führt dann seine
**Aktionen** aus und geht weiter.

```json
{
  "id": "lager",
  "ziel": "Folge der Spur nach Osten zum Lager",
  "ausloeser": { "typ": "bereich", "pos": [2132, 2032], "radius": 10 },
  "bedingung": { "flags": ["brief_gelesen"], "nicht": ["lager_verbrannt"] },
  "aktionen": [ ... ],
  "weiter": "gespraech"
}
```

| Feld | Bedeutung |
|---|---|
| `id` | Name des Schritts (für `weiter`, Sprünge und Testbefehle) |
| `ziel` | Text oben rechts, solange dieser Schritt aktiv ist (leer = keine Anzeige) |
| `ausloeser` | ein Auslöser oder eine Liste; **einer** reicht |
| `bedingung` | optional: Flags, die gesetzt (`flags`) bzw. nicht gesetzt (`nicht`) sein müssen |
| `aktionen` | was passiert, der Reihe nach |
| `weiter` | nächster Schritt. Ohne `weiter` geht es mit dem nächsten Schritt der Liste weiter; nach dem letzten ist die Geschichte fertig. Für einen Schritt, der sich wiederholen soll: `"weiter": "<eigene id>"` |
| `abschluss` | `true`: Nach diesem Schritt ist die Geschichte abgeschlossen |

## Auslöser

| `typ` | Felder | Wann |
|---|---|---|
| `ankunft` | – | bei jeder Ankunft in der Welt |
| `bereich` | `pos` (`[x, z]` oder `[x, y, z]`), `radius` (Standard 6) | Spieler ist höchstens `radius` Blöcke waagerecht entfernt |
| `item` | `item`, `anzahl` (Standard 1) | Spieler hat das Item im Inventar |
| `abgeben` | `figur`, `item`, `anzahl` | Rechtsklick auf die Figur mit dem Item in der Hand; es wird verbraucht |
| `besiegt` | `mob`, `anzahl` | so viele Mobs in diesem Schritt besiegt (`minecraft:zombie`) |
| `reden` | `figur` | Rechtsklick auf die Figur |
| `block` | `pos` (`[x, y, z]`) | Rechtsklick auf genau diesen Block |
| `notiz_gelesen` | `notiz` | die Notiz wurde im Rift-Tagebuch geöffnet |
| `zeit` | `wann` (`tag` oder `nacht`) | Tageszeit |
| `flag` | `flag` | das Flag ist gesetzt |
| `antwort` | `antwort` | im Dialog wurde die Antwort mit dieser `id` gewählt |

Zustände (Bereich, Item, Zeit, Flag, Notiz, Antwort) prüft der Mod einmal pro Sekunde.

## Aktionen

| `typ` | Felder | Was passiert |
|---|---|---|
| `notiz` | `id`, `titel`, `text`, `geltung` (optional) | neue Seite im Rift-Tagebuch |
| `dialog` | `figur`, `zeilen` | Gespräch über der Figur (siehe unten). Alles danach läuft erst, wenn der Dialog zu Ende ist |
| `titel` | `titel`, `untertitel` | großer Text in der Bildschirmmitte |
| `nachricht` | `text` | Zeile im Chat |
| `item` | `item`, `anzahl`, `name` (optional) | Item geben, optional mit eigenem Namen |
| `xp` | `menge` | Erfahrungspunkte |
| `sound` | `sound`, `lautstaerke`, `tonhoehe` | Geräusch, z. B. `minecraft:entity.player.levelup` |
| `flag` | `flag`, `wert` (Standard `true`), `geltung` (optional) | Flag setzen oder mit `"wert": false` löschen |
| `figur` | `figur`, `aktion` (`erscheinen`/`verschwinden`), `ort` (optional) | Figur erscheinen lassen (optional an einem anderen Ort) oder entfernen |
| `mob` | `mob`, `ort`, `anzahl` | Mobs erscheinen lassen |
| `bloecke` | `bloecke` | Blöcke setzen – die Welt verändert sich dauerhaft (siehe unten) |
| `schritt` | `schritt` | zu einem anderen Schritt springen |
| `abschluss` | – | Geschichte abschließen |

**Blöcke:** Jeder Eintrag ist entweder `{ "pos": [x, z], "ueber_boden": 0, "block": "…" }` (auf dem Boden, `ueber_boden`
Blöcke höher), `{ "pos": [x, y, z], "block": "…" }` oder ein Quader `{ "von": [x, y, z], "bis": [x, y, z], "block": "…" }`.
Der Boden wird für alle Einträge einer Aktion vor dem Setzen bestimmt, so lassen sich Türme stapeln.

## Dialoge

```json
{
  "typ": "dialog",
  "figur": "mira",
  "zeilen": [
    "Eine einfache Zeile.",
    { "text": "Nur, wenn das Flag gesetzt ist.", "wenn": "hilft" },
    { "text": "Nur, wenn es NICHT gesetzt ist.", "wenn": "!hilft" },
    { "text": "Ein anderer spricht.", "sprecher": "Eldric (aus der Ferne)" },
    {
      "text": "Hilfst du mir?",
      "antworten": [
        { "text": "Natürlich.", "id": "hilft", "flag": "hilft", "schritt": "karte" },
        { "text": "Später.", "aktionen": [ { "typ": "nachricht", "text": "Mira seufzt." } ] }
      ]
    }
  ]
}
```

- Der Text schwebt über der Figur, nur für den Spieler im Gespräch. **Rechtsklick** zeigt die nächste Zeile.
- **Antworten** (höchstens 4) wählt man mit den Tasten **1–4**, während man die Figur ansieht.
- Eine Antwort kann `flag` setzen, eine `id` haben (für den Auslöser `antwort`), eigene `aktionen` ausführen und mit
  `schritt` springen. **Mit** `schritt` endet der Dialog sofort, **ohne** geht er mit der nächsten Zeile weiter.
- Wer sich mehr als 10 Blöcke entfernt, beendet das Gespräch. Es beginnt beim nächsten Ansprechen von vorn, und der
  Schritt geht erst weiter, wenn ein Dialog zu Ende ist.

## Flags und Geltung

Flags sind Merker wie `hilft` oder `lager_verbrannt`. Mit ihnen verzweigt eine Geschichte: über `bedingung` an Schritten,
`wenn` an Dialogzeilen und den Auslöser `flag`.

Standardmäßig liegen Schritt, Flags und Notizen bei der **Welt** und gelten für alle Spieler. Mit `"geltung": "spieler"`
an der Storyline hat jeder Spieler seinen eigenen Stand. Einzelne `flag`- und `notiz`-Aktionen können mit
`"geltung": "spieler"` (oder `"welt"`) davon abweichen – so lassen sich gemeinsame Ereignisse und persönliche Notizen
mischen.

## Testen

| Befehl | Wirkung |
|---|---|
| `/riftverse testwelt` | Rift in den Prüfstein (Config `entwicklung.testwelten = true`, dann Welt neu laden) |
| `/riftverse story status` | alle Storylines mit Schritt, Flags, Notizen und kaputten Dateien |
| `/riftverse story schritt <storyline> <schritt>` | direkt zu einem Schritt springen |
| `/riftverse story flag <storyline> <flag> true\|false` | Flag setzen oder löschen |
| `/riftverse story zuruecksetzen <storyline>` | Geschichte von vorn, Figuren weg |

Koordinaten in einer einzigartigen Welt: Die Welt ist die Zelle 0,0, ihre Mitte liegt bei x = 2048, z = 2048.
Mit F3 siehst du im Spiel, wo du stehst.
