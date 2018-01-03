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

public class LightUITextField extends LightUserControl {
    public LightUITextField(final String id) {
        super(id);
        this.setType(TYPE_TEXT_FIELD);
        this.setValueType(LightUIElement.VALUE_TYPE_STRING);
    }

    public LightUITextField(final JSONObject json) {
        super(json);
    }

    public LightUITextField(final LightUITextField text) {
        super(text);
    }

    @Override
    public LightUIElement clone() {
        return new LightUITextField(this);
    }

    @Override
    public JSONToLightUIConvertor getConvertor() {
        return new JSONToLightUIConvertor() {
            @Override
            public LightUIElement convert(final JSONObject json) {
                return new LightUITextField(json);
            }
        };
    }

    @Override
    public Object getValueForContext() {
        return (this.getValue() == null) ? null : ((this.getValue().trim().equals("")) ? null : this.getValue());
    }

    @Override
    public void _setValueFromContext(final Object value) {
        final String strValue = JSONConverter.getObjectFromJSON(value, String.class);
        if (strValue != null && !strValue.trim().isEmpty()) {
            this.setValue(strValue);
        } else {
            this.setValue(null);
        }
    }
}
