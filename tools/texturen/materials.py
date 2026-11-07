"""Materialien, Erze, Fluids und Bauteile."""
import math

from lib import Tex, R, T, H, A, OUT, BLACK, rng_for

G, P, W, GR = R['green'], R['petrol'], R['white'], R['graphite']


# =============================================================================
# Blöcke
# =============================================================================
def stone_base(name, ramp, w=(0.07, 0.24, 0.42, 0.22, 0.05)):
    t = Tex()
    rng = rng_for(name)
    t.noise(ramp, w, rng, clump=0.45)
    for _ in range(7):
        x, y = rng.randrange(16), rng.randrange(16)
        t.p(x, y, ramp[0])
        t.p(x + 1, y, ramp[1])
    return t


CLUSTERS = [(2, 2), (9, 1), (5, 7), (12, 8), (1, 12), (8, 12)]
SHAPES = [
    [(0, 0), (1, 0), (1, 1), (2, 1)],
    [(0, 0), (0, 1), (1, 1)],
    [(1, 0), (0, 1), (1, 1), (2, 1), (1, 2)],
    [(0, 0), (1, 0), (2, 1)],
]


def ore(name, base_ramp, vein, glow, n=5, glow_count=3):
    t = stone_base(name, base_ramp)
    rng = rng_for(name, 1)
    pts = []
    for i, (cx, cy) in enumerate(CLUSTERS[:n]):
        shape = SHAPES[i % len(SHAPES)]
        for dx, dy in shape:
            x, y = cx + dx, cy + dy
            t.p(x, y, vein[2])
            t.p(x + 1, y + 1, vein[0])
            pts.append((x, y))
        t.p(cx + shape[0][0], cy + shape[0][1], vein[3])
    for x, y in rng.sample(pts, glow_count):
        t.p(x, y, glow)
    return t


def xenit_block():
    t = Tex(fill=P[2])
    rng = rng_for('xenit_block')
    t.noise(P, (0, 0.15, 0.6, 0.25, 0), rng, 1, 1, 14, 14, clump=0.5)
    t.bevel(0, 0, 15, 15, P[4], P[0])
    t.bevel(1, 1, 14, 14, P[3], P[1])
    for x, y in [(3, 3), (12, 3), (3, 12), (12, 12)]:
        t.p(x, y, P[4])
        t.p(x, y + 1, P[0])
    # grüner Riss
    for x, y in [(6, 6), (7, 7), (8, 7), (9, 8), (8, 9), (7, 9)]:
        t.p(x, y, G[3])
    t.p(8, 8, G[4])
    return t


def schrott_haufen():
    t = Tex()
    rng = rng_for('schrott')
    t.noise(R['rust'], (0.1, 0.3, 0.35, 0.2, 0.05), rng, clump=0.3)
    for _ in range(6):   # Blechstücke
        x, y = rng.randrange(14), rng.randrange(14)
        c = rng.choice([R['steel'], P, R['rust']])
        t.rect(x, y, x + rng.randrange(1, 4), y + 1, c[3])
        t.rect(x, y + 2, x + 2, y + 2, c[1])
    for _ in range(3):   # Kabel
        x, y = rng.randrange(16), rng.randrange(16)
        t.line(x, y, x + 3, y + rng.randrange(-2, 3), R['red'][2])
    t.p(rng.randrange(16), rng.randrange(16), G[3])
    return t


def flux_block_frames():
    frames = []
    for f in range(8):
        k = 0.5 + 0.5 * math.sin(f / 8 * 2 * math.pi)
        t = Tex(fill=G[1])
        # Facetten
        for y in range(16):
            for x in range(16):
                fac = ((x + y) // 4 + (x - y + 16) // 5) % 3
                t.p(x, y, G[[1, 2, 2][fac]])
        for i in range(16):
            t.p(i, (i * 3) % 16, G[3])
        core = int(2 + round(k * 2))
        t.disc(7.5, 7.5, 2.6, G[core])
        t.disc(7.5, 7.5, 1.2, G[4] if k > 0.4 else G[3])
        t.bevel(0, 0, 15, 15, G[3], G[0])
        frames.append(t)
    return frames


def schwebstein():
    t = stone_base('schwebstein', R['mirror'], (0.08, 0.35, 0.4, 0.15, 0.02))
    for y0 in (3, 9, 14):
        for x in range(16):
            y = y0 + int(round(math.sin(x / 2.5 + y0) * 0.8))
            t.p(x, y, W[4])
            t.p(x, y + 1, W[1])
    return t


def chronoglas_frames():
    frames = []
    V = R['violet']
    for f in range(8):
        t = Tex()
        t.rect(1, 1, 14, 14, A(V[2], 110))
        t.frame(0, 0, 15, 15, V[1])
        t.p(1, 1, V[4]); t.p(2, 1, V[3]); t.p(1, 2, V[3])
        t.ring(7.5, 7.5, 5.2, A(V[3], 200), 0.5)
        for k in range(12):
            a = k / 12 * 2 * math.pi
            t.p(int(round(7.5 + 5.2 * math.cos(a))), int(round(7.5 + 5.2 * math.sin(a))), V[4])
        a = f / 8 * 2 * math.pi - math.pi / 2
        for r in range(0, 5):
            t.p(int(round(7.5 + r * math.cos(a))), int(round(7.5 + r * math.sin(a))), V[4])
        a2 = f / 32 * 2 * math.pi - math.pi / 2
        for r in range(0, 3):
            t.p(int(round(7.5 + r * math.cos(a2))), int(round(7.5 + r * math.sin(a2))), V[0])
        frames.append(t)
    return frames


def gravitit_ore():
    t = stone_base('gravitit_ore', R['grav'], (0.15, 0.35, 0.35, 0.15, 0))
    for cx, cy in [(4, 4), (11, 6), (6, 11), (12, 12)]:
        t.shaded_disc(cx, cy, 1.6, R['grav'], 1, 4)
        t.p(cx, cy + 2, BLACK)
    return t


def null_frames(size=16, seed='null', edge=True):
    frames = []
    rng = rng_for(seed)
    stars = [(rng.randrange(size), rng.randrange(size), rng.random()) for _ in range(size)]
    for f in range(8):
        t = Tex(size, size, fill=R['null'][1])
        rng2 = rng_for(seed, 7)
        t.noise(R['null'], (0.3, 0.45, 0.2, 0.05, 0), rng2, clump=0.5)
        for x, y, ph in stars:
            xx = (x + f // 2) % size
            b = 0.5 + 0.5 * math.sin((f / 8 + ph) * 2 * math.pi)
            t.p(xx, y, H('#ffffff') if b > 0.75 else R['null'][4] if b > 0.3 else R['null'][3])
        if edge:
            t.frame(0, 0, size - 1, size - 1, G[1])
            for x, y in [(0, 0), (size - 1, 0), (0, size - 1), (size - 1, size - 1)]:
                t.p(x, y, G[3])
        frames.append(t)
    return frames


def fluid_frames(ramp, size=16, flow=False, stars=False, n=16):
    frames = []
    rng = rng_for(f'fluid{ramp[2]}{flow}')
    sp = [(rng.randrange(size), rng.randrange(size)) for _ in range(6)]
    for f in range(n):
        t = Tex(size, size)
        ph = f / n * 2 * math.pi
        c = (size - 1) / 2
        for y in range(size):
            for x in range(size):
                if flow:
                    v = math.sin(x / size * 6.28 * 2 + ph) * 0.5 + math.sin((y / size * 6.28 * 2) - ph * 2 + x * 0.3)
                else:
                    dx, dy = (x - c) / size, (y - c) / size
                    r = (dx * dx + dy * dy) ** 0.5
                    ang = math.atan2(dy, dx)
                    v = math.sin(ang * 3 + r * 14 - ph) + 0.4 * math.sin(x * 0.9 + y * 0.6 + ph)
                i = int((v + 1.5) / 3 * 4.99)
                t.p(x, y, ramp[max(1, min(4, i))])
        if stars:
            for x, y in sp:
                yy = (y + (f if flow else 0)) % size
                if (x + f) % 3:
                    t.p(x, yy, H('#ffffff'))
        frames.append(t)
    return frames


# =============================================================================
# Item-Vorlagen
# =============================================================================
BAR_ROWS = [
"................","................","..........aa....",".......aaabbc...",
"....aaabddddbc..",".aaabddddddddbc.",".addddddddddeecc","abeddddddeeedcbf",
"abbeddeeedgccgbf","abbbeedgccccbbb.","agbbdgccggbbgff.",".agbdgccggfff...",
"..agbghfff......","...aaff.........","................","................"]


def bar(ramp, accent=None):
    """Barren in der klassischen Minecraft-Barrenform (Pixelraster des Vanilla-Barrens, leicht
    abgerundete Ecken), nur in den Farben der Rampe."""
    t = Tex()
    leg = {'a': ramp[1], 'b': ramp[3], 'c': ramp[2], 'd': ramp[4], 'e': H('#ffffff'),
           'f': ramp[0], 'g': ramp[2], 'h': ramp[1]}
    t.sprite(BAR_ROWS, leg)
    if accent:
        t.p(6, 7, accent[3]); t.p(7, 7, accent[4]); t.p(8, 7, accent[3])
    return t


def poly_fill(t, pts, c):
    """Füllt ein konvexes/einfaches Polygon (Pixelmitte-Test)."""
    n = len(pts)
    for y in range(t.h):
        for x in range(t.w):
            px, py = x + 0.5, y + 0.5
            inside = False
            j = n - 1
            for i in range(n):
                xi, yi = pts[i]
                xj, yj = pts[j]
                if (yi > py) != (yj > py) and px < (xj - xi) * (py - yi) / (yj - yi + 1e-9) + xi:
                    inside = not inside
                j = i
            if inside:
                t.p(x, y, c)


def raw_chunk(ramp, accent, seed):
    t = Tex()
    rng = rng_for(seed)
    shape = [(5, 4, 9), (6, 3, 11), (7, 3, 12), (8, 4, 12), (9, 3, 11), (10, 4, 10), (11, 6, 9)]
    for y, x0, x1 in shape:
        for x in range(x0, x1 + 1):
            t.p(x, y, ramp[rng.choice([1, 2, 2, 3])])
    t.p(5, 5, ramp[4]); t.p(6, 4, ramp[4])
    for _ in range(3):
        t.p(rng.randrange(5, 11), rng.randrange(5, 10), accent[3])
    t.outline()
    return t


PLATE_ROWS = [
"................",
"...000..........",
"...0WW0000......",
"...0WWWWWW0000..",
"..0WWWWWWWWLL0..",
"..0WWWWWWWLLL0..",
"..0WWWWWWLLL0...",
"..0WWWWWLLLL0...",
".0WWWWWLLLLL0...",
".0WWWWLLLLLL0...",
".0WWWLLLLLL0....",
".000LLLLLLL0....",
"....0000LLL0....",
"........000.....",
"................",
"................"]


def plate(ramp, rivets=True, mark=None):
    """Platte: flaches, gerades Blech (um 45 Grad gedreht), weicher Rand, einfarbige Fuellung.
    `rivets` bleibt aus Kompatibilitaet bestehen, wird nicht mehr gezeichnet."""
    t = Tex()
    t.sprite(PLATE_ROWS, {'0': ramp[2], 'W': ramp[3], 'L': ramp[3]})
    if mark:
        for x, y, c in mark:
            t.p(x, y, c)
    return t


def crystal(ramp):
    rows = [
        "..........o.....",
        ".........o4o....",
        "........o432o...",
        "........o432o...",
        "...o...o4332o...",
        "..o4o..o4322o...",
        "..o43o.o4322o...",
        "..o432oo3221o...",
        "...o32o43221o...",
        "...o321o3211o...",
        "....o21o211o....",
        "....o221111o....",
        ".....oo0000o....",
        ".......ooo......",
    ]
    t = Tex()
    t.sprite(rows, {'o': ramp[0] if ramp is not G else H('#06301a'), '0': ramp[0], '1': ramp[1],
                    '2': ramp[2], '3': ramp[3], '4': ramp[4]}, 0, 1)
    return t


def dust(ramp, seed, accent=None):
    t = Tex()
    rng = rng_for(seed)
    for y in range(8, 14):
        half = [2, 4, 5, 6, 6, 5][y - 8]
        for x in range(8 - half, 8 + half):
            t.p(x, y, ramp[rng.choice([1, 2, 2, 3, 3, 4] if y < 11 else [0, 1, 1, 2, 2])])
    if accent:
        for _ in range(3):
            t.p(rng.randrange(4, 12), rng.randrange(9, 13), accent)
    t.outline()
    return t


def vial(liquid, cork=R['wood'], level=8, bubbles=True, glow=False):
    t = Tex()
    glass = A(H('#d8f0f4'), 90)
    t.rect(6, 1, 9, 2, cork[2]); t.p(6, 1, cork[3]); t.rect(6, 3, 9, 3, cork[1])
    t.rect(6, 4, 9, 5, glass)
    for y in range(6, 15):
        half = 4 if 7 <= y <= 13 else 3
        t.rect(8 - half, y, 7 + half, y, glass)
    for y in range(level, 15):
        half = 4 if 7 <= y <= 13 else 3
        t.rect(8 - half, y, 7 + half, y, liquid[2])
        t.p(8 - half, y, liquid[3])
        t.p(7 + half, y, liquid[1])
    t.rect(5, level, 10, level, liquid[4])
    if bubbles:
        t.p(6, level + 2, liquid[4]); t.p(9, level + 4, liquid[4]); t.p(7, level + 5, liquid[3])
    t.p(5, 7, H('#ffffff', 200)); t.p(5, 8, H('#ffffff', 140))
    t.outline(H('#203038'))
    return t


def orb(ramp, highlight=None):
    t = Tex()
    t.shaded_disc(7.5, 8, 5.2, ramp, 0, 4)
    if highlight:
        t.p(5, 5, highlight); t.p(6, 5, highlight)
    t.outline()
    return t


def iso_cube(top, left, right, edge, stars=None, frame=0):
    """Isometrischer Würfel, 14 px breit, Kanten in Akzentfarbe."""
    t = Tex()
    poly_fill(t, [(8, 1), (15, 4.5), (8, 8), (1, 4.5)], top)
    poly_fill(t, [(1, 4.5), (8, 8), (8, 15.5), (1, 12)], left)
    poly_fill(t, [(8, 8), (15, 4.5), (15, 12), (8, 15.5)], right)
    for y in range(8, 16):
        t.p(7, y, edge)
    for x in range(1, 8):           # Kante oben links -> Mitte
        t.p(x, 4 + (x - 1) // 2, edge)
    for x in range(8, 15):
        t.p(x, 7 - (x - 8) // 2, edge)
    if stars:
        rng = rng_for('cube', frame)
        for _ in range(6):
            x, y = rng.randrange(2, 14), rng.randrange(2, 15)
            if t.get(x, y)[3] and t.get(x, y) != tuple(edge):
                t.p(x, y, stars)
    t.outline()
    return t


def circuit(board, tier):
    t = Tex()
    t.rect(2, 3, 13, 12, board[2])
    t.bevel(2, 3, 13, 12, board[3], board[0])
    trace = {0: R['copper'][3], 1: G[2], 2: G[3], 3: G[3], 4: G[4]}[tier]
    lines = [((3, 5), (8, 5)), ((3, 10), (6, 10)), ((10, 5), (12, 5)), ((9, 10), (12, 10)),
             ((4, 7), (4, 9)), ((12, 7), (12, 8)), ((3, 7), (5, 7)), ((10, 8), (12, 8))]
    for (x0, y0), (x1, y1) in lines[:3 + tier * 1 + 1]:
        t.line(x0, y0, x1, y1, trace)
    t.rect(6, 6, 9, 9, BLACK)
    t.p(6, 6, R['steel'][2])
    for x in (6, 7, 8, 9):
        t.p(x, 5 if tier >= 1 else 4, R['steel'][3]) if tier < 2 else None
    if tier >= 2:
        t.rect(7, 7, 8, 8, G[3] if tier < 4 else R['white'][4])
    if tier == 4:
        t.p(7, 7, H('#ffffff'))
    for x, y in [(3, 4), (12, 4), (3, 11), (12, 11)]:
        t.p(x, y, R['brass'][3])
    for x in range(3, 13, 2):   # Pins
        t.p(x, 13, R['steel'][3])
        t.p(x, 2, R['steel'][3])
    t.outline()
    return t


def lens(ring, glass, core=None):
    t = Tex()
    t.disc(7.5, 7.5, 5.6, ring[2])
    t.ring(7.5, 7.5, 5.6, ring[1], 0.6)
    t.disc(7.5, 7.5, 4.1, glass[2])
    t.disc(6.5, 6.5, 2.4, glass[3])
    t.p(5, 5, glass[4]); t.p(6, 5, glass[4]); t.p(5, 6, glass[4])
    if core:
        t.disc(7.5, 7.5, 2.2, core[0])
        t.ring(7.5, 7.5, 3.2, core[1], 0.5)
    t.p(4, 4, ring[4]); t.p(11, 11, ring[0])
    t.outline()
    return t


def coil(wire, core=R['steel']):
    t = Tex()
    t.rect(3, 6, 12, 10, core[2])
    t.rect(3, 6, 12, 6, core[3])
    t.rect(1, 5, 2, 11, core[1]); t.rect(13, 5, 14, 11, core[1])
    t.rect(1, 5, 2, 5, core[3]); t.rect(13, 5, 14, 5, core[3])
    for x in range(3, 13):
        c = wire[3] if x % 2 == 0 else wire[1]
        for y in range(5, 12):
            t.p(x, y, wire[2] if y not in (5, 11) else c)
        t.p(x, 6, wire[4] if x % 2 == 0 else wire[3])
        t.p(x, 10, wire[0])
    t.outline()
    return t


def chip(ramp, symbol):
    t = Tex()
    t.rect(3, 2, 12, 13, GR[2])
    t.bevel(3, 2, 12, 13, GR[4], GR[0])
    t.rect(5, 4, 10, 9, BLACK)
    for x, y, c in symbol(ramp):
        t.p(x, y, c)
    for x in range(4, 12, 2):
        t.p(x, 14, R['brass'][3])
    t.rect(5, 11, 10, 11, ramp[2])
    t.outline()
    return t


def sym_tempo(r):
    return [(6, 5, r[3]), (7, 6, r[3]), (6, 7, r[3]), (8, 5, r[4]), (9, 6, r[4]), (8, 7, r[4]), (6, 8, r[1]), (9, 8, r[1])]


def sym_eff(r):
    return [(8, 4, r[3]), (7, 5, r[3]), (6, 6, r[4]), (7, 6, r[4]), (8, 6, r[4]), (8, 7, r[3]), (7, 8, r[3]), (6, 9, r[2])]


def sym_par(r):
    return [(x, y, r[3]) for x in (6, 9) for y in range(5, 9)] + [(7, 5, r[1]), (8, 8, r[1])]


def sym_ki(r):
    return [(6, 5, r[4]), (9, 5, r[4]), (6, 6, r[3]), (9, 6, r[3]), (6, 8, r[2]), (7, 8, r[3]), (8, 8, r[3]), (9, 8, r[2])]


SHARD = [
    ".........o......",
    "........o4o.....",
    ".......o443o....",
    "......o4433o....",
    ".....o44332o....",
    "....o443322oo...",
    "...o4433221o3o..",
    "...o433221o332o.",
    "..o4332211o3221o",
    "..o332211o3221o.",
    ".o432211o3211o..",
    ".o32211o211o....",
    ".o2211o.o1o.....",
    "..o11o...o......",
    "...oo...........",
]


def shard(ramp, seed, frame=None):
    t = Tex()
    t.sprite(SHARD, {'o': OUT, '0': ramp[0], '1': ramp[1], '2': ramp[2], '3': ramp[3], '4': ramp[4]}, 0, 1)
    return t


def card(ramp, dots):
    t = Tex()
    t.rect(2, 3, 13, 12, ramp[2])
    t.bevel(2, 3, 13, 12, ramp[3], ramp[0])
    t.rect(4, 5, 11, 10, ramp[1])
    for x, y in dots:
        t.p(x, y, G[3])
    t.line(5, 9, 10, 6, A(G[2], 160))
    t.p(12, 4, G[4])
    t.outline()
    return t


# =============================================================================
# Registrierung
# =============================================================================
def build(pk):
    S, D = R['stone'], R['deep']
    blocks = [
        ('xenit_ore', ore('xenit_ore', S, P, G[3]), 'Xenit-Erz'),
        ('deepslate_xenit_ore', ore('deepslate_xenit_ore', D, P, G[3]), 'Tiefenschiefer-Xenit-Erz'),
        ('fluxkristall_ore', ore('flux_ore', D, G, G[4], n=6, glow_count=5), 'Fluxkristall-Erz'),
        ('gravitit_ore', gravitit_ore(), 'Gravitit-Erz'),
        ('xenit_block', xenit_block(), 'Xenit-Block'),
        ('schwebstein', schwebstein(), 'Schwebstein'),
        ('schrott_haufen', schrott_haufen(), 'Schrotthaufen'),
    ]
    for tid, tex, name in blocks:
        pk.add('Materialien', 'block', tid, tex, name, lang_key=f'block.riftverse.{tid}')
    pk.add('Materialien', 'block', 'fluxkristall_block', flux_block_frames(), 'Fluxkristall-Block',
           frametime=3, interpolate=True, lang_key='block.riftverse.fluxkristall_block')
    pk.add('Materialien', 'block', 'chronoglas', chronoglas_frames(), 'Chronoglas',
           frametime=6, lang_key='block.riftverse.chronoglas')
    pk.add('Materialien', 'block', 'nullmaterie_block', null_frames(), 'Nullmaterie-Block',
           frametime=4, lang_key='block.riftverse.nullmaterie_block')

    fluids = [
        ('roh_portalfluid', R['acid'], False, 'Roh-Portalfluid'),
        ('portalfluid', G, False, 'Stabilisiertes Portalfluid'),
        ('portalfluid_konzentrat', [G[0], R['cyan'][1], G[1], R['cyan'][2], G[3]], False, 'Portalfluid-Konzentrat'),
        ('weltenformer_essenz', [R['null'][0], R['null'][1], R['null'][2], G[1], G[3]], True, 'Weltenformer-Essenz'),
    ]
    for tid, ramp, st, name in fluids:
        pk.add('Fluids', 'block', tid + '_still', fluid_frames(ramp, 16, False, st), name,
               frametime=2, lang_key=f'fluid_type.riftverse.{tid}')
        pk.add('Fluids', 'block', tid + '_flow', fluid_frames(ramp, 32, True, st), name + ' (fließend)', frametime=2)

    V, AC, FL, GV, MI = R['violet'], R['acid'], R['flesh'], R['grav'], R['mirror']
    items = [
        ('raw_xenit', raw_chunk(P, G, 'raw_xenit'), 'Rohes Xenit'),
        ('xenit_ingot', bar(P, G), 'Xenit-Barren'),
        ('xenit_platte', plate(P), 'Xenit-Platte'),
        ('quantum_ingot', bar(R['quantum']), 'Quanten-Barren'),
        ('alien_schrott', None, 'Alien-Schrott'),
        ('schrottplatte', plate(R['rust'], mark=[(8, 7, R['steel'][3]), (9, 7, R['steel'][3])]), 'Schrottplatte'),
        ('fluxkristall', crystal(G), 'Fluxkristall'),
        ('fluxstaub', dust(G, 'fluxstaub', G[4]), 'Fluxstaub'),
        ('saeuregel', vial(AC, level=7), 'Säuregel'),
        ('aetzloesung', vial(R['cyan'], level=9), 'Ätzlösung'),
        ('toxinrest', dust(AC, 'toxinrest', R['flesh'][2]), 'Toxinrest'),
        ('schwebstein_brocken', raw_chunk(W, R['sky'], 'schwebstein_brocken'), 'Schwebstein-Brocken'),
        ('antigrav_platte', plate(W, rivets=False, mark=[(6, 10, R['sky'][2]), (7, 9, R['sky'][3]), (8, 8, R['sky'][4]), (9, 7, R['sky'][3]), (10, 6, R['sky'][2])]), 'Antigrav-Platte'),
        ('chronoglas_scheibe', None, 'Chronoglas-Scheibe'),
        ('biomasse_pulpe', None, 'Biomasse-Pulpe'),
        ('naehrserum', vial(FL, cork=R['steel'], level=8), 'Nährserum'),
        ('gravitit', orb(GV, GV[4]), 'Gravitit'),
        ('gravitit_rahmen', None, 'Gravitit-Rahmen'),
        ('inversionssplitter', shard(MI, 'inv'), 'Inversionssplitter'),
        ('universumsprobe', None, 'Universumsprobe'),
        ('koordinatenkarte', card(P, [(6, 7), (9, 6), (7, 9), (10, 8)]), 'Koordinatenkarte'),
        ('universums_kern', None, 'Universums-Kern'),
    ]
    items = dict((k, (v, n)) for k, v, n in items)

    # Sonderfälle
    t = Tex()
    rng = rng_for('alien_schrott')
    t.sprite([
        "....oooo........",
        "...o3443oo......",
        "..o332344oo.....",
        "..o2222333o.....",
        "...o12222o......",
        "....o122oooo....",
        ".....o1o3443o...",
        ".....oo332223o..",
        "......o222111o..",
        "......o11000o...",
        ".......ooooo....",
    ], {'o': OUT, '0': R['steel'][0], '1': R['steel'][1], '2': R['steel'][2], '3': R['steel'][3], '4': R['steel'][4]}, 1, 2)
    for x, y in [(4, 5), (6, 5), (5, 6), (10, 10), (11, 11)]:
        t.p(x, y, G[3])
    t.line(12, 5, 14, 2, R['red'][2]); t.p(14, 1, R['copper'][3])
    items['alien_schrott'] = (t, items['alien_schrott'][1])

    t = Tex()
    t.disc(7.5, 7.5, 5.8, A(V[2], 200))
    t.ring(7.5, 7.5, 5.8, V[1], 0.6)
    t.disc(6, 6, 1.8, V[3])
    t.line(8, 8, 8, 4, V[4]); t.line(8, 8, 10, 8, V[4])
    t.outline()
    items['chronoglas_scheibe'] = (t, items['chronoglas_scheibe'][1])

    t = Tex()
    rng = rng_for('biomasse')
    t.shaded_disc(7.5, 9, 5, FL, 1, 4)
    t.shaded_disc(10, 6, 2.6, FL, 1, 4)
    for _ in range(5):
        x, y = rng.randrange(4, 12), rng.randrange(6, 13)
        t.p(x, y, FL[0])
    t.outline()
    items['biomasse_pulpe'] = (t, items['biomasse_pulpe'][1])

    t = Tex()
    t.frame(2, 2, 13, 13, GV[2]); t.frame(3, 3, 12, 12, GV[1])
    t.frame(1, 1, 14, 14, GV[3])
    for x, y in [(1, 1), (14, 1), (1, 14), (14, 14)]:
        t.p(x, y, GV[4])
    t.line(3, 3, 12, 12, GV[2]); t.p(7, 7, R['sky'][3]); t.p(8, 8, R['sky'][3])
    t.outline()
    items['gravitit_rahmen'] = (t, items['gravitit_rahmen'][1])

    t = Tex()
    t.disc(7.5, 7.5, 6, A(H('#d8f0f4'), 70))
    t.shaded_disc(7.5, 8.5, 3.2, R['sky'], 1, 4)
    t.p(6, 8, G[2]); t.p(7, 9, G[2]); t.p(9, 7, G[3]); t.p(8, 10, G[1])
    t.ring(7.5, 7.5, 6, H('#a8c8d0'), 0.5)
    t.p(4, 4, H('#ffffff'))
    t.rect(6, 0, 9, 1, R['steel'][2])
    t.outline(H('#203038'))
    items['universumsprobe'] = (t, items['universumsprobe'][1])

    uk = []
    for f in range(8):
        t = Tex()
        t.shaded_disc(7.5, 7.5, 5.5, G, 0, 4)
        a = f / 8 * 2 * math.pi
        for r in range(6):
            aa = a + r * 0.6
            t.p(int(round(7.5 + r * math.cos(aa))), int(round(7.5 + r * math.sin(aa))), R['null'][1])
        t.ring(7.5, 7.5, 6.5, A(G[4], 150), 0.4)
        t.outline()
        uk.append(t)

    for tid, (tex, name) in items.items():
        if tid == 'universums_kern':
            continue
        pk.add('Materialien', 'item', tid, tex, name, lang_key=f'item.riftverse.{tid}')
    pk.add('Materialien', 'item', 'universums_kern', uk, 'Universums-Kern', frametime=3,
           lang_key='item.riftverse.universums_kern')

    nm = [iso_cube(R['null'][3], R['null'][2], R['null'][1], G[2], H('#ffffff'), f) for f in range(6)]
    pk.add('Materialien', 'item', 'nullmaterie', nm, 'Nullmaterie', frametime=5, lang_key='item.riftverse.nullmaterie')

    sf = []
    # Jede Szene: (Himmel, Boden) – Spiegelscherbe zeigt wechselnde Universen
    scenes = [(R['sky'], G), (R['violet'], R['white']), (R['null'], G), (R['acid'], R['rust']),
              (R['orange'], R['flesh']), (R['cyan'], R['grav'])]
    for f, (sky, ground) in enumerate(scenes):
        t = shard(R['mirror'], 'sf')
        for y in range(16):
            for x in range(16):
                c = t.get(x, y)
                if c[3] and c != OUT:
                    horizon = 9 + (1 if x % 5 == 0 else 0)
                    t.p(x, y, sky[3] if y < horizon else ground[2])
        t.p(5, 4, H('#ffffff')); t.p(4, 6, H('#ffffff', 200))
        sf.append(t)
    pk.add('Materialien', 'item', 'story_fragment', sf, 'Story-Fragment', frametime=20,
           lang_key='item.riftverse.story_fragment')

    # --- Bauteil-Leitern ----------------------------------------------------
    boards = [R['rust'], P, W, GR, R['null']]
    for i, (tid, name) in enumerate([('basis_schaltkreis', 'Basis-Schaltkreis'), ('rift_schaltkreis', 'Rift-Schaltkreis'),
                                     ('quanten_schaltkreis', 'Quanten-Schaltkreis'), ('multiversal_prozessor', 'Multiversal-Prozessor')]):
        b = boards[i if i < 3 else 4] if i < 3 else boards[4]
        if i == 2:
            b = W
        pk.add('Bauteile', 'item', tid, circuit(b, i + (1 if i == 3 else 0)), name, lang_key=f'item.riftverse.{tid}')

    lenses = [
        ('fluxlinse', R['steel'], G, None, 'Fluxlinse'),
        ('stabilisierte_linse', P, G, None, 'Stabilisierte Linse'),
        ('chrono_linse', R['brass'], R['violet'], None, 'Chrono-Linse'),
        ('singularitaetslinse', GR, G, (R['null'][0], G[3]), 'Singularitätslinse'),
    ]
    for tid, ring, glass, core, name in lenses:
        pk.add('Bauteile', 'item', tid, lens(ring, glass, core), name, lang_key=f'item.riftverse.{tid}')

    coils = [('xenit_spule', P, 'Xenit-Spule'), ('flux_spule', G, 'Flux-Spule'),
             ('gravitit_spule', GV, 'Gravitit-Spule'), ('null_spule', [R['null'][1], R['null'][2], R['null'][3], G[2], G[3]], 'Null-Spule')]
    for tid, wire, name in coils:
        pk.add('Bauteile', 'item', tid, coil(wire), name, lang_key=f'item.riftverse.{tid}')

    chips = [('tempo_chip', R['orange'], sym_tempo, 'Tempo-Chip'), ('effizienz_chip', G, sym_eff, 'Effizienz-Chip'),
             ('parallel_chip', R['cyan'], sym_par, 'Parallel-Chip'), ('ki_chip', R['violet'], sym_ki, 'KI-Chip')]
    for tid, rp, sym, name in chips:
        pk.add('Bauteile', 'item', tid, chip(rp, sym), name, lang_key=f'item.riftverse.{tid}')

    # Kleinteile
    t = Tex()
    CU = R['copper']
    for y in range(4, 12):
        x0 = 4 + (y % 2)
        t.rect(x0, y, x0 + 7, y, CU[2 if y % 2 else 3])
        t.p(x0, y, CU[4])
    t.line(12, 11, 14, 13, CU[3]); t.line(3, 4, 1, 2, CU[3])
    t.outline()
    pk.add('Bauteile', 'item', 'draht', t, 'Draht', lang_key='item.riftverse.draht')

    t = Tex()
    ST = R['steel']
    for i, y in enumerate(range(3, 13, 2)):
        t.line(4, y, 11, y + 1, ST[3]); t.p(4, y, ST[4]); t.line(11, y + 1, 4, y + 2, ST[1])
    t.outline()
    pk.add('Bauteile', 'item', 'feder', t, 'Feder', lang_key='item.riftverse.feder')

    t = Tex()
    t.rect(3, 5, 10, 11, ST[2]); t.rect(3, 5, 10, 5, ST[3])
    for x in range(4, 10, 2):
        t.line(x, 6, x, 11, ST[1])
    t.rect(11, 7, 13, 9, CU[2]); t.p(14, 8, ST[3])
    t.rect(4, 12, 9, 12, GR[1])
    t.p(5, 4, R['red'][2]); t.p(8, 4, BLACK)
    t.outline()
    pk.add('Bauteile', 'item', 'mini_motor', t, 'Mini-Motor', lang_key='item.riftverse.mini_motor')
