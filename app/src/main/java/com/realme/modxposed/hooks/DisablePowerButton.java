package com.realme.modxposed.hooks;

import android.view.KeyEvent;

import com.realme.modxposed.IXposedHookLoadPackage;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class DisablePowerButton implements IXposedHookLoadPackage {
    @Override
    public void init(XC_LoadPackage.LoadPackageParam lpparam) {
            XposedBridge.log("Hooking power Button Disabled");
        try{
            Class<?> manager;
             manager = XposedHelpers.findClassIfExists("com.android.internal.policy.impl.PhoneWindowManager",null);
            if(manager==null) manager = XposedHelpers.findClassIfExists("com.android.server.policy.PhoneWindowManager",null);
            if(manager==null) manager = XposedHelpers.findClass("com.android.server.policy.OemPhoneWindowManager",null);
            XposedHelpers.findAndHookMethod(manager, "interceptKeyBeforeQueueing", KeyEvent.class, int.class, boolean.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    KeyEvent event = (KeyEvent) param.args[0];
                    XposedBridge.log("Disable power button");
                    if(event.getKeyCode()==KeyEvent.KEYCODE_POWER){
                        param.setResult(0);
                    }
                }
            });
        }
        catch(Exception e){
            XposedBridge.log(e.toString());
        }
    }//init
}
