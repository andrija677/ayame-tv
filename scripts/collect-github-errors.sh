#!/usr/bin/env bash
set -euo pipefail
project_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
cd "$project_dir"
gh_cli="$project_dir/.tooling/github-cli/bin/gh"
if command -v gh >/dev/null; then gh_cli=$(command -v gh); fi
mkdir -p .tooling
report="$project_dir/.tooling/github-build-errors.log"
"$gh_cli" run list --repo andrija677/ayame-tv --limit 20 --json databaseId,workflowName,conclusion,status > .tooling/github-runs.json
python - <<'PY' > .tooling/github-failed-run-ids
import json
from pathlib import Path
seen = set()
for run in json.loads(Path('.tooling/github-runs.json').read_text()):
    name = run['workflowName']
    if name not in seen and run['conclusion'] == 'failure':
        seen.add(name)
        print(run['databaseId'])
PY
: > "$report"
while IFS= read -r run_id; do
    "$gh_cli" run view "$run_id" --repo andrija677/ayame-tv --log-failed >> "$report" 2>&1
done < .tooling/github-failed-run-ids
if [[ ! -s "$report" ]]; then
    echo 'No failed workflow logs found. Recent runs:' >> "$report"
    "$gh_cli" run list --repo andrija677/ayame-tv --limit 5 >> "$report"
fi
echo "Saved workflow errors to $report"
