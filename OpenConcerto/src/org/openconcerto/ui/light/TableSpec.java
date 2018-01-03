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

import org.openconcerto.utils.io.JSONConverter;
import org.openconcerto.utils.io.Transferable;

import net.minidev.json.JSONObject;

public class TableSpec implements Transferable {
    private String id;
    private ColumnsSpec columns;
    private TableContent content;
    private RowSelectionSpec selection;
    private SearchSpec search;
    private Boolean variableColumnsCount = false;

    public TableSpec(final String tableId, final RowSelectionSpec selection, final ColumnsSpec columns) {
        this.id = tableId + ".spec";
        if (selection == null) {
            throw new IllegalArgumentException("null RowSelectionSpec");
        }
        if (columns == null) {
            throw new IllegalArgumentException("null ColumnsSpec");
        }
        this.selection = selection;
        this.columns = columns;
        this.content = new TableContent(tableId);
    }

    public TableSpec(final JSONObject json) {
        this.fromJSON(json);
    }

    public String getId() {
        return this.id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public ColumnsSpec getColumns() {
        return this.columns;
    }

    public void setColumns(final ColumnsSpec columns) {
        this.columns = columns;
    }

    public TableContent getContent() {
        return this.content;
    }

    public void setContent(final TableContent content) {
        this.content = content;
    }

    public RowSelectionSpec getSelection() {
        return this.selection;
    }

    public void setSelection(final RowSelectionSpec selection) {
        this.selection = selection;
    }

    public SearchSpec getSearch() {
        return this.search;
    }

    public void setSearch(final SearchSpec search) {
        this.search = search;
    }

    public Boolean isVariableColumnsCount() {
        return this.variableColumnsCount;
    }

    public void setVariableColumnsCount(final Boolean variableColumnsCount) {
        this.variableColumnsCount = variableColumnsCount;
    }

    @Override
    public JSONObject toJSON() {
        final JSONObject result = new JSONObject();
        result.put("class", "TableSpec");
        result.put("id", this.id);
        if (this.columns != null) {
            result.put("columns", JSONConverter.getJSON(this.columns));
        }
        if (this.content != null) {
            result.put("content", JSONConverter.getJSON(this.content));
        }
        if (this.selection != null) {
            result.put("selection", JSONConverter.getJSON(this.selection));
        }
        if (this.search != null) {
            result.put("search", JSONConverter.getJSON(this.search));
        }
        if (this.variableColumnsCount) {
            result.put("variable-columns-count", JSONConverter.getJSON(true));
        }

        return result;
    }

    @Override
    public void fromJSON(final JSONObject json) {
        this.id = (String) JSONConverter.getParameterFromJSON(json, "id", String.class);

        final JSONObject jsonColumns = (JSONObject) JSONConverter.getParameterFromJSON(json, "columns", JSONObject.class);
        if (jsonColumns != null) {
            this.columns = new ColumnsSpec(jsonColumns);
        }

        final JSONObject jsonContent = (JSONObject) JSONConverter.getParameterFromJSON(json, "content", JSONObject.class);
        if (jsonContent != null) {
            this.content = new TableContent(jsonContent);
        }
        final JSONObject jsonSelection = (JSONObject) JSONConverter.getParameterFromJSON(json, "selection", JSONObject.class);
        if (jsonSelection != null) {
            this.selection = new RowSelectionSpec(jsonSelection);
        } else {
            throw new IllegalArgumentException("null selection");
        }

        final JSONObject jsonSearch = (JSONObject) JSONConverter.getParameterFromJSON(json, "search", JSONObject.class);
        if (jsonSearch != null) {
            this.search = new SearchSpec(jsonSearch);
        }

        this.variableColumnsCount = (Boolean) JSONConverter.getParameterFromJSON(json, "variable-columns-count", Boolean.class);
    }
}
