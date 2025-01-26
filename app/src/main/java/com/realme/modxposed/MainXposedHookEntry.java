package com.realme.modxposed;

import android.view.KeyEvent;

import com.realme.modxposed.hooks.AccountManagerHook;
import com.realme.modxposed.hooks.AccountManagerHook5;
import com.realme.modxposed.hooks.DisablePowerButton;
import com.realme.modxposed.hooks.GhokSewaMod;
import com.realme.modxposed.hooks.HamroCsit;
import com.realme.modxposed.hooks.HookClock;
import com.realme.modxposed.hooks.HookGame;
import com.realme.modxposed.hooks.HookKeyguardPinLock;
import com.realme.modxposed.hooks.HookSystemLauncher;
import com.realme.modxposed.hooks.DynamicHooking;
import com.realme.modxposed.hooks.LauncherAnimationHook;
import com.realme.modxposed.hooks.Siddha;
import com.realme.modxposed.hooks.VideoDownloader;


import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainXposedHookEntry implements IXposedHookLoadPackage, IXposedHookInitPackageResources {

  @Override
  public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
      switch (lpparam.packageName) {
          case ClassesConstants.SystemUi:
              new HookKeyguardPinLock().init(lpparam);
              new HookClock().init(lpparam);

              break;
          case "com.hamrocsit":
              new HamroCsit().init(lpparam);
              break;
          case "com.engineeringnepal.ghoksewa":
              break;
          case "com.google.android.gm":
          case "com.google.android.apps.photos":
//              new AccountManagerHook().init(lpparam);
              new AccountManagerHook5().init(lpparam);
              break;

          case "com.f1soft.banksmart.siddhartha":
              new Siddha().init(lpparam);
              break;
//          case "android":new DisablePowerButton().init(lpparam);


//          default: new VideoDownloader().init(lpparam);

//          default:
//              new HookGame().init(lpparam);
      }

//    else if ( lpparam.packageName.equals("com.google.android.googlequicksearchbox") || lpparam.packageName.equals("com.android.launcher")|| lpparam.packageName.equals("com.coloros.gallery3d") ){
//      new LauncherAnimationHook().init(lpparam);
//    }

    // if for NIC
    //new NIC().init(lpparam);
//    if(lpparam.packageName.equals("com.f1soft.esewa"))
//    else new DynamicHooking().init(lpparam);

  }

  @Override
  public void handleInitPackageResources(XC_InitPackageResources.InitPackageResourcesParam resparam) throws Throwable {

//    if (resparam.packageName.equals(ClassesConstants.SystemLauncher))
//      new HookSystemLauncher().init(resparam);

  }

//    @Override
//    public void initZygote(StartupParam startupParam) throws Throwable {
//        XposedBridge.log("Hooking power Button Disabled");
//        try{
//            Class<?> manager;
//            manager = XposedHelpers.findClassIfExists("com.android.internal.policy.impl.PhoneWindowManager",null);
//            if(manager==null) manager = XposedHelpers.findClassIfExists("com.android.server.policy.PhoneWindowManager",null);
//            if(manager==null) manager = XposedHelpers.findClass("com.android.server.policy.OemPhoneWindowManager",null);
//            XposedHelpers.findAndHookMethod(manager, "interceptKeyBeforeQueueing", KeyEvent.class, int.class, boolean.class, new XC_MethodHook() {
//                @Override
//                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
//                    KeyEvent event = (KeyEvent) param.args[0];
//                    XposedBridge.log("Disable power button");
//                    if(event.getKeyCode()==KeyEvent.KEYCODE_POWER){
//                        param.setResult(0);
//                    }
//                }
//            });
//        }
//        catch(Exception e){
//            XposedBridge.log(e.toString());
//        }
//    }//initZygote
}
