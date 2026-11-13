package com.pocketai.agent.data.device

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File

enum class BatteryProfile(val displayName: String, val description: String) {
    BALANCED("Balanced Mode", "Standard background scheduling, normal power usage"),
    PERFORMANCE("Performance Mode", "Prioritize task completion speed when device allows"),
    BATTERY_SAVER("Battery Saver Mode", "Defer non-critical tasks, pause background sync below 20%"),
    CHARGING_ONLY("Charging Only Mode", "Only execute heavy AI coding jobs when connected to power"),
    WIFI_ONLY("Wi-Fi Only Mode", "Block large file downloads or model calls when on mobile data"),
    THERMAL_PROTECTION("Thermal Protection", "Pause CPU-intensive workloads if device temperature rises")
}

data class DeviceMetrics(
    val batteryPercent: Int,
    val isCharging: Boolean,
    val batteryTemperatureCelsius: Float,
    val totalRamMb: Long,
    val availableRamMb: Long,
    val availableStorageGb: Double,
    val isWifiConnected: Boolean,
    val networkType: String,
    val activeTasksCount: Int
)

class DeviceManager(private val context: Context) {

    fun getDeviceMetrics(activeTasksCount: Int = 0): DeviceMetrics {
        // Battery info
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 80
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: 100
        val pct = if (level >= 0 && scale > 0) (level * 100 / scale) else 80

        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        val tempTenths = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 280) ?: 280
        val tempCelsius = tempTenths / 10.0f

        // Memory info
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? android.app.ActivityManager
        val memInfo = android.app.ActivityManager.MemoryInfo()
        actManager?.getMemoryInfo(memInfo)
        val totalRamMb = memInfo.totalMem / (1024 * 1024)
        val availRamMb = memInfo.availMem / (1024 * 1024)

        // Storage info
        val stat = StatFs(Environment.getDataDirectory().path)
        val availStorageGb = (stat.availableBlocksLong * stat.blockSizeLong).toDouble() / (1024 * 1024 * 1024)

        // Network info
        val connManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNetwork = connManager?.activeNetwork
        val caps = connManager?.getNetworkCapabilities(activeNetwork)
        val isWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        val isCellular = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
        val netType = when {
            isWifi -> "Wi-Fi (High Speed)"
            isCellular -> "Cellular (Mobile Data)"
            else -> "Disconnected"
        }

        return DeviceMetrics(
            batteryPercent = pct,
            isCharging = isCharging,
            batteryTemperatureCelsius = tempCelsius,
            totalRamMb = totalRamMb,
            availableRamMb = availRamMb,
            availableStorageGb = String.format("%.2f", availStorageGb).toDoubleOrNull() ?: 10.0,
            isWifiConnected = isWifi,
            networkType = netType,
            activeTasksCount = activeTasksCount
        )
    }

    fun isExecutionPermitted(profile: BatteryProfile): Pair<Boolean, String?> {
        val metrics = getDeviceMetrics()
        return when (profile) {
            BatteryProfile.BATTERY_SAVER -> {
                if (metrics.batteryPercent < 20 && !metrics.isCharging) {
                    Pair(false, "Paused: Battery is below 20% in Battery Saver mode.")
                } else Pair(true, null)
            }
            BatteryProfile.CHARGING_ONLY -> {
                if (!metrics.isCharging) {
                    Pair(false, "Paused: Device is not plugged into a charger.")
                } else Pair(true, null)
            }
            BatteryProfile.WIFI_ONLY -> {
                if (!metrics.isWifiConnected) {
                    Pair(false, "Paused: Wi-Fi connection required under Wi-Fi Only mode.")
                } else Pair(true, null)
            }
            BatteryProfile.THERMAL_PROTECTION -> {
                if (metrics.batteryTemperatureCelsius > 42.0f) {
                    Pair(false, "Paused: Device temperature high (${metrics.batteryTemperatureCelsius}°C). Cooling down.")
                } else Pair(true, null)
            }
            else -> Pair(true, null)
        }
    }
}
