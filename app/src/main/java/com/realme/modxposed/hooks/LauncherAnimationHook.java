package com.realme.modxposed.hooks;

import com.realme.modxposed.IXposedHookLoadPackage;

import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class LauncherAnimationHook implements IXposedHookLoadPackage {

    @Override
    public void init(XC_LoadPackage.LoadPackageParam lpparam) {
        // Disabled per user instruction - LauncherAnimationHook is no-op
    }
}
