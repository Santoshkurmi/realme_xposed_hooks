package com.realme.modxposed.hooks;

import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.realme.modxposed.ClassesConstants;
import com.realme.modxposed.IXposedHookInitResources;

import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LayoutInflated;

public class HookSystemLauncher implements IXposedHookInitResources {


    @Override
    public void init(XC_InitPackageResources.InitPackageResourcesParam param) {

        param.res.hookLayout("com.android.launcher", "layout", "all_apps_fast_scroller", new XC_LayoutInflated() {
            @Override
            public void handleLayoutInflated(XC_LayoutInflated.LayoutInflatedParam layoutInflatedParam) throws Throwable {

                layoutInflatedParam.view.setLayoutParams(new RelativeLayout.LayoutParams(0,0));
            }
        });



        param.res.hookLayout("com.android.launcher", "layout", "all_apps_rv_layout", new XC_LayoutInflated() {
            @Override
            public void handleLayoutInflated(LayoutInflatedParam layoutInflatedParam) throws Throwable {
                ViewGroup view = layoutInflatedParam.view.findViewById(layoutInflatedParam.res.getIdentifier("apps_list_view","id", ClassesConstants.SystemLauncher));

                view.setForegroundGravity(View.TEXT_ALIGNMENT_CENTER);

//
            }
        });//new


        param.res.hookLayout("com.android.launcher", "layout", "all_apps_divider", new XC_LayoutInflated() {
            @Override
            public void handleLayoutInflated(LayoutInflatedParam layoutInflatedParam) throws Throwable {
//                XposedBridge.log("Hooked the launcher");
                RelativeLayout layout = (RelativeLayout) layoutInflatedParam.view;
                layout.setLayoutParams(new LinearLayout.LayoutParams(0,0));

            }
        });



    }//init



}
