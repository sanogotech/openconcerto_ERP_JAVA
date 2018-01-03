package org.jopencalendar.ui;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.swing.ImageIcon;

import org.jopencalendar.model.Flag;
import org.jopencalendar.model.JCalendarItem;
import org.jopencalendar.model.JCalendarItemGroup;

public class JCalendarItemProvider {
    private String name;

    public JCalendarItemProvider(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public List<JCalendarItem> getItemInDay(int dayOfYear, int year) {
        List<JCalendarItem> l = new ArrayList<JCalendarItem>();
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.DAY_OF_YEAR, dayOfYear);
        cal.add(Calendar.HOUR_OF_DAY, 8);
        int gCount = 0;
        JCalendarItemGroup g = new JCalendarItemGroup();
        g.setName("Group " + gCount);
        Flag flag = new Flag("planned", new ImageIcon(JCalendarItemProvider.class.getResource("calendar_small.png")), "Planned", "planned item");
        for (int d = 0; d < 8; d++) {
            JCalendarItem i = new JCalendarItem();
            i.addFlag(flag);
            i.setSummary(year + ": day " + dayOfYear);
            i.setDtStart(cal);
            Calendar cal2 = (Calendar) cal.clone();
            cal2.add(Calendar.HOUR_OF_DAY, 1);
            i.setDtEnd(cal2);
            i.setDayOnly(false);
            g.addItem(i);
            l.add(i);
            if (d % 3 == 0) {
                gCount++;
                g = new JCalendarItemGroup();
                g.setName("Group " + gCount);
            }
            if (d % 2 == 0) {
                i.setLocation("Location" + d);
            }
        }

        return l;
    }

    /**
     * @param week 1 - 52
     * @param year
     */
    public List<JCalendarItem> getItemInWeek(int week, int year) {
        final List<JCalendarItem> l = new ArrayList<JCalendarItem>();
        JCalendarItem i = new JCalendarItem();
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.WEEK_OF_YEAR, week);
        cal.set(Calendar.HOUR_OF_DAY, 8);
        cal.set(Calendar.MINUTE, 0);
        i.setSummary("In W: " + week + " year:" + year);
        i.setDtStart(cal);
        Calendar cal2 = (Calendar) cal.clone();
        cal2.add(Calendar.HOUR_OF_DAY, 1);
        i.setDtEnd(cal2);
        i.setDayOnly(true);
        l.add(i);
        int gCount = 0;
        JCalendarItemGroup g = new JCalendarItemGroup();
        g.setName("Group " + gCount);
        final Flag flag = new Flag("planned", new ImageIcon(JCalendarItemProvider.class.getResource("calendar_small.png")), "Planned", "planned item");

        for (int d = 1; d < 6; d++) {
            {
                JCalendarItem item = new JCalendarItem();
                item.addFlag(flag);
                cal.clear();
                cal.set(Calendar.YEAR, year);
                cal.set(Calendar.WEEK_OF_YEAR, week);
                cal.set(Calendar.HOUR_OF_DAY, 7);
                cal.set(Calendar.MINUTE, 0);
                cal.add(Calendar.DAY_OF_WEEK, d);
                item.setSummary(year + ": day " + d);
                item.setDtStart(cal);
                Calendar c2 = (Calendar) cal.clone();
                c2.set(Calendar.HOUR_OF_DAY, 7 + d);
                item.setDtEnd(c2);
                item.setDayOnly(false);
                g.addItem(item);
                l.add(item);
                if (d % 2 == 0) {
                    gCount++;
                    g = new JCalendarItemGroup();
                    g.setName("Group " + gCount);
                }
                if (d % 2 == 0) {
                    i.setLocation("Location" + d);
                }
            }
            {
                JCalendarItem item = new JCalendarItem();
                cal.clear();
                cal.set(Calendar.YEAR, year);
                cal.set(Calendar.WEEK_OF_YEAR, week);
                cal.set(Calendar.HOUR_OF_DAY, 7);
                cal.set(Calendar.MINUTE, 0);
                cal.add(Calendar.DAY_OF_WEEK, d);
                item.setSummary(year + ": day " + d);
                item.setDtStart(cal);
                Calendar c2 = (Calendar) cal.clone();
                c2.set(Calendar.HOUR_OF_DAY, 7 + d);
                item.setDtEnd(c2);
                item.setDayOnly(false);
                g.addItem(item);
                l.add(item);
                if (d % 2 == 0) {
                    gCount++;
                    g = new JCalendarItemGroup();
                    g.setName("Group " + gCount);
                }
            }
            {
                JCalendarItem item = new JCalendarItem();
                cal.clear();
                cal.set(Calendar.YEAR, year);
                cal.set(Calendar.WEEK_OF_YEAR, week);
                cal.set(Calendar.HOUR_OF_DAY, 7);
                cal.set(Calendar.MINUTE, 0);
                cal.add(Calendar.DAY_OF_WEEK, d);
                item.setSummary(year + ": day " + d);
                item.setDtStart(cal);
                Calendar c2 = (Calendar) cal.clone();
                c2.set(Calendar.HOUR_OF_DAY, 7 + d);
                item.setDtEnd(c2);
                item.setDayOnly(false);
                g.addItem(item);
                l.add(item);
                if (d % 2 == 0) {
                    gCount++;
                    g = new JCalendarItemGroup();
                    g.setName("Group " + gCount);
                }
            }
            {
                JCalendarItem item = new JCalendarItem();
                cal.clear();
                cal.set(Calendar.YEAR, year);
                cal.set(Calendar.WEEK_OF_YEAR, week);
                cal.set(Calendar.HOUR_OF_DAY, 8);
                cal.set(Calendar.MINUTE, 0);
                cal.add(Calendar.DAY_OF_WEEK, d);
                item.setSummary(year + ": day " + d);
                item.setDtStart(cal);
                Calendar c2 = (Calendar) cal.clone();
                c2.set(Calendar.HOUR_OF_DAY, 8 + d);
                item.setDtEnd(c2);
                item.setDayOnly(false);
                g.addItem(item);
                l.add(item);
                if (d % 2 == 0) {
                    gCount++;
                    g = new JCalendarItemGroup();
                    g.setName("Group " + gCount);
                }
            }
        }

        return l;
    }

    public List<JCalendarItem> getItemInYear(int year, int week1, int week2) {
        List<JCalendarItem> l = new ArrayList<JCalendarItem>();
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(Calendar.YEAR, year);
        int gCount = 0;
        JCalendarItemGroup g = new JCalendarItemGroup();
        JCalendarItemGroup g0 = g;
        g.setName("Group " + gCount);
        for (int d = 1; d < 350; d++) {
            JCalendarItem i = new JCalendarItem();
            cal.set(Calendar.DAY_OF_YEAR, d);
            cal.set(Calendar.HOUR_OF_DAY, 8);
            cal.set(Calendar.MINUTE, 0);
            i.setSummary(year + ": day " + d);
            i.setDtStart(cal);
            Calendar cal2 = (Calendar) cal.clone();
            cal2.add(Calendar.HOUR_OF_DAY, 1);
            i.setDtEnd(cal2);
            i.setDayOnly(false);
            g.addItem(i);
            l.add(i);
            if (d % 8 == 0) {
                gCount++;
                g = new JCalendarItemGroup();
                g.setName("Group " + gCount);
            }

        }
        JCalendarItem i = new JCalendarItem();
        i.setColor(Color.RED);
        cal.set(Calendar.DAY_OF_YEAR, 3);
        cal.set(Calendar.HOUR_OF_DAY, 8);
        cal.set(Calendar.MINUTE, 0);
        i.setSummary(year + ": day " + 3);
        i.setDtStart(cal);
        Calendar cal2 = (Calendar) cal.clone();
        cal2.add(Calendar.HOUR_OF_DAY, 1);
        i.setDtEnd(cal2);
        i.setDayOnly(false);
        g0.addItem(i);
        l.add(i);

        return l;
    }
}
