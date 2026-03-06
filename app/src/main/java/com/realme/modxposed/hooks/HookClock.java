package com.realme.modxposed.hooks;

import android.content.Context;
import android.widget.TextView;

import com.realme.modxposed.IXposedHookLoadPackage;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookClock implements IXposedHookLoadPackage {

    @Override
    public void init(XC_LoadPackage.LoadPackageParam lpparam) {
        // Package changed from com.oplusos → com.oplus
        final String HELPER_CLASS = "com.oplus.systemui.keyguard.clock.WeatherInfoParseHelper";
        final String TIMEINFO_CLASS = HELPER_CLASS + "$TimeInfo";

        final BsToAdConverter bsToAdConverter = new BsToAdConverter();

        // Hook 1: Sync version - getLocalTimeInfo(Context) → returns TimeInfo
        // Still exists but marked @Deprecated. Some callers still use it.
        XposedHelpers.findAndHookMethod(
                HELPER_CLASS, lpparam.classLoader,
                "getLocalTimeInfo",
                Context.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Object timeInfo = param.getResult();
                        if (timeInfo != null) {
                            XposedHelpers.callMethod(timeInfo, "setDateInfo", bsToAdConverter.getMonth());
                            XposedBridge.log("HookClock: sync getLocalTimeInfo → Nepali date set");
                        }
                    }
                }
        );

        // Hook 2: Async version - getLocalTimeInfo(Context, Consumer<TimeInfo>)
        // This is what most UI components (lockscreen, QS, AOD) now use.
        // It calls getLocalTimeInfo(Context) internally, then delivers via Consumer on main thread.
        // Hook 1 above already modifies the result before the Consumer receives it,
        // so this should work automatically. But as a safety net:
        XposedHelpers.findAndHookMethod(
                HELPER_CLASS, lpparam.classLoader,
                "packageTimeInfo",
                Context.class, Calendar.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Object timeInfo = param.getResult();
                        if (timeInfo != null) {
                            XposedHelpers.callMethod(timeInfo, "setDateInfo", bsToAdConverter.getMonth());
                            XposedBridge.log("HookClock: packageTimeInfo → Nepali date set");
                        }
                    }
                }
        );

    }
}
