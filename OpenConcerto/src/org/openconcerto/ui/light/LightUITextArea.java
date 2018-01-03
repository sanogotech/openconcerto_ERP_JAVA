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

public class LightUITextArea extends LightUserControl {

    private int nbLine = 4;

    public LightUITextArea(final String id) {
        super(id);
        this.setType(TYPE_TEXT_AREA);
        this.setValueType(LightUIElement.VALUE_TYPE_STRING);
    }

    public LightUITextArea(final JSONObject json) {
        super(json);
    }

    public LightUITextArea(final LightUITextArea text) {
        super(text);
    }

    public void setNbLine(int nbLine) {
        this.nbLine = nbLine;
    }

    public int getNbLine() {
        return this.nbLine;
    }

    @Override
    public JSONToLightUIConvertor getConvertor() {
        return new JSONToLightUIConvertor() {
            @Override
            public LightUIElement convert(final JSONObject json) {
                return new LightUITextArea(json);
            }
        };
    }

    @Override
    public LightUIElement clone() {
        return new LightUITextArea(this);
    }

    @Override
    protected void copy(final LightUIElement element) {
        super.copy(element);
        if (!(element instanceof LightUITextArea)) {
            throw new InvalidClassException(this.getClassName(), element.getClassName(), element.getId());
        }

        final LightUITextArea text = (LightUITextArea) element;
        this.nbLine = text.nbLine;
    }

    @Override
    public void _setValueFromContext(final Object value) {
        this.setValue((String) JSONConverter.getObjectFromJSON(value, String.class));
    }

    @Override
    public Object getValueForContext() {
        return (this.getValue() == null) ? null : ((this.getValue().trim().equals("")) ? null : this.getValue());
    }

    @Override
    public JSONObject toJSON() {
        final JSONObject json = super.toJSON();
        json.put("nbline", this.nbLine);
        return json;
    }

    @Override
    public void fromJSON(final JSONObject json) {
        super.fromJSON(json);
        final Integer jsonValues = (Integer) JSONConverter.getParameterFromJSON(json, "nbline", Integer.class);
        this.nbLine = jsonValues.intValue();
    }

}
