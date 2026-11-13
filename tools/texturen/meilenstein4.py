"""Meilenstein 4: T3 Multiversum-Industrie – Gravitit-Leitungen, Umspanner II ↔ III und III ↔ IV, Graphit-Hülle,
Spiegelkristall, Parallelzapfer-Front, aktive Zustände von Chrono-Speicher, Klon-Tank und Kabelknoten und die neuen Items.

T3 heißt Graphit mit Petrol-Leiterbahnen; Portal-Grün bleibt den aktiven Zuständen vorbehalten.
"""
from lib import Tex, R, H, A, rng_for
from materials import stone_base, plate
from machines import casing, speicher, kabelknoten_top, klon_tank, DARK

G, P, W, GR, V = R['green'], R['petrol'], R['white'], R['graphite'], R['violet']
ST, GV, MI, CY, GO = R['steel'], R['grav'], R['mirror'], R['cyan'], R['gold']


# =============================================================================
# Netz
# =============================================================================
def gravitit_leitung():
    """Blank: schwarzblaue Gravitit-Litze mit Stahladern."""
    t = Tex()
    for y in range(16):
        for x in range(16):
            k = (x + 2 * y) % 5
            t.p(x, y, GV[[1, 2, 3, 2, 4][k]])
    for i in range(0, 16, 4):
        t.p(i, (i + 1) % 16, ST[3])
        t.p((i + 2) % 16, (i + 3) % 16, ST[4])
    return t


def gravitit_isoliert(doppelt):
    """Dunkle Folie mit hellen Streifen (Tint: Gravitit-Blau oder Farbstoff); doppelt isoliert mit zweiter Lage."""
    t = Tex(fill=GR[1])
    rng = rng_for('grav_iso_d' if doppelt else 'grav_iso')
    t.noise(GR, (0.1, 0.4, 0.4, 0.1, 0), rng, clump=0.6)
    step = 6 if doppelt else 8
    for y in range(16):
        for x in range(16):
            k = (x + y) % step
            if k == 0:
                t.p(x, y, H('#e8e8e8'))
            elif k == 1:
                t.p(x, y, H('#b0b0b0'))
            elif k == 4 and not doppelt:
                t.p(x, y, GV[3])
    if doppelt:
        t.frame(0, 0, 15, 15, GR[0])
    return t


def umspanner_side(tier):
    """Graphit mit zwei Spulenwicklungen; die zweite Spule glänzt in der Farbe der höheren Stufe."""
    t = casing(3)
    hi = R['grav'] if tier == 3 else R['copper']
    for cx, ramp in ((4, R['copper']), (11, hi)):
        t.rect(cx - 2, 3, cx + 1, 12, ramp[2])
        for y in range(3, 13, 2):
            t.rect(cx - 2, y, cx + 1, y, ramp[3])
        t.rect(cx - 2, 3, cx + 1, 3, ST[3]); t.rect(cx - 2, 12, cx + 1, 12, ST[1])
    t.rect(7, 5, 8, 10, ST[2])
    return t


def umspanner_front(tier, hoch):
    """Hochspannungsseite mit Stufen-Ziffer (II oder III); spannt der Umspanner hoch, leuchtet sie grün."""
    t = casing(3)
    t.rect(2, 2, 13, 13, GR[0])
    t.bevel(2, 2, 13, 13, GR[1], GR[3])
    for cx, cy in [(4, 4), (11, 4), (4, 11), (11, 11)]:
        c = G[3] if hoch else ST[3]
        t.p(cx, cy, c)
    digit = G[3] if hoch else R['orange'][2]
    if tier == 2:      # II ↔ III: Ziffer der höheren Stufe, „III“
        for x in (5, 7, 9):
            t.rect(x, 5, x, 10, digit)
        t.rect(4, 5, 10, 5, digit); t.rect(4, 10, 10, 10, digit)
    else:              # III ↔ IV: „IV“
        t.rect(4, 5, 4, 10, digit); t.rect(3, 5, 5, 5, digit); t.rect(3, 10, 5, 10, digit)
        t.line(7, 5, 9, 10, digit); t.line(11, 5, 9, 10, digit)
    return t


# =============================================================================
# Gehäuse, Welten
# =============================================================================
def graphit_huelle():
    """Multiblock-Hülle: Graphit mit Fachwerk aus Stahl, ohne Leiterbahnen."""
    rng = rng_for('graphit_huelle')
    t = Tex()
    t.noise(GR, (0.05, 0.25, 0.55, 0.15, 0), rng, clump=0.7)
    t.bevel(0, 0, 15, 15, GR[4], GR[0])
    t.frame(2, 2, 13, 13, ST[1])
    t.line(2, 2, 13, 13, ST[2])
    t.line(13, 2, 2, 13, ST[1])
    for x, y in [(2, 2), (13, 2), (2, 13), (13, 13)]:
        t.p(x, y, P[3])
    return t


def spiegelkristall():
    """Heller Stein mit eingewachsenen Spiegelscherben, die das Licht verkehrt herum zurückwerfen."""
    t = stone_base('spiegelkristall', MI, (0.05, 0.25, 0.4, 0.25, 0.05))
    for pts in ([(3, 3), (4, 3), (3, 4), (4, 4), (5, 5)], [(10, 2), (11, 2), (11, 3), (12, 3)],
                [(9, 9), (10, 9), (10, 10), (11, 10), (11, 11), (12, 12)], [(2, 11), (3, 11), (3, 12)]):
        for i, (x, y) in enumerate(pts):
            t.p(x, y, H('#ffffff') if i == 0 else CY[3] if i % 2 else MI[4])
    t.p(4, 3, V[3]); t.p(11, 10, V[3])
    return t


# =============================================================================
# Maschinen (aktive Zustände und Fronten)
# =============================================================================
def chrono_speicher_on():
    t = speicher(3, V, 5)
    for y in (1, 14):
        t.rect(6, y, 9, y, G[3])
    return t


def zapfer_front(on):
    """Steuerblock des Parallelzapfers: Antennen-Raster über einem Sichtfenster auf den Riss."""
    t = casing(3)
    t.rect(4, 4, 11, 11, DARK)
    t.frame(3, 3, 12, 12, ST[1])
    for y in range(5, 11):
        t.p(7 + (y % 2), y, G[3] if on else R['null'][3])
    if on:
        t.p(7, 7, G[4]); t.p(8, 8, G[4])
        t.line(4, 4, 6, 6, CY[4]); t.line(11, 11, 9, 9, CY[4])
    for x in (2, 13):
        t.rect(x, 2, x, 13, ST[2])
        t.p(x, 2, ST[4])
    t.p(3, 13, G[4] if on else R['yellow'][2]); t.p(4, 13, G[2] if on else R['yellow'][1])
    return t


def kabelknoten_top_on():
    t = kabelknoten_top()
    for (x, y) in [(5, 4), (6, 6), (9, 9), (10, 11), (8, 7)]:
        t.p(x, y, G[4])
    t.ring(7.5, 7.5, 3.2, A(G[3], 180), 0.4)
    return t


def klon_tank_on():
    """Aktiv: die Flüssigkeit leuchtet heller und blubbert stärker."""
    bot, top = klon_tank()
    for t, pts in ((bot, [(3, 4), (12, 9), (4, 7), (11, 3), (3, 11)]), (top, [(4, 4), (11, 9), (3, 12), (12, 2), (11, 13)])):
        for x, y in pts:
            t.p(x, y, G[4])
        t.rect(2, 0, 2, 15, A(G[4], 120))
    return bot, top


# =============================================================================
# Items
# =============================================================================
def klon_tank_item():
    t = Tex()
    t.rect(4, 1, 11, 2, ST[3]); t.rect(4, 13, 11, 14, ST[2]); t.rect(4, 14, 11, 14, ST[0])
    t.rect(5, 3, 10, 12, A(G[2], 200))
    t.rect(5, 3, 5, 12, A(H('#ffffff'), 140))
    t.rect(7, 6, 8, 11, A(G[0], 230)); t.disc(7.5, 5, 1.2, A(G[0], 230))
    t.p(9, 4, G[4]); t.p(6, 10, G[4])
    t.outline()
    return t


def graphitplatte():
    t = plate(R['coal'])
    for x, y in [(5, 6), (6, 6), (9, 9), (10, 9), (7, 10)]:
        t.p(x, y, P[3])
    return t


def fragment_replik():
    """Nachgebautes Story-Fragment: Spiegelscherbe, mit Draht geflickt, ein grüner Riss."""
    t = Tex()
    t.sprite(["......oo......",
              ".....o44o.....",
              "....o4433o....",
              "...o443332o...",
              "..o4433322o...",
              "..o4333221o...",
              "...o332211o...",
              "...o32211o....",
              "....o211o.....",
              ".....o1o......",
              "......o......."],
             {'o': H('#2a3036'), '1': MI[0], '2': MI[1], '3': MI[2], '4': MI[4]}, 1, 2)
    t.line(5, 4, 9, 10, G[3]); t.p(7, 7, G[4])
    t.line(4, 9, 10, 5, GO[2])
    t.outline()
    return t


def kartusche_konzentrat():
    """Wie die stabile Kartusche, gefüllt mit dichtem, cyanfarbenem Konzentrat."""
    t = Tex()
    t.rect(4, 1, 11, 2, ST[3]); t.rect(4, 13, 11, 14, ST[1]); t.rect(4, 14, 11, 14, ST[0])
    t.rect(5, 3, 10, 12, A(H('#cdeef2'), 110))
    t.rect(5, 3, 10, 12, CY[1]); t.rect(5, 3, 10, 3, CY[4]); t.p(6, 6, CY[4]); t.p(9, 9, CY[3]); t.p(7, 11, G[3])
    t.rect(4, 3, 4, 12, ST[2]); t.rect(11, 3, 11, 12, ST[1])
    t.rect(6, 1, 9, 1, CY[3])                 # cyanfarbener Deckel: Konzentrat
    t.outline()
    return t


def kopplungskarte():
    """Karte mit zwei Antennen-Symbolen, die ein grüner Riss verbindet."""
    t = Tex()
    t.rect(2, 3, 13, 12, H('#d8d2c0'))
    t.bevel(2, 3, 13, 12, H('#f0ecde'), H('#a8a090'))
    for cx in (4, 11):
        t.rect(cx, 6, cx, 10, ST[1]); t.p(cx, 5, G[3])
        t.p(cx - 1, 10, ST[1]); t.p(cx + 1, 10, ST[1])
    for x in range(5, 11):
        t.p(x, 7 + (x % 2), G[2])
    t.outline()
    return t


# =============================================================================
def build(pk):
    cat = 'Meilenstein 4'
    pk.add(cat, 'block', 'gravitit_leitung', gravitit_leitung(), 'Gravitit-Leitung')
    pk.add(cat, 'block', 'gravitit_leitung_isoliert', gravitit_isoliert(False), 'Gravitit-Leitung (isoliert)')
    pk.add(cat, 'block', 'gravitit_leitung_doppelt', gravitit_isoliert(True), 'Gravitit-Leitung (2× isoliert)')
    pk.add(cat, 'item', 'gravitit_leitung', gravitit_leitung(), 'Gravitit-Leitung (Item)')
    pk.add(cat, 'item', 'gravitit_leitung_isoliert', gravitit_isoliert(False), 'Gravitit-Leitung isoliert (Item)')
    pk.add(cat, 'item', 'gravitit_leitung_doppelt', gravitit_isoliert(True), 'Gravitit-Leitung 2× isoliert (Item)')
    for tier, name in ((2, 'umspanner_ii'), (3, 'umspanner_iii')):
        label = 'II ↔ III' if tier == 2 else 'III ↔ IV'
        pk.add(cat, 'block', name + '_side', umspanner_side(tier), f'Umspanner {label} (Seite)')
        pk.add(cat, 'block', name + '_front', umspanner_front(tier, False), f'Umspanner {label} (runter)')
        pk.add(cat, 'block', name + '_front_hoch', umspanner_front(tier, True), f'Umspanner {label} (hoch)')
    pk.add(cat, 'block', 'graphit_huelle', graphit_huelle(), 'Graphit-Hülle')
    pk.add(cat, 'block', 'spiegelkristall', spiegelkristall(), 'Spiegelkristall')
    pk.add(cat, 'block', 'chrono_speicher_side_on', chrono_speicher_on(), 'Chrono-Speicher (aktiv)')
    pk.add(cat, 'block', 'parallelzapfer_front', zapfer_front(False), 'Parallelzapfer (Front)')
    pk.add(cat, 'block', 'parallelzapfer_front_on', zapfer_front(True), 'Parallelzapfer (aktiv)')
    pk.add(cat, 'block', 'rift_kabelknoten_top_on', kabelknoten_top_on(), 'Rift-Kabelknoten (aktiv)')
    kb, kt = klon_tank_on()
    pk.add(cat, 'block', 'klon_tank_bottom_on', kb, 'Klon-Tank unten (aktiv)')
    pk.add(cat, 'block', 'klon_tank_top_on', kt, 'Klon-Tank oben (aktiv)')

    pk.add(cat, 'item', 'klon_tank', klon_tank_item(), 'Klon-Tank (Item)')
    pk.add(cat, 'item', 'graphitplatte', graphitplatte(), 'Graphitplatte')
    pk.add(cat, 'item', 'fragment_replik', fragment_replik(), 'Fragment-Replik')
    pk.add(cat, 'item', 'fluid_kartusche_konzentrat', kartusche_konzentrat(), 'Fluid-Kartusche (Konzentrat)')
    pk.add(cat, 'item', 'kopplungskarte', kopplungskarte(), 'Kopplungskarte')
