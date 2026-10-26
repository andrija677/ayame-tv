package dev.andrija.ayametv

import android.graphics.Bitmap
import kotlin.math.roundToInt

/** Small shared frost texture, computed off the UI thread once per wallpaper. */
internal fun frostedWallpaper(source: Bitmap): Bitmap {
    val scale = minOf(1f, 384f / maxOf(source.width, source.height))
    val width = (source.width * scale).roundToInt().coerceAtLeast(1)
    val height = (source.height * scale).roundToInt().coerceAtLeast(1)
    val reduced = Bitmap.createScaledBitmap(source, width, height, true)
    val pixels = IntArray(width * height)
    reduced.getPixels(pixels, 0, width, 0, 0, width, height)
    if (reduced !== source) reduced.recycle()
    val scratch = IntArray(pixels.size)
    // Three separable box passes approximate a soft Gaussian. Clamped edges
    // avoid dark fringes; the original wallpaper remains untouched.
    repeat(3) {
        boxBlur(pixels, scratch, width, height, horizontal = true)
        boxBlur(scratch, pixels, width, height, horizontal = false)
    }
    return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
}

private fun boxBlur(input: IntArray, output: IntArray, width: Int, height: Int, horizontal: Boolean) {
    val radius = 4
    val count = radius * 2 + 1
    val length = if (horizontal) width else height
    val lines = if (horizontal) height else width
    val stride = if (horizontal) 1 else width
    for (line in 0 until lines) {
        val start = if (horizontal) line * width else line
        fun pixel(position: Int) = input[start + position.coerceIn(0, length - 1) * stride]
        var a = 0; var r = 0; var g = 0; var b = 0
        fun add(color: Int, direction: Int) {
            a += (color ushr 24) * direction
            r += ((color ushr 16) and 255) * direction
            g += ((color ushr 8) and 255) * direction
            b += (color and 255) * direction
        }
        for (offset in -radius..radius) add(pixel(offset), 1)
        for (position in 0 until length) {
            output[start + position * stride] = ((a / count) shl 24) or
                ((r / count) shl 16) or ((g / count) shl 8) or (b / count)
            add(pixel(position - radius), -1)
            add(pixel(position + radius + 1), 1)
        }
    }
}
