"""Prüft alle Storylines (data/*/riftverse_story/**/*.json) gegen das Story-Format aus docs/story.md.

- Pflichtfelder, bekannte Auslöser- und Aktionstypen
- Welt ist ein einzigartiges Universum
- Sprünge (weiter, schritt), Figuren, Notizen und Antworten, auf die verwiesen wird, gibt es
- Flags, die abgefragt werden, setzt auch irgendetwas
- Riftverse-Items, -Blöcke und Fortschritte gibt es wirklich

Aufruf aus dem Repo-Wurzelordner:
    python3 tools/story/pruefen.py

Läuft ohne Zusatzpakete (auch in GitHub Actions). Gibt bei Fehlern Exit-Code 1 zurück.
"""
import json
import os
import re
import sys

ROOT = os.path.join(os.path.dirname(__file__), '..', '..')
RES = os.path.join(ROOT, 'src', 'main', 'resources')
DATA = os.path.join(RES, 'data')
TYPE_JAVA = os.path.join(ROOT, 'src', 'main', 'java', 'dev', 'riftverse', 'universe', 'UniverseType.java')
LANG = os.path.join(RES, 'assets', 'riftverse', 'lang', 'de_de.json')

AUSLOESER = {
    'ankunft': [], 'bereich': ['pos'], 'item': ['item'], 'abgeben': ['figur', 'item'], 'besiegt': ['mob'],
    'reden': ['figur'], 'block': ['pos'], 'notiz_gelesen': ['notiz'], 'zeit': ['wann'], 'flag': ['flag'],
    'antwort': ['antwort'],
}
AKTIONEN = {
    'notiz': ['id', 'titel', 'text'], 'dialog': ['figur', 'zeilen'], 'titel': ['titel'], 'nachricht': ['text'],
    'item': ['item'], 'xp': ['menge'], 'sound': ['sound'], 'flag': ['flag'], 'figur': ['figur'], 'mob': ['mob', 'ort'],
    'bloecke': ['bloecke'], 'schritt': ['schritt'], 'abschluss': [],
}

fehler = []


def einzigartige_welten():
    """Liest die einzigartigen Typen aus UniverseType.java (fünftes Argument true)."""
    out = set()
    with open(TYPE_JAVA, encoding='utf-8') as fh:
        for m in re.finditer(r'\b[A-Z_]+\("([a-z0-9_]+)",\s*[^,]+,\s*[^,]+,\s*\d+,\s*(true|false),\s*(true|false)\)', fh.read()):
            if m.group(2) == 'true':
                out.add(m.group(1))
    return out


def riftverse_ids():
    with open(LANG, encoding='utf-8') as fh:
        lang = json.load(fh)
    items = {k.split('.', 2)[2] for k in lang if k.startswith('item.riftverse.') and k.count('.') == 2}
    blocks = {k.split('.', 2)[2] for k in lang if k.startswith('block.riftverse.') and k.count('.') == 2}
    return items | blocks, blocks


def fortschritt_gibt_es(rl):
    ns, path = rl.split(':', 1) if ':' in rl else ('minecraft', rl)
    if ns != 'riftverse':
        return True
    return os.path.isfile(os.path.join(DATA, ns, 'advancement', path + '.json'))


def ort_ok(o, braucht_y=False):
    return isinstance(o, list) and len(o) in ((3,) if braucht_y else (2, 3)) and all(isinstance(x, int) for x in o)


def liste(x):
    return x if isinstance(x, list) else [x]


def pruefe(datei, rel):
    wo = rel
    try:
        with open(datei, encoding='utf-8') as fh:
            s = json.load(fh)
    except (OSError, ValueError) as e:
        fehler.append(f'{wo}: kein gültiges JSON ({e})')
        return
    def f(msg):
        fehler.append(f'{wo}: {msg}')

    for k in ('welt', 'titel', 'schritte'):
        if k not in s:
            f(f'„{k}“ fehlt')
    if s.get('welt') not in WELTEN:
        f(f'Welt „{s.get("welt")}“ ist kein einzigartiges Universum ({", ".join(sorted(WELTEN))})')
    if s.get('geltung', 'welt') not in ('welt', 'spieler'):
        f('„geltung“ muss welt oder spieler sein')
    if 'fortschritt' in s and not fortschritt_gibt_es(s['fortschritt']):
        f(f'Fortschritt {s["fortschritt"]} gibt es nicht (data/riftverse/advancement/…)')

    figuren = s.get('figuren', {})
    for fid, fig in figuren.items():
        if 'name' not in fig:
            f(f'Figur „{fid}“: „name“ fehlt')
        if not ort_ok(fig.get('ort')):
            f(f'Figur „{fid}“: „ort“ muss [x, z] oder [x, y, z] sein')

    schritte = s.get('schritte', [])
    ids = []
    for i, sch in enumerate(schritte):
        ids.append(sch.get('id', f'schritt_{i + 1}'))
    if len(ids) != len(set(ids)):
        f('Schritt-Ids doppelt')
    start = s.get('start', ids[0] if ids else None)
    if start not in ids:
        f(f'Start-Schritt „{start}“ gibt es nicht')

    gesetzt, abgefragt, notizen, gelesen, antwort_ids, antwort_abfragen = set(), set(), set(), set(), set(), set()

    def item_ok(rl, wo2, block=False):
        if not isinstance(rl, str) or not re.fullmatch(r'[a-z0-9_.-]+:[a-z0-9_./-]+', rl):
            f(f'{wo2}: ungültige Id „{rl}“')
            return
        ns, path = rl.split(':', 1)
        if ns == 'riftverse' and path not in (BLOECKE if block else ITEMS):
            f(f'{wo2}: {rl} gibt es im Mod nicht')

    def aktionen(liste_, wo2):
        for k, a in enumerate(liste(liste_ or [])):
            w = f'{wo2}, Aktion {k + 1}'
            typ = a.get('typ')
            if typ not in AKTIONEN:
                f(f'{w}: unbekannter Typ „{typ}“')
                continue
            for feld in AKTIONEN[typ]:
                if feld not in a:
                    f(f'{w} ({typ}): „{feld}“ fehlt')
            if typ == 'schritt' and a.get('schritt') not in ids:
                f(f'{w}: Sprung zu „{a.get("schritt")}“, das gibt es nicht')
            if typ in ('dialog', 'figur') and a.get('figur') not in figuren:
                f(f'{w}: unbekannte Figur „{a.get("figur")}“')
            if typ == 'flag':
                gesetzt.add(a.get('flag'))
            if typ == 'notiz':
                notizen.add(a.get('id'))
            if typ == 'item':
                item_ok(a.get('item'), w)
            if typ == 'bloecke':
                for b in a.get('bloecke', []):
                    item_ok(b.get('block'), w, block=True)
                    if 'von' in b:
                        if not (ort_ok(b.get('von'), True) and ort_ok(b.get('bis'), True)):
                            f(f'{w}: „von“ und „bis“ brauchen [x, y, z]')
                    elif not ort_ok(b.get('pos')):
                        f(f'{w}: Block ohne gültige „pos“')
            if typ == 'dialog':
                zeilen = a.get('zeilen', [])
                if not zeilen:
                    f(f'{w}: Dialog ohne Zeilen')
                for z_i, z in enumerate(zeilen):
                    if isinstance(z, str):
                        continue
                    if 'text' not in z:
                        f(f'{w}, Zeile {z_i + 1}: „text“ fehlt')
                    if 'wenn' in z:
                        abgefragt.add(z['wenn'].lstrip('!'))
                    antworten = z.get('antworten', [])
                    if len(antworten) > 4:
                        f(f'{w}, Zeile {z_i + 1}: höchstens 4 Antworten')
                    for an in antworten:
                        if 'text' not in an:
                            f(f'{w}, Zeile {z_i + 1}: Antwort ohne „text“')
                        if 'flag' in an:
                            gesetzt.add(an['flag'])
                        if 'id' in an:
                            antwort_ids.add(an['id'])
                        if 'schritt' in an and an['schritt'] not in ids:
                            f(f'{w}: Antwort springt zu „{an["schritt"]}“, das gibt es nicht')
                        aktionen(an.get('aktionen'), f'{w}, Antwort')

    for i, sch in enumerate(schritte):
        w = f'Schritt „{ids[i]}“'
        if 'ausloeser' not in sch:
            f(f'{w}: „ausloeser“ fehlt')
        for k, a in enumerate(liste(sch.get('ausloeser', []))):
            typ = a.get('typ')
            if typ not in AUSLOESER:
                f(f'{w}, Auslöser {k + 1}: unbekannter Typ „{typ}“')
                continue
            for feld in AUSLOESER[typ]:
                if feld not in a:
                    f(f'{w}, Auslöser {k + 1} ({typ}): „{feld}“ fehlt')
            if typ in ('reden', 'abgeben') and a.get('figur') not in figuren:
                f(f'{w}: unbekannte Figur „{a.get("figur")}“')
            if typ == 'flag':
                abgefragt.add(a.get('flag'))
            if typ == 'notiz_gelesen':
                gelesen.add(a.get('notiz'))
            if typ == 'antwort':
                antwort_abfragen.add(a.get('antwort'))
            if typ == 'block' and not ort_ok(a.get('pos'), True):
                f(f'{w}: „block“ braucht pos [x, y, z]')
            if typ == 'bereich' and not ort_ok(a.get('pos')):
                f(f'{w}: „bereich“ braucht pos [x, z] oder [x, y, z]')
            if typ == 'zeit' and a.get('wann') not in ('tag', 'nacht'):
                f(f'{w}: „wann“ muss tag oder nacht sein')
            if typ in ('item', 'abgeben'):
                item_ok(a.get('item'), w)
        bed = sch.get('bedingung', {})
        for k in ('flags', 'nicht'):
            abgefragt.update(liste(bed.get(k, [])))
        if 'weiter' in sch and sch['weiter'] not in ids:
            f(f'{w}: „weiter“ zeigt auf „{sch["weiter"]}“, das gibt es nicht')
        aktionen(sch.get('aktionen'), w)

    for flag in sorted(abgefragt - gesetzt):
        f(f'Flag „{flag}“ wird abgefragt, aber nirgends gesetzt')
    for n in sorted(gelesen - notizen):
        f(f'notiz_gelesen wartet auf „{n}“, aber keine Aktion gibt diese Notiz')
    for a in sorted(antwort_abfragen - antwort_ids):
        f(f'Auslöser „antwort“ wartet auf „{a}“, aber keine Antwort hat diese Id')


def main():
    global WELTEN, ITEMS, BLOECKE
    WELTEN = einzigartige_welten()
    ITEMS, BLOECKE = riftverse_ids()
    anzahl = 0
    for ns in sorted(os.listdir(DATA)):
        base = os.path.join(DATA, ns, 'riftverse_story')
        if not os.path.isdir(base):
            continue
        for dirpath, _, files in os.walk(base):
            for fn in sorted(files):
                if fn.endswith('.json'):
                    p = os.path.join(dirpath, fn)
                    pruefe(p, os.path.relpath(p, DATA).replace(os.sep, '/'))
                    anzahl += 1
    print(f'Storylines: {anzahl} Dateien, einzigartige Welten: {", ".join(sorted(WELTEN))}')
    if fehler:
        for x in fehler:
            print('FEHLER', x)
        sys.exit(1)
    print('Alle Storylines sind in Ordnung.')


if __name__ == '__main__':
    main()
