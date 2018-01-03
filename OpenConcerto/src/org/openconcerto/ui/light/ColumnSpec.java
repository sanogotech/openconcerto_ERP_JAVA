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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.jdom2.Element;

import net.minidev.json.JSONObject;

public class ColumnSpec implements Externalizable, Transferable {
    private static final String DEFAULT_VALUE = "default-value";
    private static final String EDITORS = "editors";
    private static final String VALUE_CLASS = "value-class";
    private static final String ID = "id";
    private static final String COLUMN_NAME = "column-name";
    private static final String WIDTH = "width";
    private static final String MAX_WIDTH = "max-width";
    private static final String MIN_WIDTH = "min-width";
    private static final String EDITABLE = "editable";
    private static final String HORIZONTAL_ALIGNMENT = "h-align";
    // Must stay immutable
    private String id;
    private String columnName;

    private Class<?> valueClass;

    // Default value (to add a new line)
    private Object defaultValue;

    private Integer horizontalAlignment = null;

    private double width;
    private double maxWidth;
    private double minWidth;

    private boolean editable;

    private LightUIElement editors;

    public ColumnSpec() {
        // Serialization
    }

    public ColumnSpec(final JSONObject json) {
        this.fromJSON(json);
    }

    public ColumnSpec(final String id, final Class<?> valueClass, final String columnName, final Object defaultValue, final double width, final boolean editable, final LightUIElement editors) {
        this.init(id, valueClass, columnName, defaultValue, editable, editors);
        this.width = width;

        final double minWidth = width - 200;
        final double maxWidth = width + 200;

        this.minWidth = (minWidth < 10) ? 10 : minWidth;
        this.maxWidth = maxWidth;
    }

    public ColumnSpec(final String id, final Class<?> valueClass, final String columnName, final Object defaultValue, final boolean editable, final LightUIElement editors) {
        this.init(id, valueClass, columnName, defaultValue, editable, editors);
        this.setDefaultPrefs();
    }

    private void init(final String id, final Class<?> valueClass, final String columnName, final Object defaultValue, final boolean editable, final LightUIElement editors) {
        this.id = id;
        this.valueClass = valueClass;
        this.columnName = columnName;
        this.defaultValue = defaultValue;
        this.editable = editable;
        this.editors = editors;
    }

    public void setPrefs(final double width, final double maxWidth, final double minWidth) {
        this.width = width;
        this.maxWidth = maxWidth;
        this.minWidth = minWidth;
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Class<?> getValueClass() {
        return this.valueClass;
    }

    public void setValueClass(Class<?> valueClass) {
        this.valueClass = valueClass;
    }

    public int getHorizontalAlignment() {
        return this.horizontalAlignment;
    }

    public void setHorizontalAlignment(final int horizontalAlignment) {
        this.horizontalAlignment = horizontalAlignment;
    }

    public String getColumnName() {
        return this.columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public Object getDefaultValue() {
        return this.defaultValue;
    }

    public void setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    public double getMaxWidth() {
        return this.maxWidth;
    }

    public void setMaxWidth(final double maxWidth) {
        this.maxWidth = maxWidth;
    }

    public double getMinWidth() {
        return this.minWidth;
    }

    public void setMinWidth(final double minWidth) {
        this.minWidth = minWidth;
    }

    public double getWidth() {
        return this.width;
    }

    public void setWidth(double width) {
        this.width = width;
    }

    public boolean isEditable() {
        return this.editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    public LightUIElement getEditor() {
        return this.editors;
    }

    public void setEditors(LightUIElement editors) {
        this.editors = editors;
    }

    private void setDefaultPrefs() {
        // TODO : Faire varier en fonction du type;
        this.width = 200;
        this.maxWidth = 500;
        this.minWidth = 50;
    }

    public Element createXmlColumnPref() {
        final Element columnElement = new Element("column");
        columnElement.setAttribute(ID, this.getId());
        columnElement.setAttribute(MAX_WIDTH, String.valueOf(this.getMaxWidth()));
        columnElement.setAttribute(MIN_WIDTH, String.valueOf(this.getMinWidth()));
        columnElement.setAttribute(WIDTH, String.valueOf(this.getWidth()));
        return columnElement;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeUTF(this.id);
        out.writeUTF(this.columnName);
        out.writeDouble(this.width);
        out.writeDouble(this.maxWidth);
        out.writeDouble(this.minWidth);
        out.writeObject(this.defaultValue);
        out.writeBoolean(this.editable);
        out.writeObject(this.editors);
        out.writeObject(this.valueClass);
        out.writeInt(this.horizontalAlignment);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        this.id = in.readUTF();
        this.columnName = in.readUTF();
        this.width = in.readDouble();
        this.maxWidth = in.readDouble();
        this.minWidth = in.readDouble();
        this.defaultValue = in.readObject();
        this.editable = in.readBoolean();
        this.editors = (LightUIElement) in.readObject();
        this.valueClass = (Class<?>) in.readObject();
        this.horizontalAlignment = in.readInt();
    }

    @Override
    public JSONObject toJSON() {
        final JSONObject result = new JSONObject();
        result.put("class", "ColumnSpec");
        result.put(ID, this.id);
        result.put(COLUMN_NAME, this.columnName);
        result.put(WIDTH, this.width);
        result.put(MAX_WIDTH, this.maxWidth);
        result.put(MIN_WIDTH, this.minWidth);
        if (this.defaultValue != null) {
            result.put(DEFAULT_VALUE, JSONConverter.getJSON(this.defaultValue));
        }
        if (this.editable) {
            result.put(EDITABLE, true);
        }
        if (this.editors != null) {
            result.put(EDITORS, JSONConverter.getJSON(this.editors));
        }
        result.put(VALUE_CLASS, JSONConverter.getJSON(this.valueClass));

        if (this.horizontalAlignment != null) {
            result.put(HORIZONTAL_ALIGNMENT, this.horizontalAlignment);
        }
        return result;
    }

    @Override
    public void fromJSON(final JSONObject json) {
        this.id = JSONConverter.getParameterFromJSON(json, ID, String.class);
        this.columnName = JSONConverter.getParameterFromJSON(json, COLUMN_NAME, String.class);

        /** JavaScript convert Double to Long, this will fix that. **/
        final Number numWidth = JSONConverter.getParameterFromJSON(json, WIDTH, Number.class);
        final Number numMaxWidth = JSONConverter.getParameterFromJSON(json, MAX_WIDTH, Number.class);
        final Number numMinWidth = JSONConverter.getParameterFromJSON(json, MIN_WIDTH, Number.class);
        if (numWidth != null) {
            this.width = numWidth.doubleValue();
        }
        if (numMaxWidth != null) {
            this.maxWidth = numMaxWidth.doubleValue();
        }
        if (numMinWidth != null) {
            this.minWidth = numMinWidth.doubleValue();
        }
        /************************************************************/

        this.editable = JSONConverter.getParameterFromJSON(json, EDITABLE, Boolean.class, Boolean.FALSE);
        this.horizontalAlignment = JSONConverter.getParameterFromJSON(json, HORIZONTAL_ALIGNMENT, Integer.class);

        final JSONObject jsonDefaultValue = JSONConverter.getParameterFromJSON(json, DEFAULT_VALUE, JSONObject.class);
        // TODO: implement default value

        final JSONObject jsonEditors = JSONConverter.getParameterFromJSON(json, EDITORS, JSONObject.class);
        if (jsonEditors != null) {
            this.editors = JSONToLightUIConvertorManager.getInstance().createUIElementFromJSON(jsonEditors);
        }

        final String sValueClass = JSONConverter.getParameterFromJSON(json, VALUE_CLASS, String.class);
        if (sValueClass != null) {
            try {
                this.valueClass = Class.forName(sValueClass);
            } catch (Exception ex) {
                throw new IllegalArgumentException("invalid value for 'value-class', " + ex.getMessage());
            }
        }
    }
}
