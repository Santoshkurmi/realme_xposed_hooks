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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
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

    private static boolean showCpu = true;
    private static long batteryIntervalMs = 5000;
    private static long cpuIntervalMs = 1000;

    // Target Classes
    private static final String STAT_BATTERY_VIEW_CLASS = "com.oplus.systemui.statusbar.pipeline.battery.ui.view.StatBatteryMeterView";
    private static final String HORIZONTAL_DRAWABLE_CLASS = "com.oplus.systemui.statusbar.pipeline.battery.ui.drawable.HorizontalBatteryContentDrawable";

    // Pre-allocated CPU Strings Table (0..99%) to eliminate runtime String allocations during polling loop
    private static final String[] CPU_STRINGS = new String[100];
    static {
        for (int i = 0; i < 100; i++) {
            String cpuStr = (i < 10) ? ("\u2007" + i) : String.valueOf(i);
            CPU_STRINGS[i] = " " + cpuStr + "%";
        }
    }

    private static volatile String cachedRawBatterySoc = null;
    private static volatile int cachedCpuPercentage = 0;
    private static volatile String cachedDecimalPercentage = null;

    private static volatile boolean isScreenOn = true;
    private static volatile boolean isReceiverRegistered = false;

    private Handler backgroundBatteryHandler;
    private Handler backgroundCpuHandler;
    private final Set<TextView> activeTextViewSet = Collections.synchronizedSet(new HashSet<TextView>());

    private long prevCpuTotal = 0;
    private long prevCpuIdle = 0;
    private boolean isFirstCpuSample = true;

    // Zero-GC buffer for /proc/stat reading
    private final byte[] procStatBuffer = new byte[256];
    private final int[] parsePos = new int[1];

    // Pre-allocated permanent Runnable references to avoid GC heap allocations on every poll cycle
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
            updateCombinedPercentageAndNotify();
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

            // Hook onAttachedToWindow
            XposedHelpers.findAndHookMethod(clazz, "onAttachedToWindow", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    View view = (View) param.thisObject;
                    ensureScreenReceiverRegistered(view.getContext());
                    registerAndImmediatelyUpdate(view);
                }
            });

            // Hook onDetachedFromWindow
            XposedHelpers.findAndHookMethod(clazz, "onDetachedFromWindow", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    View view = (View) param.thisObject;
                    unregisterView(view);
                }
            });

            // Hook dispatchDraw (Charger plug/unplug flicker prevention)
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

                    appContext.registerReceiver(new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context ctx, Intent intent) {
                            if (intent == null || intent.getAction() == null) return;
                            String action = intent.getAction();
                            if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                                isScreenOn = false;
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
    }

    private synchronized void updateCombinedPercentageAndNotify() {
        if (cachedRawBatterySoc != null) {
            String combined = showCpu ? (cachedRawBatterySoc + CPU_STRINGS[cachedCpuPercentage]) : cachedRawBatterySoc;
            if (!combined.equals(cachedDecimalPercentage)) {
                cachedDecimalPercentage = combined;
                updateAllRegisteredViews(combined);
            }
        }
    }

    private void calculateCpuUsageZeroGc() {
        if (!isScreenOn) return;

        FileInputStream fis = null;
        try {
            fis = new FileInputStream(PROC_STAT_PATH);
            int bytesRead = fis.read(procStatBuffer);
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

                long currentIdle = idle + iowait;
                long currentTotal = user + nice + system + idle + iowait + irq + softirq + steal;

                if (isFirstCpuSample) {
                    prevCpuTotal = currentTotal;
                    prevCpuIdle = currentIdle;
                    isFirstCpuSample = false;
                    cachedCpuPercentage = 0;
                } else {
                    long totalDiff = currentTotal - prevCpuTotal;
                    long idleDiff = currentIdle - prevCpuIdle;
                    prevCpuTotal = currentTotal;
                    prevCpuIdle = currentIdle;

                    if (totalDiff > 0) {
                        long busyDiff = totalDiff - idleDiff;
                        int cpu = (int) Math.round((busyDiff * 100.0) / totalDiff);
                        cachedCpuPercentage = Math.min(99, Math.max(0, cpu));
                    }
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (fis != null) {
                try { fis.close(); } catch (Exception ignored) {}
            }
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

            // Target ONLY battery_percentage_view
            if ("battery_percentage_view".equals(resName)) {
                if (!activeTextViewSet.contains(tv)) {
                    activeTextViewSet.add(tv);
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
            tv.setText(targetText);
        }
    }

    private String readGaugeInfoFromSysfs() {
        try (BufferedReader reader = new BufferedReader(new FileReader(GAUGE_INFO_PATH))) {
            String info = reader.readLine();
            if (info == null) return null;

            int idx10 = info.indexOf("0x10=");
            int idx12 = info.indexOf("0x12=");
            if (idx10 == -1 || idx12 == -1) return null;

            String hex10_byte1 = info.substring(idx10 + 5, idx10 + 7);
            String hex10_byte2 = info.substring(idx10 + 8, idx10 + 10);
            int rem = Integer.parseInt(hex10_byte2 + hex10_byte1, 16);

            String hex12_byte1 = info.substring(idx12 + 5, idx12 + 7);
            String hex12_byte2 = info.substring(idx12 + 8, idx12 + 10);
            int full = Integer.parseInt(hex12_byte2 + hex12_byte1, 16);

            if (full == 0) return null;

            double decimalSoc = (rem * 100.0) / full;
            return String.format(Locale.US, "%.2f%%", decimalSoc);
        } catch (Exception e) {
            return null;
        }
    }
}
