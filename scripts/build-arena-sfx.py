#!/usr/bin/env python3
"""Synthesize the original arena cues; requires FFmpeg with a Vorbis encoder."""
import math
from pathlib import Path
import random
import struct
import subprocess

RATE = 24000
OUT = Path(__file__).resolve().parents[1] / 'src/main/resources/assets/cobblemon_rumble_core/sounds/arena'


def sweep(t, f, slope):
    return math.sin(math.tau * (f * t + slope * t * t / 2))


def impact(t, noise, bass, metal):
    if t < 0:
        return 0
    attack = 1 - math.exp(-1800 * t)
    sub = math.exp(-8 * t) * sweep(t, bass, -52)
    body = math.exp(-16 * t) * sweep(t, bass * 2.25, -130)
    crack = math.exp(-68 * t) * (noise + .38 * sweep(t, 2910, -1400))
    ring = math.exp(-10 * t) * (sweep(t, metal, -40) + .43 * sweep(t, metal * 1.48, 15))
    return attack * (.92 * sub + .49 * body + .64 * crack + .33 * ring)


def rush(t, noise, duration):
    if not 0 <= t < duration:
        return 0
    x = t / duration
    return math.sin(math.pi * x) ** 1.4 * (x * .66 * noise + .39 * sweep(t, 91, 720)
                                           + .17 * sweep(t, 290, 1460))


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    # Arcade-style contrast: a swept lead-in, a sharp crack, then a low-end hit with a short metallic tail.
    cues = {
        'intro': (.84, lambda t, n: rush(t, n, .76) + .55 * impact(t - .51, n, 91, 610)),
        'face_off': (.72, lambda t, n: 1.23 * impact(t, n, 96, 780) + .43 * impact(t - .104, n, 71, 1120)),
        'fight': (.51, lambda t, n: .93 * impact(t, n, 135, 1240) + .34 * impact(t - .087, n, 182, 1810)),
    }
    encoders = subprocess.check_output(['ffmpeg', '-hide_banner', '-encoders'], text=True)
    codec = ['-c:a', 'libvorbis'] if 'libvorbis' in encoders else ['-ac', '2', '-c:a', 'vorbis', '-strict', 'experimental']
    for name, (duration, signal) in cues.items():
        rng = random.Random(name)
        data = bytearray()
        for i in range(round(RATE * duration)):
            t = i / RATE
            envelope = min(1, (duration - t) / .04)
            data += struct.pack('<h', round(31000 * envelope * math.tanh(signal(t, rng.uniform(-1, 1)))))
        subprocess.run(['ffmpeg', '-v', 'error', '-y', '-f', 's16le', '-ar', str(RATE), '-ac', '1', '-i', '-',
                        *codec, '-q:a', '5', str(OUT / (name + '.ogg'))], input=data, check=True)


if __name__ == '__main__':
    main()
