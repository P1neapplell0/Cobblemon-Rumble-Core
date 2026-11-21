#!/usr/bin/env python3
"""Recover the full-resolution wordmark from the chroma-key render and export window icons."""
import argparse
from pathlib import Path

import numpy as np
from PIL import Image, ImageFilter, ImageOps

TITLE = Path(__file__).resolve().parents[1] / 'docs/title-screen'


def wordmark(source: Path):
    rgb = np.asarray(Image.open(source).convert('RGB'), dtype=np.float32)
    key = np.median(np.concatenate((rgb[0], rgb[-1], rgb[:, 0], rgb[:, -1])), axis=0)
    red, green, blue = rgb.transpose(2, 0, 1)
    opaque = green < np.maximum(red, blue) + 5
    foreground = rgb.copy()
    known = opaque.copy()

    # Extend each solid edge color towards the green matte before solving for coverage.
    for _ in range(8):
        next_known = known.copy()
        for dy, dx in ((0, 1), (0, -1), (1, 0), (-1, 0), (1, 1), (-1, -1), (1, -1), (-1, 1)):
            neighbor = np.roll(known, (dy, dx), axis=(0, 1))
            new = neighbor & ~next_known
            foreground[new] = np.roll(foreground, (dy, dx), axis=(0, 1))[new]
            next_known |= neighbor
        known = next_known

    vector = foreground - key
    coverage = np.sum((rgb - key) * vector, axis=2) / np.maximum(1, np.sum(vector * vector, axis=2))
    coverage = np.where(opaque, 1, np.where(known, (coverage - .065) / .935, 0)).clip(0, 1)
    # PNG stores straight alpha. Keep edge RGB in the transparent gutter for GL filtering.
    rgba = np.dstack((foreground, coverage * 255)).round().astype(np.uint8)
    image = Image.fromarray(rgba)
    left, top, right, bottom = image.getbbox()
    image.crop((left - 4, top - 4, right + 4, bottom + 4)).save(TITLE / 'arena-wordmark.png', optimize=True)


def icons():
    root = TITLE / 'game' / 'window-icons'
    root.mkdir(parents=True, exist_ok=True)
    mark = Image.open(TITLE / 'cobblemon-mark.png').convert('RGBA')
    mark = mark.crop(mark.getbbox())
    canvas = Image.new('RGBA', (256, 256))
    mark.thumbnail((232, 232), Image.Resampling.LANCZOS)
    canvas.paste(mark, ((256 - mark.width) // 2, (256 - mark.height) // 2))
    for size in (16, 32, 48, 128, 256):
        canvas.resize((size, size), Image.Resampling.LANCZOS).save(root / f'icon_{size}x{size}.png', optimize=True)
    canvas.save(root / 'minecraft.icns')


def backdrop(source: Path):
    with Image.open(source) as frame:
        arena = ImageOps.fit(frame.convert('RGB'), (1280, 720), Image.Resampling.LANCZOS)
    blurred = arena.filter(ImageFilter.GaussianBlur(13))
    dim = Image.blend(blurred, Image.new('RGB', blurred.size, '#080e1b'), .32)
    dim.save(TITLE / 'game' / 'arena-blur.png', optimize=True)


if __name__ == '__main__':
    cli = argparse.ArgumentParser(description=__doc__)
    cli.add_argument('source', type=Path, nargs='?', help='full-resolution green-background wordmark PNG')
    cli.add_argument('--backdrop', type=Path, help='export a blurred arena from a title frame without menu controls')
    args = cli.parse_args()
    if args.backdrop:
        backdrop(args.backdrop)
    if args.source:
        wordmark(args.source)
    if args.source or not args.backdrop:
        icons()
