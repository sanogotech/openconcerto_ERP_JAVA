package org.openconcerto.modules.operation;

import java.awt.Font;
import java.awt.print.PageFormat;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.List;

import org.jopencalendar.model.JCalendarItem;
import org.jopencalendar.print.CalendarItemPrinter;

public class OperationCalendarItemPrinter extends CalendarItemPrinter {
    public static final Font FONT_LINE = new Font("Arial", Font.PLAIN, 10);

    public OperationCalendarItemPrinter(String title, List<JCalendarItem> items, PageFormat pf) {
        super(title, items, pf);
    }

    @Override
    public String getLine1Text(JCalendarItem item) {
        final String siteName = ((JCalendarItemDB) item).getSiteName().toUpperCase();
        final String siteComment = ((JCalendarItemDB) item).getSiteComment();
        if (siteComment != null && !siteComment.isEmpty()) {
            return siteName + " - " + siteComment;
        }
        return siteName;
    }

    @Override
    public String getLine2Text(JCalendarItem item) {
        final String type = ((JCalendarItemDB) item).getType();
        final String description = item.getDescription();
        if (description != null && !description.isEmpty()) {
            return type + " - " + description;
        }
        return type;
    }

    @Override
    public String getTitle() {
        final List<JCalendarItem> items = this.getItems();
        int totalMinutes = 0;
        for (JCalendarItem jCalendarItem : items) {
            long t2 = jCalendarItem.getDtEnd().getTimeInMillis();
            long t1 = jCalendarItem.getDtStart().getTimeInMillis();
            int min = (int) ((t2 - t1) / (1000 * 60));
            totalMinutes += min;
        }

        DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT);
        String title = super.getTitle();
        title += " (au " + df.format(Calendar.getInstance().getTime()) + ") Total : ";
        String m = String.valueOf(totalMinutes % 60);
        if (m.length() < 2) {
            m = "0" + m;
        }
        title += totalMinutes / 60 + "h " + m + " m";

        return title;
    }

    @Override
    public String getDate(DateFormat df, JCalendarItem item) {
        String str = super.getDate(df, item);
        int week = item.getDtStart().get(Calendar.WEEK_OF_YEAR);
        return str + ", semaine " + week;
    }

    @Override
    public Font getLine1Font(JCalendarItem item) {
        return FONT_LINE;
    }

    @Override
    public Font getLine2Font(JCalendarItem item) {
        return FONT_LINE;
    }
}
