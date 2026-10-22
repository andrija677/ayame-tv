#!/usr/bin/env bash
set -euo pipefail
project_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
cd "$project_dir"
export PATH="$project_dir/.tooling/github-cli/bin:$PATH"
repository=andrija677/ayame-tv
git add .github/workflows/android.yml .github/workflows/release.yml scripts/collect-github-errors.sh scripts/retry-github-release.sh docs/RELEASING.md
if ! git diff --cached --quiet; then
    git commit -m 'Fix obsolete SDK package in CI and allow release retries'
fi
git -c credential.helper= -c 'credential.helper=!gh auth git-credential' push origin main
release_tag=$(python - <<'PY'
from pathlib import Path
values = dict(line.split('=', 1) for line in Path('version.properties').read_text().splitlines() if '=' in line)
print('v' + values['versionName'])
PY
)
for attempt in 1 2 3 4 5; do
    if gh workflow run release.yml --repo "$repository" --ref main -f "release_tag=$release_tag"; then
        echo "Release retry started: https://github.com/$repository/actions"
        exit 0
    fi
    if [[ "$attempt" -lt 5 ]]; then sleep 2; fi
done
echo 'Could not start the release workflow. See the GitHub CLI error above.' >&2
exit 1
