"""Meilenstein 6: Gadgets und Rüstung – was dem Texturpaket noch fehlte: Transformator-Chip, Kalibrier-Satz und
Multiversum-Legierung. Gadgets, Module, Rüstungsteile und Rüstungsschichten gibt es schon seit Meilenstein 1."""
from lib import Tex, R, H
from materials import chip

G, ST, V, CY, GOLD = R['green'], R['steel'], R['violet'], R['cyan'], R['gold']


def sym_trafo(r):
    """Zwei Spulen mit einem Pfeil nach oben: eine Spannungsstufe mehr."""
    return [(6, 5, r[3]), (6, 6, r[2]), (6, 7, r[3]), (6, 8, r[2]), (9, 5, r[4]), (9, 6, r[3]), (9, 7, r[4]), (9, 8, r[3]),
            (7, 6, r[4]), (8, 5, r[4]), (8, 7, r[1])]


def kalibrier_satz():
    """Ein Etui mit drei Proben (Fluxkristall grün, Säuregel gelbgrün, Chronoglas violett) und einem Fragment-Siegel."""
    t = Tex()
    t.rect(2, 4, 13, 12, ST[1])
    t.bevel(2, 4, 13, 12, ST[3], ST[0])
    for x, ramp in ((4, G), (7, R['yellow']), (10, V)):
        t.rect(x, 6, x + 1, 10, ramp[2])
        t.rect(x, 6, x + 1, 6, ramp[4])
        t.p(x + 1, 9, ramp[1])
    t.rect(6, 2, 9, 3, GOLD[3])
    t.p(7, 2, GOLD[4])
    t.outline()
    return t


def multiversum_legierung():
    """Ein Barren mit Bändern in den Farben von sechs Universen."""
    t = Tex()
    bands = [R['grav'][3], R['mirror'][3], R['cyan'][3], R['orange'][3], ST[2], G[3]]
    for i, c in enumerate(bands):
        x0 = 2 + i * 2
        t.rect(x0, 6, x0 + 1, 11, c)
    t.rect(2, 5, 13, 5, H('#ffffff'))
    t.bevel(2, 5, 13, 11, ST[4], ST[0])
    t.outline()
    return t


def build(pk):
    cat = 'Meilenstein 6'
    pk.add(cat, 'item', 'transformator_chip', chip(R['yellow'], sym_trafo), 'Transformator-Chip')
    pk.add(cat, 'item', 'kalibrier_satz', kalibrier_satz(), 'Kalibrier-Satz')
    pk.add(cat, 'item', 'multiversum_legierung', multiversum_legierung(), 'Multiversum-Legierung')
