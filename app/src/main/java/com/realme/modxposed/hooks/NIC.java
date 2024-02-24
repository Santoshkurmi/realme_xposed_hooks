package com.realme.modxposed.hooks;

import com.realme.modxposed.IXposedHookLoadPackage;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class NIC implements IXposedHookLoadPackage {
    @Override
    public void init(XC_LoadPackage.LoadPackageParam lpparam) {


        XposedHelpers.findAndHookMethod("com.f1soft.banksmart.android.core.utils.CommonUtils", lpparam.classLoader, "isClonedApp","android.content.Context",new XC_MethodHook() {

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {

                    param.setResult(false);
            }
        });


        XposedHelpers.findAndHookMethod("android.provider.Settings.Secure", lpparam.classLoader, "getString","android.content.ContentResolver","java.lang.String",new XC_MethodHook() {

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
		    if(param.args[1].equals("android_id") )
                    	param.setResult("bcdd6365d8fa1583");
            }
        });

    }//init
}
