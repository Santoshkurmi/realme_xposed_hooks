package com.realme.modxposed.model

data class HookItem(
    val id: String,
    val name: String,
    val description: String,
    val targetClass: String,
    val supportsConfig: Boolean = false
)

data class TargetApp(
    val packageName: String,
    val appName: String,
    val description: String,
    val primaryColorHex: Long,
    val hooks: List<HookItem>
)

object HookRegistry {
    val targetApps = listOf(
        TargetApp(
            packageName = "com.android.systemui",
            appName = "System UI",
            description = "Status bar battery decimal, clock, and gesture navigation customizations",
            primaryColorHex = 0xFF6366F1, // Indigo
            hooks = listOf(
                HookItem(
                    id = "HookRealBatteryDecimal",
                    name = "Decimal Battery & CPU Display",
                    description = "Shows real-time battery SOC (2 decimal places) + CPU usage % in the status bar",
                    targetClass = "com.oplus.systemui.statusbar.pipeline.battery.ui.view.StatBatteryMeterView",
                    supportsConfig = true
                ),
                HookItem(
                    id = "HookClock",
                    name = "Status Bar Clock Formatting",
                    description = "Customizes clock format and display in System UI header",
                    targetClass = "com.android.systemui.statusbar.policy.Clock"
                ),
                HookItem(
                    id = "GestureNavigationView",
                    name = "Gesture Navigation Bar Tweaks",
                    description = "Adjusts gesture navigation bar parameters and visibility",
                    targetClass = "com.android.systemui.navigationbar.gestural.GestureNavigationView"
                )
            )
        ),
        TargetApp(
            packageName = "com.android.launcher",
            appName = "System Launcher",
            description = "Home screen launcher gesture and animation modifications",
            primaryColorHex = 0xFF10B981, // Emerald Green
            hooks = listOf(
                HookItem(
                    id = "LauncherAnimationHook",
                    name = "Bottom Gesture Height Override",
                    description = "Adjusts bottom swipe gesture height in System Launcher. 0 = Default (No Mod).",
                    targetClass = "com.android.launcher.navigation.NavigationController",
                    supportsConfig = true
                )
            )
        ),
        TargetApp(
            packageName = "com.f1soft.banksmart.siddhartha",
            appName = "Siddhartha Bank Smart",
            description = "Siddhartha Bank application features",
            primaryColorHex = 0xFFF59E0B, // Amber Gold
            hooks = listOf(
                HookItem(
                    id = "Siddha",
                    name = "Siddhartha Bank Mod",
                    description = "Unlocks features and bypasses checks in Siddhartha Bank app",
                    targetClass = "com.f1soft.banksmart.siddhartha.MainActivity"
                )
            )
        ),
        TargetApp(
            packageName = "com.hamrocsit",
            appName = "Hamro CSIT",
            description = "Hamro CSIT portal tweaks",
            primaryColorHex = 0xFF8B5CF6, // Purple
            hooks = listOf(
                HookItem(
                    id = "HamroCsit",
                    name = "Hamro CSIT Quiz Mod",
                    description = "Unlocks premium quiz solutions and features in Hamro CSIT",
                    targetClass = "com.hamrocsit.MainActivity"
                )
            )
        ),
        TargetApp(
            packageName = "com.engineeringnepal.ghoksewa",
            appName = "GhokSewa",
            description = "GhokSewa engineering prep features",
            primaryColorHex = 0xFF14B8A6, // Teal
            hooks = listOf(
                HookItem(
                    id = "GhokSewaMod",
                    name = "GhokSewa Prep Mod",
                    description = "Unlocks practice sets and premium material in GhokSewa",
                    targetClass = "com.engineeringnepal.ghoksewa.MainActivity"
                )
            )
        ),
        TargetApp(
            packageName = "com.realme.modxposed.inspector",
            appName = "⚡ Cool App Modder & Inspector",
            description = "Dynamic In-App Floating Overlay for SharedPreferences, Databases, Intents, and JSON/Gson Logging across custom package names",
            primaryColorHex = 0xFF00E5FF, // Cyan
            hooks = listOf(
                HookItem(
                    id = "AppInspectorHook",
                    name = "In-App Inspector & Modder Suite",
                    description = "Injects floating overlay hub and enables real-time memory & network inspections for configured packages",
                    targetClass = "com.realme.modxposed.hooks.SharedPrefsInspector",
                    supportsConfig = true
                )
            )
        )
    )
}
