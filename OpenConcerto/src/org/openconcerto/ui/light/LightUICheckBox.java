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

import net.minidev.json.JSONObject;

public class LightUICheckBox extends LightUserControl {

    public LightUICheckBox(final JSONObject json) {
        super(json);
    }

    public LightUICheckBox(final String id, final String label) {
        super(id);
        this.setType(TYPE_CHECKBOX);

        this.setLabel(label);
        this.setValueType(LightUIElement.VALUE_TYPE_BOOLEAN);
    }

    public LightUICheckBox(final LightUICheckBox button) {
        super(button);
    }

    public void setChecked(boolean checked) {
        if (checked) {
            this.setValue("true");
        } else {
            this.setValue("false");
        }
    }

    public boolean isChecked() {
        if (this.getValue() == null) {
            return false;
        }
        return this.getValue().equals("true");
    }

    @Override
    public JSONToLightUIConvertor getConvertor() {
        return new JSONToLightUIConvertor() {
            @Override
            public LightUIElement convert(final JSONObject json) {
                return new LightUICheckBox(json);
            }
        };
    }

    @Override
    public LightUIElement clone() {
        return new LightUICheckBox(this);
    }

    @Override
    public Object getValueForContext() {
        return this.isChecked();
    }

    @Override
    public void _setValueFromContext(Object value) {
        boolean bValue = false;
        if (value != null) {
            if (value.equals("true")) {
                bValue = true;
            }
            if (value.equals(true)) {
                bValue = true;
            }
        }
        this.setChecked(bValue);
    }
}
