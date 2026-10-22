package dev.andrija.ayametv

import kotlin.math.pow
import kotlin.math.roundToInt

data class TextProtection(val foreground: Int, val backing: Int, val opacity: Float)

fun colorLuminance(color: Int): Double {
    fun linear(channel: Int): Double {
        val value = channel / 255.0
        return if (value <= .04045) value / 12.92 else ((value + .055) / 1.055).pow(2.4)
    }
    return .2126 * linear(color ushr 16 and 255) + .7152 * linear(color ushr 8 and 255) + .0722 * linear(color and 255)
}

fun contrastRatio(first: Int, second: Int): Double {
    val a = colorLuminance(first)
    val b = colorLuminance(second)
    return (maxOf(a, b) + .05) / (minOf(a, b) + .05)
}

fun compositeBacking(backing: Int, wallpaper: Int, opacity: Float): Int {
    fun channel(shift: Int) = (((backing ushr shift and 255) * opacity) +
        ((wallpaper ushr shift and 255) * (1f - opacity))).roundToInt().coerceIn(0, 255)
    return (255 shl 24) or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
}

/** Retains the palette foreground when possible; protects it against any image pixel. */
fun protectText(preferred: Int, backing: Int): TextProtection {
    val black = 0xFF000000.toInt()
    val white = 0xFFFFFFFF.toInt()
    val foreground = if (contrastRatio(preferred, backing) >= 4.5) preferred
        else if (contrastRatio(black, backing) >= contrastRatio(white, backing)) black else white
    val textLuminance = colorLuminance(foreground)
    for (percent in 45..100) {
        val opacity = percent / 100f
        val darkest = compositeBacking(backing, black, opacity)
        val lightest = compositeBacking(backing, white, opacity)
        val staysOnOneSide = textLuminance < colorLuminance(darkest) || textLuminance > colorLuminance(lightest)
        if (staysOnOneSide && contrastRatio(foreground, darkest) >= 4.5 && contrastRatio(foreground, lightest) >= 4.5) {
            return TextProtection(foreground, backing, opacity)
        }
    }
    return TextProtection(foreground, backing, 1f)
}
