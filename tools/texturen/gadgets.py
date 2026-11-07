"""Gadgets, Kanonen-Bauteile, Werkzeuge und Kleingeräte."""
import math

from lib import Tex, R, T, H, A, OUT, BLACK, rng_for
from materials import vial

G, P, W, GR, V = R['green'], R['petrol'], R['white'], R['graphite'], R['violet']
ST, WO, RU, BR, CU, OR = R['steel'], R['wood'], R['rust'], R['brass'], R['copper'], R['orange']
NU = R['null']
GLASS = A(H('#cdeef2'), 130)


def spr(rows, legend, ox=0, oy=0, t=None):
    t = t or Tex()
    rows = [r.ljust(16, '.')[:16] for r in rows]
    t.sprite(rows, legend, ox, oy)
    return t


# =============================================================================
# Portal-Kanone Mk I–IV
# =============================================================================
GUN = [
    "................",
    "................",
    "................",
    "....occcco......",
    "...ocFFffco.....",
    "...ocfffffco....",
    ".ooBBBBBBBBBoo..",
    "oBBBBBBBBBBBBLLo",
    "obbbbbbbbbbbbllo",
    ".obbbbbbbbbbboo.",
    "..ohHho.ro......",
    "..ohHhooo.......",
    "..ohHho.........",
    "..ohhho.........",
    "...ooo..........",
]


def portal_gun(mk, frame=0):
    if mk == 1:
        body, grip, lens = ST, WO, G
    elif mk == 2:
        body, grip, lens = P, ST, G
    elif mk == 3:
        body, grip, lens = W, GR, G
    else:
        body, grip, lens = NU, GR, G
    fl = [G[1], G[2], G[3], G[4]]
    if mk == 4:
        fl = [G[2], G[3], G[4], H('#ffffff')]
    lg = {
        'o': OUT, 'c': A(H('#d8f4f8'), 200), 'f': fl[1], 'F': fl[3],
        'B': body[3], 'b': body[2] if mk != 4 else body[2], 'h': grip[2], 'H': grip[3],
        'L': lens[4] if mk >= 3 else lens[3], 'l': lens[2], 'r': ST[1],
    }
    t = spr(GUN, lg)
    t.rect(1, 9, 12, 9, body[1] if mk != 4 else NU[1])
    if mk == 1:          # Klebeband + Schrauben + Rostflecken
        t.rect(6, 6, 7, 9, H('#9a9a92')); t.p(6, 7, H('#bcbcb4'))
        t.p(3, 8, RU[2]); t.p(10, 8, RU[3]); t.p(2, 7, BR[3])
        t.rect(5, 6, 8, 6, RU[2])
    elif mk == 2:
        t.p(3, 7, P[4]); t.p(9, 7, P[4]); t.rect(5, 8, 9, 8, P[1])
    elif mk == 3:
        t.rect(4, 7, 8, 8, BLACK); t.p(5, 8, G[3]); t.p(6, 7, G[3]); t.p(7, 8, G[2])
    elif mk == 4:
        rng = rng_for('gun4', frame)
        for _ in range(4):
            x, y = rng.randrange(2, 12), rng.choice([7, 8, 9])
            t.p(x, y, H('#ffffff'))
        t.rect(1, 6, 12, 6, G[1])
    return t


# =============================================================================
# Gadgets
# =============================================================================
def drohne():
    t = Tex()
    t.line(2, 2, 6, 2, ST[3]); t.line(9, 2, 13, 2, ST[3]); t.rect(7, 2, 8, 3, ST[1])
    t.rect(3, 4, 12, 11, ST[3]); t.rect(3, 4, 12, 4, ST[4]); t.rect(12, 5, 12, 11, ST[1])
    t.rect(3, 10, 12, 11, ST[2]); t.rect(3, 10, 12, 10, G[2])
    t.disc(7.5, 7, 2.4, BLACK); t.disc(7.5, 7.5, 1.2, G[3]); t.p(7, 6, H('#ffffff'))
    t.line(7, 12, 7, 13, ST[1]); t.p(6, 14, ST[2]); t.p(8, 14, ST[2])
    t.outline()
    return t


def holo():
    t = Tex()
    for y in range(2, 12):
        half = (y - 2) // 2 + 1
        for x in range(8 - half, 8 + half):
            if (x + y) % 2 == 0:
                t.p(x, y, A(R['cyan'][3], 120))
    t.disc(7.5, 4, 1.4, A(G[3], 210))
    t.rect(6, 6, 9, 9, A(G[3], 180)); t.rect(6, 10, 6, 11, A(G[3], 150)); t.rect(9, 10, 9, 11, A(G[3], 150))
    t.rect(2, 12, 13, 13, GR[2]); t.rect(2, 12, 13, 12, GR[4]); t.rect(5, 12, 10, 12, R['cyan'][3])
    t.outline()
    return t


def schrumpfstrahler():
    rows = [
        "................",
        "................",
        "...........oo...",
        "..........oTto..",
        ".ooooooooooTTto.",
        "oPPRRPPRRPPTTto.",
        "oPpRrpPRrpPTTto.",
        "oppRrppRrppTTto.",
        ".ooooppoooooTto.",
        "....oHho...oo...",
        "....oHhoo.......",
        "....oHho........",
        "....ohho........",
        ".....oo.........",
    ]
    return spr(rows, {'o': OUT, 'P': P[3], 'p': P[2], 'R': BR[4], 'r': BR[2], 'T': BR[3], 't': BR[1],
                      'H': ST[3], 'h': ST[2]}, 0, 1)


def granate():
    t = Tex()
    t.shaded_disc(7.5, 8.5, 4.8, W, 1, 4)
    t.rect(3, 8, 12, 9, R['sky'][2]); t.rect(3, 8, 12, 8, R['sky'][3])
    t.rect(6, 2, 9, 3, ST[2]); t.ring(10.5, 2.5, 1.4, ST[3], 0.45)
    t.p(5, 6, H('#ffffff'))
    t.outline()
    return t


def halsband():
    t = Tex()
    LE = R['leather']
    t.ring(7.5, 7.5, 5.2, LE[2], 0.9)
    t.ring(7.5, 7.2, 5.2, LE[3], 0.3)
    t.rect(5, 11, 10, 14, GR[2]); t.rect(5, 11, 10, 11, GR[4])
    for x in (6, 8):
        t.p(x, 13, BLACK)
    t.p(9, 12, G[4])
    t.rect(12, 6, 13, 8, BR[3])
    t.outline()
    return t


def phiole():
    t = vial(R['cyan'], cork=BR, level=7, bubbles=False)
    cols = [G[3], V[3], OR[3], R['sky'][3], R['flesh'][3]]
    k = 0
    for y in range(8, 14):
        for x in range(5, 11):
            if (x * 3 + y * 5) % 7 == 0:
                t.p(x, y, cols[k % len(cols)]); k += 1
    return t


def monokel():
    t = Tex()
    t.disc(6.5, 6.5, 4.6, A(G[2], 170))
    t.disc(5.5, 5.5, 1.8, A(G[4], 200))
    t.ring(6.5, 6.5, 4.6, BR[2], 0.7)
    t.p(3, 4, BR[4])
    for i, (x, y) in enumerate([(10, 10), (11, 11), (12, 12), (12, 13), (13, 14)]):
        t.p(x, y, BR[3] if i % 2 else BR[1])
    t.outline()
    return t


def multitool():
    rows = [
        "............o...",
        "...........oDo..",
        "..........oDdo..",
        ".........oDdo...",
        "..oo....oDdo....",
        ".oWwo..oPPo.....",
        ".oWwo.oPPpo.....",
        "..oWwoPPpo......",
        "...oWPPpo.......",
        "....oPpo.oo.....",
        "...oPpo.oLlo....",
        "..oPpo..oLlo....",
        ".oPpo....oo.....",
        ".ooo............",
    ]
    return spr(rows, {'o': OUT, 'D': ST[4], 'd': ST[2], 'W': ST[3], 'w': ST[1], 'P': P[3], 'p': P[1],
                      'L': G[4], 'l': G[2]}, 0, 1)


def taschenuhr():
    t = Tex()
    t.rect(7, 1, 8, 2, BR[3]); t.ring(7.5, 1.5, 1.6, BR[2], 0.4)
    t.disc(7.5, 8.5, 5.6, BR[2]); t.ring(7.5, 8.5, 5.6, BR[0], 0.5)
    t.disc(7.5, 8.5, 4.3, A(V[2], 240))
    for k in range(4):
        a = k * math.pi / 2
        t.p(int(round(7.5 + 3.6 * math.cos(a))), int(round(8.5 + 3.6 * math.sin(a))), V[4])
    t.line(8, 9, 8, 6, H('#ffffff')); t.line(8, 9, 10, 9, V[0])
    t.p(5, 6, A(H('#ffffff'), 160))
    t.outline()
    return t


def spritze():
    rows = [
        "............oo..",
        "...........oSo..",
        "..........oSSo..",
        ".........o||o...",
        "........oCabo...",
        ".......oCabCo...",
        "......oCabCo....",
        ".....oCabCo.....",
        "....oCabCo......",
        "...ooCCCo.......",
        "..oNo.oo........",
        ".oNo............",
        "oNo.............",
        "oo..............",
    ]
    t = spr(rows, {'o': OUT, 'S': ST[3], '|': ST[2], 'C': A(H('#d8f4f8'), 220), 'a': V[3], 'b': G[3],
                   'N': ST[4]}, 0, 1)
    return t


def aufgaben_box():
    t = Tex()
    t.rect(2, 5, 13, 14, ST[2]); t.rect(2, 5, 13, 5, ST[4]); t.rect(13, 6, 13, 14, ST[1]); t.rect(2, 14, 13, 14, ST[0])
    for x, y in [(3, 6), (12, 6), (3, 13), (12, 13)]:
        t.p(x, y, ST[4])
    t.disc(7.5, 4, 2.6, OR[2]); t.rect(5, 4, 10, 5, OR[1]); t.p(6, 2, OR[4]); t.p(7, 2, OR[3])
    t.rect(5, 8, 10, 11, BLACK); t.p(6, 9, G[3]); t.p(9, 9, G[3]); t.rect(6, 10, 9, 10, G[2])
    t.outline()
    return t


def gleiter():
    rows = [
        "................",
        "................",
        "................",
        ".....oooo.......",
        "....oCCcco......",
        "...oCCcccco.....",
        ".ooRRRRRRRRooo..",
        "oRRrrrrrrrrrRRo.",
        "orrrSSrrrSSrrrro",
        ".oggo.ooooo.ogGo",
        "..oo........oo..",
    ]
    return spr(rows, {'o': OUT, 'C': A(H('#d8f4f8'), 220), 'c': A(R['cyan'][2], 220), 'R': RU[3], 'r': RU[2],
                      'S': ST[3], 'g': G[2], 'G': G[4]}, 0, 3)


# =============================================================================
# Kanonen-Bauteile, Kartuschen, Werkzeuge
# =============================================================================
def gehaeuse():
    t = portal_gun(1)
    for y in range(3, 6):
        for x in range(3, 12):
            t.p(x, y, T) if t.get(x, y)[3] else None
    t.a[3:6, 3:12] = 0
    t.rect(4, 5, 9, 5, ST[1])
    t.rect(12, 7, 14, 8, T)
    t.a[7:9, 13:16] = 0
    t.outline()
    return t


def kanonenkern():
    t = Tex()
    t.rect(3, 5, 12, 10, P[2]); t.rect(3, 5, 12, 5, P[4]); t.rect(3, 10, 12, 10, P[0])
    t.rect(1, 6, 2, 9, ST[2]); t.rect(13, 6, 14, 9, ST[2])
    t.disc(7.5, 7.5, 2.2, G[2]); t.p(7, 7, G[4])
    for x in (4, 11):
        t.rect(x, 6, x, 9, R['white'][3])
    t.p(5, 6, R['brass'][3]); t.p(10, 9, R['brass'][3])
    t.outline()
    return t


def griff():
    rows = [
        "......oo........",
        ".....oVvo.......",
        ".....oVvo.......",
        "....oVvvo.......",
        "....oVvo........",
        "...oVvvo........",
        "...oVvo.........",
        "..oVvvo.........",
        "..oBBBBo........",
        "..oBbbbo........",
        "...oooo.........",
    ]
    t = spr(rows, {'o': OUT, 'V': V[3], 'v': V[1], 'B': BR[3], 'b': BR[1]}, 4, 2)
    t.p(8, 5, V[4]); t.p(7, 8, V[4])
    return t


def kartusche(full):
    t = Tex()
    t.rect(4, 1, 11, 2, ST[3]); t.rect(4, 13, 11, 14, ST[1]); t.rect(4, 14, 11, 14, ST[0])
    t.rect(5, 3, 10, 12, A(H('#cdeef2'), 110))
    if full:
        t.rect(5, 4, 10, 12, G[2]); t.rect(5, 4, 10, 4, G[4]); t.p(6, 7, G[4]); t.p(9, 10, G[3])
    t.rect(4, 3, 4, 12, ST[2]); t.rect(11, 3, 11, 12, ST[1])
    t.rect(6, 1, 9, 1, ST[4])
    t.outline()
    return t


def eimer():
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
    return spr(rows, {'o': OUT, 'S': ST[3], 's': ST[2], 'F': G[4], 'f': G[2]}, 0, 1)


def hammer():
    rows = [
        "......oooo......",
        ".....oSSSSoo....",
        "....oSSsssSSo...",
        ".....ooossSSo...",
        ".......oWwoo....",
        "......oWwo......",
        ".....oWwo.......",
        "....oWwo........",
        "...oWwo.........",
        "..oWwo..........",
        ".oWwo...........",
        ".ooo............",
    ]
    return spr(rows, {'o': OUT, 'S': ST[3], 's': ST[1], 'W': WO[4], 'w': WO[2]}, 1, 2)


def loetkolben():
    rows = [
        "..............o.",
        ".............oTo",
        "............oTo.",
        "...........oSo..",
        "..........oSo...",
        ".........oSSo...",
        "........oRRo....",
        ".......oRrRo....",
        "......oRrRo.....",
        ".....oRrRo......",
        "....oRrRo.......",
        "...oooRo........",
        "..oKo.o.........",
        ".oKo............",
        "oKo.............",
        "oo..............",
    ]
    return spr(rows, {'o': OUT, 'T': OR[3], 'S': ST[3], 'R': R['red'][2], 'r': R['red'][1], 'K': GR[3]})


# =============================================================================
def build(pk):
    for mk in (1, 2, 3):
        pk.add('Gadgets', 'item', f'portal_kanone_mk{mk}', portal_gun(mk), f'Portal-Kanone Mk {"I" * mk}',
               lang_key=f'item.riftverse.portal_kanone_mk{mk}')
    pk.add('Gadgets', 'item', 'portal_kanone_mk4', [portal_gun(4, f) for f in range(6)], 'Portal-Kanone Mk IV',
           frametime=4, lang_key='item.riftverse.portal_kanone_mk4')
    gadgets = [
        ('einzweck_drohne', drohne(), 'Einzweck-Drohne'),
        ('holo_koeder', holo(), 'Holo-Köder'),
        ('schrumpfstrahler', schrumpfstrahler(), 'Schrumpfstrahler'),
        ('anti_grav_granate', granate(), 'Anti-Grav-Granate'),
        ('uebersetzer_halsband', halsband(), 'Übersetzer-Halsband'),
        ('erinnerungsphiole', phiole(), 'Erinnerungsphiole'),
        ('tarn_detektor', monokel(), 'Tarn-Detektor'),
        ('multiversal_multitool', multitool(), 'Multiversal-Multitool'),
        ('zeitstopper', taschenuhr(), 'Zeitstopper'),
        ('mutagen_spritze', spritze(), 'Mutagen-Spritze'),
        ('aufgaben_box', aufgaben_box(), 'Aufgaben-Box'),
        ('garagen_raumgleiter', gleiter(), 'Garagen-Raumgleiter'),
    ]
    for tid, tex, name in gadgets:
        pk.add('Gadgets', 'item', tid, tex, name, lang_key=f'item.riftverse.{tid}')

    parts = [
        ('kanonengehaeuse', gehaeuse(), 'Kanonengehäuse'),
        ('kanonenkern', kanonenkern(), 'Kanonenkern'),
        ('chronoglas_griff', griff(), 'Chronoglas-Griff'),
        ('fluid_kartusche', kartusche(False), 'Fluid-Kartusche (leer)'),
        ('fluid_kartusche_voll', kartusche(True), 'Fluid-Kartusche (voll)'),
        ('portalfluid_eimer', eimer(), 'Portalfluid-Eimer'),
        ('garagen_hammer', hammer(), 'Garagen-Hammer'),
        ('loetkolben', loetkolben(), 'Lötkolben'),
    ]
    for tid, tex, name in parts:
        pk.add('Kanonenteile & Werkzeuge', 'item', tid, tex, name, lang_key=f'item.riftverse.{tid}')
