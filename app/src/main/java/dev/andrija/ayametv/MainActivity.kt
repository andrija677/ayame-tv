package dev.andrija.ayametv

import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ClipData
import android.graphics.Bitmap
import android.app.role.RoleManager
import android.os.Build
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private val homeRoleRequest = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) message("Ayame TV is now your default Home app.")
        else message("Home selection was not changed. Some TVs require setup through ADB.")
    }
    private lateinit var store: LauncherStore
    private var preferences by mutableStateOf(Preferences())
    private var apps by mutableStateOf<List<LauncherApp>>(emptyList())
    private var wallpaperBitmap by mutableStateOf<Bitmap?>(null)
    private var loaded by mutableStateOf(false)
    private var busy by mutableStateOf(false)
    private var gallery by mutableStateOf<List<LocalWallpaper>?>(null)
    private var homeRequest by mutableStateOf(0)
    private var appJob: Job? = null
    private lateinit var updater: ReleaseUpdater
    private var updateStatus by mutableStateOf("Check GitHub releases when you choose.")
    private var updateBusy by mutableStateOf(false)
    private var availableUpdate by mutableStateOf<AvailableUpdate?>(null)
    private var installedVersion by mutableStateOf("")
    private var pendingInstall: java.io.File? = null

    private val installPermission = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (packageManager.canRequestPackageInstalls()) pendingInstall?.let { file -> launchInstaller(file) }
        else updateStatus = "Installation permission was not granted. You can try again."
    }

    private val pickWallpaper = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) task {
            val image = withContext(Dispatchers.IO) { store.importWallpaper(uri) }
            wallpaperBitmap = image
            if (preferences.automaticColors) {
                val seed = withContext(Dispatchers.Default) { store.wallpaperSeed(image) }
                update(preferences.copy(seed = seed))
            }
        }
    }
    private val exportTheme = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) task {
            val value = preferences
            withContext(Dispatchers.IO) { store.exportTheme(uri, value) }
            message("Theme saved. Share it with another Ayame TV user.")
        }
    }
    private val importTheme = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) task {
            var imported = withContext(Dispatchers.IO) { store.importTheme(uri) }
            val image = wallpaperBitmap
            if (imported.automaticColors && image != null) {
                imported = imported.copy(seed = withContext(Dispatchers.Default) { store.wallpaperSeed(image) })
            }
            update(imported)
            message("Theme applied")
        }
    }
    private val packageChanges = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) { refreshApps() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = LauncherStore(this)
        updater = ReleaseUpdater(this)
        installedVersion = updater.installedVersion().second
        savedInstanceState?.getString("pendingUpdate")?.let { path ->
            val candidate = java.io.File(path)
            val directory = java.io.File(cacheDir, "updates").canonicalFile
            if (candidate.parentFile?.canonicalFile == directory && candidate.isFile) pendingInstall = candidate
        }
        preferences = store.load()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(this, packageChanges, filter, ContextCompat.RECEIVER_EXPORTED)
        lifecycleScope.launch {
            wallpaperBitmap = withContext(Dispatchers.IO) { store.wallpaper() }
            loaded = true
        }
        setContent {
            AyameLauncher(
                preferences = preferences, apps = apps, wallpaper = wallpaperBitmap,
                loaded = loaded, busy = busy, homeRequest = homeRequest,
                installedVersion = installedVersion, updateStatus = updateStatus, updateBusy = updateBusy,
                availableVersion = availableUpdate?.manifest?.versionName,
                onCheckUpdates = ::checkUpdates, onInstallUpdate = ::downloadUpdate,
                gallery = gallery,
                onOpenGallery = { task { gallery = withContext(Dispatchers.IO) { store.gallery() } } },
                onDismissGallery = { gallery = null },
                onSelectWallpaper = { item ->
                    task {
                        val image = withContext(Dispatchers.IO) { store.importWallpaper(Uri.fromFile(java.io.File(item.path))) }
                        val seed = withContext(Dispatchers.Default) { store.wallpaperSeed(image) }
                        wallpaperBitmap = image
                        update(preferences.copy(seed = seed, automaticColors = true))
                        gallery = null
                    }
                },
                onPreferences = ::update, onLaunch = ::launchApp,
                onWallpaper = { safePicker { pickWallpaper.launch(arrayOf("image/*")) } },
                onClearWallpaper = {
                    task {
                        withContext(Dispatchers.IO) { store.wallpaperFile.delete() }
                        wallpaperBitmap = null
                    }
                },
                onExtractColors = {
                    val image = wallpaperBitmap
                    if (image == null) update(preferences.copy(automaticColors = true))
                    else task {
                        val seed = withContext(Dispatchers.Default) { store.wallpaperSeed(image) }
                        update(preferences.copy(seed = seed, automaticColors = true))
                    }
                },
                onExport = { safePicker { exportTheme.launch("ayame-tv-theme.json") } },
                onImport = { safePicker { importTheme.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) } },
                onSystemSettings = { safeStart(Intent(Settings.ACTION_SETTINGS)) },
                onHomeSettings = ::chooseHomeApp,
            )
        }
    }

    private fun chooseHomeApp() {
        if (Build.VERSION.SDK_INT >= 29) {
            val roles = getSystemService(RoleManager::class.java)
            if (roles != null && roles.isRoleAvailable(RoleManager.ROLE_HOME)) {
                if (roles.isRoleHeld(RoleManager.ROLE_HOME)) {
                    message("Ayame TV is already your default Home app.")
                    return
                }
                try {
                    homeRoleRequest.launch(roles.createRequestRoleIntent(RoleManager.ROLE_HOME))
                    return
                } catch (_: ActivityNotFoundException) {
                    // Some TV firmware exposes the role without a working picker.
                }
            }
        }
        try {
            startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
        } catch (_: ActivityNotFoundException) {
            message("This TV hides Home selection. Use the Ayame TV ADB setup from your computer.")
        }
    }

    override fun onResume() {
        super.onResume()
        if (::store.isInitialized) refreshApps()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) homeRequest++
    }

    private fun refreshApps() {
        appJob?.cancel()
        appJob = lifecycleScope.launch {
            apps = withContext(Dispatchers.IO) { store.apps() }
        }
    }

    private fun checkUpdates() {
        if (updateBusy) return
        lifecycleScope.launch {
            updateBusy = true
            updateStatus = "Checking GitHub releases…"
            try {
                val candidate = withContext(Dispatchers.IO) { updater.check() }
                val current = updater.installedVersion().first
                availableUpdate = candidate?.takeIf { it.manifest.versionCode > current }
                updateStatus = when {
                    candidate == null -> "No public releases yet. This will work after the first release is published."
                    availableUpdate == null -> "You're up to date · $installedVersion"
                    else -> "Version ${candidate.manifest.versionName} is available."
                }
            } catch (exception: kotlinx.coroutines.CancellationException) { throw exception }
            catch (exception: Exception) {
                availableUpdate = null
                updateStatus = "Could not check updates: ${exception.message ?: "check your connection"}"
            } finally { updateBusy = false }
        }
    }

    private fun downloadUpdate() {
        if (updateBusy) return
        val release = availableUpdate ?: return
        lifecycleScope.launch {
            updateBusy = true
            try {
                val apk = withContext(Dispatchers.IO) {
                    updater.download(release) { percent -> runOnUiThread { updateStatus = "Downloading update… $percent%" } }
                }
                pendingInstall = apk
                if (packageManager.canRequestPackageInstalls()) launchInstaller(apk)
                else {
                    updateStatus = "Allow Ayame TV to install updates, then return here."
                    installPermission.launch(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName")))
                }
            } catch (exception: kotlinx.coroutines.CancellationException) { throw exception }
            catch (exception: Exception) { updateStatus = exception.message ?: "Could not install update" }
            finally { updateBusy = false }
        }
    }

    private fun launchInstaller(apk: java.io.File) {
        try {
            updater.verifyApk(apk)
            val uri = FileProvider.getUriForFile(this, "$packageName.updates", apk)
            startActivity(Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                clipData = ClipData.newRawUri("Ayame TV update", uri)
            })
            pendingInstall = null
            updateStatus = "Confirm the update in Android's installer."
        } catch (exception: Exception) { updateStatus = exception.message ?: "The installer is unavailable on this device" }
    }

    private fun update(value: Preferences) {
        preferences = value
        store.save(value)
    }

    private fun launchApp(app: LauncherApp) {
        val component = ComponentName.unflattenFromString(app.id) ?: return
        safeStart(Intent(Intent.ACTION_MAIN).apply {
            addCategory(if (app.tvApp) Intent.CATEGORY_LEANBACK_LAUNCHER else Intent.CATEGORY_LAUNCHER)
            setComponent(component)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        })
    }

    private fun safeStart(intent: Intent) {
        runCatching { startActivity(intent) }.onFailure { message("This action is unavailable on this device.") }
    }

    private fun safePicker(block: () -> Unit) {
        try { block() } catch (_: ActivityNotFoundException) {
            message("No file picker is installed. Install a TV file manager with document-picker support.")
        }
    }

    private fun task(block: suspend () -> Unit) {
        if (busy) return
        lifecycleScope.launch {
            busy = true
            try { block() } catch (e: Exception) { message(e.message ?: "Could not complete action") }
            finally { busy = false }
        }
    }

    private fun message(text: String) = Toast.makeText(this, text, Toast.LENGTH_LONG).show()

    override fun onDestroy() {
        unregisterReceiver(packageChanges)
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        pendingInstall?.let { outState.putString("pendingUpdate", it.path) }
        super.onSaveInstanceState(outState)
    }
}
