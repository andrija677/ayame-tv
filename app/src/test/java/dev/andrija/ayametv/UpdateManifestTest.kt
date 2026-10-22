package dev.andrija.ayametv

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class UpdateManifestTest {
    private fun manifest() = JSONObject().apply {
        put("schemaVersion", 1)
        put("packageName", ReleaseSource.packageName)
        put("versionCode", 3)
        put("versionName", "0.1.1")
        put("apkName", "ayame-tv-0.1.1.apk")
        put("sha256", "a".repeat(64))
    }

    @Test fun parsesVersionCodesInsteadOfComparingVersionStrings() {
        assertEquals(3L, UpdateManifest.parse(manifest()).versionCode)
    }
    @Test(expected = IllegalArgumentException::class)
    fun rejectsDifferentPackage() { UpdateManifest.parse(manifest().put("packageName", "other.app")) }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsMissingChecksum() { UpdateManifest.parse(manifest().put("sha256", "")) }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsTraversalInApkNames() { UpdateManifest.parse(manifest().put("apkName", "../../different.apk")) }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsUnsupportedSchema() { UpdateManifest.parse(manifest().put("schemaVersion", 5)) }

    @Test fun acceptsOnlyHttpsGitHubDownloadHosts() {
        assertTrue(isTrustedUpdateUrl("https://github.com/andrija677/ayame-tv/releases/download/v0.1.1/app.apk"))
        assertTrue(isTrustedUpdateUrl("https://release-assets.githubusercontent.com/a/file"))
        assertFalse(isTrustedUpdateUrl("http://github.com/file"))
        assertFalse(isTrustedUpdateUrl("https://github.com.evil.example/file"))
        assertFalse(isTrustedUpdateUrl("https://evil.example@github.com/file"))
        assertFalse(isTrustedUpdateUrl("https://github.com:8443/file"))
        assertFalse(isTrustedUpdateUrl("file:///tmp/app.apk"))
    }
}
