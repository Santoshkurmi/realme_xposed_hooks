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
            description = "Home screen launcher speed and animation modifications",
            primaryColorHex = 0xFF10B981, // Emerald Green
            hooks = listOf(
                HookItem(
                    id = "LauncherAnimationHook",
                    name = "Launcher Animation Tweaks",
                    description = "Adjusts home screen transition and app launch animation speeds",
                    targetClass = "com.android.launcher.LauncherAnimation"
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
        )
    )
}
