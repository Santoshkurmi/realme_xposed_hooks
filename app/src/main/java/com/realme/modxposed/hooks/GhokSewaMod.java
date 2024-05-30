package com.realme.modxposed.hooks;

import android.app.AndroidAppHelper;
import android.content.Context;
import android.os.Bundle;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.realme.modxposed.IXposedHookLoadPackage;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileOutputStream;
import java.lang.reflect.Proxy;
import java.util.ArrayList;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class GhokSewaMod implements IXposedHookLoadPackage {
    Object database;
    ArrayList<String> paths = new ArrayList<>();
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
//                startFirebase("/iq/subj-01/questions");
                hookFirebase();
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
                    XposedBridge.log("Error: "+args[0].toString()+" for "+fileName);
                    return null;
                case "hashCode": return 1;

                default: return null;
            }//swtich
        });//listener
        return listener;
    }//implement listenere

    public void hookFirebase(){
        Class<?> DatabaseReference = XposedHelpers.findClass("com.google.firebase.database.DatabaseReference",lpparam.classLoader);
        Class<?> Repo = XposedHelpers.findClass("com.google.firebase.database.core.Repo",lpparam.classLoader);
        Class<?> Path = XposedHelpers.findClass("com.google.firebase.database.core.Path",lpparam.classLoader);

        XposedHelpers.findAndHookConstructor(DatabaseReference, Repo, Path, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                String path = param.args[1].toString();
                int count = path.split("/").length;
                if(count ==4){
//                    if(path.contains("users") && count >2) return;
                    if( !paths.contains(path) ){
                        paths.add(path);
                        XposedBridge.log(param.args[1].toString()+": "+count);
                        startFirebase(path);
                    }//if not
//                    else{
//                        XposedBridge.log("Already downloaded the path:"+path);
//                    }
                }
            }
        });//findANdHookConstructor
    }//hookFIrebase

    public void startFirebase(String node){
//        String node = "users";
        String fileName = node.replace('/',' ')+".json";
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
        ArrayList<String> questions = new ArrayList<>();

        for(Object child : collection){
            usersCount++;
            Object key = XposedHelpers.callMethod(child,"getKey");
            Object object = XposedHelpers.callMethod(child,"getValue");
            Gson gson = new GsonBuilder().disableHtmlEscaping().create();
            String json = gson.toJson(object);
            questions.add(json);
//            Question question = (Question) XposedHelpers.callMethod(child,"getValue", Question.class);
//                    XposedBridge.log(value.toString());
        }
        XposedBridge.log("Total children count is "+usersCount);
        writeToFile(AndroidAppHelper.currentApplication().getApplicationContext(),fileName,questions.toString());
//        XposedBridge.log(jsonList.toString());
    }//laod


}
