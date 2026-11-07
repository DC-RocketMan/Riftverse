"""Meilenstein 1: Erdmetalle, Zinnerz, T1-Maschinen, Kupferkabel, Ein-/Ausspeiser, Energiezelle.

Baut auf denselben Vorlagen auf wie materials.py und machines.py, damit alles
zum Design-Leitfaden passt (matte Flächen, Portal-Grün nur für "aktiv").
"""
import math

from lib import Tex, R, T, H, A, OUT, BLACK, rng_for
from materials import ore, stone_base, bar, plate, dust, raw_chunk
from machines import casing, window, porthole, vents, lamp, DARK

G, P, W, GR, V = R['green'], R['petrol'], R['white'], R['graphite'], R['violet']
ST, WO, RU, BR, CU, OR = R['steel'], R['wood'], R['rust'], R['brass'], R['copper'], R['orange']
TIN, IRON, GOLD, BRZ, COAL = R['tin'], R['iron'], R['gold'], R['bronze'], R['coal']


# =============================================================================
# Blöcke
# =============================================================================
def metal_block(name, ramp):
    t = Tex(fill=ramp[2])
    rng = rng_for(name)
    t.noise(ramp, (0, 0.2, 0.55, 0.25, 0), rng, 1, 1, 14, 14, clump=0.55)
    t.bevel(0, 0, 15, 15, ramp[4], ramp[0])
    t.bevel(1, 1, 14, 14, ramp[3], ramp[1])
    for y in (5, 10):
        t.rect(2, y, 13, y, ramp[1])
        t.rect(2, y + 1, 13, y + 1, ramp[3])
    return t


def kabel_blank():
    """Blanker Kupferdraht: verdrillte Litzen, wirkt aus jeder Richtung gleich."""
    t = Tex()
    for y in range(16):
        for x in range(16):
            k = (x + y) % 4
            t.p(x, y, CU[[1, 2, 3, 4][k]])
    for i in range(0, 16, 4):
        t.p(i, (i + 1) % 16, CU[4])
    return t


def kabel_isoliert():
    """Dunkle Folie mit hellen Diagonalstreifen; die Streifen nehmen per Tint die Farbe an."""
    t = Tex(fill=GR[2])
    rng = rng_for('kabel_iso')
    t.noise(GR, (0, 0.25, 0.55, 0.2, 0), rng, clump=0.6)
    for y in range(16):
        for x in range(16):
            if (x + y) % 8 in (0, 1):
                t.p(x, y, H('#d8d8d8') if (x + y) % 8 == 0 else H('#b0b0b0'))
    return t


def front_erzmuehle(on):
    t = casing(1)
    # Trichter oben
    for k, y in enumerate(range(2, 5)):
        t.rect(3 + k, y, 12 - k, y, ST[2] if k == 0 else DARK)
    t.rect(3, 2, 12, 2, ST[3])
    # Gitter mit zwei Mahlwalzen
    window(t, 3, 7, 12, 12, P[0], H('#151c20'))
    for cx in (5.5, 9.5):
        t.disc(cx, 9.5, 1.9, ST[2])
        t.ring(cx, 9.5, 1.9, ST[1], 0.45)
        for k in range(4):
            a = k * math.pi / 2 + (0.8 if on else 0.0) * (1 if cx < 7 else -1)
            t.p(int(round(cx + 1.4 * math.cos(a))), int(round(9.5 + 1.4 * math.sin(a))), ST[4])
    for x in range(3, 13, 2):          # Gitterstäbe
        t.p(x, 7, ST[1]); t.p(x, 12, ST[1])
    if on:
        for x, y in [(4, 12), (7, 13), (10, 12), (11, 13), (6, 13)]:
            t.p(x, y, IRON[3])
    lamp(t, 12, 14, on)
    return t


def front_walzbank(on):
    t = casing(1)
    for y0 in (4, 8):                  # zwei blanke Walzen
        t.rect(2, y0, 10, y0 + 2, ST[2])
        t.rect(2, y0, 10, y0, ST[4])
        t.rect(2, y0 + 2, 10, y0 + 2, ST[1])
        for x in range(2 + (1 if on else 0), 11, 3):
            t.p(x, y0 + 1, ST[3])
        t.p(1, y0 + 1, ST[0]); t.p(11, y0 + 1, ST[0])
    # Drahtrolle an der Seite
    t.disc(13, 7.5, 2.2, CU[2])
    t.ring(13, 7.5, 2.2, CU[0], 0.45)
    t.p(13, 7, CU[4]); t.p(13, 8, BLACK)
    if on:   # Draht läuft durch die Walzen auf die Rolle
        t.line(2, 7, 12, 7, CU[3])
        t.p(5, 7, CU[4]); t.p(9, 7, CU[4])
    t.rect(3, 12, 9, 13, P[1]); t.rect(3, 12, 9, 12, P[3])
    lamp(t, 12, 12, on)
    return t


def front_legierungsofen(on):
    t = casing(1)
    for x0 in (2, 9):                  # zwei Einfülltrichter
        t.rect(x0, 2, x0 + 4, 2, ST[3])
        t.rect(x0 + 1, 3, x0 + 3, 3, DARK)
        t.rect(x0 + 2, 4, x0 + 2, 5, DARK)
    porthole(t, 7.5, 10, 3.2, ST, DARK)
    if on:
        t.disc(7.5, 10.5, 2.6, OR[1]); t.disc(7.5, 11, 1.5, OR[2])
        t.line(4, 4, 7, 9, CU[3])        # Kupfer
        t.line(11, 4, 8, 9, TIN[3])      # Zinn
        t.p(7, 11, BRZ[4]); t.p(8, 11, BRZ[3]); t.p(7, 12, R['yellow'][3])
    else:
        t.p(7, 11, OR[0]); t.p(8, 11, RU[1])
    lamp(t, 12, 14, on)
    return t


def front_port(einspeiser, on):
    """Schaltkasten mit Stecker-Symbol; der grüne Pfeil zeigt die Flussrichtung."""
    t = casing(1)
    t.rect(3, 3, 12, 12, GR[2])
    t.bevel(3, 3, 12, 12, GR[4], GR[0])
    # Steckdose
    t.disc(7.5, 9.5, 2.4, ST[3]); t.ring(7.5, 9.5, 2.4, ST[1], 0.45)
    t.rect(6, 9, 6, 10, BLACK); t.rect(9, 9, 9, 10, BLACK)
    # Pfeil: Einspeiser zeigt hinein (nach unten), Ausspeiser heraus (nach oben)
    c1 = G[4] if on else G[2]
    c2 = G[3] if on else G[1]
    if einspeiser:
        t.rect(7, 4, 8, 5, c2)
        t.rect(5, 5, 10, 5, c1); t.rect(6, 6, 9, 6, c1); t.rect(7, 7, 8, 7, c1)
    else:
        t.rect(7, 4, 8, 4, c1); t.rect(6, 5, 9, 5, c1); t.rect(5, 6, 10, 6, c1)
        t.rect(7, 7, 8, 7, c2)
    return t


def kreativ_quelle():
    t = Tex(fill=V[1])
    rng = rng_for('kreativ')
    t.noise(V, (0.1, 0.35, 0.35, 0.15, 0.05), rng, clump=0.5)
    t.bevel(0, 0, 15, 15, V[4], V[0])
    t.frame(1, 1, 14, 14, G[1])
    bolt = [(9, 2), (8, 3), (7, 4), (6, 5), (5, 6), (6, 7), (7, 7), (8, 7), (9, 7), (8, 8), (7, 9), (6, 10), (5, 11), (4, 12)]
    for x, y in bolt:
        t.p(x, y, G[4]); t.p(x + 1, y, G[2])
    return t


# =============================================================================
# Items
# =============================================================================
def wire_roll(ramp):
    """Drahtrolle wie der vorhandene 'Draht', in der Farbe des Metalls."""
    t = Tex()
    for y in range(4, 12):
        x0 = 4 + (y % 2)
        t.rect(x0, y, x0 + 7, y, ramp[2 if y % 2 else 3])
        t.p(x0, y, ramp[4])
    t.line(12, 11, 14, 13, ramp[3]); t.line(3, 4, 1, 2, ramp[3])
    t.outline()
    return t


def spool(ramp):
    """Lötzinn: Spule mit aufgewickeltem Zinndraht."""
    t = Tex()
    t.rect(3, 3, 12, 4, WO[2]); t.rect(3, 3, 12, 3, WO[3])
    t.rect(3, 11, 12, 12, WO[2]); t.rect(3, 12, 12, 12, WO[1])
    for y in range(5, 11):
        t.rect(4, y, 11, y, ramp[2 if y % 2 else 3])
        t.p(4, y, ramp[4])
    t.line(12, 8, 14, 10, ramp[3]); t.p(14, 11, ramp[4])
    t.outline()
    return t


def huelse(ramp):
    t = Tex()
    t.rect(5, 4, 10, 12, ramp[2])
    t.rect(5, 4, 5, 12, ramp[3]); t.rect(10, 4, 10, 12, ramp[1])
    t.rect(5, 3, 10, 3, ramp[4])
    t.rect(6, 3, 9, 4, DARK)          # Öffnung
    t.rect(5, 12, 10, 12, ramp[0])
    t.p(6, 7, ramp[4]); t.p(6, 8, ramp[3])
    t.outline()
    return t


def stange(ramp, accent=None):
    t = Tex()
    for i in range(10):
        x, y = 3 + i, 12 - i
        t.p(x, y, ramp[3]); t.p(x + 1, y, ramp[2]); t.p(x + 1, y + 1, ramp[1])
    t.p(3, 12, ramp[4]); t.p(12, 3, ramp[4])
    if accent:
        t.p(7, 8, accent[3]); t.p(9, 6, accent[4])
    t.outline()
    return t


def zahnrad(ramp):
    t = Tex()
    for k in range(8):
        a = k * math.pi / 4
        cx, cy = 7.5 + 5.6 * math.cos(a), 7.5 + 5.6 * math.sin(a)
        t.rect(int(round(cx - 0.5)), int(round(cy - 0.5)), int(round(cx + 0.5)), int(round(cy + 0.5)), ramp[2])
    t.shaded_disc(7.5, 7.5, 4.6, ramp, 1, 4)
    t.disc(7.5, 7.5, 1.6, T)
    t.ring(7.5, 7.5, 1.9, ramp[0], 0.4)
    t.outline()
    return t


def mischmetall():
    t = bar(ST)
    stripes = [BRZ, TIN, BRZ]
    for y in range(5, 12):
        for x in range(2, 14):
            c = t.get(x, y)
            if c[3] and c != OUT:
                k = ((x + y) // 2) % 3
                if k < 2:
                    ramp = stripes[(x + y) // 6 % 3]
                    # gleiche Helligkeitsstufe wie der Stahlpixel
                    lvl = [tuple(v) for v in ST].index(c) if c in [tuple(v) for v in ST] else 2
                    t.p(x, y, ramp[lvl])
    return t


def polymerfetzen():
    t = Tex()
    rng = rng_for('polymer')
    pieces = [(3, 4, 7, 7), (8, 3, 12, 6), (5, 8, 10, 12), (10, 9, 13, 11)]
    for x0, y0, x1, y1 in pieces:
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                if rng.random() < 0.85:
                    t.p(x, y, GR[rng.choice([2, 3, 3, 4])])
        t.p(x0, y0, GR[4])
    t.p(6, 10, G[1]); t.p(11, 4, G[1])
    t.outline()
    return t


def isolierfolie():
    t = Tex()
    # aufgerollt: Rolle links, Folienbahn nach rechts
    t.rect(2, 4, 6, 12, GR[2]); t.rect(2, 4, 6, 4, GR[4]); t.rect(2, 12, 6, 12, GR[0])
    t.rect(7, 6, 14, 12, GR[1]); t.rect(7, 6, 14, 6, GR[3])
    t.disc(4, 8, 1.2, GR[0])
    for x, y in [(8, 8), (10, 8), (9, 10), (12, 9), (13, 11), (11, 11)]:   # Alien-Schriftzeichen
        t.p(x, y, W[1])
    t.outline()
    return t


def handkurbel():
    rows = [
        "........ooo.....",
        ".......oHHHo....",
        ".......oHhHo....",
        "........oSo.....",
        "........oSo.....",
        "..oooooooSo.....",
        ".oBBBBBBBSo.....",
        ".oBbbbbbbbo.....",
        ".oBoooooooo.....",
        ".oBo............",
        "ooBoo...........",
        "oGGGo...........",
        "oGgGo...........",
        "oGGGo...........",
        ".ooo............",
    ]
    t = Tex()
    t.sprite([r.ljust(16, '.') for r in rows], {'o': OUT, 'H': WO[3], 'h': WO[1], 'S': ST[3], 'B': BRZ[3],
                                                 'b': BRZ[1], 'G': BRZ[2], 'g': BRZ[0]}, 0, 1)
    return t


def energiezelle():
    t = Tex()
    t.rect(6, 1, 9, 2, TIN[3]); t.rect(6, 1, 9, 1, TIN[4])          # Pol
    t.rect(4, 3, 11, 14, P[2])
    t.rect(4, 3, 4, 14, P[3]); t.rect(11, 3, 11, 14, P[1])
    t.rect(4, 3, 11, 3, TIN[3]); t.rect(4, 14, 11, 14, TIN[1])
    t.rect(6, 5, 9, 12, DARK)                                         # Füllstandsfenster
    for y in range(8, 13):
        t.rect(6, y, 9, y, G[2] if y > 8 else G[4])
    t.p(5, 5, P[4]); t.p(5, 6, P[4])
    t.outline()
    return t


def kabel_item(insulated):
    t = Tex()
    for i in range(11):
        x, y = 2 + i, 13 - i
        if insulated:
            t.p(x, y, GR[3]); t.p(x + 1, y, GR[2]); t.p(x, y - 1, GR[3]); t.p(x + 1, y + 1, GR[1])
            if i % 3 == 1:
                t.p(x, y, P[4])
        else:
            t.p(x, y, CU[3]); t.p(x + 1, y, CU[2]); t.p(x + 1, y + 1, CU[1])
    if insulated:     # blanke Enden
        t.p(1, 14, CU[3]); t.p(14, 1, CU[3])
    else:
        t.p(2, 13, CU[4]); t.p(12, 3, CU[4])
    t.outline()
    return t


# =============================================================================
def build(pk):
    S, D = R['stone'], R['deep']
    vein = [TIN[0], TIN[1], TIN[3], TIN[4], TIN[4]]     # heller als der Stein, damit die Flecken auffallen
    blocks = [
        ('tin_ore', ore('tin_ore', S, vein, H('#f4f8fa'), n=5, glow_count=3), 'Zinnerz'),
        ('deepslate_tin_ore', ore('deepslate_tin_ore', D, vein, H('#f4f8fa'), n=5, glow_count=3), 'Tiefenschiefer-Zinnerz'),
        ('tin_block', metal_block('tin_block', TIN), 'Zinnblock'),
    ]
    for tid, tex, name in blocks:
        pk.add('Meilenstein 1', 'block', tid, tex, name, lang_key=f'block.riftverse.{tid}')

    pk.add('Meilenstein 1', 'block', 'kupferkabel', kabel_blank(), 'Kupferkabel (Block)')
    pk.add('Meilenstein 1', 'block', 'kupferkabel_isoliert', kabel_isoliert(), 'Kupferkabel isoliert (Block)')

    for tid, fn, name in (('erzmuehle', front_erzmuehle, 'Erzmühle'), ('walzbank', front_walzbank, 'Walzbank'),
                          ('legierungsofen', front_legierungsofen, 'Legierungsofen')):
        pk.add('Meilenstein 1', 'block', tid + '_front', fn(False), name, lang_key=f'block.riftverse.{tid}')
        pk.add('Meilenstein 1', 'block', tid + '_front_on', fn(True), name + ' (aktiv)')
    for tid, ein, name in (('einspeiser', True, 'Einspeiser'), ('ausspeiser', False, 'Ausspeiser')):
        pk.add('Meilenstein 1', 'block', tid + '_front', front_port(ein, False), name, lang_key=f'block.riftverse.{tid}')
        pk.add('Meilenstein 1', 'block', tid + '_front_on', front_port(ein, True), name + ' (aktiv)')
    pk.add('Meilenstein 1', 'block', 'kreativ_quelle', kreativ_quelle(), 'Kreativ-Quelle',
           lang_key='block.riftverse.kreativ_quelle')

    items = [
        ('raw_tin', raw_chunk(TIN, TIN, 'raw_tin'), 'Rohes Zinn'),
        ('tin_ingot', bar(TIN), 'Zinnbarren'),
        ('bronze_ingot', bar(BRZ), 'Bronzebarren'),
        ('steel_ingot', bar(ST), 'Stahlbarren'),
        ('mischmetall', mischmetall(), 'Mischmetall'),
        ('eisen_staub', dust(IRON, 'eisen_staub'), 'Eisenstaub'),
        ('kupfer_staub', dust(CU, 'kupfer_staub'), 'Kupferstaub'),
        ('gold_staub', dust(GOLD, 'gold_staub'), 'Goldstaub'),
        ('zinn_staub', dust(TIN, 'zinn_staub'), 'Zinnstaub'),
        ('xenit_staub', dust(P, 'xenit_staub', G[3]), 'Xenit-Staub'),
        ('bronze_staub', dust(BRZ, 'bronze_staub'), 'Bronzestaub'),
        ('kohle_staub', dust(COAL, 'kohle_staub'), 'Kohlestaub'),
        ('eisen_platte', plate(IRON, rivets=False), 'Eisenplatte'),
        ('kupfer_platte', plate(CU, rivets=False), 'Kupferplatte'),
        ('zinn_platte', plate(TIN, rivets=False), 'Zinnplatte'),
        ('gold_platte', plate(GOLD, rivets=False), 'Goldplatte'),
        ('bronze_platte', plate(BRZ, rivets=False), 'Bronzeplatte'),
        ('stahl_platte', plate(ST, rivets=False), 'Stahlplatte'),
        ('kupferdraht', wire_roll(CU), 'Kupferdraht'),
        ('golddraht', wire_roll(GOLD), 'Golddraht'),
        ('loetzinn', spool(TIN), 'Lötzinn'),
        ('zinn_huelse', huelse(TIN), 'Zinnhülse'),
        ('eisen_stange', stange(IRON), 'Eisenstange'),
        ('xenit_stange', stange(P, G), 'Xenit-Stange'),
        ('bronze_zahnrad', zahnrad(BRZ), 'Bronze-Zahnrad'),
        ('polymerfetzen', polymerfetzen(), 'Polymerfetzen'),
        ('isolierfolie', isolierfolie(), 'Isolierfolie'),
        ('handkurbel', handkurbel(), 'Handkurbel'),
        ('energiezelle', energiezelle(), 'Energiezelle'),
        ('kupferkabel', kabel_item(False), 'Kupferkabel'),
        ('kupferkabel_isoliert', kabel_item(True), 'Isoliertes Kupferkabel'),
    ]
    for tid, tex, name in items:
        pk.add('Meilenstein 1', 'item', tid, tex, name, lang_key=f'item.riftverse.{tid}')
