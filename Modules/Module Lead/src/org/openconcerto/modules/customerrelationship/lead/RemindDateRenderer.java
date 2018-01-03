/*
 * Créé le 14 févr. 2012
 */
package org.openconcerto.modules.customerrelationship.lead;

import java.awt.Color;
import java.awt.Component;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import org.openconcerto.ui.table.TableCellRendererUtils;

public class RemindDateRenderer extends DefaultTableCellRenderer {

    private final Color couleurOrange = new Color(253, 173, 53);
    DateFormat dateFormat = new SimpleDateFormat("dd/MM/yy");
    Date toDay = Calendar.getInstance().getTime();

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        final Component res = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        TableCellRendererUtils.setColors(res, table, isSelected);
        if (value != null) {

            final Date date = (Date) value;
            String t = dateFormat.format(date);
            ((JLabel) res).setText(t);
            if (toDay.after(date)) {
                ((JLabel) res).setBackground(couleurOrange);
            }
        }
        return res;
    }

}
