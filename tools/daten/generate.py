"""Erzeugt Blockstates, Modelle, Loot-Tabellen, Tags, Rezepte, Worldgen, Dimensionen und Sprachdateien.

Meilenstein 1 (Fundament) und Meilenstein 2 (Spielbarer Prototyp) stehen in getrennten Abschnitten.

Aufruf aus dem Repo-Wurzelordner:
    python3 tools/daten/generate.py

Das Skript ist idempotent: Tags und Sprachdateien werden zusammengeführt, alles andere überschrieben.
Neue Inhalte zuerst hier eintragen, dann das Skript laufen lassen und das Ergebnis committen.
"""
import json
import os

ROOT = os.path.join(os.path.dirname(__file__), '..', '..', 'src', 'main', 'resources')
NS = 'riftverse'
ASSETS = os.path.join(ROOT, 'assets', NS)
DATA = os.path.join(ROOT, 'data')


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as fh:
        json.dump(obj, fh, indent=2, ensure_ascii=False)
        fh.write('\n')


def rl(name):
    return name if ':' in name else f'{NS}:{name}'


def tex(name):
    return f'{NS}:block/{name}'


# =============================================================================
# Modelle und Blockstates
# =============================================================================
def block_model(name, parent, textures, **extra):
    write(os.path.join(ASSETS, 'models', 'block', name + '.json'), {'parent': parent, 'textures': textures, **extra})


def item_from_block(name, model=None):
    write(os.path.join(ASSETS, 'models', 'item', name + '.json'), {'parent': f'{NS}:block/{model or name}'})


def item_generated(name, handheld=False, texture=None):
    write(os.path.join(ASSETS, 'models', 'item', name + '.json'), {
        'parent': 'minecraft:item/handheld' if handheld else 'minecraft:item/generated',
        'textures': {'layer0': f'{NS}:item/{texture or name}'}})


def blockstate(name, variants):
    write(os.path.join(ASSETS, 'blockstates', name + '.json'), {'variants': variants})


HORIZONTAL = {'north': 0, 'east': 90, 'south': 180, 'west': 270}
ALL_DIRS = {'north': (0, 0), 'east': (0, 90), 'south': (0, 180), 'west': (0, 270), 'up': (270, 0), 'down': (90, 0)}


def variant(model, x=0, y=0):
    v = {'model': f'{NS}:block/{model}'}
    if x:
        v['x'] = x
    if y:
        v['y'] = y
    return v


def cube_all(name, texture=None):
    block_model(name, 'minecraft:block/cube_all', {'all': tex(texture or name)})
    blockstate(name, {'': variant(name)})
    item_from_block(name)


def casing_block(name, tier):
    block_model(name, 'minecraft:block/cube_bottom_top', {
        'top': tex(f'casing_t{tier}_top'), 'side': tex(f'casing_t{tier}'), 'bottom': tex(f'casing_t{tier}')})
    blockstate(name, {'': variant(name)})
    item_from_block(name)


def machine(name, tier):
    """Front zeigt nach facing, active schaltet auf die _on-Front."""
    for suffix in ('', '_on'):
        block_model(name + suffix, 'minecraft:block/orientable', {
            'top': tex(f'casing_t{tier}_top'), 'side': tex(f'casing_t{tier}'), 'front': tex(f'{name}_front{suffix}')})
    variants = {}
    for facing, y in HORIZONTAL.items():
        for active in ('false', 'true'):
            variants[f'active={active},facing={facing}'] = variant(name + ('_on' if active == 'true' else ''), y=y)
    blockstate(name, variants)
    item_from_block(name)


def tonnenreaktor():
    for suffix, top in (('', 'tonnenreaktor_top'), ('_on', 'tonnenreaktor_top_on')):
        block_model('tonnenreaktor' + suffix, 'minecraft:block/cube_column', {'end': tex(top), 'side': tex('tonnenreaktor_side')})
    variants = {}
    for facing in HORIZONTAL:
        for active in ('false', 'true'):
            variants[f'active={active},facing={facing}'] = variant('tonnenreaktor' + ('_on' if active == 'true' else ''))
    blockstate('tonnenreaktor', variants)
    item_from_block('tonnenreaktor')


def werkbank():
    block_model('garagen_werkbank', 'minecraft:block/cube', {
        'particle': tex('garagen_werkbank_front'), 'up': tex('garagen_werkbank_top'), 'down': tex('casing_t0'),
        'north': tex('garagen_werkbank_front'), 'south': tex('garagen_werkbank_front'),
        'east': tex('garagen_werkbank_side'), 'west': tex('garagen_werkbank_side')})
    blockstate('garagen_werkbank', {f'facing={f}': variant('garagen_werkbank', y=y) for f, y in HORIZONTAL.items()})
    item_from_block('garagen_werkbank')


def energy_port(name):
    """Front (Stecker) Richtung facing, alle anderen Seiten Xenit-Gehäuse."""
    for suffix in ('', '_on'):
        side = tex('casing_t1')
        block_model(name + suffix, 'minecraft:block/cube', {
            'particle': side, 'north': tex(f'{name}_front{suffix}'), 'south': side, 'east': side, 'west': side,
            'up': tex('casing_t1_top'), 'down': tex('casing_t1_top')})
    variants = {}
    for facing, (x, y) in ALL_DIRS.items():
        for active in ('false', 'true'):
            variants[f'active={active},facing={facing}'] = variant(name + ('_on' if active == 'true' else ''), x=x, y=y)
    blockstate(name, variants)
    item_from_block(name)


def cable(name, thickness, tinted):
    """IC2-artige Leitung: Kern + Arme (multipart)."""
    lo, hi = 8 - thickness / 2, 8 + thickness / 2

    def face(uv, cull=None):
        f = {'texture': '#cable', 'uv': uv}
        if cull:
            f['cullface'] = cull
        if tinted:
            f['tintindex'] = 0
        return f

    end_uv = [lo, lo, hi, hi]
    core = {'from': [lo, lo, lo], 'to': [hi, hi, hi],
            'faces': {d: face(end_uv) for d in ('north', 'south', 'east', 'west', 'up', 'down')}}
    side_uv = [lo, 0, hi, lo]
    arm = {'from': [lo, lo, 0], 'to': [hi, hi, lo],
           'faces': {'north': face(end_uv, 'north'), 'east': face(side_uv), 'west': face(side_uv),
                     'up': face(side_uv), 'down': face(side_uv)}}
    textures = {'cable': tex(name), 'particle': tex(name)}
    write(os.path.join(ASSETS, 'models', 'block', name + '_core.json'), {'textures': textures, 'elements': [core]})
    write(os.path.join(ASSETS, 'models', 'block', name + '_side.json'), {'textures': textures, 'elements': [arm]})
    parts = [{'apply': {'model': f'{NS}:block/{name}_core'}}]
    for d, (x, y) in ALL_DIRS.items():
        apply = {'model': f'{NS}:block/{name}_side'}
        if x:
            apply['x'] = x
        if y:
            apply['y'] = y
        parts.append({'when': {d: 'true'}, 'apply': apply})
    write(os.path.join(ASSETS, 'blockstates', name + '.json'), {'multipart': parts})
    item_generated(name)


def models():
    for n in ('tin_ore', 'deepslate_tin_ore', 'tin_block', 'xenit_ore', 'deepslate_xenit_ore', 'xenit_block', 'kreativ_quelle'):
        cube_all(n)
    casing_block('garagen_gehaeuse', 0)
    casing_block('xenit_gehaeuse', 1)
    werkbank()
    for name, tier in (('garagenpresse', 0), ('kurbel_dynamo', 0), ('erzmuehle', 1), ('walzbank', 1), ('legierungsofen', 1)):
        machine(name, tier)
    tonnenreaktor()
    energy_port('einspeiser')
    energy_port('ausspeiser')
    cable('kupferkabel', 4, False)
    cable('kupferkabel_isoliert', 6, True)

    for n in ITEMS:
        item_generated(n, handheld=n in HANDHELD)


ITEMS = ['raw_tin', 'tin_ingot', 'raw_xenit', 'xenit_ingot', 'bronze_ingot', 'steel_ingot', 'mischmetall',
         'eisen_staub', 'kupfer_staub', 'gold_staub', 'zinn_staub', 'xenit_staub', 'bronze_staub', 'kohle_staub',
         'eisen_platte', 'kupfer_platte', 'zinn_platte', 'gold_platte', 'bronze_platte', 'stahl_platte', 'xenit_platte',
         'kupferdraht', 'golddraht', 'loetzinn', 'zinn_huelse', 'eisen_stange', 'xenit_stange',
         'bronze_zahnrad', 'xenit_spule', 'alien_schrott', 'schrottplatte', 'polymerfetzen', 'isolierfolie', 'feder',
         'mini_motor', 'garagen_hammer', 'loetkolben', 'handkurbel', 'energiezelle']
HANDHELD = {'garagen_hammer', 'loetkolben', 'handkurbel'}


# =============================================================================
# Loot-Tabellen
# =============================================================================
def loot_self(name):
    write(os.path.join(DATA, NS, 'loot_table', 'blocks', name + '.json'), {
        'type': 'minecraft:block',
        'pools': [{'rolls': 1, 'bonus_rolls': 0,
                   'entries': [{'type': 'minecraft:item', 'name': rl(name)}],
                   'conditions': [{'condition': 'minecraft:survives_explosion'}]}],
        'random_sequence': f'{NS}:blocks/{name}'})


def loot_ore(name, raw):
    write(os.path.join(DATA, NS, 'loot_table', 'blocks', name + '.json'), {
        'type': 'minecraft:block',
        'pools': [{'rolls': 1, 'bonus_rolls': 0, 'entries': [{
            'type': 'minecraft:alternatives',
            'children': [
                {'type': 'minecraft:item', 'name': rl(name), 'conditions': [{
                    'condition': 'minecraft:match_tool',
                    'predicate': {'predicates': {'minecraft:enchantments': [
                        {'enchantments': 'minecraft:silk_touch', 'levels': {'min': 1}}]}}}]},
                {'type': 'minecraft:item', 'name': rl(raw), 'functions': [
                    {'function': 'minecraft:apply_bonus', 'enchantment': 'minecraft:fortune', 'formula': 'minecraft:ore_drops'},
                    {'function': 'minecraft:explosion_decay'}]}]}]}],
        'random_sequence': f'{NS}:blocks/{name}'})


def loot():
    for n in ('tin_ore', 'deepslate_tin_ore'):
        loot_ore(n, 'raw_tin')
    for n in ('xenit_ore', 'deepslate_xenit_ore'):
        loot_ore(n, 'raw_xenit')
    for n in ('tin_block', 'xenit_block', 'garagen_gehaeuse', 'xenit_gehaeuse', 'garagen_werkbank', 'garagenpresse',
              'kurbel_dynamo', 'erzmuehle', 'walzbank', 'legierungsofen', 'tonnenreaktor', 'einspeiser', 'ausspeiser',
              'kupferkabel', 'kupferkabel_isoliert'):
        loot_self(n)


# =============================================================================
# Tags
# =============================================================================
def tag(kind, tag_id, values):
    ns, path = tag_id.split(':')
    p = os.path.join(DATA, ns, 'tags', kind, path + '.json')
    existing = []
    if os.path.exists(p):
        with open(p, encoding='utf-8') as fh:
            existing = json.load(fh).get('values', [])
    merged = list(existing)
    for v in values:
        v = v if v.startswith('#') or ':' in v else rl(v)
        if v not in merged:
            merged.append(v)
    write(p, {'values': merged})


METALS_DUST = {'iron': 'eisen_staub', 'copper': 'kupfer_staub', 'gold': 'gold_staub', 'tin': 'zinn_staub',
               'xenit': 'xenit_staub', 'bronze': 'bronze_staub', 'coal': 'kohle_staub'}
METALS_PLATE = {'iron': 'eisen_platte', 'copper': 'kupfer_platte', 'tin': 'zinn_platte', 'gold': 'gold_platte',
                'bronze': 'bronze_platte', 'steel': 'stahl_platte', 'xenit': 'xenit_platte'}
INGOT = {'iron': 'minecraft:iron_ingot', 'copper': 'minecraft:copper_ingot', 'gold': 'minecraft:gold_ingot',
         'tin': 'tin_ingot', 'bronze': 'bronze_ingot', 'steel': 'steel_ingot', 'xenit': 'xenit_ingot'}


def tags():
    pickaxe = ['tin_ore', 'deepslate_tin_ore', 'tin_block', 'xenit_ore', 'deepslate_xenit_ore', 'xenit_block',
               'xenit_gehaeuse', 'erzmuehle', 'walzbank', 'legierungsofen', 'tonnenreaktor', 'einspeiser', 'ausspeiser',
               'kupferkabel', 'kupferkabel_isoliert', 'garagenpresse', 'kurbel_dynamo']
    tag('block', 'minecraft:mineable/pickaxe', pickaxe)
    tag('block', 'minecraft:mineable/axe', ['garagen_werkbank', 'garagen_gehaeuse', 'garagenpresse', 'kurbel_dynamo'])
    tag('block', 'minecraft:needs_stone_tool', ['tin_ore', 'deepslate_tin_ore', 'tin_block', 'xenit_gehaeuse', 'erzmuehle',
                                                'walzbank', 'legierungsofen', 'tonnenreaktor', 'einspeiser', 'ausspeiser'])
    tag('block', 'minecraft:needs_iron_tool', ['xenit_ore', 'deepslate_xenit_ore', 'xenit_block'])

    for kind in ('block', 'item'):
        tag(kind, 'c:ores/tin', ['tin_ore', 'deepslate_tin_ore'])
        tag(kind, 'c:ores/xenit', ['xenit_ore', 'deepslate_xenit_ore'])
        tag(kind, 'c:ores', ['#c:ores/tin', '#c:ores/xenit'])
        tag(kind, 'c:ores_in_ground/stone', ['tin_ore', 'xenit_ore'])
        tag(kind, 'c:ores_in_ground/deepslate', ['deepslate_tin_ore', 'deepslate_xenit_ore'])
        tag(kind, 'c:storage_blocks/tin', ['tin_block'])
        tag(kind, 'c:storage_blocks/xenit', ['xenit_block'])
        tag(kind, 'c:storage_blocks', ['#c:storage_blocks/tin', '#c:storage_blocks/xenit'])

    tag('item', 'c:raw_materials/tin', ['raw_tin'])
    tag('item', 'c:raw_materials/xenit', ['raw_xenit'])
    tag('item', 'c:raw_materials', ['#c:raw_materials/tin', '#c:raw_materials/xenit'])
    for m in ('tin', 'bronze', 'steel', 'xenit'):
        tag('item', f'c:ingots/{m}', [INGOT[m]])
    tag('item', 'c:ingots', ['#c:ingots/tin', '#c:ingots/bronze', '#c:ingots/steel', '#c:ingots/xenit'])
    for m, item in METALS_DUST.items():
        tag('item', f'c:dusts/{m}', [item])
    tag('item', 'c:dusts', [f'#c:dusts/{m}' for m in METALS_DUST])
    for m, item in METALS_PLATE.items():
        tag('item', f'c:plates/{m}', [item])
    tag('item', 'c:plates', [f'#c:plates/{m}' for m in METALS_PLATE])
    tag('item', 'c:wires/copper', ['kupferdraht'])
    tag('item', 'c:wires/gold', ['golddraht'])
    tag('item', 'c:wires', ['#c:wires/copper', '#c:wires/gold'])
    tag('item', 'c:rods/iron', ['eisen_stange'])
    tag('item', 'c:rods/xenit', ['xenit_stange'])
    tag('item', 'c:rods', ['#c:rods/iron', '#c:rods/xenit'])
    tag('item', 'c:gears/bronze', ['bronze_zahnrad'])
    tag('item', 'c:gears', ['#c:gears/bronze'])
    tag('item', f'{NS}:schrott', ['alien_schrott', 'schrottplatte'])

    tag('damage_type', 'minecraft:bypasses_armor', [f'{NS}:stromschlag'])
    tag('damage_type', 'minecraft:no_knockback', [f'{NS}:stromschlag'])


# =============================================================================
# Rezepte
# =============================================================================
def ing(x):
    if isinstance(x, list):
        return [ing(i) for i in x]
    if x.startswith('#'):
        return {'tag': x[1:]}
    return {'item': rl(x)}


def result(item, count=1):
    r = {'id': rl(item)}
    if count != 1:
        r['count'] = count
    return r


def recipe(path, obj):
    write(os.path.join(DATA, NS, 'recipe', path + '.json'), obj)


def shaped_vanilla(path, pattern, key, out, count=1):
    recipe(path, {'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': pattern,
                  'key': {k: ing(v) for k, v in key.items()}, 'result': result(out, count)})


def shapeless_vanilla(path, ingredients, out, count=1):
    recipe(path, {'type': 'minecraft:crafting_shapeless', 'category': 'misc',
                  'ingredients': [ing(i) for i in ingredients], 'result': result(out, count)})


def smelt(name, ingredient, out, xp=0.7):
    recipe(f'smelting/{name}', {'type': 'minecraft:smelting', 'category': 'misc', 'cookingtime': 200,
                                'experience': xp, 'ingredient': ing(ingredient), 'result': result(out)})
    recipe(f'blasting/{name}', {'type': 'minecraft:blasting', 'category': 'misc', 'cookingtime': 100,
                                'experience': xp, 'ingredient': ing(ingredient), 'result': result(out)})


def werkbank_shaped(path, pattern, key, out, count=1, hammer=0, loetzinn=0):
    r = {'type': f'{NS}:werkbank_shaped', 'pattern': pattern, 'key': {k: ing(v) for k, v in key.items()},
         'result': result(out, count)}
    if hammer:
        r['hammer'] = hammer
    if loetzinn:
        r['loetzinn'] = loetzinn
    recipe(f'garagen_werkbank/{path}', r)


def werkbank_shapeless(path, ingredients, out, count=1, hammer=0, loetzinn=0):
    r = {'type': f'{NS}:werkbank_shapeless', 'ingredients': [ing(i) for i in ingredients], 'result': result(out, count)}
    if hammer:
        r['hammer'] = hammer
    if loetzinn:
        r['loetzinn'] = loetzinn
    recipe(f'garagen_werkbank/{path}', r)


def machine_recipe(machine, path, ingredients, out, count=1, time=None, mode=None):
    r = {'type': f'{NS}:{machine}',
         'ingredients': [{'ingredient': ing(i), **({'count': c} if c != 1 else {})} for i, c in ingredients],
         'result': result(out, count)}
    if time:
        r['time'] = time
    if mode:
        r['mode'] = mode
    recipe(f'{machine}/{path}', r)


def recipes():
    # --- Einstieg über die normale Werkbank -----------------------------------
    shaped_vanilla('garagen_werkbank', ['ICI', 'LTL', 'L L'],
                   {'I': '#c:ingots/iron', 'C': '#c:ingots/copper', 'L': '#minecraft:logs', 'T': 'minecraft:crafting_table'},
                   'garagen_werkbank')
    shaped_vanilla('tin_block', ['III', 'III', 'III'], {'I': '#c:ingots/tin'}, 'tin_block')
    shapeless_vanilla('tin_ingot_from_block', ['tin_block'], 'tin_ingot', 9)
    shaped_vanilla('xenit_block', ['III', 'III', 'III'], {'I': '#c:ingots/xenit'}, 'xenit_block')
    shapeless_vanilla('xenit_ingot_from_block', ['xenit_block'], 'xenit_ingot', 9)

    # --- Ofen ------------------------------------------------------------------
    smelt('tin_ingot_from_raw', 'raw_tin', 'tin_ingot')
    smelt('tin_ingot_from_ore', '#c:ores/tin', 'tin_ingot')
    smelt('xenit_ingot_from_raw', 'raw_xenit', 'xenit_ingot', 1.0)
    smelt('xenit_ingot_from_ore', '#c:ores/xenit', 'xenit_ingot', 1.0)
    for dust, ingot in (('eisen_staub', 'minecraft:iron_ingot'), ('kupfer_staub', 'minecraft:copper_ingot'),
                        ('gold_staub', 'minecraft:gold_ingot'), ('zinn_staub', 'tin_ingot'), ('bronze_staub', 'bronze_ingot')):
        smelt(f'{ingot.split(":")[-1]}_from_{dust}', dust, ingot, 0.3)
    # Xenit-Staub schmilzt nur die Xenit-Schmelze (Meilenstein 2)

    # --- Garagen-Werkbank: T0 ----------------------------------------------------
    # Bronze-Mischung von Hand: halbe Ausbeute, der Legierungsofen macht 3 + 1 -> 4
    werkbank_shapeless('bronze_staub_von_hand', ['#c:ingots/copper'] * 3 + ['#c:ingots/tin'], 'bronze_staub', 2)
    werkbank_shapeless('bronze_staub_aus_staub', ['#c:dusts/copper'] * 3 + ['#c:dusts/tin'], 'bronze_staub', 4)
    werkbank_shaped('garagen_hammer', ['BBB', 'BSB', ' S '], {'B': '#c:ingots/bronze', 'S': 'minecraft:stick'}, 'garagen_hammer')
    werkbank_shaped('loetkolben', ['  K', ' RI', 'W  '],
                    {'K': '#c:plates/copper', 'R': '#c:dusts/redstone', 'I': '#c:plates/iron', 'W': '#minecraft:planks'},
                    'loetkolben', hammer=1)
    werkbank_shapeless('loetzinn', ['#c:ingots/tin'], 'loetzinn', 1, hammer=1)
    # Hammer: 2 Barren -> 1 Platte (die Garagenpresse macht 1 -> 1)
    for metal, plate_item in METALS_PLATE.items():
        werkbank_shapeless(f'platte/{metal}', [f'#c:ingots/{metal}'] * 2, plate_item, 1, hammer=1)
    werkbank_shaped('bronze_zahnrad', [' P ', 'P P', ' P '], {'P': '#c:plates/bronze'}, 'bronze_zahnrad', hammer=1)
    werkbank_shaped('handkurbel', ['  S', 'PPP', 'G  '],
                    {'S': 'minecraft:stick', 'P': '#c:plates/bronze', 'G': 'bronze_zahnrad'}, 'handkurbel', hammer=1)
    werkbank_shaped('garagen_gehaeuse', ['IBI', 'W W', 'IBI'],
                    {'I': '#c:plates/iron', 'B': '#c:plates/bronze', 'W': '#minecraft:planks'}, 'garagen_gehaeuse', hammer=1)
    werkbank_shaped('garagenpresse', ['IPI', 'GCG', 'BBB'],
                    {'I': '#c:plates/iron', 'P': 'minecraft:piston', 'G': 'bronze_zahnrad', 'C': 'garagen_gehaeuse',
                     'B': '#c:plates/bronze'}, 'garagenpresse', hammer=2)
    werkbank_shaped('kurbel_dynamo', ['GKG', 'KCK', 'IRI'],
                    {'G': 'bronze_zahnrad', 'K': '#c:plates/copper', 'C': 'garagen_gehaeuse', 'I': '#c:plates/iron',
                     'R': '#c:dusts/redstone'}, 'kurbel_dynamo', hammer=1)

    # --- Garagen-Werkbank: T1 ----------------------------------------------------
    werkbank_shaped('xenit_gehaeuse', ['IZI', 'X X', 'IZI'],
                    {'I': '#c:plates/iron', 'Z': '#c:plates/tin', 'X': '#c:plates/xenit'}, 'xenit_gehaeuse', hammer=2)
    werkbank_shaped('xenit_spule', ['DDD', ' S ', 'DDD'], {'D': '#c:wires/copper', 'S': '#c:rods/xenit'}, 'xenit_spule')
    werkbank_shaped('walzbank', ['GIG', 'ICI', 'KRK'],
                    {'G': 'bronze_zahnrad', 'I': '#c:ingots/iron', 'C': 'xenit_gehaeuse', 'K': '#c:plates/copper',
                     'R': '#c:dusts/redstone'}, 'walzbank', loetzinn=2)
    werkbank_shaped('erzmuehle', ['FHF', 'ICI', 'SRS'],
                    {'F': 'minecraft:flint', 'H': 'minecraft:hopper', 'I': '#c:plates/iron', 'C': 'xenit_gehaeuse',
                     'S': 'xenit_spule', 'R': '#c:dusts/redstone'}, 'erzmuehle', loetzinn=2)
    werkbank_shaped('legierungsofen', ['H H', 'FCF', 'ISI'],
                    {'H': 'minecraft:hopper', 'F': 'minecraft:furnace', 'C': 'xenit_gehaeuse', 'I': '#c:plates/iron',
                     'S': 'xenit_spule'}, 'legierungsofen', loetzinn=2)
    werkbank_shaped('tonnenreaktor', ['XBX', 'ICI', 'KFK'],
                    {'X': '#c:plates/xenit', 'B': 'minecraft:iron_bars', 'I': '#c:plates/iron', 'C': 'minecraft:cauldron',
                     'K': '#c:plates/copper', 'F': 'minecraft:furnace'}, 'tonnenreaktor', hammer=1, loetzinn=1)
    werkbank_shaped('kupferkabel', ['DDD'], {'D': '#c:wires/copper'}, 'kupferkabel', 6)
    werkbank_shapeless('kupferkabel_isoliert', ['kupferkabel', 'isolierfolie'], 'kupferkabel_isoliert')
    werkbank_shaped('einspeiser', [' S ', 'KCK', ' R '],
                    {'S': 'xenit_spule', 'K': 'kupferkabel', 'C': 'xenit_gehaeuse', 'R': '#c:dusts/redstone'},
                    'einspeiser', loetzinn=2)
    werkbank_shaped('ausspeiser', [' S ', 'KCK', ' Q '],
                    {'S': 'xenit_spule', 'K': 'kupferkabel', 'C': 'xenit_gehaeuse', 'Q': 'minecraft:comparator'},
                    'ausspeiser', loetzinn=2)
    werkbank_shaped('energiezelle', [' D ', 'RHR', ' X '],
                    {'D': '#c:wires/copper', 'R': '#c:dusts/redstone', 'H': 'zinn_huelse', 'X': '#c:plates/xenit'},
                    'energiezelle', loetzinn=1)
    werkbank_shaped('mischmetall', ['SSS', 'BBB', 'ZZZ'],
                    {'S': '#c:plates/steel', 'B': '#c:plates/bronze', 'Z': '#c:plates/tin'}, 'mischmetall', 2, hammer=1)

    # --- Erzmühle: 1 Erz -> 2 Staub ------------------------------------------------
    for metal in ('iron', 'copper', 'gold', 'tin', 'xenit'):
        dust = METALS_DUST[metal]
        machine_recipe('erzmuehle', f'{metal}_ore', [(f'#c:ores/{metal}', 1)], dust, 2, time=150)
        machine_recipe('erzmuehle', f'raw_{metal}', [(f'#c:raw_materials/{metal}', 1)], dust, 2, time=150)
        machine_recipe('erzmuehle', f'{metal}_ingot', [(f'#c:ingots/{metal}', 1)], dust, 1, time=100)
    machine_recipe('erzmuehle', 'bronze_ingot', [('#c:ingots/bronze', 1)], 'bronze_staub', 1, time=100)
    machine_recipe('erzmuehle', 'coal', [('minecraft:coal', 1)], 'kohle_staub', 1, time=100)
    machine_recipe('erzmuehle', 'charcoal', [('minecraft:charcoal', 1)], 'kohle_staub', 1, time=100)

    # --- Walzbank -------------------------------------------------------------------
    machine_recipe('walzbank', 'kupferdraht', [('#c:plates/copper', 1)], 'kupferdraht', 3, time=100, mode='walzen')
    machine_recipe('walzbank', 'golddraht', [('#c:plates/gold', 1)], 'golddraht', 3, time=100, mode='walzen')
    machine_recipe('walzbank', 'loetzinn', [('#c:plates/tin', 1)], 'loetzinn', 3, time=100, mode='walzen')
    machine_recipe('walzbank', 'zinn_huelse', [('#c:plates/tin', 1)], 'zinn_huelse', 2, time=100, mode='schneiden')
    machine_recipe('walzbank', 'eisen_stange', [('#c:ingots/iron', 1)], 'eisen_stange', 2, time=100, mode='ziehen')
    machine_recipe('walzbank', 'xenit_stange', [('#c:ingots/xenit', 1)], 'xenit_stange', 2, time=100, mode='ziehen')

    # --- Legierungsofen ------------------------------------------------------------
    machine_recipe('legierungsofen', 'bronze', [('#c:ingots/copper', 3), ('#c:ingots/tin', 1)], 'bronze_ingot', 4, time=200)
    machine_recipe('legierungsofen', 'bronze_aus_staub', [('#c:dusts/copper', 3), ('#c:dusts/tin', 1)], 'bronze_ingot', 4, time=200)
    machine_recipe('legierungsofen', 'stahl', [('#c:ingots/iron', 1), ('#c:dusts/coal', 2)], 'steel_ingot', 1, time=200)
    machine_recipe('legierungsofen', 'stahl_aus_staub', [('#c:dusts/iron', 1), ('#c:dusts/coal', 2)], 'steel_ingot', 1, time=200)

    # --- Garagenpresse (Kurbel-Ticks) ------------------------------------------------
    for metal, plate_item in METALS_PLATE.items():
        machine_recipe('garagenpresse', f'platte/{metal}', [(f'#c:ingots/{metal}', 1)], plate_item, 1, time=40)
    machine_recipe('garagenpresse', 'schrottplatte', [('alien_schrott', 1)], 'schrottplatte', 1, time=40)
    machine_recipe('garagenpresse', 'isolierfolie', [('polymerfetzen', 2)], 'isolierfolie', 1, time=40)


# =============================================================================
# Worldgen und Schadenstyp
# =============================================================================
def worldgen():
    write(os.path.join(DATA, NS, 'worldgen', 'configured_feature', 'ore_tin.json'), {
        'type': 'minecraft:ore',
        'config': {'discard_chance_on_air_exposure': 0.0, 'size': 9, 'targets': [
            {'state': {'Name': rl('tin_ore')},
             'target': {'predicate_type': 'minecraft:tag_match', 'tag': 'minecraft:stone_ore_replaceables'}},
            {'state': {'Name': rl('deepslate_tin_ore')},
             'target': {'predicate_type': 'minecraft:tag_match', 'tag': 'minecraft:deepslate_ore_replaceables'}}]}})
    # So häufig wie in FTB-Packs (Vorlage: Zinn aus Mekanism)
    write(os.path.join(DATA, NS, 'worldgen', 'placed_feature', 'ore_tin.json'), {
        'feature': rl('ore_tin'),
        'placement': [
            {'type': 'minecraft:count', 'count': 14},
            {'type': 'minecraft:in_square'},
            {'type': 'minecraft:height_range', 'height': {'type': 'minecraft:trapezoid',
                                                          'min_inclusive': {'absolute': -24},
                                                          'max_inclusive': {'absolute': 112}}},
            {'type': 'minecraft:biome'}]})
    write(os.path.join(DATA, NS, 'neoforge', 'biome_modifier', 'add_tin_ore.json'), {
        'type': f'{NS}:add_features_if', 'biomes': '#minecraft:is_overworld', 'features': rl('ore_tin'),
        'step': 'underground_ores', 'config': 'tin_ore'})
    write(os.path.join(DATA, NS, 'damage_type', 'stromschlag.json'), {
        'exhaustion': 0.1, 'message_id': f'{NS}.stromschlag', 'scaling': 'never'})


# =============================================================================
# Sprache
# =============================================================================
DE = {
    # Blöcke
    'block.riftverse.tin_ore': 'Zinnerz', 'block.riftverse.deepslate_tin_ore': 'Tiefenschiefer-Zinnerz',
    'block.riftverse.tin_block': 'Zinnblock', 'block.riftverse.xenit_ore': 'Xenit-Erz',
    'block.riftverse.deepslate_xenit_ore': 'Tiefenschiefer-Xenit-Erz', 'block.riftverse.xenit_block': 'Xenit-Block',
    'block.riftverse.garagen_gehaeuse': 'Garagen-Gehäuse', 'block.riftverse.xenit_gehaeuse': 'Xenit-Gehäuse',
    'block.riftverse.garagen_werkbank': 'Garagen-Werkbank', 'block.riftverse.garagenpresse': 'Garagenpresse',
    'block.riftverse.kurbel_dynamo': 'Kurbel-Dynamo', 'block.riftverse.tonnenreaktor': 'Tonnenreaktor',
    'block.riftverse.erzmuehle': 'Erzmühle', 'block.riftverse.walzbank': 'Walzbank',
    'block.riftverse.legierungsofen': 'Legierungsofen', 'block.riftverse.kupferkabel': 'Kupferkabel',
    'block.riftverse.kupferkabel_isoliert': 'Isoliertes Kupferkabel', 'block.riftverse.einspeiser': 'Einspeiser',
    'block.riftverse.ausspeiser': 'Ausspeiser', 'block.riftverse.kreativ_quelle': 'Kreativ-Quelle',
    # Items
    'item.riftverse.raw_tin': 'Rohes Zinn', 'item.riftverse.tin_ingot': 'Zinnbarren',
    'item.riftverse.raw_xenit': 'Rohes Xenit', 'item.riftverse.xenit_ingot': 'Xenit-Barren',
    'item.riftverse.bronze_ingot': 'Bronzebarren', 'item.riftverse.steel_ingot': 'Stahlbarren',
    'item.riftverse.mischmetall': 'Mischmetall',
    'item.riftverse.eisen_staub': 'Eisenstaub', 'item.riftverse.kupfer_staub': 'Kupferstaub',
    'item.riftverse.gold_staub': 'Goldstaub', 'item.riftverse.zinn_staub': 'Zinnstaub',
    'item.riftverse.xenit_staub': 'Xenit-Staub', 'item.riftverse.bronze_staub': 'Bronzestaub',
    'item.riftverse.kohle_staub': 'Kohlestaub',
    'item.riftverse.eisen_platte': 'Eisenplatte', 'item.riftverse.kupfer_platte': 'Kupferplatte',
    'item.riftverse.zinn_platte': 'Zinnplatte', 'item.riftverse.gold_platte': 'Goldplatte',
    'item.riftverse.bronze_platte': 'Bronzeplatte', 'item.riftverse.stahl_platte': 'Stahlplatte',
    'item.riftverse.xenit_platte': 'Xenit-Platte',
    'item.riftverse.kupferdraht': 'Kupferdraht', 'item.riftverse.golddraht': 'Golddraht',
    'item.riftverse.loetzinn': 'Lötzinn', 'item.riftverse.zinn_huelse': 'Zinnhülse',
    'item.riftverse.eisen_stange': 'Eisenstange', 'item.riftverse.xenit_stange': 'Xenit-Stange',
    'item.riftverse.bronze_zahnrad': 'Bronze-Zahnrad', 'item.riftverse.xenit_spule': 'Xenit-Spule',
    'item.riftverse.alien_schrott': 'Alien-Schrott', 'item.riftverse.schrottplatte': 'Schrottplatte',
    'item.riftverse.polymerfetzen': 'Polymerfetzen', 'item.riftverse.isolierfolie': 'Isolierfolie',
    'item.riftverse.feder': 'Feder', 'item.riftverse.mini_motor': 'Mini-Motor',
    'item.riftverse.garagen_hammer': 'Garagen-Hammer', 'item.riftverse.loetkolben': 'Lötkolben',
    'item.riftverse.handkurbel': 'Handkurbel', 'item.riftverse.energiezelle': 'Energiezelle',
    # GUI und Meldungen
    'itemGroup.riftverse': 'Riftverse',
    'gui.riftverse.mode.walzen': 'Walzen', 'gui.riftverse.mode.schneiden': 'Schneiden', 'gui.riftverse.mode.ziehen': 'Ziehen',
    'gui.riftverse.momentum': 'Schwung', 'gui.riftverse.parts': 'Kleinteile',
    'tier.riftverse.voltage.i': 'Stufe I', 'tier.riftverse.voltage.ii': 'Stufe II',
    'tier.riftverse.voltage.iii': 'Stufe III', 'tier.riftverse.voltage.iv': 'Stufe IV',
    'tooltip.riftverse.voltage': '%s (max. %s FE/t pro Paket)',
    'tooltip.riftverse.voltage_unlimited': '%s (unbegrenzt)',
    'tooltip.riftverse.energy': '%s / %s FE',
    'tooltip.riftverse.energiezelle': 'Akku-Slot: Maschinen ziehen Strom, Generatoren laden',
    'tooltip.riftverse.consumption': 'Verbrauch: %s FE/t',
    'tooltip.riftverse.output': 'Leistung: %s FE/t',
    'tooltip.riftverse.progress': 'Fortschritt: %s %%',
    'tooltip.riftverse.crank_hint': 'Mit der Handkurbel kurbeln: Rechtsklick gedrückt halten',
    'tooltip.riftverse.reactor_fuel': 'Verbrennt jedes Item, Schrott zählt doppelt',
    'tooltip.riftverse.werkbank_tool': 'Werkzeugslot',
    'message.riftverse.net.empty': 'Kein Riftverse-Netz an dieser Leitung',
    'message.riftverse.net.info': 'Netz: %s Quellen, %s Verbraucher, %s Leitungen · %s FE/t · %s',
    'message.riftverse.einspeiser': 'Einspeiser: %s / %s FE · speist mit %s ein',
    'message.riftverse.ausspeiser': 'Ausspeiser: %s / %s FE · nimmt %s an',
    'message.riftverse.kreativ_quelle': 'Kreativ-Quelle: %s',
    'death.attack.riftverse.stromschlag': '%1$s hat an einer blanken Leitung einen Stromschlag bekommen',
    'death.attack.riftverse.stromschlag.player': '%1$s hat auf der Flucht vor %2$s einen Stromschlag bekommen',
}

EN = {
    'block.riftverse.tin_ore': 'Tin Ore', 'block.riftverse.deepslate_tin_ore': 'Deepslate Tin Ore',
    'block.riftverse.tin_block': 'Block of Tin', 'block.riftverse.xenit_ore': 'Xenite Ore',
    'block.riftverse.deepslate_xenit_ore': 'Deepslate Xenite Ore', 'block.riftverse.xenit_block': 'Block of Xenite',
    'block.riftverse.garagen_gehaeuse': 'Garage Casing', 'block.riftverse.xenit_gehaeuse': 'Xenite Casing',
    'block.riftverse.garagen_werkbank': 'Garage Workbench', 'block.riftverse.garagenpresse': 'Garage Press',
    'block.riftverse.kurbel_dynamo': 'Crank Dynamo', 'block.riftverse.tonnenreaktor': 'Barrel Reactor',
    'block.riftverse.erzmuehle': 'Ore Mill', 'block.riftverse.walzbank': 'Rolling Bench',
    'block.riftverse.legierungsofen': 'Alloy Furnace', 'block.riftverse.kupferkabel': 'Copper Cable',
    'block.riftverse.kupferkabel_isoliert': 'Insulated Copper Cable', 'block.riftverse.einspeiser': 'Energy Inlet',
    'block.riftverse.ausspeiser': 'Energy Outlet', 'block.riftverse.kreativ_quelle': 'Creative Source',
    'item.riftverse.raw_tin': 'Raw Tin', 'item.riftverse.tin_ingot': 'Tin Ingot',
    'item.riftverse.raw_xenit': 'Raw Xenite', 'item.riftverse.xenit_ingot': 'Xenite Ingot',
    'item.riftverse.bronze_ingot': 'Bronze Ingot', 'item.riftverse.steel_ingot': 'Steel Ingot',
    'item.riftverse.mischmetall': 'Mixed Metal Ingot',
    'item.riftverse.eisen_staub': 'Iron Dust', 'item.riftverse.kupfer_staub': 'Copper Dust',
    'item.riftverse.gold_staub': 'Gold Dust', 'item.riftverse.zinn_staub': 'Tin Dust',
    'item.riftverse.xenit_staub': 'Xenite Dust', 'item.riftverse.bronze_staub': 'Bronze Dust',
    'item.riftverse.kohle_staub': 'Coal Dust',
    'item.riftverse.eisen_platte': 'Iron Plate', 'item.riftverse.kupfer_platte': 'Copper Plate',
    'item.riftverse.zinn_platte': 'Tin Plate', 'item.riftverse.gold_platte': 'Gold Plate',
    'item.riftverse.bronze_platte': 'Bronze Plate', 'item.riftverse.stahl_platte': 'Steel Plate',
    'item.riftverse.xenit_platte': 'Xenite Plate',
    'item.riftverse.kupferdraht': 'Copper Wire', 'item.riftverse.golddraht': 'Gold Wire',
    'item.riftverse.loetzinn': 'Solder', 'item.riftverse.zinn_huelse': 'Tin Casing',
    'item.riftverse.eisen_stange': 'Iron Rod', 'item.riftverse.xenit_stange': 'Xenite Rod',
    'item.riftverse.bronze_zahnrad': 'Bronze Gear', 'item.riftverse.xenit_spule': 'Xenite Coil',
    'item.riftverse.alien_schrott': 'Alien Scrap', 'item.riftverse.schrottplatte': 'Scrap Plate',
    'item.riftverse.polymerfetzen': 'Polymer Scraps', 'item.riftverse.isolierfolie': 'Insulation Foil',
    'item.riftverse.feder': 'Spring', 'item.riftverse.mini_motor': 'Mini Motor',
    'item.riftverse.garagen_hammer': 'Garage Hammer', 'item.riftverse.loetkolben': 'Soldering Iron',
    'item.riftverse.handkurbel': 'Hand Crank', 'item.riftverse.energiezelle': 'Energy Cell',
    'itemGroup.riftverse': 'Riftverse',
    'gui.riftverse.mode.walzen': 'Roll', 'gui.riftverse.mode.schneiden': 'Cut', 'gui.riftverse.mode.ziehen': 'Draw',
    'gui.riftverse.momentum': 'Momentum', 'gui.riftverse.parts': 'Small parts',
    'tier.riftverse.voltage.i': 'Tier I', 'tier.riftverse.voltage.ii': 'Tier II',
    'tier.riftverse.voltage.iii': 'Tier III', 'tier.riftverse.voltage.iv': 'Tier IV',
    'tooltip.riftverse.voltage': '%s (max. %s FE/t per packet)',
    'tooltip.riftverse.voltage_unlimited': '%s (unlimited)',
    'tooltip.riftverse.energy': '%s / %s FE',
    'tooltip.riftverse.energiezelle': 'Battery slot: machines draw power, generators charge',
    'tooltip.riftverse.consumption': 'Usage: %s FE/t',
    'tooltip.riftverse.output': 'Output: %s FE/t',
    'tooltip.riftverse.progress': 'Progress: %s %%',
    'tooltip.riftverse.crank_hint': 'Crank it with the Hand Crank: hold right-click',
    'tooltip.riftverse.reactor_fuel': 'Burns any item, scrap counts double',
    'tooltip.riftverse.werkbank_tool': 'Tool slot',
    'message.riftverse.net.empty': 'No Riftverse grid on this cable',
    'message.riftverse.net.info': 'Grid: %s sources, %s consumers, %s cables · %s FE/t · %s',
    'message.riftverse.einspeiser': 'Energy Inlet: %s / %s FE · feeds in at %s',
    'message.riftverse.ausspeiser': 'Energy Outlet: %s / %s FE · accepts %s',
    'message.riftverse.kreativ_quelle': 'Creative Source: %s',
    'death.attack.riftverse.stromschlag': '%1$s got shocked by a bare cable',
    'death.attack.riftverse.stromschlag.player': '%1$s got shocked by a bare cable while fleeing %2$s',
}


def lang(texture_pack_de=None):
    for code, entries in (('de_de', {**DE, **M2_DE}), ('en_us', {**EN, **M2_EN})):
        p = os.path.join(ASSETS, 'lang', code + '.json')
        data = {}
        if os.path.exists(p):
            with open(p, encoding='utf-8') as fh:
                data = json.load(fh)
        if code == 'de_de' and texture_pack_de:
            for k, v in texture_pack_de.items():   # Namen späterer Meilensteine aus dem Texturpaket
                data.setdefault(k, v)
        for k in M2_REMOVE:
            data.pop(k, None)
        data.update(entries)
        write(p, data)


# =============================================================================
# Meilenstein 2 – Spielbarer Prototyp
# =============================================================================
M2_MACHINES = ('xenit_schmelze', 'platinen_aetzer', 'kristall_schleifer', 'recycler', 'fluid_mixer')
M2_ITEMS = ['fluxkristall', 'fluxstaub', 'fluxlinse', 'basis_schaltkreis', 'saeuregel', 'kanonengehaeuse',
            'fluid_kartusche', 'fluid_kartusche_voll', 'saeure_eimer', 'universe_scanner']
PEILER_FRAMES = 16


def m2_models():
    for n in ('meteoritenkern', 'schrott_haufen', 'fluxkristall_ore', 'fluxkristall_block', 'knospender_fluxkristall', 'rift_stone'):
        cube_all(n)
    for n in M2_MACHINES:
        machine(n, 1)

    # Xenit-Zelle: rundum das Zellen-Gehäuse, oben der T1-Deckel
    for suffix in ('', '_on'):
        block_model('xenit_zelle' + suffix, 'minecraft:block/cube_bottom_top', {
            'top': tex('casing_t1_top'), 'bottom': tex('casing_t1_top'), 'side': tex('xenit_zelle_side' + suffix)})
    variants = {}
    for facing, y in HORIZONTAL.items():
        for active in ('false', 'true'):
            variants[f'active={active},facing={facing}'] = variant('xenit_zelle' + ('_on' if active == 'true' else ''), y=y)
    blockstate('xenit_zelle', variants)
    item_from_block('xenit_zelle')

    # Fluxkristall-Cluster: wie Amethyst-Cluster, sechs Richtungen
    block_model('fluxkristall_cluster', 'minecraft:block/cross', {'cross': tex('fluxkristall_cluster')}, render_type='minecraft:cutout')
    rot = {'up': (0, 0), 'down': (180, 0), 'north': (90, 0), 'south': (90, 180), 'east': (90, 90), 'west': (90, 270)}
    blockstate('fluxkristall_cluster', {f'facing={f},waterlogged={w}': variant('fluxkristall_cluster', x=x, y=y)
                                        for f, (x, y) in rot.items() for w in ('false', 'true')})
    write(os.path.join(ASSETS, 'models', 'item', 'fluxkristall_cluster.json'), {
        'parent': 'minecraft:item/generated', 'textures': {'layer0': tex('fluxkristall_cluster')}})

    # Säure: Modell nur für Partikel, gerendert wird über die Fluid-Texturen
    write(os.path.join(ASSETS, 'models', 'block', 'saeure.json'), {'textures': {'particle': tex('saeure_still')}})
    blockstate('saeure', {'': variant('saeure')})

    # Rift-Anker: Fußplatte, Eisenpfahl, Fluxlinse an der Spitze
    def cube(frm, to, texture, uv_side=None):
        faces = {}
        for d in ('north', 'south', 'east', 'west', 'up', 'down'):
            f = {'texture': texture}
            if uv_side and d not in ('up', 'down'):
                f['uv'] = uv_side
            faces[d] = f
        return {'from': frm, 'to': to, 'faces': faces}
    write(os.path.join(ASSETS, 'models', 'block', 'rift_anker.json'), {
        'parent': 'minecraft:block/block', 'render_type': 'minecraft:cutout',
        'textures': {'particle': tex('rift_anker_pfahl'), 'pfahl': tex('rift_anker_pfahl'), 'linse': tex('rift_anker_linse')},
        'elements': [cube([4, 0, 4], [12, 2, 12], '#pfahl', [4, 14, 12, 16]),
                     cube([7, 2, 7], [9, 12, 9], '#pfahl', [7, 2, 9, 12]),
                     cube([5.5, 12, 5.5], [10.5, 16, 10.5], '#linse')]})
    blockstate('rift_anker', {'': variant('rift_anker')})
    item_generated('rift_anker')

    for n in M2_ITEMS:
        item_generated(n)
    item_generated('portal_kanone_mk1', handheld=True)
    write(os.path.join(ASSETS, 'models', 'item', 'schrott_golem_spawn_egg.json'), {'parent': 'minecraft:item/template_spawn_egg'})

    # Meteoritenpeiler: 16 Nadelstellungen, jeweils normal und grün (Xenit-Zone)
    for zone in (False, True):
        for k in range(PEILER_FRAMES):
            name = f'meteoritenpeiler_{"zone_" if zone else ""}{k:02d}'
            item_generated(name, texture=name)
    overrides = []
    for zone in (False, True):
        pre = 'zone_' if zone else ''
        for k in range(PEILER_FRAMES + 1):
            frame = k % PEILER_FRAMES
            angle = 0.0 if k == 0 else (2 * k - 1) / (2 * PEILER_FRAMES)
            pred = {'angle': round(angle, 6)}
            if zone:
                pred[f'{NS}:zone'] = 1
            overrides.append({'predicate': pred, 'model': f'{NS}:item/meteoritenpeiler_{pre}{frame:02d}'})
    write(os.path.join(ASSETS, 'models', 'item', 'meteoritenpeiler.json'), {
        'parent': 'minecraft:item/generated', 'textures': {'layer0': f'{NS}:item/meteoritenpeiler_00'}, 'overrides': overrides})


def loot_drop(name, drop, lo, hi, fortune=True, silk=True):
    """Erz-artiger Drop: mit Behutsamkeit der Block selbst, sonst lo–hi Stück (mit Glück mehr)."""
    item = {'type': 'minecraft:item', 'name': rl(drop), 'functions': [
        {'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': lo, 'max': hi}, 'add': False}]}
    if fortune:
        item['functions'].append({'function': 'minecraft:apply_bonus', 'enchantment': 'minecraft:fortune',
                                  'formula': 'minecraft:ore_drops'})
    item['functions'].append({'function': 'minecraft:explosion_decay'})
    children = [item]
    if silk:
        children.insert(0, {'type': 'minecraft:item', 'name': rl(name), 'conditions': [{
            'condition': 'minecraft:match_tool',
            'predicate': {'predicates': {'minecraft:enchantments': [
                {'enchantments': 'minecraft:silk_touch', 'levels': {'min': 1}}]}}}]})
    write(os.path.join(DATA, NS, 'loot_table', 'blocks', name + '.json'), {
        'type': 'minecraft:block',
        'pools': [{'rolls': 1, 'bonus_rolls': 0, 'entries': [{'type': 'minecraft:alternatives', 'children': children}]}],
        'random_sequence': f'{NS}:blocks/{name}'})


def m2_loot():
    for n in M2_MACHINES + ('rift_anker', 'fluxkristall_block', 'rift_stone'):
        loot_self(n)
    # Xenit-Zelle behält ihre Ladung
    write(os.path.join(DATA, NS, 'loot_table', 'blocks', 'xenit_zelle.json'), {
        'type': 'minecraft:block',
        'pools': [{'rolls': 1, 'bonus_rolls': 0,
                   'entries': [{'type': 'minecraft:item', 'name': rl('xenit_zelle'), 'functions': [
                       {'function': 'minecraft:copy_components', 'source': 'block_entity', 'include': [f'{NS}:energy']}]}],
                   'conditions': [{'condition': 'minecraft:survives_explosion'}]}],
        'random_sequence': f'{NS}:blocks/xenit_zelle'})
    loot_drop('meteoritenkern', 'raw_xenit', 3, 5)
    loot_drop('schrott_haufen', 'alien_schrott', 1, 3)
    loot_drop('fluxkristall_ore', 'fluxkristall', 1, 2)
    loot_drop('fluxkristall_cluster', 'fluxkristall', 1, 2)
    write(os.path.join(DATA, NS, 'loot_table', 'blocks', 'knospender_fluxkristall.json'),
          {'type': 'minecraft:block', 'pools': [], 'random_sequence': f'{NS}:blocks/knospender_fluxkristall'})
    write(os.path.join(DATA, NS, 'loot_table', 'entities', 'schrott_golem.json'), {
        'type': 'minecraft:entity',
        'pools': [
            {'rolls': 1, 'bonus_rolls': 0, 'entries': [{'type': 'minecraft:item', 'name': rl('alien_schrott'), 'functions': [
                {'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': 1, 'max': 3}, 'add': False},
                {'function': 'minecraft:enchanted_count_increase', 'enchantment': 'minecraft:looting',
                 'count': {'type': 'minecraft:uniform', 'min': 0, 'max': 1}}]}]},
            {'rolls': 1, 'bonus_rolls': 0, 'entries': [{'type': 'minecraft:item', 'name': 'minecraft:iron_nugget', 'functions': [
                {'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': 2, 'max': 5}, 'add': False}]}]}],
        'random_sequence': f'{NS}:entities/schrott_golem'})


def m2_tags():
    tag('block', 'minecraft:mineable/pickaxe', ['meteoritenkern', 'fluxkristall_ore', 'fluxkristall_block', 'fluxkristall_cluster',
                                                'knospender_fluxkristall', 'xenit_zelle', 'rift_anker', 'rift_stone', *M2_MACHINES])
    tag('block', 'minecraft:mineable/shovel', ['schrott_haufen'])
    tag('block', 'minecraft:needs_stone_tool', ['xenit_zelle', 'rift_anker', 'rift_stone', *M2_MACHINES])
    tag('block', 'minecraft:needs_iron_tool', ['meteoritenkern', 'fluxkristall_ore'])
    tag('block', 'minecraft:crystal_sound_blocks', ['fluxkristall_block', 'knospender_fluxkristall'])
    for kind in ('block', 'item'):
        tag(kind, 'c:ores/fluxkristall', ['fluxkristall_ore'])
        tag(kind, 'c:ores', ['#c:ores/fluxkristall'])
        tag(kind, 'c:ores_in_ground/deepslate', ['fluxkristall_ore'])
    tag('item', 'c:gems/fluxkristall', ['fluxkristall'])
    tag('item', 'c:dusts/fluxkristall', ['fluxstaub'])
    tag('item', 'c:buckets/saeure', ['saeure_eimer'])
    tag('item', f'{NS}:schrott', ['schrott_haufen'])
    tag('block', f'{NS}:zaehlt_nicht_als_bau', ['minecraft:torch', 'minecraft:wall_torch', 'minecraft:soul_torch',
                                               'minecraft:soul_wall_torch', 'minecraft:redstone_torch',
                                               'minecraft:redstone_wall_torch', 'minecraft:scaffolding', 'minecraft:ladder'])
    tag('worldgen/biome', f'{NS}:has_structure/meteoritenkrater', ['#minecraft:is_overworld'])
    tag('worldgen/structure', f'{NS}:meteoritenkrater', [f'{NS}:meteoritenkrater'])
    tag('worldgen/biome', 'minecraft:allows_surface_slime_spawns', [f'{NS}:toxisch_gruen', f'{NS}:toxisch_schwefel'])
    tag('damage_type', 'minecraft:no_knockback', [f'{NS}:saeure'])
    tag('damage_type', 'minecraft:bypasses_armor', [f'{NS}:saeure'])


def machine_recipe2(machine, path, ingredients, out, count=1, time=None, byproducts=None):
    r = {'type': f'{NS}:{machine}',
         'ingredients': [{'ingredient': ing(i), **({'count': c} if c != 1 else {})} for i, c in ingredients],
         'result': result(out, count)}
    if time:
        r['time'] = time
    if byproducts:
        r['byproducts'] = [{'result': result(i, c), **({'chance': ch} if ch < 1 else {})} for i, c, ch in byproducts]
    recipe(f'{machine}/{path}', r)


def m2_recipes():
    # --- Garagen-Werkbank ---------------------------------------------------------
    werkbank_shaped('meteoritenpeiler', ['K K', 'RCR', 'K K'],
                    {'K': '#c:plates/copper', 'R': '#c:dusts/redstone', 'C': 'minecraft:compass'}, 'meteoritenpeiler', hammer=1)
    werkbank_shaped('kanonengehaeuse', ['XXX', 'XS ', ' S '], {'X': '#c:plates/xenit', 'S': '#c:rods/iron'},
                    'kanonengehaeuse', hammer=2)
    werkbank_shaped('fluid_kartusche', [' H ', 'G G', ' H '], {'H': 'zinn_huelse', 'G': '#c:glass_panes'},
                    'fluid_kartusche', 2, loetzinn=1)
    werkbank_shaped('portal_kanone_mk1', [' L ', 'SGS', ' K '],
                    {'L': 'fluxlinse', 'S': 'basis_schaltkreis', 'G': 'kanonengehaeuse',
                     'K': ['fluid_kartusche', 'fluid_kartusche_voll']}, 'portal_kanone_mk1', loetzinn=2)
    werkbank_shaped('rift_anker', ['ILI', ' X ', 'I I'], {'I': '#c:plates/iron', 'L': 'fluxlinse', 'X': '#c:rods/xenit'},
                    'rift_anker', hammer=1)
    werkbank_shaped('universe_scanner', [' L ', 'XCX', ' B '],
                    {'L': 'fluxlinse', 'X': '#c:plates/xenit', 'C': 'minecraft:compass', 'B': 'basis_schaltkreis'},
                    'universe_scanner', loetzinn=1)
    werkbank_shaped('xenit_zelle', ['ESE', 'ECE', 'EBE'],
                    {'E': 'energiezelle', 'S': 'xenit_spule', 'C': 'xenit_gehaeuse', 'B': 'basis_schaltkreis'},
                    'xenit_zelle', loetzinn=2)
    werkbank_shaped('xenit_schmelze', ['XFX', 'SCS', 'IBI'],
                    {'X': '#c:plates/xenit', 'F': 'minecraft:furnace', 'S': 'xenit_spule', 'C': 'xenit_gehaeuse',
                     'I': '#c:plates/iron', 'B': 'basis_schaltkreis'}, 'xenit_schmelze', loetzinn=2)
    # Der Platinen-Ätzer macht die Schaltkreise erst, also kommt er ohne aus
    werkbank_shaped('platinen_aetzer', ['RLR', 'ICI', 'KSK'],
                    {'R': '#c:dusts/redstone', 'L': 'minecraft:redstone_lamp', 'I': '#c:plates/iron', 'C': 'xenit_gehaeuse',
                     'K': '#c:plates/copper', 'S': 'xenit_spule'}, 'platinen_aetzer', loetzinn=2)
    werkbank_shaped('kristall_schleifer', ['FGF', 'ICI', 'BSB'],
                    {'F': 'minecraft:flint', 'G': 'minecraft:grindstone', 'I': '#c:plates/iron', 'C': 'xenit_gehaeuse',
                     'B': 'basis_schaltkreis', 'S': 'xenit_spule'}, 'kristall_schleifer', loetzinn=2)
    werkbank_shaped('recycler', ['HMH', 'ICI', 'BSB'],
                    {'H': 'minecraft:hopper', 'M': 'mini_motor', 'I': '#c:plates/iron', 'C': 'xenit_gehaeuse',
                     'B': 'basis_schaltkreis', 'S': 'xenit_spule'}, 'recycler', loetzinn=2)
    werkbank_shaped('fluid_mixer', ['GEG', 'TCT', 'BSB'],
                    {'G': 'minecraft:glass', 'E': 'minecraft:bucket', 'T': 'zinn_huelse', 'C': 'xenit_gehaeuse',
                     'B': 'basis_schaltkreis', 'S': 'xenit_spule'}, 'fluid_mixer', loetzinn=2)

    # --- Normale Werkbank ------------------------------------------------------------
    shaped_vanilla('fluxkristall_block', ['KK', 'KK'], {'K': 'fluxkristall'}, 'fluxkristall_block')
    shaped_vanilla('rift_stone', ['SSS', 'SFS', 'SSS'], {'S': 'minecraft:stone', 'F': 'fluxstaub'}, 'rift_stone', 8)

    # --- Maschinen ---------------------------------------------------------------------
    machine_recipe2('erzmuehle', 'fluxkristall_ore', [('#c:ores/fluxkristall', 1)], 'fluxkristall', 2, time=150)
    machine_recipe2('erzmuehle', 'meteoritenkern', [('meteoritenkern', 1)], 'xenit_staub', 6, time=300)
    machine_recipe2('xenit_schmelze', 'xenit_ingot', [('#c:dusts/xenit', 1)], 'xenit_ingot', 1, time=100)
    machine_recipe2('xenit_schmelze', 'xenit_ingot_from_raw', [('#c:raw_materials/xenit', 1)], 'xenit_ingot', 1, time=100)
    machine_recipe2('platinen_aetzer', 'basis_schaltkreis',
                    [('schrottplatte', 1), ('#c:dusts/redstone', 1), ('#c:wires/copper', 3)], 'basis_schaltkreis', 1, time=200)
    machine_recipe2('kristall_schleifer', 'fluxlinse', [('#c:gems/fluxkristall', 1)], 'fluxlinse', 1, time=160,
                    byproducts=[('fluxstaub', 2, 1.0)])
    machine_recipe2('recycler', 'alien_schrott', [('alien_schrott', 1)], 'polymerfetzen', 1, time=100,
                    byproducts=[('kupferdraht', 1, 0.4), ('feder', 1, 0.25), ('mini_motor', 1, 0.1), ('minecraft:iron_nugget', 2, 0.3)])
    recipe('fluid_mixer/roh_portalfluid', {
        'type': f'{NS}:fluid_mixer',
        'ingredients': [{'ingredient': ing('#c:dusts/fluxkristall')}],
        'fluid': {'fluid': 'minecraft:water', 'amount': 1000},
        'result': {'id': rl('roh_portalfluid'), 'amount': 1000},
        'time': 200})


# --- Worldgen: Krater, Xenit-Zone, Fluxkristall, Wracks --------------------------------
def placed(name, feature, placement):
    write(os.path.join(DATA, NS, 'worldgen', 'placed_feature', name + '.json'), {'feature': rl(feature), 'placement': placement})


def add_features(name, features, step, biomes='#minecraft:is_overworld'):
    write(os.path.join(DATA, NS, 'neoforge', 'biome_modifier', name + '.json'), {
        'type': 'neoforge:add_features', 'biomes': biomes, 'features': rl(features), 'step': step})


def ore_feature(name, stone, deepslate, size):
    targets = []
    if stone:
        targets.append({'state': {'Name': rl(stone)},
                        'target': {'predicate_type': 'minecraft:tag_match', 'tag': 'minecraft:stone_ore_replaceables'}})
    targets.append({'state': {'Name': rl(deepslate)},
                    'target': {'predicate_type': 'minecraft:tag_match', 'tag': 'minecraft:deepslate_ore_replaceables'}})
    write(os.path.join(DATA, NS, 'worldgen', 'configured_feature', name + '.json'), {
        'type': 'minecraft:ore', 'config': {'discard_chance_on_air_exposure': 0.0, 'size': size, 'targets': targets}})


def m2_worldgen():
    # Meteoritenkrater: random_spread, spacing 62 / separation 56 Chunks -> alle 900–1.100 Blöcke
    write(os.path.join(DATA, NS, 'worldgen', 'structure', 'meteoritenkrater.json'), {
        'type': rl('meteoritenkrater'), 'biomes': f'#{NS}:has_structure/meteoritenkrater', 'spawn_overrides': {},
        'step': 'surface_structures', 'terrain_adaptation': 'none'})
    write(os.path.join(DATA, NS, 'worldgen', 'structure_set', 'meteoritenkrater.json'), {
        'structures': [{'structure': rl('meteoritenkrater'), 'weight': 1}],
        'placement': {'type': 'minecraft:random_spread', 'salt': 1847263195, 'spacing': 62, 'separation': 56,
                      'locate_offset': [8, 0, 8]}})

    # Xenit-Zone: 1–2 Adern à 3–6 Erz pro Chunk am Krater, Eisen-Band Y -24..56 (am dichtesten um Y 16)
    ore_feature('ore_xenit_krater', 'xenit_ore', 'deepslate_xenit_ore', 6)
    placed('ore_xenit_krater', 'ore_xenit_krater', [
        {'type': 'minecraft:count', 'count': {'type': 'minecraft:uniform', 'min_inclusive': 1, 'max_inclusive': 2}},
        {'type': rl('krater_zone')},
        {'type': 'minecraft:in_square'},
        {'type': 'minecraft:height_range', 'height': {'type': 'minecraft:trapezoid',
                                                      'min_inclusive': {'absolute': -24}, 'max_inclusive': {'absolute': 56}}},
        {'type': 'minecraft:biome'}])
    add_features('add_xenit_krater', 'ore_xenit_krater', 'underground_ores')

    # Fluxkristall: Overworld unter Y 0, selten
    ore_feature('ore_fluxkristall', None, 'fluxkristall_ore', 4)
    placed('ore_fluxkristall', 'ore_fluxkristall', [
        {'type': 'minecraft:count', 'count': 2},
        {'type': 'minecraft:in_square'},
        {'type': 'minecraft:height_range', 'height': {'type': 'minecraft:uniform',
                                                      'min_inclusive': {'above_bottom': 0}, 'max_inclusive': {'absolute': 0}}},
        {'type': 'minecraft:biome'}])
    add_features('add_fluxkristall', 'ore_fluxkristall', 'underground_ores')

    # Alien-Wracks: in der Overworld selten
    write(os.path.join(DATA, NS, 'worldgen', 'configured_feature', 'alien_wrack.json'), {'type': rl('alien_wrack'), 'config': {}})
    placed('alien_wrack', 'alien_wrack', [
        {'type': 'minecraft:rarity_filter', 'chance': 48},
        {'type': 'minecraft:in_square'},
        {'type': 'minecraft:heightmap', 'heightmap': 'WORLD_SURFACE_WG'},
        {'type': 'minecraft:biome'}])
    add_features('add_alien_wrack', 'alien_wrack', 'surface_structures')

    write(os.path.join(DATA, NS, 'damage_type', 'saeure.json'), {
        'exhaustion': 0.1, 'message_id': f'{NS}.saeure', 'scaling': 'never'})


# --- Universen: Dimensionen und Biome -----------------------------------------------------
def mood():
    return {'sound': 'minecraft:ambient.cave', 'tick_delay': 6000, 'block_search_extent': 8, 'offset': 2.0}


def spawn(kind, weight, lo, hi):
    return {'type': kind, 'weight': weight, 'minCount': lo, 'maxCount': hi}


def biome(name, precipitation, effects, monsters, creatures, temperature=0.8):
    write(os.path.join(DATA, NS, 'worldgen', 'biome', name + '.json'), {
        'has_precipitation': precipitation, 'temperature': temperature, 'downfall': 0.4 if precipitation else 0.0,
        'effects': {**effects, 'mood_sound': mood()},
        'spawners': {'monster': monsters, 'creature': creatures,
                     'ambient': [spawn('minecraft:bat', 10, 2, 4)], 'axolotls': [], 'underground_water_creature': [],
                     'water_creature': [], 'water_ambient': [], 'misc': []},
        'spawn_costs': {}, 'carvers': {'air': [], 'liquid': []}, 'features': []})


UNIVERSES = {
    # typ: (Biome, fixed_time, ambient_light)
    'schrottwelt': (['schrott_dunst', 'schrott_rost'], None, 0.0),
    'kristallwelt': (['kristall_glanz', 'kristall_aurora'], 6000, 0.1),
    'toxische_welt': (['toxisch_gruen', 'toxisch_schwefel'], None, 0.0),
}


def m2_universes():
    scrap_monsters = [spawn(f'{NS}:schrott_golem', 40, 1, 2), spawn('minecraft:zombie', 40, 2, 3), spawn('minecraft:spider', 20, 1, 2)]
    crystal_monsters = [spawn('minecraft:skeleton', 40, 1, 2), spawn('minecraft:endermite', 10, 1, 2)]
    toxic_monsters = [spawn('minecraft:slime', 60, 1, 3), spawn('minecraft:zombie', 40, 2, 3),
                      spawn('minecraft:spider', 30, 1, 2), spawn('minecraft:witch', 10, 1, 1)]
    rabbits = [spawn('minecraft:rabbit', 6, 1, 2)]
    biome('schrott_dunst', False, {'sky_color': 0x8E8B7E, 'fog_color': 0x7F7A70, 'water_color': 0x5A5A3C,
                                   'water_fog_color': 0x2F2F20, 'grass_color': 0x8A7A50, 'foliage_color': 0x7A6A40},
          scrap_monsters, rabbits)
    biome('schrott_rost', False, {'sky_color': 0x9A7A60, 'fog_color': 0x8A6A50, 'water_color': 0x6A4A30,
                                  'water_fog_color': 0x3A2A1A, 'grass_color': 0x8A6A40, 'foliage_color': 0x7A5A30},
          scrap_monsters, rabbits)
    biome('kristall_glanz', False, {'sky_color': 0x9AD8FF, 'fog_color': 0xD8FFF0, 'water_color': 0x7FFFD4,
                                    'water_fog_color': 0x3FCFAF, 'grass_color': 0x7FE0A0, 'foliage_color': 0x5FC080},
          crystal_monsters, rabbits)
    biome('kristall_aurora', False, {'sky_color': 0xB8A0FF, 'fog_color': 0xE0D8FF, 'water_color': 0x9A6BFF,
                                     'water_fog_color': 0x5A3AA8, 'grass_color': 0x9AA0F0, 'foliage_color': 0x7A80D0},
          crystal_monsters, rabbits)
    frogs = [spawn('minecraft:frog', 10, 2, 4)]
    biome('toxisch_gruen', True, {'sky_color': 0xA8C840, 'fog_color': 0xB8D860, 'water_color': 0xB2D62A,
                                  'water_fog_color': 0x76980F, 'grass_color': 0x9ACD32, 'foliage_color': 0x7FA020},
          toxic_monsters, frogs)
    biome('toxisch_schwefel', True, {'sky_color': 0xD8C840, 'fog_color': 0xE0D070, 'water_color': 0xD5F45A,
                                     'water_fog_color': 0x8A9A10, 'grass_color': 0xB8B030, 'foliage_color': 0x9A9020},
          toxic_monsters, frogs)

    for dim, (biomes, fixed_time, ambient) in UNIVERSES.items():
        dt = {'ultrawarm': False, 'natural': True, 'piglin_safe': False, 'respawn_anchor_works': False, 'bed_works': True,
              'has_raids': False, 'has_skylight': True, 'has_ceiling': False, 'coordinate_scale': 1.0,
              'ambient_light': ambient, 'logical_height': 384, 'effects': 'minecraft:overworld',
              'infiniburn': '#minecraft:infiniburn_overworld', 'min_y': -64, 'height': 384,
              'monster_spawn_light_level': 7, 'monster_spawn_block_light_limit': 0}
        if fixed_time is not None:
            dt['fixed_time'] = fixed_time
        write(os.path.join(DATA, NS, 'dimension_type', dim + '.json'), dt)
        write(os.path.join(DATA, NS, 'dimension', dim + '.json'), {
            'type': rl(dim),
            'generator': {'type': rl('multiverse'), 'universe': dim,
                          'biome_source': {'type': rl('universe'), 'universe': dim, 'biomes': [rl(b) for b in biomes]}}})


M2_DE = {
    'block.riftverse.meteoritenkern': 'Meteoritenkern', 'block.riftverse.schrott_haufen': 'Schrotthaufen',
    'block.riftverse.fluxkristall_ore': 'Fluxkristall-Erz', 'block.riftverse.fluxkristall_block': 'Fluxkristall-Block',
    'block.riftverse.fluxkristall_cluster': 'Fluxkristall-Cluster', 'block.riftverse.knospender_fluxkristall': 'Knospender Fluxkristall',
    'block.riftverse.saeure': 'Säure', 'block.riftverse.xenit_schmelze': 'Xenit-Schmelze',
    'block.riftverse.platinen_aetzer': 'Platinen-Ätzer', 'block.riftverse.kristall_schleifer': 'Kristall-Schleifer',
    'block.riftverse.recycler': 'Recycler', 'block.riftverse.fluid_mixer': 'Fluid-Mixer', 'block.riftverse.xenit_zelle': 'Xenit-Zelle',
    'block.riftverse.rift_anker': 'Rift-Anker', 'block.riftverse.rift_stone': 'Rift-Stein',
    'item.riftverse.fluxkristall': 'Fluxkristall', 'item.riftverse.fluxstaub': 'Fluxstaub', 'item.riftverse.fluxlinse': 'Fluxlinse',
    'item.riftverse.basis_schaltkreis': 'Basis-Schaltkreis', 'item.riftverse.saeuregel': 'Säuregel',
    'item.riftverse.saeuregel.lore': 'Glasflasche an einer Säurequelle füllen. Wird ab dem Rift-Labor gebraucht.',
    'item.riftverse.saeure_eimer': 'Säureeimer', 'item.riftverse.kanonengehaeuse': 'Kanonengehäuse',
    'item.riftverse.fluid_kartusche': 'Fluid-Kartusche (leer)', 'item.riftverse.fluid_kartusche_voll': 'Fluid-Kartusche (voll)',
    'item.riftverse.portal_kanone_mk1': 'Portal-Kanone Mk I', 'item.riftverse.meteoritenpeiler': 'Meteoritenpeiler',
    'item.riftverse.universe_scanner': 'Universums-Scanner',
    'item.riftverse.universe_scanner.lore': 'Rechtsklick: Typ, Naturgesetze und Modifikatoren des Universums anzeigen.',
    'item.riftverse.schrott_golem_spawn_egg': 'Schrott-Golem-Spawn-Ei',
    'entity.riftverse.schrott_golem': 'Schrott-Golem', 'entity.riftverse.rift_portal': 'Rift', 'entity.riftverse.rift_boss': 'Rift-Boss',
    'fluid_type.riftverse.saeure': 'Säure', 'fluid_type.riftverse.roh_portalfluid': 'Roh-Portalfluid',
    'universe.riftverse.schrottwelt': 'Schrottwelt', 'universe.riftverse.kristallwelt': 'Kristallwelt',
    'universe.riftverse.toxische_welt': 'Toxische Welt',
    'dimension.riftverse.schrottwelt': 'Schrottwelten', 'dimension.riftverse.kristallwelt': 'Kristallwelten',
    'dimension.riftverse.toxische_welt': 'Toxische Welten',
    'biome.riftverse.schrott_dunst': 'Schrottdunst', 'biome.riftverse.schrott_rost': 'Rostige Halden',
    'biome.riftverse.kristall_glanz': 'Kristallglanz', 'biome.riftverse.kristall_aurora': 'Kristall-Aurora',
    'biome.riftverse.toxisch_gruen': 'Giftgrüne Weiten', 'biome.riftverse.toxisch_schwefel': 'Schwefeldunst',
    'modifier.riftverse.riesen_mobs': 'Riesen-Mobs', 'modifier.riftverse.mini_mobs': 'Mini-Mobs',
    'modifier.riftverse.doppelte_erze': 'Doppelte Erzvorkommen', 'modifier.riftverse.keine_tiere': 'Keine Tiere',
    'modifier.riftverse.gewitter': 'Dauergewitter', 'modifier.riftverse.supraleitend': 'Supraleitend',
    'modifier.riftverse.hochohmig': 'Hochohmig', 'modifier.riftverse.huepfen': 'Hüpfende Kreaturen',
    'modifier.riftverse.leuchten': 'Disco-Kreaturen',
    'key.riftverse.rueckkehr': 'Portal-Kanone: Rückkehr', 'key.categories.riftverse': 'Riftverse',
    'gui.riftverse.laden': 'Laden', 'gui.riftverse.entladen': 'Entladen',
    'tooltip.riftverse.tank': '%s: %s / %s mB',
    'tooltip.riftverse.kartusche': '%s mB Roh-Portalfluid',
    'tooltip.riftverse.stabil': 'Stabil (Kreativmodus)',
    'tooltip.riftverse.zerfallen': 'Zerfallen',
    'tooltip.riftverse.zerfaellt_in': 'Zerfällt in %s',
    'tooltip.riftverse.kanone_tank': 'Roh-Portalfluid: %s / %s mB (%s mB pro Schuss)',
    'tooltip.riftverse.kanone_mk1': 'Mk I: rein zufällige Sprünge in Schrott-, Kristall- und toxische Welten',
    'tooltip.riftverse.kanone_bedienung': 'Rechtsklick: Rift öffnen · Schleichen oder R: Rückkehr zum Heimanker',
    'tooltip.riftverse.peiler_kein_ziel': 'Kein Krater in Reichweite (nur in der Overworld)',
    'tooltip.riftverse.peiler_ziel': 'Die Nadel zeigt zum nächsten Meteoritenkrater',
    'tooltip.riftverse.peiler_zone': 'Du bist in der Xenit-Zone!',
    'tooltip.riftverse.rift_anker': 'Hält das Universum, in dem er steht: Es wird nie zurückgesetzt.',
    'tooltip.riftverse.xenit_zelle': 'Stufe I · behält die Ladung beim Abbauen',
    'message.riftverse.arrival': '%s · %s',
    'message.riftverse.rift_open': 'Rift geöffnet → unbekanntes Universum',
    'message.riftverse.rift_home': 'Rift geöffnet → Heimanker',
    'message.riftverse.already_home': 'Du bist schon in deiner Heimat-Realität.',
    'message.riftverse.kein_fluid': 'Kein Roh-Portalfluid! Fülle Fluid-Kartuschen im Fluid-Mixer.',
    'message.riftverse.kanone_zerfallen': 'Das Roh-Portalfluid in der Kanone ist zerfallen.',
    'message.riftverse.kartusche_zerfallen': 'Roh-Portalfluid in einer Kartusche ist zerfallen.',
    'message.riftverse.keine_kanone': 'Du hast keine Portal-Kanone dabei.',
    'message.riftverse.toxic_air': '☣ Giftige Luft – setz einen Helm auf!',
    'message.riftverse.acid_rain': '☣ Säureregen frisst deine Rüstung an!',
    'message.riftverse.resonance': 'Die Fluxkristalle schwingen mit – schleich lieber!',
    'message.riftverse.scanner.home': '⌁ Scanner: Heimat-Realität. 1,00 g, atembar.',
    'message.riftverse.scanner.type': 'Typ', 'message.riftverse.scanner.modifiers': 'Modifikatoren',
    'message.riftverse.scanner.gravity': 'Gravitation', 'message.riftverse.scanner.air': 'Luft',
    'message.riftverse.scanner.terrain': 'Gelände', 'message.riftverse.scanner.status': 'Zustand',
    'message.riftverse.scanner.anchored': 'verankert',
    'message.riftverse.scanner.built': 'bebaut – bleibt erhalten',
    'message.riftverse.scanner.reset': 'wird in %s Tagen zurückgesetzt (ohne Besuch)',
    'death.attack.riftverse.saeure': '%1$s hat sich in Säure aufgelöst',
    'death.attack.riftverse.saeure.player': '%1$s hat sich auf der Flucht vor %2$s in Säure aufgelöst',
}

M2_EN = {
    'block.riftverse.meteoritenkern': 'Meteorite Core', 'block.riftverse.schrott_haufen': 'Scrap Heap',
    'block.riftverse.fluxkristall_ore': 'Flux Crystal Ore', 'block.riftverse.fluxkristall_block': 'Block of Flux Crystal',
    'block.riftverse.fluxkristall_cluster': 'Flux Crystal Cluster', 'block.riftverse.knospender_fluxkristall': 'Budding Flux Crystal',
    'block.riftverse.saeure': 'Acid', 'block.riftverse.xenit_schmelze': 'Xenite Smelter',
    'block.riftverse.platinen_aetzer': 'Circuit Etcher', 'block.riftverse.kristall_schleifer': 'Crystal Grinder',
    'block.riftverse.recycler': 'Recycler', 'block.riftverse.fluid_mixer': 'Fluid Mixer', 'block.riftverse.xenit_zelle': 'Xenite Cell',
    'block.riftverse.rift_anker': 'Rift Anchor', 'block.riftverse.rift_stone': 'Rift Stone',
    'item.riftverse.fluxkristall': 'Flux Crystal', 'item.riftverse.fluxstaub': 'Flux Dust', 'item.riftverse.fluxlinse': 'Flux Lens',
    'item.riftverse.basis_schaltkreis': 'Basic Circuit', 'item.riftverse.saeuregel': 'Acid Gel',
    'item.riftverse.saeuregel.lore': 'Fill a glass bottle at an acid source. Needed from the Rift Lab on.',
    'item.riftverse.saeure_eimer': 'Acid Bucket', 'item.riftverse.kanonengehaeuse': 'Gun Housing',
    'item.riftverse.fluid_kartusche': 'Fluid Cartridge (empty)', 'item.riftverse.fluid_kartusche_voll': 'Fluid Cartridge (full)',
    'item.riftverse.portal_kanone_mk1': 'Portal Gun Mk I', 'item.riftverse.meteoritenpeiler': 'Meteorite Locator',
    'item.riftverse.universe_scanner': 'Universe Scanner',
    'item.riftverse.universe_scanner.lore': 'Right-click: show type, laws of physics and modifiers of this universe.',
    'item.riftverse.schrott_golem_spawn_egg': 'Scrap Golem Spawn Egg',
    'entity.riftverse.schrott_golem': 'Scrap Golem', 'entity.riftverse.rift_portal': 'Rift', 'entity.riftverse.rift_boss': 'Rift Boss',
    'fluid_type.riftverse.saeure': 'Acid', 'fluid_type.riftverse.roh_portalfluid': 'Raw Portal Fluid',
    'universe.riftverse.schrottwelt': 'Scrap World', 'universe.riftverse.kristallwelt': 'Crystal World',
    'universe.riftverse.toxische_welt': 'Toxic World',
    'dimension.riftverse.schrottwelt': 'Scrap Worlds', 'dimension.riftverse.kristallwelt': 'Crystal Worlds',
    'dimension.riftverse.toxische_welt': 'Toxic Worlds',
    'biome.riftverse.schrott_dunst': 'Scrap Haze', 'biome.riftverse.schrott_rost': 'Rusty Heaps',
    'biome.riftverse.kristall_glanz': 'Crystal Gleam', 'biome.riftverse.kristall_aurora': 'Crystal Aurora',
    'biome.riftverse.toxisch_gruen': 'Toxic Expanse', 'biome.riftverse.toxisch_schwefel': 'Sulphur Haze',
    'modifier.riftverse.riesen_mobs': 'Giant Mobs', 'modifier.riftverse.mini_mobs': 'Tiny Mobs',
    'modifier.riftverse.doppelte_erze': 'Double Ores', 'modifier.riftverse.keine_tiere': 'No Animals',
    'modifier.riftverse.gewitter': 'Endless Thunderstorm', 'modifier.riftverse.supraleitend': 'Superconducting',
    'modifier.riftverse.hochohmig': 'High Resistance', 'modifier.riftverse.huepfen': 'Bouncy Creatures',
    'modifier.riftverse.leuchten': 'Disco Creatures',
    'key.riftverse.rueckkehr': 'Portal Gun: Return', 'key.categories.riftverse': 'Riftverse',
    'gui.riftverse.laden': 'Charge', 'gui.riftverse.entladen': 'Discharge',
    'tooltip.riftverse.tank': '%s: %s / %s mB',
    'tooltip.riftverse.kartusche': '%s mB Raw Portal Fluid',
    'tooltip.riftverse.stabil': 'Stable (Creative)',
    'tooltip.riftverse.zerfallen': 'Decayed',
    'tooltip.riftverse.zerfaellt_in': 'Decays in %s',
    'tooltip.riftverse.kanone_tank': 'Raw Portal Fluid: %s / %s mB (%s mB per shot)',
    'tooltip.riftverse.kanone_mk1': 'Mk I: purely random jumps into Scrap, Crystal and Toxic worlds',
    'tooltip.riftverse.kanone_bedienung': 'Right-click: open rift · Sneak or R: return to home anchor',
    'tooltip.riftverse.peiler_kein_ziel': 'No crater in range (Overworld only)',
    'tooltip.riftverse.peiler_ziel': 'The needle points to the nearest meteorite crater',
    'tooltip.riftverse.peiler_zone': 'You are inside the xenite zone!',
    'tooltip.riftverse.rift_anker': 'Holds the universe it stands in: it is never reset.',
    'tooltip.riftverse.xenit_zelle': 'Tier I · keeps its charge when mined',
    'message.riftverse.arrival': '%s · %s',
    'message.riftverse.rift_open': 'Rift opened → unknown universe',
    'message.riftverse.rift_home': 'Rift opened → home anchor',
    'message.riftverse.already_home': 'You are already in your home reality.',
    'message.riftverse.kein_fluid': 'No raw portal fluid! Fill fluid cartridges in the Fluid Mixer.',
    'message.riftverse.kanone_zerfallen': 'The raw portal fluid in your gun has decayed.',
    'message.riftverse.kartusche_zerfallen': 'Raw portal fluid in a cartridge has decayed.',
    'message.riftverse.keine_kanone': 'You are not carrying a portal gun.',
    'message.riftverse.toxic_air': '☣ Toxic air – put on a helmet!',
    'message.riftverse.acid_rain': '☣ Acid rain is eating your armor!',
    'message.riftverse.resonance': 'The flux crystals resonate – better sneak!',
    'message.riftverse.scanner.home': '⌁ Scanner: home reality. 1.00 g, breathable.',
    'message.riftverse.scanner.type': 'Type', 'message.riftverse.scanner.modifiers': 'Modifiers',
    'message.riftverse.scanner.gravity': 'Gravity', 'message.riftverse.scanner.air': 'Air',
    'message.riftverse.scanner.terrain': 'Terrain', 'message.riftverse.scanner.status': 'State',
    'message.riftverse.scanner.anchored': 'anchored',
    'message.riftverse.scanner.built': 'built on – will be kept',
    'message.riftverse.scanner.reset': 'resets in %s days without a visit',
    'death.attack.riftverse.saeure': '%1$s dissolved in acid',
    'death.attack.riftverse.saeure.player': '%1$s dissolved in acid while fleeing %2$s',
}

# Schlüssel aus dem Prototyp, die es nicht mehr gibt
M2_REMOVE = [
    'block.riftverse.quantum_ore', 'block.riftverse.quantum_refiner', 'block.riftverse.fluid_synthesizer',
    'block.riftverse.rift_assembler', 'item.riftverse.raw_quantum', 'item.riftverse.quantum_ingot',
    'item.riftverse.rift_fluid_cell', 'item.riftverse.rift_core', 'item.riftverse.gun_frame', 'item.riftverse.universe_shard',
    'item.riftverse.rift_gun', 'item.riftverse.rift_gun_mk2', 'item.riftverse.rift_boss_spawn_egg',
    'item.riftverse.raw_quantum.lore', 'item.riftverse.quantum_ingot.lore', 'item.riftverse.rift_fluid_cell.lore',
    'item.riftverse.rift_core.lore', 'item.riftverse.gun_frame.lore', 'item.riftverse.universe_shard.lore',
    'gui.riftverse.recipes',
    'biome.riftverse.emerald_haze', 'biome.riftverse.violet_dusk', 'biome.riftverse.amber_sky', 'biome.riftverse.azure_deep',
    'biome.riftverse.crimson_veil', 'biome.riftverse.ashen_waste', 'biome.riftverse.pink_nebula', 'biome.riftverse.teal_mist',
    'biome.riftverse.toxic_lime', 'biome.riftverse.frost_white', 'biome.riftverse.inferno_ember', 'biome.riftverse.sulfur_haze',
    'biome.riftverse.void_aurora', 'biome.riftverse.starless_black',
    'dimension.riftverse.verse_sun', 'dimension.riftverse.verse_night', 'dimension.riftverse.verse_inferno',
    'dimension.riftverse.verse_void',
]


if __name__ == '__main__':
    import sys
    pack_de = None
    if len(sys.argv) > 1:   # optional: lang/de_de.json aus dem Texturpaket
        with open(sys.argv[1], encoding='utf-8') as fh:
            pack_de = json.load(fh)
    models()
    loot()
    tags()
    recipes()
    worldgen()
    m2_models()
    m2_loot()
    m2_tags()
    m2_recipes()
    m2_worldgen()
    m2_universes()
    lang(pack_de)
    print('fertig')
