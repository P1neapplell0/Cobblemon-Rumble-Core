#!/usr/bin/env python3
"""Bundle the title menu, generated scenery, and installed Pokémon into offline HTML."""
import argparse
import base64
import hashlib
import io
import json
import re
import tarfile
from pathlib import Path
from urllib.request import urlopen
from zipfile import ZipFile

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / 'docs/title-screen'
THREE_URL = 'https://registry.npmjs.org/three/-/three-0.160.1.tgz'
THREE_SHA512 = 'Bgl2wPJypDOZ1stAxwfWAcJ0WQf7QzlptsxkjYiURPz+n5k4RBDLsq+6f9Y75TYxn6aHLcWz+JNmwTOXWrQTBQ=='


def load_three():
    archive = ROOT / 'build/battle-intro/three-0.160.1.tgz'
    if not archive.exists():
        with urlopen(THREE_URL, timeout=25) as response:
            package = response.read()
    else:
        package = archive.read_bytes()
    if base64.b64encode(hashlib.sha512(package).digest()).decode() != THREE_SHA512:
        raise SystemExit('Three.js 压缩包校验失败。')
    archive.parent.mkdir(parents=True, exist_ok=True)
    archive.write_bytes(package)
    with tarfile.open(fileobj=io.BytesIO(package)) as tar:
        return tar.extractfile('package/build/three.min.js').read().decode()


def data_url(data, mime):
    return f'data:{mime};base64,{base64.b64encode(data).decode()}'


def pokemon_assets(cobblemon, mega):
    pokemon = 'assets/cobblemon/bedrock/pokemon'
    textures = 'assets/cobblemon/textures/pokemon'
    with ZipFile(cobblemon) as jar, ZipFile(mega) as mega_jar:
        def read_json(archive, path):
            return json.loads(archive.read(path))

        rigs = {}
        for species, folder, model, animation, skin, model_jar, names in (
            ('pikachu', '0025_pikachu', 'pikachu_male', 'pikachu', 'pikachu', jar, ('battle_idle', 'special', 'ground_run')),
            ('lucario', '0448_lucario', 'lucario', 'lucario', 'lucario', jar, ('battle_idle', 'blink')),
            ('salamence', '0373_salamence', 'salamencemega', 'salamence_mega', 'salamence_mega', mega_jar,
             ('air_idle', 'blink')),
            ('scizor', '0212_scizor', 'scizor_male', 'scizor', 'scizor', jar, ('battle_idle', 'blink')),
            ('meowscarada', '0908_meowscarada', 'meowscarada', 'meowscarada', 'meowscarada', jar,
             ('battle_idle', 'blink')),
            ('hitmontop', '0237_hitmontop', 'hitmontop', 'hitmontop', 'hitmontop', jar, ('battle_idle', 'blink')),
            ('greninja', '0658_greninja', 'greninja', 'greninja', 'greninja', jar, ('battle_idle', 'blink')),
            ('charizard', '0006_charizard', 'charizard_mega_x', 'charizard', 'megacharizardx', mega_jar,
             ('air_idle', 'air_battle_cry')),
            ('garchomp', '0445_garchomp', 'garchomp_male', 'garchomp', 'garchomp', jar, ('battle_idle', 'blink')),
            ('blaziken', '0257_blaziken', 'blaziken_male', 'blaziken', 'blaziken', jar, ('battle_idle', 'blink')),
            ('ceruledge', '0937_ceruledge', 'ceruledge', 'ceruledge', 'ceruledge_1', jar, ('battle_idle', 'blink')),
            ('cinderace', '0815_cinderace', 'cinderace', 'cinderace', 'cinderace', jar, ('battle_idle', 'blink')),
        ):
            animation_jar = mega_jar if species == 'salamence' else jar
            clips = read_json(animation_jar, f'{pokemon}/animations/{folder}/{animation}.animation.json')['animations']
            rigs[species] = {
                'geo': read_json(model_jar, f'{pokemon}/models/{folder}/{model}.geo.json'),
                'image': data_url(model_jar.read(f'{textures}/{folder}/{skin}.png'), 'image/png'),
                'clips': {name: clips[f'animation.{animation}.{name}'] for name in names},
            }
        rigs['charizard']['glow'] = [
            data_url(mega_jar.read(f'{textures}/0006_charizard/x_glow{suffix}.png'), 'image/png')
            for suffix in ('', '2', '3', '4')
        ]
        rigs['ceruledge']['glow'] = [
            data_url(jar.read(f'{textures}/0937_ceruledge/ceruledge_emissive_{frame}.png'), 'image/png')
            for frame in range(1, 9)
        ]
        sound_paths = {
            'pikachu': 'pokemon/pikachu/pikachu_cry.ogg',
            'lightning': 'move/thunderbolt/thunderbolt_actor.ogg',
            'charizard': 'pokemon/charizard/charizard_cry.ogg',
        }
        sounds = {name: data_url(jar.read(f'assets/cobblemon/sounds/{file}'), 'audio/ogg')
                  for name, file in sound_paths.items()}
        return {'rigs': rigs, 'sounds': sounds}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--cobblemon', type=Path)
    parser.add_argument('--mega', type=Path)
    parser.add_argument('--output', type=Path, default=ROOT / 'build/title-screen/index.html')
    options = parser.parse_args()
    cobblemon = options.cobblemon or next(iter(sorted((ROOT / 'libs').glob('Cobblemon-neoforge-*.jar'))), None)
    mega = options.mega or next(iter(sorted((ROOT / 'libs').glob('mega_showdown-neoforge-*.jar'))), None)
    if not cobblemon or not mega:
        parser.error('请将 Cobblemon 和 Mega Showdown 放入 libs/，或指定 --cobblemon 和 --mega。')

    blocks = {
        'CSS': (SOURCE / 'style.css').read_text(encoding='utf-8').replace(
            '/*@@BACKGROUND@@*/', data_url((SOURCE / 'clean-arena.jpg').read_bytes(), 'image/jpeg')),
        'THREE': load_three(),
        'RIG': (SOURCE / 'rig.js').read_text(encoding='utf-8'),
        'ELECTRIC': (SOURCE / 'electric.js').read_text(encoding='utf-8'),
        'WORDMARK': data_url((SOURCE / 'arena-wordmark.png').read_bytes(), 'image/png'),
        'MARK': data_url((SOURCE / 'cobblemon-mark.png').read_bytes(), 'image/png'),
        'ICONS': (SOURCE / 'icons.js').read_text(encoding='utf-8'),
        'APP': (SOURCE / 'preview.js').read_text(encoding='utf-8'),
        'ASSETS': json.dumps(pokemon_assets(cobblemon, mega), separators=(',', ':')).replace('<', '\\u003c'),
    }
    template = (SOURCE / 'index.template.html').read_text(encoding='utf-8')
    html = re.sub(r'/\*@@([A-Z]+)@@\*/', lambda match: blocks[match[1]], template)
    options.output.parent.mkdir(parents=True, exist_ok=True)
    options.output.write_text(html, encoding='utf-8')
    print(options.output.resolve())


if __name__ == '__main__':
    main()
