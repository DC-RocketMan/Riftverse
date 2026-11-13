"""Meilenstein 3: T2 Rift-Labor – Flux-Leitung, Umspanner, Flux-Zelle, Labor-Hülle, Resonator-Kammer,
Schwebstein-Kern, Chronoglas-Sand, Biomasse, fressende Flora und die neuen Bauteile.

Baut auf den Vorlagen aus materials.py, machines.py und gadgets.py auf. T2 heißt Labor-Weiß mit
abgeschrägten Kanten; Portal-Grün bleibt den aktiven Zuständen vorbehalten.
"""
import math

from lib import Tex, R, T, H, A, OUT, rng_for
from materials import stone_base, plate, vial, coil
from machines import casing, lamp, speicher, DARK

G, P, W, GR, V = R['green'], R['petrol'], R['white'], R['graphite'], R['violet']
ST, CU, GO, FL, MI, TI = R['steel'], R['copper'], R['gold'], R['flesh'], R['mirror'], R['tin']


# =============================================================================
# Netz
# =============================================================================
def flux_leitung():
    """Blanker Golddraht mit grünem Schimmer (Fluxstaub in der Litze)."""
    t = Tex()
    for y in range(16):
        for x in range(16):
            k = (x + y) % 4
            t.p(x, y, GO[[1, 2, 3, 4][k]])
    for i in range(0, 16, 4):
        t.p(i, (i + 2) % 16, G[3])
        t.p((i + 1) % 16, (i + 3) % 16, G[2])
    return t


def flux_leitung_isoliert():
    """Dunkle Folie, doppelte Streifen (Tint: Labor-Weiß oder Farbstoff), goldene Kante."""
    t = Tex(fill=GR[2])
    rng = rng_for('flux_iso')
    t.noise(GR, (0, 0.3, 0.5, 0.2, 0), rng, clump=0.6)
    for y in range(16):
        for x in range(16):
            k = (x + y) % 8
            if k in (0, 1):
                t.p(x, y, H('#e0e0e0') if k == 0 else H('#b8b8b8'))
            elif k == 4:
                t.p(x, y, GO[2])
    return t


def umspanner_side():
    """Labor-Weiß mit zwei Spulenwicklungen und Kühlrippen."""
    t = casing(2)
    for cx in (4, 11):
        t.rect(cx - 2, 3, cx + 1, 12, CU[2])
        for y in range(3, 13, 2):
            t.rect(cx - 2, y, cx + 1, y, CU[3])
        t.rect(cx - 2, 3, cx + 1, 3, ST[3]); t.rect(cx - 2, 12, cx + 1, 12, ST[1])
    t.rect(7, 5, 8, 10, ST[2])
    return t


def umspanner_front(hoch):
    """Hochspannungsseite: Punkte wie in IC2; spannt der Umspanner hoch, leuchten sie grün."""
    t = casing(2)
    t.rect(3, 3, 12, 12, GR[1])
    t.bevel(3, 3, 12, 12, GR[0], GR[3])
    for cx, cy in [(5, 5), (10, 5), (5, 10), (10, 10)]:
        c = G[3] if hoch else ST[3]
        t.rect(cx - 1, cy - 1, cx, cy, c)
        t.p(cx - 1, cy - 1, G[4] if hoch else ST[4])
    t.rect(7, 7, 8, 8, R['orange'][2] if not hoch else G[2])
    return t


def flux_zelle_side(band):
    """Füllstandsband 0–4 (leer … voll)."""
    return speicher(2, G, [0, 2, 3, 5, 6][band])


def labor_huelle():
    """Struktur der Multiblöcke: Labor-Weiß mit Fachwerk-Stahl."""
    t = casing(2, top=False)
    t.rect(3, 13, 12, 13, W[2])          # kein grüner Streifen: reine Hülle
    t.frame(2, 2, 13, 13, ST[2])
    t.line(2, 2, 13, 13, ST[3])
    t.line(13, 2, 2, 13, ST[1])
    for x, y in [(2, 2), (13, 2), (2, 13), (13, 13)]:
        t.p(x, y, ST[4])
    return t


def resonator_kammer():
    """Kammer mit Sichtfenster auf die Kristallstäbe."""
    t = casing(2)
    t.rect(3, 3, 12, 12, DARK)
    t.frame(2, 2, 13, 13, ST[1])
    for x in (4, 7, 10):
        t.rect(x, 4, x + 1, 11, G[1])
        t.rect(x, 4, x, 11, G[2])
        t.p(x, 4, G[4])
    t.p(3, 3, A(H('#ffffff'), 90))
    return t


def schwebstein_kern():
    """Polierter Schwebstein-Block mit Stahlbändern: das Gewicht des Pendels."""
    t = stone_base('schwebstein_kern', MI, (0.02, 0.2, 0.4, 0.3, 0.08))
    t.rect(0, 3, 15, 4, ST[2]); t.rect(0, 3, 15, 3, ST[3])
    t.rect(0, 11, 15, 12, ST[2]); t.rect(0, 11, 15, 11, ST[3])
    t.disc(7.5, 7.5, 2.2, W[4])
    t.disc(7.5, 7.5, 1.1, R['sky'][3])
    t.bevel(0, 0, 15, 15, W[4], MI[0])
    return t


# =============================================================================
# Welten
# =============================================================================
def chronoglas_sand():
    """Violetter Sand mit funkelnden Uhrglas-Körnern."""
    rng = rng_for('chronoglas_sand')
    t = Tex()
    t.noise(V, (0.05, 0.25, 0.4, 0.25, 0.05), rng, clump=0.35)
    for _ in range(6):
        x, y = rng.randrange(16), rng.randrange(16)
        t.p(x, y, H('#f4ecff'))
    return t


def biomasse_frames():
    """Rosa-oranger Klumpen mit dunklen Adern; der Boden „atmet“ (pulsiert in 8 Bildern)."""
    rng = rng_for('biomasse')
    base = Tex()
    base.noise(FL, (0.05, 0.25, 0.45, 0.2, 0.05), rng, clump=0.55)
    veins = [[(0, 5), (4, 6), (7, 4), (11, 6), (15, 5)], [(2, 15), (4, 11), (8, 10), (12, 12), (14, 15)],
             [(7, 4), (8, 8), (8, 10)]]
    for path in veins:
        for a, b in zip(path, path[1:]):
            base.line(a[0], a[1], b[0], b[1], FL[0])
    frames = []
    for f in range(8):
        t = base.copy()
        s = (math.sin(f / 8 * 2 * math.pi) + 1) / 2
        r = 1.5 + 1.6 * s
        for cx, cy in [(4, 9), (11, 3), (12, 10)]:
            for y in range(16):
                for x in range(16):
                    d = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
                    if d <= r:
                        t.p(x, y, FL[4] if d < r * 0.4 else FL[3])
        frames.append(t)
    return frames


def fressende_flora():
    """Kreuzmodell: ein Maul aus zwei Blättern mit Zähnen auf einem dicken Stiel."""
    t = Tex()
    LE = R['red']
    t.rect(7, 9, 8, 15, R['acid'][1]); t.p(7, 9, R['acid'][2])
    t.line(8, 13, 11, 11, R['acid'][1]); t.line(7, 14, 4, 12, R['acid'][1])
    for y in range(2, 10):
        k = (y - 2) / 7
        w = int(round(5 * math.sin(k * math.pi))) + 1
        for x in range(8 - w, 8 + w):
            t.p(x, y, LE[2] if x < 8 else LE[1])
    t.rect(4, 5, 11, 6, H('#2a0a10'))          # offenes Maul
    for x in range(4, 12, 2):
        t.p(x, 5, W[4]); t.p(x + 1, 6, W[4])  # Zähne
    t.p(6, 3, LE[4]); t.p(5, 4, LE[3])
    return t


# =============================================================================
# Items
# =============================================================================
def kartusche_stabil():
    """Wie die Fluid-Kartusche, gefüllt mit violettem stabilisiertem Portalfluid."""
    t = Tex()
    t.rect(4, 1, 11, 2, ST[3]); t.rect(4, 13, 11, 14, ST[1]); t.rect(4, 14, 11, 14, ST[0])
    t.rect(5, 3, 10, 12, A(H('#cdeef2'), 110))
    t.rect(5, 4, 10, 12, V[2]); t.rect(5, 4, 10, 4, V[4]); t.p(6, 7, V[4]); t.p(9, 10, V[3])
    t.rect(4, 3, 4, 12, ST[2]); t.rect(11, 3, 11, 12, ST[1])
    t.rect(6, 1, 9, 1, G[3])                  # grüner Deckel: stabil
    t.outline()
    return t


def flux_stabilisator():
    """Grüner Kristall in einem Stahlring, gelbes Säure-Siegel."""
    t = Tex()
    t.disc(7.5, 7.5, 6.2, ST[1])
    t.disc(7.5, 7.5, 5.0, ST[3])
    t.disc(7.5, 7.5, 4.0, DARK)
    t.sprite(["..o..", ".o4o.", "o432o", "o321o", ".o1o.", "..o.."],
             {'o': G[0], '1': G[1], '2': G[2], '3': G[3], '4': G[4]}, 5, 5)
    t.p(12, 3, R['acid'][3]); t.p(13, 4, R['acid'][2])
    t.outline()
    return t


def fluxkristall_stab():
    """Brennstab: Zinnhülse mit grünem Kristallfenster."""
    t = Tex()
    for i in range(10):
        x, y = 3 + i, 12 - i
        t.p(x, y, TI[3]); t.p(x + 1, y, TI[2]); t.p(x, y + 1, TI[1]); t.p(x + 1, y + 1, TI[1])
    for i in range(3, 8):
        x, y = 3 + i, 12 - i
        t.p(x, y, G[3]); t.p(x + 1, y, G[2]); t.p(x, y + 1, G[1])
    t.p(2, 13, ST[2]); t.p(3, 14, ST[1]); t.p(13, 2, ST[3]); t.p(14, 1, ST[4])
    t.outline()
    return t


def kuehlkoerper():
    """Kupferblock mit Kühlrippen und Zinnfuß."""
    t = Tex()
    t.rect(3, 11, 12, 13, TI[2]); t.rect(3, 11, 12, 11, TI[3])
    for x in range(3, 13, 2):
        t.rect(x, 3, x, 10, CU[3]); t.p(x, 3, CU[4])
        t.rect(x + 1, 4, x + 1, 10, CU[1])
    t.outline()
    return t


def verbundplatte():
    """Mischmetall-Platte mit eingewalzten Xenit- und Flux-Fasern."""
    t = plate(R['steel'])
    for x, y, c in [(5, 7, P[3]), (6, 8, P[4]), (9, 6, G[2]), (10, 7, G[3]), (7, 10, P[3]), (8, 9, G[2])]:
        t.p(x, y, c)
    return t


# =============================================================================
def build(pk):
    cat = 'Meilenstein 3'
    pk.add(cat, 'block', 'flux_leitung', flux_leitung(), 'Flux-Leitung')
    pk.add(cat, 'block', 'flux_leitung_isoliert', flux_leitung_isoliert(), 'Flux-Leitung (isoliert)')
    pk.add(cat, 'item', 'flux_leitung', flux_leitung(), 'Flux-Leitung (Item)')
    pk.add(cat, 'item', 'flux_leitung_isoliert', flux_leitung_isoliert(), 'Flux-Leitung isoliert (Item)')
    pk.add(cat, 'block', 'umspanner_side', umspanner_side(), 'Umspanner (Seite)')
    pk.add(cat, 'block', 'umspanner_front', umspanner_front(False), 'Umspanner (runter)')
    pk.add(cat, 'block', 'umspanner_front_hoch', umspanner_front(True), 'Umspanner (hoch)')
    for n in range(5):
        pk.add(cat, 'block', f'flux_zelle_side_{n}', flux_zelle_side(n), f'Flux-Zelle {n}/4')
    pk.add(cat, 'block', 'labor_huelle', labor_huelle(), 'Labor-Hülle')
    pk.add(cat, 'block', 'resonator_kammer', resonator_kammer(), 'Resonator-Kammer')
    pk.add(cat, 'block', 'schwebstein_kern', schwebstein_kern(), 'Schwebstein-Kern')
    pk.add(cat, 'block', 'chronoglas_sand', chronoglas_sand(), 'Chronoglas-Sand')
    pk.add(cat, 'block', 'biomasse_block', biomasse_frames(), 'Biomasse ▶', frametime=4, interpolate=True)
    pk.add(cat, 'block', 'fressende_flora', fressende_flora(), 'Fressende Flora')

    pk.add(cat, 'item', 'fluid_kartusche_stabil', kartusche_stabil(), 'Fluid-Kartusche (stabil)')
    pk.add(cat, 'item', 'flux_stabilisator', flux_stabilisator(), 'Flux-Stabilisator')
    pk.add(cat, 'item', 'fluxkristall_stab', fluxkristall_stab(), 'Fluxkristall-Stab')
    pk.add(cat, 'item', 'kuehlkoerper', kuehlkoerper(), 'Kühlkörper')
    pk.add(cat, 'item', 'verbundplatte', verbundplatte(), 'Verbundplatte')
