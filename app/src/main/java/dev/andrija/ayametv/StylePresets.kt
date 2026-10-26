package dev.andrija.ayametv

enum class NavigationStyle(val label: String) { Bar("Full bar"), Dock("Centered dock"), Minimal("Minimal") }
enum class AppTileStyle(val label: String, val aspectRatio: Float) {
    Banner("TV banners", 16f / 9f), Square("Rounded icons", 1f), Circle("Circle icons", 1f),
}
enum class ClockPlacement(val label: String) { Left("Left"), Center("Center"), Right("Right") }

internal inline fun <reified T : Enum<T>> enumPreference(value: String, fallback: T): T =
    enumValues<T>().firstOrNull { it.name == value } ?: fallback

enum class HomeStyle(val label: String, val description: String) {
    Ayame("Ayame", "Wide TV banners, a full glass bar, and a left-aligned clock."),
    Orbit("Orbit", "A centered dock, circular app icons, and a centered clock."),
    Quiet("Quiet", "Minimal navigation, rounded icons, and solid surfaces."),
    Cinema("Cinema", "Wide banners and room for your wallpaper, without a clock.");

    // Copy only visual fields. Artwork, palette source, favorites, app visibility,
    // and accessibility/motion choices remain the user's own.
    fun applyTo(p: Preferences): Preferences = when (this) {
        Ayame -> p.copy(navigationStyle = NavigationStyle.Bar, appTileStyle = AppTileStyle.Banner,
            iconScale = 72, clockPlacement = ClockPlacement.Left, showClock = true, showDate = true,
            glassEnabled = true, glassBlur = true, roundness = 24, surfaceOpacity = 88, columns = 5, showLabels = true)
        Orbit -> p.copy(navigationStyle = NavigationStyle.Dock, appTileStyle = AppTileStyle.Circle,
            iconScale = 65, clockPlacement = ClockPlacement.Center, showClock = true, showDate = true,
            glassEnabled = true, glassBlur = true, roundness = 36, surfaceOpacity = 75, columns = 6, showLabels = true)
        Quiet -> p.copy(navigationStyle = NavigationStyle.Minimal, appTileStyle = AppTileStyle.Square,
            iconScale = 60, clockPlacement = ClockPlacement.Right, showClock = true, showDate = false,
            glassEnabled = false, glassBlur = false, roundness = 16, surfaceOpacity = 100, columns = 6, showLabels = true)
        Cinema -> p.copy(navigationStyle = NavigationStyle.Dock, appTileStyle = AppTileStyle.Banner,
            iconScale = 80, clockPlacement = ClockPlacement.Left, showClock = false, showDate = false,
            glassEnabled = true, glassBlur = false, roundness = 12, surfaceOpacity = 80, columns = 4, showLabels = true)
    }
}
