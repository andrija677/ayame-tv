@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package dev.andrija.ayametv

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.Crossfade
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.core.graphics.ColorUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private data class ShellColors(
    val accent: Color, val onAccent: Color, val surface: Color,
    val elevated: Color, val text: Color, val muted: Color, val background: Color,
)

private fun colors(preferences: Preferences): ShellColors {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(preferences.seed, hsl)
    fun tone(saturation: Float, lightness: Float) = Color(ColorUtils.HSLToColor(
        floatArrayOf(hsl[0], saturation, lightness)))
    return if (preferences.light) ShellColors(
        tone(.45f, .36f), Color.White, tone(.28f, .94f), tone(.35f, .86f),
        tone(.22f, .12f), tone(.16f, .35f), tone(.26f, .88f),
    ) else ShellColors(
        tone(.68f, .80f), tone(.38f, .16f), tone(.20f, .13f), tone(.24f, .22f),
        tone(.25f, .94f), tone(.15f, .84f), tone(.25f, .065f),
    )
}

private val LocalShellColors = staticCompositionLocalOf { colors(Preferences()) }
private val LocalPreferences = staticCompositionLocalOf { Preferences() }
private val LocalBackdrop = staticCompositionLocalOf<Bitmap?> { null }
private val LocalSettingsRail = staticCompositionLocalOf<FocusRequester?> { null }
private val SoftEase = CubicBezierEasing(.22f, .61f, .36f, 1f)
private fun Preferences.duration(normal: Int = 220) = if (animations) normal * 2 * motionScale / 100 else 0

@Composable
private fun roundedTypography(): Typography {
    val context = LocalContext.current
    val font = remember(context) {
        FontFamily(android.graphics.Typeface.Builder(context.assets, "fonts/google_sans_flex.ttf")
            .setFontVariationSettings("'wght' 550, 'ROND' 100").build())
    }
    return remember(font) {
        val base = Typography()
        Typography(
            displayLarge = base.displayLarge.copy(fontFamily = font, fontWeight = FontWeight.SemiBold),
            displayMedium = base.displayMedium.copy(fontFamily = font, fontWeight = FontWeight.SemiBold),
            displaySmall = base.displaySmall.copy(fontFamily = font, fontWeight = FontWeight.SemiBold),
            headlineLarge = base.headlineLarge.copy(fontFamily = font, fontWeight = FontWeight.SemiBold),
            headlineMedium = base.headlineMedium.copy(fontFamily = font, fontWeight = FontWeight.SemiBold),
            headlineSmall = base.headlineSmall.copy(fontFamily = font, fontWeight = FontWeight.SemiBold),
            titleLarge = base.titleLarge.copy(fontFamily = font, fontWeight = FontWeight.SemiBold),
            titleMedium = base.titleMedium.copy(fontFamily = font, fontWeight = FontWeight.SemiBold),
            titleSmall = base.titleSmall.copy(fontFamily = font, fontWeight = FontWeight.SemiBold),
            bodyLarge = base.bodyLarge.copy(fontFamily = font, fontWeight = FontWeight.Medium),
            bodyMedium = base.bodyMedium.copy(fontFamily = font, fontWeight = FontWeight.Medium),
            bodySmall = base.bodySmall.copy(fontFamily = font, fontWeight = FontWeight.Medium),
            labelLarge = base.labelLarge.copy(fontFamily = font, fontWeight = FontWeight.SemiBold),
            labelMedium = base.labelMedium.copy(fontFamily = font, fontWeight = FontWeight.SemiBold),
            labelSmall = base.labelSmall.copy(fontFamily = font, fontWeight = FontWeight.SemiBold),
        )
    }
}

@Composable
private fun ProtectedLabel(text: String, modifier: Modifier = Modifier, fontSize: Int = 12) {
    val c = LocalShellColors.current
    val protection = remember(c.text, c.surface) { protectText(c.text.toArgb(), c.surface.toArgb()) }
    Box(modifier) {
        Text(text, color = Color(protection.foreground), fontSize = fontSize.sp, fontWeight = FontWeight.SemiBold,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.clip(RoundedCornerShape(14.dp))
                .background(Color(protection.backing).copy(alpha = protection.opacity))
                .padding(horizontal = 10.dp, vertical = 5.dp))
    }
}

private fun Preferences.movement(): FiniteAnimationSpec<Float> = when {
    !animations -> snap()
    bouncyAnimations -> spring(dampingRatio = .72f, stiffness = 100f * (100f / motionScale) * (100f / motionScale))
    else -> tween(duration(240), easing = SoftEase)
}

@Composable
private fun WallpaperGlassBackdrop(bitmap: Bitmap, modifier: Modifier = Modifier) {
    val view = LocalView.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current.density
    val windowWidth = configuration.screenWidthDp * density
    val windowHeight = configuration.screenHeightDp * density
    val image = remember(bitmap) { bitmap.asImageBitmap() }
    var origin by remember { mutableStateOf(Offset.Zero) }
    Canvas(modifier.onGloballyPositioned { coordinates ->
        val screen = IntArray(2)
        view.getLocationOnScreen(screen)
        origin = coordinates.positionInRoot() + Offset(screen[0].toFloat(), screen[1].toFloat())
    }) {
        // Sample the matching wallpaper region rather than repeating the full
        // artwork inside every tile. Match the root's ContentScale.Crop.
        val scale = maxOf(windowWidth / bitmap.width, windowHeight / bitmap.height)
        val cropX = (bitmap.width - windowWidth / scale) / 2f
        val cropY = (bitmap.height - windowHeight / scale) / 2f
        val left = (cropX + origin.x / scale).toInt().coerceIn(0, bitmap.width - 1)
        val top = (cropY + origin.y / scale).toInt().coerceIn(0, bitmap.height - 1)
        val width = (size.width / scale).toInt().coerceIn(1, bitmap.width - left)
        val height = (size.height / scale).toInt().coerceIn(1, bitmap.height - top)
        drawImage(image, srcOffset = IntOffset(left, top), srcSize = IntSize(width, height),
            dstSize = IntSize(size.width.roundToInt().coerceAtLeast(1), size.height.roundToInt().coerceAtLeast(1)))
    }
}

/** Wallpaper-backed frost; blur affects only the backdrop, never text or controls. */
@Composable
private fun GlassFrame(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val p = LocalPreferences.current
    val c = LocalShellColors.current
    val backdrop = LocalBackdrop.current
    val shape = RoundedCornerShape(p.roundness.dp)
    val neutral = if (p.light) Color(0xFFF1F4FA) else Color(0xFF171B25)
    val tint = if (p.glassEnabled) lerp(neutral, c.surface, .24f) else c.surface
    val protection = remember(tint, c.muted) { protectText(c.muted.toArgb(), tint.toArgb()) }
    val opacity = if (p.glassEnabled) protection.opacity + (1f - protection.opacity) *
        ((p.surfaceOpacity - 50) / 50f).coerceIn(0f, 1f) * .5f
        else maxOf(p.surfaceOpacity / 100f, protection.opacity)
    Box(modifier.clip(shape)) {
        if (p.glassEnabled && backdrop != null) {
            WallpaperGlassBackdrop(backdrop, modifier = Modifier.matchParentSize())
        }
        Box(Modifier.matchParentSize().background(tint.copy(alpha = opacity)))
        if (p.glassEnabled) {
            Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(
                Color.White.copy(alpha = .07f), Color.Transparent, c.accent.copy(alpha = .025f)))))
            Box(Modifier.matchParentSize().border(1.dp, Brush.linearGradient(listOf(
                Color.White.copy(alpha = .28f), Color.White.copy(alpha = .05f), c.accent.copy(alpha = .14f))), shape))
        }
        content()
    }
}

private fun shiftHue(seed: Int, degrees: Float): Int {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(seed, hsl)
    hsl[0] = (hsl[0] + degrees + 360f) % 360f
    return ColorUtils.HSLToColor(hsl)
}

@Composable
fun AyameLauncher(
    preferences: Preferences, apps: List<LauncherApp>, wallpaper: Bitmap?,
    loaded: Boolean, busy: Boolean, homeRequest: Int,
    installedVersion: String, updateStatus: String, updateBusy: Boolean, availableVersion: String?,
    onCheckUpdates: () -> Unit, onInstallUpdate: () -> Unit,
    gallery: List<LocalWallpaper>?, onOpenGallery: () -> Unit, onDismissGallery: () -> Unit,
    onSelectWallpaper: (LocalWallpaper) -> Unit,
    onPreferences: (Preferences) -> Unit, onLaunch: (LauncherApp) -> Unit,
    onWallpaper: () -> Unit, onClearWallpaper: () -> Unit, onExtractColors: () -> Unit,
    onExport: () -> Unit, onImport: () -> Unit,
    onSystemSettings: () -> Unit, onHomeSettings: () -> Unit,
) {
    val target = remember(preferences.seed, preferences.light) { colors(preferences) }
    val accent by animateColorAsState(target.accent, tween(preferences.duration(380), easing = SoftEase), label = "palette")
    val surface by animateColorAsState(target.surface, tween(preferences.duration(380), easing = SoftEase), label = "surface")
    val elevated by animateColorAsState(target.elevated, tween(preferences.duration(380), easing = SoftEase), label = "elevated")
    val foreground by animateColorAsState(target.text, tween(preferences.duration(380), easing = SoftEase), label = "text")
    val muted by animateColorAsState(target.muted, tween(preferences.duration(380), easing = SoftEase), label = "muted")
    val background by animateColorAsState(target.background, tween(preferences.duration(380), easing = SoftEase), label = "background")
    val onAccent by animateColorAsState(target.onAccent, tween(preferences.duration(380), easing = SoftEase), label = "accent text")
    val animated = ShellColors(accent, onAccent, surface, elevated, foreground, muted, background)
    val scheme = if (preferences.light) lightColorScheme(
        primary = accent, onPrimary = animated.onAccent, surface = surface,
        onSurface = animated.text, background = animated.background,
    ) else darkColorScheme(
        primary = accent, onPrimary = animated.onAccent, surface = surface,
        onSurface = animated.text, background = animated.background,
    )
    val frostEnabled = preferences.glassEnabled && preferences.glassBlur
    val frost by produceState<Bitmap?>(null, wallpaper, frostEnabled) {
        value = null
        if (frostEnabled && wallpaper != null) {
            value = withContext(Dispatchers.Default) { frostedWallpaper(wallpaper) }
        }
    }
    CompositionLocalProvider(LocalShellColors provides animated, LocalPreferences provides preferences,
        LocalBackdrop provides if (frostEnabled) frost else wallpaper) {
        MaterialTheme(colorScheme = scheme, typography = roundedTypography()) {
            var settings by remember { mutableStateOf(false) }
            val panelProgress by animateFloatAsState(if (settings) 1f else 0f,
                if (settings) preferences.movement() else tween(preferences.duration(220), easing = SoftEase),
                label = "settings unfold")
            var selected by remember { mutableStateOf<LauncherApp?>(null) }
            var cachedMenu by remember { mutableStateOf<LauncherApp?>(null) }
            var cachedGallery by remember { mutableStateOf<List<LocalWallpaper>?>(null) }
            LaunchedEffect(selected) { if (selected != null) cachedMenu = selected }
            LaunchedEffect(gallery) { if (gallery != null) cachedGallery = gallery }
            val menuProgress by animateFloatAsState(if (selected != null) 1f else 0f,
                if (selected != null) preferences.movement() else tween(preferences.duration(160)), label = "app menu")
            val galleryProgress by animateFloatAsState(if (gallery != null) 1f else 0f,
                if (gallery != null) preferences.movement() else tween(preferences.duration(180)), label = "gallery")
            var searching by remember { mutableStateOf(false) }
            var query by remember { mutableStateOf("") }
            var tab by remember { mutableStateOf("home") }
            var lastFocused by remember { mutableStateOf("nav:home") }
            var restoreKey by remember { mutableStateOf<String?>(null) }
            var restoreGeneration by remember { mutableIntStateOf(0) }
            var pendingRestore by remember { mutableStateOf(false) }
            val initialFocus = remember { FocusRequester() }
            val navigationFocus = remember { listOf(initialFocus, FocusRequester(), FocusRequester(), FocusRequester()) }
            val searchFocus = remember { FocusRequester() }
            val homeScroll = rememberLazyListState()
            val appsScroll = rememberLazyGridState()
            val appRequesters = remember { mutableMapOf<String, FocusRequester>() }
            val focusScope = rememberCoroutineScope()
            var lastAppId by remember { mutableStateOf<String?>(null) }
            var focusJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
            val visibleApps = remember(apps, preferences.showMobileApps) {
                apps.filter { preferences.showMobileApps || it.tvApp }
            }
            val pins = remember(visibleApps, preferences.favorites) {
                preferences.favorites.mapNotNull { id -> visibleApps.find { it.id == id } }
            }
            val results = remember(visibleApps, query) {
                visibleApps.filter { it.label.contains(query, ignoreCase = true) }
            }
            fun restore() {
                pendingRestore = true
            }
            fun closeSettings() { settings = false; restore() }
            fun closeMenu() { selected = null; restore() }
            fun updatePins(app: LauncherApp) {
                val ids = preferences.favorites
                onPreferences(preferences.copy(favorites = if (app.id in ids) ids - app.id else ids + app.id))
            }
            LaunchedEffect(pendingRestore, settings, selected, panelProgress, menuProgress) {
                if (pendingRestore && !settings && selected == null && panelProgress == 0f && menuProgress == 0f) {
                    val displayed = if (tab == "home" && !searching) {
                        if (pins.isEmpty()) visibleApps else pins
                    } else results
                    restoreKey = if (lastFocused.startsWith("nav:") || displayed.any { it.id == lastFocused }) {
                        lastFocused
                    } else "nav:home"
                    restoreGeneration++
                    pendingRestore = false
                }
            }
            LaunchedEffect(loaded) {
                if (loaded) { delay(80); initialFocus.requestFocus() }
            }
            LaunchedEffect(homeRequest) {
                if (homeRequest > 0) {
                    settings = false; selected = null; searching = false; query = ""; tab = "home"
                    lastFocused = "nav:home"
                    pendingRestore = true
                }
            }
            LaunchedEffect(searching) {
                if (searching) { delay(80); searchFocus.requestFocus() }
            }
            BackHandler {
                when {
                    selected != null -> closeMenu()
                    settings -> closeSettings()
                    searching -> { searching = false; query = ""; restore() }
                    tab != "home" -> { tab = "home"; initialFocus.requestFocus() }
                    else -> initialFocus.requestFocus()
                }
            }
            BoxWithConstraints(Modifier.fillMaxSize().background(animated.background)) {
                val fontScale = LocalDensity.current.fontScale
                val windowWidth = maxWidth
                val layout = LayoutMetrics.calculate(windowWidth.value, maxHeight.value, fontScale, preferences.columns)
                fun requestApp(index: Int, horizontal: Boolean, list: List<LauncherApp>) {
                    val app = list.getOrNull(index) ?: return
                    val requester = appRequesters.getOrPut("${if (horizontal) "home" else "apps"}:${app.id}") { FocusRequester() }
                    focusJob?.cancel()
                    focusJob = focusScope.launch {
                        if (horizontal) {
                            if (homeScroll.layoutInfo.visibleItemsInfo.none { it.index == index }) homeScroll.scrollToItem(index)
                        } else {
                            if (appsScroll.layoutInfo.visibleItemsInfo.none { it.index == index }) appsScroll.scrollToItem(index)
                        }
                        withFrameNanos { }
                        if ((tab == "home" && !searching) == horizontal) requester.requestFocus()
                    }
                }
                fun navigateApp(index: Int, horizontal: Boolean, list: List<LauncherApp>, key: Key): Boolean {
                    val arrow = when (key) {
                        Key.DirectionUp -> Arrow.Up
                        Key.DirectionDown -> Arrow.Down
                        Key.DirectionLeft -> Arrow.Left
                        Key.DirectionRight -> Arrow.Right
                        else -> return false
                    }
                    val next = nextAppFocus(index, list.size, layout.columns, horizontal, arrow)
                    if (next < 0 && searching) searchFocus.requestFocus()
                    else if (next < 0) navigationFocus[if (tab == "home") 0 else 1].requestFocus()
                    else if (next != index) requestApp(next, horizontal, list)
                    return true
                }
                Wallpaper(wallpaper)
                if (!settings && panelProgress == 0f) {
                Column(Modifier.fillMaxSize().focusProperties { canFocus = selected == null }
                    .padding(horizontal = layout.horizontalInset.dp, vertical = layout.verticalInset.dp)) {
                    TopNavigation(tab, layout.compactHeight, navigationFocus, restoreKey, restoreGeneration,
                        onFocus = { lastFocused = it },
                        onDown = {
                            val horizontal = tab == "home" && !searching
                            val list = if (horizontal) { if (pins.isEmpty()) visibleApps else pins } else results
                            if (searching) searchFocus.requestFocus()
                            else requestApp(list.indexOfFirst { it.id == lastAppId }.coerceAtLeast(0), horizontal, list)
                        },
                        onHome = { tab = "home"; searching = false; query = "" },
                        onApps = { tab = "apps"; searching = false; query = "" },
                        onSearch = { searching = !searching; query = "" },
                        onSettings = { settings = true })
                    AnimatedContent(targetState = tab to searching, modifier = Modifier.weight(1f),
                        transitionSpec = {
                            (fadeIn(tween(preferences.duration(220))) +
                                slideInVertically(tween(preferences.duration(260), easing = SoftEase)) { it / 14 }) togetherWith
                                (fadeOut(tween(preferences.duration(100))) +
                                    slideOutVertically(tween(preferences.duration(180), easing = SoftEase)) { -it / 20 })
                        }, label = "launcher page") { (page, searchMode) ->
                        Column(Modifier.fillMaxSize().focusProperties { canFocus = page == tab && searchMode == searching }
                            .then(if (layout.compactHeight && page == "home" && !searchMode)
                                Modifier.verticalScroll(rememberScrollState()) else Modifier)) {
                    Spacer(Modifier.height(if (layout.compactHeight) 10.dp else 24.dp))
                    if (preferences.showClock && page == "home" && !searchMode && !layout.compactHeight) {
                        Clock(preferences.clock24, layout.clockSize, modifier = Modifier.weight(1f))
                    } else {
                        Spacer(Modifier.height(6.dp))
                    }
                    if (searchMode) {
                        BasicTextField(
                            value = query, onValueChange = { query = it }, singleLine = true,
                            textStyle = TextStyle(color = animated.text, fontSize = 21.sp),
                            cursorBrush = androidx.compose.ui.graphics.SolidColor(accent),
                            modifier = Modifier.fillMaxWidth().focusRequester(searchFocus)
                                .onPreviewKeyEvent { event ->
                                    if (event.type != KeyEventType.KeyDown) false
                                    else when (event.key) {
                                        Key.DirectionDown -> { requestApp(0, false, results); true }
                                        Key.DirectionUp -> { navigationFocus[2].requestFocus(); true }
                                        else -> false
                                    }
                                }
                                .clip(RoundedCornerShape(18.dp)).background(surface).padding(18.dp),
                            decorationBox = { inner ->
                                if (query.isEmpty()) Text("Search your apps…", color = animated.muted, fontSize = 21.sp)
                                inner()
                            },
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                    val home = page == "home" && !searchMode
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        ProtectedLabel(if (searchMode) "Search results" else if (home) "Your space" else "All apps",
                            modifier = Modifier.weight(1f), fontSize = 22)
                        if (windowWidth >= 800.dp && !layout.compactHeight) {
                            ProtectedLabel(if (home) "Hold OK to make it yours" else "${results.size} apps · Hold OK for options")
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    if (home) {
                        val shown = if (pins.isEmpty()) visibleApps else pins
                        if (shown.isEmpty()) {
                            EmptyState(if (loaded) "Your apps will appear here" else "Getting your space ready…",
                                "Open Settings to include apps without a TV entry point.")
                        } else {
                            LazyRow(
                                state = homeScroll,
                                modifier = Modifier.fillMaxWidth().height(
                                    (layout.tileWidth / preferences.appTileStyle.aspectRatio + 28f +
                                        if (preferences.showLabels) 12f + 24f * fontScale else 0f).dp
                                ).focusGroup(),
                                contentPadding = PaddingValues(horizontal = 9.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(18.dp),
                            ) {
                                itemsIndexed(shown, key = { _, app -> app.id }) { index, app ->
                                    AppTile(app, Modifier.width(layout.tileWidth.dp),
                                        restoreKey == app.id, restoreGeneration,
                                        requester = appRequesters.getOrPut("home:${app.id}") { FocusRequester() },
                                        onNavigate = { navigateApp(index, true, shown, it) },
                                        onFocus = { lastFocused = app.id; lastAppId = app.id }, onClick = { onLaunch(app) },
                                        onOptions = { selected = app })
                                }
                            }
                            if (pins.isEmpty() && !layout.compactHeight) ProtectedLabel("Pin favorites from an app’s options to shape your home row.",
                                modifier = Modifier.padding(start = 9.dp))
                        }
                    } else {
                        if (results.isEmpty()) EmptyState("No apps found", "Try a different name or include non-TV apps in Settings.")
                        LazyVerticalGrid(
                            state = appsScroll,
                            columns = GridCells.Fixed(layout.columns),
                            modifier = Modifier.weight(1f).focusGroup(),
                            contentPadding = PaddingValues(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(18.dp),
                            verticalArrangement = Arrangement.spacedBy(18.dp),
                        ) {
                            gridItemsIndexed(results, key = { _, app -> app.id }) { index, app ->
                                AppTile(app, Modifier.fillMaxWidth(), restoreKey == app.id, restoreGeneration,
                                    requester = appRequesters.getOrPut("apps:${app.id}") { FocusRequester() },
                                    onNavigate = { navigateApp(index, false, results, it) },
                                    onFocus = { lastFocused = app.id; lastAppId = app.id }, onClick = { onLaunch(app) },
                                    onOptions = { selected = app })
                            }
                        }
                    }
                        }
                    }
                }
                }
                // Settings is a full page over the wallpaper; only app menus dim Home.
                if (selected != null) {
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .42f)))
                }
                if (settings || panelProgress > 0f) {
                    SettingsPage(preferences, busy, panelProgress, settings, installedVersion, updateStatus, updateBusy,
                        availableVersion, onCheckUpdates, onInstallUpdate, onPreferences, ::closeSettings,
                        onWallpaper, onOpenGallery, onClearWallpaper, onExtractColors, onExport, onImport,
                        onSystemSettings, onHomeSettings)
                }
                (selected ?: cachedMenu)?.takeIf { selected != null || menuProgress > 0f }?.let { app ->
                    AppOptions(app, preferences, menuProgress, onLaunch = { onLaunch(app); closeMenu() },
                        onPin = { updatePins(app); closeMenu() },
                        onMove = { direction ->
                            val ids = preferences.favorites.toMutableList()
                            val index = ids.indexOf(app.id)
                            val next = (index + direction).coerceIn(0, (ids.size - 1).coerceAtLeast(0))
                            if (index >= 0 && next != index) {
                                ids.removeAt(index); ids.add(next, app.id)
                                onPreferences(preferences.copy(favorites = ids))
                            }
                        }, onClose = ::closeMenu)
                }
                (gallery ?: cachedGallery)?.takeIf { gallery != null || galleryProgress > 0f }?.let { images ->
                    WallpaperGallery(images, galleryProgress, onSelectWallpaper, onDismissGallery)
                }
            }
        }
    }
}

@Composable
private fun TopNavigation(
    tab: String, compact: Boolean, focus: List<FocusRequester>, restoreKey: String?, generation: Int,
    onFocus: (String) -> Unit, onHome: () -> Unit, onApps: () -> Unit,
    onSearch: () -> Unit, onSettings: () -> Unit, onDown: () -> Unit,
) {
    val p = LocalPreferences.current
    val c = LocalShellColors.current
    val density = LocalDensity.current
    var homeWidth by remember { mutableIntStateOf(90) }
    var appsWidth by remember { mutableIntStateOf(100) }
    val tabGap = with(density) { 12.dp.toPx() }
    val pillX by animateFloatAsState(if (tab == "home") 0f else homeWidth + tabGap, p.movement(), label = "tab position")
    val pillWidth by animateFloatAsState(if (tab == "home") homeWidth.toFloat() else appsWidth.toFloat(),
        p.movement(), label = "tab width")
    fun navigationModifier(index: Int): Modifier = Modifier.focusRequester(focus[index]).focusProperties {
        left = if (index > 0) focus[index - 1] else FocusRequester.Cancel
        right = if (index < 3) focus[index + 1] else FocusRequester.Cancel
        up = FocusRequester.Cancel
    }
    val navigationContent: @Composable BoxScope.() -> Unit = {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box {
                Box(Modifier.matchParentSize()) {
                    Box(Modifier.align(if (p.navigationStyle == NavigationStyle.Minimal) Alignment.BottomStart else Alignment.TopStart)
                        .offset { IntOffset(pillX.roundToInt(), 0) }
                        .width(with(density) { pillWidth.coerceAtLeast(1f).toDp() })
                        .then(if (p.navigationStyle == NavigationStyle.Minimal) Modifier.height(3.dp) else Modifier.fillMaxHeight())
                        .clip(RoundedCornerShape(50)).background(c.accent.copy(alpha = .22f))
                        .border(1.dp, c.accent.copy(alpha = .30f), RoundedCornerShape(50)))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ShellButton("Home", navigationModifier(0).onSizeChanged { homeWidth = it.width },
                        restore = restoreKey == "nav:home", generation = generation,
                        onFocus = { onFocus("nav:home"); onHome() }, onClick = onHome)
                    ShellButton("All apps", navigationModifier(1).onSizeChanged { appsWidth = it.width },
                        restore = restoreKey == "nav:apps", generation = generation,
                        onFocus = { onFocus("nav:apps"); onApps() }, onClick = onApps)
                }
            }
            ShellButton("Search", navigationModifier(2), restore = restoreKey == "nav:search", generation = generation,
                onFocus = { onFocus("nav:search") }, onClick = onSearch)
            ShellButton("Settings", navigationModifier(3), restore = restoreKey == "nav:settings", generation = generation,
                onFocus = { onFocus("nav:settings") }, onClick = onSettings)
            if (p.showClock && compact) Clock(p.clock24, 16f, compact = true)
        }
    }
    Box(Modifier.fillMaxWidth(), contentAlignment = if (p.navigationStyle == NavigationStyle.Dock)
        Alignment.TopCenter else Alignment.TopStart) {
        val frame = Modifier.then(if (p.navigationStyle == NavigationStyle.Dock) Modifier.widthIn(max = 720.dp) else Modifier)
            .fillMaxWidth().onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionDown) { onDown(); true } else false
            }
        if (p.navigationStyle == NavigationStyle.Minimal) Box(frame, content = navigationContent)
        else GlassFrame(frame, content = navigationContent)
    }

}

@Composable
private fun Wallpaper(bitmap: Bitmap?) {
    val c = LocalShellColors.current
    val p = LocalPreferences.current
    Crossfade(targetState = bitmap, animationSpec = tween(p.duration(400), easing = SoftEase), label = "wallpaper dissolve") { image ->
        if (image != null) {
            Image(image.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize())
        } else {
            Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(
                c.background, lerp(c.background, c.accent, .30f), lerp(c.background, c.accent, .08f),
            ))))
            Box(Modifier.fillMaxSize().background(Brush.radialGradient(
                listOf(c.accent.copy(alpha = .16f), Color.Transparent), radius = 1100f,
            )))
        }
    }
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = p.wallpaperDim / 100f)))
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(
        Color.Transparent, c.background.copy(alpha = .10f), c.background.copy(alpha = .70f),
    ))))
}

@Composable
private fun Clock(use24: Boolean, clockSize: Float, modifier: Modifier = Modifier, compact: Boolean = false) {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) { while (true) { now = LocalDateTime.now(); delay(60_000L - System.currentTimeMillis() % 60_000L) } }
    val c = LocalShellColors.current
    val p = LocalPreferences.current
    val alignment = when (p.clockPlacement) {
        ClockPlacement.Left -> Alignment.Start
        ClockPlacement.Center -> Alignment.CenterHorizontally
        ClockPlacement.Right -> Alignment.End
    }
    val protection = remember(c.text, c.surface) { protectText(c.text.toArgb(), c.surface.toArgb()) }
    Column(modifier.then(if (!compact) Modifier.fillMaxWidth() else Modifier),
        horizontalAlignment = alignment, verticalArrangement = Arrangement.Center) {
        Column(Modifier.clip(RoundedCornerShape(if (compact) 12.dp else 22.dp))
            .background(Color(protection.backing).copy(alpha = protection.opacity))
            .padding(horizontal = if (compact) 10.dp else 16.dp, vertical = if (compact) 5.dp else 10.dp)) {
            Text(now.format(DateTimeFormatter.ofPattern(if (use24) "HH:mm" else "h:mm a")),
                color = Color(protection.foreground), style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = clockSize.sp, lineHeight = (clockSize * 1.15f).sp,
                    fontWeight = FontWeight.SemiBold, letterSpacing = (-1).sp))
            if (!compact && p.showDate) {
                Text(now.format(DateTimeFormatter.ofPattern("EEEE, MMMM d")),
                    color = Color(protection.foreground), fontSize = 17.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun ShellButton(
    text: String, modifier: Modifier = Modifier, selected: Boolean = false,
    restore: Boolean = false, generation: Int = 0, onFocus: () -> Unit = {}, onClick: () -> Unit,
) {
    val c = LocalShellColors.current
    val p = LocalPreferences.current
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val requester = remember { FocusRequester() }
    LaunchedEffect(generation) { if (restore) { delay(50); requester.requestFocus() } }
    LaunchedEffect(focused) { if (focused) onFocus() }
    val fill by animateColorAsState(
        if (focused) c.accent else if (selected) c.elevated else Color.Transparent,
        tween(p.duration(160), easing = SoftEase), label = "button color",
    )
    val scale by animateFloatAsState(if (focused) 1.055f else 1f, p.movement(), label = "button lift")
    Box(
        modifier.focusRequester(requester).graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(50)).background(fill)
            .combinedClickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = if (focused) c.onAccent else c.text, fontSize = 14.sp,
            fontWeight = if (focused || selected) FontWeight.SemiBold else FontWeight.Medium)
    }
}

@Composable
private fun AppTile(
    app: LauncherApp, modifier: Modifier, restore: Boolean, generation: Int,
    requester: FocusRequester, onNavigate: (Key) -> Boolean,
    onFocus: () -> Unit, onClick: () -> Unit, onOptions: () -> Unit,
) {
    val p = LocalPreferences.current
    val c = LocalShellColors.current
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    LaunchedEffect(generation) { if (restore) { delay(60); requester.requestFocus() } }
    LaunchedEffect(focused) { if (focused) onFocus() }
    val scale by animateFloatAsState(if (focused) 1.065f else 1f, p.movement(), label = "tile scale")
    val lift by animateFloatAsState(if (focused) -3f else 0f, p.movement(), label = "tile lift")
    val fill by animateColorAsState(if (focused) c.elevated else c.surface,
        tween(p.duration(240), easing = SoftEase), label = "tile surface")
    val shape = RoundedCornerShape(p.roundness.dp)
    Column(
        modifier.focusRequester(requester).graphicsLayer {
            scaleX = scale; scaleY = scale; translationY = lift.dp.toPx()
        }.onPreviewKeyEvent {
            when {
                it.type == KeyEventType.KeyDown && onNavigate(it.key) -> true
                it.key == Key.Menu && it.type == KeyEventType.KeyUp -> { onOptions(); true }
                else -> false
            }
        }.combinedClickable(interactionSource = interaction, indication = null,
            onClick = onClick, onLongClick = onOptions),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val tileShape = if (p.appTileStyle == AppTileStyle.Circle) RoundedCornerShape(50) else shape
        GlassFrame(Modifier.fillMaxWidth().aspectRatio(p.appTileStyle.aspectRatio).clip(tileShape)
            .border(BorderStroke(if (focused) 2.dp else 1.dp,
                if (focused) c.accent else c.text.copy(alpha = .10f)), tileShape)) {
            if (p.appTileStyle == AppTileStyle.Banner && app.banner != null) {
                Image(app.banner.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize())
            } else {
                BoxWithConstraints(Modifier.fillMaxSize().background(fill.copy(alpha = .22f)), contentAlignment = Alignment.Center) {
                    Image(app.icon.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Fit,
                        modifier = Modifier.size((minOf(maxHeight, maxWidth) * (p.iconScale / 100f)).coerceAtMost(144.dp)))
                }
            }
        }
        if (p.showLabels) {
            Spacer(Modifier.height(8.dp))
            ProtectedLabel(app.label, fontSize = 14)
        }
    }
}

@Composable
private fun EmptyState(title: String, subtitle: String) {
    val c = LocalShellColors.current
    Column(Modifier.fillMaxWidth().padding(vertical = 24.dp)) {
        Text(title, color = c.text, fontSize = 19.sp)
        Spacer(Modifier.height(6.dp))
        Text(subtitle, color = c.muted, fontSize = 14.sp)
    }
}

@Composable
private fun AppOptions(
    app: LauncherApp, preferences: Preferences, progress: Float, onLaunch: () -> Unit,
    onPin: () -> Unit, onMove: (Int) -> Unit, onClose: () -> Unit,
) {
    val c = LocalShellColors.current
    val first = remember { FocusRequester() }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // Dialog creates its own focus root, keeping the home row out of remote navigation.
        androidx.compose.ui.window.Dialog(onDismissRequest = onClose) {
            LaunchedEffect(Unit) { delay(60); first.requestFocus() }
            GlassFrame(Modifier.width(340.dp).graphicsLayer {
                alpha = progress.coerceIn(0f, 1f); scaleX = .94f + .06f * progress; scaleY = .94f + .06f * progress
            }) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(app.label, color = c.text, fontSize = 23.sp, fontWeight = FontWeight.SemiBold)
                if (!app.tvApp) Text("This app may require a mouse or touch input.", color = c.muted, fontSize = 13.sp)
                ShellButton("Open app", Modifier.fillMaxWidth().focusRequester(first), onClick = onLaunch)
                ShellButton(if (app.id in preferences.favorites) "Unpin from home" else "Pin to home",
                    Modifier.fillMaxWidth(), onClick = onPin)
                if (app.id in preferences.favorites && preferences.favorites.size > 1) {
                    ShellButton("Move left", Modifier.fillMaxWidth()) { onMove(-1) }
                    ShellButton("Move right", Modifier.fillMaxWidth()) { onMove(1) }
                }
                ShellButton("Done", Modifier.fillMaxWidth(), onClick = onClose)
            }
            }
        }
    }
}

@Composable
private fun <T> ChoiceSetting(title: String, choices: List<T>, selected: T, label: (T) -> String, change: (T) -> Unit) {
    val index = choices.indexOf(selected).coerceAtLeast(0)
    StepSetting(title, label(selected),
        { change(choices[(index + choices.size - 1) % choices.size]) },
        { change(choices[(index + 1) % choices.size]) })
}

@Composable
private fun StylePreview(style: HomeStyle, current: Preferences, apply: () -> Unit) {
    val c = LocalShellColors.current
    val preview = style.applyTo(current)
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
        .background(c.surface.copy(alpha = .65f)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Canvas(Modifier.size(width = 112.dp, height = 72.dp).clip(RoundedCornerShape(10.dp))
            .background(Brush.linearGradient(listOf(c.background, c.accent.copy(alpha = .35f))))) {
            val barWidth = size.width * if (preview.navigationStyle == NavigationStyle.Dock) .62f else .88f
            val barLeft = (size.width - barWidth) / 2
            drawRoundRect(c.text.copy(alpha = .25f), Offset(barLeft, size.height * .12f),
                androidx.compose.ui.geometry.Size(barWidth, if (preview.navigationStyle == NavigationStyle.Minimal) 2f else size.height * .12f),
                androidx.compose.ui.geometry.CornerRadius(4f))
            if (preview.showClock) {
                val x = when (preview.clockPlacement) {
                    ClockPlacement.Left -> size.width * .08f
                    ClockPlacement.Center -> size.width * .38f
                    ClockPlacement.Right -> size.width * .68f
                }
                drawRoundRect(c.text.copy(alpha = .8f), Offset(x, size.height * .37f),
                    androidx.compose.ui.geometry.Size(size.width * .23f, size.height * .09f),
                    androidx.compose.ui.geometry.CornerRadius(3f))
            }
            repeat(4) { index ->
                val tileWidth = size.width * .18f
                val tileHeight = tileWidth / preview.appTileStyle.aspectRatio
                val origin = Offset(size.width * (.08f + index * .22f), size.height * .68f)
                if (preview.appTileStyle == AppTileStyle.Circle) drawCircle(c.accent,
                    tileWidth / 2, origin + Offset(tileWidth / 2, tileHeight / 2))
                else drawRoundRect(c.accent, origin, androidx.compose.ui.geometry.Size(tileWidth, tileHeight),
                    androidx.compose.ui.geometry.CornerRadius(4f))
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ShellButton(style.label, selected = preview == current, onClick = apply)
            Text(style.description, color = c.muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun SettingsPage(
    p: Preferences, busy: Boolean, progress: Float, active: Boolean,
    installedVersion: String, updateStatus: String, updateBusy: Boolean, availableVersion: String?,
    onCheckUpdates: () -> Unit, onInstallUpdate: () -> Unit,
    change: (Preferences) -> Unit, close: () -> Unit,
    wallpaper: () -> Unit, openGallery: () -> Unit, clearWallpaper: () -> Unit, extract: () -> Unit,
    export: () -> Unit, import: () -> Unit, system: () -> Unit, home: () -> Unit,
) {
    val c = LocalShellColors.current
    val first = remember { FocusRequester() }
    var category by remember { mutableStateOf("Styles") }
    val categories = listOf("Styles", "Wallpaper", "Appearance", "Glass", "Layout", "Motion", "System")
    val railFocus = remember { List(categories.size) { FocusRequester() } }
    val contentFocus = remember { FocusRequester() }
    val descriptions = mapOf(
        "Styles" to "Start with a look. Make every detail your own.",
        "Wallpaper" to "Your artwork sets the mood. The colors follow.",
        "Appearance" to "A palette that feels like you.",
        "Glass" to "Soft layers, light edges, and space for your wallpaper.",
        "Layout" to "Shape your home for your screen and your sofa.",
        "Motion" to "Give every interaction a little room to breathe.",
        "System" to "Your apps, shared themes, and device settings.",
    )
    Box(Modifier.fillMaxSize().focusProperties { canFocus = active }) {
        LaunchedEffect(active) { if (active) { delay(80); railFocus[0].requestFocus() } }
        BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 20.dp)
            .graphicsLayer {
                alpha = progress.coerceIn(0f, 1f)
                translationY = (1f - progress) * 30.dp.toPx()
            }) {
            val narrow = maxWidth < 720.dp
            fun railModifier(index: Int) = Modifier.focusRequester(railFocus[index]).focusProperties {
                if (narrow) {
                    left = if (index > 0) railFocus[index - 1] else FocusRequester.Cancel
                    right = if (index < categories.lastIndex) railFocus[index + 1] else FocusRequester.Cancel
                    down = contentFocus
                    up = first
                } else {
                    up = if (index > 0) railFocus[index - 1] else FocusRequester.Cancel
                    down = if (index < categories.lastIndex) railFocus[index + 1] else FocusRequester.Cancel
                    right = contentFocus
                    left = FocusRequester.Cancel
                }
            }
            Box(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxSize().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (!narrow) {
                        GlassFrame(Modifier.width(190.dp).fillMaxHeight()) {
                            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Settings", color = c.text, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                                Text("Your TV. Your way.", color = c.muted, fontSize = 12.sp)
                                SectionTitle("Personalize")
                                categories.forEachIndexed { index, name ->
                                    ShellButton(name, Modifier.fillMaxWidth().then(railModifier(index)), selected = category == name,
                                        onFocus = { category = name }) { category = name }
                                }
                                Spacer(Modifier.height(18.dp))
                                Text("Ayame TV · Open source", color = c.muted, fontSize = 11.sp)
                            }
                        }
                    }
                    GlassFrame(Modifier.weight(1f).fillMaxHeight()) {
                    Column(Modifier.fillMaxSize().padding(22.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(category, color = c.text, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
                                Text(descriptions[category].orEmpty(), color = c.muted, fontSize = 12.sp)
                            }
                            ShellButton("Back to Home", Modifier.focusRequester(first).focusProperties {
                                left = railFocus[categories.indexOf(category)]
                                down = contentFocus
                                up = FocusRequester.Cancel
                                right = FocusRequester.Cancel
                            }, onClick = close)
                        }
                        if (narrow) {
                            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 12.dp)) {
                                categories.forEachIndexed { index, name ->
                                    ShellButton(name, railModifier(index), selected = category == name,
                                        onFocus = { category = name }) { category = name }
                                }
                            }
                        }
                        if (busy) Text("Working…", color = c.accent, fontSize = 13.sp)
                        Spacer(Modifier.height(16.dp))
                        CompositionLocalProvider(LocalSettingsRail provides railFocus[categories.indexOf(category)]) {
                        AnimatedContent(category, modifier = Modifier.weight(1f).focusRequester(contentFocus).focusGroup(), transitionSpec = {
                            (fadeIn(tween(p.duration(180))) + slideInVertically(tween(p.duration(220), easing = SoftEase)) { it / 18 }) togetherWith
                                fadeOut(tween(p.duration(90)))
                        }, label = "settings category") { page ->
                            Column(Modifier.fillMaxSize().focusProperties { canFocus = page == category }
                                .verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                when (page) {
                                    "Styles" -> {
                                        SectionTitle("Home presets")
                                        Text("Presets keep your wallpaper, colors, favorites, and motion preferences. Every detail below can be changed independently.",
                                            color = c.muted, fontSize = 13.sp)
                                        HomeStyle.entries.forEach { style ->
                                            StylePreview(style, p) { change(style.applyTo(p)) }
                                        }
                                        SectionTitle("Make it yours")
                                        ChoiceSetting("Top bar", NavigationStyle.entries, p.navigationStyle, { it.label }) {
                                            change(p.copy(navigationStyle = it))
                                        }
                                        ChoiceSetting("App tile design", AppTileStyle.entries, p.appTileStyle, { it.label }) {
                                            change(p.copy(appTileStyle = it))
                                        }
                                        StepSetting("Icon size", "${p.iconScale}%",
                                            { change(p.copy(iconScale = (p.iconScale - 5).coerceAtLeast(45))) },
                                            { change(p.copy(iconScale = (p.iconScale + 5).coerceAtMost(90))) })
                                        ChoiceSetting("Clock position", ClockPlacement.entries, p.clockPlacement, { it.label }) {
                                            change(p.copy(clockPlacement = it))
                                        }
                                        ToggleSetting("Show date", p.showDate) { change(p.copy(showDate = !p.showDate)) }
                                    }
                                    "Wallpaper" -> {
                                        SectionTitle("Your collection")
                                        SettingAction("Wallpaper gallery", "Preview and apply your local wallpapers", openGallery)
                                        SettingAction("Import an image", "Browse images with your file manager", wallpaper)
                                        SettingAction("Generated background", "Use a quiet gradient instead of artwork", clearWallpaper)
                                        SectionTitle("Atmosphere")
                                        StepSetting("Wallpaper dim", "${p.wallpaperDim}%",
                                            { change(p.copy(wallpaperDim = (p.wallpaperDim - 5).coerceAtLeast(0))) },
                                            { change(p.copy(wallpaperDim = (p.wallpaperDim + 5).coerceAtMost(80))) })
                                    }
                                    "Appearance" -> {
                                        SectionTitle("Color system")
                                        ToggleSetting("Light appearance", p.light) { change(p.copy(light = !p.light)) }
                                        ToggleSetting("Follow wallpaper colors", p.automaticColors) {
                                            if (p.automaticColors) change(p.copy(automaticColors = false)) else extract()
                                        }
                                        SectionTitle("Accent palette")
                                        val presets = listOf("Iris" to 0xFFB6A0EF, "Sakura" to 0xFFEAA9C1, "Ocean" to 0xFF8BC9E8,
                                            "Moss" to 0xFFA7CCA0, "Honey" to 0xFFE8BE7C)
                                        val presetFocus = remember { List(presets.size) { FocusRequester() } }
                                        Row(Modifier.fillMaxWidth().focusGroup().horizontalScroll(rememberScrollState())) {
                                            presets.forEachIndexed { index, (name, seed) ->
                                                ShellButton(name, Modifier.focusRequester(presetFocus[index]).focusProperties {
                                                    left = if (index > 0) presetFocus[index - 1] else railFocus[categories.indexOf("Appearance")]
                                                    right = if (index < presets.lastIndex) presetFocus[index + 1] else FocusRequester.Cancel
                                                }, selected = !p.automaticColors && p.seed == seed.toInt()) {
                                                    change(p.copy(seed = seed.toInt(), automaticColors = false))
                                                }
                                            }
                                        }
                                        val hue = FloatArray(3).also { ColorUtils.colorToHSL(p.seed, it) }[0].toInt()
                                        StepSetting("Accent hue", "$hue°",
                                            { change(p.copy(seed = shiftHue(p.seed, -15f), automaticColors = false)) },
                                            { change(p.copy(seed = shiftHue(p.seed, 15f), automaticColors = false)) })
                                    }
                                    "Glass" -> {
                                        SectionTitle("Surface treatment")
                                        ToggleSetting("Glass surfaces", p.glassEnabled) { change(p.copy(glassEnabled = !p.glassEnabled)) }
                                        ToggleSetting("Frosted wallpaper", p.glassBlur) { change(p.copy(glassBlur = !p.glassBlur)) }
                                        StepSetting("Surface tint", "${p.surfaceOpacity}%",
                                            { change(p.copy(surfaceOpacity = (p.surfaceOpacity - 5).coerceAtLeast(50))) },
                                            { change(p.copy(surfaceOpacity = (p.surfaceOpacity + 5).coerceAtMost(100))) })
                                        Text("Glass reveals a softened layer of your wallpaper. Turn frost off for a lighter effect; turn glass off for solid-style surfaces.",
                                            color = c.muted, fontSize = 13.sp, modifier = Modifier.padding(12.dp))
                                    }
                                    "Layout" -> {
                                        SectionTitle("Shape & density")
                                        StepSetting("App columns", p.columns.toString(),
                                            { change(p.copy(columns = (p.columns - 1).coerceAtLeast(3))) },
                                            { change(p.copy(columns = (p.columns + 1).coerceAtMost(7))) })
                                        StepSetting("Corner radius", "${p.roundness}",
                                            { change(p.copy(roundness = (p.roundness - 4).coerceAtLeast(0))) },
                                            { change(p.copy(roundness = (p.roundness + 4).coerceAtMost(36))) })
                                        SectionTitle("Home elements")
                                        ToggleSetting("App names", p.showLabels) { change(p.copy(showLabels = !p.showLabels)) }
                                        ToggleSetting("Home clock", p.showClock) { change(p.copy(showClock = !p.showClock)) }
                                        ToggleSetting("24-hour time", p.clock24) { change(p.copy(clock24 = !p.clock24)) }
                                    }
                                    "Motion" -> {
                                        SectionTitle("Movement")
                                        ToggleSetting("Animations", p.animations) { change(p.copy(animations = !p.animations)) }
                                        ToggleSetting("Gentle bounce", p.bouncyAnimations) { change(p.copy(bouncyAnimations = !p.bouncyAnimations)) }
                                        StepSetting("Motion duration", "${p.motionScale}%",
                                            { change(p.copy(motionScale = (p.motionScale - 25).coerceAtLeast(50))) },
                                            { change(p.copy(motionScale = (p.motionScale + 25).coerceAtMost(200))) })
                                        Text("Higher duration means slower movement. Switching animations off also stops page transitions, focus effects, and wallpaper dissolves.",
                                            color = c.muted, fontSize = 13.sp, modifier = Modifier.padding(12.dp))
                                    }
                                    "System" -> {
                                        SectionTitle("Ayame TV updates")
                                        SettingAction(if (updateBusy) "Working…" else "Check for updates",
                                            "Installed · $installedVersion", onCheckUpdates)
                                        Text(updateStatus, color = c.text, fontSize = 13.sp, modifier = Modifier.padding(12.dp))
                                        if (availableVersion != null && !updateBusy) {
                                            SettingAction("Download & install $availableVersion", "Android will ask you to confirm installation.", onInstallUpdate)
                                        }
                                        SectionTitle("Apps & device")
                                        ToggleSetting("Include non-TV apps", p.showMobileApps) { change(p.copy(showMobileApps = !p.showMobileApps)) }
                                        SettingAction("Android settings", "Wi-Fi, Bluetooth, display, and sound", system)
                                        SettingAction("Use Ayame as Home", "Choose Ayame for the remote's Home button", home)
                                        SectionTitle("Share your rice")
                                        SettingAction("Export theme", "Save your appearance, layout, and favorite order", export)
                                        SettingAction("Import theme", "Apply an Ayame TV JSON theme", import)
                                    }
                                }
                            }
                        }
                        }
                    }
                    }
                }
            }
        }
    }
}

@Composable
private fun WallpaperGallery(
    images: List<LocalWallpaper>, progress: Float, select: (LocalWallpaper) -> Unit, close: () -> Unit,
) {
    val c = LocalShellColors.current
    val first = remember { FocusRequester() }
    androidx.compose.ui.window.Dialog(onDismissRequest = close,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
        LaunchedEffect(Unit) { delay(80); first.requestFocus() }
        GlassFrame(Modifier.fillMaxWidth(.85f).fillMaxHeight(.85f).graphicsLayer {
            alpha = progress.coerceIn(0f, 1f); scaleX = .95f + .05f * progress; scaleY = .95f + .05f * progress
        }) {
        Column(Modifier.fillMaxSize().padding(22.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Your wallpapers", color = c.text, fontSize = 24.sp, modifier = Modifier.weight(1f))
                ShellButton("Done", Modifier.focusRequester(first), onClick = close)
            }
            Text("Select an image to apply its wallpaper and colors.", color = c.muted, fontSize = 13.sp)
            Spacer(Modifier.height(16.dp))
            if (images.isEmpty()) {
                EmptyState("No local wallpapers yet", "Run scripts/setup-tv-wallpapers.sh, then reopen this gallery.")
            }
            LazyVerticalGrid(columns = GridCells.Adaptive(180.dp), modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(8.dp)) {
                items(images, key = { it.path }) { image ->
                    val interaction = remember { MutableInteractionSource() }
                    val focused by interaction.collectIsFocusedAsState()
                    val p = LocalPreferences.current
                    val scale by animateFloatAsState(if (focused) 1.035f else 1f,
                        p.movement(), label = "wallpaper focus")
                    Column(Modifier.graphicsLayer { scaleX = scale; scaleY = scale }
                        .clip(RoundedCornerShape(16.dp)).background(c.elevated)
                        .border(if (focused) 2.dp else 0.dp, if (focused) c.accent else Color.Transparent, RoundedCornerShape(16.dp))
                        .combinedClickable(interactionSource = interaction, indication = null, onClick = { select(image) })) {
                        Image(image.thumbnail.asImageBitmap(), contentDescription = image.label,
                            contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f))
                        Text(image.label, color = c.text, fontSize = 13.sp, maxLines = 1,
                            overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(10.dp))
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, color = LocalShellColors.current.accent, fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 14.dp, bottom = 2.dp))
}

@Composable
private fun SettingRow(title: String, subtitle: String?, action: () -> Unit, trailing: @Composable () -> Unit) {
    val c = LocalShellColors.current
    val p = LocalPreferences.current
    val rail = LocalSettingsRail.current
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (focused) 1.012f else 1f, p.movement(), label = "setting focus")
    val fill by animateColorAsState(if (focused) c.accent.copy(alpha = .22f) else c.elevated.copy(alpha = .45f),
        tween(p.duration(140)), label = "setting tint")
    Row(Modifier.fillMaxWidth().focusProperties {
        left = rail ?: FocusRequester.Default
        right = FocusRequester.Cancel
    }.graphicsLayer { scaleX = scale; scaleY = scale }
        .clip(RoundedCornerShape(18.dp)).background(fill)
        .border(1.dp, if (focused) c.accent.copy(alpha = .65f) else Color.Transparent, RoundedCornerShape(18.dp))
        .combinedClickable(interactionSource = interaction, indication = null, onClick = action)
        .padding(horizontal = 18.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = c.text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            if (subtitle != null) Text(subtitle, color = c.muted, fontSize = 12.sp)
        }
        trailing()
    }
}

@Composable
private fun SettingAction(title: String, subtitle: String, action: () -> Unit) {
    SettingRow(title, subtitle, action) {
        Text("›", color = LocalShellColors.current.muted, fontSize = 26.sp)
    }
}

@Composable
private fun ToggleSetting(title: String, enabled: Boolean, action: () -> Unit) {
    val c = LocalShellColors.current
    val p = LocalPreferences.current
    val knob by animateFloatAsState(if (enabled) 18f else 0f, p.movement(), label = "switch knob")
    val track by animateColorAsState(if (enabled) c.accent else c.text.copy(alpha = .20f),
        tween(p.duration(160)), label = "switch tint")
    SettingRow(title, if (enabled) "On" else "Off", action) {
        Box(Modifier.width(42.dp).height(24.dp).clip(RoundedCornerShape(50)).background(track).padding(4.dp)) {
            Box(Modifier.offset { IntOffset(knob.dp.roundToPx(), 0) }.size(16.dp)
                .clip(RoundedCornerShape(50)).background(if (enabled) c.onAccent else c.text))
        }
    }
}

@Composable
private fun StepSetting(title: String, value: String, less: () -> Unit, more: () -> Unit) {
    val c = LocalShellColors.current
    val rail = LocalSettingsRail.current
    val decrease = remember { FocusRequester() }
    val increase = remember { FocusRequester() }
    Row(Modifier.fillMaxWidth().focusGroup().clip(RoundedCornerShape(18.dp)).background(c.elevated.copy(alpha = .45f))
        .padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, color = c.text, fontSize = 14.sp, modifier = Modifier.weight(1f))
        ShellButton("−", Modifier.focusRequester(decrease).focusProperties {
            left = rail ?: FocusRequester.Default
            right = increase
        }, onClick = less)
        Text(value, color = c.accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.widthIn(min = 42.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        ShellButton("+", Modifier.focusRequester(increase).focusProperties {
            left = decrease
            right = FocusRequester.Cancel
        }, onClick = more)
    }
}
