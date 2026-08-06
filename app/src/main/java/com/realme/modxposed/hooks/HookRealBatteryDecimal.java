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
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.WeakHashMap;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookRealBatteryDecimal implements IXposedHookLoadPackage {

    private static final String TAG = "RealBatteryDecimal";
    private static final String GAUGE_INFO_PATH = "/sys/devices/virtual/oplus_chg/battery/gauge_info";
    private static final long REFRESH_INTERVAL_MS = 5000;

    private Handler backgroundHandler;
    private final Set<WeakReference<TextView>> registeredTextViews = Collections.newSetFromMap(new WeakHashMap<WeakReference<TextView>, Boolean>());
    private String lastAppliedDecimal = "";
    private boolean isTimerRunning = false;
    private long tickCounter = 0;

    private void logBoth(String msg) {
        XposedBridge.log("[" + TAG + "] " + msg);
        Log.d(TAG, msg);
    }

    @Override
    public void init(XC_LoadPackage.LoadPackageParam lpparam) {
        logBoth(">>> INIT STARTED for package: " + lpparam.packageName);

        try {
            HandlerThread thread = new HandlerThread("BatteryDecimalThread");
            thread.start();
            backgroundHandler = new Handler(thread.getLooper());
            logBoth("Background HandlerThread started successfully.");
        } catch (Throwable t) {
            logBoth("ERROR starting HandlerThread: " + t.getMessage());
        }

        String[] targetClasses = {
            "com.oplus.systemui.statusbar.pipeline.battery.ui.view.StatBatteryMeterView",
            "com.oplus.systemui.statusbar.pipeline.battery.StatBatteryMeterViewController",
            "com.oplus.systemui.statusbar.pipeline.battery.ui.view.BaseBatteryMeterView",
            "com.oplusos.systemui.statusbar.phone.OplusBatteryMeterView",
            "com.android.systemui.statusbar.phone.BatteryMeterView"
        };

        for (String className : targetClasses) {
            logBoth("Searching class in ClassLoader: " + className);
            try {
                Class<?> clazz = XposedHelpers.findClass(className, lpparam.classLoader);
                logBoth("FOUND & HOOKING CLASS: " + className);

                try {
                    XposedHelpers.findAndHookMethod(clazz, "onFinishInflate", new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            View view = (View) param.thisObject;
                            logBoth("EVENT: onFinishInflate triggered in class " + param.thisObject.getClass().getName() + " (ViewID: " + view.getId() + ")");
                            registerViewsFromRoot(view, param.thisObject.getClass().getName());
                        }
                    });
                    logBoth("Hooked onFinishInflate in " + className);
                } catch (Throwable t) {
                    logBoth("Could not hook onFinishInflate in " + className + ": " + t.getMessage());
                }

                try {
                    XposedHelpers.findAndHookMethod(clazz, "onAttachedToWindow", new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            View view = (View) param.thisObject;
                            logBoth("EVENT: onAttachedToWindow triggered in class " + param.thisObject.getClass().getName() + " (ViewID: " + view.getId() + ")");
                            registerViewsFromRoot(view, param.thisObject.getClass().getName());
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

        startMasterTimer();
        logBoth("<<< INIT COMPLETED for package: " + lpparam.packageName);
    }

    private synchronized void startMasterTimer() {
        if (isTimerRunning) {
            logBoth("Master Timer is already running. Skipping duplicate start.");
            return;
        }
        isTimerRunning = true;

        backgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                tickCounter++;
                logBoth("--- MASTER TIMER TICK #" + tickCounter + " (Active Registered Views: " + registeredTextViews.size() + ") ---");
                updateAllRegisteredViews();
                backgroundHandler.postDelayed(this, REFRESH_INTERVAL_MS);
            }
        });
        logBoth("Single Master Background Refresh Timer Loop Successfully Started.");
    }

    private void registerViewsFromRoot(View root, String sourceClassName) {
        if (root == null) {
            logBoth("registerViewsFromRoot called with NULL root from " + sourceClassName);
            return;
        }

        logBoth("Traversing view hierarchy from " + sourceClassName + ": " + root.getClass().getName() + " (Hash: " + System.identityHashCode(root) + ")");

        if (root instanceof TextView) {
            TextView tv = (TextView) root;
            addTextViewIfNew(tv, sourceClassName);
        } else if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            int count = group.getChildCount();
            logBoth("ViewGroup " + root.getClass().getName() + " has " + count + " child views.");
            for (int i = 0; i < count; i++) {
                registerViewsFromRoot(group.getChildAt(i), sourceClassName);
            }
        }
    }

    private void addTextViewIfNew(TextView tv, String sourceClassName) {
        int hash = System.identityHashCode(tv);
        for (WeakReference<TextView> ref : registeredTextViews) {
            if (ref.get() == tv) {
                logBoth("View already registered. Skipping duplicate registration for " + tv.getClass().getName() + " (Hash: " + hash + ")");
                return;
            }
        }

        registeredTextViews.add(new WeakReference<>(tv));
        CharSequence currentText = tv.getText();
        logBoth(">>> REGISTERED NEW TEXTVIEW! Class: " + tv.getClass().getName() + " | SourceClass: " + sourceClassName + " | Hash: " + hash + " | InitialText: '" + currentText + "' | Total Active Registered Views: " + registeredTextViews.size());
    }

    private void updateAllRegisteredViews() {
        if (registeredTextViews.isEmpty()) {
            logBoth("No active TextViews registered. Timer waiting...");
            return;
        }

        final String currentDecimal = getRealBatteryDecimal();
        if (currentDecimal == null) {
            logBoth("getRealBatteryDecimal() returned NULL. Aborting tick update.");
            return;
        }

        if (currentDecimal.equals(lastAppliedDecimal)) {
            logBoth("Decimal unchanged (" + currentDecimal + "). Skipping redraw for all " + registeredTextViews.size() + " views.");
            return;
        }

        logBoth("DECIMAL CHANGED: '" + lastAppliedDecimal + "' -> '" + currentDecimal + "'. Updating " + registeredTextViews.size() + " registered views...");
        lastAppliedDecimal = currentDecimal;

        Iterator<WeakReference<TextView>> iterator = registeredTextViews.iterator();
        int activeCount = 0;
        int cleanedCount = 0;

        while (iterator.hasNext()) {
            WeakReference<TextView> ref = iterator.next();
            final TextView tv = ref.get();
            if (tv == null) {
                iterator.remove();
                cleanedCount++;
                continue;
            }

            activeCount++;
            final int viewHash = System.identityHashCode(tv);
            final String className = tv.getClass().getName();

            tv.post(new Runnable() {
                @Override
                public void run() {
                    CharSequence beforeText = tv.getText();
                    tv.setText(currentDecimal);
                    logBoth("APPLIED TEXT! ViewClass: " + className + " | Hash: " + viewHash + " | Before: '" + beforeText + "' -> After: '" + currentDecimal + "'");
                }
            });
        }

        logBoth("Tick update dispatched to " + activeCount + " active views (Cleaned " + cleanedCount + " dead GC references).");
    }

    private String getRealBatteryDecimal() {
        logBoth("Reading " + GAUGE_INFO_PATH);
        try (BufferedReader reader = new BufferedReader(new FileReader(GAUGE_INFO_PATH))) {
            String info = reader.readLine();
            if (info == null) {
                logBoth("gauge_info read line is NULL");
                return null;
            }

            int idx10 = info.indexOf("0x10=");
            int idx12 = info.indexOf("0x12=");
            if (idx10 == -1 || idx12 == -1) {
                logBoth("gauge_info missing 0x10= or 0x12= indexes");
                return null;
            }

            String hex10_byte1 = info.substring(idx10 + 5, idx10 + 7);
            String hex10_byte2 = info.substring(idx10 + 8, idx10 + 10);
            int rem = Integer.parseInt(hex10_byte2 + hex10_byte1, 16);

            String hex12_byte1 = info.substring(idx12 + 5, idx12 + 7);
            String hex12_byte2 = info.substring(idx12 + 8, idx12 + 10);
            int full = Integer.parseInt(hex12_byte2 + hex12_byte1, 16);

            if (full == 0) return null;

            double decimalSoc = (rem * 100.0) / full;
            String result = String.format(Locale.US, "%.2f%%", decimalSoc);
            logBoth("Parsed rem=" + rem + " mAh, full=" + full + " mAh -> Result: " + result);
            return result;
        } catch (Exception e) {
            logBoth("EXCEPTION in getRealBatteryDecimal: " + e.getMessage());
            return null;
        }
    }
}
