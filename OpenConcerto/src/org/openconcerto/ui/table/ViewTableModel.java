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
 
 package org.openconcerto.ui.table;

import org.openconcerto.utils.TableModelAdapter;

import javax.swing.JTable;

/**
 * A tableModel whose columns are accessed by view indexes.
 * 
 * @author ILM Informatique
 */
public class ViewTableModel extends TableModelAdapter {

    private final JTable table;

    public ViewTableModel(final JTable table) {
        super(table.getModel());
        this.table = table;
    }

    @Override
    protected int adaptCol(final int columnIndex) {
        return this.table.convertColumnIndexToModel(columnIndex);
    }

    @Override
    public int getColumnCount() {
        return this.table.getColumnCount();
    }

    @Override
    protected int adaptRow(int rowIndex) {
        return this.table.convertRowIndexToModel(rowIndex);
    }

    @Override
    public int getRowCount() {
        return this.table.getRowCount();
    }

    // the 4 methods above should be enough, but since JTable also defines the following ones, use
    // them in case they are overloaded.

    @Override
    public String getColumnName(final int column) {
        return this.table.getColumnName(column);
    }

    @Override
    public Class<?> getColumnClass(final int column) {
        return this.table.getColumnClass(column);
    }

    @Override
    public Object getValueAt(final int row, final int column) {
        return this.table.getValueAt(row, column);
    }

    @Override
    public void setValueAt(final Object aValue, final int row, final int column) {
        this.table.setValueAt(aValue, row, column);
    }

    @Override
    public boolean isCellEditable(final int row, final int column) {
        return this.table.isCellEditable(row, column);
    }
}
