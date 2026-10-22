#!/usr/bin/env bash
set -euo pipefail
project_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
"$project_dir/gradlew" :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --console=plain
bash "$project_dir/scripts/run-tv-emulator.sh"
bash "$project_dir/scripts/setup-tv-wallpapers.sh"
