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

public class LightUIButtonWithSelectionContext extends LightUIButton {

    RowSelectionSpec tableSelection;

    public LightUIButtonWithSelectionContext(final JSONObject json) {
        super(json);
    }

    public LightUIButtonWithSelectionContext(final String id, final RowSelectionSpec tableSelection) {
        super(id, LightUIElement.TYPE_BUTTON_WITH_SELECTION_CONTEXT);
        this.tableSelection = tableSelection;
    }

    public LightUIButtonWithSelectionContext(final LightUIButtonWithSelectionContext button) {
        super(button);
    }

    public RowSelectionSpec getTableSelection() {
        return this.tableSelection;
    }

    @Override
    protected void copy(final LightUIElement element) {
        super.copy(element);

        if (!(element instanceof LightUIButtonWithSelectionContext)) {
            throw new InvalidClassException(LightUIButtonWithSelectionContext.class.getName(), element.getClassName(), element.getId());
        }
        final LightUIButtonWithSelectionContext button = (LightUIButtonWithSelectionContext) element;
        this.tableSelection = button.tableSelection;
    }

    @Override
    public void fromJSON(final JSONObject json) {
        super.fromJSON(json);

        this.tableSelection = (RowSelectionSpec) JSONConverter.getParameterFromJSON(json, "table-selection", RowSelectionSpec.class);
    }

    @Override
    public JSONObject toJSON() {
        final JSONObject json = super.toJSON();
        json.put("table-selection", this.tableSelection.toJSON());
        return json;
    }
}
