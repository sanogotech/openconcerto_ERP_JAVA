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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * A set of table aliases, eg {OBSERVATION, OBSERVATION obs, ARTICLE art2}.
 * 
 * @author Sylvain CUAZ
 */
class AliasedTables {

    private final Map<String, TableRef> tables;
    private DBSystemRoot sysRoot;

    // not public or we would need to check coherence between parameters
    private AliasedTables(Map<String, TableRef> m, DBSystemRoot sysRoot) {
        this.tables = new LinkedHashMap<String, TableRef>(m);
        this.sysRoot = sysRoot;
    }

    public AliasedTables() {
        this((DBSystemRoot) null);
    }

    public AliasedTables(final DBSystemRoot sysRoot) {
        this(Collections.<String, TableRef> emptyMap(), sysRoot);
    }

    AliasedTables(AliasedTables at) {
        this(at.tables, at.sysRoot);
    }

    /**
     * Adds a new declaration if not already present.
     * 
     * @param table the table to add, e.g. /OBSERVATION/.
     * @param mustBeNew <code>true</code> if table cannot already be present.
     * @return <code>true</code> if the table was new, <code>false</code> if the alias was already
     *         present.
     * @throws IllegalArgumentException if the alias was already used for another table or
     *         <code>mustBeNew</code> is <code>true</code> and the alias was already used (even for
     *         the same table).
     */
    public boolean add(TableRef table, final boolean mustBeNew) throws IllegalArgumentException {
        final boolean nullSysRoot = this.sysRoot == null;
        if (!nullSysRoot && this.sysRoot != table.getTable().getDBSystemRoot())
            throw new IllegalArgumentException(table + " not in " + this.sysRoot);
        final String alias = table.getAlias();
        final boolean res;
        if (!this.contains(alias)) {
            res = true;
            this.tables.put(alias, table);
            if (nullSysRoot)
                this.sysRoot = table.getTable().getDBSystemRoot();
        } else if (this.getTable(alias) != table.getTable() || mustBeNew) {
            throw new IllegalArgumentException(table.getTable().getSQLName() + " can't be aliased to " + alias + " : " + this.getTable(alias).getSQLName() + " already is");
        } else {
            res = false;
        }

        return res;
    }

    public boolean remove(TableRef table) {
        final String alias = table.getAlias();
        final boolean res;
        if (this.contains(alias)) {
            this.tables.remove(alias);
            // don't unset sysRoot as it may have been set in the constructor
            res = true;
        } else {
            res = false;
        }
        return res;
    }

    public final DBSystemRoot getSysRoot() {
        return this.sysRoot;
    }

    final Map<String, TableRef> getMap() {
        return Collections.unmodifiableMap(this.tables);
    }

    public SQLTable getTable(String alias) {
        return getAliasedTable(alias).getTable();
    }

    /**
     * Return the alias for the passed table.
     * 
     * @param t a table.
     * @return the alias for <code>t</code>, or <code>null</code> if <code>t</code> is not exactly
     *         once in this.
     */
    public TableRef getAlias(SQLTable t) {
        return CollectionUtils.getSole(getAliases(t));
    }

    public List<TableRef> getAliases(SQLTable t) {
        final List<TableRef> res = new ArrayList<TableRef>();
        for (final TableRef at : this.tables.values())
            if (at.getTable().equals(t))
                res.add(at);
        return res;
    }

    public TableRef getAliasedTable(String alias) {
        return this.tables.get(alias);
    }

    public String getDeclaration(String alias) {
        return getAliasedTable(alias).getSQL();
    }

    public boolean contains(String alias) {
        return this.tables.containsKey(alias);
    }

    public LinkedHashSet<String> getAliases() {
        return new LinkedHashSet<String>(this.tables.keySet());
    }
}
