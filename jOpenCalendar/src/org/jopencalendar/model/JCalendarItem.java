package org.jopencalendar.model;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class JCalendarItem {
    private String summary;
    private String description;
    private String location;
    private Calendar dtStart;
    private Calendar dtEnd;
    private JCalendarItemGroup group;
    private boolean isDayOnly;
    private List<Flag> flags = new ArrayList<Flag>();
    private Color color = Color.DARK_GRAY;
    private Object cookie;
    private Object userId;
    private String uid;

    public String getUId() {
        return this.uid;
    }

    public void setUId(String uid) {
        this.uid = uid;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Calendar getDtStart() {
        return dtStart;
    }

    public void setDtStart(Calendar dtStart) {
        this.dtStart = (Calendar) dtStart.clone();
        fixDayOnly(isDayOnly);
    }

    public Calendar getDtEnd() {
        return dtEnd;
    }

    public void setDtEnd(Calendar dtEnd) {
        this.dtEnd = (Calendar) dtEnd.clone();
        fixDayOnly(isDayOnly);
    }

    public void setGroup(JCalendarItemGroup group) {
        if (group == this.group) {
            return;
        }
        if (this.group != null) {
            this.group.removeItem(this);
        }
        this.group = group;
        if (this.group != null) {
            this.group.addItem(this);
        }
    }

    public JCalendarItemGroup getGroup() {
        return group;
    }

    public boolean isDayOnly() {
        return isDayOnly;
    }

    public void setDayOnly(boolean isDayOnly) {
        this.isDayOnly = isDayOnly;
        fixDayOnly(isDayOnly);
    }

    private void fixDayOnly(boolean isDayOnly) {
        if (isDayOnly) {
            this.dtStart.set(Calendar.HOUR_OF_DAY, 0);
            this.dtStart.set(Calendar.MINUTE, 0);
            this.dtStart.set(Calendar.SECOND, 0);
            this.dtStart.set(Calendar.MILLISECOND, 0);

            this.dtEnd.set(Calendar.HOUR_OF_DAY, 23);
            this.dtEnd.set(Calendar.MINUTE, 59);
            this.dtEnd.set(Calendar.SECOND, 59);
            this.dtEnd.set(Calendar.MILLISECOND, 999);
        }
    }

    public void addFlag(Flag f) {
        if (f == null) {
            throw new IllegalArgumentException("Flag cannot be null");
        }
        flags.add(f);
    }

    public Color getColor() {
        return this.color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public void removeFlag(Flag f) {
        flags.remove(f);
    }

    public Object getUserId() {
        return userId;
    }

    public void setUserId(Object userId) {
        this.userId = userId;
    }

    public Object getCookie() {
        return this.cookie;
    }

    public void setCookie(Object cookie) {
        this.cookie = cookie;
    }

    @Override
    public String toString() {
        return getDtStart().getTime() + " - " + getDtEnd().getTime() + " " + isDayOnly;
    }

    public boolean hasFlag(Flag flag) {
        return flags.contains(flag);
    }

    public List<Flag> getFlags() {
        return new ArrayList<Flag>(flags);
    }
}
