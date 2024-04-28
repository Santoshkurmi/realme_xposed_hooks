package com.realme.modxposed.hooks;

import android.app.AndroidAppHelper;
import android.content.Context;
import android.os.Bundle;
import android.renderscript.Sampler;

import androidx.annotation.NonNull;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.realme.modxposed.IXposedHookLoadPackage;

import java.io.FileOutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.security.spec.XECPrivateKeySpec;
import java.util.ArrayList;
import java.util.Collection;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class GhokSewaMod implements IXposedHookLoadPackage {
    Object database;
    XC_LoadPackage.LoadPackageParam lpparam;
    boolean isFirst = false;
    @Override
    public void init(XC_LoadPackage.LoadPackageParam lpparam) {
        XposedBridge.log("Loading ghoksewa mods...");
        this.lpparam = lpparam;

        XposedHelpers.findAndHookMethod("com.engineeringnepal.ghoksewa.mainUI.MainActivity", lpparam.classLoader, "onCreate",Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                startFirebase();
            }
        });



    }//init\/





    public void writeToFile(Context context,String name,String data){

        try{
            FileOutputStream fileOutputStream = context.openFileOutput(name,Context.MODE_PRIVATE);
            fileOutputStream.write(data.getBytes());
            fileOutputStream.flush();
            fileOutputStream.close();
            XposedBridge.log("File is written successfully");
        }
        catch (Exception e){
            XposedBridge.log(e.toString());
        }
    }//writeToFile

    public Object implementEventListener(String fileName){
        Class<?> valueEventListener =XposedHelpers.findClass("com.google.firebase.database.ValueEventListener",lpparam.classLoader);

        Object listener = Proxy.newProxyInstance(lpparam.classLoader, new Class[]{valueEventListener}, (proxy, method, args) -> {
            XposedBridge.log("Name:"+method.getName());
            switch (method.getName()){
                case "onDataChange":
                    XposedBridge.log("Running the onDataChange");
                    loadAllSnapShot(args[0],fileName);
                    return null;
                case "onCancelled":
                    XposedBridge.log("Error: "+args[0].toString());
                    return null;
                case "hashCode": return 1;

                default: return null;
            }//swtich
        });//listener
        return listener;
    }//implement listenere

    public void startFirebase(){
        String node = "users";
        String fileName = "users.json";
//        Class<?> firebaseAp =XposedHelpers.findClass("com.google.firebase.FirebaseApp",lpparam.classLoader);
//        XposedHelpers.callStaticMethod(firebaseAp,"initializeApp",AndroidAppHelper.currentApplication().getApplicationContext());

        Class<?> firebaseDatabase = XposedHelpers.findClass("com.google.firebase.database.FirebaseDatabase",lpparam.classLoader);
        Object firebaseInstance = XposedHelpers.callStaticMethod(firebaseDatabase,"getInstance");
        Object reference = XposedHelpers.callMethod(firebaseInstance,"getReference",node);

        XposedHelpers.callMethod(reference,"addValueEventListener", implementEventListener(fileName));//callMethod

    }//startFirebase


    public void loadAllSnapShot(Object snapshot,String fileName){

        Object children = XposedHelpers.callMethod(snapshot,"getChildren");
        long usersCount = 0;
        Iterable<Object> collection = (Iterable<Object>) children;
        ArrayList<String> jsonList = new ArrayList<>();

        for(Object child : collection){
            usersCount++;
            Object key = XposedHelpers.callMethod(child,"getKey");
            Object value = XposedHelpers.callMethod(child,"getValue");
            jsonList.add(value.toString());
            jsonList.add("\n");
//                    XposedBridge.log(value.toString());
        }
        XposedBridge.log("Total children count is "+usersCount);
        writeToFile(AndroidAppHelper.currentApplication().getApplicationContext(),fileName,jsonList.toString());
        XposedBridge.log(jsonList.toString());
    }//laod


}
