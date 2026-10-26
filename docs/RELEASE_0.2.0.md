# Ayame TV 0.2.0

- Settings is now a full launcher page over the wallpaper.
- Four home presets with previews: Ayame, Orbit, Quiet, and Cinema.
- Independent top-bar, app-tile, icon-size, clock-position, and date controls.
- Existing themes retain their original layout; new style controls export/import.
- Shared wallpaper frost reduces per-surface rendering work during navigation.
- Home-role selection and a reversible ADB helper for TVs with hidden Home settings.
- Expanded local wallpaper color-test scripts; third-party artwork stays outside the APK.

This is version code 3, following 0.1.0 (code 2). An installed official 0.1.0
release can discover this version under Settings → System → Check for updates
after GitHub publishes the signed release. Exported themes and settings are kept
by an in-place update using the same release key. Development APKs use another
key and require the one-time transition described in RELEASING.md.

Frame pacing and remote-Home behavior still need verification on Xiaomi firmware.
