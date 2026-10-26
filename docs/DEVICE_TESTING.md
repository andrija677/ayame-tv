# Device acceptance checks

Build first with `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug`.
Record device model, Android version, display scaling, and firmware version.

Repeat navigation at 720p, 1080p, and 4K output where supported, and with larger
Android display/text settings. Physical pixels and Android window dp are different:
record both. Include 640×360, 960×540, and 1280×720 dp emulator windows, plus
1.0×, 1.3×, 1.5×, and 2.0× text scales. Check every column setting from 3 through
7; the effective count should drop before tiles become too narrow. On shorter
windows the clock moves into the scrollable navigation bar. Panels must remain
within the window and their contents must scroll, including app options.

1. Install and open as an ordinary app. Confirm the stock home remains usable.
2. Use only the remote: visit every top-bar item, app, settings control, popup,
   and theme action. Focus must remain visible and scroll into view.
3. Open/close settings and app options with OK and Back repeatedly. Focus must
   return to the initiating control; hidden controls must not take D-pad focus.
4. Launch TV apps, including an app with separate phone and TV activities.
   Return home. Check optional non-TV apps and unavailable-app error handling.
5. Pin three apps; move them left/right; unpin; restart Ayame and verify order.
6. Install, update, and uninstall an app while Ayame is running. Revisit Ayame
   and verify the list updates without stale launch targets or duplicate apps.
7. Import a warm, cool, monochrome, very large, and invalid wallpaper. Check
   palette, readability, restart persistence, cancellation, and picker absence.
8. Test minimum/maximum columns, corners, opacity, and dimming in both modes.
   Toggle labels, clock, time format, and animations. No action requires touch.
9. Export a theme, change preferences, import, and verify restoration. Try
   malformed JSON, unsupported schema versions, and oversized files.
10. Press Back at the root; it should keep the home open. Check Home intent
    resets navigation when Ayame is selected as the launcher.
11. Rapidly navigate for several minutes; check no clipping, focus loss, stuck
    animations, rising memory, or image-decoding work on the main thread.
12. Switch Home/All Apps rapidly, then open/close settings, app options, and the
    gallery during transitions. Outgoing content must not take focus. Test every
    settings category with the remote, including the horizontal narrow layout.
13. Toggle glass, frost, animations, and bounce independently; export/import and
    relaunch to verify persistence. With animations off, tab indicators, switches,
    focus scaling, page changes, and dialogs must change immediately. Compare
    navigation frame pacing with frost enabled and disabled on the actual Stick.
14. Use arrow keys without clicking: select Home/All Apps in the bar, press Down
    into apps, traverse every grid edge and a partial final row, and press Up
    from the top row. Scroll beyond initially mounted apps, return to the bar,
    and press Down to check focus restoration. Search Up/Down must link the
    bar, text field, and results. In settings, test category Up/Down, Right into
    content, Left back to the rail, and minus/plus focus within each stepper.
15. On a bright wallpaper, open settings above the app grid. Confirm app labels
    underneath no longer show through the panel. Check text contrast in both
    appearances, with frost on/off and at the surface-tint extremes.
16. Verify the rounded font loads on the emulator and Stick. Check the bar's
    consistent gaps, title/hint/clock backings over very bright and very dark
    images, TV banners and icon-only apps, and larger text at every column count.
    Focus enlargement must leave captions and the 16:9 image surface unclipped.
17. Open Settings: Home tiles and the Home navigation bar must disappear, leaving
    a full page over the wallpaper. Test Back, Back to Home, and the remote Home
    button; focus must return to the originating navigation item.
18. Apply every home preset, then mix top bars, app tile shapes, icon size, clock
    position, and date visibility. Test both app pages at compact and large sizes.
    Export/import the custom style and an older theme without style fields.
    Presets must keep wallpaper colors, favorites, app visibility, and motion settings.
19. Change from a dark wallpaper to the brightest local/anime images while Settings
    is open. Verify the category rail has one continuous surface, with legible
    headings, selected/unselected labels, and footer. Repeat with glass/frost off,
    both appearances, and enlarged text. The content surface must stay legible too.
20. On a fresh 0.2.2 installation with networking off, open the wallpaper gallery.
    All twelve bundled images must appear without a picker or ADB copies. Apply
    one, restart, and verify persistence. Clear only app cache in Android settings
    and reopen the gallery; the built-in pack must recreate itself. User imports
    and the selected wallpaper must survive an ordinary in-place app update.
21. Open Quick Settings using only the remote in every navigation style and on a
    narrow window. Navigate all controls, close via Back/Close, and confirm focus
    returns to Quick settings. Open full Ayame Settings and verify its category
    rail owns focus. Press the remote Home button while the popup is open.
22. Check Wi-Fi, Ethernet, disconnected, and captive-portal status where available.
    Open Wi-Fi, Bluetooth, sound, and general settings; return and check state
    refresh. Test firmware that lacks a dedicated settings action. Change media
    volume with the panel and remote; on fixed-volume output the +/- controls
    must be absent. No radio toggle or location/Bluetooth permission prompt should
    appear. Pause the activity and close the popup to check status polling stops.
23. Toggle appearance, glass, and animations in Quick Settings. Check contrast on
    bright/dark wallpapers, immediate transitions with animations off, persistence
    after restart, and panel scrolling with enlarged text.

Useful diagnostics with an already connected TV:

For performance comparisons, use a signed release APK and the same wallpaper,
columns, animation settings, and output resolution. Reset frame counters with
`adb shell dumpsys gfxinfo dev.andrija.ayametv reset`, navigate the same route
for 30 seconds, then save `adb shell dumpsys gfxinfo dev.andrija.ayametv framestats`.
Compare frost on/off and check janky frames and frame-time percentiles. The frost
texture is now prepared once in the background and shared across glass surfaces;
there should be no per-surface live blur. Check wallpaper replacement and frost
toggles for correct colors, alignment, and memory settling after several changes.

## Default Home on a TV

Open Ayame Settings → System → Use Ayame as Home. On Android 10+ this requests
the Home role when available; otherwise it opens Android's Home settings.
If the TV hides both, connect ADB and run from the project directory:

```bash
bash scripts/set-tv-home.sh YOUR_ADB_SERIAL
```

Use the serial printed by `adb devices` (for network ADB, normally IP:port).
The script saves the previous resolved Home component when possible, sets Ayame
for the current user, verifies resolution, and starts Home. It leaves all stock
packages enabled. Test Home from another app and after reboot; firmware can
still intercept the remote or reset the preferred activity. Restore with
`bash scripts/set-tv-home.sh YOUR_ADB_SERIAL restore` if a previous Home was saved.
Do not uninstall Ayame while it is the selected Home app; restore Home first.

## Diagnostics

```bash
adb shell getprop ro.build.version.release
adb shell dumpsys meminfo dev.andrija.ayametv
adb shell dumpsys gfxinfo dev.andrija.ayametv
adb logcat -d -s AndroidRuntime
```

If the app crashes, preserve the exception and stack trace. Do not select it as
the default launcher until basic launch, persistence, and recovery checks pass.
