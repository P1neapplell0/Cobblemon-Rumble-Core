#!/usr/bin/env python3
"""Validate the shipped raid rosters against local Cobblemon/RTC assets and render the Mega table."""
import argparse
import csv
import json
from collections import Counter, defaultdict
from pathlib import Path
import zipfile

ROOT = Path(__file__).resolve().parent.parent
LIBS = ROOT / 'libs'
LANG = ROOT / 'src/main/resources/assets/cobblemon_rumble_core/lang'
LEGENDARY = ROOT / 'src/main/resources/data/cobblemon_rumble_core/legendary_trials.tsv'
MEGA = ROOT / 'modpack_override/config/cobblemon_rumble_core/mega_challenges.tsv'
MEGA_DOC = ROOT / '流程剧本/副本设计/mega副本设计.md'


def rows(path):
    with path.open(encoding='utf-8', newline='') as file:
        return list(csv.DictReader((s for s in file if s.strip() and not s.startswith('#')), delimiter='\t'))


def load_assets():
    with zipfile.ZipFile(next(LIBS.glob('Cobblemon-neoforge-*.jar'))) as jar:
        species = {Path(file).stem: json.loads(jar.read(file)) for file in jar.namelist()
                   if file.startswith('data/cobblemon/species/') and file.endswith('.json')}
        names = json.loads(jar.read('assets/cobblemon/lang/zh_cn.json'))
    with zipfile.ZipFile(next(LIBS.glob('realtimecobblemon-*.jar'))) as jar:
        moves = json.loads(jar.read('data/realtimecobblemon/smash_moves/showdown.json'))
    return species, names, moves


def validate(legend, mega, species, moves):
    seen = set()
    groups = defaultdict(list)
    locales = [json.loads((LANG / f'{ln}.json').read_text(encoding='utf-8')) for ln in ('zh_cn', 'en_us')]
    for row in legend:
        ident, mon, aspect = row['id'], row['species'], row['aspect']
        assert (ident, mon, aspect) not in seen, (ident, mon, aspect)
        seen.add((ident, mon, aspect))
        groups[ident].append(row)
        assert all('rumble.legendary.trial.' + ident in locale for locale in locales), ident
        base = species[mon]
        form = base if aspect == '-' else next(f for f in base.get('forms', []) if aspect in f['aspects'])
        learnset = {move.split(':')[-1] for move in form.get('moves', base['moves'])}
        assert 1 <= int(row['level']) <= 100, row
        learned = row['moves'].split(',')
        assert len(set(learned)) == len(learned) == 4, row
        for move in learned:
            assert move in learnset, (mon, aspect, move, 'not in form learnset')
            assert moves[move]['power'] > 0 and moves[move]['category'] != 'status', (mon, move, 'not selectable by RTC Brawl')
    for ident, group in groups.items():
        assert 1 <= len(group) <= 4, ident
        assert len({(row['generation'], row['arena']) for row in group}) == 1, ident
    assert len({row['id'] for row in mega}) == len(mega), 'duplicate Mega ID'
    for row in mega:
        assert row['species'] in species and 1 <= int(row['level']) <= 100, row
        learned = row['moves'].split(',')
        assert len(set(learned)) == len(learned) == 4, row
        assert all(move in moves for move in learned), row
    print(f'{len(groups)} legendary encounters / {len(legend)} combatants; {len(mega)} Mega loadouts')


def mega_markdown(mega, names, moves):
    levels = Counter(int(row['level']) for row in mega)
    distribution = '、'.join(f'Lv.{lv}（{num} 场）' for lv, num in sorted(levels.items()))
    lines = [
        '# Mega 模拟试炼设计', '',
        f'共配置 **{len(mega)} 场**单种形态的挑战：{distribution}。不同 Mega 形态分别占一行。', '',
        '此表从 [实际运行配置](../../modpack_override/config/cobblemon_rumble_core/mega_challenges.tsv) 导出，按配置顺序排列。'
        '等级、性格、努力值及招式以 TSV 为准；装载时只开放当前 Cobblemon 数据中存在的形态。', '',
        '招式以 Smogon 对应赛制的社区配招为参考，再按本机学招表调整。强化、恢复等变化招式仍保留在配置中；'
        '当前 RTC 敌方 AI 会从其中可执行的伤害招式选择，不会按回合制顺序打出整套连招。', '',
        '| 试炼 ID | 宝可梦 | 等级 | 性格 | 招式 | 宝可币 | 来源 |',
        '| --- | --- | ---: | --- | --- | ---: | --- |',
    ]
    for row in mega:
        mon = names.get(f"cobblemon.species.{row['species']}.name", row['species'])
        if row['base_aspect'] != '-':
            mon += '（永恒之花）' if row['base_aspect'] == 'flower-eternal' else f"（{row['base_aspect']}）"
        form = {'mega': 'Mega', 'mega_x': 'Mega X', 'mega_y': 'Mega Y'}.get(row['form'], row['form'])
        nature = names.get(f"cobblemon.nature.{row['nature']}", row['nature'])
        attacks = '／'.join(names.get(f'cobblemon.move.{move}', moves[move]['name']) for move in row['moves'].split(','))
        lines.append(f"| `{row['id']}` | {mon} · {form} | {row['level']} | {nature} | {attacks} | {int(row['coins']):,} | {row['source']} |")
    lines += ['', '更新表格：`python3 scripts/check-encounter-design.py --write-mega`。', '']
    return '\n'.join(lines)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--write-mega', action='store_true')
    args = parser.parse_args()
    species, names, moves = load_assets()
    legend, mega = rows(LEGENDARY), rows(MEGA)
    validate(legend, mega, species, moves)
    markdown = mega_markdown(mega, names, moves)
    if args.write_mega:
        MEGA_DOC.write_text(markdown, encoding='utf-8')
    elif MEGA_DOC.read_text(encoding='utf-8') != markdown:
        raise SystemExit('Mega design table differs from the active default config; use --write-mega')


if __name__ == '__main__':
    main()
