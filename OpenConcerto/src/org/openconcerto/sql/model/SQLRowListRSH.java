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

import org.openconcerto.sql.model.SQLSelect.LockStrength;
import org.openconcerto.utils.Tuple2;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.dbutils.ResultSetHandler;

public final class SQLRowListRSH implements ResultSetHandler {

    // hashCode()/equals() needed for data source cache
    public static final class RSH implements ResultSetHandler {
        private final Tuple2<SQLTable, List<String>> names;

        // allow to create rows from arbitrary columns (and not just directly from actual fields of
        // the same table)
        // ATTN doesn't check that the types of columns are coherent with the types of the fields
        public RSH(final SQLTable t, final List<String> names) {
            this(Tuple2.create(t, names));
            if (!t.getFieldsName().containsAll(names))
                throw new IllegalArgumentException("Not all names are fields of " + t + " : " + names);
        }

        private RSH(Tuple2<SQLTable, List<String>> names) {
            this.names = names;
        }

        @Override
        public List<SQLRow> handle(ResultSet rs) throws SQLException {
            // since the result will be cached, disallow its modification (e.g.avoid
            // ConcurrentModificationException)
            return Collections.unmodifiableList(SQLRow.createListFromRS(this.names.get0(), rs, this.names.get1()));
        }

        @Override
        public int hashCode() {
            return this.names.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            final RSH other = (RSH) obj;
            return this.names.equals(other.names);
        }
    }

    private static TableRef checkTable(final TableRef t) {
        if (t == null)
            throw new IllegalArgumentException("null table");
        if (!t.getTable().isRowable())
            throw new IllegalArgumentException("table isn't rowable : " + t);
        return t;
    }

    static Tuple2<SQLTable, List<String>> getIndexes(SQLSelect sel, final TableRef passedTable, final boolean findTable) {
        final List<FieldRef> selectFields = sel.getSelectFields();
        final int size = selectFields.size();
        if (size == 0)
            throw new IllegalArgumentException("empty select : " + sel);
        TableRef t;
        if (findTable) {
            if (passedTable != null)
                throw new IllegalArgumentException("non null table " + passedTable);
            t = null;
        } else {
            t = checkTable(passedTable);
        }
        final List<String> l = new ArrayList<String>(size);
        for (int i = 0; i < size; i++) {
            final FieldRef field = selectFields.get(i);
            if (field == null) {
                // computed field
                l.add(null);
            } else {
                if (t == null) {
                    assert findTable;
                    t = checkTable(field.getTableRef());
                }
                assert t != null && t.getTable().isRowable();

                if (field.getTableRef().equals(t)) {
                    l.add(field.getField().getName());
                } else if (findTable) {
                    // prevent ambiguity : either specify a table or there must be only one table
                    throw new IllegalArgumentException(field + " is not in " + t);
                } else {
                    l.add(null);
                }
            }
        }
        return Tuple2.create(t.getTable(), l);
    }

    /**
     * Create a handler that don't need metadata.
     * 
     * @param sel the select that will produce the result set, must only have one table.
     * @return a handler creating a list of {@link SQLRow}.
     * @deprecated use {@link SQLSelectHandlerBuilder}
     */
    static public ResultSetHandler createFromSelect(final SQLSelect sel) {
        return create(getIndexes(sel, null, true));
    }

    /**
     * Create a handler that don't need metadata. Useful since some JDBC drivers perform queries for
     * each metadata.
     * 
     * @param sel the select that will produce the result set.
     * @param t the table for which to create rows.
     * @return a handler creating a list of {@link SQLRow}.
     * @deprecated use {@link SQLSelectHandlerBuilder}
     */
    static public ResultSetHandler createFromSelect(final SQLSelect sel, final TableRef t) {
        return create(getIndexes(sel, t, false));
    }

    static ResultSetHandler create(final Tuple2<SQLTable, List<String>> names) {
        return new RSH(names);
    }

    /**
     * Execute the passed select and return rows. NOTE if there's more than one table in the query
     * {@link #execute(SQLSelect, TableRef)} must be used.
     * 
     * @param sel the query to execute.
     * @return rows.
     * @throws IllegalArgumentException if there's more than one table in the query.
     */
    static public List<SQLRow> execute(final SQLSelect sel) throws IllegalArgumentException {
        return execute(sel, true, true);
    }

    static public List<SQLRow> execute(final SQLSelect sel, final boolean readCache, final boolean writeCache) {
        return new SQLSelectHandlerBuilder(sel).setReadCache(readCache).setWriteCache(writeCache).execute();
    }

    /**
     * Execute the passed select and return rows of <code>t</code>. NOTE if there's only one table
     * in the query {@link #execute(SQLSelect)} should be used.
     * 
     * @param sel the query to execute.
     * @param t the table to use.
     * @return rows of <code>t</code>.
     * @throws NullPointerException if <code>t</code> is <code>null</code>.
     */
    static public List<SQLRow> execute(final SQLSelect sel, final TableRef t) throws NullPointerException {
        return new SQLSelectHandlerBuilder(sel).setTableRef(t).execute();
    }

    static IResultSetHandler createFromSelect(final SQLSelect sel, final Tuple2<SQLTable, List<String>> indexes, final boolean readCache, final boolean writeCache) {
        final Set<SQLTable> tables = new HashSet<SQLTable>();
        // not just tables of the fields of the SELECT clause since inner joins can change the rows
        // returned
        for (final TableRef ref : sel.getTableRefs().values()) {
            tables.add(ref.getTable());
        }

        // the SELECT requesting a lock means the caller expects the DB to be accessed
        final boolean acquireLock = sel.getLockStrength() != LockStrength.NONE;
        return new IResultSetHandler(create(indexes), readCache && !acquireLock, writeCache && !acquireLock) {
            @Override
            public Set<? extends SQLData> getCacheModifiers() {
                return tables;
            }
        };
    }

    private final SQLTable t;
    private final boolean tableOnly;

    public SQLRowListRSH(SQLTable t) {
        this(t, false);
    }

    public SQLRowListRSH(SQLTable t, final boolean tableOnly) {
        super();
        this.t = t;
        this.tableOnly = tableOnly;
    }

    public Object handle(ResultSet rs) throws SQLException {
        return SQLRow.createListFromRS(this.t, rs, this.tableOnly);
    }
}
