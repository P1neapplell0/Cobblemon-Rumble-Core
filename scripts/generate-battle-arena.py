#!/usr/bin/env python3
"""Build the symmetrical League stadium as a Minecraft 1.21.1 Anvil template (requires nbtlib)."""
import argparse
import io
import math
import struct
import zlib
from collections import defaultdict
from pathlib import Path

import nbtlib as nbt

# All coordinates mirror across x=z=0. The grass pitch is at Y=64.
RADIUS = 62
Y_FLOOR = 63


def design(x, z):
    r = math.sqrt(x * x + z * z)
    ax, az = abs(x), abs(z)
    if r > RADIUS + 1:
        return
    for y in range(60, Y_FLOOR):
        yield y, 'polished_deepslate'
    if r < 31.8:
        # Poké Ball pitch emblem, with a neutral stripe and two mirrored start pads.
        if r < 11:
            tile = ('black_concrete' if abs(r - 10) < 1 or az <= 1 or 3 <= r < 5
                    else 'white_concrete' if r < 3 or z > 0 else 'red_concrete')
        elif 20 <= ax <= 25 and az <= 4:
            tile = 'white_concrete' if ax in (20, 25) or az == 4 else 'light_blue_concrete'
        elif r >= 30:
            tile = 'white_concrete' if ax % 6 == 0 or az % 6 == 0 else 'yellow_concrete'
        else:
            tile = 'moss_block' if (ax // 3 + az // 4) % 5 else 'green_concrete_powder'
        yield Y_FLOOR, tile
        return
    yield Y_FLOOR, 'smooth_stone' if r < 38 else 'polished_andesite'
    if 35 <= r < 36.25:
        yield 64, 'polished_blackstone'
        yield 65, 'sea_lantern' if (ax + az) % 4 == 0 else 'light_blue_stained_glass'
        # Keep the view through the pitch boundary, including near overhead flight.
        for y in range(66, 103):
            yield y, 'barrier'
    # Twin grandstand levels, split by aisles and internal promenades.
    if 39 <= r < 56:
        lower = r < 46
        tier = int((r - (39 if lower else 47)) / 1.8) if lower or r >= 47 else 0
        tile = 'quartz_block' if min(ax, az) <= 2 or abs(ax - az) <= 2 else 'stone_bricks'
        bottom = 64 if lower else 70
        top = bottom + max(0, tier)
        for y in range(64, top + 1):
            yield y, tile
        if tile == 'stone_bricks' and (ax + az) % 5 != 0 and not 46 <= r < 47:
            colors = ('cyan_terracotta', 'light_gray_terracotta', 'blue_terracotta', 'light_blue_terracotta')
            yield top + 1, colors[(ax // 6 + az // 6 + tier) % len(colors)]
    if 56 <= r < 59:
        for y in range(64, 81):
            yield y, 'sea_lantern' if y in (69, 78) else 'smooth_quartz' if y in (76, 79, 80) else 'polished_andesite'
    if 53 <= r < 63:
        yield 82, 'smooth_quartz'
        yield 83, 'gray_concrete'
    if 60 <= r < 62 and (abs(ax - az) <= 3 or min(ax, az) <= 2):
        for y in range(64, 89):
            yield y, 'quartz_pillar'
        for y in range(89, 91):
            yield y, 'sea_lantern'
    if 57 <= r < 58 and (ax <= 9 or az <= 9):
        for y in range(73, 78):
            ball = math.hypot(min(ax, az), y - 75)
            yield y, ('black_concrete' if y == 75 and ball < 5 else 'white_concrete' if ball < 5 and y < 75
                      else 'red_concrete' if ball < 5 else 'black_concrete')


def section(sy, blocks):
    names = sorted(set(blocks.values()) | {'air'})
    lookup = {name: i for i, name in enumerate(names)}
    palette = nbt.List[nbt.Compound]([nbt.Compound({'Name': nbt.String('minecraft:' + name)}) for name in names])
    states = nbt.Compound({'palette': palette})
    if len(names) > 1:
        bits = max(4, (len(names) - 1).bit_length())
        width = 64 // bits
        words = [0] * math.ceil(4096 / width)
        for i in range(4096):
            words[i // width] |= lookup[blocks.get(i, 'air')] << (i % width * bits)
        states['data'] = nbt.LongArray([w if w < 2**63 else w - 2**64 for w in words])
    return nbt.Compound({'Y': nbt.Byte(sy), 'block_states': states,
                         'biomes': nbt.Compound({'palette': nbt.List[nbt.String](['minecraft:the_void'])})})


def write_arena(destination, column=design):
    chunks = defaultdict(lambda: defaultdict(dict))
    for x in range(-RADIUS - 1, RADIUS + 2):
        for z in range(-RADIUS - 1, RADIUS + 2):
            for y, block in column(x, z):
                chunks[x // 16, z // 16][y // 16][(y % 16) * 256 + (z % 16) * 16 + x % 16] = block
    regions = defaultdict(lambda: bytearray(8192))
    for (cx, cz), sections in sorted(chunks.items()):
        root = nbt.File({'DataVersion': nbt.Int(3955), 'xPos': nbt.Int(cx), 'zPos': nbt.Int(cz), 'yPos': nbt.Int(-4),
                         'Status': nbt.String('minecraft:full'), 'isLightOn': nbt.Byte(0),
                         'sections': nbt.List[nbt.Compound]([section(y, sections[y]) for y in sorted(sections)])})
        buf = io.BytesIO()
        root.write(buf)
        data = zlib.compress(buf.getvalue())
        record = struct.pack('>IB', len(data) + 1, 2) + data
        occupied = math.ceil(len(record) / 4096)
        region = regions[cx // 32, cz // 32]
        address = 4 * (cx % 32 + (cz % 32) * 32)
        region[address:address + 4] = (len(region) // 4096).to_bytes(3, 'big') + bytes([occupied])
        region.extend(record.ljust(occupied * 4096, b'\0'))
    destination.mkdir(parents=True, exist_ok=True)
    for (x, z), region in regions.items():
        (destination / f'r.{x}.{z}.mca').write_bytes(region)
    print(f'{len(chunks)} chunks in {len(regions)} region files')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('destination', type=Path)
    write_arena(parser.parse_args().destination)
