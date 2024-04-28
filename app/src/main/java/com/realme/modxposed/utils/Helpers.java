package com.realme.modxposed.utils;

import com.moandjiezana.toml.Toml;
import com.realme.modxposed.todao.Hooks;
import com.realme.modxposed.todao.Root;


import java.io.File;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

public class Helpers {
    Pattern pattern = Pattern.compile("\\{(\\d+)\\}");

    public Root parseConfig(String packageName){
        String path = "/data/data/"+packageName+"/hooks.toml";
        if(!new File(path).exists()) {
            XposedBridge.log("ERROR: hooks.toml not found in "+ path);
            return null;
        };

        try{
            byte [] bytes = Files.readAllBytes(Paths.get(path));
            String content = new String(bytes, StandardCharsets.UTF_8);
            Toml toml = new Toml().read(content);
            return toml.to(Root.class);
        }
        catch (Exception e){
            XposedBridge.log(e.toString());
            return null;
        }
    }//parser

    public String makePrint(Method method, XC_MethodHook.MethodHookParam param, String print){

        String patched = print;
        if(param.getResult() !=null)
            patched = patched.replace( "{result}", param.getResult().toString());
        patched = patched.replace((CharSequence) "{method}", method.getName());
        patched = patched.replace((CharSequence) "{count}", param.args.length+"");

        Matcher matcher = pattern.matcher(print);
        while (matcher.find()){
            int number = Integer.parseInt(matcher.group(1));
//            XposedBridge.log(number+", "+ param.args.length);
            if(number < param.args.length && param.args[number] !=null )
                patched = patched.replace( "{"+number+"}", param.args[number].toString());
            else if(number < param.args.length && param.args[number] ==null )
                patched = patched.replace( "{"+number+"}", "null");

        }//while
        return patched;
    }//printer


    public String makeDebugString(Method method){
        StringBuilder builder = new StringBuilder();
        builder.append("DEBUG:").append(method.getName()).append("( ");
        String returnType = method.getReturnType().getName();
        for(Class<?> type:method.getParameterTypes()){
            builder.append(type.getName()).append(", ");
        }//types
        builder.append(")").append("=>").append(returnType);
        return builder.toString();
    }

    public void setResult(Method method, String result, XC_MethodHook.MethodHookParam param,boolean isResultPrint){

        String returnType = method.getReturnType().getCanonicalName();
        if(returnType ==null) return;
        switch (returnType) {
            case "java.lang.String":
                if(isResultPrint)
                    XposedBridge.log("RESULT: Setting String value from "+param.getResult()+" to "+ result+" in "+method.getName());
                param.setResult(result);
                break;
            case "boolean":
                if(isResultPrint)
                    XposedBridge.log("RESULT: Setting boolean value from "+param.getResult()+" to "+ result+" in "+method.getName());
                param.setResult(Boolean.parseBoolean(result));
                break;
            case "void":
                if(isResultPrint)
                    XposedBridge.log("RESULT: Exiting the function call of method "+method.getName());
                param.setResult(null);
                break;
            case "long":
                if(isResultPrint)
                    XposedBridge.log("RESULT: Setting long value from "+param.getResult()+" to "+ result+" in "+method.getName());
                param.setResult(Long.parseLong(result));
                break;
            case "int":
                if(isResultPrint)
                    XposedBridge.log("RESULT: Setting int value from "+param.getResult()+" to "+ result+" in "+method.getName());
                param.setResult(Integer.parseInt(result));
                break;
            case "float":
                if(isResultPrint)
                    XposedBridge.log("RESULT: Setting float value from "+param.getResult()+" to "+ result+" in "+method.getName());
                param.setResult(Float.parseFloat(result));
                break;
        }
    }






    public void setParam(String methodName,String methodType, int arg,String value, XC_MethodHook.MethodHookParam param,Hooks hook){

        switch (methodType) {
            case "java.lang.String":
                if(hook.isParamPrint())
                    XposedBridge.log("PARAM: Setting String arg "+arg+" value from "+param.args[arg]+" to "+ value+" in "+methodName);
                param.args[arg] =value ;
                break;
            case "boolean":
                if(hook.isParamPrint())
                    XposedBridge.log("PARAM: Setting boolean arg "+arg+" value from "+param.args[arg]+" to "+ value+" in "+methodName);
                param.args[arg] = (Boolean.parseBoolean(value));
                break;
            case "int":
                if(hook.isParamPrint())
                    XposedBridge.log("PARAM: Setting int arg "+arg+" value from "+param.args[arg]+" to "+ value+" in "+methodName);
                param.args[arg] = (Integer.parseInt(value));
                break;
            case "float":
                if(hook.isParamPrint())
                    XposedBridge.log("PARAM: Setting float arg "+arg+" value from "+param.args[arg]+" to "+ value+" in "+methodName);
                param.args[arg] = (Float.parseFloat(value));
                break;
        }
    }










}//helper
