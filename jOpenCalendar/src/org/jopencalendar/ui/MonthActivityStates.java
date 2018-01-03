package org.jopencalendar.ui;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.jopencalendar.model.JCalendarItem;

public class MonthActivityStates {
    // ex : null,null,item1,item1,null x 27 for item1 from 3 to 4 january
    private List<List<JCalendarItem>> l;
    private final int month;
    private final int year;
    private final List<Integer> mIndex = new ArrayList<Integer>(5);
    private int daysInMonth;

    public MonthActivityStates(int month, int year) {
        this.month = month;
        this.year = year;
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(Calendar.YEAR, year);
        c.set(Calendar.MONTH, this.month);
        c.set(Calendar.DAY_OF_MONTH, 1);
        c.set(Calendar.SECOND, 1);
        daysInMonth = c.getActualMaximum(Calendar.DAY_OF_MONTH);

        for (int i = 0; i < daysInMonth; i++) {
            if (c.get(Calendar.DAY_OF_WEEK) == 1) {
                mIndex.add(i + 1);
            }

            c.add(Calendar.DAY_OF_MONTH, 1);

        }

    }

    public void set(JCalendarItem jCalendarItem) {
        if (l == null) {
            l = new ArrayList<List<JCalendarItem>>(daysInMonth);
            for (int i = 0; i < daysInMonth; i++) {

                l.add(null);
            }
        }

        Calendar cStart = jCalendarItem.getDtStart();
        cStart.set(Calendar.HOUR_OF_DAY, 0);
        cStart.set(Calendar.MINUTE, 0);
        cStart.set(Calendar.SECOND, 0);
        cStart.set(Calendar.MILLISECOND, 0);
        Calendar cEnd = jCalendarItem.getDtEnd();
        cEnd.set(Calendar.HOUR_OF_DAY, 0);
        cEnd.set(Calendar.MINUTE, 0);
        cEnd.set(Calendar.SECOND, 0);
        cEnd.set(Calendar.MILLISECOND, 0);
        cEnd.add(Calendar.HOUR_OF_DAY, 24);
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(Calendar.YEAR, year);
        c.set(Calendar.MONTH, this.month);
        c.set(Calendar.DAY_OF_MONTH, 1);
        c.set(Calendar.SECOND, 1);

        final int dayInMonth = l.size();
        for (int i = 0; i < dayInMonth; i++) {
            if (c.after(cStart) && c.before(cEnd)) {
                List<JCalendarItem> list = l.get(i);
                if (list == null) {
                    list = new ArrayList<JCalendarItem>(2);
                    l.set(i, list);
                }
                list.add(jCalendarItem);
            }
            c.add(Calendar.DAY_OF_MONTH, 1);
        }

    }

    public List<List<JCalendarItem>> getList() {
        return l;
    }

    public List<Integer> getMondayIndex() {
        return mIndex;
    }

    @Override
    public String toString() {
        String string = super.toString();
        if (l != null) {
            string += " size: " + l.size() + ", items: " + l.toString();
        }
        return string;
    }

    public int getYear() {
        return year;
    }

    public int getMonth() {
        return month;
    }

}
