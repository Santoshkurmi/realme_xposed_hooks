package com.realme.modxposed.analytics

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TelemetryAnalyticsScreen(
    summary: DailyTelemetrySummary,
    onBack: () -> Unit
) {
    var selectedFilterChip by remember { mutableStateOf("Full Day") }
    var startHourState by remember { mutableStateOf(0f) }
    var endHourState by remember { mutableStateOf(23f) }
    var selectedTab by remember { mutableStateOf(0) } // 0 = Hourly, 1 = Minute, 2 = Raw 1-Sec Logs

    val (startHour, endHour) = remember(selectedFilterChip, startHourState, endHourState) {
        when (selectedFilterChip) {
            "Morning (00-06)" -> 0 to 5
            "Day (06-12)" -> 6 to 11
            "Afternoon (12-18)" -> 12 to 17
            "Night (18-24)" -> 18 to 23
            "Custom Range" -> startHourState.roundToInt() to endHourState.roundToInt().coerceAtLeast(startHourState.roundToInt())
            else -> 0 to 23
        }
    }

    val rangeSummary = remember(summary, startHour, endHour) {
        TelemetryLogParser.computeFilteredSummary(summary.records, startHour, endHour)
    }

    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.US) }

    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Telemetry Log File?") },
            text = { Text("Are you sure you want to delete ${summary.file.name}? This action cannot be undone.") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        showDeleteDialog = false
                        summary.file.delete()
                        onBack()
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Telemetry Analytics Report",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = summary.dateStr,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Log File",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(innerPadding)
        ) {
            // 1. Time Scope Filter Chips
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Time Range Filter",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val ranges = listOf("Full Day", "Morning (00-06)", "Day (06-12)", "Afternoon (12-18)", "Night (18-24)", "Custom Range")
                        ranges.forEach { chip ->
                            FilterChip(
                                selected = selectedFilterChip == chip,
                                onClick = { selectedFilterChip = chip },
                                label = { Text(chip, fontSize = 12.sp) }
                            )
                        }
                    }

                    if (selectedFilterChip == "Custom Range") {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Custom Hours Scope: ${startHourState.roundToInt()}:00 to ${endHourState.roundToInt()}:00",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                RangeSlider(
                                    value = startHourState..endHourState,
                                    onValueChange = { range ->
                                        startHourState = range.start
                                        endHourState = range.endInclusive
                                    },
                                    valueRange = 0f..23f,
                                    steps = 22
                                )
                            }
                        }
                    }
                }
            }

            if (rangeSummary == null) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                            Text("No telemetry logs recorded in selected time range", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                val startTimeStr = timeFormat.format(Date(rangeSummary.firstTimestampMs))
                val endTimeStr = timeFormat.format(Date(rangeSummary.lastTimestampMs))
                val durationSec = rangeSummary.loggingDurationMs / 1000L

                // 2. Logging Span & Battery Delta Header Banner
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Log Span: $startTimeStr -> $endTimeStr", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                                Text(formatDuration(durationSec), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary)
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Start -> End Battery", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = String.format(Locale.US, "%.2f%% -> %.2f%%", rangeSummary.startBattery, rangeSummary.endBattery),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }

                                val deltaColor = if (rangeSummary.batteryDelta >= 0) Color(0xFF00E676) else Color(0xFFFF5252)
                                val deltaSign = if (rangeSummary.batteryDelta >= 0) "+" else ""
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(deltaColor.copy(alpha = 0.2f))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "$deltaSign${String.format(Locale.US, "%.2f", rangeSummary.batteryDelta)}%",
                                        fontWeight = FontWeight.Bold,
                                        color = deltaColor,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. System Telemetry Cards Grid (Screen Awake Drain vs Standby Drain vs Charging)
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        val sotHrs = (rangeSummary.sotSeconds.toFloat() / 3600f).coerceAtLeast(0.0001f)
                        val offHrs = (rangeSummary.screenOffSeconds.toFloat() / 3600f).coerceAtLeast(0.0001f)
                        val chgHrs = (rangeSummary.chargingSeconds.toFloat() / 3600f).coerceAtLeast(0.0001f)

                        val sotRate = rangeSummary.sotDischargingBattDelta / sotHrs
                        val offRate = rangeSummary.screenOffDischargingBattDelta / offHrs
                        val chgRate = rangeSummary.chargingBattGain / chgHrs

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ReportCard(
                                title = "Screen Awake (SOT)",
                                value = formatDuration(rangeSummary.sotSeconds),
                                subtext = String.format(Locale.US, "Drain: %.2f%% (%.2f%%/hr)", rangeSummary.sotDischargingBattDelta, sotRate),
                                icon = Icons.Default.Smartphone,
                                accentColor = Color(0xFFFF9100),
                                modifier = Modifier.weight(1f)
                            )

                            ReportCard(
                                title = "Screen Off (Standby)",
                                value = formatDuration(rangeSummary.screenOffSeconds),
                                subtext = String.format(Locale.US, "Drain: %.2f%% (%.2f%%/hr)", rangeSummary.screenOffDischargingBattDelta, offRate),
                                icon = Icons.Default.PowerSettingsNew,
                                accentColor = Color(0xFF29B6F6),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ReportCard(
                                title = "Charging Performance",
                                value = formatDuration(rangeSummary.chargingSeconds),
                                subtext = String.format(Locale.US, "Gain: +%.2f%% (+%.2f%%/hr)", rangeSummary.chargingBattGain, chgRate),
                                icon = Icons.Default.BatteryChargingFull,
                                accentColor = Color(0xFF00E676),
                                modifier = Modifier.weight(1f)
                            )

                            ReportCard(
                                title = "CPU / GPU Load",
                                value = String.format(Locale.US, "CPU %.1f%% | GPU %.1f%%", rangeSummary.avgCpu, rangeSummary.avgGpu),
                                subtext = "Peak: CPU ${rangeSummary.peakCpu}% | GPU ${rangeSummary.peakGpu}%",
                                icon = Icons.Default.Memory,
                                accentColor = Color(0xFFD500F9),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // 4. Granularity Tabs
                item {
                    TabRow(selectedTabIndex = selectedTab) {
                        Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Hourly (${rangeSummary.hourlyBreakdown.size})", fontSize = 12.sp) })
                        Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Minute (${rangeSummary.minuteBreakdown.size})", fontSize = 12.sp) })
                        Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Raw Logs (${rangeSummary.records.size})", fontSize = 12.sp) })
                    }
                }

                // 5. Tab Contents
                when (selectedTab) {
                    0 -> {
                        items(rangeSummary.hourlyBreakdown) { hourly ->
                            HourlyReportItemCard(hourly)
                        }
                    }
                    1 -> {
                        items(rangeSummary.minuteBreakdown) { minute ->
                            MinuteReportItemCard(minute)
                        }
                    }
                    2 -> {
                        items(rangeSummary.records.take(500)) { rec ->
                            RawLogRecordRow(rec, timeFormat)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReportCard(
    title: String,
    value: String,
    subtext: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(subtext, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun HourlyReportItemCard(hourly: HourlyStats) {
    var expanded by remember { mutableStateOf(false) }
    val deltaColor = if (hourly.batteryDelta >= 0) Color(0xFF00E676) else Color(0xFFFF5252)

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(hourly.label, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = String.format(Locale.US, "%.2f%% -> %.2f%%", hourly.startBattery, hourly.endBattery),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "(${if (hourly.batteryDelta >= 0) "+" else ""}${String.format(Locale.US, "%.2f", hourly.batteryDelta)}%)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = deltaColor
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Awake (SOT): ${formatDuration(hourly.sotSeconds)} (${String.format(Locale.US, "%.2f", hourly.sotDischargingBattDelta)}%)", fontSize = 11.sp, color = Color(0xFFFF9100))
                        Text("Standby (Off): ${formatDuration(hourly.screenOffSeconds)} (${String.format(Locale.US, "%.2f", hourly.screenOffDischargingBattDelta)}%)", fontSize = 11.sp, color = Color(0xFF29B6F6))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Charging: ${formatDuration(hourly.chargingSeconds)} (+${String.format(Locale.US, "%.2f", hourly.chargingBattGain)}%)", fontSize = 11.sp, color = Color(0xFF00E676))
                        Text("CPU/GPU: ${String.format(Locale.US, "%.0f", hourly.avgCpu)}% / ${String.format(Locale.US, "%.0f", hourly.avgGpu)}%", fontSize = 11.sp, color = Color(0xFFD500F9))
                    }
                }
            }
        }
    }
}

@Composable
fun MinuteReportItemCard(minute: MinuteStats) {
    val deltaColor = if (minute.batteryDelta >= 0) Color(0xFF00E676) else Color(0xFFFF5252)

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(minute.minuteLabel, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Text("CPU ${String.format(Locale.US, "%.0f", minute.avgCpu)}%", fontSize = 11.sp, color = Color(0xFFFF9100))
                Spacer(modifier = Modifier.width(6.dp))
                Text("GPU ${String.format(Locale.US, "%.0f", minute.avgGpu)}%", fontSize = 11.sp, color = Color(0xFFD500F9))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = String.format(Locale.US, "%.2f%% -> %.2f%%", minute.startBattery, minute.endBattery),
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "(${if (minute.batteryDelta >= 0) "+" else ""}${String.format(Locale.US, "%.2f", minute.batteryDelta)}%)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = deltaColor
                )
            }
        }
    }
}

@Composable
fun RawLogRecordRow(rec: TelemetryRecord, timeFormat: SimpleDateFormat) {
    val timeStr = timeFormat.format(Date(rec.timestampMs))
    val chargingStr = if (rec.isCharging > 0) "Charging" else "Discharging"
    val screenStr = if (rec.isScreenOn) "Screen On" else "Screen Off"
    val battColor = if (rec.isCharging > 0) Color(0xFF00E676) else MaterialTheme.colorScheme.onSurface

    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(timeStr, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            Text("Batt: ${String.format(Locale.US, "%.2f", rec.batterySoc)}%", fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = battColor)
            Text("CPU ${rec.cpuPct}%", fontSize = 11.sp, color = Color(0xFFFF9100))
            Text("GPU ${rec.gpuPct}%", fontSize = 11.sp, color = Color(0xFFD500F9))
            Text("$chargingStr | $screenStr", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun formatDuration(seconds: Long): String {
    val hrs = seconds / 3600
    val mins = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hrs > 0) "${hrs}h ${mins}m ${secs}s" else if (mins > 0) "${mins}m ${secs}s" else "${secs}s"
}
