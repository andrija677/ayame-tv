#!/usr/bin/env bash
# Create one persistent local release identity. Secrets stay in ignored .signing/.
set -euo pipefail
umask 077
project_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
signing_dir="$project_dir/.signing"
keytool="$project_dir/.tooling/jdk/bin/keytool"
[[ -x "$keytool" ]] || { echo 'Run scripts/setup-android.sh first.' >&2; exit 1; }
if [[ -f "$signing_dir/signing.properties" ]]; then
    echo 'Existing release signing configuration preserved.'
    exit 0
fi
mkdir -p "$signing_dir"
[[ ! -e "$signing_dir/ayame-tv-release.jks" ]] || {
    echo 'A signing key already exists without its configuration. Restore its configuration before continuing.' >&2; exit 1;
}
export AYAME_STORE_PASSWORD
AYAME_STORE_PASSWORD=$(python -c 'import secrets; print(secrets.token_hex(24))')
"$keytool" -genkeypair -keystore "$signing_dir/ayame-tv-release.jks" -storetype JKS \
    -alias ayame-tv -keyalg RSA -keysize 3072 -validity 10000 \
    -storepass:env AYAME_STORE_PASSWORD -keypass:env AYAME_STORE_PASSWORD \
    -dname 'CN=Ayame TV, O=Ayame TV Open Source'
python - "$signing_dir" <<'PY'
import os, sys
from pathlib import Path
directory = Path(sys.argv[1])
password = os.environ['AYAME_STORE_PASSWORD']
(directory / 'signing.properties').write_text(
    'storeFile=.signing/ayame-tv-release.jks\n' +
    f'storePassword={password}\nkeyAlias=ayame-tv\nkeyPassword={password}\n')
PY
unset AYAME_STORE_PASSWORD
echo 'Release key created. Back up .signing/ securely; future releases must use this same key.'
echo 'The key and passwords are ignored by Git and must never be committed.'
