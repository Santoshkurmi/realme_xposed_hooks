package com.realme.modxposed.prefs

import android.content.Context
import android.content.SharedPreferences
import java.io.File

object PreferencesManager {
    const val PREF_NAME = "settings"

    const val KEY_BATTERY_SHOW_CPU = "battery_decimal_show_cpu"
    const val KEY_BATTERY_SHOW_GPU = "battery_decimal_show_gpu"
    const val KEY_BATTERY_SHOW_POWER = "battery_decimal_show_power"
    const val KEY_BATTERY_SMOOTH_ESTIMATE = "battery_decimal_smooth_estimate"
    const val KEY_BATTERY_ENABLE_LOGGER = "battery_decimal_enable_logger"
    const val KEY_BATTERY_LOGGER_FLUSH_INTERVAL = "battery_decimal_logger_flush_interval"
    const val KEY_BATTERY_CPU_INTERVAL = "battery_decimal_cpu_interval"
    const val KEY_BATTERY_POLL_INTERVAL = "battery_decimal_poll_interval"
    const val KEY_LAUNCHER_GESTURE_HEIGHT = "launcher_gesture_height"

    fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    private fun makeWorldReadable(context: Context) {
        try {
            val prefsDir = File(context.applicationInfo.dataDir, "shared_prefs")
            if (prefsDir.exists()) {
                prefsDir.setReadable(true, false)
                prefsDir.setWritable(true, false)
                prefsDir.setExecutable(true, false)
                val prefsFile = File(prefsDir, "$PREF_NAME.xml")
                if (prefsFile.exists()) {
                    prefsFile.setReadable(true, false)
                    prefsFile.setWritable(true, false)
                }
            }
        } catch (ignored: Throwable) {}
    }

    fun isAppEnabled(context: Context, packageName: String): Boolean {
        return getPrefs(context).getBoolean("app_enabled_$packageName", true)
    }

    fun setAppEnabled(context: Context, packageName: String, enabled: Boolean) {
        getPrefs(context).edit().putBoolean("app_enabled_$packageName", enabled).commit()
        makeWorldReadable(context)
    }

    fun isHookEnabled(context: Context, hookId: String): Boolean {
        return getPrefs(context).getBoolean("hook_enabled_$hookId", true)
    }

    fun setHookEnabled(context: Context, hookId: String, enabled: Boolean) {
        getPrefs(context).edit().putBoolean("hook_enabled_$hookId", enabled).commit()
        makeWorldReadable(context)
    }

    fun getLauncherGestureHeight(context: Context): Int {
        return getPrefs(context).getInt(KEY_LAUNCHER_GESTURE_HEIGHT, 0)
    }

    fun setLauncherGestureHeight(context: Context, heightDp: Int) {
        getPrefs(context).edit().putInt(KEY_LAUNCHER_GESTURE_HEIGHT, heightDp).commit()
        makeWorldReadable(context)
    }

    fun getBatteryShowCpu(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_BATTERY_SHOW_CPU, true)
    }

    fun setBatteryShowCpu(context: Context, show: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_BATTERY_SHOW_CPU, show).commit()
        makeWorldReadable(context)
    }

    fun getBatteryShowGpu(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_BATTERY_SHOW_GPU, true)
    }

    fun setBatteryShowGpu(context: Context, show: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_BATTERY_SHOW_GPU, show).commit()
        makeWorldReadable(context)
    }

    fun getBatteryEnableLogger(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_BATTERY_ENABLE_LOGGER, false)
    }

    fun setBatteryEnableLogger(context: Context, enable: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_BATTERY_ENABLE_LOGGER, enable).commit()
        makeWorldReadable(context)
    }

    fun getBatteryLoggerFlushInterval(context: Context): Long {
        return getPrefs(context).getLong(KEY_BATTERY_LOGGER_FLUSH_INTERVAL, 60L)
    }

    fun setBatteryLoggerFlushInterval(context: Context, intervalSeconds: Long) {
        getPrefs(context).edit().putLong(KEY_BATTERY_LOGGER_FLUSH_INTERVAL, intervalSeconds).commit()
        makeWorldReadable(context)
    }

    fun getBatteryCpuInterval(context: Context): Long {
        return getPrefs(context).getLong(KEY_BATTERY_CPU_INTERVAL, 1000L)
    }

    fun setBatteryCpuInterval(context: Context, intervalMs: Long) {
        getPrefs(context).edit().putLong(KEY_BATTERY_CPU_INTERVAL, intervalMs).commit()
        makeWorldReadable(context)
    }

    fun getBatteryPollInterval(context: Context): Long {
        return getPrefs(context).getLong(KEY_BATTERY_POLL_INTERVAL, 5000L)
    }

    fun setBatteryPollInterval(context: Context, intervalMs: Long) {
        getPrefs(context).edit().putLong(KEY_BATTERY_POLL_INTERVAL, intervalMs).commit()
        makeWorldReadable(context)
    }

    fun getBatteryShowPower(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_BATTERY_SHOW_POWER, false)
    }

    fun setBatteryShowPower(context: Context, show: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_BATTERY_SHOW_POWER, show).commit()
        makeWorldReadable(context)
    }

    fun getBatterySmoothEstimate(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_BATTERY_SMOOTH_ESTIMATE, false)
    }

    fun setBatterySmoothEstimate(context: Context, smooth: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_BATTERY_SMOOTH_ESTIMATE, smooth).commit()
        makeWorldReadable(context)
    }

    // --- App Inspector & Modder Preferences ---
    const val KEY_INSPECTOR_TARGET_PACKAGES = "inspector_target_packages"
    const val KEY_INSPECTOR_HOOK_SHARED_PREFS = "inspector_hook_shared_prefs"
    const val KEY_INSPECTOR_HOOK_DATABASE = "inspector_hook_database"
    const val KEY_INSPECTOR_HOOK_INTENTS = "inspector_hook_intents"
    const val KEY_INSPECTOR_HOOK_JSON = "inspector_hook_json"
    const val KEY_INSPECTOR_HOOK_OVERLAY = "inspector_hook_overlay"

    fun getInspectorTargetPackages(context: Context): String {
        return getPrefs(context).getString(KEY_INSPECTOR_TARGET_PACKAGES, "com.mventus.ncell.activity") ?: "com.mventus.ncell.activity"
    }

    fun setInspectorTargetPackages(context: Context, packages: String) {
        getPrefs(context).edit().putString(KEY_INSPECTOR_TARGET_PACKAGES, packages).commit()
        makeWorldReadable(context)
    }

    fun getInspectorHookSharedPrefs(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_INSPECTOR_HOOK_SHARED_PREFS, true)
    }

    fun setInspectorHookSharedPrefs(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_INSPECTOR_HOOK_SHARED_PREFS, enabled).commit()
        makeWorldReadable(context)
    }

    fun getInspectorHookDatabase(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_INSPECTOR_HOOK_DATABASE, true)
    }

    fun setInspectorHookDatabase(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_INSPECTOR_HOOK_DATABASE, enabled).commit()
        makeWorldReadable(context)
    }

    fun getInspectorHookIntents(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_INSPECTOR_HOOK_INTENTS, true)
    }

    fun setInspectorHookIntents(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_INSPECTOR_HOOK_INTENTS, enabled).commit()
        makeWorldReadable(context)
    }

    fun getInspectorHookJson(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_INSPECTOR_HOOK_JSON, true)
    }

    fun setInspectorHookJson(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_INSPECTOR_HOOK_JSON, enabled).commit()
        makeWorldReadable(context)
    }

    fun getInspectorHookOverlay(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_INSPECTOR_HOOK_OVERLAY, true)
    }

    fun setInspectorHookOverlay(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_INSPECTOR_HOOK_OVERLAY, enabled).commit()
        makeWorldReadable(context)
    }
}
