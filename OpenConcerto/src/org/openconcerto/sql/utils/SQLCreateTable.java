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
 
 package org.openconcerto.sql.utils;

import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLSyntax;
import org.openconcerto.sql.model.SQLSystem;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.graph.SQLKey;

import java.util.Collections;
import java.util.List;

/**
 * Construct an CREATE TABLE statement with an ID, a field archive and order.
 * 
 * @author Sylvain
 */
public class SQLCreateTable extends SQLCreateTableBase<SQLCreateTable> {

    private final DBRoot b;
    private boolean createID, createOrder, createArchive;

    public SQLCreateTable(final DBRoot b, final String name) {
        super(b.getDBSystemRoot().getSyntax(), b.getName(), name);
        this.b = b;
        this.reset();
    }

    @Override
    public void reset() {
        super.reset();
        this.setPlain(false);
    }

    public final DBRoot getRoot() {
        return this.b;
    }

    /**
     * Whether ID, ARCHIVE and ORDER are added automatically.
     * 
     * @param b <code>true</code> if no clauses should automagically be added.
     */
    public final void setPlain(boolean b) {
        this.setCreateID(!b);
        this.setCreateOrder(!b);
        this.setCreateArchive(!b);
    }

    public final boolean isCreateID() {
        return this.createID;
    }

    public final void setCreateID(boolean createID) {
        this.createID = createID;
    }

    public final boolean isCreateOrder() {
        return this.createOrder;
    }

    public final void setCreateOrder(boolean createOrder) {
        this.createOrder = createOrder;
    }

    public final boolean isCreateArchive() {
        return this.createArchive;
    }

    public final void setCreateArchive(boolean createArchive) {
        this.createArchive = createArchive;
    }

    public SQLCreateTable addForeignColumn(String foreignTable) {
        return this.addForeignColumn(foreignTable, "");
    }

    public SQLCreateTable addForeignColumn(String foreignTableN, String suffix) {
        final String fk = SQLKey.PREFIX + foreignTableN + (suffix.length() == 0 ? "" : "_" + suffix);
        final SQLTable foreignTable = this.b.getTable(foreignTableN);
        if (foreignTable == null)
            throw new IllegalArgumentException("Unknown table in " + this.b + " : " + foreignTableN);
        return this.addForeignColumn(fk, foreignTable, true);
    }

    @Override
    public List<String> getPrimaryKey() {
        return !this.isCreateID() ? super.getPrimaryKey() : Collections.singletonList(SQLSyntax.ID_NAME);
    }

    @Override
    protected void checkPK() {
        if (this.isCreateID())
            throw new IllegalStateException("can only set primary key in plain mode, otherwise it is automatically added");
    }

    @Override
    protected void modifyClauses(final List<String> genClauses, final NameTransformer transf, final ClauseType type) {
        super.modifyClauses(genClauses, transf, type);
        if (type == ClauseType.ADD_COL) {
            if (this.isCreateID())
                genClauses.add(0, SQLBase.quoteIdentifier(SQLSyntax.ID_NAME) + this.getSyntax().getPrimaryIDDefinition());
            if (this.isCreateArchive())
                genClauses.add(SQLBase.quoteIdentifier(SQLSyntax.ARCHIVE_NAME) + this.getSyntax().getArchiveDefinition());
            if (this.isCreateOrder()) {
                // MS unique constraint is not standard so add it in modifyOutClauses()
                if (getSyntax().getSystem() == SQLSystem.MSSQL) {
                    genClauses.add(SQLBase.quoteIdentifier(SQLSyntax.ORDER_NAME) + this.getSyntax().getOrderType() + " DEFAULT " + this.getSyntax().getOrderDefault());
                } else {
                    genClauses.add(SQLBase.quoteIdentifier(SQLSyntax.ORDER_NAME) + this.getSyntax().getOrderDefinition());
                }
            }
        }
    }

    @Override
    protected void modifyOutClauses(final List<DeferredClause> clauses, final ClauseType type) {
        super.modifyOutClauses(clauses, type);
        if (type == ClauseType.ADD_CONSTRAINT && this.isCreateOrder() && getSyntax().getSystem() == SQLSystem.MSSQL) {
            clauses.add(this.createUniquePartialIndex("orderIdx", Collections.singletonList(SQLSyntax.ORDER_NAME), null));
        }
    }
}
