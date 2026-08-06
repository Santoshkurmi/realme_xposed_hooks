package com.realme.modxposed.hooks;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
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
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookRealBatteryDecimal implements IXposedHookLoadPackage {

    private static final String TAG = "RealBatteryDecimal";
    private static final String GAUGE_INFO_PATH = "/sys/devices/virtual/oplus_chg/battery/gauge_info";
    private static final long POLL_INTERVAL_MS = 10000; // 10 Seconds background poll

    // Winning OxygenOS SystemUI Classes
    private static final String STAT_BATTERY_VIEW_CLASS = "com.oplus.systemui.statusbar.pipeline.battery.ui.view.StatBatteryMeterView";
    private static final String HORIZONTAL_DRAWABLE_CLASS = "com.oplus.systemui.statusbar.pipeline.battery.ui.drawable.HorizontalBatteryContentDrawable";

    private static volatile String cachedDecimalPercentage = null;

    private Handler backgroundHandler;
    private final Set<TextView> activeTextViewSet = Collections.synchronizedSet(new HashSet<TextView>());

    @Override
    public void init(XC_LoadPackage.LoadPackageParam lpparam) {
        // 1. Start Background Polling Thread
        try {
            HandlerThread thread = new HandlerThread("BatteryDecimalThread");
            thread.start();
            backgroundHandler = new Handler(thread.getLooper());
            startBackgroundPolling();
        } catch (Throwable ignored) {}

        // Hook Winning Target 1: StatBatteryMeterView
        try {
            Class<?> clazz = XposedHelpers.findClass(STAT_BATTERY_VIEW_CLASS, lpparam.classLoader);

            // Hook onFinishInflate
            XposedHelpers.findAndHookMethod(clazz, "onFinishInflate", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    View view = (View) param.thisObject;
                    registerAndImmediatelyUpdate(view, "StatBatteryMeterView#onFinishInflate");
                }
            });

            // Hook onAttachedToWindow
            XposedHelpers.findAndHookMethod(clazz, "onAttachedToWindow", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    View view = (View) param.thisObject;
                    registerAndImmediatelyUpdate(view, "StatBatteryMeterView#onAttachedToWindow");
                }
            });

        } catch (Throwable ignored) {}

        // Hook Winning Target 2: HorizontalBatteryContentDrawable
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
        backgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                String newDecimal = readGaugeInfoFromSysfs();
                if (newDecimal != null) {
                    cachedDecimalPercentage = newDecimal;
                    updateAllRegisteredViews(newDecimal);
                }
                backgroundHandler.postDelayed(this, POLL_INTERVAL_MS);
            }
        });
    }

    private void registerAndImmediatelyUpdate(View root, String sourceHook) {
        if (root == null) return;

        if (root instanceof TextView) {
            TextView tv = (TextView) root;
            if (!activeTextViewSet.contains(tv)) {
                activeTextViewSet.add(tv);
            }
            
            // Direct Immediate Text Apply on UI thread
            String textToApply = cachedDecimalPercentage;
            if (textToApply != null) {
                CharSequence before = tv.getText();
                if (before == null || !before.toString().equals(textToApply)) {
                    tv.setText(textToApply);
                }
            }
        } else if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            int count = group.getChildCount();
            for (int i = 0; i < count; i++) {
                registerAndImmediatelyUpdate(group.getChildAt(i), sourceHook);
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
                        CharSequence before = tv.getText();
                        if (before == null || !before.toString().equals(newDecimal)) {
                            tv.setText(newDecimal);
                        }
                    }
                });
            }
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
