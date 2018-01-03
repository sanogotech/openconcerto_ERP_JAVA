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
 
 package org.openconcerto.sql.view.list;

import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.request.ListSQLRequest;

/**
 * A SQLTableModelSource *not* directly tied to the database.
 * 
 * @author Sylvain
 */
public class SQLTableModelSourceOffline extends SQLTableModelSource {

    public SQLTableModelSourceOffline(final ListSQLRequest req, final SQLElement elem) {
        super(req, elem);
    }

    @Override
    protected boolean allowBiggerGraph() {
        // MAYBE allow if all lines source have only committed rows (refresh them)
        return this.getLinesCount() == 0;
    }

    @Override
    protected SQLTableModelLinesSourceOffline _createLinesSource(final ITableModel model) {
        return new SQLTableModelLinesSourceOffline(this, model);
    }
}
