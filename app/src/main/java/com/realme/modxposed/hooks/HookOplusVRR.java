package com.realme.modxposed.hooks;

import android.os.Handler;
import android.os.IBinder;
import com.realme.modxposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookOplusVRR implements IXposedHookLoadPackage {

    private static final String TAG = "[ModXposedVRR]";
    private static final int REFRESH_RATE_ID_120HZ = 3;

    private static void log(String msg) {
        XposedBridge.log(TAG + " " + msg);
    }

    private static void log(String msg, Throwable t) {
        XposedBridge.log(TAG + " " + msg + ": " + (t != null ? t.getMessage() : "null"));
    }

    @Override
    public void init(XC_LoadPackage.LoadPackageParam lpparam) {
        // VRR services run inside system_server (package "android")
        if (!"android".equals(lpparam.packageName)) {
            return;
        }

        log("Initializing Oplus VRR short-circuit & 120Hz lock hooks...");

        ClassLoader classLoader = lpparam.classLoader;

        // 1. Disable VRR & ADFR feature flags & video low freq in OPlusRefreshRateManager
        try {
            Class<?> managerClass = XposedHelpers.findClassIfExists("com.oplus.vrr.OPlusRefreshRateManager", classLoader);
            if (managerClass != null) {
                XposedHelpers.findAndHookMethod(managerClass, "hasVRRFeature", XC_MethodReplacement.returnConstant(false));
                XposedHelpers.findAndHookMethod(managerClass, "hasADFRFeature", XC_MethodReplacement.returnConstant(false));
                XposedHelpers.findAndHookMethod(managerClass, "getRefreshRatePolicy", float.class, XC_MethodReplacement.returnConstant(0));
                XposedHelpers.findAndHookMethod(managerClass, "setLowFreqVideo", boolean.class, XC_MethodReplacement.DO_NOTHING);
                
                // Prevent service calls from sending binder messages
                XposedHelpers.findAndHookMethod(managerClass, "notifyBrightnessChange", int.class, long.class, float.class, float.class, XC_MethodReplacement.DO_NOTHING);
                XposedHelpers.findAndHookMethod(managerClass, "notifyNitsChange", float.class, XC_MethodReplacement.DO_NOTHING);
                XposedHelpers.findAndHookMethod(managerClass, "screenStateChange", int.class, long.class, int.class, XC_MethodReplacement.DO_NOTHING);
                XposedHelpers.findAndHookMethod(managerClass, "setRefreshRatePolicy", int.class, float.class, int.class, boolean.class, XC_MethodReplacement.DO_NOTHING);

                log("Successfully hooked com.oplus.vrr.OPlusRefreshRateManager");
            }
        } catch (Throwable t) {
            log("Failed hooking OPlusRefreshRateManager", t);
        }

        // 2. Prevent LocalDisplayAdapter from using OPlusVRRThread handler
        try {
            Class<?> adapterClass = XposedHelpers.findClassIfExists("com.android.server.display.LocalDisplayAdapterExtImpl", classLoader);
            if (adapterClass != null) {
                XposedHelpers.findAndHookMethod(adapterClass, "getOPlusRefreshRateHandler", Handler.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        Handler defHandler = (Handler) param.args[0];
                        param.setResult(defHandler);
                    }
                });
                log("Successfully hooked LocalDisplayAdapterExtImpl.getOPlusRefreshRateHandler");
            }
        } catch (Throwable t) {
            log("Failed hooking LocalDisplayAdapterExtImpl", t);
        }

        // 3. Short-circuit DisplayModeDirector VRR policy & ADFR checks
        try {
            Class<?> directorClass = XposedHelpers.findClassIfExists("com.android.server.display.DisplayModeDirectorExtImpl", classLoader);
            if (directorClass != null) {
                XposedHelpers.findAndHookMethod(directorClass, "isAdfrEnabled", XC_MethodReplacement.returnConstant(false));
                XposedHelpers.findAndHookMethod(directorClass, "getVrrPolicy", float.class, XC_MethodReplacement.returnConstant(0));
                log("Successfully hooked DisplayModeDirectorExtImpl");
            }
        } catch (Throwable t) {
            log("Failed hooking DisplayModeDirectorExtImpl", t);
        }

        // 4. Short-circuit SurfaceFlinger Event registration in OplusInfoMonitor
        try {
            Class<?> monitorClass = XposedHelpers.findClassIfExists("com.oplus.display.OplusInfoMonitor", classLoader);
            if (monitorClass != null) {
                XposedHelpers.findAndHookMethod(monitorClass, "registerSfEvent", 
                        XposedHelpers.findClass("com.oplus.display.OplusInfoMonitor$OplusInfoMonitorListener", classLoader),
                        int.class, String.class, XC_MethodReplacement.DO_NOTHING);
                log("Successfully hooked OplusInfoMonitor.registerSfEvent");
            }
        } catch (Throwable t) {
            log("Failed hooking OplusInfoMonitor", t);
        }

        // 5. Force OplusRefreshRateCore to return rate ID 3 (120Hz)
        try {
            Class<?> coreClass = XposedHelpers.findClassIfExists("com.android.server.wm.OplusRefreshRateCore", classLoader);
            if (coreClass != null) {
                XposedHelpers.findAndHookMethod(coreClass, "getFinalDisplayRefreshRateIdLocked", boolean.class, XC_MethodReplacement.returnConstant(REFRESH_RATE_ID_120HZ));
                XposedHelpers.findAndHookMethod(coreClass, "getMEMCRefreshRate", XC_MethodReplacement.returnConstant(120.0f));
                log("Successfully hooked OplusRefreshRateCore");
            }
        } catch (Throwable t) {
            log("Failed hooking OplusRefreshRateCore", t);
        }

        // 6. Disable IME keyboard & low-freq video refresh rate drops in OplusRefreshRateConfigs
        try {
            Class<?> configsClass = XposedHelpers.findClassIfExists("com.android.server.wm.OplusRefreshRateConfigs", classLoader);
            if (configsClass != null) {
                XposedHelpers.findAndHookMethod(configsClass, "allowInputMethodLowRate", XC_MethodReplacement.returnConstant(false));
                XposedHelpers.findAndHookMethod(configsClass, "allowVoiceSceneLowRate", XC_MethodReplacement.returnConstant(false));
                XposedHelpers.findAndHookMethod(configsClass, "isLowRateDisplay", String.class, XC_MethodReplacement.returnConstant(false));
                XposedHelpers.findAndHookMethod(configsClass, "enableNearFlashAppLowRate", XC_MethodReplacement.returnConstant(false));
                XposedHelpers.findAndHookMethod(configsClass, "getDefaultRateId", int.class, XC_MethodReplacement.returnConstant(REFRESH_RATE_ID_120HZ));
                log("Successfully hooked OplusRefreshRateConfigs (IME & Video 60Hz disable)");
            }
        } catch (Throwable t) {
            log("Failed hooking OplusRefreshRateConfigs", t);
        }

        // 7. Force OplusRefreshRateUtils to resolve 120Hz ID (3)
        try {
            Class<?> utilsClass = XposedHelpers.findClassIfExists("com.android.server.wm.OplusRefreshRateUtils", classLoader);
            if (utilsClass != null) {
                XposedHelpers.findAndHookMethod(utilsClass, "getDefaultRefreshRateId", int.class, XC_MethodReplacement.returnConstant(REFRESH_RATE_ID_120HZ));
                log("Successfully hooked OplusRefreshRateUtils.getDefaultRefreshRateId");
            }
        } catch (Throwable t) {
            log("Failed hooking OplusRefreshRateUtils", t);
        }

        // 8. Override Window Refresh Rate in OplusDisplayModeService to force 120Hz ID (3)
        try {
            Class<?> displayModeService = XposedHelpers.findClassIfExists("com.android.server.wm.OplusDisplayModeService", classLoader);
            if (displayModeService != null) {
                XposedHelpers.findAndHookMethod(displayModeService, "overrideWindowRefreshRate", IBinder.class, int.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        param.args[1] = REFRESH_RATE_ID_120HZ;
                    }
                });
                XposedHelpers.findAndHookMethod(displayModeService, "getAppOverrideRefreshRate", String.class, int.class, XC_MethodReplacement.returnConstant(REFRESH_RATE_ID_120HZ));
                log("Successfully hooked OplusDisplayModeService.overrideWindowRefreshRate to 120Hz");
            }
        } catch (Throwable t) {
            log("Failed hooking OplusDisplayModeService", t);
        }

        // 9. Prevent OPlusVRRThread from spawning in kernel thread list
        try {
            Class<?> vrrThreadClass = XposedHelpers.findClassIfExists("com.oplus.vrr.OPlusVRRThread", classLoader);
            if (vrrThreadClass != null) {
                XposedHelpers.findAndHookMethod(vrrThreadClass, "ensureThreadLocked", XC_MethodReplacement.DO_NOTHING);
                log("Successfully hooked OPlusVRRThread.ensureThreadLocked");
            }
        } catch (Throwable t) {
            log("Failed hooking OPlusVRRThread", t);
        }
    }
}
