package dev.andrija.ayametv

import org.json.JSONObject
import java.net.URI

object ReleaseSource {
    const val repository = "andrija677/ayame-tv"
    const val packageName = "dev.andrija.ayametv"
}

data class UpdateManifest(
    val versionCode: Long, val versionName: String, val apkName: String, val sha256: String,
) {
    companion object {
        fun parse(json: JSONObject): UpdateManifest {
            require(json.getInt("schemaVersion") == 1) { "Unsupported update format" }
            require(json.getString("packageName") == ReleaseSource.packageName) { "Update is for a different app" }
            val code = json.getLong("versionCode")
            val name = json.getString("versionName")
            val apk = json.getString("apkName")
            val hash = json.getString("sha256").lowercase()
            require(code > 0 && code <= Int.MAX_VALUE) { "Invalid version code" }
            require(name.matches(Regex("[0-9]+\\.[0-9]+\\.[0-9]+(?:-[a-zA-Z0-9.-]+)?"))) { "Invalid version name" }
            require(apk == "ayame-tv-$name.apk") { "Unexpected APK name" }
            require(hash.matches(Regex("[0-9a-f]{64}"))) { "Missing update checksum" }
            return UpdateManifest(code, name, apk, hash)
        }
    }
}

data class AvailableUpdate(val manifest: UpdateManifest, val downloadUrl: String, val size: Long)

fun isTrustedUpdateUrl(value: String): Boolean = runCatching {
    val uri = URI(value)
    uri.scheme == "https" && uri.userInfo == null && (uri.port == -1 || uri.port == 443) &&
        uri.host in setOf("api.github.com", "github.com", "objects.githubusercontent.com", "release-assets.githubusercontent.com")
}.getOrDefault(false)
