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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openconcerto.utils.io.JSONAble;
import org.openconcerto.utils.io.JSONConverter;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

public class RowsBulk implements Externalizable, JSONAble {

    private List<Row> rows;
    private int offset;
    private int total;
    private boolean remove;

    public RowsBulk() {// Serialization
    }

    public RowsBulk(final JSONObject json) {
        this.fromJSON(json);
    }

    public RowsBulk(List<Row> rows, int offset, int total, final boolean remove) {
        this.rows = rows;
        this.offset = offset;
        this.total = total;
        this.remove = remove;
    }

    // Sending by column : size gain is 5%
    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        int rowCount = in.readInt();
        if (rowCount == 0) {
            this.rows = Collections.emptyList();
        } else {
            this.rows = new ArrayList<Row>(rowCount);// colcount
            int columnCount = in.readByte();
            // id
            for (int j = 0; j < rowCount; j++) {
                Row row = new Row((Number) in.readObject(), columnCount);
                this.rows.add(row);
            }

            for (int i = 0; i < columnCount; i++) {
                for (int j = 0; j < rowCount; j++) {
                    Object v = in.readObject();
                    this.rows.get(j).addValue(v);
                }

            }

        }
        this.offset = in.readInt();
        this.total = in.readInt();
        this.remove = in.readBoolean();
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        // nb rows
        int rowCount = this.rows.size();
        out.writeInt(rowCount);
        // content
        if (this.rows.size() > 0) {
            // nbcols
            int columnCount = this.rows.get(0).getValues().size();
            out.writeByte(columnCount);
            // ids
            for (int j = 0; j < rowCount; j++) {
                Row row = this.rows.get(j);
                out.writeObject(row.getId());

            }

            // send cols by cols
            for (int i = 0; i < columnCount; i++) {

                for (int j = 0; j < rowCount; j++) {
                    Row row = this.rows.get(j);
                    Object v = row.getValues().get(i);
                    out.writeObject(v);
                }

            }

        }
        out.writeInt(this.offset);
        out.writeInt(this.total);
        out.writeBoolean(this.remove);
    }

    public final List<Row> getRows() {
        return this.rows;
    }

    public final int getOffset() {
        return this.offset;
    }

    public final int getTotal() {
        return this.total;
    }

    public final boolean isRemove() {
        return this.remove;
    }

    @Override
    public JSONObject toJSON() {
        final JSONObject result = new JSONObject();
        result.put("class", "RowsBulk");
        result.put("rows", JSONConverter.getJSON(this.rows));
        result.put("offset", this.offset);
        result.put("total", this.total);
        result.put("remove", this.remove);
        return result;
    }

    @Override
    public void fromJSON(final JSONObject json) {
        this.offset = JSONConverter.getParameterFromJSON(json, "offset", Integer.class);
        this.total = JSONConverter.getParameterFromJSON(json, "total", Integer.class);

        final JSONArray jsonRows = JSONConverter.getParameterFromJSON(json, "rows", JSONArray.class);
        this.rows = new ArrayList<Row>();
        if (jsonRows != null) {
            for (final Object o : jsonRows) {
                this.rows.add(new Row(JSONConverter.getObjectFromJSON(o, JSONObject.class)));
            }
        }
    }

    @Override
    public String toString() {
        return this.rows.size() + " rows at offset " + offset + " (total:" + total + ") remove:" + this.remove;
    }
}
