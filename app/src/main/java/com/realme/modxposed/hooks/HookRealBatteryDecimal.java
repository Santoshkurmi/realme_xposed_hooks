package com.realme.modxposed.hooks;

import android.os.Handler;
import android.os.HandlerThread;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.realme.modxposed.IXposedHookLoadPackage;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookRealBatteryDecimal implements IXposedHookLoadPackage {

    private static final String GAUGE_INFO_PATH = "/sys/devices/virtual/oplus_chg/battery/gauge_info";
    private static final String PROC_STAT_PATH = "/proc/stat";
    private static final long BATTERY_POLL_INTERVAL_MS = 5000; // 5 Seconds battery background poll
    private static final long CPU_POLL_INTERVAL_MS = 1000;     // 1 Second CPU background poll

    // Target Classes
    private static final String STAT_BATTERY_VIEW_CLASS = "com.oplus.systemui.statusbar.pipeline.battery.ui.view.StatBatteryMeterView";
    private static final String HORIZONTAL_DRAWABLE_CLASS = "com.oplus.systemui.statusbar.pipeline.battery.ui.drawable.HorizontalBatteryContentDrawable";

    private static volatile String cachedRawBatterySoc = null;
    private static volatile int cachedCpuPercentage = 0;
    private static volatile String cachedDecimalPercentage = null;

    private Handler backgroundBatteryHandler;
    private Handler backgroundCpuHandler;
    private final Set<TextView> activeTextViewSet = Collections.synchronizedSet(new HashSet<TextView>());

    private long prevCpuTotal = 0;
    private long prevCpuIdle = 0;
    private boolean isFirstCpuSample = true;

    @Override
    public void init(XC_LoadPackage.LoadPackageParam lpparam) {
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

            // Hook onFinishInflate
            XposedHelpers.findAndHookMethod(clazz, "onFinishInflate", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    View view = (View) param.thisObject;
                    registerAndImmediatelyUpdate(view);
                }
            });

            // Hook onAttachedToWindow
            XposedHelpers.findAndHookMethod(clazz, "onAttachedToWindow", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    View view = (View) param.thisObject;
                    registerAndImmediatelyUpdate(view);
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

    private void startBackgroundPolling() {
        // Battery Polling Task
        backgroundBatteryHandler.post(new Runnable() {
            @Override
            public void run() {
                String newDecimal = readGaugeInfoFromSysfs();
                if (newDecimal != null) {
                    cachedRawBatterySoc = newDecimal;
                    updateCombinedPercentageAndNotify();
                }
                backgroundBatteryHandler.postDelayed(this, BATTERY_POLL_INTERVAL_MS);
            }
        });

        // CPU Polling Task (Every 1 Second)
        backgroundCpuHandler.post(new Runnable() {
            @Override
            public void run() {
                calculateCpuUsage();
                updateCombinedPercentageAndNotify();
                backgroundCpuHandler.postDelayed(this, CPU_POLL_INTERVAL_MS);
            }
        });
    }

    private synchronized void updateCombinedPercentageAndNotify() {
        if (cachedRawBatterySoc != null) {
            String cpuStr = (cachedCpuPercentage < 10) ? ("\u2007" + cachedCpuPercentage) : String.valueOf(cachedCpuPercentage);
            String combined = cachedRawBatterySoc + " " + cpuStr + "%";
            cachedDecimalPercentage = combined;
            updateAllRegisteredViews(combined);
        }
    }

    private void calculateCpuUsage() {
        try (BufferedReader reader = new BufferedReader(new FileReader(PROC_STAT_PATH))) {
            String line = reader.readLine();
            if (line != null && line.startsWith("cpu")) {
                String[] tokens = line.trim().split("\\s+");
                if (tokens.length >= 5) {
                    long user = Long.parseLong(tokens[1]);
                    long nice = Long.parseLong(tokens[2]);
                    long system = Long.parseLong(tokens[3]);
                    long idle = Long.parseLong(tokens[4]);
                    long iowait = tokens.length > 5 ? Long.parseLong(tokens[5]) : 0;
                    long irq = tokens.length > 6 ? Long.parseLong(tokens[6]) : 0;
                    long softirq = tokens.length > 7 ? Long.parseLong(tokens[7]) : 0;
                    long steal = tokens.length > 8 ? Long.parseLong(tokens[8]) : 0;

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
                            // Bounded to max 99% as 100% is capped to 99%
                            cachedCpuPercentage = Math.min(99, Math.max(0, cpu));
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    private void registerAndImmediatelyUpdate(View root) {
        if (root == null) return;

        if (root instanceof TextView) {
            TextView tv = (TextView) root;
            if (!activeTextViewSet.contains(tv)) {
                activeTextViewSet.add(tv);
            }
            applyTextToView(tv, cachedDecimalPercentage);
        } else if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            int count = group.getChildCount();
            for (int i = 0; i < count; i++) {
                registerAndImmediatelyUpdate(group.getChildAt(i));
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
        if (activeTextViewSet.isEmpty()) return;

        synchronized (activeTextViewSet) {
            Iterator<TextView> iterator = activeTextViewSet.iterator();
            while (iterator.hasNext()) {
                final TextView tv = iterator.next();
                if (!tv.isAttachedToWindow()) {
                    iterator.remove();
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

