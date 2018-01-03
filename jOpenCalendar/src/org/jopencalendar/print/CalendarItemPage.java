package org.jopencalendar.print;

import java.util.ArrayList;
import java.util.List;
import org.jopencalendar.model.JCalendarItem;

public class CalendarItemPage {

    private final List<JCalendarItem> items = new ArrayList<JCalendarItem>();
    private final List<Double> heights = new ArrayList<Double>();
    private final List<Boolean> showDates = new ArrayList<Boolean>();
    private int pageIndex;

    public void add(JCalendarItem item) {
        items.add(item);
    }

    public List<JCalendarItem> getItems() {
        return items;
    }

    public void addHeight(Double h) {
        heights.add(h);
    }

    public List<Double> getHeights() {
        return heights;
    }

    public void addShowDate(Boolean showDate) {
        showDates.add(showDate);
    }

    public List<Boolean> getShowDates() {
        return showDates;
    }

    public int getPageIndex() {
        return pageIndex;
    }

    public void setPageIndex(int pageIndex) {
        this.pageIndex = pageIndex;
    }
}
