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

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.view.list.ITableModel;
import org.openconcerto.ui.light.LightUIElement;

public class LightForeignRowValuesTableOnline extends LightRowValuesTableOnline {
    private SQLField foreignField;
    private Number parentRowId;

    public LightForeignRowValuesTableOnline(final Configuration configuration, final Number userId, final String id, final ITableModel model, final SQLField foreignField, final Number parentRowId) {
        super(configuration, userId, id, model);

        this.foreignField = foreignField;
        this.parentRowId = parentRowId;
    }

    public LightForeignRowValuesTableOnline(final LightForeignRowValuesTableOnline table) {
        super(table);

        this.foreignField = table.foreignField;
        this.parentRowId = table.parentRowId;
    }

    public final SQLField getForeignField() {
        return this.foreignField;
    }

    public final Number getParentRowId() {
        return this.parentRowId;
    }

    @Override
    public LightUIElement clone() {
        return new LightForeignRowValuesTableOnline(this);
    }
}
