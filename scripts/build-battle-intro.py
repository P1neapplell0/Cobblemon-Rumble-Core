#!/usr/bin/env python3
"""Build a self-contained local HTML preview from the installed game assets."""
import argparse
import base64
import hashlib
import io
import json
from pathlib import Path
import tarfile
from urllib.request import urlopen
from zipfile import ZipFile

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / 'docs/battle-intro'
THREE_URL = 'https://registry.npmjs.org/three/-/three-0.160.1.tgz'
THREE_SHA512 = 'Bgl2wPJypDOZ1stAxwfWAcJ0WQf7QzlptsxkjYiURPz+n5k4RBDLsq+6f9Y75TYxn6aHLcWz+JNmwTOXWrQTBQ=='
BEDROCK = 'assets/cobblemon/bedrock/'
TEXTURE = 'assets/cobblemon/textures/'


def first_asset(candidates, asset):
    for path in sorted(candidates):
        with ZipFile(path) as jar:
            if asset in jar.namelist():
                return path
    raise SystemExit(f'未找到 {asset}，请用 --minecraft 指定 Minecraft 1.21.1 客户端 JAR。')


def extract_assets(options):
    def installed(pattern):
        return next(iter(sorted((ROOT / 'libs').glob(pattern))), None)

    cobblemon = options.cobblemon or installed('Cobblemon-neoforge-*.jar')
    mega = options.mega or installed('mega_showdown-neoforge-*.jar')
    if not cobblemon or not mega:
        raise SystemExit('请将 Cobblemon 和 Mega Showdown 放入 libs/，或指定 --cobblemon 和 --mega。')
    player_textures = 'assets/minecraft/textures/entity/player/'
    minecraft = options.minecraft or first_asset(
        ROOT.glob('.gradle/**/minecraft-clientOnly-*.jar'), player_textures + 'wide/steve.png')

    def read(jar, path):
        return json.loads(jar.read(path))

    def data_url(jar, path):
        mime = 'image/png' if path.endswith('.png') else 'audio/ogg'
        return 'data:' + mime + ';base64,' + base64.b64encode(jar.read(path)).decode()

    with ZipFile(cobblemon) as c, ZipFile(mega) as m, ZipFile(minecraft) as mc:
        result = {}
        for model, skin in [('steve', 'wide/steve.png'), ('alex', 'slim/alex.png')]:
            result[model] = dict(geo=read(c, BEDROCK+f'npcs/models/{model}.geo.json'),
                                 image=data_url(mc, player_textures+skin))
        trainer_skin = ROOT / 'src/main/resources/assets/cobblemon_rumble_core/textures/entity/npc/fire_leader.png'
        result['trainer'] = dict(geo=result['steve']['geo'], image='data:image/png;base64,' +
                                 base64.b64encode(trainer_skin.read_bytes()).decode())
        for key, species, folder, model, jar, texture, clips in [
            ('charizard', 'charizard', '0006_charizard', 'charizard_mega_x', m, 'megacharizardx', ['battle_idle', 'cry']),
            ('mewtwo', 'mewtwo', '0150_mewtwo', 'mewtwo', c, 'mewtwo', ['ground_idle', 'surprised']),
        ]:
            prefix = BEDROCK+'pokemon/'
            all_clips = read(c, prefix+f'animations/{folder}/{species}.animation.json')['animations']
            result[key] = dict(
                geo=read(jar, prefix+f'models/{folder}/{model}.geo.json'),
                image=data_url(jar, TEXTURE+f'pokemon/{folder}/{texture}.png'),
                clips={name: all_clips[f'animation.{species}.{name}'] for name in clips},
                cry=data_url(c, f'assets/cobblemon/sounds/pokemon/{species}/{species}_cry.ogg'),
            )
        result['charizard']['glow'] = [data_url(m, TEXTURE+'pokemon/0006_charizard/x_glow'+suffix+'.png')
                                      for suffix in ('', '2', '3', '4')]
    return result


def three_bundle():
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


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('cobblemon', 'mega', 'minecraft'):
        parser.add_argument('--'+name, type=Path)
    parser.add_argument('--output', type=Path, default=ROOT / 'build/battle-intro/index.html')
    options = parser.parse_args()
    assets = extract_assets(options)
    template = (SOURCE/'index.template.html').read_text(encoding='utf-8')
    blocks = {'CSS': (SOURCE/'style.css').read_text(encoding='utf-8'), 'THREE': three_bundle(),
              'ASSETS': json.dumps(assets, separators=(',', ':')).replace('<', '\\u003c'),
              'APP': (SOURCE/'preview.js').read_text(encoding='utf-8')}
    for marker, text in blocks.items():
        template = template.replace(f'/*@@{marker}@@*/', text)
    options.output.parent.mkdir(parents=True, exist_ok=True)
    options.output.write_text(template, encoding='utf-8')
    print(options.output.resolve())


if __name__ == '__main__':
    main()
