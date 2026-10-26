#!/usr/bin/env python3
"""Check the built-in pack's image hashes, including the actual APK when supplied."""
import argparse
import hashlib
import json
import zipfile
from pathlib import Path

root = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--apk', type=Path)
args = parser.parse_args()
directory = root / 'app/src/main/assets/wallpapers'
manifest_bytes = (directory / 'SOURCES.json').read_bytes()
manifest = json.loads(manifest_bytes)
names = [entry['file'] for entry in manifest['images']]
assert len(names) == 12 and len(set(names)) == 12, 'Expected twelve distinct built-in wallpapers'
image_names = {path.name for path in directory.iterdir() if path.suffix.lower() in {'.jpg', '.jpeg', '.png', '.webp'}}
assert image_names == set(names), 'Image files and source attribution list do not match'
archive = zipfile.ZipFile(args.apk) if args.apk else None
try:
    if archive:
        assert archive.read('assets/wallpapers/SOURCES.json') == manifest_bytes, 'APK has a stale wallpaper manifest'
    for entry in manifest['images']:
        name = entry['file']
        assert Path(name).name == name and entry['source'], 'Invalid file name or missing attribution'
        data = (directory / name).read_bytes()
        assert hashlib.sha256(data).hexdigest() == entry['sha256'], f'Wallpaper hash mismatch: {name}'
        assert (data.startswith(b'\xff\xd8\xff') or data.startswith(b'\x89PNG\r\n\x1a\n')
                or (data[:4] == b'RIFF' and data[8:12] == b'WEBP')), f'Unsupported image: {name}'
        if archive:
            assert archive.read('assets/wallpapers/' + name) == data, f'Wallpaper missing or stale in APK: {name}'
finally:
    if archive:
        archive.close()
print(f'Verified {len(names)} bundled wallpapers' + (f' inside {args.apk}' if args.apk else ' in source assets'))
