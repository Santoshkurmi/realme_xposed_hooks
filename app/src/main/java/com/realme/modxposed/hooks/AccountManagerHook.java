package com.realme.modxposed.hooks;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.GoogleApi;
import com.realme.modxposed.IXposedHookLoadPackage;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class AccountManagerHook implements IXposedHookLoadPackage {
    String packageName = "com.google.android.gm";

    @Override
    public void init(XC_LoadPackage.LoadPackageParam param) {

        Class<?> googleSignIn = XposedHelpers.findClass("com.google.android.gms.auth.api.signin.GoogleSignInAccount",param.classLoader);


        XposedBridge.hookAllMethods(googleSignIn,"zaa", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                XposedBridge.log("yes cllaled");
            }
        });
//        XposedHelpers.findAndHookMethod(googleSignIn, "GoogleSignInAccount", new XC_MethodHook() {
//            @Override
//            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//                super.afterHookedMethod(param);
////                XposedBridge.ho
//
//                XposedBridge.log("Called this function");
//            }
//        });


//
    }//init

}//AccountManager

