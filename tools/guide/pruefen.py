"""Prüft das Riftverse-Handbuch (GuideME-Seiten) gegen den Mod.

- Jedes Riftverse-Item hat genau eine Seite (item_ids im Kopf der Seite), damit „G halten“ überall funktioniert.
- Alle Item-, Block- und Rezept-Ids in den Seiten gibt es wirklich.
- <RecipeFor>/<RecipesFor> nur für Items, die ein Rezept haben.
- Links auf andere Seiten, Eltern in der Navigation und Symbole stimmen.

Aufruf aus dem Repo-Wurzelordner:
    python3 tools/guide/pruefen.py

Läuft ohne Zusatzpakete (auch in GitHub Actions). Gibt bei Fehlern Exit-Code 1 zurück.
"""
import json
import os
import re
import sys

ROOT = os.path.join(os.path.dirname(__file__), '..', '..', 'src', 'main', 'resources')
NS = 'riftverse'
ASSETS = os.path.join(ROOT, 'assets', NS)
DATA = os.path.join(ROOT, 'data', NS)
GUIDE = os.path.join(ASSETS, 'guides', NS, 'handbuch')

fehler = []


def fehl(msg):
    fehler.append(msg)


# ------------------------------------------------------------------ Was es im Mod gibt
def registrierte_items():
    """Alle Items mit Item-Modell und Namen (die Peiler-Animationsbilder haben keinen Namen)."""
    with open(os.path.join(ASSETS, 'lang', 'de_de.json'), encoding='utf-8') as fh:
        lang = json.load(fh)
    out = set()
    for f in os.listdir(os.path.join(ASSETS, 'models', 'item')):
        name = f[:-5]
        if f'item.{NS}.{name}' in lang or f'block.{NS}.{name}' in lang:
            out.add(name)
    return out


def bloecke():
    return {f[:-5] for f in os.listdir(os.path.join(ASSETS, 'blockstates'))}


def rezepte():
    """Rezept-Ids und welche Items ein Rezept als Ergebnis haben (inkl. Nebenprodukte)."""
    ids, ergebnisse = set(), set()
    base = os.path.join(DATA, 'recipe')
    for dirpath, _, files in os.walk(base):
        for f in files:
            if not f.endswith('.json'):
                continue
            rel = os.path.relpath(os.path.join(dirpath, f), base)[:-5].replace(os.sep, '/')
            ids.add(f'{NS}:{rel}')
            with open(os.path.join(dirpath, f), encoding='utf-8') as fh:
                r = json.load(fh)
            res = r.get('result')
            if isinstance(res, dict) and 'id' in res:
                ergebnisse.add(res['id'])
            elif isinstance(res, str):
                ergebnisse.add(res)
    return ids, ergebnisse


# ------------------------------------------------------------------ Seiten lesen
def frontmatter(text):
    """Sehr kleiner YAML-Leser für die Kopfzeilen der Seiten: Schlüssel, eine Ebene Unterschlüssel, Listen."""
    if not text.startswith('---\n'):
        return {}, text
    ende = text.index('\n---\n', 4)
    kopf, rest = text[4:ende], text[ende + 5:]
    zeilen = [z for z in kopf.splitlines() if z.strip() and not z.strip().startswith('#')]
    data, aktuell = {}, None
    for n, zeile in enumerate(zeilen):
        einr = len(zeile) - len(zeile.lstrip())
        z = zeile.strip()
        if z.startswith('- '):
            data[aktuell].append(z[2:].strip())
            continue
        k, _, v = z.partition(':')
        v = v.strip()
        if einr == 0:
            aktuell = k
            if v:
                data[k] = v
            else:
                naechste = zeilen[n + 1].strip() if n + 1 < len(zeilen) else ''
                data[k] = [] if naechste.startswith('- ') else {}
        else:
            data[aktuell][k] = v
    return data, rest


def seiten():
    out = {}
    for dirpath, _, files in os.walk(GUIDE):
        for f in files:
            if f.endswith('.md'):
                p = os.path.join(dirpath, f)
                rel = os.path.relpath(p, GUIDE).replace(os.sep, '/')
                if rel.startswith('_'):
                    continue   # Übersetzungen
                with open(p, encoding='utf-8') as fh:
                    out[rel] = fh.read()
    return out


def kurz(i):
    """riftverse:x -> x, minecraft:y bleibt."""
    if ':' not in i:
        return i
    ns, name = i.split(':', 1)
    return name if ns == NS else i


def main():
    items = registrierte_items()
    blocks = bloecke()
    rezept_ids, ergebnisse = rezepte()
    pages = seiten()
    if 'index.md' not in pages:
        fehl('index.md (Startseite) fehlt')

    zuordnung = {}
    for name, text in sorted(pages.items()):
        try:
            fm, body = frontmatter(text)
        except ValueError:
            fehl(f'{name}: Kopf nicht geschlossen')
            continue
        for i in fm.get('item_ids', []) or []:
            k = kurz(i)
            if ':' in k:
                continue
            if k not in items:
                fehl(f'{name}: item_ids nennt unbekanntes Item {i}')
            if k in zuordnung:
                fehl(f'{name}: {i} hat schon die Seite {zuordnung[k]}')
            zuordnung[k] = name
        nav = fm.get('navigation')
        if isinstance(nav, dict):
            parent = nav.get('parent')
            if parent and parent not in pages:
                fehl(f'{name}: Eltern-Seite {parent} gibt es nicht')
            icon = nav.get('icon')
            if icon and ':' not in kurz(icon) and kurz(icon) not in items:
                fehl(f'{name}: Symbol {icon} gibt es nicht')
            if not nav.get('title'):
                fehl(f'{name}: Navigation ohne Titel')
        if not re.search(r'^# ', body, re.M):
            fehl(f'{name}: keine Überschrift erster Ordnung')

        for tag, i in re.findall(r'<(ItemLink|ItemImage|ItemIcon|RecipeFor|RecipesFor|BlockImage|Block)\b[^>]*?\bid="([^"]+)"', body):
            k = kurz(i)
            if ':' in k:
                continue   # Vanilla
            if tag in ('BlockImage', 'Block'):
                if k not in blocks:
                    fehl(f'{name}: <{tag}> mit unbekanntem Block {i}')
            elif k not in items:
                fehl(f'{name}: <{tag}> mit unbekanntem Item {i}')
            if tag in ('RecipeFor', 'RecipesFor') and f'{NS}:{k}' not in ergebnisse:
                fehl(f'{name}: <{tag} id="{i}"> – kein Rezept stellt das her')
        for i in re.findall(r'<Recipe\b[^>]*?\bid="([^"]+)"', body):
            full = i if ':' in i else f'{NS}:{i}'
            if full not in rezept_ids:
                fehl(f'{name}: Rezept {i} gibt es nicht')
        for ziel in re.findall(r'\]\(([^)#]+\.md)(?:#[^)]*)?\)', body):
            if ':' in ziel:
                continue
            p = os.path.normpath(os.path.join(os.path.dirname(name), ziel)).replace(os.sep, '/')
            if p not in pages:
                fehl(f'{name}: Link auf {ziel} führt ins Leere')

    ohne = sorted(items - set(zuordnung))
    for k in ohne:
        fehl(f'Item ohne Handbuch-Seite: {NS}:{k}')

    print(f'Handbuch: {len(pages)} Seiten, {len(zuordnung)} von {len(items)} Items mit eigener Seite')
    if fehler:
        for f in fehler:
            print('FEHLER', f)
        sys.exit(1)
    print('Handbuch ist vollständig.')


if __name__ == '__main__':
    main()
