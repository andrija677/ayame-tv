#!/usr/bin/env python3
"""Prepare personal-use color tests in ignored storage; never bundle artwork."""
import argparse
import json
import shutil
import sys
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DESTINATION = ROOT / '.tooling/test-wallpapers'
LOCAL = Path.home() / 'Pictures/wallpapers'
LOCAL_IMAGES = [
    ('local-warm-sunset.jpg', 'pywal/city_sunset.jpg'),
    ('local-spring-green.jpg', 'pywal/spring.jpg'),
    ('local-orange-cloud.png', 'pywal/orange_cloud.png'),
    ('local-cotton-candy.jpg', 'pywal/cotton_candy.jpg'),
    ('local-deep-blue.png', 'nightfox/earth-in-space.png'),
    ('local-rain-city.png', 'nightfox/ign_cityRain.png'),
]
REMOTE_IMAGES = [
    ('marin-bright.webp', 'https://www.oneesports.gg/anime/reasons-to-watch-my-dress-up-darling/', [
        'https://cdn.oneesports.gg/wp-content/uploads/2022/02/Anime_MyDressUpDarling.webp',
        'https://cdn.oneesports.gg/wp-content/uploads/2022/02/Anime_MyDressUpDarling-450x253.webp']),
    ('marin-festival.jpg', 'https://www.sma.co.jp/s/sma/news/detail/97567?link=ROBO004', [
        'https://www.sma.co.jp/files/15/artist/169/462/FOx9Q2VacAULymA.jpg',
        'https://otakuauthor.com/wp-content/uploads/2022/04/My-Dress-Up-Darling-Episode-12-Gojou-Wakana-and-Marin-Kitagawa-fireworks.jpg']),
    ('roshidere-alya-pink.jpg', 'https://x.com/roshidere/status/1874289451985420423', [
        'https://pbs.twimg.com/media/GfyvB-KbsAATrGY.jpg?name=orig',
        'https://pbs.twimg.com/media/GfyvB-KbsAATrGY.jpg']),
    ('roshidere-alya-seaside.jpg', 'https://animecorner.me/roshidere-season-2-reveals-new-teaser-visual/', [
        'https://static.animecorner.me/2025/11/1762517011-d035dde3a0e0434d245a245869978d58.jpg',
        'https://realsound.jp/wp-content/uploads/2025/11/20251107-rosidere-02.jpg']),
    ('bunny-girl-senpai-sunset.jpg', 'https://ao-buta.com/special/icon_wp/index.html', [
        'https://ao-buta.com/special/icon_wp/assets/img/wp_sp.jpg',
        'https://rascaldoesnotdream.com/tv/special/icon-wp/assets/img/wp_sp.jpg']),
    ('bunny-girl-senpai-shrine.jpg', 'https://ao-buta.com/dearfriend/special/memorialvisual/digicon/', [
        'https://ao-buta.com/dearfriend/special/memorialvisual/digicon/assets/img/wp_03.jpg']),
]


def valid_image(data):
    return (data.startswith(b'\xff\xd8\xff') or data.startswith(b'\x89PNG\r\n\x1a\n')
            or (data[:4] == b'RIFF' and data[8:12] == b'WEBP'))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--offline', action='store_true', help='Copy local images without downloading')
    args = parser.parse_args()
    DESTINATION.mkdir(parents=True, exist_ok=True)
    sources = []
    failures = []
    for name, relative in LOCAL_IMAGES:
        original = LOCAL / relative
        if original.is_file():
            shutil.copyfile(original, DESTINATION / name)
            sources.append({'file': name, 'source': str(original)})
            print(f'Copied {name}')
    if not args.offline:
        for name, page, urls in REMOTE_IMAGES:
            target = DESTINATION / name
            if target.is_file() and valid_image(target.read_bytes()):
                sources.append({'file': name, 'source': page})
                continue
            for url in urls:
                try:
                    print(f'Downloading {name}', flush=True)
                    request = urllib.request.Request(url, headers={'User-Agent': 'AyameTV-WallpaperTest/1.0'})
                    with urllib.request.urlopen(request, timeout=25) as response:
                        data = response.read(20 * 1024 * 1024 + 1)
                    if len(data) > 20 * 1024 * 1024 or not valid_image(data):
                        raise ValueError('Host did not return a supported image below 20 MB')
                    temporary = target.with_suffix(target.suffix + '.part')
                    temporary.write_bytes(data)
                    temporary.replace(target)
                    sources.append({'file': name, 'source': page, 'download': url})
                    break
                except Exception as error:
                    print(f'{name}: {error}', file=sys.stderr)
            else:
                failures.append(name)
    (DESTINATION / 'color-test-sources.json').write_text(json.dumps(sources, indent=2) + '\n')
    print(f'Prepared {len(sources)} color-test wallpapers in {DESTINATION}')
    if failures:
        print('Downloads unavailable: ' + ', '.join(failures), file=sys.stderr)
        return 1
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
