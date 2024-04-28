package com.realme.modxposed.utils;

import android.app.AndroidAppHelper;
import android.content.pm.ApplicationInfo;

import com.realme.modxposed.todao.HookParam;
import com.realme.modxposed.todao.Hooks;
import com.realme.modxposed.todao.Root;
import com.realme.modxposed.todao.Search;


import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Enumeration;

import dalvik.system.DexFile;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class Compiler {

    Helpers helpers = new Helpers();
    long noOfHookedMethods = 0;

    public void handleHookingMethod(String packageName, XC_LoadPackage.LoadPackageParam lpparam) throws ClassNotFoundException, IOException {
        Root config = helpers.parseConfig(packageName);

        Class<?> clazz;
        for(Hooks hook:config.getHooks()){
            if(!hook.isEnable()) continue;
            clazz = XposedHelpers.findClassIfExists(hook.getClassName(),lpparam.classLoader);
            if(clazz == null){
                XposedBridge.log("ERROR:Unable to find class "+hook.getClassName()+" . Continuing");
                continue;
            }
            for(Method method:clazz.getDeclaredMethods()){
                if(hook.getExcludes().contains(method.getName())) continue;
                if(  hook.isHookAll() || ( hook.getMethod()!=null && hook.getMethod().equals(method.getName()) ) || hook.getMethods().contains(method.getName()) ){
                    if(Modifier.isAbstract(method.getModifiers())) {
                        XposedBridge.log("ABSTRACT:Ignoring method "+method.getName());
                        continue;
                    }

                    if(hook.isPrintWhileHooking()) XposedBridge.log("HOOKING:"+helpers.makeDebugString(method).replace("DEBUG",""));
                    if(hook.isDebug()) hookMethod(hook,method,helpers.makeDebugString(method));
                    else hookMethod(hook,method,null);
                }//if found teh hooked function
            }//forMethods

        }//forHooks
        if(config.getSearch().getClasses().size()>0 && config.getSearch().isEnable())
            new Thread(() -> {
                try {
                    XposedBridge.log("SEARCHING: Starting the classes searching");
                    searchClassMethods(config.getSearch(),lpparam.classLoader);
                } catch (IOException e) {
                    XposedBridge.log(e.toString());
                }
            }).start();

    }//init

    Enumeration<String> getAllClasses() throws IOException {
        ApplicationInfo applicationInfo = AndroidAppHelper.currentApplicationInfo();
        DexFile dex = new DexFile(applicationInfo.sourceDir);
        return dex.entries();
    }
    void getSystemClasses(){
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
//        classLoader.getResourceAsStream()
    }

    ArrayList<String> sanitizeArray(ArrayList<String> array){
        ArrayList<String> sanitized = new ArrayList<>();
//        if(array.size()>0)
//         XposedBridge.log(array.get(0).replaceAll("\\.","\\.").replaceAll("\\*",".*"));
        for(String str:array){
            sanitized.add(str.replaceAll("\\.","\\.").replaceAll("\\*",".*"));
        }
        return sanitized;
    }//sanitize

    void searchClassMethods(Search search,ClassLoader classLoader) throws IOException {
        noOfHookedMethods = 0;
        Enumeration<String> entries = getAllClasses();
        ArrayList<String> sanitizedClasses = sanitizeArray(search.getClasses());
        ArrayList<String> sanitizedClassesExcludes = sanitizeArray(search.getExcludeClasses());
        String currentClass;
        long completed = 0,found = 0;
        while (entries.hasMoreElements()){
            completed++;
            currentClass = entries.nextElement();
            if(matchInArray(currentClass,sanitizedClassesExcludes)) continue;
            if(matchInArray(currentClass,sanitizedClasses)) {
                if(search.isPrintClasses())
                    XposedBridge.log("SEARCH: Match found of class "+currentClass);
                found++;
                if(search.getMethods().size()==0) continue;
                searchAndHookMethods(search,currentClass,classLoader);
            }

        }//while
        XposedBridge.log("COMPLETED:Searching completed found "+found+"/"+completed+" classes and methods: "+noOfHookedMethods);

    }//searchClassMethods

    boolean matchInArray(String value,ArrayList<String> array){
       if(value == null) return false;
       for(String arr:array) {
           if (value.matches(arr.replaceAll("\\*",".*")) ) return true;

       }//for
        return false;
    }//

    boolean matchInArrays(Class<?>[] types,ArrayList<String> params){
        String typee;
        for(Class<?> type:types) {
            for(String param:params){
                typee = type.getCanonicalName();
                if(typee !=null)
                    if (typee.matches(param.replaceAll("\\*",".*")) ) return true;
            }

        }//for
        return false;
    }//

    void searchAndHookMethods(Search search,String cls,ClassLoader classLoader){
//        if(! search.isHook() ) return;
        Class<?> clazz = XposedHelpers.findClassIfExists(cls,classLoader);
        if(clazz == null){
            XposedBridge.log("ERROR:Unable to find class "+cls+" .Continuing");
            return;
        }
        Method[] methods;
        try{
            methods = clazz.getDeclaredMethods();
        }
        catch (NoClassDefFoundError error){
            XposedBridge.log("ERROR: ClassDefNotFound for class "+cls+" . Continuing");
            return;
        }
        for(Method method:methods){
            if(matchInArray(method.getName(),search.getExcludeMethods())) continue;
            if(   matchInArray(method.getName(),search.getMethods())  ){

                if(method.getParameterTypes().length < search.getParamsMinLen()) continue;
                if(search.getParamsMaxLen()!=-1 && method.getParameterTypes().length > search.getParamsMaxLen() ) continue;

                if(search.getReturnTypes().size()>0)
                    if (!matchInArray(method.getReturnType().getCanonicalName(),search.getReturnTypes()) ) continue;

                if(search.getParams().size()>0)
                    if (!matchInArrays(method.getParameterTypes(),search.getParams()) ) continue;

                if(method.getName().length() < search.getMetMinLen()) continue;
                if(search.getMetMaxLen()!=-1 && method.getName().length() > search.getMetMaxLen() ) continue;


                if(Modifier.isAbstract(method.getModifiers())) {
                    if(search.isPrintAbstract())
                        XposedBridge.log("ABSTRACT:Ignoring method "+method.getName());
                    continue;
                }
                noOfHookedMethods++;
                if(noOfHookedMethods % 500 ==0) XposedBridge.log("Hook:Hooked upto method number "+noOfHookedMethods);
                if( (search.getTo() !=-1 && noOfHookedMethods >search.getTo()) || noOfHookedMethods <search.getFrom() ) return;

                if(search.isPrintMethods()) XposedBridge.log("METHOD: Matched method "+helpers.makeDebugString(method).replace("DEBUG:","")+" in "+ cls);
//                if(search.isPrintDebugWhileSearching()) XposedBridge.log(helpers.makeDebugString(method)+" in "+cls);
                if(!search.isHook()) continue;
                if(search.isPrintWhileHooking())
                    XposedBridge.log("HOOKING:"+method.getName());
                if(search.isDebug() )
                    hookMethodSearch(search,method,helpers.makeDebugString(method),cls);
                else
                    hookMethodSearch(search,method,null,cls);
            }//if found teh hooked function
        }//forMethods
    }

    void searchInParams(Search search,Method method,String cls){

    }

    void hookMethodSearch(Search search,Method method,String debug,String cls){
        if(search.isAfter())
            XposedBridge.hookMethod(method, new XC_MethodHook() {
            final String result = search.getResults().get(method.getName()) ;
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if(debug !=null) XposedBridge.log(debug+"|"+param.getResult());
                if(search.getPrint() !=null) XposedBridge.log("PRINT:"+helpers.makePrint(method,param,search.getPrint()));
                if(result !=null) helpers.setResult(method,result,param,search.isResultPrint());
                if(search.getData() ==null) return;
                for(int i=0;i< param.args.length;i++){
                   if(param.args[i] ==null ) continue;
                   String args = param.args[i].toString();
                    if(args.matches(search.getData().replaceAll("\\*",".*"))) XposedBridge.log("MATCHED: Matched in args "+search.getData()+" in method  "+ method.getName()+" in class "+ cls+" with "+args);

                }//
                if(param.getResult() !=null)
                    if(param.getResult().toString().matches(search.getData().replaceAll("\\*",".*"))) XposedBridge.log("MATCHED: Matched in return "+search.getData()+" in method "+ method.getName()+" in class "+ cls+" with "+param.getResult());
            }//after
        });//
        else
            XposedBridge.hookMethod(method, new XC_MethodHook() {
                final String result = search.getResults().get(method.getName()) ;
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if(debug !=null) XposedBridge.log(debug+"|"+param.getResult());
                    if(search.getPrint() !=null) XposedBridge.log("PRINT:"+helpers.makePrint(method,param,search.getPrint()));
                    if(result !=null) helpers.setResult(method,result,param,search.isResultPrint());

                    if(search.getData() ==null) return;
                    for(int i=0;i< param.args.length;i++){
                        if(param.args[i] ==null ) continue;
                        String args = param.args[i].toString();
                        if(args.matches(search.getData().replaceAll("\\*",".*"))) XposedBridge.log("MATCHED: Matched "+search.getData()+" in method "+ method.getName()+" in class "+ cls+" with "+args);
                    }//
                }//after
            });//

    }//HookMethod

    void hookMethod(Hooks hook,Method method,String debug){
        if(hook.isAfter())
            XposedBridge.hookMethod(method, new XC_MethodHook() {
            String result = hook.getResults().get(method.getName()) ;
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if(debug !=null) XposedBridge.log(debug+"|"+param.getResult());
                if(hook.getPrint() !=null) XposedBridge.log("PRINT:"+helpers.makePrint(method,param,hook.getPrint()));

                if(result !=null) helpers.setResult(method,result,param,hook.isResultPrint());
                else if ( hook.getResult() !=null) helpers.setResult(method,hook.getResult(),param,hook.isResultPrint());


            }//after
        });//
        else
            XposedBridge.hookMethod(method, new XC_MethodHook() {
                String result = hook.getResults().get(method.getName()) ;
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if(result !=null) helpers.setResult(method,result,param,hook.isResultPrint());
                    else if ( hook.getResult() !=null) helpers.setResult(method,hook.getResult(),param,hook.isResultPrint());

                    if(debug !=null) XposedBridge.log(debug+"|"+param.getResult());
                    if(hook.getPrint() !=null) XposedBridge.log("PRINT:"+helpers.makePrint(method,param,hook.getPrint()));
                    hookParams(hook,method,param);


                }//after
            });//


    }//HookMethod

    void hookParams(Hooks hook,Method method, XC_MethodHook.MethodHookParam param){
        for(HookParam arg: hook.getParams()){
            if(arg.getArg() >=param.args.length) continue;
            if(arg.getPattern() ==null) helpers.setParam(method.getParameterTypes()[arg.getArg()].getName(),method.getName(),arg.getArg(),arg.getValue(),param,hook);
            else if(param.args[arg.getArg()] !=null ){
                param.args[arg.getArg()] = param.args[arg.getArg()].toString().replaceAll(arg.getPattern(), arg.getValue());
                if(hook.isParamPrint())
                    XposedBridge.log("PARAM:Setting String value of arg "+arg.getArg()+" to "+param.args[arg.getArg()]);
            }//else
        }//for loop
    }//hookParams

}//class
