#!/usr/bin/env bash
# Create a private Android TV AVD, boot it on the desktop, and install Ayame.
set -euo pipefail
project_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
boot_only=false
gpu_mode=auto
for option in "$@"; do
    case "$option" in
        --boot-only) boot_only=true ;;
        --software-gpu) gpu_mode=software ;;
        *) echo "Unknown option: $option. Supported: --boot-only --software-gpu" >&2; exit 1 ;;
    esac
done
export JAVA_HOME="$project_dir/.tooling/jdk"
export ANDROID_HOME="$project_dir/.tooling/android-sdk"
export ANDROID_AVD_HOME="$project_dir/.tooling/avd"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"
sdkmanager="$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager"
avdmanager="$ANDROID_HOME/cmdline-tools/latest/bin/avdmanager"
avd_name=Ayame_TV
serial=emulator-5580
apk="$project_dir/app/build/outputs/apk/debug/app-debug.apk"
[[ -x "$sdkmanager" && -x "$JAVA_HOME/bin/java" ]] || {
    echo 'Run scripts/setup-android.sh first.' >&2; exit 1;
}
if [[ "$boot_only" != true && ! -f "$apk" ]]; then
    echo 'Build the debug APK first: ./gradlew :app:assembleDebug' >&2; exit 1
fi
mkdir -p "$ANDROID_AVD_HOME"
if [[ ! -x "$ANDROID_HOME/emulator/emulator" ]]; then
    "$sdkmanager" --sdk_root="$ANDROID_HOME" emulator
fi
emulator="$ANDROID_HOME/emulator/emulator"
if ! "$emulator" -accel-check; then
    echo 'Hardware acceleration is unavailable in this terminal.' >&2
    echo 'On an Intel Arch PC, try: sudo modprobe kvm_intel' >&2
    echo 'Check /dev/kvm permissions; virtualization may also need enabling in BIOS.' >&2
    exit 1
fi
if [[ ! -f "$ANDROID_AVD_HOME/$avd_name.ini" ]]; then
    package_list="$project_dir/.tooling/tv-packages.txt"
    # Older TV images can be marked obsolete; newer stable images also work
    # with an app compiled against SDK 35. Keep the list for diagnosis.
    "$sdkmanager" --sdk_root="$ANDROID_HOME" --list --include_obsolete --channel=0 > "$package_list"
    if ! image_package=$(python "$project_dir/scripts/select-tv-image.py" "$package_list"); then
        echo "Package list saved to $package_list" >&2
        echo 'The SDK repository may have failed to load; preserve any warnings above.' >&2
        exit 1
    fi
    echo "Installing $image_package (this may download several GB)…"
    "$sdkmanager" --sdk_root="$ANDROID_HOME" "$image_package"
    # 'no' declines creation of a custom hardware profile; configure it below.
    "$avdmanager" create avd --name "$avd_name" --package "$image_package" <<< 'no'
    python - "$ANDROID_AVD_HOME/$avd_name.avd/config.ini" <<'PY'
from pathlib import Path
import sys
path = Path(sys.argv[1])
settings = dict(line.split('=', 1) for line in path.read_text().splitlines() if '=' in line)
settings.update({
    'hw.lcd.width': '1920', 'hw.lcd.height': '1080', 'hw.lcd.density': '320',
    'hw.ramSize': '2048', 'hw.cpu.ncore': '2', 'hw.dPad': 'yes',
    'hw.keyboard': 'yes', 'hw.mainKeys': 'yes', 'hw.battery': 'no',
    'hw.gpu.enabled': 'yes', 'hw.gpu.mode': 'auto',
    'hw.initialOrientation': 'landscape', 'hw.audioInput': 'no',
    'disk.dataPartition.size': '4G', 'avd.ini.displayname': 'Ayame TV · 1080p',
    'showDeviceFrame': 'no',
})
path.write_text(''.join(f'{key}={value}\n' for key, value in settings.items()))
PY
fi
adb start-server
if ! adb -s "$serial" get-state >/dev/null 2>&1; then
    # Native Qt Wayland support varies between emulator builds; use XWayland.
    # env applies only to the emulator process and does not change the desktop.
    nohup env QT_QPA_PLATFORM=xcb "$emulator" -avd "$avd_name" -port 5580 \
        -gpu "$gpu_mode" -no-boot-anim -no-snapshot-load \
        > "$project_dir/.tooling/tv-emulator.log" 2>&1 < /dev/null &
    emulator_pid=$!
    echo "Opening virtual TV (log: .tooling/tv-emulator.log; pid $emulator_pid)…"
fi
booted=false
for ((attempt=0; attempt<180; attempt++)); do
    if [[ $(adb -s "$serial" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r') == 1 ]] \
        && adb -s "$serial" shell pm path android 2>/dev/null | grep -q '^package:'; then
        booted=true
        break
    fi
    if [[ -n ${emulator_pid:-} ]] && ! kill -0 "$emulator_pid" 2>/dev/null; then
        tail -n 40 "$project_dir/.tooling/tv-emulator.log" >&2
        exit 1
    fi
    sleep 2
done
if [[ "$booted" != true ]]; then
    echo 'TV boot timed out. Check .tooling/tv-emulator.log.' >&2
    exit 1
fi
if [[ "$boot_only" == true ]]; then
    adb -s "$serial" shell am start -a android.intent.action.MAIN -c android.intent.category.HOME
    echo 'TV booted. Installed apps and user data were kept; no APK was installed.'
    exit 0
fi
install_log="$project_dir/.tooling/tv-install.log"
# Avoid streamed installation while the emulator is finishing its first boot.
# Retry transport failures only; package validation failures need diagnosis.
installed=false
for ((attempt=1; attempt<=3; attempt++)); do
    if adb -s "$serial" install --no-streaming -r "$apk" > "$install_log" 2>&1; then
        cat "$install_log"
        installed=true
        break
    fi
    cat "$install_log" >&2
    if ! grep -Eqi 'offline|device .*not found|connection reset|closed|broken pipe|no devices' "$install_log"; then
        break
    fi
    echo 'Emulator transport is not ready; retrying shortly…' >&2
    sleep 3
done
if [[ "$installed" != true ]]; then
    adb devices -l > "$project_dir/.tooling/tv-adb-devices.log" 2>&1 || true
    adb -s "$serial" logcat -d -t 250 -s PackageManager PackageInstaller AndroidRuntime \
        > "$project_dir/.tooling/tv-package-manager.log" 2>&1 || true
    echo "Installation failed. Exact error saved in $install_log" >&2
    exit 1
fi
adb -s "$serial" shell am start -n dev.andrija.ayametv/.MainActivity
echo 'Ayame is open. Use arrow keys, Enter (OK), and Escape (Back).'
echo 'Click the TV window first; extended controls (…) also include a TV D-pad.'
echo 'To reopen after rebuilding, run this script again.'
