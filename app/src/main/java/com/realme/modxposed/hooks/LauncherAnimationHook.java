package com.realme.modxposed.hooks;

import android.app.AndroidAppHelper;
import android.content.ContentResolver;
import android.content.pm.ApplicationInfo;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.realme.modxposed.IXposedHookLoadPackage;
import com.realme.modxposed.utils.Compiler;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Enumeration;

import dalvik.system.DexFile;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class LauncherAnimationHook implements IXposedHookLoadPackage {
    String path;
    String packageName;
    Cursor cursorSaved;
    boolean isFetching = false;
    String feature;
    Uri uri;
    String[] paramsString;

    @Override
    public void init(XC_LoadPackage.LoadPackageParam lpparam) {
// Target: com.android.launcher (check with: adb shell pm list packages | grep launcher)
        XposedBridge.log("HOOKing launcher");
        // Target: com.android.launcher package
        final int EXTRA_HEIGHT = 200;

// THE hook that actually matters - increase the gesture area size returned by NavigationController
        XposedHelpers.findAndHookMethod(
                "com.oplus.quickstep.navigation.NavigationController",
                lpparam.classLoader,
                "getGestureAreaSizeInMistouch",
                android.content.res.Resources.class,
                XposedHelpers.findClass("com.android.quickstep.OrientationRectF", lpparam.classLoader),
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        float original = (float) param.getResult();
                        param.setResult(original + EXTRA_HEIGHT);
                        XposedBridge.log("getGestureAreaSizeInMistouch: " + original + " -> " + (original + EXTRA_HEIGHT));
                    }
                }
        );


        XposedHelpers.findAndHookMethod(
                "com.oplus.quickstep.utils.DefaultAnimationSeqHelper", lpparam.classLoader,
                "canInterceptGesture",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        param.setResult(true); // Always allow gesture interception
                    }
                }
        );


        XposedHelpers.findAndHookMethod(
                "com.oplus.quickstep.utils.DefaultAnimationController", lpparam.classLoader,
                "isOpeningAnim",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        param.setResult(false); // Pretend no opening animation
                    }
                }
        );


        XposedHelpers.findAndHookMethod(
                "com.android.systemui.shared.system.AnimSeqTimeStamp", lpparam.classLoader,
                "getTimeGapToLastStartAppTime",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        param.setResult(999L); // Always above 300ms threshold
                    }
                }
        );





    }//init

    private static void hookLiveTileMode(XC_LoadPackage.LoadPackageParam lpparam, String className) {
        try {
            Class<?> clazz = XposedHelpers.findClass(className, lpparam.classLoader);
            XposedHelpers.findAndHookMethod(clazz, "isInLiveTileMode", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    param.setResult(false);
                }
            });
        } catch (Throwable t) {
            // Ignore if class not found
        }
    }


}//class
