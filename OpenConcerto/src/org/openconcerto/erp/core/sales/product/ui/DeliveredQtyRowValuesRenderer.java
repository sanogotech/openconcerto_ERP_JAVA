/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2011 OpenConcerto, by ILM Informatique. All rights reserved.
 * 
 * The contents of this file are subject to the terms of the GNU General Public License Version 3
 * only ("GPL"). You may not use this file except in compliance with the License. You can obtain a
 * copy of the License at http://www.gnu.org/licenses/gpl-3.0.html See the License for the specific
 * language governing permissions and limitations under the License.
 * 
 * When distributing the software, include this License Header Notice in each file.
 */
 
 package org.openconcerto.erp.core.sales.product.ui;

import org.openconcerto.erp.core.common.ui.DeviseNiceTableCellRenderer;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.view.list.RowValuesTable;
import org.openconcerto.sql.view.list.RowValuesTableModel;
import org.openconcerto.ui.table.AlternateTableCellRenderer;
import org.openconcerto.utils.CollectionUtils;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;

public class DeliveredQtyRowValuesRenderer extends DeviseNiceTableCellRenderer {

    // Red
    public static final Color red = new Color(255, 31, 52);
    public static final Color redLightGrey = new Color(240, 65, 85);

    // Orange
    public final static Color orange = new Color(243, 125, 75);
    public final static Color orangeGrey = new Color(222, 107, 47);

    // Blue
    public final static Color light = new Color(232, 238, 250);
    public final static Color lightGrey = new Color(211, 220, 222);

    // Black
    public final static Color lightBlack = new Color(192, 192, 192);
    public final static Color lightBlackGrey = new Color(155, 155, 155);

    public DeliveredQtyRowValuesRenderer() {
        AlternateTableCellRenderer.setBGColorMap(this, CollectionUtils.createMap(lightBlack, lightBlackGrey, red, redLightGrey, orange, orangeGrey));
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

        Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        if (table instanceof RowValuesTable) {

            ((JLabel) comp).setHorizontalAlignment(SwingConstants.RIGHT);
            RowValuesTableModel model = ((RowValuesTable) table).getRowValuesTableModel();
            SQLRowValues rowVals = model.getRowValuesAt(row);

            Number qte = (Number) rowVals.getObject("QTE");
            Number qteL = (Number) rowVals.getObject("QTE_LIVREE");
            if (qte != null && qteL != null) {
                if (qte.intValue() < qteL.intValue()) {
                    comp.setBackground(red);
                } else if (qteL.intValue() <= 0) {
                    comp.setBackground(lightBlack);
                } else if (qteL.intValue() != qte.intValue()) {
                    comp.setBackground(orange);
                }
            }
        }
        return comp;
    }
}
