package org.jopencalendar.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.Calendar;

import org.jopencalendar.model.JCalendarItem;

public class AllDayItem {
    private JCalendarItem item;
    private int x, y, w, h;
    private boolean hasLeftBorder;
    private boolean hasRightBorder;

    public AllDayItem(JCalendarItem item) {
        this.item = item;
    }

    public final JCalendarItem getItem() {
        return item;
    }

    public final int getX() {
        return x;
    }

    public final void setX(int x) {
        this.x = x;
    }

    public final int getY() {
        return y;
    }

    public final void setY(int y) {
        this.y = y;
    }

    public final int getW() {
        return w;
    }

    public final void setW(int w) {
        this.w = w;
    }

    public final int getH() {
        return h;
    }

    public final void setH(int h) {
        this.h = h;
    }

    public final boolean hasLeftBorder() {
        return hasLeftBorder;
    }

    public final void setLeftBorder(boolean hasLeftBorder) {
        this.hasLeftBorder = hasLeftBorder;
    }

    public final boolean hasRightBorder() {
        return hasRightBorder;
    }

    public final void setRightBorder(boolean hasRightBorder) {
        this.hasRightBorder = hasRightBorder;
    }

    public void draw(Graphics2D g2, int x2, int offsetY, int lineHeight, int width) {
        final Color c = this.item.getColor();
        g2.setStroke(new BasicStroke(1f));
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(WeekView.WHITE_TRANSPARENCY_COLOR);
        // int x2 = offsetX + x * columnWidth;
        final int y2 = offsetY + y * (lineHeight) + 2;

        if (hasLeftBorder) {
            width -= 4;
            x2 += 4;
        }
        if (hasRightBorder) {
            width -= 4;
        }
        g2.fillRect(x2, y2, width, h * lineHeight - 2);
        g2.setColor(Color.BLACK);
        g2.drawString(this.item.getSummary(), x2 + 4, y2 + g2.getFont().getSize());
        // Borders top & bottom
        g2.setColor(c);
        g2.drawLine(x2, y2, x2 + width, y2);
        g2.drawLine(x2, y2 + h * lineHeight - 3, x2 + width, y2 + h * lineHeight - 3);

        g2.setStroke(new BasicStroke(5f));
        if (hasLeftBorder) {
            g2.drawLine(x2, y2 + 2, x2, y2 + h * lineHeight - 5);
        }
        if (hasRightBorder) {
            g2.drawLine(x2 + width, y2 + 2, x2 + width, y2 + h * lineHeight - 5);
        }
    }

    public int getLengthFrom(Calendar c) {
        // TODO Auto-generated method stub
        return 0;
    }

    public boolean conflictWith(AllDayItem item) {
        if (this.getItem().getDtEnd().before(item.getItem().getDtStart())) {
            return false;
        }
        if (item.getItem().getDtEnd().before(this.getItem().getDtStart())) {
            return false;
        }

        return true;
    }
}
