package com.realme.modxposed.hooks;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.realme.modxposed.IXposedHookLoadPackage;

import java.io.File;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Supercharged In-App Inspector & Debug Overlay:
 * - SharedPreferences Explorer (All + Per-File + Search + Copy)
 * - SQLite / Room Database Explorer (2-Tier DB & Table Selectors + Rows)
 * - Live Intent & Broadcast Interceptor
 * - Dynamic JSON / Gson Serialization Logger
 * - Background Threaded Loading + Debounced Instant Search + Loading State Indicator
 */
public class SharedPrefsInspector implements IXposedHookLoadPackage {

    private static final String TAG = "AppInspector";
    private static final String ALL_PREFS_KEY = "__ALL__";
    private static final int MAX_BUFFER_SIZE = 100;
    private static final int MAX_DISPLAY_ITEMS = 200;

    // Tabs
    private enum Tab { PREFERENCES, DATABASE, INTENTS, JSON_LOGS }
    private static Tab activeTab = Tab.PREFERENCES;

    // SharedPrefs Storage
    private static final Set<String> PREF_FILES = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static String selectedPrefFile = ALL_PREFS_KEY;

    // Database Storage
    private static final Set<String> DB_FILES = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static String selectedDbFile = null;
    private static String selectedDbTable = null;

    // Intent Logging Storage
    public static class IntentLogItem {
        public final String timestamp;
        public final String type;
        public final String action;
        public final String component;
        public final String dataUri;
        public final String extrasSummary;

        public IntentLogItem(String type, String action, String component, String dataUri, String extrasSummary) {
            this.timestamp = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(new Date());
            this.type = type;
            this.action = action;
            this.component = component;
            this.dataUri = dataUri;
            this.extrasSummary = extrasSummary;
        }
    }
    private static final List<IntentLogItem> INTENT_LOGS = new CopyOnWriteArrayList<>();

    // JSON Logging Storage
    public static class JsonLogItem {
        public final String timestamp;
        public final String source;
        public final String targetClass;
        public final String payload;

        public JsonLogItem(String source, String targetClass, String payload) {
            this.timestamp = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(new Date());
            this.source = source;
            this.targetClass = targetClass;
            this.payload = payload;
        }
    }
    private static final List<JsonLogItem> JSON_LOGS = new CopyOnWriteArrayList<>();

    // Threading & Search Debouncing
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private static final ExecutorService BG_EXECUTOR = Executors.newSingleThreadExecutor();
    private static final AtomicLong QUERY_TOKEN = new AtomicLong(0);
    private static final Runnable SEARCH_DEBOUNCE_RUNNABLE = new Runnable() {
        @Override
        public void run() {
            Activity act = currentActivityRef.get();
            if (act != null && isPanelOpen) {
                renderEntries(act);
            }
        }
    };

    // Lightweight UI Card Model computed off-thread
    private static class EntryCardModel {
        public final String sourceBadge;
        public final String title;
        public final String body;
        public final String type;
        public final int color;
        public final boolean isPref;

        public EntryCardModel(String sourceBadge, String title, String body, String type, int color, boolean isPref) {
            this.sourceBadge = sourceBadge;
            this.title = title;
            this.body = body;
            this.type = type;
            this.color = color;
            this.isPref = isPref;
        }
    }

    // Floating UI state preserved across activity transitions
    private static float floatingX = -1;
    private static float floatingY = -1;
    private static boolean isPanelOpen = false;
    private static String currentSearchFilter = "";

    private static WeakReference<Activity> currentActivityRef = new WeakReference<>(null);
    private static View floatingRootView = null;
    private static LinearLayout panelContainer = null;
    private static LinearLayout entriesListContainer = null;
    private static LinearLayout subHeaderContainer = null;
    private static TextView headerSubtitle = null;
    private static LinearLayout tabsContainer = null;

    private static XSharedPreferences prefs;
    private static synchronized XSharedPreferences getPrefs() {
        if (prefs == null) {
            prefs = new XSharedPreferences("com.realme.modxposed", "settings");
            prefs.makeWorldReadable();
        } else {
            prefs.reload();
        }
        return prefs;
    }

    private static boolean isFeatureEnabled(String key) {
        try {
            XSharedPreferences p = getPrefs();
            if (p != null) return p.getBoolean(key, true);
        } catch (Throwable ignored) {}
        return true;
    }

    @Override
    public void init(XC_LoadPackage.LoadPackageParam lpparam) {
        XposedBridge.log(TAG + ": Initializing Supercharged Inspector for " + lpparam.packageName);

        // 1. SharedPreferences Hook (if enabled)
        if (isFeatureEnabled("inspector_hook_shared_prefs")) {
            try {
                XposedHelpers.findAndHookMethod(
                    "android.app.ContextImpl",
                    lpparam.classLoader,
                    "getSharedPreferences",
                    String.class,
                    int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            String name = (String) param.args[0];
                            if (name != null && !name.trim().isEmpty()) {
                                if (PREF_FILES.add(name)) {
                                    updateUiAsync();
                                }
                            }
                        }
                    }
                );
            } catch (Throwable t) {
                XposedBridge.log(TAG + " Error hooking getSharedPreferences: " + t.getMessage());
            }
        }

        // 2. Intent & Broadcast Interceptors (if enabled)
        if (isFeatureEnabled("inspector_hook_intents")) {
            hookIntents(lpparam);
        }

        // 3. Dynamic JSON / Gson Serialization Loggers (if enabled)
        if (isFeatureEnabled("inspector_hook_json")) {
            hookJsonSerializers(lpparam);
        }

        // 4. Activity Lifecycle Hooks for In-App Overlay UI (if enabled)
        if (isFeatureEnabled("inspector_hook_overlay")) {
            try {
                XposedHelpers.findAndHookMethod(
                    "android.app.Activity",
                    lpparam.classLoader,
                    "onPostResume",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            Activity activity = (Activity) param.thisObject;
                            currentActivityRef = new WeakReference<>(activity);

                            if (isFeatureEnabled("inspector_hook_shared_prefs")) {
                                scanDiskPreferences(activity);
                            }
                            if (isFeatureEnabled("inspector_hook_database")) {
                                scanDiskDatabases(activity);
                            }

                            MAIN_HANDLER.post(() -> attachOverlayToActivity(activity));
                        }
                    }
                );

                XposedHelpers.findAndHookMethod(
                    "android.app.Activity",
                    lpparam.classLoader,
                    "onPause",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            Activity activity = (Activity) param.thisObject;
                            detachOverlayFromActivity(activity);
                        }
                    }
                );
            } catch (Throwable t) {
                XposedBridge.log(TAG + " Error hooking Activity lifecycle: " + t.getMessage());
            }
        }
    }

    // =========================================================================
    // Hooking: Intents & Broadcasts
    // =========================================================================

    private void hookIntents(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            XposedHelpers.findAndHookMethod(
                "android.app.Activity",
                lpparam.classLoader,
                "startActivity",
                Intent.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        recordIntentLog("START_ACTIVITY", (Intent) param.args[0]);
                    }
                }
            );

            XposedHelpers.findAndHookMethod(
                "android.app.ContextImpl",
                lpparam.classLoader,
                "sendBroadcast",
                Intent.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        recordIntentLog("BROADCAST", (Intent) param.args[0]);
                    }
                }
            );

            XposedHelpers.findAndHookMethod(
                "android.app.ContextImpl",
                lpparam.classLoader,
                "startService",
                Intent.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        recordIntentLog("SERVICE", (Intent) param.args[0]);
                    }
                }
            );
        } catch (Throwable t) {
            XposedBridge.log(TAG + " Error hooking Intent methods: " + t.getMessage());
        }
    }

    private static void recordIntentLog(String type, Intent intent) {
        if (intent == null) return;
        try {
            String action = intent.getAction() != null ? intent.getAction() : "None";
            ComponentName cn = intent.getComponent();
            String component = cn != null ? cn.flattenToShortString() : "None";
            Uri data = intent.getData();
            String dataUri = data != null ? data.toString() : "";

            StringBuilder extras = new StringBuilder();
            Bundle b = intent.getExtras();
            if (b != null) {
                for (String k : b.keySet()) {
                    Object val = b.get(k);
                    extras.append(k).append("=").append(val).append("\n");
                }
            }

            IntentLogItem item = new IntentLogItem(type, action, component, dataUri, extras.toString().trim());
            if (INTENT_LOGS.size() >= MAX_BUFFER_SIZE) {
                INTENT_LOGS.remove(0);
            }
            INTENT_LOGS.add(item);

            if (activeTab == Tab.INTENTS) {
                updateUiAsync();
            }
        } catch (Throwable ignored) {
        }
    }

    // =========================================================================
    // Hooking: JSON / Gson Serialization
    // =========================================================================

    private void hookJsonSerializers(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> gsonClass = XposedHelpers.findClassIfExists("com.google.gson.Gson", lpparam.classLoader);
            if (gsonClass != null) {
                XposedHelpers.findAndHookMethod(
                    gsonClass,
                    "fromJson",
                    String.class,
                    Class.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            String json = (String) param.args[0];
                            Class<?> clz = (Class<?>) param.args[1];
                            if (json != null && json.length() > 2) {
                                recordJsonLog("GSON_FROM", clz != null ? clz.getSimpleName() : "Unknown", json);
                            }
                        }
                    }
                );

                XposedHelpers.findAndHookMethod(
                    gsonClass,
                    "toJson",
                    Object.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            String result = (String) param.getResult();
                            Object src = param.args[0];
                            if (result != null && result.length() > 2) {
                                recordJsonLog("GSON_TO", src != null ? src.getClass().getSimpleName() : "Object", result);
                            }
                        }
                    }
                );
            }
        } catch (Throwable ignored) {
        }

        try {
            XposedHelpers.findAndHookConstructor(
                "org.json.JSONObject",
                lpparam.classLoader,
                String.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        String json = (String) param.args[0];
                        if (json != null && json.length() > 2) {
                            recordJsonLog("JSON_OBJECT", "JSONObject", json);
                        }
                    }
                }
            );
        } catch (Throwable ignored) {
        }
    }

    private static void recordJsonLog(String source, String targetClass, String payload) {
        try {
            JsonLogItem item = new JsonLogItem(source, targetClass, payload);
            if (JSON_LOGS.size() >= MAX_BUFFER_SIZE) {
                JSON_LOGS.remove(0);
            }
            JSON_LOGS.add(item);

            if (activeTab == Tab.JSON_LOGS) {
                updateUiAsync();
            }
        } catch (Throwable ignored) {
        }
    }

    // =========================================================================
    // Auto-Discovery: Preferences & Databases
    // =========================================================================

    private static void scanDiskPreferences(Context context) {
        try {
            File dataDir = context.getDataDir();
            File prefsDir = new File(dataDir, "shared_prefs");
            if (prefsDir.exists() && prefsDir.isDirectory()) {
                File[] files = prefsDir.listFiles((dir, name) -> name.endsWith(".xml"));
                if (files != null) {
                    boolean hasNew = false;
                    for (File f : files) {
                        String prefName = f.getName().replace(".xml", "");
                        if (PREF_FILES.add(prefName)) {
                            hasNew = true;
                        }
                    }
                    if (hasNew && activeTab == Tab.PREFERENCES) {
                        updateUiAsync();
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static void scanDiskDatabases(Context context) {
        try {
            File dataDir = context.getDataDir();
            File dbDir = new File(dataDir, "databases");
            if (dbDir.exists() && dbDir.isDirectory()) {
                File[] files = dbDir.listFiles((dir, name) -> 
                    !name.endsWith("-journal") && 
                    !name.endsWith("-wal") && 
                    !name.endsWith("-shm") && 
                    !name.endsWith(".lock") &&
                    !name.equals("android_metadata")
                );
                if (files != null) {
                    boolean hasNew = false;
                    for (File f : files) {
                        if (DB_FILES.add(f.getName())) {
                            hasNew = true;
                        }
                    }
                    if (hasNew && activeTab == Tab.DATABASE) {
                        updateUiAsync();
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static void updateUiAsync() {
        MAIN_HANDLER.post(() -> {
            Activity act = currentActivityRef.get();
            if (act != null && isPanelOpen) {
                renderSubHeaders(act);
                renderEntries(act);
            }
        });
    }

    // =========================================================================
    // UI Construction & Overlay Logic (Pure Programmatic, 0 XML Dependencies)
    // =========================================================================

    private static void attachOverlayToActivity(Activity activity) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;

        ViewGroup decor = (ViewGroup) activity.getWindow().getDecorView();
        if (decor == null) return;

        detachOverlayFromActivity(activity);

        DisplayMetrics dm = activity.getResources().getDisplayMetrics();
        int screenWidth = dm.widthPixels;
        int screenHeight = dm.heightPixels;

        if (floatingX < 0 || floatingY < 0) {
            floatingX = screenWidth - dp(activity, 68);
            floatingY = screenHeight / 3f;
        }

        FrameLayout rootLayout = new FrameLayout(activity);
        floatingRootView = rootLayout;

        FrameLayout bubble = createFloatingBubble(activity, screenWidth, screenHeight);
        panelContainer = createInspectorPanel(activity);
        panelContainer.setVisibility(isPanelOpen ? View.VISIBLE : View.GONE);

        rootLayout.addView(panelContainer);
        rootLayout.addView(bubble);

        decor.addView(rootLayout, new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ));

        if (isPanelOpen) {
            renderTabs(activity);
            renderSubHeaders(activity);
            renderEntries(activity);
        }
    }

    private static void detachOverlayFromActivity(Activity activity) {
        if (activity == null) return;
        try {
            ViewGroup decor = (ViewGroup) activity.getWindow().getDecorView();
            if (decor != null && floatingRootView != null) {
                decor.removeView(floatingRootView);
            }
        } catch (Throwable ignored) {
        }
        floatingRootView = null;
    }

    private static FrameLayout createFloatingBubble(Activity activity, int screenWidth, int screenHeight) {
        int size = dp(activity, 54);
        FrameLayout bubble = new FrameLayout(activity);

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColors(new int[]{0xFF0D47A1, 0xFF00E5FF}); // Cyber cyan/blue gradient
        bg.setGradientType(GradientDrawable.LINEAR_GRADIENT);
        bg.setStroke(dp(activity, 2), 0x80FFFFFF);
        bubble.setBackground(bg);
        bubble.setElevation(dp(activity, 14));

        TextView tv = new TextView(activity);
        tv.setText("⚡DEV");
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setGravity(Gravity.CENTER);

        FrameLayout.LayoutParams tvLp = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        );
        bubble.addView(tv, tvLp);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(size, size);
        lp.gravity = Gravity.TOP | Gravity.START;
        bubble.setLayoutParams(lp);

        bubble.setTranslationX(floatingX);
        bubble.setTranslationY(floatingY);

        bubble.setOnTouchListener(new View.OnTouchListener() {
            private float startRawX, startRawY;
            private float initialX, initialY;
            private boolean isDragging = false;

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        startRawX = event.getRawX();
                        startRawY = event.getRawY();
                        initialX = floatingX;
                        initialY = floatingY;
                        isDragging = false;
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        float deltaX = event.getRawX() - startRawX;
                        float deltaY = event.getRawY() - startRawY;
                        if (Math.hypot(deltaX, deltaY) > 10) {
                            isDragging = true;
                        }
                        if (isDragging) {
                            floatingX = Math.max(0, Math.min(screenWidth - size, initialX + deltaX));
                            floatingY = Math.max(0, Math.min(screenHeight - size, initialY + deltaY));
                            view.setTranslationX(floatingX);
                            view.setTranslationY(floatingY);
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                        if (!isDragging) {
                            togglePanel(activity);
                        }
                        return true;
                }
                return false;
            }
        });

        return bubble;
    }

    private static void togglePanel(Activity activity) {
        isPanelOpen = !isPanelOpen;
        if (panelContainer != null) {
            panelContainer.setVisibility(isPanelOpen ? View.VISIBLE : View.GONE);
            if (isPanelOpen) {
                renderTabs(activity);
                renderSubHeaders(activity);
                renderEntries(activity);
            }
        }
    }

    private static LinearLayout createInspectorPanel(Activity activity) {
        LinearLayout panel = new LinearLayout(activity);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setElevation(dp(activity, 20));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xF410111D); // Dark OLED translucent glass
        bg.setCornerRadius(dp(activity, 18));
        bg.setStroke(dp(activity, 1), 0x33FFFFFF);
        panel.setBackground(bg);
        panel.setPadding(dp(activity, 12), dp(activity, 12), dp(activity, 12), dp(activity, 12));

        int screenHeight = activity.getResources().getDisplayMetrics().heightPixels;
        int panelHeight = (int) (screenHeight * 0.78f);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            panelHeight
        );
        lp.gravity = Gravity.BOTTOM;
        lp.leftMargin = dp(activity, 10);
        lp.rightMargin = dp(activity, 10);
        lp.bottomMargin = dp(activity, 14);
        panel.setLayoutParams(lp);

        // Header Row
        LinearLayout header = new LinearLayout(activity);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout titleBlock = new LinearLayout(activity);
        titleBlock.setOrientation(LinearLayout.VERTICAL);
        titleBlock.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView title = new TextView(activity);
        title.setText("⚡ App Inspector Hub");
        title.setTextColor(0xFF00E5FF);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        title.setTypeface(Typeface.DEFAULT_BOLD);

        headerSubtitle = new TextView(activity);
        headerSubtitle.setText("Live Analysis Hub");
        headerSubtitle.setTextColor(0xFF8888AA);
        headerSubtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);

        titleBlock.addView(title);
        titleBlock.addView(headerSubtitle);

        TextView btnRefresh = createIconBtn(activity, "🔄", 0xFF2196F3, () -> {
            scanDiskPreferences(activity);
            scanDiskDatabases(activity);
            renderSubHeaders(activity);
            renderEntries(activity);
            Toast.makeText(activity, "Refreshed Data", Toast.LENGTH_SHORT).show();
        });

        TextView btnClear = createIconBtn(activity, "🗑️", 0xFF455A64, () -> {
            if (activeTab == Tab.INTENTS) INTENT_LOGS.clear();
            if (activeTab == Tab.JSON_LOGS) JSON_LOGS.clear();
            renderEntries(activity);
            Toast.makeText(activity, "Cleared Logs", Toast.LENGTH_SHORT).show();
        });

        TextView btnClose = createIconBtn(activity, "✕", 0xFFE53935, () -> togglePanel(activity));

        header.addView(titleBlock);
        header.addView(btnRefresh);
        header.addView(btnClear);
        header.addView(btnClose);
        panel.addView(header);

        // Top Navigation Tabs (Preferences, Database, Intents, JSON)
        HorizontalScrollView tabScroll = new HorizontalScrollView(activity);
        tabScroll.setHorizontalScrollBarEnabled(false);
        tabsContainer = new LinearLayout(activity);
        tabsContainer.setOrientation(LinearLayout.HORIZONTAL);
        tabScroll.addView(tabsContainer);

        LinearLayout.LayoutParams tabScrollLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        tabScrollLp.topMargin = dp(activity, 8);
        tabScrollLp.bottomMargin = dp(activity, 8);
        panel.addView(tabScroll, tabScrollLp);

        // Search Bar with 200ms Debounce
        EditText searchBar = new EditText(activity);
        searchBar.setHint("🔍 Search across all keys, tables, logs...");
        searchBar.setHintTextColor(0xFF666688);
        searchBar.setTextColor(Color.WHITE);
        searchBar.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        searchBar.setSingleLine(true);
        searchBar.setPadding(dp(activity, 10), dp(activity, 8), dp(activity, 10), dp(activity, 8));

        GradientDrawable searchBg = new GradientDrawable();
        searchBg.setColor(0xFF1E1E2F);
        searchBg.setCornerRadius(dp(activity, 10));
        searchBg.setStroke(dp(activity, 1), 0x22FFFFFF);
        searchBar.setBackground(searchBg);

        LinearLayout.LayoutParams searchLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        searchLp.bottomMargin = dp(activity, 8);
        panel.addView(searchBar, searchLp);

        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchFilter = s.toString().trim().toLowerCase();
                MAIN_HANDLER.removeCallbacks(SEARCH_DEBOUNCE_RUNNABLE);
                MAIN_HANDLER.postDelayed(SEARCH_DEBOUNCE_RUNNABLE, 200);
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Dedicated Subheader Container
        subHeaderContainer = new LinearLayout(activity);
        subHeaderContainer.setOrientation(LinearLayout.VERTICAL);
        panel.addView(subHeaderContainer);

        // Entries Scroll View
        ScrollView entriesScroll = new ScrollView(activity);
        entriesScroll.setVerticalScrollBarEnabled(true);
        entriesListContainer = new LinearLayout(activity);
        entriesListContainer.setOrientation(LinearLayout.VERTICAL);
        entriesScroll.addView(entriesListContainer);

        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            0,
            1.0f
        );
        panel.addView(entriesScroll, scrollLp);

        return panel;
    }

    // =========================================================================
    // Tab Navigation & Subheader Rendering
    // =========================================================================

    private static void renderTabs(Activity activity) {
        if (tabsContainer == null) return;
        tabsContainer.removeAllViews();

        boolean hasPref = isFeatureEnabled("inspector_hook_shared_prefs");
        boolean hasDb = isFeatureEnabled("inspector_hook_database");
        boolean hasIntents = isFeatureEnabled("inspector_hook_intents");
        boolean hasJson = isFeatureEnabled("inspector_hook_json");

        if (hasPref) {
            tabsContainer.addView(createTabBtn(activity, "📁 SharedPrefs (" + PREF_FILES.size() + ")", Tab.PREFERENCES, 0xFF007ACC));
        }
        if (hasDb) {
            tabsContainer.addView(createTabBtn(activity, "🗄️ Databases (" + DB_FILES.size() + ")", Tab.DATABASE, 0xFF00897B));
        }
        if (hasIntents) {
            tabsContainer.addView(createTabBtn(activity, "📡 Intents (" + INTENT_LOGS.size() + ")", Tab.INTENTS, 0xFFF57C00));
        }
        if (hasJson) {
            tabsContainer.addView(createTabBtn(activity, "📦 JSON / Gson (" + JSON_LOGS.size() + ")", Tab.JSON_LOGS, 0xFF8E24AA));
        }
    }

    private static TextView createTabBtn(Activity activity, String label, Tab tab, int activeColor) {
        boolean isSelected = activeTab == tab;
        TextView btn = new TextView(activity);
        btn.setText(label);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        btn.setTypeface(isSelected ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        btn.setTextColor(isSelected ? Color.WHITE : 0xFFAAAAAA);
        btn.setPadding(dp(activity, 10), dp(activity, 5), dp(activity, 10), dp(activity, 5));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(isSelected ? activeColor : 0xFF1C1D2A);
        bg.setCornerRadius(dp(activity, 8));
        if (isSelected) bg.setStroke(dp(activity, 1), 0xFF00E5FF);
        btn.setBackground(bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.rightMargin = dp(activity, 6);
        btn.setLayoutParams(lp);

        btn.setOnClickListener(v -> {
            activeTab = tab;
            renderTabs(activity);
            renderSubHeaders(activity);
            renderEntries(activity);
        });
        return btn;
    }

    private static void renderSubHeaders(Activity activity) {
        if (subHeaderContainer == null) return;
        subHeaderContainer.removeAllViews();

        switch (activeTab) {
            case PREFERENCES:
                renderPreferenceChips(activity);
                break;
            case DATABASE:
                renderDatabase2TierSelectors(activity);
                break;
            case INTENTS:
            case JSON_LOGS:
                subHeaderContainer.setVisibility(View.GONE);
                break;
        }
    }

    private static void renderPreferenceChips(Activity activity) {
        if (subHeaderContainer == null) return;
        subHeaderContainer.removeAllViews();
        subHeaderContainer.setVisibility(View.VISIBLE);

        HorizontalScrollView chipScroll = new HorizontalScrollView(activity);
        chipScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout chips = new LinearLayout(activity);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        chipScroll.addView(chips);

        boolean isAllSelected = ALL_PREFS_KEY.equals(selectedPrefFile);
        TextView allChip = createChip(
            activity,
            "🌟 ALL (" + PREF_FILES.size() + " files)",
            isAllSelected,
            0xFF8E24AA,
            v -> {
                selectedPrefFile = ALL_PREFS_KEY;
                renderPreferenceChips(activity);
                renderEntries(activity);
            }
        );
        chips.addView(allChip);

        List<String> sortedList = new ArrayList<>(PREF_FILES);
        Collections.sort(sortedList);

        for (String prefName : sortedList) {
            boolean isSelected = prefName.equals(selectedPrefFile);
            TextView chip = createChip(
                activity,
                prefName,
                isSelected,
                0xFF007ACC,
                v -> {
                    selectedPrefFile = prefName;
                    renderPreferenceChips(activity);
                    renderEntries(activity);
                }
            );
            chips.addView(chip);
        }

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.bottomMargin = dp(activity, 8);
        subHeaderContainer.addView(chipScroll, lp);
    }

    private static void renderDatabase2TierSelectors(Activity activity) {
        if (subHeaderContainer == null) return;
        subHeaderContainer.removeAllViews();
        subHeaderContainer.setVisibility(View.VISIBLE);

        if (DB_FILES.isEmpty()) {
            TextView empty = new TextView(activity);
            empty.setText("No SQLite databases detected in /databases directory");
            empty.setTextColor(0xFF8888AA);
            empty.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            empty.setPadding(0, dp(activity, 4), 0, dp(activity, 8));
            subHeaderContainer.addView(empty);
            return;
        }

        List<String> sortedDbs = new ArrayList<>(DB_FILES);
        Collections.sort(sortedDbs);

        if (selectedDbFile == null || !DB_FILES.contains(selectedDbFile)) {
            selectedDbFile = sortedDbs.get(0);
            selectedDbTable = null;
        }

        // TIER 1: DATABASE FILE ROW
        LinearLayout dbRow = new LinearLayout(activity);
        dbRow.setOrientation(LinearLayout.HORIZONTAL);
        dbRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView dbLabel = new TextView(activity);
        dbLabel.setText("🗄️ DB:");
        dbLabel.setTextColor(0xFF00E5FF);
        dbLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        dbLabel.setTypeface(Typeface.DEFAULT_BOLD);
        dbLabel.setPadding(0, 0, dp(activity, 6), 0);
        dbRow.addView(dbLabel);

        HorizontalScrollView dbScroll = new HorizontalScrollView(activity);
        dbScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout dbChips = new LinearLayout(activity);
        dbChips.setOrientation(LinearLayout.HORIZONTAL);
        dbScroll.addView(dbChips);

        for (String dbName : sortedDbs) {
            boolean isSelected = dbName.equals(selectedDbFile);
            TextView chip = createChip(
                activity,
                dbName,
                isSelected,
                0xFF00897B,
                v -> {
                    selectedDbFile = dbName;
                    selectedDbTable = null;
                    renderDatabase2TierSelectors(activity);
                    renderEntries(activity);
                }
            );
            dbChips.addView(chip);
        }
        dbRow.addView(dbScroll);

        LinearLayout.LayoutParams dbRowLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        dbRowLp.bottomMargin = dp(activity, 6);
        subHeaderContainer.addView(dbRow, dbRowLp);

        // Fetch tables for the selected DB
        List<String> tables = getTablesForDb(activity, selectedDbFile);
        if (tables.isEmpty()) {
            TextView noTables = new TextView(activity);
            noTables.setText("  No tables found in " + selectedDbFile);
            noTables.setTextColor(0xFF8888AA);
            noTables.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            subHeaderContainer.addView(noTables);
            return;
        }

        if (selectedDbTable == null || !tables.contains(selectedDbTable)) {
            selectedDbTable = tables.get(0);
        }

        // TIER 2: TABLES ROW
        LinearLayout tableRow = new LinearLayout(activity);
        tableRow.setOrientation(LinearLayout.HORIZONTAL);
        tableRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView tableLabel = new TextView(activity);
        tableLabel.setText("📋 Table:");
        tableLabel.setTextColor(0xFFFFB74D);
        tableLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tableLabel.setTypeface(Typeface.DEFAULT_BOLD);
        tableLabel.setPadding(0, 0, dp(activity, 6), 0);
        tableRow.addView(tableLabel);

        HorizontalScrollView tableScroll = new HorizontalScrollView(activity);
        tableScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout tableChips = new LinearLayout(activity);
        tableChips.setOrientation(LinearLayout.HORIZONTAL);
        tableScroll.addView(tableChips);

        for (String tbl : tables) {
            boolean isSelected = tbl.equals(selectedDbTable);
            TextView chip = createChip(
                activity,
                tbl,
                isSelected,
                0xFFF57C00,
                v -> {
                    selectedDbTable = tbl;
                    renderDatabase2TierSelectors(activity);
                    renderEntries(activity);
                }
            );
            tableChips.addView(chip);
        }
        tableRow.addView(tableScroll);

        LinearLayout.LayoutParams tblRowLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        tblRowLp.bottomMargin = dp(activity, 8);
        subHeaderContainer.addView(tableRow, tblRowLp);
    }

    private static List<String> getTablesForDb(Activity activity, String dbName) {
        List<String> tables = new ArrayList<>();
        if (dbName == null) return tables;

        SQLiteDatabase db = null;
        try {
            File dbPath = activity.getDatabasePath(dbName);
            if (dbPath.exists()) {
                db = SQLiteDatabase.openDatabase(dbPath.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
                Cursor cursor = db.rawQuery(
                    "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'android_metadata' ORDER BY name ASC",
                    null
                );
                while (cursor.moveToNext()) {
                    tables.add(cursor.getString(0));
                }
                cursor.close();
            }
        } catch (Throwable ignored) {
        } finally {
            if (db != null && db.isOpen()) db.close();
        }
        return tables;
    }

    private static TextView createChip(Activity activity, String label, boolean isSelected, int activeColor, View.OnClickListener onClick) {
        TextView chip = new TextView(activity);
        chip.setText(label);
        chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        chip.setTypeface(isSelected ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        chip.setTextColor(isSelected ? Color.WHITE : 0xFFAAAAAA);
        chip.setPadding(dp(activity, 10), dp(activity, 5), dp(activity, 10), dp(activity, 5));

        GradientDrawable chipBg = new GradientDrawable();
        chipBg.setColor(isSelected ? activeColor : 0xFF222238);
        chipBg.setCornerRadius(dp(activity, 10));
        if (isSelected) {
            chipBg.setStroke(dp(activity, 1), 0xFF00E5FF);
        }
        chip.setBackground(chipBg);

        LinearLayout.LayoutParams chipLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        chipLp.rightMargin = dp(activity, 6);
        chip.setLayoutParams(chipLp);
        chip.setOnClickListener(onClick);
        return chip;
    }

    // =========================================================================
    // Asynchronous Content Loading & Rendering Engine
    // =========================================================================

    private static void renderEntries(Activity activity) {
        if (entriesListContainer == null) return;
        entriesListContainer.removeAllViews();

        // 1. Show immediate sleek loading state
        showLoadingState(activity);

        final long token = QUERY_TOKEN.incrementAndGet();
        final Tab currentTab = activeTab;
        final String searchFilter = currentSearchFilter;
        final String currentPrefFile = selectedPrefFile;
        final String currentDb = selectedDbFile;
        final String currentTable = selectedDbTable;

        // 2. Offload heavy disk / JSON / SP reading and filtering to Background Thread
        BG_EXECUTOR.submit(() -> {
            List<EntryCardModel> resultCards = new ArrayList<>();
            String subtitleText = "";
            String topHeaderBanner = null;

            try {
                switch (currentTab) {
                    case PREFERENCES: {
                        if (ALL_PREFS_KEY.equals(currentPrefFile)) {
                            List<String> sortedFiles = new ArrayList<>(PREF_FILES);
                            Collections.sort(sortedFiles);

                            int total = 0;
                            for (String file : sortedFiles) {
                                try {
                                    SharedPreferences sp = activity.getSharedPreferences(file, Context.MODE_PRIVATE);
                                    Map<String, ?> all = sp.getAll();
                                    for (Map.Entry<String, ?> entry : all.entrySet()) {
                                        String key = entry.getKey();
                                        Object val = entry.getValue();
                                        String valStr = (val == null) ? "null" : String.valueOf(val);
                                        String typeStr = (val == null) ? "NULL" : val.getClass().getSimpleName().toUpperCase();

                                        if (!TextUtils.isEmpty(searchFilter)) {
                                            boolean matchesFile = file.toLowerCase().contains(searchFilter);
                                            boolean matchesKey = key.toLowerCase().contains(searchFilter);
                                            boolean matchesVal = valStr.toLowerCase().contains(searchFilter);
                                            if (!matchesFile && !matchesKey && !matchesVal) continue;
                                        }

                                        total++;
                                        if (resultCards.size() < MAX_DISPLAY_ITEMS) {
                                            resultCards.add(new EntryCardModel(
                                                "📁 " + file,
                                                key,
                                                valStr,
                                                typeStr,
                                                getTypeColor(typeStr),
                                                true
                                            ));
                                        }
                                    }
                                } catch (Throwable ignored) {}
                            }
                            subtitleText = "Showing " + total + " keys across " + PREF_FILES.size() + " files";
                        } else {
                            try {
                                SharedPreferences sp = activity.getSharedPreferences(currentPrefFile, Context.MODE_PRIVATE);
                                Map<String, ?> all = sp.getAll();
                                int total = 0;

                                for (Map.Entry<String, ?> entry : all.entrySet()) {
                                    String key = entry.getKey();
                                    Object val = entry.getValue();
                                    String valStr = (val == null) ? "null" : String.valueOf(val);
                                    String typeStr = (val == null) ? "NULL" : val.getClass().getSimpleName().toUpperCase();

                                    if (!TextUtils.isEmpty(searchFilter)) {
                                        boolean matchesKey = key.toLowerCase().contains(searchFilter);
                                        boolean matchesVal = valStr.toLowerCase().contains(searchFilter);
                                        if (!matchesKey && !matchesVal) continue;
                                    }

                                    total++;
                                    if (resultCards.size() < MAX_DISPLAY_ITEMS) {
                                        resultCards.add(new EntryCardModel(
                                            null,
                                            key,
                                            valStr,
                                            typeStr,
                                            getTypeColor(typeStr),
                                            true
                                        ));
                                    }
                                }
                                subtitleText = currentPrefFile + " (" + total + " keys)";
                            } catch (Throwable t) {
                                subtitleText = "Error: " + t.getMessage();
                            }
                        }
                        break;
                    }

                    case DATABASE: {
                        if (currentDb != null && currentTable != null) {
                            SQLiteDatabase db = null;
                            try {
                                File dbPath = activity.getDatabasePath(currentDb);
                                if (dbPath.exists()) {
                                    db = SQLiteDatabase.openDatabase(dbPath.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
                                    Cursor rows = db.rawQuery("SELECT * FROM [" + currentTable + "] LIMIT 50", null);
                                    String[] colNames = rows.getColumnNames();
                                    topHeaderBanner = "🗄️ " + currentDb + "  ➔  📋 " + currentTable + " (" + colNames.length + " cols)";

                                    int count = 0;
                                    while (rows.moveToNext()) {
                                        StringBuilder rowData = new StringBuilder();
                                        boolean matchesFilter = TextUtils.isEmpty(searchFilter);

                                        for (int i = 0; i < colNames.length; i++) {
                                            String colVal = rows.getString(i);
                                            rowData.append(colNames[i]).append(" = ").append(colVal).append("\n");
                                            if (!matchesFilter && colVal != null && colVal.toLowerCase().contains(searchFilter)) {
                                                matchesFilter = true;
                                            }
                                        }

                                        if (matchesFilter) {
                                            count++;
                                            resultCards.add(new EntryCardModel(
                                                null,
                                                "ROW #" + (rows.getPosition() + 1),
                                                rowData.toString().trim(),
                                                "DB_ROW",
                                                0xFF00897B,
                                                false
                                            ));
                                        }
                                    }
                                    rows.close();
                                    subtitleText = currentDb + " > " + currentTable + " (" + count + " rows)";
                                }
                            } catch (Throwable t) {
                                subtitleText = "DB Error: " + t.getMessage();
                            } finally {
                                if (db != null && db.isOpen()) db.close();
                            }
                        }
                        break;
                    }

                    case INTENTS: {
                        subtitleText = "Live Intent Stream (" + INTENT_LOGS.size() + " captured)";
                        for (int i = INTENT_LOGS.size() - 1; i >= 0; i--) {
                            IntentLogItem item = INTENT_LOGS.get(i);
                            String fullText = item.action + " " + item.component + " " + item.dataUri + " " + item.extrasSummary;

                            if (!TextUtils.isEmpty(searchFilter) && !fullText.toLowerCase().contains(searchFilter)) {
                                continue;
                            }

                            StringBuilder details = new StringBuilder();
                            details.append("Action: ").append(item.action).append("\n");
                            if (!item.component.equals("None")) details.append("Component: ").append(item.component).append("\n");
                            if (!item.dataUri.isEmpty()) details.append("Data: ").append(item.dataUri).append("\n");
                            if (!item.extrasSummary.isEmpty()) details.append("\n--- Extras ---\n").append(item.extrasSummary);

                            int typeColor = item.type.equals("START_ACTIVITY") ? 0xFFF57C00 : (item.type.equals("BROADCAST") ? 0xFF0288D1 : 0xFF7B1FA2);
                            resultCards.add(new EntryCardModel(
                                null,
                                "[" + item.timestamp + "] " + item.type,
                                details.toString().trim(),
                                "INTENT",
                                typeColor,
                                false
                            ));
                        }
                        break;
                    }

                    case JSON_LOGS: {
                        subtitleText = "Live JSON / Gson Stream (" + JSON_LOGS.size() + " captured)";
                        for (int i = JSON_LOGS.size() - 1; i >= 0; i--) {
                            JsonLogItem item = JSON_LOGS.get(i);
                            String fullText = item.source + " " + item.targetClass + " " + item.payload;

                            if (!TextUtils.isEmpty(searchFilter) && !fullText.toLowerCase().contains(searchFilter)) {
                                continue;
                            }

                            int color = item.source.startsWith("GSON") ? 0xFF8E24AA : 0xFF00897B;
                            resultCards.add(new EntryCardModel(
                                null,
                                "[" + item.timestamp + "] " + item.source + " (" + item.targetClass + ")",
                                item.payload,
                                "JSON",
                                color,
                                false
                            ));
                        }
                        break;
                    }
                }
            } catch (Throwable ignored) {}

            final String finalSubtitle = subtitleText;
            final String finalBanner = topHeaderBanner;

            // 3. Post back to UI Thread smoothly
            MAIN_HANDLER.post(() -> {
                if (token != QUERY_TOKEN.get()) return; // Outdated query, drop

                if (headerSubtitle != null && !TextUtils.isEmpty(finalSubtitle)) {
                    headerSubtitle.setText(finalSubtitle);
                }

                if (entriesListContainer != null) {
                    entriesListContainer.removeAllViews();

                    if (finalBanner != null) {
                        TextView bannerView = new TextView(activity);
                        bannerView.setText(finalBanner);
                        bannerView.setTextColor(0xFF00E5FF);
                        bannerView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                        bannerView.setTypeface(Typeface.DEFAULT_BOLD);
                        bannerView.setPadding(0, 0, 0, dp(activity, 6));
                        entriesListContainer.addView(bannerView);
                    }

                    if (resultCards.isEmpty()) {
                        showEmptyMessage(!TextUtils.isEmpty(searchFilter) ? "No entries matching '" + searchFilter + "'" : "No entries found");
                        return;
                    }

                    for (EntryCardModel m : resultCards) {
                        if (m.isPref) {
                            entriesListContainer.addView(createPrefCard(activity, m.sourceBadge, m.title, m.body, m.type));
                        } else {
                            entriesListContainer.addView(createGenericCard(activity, m.title, m.body, m.color));
                        }
                    }

                    if (resultCards.size() >= MAX_DISPLAY_ITEMS) {
                        TextView note = new TextView(activity);
                        note.setText("⚡ Displaying first " + MAX_DISPLAY_ITEMS + " results. Use search bar to filter specific keys.");
                        note.setTextColor(0xFF8888AA);
                        note.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
                        note.setPadding(0, dp(activity, 8), 0, dp(activity, 8));
                        note.setGravity(Gravity.CENTER);
                        entriesListContainer.addView(note);
                    }
                }
            });
        });
    }

    private static void showLoadingState(Activity activity) {
        if (entriesListContainer == null) return;
        LinearLayout loadingLayout = new LinearLayout(activity);
        loadingLayout.setOrientation(LinearLayout.VERTICAL);
        loadingLayout.setGravity(Gravity.CENTER);
        loadingLayout.setPadding(0, dp(activity, 40), 0, dp(activity, 40));

        TextView tv = new TextView(activity);
        tv.setText("⏳ Loading & indexing entries...");
        tv.setTextColor(0xFF00E5FF);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        loadingLayout.addView(tv);

        entriesListContainer.addView(loadingLayout);
    }

    // =========================================================================
    // Card Views & UI Helpers
    // =========================================================================

    private static View createPrefCard(Activity activity, String sourceFile, String key, String value, String type) {
        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(activity, 10), dp(activity, 8), dp(activity, 10), dp(activity, 8));

        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(0xFF1B1B2C);
        cardBg.setCornerRadius(dp(activity, 8));
        card.setBackground(cardBg);

        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.bottomMargin = dp(activity, 6);
        card.setLayoutParams(cardLp);

        if (sourceFile != null) {
            TextView fileBadge = new TextView(activity);
            fileBadge.setText(sourceFile);
            fileBadge.setTextColor(0xFFBA68C8);
            fileBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
            fileBadge.setTypeface(Typeface.DEFAULT_BOLD);
            fileBadge.setPadding(0, 0, 0, dp(activity, 2));
            card.addView(fileBadge);
        }

        LinearLayout topRow = new LinearLayout(activity);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView typePill = new TextView(activity);
        typePill.setText(type);
        typePill.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9);
        typePill.setTypeface(Typeface.DEFAULT_BOLD);
        typePill.setTextColor(Color.BLACK);
        typePill.setPadding(dp(activity, 5), dp(activity, 2), dp(activity, 5), dp(activity, 2));

        GradientDrawable pillBg = new GradientDrawable();
        pillBg.setColor(getTypeColor(type));
        pillBg.setCornerRadius(dp(activity, 4));
        typePill.setBackground(pillBg);

        TextView tvKey = new TextView(activity);
        tvKey.setText("  " + key);
        tvKey.setTextColor(0xFF00E5FF);
        tvKey.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvKey.setTypeface(Typeface.DEFAULT_BOLD);

        topRow.addView(typePill);
        topRow.addView(tvKey);
        card.addView(topRow);

        TextView tvVal = new TextView(activity);
        tvVal.setText(value);
        tvVal.setTextColor(0xFFDDDDDD);
        tvVal.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvVal.setTextIsSelectable(true);
        tvVal.setPadding(0, dp(activity, 4), 0, 0);
        card.addView(tvVal);

        card.setOnClickListener(v -> copyToClipboard(activity, key, value));
        return card;
    }

    private static View createGenericCard(Activity activity, String title, String body, int titleColor) {
        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(activity, 10), dp(activity, 8), dp(activity, 10), dp(activity, 8));

        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(0xFF1B1B2C);
        cardBg.setCornerRadius(dp(activity, 8));
        card.setBackground(cardBg);

        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.bottomMargin = dp(activity, 6);
        card.setLayoutParams(cardLp);

        TextView tvTitle = new TextView(activity);
        tvTitle.setText(title);
        tvTitle.setTextColor(titleColor);
        tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        card.addView(tvTitle);

        TextView tvBody = new TextView(activity);
        tvBody.setText(body);
        tvBody.setTextColor(0xFFEEEEEE);
        tvBody.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvBody.setTextIsSelectable(true);
        tvBody.setPadding(0, dp(activity, 4), 0, 0);
        card.addView(tvBody);

        card.setOnClickListener(v -> copyToClipboard(activity, title, body));
        return card;
    }

    private static void copyToClipboard(Activity activity, String label, String text) {
        try {
            ClipboardManager cm = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText(label, text);
            if (cm != null) {
                cm.setPrimaryClip(clip);
                Toast.makeText(activity, "Copied to clipboard", Toast.LENGTH_SHORT).show();
            }
        } catch (Throwable ignored) {}
    }

    private static void showEmptyMessage(String message) {
        if (entriesListContainer == null) return;
        TextView empty = new TextView(entriesListContainer.getContext());
        empty.setText(message);
        empty.setTextColor(0xFF8888AA);
        empty.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        empty.setPadding(dp(entriesListContainer.getContext(), 8), dp(entriesListContainer.getContext(), 24), dp(entriesListContainer.getContext(), 8), dp(entriesListContainer.getContext(), 24));
        empty.setGravity(Gravity.CENTER);
        entriesListContainer.addView(empty);
    }

    private static int getTypeColor(String type) {
        switch (type) {
            case "STRING": return 0xFF81C784;
            case "BOOLEAN": return 0xFFFFB74D;
            case "INTEGER": case "INT": return 0xFF64B5F6;
            case "LONG": return 0xFFBA68C8;
            case "FLOAT": return 0xFFFFD54F;
            case "SET": return 0xFF4DD0E1;
            default: return 0xFFE0E0E0;
        }
    }

    private static TextView createIconBtn(Activity activity, String text, int color, Runnable onClick) {
        TextView btn = new TextView(activity);
        btn.setText(text);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        btn.setTextColor(Color.WHITE);
        btn.setGravity(Gravity.CENTER);
        btn.setPadding(dp(activity, 8), dp(activity, 4), dp(activity, 8), dp(activity, 4));

        GradientDrawable btnBg = new GradientDrawable();
        btnBg.setColor(color);
        btnBg.setCornerRadius(dp(activity, 8));
        btn.setBackground(btnBg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = dp(activity, 6);
        btn.setLayoutParams(lp);

        btn.setOnClickListener(v -> onClick.run());
        return btn;
    }

    private static int dp(Context context, int dpVal) {
        return (int) TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dpVal,
            context.getResources().getDisplayMetrics()
        );
    }
}
