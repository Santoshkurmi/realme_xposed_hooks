package com.realme.modxposed.hooks;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.realme.modxposed.IXposedHookLoadPackage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookRealBatteryDecimal implements IXposedHookLoadPackage {

    private static final String GAUGE_INFO_PATH = "/sys/devices/virtual/oplus_chg/battery/gauge_info";
    private static final String PROC_STAT_PATH = "/proc/stat";
    private static final String GPU_BUSY_PATH = "/sys/class/kgsl/kgsl-3d0/gpubusy";
    private static final String LOG_DIR_PATH = "/storage/emulated/0/logs";

    private static final byte[] KEY_08 = new byte[]{'0', 'x', '0', '8', '='};
    private static final byte[] KEY_0C = new byte[]{'0', 'x', '0', 'c', '='};
    private static final byte[] KEY_10 = new byte[]{'0', 'x', '1', '0', '='};
    private static final byte[] KEY_12 = new byte[]{'0', 'x', '1', '2', '='};

    private final java.text.SimpleDateFormat sessionDateFormat = new java.text.SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.US);
    private String currentSessionLogPath = null;

    private synchronized String getDailyLogFilePath() {
        if (currentSessionLogPath == null) {
            File dir = new File(LOG_DIR_PATH);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            String sessionStr = sessionDateFormat.format(new java.util.Date());
            currentSessionLogPath = LOG_DIR_PATH + "/system_metrics_" + sessionStr + ".bin";
        }
        return currentSessionLogPath;
    }

    private static boolean showCpu = true;
    private static boolean showGpu = true;
    private static boolean showPower = false;
    private static boolean useSmoothEstimate = false;
    private static boolean enableLogger = false;
    private static long loggerFlushIntervalSec = 60L;
    private static long batteryIntervalMs = 5000;
    private static long cpuIntervalMs = 1000;

    // Target Classes
    private static final String STAT_BATTERY_VIEW_CLASS = "com.oplus.systemui.statusbar.pipeline.battery.ui.view.StatBatteryMeterView";
    private static final String HORIZONTAL_DRAWABLE_CLASS = "com.oplus.systemui.statusbar.pipeline.battery.ui.drawable.HorizontalBatteryContentDrawable";

    // Pre-allocated CPU Strings Table (0..99)
    private static final String[] CPU_STRINGS = new String[100];
    static {
        for (int i = 0; i < 100; i++) {
            String cpuStr = (i < 10) ? ("\u2007" + i) : String.valueOf(i);
            CPU_STRINGS[i] = " " + cpuStr;
        }
    }

    // Pre-allocated GPU Strings Table (0..100 -> max 99)
    private static final String[] GPU_STRINGS = new String[101];
    static {
        for (int i = 0; i <= 100; i++) {
            int displayVal = Math.min(99, i);
            String gpuStr = (displayVal < 10) ? ("\u2007" + displayVal) : String.valueOf(displayVal);
            GPU_STRINGS[i] = " " + gpuStr;
        }
    }

    // Pre-allocated Battery Decimal Strings Table (0..10000 -> 0.00% to 100.00%)
    private static final String[] BATTERY_DECIMAL_STRINGS = new String[10001];
    static {
        for (int i = 0; i <= 10000; i++) {
            int integerPart = i / 100;
            int fractionPart = i % 100;
            BATTERY_DECIMAL_STRINGS[i] = String.format(Locale.US, "%d.%02d", integerPart, fractionPart);
        }
    }

    // Pre-allocated Power Watts Strings Table (0..2000 -> 0.00 to 20.00)
    private static final String[] POWER_WATTS_STRINGS = new String[2001];
    static {
        for (int i = 0; i <= 2000; i++) {
            int integerPart = i / 100;
            int fractionPart = i % 100;
            POWER_WATTS_STRINGS[i] = String.format(Locale.US, "%d.%02d", integerPart, fractionPart);
        }
    }

    private static volatile String cachedRawBatterySoc = null;
    private static volatile short cachedBatterySocHundredths = 0;
    private static volatile byte cachedIsCharging = 0; // 0 = Discharging, 1 = Charging, 2 = Full
    private static volatile int cachedCpuPercentage = 0;
    private static volatile int cachedGpuPercentage = 0;
    private static volatile String cachedPowerWattsString = null;
    private static volatile String cachedDecimalPercentage = null;

    // Coulomb counter integration state
    private int lastRemMah = -1;
    private int lastFullMah = -1;
    private double accumMah = 0.0;
    private int prevCurrentMa = 0;
    private boolean hasPrevSample = false;
    private long lastGaugeSampleTimeMs = 0;

    private static volatile boolean isScreenOn = true;
    private static volatile boolean isReceiverRegistered = false;

    private Handler backgroundBatteryHandler;
    private Handler backgroundCpuHandler;
    private final Set<TextView> activeTextViewSet = Collections.synchronizedSet(new HashSet<TextView>());

    private long prevCpuTotal = 0;
    private long prevCpuIdle = 0;
    private boolean isFirstCpuSample = true;

    // Persistent Zero-GC RandomAccessFile instances
    private RandomAccessFile batteryRaf = null;
    private RandomAccessFile cpuRaf = null;
    private RandomAccessFile gpuRaf = null;

    private final byte[] batteryStatBuffer = new byte[512];
    private final byte[] procStatBuffer = new byte[256];
    private final byte[] gpuStatBuffer = new byte[64];
    private final int[] parsePos = new int[1];

    // Pre-allocated Zero-GC binary logger buffer (300 records max = 4800 bytes)
    private final byte[] loggerRamBuffer = new byte[4800];
    private int loggerBufferPos = 0;
    private long lastFlushTimeMs = 0;

    // Pre-allocated permanent Runnable references
    private final Runnable batteryPollRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isScreenOn) return;
            String newDecimal = readGaugeInfoFromSysfs();
            if (newDecimal != null) {
                cachedRawBatterySoc = newDecimal;
                updateCombinedPercentageAndNotify();
            }
            if (isScreenOn && backgroundBatteryHandler != null) {
                backgroundBatteryHandler.postDelayed(this, batteryIntervalMs);
            }
        }
    };

    private final Runnable cpuPollRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isScreenOn) return;
            if (showCpu) {
                calculateCpuUsageZeroGc();
            }
            if (showGpu) {
                calculateGpuUsageZeroGc();
            }
            updateCombinedPercentageAndNotify();

            if (enableLogger) {
                long now = System.currentTimeMillis();
                writeLogRecordToRamBuffer(now, cachedBatterySocHundredths, cachedCpuPercentage, cachedGpuPercentage, cachedIsCharging, (byte) 1);
                if (now - lastFlushTimeMs >= loggerFlushIntervalSec * 1000L) {
                    flushRamBufferToDisk();
                }
            }

            if (isScreenOn && backgroundCpuHandler != null) {
                backgroundCpuHandler.postDelayed(this, cpuIntervalMs);
            }
        }
    };

    private void loadXposedPreferences() {
        try {
            XSharedPreferences prefs = new XSharedPreferences("com.realme.modxposed", "settings");
            prefs.makeWorldReadable();
            showCpu = prefs.getBoolean("battery_decimal_show_cpu", true);
            showGpu = prefs.getBoolean("battery_decimal_show_gpu", true);
            showPower = prefs.getBoolean("battery_decimal_show_power", false);
            useSmoothEstimate = prefs.getBoolean("battery_decimal_smooth_estimate", false);
            enableLogger = prefs.getBoolean("battery_decimal_enable_logger", false);
            loggerFlushIntervalSec = prefs.getLong("battery_decimal_logger_flush_interval", 60L);
            cpuIntervalMs = prefs.getLong("battery_decimal_cpu_interval", 1000L);
            batteryIntervalMs = prefs.getLong("battery_decimal_poll_interval", 5000L);
        } catch (Throwable ignored) {}
    }

    @Override
    public void init(XC_LoadPackage.LoadPackageParam lpparam) {
        loadXposedPreferences();

        // 1. Start Background Polling Threads
        try {
            HandlerThread batteryThread = new HandlerThread("BatteryDecimalThread");
            batteryThread.start();
            backgroundBatteryHandler = new Handler(batteryThread.getLooper());

            HandlerThread cpuThread = new HandlerThread("CpuStatThread");
            cpuThread.start();
            backgroundCpuHandler = new Handler(cpuThread.getLooper());

            startBackgroundPolling();
        } catch (Throwable ignored) {}

        // Target 1: StatBatteryMeterView
        try {
            Class<?> clazz = XposedHelpers.findClass(STAT_BATTERY_VIEW_CLASS, lpparam.classLoader);

            XposedHelpers.findAndHookMethod(clazz, "onAttachedToWindow", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    View view = (View) param.thisObject;
                    ensureScreenReceiverRegistered(view.getContext());
                    registerAndImmediatelyUpdate(view);
                }
            });

            XposedHelpers.findAndHookMethod(clazz, "onDetachedFromWindow", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    View view = (View) param.thisObject;
                    unregisterView(view);
                }
            });

            // Hook dispatchDraw: Instantly intercepts charger plug/unplug integer resets!
            XposedHelpers.findAndHookMethod(clazz, "dispatchDraw", android.graphics.Canvas.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    View view = (View) param.thisObject;
                    updateBatteryTextViewsInstant(view);
                }
            });
        } catch (Throwable ignored) {}

        // Target 2: HorizontalBatteryContentDrawable
        try {
            Class<?> clazz = XposedHelpers.findClass(HORIZONTAL_DRAWABLE_CLASS, lpparam.classLoader);

            XposedHelpers.findAndHookMethod(clazz, "draw", android.graphics.Canvas.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    String currentDecimal = cachedDecimalPercentage;
                    if (currentDecimal != null) {
                        try {
                            XposedHelpers.setObjectField(param.thisObject, "levelString", currentDecimal);
                        } catch (Throwable ignored) {}
                    }
                }
            });
        } catch (Throwable ignored) {}
    }

    private void ensureScreenReceiverRegistered(Context context) {
        if (isReceiverRegistered || context == null) return;
        synchronized (HookRealBatteryDecimal.class) {
            if (!isReceiverRegistered) {
                try {
                    Context appContext = context.getApplicationContext();
                    IntentFilter filter = new IntentFilter();
                    filter.addAction(Intent.ACTION_SCREEN_ON);
                    filter.addAction(Intent.ACTION_SCREEN_OFF);
                    filter.addAction(Intent.ACTION_BATTERY_CHANGED);

                    appContext.registerReceiver(new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context ctx, Intent intent) {
                            if (intent == null || intent.getAction() == null) return;
                            String action = intent.getAction();
                            if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                                int status = intent.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1);
                                if (status == android.os.BatteryManager.BATTERY_STATUS_CHARGING) {
                                    cachedIsCharging = 1;
                                } else if (status == android.os.BatteryManager.BATTERY_STATUS_FULL) {
                                    cachedIsCharging = 2;
                                } else {
                                    cachedIsCharging = 0;
                                }
                            } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                                isScreenOn = false;
                                if (enableLogger) {
                                    writeLogRecordToRamBuffer(System.currentTimeMillis(), cachedBatterySocHundredths, cachedCpuPercentage, cachedGpuPercentage, cachedIsCharging, (byte) 0);
                                    flushRamBufferToDisk();
                                }
                                stopBackgroundPolling();
                            } else if (Intent.ACTION_SCREEN_ON.equals(action)) {
                                isScreenOn = true;
                                startBackgroundPolling();
                            }
                        }
                    }, filter);

                    isReceiverRegistered = true;
                } catch (Throwable ignored) {}
            }
        }
    }

    private synchronized void startBackgroundPolling() {
        if (!isScreenOn) return;
        stopBackgroundPolling(); // Prevent duplicate posts

        readGaugeInfoFromSysfs();

        if (backgroundBatteryHandler != null) {
            backgroundBatteryHandler.post(batteryPollRunnable);
        }
        if (backgroundCpuHandler != null) {
            backgroundCpuHandler.post(cpuPollRunnable);
        }
    }

    private synchronized void stopBackgroundPolling() {
        if (backgroundBatteryHandler != null) {
            backgroundBatteryHandler.removeCallbacks(batteryPollRunnable);
        }
        if (backgroundCpuHandler != null) {
            backgroundCpuHandler.removeCallbacks(cpuPollRunnable);
        }
        isFirstCpuSample = true;

        closeRaf(batteryRaf); batteryRaf = null;
        closeRaf(cpuRaf); cpuRaf = null;
        closeRaf(gpuRaf); gpuRaf = null;
    }

    private static void closeRaf(RandomAccessFile raf) {
        if (raf != null) {
            try { raf.close(); } catch (Throwable ignored) {}
        }
    }

    private synchronized void updateCombinedPercentageAndNotify() {
        if (cachedRawBatterySoc != null) {
            String combined = cachedRawBatterySoc;
            if (showCpu) {
                combined = combined + CPU_STRINGS[cachedCpuPercentage];
            }
            if (showGpu) {
                combined = combined + GPU_STRINGS[cachedGpuPercentage];
            }
            if (showPower && cachedPowerWattsString != null) {
                combined = combined + " " + cachedPowerWattsString;
            }
            if (enableLogger) {
                combined = combined + ".";
            }
            if (!combined.equals(cachedDecimalPercentage)) {
                cachedDecimalPercentage = combined;
                updateAllRegisteredViews(combined);
            }
        }
    }

    private void calculateCpuUsageZeroGc() {
        if (!isScreenOn) return;

        try {
            if (cpuRaf == null) {
                cpuRaf = new RandomAccessFile(PROC_STAT_PATH, "r");
            }
            cpuRaf.seek(0);
            int bytesRead = cpuRaf.read(procStatBuffer);
            if (bytesRead <= 0) return;

            if (bytesRead > 3 && procStatBuffer[0] == 'c' && procStatBuffer[1] == 'p' && procStatBuffer[2] == 'u') {
                parsePos[0] = 3;
                long user = parseNextLong(procStatBuffer, bytesRead, parsePos);
                long nice = parseNextLong(procStatBuffer, bytesRead, parsePos);
                long system = parseNextLong(procStatBuffer, bytesRead, parsePos);
                long idle = parseNextLong(procStatBuffer, bytesRead, parsePos);
                long iowait = parseNextLong(procStatBuffer, bytesRead, parsePos);
                long irq = parseNextLong(procStatBuffer, bytesRead, parsePos);
                long softirq = parseNextLong(procStatBuffer, bytesRead, parsePos);
                long steal = parseNextLong(procStatBuffer, bytesRead, parsePos);

                long totalCpu = user + nice + system + idle + iowait + irq + softirq + steal;
                long idleCpu = idle + iowait;

                if (!isFirstCpuSample) {
                    long totalDiff = totalCpu - prevCpuTotal;
                    long idleDiff = idleCpu - prevCpuIdle;

                    if (totalDiff > 0) {
                        int cpu = (int) Math.round(((totalDiff - idleDiff) * 100.0) / totalDiff);
                        cachedCpuPercentage = Math.min(99, Math.max(0, cpu));
                    }
                } else {
                    isFirstCpuSample = false;
                }

                prevCpuTotal = totalCpu;
                prevCpuIdle = idleCpu;
            }
        } catch (Throwable e) {
            closeRaf(cpuRaf);
            cpuRaf = null;
        }
    }

    private void calculateGpuUsageZeroGc() {
        if (!isScreenOn) return;

        try {
            if (gpuRaf == null) {
                gpuRaf = new RandomAccessFile(GPU_BUSY_PATH, "r");
            }
            gpuRaf.seek(0);
            int bytesRead = gpuRaf.read(gpuStatBuffer);
            if (bytesRead <= 0) {
                closeRaf(gpuRaf);
                gpuRaf = new RandomAccessFile(GPU_BUSY_PATH, "r");
                bytesRead = gpuRaf.read(gpuStatBuffer);
            }

            if (bytesRead > 0) {
                parsePos[0] = 0;
                long currentBusy = parseNextLong(gpuStatBuffer, bytesRead, parsePos);
                long currentTotal = parseNextLong(gpuStatBuffer, bytesRead, parsePos);

                if (currentTotal > 0 && currentBusy >= 0) {
                    int gpu = (int) Math.round((currentBusy * 100.0) / currentTotal);
                    cachedGpuPercentage = Math.min(100, Math.max(0, gpu));
                } else {
                    cachedGpuPercentage = 0;
                }
            }
        } catch (Throwable e) {
            closeRaf(gpuRaf);
            gpuRaf = null;
        }
    }

    private synchronized void writeLogRecordToRamBuffer(long timestampMs, int batterySocHundredths, int cpuPct, int gpuPct, byte isCharging, byte isScreenOnByte) {
        if (batterySocHundredths <= 0) {
            return;
        }

        if (loggerBufferPos + 16 > loggerRamBuffer.length) {
            flushRamBufferToDisk();
        }

        int pos = loggerBufferPos;

        // Bytes 0..7: timestampMs (long)
        loggerRamBuffer[pos] = (byte) (timestampMs >>> 56);
        loggerRamBuffer[pos + 1] = (byte) (timestampMs >>> 48);
        loggerRamBuffer[pos + 2] = (byte) (timestampMs >>> 40);
        loggerRamBuffer[pos + 3] = (byte) (timestampMs >>> 32);
        loggerRamBuffer[pos + 4] = (byte) (timestampMs >>> 24);
        loggerRamBuffer[pos + 5] = (byte) (timestampMs >>> 16);
        loggerRamBuffer[pos + 6] = (byte) (timestampMs >>> 8);
        loggerRamBuffer[pos + 7] = (byte) (timestampMs);

        // Bytes 8..9: batterySocHundredths (short)
        loggerRamBuffer[pos + 8] = (byte) (batterySocHundredths >>> 8);
        loggerRamBuffer[pos + 9] = (byte) (batterySocHundredths);

        // Byte 10: cpuPct
        loggerRamBuffer[pos + 10] = (byte) cpuPct;

        // Byte 11: gpuPct
        loggerRamBuffer[pos + 11] = (byte) gpuPct;

        // Byte 12: isCharging
        loggerRamBuffer[pos + 12] = isCharging;

        // Byte 13: isScreenOnByte
        loggerRamBuffer[pos + 13] = isScreenOnByte;

        // Bytes 14..15: reserved (0x0000)
        loggerRamBuffer[pos + 14] = 0;
        loggerRamBuffer[pos + 15] = 0;

        loggerBufferPos += 16;
    }

    private synchronized void flushRamBufferToDisk() {
        if (loggerBufferPos == 0) return;
        FileOutputStream fos = null;
        try {
            String logFilePath = getDailyLogFilePath();
            fos = new FileOutputStream(logFilePath, true);
            fos.write(loggerRamBuffer, 0, loggerBufferPos);
            fos.flush();
        } catch (Throwable ignored) {
        } finally {
            if (fos != null) {
                try { fos.close(); } catch (Throwable ignored) {}
            }
            loggerBufferPos = 0;
            lastFlushTimeMs = System.currentTimeMillis();
        }
    }

    private static long parseNextLong(byte[] buf, int limit, int[] pos) {
        int i = pos[0];
        while (i < limit && buf[i] <= ' ' && buf[i] != '\n' && buf[i] != '\r') {
            i++;
        }
        if (i >= limit || buf[i] == '\n' || buf[i] == '\r') {
            pos[0] = i;
            return 0;
        }
        long val = 0;
        while (i < limit && buf[i] >= '0' && buf[i] <= '9') {
            val = val * 10 + (buf[i] - '0');
            i++;
        }
        pos[0] = i;
        return val;
    }

    private void registerAndImmediatelyUpdate(View root) {
        if (root == null) return;

        if (root instanceof TextView) {
            TextView tv = (TextView) root;
            String resName = "NO_ID";
            int intId = tv.getId();
            try {
                if (intId != View.NO_ID && tv.getResources() != null) {
                    resName = tv.getResources().getResourceEntryName(intId);
                }
            } catch (Throwable ignored) {}

            if ("battery_percentage_view".equals(resName)) {
                if (!activeTextViewSet.contains(tv)) {
                    activeTextViewSet.add(tv);
                    try {
                        tv.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_NONE);
                        tv.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                    } catch (Throwable ignored) {}
                }
                applyTextToView(tv, cachedDecimalPercentage);
            }
        } else if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            int count = group.getChildCount();
            for (int i = 0; i < count; i++) {
                registerAndImmediatelyUpdate(group.getChildAt(i));
            }
        }
    }

    private void unregisterView(View root) {
        if (root == null) return;

        if (root instanceof TextView) {
            TextView tv = (TextView) root;
            activeTextViewSet.remove(tv);
        } else if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            int count = group.getChildCount();
            for (int i = 0; i < count; i++) {
                unregisterView(group.getChildAt(i));
            }
        }
    }

    private void updateBatteryTextViewsInstant(View root) {
        if (root == null || cachedDecimalPercentage == null) return;

        if (root instanceof TextView) {
            TextView tv = (TextView) root;
            applyTextToView(tv, cachedDecimalPercentage);
        } else if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            int count = group.getChildCount();
            for (int i = 0; i < count; i++) {
                updateBatteryTextViewsInstant(group.getChildAt(i));
            }
        }
    }

    private void updateAllRegisteredViews(final String newDecimal) {
        if (activeTextViewSet.isEmpty() || newDecimal == null) return;

        synchronized (activeTextViewSet) {
            Iterator<TextView> iterator = activeTextViewSet.iterator();
            while (iterator.hasNext()) {
                final TextView tv = iterator.next();
                if (!tv.isAttachedToWindow()) {
                    iterator.remove();
                    continue;
                }

                if (!tv.isShown()) {
                    continue;
                }

                tv.post(new Runnable() {
                    @Override
                    public void run() {
                        applyTextToView(tv, newDecimal);
                    }
                });
            }
        }
    }

    private void applyTextToView(TextView tv, String targetText) {
        if (tv == null || targetText == null) return;
        CharSequence before = tv.getText();
        if (before == null || !before.toString().equals(targetText)) {
            if (before != null && before.length() == targetText.length() && before instanceof android.text.SpannableStringBuilder) {
                android.text.SpannableStringBuilder ssb = (android.text.SpannableStringBuilder) before;
                ssb.replace(0, ssb.length(), targetText);
                tv.invalidate();
            } else {
                tv.setText(targetText, TextView.BufferType.SPANNABLE);
            }
        }
    }

    private static int cachedOffset08 = -1;
    private static int cachedOffset0C = -1;
    private static int cachedOffset10 = -1;
    private static int cachedOffset12 = -1;

    private static int parseHexShortAtKeyCached(byte[] buffer, int limit, byte[] key, int keyIndex) {
        int keyLen = key.length;
        int cachedOffset = -1;
        if (keyIndex == 0) cachedOffset = cachedOffset08;
        else if (keyIndex == 1) cachedOffset = cachedOffset0C;
        else if (keyIndex == 2) cachedOffset = cachedOffset10;
        else if (keyIndex == 3) cachedOffset = cachedOffset12;

        if (cachedOffset >= 0 && cachedOffset <= limit - keyLen - 5) {
            boolean match = true;
            for (int j = 0; j < keyLen; j++) {
                if (buffer[cachedOffset + j] != key[j]) {
                    match = false;
                    break;
                }
            }
            if (match) {
                int p = cachedOffset + keyLen;
                int b1 = parseHexByte(buffer, p);
                int b2 = parseHexByte(buffer, p + 3);
                if (b1 != -1 && b2 != -1) {
                    return (b2 << 8) | b1;
                }
            }
        }

        for (int i = 0; i <= limit - keyLen - 5; i++) {
            boolean match = true;
            for (int j = 0; j < keyLen; j++) {
                if (buffer[i + j] != key[j]) {
                    match = false;
                    break;
                }
            }
            if (match) {
                if (keyIndex == 0) cachedOffset08 = i;
                else if (keyIndex == 1) cachedOffset0C = i;
                else if (keyIndex == 2) cachedOffset10 = i;
                else if (keyIndex == 3) cachedOffset12 = i;

                int p = i + keyLen;
                int b1 = parseHexByte(buffer, p);
                int b2 = parseHexByte(buffer, p + 3);
                if (b1 != -1 && b2 != -1) {
                    return (b2 << 8) | b1;
                }
            }
        }
        return -1;
    }

    private static int parseHexByte(byte[] buffer, int pos) {
        int h1 = hexCharToInt(buffer[pos]);
        int h2 = hexCharToInt(buffer[pos + 1]);
        if (h1 == -1 || h2 == -1) return -1;
        return (h1 << 4) | h2;
    }

    private static int hexCharToInt(byte b) {
        if (b >= '0' && b <= '9') return b - '0';
        if (b >= 'a' && b <= 'f') return b - 'a' + 10;
        if (b >= 'A' && b <= 'F') return b - 'A' + 10;
        return -1;
    }

    private String readGaugeInfoFromSysfs() {
        try {
            if (batteryRaf == null) {
                batteryRaf = new RandomAccessFile(GAUGE_INFO_PATH, "r");
            }
            batteryRaf.seek(0);
            int bytesRead = batteryRaf.read(batteryStatBuffer);
            if (bytesRead <= 0) return null;

            int rawVoltage = parseHexShortAtKeyCached(batteryStatBuffer, bytesRead, KEY_08, 0);
            int rawCurrentUnsigned = parseHexShortAtKeyCached(batteryStatBuffer, bytesRead, KEY_0C, 1);
            int rem = parseHexShortAtKeyCached(batteryStatBuffer, bytesRead, KEY_10, 2);
            int full = parseHexShortAtKeyCached(batteryStatBuffer, bytesRead, KEY_12, 3);

            if (full <= 0 || rem < 0) return null;

            short currentMa = (short) rawCurrentUnsigned;

            if (rawVoltage > 0) {
                int powerHundredths = (int) Math.round((rawVoltage * (double) Math.abs(currentMa)) / 10000.0);
                powerHundredths = Math.min(2000, Math.max(0, powerHundredths));
                cachedPowerWattsString = POWER_WATTS_STRINGS[powerHundredths];
            } else {
                cachedPowerWattsString = null;
            }

            long nowMs = System.currentTimeMillis();

            if (rem != lastRemMah || full != lastFullMah) {
                accumMah = 0.0;
                lastRemMah = rem;
                lastFullMah = full;
                hasPrevSample = false;
            }

            if (hasPrevSample && lastGaugeSampleTimeMs > 0) {
                double deltaSeconds = (nowMs - lastGaugeSampleTimeMs) / 1000.0;
                if (deltaSeconds > 0 && deltaSeconds < 30.0) {
                    double avgCurrentMa = (prevCurrentMa + currentMa) / 2.0;
                    accumMah += (avgCurrentMa * deltaSeconds) / 3600.0;
                }
            }
            lastGaugeSampleTimeMs = nowMs;
            prevCurrentMa = currentMa;
            hasPrevSample = true;

            double decimalSoc;
            if (useSmoothEstimate) {
                double estMah = rem + accumMah;
                if (estMah < 0) estMah = 0;
                decimalSoc = (estMah * 100.0) / full;
                if (decimalSoc > 100.0) decimalSoc = 100.0;
                if (decimalSoc < 0.0) decimalSoc = 0.0;
            } else {
                decimalSoc = (rem * 100.0) / full;
                if (decimalSoc > 100.0) decimalSoc = 100.0;
            }

            int hundredths = (int) Math.round(decimalSoc * 100.0);
            hundredths = Math.min(10000, Math.max(0, hundredths));
            cachedBatterySocHundredths = (short) hundredths;
            return BATTERY_DECIMAL_STRINGS[hundredths];
        } catch (Throwable e) {
            closeRaf(batteryRaf);
            batteryRaf = null;
            return null;
        }
    }
}
