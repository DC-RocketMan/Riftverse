"""Multiversal-Rüstung: Item-Icons (3 Ausbaustufen), Rüstungs-Layer 64x32 und Modul-Icons."""
from lib import Tex, R, T, H, A, OUT, BLACK, rng_for
from gadgets import spr

G, P, W, GR, ST, V = R['green'], R['petrol'], R['white'], R['graphite'], R['steel'], R['violet']

STAGES = {
    'rohling':        dict(plate=ST, coat=[W[0], W[1], W[2], W[2], W[3]], glow=(G[1], G[2]), trace=None, stars=False, name='Rohling'),
    'kalibriert':     dict(plate=GR, coat=W, glow=(G[2], G[3]), trace=P[3], stars=False, name='Kalibriert'),
    'perfektioniert': dict(plate=GR, coat=W, glow=(G[3], G[4]), trace=P[4], stars=True, name='Perfektioniert'),
}

# --- Item-Icons ----------------------------------------------------------------
HELM = [
    "................",
    "................",
    ".....oooooo.....",
    "....oPPPPPpo....",
    "...oPPpppppqo...",
    "..oPpppppppqqo..",
    "..oLLvvvvvvvqo..",
    "..oLlvvvvvvvqo..",
    "..ogvvvvvvvvqo..",
    "..opppptppppqo..",
    "..opqo....opqo..",
    "..opqo....opqo..",
    "..oqqo....oqqo..",
    "...oo......oo...",
]
BRUST = [
    "................",
    "..ooo......ooo..",
    ".oPPPoooooopPPo.",
    ".oPppCCCCCCpppo.",
    ".oPpppCCCCpppqo.",
    ".oppptpggptppqo.",
    ".ooppppgGgppqoo.",
    "..oppppgggppqo..",
    "..oCppptpppqCo..",
    "..oCCpppppqCCo..",
    "..oCkqqqqqqkCo..",
    "..oCko....okCo..",
    "..oCko....okCo..",
    "...oo......oo...",
]
HOSE = [
    "................",
    "................",
    "...oooooooooo...",
    "...oPPPgPPPPo...",
    "...opppppppqo...",
    "...oppptoppqo...",
    "...oSppo.oSpo...",
    "...oSPpo.oSpo...",
    "...oSppo.oSpo...",
    "...oSPpo.oSPo...",
    "...oSppo.oSpo...",
    "...oSpqo.oSqo...",
    "...oqqqo.oqqo...",
    "....ooo...oo....",
]
STIEFEL = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "....ooo...ooo...",
    "...oPpo..oPpo...",
    "...optoo.optoo..",
    "...oppqo.oppqo..",
    "..oPpppqooPpppqo",
    ".oPppppqoPppppqo",
    ".ogggggGogggggGo",
    ".oqqqqqqoqqqqqqo",
    "..oooooo.oooooo.",
]


def icon(rows, stage):
    s = STAGES[stage]
    pl, co = s['plate'], s['coat']
    lg = {'o': OUT, 'P': pl[4], 'p': pl[2], 'q': pl[1], 'C': co[4], 'k': co[1],
          'g': s['glow'][0], 'G': s['glow'][1], 'v': H('#0a0c10'), 'L': s['glow'][1], 'l': s['glow'][0],
          't': s['trace'] or pl[3], 'S': ST[3]}
    t = spr(rows, lg)
    if s['stars']:
        rng = rng_for('armor_icon' + ''.join(rows))
        cands = [(x, y) for y in range(16) for x in range(16) if t.get(x, y) == tuple(pl[2])]
        for x, y in rng.sample(cands, min(4, len(cands))):
            t.p(x, y, H('#ffffff'))
    if stage == 'rohling':   # Nieten
        cands = [(x, y) for y in range(16) for x in range(16) if t.get(x, y) == tuple(pl[2])]
        for x, y in cands[::7]:
            t.p(x, y, ST[4])
    return t


# --- Rüstungs-Layer (Spieler-UV-Layout 64x32) ---------------------------------------
def fill(t, x, y, w, h, c):
    t.rect(x, y, x + w - 1, y + h - 1, c)


def plate(t, x, y, w, h, s, rng):
    pl = s['plate']
    for yy in range(y, y + h):
        for xx in range(x, x + w):
            t.p(xx, yy, pl[rng.choice([2, 2, 2, 3, 1])])
    for xx in range(x, x + w):
        t.p(xx, y, pl[3])
    if s['stars']:
        for _ in range(max(1, w * h // 14)):
            t.p(rng.randrange(x, x + w), rng.randrange(y, y + h), H('#ffffff'))
    if s['trace']:
        t.p(x + w // 2, y + h - 1, s['trace'])
        if h > 3:
            t.p(x + w // 2, y + h - 2, s['trace'])


def coat(t, x, y, w, h, s):
    co = s['coat']
    for yy in range(y, y + h):
        for xx in range(x, x + w):
            t.p(xx, yy, co[3] if (xx + yy) % 5 else co[2])
    for xx in range(x, x + w):
        t.p(xx, y + h - 1, co[1])


def layer1(stage):
    s = STAGES[stage]
    rng = rng_for('l1' + stage)
    t = Tex(64, 32)
    gl, GL = s['glow']
    # Helm: Kopf-Box bei (0,0), 8x8x8
    plate(t, 8, 0, 8, 8, s, rng)                       # oben
    t.rect(11, 0, 12, 7, s['trace'] or s['plate'][3]); t.p(11, 3, GL)
    plate(t, 8, 8, 8, 8, s, rng)                       # vorne
    fill(t, 8, 10, 8, 3, H('#0a0c10'))                 # Visier
    t.rect(9, 10, 10, 11, GL); t.p(9, 12, gl)          # Scanner-Linse (Träger rechts = Bild links)
    t.a[14:16, 10:14] = 0                              # Mund und Kinn frei
    plate(t, 0, 8, 8, 8, s, rng); t.rect(3, 11, 4, 12, s['plate'][4]); t.a[14:16, 0:4] = 0
    plate(t, 16, 8, 8, 8, s, rng); t.rect(19, 11, 20, 12, s['plate'][4]); t.a[14:16, 20:24] = 0
    plate(t, 24, 8, 8, 8, s, rng); t.rect(27, 9, 28, 15, s['plate'][1])
    # Brust: Körper-Box bei (16,16), 8x12x4
    plate(t, 20, 16, 8, 4, s, rng); t.rect(22, 16, 25, 17, s['coat'][4])      # Kragen
    plate(t, 20, 20, 8, 8, s, rng)                                              # Front oben
    t.rect(23, 23, 24, 25, gl); t.rect(23, 24, 24, 24, GL)                      # Energiekern
    t.p(22, 24, gl); t.p(25, 24, gl)
    coat(t, 20, 28, 2, 4, s); coat(t, 26, 28, 2, 4, s)                          # Mantel offen
    fill(t, 22, 28, 4, 4, s['plate'][1])
    coat(t, 16, 20, 4, 12, s); plate(t, 16, 20, 4, 3, s, rng)                   # Seiten
    coat(t, 28, 20, 4, 12, s); plate(t, 28, 20, 4, 3, s, rng)
    coat(t, 32, 20, 8, 12, s); plate(t, 32, 20, 8, 2, s, rng)                   # Rücken
    t.rect(35, 24, 36, 31, s['coat'][1])
    # Arme: (40,16), 4x12x4
    plate(t, 44, 16, 4, 4, s, rng)
    for fx in (40, 44, 48, 52):
        coat(t, fx, 20, 4, 12, s)
        plate(t, fx, 20, 4, 4, s, rng)                                          # Schulterplatte
        fill(t, fx, 30, 4, 2, s['plate'][1]); t.rect(fx, 30, fx + 3, 30, gl)    # Manschette
    # Stiefel: Bein-Box bei (0,16), 4x12x4 – nur unterer Teil
    plate(t, 8, 16, 4, 4, s, rng)                                               # Sohle
    fill(t, 8, 16, 4, 4, s['plate'][0])
    for fx in (0, 4, 8, 12):
        plate(t, fx, 26, 4, 6, s, rng)
        t.rect(fx, 31, fx + 3, 31, GL)                                          # Leuchtsohle
        t.rect(fx, 26, fx + 3, 26, s['plate'][4])
    return t


def layer2(stage):
    s = STAGES[stage]
    rng = rng_for('l2' + stage)
    t = Tex(64, 32)
    gl, GL = s['glow']
    # Gürtel im Körperbereich
    for fx, fw in ((16, 4), (20, 8), (28, 4), (32, 8)):
        fill(t, fx, 28, fw, 4, s['plate'][1])
        t.rect(fx, 28, fx + fw - 1, 28, s['plate'][3])
    t.rect(23, 29, 24, 30, GL)                                                   # Schnalle
    fill(t, 20, 16, 8, 4, T)
    # Beine: (0,16), obere 9 Reihen
    plate(t, 4, 16, 4, 4, s, rng)
    for fx in (0, 4, 8, 12):
        plate(t, fx, 20, 4, 9, s, rng)
    t.rect(0, 21, 0, 28, ST[3]); t.rect(1, 22, 1, 27, ST[1])                     # Hydraulik außen
    t.rect(4, 25, 7, 26, s['plate'][4]); t.p(5, 25, gl)                          # Knieschutz
    fill(t, 9, 21, 2, 3, R['leather'][2]); t.p(9, 21, R['leather'][4])           # Holster
    return t


# --- Modul-Icons ------------------------------------------------------------------
SLOT = {'helm': R['cyan'], 'brust': G, 'hose': R['orange'], 'stiefel': V}
GLYPH = {
    'hud':       ["#...#", ".#.#.", "..#..", ".#.#.", "#...#"],
    'auge':      [".###.", "#...#", "#.#.#", "#...#", ".###."],
    'filter':    ["#####", "#.#.#", "#####", "#.#.#", "#####"],
    'tarn':      ["##.##", "#...#", ".....", "#...#", "##.##"],
    'sprache':   ["####.", "#..#.", "####.", "#....", "....."],
    'schild':    ["#####", "#...#", "#...#", ".#.#.", "..#.."],
    'portal':    [".###.", "#.#.#", "##.##", "#.#.#", ".###."],
    'kanone':    ["###..", "#####", "###.#", ".#...", ".#..."],
    'klon':      [".#...", "###..", ".#.#.", "..###", "...#."],
    'halter':    ["#.#.#", "#####", ".....", "#####", "#.#.#"],
    'servo':     ["..#..", ".###.", "#####", "..#..", "..#.."],
    'sprint':    ["#..#.", ".#..#", "..#..", ".#..#", "#..#."],
    'grav':      ["#####", ".....", "..#..", ".###.", "#####"],
    'schrumpf':  ["#...#", ".#.#.", "..#..", ".#.#.", "#...#"][::-1],
    'fall':      ["..#..", "..#..", "#.#.#", ".###.", "#####"],
    'feder':     ["#####", ".#...", "..#..", "...#.", "#####"],
    'phase':     ["#.#..", ".#.#.", "..#.#", ".#.#.", "#.#.."],
    'haft':      ["#.#.#", "#.#.#", "#####", ".....", "#####"],
}
MODULES = [
    ('analyse_hud', 'helm', 'hud', 'Modul: Analyse-HUD'),
    ('nachtsicht_linse', 'helm', 'auge', 'Modul: Nachtsicht-Linse'),
    ('atemfilter', 'helm', 'filter', 'Modul: Atemfilter'),
    ('tarn_detektor_modul', 'helm', 'tarn', 'Modul: Tarn-Detektor'),
    ('universal_uebersetzer', 'helm', 'sprache', 'Modul: Universal-Übersetzer'),
    ('energieschild', 'brust', 'schild', 'Modul: Energieschild'),
    ('not_portal', 'brust', 'portal', 'Modul: Not-Portal'),
    ('kanonen_integration', 'brust', 'kanone', 'Modul: Kanonen-Integration'),
    ('klon_link', 'brust', 'klon', 'Modul: Klon-Link'),
    ('gadget_halterung', 'brust', 'halter', 'Modul: Gadget-Halterung'),
    ('servo_verstaerker', 'hose', 'servo', 'Modul: Servo-Verstärker'),
    ('sprint_booster', 'hose', 'sprint', 'Modul: Sprint-Booster'),
    ('gravitationsregler', 'hose', 'grav', 'Modul: Gravitationsregler'),
    ('schrumpf_modul', 'hose', 'schrumpf', 'Modul: Schrumpf-Modul'),
    ('fall_daempfer', 'stiefel', 'fall', 'Modul: Fall-Dämpfer'),
    ('sprungfeder', 'stiefel', 'feder', 'Modul: Sprungfeder'),
    ('phasenschritt', 'stiefel', 'phase', 'Modul: Phasenschritt'),
    ('haftsohlen', 'stiefel', 'haft', 'Modul: Haftsohlen'),
]


def module_icon(slot, glyph):
    c = SLOT[slot]
    t = Tex()
    # sechseckige Patrone
    for y in range(2, 14):
        inset = max(0, 4 - y) if y < 4 else max(0, y - 11)
        t.rect(2 + inset, y, 13 - inset, y, GR[2])
    t.rect(4, 2, 11, 2, GR[4])
    for y in range(4, 12):
        t.p(2, y, GR[3]); t.p(13, y, GR[1])
    t.rect(4, 4, 11, 11, BLACK)
    t.frame(4, 4, 11, 11, c[1])
    for gy, row in enumerate(GLYPH[glyph]):
        for gx, ch in enumerate(row):
            if ch == '#':
                t.p(5 + gx + 1 - 1, 5 + gy + 1 - 1, c[3] if gy < 2 else c[2])
    t.p(6, 13, R['brass'][3]); t.p(9, 13, R['brass'][3])
    t.outline()
    return t


def build(pk):
    pieces = [('analyse_visier', HELM, 'Analyse-Visier'), ('kernmantel', BRUST, 'Kernmantel'),
              ('exo_rahmen', HOSE, 'Exo-Rahmen'), ('gravi_stiefel', STIEFEL, 'Gravi-Stiefel')]
    for stage, s in STAGES.items():
        for pid, rows, name in pieces:
            tid = f'{pid}_{stage}'
            pk.add('Rüstung', 'item', tid, icon(rows, stage), f'{name} ({s["name"]})',
                   lang_key=f'item.riftverse.{tid}')
    for stage, s in STAGES.items():
        pk.add('Rüstungs-Layer', 'models/armor', f'multiversal_{stage}_layer_1', layer1(stage),
               f'Layer 1 – {s["name"]} (Helm, Brust, Stiefel)')
        pk.add('Rüstungs-Layer', 'models/armor', f'multiversal_{stage}_layer_2', layer2(stage),
               f'Layer 2 – {s["name"]} (Hose)')
    for tid, slot, glyph, name in MODULES:
        pk.add('Rüstungsmodule', 'item', 'modul_' + tid, module_icon(slot, glyph), name,
               lang_key=f'item.riftverse.modul_{tid}')
