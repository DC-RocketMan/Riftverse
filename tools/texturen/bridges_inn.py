"""Bridge's Inn (Meilenstein 10 der neuen Universen): Leuchtpflanzen, Deko-Blöcke, Items, Gleebs und Tiere.

Aufruf für nur diese Texturen (schreibt direkt in die Mod-Ressourcen):
    python3 tools/texturen/bridges_inn.py
"""
import os
import random
import sys

sys.path.insert(0, os.path.dirname(__file__))
from lib import Tex, R, H  # noqa: E402

TRANS = (0, 0, 0, 0)
G, VI, CY, PE = R['green'], R['violet'], R['cyan'], R['petrol']
WO, ST, BR, YE = R['wood'], R['stone'], R['brass'], R['yellow']


def mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3)) + (255,)


def rgb(h):
    return H('#%06x' % h)


def hell(c, t=0.45):
    return mix(c, (255, 255, 255, 255), t)


def dunkel(c, t=0.45):
    return mix(c, (0, 0, 0, 255), t)


STIEL = H('#3f8a3a')
STIEL_D = H('#2c6328')

# ---------------------------------------------------------------------------------------------- Leuchtpflanzen

PFLANZEN = {
    'sternenblume_weiss': ('sternenblume', 0xF4F0FF), 'sternenblume_gold': ('sternenblume', 0xFFD45A),
    'leuchtglocke_blau': ('leuchtglocke', 0x4A8CFF), 'leuchtglocke_violett': ('leuchtglocke', 0xB06BFF),
    'leuchtglocke_tuerkis': ('leuchtglocke', 0x3FF0D8), 'leuchtglocke_rosa': ('leuchtglocke', 0xFF7FC8),
    'mondkraut': ('mondkraut', 0xC8E0FF),
    'funkenmohn_rot': ('funkenmohn', 0xFF4A3A), 'funkenmohn_orange': ('funkenmohn', 0xFF9A2A),
    'zauberpilz_violett': ('zauberpilz', 0xC05AFF), 'zauberpilz_gruen': ('zauberpilz', 0x6AFF7A),
}


def stiel(t, x, y0, y1):
    t.line(x, y0, x, y1, STIEL)
    t.p(x + 1, y1 - 2, STIEL_D)


def sternenblume(farbe, an):
    t = Tex()
    c = rgb(farbe)
    kern = hell(c, 0.7) if an else c
    blatt = hell(c, 0.25) if an else dunkel(c, 0.15)
    stiel(t, 8, 8, 15)
    t.line(6, 12, 7, 11, STIEL); t.line(10, 13, 9, 12, STIEL)
    for dx, dy in ((0, -2), (2, 0), (0, 2), (-2, 0), (1, -1), (1, 1), (-1, 1), (-1, -1)):
        t.p(8 + dx, 6 + dy, blatt)
    t.p(8, 3, blatt); t.p(11, 6, blatt); t.p(8, 9, blatt); t.p(5, 6, blatt)
    t.p(8, 6, kern)
    if an:
        for x, y in ((4, 2), (12, 3), (3, 9), (13, 9)):
            t.p(x, y, hell(c, 0.8))
    return t


def leuchtglocke(farbe, an):
    t = Tex()
    c = rgb(farbe)
    glocke = hell(c, 0.35) if an else c
    rand = hell(c, 0.7) if an else hell(c, 0.25)
    stiel(t, 7, 3, 15)
    t.line(7, 3, 10, 2, STIEL)
    for gx, gy in ((10, 4), (4, 7), (10, 10)):
        t.rect(gx - 1, gy, gx + 1, gy + 2, glocke)
        t.p(gx - 2, gy + 2, rand); t.p(gx + 2, gy + 2, rand)
        t.p(gx, gy + 3, hell(c, 0.85) if an else rand)
        t.line(7, gy, gx, gy - 1 if gx > 7 else gy, STIEL)
    return t


def mondkraut(farbe, an):
    t = Tex()
    c = rgb(farbe)
    rng = random.Random(7)
    for x in (3, 5, 7, 8, 10, 12):
        h = 6 + rng.randint(0, 7)
        col = hell(c, 0.4) if an else mix(c, H('#7aa0a8'), 0.5)
        t.line(x, 15, x + rng.choice((-1, 0, 1)), 15 - h, col)
        t.p(x, 15 - h, hell(c, 0.85) if an else c)
    return t


def funkenmohn(farbe, an):
    t = Tex()
    c = rgb(farbe)
    bl = hell(c, 0.3) if an else c
    stiel(t, 8, 7, 15)
    t.rect(6, 4, 10, 6, bl)
    t.rect(7, 3, 9, 7, bl)
    t.p(8, 5, H('#1a1010'))
    if an:
        for x, y in ((5, 2), (11, 1), (12, 5), (4, 6)):
            t.p(x, y, YE[3])
    return t


def zauberpilz(farbe, an):
    t = Tex()
    c = rgb(farbe)
    hut = hell(c, 0.3) if an else c
    t.rect(7, 9, 8, 15, H('#e8dcc8'))
    t.rect(4, 6, 11, 8, hut)
    t.rect(5, 5, 10, 5, hut)
    for x, y in ((5, 7), (8, 6), (10, 7)):
        t.p(x, y, hell(c, 0.85))
    t.rect(11, 12, 12, 15, H('#e8dcc8')); t.rect(10, 11, 13, 11, hut)
    return t


ZEICHNER = {'sternenblume': sternenblume, 'leuchtglocke': leuchtglocke, 'mondkraut': mondkraut,
            'funkenmohn': funkenmohn, 'zauberpilz': zauberpilz}


def lichtranke(an):
    t = Tex()
    c = rgb(0x9AF0FF)
    for x, wob in ((5, 0), (10, 1)):
        for y in range(0, 16):
            xx = x + (1 if (y // 3 + wob) % 2 else 0)
            t.p(xx, y, H('#3f8a5a'))
            if y % 4 == 2:
                t.p(xx + 1, y, hell(c, 0.5) if an else mix(c, H('#3f8a5a'), 0.5))
    return t


def kraeuterbuendel():
    t = Tex()
    t.rect(7, 0, 8, 3, H('#8a6a3a'))
    for x, col in ((5, '#4a8a3a'), (7, '#6aa04a'), (9, '#8ab05a'), (11, '#5a7a3a')):
        t.line(x, 4, x + (1 if x > 8 else -1), 13, H(col))
        t.p(x, 13, H('#c8a0d8'))
    t.rect(6, 3, 10, 4, H('#b0402a'))
    return t


# ---------------------------------------------------------------------------------------------- Deko

def dachziegel():
    t = Tex()
    rng = random.Random(3)
    for y in range(16):
        for x in range(16):
            reihe = y // 4
            off = 4 if reihe % 2 else 0
            ziegel = (x + off) // 8
            base = VI[1] if (ziegel + reihe) % 3 else VI[2]
            if y % 4 == 3:
                base = dunkel(VI[0], 0.3)
            elif (x + off) % 8 == 0:
                base = VI[0]
            elif y % 4 == 0:
                base = hell(base, 0.15)
            if rng.random() < 0.08:
                base = hell(base, 0.1)
            t.p(x, y, base)
    return t


def fachwerk(kreuz):
    t = Tex(fill=H('#efe6d6'))
    rng = random.Random(5 if kreuz else 4)
    for y in range(16):
        for x in range(16):
            if rng.random() < 0.12:
                t.p(x, y, H('#e2d8c4'))
    balken = H('#5a3a22')
    t.rect(0, 0, 15, 1, balken); t.rect(0, 14, 15, 15, balken)
    t.rect(0, 0, 1, 15, balken); t.rect(14, 0, 15, 15, balken)
    if kreuz:
        t.line(2, 2, 13, 13, balken); t.line(2, 13, 13, 2, balken)
        t.line(3, 2, 13, 12, balken); t.line(3, 13, 13, 3, balken)
    else:
        t.rect(7, 0, 8, 15, balken)
        t.line(2, 13, 6, 2, balken)
    return t


def kopfstein():
    t = Tex(fill=H('#5a5a62'))
    rng = random.Random(11)
    farben = ['#a07a9a', '#7a9ab0', '#a0a07a', '#8a8a92', '#9a7a7a', '#7aa08a']
    for (x0, y0, x1, y1) in ((0, 0, 6, 4), (8, 0, 15, 5), (0, 6, 4, 10), (6, 6, 11, 11), (13, 7, 15, 12),
                             (0, 12, 7, 15), (9, 13, 15, 15), (5, 12, 7, 12)):
        c = H(rng.choice(farben))
        t.rect(x0, y0, x1, y1, c)
        t.line(x0, y0, x1, y0, hell(c, 0.2))
        t.line(x0, y1, x1, y1, dunkel(c, 0.2))
    return t


def sternglas():
    t = Tex()
    rand = H('#c8d8ff')
    t.frame(0, 0, 15, 15, rand)
    for x, y in ((4, 4), (11, 3), (7, 9), (12, 12), (3, 12)):
        t.p(x, y, H('#ffffff'))
        t.p(x + 1, y, (220, 230, 255, 160)); t.p(x - 1, y, (220, 230, 255, 160))
        t.p(x, y + 1, (220, 230, 255, 160)); t.p(x, y - 1, (220, 230, 255, 160))
    for y in range(1, 15):
        for x in range(1, 15):
            if t.get(x, y)[3] == 0:
                t.p(x, y, (150, 170, 255, 50))
    return t


def stamm_seite():
    t = Tex()
    rng = random.Random(8)
    for x in range(16):
        base = H('#7a3a6a') if x % 4 else H('#5a2a4a')
        for y in range(16):
            c = base if rng.random() > 0.1 else hell(base, 0.12)
            t.p(x, y, c)
    for x, y in ((3, 4), (10, 9), (6, 13)):
        t.p(x, y, H('#ff9ae8'))
    return t


def stamm_oben():
    t = Tex(fill=H('#c88ab0'))
    for r, col in ((7, '#5a2a4a'), (5, '#b07aa0'), (3, '#c88ab0'), (1, '#a06a90')):
        t.ring(7.5, 7.5, r, H(col), 0.6)
    return t


def bretter():
    t = Tex()
    rng = random.Random(9)
    for y in range(16):
        reihe = y // 4
        base = [H('#c890b8'), H('#b880a8'), H('#d0a0c0'), H('#c088b0')][reihe]
        for x in range(16):
            c = base
            if y % 4 == 3:
                c = dunkel(base, 0.2)
            elif (x + reihe * 5) % 9 == 0:
                c = dunkel(base, 0.15)
            elif rng.random() < 0.08:
                c = hell(base, 0.1)
            t.p(x, y, c)
    return t


def funkellaub():
    t = Tex()
    rng = random.Random(10)
    farben = [H('#ffb8e0'), H('#f8a0d8'), H('#ffd0ec'), H('#e890d0'), H('#c8a0ff')]
    for y in range(16):
        for x in range(16):
            if rng.random() < 0.82:
                t.p(x, y, rng.choice(farben))
    for x, y in ((3, 3), (11, 6), (6, 12), (13, 13)):
        t.p(x, y, H('#ffffff'))
    return t


def schwebelaterne():
    t = Tex()
    t.rect(4, 3, 11, 12, (180, 120, 255, 200))
    t.rect(6, 5, 9, 10, H('#fff0a0'))
    t.rect(7, 6, 8, 9, H('#ffffff'))
    t.frame(4, 3, 11, 12, BR[2])
    t.rect(5, 1, 10, 2, BR[1]); t.rect(5, 13, 10, 14, BR[1])
    return t


def feenlicht():
    t = Tex()
    t.disc(7.5, 7.5, 6, (255, 200, 255, 90))
    t.disc(7.5, 7.5, 4, (255, 220, 255, 170))
    t.disc(7.5, 7.5, 2.2, H('#ffffff'))
    return t


# ---------------------------------------------------------------------------------------------- Items

def sternenstaub():
    t = Tex()
    rng = random.Random(12)
    for _ in range(26):
        x, y = rng.randint(3, 12), rng.randint(5, 13)
        t.p(x, y, rng.choice([H('#ffe8a0'), H('#fff8e0'), H('#c8a0ff'), H('#a0e0ff')]))
    for x, y in ((7, 3), (11, 6), (4, 7)):
        t.p(x, y, H('#ffffff')); t.p(x + 1, y, H('#fff0b0')); t.p(x, y + 1, H('#fff0b0'))
    return t


def sternen_chip():
    t = Tex()
    t.rect(3, 3, 12, 12, H('#2a1e4a'))
    t.frame(3, 3, 12, 12, BR[3])
    for i in range(4, 12, 2):
        t.p(i, 2, BR[2]); t.p(i, 13, BR[2]); t.p(2, i, BR[2]); t.p(13, i, BR[2])
    t.p(7, 7, H('#ffffff')); t.p(8, 7, H('#fff0a0')); t.p(7, 8, H('#fff0a0')); t.p(6, 7, H('#c8a0ff')); t.p(7, 6, H('#c8a0ff'))
    return t


def sternenbrot():
    t = Tex()
    t.rect(2, 6, 13, 11, H('#c8883a'))
    t.rect(3, 5, 12, 5, H('#d89a4a'))
    t.rect(2, 11, 13, 12, H('#8a5a2a'))
    for x, y in ((4, 7), (8, 6), (11, 8), (6, 9)):
        t.p(x, y, H('#fff0a0'))
    t.outline()
    return t


def glimmhaar():
    t = Tex()
    for i, col in enumerate(('#d8f0ff', '#a0e0ff', '#ffffff', '#80c8f0')):
        t.line(4 + i * 2, 13, 7 + i * 2, 3, H(col))
    return t


def flausch():
    t = Tex()
    t.disc(7.5, 8.5, 5.2, H('#f8e8ff'))
    t.disc(5.5, 7, 3, H('#ffffff'))
    t.disc(10, 9.5, 3, H('#e8d0ff'))
    t.outline()
    return t


def falterstaub():
    t = Tex()
    t.rect(5, 9, 10, 13, H('#e0d8f0'))
    t.rect(6, 6, 9, 8, H('#e0d8f0'))
    t.rect(6, 4, 9, 5, H('#8a6a4a'))
    for x, y in ((6, 10), (8, 11), (7, 12), (9, 10)):
        t.p(x, y, H('#7affe8'))
    t.outline()
    return t


def verhuehnert_icon():
    t = Tex(18, 18)
    t.rect(5, 6, 12, 13, H('#ffffff'))
    t.rect(10, 3, 13, 7, H('#ffffff'))
    t.p(12, 4, H('#202020'))
    t.rect(14, 5, 15, 6, H('#ffb020'))
    t.rect(11, 1, 12, 2, H('#e02020'))
    t.rect(7, 14, 7, 16, H('#ffb020')); t.rect(10, 14, 10, 16, H('#ffb020'))
    t.outline()
    return t


# ---------------------------------------------------------------------------------------------- Entities

def box(t, u, v, w, h, d, faces):
    """Malt eine Modell-Box im Minecraft-UV-Layout; faces: top, bottom, rechts, vorne, links, hinten -> Farbe oder Funktion."""
    bereiche = {
        'top': (u + d, v, w, d), 'bottom': (u + d + w, v, w, d),
        'rechts': (u, v + d, d, h), 'vorne': (u + d, v + d, w, h),
        'links': (u + d + w, v + d, d, h), 'hinten': (u + 2 * d + w, v + d, w, h),
    }
    for name, (x0, y0, ww, hh) in bereiche.items():
        f = faces.get(name, faces.get('alle'))
        if f is None or ww <= 0 or hh <= 0:
            continue
        for y in range(hh):
            for x in range(ww):
                c = f(x, y, ww, hh) if callable(f) else f
                if c is not None:
                    t.p(x0 + x, y0 + y, c)


# Gleebs: zwergenhafte Elfen mit Knollennase, Spitzohren, weichem Schnallenhut und bunter, gemusterter Kleidung.
# Box-Layout wie in client/dorf/GleebModel (64 x 64).
BERUFE = {
    # haut, haar/bart (None = kein Bart), hut, hutband, hemd, weste/robe, muster, hose, stiefel, art
    'buerger': ('#f6c9a4', '#d8662a', '#2f9a4a', '#1d5a2a', '#f4efe0', '#2f8a46', '#ffd34a', '#7a4a2a', '#3a2418', 'weste'),
    'baeckerin': ('#f9d2b8', None, '#f6e6c8', '#d88a6a', '#f2a0b0', '#fff8ee', '#e86a8a', '#b0603a', '#5a3420', 'schuerze'),
    'kraeuterhexe': ('#e8c9a0', None, '#5a7a2a', '#8a4a2a', '#e8d8a8', '#7a5a2a', '#b8e060', '#4a6a2a', '#3a2a18', 'weste'),
    'alchemist': ('#f1c7a8', '#eeeef4', '#6a3ab0', '#ffcf4a', '#e8e0ff', '#5a2e9a', '#ffcf4a', '#3a2a6a', '#2a1a3a', 'robe'),
    'runenschmied': ('#e4ae86', '#4a2c1a', '#5a3a26', '#c84a2a', '#c8402a', '#8a5a34', '#5ae8ff', '#4a3a30', '#2a1c14', 'schuerze'),
    'sternenkundlerin': ('#f4d0b4', None, '#22357a', '#ffe36a', '#cfe0ff', '#2c4aa0', '#ffe36a', '#1e2c5a', '#1a1a2e', 'robe'),
}

# Haarfarbe der Gleebs ohne Bart (mit Bart hat das Haar die Bartfarbe)
HAAR = {'baeckerin': '#e8a040', 'kraeuterhexe': '#b0502a', 'sternenkundlerin': '#eceaff'}


def gleeb(beruf):
    haut, bart, hut, band, hemd, weste, muster, hose, stiefel, art = BERUFE[beruf]
    haut, hut, band, hemd, weste, muster, hose, stiefel = (H(c) for c in (haut, hut, band, hemd, weste, muster, hose, stiefel))
    bart = H(bart) if bart else None
    haar = bart if bart else H(HAAR[beruf])
    t = Tex(64, 64)
    wange, mund, auge, pupille = mix(haut, H('#ff7a8a'), 0.45), dunkel(haut, 0.4), H('#ffffff'), H('#2a1e1a')
    gold = H('#ffd34a')
    guertel = H('#4a2a18')

    # --- Kopf (8 x 8 x 7) ---
    def gesicht(x, y, w, h):
        if y == 0:
            return haar
        if y == 2 and x in (1, 2, 5, 6):
            return dunkel(haar, 0.1)          # Augenbrauen
        if y == 3 and x in (1, 6):
            return auge
        if y == 3 and x in (2, 5):
            return pupille
        if y == 5 and x in (0, 7):
            return wange
        if y == 5 and x in (2, 5):
            return mund                       # Lächeln
        if y == 6 and x in (3, 4):
            return mund
        return haut

    def seite(x, y, w, h):
        return haar if y <= 2 else haut

    def hinten(x, y, w, h):
        return haar if y <= 5 or (x + y) % 3 else dunkel(haar, 0.15)
    box(t, 0, 0, 8, 8, 7, {'top': haar, 'bottom': haut, 'vorne': gesicht, 'rechts': seite, 'links': seite, 'hinten': hinten})
    nase = mix(haut, H('#ff8a7a'), 0.3)
    box(t, 30, 0, 2, 2, 1, {'alle': nase, 'vorne': lambda x, y, w, h: hell(nase, 0.3) if (x, y) == (0, 0) else nase})
    ohr = {'alle': haut, 'vorne': lambda x, y, w, h: wange if y == 1 else haut}
    box(t, 36, 0, 1, 3, 2, ohr)
    box(t, 42, 0, 1, 3, 2, ohr)
    if bart:
        def bartf(x, y, w, h):
            if y == 0 and x in (2, 3):
                return None                   # Platz für den Mund unter der Nase
            return bart if (x + y) % 2 else dunkel(bart, 0.15)
        box(t, 30, 5, 6, 3, 1, {'alle': bart, 'vorne': bartf})

    # --- Körper (7 x 6 x 4) ---
    def koerper_vorne(x, y, w, h):
        if y == 4:
            return gold if x == 3 else guertel                            # Gürtel mit Schnalle
        if y == 5:
            return hose if art != 'robe' else weste
        if art == 'weste':
            if x <= 1 or x >= 5:
                return muster if (x + y) % 3 == 0 else weste              # Weste mit Rautenmuster
            return gold if x == 3 and y in (1, 3) else hemd                # Hemd mit Knöpfen
        if art == 'schuerze':
            if 1 <= x <= 5 and y >= 1:
                return muster if (y == 3 and x % 2) else weste            # Schürze mit Ziernaht
            return hemd
        if x == 3 or (x == 2 and y % 2) or (x == 4 and not y % 2):
            return muster                                                 # Robe: Zickzack-Borte
        return weste

    def koerper_seite(x, y, w, h):
        if y == 4:
            return guertel
        if y == 5:
            return hose if art != 'robe' else weste
        return (weste if art != 'schuerze' else hemd) if (x + y) % 4 else muster

    def koerper_hinten(x, y, w, h):
        if y == 4:
            return guertel
        if beruf == 'sternenkundlerin' and (x * 5 + y * 3) % 7 == 0:
            return muster                                                 # Sternenumhang
        if y == 5:
            return hose if art != 'robe' else weste
        return weste if art != 'schuerze' else hemd
    box(t, 0, 16, 7, 6, 4, {'top': weste, 'bottom': hose, 'vorne': koerper_vorne, 'rechts': koerper_seite,
                            'links': koerper_seite, 'hinten': koerper_hinten})

    # --- Arme (2 x 6 x 2): Ärmel, Manschette, Hand ---
    aermel = hemd if art in ('schuerze', 'weste') else weste

    def arm(x, y, w, h):
        if y >= 5:
            return haut
        if y == 4:
            return muster
        return aermel if (x + y) % 5 else dunkel(aermel, 0.12)
    box(t, 22, 16, 2, 6, 2, {'alle': arm, 'top': aermel, 'bottom': haut})
    box(t, 30, 16, 2, 6, 2, {'alle': arm, 'top': aermel, 'bottom': haut})

    # --- Beine (3 x 5 x 3): Hose mit Muster, Stiefel mit Schnalle ---
    def bein(x, y, w, h):
        if y >= 3:
            return gold if (y == 3 and x == 1) else stiefel
        if beruf == 'buerger':
            return dunkel(hose, 0.2) if (x + y) % 2 else hose             # kariert
        return hose if (x * 2 + y) % 5 else muster
    for u in (38, 50):
        box(t, u, 16, 3, 5, 3, {'alle': bein, 'top': hose, 'bottom': dunkel(stiefel, 0.3)})

    # --- Hut: Krempe, Krone mit Schnallenband, gestreifter Zipfel, Bommel ---
    def krempe(x, y, w, h):
        return dunkel(hut, 0.25) if (x in (0, w - 1) or y in (0, h - 1)) else hut
    box(t, 0, 27, 10, 1, 9, {'alle': dunkel(hut, 0.25), 'top': krempe, 'bottom': dunkel(hut, 0.35)})

    def krone(x, y, w, h):
        if y == 2:
            if w == 7 and 2 <= x <= 4:
                return gold if x != 3 else band                           # Schnalle
            return band
        if beruf in ('alchemist', 'sternenkundlerin') and (x * 3 + y * 5) % 7 == 0:
            return muster                                                 # Sterne
        if beruf == 'kraeuterhexe' and y == 0 and x % 2:
            return H('#8ad04a')                                           # Blätter am Hut
        return hut if (x + y) % 4 else hell(hut, 0.08)
    box(t, 0, 38, 7, 4, 7, {'alle': krone, 'top': hut})

    def zipfel(x, y, w, h):
        return muster if (x + y) % 3 == 0 else hut
    box(t, 28, 38, 5, 2, 5, {'alle': zipfel})
    box(t, 48, 38, 3, 2, 3, {'alle': zipfel})
    box(t, 48, 44, 2, 2, 2, {'alle': lambda x, y, w, h: hell(muster, 0.3) if (x + y) % 2 else muster})

    # --- Besen ---
    box(t, 0, 50, 1, 1, 12, {'alle': lambda x, y, w, h: H('#7a4a2a') if (x + y) % 3 else H('#5a3418')})
    box(t, 28, 50, 3, 3, 4, {'alle': lambda x, y, w, h: H('#e0c060') if (x + y) % 2 else H('#c8a040')})
    return t


def glimmhase(leuchten):
    t = Tex(64, 32)
    fell, bauch, glow = H('#e8e0f8'), H('#ffffff'), H('#9af0ff')
    if leuchten:
        box(t, 48, 0, 1, 6, 2, {'alle': lambda x, y, w, h: glow if y <= 2 else None})
        box(t, 54, 0, 1, 6, 2, {'alle': lambda x, y, w, h: glow if y <= 2 else None})
        box(t, 8, 13, 3, 3, 3, {'alle': glow})
        return t
    box(t, 0, 0, 6, 5, 8, {'alle': fell, 'bottom': bauch})

    def kopf(x, y, w, h):
        if y == 1 and x in (0, 4):
            return H('#4a6aff')
        if y == 3 and x == 2:
            return H('#ff9ac0')
        return fell
    box(t, 28, 0, 5, 5, 5, {'alle': fell, 'vorne': kopf})
    box(t, 48, 0, 1, 6, 2, {'alle': lambda x, y, w, h: glow if y <= 2 else fell})
    box(t, 54, 0, 1, 6, 2, {'alle': lambda x, y, w, h: glow if y <= 2 else fell})
    box(t, 0, 13, 2, 3, 2, {'alle': dunkel(fell, 0.1)})
    box(t, 8, 13, 3, 3, 3, {'alle': bauch})
    return t


def pusteling():
    t = Tex(64, 32)
    rng = random.Random(21)
    farben = [H('#ffe0f4'), H('#ffd0ec'), H('#fff0fa'), H('#f0c8ff')]

    def flausch_(x, y, w, h):
        return rng.choice(farben)

    def gesicht(x, y, w, h):
        if y in (3, 4) and x in (2, 5):
            return H('#2a1a3a')
        if y == 6 and 3 <= x <= 4:
            return H('#ff7aa0')
        return rng.choice(farben)
    box(t, 0, 0, 8, 8, 8, {'alle': flausch_, 'vorne': gesicht})
    box(t, 32, 0, 4, 3, 4, {'alle': lambda x, y, w, h: H('#c8a0ff')})
    return t


def schimmerfalter(leuchten):
    t = Tex(32, 32)
    if leuchten:
        for v in (8, 16):
            box(t, 0, v, 7, 0, 6, {'top': lambda x, y, w, h: H('#7affe8') if (x + y) % 3 == 0 else None,
                                  'bottom': lambda x, y, w, h: H('#7affe8') if (x + y) % 3 == 0 else None})
        return t
    box(t, 0, 0, 1, 1, 5, {'alle': H('#3a2a4a')})
    for v in (8, 16):
        def fluegel(x, y, w, h):
            if x in (0, w - 1) or y in (0, h - 1):
                return H('#4a2a6a')
            return H('#b07aff') if (x + y) % 2 else H('#d8b0ff')
        box(t, 0, v, 7, 0, 6, {'top': fluegel, 'bottom': fluegel})
    return t


def irrlicht():
    t = Tex()
    t.disc(7.5, 7.5, 7, (120, 255, 230, 60))
    t.disc(7.5, 7.5, 5, (160, 255, 240, 140))
    t.disc(7.5, 7.5, 3, (220, 255, 250, 230))
    t.disc(7.5, 7.5, 1.5, H('#ffffff'))
    return t


# ---------------------------------------------------------------------------------------------- Ausgabe

def alle():
    """(art, id, Tex) für alles in diesem Modul."""
    out = []
    for name, (art, farbe) in PFLANZEN.items():
        out.append(('block', name, ZEICHNER[art](farbe, False)))
        out.append(('block', name + '_an', ZEICHNER[art](farbe, True)))
    out += [('block', 'lichtranke', lichtranke(False)), ('block', 'lichtranke_an', lichtranke(True)),
            ('block', 'kraeuterbuendel', kraeuterbuendel()), ('block', 'violette_dachziegel', dachziegel()),
            ('block', 'fachwerk', fachwerk(False)), ('block', 'fachwerk_kreuz', fachwerk(True)),
            ('block', 'buntes_kopfsteinpflaster', kopfstein()), ('block', 'sternglas', sternglas()),
            ('block', 'funkelholz_stamm', stamm_seite()), ('block', 'funkelholz_stamm_top', stamm_oben()),
            ('block', 'funkelholz_bretter', bretter()), ('block', 'funkellaub', funkellaub()),
            ('block', 'schwebelaterne', schwebelaterne()), ('block', 'feenlicht', feenlicht()),
            ('item', 'sternenstaub', sternenstaub()), ('item', 'sternen_chip', sternen_chip()),
            ('item', 'sternenbrot', sternenbrot()), ('item', 'glimmhaar', glimmhaar()),
            ('item', 'flausch', flausch()), ('item', 'falterstaub', falterstaub()),
            ('mob_effect', 'verhuehnert', verhuehnert_icon())]
    for beruf in BERUFE:
        out.append(('entity/gleeb', beruf, gleeb(beruf)))
    out += [('entity/tiere', 'glimmhase', glimmhase(False)), ('entity/tiere', 'glimmhase_leuchten', glimmhase(True)),
            ('entity/tiere', 'pusteling', pusteling()),
            ('entity/tiere', 'schimmerfalter', schimmerfalter(False)),
            ('entity/tiere', 'schimmerfalter_leuchten', schimmerfalter(True)),
            ('entity/tiere', 'irrlicht', irrlicht())]
    return out


def build(pk):
    for art, tid, tex in alle():
        pk.add("Bridge's Inn", art, tid, tex, tid)


if __name__ == '__main__':
    root = os.path.join(os.path.dirname(__file__), '..', '..', 'src', 'main', 'resources', 'assets', 'riftverse', 'textures')
    for art, tid, tex in alle():
        d = os.path.join(root, art)
        os.makedirs(d, exist_ok=True)
        tex.image().save(os.path.join(d, tid + '.png'))
    print(len(alle()), 'Texturen geschrieben')
