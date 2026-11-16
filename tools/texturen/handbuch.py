"""Handbuch und Garagen-Akku: das Riftverse-Handbuch (GuideME) und der erste Speicher ohne Xenit."""
from lib import Tex, R, H
from machines import casing, DARK

G, RU, CU, ST, RED, LE, WO = R['green'], R['rust'], R['copper'], R['steel'], R['red'], R['leather'], R['wood']


def akku_seite(an):
    """Holzkiste der Garage mit einem Batteriefenster: drei Zellen, rot im Leerlauf, grün beim Laden und Entladen."""
    t = casing(0)
    t.rect(4, 3, 11, 12, DARK)
    t.frame(3, 2, 12, 13, RU[2])
    t.p(3, 2, RU[4]); t.p(12, 13, RU[0])
    ramp = G if an else RED
    for y in (4, 7, 10):
        t.rect(5, y, 10, y + 1, ramp[2])
        t.rect(5, y, 10, y, ramp[3])
        t.p(5, y, ramp[4])
    return t


def akku_deckel():
    """Deckel mit zwei Polen: Kupfer (+) und Eisen (−)."""
    t = casing(0, top=True)
    for x0, ramp in ((3, CU), (10, ST)):
        t.rect(x0, 3, x0 + 2, 5, ramp[2])
        t.rect(x0, 3, x0 + 2, 3, ramp[4])
        t.p(x0 + 2, 5, ramp[0])
    t.rect(3, 8, 5, 8, CU[4]); t.rect(4, 7, 4, 9, CU[4])      # +
    t.rect(10, 8, 12, 8, ST[4])                                 # −
    t.line(4, 6, 4, 12, RU[1]); t.line(11, 6, 11, 12, RU[1])    # Kabel
    return t


def handbuch():
    """Dickes Werkstattbuch: Ledereinband mit Kupferecken, vorne ein grüner Portalwirbel, Seiten hell."""
    t = Tex()
    t.rect(3, 1, 12, 14, LE[2])            # Einband
    t.rect(3, 1, 4, 14, LE[1])             # Rücken
    t.rect(12, 2, 13, 14, H('#e6dcc4'))    # Seiten
    for y in range(3, 14, 2):
        t.p(13, y, H('#b8ad94'))
    t.disc(8.5, 7.5, 3.2, G[1])
    t.ring(8.5, 7.5, 3.0, G[3], 0.6)
    t.disc(8.5, 7.5, 1.6, G[2])
    t.p(8, 7, G[4]); t.p(9, 6, G[4])
    for x, y in ((5, 1), (11, 1), (5, 14), (11, 14)):
        t.p(x, y, CU[3])
    t.rect(5, 12, 11, 12, CU[2])           # Messingband
    t.outline()
    return t


def build(pk):
    cat = 'Handbuch'
    pk.add(cat, 'block', 'garagen_akku_side', akku_seite(False), 'Garagen-Akku', lang_key='block.riftverse.garagen_akku')
    pk.add(cat, 'block', 'garagen_akku_side_on', akku_seite(True), 'Garagen-Akku (aktiv)')
    pk.add(cat, 'block', 'garagen_akku_top', akku_deckel(), 'Garagen-Akku (Deckel)')
    pk.add(cat, 'item', 'handbuch', handbuch(), 'Riftverse-Handbuch', lang_key='item.riftverse.handbuch')
