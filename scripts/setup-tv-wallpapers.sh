#!/usr/bin/env bash
# Local emulator test assets and an upstream open-source document picker.
# Artwork is downloaded into ignored .tooling, never bundled into the app.
set -euo pipefail
project_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
adb="$project_dir/.tooling/android-sdk/platform-tools/adb"
serial=emulator-5580
asset_dir="$project_dir/.tooling/test-wallpapers"
mkdir -p "$asset_dir"
"$adb" -s "$serial" get-state >/dev/null

download_image() {
    local name=$1 source_url
    shift
    [[ -f "$asset_dir/$name" ]] && return 0
    for source_url in "$@"; do
        echo "Downloading $name from $source_url"
        if ! curl --fail --location --retry 1 --connect-timeout 15 --max-time 90 "$source_url" -o "$asset_dir/$name.part"; then
            echo 'Trying the next image source…' >&2
            continue
        fi
        if ! python - "$asset_dir/$name.part" <<'PY'
from pathlib import Path
import sys
data = Path(sys.argv[1]).read_bytes()
if not (data.startswith(b'\xff\xd8\xff') or data.startswith(b'\x89PNG\r\n\x1a\n') or data[:4] == b'RIFF'):
    raise SystemExit('The image host returned a page instead of a supported image.')
PY
        then
            continue
        fi
        mv "$asset_dir/$name.part" "$asset_dir/$name"
        return 0
    done
    rm -f "$asset_dir/$name.part"
    echo "Could not download $name; continuing with the file picker setup." >&2
    return 1
}

download_image marin-bright.webp \
    'https://cdn.oneesports.gg/wp-content/uploads/2022/02/Anime_MyDressUpDarling.webp' \
    'https://cdn.oneesports.gg/wp-content/uploads/2022/02/Anime_MyDressUpDarling-450x253.webp' || true
download_image marin-festival.jpg \
    'https://www.sma.co.jp/files/15/artist/169/462/FOx9Q2VacAULymA.jpg' \
    'https://otakuauthor.com/wp-content/uploads/2022/04/My-Dress-Up-Darling-Episode-12-Gojou-Wakana-and-Marin-Kitagawa-fireworks.jpg' || true
cat > "$asset_dir/SOURCES.txt" <<'EOF'
Local wallpaper test downloads; not part of Ayame TV's MIT-licensed artwork.
Marin bright: https://www.oneesports.gg/anime/reasons-to-watch-my-dress-up-darling/
Marin festival: https://www.sma.co.jp/s/sma/news/detail/97567?link=ROBO004
Festival fallback: https://otakuauthor.com/my-dress-up-darling-episode-12-my-dress-up-darling/
Characters and anime artwork belong to their respective rights holders.
EOF

echo 'Downloading Material Files release metadata from its official repository…'
metadata="$project_dir/.tooling/material-files-release.json"
curl --fail --location --retry 2 'https://api.github.com/repos/zhanghai/MaterialFiles/releases/latest' -o "$metadata"
readarray -t picker_asset < <(python - "$metadata" <<'PY'
import json, sys
release = json.load(open(sys.argv[1]))
assets = [a for a in release['assets'] if a['name'].endswith('.apk')]
if not assets: raise SystemExit('No APK found in the upstream Material Files release.')
asset = max(assets, key=lambda a: ('universal' in a['name'].lower(), not any(arch in a['name'].lower() for arch in ('arm', 'x86')), a['size']))
print(asset['browser_download_url'])
print(asset.get('digest') or 'unavailable')
PY
)
[[ ${#picker_asset[@]} == 2 ]] || exit 1
picker_apk="$project_dir/.tooling/material-files.apk"
curl --fail --location --retry 2 "${picker_asset[0]}" -o "$picker_apk.part"
if [[ ${picker_asset[1]} == sha256:* ]]; then
    printf '%s  %s\n' "${picker_asset[1]#sha256:}" "$picker_apk.part" | sha256sum --check --status
fi
"$project_dir/.tooling/android-sdk/build-tools/35.0.0/aapt2" dump badging "$picker_apk.part" \
    > "$project_dir/.tooling/material-files-badging.txt"
grep -q "package: name='me.zhanghai.android.files'" "$project_dir/.tooling/material-files-badging.txt"
mv "$picker_apk.part" "$picker_apk"
"$adb" -s "$serial" install --no-streaming -r "$picker_apk"
# File-manager access is granted only inside this dedicated emulator.
"$adb" -s "$serial" shell appops set me.zhanghai.android.files MANAGE_EXTERNAL_STORAGE allow
private_gallery=/sdcard/Android/data/dev.andrija.ayametv/files/Pictures/Wallpapers
shared_gallery=/sdcard/Pictures/Ayame
"$adb" -s "$serial" shell mkdir -p "$private_gallery" "$shared_gallery"
shopt -s nullglob
images=("$asset_dir"/*.jpg "$asset_dir"/*.webp)
for image in "${images[@]}"; do
    "$adb" -s "$serial" push "$image" "$private_gallery/"
    "$adb" -s "$serial" push "$image" "$shared_gallery/"
done
"$adb" -s "$serial" shell am force-stop dev.andrija.ayametv
"$adb" -s "$serial" shell am start -n dev.andrija.ayametv/.MainActivity
echo "Copied ${#images[@]} wallpapers. Open Settings → Wallpaper gallery to see available images."
echo 'Import wallpaper opens Material Files; browse Internal storage → Pictures → Ayame.'
