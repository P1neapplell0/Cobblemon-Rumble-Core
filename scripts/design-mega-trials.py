#!/usr/bin/env python3
"""Draft the simulator table from installed species, Showdown moves and community sets."""
import argparse
import json
import re
import zipfile
from pathlib import Path

UNSUITABLE = {'protect', 'fakeout', 'return', 'frustration', 'focuspunch', 'lastresort', 'belch', 'dreameater'}


def key(value):
    return re.sub('[^a-z0-9]', '', value.lower())


def design(libs, move_file, set_paths):
    sets = [json.loads(path.read_text()) for path in set_paths]
    moves = json.loads(move_file.read_text())
    species, forms, additions = {}, {}, {}
    for pattern in ('Cobblemon-neoforge-', 'mega_showdown-', 'zamega-'):
        with zipfile.ZipFile(next(libs.glob(pattern + '*.jar'))) as jar:
            for entry in jar.namelist():
                if entry.startswith('data/cobblemon/species/') and entry.endswith('.json'):
                    data = json.loads(jar.read(entry))
                    species[key(data['name'])] = data
                elif entry.startswith('data/cobblemon/species_additions/') and entry.endswith('.json'):
                    data = json.loads(jar.read(entry))
                    target = key(data.get('target', '').split(':')[-1])
                    if target:
                        additions.setdefault(target, []).extend(data.get('forms', []))
    with zipfile.ZipFile(next(libs.glob('mega_showdown-*.jar'))) as jar:
        stones = json.loads(jar.read('data/mega_showdown/tags/item/mega_stone.json'))['values']
        for stone in stones:
            name = stone.split(':')[1]
            data = json.loads(jar.read(f'data/mega_showdown/mega_showdown/mega/{name}.json'))
            rule = data['aspect_conditions']['apply']
            forms[name] = (data['pokemons'][0], rule['aspects'][0].split('=')[1], stone)
    # Rayquaza uses Dragon Ascent rather than a stone; give a raw stone alongside the larger payout.
    forms['rayquaza'] = ('Rayquaza', 'mega', 'mega_showdown:mega_stone')
    yield 'id\tspecies\tform\tbase_aspect\tlevel\tnature\tev\tmoves\tstone\tcoins\tsource'
    for ident, (name, form, stone) in sorted(forms.items(), key=lambda row: (species[key(row[1][0])]['nationalPokedexNumber'], row[1][1])):
        label = f'{name}-Mega' + ('-' + form[-1].upper() if form != 'mega' else '')
        mon = species[key(name)]
        stats = mon['baseStats']
        native = {key(m.split(':')[-1]) for m in mon['moves']}
        base_aspect = 'flower-eternal' if name == 'Floette' else '-'
        form_data = next((f for f in additions.get(key(name), []) + mon.get('forms', []) if form in f.get('aspects', [])), {})
        stats = form_data.get('baseStats', stats)
        native.update(key(m.split(':')[-1]) for m in form_data.get('moves', []))
        if base_aspect != '-':
            native.update(key(m.split(':')[-1]) for f in mon.get('forms', []) if base_aspect in f.get('aspects', []) for m in f.get('moves', []))
        physical = stats['attack'] >= stats['special_attack']
        nature = 'jolly' if physical else 'timid'
        ev = '0,252,4,0,0,252' if physical else '0,0,4,252,0,252'
        source = 'rumble'
        selected = []
        for data in sets:
            candidates = data.get(label, {})
            if not candidates:
                candidates = {fmt: {desc: item for desc, item in builds.items()
                                     if any(key(i) == key(ident) for i in flatten(item.get('item', [])))}
                              for fmt, builds in data.get(name, {}).items()}
            group = sorted(candidates, key=lambda f: ('ou', 'ubers', 'nationaldex', 'uu', 'ru', 'nu', 'pu').index(f) if f in ('ou', 'ubers', 'nationaldex', 'uu', 'ru', 'nu', 'pu') else 10)
            choices = []
            for fmt in group:
                for build in candidates[fmt].values():
                    chosen = [next((key(m) for m in flatten(options) if key(m) in native and key(m) in moves and key(m) not in UNSUITABLE), '')
                              for options in build.get('moves', [])]
                    chosen = list(dict.fromkeys(filter(None, chosen)))[:4]
                    if len(chosen) >= 3:
                        choices.append((len(chosen), chosen, build, fmt))
            if choices:
                _, selected, build, fmt = max(choices, key=lambda choice: choice[0])
                nature = key(flatten(build.get('nature', nature))[0])
                spread = build.get('evs', {})
                if isinstance(spread, list):
                    spread = spread[0]
                if spread:
                    ev = ','.join(str(spread.get(stat, 0)) for stat in ('hp', 'atk', 'def', 'spa', 'spd', 'spe'))
                source = f'smogon:{fmt}'
                break
        types = {form_data.get('primaryType', mon.get('primaryType')), form_data.get('secondaryType', mon.get('secondaryType'))}
        available = [m for m in sorted(native - UNSUITABLE) if m in moves and moves[m]['basePower'] >= 45 and not moves[m].get('isNonstandard')]
        def rating(m):
            move = moves[m]
            acc = 100 if move['accuracy'] is True else move['accuracy']
            return move['basePower'] * (acc / 100) * (1.4 if move['type'].lower() in types else 1) * (1.35 if (move['category'] == 'Physical') == physical else 1) * (0.6 if move.get('flags', {}).get('charge') or move.get('selfdestruct') else 1)
        if name == 'Rayquaza' and 'dragonascent' not in selected:
            selected = ['dragonascent'] + selected[:3]
        for move in sorted(available, key=rating, reverse=True):
            if len(selected) == 4:
                break
            if move not in selected and (len(selected) < 2 or sum(moves[m]['type'] == moves[move]['type'] for m in selected) < 2):
                selected.append(move)
        if len(selected) != 4:
            raise ValueError(f'{name}: missing moves {selected}')
        legendary = {'Mewtwo': 72, 'Rayquaza': 75, 'Latias': 62, 'Latios': 62, 'Diancie': 60}
        total = sum(stats.values())
        level = legendary.get(name, 38 if total < 490 else 42 if total < 550 else 48 if total < 625 else 55)
        coins = 5000 if level <= 42 else 7000 if level < 60 else 11000
        yield '\t'.join(map(str, (ident, key(name), form, base_aspect, level, nature, ev, ','.join(selected), stone, coins, source)))


def flatten(value):
    return value if isinstance(value, list) else [value]


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('libs', type=Path)
    parser.add_argument('moves', type=Path, help='JSON export of Showdown Moves')
    parser.add_argument('output', type=Path)
    parser.add_argument('sets', type=Path, nargs='+', help='Smogon set JSON, highest priority first')
    args = parser.parse_args()
    args.output.write_text('\n'.join(design(args.libs, args.moves, args.sets)) + '\n')
