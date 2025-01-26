package com.realme.modxposed.hooks;

import com.realme.modxposed.IXposedHookLoadPackage;

import java.util.List;
import java.util.Map;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class Siddha implements IXposedHookLoadPackage {
    @Override
    public void init(XC_LoadPackage.LoadPackageParam lpparam) {

        XposedBridge.log("Running siddha baba");
        XposedHelpers.findAndHookMethod("xg.i", lpparam.classLoader, "i",String.class, List.class, Map.class,int.class,String.class,new XC_MethodHook() {

            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    XposedBridge.log("Setting ssl pinning to false");
                    param.setResult(true);
            }
        });


        XposedHelpers.findAndHookMethod("android.provider.Settings.Secure", lpparam.classLoader, "getString","android.content.ContentResolver","java.lang.String",new XC_MethodHook() {

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
		    if(param.args[1].equals("android_id") ){
                XposedBridge.log("Setting2 the fake android_id");
                param.setResult("bcdd6365d8fa1590");
//			if(lpparam.packageName.equals("com.f1soft.nicasiamobilebankini")) param.setResult("bcdd6365d8fa1591");
//			if(lpparam.packageName.equals("com.f1soft.nicasiamobilebankinj")) param.setResult("bcdd6365d8fa1592");
		    }//if android_id
            }
        });

    }//init
}
