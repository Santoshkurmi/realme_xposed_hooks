package com.realme.modxposed.ui.screens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.realme.modxposed.model.HookItem
import com.realme.modxposed.model.HookRegistry
import com.realme.modxposed.model.TargetApp
import com.realme.modxposed.prefs.PreferencesManager
import kotlin.math.roundToLong

import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.realme.modxposed.utils.RootUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen() {
    val context = LocalContext.current
    var selectedApp by remember { mutableStateOf<TargetApp?>(null) }
    var showMenu by remember { mutableStateOf(false) }

    // Intercept system Back button / gesture when viewing App Detail Screen
    BackHandler(enabled = selectedApp != null) {
        selectedApp = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = selectedApp?.appName ?: "Realme ModXposed",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Text(
                            text = if (selectedApp != null) selectedApp!!.packageName else "Xposed Hook Controller",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    if (selectedApp != null) {
                        IconButton(onClick = { selectedApp = null }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "Logo",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                },
                actions = {
                    if (selectedApp != null) {
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Options")
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Restart ${selectedApp!!.appName} Scope") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Refresh,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        val app = selectedApp!!
                                        RootUtils.killAndRestartPackage(context, app.packageName, app.appName)
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        AnimatedContent(
            targetState = selectedApp,
            modifier = Modifier.padding(innerPadding),
            transitionSpec = {
                if (targetState != null) {
                    slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                } else {
                    slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                }
            },
            label = "screen_transition"
        ) { app ->
            if (app == null) {
                AppListOverviewScreen(
                    onSelectApp = { selectedApp = it }
                )
            } else {
                AppDetailScreen(
                    app = app
                )
            }
        }
    }
}

@Composable
fun AppListOverviewScreen(onSelectApp: (TargetApp) -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Reboot Required Notice",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "After changing hook toggles or interval values, restart the target app or reboot your device to apply changes.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "Target Applications (${HookRegistry.targetApps.size})",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
        }

        items(HookRegistry.targetApps) { app ->
            TargetAppCard(
                app = app,
                onClick = { onSelectApp(app) }
            )
        }
    }
}

@Composable
fun TargetAppCard(app: TargetApp, onClick: () -> Unit) {
    val context = LocalContext.current
    var isAppEnabled by remember {
        mutableStateOf(PreferencesManager.isAppEnabled(context, app.packageName))
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Icon Circle
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(app.primaryColorHex).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = app.appName.take(1).uppercase(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(app.primaryColorHex)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.appName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${app.hooks.size} Hook${if (app.hooks.size > 1) "s" else ""} designed",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Global App Toggle Switch
            Switch(
                checked = isAppEnabled,
                onCheckedChange = { enabled ->
                    isAppEnabled = enabled
                    PreferencesManager.setAppEnabled(context, app.packageName, enabled)
                }
            )

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Open",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AppDetailScreen(app: TargetApp) {
    val context = LocalContext.current
    var isAppEnabled by remember {
        mutableStateOf(PreferencesManager.isAppEnabled(context, app.packageName))
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Master Control Header Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(app.primaryColorHex).copy(alpha = 0.15f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Enable All Hooks for ${app.appName}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isAppEnabled) "Hooks are active when app runs" else "All hooks for this app are globally disabled",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = isAppEnabled,
                        onCheckedChange = { enabled ->
                            isAppEnabled = enabled
                            PreferencesManager.setAppEnabled(context, app.packageName, enabled)
                        }
                    )
                }
            }
        }

        item {
            Text(
                text = "Individual Hook Controllers",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(app.hooks) { hook ->
            HookItemCard(
                hook = hook,
                isParentAppEnabled = isAppEnabled
            )
        }
    }
}

@Composable
fun HookItemCard(hook: HookItem, isParentAppEnabled: Boolean) {
    val context = LocalContext.current
    var isHookEnabled by remember {
        mutableStateOf(PreferencesManager.isHookEnabled(context, hook.id))
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = hook.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = hook.description,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = isHookEnabled && isParentAppEnabled,
                    enabled = isParentAppEnabled,
                    onCheckedChange = { enabled ->
                        isHookEnabled = enabled
                        PreferencesManager.setHookEnabled(context, hook.id, enabled)
                    }
                )
            }

            // Custom Feature Config Panel (Specialized for HookRealBatteryDecimal)
            if (hook.supportsConfig && isHookEnabled && isParentAppEnabled) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                Spacer(modifier = Modifier.height(12.dp))

                RealBatteryDecimalConfigPanel(context = context)
            }
        }
    }
}

@Composable
fun RealBatteryDecimalConfigPanel(context: Context) {
    var showCpu by remember {
        mutableStateOf(PreferencesManager.getBatteryShowCpu(context))
    }
    var showGpu by remember {
        mutableStateOf(PreferencesManager.getBatteryShowGpu(context))
    }
    var cpuInterval by remember {
        mutableStateOf(PreferencesManager.getBatteryCpuInterval(context).toFloat())
    }
    var batteryInterval by remember {
        mutableStateOf(PreferencesManager.getBatteryPollInterval(context).toFloat())
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Feature Tweaks",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.primary
        )

        // Show CPU Toggle
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Display System CPU Percentage",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = if (showCpu) "Shows CPU percentage after battery decimal" else "CPU percentage hidden",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Switch(
                checked = showCpu,
                onCheckedChange = { checked ->
                    showCpu = checked
                    PreferencesManager.setBatteryShowCpu(context, checked)
                }
            )
        }

        // Show GPU Toggle
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Display System GPU Percentage",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = if (showGpu) "Shows GPU percentage (/sys/class/kgsl/kgsl-3d0) after CPU" else "GPU percentage hidden",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Switch(
                checked = showGpu,
                onCheckedChange = { checked ->
                    showGpu = checked
                    PreferencesManager.setBatteryShowGpu(context, checked)
                }
            )
        }

        // CPU & GPU Polling Interval Slider
        if (showCpu || showGpu) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "CPU & GPU Polling Interval",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${cpuInterval.roundToLong()} ms",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Slider(
                    value = cpuInterval,
                    onValueChange = { cpuInterval = it },
                    onValueChangeFinished = {
                        PreferencesManager.setBatteryCpuInterval(context, cpuInterval.roundToLong())
                    },
                    valueRange = 500f..5000f,
                    steps = 8
                )
            }
        }

        // Battery Polling Interval Slider
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Battery Sysfs Polling Interval",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${batteryInterval.roundToLong()} ms",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            Slider(
                value = batteryInterval,
                onValueChange = { batteryInterval = it },
                onValueChangeFinished = {
                    PreferencesManager.setBatteryPollInterval(context, batteryInterval.roundToLong())
                },
                valueRange = 1000f..10000f,
                steps = 8
            )
        }
    }
}
