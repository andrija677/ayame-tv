#!/usr/bin/env bash
# Source-only Gradle bootstrap. Downloads the pinned official distribution,
# verifies its published checksum, then delegates to Gradle. No system install.
set -euo pipefail
project_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
gradle_version=8.11.1
tooling_dir="$project_dir/.tooling"
gradle_dir="$tooling_dir/gradle-$gradle_version"
if [[ -d "$tooling_dir/jdk" && -x "$tooling_dir/jdk/bin/java" ]]; then
    export JAVA_HOME="$tooling_dir/jdk"
    export PATH="$JAVA_HOME/bin:$PATH"
fi
if [[ -d "$tooling_dir/android-sdk" ]]; then
    export ANDROID_HOME="$tooling_dir/android-sdk"
fi
if ! command -v java >/dev/null; then
    echo 'Java 17 or newer is required. Run: bash scripts/setup-android.sh' >&2
    exit 1
fi
if [[ ! -x "$gradle_dir/bin/gradle" ]]; then
    command -v curl >/dev/null
    command -v unzip >/dev/null
    mkdir -p "$tooling_dir"
    download_dir=$(mktemp -d "$tooling_dir/gradle-download.XXXXXX")
    trap 'rm -rf -- "$download_dir"' EXIT
    distribution="https://services.gradle.org/distributions/gradle-$gradle_version-bin.zip"
    curl --fail --location --retry 3 "$distribution" -o "$download_dir/gradle.zip"
    expected=$(curl --fail --silent --show-error --location "$distribution.sha256")
    [[ "$expected" =~ ^[0-9a-fA-F]{64}$ ]] || { echo 'Invalid Gradle checksum response' >&2; exit 1; }
    printf '%s  %s\n' "$expected" "$download_dir/gradle.zip" | sha256sum --check --status
    unzip -q "$download_dir/gradle.zip" -d "$download_dir"
    mv -- "$download_dir/gradle-$gradle_version" "$gradle_dir"
fi
exec "$gradle_dir/bin/gradle" -p "$project_dir" "$@"
