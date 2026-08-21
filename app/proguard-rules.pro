# Keep Xposed entry point and hook classes from being obfuscated or stripped by R8
-keep class com.realme.modxposed.MainXposedHookEntry { *; }
-keep class com.realme.modxposed.hooks.** { *; }
-keep class de.robv.android.xposed.** { *; }
-keep interface de.robv.android.xposed.** { *; }
-keep class com.realme.modxposed.model.** { *; }
-keep class com.realme.modxposed.prefs.** { *; }
-keep class com.realme.modxposed.utils.** { *; }