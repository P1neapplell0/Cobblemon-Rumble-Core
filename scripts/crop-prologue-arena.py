#!/usr/bin/env python3
"""Extract the old Gyms plaza as an Anvil template. Requires nbtlib; does not modify the source."""
import argparse
import io
import math
import struct
import zlib
from pathlib import Path

import nbtlib as nbt

CENTER_X, CENTER_Z = -14, 0
FLOOR_Y = 49
NEIGHBORS = ((1, 0), (-1, 0), (0, 1), (0, -1))


def inside(x, z):
    dx, dz = abs(x - CENTER_X), abs(z - CENTER_Z)
    return dx <= 42 and dz <= 46 and dx + dz <= 65


def unpack(states):
    palette = states['palette']
    if len(palette) == 1:
        return [0] * 4096
    bits = max(4, (len(palette) - 1).bit_length())
    per = 64 // bits
    mask = (1 << bits) - 1
    return [(int(states['data'][i // per]) >> ((i % per) * bits)) & mask for i in range(4096)]


def pack(palette, blocks):
    if len(palette) == 1:
        return nbt.Compound({'palette': nbt.List[nbt.Compound](palette)})
    bits = max(4, (len(palette) - 1).bit_length())
    per = 64 // bits
    words = [0] * math.ceil(len(blocks) / per)
    for i, entry in enumerate(blocks):
        words[i // per] |= entry << (i % per * bits)
    return nbt.Compound({'palette': nbt.List[nbt.Compound](palette), 'data': nbt.LongArray([
        word if word < (1 << 63) else word - (1 << 64) for word in words
    ])})


def state(name):
    return nbt.Compound({'Name': nbt.String('minecraft:' + name)})


def recessed_floor(regions):
    # The original plaza has an air ring between the birch and quartz slab borders.
    # Follow that connected component across chunks without touching any of the surrounding terrain.
    top = {}
    for chunks in regions.values():
        for _, chunk, _ in chunks:
            cx, cz = int(chunk['xPos']) * 16, int(chunk['zPos']) * 16
            section = next((s for s in chunk['sections'] if s['Y'] == FLOOR_Y // 16), None)
            if section is None or 'block_states' not in section:
                continue
            states = section['block_states']
            names = [str(block['Name']) for block in states['palette']]
            start = (FLOOR_Y % 16) * 256
            for i, block in enumerate(unpack(states)[start:start + 256]):
                top[cx + i % 16, cz + i // 16] = names[block]

    seed = (CENTER_X, CENTER_Z - 19)
    air = ('minecraft:air', 'minecraft:cave_air', 'minecraft:void_air')
    if top.get(seed) not in air:
        raise ValueError('Missing original plaza floor recess')
    floor, pending, rims = {seed}, [seed], set()
    while pending:
        x, z = pending.pop()
        if abs(x - CENTER_X) > 21 or abs(z - CENTER_Z) > 21:
            raise ValueError('Plaza floor recess is no longer enclosed')
        for dx, dz in NEIGHBORS:
            pos = (x + dx, z + dz)
            name = top.get(pos)
            if name not in air:
                rims.add(name)
            elif pos not in floor:
                floor.add(pos)
                pending.append(pos)
    if rims != {'minecraft:birch_slab', 'minecraft:quartz_slab'}:
        raise ValueError(f'Unexpected plaza floor borders: {rims}')
    return floor


def crop(chunk, columns, floor):
    cx, cz = int(chunk['xPos']) * 16, int(chunk['zPos']) * 16
    sections = {int(section['Y']): section for section in chunk['sections']}
    for sy in range(-4, 20):
        section = sections.get(sy)
        if section is None:
            if not (2 <= sy <= 5) or 2 not in columns:
                continue
            section = nbt.Compound({'Y': nbt.Byte(sy), 'biomes': nbt.Compound({
                'palette': nbt.List[nbt.String](['minecraft:the_void'])})})
            sections[sy] = section
        states = section.get('block_states')
        palette = list(states['palette']) if states else [state('air')]
        blocks = unpack(states) if states else [0] * 4096
        air, barrier = len(palette), len(palette) + 1
        palette += [state('air'), state('barrier')]
        if sy == FLOOR_Y // 16:
            repair = len(palette)
            slab = state('smooth_stone_slab')
            slab['Properties'] = nbt.Compound({'type': nbt.String('bottom'), 'waterlogged': nbt.String('false')})
            palette.append(slab)
            for i in range(256):
                if (cx + i % 16, cz + i // 16) in floor:
                    blocks[(FLOOR_Y % 16) * 256 + i] = repair
        for y in range(16):
            for i, column in enumerate(columns):
                index = 256 * y + i
                if not column:
                    blocks[index] = air
                elif column == 2 and 43 <= 16 * sy + y <= 91:
                    blocks[index] = barrier
        used = sorted(set(blocks))
        remap = {old: new for new, old in enumerate(used)}
        section['block_states'] = pack([palette[i] for i in used], [remap[i] for i in blocks])
        # Relight the cut perimeter on load.
        section.pop('BlockLight', None)
        section.pop('SkyLight', None)
    chunk['sections'] = nbt.List[nbt.Compound]([sections[i] for i in sorted(sections)])
    # No old transport links, inventory contents or pending block/entity activity in the simulation.
    chunk['block_entities'] = nbt.List[nbt.Compound]()
    chunk['block_ticks'] = nbt.List[nbt.Compound]()
    chunk['fluid_ticks'] = nbt.List[nbt.Compound]()
    for key in ('Heightmaps', 'isLightOn', 'PostProcessing', 'entities', 'Entities'):
        chunk.pop(key, None)
    return chunk


def arena_chunks(path):
    rx, rz = map(int, path.name.split('.')[1:3])
    original = path.read_bytes()
    for i in range(1024):
        cx, cz = rx * 32 + i % 32, rz * 32 + i // 32
        columns = []
        for j in range(256):
            x, z = cx * 16 + j % 16, cz * 16 + j // 16
            columns.append(0 if not inside(x, z) else 1 if all(
                inside(x + dx, z + dz) for dx, dz in NEIGHBORS) else 2)
        if not any(columns):
            continue
        offset = int.from_bytes(original[i * 4:i * 4 + 3], 'big') * 4096
        if not offset:
            raise ValueError(f'Missing source chunk {cx}, {cz}')
        length, encoding = struct.unpack_from('>IB', original, offset)
        if encoding != 2:
            raise ValueError(f'Unsupported compression {encoding} at {cx}, {cz}')
        chunk = nbt.File.parse(io.BytesIO(zlib.decompress(original[offset + 5:offset + 4 + length])))
        yield i, chunk, columns


def transform(source, destination):
    regions = {path.name: list(arena_chunks(path)) for path in sorted(source.glob('r.*.*.mca'))}
    if not any(regions.values()):
        raise ValueError(f'No plaza chunks found in {source}')
    floor = recessed_floor(regions)
    destination.mkdir(parents=True, exist_ok=True)
    for name, chunks in regions.items():
        if not chunks:
            continue
        region = bytearray(8192)
        for i, chunk, columns in chunks:
            data = io.BytesIO()
            crop(chunk, columns, floor).write(data)
            compressed = zlib.compress(data.getvalue(), 9)
            record = struct.pack('>IB', len(compressed) + 1, 2) + compressed
            sectors = math.ceil(len(record) / 4096)
            region[i * 4:i * 4 + 4] = (len(region) // 4096).to_bytes(3, 'big') + bytes([sectors])
            region.extend(record.ljust(sectors * 4096, b'\0'))
        (destination / name).write_bytes(region)
    print(f'Wrote {sum(map(len, regions.values()))} arena chunks; repaired {len(floor)} floor blocks')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('source', type=Path, help='original gyms_dim/region directory')
    parser.add_argument('destination', type=Path, help='simulation region output directory')
    args = parser.parse_args()
    transform(args.source, args.destination)
