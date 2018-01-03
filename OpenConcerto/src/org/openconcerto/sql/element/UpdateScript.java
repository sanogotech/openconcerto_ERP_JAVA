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

import org.openconcerto.sql.model.ConnectionHandlerNoSetup;
import org.openconcerto.sql.model.SQLDataSource;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLRowValuesCluster.StoreMode;
import org.openconcerto.sql.model.SQLRowValuesCluster.StoreResult;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.utils.SQLUtils;
import org.openconcerto.utils.SetMap;
import org.openconcerto.utils.Tuple2;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Apply updates to a row (including archiving obsolete privates).
 * 
 * @author Sylvain
 */
public final class UpdateScript {

    static public enum RowModifType {
        NONE(false, false), INSERTION(true, false), MODIFICATION(true, false), ARCHIVAL(false, true), UNARCHIVAL(true, false), DELETION(false, true);

        private final boolean rowWasStored, rowWasRemoved;

        private RowModifType(boolean rowWasStored, boolean rowWasRemoved) {
            assert !(rowWasStored && rowWasRemoved);
            this.rowWasStored = rowWasStored;
            this.rowWasRemoved = rowWasRemoved;
        }

        /**
         * Whether the modification updates the DB and the row is valid after this modification.
         * 
         * @return <code>true</code> if the row is valid.
         */
        public final boolean storesRow() {
            return this.rowWasStored;
        }

        /**
         * Whether the modification updates the DB and the row is no longer valid after this
         * modification.
         * 
         * @return <code>true</code> if the row is no longer valid.
         */
        public final boolean removesRow() {
            return this.rowWasRemoved;
        }
    }

    private final SQLElement elem;
    private final Map<SQLRowValues, SQLRowValues> target2source;
    private final SQLRowValues target;
    private final SQLRowValues updateRow;
    private final Map<SQLRowValues, SQLRowValues> map;
    private final SetMap<SQLElement, SQLRow> toArchive;
    private final SetMap<SQLTable, Number> toDelete;
    private StoreResult storeResult;

    /**
     * Create a new instance.
     * 
     * @param element the element.
     * @param source the instance passed to
     *        {@link SQLElement#update(SQLRowValues, SQLRowValues, boolean)}.
     * @param target the instance passed to update().
     */
    UpdateScript(final SQLElement element, final SQLRowValues source, final SQLRowValues target) {
        if (element.getTable() != target.getTable() || element.getTable() != source.getTable())
            throw new IllegalArgumentException("Table mismatch");
        this.elem = element;
        this.target = target;
        this.target2source = new IdentityHashMap<SQLRowValues, SQLRowValues>(8);
        this.target2source.put(target, source);
        /**
         * create new empty row so that we have the minimum rows to update, e.g.
         * 
         * <pre>
         * update
         *   A(name, age) <- join -> B        
         * to
         *   A(new name) <- join -> B
         * </pre>
         * 
         * : no need to update the join row, the graph to commit will only include A
         */
        this.updateRow = new SQLRowValues(target.getTable());
        // the map from the instances passed to update(), to the instances used to store the new
        // values into the DB.
        this.map = new IdentityHashMap<SQLRowValues, SQLRowValues>();
        this.map.put(target, this.getUpdateRow());
        this.toArchive = new SetMap<SQLElement, SQLRow>();
        this.toDelete = new SetMap<SQLTable, Number>(4);
        this.storeResult = null;
    }

    public final SQLElement getElement() {
        return this.elem;
    }

    public final boolean isDone() {
        return this.storeResult != null;
    }

    private void checkNotDone() {
        this.checkDone(false);
    }

    private void checkDone() {
        this.checkDone(true);
    }

    private void checkDone(final boolean done) {
        if (this.isDone() != done)
            throw new IllegalStateException(done ? "Not yet done" : "Already done");
    }

    final SQLRowValues getTarget() {
        return this.target;
    }

    final SQLRowValues getUpdateRow() {
        return this.updateRow;
    }

    final SQLRowValues getUpdateRow(final SQLRowValues v) {
        if (!this.getTarget().getGraph().getItems().contains(v))
            throw new IllegalArgumentException("The row wasn't passed to update()");
        return this.map.get(v);
    }

    final void addToArchive(SQLElement elem, SQLRowAccessor r) {
        checkNotDone();
        this.toArchive.add(elem, r.asRow());
    }

    public final Map<SQLElement, TreesOfSQLRows> getTreesToArchive() {
        final Map<SQLElement, TreesOfSQLRows> res = new HashMap<SQLElement, TreesOfSQLRows>();
        for (final Entry<SQLElement, Set<SQLRow>> e : this.toArchive.entrySet()) {
            final SQLElement elem = e.getKey();
            res.put(elem, new TreesOfSQLRows(elem, e.getValue()));
        }
        return res;
    }

    final void addToDelete(SQLRowAccessor r) {
        checkNotDone();
        if (!r.hasID())
            throw new IllegalArgumentException("Nothing to delete");
        this.toDelete.add(r.getTable(), r.getIDNumber());
    }

    final void put(String field, UpdateScript s) {
        this.getUpdateRow().put(field, s.getUpdateRow());
        this.add(s);
    }

    final void add(UpdateScript s) {
        assert s.getElement().isPrivate();
        checkNotDone();
        s.checkNotDone();
        checkGraphs(s.getTarget(), s.getUpdateRow());
        this.toArchive.merge(s.toArchive);
        this.toDelete.merge(s.toDelete);
        this.map.putAll(s.map);
        this.target2source.putAll(s.target2source);
    }

    private void checkGraphs(final SQLRowValues orig, final SQLRowValues v) {
        if (orig.getGraph() != this.getTarget().getGraph())
            throw new IllegalArgumentException("Origin row not in the same graph");
        if (v.getGraph() != this.getUpdateRow().getGraph())
            throw new IllegalArgumentException("Update row not in the same graph");
    }

    final void mapRow(final SQLRowValues orig, final SQLRowValues v) {
        checkNotDone();
        if (orig == null)
            throw new NullPointerException();
        checkGraphs(orig, v);
        final SQLRowValues prev = this.map.put(orig, v);
        assert prev == null;
    }

    /**
     * Return the source row that was matched to the passed row.
     * 
     * @param targetRow a row passed to {@link SQLElement#update(SQLRowValues, SQLRowValues)}.
     * @return the source row that is updated by {@link #exec()}, <code>null</code> if
     *         <code>targetRow</code> values are inserted by {@link #exec()}.
     */
    public final SQLRowValues getSource(final SQLRowValues targetRow) {
        if (targetRow.getGraph() != this.getTarget().getGraph())
            throw new IllegalArgumentException("The row wasn't passed to update()");
        return this.target2source.get(targetRow);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " " + this.getUpdateRow() + " toArchive: " + this.toArchive;
    }

    public final SQLRow exec() throws SQLException {
        checkNotDone();
        this.storeResult = SQLUtils.executeAtomic(this.getUpdateRow().getTable().getDBSystemRoot().getDataSource(), new ConnectionHandlerNoSetup<StoreResult, SQLException>() {
            @Override
            public StoreResult handle(SQLDataSource ds) throws SQLException {
                return _exec();
            }
        });
        checkDone();
        return this.storeResult.getStoredRow(this.getUpdateRow());
    }

    private final StoreResult _exec() throws SQLException {
        final StoreResult res = this.getUpdateRow().getGraph().store(StoreMode.COMMIT);
        for (final Entry<SQLElement, Set<SQLRow>> e : this.toArchive.entrySet()) {
            final SQLElement elem = e.getKey();
            elem.archive(e.getValue());
        }
        for (final Entry<SQLTable, Set<Number>> e : this.toDelete.entrySet()) {
            final SQLTable t = e.getKey();
            final Set<Number> ids = e.getValue();
            t.getDBSystemRoot().getDataSource().execute("DELETE FROM " + t.getSQLName() + " WHERE " + new Where(t.getKey(), ids));
            for (final Number id : ids)
                t.fireRowDeleted(id.intValue());
        }
        return res;
    }

    /**
     * Return the values in the DB where the passed argument was stored.
     * 
     * @param v a row passed to {@link SQLElement#update(SQLRowValues, SQLRowValues)}.
     * @return first the type of modification to the DB, second the row in the DB (<code>null</code>
     *         if {@link RowModifType#NONE no modification} was made).
     */
    public final Tuple2<RowModifType, SQLRowValues> getStoredValues(final SQLRowValues v) {
        checkDone();
        final SQLRowValues updateRow = this.getUpdateRow(v);
        final RowModifType res;
        if (updateRow == null) {
            res = RowModifType.NONE;
        } else if (updateRow.hasID()) {
            res = RowModifType.MODIFICATION;
        } else {
            res = RowModifType.INSERTION;
        }
        final SQLRowValues storedVals = res == RowModifType.NONE ? null : this.storeResult.getStoredValues(updateRow);
        assert (res == RowModifType.NONE) == (storedVals == null) : "Stored row not found";
        return Tuple2.create(res, storedVals);
    }

    public final RowModifType getModifType(final SQLRow r) {
        if (this.toDelete.getNonNull(r.getTable()).contains(r.getIDNumber()))
            return RowModifType.DELETION;
        else if (this.toArchive.get(this.getElement().getElement(r.getTable())).contains(r.asRow()))
            return RowModifType.ARCHIVAL;
        else
            return null;
    }
}
