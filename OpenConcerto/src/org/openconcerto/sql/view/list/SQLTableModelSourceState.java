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

import org.openconcerto.sql.request.ListSQLRequest;

import net.jcip.annotations.Immutable;

@Immutable
public final class SQLTableModelSourceState {

    private final ListSQLRequest req;
    private final SQLTableModelColumns allCols;

    protected SQLTableModelSourceState(final SQLTableModelColumns allCols, final ListSQLRequest req) {
        this.req = req.toUnmodifiable();
        this.allCols = allCols;
    }

    public final ListSQLRequest getReq() {
        return this.req;
    }

    public final SQLTableModelColumns getAllColumns() {
        return this.allCols;
    }
}
