# Ayame TV 0.3.0

- Open the new wallpaper-colored Quick Settings panel from the top navigation.
- Check connection status and open Android's Wi-Fi/network settings.
- Open Bluetooth/accessory settings and sound settings, with general-settings
  fallbacks for firmware that omits a particular public settings screen.
- Adjust media volume when Android exposes variable volume. Fixed-volume TVs
  show that the TV/audio device controls volume instead of presenting dead buttons.
- Switch light appearance, glass surfaces, and animations directly in the panel.
- Jump to full Ayame Settings; close with Back, Close, or a click outside the panel.
- Refresh local device state only while the panel is open and the activity is
  visible; pause refresh while Android's settings is in front.

Wi-Fi and Bluetooth entries open Android controls rather than silently toggling
radios. No location or Bluetooth runtime permission is needed. Normal network-state
and audio-settings permissions support status display and media-volume adjustment.
The panel requires on-device verification of firmware routes and audio behavior.

Version code 7, signed with the existing release key for in-place updates.
