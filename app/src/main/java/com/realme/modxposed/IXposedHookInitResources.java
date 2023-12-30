package com.realme.modxposed;

import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public interface IXposedHookInitResources {
    public void init(XC_InitPackageResources.InitPackageResourcesParam param);
}
