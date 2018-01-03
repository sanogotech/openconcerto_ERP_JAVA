package org.jopencalendar.print;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Pageable;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.PrintQuality;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.jopencalendar.LayoutUtils;
import org.jopencalendar.model.Flag;
import org.jopencalendar.model.JCalendarItem;

public class CalendarItemPrinter implements Printable, Pageable {

    private List<JCalendarItem> items;

    private List<CalendarItemPage> pages = null;

    private String title;

    private boolean showDuration = true;

    private PageFormat pf;

    public CalendarItemPrinter(String title, List<JCalendarItem> items, PageFormat pf) {
        this.title = title;
        this.items = items;
        this.pf = pf;
    }

    public String getTitle() {
        return title;
    }

    public static final Font FONT_NORMAL = new Font("Arial", Font.PLAIN, 11);
    public static final Font FONT_BOLD = FONT_NORMAL.deriveFont(Font.BOLD);

    public int print(Graphics g, PageFormat pf, int pageIndex) throws PrinterException {

        if (pages == null) {
            computeLayout(g, pf);
        }

        if (pageIndex >= pages.size()) {
            return NO_SUCH_PAGE;
        }

        final Graphics2D g2d = (Graphics2D) g;
        g2d.translate(pf.getImageableX(), pf.getImageableY());
        final CalendarItemPage page = this.pages.get(pageIndex);

        printPage(g, pf, page);

        /* tell the caller that this page is part of the printed document */
        return PAGE_EXISTS;
    }

    public void printPage(Graphics g, PageFormat pf, final CalendarItemPage page) {
        final Graphics2D g2d = (Graphics2D) g;
        // Title
        g.setFont(getTitleFont());
        g.setColor(getTitleColor());
        final int xTitle = ((int) pf.getImageableWidth() - (int) g2d.getFontMetrics().getStringBounds(getTitle(), g2d).getWidth()) / 2;
        g.drawString(getTitle(), xTitle, g2d.getFontMetrics().getHeight());
        // Page number
        g.setFont(getPageNumberFont());
        g.setColor(getPageNumberColor());
        int y = getTitleHeight();
        final DateFormat df = DateFormat.getDateInstance(DateFormat.FULL);
        final int size = page.getItems().size();
        final String strPages = (page.getPageIndex() + 1) + " / " + pages.size();
        int xStrPages = (int) pf.getImageableWidth() - (int) g2d.getFontMetrics().getStringBounds(strPages, g2d).getWidth();
        g.drawString(strPages, xStrPages, g2d.getFontMetrics().getHeight());

        final double hourColumnWidth = getHourColumnWidth();
        for (int i = 0; i < size; i++) {
            final JCalendarItem item = page.getItems().get(i);
            final double lineHeight = page.getHeights().get(i);
            int lY = y;
            drawBackground(0, lY, (int) pf.getImageableWidth(), (int) lineHeight);
            if (page.getShowDates().get(i)) {
                g2d.setColor(getHourColor(item));
                g.setFont(getHourFont(item));
                int dateHeight = g2d.getFontMetrics().getHeight();
                lY += dateHeight;
                final String format = getDate(df, item);
                g2d.drawString(format, 2, lY);
            }
            // Vertical bar
            g2d.setColor(getVerticalBarColor(item));
            g2d.drawLine((int) hourColumnWidth - 5, lY + 6, (int) hourColumnWidth - 5, (int) (y + lineHeight));

            final Calendar dtStart = item.getDtStart();
            final Calendar dtEnd = item.getDtEnd();
            // Hour
            g.setColor(getHourColor(item));
            g.setFont(getHourFont(item));
            final int hourY = lY + g2d.getFontMetrics().getHeight();
            g2d.drawString(formatTime(dtStart) + " - " + formatTime(dtEnd), 3, hourY);
            // Duration
            if (this.showDuration) {
                g.setColor(getDurationColor(item));
                g.setFont(getDurationFont(item));
                final int durationY = hourY + g2d.getFontMetrics().getHeight();
                g2d.drawString(formatDuration(dtStart, dtEnd), 3, durationY);
            }

            final double maxWidth = pf.getImageableWidth() - hourColumnWidth;
            // Summary
            g.setFont(getLine1Font(item));

            final List<String> l1 = LayoutUtils.wrap(getLine1Text(item), g.getFontMetrics(), (int) maxWidth);
            if (item.hasFlag(Flag.getFlag("warning"))) {
                g2d.setColor(new Color(255, 249, 144));
                g2d.fillRect((int) hourColumnWidth - 1, lY + 3, (int) maxWidth, (int) g2d.getFontMetrics().getHeight() * l1.size());
            }
            g.setColor(getLine1Color(item));
            for (String string : l1) {
                lY += g2d.getFontMetrics().getHeight();
                g2d.drawString(string, (int) hourColumnWidth, lY);
            }
            // Description
            g.setFont(getLine2Font(item));
            g.setColor(getLine2Color(item));
            final List<String> l2 = LayoutUtils.wrap(getLine2Text(item), g.getFontMetrics(), (int) maxWidth);
            for (String string : l2) {
                lY += g2d.getFontMetrics().getHeight();
                g2d.drawString(string, (int) hourColumnWidth, lY);
            }
            //
            y += lineHeight;
        }
    }

    public String getDate(final DateFormat df, final JCalendarItem item) {
        return LayoutUtils.firstUp(df.format(item.getDtStart().getTime()));
    }

    public String formatDuration(Calendar dtStart, Calendar dtEnd) {
        long t2 = dtEnd.getTimeInMillis();
        long t1 = dtStart.getTimeInMillis();
        long seconds = (t2 - t1) / 1000;

        final long minutes = seconds / 60;

        String l = String.valueOf(minutes % 60);
        if (l.length() < 2) {
            l = "0" + l;
        }
        return "(" + minutes / 60 + "h" + l + ")";
    }

    public void drawBackground(int x, int y, int w, int h) {
        // for subclass to add some background
    }

    public Color getPageNumberColor() {
        return Color.BLACK;
    }

    public Font getPageNumberFont() {
        return FONT_NORMAL;
    }

    // Title
    public Color getTitleColor() {
        return Color.BLACK;
    }

    public Font getTitleFont() {
        return FONT_NORMAL;
    }

    // Hour
    public Color getHourColor(JCalendarItem item) {
        return Color.BLACK;
    }

    public Font getHourFont(JCalendarItem item) {
        return FONT_BOLD;
    }

    // Duration
    public Color getDurationColor(JCalendarItem item) {
        return Color.BLACK;
    }

    public Font getDurationFont(JCalendarItem item) {
        return FONT_NORMAL;
    }

    // Line 1
    public Color getLine1Color(final JCalendarItem item) {
        return Color.BLACK;
    }

    public String getLine1Text(final JCalendarItem item) {
        return item.getSummary();
    }

    public Font getLine1Font(final JCalendarItem item) {
        return FONT_NORMAL;
    }

    // Line 2
    public Color getLine2Color(final JCalendarItem item) {
        return Color.BLACK;
    }

    public String getLine2Text(final JCalendarItem item) {
        return item.getDescription();
    }

    public Font getLine2Font(final JCalendarItem item) {
        return FONT_NORMAL;
    }

    public Color getVerticalBarColor(final JCalendarItem item) {
        return new Color(0, 68, 128);
    }

    public void setShowDuration(boolean showDuration) {
        this.showDuration = showDuration;
    }

    public String formatTime(final Calendar dtStart) {
        String h = String.valueOf(dtStart.get(Calendar.HOUR_OF_DAY));
        String m = String.valueOf(dtStart.get(Calendar.MINUTE));
        if (h.length() < 2) {
            h = " " + h;
        }
        if (m.length() < 2) {
            m = "0" + m;
        }
        return h + ":" + m;
    }

    private void computeLayout(Graphics g, PageFormat pf) {
        pages = new ArrayList<CalendarItemPage>();
        CalendarItemPage page = new CalendarItemPage();
        this.pages.add(page);
        if (items.isEmpty()) {
            return;
        }
        double remainingHeight = pf.getImageableHeight() - getTitleHeight();
        boolean showDate = true;
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(this.items.get(0).getDtStart().getTimeInMillis());
        for (int i = 0; i < this.items.size(); i++) {
            JCalendarItem item = this.items.get(i);
            if (i > 0) {
                showDate = (item.getDtStart().get(Calendar.YEAR) != c.get(Calendar.YEAR) || item.getDtStart().get(Calendar.DAY_OF_YEAR) != c.get(Calendar.DAY_OF_YEAR));
            }
            c = item.getDtStart();
            double h = getPrintHeight(g, pf, item);
            double secureMargin = 20D;
            if (remainingHeight < h + secureMargin) {
                page = new CalendarItemPage();
                showDate = true;
                remainingHeight = pf.getImageableHeight() - getTitleHeight();
                this.pages.add(page);
            }
            if (showDate) {
                g.setFont(getHourFont(item));
                h += g.getFontMetrics().getHeight();
            }
            remainingHeight -= h;

            page.add(item);
            page.addHeight(Double.valueOf(h));
            page.addShowDate(showDate);
        }

    }

    public int getHourColumnWidth() {
        return 90;
    }

    public int getTitleHeight() {
        return 40;
    }

    private double getPrintHeight(Graphics g, PageFormat pf, JCalendarItem item) {
        double maxWidth = pf.getImageableWidth() - getHourColumnWidth();
        g.setFont(this.getLine1Font(item));
        int l1 = LayoutUtils.wrap(getLine1Text(item), g.getFontMetrics(), (int) maxWidth).size();
        g.setFont(this.getLine2Font(item));
        int l2 = LayoutUtils.wrap(getLine2Text(item), g.getFontMetrics(), (int) maxWidth).size();
        return (l1 + l2) * g.getFontMetrics().getHeight();
    }

    public List<CalendarItemPage> getPages() {
        return pages;
    }

    public static void main(String args[]) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                final JFrame f = new JFrame("Printing Pagination Example");
                f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                final JButton printButton = new JButton("Print test pages");
                printButton.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        PrinterJob job = PrinterJob.getPrinterJob();
                        List<JCalendarItem> items = new ArrayList<JCalendarItem>();
                        Calendar c = Calendar.getInstance();
                        Flag.register(new Flag("warning", null, "Warning", "A default warning"));
                        for (int i = 0; i < 50; i++) {
                            JCalendarItem item = new JCalendarItem();
                            item.setSummary("Item " + i);
                            StringBuilder d = new StringBuilder();
                            d.append("Description");
                            for (int j = 0; j < i; j++) {
                                if (i % 2 == 0)
                                    d.append(" Hello");
                                else
                                    d.append(" World");
                            }
                            d.append("END");
                            if (i % 6 == 0) {
                                item.addFlag(Flag.getFlag("warning"));
                            }
                            item.setDescription(d.toString());
                            item.setDtStart(c);
                            c.add(Calendar.HOUR_OF_DAY, 1);
                            item.setDtEnd(c);
                            c.add(Calendar.HOUR_OF_DAY, 1);
                            items.add(item);
                        }
                        final PrintRequestAttributeSet printAttributes = new HashPrintRequestAttributeSet();
                        printAttributes.add(PrintQuality.HIGH);
                        job.setPrintable(new CalendarItemPrinter("OpenConcerto", items, job.getPageFormat(printAttributes)));
                        boolean ok = job.printDialog();
                        if (ok) {
                            try {
                                job.print();
                            } catch (PrinterException ex) {
                                /* The job did not successfully complete */
                                ex.printStackTrace();
                            }
                        }

                    }
                });
                f.add("Center", printButton);
                f.pack();
                f.setLocationRelativeTo(null);
                f.setVisible(true);
            }
        });
    }

    public List<JCalendarItem> getItems() {
        return this.items;
    }

    @Override
    public int getNumberOfPages() {
        if (this.pages == null) {
            BufferedImage off_Image = new BufferedImage((int) pf.getHeight(), (int) pf.getWidth(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = off_Image.createGraphics();
            computeLayout(g2, pf);
        }
        return this.pages.size();
    }

    @Override
    public PageFormat getPageFormat(int pageIndex) throws IndexOutOfBoundsException {
        return this.pf;
    }

    @Override
    public Printable getPrintable(int pageIndex) throws IndexOutOfBoundsException {
        final CalendarItemPage page = this.pages.get(pageIndex);
        return new Printable() {

            @Override
            public int print(Graphics graphics, PageFormat pageFormat, int i) throws PrinterException {
                final Graphics2D g2d = (Graphics2D) graphics;
                g2d.translate(pf.getImageableX(), pf.getImageableY());
                printPage(graphics, pageFormat, page);
                return PAGE_EXISTS;
            }
        };
    }
}
