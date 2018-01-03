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

import net.minidev.json.JSONObject;

public class LightUISlider extends LightUserControl {
    Integer maxValue = 1;
    Integer minValue = 0;
    Integer increment = 1;

    public LightUISlider(final String id) {
        super(id);
        this.setType(TYPE_SLIDER);
        this.setValueType(VALUE_TYPE_INTEGER);
        this.setValue("0");
    }

    public LightUISlider(final JSONObject json) {
        super(json);
    }

    public LightUISlider(final LightUISlider slider) {
        super(slider);
        this.maxValue = slider.maxValue;
        this.minValue = slider.minValue;
        this.increment = slider.increment;
    }

    public Integer getMaxValue() {
        return this.maxValue;
    }

    public void setMaxValue(final int maxValue) {
        this.maxValue = maxValue;
    }

    public Integer getMinValue() {
        return this.minValue;
    }

    public void setMinValue(final int minValue) {
        this.minValue = minValue;
    }

    public Integer getIncrement() {
        return this.increment;
    }

    public void setIncrement(final int increment) {
        this.increment = increment;
    }

    @Override
    public JSONObject toJSON() {
        final JSONObject json = super.toJSON();
        json.put("max-value", this.maxValue);
        json.put("min-value", this.minValue);
        json.put("increment", this.increment);

        return json;
    }

    @Override
    public void fromJSON(JSONObject json) {
        super.fromJSON(json);
        this.maxValue = JSONConverter.getParameterFromJSON(json, "max-value", Integer.class, 1);
        this.minValue = JSONConverter.getParameterFromJSON(json, "min-value", Integer.class, 0);
        this.increment = JSONConverter.getParameterFromJSON(json, "increment", Integer.class, 1);
    }

    @Override
    public void _setValueFromContext(Object value) {
        if (value instanceof Number) {
            this.setValue(value.toString());
        } else {
            throw new IllegalArgumentException("Incorrect value " + value + "type for ui element: " + this.getId());
        }
    }

    @Override
    public Object getValueForContext() {
        if (this.getValue() == null) {
            return null;
        } else {
            return Integer.parseInt(this.getValue());
        }
    }
}
