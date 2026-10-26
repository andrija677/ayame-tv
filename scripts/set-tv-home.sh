#!/usr/bin/env bash
# Set the preferred Home activity without disabling the manufacturer's launcher.
set -euo pipefail
project_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
export PATH="$project_dir/.tooling/android-sdk/platform-tools:$PATH"
serial=${1:?Usage: bash scripts/set-tv-home.sh ADB_SERIAL [restore]}
mode=${2:-set}
[[ "$mode" == set || "$mode" == restore ]] || { echo 'Mode must be set or restore.' >&2; exit 1; }
adb -s "$serial" get-state >/dev/null
user_id=$(adb -s "$serial" shell am get-current-user | tr -d '\r')
[[ "$user_id" =~ ^[0-9]+$ ]] || { echo 'Could not identify the TV user.' >&2; exit 1; }
mkdir -p "$project_dir/.tooling/home-backups"
backup_id=$(printf '%s' "$serial/$user_id" | sha256sum)
backup="$project_dir/.tooling/home-backups/${backup_id%% *}.txt"
component=dev.andrija.ayametv/.MainActivity
if [[ "$mode" == restore ]]; then
    [[ -s "$backup" ]] || { echo 'No saved previous Home app for this device/user.' >&2; exit 1; }
    component=$(cat "$backup")
else
    previous=$(adb -s "$serial" shell cmd package resolve-activity --brief --user "$user_id" -a android.intent.action.MAIN -c android.intent.category.HOME | tr -d '\r' | tail -n 1)
    if [[ ! -e "$backup" && "$previous" != dev.andrija.ayametv/* && "$previous" != android/* && "$previous" =~ ^[a-zA-Z0-9_.]+/[a-zA-Z0-9_.]+$ ]]; then
        printf '%s\n' "$previous" > "$backup"
    fi
fi
[[ "$component" =~ ^[a-zA-Z0-9_.]+/[a-zA-Z0-9_.]+$ ]] || { echo 'Invalid Home component.' >&2; exit 1; }
adb -s "$serial" shell cmd package set-home-activity --user "$user_id" "$component"
actual=$(adb -s "$serial" shell cmd package resolve-activity --brief --user "$user_id" -a android.intent.action.MAIN -c android.intent.category.HOME | tr -d '\r' | tail -n 1)
echo "Home resolves to: $actual"
normalize_component() {
    local package=${1%%/*} activity=${1#*/}
    if [[ "$activity" == .* ]]; then activity="$package$activity"; fi
    printf '%s/%s' "$package" "$activity"
}
if [[ "$(normalize_component "$actual")" != "$(normalize_component "$component")" ]]; then
    echo 'The firmware did not retain the selected Home app.' >&2
    exit 1
fi
adb -s "$serial" shell am start --user "$user_id" -a android.intent.action.MAIN -c android.intent.category.HOME
echo 'Test the remote Home button and reboot. Some firmware overrides the preferred Home app.'
echo "Restore the saved Home app with: bash scripts/set-tv-home.sh '$serial' restore"
