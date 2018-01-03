package org.jopencalendar.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;

import org.jopencalendar.model.JCalendarItem;
import org.jopencalendar.model.JCalendarItemPart;

public abstract class MultipleDayView extends JPanel implements Scrollable {
    public static final String CALENDARD_ITEMS_PROPERTY = "calendard_items_changed";
    public final static Color WHITE_TRANSPARENCY_COLOR = new Color(255, 255, 255, 180);
    public static final Color LIGHT_BLUE = new Color(202, 212, 220);
    public final static Color LIGHT = new Color(222, 222, 222);
    public static final int TITLE_HEIGHT = 20;
    public static final int HOURS_LABEL_WIDTH = 50;

    protected static final int YEAR_HEIGHT = 20;
    private int deltaY = 10;
    private int rowHeight = 44;

    private List<List<AllDayItem>> allDayItems = new ArrayList<List<AllDayItem>>();
    private List<List<ItemPartView>> itemParts = new ArrayList<List<ItemPartView>>();

    private int allDayItemsRowCount;
    public int allDayItemsRowHeight = 20;

    private Set<ItemPartView> selectedItems = new HashSet<ItemPartView>();
    // Right click menu
    private JPopupMenuProvider popupProvider;

    protected List<ItemPartHoverListener> hListeners = new ArrayList<ItemPartHoverListener>();
    private int startHour = 0;
    private int endHour = 24;

    public MultipleDayView() {

        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                ItemPartView newSelection = getItemPartAt(e.getX(), e.getY());
                if (newSelection != null) {
                    boolean isCurrentlySelected = newSelection.isSelected();
                    // Click on a non selected item
                    boolean ctrlPressed = ((e.getModifiers() & InputEvent.CTRL_MASK) == InputEvent.CTRL_MASK);
                    if (!ctrlPressed) {
                        deselectAll();
                    }
                    if (e.getButton() == MouseEvent.BUTTON1) {
                        if (isCurrentlySelected) {
                            // Click on a selected item
                            newSelection.setSelected(false);
                            selectedItems.remove(newSelection);
                        } else {
                            newSelection.setSelected(true);
                            selectedItems.add(newSelection);
                        }
                    } else {
                        newSelection.setSelected(true);
                        selectedItems.add(newSelection);
                    }
                    repaint();
                }
                // Deselect all if nothing is selected
                if (newSelection == null && !selectedItems.isEmpty()) {
                    deselectAll();
                    repaint();
                }
                // For mac
                showPopup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                showPopup(e);
            }

            private void showPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    ItemPartView newSelection = getItemPartAt(e.getX(), e.getY());
                    if (newSelection != null && !newSelection.isSelected()) {
                        newSelection.setSelected(true);
                        selectedItems.add(newSelection);
                        repaint();
                    }

                    if (popupProvider != null) {
                        // Selected parts
                        final List<JCalendarItemPart> sParts = new ArrayList<JCalendarItemPart>();
                        for (ItemPartView p : selectedItems) {
                            sParts.add(p.getPart());
                        }
                        // Current column parts
                        final List<JCalendarItemPart> currentColumnParts = new ArrayList<JCalendarItemPart>();
                        int columnIndex = getColumnIndex(e.getX());
                        List<ItemPartView> l = MultipleDayView.this.itemParts.get(columnIndex);
                        for (ItemPartView p : l) {
                            currentColumnParts.add(p.getPart());
                        }

                        JPopupMenu popup = popupProvider.getPopup(sParts, currentColumnParts);
                        if (popup != null) {
                            popup.show(MultipleDayView.this, e.getX(), e.getY());
                        }
                    }
                }
            }
        });
        this.addMouseMotionListener(new MouseAdapter() {
            ItemPartView previous;

            @Override
            public void mouseMoved(MouseEvent e) {
                if (hListeners.isEmpty()) {
                    return;
                }
                ItemPartView newSelection = getItemPartAt(e.getX(), e.getY());

                if (newSelection != previous) {

                    for (ItemPartHoverListener l : hListeners) {
                        if (newSelection == null) {
                            l.mouseOn(null);
                        } else {
                            l.mouseOn(newSelection.getPart());
                        }
                    }
                    previous = newSelection;
                }

            }

        });

    }

    protected int getColumnIndex(int currentX) {
        int x = HOURS_LABEL_WIDTH;
        int columnCount = getColumnCount();
        for (int i = 0; i < columnCount - 1; i++) {
            x += getColumnWidth(i);
            if (currentX < x) {
                return i;
            }
        }
        return 0;
    }

    public void setHourRange(int start, int end) {
        if (start < 0 || start > 24) {
            throw new IllegalArgumentException("Bad start hour : " + start);
        }
        if (end < 0 || end > 24) {
            throw new IllegalArgumentException("Bad end hour : " + end);
        }
        if (end - start < 1) {
            throw new IllegalArgumentException("Bad end hour must be > (start + 1)");
        }
        this.startHour = start;
        this.endHour = end;
        repaint();
    }

    protected void setItems(List<List<JCalendarItem>> list) {
        assert SwingUtilities.isEventDispatchThread();
        this.itemParts.clear();
        this.allDayItems.clear();
        for (List<JCalendarItem> l : list) {
            final List<AllDayItem> aDayItems = new ArrayList<AllDayItem>();
            final List<ItemPartView> aItems = new ArrayList<ItemPartView>();
            for (JCalendarItem jCalendarItem : l) {
                if (jCalendarItem.getDtStart().before(jCalendarItem.getDtEnd())) {
                    if (jCalendarItem.isDayOnly()) {
                        aDayItems.add(new AllDayItem(jCalendarItem));
                    } else {
                        aItems.addAll(ItemPartView.split(jCalendarItem));
                    }
                } else {
                    System.err.println(
                            "MultipleDayView.setItems() " + jCalendarItem.getSummary() + " start :" + jCalendarItem.getDtStart().getTime() + " after " + jCalendarItem.getDtEnd().getTime() + " !");
                }
            }
            this.allDayItems.add(aDayItems);
            this.itemParts.add(aItems);
        }

        layoutAllDayItems();
        layoutItemParts();
        // Tweak to force header to be relayouted and repainted
        final Container parent = getParent().getParent();
        final int w = parent.getSize().width;
        final int h = parent.getSize().height;
        parent.setSize(w + 1, h);
        parent.validate();
        parent.setSize(w, h);
        parent.validate();
        firePropertyChange(CALENDARD_ITEMS_PROPERTY, null, list);
    }

    private void layoutItemParts() {
        ItemPartViewLayouter layouter = new ItemPartViewLayouter();
        for (List<ItemPartView> items : itemParts) {
            layouter.layout(items);
        }

    }

    private void layoutAllDayItems() {
        // final Calendar c = getFirstDayCalendar();
        // Collections.sort(this.allDayItems, new Comparator<AllDayItem>() {
        //
        // @Override
        // public int compare(AllDayItem o1, AllDayItem o2) {
        // return o1.getLengthFrom(c) - o2.getLengthFrom(c);
        // }
        // });
        // final int size = this.allDayItems.size();
        // for (int i = 0; i < size; i++) {
        // AllDayItem jCalendarItem = this.allDayItems.get(i);
        // // set X and W
        // Calendar start = jCalendarItem.getItem().getDtStart();
        // Calendar end = jCalendarItem.getItem().getDtEnd();
        // if (start.before(getFirstDayCalendar())) {
        // start = getFirstDayCalendar();
        // } else {
        // jCalendarItem.setLeftBorder(true);
        // }
        //
        // if (end.after(getAfterLastDayCalendar())) {
        // end = getAfterLastDayCalendar();
        // end.add(Calendar.MILLISECOND, -1);
        // } else {
        // jCalendarItem.setRightBorder(true);
        // }
        // int x = (int) (start.getTimeInMillis() - getFirstDayCalendar().getTimeInMillis());
        // x = x / MILLIS_PER_DAY;
        // jCalendarItem.setX(x);
        // int l = ((int) (end.getTimeInMillis() - start.getTimeInMillis())) / MILLIS_PER_DAY + 1;
        // jCalendarItem.setW(l);
        //
        // // Y and H
        // jCalendarItem.setY(0);
        // jCalendarItem.setH(1);
        // for (int j = 0; j < i; j++) {
        // AllDayItem layoutedItem = this.allDayItems.get(j);
        // if (layoutedItem.conflictWith(jCalendarItem)) {
        // jCalendarItem.setY(layoutedItem.getY() + 1);
        // }
        // }
        // }
        // allDayItemsRowCount = 0;
        // for (int i = 0; i < size; i++) {
        // AllDayItem jCalendarItem = this.allDayItems.get(i);
        // if (jCalendarItem.getY() + jCalendarItem.getH() > allDayItemsRowCount) {
        // allDayItemsRowCount = jCalendarItem.getY() + jCalendarItem.getH();
        // }
        // }
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(800, TITLE_HEIGHT + (this.endHour - this.startHour) * rowHeight);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        float[] dash4 = { 8f, 4f };

        BasicStroke bs4 = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 1.0f, dash4, 1f);

        final BasicStroke s = new BasicStroke(1f);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, this.getWidth(), this.getHeight());
        int columnCount = getColumnCount();
        if (columnCount < 1)
            return;

        g2.setStroke(s);
        // H lines : Hours
        int y = deltaY;
        int hourPaintedCount = this.endHour - this.startHour;
        for (int i = 0; i < hourPaintedCount; i++) {
            g.setColor(Color.LIGHT_GRAY);
            g.drawLine(HOURS_LABEL_WIDTH, y, this.getWidth(), y);
            y += rowHeight;
        }
        // H lines : 1/2 Hours
        y = deltaY + rowHeight / 2;
        if (rowHeight > 44) {
            g2.setStroke(s);
        } else {
            g2.setStroke(bs4);
        }
        g.setColor(LIGHT);
        for (int i = 0; i < hourPaintedCount; i++) {
            g.drawLine(HOURS_LABEL_WIDTH, y, this.getWidth(), y);
            y += rowHeight;
        }

        if (rowHeight > 44) {
            g2.setStroke(bs4);
            y = deltaY + rowHeight / 4;
            for (int i = 0; i < hourPaintedCount * 2; i++) {
                g.drawLine(HOURS_LABEL_WIDTH, y, this.getWidth(), y);
                y += rowHeight / 2;
            }
        }

        // Horizontal lines : 5 minutes
        g.setColor(Color.LIGHT_GRAY);
        if (rowHeight > 120) {
            g2.setStroke(s);
            final float h5 = rowHeight / 12f;

            // int w = this.getWidth();
            int x = HOURS_LABEL_WIDTH;
            for (int j = 0; j < columnCount - 1; j++) {
                x += getColumnWidth(j);
                y = deltaY + (int) h5;
                int x2 = x + 10;
                for (int i = 0; i < 24; i++) {
                    float y1 = y;
                    g.drawLine(x, (int) y1, x2, (int) y1);
                    y1 += h5;
                    g.drawLine(x, (int) y1, x2, (int) y1);
                    y1 += 2 * h5;
                    g.drawLine(x, (int) y1, x2, (int) y1);
                    y1 += h5;
                    g.drawLine(x, (int) y1, x2, (int) y1);
                    y += rowHeight / 2;
                }
            }
        }

        // Hours : text on left
        g2.setStroke(s);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(getFont().deriveFont(14f));
        y = deltaY + 6;
        g.setColor(Color.GRAY);
        for (int i = this.startHour; i < this.endHour; i++) {
            g.drawString(getHourLabel(i), 5, y);
            y += rowHeight;
        }
        // Vertical lines
        int x = HOURS_LABEL_WIDTH;

        for (int i = 0; i < columnCount - 1; i++) {
            x += getColumnWidth(i);
            g.drawLine(x, 0, x, this.getHeight());
        }
        int dY = deltaY - this.startHour * rowHeight;
        for (int i = 0; i < this.itemParts.size(); i++) {
            List<ItemPartView> items = this.itemParts.get(i);
            final int columnWidth = getColumnWidth(i);
            // Draw standard items
            for (ItemPartView jCalendarItem : items) {
                final int itemX = getColumnX(i);
                jCalendarItem.draw(g2, itemX, dY, rowHeight, columnWidth);
            }
        }

    }

    String getHourLabel(int i) {
        if (i < 10) {
            return "0" + i + ":00";
        }
        return String.valueOf(i) + ":00";
    }

    public int getColumnWidth(int column) {
        final int c = (this.getWidth() - HOURS_LABEL_WIDTH) / getColumnCount();

        return c;
    }

    public int getColumnX(int column) {
        int x = HOURS_LABEL_WIDTH;
        for (int i = 0; i < column; i++) {
            x += getColumnWidth(i);

        }
        return x;
    }

    public void paintHeader(Graphics g) {

        g.setColor(Color.WHITE);
        g.fillRect(0, 0, this.getWidth() + 100, getHeaderHeight());
        g.setColor(Color.GRAY);
        // Vertical lines
        int columnCount = getColumnCount();
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
            r = g.getFontMetrics().getStringBounds(str, g).getBounds();
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

    public String getTitle() {
        return "Title";
    }

    public void paintHeaderAlldays(Graphics g) {
        // Draw alldays
        // for (AllDayItem jCalendarItem : this.allDayItems) {
        // int w = 0;
        // for (int i = jCalendarItem.getX(); i < jCalendarItem.getX() + jCalendarItem.getW(); i++)
        // {
        // w += getColumnWidth(i);
        // }
        // int x2 = getColumnX(jCalendarItem.getX());
        // jCalendarItem.draw(g2, x2, YEAR_HEIGHT + TITLE_HEIGHT, allDayItemsRowHeight, w);
        // }
    }

    public int getHeaderHeight() {
        return YEAR_HEIGHT + TITLE_HEIGHT + this.allDayItemsRowCount * this.allDayItemsRowHeight + 4;
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return new Dimension(1024, 768);
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return orientation * this.rowHeight * 2;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return this.rowHeight;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }

    public void mouseWheelMoved(MouseWheelEvent e, MouseWheelListener[] l) {

        if (e.getModifiers() == InputEvent.CTRL_MASK) {

            if (e.getWheelRotation() < 0) {
                zoom(22);
            } else {
                zoom(-22);
            }

            e.consume();
        } else {
            for (int i = 0; i < l.length; i++) {
                MouseWheelListener mouseWheelListener = l[i];
                mouseWheelListener.mouseWheelMoved(e);
            }
        }

    }

    public void setZoom(int zIndex) {
        if (zIndex >= 0) {
            int rh = 22 + zIndex * 22;

            this.rowHeight = rh;
            // update size
            final int w = this.getSize().width;
            final int h = getPreferredSize().height;
            setSize(new Dimension(w, h));
            validate();
            repaint();
        }
    }

    private void zoom(int v) {
        if (rowHeight < 200 && rowHeight > 60) {
            rowHeight += v;
            // update size
            final int w = this.getSize().width;
            final int h = getPreferredSize().height;
            setSize(new Dimension(w, h));
            validate();
            repaint();
        }
    }

    public int getRowHeight() {
        return this.rowHeight;
    }

    public void setPopupMenuProvider(JPopupMenuProvider popupProvider) {
        this.popupProvider = popupProvider;
    }

    public Set<ItemPartView> getSelectedItems() {
        return selectedItems;
    }

    public ItemPartView getItemPartAt(int x, int y) {
        ItemPartView newSelection = null;
        for (List<ItemPartView> l : itemParts) {
            for (ItemPartView p : l) {
                if (p.contains(x, y)) {
                    newSelection = p;
                    break;
                }
            }
        }
        return newSelection;
    }

    public void addItemPartHoverListener(ItemPartHoverListener l) {
        this.hListeners.add(l);
    }

    public void removeItemPartHoverListener(ItemPartHoverListener l) {
        this.hListeners.remove(l);
    }

    abstract public String getColumnTitle(int index);

    abstract public int getColumnCount();

    abstract public void reload();

    public void deselectAll() {
        for (ItemPartView p : selectedItems) {
            p.setSelected(false);
        }
        selectedItems.clear();

    }
}
