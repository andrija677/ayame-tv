#!/usr/bin/env bash
# Downloads an isolated toolchain into this project. Never runs pacman or sudo.
set -euo pipefail
project_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
tooling_dir="$project_dir/.tooling"
mkdir -p "$tooling_dir"
for required in curl python unzip tar sha256sum; do
    command -v "$required" >/dev/null || { echo "Missing tool: $required" >&2; exit 1; }
done
[[ $(uname -m) == x86_64 ]] || { echo 'This setup script currently supports x86_64 Linux.' >&2; exit 1; }
download_dir=$(mktemp -d "$tooling_dir/setup.XXXXXX")
trap 'rm -rf -- "$download_dir"' EXIT
if [[ ! -x "$tooling_dir/jdk/bin/java" ]]; then
    echo 'Downloading Eclipse Temurin Java 17 metadata…'
    curl --fail --location --retry 3 \
        'https://api.adoptium.net/v3/assets/latest/17/hotspot?architecture=x64&image_type=jdk&os=linux&vendor=eclipse' \
        -o "$download_dir/jdk.json"
    readarray -t jdk_asset < <(python - "$download_dir/jdk.json" <<'PY'
import json, sys
data = json.load(open(sys.argv[1]))[0]['binary']['package']
print(data['link'])
print(data['checksum'])
PY
    )
    [[ ${#jdk_asset[@]} == 2 && ${jdk_asset[1]} =~ ^[0-9a-fA-F]{64}$ ]] || exit 1
    curl --fail --location --retry 3 "${jdk_asset[0]}" -o "$download_dir/jdk.tar.gz"
    printf '%s  %s\n' "${jdk_asset[1]}" "$download_dir/jdk.tar.gz" | sha256sum --check --status
    mkdir "$download_dir/jdk"
    tar -xzf "$download_dir/jdk.tar.gz" --strip-components=1 -C "$download_dir/jdk"
    mv "$download_dir/jdk" "$tooling_dir/jdk"
fi
export JAVA_HOME="$tooling_dir/jdk"
export PATH="$JAVA_HOME/bin:$PATH"
export ANDROID_HOME="$tooling_dir/android-sdk"
if [[ ! -x "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" ]]; then
    echo 'Resolving Android command-line tools from Google’s repository metadata…'
    curl --fail --location --retry 3 'https://dl.google.com/android/repository/repository2-1.xml' \
        -o "$download_dir/repository.xml"
    readarray -t sdk_asset < <(python - "$download_dir/repository.xml" <<'PY'
import sys, xml.etree.ElementTree as ET
root = ET.parse(sys.argv[1]).getroot()
candidates = []
for package in root.findall('remotePackage'):
    if not package.attrib.get('path', '').startswith('cmdline-tools;'): continue
    channel = package.find('channelRef')
    if channel is not None and channel.attrib.get('ref') != 'channel-0': continue
    for archive in package.findall('archives/archive'):
        if archive.findtext('host-os') != 'linux': continue
        revision = tuple(int(package.findtext('revision/' + part, '0')) for part in ('major','minor','micro'))
        complete = archive.find('complete')
        candidates.append((revision, complete.findtext('url'), complete.find('checksum').text))
if not candidates: raise SystemExit('No stable Linux Android tools found')
_, url, checksum = max(candidates)
print('https://dl.google.com/android/repository/' + url)
print(checksum)
PY
    )
    [[ ${#sdk_asset[@]} == 2 && ${sdk_asset[1]} =~ ^[0-9a-fA-F]{40}$ ]] || exit 1
    curl --fail --location --retry 3 "${sdk_asset[0]}" -o "$download_dir/android-tools.zip"
    # Google's repository metadata publishes SHA-1 for these archives.
    printf '%s  %s\n' "${sdk_asset[1]}" "$download_dir/android-tools.zip" | sha1sum --check --status
    unzip -q "$download_dir/android-tools.zip" -d "$download_dir/android"
    mkdir -p "$ANDROID_HOME/cmdline-tools"
    mv "$download_dir/android/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest"
fi
sdkmanager="$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager"
echo 'Review the Android SDK licenses when prompted.'
"$sdkmanager" --sdk_root="$ANDROID_HOME" --licenses
"$sdkmanager" --sdk_root="$ANDROID_HOME" 'platform-tools' 'platforms;android-35' 'build-tools;35.0.0'
echo 'Toolchain ready. Building Ayame TV…'
"$project_dir/gradlew" --no-daemon :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
echo "APK: $project_dir/app/build/outputs/apk/debug/app-debug.apk"
