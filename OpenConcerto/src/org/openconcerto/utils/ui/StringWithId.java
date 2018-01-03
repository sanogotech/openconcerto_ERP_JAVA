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
 
 package org.openconcerto.utils.ui;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.openconcerto.utils.io.JSONConverter;
import org.openconcerto.utils.io.Transferable;
import net.minidev.json.JSONObject;

public class StringWithId implements Transferable, Externalizable {
    private long id;
    // value is always trimed
    private String value;

    public StringWithId() {
    }

    public StringWithId(final JSONObject json) {
        this.fromJSON(json);
    }

    public StringWithId(long id, String value) {
        this.id = id;
        this.value = value.trim();
    }

    public StringWithId(String condensedValue) {
        int index = condensedValue.indexOf(',');
        if (index <= 0) {
            throw new IllegalArgumentException("invalid condensed value " + condensedValue);
        }
        this.id = Long.parseLong(condensedValue.substring(0, index));
        this.value = condensedValue.substring(index + 1).trim();
    }

    public long getId() {
        return this.id;
    }

    public String getValue() {
        return this.value;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeLong(this.id);
        out.writeUTF(this.value);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        this.id = in.readLong();
        this.value = in.readUTF().trim();
    }

    @Override
    public String toString() {
        return this.value;
    }

    @Override
    public boolean equals(Object obj) {
        StringWithId o = (StringWithId) obj;
        return o.getId() == this.getId() && o.getValue().endsWith(this.getValue());
    }

    @Override
    public int hashCode() {
        return (int) this.id + this.value.hashCode();
    }

    public String toCondensedString() {
        return this.id + "," + this.value;
    }

    @Override
    public JSONObject toJSON() {
        final JSONObject result = new JSONObject();
        result.put("class", "StringWithId");
        result.put("id", this.id);
        result.put("value", this.value);
        return result;
    }

    @Override
    public void fromJSON(JSONObject json) {
        this.id = (Integer) JSONConverter.getParameterFromJSON(json, "id", Integer.class);
        this.value = (String) JSONConverter.getParameterFromJSON(json, "value", String.class);
    }
}
