"""Vorschaubögen: jede Textur 6x vergrößert mit Namen darunter."""
from PIL import Image, ImageDraw, ImageFont

FONT = '/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf'
FONT_B = '/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf'


def _first(tex):
    return tex[0] if isinstance(tex, list) else tex


def sheet(entries, title, path, scale=6, cols=8, bg=(28, 30, 36)):
    font = ImageFont.truetype(FONT, 12)
    tfont = ImageFont.truetype(FONT_B, 20)
    cell_w = 16 * scale + 40
    cell_h = 16 * scale + 44
    big = [e for e in entries if _first(e[3]).w > 16]
    small = [e for e in entries if _first(e[3]).w <= 16]
    rows = (len(small) + cols - 1) // cols
    big_h = sum(_first(e[3]).h * 4 + 40 for e in big)
    W = cols * cell_w + 32
    Hh = 60 + rows * cell_h + big_h + 20
    img = Image.new('RGB', (W, Hh), bg)
    d = ImageDraw.Draw(img)
    d.text((16, 16), title, font=tfont, fill=(230, 234, 236))
    # Schachbrett für Transparenz
    def checker(x, y, w, h):
        s = 8
        for yy in range(0, h, s):
            for xx in range(0, w, s):
                c = (44, 47, 54) if (xx // s + yy // s) % 2 else (36, 39, 45)
                d.rectangle([x + xx, y + yy, x + min(xx + s, w) - 1, y + min(yy + s, h) - 1], fill=c)
    for i, (cat, kind, tid, tex, name) in enumerate(small):
        cx = 16 + (i % cols) * cell_w + 20
        cy = 60 + (i // cols) * cell_h
        t = _first(tex)
        checker(cx, cy, 16 * scale, 16 * scale)
        im = t.image().resize((16 * scale, 16 * scale), Image.NEAREST)
        img.paste(im, (cx, cy), im)
        label = name or tid
        if isinstance(tex, list):
            label += ' ▶'
        while d.textlength(label, font=font) > cell_w - 6 and len(label) > 4:
            label = label[:-2] + '…' if not label.endswith('…') else label[:-3] + '…'
        tw = d.textlength(label, font=font)
        d.text((cx + 8 * scale - tw / 2, cy + 16 * scale + 6), label, font=font, fill=(200, 205, 210))
    y = 60 + rows * cell_h
    for (cat, kind, tid, tex, name) in big:
        t = _first(tex)
        sc = 4 if t.w <= 64 else 2
        checker(32, y, t.w * sc, t.h * sc)
        im = t.image().resize((t.w * sc, t.h * sc), Image.NEAREST)
        img.paste(im, (32, y), im)
        d.text((32 + t.w * sc + 16, y + 8), name or tid, font=font, fill=(200, 205, 210))
        y += t.h * sc + 40
    img.save(path)
    return path
