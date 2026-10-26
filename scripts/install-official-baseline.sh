#!/usr/bin/env bash
# Replace the emulator preview with the downloaded signed 0.1.0 update baseline.
set -euo pipefail
project_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
export PATH="$project_dir/.tooling/jdk/bin:$project_dir/.tooling/android-sdk/platform-tools:$PATH"
apk="$HOME/Downloads/ayame-tv-0.1.0.apk"
serial=emulator-5580
package=dev.andrija.ayametv
backup="$project_dir/.tooling/baseline-backup-$(date +%Y%m%d-%H%M%S)"
[[ -f "$apk" ]] || { echo "APK missing: $apk" >&2; exit 1; }
"$project_dir/.tooling/android-sdk/build-tools/35.0.0/apksigner" verify "$apk"
badging=$("$project_dir/.tooling/android-sdk/build-tools/35.0.0/aapt2" dump badging "$apk")
[[ "$badging" == *"package: name='$package' versionCode='2' versionName='0.1.0'"* ]] || {
    echo 'Expected the official Ayame TV 0.1.0 baseline APK.' >&2; exit 1;
}
adb -s "$serial" get-state >/dev/null
mkdir -p "$backup"
adb -s "$serial" shell am force-stop "$package"
if ! adb -s "$serial" install --no-streaming -r "$apk" > "$backup/install.log" 2>&1; then
    cat "$backup/install.log"
    grep -q 'INSTALL_FAILED_UPDATE_INCOMPATIBLE' "$backup/install.log" || exit 1
    echo 'Saving preview settings and wallpaper before changing signing keys…'
    adb -s "$serial" exec-out run-as "$package" cat shared_prefs/ayame.xml > "$backup/preferences.xml"
    python - "$backup" <<'PY'
import json, sys, xml.etree.ElementTree as ET
from pathlib import Path
directory = Path(sys.argv[1])
root = ET.parse(directory / 'preferences.xml').getroot()
value = next((node.text for node in root.findall('string') if node.get('name') == 'preferences'), '{}')
(directory / 'ayame-before-release.json').write_text(json.dumps(json.loads(value or '{}'), indent=2) + '\n')
PY
    if adb -s "$serial" shell run-as "$package" test -f files/wallpaper.jpg; then
        adb -s "$serial" exec-out run-as "$package" cat files/wallpaper.jpg > "$backup/wallpaper.jpg"
    fi
    gallery=/sdcard/Android/data/dev.andrija.ayametv/files/Pictures/Wallpapers
    if adb -s "$serial" shell test -d "$gallery"; then
        adb -s "$serial" pull "$gallery" "$backup/Wallpapers"
    fi
    echo "Backup saved: $backup"
    adb -s "$serial" uninstall "$package"
    adb -s "$serial" install --no-streaming "$apk"
    adb -s "$serial" shell mkdir -p /sdcard/Download/Ayame-backup
    adb -s "$serial" push "$backup/ayame-before-release.json" /sdcard/Download/Ayame-backup/
    if [[ -f "$backup/wallpaper.jpg" ]]; then
        adb -s "$serial" push "$backup/wallpaper.jpg" /sdcard/Download/Ayame-backup/
    fi
    if [[ -d "$backup/Wallpapers" ]]; then
        adb -s "$serial" shell mkdir -p "$gallery"
        adb -s "$serial" push "$backup/Wallpapers/." "$gallery/"
    fi
    echo 'Import your saved theme from Downloads → Ayame-backup; reselect your wallpaper.'
fi
adb -s "$serial" shell am start -n "$package/.MainActivity"
echo 'Official 0.1.0 installed. Settings → System → Check for updates can now install the latest release.'
