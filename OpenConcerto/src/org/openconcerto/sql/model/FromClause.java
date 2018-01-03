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

import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.cc.ITransformer;

import java.util.ArrayList;
import java.util.List;

/**
 * The FROM clause of an SQLSelect, eg "FROM OBSERVATION O JOIN TENSION T on O.ID_TENSION = T.ID".
 * Ignores already added tables.
 * 
 * @author Sylvain CUAZ
 */
class FromClause implements SQLItem {

    private static final SQLItem COMMA = new SQLItem() {
        @Override
        public String getSQL() {
            return ", ";
        }
    };
    private static final SQLItem NEWLINE = new SQLItem() {
        @Override
        public String getSQL() {
            return "\n";
        }
    };

    private final List<SQLItem> sql;

    public FromClause() {
        this.sql = new ArrayList<SQLItem>();
    }

    public FromClause(FromClause f) {
        this();
        this.sql.addAll(f.sql);
    }

    void add(TableRef res) {
        if (!this.sql.isEmpty())
            this.sql.add(COMMA);
        this.sql.add(res);
    }

    void add(SQLSelectJoin j) {
        if (this.sql.isEmpty())
            throw new IllegalArgumentException("nothing to join with " + j);
        this.sql.add(NEWLINE);
        this.sql.add(j);
    }

    void remove(SQLSelectJoin j) {
        final int index = this.sql.indexOf(j);
        if (index >= 0) {
            final List<SQLItem> toClear = this.sql.subList(index - 1, index + 1);
            assert toClear.get(0) == NEWLINE && toClear.get(1).equals(j) && toClear.size() == 2;
            toClear.clear();
        }
    }

    @Override
    public String getSQL() {
        return "FROM " + CollectionUtils.join(this.sql, "", new ITransformer<SQLItem, String>() {
            @Override
            public String transformChecked(SQLItem input) {
                return input.getSQL();
            }
        });
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " <" + this.getSQL() + ">";
    }
}
