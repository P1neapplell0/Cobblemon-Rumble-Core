#!/usr/bin/env python3
"""Patch only the four equipment footprints in the default hall Anvil regions."""
import argparse
import io
import math
import runpy
import struct
import zlib
from pathlib import Path

import nbtlib as nbt

codec = runpy.run_path(str(Path(__file__).with_name('crop-prologue-arena.py')))


def patch(save, preset):
    edits = {(-19, 50, 0): 'cobblemon:healing_machine',
             (-9, 50, 0): 'cobblemon:pc', (-9, 51, 0): 'cobblemon:pc'}
    for z, facing in ((5, 'north'), (-5, 'south')):
        for y, half in ((50, 'lower'), (51, 'upper')):
            edits[-14, y, z] = nbt.Compound({
                'Name': nbt.String('cobblebash:training_simulator'),
                'Properties': nbt.Compound({'facing': nbt.String(facing), 'half': nbt.String(half)})})

    by_chunk = {}
    for (x, y, z), state in edits.items():
        by_chunk.setdefault((x // 16, z // 16), {})[x, y, z] = state

    for (cx, cz), changes in by_chunk.items():
        slot = (cz % 32) * 32 + cx % 32
        filename = f'r.{cx // 32}.{cz // 32}.mca'
        target = next(chunk for i, chunk, _ in codec['arena_chunks'](preset / filename) if i == slot)
        source = None
        if any(isinstance(state, str) for state in changes.values()):
            source = next(chunk for i, chunk, _ in codec['arena_chunks'](save / filename) if i == slot)
        copy_states(source, target, changes)
        put_chunk(preset / filename, slot, target)


def copy_states(source, target, edits):
    # All equipment sits in the same vertical section of its respective chunk.
    section = lambda chunk: next(s for s in chunk['sections'] if int(s['Y']) == 3)
    dest = section(target)
    states = dest['block_states']
    blocks, palette = codec['unpack'](states), list(states['palette'])
    source_states = section(source)['block_states'] if source is not None else None
    source_indices = codec['unpack'](source_states) if source_states is not None else None
    for (x, y, z), state in edits.items():
        index = (y % 16) * 256 + (z % 16) * 16 + x % 16
        if isinstance(state, str):
            original = source_states['palette'][source_indices[index]]
            if original['Name'] != state:
                raise ValueError(f'Missing {state} at {x}, {y}, {z}: {original}')
            state = original
        if state not in palette:
            palette.append(state)
        blocks[index] = palette.index(state)
    dest['block_states'] = codec['pack'](palette, blocks)
    entities = [b for b in target.get('block_entities', []) if tuple(int(b[k]) for k in 'xyz') not in edits]
    if source is not None:
        entities.extend(b for b in source.get('block_entities', []) if b['id'] == edits.get(tuple(int(b[k]) for k in 'xyz')))
    target['block_entities'] = nbt.List[nbt.Compound](entities)
    target.pop('isLightOn', None)


def put_chunk(path, slot, chunk):
    raw = path.read_bytes()
    output = bytearray(raw[:8192])
    for i in range(1024):
        offset = int.from_bytes(raw[i * 4:i * 4 + 3], 'big') * 4096
        if not offset:
            continue
        if i == slot:
            nbt_bytes = io.BytesIO()
            chunk.write(nbt_bytes)
            compressed = zlib.compress(nbt_bytes.getvalue(), 9)
            record = struct.pack('>IB', len(compressed) + 1, 2) + compressed
        else:
            record = raw[offset:offset + raw[i * 4 + 3] * 4096]
        sectors = math.ceil(len(record) / 4096)
        output[i * 4:i * 4 + 4] = (len(output) // 4096).to_bytes(3, 'big') + bytes([sectors])
        output.extend(record.ljust(sectors * 4096, b'\0'))
    temp = path.with_suffix('.mca.tmp')
    temp.write_bytes(output)
    temp.replace(path)


if __name__ == '__main__':
    args = argparse.ArgumentParser(description=__doc__)
    args.add_argument('save', type=Path, help='test save gym region directory (read-only)')
    args.add_argument('preset', type=Path, help='default gym region directory')
    paths = args.parse_args()
    patch(paths.save, paths.preset)
