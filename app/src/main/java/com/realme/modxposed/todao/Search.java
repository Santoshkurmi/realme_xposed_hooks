package com.realme.modxposed.todao;

import java.util.ArrayList;
import java.util.HashMap;

public class Search {
    ArrayList<String> classes = new ArrayList<>(),methods = new ArrayList<>(),excludeClasses = new ArrayList<>(),excludeMethods = new ArrayList<>(),returnType = new ArrayList<>(),params = new ArrayList<>();
    String data;
    boolean hook,printWhileHooking,resultPrint,printClasses,printMethods,after=true,debug,printDebugWhileSearching,enable = true;

    String print;
    HashMap<String,String> results = new HashMap<>();

    public ArrayList<String> getExcludeClasses() {
        return excludeClasses;
    }

    public void setExcludeClasses(ArrayList<String> excludeClasses) {
        this.excludeClasses = excludeClasses;
    }

    public ArrayList<String> getExcludeMethods() {
        return excludeMethods;
    }

    public void setExcludeMethods(ArrayList<String> excludeMethods) {
        this.excludeMethods = excludeMethods;
    }

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public boolean isPrintDebugWhileSearching() {
        return printDebugWhileSearching;
    }

    public void setPrintDebugWhileSearching(boolean printDebugWhileSearching) {
        this.printDebugWhileSearching = printDebugWhileSearching;
    }

    public HashMap<String, String> getResults() {
        return results;
    }

    public void setResults(HashMap<String, String> results) {
        this.results = results;
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

    public ArrayList<String> getClasses() {
        return classes;
    }

    public boolean isPrintWhileHooking() {
        return printWhileHooking;
    }

    public void setPrintWhileHooking(boolean printWhileHooking) {
        this.printWhileHooking = printWhileHooking;
    }

    public boolean isResultPrint() {
        return resultPrint;
    }

    public void setResultPrint(boolean resultPrint) {
        this.resultPrint = resultPrint;
    }

    public boolean isPrintClasses() {
        return printClasses;
    }

    public void setPrintClasses(boolean printClasses) {
        this.printClasses = printClasses;
    }

    public boolean isPrintMethods() {
        return printMethods;
    }

    public void setPrintMethods(boolean printMethods) {
        this.printMethods = printMethods;
    }

    public String getPrint() {
        return print;
    }

    public void setPrint(String print) {
        this.print = print;
    }

    public void setClasses(ArrayList<String> classes) {
        this.classes = classes;
    }

    public ArrayList<String> getMethods() {
        return methods;
    }

    public void setMethods(ArrayList<String> methods) {
        this.methods = methods;
    }


    public ArrayList<String> getReturnType() {
        return returnType;
    }

    public void setReturnType(ArrayList<String> returnType) {
        this.returnType = returnType;
    }

    public ArrayList<String> getParams() {
        return params;
    }

    public void setParams(ArrayList<String> params) {
        this.params = params;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public boolean isHook() {
        return hook;
    }

    public void setHook(boolean hook) {
        this.hook = hook;
    }
}
