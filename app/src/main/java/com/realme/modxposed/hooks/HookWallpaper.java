package com.realme.modxposed.hooks;

import com.realme.modxposed.IXposedHookLoadPackage;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookWallpaper implements IXposedHookLoadPackage {

    @Override
    public void init(XC_LoadPackage.LoadPackageParam lpparam) {
        // This app controls the Lockscreen Clock widgets on ColorOS/RealmeUI
        XposedBridge.log("Hello world");


        // The inner class 'b' inside ColorClockView handles formatting logic
        final String CLOCK_INNER_CLASS = "com.oplus.wallpapers.core.base.view.ColorClockView$b";
        final BsToAdConverter bsToAdConverter = new BsToAdConverter();

        XposedHelpers.findAndHookMethod(
                CLOCK_INNER_CLASS, lpparam.classLoader,
                "d", // This method returns the final date string for the lockscreen
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        // Original result is usually "Friday, Oct 24" or localized equivalent
                        String originalDate = (String) param.getResult();

                        if (originalDate != null) {
                            // Replace or Append the Nepali Date
                            // If you want BOTH dates: String result = originalDate + " | " + bsToAdConverter.getMonth();
                            // If you want ONLY Nepali: String result = bsToAdConverter.getMonth();

                            param.setResult(bsToAdConverter.getMonth());
                            // XposedBridge.log("HookWallpaper: Lockscreen date modified");
                        }
                    }
                }
        );
    }
}