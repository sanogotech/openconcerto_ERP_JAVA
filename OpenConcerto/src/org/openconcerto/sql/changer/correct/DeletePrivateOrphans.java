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
 
 package org.openconcerto.sql.changer.correct;

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.changer.Changer;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.element.SQLElementLink;
import org.openconcerto.sql.model.DBSystemRoot;
import org.openconcerto.sql.model.IResultSetHandler;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLDataSource;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLName;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLSelect.ArchiveMode;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.utils.SQLCreateTable;
import org.openconcerto.sql.utils.SQLUtils;
import org.openconcerto.sql.utils.SQLUtils.SQLFactory;

import java.sql.SQLException;
import java.util.Collection;

/**
 * Delete unreferenced private rows.
 * 
 * @author Sylvain
 */
public class DeletePrivateOrphans extends Changer<SQLTable> {

    private boolean deleteAll;

    public DeletePrivateOrphans(DBSystemRoot b) {
        super(b);
        this.setDeleteAll(true);
    }

    @Override
    public void setUpFromSystemProperties() {
        super.setUpFromSystemProperties();
        this.setDeleteAll(!Boolean.getBoolean("deleteOnlyUnarchived"));
    }

    public void setDeleteAll(boolean deleteArchived) {
        this.deleteAll = deleteArchived;
    }

    public boolean getDeleteAll() {
        return this.deleteAll;
    }

    @Override
    protected void changeImpl(final SQLTable t) throws SQLException {
        getStream().print(t);
        if (Configuration.getInstance() == null || Configuration.getInstance().getDirectory() == null)
            throw new IllegalStateException("no directory");
        final SQLElement elem = Configuration.getInstance().getDirectory().getElement(t);
        if (elem == null) {
            getStream().println(" : no element");
            return;
        }

        if (!elem.isPrivate()) {
            getStream().println(" : not a private table");
            return;
        }
        if (elem.getParentLink() != null)
            throw new IllegalStateException("Private with a parent : " + elem.getParentLink());

        getStream().println("... ");

        SQLUtils.executeAtomic(getDS(), new SQLFactory<Object>() {
            @Override
            public Object create() throws SQLException {
                final SQLCreateTable createTable = new SQLCreateTable(t.getDBRoot(), "TO_DELETE_IDS");
                // don't use SQLTable since refreshing takes a lot of time
                // also we can thus use a temporary table
                createTable.setTemporary(true);
                createTable.setPlain(true);
                final String pkName = "ID";
                createTable.addColumn(pkName, t.getKey());
                createTable.setPrimaryKey(t.getKey().getName());
                getDS().execute(createTable.asString());
                // temporary table shouldn't be prefixed
                final SQLName toDeleteIDsName = new SQLName(createTable.getName());

                final SQLSelect selAllIDs = new SQLSelect(true).addSelect(t.getKey());
                // don't delete undefined
                selAllIDs.setExcludeUndefined(true);
                selAllIDs.setArchivedPolicy(getDeleteAll() ? ArchiveMode.BOTH : ArchiveMode.UNARCHIVED);
                getDS().execute("INSERT INTO " + toDeleteIDsName.quote() + " " + selAllIDs.asString());
                final long total = getCount(toDeleteIDsName);

                if (total == 0) {
                    getStream().println("nothing to delete");
                } else {
                    // delete all used IDs
                    for (final SQLElementLink privateLink : elem.getContainerLinks(true, false).getByPath().values()) {
                        assert privateLink.getStepToChild().getTo() == t;
                        final SQLField pp = privateLink.getStepToChild().getSingleField();
                        getDS().execute(t.getBase().quote("DELETE from %i where %i in ( " + new SQLSelect(true).addSelect(pp).asString() + ")", toDeleteIDsName, pkName));
                    }
                    // delete unused rows
                    getStream().println("archiving " + getCount(toDeleteIDsName) + " / " + total);
                    if (!isDryRun()) {
                        // archive join rows and cut private associations
                        @SuppressWarnings("unchecked")
                        final Collection<? extends Number> ids = getDS().executeCol("select " + SQLBase.quoteIdentifier(pkName) + " from " + toDeleteIDsName.quote());
                        elem.archiveIDs(ids);
                    }
                }
                getDS().execute("DROP TABLE " + toDeleteIDsName.quote());

                return null;
            }
        });

        getStream().println(t + " done");
    }

    private final long getCount(final SQLName toDeleteIDsName) {
        // since we don't use SQLTable.fire() don't use the cache
        return ((Number) getDS().execute("SELECT count(*) from " + toDeleteIDsName.quote(), new IResultSetHandler(SQLDataSource.SCALAR_HANDLER, false))).longValue();
    }
}
