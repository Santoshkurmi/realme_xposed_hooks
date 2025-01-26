package com.realme.modxposed.dao;

import com.realme.modxposed.globals.Globals;

import java.lang.reflect.Method;
import java.util.ArrayList;

public class PackageInfo {
    String name;
    boolean isActive;
    ArrayList<ClassInfo> classList = new ArrayList<>();



    public void addClass(ClassInfo classInfo){
        classList.add(classInfo);
    }

    public ArrayList<ClassInfo> getClassList(){
        return classList;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }


}
