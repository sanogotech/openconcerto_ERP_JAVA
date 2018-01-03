package org.jopencalendar.model;

import java.util.ArrayList;
import java.util.List;

public class JCalendar {
    private List<JCalendarItemGroup> groups = new ArrayList<JCalendarItemGroup>();

    public void addGroup(JCalendarItemGroup g) {
        this.groups.add(g);
    }

    public void removeGroup(JCalendarItemGroup g) {
        this.groups.remove(g);
    }

    public int getGroupCount() {
        return this.groups.size();
    }

    public JCalendarItemGroup getGroup(int index) {
        return this.groups.get(index);
    }

}
