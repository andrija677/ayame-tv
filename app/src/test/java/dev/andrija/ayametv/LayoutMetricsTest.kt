package dev.andrija.ayametv

import org.junit.Assert.*
import org.junit.Test

class LayoutMetricsTest {
    @Test fun gridFitsCommonTvWindowsAndFontScales() {
        // dp sizes cover different pixel resolutions, densities, and display scaling settings.
        for ((width, height) in listOf(640f to 360f, 960f to 540f, 1280f to 720f, 1920f to 1080f)) {
            for (scale in listOf(1f, 1.3f, 1.5f, 2f)) {
                for (requested in 3..7) {
                    val layout = LayoutMetrics.calculate(width, height, scale, requested)
                    val available = width - layout.horizontalInset * 2 - 20
                    val used = layout.tileWidth * layout.columns + 18 * (layout.columns - 1)
                    assertTrue("Grid overflows $width dp at $scale font scale", used <= available + .01f)
                    assertTrue(layout.columns in 1..requested)
                    assertTrue(layout.panelWidth + 24 <= width)
                }
            }
        }
    }

    @Test fun userPreferenceSurvivesTemporaryNarrowWindow() {
        assertTrue(LayoutMetrics.calculate(640f, 360f, 1.5f, 7).columns < 7)
        assertEquals(7, LayoutMetrics.calculate(1920f, 1080f, 1f, 7).columns)
    }

    @Test fun compactHeightRespondsToAccessibilityTextScaling() {
        assertFalse(LayoutMetrics.calculate(960f, 540f, 1f, 5).compactHeight)
        assertTrue(LayoutMetrics.calculate(960f, 540f, 1.5f, 5).compactHeight)
    }
}
