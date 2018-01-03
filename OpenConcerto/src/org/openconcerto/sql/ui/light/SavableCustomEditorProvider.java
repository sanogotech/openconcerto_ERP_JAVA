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
 
 package org.openconcerto.sql.ui.light;

import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.ui.light.CustomEditorProvider;
import org.openconcerto.ui.light.LightUIElement;

public abstract class SavableCustomEditorProvider extends CustomEditorProvider {

    public final void save(final SQLRowValues sqlRow, final SQLField sqlField, final LightUIElement uiElement) throws IllegalArgumentException, IllegalStateException {
        if (sqlRow == null || sqlField == null) {
            throw new IllegalStateException("Impossible to save this editor: " + uiElement.getId());
        }
        this._save(sqlRow, sqlField, uiElement);
    }

    protected abstract void _save(final SQLRowValues sqlRow, final SQLField sqlField, final LightUIElement uiElement) throws IllegalArgumentException, IllegalStateException;
}
