"""Meilenstein 2: Meteoritenkrater, Meteoritenpeiler, Kristallwelt, Säure, Rift-Anker, Xenit-Zelle, Schrott-Golem.

Baut auf denselben Vorlagen auf wie materials.py und machines.py (Design-Leitfaden: matte Flächen,
Portal-Grün nur für "aktiv" und Leuchten).
"""
import math

from lib import Tex, R, T, H, A, OUT, BLACK, rng_for
from materials import stone_base, fluid_frames
from machines import casing, DARK

G, P, W, GR, V = R['green'], R['petrol'], R['white'], R['graphite'], R['violet']
ST, RU, CU, AC, NU = R['steel'], R['rust'], R['copper'], R['acid'], R['null']
IRON = R['iron']


# =============================================================================
# Blöcke
# =============================================================================
def meteoritenkern():
    """Verschmolzener schwarzer Stein mit Petrol-Adern, dazwischen schwach grün glühend."""
    rng = rng_for('meteoritenkern')
    t = Tex()
    t.noise(GR, (0.3, 0.4, 0.22, 0.08, 0), rng, clump=0.55)
    veins = [[(0, 4), (3, 5), (6, 4), (9, 7), (12, 6), (15, 8)],
             [(2, 15), (4, 11), (7, 10), (8, 13), (11, 12), (14, 15)],
             [(5, 0), (6, 2), (5, 4)], [(13, 0), (12, 3), (14, 5)]]
    for path in veins:
        for a, b in zip(path, path[1:]):
            t.line(a[0], a[1], b[0], b[1], P[2])
        for x, y in path[1:-1]:
            t.p(x, y, P[3])
    for x, y in [(6, 4), (9, 7), (7, 10), (11, 12), (12, 3)]:
        t.p(x, y, G[2])
    t.p(9, 7, G[3]); t.p(7, 10, G[3])
    t.bevel(0, 0, 15, 15, GR[3], NU[0])
    return t


def knospender_fluxkristall():
    """Wie knospender Amethyst: grüner Kristallgrund mit dunklen Knospenlöchern."""
    rng = rng_for('knospe')
    t = Tex()
    t.noise(G, (0.25, 0.4, 0.25, 0.1, 0), rng, clump=0.5)
    for cx, cy in [(3, 3), (11, 2), (7, 8), (2, 11), (12, 11), (8, 14)]:
        t.p(cx, cy, H('#06301a')); t.p(cx + 1, cy, H('#06301a')); t.p(cx, cy + 1, G[0])
        t.p(cx + 1, cy - 1, G[4])
    t.bevel(0, 0, 15, 15, G[3], G[0])
    return t


def fluxkristall_cluster():
    """Kreuzmodell: drei Kristallspitzen, die mittlere am höchsten."""
    t = Tex()
    spikes = [(7.5, 1, 2.6), (3.5, 6, 1.8), (12, 4, 2.0)]
    for cx, top, half in spikes:
        for y in range(top, 16):
            k = (y - top) / max(1, 15 - top)
            w = max(0.6, half * min(1.0, k * 1.6))
            for x in range(16):
                dx = x + 0.5 - cx
                if abs(dx) <= w:
                    if dx < -w * 0.3:
                        c = G[4] if y < top + 3 else G[3]
                    elif dx > w * 0.4:
                        c = G[1]
                    else:
                        c = G[2] if y > top + 2 else G[3]
                    t.p(x, y, c)
    t.p(7, 1, G[4]); t.p(3, 6, G[4]); t.p(12, 4, G[4])
    return t


def rift_anker_pfahl():
    t = Tex()
    for x in range(16):
        c = IRON[[2, 3, 3, 2, 1, 2, 3, 4][x % 8]]
        for y in range(16):
            t.p(x, y, c)
    for y in (3, 9, 15):
        t.rect(0, y, 15, y, IRON[1])
    for x, y in [(2, 1), (10, 6), (5, 12)]:
        t.p(x, y, IRON[4])
    return t


def rift_anker_linse():
    t = Tex(fill=G[2])
    t.disc(7.5, 7.5, 7.2, G[1])
    t.disc(7.5, 7.5, 5.6, G[2])
    t.disc(6.0, 6.0, 3.0, G[3])
    t.disc(5.0, 5.0, 1.4, G[4])
    t.frame(0, 0, 15, 15, P[2])
    return t


def xenit_zelle_on():
    """Wie die Xenit-Zelle, aber mit grün leuchtendem Füllstand (beim Laden oder Entladen)."""
    t = casing(1)
    t.rect(6, 2, 9, 13, DARK)
    t.frame(5, 1, 10, 14, P[1])
    for i in range(5):
        y = 12 - i * 2
        t.rect(6, y, 9, y, G[2]); t.p(6, y, G[4])
    t.p(12, 2, G[4])
    return t


# =============================================================================
# Items
# =============================================================================
def peiler(frame, zone):
    """Kompass im Kupfergehäuse, Nadel aus Xenit; in der Xenit-Zone leuchtet sie grün."""
    t = Tex()
    t.disc(7.5, 7.5, 6.9, CU[1])
    t.disc(7.5, 7.5, 6.2, CU[2])
    t.ring(7.5, 7.5, 6.4, CU[3], 0.45)
    t.disc(7.5, 7.5, 4.9, GR[1] if not zone else H('#0a2a18'))
    for k in range(8):   # Skalenstriche
        a = k * math.pi / 4
        t.p(int(round(7.5 + 4.3 * math.sin(a))), int(round(7.5 - 4.3 * math.cos(a))), GR[3] if not zone else G[1])
    a = frame * 2 * math.pi / 16
    tip_x, tip_y = 7.5 + 4.0 * math.sin(a), 7.5 - 4.0 * math.cos(a)
    tail_x, tail_y = 7.5 - 2.4 * math.sin(a), 7.5 + 2.4 * math.cos(a)
    head = (G[4], G[3]) if zone else (P[4], P[3])
    t.line(int(round(tail_x)), int(round(tail_y)), 7 if tip_x < 7.5 else 8, 7 if tip_y < 7.5 else 8, ST[2])
    t.line(7 if tip_x < 7.5 else 8, 7 if tip_y < 7.5 else 8, int(round(tip_x)), int(round(tip_y)), head[1])
    t.p(int(round(tip_x)), int(round(tip_y)), head[0])
    t.p(7, 7, ST[4]); t.p(8, 8, ST[1])
    t.p(4, 3, CU[4]); t.p(3, 4, CU[4])
    t.outline()
    return t


def saeure_eimer():
    rows = [
        "................",
        "................",
        "...oooooooooo...",
        "..oSFFFFFFFFSo..",
        "..oSfffffffFSo..",
        "..ossSSSSSSsso..",
        "..osSSSSSSSsso..",
        "...osSSSSSsso...",
        "...osSSSSSsso...",
        "...osSSSSSsso...",
        "....osSSSsso....",
        "....osssssso....",
        ".....oooooo.....",
    ]
    t = Tex()
    t.sprite([r.ljust(16, '.') for r in rows], {'o': OUT, 'S': ST[3], 's': ST[2], 'F': AC[4], 'f': AC[2]}, 0, 1)
    t.p(6, 4, AC[3]); t.p(9, 4, AC[3])
    return t


def rift_anker_item():
    t = Tex()
    t.rect(4, 13, 11, 14, IRON[2]); t.rect(4, 13, 11, 13, IRON[4]); t.rect(4, 14, 11, 14, IRON[1])
    t.rect(7, 5, 8, 12, IRON[3]); t.rect(8, 5, 8, 12, IRON[1])
    t.disc(7.5, 3.5, 2.8, G[2])
    t.disc(7.0, 3.0, 1.2, G[4])
    t.ring(7.5, 3.5, 2.9, G[0], 0.45)
    t.outline()
    return t


# =============================================================================
# Kreaturen
# =============================================================================
def schrott_golem():
    """Textur im Raster des Eisengolem-Modells (128 × 128): Rost, Blech, Nieten, ein grünes Auge."""
    rng = rng_for('schrott_golem')
    t = Tex(128, 128)
    metals = [RU, RU, ST, CU, R['brass']]
    for by in range(0, 128, 8):
        for bx in range(0, 128, 8):
            ramp = metals[rng.randrange(len(metals))]
            t.noise(ramp, (0.1, 0.3, 0.4, 0.15, 0.05), rng, bx, by, bx + 7, by + 7, clump=0.5)
            t.p(bx + 1, by + 1, ramp[4])
            if rng.random() < 0.35:
                t.p(bx + 6, by + 6, ST[4])   # Niete
    # Kopf vorne (8..15, 8..17): Stirn, ein großes grünes Auge, ein kaputtes
    t.rect(8, 8, 15, 10, RU[1])
    t.rect(9, 11, 15, 11, GR[0])
    t.rect(9, 12, 10, 13, G[4]); t.p(9, 12, H('#ffffff'))
    t.rect(13, 12, 14, 13, GR[1]); t.p(14, 13, G[1])
    t.rect(9, 15, 14, 15, GR[0])
    # Nase (26..27, 2..5)
    t.rect(26, 2, 27, 5, ST[2])
    return t


# =============================================================================
def build(pk):
    cat = 'Meilenstein 2'
    blocks = [
        ('meteoritenkern', meteoritenkern(), 'Meteoritenkern'),
        ('knospender_fluxkristall', knospender_fluxkristall(), 'Knospender Fluxkristall'),
        ('fluxkristall_cluster', fluxkristall_cluster(), 'Fluxkristall-Cluster'),
        ('rift_anker_pfahl', rift_anker_pfahl(), 'Rift-Anker (Pfahl)'),
        ('rift_anker_linse', rift_anker_linse(), 'Rift-Anker (Linse)'),
        ('xenit_zelle_side_on', xenit_zelle_on(), 'Xenit-Zelle (aktiv)'),
    ]
    for tid, tex, name in blocks:
        pk.add(cat, 'block', tid, tex, name)
    pk.add(cat, 'block', 'saeure_still', fluid_frames(AC, 16, False), 'Säure', frametime=2)
    pk.add(cat, 'block', 'saeure_flow', fluid_frames(AC, 32, True), 'Säure (fließend)', frametime=2)

    pk.add(cat, 'item', 'saeure_eimer', saeure_eimer(), 'Säureeimer')
    pk.add(cat, 'item', 'rift_anker', rift_anker_item(), 'Rift-Anker')
    for zone in (False, True):
        for k in range(16):
            name = f'meteoritenpeiler_{"zone_" if zone else ""}{k:02d}'
            label = 'Meteoritenpeiler' + (' (Zone)' if zone else '') + f' {k}'
            pk.add(cat, 'item', name, peiler(k, zone), label)
    pk.add(cat, 'entity', 'schrott_golem', schrott_golem(), 'Schrott-Golem (Textur)')
