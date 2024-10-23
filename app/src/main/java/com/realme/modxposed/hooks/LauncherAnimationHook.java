package com.realme.modxposed.hooks;

import android.app.AndroidAppHelper;
import android.content.pm.ApplicationInfo;
import android.util.Log;

import com.realme.modxposed.IXposedHookLoadPackage;
import com.realme.modxposed.utils.Compiler;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Enumeration;

import dalvik.system.DexFile;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class LauncherAnimationHook implements IXposedHookLoadPackage {
    String path;
    String packageName;

    @Override
    public void init(XC_LoadPackage.LoadPackageParam lpparam) {
        String className = "com.android.common.util.AppFeatureUtils";
        XposedBridge.log("WOrking or not");

        XposedHelpers.findAndHookMethod(className, lpparam.classLoader, "isLightOSAnimation", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                param.setResult(false);
//                XposedBridge.log("Setting the light animation to false");
            }
        });
        XposedHelpers.findAndHookMethod("com.android.common.util.AppFeatureUtils$isHomeGestureLightAnimation$2", lpparam.classLoader, "invoke", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                param.setResult(false);
                XposedBridge.log("Hooked isHomeGestureLightAnimation");
            }
        });


    }//init


}//class
