package dev.andrija.ayametv

import android.content.Context
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.provider.Settings

enum class DeviceSettingsTarget(val actions: List<String>) {
    Wifi(listOf(Settings.ACTION_WIFI_SETTINGS, Settings.ACTION_WIRELESS_SETTINGS, Settings.ACTION_SETTINGS)),
    Bluetooth(listOf(Settings.ACTION_BLUETOOTH_SETTINGS, Settings.ACTION_SETTINGS)),
    Sound(listOf(Settings.ACTION_SOUND_SETTINGS, Settings.ACTION_SETTINGS)),
    Android(listOf(Settings.ACTION_SETTINGS)),
}

data class DeviceQuickState(
    val network: String = "Checking connection…",
    val connectionDetail: String = "Open network settings to manage connections",
    val volume: Int = 0,
    val maxVolume: Int = 0,
    val fixedVolume: Boolean = true,
    val muted: Boolean = false,
) {
    val volumeLabel: String get() = when {
        fixedVolume || maxVolume <= 0 -> "Controlled by TV or audio device"
        muted -> "Muted"
        else -> "${(volume.coerceIn(0, maxVolume) * 100L / maxVolume)}%"
    }
}

/** Uses public settings intents and local device state, without radio toggles. */
class DeviceControls(context: Context) {
    private val connectivity = context.applicationContext.getSystemService(ConnectivityManager::class.java)
    private val audio = context.applicationContext.getSystemService(AudioManager::class.java)

    fun snapshot(): DeviceQuickState {
        val network = runCatching {
            val capabilities = connectivity?.activeNetwork?.let { connectivity.getNetworkCapabilities(it) }
            val label = when {
                capabilities == null -> "Not connected"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN connection"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet connected"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi connected"
                else -> "Network connected"
            }
            val detail = when {
                capabilities == null -> "Open network settings to connect"
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL) -> "Sign-in required"
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) -> "Internet available"
                else -> "Internet access not verified"
            }
            label to detail
        }.getOrDefault("Network status unavailable" to "Open network settings to check your connection")
        return runCatching {
            DeviceQuickState(network.first, network.second,
                volume = audio?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0,
                maxVolume = audio?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 0,
                fixedVolume = audio?.isVolumeFixed ?: true,
                muted = audio?.isStreamMute(AudioManager.STREAM_MUSIC) ?: false)
        }.getOrElse { DeviceQuickState(network.first, network.second) }
    }

    fun adjustVolume(direction: Int) {
        val manager = audio ?: return
        if (manager.isVolumeFixed || direction == 0) return
        manager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
            if (direction > 0) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER, 0)
    }
}
