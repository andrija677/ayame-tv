# Development and emulator notes

## Local tools and builds

Run `bash scripts/setup-android.sh` from the project directory. Tools live under
`.tooling/`; no system-wide Gradle, SDK, or JDK installation is required.
The setup uses JDK 17, SDK/platform 35, build-tools 35.0.0, and Gradle 8.11.1.
Review the SDK license prompts. Allow several GB for downloads, caches, and output.

With an existing SDK, set `ANDROID_HOME`, install `platforms;android-35` and
`build-tools;35.0.0`, and use JDK 17. The project also opens in Android Studio.

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
python scripts/test_select_tv_image.py
python scripts/test_release_metadata.py
python scripts/verify-wallpaper-pack.py --apk app/build/outputs/apk/debug/app-debug.apk
```

Debug output is `app/build/outputs/apk/debug/app-debug.apk`. Signed builds and
GitHub publication are covered in [RELEASING.md](RELEASING.md).

## Emulator

`bash scripts/run-tv-emulator.sh` creates the private `Ayame_TV` AVD when needed,
boots it, installs the current debug APK, and opens Ayame. It uses port 5580,
serial `emulator-5580`, and `.tooling/avd` rather than a shared AVD directory.
KVM access, a graphical desktop, and XWayland are required. Initial image setup
can involve a multi-GB download. Android API 26+ is supported by the app; the
emulator image can be newer than the compile SDK.

To boot without installing an APK, including while testing signed releases:

```bash
bash scripts/run-tv-emulator.sh --boot-only
```

The normal renderer is `auto`. `--software-gpu` is an optional fallback to try
after stopping an emulator with graphics-driver problems; it can be slower and
does not work on every host. Restart before changing renderer options.

Logs live in `.tooling/tv-emulator.log` and `.tooling/tv-install.log`. The installer
waits for Android's package manager and uses non-streamed ADB installation.

`bash scripts/update-tv-launcher.sh` builds, checks, and installs the debug APK.
Use it for development; it does not preserve the release signing identity.
Avoid it during an official-release update test.

For the optional Material Files picker and shared test images, run
`bash scripts/setup-tv-wallpapers.sh`. All twelve built-in wallpapers are already
in the APK; these extra emulator files are not required by the gallery.

## Rendering and image handling

Layout is calculated in Android window dp and respects text scaling. A TV's
physical 4K output can expose the same dp as a 1080p output, so test both window
size and font scale. Narrow Settings uses a horizontal category strip.

Glass samples the wallpaper rather than refracting arbitrary app content.
Frost is prepared once per wallpaper on a worker dispatcher, at a maximum edge
of 384 pixels, and shared across surfaces. It does not use per-tile live GPU blur
or require Android 12's blur API. Animated focus effects use layer transforms.
Turning animations off removes focus, page, wallpaper, and palette transitions.

Images are processed off the main thread. Imports are bounded to 40 MB encoded
input and downsampled to at most 2560 × 1440. Gallery thumbnails are bounded to
384 × 216. Imported wallpapers are copied into private storage; the original
removable drive is not needed afterward. EXIF rotation is not currently applied.

The bundled pack is extracted into app cache only when the gallery opens.
Eviction recreates it from the APK. Follow [ARCHITECTURE.md](ARCHITECTURE.md) when
changing pack contents or cache versions. Source attribution and hashes ship
with the pack; CI checks that built APKs contain the exact source images.

## Device verification

Use the [device acceptance checks](DEVICE_TESTING.md) for D-pad routes, scaling,
memory, frame pacing, wallpaper contrast, and Home-button behavior. Compare
performance on signed release builds with the same settings and wallpaper.
Emulator GPU behavior is not a performance measurement of the Xiaomi Stick.

The app's Home-role picker and the reversible ADB helper leave the stock launcher
enabled. Some firmware still intercepts Home or resets its preferred launcher.
