package com.realme.modxposed.hooks;

import android.app.AndroidAppHelper;
import android.content.pm.ApplicationInfo;
import android.util.Log;

import com.realme.modxposed.IXposedHookLoadPackage;

import com.realme.modxposed.utils.Compiler;


import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import java.util.ArrayList;
import java.util.Enumeration;

import dalvik.system.DexFile;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class DynamicHooking implements IXposedHookLoadPackage {
    String path;
    String packageName;

    @Override
    public void init(XC_LoadPackage.LoadPackageParam lpparam) {

        packageName = lpparam.packageName;
        try {
            new Compiler().handleHookingMethod(packageName,lpparam);

        }
        catch (Exception e){
            XposedBridge.log(e.toString());
        }

    }//init



    public void listAllClasses(XC_LoadPackage.LoadPackageParam lpparam){
        //run
        new Thread(() -> {
            XposedBridge.log("Running teh setup");
//                ApplicationInfo info = getApplicationInfo().sour;
            try {
                ApplicationInfo applicationInfo = AndroidAppHelper.currentApplicationInfo();
                DexFile dex = new DexFile(applicationInfo.sourceDir);
                Enumeration<String> entries = dex.entries();
                String currentClass;
                while (entries.hasMoreElements()){
                    currentClass = entries.nextElement();
                    if(currentClass.startsWith(lpparam.packageName)){
//                            Log.d("abcdef", currentClass);
                        try{
//                            final Class<?> clazz = Class.forName(currentClass);
                            final Class<?> clazz = lpparam.classLoader.loadClass(currentClass);
                            if(clazz.isInterface()) continue;
//                                clazz.getDeclaredMethods()
//                                Log.d("abcdef*************",clazz.toString());
                            for(final Method method : clazz.getDeclaredMethods()){
//                                    Log.d("abcdef", method.getName());
                                String finalCurrentClass = currentClass;
                                if(!Modifier.isAbstract(method.getModifiers()) )
                                    XposedBridge.hookMethod(method, new XC_MethodHook() {
                                    @Override
                                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//                                        super.afterHookedMethod(param);
                                        if(!finalCurrentClass.startsWith("com.f1soft.esewa")) return;
                                        XposedBridge.log(clazz+"*"+method.toString()+"=>"+param.getResult());
                                        if(method.getName().equals("getUsername")) param.setResult("Tiger is good");

                                    }
                                });
                            }//for method
                        }//inner
                        catch (ClassNotFoundException _e){
                            Log.d("abcdef", _e.toString());
                            XposedBridge.log(_e.toString());


                        }//catch

                    }//if found
                }//while
            } catch (Exception e ) {
                XposedBridge.log(e.toString());
                Log.d("abcdef",e.toString());
            }
        }).start();

    }//listAllClasses


}//class
