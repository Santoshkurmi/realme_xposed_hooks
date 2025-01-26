package com.realme.modxposed.hooks;

import com.realme.modxposed.IXposedHookLoadPackage;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HamroCsit implements IXposedHookLoadPackage {
    @Override
    public void init(XC_LoadPackage.LoadPackageParam lpparam) {


        XposedHelpers.findAndHookMethod("okhttp3.ResponseBody", lpparam.classLoader, "string",new XC_MethodHook() {

            String sub = "\"subscription\":{\"start\":\"2023-09-24 09:19:53\",\"end\":\"2060-10-24 09:19:53\",\"next\":0,\"trial\":0,\"end_date\":\"2698139293000\",\"selected\":{\"price\":450,\"id\":7489,\"for\":\"yearly\"}}";
            String fin = "{\"success\":true,\"id\":\"21\",\"loginID\":\"123456789012323\",\"type\":\"google\",\"status\":\"approved\",\"authentication\":{\"token\":\"m526j3o5k5p444o5q394u4i335r4s4x2r3f4e3p4r4g41523g5d3d3a4v4o5g4w3y3c3t4f4346456f306f5v2k5y5g5c4r5g3d3q5k4n5i335l3z4o393v344k356g464n383p476u4q4h3y4i3o556q554s4d44675s3t4c4r4e495g4d3p5s4n5v375g4w4j3b4m4r5v365x43604n336r5e5r3p3h4o3o5v466i335f3e4z3y3x5w4q4u255w393l4a33663v514x2d3j43306e4460445a4t3a4s4e5c4h4u3z2y4762604o5h4c475j4m5p5\",\"expires\":false},\"userlimit\":{\"total\":30,\"user\":30},\"donator\":true,\"subscription\":{\"start\":\"2023-09-24 09:19:53\",\"end\":\"2060-10-24 09:19:53\",\"next\":0,\"trial\":0,\"end_date\":\"2698139293000\",\"selected\":{\"price\":450,\"id\":7489,\"for\":\"yearly\"}},\"productinfo\":{\"pricing\":[{\"price\":100,\"id\":7486,\"for\":\"monthly\"},{\"price\":250,\"id\":7487,\"for\":\"quarterly\"},{\"price\":450,\"id\":7489,\"for\":\"yearly\"}],\"features\":[\"Access to All Question's Answer\",\"Access to All Question Banks\",\"Access to All Practicals\",\"Access to All Viva Question\",\"Entrance Preparation\",\"No Irritating Advertisement\"]},\"data\":{\"username\":\"hacker\",\"name\":\"Hacker\",\"email\":\"welcome@fun.com\",\"profile\":\"https://secure.gravatar.com/avatar/a78e371049376b0772817629a99b758e?s=96&d=mm&r=g\",\"meta\":{\"playerID\":\"\",\"gender\":\"Male\",\"description\":\"\",\"youare\":\"Hacker\",\"college\":\"Just For Fun\",\"youtube\":\"https://youtube.com\",\"facebook\":\"https://facebook.com\",\"instagram\":\"https://instagram.com\",\"github\":\"https://github.com\",\"stackoverflow\":\"\",\"twitter\":\"https://twitter.com\",\"phone_number\":\"9800000000\",\"semester\":\"third\",\"fname\":\"Guest\",\"lname\":\"User\"},\"contribution\":{\"ans\":1,\"qns\":1,\"notes\":0,\"comments\":0,\"others\":0},\"plan\":{\"plan\":\"free\",\"points\":0,\"perday\":40,\"permonth\":1000}}}";
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {


                String result = (String) param.getResult();
                XposedBridge.log(result);

                if (result.contains("subscription")){
                    param.setResult( result.replace("\"subscription\":false", sub) );
                }
            }
        });

    }//init
}
