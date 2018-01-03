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

import org.openconcerto.sql.Log;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLRowValues.CreateMode;
import org.openconcerto.sql.model.SQLRowValuesCluster.State;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.SQLTableEvent;
import org.openconcerto.sql.model.SQLTableModifiedListener;
import org.openconcerto.sql.model.graph.Link.Direction;
import org.openconcerto.sql.model.graph.Path;
import org.openconcerto.sql.view.list.UpdateRunnable.RmAllRunnable;
import org.openconcerto.sql.view.list.search.SearchOne;
import org.openconcerto.sql.view.list.search.SearchOne.Mode;
import org.openconcerto.sql.view.list.search.SearchQueue;
import org.openconcerto.sql.view.list.search.SearchQueue.SetStateRunnable;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.ListMap;
import org.openconcerto.utils.RecursionType;
import org.openconcerto.utils.SleepingQueue;
import org.openconcerto.utils.Tuple2;
import org.openconcerto.utils.cc.IClosure;
import org.openconcerto.utils.cc.IPredicate;
import org.openconcerto.utils.cc.ITransformer;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.FutureTask;
import java.util.logging.Level;

import net.jcip.annotations.GuardedBy;

public final class UpdateQueue extends SleepingQueue {

    /**
     * Whether the passed future performs an update.
     * 
     * @param f a task in this queue, can be <code>null</code>.
     * @return <code>true</code> if <code>f</code> loads from the db.
     */
    static boolean isUpdate(FutureTask<?> f) {
        return isUpdate(SearchQueue.getRunnable(f));
    }

    static boolean isUpdate(Runnable r) {
        return r instanceof UpdateRunnable;
    }

    private static boolean isCancelableUpdate(Runnable r) {
        // don't cancel RmAll so we can put an UpdateAll right after it (the UpdateAll won't be
        // executed since RmAll put the queue to sleep)
        return isUpdate(r) && !(r instanceof RmAllRunnable);
    }

    private final class TableListener implements SQLTableModifiedListener, PropertyChangeListener {
        @Override
        public void tableModified(SQLTableEvent evt) {
            if (UpdateQueue.this.alwaysUpdateAll)
                putUpdateAll();
            else if (evt.getMode() == SQLTableEvent.Mode.ROW_UPDATED) {
                rowModified(evt);
            } else {
                rowAddedOrDeleted(evt);
            }
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            // where changed
            stateChanged(null, getModel().getReq().createState());
        }
    }

    private final ITableModel tableModel;
    // thread-confined
    private SQLTableModelSourceState state;
    @GuardedBy("itself")
    private final List<ListSQLLine> fullList;
    @GuardedBy("fullList")
    private SQLTableModelColumns columns;
    private final TableListener tableListener;
    // TODO rm : needed for now since our optimizations are false if there's a where not on the
    // primary table, see http://192.168.1.10:3000/issues/show/22
    private boolean alwaysUpdateAll = false;
    private final IClosure<Deque<FutureTask<?>>> cancelClosure;

    public UpdateQueue(ITableModel model) {
        super(UpdateQueue.class.getSimpleName() + " on " + model);
        this.tableModel = model;
        this.fullList = new ArrayList<ListSQLLine>();
        this.cancelClosure = createCancelClosure(this, new ITransformer<FutureTask<?>, TaskType>() {
            @Override
            public TaskType transformChecked(FutureTask<?> input) {
                final Runnable r = SearchQueue.getRunnable(input);
                if (isCancelableUpdate(r))
                    return TaskType.COMPUTE;
                else if (r instanceof SetStateRunnable)
                    return TaskType.SET_STATE;
                else
                    return TaskType.USER;
            }
        });
        this.tableListener = new TableListener();
    }

    private final ITableModel getModel() {
        return this.tableModel;
    }

    @Override
    protected void started() {
        // savoir quand les tables qu'on affiche changent
        addSourceListener();
        stateChanged(null, this.getModel().getReq().createState());
        // Only starts once there's something to search, that way the runnable passed to
        // ITableModel.search() will be meaningful
        // SetStateRunnable since this must not be cancelled by an updateAll, but it shouldn't
        // prevent earlier updates to be cancelled
        this.put(new SetStateRunnable() {
            @Override
            public void run() {
                getModel().getSearchQueue().start();
            }
        });
    }

    @Override
    protected void dying() throws Exception {
        super.dying();
        assert currentlyInQueue();

        // only kill searchQ once updateQ is really dead, otherwise the currently executing
        // update might finish once the searchQ is already dead.

        final SearchQueue searchQueue = getModel().getSearchQueue();
        // state cannot change since we only change it in this thread (except for an Error being
        // thrown and killing the queue)
        final RunningState state = searchQueue.getRunningState();
        // not started or there was an Error
        if (state == RunningState.NEW || state == RunningState.DEAD)
            return;
        if (state == RunningState.WILL_DIE || state == RunningState.DYING)
            throw new IllegalStateException("Someone else already called die()");
        try {
            searchQueue.die().get();
        } catch (Exception e) {
            if (searchQueue.getRunningState() != RunningState.DEAD)
                throw e;
            // there was an Error in the last run task or while in die(), but it's OK we wanted the
            // queue dead
            Log.get().log(Level.CONFIG, "Exception while killing search queue", e);
        }
        assert searchQueue.getRunningState().compareTo(RunningState.DYING) >= 0;
        searchQueue.join();
        assert searchQueue.getRunningState() == RunningState.DEAD;
    }

    final List<ListSQLLine> getFullList() {
        return this.fullList;
    }

    public final Tuple2<List<ListSQLLine>, SQLTableModelColumns> copyFullList() {
        final Tuple2<List<ListSQLLine>, SQLTableModelColumns> res;
        final List<ListSQLLine> fullList = this.getFullList();
        synchronized (fullList) {
            res = Tuple2.<List<ListSQLLine>, SQLTableModelColumns> create(new ArrayList<ListSQLLine>(fullList), this.columns);
        }
        return res;
    }

    public final ListSQLLine getLine(final Number id) {
        final ListSQLLine res;
        final List<ListSQLLine> fullList = this.getFullList();
        synchronized (fullList) {
            res = ListSQLLine.fromID(fullList, id.intValue());
        }
        return res;
    }

    /**
     * The lines and their path affected by a change of the passed row.
     * 
     * @param r the row that has changed.
     * @return the refreshed lines and their changed paths.
     */
    protected final ListMap<ListSQLLine, Path> getAffectedLines(final SQLRow r) {
        return this.getAffected(r, new ListMap<ListSQLLine, Path>(), true);
    }

    protected final ListMap<Path, ListSQLLine> getAffectedPaths(final SQLRow r) {
        return this.getAffected(r, new ListMap<Path, ListSQLLine>(), false);
    }

    // must be called from within this queue, as this method use fullList
    private <K, V> ListMap<K, V> getAffected(final SQLRow r, final ListMap<K, V> res, final boolean byLine) {
        final List<ListSQLLine> fullList = this.getFullList();
        synchronized (fullList) {
            final SQLTable t = r.getTable();
            final int id = r.getID();
            if (id < SQLRow.MIN_VALID_ID)
                throw new IllegalArgumentException("invalid ID: " + id);
            if (!fullList.isEmpty()) {
                final SQLRowValues proto = this.getState().getReq().getGraphToFetch();
                final List<Path> pathsToT = new ArrayList<Path>();
                proto.getGraph().walk(proto, pathsToT, new ITransformer<State<List<Path>>, List<Path>>() {
                    @Override
                    public List<Path> transformChecked(State<List<Path>> input) {
                        if (input.getCurrent().getTable() == t) {
                            input.getAcc().add(input.getPath());
                        }
                        return input.getAcc();
                    }
                }, RecursionType.BREADTH_FIRST, Direction.ANY);
                for (final Path p : pathsToT) {
                    final String lastReferentField = SearchQueue.getLastReferentField(p);
                    for (final ListSQLLine line : fullList) {
                        boolean put = false;
                        for (final SQLRowValues current : line.getRow().followPath(p, CreateMode.CREATE_NONE, false)) {
                            // works for rowValues w/o any ID
                            if (current != null && current.getID() == id) {
                                put = true;
                            }
                        }
                        // if the modified row isn't in the existing line, it might still affect it
                        // if it's a referent row insertion
                        if (!put && lastReferentField != null && r.exists()) {
                            final int foreignID = r.getInt(lastReferentField);
                            for (final SQLRowValues current : line.getRow().followPath(p.minusLast(), CreateMode.CREATE_NONE, false)) {
                                if (current.getID() == foreignID) {
                                    put = true;
                                }
                            }
                        }
                        if (put) {
                            // add to the list of paths that have been refreshed
                            add(byLine, res, p, line);
                        }
                    }
                }
            }
        }
        return res;
    }

    @SuppressWarnings("unchecked")
    <V, K> void add(boolean byLine, ListMap<K, V> res, final Path p, final ListSQLLine line) {
        if (byLine)
            res.add((K) line, (V) p);
        else
            res.add((K) p, (V) line);
    }

    final void setFullList(final List<ListSQLLine> tmp, final SQLTableModelColumns cols) {
        final List<ListSQLLine> fullList = this.getFullList();
        synchronized (fullList) {
            fullList.clear();
            fullList.addAll(tmp);
            // MAYBE only sort() if it can't be done by the SELECT
            // but comparing ints (field ORDRE) is quite fast : 170ms for 100,000 items
            Collections.sort(fullList);
            if (cols != null)
                this.columns = cols;
        }
        this.tableModel.getSearchQueue().fullListChanged();
    }

    final void reorder(final List<Integer> idsOrder) {
        final List<ListSQLLine> fullList = this.getFullList();
        synchronized (fullList) {
            for (final ListSQLLine l : fullList) {
                final Number newOrder;
                if (idsOrder == null) {
                    newOrder = null;
                } else {
                    final int index = idsOrder.indexOf(l.getID());
                    if (index < 0)
                        throw new IllegalArgumentException("Missing id " + l.getID() + " in " + idsOrder);
                    newOrder = index;
                }
                l.setOrder(newOrder);
            }
            Collections.sort(fullList);
        }
        this.tableModel.getSearchQueue().orderChanged();
    }

    // vals can be null if we're removing a referent row
    final void updateLine(ListSQLLine line, Path p, int valsID, SQLRowValues vals) {
        final Set<Integer> modifiedCols = line.loadAt(valsID, vals, p);
        this.tableModel.getSearchQueue().changeFullList(line.getID(), line, modifiedCols, SearchOne.Mode.CHANGE);
    }

    final ListSQLLine replaceLine(final int id, final ListSQLLine newLine) {
        final Mode mode;
        final List<ListSQLLine> fullList = this.getFullList();
        final ListSQLLine oldLine;
        synchronized (fullList) {
            final int modifiedIndex = ListSQLLine.indexFromID(fullList, id);
            oldLine = modifiedIndex < 0 ? null : fullList.get(modifiedIndex);

            if (modifiedIndex < 0) {
                // la ligne n'était dans notre liste
                if (newLine != null) {
                    // mais elle existe : ajout
                    // ATTN on ajoute à la fin, sans se soucier de l'ordre
                    fullList.add(newLine);
                    Collections.sort(fullList);
                    mode = Mode.ADD;
                } else {
                    // et elle n'y est toujours pas
                    mode = Mode.NO_CHANGE;
                }
            } else {
                // la ligne était dans notre liste
                if (newLine != null) {
                    // mettre à jour
                    fullList.set(modifiedIndex, newLine);
                    Collections.sort(fullList);
                    mode = Mode.CHANGE;
                } else {
                    // elle est effacée ou filtrée
                    fullList.remove(modifiedIndex);
                    mode = Mode.REMOVE;
                }
            }
        }

        // notify search queue
        this.tableModel.getSearchQueue().changeFullList(id, newLine, null, mode);
        return oldLine;
    }

    public final int getFullListSize() {
        final List<ListSQLLine> fullList = this.getFullList();
        synchronized (fullList) {
            return fullList.size();
        }
    }

    void setAlwaysUpdateAll(boolean b) {
        this.alwaysUpdateAll = b;
    }

    // *** listeners

    void stateChanged(final SQLTableModelSourceState beforeState, final SQLTableModelSourceState afterState) {
        if (afterState == null)
            throw new NullPointerException("Null state");

        // As in SearchQueue :
        // needs to be 2 different runnables, that way if the source is changed and then the table
        // is updated : the queue would naively contain setState, updateAll, updateAll and thus we
        // can cancel one updateAll. Whereas if the setState was contained in updateAll, we
        // couldn't cancel it.
        // use tasksDo() so that no other runnable can come between setState and updateAll.
        // Otherwise an updateOne might use new columns and add a line with different columns than
        // the full list.
        this.tasksDo(new IClosure<Deque<FutureTask<?>>>() {
            @Override
            public void executeChecked(Deque<FutureTask<?>> input) {
                put(new SetStateRunnable() {
                    @Override
                    public void run() {
                        setState(afterState);
                    }
                });
                // TODO if request didn't change and the new graph is smaller, copy and prune the
                // rows
                putUpdateAll();
            }
        });
    }

    protected final void setState(final SQLTableModelSourceState newState) {
        if (this.state != null)
            this.rmTableListener();
        this.state = newState;
        if (this.state != null)
            this.addTableListener();
    }

    protected final SQLTableModelSourceState getState() {
        assert this.currentlyInQueue();
        if (this.state == null)
            throw new IllegalStateException("Not yet started");
        return this.state;
    }

    @Override
    protected void willDie() {
        this.rmTableListener();
        this.removeSourceListener();
        super.willDie();
    }

    protected final void addTableListener() {
        this.getState().getReq().addTableListener(this.tableListener);
    }

    private void addSourceListener() {
        this.tableModel.getLinesSource().addListener(this.tableListener);
    }

    protected final void rmTableListener() {
        this.getState().getReq().removeTableListener(this.tableListener);
    }

    private void removeSourceListener() {
        this.tableModel.getLinesSource().rmListener(this.tableListener);
    }

    // *** une des tables que l'on affiche a changé

    void rowModified(final SQLTableEvent evt) {
        final int id = evt.getId();
        if (id < SQLRow.MIN_VALID_ID) {
            this.putUpdateAll();
        } else if (CollectionUtils.containsAny(this.tableModel.getReq().getLineFields(), evt.getFields())) {
            this.put(evt);
        }
        // si on n'affiche pas le champ ignorer
    }

    // takes 1-2ms, perhaps cache
    final Set<SQLTable> getNotForeignTables() {
        final Set<SQLTable> res = new HashSet<SQLTable>();
        final SQLRowValues maxGraph = this.tableModel.getReq().getMaxGraph();
        maxGraph.getGraph().walk(maxGraph, res, new ITransformer<State<Set<SQLTable>>, Set<SQLTable>>() {
            @Override
            public Set<SQLTable> transformChecked(State<Set<SQLTable>> input) {
                if (input.getPath().length() == 0 || input.isBackwards())
                    input.getAcc().add(input.getCurrent().getTable());
                return input.getAcc();
            }
        }, RecursionType.BREADTH_FIRST, Direction.ANY);
        return res;
    }

    void rowAddedOrDeleted(final SQLTableEvent evt) {
        if (evt.getId() < SQLRow.MIN_VALID_ID)
            this.putUpdateAll();
        // if a row of a table that we point to is added, we will care when the referent table will
        // point to it
        else if (this.getNotForeignTables().contains(evt.getTable()))
            this.put(evt);
    }

    // *** puts

    public final void putExternalUpdated(final String externalID, final IPredicate<ListSQLLine> affectedPredicate) {
        this.put(new Runnable() {
            @Override
            public void run() {
                externalUpdated(externalID, affectedPredicate);
            }
        });
    }

    protected final void externalUpdated(final String externalID, final IPredicate<ListSQLLine> affectedPredicate) {
        final List<ListSQLLine> fullList = this.getFullList();
        synchronized (fullList) {
            final Set<Integer> indexes = new HashSet<Integer>();
            int i = 0;
            for (final SQLTableModelColumn col : this.columns.getAllColumns()) {
                if (col.getUsedExternals().contains(externalID)) {
                    indexes.add(i);
                }
                i++;
            }
            if (indexes.isEmpty()) {
                Log.get().log(Level.INFO, "No columns use " + externalID + " in " + this);
                return;
            }

            for (final ListSQLLine line : fullList) {
                if (affectedPredicate.evaluateChecked(line)) {
                    this.tableModel.getSearchQueue().changeFullList(line.getID(), line, indexes, SearchOne.Mode.CHANGE);
                }
            }
        }
    }

    private void put(SQLTableEvent evt) {
        this.put(UpdateRunnable.create(this.tableModel, evt));
    }

    public void putUpdateAll() {
        this.put(UpdateRunnable.create(this.tableModel));
    }

    /**
     * If this is sleeping, empty the list and call {@link #putUpdateAll()} so that the list reload
     * itself when this wakes up.
     * 
     * @throws IllegalStateException if not sleeping.
     */
    void putRemoveAll() {
        if (!this.isSleeping())
            throw new IllegalStateException("not sleeping");
        // no user runnables can come between the RmAll and the UpdateAll since runnableAdded()
        // is blocked by our lock, so there won't be any incoherence for them
        this.put(UpdateRunnable.createRmAll(this, this.tableModel));
        this.setSleeping(false);
        // reload the empty list when waking up
        this.putUpdateAll();
    }

    @Override
    protected void willPut(final FutureTask<?> qr) throws InterruptedException {
        if (SearchQueue.getRunnable(qr) instanceof ChangeAllRunnable) {
            // si on met tout à jour, ne sert à rien de garder les maj précédentes.
            this.tasksDo(this.cancelClosure);
        }
    }

    static public enum TaskType {
        USER(false, true), COMPUTE(true, false), SET_STATE(false, false);

        private final boolean cancelable, dependsOnPrevious;

        private TaskType(boolean cancelable, boolean dependsOnPrevious) {
            this.cancelable = cancelable;
            this.dependsOnPrevious = dependsOnPrevious;
        }
    }

    static public final IClosure<Deque<FutureTask<?>>> createCancelClosure(final SleepingQueue q, final ITransformer<? super FutureTask<?>, TaskType> cancelablePred) {
        return new IClosure<Deque<FutureTask<?>>>() {
            @Override
            public void executeChecked(final Deque<FutureTask<?>> tasks) {
                // on part de la fin et on supprime toutes les maj jusqu'a ce qu'on trouve
                // un runnable qui n'est pas annulable
                final Iterator<FutureTask<?>> iter = tasks.descendingIterator();
                boolean needsPrevious = false;
                while (iter.hasNext() && !needsPrevious) {
                    final FutureTask<?> current = iter.next();
                    final TaskType type = cancelablePred.transformChecked(current);
                    needsPrevious = type.dependsOnPrevious;
                    if (type.cancelable)
                        iter.remove();
                }
                // if we stop only because we ran out of items, continue with beingRun
                if (!needsPrevious) {
                    // before trying to cancel being run we should have been through all the backlog
                    assert !iter.hasNext();
                    final FutureTask<?> br = q.getBeingRun();
                    if (br != null && cancelablePred.transformChecked(br).cancelable) {
                        // might already be done by now, but it's OK cancel() will just return false
                        br.cancel(true);
                    }
                }
            }
        };
    }

}
