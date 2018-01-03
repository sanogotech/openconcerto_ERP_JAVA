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

public class LightUIHourEditor extends LightUserControl {

    private int hour;
    private int minute;

    public LightUIHourEditor(final JSONObject json) {
        super(json);
    }

    public LightUIHourEditor(final String id, int hour, int minute) {
        super(id);
        this.hour = hour;
        this.minute = minute;
        this.setType(TYPE_HOUR_EDITOR);
        this.setValueType(LightUIElement.VALUE_TYPE_INTEGER);
    }

    public LightUIHourEditor(LightUIHourEditor lightUIHourEditor) {
        super(lightUIHourEditor);
        this.hour = lightUIHourEditor.hour;
        this.minute = lightUIHourEditor.minute;
    }

    public int getHour() {
        return hour;
    }

    public void setHour(int hour) {
        this.hour = hour;
    }

    public int getMinute() {
        return minute;
    }

    public void setMinute(int minute) {
        this.minute = minute;
    }

    @Override
    public JSONToLightUIConvertor getConvertor() {
        return new JSONToLightUIConvertor() {
            @Override
            public LightUIElement convert(final JSONObject json) {
                return new LightUIHourEditor(json);
            }
        };
    }

    @Override
    public Object getValueForContext() {
        return this.hour + 60 * this.minute;
    }

    @Override
    public void _setValueFromContext(Object value) {
        final Integer strValue = (Integer) JSONConverter.getObjectFromJSON(value, Integer.class);
        this.hour = strValue / 60;
        this.minute = strValue % 60;
    }

    @Override
    public LightUIElement clone() {
        return new LightUIHourEditor(this);
    }
}
