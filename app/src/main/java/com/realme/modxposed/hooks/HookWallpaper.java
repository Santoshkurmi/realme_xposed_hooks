package com.realme.modxposed.hooks;

import com.realme.modxposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookWallpaper implements IXposedHookLoadPackage {

    @Override
    public void init(XC_LoadPackage.LoadPackageParam lpparam) {
        final String CLOCK_INNER_CLASS = "com.oplus.wallpapers.core.base.view.ColorClockView$b";
        final BsToAdConverter bsToAdConverter = new BsToAdConverter();

        XposedHelpers.findAndHookMethod(
                CLOCK_INNER_CLASS, lpparam.classLoader,
                "d", // This method returns the final date string for the lockscreen
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        String originalDate = (String) param.getResult();
                        if (originalDate != null) {
                            param.setResult(bsToAdConverter.getMonth());
                        }
                    }
                }
        );
    }
}