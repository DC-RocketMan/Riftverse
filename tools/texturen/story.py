"""Story-Gerüst (Meilenstein 9 der neuen Universen): das Rift-Tagebuch."""
from lib import Tex, R, H

G, CU, LE = R['green'], R['copper'], R['leather']
PE = R['petrol']


def rift_tagebuch():
    """Schmales Reisetagebuch: dunkelpetrol Einband mit Kupferschließe, vorne ein kleiner Rift-Riss, Lesebändchen."""
    t = Tex()
    t.rect(4, 1, 12, 14, PE[1])            # Einband
    t.rect(4, 1, 5, 14, PE[0])             # Rücken
    t.rect(12, 2, 13, 13, H('#e6dcc4'))    # Seiten
    for y in range(3, 13, 2):
        t.p(13, y, H('#b8ad94'))
    # Rift-Riss: schräge, leuchtende Linie
    t.line(7, 4, 10, 10, G[3])
    t.line(8, 4, 11, 10, G[2])
    t.p(9, 7, G[4]); t.p(10, 9, G[4])
    t.rect(11, 7, 12, 8, CU[3])            # Schließe
    t.p(12, 7, CU[4])
    t.line(6, 14, 6, 15, H('#b23a3a'))     # Lesebändchen
    t.outline()
    return t


def build(pk):
    pk.add('Story', 'item', 'rift_tagebuch', rift_tagebuch(), 'Rift-Tagebuch', lang_key='item.riftverse.rift_tagebuch')
