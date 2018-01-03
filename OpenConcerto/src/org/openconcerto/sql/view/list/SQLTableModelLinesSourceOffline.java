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

import org.openconcerto.sql.model.ConnectionHandlerNoSetup;
import org.openconcerto.sql.model.OrderComparator;
import org.openconcerto.sql.model.SQLDataSource;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable.VirtualFields;
import org.openconcerto.sql.model.graph.Path;
import org.openconcerto.sql.utils.SQLUtils;
import org.openconcerto.utils.CompareUtils;
import org.openconcerto.utils.NumberUtils;
import org.openconcerto.utils.SetMap;
import org.openconcerto.utils.Value;
import org.openconcerto.utils.cc.IClosure;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicReference;

import net.jcip.annotations.NotThreadSafe;

/**
 * Lines are stored in SQLRowValues and committed to the database on demand. Unless otherwise noted,
 * public methods are thread-safe, others are required to be called from the {@link UpdateQueue}.
 * 
 * @author Sylvain
 */
public class SQLTableModelLinesSourceOffline extends SQLTableModelLinesSource {

    // row container with equals() using reference equality
    @NotThreadSafe
    static private final class Row {
        private final Integer id;
        private SQLRowValues vals;

        private Row(Integer id, SQLRowValues vals) {
            super();
            this.id = id;
            this.setRow(vals);
        }

        public final Integer getID() {
            return this.id;
        }

        public final SQLRowValues getRow() {
            return this.vals;
        }

        public final void setRow(SQLRowValues newVals) {
            if (!newVals.isFrozen())
                throw new IllegalArgumentException("Not frozen : " + newVals);
            this.vals = newVals;
        }

        @Override
        public String toString() {
            return this.getClass().getSimpleName() + " of " + this.getID();
        }
    }

    static private abstract class OfflineCallable<V> implements Callable<V> {

    }

    static private abstract class OfflineRunnable extends OfflineCallable<Object> implements Runnable {
        @Override
        public final Object call() throws Exception {
            this.run();
            return null;
        }
    }

    // all mutable attributes are accessed from the UpdateQueue thread

    private final SQLTableModelSourceOffline parent;
    private final List<Row> lines;
    // since new lines have no database ID, give them a virtual one
    private int freeID;
    private final Map<Integer, Row> id2line;
    // values that can be modified, read-only
    private final SQLRowValues modifiableVals;
    // original value for modified lines
    private final Map<Row, SQLRowValues> dbVals;
    // removed lines
    private final Set<Number> deleted;

    private boolean dbOrder;

    {
        this.lines = new LinkedList<Row>();
        // the firsts are used in other part of the fwk
        this.freeID = SQLRow.MIN_VALID_ID - 10;
        this.id2line = new HashMap<Integer, Row>();
        this.dbOrder = true;
    }

    public SQLTableModelLinesSourceOffline(SQLTableModelSourceOffline parent, ITableModel model) {
        super(model);
        this.parent = parent;
        this.modifiableVals = this.getParent().getElem().getPrivateGraph().toImmutable();
        this.dbVals = new HashMap<Row, SQLRowValues>();
        this.deleted = new HashSet<Number>();
    }

    @Override
    public final SQLTableModelSourceOffline getParent() {
        return this.parent;
    }

    private synchronized boolean checkUpdateThread() {
        return this.getModel().getUpdateQ().currentlyInQueue();
    }

    private final Row getRow(Integer id) {
        return this.getRow(id, false);
    }

    private final Row getRow(Integer id, final boolean required) {
        return this.getRow(id, required, null);
    }

    private final Row getRow(Integer id, final boolean required, final Object idSource) {
        final Row res = this.id2line.get(id);
        if (required && res == null)
            throw new IllegalArgumentException("Not in the list : " + (idSource == null ? id : idSource));
        return res;
    }

    protected final int getSize() {
        return this.lines.size();
    }

    private final int indexOf(Row r) {
        return this.lines.indexOf(r);
    }

    protected final List<ListSQLLine> getLines() {
        final List<ListSQLLine> res = new ArrayList<ListSQLLine>();
        for (final Row r : this.lines) {
            final ListSQLLine l = createLine(r);
            if (l != null)
                res.add(l);
        }
        return res;
    }

    // if the user has moved rows, DB order can no longer be used
    private final boolean isDBOrder() {
        return this.dbOrder;
    }

    private final boolean setDBOrder(final boolean dbOrder) {
        assert checkUpdateThread();
        if (this.dbOrder != dbOrder) {
            this.dbOrder = dbOrder;
            return true;
        } else {
            return false;
        }
    }

    private final Number getOrder(final Row r) {
        if (this.isDBOrder())
            return null;
        else
            return this.indexOf(r);
    }

    private final ListSQLLine createLine(final Row r) {
        if (r == null)
            return null;
        final ListSQLLine res = this.createLine(r.vals, r.id);
        if (res != null)
            res.setOrder(getOrder(r));
        return res;
    }

    /**
     * Compare 2 lines. Only use state of the parameters, so this method is thread-safe.
     * 
     * @param l1 the first line.
     * @param l2 the second line.
     * @return a negative integer, zero, or a positive integer as this object is less than, equal
     *         to, or greater than the specified object.
     * @see Comparator#compare(Object, Object)
     */
    @Override
    public int compare(ListSQLLine l1, ListSQLLine l2) {
        if (l1 == l2)
            return 0;

        final Number order1, order2;
        final SQLTableModelLinesSource src = l1.getSrc();
        // avoid comparing in the middle of a reordering
        synchronized (src.getModel().getUpdateQ().getFullList()) {
            order1 = l1.getOrder();
            order2 = l2.getOrder();
        }
        if (order1 != null) {
            if (order2 == null)
                throw new IllegalStateException("Order mismatch :\n" + order1 + " for " + l1 + " not coherent with\n" + order2 + " for " + l2);
            return NumberUtils.compare(order1, order2);
        } else {
            if (order2 != null)
                throw new IllegalStateException("Order mismatch :\n" + order1 + " for " + l1 + " not coherent with\n" + order2 + " for " + l2);
            return OrderComparator.INSTANCE.compare(l1.getRow(), l2.getRow());
        }
    }

    protected final List<SQLRowValues> fetch() {
        return this.getUpdateQueueReq().getValues();
    }

    /**
     * Fetch all rows and update our lines. Must be called by the {@link UpdateQueue}. Deleted rows
     * will be removed, inserted rows added, virtual rows unchanged, and updated rows will only be
     * updated if unchanged.
     * 
     * @return the new lines.
     */
    @Override
    public List<ListSQLLine> getAll() {
        assert checkUpdateThread();
        final List<SQLRowValues> dbRows = this.fetch();

        if (this.lines.isEmpty()) {
            // optimization of the else block
            for (final SQLRowValues dbRow : dbRows) {
                this._add(dbRow, false, false);
            }
        } else {
            // delete
            final Set<Integer> dbIDs = new HashSet<Integer>();
            for (final SQLRowValues dbRow : dbRows) {
                dbIDs.add(dbRow.getIDNumber(true).intValue());
            }
            final Set<Integer> deletedIDs = new HashSet<Integer>(this.id2line.keySet());
            deletedIDs.removeAll(dbIDs);
            for (final Integer id : deletedIDs) {
                // don't delete virtual rows
                if (id.intValue() >= SQLRow.MIN_VALID_ID) {
                    final Value<ListSQLLine> val = this.updateRow(id.intValue(), null);
                    assert val.getValue() == null;
                }
            }
            // update/insert
            for (final SQLRowValues dbRow : dbRows) {
                this.updateRow(dbRow.getID(), dbRow);
            }
        }
        return this.getLines();
    }

    // row is null, if it was deleted
    private final Value<ListSQLLine> updateRow(final int id, final SQLRowValues row) {
        final Row existingLine = this.getRow(id);

        // if the row wasn't removed and we updated it, ignore new values
        if (row != null && existingLine != null && this.dbVals.containsKey(existingLine)) {
            // MAYBE warn if ignoring changes
            return Value.getNone();
        } else {
            final Row newRow;
            if (row == null) {
                // if this id is not part of us, rm from our list
                this._rm(existingLine);
                newRow = null;
            } else if (existingLine != null) {
                existingLine.setRow(row);
                newRow = existingLine;
            } else {
                // don't fire as the new line is returned from this method
                newRow = this._add(row, false, false);
            }
            return Value.getSome(this.createLine(newRow));
        }
    }

    /**
     * {@inheritDoc} Must be called by the {@link UpdateQueue}.
     */
    @Override
    public Value<ListSQLLine> get(final int id) {
        assert checkUpdateThread();
        return updateRow(id, this.getUpdateQueueReq().getValues(id));
    }

    // *** Modify virtual rows ***

    public final Future<Number> add(final SQLRowValues vals) {
        // since SQLRowValues isn't thread-safe, use AtomicReference to safely pass it to another
        // thread
        final AtomicReference<SQLRowValues> copy = new AtomicReference<SQLRowValues>(vals.deepCopy());
        return this.getModel().getUpdateQ().execute(new FutureTask<Number>(new OfflineCallable<Number>() {
            @Override
            public Number call() throws Exception {
                return _add(copy.get(), true, true).getID();
            }
        }));
    }

    protected Row _add(final SQLRowValues vals, final boolean grow, final boolean fireAdd) {
        assert checkUpdateThread();
        // make sure every needed path is there
        if (grow)
            vals.grow(getUpdateQueueReq().getGraphToFetch(), false);
        // ATTN only works because vals was just fetched or just copied
        vals.getGraph().freeze();
        final boolean fromDB = vals.hasID();
        final List<Integer> order;
        final Row r;
        r = new Row(fromDB ? vals.getID() : this.freeID--, vals);
        this.id2line.put(r.getID(), r);
        this.lines.add(r);

        if (!fromDB && this.setDBOrder(false)) {
            order = this.getIDsOrder();
            assert order != null;
        } else {
            order = null;
        }
        if (order != null) {
            this.getModel().getUpdateQ().reorder(order);
            // put a setList() in searchQ
        }
        if (fireAdd) {
            // even if the row is filtered (i.e. line is null), don't remove as the filter might
            // change afterwards
            final ListSQLLine line = createLine(r);
            this.getModel().getUpdateQ().replaceLine(r.getID().intValue(), line);
            // add the line in fullList
            // put a addList() in searchQ
        }
        return r;
    }

    public final Future<SQLRowValues> remove(final Number id) {
        return this.getModel().getUpdateQ().execute(new FutureTask<SQLRowValues>(new OfflineCallable<SQLRowValues>() {
            @Override
            public SQLRowValues call() throws Exception {
                final Row r = getRow(id.intValue());
                return r == null ? null : rm(r).vals;
            }
        }));
    }

    private Row rm(final Row r) {
        if (r != null) {
            this._rm(r);
            // add to a list of id to archive if it's in the DB
            if (r.vals.hasID())
                this.deleted.add(r.vals.getIDNumber());
            this.getModel().getUpdateQ().replaceLine(r.getID().intValue(), null);
        }
        return r;
    }

    private void _rm(final Row l) {
        assert checkUpdateThread();
        if (l != null) {
            this.lines.remove(l);
            this.id2line.remove(l.id);
            this.dbVals.remove(l);
        }
    }

    @Override
    public void commit(final ListSQLLine l, final Path path, final SQLRowValues vals) {
        checkCanModif(path);
        if (!vals.isFrozen())
            throw new IllegalArgumentException("Not frozen");
        // since SQLRowValues isn't thread-safe, use AtomicReference to safely pass it to another
        // thread
        final AtomicReference<SQLRowValues> copy = new AtomicReference<SQLRowValues>(vals);
        this.getModel().getUpdateQ().put(new OfflineRunnable() {
            @Override
            public void run() {
                getModel().getUpdateQ().updateLine(l, path, copy.get().getID(), copy.get());
                recordOriginal(l);
            }
        });
    }

    public Future<?> replaceRow(final Number id, final SQLRowValues vals) {
        checkCanModif(Path.get(vals.getTable()));
        final SQLRowValues copy = vals.deepCopy();
        return this.updateRow(id, new IClosure<SQLRowValues>() {
            @Override
            public void executeChecked(SQLRowValues newVals) {
                final Set<String> contentFields = newVals.getTable().getFieldsNames(VirtualFields.CONTENT);
                newVals.clearReferents().removeAll(contentFields);
                newVals.load(copy, contentFields);
                if (copy.hasReferents()) {
                    for (final Entry<SQLField, Set<SQLRowValues>> e : new SetMap<SQLField, SQLRowValues>(copy.getReferentsMap()).entrySet()) {
                        for (final SQLRowValues ref : e.getValue()) {
                            ref.put(e.getKey().getName(), newVals);
                        }
                    }
                }
                assert copy.getGraphSize() == 1;
            }
        }, false);
    }

    public Future<?> updateRow(final Number id, final SQLRowValues vals) {
        checkCanModif(Path.get(vals.getTable()));
        if (vals.getGraphSize() > 1)
            throw new IllegalArgumentException("This method doesn't merge graphs");
        final SQLRowValues copy = vals.deepCopy();
        return this.updateRow(id, new IClosure<SQLRowValues>() {
            @Override
            public void executeChecked(SQLRowValues newVals) {
                final Set<String> contentFields = newVals.getTable().getFieldsNames(VirtualFields.CONTENT);
                newVals.load(copy, contentFields);
            }
        }, false);
    }

    public Future<?> updateRow(final Number id, final IClosure<SQLRowValues> valsClosure) {
        return this.updateRow(id, valsClosure, true);
    }

    private Future<?> updateRow(final Number id, final IClosure<SQLRowValues> valsClosure, final boolean mdCanChange) {
        return this.getModel().getUpdateQ().put(new OfflineRunnable() {
            @Override
            public void run() {
                _updateRow(id, valsClosure, mdCanChange);
            }
        });
    }

    protected Row _updateRow(final Number id, final IClosure<SQLRowValues> valsClosure, final boolean mdCanChange) {
        assert checkUpdateThread();
        final Row r = getRow(id.intValue(), true);
        final SQLRowValues newVals = r.getRow().deepCopy();
        valsClosure.executeChecked(newVals);
        // make sure PK and metadata don't change
        if (mdCanChange) {
            final Set<String> notContent = newVals.getTable().getFieldsNames(VirtualFields.CONTENT.complement());
            newVals.removeAll(notContent);
            newVals.putAll(r.getRow().getValues(notContent, false));
        }
        assert CompareUtils.equals(r.getRow().getIDNumber(), newVals.getIDNumber());
        // make sure every needed path is there
        newVals.grow(getUpdateQueueReq().getGraphToFetch(), false);
        newVals.getGraph().freeze();
        setRow(r, newVals);

        // call createLine() to apply filter
        this.getModel().getUpdateQ().replaceLine(r.getID().intValue(), createLine(r));
        return r;
    }

    private void checkCanModif(Path path) {
        if (this.modifiableVals.followPath(path) == null)
            throw new IllegalArgumentException("can only modify " + this.modifiableVals);
    }

    private void recordOriginal(ListSQLLine l) {
        setRow(getRow(l.getID(), true), l.getRow());
    }

    private void setRow(Row r, SQLRowValues newVals) {
        // if the existing row isn't in the DB, no need to update, the new values will be inserted
        if (r.getRow().hasID() && !this.dbVals.containsKey(r)) {
            // copy the initial state
            this.dbVals.put(r, r.getRow());
        }
        r.setRow(newVals);
    }

    // *** Order ***

    @Override
    public Future<?> moveBy(final List<? extends SQLRowAccessor> list, final int inc) {
        if (inc == 0 || list.size() == 0)
            return null;

        // since SQLRowValues isn't thread-safe, use Concurrent Collection to safely pass it to
        // another thread
        final List<SQLRowAccessor> copy = new CopyOnWriteArrayList<SQLRowAccessor>(list);
        return this.getModel().getUpdateQ().put(new OfflineRunnable() {
            @Override
            public void run() {
                _moveBy(copy, inc);
            }
        });
    }

    protected void _moveBy(final List<? extends SQLRowAccessor> list, final int inc) {
        assert checkUpdateThread();
        final int count = this.lines.size();
        final boolean after = inc > 0;

        final List<Integer> order;
        // same algorithm as MoveQueue
        int outerIndex = -1;
        final List<Row> ourLines = new ArrayList<Row>(list.size());
        for (final SQLRowAccessor r : list) {
            final Row ourLine = this.getRow(r.getID(), true, r);
            final int index = this.indexOf(ourLine);
            ourLines.add(ourLine);
            if (outerIndex < 0 || after && index > outerIndex || !after && index < outerIndex) {
                outerIndex = index;
            }
        }
        assert outerIndex >= 0 && ourLines.size() == list.size();

        this.lines.removeAll(ourLines);
        assert this.lines.size() == count - ourLines.size();
        final int newIndex = after ? outerIndex + inc - list.size() + 1 : outerIndex + inc;
        this.lines.addAll(newIndex, ourLines);
        assert this.lines.size() == count : "Move has changed the number of rows from " + count + " to " + this.lines.size();
        this.setDBOrder(false);
        order = this.getIDsOrder();
        this.getModel().getUpdateQ().reorder(order);
    }

    private List<Integer> getIDsOrder() {
        final List<Integer> ids = new ArrayList<Integer>();
        for (final Row r : this.lines)
            ids.add(r.getID());
        return ids;
    }

    @Override
    public Future<?> moveTo(final List<? extends Number> ids, final int index) {
        return this.getModel().getUpdateQ().put(new OfflineRunnable() {
            @Override
            public void run() {
                _moveTo(ids, index);
            }
        });
    }

    protected void _moveTo(final List<?> ids, final int index) {
        assert checkUpdateThread();
        final int count = this.lines.size();

        final List<Integer> order;
        final List<Row> list = new ArrayList<Row>(ids.size());
        for (final Object o : ids) {
            final Integer id = o instanceof SQLRowAccessor ? ((SQLRowAccessor) o).getID() : ((Number) o).intValue();
            list.add(this.getRow(id, true, o));
        }
        if (index <= 0) {
            this.lines.removeAll(list);
            this.lines.addAll(0, list);
        } else if (index >= this.lines.size()) {
            this.lines.removeAll(list);
            this.lines.addAll(list);
        } else {
            Row destLine = null;
            int i = index;
            boolean contains = true;
            while (i < this.lines.size() && contains) {
                destLine = this.lines.get(i);
                contains = list.contains(destLine);
                if (contains)
                    i++;
            }
            if (contains) {
                this.lines.removeAll(list);
                this.lines.addAll(list);
            } else {
                this.lines.removeAll(list);
                final int newIndex = this.indexOf(destLine);
                this.lines.addAll(newIndex, list);
            }
        }
        assert this.lines.size() == count : "Move has changed the number of rows from " + count + " to " + this.lines.size();
        this.setDBOrder(false);
        order = this.getIDsOrder();
        this.getModel().getUpdateQ().reorder(order);
    }

    // *** Roll back or Commit ***

    /**
     * Lose any changes and refetch from the database.
     * 
     * @return the future.
     */
    public final Future<?> reset() {
        return this.getModel().getUpdateQ().put(new OfflineRunnable() {
            @Override
            public void run() {
                _reset();
            }
        });
    }

    protected void _reset() {
        assert checkUpdateThread();

        this.lines.clear();
        this.id2line.clear();
        this.dbVals.clear();
        this.deleted.clear();
        this.setDBOrder(true);

        for (final SQLRowValues r : this.fetch())
            this._add(r, false, false);
        this.getModel().getUpdateQ().setFullList(getLines(), null);
    }

    /**
     * Make all changes applied to this persistent.
     * 
     * @return the future.
     */
    public final Future<?> commit() {
        return this.getModel().getUpdateQ().execute(new FutureTask<Object>(new OfflineCallable<Object>() {
            @Override
            public Object call() throws Exception {
                _commit();
                return null;
            }
        }));
    }

    protected final void _commit() throws SQLException {
        assert checkUpdateThread();

        // don't listen to every commit and then to re-order, just updateAll() at the end
        this.getModel().getUpdateQ().rmTableListener();

        try {
            SQLUtils.executeAtomic(this.getParent().getPrimaryTable().getDBSystemRoot().getDataSource(), new ConnectionHandlerNoSetup<Object, SQLException>() {
                @Override
                public Object handle(SQLDataSource ds) throws SQLException {
                    coreCommit();
                    return null;
                }
            });
        } finally {
            this.getModel().getUpdateQ().addTableListener();
        }
        this._reset();
    }

    protected void coreCommit() throws SQLException {
        // delete. Must be done first (e.g. there's a unique constraint and a deleted row conflicts
        // with a new row)
        getParent().getElem().archiveIDs(this.deleted);
        this.deleted.clear();

        // ordered rows
        final Map<Row, SQLRow> newRows = new LinkedHashMap<Row, SQLRow>();
        // insert, copy since we will remove some of the lines
        for (final Row l : this.lines) {
            final SQLRow newRow;
            if (!l.getRow().hasID()) {
                // only commit modified values, avoid updating each local, batiment, etc
                newRow = l.getRow().prune(this.modifiableVals).commit();
            } else {
                newRow = l.getRow().asRow();
            }
            // if the line is to be updated, this will get replaced below but it won't
            // change the ordering of the map
            newRows.put(l, newRow);
        }

        // update
        for (final Map.Entry<Row, SQLRowValues> e : this.dbVals.entrySet()) {
            final Row l = e.getKey();
            assert newRows.containsKey(l);
            newRows.put(l, this.getParent().getElem().update(e.getValue(), l.getRow()).exec());
        }
        this.dbVals.clear();

        final List<SQLRow> wantedOrder = new ArrayList<SQLRow>(newRows.values());
        final List<SQLRow> dbOrder = new ArrayList<SQLRow>(newRows.values());
        Collections.sort(dbOrder, OrderComparator.INSTANCE);
        if (!wantedOrder.equals(dbOrder)) {
            MoveQueue.moveAtOnce(wantedOrder.subList(1, wantedOrder.size()), true, wantedOrder.get(0));
        }
    }
}
