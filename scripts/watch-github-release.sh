#!/usr/bin/env bash
set -euo pipefail
project_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
cd "$project_dir"
export PATH="$project_dir/.tooling/github-cli/bin:$PATH"
repository=andrija677/ayame-tv
mkdir -p .tooling
run_id=$(gh run list --repo "$repository" --workflow release.yml --limit 1 --json databaseId --jq '.[0].databaseId // empty')
[[ -n "$run_id" ]] || { echo 'No release build found.' >&2; exit 1; }
if gh run watch "$run_id" --repo "$repository" --interval 15 --exit-status; then
    release_tag=$(python - <<'PY'
from pathlib import Path
values = dict(line.split('=', 1) for line in Path('version.properties').read_text().splitlines() if '=' in line)
print('v' + values['versionName'])
PY
)
    gh release view "$release_tag" --repo "$repository" --json url,assets --jq '{url: .url, assets: [.assets[].name]}' | tee .tooling/github-release-status.json
else
    gh run view "$run_id" --repo "$repository" --log-failed > .tooling/github-build-errors.log 2>&1
    echo "Release failed. Saved details to $project_dir/.tooling/github-build-errors.log"
    exit 1
fi
