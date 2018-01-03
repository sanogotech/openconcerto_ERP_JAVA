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
import java.util.List;

import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.io.JSONAble;
import org.openconcerto.utils.io.JSONConverter;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

public class RowSelectionSpec implements Externalizable, JSONAble {
    private String tableId;

    private List<Number> ids;

    /**
     * Define selected ids of a table. ids are ids from selected lines
     */
    public RowSelectionSpec() {
        // Serialization
    }

    public RowSelectionSpec(final JSONObject json) {
        this.fromJSON(json);
    }
    
    public RowSelectionSpec(String tableId) {
        this(tableId, new ArrayList<Number>());
    }

    public RowSelectionSpec(final String tableId, final List<Number> selectedIds) {
        this.tableId = tableId;
        this.ids = selectedIds;
    }

    public final List<Number> getIds() {
        return this.ids;
    }
    
    public final void setIds(List<Number> ids) {
        this.ids = ids;
    }

    public String getTableId() {
        return this.tableId;
    }
    
    public boolean hasSelectedId(){
        return this.getIds() != null && !this.getIds().isEmpty();
    }

    @Override
    public String toString() {
        final StringBuilder r = new StringBuilder("RowSelectionSpec: ").append(this.tableId).append(" : ");
        final int idsSize = this.ids.size();
        for (int i = 0; i < idsSize; i++) {
            if (i < idsSize - 1) {
                r.append(this.ids.get(i)).append(", ");
            } else {
                r.append(this.ids.get(i));
            }
        }
        return r.toString();
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        try {
            out.writeUTF(this.tableId);
            out.writeObject(this.ids);

        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException(e);
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        this.tableId = in.readUTF();
        this.ids = CollectionUtils.castList((List<?>) in.readObject(), Number.class);

    }

    @Override
    public JSONObject toJSON() {
        final JSONObject json = new JSONObject();
        json.put("class", "RowSelectionSpec");
        json.put("table-id", this.tableId);
        json.put("ids", this.ids);
        return json;
    }

    @Override
    public void fromJSON(JSONObject json) {
        this.tableId = JSONConverter.getParameterFromJSON(json, "table-id", String.class);
        final JSONArray jsonIds = JSONConverter.getParameterFromJSON(json, "ids", JSONArray.class);
        this.ids = new ArrayList<Number>();
        for (final Object jsonId : jsonIds) {
            this.ids.add(JSONConverter.getObjectFromJSON(jsonId, Number.class));
        }
    }

    public void setTableId(String id) {
        this.tableId = id;
    }

}
