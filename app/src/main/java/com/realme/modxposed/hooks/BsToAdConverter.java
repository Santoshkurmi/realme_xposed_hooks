package com.realme.modxposed.hooks;

import java.util.Calendar;
import java.util.Locale;

public class BsToAdConverter {

    // Official Bikram Sambat calendar data for years 2000 BS through 2100 BS (101 years)
    private static final int[][] BS_MONTH_DAYS = {
        {30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31}, // 2000 BS
        {31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30}, // 2001 BS
        {31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30}, // 2002 BS
        {31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31}, // 2003 BS
        {30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31}, // 2004 BS
        {31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30}, // 2005 BS
        {31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30}, // 2006 BS
        {31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31}, // 2007 BS
        {31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 29, 31}, // 2008 BS
        {31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30}, // 2009 BS
        {31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30}, // 2010 BS
        {31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31}, // 2011 BS
        {31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 30, 30}, // 2012 BS
        {31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30}, // 2013 BS
        {31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30}, // 2014 BS
        {31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31}, // 2015 BS
        {31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 30, 30}, // 2016 BS
        {31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30}, // 2017 BS
        {31, 32, 31, 32, 31, 30, 30, 29, 30, 29, 30, 30}, // 2018 BS
        {31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31}, // 2019 BS
        {31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30}, // 2020 BS
        {31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30}, // 2021 BS
        {31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30}, // 2022 BS
        {31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31}, // 2023 BS
        {31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30}, // 2024 BS
        {31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30}, // 2025 BS
        {31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31}, // 2026 BS
        {30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31}, // 2027 BS
        {31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30}, // 2028 BS
        {31, 31, 32, 31, 32, 30, 30, 29, 30, 29, 30, 30}, // 2029 BS
        {31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31}, // 2030 BS
        {30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31}, // 2031 BS
        {31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30}, // 2032 BS
        {31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30}, // 2033 BS
        {31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31}, // 2034 BS
        {30, 32, 31, 32, 31, 31, 29, 30, 30, 29, 29, 31}, // 2035 BS
        {31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30}, // 2036 BS
        {31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30}, // 2037 BS
        {31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31}, // 2038 BS
        {31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 30, 30}, // 2039 BS
        {31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30}, // 2040 BS
        {31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30}, // 2041 BS
        {31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31}, // 2042 BS
        {31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 30, 30}, // 2043 BS
        {31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30}, // 2044 BS
        {31, 32, 31, 32, 31, 30, 30, 29, 30, 29, 30, 30}, // 2045 BS
        {31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31}, // 2046 BS
        {31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30}, // 2047 BS
        {31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30}, // 2048 BS
        {31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30}, // 2049 BS
        {31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31}, // 2050 BS
        {31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30}, // 2051 BS
        {31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30}, // 2052 BS
        {31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30}, // 2053 BS
        {31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31}, // 2054 BS
        {31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30}, // 2055 BS
        {31, 31, 32, 31, 32, 30, 30, 29, 30, 29, 30, 30}, // 2056 BS
        {31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31}, // 2057 BS
        {30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31}, // 2058 BS
        {31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30}, // 2059 BS
        {31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30}, // 2060 BS
        {31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31}, // 2061 BS
        {31, 31, 31, 32, 31, 31, 29, 30, 29, 30, 29, 31}, // 2062 BS
        {31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30}, // 2063 BS
        {31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30}, // 2064 BS
        {31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31}, // 2065 BS
        {31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 29, 31}, // 2066 BS
        {31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30}, // 2067 BS
        {31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30}, // 2068 BS
        {31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31}, // 2069 BS
        {31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 30, 30}, // 2070 BS
        {31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30}, // 2071 BS
        {31, 32, 31, 32, 31, 30, 30, 29, 30, 29, 30, 30}, // 2072 BS
        {31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31}, // 2073 BS
        {31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30}, // 2074 BS
        {31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30}, // 2075 BS
        {31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30}, // 2076 BS
        {31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31}, // 2077 BS
        {31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30}, // 2078 BS
        {31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30}, // 2079 BS
        {31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30}, // 2080 BS
        {31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31}, // 2081 BS
        {31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30}, // 2082 BS
        {31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30}, // 2083 BS
        {31, 31, 32, 31, 31, 30, 30, 30, 29, 30, 30, 30}, // 2084 BS
        {31, 32, 31, 32, 30, 31, 30, 30, 29, 30, 30, 30}, // 2085 BS
        {30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 30, 30}, // 2086 BS
        {31, 31, 32, 31, 31, 31, 30, 30, 30, 30, 30, 30}, // 2087 BS
        {30, 31, 32, 32, 30, 31, 30, 30, 29, 30, 30, 30}, // 2088 BS
        {30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 30, 30}, // 2089 BS
        {30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 30, 30}, // 2090 BS
        {31, 31, 32, 31, 31, 31, 30, 30, 29, 30, 30, 30}, // 2091 BS
        {30, 31, 32, 32, 31, 30, 30, 30, 29, 30, 30, 30}, // 2092 BS
        {30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 30, 30}, // 2093 BS
        {31, 31, 32, 31, 31, 30, 30, 30, 29, 30, 30, 30}, // 2094 BS
        {31, 31, 32, 31, 31, 31, 30, 29, 30, 30, 30, 30}, // 2095 BS
        {30, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30}, // 2096 BS
        {31, 32, 31, 31, 31, 30, 30, 30, 29, 30, 30, 30}, // 2097 BS
        {31, 31, 32, 31, 31, 31, 29, 30, 29, 30, 29, 31}, // 2098 BS
        {31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31}, // 2099 BS
        {30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31}  // 2100 BS
    };

    // Reference Anchor: 1 Baisakh 2000 BS = April 14, 1943 AD
    private static final int START_BS_YEAR = 2000;
    private static final long ANCHOR_TIME_MS;

    // Pre-computed Cumulative Month Days table for instantaneous O(1) runtime lookup
    private static final int CUMULATIVE_MONTHS = BS_MONTH_DAYS.length * 12;
    private static final long[] MONTH_CUMULATIVE_DAYS = new long[CUMULATIVE_MONTHS + 1];
    private static final int[] MONTH_BS_YEAR = new int[CUMULATIVE_MONTHS];
    private static final int[] MONTH_BS_INDEX = new int[CUMULATIVE_MONTHS];

    static {
        Calendar anchor = Calendar.getInstance();
        anchor.set(1943, Calendar.APRIL, 14, 0, 0, 0);
        anchor.set(Calendar.MILLISECOND, 0);
        ANCHOR_TIME_MS = anchor.getTimeInMillis();

        long accumDays = 0;
        int idx = 0;
        for (int y = 0; y < BS_MONTH_DAYS.length; y++) {
            int year = START_BS_YEAR + y;
            for (int m = 0; m < 12; m++) {
                MONTH_CUMULATIVE_DAYS[idx] = accumDays;
                MONTH_BS_YEAR[idx] = year;
                MONTH_BS_INDEX[idx] = m;
                accumDays += BS_MONTH_DAYS[y][m];
                idx++;
            }
        }
        MONTH_CUMULATIVE_DAYS[idx] = accumDays; // End sentinel
    }

    private static final String[] MONTHS_NEPALI = {
        "बैशाख", "जेठ", "असार", "साउन", "भदौ", "असोज", "कार्तिक", "मंसिर", "पुष", "माघ", "फागुन", "चैत"
    };

    private static final String[] NEPALI_NUMBERS = {
        "०१","०२","०३","०४","०५","०६","०७","०८","०९","१०",
        "११","१२","१३","१४","१५","१६","१७","१८","१९","२०",
        "२१","२२","२३","२४","२५","२६","२७","२८","२९","३०",
        "३१","३२"
    };

    private int cacheYear = 0, cacheMonth = 0, cacheDay = 0;
    private String cacheDate = null;

    public String getMonth() {
        Calendar today = Calendar.getInstance();
        int curYear = today.get(Calendar.YEAR);
        int curMonth = today.get(Calendar.MONTH);
        int curDay = today.get(Calendar.DAY_OF_MONTH);

        if (curDay == cacheDay && curYear == cacheYear && curMonth == cacheMonth && cacheDate != null) {
            return cacheDate;
        }

        String englishDate = today.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.ENGLISH) + " " + curDay;
        String dayOfWeek = today.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.ENGLISH);

        long todayMs = today.getTimeInMillis();
        if (todayMs < ANCHOR_TIME_MS) {
            cacheDate = dayOfWeek + ", " + englishDate;
            return cacheDate;
        }

        // Calculate days elapsed from 1 Baisakh 2000 BS (April 13, 1943)
        long daysElapsed = (todayMs - ANCHOR_TIME_MS) / (24 * 60 * 60 * 1000L);

        // Instant pre-computed cumulative lookup
        int m = 0;
        while (m < CUMULATIVE_MONTHS && daysElapsed >= MONTH_CUMULATIVE_DAYS[m + 1]) {
            m++;
        }

        if (m >= CUMULATIVE_MONTHS) {
            cacheDate = dayOfWeek + ", " + englishDate;
            return cacheDate;
        }

        int bsDay = (int) (daysElapsed - MONTH_CUMULATIVE_DAYS[m]) + 1;
        int bsMonthIndex = MONTH_BS_INDEX[m];

        String nepaliDayStr = (bsDay >= 1 && bsDay <= NEPALI_NUMBERS.length) ? NEPALI_NUMBERS[bsDay - 1] : String.valueOf(bsDay);
        cacheDate = dayOfWeek + ", " + MONTHS_NEPALI[bsMonthIndex] + " " + nepaliDayStr + " 😊 " + englishDate;

        cacheDay = curDay;
        cacheMonth = curMonth;
        cacheYear = curYear;

        return cacheDate;
    }
}
