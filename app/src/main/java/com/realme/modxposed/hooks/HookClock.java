package com.realme.modxposed.hooks;

import android.content.Context;
import com.realme.modxposed.IXposedHookLoadPackage;
import java.util.Calendar;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookClock implements IXposedHookLoadPackage {

    @Override
    public void init(XC_LoadPackage.LoadPackageParam lpparam) {
        final String HELPER_CLASS = "com.oplus.systemui.keyguard.clock.WeatherInfoParseHelper";
        final BsToAdConverter bsToAdConverter = new BsToAdConverter();

        // Hook 1: Sync version - getLocalTimeInfo(Context) → returns TimeInfo
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
                        }
                    }
                }
        );

        // Hook 2: Async version - packageTimeInfo
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
                        }
                    }
                }
        );
    }
}
