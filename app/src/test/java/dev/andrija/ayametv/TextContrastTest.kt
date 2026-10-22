package dev.andrija.ayametv

import org.junit.Assert.*
import org.junit.Test

class TextContrastTest {
    @Test fun protectsPaletteTextAcrossBrightDarkAndColoredWallpaperPixels() {
        val colors = listOf(0xFF18141F, 0xFFF9F1FA, 0xFF986576, 0xFF547CAA, 0xFF839C68).map { it.toInt() }
        for (backing in colors) for (preferred in colors + listOf(-1, 0xFF000000.toInt())) {
            val protection = protectText(preferred, backing)
            for (level in 0..255 step 5) {
                for (pixel in listOf(0xFF000000.toInt() or (level shl 16) or (level shl 8) or level,
                    0xFFFF00FF.toInt(), 0xFF00FF00.toInt(), 0xFF0000FF.toInt())) {
                    val rendered = compositeBacking(backing, pixel, protection.opacity)
                    assertTrue("Unprotected foreground", contrastRatio(protection.foreground, rendered) >= 4.5)
                }
            }
        }
    }

    @Test fun preservesAnAlreadyReadableWallpaperTint() {
        val preferred = 0xFFFFD9ED.toInt()
        assertEquals(preferred, protectText(preferred, 0xFF18141F.toInt()).foreground)
    }
}
