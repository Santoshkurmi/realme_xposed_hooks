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
        String className = "com.android.common.util.AppFeatureUtils";
        XposedBridge.log("WOrking or not");

//        XposedHelpers.findAndHookMethod(className, lpparam.classLoader, "isLightOSAnimation", new XC_MethodHook() {
//            @Override
//            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
//                super.beforeHookedMethod(param);
//                param.setResult(false);
////                XposedBridge.log("Setting the light animation to false");
//            }
//        });
        XposedHelpers.findAndHookMethod("com.android.common.util.AppFeatureUtils$isHomeGestureLightAnimation$2", lpparam.classLoader, "invoke", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                param.setResult(false);
                XposedBridge.log("Hooked isHomeGestureLightAnimation");
            }
        });

//        ContentResolver resolver;
//        resolver.query(new Uri(),"","hello",new String[]{""},"");

//        XposedHelpers.findAndHookMethod("android.content.ContentResolver", lpparam.classLoader, "query",Uri.class,String[].class, String.class,String[].class,String.class,new XC_MethodHook() {
//            @Override
//            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
////                super.afterHookedMethod(param);
////                param.setResult(false);
////                String[] abc = new String[]{"hello"};
////                Arrays.toString(abc);
//                if(isFetching)return;
//                ContentResolver resolver = (ContentResolver) param.thisObject;
//                Cursor cursor = (Cursor) param.getResult();
////                XposedBridge.log(cursor.getClass().toString());
//
////                XposedBridge.hookMethod(cursor.getCount,)
////
//                if(cursor!=null){
//                    if(cursor.getCount()>0){
////                        cursorSaved = ;
//                        uri =(Uri) param.args[0];
//                        feature = (String) param.args[2];
//                        paramsString = (String[]) param.args[3];
////                        cursorSaved = resolver.query((Uri)param.args[0],(String[])param.args[1],(String)param.args[2],(String[])param.args[3],(String)param.args[4]);
//                        XposedBridge.log(Arrays.toString((String[])param.args[3])+" = True" );
//                    }
//                    else{
//                        XposedBridge.log(Arrays.toString((String[])param.args[3])+"= False" );
////                        param.setResult(cursorSaved);
//                        if(uri==null) return;
//                        isFetching = true;
//                        cursorSaved = resolver.query(uri,null,feature,paramsString,null);
//                        isFetching = false;
//                        if(cursorSaved !=null) param.setResult(cursorSaved);
//
//
//                    }
//                }
//            }
//        });

//        XposedHelpers.findAndHookMethod("android.content.ContentResolver$CursorWrapperInner", lpparam.classLoader, "getCount", new XC_MethodHook() {
//            @Override
//            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
//                super.beforeHookedMethod(param);
//                param.setResult(1);
//                XposedBridge.log("Done");
//            }
//        });

//        Class<?> appFeatures = XposedHelpers.findClassIfExists("com.oplus.coreapp.appfeature.AppFeatureProviderUtils",lpparam.classLoader);
//        XposedBridge.hookAllMethods(appFeatures, "isFeatureSupport", new XC_MethodHook() {
//            @Override
//            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//                super.afterHookedMethod(param);
//                XposedBridge.log("Result of "+param.args[1]+"=>"+param.getResult());
//            }
//        });


    }//init


}//class
