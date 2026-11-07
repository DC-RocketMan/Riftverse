"""Sprachdateien, Block-/Item-Modelle und Blockstates als Starthilfe."""
import json
import os

HANDHELD = ('portal_kanone', 'multiversal_multitool', 'schrumpfstrahler', 'garagen_hammer', 'loetkolben',
            'mutagen_spritze')

# Maschinen, deren Front zwei Zustände hat: id -> (Gehäuse-Tier)
SIMPLE_CUBES = ['xenit_ore', 'deepslate_xenit_ore', 'fluxkristall_ore', 'gravitit_ore', 'xenit_block',
                'schwebstein', 'schrott_haufen', 'fluxkristall_block', 'nullmaterie_block']
TRANSLUCENT = ['chronoglas']


def _w(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as fh:
        json.dump(data, fh, indent=2, ensure_ascii=False)


def write(pk):
    ns = pk.ns
    base = os.path.join(pk.root, 'assets', ns)
    bm = lambda n: os.path.join(base, 'models', 'block', n + '.json')
    im = lambda n: os.path.join(base, 'models', 'item', n + '.json')
    bs = lambda n: os.path.join(base, 'blockstates', n + '.json')
    tex = lambda n: f'{ns}:block/{n}'

    def block_item(name, model=None):
        _w(im(name), {'parent': f'{ns}:block/{model or name}'})

    def single(name, model=None):
        _w(bs(name), {'variants': {'': {'model': f'{ns}:block/{model or name}'}}})
        block_item(name, model)

    def facing_active(name, has_active=True):
        rot = {'north': 0, 'east': 90, 'south': 180, 'west': 270}
        v = {}
        for f, y in rot.items():
            if has_active:
                for a in ('false', 'true'):
                    m = name + ('_on' if a == 'true' else '')
                    v[f'active={a},facing={f}'] = {'model': f'{ns}:block/{m}', **({'y': y} if y else {})}
            else:
                v[f'facing={f}'] = {'model': f'{ns}:block/{name}', **({'y': y} if y else {})}
        _w(bs(name), {'variants': v})
        block_item(name)

    # --- einfache Würfel ----------------------------------------------------
    for n in SIMPLE_CUBES:
        _w(bm(n), {'parent': 'minecraft:block/cube_all', 'textures': {'all': tex(n)}})
        single(n)
    _w(bm('chronoglas'), {'parent': 'minecraft:block/cube_all', 'render_type': 'minecraft:translucent',
                          'textures': {'all': tex('chronoglas')}})
    single('chronoglas')

    # --- Maschinen und Generatoren mit Front + Aktiv-Zustand ------------------
    for mid, meta in pk.machine_meta.items():
        t = meta['tier']
        for suffix in ('', '_on'):
            _w(bm(mid + suffix), {'parent': 'minecraft:block/orientable', 'textures': {
                'top': tex(f'casing_t{t}_top'), 'side': tex(f'casing_t{t}'),
                'front': tex(f'{mid}_front{suffix}')}})
        facing_active(mid)
    _w(bm('dimensions_scanner'), {'parent': 'minecraft:block/orientable', 'textures': {
        'top': tex('dimensions_scanner_top'), 'side': tex('casing_t2'), 'front': tex('dimensions_scanner_front')}})
    _w(bm('dimensions_scanner_on'), {'parent': 'minecraft:block/orientable', 'textures': {
        'top': tex('dimensions_scanner_top'), 'side': tex('casing_t2'), 'front': tex('dimensions_scanner_front_on')}})

    # --- Sonderblöcke --------------------------------------------------------
    _w(bm('garagen_werkbank'), {'parent': 'minecraft:block/cube', 'textures': {
        'particle': tex('garagen_werkbank_front'), 'up': tex('garagen_werkbank_top'), 'down': tex('casing_t0'),
        'north': tex('garagen_werkbank_front'), 'south': tex('garagen_werkbank_front'),
        'east': tex('garagen_werkbank_side'), 'west': tex('garagen_werkbank_side')}})
    single('garagen_werkbank')
    for suffix, top in (('', 'tonnenreaktor_top'), ('_on', 'tonnenreaktor_top_on')):
        _w(bm('tonnenreaktor' + suffix), {'parent': 'minecraft:block/cube_column', 'textures': {
            'end': tex(top), 'side': tex('tonnenreaktor_side')}})
    _w(bs('tonnenreaktor'), {'variants': {'active=false': {'model': f'{ns}:block/tonnenreaktor'},
                                          'active=true': {'model': f'{ns}:block/tonnenreaktor_on'}}})
    block_item('tonnenreaktor')
    for suffix, top in (('', 'mikrokosmos_zelle_top'), ('_on', 'mikrokosmos_zelle_top_on')):
        _w(bm('mikrokosmos_zelle' + suffix), {'parent': 'minecraft:block/cube_bottom_top', 'textures': {
            'top': tex(top), 'side': tex('mikrokosmos_zelle_side'), 'bottom': tex('casing_t3')}})
    _w(bs('mikrokosmos_zelle'), {'variants': {'active=false': {'model': f'{ns}:block/mikrokosmos_zelle'},
                                              'active=true': {'model': f'{ns}:block/mikrokosmos_zelle_on'}}})
    block_item('mikrokosmos_zelle')
    for name, top, side, bottom in (('zeitschleifen_solar', 'zeitschleifen_solar_top', 'casing_t2', 'casing_t2'),
                                    ('parallelzapfer', 'parallelzapfer_top', 'casing_t3', 'casing_t3'),
                                    ('xenit_zelle', 'casing_t1_top', 'xenit_zelle_side', 'casing_t1'),
                                    ('flux_zelle', 'casing_t2_top', 'flux_zelle_side', 'casing_t2'),
                                    ('chrono_speicher', 'casing_t3_top', 'chrono_speicher_side', 'casing_t3')):
        _w(bm(name), {'parent': 'minecraft:block/cube_bottom_top', 'textures': {
            'top': tex(top), 'side': tex(side), 'bottom': tex(bottom)}})
        single(name)
    _w(bm('rift_kabelknoten'), {'parent': 'minecraft:block/cube_column', 'textures': {
        'end': tex('rift_kabelknoten_top'), 'side': tex('rift_kabelknoten_side')}})
    single('rift_kabelknoten')
    for name, side in (('rift_schmiede', 'casing_t4'), ('weltenformer', 'casing_t4'), ('singularitaetskern', 'casing_t3')):
        _w(bm(name), {'parent': 'minecraft:block/orientable', 'textures': {
            'top': tex(side if side == 'casing_t4' else 'casing_t3_top'), 'side': tex(side), 'front': tex(name + '_front')}})
        facing_active(name, has_active=False)
    for half in ('bottom', 'top'):
        _w(bm(f'klon_tank_{half}'), {'parent': 'minecraft:block/cube_column', 'render_type': 'minecraft:translucent',
                                     'textures': {'end': tex('casing_t3_top'), 'side': tex(f'klon_tank_{half}')}})
    _w(bs('klon_tank'), {'variants': {'half=lower': {'model': f'{ns}:block/klon_tank_bottom'},
                                      'half=upper': {'model': f'{ns}:block/klon_tank_top'}}})
    _w(im('klon_tank'), {'parent': f'{ns}:block/klon_tank_bottom'})

    # --- Item-Modelle ----------------------------------------------------------
    for cat, kind, tid, t, name in pk.entries:
        if kind != 'item':
            continue
        parent = 'minecraft:item/handheld' if tid.startswith(HANDHELD) else 'minecraft:item/generated'
        _w(im(tid), {'parent': parent, 'textures': {'layer0': f'{ns}:item/{tid}'}})

    # --- Sprachdateien ---------------------------------------------------------
    lang = dict(sorted(pk.lang.items()))
    _w(os.path.join(base, 'lang', 'de_de.json'), lang)
    _w(os.path.join(base, 'lang', 'en_us.json'), lang)   # Platzhalter: bis zur Übersetzung deutsch
