package com.realme.modxposed.hooks;

import android.content.Context;

import com.realme.modxposed.IXposedHookLoadPackage;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookClock implements IXposedHookLoadPackage {

    Object timeInfo = null;
    XC_MethodHook.Unhook hooked;
    @Override
    public void init(XC_LoadPackage.LoadPackageParam lpparam) {

        Class<?> clsWeather = XposedHelpers.findClass("com.oplusos.systemui.keyguard.clock.WeatherInfoParseHelper.TimeInfo",lpparam.classLoader);

        hooked = XposedHelpers.findAndHookMethod(clsWeather, "getDateInfo", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                XposedBridge.log("***********Hello world*******");
                    timeInfo = param.thisObject;
                    hooked.unhook();


//                XposedBridge.log("*********&&&&&&&&&"+param.getResult());
            }
        });//findandHook

        Class<?> clsWeathera = XposedHelpers.findClass("com.oplusos.systemui.keyguard.clock.WeatherInfoParseHelper",lpparam.classLoader);

        XposedHelpers.findAndHookMethod(clsWeathera, "getLocalTimeInfo", Context.class, new XC_MethodHook() {
            final BsToAdConverter bsToAdConverter = new BsToAdConverter();
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (timeInfo !=null) {
                        XposedHelpers.callMethod(timeInfo,"setDateInfo",bsToAdConverter.getMonth());
                        param.setResult(timeInfo);

                }
            }//before
        });


    }//init
}
