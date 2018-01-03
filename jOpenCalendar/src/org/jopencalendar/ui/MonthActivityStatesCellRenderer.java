package org.jopencalendar.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.util.Calendar;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import org.jopencalendar.model.JCalendarItem;

public class MonthActivityStatesCellRenderer extends DefaultTableCellRenderer {
    private int squareSize;

    MonthActivityStatesCellRenderer(int squareSize) {
        this.squareSize = squareSize;
    }

    @Override
    public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected, boolean hasFocus, final int row, final int column) {
        JComponent c = new JPanel() {
            @Override
            public void paint(Graphics g) {

                MonthActivityStates m = (MonthActivityStates) value;
                if (!isSelected) {
                    g.setColor(Color.WHITE);
                } else {
                    g.setColor(table.getSelectionBackground());
                }
                final int h = getHeight();
                g.fillRect(0, 0, getWidth(), h);
                // Week delimiters
                List<Integer> lIndexes = m.getMondayIndex();

                if (isSelected) {
                    g.setColor(Color.WHITE);
                } else {
                    g.setColor(Color.LIGHT_GRAY);
                }
                // Draw Week number
                if (row % 5 == 0) {
                    for (Integer integer : lIndexes) {
                        Calendar c = Calendar.getInstance();
                        c.clear();
                        c.set(Calendar.YEAR, m.getYear());
                        c.set(Calendar.MONTH, m.getMonth());
                        c.set(Calendar.DAY_OF_MONTH, integer);
                        int w = c.get(Calendar.WEEK_OF_YEAR) + 1;
                        int x = 4 + integer * squareSize;
                        int y = 16;
                        g.drawString(String.valueOf(w), x, y);
                    }
                }
                //
                for (Integer integer : lIndexes) {

                    int x = integer * squareSize;
                    if (x != 0) {
                        g.drawLine(x, 0, x, h / 2 - 2);
                        g.drawLine(x, h / 2 + 4, x, h - 4);
                    }
                }
                // Days
                List<List<JCalendarItem>> l = m.getList();
                if (l != null) {
                    int x = 0;

                    for (List<JCalendarItem> list : l) {
                        if (list != null) {
                            int y = 2;
                            int sh = (h - 1) / list.size();
                            for (JCalendarItem jCalendarItem : list) {
                                g.setColor(jCalendarItem.getColor());
                                g.fillRect(x, y, squareSize - 1, sh);
                                y += sh;
                            }

                        }
                        x += squareSize;
                    }
                }

            }
        };

        return c;
    }
}
