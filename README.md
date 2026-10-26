# Ayame TV

A quiet, colorful home for Android TV and Google TV. Inspired by the original
[Ayame Shell](https://github.com/andrija677/ayame-shell): wallpaper-led palettes,
floating surfaces, rounded controls, and deliberate motion. Built in Kotlin
and Jetpack Compose. MIT licensed.

**Early development:** signed releases and CI are operational. Ayame is being
tested in an Android TV emulator and on a Xiaomi TV Stick 4K, 2nd generation.
The launcher runs on the Stick, but frame pacing and firmware-specific Home
behavior are still being tuned.

## First version

- Discover and launch installed TV apps, with an optional non-TV app list.
- Search by name; pin, unpin, and reorder a favorites row.
- Import a local wallpaper and extract its accent using AndroidX Palette.
- Derive a coordinated light or dark palette; manual accent presets and hue control.
- Animated focus scaling, button colors, palette transitions, and settings entry.
- Full-page settings with a category rail and keyboard/remote navigation.
- Four editable home presets: Ayame, Orbit, Quiet, and Cinema. Mix full-bar,
  centered-dock, or minimal navigation with TV banners, rounded icons, or circles.
- Adjust icon size, clock alignment, and date visibility independently. Style
  presets retain your wallpaper, colors, favorites, and motion preferences.
- Adjust grid columns, corners, surface opacity, wallpaper dimming, clock, labels,
  and motion duration. Preferences persist between launches.
- Adapt columns, tile widths, margins, clock, and panel width to available window
  space and text scaling. The saved column preference remains unchanged when a
  smaller display temporarily needs fewer columns.
- Export and import a versioned JSON theme, including favorite order.
- Refresh the app list after installation, removal, and returning home.
- A manual GitHub release updater with APK checksum and signing-key checks.
- Shared, background-prepared wallpaper frost instead of per-tile live blur.
- No advertising, analytics, account, or app backend. Network access happens
  only when you request an update check or download.

This is an Android implementation of Ayame's design direction, not a QML port.
It uses local color extraction and HSL tonal surfaces rather than invoking
Matugen. Widgets, custom fonts, custom rows, and additional motion presets are
future work.

## Build on Arch / EndeavourOS

For personal-use wallpaper samples from My Dress-Up Darling, Roshidere, Bunny
Girl Senpai, and a local wallpaper collection, see
[color-test setup](docs/COLOR_TEST_WALLPAPERS.md). These images stay outside the APK.

From this project directory:

```bash
bash scripts/setup-android.sh
```

The script downloads a checksum-verified Temurin JDK 17, Android command-line
tools, SDK 35, and Gradle 8.11.1 into `.tooling/`. It asks you to review Google's
SDK licenses, then builds, runs unit tests, and lints. No sudo or AUR is needed.
Allow roughly 2–4 GB for tools, caches, and build output. Download sizes vary.

The source-only `gradlew` script bootstraps the pinned official Gradle
distribution rather than shipping a wrapper JAR. Subsequent builds:

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Alternatively use an existing JDK 17 and Android SDK, set `ANDROID_HOME`, install
`platforms;android-35` and `build-tools;35.0.0`, then run the same build command.
Android Studio can open the project as well.

APK output: `app/build/outputs/apk/debug/app-debug.apk`.
The GitHub Actions workflow also builds a debug APK artifact on pushes and PRs.
Signed public releases are produced by the tag-triggered GitHub workflow after
the repository's release-signing secrets have been configured.

## Try it on a TV

To test on your PC first, run:

```bash
bash scripts/run-tv-emulator.sh
```

This installs the Android emulator and a stable TV system image, creates a
project-local 1080p virtual TV, opens it, and installs the current debug APK.
It needs Linux KVM access, a graphical desktop with XWayland, and several GB
of additional disk space. SDK license prompts may appear. Use arrow keys, Enter,
and Escape to navigate. Emulator logs are in `.tooling/tv-emulator.log`.
The emulator is useful for layout and input testing; real Stick performance,
firmware integration, and remote-specific behavior still need hardware checks.

For the slower-motion demo with local Marin Kitagawa wallpaper samples and a
document picker, run `bash scripts/update-tv-demo.sh`. This rebuilds Ayame, opens
the virtual TV, installs Material Files from its official upstream release,
and downloads two anime images into ignored `.tooling/test-wallpapers/` and
the emulator. Sources are recorded alongside the downloads. The test images are
not bundled in the APK or licensed as part of this project. Settings → Wallpaper
gallery can apply them without any external picker; Import wallpaper uses
Material Files to browse `Pictures/Ayame`. The gallery samples live in app-owned
external storage and need no broad storage permission in Ayame.

Motion now uses slower eased transitions, animated tile lift, wallpaper dissolves,
and coordinated palette changes. The motion-duration control still adjusts the
pace, and disabling animations removes all those transitions.

For subsequent launcher-only updates, use `bash scripts/update-tv-launcher.sh`.
This preserves the installed file manager and downloaded wallpapers.

Settings use a category rail inspired by Lucid Shell, with a horizontal category
strip on narrow windows. Wallpaper, Appearance, Glass, Layout, Motion, and System
each have their own scrollable page. Home/All Apps selection has a moving pill;
pages fade and slide; focus, switches, and panel entrances use gentle springs.
Glass, wallpaper frost, animations, and bounce can each be toggled independently
and are included in theme exports. Motion duration also adjusts spring stiffness.

Glass samples the wallpaper region underneath each surface, adds tint and edge
highlights, and optionally blurs that image layer. It is a wallpaper-based glass
treatment, not optical refraction of arbitrary underlying apps. Compose backdrop
blur requires Android 12 or newer; older systems retain translucency and edges.
No idle animation runs continuously. Disable frost if profiling on TV hardware
shows that its GPU cost affects navigation.

The frost layer uses an opaque wallpaper sample so icons and labels underneath
settings do not bleed through. Surface tint is mostly neutral with a small
wallpaper hue contribution. The frame remains visually translucent through its
wallpaper sample, with subdued edge highlights.

Keyboard/remote navigation has explicit routes: Left/Right traverse the top bar
and select Home/All Apps on focus; Down enters the current app list and remembers
the last app; Up from the top grid row returns to the bar. Grid edges do not wrap
across rows or target missing cells. Search links the bar, text field, and results.
In settings, Up/Down select categories, Right enters the active page, and Left
from a card or the leftmost stepper returns to the category rail. Narrow windows
use a horizontal category strip with Down entering the content.

The main bar has no wordmark and uses consistent gaps between navigation items.
Typography is bundled Google Sans Flex with its rounded axis enabled and medium
or semibold weights. See [third-party notices](THIRD_PARTY_NOTICES.md) for its OFL.
Wallpaper-facing captions, hints, and the clock use palette-derived protective
backings. A contrast guard preserves the theme foreground when possible and
adjusts the backing opacity against bright, dark, and saturated image pixels;
glass tint also has a readability floor. Color matching alone does not ensure
readable text over arbitrary artwork.

Apps use native TV banners when provided, with fitted, undistorted icons as a
fallback. Tiles have a consistent 16:9 image surface and a separate caption,
rather than stretching each app's icon into a tall glass card. The home row
reserves enough height for that aspect ratio and the active text scale.

Enable developer options and the device's available debugging mode. Pair or
connect ADB using the address/port displayed by the TV; pairing and connection
ports can differ. With the device visible in `adb devices`:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n dev.andrija.ayametv/.MainActivity
```

Open Ayame from the current launcher's app list too. Testing it this way leaves
the existing home app available. After device testing, try Ayame Settings →
Home app settings. Availability of default-launcher selection depends on the
firmware. This project does not disable or uninstall Google's launcher.

Controls: D-pad to navigate; OK to select; hold OK or press Menu on an app for
pinning and ordering; Back closes the current panel/search or returns to Home.
Home resets Ayame's navigation when the firmware routes HOME intents to it.

Wallpaper and theme import/export require a system document picker or a file
manager that implements Android's document-provider UI. Some TV firmware omits
one; Ayame reports this rather than crashing. Wallpapers are downsampled and
copied into private app storage, so removable storage can be disconnected after
import. EXIF rotation is not currently applied; use a correctly oriented image.

## Share a rice

Export a JSON file from Settings and share it. A theme contains only appearance,
layout, motion, and favorite component IDs. Import replaces these settings.
Automatic-color themes derive their colors from the receiving device's current
wallpaper. Disable automatic colors before export to preserve a specific accent.
Wallpapers are not embedded; distribute artwork separately under its own license.
Unknown favorites stay in the saved theme and appear if those apps are installed.

See [device testing](docs/DEVICE_TESTING.md) and [architecture](docs/ARCHITECTURE.md).

## Contributing

Report device model, firmware/Android version, reproduction steps, and logs with
issues. Focus navigation, readable text at sofa distance, bounded image memory,
and responsiveness on modest TV hardware are requirements. Prefer brief,
interruptible animations. Please test all new controls using a D-pad without a
mouse. Keep platform integration outside visual components and document new
theme fields with a schema migration when necessary.
