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
 
 package org.openconcerto.sql.ui.light;

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.view.list.ITableModel;
import org.openconcerto.sql.view.list.ListSQLLine;
import org.openconcerto.sql.view.list.SQLTableModelColumn;
import org.openconcerto.sql.view.list.SQLTableModelSourceOnline;
import org.openconcerto.ui.SwingThreadUtils;
import org.openconcerto.ui.light.ColumnSpec;
import org.openconcerto.ui.light.ColumnsSpec;
import org.openconcerto.ui.light.LightUIElement;
import org.openconcerto.ui.light.LightUITable;
import org.openconcerto.ui.light.Row;
import org.openconcerto.ui.light.SearchSpec;
import org.openconcerto.utils.NumberUtils;
import org.openconcerto.utils.cc.ITransformer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import javax.swing.SwingUtilities;
import javax.swing.event.TableModelListener;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.DOMBuilder;

import net.minidev.json.JSONObject;

public abstract class LightRowValuesTable extends LightUITable {

    public static final int MAX_LINE_TO_SEND = 100;

    private int totalRowCount = -1;

    private int offset = 0;

    private ITableModel model;

    private final ITransformer<SQLSelect, SQLSelect> orginTransformer;

    private List<TableModelListener> tableModelListeners = new ArrayList<TableModelListener>();

    public LightRowValuesTable(final Configuration configuration, final Number userId, final String id, final ITableModel model) {
        super(id);

        this.model = model;
        final ColumnsSpec columnsSpec = this.createColumnsSpecFromModelSource(configuration, userId);

        this.getTableSpec().setColumns(columnsSpec);

        if (model.getReq() instanceof SQLTableModelSourceOnline) {
            final SQLTableModelSourceOnline source = (SQLTableModelSourceOnline) model.getReq();
            this.orginTransformer = source.getReq().getSelectTransf();
        } else {
            this.orginTransformer = null;
        }

        this.setDynamicLoad(true);
    }

    // Clone constructor
    public LightRowValuesTable(final LightRowValuesTable tableElement) {
        super(tableElement);

        this.model = tableElement.model;
        this.totalRowCount = tableElement.totalRowCount;
        this.orginTransformer = tableElement.orginTransformer;
    }

    // Json constructor
    public LightRowValuesTable(final JSONObject json) {
        super(json);
        this.orginTransformer = null;
    }

    public final int getTotalRowsCount() {
        return this.totalRowCount;
    }

    public final int getOffset() {
        return this.offset;
    }

    public final void setOffset(final int offset) {
        this.offset = offset;
    }

    public final void addTableModelListener(final TableModelListener tableModelListener) {
        this.tableModelListeners.add(tableModelListener);
        this.model.addTableModelListener(tableModelListener);
    }

    public final void removeTableModelListener(final TableModelListener tableModelListener) {
        this.tableModelListeners.remove(tableModelListener);
        this.model.removeTableModelListener(tableModelListener);
    }

    public ITableModel getModel() {
        return this.model;
    }

    public final LightListSqlRow getRowFromSqlID(final Number sqlID) {
        if (this.hasRow()) {
            final int size = this.getTableSpec().getContent().getRowsCount();
            for (int i = 0; i < size; i++) {
                final LightListSqlRow row = (LightListSqlRow) this.getRow(i);
                if (NumberUtils.areNumericallyEqual(row.getSqlRow().getIDNumber(), sqlID)) {
                    return row;
                }
            }
        }
        return null;
    }

    public final LightListSqlRow createLightListRowFromListLine(final ListSQLLine listSqlLine, final int index) throws IllegalStateException {
        final ColumnsSpec columnsSpec = this.getTableSpec().getColumns();
        final List<SQLTableModelColumn> sqlColumns = this.getModelColumns();
        final int colSize = sqlColumns.size();

        final LightListSqlRow row = new LightListSqlRow(listSqlLine.getRow(), listSqlLine.getID());
        final List<Object> values = new ArrayList<Object>();
        for (int i = 0; i < colSize; i++) {
            final String columnId = columnsSpec.getColumn(i).getId();
            final SQLTableModelColumn col = getColumnFromId(sqlColumns, columnId);

            if (col != null) {
                Object value = col.show(row.getSqlRow());
                if (col.getLightUIrenderer() != null) {
                    value = col.getLightUIrenderer().getLightUIElement(value, 0, i);
                }
                values.add(value);
            } else {
                throw new IllegalArgumentException("column " + columnId + " is in ColumnsSpec but it is not found in SQLTableModelColumn");
            }
        }
        row.setValues(values);

        return row;
    }

    public final SQLRowAccessor getFirstSelectedSqlRow() {
        final List<Row> selectedRows = this.getSelectedRows();
        if (selectedRows.isEmpty()) {
            return null;
        } else {
            return ((LightListSqlRow) selectedRows.get(0)).getSqlRow();
        }
    }

    private final List<SQLTableModelColumn> getModelColumns() {
        try {
            // TODO: clean swing
            return SwingThreadUtils.call(new Callable<List<SQLTableModelColumn>>() {
                @Override
                public List<SQLTableModelColumn> call() throws Exception {
                    return LightRowValuesTable.this.getModel().getReq().getColumns();
                }
            });
        } catch (final Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private final SQLTableModelColumn getColumnFromId(final List<SQLTableModelColumn> allCols, final String columnId) {
        final int columnSize = allCols.size();
        for (int i = 0; i < columnSize; i++) {
            final SQLTableModelColumn tableModelColumn = allCols.get(i);
            if (tableModelColumn.getIdentifier().equals(columnId)) {
                return tableModelColumn;
            }
        }
        return null;
    }

    /**
     * Get columns user preferences for a specific table
     * 
     * @param configuration - The user SQL configuration
     * @param userId - Id of the user who want view the table
     * 
     * @return the XML which contains user preferences
     * 
     * @throws IllegalArgumentException
     * @throws IllegalStateException
     * 
     */
    // TODO: move in LightUITable, maybe move LightUITable in FrameWork_SQL
    private final Document getColumnsSpecUserPerfs(final Configuration configuration, final Number userId) throws IllegalArgumentException, IllegalStateException {
        Document columnsPrefs = null;
        final DOMBuilder in = new DOMBuilder();
        org.w3c.dom.Document w3cDoc = null;

        w3cDoc = configuration.getXMLConf(userId, this.getId());
        if (w3cDoc != null) {
            columnsPrefs = in.build(w3cDoc);
        }
        return columnsPrefs;
    }

    /**
     * Create ColumnsSpec from list of SQLTableModelColumn and apply user preferences
     * 
     * @param configuration - current SQL configuration of user
     * @param userId - Id of user
     * 
     * @return New ColumnsSpec with user preferences application
     * 
     * @throws IllegalArgumentException
     * @throws IllegalStateException
     */
    private final ColumnsSpec createColumnsSpecFromModelSource(final Configuration configuration, final Number userId) throws IllegalArgumentException, IllegalStateException {
        final List<String> possibleColumnIds = new ArrayList<String>();
        final List<String> sortedIds = new ArrayList<String>();
        final List<ColumnSpec> columnsSpec = new ArrayList<ColumnSpec>();

        final List<SQLTableModelColumn> columns = this.getModelColumns();
        final int columnsCount = columns.size();

        for (int i = 0; i < columnsCount; i++) {
            final SQLTableModelColumn sqlColumn = columns.get(i);
            // TODO : creer la notion d'ID un peu plus dans le l'esprit sales.invoice.amount
            final String columnId = sqlColumn.getIdentifier();

            possibleColumnIds.add(columnId);
            Class<?> valueClass = sqlColumn.getValueClass();
            if (sqlColumn.getLightUIrenderer() != null) {
                valueClass = LightUIElement.class;
            }

            columnsSpec.add(new ColumnSpec(columnId, valueClass, sqlColumn.getName(), null, false, null));
        }

        // TODO : recuperer l'info sauvegardée sur le serveur par user (à coder)
        sortedIds.add(columnsSpec.get(0).getId());

        final ColumnsSpec cSpec = new ColumnsSpec(this.getId(), columnsSpec, possibleColumnIds, sortedIds);
        cSpec.setAllowMove(true);
        cSpec.setAllowResize(true);

        final Document xmlColumnsPref = this.getColumnsSpecUserPerfs(configuration, userId);
        if (!cSpec.setUserPrefs(xmlColumnsPref)) {
            configuration.removeXMLConf(userId, this.getId());
        }

        return cSpec;
    }

    // TODO: merge with OpenConcerto List search system
    public abstract void doSearch(final Configuration configuration, final SearchSpec searchSpec, final int offset);

    @Override
    public Row getRowById(Number rowId) {
        for (int i = 0; i < this.model.getRowCount(); i++) {
            if (NumberUtils.areNumericallyEqual(this.model.getRow(i).getID(), rowId)) {
                return this.createLightListRowFromListLine(this.model.getRow(i), i);
            }
        }
        return super.getRowById(rowId);
    }

    /**
     * Create default columns preferences from the SQLTableModelLinesSourceOnline
     * 
     * @return XML document with columns preferences
     */
    @Override
    public Document createDefaultXmlPreferences() {
        final Element rootElement = new Element("list");
        final List<SQLTableModelColumn> columns = this.getModelColumns();

        final int sqlColumnsCount = columns.size();
        for (int i = 0; i < sqlColumnsCount; i++) {
            final SQLTableModelColumn sqlColumn = columns.get(i);
            final String columnId = sqlColumn.getIdentifier();
            final ColumnSpec columnSpec = new ColumnSpec(columnId, sqlColumn.getValueClass(), sqlColumn.getName(), null, false, null);
            final Element columnElement = this.createXmlColumn(columnId, columnSpec.getMaxWidth(), columnSpec.getMinWidth(), columnSpec.getWidth());
            rootElement.addContent(columnElement);
        }
        final Document xmlConf = new Document(rootElement);

        return xmlConf;
    }

    @Override
    public void destroy() {
        super.destroy();

        // TODO: clean swing
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    final List<TableModelListener> tableModelListeners = LightRowValuesTable.this.tableModelListeners;
                    for (int i = tableModelListeners.size() - 1; i > -1; i--) {
                        LightRowValuesTable.this.removeTableModelListener(tableModelListeners.get(i));
                    }
                }
            });
        } catch (final Exception ex) {
            throw new IllegalStateException(ex);
        }

        this.clearRows();
    }
}
