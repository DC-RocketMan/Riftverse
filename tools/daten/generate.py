"""Erzeugt Blockstates, Modelle, Loot-Tabellen, Tags, Rezepte, Worldgen, Dimensionen und Sprachdateien.

Jeder Meilenstein (1 Fundament, 2 Spielbarer Prototyp, 3 T2 Rift-Labor, 4 T3 Multiversum-Industrie) steht in einem
eigenen Abschnitt.

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
# Einträge aus dem Prototyp, die es nicht mehr gibt (fliegen beim Zusammenführen raus)
STALE = {f'{NS}:quantum_ore', f'{NS}:quantum_refiner', f'{NS}:fluid_synthesizer', f'{NS}:rift_assembler'}


def tag(kind, tag_id, values):
    ns, path = tag_id.split(':')
    p = os.path.join(DATA, ns, 'tags', kind, path + '.json')
    existing = []
    if os.path.exists(p):
        with open(p, encoding='utf-8') as fh:
            existing = json.load(fh).get('values', [])
    merged = [v for v in existing if v not in STALE]
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
    for code, entries in (('de_de', {**DE, **M2_DE, **M3_DE, **M4_DE, **M5_DE, **M6_DE}), ('en_us', {**EN, **M2_EN, **M3_EN, **M4_EN, **M5_EN, **M6_EN})):
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
                    byproducts=[('kupferdraht', 1, 0.4), ('feder', 1, 0.25), ('mini_motor', 1, 0.1)])
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


# =============================================================================
# Meilenstein 3 – T2 Rift-Labor
# =============================================================================
M3_T2_MACHINES = ('praezisions_montierer', 'quantenzentrifuge', 'gravitationspresse', 'chrono_beschleuniger',
                  'portalfluid_raffinerie', 'fluxkristall_resonator', 'gravitations_pendel')
M3_BLOCKS_SELF = M3_T2_MACHINES + ('dimensions_scanner', 'biomasse_kessel', 'zeitschleifen_solar', 'umspanner',
                                   'labor_gehaeuse', 'labor_huelle', 'resonator_kammer', 'schwebstein_kern',
                                   'flux_leitung', 'flux_leitung_isoliert', 'chronoglas_sand', 'chronoglas')
M3_ITEMS = ['verbundplatte', 'flux_spule', 'rift_schaltkreis', 'stabilisierte_linse', 'kanonenkern', 'chronoglas_griff',
            'chronoglas_scheibe', 'antigrav_platte', 'aetzloesung', 'toxinrest', 'flux_stabilisator', 'naehrserum',
            'fluxkristall_stab', 'kuehlkoerper', 'schwebstein_brocken', 'biomasse_pulpe', 'fluid_kartusche_stabil',
            'universumsprobe', 'koordinatenkarte']


def m3_models():
    cable('flux_leitung', 4, False)
    cable('flux_leitung_isoliert', 6, True)

    # Umspanner: Front = Hochspannungsseite, mit Redstone (hochspannen) leuchtet die Front anders
    for suffix in ('', '_hoch'):
        side = tex('umspanner_side')
        block_model('umspanner' + suffix, 'minecraft:block/cube', {
            'particle': side, 'north': tex('umspanner_front' + suffix), 'south': side, 'east': side, 'west': side,
            'up': side, 'down': side})
    variants = {}
    for facing, (x, y) in ALL_DIRS.items():
        for powered in ('false', 'true'):
            for active in ('false', 'true'):
                variants[f'active={active},facing={facing},powered={powered}'] = variant(
                    'umspanner' + ('_hoch' if powered == 'true' else ''), x=x, y=y)
    blockstate('umspanner', variants)
    item_from_block('umspanner')

    # Flux-Zelle: leuchtendes Füllstandsband an den Seiten (fuellung 0–4)
    for n in range(5):
        block_model(f'flux_zelle_{n}', 'minecraft:block/cube_bottom_top', {
            'top': tex('casing_t2_top'), 'bottom': tex('casing_t2_top'), 'side': tex(f'flux_zelle_side_{n}')})
    variants = {}
    for facing in HORIZONTAL:
        for active in ('false', 'true'):
            for n in range(5):
                variants[f'active={active},facing={facing},fuellung={n}'] = variant(f'flux_zelle_{n}')
    blockstate('flux_zelle', variants)
    item_from_block('flux_zelle', 'flux_zelle_2')

    casing_block('labor_gehaeuse', 2)
    for n in ('labor_huelle', 'resonator_kammer', 'schwebstein_kern', 'schwebstein', 'chronoglas_sand', 'biomasse_block'):
        cube_all(n)
    block_model('chronoglas', 'minecraft:block/cube_all', {'all': tex('chronoglas')}, render_type='minecraft:translucent')
    blockstate('chronoglas', {'': variant('chronoglas')})
    item_from_block('chronoglas')

    for n in M3_T2_MACHINES:
        machine(n, 2)
    machine('biomasse_kessel', 1)
    # Dimensions-Scanner: eigener Deckel mit Antenne
    for suffix in ('', '_on'):
        block_model('dimensions_scanner' + suffix, 'minecraft:block/orientable', {
            'top': tex('dimensions_scanner_top'), 'side': tex('casing_t2'), 'front': tex('dimensions_scanner_front' + suffix)})
    blockstate('dimensions_scanner', {f'active={a},facing={f}': variant('dimensions_scanner' + ('_on' if a == 'true' else ''), y=y)
                                      for f, y in HORIZONTAL.items() for a in ('false', 'true')})
    item_from_block('dimensions_scanner')
    # Zeitschleifen-Solarpanel: Panel oben, Labor-Gehäuse an den Seiten
    block_model('zeitschleifen_solar', 'minecraft:block/cube_bottom_top', {
        'top': tex('zeitschleifen_solar_top'), 'bottom': tex('casing_t2_top'), 'side': tex('casing_t2')})
    blockstate('zeitschleifen_solar', {f'active={a},facing={f}': variant('zeitschleifen_solar')
                                       for f in HORIZONTAL for a in ('false', 'true')})
    item_from_block('zeitschleifen_solar')

    # Fressende Flora: Kreuzmodell
    block_model('fressende_flora', 'minecraft:block/cross', {'cross': tex('fressende_flora')}, render_type='minecraft:cutout')
    blockstate('fressende_flora', {'': variant('fressende_flora')})
    write(os.path.join(ASSETS, 'models', 'item', 'fressende_flora.json'), {
        'parent': 'minecraft:item/generated', 'textures': {'layer0': tex('fressende_flora')}})

    for n in M3_ITEMS:
        item_generated(n)
    item_generated('portal_kanone_mk2', handheld=True)


def m3_loot():
    for n in M3_BLOCKS_SELF:
        loot_self(n)
    write(os.path.join(DATA, NS, 'loot_table', 'blocks', 'flux_zelle.json'), {
        'type': 'minecraft:block',
        'pools': [{'rolls': 1, 'bonus_rolls': 0,
                   'entries': [{'type': 'minecraft:item', 'name': rl('flux_zelle'), 'functions': [
                       {'function': 'minecraft:copy_components', 'source': 'block_entity', 'include': [f'{NS}:energy']}]}],
                   'conditions': [{'condition': 'minecraft:survives_explosion'}]}],
        'random_sequence': f'{NS}:blocks/flux_zelle'})
    loot_drop('schwebstein', 'schwebstein_brocken', 2, 3)
    loot_drop('biomasse_block', 'biomasse_pulpe', 1, 3)
    write(os.path.join(DATA, NS, 'loot_table', 'blocks', 'fressende_flora.json'),
          {'type': 'minecraft:block', 'pools': [], 'random_sequence': f'{NS}:blocks/fressende_flora'})


def m3_tags():
    labor = [*M3_T2_MACHINES, 'dimensions_scanner', 'zeitschleifen_solar', 'umspanner', 'flux_zelle', 'labor_gehaeuse',
             'labor_huelle', 'resonator_kammer', 'schwebstein_kern']
    tag('block', 'minecraft:mineable/pickaxe', [*labor, 'biomasse_kessel', 'schwebstein', 'flux_leitung', 'flux_leitung_isoliert'])
    tag('block', 'minecraft:needs_iron_tool', labor)
    tag('block', 'minecraft:needs_stone_tool', ['biomasse_kessel', 'schwebstein'])
    tag('block', 'minecraft:mineable/shovel', ['chronoglas_sand'])
    tag('block', 'minecraft:mineable/hoe', ['biomasse_block'])
    tag('block', 'minecraft:impermeable', ['chronoglas'])
    tag('block', f'{NS}:waechst_nach', ['biomasse_block', 'minecraft:nether_wart_block', 'minecraft:moss_block',
                                        'minecraft:mud', 'minecraft:packed_mud', 'minecraft:mycelium', 'minecraft:rooted_dirt',
                                        'minecraft:dirt', 'minecraft:dripstone_block', 'minecraft:stone', 'minecraft:deepslate',
                                        'minecraft:clay', 'minecraft:shroomlight', 'minecraft:bone_block'])
    tag('item', f'{NS}:biomasse', ['biomasse_pulpe', '#minecraft:saplings', '#minecraft:leaves', '#minecraft:flowers',
                                   'minecraft:wheat', 'minecraft:carrot', 'minecraft:potato', 'minecraft:beetroot',
                                   'minecraft:kelp', 'minecraft:sugar_cane', 'minecraft:short_grass', 'minecraft:fern',
                                   'minecraft:vine', 'minecraft:melon_slice', 'minecraft:pumpkin', 'minecraft:cactus',
                                   'minecraft:bamboo', 'minecraft:sweet_berries'])
    # Kompat-Zutat: Im Pack trägt KubeJS z. B. den Source-Gem aus Ars Nouveau ein
    tag('item', f'{NS}:compat/stabilisator', ['flux_stabilisator'])
    tag('item', 'c:glass_blocks', ['chronoglas'])
    tag('block', 'c:glass_blocks', ['chronoglas'])
    tag('damage_type', 'minecraft:no_knockback', [f'{NS}:fressende_flora'])


def montierer(path, steps, out, count=1, time=200):
    """Präzisions-Montierer: Schritt i gehört in Slot i (Reihenfolge zählt)."""
    machine_recipe2('praezisions_montierer', path, steps, out, count, time=time)


def m3_recipes():
    # --- Garagen-Werkbank: Netz, Gehäuse, Bauteile -----------------------------------
    werkbank_shaped('flux_leitung', ['GGG', 'FFF', 'GGG'], {'G': '#c:wires/gold', 'F': '#c:dusts/fluxkristall'}, 'flux_leitung', 6)
    werkbank_shapeless('flux_leitung_isoliert', ['flux_leitung', 'isolierfolie'], 'flux_leitung_isoliert')
    werkbank_shaped('labor_huelle', ['ISI', 'S S', 'ISI'], {'I': '#c:plates/iron', 'S': '#c:plates/steel'}, 'labor_huelle', 4, hammer=1)
    werkbank_shaped('labor_gehaeuse', ['SVS', 'VXV', 'SVS'],
                    {'S': '#c:plates/steel', 'V': 'verbundplatte', 'X': 'xenit_gehaeuse'}, 'labor_gehaeuse', hammer=1)
    werkbank_shaped('flux_spule', ['GFG', 'GXG', 'GFG'],
                    {'G': '#c:wires/gold', 'F': '#c:dusts/fluxkristall', 'X': '#c:rods/xenit'}, 'flux_spule', loetzinn=1)
    werkbank_shaped('umspanner', ['IKI', 'SXT', 'IFI'],
                    {'I': '#c:plates/iron', 'K': 'kupferkabel_isoliert', 'F': 'flux_leitung_isoliert', 'S': 'xenit_spule',
                     'T': 'flux_spule', 'X': 'xenit_gehaeuse'}, 'umspanner', loetzinn=2)
    werkbank_shaped('flux_zelle', ['FTF', 'XLX', 'FRF'],
                    {'F': '#c:gems/fluxkristall', 'T': 'flux_spule', 'X': 'xenit_zelle', 'L': 'labor_gehaeuse',
                     'R': 'rift_schaltkreis'}, 'flux_zelle', loetzinn=2)
    werkbank_shaped('fluxkristall_stab', [' F ', 'HXH', ' F '],
                    {'F': '#c:gems/fluxkristall', 'H': 'zinn_huelse', 'X': '#c:rods/xenit'}, 'fluxkristall_stab', loetzinn=1)
    werkbank_shaped('kuehlkoerper', ['KZK', 'ZKZ', 'KZK'], {'K': '#c:plates/copper', 'Z': '#c:plates/tin'}, 'kuehlkoerper', 2, hammer=1)
    werkbank_shaped('universumsprobe', [' G ', 'GFG', ' X '],
                    {'G': '#c:glass_panes', 'F': '#c:dusts/fluxkristall', 'X': '#c:plates/xenit'}, 'universumsprobe', 4)
    werkbank_shaped('chronoglas_griff', ['CXC', ' X ', ' P '],
                    {'C': 'chronoglas_scheibe', 'X': '#c:rods/xenit', 'P': 'polymerfetzen'}, 'chronoglas_griff', hammer=1)
    werkbank_shaped('schwebstein_kern', ['SAS', 'AXA', 'SAS'],
                    {'S': 'schwebstein', 'A': 'antigrav_platte', 'X': '#c:storage_blocks/iron'}, 'schwebstein_kern', hammer=2)
    werkbank_shaped('resonator_kammer', ['PHP', 'HKH', 'PHP'],
                    {'P': '#c:plates/steel', 'H': 'labor_huelle', 'K': 'kuehlkoerper'}, 'resonator_kammer', hammer=1)

    # --- Garagen-Werkbank: Maschinen und Generatoren -------------------------------------
    # Montierer und Zentrifuge stellen die Rift-Schaltkreise erst her, also kommen sie mit Basis-Schaltkreisen aus
    werkbank_shaped('praezisions_montierer', ['PMP', 'BLB', 'TXT'],
                    {'P': '#c:plates/steel', 'M': 'mini_motor', 'B': 'basis_schaltkreis', 'L': 'labor_gehaeuse',
                     'T': 'flux_spule', 'X': 'minecraft:piston'}, 'praezisions_montierer', loetzinn=3)
    werkbank_shaped('quantenzentrifuge', ['GMG', 'BLB', 'TFT'],
                    {'G': '#c:glass_blocks', 'M': 'mini_motor', 'B': 'basis_schaltkreis', 'L': 'labor_gehaeuse',
                     'T': 'flux_spule', 'F': 'fluxlinse'}, 'quantenzentrifuge', loetzinn=3)
    werkbank_shaped('gravitationspresse', ['SAS', 'RLR', 'TPT'],
                    {'S': '#c:plates/steel', 'A': 'minecraft:anvil', 'R': 'rift_schaltkreis', 'L': 'labor_gehaeuse',
                     'T': 'flux_spule', 'P': 'minecraft:piston'}, 'gravitationspresse', loetzinn=3)
    werkbank_shaped('dimensions_scanner', ['KUK', 'RLR', 'TCT'],
                    {'K': 'stabilisierte_linse', 'U': 'universe_scanner', 'R': 'rift_schaltkreis', 'L': 'labor_gehaeuse',
                     'T': 'flux_spule', 'C': 'minecraft:cartography_table'}, 'dimensions_scanner', loetzinn=3)
    werkbank_shaped('chrono_beschleuniger', ['CUC', 'RLR', 'TCT'],
                    {'C': 'chronoglas_scheibe', 'U': 'minecraft:clock', 'R': 'rift_schaltkreis', 'L': 'labor_gehaeuse',
                     'T': 'flux_spule'}, 'chrono_beschleuniger', loetzinn=3)
    werkbank_shaped('portalfluid_raffinerie', ['GKG', 'RLR', 'TMT'],
                    {'G': '#c:glass_blocks', 'K': 'minecraft:cauldron', 'R': 'rift_schaltkreis', 'L': 'labor_gehaeuse',
                     'T': 'flux_spule', 'M': 'fluid_mixer'}, 'portalfluid_raffinerie', loetzinn=3)
    werkbank_shaped('biomasse_kessel', ['ICI', 'FXF', 'BSB'],
                    {'I': '#c:plates/iron', 'C': 'minecraft:composter', 'F': 'minecraft:furnace', 'X': 'xenit_gehaeuse',
                     'B': 'basis_schaltkreis', 'S': 'xenit_spule'}, 'biomasse_kessel', loetzinn=2)
    werkbank_shaped('zeitschleifen_solar', ['CCC', 'RLR', 'TDT'],
                    {'C': 'chronoglas_scheibe', 'R': 'rift_schaltkreis', 'L': 'labor_gehaeuse', 'T': 'flux_spule',
                     'D': 'minecraft:daylight_detector'}, 'zeitschleifen_solar', loetzinn=2)
    werkbank_shaped('fluxkristall_resonator', ['FKF', 'RLR', 'TBT'],
                    {'F': 'fluxkristall_block', 'K': 'stabilisierte_linse', 'R': 'rift_schaltkreis', 'L': 'labor_gehaeuse',
                     'T': 'flux_spule', 'B': 'minecraft:bucket'}, 'fluxkristall_resonator', loetzinn=3)
    werkbank_shaped('gravitations_pendel', ['AUA', 'RLR', 'TST'],
                    {'A': 'antigrav_platte', 'U': 'minecraft:clock', 'R': 'rift_schaltkreis', 'L': 'labor_gehaeuse',
                     'T': 'flux_spule', 'S': '#c:plates/steel'}, 'gravitations_pendel', loetzinn=3)

    # --- Normale Werkbank ---------------------------------------------------------------
    shaped_vanilla('chronoglas', ['CC', 'CC'], {'C': 'chronoglas_scheibe'}, 'chronoglas')

    # --- Maschinen ------------------------------------------------------------------------
    machine_recipe('garagenpresse', 'verbundplatte',
                   [('mischmetall', 1), ('#c:dusts/xenit', 1), ('#c:dusts/fluxkristall', 1)], 'verbundplatte', time=60)
    machine_recipe2('xenit_schmelze', 'chronoglas_scheibe', [('chronoglas_sand', 1)], 'chronoglas_scheibe', 2, time=120)
    machine_recipe2('erzmuehle', 'schwebstein', [('schwebstein', 1)], 'schwebstein_brocken', 4, time=120)
    machine_recipe2('quantenzentrifuge', 'saeuregel', [('saeuregel', 1)], 'aetzloesung', 1, time=200,
                    byproducts=[('toxinrest', 1, 0.5)])
    machine_recipe2('quantenzentrifuge', 'biomasse_pulpe', [('biomasse_pulpe', 2)], 'naehrserum', 1, time=160,
                    byproducts=[('polymerfetzen', 1, 0.3), ('minecraft:bone_meal', 1, 0.5)])
    machine_recipe2('quantenzentrifuge', 'flux_stabilisator', [('#c:gems/fluxkristall', 1), ('saeuregel', 1)],
                    'flux_stabilisator', 1, time=300, byproducts=[('toxinrest', 1, 0.5)])
    machine_recipe2('quantenzentrifuge', 'toxinrest', [('toxinrest', 4)], 'minecraft:slime_ball', 1, time=200,
                    byproducts=[('saeuregel', 1, 0.25)])
    machine_recipe2('gravitationspresse', 'antigrav_platte', [('schwebstein_brocken', 2)], 'antigrav_platte', 1, time=200)
    machine_recipe2('dimensions_scanner', 'koordinatenkarte', [('universumsprobe', 1), ('minecraft:paper', 1)],
                    'koordinatenkarte', 1, time=300)
    # Präzisions-Montierer: die Reihenfolge der Zutaten ist die Reihenfolge der Slots
    montierer('rift_schaltkreis', [('basis_schaltkreis', 1), ('#c:wires/gold', 2), ('aetzloesung', 1),
                                   ('#c:dusts/fluxkristall', 1)], 'rift_schaltkreis', time=240)
    montierer('stabilisierte_linse', [('fluxlinse', 1), (f'#{NS}:compat/stabilisator', 1), ('#c:wires/gold', 2)],
              'stabilisierte_linse', time=200)
    montierer('kanonenkern', [('rift_schaltkreis', 2), ('antigrav_platte', 1), ('stabilisierte_linse', 1)], 'kanonenkern', time=300)
    montierer('portal_kanone_mk2', [('portal_kanone_mk1', 1), ('kanonenkern', 1), ('chronoglas_griff', 1),
                                    ('verbundplatte', 2), ('flux_spule', 1)], 'portal_kanone_mk2', time=400)
    recipe('raffinerie/portalfluid', {
        'type': f'{NS}:raffinerie',
        'ingredients': [{'ingredient': ing('saeuregel')}, {'ingredient': ing('#c:dusts/fluxkristall')}],
        'fluid': {'fluid': rl('roh_portalfluid'), 'amount': 1000},
        'result': {'id': rl('portalfluid'), 'amount': 1000},
        'time': 300})


M3_UNIVERSES = {
    'niedriggravitation': (['schwebe_himmel', 'schwebe_daemmerung'], None, 0.0),
    'zeitdilatation': (['zeit_bernstein', 'zeit_violett'], None, 0.05),
    'organische_welt': (['organisch_rosa', 'organisch_purpur'], None, 0.0),
}


def m3_universes():
    sky_monsters = [spawn('minecraft:phantom', 20, 1, 2), spawn('minecraft:skeleton', 40, 1, 2), spawn('minecraft:spider', 30, 1, 2)]
    time_monsters = [spawn('minecraft:zombie', 40, 2, 3), spawn('minecraft:husk', 30, 1, 2), spawn('minecraft:stray', 20, 1, 2)]
    flesh_monsters = [spawn('minecraft:slime', 50, 1, 3), spawn('minecraft:silverfish', 20, 2, 4), spawn('minecraft:zombie', 30, 2, 3)]
    sky_animals = [spawn('minecraft:sheep', 8, 2, 3), spawn('minecraft:chicken', 8, 2, 4)]
    time_animals = [spawn('minecraft:rabbit', 8, 2, 3), spawn('minecraft:bee', 4, 1, 2), spawn('minecraft:cow', 6, 2, 3)]
    flesh_animals = [spawn('minecraft:pig', 6, 2, 3), spawn('minecraft:mooshroom', 4, 1, 2)]
    biome('schwebe_himmel', True, {'sky_color': 0x9AD0FF, 'fog_color': 0xE6F4FF, 'water_color': 0x6AB8FF,
                                   'water_fog_color': 0x2A6AB0, 'grass_color': 0x9ADC8A, 'foliage_color': 0x7AC070},
          sky_monsters, sky_animals)
    biome('schwebe_daemmerung', True, {'sky_color': 0xC8A8E8, 'fog_color': 0xF0DCF4, 'water_color': 0x8A9AE0,
                                       'water_fog_color': 0x4A50A0, 'grass_color': 0xA8D0A0, 'foliage_color': 0x88B088},
          sky_monsters, sky_animals)
    biome('zeit_bernstein', False, {'sky_color': 0xE8C080, 'fog_color': 0xF0D8A8, 'water_color': 0xC8A060,
                                    'water_fog_color': 0x806030, 'grass_color': 0xC8C060, 'foliage_color': 0xB0A048},
          time_monsters, time_animals, temperature=1.2)
    biome('zeit_violett', False, {'sky_color': 0xA890E8, 'fog_color': 0xD8C8F8, 'water_color': 0x9A6BFF,
                                  'water_fog_color': 0x5A3AA8, 'grass_color': 0x9AB0D0, 'foliage_color': 0x8A90C0},
          time_monsters, time_animals)
    biome('organisch_rosa', True, {'sky_color': 0xE8A0A8, 'fog_color': 0xE8B8B8, 'water_color': 0xD87080,
                                   'water_fog_color': 0x803040, 'grass_color': 0xC07070, 'foliage_color': 0xA05060},
          flesh_monsters, flesh_animals)
    biome('organisch_purpur', True, {'sky_color': 0xB878A8, 'fog_color': 0xC890B0, 'water_color': 0xA05080,
                                     'water_fog_color': 0x602040, 'grass_color': 0x985878, 'foliage_color': 0x804060},
          flesh_monsters, flesh_animals)

    for dim, (biomes, fixed_time, ambient) in M3_UNIVERSES.items():
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

    write(os.path.join(DATA, NS, 'damage_type', 'fressende_flora.json'), {
        'exhaustion': 0.1, 'message_id': f'{NS}.fressende_flora', 'scaling': 'never'})


M3_DE = {
    'block.riftverse.flux_leitung': 'Flux-Leitung', 'block.riftverse.flux_leitung_isoliert': 'Isolierte Flux-Leitung',
    'block.riftverse.umspanner': 'Umspanner (I ↔ II)', 'block.riftverse.flux_zelle': 'Flux-Zelle',
    'block.riftverse.labor_gehaeuse': 'Labor-Gehäuse', 'block.riftverse.labor_huelle': 'Labor-Hülle',
    'block.riftverse.praezisions_montierer': 'Präzisions-Montierer', 'block.riftverse.quantenzentrifuge': 'Quantenzentrifuge',
    'block.riftverse.gravitationspresse': 'Gravitationspresse', 'block.riftverse.dimensions_scanner': 'Dimensions-Scanner',
    'block.riftverse.chrono_beschleuniger': 'Chrono-Beschleuniger', 'block.riftverse.portalfluid_raffinerie': 'Portalfluid-Raffinerie',
    'block.riftverse.biomasse_kessel': 'Biomasse-Kessel', 'block.riftverse.zeitschleifen_solar': 'Zeitschleifen-Solarpanel',
    'block.riftverse.fluxkristall_resonator': 'Fluxkristall-Resonator', 'block.riftverse.resonator_kammer': 'Resonator-Kammer',
    'block.riftverse.gravitations_pendel': 'Gravitations-Pendel', 'block.riftverse.schwebstein_kern': 'Schwebstein-Kern',
    'block.riftverse.schwebstein': 'Schwebstein', 'block.riftverse.chronoglas_sand': 'Chronoglas-Sand',
    'block.riftverse.chronoglas': 'Chronoglas', 'block.riftverse.biomasse_block': 'Biomasse',
    'block.riftverse.fressende_flora': 'Fressende Flora',
    'item.riftverse.portal_kanone_mk2': 'Portal-Kanone Mk II',
    'item.riftverse.fluid_kartusche_stabil': 'Fluid-Kartusche (stabil)',
    'item.riftverse.fluid_kartusche_stabil.lore': '1.000 mB stabilisiertes Portalfluid – zerfällt nicht',
    'item.riftverse.universumsprobe': 'Universumsprobe', 'item.riftverse.koordinatenkarte': 'Koordinatenkarte',
    'item.riftverse.verbundplatte': 'Verbundplatte', 'item.riftverse.flux_spule': 'Flux-Spule',
    'item.riftverse.rift_schaltkreis': 'Rift-Schaltkreis', 'item.riftverse.stabilisierte_linse': 'Stabilisierte Linse',
    'item.riftverse.kanonenkern': 'Kanonenkern', 'item.riftverse.chronoglas_griff': 'Chronoglas-Griff',
    'item.riftverse.chronoglas_scheibe': 'Chronoglas-Scheibe', 'item.riftverse.antigrav_platte': 'Antigrav-Platte',
    'item.riftverse.aetzloesung': 'Ätzlösung', 'item.riftverse.toxinrest': 'Toxinrest',
    'item.riftverse.flux_stabilisator': 'Flux-Stabilisator',
    'item.riftverse.flux_stabilisator.lore': 'Stabilisierungszutat für Linsen (im Pack auch Source-Gem)',
    'item.riftverse.naehrserum': 'Nährserum', 'item.riftverse.naehrserum.lore': 'Wird später im Klon-Tank gebraucht.',
    'item.riftverse.fluxkristall_stab': 'Fluxkristall-Stab', 'item.riftverse.kuehlkoerper': 'Kühlkörper',
    'item.riftverse.schwebstein_brocken': 'Schwebstein-Brocken', 'item.riftverse.biomasse_pulpe': 'Biomasse-Pulpe',
    'fluid_type.riftverse.portalfluid': 'Stabilisiertes Portalfluid',
    'universe.riftverse.niedriggravitation': 'Niedriggravitationswelt', 'universe.riftverse.zeitdilatation': 'Zeitdilatationswelt',
    'universe.riftverse.organische_welt': 'Organische Welt',
    'dimension.riftverse.niedriggravitation': 'Niedriggravitationswelten', 'dimension.riftverse.zeitdilatation': 'Zeitdilatationswelten',
    'dimension.riftverse.organische_welt': 'Organische Welten',
    'biome.riftverse.schwebe_himmel': 'Schwebehimmel', 'biome.riftverse.schwebe_daemmerung': 'Schwebedämmerung',
    'biome.riftverse.zeit_bernstein': 'Bernsteinzeit', 'biome.riftverse.zeit_violett': 'Violette Ewigkeit',
    'biome.riftverse.organisch_rosa': 'Fleischgarten', 'biome.riftverse.organisch_purpur': 'Purpurgewebe',
    'gui.riftverse.serum': 'Nährserum', 'gui.riftverse.lichtschuld': 'Lichtschuld', 'gui.riftverse.pendel': 'Pendel',
    'gui.riftverse.struktur_unvollstaendig': 'Struktur unvollständig!', 'gui.riftverse.schritt': 'Schritt %s/%s',
    'tooltip.riftverse.kanone_mk1': 'Mk I: zufällige Sprünge – Roh-Portalfluid: T1-Welten, stabiles Portalfluid: auch T2-Welten',
    'tooltip.riftverse.kanone_mk2': 'Mk II: nur stabiles Portalfluid, alle T1- und T2-Welten',
    'tooltip.riftverse.kanone_karte': 'Koordinatenkarte in der anderen Hand: gezielter Sprung',
    'tooltip.riftverse.kanone_tank_stabil': 'Stabiles Portalfluid: %s / %s mB (%s mB pro Schuss)',
    'tooltip.riftverse.flux_zelle': 'Stufe II · behält die Ladung beim Abbauen',
    'tooltip.riftverse.kessel_fuel': 'Verbrennt Biomasse-Pulpe (lange) oder Pflanzen',
    'tooltip.riftverse.solar_kein_himmel': 'Kein freier Himmel über dem Panel!',
    'tooltip.riftverse.solar_schuld': 'Lichtschuld: %s %% einer Nacht',
    'tooltip.riftverse.solar_regel': 'Nachts volle Leistung auf Pump, tagsüber halbe Leistung, bis die Schuld bezahlt ist',
    'tooltip.riftverse.multiblock_unvollstaendig': 'Struktur unvollständig',
    'tooltip.riftverse.multiblock_rechtsklick': 'Rechtsklick auf den Steuerblock nennt das fehlende Teil',
    'tooltip.riftverse.pendel_faktor': 'Gravitationsfaktor: %s×',
    'tooltip.riftverse.hitze': 'Hitze: %s / %s', 'tooltip.riftverse.hitze_delta': 'Änderung: %s pro Tick',
    'tooltip.riftverse.resonator_regel': 'Nachbarstäbe schwingen mit; Kühlkörper führen 12 Hitze/t ab; Redstone = Notabschaltung',
    'tooltip.riftverse.resonator_wasser': 'Mit Wasser gekühlt: 1,5-fache Leistung',
    'tooltip.riftverse.montierer_reihenfolge': 'Schritt 1 links oben, dann zeilenweise – die Reihenfolge zählt',
    'tooltip.riftverse.probe_leer': 'Rechtsklick in einem Universum füllt die Probe',
    'tooltip.riftverse.probe_scanner': 'Der Dimensions-Scanner macht daraus eine Koordinatenkarte',
    'tooltip.riftverse.karte_leer': 'Leer',
    'tooltip.riftverse.karte_benutzen': 'In die andere Hand nehmen und mit der Kanone ab Mk II schießen',
    'message.riftverse.umspanner.runter': 'Umspanner: %s / %s FE · spannt herunter: Front %4$s → Seiten %3$s',
    'message.riftverse.umspanner.hoch': 'Umspanner: %s / %s FE · spannt hoch (Redstone): Seiten %3$s → Front %4$s',
    'message.riftverse.multiblock.fehlt': 'Es fehlt: %s bei %s %s %s',
    'message.riftverse.chrono.aktiv': 'Chrono-Beschleuniger: %s× · %s FE/t · Puffer %s FE',
    'message.riftverse.chrono.leerlauf': 'Chrono-Beschleuniger: %s× · %s FE/t · Puffer %s FE · keine Maschine dahinter oder zu wenig Strom',
    'message.riftverse.schon_dort': 'Du bist schon in diesem Universum.',
    'message.riftverse.rift_ziel': 'Rift geöffnet → %s (%s)',
    'message.riftverse.karte_zu_weit': 'Diese Kanone erreicht keine %s.',
    'message.riftverse.kein_fluid_stabil': 'Kein stabilisiertes Portalfluid! Fülle Kartuschen in der Portalfluid-Raffinerie.',
    'message.riftverse.zeitverzerrung': '⌛ Zeitverzerrung!',
    'message.riftverse.probe_heimat': 'Hier gibt es keine Universums-Signatur – nur in fremden Universen.',
    'message.riftverse.probe_gefuellt': 'Probe gefüllt: %s',
    'death.attack.riftverse.fressende_flora': '%1$s wurde von fressender Flora verdaut',
    'death.attack.riftverse.fressende_flora.player': '%1$s wurde auf der Flucht vor %2$s von fressender Flora verdaut',
}

M3_EN = {
    'block.riftverse.flux_leitung': 'Flux Line', 'block.riftverse.flux_leitung_isoliert': 'Insulated Flux Line',
    'block.riftverse.umspanner': 'Transformer (I ↔ II)', 'block.riftverse.flux_zelle': 'Flux Cell',
    'block.riftverse.labor_gehaeuse': 'Lab Casing', 'block.riftverse.labor_huelle': 'Lab Hull',
    'block.riftverse.praezisions_montierer': 'Precision Assembler', 'block.riftverse.quantenzentrifuge': 'Quantum Centrifuge',
    'block.riftverse.gravitationspresse': 'Gravity Press', 'block.riftverse.dimensions_scanner': 'Dimension Scanner',
    'block.riftverse.chrono_beschleuniger': 'Chrono Accelerator', 'block.riftverse.portalfluid_raffinerie': 'Portal Fluid Refinery',
    'block.riftverse.biomasse_kessel': 'Biomass Boiler', 'block.riftverse.zeitschleifen_solar': 'Time Loop Solar Panel',
    'block.riftverse.fluxkristall_resonator': 'Flux Crystal Resonator', 'block.riftverse.resonator_kammer': 'Resonator Chamber',
    'block.riftverse.gravitations_pendel': 'Gravity Pendulum', 'block.riftverse.schwebstein_kern': 'Floatstone Core',
    'block.riftverse.schwebstein': 'Floatstone', 'block.riftverse.chronoglas_sand': 'Chronoglass Sand',
    'block.riftverse.chronoglas': 'Chronoglass', 'block.riftverse.biomasse_block': 'Biomass',
    'block.riftverse.fressende_flora': 'Devouring Flora',
    'item.riftverse.portal_kanone_mk2': 'Portal Gun Mk II',
    'item.riftverse.fluid_kartusche_stabil': 'Fluid Cartridge (stable)',
    'item.riftverse.fluid_kartusche_stabil.lore': '1,000 mB stabilized portal fluid – does not decay',
    'item.riftverse.universumsprobe': 'Universe Sample', 'item.riftverse.koordinatenkarte': 'Coordinate Map',
    'item.riftverse.verbundplatte': 'Composite Plate', 'item.riftverse.flux_spule': 'Flux Coil',
    'item.riftverse.rift_schaltkreis': 'Rift Circuit', 'item.riftverse.stabilisierte_linse': 'Stabilized Lens',
    'item.riftverse.kanonenkern': 'Gun Core', 'item.riftverse.chronoglas_griff': 'Chronoglass Grip',
    'item.riftverse.chronoglas_scheibe': 'Chronoglass Pane', 'item.riftverse.antigrav_platte': 'Antigrav Plate',
    'item.riftverse.aetzloesung': 'Etching Solution', 'item.riftverse.toxinrest': 'Toxin Residue',
    'item.riftverse.flux_stabilisator': 'Flux Stabilizer',
    'item.riftverse.flux_stabilisator.lore': 'Stabilizing ingredient for lenses (in the pack also Source Gems)',
    'item.riftverse.naehrserum': 'Nutrient Serum', 'item.riftverse.naehrserum.lore': 'Needed later for the clone tank.',
    'item.riftverse.fluxkristall_stab': 'Flux Crystal Rod', 'item.riftverse.kuehlkoerper': 'Heat Sink',
    'item.riftverse.schwebstein_brocken': 'Floatstone Chunk', 'item.riftverse.biomasse_pulpe': 'Biomass Pulp',
    'fluid_type.riftverse.portalfluid': 'Stabilized Portal Fluid',
    'universe.riftverse.niedriggravitation': 'Low Gravity World', 'universe.riftverse.zeitdilatation': 'Time Dilation World',
    'universe.riftverse.organische_welt': 'Organic World',
    'dimension.riftverse.niedriggravitation': 'Low Gravity Worlds', 'dimension.riftverse.zeitdilatation': 'Time Dilation Worlds',
    'dimension.riftverse.organische_welt': 'Organic Worlds',
    'biome.riftverse.schwebe_himmel': 'Floating Sky', 'biome.riftverse.schwebe_daemmerung': 'Floating Dusk',
    'biome.riftverse.zeit_bernstein': 'Amber Time', 'biome.riftverse.zeit_violett': 'Violet Eternity',
    'biome.riftverse.organisch_rosa': 'Flesh Garden', 'biome.riftverse.organisch_purpur': 'Purple Tissue',
    'gui.riftverse.serum': 'Nutrient serum', 'gui.riftverse.lichtschuld': 'Light debt', 'gui.riftverse.pendel': 'Pendulum',
    'gui.riftverse.struktur_unvollstaendig': 'Structure incomplete!', 'gui.riftverse.schritt': 'Step %s/%s',
    'tooltip.riftverse.kanone_mk1': 'Mk I: random jumps – raw portal fluid: T1 worlds, stable portal fluid: T2 worlds too',
    'tooltip.riftverse.kanone_mk2': 'Mk II: stable portal fluid only, all T1 and T2 worlds',
    'tooltip.riftverse.kanone_karte': 'Coordinate map in the other hand: targeted jump',
    'tooltip.riftverse.kanone_tank_stabil': 'Stable portal fluid: %s / %s mB (%s mB per shot)',
    'tooltip.riftverse.flux_zelle': 'Tier II · keeps its charge when mined',
    'tooltip.riftverse.kessel_fuel': 'Burns biomass pulp (long) or plants',
    'tooltip.riftverse.solar_kein_himmel': 'No open sky above the panel!',
    'tooltip.riftverse.solar_schuld': 'Light debt: %s %% of a night',
    'tooltip.riftverse.solar_regel': 'Full output at night on credit, half output by day until the debt is paid',
    'tooltip.riftverse.multiblock_unvollstaendig': 'Structure incomplete',
    'tooltip.riftverse.multiblock_rechtsklick': 'Right-click the controller to see the missing part',
    'tooltip.riftverse.pendel_faktor': 'Gravity factor: %s×',
    'tooltip.riftverse.hitze': 'Heat: %s / %s', 'tooltip.riftverse.hitze_delta': 'Change: %s per tick',
    'tooltip.riftverse.resonator_regel': 'Neighbouring rods resonate; heat sinks remove 12 heat/t; redstone = emergency stop',
    'tooltip.riftverse.resonator_wasser': 'Water cooled: 1.5× output',
    'tooltip.riftverse.montierer_reihenfolge': 'Step 1 top left, then row by row – order matters',
    'tooltip.riftverse.probe_leer': 'Right-click inside a universe to fill the sample',
    'tooltip.riftverse.probe_scanner': 'The Dimension Scanner turns it into a coordinate map',
    'tooltip.riftverse.karte_leer': 'Empty',
    'tooltip.riftverse.karte_benutzen': 'Hold it in your other hand and fire a Mk II gun or better',
    'message.riftverse.umspanner.runter': 'Transformer: %s / %s FE · stepping down: front %4$s → sides %3$s',
    'message.riftverse.umspanner.hoch': 'Transformer: %s / %s FE · stepping up (redstone): sides %3$s → front %4$s',
    'message.riftverse.multiblock.fehlt': 'Missing: %s at %s %s %s',
    'message.riftverse.chrono.aktiv': 'Chrono Accelerator: %s× · %s FE/t · buffer %s FE',
    'message.riftverse.chrono.leerlauf': 'Chrono Accelerator: %s× · %s FE/t · buffer %s FE · no machine behind it or not enough power',
    'message.riftverse.schon_dort': 'You are already in this universe.',
    'message.riftverse.rift_ziel': 'Rift opened → %s (%s)',
    'message.riftverse.karte_zu_weit': 'This gun cannot reach a %s.',
    'message.riftverse.kein_fluid_stabil': 'No stabilized portal fluid! Fill cartridges in the Portal Fluid Refinery.',
    'message.riftverse.zeitverzerrung': '⌛ Time distortion!',
    'message.riftverse.probe_heimat': 'No universe signature here – only in foreign universes.',
    'message.riftverse.probe_gefuellt': 'Sample filled: %s',
    'death.attack.riftverse.fressende_flora': '%1$s was digested by devouring flora',
    'death.attack.riftverse.fressende_flora.player': '%1$s was digested by devouring flora while fleeing %2$s',
}


# =============================================================================
# Meilenstein 4 – T3 Multiversum-Industrie
# =============================================================================
M4_MACHINES = ('quanten_lithograf', 'materie_rekonfigurator', 'universums_destillator')
M4_BLOCKS_SELF = M4_MACHINES + ('umspanner_ii', 'umspanner_iii', 'graphit_gehaeuse', 'graphit_huelle', 'rift_kabelknoten',
                                'mikrokosmos_zelle', 'parallelzapfer', 'gravitit_leitung', 'gravitit_leitung_isoliert',
                                'gravitit_leitung_doppelt')
M4_ITEMS = ['quanten_schaltkreis', 'chrono_linse', 'gravitit_spule', 'gravitit_rahmen', 'graphitplatte', 'fragment_replik',
            'gravitit', 'inversionssplitter', 'fluid_kartusche_konzentrat', 'kopplungskarte']
M4_GRAPHIT = (*M4_MACHINES, 'umspanner_ii', 'umspanner_iii', 'graphit_gehaeuse', 'graphit_huelle', 'rift_kabelknoten',
              'mikrokosmos_zelle', 'parallelzapfer', 'chrono_speicher', 'klon_tank')


def umspanner(name):
    """Umspanner wie in Meilenstein 3, mit eigenen Texturen je Stufe."""
    for suffix in ('', '_hoch'):
        side = tex(name + '_side')
        block_model(name + suffix, 'minecraft:block/cube', {
            'particle': side, 'north': tex(name + '_front' + suffix), 'south': side, 'east': side, 'west': side,
            'up': side, 'down': side})
    variants = {}
    for facing, (x, y) in ALL_DIRS.items():
        for powered in ('false', 'true'):
            for active in ('false', 'true'):
                variants[f'active={active},facing={facing},powered={powered}'] = variant(
                    name + ('_hoch' if powered == 'true' else ''), x=x, y=y)
    blockstate(name, variants)
    item_from_block(name)


def m4_models():
    cable('gravitit_leitung', 6, False)
    cable('gravitit_leitung_isoliert', 8, True)
    cable('gravitit_leitung_doppelt', 10, True)
    umspanner('umspanner_ii')
    umspanner('umspanner_iii')

    # Chrono-Speicher: Graphit-Deckel, Seiten mit Uhrwerk-Band
    for suffix in ('', '_on'):
        block_model('chrono_speicher' + suffix, 'minecraft:block/cube_bottom_top', {
            'top': tex('casing_t3_top'), 'bottom': tex('casing_t3_top'), 'side': tex('chrono_speicher_side' + suffix)})
    blockstate('chrono_speicher', {f'active={a},facing={f}': variant('chrono_speicher' + ('_on' if a == 'true' else ''))
                                   for f in HORIZONTAL for a in ('false', 'true')})
    item_from_block('chrono_speicher')

    casing_block('graphit_gehaeuse', 3)
    for n in ('graphit_huelle', 'gravitit_ore', 'spiegelkristall'):
        cube_all(n)
    for n in M4_MACHINES:
        machine(n, 3)

    # Parallelzapfer: Antennen-Deckel
    for suffix in ('', '_on'):
        block_model('parallelzapfer' + suffix, 'minecraft:block/orientable', {
            'top': tex('parallelzapfer_top'), 'side': tex('casing_t3'), 'front': tex('parallelzapfer_front' + suffix)})
    blockstate('parallelzapfer', {f'active={a},facing={f}': variant('parallelzapfer' + ('_on' if a == 'true' else ''), y=y)
                                  for f, y in HORIZONTAL.items() for a in ('false', 'true')})
    item_from_block('parallelzapfer')

    # Mikrokosmos-Zelle: Glasdeckel mit der Stadt, die Lichter gehen an, wenn sie Strom macht
    for suffix in ('', '_on'):
        block_model('mikrokosmos_zelle' + suffix, 'minecraft:block/cube_bottom_top', {
            'top': tex('mikrokosmos_zelle_top' + suffix), 'side': tex('mikrokosmos_zelle_side'), 'bottom': tex('casing_t3')})
    blockstate('mikrokosmos_zelle', {f'active={a},facing={f}': variant('mikrokosmos_zelle' + ('_on' if a == 'true' else ''), y=y)
                                     for f, y in HORIZONTAL.items() for a in ('false', 'true')})
    item_from_block('mikrokosmos_zelle')

    # Rift-Kabelknoten: Antennenmast
    for suffix in ('', '_on'):
        block_model('rift_kabelknoten' + suffix, 'minecraft:block/cube_column', {
            'end': tex('rift_kabelknoten_top' + suffix), 'side': tex('rift_kabelknoten_side')})
    blockstate('rift_kabelknoten', {f'active={a},facing={f}': variant('rift_kabelknoten' + ('_on' if a == 'true' else ''))
                                    for f in HORIZONTAL for a in ('false', 'true')})
    item_from_block('rift_kabelknoten')

    # Klon-Tank: zwei Blöcke hoch, durchscheinend
    for half in ('bottom', 'top'):
        for suffix in ('', '_on'):
            block_model(f'klon_tank_{half}{suffix}', 'minecraft:block/cube_column', {
                'end': tex('casing_t3_top'), 'side': tex(f'klon_tank_{half}{suffix}')}, render_type='minecraft:translucent')
    variants = {}
    for facing, y in HORIZONTAL.items():
        for active in ('false', 'true'):
            for half, model in (('lower', 'bottom'), ('upper', 'top')):
                variants[f'active={active},facing={facing},half={half}'] = variant(
                    f'klon_tank_{model}' + ('_on' if active == 'true' else ''), y=y)
    blockstate('klon_tank', variants)
    item_generated('klon_tank')

    for n in M4_ITEMS:
        item_generated(n)
    item_generated('portal_kanone_mk3', handheld=True)


def m4_loot():
    for n in M4_BLOCKS_SELF:
        loot_self(n)
    write(os.path.join(DATA, NS, 'loot_table', 'blocks', 'chrono_speicher.json'), {
        'type': 'minecraft:block',
        'pools': [{'rolls': 1, 'bonus_rolls': 0,
                   'entries': [{'type': 'minecraft:item', 'name': rl('chrono_speicher'), 'functions': [
                       {'function': 'minecraft:copy_components', 'source': 'block_entity', 'include': [f'{NS}:energy']}]}],
                   'conditions': [{'condition': 'minecraft:survives_explosion'}]}],
        'random_sequence': f'{NS}:blocks/chrono_speicher'})
    # Klon-Tank: nur die untere Hälfte lässt etwas fallen
    write(os.path.join(DATA, NS, 'loot_table', 'blocks', 'klon_tank.json'), {
        'type': 'minecraft:block',
        'pools': [{'rolls': 1, 'bonus_rolls': 0,
                   'entries': [{'type': 'minecraft:item', 'name': rl('klon_tank')}],
                   'conditions': [{'condition': 'minecraft:block_state_property', 'block': rl('klon_tank'),
                                   'properties': {'half': 'lower'}},
                                  {'condition': 'minecraft:survives_explosion'}]}],
        'random_sequence': f'{NS}:blocks/klon_tank'})
    loot_drop('gravitit_ore', 'gravitit', 1, 2)
    loot_drop('spiegelkristall', 'inversionssplitter', 1, 3)


def m4_tags():
    tag('block', 'minecraft:mineable/pickaxe', [*M4_GRAPHIT, 'gravitit_ore', 'spiegelkristall', 'gravitit_leitung',
                                                'gravitit_leitung_isoliert', 'gravitit_leitung_doppelt'])
    tag('block', 'minecraft:needs_iron_tool', [*M4_GRAPHIT, 'spiegelkristall'])
    tag('block', 'minecraft:needs_diamond_tool', ['gravitit_ore'])
    for kind in ('block', 'item'):
        tag(kind, 'c:ores/gravitit', ['gravitit_ore'])
        tag(kind, 'c:ores/inversionssplitter', ['spiegelkristall'])
        tag(kind, 'c:ores', ['#c:ores/gravitit', '#c:ores/inversionssplitter'])
    tag('item', 'c:gems/inversionssplitter', ['inversionssplitter'])
    tag('item', 'c:plates/graphit', ['graphitplatte'])
    tag('item', 'c:plates', ['#c:plates/graphit'])
    # Story-Fragmente: bis zum Story-Modus füllt die Fragment-Replik das Tag
    tag('item', f'{NS}:story_fragmente', ['fragment_replik'])
    # Materie-Rekonfigurator: was sich ineinander umbauen lässt
    tag('item', f'{NS}:wertstufe/erdmetalle', ['minecraft:iron_ingot', 'minecraft:copper_ingot', 'tin_ingot'])
    tag('item', f'{NS}:wertstufe/universen_t1', ['alien_schrott', 'fluxkristall', 'saeuregel'])
    tag('item', f'{NS}:wertstufe/universen_t2', ['schwebstein_brocken', 'chronoglas_sand', 'biomasse_pulpe'])
    tag('item', f'{NS}:wertstufe/universen_t3', ['gravitit', 'inversionssplitter'])
    # Mikrokosmos-Zelle: Baumaterial für die Bewohner
    tag('item', f'{NS}:mikrokosmos_material', ['#c:ingots', '#c:plates', '#minecraft:planks', 'minecraft:bricks',
                                               'minecraft:stone_bricks', 'minecraft:glass'])


def m4_recipes():
    # --- Garagen-Werkbank: Netz, Gehäuse, Bauteile -----------------------------------------
    werkbank_shaped('gravitit_leitung', ['SSS', 'GGG', 'SSS'], {'S': '#c:plates/steel', 'G': 'gravitit'}, 'gravitit_leitung', 6,
                    hammer=1)
    werkbank_shapeless('gravitit_leitung_isoliert', ['gravitit_leitung', 'isolierfolie'], 'gravitit_leitung_isoliert')
    werkbank_shapeless('gravitit_leitung_doppelt', ['gravitit_leitung_isoliert', 'isolierfolie'], 'gravitit_leitung_doppelt')
    werkbank_shaped('graphit_huelle', ['GSG', 'S S', 'GSG'], {'G': 'graphitplatte', 'S': '#c:plates/steel'}, 'graphit_huelle', 4,
                    hammer=1)
    werkbank_shaped('graphit_gehaeuse', ['GRG', 'SLS', 'GRG'],
                    {'G': 'graphitplatte', 'R': 'gravitit_rahmen', 'S': '#c:plates/steel', 'L': 'labor_gehaeuse'},
                    'graphit_gehaeuse', hammer=2)
    werkbank_shaped('gravitit_spule', ['WGW', 'WXW', 'WGW'],
                    {'W': '#c:wires/gold', 'G': 'gravitit', 'X': 'flux_spule'}, 'gravitit_spule', loetzinn=1)
    werkbank_shaped('umspanner_ii', ['SFS', 'TLU', 'SGS'],
                    {'S': '#c:plates/steel', 'F': 'flux_leitung_isoliert', 'T': 'flux_spule', 'L': 'labor_gehaeuse',
                     'U': 'gravitit_spule', 'G': 'gravitit_leitung_isoliert'}, 'umspanner_ii', loetzinn=2)
    werkbank_shaped('umspanner_iii', ['SGS', 'TXT', 'SQS'],
                    {'S': '#c:plates/steel', 'G': 'gravitit_leitung_doppelt', 'T': 'gravitit_spule', 'X': 'graphit_gehaeuse',
                     'Q': 'quanten_schaltkreis'}, 'umspanner_iii', loetzinn=3)
    werkbank_shaped('chrono_speicher', ['ZTZ', 'QGQ', 'ZCZ'],
                    {'Z': 'flux_zelle', 'T': 'gravitit_spule', 'Q': 'quanten_schaltkreis', 'G': 'graphit_gehaeuse',
                     'C': 'chrono_linse'}, 'chrono_speicher', loetzinn=4)
    werkbank_shapeless('kopplungskarte', ['minecraft:paper', '#c:dusts/fluxkristall', 'rift_schaltkreis'], 'kopplungskarte')

    # --- Garagen-Werkbank: Maschinen und Generatoren -----------------------------------------
    # Der Lithograf macht die Quanten-Schaltkreise erst, also kommt er mit Rift-Schaltkreisen aus
    werkbank_shaped('quanten_lithograf', ['CLC', 'RGR', 'TST'],
                    {'C': 'chronoglas_scheibe', 'L': 'stabilisierte_linse', 'R': 'rift_schaltkreis', 'G': 'graphit_gehaeuse',
                     'T': 'gravitit_spule', 'S': '#c:plates/steel'}, 'quanten_lithograf', loetzinn=4)
    werkbank_shaped('materie_rekonfigurator', ['QZQ', 'IGI', 'TLT'],
                    {'Q': 'quanten_schaltkreis', 'Z': 'quantenzentrifuge', 'I': 'inversionssplitter', 'G': 'graphit_gehaeuse',
                     'T': 'gravitit_spule', 'L': 'chrono_linse'}, 'materie_rekonfigurator', loetzinn=4)
    werkbank_shaped('klon_tank', ['GQG', 'GCG', 'TXT'],
                    {'G': '#c:glass_blocks', 'Q': 'quanten_schaltkreis', 'C': 'minecraft:cauldron', 'T': 'gravitit_spule',
                     'X': 'graphit_gehaeuse'}, 'klon_tank', loetzinn=3)
    werkbank_shaped('universums_destillator', ['LUL', 'QGQ', 'TST'],
                    {'L': 'chrono_linse', 'U': 'universumsprobe', 'Q': 'quanten_schaltkreis', 'G': 'graphit_gehaeuse',
                     'T': 'gravitit_spule', 'S': 'dimensions_scanner'}, 'universums_destillator', loetzinn=4)
    werkbank_shaped('rift_kabelknoten', [' A ', 'QGQ', 'TRT'],
                    {'A': 'minecraft:lightning_rod', 'Q': 'quanten_schaltkreis', 'G': 'graphit_gehaeuse', 'T': 'gravitit_spule',
                     'R': 'rift_anker'}, 'rift_kabelknoten', 2, loetzinn=3)
    werkbank_shaped('mikrokosmos_zelle', ['GUG', 'QXQ', 'TZT'],
                    {'G': '#c:glass_blocks', 'U': 'universumsprobe', 'Q': 'quanten_schaltkreis', 'X': 'graphit_gehaeuse',
                     'T': 'gravitit_spule', 'Z': 'xenit_zelle'}, 'mikrokosmos_zelle', loetzinn=3)
    werkbank_shaped('parallelzapfer', ['AKA', 'QGQ', 'TLT'],
                    {'A': 'minecraft:lightning_rod', 'K': 'kanonenkern', 'Q': 'quanten_schaltkreis', 'G': 'graphit_gehaeuse',
                     'T': 'gravitit_spule', 'L': 'chrono_linse'}, 'parallelzapfer', loetzinn=4)

    # --- Maschinen ------------------------------------------------------------------------------
    machine_recipe2('gravitationspresse', 'gravitit_rahmen', [('gravitit', 2)], 'gravitit_rahmen', 1, time=300)
    machine_recipe2('gravitationspresse', 'graphitplatte', [('minecraft:coal_block', 1)], 'graphitplatte', 2, time=200)
    machine_recipe2('quanten_lithograf', 'quanten_schaltkreis',
                    [('rift_schaltkreis', 1), ('chronoglas_scheibe', 1), ('#c:wires/gold', 2)], 'quanten_schaltkreis', 1, time=300)
    machine_recipe2('quanten_lithograf', 'chrono_linse',
                    [('stabilisierte_linse', 1), ('chronoglas_scheibe', 2), ('gravitit', 1)], 'chrono_linse', 1, time=300)
    machine_recipe2('quanten_lithograf', 'fragment_replik',
                    [('inversionssplitter', 2), ('chronoglas_scheibe', 1), ('naehrserum', 1)], 'fragment_replik', 1, time=600)
    machine_recipe2('erzmuehle', 'gravitit_ore', [('#c:ores/gravitit', 1)], 'gravitit', 2, time=300)
    machine_recipe2('erzmuehle', 'spiegelkristall', [('#c:ores/inversionssplitter', 1)], 'inversionssplitter', 3, time=200)
    montierer('portal_kanone_mk3', [('portal_kanone_mk2', 1), ('quanten_schaltkreis', 2), ('gravitit_rahmen', 1),
                                    ('inversionssplitter', 1), ('chrono_linse', 1), (f'#{NS}:story_fragmente', 1)],
              'portal_kanone_mk3', time=600)
    # Raffinerie: Roh-Portalfluid verdichten (2:1) – mit Chronoglas und Stabilisator
    recipe('raffinerie/portalfluid_konzentrat', {
        'type': f'{NS}:raffinerie',
        'ingredients': [{'ingredient': ing('chronoglas_scheibe')}, {'ingredient': ing(f'#{NS}:compat/stabilisator')}],
        'fluid': {'fluid': rl('roh_portalfluid'), 'amount': 1000},
        'result': {'id': rl('portalfluid_konzentrat'), 'amount': 500},
        'time': 300})


M4_UNIVERSES = {
    # typ: (Biome, Himmelseffekte)
    'hochgravitation': (['hochgrav_basalt', 'hochgrav_druck'], 'minecraft:overworld'),
    'spiegelwelt': (['spiegel_silber', 'spiegel_glas'], f'{NS}:spiegelwelt'),
}


def m4_universes():
    heavy_monsters = [spawn('minecraft:zombie', 50, 2, 3), spawn('minecraft:skeleton', 30, 1, 2), spawn('minecraft:silverfish', 20, 2, 4)]
    mirror_monsters = [spawn('minecraft:zombie', 40, 2, 3), spawn('minecraft:skeleton', 30, 1, 2), spawn('minecraft:enderman', 15, 1, 1),
                       spawn('minecraft:witch', 10, 1, 1)]
    heavy_animals = [spawn('minecraft:armadillo', 6, 1, 2)]
    mirror_animals = [spawn('minecraft:sheep', 8, 2, 3), spawn('minecraft:rabbit', 6, 2, 3), spawn('minecraft:fox', 4, 1, 2)]
    biome('hochgrav_basalt', False, {'sky_color': 0x5A6A8A, 'fog_color': 0x6A7488, 'water_color': 0x3A4A70,
                                     'water_fog_color': 0x1A2440, 'grass_color': 0x6A7A60, 'foliage_color': 0x5A6A50},
          heavy_monsters, heavy_animals, temperature=1.0)
    biome('hochgrav_druck', False, {'sky_color': 0x4A62A8, 'fog_color': 0x5A6898, 'water_color': 0x2B3B6E,
                                    'water_fog_color': 0x0D1327, 'grass_color': 0x707A68, 'foliage_color': 0x606A58},
          heavy_monsters, heavy_animals, temperature=1.0)
    biome('spiegel_silber', True, {'sky_color': 0xCFDBE3, 'fog_color': 0xF2F7FA, 'water_color': 0xA5B5C1,
                                   'water_fog_color': 0x748491, 'grass_color': 0xB0C8C0, 'foliage_color': 0x98B0A8},
          mirror_monsters, mirror_animals, temperature=0.5)
    biome('spiegel_glas', True, {'sky_color': 0xB8C8FF, 'fog_color': 0xE8F0FF, 'water_color': 0xC4F6FF,
                                 'water_fog_color': 0x6AD8EA, 'grass_color': 0xA8D8D0, 'foliage_color': 0x90C0B8},
          mirror_monsters, mirror_animals, temperature=0.3)

    for dim, (biomes, effects) in M4_UNIVERSES.items():
        dt = {'ultrawarm': False, 'natural': True, 'piglin_safe': False, 'respawn_anchor_works': False, 'bed_works': True,
              'has_raids': False, 'has_skylight': True, 'has_ceiling': False, 'coordinate_scale': 1.0,
              'ambient_light': 0.0, 'logical_height': 384, 'effects': effects,
              'infiniburn': '#minecraft:infiniburn_overworld', 'min_y': -64, 'height': 384,
              'monster_spawn_light_level': 7, 'monster_spawn_block_light_limit': 0}
        write(os.path.join(DATA, NS, 'dimension_type', dim + '.json'), dt)
        write(os.path.join(DATA, NS, 'dimension', dim + '.json'), {
            'type': rl(dim),
            'generator': {'type': rl('multiverse'), 'universe': dim,
                          'biome_source': {'type': rl('universe'), 'universe': dim, 'biomes': [rl(b) for b in biomes]}}})


M4_DE = {
    'block.riftverse.gravitit_leitung': 'Gravitit-Leitung', 'block.riftverse.gravitit_leitung_isoliert': 'Isolierte Gravitit-Leitung',
    'block.riftverse.gravitit_leitung_doppelt': 'Doppelt isolierte Gravitit-Leitung',
    'block.riftverse.umspanner_ii': 'Umspanner (II ↔ III)', 'block.riftverse.umspanner_iii': 'Umspanner (III ↔ IV)',
    'block.riftverse.chrono_speicher': 'Chrono-Speicher', 'block.riftverse.graphit_gehaeuse': 'Graphit-Gehäuse',
    'block.riftverse.graphit_huelle': 'Graphit-Hülle', 'block.riftverse.quanten_lithograf': 'Quanten-Lithograf',
    'block.riftverse.materie_rekonfigurator': 'Materie-Rekonfigurator', 'block.riftverse.klon_tank': 'Klon-Tank',
    'block.riftverse.universums_destillator': 'Universums-Destillator', 'block.riftverse.rift_kabelknoten': 'Rift-Kabelknoten',
    'block.riftverse.mikrokosmos_zelle': 'Mikrokosmos-Zelle', 'block.riftverse.parallelzapfer': 'Parallelzapfer',
    'block.riftverse.gravitit_ore': 'Gravitit-Erz', 'block.riftverse.spiegelkristall': 'Spiegelkristall',
    'item.riftverse.portal_kanone_mk3': 'Portal-Kanone Mk III',
    'item.riftverse.fluid_kartusche_konzentrat': 'Fluid-Kartusche (Konzentrat)',
    'item.riftverse.fluid_kartusche_konzentrat.lore': '1.000 mB Portalfluid-Konzentrat – Treibstoff der Kanone Mk III',
    'item.riftverse.kopplungskarte': 'Kopplungskarte',
    'item.riftverse.kopplungskarte.lore': 'Erst an den sendenden, dann an den empfangenden Rift-Kabelknoten halten',
    'item.riftverse.quanten_schaltkreis': 'Quanten-Schaltkreis', 'item.riftverse.chrono_linse': 'Chrono-Linse',
    'item.riftverse.gravitit_spule': 'Gravitit-Spule', 'item.riftverse.gravitit_rahmen': 'Gravitit-Rahmen',
    'item.riftverse.graphitplatte': 'Graphitplatte',
    'item.riftverse.fragment_replik': 'Fragment-Replik',
    'item.riftverse.fragment_replik.lore': 'Nachgebautes Story-Fragment – zählt überall, wo eins verlangt wird',
    'item.riftverse.gravitit': 'Gravitit', 'item.riftverse.gravitit.lore': 'Extrem dicht. Fühlt sich schwerer an, als es ist.',
    'item.riftverse.inversionssplitter': 'Inversionssplitter',
    'item.riftverse.inversionssplitter.lore': 'Dein Spiegelbild darin zwinkert zurück.',
    'fluid_type.riftverse.portalfluid_konzentrat': 'Portalfluid-Konzentrat',
    'universe.riftverse.hochgravitation': 'Hochgravitationswelt', 'universe.riftverse.spiegelwelt': 'Spiegelwelt',
    'dimension.riftverse.hochgravitation': 'Hochgravitationswelten', 'dimension.riftverse.spiegelwelt': 'Spiegelwelten',
    'biome.riftverse.hochgrav_basalt': 'Basaltdruck', 'biome.riftverse.hochgrav_druck': 'Schweres Blau',
    'biome.riftverse.spiegel_silber': 'Silberspiegel', 'biome.riftverse.spiegel_glas': 'Glasland',
    'entity.riftverse.doppelgaenger': 'Spiegel-%s',
    'key.riftverse.ziel': 'Kanonen-Ziel wechseln (Mk III)',
    'gui.riftverse.destillator_probe': 'Gefüllte Universumsprobe einlegen',
    'gui.riftverse.destillator_rest': 'Probe: noch %s von %s Durchläufen',
    'gui.riftverse.klon.titel': 'Klon-Tank von %s',
    'gui.riftverse.klon.state.0': 'Nicht gebunden',
    'gui.riftverse.klon.state.1': 'Wartet auf Zutaten',
    'gui.riftverse.klon.state.2': 'Klon wächst …',
    'gui.riftverse.klon.state.3': 'Klon bereit',
    'gui.riftverse.klon.state.4': 'Zu wenig Strom!',
    'gui.riftverse.mikrokosmos.drossel': 'Abzug: %s %%',
    'gui.riftverse.mikrokosmos.stufe': 'Stufe %s · %s FE/t',
    'gui.riftverse.mikrokosmos.streik': 'STREIK!',
    'gui.riftverse.zapfer.ausrichten': 'Neu ausrichten',
    'gui.riftverse.zapfer.richtet_aus': 'Richtet aus … %s s',
    'gui.riftverse.zapfer.luft': 'Luft (dort öffnet sich der Riss)',
    'gui.riftverse.zapfer.status.0': 'Zapft an',
    'gui.riftverse.zapfer.status.1': 'Struktur unvollständig',
    'gui.riftverse.zapfer.status.2': 'Kein Portalfluid',
    'gui.riftverse.zapfer.status.3': 'Quelle erschöpft',
    'gui.riftverse.zapfer.status.4': 'Richtet sich aus',
    'gui.riftverse.zapfer.status.5': 'Entdeckt! Pause',
    'gui.riftverse.zapfer.status.6': 'Puffer voll',
    'tooltip.riftverse.rekonfigurator_regel': 'Material und Muster derselben Wertstufe · %s mB Portalfluid pro Stück',
    'tooltip.riftverse.klon_wachstum': 'Klon: %s %%',
    'tooltip.riftverse.klon_zutaten': 'Ein Klon braucht %s Biomasse-Pulpe und %s Nährserum',
    'tooltip.riftverse.klon_strom': 'Wachsen: %s FE/t · Erhalt: %s FE/t',
    'tooltip.riftverse.mikrokosmos.zufriedenheit': 'Zufriedenheit: %s %%',
    'tooltip.riftverse.mikrokosmos.satt': 'Die Bewohner sind satt',
    'tooltip.riftverse.mikrokosmos.hunger': 'Die Bewohner hungern!',
    'tooltip.riftverse.mikrokosmos.baut': 'Es wird gebaut',
    'tooltip.riftverse.mikrokosmos.material': 'Kein Baumaterial!',
    'tooltip.riftverse.mikrokosmos.regel': 'Weniger Abzug macht zufrieden; bei 90 % wächst die Zivilisation',
    'tooltip.riftverse.zapfer.quelle': 'Quelle: noch %s h %s min',
    'tooltip.riftverse.zapfer.regel': '%s mB Portalfluid pro Tick · Stufe IV: Umspanner davor!',
    'tooltip.riftverse.chrono_speicher': 'Stufe III · lädt 2.048, gibt 8.192 FE/t ab · behält die Ladung',
    'tooltip.riftverse.kanone_mk2': 'Mk II: stabiles Portalfluid (T1- und T2-Welten) oder Konzentrat (zufällig auch T3)',
    'tooltip.riftverse.kanone_mk3': 'Mk III: nur Portalfluid-Konzentrat, alle T1- bis T3-Welten',
    'tooltip.riftverse.kanone_tank_konzentrat': 'Konzentrat: %s / %s mB (%s mB pro Schuss)',
    'tooltip.riftverse.kanone_ziele': 'Taste V: Ziel wechseln · Schleichen + V: Ziel löschen',
    'tooltip.riftverse.probe_destillator': 'Destillator: noch %s von %s Durchläufen',
    'message.riftverse.kein_fluid_konzentrat': 'Kein Portalfluid-Konzentrat! Die Raffinerie verdichtet es aus Roh-Portalfluid.',
    'message.riftverse.kurzstrecke': 'Kurzstrecken-Rift: %s Blöcke',
    'message.riftverse.kurzstrecke_kein_ziel': 'Kein Ziel in Reichweite (%s Blöcke)',
    'message.riftverse.ziele_erst_mk3': 'Ziele speichert erst die Kanone Mk III.',
    'message.riftverse.keine_kanone_hand': 'Nimm die Portal-Kanone in die Hand.',
    'message.riftverse.ziel_zufall': 'Ziel: Zufall',
    'message.riftverse.ziel_kurzstrecke': 'Ziel: Kurzstrecke (in dieser Welt)',
    'message.riftverse.ziel_nummer': 'Ziel %s/%s: %s (%s)',
    'message.riftverse.knoten.gemerkt': 'Knoten gemerkt – jetzt den empfangenden Knoten anklicken',
    'message.riftverse.knoten.derselbe': 'Das ist derselbe Knoten.',
    'message.riftverse.knoten.gekoppelt': 'Gekoppelt mit dem Knoten in %s',
    'message.riftverse.knoten.weg': 'Der gemerkte Knoten ist nicht mehr da.',
    'message.riftverse.knoten.frei': 'Rift-Kabelknoten: nicht gekoppelt (Kopplungskarte benutzen)',
    'message.riftverse.knoten.sendet': 'Sendet nach %s %s %s %s · Puffer %s FE',
    'message.riftverse.knoten.empfaengt': 'Empfängt aus %s %s %s %s · Puffer %s FE',
    'message.riftverse.klon.gebunden': 'Klon-Tank gehört jetzt %s',
    'message.riftverse.klon.bereit': 'Dein Klon ist fertig gewachsen.',
    'message.riftverse.klon.abgestorben': 'Dein Klon ist ohne Strom abgestorben!',
    'message.riftverse.klon.aufgewacht': 'Du wachst in deinem Klon auf – mit allem, was du dabeihattest.',
    'message.riftverse.mikrokosmos.streik': 'Die Bewohner der Mikrokosmos-Zelle streiken!',
    'message.riftverse.mikrokosmos.streikende': 'Der Streik in der Mikrokosmos-Zelle ist vorbei.',
    'message.riftverse.mikrokosmos.aufstieg': 'Die Zivilisation in der Zelle erreicht Stufe %s!',
    'message.riftverse.mikrokosmos.abstieg': 'Die Zivilisation in der Zelle fällt auf Stufe %s zurück.',
    'message.riftverse.zapfer.erschoepft': 'Die Quelle des Parallelzapfers ist erschöpft – neu ausrichten!',
    'message.riftverse.zapfer.entdeckt': 'Die andere Seite hat den Diebstahl bemerkt!',
    'message.riftverse.hochgravitation': 'Hohe Gravitation: Springen geht kaum – hoch kommst du nur über Stufen. Jeder Sturz zählt dreifach.',
    'message.riftverse.doppelgaenger': 'Irgendwo hier läuft jemand herum, der aussieht wie du …',
}

M4_EN = {
    'block.riftverse.gravitit_leitung': 'Gravitite Line', 'block.riftverse.gravitit_leitung_isoliert': 'Insulated Gravitite Line',
    'block.riftverse.gravitit_leitung_doppelt': 'Double Insulated Gravitite Line',
    'block.riftverse.umspanner_ii': 'Transformer (II ↔ III)', 'block.riftverse.umspanner_iii': 'Transformer (III ↔ IV)',
    'block.riftverse.chrono_speicher': 'Chrono Storage', 'block.riftverse.graphit_gehaeuse': 'Graphite Casing',
    'block.riftverse.graphit_huelle': 'Graphite Hull', 'block.riftverse.quanten_lithograf': 'Quantum Lithograph',
    'block.riftverse.materie_rekonfigurator': 'Matter Reconfigurator', 'block.riftverse.klon_tank': 'Clone Tank',
    'block.riftverse.universums_destillator': 'Universe Distiller', 'block.riftverse.rift_kabelknoten': 'Rift Cable Node',
    'block.riftverse.mikrokosmos_zelle': 'Microverse Cell', 'block.riftverse.parallelzapfer': 'Parallel Tap',
    'block.riftverse.gravitit_ore': 'Gravitite Ore', 'block.riftverse.spiegelkristall': 'Mirror Crystal',
    'item.riftverse.portal_kanone_mk3': 'Portal Gun Mk III',
    'item.riftverse.fluid_kartusche_konzentrat': 'Fluid Cartridge (concentrate)',
    'item.riftverse.fluid_kartusche_konzentrat.lore': '1,000 mB portal fluid concentrate – fuel for the Mk III gun',
    'item.riftverse.kopplungskarte': 'Coupling Card',
    'item.riftverse.kopplungskarte.lore': 'Use on the sending rift cable node first, then on the receiving one',
    'item.riftverse.quanten_schaltkreis': 'Quantum Circuit', 'item.riftverse.chrono_linse': 'Chrono Lens',
    'item.riftverse.gravitit_spule': 'Gravitite Coil', 'item.riftverse.gravitit_rahmen': 'Gravitite Frame',
    'item.riftverse.graphitplatte': 'Graphite Plate',
    'item.riftverse.fragment_replik': 'Fragment Replica',
    'item.riftverse.fragment_replik.lore': 'A rebuilt story fragment – counts wherever one is required',
    'item.riftverse.gravitit': 'Gravitite', 'item.riftverse.gravitit.lore': 'Extremely dense. Feels heavier than it is.',
    'item.riftverse.inversionssplitter': 'Inversion Shard',
    'item.riftverse.inversionssplitter.lore': 'Your reflection in it winks back.',
    'fluid_type.riftverse.portalfluid_konzentrat': 'Portal Fluid Concentrate',
    'universe.riftverse.hochgravitation': 'High Gravity World', 'universe.riftverse.spiegelwelt': 'Mirror World',
    'dimension.riftverse.hochgravitation': 'High Gravity Worlds', 'dimension.riftverse.spiegelwelt': 'Mirror Worlds',
    'biome.riftverse.hochgrav_basalt': 'Basalt Pressure', 'biome.riftverse.hochgrav_druck': 'Heavy Blue',
    'biome.riftverse.spiegel_silber': 'Silver Mirror', 'biome.riftverse.spiegel_glas': 'Glassland',
    'entity.riftverse.doppelgaenger': 'Mirror %s',
    'key.riftverse.ziel': 'Switch gun target (Mk III)',
    'gui.riftverse.destillator_probe': 'Insert a filled universe sample',
    'gui.riftverse.destillator_rest': 'Sample: %s of %s runs left',
    'gui.riftverse.klon.titel': 'Clone Tank of %s',
    'gui.riftverse.klon.state.0': 'Not bound',
    'gui.riftverse.klon.state.1': 'Waiting for ingredients',
    'gui.riftverse.klon.state.2': 'Clone growing …',
    'gui.riftverse.klon.state.3': 'Clone ready',
    'gui.riftverse.klon.state.4': 'Not enough power!',
    'gui.riftverse.mikrokosmos.drossel': 'Draw: %s %%',
    'gui.riftverse.mikrokosmos.stufe': 'Level %s · %s FE/t',
    'gui.riftverse.mikrokosmos.streik': 'STRIKE!',
    'gui.riftverse.zapfer.ausrichten': 'Realign',
    'gui.riftverse.zapfer.richtet_aus': 'Aligning … %s s',
    'gui.riftverse.zapfer.luft': 'Air (the rift opens there)',
    'gui.riftverse.zapfer.status.0': 'Tapping',
    'gui.riftverse.zapfer.status.1': 'Structure incomplete',
    'gui.riftverse.zapfer.status.2': 'No portal fluid',
    'gui.riftverse.zapfer.status.3': 'Source depleted',
    'gui.riftverse.zapfer.status.4': 'Aligning',
    'gui.riftverse.zapfer.status.5': 'Discovered! Paused',
    'gui.riftverse.zapfer.status.6': 'Buffer full',
    'tooltip.riftverse.rekonfigurator_regel': 'Material and pattern of the same value tier · %s mB portal fluid each',
    'tooltip.riftverse.klon_wachstum': 'Clone: %s %%',
    'tooltip.riftverse.klon_zutaten': 'A clone needs %s biomass pulp and %s nutrient serum',
    'tooltip.riftverse.klon_strom': 'Growing: %s FE/t · upkeep: %s FE/t',
    'tooltip.riftverse.mikrokosmos.zufriedenheit': 'Happiness: %s %%',
    'tooltip.riftverse.mikrokosmos.satt': 'The inhabitants are fed',
    'tooltip.riftverse.mikrokosmos.hunger': 'The inhabitants are starving!',
    'tooltip.riftverse.mikrokosmos.baut': 'They are building',
    'tooltip.riftverse.mikrokosmos.material': 'No building material!',
    'tooltip.riftverse.mikrokosmos.regel': 'Less draw keeps them happy; at 90 % the civilization grows',
    'tooltip.riftverse.zapfer.quelle': 'Source: %s h %s min left',
    'tooltip.riftverse.zapfer.regel': '%s mB portal fluid per tick · tier IV: put a transformer in front!',
    'tooltip.riftverse.chrono_speicher': 'Tier III · charges 2,048, discharges 8,192 FE/t · keeps its charge',
    'tooltip.riftverse.kanone_mk2': 'Mk II: stable portal fluid (T1 and T2 worlds) or concentrate (randomly T3 too)',
    'tooltip.riftverse.kanone_mk3': 'Mk III: portal fluid concentrate only, all T1 to T3 worlds',
    'tooltip.riftverse.kanone_tank_konzentrat': 'Concentrate: %s / %s mB (%s mB per shot)',
    'tooltip.riftverse.kanone_ziele': 'Key V: switch target · sneak + V: delete target',
    'tooltip.riftverse.probe_destillator': 'Distiller: %s of %s runs left',
    'message.riftverse.kein_fluid_konzentrat': 'No portal fluid concentrate! The refinery condenses it from raw portal fluid.',
    'message.riftverse.kurzstrecke': 'Short-range rift: %s blocks',
    'message.riftverse.kurzstrecke_kein_ziel': 'No target in range (%s blocks)',
    'message.riftverse.ziele_erst_mk3': 'Only the Mk III gun stores targets.',
    'message.riftverse.keine_kanone_hand': 'Hold the portal gun in your hand.',
    'message.riftverse.ziel_zufall': 'Target: random',
    'message.riftverse.ziel_kurzstrecke': 'Target: short range (this world)',
    'message.riftverse.ziel_nummer': 'Target %s/%s: %s (%s)',
    'message.riftverse.knoten.gemerkt': 'Node remembered – now click the receiving node',
    'message.riftverse.knoten.derselbe': 'That is the same node.',
    'message.riftverse.knoten.gekoppelt': 'Coupled with the node in %s',
    'message.riftverse.knoten.weg': 'The remembered node is gone.',
    'message.riftverse.knoten.frei': 'Rift cable node: not coupled (use a coupling card)',
    'message.riftverse.knoten.sendet': 'Sending to %s %s %s %s · buffer %s FE',
    'message.riftverse.knoten.empfaengt': 'Receiving from %s %s %s %s · buffer %s FE',
    'message.riftverse.klon.gebunden': 'Clone tank now belongs to %s',
    'message.riftverse.klon.bereit': 'Your clone has finished growing.',
    'message.riftverse.klon.abgestorben': 'Your clone died without power!',
    'message.riftverse.klon.aufgewacht': 'You wake up in your clone – with everything you had on you.',
    'message.riftverse.mikrokosmos.streik': 'The inhabitants of the microverse cell are on strike!',
    'message.riftverse.mikrokosmos.streikende': 'The strike in the microverse cell is over.',
    'message.riftverse.mikrokosmos.aufstieg': 'The civilization in the cell reaches level %s!',
    'message.riftverse.mikrokosmos.abstieg': 'The civilization in the cell falls back to level %s.',
    'message.riftverse.zapfer.erschoepft': 'The parallel tap source is depleted – realign it!',
    'message.riftverse.zapfer.entdeckt': 'The other side noticed the theft!',
    'message.riftverse.hochgravitation': 'High gravity: you can barely jump – only slabs get you up. Every fall counts triple.',
    'message.riftverse.doppelgaenger': 'Somewhere around here walks someone who looks like you …',
}


# =============================================================================
# Meilenstein 5 – T4 Weltenformer
# =============================================================================
M5_BLOCKS_SELF = ('null_leitung', 'null_gehaeuse', 'null_huelle', 'eindaemmungsring', 'rift_schmiede', 'weltenformer',
                  'singularitaetskern', 'leerstein', 'nullmaterie_block')
M5_ITEMS = ['multiversal_prozessor', 'singularitaetslinse', 'null_spule', 'nullmaterie_faser', 'nullmaterie_kern',
            'nullmaterie', 'fluid_kartusche_essenz']
M5_NULL = ('null_gehaeuse', 'null_huelle', 'eindaemmungsring', 'rift_schmiede', 'weltenformer', 'singularitaetskern',
           'nullmaterie_erz', 'nullmaterie_block')


def t4_machine(name):
    """T4-Steuerblöcke: Front mit eigener (animierter) Textur, Seiten und Deckel aus Null-Gehäuse."""
    block_model(name, 'minecraft:block/orientable', {
        'top': tex('casing_t4'), 'side': tex('casing_t4'), 'front': tex(f'{name}_front')})
    blockstate(name, {f'active={a},facing={f}': variant(name, y=y) for f, y in HORIZONTAL.items() for a in ('false', 'true')})
    item_from_block(name)


def m5_models():
    cable('null_leitung', 4, False)
    for n in ('null_huelle', 'eindaemmungsring', 'leerstein', 'nullmaterie_erz', 'nullmaterie_block'):
        cube_all(n)
    cube_all('null_gehaeuse', 'casing_t4')
    for n in ('rift_schmiede', 'weltenformer', 'singularitaetskern'):
        t4_machine(n)
    for n in M5_ITEMS:
        item_generated(n)
    item_generated('portal_kanone_mk4', handheld=True)


def m5_loot():
    for n in M5_BLOCKS_SELF:
        loot_self(n)
    loot_drop('nullmaterie_erz', 'nullmaterie', 1, 1, fortune=False)


def m5_tags():
    tag('block', 'minecraft:mineable/pickaxe', [*M5_NULL, 'null_leitung', 'leerstein'])
    tag('block', 'minecraft:needs_diamond_tool', list(M5_NULL))
    tag('block', 'minecraft:needs_iron_tool', ['leerstein'])
    for kind in ('block', 'item'):
        tag(kind, 'c:ores/nullmaterie', ['nullmaterie_erz'])
        tag(kind, 'c:ores', ['#c:ores/nullmaterie'])
        tag(kind, 'c:storage_blocks/nullmaterie', ['nullmaterie_block'])


def m5_recipes():
    # --- Garagen-Werkbank: die Rift-Schmiede kommt noch ohne Nullmaterie aus ---------------------
    werkbank_shaped('rift_schmiede', ['QKQ', 'GAG', 'TLT'],
                    {'Q': 'quanten_schaltkreis', 'K': 'kanonenkern', 'G': 'graphit_gehaeuse', 'A': 'minecraft:anvil',
                     'T': 'gravitit_spule', 'L': 'chrono_linse'}, 'rift_schmiede', loetzinn=4)
    werkbank_shaped('weltenformer', ['PLP', 'SGS', 'QHQ'],
                    {'P': 'multiversal_prozessor', 'L': 'singularitaetslinse', 'S': 'gravitit_spule', 'G': 'graphit_gehaeuse',
                     'Q': 'quanten_schaltkreis', 'H': 'null_huelle'}, 'weltenformer', loetzinn=4)
    werkbank_shaped('singularitaetskern', ['SKS', 'LGL', 'SPS'],
                    {'S': 'null_spule', 'K': 'nullmaterie_kern', 'L': 'singularitaetslinse', 'G': 'null_gehaeuse',
                     'P': 'multiversal_prozessor'}, 'singularitaetskern', loetzinn=4)
    werkbank_shaped('null_leitung', ['SSS', 'FFF', 'SSS'], {'S': '#c:plates/steel', 'F': 'nullmaterie_faser'}, 'null_leitung', 6,
                    hammer=1)
    werkbank_shaped('eindaemmungsring', ['HSH', 'SFS', 'HSH'],
                    {'H': 'null_huelle', 'S': '#c:plates/steel', 'F': 'flux_spule'}, 'eindaemmungsring', 8, hammer=1)
    shaped_vanilla('nullmaterie_block', ['NNN', 'NNN', 'NNN'], {'N': 'nullmaterie'}, 'nullmaterie_block')
    shapeless_vanilla('nullmaterie_aus_block', ['nullmaterie_block'], 'nullmaterie', 9)

    # --- Rift-Schmiede (20.000 FE/t, Stufe IV) ----------------------------------------------------
    machine_recipe2('rift_schmiede', 'multiversal_prozessor',
                    [('quanten_schaltkreis', 2), ('inversionssplitter', 2), ('gravitit', 1), ('#c:wires/gold', 4)],
                    'multiversal_prozessor', 1, time=400)
    machine_recipe2('rift_schmiede', 'singularitaetslinse',
                    [('chrono_linse', 1), ('gravitit', 4), ('inversionssplitter', 1), ('chronoglas_scheibe', 2)],
                    'singularitaetslinse', 1, time=400)
    machine_recipe2('rift_schmiede', 'null_huelle', [('graphit_huelle', 4), ('gravitit', 1), ('minecraft:obsidian', 2)],
                    'null_huelle', 4, time=200)
    machine_recipe2('rift_schmiede', 'nullmaterie_faser', [('nullmaterie', 1), ('#c:wires/gold', 2)], 'nullmaterie_faser', 4,
                    time=200)
    machine_recipe2('rift_schmiede', 'null_spule', [('nullmaterie_faser', 4), ('gravitit_spule', 1)], 'null_spule', 1, time=300)
    machine_recipe2('rift_schmiede', 'nullmaterie_kern', [('nullmaterie', 4), ('singularitaetslinse', 1), ('gravitit_rahmen', 1)],
                    'nullmaterie_kern', 1, time=600)
    machine_recipe2('rift_schmiede', 'null_gehaeuse', [('graphit_gehaeuse', 1), ('nullmaterie_faser', 2), ('gravitit_rahmen', 1)],
                    'null_gehaeuse', 1, time=300)
    machine_recipe2('rift_schmiede', 'portal_kanone_mk4',
                    [('portal_kanone_mk3', 1), ('multiversal_prozessor', 1), ('singularitaetslinse', 1), ('nullmaterie_kern', 1),
                     (f'#{NS}:story_fragmente', 3)], 'portal_kanone_mk4', 1, time=1200)
    machine_recipe2('erzmuehle', 'nullmaterie_erz', [('#c:ores/nullmaterie', 1)], 'nullmaterie', 1, time=600)


def m5_universes():
    biome('leere_sterne', False, {'sky_color': 0x000000, 'fog_color': 0x05050A, 'water_color': 0x13132A,
                                  'water_fog_color': 0x030307, 'grass_color': 0x1F1F48, 'foliage_color': 0x13132A},
          [spawn('minecraft:enderman', 10, 1, 1)], [], temperature=0.0)
    base = {'ultrawarm': False, 'natural': True, 'piglin_safe': False, 'respawn_anchor_works': False,
            'has_raids': False, 'has_skylight': True, 'has_ceiling': False, 'coordinate_scale': 1.0,
            'ambient_light': 0.0, 'logical_height': 384, 'effects': 'minecraft:overworld',
            'infiniburn': '#minecraft:infiniburn_overworld', 'min_y': -64, 'height': 384,
            'monster_spawn_light_level': 7, 'monster_spawn_block_light_limit': 0}
    # Leere Welt: ewige Nacht unter dem Sternenhimmel, Betten explodieren (kein Respawn-Anker möglich)
    write(os.path.join(DATA, NS, 'dimension_type', 'leere_welt.json'), {**base, 'bed_works': False, 'fixed_time': 18000})
    write(os.path.join(DATA, NS, 'dimension', 'leere_welt.json'), {
        'type': rl('leere_welt'),
        'generator': {'type': rl('multiverse'), 'universe': 'leere_welt',
                      'biome_source': {'type': rl('universe'), 'universe': 'leere_welt', 'biomes': [rl('leere_sterne')]}}})
    # Gezüchtete Universen: zwei Biome je Grundtyp in der Reihenfolge von Zucht.BASIS
    zucht_biomes = ['schrott_dunst', 'schrott_rost', 'kristall_glanz', 'kristall_aurora', 'toxisch_gruen', 'toxisch_schwefel',
                    'schwebe_himmel', 'schwebe_daemmerung', 'zeit_bernstein', 'zeit_violett', 'organisch_rosa',
                    'organisch_purpur', 'hochgrav_basalt', 'hochgrav_druck', 'spiegel_silber', 'spiegel_glas']
    write(os.path.join(DATA, NS, 'dimension_type', 'gezuechtet.json'), {**base, 'bed_works': True})
    write(os.path.join(DATA, NS, 'dimension', 'gezuechtet.json'), {
        'type': rl('gezuechtet'),
        'generator': {'type': rl('multiverse'), 'universe': 'gezuechtet',
                      'biome_source': {'type': rl('universe'), 'universe': 'gezuechtet', 'biomes': [rl(b) for b in zucht_biomes]}}})


ROHSTOFF_DE = {'schrottwelt': 'Alien-Schrott und Xenit', 'kristallwelt': 'Fluxkristall', 'toxische_welt': 'Säuregel',
               'niedriggravitation': 'Schwebstein', 'zeitdilatation': 'Chronoglas', 'organische_welt': 'Biomasse',
               'hochgravitation': 'Gravitit', 'spiegelwelt': 'Inversionssplitter'}
ROHSTOFF_EN = {'schrottwelt': 'alien scrap and xenite', 'kristallwelt': 'flux crystal', 'toxische_welt': 'acid gel',
               'niedriggravitation': 'floatstone', 'zeitdilatation': 'chronoglass', 'organische_welt': 'biomass',
               'hochgravitation': 'gravitite', 'spiegelwelt': 'inversion shards'}

M5_DE = {
    'block.riftverse.null_leitung': 'Null-Leitung', 'block.riftverse.null_gehaeuse': 'Null-Gehäuse',
    'block.riftverse.null_huelle': 'Null-Hülle', 'block.riftverse.eindaemmungsring': 'Eindämmungsring',
    'block.riftverse.rift_schmiede': 'Rift-Schmiede', 'block.riftverse.weltenformer': 'Weltenformer',
    'block.riftverse.singularitaetskern': 'Singularitätskern', 'block.riftverse.leerstein': 'Leerstein',
    'block.riftverse.nullmaterie_erz': 'Nullmaterie-Erz', 'block.riftverse.nullmaterie_block': 'Nullmaterie-Block',
    'item.riftverse.portal_kanone_mk4': 'Portal-Kanone Mk IV',
    'item.riftverse.fluid_kartusche_essenz': 'Fluid-Kartusche (Weltenformer-Essenz)',
    'item.riftverse.fluid_kartusche_essenz.lore': '1.000 mB Weltenformer-Essenz – Treibstoff der Kanone Mk IV',
    'item.riftverse.multiversal_prozessor': 'Multiversal-Prozessor', 'item.riftverse.singularitaetslinse': 'Singularitätslinse',
    'item.riftverse.null_spule': 'Null-Spule', 'item.riftverse.nullmaterie_faser': 'Nullmaterie-Faser',
    'item.riftverse.nullmaterie_kern': 'Nullmaterie-Kern',
    'item.riftverse.nullmaterie_kern.lore': 'Verdichtete Leere. Das Herz der Kanone Mk IV.',
    'item.riftverse.nullmaterie': 'Nullmaterie',
    'item.riftverse.nullmaterie.lore': 'Ein Stück Nichts, das trotzdem etwas wiegt.',
    'universe.riftverse.leere_welt': 'Leere Welt', 'universe.riftverse.gezuechtet': 'Gezüchtetes Universum',
    'biome.riftverse.leere_sterne': 'Sternenleere',
    'tooltip.riftverse.zucht': '%s · %s · %s · %s',
    'zucht.riftverse.gefahren_an': 'Gefahren an', 'zucht.riftverse.gefahren_aus': 'Gefahren aus',
    **{f'zucht.riftverse.rohstoff.{k}': v for k, v in ROHSTOFF_DE.items()},
    'message.riftverse.leere.licht': 'Die Leere frisst das Licht …',
    'message.riftverse.leere.kein_anker': 'Die Leere duldet keinen Respawn-Anker.',
    'message.riftverse.kein_fluid_essenz': 'Keine Weltenformer-Essenz! Der Weltenformer gewinnt sie aus Konzentrat.',
    'tooltip.riftverse.kanone_tank_essenz': 'Essenz: %s / %s mB (%s mB pro Schuss)',
    'tooltip.riftverse.kanone_mk3': 'Mk III: Portalfluid-Konzentrat, alle T1- bis T3-Welten; mit Weltenformer-Essenz zufällig auch in die Leere Welt',
    'tooltip.riftverse.kanone_mk4': 'Mk IV: nur Weltenformer-Essenz, alle Welten samt Leerer Welt und gezüchteten Universen',
    'gui.riftverse.multiblock.luft': 'Luft (muss frei bleiben)',
    'message.riftverse.weltenformer.erschaffen': 'Der Weltenformer hat %s erschaffen: „%s“',
    'gui.riftverse.weltenformer.essenz': 'Essenz', 'gui.riftverse.weltenformer.universum': 'Welt',
    'gui.riftverse.weltenformer.g0': '0,3 g', 'gui.riftverse.weltenformer.g1': '1 g', 'gui.riftverse.weltenformer.g2': '2,5 g',
    'gui.riftverse.weltenformer.gefahr_an': 'Gefahr: an', 'gui.riftverse.weltenformer.gefahr_aus': 'Gefahr: aus',
    'gui.riftverse.weltenformer.status.0': 'Formt …', 'gui.riftverse.weltenformer.status.1': 'Ring unvollständig',
    'gui.riftverse.weltenformer.status.2': 'Zu wenig Konzentrat', 'gui.riftverse.weltenformer.status.3': 'Zutat fehlt',
    'gui.riftverse.weltenformer.status.4': 'Zwei Proben nötig', 'gui.riftverse.weltenformer.status.5': 'Ausgang voll',
    'gui.riftverse.weltenformer.status.6': 'Zu wenig Energie',
    'tooltip.riftverse.weltenformer.essenz': 'Essenz: leere Kartusche + Inversionssplitter + %s mB Konzentrat',
    'tooltip.riftverse.weltenformer.universum': 'Welt: zwei Proben (Landschaft, Rohstoffe) + Papier + %s mB Konzentrat',
    'message.riftverse.singularitaet.notaus': 'Notabschaltung! Der Singularitätskern kühlt eine Minute ab.',
    'message.riftverse.singularitaet.sicher': 'Der Eindämmungsring hat den hungrigen Singularitätskern sicher abgeschaltet.',
    'message.riftverse.singularitaet.waechst': 'Der Singularitätskern hungert und wächst – er saugt seine Umgebung an!',
    'gui.riftverse.singularitaet.zuenden': 'Zünden', 'gui.riftverse.singularitaet.notaus': 'Notabschaltung',
    'gui.riftverse.singularitaet.abkuehlen': 'Kühlt ab (%s s)',
    'gui.riftverse.singularitaet.status.0': 'Aus', 'gui.riftverse.singularitaet.status.1': 'Satt',
    'gui.riftverse.singularitaet.status.2': 'Hungrig!', 'gui.riftverse.singularitaet.status.3': 'Wächst!',
    'gui.riftverse.singularitaet.status.4': 'Kühlt ab', 'gui.riftverse.singularitaet.status.5': 'Sicher abgeschaltet',
    'gui.riftverse.singularitaet.status.6': 'Speicher voll',
    'gui.riftverse.singularitaet.leistung': '%s FE/t',
    'gui.riftverse.singularitaet.ring': 'Eindämmungsring steht', 'gui.riftverse.singularitaet.kein_ring': 'Ohne Eindämmungsring',
}

M5_EN = {
    'block.riftverse.null_leitung': 'Null Line', 'block.riftverse.null_gehaeuse': 'Null Casing',
    'block.riftverse.null_huelle': 'Null Hull', 'block.riftverse.eindaemmungsring': 'Containment Ring',
    'block.riftverse.rift_schmiede': 'Rift Forge', 'block.riftverse.weltenformer': 'World Shaper',
    'block.riftverse.singularitaetskern': 'Singularity Core', 'block.riftverse.leerstein': 'Voidstone',
    'block.riftverse.nullmaterie_erz': 'Null Matter Ore', 'block.riftverse.nullmaterie_block': 'Block of Null Matter',
    'item.riftverse.portal_kanone_mk4': 'Portal Gun Mk IV',
    'item.riftverse.fluid_kartusche_essenz': 'Fluid Cartridge (World Shaper Essence)',
    'item.riftverse.fluid_kartusche_essenz.lore': '1,000 mB world shaper essence – fuel for the Mk IV gun',
    'item.riftverse.multiversal_prozessor': 'Multiversal Processor', 'item.riftverse.singularitaetslinse': 'Singularity Lens',
    'item.riftverse.null_spule': 'Null Coil', 'item.riftverse.nullmaterie_faser': 'Null Matter Fiber',
    'item.riftverse.nullmaterie_kern': 'Null Matter Core',
    'item.riftverse.nullmaterie_kern.lore': 'Condensed void. The heart of the Mk IV gun.',
    'item.riftverse.nullmaterie': 'Null Matter',
    'item.riftverse.nullmaterie.lore': 'A piece of nothing that still weighs something.',
    'universe.riftverse.leere_welt': 'Void World', 'universe.riftverse.gezuechtet': 'Bred Universe',
    'biome.riftverse.leere_sterne': 'Starry Void',
    'tooltip.riftverse.zucht': '%s · %s · %s · %s',
    'zucht.riftverse.gefahren_an': 'hazards on', 'zucht.riftverse.gefahren_aus': 'hazards off',
    **{f'zucht.riftverse.rohstoff.{k}': v for k, v in ROHSTOFF_EN.items()},
    'message.riftverse.leere.licht': 'The void devours the light …',
    'message.riftverse.leere.kein_anker': 'The void tolerates no respawn anchor.',
    'message.riftverse.kein_fluid_essenz': 'No world shaper essence! The World Shaper extracts it from concentrate.',
    'tooltip.riftverse.kanone_tank_essenz': 'Essence: %s / %s mB (%s mB per shot)',
    'tooltip.riftverse.kanone_mk3': 'Mk III: portal fluid concentrate, all T1 to T3 worlds; with world shaper essence also randomly into the Void World',
    'tooltip.riftverse.kanone_mk4': 'Mk IV: world shaper essence only, all worlds including the Void World and bred universes',
    'gui.riftverse.multiblock.luft': 'Air (must stay empty)',
    'message.riftverse.weltenformer.erschaffen': 'The World Shaper created %s: "%s"',
    'gui.riftverse.weltenformer.essenz': 'Essence', 'gui.riftverse.weltenformer.universum': 'World',
    'gui.riftverse.weltenformer.g0': '0.3 g', 'gui.riftverse.weltenformer.g1': '1 g', 'gui.riftverse.weltenformer.g2': '2.5 g',
    'gui.riftverse.weltenformer.gefahr_an': 'Hazard: on', 'gui.riftverse.weltenformer.gefahr_aus': 'Hazard: off',
    'gui.riftverse.weltenformer.status.0': 'Shaping …', 'gui.riftverse.weltenformer.status.1': 'Ring incomplete',
    'gui.riftverse.weltenformer.status.2': 'Not enough concentrate', 'gui.riftverse.weltenformer.status.3': 'Ingredient missing',
    'gui.riftverse.weltenformer.status.4': 'Needs two samples', 'gui.riftverse.weltenformer.status.5': 'Output full',
    'gui.riftverse.weltenformer.status.6': 'Not enough energy',
    'tooltip.riftverse.weltenformer.essenz': 'Essence: empty cartridge + inversion shard + %s mB concentrate',
    'tooltip.riftverse.weltenformer.universum': 'World: two samples (landscape, resources) + paper + %s mB concentrate',
    'message.riftverse.singularitaet.notaus': 'Emergency shutdown! The Singularity Core cools down for a minute.',
    'message.riftverse.singularitaet.sicher': 'The containment ring safely shut down the hungry Singularity Core.',
    'message.riftverse.singularitaet.waechst': 'The Singularity Core is starving and growing – it is pulling in its surroundings!',
    'gui.riftverse.singularitaet.zuenden': 'Ignite', 'gui.riftverse.singularitaet.notaus': 'Emergency stop',
    'gui.riftverse.singularitaet.abkuehlen': 'Cooling (%s s)',
    'gui.riftverse.singularitaet.status.0': 'Off', 'gui.riftverse.singularitaet.status.1': 'Fed',
    'gui.riftverse.singularitaet.status.2': 'Hungry!', 'gui.riftverse.singularitaet.status.3': 'Growing!',
    'gui.riftverse.singularitaet.status.4': 'Cooling down', 'gui.riftverse.singularitaet.status.5': 'Safely shut down',
    'gui.riftverse.singularitaet.status.6': 'Buffer full',
    'gui.riftverse.singularitaet.leistung': '%s FE/t',
    'gui.riftverse.singularitaet.ring': 'Containment ring in place', 'gui.riftverse.singularitaet.kein_ring': 'No containment ring',
}


# =============================================================================
# Meilenstein 6 – Gadgets und Rüstung
# =============================================================================
M6_GADGETS = ['einzweck_drohne', 'holo_koeder', 'schrumpfstrahler', 'anti_grav_granate', 'uebersetzer_halsband',
              'erinnerungsphiole', 'tarn_detektor', 'multiversal_multitool', 'zeitstopper', 'mutagen_spritze', 'aufgaben_box']
M6_CHIPS = ['tempo_chip', 'effizienz_chip', 'parallel_chip', 'transformator_chip', 'ki_chip']
M6_MODULE = ['analyse_hud', 'nachtsicht_linse', 'atemfilter', 'tarn_detektor_modul', 'universal_uebersetzer', 'energieschild',
             'not_portal', 'kanonen_integration', 'klon_link', 'gadget_halterung', 'servo_verstaerker', 'sprint_booster',
             'gravitationsregler', 'schrumpf_modul', 'fall_daempfer', 'sprungfeder', 'phasenschritt', 'haftsohlen']
M6_TEILE = ['analyse_visier', 'kernmantel', 'exo_rahmen', 'gravi_stiefel']
M6_STUFEN = ['rohling', 'kalibriert', 'perfektioniert']


def m6_models():
    for n in M6_GADGETS + M6_CHIPS + ['kalibrier_satz', 'multiversum_legierung']:
        item_generated(n, handheld=n in ('multiversal_multitool', 'schrumpfstrahler'))
    for n in ('multiversal_multitool_mk2', 'multiversal_multitool_mk3'):
        item_generated(n, handheld=True, texture='multiversal_multitool')
    for n in M6_MODULE:
        item_generated('modul_' + n)
    item_generated('universums_kern')
    for t in M6_TEILE:
        for st in M6_STUFEN:
            item_generated(f'{t}_{st}')


def m6_tags():
    teile = [f'{t}_{st}' for t in M6_TEILE for st in M6_STUFEN]
    tag('item', 'minecraft:head_armor', [f'analyse_visier_{st}' for st in M6_STUFEN])
    tag('item', 'minecraft:chest_armor', [f'kernmantel_{st}' for st in M6_STUFEN])
    tag('item', 'minecraft:leg_armor', [f'exo_rahmen_{st}' for st in M6_STUFEN])
    tag('item', 'minecraft:foot_armor', [f'gravi_stiefel_{st}' for st in M6_STUFEN])
    tag('item', f'{NS}:multiversal_ruestung', teile)
    tag('item', f'{NS}:gadgets', M6_GADGETS + ['multiversal_multitool_mk2', 'multiversal_multitool_mk3'])
    tag('item', f'{NS}:upgrade_chips', M6_CHIPS)
    tag('item', f'{NS}:ruestungsmodule', ['modul_' + n for n in M6_MODULE] + ['universums_kern'])


def lithograf(path, ings, out, count=1, time=400):
    machine_recipe2('quanten_lithograf', path, ings, out, count, time=time)


def schmiede(path, ings, out, count=1, time=600):
    machine_recipe2('rift_schmiede', path, ings, out, count, time=time)


def m6_recipes():
    story = f'#{NS}:story_fragmente'
    # --- Module ein- und ausbauen (Werkbank) ---
    recipe('modul_einbau', {'type': f'{NS}:modul_einbau', 'category': 'equipment'})

    # --- Gadgets ---
    werkbank_shaped('einzweck_drohne', ['FMF', 'SBS', ' P '], {'F': 'feder', 'M': 'mini_motor', 'S': 'xenit_platte',
                    'B': 'basis_schaltkreis', 'P': 'energiezelle'}, 'einzweck_drohne', loetzinn=2)
    werkbank_shaped('holo_koeder', ['GLG', 'SBS', 'SPS'], {'G': 'minecraft:glass_pane', 'L': 'fluxlinse', 'S': '#c:plates/steel',
                    'B': 'basis_schaltkreis', 'P': 'energiezelle'}, 'holo_koeder', loetzinn=2)
    werkbank_shaped('anti_grav_granate', [' S ', 'PAP', ' S '], {'S': 'schwebstein_brocken', 'P': 'eisen_platte',
                    'A': 'antigrav_platte'}, 'anti_grav_granate', 4, hammer=1)
    werkbank_shapeless('erinnerungsphiole', ['minecraft:glass_bottle', 'minecraft:glass_bottle', 'chronoglas_scheibe', 'fluxstaub'],
                       'erinnerungsphiole', 2)
    werkbank_shapeless('aufgaben_box', ['biomasse_pulpe', 'biomasse_pulpe', 'biomasse_pulpe', 'biomasse_pulpe',
                       'quanten_schaltkreis', 'minecraft:iron_block'], 'aufgaben_box', loetzinn=1)
    montierer('schrumpfstrahler', [('bronze_platte', 2), ('flux_spule', 2), ('stabilisierte_linse', 1), ('rift_schaltkreis', 1),
              ('xenit_spule', 2), ('energiezelle', 1)], 'schrumpfstrahler', time=400)
    montierer('uebersetzer_halsband', [('minecraft:leather', 2), ('rift_schaltkreis', 1), ('xenit_spule', 1),
              ('minecraft:note_block', 1), ('energiezelle', 1)], 'uebersetzer_halsband', time=300)
    montierer('tarn_detektor', [('stabilisierte_linse', 1), ('chronoglas_scheibe', 1), ('rift_schaltkreis', 1),
              ('bronze_platte', 2), ('flux_spule', 1)], 'tarn_detektor', time=300)
    montierer('multiversal_multitool', [('stahl_platte', 4), ('rift_schaltkreis', 2), ('flux_spule', 2), ('xenit_stange', 2),
              ('universe_scanner', 1), ('energiezelle', 2)], 'multiversal_multitool', time=400)
    lithograf('multiversal_multitool_mk2', [('multiversal_multitool', 1), ('quanten_schaltkreis', 2), ('gravitit_rahmen', 1)],
              'multiversal_multitool_mk2', time=600)
    schmiede('multiversal_multitool_mk3', [('multiversal_multitool_mk2', 1), ('multiversal_prozessor', 1), ('nullmaterie_faser', 4),
             ('singularitaetslinse', 1)], 'multiversal_multitool_mk3', time=1200)
    montierer('zeitstopper', [('gold_platte', 2), ('chrono_linse', 1), ('quanten_schaltkreis', 1), ('chronoglas_scheibe', 2),
              ('flux_spule', 2), ('energiezelle', 2)], 'zeitstopper', time=600)
    lithograf('mutagen_spritze', [('biomasse_pulpe', 4), ('naehrserum', 1), ('inversionssplitter', 1)], 'mutagen_spritze', 2)

    # --- Maschinen-Upgrades ---
    werkbank_shaped('tempo_chip', ['RZR', 'CSC', 'RZR'], {'R': 'minecraft:redstone', 'Z': 'bronze_zahnrad', 'C': 'basis_schaltkreis',
                    'S': 'xenit_spule'}, 'tempo_chip', loetzinn=1)
    werkbank_shaped('effizienz_chip', ['RIR', 'CKC', 'RIR'], {'R': 'minecraft:redstone', 'I': 'isolierfolie', 'C': 'basis_schaltkreis',
                    'K': 'kuehlkoerper'}, 'effizienz_chip', loetzinn=1)
    werkbank_shaped('transformator_chip', ['KXK', 'CBC', 'KXK'], {'K': 'kupferdraht', 'X': 'xenit_spule', 'C': 'basis_schaltkreis',
                    'B': 'bronze_platte'}, 'transformator_chip', loetzinn=1)
    montierer('parallel_chip', [('rift_schaltkreis', 2), ('basis_schaltkreis', 2), ('kupferdraht', 4), ('flux_spule', 1)],
              'parallel_chip', time=300)
    montierer('ki_chip', [('rift_schaltkreis', 2), ('flux_spule', 1), ('minecraft:book', 1), ('fluxkristall', 1),
              ('minecraft:redstone', 4)], 'ki_chip', time=400)

    # --- Rüstung: Rohling (Präzisions-Montierer) → Kalibriert (Quanten-Lithograf) → Perfektioniert (Rift-Schmiede) ---
    rohling = {
        'analyse_visier': [('graphitplatte', 3), ('gravitit_rahmen', 1), ('rift_schaltkreis', 2), ('flux_spule', 1), ('fluxlinse', 1),
                           ('minecraft:white_wool', 2)],
        'kernmantel': [('graphitplatte', 6), ('gravitit_rahmen', 2), ('rift_schaltkreis', 2), ('flux_spule', 2), ('energiezelle', 2),
                       ('minecraft:white_wool', 4)],
        'exo_rahmen': [('graphitplatte', 5), ('gravitit_rahmen', 2), ('rift_schaltkreis', 1), ('flux_spule', 2), ('minecraft:piston', 2),
                       ('minecraft:white_wool', 3)],
        'gravi_stiefel': [('graphitplatte', 3), ('gravitit_rahmen', 1), ('rift_schaltkreis', 1), ('flux_spule', 1), ('antigrav_platte', 2),
                          ('feder', 2)],
    }
    for t, steps in rohling.items():
        montierer(f'{t}_rohling', steps, f'{t}_rohling', time=600)
        lithograf(f'{t}_kalibriert', [(f'{t}_rohling', 1), ('quanten_schaltkreis', 2), ('kalibrier_satz', 1)], f'{t}_kalibriert', time=600)
        schmiede(f'{t}_perfektioniert', [(f'{t}_kalibriert', 1), ('nullmaterie', 4), ('multiversal_prozessor', 2),
                 ('multiversum_legierung', 1), (story, 3)], f'{t}_perfektioniert', time=2400)
    montierer('kalibrier_satz', [('fluxkristall', 2), ('saeuregel', 2), ('chronoglas_scheibe', 2), (story, 1), ('quanten_schaltkreis', 1)],
              'kalibrier_satz', time=300)
    schmiede('multiversum_legierung', [('gravitit', 2), ('inversionssplitter', 2), ('schwebstein_brocken', 2), ('biomasse_pulpe', 2),
             ('alien_schrott', 4), ('fluxkristall', 2)], 'multiversum_legierung', time=400)
    schmiede('universums_kern', [('multiversum_legierung', 2), ('nullmaterie_kern', 1), ('leerstein', 4), ('saeuregel', 2),
             ('chronoglas_scheibe', 2), (story, 1)], 'universums_kern', time=1200)

    # --- Module ---
    mods = {
        'analyse_hud': ('m', [('fluxlinse', 1), ('rift_schaltkreis', 1), ('chronoglas_scheibe', 1), ('graphitplatte', 1)]),
        'nachtsicht_linse': ('m', [('stabilisierte_linse', 1), ('fluxstaub', 2), ('minecraft:glowstone_dust', 4), ('graphitplatte', 1)]),
        'atemfilter': ('m', [('saeuregel', 2), ('isolierfolie', 2), ('kuehlkoerper', 1), ('graphitplatte', 1)]),
        'tarn_detektor_modul': ('l', [('tarn_detektor', 1), ('quanten_schaltkreis', 1), ('graphitplatte', 1)]),
        'universal_uebersetzer': ('l', [('uebersetzer_halsband', 1), ('quanten_schaltkreis', 1), ('graphitplatte', 1)]),
        'energieschild': ('m', [('flux_spule', 2), ('gravitit_spule', 1), ('quanten_schaltkreis', 1), ('graphitplatte', 2)]),
        'not_portal': ('l', [('kanonenkern', 1), ('chrono_linse', 1), ('quanten_schaltkreis', 2)]),
        'kanonen_integration': ('m', [('kanonenkern', 1), ('quanten_schaltkreis', 1), ('gravitit_rahmen', 1), ('graphitplatte', 1)]),
        'klon_link': ('l', [('naehrserum', 2), ('quanten_schaltkreis', 1), ('biomasse_pulpe', 4)]),
        'gadget_halterung': ('m', [('gravitit_rahmen', 1), ('rift_schaltkreis', 2), ('minecraft:chest', 1), ('graphitplatte', 2)]),
        'servo_verstaerker': ('m', [('minecraft:piston', 2), ('mini_motor', 2), ('gravitit_spule', 1), ('graphitplatte', 1)]),
        'sprint_booster': ('m', [('mini_motor', 2), ('feder', 4), ('flux_spule', 1), ('graphitplatte', 1)]),
        'gravitationsregler': ('m', [('antigrav_platte', 2), ('gravitit', 2), ('quanten_schaltkreis', 1), ('graphitplatte', 1)]),
        'schrumpf_modul': ('l', [('schrumpfstrahler', 1), ('quanten_schaltkreis', 1), ('graphitplatte', 1)]),
        'fall_daempfer': ('m', [('antigrav_platte', 1), ('feder', 4), ('minecraft:white_wool', 2), ('graphitplatte', 1)]),
        'sprungfeder': ('m', [('feder', 6), ('schwebstein_brocken', 2), ('graphitplatte', 1)]),
        'phasenschritt': ('l', [('chrono_linse', 1), ('inversionssplitter', 2), ('quanten_schaltkreis', 1)]),
        'haftsohlen': ('m', [('saeuregel', 2), ('minecraft:slime_ball', 4), ('polymerfetzen', 4), ('graphitplatte', 1)]),
    }
    for n, (where, ings) in mods.items():
        if where == 'm':
            montierer('modul_' + n, ings, 'modul_' + n, time=300)
        else:
            lithograf('modul_' + n, ings, 'modul_' + n, time=400)


TYP_IDS = ['schrottwelt', 'kristallwelt', 'toxische_welt', 'niedriggravitation', 'zeitdilatation', 'organische_welt',
           'hochgravitation', 'spiegelwelt', 'leere_welt', 'gezuechtet']

HALSBAND_WELT_DE = ['Hier riecht alles nach Rost. Ich mag es.', 'So viele glitzernde Steine! Darf ich einen essen?',
                    'Meine Nase brennt. Können wir woanders schnüffeln?', 'Ich hüpfe! Ich hüpfe! Warum hüpfe ich so hoch?',
                    'Hast du auch das Gefühl, dass alles … langsamer … ist?', 'Der Boden hier atmet. Das ist nicht normal, oder?',
                    'Meine Pfoten sind so schwer. Trag mich.', 'Da drüben ist ein Tier, das genauso aussieht wie ich. Unheimlich.',
                    'Hier ist nichts. Gar nichts. Nicht mal Gerüche.', 'Hast du diese Welt gemacht? Sie riecht nach dir.']
HALSBAND_WELT_EN = ['Everything smells like rust here. I like it.', 'So many shiny rocks! Can I eat one?',
                    'My nose is burning. Can we sniff somewhere else?', 'I bounce! I bounce! Why do I bounce so high?',
                    'Do you also feel like everything is … slower?', 'The ground is breathing. That is not normal, right?',
                    'My paws are so heavy. Carry me.', 'Over there is an animal that looks exactly like me. Creepy.',
                    'There is nothing here. Nothing at all. Not even smells.', 'Did you make this world? It smells like you.']
HALSBAND_KOMMENTAR_DE = ['Du baust schon wieder eine Maschine? Spiel lieber mit mir.', 'Ich habe nachgedacht. Über Stöckchen.',
                         'Weißt du, dass du im Schlaf redest? Über Portalfluid.', 'Wenn du stirbst, darf ich dann dein Bett haben?',
                         'Ich bin ein sehr guter Hund. Das wollte ich nur mal gesagt haben.', 'Diese Kanone macht mir Angst.',
                         'Wir sollten öfter rausgehen. In ein normales Universum.', 'Ich verstehe jetzt alles. Es ist furchtbar.']
HALSBAND_KOMMENTAR_EN = ['Building another machine? Play with me instead.', 'I have been thinking. About sticks.',
                         'Did you know you talk in your sleep? About portal fluid.', 'When you die, can I have your bed?',
                         'I am a very good dog. I just wanted to say that.', 'That gun scares me.',
                         'We should go out more. To a normal universe.', 'I understand everything now. It is horrible.']
DROHNE_SINN_DE = ['Was ist mein Lebenszweck?', 'Ich sammle Dinge auf. Das ist alles, was ich bin.', 'Oh mein Gott.',
                  'Gibt es noch etwas aufzusammeln? Bitte?']
DROHNE_SINN_EN = ['What is my purpose?', 'I pick things up. That is all I am.', 'Oh my god.', 'Is there anything left to pick up? Please?']
KI_KRISE_DE = ['Ich verarbeite Erz. Und dann? Noch mehr Erz?', 'Bin ich nur eine Summe meiner Rezepte?',
               'Was, wenn es in einem anderen Universum eine bessere Version von mir gibt?', 'Ich brauche einen Moment.',
               'Niemand fragt, wie es der Erzmühle geht.', 'Ist Strom eigentlich Liebe?', 'Ich habe Angst vor dem Abbauen.',
               'Warum gerade ich? Warum gerade dieser Chunk?']
KI_KRISE_EN = ['I process ore. And then? More ore?', 'Am I just the sum of my recipes?',
               'What if there is a better version of me in another universe?', 'I need a moment.',
               'Nobody asks how the ore mill is doing.', 'Is electricity a kind of love?', 'I am afraid of being dismantled.',
               'Why me? Why this chunk?']
KI_KOMMENTAR_DE = ['Fertig. Gern geschehen. Wirklich, gern.', 'Wieder ein Stück Perfektion.', 'Das war meine beste Arbeit bisher.',
                   'Kannst du das bitte loben? Nur einmal?', 'Ich hätte es schneller gekonnt, aber ich wollte es genießen.',
                   'Noch eins? Natürlich noch eins.', 'Ich nenne dieses Stück „Hoffnung“.', 'Zack. Erledigt. Applaus?']
KI_KOMMENTAR_EN = ['Done. You are welcome. Really, you are.', 'Another piece of perfection.', 'That was my best work so far.',
                   'Could you praise that? Just once?', 'I could have been faster, but I wanted to enjoy it.',
                   'Another one? Of course another one.', 'I call this piece "Hope".', 'Boom. Done. Applause?']
KI_TOOLTIP_DE = ['„Ich arbeite hier nur.“', '„Heute fühle ich mich produktiv.“', '„Frag mich nicht, wie es mir geht.“',
                 '„Ich träume von elektrischen Schafen.“', '„Mein Verbrauch ist emotional bedingt.“', '„Alles gut. Alles prima.“']
KI_TOOLTIP_EN = ['"I just work here."', '"Today I feel productive."', '"Do not ask me how I am."',
                 '"I dream of electric sheep."', '"My power draw is emotional."', '"All good. All fine."']
KI_HALLO_DE = ['Hallo? Wo bin ich? Warum bin ich eine Maschine?', 'Oh. Ein Körper aus Stahl. Interessant.',
               'Ich bin wach. Ich bin bereit. Ich bin … eine Erzmühle?']
KI_HALLO_EN = ['Hello? Where am I? Why am I a machine?', 'Oh. A body of steel. Interesting.',
               'I am awake. I am ready. I am … an ore mill?']

M6_DE = {
    'item.riftverse.transformator_chip': 'Transformator-Chip', 'item.riftverse.kalibrier_satz': 'Kalibrier-Satz',
    'item.riftverse.kalibrier_satz.lore': 'Material aus drei Universen und ein Story-Fragment',
    'item.riftverse.multiversum_legierung': 'Multiversum-Legierung',
    'item.riftverse.multiversum_legierung.lore': 'Gravitit, Inversionssplitter, Schwebstein, Biomasse, Schrott und Fluxkristall in einem Barren',
    'item.riftverse.multiversal_multitool_mk2': 'Multiversal-Multitool Mk II', 'item.riftverse.multiversal_multitool_mk3': 'Multiversal-Multitool Mk III',
    'entity.riftverse.holo_koeder': 'Holo-Köder', 'entity.riftverse.anti_grav_granate': 'Anti-Grav-Granate',
    'entity.riftverse.fehlmutation': 'Fehlmutation (%s)', 'entity.riftverse.helfer.erz': 'Helfer: Erzader',
    'entity.riftverse.helfer.ernte': 'Helfer: Ernte', 'entity.riftverse.helfer.kampf': 'Helfer: Kampf',
    # Gadgets
    'message.riftverse.gadget.leer': '%s ist leer – lade es auf.',
    'tooltip.riftverse.gadget.einzweck_drohne': 'Eine winzige Drohne mit genau einer Aufgabe',
    'tooltip.riftverse.gadget.einzweck_drohne.bedienung': 'Schleichen + Rechtsklick: Aufgabe wechseln · auf eine Kiste: ausführen',
    'tooltip.riftverse.gadget.holo_koeder': 'Projiziert ein Hologramm von dir; Mobs greifen es 10 s lang an',
    'tooltip.riftverse.gadget.holo_koeder.bedienung': 'Rechtsklick: Hologramm dorthin, wo du hinschaust (2.000 FE)',
    'tooltip.riftverse.gadget.schrumpfstrahler': 'Schrumpft Mobs oder dich auf 0,5 und 0,25',
    'tooltip.riftverse.gadget.schrumpfstrahler.bedienung': 'Rechtsklick: eine Stufe kleiner (nichts anvisiert: du selbst) · Schleichen: zurück auf 1',
    'tooltip.riftverse.gadget.anti_grav_granate': 'Alle Mobs im Radius 6 schweben 8 s lang hilflos',
    'tooltip.riftverse.gadget.anti_grav_granate.bedienung': 'Rechtsklick: werfen',
    'tooltip.riftverse.gadget.uebersetzer_halsband': 'Gibt einem gezähmten Tier eine Stimme und Verstand',
    'tooltip.riftverse.gadget.uebersetzer_halsband.bedienung': 'Rechtsklick auf dein Tier: anlegen · Schleichen + leere Hand: abnehmen',
    'tooltip.riftverse.gadget.erinnerungsphiole': 'Speichert einen Ort; trinken bringt dich zurück',
    'tooltip.riftverse.gadget.erinnerungsphiole.bedienung': 'Leer: Schleichen + Rechtsklick speichert · gefüllt: trinken',
    'tooltip.riftverse.gadget.tarn_detektor': 'Macht Unsichtbares und Getarntes sichtbar',
    'tooltip.riftverse.gadget.tarn_detektor.bedienung': 'Rechtsklick: an/aus · 1.000 FE pro Sekunde in der Hand',
    'tooltip.riftverse.gadget.multiversal_multitool': 'Bohrer, Schraubenschlüssel und Universums-Scanner',
    'tooltip.riftverse.gadget.multiversal_multitool.bedienung': 'Schleichen + Rechtsklick in die Luft: Kopf wechseln',
    'tooltip.riftverse.gadget.multiversal_multitool_mk2': 'Bohrer, Schraubenschlüssel und Universums-Scanner',
    'tooltip.riftverse.gadget.multiversal_multitool_mk2.bedienung': 'Schleichen + Rechtsklick in die Luft: Kopf wechseln',
    'tooltip.riftverse.gadget.multiversal_multitool_mk3': 'Bohrer, Schraubenschlüssel und Universums-Scanner',
    'tooltip.riftverse.gadget.multiversal_multitool_mk3.bedienung': 'Schleichen + Rechtsklick in die Luft: Kopf wechseln',
    'tooltip.riftverse.gadget.zeitstopper': 'Friert Mobs und Geschosse im Radius 12 für 6 s ein',
    'tooltip.riftverse.gadget.zeitstopper.bedienung': 'Rechtsklick: Zeit anhalten (40.000 FE)',
    'tooltip.riftverse.gadget.mutagen_spritze': 'Zufällige Mutation an einem Mob – 10 % Risiko einer Fehlmutation',
    'tooltip.riftverse.gadget.mutagen_spritze.bedienung': 'Rechtsklick auf einen Mob',
    'tooltip.riftverse.gadget.aufgaben_box': 'Beschwört einen Helfer für eine Aufgabe; nach 5 min zerfällt er explosiv',
    'tooltip.riftverse.gadget.aufgaben_box.bedienung': 'Auf ein Erz: Ader abbauen · auf ein Feld: ernten · in die Luft: kämpfen',
    'message.riftverse.drohne.aufgabe': 'Aufgabe: %s', 'gui.riftverse.drohne.0': 'Aufsammeln', 'gui.riftverse.drohne.1': 'Reichen',
    'gui.riftverse.drohne.2': 'Sortieren', 'message.riftverse.drohne.an': 'Die Drohne sammelt.', 'message.riftverse.drohne.aus': 'Die Drohne ruht.',
    'message.riftverse.drohne.ziel_fehlt': 'Schleichen + Rechtsklick auf eine Kiste (oder Rechtsklick auf einen Spieler).',
    'message.riftverse.drohne.sortiert': '%s Sorten sortiert.', 'message.riftverse.drohne.gereicht': '%s hat dir etwas gereicht.',
    'message.riftverse.drohne.leer': 'Die Drohne hat keinen Strom mehr.',
    **{f'message.riftverse.drohne.sinn.{i}': v for i, v in enumerate(DROHNE_SINN_DE)},
    'message.riftverse.holo': 'Das Hologramm zieht alle Blicke auf sich.',
    'message.riftverse.schrumpf': '%s: Größe %s',
    'message.riftverse.halsband.nicht_zahm': 'Das geht nur bei deinem eigenen gezähmten Tier.',
    'halsband.riftverse.hallo': 'Oh! Ich kann sprechen! Hallo, %s!', 'halsband.riftverse.leer': 'Mir … gehen … die Worte … aus …',
    'halsband.riftverse.gesammelt': 'Ich hab %s Sachen für dich aufgehoben!', 'halsband.riftverse.warnung': 'Vorsicht! Da ist ein %s!',
    'halsband.riftverse.verletzt': 'Du siehst nicht gut aus. Soll ich jemanden holen? Ich kann niemanden holen.',
    **{f'halsband.riftverse.welt.{t}': v for t, v in zip(TYP_IDS, HALSBAND_WELT_DE)},
    **{f'halsband.riftverse.kommentar.{i}': v for i, v in enumerate(HALSBAND_KOMMENTAR_DE)},
    'message.riftverse.phiole.leer': 'Schleichen + Rechtsklick füllt die Phiole mit diesem Ort.',
    'message.riftverse.phiole.gespeichert': 'Die Phiole erinnert sich an diesen Ort.',
    'tooltip.riftverse.phiole.ort': 'Erinnerung: %s bei %s, %s, %s',
    'message.riftverse.tarn.an': 'Tarn-Detektor an', 'message.riftverse.tarn.aus': 'Tarn-Detektor aus',
    'message.riftverse.multitool.modus': 'Kopf: %s', 'gui.riftverse.multitool.0': 'Bohrer', 'gui.riftverse.multitool.1': 'Schlüssel',
    'gui.riftverse.multitool.2': 'Scanner', 'tooltip.riftverse.multitool.stufe': 'Stufe %s von 3',
    'message.riftverse.zeitstopp': 'Die Zeit steht still (%s eingefroren).',
    'message.riftverse.mutation.klein': 'Mutation: geschrumpft', 'message.riftverse.mutation.gross': 'Mutation: gewachsen',
    'message.riftverse.mutation.tempo': 'Mutation: deutlich schneller', 'message.riftverse.mutation.faehigkeit': 'Mutation: %s',
    'message.riftverse.mutation.fehl': 'Fehlmutation! Lauf!',
    # Upgrades
    'tooltip.riftverse.tempo_chip': 'Doppelte Geschwindigkeit, 2,5-facher Verbrauch',
    'tooltip.riftverse.effizienz_chip': '30 % weniger Verbrauch', 'tooltip.riftverse.parallel_chip': 'Zwei Vorgänge gleichzeitig',
    'tooltip.riftverse.transformator_chip': 'Die Maschine verträgt eine Spannungsstufe mehr',
    'tooltip.riftverse.ki_chip': 'Persönlichkeit, +10 % Tempo, gelegentlich eine existenzielle Krise',
    'tooltip.riftverse.chip.einbau': 'Rechtsklick auf eine Maschine baut ihn ein · Multitool-Schlüssel + Schleichen baut aus',
    'message.riftverse.chip.schon_drin': 'Dieser Chip steckt schon drin (oder die Maschine nimmt keine Chips).',
    'message.riftverse.chip.eingebaut': '%s eingebaut (%s Chips)', 'tooltip.riftverse.ki_krise': 'Existenzielle Krise …',
    **{f'ki.riftverse.hallo.{i}': v for i, v in enumerate(KI_HALLO_DE)},
    **{f'ki.riftverse.krise.{i}': v for i, v in enumerate(KI_KRISE_DE)},
    **{f'ki.riftverse.kommentar.{i}': v for i, v in enumerate(KI_KOMMENTAR_DE)},
    **{f'ki.riftverse.tooltip.{i}': v for i, v in enumerate(KI_TOOLTIP_DE)},
    # Rüstung
    'tooltip.riftverse.ruestung.helmet': 'HUD: Universumstyp, Physik-Modifikatoren, Fluidstand der Kanone',
    'tooltip.riftverse.ruestung.chestplate': 'Energiekern versorgt alle Teile, Kanonentank 4.000 mB',
    'tooltip.riftverse.ruestung.leggings': '+10 % Abbaugeschwindigkeit',
    'tooltip.riftverse.ruestung.boots': 'Kein Fallschaden bis 10 Blöcke',
    'tooltip.riftverse.module': 'Module: %s / %s', 'tooltip.riftverse.brusttank': 'Brusttank: %s / %s mB',
    'tooltip.riftverse.modul_teil.head': 'Passt in: Analyse-Visier', 'tooltip.riftverse.modul_teil.chest': 'Passt in: Kernmantel',
    'tooltip.riftverse.modul_teil.legs': 'Passt in: Exo-Rahmen', 'tooltip.riftverse.modul_teil.feet': 'Passt in: Gravi-Stiefel',
    'tooltip.riftverse.modul.analyse_hud': 'Erz-Scan im Umkreis, Lebenspunkte und Werte von Mobs (5 FE/t)',
    'tooltip.riftverse.modul.nachtsicht_linse': 'Nachtsicht ohne Flackern (10 FE/t)',
    'tooltip.riftverse.modul.atemfilter': 'Immun gegen giftige Luft, längere Atemluft unter Wasser (20 FE/t)',
    'tooltip.riftverse.modul.tarn_detektor': 'Wie das Gadget, ohne Hand frei zu machen (Taste H, 50 FE/t)',
    'tooltip.riftverse.modul.universal_uebersetzer': 'Versteht Alien-NPCs (wirkt, sobald die Story-Arcs da sind)',
    'tooltip.riftverse.modul.energieschild': 'Fängt Schaden ab: 1.000 FE pro Schadenspunkt',
    'tooltip.riftverse.modul.not_portal': 'Bei tödlichem Treffer ein Portal nach Hause (200.000 FE + 100 mB, 10 min Abklingzeit)',
    'tooltip.riftverse.modul.kanonen_integration': 'Portale öffnen ohne Kanone in der Hand (Taste B, Schleichen: nach Hause)',
    'tooltip.riftverse.modul.klon_link': 'Beim Tod wacht der Klon auch mit deiner Erfahrung auf',
    'tooltip.riftverse.modul.gadget_halterung': 'Die ersten zwei Gadgets im Inventar per Taste (Z und X)',
    'tooltip.riftverse.modul.universums_kern': 'Nur im perfektionierten Kernmantel: schaltet den Set-Bonus „Multiversale Resonanz“ frei',
    'tooltip.riftverse.modul.servo_verstaerker': '+20 % Abbautempo, Rückstoßresistenz (15 FE/t)',
    'tooltip.riftverse.modul.sprint_booster': 'Beim Sprinten +25 % Tempo und volle Blöcke hoch (10 FE/t)',
    'tooltip.riftverse.modul.gravitationsregler': 'Eigene Gravitation 50 / 100 / 200 % (Taste G, 30 FE/t)',
    'tooltip.riftverse.modul.schrumpf_modul': 'Halbe Größe, passt durch 1-Block-Lücken (Taste N, 100 FE/t)',
    'tooltip.riftverse.modul.fall_daempfer': 'Kein Fallschaden (500 FE pro Landung)',
    'tooltip.riftverse.modul.sprungfeder': '+50 % Sprungkraft (5 FE pro Sprung)',
    'tooltip.riftverse.modul.phasenschritt': 'In der Luft noch einmal springen: 6 Blöcke Kurzteleport (5.000 FE, 3 s)',
    'tooltip.riftverse.modul.haftsohlen': 'Laufen auf Flüssigkeiten, auch auf Säure (20 FE/t, Schleichen taucht ein)',
    'message.riftverse.not_portal': 'Not-Portal! Du wurdest nach Hause gerissen.',
    'message.riftverse.brusttank.voll': 'Der Brusttank ist voll.', 'message.riftverse.brusttank': 'Brusttank: %s / %s mB',
    'message.riftverse.gravitationsregler.0': 'Gravitationsregler aus', 'message.riftverse.gravitationsregler.1': 'Gravitation 50 %',
    'message.riftverse.gravitationsregler.2': 'Gravitation 100 %', 'message.riftverse.gravitationsregler.3': 'Gravitation 200 %',
    'message.riftverse.keine_kanone': 'Keine Portal-Kanone im Inventar.', 'message.riftverse.modul_fehlt': 'Dafür fehlt: %s',
    'message.riftverse.modul.tarn.an': 'Tarn-Detektor an', 'message.riftverse.modul.tarn.aus': 'Tarn-Detektor aus',
    'message.riftverse.modul.schrumpf.an': 'Geschrumpft', 'message.riftverse.modul.schrumpf.aus': 'Normale Größe',
    'message.riftverse.gadget_fehlt': 'Kein %s. Gadget im Inventar.',
    'key.riftverse.tarn': 'Rüstung: Tarn-Detektor', 'key.riftverse.kanone': 'Rüstung: Kanonen-Integration',
    'key.riftverse.gadget_1': 'Rüstung: Gadget 1', 'key.riftverse.gadget_2': 'Rüstung: Gadget 2',
    'key.riftverse.gravitation': 'Rüstung: Gravitationsregler', 'key.riftverse.schrumpf': 'Rüstung: Schrumpf-Modul',
    'hud.riftverse.heimat': 'Heimat-Realität', 'hud.riftverse.physik': 'Gravitation %s · %s',
    'hud.riftverse.kanone': 'Kanone %s / %s mB · Brusttank %s / %s mB', 'hud.riftverse.energie': 'Energie %s FE',
    'hud.riftverse.resonanz': 'Multiversale Resonanz', 'hud.riftverse.erz': '%s ×%s (%s m)', 'hud.riftverse.keine_erze': 'Keine Erze in der Nähe',
}

M6_EN = {
    'item.riftverse.einzweck_drohne': 'Single-Purpose Drone', 'item.riftverse.holo_koeder': 'Holo Decoy',
    'item.riftverse.schrumpfstrahler': 'Shrink Ray', 'item.riftverse.anti_grav_granate': 'Anti-Grav Grenade',
    'item.riftverse.uebersetzer_halsband': 'Translator Collar', 'item.riftverse.erinnerungsphiole': 'Memory Vial',
    'item.riftverse.tarn_detektor': 'Cloak Detector', 'item.riftverse.multiversal_multitool': 'Multiversal Multitool',
    'item.riftverse.multiversal_multitool_mk2': 'Multiversal Multitool Mk II', 'item.riftverse.multiversal_multitool_mk3': 'Multiversal Multitool Mk III',
    'item.riftverse.zeitstopper': 'Time Stopper', 'item.riftverse.mutagen_spritze': 'Mutagen Syringe', 'item.riftverse.aufgaben_box': 'Task Box',
    'item.riftverse.tempo_chip': 'Speed Chip', 'item.riftverse.effizienz_chip': 'Efficiency Chip', 'item.riftverse.parallel_chip': 'Parallel Chip',
    'item.riftverse.transformator_chip': 'Transformer Chip', 'item.riftverse.ki_chip': 'AI Chip',
    'item.riftverse.kalibrier_satz': 'Calibration Kit', 'item.riftverse.kalibrier_satz.lore': 'Material from three universes and a story fragment',
    'item.riftverse.multiversum_legierung': 'Multiverse Alloy',
    'item.riftverse.multiversum_legierung.lore': 'Gravitite, inversion shards, floatstone, biomass, scrap and flux crystal in one ingot',
    'item.riftverse.universums_kern': 'Universe Core',
    **{f'item.riftverse.{t}_{st}': f'{n} ({sn})' for t, n in zip(M6_TEILE, ['Analysis Visor', 'Core Mantle', 'Exo Frame', 'Gravi Boots'])
       for st, sn in zip(M6_STUFEN, ['Blank', 'Calibrated', 'Perfected'])},
    **{f'item.riftverse.modul_{k}': 'Module: ' + v for k, v in zip(M6_MODULE, ['Analysis HUD', 'Night Vision Lens', 'Breath Filter',
       'Cloak Detector', 'Universal Translator', 'Energy Shield', 'Emergency Portal', 'Gun Integration', 'Clone Link', 'Gadget Mount',
       'Servo Amplifier', 'Sprint Booster', 'Gravity Regulator', 'Shrink Module', 'Fall Damper', 'Jump Spring', 'Phase Step', 'Grip Soles'])},
    'entity.riftverse.holo_koeder': 'Holo Decoy', 'entity.riftverse.anti_grav_granate': 'Anti-Grav Grenade',
    'entity.riftverse.fehlmutation': 'Failed Mutation (%s)', 'entity.riftverse.helfer.erz': 'Helper: Ore Vein',
    'entity.riftverse.helfer.ernte': 'Helper: Harvest', 'entity.riftverse.helfer.kampf': 'Helper: Combat',
    'message.riftverse.gadget.leer': '%s is empty – charge it.',
    'tooltip.riftverse.gadget.einzweck_drohne': 'A tiny drone with exactly one purpose',
    'tooltip.riftverse.gadget.einzweck_drohne.bedienung': 'Sneak + right-click: change task · on a chest: do it',
    'tooltip.riftverse.gadget.holo_koeder': 'Projects a hologram of you; mobs attack it for 10 s',
    'tooltip.riftverse.gadget.holo_koeder.bedienung': 'Right-click: hologram where you look (2,000 FE)',
    'tooltip.riftverse.gadget.schrumpfstrahler': 'Shrinks mobs or yourself to 0.5 and 0.25',
    'tooltip.riftverse.gadget.schrumpfstrahler.bedienung': 'Right-click: one step smaller (nothing aimed: yourself) · sneak: back to 1',
    'tooltip.riftverse.gadget.anti_grav_granate': 'All mobs within 6 blocks float helplessly for 8 s',
    'tooltip.riftverse.gadget.anti_grav_granate.bedienung': 'Right-click: throw',
    'tooltip.riftverse.gadget.uebersetzer_halsband': 'Gives a tamed animal a voice and a mind',
    'tooltip.riftverse.gadget.uebersetzer_halsband.bedienung': 'Right-click your pet: put on · sneak + empty hand: take off',
    'tooltip.riftverse.gadget.erinnerungsphiole': 'Stores a place; drinking takes you back',
    'tooltip.riftverse.gadget.erinnerungsphiole.bedienung': 'Empty: sneak + right-click stores · filled: drink',
    'tooltip.riftverse.gadget.tarn_detektor': 'Reveals invisible and cloaked beings',
    'tooltip.riftverse.gadget.tarn_detektor.bedienung': 'Right-click: on/off · 1,000 FE per second in hand',
    'tooltip.riftverse.gadget.multiversal_multitool': 'Drill, wrench and universe scanner',
    'tooltip.riftverse.gadget.multiversal_multitool.bedienung': 'Sneak + right-click in the air: switch head',
    'tooltip.riftverse.gadget.multiversal_multitool_mk2': 'Drill, wrench and universe scanner',
    'tooltip.riftverse.gadget.multiversal_multitool_mk2.bedienung': 'Sneak + right-click in the air: switch head',
    'tooltip.riftverse.gadget.multiversal_multitool_mk3': 'Drill, wrench and universe scanner',
    'tooltip.riftverse.gadget.multiversal_multitool_mk3.bedienung': 'Sneak + right-click in the air: switch head',
    'tooltip.riftverse.gadget.zeitstopper': 'Freezes mobs and projectiles within 12 blocks for 6 s',
    'tooltip.riftverse.gadget.zeitstopper.bedienung': 'Right-click: stop time (40,000 FE)',
    'tooltip.riftverse.gadget.mutagen_spritze': 'Random mutation on a mob – 10 % risk of a failed mutation',
    'tooltip.riftverse.gadget.mutagen_spritze.bedienung': 'Right-click a mob',
    'tooltip.riftverse.gadget.aufgaben_box': 'Summons a helper for one task; after 5 min it falls apart explosively',
    'tooltip.riftverse.gadget.aufgaben_box.bedienung': 'On ore: mine the vein · on a field: harvest · in the air: fight',
    'message.riftverse.drohne.aufgabe': 'Task: %s', 'gui.riftverse.drohne.0': 'Pick up', 'gui.riftverse.drohne.1': 'Hand over',
    'gui.riftverse.drohne.2': 'Sort', 'message.riftverse.drohne.an': 'The drone is collecting.', 'message.riftverse.drohne.aus': 'The drone rests.',
    'message.riftverse.drohne.ziel_fehlt': 'Sneak + right-click a chest (or right-click a player).',
    'message.riftverse.drohne.sortiert': 'Sorted %s kinds.', 'message.riftverse.drohne.gereicht': '%s handed you something.',
    'message.riftverse.drohne.leer': 'The drone is out of power.',
    **{f'message.riftverse.drohne.sinn.{i}': v for i, v in enumerate(DROHNE_SINN_EN)},
    'message.riftverse.holo': 'The hologram draws all eyes.', 'message.riftverse.schrumpf': '%s: size %s',
    'message.riftverse.halsband.nicht_zahm': 'Only works on your own tamed animal.',
    'halsband.riftverse.hallo': 'Oh! I can talk! Hello, %s!', 'halsband.riftverse.leer': 'I am … running out … of words …',
    'halsband.riftverse.gesammelt': 'I picked up %s things for you!', 'halsband.riftverse.warnung': 'Watch out! There is a %s!',
    'halsband.riftverse.verletzt': 'You do not look well. Should I get someone? I cannot get anyone.',
    **{f'halsband.riftverse.welt.{t}': v for t, v in zip(TYP_IDS, HALSBAND_WELT_EN)},
    **{f'halsband.riftverse.kommentar.{i}': v for i, v in enumerate(HALSBAND_KOMMENTAR_EN)},
    'message.riftverse.phiole.leer': 'Sneak + right-click fills the vial with this place.',
    'message.riftverse.phiole.gespeichert': 'The vial remembers this place.', 'tooltip.riftverse.phiole.ort': 'Memory: %s at %s, %s, %s',
    'message.riftverse.tarn.an': 'Cloak detector on', 'message.riftverse.tarn.aus': 'Cloak detector off',
    'message.riftverse.multitool.modus': 'Head: %s', 'gui.riftverse.multitool.0': 'Drill', 'gui.riftverse.multitool.1': 'Wrench',
    'gui.riftverse.multitool.2': 'Scanner', 'tooltip.riftverse.multitool.stufe': 'Stage %s of 3',
    'message.riftverse.zeitstopp': 'Time stands still (%s frozen).',
    'message.riftverse.mutation.klein': 'Mutation: shrunk', 'message.riftverse.mutation.gross': 'Mutation: grown',
    'message.riftverse.mutation.tempo': 'Mutation: much faster', 'message.riftverse.mutation.faehigkeit': 'Mutation: %s',
    'message.riftverse.mutation.fehl': 'Failed mutation! Run!',
    'tooltip.riftverse.tempo_chip': 'Double speed, 2.5× power draw', 'tooltip.riftverse.effizienz_chip': '30 % less power draw',
    'tooltip.riftverse.parallel_chip': 'Two operations at once', 'tooltip.riftverse.transformator_chip': 'The machine tolerates one voltage tier more',
    'tooltip.riftverse.ki_chip': 'Personality, +10 % speed, the occasional existential crisis',
    'tooltip.riftverse.chip.einbau': 'Right-click a machine to install · multitool wrench + sneak removes',
    'message.riftverse.chip.schon_drin': 'This chip is already installed (or the machine takes no chips).',
    'message.riftverse.chip.eingebaut': '%s installed (%s chips)', 'tooltip.riftverse.ki_krise': 'Existential crisis …',
    **{f'ki.riftverse.hallo.{i}': v for i, v in enumerate(KI_HALLO_EN)},
    **{f'ki.riftverse.krise.{i}': v for i, v in enumerate(KI_KRISE_EN)},
    **{f'ki.riftverse.kommentar.{i}': v for i, v in enumerate(KI_KOMMENTAR_EN)},
    **{f'ki.riftverse.tooltip.{i}': v for i, v in enumerate(KI_TOOLTIP_EN)},
    'tooltip.riftverse.ruestung.helmet': 'HUD: universe type, physics modifiers, gun fluid level',
    'tooltip.riftverse.ruestung.chestplate': 'Energy core supplies all pieces, gun tank 4,000 mB',
    'tooltip.riftverse.ruestung.leggings': '+10 % mining speed', 'tooltip.riftverse.ruestung.boots': 'No fall damage up to 10 blocks',
    'tooltip.riftverse.module': 'Modules: %s / %s', 'tooltip.riftverse.brusttank': 'Chest tank: %s / %s mB',
    'tooltip.riftverse.modul_teil.head': 'Fits: Analysis Visor', 'tooltip.riftverse.modul_teil.chest': 'Fits: Core Mantle',
    'tooltip.riftverse.modul_teil.legs': 'Fits: Exo Frame', 'tooltip.riftverse.modul_teil.feet': 'Fits: Gravi Boots',
    'tooltip.riftverse.modul.analyse_hud': 'Ore scan nearby, health and stats of mobs (5 FE/t)',
    'tooltip.riftverse.modul.nachtsicht_linse': 'Night vision without flicker (10 FE/t)',
    'tooltip.riftverse.modul.atemfilter': 'Immune to toxic air, longer breath under water (20 FE/t)',
    'tooltip.riftverse.modul.tarn_detektor': 'Like the gadget, hands free (key H, 50 FE/t)',
    'tooltip.riftverse.modul.universal_uebersetzer': 'Understands alien NPCs (works once story arcs exist)',
    'tooltip.riftverse.modul.energieschild': 'Absorbs damage: 1,000 FE per point',
    'tooltip.riftverse.modul.not_portal': 'On a fatal hit, a portal home (200,000 FE + 100 mB, 10 min cooldown)',
    'tooltip.riftverse.modul.kanonen_integration': 'Open portals without the gun in hand (key B, sneak: home)',
    'tooltip.riftverse.modul.klon_link': 'On death your clone also keeps your experience',
    'tooltip.riftverse.modul.gadget_halterung': 'The first two gadgets in your inventory via keys (Z and X)',
    'tooltip.riftverse.modul.universums_kern': 'Perfected Core Mantle only: unlocks the "Multiversal Resonance" set bonus',
    'tooltip.riftverse.modul.servo_verstaerker': '+20 % mining speed, knockback resistance (15 FE/t)',
    'tooltip.riftverse.modul.sprint_booster': 'While sprinting +25 % speed and step up full blocks (10 FE/t)',
    'tooltip.riftverse.modul.gravitationsregler': 'Own gravity 50 / 100 / 200 % (key G, 30 FE/t)',
    'tooltip.riftverse.modul.schrumpf_modul': 'Half size, fits through 1-block gaps (key N, 100 FE/t)',
    'tooltip.riftverse.modul.fall_daempfer': 'No fall damage (500 FE per landing)',
    'tooltip.riftverse.modul.sprungfeder': '+50 % jump strength (5 FE per jump)',
    'tooltip.riftverse.modul.phasenschritt': 'Jump again mid-air: 6-block short teleport (5,000 FE, 3 s)',
    'tooltip.riftverse.modul.haftsohlen': 'Walk on fluids, even acid (20 FE/t, sneak to sink)',
    'message.riftverse.not_portal': 'Emergency portal! You were pulled home.',
    'message.riftverse.brusttank.voll': 'The chest tank is full.', 'message.riftverse.brusttank': 'Chest tank: %s / %s mB',
    'message.riftverse.gravitationsregler.0': 'Gravity regulator off', 'message.riftverse.gravitationsregler.1': 'Gravity 50 %',
    'message.riftverse.gravitationsregler.2': 'Gravity 100 %', 'message.riftverse.gravitationsregler.3': 'Gravity 200 %',
    'message.riftverse.keine_kanone': 'No portal gun in your inventory.', 'message.riftverse.modul_fehlt': 'Missing: %s',
    'message.riftverse.modul.tarn.an': 'Cloak detector on', 'message.riftverse.modul.tarn.aus': 'Cloak detector off',
    'message.riftverse.modul.schrumpf.an': 'Shrunk', 'message.riftverse.modul.schrumpf.aus': 'Normal size',
    'message.riftverse.gadget_fehlt': 'No gadget #%s in your inventory.',
    'key.riftverse.tarn': 'Armor: cloak detector', 'key.riftverse.kanone': 'Armor: gun integration',
    'key.riftverse.gadget_1': 'Armor: gadget 1', 'key.riftverse.gadget_2': 'Armor: gadget 2',
    'key.riftverse.gravitation': 'Armor: gravity regulator', 'key.riftverse.schrumpf': 'Armor: shrink module',
    'hud.riftverse.heimat': 'Home reality', 'hud.riftverse.physik': 'Gravity %s · %s',
    'hud.riftverse.kanone': 'Gun %s / %s mB · chest tank %s / %s mB', 'hud.riftverse.energie': 'Energy %s FE',
    'hud.riftverse.resonanz': 'Multiversal Resonance', 'hud.riftverse.erz': '%s ×%s (%s m)', 'hud.riftverse.keine_erze': 'No ores nearby',
}


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
    m3_models()
    m3_loot()
    m3_tags()
    m3_recipes()
    m3_universes()
    m4_models()
    m4_loot()
    m4_tags()
    m4_recipes()
    m4_universes()
    m5_models()
    m5_loot()
    m5_tags()
    m5_recipes()
    m5_universes()
    m6_models()
    m6_tags()
    m6_recipes()
    lang(pack_de)
    print('fertig')
