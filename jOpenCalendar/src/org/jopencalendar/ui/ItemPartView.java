package org.jopencalendar.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JPanel;

import org.jopencalendar.LayoutUtils;
import org.jopencalendar.model.Flag;
import org.jopencalendar.model.JCalendarItem;
import org.jopencalendar.model.JCalendarItemPart;

public class ItemPartView {
    private static final BasicStroke STROKE_1 = new BasicStroke(1f);

    private JCalendarItemPart itemPart;

    private int column;

    private int x;

    private boolean hasTopBorder;
    private boolean hasBottomBorder;

    private Font fontSummary;
    private Font fontDescription;

    private Rectangle rectangle;
    private boolean selected;

    private List<ItemPartView> parts = new ArrayList<ItemPartView>();

    private int maxColumn = 1;

    private int width = 1;

    public static final Color COLOR_HIGHLIGHT_BLUE = new Color(232, 242, 250);

    /**
     * Part of a JCalendarItem The item is split to 1 ItemPart per day
     */
    public ItemPartView(JCalendarItemPart itemPart) {
        if (itemPart == null) {
            throw new IllegalArgumentException("null itemPart");
        }
        this.itemPart = itemPart;
    }

    public Font getFontSummary() {
        if (fontSummary == null) {
            this.fontSummary = new Font("Arial", Font.BOLD, 12);
        }
        return fontSummary;
    }

    public void setFontSummary(Font fontSummary) {
        this.fontSummary = fontSummary;
    }

    public Font getFontDescription() {
        if (fontDescription == null) {
            this.fontDescription = new Font("Arial", Font.PLAIN, 12);
        }
        return fontDescription;
    }

    public void setFontDescription(Font fontDescription) {
        this.fontDescription = fontDescription;
    }

    public static List<ItemPartView> split(JCalendarItem item) {
        if (!item.getDtStart().before(item.getDtEnd())) {
            throw new IllegalArgumentException("Start is not before end. Start " + item.getDtStart().getTime() + " End " + item.getDtEnd().getTime());
        }
        final List<ItemPartView> result = new ArrayList<ItemPartView>();
        final Calendar c = (Calendar) item.getDtStart().clone();
        while (c.before(item.getDtEnd())) {
            JCalendarItemPart itemPart = new JCalendarItemPart(item, c.get(Calendar.YEAR), c.get(Calendar.DAY_OF_YEAR), 0, 0, 0);
            final ItemPartView pv = new ItemPartView(itemPart);
            result.add(pv);
            // next
            c.add(Calendar.HOUR, 24);
        }
        final int size = result.size();
        if (size == 0) {
            throw new IllegalStateException("No part");
        } else if (size == 1) {
            final ItemPartView p = result.get(0);
            int startHour = item.getDtStart().get(Calendar.HOUR_OF_DAY);
            int startMinute = item.getDtStart().get(Calendar.MINUTE);
            final int endH = item.getDtEnd().get(Calendar.HOUR_OF_DAY);
            final int endM = item.getDtEnd().get(Calendar.MINUTE);
            int durationInMinute = (endH * 60 + endM) - (startHour * 60 + startMinute);
            p.setItemPart(new JCalendarItemPart(item, p.getYear(), p.getDayInYear(), startHour, startMinute, durationInMinute));
            p.hasTopBorder = true;
            p.hasBottomBorder = true;
        } else {
            // First
            final ItemPartView p0 = result.get(0);

            int p0StartHour = item.getDtStart().get(Calendar.HOUR_OF_DAY);
            int p0StartMinute = item.getDtStart().get(Calendar.MINUTE);
            int p0DurationInMinute = (24 * 60) - (p0StartHour * 60 + p0StartMinute);
            p0.setItemPart(new JCalendarItemPart(item, p0.getYear(), p0.getDayInYear(), p0StartHour, p0StartMinute, p0DurationInMinute));

            p0.hasTopBorder = true;
            for (int i = 1; i < size - 1; i++) {
                final ItemPartView p = result.get(i);
                int pStartHour = 0;
                int pStartMinute = 0;
                int pDurationInMinute = 24 * 60;
                p.setItemPart(new JCalendarItemPart(item, p.getYear(), p.getDayInYear(), pStartHour, pStartMinute, pDurationInMinute));
            }

            // Last
            final ItemPartView pLast = result.get(size - 1);
            int pLastStartHour = 0;
            int pLastStartMinute = 0;
            int pLastDurationInMinute = item.getDtEnd().get(Calendar.HOUR_OF_DAY) * 60 + item.getDtEnd().get(Calendar.MINUTE) * 60;
            pLast.setItemPart(new JCalendarItemPart(item, pLast.getYear(), pLast.getDayInYear(), pLastStartHour, pLastStartMinute, pLastDurationInMinute));
            pLast.hasBottomBorder = true;
        }
        for (ItemPartView itemPart : result) {
            itemPart.setParts(result);
        }
        return result;
    }

    private void setParts(List<ItemPartView> result) {
        this.parts.clear();
        this.parts.addAll(result);

    }

    public void setColumnIndex(int column) {
        this.column = column;

    }

    public int getColumn() {
        return column;
    }

    public int getX() {
        return this.x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void draw(Graphics2D g2, int offsetX, int offsetY, int lineHeight, int columnWidth) {
        int h = (lineHeight * getPart().getDurationInMinute()) / 60 - 1;
        int w = (getWidth() * columnWidth) / getMaxColumn();
        int colW = columnWidth / getMaxColumn();
        final JCalendarItem item = this.itemPart.getItem();
        final Color color = item.getColor();
        if (isSelected()) {
            g2.setColor(COLOR_HIGHLIGHT_BLUE);
        } else {
            g2.setColor(WeekView.WHITE_TRANSPARENCY_COLOR);
        }
        g2.setStroke(STROKE_1);
        int y0 = (lineHeight * (this.getPart().getStartHour() * 60 + this.getPart().getStartMinute())) / 60;
        final int x1 = offsetX + 2 + getX() * colW;
        final int y1 = y0 + offsetY + 1;
        final int y2 = y0 + offsetY + h;
        this.setRect(x1, y1, w, h);
        g2.fillRect(x1, y1, w - 2, h);
        // Draw icons
        List<Flag> flags = itemPart.getItem().getFlags();
        List<Icon> icons = new ArrayList<Icon>();
        for (Flag flag : flags) {
            if (flag.getIcon() != null) {
                icons.add(flag.getIcon());
            }
        }
        final JPanel c = new JPanel();
        int yIcon = y1 + h;
        for (Icon icon : icons) {
            if (w > icon.getIconWidth()) {

                yIcon -= 1 + icon.getIconHeight();
                if (yIcon > y1) {
                    icon.paintIcon(c, g2, x1 + w - 4 - icon.getIconWidth(), yIcon);
                } else {
                    yIcon += 1 + icon.getIconHeight();
                }
            }
        }

        g2.setColor(color);
        // Right
        final int x2 = x1 + w - 4;
        g2.fillRect(x2, y1, 1, h);
        // Left
        g2.fillRect(x1, y1, 6, h);
        // Top
        if (hasTopBorder) {
            g2.drawLine(x1, y1, x2, y1);
        }

        // Bottom
        if (hasBottomBorder) {
            g2.drawLine(x1, y2, x2, y2);
        }
        // Test
        String str = item.getSummary();
        g2.setColor(Color.BLACK);
        double y = y1;
        int leftMargin = 8;
        if (str != null) {
            g2.setFont(getFontSummary());

            List<String> lines = LayoutUtils.wrap(str, g2.getFontMetrics(), w - leftMargin - 5);
            for (String string : lines) {

                y += g2.getFontMetrics().getHeight();
                if (y > y2) {
                    break;
                }
                g2.drawString(string, x1 + leftMargin, (int) y);

            }
        }
        String str2 = item.getDescription();
        if (item.getLocation() != null) {
            str2 += "\n" + item.getLocation();
        }
        g2.setColor(Color.BLACK);
        if (str2 != null) {
            g2.setFont(getFontDescription());
            List<String> lines = LayoutUtils.wrap(str2, g2.getFontMetrics(), w - leftMargin - 2);
            for (String string : lines) {

                y += g2.getFontMetrics().getHeight();
                if (y > y2) {
                    break;
                }
                g2.drawString(string, x1 + leftMargin, (int) y);

            }
        }

    }

    public int getMaxColumn() {
        return maxColumn;
    }

    public void setMaxColumn(int maxColumn) {
        this.maxColumn = maxColumn;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    private void setRect(int x, int y, int w, int h) {
        Rectangle r = new Rectangle(x, y, w, h);
        this.rectangle = r;
    }

    public boolean contains(int x, int y) {
        if (rectangle == null) {
            return false;
        }
        return this.rectangle.contains(x, y);
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
        for (ItemPartView item : this.parts) {
            if (item != this) {
                if (item.isSelected() != selected) {
                    item.setSelected(selected);
                }
            }
        }
    }

    public int getDayInYear() {
        return this.itemPart.getDayOfYear();
    }

    public int getYear() {
        return this.itemPart.getYear();
    }

    public boolean conflictWith(ItemPartView p) {
        return this.itemPart.conflictWith(p.itemPart);
    }

    public JCalendarItemPart getPart() {
        return this.itemPart;
    }

    public void setItemPart(JCalendarItemPart itemPart) {
        this.itemPart = itemPart;
    }
}
