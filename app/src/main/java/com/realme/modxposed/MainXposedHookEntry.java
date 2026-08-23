package com.realme.modxposed;

import com.realme.modxposed.hooks.GestureNavigationView;
import com.realme.modxposed.hooks.HamroCsit;
import com.realme.modxposed.hooks.HookClock;
import com.realme.modxposed.hooks.HookOplusVRR;
import com.realme.modxposed.hooks.HookRealBatteryDecimal;
import com.realme.modxposed.hooks.LauncherAnimationHook;
import com.realme.modxposed.hooks.Siddha;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainXposedHookEntry implements IXposedHookLoadPackage, IXposedHookInitPackageResources {

    private static XSharedPreferences prefs;

    private static synchronized XSharedPreferences getPrefs() {
        if (prefs == null) {
            prefs = new XSharedPreferences("com.realme.modxposed", "settings");
            prefs.makeWorldReadable();
        } else {
            prefs.reload();
        }
        return prefs;
    }

    private static boolean isAppEnabled(String pkg) {
        try {
            XSharedPreferences p = getPrefs();
            if (p != null) return p.getBoolean("app_enabled_" + pkg, true);
        } catch (Throwable ignored) {}
        return true;
    }

    private static boolean isHookEnabled(String hookId) {
        try {
            XSharedPreferences p = getPrefs();
            if (p != null) return p.getBoolean("hook_enabled_" + hookId, true);
        } catch (Throwable ignored) {}
        return true;
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        String pkgName = lpparam.packageName;

        // Global check: Is hook enabled for this target app as a whole?
        if (!isAppEnabled(pkgName)) {
            return;
        }

        switch (pkgName) {
            case "android":
                new HookOplusVRR().init(lpparam);
                break;

            case ClassesConstants.SystemUi:
                if (isHookEnabled("GestureNavigationView")) {
                    new GestureNavigationView().init(lpparam);
                }
                // HookKeyguardPinLock is intentionally disabled per user instruction
                if (isHookEnabled("HookClock")) {
                    new HookClock().init(lpparam);
                }
                if (isHookEnabled("HookRealBatteryDecimal")) {
                    new HookRealBatteryDecimal().init(lpparam);
                }
                break;

            case "com.android.launcher":
                if (isHookEnabled("LauncherAnimationHook")) {
                    new LauncherAnimationHook().init(lpparam);
                }
                break;

            case "com.hamrocsit":
                if (isHookEnabled("HamroCsit")) {
                    new HamroCsit().init(lpparam);
                }
                break;

            case "com.f1soft.banksmart.siddhartha":
                if (isHookEnabled("Siddha")) {
                    new Siddha().init(lpparam);
                }
                break;

            case "com.engineeringnepal.ghoksewa":
                if (isHookEnabled("GhokSewaMod")) {
                    // new GhokSewaMod().init(lpparam);
                }
                break;
        }
    }

    @Override
    public void handleInitPackageResources(XC_InitPackageResources.InitPackageResourcesParam resparam) throws Throwable {
    }
}
