package dev.andrija.ayametv

enum class Arrow { Up, Down, Left, Right }

/** -1 returns to the navigation bar; edges otherwise retain the current app. */
fun nextAppFocus(index: Int, count: Int, columns: Int, horizontal: Boolean, arrow: Arrow): Int {
    if (count <= 0) return -1
    val current = index.coerceIn(0, count - 1)
    if (horizontal) return when (arrow) {
        Arrow.Up -> -1
        Arrow.Down -> current
        Arrow.Left -> (current - 1).coerceAtLeast(0)
        Arrow.Right -> (current + 1).coerceAtMost(count - 1)
    }
    val width = columns.coerceAtLeast(1)
    return when (arrow) {
        Arrow.Up -> if (current < width) -1 else current - width
        Arrow.Down -> if (current + width < count) current + width else current
        Arrow.Left -> if (current % width > 0) current - 1 else current
        Arrow.Right -> if (current % width < width - 1 && current + 1 < count) current + 1 else current
    }
}
