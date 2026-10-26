# Personal wallpaper color tests

Run `bash scripts/add-color-test-wallpapers.sh` for the emulator, or pass a
connected TV's ADB serial as its first argument. The script keeps the installed
APK so that an older official release can still test the updater.

It copies six local images from `~/Pictures/wallpapers` when available: warm
sunset, spring green, orange clouds, pastel sky, deep-blue space, and a rainy city.
It also downloads two images from each of My Dress-Up Darling, Roshidere, and
Bunny Girl Senpai. These are existing anime/promotional images, not generated art.
Some promotional images are portrait-oriented and will be cropped by the TV.

Images live in ignored `.tooling/test-wallpapers`, then are copied to both the
app's wallpaper gallery and shared `Pictures/Ayame` on the target. They are not
included in the APK or the source license. Download/source attribution is saved
as `color-test-sources.json` beside the local test images.

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
