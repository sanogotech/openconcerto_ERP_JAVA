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

/**
 * @author ILM Informatique
 */
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.FieldExpander;
import org.openconcerto.sql.element.SQLElementDirectory;
import org.openconcerto.sql.model.FieldPath;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLRowValuesListFetcher;
import org.openconcerto.sql.model.SQLSearchMode;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.model.graph.Path;
import org.openconcerto.sql.sqlobject.IComboSelectionItem;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.CompareUtils;
import org.openconcerto.utils.RTInterruptedException;
import org.openconcerto.utils.Tuple2;
import org.openconcerto.utils.cache.CacheResult;
import org.openconcerto.utils.cc.IClosure;
import org.openconcerto.utils.cc.IPredicate;
import org.openconcerto.utils.cc.ITransformer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;

// final: use setSelectTransf()
@ThreadSafe
public final class ComboSQLRequest extends FilteredFillSQLRequest implements Cloneable {

    static private final SQLCache<CacheKey, List<IComboSelectionItem>> cache = new SQLCache<CacheKey, List<IComboSelectionItem>>(60, -1, "items of " + ComboSQLRequest.class);

    // encapsulate all values that can change the result
    @Immutable
    protected static final class CacheKey {

        private final SQLRowValues graph;
        private final SQLRowValuesListFetcher fetcher;
        private final Where where;
        private final ITransformer<SQLSelect, SQLSelect> selTransformer;
        // compute request once and for all to speed up equals (OK since the fetcher and its
        // parameter are immutable)
        private final String select;
        private final String fieldSeparator;
        private final String undefLabel;
        private final KeepMode keepRows;

        private final IClosure<IComboSelectionItem> customizeItem;
        private final Comparator<? super IComboSelectionItem> itemsOrder;

        // ATTN selTransformer (as the fetcher and the where) should be immutable
        public CacheKey(SQLRowValues graph, SQLRowValuesListFetcher f, Where w, ITransformer<SQLSelect, SQLSelect> selTransformer, String fieldSeparator, String undefLabel,
                IClosure<IComboSelectionItem> c, KeepMode keepRows, Comparator<? super IComboSelectionItem> itemsOrder) {
            super();
            if (!graph.isFrozen())
                throw new IllegalArgumentException("Not frozen : " + graph);
            this.graph = graph;
            // frozen, thus immutable otherwise it will change with the filter and that will cause
            // the cache to fail
            if (f != null && !f.isFrozen())
                throw new IllegalArgumentException("Not frozen : " + f);
            this.fetcher = f;
            this.where = w;
            this.selTransformer = selTransformer;
            this.select = this.fetcher.getReq(this.where, this.selTransformer).asString();

            this.fieldSeparator = fieldSeparator;
            this.undefLabel = undefLabel;
            this.keepRows = keepRows;

            this.customizeItem = c;
            this.itemsOrder = itemsOrder;
        }

        public final KeepMode getKeepMode() {
            return this.keepRows;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((this.fetcher == null) ? 0 : this.fetcher.hashCode());
            result = prime * result + this.fieldSeparator.hashCode();
            result = prime * result + this.keepRows.hashCode();
            result = prime * result + ((this.undefLabel == null) ? 0 : this.undefLabel.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null || getClass() != obj.getClass())
                return false;
            final CacheKey other = (CacheKey) obj;
            return this.keepRows == other.keepRows && this.fieldSeparator.equals(other.fieldSeparator) && CompareUtils.equals(this.undefLabel, other.undefLabel)
                    && this.graph.getGraphFirstDifference(other.graph, true) == null && this.select.equals(other.select) && CompareUtils.equals(this.customizeItem, other.customizeItem)
                    && CompareUtils.equals(this.itemsOrder, other.itemsOrder);
        }
    };

    public static enum KeepMode {
        /**
         * Only the ID is kept.
         */
        NONE,
        /**
         * Only the {@link SQLRow} is kept.
         */
        ROW,
        /**
         * The full {@link SQLRowValues graph} is kept.
         */
        GRAPH
    }

    private static final String SEP_CHILD = " ◄ ";
    @GuardedBy("this")
    private static String SEP_FIELD;
    @GuardedBy("this")
    private static Comparator<? super IComboSelectionItem> DEFAULT_COMPARATOR;

    /**
     * Set the default {@link #setFieldSeparator(String) field separator}.
     * 
     * @param separator the default separator to use from now on.
     */
    public static synchronized void setDefaultFieldSeparator(String separator) {
        SEP_FIELD = separator;
    }

    public static synchronized String getDefaultFieldSeparator() {
        return SEP_FIELD;
    }

    public static synchronized void setDefaultItemsOrder(final Comparator<? super IComboSelectionItem> comp) {
        DEFAULT_COMPARATOR = comp;
    }

    public static synchronized Comparator<? super IComboSelectionItem> getDefaultItemsOrder() {
        return DEFAULT_COMPARATOR;
    }

    static {
        setDefaultFieldSeparator(" | ");
        setDefaultItemsOrder(null);
    }

    private static final SQLElementDirectory getDirectory(final SQLElementDirectory dir) {
        if (dir != null)
            return dir;

        final Configuration conf = Configuration.getInstance();
        return conf == null ? null : conf.getDirectory();
    }

    private static final FieldExpander getExpander(SQLElementDirectory dir) {
        dir = getDirectory(dir);
        return dir != null ? ComboSQLRequestUtils.getShowAs(dir) : FieldExpander.getEmpty();
    }

    // immutable
    private final SQLElementDirectory dir;

    @GuardedBy("this")
    private String fieldSeparator = getDefaultFieldSeparator();
    @GuardedBy("this")
    private String undefLabel;
    @GuardedBy("this")
    private KeepMode keepRows;
    @GuardedBy("this")
    private IClosure<IComboSelectionItem> customizeItem;

    @GuardedBy("this")
    private Comparator<? super IComboSelectionItem> itemsOrder;

    public ComboSQLRequest(SQLTable table, List<String> l) {
        this(table, l, null);
    }

    public ComboSQLRequest(SQLTable table, List<String> l, Where where) {
        this(table, l, where, null);
    }

    public ComboSQLRequest(SQLTable table, List<String> l, Where where, final SQLElementDirectory dir) {
        this(computeGraph(table, l, getExpander(dir)), where, dir);
    }

    public ComboSQLRequest(SQLRowValues graph, Where where, final SQLElementDirectory dir) {
        super(graph, where);
        this.dir = dir;
        this.undefLabel = null;
        // don't use memory
        this.keepRows = KeepMode.NONE;
        this.customizeItem = null;
        this.itemsOrder = getDefaultItemsOrder();
    }

    protected ComboSQLRequest(ComboSQLRequest c, final boolean freeze) {
        super(c, freeze);
        this.dir = c.dir;
        synchronized (c) {
            this.itemsOrder = c.itemsOrder;

            this.fieldSeparator = c.fieldSeparator;
            this.undefLabel = c.undefLabel;
            this.keepRows = c.keepRows;
            this.customizeItem = c.customizeItem;
        }
    }

    private final SQLElementDirectory getDirectory() {
        return getDirectory(this.dir);
    }

    @Override
    public ComboSQLRequest toUnmodifiable() {
        return this.toUnmodifiableP(this.getClass());
    }

    @Override
    public ComboSQLRequest clone() {
        synchronized (this) {
            return this.clone(false);
        }
    }

    @Override
    protected ComboSQLRequest clone(boolean forFreeze) {
        return new ComboSQLRequest(this, forFreeze);
    }

    /**
     * Set the label of the undefined row. If <code>null</code> (the default) then the undefined
     * will not be fetched, otherwise it will and its label will be <code>undefLabel</code>.
     * 
     * @param undefLabel the new label, can be <code>null</code>.
     */
    public synchronized final void setUndefLabel(final String undefLabel) {
        checkFrozen();
        this.undefLabel = undefLabel;
    }

    public synchronized final String getUndefLabel() {
        return this.undefLabel;
    }

    public synchronized final void setItemCustomizer(IClosure<IComboSelectionItem> customizeItem) {
        checkFrozen();
        this.customizeItem = customizeItem;
    }

    /**
     * Retourne le comboItem correspondant à cet ID.
     * 
     * @param id l'id voulu de la primary table.
     * @return l'élément correspondant s'il existe et n'est pas archivé, <code>null</code>
     *         autrement.
     */
    public final IComboSelectionItem getComboItem(int id) {
        // historically this method didn't use the cache
        final List<IComboSelectionItem> res = getComboItems(id, null, null, null, false, false);
        return getSole(res, id);
    }

    public final List<IComboSelectionItem> getComboItems() {
        return this.getComboItems(true);
    }

    public final List<IComboSelectionItem> getComboItems(final boolean readCache) {
        return this.getComboItems(readCache, null, null, null);
    }

    public final List<IComboSelectionItem> getComboItems(final boolean readCache, final List<String> searchQuery, final Locale locale, final Where searchForceInclude) {
        return this.getComboItems(null, searchQuery, locale, searchForceInclude, readCache, true);
    }

    private final List<IComboSelectionItem> getComboItems(final Number id, final List<String> searchQuery, final Locale locale, final Where searchForceInclude, final boolean readCache,
            final boolean writeCache) {
        final Where w = id == null ? null : new Where(this.getPrimaryTable().getKey(), "=", id);

        // this encapsulates a snapshot of our state, so this method doesn't access any of our
        // fields and doesn't need to be synchronized
        final CacheKey cacheKey = getCacheKey(w, searchQuery, locale, searchForceInclude);
        final SQLRowValuesListFetcher comboSelect = cacheKey.fetcher;
        final CacheResult<List<IComboSelectionItem>> l = cache.check(cacheKey, readCache, writeCache, comboSelect.getGraph().getGraph().getTables());
        if (l.getState() == CacheResult.State.INTERRUPTED)
            throw new RTInterruptedException("interrupted while waiting for the cache");
        else if (l.getState() == CacheResult.State.VALID)
            return l.getRes();

        try {
            // group fields by ancestor, need not be part of CacheKey assuming parent-child
            // relations don't change
            final List<Tuple2<Path, List<FieldPath>>> ancestors = ComboSQLRequestUtils.expandGroupBy(cacheKey.graph, getDirectory());
            final List<IComboSelectionItem> result = new ArrayList<IComboSelectionItem>();
            // SQLRowValuesListFetcher doesn't cache
            for (final SQLRowValues vals : comboSelect.fetch(w, cacheKey.selTransformer, null)) {
                if (Thread.currentThread().isInterrupted())
                    throw new RTInterruptedException("interrupted in fill");
                // each item should be created with the same state and since it will be put in
                // cache it should only depend on the cache key.
                result.add(createItem(vals, cacheKey, ancestors));
            }
            if (cacheKey.itemsOrder != null)
                Collections.sort(result, cacheKey.itemsOrder);

            if (writeCache)
                cache.put(l, result);

            return result;
        } catch (RuntimeException exn) {
            // don't use finally, otherwise we'll do both put() and rmRunning()
            cache.removeRunning(l);
            throw exn;
        }
    }

    protected final CacheKey getCacheKey() {
        return getCacheKey(null, null, null, null);
    }

    private final synchronized CacheKey getCacheKey(final Where w, final List<String> searchQuery, final Locale l, final Where searchForceInclude) {
        return new CacheKey(this.getGraph(), this.getFetcher(), w, this.createSearchTransformer(searchQuery, l, searchForceInclude), this.fieldSeparator, this.undefLabel, this.customizeItem,
                this.keepRows, this.itemsOrder);
    }

    @Override
    protected synchronized final SQLSelect transformSelect(SQLSelect sel) {
        sel.setExcludeUndefined(this.getUndefLabel() == null, getPrimaryTable());
        return super.transformSelect(sel);
    }

    @Override
    protected Collection<SearchField> getDefaultSearchFields() {
        final List<SearchField> res = new ArrayList<SearchField>();
        final List<Tuple2<Path, List<FieldPath>>> expandGroupBy = ComboSQLRequestUtils.expandGroupBy(getGraph(), getDirectory());
        int rank = 10;
        final ListIterator<Tuple2<Path, List<FieldPath>>> iter = expandGroupBy.listIterator(expandGroupBy.size());
        assert !iter.hasNext();
        while (iter.hasPrevious()) {
            final Tuple2<Path, List<FieldPath>> element = iter.previous();
            for (final FieldPath fp : element.get1()) {
                res.add(new SearchField(fp, SQLSearchMode.CONTAINS, rank));
            }
            rank *= 5;
        }
        return res;
    }

    @Override
    protected List<Path> getDefaultOrder() {
        // order the combo by ancestors
        final List<Tuple2<Path, List<FieldPath>>> expandGroupBy = ComboSQLRequestUtils.expandGroupBy(getGraph(), getDirectory());
        final List<Path> res = new ArrayList<Path>(expandGroupBy.size());
        for (final Tuple2<Path, List<FieldPath>> ancestor : expandGroupBy)
            res.add(0, ancestor.get0());
        return res;
    }

    public final void setNaturalItemsOrder(final boolean b) {
        this.setItemsOrder(b ? CompareUtils.<IComboSelectionItem> naturalOrder() : null);
    }

    /**
     * Set the in-memory sort on items.
     * 
     * @param comp how to sort items, <code>null</code> meaning don't sort (i.e. only
     *        {@link #getOrder() SQL order} will be used).
     */
    public synchronized final void setItemsOrder(final Comparator<? super IComboSelectionItem> comp) {
        checkFrozen();
        this.itemsOrder = comp;
    }

    public synchronized final Comparator<? super IComboSelectionItem> getItemsOrder() {
        return this.itemsOrder;
    }

    // static to make sure that this method doesn't access any instance state
    static private final IComboSelectionItem createItem(final SQLRowValues rs, final CacheKey ck, final List<Tuple2<Path, List<FieldPath>>> ancestors) {
        final String desc;
        if (ck.undefLabel != null && rs.isUndefined())
            desc = ck.undefLabel;
        else
            desc = CollectionUtils.join(ancestors, SEP_CHILD, new ITransformer<Tuple2<Path, List<FieldPath>>, Object>() {
                public Object transformChecked(Tuple2<Path, List<FieldPath>> ancestorFields) {
                    final List<String> filtered = CollectionUtils.transformAndFilter(ancestorFields.get1(), new ITransformer<FieldPath, String>() {
                        // no need to keep this Transformer in an attribute
                        // even when creating one per line it's the same speed
                        public String transformChecked(FieldPath input) {
                            return getFinalValueOf(input, rs);
                        }
                    }, IPredicate.notNullPredicate(), new ArrayList<String>());
                    return CollectionUtils.join(filtered, ck.fieldSeparator);
                }
            });
        final IComboSelectionItem res;
        if (ck.getKeepMode() == KeepMode.GRAPH)
            res = new IComboSelectionItem(rs, desc);
        else if (ck.getKeepMode() == KeepMode.ROW)
            res = new IComboSelectionItem(rs.asRow(), desc);
        else
            res = new IComboSelectionItem(rs.getID(), desc);
        if (ck.customizeItem != null)
            ck.customizeItem.executeChecked(res);
        return res;
    }

    /**
     * Renvoie la valeur du champ sous forme de String. De plus est sensé faire quelques
     * conversions, eg traduire les booléens en "oui" "non".
     * 
     * @param element le champ dont on veut la valeur.
     * @param rs un resultSet contenant le champ demandé.
     * @return la valeur du champ en String.
     */
    protected static String getFinalValueOf(FieldPath element, SQLRowValues rs) {
        String result = element.getString(rs);
        // TODO
        // if (element.getType() == "FLOAT") {
        // result = result.replace('.', ',');
        // } else if (element.getType() == "BOOL") {
        // result = result.equals("0") ? "non" : "oui";
        // }
        return result;
    }

    /**
     * Set the string that is used to join the fields of a row.
     * 
     * @param string the new separator, e.g. " | ".
     */
    public synchronized final void setFieldSeparator(String string) {
        checkFrozen();
        this.fieldSeparator = string;
    }

    /**
     * Characters that may not be displayed correctly by all fonts.
     * 
     * @return characters that may not be displayed correctly.
     */
    public synchronized String getSeparatorsChars() {
        return SEP_CHILD + this.fieldSeparator;
    }

    public synchronized final KeepMode getKeepMode() {
        return this.keepRows;
    }

    /**
     * Whether {@link IComboSelectionItem items} retain their rows.
     * 
     * @param b <code>true</code> if the rows should be retained.
     * @see IComboSelectionItem#getRow()
     */
    public final void keepRows(boolean b) {
        this.keepRows(b ? KeepMode.ROW : KeepMode.NONE);
    }

    public synchronized final void keepRows(final KeepMode mode) {
        checkFrozen();
        this.keepRows = mode;
    }
}
