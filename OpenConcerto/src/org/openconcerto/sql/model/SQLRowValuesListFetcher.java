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
import org.openconcerto.sql.model.SQLRowValuesCluster.State;
import org.openconcerto.sql.model.SQLRowValuesCluster.WalkOptions;
import org.openconcerto.sql.model.graph.Link.Direction;
import org.openconcerto.sql.model.graph.Path;
import org.openconcerto.sql.model.graph.Step;
import org.openconcerto.utils.CollectionMap2Itf.ListMapItf;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.CompareUtils;
import org.openconcerto.utils.CopyUtils;
import org.openconcerto.utils.ListMap;
import org.openconcerto.utils.RTInterruptedException;
import org.openconcerto.utils.RecursionType;
import org.openconcerto.utils.Tuple2;
import org.openconcerto.utils.cc.ITransformer;
import org.openconcerto.utils.cc.Transformer;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.dbutils.ResultSetHandler;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;

/**
 * Construct a list of linked SQLRowValues from one request.
 * 
 * @author Sylvain
 */
@ThreadSafe
public class SQLRowValuesListFetcher {

    /**
     * Create an ordered fetcher with the necessary grafts to fetch the passed graph.
     * 
     * @param graph what to fetch, can be any tree.
     * @return the fetcher.
     */
    public static SQLRowValuesListFetcher create(final SQLRowValues graph) {
        // ORDER shouldn't slow down the query and it makes the result predictable and repeatable
        return create(graph, true);
    }

    public static SQLRowValuesListFetcher create(final SQLRowValues graph, final boolean ordered) {
        // path -> longest referent only path
        // i.e. map each path to the main fetcher or a referent graft
        final Map<Path, Path> handledPaths = new HashMap<Path, Path>();
        final Path emptyPath = Path.get(graph.getTable());
        handledPaths.put(emptyPath, emptyPath);
        // find out referent only paths (yellow in the diagram)
        graph.getGraph().walk(graph, null, new ITransformer<State<Object>, Object>() {
            @Override
            public Path transformChecked(State<Object> input) {
                final Path p = input.getPath();
                for (int i = p.length(); i > 0; i--) {
                    final Path subPath = p.subPath(0, i);
                    if (handledPaths.containsKey(subPath))
                        break;
                    handledPaths.put(subPath, p);
                }
                return null;
            }
        }, RecursionType.DEPTH_FIRST, Direction.REFERENT);

        // find out needed grafts
        final ListMap<Path, SQLRowValuesListFetcher> grafts = new ListMap<Path, SQLRowValuesListFetcher>();
        graph.getGraph().walk(graph, null, new ITransformer<State<Object>, Object>() {
            @Override
            public Path transformChecked(State<Object> input) {
                final Path p = input.getPath();
                if (!handledPaths.containsKey(p)) {
                    final Path pMinusLast = p.minusLast();
                    if (!input.isBackwards()) {
                        // Forwards can be fetched by existing fetcher (blue in the diagram)
                        final Path existingRefPath = handledPaths.get(pMinusLast);
                        assert existingRefPath != null;
                        handledPaths.put(p, existingRefPath);
                    } else {
                        // Backwards needs another fetcher
                        if (!grafts.containsKey(pMinusLast)) {
                            final SQLRowValues copy = graph.deepCopy();
                            final SQLRowValues graftNode = copy.followPath(pMinusLast);
                            graftNode.clear();
                            final SQLRowValues previous = copy.followPath(pMinusLast.minusLast());
                            assert p.getStep(-2).isForeign();
                            previous.remove(p.getStep(-2).getSingleField().getName());
                            // don't recurse forever
                            if (previous.getGraph() == graftNode.getGraph())
                                throw new IllegalArgumentException("Graph is not a tree");
                            // ATTN pMinusLast might not be on the main fetcher so don't graft now
                            // also we can only graft non empty descendant path fetchers (plus
                            // removing a fetcher saves one request)
                            final SQLRowValuesListFetcher rec = create(graftNode, ordered);
                            final Collection<SQLRowValuesListFetcher> ungrafted = rec.ungraft();
                            if (ungrafted == null || ungrafted.size() == 0) {
                                // i.e. only one referent and thus graft not necessary
                                assert rec.descendantPath.length() > 0;
                                grafts.add(pMinusLast, rec);
                            } else {
                                grafts.addAll(pMinusLast, ungrafted);
                            }
                        }
                        throw new SQLRowValuesCluster.StopRecurseException().setCompletely(false);
                    }
                }
                return null;
            }
        }, new WalkOptions(Direction.ANY).setRecursionType(RecursionType.BREADTH_FIRST).setStartIncluded(false));

        final Set<Path> refPaths = new HashSet<Path>(handledPaths.values());
        // remove the main fetcher
        refPaths.remove(emptyPath);
        // fetchers for the referent paths (yellow part)
        final Map<Path, SQLRowValuesListFetcher> graftedFetchers;
        // create the main fetcher and grafts
        final SQLRowValuesListFetcher res;
        if (refPaths.size() == 1) {
            res = new SQLRowValuesListFetcher(graph, refPaths.iterator().next());
            graftedFetchers = Collections.emptyMap();
        } else {
            res = new SQLRowValuesListFetcher(graph, false);
            graftedFetchers = new HashMap<Path, SQLRowValuesListFetcher>();
            if (refPaths.size() > 0) {
                final Path graftPath = new Path(graph.getTable());
                final SQLRowValues copy = graph.deepCopy();
                copy.clear();
                for (final Path refPath : refPaths) {
                    final SQLRowValuesListFetcher f = new SQLRowValuesListFetcher(copy, refPath, true).setOrdered(ordered);
                    res.graft(f, graftPath);
                    graftedFetchers.put(refPath, f);
                }
            }
        }
        res.setOrdered(ordered);

        // now graft recursively created grafts
        for (final Entry<Path, ? extends Collection<SQLRowValuesListFetcher>> e : grafts.entrySet()) {
            final Path graftPath = e.getKey();
            final Path refPath = handledPaths.get(graftPath);
            // can be grafted on the main fetcher or on the referent fetchers
            final SQLRowValuesListFetcher f = graftedFetchers.containsKey(refPath) ? graftedFetchers.get(refPath) : res;
            for (final SQLRowValuesListFetcher recFetcher : e.getValue())
                f.graft(recFetcher, graftPath);
        }
        return res;
    }

    // return the referent single link path starting from graph
    private static Path computePath(SQLRowValues graph) {
        // check that there's only one referent for each row
        // (otherwise huge joins, e.g. LOCAL<-CPI,SOURCE,RECEPTEUR,etc.)
        final AtomicReference<Path> res = new AtomicReference<Path>(null);
        graph.getGraph().walk(graph, null, new ITransformer<State<Path>, Path>() {
            @Override
            public Path transformChecked(State<Path> input) {
                final Collection<SQLRowValues> referentRows = input.getCurrent().getReferentRows();
                final int size = referentRows.size();
                if (size > 1) {
                    // remove the foreign rows which are all the same (since they point to
                    // current) so the exn is more legible
                    final List<SQLRowValues> toPrint = SQLRowValues.trim(referentRows);
                    throw new IllegalArgumentException(input.getCurrent() + " is referenced by " + toPrint + "\nat " + input.getPath());
                } else if (size == 0) {
                    if (res.get() == null)
                        res.set(input.getPath());
                    else
                        throw new IllegalStateException();
                }
                return input.getAcc();
            }
        }, RecursionType.BREADTH_FIRST, Direction.REFERENT);
        // since includeStart=true
        assert res.get() != null;
        return res.get();
    }

    static private final ListMap<Tuple2<Path, Number>, SQLRowValues> createCollectionMap() {
        // we need a List in merge()
        return new ListMap<Tuple2<Path, Number>, SQLRowValues>() {
            @Override
            public List<SQLRowValues> createCollection(Collection<? extends SQLRowValues> v) {
                final List<SQLRowValues> res = new ArrayList<SQLRowValues>(8);
                res.addAll(v);
                return res;
            }
        };
    }

    // unmodifiable
    private final SQLRowValues graph;
    private final Path descendantPath;
    @GuardedBy("this")
    private List<ITransformer<SQLSelect, SQLSelect>> selTransf;
    @GuardedBy("this")
    private Number selID;
    @GuardedBy("this")
    private Set<Path> ordered;
    @GuardedBy("this")
    private boolean descendantsOrdered;
    @GuardedBy("this")
    private SQLRowValues minGraph;
    @GuardedBy("this")
    private boolean includeForeignUndef;
    @GuardedBy("this")
    private SQLSelect frozen;
    @GuardedBy("this")
    private boolean freezeRows;
    // graftPlace -> {referent path -> fetcher}, unmodifiable
    @GuardedBy("this")
    private Map<Path, Map<Path, SQLRowValuesListFetcher>> grafts;

    /**
     * Construct a new instance with the passed graph of SQLRowValues.
     * 
     * @param graph what SQLRowValues should be returned by {@link #fetch()}, eg <code>new
     *        SQLRowValues("SITE").setAllToNull().put("ID_CONTACT", new SQLRowValues("CONTACT"))</code>
     *        to return all sites (with all their fields) with their associated contacts.
     */
    public SQLRowValuesListFetcher(SQLRowValues graph) {
        this(graph, false);
    }

    /**
     * Construct a new instance with the passed graph of SQLRowValues. Eg if <code>graph</code> is a
     * BATIMENT which points to SITE, is pointed by LOCAL, CPI_BT and <code>referents</code> is
     * <code>true</code>, {@link #fetch()} could return
     * 
     * <pre>
     * SITE[2]  BATIMENT[2]     LOCAL[2]    CPI_BT[3]       
     *                                      CPI_BT[2]       
     *                          LOCAL[3]        
     *                          LOCAL[5]    CPI_BT[5]
     * SITE[7]  BATIMENT[3]     LOCAL[4]    CPI_BT[4]       
     * SITE[7]  BATIMENT[4]
     * </pre>
     * 
     * @param graph what SQLRowValues should be returned by {@link #fetch()}, eg <code>new
     *        SQLRowValues("SITE").setAllToNull().put("ID_CONTACT", new SQLRowValues("CONTACT"))</code>
     *        to return all sites (with all their fields) with their associated contacts.
     * @param referents <code>true</code> if referents to <code>graph</code> should also be fetched.
     */
    public SQLRowValuesListFetcher(SQLRowValues graph, final boolean referents) {
        this(graph, referents ? computePath(graph) : null);
    }

    /**
     * Construct a new instance.
     * 
     * @param graph what SQLRowValues should be returned by {@link #fetch()}.
     * @param referentPath a {@link Path#isSingleField() single link} path from the primary table,
     *        <code>null</code> meaning don't fetch referent rows.
     */
    public SQLRowValuesListFetcher(SQLRowValues graph, final Path referentPath) {
        this(graph, referentPath, true);
    }

    /**
     * Construct a new instance.
     * 
     * @param graph what SQLRowValues should be returned by {@link #fetch()}.
     * @param referentPath a {@link Path#isSingleField() single link} path from the primary table,
     *        <code>null</code> meaning don't fetch referent rows.
     * @param prune if <code>true</code> the graph will be pruned to only contain
     *        <code>referentPath</code>. If <code>false</code> the graph will be kept as is, which
     *        can produce undefined results if there exist more than one referent row outside of
     *        <code>referentPath</code>.
     */
    SQLRowValuesListFetcher(final SQLRowValues graph, final Path referentPath, final boolean prune) {
        super();
        this.graph = graph.deepCopy();

        this.descendantPath = referentPath == null ? Path.get(graph.getTable()) : referentPath;
        if (!this.descendantPath.isDirection(Direction.REFERENT))
            throw new IllegalArgumentException("path is not (exclusively) referent : " + this.descendantPath);
        final SQLRowValues descRow = this.graph.followPath(this.descendantPath);
        if (descRow == null)
            throw new IllegalArgumentException("path is not contained in the passed rowValues : " + referentPath + "\n" + this.graph.printTree());
        // followPath() do the following check
        assert this.descendantPath.getFirst() == this.graph.getTable() && this.descendantPath.isSingleField();

        if (prune) {
            this.graph.getGraph().walk(descRow, null, new ITransformer<State<Object>, Object>() {
                @Override
                public Object transformChecked(State<Object> input) {
                    if (input.getFrom() == null) {
                        input.getCurrent().clearReferents();
                    } else {
                        input.getCurrent().retainReferent(input.getPrevious());
                    }
                    return null;
                }
            }, RecursionType.BREADTH_FIRST, Direction.FOREIGN);
        }

        // always need IDs
        for (final SQLRowValues curr : this.graph.getGraph().getItems()) {
            // don't overwrite existing values
            if (!curr.hasID())
                curr.setID(null);
        }

        this.graph.getGraph().freeze();

        synchronized (this) {
            this.selTransf = Collections.emptyList();
            this.selID = null;
            this.ordered = Collections.<Path> emptySet();
            this.descendantsOrdered = false;
            this.minGraph = null;
            this.includeForeignUndef = false;
            this.frozen = null;
            this.freezeRows = false;
            this.grafts = Collections.emptyMap();
        }
    }

    // be aware that the new instance will share the same selTransf, and if it doesn't directly
    // (with copyTransf) some state can still be shared
    private SQLRowValuesListFetcher(SQLRowValuesListFetcher f, final boolean copyTransf) {
        synchronized (f) {
            this.graph = f.getGraph().toImmutable();
            this.descendantPath = f.getReferentPath();
            // can't deadlock since this hasn't been published
            synchronized (this) {
                this.selTransf = copyTransf ? CopyUtils.copy(f.selTransf) : f.selTransf;
                this.selID = f.getSelID();
                this.ordered = f.getOrder();
                this.descendantsOrdered = f.areReferentsOrdered();
                this.minGraph = f.minGraph == null ? null : f.minGraph.toImmutable();
                this.includeForeignUndef = f.includeForeignUndef;
                // a new instance is always mutable
                this.frozen = null;

                this.freezeRows = f.freezeRows;

                // Recursively copy grafts
                final Map<Path, Map<Path, SQLRowValuesListFetcher>> outerMutable = new HashMap<Path, Map<Path, SQLRowValuesListFetcher>>(f.grafts);
                for (final Entry<Path, Map<Path, SQLRowValuesListFetcher>> e : outerMutable.entrySet()) {
                    final Map<Path, SQLRowValuesListFetcher> innerMutable = new HashMap<Path, SQLRowValuesListFetcher>(e.getValue());
                    for (final Entry<Path, SQLRowValuesListFetcher> innerEntry : innerMutable.entrySet()) {
                        innerEntry.setValue(new SQLRowValuesListFetcher(innerEntry.getValue(), copyTransf));
                    }
                    e.setValue(Collections.unmodifiableMap(innerMutable));
                }
                this.grafts = Collections.unmodifiableMap(outerMutable);
            }
        }
    }

    /**
     * Get a frozen version of this. If not already {@link #isFrozen() frozen}, copy this and its
     * grafts and {@link #freeze()} the copy.
     * 
     * @return <code>this</code> if already frozen, otherwise a frozen copy of <code>this</code>.
     */
    public final SQLRowValuesListFetcher toUnmodifiable() {
        synchronized (this) {
            if (this.isFrozen())
                return this;
            // no need to try to deep copy since we freeze before releasing the lock
            return new SQLRowValuesListFetcher(this, false).freeze();
        }
    }

    /**
     * Make this instance immutable. Ie all setters will now throw {@link IllegalStateException}.
     * Furthermore the request will be computed now once and for all, so as not to be subject to
     * outside modification by {@link #getSelectTransformers()}.
     * 
     * @return this.
     */
    public synchronized final SQLRowValuesListFetcher freeze() {
        if (!this.isFrozen()) {
            this.frozen = new SQLSelect(this.getReq());
            for (final Map<Path, SQLRowValuesListFetcher> m : this.grafts.values()) {
                for (final SQLRowValuesListFetcher f : m.values())
                    f.freeze();
            }
        }
        return this;
    }

    public synchronized final boolean isFrozen() {
        return this.frozen != null;
    }

    private final void checkFrozen() {
        if (this.isFrozen())
            throw new IllegalStateException("this has been frozen: " + this);
    }

    /**
     * Whether the rows returned by {@link #fetch()} are {@link SQLRowValues#isFrozen()
     * unmodifiable}.
     * 
     * @param b <code>true</code> to make all rows unmodifiable.
     */
    public synchronized final void setReturnedRowsUnmodifiable(final boolean b) {
        this.checkFrozen();
        this.freezeRows = b;
    }

    /**
     * Whether the rows returned by {@link #fetch()} are {@link SQLRowValues#isFrozen()
     * unmodifiable}.
     * 
     * @return <code>true</code> if all rows are returned unmodifiable.
     */
    public synchronized boolean areReturnedRowsUnmodifiable() {
        return this.freezeRows;
    }

    public SQLRowValues getGraph() {
        return this.graph;
    }

    public final Path getReferentPath() {
        return this.descendantPath;
    }

    /**
     * Whether to include undefined rows (of tables other than the graph's).
     * 
     * @param includeForeignUndef <code>true</code> to include undefined rows.
     */
    public synchronized final void setIncludeForeignUndef(boolean includeForeignUndef) {
        this.checkFrozen();
        this.includeForeignUndef = includeForeignUndef;
    }

    /**
     * Require that only rows with values for the full graph are returned. Eg if the graph is CPI ->
     * OBS, setting this to <code>true</code> will excludes CPI without OBS.
     * 
     * @param b <code>true</code> if only full rows should be fetched.
     */
    public synchronized final void setFullOnly(boolean b) {
        this.checkFrozen();
        if (b)
            this.minGraph = this.getGraph().deepCopy();
        else
            this.minGraph = null;
    }

    public synchronized final void requirePath(final Path p) {
        this.checkFrozen();
        if (this.getGraph().followPath(p) == null)
            throw new IllegalArgumentException("Path not included in this graph : " + p + "\n" + this.getGraph().printGraph());
        if (this.minGraph == null)
            this.minGraph = new SQLRowValues(getGraph().getTable());
        this.minGraph.assurePath(p);
    }

    private synchronized final boolean isPathRequired(final Path p) {
        return this.minGraph != null && this.minGraph.followPath(p) != null;
    }

    private boolean fetchReferents() {
        return this.descendantPath.length() > 0;
    }

    /**
     * To modify the query before execution.
     * 
     * @param selTransf will be passed the query which has been constructed, and the return value
     *        will be actually executed, can be <code>null</code>.
     */
    public synchronized void setSelTransf(ITransformer<SQLSelect, SQLSelect> selTransf) {
        this.checkFrozen();
        this.selTransf = selTransf != null ? Collections.singletonList(selTransf) : Collections.<ITransformer<SQLSelect, SQLSelect>> emptyList();
    }

    public void clearSelTransf() {
        this.setSelTransf(null);
    }

    public void appendSelTransf(ITransformer<SQLSelect, SQLSelect> selTransf) {
        this.addSelTransf(selTransf, -1);
    }

    public void prependSelTransf(ITransformer<SQLSelect, SQLSelect> selTransf) {
        this.addSelTransf(selTransf, 0);
    }

    public synchronized void addSelTransf(ITransformer<SQLSelect, SQLSelect> selTransf, final int index) {
        this.checkFrozen();
        if (selTransf != null) {
            final List<ITransformer<SQLSelect, SQLSelect>> copy = new ArrayList<ITransformer<SQLSelect, SQLSelect>>(this.selTransf);
            final int size = copy.size();
            final int realIndex = index < 0 ? size + index + 1 : index;
            copy.add(realIndex, selTransf);
            this.selTransf = Collections.unmodifiableList(copy);
        }
    }

    public synchronized boolean removeSelTransf(ITransformer<SQLSelect, SQLSelect> selTransf) {
        this.checkFrozen();
        if (selTransf != null) {
            final List<ITransformer<SQLSelect, SQLSelect>> copy = new ArrayList<ITransformer<SQLSelect, SQLSelect>>(this.selTransf);
            if (copy.remove(selTransf)) {
                this.selTransf = Collections.unmodifiableList(copy);
                return true;
            }
        }
        return false;
    }

    public synchronized final ITransformer<SQLSelect, SQLSelect> getSelTransf() {
        if (this.selTransf.size() > 1)
            throw new IllegalStateException("More than one transformer");
        return CollectionUtils.getFirst(this.selTransf);
    }

    public synchronized final List<ITransformer<SQLSelect, SQLSelect>> getSelectTransformers() {
        return this.selTransf;
    }

    /**
     * Add a where in {@link #getReq()} to restrict the primary key.
     * 
     * @param selID an ID for the primary key, <code>null</code> to not filter.
     */
    public synchronized void setSelID(Number selID) {
        this.checkFrozen();
        this.selID = selID;
    }

    public synchronized final Number getSelID() {
        return this.selID;
    }

    /**
     * Whether to add ORDER BY in {@link #getReq()}.
     * 
     * @param b <code>true</code> if the query should be ordered.
     * @return this.
     */
    public synchronized final SQLRowValuesListFetcher setOrdered(final boolean b) {
        this.setOrder(b ? Collections.singleton(Path.get(getGraph().getTable())) : Collections.<Path> emptySet(), true);
        this.setReferentsOrdered(b, false);
        return this;
    }

    public final SQLRowValuesListFetcher setOrder(final List<Path> order) {
        return this.setOrder(order, false);
    }

    private synchronized final SQLRowValuesListFetcher setOrder(final Collection<Path> order, final boolean safeVal) {
        this.checkFrozen();
        for (final Path p : order)
            if (this.getGraph().followPath(p) == null)
                throw new IllegalArgumentException("Path not in this " + p);
        this.ordered = safeVal ? (Set<Path>) order : Collections.unmodifiableSet(new LinkedHashSet<Path>(order));
        return this;
    }

    public synchronized final Set<Path> getOrder() {
        return this.ordered;
    }

    /**
     * Whether to order referent rows in this fetcher.
     * 
     * @param b <code>true</code> to order referent rows starting from the primary node, e.g. if the
     *        graph is
     * 
     *        <pre>
     * *SITE* <- BATIMENT <- LOCAL
     *        </pre>
     * 
     *        then this will cause ORDER BY BATIMENT.ORDRE, LOCAL.ORDRE.
     * @param rec if grafts should also be changed.
     * @return this.
     */
    public synchronized final SQLRowValuesListFetcher setReferentsOrdered(final boolean b, final boolean rec) {
        this.descendantsOrdered = b;
        if (rec) {
            for (final Map<Path, SQLRowValuesListFetcher> m : this.grafts.values()) {
                for (final SQLRowValuesListFetcher f : m.values())
                    f.setReferentsOrdered(b, rec);
            }
        }
        return this;
    }

    public synchronized final boolean areReferentsOrdered() {
        return this.descendantsOrdered;
    }

    public final SQLRowValuesListFetcher graft(final SQLRowValuesListFetcher other) {
        return this.graft(other, Path.get(getGraph().getTable()));
    }

    public final SQLRowValuesListFetcher graft(final SQLRowValues other, Path graftPath) {
        // with referents otherwise it's useless
        return this.graft(new SQLRowValuesListFetcher(other, true), graftPath);
    }

    /**
     * Graft a fetcher on this graph.
     * 
     * @param other another instance fetching rows of the table at <code>graftPath</code>.
     * @param graftPath a path from this values to where <code>other</code> rows should be grafted.
     * @return the previous fetcher.
     */
    public synchronized final SQLRowValuesListFetcher graft(final SQLRowValuesListFetcher other, Path graftPath) {
        checkFrozen();
        if (this == other)
            throw new IllegalArgumentException("trying to graft onto itself");
        if (other.getGraph().getTable() != graftPath.getLast())
            throw new IllegalArgumentException("trying to graft " + other.getGraph().getTable() + " at " + graftPath);
        final SQLRowValues graftPlace = this.getGraph().followPath(graftPath);
        if (graftPlace == null)
            throw new IllegalArgumentException("path doesn't exist: " + graftPath);
        assert graftPath.getLast() == graftPlace.getTable();
        if (other.getGraph().hasForeigns())
            throw new IllegalArgumentException("shouldn't have foreign rows");

        final Path descendantPath = computePath(other.getGraph());
        final int descendantPathLength = descendantPath.length();
        if (descendantPathLength == 0)
            throw new IllegalArgumentException("empty path");
        // checked by computePath
        assert descendantPath.isSingleField();
        // we used to disallow that :
        // this is LOCAL* -> BATIMENT -> SITE and CPI -> LOCAL -> BATIMENT* is being grafted
        // but this is sometimes desirable, e.g. for each LOCAL find all of its siblings with the
        // same capacity (or any other predicate)

        // shallow copy : all values are still immutable
        final Map<Path, Map<Path, SQLRowValuesListFetcher>> outerMutable = new HashMap<Path, Map<Path, SQLRowValuesListFetcher>>(this.grafts);
        final Map<Path, SQLRowValuesListFetcher> innerMutable;
        if (!this.grafts.containsKey(graftPath)) {
            // allow getFetchers() to use a list, easing tests and avoiding using equals()
            innerMutable = new LinkedHashMap<Path, SQLRowValuesListFetcher>(4);
        } else {
            final Map<Path, SQLRowValuesListFetcher> map = this.grafts.get(graftPath);
            innerMutable = new LinkedHashMap<Path, SQLRowValuesListFetcher>(map);
            // e.g. fetching *BATIMENT* <- LOCAL and *BATIMENT* <- LOCAL <- CPI (with different
            // WHERE) and LOCAL have different fields. This isn't supported since we would have to
            // merge fields in merge() and it would be quite long
            for (Entry<Path, SQLRowValuesListFetcher> e : map.entrySet()) {
                final Path fetcherPath = e.getKey();
                final SQLRowValuesListFetcher fetcher = e.getValue();
                for (int i = 1; i <= descendantPathLength; i++) {
                    final Path subPath = descendantPath.subPath(0, i);
                    if (fetcherPath.startsWith(subPath)) {
                        if (!fetcher.getGraph().followPath(subPath).getFields().equals(other.getGraph().followPath(subPath).getFields()))
                            throw new IllegalArgumentException("The same node have different fields in different fetcher\n" + graftPath + "\n" + subPath);
                    } else {
                        break;
                    }
                }
            }
        }
        final SQLRowValuesListFetcher res = innerMutable.put(descendantPath, other);
        outerMutable.put(graftPath, Collections.unmodifiableMap(innerMutable));
        this.grafts = Collections.unmodifiableMap(outerMutable);
        return res;
    }

    public final Collection<SQLRowValuesListFetcher> ungraft() {
        return this.ungraft(Path.get(getGraph().getTable()));
    }

    public synchronized final Collection<SQLRowValuesListFetcher> ungraft(final Path graftPath) {
        checkFrozen();
        if (!this.grafts.containsKey(graftPath))
            return null;
        final Map<Path, Map<Path, SQLRowValuesListFetcher>> outerMutable = new HashMap<Path, Map<Path, SQLRowValuesListFetcher>>(this.grafts);
        final Map<Path, SQLRowValuesListFetcher> res = outerMutable.remove(graftPath);
        this.grafts = Collections.unmodifiableMap(outerMutable);
        return res == null ? null : res.values();
    }

    private synchronized final Map<Path, Map<Path, SQLRowValuesListFetcher>> getGrafts() {
        return this.grafts;
    }

    /**
     * The fetchers grafted at the passed path.
     * 
     * @param graftPath where the fetchers are grafted, e.g. MISSION, DOSSIER, SITE.
     * @return the grafts by their path to fetch, e.g. SITE, BATIMENT, LOCAL, CPI_BT.
     */
    public final Map<Path, SQLRowValuesListFetcher> getGrafts(final Path graftPath) {
        return this.getGrafts().get(graftPath);
    }

    /**
     * Get all fetchers.
     * 
     * @param includeSelf <code>true</code> to include <code>this</code> (with a <code>null</code>
     *        key).
     * @return all instances indexed by the graft path.
     */
    public final ListMapItf<Path, SQLRowValuesListFetcher> getFetchers(final boolean includeSelf) {
        final ListMap<Path, SQLRowValuesListFetcher> res = new ListMap<Path, SQLRowValuesListFetcher>();
        for (final Entry<Path, Map<Path, SQLRowValuesListFetcher>> e : this.getGrafts().entrySet()) {
            assert e.getKey() != null;
            res.putCollection(e.getKey(), e.getValue().values());
        }
        if (includeSelf)
            res.add(null, this);
        return ListMap.unmodifiableMap(res);
    }

    /**
     * Get instances which fetch the {@link Path#getLast() last table} of the passed path. E.g.
     * useful if you want to add a where to a join. This method is recursively called on
     * {@link #getGrafts(Path) grafts} thus the returned paths may be fetched by grafts.
     * 
     * @param fetchedPath a path starting by this table.
     * @return all instances indexed by the graft path, i.e. <code>fetchedPath</code> is between
     *         with it and (it+fetchers.{@link #getReferentPath()}).
     */
    public final ListMap<Path, SQLRowValuesListFetcher> getFetchers(final Path fetchedPath) {
        final ListMap<Path, SQLRowValuesListFetcher> res = new ListMap<Path, SQLRowValuesListFetcher>();
        if (this.getGraph().followPath(fetchedPath) != null)
            res.add(Path.get(getGraph().getTable()), this);
        // search grafts
        for (final Entry<Path, Map<Path, SQLRowValuesListFetcher>> e : this.getGrafts().entrySet()) {
            final Path graftPlace = e.getKey();
            if (fetchedPath.startsWith(graftPlace) && fetchedPath.length() > graftPlace.length()) {
                final Path rest = fetchedPath.subPath(graftPlace.length());
                // we want requests that use the last step of fetchedPath
                assert rest.length() > 0;
                for (final Entry<Path, SQLRowValuesListFetcher> e2 : e.getValue().entrySet()) {
                    final Path refPath = e2.getKey();
                    final SQLRowValuesListFetcher graft = e2.getValue();
                    if (refPath.startsWith(rest)) {
                        res.add(graftPlace, graft);
                    } else if (rest.startsWith(refPath)) {
                        // otherwise rest == refPath and the above if would have been executed
                        assert rest.length() > refPath.length();
                        for (final Entry<Path, List<SQLRowValuesListFetcher>> e3 : graft.getFetchers(rest).entrySet()) {
                            res.addAll(graftPlace.append(e3.getKey()), e3.getValue());
                        }
                    }
                }
            }
        }
        return res;
    }

    private final void addFields(final SQLSelect sel, final SQLRowValues vals, final String alias) {
        // put key first
        final SQLField key = vals.getTable().getKey();
        sel.addSelect(new AliasedField(key, alias));
        for (final String fieldName : vals.getFields()) {
            if (!fieldName.equals(key.getName()))
                sel.addSelect(new AliasedField(vals.getTable().getField(fieldName), alias));
        }
    }

    public final SQLSelect getReq() {
        return this.getReq(null, null);
    }

    static private final SQLSelect checkTr(final List<String> origSelect, final SQLSelect tr) {
        if (!origSelect.equals(tr.getSelect()))
            throw new IllegalArgumentException("Select clause cannot be modified");
        return tr;
    }

    public synchronized final SQLSelect getReq(final Where w, final ITransformer<SQLSelect, SQLSelect> selTransf) {
        checkTable(w);
        final boolean isNopTransf = selTransf == null || selTransf == Transformer.<SQLSelect> nopTransformer();
        if (this.isFrozen()) {
            if (w == null && isNopTransf) {
                return this.frozen;
            } else {
                final SQLSelect copy = new SQLSelect(this.frozen);
                final SQLSelect res = isNopTransf ? copy : checkTr(copy.getSelect(), selTransf.transformChecked(copy));
                return res.andWhere(w);
            }
        }

        final SQLTable t = this.getGraph().getTable();
        final SQLSelect sel = new SQLSelect();

        if (this.includeForeignUndef) {
            sel.setExcludeUndefined(false);
            sel.setExcludeUndefined(true, t);
        }

        walk(null, new ITransformer<State<String>, String>() {
            @Override
            public String transformChecked(State<String> input) {
                final String alias;
                if (input.getFrom() != null) {
                    alias = getAlias(sel, input.getPath());
                    final String aliasPrev = input.getAcc();
                    final String joinType = isPathRequired(input.getPath()) ? "INNER" : "LEFT";
                    sel.addJoin(joinType, aliasPrev, input.getPath().getStep(-1), alias);
                } else {
                    alias = null;
                }
                addFields(sel, input.getCurrent(), alias);

                return alias;
            }

        });
        for (final Path p : this.getOrder())
            sel.addOrder(sel.followPath(t.getName(), p), false);
        // after getOrder() since it can specify more precise order
        if (this.areReferentsOrdered()) {
            final int descSize = this.descendantPath.length();
            for (int i = 1; i <= descSize; i++) {
                sel.addOrder(sel.followPath(t.getName(), this.descendantPath.subPath(0, i)), false);
            }
        }

        if (this.getSelID() != null)
            sel.andWhere(getIDWhere(t, this.getSelID()));
        final List<String> origSel = new ArrayList<String>(sel.getSelect());
        SQLSelect res = sel;
        for (final ITransformer<SQLSelect, SQLSelect> tr : this.getSelectTransformers()) {
            res = tr.transformChecked(res);
        }
        if (!isNopTransf)
            res = selTransf.transformChecked(res);
        return checkTr(origSel, res).andWhere(w);
    }

    static private Where getIDWhere(final SQLTable t, final Number id) {
        if (id == null)
            return null;
        return new Where(t.getKey(), "=", id);
    }

    static String getAlias(final SQLSelect sel, final Path path) {
        String res = "tAlias";
        final int stop = path.length();
        for (int i = 0; i < stop; i++) {
            res += "__" + path.getSingleField(i).getName();
        }
        // needed for backward, otherwise tableAlias__ID_BATIMENT for LOCAL
        res += "__" + path.getTable(stop).getName();
        return sel.getUniqueAlias(res);
    }

    // assure that the graph is explored the same way for the construction of the request
    // and the reading of the resultSet
    private <S> void walk(final S sel, final ITransformer<State<S>, S> transf) {
        // walk through foreign keys and never walk back (use graft())
        this.getGraph().getGraph().walk(this.getGraph(), sel, transf, RecursionType.BREADTH_FIRST, Direction.FOREIGN);
        // walk starting backwards but allowing forwards
        this.getGraph().getGraph().walk(this.getGraph(), sel, new ITransformer<State<S>, S>() {
            @Override
            public S transformChecked(State<S> input) {
                final Path p = input.getPath();
                if (p.getStep(0).isForeign())
                    throw new SQLRowValuesCluster.StopRecurseException().setCompletely(false);
                final Step lastStep = p.getStep(p.length() - 1);
                // if we go backwards it should be from the start (i.e. we can't go backwards, then
                // forwards and backwards again)
                if (!lastStep.isForeign() && p.getDirection() != Direction.REFERENT)
                    throw new SQLRowValuesCluster.StopRecurseException().setCompletely(false);
                return transf.transformChecked(input);
            }
        }, new WalkOptions(Direction.ANY).setRecursionType(RecursionType.BREADTH_FIRST).setStartIncluded(false));
    }

    // models the graph, so that we don't have to walk it for each row
    private static final class GraphNode {
        private final SQLTable t;
        private final int fieldCount;
        private final int foreignCount;
        private final int linkIndex;
        private final Step from;

        private GraphNode(final State<Integer> input) {
            super();
            this.t = input.getCurrent().getTable();
            this.fieldCount = input.getCurrent().size();
            this.foreignCount = input.getCurrent().getForeigns().size();
            this.linkIndex = input.getAcc();
            final int length = input.getPath().length();
            this.from = length == 0 ? null : input.getPath().getStep(length - 1);
        }

        public final SQLTable getTable() {
            return this.t;
        }

        public final int getFieldCount() {
            return this.fieldCount;
        }

        public final int getForeignCount() {
            return this.foreignCount;
        }

        public final int getLinkIndex() {
            return this.linkIndex;
        }

        public final String getFromName() {
            return this.from.getSingleField().getName();
        }

        public final boolean isBackwards() {
            return !this.from.isForeign();
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + this.fieldCount;
            result = prime * result + ((this.from == null) ? 0 : this.from.hashCode());
            result = prime * result + this.linkIndex;
            result = prime * result + this.t.hashCode();
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
            final GraphNode other = (GraphNode) obj;
            return this.fieldCount == other.fieldCount && this.linkIndex == other.linkIndex && this.t.equals(other.t) && CompareUtils.equals(this.from, other.from);
        }

        @Override
        public String toString() {
            final String link = this.from == null ? "" : " linked to " + getLinkIndex() + " by " + this.getFromName() + (this.isBackwards() ? " backwards" : " forewards");
            return this.getFieldCount() + " fields of " + this.getTable() + link;
        }
    }

    static private final class RSH implements ResultSetHandler {
        private final List<String> selectFields;
        private final List<GraphNode> graphNodes;
        private final boolean freezeRows;

        private RSH(List<String> selectFields, List<GraphNode> l, boolean freezeRows) {
            this.selectFields = selectFields;
            this.graphNodes = l;
            this.freezeRows = freezeRows;
        }

        @Override
        public Object handle(final ResultSet rs) throws SQLException {
            final List<GraphNode> l = this.graphNodes;
            final int graphSize = l.size();
            int nextToLink = 0;
            final List<Future<?>> futures = new ArrayList<Future<?>>();

            final List<SQLRowValues> res = new ArrayList<SQLRowValues>(64);
            final List<List<SQLRowValues>> rows = Collections.synchronizedList(new ArrayList<List<SQLRowValues>>(64));
            // for each rs row, create all SQLRowValues without linking them together
            // if we're multi-threaded, link them in another thread
            while (rs.next()) {
                int rsIndex = 1;

                // MAYBE cancel() futures
                if (Thread.currentThread().isInterrupted())
                    throw new RTInterruptedException("interrupted while fetching");
                final List<SQLRowValues> row = new ArrayList<SQLRowValues>(graphSize);
                for (int i = 0; i < graphSize; i++) {
                    final GraphNode node = l.get(i);
                    final int stop = rsIndex + node.getFieldCount();
                    final SQLRowValues creatingVals;
                    // the PK is always first and it can only be null if there was no row, i.e. all
                    // other fields will be null.
                    final Object first = rs.getObject(rsIndex);
                    if (first == null) {
                        creatingVals = null;
                        // don't bother reading all nulls
                        rsIndex = stop;
                    } else {
                        // don't pass referent count as it can be fetched by a graft, or else
                        // several rows might later be merged (e.g. *BATIMENT* <- LOCAL has only one
                        // referent but all locals of a batiment will point to the same row)
                        creatingVals = new SQLRowValues(node.getTable(), node.getFieldCount(), node.getForeignCount(), -1);
                        put(creatingVals, rsIndex, first);
                        rsIndex++;
                    }
                    if (i == 0) {
                        if (creatingVals == null)
                            throw new IllegalStateException("Null primary row");
                        res.add(creatingVals);
                    }

                    for (; rsIndex < stop; rsIndex++) {
                        try {
                            put(creatingVals, rsIndex, rs.getObject(rsIndex));
                        } catch (SQLException e) {
                            throw new IllegalStateException("unable to fill " + creatingVals, e);
                        }
                    }
                    row.add(creatingVals);
                }
                rows.add(row);
                // become multi-threaded only for large values
                final int currentCount = rows.size();
                if (currentCount % 1000 == 0) {
                    futures.add(exec.submit(new Linker(l, rows, nextToLink, currentCount, this.freezeRows)));
                    nextToLink = currentCount;
                }
            }
            final int rowSize = rows.size();
            assert nextToLink > 0 == futures.size() > 0;
            if (nextToLink > 0)
                futures.add(exec.submit(new Linker(l, rows, nextToLink, rowSize, this.freezeRows)));

            // either link all rows, or...
            if (nextToLink == 0)
                link(l, rows, 0, rowSize, this.freezeRows);
            else {
                // ...wait for every one and most importantly check for any exceptions
                try {
                    for (final Future<?> f : futures)
                        f.get();
                } catch (Exception e) {
                    throw new IllegalStateException("couldn't link", e);
                }
            }

            return res;
        }

        protected void put(final SQLRowValues creatingVals, int rsIndex, final Object obj) {
            // -1 since rs starts at 1
            // field names checked only once when nodes are created
            creatingVals.put(this.selectFields.get(rsIndex - 1), obj, false);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + this.graphNodes.hashCode();
            result = prime * result + this.selectFields.hashCode();
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
            final RSH other = (RSH) obj;
            return this.graphNodes.equals(other.graphNodes) && this.selectFields.equals(other.selectFields);
        }

    }

    /**
     * Execute the request transformed by <code>selTransf</code> and return the result as a list of
     * SQLRowValues. NOTE: this method doesn't use the cache of SQLDataSource.
     * 
     * @return a list of SQLRowValues, one item per row, each item having the same structure as the
     *         SQLRowValues passed to the constructor.
     */
    public final List<SQLRowValues> fetch() {
        return this.fetch(null);
    }

    private void checkTable(final Where w) throws IllegalArgumentException {
        if (w == null)
            return;
        final SQLTable t = this.getGraph().getTable();
        for (final FieldRef f : w.getFields()) {
            if (!f.getTableRef().equals(t))
                throw new IllegalArgumentException("Not all from the primary table " + t + " : " + w);
        }
    }

    public final SQLRowValues fetchOne(final Number id) {
        return this.fetchOne(id, null);
    }

    public final SQLRowValues fetchOne(final Number id, final Boolean unmodifiableRows) {
        if (id == null)
            throw new NullPointerException("Null ID");
        if (this.getSelID() != null)
            throw new IllegalStateException("ID already set to " + getSelID());
        final List<SQLRowValues> res = this.fetch(getIDWhere(getGraph().getTable(), id), unmodifiableRows);
        if (res.size() > 1)
            throw new IllegalStateException("More than one row for ID " + id + " : " + res);
        return CollectionUtils.getFirst(res);
    }

    /**
     * Execute the request transformed by <code>selTransf</code> and with the passed where (even if
     * {@link #isFrozen()}) and return the result as a list of SQLRowValues. NOTE: this method
     * doesn't use the cache of SQLDataSource.
     * 
     * @param w a where to {@link SQLSelect#andWhere(Where) restrict} the result, can only uses the
     *        primary table (others have unspecified aliases), can be <code>null</code>.
     * @return a list of SQLRowValues, one item per row, each item having the same structure as the
     *         SQLRowValues passed to the constructor.
     * @throws IllegalArgumentException if the passed where doesn't refer exclusively to the primary
     *         table.
     */
    public final List<SQLRowValues> fetch(final Where w) throws IllegalArgumentException {
        return this.fetch(w, null);
    }

    /**
     * Execute the request transformed by <code>selTransf</code> and with the passed where (even if
     * {@link #isFrozen()}) and return the result as a list of SQLRowValues. NOTE: this method
     * doesn't use the cache of SQLDataSource.
     * 
     * @param w a where to {@link SQLSelect#andWhere(Where) restrict} the result, can only uses the
     *        primary table (others have unspecified aliases), can be <code>null</code>.
     * @param unmodifiableRows whether to return unmodifiable rows, <code>null</code> to use
     *        {@link #areReturnedRowsUnmodifiable() the default}.
     * @return a list of SQLRowValues, one item per row, each item having the same structure as the
     *         SQLRowValues passed to the constructor.
     * @throws IllegalArgumentException if the passed where doesn't refer exclusively to the primary
     *         table.
     */
    public final List<SQLRowValues> fetch(final Where w, final Boolean unmodifiableRows) throws IllegalArgumentException {
        return this.fetch(w, null, unmodifiableRows);
    }

    public final List<SQLRowValues> fetch(final Where w, final ITransformer<SQLSelect, SQLSelect> selTransf, final Boolean unmodifiableRows) throws IllegalArgumentException {
        return this.fetch(true, w, selTransf, unmodifiableRows);
    }

    private final List<SQLRowValues> fetch(final boolean merge, final Where w, final ITransformer<SQLSelect, SQLSelect> selTransf, final Boolean unmodifiableRows) throws IllegalArgumentException {
        final SQLSelect req;
        final Map<Path, Map<Path, SQLRowValuesListFetcher>> grafts;
        final boolean freezeRows;
        // the only other internal state used is this.descendantPath which is final immutable
        synchronized (this) {
            req = this.getReq(w, selTransf);
            grafts = this.getGrafts();
            freezeRows = unmodifiableRows == null ? this.areReturnedRowsUnmodifiable() : unmodifiableRows.booleanValue();
        }
        // getName() would take 5% of ResultSetHandler.handle()
        final List<FieldRef> selectFields = req.getSelectFields();
        final int selectFieldsSize = selectFields.size();
        final List<String> selectFieldsNames = req.getSelectNames();
        final SQLTable table = getGraph().getTable();

        // create a flat list of the graph nodes, we just need the table, field count and the index
        // in this list of its linked table, eg for CPI -> LOCAL -> BATIMENT -> SITE :
        // <LOCAL,2,0>, <BATIMENT,2,0>, <SITE,5,1>, <CPI,4,0>
        final int graphSize = this.getGraph().getGraph().size();
        final List<GraphNode> l = new ArrayList<GraphNode>(graphSize);
        // check field names only once since each row has the same fields
        final AtomicInteger fieldIndex = new AtomicInteger(0);
        walk(0, new ITransformer<State<Integer>, Integer>() {
            @Override
            public Integer transformChecked(State<Integer> input) {
                final int index = l.size();
                final GraphNode node = new GraphNode(input);
                final int stop = fieldIndex.get() + node.getFieldCount();
                for (int i = fieldIndex.get(); i < stop; i++) {
                    if (i >= selectFieldsSize)
                        throw new IllegalStateException("Fields were removed from the select");
                    final FieldRef field = selectFields.get(i);
                    if (!node.getTable().equals(field.getTableRef().getTable()))
                        throw new IllegalStateException("Select field not in " + node + " : " + field);
                }
                fieldIndex.set(stop);
                l.add(node);
                // used by link index of GraphNode
                return index;
            }
        });
        // otherwise walk() would already have thrown an exception
        assert fieldIndex.get() <= selectFieldsSize;
        if (fieldIndex.get() != selectFieldsSize) {
            throw new IllegalStateException("Items have been added to the select (which is useless, since only fields specified by rows are returned and WHERE cannot access SELECT columns) : "
                    + selectFields.subList(fieldIndex.get(), selectFieldsSize));
        }
        assert l.size() == graphSize : "All nodes weren't explored once : " + l.size() + " != " + graphSize + "\n" + this.getGraph().printGraph();

        final boolean mergeReferents = merge && this.fetchReferents();
        final boolean mergeGrafts = grafts.size() > 0;
        // if it is possible let the handler do the freeze, avoid another loop and further is
        // multi-threaded
        final boolean handlerCanFreeze = !mergeReferents && !mergeGrafts;

        // if we wanted to use the cache, we'd need to copy the returned list and its items (i.e.
        // deepCopy()), since we modify them afterwards. Or perhaps include the code after this line
        // into the result set handler.
        final IResultSetHandler rsh = new IResultSetHandler(new RSH(selectFieldsNames, l, freezeRows && handlerCanFreeze), false);
        @SuppressWarnings("unchecked")
        final List<SQLRowValues> res = (List<SQLRowValues>) table.getBase().getDataSource().execute(req.asString(), rsh, false);
        // e.g. list of batiment pointing to site
        final List<SQLRowValues> merged = mergeReferents ? merge(res) : res;
        if (mergeGrafts) {
            for (final Entry<Path, Map<Path, SQLRowValuesListFetcher>> graftPlaceEntry : grafts.entrySet()) {
                // e.g. BATIMENT
                final Path graftPlace = graftPlaceEntry.getKey();
                final Path mapPath = Path.get(graftPlace.getLast());
                // list of BATIMENT to only fetch what's necessary
                final Set<Number> ids = new HashSet<Number>();
                // byRows is common to all grafts to support CPI -> LOCAL -> BATIMENT and RECEPTEUR
                // -> LOCAL -> BATIMENT (ie avoid duplicate LOCAL)
                // CollectionMap since the same row can be in multiple index of merged, e.g. when
                // fetching *BATIMENT* -> SITE each site will be repeated as many times as it has
                // children and if we want their DOSSIER they must be grafted on each line.
                final ListMap<Tuple2<Path, Number>, SQLRowValues> byRows = createCollectionMap();
                for (final SQLRowValues vals : merged) {
                    // can be empty when grafting on optional row
                    for (final SQLRowValues graftPlaceVals : vals.followPath(graftPlace, CreateMode.CREATE_NONE, false)) {
                        ids.add(graftPlaceVals.getIDNumber());
                        byRows.add(Tuple2.create(mapPath, graftPlaceVals.getIDNumber()), graftPlaceVals);
                    }
                }
                assert ids.size() == byRows.size();
                for (final Entry<Path, SQLRowValuesListFetcher> e : graftPlaceEntry.getValue().entrySet()) {
                    // e.g BATIMENT <- LOCAL <- CPI
                    final Path descendantPath = e.getKey();
                    assert descendantPath.getFirst() == graftPlace.getLast() : descendantPath + " != " + graftPlace;
                    final SQLRowValuesListFetcher graft = e.getValue();

                    // don't merge then...
                    final List<SQLRowValues> referentVals = graft.fetch(false, new Where(graft.getGraph().getTable().getKey(), ids), null, false);
                    // ...but now
                    merge(merged, referentVals, byRows, descendantPath);
                }
            }
        }
        if (freezeRows && !handlerCanFreeze) {
            for (final SQLRowValues r : merged) {
                r.getGraph().freeze();
            }
        }
        return merged;
    }

    // no need to set keep-alive too low, since on finalize() the pool shutdowns itself
    private static final ExecutorService exec = new ThreadPoolExecutor(0, 2, 10L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

    private static final class Linker implements Callable<Object> {

        private final List<GraphNode> l;
        private final List<List<SQLRowValues>> rows;
        private final int fromIndex;
        private final int toIndex;
        private final boolean freezeRows;

        public Linker(final List<GraphNode> l, final List<List<SQLRowValues>> rows, final int first, final int last, final boolean freezeRows) {
            super();
            this.l = l;
            this.rows = rows;
            this.fromIndex = first;
            this.toIndex = last;
            this.freezeRows = freezeRows;
        }

        @Override
        public Object call() throws Exception {
            link(this.l, this.rows, this.fromIndex, this.toIndex, this.freezeRows);
            return null;
        }

    }

    private static void link(final List<GraphNode> l, final List<List<SQLRowValues>> rows, final int start, final int stop, final boolean freezeRows) {
        final int graphSize = l.size();
        for (int nodeIndex = 1; nodeIndex < graphSize; nodeIndex++) {
            final GraphNode node = l.get(nodeIndex);

            final String fromName = node.getFromName();
            final int linkIndex = node.getLinkIndex();
            final boolean backwards = node.isBackwards();

            // freeze after the last put()
            final boolean freeze = freezeRows && nodeIndex == graphSize - 1;

            for (int i = start; i < stop; i++) {
                final List<SQLRowValues> row = rows.get(i);
                final SQLRowValues creatingVals = row.get(nodeIndex);
                // don't link empty values (LEFT JOIN produces rowValues filled with
                // nulls) to the graph
                if (creatingVals != null) {
                    final SQLRowValues valsToFill;
                    final SQLRowValues valsToPut;
                    if (backwards) {
                        valsToFill = creatingVals;
                        valsToPut = row.get(linkIndex);
                    } else {
                        valsToFill = row.get(linkIndex);
                        valsToPut = creatingVals;
                    }

                    // check is done by updateLinks()
                    valsToFill.put(fromName, valsToPut, false);
                }
                // can't use creatingVals, use primary row which is never null
                if (freeze)
                    row.get(0).getGraph().freeze();
            }
        }
        if (freezeRows && graphSize == 1) {
            for (int i = start; i < stop; i++) {
                final List<SQLRowValues> row = rows.get(i);
                final boolean justFrozen = row.get(0).getGraph().freeze();
                assert justFrozen : "Already frozen";
            }
        }
    }

    /**
     * Merge a list of fetched rowValues so that remove any duplicated rowValues. Eg, transforms
     * this :
     * 
     * <pre>
     * BATIMENT[2]     LOCAL[2]        CPI_BT[3]       
     * BATIMENT[2]     LOCAL[2]        CPI_BT[2]       
     * BATIMENT[2]     LOCAL[3]        
     * BATIMENT[2]     LOCAL[5]        CPI_BT[5]
     * BATIMENT[3]     LOCAL[4]        CPI_BT[4]       
     * BATIMENT[4]
     * </pre>
     * 
     * into this :
     * 
     * <pre>
     * BATIMENT[2]     LOCAL[2]        CPI_BT[3]       
     *                                 CPI_BT[2]       
     *                 LOCAL[3]        
     *                 LOCAL[5]        CPI_BT[5]
     * BATIMENT[3]     LOCAL[4]        CPI_BT[4]       
     * BATIMENT[4]
     * </pre>
     * 
     * @param l a list of fetched rowValues.
     * @return a smaller list in which all rowValues are unique.
     */
    private final List<SQLRowValues> merge(final List<SQLRowValues> l) {
        return merge(l, l, null, this.descendantPath);
    }

    /**
     * Merge a list of rowValues and optionally graft it onto another one.
     * 
     * @param tree the list receiving the graft.
     * @param graft the list being merged and optionally grafted on <code>tree</code>, can be the
     *        same as <code>tree</code>.
     * @param graftPlaceRows if this is a graft the destination rowValues, otherwise
     *        <code>null</code>, this instance will be modified.
     * @param descendantPath the path to merge.
     * @return the merged and grafted values.
     */
    static private final List<SQLRowValues> merge(final List<SQLRowValues> tree, final List<SQLRowValues> graft, final ListMap<Tuple2<Path, Number>, SQLRowValues> graftPlaceRows,
            Path descendantPath) {
        final boolean isGraft = graftPlaceRows != null;
        assert (tree != graft) == isGraft : "Trying to graft onto itself";
        final List<SQLRowValues> res = isGraft ? tree : new ArrayList<SQLRowValues>();
        // so that every graft is actually grafted onto the tree
        final ListMap<Tuple2<Path, Number>, SQLRowValues> map = isGraft ? graftPlaceRows : createCollectionMap();

        final int stop = descendantPath.length();
        for (final SQLRowValues v : graft) {
            boolean doAdd = true;
            SQLRowValues previous = null;
            for (int i = stop; i >= 0 && doAdd; i--) {
                final Path subPath = descendantPath.subPath(0, i);
                final SQLRowValues desc = v.followPath(subPath);
                if (desc != null) {
                    final Tuple2<Path, Number> row = Tuple2.create(subPath, desc.getIDNumber());
                    if (map.containsKey(row)) {
                        doAdd = false;
                        assert map.get(row).get(0).getFields().containsAll(desc.getFields()) : "Discarding an SQLRowValues with more fields : " + desc;
                        // previous being null can happen when 2 grafted paths share some steps at
                        // the start, e.g. SOURCE -> LOCAL and CPI -> LOCAL with a LOCAL having a
                        // SOURCE but no CPI
                        if (previous != null) {
                            final List<SQLRowValues> destinationRows = map.get(row);
                            final int destinationSize = destinationRows.size();
                            assert destinationSize > 0 : "Map contains row but have no corresponding value: " + row;
                            final String ffName = descendantPath.getSingleField(i).getName();
                            // avoid the first deepCopy() (needed since rows of 'previous' have
                            // already been added to 'map') and copy before merging
                            for (int j = 1; j < destinationSize; j++) {
                                final SQLRowValues previousCopy = previous.deepCopy().put(ffName, destinationRows.get(j));
                                // put the copied rowValues into 'map' otherwise they'd be
                                // unreachable and thus couldn't have referents. Tested by
                                // SQLRowValuesListFetcherTest.testSameReferentMergedMultipleTimes()
                                // i+1 since we start from 'previous' not 'desc'
                                for (int k = stop; k >= i + 1; k--) {
                                    final SQLRowValues descCopy = previousCopy.followPath(descendantPath.subPath(i + 1, k));
                                    if (descCopy != null) {
                                        final Tuple2<Path, Number> rowCopy = Tuple2.create(descendantPath.subPath(0, k), descCopy.getIDNumber());
                                        assert map.containsKey(rowCopy) : "Since we already iterated with i";
                                        map.add(rowCopy, descCopy);
                                    }
                                }
                            }
                            // don't call map.put() it has already been handled below
                            previous.put(ffName, destinationRows.get(0));
                        }
                    } else {
                        map.add(row, desc);
                    }
                    previous = desc;
                }
            }
            if (doAdd) {
                assert !isGraft : "Adding graft values as tree values";
                res.add(v);
            }
        }
        return res;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " for " + this.getGraph() + " with " + this.getSelID() + " and " + this.getSelectTransformers();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SQLRowValuesListFetcher) {
            final SQLRowValuesListFetcher o = (SQLRowValuesListFetcher) obj;
            final SQLSelect thisReq, oReq;
            final Map<Path, Map<Path, SQLRowValuesListFetcher>> thisGrafts, oGrafts;
            synchronized (this) {
                thisReq = this.getReq();
                thisGrafts = this.getGrafts();
            }
            synchronized (o) {
                oReq = o.getReq();
                oGrafts = o.getGrafts();
            }
            // use getReq() to avoid selTransf equality pb (ie we generally use anonymous classes
            // which thus lack equals())
            return thisReq.equals(oReq) && CompareUtils.equals(this.descendantPath, o.descendantPath) && thisGrafts.equals(oGrafts);
        } else
            return false;
    }

    @Override
    public int hashCode() {
        return this.getReq().hashCode();
    }
}
