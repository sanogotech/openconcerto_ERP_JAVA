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

import org.openconcerto.utils.i18n.TranslationManager;
import org.openconcerto.utils.io.JSONConverter;

import java.util.ArrayList;
import java.util.List;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

public class LightUIComboBox extends LightUserControl {
    private static final String HAS_NOT_SPECIFIED_LINE = "has-not-specified-line";
    private static final String VALUES = "values";
    private static final String SELECTED_VALUE = "selected-value";
    private static final String ALREADY_FILLED = "already-filled";

    private boolean alreadyFilled = false;
    private boolean hasNotSpecifedLine = false;

    private LightUIComboBoxElement selectedValue = null;

    private List<LightUIComboBoxElement> values = new ArrayList<LightUIComboBoxElement>();

    // Init from json constructor
    public LightUIComboBox(final JSONObject json) {
        super(json);
    }

    // Clone constructor
    public LightUIComboBox(final LightUIComboBox combo) {
        super(combo);
    }

    public LightUIComboBox(final String id) {
        super(id);
        this.setType(TYPE_COMBOBOX);
    }

    public void addValue(final LightUIComboBoxElement values) {
        this.values.add(values);
    }

    public void addValues(final List<LightUIComboBoxElement> values) {
        this.values.addAll(values);
    }

    public static LightUIComboBoxElement getDefaultValue() {
        final String defaultLabelKey = "not.specified.label";
        final String defaultLabel = TranslationManager.getInstance().getTranslationForItem(defaultLabelKey);

        final LightUIComboBoxElement defaultElement = new LightUIComboBoxElement(0);
        if (defaultLabel != null) {
            defaultElement.setValue1(defaultLabel);
        } else {
            defaultElement.setValue1(defaultLabelKey);
        }

        return defaultElement;
    }

    public List<LightUIComboBoxElement> getValues() {
        return this.values;
    }

    // if id=0 means this is the not specifed line
    public boolean hasSelectedValue() {
        return this.selectedValue != null && ((this.hasNotSpecifedLine && this.selectedValue.getId() != 0) || !this.hasNotSpecifedLine);
    }

    public LightUIComboBoxElement getSelectedValue() {
        return this.selectedValue;
    }

    public void setSelectedValue(final LightUIComboBoxElement selectedValue) {
        this.selectedValue = selectedValue;
    }

    public void clearValues() {
        this.selectedValue = null;
        this.values.clear();
    }

    public void setAlreadyFilled(final boolean alreadyFilled) {
        this.alreadyFilled = alreadyFilled;
    }

    public boolean isAlreadyFilled() {
        return this.alreadyFilled;
    }

    public void setHasNotSpecifedLine(final boolean hasNotSpecifedLine) {
        this.hasNotSpecifedLine = hasNotSpecifedLine;
    }

    public boolean hasNotSpecifedLine() {
        return this.hasNotSpecifedLine;
    }

    public void setSelectedId(final Integer id) {
        if (id == null) {
            this.setSelectedValue(null);
        } else {
            for (final LightUIComboBoxElement value : this.values) {
                if (value.getId() == id) {
                    this.setSelectedValue(value);
                    break;
                }
            }
        }
    }

    @Override
    protected void copy(LightUIElement element) {
        super.copy(element);

        if (!(element instanceof LightUIComboBox)) {
            throw new InvalidClassException(this.getClassName(), element.getClassName(), element.getId());
        }

        final LightUIComboBox combo = (LightUIComboBox) element;
        this.alreadyFilled = combo.alreadyFilled;
        this.values = combo.values;
        this.selectedValue = combo.selectedValue;
    }

    @Override
    public JSONToLightUIConvertor getConvertor() {
        return new JSONToLightUIConvertor() {
            @Override
            public LightUIElement convert(final JSONObject json) {
                return new LightUIComboBox(json);
            }
        };
    }

    @Override
    public LightUIElement clone() {
        return new LightUIComboBox(this);
    }

    @Override
    public JSONObject toJSON() {
        final JSONObject json = super.toJSON();

        if (this.values != null && this.values.size() > 0) {
            final JSONArray jsonValues = new JSONArray();
            for (final LightUIComboBoxElement value : this.values) {
                jsonValues.add(value.toJSON());
            }
            json.put(VALUES, jsonValues);
        }

        if (this.alreadyFilled) {
            json.put(ALREADY_FILLED, true);
        }
        if (this.hasNotSpecifedLine) {
            json.put(HAS_NOT_SPECIFIED_LINE, true);
        }
        if (this.selectedValue != null) {
            json.put(SELECTED_VALUE, this.selectedValue.toJSON());
        }

        return json;
    }

    @Override
    public void fromJSON(final JSONObject json) {
        super.fromJSON(json);

        this.alreadyFilled = JSONConverter.getParameterFromJSON(json, ALREADY_FILLED, Boolean.class, false);
        this.hasNotSpecifedLine = JSONConverter.getParameterFromJSON(json, HAS_NOT_SPECIFIED_LINE, Boolean.class, false);

        final JSONObject jsonSelectedValue = JSONConverter.getParameterFromJSON(json, "", JSONObject.class);
        if (jsonSelectedValue != null) {
            this.selectedValue = new LightUIComboBoxElement(jsonSelectedValue);
        }

        final JSONArray jsonValues = JSONConverter.getParameterFromJSON(json, VALUES, JSONArray.class);
        this.values = new ArrayList<LightUIComboBoxElement>();
        if (jsonValues != null) {
            for (final Object jsonValue : jsonValues) {
                this.values.add(new LightUIComboBoxElement(JSONConverter.getObjectFromJSON(jsonValue, JSONObject.class)));
            }
        }
    }

    @Override
    public Object getValueForContext() {
        if (this.hasSelectedValue()) {
            return this.getSelectedValue();
        } else {
            return null;
        }
    }

    @Override
    public void _setValueFromContext(Object value) {
        if (value != null) {
            final JSONObject jsonSelectedValue = JSONConverter.getObjectFromJSON(value, JSONObject.class);
            this.selectedValue = new LightUIComboBoxElement(jsonSelectedValue);
        } else {
            this.selectedValue = null;
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        this.clearValues();
    }
}
