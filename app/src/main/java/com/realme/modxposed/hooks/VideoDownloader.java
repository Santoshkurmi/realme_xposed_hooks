package com.realme.modxposed.hooks;

import com.realme.modxposed.IXposedHookLoadPackage;

import java.io.File;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class VideoDownloader implements IXposedHookLoadPackage {
    @Override
    public void init(XC_LoadPackage.LoadPackageParam lpparam) {


        XposedHelpers.findAndHookMethod("a4.k", lpparam.classLoader, "c",String.class,String.class,new XC_MethodHook() {


            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {

                XposedBridge.log("helo "+ param.args[0]+" "+param.args[1]);
                param.setResult(true);

            }
        });

        XposedHelpers.findAndHookMethod("a4.k", lpparam.classLoader, "d", File.class,String.class,new XC_MethodHook() {


            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {

                XposedBridge.log("File "+ param.args[0]+" "+param.args[1]);
                param.setResult(true);

            }
        });



        XposedHelpers.findAndHookMethod("a4.x", lpparam.classLoader, "a",String.class,new XC_MethodHook() {


            protected void afterHookedMethod(MethodHookParam param) throws Throwable {

                XposedBridge.log("Link: "+ param.args[0]);
                XposedBridge.log("Link output: "+ param.getResult());

//                param.setResult(true);

            }
        });

        XposedHelpers.findAndHookConstructor("com.appx.core.model.EncryptedRecordModel", lpparam.classLoader, String.class,String.class,String.class,String.class,String.class,new XC_MethodHook() {


            protected void afterHookedMethod(MethodHookParam param) throws Throwable {

//                XposedBridge.log("args: "+ param.args[0]+" "+ args[1]);
                for (int i=0;i<param.args.length;i++){
                    XposedBridge.log("args "+i+" "+param.args[i]);
                }
//                XposedBridge.log("Link output: "+ param.getResult());

//                param.setResult(true);

            }
        });

    }//init
}
