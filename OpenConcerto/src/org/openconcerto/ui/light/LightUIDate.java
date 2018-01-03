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

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import net.minidev.json.JSONObject;

public class LightUIDate extends LightUserControl {

    public LightUIDate(final JSONObject json) {
        super(json);
    }

    public LightUIDate(final String id) {
        super(id);
        this.setType(TYPE_DATE);
        this.setValueType(LightUIElement.VALUE_TYPE_DATE);
    }

    public LightUIDate(final LightUIDate date) {
        super(date);
    }

    public Timestamp getValueAsDate() {
        if (this.getValue() != null && this.getValue() != "") {
            SimpleDateFormat df2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S");
            try {
                return new Timestamp(df2.parse(this.getValue()).getTime());
            } catch (final ParseException ex) {
                throw new IllegalArgumentException(ex.getMessage(), ex);
            }
        }
        return null;
    }

    @Override
    public JSONToLightUIConvertor getConvertor() {
        return new JSONToLightUIConvertor() {
            @Override
            public LightUIElement convert(final JSONObject json) {
                return new LightUIDate(json);
            }
        };
    }

    @Override
    public Object getValueForContext() {
        return this.getValueAsDate();
    }

    @Override
    public void _setValueFromContext(Object value) {
        final String strValue = (String) JSONConverter.getObjectFromJSON(value, String.class);
        this.setValue(strValue);
    }

    @Override
    public LightUIElement clone() {
        return new LightUIDate(this);
    }
}
