package org.jopencalendar.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.swing.Scrollable;
import javax.swing.SwingWorker;

import org.jopencalendar.model.JCalendarItem;

public class DayView extends MultipleDayView implements Scrollable {

    private final List<JCalendarItemProvider> providers;
    // Current day
    private int dayOfYear;
    private int year;

    public DayView(List<JCalendarItemProvider> providers) {
        if (providers == null) {
            throw new IllegalArgumentException("null providers");
        }
        this.providers = providers;
    }

    /**
     * Load day
     * 
     */
    public void loadDay(final int dayOfYear, final int year) {
        this.dayOfYear = dayOfYear;
        this.year = year;

        final int columnCount = this.getColumnCount();

        SwingWorker<List<List<JCalendarItem>>, Object> w = new SwingWorker<List<List<JCalendarItem>>, Object>() {

            @Override
            protected List<List<JCalendarItem>> doInBackground() throws Exception {
                List<List<JCalendarItem>> l = new ArrayList<List<JCalendarItem>>(columnCount);
                for (int i = 0; i < columnCount; i++) {
                    List<JCalendarItem> items = providers.get(i).getItemInDay(dayOfYear, year);
                    l.add(items);
                }
                return l;
            }

            protected void done() {
                try {
                    List<List<JCalendarItem>> l = get();
                    setItems(l);
                } catch (Exception e) {
                    throw new IllegalStateException("error while getting items", e);
                }

            };
        };
        w.execute();
    }

    public void paintHeader(Graphics g) {

        g.setColor(Color.WHITE);
        g.fillRect(0, 0, this.getWidth() + 100, getHeaderHeight());
        g.setColor(Color.GRAY);
        // Vertical lines
        int columnCount = getColumnCount();
        if (columnCount < 1) {
            return;
        }
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

        String strTitle = getTitle();

        Rectangle r = g.getFontMetrics().getStringBounds(strTitle, g).getBounds();
        int w = 0;
        for (int i = 0; i < getColumnCount(); i++) {
            w += getColumnWidth(i);
        }
        g.drawString(strTitle, (int) (HOURS_LABEL_WIDTH + (w - r.getWidth()) / 2), 15);

        // Text : day
        final Font font1 = getFont().deriveFont(12f);
        g.setFont(font1);
        x = HOURS_LABEL_WIDTH;
        Rectangle clipRect = g.getClipBounds();
        for (int i = 0; i < columnCount; i++) {
            String str = getColumnTitle(i);
            int columnWidth = getColumnWidth(i);
            g.setClip(x, YEAR_HEIGHT, columnWidth - 1, YEAR_HEIGHT + 20);
            // Centering
            int x2 = (int) (x + (columnWidth - r.getWidth()) / 2);
            // If no room, left align
            if (x2 < x + 2) {
                x2 = x + 2;
            }
            g.drawString(str, x2, YEAR_HEIGHT + 15);
            x += columnWidth;
        }
        g.setClip(clipRect);
        paintHeaderAlldays(g);
    }

    public void reload() {
        loadDay(this.dayOfYear, this.year);
    }

    @Override
    public String getColumnTitle(int index) {
        return providers.get(index).getName();
    }

    @Override
    public String getTitle() {
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(Calendar.YEAR, this.year);
        c.set(Calendar.DAY_OF_YEAR, this.dayOfYear);
        return DateFormat.getDateInstance().format(c.getTime());
    }

    @Override
    public int getColumnCount() {
        return providers.size();
    }
}
