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
 
 package org.openconcerto.sql.request;

import org.openconcerto.sql.FieldExpander;
import org.openconcerto.sql.TM;
import org.openconcerto.sql.model.FieldRef;
import org.openconcerto.sql.model.IFieldPath;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLRowValues.CreateMode;
import org.openconcerto.sql.model.SQLRowValuesListFetcher;
import org.openconcerto.sql.model.SQLSearchMode;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLSyntax;
import org.openconcerto.sql.model.SQLSyntax.CaseBuilder;
import org.openconcerto.sql.model.SQLSyntax.DateProp;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.SQLTable.VirtualFields;
import org.openconcerto.sql.model.SQLTableModifiedListener;
import org.openconcerto.sql.model.SQLType;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.model.graph.Path;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.Tuple2;
import org.openconcerto.utils.cc.IClosure;
import org.openconcerto.utils.cc.ITransformer;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;

@ThreadSafe
public abstract class BaseFillSQLRequest extends BaseSQLRequest {

    private static boolean DEFAULT_SELECT_LOCK = true;

    /**
     * Whether to use "FOR SHARE" in list requests (preventing roles with just SELECT right from
     * seeing the list).
     * 
     * @return <code>true</code> if select should obtain a lock.
     * @see SQLSelect#setWaitPreviousWriteTX(boolean)
     */
    public static final boolean getDefaultLockSelect() {
        return DEFAULT_SELECT_LOCK;
    }

    public static final void setDefaultLockSelect(final boolean b) {
        DEFAULT_SELECT_LOCK = b;
    }

    static public void setupForeign(final SQLRowValuesListFetcher fetcher) {
        // include rows having NULL (not undefined ID) foreign keys
        fetcher.setFullOnly(false);
        // treat the same way tables with or without undefined ID
        fetcher.setIncludeForeignUndef(false);
        // be predictable
        fetcher.setReferentsOrdered(true, true);
    }

    static public final boolean addToFetch(final SQLRowValues input, final Path p, final Collection<String> fields) {
        assert p == null || p.isSingleLink() : "Graph size not sufficient to know if graph was modified";
        final int graphSize = input.getGraphSize();
        // don't back track : e.g. if path is SITE -> CLIENT <- SITE we want the siblings of SITE,
        // if we want fields of the primary SITE we pass the path SITE
        final SQLRowValues r = p == null ? input : input.followPathToOne(p, CreateMode.CREATE_ONE, false);
        boolean modified = input.getGraphSize() > graphSize;
        for (final String f : fields) {
            // don't overwrite foreign rows and update modified
            if (!r.getFields().contains(f)) {
                r.put(f, null);
                modified = true;
            }
        }
        return modified;
    }

    private final SQLTable primaryTable;
    @GuardedBy("this")
    private List<Path> order;
    @GuardedBy("this")
    private Where where;
    @GuardedBy("this")
    private Map<IFieldPath, SearchField> searchFields;
    @GuardedBy("this")
    private int searchLimit;
    @GuardedBy("this")
    private ITransformer<SQLSelect, SQLSelect> selTransf;
    @GuardedBy("this")
    private boolean lockSelect;

    private final SQLRowValues graph;
    @GuardedBy("this")
    private SQLRowValues graphToFetch;

    @GuardedBy("this")
    private SQLRowValuesListFetcher frozen;

    {
        // a new instance is never frozen
        this.frozen = null;
    }

    private final PropertyChangeSupport supp = new PropertyChangeSupport(this);

    public BaseFillSQLRequest(final SQLRowValues graph, final Where w) {
        super();
        if (graph == null)
            throw new NullPointerException();
        this.primaryTable = graph.getTable();
        this.setOrder(null);
        this.where = w;
        this.searchFields = Collections.emptyMap();
        this.searchLimit = 35;
        this.selTransf = null;
        this.lockSelect = getDefaultLockSelect();
        this.graph = graph.toImmutable();
        this.graphToFetch = null;
    }

    public BaseFillSQLRequest(final BaseFillSQLRequest req) {
        super();
        this.primaryTable = req.getPrimaryTable();
        synchronized (req) {
            this.order = req.order;
            this.where = req.where;
            this.searchFields = req.searchFields;
            this.searchLimit = req.searchLimit;
            this.selTransf = req.selTransf;
            this.lockSelect = req.lockSelect;
            // use methods since they're both lazy
            this.graph = req.getGraph();
            this.graphToFetch = req.getGraphToFetch();
        }
    }

    public synchronized final boolean isFrozen() {
        return this.frozen != null;
    }

    public final void freeze() {
        this.freeze(this);
    }

    private final synchronized void freeze(final BaseFillSQLRequest from) {
        if (!this.isFrozen()) {
            // compute the fetcher once and for all
            this.frozen = from.getFetcher();
            assert this.frozen.isFrozen();
            this.wasFrozen();
        }
    }

    protected void wasFrozen() {
    }

    protected final void checkFrozen() {
        if (this.isFrozen())
            throw new IllegalStateException("this has been frozen: " + this);
    }

    // not final so we can narrow down the return type
    public BaseFillSQLRequest toUnmodifiable() {
        return this.toUnmodifiableP(this.getClass());
    }

    // should be passed the class created by cloneForFreeze(), i.e. not this.getClass() or this
    // won't support anonymous classes
    protected final <T extends BaseFillSQLRequest> T toUnmodifiableP(final Class<T> clazz) {
        final Class<? extends BaseFillSQLRequest> thisClass = this.getClass();
        if (clazz != thisClass && !(thisClass.isAnonymousClass() && clazz == thisClass.getSuperclass()))
            throw new IllegalArgumentException("Passed class isn't our class : " + clazz + " != " + thisClass);
        final BaseFillSQLRequest res;
        synchronized (this) {
            if (this.isFrozen()) {
                res = this;
            } else {
                res = this.clone(true);
                if (res.getClass() != clazz)
                    throw new IllegalStateException("Clone class mismatch : " + res.getClass() + " != " + clazz);
                // freeze before releasing lock (even if not recommended, allow to modify the state
                // of getSelectTransf() while holding our lock)
                // pass ourselves so that if we are an anonymous class the fetcher created with our
                // overloaded methods is used
                res.freeze(this);
            }
        }
        assert res.getClass() == clazz || res.getClass().getSuperclass() == clazz;
        @SuppressWarnings("unchecked")
        final T casted = (T) res;
        return casted;
    }

    // must be called with our lock
    protected abstract BaseFillSQLRequest clone(boolean forFreeze);

    static protected final SQLRowValues computeGraph(final SQLTable t, final Collection<String> fields, final FieldExpander exp) {
        final SQLRowValues vals = new SQLRowValues(t).putNulls(fields);
        exp.expand(vals);
        return vals.toImmutable();
    }

    /**
     * The graph with fields to be automatically added to the UI.
     * 
     * @return the expanded frozen graph.
     */
    public final SQLRowValues getGraph() {
        return this.graph;
    }

    /**
     * The graph to fetch, should be a superset of {@link #getGraph()}. To modify it, see
     * {@link #addToGraphToFetch(Path, Set)} and {@link #changeGraphToFetch(IClosure)}.
     * 
     * @return the graph to fetch, frozen.
     */
    public final SQLRowValues getGraphToFetch() {
        synchronized (this) {
            if (this.graphToFetch == null && this.getGraph() != null) {
                assert !this.isFrozen() : "no computation should take place after frozen()";
                final SQLRowValues tmp = this.getGraph().deepCopy();
                this.customizeToFetch(tmp);
                this.setGraphToFetch(tmp, true);
            }
            return this.graphToFetch;
        }
    }

    public final void addToGraphToFetch(final String... fields) {
        this.addToGraphToFetch(Arrays.asList(fields));
    }

    public final void addToGraphToFetch(final Collection<String> fields) {
        this.addToGraphToFetch(null, fields);
    }

    public final void addForeignToGraphToFetch(final String foreignField, final Collection<String> fields) {
        this.addToGraphToFetch(new Path(getPrimaryTable()).addForeignField(foreignField), fields);
    }

    /**
     * Make sure that the fields at the end of the path are fetched.
     * 
     * @param p a path.
     * @param fields fields to fetch.
     */
    public final void addToGraphToFetch(final Path p, final Collection<String> fields) {
        this.changeGraphToFetch(new IClosure<SQLRowValues>() {
            @Override
            public void executeChecked(SQLRowValues input) {
                addToFetch(input, p, fields);
            }
        }, false);
    }

    public final void changeGraphToFetch(IClosure<SQLRowValues> cl) {
        this.changeGraphToFetch(cl, true);
    }

    private final void changeGraphToFetch(IClosure<SQLRowValues> cl, final boolean checkNeeded) {
        synchronized (this) {
            checkFrozen();
            final SQLRowValues tmp = this.getGraphToFetch().deepCopy();
            cl.executeChecked(tmp);
            this.setGraphToFetch(tmp, checkNeeded);
        }
        fireWhereChange();
    }

    private final void setGraphToFetch(final SQLRowValues tmp, final boolean checkNeeded) {
        assert Thread.holdsLock(this) && !this.isFrozen();
        if (checkNeeded && !tmp.graphContains(this.getGraph()))
            throw new IllegalArgumentException("New graph too small");
        this.graphToFetch = tmp.toImmutable();
    }

    protected void customizeToFetch(final SQLRowValues graphToFetch) {
    }

    protected synchronized final SQLRowValuesListFetcher getFetcher() {
        if (this.isFrozen())
            return this.frozen;
        // fetch order fields, so that consumers can order an updated row in an existing list
        final SQLRowValues tmp = getGraphToFetch().deepCopy();
        for (final Path orderP : this.getOrder()) {
            final SQLRowValues orderVals = tmp.followPath(orderP);
            if (orderVals != null && orderVals.getTable().isOrdered()) {
                orderVals.put(orderVals.getTable().getOrderField().getName(), null);
            }
        }
        // graphToFetch can be modified freely so don't the use the simple constructor
        // order to have predictable result (this will both order the referent rows and main rows.
        // The latter will be overwritten by our own getOrder())
        return setupFetcher(SQLRowValuesListFetcher.create(tmp, true));
    }

    // allow to pass fetcher since they are mostly immutable (and for huge graphs they are slow to
    // create)
    protected final SQLRowValuesListFetcher setupFetcher(final SQLRowValuesListFetcher fetcher) {
        final String tableName = getPrimaryTable().getName();
        setupForeign(fetcher);
        synchronized (this) {
            fetcher.setOrder(getOrder());
            fetcher.setReturnedRowsUnmodifiable(true);
            fetcher.appendSelTransf(new ITransformer<SQLSelect, SQLSelect>() {
                @Override
                public SQLSelect transformChecked(SQLSelect sel) {
                    sel = transformSelect(sel);
                    if (isLockSelect())
                        sel.addLockedTable(tableName);
                    return sel.andWhere(getWhere());
                }
            });
            // freeze to execute setSelTransf() before leaving the synchronized block
            fetcher.freeze();
        }
        return fetcher;
    }

    protected synchronized final List<Path> getOrder() {
        if (this.order != null)
            return this.order;
        return this.getDefaultOrder();
    }

    protected List<Path> getDefaultOrder() {
        return Collections.singletonList(Path.get(getPrimaryTable()));
    }

    /**
     * Change the ordering of this request.
     * 
     * @param l the list of tables, <code>null</code> to restore the {@link #getDefaultOrder()
     *        default} .
     */
    public synchronized final void setOrder(List<Path> l) {
        checkFrozen();
        this.order = l == null ? null : Collections.unmodifiableList(new ArrayList<Path>(l));
    }

    public final void setWhere(final Where w) {
        synchronized (this) {
            checkFrozen();
            this.where = w;
        }
        fireWhereChange();
    }

    public synchronized final Where getWhere() {
        return this.where;
    }

    /**
     * Whether this request is searchable.
     * 
     * @param b <code>true</code> if the {@link #getFields() local fields} should be used,
     *        <code>false</code> to not be searchable.
     */
    public final void setSearchable(final boolean b) {
        this.setSearchFields(b ? getDefaultSearchFields() : Collections.<SearchField> emptyList());
    }

    protected Collection<SearchField> getDefaultSearchFields() {
        final Set<String> names = CollectionUtils.inter(this.getGraph().getFields(), this.getPrimaryTable().getFieldsNames(VirtualFields.LOCAL_CONTENT));
        return mapOfModesToSearchFields(CollectionUtils.<String, SQLSearchMode> createMap(names));
    }

    /**
     * Set the fields used to search.
     * 
     * @param searchFields only rows with these fields containing the terms will match.
     * @see #setSearch(String)
     */
    public final void setSearchFieldsNames(final Collection<String> searchFields) {
        this.setSearchFieldsNames(CollectionUtils.<String, SQLSearchMode> createMap(searchFields));
    }

    protected final Collection<SearchField> mapOfModesToSearchFields(Map<String, SQLSearchMode> searchFields) {
        final List<SearchField> list = new ArrayList<SearchField>();
        for (final Entry<String, SQLSearchMode> e : searchFields.entrySet()) {
            list.add(new SearchField(getPrimaryTable().getField(e.getKey()), e.getValue() == null ? SQLSearchMode.CONTAINS : e.getValue()));
        }
        return list;
    }

    /**
     * Set the fields used to search.
     * 
     * @param searchFields for each field to search, how to match.
     * @see #setSearch(String)
     */
    public final void setSearchFieldsNames(Map<String, SQLSearchMode> searchFields) {
        this.setSearchFields(mapOfModesToSearchFields(searchFields));
    }

    public final void setSearchFields(final Collection<SearchField> searchFields) {
        // can be outside the synchronized block, since it can't be reverted
        checkFrozen();
        final Map<IFieldPath, SearchField> copy = new HashMap<IFieldPath, SearchField>();
        for (final SearchField f : searchFields) {
            final SearchField prev = copy.put(f.getField(), f);
            if (prev != null)
                throw new IllegalArgumentException("Duplicate : " + f.getField());
        }
        synchronized (this) {
            this.searchFields = Collections.unmodifiableMap(copy);
        }
        fireWhereChange();
    }

    public Map<IFieldPath, SearchField> getSearchFields() {
        synchronized (this) {
            return this.searchFields;
        }
    }

    public synchronized final boolean isSearchable() {
        return !this.getSearchFields().isEmpty();
    }

    public synchronized final void setSearchLimit(final int limit) {
        this.searchLimit = limit;
    }

    public synchronized final int getSearchLimit() {
        return this.searchLimit;
    }

    public final synchronized void setLockSelect(boolean lockSelect) {
        checkFrozen();
        this.lockSelect = lockSelect;
    }

    public final synchronized boolean isLockSelect() {
        return this.lockSelect;
    }

    public Set<SQLTable> getTables() {
        final Set<SQLTable> res = new HashSet<SQLTable>();
        for (final SQLRowValues v : this.getGraphToFetch().getGraph().getItems())
            res.add(v.getTable());
        return res;
    }

    public final void addTableListener(SQLTableModifiedListener l) {
        for (final SQLTable t : this.getTables()) {
            t.addTableModifiedListener(l);
        }
    }

    public final void removeTableListener(SQLTableModifiedListener l) {
        for (final SQLTable t : this.getTables()) {
            t.removeTableModifiedListener(l);
        }
    }

    protected final List<SQLField> getFields() {
        return this.getPrimaryTable().getFields(this.getGraph().getFields());
    }

    protected SQLSelect transformSelect(final SQLSelect sel) {
        final ITransformer<SQLSelect, SQLSelect> transf = this.getSelectTransf();
        return transf == null ? sel : transf.transformChecked(sel);
    }

    // @param searchQuery null means don't want to search in SQL (i.e. no WHERE, no LIMIT), empty
    // means nothing to search (i.e. no WHERE but LIMIT).
    protected final ITransformer<SQLSelect, SQLSelect> createSearchTransformer(final List<String> searchQuery, final Locale l, final Where forceInclude) {
        if (searchQuery == null)
            return null;
        final Map<IFieldPath, SearchField> searchFields;
        final int searchLimit;
        final boolean searchable;
        synchronized (this) {
            searchFields = this.getSearchFields();
            searchLimit = this.getSearchLimit();
            searchable = this.isSearchable();
        }
        if (!searchable) {
            throw new IllegalArgumentException("Cannot search " + searchQuery);
        }
        // continue even if searchQuery is empty to apply the LIMIT
        final List<String> immutableQuery = Collections.unmodifiableList(new ArrayList<String>(searchQuery));
        return new ITransformer<SQLSelect, SQLSelect>() {
            @Override
            public SQLSelect transformChecked(SQLSelect sel) {
                return transformSelectSearch(sel, searchFields, searchLimit, immutableQuery, l, forceInclude);
            }
        };
    }

    static protected final SQLSelect transformSelectSearch(final SQLSelect sel, final Map<IFieldPath, SearchField> searchFields, final int searchLimit, final List<String> searchQuery, final Locale l,
            final Where forceInclude) {
        final Where w;
        final Set<String> matchScore = new HashSet<String>();
        if (!searchQuery.isEmpty()) {
            final SQLSyntax syntax = sel.getSyntax();
            Where where = null;
            for (final String searchTerm : searchQuery) {
                Where termWhere = null;
                for (final SearchField searchField : searchFields.values()) {
                    final FieldRef selF = sel.followFieldPath(searchField.getField());
                    final SQLSearchMode mode = searchField.getMode();
                    final List<String> formatted = searchField.format(selF, l);
                    final String fieldWhere = createWhere(syntax, formatted, mode, searchTerm);
                    termWhere = Where.createRaw(fieldWhere).or(termWhere);
                    if (searchField.getScore() > 0 || !searchField.getHigherModes().isEmpty()) {
                        final CaseBuilder caseBuilder = syntax.createCaseWhenBuilder().setElse("0");
                        for (final Tuple2<SQLSearchMode, Integer> hm : searchField.getHigherModes()) {
                            caseBuilder.addWhen(createWhere(syntax, formatted, hm.get0(), searchTerm), String.valueOf(hm.get1()));
                        }
                        if (searchField.getScore() > 0) {
                            caseBuilder.addWhen(fieldWhere, String.valueOf(searchField.getScore()));
                        }
                        matchScore.add(caseBuilder.build());
                    }
                }
                where = Where.and(termWhere, where);
            }
            // only use forceInclude when there's a restriction otherwise the include transforms
            // itself into a restrict
            if (where != null)
                where = where.or(forceInclude);
            w = where;
        } else {
            w = null;
        }
        sel.andWhere(w);
        if (forceInclude != null)
            matchScore.add("case when " + forceInclude + " then 10000 else 0 end");
        if (!matchScore.isEmpty())
            sel.getOrder().add(0, CollectionUtils.join(matchScore, " + ") + " DESC");
        if (searchLimit >= 0)
            sel.setLimit(searchLimit);

        return sel;
    }

    static protected final String createWhere(final SQLSyntax syntax, final List<String> formatted, final SQLSearchMode mode, final String searchQuery) {
        return CollectionUtils.join(formatted, " OR ", new ITransformer<String, String>() {
            @Override
            public String transformChecked(String sqlExpr) {
                return createWhere(sqlExpr, mode, syntax, searchQuery);
            }
        });
    }

    static public final List<String> defaultFormat(final FieldRef selF, final Locale l) {
        final SQLType type = selF.getField().getType();
        final SQLSyntax syntax = SQLSyntax.get(selF.getField());
        if (type.getJavaType() == String.class) {
            return Collections.singletonList(selF.getFieldRef());
        } else if (type.getJavaType() == Boolean.class) {
            return Collections.singletonList("case when " + selF.getFieldRef() + " then " + syntax.quoteString(TM.tr("true_key")) + " else " + syntax.quoteString(TM.tr("false_key")) + " end");
        } else if (Timestamp.class.isAssignableFrom(type.getJavaType())) {
            final String shortFmt = formatTime(selF, DateProp.SHORT_DATETIME_SKELETON, l, syntax);
            final String longFmt = formatTime(selF, DateProp.LONG_DATETIME_SKELETON, l, syntax);
            return Arrays.asList(shortFmt, longFmt);
        } else if (Time.class.isAssignableFrom(type.getJavaType())) {
            return Collections.singletonList(formatTime(selF, DateProp.TIME_SKELETON, l, syntax));
        } else if (Date.class.isAssignableFrom(type.getJavaType())) {
            final String shortFmt = formatTime(selF, DateProp.SHORT_DATE_SKELETON, l, syntax);
            final String longFmt = formatTime(selF, DateProp.LONG_DATE_SKELETON, l, syntax);
            return Arrays.asList(shortFmt, longFmt);
        } else {
            return Collections.singletonList(syntax.cast(selF.getFieldRef(), String.class));
        }
    }

    static public final String formatTime(final FieldRef selF, final List<String> simpleFormat, final Locale l, final SQLSyntax syntax) {
        return syntax.getFormatTimestampSimple(selF.getFieldRef(), DateProp.getBestPattern(simpleFormat, l), l);
    }

    static protected final String createWhere(final String sqlExpr, final SQLSearchMode mode, final SQLSyntax syntax, final String searchQuery) {
        return "lower(" + sqlExpr + ") " + mode.generateSQL(syntax, searchQuery.toLowerCase());
    }

    static public class SearchField {
        private final IFieldPath field;
        private final SQLSearchMode mode;
        private final int score;
        private final List<Tuple2<SQLSearchMode, Integer>> higherModes;

        public SearchField(IFieldPath field, SQLSearchMode mode) {
            this(field, mode, 1);
        }

        /**
         * Create a new search field.
         * 
         * @param field which field to search.
         * @param mode how to search.
         * @param score the score (>0) to attribute if the field matches. Allow to rank fields
         *        between themselves.
         */
        public SearchField(IFieldPath field, SQLSearchMode mode, int score) {
            this(field, mode, score, -1, -1);
        }

        public SearchField(final IFieldPath field, final SQLSearchMode mode, final int score, final int score2, final int score3) {
            super();
            if (field.getField().getFieldGroup().getKeyType() != null)
                throw new IllegalArgumentException("Field is a key : " + field);
            this.field = field;
            this.mode = mode;
            /*
             * for now we could pass <code>1</code> so that a row with more matches is higher ranked
             * (e.g. if searching "a" ["ant", "cat"] is better than ["ant", "horse"]), or
             * <code>0</code> to ignore the match count. But this only works because we have
             * separate WHERE and ORDER BY ; if we had a computed column with "WHERE score > 0 ORDER
             * BY score" this would be complicated.
             */
            if (score < 1)
                throw new IllegalArgumentException("Invalid score : " + score);
            this.score = score;
            final List<SQLSearchMode> higherModes = field.getField().getType().getJavaType() == String.class ? this.mode.getHigherModes() : Collections.<SQLSearchMode> emptyList();
            if (higherModes.isEmpty()) {
                this.higherModes = Collections.emptyList();
            } else {
                if (higherModes.size() > 2)
                    throw new IllegalStateException("Too many higher modes " + higherModes);
                final List<Tuple2<SQLSearchMode, Integer>> tmp = new ArrayList<Tuple2<SQLSearchMode, Integer>>(2);
                tmp.add(Tuple2.create(higherModes.get(0), score3 < 1 ? Math.max((int) (this.score * 1.5), this.score + 2) : score3));
                if (higherModes.size() > 1)
                    tmp.add(Tuple2.create(higherModes.get(1), score2 < 1 ? Math.max((int) (this.score * 1.2), this.score + 1) : score2));
                this.higherModes = Collections.unmodifiableList(tmp);
            }
        }

        public final IFieldPath getField() {
            return this.field;
        }

        public final SQLSearchMode getMode() {
            return this.mode;
        }

        public final int getScore() {
            return this.score;
        }

        public List<Tuple2<SQLSearchMode, Integer>> getHigherModes() {
            return this.higherModes;
        }

        protected List<String> format(final FieldRef selF, final Locale l) {
            if (getField().getField() != selF.getField())
                throw new IllegalArgumentException("Wrong field");
            return defaultFormat(selF, l);
        }
    }

    public final synchronized ITransformer<SQLSelect, SQLSelect> getSelectTransf() {
        return this.selTransf;
    }

    /**
     * Allows to transform the SQLSelect returned by getFillRequest().
     * 
     * @param transf the transformer to apply, needs to be thread-safe.
     */
    public final void setSelectTransf(final ITransformer<SQLSelect, SQLSelect> transf) {
        synchronized (this) {
            checkFrozen();
            this.selTransf = transf;
        }
        this.fireWhereChange();
    }

    public final SQLTable getPrimaryTable() {
        return this.primaryTable;
    }

    protected final void fireWhereChange() {
        // don't call unknown code with our lock
        assert !Thread.holdsLock(this);
        this.supp.firePropertyChange("where", null, null);
    }

    public final void addWhereListener(final PropertyChangeListener l) {
        this.supp.addPropertyChangeListener("where", l);
    }

    public final void rmWhereListener(final PropertyChangeListener l) {
        this.supp.removePropertyChangeListener("where", l);
    }

    @Override
    public String toString() {
        return this.getClass().getName() + " on " + this.getPrimaryTable();
    }
}
