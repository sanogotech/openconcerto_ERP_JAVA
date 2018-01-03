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

import org.openconcerto.sql.model.FieldPath;
import org.openconcerto.sql.model.SQLField;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import net.jcip.annotations.Immutable;

@Immutable
public final class SQLTableModelColumns {

    static private final SQLTableModelColumns EMPTY = new SQLTableModelColumns();

    static public final SQLTableModelColumns empty() {
        return EMPTY;
    }

    private final List<SQLTableModelColumn> cols;
    private final List<SQLTableModelColumn> allCols;

    public SQLTableModelColumns(final List<SQLTableModelColumn> cols, final List<SQLTableModelColumn> debugCols) {
        this.cols = Collections.unmodifiableList(new ArrayList<SQLTableModelColumn>(cols));
        final List<SQLTableModelColumn> tmp = new ArrayList<SQLTableModelColumn>(cols.size() + debugCols.size());
        tmp.addAll(cols);
        tmp.addAll(debugCols);
        this.allCols = Collections.unmodifiableList(tmp);
    }

    private SQLTableModelColumns() {
        this.cols = Collections.emptyList();
        this.allCols = Collections.emptyList();
    }

    public final List<SQLTableModelColumn> getColumns() {
        return this.cols;
    }

    // start with getCols()
    public final List<SQLTableModelColumn> getAllColumns() {
        return this.allCols;
    }

    public int size() {
        return this.allCols.size();
    }

    /**
     * All the columns that depends on the passed field.
     * 
     * @param f the field.
     * @return all columns needing <code>f</code>.
     */
    public final List<SQLTableModelColumn> getColumns(SQLField f) {
        final List<SQLTableModelColumn> res = new ArrayList<SQLTableModelColumn>();
        for (final SQLTableModelColumn col : this.getColumns())
            if (col.getFields().contains(f))
                res.add(col);
        return res;
    }

    /**
     * The column depending solely on the passed field.
     * 
     * @param f the field.
     * @return the column needing only <code>f</code>.
     * @throws IllegalArgumentException if more than one column matches.
     */
    public final SQLTableModelColumn getColumn(SQLField f) {
        final Set<SQLField> singleton = Collections.singleton(f);
        SQLTableModelColumn res = null;
        for (final SQLTableModelColumn col : this.getColumns())
            if (col.getFields().equals(singleton)) {
                if (res == null)
                    res = col;
                else
                    throw new IllegalArgumentException("Not exactly one column for " + f);
            }
        return res;
    }

    /**
     * The column depending solely on the passed path.
     * 
     * @param fp the field path.
     * @return the column needing only <code>fp</code>.
     * @throws IllegalArgumentException if more than one column matches.
     */
    public final SQLTableModelColumn getColumn(FieldPath fp) {
        final Set<FieldPath> singleton = Collections.singleton(fp);
        SQLTableModelColumn res = null;
        for (final SQLTableModelColumn col : this.getColumns())
            if (col.getPaths().equals(singleton)) {
                if (res == null)
                    res = col;
                else
                    throw new IllegalArgumentException("Not exactly one column for " + fp);
            }
        return res;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        return prime + this.allCols.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final SQLTableModelColumns other = (SQLTableModelColumns) obj;
        return this.allCols.equals(other.allCols);
    }
}
