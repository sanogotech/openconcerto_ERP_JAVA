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

import java.awt.Color;

import net.minidev.json.JSONObject;

public class LightUIColorPicker extends LightUserControl {

    public LightUIColorPicker(final String id) {
        super(id);
        this.setType(TYPE_COLOR_PICKER);
        this.setBorderColor(Color.BLACK);
    }

    public LightUIColorPicker(final JSONObject json) {
        super(json);
    }

    public LightUIColorPicker(final LightUIColorPicker element) {
        super(element);
    }

    public void setSelectedColor(final Color color) {
        this.setValue(String.valueOf(color.getRGB()));
    }

    public Color getSelectedColor() {
        if (this.getValue() == null || this.getValue().isEmpty()) {
            return null;
        } else {
            return Color.decode(this.getValue());
        }
    }

    @Override
    protected void _setValueFromContext(final Object value) {
        this.setValue((String) value);
    }

    @Override
    public Object getValueForContext() {
        return this.getValue();
    }
}
