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
 
 package org.openconcerto.sql.model;

import org.openconcerto.utils.Tuple2;
import org.openconcerto.utils.Value;

import java.util.List;

public final class SQLSelectHandlerBuilder {

    private final SQLSelect sel;
    private Value<TableRef> t;
    private boolean readCache, writeCache;

    public SQLSelectHandlerBuilder(final SQLSelect sel) {
        if (sel == null)
            throw new NullPointerException("Null query");
        this.sel = sel;
        this.t = Value.getNone();
        this.setUseCache(true);
    }

    public SQLSelectHandlerBuilder setUseCache(final boolean b) {
        this.setReadCache(b);
        this.setWriteCache(b);
        return this;
    }

    public SQLSelectHandlerBuilder setReadCache(final boolean b) {
        this.readCache = b;
        return this;
    }

    public final boolean isReadCache() {
        return this.readCache;
    }

    public SQLSelectHandlerBuilder setWriteCache(final boolean b) {
        this.writeCache = b;
        return this;
    }

    public final boolean isWriteCache() {
        return this.writeCache;
    }

    /**
     * Set the table of the rows to be created. Must be used if the query has more than one table.
     * 
     * @param t a table, not <code>null</code>.
     * @return this.
     * @throws NullPointerException if t is <code>null</code>.
     */
    public SQLSelectHandlerBuilder setTableRef(final TableRef t) throws NullPointerException {
        if (t == null)
            throw new NullPointerException("Null table");
        this.t = Value.getSome(t);
        return this;
    }

    public SQLSelectHandlerBuilder unsetTableRef() {
        this.t = Value.getNone();
        return this;
    }

    public IResultSetHandler createHandler() {
        final Tuple2<SQLTable, List<String>> indexes = SQLRowListRSH.getIndexes(this.sel, this.t.toNonNull(), !this.t.hasValue());
        return SQLRowListRSH.createFromSelect(this.sel, indexes, isReadCache(), isWriteCache());
    }

    @SuppressWarnings("unchecked")
    public List<SQLRow> execute() {
        return (List<SQLRow>) this.sel.getSystemRoot().getDataSource().execute(this.sel.asString(), createHandler());
    }
}
