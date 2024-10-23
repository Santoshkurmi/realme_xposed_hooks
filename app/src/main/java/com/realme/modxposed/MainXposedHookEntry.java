package com.realme.modxposed;

import com.realme.modxposed.hooks.AccountManagerHook;
import com.realme.modxposed.hooks.GhokSewaMod;
import com.realme.modxposed.hooks.HamroCsit;
import com.realme.modxposed.hooks.HookClock;
import com.realme.modxposed.hooks.HookKeyguardPinLock;
import com.realme.modxposed.hooks.HookSystemLauncher;
import com.realme.modxposed.hooks.DynamicHooking;
import com.realme.modxposed.hooks.LauncherAnimationHook;


import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainXposedHookEntry implements IXposedHookLoadPackage, IXposedHookInitPackageResources {

  @Override
  public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
    if (lpparam.packageName.equals(ClassesConstants.SystemUi)) {
      new HookKeyguardPinLock().init(lpparam);
      new HookClock().init(lpparam);

    } // if SystemUI

    else if (lpparam.packageName.equals("com.hamrocsit")) {
      new HamroCsit().init(lpparam);
    } // if hamrocist
    else if(lpparam.packageName.equals("com.engineeringnepal.ghoksewa")){
    } else if (lpparam.packageName.equals("com.google.android.gm")||lpparam.packageName.equals("com.google.android.apps.photo") ) {
       new AccountManagerHook().init(lpparam);
    }

    else if (lpparam.packageName.equals("com.android.launcher")){
      new LauncherAnimationHook().init(lpparam);
    }

    // if for NIC
    //new NIC().init(lpparam);
//    if(lpparam.packageName.equals("com.f1soft.esewa"))
//    else new DynamicHooking().init(lpparam);

  }

  @Override
  public void handleInitPackageResources(XC_InitPackageResources.InitPackageResourcesParam resparam) throws Throwable {

    if (resparam.packageName.equals(ClassesConstants.SystemLauncher))
      new HookSystemLauncher().init(resparam);

  }
}
