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
 
 package org.openconcerto.erp.core.supplychain.supplier.ui;

import org.openconcerto.utils.GestionDevise;

import java.awt.Color;
import java.awt.Component;
import java.math.BigInteger;
import java.text.DateFormat;
import java.util.Date;

import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

public class ListEcheanceFournRenderer extends DefaultTableCellRenderer {

    private final static Color couleurEcheance = new Color(255, 128, 64);
    private final DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM);

    public ListEcheanceFournRenderer() {
        super();
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        if (!isSelected) {

            if (value instanceof Date) {
                if (!((Date) value).after(new Date())) {
                    setForeground(couleurEcheance);
                } else {
                    setForeground(Color.BLACK);
                }
            }
        }

        if (value instanceof Date) {
            this.setText(dateFormat.format((Date) value));
        }

        if (value != null && (table.getColumnClass(column) == Long.class || table.getColumnClass(column) == BigInteger.class) && value.getClass() == Long.class) {
            this.setText(GestionDevise.currencyToString(((Long) value).longValue()));
        }
        this.setHorizontalAlignment(SwingConstants.RIGHT);
        if (table.getColumnClass(column) == String.class) {
            this.setHorizontalAlignment(SwingConstants.LEFT);
        }
        return this;
    }
}
