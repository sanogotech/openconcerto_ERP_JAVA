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
 * A SQLTableModelSource directly tied to the database. Any changes to its lines are propagated to
 * the database without any delay.
 * 
 * @author Sylvain
 */
public class SQLTableModelSourceOnline extends SQLTableModelSource {

    public SQLTableModelSourceOnline(final ListSQLRequest req, final SQLElement elem) {
        super(req, elem);
    }

    public SQLTableModelSourceOnline(SQLTableModelSourceOnline src) {
        super(src);
    }

    @Override
    protected boolean allowBiggerGraph() {
        return true;
    }

    @Override
    protected SQLTableModelLinesSourceOnline _createLinesSource(final ITableModel model) {
        return new SQLTableModelLinesSourceOnline(this, model);
    }
}
