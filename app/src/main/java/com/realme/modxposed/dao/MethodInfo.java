package com.realme.modxposed.dao;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;

public class MethodInfo {
    boolean isActive,isHookBefore,isDebug;
    String name,print;
    ArrayList<String> params = new ArrayList<>();
    String returnValue,returnType;
    HashMap<Integer,String> paramsChange = new HashMap<>();

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public boolean isHookBefore() {
        return isHookBefore;
    }

    public void setHookBefore(boolean hookBefore) {
        isHookBefore = hookBefore;
    }

    public boolean isDebug() {
        return isDebug;
    }

    public void setDebug(boolean debug) {
        isDebug = debug;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPrint() {
        return print;
    }

    public void setPrint(String print) {
        this.print = print;
    }

    public ArrayList<String> getParams() {
        return params;
    }

    public void addParam(String param) {
        params.add(param);
    }

    public String getReturnValue() {
        return returnValue;
    }

    public void setReturnValue(String returnValue) {
        this.returnValue = returnValue;
    }

    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    public HashMap<Integer, String> getParamsChange() {
        return paramsChange;
    }

    public void addParamsChange(Integer position,String value) {
        this.paramsChange.put(position,value);
    }
}
