"""Maschinengehäuse T0–T4, Maschinenfronten, Generatoren und Speicher."""
import math

from lib import Tex, R, T, H, A, BLACK, rng_for
from materials import null_frames

G, P, W, GR, V = R['green'], R['petrol'], R['white'], R['graphite'], R['violet']
ST, WO, RU, BR, CU, OR = R['steel'], R['wood'], R['rust'], R['brass'], R['copper'], R['orange']
GLASS = A(H('#bfe8ee'), 120)
DARK = H('#0c0e12')


# =============================================================================
# Gehäuse
# =============================================================================
def casing(tier, top=False):
    rng = rng_for(f'casing{tier}{top}')
    t = Tex()
    if tier == 0:
        t.noise(WO, (0.05, 0.25, 0.45, 0.2, 0.05), rng, clump=0.7)
        for y in (3, 7, 11, 15):
            t.rect(0, y, 15, y, WO[0])
        for y in range(0, 16, 4):     # Brettfugen versetzt
            x = (y * 5 + 3) % 16
            t.rect(x, y, x, y + 2, WO[1])
        for cx, cy, fx, fy in [(0, 0, 1, 1), (15, 0, -1, 1), (0, 15, 1, -1), (15, 15, -1, -1)]:
            for k in range(4):
                t.p(cx + fx * k, cy, RU[2 + (k == 0)])
                t.p(cx, cy + fy * k, RU[2])
            t.p(cx + fx, cy + fy, RU[1])
        t.p(1, 1, BR[3]); t.p(14, 1, ST[3]); t.p(1, 14, ST[3]); t.p(14, 14, BR[2])
        if not top:   # Klebeband
            for k in range(5):
                t.p(10 + k, 12 - k, H('#9a9a92')); t.p(10 + k, 13 - k, H('#7a7a72'))
    elif tier == 1:
        t.noise(P, (0, 0.15, 0.65, 0.2, 0), rng, clump=0.6)
        t.bevel(0, 0, 15, 15, P[3], P[0])
        t.frame(1, 1, 14, 14, P[1])
        t.bevel(2, 2, 13, 13, P[3], P[1])
        for x, y in [(1, 1), (14, 1), (1, 14), (14, 14)]:
            t.p(x, y, P[4])
        if top:
            for x in range(4, 12, 2):
                t.rect(x, 5, x, 10, P[0])
    elif tier == 2:
        t.noise(W, (0, 0.05, 0.3, 0.6, 0.05), rng, clump=0.7)
        t.bevel(0, 0, 15, 15, W[4], W[0])
        t.bevel(1, 1, 14, 14, W[4], W[1])
        t.p(0, 0, W[2]); t.p(15, 0, W[2]); t.p(0, 15, W[1]); t.p(15, 15, W[0])
        if top:
            t.rect(4, 4, 11, 11, W[2])
            t.bevel(4, 4, 11, 11, W[1], W[4])
            t.disc(7.5, 7.5, 1.6, G[2])
        else:
            t.rect(3, 13, 12, 13, G[2]); t.p(3, 13, G[3])
    elif tier == 3:
        t.noise(GR, (0.05, 0.2, 0.6, 0.15, 0), rng, clump=0.7)
        t.bevel(0, 0, 15, 15, GR[4], GR[0])
        t.frame(1, 1, 14, 14, GR[1])
        traces = [((2, 4), (6, 4), (6, 8)), ((13, 3), (10, 3), (10, 6)), ((3, 12), (8, 12), (8, 10)), ((13, 10), (13, 13))]
        for path in traces:
            for a, b in zip(path, path[1:]):
                t.line(a[0], a[1], b[0], b[1], P[3])
            t.p(path[-1][0], path[-1][1], G[2])
        if top:
            t.disc(7.5, 7.5, 2.5, GR[0])
            t.ring(7.5, 7.5, 2.5, P[3], 0.5)
    return t


def casing_t4_frames():
    frames = null_frames(seed='casing_t4', edge=False)
    for t in frames:
        t.bevel(0, 0, 15, 15, R['null'][4], R['null'][0])
        for x, y in [(3, 0), (4, 1), (4, 2), (12, 15), (11, 14), (0, 9), (1, 9), (2, 10), (15, 4), (14, 5)]:
            t.p(x, y, G[3])
    return frames


# --- kleine Helfer ------------------------------------------------------------
def lamp(t, x, y, on, off_c=None):
    t.p(x, y, G[4] if on else (off_c or R['yellow'][2]))
    t.p(x + 1, y, G[2] if on else R['yellow'][1])


def window(t, x0, y0, x1, y1, frame_c, inner=DARK):
    t.rect(x0, y0, x1, y1, inner)
    t.frame(x0 - 1, y0 - 1, x1 + 1, y1 + 1, frame_c)
    t.p(x0, y0, A(H('#ffffff'), 90))


def porthole(t, cx, cy, r, ring, inner):
    t.disc(cx, cy, r + 1, ring[1])
    t.ring(cx, cy, r + 0.6, ring[3], 0.5)
    t.disc(cx, cy, r, inner)


def vents(t, y, x0, x1, c):
    for x in range(x0, x1 + 1, 2):
        t.p(x, y, c)
        t.p(x, y + 1, c)


# =============================================================================
# Maschinenfronten (Funktion: on -> Tex)
# =============================================================================
def garagenpresse(on):
    t = casing(0)
    t.rect(2, 2, 3, 13, ST[2]); t.rect(12, 2, 13, 13, ST[2])
    t.rect(2, 2, 2, 13, ST[3]); t.rect(12, 2, 12, 13, ST[3])
    t.rect(2, 2, 13, 3, ST[1]); t.rect(2, 2, 13, 2, ST[3])
    sy = 7 if on else 4
    t.rect(7, 4, 8, sy, ST[3])
    t.rect(5, sy, 10, sy + 2, ST[2]); t.rect(5, sy + 2, 10, sy + 2, ST[0])
    t.rect(4, 11, 11, 12, ST[1]); t.rect(4, 11, 11, 11, ST[3])
    if on:
        t.rect(5, 10, 10, 10, RU[3])
    t.rect(12, 7, 13, 8, H('#9a9a92'))   # Klebeband
    return t


def xenit_schmelze(on):
    t = casing(1)
    vents(t, 3, 4, 11, P[0])
    porthole(t, 7.5, 9.5, 3.2, ST, DARK)
    if on:
        t.disc(7.5, 10, 2.6, OR[1]); t.disc(7.5, 10.5, 1.6, OR[2]); t.p(7, 11, OR[4]); t.p(8, 11, R['yellow'][3])
        t.ring(7.5, 9.5, 3.2, A(G[3], 160), 0.4)
    lamp(t, 12, 3, on)
    return t


def platinen_aetzer(on):
    t = casing(1)
    t.rect(3, 5, 12, 5, V[3] if on else V[0])
    window(t, 4, 7, 11, 11, P[0], V[1] if on else DARK)
    if on:
        t.rect(5, 8, 10, 10, A(V[2], 200))
        t.line(5, 9, 8, 9, G[3]); t.p(9, 8, G[3])
    lamp(t, 3, 13, on)
    return t


def kristall_schleifer(on):
    t = casing(1)
    window(t, 3, 3, 12, 12, P[0], A(H('#20343a'), 255))
    t.disc(7.5, 7.5, 3.6, ST[2]); t.ring(7.5, 7.5, 3.6, ST[1], 0.5)
    t.disc(7.5, 7.5, 1, ST[4])
    for k in range(4):
        a = k * math.pi / 2 + (0.4 if on else 0)
        t.p(int(round(7.5 + 2.4 * math.cos(a))), int(round(7.5 + 2.4 * math.sin(a))), ST[0])
    t.rect(3, 12, 12, 12, G[1])
    if on:
        for x, y in [(11, 9), (12, 8), (11, 11), (4, 4), (12, 10)]:
            t.p(x, y, G[4])
    return t


def recycler(on):
    t = casing(1)
    for k, y in enumerate(range(2, 6)):
        t.rect(3 + k, y, 12 - k, y, DARK if k else ST[2])
    t.disc(4, 9, 2.2, BR[2]); t.disc(4, 9, 0.8, BR[0])
    t.disc(11, 9, 1.7, BR[3]); t.disc(11, 9, 0.6, BR[0])
    if on:
        t.p(6, 9, BR[4]); t.p(9, 9, BR[4])
    t.rect(3, 12, 12, 14, P[1]); t.rect(3, 12, 12, 12, P[3]); t.rect(6, 13, 9, 13, ST[3])
    return t


def fluid_mixer(on):
    t = casing(1)
    t.rect(4, 2, 11, 13, A(H('#bfe8ee'), 80))
    t.frame(3, 1, 12, 14, ST[2])
    t.rect(4, 7, 11, 13, G[1])
    t.rect(4, 7, 11, 7, G[3])
    t.rect(7, 2, 8, 11, ST[3])
    if on:
        for x, y in [(5, 9), (6, 10), (9, 9), (10, 10), (6, 12), (9, 12), (5, 11), (10, 8)]:
            t.p(x, y, G[3])
        t.p(5, 8, G[4]); t.p(10, 11, G[4])
    return t


def raffinerie(on):
    t = casing(2)
    for x in (2, 12):
        t.rect(x, 1, x + 1, 14, A(H('#bfe8ee'), 100))
        t.rect(x, 1, x, 14, W[1])
        lvl = 4 if on else 9
        t.rect(x, lvl, x + 1, 14, G[2] if on else G[1])
        if on:
            t.p(x + 1, 6, G[4]); t.p(x, 10, G[4])
    window(t, 5, 4, 10, 8, W[1], DARK)
    for i in range(5 if on else 2):
        t.p(6 + i % 4, 7 - i // 4, G[3])
    t.rect(5, 11, 10, 12, W[2]); t.rect(5, 11, 10, 11, W[4])
    lamp(t, 7, 12, on)
    return t


def montierer(on):
    t = casing(2)
    window(t, 2, 2, 13, 12, W[1], H('#1a2228'))
    t.rect(2, 11, 13, 12, ST[1])
    if on:
        t.line(3, 10, 6, 5, ST[3]); t.line(6, 5, 9, 7, ST[3]); t.p(10, 8, G[4]); t.p(9, 8, G[3])
    else:
        t.line(3, 10, 4, 4, ST[3]); t.line(4, 4, 8, 4, ST[3]); t.p(8, 5, ST[2])
    t.rect(3, 10, 4, 10, ST[2])
    t.rect(9, 10, 12, 10, P[3])
    return t


def zentrifuge(on):
    t = casing(2)
    porthole(t, 7.5, 7.5, 4.3, W, H('#20262c'))
    for k in range(6):
        a = k * math.pi / 3 + (0.5 if on else 0)
        for r in (1.5, 2.5, 3.5):
            t.p(int(round(7.5 + r * math.cos(a))), int(round(7.5 + r * math.sin(a))), R['acid'][2] if on else ST[1])
    t.disc(7.5, 7.5, 1, ST[3])
    lamp(t, 12, 13, on)
    return t


def gravitationspresse(on):
    t = casing(2)
    GV = R['grav']
    t.rect(3, 2, 12, 4, GV[2]); t.rect(3, 2, 12, 2, GV[4]); t.rect(3, 4, 12, 4, GV[1])
    t.rect(3, 11, 12, 13, GV[2]); t.rect(3, 11, 12, 11, GV[4]); t.rect(3, 13, 12, 13, GV[0])
    rng = rng_for('gpress', on)
    for _ in range(9 if on else 4):
        t.p(rng.randrange(4, 12), rng.randrange(6, 10), R['sky'][4] if on else R['sky'][2])
    return t


def scanner(on):
    t = casing(2)
    window(t, 3, 3, 12, 11, W[1], H('#0c1a20'))
    t.shaded_disc(7.5, 7, 3, G if on else R['sky'], 1, 4)
    t.ring(7.5, 7, 4.2, A(R['cyan'][3], 140), 0.4)
    t.rect(4, 13, 11, 13, W[1])
    return t


def scanner_top():
    t = casing(2, top=True)
    t.rect(3, 3, 12, 12, W[2])
    t.shaded_disc(7.5, 7.5, 3.4, R['sky'], 1, 4)
    t.p(6, 8, G[2]); t.p(8, 6, G[3]); t.p(9, 9, G[2])
    t.ring(7.5, 7.5, 4.8, A(R['cyan'][3], 160), 0.4)
    return t


def chrono(on):
    t = casing(2)
    t.disc(7.5, 7.5, 5.6, BR[2]); t.ring(7.5, 7.5, 5.6, BR[0], 0.5)
    t.disc(7.5, 7.5, 4.4, A(V[2], 230))
    for k in range(12):
        a = k / 12 * 2 * math.pi
        t.p(int(round(7.5 + 4 * math.cos(a))), int(round(7.5 + 4 * math.sin(a))), V[4])
    a1 = -math.pi / 2 + (2.0 if on else 0.0)
    a2 = -math.pi / 2 + (4.4 if on else 1.6)
    for r in range(4):
        t.p(int(round(7.5 + r * math.cos(a1))), int(round(7.5 + r * math.sin(a1))), V[0])
    for r in range(3):
        t.p(int(round(7.5 + r * math.cos(a2))), int(round(7.5 + r * math.sin(a2))), H('#ffffff'))
    return t


def lithograf(on):
    t = casing(3)
    window(t, 3, 4, 12, 10, P[1], H('#0e0a1a'))
    t.rect(4, 9, 11, 10, G[1])
    ly = 6 if on else 5
    t.rect(3, ly, 12, ly, V[3] if on else V[1])
    if on:
        t.rect(3, ly + 1, 12, ly + 1, A(V[2], 120)); t.p(8, 9, V[4])
    lamp(t, 3, 13, on)
    return t


def rekonfigurator(on):
    t = casing(3)
    for x0 in (2, 10):
        window(t, x0, 4, x0 + 3, 11, P[1], H('#101a20'))
    t.rect(6, 7, 9, 8, A(H('#bfe8ee'), 120)); t.rect(6, 7, 9, 7, P[3])
    if on:
        for x, y in [(2, 5), (4, 7), (3, 10), (5, 6)]:
            t.p(x, y, OR[3])
        t.rect(10, 9, 13, 11, R['sky'][3]); t.p(11, 8, R['sky'][4])
        t.p(7, 8, G[4]); t.p(8, 7, G[4])
    else:
        t.rect(2, 9, 5, 11, OR[2])
    return t


def destillator(on):
    t = casing(3)
    porthole(t, 7.5, 7.5, 4.2, ST, H('#0a1018'))
    t.shaded_disc(7.5, 7.5, 2.6, R['sky'], 1, 4)
    t.p(6, 8, G[2]); t.p(7, 9, G[2]); t.p(8, 6, G[3])
    if on:
        t.ring(7.5, 7.5, 3.6, A(G[3], 200), 0.4)
    return t


def kabelknoten_top():
    t = casing(3, top=True)
    t.disc(7.5, 7.5, 3, DARK)
    for (x, y) in [(6, 5), (7, 6), (7, 7), (8, 8), (8, 9), (9, 10), (6, 4)]:
        t.p(x, y, G[3])
    t.p(7, 7, G[4])
    return t


def kabelknoten_side():
    t = casing(3)
    t.rect(7, 1, 8, 14, ST[2]); t.rect(7, 1, 7, 14, ST[3])
    for y in (4, 8, 12):
        t.rect(5, y, 10, y, ST[1])
    t.p(7, 1, G[4]); t.p(8, 1, G[3])
    return t


def schmiede(frames=False):
    out = []
    for f, base in enumerate(casing_t4_frames()):
        t = base
        ph = f / 8 * 2 * math.pi
        for y in range(10, 15):
            for x in range(2, 14):
                if ((x - 7.5) / 5.5) ** 2 + ((y - 12) / 2.2) ** 2 <= 1:
                    v = math.sin(math.atan2(y - 12, x - 7.5) * 2 + ph)
                    t.p(x, y, G[3] if v > 0.3 else G[1] if v > -0.4 else R['null'][2])
        t.rect(4, 4, 11, 5, ST[3]); t.rect(3, 4, 3, 4, ST[2]); t.rect(12, 4, 13, 5, ST[2])
        t.rect(6, 6, 9, 7, ST[2]); t.rect(5, 8, 10, 8, ST[1])
        t.rect(4, 4, 11, 4, ST[4])
        out.append(t)
    return out


def weltenformer():
    out = []
    for f, base in enumerate(casing_t4_frames()):
        t = base
        t.ring(7.5, 7.5, 5.8, GR[3], 0.8)
        t.ring(7.5, 7.5, 5.8, G[2], 0.25)
        k = f % 4
        t.shaded_disc(7.5, 7.5, 3.2, R['sky'], 1, 4)
        for x, y in [(6 + k % 3, 7), (7, 8 + k % 2), (8, 6)]:
            t.p(x, y, G[2])
        out.append(t)
    return out


def werkbank():
    top = Tex()
    rng = rng_for('wb_top')
    top.noise(WO, (0.02, 0.2, 0.5, 0.25, 0.03), rng, clump=0.75)
    for y in (4, 9, 14):
        top.rect(0, y, 15, y, WO[1])
    top.frame(0, 0, 15, 15, WO[0])
    top.rect(1, 1, 4, 3, ST[2]); top.rect(1, 1, 4, 1, ST[3]); top.p(2, 2, ST[0])   # Schraubstock
    top.line(9, 2, 13, 6, ST[3]); top.rect(12, 6, 13, 7, WO[4])                  # Lötkolben
    top.line(4, 11, 8, 11, WO[3]); top.rect(8, 10, 9, 12, ST[2])                 # Hammer
    top.ring(11.5, 11.5, 1.8, H('#5a3a1c'), 0.45)                                 # Kaffeefleck
    front = casing(0)
    front.rect(2, 3, 13, 7, WO[1]); front.frame(2, 3, 13, 7, WO[0]); front.rect(6, 5, 9, 5, ST[3])
    front.rect(2, 9, 3, 15, WO[0]); front.rect(12, 9, 13, 15, WO[0])
    side = casing(0)
    side.rect(1, 1, 14, 8, WO[1])
    for y in range(2, 8, 2):
        for x in range(2, 14, 2):
            side.p(x, y, WO[0])
    side.line(3, 2, 3, 6, ST[3]); side.rect(2, 2, 4, 2, ST[2])   # Schraubenschlüssel
    side.line(7, 2, 7, 6, WO[4]); side.rect(6, 2, 8, 3, ST[2])   # Hammer
    side.line(11, 2, 12, 7, R['red'][2])                          # Kabel
    return top, front, side


# --- Generatoren --------------------------------------------------------------
def kurbel_dynamo(on):
    t = casing(0)
    t.disc(7.5, 8.5, 3.6, ST[2]); t.ring(7.5, 8.5, 3.6, ST[1], 0.5)
    for k in range(6):
        a = k * math.pi / 3 + (0.5 if on else 0)
        t.p(int(round(7.5 + 2.4 * math.cos(a))), int(round(8.5 + 2.4 * math.sin(a))), ST[0])
    t.disc(7.5, 8.5, 0.9, ST[4])
    ex, ey = (12, 4) if not on else (12, 12)
    t.line(8, 8, ex, ey, ST[3]); t.rect(ex, ey - 1, ex + 1, ey, WO[3])
    return t


def tonne_side():
    t = Tex()
    for x in range(16):
        c = ST[[1, 2, 3, 2][x % 4]]
        t.rect(x, 0, x, 15, c)
    t.rect(0, 0, 15, 1, ST[3]); t.rect(0, 14, 15, 15, ST[1])
    rng = rng_for('tonne')
    for _ in range(10):
        x, y = rng.randrange(16), rng.randrange(3, 15)
        t.p(x, y, RU[rng.choice([1, 2, 3])])
    t.rect(0, 2, 15, 2, G[2]); t.p(5, 2, G[4]); t.p(11, 2, G[4])
    return t


def tonne_top(on):
    t = Tex()
    t.disc(7.5, 7.5, 7.8, ST[3]); t.disc(7.5, 7.5, 7.8, ST[2])
    for r in (6.5, 4.5):
        t.ring(7.5, 7.5, r, ST[1], 0.4)
    t.ring(7.5, 7.5, 7.6, G[3] if on else G[1], 0.45)
    t.rect(5, 7, 10, 8, ST[1]); t.rect(5, 7, 10, 7, ST[4])
    t.rect(11, 2, 12, 4, ST[0]); t.p(11, 1, H('#7a7a72'))   # Ofenrohr
    return t


def kessel(on):
    t = Tex()
    rng = rng_for('kessel')
    t.noise(CU, (0.05, 0.2, 0.5, 0.2, 0.05), rng, clump=0.7)
    t.bevel(0, 0, 15, 15, CU[4], CU[0])
    for y in (3, 12):
        t.rect(0, y, 15, y, CU[1])
        for x in range(1, 15, 3):
            t.p(x, y, CU[4])
    porthole(t, 7.5, 7.5, 3, BR, R['flesh'][2 if on else 1])
    if on:
        t.disc(7, 7, 1.4, R['flesh'][3]); t.p(9, 9, R['flesh'][4])
        t.p(13, 1, A(W[4], 180)); t.p(14, 0, A(W[4], 140))
    return t


def solar_top():
    t = Tex(fill=V[1])
    t.bevel(0, 0, 15, 15, V[3], V[0])
    for cy in range(2, 14, 4):
        for cx in range(2, 14, 4):
            t.rect(cx, cy, cx + 2, cy + 2, R['sky'][1])
            t.p(cx, cy, R['sky'][3])
            t.p(cx + 2, cy + 2, R['sky'][0])
    t.p(5, 5, R['yellow'][3]); t.p(9, 9, R['yellow'][2])
    return t


def resonator(on):
    t = casing(2)
    t.rect(4, 3, 5, 10, ST[3]); t.rect(10, 3, 11, 10, ST[3])
    t.rect(4, 10, 11, 11, ST[2]); t.rect(7, 12, 8, 13, ST[1])
    t.sprite(["..o..", ".o4o.", ".o32o", "o432o", "o321o", ".o1o."], {'o': G[0], '1': G[1], '2': G[2], '3': G[3], '4': G[4]}, 5, 3)
    if on:
        t.p(6, 2, G[4]); t.p(9, 3, G[4]); t.p(3, 6, A(G[4], 150)); t.p(12, 6, A(G[4], 150))
    t.rect(13, 2, 13, 13, W[1])
    hot = 9 if on else 4
    t.rect(13, 13 - hot, 13, 13, OR[2])
    return t


def pendel(on):
    t = casing(2)
    window(t, 3, 3, 12, 12, ST[1], H('#10161e'))
    t.rect(3, 3, 12, 3, R['sky'][1]); t.rect(3, 12, 12, 12, R['grav'][2])
    bx = 10 if on else 7
    t.line(7, 4, bx, 8, ST[3])
    t.shaded_disc(bx + 0.5, 9.5, 2.1, R['mirror'], 1, 4)
    if on:
        for x, y in [(5, 10), (4, 9), (6, 11)]:
            t.p(x, y, A(R['sky'][3], 140))
    lamp(t, 7, 14, on)
    return t


def zelle_top(on):
    t = Tex(fill=H('#1a1c20'))
    t.bevel(0, 0, 15, 15, H('#3a3e44'), BLACK)
    t.rect(2, 2, 13, 13, A(H('#2a4048'), 255))
    rng = rng_for('city', on)
    for x in range(2, 14):        # Mini-Stadt
        h = rng.choice([1, 2, 3, 4, 5, 3, 2])
        t.rect(x, 13 - h, x, 13, GR[3] if x % 2 else GR[2])
        if rng.random() < (0.85 if on else 0.4):
            t.p(x, 13 - h + rng.randrange(h), R['yellow'][3])
    t.rect(2, 2, 13, 2, A(H('#ffffff'), 60))
    t.rect(0, 0, 2, 1, R['red'][2]); t.rect(13, 0, 15, 1, BLACK)
    return t


def zelle_side():
    t = Tex(fill=H('#1e2024'))
    t.bevel(0, 0, 15, 15, H('#3a3e44'), BLACK)
    t.rect(1, 6, 14, 9, G[1]); t.rect(1, 6, 14, 6, G[3])
    t.rect(3, 7, 5, 8, BLACK); t.p(10, 7, H('#ffffff')); t.p(11, 8, H('#ffffff'))
    t.rect(1, 1, 3, 2, R['red'][2]); t.rect(12, 1, 14, 2, GR[0])
    return t


def zapfer_top():
    t = casing(3, top=True)
    t.disc(7.5, 7.5, 3.4, R['null'][1])
    for y in range(4, 12):
        t.p(7 + (y % 2), y, G[3])
    t.p(7, 7, G[4]); t.p(8, 8, G[4])
    for x, y in [(2, 2), (13, 2), (2, 13), (13, 13)]:
        t.rect(x, y, x, y, ST[4])
    t.line(2, 2, 5, 5, R['cyan'][4]); t.line(13, 13, 10, 10, R['cyan'][4])
    return t


def singularitaet():
    out = []
    for f in range(8):
        t = casing(3)
        t.disc(7.5, 7.5, 5.6, G[1]); t.disc(7.5, 7.5, 4.8, GR[1])
        t.disc(7.5, 7.5, 3.6, BLACK)
        ph = f / 8 * 2 * math.pi
        for k in range(10):
            a = ph + k * 0.63
            r = 5.2 - (k % 3) * 0.6
            t.p(int(round(7.5 + r * math.cos(a))), int(round(7.5 + r * math.sin(a))), G[4] if k % 2 else G[3])
        t.p(6, 6, R['null'][3])
        out.append(t)
    return out


def speicher(tier, ramp, fill_steps):
    t = casing(tier)
    t.rect(6, 2, 9, 13, DARK)
    t.frame(5, 1, 10, 14, ramp[1])
    for i in range(fill_steps):
        y = 12 - i * 2
        t.rect(6, y, 9, y, ramp[3]); t.p(6, y, ramp[4])
    return t


def klon_tank():
    FL = G
    bot, top = Tex(), Tex()
    for t in (bot, top):
        t.rect(1, 0, 14, 15, A(H('#bfe8ee'), 70))
        t.rect(2, 0, 13, 15, A(FL[2], 170))
        t.rect(1, 0, 1, 15, A(H('#ffffff'), 120)); t.rect(14, 0, 14, 15, A(FL[0], 200))
    bot.rect(0, 13, 15, 15, ST[2]); bot.rect(0, 13, 15, 13, ST[4]); bot.rect(0, 15, 15, 15, ST[0])
    bot.rect(6, 4, 9, 12, A(FL[0], 220))           # Torso/Beine
    bot.rect(6, 9, 6, 12, A(FL[0], 220)); bot.rect(9, 9, 9, 12, A(FL[0], 220))
    bot.rect(7, 9, 8, 12, A(FL[2], 200))
    top.rect(0, 0, 15, 2, ST[2]); top.rect(0, 2, 15, 2, ST[0]); top.rect(0, 0, 15, 0, ST[4])
    top.rect(6, 8, 9, 15, A(FL[0], 220)); top.rect(5, 9, 5, 14, A(FL[0], 200)); top.rect(10, 9, 10, 14, A(FL[0], 200))
    top.disc(7.5, 6, 1.8, A(FL[0], 230))
    for t, pts in ((bot, [(3, 10), (11, 6), (4, 3)]), (top, [(11, 12), (3, 7), (12, 4)])):
        for x, y in pts:
            t.p(x, y, FL[4])
    return bot, top


# =============================================================================
def build(pk):
    for tier in range(4):
        pk.add('Gehäuse', 'block', f'casing_t{tier}', casing(tier), f'Gehäuse T{tier}')
        pk.add('Gehäuse', 'block', f'casing_t{tier}_top', casing(tier, top=True), f'Gehäuse T{tier} oben')
    pk.add('Gehäuse', 'block', 'casing_t4', casing_t4_frames(), 'Gehäuse T4', frametime=4)

    wt, wf, ws = werkbank()
    pk.add('Maschinen', 'block', 'garagen_werkbank_top', wt, 'Garagen-Werkbank (oben)', lang_key='block.riftverse.garagen_werkbank')
    pk.lang['block.riftverse.garagen_werkbank'] = 'Garagen-Werkbank'
    pk.add('Maschinen', 'block', 'garagen_werkbank_front', wf, 'Garagen-Werkbank (vorne)')
    pk.add('Maschinen', 'block', 'garagen_werkbank_side', ws, 'Garagen-Werkbank (Seite)')

    machines = [
        ('garagenpresse', garagenpresse, 'Garagenpresse', 0),
        ('xenit_schmelze', xenit_schmelze, 'Xenit-Schmelze', 1),
        ('platinen_aetzer', platinen_aetzer, 'Platinen-Ätzer', 1),
        ('kristall_schleifer', kristall_schleifer, 'Kristall-Schleifer', 1),
        ('recycler', recycler, 'Recycler', 1),
        ('fluid_mixer', fluid_mixer, 'Fluid-Mixer', 1),
        ('portalfluid_raffinerie', raffinerie, 'Portalfluid-Raffinerie', 2),
        ('praezisions_montierer', montierer, 'Präzisions-Montierer', 2),
        ('quantenzentrifuge', zentrifuge, 'Quantenzentrifuge', 2),
        ('gravitationspresse', gravitationspresse, 'Gravitationspresse', 2),
        ('dimensions_scanner', scanner, 'Dimensions-Scanner', 2),
        ('chrono_beschleuniger', chrono, 'Chrono-Beschleuniger', 2),
        ('quanten_lithograf', lithograf, 'Quanten-Lithograf', 3),
        ('materie_rekonfigurator', rekonfigurator, 'Materie-Rekonfigurator', 3),
        ('universums_destillator', destillator, 'Universums-Destillator', 3),
    ]
    gens = [
        ('kurbel_dynamo', kurbel_dynamo, 'Kurbel-Dynamo', 0),
        ('biomasse_kessel', kessel, 'Biomasse-Kessel', 1),
        ('fluxkristall_resonator', resonator, 'Fluxkristall-Resonator', 2),
        ('gravitations_pendel', pendel, 'Gravitations-Pendel', 2),
    ]
    pk.machine_meta = {}
    for group, lst in (('Maschinen', machines), ('Generatoren', gens)):
        for tid, fn, name, tier in lst:
            pk.add(group, 'block', tid + '_front', fn(False), name, lang_key=f'block.riftverse.{tid}')
            pk.add(group, 'block', tid + '_front_on', fn(True), name + ' (aktiv)')
            pk.machine_meta[tid] = {'tier': tier, 'on': True}

    pk.add('Maschinen', 'block', 'dimensions_scanner_top', scanner_top(), 'Dimensions-Scanner (oben)')
    kb, kt = klon_tank()
    pk.add('Maschinen', 'block', 'klon_tank_bottom', kb, 'Klon-Tank (unten)', lang_key='block.riftverse.klon_tank')
    pk.add('Maschinen', 'block', 'klon_tank_top', kt, 'Klon-Tank (oben)')
    pk.add('Maschinen', 'block', 'rift_kabelknoten_top', kabelknoten_top(), 'Rift-Kabelknoten (oben)', lang_key='block.riftverse.rift_kabelknoten')
    pk.add('Maschinen', 'block', 'rift_kabelknoten_side', kabelknoten_side(), 'Rift-Kabelknoten (Seite)')
    pk.add('Maschinen', 'block', 'rift_schmiede_front', schmiede(), 'Rift-Schmiede', frametime=3, lang_key='block.riftverse.rift_schmiede')
    pk.add('Maschinen', 'block', 'weltenformer_front', weltenformer(), 'Weltenformer', frametime=5, lang_key='block.riftverse.weltenformer')

    pk.add('Generatoren', 'block', 'tonnenreaktor_side', tonne_side(), 'Tonnenreaktor (Seite)', lang_key='block.riftverse.tonnenreaktor')
    pk.add('Generatoren', 'block', 'tonnenreaktor_top', tonne_top(False), 'Tonnenreaktor (Deckel)')
    pk.add('Generatoren', 'block', 'tonnenreaktor_top_on', tonne_top(True), 'Tonnenreaktor (aktiv)')
    pk.add('Generatoren', 'block', 'zeitschleifen_solar_top', solar_top(), 'Zeitschleifen-Solarpanel', lang_key='block.riftverse.zeitschleifen_solar')
    pk.add('Generatoren', 'block', 'mikrokosmos_zelle_top', zelle_top(False), 'Mikrokosmos-Zelle (oben)', lang_key='block.riftverse.mikrokosmos_zelle')
    pk.add('Generatoren', 'block', 'mikrokosmos_zelle_top_on', zelle_top(True), 'Mikrokosmos-Zelle (aktiv)')
    pk.add('Generatoren', 'block', 'mikrokosmos_zelle_side', zelle_side(), 'Mikrokosmos-Zelle (Seite)')
    pk.add('Generatoren', 'block', 'parallelzapfer_top', zapfer_top(), 'Parallelzapfer (oben)', lang_key='block.riftverse.parallelzapfer')
    pk.add('Generatoren', 'block', 'singularitaetskern_front', singularitaet(), 'Singularitätskern', frametime=2, lang_key='block.riftverse.singularitaetskern')

    pk.add('Speicher', 'block', 'xenit_zelle_side', speicher(1, P, 2), 'Xenit-Zelle', lang_key='block.riftverse.xenit_zelle')
    pk.add('Speicher', 'block', 'flux_zelle_side', speicher(2, G, 4), 'Flux-Zelle', lang_key='block.riftverse.flux_zelle')
    pk.add('Speicher', 'block', 'chrono_speicher_side', speicher(3, V, 5), 'Chrono-Speicher', lang_key='block.riftverse.chrono_speicher')
