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
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.view.list.ITableModel;
import org.openconcerto.sql.view.list.SQLTableModelLinesSourceOffline;
import org.openconcerto.ui.light.LightUIElement;
import org.openconcerto.ui.light.SearchSpec;
import org.openconcerto.ui.light.TableContent;

import java.util.concurrent.Future;

import net.minidev.json.JSONObject;

public class LightForeignRowValuesTableOffline extends LightRowValuesTable {

    private SQLField foreignField;
    private Number parentRowId;

    public LightForeignRowValuesTableOffline(final Configuration configuration, final Number userId, final String id, final ITableModel model, final SQLField foreignField, final Number parentRowId) {
        super(configuration, userId, id, model);

        this.foreignField = foreignField;
        this.parentRowId = parentRowId;
        this.init();
    }

    public LightForeignRowValuesTableOffline(final LightForeignRowValuesTableOffline table) {
        super(table);

        this.foreignField = table.foreignField;
        this.parentRowId = table.parentRowId;
        this.init();
    }

    private final void init() {
        if (this.getTableSpec().getContent() == null) {
            this.getTableSpec().setContent(new TableContent(this.getId()));
        }
    }

    public final SQLField getForeignField() {
        return this.foreignField;
    }

    public final Number getParentRowId() {
        return this.parentRowId;
    }

    public Future<?> commitRows() {
        return ((SQLTableModelLinesSourceOffline) this.getModel().getLinesSource()).commit();
    }

    public void addNewRow(final SQLRowValues sqlRow) {
        ((SQLTableModelLinesSourceOffline) this.getModel().getLinesSource()).add(sqlRow);
    }

    @Override
    public void doSearch(final Configuration configuration, final SearchSpec searchSpec, final int offset) {
        // TODO: Implement search in offline table
        this.getModel().fireTableRowsInserted(0, Integer.MAX_VALUE);
    }

    @Override
    public LightUIElement clone() {
        return new LightForeignRowValuesTableOffline(this);
    }

    @Override
    public void fromJSON(final JSONObject json) {
        super.fromJSON(json);

        if (this.getTableSpec().getContent() != null) {
            this.getTableSpec().getContent().clearRows();
        }
    }
}
