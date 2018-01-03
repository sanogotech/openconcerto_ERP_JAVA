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
 
 package org.openconcerto.ui.light;

import java.util.ArrayList;
import java.util.List;

import org.openconcerto.utils.io.JSONConverter;
import org.openconcerto.utils.io.Transferable;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

public class TableContent implements Transferable {
    private static final long serialVersionUID = 3648381615123520834L;
    private String tableId;
    private List<Row> rows;

    public TableContent() {
        // Serialization
    }

    public TableContent(final String tableId) {
        this.init(tableId, new ArrayList<Row>());
    }

    public TableContent(final String tableId, final List<Row> rows) {
        this.init(tableId, rows);
    }

    public TableContent(final JSONObject json) {
        this.fromJSON(json);
    }

    private final void init(final String tableId, final List<Row> rows) {
        this.tableId = tableId;
        this.rows = rows;
    }

    public final String getTableId() {
        return this.tableId;
    }

    public final void setTableId(final String tableId) {
        this.tableId = tableId;
    }

    public final synchronized Row getRow(final int index) {
        return this.rows.get(index);
    }

    public final synchronized int getRowsCount() {
        return this.rows.size();
    }

    public final synchronized boolean addRow(final Row row) {
        return this.rows.add(row);
    }

    public final synchronized Row setRow(final int index, final Row row) {
        return this.rows.set(index, row);
    }

    public final synchronized Row removeRow(final int index) {
        return this.rows.remove(index);
    }

    public final synchronized boolean removeRow(final Row row) {
        return this.rows.remove(row);
    }

    public final synchronized void setRows(List<Row> rows) {
        this.rows = rows;
    }

    public final synchronized void clearRows() {
        this.rows.clear();
    }
    
    /**
     * @return a copy of the list
     */
    public final synchronized List<Row> getRows(){
        return new ArrayList<Row>(this.rows);
    }

    @Override
    public synchronized String toString() {
        return "TableContent of " + this.tableId + " lines count : " + this.getRowsCount();
    }

    @Override
    public synchronized JSONObject toJSON() {
        final JSONObject result = new JSONObject();
        result.put("class", "TableContent");
        result.put("table-id", this.tableId);
        result.put("rows", JSONConverter.getJSON(this.rows));
        return result;
    }

    @Override
    public synchronized void fromJSON(final JSONObject json) {
        this.tableId = JSONConverter.getParameterFromJSON(json, "table-id", String.class);
        final JSONArray jsonRows = JSONConverter.getParameterFromJSON(json, "rows", JSONArray.class);
        if (jsonRows != null) {
            final List<Row> listRows = new ArrayList<Row>();
            for (final Object o : jsonRows) {
                listRows.add(new Row(JSONConverter.getObjectFromJSON(o, JSONObject.class)));
            }
            this.setRows(listRows);
        }
    }
}
