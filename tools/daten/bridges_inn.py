"""Daten für Bridge's Inn (Meilenstein 10 der neuen Universen): Modelle, Blockstates, Loot, Tags, Rezepte, Biom,
Dimension und Texte.

Aufruf aus dem Repo-Wurzelordner (läuft auch am Ende von generate.py mit):
    python3 tools/daten/bridges_inn.py
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
from generate import (ASSETS, DATA, NS, write, rl, tex, block_model, item_from_block, item_generated, blockstate,  # noqa: E402
                      variant, loot_self, tag, shaped_vanilla, shapeless_vanilla, werkbank_shaped, werkbank_shapeless,
                      spawn, mood)

PFLANZEN = {
    'sternenblume_weiss': 'Weiße Sternenblume', 'sternenblume_gold': 'Goldene Sternenblume',
    'leuchtglocke_blau': 'Blaue Leuchtglocke', 'leuchtglocke_violett': 'Violette Leuchtglocke',
    'leuchtglocke_tuerkis': 'Türkise Leuchtglocke', 'leuchtglocke_rosa': 'Rosa Leuchtglocke',
    'mondkraut': 'Mondkraut', 'funkenmohn_rot': 'Roter Funkenmohn', 'funkenmohn_orange': 'Orangener Funkenmohn',
    'zauberpilz_violett': 'Violetter Zauberpilz', 'zauberpilz_gruen': 'Grüner Zauberpilz',
}
PFLANZEN_EN = {
    'sternenblume_weiss': 'White Starbloom', 'sternenblume_gold': 'Golden Starbloom',
    'leuchtglocke_blau': 'Blue Glowbell', 'leuchtglocke_violett': 'Violet Glowbell',
    'leuchtglocke_tuerkis': 'Turquoise Glowbell', 'leuchtglocke_rosa': 'Pink Glowbell',
    'mondkraut': 'Moonwort', 'funkenmohn_rot': 'Red Sparkpoppy', 'funkenmohn_orange': 'Orange Sparkpoppy',
    'zauberpilz_violett': 'Violet Spellshroom', 'zauberpilz_gruen': 'Green Spellshroom',
}
# Pflanze -> Farbstoff
FARBSTOFF = {
    'sternenblume_weiss': 'white_dye', 'sternenblume_gold': 'yellow_dye', 'leuchtglocke_blau': 'blue_dye',
    'leuchtglocke_violett': 'purple_dye', 'leuchtglocke_tuerkis': 'cyan_dye', 'leuchtglocke_rosa': 'pink_dye',
    'mondkraut': 'light_blue_dye', 'funkenmohn_rot': 'red_dye', 'funkenmohn_orange': 'orange_dye',
}
DEKO_WUERFEL = ['violette_dachziegel', 'fachwerk', 'fachwerk_kreuz', 'buntes_kopfsteinpflaster', 'funkelholz_bretter']
ITEMS = ['sternenstaub', 'sternen_chip', 'sternenbrot', 'glimmhaar', 'flausch', 'falterstaub']
EIER = ['gleeb', 'glimmhase', 'pusteling', 'schimmerfalter']

LEUCHTEN = {'block_light': 15, 'sky_light': 15}


# =============================================================================
# Modelle
# =============================================================================
def kreuz(name, texture, leuchtet):
    """Kreuzmodell wie minecraft:block/cross, wahlweise voll leuchtend."""
    extra = {'neoforge_data': LEUCHTEN} if leuchtet else {}
    flaeche = {'uv': [0, 0, 16, 16], 'texture': '#cross'}
    block_model(name, 'minecraft:block/block', {'cross': tex(texture), 'particle': tex(texture)},
                ambientocclusion=False, render_type='minecraft:cutout', elements=[
                    {'from': [0.8, 0, 8], 'to': [15.2, 16, 8], 'shade': False, **extra,
                     'rotation': {'origin': [8, 8, 8], 'axis': 'y', 'angle': 45, 'rescale': True},
                     'faces': {'north': flaeche, 'south': flaeche}},
                    {'from': [8, 0, 0.8], 'to': [8, 16, 15.2], 'shade': False, **extra,
                     'rotation': {'origin': [8, 8, 8], 'axis': 'y', 'angle': 45, 'rescale': True},
                     'faces': {'west': flaeche, 'east': flaeche}}])


def leuchtend(name):
    """Pflanze mit Zuständen leuchtet/angeregt: aus = normales Modell, sonst das leuchtende."""
    kreuz(name, name, False)
    kreuz(name + '_an', name + '_an', True)
    v = {}
    for l in ('false', 'true'):
        for a in ('false', 'true'):
            v[f'angeregt={a},leuchtet={l}'] = variant(name + ('_an' if 'true' in (l, a) else ''))
    blockstate(name, v)
    write(os.path.join(ASSETS, 'models', 'item', name + '.json'), {
        'parent': 'minecraft:item/generated', 'textures': {'layer0': f'{NS}:block/{name}_an'}})


def treppen(name, textur):
    t = {'bottom': tex(textur), 'top': tex(textur), 'side': tex(textur)}
    block_model(name, 'minecraft:block/stairs', t)
    block_model(name + '_inner', 'minecraft:block/inner_stairs', t)
    block_model(name + '_outer', 'minecraft:block/outer_stairs', t)
    v = {}
    dreh = {'east': 0, 'south': 90, 'west': 180, 'north': 270}
    for facing, y in dreh.items():
        for half in ('bottom', 'top'):
            for shape in ('straight', 'inner_left', 'inner_right', 'outer_left', 'outer_right'):
                model = name + ('_inner' if shape.startswith('inner') else '_outer' if shape.startswith('outer') else '')
                yy = y
                if shape.endswith('left'):
                    yy = (y + 270) % 360
                x = 0
                if half == 'top':
                    x = 180
                    if shape != 'straight':
                        yy = (yy + 90) % 360
                e = {'model': f'{NS}:block/{model}'}
                if x:
                    e['x'] = x
                if yy:
                    e['y'] = yy
                if x or yy:
                    e['uvlock'] = True
                v[f'facing={facing},half={half},shape={shape}'] = e
    blockstate(name, v)
    item_from_block(name)


def stufe(name, textur, voll):
    t = {'bottom': tex(textur), 'top': tex(textur), 'side': tex(textur)}
    block_model(name, 'minecraft:block/slab', t)
    block_model(name + '_top', 'minecraft:block/slab_top', t)
    blockstate(name, {'type=bottom': variant(name), 'type=top': variant(name + '_top'), 'type=double': variant(voll)})
    item_from_block(name)


def modelle():
    for n in PFLANZEN:
        leuchtend(n)
    leuchtend('lichtranke')
    kreuz('kraeuterbuendel', 'kraeuterbuendel', False)
    blockstate('kraeuterbuendel', {'': variant('kraeuterbuendel')})
    write(os.path.join(ASSETS, 'models', 'item', 'kraeuterbuendel.json'), {
        'parent': 'minecraft:item/generated', 'textures': {'layer0': f'{NS}:block/kraeuterbuendel'}})

    for n in DEKO_WUERFEL:
        block_model(n, 'minecraft:block/cube_all', {'all': tex(n)})
        blockstate(n, {'': variant(n)})
        item_from_block(n)
    treppen('violette_dachziegel_treppe', 'violette_dachziegel')
    stufe('violette_dachziegel_stufe', 'violette_dachziegel', 'violette_dachziegel')

    block_model('sternglas', 'minecraft:block/cube_all', {'all': tex('sternglas')}, render_type='minecraft:translucent')
    blockstate('sternglas', {'': variant('sternglas')})
    item_from_block('sternglas')

    block_model('funkelholz_stamm', 'minecraft:block/cube_column',
                {'end': tex('funkelholz_stamm_top'), 'side': tex('funkelholz_stamm')})
    block_model('funkelholz_stamm_horizontal', 'minecraft:block/cube_column_horizontal',
                {'end': tex('funkelholz_stamm_top'), 'side': tex('funkelholz_stamm')})
    blockstate('funkelholz_stamm', {'axis=y': variant('funkelholz_stamm'),
                                    'axis=z': variant('funkelholz_stamm_horizontal', x=90),
                                    'axis=x': variant('funkelholz_stamm_horizontal', x=90, y=90)})
    item_from_block('funkelholz_stamm')

    block_model('funkellaub', 'minecraft:block/leaves', {'all': tex('funkellaub')}, render_type='minecraft:cutout_mipped')
    blockstate('funkellaub', {'': variant('funkellaub')})
    item_from_block('funkellaub')

    # Schwebelaterne: leuchtende Laterne ohne Aufhängung
    lat = {'texture': '#l'}
    block_model('schwebelaterne', 'minecraft:block/block', {'l': tex('schwebelaterne'), 'particle': tex('schwebelaterne')},
                render_type='minecraft:cutout', elements=[{
                    'from': [4, 2, 4], 'to': [12, 13, 12], 'neoforge_data': LEUCHTEN,
                    'faces': {d: {**lat, 'uv': [4, 2, 12, 13]} for d in ('north', 'south', 'east', 'west')} | {
                        'up': {**lat, 'uv': [4, 4, 12, 12]}, 'down': {**lat, 'uv': [4, 4, 12, 12]}}}])
    blockstate('schwebelaterne', {'': variant('schwebelaterne')})
    write(os.path.join(ASSETS, 'models', 'item', 'schwebelaterne.json'), {
        'parent': 'minecraft:item/generated', 'textures': {'layer0': f'{NS}:block/schwebelaterne'}})

    kreuz('feenlicht', 'feenlicht', True)
    blockstate('feenlicht', {'': variant('feenlicht')})
    write(os.path.join(ASSETS, 'models', 'item', 'feenlicht.json'), {
        'parent': 'minecraft:item/generated', 'textures': {'layer0': f'{NS}:block/feenlicht'}})

    for n in ITEMS:
        item_generated(n)
    for e in EIER:
        write(os.path.join(ASSETS, 'models', 'item', e + '_spawn_egg.json'), {'parent': 'minecraft:item/template_spawn_egg'})


# =============================================================================
# Loot
# =============================================================================
def schere_oder_behutsam():
    return {'condition': 'minecraft:any_of', 'terms': [
        {'condition': 'minecraft:match_tool', 'predicate': {'items': 'minecraft:shears'}},
        {'condition': 'minecraft:match_tool', 'predicate': {'predicates': {'minecraft:enchantments': [
            {'enchantments': 'minecraft:silk_touch', 'levels': {'min': 1}}]}}}]}


def tier_loot(name, item, lo, hi):
    write(os.path.join(DATA, NS, 'loot_table', 'entities', name + '.json'), {
        'type': 'minecraft:entity',
        'pools': [{'rolls': 1, 'bonus_rolls': 0, 'entries': [{'type': 'minecraft:item', 'name': rl(item), 'functions': [
            {'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': lo, 'max': hi}},
            {'function': 'minecraft:enchanted_count_increase', 'enchantment': 'minecraft:looting',
             'count': {'type': 'minecraft:uniform', 'min': 0, 'max': 1}}]}]}],
        'random_sequence': f'{NS}:entities/{name}'})


def eintrag(item, gewicht, lo=1, hi=1):
    e = {'type': 'minecraft:item', 'name': rl(item), 'weight': gewicht}
    if (lo, hi) != (1, 1):
        e['functions'] = [{'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': lo, 'max': hi}}]
    return e


def kiste(name, rolls, eintraege):
    write(os.path.join(DATA, NS, 'loot_table', 'chests', name + '.json'), {
        'type': 'minecraft:chest',
        'pools': [{'rolls': rolls, 'bonus_rolls': 0, 'entries': eintraege}],
        'random_sequence': f'{NS}:chests/{name}'})


def loot():
    for n in list(PFLANZEN) + ['lichtranke', 'kraeuterbuendel'] + DEKO_WUERFEL + [
            'violette_dachziegel_treppe', 'sternglas', 'funkelholz_stamm', 'schwebelaterne', 'feenlicht']:
        loot_self(n)
    # Stufe: doppelt = 2
    write(os.path.join(DATA, NS, 'loot_table', 'blocks', 'violette_dachziegel_stufe.json'), {
        'type': 'minecraft:block',
        'pools': [{'rolls': 1, 'bonus_rolls': 0, 'entries': [{'type': 'minecraft:item', 'name': rl('violette_dachziegel_stufe'),
                   'functions': [{'function': 'minecraft:set_count', 'count': 2, 'add': False, 'conditions': [{
                       'condition': 'minecraft:block_state_property', 'block': rl('violette_dachziegel_stufe'),
                       'properties': {'type': 'double'}}]}, {'function': 'minecraft:explosion_decay'}]}]}],
        'random_sequence': f'{NS}:blocks/violette_dachziegel_stufe'})
    # Funkellaub: mit Schere oder Behutsamkeit das Laub, sonst manchmal Stöcke – und ganz selten Sternenstaub
    write(os.path.join(DATA, NS, 'loot_table', 'blocks', 'funkellaub.json'), {
        'type': 'minecraft:block',
        'pools': [
            {'rolls': 1, 'bonus_rolls': 0, 'entries': [{'type': 'minecraft:alternatives', 'children': [
                {'type': 'minecraft:item', 'name': rl('funkellaub'), 'conditions': [schere_oder_behutsam()]},
                {'type': 'minecraft:item', 'name': 'minecraft:stick', 'conditions': [
                    {'condition': 'minecraft:survives_explosion'},
                    {'condition': 'minecraft:table_bonus', 'enchantment': 'minecraft:fortune',
                     'chances': [0.02, 0.022222223, 0.025, 0.033333335, 0.1]}]}]}]},
            {'rolls': 1, 'bonus_rolls': 0, 'conditions': [{'condition': 'minecraft:inverted', 'term': schere_oder_behutsam()},
                                                          {'condition': 'minecraft:random_chance', 'chance': 0.01}],
             'entries': [{'type': 'minecraft:item', 'name': rl('sternenstaub')}]}],
        'random_sequence': f'{NS}:blocks/funkellaub'})

    tier_loot('glimmhase', 'glimmhaar', 1, 2)
    tier_loot('pusteling', 'flausch', 1, 3)
    tier_loot('schimmerfalter', 'falterstaub', 1, 2)

    kiste('gleeb_haus', {'type': 'minecraft:uniform', 'min': 2, 'max': 5}, [
        eintrag('sternenstaub', 10, 1, 3), eintrag('sternenbrot', 8, 1, 2), eintrag('minecraft:bread', 10, 1, 3),
        eintrag('minecraft:cookie', 8, 2, 6), eintrag('kraeuterbuendel', 6, 1, 2), eintrag('mondkraut', 6, 1, 3),
        eintrag('sternenblume_weiss', 5, 1, 3), eintrag('leuchtglocke_blau', 5, 1, 3), eintrag('feenlicht', 3, 1, 2),
        eintrag('minecraft:amethyst_shard', 4, 1, 3), eintrag('minecraft:book', 4), eintrag('minecraft:string', 5, 1, 4),
        eintrag('flausch', 4, 1, 3), eintrag('minecraft:emerald', 2)])
    kiste('irrlicht', {'type': 'minecraft:uniform', 'min': 3, 'max': 6}, [
        eintrag('sternenstaub', 14, 2, 5), eintrag('sternenbrot', 8, 1, 3), eintrag('feenlicht', 6, 1, 3),
        eintrag('schwebelaterne', 4, 1, 2), eintrag('zauberpilz_violett', 6, 1, 3), eintrag('zauberpilz_gruen', 6, 1, 3),
        eintrag('sternenblume_gold', 6, 1, 3), eintrag('leuchtglocke_tuerkis', 6, 1, 3), eintrag('falterstaub', 6, 1, 3),
        eintrag('minecraft:amethyst_shard', 6, 2, 5), eintrag('minecraft:experience_bottle', 4, 1, 3),
        eintrag('minecraft:emerald', 3, 1, 2), eintrag('sternen_chip', 1)])


# =============================================================================
# Tags
# =============================================================================
def tags():
    tag('block', 'minecraft:mineable/pickaxe', ['violette_dachziegel', 'violette_dachziegel_treppe', 'violette_dachziegel_stufe',
                                                'buntes_kopfsteinpflaster', 'schwebelaterne'])
    tag('block', 'minecraft:mineable/axe', ['fachwerk', 'fachwerk_kreuz', 'funkelholz_stamm', 'funkelholz_bretter'])
    tag('block', 'minecraft:mineable/hoe', ['funkellaub'])
    tag('block', 'minecraft:leaves', ['funkellaub'])
    tag('block', 'minecraft:logs_that_burn', ['funkelholz_stamm'])
    tag('block', 'minecraft:planks', ['funkelholz_bretter'])
    tag('block', 'minecraft:stairs', ['violette_dachziegel_treppe'])
    tag('block', 'minecraft:slabs', ['violette_dachziegel_stufe'])
    tag('block', 'minecraft:climbable', ['lichtranke'])
    tag('block', 'minecraft:small_flowers', [n for n in PFLANZEN if not n.startswith('zauberpilz')])
    tag('item', 'minecraft:leaves', ['funkellaub'])
    tag('item', 'minecraft:logs_that_burn', ['funkelholz_stamm'])
    tag('item', 'minecraft:planks', ['funkelholz_bretter'])
    tag('item', 'minecraft:stairs', ['violette_dachziegel_treppe'])
    tag('item', 'minecraft:slabs', ['violette_dachziegel_stufe'])
    tag('item', 'minecraft:small_flowers', [n for n in PFLANZEN if not n.startswith('zauberpilz')])


# =============================================================================
# Rezepte
# =============================================================================
def rezepte():
    p = 'bridges_inn/'
    shaped_vanilla(p + 'sternglas', [' G ', 'GSG', ' G '], {'G': 'minecraft:glass', 'S': 'sternenstaub'}, 'sternglas', 4)
    shapeless_vanilla(p + 'feenlicht', ['glimmhaar', 'sternenstaub', 'minecraft:glowstone_dust'], 'feenlicht', 2)
    shapeless_vanilla(p + 'schwebelaterne', ['minecraft:lantern', 'sternenstaub', 'falterstaub'], 'schwebelaterne')
    shapeless_vanilla(p + 'sternenbrot', ['minecraft:bread', 'sternenstaub'], 'sternenbrot')
    shapeless_vanilla(p + 'sternenstaub_aus_falterstaub', ['falterstaub'] * 4, 'sternenstaub')
    shapeless_vanilla(p + 'violette_dachziegel', ['minecraft:bricks', 'minecraft:bricks', 'minecraft:purple_dye'],
                      'violette_dachziegel', 2)
    shaped_vanilla(p + 'violette_dachziegel_treppe', ['D  ', 'DD ', 'DDD'], {'D': 'violette_dachziegel'}, 'violette_dachziegel_treppe', 4)
    shaped_vanilla(p + 'violette_dachziegel_stufe', ['DDD'], {'D': 'violette_dachziegel'}, 'violette_dachziegel_stufe', 6)
    shapeless_vanilla(p + 'fachwerk', ['minecraft:white_terracotta', 'minecraft:white_terracotta', 'minecraft:stick',
                                       'minecraft:stick'], 'fachwerk', 2)
    shapeless_vanilla(p + 'fachwerk_kreuz', ['fachwerk', 'minecraft:stick'], 'fachwerk_kreuz')
    shapeless_vanilla(p + 'fachwerk_aus_kreuz', ['fachwerk_kreuz'], 'fachwerk')
    shapeless_vanilla(p + 'buntes_kopfsteinpflaster', ['minecraft:cobblestone'] * 4 + ['minecraft:red_dye', 'minecraft:light_blue_dye'],
                      'buntes_kopfsteinpflaster', 4)
    shapeless_vanilla(p + 'funkelholz_bretter', ['funkelholz_stamm'], 'funkelholz_bretter', 4)
    shapeless_vanilla(p + 'kraeuterbuendel', ['minecraft:string', 'mondkraut', 'mondkraut'], 'kraeuterbuendel')
    shapeless_vanilla(p + 'lichtranke', ['minecraft:vine', 'glimmhaar'], 'lichtranke', 2)
    shapeless_vanilla(p + 'weisse_wolle_aus_flausch', ['flausch'] * 4, 'minecraft:white_wool')
    for pflanze, farbe in FARBSTOFF.items():
        shapeless_vanilla(p + 'farbstoff_' + pflanze, [pflanze], 'minecraft:' + farbe)
    # Sternen-Chip an der Garagen-Werkbank; damit wird der Tempo-Chip günstiger (ohne Xenit-Spule)
    werkbank_shapeless('sternen_chip', ['sternenstaub'] * 4 + ['basis_schaltkreis'], 'sternen_chip', loetzinn=1)
    werkbank_shaped('tempo_chip_sternen', ['RZR', 'CSC', 'RZR'], {'R': 'minecraft:redstone', 'Z': 'bronze_zahnrad',
                                                                 'C': 'basis_schaltkreis', 'S': 'sternen_chip'},
                    'tempo_chip', loetzinn=1)


# =============================================================================
# Biom und Dimension
# =============================================================================
def welt():
    write(os.path.join(DATA, NS, 'worldgen', 'biome', 'bridges_inn.json'), {
        'has_precipitation': True, 'temperature': 0.75, 'downfall': 0.5,
        'effects': {'sky_color': 0x8ED8FF, 'fog_color': 0xE6D8FF, 'water_color': 0x3FB6FF, 'water_fog_color': 0x1E5A9A,
                    'grass_color': 0x6FE07A, 'foliage_color': 0x5FD67A, 'mood_sound': mood(),
                    'particle': {'options': {'type': 'minecraft:glow'}, 'probability': 0.0012},
                    'music': {'sound': 'minecraft:music.overworld.cherry_grove', 'min_delay': 12000, 'max_delay': 24000,
                              'replace_current_music': False}},
        'spawners': {'monster': [], 'creature': [spawn(f'{NS}:glimmhase', 10, 2, 4), spawn(f'{NS}:pusteling', 8, 2, 3),
                                                 spawn(f'{NS}:schimmerfalter', 6, 2, 4)],
                     'ambient': [], 'axolotls': [], 'underground_water_creature': [],
                     'water_creature': [], 'water_ambient': [spawn('minecraft:tropical_fish', 10, 3, 6)], 'misc': []},
        'spawn_costs': {}, 'carvers': {'air': [], 'liquid': []}, 'features': []})
    dim = 'magisches_dorf'
    write(os.path.join(DATA, NS, 'dimension_type', dim + '.json'), {
        'ultrawarm': False, 'natural': True, 'piglin_safe': False, 'respawn_anchor_works': False, 'bed_works': True,
        'has_raids': False, 'has_skylight': True, 'has_ceiling': False, 'coordinate_scale': 1.0, 'ambient_light': 0.05,
        'logical_height': 384, 'effects': 'minecraft:overworld', 'infiniburn': '#minecraft:infiniburn_overworld',
        'min_y': -64, 'height': 384, 'monster_spawn_light_level': 0, 'monster_spawn_block_light_limit': 0})
    write(os.path.join(DATA, NS, 'dimension', dim + '.json'), {
        'type': rl(dim),
        'generator': {'type': rl('multiverse'), 'universe': dim,
                      'biome_source': {'type': rl('universe'), 'universe': dim, 'biomes': [rl('bridges_inn')]}}})


# =============================================================================
# Texte
# =============================================================================
DE = {
    'universe.riftverse.magisches_dorf': "Bridge's Inn",
    'dimension.riftverse.magisches_dorf': "Bridge's Inn",
    'biome.riftverse.bridges_inn': "Bridge's Inn",
    'einzigartig.riftverse.magisches_dorf.erster_besuch': 'Ups … dieses Universum fühlt sich anders an. Etwas Freundliches zieht dich hierher, und es lässt dich nicht mehr los.',
    'einzigartig.riftverse.magisches_dorf.rueckkehr.1': 'Die Laternen brennen noch für dich.',
    'einzigartig.riftverse.magisches_dorf.rueckkehr.2': "Bridge's Inn hat dich vermisst.",
    'einzigartig.riftverse.magisches_dorf.rueckkehr.3': 'Irgendwo backt jemand Sternenbrot.',
    'einzigartig.riftverse.magisches_dorf.rueckkehr.4': 'Willkommen zurück, Reisender.',
    'halsband.riftverse.welt.magisches_dorf': 'Hier riecht es nach Kuchen und Blumen. Bleiben wir für immer?',
    'block.riftverse.lichtranke': 'Lichtranke', 'block.riftverse.kraeuterbuendel': 'Kräuterbündel',
    'block.riftverse.violette_dachziegel': 'Violette Dachziegel', 'block.riftverse.violette_dachziegel_treppe': 'Violette Dachziegeltreppe',
    'block.riftverse.violette_dachziegel_stufe': 'Violette Dachziegelstufe', 'block.riftverse.fachwerk': 'Fachwerk',
    'block.riftverse.fachwerk_kreuz': 'Fachwerk mit Kreuz', 'block.riftverse.buntes_kopfsteinpflaster': 'Buntes Kopfsteinpflaster',
    'block.riftverse.sternglas': 'Sternglas', 'block.riftverse.funkelholz_stamm': 'Funkelholzstamm',
    'block.riftverse.funkelholz_bretter': 'Funkelholzbretter', 'block.riftverse.funkellaub': 'Funkellaub',
    'block.riftverse.schwebelaterne': 'Schwebelaterne', 'block.riftverse.feenlicht': 'Feenlicht',
    'item.riftverse.sternenstaub': 'Sternenstaub', 'item.riftverse.sternen_chip': 'Sternen-Chip',
    'item.riftverse.sternenbrot': 'Sternenbrot', 'item.riftverse.glimmhaar': 'Glimmhaar', 'item.riftverse.flausch': 'Flausch',
    'item.riftverse.falterstaub': 'Falterstaub',
    'item.riftverse.gleeb_spawn_egg': 'Gleeb-Spawn-Ei', 'item.riftverse.glimmhase_spawn_egg': 'Glimmhasen-Spawn-Ei',
    'item.riftverse.pusteling_spawn_egg': 'Pusteling-Spawn-Ei', 'item.riftverse.schimmerfalter_spawn_egg': 'Schimmerfalter-Spawn-Ei',
    'entity.riftverse.gleeb': 'Gleeb', 'entity.riftverse.glimmhase': 'Glimmhase', 'entity.riftverse.pusteling': 'Pusteling',
    'entity.riftverse.schimmerfalter': 'Schimmerfalter', 'entity.riftverse.irrlicht': 'Irrlicht',
    'effect.riftverse.verhuehnert': 'Verhühnert',
    'beruf.riftverse.buerger': 'Bürger', 'beruf.riftverse.baeckerin': 'Bäckerin', 'beruf.riftverse.kraeuterhexe': 'Kräuterhexe',
    'beruf.riftverse.alchemist': 'Alchemist', 'beruf.riftverse.runenschmied': 'Runenschmied',
    'beruf.riftverse.sternenkundlerin': 'Sternenkundlerin',
    'dorf.riftverse.verhuehnert': 'Gack! Wer einen Gleeb schlägt, wird für fünf Minuten zum Huhn.',
    'dorf.riftverse.entheuhnert': 'Du bist wieder du selbst. Die Gleebs kichern noch.',
    'dorf.riftverse.huhn_kein_handel': '%s kichert: „Mit Hühnern handle ich nicht!“',
    'dorf.riftverse.beschaeftigt': '%s ist gerade beschäftigt.',
    'dorf.riftverse.gruss.1': '%s: „Schön, dass du da bist! Die Laternen leuchten heute besonders hell.“',
    'dorf.riftverse.gruss.2': '%s: „Hast du schon Sternenbrot probiert? Die Bäckerin backt das beste.“',
    'dorf.riftverse.gruss.3': '%s: „Nachts tanzen die Leuchtglocken. Lauf mal durch, dann siehst du es!“',
    'dorf.riftverse.gruss.4': '%s: „Folge nie einem Irrlicht … außer du magst Überraschungen.“',
    'dorf.riftverse.gruss.5': '%s: „Wenn eine Sternschnuppe fällt, renn hin! Sternenstaub ist hier wie Gold.“',
    'dorf.riftverse.gruss.6': '%s: „Im Wirtshaus auf der Brücke gibt es immer einen Platz für dich.“',
    'tooltip.riftverse.sternenstaub': 'Währung der Gleebs · fällt mit Sternschnuppen vom Himmel',
    'tooltip.riftverse.sternen_chip': 'Ersetzt die Xenit-Spule im Tempo-Chip',
    'tooltip.riftverse.leuchtpflanze': 'Leuchtet nachts · strahlt hell, wenn man hindurchläuft',
    'info.riftverse.leuchtpflanzen': 'Leuchtpflanzen wachsen in Bridge\'s Inn. Nachts leuchten sie in ihrer Farbe, und wer hindurchläuft, bringt sie für ein paar Sekunden zum Strahlen. Alle außer den Zauberpilzen lassen sich zu Farbstoff verarbeiten.',
    'info.riftverse.sternenstaub': 'Nachts fallen in Bridge\'s Inn Sternschnuppen – dort, wo sie landen, liegt glitzernder Sternenstaub. Die Gleebs kaufen fremde Rohstoffe gegen Sternenstaub und verkaufen dafür Pflanzen, Deko und Magisches.',
}
EN = {
    'universe.riftverse.magisches_dorf': "Bridge's Inn",
    'dimension.riftverse.magisches_dorf': "Bridge's Inn",
    'biome.riftverse.bridges_inn': "Bridge's Inn",
    'einzigartig.riftverse.magisches_dorf.erster_besuch': 'Oops … this universe feels different. Something friendly is pulling you here, and it will not let you go.',
    'einzigartig.riftverse.magisches_dorf.rueckkehr.1': 'The lanterns are still burning for you.',
    'einzigartig.riftverse.magisches_dorf.rueckkehr.2': "Bridge's Inn missed you.",
    'einzigartig.riftverse.magisches_dorf.rueckkehr.3': 'Somewhere, someone is baking starbread.',
    'einzigartig.riftverse.magisches_dorf.rueckkehr.4': 'Welcome back, traveller.',
    'halsband.riftverse.welt.magisches_dorf': 'It smells like cake and flowers here. Can we stay forever?',
    'block.riftverse.lichtranke': 'Glowvine', 'block.riftverse.kraeuterbuendel': 'Herb Bundle',
    'block.riftverse.violette_dachziegel': 'Violet Roof Tiles', 'block.riftverse.violette_dachziegel_treppe': 'Violet Roof Tile Stairs',
    'block.riftverse.violette_dachziegel_stufe': 'Violet Roof Tile Slab', 'block.riftverse.fachwerk': 'Timber Frame',
    'block.riftverse.fachwerk_kreuz': 'Cross Timber Frame', 'block.riftverse.buntes_kopfsteinpflaster': 'Colourful Cobbles',
    'block.riftverse.sternglas': 'Starglass', 'block.riftverse.funkelholz_stamm': 'Sparkwood Log',
    'block.riftverse.funkelholz_bretter': 'Sparkwood Planks', 'block.riftverse.funkellaub': 'Sparkwood Leaves',
    'block.riftverse.schwebelaterne': 'Floating Lantern', 'block.riftverse.feenlicht': 'Fairy Light',
    'item.riftverse.sternenstaub': 'Stardust', 'item.riftverse.sternen_chip': 'Star Chip',
    'item.riftverse.sternenbrot': 'Starbread', 'item.riftverse.glimmhaar': 'Glimmer Hair', 'item.riftverse.flausch': 'Fluff',
    'item.riftverse.falterstaub': 'Moth Dust',
    'item.riftverse.gleeb_spawn_egg': 'Gleeb Spawn Egg', 'item.riftverse.glimmhase_spawn_egg': 'Glimmer Hare Spawn Egg',
    'item.riftverse.pusteling_spawn_egg': 'Puffling Spawn Egg', 'item.riftverse.schimmerfalter_spawn_egg': 'Shimmer Moth Spawn Egg',
    'entity.riftverse.gleeb': 'Gleeb', 'entity.riftverse.glimmhase': 'Glimmer Hare', 'entity.riftverse.pusteling': 'Puffling',
    'entity.riftverse.schimmerfalter': 'Shimmer Moth', 'entity.riftverse.irrlicht': 'Will-o\'-the-Wisp',
    'effect.riftverse.verhuehnert': 'Chickened',
    'beruf.riftverse.buerger': 'Villager', 'beruf.riftverse.baeckerin': 'Baker', 'beruf.riftverse.kraeuterhexe': 'Herb Witch',
    'beruf.riftverse.alchemist': 'Alchemist', 'beruf.riftverse.runenschmied': 'Runesmith',
    'beruf.riftverse.sternenkundlerin': 'Stargazer',
    'dorf.riftverse.verhuehnert': 'Cluck! Whoever hits a Gleeb becomes a chicken for five minutes.',
    'dorf.riftverse.entheuhnert': 'You are yourself again. The Gleebs are still giggling.',
    'dorf.riftverse.huhn_kein_handel': '%s giggles: "I do not trade with chickens!"',
    'dorf.riftverse.beschaeftigt': '%s is busy right now.',
    'dorf.riftverse.gruss.1': '%s: "So nice to see you! The lanterns are extra bright today."',
    'dorf.riftverse.gruss.2': '%s: "Have you tried starbread yet? The baker makes the best."',
    'dorf.riftverse.gruss.3': '%s: "At night the glowbells dance. Walk through them and you will see!"',
    'dorf.riftverse.gruss.4': '%s: "Never follow a will-o\'-the-wisp … unless you like surprises."',
    'dorf.riftverse.gruss.5': '%s: "When a shooting star falls, run! Stardust is like gold here."',
    'dorf.riftverse.gruss.6': '%s: "There is always a seat for you at the inn on the bridge."',
    'tooltip.riftverse.sternenstaub': 'Gleeb currency · falls from the sky with shooting stars',
    'tooltip.riftverse.sternen_chip': 'Replaces the xenite coil in the speed chip',
    'tooltip.riftverse.leuchtpflanze': 'Glows at night · shines brightly when you walk through it',
    'info.riftverse.leuchtpflanzen': "Glow plants grow in Bridge's Inn. At night they glow in their colour, and walking through them makes them shine for a few seconds. All of them except the spellshrooms can be turned into dye.",
    'info.riftverse.sternenstaub': "At night, shooting stars fall in Bridge's Inn – where they land, glittering stardust lies. The Gleebs buy foreign resources for stardust and sell plants, decoration and magic things for it.",
}
for n, name in PFLANZEN.items():
    DE[f'block.riftverse.{n}'] = name
    EN[f'block.riftverse.{n}'] = PFLANZEN_EN[n]


def texte():
    for code, eintraege in (('de_de', DE), ('en_us', EN)):
        p = os.path.join(ASSETS, 'lang', code + '.json')
        with open(p, encoding='utf-8') as fh:
            data = json.load(fh)
        data.update(eintraege)
        write(p, data)


def alles():
    modelle()
    loot()
    tags()
    rezepte()
    welt()
    texte()


if __name__ == '__main__':
    alles()
    print("Bridge's Inn: fertig")
