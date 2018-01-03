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

import org.openconcerto.sql.model.SQLRowValues.CreateMode;
import org.openconcerto.sql.model.SQLRowValues.ForeignCopyMode;
import org.openconcerto.sql.model.SQLRowValues.ReferentChangeEvent;
import org.openconcerto.sql.model.SQLRowValues.ReferentChangeListener;
import org.openconcerto.sql.model.graph.Link.Direction;
import org.openconcerto.sql.model.graph.Path;
import org.openconcerto.sql.model.graph.Step;
import org.openconcerto.sql.utils.SQLUtils;
import org.openconcerto.utils.CollectionMap2.Mode;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.CompareUtils;
import org.openconcerto.utils.Matrix;
import org.openconcerto.utils.RecursionType;
import org.openconcerto.utils.SetMap;
import org.openconcerto.utils.StringUtils;
import org.openconcerto.utils.StringUtils.Side;
import org.openconcerto.utils.cc.Closure;
import org.openconcerto.utils.cc.IClosure;
import org.openconcerto.utils.cc.ITransformer;
import org.openconcerto.utils.cc.IdentityHashSet;
import org.openconcerto.utils.cc.IdentitySet;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EventObject;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import net.jcip.annotations.Immutable;

/**
 * A set of linked SQLRowValues.
 * 
 * @author Sylvain
 */
public class SQLRowValuesCluster {
    private static final Comparator<SQLField> FIELD_COMPARATOR = new Comparator<SQLField>() {
        @Override
        public int compare(SQLField o1, SQLField o2) {
            return o1.getSQLName().quote().compareTo(o2.getSQLName().quote());
        }
    };

    /**
     * The list of links in the order they've been set. This allows deterministic and predictable
     * insertion order. E.g. for
     * 
     * <pre>
     * r2.put(f, r1);
     * r3.put(f2, r2);
     * r4.put(f2, r2);
     * </pre>
     * 
     * the links will be :
     * 
     * <pre>
     * r1
     * r2 f r1
     * r3 f2 r2
     * r4 f2 r2
     * </pre>
     */
    private final List<Link> links;
    private final IdentitySet<SQLRowValues> items;
    // { vals -> listener on vals' graph }
    private Map<SQLRowValues, List<ValueChangeListener>> listeners;

    private boolean frozen = false;

    private SQLRowValuesCluster() {
        this.links = new ArrayList<Link>();
        // SQLRowValues equals() depends on their values, but we must tell apart each reference
        this.items = new IdentityHashSet<SQLRowValues>();
        this.listeners = null;
    }

    SQLRowValuesCluster(SQLRowValues vals) {
        this();
        addVals(-1, vals);
    }

    // add a lonely node to this
    private final void addVals(final int index, final SQLRowValues vals) {
        assert vals.getGraph(false) == null;
        if (index < 0)
            this.links.add(new Link(vals));
        else
            this.links.add(index, new Link(vals));
        this.items.add(vals);
    }

    private final SQLRowValues getHead() {
        return this.links.get(0).getSrc();
    }

    private final DBSystemRoot getSystemRoot() {
        return this.getHead().getTable().getDBSystemRoot();
    }

    /**
     * All the rowValues in this cluster.
     * 
     * @return the set of SQLRowValues.
     */
    public final Set<SQLRowValues> getItems() {
        return Collections.unmodifiableSet(this.items);
    }

    /**
     * The number of items in this instance. NOTE: calling {@link SQLRowValues#getGraphSize()} is
     * preferable since it might avoid an allocation.
     * 
     * @return the number of items.
     */
    public final int size() {
        return this.items.size();
    }

    public final boolean contains(SQLRowValues start) {
        return this.items.contains(start);
    }

    private final void containsCheck(SQLRowValues vals) {
        if (!this.contains(vals))
            throw new IllegalArgumentException(vals + " not in " + this);
    }

    // if true a single link path passed to followPath() will never yield more than one row
    final boolean hasOneRowPerPath() {
        for (final SQLRowValues item : this.getItems()) {
            for (final Entry<SQLField, Set<SQLRowValues>> e : item.getReferents().entrySet()) {
                if (e.getValue().size() > 1)
                    return false;
            }
        }
        return true;
    }

    public final Set<SQLTable> getTables() {
        final Set<SQLTable> res = new HashSet<SQLTable>();
        for (final SQLRowValues v : this.items)
            res.add(v.getTable());
        return res;
    }

    public final boolean isFrozen() {
        return this.frozen;
    }

    /**
     * Freeze this graph so that no modification can be made. Once this method returns, this
     * instance can be safely published (e.g. stored into a field that is properly guarded by a
     * lock) to other threads without further synchronizations.
     * 
     * @return <code>true</code> if this call changed the frozen status.
     */
    public final boolean freeze() {
        if (this.frozen)
            return false;
        this.frozen = true;
        return true;
    }

    void remove(SQLRowValues src, SQLField f, SQLRowValues dest) {
        assert dest != null;
        assert src.getGraph() == this;
        assert src.getTable() == f.getTable();
        assert !this.isFrozen() : "Should already be checked by SQLRowValues";

        final Link toRm = new Link(src, f, dest);
        this.links.remove(toRm);

        // test if the removal of the link split the graph
        final IdentitySet<SQLRowValues> reachable = this.getReachable(src);
        if (reachable.size() < this.size()) {
            final SQLRowValuesCluster newCluster = new SQLRowValuesCluster();

            // moves the links no longer in us into a new cluster
            final Iterator<Link> iter = this.links.iterator();
            while (iter.hasNext()) {
                final Link l = iter.next();
                // the graph is split in two, so every link is in one part
                assert l.getDest() == null || reachable.contains(l.getSrc()) == reachable.contains(l.getDest());
                // hence only test the source
                if (!reachable.contains(l.getSrc())) {
                    iter.remove();
                    newCluster.links.add(l);
                }
            }
            // move unreachable items
            assert newCluster.items.isEmpty();
            final Iterator<SQLRowValues> itemIter = this.items.iterator();
            while (itemIter.hasNext()) {
                final SQLRowValues key = itemIter.next();
                if (!reachable.contains(key)) {
                    itemIter.remove();
                    newCluster.items.add(key);
                    if (this.listeners != null && this.listeners.containsKey(key))
                        newCluster.getListeners().put(key, this.listeners.remove(key));
                }
            }
            // http://bugs.java.com/bugdatabase/view_bug.do?bug_id=6612102
            // resolved in 7b25 : iterator.remove() might decrement the size twice : e.g. size is 0
            // and thus isEmpty() is true, while there's still elements in this.items
            assert !this.items.isEmpty() : "Empty items while removing " + f + " -> " + dest + " from " + src;
            assert !newCluster.items.isEmpty() : "New graph is empty while removing " + f + " -> " + dest + " from " + src;
            assert !CollectionUtils.containsAny(this.items, newCluster.items) : "Shared items while removing " + f + " -> " + dest + " from " + src;

            for (final SQLRowValues vals : newCluster.getItems())
                vals.setGraph(newCluster);
        }
    }

    void add(SQLRowValues src, SQLField f, SQLRowValues dest) {
        assert dest != null;
        assert src.getTable() == f.getTable();
        assert !this.isFrozen() : "Should already be checked by SQLRowValues";

        final boolean containsSrc = this.contains(src);
        final boolean containsDest = this.contains(dest);
        if (!containsSrc && !containsDest)
            throw new IllegalArgumentException("Neither source nor destination are contained in this :\n" + src + "\n" + dest);

        final Link toAdd = new Link(src, f, dest);
        if (containsSrc && containsDest) {
            // both source and dest are in us
            this.links.add(toAdd);
        } else {
            assert src.getGraph(false) != dest.getGraph(false);
            final SQLRowValues rowToAdd;
            final int index;
            if (containsSrc) {
                rowToAdd = dest;
                // merge the two graphs
                // add dest before src since it will be needed to store us (adding at the end would
                // work, but src would need to be inserted, then updated)
                // add dest just before src, to keep the order of foreigns (needed for deepCopy()
                // and insertion order) (adding at the beginning would reverse the order of foreign
                // rows to insert)
                final int srcIndex = this.links.indexOf(new Link(src));
                if (srcIndex < 0)
                    throw new IllegalStateException("Source link not found for " + src);
                index = srcIndex;
            } else {
                assert containsDest;
                rowToAdd = src;
                index = -1;
            }
            final SQLRowValuesCluster graphToAdd = rowToAdd.getGraph(false);

            // to preserve memory a single node has no graph unless required
            // this way rowToAdd never had to create a Cluster, it will use us.
            if (graphToAdd == null) {
                this.addVals(index, rowToAdd);
                rowToAdd.setGraph(this);
            } else {
                if (index < 0)
                    this.links.addAll(graphToAdd.links);
                else
                    this.links.addAll(index, graphToAdd.links);
                graphToAdd.links.clear();
                this.items.addAll(graphToAdd.items);
                for (final SQLRowValues newlyAdded : graphToAdd.items) {
                    newlyAdded.setGraph(this);
                }
                graphToAdd.items.clear();
                if (graphToAdd.listeners != null) {
                    this.getListeners().putAll(graphToAdd.listeners);
                    graphToAdd.listeners = null;
                }
            }
            // To keep foreign links order, new links should be added after existing ones from src
            // (which are after srcIndex). Since they're merged in store(), just add at the end.
            this.links.add(toAdd);
        }
        assert src.getGraph() == dest.getGraph();
    }

    private IdentitySet<SQLRowValues> getReachable(final SQLRowValues from) {
        final IdentitySet<SQLRowValues> res = new IdentityHashSet<SQLRowValues>();
        getReachableRec(from, res);
        return res;
    }

    private void getReachableRec(final SQLRowValues from, final IdentitySet<SQLRowValues> acc) {
        if (!acc.add(from))
            return;

        for (final SQLRowValues fVals : from.getForeigns().values()) {
            this.getReachableRec(fVals, acc);
        }
        for (final SQLRowValues fVals : from.getReferentRows()) {
            this.getReachableRec(fVals, acc);
        }
    }

    final SQLRowValues deepCopy(SQLRowValues v, final boolean freeze) {
        return deepCopy(freeze).get(v);
    }

    public final Map<SQLRowValues, SQLRowValues> deepCopy(final boolean freeze) {
        // copy all rowValues of this graph
        final Map<SQLRowValues, SQLRowValues> noLinkCopy = new IdentityHashMap<SQLRowValues, SQLRowValues>();
        // don't copy foreigns, but we want to preserve the order of all fields. This works because
        // the second put() with the actual foreign row doesn't change the order.
        final ForeignCopyMode copyMode = ForeignCopyMode.COPY_NULL;
        for (final SQLRowValues n : this.getItems()) {
            final SQLRowValues copy;
            // might as well use the minimum memory if the values won't change
            if (freeze) {
                copy = new SQLRowValues(n.getTable(), n.size(), n.getForeignsSize(), n.getReferents().size());
                copy.setAll(n.getAllValues(copyMode));
            } else {
                copy = new SQLRowValues(n, copyMode);
            }
            noLinkCopy.put(n, copy);
        }

        // and link them together in order
        for (final Link l : this.links) {
            if (l.getField() != null) {
                noLinkCopy.get(l.getSrc()).put(l.getField().getName(), noLinkCopy.get(l.getDest()));
            } else {
                assert noLinkCopy.containsKey(l.getSrc());
            }
        }

        final SQLRowValues res = noLinkCopy.values().iterator().next();
        // only force graph creation if needed
        if (freeze)
            res.getGraph().freeze();
        assert res.isFrozen() == freeze;

        return noLinkCopy;
    }

    public final StoreResult insert() throws SQLException {
        return this.store(StoreMode.INSERT);
    }

    public final StoreResult store(final StoreMode mode) throws SQLException {
        return this.store(mode, null);
    }

    // checkValidity false useful when we want to avoid loading the graph
    public final StoreResult store(final StoreMode mode, final Boolean checkValidity) throws SQLException {
        return this.store(mode, null, null, checkValidity, true);
    }

    /**
     * Store this graph into the DB.
     * 
     * @param mode how to store.
     * @param start when storing a subset, the start of <code>pruneGraph</code> in this, can be
     *        <code>null</code>.
     * @param pruneGraph the maximum graph to store, can be <code>null</code>.
     * @param checkValidity whether to ask for checking if foreign keys point to valid rows, see
     *        {@link SQLRowValues#setValidityChecked(SQLRowValues.ValidityCheck)}.
     * @param fireEvent <code>false</code> if stored rows shouldn't be fetched and
     *        {@link SQLTableEvent} should not be fired.
     * @return the store result.
     * @throws SQLException if an exception occurs.
     * @see {@link #prune(SQLRowValues, SQLRowValues)}
     */
    public final StoreResult store(final StoreMode mode, final SQLRowValues start, final SQLRowValues pruneGraph, final Boolean checkValidity, final boolean fireEvent) throws SQLException {
        final Map<SQLRowValues, SQLRowValues> prune2orig;
        final SQLRowValuesCluster toStore;
        final boolean prune = pruneGraph != null;
        if (!prune) {
            toStore = this;
            prune2orig = null;
        } else {
            final Map<SQLRowValues, SQLRowValues> orig2prune = this.pruneMap(start, pruneGraph, true);
            toStore = orig2prune.get(start).getGraph();
            prune2orig = CollectionUtils.invertMap(new IdentityHashMap<SQLRowValues, SQLRowValues>(), orig2prune);
        }
        final Map<SQLRowValues, Node> nodes = new IdentityHashMap<SQLRowValues, Node>(toStore.size());
        final Map<SQLRowValues, Node> res = prune ? new IdentityHashMap<SQLRowValues, Node>(toStore.size()) : nodes;
        for (final SQLRowValues vals : toStore.getItems()) {
            nodes.put(vals, new Node(vals));
            if (prune) {
                final SQLRowValues src = prune2orig.get(vals);
                assert this.contains(src);
                res.put(src, nodes.get(vals));
            }
        }
        // check validity first, avoid beginning a transaction for nothing
        // do it after reset otherwise check previous values
        if (SQLRowValues.isValidityChecked(checkValidity))
            for (final Node n : nodes.values()) {
                n.noLink.checkValidity();
            }
        // this will hold the links and their ID as they are known
        /**
         * A cycle example :
         * 
         * <pre>
         * s null
         * c ID_SITE s
         * c null
         * s ID_CONTACT_RAPPORT c
         * s ID_CONTACT_UTILE c
         * </pre>
         * 
         * First s will be inserted :
         * 
         * <pre>
         * c ID_SITE sID
         * c null
         * s ID_CONTACT_RAPPORT c
         * s ID_CONTACT_UTILE c
         * </pre>
         * 
         * Then c :
         * 
         * <pre>
         * s ID_CONTACT_RAPPORT cID
         * s ID_CONTACT_UTILE cID
         * </pre>
         * 
         * And finally, s will be updated.
         */
        final List<StoringLink> storingLinks = new ArrayList<StoringLink>(toStore.links.size());
        for (final Link l : toStore.links)
            storingLinks.add(new StoringLink(l));

        // store the whole graph atomically
        final List<SQLTableEvent> events = SQLUtils.executeAtomic(getSystemRoot().getDataSource(), new ConnectionHandlerNoSetup<List<SQLTableEvent>, SQLException>() {
            @Override
            public List<SQLTableEvent> handle(SQLDataSource ds) throws SQLException {
                final List<SQLTableEvent> res = new ArrayList<SQLTableEvent>();
                while (storingLinks.size() > 0) {
                    final StoringLink toStore = storingLinks.remove(0);
                    if (!toStore.canStore())
                        throw new IllegalStateException();
                    final Node n = nodes.get(toStore.getSrc());

                    // merge the maximum of links starting from the row to be stored
                    boolean lastDBAccess = true;
                    final Iterator<StoringLink> iter = storingLinks.iterator();
                    while (iter.hasNext()) {
                        final StoringLink sl = iter.next();
                        if (sl.getSrc() == toStore.getSrc()) {
                            if (sl.canStore()) {
                                iter.remove();
                                // sl can either be the main row or one the link from the row
                                // (bear in mind that toStore can be not the main row if the link
                                // destination has already been inserted)
                                if (sl.destID != null)
                                    n.noLink.put(sl.getField().getName(), sl.destID);
                            } else {
                                lastDBAccess = false;
                            }
                        }
                    }

                    if (n.isStored()) {
                        // if there's a cycle, we have to update an already inserted row
                        res.add(n.update(fireEvent));
                    } else {
                        res.add(n.store(fireEvent, mode));
                        final SQLRow r = n.getStoredRow();

                        // fill the noLink of referent nodes with the new ID
                        for (final StoringLink sl : storingLinks) {
                            if (sl.getDest() == toStore.getSrc()) {
                                sl.destID = r.getIDNumber();
                                nodes.get(sl.getSrc()).noLink.put(sl.getField().getName(), r.getIDNumber());
                            }
                        }
                    }

                    // link together the new values
                    // if there is a cycle not all foreign keys can be stored at the same time, so
                    // wait for the last DB access
                    if (lastDBAccess) {
                        for (final Map.Entry<String, SQLRowValues> e : toStore.getSrc().getForeigns().entrySet()) {
                            final SQLRowValues foreign = nodes.get(e.getValue()).getStoredValues();
                            assert foreign != null : "since this the last db access for this row, all foreigns should have been inserted";
                            // check coherence
                            if (n.getStoredValues().getLong(e.getKey()) != foreign.getIDNumber().longValue())
                                throw new IllegalStateException("stored " + n.getStoredValues().getObject(e.getKey()) + " but foreign is " + SQLRowValues.trim(foreign));
                            n.getStoredValues().put(e.getKey(), foreign);
                        }
                    }
                }
                // all nodes share the same graph, so pick any and freeze the graph
                // null if !fireEvent or if non-rowable table
                final SQLRowValues graphFetched = nodes.values().iterator().next().getStoredValues();
                if (graphFetched != null)
                    graphFetched.getGraph().freeze();
                return res;
            }
        });
        // fire events
        if (fireEvent) {
            for (final SQLTableEvent n : events) {
                // MAYBE put a Map<SQLRowValues, SQLTableEvent> to know how our fellow values have
                // been affected
                n.getTable().fire(n);
            }
        }

        return new StoreResult(res);
    }

    static public final class WalkOptions {
        private final Direction direction;
        private RecursionType recType;
        private boolean allowCycle;
        private boolean includeStart;
        private boolean ignoreForeignsOrder;

        public WalkOptions(final Direction dir) {
            if (dir == null)
                throw new NullPointerException("No direction");
            this.direction = dir;
            this.recType = RecursionType.BREADTH_FIRST;
            this.allowCycle = false;
            this.includeStart = true;
            this.ignoreForeignsOrder = true;
        }

        public Direction getDirection() {
            return this.direction;
        }

        public RecursionType getRecursionType() {
            return this.recType;
        }

        public WalkOptions setRecursionType(RecursionType recType) {
            if (recType == null)
                throw new NullPointerException("No type");
            this.recType = recType;
            return this;
        }

        public boolean isCycleAllowed() {
            return this.allowCycle;
        }

        /**
         * Set whether cycles (encountering the same row twice) are allowed. If <code>false</code>
         * is passed, then steps that go back to already visited rows will never be crossed :
         * 
         * <pre>
         *       /- ID_CONTACT_RAPPORT \
         *  SITE -- ID_CONTACT_CHEF ---> CONTACT 
         *       \-     ID_SITE        /
         * </pre>
         * 
         * The SITE will only be passed once and ID_SITE would never be crossed from CONTACT. To go
         * through all paths, pass <code>true</code> and SITE will be visited 3 times if
         * {@link Direction#FOREIGN} and 7 times if {@link Direction#ANY}. Note that in both cases,
         * CONTACT will be visited 3 times if {@link Direction#ANY} and twice if
         * {@link Direction#FOREIGN}.
         * 
         * @param allowCycle <code>true</code> if {@link State#getValsPath()} can contains the same
         *        row twice, <code>false</code> to stop before that happens.
         * @return this.
         */
        public WalkOptions setCycleAllowed(boolean allowCycle) {
            this.allowCycle = allowCycle;
            return this;
        }

        public boolean isStartIncluded() {
            return this.includeStart;
        }

        public WalkOptions setStartIncluded(boolean includeStart) {
            this.includeStart = includeStart;
            return this;
        }

        public boolean isForeignsOrderIgnored() {
            return this.ignoreForeignsOrder;
        }

        public WalkOptions setForeignsOrderIgnored(boolean ignoreForeignsOrder) {
            this.ignoreForeignsOrder = ignoreForeignsOrder;
            return this;
        }
    }

    /**
     * Walk the graph from the passed node, executing the closure for each node on the path. NOTE
     * that this method only goes one way through foreign keys, ie if this cluster is a tree and
     * <code>start</code> is not the root, some nodes will not be traversed.
     * 
     * @param <T> type of acc
     * @param start where to start the walk.
     * @param acc the initial value.
     * @param closure what to do on each node.
     */
    public final <T> void walk(final SQLRowValues start, T acc, ITransformer<State<T>, T> closure) {
        this.walk(start, acc, closure, RecursionType.BREADTH_FIRST);
    }

    /**
     * Walk the graph from the passed node, executing the closure for each node on the path. NOTE
     * that this method only goes one way through foreign keys, ie if this cluster is a tree and
     * <code>start</code> is not the root, some nodes will not be traversed. Also you can stop the
     * recursion by throwing {@link StopRecurseException} in <code>closure</code>.
     * 
     * @param <T> type of acc
     * @param start where to start the walk.
     * @param acc the initial value.
     * @param closure what to do on each node.
     * @param recType how to recurse.
     * @return the exception that stopped the recursion, <code>null</code> if none was thrown.
     */
    public final <T> StopRecurseException walk(final SQLRowValues start, T acc, ITransformer<State<T>, T> closure, RecursionType recType) {
        return this.walk(start, acc, closure, recType, Direction.FOREIGN);
    }

    public final <T> StopRecurseException walk(final SQLRowValues start, T acc, ITransformer<State<T>, T> closure, RecursionType recType, final Direction foreign) {
        return this.walk(start, acc, closure, new WalkOptions(foreign).setRecursionType(recType));
    }

    public final <T> StopRecurseException walk(final SQLRowValues start, T acc, ITransformer<State<T>, T> closure, final WalkOptions options) {
        this.containsCheck(start);
        return this.walk(new State<T>(Collections.singletonList(start), Path.get(start.getTable()), acc, closure), options, options.isStartIncluded());
    }

    /**
     * Walk through the graph from the passed state. NOTE: this method will call the {@link State}
     * with each path a row can be reached (except if this would cause a cycle, see
     * {@link WalkOptions#setCycleAllowed(boolean)}). E.g. for :
     * 
     * <pre>
     *  SITE -- ID_CONTACT_CHEF ---> CONTACT -- ID_TITLE ---> TITLE 
     *       \- ID_CONTACT_RAPPORT /
     * </pre>
     * 
     * The CONTACT and TITLE will be passed twice, once for each link.
     * 
     * @param <T> type of acc.
     * @param state the current position in the graph.
     * @param options how to walk the graph.
     * @param computeThisState <code>false</code> if the <code>state</code> should not be
     *        {@link State#compute() computed}.
     * @return the exception that stopped the recursion, <code>null</code> if none was thrown.
     */
    private final <T> StopRecurseException walk(final State<T> state, final WalkOptions options, final boolean computeThisState) {
        if (computeThisState && options.getRecursionType() == RecursionType.BREADTH_FIRST) {
            final StopRecurseException e = state.compute();
            if (e != null)
                return e;
        }

        if (!options.isCycleAllowed() || !state.hasCycle()) {
            // get the foreign or referents rowValues
            StopRecurseException res = null;
            if (options.getDirection() != Direction.REFERENT) {
                res = rec(state, options, Direction.FOREIGN);
            }
            if (res != null)
                return res;
            if (options.getDirection() != Direction.FOREIGN) {
                res = rec(state, options, Direction.REFERENT);
            }
            if (res != null)
                return res;
        }

        if (computeThisState && options.getRecursionType() == RecursionType.DEPTH_FIRST) {
            final StopRecurseException e = state.compute();
            if (e != null)
                return e;
        }
        return null;
    }

    private <T> StopRecurseException rec(final State<T> state, final WalkOptions options, final Direction actualDirection) {
        final SQLRowValues current = state.getCurrent();
        final List<SQLRowValues> currentValsPath = state.getValsPath();
        final SetMap<SQLField, SQLRowValues> nextVals;
        if (actualDirection == Direction.FOREIGN) {
            final Map<SQLField, SQLRowValues> foreigns = current.getForeignsBySQLField();
            nextVals = new SetMap<SQLField, SQLRowValues>(new LinkedHashMap<SQLField, Set<SQLRowValues>>(foreigns.size()), Mode.NULL_FORBIDDEN);
            nextVals.mergeScalarMap(foreigns);
        } else {
            assert actualDirection == Direction.REFERENT;
            nextVals = current.getReferents();
        }
        // predictable and repeatable order (SQLRowValues.referents has no order, but .foreigns has)
        final List<SQLField> keys = new ArrayList<SQLField>(nextVals.keySet());
        if (actualDirection == Direction.REFERENT || options.isForeignsOrderIgnored())
            Collections.sort(keys, FIELD_COMPARATOR);
        for (final SQLField f : keys) {
            final Step step = Step.create(f, actualDirection);
            // we must never go back from where we came
            final boolean backtrack = state.getPath().length() > 0 && state.getPath().getStep(-1).equals(step.reverse());
            for (final SQLRowValues v : nextVals.getNonNull(f)) {
                // backtrack is not enough, there can be other referents than previous
                // avoid infinite loop (don't use equals so that we can go over several equals rows)
                if (!(backtrack && v == state.getPrevious()) && (options.isCycleAllowed() || !state.identityContains(v))) {
                    final Path path = state.getPath().add(step);
                    final List<SQLRowValues> valsPath = new ArrayList<SQLRowValues>(currentValsPath);
                    valsPath.add(v);
                    final StopRecurseException e = this.walk(new State<T>(Collections.unmodifiableList(valsPath), path, state.getAcc(), state.closure), options, true);
                    if (e != null && e.isCompletely())
                        return e;
                }
            }
        }
        return null;
    }

    @Immutable
    static public final class IndexedRows {
        private final List<State<?>> flatList;
        private final Map<SQLRowValues, Integer> indexes;

        // optimization
        private IndexedRows(final SQLRowValues sole) {
            this(Collections.<State<?>> singletonList(new State<Object>(Collections.singletonList(sole), Path.get(sole.getTable()), null, null)), Collections.singletonMap(sole, 0));
            if (sole.getGraphSize() != 1)
                throw new IllegalArgumentException("Row is not alone : " + sole.printGraph());
        }

        // private to make sure the arguments are immutable
        private IndexedRows(List<State<?>> flatList, Map<SQLRowValues, Integer> indexes) {
            super();
            this.flatList = flatList;
            this.indexes = indexes;
            assert flatList.size() == indexes.size();
        }

        List<State<?>> getStates() {
            return this.flatList;
        }

        public int getSize() {
            return this.flatList.size();
        }

        State<?> getFirstState(int i) {
            return this.flatList.get(i);
        }

        public SQLRowValues getRow(int i) {
            return this.getFirstState(i).getCurrent();
        }

        public Path getFirstPath(int i) {
            return this.getFirstState(i).getPath();
        }

        public int getIndex(final SQLRowValues v) {
            return this.indexes.get(v);
        }
    }

    /**
     * Return all rows of this graph. This method will
     * {@link #walk(SQLRowValues, Object, ITransformer, WalkOptions) walk} the graph with the passed
     * parameters and collect the {@link State state} when it first reaches each row.
     * 
     * @param vals where to start.
     * @param recType how to recurse.
     * @param useForeignsOrder <code>true</code> to use the order of foreign keys.
     * @return all rows.
     */
    public final IndexedRows getIndexedRows(final SQLRowValues vals, final RecursionType recType, final boolean useForeignsOrder) {
        final int size = this.size();
        final List<State<?>> flatList = new ArrayList<State<?>>(size);
        final IdentityHashMap<SQLRowValues, Integer> thisIndexes = new IdentityHashMap<SQLRowValues, Integer>(size);

        final WalkOptions walkOptions = new WalkOptions(Direction.ANY).setRecursionType(recType).setForeignsOrderIgnored(!useForeignsOrder).setStartIncluded(true);
        this.walk(vals, null, new ITransformer<State<Object>, Object>() {
            @Override
            public Object transformChecked(State<Object> input) {
                final SQLRowValues r = input.getCurrent();
                if (thisIndexes.containsKey(r)) {
                    // happens when there's multiple paths between 2 rows
                    throw new StopRecurseException("already added").setCompletely(false);
                } else {
                    thisIndexes.put(r, flatList.size());
                    flatList.add(input);
                }
                return null;
            }
        }, walkOptions);

        assert flatList.size() == size : "missing rows, should have been " + size + " but was " + flatList.size() + " : " + flatList;
        return new IndexedRows(Collections.unmodifiableList(flatList), Collections.unmodifiableMap(thisIndexes));
    }

    final void walkFields(final SQLRowValues start, IClosure<FieldPath> closure, final boolean includeFK) {
        walkFields(start, Path.get(start.getTable()), Collections.singletonList(start), closure, includeFK);
    }

    private void walkFields(final SQLRowValues current, final Path p, final List<SQLRowValues> currentValsPath, IClosure<FieldPath> closure, final boolean includeFK) {
        final Map<String, SQLRowValues> foreigns = current.getForeigns();
        for (final String field : current.getFields()) {
            final boolean isFK = foreigns.containsKey(field);
            if (!isFK || includeFK)
                closure.executeChecked(new FieldPath(p, field));
            if (isFK) {
                final SQLRowValues newVals = foreigns.get(field);
                // avoid infinite loop
                if (!currentValsPath.contains(newVals)) {
                    final Path newP = p.add(current.getTable().getField(field), Direction.FOREIGN);
                    final List<SQLRowValues> newValsPath = new ArrayList<SQLRowValues>(currentValsPath);
                    newValsPath.add(newVals);
                    this.walkFields(newVals, newP, newValsPath, closure, includeFK);
                }
            }
        }
    }

    /**
     * Copy this leaving only the fields specified by <code>graph</code>. NOTE: the result is
     * guaranteed not to be larger than <code>graph</code>, but it might be smaller if this didn't
     * contain all the paths.
     * 
     * @param start where to start pruning, eg RECEPTEUR {DESIGNATION="rec", CONSTAT="ok",
     *        ID_LOCAL=LOCAL{DESIGNATION="local"}}.
     * @param graph the maximum structure wanted, eg RECEPTEUR {DESIGNATION=null}.
     * @return a copy of this no larger than <code>graph</code>, eg RECEPTEUR {DESIGNATION="rec"}.
     */
    public final SQLRowValues prune(final SQLRowValues start, final SQLRowValues graph) {
        return this.prune(start, graph, true);
    }

    /**
     * Copy this leaving only the fields specified by <code>graph</code>. NOTE: the result is
     * guaranteed not to be larger than <code>graph</code>, but it might be smaller if this didn't
     * contain all the paths. NOTE : only fields are considered, i.e. referents are not used. E.g.
     * with the rows below, no links would be removed, even though one LOCAL in the graph has no
     * CPI.
     * 
     * <pre>
     * Graph :
     *        LOCAL LOCAL
     *      /        |
     *   SOURCE --> CPI
     * 
     * This : 
     *        LOCAL
     *      /       \
     *   SOURCE --> CPI
     * </pre>
     * 
     * 
     * @param start where to start pruning, e.g. RECEPTEUR {DESIGNATION="rec", CONSTAT="ok",
     *        ID_LOCAL=LOCAL{DESIGNATION="local"}}.
     * @param graph the maximum structure wanted, e.g. RECEPTEUR {DESIGNATION=null}.
     * @param keepUnionOfFields only relevant when the same row from this can be reached by more
     *        than one path in <code>graph</code>.
     * 
     *        <pre>
     * Graph :
     *       /- ID_CONTACT_RAPPORT --> CONTACT1
     *  SITE -- ID_CONTACT_CHEF ---> CONTACT2 
     * 
     * This :
     *       /- ID_CONTACT_RAPPORT \
     *  SITE -- ID_CONTACT_CHEF ---> CONTACT
     *        </pre>
     * 
     *        If <code>true</code> keep the union of all fields, if <code>false</code> the
     *        intersection.
     * @return a copy of this no larger than <code>graph</code>, e.g. RECEPTEUR {DESIGNATION="rec"}.
     */
    public final SQLRowValues prune(final SQLRowValues start, final SQLRowValues graph, final boolean keepUnionOfFields) {
        return pruneMap(start, graph, keepUnionOfFields).get(start);
    }

    // private since result isn't trimmed, the values are still all there, not all in the pruned
    // graph
    private final Map<SQLRowValues, SQLRowValues> pruneMap(final SQLRowValues start, final SQLRowValues graph, final boolean keepUnionOfFields) {
        this.containsCheck(start);
        if (!start.getTable().equals(graph.getTable()))
            throw new IllegalArgumentException(start + " is not from the same table as " + graph);
        // there's no way to tell apart 2 referents
        if (!graph.getGraph().hasOneRowPerPath())
            throw new IllegalArgumentException("More than one row for " + graph.printGraph());

        final Map<SQLRowValues, SQLRowValues> map = start.getGraph().deepCopy(false);
        final SQLRowValues res = map.get(start);

        final SetMap<SQLRowValues, String> toRetain = new SetMap<SQLRowValues, String>(new IdentityHashMap<SQLRowValues, Set<String>>(), Mode.NULL_FORBIDDEN);
        // BREADTH_FIRST to stop as soon as this no longer have rows in the graph
        // CycleAllowed since we need to go through every path (e.g. what is a cycle in graph might
        // not be in this :
        // SITE1 -> CONTACT1 -> SITE1 for graph and
        // SITE1 -> CONTACT1 -> SITE2 for this)
        final WalkOptions walkOptions = new WalkOptions(Direction.ANY).setRecursionType(RecursionType.BREADTH_FIRST).setStartIncluded(true).setCycleAllowed(true);
        graph.getGraph().walk(graph, null, new ITransformer<State<Object>, Object>() {
            @Override
            public Object transformChecked(State<Object> input) {
                final SQLRowValues r = input.getCurrent();
                // since we allowed cycles in graph, allow it here
                final Collection<SQLRowValues> rows = res.followPath(input.getPath(), CreateMode.CREATE_NONE, false, true);
                // since we're using BREADTH_FIRST, the next path will be longer so no need to
                // continue if there's already no row
                if (rows.isEmpty())
                    throw new StopRecurseException().setCompletely(false);
                for (final SQLRowValues row : rows) {
                    if (keepUnionOfFields || !toRetain.containsKey(row))
                        toRetain.addAll(row, r.getFields());
                    else
                        toRetain.getCollection(row).retainAll(r.getFields());
                }
                return null;
            }
        }, walkOptions);

        // remove extra fields and flatten if necessary
        for (final Entry<SQLRowValues, Set<String>> e : toRetain.entrySet()) {
            final SQLRowValues r = e.getKey();
            r.retainAll(e.getValue());
            final Set<String> toFlatten = new HashSet<String>();
            for (final Entry<String, SQLRowValues> e2 : r.getForeigns().entrySet()) {
                final SQLRowValues foreign = e2.getValue();
                if (!toRetain.containsKey(foreign)) {
                    toFlatten.add(e2.getKey());
                }
            }
            for (final String fieldToFlatten : toFlatten) {
                r.flatten(fieldToFlatten, ForeignCopyMode.COPY_ID_OR_RM);
            }
        }
        // now, remove referents that aren't in the graph
        for (final SQLRowValues r : new ArrayList<SQLRowValues>(res.getGraph().getItems())) {
            if (!toRetain.containsKey(r)) {
                // only remove links at the border and even then, only remove links to the result :
                // avoid creating a myriad of graphs
                final Set<String> toFlatten = new HashSet<String>();
                for (final Entry<String, SQLRowValues> e2 : r.getForeigns().entrySet()) {
                    final SQLRowValues foreign = e2.getValue();
                    if (toRetain.containsKey(foreign)) {
                        toFlatten.add(e2.getKey());
                    }
                }
                r.removeAll(toFlatten);
            }
        }
        assert res.getGraph().getItems().equals(toRetain.keySet());

        return map;
    }

    // TODO handle referents (and decide how to handle multiple paths to the same node)
    final void grow(final SQLRowValues start, final SQLRowValues toGrow, final boolean checkFields) {
        this.containsCheck(start);
        if (!start.getTable().equals(toGrow.getTable()))
            throw new IllegalArgumentException(start + " is not from the same table as " + toGrow);
        this.walk(start, null, new ITransformer<State<Object>, Object>() {
            @Override
            public Object transformChecked(State<Object> input) {
                final SQLRowValues existing = toGrow.followPath(input.getPath());
                if (existing == null || (checkFields && !existing.getFields().containsAll(input.getCurrent().getFields()))) {
                    final SQLRowValues leaf = toGrow.assurePath(input.getPath());
                    if (leaf.hasID()) {
                        final SQLRowValuesListFetcher fetcher = new SQLRowValuesListFetcher(input.getCurrent());
                        fetcher.setSelTransf(new ITransformer<SQLSelect, SQLSelect>() {
                            @Override
                            public SQLSelect transformChecked(SQLSelect input) {
                                // don't exclude undef otherwise cannot grow eg
                                // LOCAL.ID_FAMILLE_2 = 1
                                input.setExcludeUndefined(false);
                                return input;
                            }
                        });
                        final SQLRowValues fetched = fetcher.fetchOne(leaf.getIDNumber());
                        if (fetched == null)
                            throw new IllegalArgumentException("no row for " + fetcher);
                        leaf.load(fetched, null);
                    } else
                        throw new IllegalArgumentException("cannot expand, missing ID in " + leaf + " at " + input.getPath());
                }
                return null;
            }
        }, RecursionType.BREADTH_FIRST);
    }

    public final String contains(final SQLRowValues start, SQLRowValues graph) {
        return this.contains(start, graph, true);
    }

    /**
     * Whether the tree begining at <code>start</code> contains the tree begining at
     * <code>graph</code>. Ie each path of <code>graph</code> must exist from <code>start</code>.
     * 
     * @param start a SQLRowValues of this.
     * @param graph another SQLRowValues.
     * @param checkFields <code>true</code> to check that each rowValues of this containsAll the
     *        fields of the other.
     * @return a String explaining the first problem, <code>null</code> if <code>graph</code> is
     *         contained in this.
     */
    public final String contains(final SQLRowValues start, SQLRowValues graph, final boolean checkFields) {
        this.containsCheck(start);
        if (!start.getTable().equals(graph.getTable()))
            throw new IllegalArgumentException(start + " is not from the same table as " + graph);
        final StopRecurseException res = graph.getGraph().walk(graph, null, new ITransformer<State<Object>, Object>() {
            @Override
            public Object transformChecked(State<Object> input) {
                final SQLRowValues v = start.followPath(input.getPath());
                if (v == null)
                    throw new StopRecurseException("no " + input.getPath() + " in " + start);
                if (checkFields && !v.getFields().containsAll(input.getCurrent().getFields()))
                    throw new StopRecurseException("at " + input.getPath() + " " + v.getFields() + " does not contain " + input.getCurrent().getFields());

                return null;
            }
        }, RecursionType.BREADTH_FIRST);
        return res == null ? null : res.getMessage();
    }

    /**
     * Return a graphical representation of the tree rooted at <code>root</code>. The returned
     * string is akin to the result of a query :
     * 
     * <pre>
     * BATIMENT[2]      LOCAL[5]         CPI_BT[5]        
     *                  LOCAL[3]         
     *                  LOCAL[2]         CPI_BT[3]        
     *                                   CPI_BT[2]
     * </pre>
     * 
     * In the above example, the BATIMENT has 3 LOCAL. LOCAL[3] is empty and LOCAL[2] has 2 CPI.
     * 
     * @param root the root of the tree to print, eg BATIMENT[2].
     * @param cellLength the length of each cell.
     * @return a string representing the tree.
     */
    public final String printTree(final SQLRowValues root, int cellLength) {
        this.containsCheck(root);
        final Map<SQLRowValues, Integer> ys = new IdentityHashMap<SQLRowValues, Integer>();
        final AtomicInteger currentY = new AtomicInteger(0);
        final Matrix<SQLRowValues> matrix = new Matrix<SQLRowValues>();
        this.walk(root, null, new Closure<State<Object>>() {
            @Override
            public void executeChecked(State<Object> input) {
                // x is easy : it's the length of the path
                // y : for each rowValues we go up the tree and set the y (if not already set)
                // then y is either this value or the current line.
                final SQLRowValues r = input.getCurrent();
                final int y;
                if (ys.containsKey(r))
                    y = ys.get(r);
                else
                    y = currentY.getAndIncrement();
                matrix.put(input.getPath().length(), y, input.getCurrent());

                final SQLRowValues ancestor = input.getPrevious();
                if (ancestor != null) {
                    ancestor.walkGraph(null, new Closure<State<Object>>() {
                        @Override
                        public void executeChecked(State<Object> input) {
                            final SQLRowValues ancestorRow = input.getCurrent();
                            if (!ys.containsKey(ancestorRow))
                                ys.put(ancestorRow, y);
                            else
                                throw new StopRecurseException();
                        }
                    });
                }
            }
        }, RecursionType.DEPTH_FIRST, Direction.REFERENT);

        return matrix.print(cellLength, new ITransformer<SQLRowValues, String>() {
            @Override
            public String transformChecked(SQLRowValues input) {
                if (input == null)
                    return "";
                else if (input.hasID())
                    // avoid requests
                    return input.asRow().simpleToString();
                else
                    return input.getTable().toString();
            }
        });
    }

    public final String printNodes() {
        final StringBuilder sb = new StringBuilder(this.getClass().getSimpleName() + " of " + size() + " nodes:\n");
        for (final SQLRowValues n : getItems()) {
            StringUtils.appendFixedWidthString(sb, String.valueOf(System.identityHashCode(n)), 12, Side.LEFT, ' ', true);
            sb.append(' ');
            sb.append(n.getTable());
            sb.append('\t');
            for (final Map.Entry<String, SQLRowValues> e : n.getForeigns().entrySet()) {
                sb.append(e.getKey());
                sb.append(" -> ");
                sb.append(System.identityHashCode(e.getValue()));
                sb.append(" ; ");
            }
            sb.append(new SQLRowValues(n, ForeignCopyMode.NO_COPY));
            sb.append("\n");
        }
        return sb.toString();
    }

    @Immutable
    public static final class DiffResult {

        private final String firstDiff;
        private final SQLRowValues vals, otherVals;
        // null if difference is trivial
        private final IndexedRows thisRows, otherRows;

        private DiffResult(final String firstDiff, final SQLRowValues vals, final SQLRowValues otherVals) {
            this.firstDiff = firstDiff;
            this.thisRows = null;
            this.otherRows = null;
            this.vals = vals;
            this.otherVals = otherVals;
        }

        private DiffResult(final String firstDiff, final IndexedRows thisRows, final IndexedRows otherRows) {
            super();
            this.firstDiff = firstDiff;
            this.thisRows = thisRows;
            this.otherRows = otherRows;
            assert !this.isEqual() || this.getRows1().getSize() == this.getRows2().getSize();
            this.vals = thisRows.getRow(0);
            this.otherVals = otherRows.getRow(0);
        }

        public String getFirstDifference() {
            return this.firstDiff;
        }

        public final boolean isEqual() {
            return this.getFirstDifference() == null;
        }

        public SQLRowValues getRow1() {
            return this.vals;
        }

        public IndexedRows getRows1() {
            return this.thisRows;
        }

        public SQLRowValues getRow2() {
            return this.otherVals;
        }

        public IndexedRows getRows2() {
            return this.otherRows;
        }

        public final void fillRowMap(final Map<SQLRow, SQLRow> m, final boolean fromFirst) {
            if (!this.isEqual())
                throw new IllegalStateException("Rows are not equal : " + this.getFirstDifference());
            final int stop = this.getRows1().getSize();
            final IndexedRows i1, i2;
            if (fromFirst) {
                i1 = this.getRows1();
                i2 = this.getRows2();
            } else {
                i1 = this.getRows2();
                i2 = this.getRows1();
            }
            for (int i = 0; i < stop; i++) {
                final SQLRow key = i1.getRow(i).asRow();
                final SQLRow value = i2.getRow(i).asRow();
                final SQLRow prev = m.put(key, value);
                if (prev != null)
                    throw new IllegalStateException(key + " already encountered in this : " + this.getRow1());
            }
        }
    }

    private static final class DiffResultBuilder {
        private final SQLRowValues vals, other;
        private IndexedRows valsRows, otherRows;

        private DiffResultBuilder(SQLRowValues vals, SQLRowValues other) {
            super();
            this.vals = vals;
            this.other = other;
        }

        private void setRows(IndexedRows thisRows, IndexedRows otherRows) {
            assert thisRows.getRow(0) == this.vals;
            assert otherRows.getRow(0) == this.other;
            this.valsRows = thisRows;
            this.otherRows = otherRows;
        }

        private DiffResult build(final String firstDiff) {
            if (this.valsRows != null && this.otherRows != null) {
                return new DiffResult(firstDiff, this.valsRows, this.otherRows);
            } else if (firstDiff == null) {
                // if rows are equal but there's no IndexedRows, then the graphs have only one row
                return new DiffResult(firstDiff, new IndexedRows(this.vals), new IndexedRows(this.other));
            } else {
                return new DiffResult(firstDiff, this.vals, this.other);
            }
        }
    }

    public final DiffResult getFirstDifference(final SQLRowValues vals, final SQLRowValues other, final boolean useForeignsOrder, final boolean useFieldsOrder, final boolean usePK) {
        this.containsCheck(vals);
        final DiffResultBuilder b = new DiffResultBuilder(vals, other);
        if (vals == other)
            return b.build(null);
        final int size = this.size();
        // don't call walk() if we can avoid as it is quite costly
        if (size != other.getGraph().size())
            return b.build("different size : " + size + " != " + other.getGraph().size());
        else if (!vals.equalsJustThis(other, useFieldsOrder, false, usePK))
            return b.build("unequal :\n" + vals + " !=\n" + other);
        if (size == 1)
            return b.build(null);

        // BREADTH_FIRST no need to go deep if the first values are not equals
        final IndexedRows thisRows = this.getIndexedRows(vals, RecursionType.BREADTH_FIRST, useForeignsOrder);
        final IndexedRows otherRows = other.getGraph().getIndexedRows(other, RecursionType.BREADTH_FIRST, useForeignsOrder);
        b.setRows(thisRows, otherRows);

        // now check that each row is equal
        // (this works because walk() always goes with the same order, see #FIELD_COMPARATOR and
        // WalkOptions.setForeignsOrderIgnored())
        for (int i = 0; i < size; i++) {
            final SQLRowValues thisVals = thisRows.getRow(i);
            final SQLRowValues oVals = otherRows.getRow(i);
            final Path thisPath = thisRows.getFirstPath(i);
            final Path oPath = otherRows.getFirstPath(i);

            if (!thisPath.equals(oPath))
                return b.build("unequal graph at index " + i + " " + thisPath + " != " + oPath);

            // no need to check again
            assert i != 0 || (thisVals == vals && oVals == other);
            // don't use foreign ID, foreign rows are checked below
            if (i != 0 && !thisVals.equalsJustThis(oVals, useFieldsOrder, false, usePK)) {
                return b.build("unequal local values at " + thisPath + " :\n" + thisVals + " !=\n" + oVals);
            }
            final Map<String, SQLRowValues> thisForeigns = thisVals.getForeigns();
            final Map<String, SQLRowValues> otherForeigns = oVals.getForeigns();
            if (!thisForeigns.keySet().equals(otherForeigns.keySet()))
                return b.build("unequal foreigns at " + thisPath + " :\n" + thisForeigns.keySet() + " !=\n" + otherForeigns.keySet());

            for (final Entry<String, SQLRowValues> e : thisForeigns.entrySet()) {
                final String ff = e.getKey();
                final SQLRowValues thisForeign = e.getValue();
                final SQLRowValues otherForeign = otherForeigns.get(ff);
                if (thisRows.getIndex(thisForeign) != otherRows.getIndex(otherForeign))
                    return b.build("unequal foreign " + ff + " at " + thisPath + " for " + thisVals + " and " + oVals);
                // since they point to the same index, the equality will be checked by the time this
                // loop ends
            }
        }

        return b.build(null);
    }

    static public final class StopRecurseException extends RuntimeException {

        private boolean completely = true;

        public StopRecurseException() {
            super();
        }

        public StopRecurseException(String message, Throwable cause) {
            super(message, cause);
        }

        public StopRecurseException(String message) {
            super(message);
        }

        public StopRecurseException(Throwable cause) {
            super(cause);
        }

        public final StopRecurseException setCompletely(boolean completely) {
            this.completely = completely;
            return this;
        }

        public final boolean isCompletely() {
            return this.completely;
        }
    }

    static public final class State<T> {
        private final List<SQLRowValues> valsPath;
        private final Path path;
        private T acc;
        private final ITransformer<State<T>, T> closure;

        State(List<SQLRowValues> valsPath, Path path, T acc, ITransformer<State<T>, T> closure) {
            super();
            this.valsPath = valsPath;
            this.path = path;
            this.acc = acc;
            this.closure = closure;
        }

        public SQLField getFrom() {
            return this.path.length() == 0 ? null : this.path.getSingleField(this.path.length() - 1);
        }

        /**
         * Whether the last step of the path was taken backwards through a foreign field. Eg
         * <code>true</code> if the path is BATIMENT,LOCAL.ID_BATIMENT, and <code>false</code> if
         * LOCAL,LOCAL.ID_BATIMENT.
         * 
         * @return <code>true</code> if the last step was backwards.
         */
        public final boolean isBackwards() {
            if (this.path.length() == 0)
                throw new IllegalStateException("empty path");
            return this.path.isBackwards(this.path.length() - 1);
        }

        /**
         * Compute the new acc.
         * 
         * @return whether this recursion should continue.
         */
        StopRecurseException compute() {
            try {
                this.acc = this.closure.transformChecked(this);
                return null;
            } catch (StopRecurseException e) {
                return e;
            }
        }

        @Override
        public String toString() {
            return this.getClass().getSimpleName() + " path: " + this.path + " current node: " + this.getCurrent() + " current acc: " + this.getAcc();
        }

        public final SQLRowValues getCurrent() {
            return CollectionUtils.getLast(this.valsPath);
        }

        public final SQLRowValues getPrevious() {
            return CollectionUtils.getNoExn(this.valsPath, this.valsPath.size() - 2);
        }

        public final List<SQLRowValues> getValsPath() {
            return this.valsPath;
        }

        final boolean identityContains(final SQLRowValues vals) {
            return CollectionUtils.identityContains(this.valsPath, vals);
        }

        boolean hasCycle() {
            final int size = this.valsPath.size();
            if (size < 2)
                return false;
            return CollectionUtils.identityContains(this.valsPath.subList(0, size - 1), this.valsPath.get(size - 1));
        }

        public Path getPath() {
            return this.path;
        }

        public T getAcc() {
            return this.acc;
        }
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " " + this.links;
    }

    private static class Link {
        private final SQLRowValues src;
        private final SQLField f;
        private final SQLRowValues dest;

        public Link(final SQLRowValues src) {
            this(src, null, null);
        }

        public Link(final SQLRowValues src, final SQLField f, final SQLRowValues dest) {
            if (src == null)
                throw new NullPointerException("src is null");
            assert (f == null && dest == null) || (dest != null && f.getTable() == src.getTable());
            this.src = src;
            this.f = f;
            this.dest = dest;
        }

        public final SQLRowValues getSrc() {
            return this.src;
        }

        public final SQLRowValues getDest() {
            return this.dest;
        }

        public final SQLField getField() {
            return this.f;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + System.identityHashCode(this.src);
            result = prime * result + System.identityHashCode(this.dest);
            result = prime * result + ((this.f == null) ? 0 : this.f.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;

            final Link other = (Link) obj;
            return this.src == other.src && this.dest == other.dest && CompareUtils.equals(this.f, other.f);
        }

        @Override
        public String toString() {
            return this.getClass().getSimpleName() + " " + System.identityHashCode(this.src) + (this.f == null ? "" : " " + this.f.getName() + " " + System.identityHashCode(this.dest));
        }
    }

    private static final class StoringLink extends Link {

        private Number destID;

        private StoringLink(Link l) {
            super(l.getSrc(), l.getField(), l.getDest());
            this.destID = null;
        }

        public final boolean canStore() {
            return this.getDest() == null || this.destID != null;
        }

        @Override
        public String toString() {
            return super.toString() + " destID: " + this.destID;
        }
    }

    public static final class StoreResult {
        private final Map<SQLRowValues, Node> nodes;

        public StoreResult(final Map<SQLRowValues, Node> nodes) {
            this.nodes = nodes;
        }

        final SQLRowValues getRowValuesFor(final SQLRow stored) {
            for (final Entry<SQLRowValues, Node> e : this.nodes.entrySet()) {
                if (e.getValue().getStoredRow().equals(stored))
                    return e.getKey();
            }
            return null;
        }

        /**
         * Get the set of rows at the time the {@link SQLRowValuesCluster#store(StoreMode)} was
         * performed. NOTE : the field values of the returned rows might have changed since the
         * store but the other methods of this class use the instance identity.
         * 
         * @return the rows that were stored.
         */
        final Set<SQLRowValues> getRowValues() {
            return Collections.unmodifiableSet(this.nodes.keySet());
        }

        public final int getStoredCount() {
            return this.nodes.size();
        }

        public final SQLRow getStoredRow(SQLRowValues vals) {
            return this.nodes.get(vals).getStoredRow();
        }

        public final SQLRowValues getStoredValues(SQLRowValues vals) {
            return this.nodes.get(vals).getStoredValues();
        }

        final List<SQLTableEvent> getEvents(SQLRowValues vals) {
            return this.nodes.get(vals).getEvents();
        }
    }

    private static final class Node {

        // don't use noLink since it might contains foreigns if store() was just called
        // or it might be out of sync with vals since the graph is only recreated on foreign change
        /** vals without any links */
        private final SQLRowValues noLink;
        private final List<SQLTableEvent> modif;

        private Node(final SQLRowValues vals) {
            this.modif = new ArrayList<SQLTableEvent>();
            this.noLink = new SQLRowValues(vals, ForeignCopyMode.NO_COPY);
        }

        private SQLTableEvent store(final boolean fetchStoredRow, StoreMode mode) throws SQLException {
            return this.store(fetchStoredRow, mode, true);
        }

        private SQLTableEvent store(final boolean fetchStoredRow, StoreMode mode, final boolean setRowValues) throws SQLException {
            assert !this.isStored();
            final SQLTableEvent evt = this.addEvent(mode.execOn(this.noLink, fetchStoredRow));
            if (fetchStoredRow && evt.getRow() != null && setRowValues)
                evt.setRowValues(evt.getRow().asRowValues());
            return evt;
        }

        private SQLTableEvent update(final boolean fetchStoredRow) throws SQLException {
            assert this.isStored();

            // fields that have been updated since last store
            final Set<String> fieldsToUpdate = new HashSet<String>(this.noLink.getFields());
            fieldsToUpdate.removeAll(this.getEvent().getFieldNames());
            assert fieldsToUpdate.size() > 0;

            final SQLRowValues updatingVals = this.getStoredRow().createEmptyUpdateRow();
            updatingVals.load(this.noLink, fieldsToUpdate);

            final SQLTableEvent evt = new Node(updatingVals).store(fetchStoredRow, StoreMode.COMMIT, false);
            // Update previous rowValues, and use it for the new event
            // that way there's only one graph of rowValues (with the final values) for all events.
            // Load all fields since updating 1 field might change the value of another (e.g.
            // with a trigger).
            if (fetchStoredRow && evt.getRow() != null) {
                this.getStoredValues().load(evt.getRow(), null);
                evt.setRowValues(this.getStoredValues());
            }
            return this.addEvent(evt);
        }

        public final boolean isStored() {
            return this.modif.size() > 0;
        }

        // the last stored row, can be null for non-rowable tables
        public final SQLRow getStoredRow() {
            return this.getEvent() == null ? null : this.getEvent().getRow();
        }

        public final SQLRowValues getStoredValues() {
            // all events have the same values
            return this.getEvent() == null ? null : this.getEvent().getRowValues();
        }

        // the last event
        private final SQLTableEvent getEvent() {
            return CollectionUtils.getLast(this.modif);
        }

        final List<SQLTableEvent> getEvents() {
            return Collections.unmodifiableList(this.modif);
        }

        private final SQLTableEvent addEvent(SQLTableEvent evt) {
            if (evt == null)
                throw new IllegalStateException("Couldn't update missing row  " + this.noLink);
            this.modif.add(evt);
            return evt;
        }

        @Override
        public String toString() {
            return this.getClass().getSimpleName() + " " + this.noLink;
        }
    }

    /**
     * What to do on each rowVals.
     * 
     * @author Sylvain
     */
    public static abstract class StoreMode {
        abstract SQLTableEvent execOn(SQLRowValues vals, final boolean fetchStoredRow) throws SQLException;

        public static final StoreMode COMMIT = new Commit();
        public static final StoreMode INSERT = new Insert(false, false);
        public static final StoreMode INSERT_VERBATIM = new Insert(true, true);
    }

    public static class Insert extends StoreMode {

        private final boolean insertPK;
        private final boolean insertOrder;

        public Insert(boolean insertPK, boolean insertOrder) {
            super();
            this.insertPK = insertPK;
            this.insertOrder = insertOrder;
        }

        @Override
        SQLTableEvent execOn(SQLRowValues vals, final boolean fetchStoredRow) throws SQLException {
            final Set<SQLField> autoFields = new HashSet<SQLField>();
            if (!this.insertPK)
                autoFields.addAll(vals.getTable().getPrimaryKeys());
            if (!this.insertOrder)
                autoFields.add(vals.getTable().getOrderField());
            return vals.insertJustThis(fetchStoredRow, autoFields);
        }
    }

    public static class Commit extends StoreMode {

        @Override
        SQLTableEvent execOn(SQLRowValues vals, final boolean fetchStoredRow) throws SQLException {
            return vals.commitJustThis(fetchStoredRow);
        }
    }

    // * listeners

    // create if necessary
    private final Map<SQLRowValues, List<ValueChangeListener>> getListeners() {
        if (this.listeners == null)
            this.listeners = new IdentityHashMap<SQLRowValues, List<ValueChangeListener>>(4);
        return this.listeners;
    }

    final void addValueListener(final SQLRowValues vals, ValueChangeListener l) {
        this.containsCheck(vals);
        List<ValueChangeListener> list = this.getListeners().get(vals);
        if (list == null) {
            list = new ArrayList<ValueChangeListener>();
            this.getListeners().put(vals, list);
        }
        list.add(l);
    }

    final void removeValueListener(final SQLRowValues vals, ValueChangeListener l) {
        if (this.listeners != null && this.listeners.containsKey(vals)) {
            final List<ValueChangeListener> list = this.listeners.get(vals);
            list.remove(l);
            // never leave an empty list so that hasListeners() works
            if (list.size() == 0)
                this.listeners.remove(vals);
        }
    }

    final void fireModification(SQLRowValues vals, String fieldName, Object newValue) {
        if (hasListeners())
            fireModification(new ValueChangeEvent(vals, fieldName, newValue));
    }

    final void fireModification(SQLRowValues vals, Map<String, ?> m) {
        if (hasListeners())
            fireModification(new ValueChangeEvent(vals, m));
    }

    final void fireModification(SQLRowValues vals, Set<String> removed) {
        if (hasListeners())
            fireModification(new ValueChangeEvent(vals, removed));
    }

    private final void fireModification(final ValueChangeEvent evt) {
        for (final List<ValueChangeListener> list : this.listeners.values())
            for (ValueChangeListener l : list)
                l.valueChange(evt);
    }

    final void fireModification(final ReferentChangeEvent evt) {
        if (referentFireNeeded(evt.isAddition())) {
            for (final List<ValueChangeListener> list : this.listeners.values())
                for (ValueChangeListener l : list)
                    l.referentChange(evt);
        }
    }

    final boolean referentFireNeeded(final boolean put) {
        // if isAddition there will be a valueChange() for the same put()
        return this.hasListeners() && !put;
    }

    final boolean hasListeners() {
        return this.listeners != null && this.listeners.size() > 0;
    }

    public static class ValueChangeEvent extends EventObject {

        private final Map<String, ?> added;
        private final Set<String> removed;

        private ValueChangeEvent(final SQLRowValues vals, final Map<String, ?> m) {
            super(vals);
            this.added = Collections.unmodifiableMap(m);
            this.removed = Collections.emptySet();
        }

        public ValueChangeEvent(SQLRowValues vals, String fieldName, Object newValue) {
            super(vals);
            this.added = Collections.singletonMap(fieldName, newValue);
            this.removed = Collections.emptySet();
        }

        public ValueChangeEvent(SQLRowValues vals, Set<String> removed) {
            super(vals);
            this.added = Collections.emptyMap();
            this.removed = Collections.unmodifiableSet(removed);
        }

        @Override
        public SQLRowValues getSource() {
            return (SQLRowValues) super.getSource();
        }

        public final Set<String> getAddedFields() {
            return this.added.keySet();
        }

        public final Set<String> getRemovedFields() {
            return this.removed;
        }

        public final Map<String, ?> getAddedValues() {
            return this.added;
        }

        @Override
        public String toString() {
            return super.toString() + " added : " + getAddedFields() + " removed: " + getRemovedFields();
        }
    }

    /**
     * This listener is notified whenever a rowValues changes. Note that
     * {@link ValueChangeListener#referentChange(ReferentChangeEvent)} is only called for
     * {@link ReferentChangeEvent#isRemoval() removals} since for addition there will be a
     * {@link ValueChangeListener#valueChange(ValueChangeEvent)}.
     * 
     * @author Sylvain CUAZ
     */
    public static interface ValueChangeListener extends ReferentChangeListener {

        void valueChange(ValueChangeEvent evt);

    }
}
