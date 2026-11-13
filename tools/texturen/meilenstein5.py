"""Meilenstein 5: T4 Weltenformer – Null-Leitung, Null-Hülle, Eindämmungsring, Leerstein, Nullmaterie-Erz und die
neuen Items (Nullmaterie-Faser, Nullmaterie-Kern, Kartusche mit Weltenformer-Essenz).

T4 heißt Null-Schwarz mit wanderndem Sternenfeld und grünen Risskanten: Der Block wirkt wie ein Loch ins All.
"""
import math

from lib import Tex, R, H, A, rng_for
from materials import null_frames

G, ST, NU, V, CY = R['green'], R['steel'], R['null'], R['violet'], R['cyan']


# =============================================================================
# Netz und Hüllen
# =============================================================================
def null_leitung():
    """Schwarze Faser mit Sternpunkten und grünen Kanten (Kabeltextur, wird auf den 4-px-Kern gemappt)."""
    frames = null_frames(seed='null_leitung', edge=False)
    for t in frames:
        for i in range(16):
            t.p(i, 6, G[1]); t.p(i, 9, G[1])
            t.p(6, i, G[1]); t.p(9, i, G[1])
        for i in range(0, 16, 5):
            t.p(i, 6, G[3]); t.p(9, (i + 2) % 16, G[3])
    return frames


def null_leitung_item():
    """Ein Stück Null-Leitung schräg im Bild: dünne schwarze Faser mit grünen Kanten und Sternpunkten."""
    t = Tex()
    for i in range(12):
        x, y = 2 + i, 13 - i
        t.p(x, y, NU[1]); t.p(x + 1, y, NU[2]); t.p(x, y - 1, G[2]); t.p(x + 1, y + 1, G[1])
    for x, y in [(5, 10), (9, 6), (12, 3)]:
        t.p(x, y, H('#ffffff'))
    t.rect(1, 13, 2, 14, ST[2]); t.rect(13, 1, 14, 2, ST[2])
    t.outline()
    return t


def null_huelle():
    """Hülle des Weltenformer-Rings: Null-Schwarz mit Fachwerk aus Stahl, grüne Nieten an den Knoten."""
    frames = null_frames(seed='null_huelle', edge=False)
    for t in frames:
        t.bevel(0, 0, 15, 15, NU[4], NU[0])
        t.frame(2, 2, 13, 13, ST[0])
        t.line(2, 2, 13, 13, ST[1])
        t.line(13, 2, 2, 13, ST[0])
        for x, y in [(2, 2), (13, 2), (2, 13), (13, 13)]:
            t.p(x, y, G[3])
    return frames


def eindaemmungsring():
    """Grün leuchtendes Ringsegment: Feldspulen in einer Null-Fassung, das Feld pulsiert."""
    out = []
    for f in range(8):
        t = Tex(fill=NU[1])
        t.bevel(0, 0, 15, 15, NU[3], NU[0])
        b = 0.5 + 0.5 * math.sin(f / 8 * 2 * math.pi)
        for y in (3, 7, 11):
            t.rect(1, y, 14, y + 1, ST[1])
            for x in range(2, 14, 2):
                t.p(x, y, ST[3])
        glow = G[4] if b > 0.7 else G[3] if b > 0.35 else G[2]
        for y in (5, 9, 13):
            t.rect(1, y, 14, y, glow)
        t.rect(1, 1, 14, 1, G[1])
        out.append(t)
    return out


# =============================================================================
# Leere Welt
# =============================================================================
def leerstein():
    """Gestein der schwebenden Kerne: fast schwarz, ein paar starre Sternpunkte."""
    rng = rng_for('leerstein')
    t = Tex(fill=NU[1])
    t.noise(NU, (0.25, 0.4, 0.25, 0.1, 0), rng, clump=0.6)
    for _ in range(7):
        x, y = rng.randrange(16), rng.randrange(16)
        t.p(x, y, NU[4] if rng.random() < 0.6 else H('#d8d8ff'))
    return t


def nullmaterie_erz():
    """Leerstein mit eingeschlossenen Nullmaterie-Knoten: tiefschwarz mit grünem Rand und wandernden Sternen."""
    base = leerstein()
    frames = []
    rng = rng_for('nullerz')
    knots = [(3, 4, 2), (10, 3, 2), (7, 10, 3), (12, 11, 1)]
    for f in range(8):
        t = base.copy()
        for cx, cy, r in knots:
            t.disc(cx, cy, r + 0.6, G[1])
            t.disc(cx, cy, r, NU[0])
            ang = f / 8 * 2 * math.pi + cx
            sx, sy = int(round(cx + math.cos(ang) * (r - 0.6))), int(round(cy + math.sin(ang) * (r - 0.6)))
            t.p(sx, sy, H('#ffffff'))
        if f % 4 == 0:
            t.p(rng.randrange(16), rng.randrange(16), G[3])
        frames.append(t)
    return frames


# =============================================================================
# Items
# =============================================================================
def nullmaterie_faser():
    """Ein gewickeltes Bündel schwarzer Fasern mit grünem Schimmer."""
    t = Tex()
    for i in range(10):
        x0, y0 = 3 + i // 3, 12 - i
        t.line(x0, y0, x0 + 6, y0 - 4, NU[2 + (i % 3)])
    t.line(3, 12, 12, 3, G[2])
    t.line(4, 13, 13, 4, NU[1])
    for x, y in [(5, 10), (8, 7), (11, 4)]:
        t.p(x, y, H('#ffffff'))
    t.rect(6, 8, 8, 9, ST[2])
    t.outline()
    return t


def nullmaterie_kern():
    """Ein Kern aus verdichteter Nullmaterie in einem Gravitit-Käfig, die Sterne darin kreisen."""
    out = []
    for f in range(8):
        t = Tex()
        t.disc(7.5, 7.5, 5.8, G[1])
        t.disc(7.5, 7.5, 5.0, NU[0])
        ph = f / 8 * 2 * math.pi
        for k in range(5):
            a = ph + k * 1.26
            r = 1.5 + (k % 3) * 1.1
            t.p(int(round(7.5 + r * math.cos(a))), int(round(7.5 + r * math.sin(a))), H('#ffffff') if k % 2 else NU[4])
        for x in (2, 13):
            t.rect(x, 3, x, 12, R['grav'][2])
        for y in (2, 13):
            t.rect(3, y, 12, y, R['grav'][3])
        t.outline()
        out.append(t)
    return out


def kartusche_essenz():
    """Kartusche mit Weltenformer-Essenz: schwarz-violett mit Sternpunkten, Deckel in Portal-Grün."""
    out = []
    for f in range(4):
        t = Tex()
        t.rect(4, 1, 11, 2, ST[3]); t.rect(4, 13, 11, 14, ST[1]); t.rect(4, 14, 11, 14, ST[0])
        t.rect(5, 3, 10, 12, NU[1])
        t.rect(5, 3, 10, 3, V[3])
        for i, (x, y) in enumerate([(6, 5), (9, 7), (7, 9), (8, 11), (6, 11)]):
            t.p(x, y, H('#ffffff') if (i + f) % 3 == 0 else V[4])
        t.rect(4, 3, 4, 12, ST[2]); t.rect(11, 3, 11, 12, ST[1])
        t.rect(6, 1, 9, 1, G[3])
        t.outline()
        out.append(t)
    return out


# =============================================================================
def build(pk):
    cat = 'Meilenstein 5'
    leitung = null_leitung()
    pk.add(cat, 'block', 'null_leitung', leitung, 'Null-Leitung', frametime=4)
    pk.add(cat, 'item', 'null_leitung', null_leitung_item(), 'Null-Leitung (Item)')
    pk.add(cat, 'block', 'null_huelle', null_huelle(), 'Null-Hülle', frametime=4)
    pk.add(cat, 'block', 'eindaemmungsring', eindaemmungsring(), 'Eindämmungsring', frametime=3)
    pk.add(cat, 'block', 'leerstein', leerstein(), 'Leerstein')
    pk.add(cat, 'block', 'nullmaterie_erz', nullmaterie_erz(), 'Nullmaterie-Erz', frametime=5)
    pk.add(cat, 'item', 'nullmaterie_faser', nullmaterie_faser(), 'Nullmaterie-Faser')
    pk.add(cat, 'item', 'nullmaterie_kern', nullmaterie_kern(), 'Nullmaterie-Kern', frametime=3)
    pk.add(cat, 'item', 'fluid_kartusche_essenz', kartusche_essenz(), 'Fluid-Kartusche (Essenz)', frametime=6)
