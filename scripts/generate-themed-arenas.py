#!/usr/bin/env python3
"""Write the themed 1.21.1 arena templates used by ArenaTheme (requires nbtlib)."""
import argparse
import math
import runpy
from pathlib import Path

base = runpy.run_path(str(Path(__file__).with_name('generate-battle-arena.py')))

# Pitch palette, perimeter architecture, and light source. Shared geometry stays
# aligned with ArenaMatch's spawn, deployment, and camera coordinates.
THEMES = {
    'rock': ('smooth_stone', 'andesite', 'tuff_bricks', 'polished_basalt', 'ochre_froglight'),
    'grass': ('moss_block', 'green_terracotta', 'mud_bricks', 'green_stained_glass', 'pearlescent_froglight'),
    'ghost': ('purple_terracotta', 'polished_deepslate', 'deepslate_bricks', 'purple_stained_glass', 'sea_lantern'),
    'ice': ('white_concrete', 'light_blue_terracotta', 'calcite', 'packed_ice', 'sea_lantern'),
    'ground': ('smooth_sandstone', 'terracotta', 'cut_red_sandstone', 'red_sandstone', 'ochre_froglight'),
    'fire': ('red_terracotta', 'gray_terracotta', 'polished_blackstone_bricks', 'orange_stained_glass', 'shroomlight'),
    'dark': ('gray_concrete', 'purple_terracotta', 'nether_bricks', 'magenta_stained_glass', 'pearlescent_froglight'),
    'electric': ('smooth_quartz', 'light_gray_concrete', 'waxed_copper_block', 'yellow_stained_glass', 'ochre_froglight'),
    'forest': ('green_terracotta', 'moss_block', 'mossy_stone_bricks', 'spruce_planks', 'verdant_froglight'),
    'water': ('prismarine_bricks', 'dark_prismarine', 'prismarine', 'cyan_stained_glass', 'sea_lantern'),
    'psychic': ('polished_diorite', 'magenta_terracotta', 'smooth_quartz', 'amethyst_block', 'pearlescent_froglight'),
    'sky': ('smooth_quartz', 'light_blue_concrete', 'calcite', 'light_blue_stained_glass', 'sea_lantern'),
}


def themed_column(theme, x, z):
    floor, variation, shell, trim, light = THEMES[theme]
    ax, az = abs(x), abs(z)
    radius = math.hypot(x, z)
    if radius > 63:
        return
    if radius < 36.25:
        for y, block in base['design'](x, z):
            if y == 63 and block in ('moss_block', 'green_concrete_powder'):
                block = floor if block == 'moss_block' else variation
            elif y == 63 and 20 < ax < 25 and az < 4 and x > 0:
                block = 'red_concrete'
            elif y == 64 and radius >= 35:
                block = shell
            elif y == 65 and radius >= 35:
                block = light if (ax + az) % 4 == 0 else trim
            yield y, block
        return

    for y in range(60, 64):
        yield y, shell
    if theme in ('grass', 'forest'):
        # Repeated tree planters with a glasshouse frame around the Grass Gym.
        if radius < 58:
            yield 63, 'moss_block'
        tx, tz = (ax + 5) % 10 - 5, (az + 5) % 10 - 5
        if 42 < radius < 57 and ax > 5 and az > 5:
            if tx == tz == 0:
                for y in range(64, 75):
                    yield y, 'spruce_log' if theme == 'forest' else 'cherry_log'
            if abs(tx) + abs(tz) <= 4:
                for y in range(69, 75 - (abs(tx) + abs(tz)) // 2):
                    if tx or tz:
                        yield y, 'green_wool' if theme == 'forest' else 'pink_wool'
        if theme == 'grass' and 59 < radius < 61:
            for y in range(64, 81):
                yield y, shell if (ax + az) % 9 == 0 or y % 8 == 0 else trim
    elif theme in ('rock', 'ground', 'ice'):
        # Terraced cliffs and crystal ribs arranged in opposing quadrants.
        crest = 67 + int(max(0, radius - 42) * .6) + (ax // 6 + az // 8) % 4
        if 41 < radius < 61 and min(ax, az) >= 4:
            for y in range(64, crest):
                yield y, trim if theme == 'ice' or y == crest - 2 else shell
        if 50 < radius < 55 and abs(ax - az) < 2:
            for y in range(crest, crest + 8):
                yield y, light if y == crest + 6 else trim
    elif theme in ('ghost', 'fire', 'dark', 'electric', 'psychic', 'sky'):
        tower = 50 < radius < 58 and (abs(ax - az) <= 2 or min(ax, az) <= 2)
        if tower:
            height = 87 if theme in ('electric', 'sky') else 81
            for y in range(64, height):
                yield y, light if y in (70, 76, height - 2) else trim if y % 4 == 0 else shell
        if theme != 'sky' and 49 < radius < 58:
            for y in range(77, 80):
                yield y, trim if y == 77 else shell
        if 39 < radius < 49 and min(ax, az) > 3:
            for y in range(64, 65 + int((radius - 39) / 2)):
                yield y, shell
    else:
        # Water is sealed behind the arena barrier and contained by the outer quay.
        if 39 < radius < 57:
            yield 64, 'water'
        if 57 <= radius < 60:
            yield 64, shell
            yield 65, light if (ax + az) % 7 == 0 else trim
    if 36.25 < radius < 39 and (ax + az) % 8 == 0:
        yield 63, light


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('destination', type=Path)
    parser.add_argument('themes', nargs='*', metavar='THEME', help='optional subset: ' + ', '.join(THEMES))
    args = parser.parse_args()
    unknown = set(args.themes) - THEMES.keys()
    if unknown:
        parser.error('unknown theme: ' + ', '.join(sorted(unknown)))
    for theme in args.themes or THEMES:
        target = args.destination / ('rumble_arena_' + theme) / 'region'
        base['write_arena'](target, lambda x, z: themed_column(theme, x, z))


if __name__ == '__main__':
    main()
