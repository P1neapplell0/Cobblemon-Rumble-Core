#!/usr/bin/env python3
"""Clone the hall simulators, PCs and healers, leaving one block between units."""
import argparse
import copy
import runpy
from pathlib import Path

import nbtlib as nbt


def expand(region):
    tools = runpy.run_path(str(Path(__file__).with_name('install-hall-equipment.py')))
    codec = tools['codec']
    chunks = {}

    def chunk_at(x, z):
        cx, cz = x // 16, z // 16
        path = region / f'r.{cx // 32}.{cz // 32}.mca'
        slot = (cz % 32) * 32 + cx % 32
        key = (path, slot)
        if key not in chunks:
            chunks[key] = next(chunk for i, chunk, _ in codec['arena_chunks'](path) if i == slot)
        return key, chunks[key]

    def state(x, y, z):
        _, chunk = chunk_at(x, z)
        blocks = next(s for s in chunk['sections'] if int(s['Y']) == y // 16)['block_states']
        return blocks['palette'][codec['unpack'](blocks)[(y % 16) * 256 + (z % 16) * 16 + x % 16]]

    edits, block_entities = {}, {}
    for x, z, height, name, targets in [
        (-14, -5, 2, 'cobblebash:training_simulator', [(-16, -5), (-12, -5)]),
        (-14, 5, 2, 'cobblebash:training_simulator', [(-16, 5), (-12, 5)]),
        (-9, 0, 2, 'cobblemon:pc', [(-9, -2), (-9, 2)]),
        (-19, 0, 1, 'cobblemon:healing_machine', [(-19, -2), (-19, 2)]),
    ]:
        _, source = chunk_at(x, z)
        for tx, tz in targets:
            key, _ = chunk_at(tx, tz)
            for y in range(50, 50 + height):
                block = state(x, y, z)
                assert block['Name'] == name, (x, y, z, block)
                destination = state(tx, y, tz)
                if destination != block and destination['Name'] != 'minecraft:air':
                    raise ValueError(f'Equipment would overwrite {destination} at {(tx, y, tz)}')
                edits.setdefault(key, {})[tx, y, tz] = block
            for entity in source.get('block_entities', []):
                if (int(entity['x']), int(entity['z'])) == (x, z) and 50 <= int(entity['y']) < 50 + height:
                    after = copy.deepcopy(entity)
                    after['x'], after['z'] = nbt.Int(tx), nbt.Int(tz)
                    block_entities.setdefault(key, []).append(after)
    for (path, slot), changes in edits.items():
        chunk = chunks[path, slot]
        tools['copy_states'](None, chunk, changes)
        new_entities = block_entities.get((path, slot), [])
        coordinates = lambda entity: tuple(int(entity[axis]) for axis in ('x', 'y', 'z'))
        replacements = {coordinates(entity) for entity in new_entities}
        existing = chunk['block_entities']
        existing[:] = [entity for entity in existing if coordinates(entity) not in replacements] + new_entities
        tools['put_chunk'](path, slot, chunk)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('region', type=Path)
    expand(parser.parse_args().region)
