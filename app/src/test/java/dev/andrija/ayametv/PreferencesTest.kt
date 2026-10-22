package dev.andrija.ayametv

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class PreferencesTest {
    @Test fun themeRoundTripPreservesAppearanceAndFavoriteOrder() {
        val original = Preferences(
            seed = 0xFF68BBAA.toInt(), automaticColors = false, light = true,
            animations = false, bouncyAnimations = false, glassEnabled = false, glassBlur = false,
            motionScale = 175, columns = 7, roundness = 8, surfaceOpacity = 65,
            wallpaperDim = 40, showClock = false, clock24 = false, showLabels = false,
            showMobileApps = true, favorites = listOf("org.example.player/.TV", "org.example.browser/.Main"),
        )
        assertEquals(original, Preferences.parse(JSONObject(original.json().toString())))
    }

    @Test fun missingFieldsUseDefaults() {
        assertEquals(Preferences(), Preferences.parse(JSONObject()))
    }

    @Test fun glassAndMotionOptionsRemainIndependent() {
        val settings = Preferences.parse(JSONObject().put("glassEnabled", false).put("animations", true)
            .put("bouncyAnimations", false).put("glassBlur", true))
        assertFalse(settings.glassEnabled)
        assertTrue(settings.animations)
        assertFalse(settings.bouncyAnimations)
        assertTrue(settings.glassBlur)
    }

    @Test fun importedLayoutIsBounded() {
        val parsed = Preferences.parse(JSONObject().apply {
            put("columns", 500); put("roundness", -9)
            put("surfaceOpacity", 0); put("wallpaperDim", 999); put("motionScale", -100)
        })
        assertEquals(7, parsed.columns)
        assertEquals(0, parsed.roundness)
        assertEquals(50, parsed.surfaceOpacity)
        assertEquals(80, parsed.wallpaperDim)
        assertEquals(50, parsed.motionScale)
    }

    @Test(expected = IllegalArgumentException::class)
    fun futureSchemaCannotSilentlyOverwriteSettings() {
        Preferences.parse(JSONObject().put("schemaVersion", 99))
    }

    @Test fun invalidAndDuplicateFavoriteIdsAreRemovedWithoutSorting() {
        val parsed = Preferences.parse(JSONObject().put("favorites", JSONArray(listOf(
            "org.player/.TV", "broken", "org.player/.TV", "org.browser/.Home", "/", "bad name/.Main",
        ))))
        assertEquals(listOf("org.player/.TV", "org.browser/.Home"), parsed.favorites)
    }

    @Test(expected = IllegalArgumentException::class)
    fun oversizedFavoriteListsAreRejected() {
        Preferences.parse(JSONObject().put("favorites", JSONArray((0..300).map { "org.app$it/.TV" })))
    }

    @Test fun importedSeedIsAlwaysOpaque() {
        val parsed = Preferences.parse(JSONObject().put("seed", 0x00AABBCC))
        assertEquals(0xFFAABBCC.toInt(), parsed.seed)
    }
}
