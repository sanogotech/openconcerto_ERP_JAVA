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

import org.openconcerto.ui.Log;
import org.openconcerto.utils.Tuple2;

import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JTable;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

/**
 * Allow to maintain columns' width so that the widest cell fits. Maintain meaning that after a
 * table change event or even after a table model change the column will be automatically resized.
 * 
 * @author Sylvain CUAZ
 */
public class ColumnSizeAdjustor {

    private final JTable table;
    private final PropertyChangeListener propL;
    // keep a reference to be able to remove our listener
    private TableColumnModel columnModel;
    private final TableColumnModelListener columnL;
    // keep a reference to be able to remove our listener
    private TableModel tableModel;
    private final TableModelListener tableL;
    private int origAutoResizeMode;
    private final PropertyChangeListener autoResizeL;
    private final List<Integer> maxWidths;

    private boolean installed;

    public ColumnSizeAdjustor(final JTable table) {
        this.table = table;

        this.autoResizeL = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals("autoResizeMode")) {
                    Log.get().warning(evt.getPropertyName() + " changed so uninstalling " + ColumnSizeAdjustor.this);
                    uninstall();
                } else if (evt.getPropertyName().equals("model")) {
                    removeTableModelListener();
                    addTableModelListener();
                    // the columns should be recreated by the JTable so no need to call
                    // packColumns() since columnL will do it for each column
                }
            }
        };
        this.propL = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                removeColModelListener();
                addColModelListener();
            }
        };
        this.columnL = new TableColumnModelAdapter() {
            @Override
            public void columnRemoved(TableColumnModelEvent e) {
                // nothing to do, if the column is later added again, columnAdded() will discard the
                // current width.
            }

            @Override
            public void columnAdded(TableColumnModelEvent e) {
                final TableColumnModel model = (TableColumnModel) e.getSource();
                packColumn(model.getColumn(e.getToIndex()).getModelIndex(), TableModelEvent.UPDATE, 0, -1);
            }
        };
        this.tableL = new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                if (e == null || e.getFirstRow() == TableModelEvent.HEADER_ROW) {
                    // structure change, the columns and the data model can be out of sync since the
                    // columns are also created by e
                    // the columns have changed so discard all current states (new columns will be
                    // handled by columnAdded())
                    ColumnSizeAdjustor.this.maxWidths.clear();
                } else {
                    // only row change
                    final boolean allCols = e.getColumn() == TableModelEvent.ALL_COLUMNS;
                    final int firstCol = allCols ? 0 : e.getColumn();
                    // +1 since we want lastCol exclusive
                    final int lastCol = allCols ? ((TableModel) e.getSource()).getColumnCount() : e.getColumn() + 1;
                    for (int i = firstCol; i < lastCol; i++) {
                        packColumn(i, e.getType(), e.getFirstRow(), e.getLastRow());
                    }
                }
            }
        };
        this.maxWidths = new ArrayList<Integer>();
        this.installed = false;
        this.tableModel = null;
        this.install();
    }

    public final boolean isInstalled() {
        return this.installed;
    }

    public void setInstalled(boolean b) {
        if (b != this.installed) {
            if (b) {
                // Disable auto resizing
                this.origAutoResizeMode = this.table.getAutoResizeMode();
                this.table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
                this.table.addPropertyChangeListener(this.autoResizeL);
                this.table.addPropertyChangeListener("columnModel", this.propL);
                addColModelListener();
                addTableModelListener();
                this.packColumns();
            } else {
                removeTableModelListener();
                this.table.removePropertyChangeListener(this.autoResizeL);
                if (this.table.getAutoResizeMode() == JTable.AUTO_RESIZE_OFF)
                    this.table.setAutoResizeMode(this.origAutoResizeMode);
                this.table.removePropertyChangeListener("columnModel", this.propL);
                removeColModelListener();
            }
            this.installed = b;
        }
    }

    private void addTableModelListener() {
        this.removeTableModelListener();
        // start with a clean state
        assert this.tableModel == null && this.maxWidths.isEmpty();
        this.tableModel = this.table.getModel();
        this.tableModel.addTableModelListener(this.tableL);
    }

    private void removeTableModelListener() {
        if (this.tableModel != null) {
            this.tableModel.removeTableModelListener(this.tableL);
            this.tableModel = null;
            this.maxWidths.clear();
        }
    }

    private void addColModelListener() {
        this.removeColModelListener();
        // start with a clean state
        assert this.columnModel == null && this.maxWidths.isEmpty();
        this.columnModel = this.table.getColumnModel();
        this.columnModel.addColumnModelListener(this.columnL);
    }

    private void removeColModelListener() {
        if (this.columnModel != null) {
            this.columnModel.removeColumnModelListener(this.columnL);
            this.columnModel = null;
            this.maxWidths.clear();
        }
    }

    public void install() {
        this.setInstalled(true);
    }

    public void uninstall() {
        this.setInstalled(false);
    }

    public void packColumns() {
        final int columnCount = this.tableModel.getColumnCount();
        this.maxWidths.clear();
        this.maxWidths.addAll(Collections.<Integer> nCopies(columnCount, null));
        for (int c = 0; c < columnCount; c++) {
            packColumn(c, TableModelEvent.UPDATE, 0, -1);
        }
    }

    private final void packColumn(int colModelIndex, final int type, final int firstModelRow, final int lastModelRow) {
        // MAYBE compute anyway if rowCount is low, or keep the 100 largest cells per column to
        // shrink
        if (type == TableModelEvent.DELETE)
            return;

        final int missing = colModelIndex - this.maxWidths.size() + 1;
        if (missing > 0) {
            this.maxWidths.addAll(Collections.<Integer> nCopies(missing, null));
        }
        final int initialWidth = this.maxWidths.get(colModelIndex) == null ? 0 : this.maxWidths.get(colModelIndex).intValue();
        final Tuple2<TableColumn, Integer> colAndWidth = computeMaxWidth(colModelIndex, initialWidth, firstModelRow, lastModelRow);
        final Integer width = colAndWidth.get1();
        this.maxWidths.set(colModelIndex, width);
        // Set the width
        if (width != null) {
            final int margin = 2;
            colAndWidth.get0().setPreferredWidth(width + 2 * margin);
        }
    }

    /**
     * Compute the max width for all cells of column <code>colModelIndex</code> from row
     * <code>firstModelRow</code> inclusive to row <code>last</code> inclusive.
     * 
     * @param colModelIndex the column index.
     * @param initialWidth the initial value to give to the result, ie useful when inserting new
     *        rows.
     * @param firstModelRow the first line to compute.
     * @param last the last inclusive line to compute.
     * @return the column and its width, both <code>null</code> if <code>colModelIndex</code> is not
     *         displayed.
     */
    private final Tuple2<TableColumn, Integer> computeMaxWidth(final int colModelIndex, final int initialWidth, final int firstModelRow, final int last) {
        final int viewIndex = this.table.convertColumnIndexToView(colModelIndex);
        if (viewIndex < 0)
            // not displayed
            return new Tuple2<TableColumn, Integer>(null, null);
        final TableColumn col = this.columnModel.getColumn(viewIndex);

        final int rowCount = this.table.getRowCount();
        // TableModelEvent use Integer.MAX_VALUE
        // +1 since last is inclusive and we want lastModelRow to be exclusive
        final int lastModelRow = last < 0 || last >= rowCount ? rowCount : last + 1;

        // don't need initialWidth if we're computing the whole column
        int width = firstModelRow == 0 && lastModelRow == rowCount ? 0 : initialWidth;
        // Get width of column header
        TableCellRenderer renderer = col.getHeaderRenderer();
        if (renderer == null) {
            renderer = this.table.getTableHeader().getDefaultRenderer();
        }
        Component comp = renderer.getTableCellRendererComponent(this.table, col.getHeaderValue(), false, false, 0, 0);
        width = Math.max(width, comp.getPreferredSize().width);

        // Get maximum width of column data
        // <= since last is inclusive
        for (int r = firstModelRow; r < lastModelRow; r++) {
            final int viewRow = this.table.convertRowIndexToView(r);
            renderer = this.table.getCellRenderer(viewRow, viewIndex);
            comp = renderer.getTableCellRendererComponent(this.table, this.table.getModel().getValueAt(r, colModelIndex), false, false, viewRow, viewIndex);
            width = Math.max(width, comp.getPreferredSize().width);
        }

        return Tuple2.create(col, width);
    }
}
