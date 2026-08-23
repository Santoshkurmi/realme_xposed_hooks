package com.realme.modxposed.ui.screens

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.realme.modxposed.analytics.DailyTelemetrySummary
import com.realme.modxposed.analytics.TelemetryAnalyticsScreen
import com.realme.modxposed.analytics.TelemetryLogParser
import com.realme.modxposed.model.HookItem
import com.realme.modxposed.model.HookRegistry
import com.realme.modxposed.model.TargetApp
import com.realme.modxposed.prefs.PreferencesManager
import com.realme.modxposed.utils.RootUtils
import java.io.File
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen() {
    val context = LocalContext.current
    var selectedApp by remember { mutableStateOf<TargetApp?>(null) }
    var selectedLogSummary by remember { mutableStateOf<DailyTelemetrySummary?>(null) }
    var showMenu by remember { mutableStateOf(false) }

    // Intercept system Back button / gesture
    BackHandler(enabled = selectedApp != null || selectedLogSummary != null) {
        if (selectedLogSummary != null) {
            selectedLogSummary = null
        } else {
            selectedApp = null
        }
    }

    if (selectedLogSummary != null) {
        TelemetryAnalyticsScreen(
            summary = selectedLogSummary!!,
            onBack = { selectedLogSummary = null }
        )
        return
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
                            text = if (selectedApp != null) selectedApp!!.packageName else "Xposed Hook Controller & Analytics",
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
                    onSelectApp = { selectedApp = it },
                    onSelectLogSummary = { selectedLogSummary = it }
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
fun AppListOverviewScreen(
    onSelectApp: (TargetApp) -> Unit,
    onSelectLogSummary: (DailyTelemetrySummary) -> Unit
) {
    val context = LocalContext.current
    val logFiles = remember { TelemetryLogParser.listLogFiles() }
    val hasStoragePermission = remember(context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            android.os.Environment.isExternalStorageManager()
        } else {
            true
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Reboot Notice Banner Card
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

        if (!hasStoragePermission) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            try {
                                val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                    data = android.net.Uri.parse("package:${context.packageName}")
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                try {
                                    val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                                    context.startActivity(intent)
                                } catch (ignored: Exception) {}
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderSpecial,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "All Files Access Required",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = "Tap here to grant All Files Storage Permission to read /sdcard/logs/",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }

        // Section 1: System Telemetry Logs Header & Cards
        item {
            Text(
                text = "System Telemetry Logs (${logFiles.size} Days Logged)",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
        }

        if (logFiles.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Analytics, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "No log files found in /sdcard/logs/. Enable the Binary System Telemetry Logger in SystemUI hook settings to start recording metrics.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(logFiles) { file ->
                TelemetryLogFileCard(
                    file = file,
                    onClick = {
                        val summary = TelemetryLogParser.parseLogFile(file)
                        if (summary != null) {
                            onSelectLogSummary(summary)
                        }
                    }
                )
            }
        }

        // Section 2: Target Applications Header & List
        item {
            Text(
                text = "Target Applications (${HookRegistry.targetApps.size})",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
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
fun TelemetryLogFileCard(file: File, onClick: () -> Unit) {
    val dateStr = remember(file) { TelemetryLogParser.extractDateFromFileName(file.name) }
    val sizeKb = remember(file) { file.length() / 1024 }

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
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF00E676).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Assessment,
                    contentDescription = null,
                    tint = Color(0xFF00E676),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Log Date: $dateStr",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${file.name} • ${sizeKb} KB",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Open Analytics",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun TargetAppCard(app: TargetApp, onClick: () -> Unit) {
    val context = LocalContext.current
    var isAppEnabled by remember(app.packageName) {
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
    var isAppEnabled by remember(app.packageName) {
        mutableStateOf(PreferencesManager.isAppEnabled(context, app.packageName))
    }
    LaunchedEffect(app.packageName) {
        isAppEnabled = PreferencesManager.isAppEnabled(context, app.packageName)
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
    var isHookEnabled by remember(hook.id) {
        mutableStateOf(PreferencesManager.isHookEnabled(context, hook.id))
    }
    LaunchedEffect(hook.id) {
        isHookEnabled = PreferencesManager.isHookEnabled(context, hook.id)
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

            // Custom Feature Config Panel
            if (hook.supportsConfig && isHookEnabled && isParentAppEnabled) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                Spacer(modifier = Modifier.height(12.dp))

                if (hook.id == "HookRealBatteryDecimal") {
                    RealBatteryDecimalConfigPanel(context = context)
                } else if (hook.id == "LauncherAnimationHook") {
                    LauncherGestureConfigPanel(context = context)
                }
            }
        }
    }
}

@Composable
fun LauncherGestureConfigPanel(context: Context) {
    var heightDp by remember {
        mutableStateOf(PreferencesManager.getLauncherGestureHeight(context).toFloat())
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Bottom Gesture Height Slider",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = if (heightDp.toInt() == 0) "0 dp - Default Stock Height (No Mod)" else "${heightDp.toInt()} dp - Custom Height Override",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Slider(
            value = heightDp,
            onValueChange = { newValue ->
                heightDp = newValue
                PreferencesManager.setLauncherGestureHeight(context, newValue.toInt())
            },
            valueRange = 0f..100f,
            steps = 100
        )
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
    var showPower by remember {
        mutableStateOf(PreferencesManager.getBatteryShowPower(context))
    }
    var smoothEstimate by remember {
        mutableStateOf(PreferencesManager.getBatterySmoothEstimate(context))
    }
    var enableLogger by remember {
        mutableStateOf(PreferencesManager.getBatteryEnableLogger(context))
    }
    var loggerFlushInterval by remember {
        mutableStateOf(PreferencesManager.getBatteryLoggerFlushInterval(context).toFloat())
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

        // Display System Power Usage (W) Toggle
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Display Power Usage (W)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = if (showPower) "Shows live power draw in Watts (e.g. 2.15) calculated from sysfs" else "Power usage hidden",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Switch(
                checked = showPower,
                onCheckedChange = { checked ->
                    showPower = checked
                    PreferencesManager.setBatteryShowPower(context, checked)
                }
            )
        }

        // High-Precision Smooth Battery Decimal Toggle
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "High-Precision Smooth Battery Decimal",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = if (smoothEstimate) "Interpolates battery decimal via live current integration between raw gauge steps" else "Using raw gauge step battery percentage",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Switch(
                checked = smoothEstimate,
                onCheckedChange = { checked ->
                    smoothEstimate = checked
                    PreferencesManager.setBatterySmoothEstimate(context, checked)
                }
            )
        }

        // Enable System Telemetry Binary Logger Toggle
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Binary System Telemetry Logger",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = if (enableLogger) "Appends 16-byte records to /sdcard/logs/system_metrics_YYYY_MM_DD.bin" else "Binary telemetry logging disabled",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Switch(
                checked = enableLogger,
                onCheckedChange = { checked ->
                    enableLogger = checked
                    PreferencesManager.setBatteryEnableLogger(context, checked)
                }
            )
        }

        // Telemetry Logger RAM Flush Interval Slider
        if (enableLogger) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "RAM-to-Disk Flush Interval",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${loggerFlushInterval.roundToLong()} s",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Slider(
                    value = loggerFlushInterval,
                    onValueChange = { loggerFlushInterval = it },
                    onValueChangeFinished = {
                        PreferencesManager.setBatteryLoggerFlushInterval(context, loggerFlushInterval.roundToLong())
                    },
                    valueRange = 10f..300f,
                    steps = 28
                )
            }
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
