package com.realme.modxposed;

import de.robv.android.xposed.callbacks.XC_LoadPackage;

public interface IXposedHookLoadPackage {
    public void init(XC_LoadPackage.LoadPackageParam param);
}
