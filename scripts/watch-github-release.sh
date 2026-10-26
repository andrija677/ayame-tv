#!/usr/bin/env bash
set -euo pipefail
project_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
cd "$project_dir"
export PATH="$project_dir/.tooling/github-cli/bin:$PATH"
repository=andrija677/ayame-tv
mkdir -p .tooling
run_id=$(gh run list --repo "$repository" --workflow release.yml --event workflow_dispatch --limit 1 --json databaseId --jq '.[0].databaseId // empty')
[[ -n "$run_id" ]] || { echo 'No release retry found.' >&2; exit 1; }
if gh run watch "$run_id" --repo "$repository" --interval 15 --exit-status; then
    gh release view --repo "$repository" --json url,assets --jq '{url: .url, assets: [.assets[].name]}' | tee .tooling/github-release-status.json
else
    gh run view "$run_id" --repo "$repository" --log-failed > .tooling/github-build-errors.log 2>&1
    echo "Release failed. Saved details to $project_dir/.tooling/github-build-errors.log"
    exit 1
fi
