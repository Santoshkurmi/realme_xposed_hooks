package com.realme.modxposed;

import android.content.res.Resources;
import android.text.format.DateFormat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.realme.modxposed.hooks.HamroCsit;
import com.realme.modxposed.hooks.NIC;
import com.realme.modxposed.hooks.HookClock;
import com.realme.modxposed.hooks.HookKeyguardPinLock;
import com.realme.modxposed.hooks.HookSystemLauncher;

import org.json.JSONObject;

import java.util.Calendar;
import java.util.Date;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSource;

public class MainXposedHookEntry  implements IXposedHookLoadPackage, IXposedHookInitPackageResources {


    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (lpparam.packageName.equals( ClassesConstants.SystemUi ) ) {
            new HookKeyguardPinLock().init(lpparam);
            new HookClock().init(lpparam);




//            String classClock = "com.android.keyguard.KeyguardClockSwitchController";
//            Class<?> clsClock = XposedHelpers.findClass(classClock,lpparam.classLoader);
//
//            Class<?> singlen = XposedHelpers.findClass("com.oplusos.systemui.keyguard.clock.SingleClockView",lpparam.classLoader);
//            XposedHelpers.findAndHookMethod(singlen, "updateDate", new XC_MethodHook() {
//                long counter =0;
//                @Override
//                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//                    try {
//                        Object date = XposedHelpers.getObjectField(param.thisObject,"mDate");
////                        String currentDate = (String) XposedHelpers.callMethod(date,"getText");
//                        String currentDate = new Date().toString();
//                        XposedHelpers.callMethod(date,"setText",currentDate+" "+counter++);
//                    }catch (Exception e){
//                        XposedBridge.log(e.toString()+"#################");
//                    }
//                }
//            });


//            XposedHelpers.findAndHookMethod(clsClock,"refresh", new XC_MethodHook() {
//                @Override
//                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//                    XposedBridge.log("I am here************");
//                    try{
//                        Object obj =XposedHelpers.getObjectField(param.thisObject,"mView");
//                        Object obja =XposedHelpers.getObjectField(obj,"mClockPlugin");
//
////                        String tr = (String) XposedHelpers.callMethod(obja,"getText");
//                        XposedBridge.log("************cls"+obja.getClass().getName());
//                    }
//                    catch (Exception e){
//                        XposedBridge.log("**********"+e.toString());
//                    }//catch
//                }//after hook
//            });

        }//if SystemUI





        if (lpparam.packageName.equals("com.hamrocsit")){
            new HamroCsit().init(lpparam);
//            XposedHelpers.findAndHookMethod("okhttp3.Request.Builder", lpparam.classLoader, "url", String.class, new XC_MethodHook() {
//                @Override
//                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
//                    XposedBridge.log((String) param.args[0]);
////                    OkHttpClient client = new OkHttpClient();
////                    Request request = new Request.Builder().url("").build();
////
////                    Response response = client.newCall(request).execute();
//
//                }
//            });









        }//if hamrocist
	
	//if for NIC
	new NIC().init(lpparam);




    }


    @Override
    public void handleInitPackageResources(XC_InitPackageResources.InitPackageResourcesParam resparam) throws Throwable {

        if (resparam.packageName.equals( ClassesConstants.SystemLauncher ) ) new HookSystemLauncher().init(resparam);

    }
}

