package com.realme.modxposed.hooks;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.OnAccountsUpdateListener;
import android.app.AndroidAppHelper;

import com.realme.modxposed.IXposedHookLoadPackage;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class AccountManagerHook5 implements IXposedHookLoadPackage {
    String packageName = "com.google.android.gm";

    @Override
    public void init(XC_LoadPackage.LoadPackageParam param) {
//        AccountManager manager = AccountManager.get(AndroidAppHelper.currentApplication());

        XposedHelpers.findAndHookMethod("android.accounts.AccountManager",param.classLoader, "get", "android.content.Context", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                XposedBridge.log("Hello tere");
                AccountManager manager = (AccountManager) param.getResult();
//                manager.getAccountsByTypeAndFeatures()

                Account[] accounts = manager.getAccounts();
                for (Account account : accounts) {
                    XposedBridge.log("Account Name: " + account.name + ", Account Type: " + account.type);
                }//for


//                param.setResult(null);


            }//after
        });

        XposedHelpers.findAndHookMethod("android.accounts.AccountManager",param.classLoader, "getAccounts", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                XposedBridge.log("Hello tere2");

                Account[] newAccount = new Account[1];
                Account[] accounts = (Account[]) param.getResult();
                newAccount[0] = accounts[1];
                param.setResult(newAccount);
//                for (Account account : accounts) {
//                    XposedBridge.log("Account Name: " + account.name + ", Account Type: " + account.type);
//                }//for


            }//after
        });

        XposedHelpers.findAndHookMethod("android.accounts.AccountManager",param.classLoader, "getAccountsByType","java.lang.String", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                XposedBridge.log("Hello tere3 "+param.args[0]);
                if( ! "com.google".equals(param.args[0])) return;

                Account[] newAccount = new Account[1];
                Account[] accounts = (Account[]) param.getResult();
                newAccount[0] = accounts[1];
                param.setResult(newAccount);
//                for (Account account : accounts) {
//                    XposedBridge.log("Account Name: " + account.name + ", Account Type: " + account.type);
//                }//for


            }//after
        });

//        AccountManager
        AccountManager manager2;
//        manager2.addOnAccountsUpdatedListener();
//        manager2.getAccountsByTypeForPackage()
//        manager2.getAccountsByTypeAndFeatures().getResult();
        Class<?> manager = XposedHelpers.findClass("android.accounts.AccountManager",param.classLoader);
        XposedBridge.hookAllMethods(manager, "addOnAccountsUpdatedListener", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                XposedBridge.log("Account Listener "+ param.args.length);
                param.args[0] = new OnAccountsUpdateListener(){

                    @Override
                    public void onAccountsUpdated(Account[] accounts) {
//                        OnAccountsUpdateListener account = (OnAccountsUpdateListener) param.args[0];
                        XposedBridge.log("Account called wow");
                    }
                };
//                Account[] accounts = new Account[0];
//                Object obj = param.args[0];
//                XposedHelpers.callMethod(obj,"onAccountsUpdated", (Object) accounts);
//                param.args[0] = (OnAccountsUpdateListener) accountss -> {
//                    XposedBridge.log("LEnght is "+accounts.length);
//                };
//


            }//after
        });

        XposedBridge.hookAllMethods(manager, "getAccountsByTypeForPackage", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                XposedBridge.log("Hello tere45 "+param.args[0]+","+param.args[1]);
                Account[] accounts = (Account[]) param.getResult();
                XposedBridge.log(accounts.length+" ");
//                param.args[0] = (OnAccountsUpdateListener) accounts -> {
//                    XposedBridge.log("LEnght is "+accounts.length);
//                };
//


            }//after
        });


//
    }//init

}//AccountManager

