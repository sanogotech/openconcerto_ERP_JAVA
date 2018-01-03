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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.dbutils.ResultSetHandler;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.Immutable;

/**
 * <code>ResultSetHandler</code> implementation that converts one <code>ResultSet</code> column into
 * a {@link List}.
 */
@Immutable
public class ColumnListHandlerGeneric<T> implements ResultSetHandler {

    @GuardedBy("cache")
    static private final Map<Class<?>, ColumnListHandlerGeneric<?>> cache = new LinkedHashMap<Class<?>, ColumnListHandlerGeneric<?>>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Class<?>, ColumnListHandlerGeneric<?>> eldest) {
            return this.size() > 20;
        }
    };

    public static <T> ColumnListHandlerGeneric<T> create(Class<T> clz) {
        synchronized (cache) {
            @SuppressWarnings("unchecked")
            ColumnListHandlerGeneric<T> res = (ColumnListHandlerGeneric<T>) cache.get(clz);
            if (res == null) {
                res = create(1, clz);
                assert res != null;
                cache.put(clz, res);
            }
            return res;
        }
    }

    // syntactic sugar
    public static <T> ColumnListHandlerGeneric<T> create(int columnIndex, Class<T> clz) {
        return new ColumnListHandlerGeneric<T>(columnIndex, clz);
    }

    /**
     * The column number to retrieve.
     */
    private final int columnIndex;

    /**
     * The column name to retrieve. Either columnName or columnIndex will be used but never both.
     */
    private final String columnName;

    private final Class<T> clz;

    public ColumnListHandlerGeneric(int columnIndex, Class<T> clz) {
        this(columnIndex, null, clz);
    }

    /**
     * Creates a new instance of ColumnListHandler.
     * 
     * @param columnName The name of the column to retrieve from the <code>ResultSet</code>.
     */
    public ColumnListHandlerGeneric(String columnName, Class<T> clz) {
        this(-1, columnName, clz);
    }

    protected ColumnListHandlerGeneric(int columnIndex, String columnName, Class<T> clz) {
        final boolean noName = columnName == null;
        final boolean noIndex = columnIndex <= 0;
        if (noName && noIndex)
            throw new IllegalArgumentException("Missing column information");
        assert noName || noIndex : "A constructor passed more than one argument";
        this.columnIndex = columnIndex;
        this.columnName = columnName;
        this.clz = clz;
    }

    /**
     * Returns one <code>ResultSet</code> column as a <code>List</code>. The elements are added to
     * the <code>List</code> via {@link SQLResultSet#getValue(ResultSet, Class, int)}.
     * 
     * @return a <code>List</code>, never <code>null</code>.
     * @throws SQLException if one object couldn't be retrieved from the result set.
     */
    @Override
    public final List<T> handle(ResultSet rs) throws SQLException {
        final List<T> result = new ArrayList<T>();
        while (rs.next()) {
            if (this.columnName == null) {
                result.add(SQLResultSet.getValue(rs, this.clz, this.columnIndex));
            } else {
                result.add(SQLResultSet.getValue(rs, this.clz, this.columnName));
            }
        }
        return result;
    }
}
