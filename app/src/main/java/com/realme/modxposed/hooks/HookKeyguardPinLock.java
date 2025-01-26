package com.realme.modxposed.hooks;

import android.annotation.SuppressLint;
import android.app.AndroidAppHelper;
import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.realme.modxposed.ClassesConstants;
import com.realme.modxposed.IXposedHookLoadPackage;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Set;

import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class HookKeyguardPinLock implements IXposedHookLoadPackage {

    long previousTimeInMillis;
    String password = null;
    ArrayList<String> passwords = new ArrayList<>();
    String realPassword = null;
    boolean isMagic = false;
    int passwordCount,passwordLen,time;
    Context context;
    int noOfTimesWithoutPasswordLogin = 0;
    XSharedPreferences preferences = new XSharedPreferences("com.realme.modxposed","settings");
    SharedPreferences preferencesSystem;
    boolean isUnlockingAuto = false;

    @Override
    public void init(XC_LoadPackage.LoadPackageParam param) {

       loadPreference();
//       noOfTimesWithoutPasswordLogin = 5;

        XposedBridge.log("Hooking SysetemUI");
        if (!isMagic) return;

        Class<?> keyguardPinView = XposedHelpers.findClass(ClassesConstants.KeyguardPinView,param.classLoader);
        Class<?> baseKeyguardSecurity = XposedHelpers.findClass(ClassesConstants.BaseKeyguardSecurityView,param.classLoader);


//        XposedHelpers.findAndHookMethod("com.android.keyguard.KeyguardUpdateMonitorCallback", param.classLoader, "onKeyguardVisibilityChanged", boolean.class, new XC_MethodHook() {
//            @Override
//            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
//                super.beforeHookedMethod(param);
//                XposedBridge.log("Visiblity for keyguard is "+param.args[0]);
//            }
//        });


        XposedHelpers.findAndHookMethod(baseKeyguardSecurity, "setMEntry",String.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                String real = (String) param.args[0];

                XposedBridge.log("Started performing lock");

                if(real.isEmpty())return;

//                XposedBridge.log("First entry***: ."+real+".");
                if (noOfTimesWithoutPasswordLogin>0 && real.equals("0")){
                    param.args[0] = realPassword;
                    noOfTimesWithoutPasswordLogin--;
                    XposedBridge.log("AutoUnlocking the things left:"+noOfTimesWithoutPasswordLogin);
                    return;
            }

                if (real.length() != passwordLen) return;


                if (real.length()==realPassword.length() ){
                    String realWithLessOne = real.substring(0,realPassword.length()-2);
                    if(realPassword.startsWith(realWithLessOne)){

                        noOfTimesWithoutPasswordLogin = Integer.valueOf( real.substring(realPassword.length() - 1) );
                        param.args[0] = realPassword;
                        return;
                    }
                    else{
                        noOfTimesWithoutPasswordLogin = 0;
                    }

                }



                if (context == null) {
                    context = AndroidAppHelper.currentApplication();
                    preferencesSystem = context.getSharedPreferences("passwords",Context.MODE_PRIVATE);
                    loadPasswordLists();

                }


                if (System.currentTimeMillis() - previousTimeInMillis > time  && passwords.size()<passwordCount && !passwords.contains(real)) {
                    if(!real.equals("123456") ){
                        passwords.add(real);
                        savePasswordLists();
                    }

                }
                if (  passwords.contains(real) ) {
                    param.args[0] = realPassword;
//                    param.setResult(realPassword);
            }

        }




        });


        XposedHelpers.findAndHookMethod(keyguardPinView, "onEntryChanged", String.class,String.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                try{
                    String previous = (String) param.args[0];
//                    String current = (String) param.args[1];
//                    param.args[1] = realPassword;

                    if (password ==null && previous.isEmpty()) {
                        previousTimeInMillis = System.currentTimeMillis();
                    }

                }
                catch (Exception e){
                    XposedBridge.log(e.toString());
                }
//                XposedBridge.log("**************Hooking and get unlocking wow hehe hehe hehe ");
            }
        });//onEntryChanged


    }//init


    public void loadPreference(){



        time =  preferences.getInt("time",15)*1000;
        passwordCount = preferences.getInt("password_count",1);
        passwordLen = preferences.getInt("password_len",6);
        realPassword = (preferences.getString("password","723426"));
        isMagic = (preferences.getBoolean("magic",false));

//        Set<String> set = preferences.getStringSet("password_list",null);
//        if (set == null) return;
//        passwords = new ArrayList<>(set);

    }//lood

    public void savePasswordLists(){
       SharedPreferences.Editor editor = preferencesSystem.edit();
       Set<String> set = new HashSet<>(passwords);
       editor.putStringSet("password_list",set);
       editor.apply();
    }//

    public void loadPasswordLists(){
        Set<String> set = preferencesSystem.getStringSet("password_list",null);
        if (set == null)return;
        passwords = new ArrayList<>(set);
    }


}
