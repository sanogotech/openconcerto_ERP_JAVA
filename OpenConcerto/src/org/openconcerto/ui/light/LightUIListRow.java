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

public class LightUIListRow extends LightUIPanel {
    private Number rowId;

    // Init from json constructor
    public LightUIListRow(final JSONObject json) {
        super(json);
    }

    // Clone constructor
    public LightUIListRow(final LightUIListRow listItem) {
        super(listItem);
        this.rowId = listItem.rowId;
    }

    public LightUIListRow(final LightUIList parent, final Number rowId) {
        super(parent.getId() + ".item.panel." + rowId.toString());
        this.rowId = rowId;
        this.setType(TYPE_LIST_ROW);
        this.setFillHeight(false);
    }

    public LightUIListRow(final LightUIList parent, final Number id, final String label) {
        this(parent, id);

        final LightUILine line = new LightUILine();
        line.addChild(new LightUILabel(this.getId() + ".label", label));
        this.addChild(line);
    }

    public Number getRowId() {
        return this.rowId;
    }

    public LightUIListRow clone() {
        return new LightUIListRow(this);
    }

    @Override
    public JSONObject toJSON() {
        final JSONObject json = super.toJSON();
        json.put("row-id", this.getRowId());
        return json;
    }

    @Override
    public void fromJSON(final JSONObject json) {
        super.fromJSON(json);
        this.rowId = JSONConverter.getParameterFromJSON(json, "row-id", Number.class);
    }
}
