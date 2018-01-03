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
 
 package org.openconcerto.sql.element;

import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.graph.Link;
import org.openconcerto.sql.model.graph.Link.Direction;
import org.openconcerto.sql.model.graph.Path;
import org.openconcerto.sql.model.graph.PathBuilder;
import org.openconcerto.sql.model.graph.SQLKey;
import org.openconcerto.sql.utils.SQLCreateTable;
import org.openconcerto.utils.Tuple2.List2;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * An element which joins two tables and is never displayed.
 * 
 * @author Sylvain CUAZ
 */
public class JoinSQLElement extends ConfSQLElement {

    static public SQLTableBuilder createBuilder(final SQLTable ownerT, final SQLTable ownedT) {
        return new SQLTableBuilder(ownerT, ownedT);
    }

    static public Builder<?> createBuilder(final SQLCreateTable ownerT, final SQLCreateTable ownedT) {
        return new CreateTableBuilder(ownerT, ownedT);
    }

    static public final String getDefaultFKName(final String tableName) {
        return SQLKey.PREFIX + tableName;
    }

    static public final String getDefaultTableName(final String owner, final String owned) {
        return owner + '_' + owned;
    }

    static public abstract class Builder<T extends Builder<T>> {

        private String ownerFK, ownedFK;
        private String tableName;

        protected Builder() {
            assert thisAsT() == this;
        }

        protected abstract T thisAsT();

        protected abstract DBRoot getOwnerRoot();

        protected abstract String getOwnerTableName();

        protected abstract String getOwnedTableName();

        public final T setTableName(String tableName) {
            this.tableName = tableName == null ? getDefaultTableName(this.getOwnerTableName(), this.getOwnedTableName()) : tableName;
            return thisAsT();
        }

        public final String getTableName() {
            assert this.tableName != null : "setTableName(null) wasn't called";
            return this.tableName;
        }

        public final T setOwnerFK(final String ownerFK) {
            if (ownerFK != null && ownerFK.equals(this.ownedFK))
                throw new IllegalArgumentException("Same name : " + ownerFK);
            this.ownerFK = ownerFK;
            return thisAsT();
        }

        public final T setOwnedFK(final String ownedFK) {
            if (ownedFK != null && ownedFK.equals(this.ownerFK))
                throw new IllegalArgumentException("Same name : " + ownedFK);
            this.ownedFK = ownedFK;
            return thisAsT();
        }

        public final List2<String> getFKs() {
            String ownerFK = this.ownerFK == null ? getDefaultFKName(this.getOwnerTableName()) : this.ownerFK;
            String ownedFK = this.ownedFK == null ? getDefaultFKName(this.getOwnedTableName()) : this.ownedFK;
            if (ownerFK.equals(ownedFK)) {
                ownerFK += "_OWNER";
                ownedFK += "_OWNED";
            }
            return new List2<String>(ownerFK, ownedFK);
        }

        protected abstract void addForeignColumns(final SQLCreateTable res, final String toOwnerFK, final String toOwnedFK);

        public final SQLCreateTable getCreateTable() {
            final List2<String> fks = getFKs();
            final SQLCreateTable res = new SQLCreateTable(this.getOwnerRoot(), this.tableName);
            this.addForeignColumns(res, fks.get0(), fks.get1());
            return res;
        }

    }

    static public final class SQLTableBuilder extends Builder<SQLTableBuilder> {
        private final SQLTable ownerT, ownedT;

        private SQLTableBuilder(final SQLTable ownerT, final SQLTable ownedT) {
            this.ownerT = ownerT;
            this.ownedT = ownedT;
            this.setTableName(null);
        }

        @Override
        protected SQLTableBuilder thisAsT() {
            return this;
        }

        @Override
        protected String getOwnedTableName() {
            return this.ownedT.getName();
        }

        @Override
        protected String getOwnerTableName() {
            return this.ownerT.getName();
        }

        @Override
        protected DBRoot getOwnerRoot() {
            return this.ownedT.getDBRoot();
        }

        @Override
        protected void addForeignColumns(SQLCreateTable res, String toOwnerFK, String toOwnedFK) {
            res.addForeignColumn(toOwnerFK, this.ownerT);
            res.addForeignColumn(toOwnedFK, this.ownedT);
        }

        public final JoinSQLElement buildElement() throws SQLException {
            final SQLTable joinT = this.getOwnerRoot().createTable(getCreateTable());
            return new JoinSQLElement(joinT, getFKs().get0());
        }
    }

    static private final class CreateTableBuilder extends Builder<CreateTableBuilder> {
        private final SQLCreateTable ownerT, ownedT;

        private CreateTableBuilder(final SQLCreateTable ownerT, final SQLCreateTable ownedT) {
            this.ownerT = ownerT;
            this.ownedT = ownedT;
            this.setTableName(null);
        }

        @Override
        protected CreateTableBuilder thisAsT() {
            return this;
        }

        @Override
        protected String getOwnedTableName() {
            return this.ownedT.getName();
        }

        @Override
        protected String getOwnerTableName() {
            return this.ownerT.getName();
        }

        @Override
        protected DBRoot getOwnerRoot() {
            return this.ownedT.getRoot();
        }

        @Override
        protected void addForeignColumns(SQLCreateTable res, String toOwnerFK, String toOwnedFK) {
            res.addForeignColumn(toOwnerFK, this.ownerT);
            res.addForeignColumn(toOwnedFK, this.ownedT);
        }
    }

    private final List2<Link> links;
    private final Path p;

    public JoinSQLElement(SQLTable table, final String... fieldsToOwner) {
        this(table, table.getDBSystemRoot().getGraph().getForeignLink(table, Arrays.asList(fieldsToOwner)));
    }

    public JoinSQLElement(SQLTable table, final Link linkToOwner) {
        super(table);
        this.links = this.checkTable(linkToOwner);
        this.p = this.createPath();
    }

    private List2<Link> checkTable(final Link linkToOwner) {
        final List<Link> fks = new ArrayList<Link>(this.getTable().getDBSystemRoot().getGraph().getForeignLinks(getTable()));
        if (fks.size() != 2)
            throw new IllegalStateException("Not 2 foreign keys : " + fks);
        final int index = fks.indexOf(linkToOwner);
        if (index < 0)
            throw new IllegalArgumentException("Link " + linkToOwner + " not in " + fks);
        return new List2<Link>(linkToOwner, index == 0 ? fks.get(1) : fks.get(0));
    }

    public final Link getLinkToOwner() {
        return this.links.get0();
    }

    public final SQLTable getOwnerTable() {
        return this.getLinkToOwner().getTarget();
    }

    public final Link getLinkToOwned() {
        return this.links.get1();
    }

    public final SQLTable getOwnedTable() {
        return this.getLinkToOwned().getTarget();
    }

    public final Link getOppositeLink(final Link l) {
        final Link l1 = this.links.get0();
        final Link l2 = this.links.get1();
        if (l1.equals(l)) {
            return l2;
        } else if (l2.equals(l)) {
            return l1;
        } else {
            throw new IllegalArgumentException(l + " isn't in " + this.links);
        }
    }

    private Path createPath() {
        return new PathBuilder(getOwnerTable()).add(getLinkToOwner(), Direction.REFERENT).add(getLinkToOwned(), Direction.FOREIGN).build();
    }

    public final Path getPathFromOwner() {
        return this.p;
    }

    // define methods with final

    @Override
    public final boolean isPrivate() {
        return false;
    }

    @Override
    protected final String getParentFFName() {
        return super.getParentFFName();
    }

    @Override
    protected final void setupLinks(SQLElementLinksSetup links) {
        super.setupLinks(links);
    }

    @Override
    public final SQLComponent createComponent() {
        return null;
    }
}
