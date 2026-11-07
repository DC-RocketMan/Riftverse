"""Riftverse Platzhalter-Texturen: kleine Pixel-Art-Bibliothek.

Farben kommen aus der Palette im Design-Dokument, jede Rampe hat 5 Stufen
(0 = dunkelste, 4 = hellste). Alles ist deterministisch (Seed = Texturname).
"""
import json
import os
import random

import numpy as np
from PIL import Image


def H(h, a=255):
    h = h.lstrip('#')
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def A(c, a):
    """Farbe mit anderer Deckkraft."""
    return (c[0], c[1], c[2], a)


T = (0, 0, 0, 0)

_RAMPS = {
    'green':    ['#0b4a24', '#12a84a', '#2fe063', '#39ff6a', '#b6ffc8'],
    'petrol':   ['#0c2a2e', '#153f44', '#1e5a61', '#2f7f87', '#5aa9b0'],
    'white':    ['#7f898e', '#aeb7bb', '#cfd6d9', '#e6eaec', '#f8fafb'],
    'graphite': ['#101216', '#1b1e23', '#2a2e35', '#3c424b', '#59616c'],
    'rust':     ['#3a1d10', '#5c2f1a', '#8a4b2a', '#ad6a3e', '#c98a5a'],
    'orange':   ['#7a3a00', '#c25e00', '#ff8a1e', '#ffb15e', '#ffd8a8'],
    'violet':   ['#2b1858', '#5a3aa8', '#9a6bff', '#bb9cff', '#e2d6ff'],
    'null':     ['#030307', '#0a0a12', '#13132a', '#1f1f48', '#35356e'],
    'stone':    ['#4e4e4e', '#626262', '#767676', '#8a8a8a', '#a2a2a2'],
    'deep':     ['#222226', '#2e2e34', '#3b3b42', '#4b4b53', '#5e5e67'],
    'wood':     ['#3b2812', '#5a3d1d', '#7a5630', '#9a7040', '#b98b55'],
    'acid':     ['#465808', '#76980f', '#b2d62a', '#d5f45a', '#f0ffa8'],
    'flesh':    ['#561c28', '#88303e', '#c45a58', '#e68a70', '#f8bca2'],
    'grav':     ['#05070e', '#0d1327', '#192446', '#2b3b6e', '#4a62a8'],
    'brass':    ['#463210', '#76571a', '#ac8730', '#d6ae50', '#f0d98a'],
    'steel':    ['#26282c', '#43464c', '#646870', '#8b8f97', '#babec6'],
    'red':      ['#480a0a', '#861a1a', '#cf3030', '#ff6262', '#ffb2b2'],
    'mirror':   ['#46525c', '#748491', '#a5b5c1', '#cfdbe3', '#f2f7fa'],
    'copper':   ['#47200f', '#783a1a', '#ad5a30', '#d6804a', '#f0a878'],
    'cyan':     ['#0a3540', '#126070', '#2aa8c0', '#6ad8ea', '#c4f6ff'],
    'leather':  ['#2e1a0e', '#4a2c16', '#6e4424', '#8e5c34', '#ad7a4c'],
    'yellow':   ['#5a4a00', '#9a8000', '#e0c020', '#ffe45a', '#fff4b0'],
    'sky':      ['#0a1c40', '#123070', '#2050b0', '#4a80e0', '#9ac0ff'],
    # Meilenstein 1: Erdmetalle
    'tin':      ['#3e464c', '#646e75', '#8f99a0', '#bcc5cb', '#e2e8eb'],
    'iron':     ['#3c3c3e', '#68686c', '#9c9ca0', '#cbcbcf', '#efeff2'],
    'gold':     ['#6e4600', '#b07f0c', '#e2b424', '#fbd94e', '#fff3a6'],
    'bronze':   ['#4e270c', '#83481b', '#bb7432', '#e19c55', '#f5c58a'],
    'coal':     ['#0c0c0e', '#19191d', '#29292f', '#3e3e46', '#5a5a64'],
}
R = {k: [H(c) for c in v] for k, v in _RAMPS.items()}
BLACK = H('#08080a')
OUT = H('#141418')          # Standard-Outline für Items


class Tex:
    def __init__(self, w=16, h=16, fill=None):
        self.w, self.h = w, h
        self.a = np.zeros((h, w, 4), np.uint8)
        if fill is not None:
            self.a[:, :] = fill

    def copy(self):
        t = Tex(self.w, self.h)
        t.a = self.a.copy()
        return t

    # --- Grundoperationen -------------------------------------------------
    def get(self, x, y):
        if 0 <= x < self.w and 0 <= y < self.h:
            return tuple(int(v) for v in self.a[y, x])
        return T

    def p(self, x, y, c):
        if c is None or not (0 <= x < self.w and 0 <= y < self.h):
            return
        if c[3] == 255:
            self.a[y, x] = c
        elif c[3] > 0:
            base = self.a[y, x].astype(float)
            al = c[3] / 255.0
            if base[3] == 0:
                self.a[y, x] = c
            else:
                rgb = base[:3] * (1 - al) + np.array(c[:3], float) * al
                self.a[y, x] = (int(rgb[0]), int(rgb[1]), int(rgb[2]), 255)

    def rect(self, x0, y0, x1, y1, c):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                self.p(x, y, c)

    def frame(self, x0, y0, x1, y1, c):
        for x in range(x0, x1 + 1):
            self.p(x, y0, c)
            self.p(x, y1, c)
        for y in range(y0, y1 + 1):
            self.p(x0, y, c)
            self.p(x1, y, c)

    def bevel(self, x0, y0, x1, y1, light, dark):
        for x in range(x0, x1 + 1):
            self.p(x, y0, light)
            self.p(x, y1, dark)
        for y in range(y0 + 1, y1):
            self.p(x0, y, light)
            self.p(x1, y, dark)

    def line(self, x0, y0, x1, y1, c):
        dx, dy = abs(x1 - x0), -abs(y1 - y0)
        sx, sy = (1 if x0 < x1 else -1), (1 if y0 < y1 else -1)
        err = dx + dy
        while True:
            self.p(x0, y0, c)
            if x0 == x1 and y0 == y1:
                break
            e2 = 2 * err
            if e2 >= dy:
                err += dy
                x0 += sx
            if e2 <= dx:
                err += dx
                y0 += sy

    def disc(self, cx, cy, r, c):
        for y in range(self.h):
            for x in range(self.w):
                if (x - cx) ** 2 + (y - cy) ** 2 <= r * r:
                    self.p(x, y, c)

    def ring(self, cx, cy, r, c, th=0.7):
        for y in range(self.h):
            for x in range(self.w):
                d = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
                if abs(d - r) <= th:
                    self.p(x, y, c)

    def shaded_disc(self, cx, cy, r, ramp, lo=0, hi=4):
        """Kugel mit Licht von oben links."""
        for y in range(self.h):
            for x in range(self.w):
                d = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
                if d <= r:
                    lx = (x - cx + 0.6 * r) ** 2 + (y - cy + 0.6 * r) ** 2
                    t = 1 - min(1, lx ** 0.5 / (1.7 * r))
                    i = lo + int(round(t * (hi - lo)))
                    self.p(x, y, ramp[max(lo, min(hi, i))])

    def noise(self, ramp, weights, rng, x0=0, y0=0, x1=None, y1=None, clump=0.0):
        x1 = self.w - 1 if x1 is None else x1
        y1 = self.h - 1 if y1 is None else y1
        idx = list(range(len(weights)))
        prev = None
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                if prev is not None and rng.random() < clump:
                    i = prev
                else:
                    i = rng.choices(idx, weights)[0]
                prev = i
                self.p(x, y, ramp[i])

    def sprite(self, rows, legend, ox=0, oy=0):
        for y, row in enumerate(rows):
            for x, ch in enumerate(row):
                if ch in ('.', ' '):
                    continue
                self.p(ox + x, oy + y, legend[ch])

    def outline(self, c=OUT, diag=False):
        src = self.a[:, :, 3] > 0
        add = []
        for y in range(self.h):
            for x in range(self.w):
                if src[y, x]:
                    continue
                nb = [(1, 0), (-1, 0), (0, 1), (0, -1)]
                if diag:
                    nb += [(1, 1), (-1, -1), (1, -1), (-1, 1)]
                for dx, dy in nb:
                    xx, yy = x + dx, y + dy
                    if 0 <= xx < self.w and 0 <= yy < self.h and src[yy, xx]:
                        add.append((x, y))
                        break
        for x, y in add:
            self.p(x, y, c)

    def paste(self, other, ox=0, oy=0):
        for y in range(other.h):
            for x in range(other.w):
                c = tuple(int(v) for v in other.a[y, x])
                if c[3]:
                    self.p(ox + x, oy + y, c)

    def flip_h(self):
        t = self.copy()
        t.a = self.a[:, ::-1].copy()
        return t

    def recolor(self, src_ramp, dst_ramp):
        t = self.copy()
        for s, d in zip(src_ramp, dst_ramp):
            m = np.all(t.a == np.array(s, np.uint8), axis=2)
            t.a[m] = d
        return t

    def image(self):
        return Image.fromarray(self.a, 'RGBA')


def rng_for(name, salt=0):
    return random.Random(f'{name}:{salt}')


# --- Ausgabe -----------------------------------------------------------------
class Pack:
    """Sammelt Texturen, schreibt Dateien und merkt sich alles für die Vorschau."""

    def __init__(self, root, ns='riftverse'):
        self.root = root
        self.ns = ns
        self.entries = []        # (kategorie, kind, id, Tex oder [Tex], anzeigename)
        self.lang = {}

    def tex_dir(self, kind):
        d = os.path.join(self.root, 'assets', self.ns, 'textures', kind)
        os.makedirs(d, exist_ok=True)
        return d

    def add(self, cat, kind, tid, tex, name=None, frametime=None, interpolate=False, lang_key=None):
        d = self.tex_dir(kind)
        path = os.path.join(d, tid + '.png')
        if isinstance(tex, list):
            w, h = tex[0].w, tex[0].h
            img = Image.new('RGBA', (w, h * len(tex)))
            for i, f in enumerate(tex):
                img.paste(f.image(), (0, i * h))
            img.save(path)
            meta = {'animation': {'frametime': frametime or 4}}
            if interpolate:
                meta['animation']['interpolate'] = True
            with open(path + '.mcmeta', 'w') as fh:
                json.dump(meta, fh, indent=2)
        else:
            tex.image().save(path)
        self.entries.append((cat, kind, tid, tex, name))
        if name and lang_key:
            self.lang[lang_key] = name
