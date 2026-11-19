#!/usr/bin/env python3
"""Replace unavailable decoration blocks in Anvil templates. Requires nbtlib."""
import argparse
from collections import Counter, defaultdict
from functools import lru_cache
import gzip
import io
import math
from pathlib import Path
import runpy
import struct
import zlib

import nbtlib as nbt

palette_tools = runpy.run_path(str(Path(__file__).with_name('crop-prologue-arena.py')))
unpack, pack = palette_tools['unpack'], palette_tools['pack']
MISSING = {'supplementaries', 'cobblefurnies', 'create'}
SOFAS = {
    'black': 'polished_blackstone_stairs', 'light_blue': 'prismarine_stairs',
    'purple': 'purpur_stairs', 'red': 'brick_stairs',
    'white': 'quartz_stairs', 'yellow': 'smooth_sandstone_stairs',
}
# Sample the building material around fixtures with no comparable block in the pack.
SAMPLE = {'clock_block', 'globe', 'globe_sepia', 'goblet', 'statue'}
STRUCTURAL = ('planks', 'wood', 'bricks', 'terracotta', 'stone', 'quartz', 'concrete', 'sandstone', 'basalt')
OFFSETS = ((1, 0, 0), (-1, 0, 0), (0, 0, 1), (0, 0, -1), (0, -1, 0), (0, 1, 0))


def state(name, **properties):
    tag = nbt.Compound({'Name': nbt.String(name if ':' in name else 'minecraft:' + name)})
    if properties:
        tag['Properties'] = nbt.Compound({k: nbt.String(str(v)) for k, v in properties.items()})
    return tag


def coordinates(pos):
    return tuple(int(pos[axis]) for axis in ('x', 'y', 'z'))


def read_chunks(directory):
    for path in sorted(directory.glob('r.*.*.mca')):
        region = path.read_bytes()
        for slot in range(1024):
            sectors = int.from_bytes(region[4 * slot:4 * slot + 3], 'big')
            if not sectors:
                continue
            offset = sectors * 4096
            size, codec = struct.unpack_from('>IB', region, offset)
            payload = region[offset + 5:offset + 4 + size]
            raw = {1: gzip.decompress, 2: zlib.decompress, 3: bytes}[codec](payload)
            yield path, slot, nbt.File.parse(io.BytesIO(raw))


def replace(directory, registry):
    chunks = {(int(chunk['xPos']), int(chunk['zPos'])): (path, slot, chunk)
              for path, slot, chunk in read_chunks(directory)}
    changes = defaultdict(Counter)
    changed_chunks = defaultdict(dict)

    @lru_cache(maxsize=256)
    def section_data(cx, sy, cz):
        record = chunks.get((cx, cz))
        if record is None:
            return None
        section = next((s for s in record[2]['sections'] if int(s['Y']) == sy and 'block_states' in s), None)
        if section is not None:
            states = section['block_states']
            return states['palette'], unpack(states)
        return None

    def block_at(x, y, z):
        section = section_data(x // 16, y // 16, z // 16)
        if section is None:
            return 'minecraft:air'
        palette, blocks = section
        return str(palette[blocks[(y % 16) * 256 + (z % 16) * 16 + x % 16]]['Name'])

    def sample(x, y, z):
        neighbors = Counter()
        for radius in (1, 2, 3):
            for dx, dy, dz in OFFSETS:
                other = block_at(x + radius * dx, y + radius * dy, z + radius * dz)
                namespace, name = other.split(':', 1)
                if namespace in ('minecraft', 'quark', 'chipped') and name.endswith(STRUCTURAL) and other in registry:
                    neighbors[other] += 4 - radius
        if not neighbors:
            raise ValueError(f'No usable terrain sample at {(x, y, z)}')
        return state(neighbors.most_common(1)[0][0])

    def replacement(old, x, y, z, tile):
        full_name = str(old['Name'])
        namespace, name = full_name.split(':', 1)
        props = old.get('Properties', {})
        facing = props.get('facing', 'north')
        waterlogged = props.get('waterlogged', 'false')
        if namespace == 'create' and name == 'red_table_cloth':
            return state('red_carpet')
        if namespace == 'cobblefurnies':
            color = name.split('_dark_chair')[0] if name.endswith('_dark_chair') else name.rsplit('_', 1)[0]
            if name.endswith(('chair', 'stool')):
                return state(f'quark:{color}_stool')
            if name.endswith('sofa'):
                return state(SOFAS[color], facing=facing, half='bottom', shape='straight', waterlogged=waterlogged)
            if name.endswith('sink') or name == 'light_toilet':
                return state('cauldron')
            if name.endswith('stove'):
                return state('smoker', facing=facing, lit='false')
            if name == 'spruce_cabinet':
                return state('barrel', facing=facing, open='false')
            if name == 'yellow_cabinetry':
                return state('yellow_terracotta')
        if namespace == 'supplementaries':
            if name == 'flower_box':
                for item in tile.get('Items', []):
                    flower = str(item.get('id', '')).removeprefix('minecraft:')
                    if f'minecraft:potted_{flower}' in registry:
                        return state('potted_' + flower)
                    tall_to_small = {'peony': 'pink_tulip', 'rose_bush': 'poppy', 'lilac': 'allium', 'sunflower': 'dandelion'}
                    if flower in tall_to_small:
                        return state('potted_' + tall_to_small[flower])
                return state('flower_pot')
            if name == 'planter':
                above = block_at(x, y + 1, z)
                # Full soil supports existing plants, including two-block flowers and crops.
                return state('flower_pot' if above.endswith((':air', ':cave_air', ':void_air')) else 'rooted_dirt')
            if name.startswith('candle_holder_'):
                color = name.removeprefix('candle_holder_')
                return state(color + '_candle', candles=props.get('candles', '1'),
                             lit=props.get('lit', 'false') if waterlogged == 'false' else 'false', waterlogged=waterlogged)
            if name == 'deepslate_lamp':
                return state('quark:blaze_lantern')
            if name == 'stone_lamp':
                return state('sea_lantern')
            if name == 'fire_pit':
                return state('campfire', facing='north', lit=props.get('lit', 'false') if waterlogged == 'false' else 'false',
                             signal_fire='false', waterlogged=waterlogged)
            if name == 'blackboard':
                return state('black_concrete')
            if name == 'jar':
                return state('decorated_pot', facing=facing, cracked='false', waterlogged=waterlogged)
            if name == 'pedestal':
                return state('chiseled_quartz_block')
            if name == 'way_sign' and 'Mimic' in tile and str(tile['Mimic']['Name']) in registry:
                return tile['Mimic']
            if name in SAMPLE or name == 'way_sign':
                return sample(x, y, z)
        raise ValueError(f'No replacement rule for {full_name}')

    for (cx, cz), (path, slot, chunk) in chunks.items():
        sections = {}
        updated = set()
        tiles = {coordinates(tile): tile for tile in chunk.get('block_entities', [])}
        for section in chunk.get('sections', []):
            sy = int(section['Y'])
            states = section.get('block_states')
            if states is None or not any(str(s['Name']).split(':')[0] in MISSING for s in states['palette']):
                continue
            palette, blocks = section_data(cx, sy, cz)
            new_palette = []
            indices = {}
            result = []
            for index, entry in enumerate(blocks):
                selected = palette[entry]
                old_name = str(selected['Name'])
                if old_name.split(':')[0] in MISSING:
                    pos = (cx * 16 + index % 16, sy * 16 + index // 256, cz * 16 + index // 16 % 16)
                    selected = replacement(selected, *pos, tiles.get(pos, {}))
                    new_name = str(selected['Name'])
                    if new_name not in registry:
                        raise ValueError(f'Unregistered replacement {new_name} at {pos}')
                    changes[old_name][new_name] += 1
                    updated.add(pos)
                key = selected.snbt()
                if key not in indices:
                    indices[key] = len(new_palette)
                    new_palette.append(selected)
                result.append(indices[key])
            sections[sy] = pack(new_palette, result)
        removed_ink = False
        if 'supplementaries:clock_block' not in registry:
            for tile in tiles.values():
                attachments = tile.get('neoforge:attachments')
                if attachments is not None and 'supplementaries:antique_ink' in attachments:
                    del attachments['supplementaries:antique_ink']
                    if not attachments:
                        del tile['neoforge:attachments']
                    removed_ink = True
        if sections or removed_ink:
            # Apply block changes after sampling so each search sees the original neighborhood.
            changed_chunks[path][slot] = (chunk, sections, updated)

    for path, records in changed_chunks.items():
        region = path.read_bytes()
        output = bytearray(8192)
        output[4096:] = region[4096:8192]
        for slot in range(1024):
            old = int.from_bytes(region[4 * slot:4 * slot + 3], 'big') * 4096
            if not old:
                continue
            if slot in records:
                chunk, sections, updated = records[slot]
                for section in chunk['sections']:
                    if int(section['Y']) in sections:
                        section['block_states'] = sections[int(section['Y'])]
                for key in ('block_entities', 'block_ticks', 'fluid_ticks'):
                    if key in chunk:
                        chunk[key] = nbt.List[nbt.Compound](entry for entry in chunk[key] if coordinates(entry) not in updated)
                if sections:
                    chunk.pop('Heightmaps', None)
                    chunk['isLightOn'] = nbt.Byte(0)
                raw = io.BytesIO()
                chunk.write(raw)
                compressed = zlib.compress(raw.getvalue(), 9)
                record = struct.pack('>IB', len(compressed) + 1, 2) + compressed
            else:
                size = struct.unpack_from('>I', region, old)[0]
                record = region[old:old + size + 4]
            sectors = math.ceil(len(record) / 4096)
            output[4 * slot:4 * slot + 4] = (len(output) // 4096).to_bytes(3, 'big') + bytes([sectors])
            output.extend(record.ljust(sectors * 4096, b'\0'))
        temp = path.with_suffix('.mca.tmp')
        temp.write_bytes(output)
        temp.replace(path)
    return changes


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('directory', type=Path, help='region directory to edit')
    parser.add_argument('block_registry', type=Path, help='registered block IDs, one per line')
    options = parser.parse_args()
    registry = set(options.block_registry.read_text().splitlines())
    for original, replacements in sorted(replace(options.directory, registry).items()):
        print(f'{original}\t' + ', '.join(f'{name} ({count})' for name, count in sorted(replacements.items())))
