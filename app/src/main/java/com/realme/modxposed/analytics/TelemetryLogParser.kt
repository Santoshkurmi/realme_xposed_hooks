package com.realme.modxposed.analytics

import android.os.Environment
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.*

data class TelemetryRecord(
    val timestampMs: Long,
    val batterySoc: Float, // e.g., 81.58f
    val cpuPct: Int,       // 0..99
    val gpuPct: Int,       // 0..99
    val isCharging: Int,   // 0 = Discharging, 1 = Charging, 2 = Full
    val isScreenOn: Boolean
)

data class MinuteStats(
    val minuteLabel: String,     // e.g. "14:23"
    val sampleCount: Int,
    val startBattery: Float,
    val endBattery: Float,
    val batteryDelta: Float,
    val avgCpu: Float,
    val maxCpu: Int,
    val avgGpu: Float,
    val maxGpu: Int,
    val sotSeconds: Long,
    val screenOffSeconds: Long,
    val chargingSeconds: Long
)

data class HourlyStats(
    val hourOfDay: Int,          // 0..23
    val label: String,           // e.g. "14:00 - 15:00"
    val sampleCount: Int,
    val avgBattery: Float,
    val startBattery: Float,
    val endBattery: Float,
    val batteryDelta: Float,
    val avgCpu: Float,
    val maxCpu: Int,
    val avgGpu: Float,
    val maxGpu: Int,
    val sotSeconds: Long,
    val screenOffSeconds: Long,
    val chargingSeconds: Long,
    val dischargingSeconds: Long,
    val sotDischargingBattDelta: Float,      // % drained during Screen On
    val screenOffDischargingBattDelta: Float,  // % drained during Screen Off
    val chargingBattGain: Float               // % gained during Charging
)

data class FilteredRangeSummary(
    val records: List<TelemetryRecord>,
    val totalRecords: Int,
    val firstTimestampMs: Long,
    val lastTimestampMs: Long,
    val loggingDurationMs: Long,
    val startBattery: Float,
    val endBattery: Float,
    val batteryDelta: Float,
    val minBattery: Float,
    val maxBattery: Float,
    val sotSeconds: Long,
    val screenOffSeconds: Long,
    val chargingSeconds: Long,
    val dischargingSeconds: Long,
    val sotDischargingBattDelta: Float,      // Total % drained during Screen On
    val screenOffDischargingBattDelta: Float,  // Total % drained during Screen Off
    val chargingBattGain: Float,              // Total % gained during Charging
    val avgCpu: Float,
    val peakCpu: Int,
    val heavyCpuSeconds: Long,
    val avgGpu: Float,
    val peakGpu: Int,
    val heavyGpuSeconds: Long,
    val hourlyBreakdown: List<HourlyStats>,
    val minuteBreakdown: List<MinuteStats>
)

data class DailyTelemetrySummary(
    val dateStr: String,
    val file: File,
    val totalRecords: Int,
    val records: List<TelemetryRecord>
)

object TelemetryLogParser {

    private val LOG_DIRS = listOf(
        File("/sdcard/logs"),
        File("/storage/emulated/0/logs"),
        File(Environment.getExternalStorageDirectory(), "logs")
    )

    fun listLogFiles(): List<File> {
        val filesList = mutableListOf<File>()
        for (dir in LOG_DIRS) {
            if (dir.exists() && dir.isDirectory) {
                dir.listFiles()?.filter { it.isFile && it.name.startsWith("system_metrics_") && it.name.endsWith(".bin") }
                    ?.forEach { filesList.add(it) }
            }
        }
        return filesList.distinctBy { it.name }.sortedByDescending { extractDateFromFileName(it.name) }
    }

    fun extractDateFromFileName(fileName: String): String {
        return try {
            val datePart = fileName.removePrefix("system_metrics_").removeSuffix(".bin")
            val parts = datePart.split("_")
            if (parts.size >= 6) {
                "${parts[0]}-${parts[1]}-${parts[2]} ${parts[3]}:${parts[4]}:${parts[5]}"
            } else if (parts.size >= 3) {
                "${parts[0]}-${parts[1]}-${parts[2]}"
            } else {
                datePart.replace("_", "-")
            }
        } catch (e: Exception) {
            fileName
        }
    }

    fun parseLogFile(file: File): DailyTelemetrySummary? {
        if (!file.exists() || file.length() < 16) return null

        val records = mutableListOf<TelemetryRecord>()
        var fis: FileInputStream? = null
        val buffer = ByteArray(16)

        try {
            fis = FileInputStream(file)
            while (fis.read(buffer, 0, 16) == 16) {
                // Bytes 0..7: timestampMs (long)
                val timestampMs =
                    ((buffer[0].toLong() and 0xFF) shl 56) or
                    ((buffer[1].toLong() and 0xFF) shl 48) or
                    ((buffer[2].toLong() and 0xFF) shl 40) or
                    ((buffer[3].toLong() and 0xFF) shl 32) or
                    ((buffer[4].toLong() and 0xFF) shl 24) or
                    ((buffer[5].toLong() and 0xFF) shl 16) or
                    ((buffer[6].toLong() and 0xFF) shl 8) or
                    (buffer[7].toLong() and 0xFF)

                // Bytes 8..9: batterySoc (short in hundredths)
                val batterySocHundredths =
                    ((buffer[8].toInt() and 0xFF) shl 8) or
                    (buffer[9].toInt() and 0xFF)
                val batterySoc = batterySocHundredths / 100.0f

                // Byte 10: cpuPct
                val cpuPct = buffer[10].toInt() and 0xFF

                // Byte 11: gpuPct
                val gpuPct = buffer[11].toInt() and 0xFF

                // Byte 12: isCharging
                val isCharging = buffer[12].toInt() and 0xFF

                // Byte 13: isScreenOn
                val isScreenOn = buffer[13].toInt() != 0

                if (batterySoc > 0f) {
                    records.add(
                        TelemetryRecord(
                            timestampMs = timestampMs,
                            batterySoc = batterySoc,
                            cpuPct = cpuPct,
                            gpuPct = gpuPct,
                            isCharging = isCharging,
                            isScreenOn = isScreenOn
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // Partial read handle
        } finally {
            try { fis?.close() } catch (ignored: Throwable) {}
        }

        if (records.isEmpty()) return null

        val dateStr = extractDateFromFileName(file.name)
        return DailyTelemetrySummary(
            dateStr = dateStr,
            file = file,
            totalRecords = records.size,
            records = records
        )
    }

    fun computeFilteredSummary(allRecords: List<TelemetryRecord>, startHour: Int, endHour: Int): FilteredRangeSummary? {
        val cal = Calendar.getInstance()
        val records = allRecords.filter { rec ->
            cal.timeInMillis = rec.timestampMs
            val h = cal.get(Calendar.HOUR_OF_DAY)
            h in startHour..endHour
        }

        if (records.isEmpty()) return null

        val firstRecord = records.first()
        val lastRecord = records.last()

        val firstTime = firstRecord.timestampMs
        val lastTime = lastRecord.timestampMs
        val durationMs = (lastTime - firstTime).coerceAtLeast(0L)

        val startBattery = firstRecord.batterySoc
        val endBattery = lastRecord.batterySoc
        val batteryDelta = endBattery - startBattery

        var minBatt = Float.MAX_VALUE
        var maxBatt = Float.MIN_VALUE

        var sotMs = 0L
        var screenOffMs = 0L
        var chargingMs = 0L
        var dischargingMs = 0L

        var sotDischargingBattDelta = 0f
        var screenOffDischargingBattDelta = 0f
        var chargingBattGain = 0f

        var totalCpuSum = 0L
        var peakCpu = 0
        var heavyCpuMs = 0L
        var totalGpuSum = 0L
        var peakGpu = 0
        var heavyGpuMs = 0L

        for (i in records.indices) {
            val rec = records[i]

            if (rec.batterySoc < minBatt) minBatt = rec.batterySoc
            if (rec.batterySoc > maxBatt) maxBatt = rec.batterySoc

            totalCpuSum += rec.cpuPct
            if (rec.cpuPct > peakCpu) peakCpu = rec.cpuPct

            totalGpuSum += rec.gpuPct
            if (rec.gpuPct > peakGpu) peakGpu = rec.gpuPct

            if (i > 0) {
                val prev = records[i - 1]
                val deltaT = (rec.timestampMs - prev.timestampMs).coerceAtLeast(0L)
                val deltaB = rec.batterySoc - prev.batterySoc

                if (prev.cpuPct >= 50) heavyCpuMs += deltaT
                if (prev.gpuPct >= 50) heavyGpuMs += deltaT

                if (prev.isScreenOn) {
                    sotMs += deltaT
                    if (prev.isCharging > 0) {
                        chargingMs += deltaT
                        if (deltaB > 0) chargingBattGain += deltaB
                    } else {
                        dischargingMs += deltaT
                        if (deltaB < 0) sotDischargingBattDelta += deltaB
                    }
                } else {
                    screenOffMs += deltaT
                    if (prev.isCharging > 0) {
                        chargingMs += deltaT
                        if (deltaB > 0) chargingBattGain += deltaB
                    } else {
                        dischargingMs += deltaT
                        if (deltaB < 0) screenOffDischargingBattDelta += deltaB
                    }
                }
            }
        }

        val avgCpu = if (records.isNotEmpty()) totalCpuSum.toFloat() / records.size else 0f
        val avgGpu = if (records.isNotEmpty()) totalGpuSum.toFloat() / records.size else 0f

        // Hourly Breakdown List
        val hourlyMap = LinkedHashMap<Int, MutableList<TelemetryRecord>>()
        for (rec in records) {
            cal.timeInMillis = rec.timestampMs
            val h = cal.get(Calendar.HOUR_OF_DAY)
            hourlyMap.getOrPut(h) { mutableListOf() }.add(rec)
        }

        val hourlyList = mutableListOf<HourlyStats>()
        for (h in startHour..endHour) {
            val list = hourlyMap[h] ?: continue
            val hStartBatt = list.first().batterySoc
            val hEndBatt = list.last().batterySoc
            val hBattDelta = hEndBatt - hStartBatt

            var hAvgBatt = 0f
            var hCpuSum = 0L
            var hMaxCpu = 0
            var hGpuSum = 0L
            var hMaxGpu = 0

            var hSotMs = 0L
            var hOffMs = 0L
            var hChgMs = 0L
            var hDisMs = 0L

            var hSotDrain = 0f
            var hOffDrain = 0f
            var hChgGain = 0f

            for (i in list.indices) {
                val r = list[i]
                hAvgBatt += r.batterySoc
                hCpuSum += r.cpuPct
                if (r.cpuPct > hMaxCpu) hMaxCpu = r.cpuPct
                hGpuSum += r.gpuPct
                if (r.gpuPct > hMaxGpu) hMaxGpu = r.gpuPct

                if (i > 0) {
                    val prev = list[i - 1]
                    val dT = (r.timestampMs - prev.timestampMs).coerceAtLeast(0L)
                    val dB = r.batterySoc - prev.batterySoc

                    if (prev.isScreenOn) {
                        hSotMs += dT
                        if (prev.isCharging > 0) {
                            hChgMs += dT
                            if (dB > 0) hChgGain += dB
                        } else {
                            hDisMs += dT
                            if (dB < 0) hSotDrain += dB
                        }
                    } else {
                        hOffMs += dT
                        if (prev.isCharging > 0) {
                            hChgMs += dT
                            if (dB > 0) hChgGain += dB
                        } else {
                            hDisMs += dT
                            if (dB < 0) hOffDrain += dB
                        }
                    }
                }
            }

            hAvgBatt /= list.size
            val hAvgCpu = hCpuSum.toFloat() / list.size
            val hAvgGpu = hGpuSum.toFloat() / list.size
            val label = String.format(Locale.US, "%02d:00 - %02d:00", h, (h + 1) % 24)

            hourlyList.add(
                HourlyStats(
                    hourOfDay = h,
                    label = label,
                    sampleCount = list.size,
                    avgBattery = hAvgBatt,
                    startBattery = hStartBatt,
                    endBattery = hEndBatt,
                    batteryDelta = hBattDelta,
                    avgCpu = hAvgCpu,
                    maxCpu = hMaxCpu,
                    avgGpu = hAvgGpu,
                    maxGpu = hMaxGpu,
                    sotSeconds = hSotMs / 1000L,
                    screenOffSeconds = hOffMs / 1000L,
                    chargingSeconds = hChgMs / 1000L,
                    dischargingSeconds = hDisMs / 1000L,
                    sotDischargingBattDelta = hSotDrain,
                    screenOffDischargingBattDelta = hOffDrain,
                    chargingBattGain = hChgGain
                )
            )
        }

        // Minute Breakdown List
        val minuteMap = LinkedHashMap<String, MutableList<TelemetryRecord>>()
        val minFormat = SimpleDateFormat("HH:mm", Locale.US)
        for (rec in records) {
            val label = minFormat.format(Date(rec.timestampMs))
            minuteMap.getOrPut(label) { mutableListOf() }.add(rec)
        }

        val minuteList = mutableListOf<MinuteStats>()
        for ((mLabel, list) in minuteMap) {
            val mStartBatt = list.first().batterySoc
            val mEndBatt = list.last().batterySoc
            var mCpuSum = 0L
            var mMaxCpu = 0
            var mGpuSum = 0L
            var mMaxGpu = 0
            var mSotMs = 0L
            var mOffMs = 0L
            var mChgMs = 0L

            for (i in list.indices) {
                val r = list[i]
                mCpuSum += r.cpuPct
                if (r.cpuPct > mMaxCpu) mMaxCpu = r.cpuPct
                mGpuSum += r.gpuPct
                if (r.gpuPct > mMaxGpu) mMaxGpu = r.gpuPct

                if (i > 0) {
                    val prev = list[i - 1]
                    val dT = (r.timestampMs - prev.timestampMs).coerceAtLeast(0L)
                    if (prev.isScreenOn) mSotMs += dT else mOffMs += dT
                    if (prev.isCharging > 0) mChgMs += dT
                }
            }

            minuteList.add(
                MinuteStats(
                    minuteLabel = mLabel,
                    sampleCount = list.size,
                    startBattery = mStartBatt,
                    endBattery = mEndBatt,
                    batteryDelta = mEndBatt - mStartBatt,
                    avgCpu = mCpuSum.toFloat() / list.size,
                    maxCpu = mMaxCpu,
                    avgGpu = mGpuSum.toFloat() / list.size,
                    maxGpu = mMaxGpu,
                    sotSeconds = mSotMs / 1000L,
                    screenOffSeconds = mOffMs / 1000L,
                    chargingSeconds = mChgMs / 1000L
                )
            )
        }

        return FilteredRangeSummary(
            records = records,
            totalRecords = records.size,
            firstTimestampMs = firstTime,
            lastTimestampMs = lastTime,
            loggingDurationMs = durationMs,
            startBattery = startBattery,
            endBattery = endBattery,
            batteryDelta = batteryDelta,
            minBattery = if (minBatt == Float.MAX_VALUE) 0f else minBatt,
            maxBattery = if (maxBatt == Float.MIN_VALUE) 0f else maxBatt,
            sotSeconds = sotMs / 1000L,
            screenOffSeconds = screenOffMs / 1000L,
            chargingSeconds = chargingMs / 1000L,
            dischargingSeconds = dischargingMs / 1000L,
            sotDischargingBattDelta = sotDischargingBattDelta,
            screenOffDischargingBattDelta = screenOffDischargingBattDelta,
            chargingBattGain = chargingBattGain,
            avgCpu = avgCpu,
            peakCpu = peakCpu,
            heavyCpuSeconds = heavyCpuMs / 1000L,
            avgGpu = avgGpu,
            peakGpu = peakGpu,
            heavyGpuSeconds = heavyGpuMs / 1000L,
            hourlyBreakdown = hourlyList,
            minuteBreakdown = minuteList
        )
    }
}
