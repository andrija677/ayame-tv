# Personal wallpaper color tests

Version 0.2.2 bundles all twelve images directly in the APK. Open Settings →
Wallpaper → Wallpaper gallery on a fresh installation to use the pack offline.
No computer, external file manager, storage permission, or download is required.

The pack contains warm sunset, spring green, orange clouds, pastel sky, deep-blue
space, and a rainy city from the user's local collection, plus two images each
from My Dress-Up Darling, Roshidere, and Bunny Girl Senpai. These are existing
images, not generated art. Some promotional images are portrait-oriented and
will be cropped by the TV.

The gallery prepares cached files and bounded thumbnails on a worker dispatcher.
Clearing app cache recreates the built-in pack; applying an image still uses the
normal bounded wallpaper import and automatic palette extraction.

Per-image sources and hashes are in `app/src/main/assets/wallpapers/SOURCES.json`
and are included in the APK. Artwork is excluded from the source-code MIT license.

The optional `bash scripts/add-color-test-wallpapers.sh` copies images to shared
`Pictures/Ayame` on the emulator; pass a connected TV's ADB serial to target a TV.
This script keeps the installed APK and is no longer needed for the built-in gallery.

Sources:

- [My Dress-Up Darling / One Esports](https://www.oneesports.gg/anime/reasons-to-watch-my-dress-up-darling/)
- [My Dress-Up Darling / Sony Music Artists](https://www.sma.co.jp/s/sma/news/detail/97567?link=ROBO004)
- [Roshidere official New Year illustration](https://x.com/roshidere/status/1874289451985420423)
- [Roshidere seaside visual / Anime Corner](https://animecorner.me/roshidere-season-2-reveals-new-teaser-visual/)
- [Bunny Girl Senpai official wallpaper](https://ao-buta.com/special/icon_wp/index.html)
- [Bunny Girl Senpai official memorial wallpapers](https://ao-buta.com/dearfriend/special/memorialvisual/digicon/)

Use Settings → Wallpaper → Wallpaper gallery. Apply warm, green, blue, and pale
images in both light and dark appearance; verify accent changes, text readability,
glass alignment, and retention of the chosen home style. Try all four presets,
then alter their individual controls and export/import the resulting theme.
