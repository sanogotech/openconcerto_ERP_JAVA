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

import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.io.JSONConverter;
import org.openconcerto.utils.io.Transferable;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.List;

import org.jdom2.Document;
import org.jdom2.Element;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

public class ColumnsSpec implements Externalizable, Transferable {
    private String id;
    // All the columns that could be displayed
    private List<ColumnSpec> columns = new ArrayList<ColumnSpec>();
    // Ids visible in the table, in the same order of the display
    private List<String> possibleColumnIds = new ArrayList<String>();
    // Ids of the sorted columns
    private List<String> sortedIds = new ArrayList<String>();
    // number of fixed columns, used for ve rtical "split"
    private int fixedColumns;

    private Boolean adaptWidth = false;
    private Boolean allowMove = false;
    private Boolean allowResize = false;

    public ColumnsSpec() {
        // Serialization
    }

    public ColumnsSpec(final JSONObject json) {
        this.fromJSON(json);
    }

    public ColumnsSpec(final String id, final List<ColumnSpec> columns, final List<String> possibleColumnIds, final List<String> sortedIds) throws IllegalArgumentException {
        // Id checks
        if (id == null) {
            throw new IllegalArgumentException("null id");
        }
        this.id = id;

        // Columns checks
        if (columns == null) {
            throw new IllegalArgumentException("null columns");
        }
        this.columns = columns;

        // Possible checks
        if (possibleColumnIds == null) {
            throw new IllegalArgumentException("null possible column ids");
        }
        this.possibleColumnIds = possibleColumnIds;

        // Sort assign
        this.sortedIds = sortedIds;

    }

    public final String getId() {
        return this.id;
    }

    public final List<String> getPossibleColumnIds() {
        return this.possibleColumnIds;
    }

    public final List<String> getSortedIds() {
        return this.sortedIds;
    }

    public final int getFixedColumns() {
        return this.fixedColumns;

    }

    public final int getColumnCount() {
        return this.columns.size();

    }

    public final ColumnSpec getColumn(int i) {
        return this.columns.get(i);
    }

    public final ColumnSpec setColumn(int i, final ColumnSpec column) {
        return this.columns.set(i, column);
    }

    public final Boolean isAdaptWidth() {
        return this.adaptWidth;
    }

    public final void setAdaptWidth(final boolean adaptWidth) {
        this.adaptWidth = adaptWidth;
    }

    public final Boolean isAllowMove() {
        return this.allowMove;
    }

    public final void setAllowMove(final boolean allowMove) {
        this.allowMove = allowMove;
    }

    public final Boolean isAllowResize() {
        return this.allowResize;
    }

    public final void setAllowResize(final boolean allowResize) {
        this.allowResize = allowResize;
    }

    public List<String> getColumnsIds() {
        final ArrayList<String> result = new ArrayList<String>(this.columns.size());
        for (ColumnSpec c : this.columns) {
            result.add(c.getId());
        }
        return result;
    }

    public final boolean setUserPrefs(final Document columnsPrefs) {
        if (columnsPrefs != null) {
            // user preferences application
            final Element rootElement = columnsPrefs.getRootElement();
            if (!rootElement.getName().equals("list")) {
                throw new IllegalArgumentException("invalid xml, roots node list expected but " + rootElement.getName() + " found");
            }
            final List<Element> xmlColumns = rootElement.getChildren();
            final int columnsCount = this.columns.size();
            if (xmlColumns.size() == columnsCount) {
                for (int i = 0; i < columnsCount; i++) {
                    final ColumnSpec columnSpec = this.columns.get(i);
                    final String columnId = columnSpec.getId();
                    boolean find = false;

                    for (int j = 0; j < columnsCount; j++) {
                        final Element xmlColumn = xmlColumns.get(j);
                        final String xmlColumnId = xmlColumn.getAttribute("id").getValue();

                        if (xmlColumnId.equals(columnId)) {

                            if (!xmlColumn.getName().equals("column")) {
                                throw new IllegalArgumentException("ColumnSpec setPrefs - Invalid xml, element node column expected but " + xmlColumn.getName() + " found");
                            }
                            if (xmlColumn.getAttribute("width") == null || xmlColumn.getAttribute("min-width") == null || xmlColumn.getAttribute("max-width") == null) {
                                throw new IllegalArgumentException("ColumnSpec setPrefs - Invalid column node for " + columnId + ", it must have attribute width, min-width, max-width");
                            }

                            final double width = Double.parseDouble(xmlColumn.getAttribute("width").getValue());
                            final double maxWidth = Double.parseDouble(xmlColumn.getAttribute("max-width").getValue());
                            final double minWidth = Double.parseDouble(xmlColumn.getAttribute("min-width").getValue());

                            columnSpec.setPrefs(width, maxWidth, minWidth);
                            if (i != j) {
                                final ColumnSpec swap = this.columns.get(i);
                                this.columns.set(i, this.columns.get(j));
                                this.columns.set(j, swap);
                                
                                i--;
                            }
                            find = true;
                            break;
                        }
                    }
                    if (!find) {
                        return false;
                    }
                }
            } else {
                return false;
            }
        }
        return true;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeUTF(this.id);
        out.writeInt(this.fixedColumns);
        out.writeObject(this.columns);
        out.writeObject(this.possibleColumnIds);
        out.writeObject(this.sortedIds);
        out.writeBoolean(this.allowMove);
        out.writeBoolean(this.allowResize);
        out.writeBoolean(this.adaptWidth);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        this.id = in.readUTF();
        this.fixedColumns = in.readInt();
        this.columns = CollectionUtils.castList((List<?>) in.readObject(), ColumnSpec.class);
        this.possibleColumnIds = CollectionUtils.castList((List<?>) in.readObject(), String.class);
        this.sortedIds = CollectionUtils.castList((List<?>) in.readObject(), String.class);
        this.allowMove = in.readBoolean();
        this.allowResize = in.readBoolean();
        this.adaptWidth = in.readBoolean();
    }

    public final List<Object> getDefaultValues() {
        final List<Object> l = new ArrayList<Object>();
        for (ColumnSpec column : this.columns) {
            final Object v = column.getDefaultValue();
            l.add(v);
        }
        return l;
    }

    public final ColumnSpec getColumn(String id) {
        for (ColumnSpec c : this.columns) {
            if (c.getId().equals(id)) {
                return c;
            }
        }
        return null;
    }

    public final ColumnSpec getColumnWithEditor(String id) {
        for (ColumnSpec c : this.columns) {
            LightUIElement editor = c.getEditor();
            if (editor != null && c.getEditor().getId().equals(id)) {
                return c;
            }
        }
        return null;
    }

    @Override
    public JSONObject toJSON() {
        final JSONObject result = new JSONObject();
        result.put("class", "ColumnsSpec");
        result.put("id", this.id);
        result.put("fixed-columns", this.fixedColumns);
        if (this.sortedIds != null && this.sortedIds.size() > 0) {
            result.put("sorted-ids", this.sortedIds);
        }
        if (this.possibleColumnIds != null && this.possibleColumnIds.size() > 0) {
            result.put("possible-column-ids", this.possibleColumnIds);
        }
        if (this.columns != null && this.columns.size() > 0) {
            result.put("columns", JSONConverter.getJSON(this.columns));
        }
        if (this.adaptWidth) {
            result.put("adapt-width", JSONConverter.getJSON(true));
        }
        if (this.allowMove) {
            result.put("allow-move", JSONConverter.getJSON(true));
        }
        if (this.allowResize) {
            result.put("allow-resize", JSONConverter.getJSON(true));
        }
        return result;
    }

    @Override
    public void fromJSON(final JSONObject json) {
        this.id = JSONConverter.getParameterFromJSON(json, "id", String.class);
        this.fixedColumns = JSONConverter.getParameterFromJSON(json, "fixed-columns", Integer.class);
        this.adaptWidth = JSONConverter.getParameterFromJSON(json, "adapt-width", Boolean.class, false);
        this.allowMove = JSONConverter.getParameterFromJSON(json, "allow-move", Boolean.class, false);
        this.allowResize = JSONConverter.getParameterFromJSON(json, "allow-resize", Boolean.class, false);
        this.sortedIds = CollectionUtils.castList(JSONConverter.getParameterFromJSON(json, "sorted-ids", List.class, new ArrayList<String>()), String.class);
        this.possibleColumnIds = CollectionUtils.castList(JSONConverter.getParameterFromJSON(json, "possible-column-ids", List.class, new ArrayList<String>()), String.class);

        final List<JSONObject> jsonColumns = CollectionUtils.castList(JSONConverter.getParameterFromJSON(json, "columns", JSONArray.class, null), JSONObject.class);
        if (jsonColumns != null) {
            for (final JSONObject jsonColumn : jsonColumns) {
                this.columns.add(new ColumnSpec(jsonColumn));
            }
        }
    }
}
