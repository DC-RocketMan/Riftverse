"""Vorschau der Modelle aus Meilenstein 10 (Gleebs und Tiere von Bridge's Inn), gerechnet wie im Spiel: Box-UV-Modelle aus
den Java-Klassen, Texturen aus src/main/resources. Ein kleiner Software-Renderer, ohne Minecraft.

Aufruf aus dem Repo-Wurzelordner:
    python3 tools/texturen/modell_vorschau.py

Schreibt tools/texturen/vorschau/meilenstein_10_modelle.png. Die Teile müssen zu client/dorf/GleebModel und
client/dorf/TierModelle passen.
"""
import math
from PIL import Image, ImageDraw

def rotm(xr, yr, zr):
    cx, sx, cy, sy, cz, sz = math.cos(xr), math.sin(xr), math.cos(yr), math.sin(yr), math.cos(zr), math.sin(zr)
    Rx = [[1,0,0],[0,cx,-sx],[0,sx,cx]]; Ry = [[cy,0,sy],[0,1,0],[-sy,0,cy]]; Rz = [[cz,-sz,0],[sz,cz,0],[0,0,1]]
    return mm(mm(Rz, Ry), Rx)
def mm(a, b): return [[sum(a[i][k]*b[k][j] for k in range(3)) for j in range(3)] for i in range(3)]
def mv(a, v): return [sum(a[i][k]*v[k] for k in range(3)) for i in range(3)]

class Teil:
    def __init__(self, pivot=(0,0,0), rot=(0,0,0), boxes=(), kinder=(), sichtbar=True):
        self.pivot, self.rot, self.boxes, self.kinder, self.sichtbar = pivot, rot, list(boxes), list(kinder), sichtbar

def faces_of(tex, b):
    (u, v), (x0, y0, z0), (w, h, d) = b
    x1, y1, z1 = x0 + w, y0 + h, z0 + d
    F = []
    # (region u0,v0,tw,th, normal, func(i,j)->3D quad corners)
    def quad(p, du, dv):
        return lambda i, j, tw, th: [add(p, du, i/tw, dv, j/th), add(p, du, (i+1)/tw, dv, j/th), add(p, du, (i+1)/tw, dv, (j+1)/th), add(p, du, i/tw, dv, (j+1)/th)]
    F.append((u+d, v+d, w, h, (0,0,-1), quad((x0,y0,z0), (w,0,0), (0,h,0))))           # vorne (Norden)
    F.append((u+2*d+w, v+d, w, h, (0,0,1), quad((x1,y0,z1), (-w,0,0), (0,h,0))))      # hinten
    F.append((u, v+d, d, h, (-1,0,0), quad((x0,y0,z1), (0,0,-d), (0,h,0))))           # rechts (-x)
    F.append((u+d+w, v+d, d, h, (1,0,0), quad((x1,y0,z0), (0,0,d), (0,h,0))))         # links (+x)
    F.append((u+d, v, w, d, (0,-1,0), quad((x0,y0,z1), (w,0,0), (0,0,-d))))           # oben
    F.append((u+d+w, v, w, d, (0,1,0), quad((x0,y1,z0), (w,0,0), (0,0,d))))           # unten
    return F

def add(p, du, a, dv, b):
    return tuple(p[k] + du[k]*a + dv[k]*b for k in range(3))

def render(tex, teil, yaw=-35, pitch=18, s=14, size=(360, 440), off=(180, 60), bg=(0,0,0,0)):
    tex = tex.convert('RGBA')
    polys = []
    def walk(t, R, T):
        if not t.sichtbar: return
        Rl = mm(R, rotm(*t.rot)); Tl = [T[k] + mv(R, t.pivot)[k] for k in range(3)]
        for b in t.boxes:
            for (u0, v0, tw, th, n, q) in faces_of(tex, b):
                if tw <= 0 or th <= 0: continue
                for i in range(tw):
                    for j in range(th):
                        c = tex.getpixel((u0+i, v0+j))
                        if c[3] < 10: continue
                        pts = [[Tl[k] + mv(Rl, p)[k] for k in range(3)] for p in q(i, j, tw, th)]
                        nn = mv(Rl, n)
                        polys.append((pts, nn, c))
        for k in t.kinder: walk(k, Rl, Tl)
    walk(teil, [[1,0,0],[0,1,0],[0,0,1]], [0,0,0])
    V = mm(rotm(math.radians(pitch), 0, 0), rotm(0, math.radians(yaw), 0))
    img = Image.new('RGBA', size, bg); dr = ImageDraw.Draw(img)
    licht = {}
    out = []
    for pts, nn, c in polys:
        n2 = mv(V, nn)
        if n2[2] >= -1e-6: continue
        p2 = [mv(V, p) for p in pts]
        z = sum(p[2] for p in p2) / 4
        hell = 0.6 + 0.4 * max(0, -n2[2]) * 0.6 + 0.4 * max(0, -n2[1]) * 0.5
        hell = min(1.0, hell)
        col = tuple(int(c[k] * hell) for k in range(3)) + (255,)
        out.append((z, [(off[0] + p[0]*s, off[1] + p[1]*s) for p in p2], col))
    out.sort(key=lambda o: -o[0])
    for z, poly, col in out:
        dr.polygon(poly, fill=col, outline=col)
    return img


# ---------------------------------------------------------------------------------------------- Modelle
import os
from PIL import ImageFont

HIER = os.path.dirname(os.path.abspath(__file__))
ENT = os.path.join(HIER, '..', '..', 'src', 'main', 'resources', 'assets', 'riftverse', 'textures', 'entity')


def B(u, v, x, y, z, w, h, d):
    return ((u, v), (x, y, z), (w, h, d))


def gleeb(besen=False, zaubert=False):
    bommel = Teil((0, -1.2, 1.6), (-0.8, 0, 0), [B(48, 44, -1, -1, -1, 2, 2, 2)])
    z2 = Teil((0, -1.5, 1.2), (-0.95, 0, 0), [B(48, 38, -1.5, -2, -1.5, 3, 2, 3)], [bommel])
    z1 = Teil((0, -4, 0.8), (-1.0 if besen else -0.7, 0, 0), [B(28, 38, -2.5, -2, -2.5, 5, 2, 5)], [z2])
    krone = Teil((0, -9, 0), (0, 0, 0), [B(0, 38, -3.5, -4, -3.5, 7, 4, 7)], [z1])
    kopf = Teil((0, 13, 0), (0, 0, 0), [B(0, 0, -4, -8, -3.5, 8, 8, 7), B(30, 0, -1, -4, -4.5, 2, 2, 1),
                                        B(30, 5, -3, -3, -4.3, 6, 3, 1), B(0, 27, -5, -9, -4.5, 10, 1, 9)],
                [Teil((-4, -4, 0), (0, -0.35, 0.45), [B(36, 0, -1, -2, -1, 1, 3, 2)]),
                 Teil((4, -4, 0), (0, 0.35, -0.45), [B(42, 0, 0, -2, -1, 1, 3, 2)]), krone])
    koerper = Teil((0, 13, 0), (0.12 if besen else 0, 0, 0), [B(0, 16, -3.5, 0, -2, 7, 6, 4)],
                   [Teil((0, 0, 0), (0, 0, 0), [B(0, 50, -0.5, 5.5, -7, 1, 1, 12), B(28, 50, -1.5, 4.5, 5, 3, 3, 4)], sichtbar=besen)])
    arm = -2.6 if zaubert else -0.9 if besen else 0
    armz = 0.35 if zaubert else 0.08
    bein = -1.35 if besen else 0
    return Teil(kinder=[kopf, koerper,
                        Teil((-4.5, 13.5, 0), (arm, 0, armz), [B(22, 16, -1, 0, -1, 2, 6, 2)]),
                        Teil((4.5, 13.5, 0), (arm, 0, -armz), [B(30, 16, -1, 0, -1, 2, 6, 2)]),
                        Teil((-1.6, 19, 0), (bein, 0.25 if besen else 0, 0), [B(38, 16, -1.5, 0, -1.5, 3, 5, 3)]),
                        Teil((1.6, 19, 0), (bein, -0.25 if besen else 0, 0), [B(50, 16, -1.5, 0, -1.5, 3, 5, 3)])])


def glimmhase():
    bein = [B(0, 13, -1, 0, -1, 2, 3, 2)]
    kopf = Teil((0, 17.5, -3.5), (0, 0, 0), [B(28, 0, -2.5, -4, -4.5, 5, 5, 5)],
                [Teil((-1.5, -4, -2), (-0.15, 0, -0.15), [B(48, 0, -0.5, -6, -1, 1, 6, 2)]),
                 Teil((1.5, -4, -2), (-0.15, 0, 0.15), [B(54, 0, -0.5, -6, -1, 1, 6, 2)])])
    return Teil(kinder=[Teil((0, 18.5, 0), (0, 0, 0), [B(0, 0, -3, -2.5, -4, 6, 5, 8)]), kopf,
                        Teil((-2, 21, -2.5), boxes=bein), Teil((2, 21, -2.5), boxes=bein),
                        Teil((-2, 21, 2.5), boxes=bein), Teil((2, 21, 2.5), boxes=bein),
                        Teil((0, 17.5, 3.5), boxes=[B(8, 13, -1.5, -1.5, 0, 3, 3, 3)])])


def pusteling():
    return Teil(kinder=[Teil((0, 24, 0), (0, 0, 0), [B(0, 0, -4, -8, -4, 8, 8, 8)],
                             [Teil((0, 0, 0), (0.1, 0, 0.08), [B(32, 0, -2, -11, -2, 4, 3, 4)])])])


def schimmerfalter():
    return Teil(kinder=[Teil((0, 20, 0), (0, 0, 0), [B(0, 0, -0.5, -0.5, -2.5, 1, 1, 5)],
                             [Teil((0.5, -0.5, 0), (0, 0, -0.5), [B(0, 8, 0, 0, -3, 7, 0, 6)]),
                              Teil((-0.5, -0.5, 0), (0, 0, 0.5), [B(0, 16, -7, 0, -3, 7, 0, 6)])])])


def bild(pfad):
    return Image.open(os.path.join(ENT, pfad))


def main():
    f = ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf', 18)
    fb = ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf', 24)
    berufe = [('buerger', 'Bürger'), ('baeckerin', 'Bäckerin'), ('kraeuterhexe', 'Kräuterhexe'), ('alchemist', 'Alchemist'),
              ('runenschmied', 'Runenschmied'), ('sternenkundlerin', 'Sternenkundlerin')]
    W, H = 300, 400
    blatt = Image.new('RGBA', (W * 6, 60 + H * 3), (40, 44, 52, 255))
    d = ImageDraw.Draw(blatt)
    d.text((16, 14), "Riftverse – Meilenstein 10: Gleebs und Tiere von Bridge's Inn", fill=(240, 240, 240), font=fb)

    def setzen(img, spalte, zeile, text):
        blatt.alpha_composite(img, (spalte * W, 60 + zeile * H))
        d.text((spalte * W + 20, 60 + zeile * H + H - 34), text, fill=(230, 230, 230), font=f)

    for i, (b, name) in enumerate(berufe):
        tex = bild(f'gleeb/{b}.png')
        setzen(render(tex, gleeb(), yaw=-32, pitch=14, s=12, size=(W, H - 40), off=(W // 2, 70)), i, 0, name)
        setzen(render(tex, gleeb(), yaw=150, pitch=14, s=12, size=(W, H - 40), off=(W // 2, 70)), i, 1, name + ' von hinten')
    setzen(render(bild('gleeb/buerger.png'), gleeb(besen=True), yaw=-55, pitch=14, s=12, size=(W, H - 40), off=(W // 2, 80)),
           0, 2, 'Besenflieger')
    setzen(render(bild('gleeb/alchemist.png'), gleeb(zaubert=True), yaw=-25, pitch=14, s=12, size=(W, H - 40), off=(W // 2, 110)),
           1, 2, 'Gleeb beim Zaubern')
    setzen(render(bild('tiere/glimmhase.png'), glimmhase(), yaw=-50, pitch=18, s=20, size=(W, H - 40), off=(W // 2, -150)),
           2, 2, 'Glimmhase')
    setzen(render(bild('tiere/pusteling.png'), pusteling(), yaw=-35, pitch=18, s=20, size=(W, H - 40), off=(W // 2, -220)),
           3, 2, 'Pusteling')
    setzen(render(bild('tiere/schimmerfalter.png'), schimmerfalter(), yaw=-20, pitch=40, s=16, size=(W, H - 40), off=(W // 2, -60)),
           4, 2, 'Schimmerfalter')
    irr = bild('tiere/irrlicht.png').convert('RGBA').resize((160, 160), Image.NEAREST)
    platte = Image.new('RGBA', (W, H - 40), (0, 0, 0, 0))
    platte.alpha_composite(irr, ((W - 160) // 2, 100))
    setzen(platte, 5, 2, 'Irrlicht')
    ziel = os.path.join(HIER, 'vorschau', 'meilenstein_10_modelle.png')
    blatt.save(ziel)
    print(ziel)


if __name__ == '__main__':
    main()
