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
import org.openconcerto.utils.io.JSONAble;
import org.openconcerto.utils.io.JSONConverter;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.List;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

public class Row implements Externalizable, JSONAble {

    private Number id;
    private String extendId;
    private List<Object> values;

    private Boolean fillWidth = false;
    private Boolean toggleable = false;
    private Boolean visible = true;

    public Row() {
        // Serialization
    }

    public Row(final JSONObject json) {
        this.fromJSON(json);
    }

    public Row(final Number id, int valueCount) {
        this.id = id;
        this.values = new ArrayList<Object>();
        if (valueCount > 0) {
            for (int i = 0; i < valueCount; i++) {
                this.values.add(null);
            }
        }
    }

    public Row(final Number id) {
        this.id = id;
    }

    public final void setValues(List<Object> values) {
        this.values = values;
    }

    public final List<Object> getValues() {
        return this.values;
    }

    public final Number getId() {
        return this.id;
    }

    public final String getExtendId() {
        return this.extendId;
    }

    public final void setExtendId(final String extendId) {
        this.extendId = extendId;
    }

    public final void addValue(Object v) {
        this.values.add(v);
    }

    public final void setValue(int index, Object v) {
        this.values.set(index, v);
    }

    public final Boolean isFillWidth() {
        return this.fillWidth;
    }

    public final void setFillWidth(final Boolean fillWidth) {
        this.fillWidth = fillWidth;
    }

    public final Boolean isToggleable() {
        return this.toggleable;
    }

    public final void setToggleable(final Boolean toggleable) {
        this.toggleable = toggleable;
    }

    public final Boolean isVisible() {
        return this.visible;
    }

    public final void setVisible(final Boolean visible) {
        this.visible = visible;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(this.id);
        out.writeUTF(this.extendId);
        out.writeObject(this.values);
        out.writeBoolean(this.fillWidth);
        out.writeBoolean(this.toggleable);
        out.writeBoolean(this.visible);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        this.id = (Number) in.readObject();
        this.extendId = in.readUTF();
        this.values = CollectionUtils.castList((List<?>) in.readObject(), Object.class);
        this.fillWidth = in.readBoolean();
        this.toggleable = in.readBoolean();
        this.visible = in.readBoolean();
    }

    @Override
    public JSONObject toJSON() {
        final JSONObject result = new JSONObject();

        result.put("class", "Row");
        result.put("id", this.id);
        if (this.extendId != null) {
            result.put("extend-id", this.extendId);
        }
        if (!this.values.isEmpty()) {
            result.put("values", JSONConverter.getJSON(this.values));
        }
        if (this.fillWidth) {
            result.put("fill-width", true);
        }
        if (this.toggleable) {
            result.put("toggleable", true);
        }
        if (!this.visible) {
            result.put("visible", false);
        }

        return result;
    }

    @Override
    public void fromJSON(final JSONObject json) {
        this.id = JSONConverter.getParameterFromJSON(json, "id", Number.class);
        this.extendId = JSONConverter.getParameterFromJSON(json, "extend-id", String.class);
        this.fillWidth = JSONConverter.getParameterFromJSON(json, "fill-width", Boolean.class, false);
        this.toggleable = JSONConverter.getParameterFromJSON(json, "toggleable", Boolean.class, false);
        this.visible = JSONConverter.getParameterFromJSON(json, "visible", Boolean.class, true);

        final JSONArray jsonValues = (JSONArray) JSONConverter.getParameterFromJSON(json, "values", JSONArray.class);
        if (jsonValues != null) {
            final int valuesSize = jsonValues.size();
            this.values = new ArrayList<Object>(valuesSize);
            for (int i = 0; i < valuesSize; i++) {
                Object objValue = jsonValues.get(i);
                if (objValue instanceof JSONObject) {
                    final JSONObject jsonValue = (JSONObject) objValue;
                    objValue = JSONToLightUIConvertorManager.getInstance().createUIElementFromJSON(jsonValue);
                } else {
                    if (objValue instanceof String) {
                        objValue = JSONConverter.getObjectFromJSON(objValue, String.class);
                    } else if (objValue instanceof Integer) {
                        objValue = JSONConverter.getObjectFromJSON(objValue, Integer.class);
                    } else if (objValue instanceof Long) {
                        objValue = JSONConverter.getObjectFromJSON(objValue, Long.class);
                    } else if (objValue instanceof Boolean) {
                        objValue = JSONConverter.getObjectFromJSON(objValue, Boolean.class);
                    } else if (objValue != null) {
                        throw new IllegalArgumentException("unknow type: " + objValue.getClass().getName());
                    }
                }
                this.values.add(objValue);
            }
        }
    }

    @Override
    public String toString() {
        return "Row index: " + this.id + " values: " + this.values;
    }
}
