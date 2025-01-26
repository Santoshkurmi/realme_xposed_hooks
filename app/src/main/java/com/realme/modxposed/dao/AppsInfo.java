package com.realme.modxposed.dao;

import java.util.HashMap;

public class AppsInfo {
    private final HashMap<String,PackageInfo> apps = new HashMap<>();

    public void addPackage(String packageName,PackageInfo packageInfo){
        apps.put(packageName,packageInfo);
    }

    public PackageInfo getPackageInfo(String packageName){
        return apps.get(packageName);
    }



}
