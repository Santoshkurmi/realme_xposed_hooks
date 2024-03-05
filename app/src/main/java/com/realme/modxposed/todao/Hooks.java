package com.realme.modxposed.todao;



import java.util.ArrayList;
import java.util.HashMap;

public class Hooks {
    String className,method,print,result;
    boolean hookAll,debug,after= true,printWhileHooking,resultPrint,paramPrint, enable = true;
    ArrayList<HookParam> params = new ArrayList<>();
    HashMap<String,String> results = new HashMap<>();
    ArrayList<String> methods = new ArrayList<>(),excludes = new ArrayList<>();

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public boolean isResultPrint() {
        return resultPrint;
    }

    public void setResultPrint(boolean resultPrint) {
        this.resultPrint = resultPrint;
    }

    public boolean isParamPrint() {
        return paramPrint;
    }

    public void setParamPrint(boolean paramPrint) {
        this.paramPrint = paramPrint;
    }

    public boolean isPrintWhileHooking() {
        return printWhileHooking;
    }

    public void setPrintWhileHooking(boolean printWhileHooking) {
        this.printWhileHooking = printWhileHooking;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getPrint() {
        return print;
    }

    public void setPrint(String print) {
        this.print = print;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public boolean isHookAll() {
        return hookAll;
    }

    public void setHookAll(boolean hookAll) {
        this.hookAll = hookAll;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public boolean isAfter() {
        return after;
    }

    public void setAfter(boolean after) {
        this.after = after;
    }

    public ArrayList<HookParam> getParams() {
        return params;
    }

    public void setParams(ArrayList<HookParam> params) {
        this.params = params;
    }

    public HashMap<String, String> getResults() {
        return results;
    }

    public void setResults(HashMap<String, String> results) {
        this.results = results;
    }

    public ArrayList<String> getMethods() {
        return methods;
    }

    public void setMethods(ArrayList<String> methods) {
        this.methods = methods;
    }

    public ArrayList<String> getExcludes() {
        return excludes;
    }

    public void setExcludes(ArrayList<String> excludes) {
        this.excludes = excludes;
    }
}
