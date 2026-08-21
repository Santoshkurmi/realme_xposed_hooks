package com.realme.modxposed.prefs

import android.content.Context
import android.content.SharedPreferences

object PreferencesManager {
    const val PREF_NAME = "settings"

    const val KEY_BATTERY_SHOW_CPU = "battery_decimal_show_cpu"
    const val KEY_BATTERY_CPU_INTERVAL = "battery_decimal_cpu_interval"
    const val KEY_BATTERY_POLL_INTERVAL = "battery_decimal_poll_interval"

    fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun isAppEnabled(context: Context, packageName: String): Boolean {
        return getPrefs(context).getBoolean("app_enabled_$packageName", true)
    }

    fun setAppEnabled(context: Context, packageName: String, enabled: Boolean) {
        getPrefs(context).edit().putBoolean("app_enabled_$packageName", enabled).apply()
    }

    fun isHookEnabled(context: Context, hookId: String): Boolean {
        return getPrefs(context).getBoolean("hook_enabled_$hookId", true)
    }

    fun setHookEnabled(context: Context, hookId: String, enabled: Boolean) {
        getPrefs(context).edit().putBoolean("hook_enabled_$hookId", enabled).apply()
    }

    fun getBatteryShowCpu(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_BATTERY_SHOW_CPU, true)
    }

    fun setBatteryShowCpu(context: Context, show: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_BATTERY_SHOW_CPU, show).apply()
    }

    fun getBatteryCpuInterval(context: Context): Long {
        return getPrefs(context).getLong(KEY_BATTERY_CPU_INTERVAL, 1000L)
    }

    fun setBatteryCpuInterval(context: Context, intervalMs: Long) {
        getPrefs(context).edit().putLong(KEY_BATTERY_CPU_INTERVAL, intervalMs).apply()
    }

    fun getBatteryPollInterval(context: Context): Long {
        return getPrefs(context).getLong(KEY_BATTERY_POLL_INTERVAL, 5000L)
    }

    fun setBatteryPollInterval(context: Context, intervalMs: Long) {
        getPrefs(context).edit().putLong(KEY_BATTERY_POLL_INTERVAL, intervalMs).apply()
    }
}
