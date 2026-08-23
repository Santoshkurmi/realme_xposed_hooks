package com.realme.modxposed.hooks;

import com.realme.modxposed.IXposedHookLoadPackage;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class GestureNavigationView implements IXposedHookLoadPackage {

    private static final String TAG = "[ModXposedGesture]";

    @Override
    public void init(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!"com.android.systemui".equals(lpparam.packageName)) {
            return;
        }

        XposedBridge.log(TAG + " SystemUI Gesture Navigation Hook Initialized.");
    }
}
