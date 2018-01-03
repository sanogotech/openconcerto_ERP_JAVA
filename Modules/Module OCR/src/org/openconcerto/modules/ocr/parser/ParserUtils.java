package org.openconcerto.modules.ocr.parser;

import java.util.Calendar;
import java.util.Date;

public class ParserUtils {
    public static Date parseDate(String text) {
        Date result = null;
        if (text.length() == 8) {
            text = text.substring(0, 2) + "." + text.substring(2, 4) + "." + text.substring(4, 8);
        } else if((result = getFromFrenchDate(text)) != null){
            return result;
        } else {
            text = text.replace("/", ".").replace("\\", ".").replace("'", ".").replace("-", ".").replace("‘", ".").replace(" ", "");
        }
        final int textLength = text.length();
        final StringBuilder date = new StringBuilder();
        for(int i = 0; i < textLength; i++){
            final String charac = text.substring(i, i + 1);
            if(isInteger(charac)){
                date.append(charac);
                for(int j = i + 1; j < textLength && date.length() < 10; j++){
                    final String charac2 = text.substring(j, j + 1);
                    if(charac2.equals(".") || isInteger(charac2)){
                        date.append(charac2);
                    } else if(!charac2.equals(" ")){
                        break;
                    }
                }
                final String[] testDate = date.toString().split("\\.");
                final Calendar c = Calendar.getInstance();
                int day, month, year = -1, swap;
                if (testDate.length > 2) {
                    try {
                        day = Integer.parseInt(testDate[0]);
                        month = Integer.parseInt(testDate[1]) - 1;
                        if(testDate[2].length() == 2){
                            year = Integer.parseInt("20" + testDate[2]);
                        } else {
                            year = Integer.parseInt(testDate[2]);
                        }
                        if (month > 12 && month <= 31 && day <= 12) {
                            swap = month;
                            month = day;
                            day = swap;
                        }
                        if (month < 12 && month >= 0 && day <= 31 && day > 0 && year > 2000 && year < 4000) {
                            c.set(Calendar.DAY_OF_MONTH, day);
                            c.set(Calendar.MONTH, month);
                            c.set(Calendar.YEAR, year);
                            result = c.getTime();
                            break;
                        }
                    } catch (Exception ex) {
                        // nothing
                    }
                }
                date.delete(0, date.length());
            }
        }
        return result;
    }

    public final static String getCleanNumberString(String s) {
        if (s.indexOf('.') >= 0 && s.indexOf(',') >= 0) {
            s = s.replace('.', ' ').replace(',', '.');
        } else if (s.indexOf('.') < 0 && s.indexOf(',') >= 0) {
            s = s.replace(',', '.');
        }

        final StringBuilder b = new StringBuilder();
        final int length = s.length();
        for (int i = 0; i < length; i++) {
            final char c = s.charAt(i);
            if (Character.isDigit(c) || c == '.') {
                b.append(c);
            }
        }
        return b.toString();

    }
    
    public final static boolean isLong(String text) {
        try {
            Long.parseLong(text);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public final static boolean isInteger(String text) {
        try {
            Integer.parseInt(text);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
    
    public static boolean compareDate(Date date1, Date date2){
        final Calendar cal = Calendar.getInstance();
        cal.setTime(date1);
        final int year1 = cal.get(Calendar.YEAR), month1 = cal.get(Calendar.MONTH), day1 = cal.get(Calendar.DATE);
        cal.setTime(date2);
        final int year2 = cal.get(Calendar.YEAR), month2 = cal.get(Calendar.MONTH), day2 = cal.get(Calendar.DATE);
        return (year1 == year2 && month1 == month2 && day1 == day2 );
    }

    private static Date getFromFrenchDate(String frenchText) {
        final int i = frenchText.indexOf("20");
        int year = -1;
        if (i != -1 && frenchText.length() >= i + 4) {
            try {
                year = Integer.parseInt(frenchText.substring(i, i + 4));
            } catch (Exception e) {
                // nothing
            }
            frenchText = frenchText.toLowerCase();
            int month = getFrenchMonth(frenchText.substring(0, i));
            if (year < 2000 || month <= 0) {
                return null;
            }
            
            final String[] split = frenchText.split("\\s+");
            final Calendar d = Calendar.getInstance();
            int day = 0;
            for (int j = 0; j < split.length; j++) {
                String sDay = split[j];
                try {
                    day = Integer.parseInt(sDay);
                    if (day > 0 && day < 32) {
                        break;
                    }
                } catch (Exception e) {
                    // nothing
                }
                try {
                    if (day != year) {
                        sDay = sDay.substring(0, 2);
                        day = Integer.parseInt(sDay);
                        if (day > 0 && day < 32) {
                            break;
                        }
                    }
                } catch (Exception e) {
                    // nothing
                }
            }
            if (day > 0) {
                d.set(year, month, day, 0, 0, 0);
            } else {
                d.set(year, month, 1, 0, 0, 0);
            }
            return d.getTime();
        } else {
            return null;
        }
    }

    private static int getFrenchMonth(String frenchDate){
        int month = -1;
        if (frenchDate.contains("anvier")) {
            month = 0;
        } else if (frenchDate.contains("vrier")) {
            month = 1;
        } else if (frenchDate.contains("mars")) {
            month = 2;
        } else if (frenchDate.contains("avr")) {
            month = 3;
        } else if (frenchDate.contains("mai")) {
            month = 4;
        } else if (frenchDate.contains("juin") || frenchDate.contains("ju1n") || (frenchDate.contains("ju") && frenchDate.contains("n"))) {
            month = 5;
        } else if (frenchDate.contains("llet") || (frenchDate.contains("ju") && frenchDate.contains("et"))) {
            month = 6;
        } else if (frenchDate.contains("ao")) {
            month = 7;
        } else if (frenchDate.contains("sept")) {
            month = 8;
        } else if (frenchDate.contains("oct") || frenchDate.contains("obre")) {
            month = 9;
        } else if (frenchDate.contains("novem")) {
            month = 10;
        } else if (frenchDate.contains("cembre") || frenchDate.contains("décerﬁbre")) {
            month = 11;
        }
        return month;
    }
    
    public int LevenshteinDistance(String s0, String s1) {                          
        int len0 = s0.length() + 1;                                                     
        int len1 = s1.length() + 1;                                                     
     
        // the array of distances                                                       
        int[] cost = new int[len0];                                                     
        int[] newcost = new int[len0];                                                  
     
        // initial cost of skipping prefix in String s0                                 
        for (int i = 0; i < len0; i++) cost[i] = i;                                     
     
        // dynamically computing the array of distances                                  
     
        // transformation cost for each letter in s1                                    
        for (int j = 1; j < len1; j++) {                                                
            // initial cost of skipping prefix in String s1                             
            newcost[0] = j;                                                             
     
            // transformation cost for each letter in s0                                
            for(int i = 1; i < len0; i++) {                                             
                // matching current letters in both strings                             
                int match = (s0.charAt(i - 1) == s1.charAt(j - 1)) ? 0 : 1;             
     
                // computing cost for each transformation                               
                int cost_replace = cost[i - 1] + match;                                 
                int cost_insert  = cost[i] + 1;                                         
                int cost_delete  = newcost[i - 1] + 1;                                  
     
                // keep minimum cost                                                    
                newcost[i] = Math.min(Math.min(cost_insert, cost_delete), cost_replace);
            }                                                                           
     
            // swap cost/newcost arrays                                                 
            int[] swap = cost; cost = newcost; newcost = swap;                          
        }                                                                               
     
        // the distance is the cost for transforming all letters in both strings        
        return cost[len0 - 1];                                                          
    }
}
