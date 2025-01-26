package com.realme.modxposed.hooks;

import android.util.Base64;

import com.realme.modxposed.IXposedHookLoadPackage;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookGame implements IXposedHookLoadPackage {
    boolean isHookRunning = false;
    @Override
    public void init(XC_LoadPackage.LoadPackageParam param) {
        XposedBridge.log("Loading the hok");
        XposedHelpers.findAndHookMethod("io.rong.imlib.common.EncryptUtil", param.classLoader,"decrypt",String.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (isHookRunning) return;
//
//                isHookRunning = true;
                String key = (String) XposedHelpers.getObjectField(param.thisObject,"key");
//
//                String str = new String( (char[]) param.args[0]);
//                isHookRunning = false;
                String hello = (String) param.getResult();
//                if (hello.contains("79989"))
                    XposedBridge.log(key);
                String cipherText = "6WtgLSOlbO+7GEISOYJwSehljwczC7VPxEODZznUO1CIXQfzwPKw7VTzjorrP63i36KKkt7KHKmyKAUJVGq0Tao8p4b2p2ePA3AsGg5euxiyNOSoFQ/2Z2Sno90Zensdg6IXWH6MlnqmpsjtHg1JN07CkipLiPvowaxLr792KJosKSL33VhiEH1j8kc3t+e8DrKbs1Ri0M6BgqM62H8zF6v0/isXFbX0HdZ2R1TLdGFjJ0mEtMK+InX1eHcamTEgUSvH3HX0LgNLYkQctzkom377XMzOzyHVTB5Xqfw78R7S+b/+2ShMBhsVg40i0RqDbAVA3kVKsLtlEFuiwEFjFz8VE5z4k8LiekVYFwaN/qj91CysDzyAXGgZYptPmIXwb4uaI2vgNKR8SnU+YxkmX99TKbHn7ElCWltd6tKUMvc4RxwEB1mFpAz8kw3gZEBafq14XRJHbdJQYO78Tpl7TuwhaBwMS+wi7aKonuG5y1GyhY08n6RzbdSD877xDd7sF0s5tFMYQ443bt5F3ZoULsp2prCr8B6c5pNX+D+r8RQgyIo3lRdgYF+UTEqHjkV/7JuuLXpzdvG1qDIHrmSQ6xSPjcbHjWYL51b7sAf1YZYKiZVw9eBrA8smxDiX+YPw0BVckONxqIvvTKjvHJovNYsFES/YlMfj5NYlUaQz8tbe5KWASw2xevUS3nEKTzBL6GWPBzMLtU/EQ4NnOdQ7UIhdB/PA8rDtVPOOius/reLfooqS3socqbIoBQlUarRNqjynhvanZ48DcCwaDl67GIGVHxS0nt+0Dr6qwAHvsrU/ywoWi4/65VOI6tKORAnfxVSJZ+UtIEHz5xGqZTvnmphD99NsBvco3UYPmgas6Jc0Z8dWoCYDWU24czJHWygCkOo3ilb5urZkK2qrQLSKwB+yStlGDvtYD0U7Rcun8EQmLeplwuMw8pjGugAzdcVHUXpUtiEx0L3TmvgsQFE/gNUkbfn/+ksIn+ffjK96CRqSIX8XXwDIoexwEFwgNDrkkUS1fIR80e4Wwa0Kq473XBwRYiMKdPKVOLa9vt9EzAq0bt3RQzwighnJbe03JtuCeJlJMd73RRwbpclapk1hU56lf7GTdD8cfHri+uAgSH2WHmes9sNiyexjnRL729/2tYnhJMT5D6suTCjBnLlSNz1iCL0HBJWmnY7fliO+AFC2Q7jqfzf2C47gluS8Cy0etZXzmsl9ICA0OZYaOKL4CL+v6L9+lvXZGVCHSasrsiu6Zd32n0oMKBgTlkzP9/fJUUi4KUqZoiyGiO004vUb5UcloUk5Pz6n3UBPeDfikCCc21LWd8MiRR65R0QrkNL+3ZtNTXgGphdACMoB8fSVPIo/NBoSj2dZBHsmHb5cFvkEanD7x1hMcLHfDnYm/johzsy+AGgltXPIGOZuSkhLT4xtauhTiwjhXShMuOJh7uBSwFFwlnskiyV9OScseJUpvPa/5gbc2m/jewBQB9RoWg==";
                key = "b78154139cbcfa18";
                try {
                    byte[] decode = Base64.decode(cipherText, 2);
                    Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                    cipher.init(2, new SecretKeySpec(key.getBytes(), "AES"), new IvParameterSpec(key.getBytes()));
                    String out =  new String(cipher.doFinal(decode));
                    XposedBridge.log(out);
                } catch (Exception e10) {
//                    RLog.i(TAG, "decrypt warning:" + e10.getMessage());
//                    return null;
                    XposedBridge.log(e10.toString());
                }

            }
        });//after
    }//init
}//class
