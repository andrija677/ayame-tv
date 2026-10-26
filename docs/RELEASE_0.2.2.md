# Ayame TV 0.2.2

- Bundle twelve wallpapers directly in the APK: two each from My Dress-Up Darling,
  Roshidere, and Bunny Girl Senpai, plus six colorful user-provided wallpapers.
- Show the pack automatically in Settings → Wallpaper → Wallpaper gallery, even
  on a fresh installation without a network connection or file-manager app.
- Prepare gallery files and bounded thumbnails off the UI thread, on demand.
  Cache cleanup recreates the built-in pack without redownloading anything.
- Keep user imports and remove duplicate sample filenames from the gallery.
- Include artwork source attribution in the APK; artwork is separate from the
  project's MIT-licensed code.

The original images add about 17.6 MB before APK compression. This is version
code 5; official installations can update in place with the existing release key.
