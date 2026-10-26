#!/usr/bin/env bash
# Publish the public project and configure a signed GitHub release.
# Run in the desktop terminal so GitHub browser/device login can complete.
set -euo pipefail
umask 077
project_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
repository=andrija677/ayame-tv
cd "$project_dir"
if ! command -v gh >/dev/null; then
    cli_dir="$project_dir/.tooling/github-cli"
    if [[ ! -x "$cli_dir/bin/gh" ]]; then
        mkdir -p "$cli_dir"
        curl --fail --location --retry 2 https://api.github.com/repos/cli/cli/releases/latest -o "$cli_dir/release.json"
        python - "$cli_dir/release.json" "$cli_dir" <<'PY'
import json, sys
from pathlib import Path
release = json.load(open(sys.argv[1]))
assets = release['assets']
archive = next(a for a in assets if a['name'].endswith('_linux_amd64.tar.gz'))
checksums = next(a for a in assets if a['name'].endswith('_checksums.txt'))
Path(sys.argv[2], 'downloads.txt').write_text('\n'.join([archive['name'], archive['browser_download_url'], checksums['browser_download_url']]) + '\n')
PY
        readarray -t cli_assets < "$cli_dir/downloads.txt"
        curl --fail --location --retry 2 "${cli_assets[1]}" -o "$cli_dir/${cli_assets[0]}"
        curl --fail --location --retry 2 "${cli_assets[2]}" -o "$cli_dir/checksums.txt"
        python - "$cli_dir" "${cli_assets[0]}" <<'PY'
import hashlib, sys
from pathlib import Path
directory, name = Path(sys.argv[1]), sys.argv[2]
lines = [line.split() for line in (directory / 'checksums.txt').read_text().splitlines()]
expected = next(parts[0] for parts in lines if parts[-1].lstrip('*') == name)
with (directory / name).open('rb') as archive:
    assert hashlib.file_digest(archive, 'sha256').hexdigest() == expected, 'GitHub CLI checksum mismatch'
PY
        tar -xzf "$cli_dir/${cli_assets[0]}" --strip-components=1 -C "$cli_dir"
    fi
    export PATH="$cli_dir/bin:$PATH"
fi
if ! gh auth status --hostname github.com >/dev/null 2>&1; then
    gh auth login --hostname github.com --git-protocol https --web --scopes repo,workflow
fi
account=$(gh api user --jq .login)
[[ "$account" == andrija677 ]] || { echo "Expected GitHub account andrija677; currently authenticated as $account." >&2; exit 1; }
if [[ -z $(git config user.name || true) ]]; then
    git config user.name "$(gh api user --jq '.name // .login')"
fi
if [[ -z $(git config user.email || true) ]]; then
    git config user.email "$(gh api user --jq '(.id | tostring) + "+" + .login + "@users.noreply.github.com"')"
fi

# Finish checks before creating a public repository or publishing anything.
bash scripts/setup-release-signing.sh
./gradlew :app:assembleDebug :app:assembleRelease :app:testDebugUnitTest :app:lintRelease --console=plain
python scripts/test_select_tv_image.py
python scripts/test_release_metadata.py
python scripts/verify-wallpaper-pack.py --apk app/build/outputs/apk/release/app-release.apk
git add --all
if git diff --cached --name-only | grep -Eq '(^|/)(\.tooling|\.signing|\.gradle|build)(/|$)|\.(jks|keystore|apk)$'; then
    echo 'Unexpected build output or signing material is staged; publication stopped.' >&2
    exit 1
fi
if ! git diff --cached --quiet; then
    git commit -m 'Build Ayame TV launcher with signed GitHub updates'
fi
if ! gh repo view "$repository" >/dev/null 2>&1; then
    gh repo create "$repository" --public --description 'A wallpaper-adaptive, customizable Android TV launcher with glass surfaces and fluid motion.'
fi
if git remote get-url origin >/dev/null 2>&1; then
    remote_url=$(git remote get-url origin)
    [[ "$remote_url" == "https://github.com/$repository.git" || "$remote_url" == "git@github.com:$repository.git" ]] || {
        echo 'The origin remote does not match the intended repository.' >&2; exit 1;
    }
else
    git remote add origin "https://github.com/$repository.git"
fi

# Send the private key only to GitHub's encrypted repository secrets, never Git.
base64 -w0 .signing/ayame-tv-release.jks > .signing/secret-input
gh secret set AYAME_KEYSTORE_BASE64 --repo "$repository" < .signing/secret-input
for mapping in 'storePassword AYAME_STORE_PASSWORD' 'keyAlias AYAME_KEY_ALIAS' 'keyPassword AYAME_KEY_PASSWORD'; do
    read -r property secret_name <<< "$mapping"
    python - "$property" > .signing/secret-input <<'PY'
from pathlib import Path
import sys
values = dict(line.split('=', 1) for line in Path('.signing/signing.properties').read_text().splitlines() if '=' in line)
print(values[sys.argv[1]], end='')
PY
    gh secret set "$secret_name" --repo "$repository" < .signing/secret-input
done
rm -f .signing/secret-input
git -c credential.helper= -c 'credential.helper=!gh auth git-credential' push -u origin main
release_tag=$(python - <<'PY'
from pathlib import Path
values = dict(line.split('=', 1) for line in Path('version.properties').read_text().splitlines() if '=' in line)
print('v' + values['versionName'])
PY
)
if ! git rev-parse --verify "refs/tags/$release_tag" >/dev/null 2>&1; then
    git tag "$release_tag"
fi
git -c credential.helper= -c 'credential.helper=!gh auth git-credential' push origin "$release_tag"
echo "Source published: https://github.com/$repository"
echo "The signed release is building: https://github.com/$repository/actions"
echo "Release page: https://github.com/$repository/releases"
echo 'Keep a secure backup of .signing/ for all future releases.'
