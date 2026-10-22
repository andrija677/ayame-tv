package dev.andrija.ayametv

import kotlin.math.floor

/** Uses window dp, never physical pixels. A 4K TV may expose the same dp as a 1080p TV. */
data class LayoutMetrics(
    val horizontalInset: Float,
    val verticalInset: Float,
    val columns: Int,
    val tileWidth: Float,
    val panelWidth: Float,
    val clockSize: Float,
    val compactHeight: Boolean,
) {
    companion object {
        fun calculate(width: Float, height: Float, fontScale: Float, requestedColumns: Int): LayoutMetrics {
            val safeWidth = width.coerceAtLeast(1f)
            val safeHeight = height.coerceAtLeast(1f)
            val inset = (safeWidth * .05f).coerceIn(16f, 64f).coerceAtMost(safeWidth / 8f)
            val content = (safeWidth - inset * 2 - 20f).coerceAtLeast(1f)
            val minimumTile = 100f * fontScale.coerceIn(1f, 2f)
            val fittingColumns = floor((content + 18f) / (minimumTile + 18f)).toInt().coerceAtLeast(1)
            val columns = requestedColumns.coerceIn(1, 7).coerceAtMost(fittingColumns)
            return LayoutMetrics(
                horizontalInset = inset,
                verticalInset = (safeHeight * .045f).coerceIn(12f, 40f).coerceAtMost(safeHeight / 8f),
                columns = columns,
                tileWidth = ((content - 18f * (columns - 1)) / columns).coerceIn(1f, 220f),
                panelWidth = (safeWidth * .46f).coerceAtLeast(320f).coerceAtMost(460f)
                    .coerceAtMost((safeWidth - 24f).coerceAtLeast(1f)),
                clockSize = (safeHeight * .125f / fontScale.coerceAtLeast(1f)).coerceIn(28f, 76f),
                compactHeight = safeHeight / fontScale.coerceAtLeast(1f) < 440f,
            )
        }
    }
}
