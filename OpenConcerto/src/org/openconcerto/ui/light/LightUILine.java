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

public class LightUILine extends LightUIContainer implements Transferable {
    private static final long serialVersionUID = 4132718509484530435L;

    public static final int ALIGN_GRID = 0;
    public static final int ALIGN_LEFT = 1;
    public static final int ALIGN_RIGHT = 2;

    private int gridAlignment = ALIGN_GRID;

    private Integer elementPadding;
    private Integer elementMargin;

    public LightUILine() {
        // Id will be set when this line will be added into a panel, or set manually.
        super("");
        this.setType(TYPE_PANEL_LINE);
    }

    public LightUILine(final JSONObject json) {
        super(json);
    }

    public LightUILine(final LightUILine line) {
        super(line);
    }

    public void setElementPadding(final Integer elementPadding) {
        this.elementPadding = elementPadding;
    }

    public Integer getElementPadding() {
        return this.elementPadding;
    }

    public void setElementMargin(final Integer elementMargin) {
        this.elementMargin = elementMargin;
    }

    public Integer getElementMargin() {
        return this.elementMargin;
    }

    public int getGridAlignment() {
        return this.gridAlignment;
    }

    public void setGridAlignment(final int gridAlignment) {
        this.gridAlignment = gridAlignment;
    }

    public Integer getTotalHeight() {
        Integer h = 0;
        final int size = this.getChildrenCount();
        for (int i = 0; i < size; i++) {
            h += this.getChild(i).getHeight();
        }
        return h;
    }

    public Integer getTotalGridWidth() {
        Integer w = 0;
        final int size = this.getChildrenCount();
        for (int i = 0; i < size; i++) {
            w += this.getChild(i).getGridWidth();
        }
        return w;
    }

    public static String createId(final LightUIPanel parent) {
        return parent.getId() + ".line." + parent.getChildrenCount();
    }

    @Override
    public LightUIElement clone() {
        return new LightUILine(this);
    }

    @Override
    protected void copy(LightUIElement element) {
        super.copy(element);

        if (!(element instanceof LightUILine)) {
            throw new InvalidClassException(this.getClassName(), element.getClassName(), element.getId());
        }

        final LightUILine line = (LightUILine) element;
        this.elementMargin = line.elementMargin;
        this.elementPadding = line.elementPadding;
        this.gridAlignment = line.gridAlignment;
    }

    @Override
    public JSONObject toJSON() {
        final JSONObject result = super.toJSON();
        result.put("class", "LightUILine");
        if (this.elementPadding != null) {
            result.put("element-padding", this.elementPadding);
        }
        if (this.elementMargin != null) {
            result.put("element-margin", this.elementMargin);
        }
        if (this.gridAlignment != ALIGN_GRID) {
            result.put("grid-alignment", this.gridAlignment);
        }
        return result;
    }

    @Override
    public void fromJSON(final JSONObject json) {
        super.fromJSON(json);
        this.elementPadding = (Integer) JSONConverter.getParameterFromJSON(json, "element-padding", Integer.class);
        this.elementMargin = (Integer) JSONConverter.getParameterFromJSON(json, "element-margin", Integer.class);
        this.gridAlignment = (Integer) JSONConverter.getParameterFromJSON(json, "grid-alignment", Integer.class, ALIGN_GRID);
    }
}
