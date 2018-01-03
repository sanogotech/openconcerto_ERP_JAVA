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
import org.openconcerto.utils.io.Transferable;

import net.minidev.json.JSONObject;

public class LightUIComboBoxElement implements Transferable {
    /**
     * ID of row, must be unique for a LightUIComboBox. It's represent the SQLRow ID when the
     * LightUICombo is attach to a field in database
     */
    private Integer id = null;

    private String value1 = null;
    private String value2 = null;
    private String icon = null;

    public LightUIComboBoxElement(final JSONObject json) {
        this.fromJSON(json);
    }

    public LightUIComboBoxElement(final int id) {
        this.id = id;
    }
    
    public LightUIComboBoxElement(final int id, final String value1) {
        this.id = id;
        this.value1 = value1;
    }
    
    public LightUIComboBoxElement(final int id, final String value1, final String value2) {
        this.id = id;
        this.value1 = value1;
        this.value2 = value2;
    }

    public LightUIComboBoxElement(final LightUIComboBoxElement comboElement) {
        this.id = comboElement.id;
        this.value1 = comboElement.value1;
        this.value2 = comboElement.value2;
        this.icon = comboElement.icon;
    }

    public int getId() {
        return this.id;
    }

    public String getValue1() {
        return this.value1;
    }

    public String getValue2() {
        return this.value2;
    }

    public String getIcon() {
        return this.icon;
    }

    public void setValue1(final String value1) {
        this.value1 = value1;
    }

    public void setValue2(final String value2) {
        this.value2 = value2;
    }

    public void setIcon(final String icon) {
        this.icon = icon;
    }

    @Override
    public JSONObject toJSON() {
        final JSONObject json = new JSONObject();

        if (this.id == null) {
            throw new IllegalArgumentException("Attribute id must be not null before send to client");
        }
        json.put("id", this.id);
        json.put("class", "LightUIComboBoxElement");

        if (this.value1 != null) {
            json.put("value1", this.value1);
        }
        if (this.value2 != null) {
            json.put("value2", this.value2);
        }
        if (this.icon != null) {
            json.put("icon", this.icon);
        }

        return json;
    }

    @Override
    public void fromJSON(final JSONObject json) {
        this.id = (Integer) JSONConverter.getParameterFromJSON(json, "id", Integer.class);
        this.value1 = (String) JSONConverter.getParameterFromJSON(json, "value1", String.class);
        this.value2 = (String) JSONConverter.getParameterFromJSON(json, "value2", String.class);
        this.icon = (String) JSONConverter.getParameterFromJSON(json, "icon", String.class);
    }
}
