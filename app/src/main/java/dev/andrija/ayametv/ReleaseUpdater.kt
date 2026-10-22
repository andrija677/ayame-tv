package dev.andrija.ayametv

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

class ReleaseUpdater(private val context: Context) {
    private val maxApkBytes = 100L * 1024 * 1024

    @Suppress("DEPRECATION")
    fun installedVersion(): Pair<Long, String> {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        return versionCode(info) to (info.versionName ?: "unknown")
    }

    fun check(): AvailableUpdate? {
        val latest = try {
            JSONObject(readText("https://api.github.com/repos/${ReleaseSource.repository}/releases/latest"))
        } catch (_: NoPublishedRelease) { return null }
        require(!latest.optBoolean("draft") && !latest.optBoolean("prerelease")) { "Release is not stable" }
        val assets = latest.getJSONArray("assets")
        fun asset(name: String): JSONObject = (0 until assets.length()).map { assets.getJSONObject(it) }
            .firstOrNull { it.getString("name") == name } ?: error("Release is missing $name")
        fun assetUrl(asset: JSONObject): String {
            val url = asset.getString("browser_download_url")
            require(url.startsWith("https://github.com/${ReleaseSource.repository}/releases/download/")) { "Unexpected release source" }
            return url
        }
        val manifest = UpdateManifest.parse(JSONObject(readText(assetUrl(asset("update.json")))))
        require(latest.getString("tag_name") == "v${manifest.versionName}") { "Release version does not match its tag" }
        val apk = asset(manifest.apkName)
        val size = apk.getLong("size")
        require(size in 1..maxApkBytes) { "Update has an unsupported size" }
        return AvailableUpdate(manifest, assetUrl(apk), size)
    }

    fun download(update: AvailableUpdate, progress: (Int) -> Unit): File {
        val directory = File(context.cacheDir, "updates").apply { mkdirs() }
        val partial = File(directory, "download-part.apk")
        val destination = File(directory, "ayame-tv-update.apk")
        val digest = MessageDigest.getInstance("SHA-256")
        val connection = open(update.downloadUrl)
        try {
            var total = 0L
            var previousPercent = -1
            connection.inputStream.use { input ->
                partial.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        require(total <= update.size && total <= maxApkBytes) { "Update download is too large" }
                        output.write(buffer, 0, count)
                        digest.update(buffer, 0, count)
                        val percent = (total * 100 / update.size).toInt()
                        if (percent != previousPercent) { previousPercent = percent; progress(percent) }
                    }
                }
            }
            require(total == update.size) { "Update download was incomplete" }
            val actualHash = digest.digest().joinToString("") { "%02x".format(it.toInt() and 255) }
            require(actualHash == update.manifest.sha256) { "Update checksum did not match" }
            verifyApk(partial, update.manifest.versionCode)
            check(partial.renameTo(destination)) { "Could not save update" }
            return destination
        } finally {
            partial.delete()
            connection.disconnect()
        }
    }

    @Suppress("DEPRECATION")
    fun verifyApk(file: File, expectedVersion: Long? = null) {
        val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        val candidate = requireNotNull(context.packageManager.getPackageArchiveInfo(file.path, flags)) { "Downloaded APK is invalid" }
        val installed = context.packageManager.getPackageInfo(context.packageName, flags)
        require(candidate.packageName == context.packageName) { "Downloaded APK is for a different app" }
        require(versionCode(candidate) > versionCode(installed)) { "This update is not newer than the installed app" }
        if (expectedVersion != null) require(versionCode(candidate) == expectedVersion) { "APK version does not match release metadata" }
        fun signers(info: PackageInfo): Set<String> {
            val values = if (Build.VERSION.SDK_INT >= 28) info.signingInfo?.apkContentsSigners else info.signatures
            return values.orEmpty().map { signature ->
                MessageDigest.getInstance("SHA-256").digest(signature.toByteArray())
                    .joinToString("") { "%02x".format(it.toInt() and 255) }
            }.toSet()
        }
        val installedSigners = signers(installed)
        require(installedSigners.isNotEmpty() && signers(candidate) == installedSigners) {
            "Different signing key. Preview builds must be replaced with the official release once before using release updates. Export your theme first."
        }
    }

    private fun readText(url: String): String {
        val connection = open(url)
        try {
            val output = ByteArrayOutputStream()
            connection.inputStream.use { input ->
                val buffer = ByteArray(4096)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    require(output.size() + count <= 1024 * 1024) { "Release metadata is too large" }
                    output.write(buffer, 0, count)
                }
            }
            return output.toString("UTF-8")
        } finally { connection.disconnect() }
    }

    private fun open(rawUrl: String): HttpURLConnection {
        var url = rawUrl
        repeat(6) {
            require(isTrustedUpdateUrl(url)) { "Untrusted update download URL" }
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.setRequestProperty("User-Agent", "Ayame-TV-Updater")
            connection.setRequestProperty("Accept", "application/vnd.github+json, application/octet-stream, application/json")
            try {
                when (connection.responseCode) {
                    in 200..299 -> return connection
                    301, 302, 303, 307, 308 -> {
                        url = URL(URL(url), requireNotNull(connection.getHeaderField("Location"))).toString()
                        connection.disconnect()
                    }
                    404 -> { connection.disconnect(); throw NoPublishedRelease() }
                    403, 429 -> error("GitHub temporarily limited update checks. Try again later.")
                    else -> error("GitHub returned HTTP ${connection.responseCode}")
                }
            } catch (exception: Exception) { connection.disconnect(); throw exception }
        }
        error("Too many update download redirects")
    }

    @Suppress("DEPRECATION")
    private fun versionCode(info: PackageInfo): Long =
        if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()

    private class NoPublishedRelease : Exception("No published release is available yet")
}
