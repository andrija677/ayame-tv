# Publishing and updates

The intended public repository is `andrija677/ayame-tv`. Its stable GitHub releases
are the update source configured in `ReleaseSource.repository`.

## First publication

Run from a desktop terminal:

```bash
bash scripts/publish-github.sh
```

The script obtains the official GitHub CLI if necessary and opens GitHub login
when it needs authentication. It verifies the account is `andrija677`, creates a
persistent local signing key, builds the debug and signed release APKs, runs
checks, commits project source, creates the public repository if absent, uploads
the signing material to encrypted repository secrets, and pushes `main` plus the
version tag. No key, password, downloaded wallpaper, SDK, build output, or APK is
committed. The signed-release workflow builds and publishes the APK and its
`update.json` metadata. The script prints repository, Actions, and release URLs.

The ignored `.signing/` directory contains your private release key and passwords.
Keep a secure backup. Existing installations accept future releases only when
they use the same signing identity. Do not create a replacement key for each
version. Do not paste signing passwords into issues or chat.

## Later versions

Increase both `versionCode` and `versionName` in `version.properties`; the code
must always increase. Commit/push the change and create a matching `vVERSION`
tag. You can rerun the publishing script after bumping those values, or push the
tag manually with your configured Git credentials. Never move an existing
release tag to new code.

The workflow expects these encrypted repository secrets:

- `AYAME_KEYSTORE_BASE64`
- `AYAME_STORE_PASSWORD`
- `AYAME_KEY_ALIAS`
- `AYAME_KEY_PASSWORD`

Local signed builds read the same values from `.signing/signing.properties`.
CI reads them from environment variables and removes its temporary key afterward.
Routine pull-request CI does not receive signing secrets or publish releases.

## In-app flow

Settings → System → Check for updates retrieves the latest stable GitHub release
only after a user action. There is no startup check, polling service, or telemetry.
The manifest compares numeric version codes, and the app offers Download & install
only for newer versions. Downloads are limited to 100 MB, fetched over HTTPS
from allowed GitHub hosts, and checked against the release's SHA-256. Before the
installer opens, Ayame verifies package name, version, and exact signing-key set.
The APK remains in private cache and is exposed through a narrowly scoped
FileProvider grant. Android asks the user to allow this install source and
confirm installation. Ayame never silently installs an APK.

Debug builds use the local Android debug key, while public APKs use the private
release key. Switching from the emulator preview to the first official release
therefore requires a one-time replacement. Export the theme first, uninstall the
preview, install the official APK, then import the theme. Wallpaper files in
app-owned storage are removed by uninstalling; retain the original image files.
After that switch, official releases update in place without removing app data.

Checksums protect download integrity. They are not a substitute for the APK
signing-key check. Signing-key rotation is not implemented in this first updater.

## Verify before the first public release

Build, unit tests, and lint must pass. Test the no-release, up-to-date, newer-version,
offline, cancelled-install, missing-install-permission, and wrong-signing-key paths.
End-to-end updates require two APKs signed with the same release key and increasing
version codes. The first public release cannot demonstrate a later release by itself.
