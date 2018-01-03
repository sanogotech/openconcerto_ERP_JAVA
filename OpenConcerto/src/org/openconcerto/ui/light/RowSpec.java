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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.List;

import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.io.JSONAble;
import org.openconcerto.utils.io.JSONConverter;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

public class RowSpec implements Externalizable, JSONAble {
    private String tableId;
    private String[] columnIds;

    public RowSpec() {
        // Serialization
    }

    public RowSpec(final JSONObject json) {
        this.fromJSON(json);
    }

    public RowSpec(String tableId, String[] columnIds) {
        this.tableId = tableId;
        this.columnIds = columnIds;
    }

    public String[] getIds() {
        return this.columnIds;
    }

    public void setIds(String[] columnIds) {
        this.columnIds = columnIds;
    }

    public void setTableId(String tableId) {
        this.tableId = tableId;
    }

    public String getTableId() {
        return this.tableId;
    }

    @Override
    public String toString() {
        String r = "RowSpec:" + this.tableId + " : ";
        for (int i = 0; i < this.columnIds.length; i++) {
            if (i < this.columnIds.length - 1) {
                r += this.columnIds[i] + ", ";
            } else {
                r += this.columnIds[i];
            }
        }
        return r;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        try {
            out.writeUTF(this.tableId);
            out.writeObject(this.columnIds);

        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException(e);
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        this.tableId = in.readUTF();
        this.columnIds = (String[]) in.readObject();
    }

    @Override
    public JSONObject toJSON() {
        final JSONObject result = new JSONObject();
        result.put("class", "RowSpec");
        result.put("table-id", this.tableId);
        result.put("column-ids", this.columnIds);
        return result;
    }

    @Override
    public void fromJSON(final JSONObject json) {
        this.tableId = (String) JSONConverter.getParameterFromJSON(json, "table-id", String.class);

        final JSONArray jsonColumnIds = (JSONArray) json.get("column-ids");
        if (jsonColumnIds != null) {
            try {
                this.columnIds = new String[jsonColumnIds.size()];
                this.columnIds = CollectionUtils.castList((List<?>) jsonColumnIds, String.class).toArray(this.columnIds);
            } catch (final Exception ex) {
                throw new IllegalArgumentException("invalid value for 'possible-column-ids', List<String> expected");
            }
        }

        if (!json.containsKey("table-id") || (json.get("table-id") instanceof String)) {
            throw new IllegalArgumentException("value for 'value-type' not found or invalid");
        }
        if (!json.containsKey("column-ids") || (json.get("column-ids") instanceof JSONArray)) {
            throw new IllegalArgumentException("value for 'value-type' not found or invalid");
        }
        this.tableId = (String) json.get("table-id");

        final int columnCount = jsonColumnIds.size();
        this.columnIds = new String[columnCount];
        for (int i = 0; i < columnCount; i++) {
            final Object jsonColumnId = jsonColumnIds.get(i);
            if (!(jsonColumnId instanceof String)) {
                throw new IllegalArgumentException("one or more column Ids are invalid in 'column-ids'");
            }
            this.columnIds[i] = (String) jsonColumnId;
        }
    }
}
