package org.jopencalendar.model;

public class JCalendarItemPart {

    private final JCalendarItem item;
    private final int year;
    private final int dayOfYear;
    private final int startHour, startMinute;
    private final int durationInMinute;

    /**
     * Part of a JCalendarItem
     * */
    public JCalendarItemPart(JCalendarItem item, int year, int dayOfYear, int startHour, int startMinute, int durationInMinute) {
        if (item == null) {
            throw new IllegalArgumentException("null item");
        }
        this.item = item;
        this.year = year;
        this.dayOfYear = dayOfYear;
        this.startHour = startHour;
        this.startMinute = startMinute;
        this.durationInMinute = durationInMinute;
    }

    public final JCalendarItem getItem() {
        return item;
    }

    public final int getYear() {
        return year;
    }

    public final int getDayOfYear() {
        return dayOfYear;
    }

    public final int getStartHour() {
        return startHour;
    }

    public final int getStartMinute() {
        return startMinute;
    }

    public final int getDurationInMinute() {
        return durationInMinute;
    }

    public boolean conflictWith(JCalendarItemPart p) {
        if (p.year != year || p.dayOfYear != dayOfYear) {
            return false;
        }
        if ((startHour * 60 + startMinute + durationInMinute) <= (p.startHour * 60 + p.startMinute)) {
            return false;
        }
        if ((p.startHour * 60 + p.startMinute + p.durationInMinute) <= (startHour * 60 + startMinute)) {
            return false;
        }
        return true;
    }

}
