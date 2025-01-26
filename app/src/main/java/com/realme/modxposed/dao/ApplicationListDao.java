package com.realme.modxposed.dao;

import android.graphics.drawable.Drawable;

public class ApplicationListDao {
    String name;
    Drawable icon;
    boolean isEnabled = false;

    public ApplicationListDao(Drawable icon, String name, boolean isEnabled) {
        this.icon = icon;
        this.name = name;
        this.isEnabled = isEnabled;
    }

    public Drawable getIcon() {
        return icon;
    }

    public void setIcon(Drawable icon) {
        this.icon = icon;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }
}
