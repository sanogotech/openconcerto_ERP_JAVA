package org.jopencalendar.model;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class JCalendarItemGroup {
    private List<JCalendarItem> items = new ArrayList<JCalendarItem>();
    private String name;
    private Object cookie;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void addItem(JCalendarItem item) {
        if (item == null) {
            throw new IllegalArgumentException("item cannot be null");
        }
        if (!this.items.contains(item)) {
            this.items.add(item);
            if (item.getGroup() != this) {
                item.setGroup(this);
            }

        }
    }

    @Override
    public String toString() {
        return getName();
    }

    public boolean removeItem(JCalendarItem item) {
        if (item == null) {
            throw new IllegalArgumentException("item cannot be null");
        }
        if (item.getGroup() != null) {
            item.setGroup(null);
        }
        return this.items.remove(item);
    }

    public int getItemCount() {
        return this.items.size();
    }

    public JCalendarItem getItem(int index) {
        return this.items.get(index);
    }

    public Object getCookie() {
        return this.cookie;
    }

    public void setCookie(Object cookie) {
        this.cookie = cookie;
    }

    public static List<JCalendarItemGroup> getGroups(List<JCalendarItem> items) {
        LinkedHashSet<JCalendarItemGroup> set = new LinkedHashSet<JCalendarItemGroup>();
        for (JCalendarItem jCalendarItem : items) {
            set.add(jCalendarItem.getGroup());
        }
        return new ArrayList<JCalendarItemGroup>(set);
    }

}
