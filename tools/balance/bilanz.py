#!/usr/bin/env python3
"""
Materialbilanz für Riftverse (Meilenstein 7).

Löst die Rezept-JSONs aus src/main/resources/data rekursiv bis auf Rohstoffe auf und rechnet einen
Modell-Durchlauf (Design-Dokument, Abschnitt „Metalle und Xenit“: rund 45 Maschinen, alle Generatoren und
Speicher, rund 220 Kabel, die Multiblöcke, Kanone Mk I–IV, vier Rüstungsteile bis „perfektioniert“ und
8 Gadgets). Das Ergebnis wird mit der Tabelle aus dem Dokument verglichen.

Außerdem:
  * listet Items, die in Rezepten vorkommen, aber selbst kein Rezept und keine Rohstoff-Zuordnung haben,
  * schätzt die Xenit-Ausbeute pro Stunde Strip-Mining aus der Platzierung (Vergleich mit dem Eisen-Band),
  * schätzt Zinn pro Chunk.

Aufruf:  python3 tools/balance/bilanz.py            (Tabelle)
         python3 tools/balance/bilanz.py --baum ID  (Aufschlüsselung eines Items)
Rückgabe 1, wenn eine Spalte mehr als 25 % vom Dokument abweicht (für die CI nicht verwendet, nur als Hinweis).
"""
import glob
import json
import math
import os
import sys
from collections import defaultdict

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
DATA = os.path.join(ROOT, 'src', 'main', 'resources', 'data')

# ---------------------------------------------------------------- Rohstoffe
# Zählung in Barren. Staub zählt wie ein Barren (Erzmühle: 1 Erz → 2 Staub → 2 Barren), Roh-Erz und Erzblock wie 2.
SPALTEN = ['xenit', 'eisen', 'kupfer', 'zinn', 'gold', 'schrott', 'redstone', 'flux', 'gravitit', 'null']
ROH = {
    'riftverse:xenit_ingot': ('xenit', 1), 'riftverse:xenit_staub': ('xenit', 1), 'riftverse:raw_xenit': ('xenit', 2),
    'riftverse:xenit_ore': ('xenit', 2), 'riftverse:deepslate_xenit_ore': ('xenit', 2),
    'minecraft:iron_ingot': ('eisen', 1), 'riftverse:eisen_staub': ('eisen', 1), 'minecraft:raw_iron': ('eisen', 2),
    'minecraft:iron_nugget': ('eisen', 1 / 9), 'minecraft:iron_ore': ('eisen', 2),
    'minecraft:copper_ingot': ('kupfer', 1), 'riftverse:kupfer_staub': ('kupfer', 1), 'minecraft:raw_copper': ('kupfer', 2),
    'minecraft:copper_ore': ('kupfer', 2),
    'riftverse:tin_ingot': ('zinn', 1), 'riftverse:zinn_staub': ('zinn', 1), 'riftverse:raw_tin': ('zinn', 2),
    'riftverse:tin_ore': ('zinn', 2),
    'minecraft:gold_ingot': ('gold', 1), 'riftverse:gold_staub': ('gold', 1), 'minecraft:raw_gold': ('gold', 2),
    'minecraft:gold_nugget': ('gold', 1 / 9), 'minecraft:gold_ore': ('gold', 2),
    'riftverse:alien_schrott': ('schrott', 1), 'minecraft:redstone': ('redstone', 1),
    'riftverse:fluxkristall': ('flux', 1), 'riftverse:fluxstaub': ('flux', 1 / 3),
    'riftverse:feder': ('schrott', 1), 'riftverse:mini_motor': ('schrott', 1), 'riftverse:gravitit': ('gravitit', 1), 'riftverse:nullmaterie': ('null', 1),
}
# Vanilla-Teile, die Metall enthalten (Werkbank-Rezepte aus Vanilla, nicht im Mod)
VANILLA = {
    'minecraft:iron_block': {'eisen': 9}, 'minecraft:gold_block': {'gold': 9}, 'minecraft:copper_block': {'kupfer': 9},
    'minecraft:compass': {'eisen': 4, 'redstone': 1}, 'minecraft:clock': {'gold': 4, 'redstone': 1},
    'minecraft:bucket': {'eisen': 3}, 'minecraft:hopper': {'eisen': 5}, 'minecraft:cauldron': {'eisen': 7},
    'minecraft:anvil': {'eisen': 31}, 'minecraft:iron_bars': {'eisen': 6 / 16}, 'minecraft:lightning_rod': {'kupfer': 1},
    'minecraft:piston': {'eisen': 1, 'redstone': 1}, 'minecraft:redstone_lamp': {'redstone': 4},
    'minecraft:note_block': {'redstone': 1}, 'minecraft:shears': {'eisen': 2}, 'minecraft:minecart': {'eisen': 5},
    'minecraft:rail': {'eisen': 6 / 16}, 'minecraft:chain': {'eisen': 1 + 2 / 9}, 'minecraft:observer': {'redstone': 2},
    'minecraft:comparator': {'redstone': 3}, 'minecraft:repeater': {'redstone': 3}, 'minecraft:redstone_block': {'redstone': 9},
    'minecraft:spyglass': {'kupfer': 2}, 'minecraft:light_weighted_pressure_plate': {'gold': 2},
    'minecraft:heavy_weighted_pressure_plate': {'eisen': 2}, 'minecraft:golden_apple': {'gold': 8},
}
# Gemeinsame Tags aus NeoForge/Vanilla, die der Mod nicht selbst füllt
TAG_FALLBACK = {
    'c:ingots/iron': 'minecraft:iron_ingot', 'c:ingots/copper': 'minecraft:copper_ingot', 'c:ingots/gold': 'minecraft:gold_ingot',
    'c:dusts/redstone': 'minecraft:redstone', 'c:storage_blocks/iron': 'minecraft:iron_block', 'c:rods/iron': 'riftverse:eisen_stange',
    'c:raw_materials/iron': 'minecraft:raw_iron', 'c:raw_materials/gold': 'minecraft:raw_gold',
    'c:raw_materials/copper': 'minecraft:raw_copper', 'c:ores/iron': 'minecraft:iron_ore', 'c:ores/gold': 'minecraft:gold_ore',
    'c:ores/copper': 'minecraft:copper_ore', 'c:glass_blocks': 'minecraft:glass', 'c:glass_panes': 'minecraft:glass_pane',
    'minecraft:planks': 'minecraft:oak_planks', 'minecraft:logs': 'minecraft:oak_log', 'c:gems/diamond': 'minecraft:diamond',
}


def lade_tags():
    tags = {}
    for path in glob.glob(os.path.join(DATA, '*', 'tags', 'item', '**', '*.json'), recursive=True):
        rel = os.path.relpath(path, DATA).replace(os.sep, '/')
        ns, _, _, rest = rel.split('/', 3)
        with open(path, encoding='utf-8') as fh:
            tags[f'{ns}:{rest[:-5]}'] = [v if isinstance(v, str) else v['id'] for v in json.load(fh)['values']]
    return tags


TAGS = lade_tags()


def tag_item(tag, seen=None):
    """Repräsentatives Item eines Tags: bevorzugt Riftverse-Items, dann der erste Eintrag."""
    seen = seen or set()
    if tag in seen:
        return None
    seen.add(tag)
    werte = TAGS.get(tag)
    if not werte:
        return TAG_FALLBACK.get(tag)
    kandidaten = []
    for v in werte:
        kandidaten.append(tag_item(v[1:], seen) if v.startswith('#') else v)
    kandidaten = [k for k in kandidaten if k]
    for k in kandidaten:
        if k in ROH:
            return k
    for k in kandidaten:
        if k.startswith('riftverse:'):
            return k
    return kandidaten[0] if kandidaten else TAG_FALLBACK.get(tag)


def zutat_id(ing):
    if isinstance(ing, list):
        ing = ing[0]
    if 'item' in ing:
        return ing['item']
    if 'tag' in ing:
        return tag_item(ing['tag']) or '#' + ing['tag']
    return None


# ---------------------------------------------------------------- Rezepte einlesen
def ergebnis(r):
    res = r.get('result')
    if isinstance(res, str):
        return res, 1
    if isinstance(res, dict) and 'id' in res:
        return res['id'], res.get('count', 1)
    return None, 0


def lade_rezepte():
    rezepte = defaultdict(list)   # Item -> [(Name, Menge, {Zutat: Anzahl})]
    for path in sorted(glob.glob(os.path.join(DATA, 'riftverse', 'recipe', '**', '*.json'), recursive=True)):
        name = os.path.relpath(path, os.path.join(DATA, 'riftverse', 'recipe')).replace(os.sep, '/')[:-5]
        with open(path, encoding='utf-8') as fh:
            r = json.load(fh)
        typ = r.get('type', '')
        out, menge = ergebnis(r)
        if typ in ('riftverse:fluid_mixer', 'riftverse:raffinerie') or not out:
            continue
        zutaten = defaultdict(float)
        if typ in ('riftverse:werkbank_shaped', 'minecraft:crafting_shaped'):
            for zeile in r['pattern']:
                for ch in zeile:
                    if ch != ' ':
                        zutaten[zutat_id(r['key'][ch])] += 1
        elif typ in ('riftverse:werkbank_shapeless', 'minecraft:crafting_shapeless'):
            for ing in r['ingredients']:
                zutaten[zutat_id(ing)] += 1
        elif typ in ('minecraft:blasting', 'minecraft:smelting'):
            zutaten[zutat_id(r['ingredient'])] += 1
        elif 'ingredients' in r:
            for ing in r['ingredients']:
                zutaten[zutat_id(ing['ingredient'])] += ing.get('count', 1)
        else:
            continue
        if r.get('loetzinn'):
            zutaten['riftverse:loetzinn'] += r['loetzinn']
        zutaten.pop(None, None)
        rezepte[out].append((name, menge, dict(zutaten)))
    return rezepte


REZEPTE = lade_rezepte()
# Fluid-Kartuschen und Portalfluid zählen nicht zum Metall (kommen aus Fluxstaub und Säuregel)
OHNE_KOSTEN_PREFIX = ('minecraft:',)
# Rohstoffe aus den Universen ohne Metall
UNIVERSUM = {'riftverse:biomasse_pulpe', 'riftverse:chronoglas_sand', 'riftverse:saeuregel', 'riftverse:schwebstein',
             'riftverse:spiegelkristall'}
FEHLT = set()


def gewicht(kosten):
    """Für die Wahl des billigsten Rezepts: Metalle voll, alles andere ein Zehntel."""
    return sum(v * (1 if k in ('xenit', 'eisen', 'kupfer', 'zinn', 'gold') else 0.1) for k, v in kosten.items())


_cache = {}


def kosten(item, stapel=()):
    """Rohstoffe für EIN Stück des Items (Dict Spalte -> Barren)."""
    if item in ROH:
        sp, n = ROH[item]
        return {sp: n}
    if item in VANILLA:
        return dict(VANILLA[item])
    if item in _cache:
        return _cache[item]
    if item in stapel:
        return None   # Kreis
    besten = None
    for name, menge, zutaten in REZEPTE.get(item, []):
        summe = defaultdict(float)
        ok = True
        for z, n in zutaten.items():
            k = kosten(z, stapel + (item,))
            if k is None:
                ok = False
                break
            for sp, v in k.items():
                summe[sp] += v * n / menge
        if ok and (besten is None or gewicht(summe) < gewicht(besten)):
            besten = dict(summe)
    if besten is None:
        if not REZEPTE.get(item):
            if not item.startswith(OHNE_KOSTEN_PREFIX) and item not in UNIVERSUM:
                FEHLT.add(item)
            besten = {}
        else:
            return None
    if not stapel:
        _cache[item] = besten
    else:
        _cache.setdefault(item, besten)
    return besten


def billigstes(item):
    besten, wahl = None, None
    for name, menge, zutaten in REZEPTE.get(item, []):
        summe = defaultdict(float)
        for z, n in zutaten.items():
            for sp, v in (kosten(z) or {}).items():
                summe[sp] += v * n / menge
        if besten is None or gewicht(summe) < gewicht(besten):
            besten, wahl = summe, (name, menge, zutaten)
    return wahl


# ---------------------------------------------------------------- Modell-Durchlauf
R = 'riftverse:'
MODELL = {
    'T0 Garage': {
        'garagen_werkbank': 1, 'garagenpresse': 1, 'kurbel_dynamo': 2, 'handkurbel': 1, 'garagen_hammer': 2,
        'loetkolben': 1, 'kupferkabel': 16, 'energiezelle': 1,
    },
    'T1 Bastler': {
        'erzmuehle': 2, 'walzbank': 1, 'legierungsofen': 1, 'tonnenreaktor': 2, 'xenit_schmelze': 1, 'platinen_aetzer': 1,
        'kristall_schleifer': 1, 'recycler': 1, 'fluid_mixer': 1, 'xenit_zelle': 1, 'biomasse_kessel': 1, 'einspeiser': 1,
        'ausspeiser': 1, 'meteoritenpeiler': 1, 'kupferkabel': 24, 'kupferkabel_isoliert': 40, 'energiezelle': 2,
        'rift_anker': 1,
    },
    'T2 Labor': {
        'praezisions_montierer': 1, 'quantenzentrifuge': 1, 'gravitationspresse': 1, 'dimensions_scanner': 1,
        'chrono_beschleuniger': 2, 'portalfluid_raffinerie': 1, 'labor_huelle': 25 + 29, 'umspanner': 2, 'flux_zelle': 1,
        'zeitschleifen_solar': 2, 'fluxkristall_resonator': 1, 'resonator_kammer': 2, 'fluxkristall_stab': 6,
        'kuehlkoerper': 4, 'gravitations_pendel': 1, 'schwebstein_kern': 1, 'flux_leitung_isoliert': 60,
        'universumsprobe': 2,
    },
    'T3 Industrie': {
        'quanten_lithograf': 1, 'materie_rekonfigurator': 1, 'klon_tank': 1, 'universums_destillator': 1,
        'rift_kabelknoten': 2, 'mikrokosmos_zelle': 1, 'parallelzapfer': 1, 'graphit_huelle': 12, 'chrono_speicher': 1,
        'umspanner_ii': 1, 'umspanner_iii': 1, 'gravitit_leitung_doppelt': 50,
    },
    'T4 Weltenformer': {
        'rift_schmiede': 1, 'weltenformer': 1, 'null_huelle': 51, 'singularitaetskern': 1, 'eindaemmungsring': 42,
        'null_leitung': 30,
    },
    'Kanone Mk I–IV': {'portal_kanone_mk4': 1, 'fluid_kartusche': 4},
    'Rüstung (4 Teile)': {
        'analyse_visier_perfektioniert': 1, 'kernmantel_perfektioniert': 1, 'exo_rahmen_perfektioniert': 1,
        'gravi_stiefel_perfektioniert': 1,
    },
    'Gadgets (8)': {
        'einzweck_drohne': 1, 'holo_koeder': 1, 'schrumpfstrahler': 1, 'uebersetzer_halsband': 1, 'tarn_detektor': 1,
        'multiversal_multitool': 1, 'zeitstopper': 1, 'aufgaben_box': 1,
    },
}
DOKUMENT = {
    'T0 Garage': [0, 30, 43, 11, 0], 'T1 Bastler': [54, 108, 111, 16, 0], 'T2 Labor': [140, 240, 112, 131, 99],
    'T3 Industrie': [104, 280, 107, 123, 61], 'T4 Weltenformer': [111, 386, 37, 35, 21],
    'Kanone Mk I–IV': [20, 12, 26, 19, 13], 'Rüstung (4 Teile)': [40, 24, 59, 44, 49], 'Gadgets (8)': [24, 12, 29, 18, 12],
}
DOKUMENT_SUMME = [490, 1090, 520, 400, 255]
DOKUMENT_SONST = {'schrott': 300, 'redstone': 330, 'flux': 140, 'gravitit': 300, 'null': 50}


def bereich(items):
    summe = defaultdict(float)
    for name, n in items.items():
        for sp, v in (kosten(R + name) or {}).items():
            summe[sp] += v * n
    return summe


# ---------------------------------------------------------------- Weltgenerierung
def lade_json(*teile):
    with open(os.path.join(DATA, 'riftverse', *teile), encoding='utf-8') as fh:
        return json.load(fh)


def adern(placed):
    for p in placed['placement']:
        if p['type'] == 'minecraft:count':
            c = p['count']
            return (c['min_inclusive'] + c['max_inclusive']) / 2 if isinstance(c, dict) else c
    return 0


def erz_pro_ader(size):
    # Vanilla-Erzadern setzen im Mittel gut die Hälfte von "size" in Stein (Messwerte für size 4–9: ~0,55 · size)
    return 0.55 * size


def weltgen():
    zeilen = []
    # Eisen-Band aus Vanilla (ore_iron_middle: 10 Adern, size 9, Y −24..56) als Bezug
    eisen_adern, eisen_size = 10, 9
    eisen_pro_stunde = 125   # Strip-Mining auf Y 16, eine Stunde, Erfahrungswert
    xen = lade_json('worldgen', 'placed_feature', 'ore_xenit_krater.json')
    xen_size = lade_json('worldgen', 'configured_feature', 'ore_xenit_krater.json')['config']['size']
    anteil = adern(xen) * erz_pro_ader(xen_size) / (eisen_adern * erz_pro_ader(eisen_size))
    for d in (0, 50, 100, 150):
        w = max(0.0, 1 - d / 200)
        zeilen.append(f'  Xenit bei {d:3d} Blöcken vom Krater: ~{eisen_pro_stunde * anteil * w:4.1f} Erz pro Stunde '
                      f'(Ziel am Krater: 20–40)')
    # Zone gesamt: Kreisfläche · mittlere Dichte (1/3 bei linearem Abfall)
    chunks = math.pi * 200 ** 2 / 256
    zone = chunks * (1 / 3) * adern(xen) * erz_pro_ader(xen_size)
    zeilen.append(f'  Xenit in einer ganzen Zone: ~{zone:.0f} Erz, dazu ~40 am Krater selbst (Bedarf im Durchlauf ~260 Erz)')
    zinn = 0
    for name in ('ore_tin', 'ore_tin_small'):
        try:
            p = lade_json('worldgen', 'placed_feature', name + '.json')
            c = lade_json('worldgen', 'configured_feature', name + '.json')
            zinn += adern(p) * erz_pro_ader(c['config']['size'])
        except FileNotFoundError:
            pass
    eisen = eisen_adern * erz_pro_ader(eisen_size) + 10 * erz_pro_ader(4)
    zeilen.append(f'  Zinn pro Chunk: ~{zinn:.0f} Erz (Eisen in Vanilla: ~{eisen:.0f}); Bedarf ~400 Barren = ~200 Erz')
    return zeilen


# ---------------------------------------------------------------- Ausgabe
def fmt(v):
    return f'{v:6.0f}'


def main():
    if len(sys.argv) > 2 and sys.argv[1] == '--baum':
        item = sys.argv[2] if ':' in sys.argv[2] else R + sys.argv[2]
        wahl = billigstes(item)
        print(item, '←', wahl[0] if wahl else 'kein Rezept')
        if wahl:
            for z, n in wahl[2].items():
                k = kosten(z) or {}
                print(f'  {n:g} × {z}:', {s: round(v * n / wahl[1], 2) for s, v in k.items()})
        print('Summe:', {s: round(v, 2) for s, v in (kosten(item) or {}).items()})
        return 0

    kopf = f'{"Bereich":20s}' + ''.join(f'{s:>14s}' for s in SPALTEN[:5])
    print(kopf)
    print('-' * len(kopf))
    gesamt = defaultdict(float)
    abweichung = False
    for name, items in MODELL.items():
        b = bereich(items)
        for k, v in b.items():
            gesamt[k] += v
        doc = DOKUMENT[name]
        zellen = ''
        for i, sp in enumerate(SPALTEN[:5]):
            zellen += f'{fmt(b.get(sp, 0))} ({doc[i]:4d})'
        print(f'{name:20s}{zellen}')
    print('-' * len(kopf))
    zellen = ''
    for i, sp in enumerate(SPALTEN[:5]):
        ist, soll = gesamt.get(sp, 0), DOKUMENT_SUMME[i]
        mark = ' ' if abs(ist - soll) <= 0.25 * soll else '!'
        abweichung |= mark == '!'
        zellen += f'{fmt(ist)} ({soll:4d}){mark}'[:14].rjust(14) if False else f'{fmt(ist)} ({soll:4d})'
    print(f'{"Summe":20s}{zellen}')
    for i, sp in enumerate(SPALTEN[:5]):
        ist, soll = gesamt.get(sp, 0), DOKUMENT_SUMME[i]
        if abs(ist - soll) > 0.25 * soll:
            print(f'  ! {sp}: {ist:.0f} statt ~{soll} ({(ist / soll - 1) * 100:+.0f} %)')
    print()
    print('Weitere Rohstoffe (Modell / Dokument):')
    for sp, soll in DOKUMENT_SONST.items():
        print(f'  {sp:10s} {gesamt.get(sp, 0):6.0f} / ~{soll}')
    print()
    print('Weltgenerierung (Schätzung):')
    for z in weltgen():
        print(z)
    if FEHLT:
        print()
        print('Ohne Rezept und ohne Rohstoff-Zuordnung (zählen 0):')
        for f in sorted(FEHLT):
            print('  ', f)
    return 1 if abweichung else 0


if __name__ == '__main__':
    sys.exit(main())
