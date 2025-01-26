package com.realme.modxposed.dao;

import java.util.ArrayList;

public class ClassInfo {
    boolean isActive;

    String  name;
    ArrayList<MethodInfo> methods = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void addMethod(MethodInfo methodInfo){
        methods.add(methodInfo);
    }
    public ArrayList<MethodInfo> getMethods(){
        return methods;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

}
