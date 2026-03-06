package com.realme.modxposed.hooks;

import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;


import com.realme.modxposed.IXposedHookLoadPackage;

import java.util.ArrayList;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class GestureNavigationView implements IXposedHookLoadPackage {

    @Override
    public void init(XC_LoadPackage.LoadPackageParam lpparam) {
        XposedBridge.log("yes cllaled");
//
//        XposedHelpers.findAndHookConstructor(
//                "com.oplus.systemui.navigationbar.gesture.sidegesture.OplusNavigationHandle",
//                lpparam.classLoader,
//                android.content.Context.class,
//                android.util.AttributeSet.class,
//                new XC_MethodHook() {
//                    @Override
//                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//                        // Get the original distance from bottom
//                        XposedBridge.log("yes cllaled");
//
//                        int originalBottom = XposedHelpers.getIntField(param.thisObject, "mHandleBottom");
//
//                        // Add 80 pixels to move it UP
//                        // (A higher 'bottom' value pushes the drawing logic higher)
//                        XposedHelpers.setIntField(param.thisObject, "mHandleBottom", originalBottom + 20);
//                    }
//                });

// ============================================================
// APPROACH A: Y-Coordinate Offset (Primary - handles Launcher's gesture detection)
// ============================================================
// Target: com.android.systemui package

        final int EXTRA_HEIGHT = 200; // extra pixels to expand the gesture zone

        XposedHelpers.findAndHookMethod(
                "com.android.systemui.navigationbar.gestural.EdgeBackGestureHandler",
                lpparam.classLoader,
                "onInputEvent$1",
                android.view.InputEvent.class,
                new XC_MethodHook() {
                    boolean offsetActive = false;
                    float yDelta = 0f;

                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!(param.args[0] instanceof android.view.MotionEvent)) return;
                        android.view.MotionEvent me = (android.view.MotionEvent) param.args[0];

                        android.graphics.Point displaySize = (android.graphics.Point)
                                XposedHelpers.getObjectField(param.thisObject, "mDisplaySize");
                        int screenHeight = displaySize.y;

                        // Get the actual original zone height from SideGestureConfiguration
                        Object sideGestureConfig = XposedHelpers.getObjectField(param.thisObject, "mSideGestureConfiguration");
                        int originalZone = (int) XposedHelpers.callMethod(sideGestureConfig, "getBottomGestureAreaHeight");
                        int expandedZone = originalZone + EXTRA_HEIGHT;

                        int action = me.getActionMasked();
                        float rawY = me.getRawY();

                        if (action == android.view.MotionEvent.ACTION_DOWN) {
                            if (rawY >= screenHeight - expandedZone && rawY < screenHeight - originalZone) {
                                yDelta = (screenHeight - originalZone + 1) - rawY;
                                offsetActive = true;
                                XposedBridge.log("GestureExpand: DOWN at Y=" + rawY + ", offsetting by " + yDelta
                                        + " (screenH=" + screenHeight + ", origZone=" + originalZone + ")");
                            } else {
                                offsetActive = false;
                                yDelta = 0f;
                            }
                        }

                        if (offsetActive) {
                            me.offsetLocation(0, yDelta);
                        }

                        if (action == android.view.MotionEvent.ACTION_UP || action == android.view.MotionEvent.ACTION_CANCEL) {
                            offsetActive = false;
                        }
                    }
                }
        );

// ============================================================
// APPROACH B: Force SystemUI to handle bottom gestures
// (Makes previous mBottomGestureAreaHeight hooks work too)
// ============================================================

        XposedHelpers.findAndHookMethod(
                "com.oplus.systemui.navigationbar.utils.SupportOutUtils",
                lpparam.classLoader,
                "isSystemUiSupportClearBottomGestureArea",
                android.content.Context.class,
                XC_MethodReplacement.returnConstant(false)
        );

// ============================================================
// APPROACH B continued: Increase the bottom gesture area height
// (Only effective when Approach B above is applied)
// ============================================================

        XposedHelpers.findAndHookMethod(
                "com.oplus.systemui.navigationbar.gesture.sidegesture.SideGestureConfiguration",
                lpparam.classLoader,
                "updateGestureBottomArea",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        int original = XposedHelpers.getIntField(param.thisObject, "mBottomGestureAreaHeight");
                        int increased = original + 40;
                        XposedHelpers.setIntField(param.thisObject, "mBottomGestureAreaHeight", increased);
                        XposedBridge.log("updateGestureBottomArea: " + original + " -> " + increased);
                    }
                }
        );




    }//init

}//class

