package dev.andrija.ayametv

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.Collator

data class LauncherApp(
    val id: String,
    val label: String,
    val icon: Bitmap,
    val tvApp: Boolean,
    val banner: Bitmap? = null,
)

data class LocalWallpaper(val path: String, val label: String, val thumbnail: Bitmap)

data class Preferences(
    val seed: Int = 0xFFB6A0EF.toInt(),
    val automaticColors: Boolean = true,
    val light: Boolean = false,
    val animations: Boolean = true,
    val bouncyAnimations: Boolean = true,
    val glassEnabled: Boolean = true,
    val glassBlur: Boolean = true,
    val motionScale: Int = 100,
    val columns: Int = 5,
    val roundness: Int = 24,
    val surfaceOpacity: Int = 88,
    val wallpaperDim: Int = 28,
    val showClock: Boolean = true,
    val clock24: Boolean = true,
    val showLabels: Boolean = true,
    val showMobileApps: Boolean = false,
    val favorites: List<String> = emptyList(),
) {
    fun json(): JSONObject = JSONObject().apply {
        put("schemaVersion", 1)
        put("seed", seed)
        put("automaticColors", automaticColors)
        put("light", light)
        put("animations", animations)
        put("bouncyAnimations", bouncyAnimations)
        put("glassEnabled", glassEnabled)
        put("glassBlur", glassBlur)
        put("motionScale", motionScale)
        put("columns", columns)
        put("roundness", roundness)
        put("surfaceOpacity", surfaceOpacity)
        put("wallpaperDim", wallpaperDim)
        put("showClock", showClock)
        put("clock24", clock24)
        put("showLabels", showLabels)
        put("showMobileApps", showMobileApps)
        put("favorites", JSONArray(favorites))
    }

    companion object {
        fun parse(json: JSONObject): Preferences {
            require(json.optInt("schemaVersion", 1) == 1) { "Unsupported theme version" }
            val defaults = Preferences()
            val pins = json.optJSONArray("favorites") ?: JSONArray()
            require(pins.length() <= 300) { "Too many favorites" }
            return Preferences(
                seed = json.optInt("seed", defaults.seed) or 0xFF000000.toInt(),
                automaticColors = json.optBoolean("automaticColors", true),
                light = json.optBoolean("light", false),
                animations = json.optBoolean("animations", true),
                bouncyAnimations = json.optBoolean("bouncyAnimations", true),
                glassEnabled = json.optBoolean("glassEnabled", true),
                glassBlur = json.optBoolean("glassBlur", true),
                motionScale = json.optInt("motionScale", 100).coerceIn(50, 200),
                columns = json.optInt("columns", 5).coerceIn(3, 7),
                roundness = json.optInt("roundness", 24).coerceIn(0, 36),
                surfaceOpacity = json.optInt("surfaceOpacity", 88).coerceIn(50, 100),
                wallpaperDim = json.optInt("wallpaperDim", 28).coerceIn(0, 80),
                showClock = json.optBoolean("showClock", true),
                clock24 = json.optBoolean("clock24", true),
                showLabels = json.optBoolean("showLabels", true),
                showMobileApps = json.optBoolean("showMobileApps", false),
                favorites = (0 until pins.length()).map { pins.getString(it) }
                    .filter { id ->
                        val parts = id.split('/')
                        parts.size == 2 && parts.all { part -> part.isNotBlank() && part.none { it.isWhitespace() } }
                    }.distinct(),
            )
        }
    }
}

class LauncherStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("ayame", Context.MODE_PRIVATE)
    val wallpaperFile = File(context.filesDir, "wallpaper.jpg")
    val galleryDirectory = File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES), "Wallpapers")
        .apply { mkdirs() }

    fun gallery(): List<LocalWallpaper> = galleryDirectory.listFiles().orEmpty()
        .filter { it.isFile && it.extension.lowercase() in setOf("jpg", "jpeg", "png", "webp") }
        .sortedBy { it.name }.take(40).mapNotNull { file ->
            runCatching {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.path, bounds)
                require(bounds.outWidth > 0 && bounds.outHeight > 0)
                var sample = 1
                while (bounds.outWidth / sample > 384 || bounds.outHeight / sample > 216) sample *= 2
                val thumbnail = requireNotNull(BitmapFactory.decodeFile(file.path,
                    BitmapFactory.Options().apply { inSampleSize = sample }))
                LocalWallpaper(file.path, file.nameWithoutExtension.replace('-', ' '), thumbnail)
            }.getOrNull()
        }

    fun load(): Preferences = runCatching {
        Preferences.parse(JSONObject(prefs.getString("preferences", "{}") ?: "{}"))
    }.getOrDefault(Preferences())

    fun save(value: Preferences) {
        prefs.edit().putString("preferences", value.json().toString()).apply()
    }

    fun apps(): List<LauncherApp> {
        val pm = context.packageManager
        val found = linkedMapOf<String, LauncherApp>()
        // TV entry points win when an app also exposes a phone entry point.
        for (category in listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)) {
            val query = Intent(Intent.ACTION_MAIN).addCategory(category)
            @Suppress("DEPRECATION")
            val activities = pm.queryIntentActivities(query, PackageManager.MATCH_ALL)
            for (info in activities) {
                val activity = info.activityInfo ?: continue
                if (activity.packageName == context.packageName || !activity.exported || !activity.enabled) continue
                if (found.containsKey(activity.packageName)) continue
                runCatching {
                    val component = ComponentName(activity.packageName, activity.name)
                    val drawable = info.loadIcon(pm)
                    val aspect = if (drawable.intrinsicHeight > 0 && drawable.intrinsicWidth > 0)
                        drawable.intrinsicWidth.toFloat() / drawable.intrinsicHeight else 1f
                    found[activity.packageName] = LauncherApp(
                        id = component.flattenToString(),
                        label = info.loadLabel(pm).toString(),
                        icon = drawable.toBitmap(if (aspect >= 1f) 144 else (144 * aspect).toInt().coerceAtLeast(1),
                            if (aspect <= 1f) 144 else (144 / aspect).toInt().coerceAtLeast(1)),
                        tvApp = category == Intent.CATEGORY_LEANBACK_LAUNCHER,
                        banner = runCatching { activity.loadBanner(pm)?.toBitmap(384, 216) }.getOrNull(),
                    )
                }
            }
        }
        val collator = Collator.getInstance()
        return found.values.sortedWith { a, b -> collator.compare(a.label, b.label) }
    }

    fun wallpaper(): Bitmap? = runCatching {
        if (wallpaperFile.exists()) decode(wallpaperFile) else null
    }.getOrNull()

    private fun decode(file: File): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Choose a supported image" }
        var sample = 1
        while (bounds.outWidth / sample > 2560 || bounds.outHeight / sample > 1440) sample *= 2
        return requireNotNull(BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply {
            inSampleSize = sample
        })) { "Could not read wallpaper" }
    }

    fun importWallpaper(uri: Uri): Bitmap {
        val inputFile = File(context.cacheDir, "wallpaper-input")
        val temp = File(context.filesDir, "wallpaper-next.jpg")
        try {
            context.contentResolver.openInputStream(uri).use { input ->
                requireNotNull(input) { "Could not open image" }
                inputFile.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var total = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        require(total <= 40L * 1024 * 1024) { "Image must be smaller than 40 MB" }
                        output.write(buffer, 0, count)
                    }
                }
            }
            val image = decode(inputFile)
            temp.outputStream().use { check(image.compress(Bitmap.CompressFormat.JPEG, 94, it)) }
            check(temp.renameTo(wallpaperFile)) { "Could not save wallpaper" }
            return image
        } finally {
            inputFile.delete()
            temp.delete()
        }
    }

    fun wallpaperSeed(image: Bitmap): Int = Palette.from(image).maximumColorCount(24)
        .resizeBitmapArea(112 * 112).generate().let {
            it.vibrantSwatch?.rgb ?: it.mutedSwatch?.rgb ?: it.dominantSwatch?.rgb ?: Preferences().seed
        }

    fun exportTheme(uri: Uri, value: Preferences) {
        context.contentResolver.openOutputStream(uri, "wt").use {
            requireNotNull(it) { "Could not open theme file" }
                .write(value.json().toString(2).toByteArray(Charsets.UTF_8))
        }
    }

    fun importTheme(uri: Uri): Preferences {
        val bytes = context.contentResolver.openInputStream(uri).use {
            val input = requireNotNull(it) { "Could not open theme" }
            val output = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(4096)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                require(output.size() + count <= 65_536) { "Theme is too large" }
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        }
        require(bytes.size <= 65_536) { "Theme is too large" }
        return Preferences.parse(JSONObject(bytes.toString(Charsets.UTF_8)))
    }
}
