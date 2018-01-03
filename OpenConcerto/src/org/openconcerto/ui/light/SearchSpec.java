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

public class SearchSpec implements Transferable {
    private String tableId;
    private List<SearchContent> content = new ArrayList<SearchContent>();

    public SearchSpec(final String tableId) {
        this.tableId = tableId;
    }

    public SearchSpec(final JSONObject json) {
        this.fromJSON(json);
    }

    public final String getTableId() {
        return this.tableId;
    }

    public final List<SearchContent> getContent() {
        return this.content;
    }

    @Override
    public JSONObject toJSON() {
        final JSONObject json = new JSONObject();
        json.put("table-id", this.tableId);
        json.put("content", JSONConverter.getJSON(this.content));
        return json;
    }

    @Override
    public void fromJSON(JSONObject json) {
        this.tableId = (String) JSONConverter.getParameterFromJSON(json, "table-id", String.class);
        final JSONArray jsonContent = (JSONArray) JSONConverter.getParameterFromJSON(json, "content", JSONArray.class);
        if (jsonContent != null) {
            this.content = new ArrayList<SearchContent>();
            for (final Object o : jsonContent) {
                this.content.add(new SearchContent((JSONObject) JSONConverter.getObjectFromJSON(o, JSONObject.class)));
            }
        }
    }
}
