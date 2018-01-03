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

import org.openconcerto.utils.io.Transferable;
import net.minidev.json.JSONObject;

public class SearchContent implements Transferable {

    private String column;
    private String text;
    private String type;
    private String operator;

    public SearchContent(final JSONObject json) {
        this.fromJSON(json);
    }

    public String getColumn() {
        return this.column;
    }

    public String getText() {
        return this.text;
    }

    public String getType() {
        return this.type;
    }

    public String getOperator() {
        return this.operator;
    }

    @Override
    public JSONObject toJSON() {
        final JSONObject json = new JSONObject();
        json.put("column", this.column);
        json.put("text", this.text);
        json.put("type", this.type);
        json.put("operator", this.operator);
        return json;
    }

    @Override
    public void fromJSON(final JSONObject json) {
        if (!json.containsKey("column") || !(json.get("column") instanceof String)) {
            throw new IllegalArgumentException("value for 'column' not found or invalid");
        }
        if (!json.containsKey("text") || !(json.get("text") instanceof String)) {
            throw new IllegalArgumentException("value for 'text' not found or invalid");
        }
        if (!json.containsKey("type") || !(json.get("type") instanceof String)) {
            throw new IllegalArgumentException("value for 'type' not found or invalid");
        }
        if (json.containsKey("operator")) {
            if (json.get("operator") != null) {
                if (!(json.get("operator") instanceof String)) {
                    throw new IllegalArgumentException("value for 'operator' not found or invalid");
                } else {
                    this.operator = (String) json.get("operator");
                }
            }
        }

        this.column = (String) json.get("column");
        this.text = (String) json.get("text");
        this.type = (String) json.get("type");
    }
}
