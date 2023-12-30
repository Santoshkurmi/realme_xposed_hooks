package com.realme.modxposed.hooks;

import java.util.Calendar;
import java.util.Locale;

import de.robv.android.xposed.XposedBridge;

public class BsToAdConverter {
    int numOfDaysBSFromPaush[] = {29,29,30,30,31,32,31,32,31,30,30,30,29,30,30,30,30,32,31,32,31,30,30,30,29,30,30,30,31};
    String monthsInNepali[] = {"बैशाख","जेठ","असार","साउन","भदौ","असोज","कार्तिक","मंसिर","पुष","माघ","फागुन","चैत"};
//    int numOfDaysAd[] = {31,28,31,30,31,30,31,31,30,31,30,31};
    String nepaliNumbers[] ={"०१","०२","०३","०४","०५","०६","०७","०८","०९","१०","११","१२","१३","१४","१५","१६","१७","१८","१९","२०","२१","२२","२३","२४","२५","२६","२७","२८","२९","३०","३१","३२"};
    int numOfDaysAd[] = {0,31,59,90,120,151,181,212,243,273,304,334,365};

    int refYear = 2023;

    int cacheYear = 0,cacheMonth = 0,cacheDay = 0;
    String cacheDate;



    public String  getMonth(){
        Calendar calendar = Calendar.getInstance();
        int current_year = calendar.get(Calendar.YEAR);
        int current_month = calendar.get(Calendar.MONTH) + 1;
        int current_day = calendar.get(Calendar.DAY_OF_MONTH);


        if (current_day == cacheDay && current_year == cacheYear && current_month == cacheMonth){
            return cacheDate;
        }
        String englishDate = calendar.getDisplayName(Calendar.MONTH,Calendar.SHORT, Locale.ENGLISH)+" "+ current_day;
        int totalDays = 0;
//        XposedBridge.log(current_month+""+ (current_day - 16));

        if (current_year == refYear && current_month==12) totalDays = current_day;
        else if (current_year<=refYear) return "Not Found";
        else {
            totalDays = 31 + numOfDaysAd[current_month-1] +current_day;
            if (current_year %4 ==0 && current_month>2) totalDays +=1;

            if (current_year- refYear >1){
                totalDays += 365*(current_year - refYear-1) + ( (int)((current_year - refYear-2)/4) ) + 1;
            }//

        }//if else
        totalDays -= 16;

        if (totalDays <= numOfDaysBSFromPaush[0]){
            cacheDate = calendar.getDisplayName(Calendar.DAY_OF_WEEK,Calendar.SHORT,Locale.ENGLISH)+","+monthsInNepali[(8)%12]+" "+(nepaliNumbers[totalDays-1])+"\uD83D\uDE0A"+englishDate;
            return cacheDate;
        }

        for(int i=1;i<numOfDaysBSFromPaush.length;i++){

           totalDays -=numOfDaysBSFromPaush[i-1];

            if (totalDays <= numOfDaysBSFromPaush[i]){
                cacheDay = current_day;
                cacheMonth = current_month;
                cacheYear = current_year;
                cacheDate = calendar.getDisplayName(Calendar.DAY_OF_WEEK,Calendar.SHORT,Locale.ENGLISH)+","+monthsInNepali[(i+8)%12]+" "+(nepaliNumbers[totalDays-1]) +"\uD83D\uDE0A"+englishDate;
                return cacheDate;
            }
        }//
        cacheDay = current_day;
        cacheMonth = current_month;
        cacheYear = current_year;
        cacheDate = calendar.getDisplayName(Calendar.DAY_OF_WEEK,Calendar.SHORT,Locale.ENGLISH)+","+englishDate;
          return cacheDate;



    }//getMonth
}//
