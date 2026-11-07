import os
import sys

sys.path.insert(0, os.path.dirname(__file__))

from lib import Pack
import preview

ARGS = [a for a in sys.argv[1:] if not a.startswith('--')]
OUT = ARGS[0] if ARGS else os.path.join(os.getcwd(), 'riftverse_textures')
PRE = os.path.join(os.path.dirname(OUT), 'riftverse_vorschau')
os.makedirs(PRE, exist_ok=True)

pk = Pack(OUT)
mods = ARGS[1].split(',') if len(ARGS) > 1 else ['materials', 'machines', 'gadgets', 'armor', 'meilenstein1', 'meilenstein2']
for m in mods:
    __import__(m).build(pk)

cats = []
for e in pk.entries:
    if e[0] not in cats:
        cats.append(e[0])
for c in cats:
    ents = [e for e in pk.entries if e[0] == c]
    fn = os.path.join(PRE, c.lower().replace(' ', '_').replace('ü', 'ue') + '.png')
    preview.sheet(ents, f'Riftverse – {c} ({len(ents)})', fn)
    print(fn, len(ents))
if {'materials', 'machines', 'gadgets', 'armor'} <= set(mods) and '--extras' in sys.argv:
    import extras
    extras.write(pk)   # Starthilfe-Modelle; im Repo erzeugt tools/daten/generate.py die echten

# Gesamtübersicht: alle Bögen untereinander
from PIL import Image
sheets = [Image.open(os.path.join(PRE, c.lower().replace(' ', '_').replace('ü', 'ue') + '.png')) for c in cats]
W = max(s.width for s in sheets)
Hh = sum(s.height for s in sheets)
big = Image.new('RGB', (W, Hh), (28, 30, 36))
y = 0
for s in sheets:
    big.paste(s, (0, y))
    y += s.height
big.save(os.path.join(PRE, '00_uebersicht.png'))
print('gesamt', len(pk.entries))
