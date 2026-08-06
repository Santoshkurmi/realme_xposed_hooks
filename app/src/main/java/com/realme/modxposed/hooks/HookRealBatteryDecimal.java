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
    private static final long POLL_INTERVAL_MS = 5000; // 5 Seconds background poll

    // Global Volatile Cached Decimal String
    private static volatile String cachedDecimalPercentage = null;

    private Handler backgroundHandler;
    private final Set<TextView> activeTextViewSet = Collections.synchronizedSet(new HashSet<TextView>());

    private void logBoth(String msg) {
        XposedBridge.log("[" + TAG + "] " + msg);
        Log.d(TAG, msg);
    }

    @Override
    public void init(XC_LoadPackage.LoadPackageParam lpparam) {
        logBoth(">>> INIT STARTED for package: " + lpparam.packageName);

        // 1. Start Background Polling Thread (Reads sysfs every 5s into cachedDecimalPercentage)
        try {
            HandlerThread thread = new HandlerThread("BatteryDecimalThread");
            thread.start();
            backgroundHandler = new Handler(thread.getLooper());
            logBoth("Background HandlerThread started successfully.");
            
            startBackgroundPolling();
        } catch (Throwable t) {
            logBoth("ERROR starting HandlerThread: " + t.getMessage());
        }

        // Target classes for OxygenOS / ColorOS 14/15/16 SystemUI
        String[] targetClasses = {
            "com.oplus.systemui.statusbar.pipeline.battery.ui.view.StatBatteryMeterView",
            "com.oplus.systemui.statusbar.pipeline.battery.StatBatteryMeterViewController",
            "com.oplus.systemui.statusbar.pipeline.battery.ui.view.BaseBatteryMeterView",
            "com.oplusos.systemui.statusbar.phone.OplusBatteryMeterView",
            "com.android.systemui.statusbar.phone.BatteryMeterView"
        };

        for (String className : targetClasses) {
            try {
                Class<?> clazz = XposedHelpers.findClass(className, lpparam.classLoader);
                logBoth("FOUND & HOOKING CLASS: " + className);

                // Hook onFinishInflate
                try {
                    XposedHelpers.findAndHookMethod(clazz, "onFinishInflate", new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            View view = (View) param.thisObject;
                            logBoth("EVENT: onFinishInflate in " + param.thisObject.getClass().getName());
                            registerAndImmediatelyUpdate(view);
                        }
                    });
                    logBoth("Hooked onFinishInflate in " + className);
                } catch (Throwable t) {
                    logBoth("Could not hook onFinishInflate in " + className + ": " + t.getMessage());
                }

                // Hook onAttachedToWindow
                try {
                    XposedHelpers.findAndHookMethod(clazz, "onAttachedToWindow", new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            View view = (View) param.thisObject;
                            logBoth("EVENT: onAttachedToWindow in " + param.thisObject.getClass().getName());
                            registerAndImmediatelyUpdate(view);
                        }
                    });
                    logBoth("Hooked onAttachedToWindow in " + className);
                } catch (Throwable t) {
                    logBoth("Could not hook onAttachedToWindow in " + className + ": " + t.getMessage());
                }

            } catch (Throwable t) {
                logBoth("CLASS NOT FOUND: " + className + " (" + t.getMessage() + ")");
            }
        }

        // Hook Drawables (Horizontal & Vertical Content Drawables)
        String[] drawableClasses = {
            "com.oplus.systemui.statusbar.pipeline.battery.ui.drawable.HorizontalBatteryContentDrawable",
            "com.oplus.systemui.statusbar.pipeline.battery.ui.drawable.VerticalBatteryContentDrawable"
        };

        for (String drawClass : drawableClasses) {
            try {
                Class<?> clazz = XposedHelpers.findClass(drawClass, lpparam.classLoader);
                logBoth("FOUND DRAWABLE CLASS: " + drawClass);

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
                logBoth("Hooked draw in " + drawClass);
            } catch (Throwable t) {
                logBoth("DRAWABLE NOT FOUND: " + drawClass + " (" + t.getMessage() + ")");
            }
        }

        logBoth("<<< INIT COMPLETED for package: " + lpparam.packageName);
    }

    private void startBackgroundPolling() {
        backgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                // Read hardware file on background thread
                String newDecimal = readGaugeInfoFromSysfs();
                if (newDecimal != null) {
                    cachedDecimalPercentage = newDecimal;
                    logBoth("POLL: Updated cachedDecimalPercentage to: " + newDecimal);
                    // Broadcast update to all registered views
                    updateAllRegisteredViews(newDecimal);
                }
                backgroundHandler.postDelayed(this, POLL_INTERVAL_MS);
            }
        });
        logBoth("Background Polling Loop Started (Every 5 seconds).");
    }

    private void registerAndImmediatelyUpdate(View root) {
        if (root == null) return;

        if (root instanceof TextView) {
            TextView tv = (TextView) root;
            if (!activeTextViewSet.contains(tv)) {
                activeTextViewSet.add(tv);
                logBoth("REGISTERED TextView: " + tv.getClass().getName() + " (Total Views: " + activeTextViewSet.size() + ")");
            }
            // Immediately apply cached value
            applyTextToView(tv, cachedDecimalPercentage);
        } else if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                registerAndImmediatelyUpdate(group.getChildAt(i));
            }
        }
    }

    private void updateAllRegisteredViews(final String newDecimal) {
        if (activeTextViewSet.isEmpty()) {
            logBoth("No active TextViews registered yet.");
            return;
        }

        synchronized (activeTextViewSet) {
            Iterator<TextView> iterator = activeTextViewSet.iterator();
            while (iterator.hasNext()) {
                TextView tv = iterator.next();
                if (!tv.isAttachedToWindow()) {
                    logBoth("Cleaning up detached view: " + tv.getClass().getName());
                    iterator.remove();
                    continue;
                }
                applyTextToView(tv, newDecimal);
            }
        }
    }

    private void applyTextToView(final TextView tv, final String textToApply) {
        if (tv == null || textToApply == null) return;

        tv.post(new Runnable() {
            @Override
            public void run() {
                CharSequence before = tv.getText();
                if (before == null || !before.toString().equals(textToApply)) {
                    tv.setText(textToApply);
                    logBoth("APPLIED TEXT: '" + before + "' -> '" + textToApply + "' on View: " + tv.getClass().getName());
                }
            }
        });
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
            logBoth("EXCEPTION reading sysfs: " + e.getMessage());
            return null;
        }
    }
}
