package org.jopencalendar.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.swing.Scrollable;
import javax.swing.SwingWorker;

import org.jopencalendar.model.JCalendarItem;

public class WeekView extends MultipleDayView implements Scrollable {

    private final JCalendarItemProvider manager;

    // Current week
    private int week;
    private int year;
    // Date of columns
    private int[] daysOfYear = new int[7];
    private int[] years = new int[7];
    private final SimpleDateFormat monthAndYearFormat = new SimpleDateFormat("MMMM yyyy");

    private boolean weekEndMinimised = true;

    public WeekView(JCalendarItemProvider manager) {
        if (manager == null) {
            throw new IllegalArgumentException("null manager");
        }
        this.manager = manager;

    }

    /**
     * Load week
     * 
     * @param week the week number (1 - 52)
     * */
    public void loadWeek(final int week, final int year, boolean forceReload) {
        if (!forceReload) {
            if (week == this.week && year == this.year) {
                return;
            }
        }
        this.week = week;
        this.year = year;
        Calendar c = getFirstDayCalendar();
        final int columnCount = this.getColumnCount();
        for (int i = 0; i < columnCount; i++) {
            this.daysOfYear[i] = c.get(Calendar.DAY_OF_YEAR);
            this.years[i] = c.get(Calendar.YEAR);
            c.add(Calendar.HOUR_OF_DAY, 24);
        }

        SwingWorker<List<JCalendarItem>, Object> w = new SwingWorker<List<JCalendarItem>, Object>() {

            @Override
            protected List<JCalendarItem> doInBackground() throws Exception {
                return manager.getItemInWeek(week, year);
            }

            protected void done() {
                try {
                    List<JCalendarItem> l = get();
                    List<List<JCalendarItem>> items = new ArrayList<List<JCalendarItem>>();
                    for (int i = 0; i < columnCount; i++) {
                        List<JCalendarItem> list = new ArrayList<JCalendarItem>();
                        int d = daysOfYear[i];
                        int y = years[i];
                        for (JCalendarItem jCalendarItem : l) {
                            if (jCalendarItem.getDtStart().get(Calendar.DAY_OF_YEAR) == d && jCalendarItem.getDtStart().get(Calendar.YEAR) == y) {
                                list.add(jCalendarItem);
                            }
                        }
                        items.add(list);
                    }
                    setItems(items);
                } catch (Exception e) {
                    throw new IllegalStateException("error while getting items", e);
                }

            };
        };
        w.execute();
    }

    public void setWeekEndMinimised(boolean b) {
        weekEndMinimised = b;
        repaint();
    }

    public void paintHeader(Graphics g) {

        g.setColor(Color.WHITE);
        g.fillRect(0, 0, this.getWidth() + 100, getHeaderHeight());
        g.setColor(Color.GRAY);
        // Vertical lines
        int columnCount = 7;
        int x = HOURS_LABEL_WIDTH;

        for (int i = 0; i < columnCount - 1; i++) {
            x += getColumnWidth(i);
            g.drawLine(x, YEAR_HEIGHT, x, this.getHeaderHeight());
        }
        g.setColor(Color.DARK_GRAY);
        // Text : month & year
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(getFont().deriveFont(13f));
        int month = getMonth(this.daysOfYear[0], this.years[0]);
        int count = 1;
        for (int i = 1; i < columnCount; i++) {
            int m = getMonth(this.daysOfYear[i], this.years[i]);
            if (m == month) {
                count++;
            }
        }
        if (count == 7) {
            String str = getMonthName(this.daysOfYear[0], this.years[0]);
            Rectangle r = g.getFontMetrics().getStringBounds(str, g).getBounds();
            int w = 0;
            for (int i = 0; i < count; i++) {
                w += getColumnWidth(i);
            }
            g.drawString(str, (int) (HOURS_LABEL_WIDTH + (w - r.getWidth()) / 2), 15);
        } else {
            // draw left month
            String strLeft = getMonthName(this.daysOfYear[0], this.years[0]);
            Rectangle r1 = g.getFontMetrics().getStringBounds(strLeft, g).getBounds();

            int wLeft = 0;
            for (int i = 0; i < count; i++) {
                wLeft += getColumnWidth(i);
            }
            int wRight = 0;
            for (int i = count; i < columnCount; i++) {
                wRight += getColumnWidth(i);
            }

            final int xLeft = (int) (HOURS_LABEL_WIDTH + (wLeft - r1.getWidth()) / 2);
            g.drawString(strLeft, xLeft, 15);

            // draw right month
            String strRight = getMonthName(this.daysOfYear[columnCount - 1], this.years[columnCount - 1]);
            Rectangle r2 = g.getFontMetrics().getStringBounds(strRight, g).getBounds();
            final int xRight = (int) (HOURS_LABEL_WIDTH + wLeft + (wRight - r2.getWidth()) / 2);
            g.drawString(strRight, xRight, 15);

            // draw separator
            g.setColor(Color.GRAY);
            int xSep = HOURS_LABEL_WIDTH + wLeft;
            g.drawLine(xSep, 0, xSep, YEAR_HEIGHT);
        }

        // Text : day
        final Font font1 = getFont().deriveFont(12f);
        g.setFont(font1);
        x = HOURS_LABEL_WIDTH;
        Calendar today = Calendar.getInstance();
        int todayYear = today.get(Calendar.YEAR);
        int todayDay = today.get(Calendar.DAY_OF_YEAR);

        Calendar c = getFirstDayCalendar();
        final DateFormat df;
        if (!weekEndMinimised) {
            df = new SimpleDateFormat("EEEE d");
        } else {
            df = new SimpleDateFormat("E d");
        }
        for (int i = 0; i < columnCount; i++) {
            String str = df.format(c.getTime());
            str = str.substring(0, 1).toUpperCase() + str.substring(1);
            int columnWidth = getColumnWidth(i);
            Rectangle r = g.getFontMetrics().getStringBounds(str, g).getBounds();
            if (c.get(Calendar.YEAR) == todayYear && c.get(Calendar.DAY_OF_YEAR) == todayDay) {
                g.setColor(new Color(232, 242, 254));
                g.fillRect(x + 1, YEAR_HEIGHT, columnWidth - 1, (int) r.getHeight() + 4);
                g.setColor(Color.BLACK);
            } else {
                g.setColor(Color.DARK_GRAY);
            }
            g.drawString(str, (int) (x + (columnWidth - r.getWidth()) / 2), YEAR_HEIGHT + 15);
            x += columnWidth;
            c.add(Calendar.HOUR_OF_DAY, 24);
        }
        paintHeaderAlldays(g);
    }

    private int getMonth(int day, int year) {
        Calendar c = Calendar.getInstance();
        c.set(year, 1, 0, 0, 0, 0);
        c.set(Calendar.MILLISECOND, 0);
        c.set(Calendar.DAY_OF_YEAR, day);
        return c.get(Calendar.MONTH);
    }

    private String getMonthName(int day, int year) {
        Calendar c = Calendar.getInstance();
        c.set(year, 1, 0, 0, 0, 0);
        c.set(Calendar.MILLISECOND, 0);
        c.set(Calendar.DAY_OF_YEAR, day);
        return monthAndYearFormat.format(c.getTime()).toUpperCase();
    }

    private Calendar getFirstDayCalendar() {
        Calendar c = Calendar.getInstance();
        c.set(this.year, 1, 0, 0, 0, 0);
        c.set(Calendar.MILLISECOND, 0);
        c.set(Calendar.WEEK_OF_YEAR, this.week);
        c.set(Calendar.DAY_OF_WEEK, c.getFirstDayOfWeek());
        return c;
    }

    @Override
    public String getColumnTitle(int index) {
        final DateFormat df;
        if (!weekEndMinimised) {
            df = new SimpleDateFormat("EEEE d");
        } else {
            df = new SimpleDateFormat("E d");
        }
        Calendar c = getFirstDayCalendar();
        c.add(Calendar.DAY_OF_YEAR, index);

        String str = df.format(c.getTime());
        str = str.substring(0, 1).toUpperCase() + str.substring(1);
        return str;
    }

    @Override
    public void reload() {
        loadWeek(this.week, this.year, true);
    }

    @Override
    public int getColumnCount() {
        return 7;
    }

    @Override
    public int getColumnWidth(int column) {
        final int c;
        if (weekEndMinimised) {
            int minSize = 50;
            if (column == 5 || column == 6) {
                return minSize;
            } else {
                c = (this.getWidth() - HOURS_LABEL_WIDTH - 2 * minSize) / (getColumnCount() - 2);
            }

        } else {
            c = (this.getWidth() - HOURS_LABEL_WIDTH) / getColumnCount();
        }
        return c;
    }
}
