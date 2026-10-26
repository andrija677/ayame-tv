#!/usr/bin/env bash
# Copy test artwork to a connected TV/emulator without replacing its launcher APK.
set -euo pipefail
project_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
serial=${1:-emulator-5580}
adb="$project_dir/.tooling/android-sdk/platform-tools/adb"
"$adb" -s "$serial" get-state >/dev/null
download_failed=0
python "$project_dir/scripts/prepare-test-wallpapers.py" || download_failed=1
private_gallery=/sdcard/Android/data/dev.andrija.ayametv/files/Pictures/Wallpapers
shared_gallery=/sdcard/Pictures/Ayame
"$adb" -s "$serial" shell mkdir -p "$private_gallery" "$shared_gallery"
shopt -s nullglob
images=("$project_dir"/.tooling/test-wallpapers/*.jpg "$project_dir"/.tooling/test-wallpapers/*.jpeg
        "$project_dir"/.tooling/test-wallpapers/*.png "$project_dir"/.tooling/test-wallpapers/*.webp)
[[ ${#images[@]} -gt 0 ]] || { echo 'No test wallpapers were prepared.' >&2; exit 1; }
for image in "${images[@]}"; do
    "$adb" -s "$serial" push "$image" "$private_gallery/"
    "$adb" -s "$serial" push "$image" "$shared_gallery/"
done
echo "Copied ${#images[@]} wallpapers. Reopen Settings → Wallpaper → Wallpaper gallery."
echo 'Your installed APK was kept, so you can still test the in-app update.'
if [[ "$download_failed" == 1 ]]; then
    echo 'Some anime downloads failed; available wallpapers were copied. See download errors above.' >&2
    exit 1
fi
