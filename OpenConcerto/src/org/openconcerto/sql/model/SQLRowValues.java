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

import org.openconcerto.sql.model.SQLRowValuesCluster.State;
import org.openconcerto.sql.model.SQLRowValuesCluster.ValueChangeListener;
import org.openconcerto.sql.model.SQLTable.FieldGroup;
import org.openconcerto.sql.model.SQLTableEvent.Mode;
import org.openconcerto.sql.model.graph.Link;
import org.openconcerto.sql.model.graph.Link.Direction;
import org.openconcerto.sql.model.graph.Path;
import org.openconcerto.sql.model.graph.SQLKey.Type;
import org.openconcerto.sql.model.graph.Step;
import org.openconcerto.sql.request.Inserter;
import org.openconcerto.sql.request.Inserter.Insertion;
import org.openconcerto.sql.request.Inserter.ReturnMode;
import org.openconcerto.sql.users.UserManager;
import org.openconcerto.sql.utils.ReOrder;
import org.openconcerto.utils.CollectionMap2Itf.SetMapItf;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.CompareUtils;
import org.openconcerto.utils.CopyUtils;
import org.openconcerto.utils.ExceptionUtils;
import org.openconcerto.utils.ListMap;
import org.openconcerto.utils.RecursionType;
import org.openconcerto.utils.SetMap;
import org.openconcerto.utils.Tuple2;
import org.openconcerto.utils.cc.IClosure;
import org.openconcerto.utils.cc.ITransformer;
import org.openconcerto.utils.cc.IdentitySet;
import org.openconcerto.utils.cc.LinkedIdentitySet;
import org.openconcerto.utils.cc.TransformedMap;
import org.openconcerto.utils.convertor.NumberConvertor;
import org.openconcerto.utils.convertor.ValueConvertor;
import org.openconcerto.utils.convertor.ValueConvertorFactory;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EventListener;
import java.util.EventObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.jcip.annotations.GuardedBy;

/**
 * A class that represent a row of a table that can be modified before being inserted or updated.
 * The row might not actually exists in the database, and it might not define all the fields. One
 * can put SQLRowValues as a foreign key value, so that it will be inserted as well.
 * 
 * @author Sylvain CUAZ
 * @see #load(SQLRowAccessor, Set)
 * @see #put(String, Object)
 * @see #insert()
 * @see #update(int)
 */
public final class SQLRowValues extends SQLRowAccessor {

    public static enum ForeignCopyMode {
        /**
         * Copy no SQLRowValues.
         */
        NO_COPY,
        /**
         * Put <code>null</code> instead of the SQLRowValues. This keeps all fields.
         */
        COPY_NULL,
        /**
         * Copy the id of SQLRowValues if any, otherwise don't copy anything. This keeps the maximum
         * of information without any foreign rowValues.
         */
        COPY_ID_OR_RM,
        /**
         * Copy the id of SQLRowValues if any, otherwise copy the row. This keeps all the
         * information.
         */
        COPY_ID_OR_ROW,
        /**
         * Copy every SQLRowValues.
         */
        COPY_ROW
    }

    static public enum CreateMode {
        /**
         * Never create rows.
         */
        CREATE_NONE,
        /**
         * For non-full step, create one row with all links. For example with a step of 3 links :
         * <ul>
         * <li>if they are all filled, do nothing</li>
         * <li>if they are all empty, create one row</li>
         * <li>if one link is filled with a row, add all empty links to it</li>
         * <li>if more than one link is filled (but not all of them), error out as it would leave a
         * hybrid state : neither 2 rows joined by all the links, nor one row per link</li>
         * </ul>
         * Then follow all existing plus created rows.
         */
        CREATE_ONE,
        /**
         * Create one row for each empty link, then follow all existing plus created rows.
         */
        CREATE_MANY
    }

    public static final Object SQL_DEFAULT = new Object() {
        @Override
        public String toString() {
            return SQLRowValues.class.getSimpleName() + ".SQL_DEFAULT";
        }
    };
    /**
     * Empty foreign field value.
     * 
     * @see #putEmptyLink(String)
     */
    public static final Object SQL_EMPTY_LINK = new Object() {
        @Override
        public String toString() {
            return SQLRowValues.class.getSimpleName() + ".SQL_EMPTY_LINK";
        }
    };

    static public enum ValidityCheck {
        /**
         * The check is never performed.
         */
        FORBIDDEN {
            @Override
            public boolean shouldCheck(final Boolean asked) {
                return false;
            }
        },
        /**
         * The check is only performed if requested.
         */
        FALSE_BY_DEFAULT {
            @Override
            public boolean shouldCheck(Boolean asked) {
                return asked == null ? false : asked.booleanValue();
            }
        },
        /**
         * The check is performed unless specified.
         */
        TRUE_BY_DEFAULT {
            @Override
            public boolean shouldCheck(Boolean asked) {
                return asked == null ? true : asked.booleanValue();
            }
        },
        /**
         * The check is always performed. This is not generally recommended as some methods of the
         * framework will fail.
         */
        FORCED {
            @Override
            public boolean shouldCheck(Boolean asked) {
                return true;
            }
        };

        public abstract boolean shouldCheck(final Boolean asked);
    }

    @GuardedBy("this")
    private static ValidityCheck checkValidity;

    /**
     * Set how {@link #getInvalid()} should be called before each data modification. Initially set
     * to {@link ValidityCheck#TRUE_BY_DEFAULT}. NOTE : that the check also makes sure that
     * referenced rows are not archived, so if it isn't performed a row could point to an archived
     * row.
     * 
     * @param vc the new mode, <code>null</code> to set the default.
     */
    public synchronized static void setValidityChecked(final ValidityCheck vc) {
        checkValidity = vc == null ? ValidityCheck.TRUE_BY_DEFAULT : vc;
    }

    /**
     * Whether or not {@link #getInvalid()} should be called.
     * 
     * @param asked what the caller requested.
     * @return <code>true</code> if the validity is checked.
     */
    public synchronized static boolean isValidityChecked(final Boolean asked) {
        return checkValidity.shouldCheck(asked);
    }

    static {
        setValidityChecked(null);
    }

    private static final boolean DEFAULT_ALLOW_BACKTRACK = true;

    // i.e. no re-hash for up to 6 entries (8*0.8=6.4)
    private static final int DEFAULT_VALUES_CAPACITY = 8;
    private static final float DEFAULT_LOAD_FACTOR = 0.8f;

    // Assure there's no copy. Don't just return plannedSize : e.g. for HashMap if it's 15
    // the initial capacity will be 16 (the nearest power of 2) and threshold will be 12.8 (with
    // our load of 0.8) so there would be a rehash at the 13th items.
    private static final int getCapacity(final int plannedSize, final int defaultCapacity) {
        return plannedSize < 0 ? defaultCapacity : Math.max((int) (plannedSize / DEFAULT_LOAD_FACTOR) + 1, 4);
    }

    private static final LinkedHashMap<String, Object> createLinkedHashMap(final int plannedSize) {
        if (plannedSize < 0)
            throw new IllegalArgumentException("Negative capacity");
        return createLinkedHashMap(plannedSize, -1);
    }

    private static final <K, V> LinkedHashMap<K, V> createLinkedHashMap(final int plannedSize, final int defaultCapacity) {
        return new LinkedHashMap<K, V>(getCapacity(plannedSize, defaultCapacity), DEFAULT_LOAD_FACTOR);
    }

    private static final <K> SetMap<K, SQLRowValues> createSetMap(final int plannedSize, final int defaultCapacity) {
        return new SetMap<K, SQLRowValues>(new HashMap<K, Set<SQLRowValues>>(getCapacity(plannedSize, defaultCapacity), DEFAULT_LOAD_FACTOR), org.openconcerto.utils.CollectionMap2.Mode.NULL_FORBIDDEN, false) {
            @Override
            public Set<SQLRowValues> createCollection(Collection<? extends SQLRowValues> coll) {
                // use LinkedHashSet so that the order is preserved, eg we can iterate over LOCALs
                // pointing to a BATIMENT with consistent and predictable (insertion-based) order.
                // use IdentitySet to be able to put two equal instances
                return coll == null ? new LinkedIdentitySet<SQLRowValues>() : new LinkedIdentitySet<SQLRowValues>(coll);
            }
        };
    }

    private final Map<String, Object> values;
    private final Map<String, SQLRowValues> foreigns;
    private final SetMap<SQLField, SQLRowValues> referents;
    private SQLRowValuesCluster graph;
    private ListMap<SQLField, ReferentChangeListener> referentsListener;

    public SQLRowValues(SQLTable t) {
        this(t, -1, -1, -1);
    }

    /**
     * Create a new instance.
     * 
     * @param t the table.
     * @param valuesPlannedSize no further allocations will be made until that number of
     *        {@link #getAbsolutelyAll() values}, pass a negative value to use a default.
     * @param foreignsPlannedSize no further allocations will be made until that number of
     *        {@link #getForeigns() foreigns}, pass a negative value to use a default.
     * @param referentsPlannedSize no further allocations will be made until that number of
     *        {@link #getReferentsMap() referents}, pass a negative value to use a default.
     * 
     */
    public SQLRowValues(SQLTable t, final int valuesPlannedSize, final int foreignsPlannedSize, final int referentsPlannedSize) {
        super(t);
        // use LinkedHashSet so that the order is preserved, see #walkFields()
        this.values = createLinkedHashMap(valuesPlannedSize, DEFAULT_VALUES_CAPACITY);
        // foreigns order should be coherent with values
        this.foreigns = createLinkedHashMap(foreignsPlannedSize, 4);
        this.referents = createSetMap(referentsPlannedSize, 4);
        // no used much so lazy init
        this.referentsListener = null;
        // Allow to reduce memory for lonely rows, and even for linked rows since before :
        // 1. create a row, create a cluster
        // 2. create a second row, create a second cluster
        // 3. put, the second row uses the first cluster, the second one can be collected
        // Now the second cluster is never created, see SQLRowValuesCluster.add().
        this.graph = null;
    }

    public SQLRowValues(SQLTable t, Map<String, ?> values) {
        this(t, values.size(), -1, -1);
        this.setAll(values);
    }

    public SQLRowValues(SQLRowValues vals) {
        this(vals, ForeignCopyMode.COPY_ROW);
    }

    /**
     * Create a new instance with the same values. If <code>copyForeigns</code> is <code>true</code>
     * the new instance will have exactly the same values, ie it will point to the same
     * SQLRowValues. If <code>copyForeigns</code> is <code>false</code> all SQLRowValues will be
     * left out.
     * 
     * @param vals the instance to copy.
     * @param copyForeigns whether to copy foreign SQLRowValues.
     */
    public SQLRowValues(SQLRowValues vals, ForeignCopyMode copyForeigns) {
        // setAll() takes care of foreigns and referents
        this(vals.getTable(), vals.getAllValues(copyForeigns));
    }

    /**
     * Copy this rowValues and all others connected to it. Ie contrary to
     * {@link #SQLRowValues(SQLRowValues)} the result will not point to the same rowValues, but to
     * copy of them.
     * 
     * @return a copy of this.
     */
    public final SQLRowValues deepCopy() {
        return this.getGraph().deepCopy(this, false);
    }

    /**
     * Get a frozen version of this. If not already {@link #isFrozen() frozen}, copy this rowValues
     * and all others connected to it and {@link SQLRowValuesCluster#freeze()} the copy. I.e. if the
     * result is to be shared among threads, it still needs to be safely published.
     * 
     * @return this if already frozen, otherwise a frozen copy of this.
     */
    public final SQLRowValues toImmutable() {
        if (this.isFrozen())
            return this;
        return this.getGraph().deepCopy(this, true);
    }

    // *** graph

    private void updateLinks(String fieldName, Object old, Object value) {
        // try to avoid getTable().getField() (which takes 1/3 of put() for nothing when there is no
        // rowvalues)
        final boolean oldRowVals = old instanceof SQLRowValues;
        final boolean newRowVals = value instanceof SQLRowValues;
        if (!oldRowVals && !newRowVals)
            return;

        final SQLField f = this.getTable().getField(fieldName);

        if (oldRowVals) {
            final SQLRowValues vals = (SQLRowValues) old;
            vals.referents.remove(f, this);
            this.foreigns.remove(fieldName);
            assert this.graph == vals.graph;
            this.graph.remove(this, f, vals);
            vals.fireRefChange(f, false, this);
        }
        if (newRowVals) {
            final SQLRowValues vals = (SQLRowValues) value;
            vals.referents.add(f, this);
            this.foreigns.put(fieldName, vals);
            // prefer vals' graph as add() is faster that way
            final SQLRowValuesCluster usedGraph = this.graph != null && vals.graph == null ? this.graph : vals.getGraph();
            usedGraph.add(this, f, vals);
            assert this.graph == vals.graph;
            vals.fireRefChange(f, true, this);
        }
    }

    /**
     * Return the graph for this instance. NOTE: for single row values the graph is only created on
     * demand.
     * 
     * @return the graph.
     */
    public final SQLRowValuesCluster getGraph() {
        return this.getGraph(true);
    }

    final SQLRowValuesCluster getGraph(final boolean create) {
        if (create && this.graph == null)
            this.graph = new SQLRowValuesCluster(this);
        return this.graph;
    }

    /**
     * The number of items in our graph. NOTE: this method doesn't allocate a graph.
     * 
     * @return the number of items in our graph.
     * @see SQLRowValuesCluster#size()
     */
    public final int getGraphSize() {
        final SQLRowValuesCluster g = this.getGraph(false);
        return g == null ? 1 : g.size();
    }

    public final <T> void walkGraph(T acc, ITransformer<State<T>, T> closure) {
        this.getGraph().walk(this, acc, closure);
    }

    /**
     * Walk through the fields of the rowValues in order. Eg if you added DESIGNATION, ID_BATIMENT
     * pointing to {DESIGNATION}, then INCLURE, <code>closure</code> would be called with
     * LOCAL.DESIGNATION, LOCAL.ID_BATIMENT.DESIGNATION, LOCAL.INCLURE. This can't be done using
     * {@link SQLRowValuesCluster#walk(SQLRowValues, Object, ITransformer, RecursionType)} since it
     * walks through rowValues so if you use {@link RecursionType#BREADTH_FIRST} you'll be passed
     * LOCAL, then BATIMENT and the reverse if you use {@link RecursionType#DEPTH_FIRST}.
     * 
     * @param closure what to do on each field.
     */
    public final void walkFields(IClosure<FieldPath> closure) {
        this.walkFields(closure, false);
    }

    public final void walkFields(IClosure<FieldPath> closure, final boolean includeFK) {
        this.getGraph().walkFields(this, closure, includeFK);
    }

    public final SQLRowValues prune(SQLRowValues graph) {
        return this.getGraph().prune(this, graph);
    }

    /**
     * Fetch if necessary and store in this the foreign row.
     * 
     * @param fk a foreign key, eg "ID_FAMILLE_2".
     * @return the foreign row, eg FAMILLE[1].
     */
    public final SQLRowValues grow(String fk) {
        final Object val = this.getContainedObject(fk);
        // if fk is in our map with a null value, nothing to grow
        if (val != null && !(val instanceof SQLRowValues)) {
            final SQLRowValues vals = new SQLRowValues(this.getTable());
            vals.putRowValues(fk).setAllToNull();
            this.grow(vals, true);
        }
        return (SQLRowValues) this.getForeign(fk);
    }

    public final SQLRowValues grow(SQLRowValues graph) {
        return this.grow(graph, true);
    }

    /**
     * Grow this rowValues to match the passed graph. If this was /RECEPTEUR/ : {DESIGNATION="des";
     * ID_LOCAL=2} and <code>graph</code> is /RECEPTEUR/ : {DESIGNATION=null; ID_LOCAL:
     * /LOCAL/:{DESIGNATION=null}}, then now this is /RECEPTEUR/ : {DESIGNATION="des"; ID_LOCAL:
     * /LOCAL/:{ID=2, DESIGNATION="local"}}
     * 
     * @param graph the target graph.
     * @param checkFields <code>true</code> if missing fields should be fetched.
     * @return this.
     * @throws IllegalArgumentException if this couldn't be grown.
     */
    public final SQLRowValues grow(SQLRowValues graph, final boolean checkFields) {
        graph.getGraph().grow(graph, this, checkFields);
        return this;
    }

    public final boolean graphContains(SQLRowValues graph) {
        return this.getGraph().contains(this, graph) == null;
    }

    void setGraph(SQLRowValuesCluster g) {
        assert g != null;
        this.graph = g;
    }

    public final boolean hasForeigns() {
        // OK since updateLinks() removes empty map entries
        return !this.foreigns.isEmpty();
    }

    public final Map<String, SQLRowValues> getForeigns() {
        return Collections.unmodifiableMap(this.foreigns);
    }

    final int getForeignsSize() {
        return this.foreigns.size();
    }

    final Map<SQLField, SQLRowValues> getForeignsBySQLField() {
        return new TransformedMap<String, SQLField, SQLRowValues>(this.getForeigns(), new ITransformer<String, SQLField>() {
            @Override
            public SQLField transformChecked(String input) {
                return getTable().getField(input);
            }
        }, new ITransformer<SQLField, String>() {
            @Override
            public String transformChecked(SQLField input) {
                return input.getName();
            }
        });
    }

    // package private since the result is modifiable, see below for the public version
    final SetMap<SQLField, SQLRowValues> getReferents() {
        return this.referents;
    }

    public final SetMapItf<SQLField, SQLRowValues> getReferentsMap() {
        return SetMap.unmodifiableMap(this.referents);
    }

    public final boolean hasReferents() {
        // OK since updateLinks() removes empty map entries
        return !this.referents.isEmpty();
    }

    @Override
    public Collection<SQLRowValues> getReferentRows() {
        // remove the backdoor since values() returns a view
        // remove duplicates (e.g. this is a CONTACT referenced by ID_CONTACT_RAPPORT &
        // ID_CONTACT_RDV from the same site)
        return this.referents.createCollection(this.referents.allValues());
    }

    @Override
    public Set<SQLRowValues> getReferentRows(SQLField refField) {
        return Collections.unmodifiableSet(this.referents.getNonNull(refField));
    }

    @Override
    public Collection<SQLRowValues> getReferentRows(SQLTable refTable) {
        // remove duplicates
        final Collection<SQLRowValues> res = this.referents.createCollection(null);
        assert res.isEmpty();
        for (final Map.Entry<SQLField, Set<SQLRowValues>> e : this.referents.entrySet()) {
            if (e.getKey().getTable().equals(refTable))
                res.addAll(e.getValue());
        }
        return res;
    }

    /**
     * Remove all links pointing to this from the referent rows.
     * 
     * @return this.
     */
    public final SQLRowValues clearReferents() {
        return this.changeReferents(ForeignCopyMode.NO_COPY);
    }

    public final SQLRowValues changeReferents(final ForeignCopyMode mode) {
        return this.changeReferents(null, false, mode);
    }

    public final SQLRowValues removeReferents(final SQLField f) {
        // don't use changeReferents() as it's less optimal
        for (final SQLRowValues ref : new ArrayList<SQLRowValues>(this.getReferentRows(f))) {
            ref.remove(f.getName());
        }
        return this;
    }

    public final SQLRowValues removeReferentFields(final Collection<SQLField> fields) {
        return this.changeReferents(fields, false);
    }

    public final SQLRowValues retainReferentFields(final Collection<SQLField> fields) {
        return this.changeReferents(fields, true);
    }

    private final SQLRowValues changeReferents(final Collection<SQLField> fields, final boolean retain) {
        return this.changeReferents(fields, retain, ForeignCopyMode.NO_COPY);
    }

    /**
     * Change referents. NOTE : depending on the {@link ForeignCopyMode mode} this method may detach
     * this row from some of its referents.
     * 
     * @param fields the fields to change or to exclude from change.
     * @param exclude <code>true</code> if fields passed to this method must be excluded from the
     *        change, <code>false</code> to only change fields passed to this method.
     * @param mode how the referent row will be changed.
     * @return this.
     */
    public final SQLRowValues changeReferents(final Collection<SQLField> fields, final boolean exclude, final ForeignCopyMode mode) {
        if (!isEmpty(fields, exclude) && mode != ForeignCopyMode.COPY_ROW) {
            // copy otherwise ConcurrentModificationException
            for (final Entry<SQLField, Set<SQLRowValues>> e : CopyUtils.copy(this.getReferents()).entrySet()) {
                // fields == null means !retain thanks to the above if
                if (fields == null || fields.contains(e.getKey()) != exclude) {
                    for (final SQLRowValues ref : e.getValue()) {
                        ref.flatten(e.getKey().getName(), mode);
                    }
                }
            }
        }
        return this;
    }

    public SQLRowValues retainReferent(SQLRowValues toRetain) {
        return this.retainReferents(Collections.singleton(toRetain));
    }

    public SQLRowValues retainReferents(Collection<SQLRowValues> toRetain) {
        toRetain = CollectionUtils.toIdentitySet(toRetain);
        // copy otherwise ConcurrentModificationException
        for (final Entry<SQLField, Set<SQLRowValues>> e : CopyUtils.copy(this.getReferents()).entrySet()) {
            for (final SQLRowValues ref : e.getValue()) {
                if (!toRetain.contains(ref))
                    ref.remove(e.getKey().getName());
            }
        }
        return this;
    }

    // *** get

    public int size() {
        return this.values.size();
    }

    @Override
    public final int getID() {
        final Number res = this.getIDNumber(false);
        if (res != null)
            return res.intValue();
        else
            return SQLRow.NONEXISTANT_ID;
    }

    @Override
    public Number getIDNumber() {
        // We never have rows in the DB with NULL primary key, so a null result means no value was
        // specified (or null was programmatically specified)
        return this.getIDNumber(false);
    }

    public final Number getIDNumber(final boolean mustBePresent) {
        final Object res = this.getObject(this.getTable().getKey().getName(), mustBePresent);
        if (res == null) {
            return null;
        } else {
            return (Number) res;
        }
    }

    @Override
    public final Object getObject(String fieldName) {
        return this.values.get(fieldName);
    }

    @Override
    public Map<String, Object> getAbsolutelyAll() {
        return getAllValues(ForeignCopyMode.COPY_ROW);
    }

    protected final Map<String, Object> getAllValues(ForeignCopyMode copyForeigns) {
        return this.getAllValues(copyForeigns, false);
    }

    private final Map<String, Object> getAllValues(ForeignCopyMode copyForeigns, final boolean copy) {
        final Map<String, Object> toAdd;
        if (copyForeigns == ForeignCopyMode.COPY_ROW || this.foreigns.size() == 0) {
            if (copy) {
                toAdd = createLinkedHashMap(this.size());
                toAdd.putAll(this.values);
            } else {
                toAdd = this.values;
            }
        } else {
            final Set<Entry<String, Object>> entrySet = this.values.entrySet();
            toAdd = createLinkedHashMap(entrySet.size());
            for (final Map.Entry<String, Object> e : entrySet) {
                if (!(e.getValue() instanceof SQLRowValues)) {
                    toAdd.put(e.getKey(), e.getValue());
                } else if (copyForeigns == ForeignCopyMode.COPY_NULL) {
                    toAdd.put(e.getKey(), null);
                } else if (copyForeigns != ForeignCopyMode.NO_COPY) {
                    final SQLRowValues foreign = (SQLRowValues) e.getValue();
                    if (foreign.hasID())
                        toAdd.put(e.getKey(), foreign.getIDNumber());
                    else if (copyForeigns == ForeignCopyMode.COPY_ID_OR_ROW)
                        toAdd.put(e.getKey(), foreign);
                }
            }
        }
        return copy ? toAdd : Collections.unmodifiableMap(toAdd);
    }

    /**
     * All current groups of this row.
     * 
     * @return the ordered groups.
     * @throws IllegalStateException if a group is incomplete (e.g. a primary key has only one of
     *         its two values).
     */
    public final Set<FieldGroup> getFieldGroups() throws IllegalStateException {
        final Set<String> fields = this.getFields();
        // keep order
        final LinkedHashSet<FieldGroup> set = new LinkedHashSet<FieldGroup>();
        final Map<String, FieldGroup> tableGroups = this.getTable().getFieldGroups();
        for (final String fieldName : fields) {
            final FieldGroup group = tableGroups.get(fieldName);
            // check that groups are complete
            if (set.add(group)) {
                if (!fields.containsAll(group.getFields()))
                    throw new IllegalStateException("Missing fields for " + group + ", current fields : " + fields);
            }
        }
        return set;
    }

    /**
     * Return the foreign row, if any, for the passed field.
     * 
     * @param fieldName name of the foreign field.
     * @return if <code>null</code> or a SQLRowValues one was put at <code>fieldName</code>, return
     *         it ; else assume that an ID was put at <code>fieldName</code> and return a new SQLRow
     *         with it.
     * @throws IllegalArgumentException if fieldName is not a foreign field or if it isn't contained
     *         in this instance.
     * @throws ClassCastException if the value is neither a SQLRowValues, nor <code>null</code> nor
     *         a Number.
     */
    @Override
    public final SQLRowAccessor getForeign(String fieldName) throws IllegalArgumentException, ClassCastException {
        return this.getForeign(this.getForeignLink(Collections.singletonList(fieldName)));
    }

    public final SQLRowAccessor getForeign(final Link l) throws IllegalArgumentException {
        if (!l.getSource().equals(this.getTable()))
            throw new IllegalArgumentException(l + " not from " + this);
        final String fieldName = l.getSingleField().getName();
        final Object val = this.getContainedObject(fieldName);
        if (val instanceof SQLRowAccessor) {
            return (SQLRowAccessor) val;
        } else if (val == null) {
            // since we used getContainedObject(), it means that a null was put in our map, not that
            // fieldName wasn't there
            return null;
        } else if (this.isDefault(fieldName)) {
            throw new IllegalStateException(fieldName + " is DEFAULT");
        } else {
            return new SQLRow(l.getTarget(), this.getInt(fieldName));
        }
    }

    public boolean isDefault(String fieldName) {
        return SQL_DEFAULT.equals(this.getObject(fieldName));
    }

    /**
     * Retourne les champs spécifiés par cette instance.
     * 
     * @return l'ensemble des noms des champs.
     */
    @Override
    public Set<String> getFields() {
        return Collections.unmodifiableSet(this.values.keySet());
    }

    @Override
    public final SQLRow asRow() {
        if (!this.hasID())
            throw new IllegalStateException(this + " has no ID");
        return new SQLRow(this.getTable(), this.getAllValues(ForeignCopyMode.COPY_ID_OR_RM));
    }

    @Override
    public final SQLRowValues asRowValues() {
        return this;
    }

    // *** set

    /**
     * Whether this can be modified.
     * 
     * @return <code>true</code> if this (and its graph) is not modifiable.
     */
    public final boolean isFrozen() {
        final SQLRowValuesCluster g = this.getGraph(false);
        return g != null && g.isFrozen();
    }

    private void checkFrozen() {
        if (this.isFrozen())
            throw new IllegalStateException("Graph is not modifiable");
    }

    /**
     * Retains only the fields in this that are contained in the specified collection. In other
     * words, removes all of its elements that are not contained in the specified collection.
     * 
     * @param fields collection containing elements to be retained, <code>null</code> meaning all.
     * @return this.
     */
    public final SQLRowValues retainAll(Collection<String> fields) {
        return this.changeFields(fields, true);
    }

    private final SQLRowValues changeFields(Collection<String> fields, final boolean retain) {
        return this.changeFields(fields, retain, false);
    }

    public final SQLRowValues changeFields(Collection<String> fields, final boolean retain, final boolean protectGraph) {
        if (isEmpty(fields, retain))
            return this;
        // clear all on an empty values == no-op
        if (!retain && fields == null && this.size() == 0)
            return this;

        final Set<String> toRm = new HashSet<String>(this.values.keySet());
        if (protectGraph)
            toRm.removeAll(this.foreigns.keySet());
        // fields == null => !retain => clear()
        if (fields != null) {
            if (retain) {
                toRm.removeAll(fields);
            } else {
                toRm.retainAll(fields);
            }
        }
        // nothing to change
        if (toRm.isEmpty())
            return this;
        // handle links
        final Map<String, FieldGroup> fieldGroups = getTable().getFieldGroups();
        for (final String fieldName : toRm) {
            if (fieldGroups.get(fieldName).getKeyType() == Type.FOREIGN_KEY)
                // name is OK since it is a foreign key
                // value null is also OK
                this._put(fieldName, null, false, ValueOperation.CHECK);
        }
        if (fields == null && !protectGraph) {
            assert !retain && toRm.equals(this.values.keySet());
            this.values.clear();
        } else {
            this.values.keySet().removeAll(toRm);
        }
        // if there's no graph, there can't be any listeners
        final SQLRowValuesCluster graph = this.getGraph(false);
        if (graph != null)
            graph.fireModification(this, toRm);
        return this;
    }

    /**
     * Removes from this all fields that are contained in the specified collection.
     * 
     * @param fields collection containing elements to be removed, <code>null</code> meaning all.
     * @return this.
     */
    public final SQLRowValues removeAll(Collection<String> fields) {
        return this.changeFields(fields, false);
    }

    public final void remove(String field) {
        // check arg & handle links
        this.put(field, null);
        // really remove
        assert !this.isFrozen() : "Should already be checked by put(null)";
        this.values.remove(field);
    }

    public final void clear() {
        this.removeAll(null);
    }

    public final void clearPrimaryKeys() {
        checkFrozen();
        this.clearPrimaryKeys(this.values);
        // by definition primary keys are not foreign keys, so no need to updateLinks()
    }

    private Map<String, Object> clearPrimaryKeys(final Map<String, Object> values) {
        return clearFields(values, this.getTable().getPrimaryKeys());
    }

    private Map<String, Object> clearFields(final Map<String, Object> values, final Set<SQLField> fields) {
        return changeFields(values, fields, false);
    }

    private Map<String, Object> changeFields(final Map<String, Object> values, final Set<SQLField> fields, final boolean retain) {
        final Iterator<String> iter = values.keySet().iterator();
        while (iter.hasNext()) {
            final String fieldName = iter.next();
            if (fields.contains(this.getTable().getField(fieldName)) ^ retain)
                iter.remove();
        }
        return values;
    }

    /**
     * Change foreign and referent rows. NOTE : this doesn't change all foreign keys, only those
     * that contain an {@link SQLRowValues}.
     * 
     * @param paths the first steps are to be changed or to be excluded from change,
     *        <code>null</code> meaning all.
     * @param exclude <code>true</code> if steps passed to this method must be excluded from the
     *        change, <code>false</code> to only change steps passed to this method.
     * @param mode how the rows will be changed.
     * @return this.
     */
    public final SQLRowValues changeGraph(final Collection<Path> paths, final boolean exclude, ForeignCopyMode mode) {
        if (this.getGraphSize() == 1)
            return this;

        final Set<SQLField> refFields;
        final Set<String> foreignFields;
        if (paths == null) {
            refFields = null;
            foreignFields = null;
        } else {
            refFields = new HashSet<SQLField>();
            foreignFields = new HashSet<String>();
            for (final Path p : paths) {
                if (p.getFirst() != this.getTable())
                    throw new IllegalArgumentException("Path not from this : " + p);
                if (p.length() > 0) {
                    final Step step = p.getStep(0);
                    for (final Link l : step.getLinks()) {
                        if (step.getDirection(l) == Direction.REFERENT)
                            refFields.addAll(l.getFields());
                        else
                            foreignFields.addAll(l.getCols());
                    }
                }
            }
        }
        changeForeigns(foreignFields, exclude, mode);
        changeReferents(refFields, exclude, mode);
        return this;
    }

    public final void detach() {
        // keep the most information
        this.detach(ForeignCopyMode.COPY_ID_OR_RM);
    }

    public final void detach(final ForeignCopyMode mode) {
        if (mode.compareTo(ForeignCopyMode.COPY_ID_OR_ROW) >= 0)
            throw new IllegalArgumentException("Might keep row and not detach : " + mode);
        this.changeGraph(null, false, mode);
        assert this.getGraphSize() == 1;
    }

    // puts

    public SQLRowValues put(String fieldName, Object value) {
        return this.put(fieldName, value, true);
    }

    SQLRowValues put(String fieldName, Object value, final boolean check) {
        return this.put(fieldName, value, check, check ? ValueOperation.CONVERT : ValueOperation.PASS);
    }

    SQLRowValues put(String fieldName, Object value, final boolean checkName, final ValueOperation checkValue) {
        _put(fieldName, value, checkName, checkValue);
        // if there's no graph, there can't be any listeners
        final SQLRowValuesCluster graph = this.getGraph(false);
        if (graph != null)
            graph.fireModification(this, fieldName, value);
        return this;
    }

    static public enum ValueOperation {
        CONVERT, CHECK, PASS
    }

    // TODO support java.time.LocalDateTime in Java 8
    static private <T, U> U convert(final Class<T> source, final Object value, final Class<U> dest) {
        final ValueConvertor<T, U> conv = ValueConvertorFactory.find(source, dest);
        if (conv == null)
            throw new IllegalArgumentException("No convertor to " + dest + " from " + source);
        assert source == value.getClass();
        @SuppressWarnings("unchecked")
        final T tVal = (T) value;
        return conv.convert(tVal);
    }

    private void _put(String fieldName, Object value, final boolean checkName, final ValueOperation checkValue) {
        // table.contains() can take up to 35% of this method
        if (checkName && !this.getTable().contains(fieldName))
            throw new IllegalArgumentException(fieldName + " is not in table " + this.getTable());
        if (value == SQL_EMPTY_LINK) {
            // keep getForeignTable since it does the check
            value = this.getForeignTable(fieldName).getUndefinedIDNumber();
        } else if (value != null && value != SQL_DEFAULT && checkValue != ValueOperation.PASS) {
            final SQLField field = this.getTable().getField(fieldName);
            if (value instanceof SQLRowValues) {
                if (!field.isForeignKey())
                    throw new IllegalArgumentException("Since value is a SQLRowValues, expected a foreign key but got " + field);
            } else {
                final Class<?> javaType = field.getType().getJavaType();
                if (!javaType.isInstance(value)) {
                    if (checkValue == ValueOperation.CONVERT) {
                        value = convert(value.getClass(), value, javaType);
                    } else {
                        throw new IllegalArgumentException("Wrong type for " + fieldName + ", expected " + javaType + " but got " + value.getClass());
                    }
                }
            }
        }
        checkFrozen();
        this.updateLinks(fieldName, this.values.put(fieldName, value), value);
    }

    public SQLRowValues put(String fieldName, int value) {
        return this.put(fieldName, Integer.valueOf(value));
    }

    public SQLRowValues putDefault(String fieldName) {
        return this.put(fieldName, SQL_DEFAULT);
    }

    /**
     * To empty a foreign key.
     * 
     * @param fieldName the name of the foreign key to empty.
     * @return this.
     */
    public SQLRowValues putEmptyLink(String fieldName) {
        return this.put(fieldName, SQL_EMPTY_LINK);
    }

    /**
     * Set a new {@link SQLRowValues} as the value of <code>fieldName</code>. ATTN contrary to many
     * methods this one do not return <code>this</code>.
     * 
     * @param fieldName the name of a foreign field.
     * @return the newly created values.
     * @throws IllegalArgumentException if <code>fieldName</code> is not a foreign field.
     */
    public final SQLRowValues putRowValues(String fieldName) throws IllegalArgumentException {
        // getForeignTable checks
        final SQLRowValues vals = new SQLRowValues(this.getForeignTable(fieldName));
        this.put(fieldName, vals);
        return vals;
    }

    public final SQLRowValues putRowValues(final Path p, final boolean createPath) throws IllegalArgumentException {
        return this.put(p, createPath, null);
    }

    /**
     * Create or follow the passed path and put the passed row at the end.
     * 
     * @param p the {@link Path#isSingleLink() single link} path.
     * @param createPath <code>true</code> if new rows must {@link #createPathToOne(Path) always be
     *        created}, <code>false</code> if existing rows can be {@link #assurePath(Path) used}.
     * @param vals the row to {@link #put(Step, SQLRowValues) put}, <code>null</code> to create a
     *        new one.
     * @return the row that was added.
     * @throws IllegalArgumentException if the path is invalid.
     */
    public final SQLRowValues put(final Path p, final boolean createPath, final SQLRowValues vals) throws IllegalArgumentException {
        if (p.length() == 0)
            throw new IllegalArgumentException("Empty path");
        if (!p.isSingleLink())
            throw new IllegalArgumentException("Multi-link path " + p);
        // checks first table
        final SQLRowValues beforeLast = createPath ? this.createPathToOne(p.minusLast()) : this.assurePath(p.minusLast());
        // checks last table
        return beforeLast.put(p.getStep(-1), vals);
    }

    public final SQLRowValues putRowValues(final Step step) throws IllegalArgumentException {
        return this.put(step, null);
    }

    /**
     * Add all links of the passed step from this to the passed row.
     * 
     * @param step a step.
     * @param vals a row, <code>null</code> to create a new one.
     * @return the row that was linked.
     * @throws IllegalArgumentException if the step is not from <code>this</code> to
     *         <code>vals</code>.
     */
    public final SQLRowValues put(final Step step, SQLRowValues vals) throws IllegalArgumentException {
        if (!step.getFrom().equals(this.getTable()))
            throw new IllegalArgumentException(step + " not from " + this);
        if (vals == null)
            vals = new SQLRowValues(step.getTo());
        else if (!step.getTo().equals(vals.getTable()))
            throw new IllegalArgumentException(step + " not to " + vals);
        for (final Link l : step.getLinks()) {
            final Direction dir = step.getDirection(l);
            if (dir == Direction.REFERENT) {
                vals._putForeign(l, this);
            } else {
                assert dir == Direction.FOREIGN;
                this._putForeign(l, vals);
            }
        }
        return vals;
    }

    private final SQLRowValues _putForeign(final Link l, SQLRowValues vals) {
        this.put(l.getSingleField().getName(), vals);
        return vals;
    }

    public final SQLRowValues putForeign(final Link l, SQLRowValues vals) throws IllegalArgumentException {
        if (!l.getSource().equals(this.getTable()))
            throw new IllegalArgumentException(l + " not from " + this);
        return _putForeign(l, vals);
    }

    public final void remove(final Step step) {
        for (final Link l : step.getLinks()) {
            if (step.getDirection(l) == Direction.FOREIGN)
                this.removeForeignKey(l);
            else
                this.removeReferentFields(l.getFields());
        }
    }

    /**
     * Safely set the passed field to the value of the primary key of <code>r</code>.
     * 
     * @param fk the field to change.
     * @param r the row, <code>null</code> meaning {@link #SQL_EMPTY_LINK empty} foreign key.
     * @return this.
     * @throws IllegalArgumentException if <code>fk</code> doesn't point to the table of
     *         <code>r</code>.
     */
    public final SQLRowValues putForeignID(final String fk, final SQLRowAccessor r) throws IllegalArgumentException {
        return this.putForeignKey(Collections.singletonList(fk), r);
    }

    public final SQLRowValues putForeignKey(final List<String> cols, final SQLRowAccessor r) throws IllegalArgumentException {
        // first check that cols are indeed a foreign key
        return this.putForeignKey(this.getForeignLink(cols), r);
    }

    public final SQLRowValues putForeignKey(final Link foreignLink, final SQLRowAccessor r) throws IllegalArgumentException {
        checkForeignLink(foreignLink);
        final List<String> cols = foreignLink.getCols();
        if (r == null) {
            if (cols.size() == 1) {
                return this.putEmptyLink(cols.get(0));
            } else {
                return this.putNulls(cols);
            }
        } else {
            checkSameTable(r, foreignLink.getTarget());
            final Iterator<String> iter = cols.iterator();
            final Iterator<String> refIter = foreignLink.getRefCols().iterator();
            while (iter.hasNext()) {
                final String col = iter.next();
                final String refCol = refIter.next();
                this.put(col, r.getObject(refCol));
            }
            return this;
        }
    }

    private void checkForeignLink(final Link foreignLink) {
        if (foreignLink.getSource() != this.getTable())
            throw new IllegalArgumentException("Link not from " + this.getTable() + " : " + foreignLink);
    }

    public final void removeForeignKey(final Link foreignLink) {
        checkForeignLink(foreignLink);
        this.removeAll(foreignLink.getCols());
    }

    private void checkSameTable(final SQLRowAccessor r, final SQLTable t) {
        if (r.getTable() != t)
            throw new IllegalArgumentException("Table mismatch : " + r.getTable().getSQLName() + " != " + t.getSQLName());
    }

    /**
     * Set the order of this row so that it will be just after/before <code>r</code>. NOTE: this may
     * reorder the table to make room.
     * 
     * @param r the row to be next to.
     * @param after whether this row will be before or after <code>r</code>.
     * @return this.
     */
    public SQLRowValues setOrder(SQLRow r, boolean after) {
        return this.setOrder(r, after, ReOrder.DISTANCE.movePointRight(2).intValue(), 0);
    }

    private SQLRowValues setOrder(SQLRow r, boolean after, int nbToReOrder, int nbReOrdered) {
        final BigDecimal freeOrder = r.getOrder(after);
        final String orderName = this.getTable().getOrderField().getName();
        if (freeOrder != null)
            return this.put(orderName, freeOrder);
        else if (nbReOrdered > r.getTable().getRowCount()) {
            throw new IllegalStateException("cannot reorder " + r.getTable().getSQLName());
        } else {
            // make room
            try {
                ReOrder.create(this.getTable(), r.getOrder().intValue() - (nbToReOrder / 2), nbToReOrder).exec();
            } catch (SQLException e) {
                throw ExceptionUtils.createExn(IllegalStateException.class, "reorder failed for " + this.getTable() + " at " + r.getOrder(), e);
            }
            r.fetchValues();
            return this.setOrder(r, after, nbToReOrder * 10, nbToReOrder);
        }
    }

    public final SQLRowValues setID(Number id) {
        // faster
        return this.setID(id, false);
    }

    /***
     * Set the {@link #getIDNumber() ID} of this row. Convert is useful to compare a row created in
     * Java and a row returned from the database, since in Java the ID will be an integer whereas
     * the DB can return anything.
     * 
     * @param id the new ID.
     * @param convert <code>true</code> if <code>id</code> should be converted to type of the
     *        primary key.
     * @return this.
     */
    public final SQLRowValues setID(Number id, final boolean convert) {
        final SQLField key = this.getTable().getKey();
        if (convert)
            id = NumberConvertor.convert(id, key.getType().getJavaType().asSubclass(Number.class));

        return this.put(key.getName(), id);
    }

    public final SQLRowValues setPrimaryKey(final SQLRowAccessor r) {
        if (r == null) {
            return this.putNulls(this.getTable().getPKsNames(), false);
        } else {
            checkSameTable(r, this.getTable());
            // required since we don't want only half of the fields of the primary key
            return this.loadAll(r.getAbsolutelyAll(), this.getTable().getPKsNames(new HashSet<String>()), true, FillMode.OVERWRITE);
        }
    }

    public final SQLRowValues setAll(Map<String, ?> m) {
        return this.loadAll(m, FillMode.CLEAR);
    }

    public final SQLRowValues putAll(Map<String, ?> m) {
        return this.putAll(m, null);
    }

    public final SQLRowValues putAll(Map<String, ?> m, final Collection<String> keys) {
        return this.loadAll(m, keys, false, FillMode.OVERWRITE);
    }

    static private enum FillMode {
        CLEAR, OVERWRITE, DONT_OVERWRITE
    }

    private final SQLRowValues loadAll(Map<String, ?> m, final FillMode fillMode) {
        return this.loadAll(m, null, false, fillMode);
    }

    private final SQLRowValues loadAll(Map<String, ?> m, final Collection<String> keys, final boolean required, final FillMode fillMode) {
        final Collection<String> keySet = keys == null ? m.keySet() : keys;
        if (!this.getTable().getFieldsName().containsAll(keySet)) {
            final List<String> l1 = new ArrayList<String>(keySet);
            final List<String> l2 = new ArrayList<String>(this.getTable().getFieldsName());
            Collections.sort(l1);
            Collections.sort(l2);
            throw new IllegalArgumentException("fields " + l1 + " are not a subset of " + this.getTable() + " : " + l2);
        }
        // copy before passing to fire()
        final Map<String, Object> toLoad = new LinkedHashMap<String, Object>(m);
        if (keys != null) {
            if (required && !m.keySet().containsAll(keys))
                throw new IllegalArgumentException("Not all are keys " + keys + " are in " + m);
            toLoad.keySet().retainAll(keys);
        }
        if (fillMode == FillMode.CLEAR) {
            clear();
        } else if (fillMode == FillMode.DONT_OVERWRITE) {
            toLoad.keySet().removeAll(this.getFields());
        }
        for (final Map.Entry<String, ?> e : toLoad.entrySet()) {
            // names are checked at the start
            this._put(e.getKey(), e.getValue(), false, ValueOperation.CONVERT);
        }
        // if there's no graph, there can't be any listeners
        final SQLRowValuesCluster graph = this.getGraph(false);
        if (graph != null)
            graph.fireModification(this, toLoad);
        return this;
    }

    public final SQLRowValues putNulls(String... fields) {
        return this.putNulls(Arrays.asList(fields));
    }

    public final SQLRowValues putNulls(Collection<String> fields) {
        return this.putNulls(fields, false);
    }

    /**
     * Set the passed fields to <code>null</code>.
     * 
     * @param fields which fields to put.
     * @param ignoreInexistant <code>true</code> if non existing field should be ignored,
     *        <code>false</code> will throw an exception if a field doesn't exist.
     * @return this.
     */
    public final SQLRowValues putNulls(Collection<String> fields, final boolean ignoreInexistant) {
        return this.fill(fields, null, ignoreInexistant, false);
    }

    /**
     * Put the same value in all the passed fields.
     * 
     * @param fields fields to change, <code>null</code> meaning all the fields of the table.
     * @param val the value to put, can be <code>null</code>.
     * @param ignoreInexistant if <code>fields</code> that aren't in the table should be ignored
     *        (not used if <code>fields</code> is <code>null</code>).
     * @param ignoreExisting <code>true</code> if no value should be overwritten.
     * @return this.
     * @throws IllegalArgumentException if <code>!ignoreInexistant</code> and some fields aren't in
     *         the table.
     */
    public final SQLRowValues fill(final Collection<String> fields, final Object val, final boolean ignoreInexistant, final boolean ignoreExisting) throws IllegalArgumentException {
        final Set<String> tableFieldsNames = getTable().getFieldsName();
        // keep order
        final Set<String> actualFields = fields == null ? tableFieldsNames : new LinkedHashSet<String>(fields);
        final Map<String, Object> m = createLinkedHashMap(actualFields.size());
        for (final String fn : actualFields) {
            if (fields == null || !ignoreInexistant || tableFieldsNames.contains(fn))
                m.put(fn, val);
        }
        return this.loadAll(m, ignoreExisting ? FillMode.DONT_OVERWRITE : FillMode.OVERWRITE);
    }

    /**
     * Fill all fields with the passed value.
     * 
     * @param val the value to put, can be <code>null</code>.
     * @param overwrite <code>true</code> if existing values must be replaced.
     * @return this.
     */
    public final SQLRowValues fillWith(final Object val, final boolean overwrite) {
        return this.fill(null, val, false, !overwrite);
    }

    /**
     * Set all the fields (including primary and foreign keys) of this row to <code>null</code>.
     * 
     * @return this.
     */
    public final SQLRowValues setAllToNull() {
        return this.fillWith(null, true);
    }

    // listener

    public class ReferentChangeEvent extends EventObject {

        private final SQLField f;
        private final SQLRowValues vals;
        private final boolean put;

        public ReferentChangeEvent(SQLField f, boolean put, SQLRowValues vals) {
            super(SQLRowValues.this);
            assert f != null && f.getDBSystemRoot().getGraph().getForeignTable(f) == getSource().getTable() && f.getTable() == vals.getTable();
            this.f = f;
            this.put = put;
            this.vals = vals;
        }

        // eg SITE[2]
        @Override
        public SQLRowValues getSource() {
            return (SQLRowValues) super.getSource();
        }

        // eg ID_SITE
        public final SQLField getField() {
            return this.f;
        }

        // eg BATIMENT[3]
        public final SQLRowValues getChangedReferent() {
            return this.vals;
        }

        // true if getChangedReferent() is a new referent of getSource(), false if it has been
        // removed from getSource()
        public final boolean isAddition() {
            return this.put;
        }

        public final boolean isRemoval() {
            return !this.isAddition();
        }

        @Override
        public String toString() {
            return this.getClass().getSimpleName() + (this.isAddition() ? " added" : " removed") + " on field " + getField() + " from " + this.getSource().asRow() + " : " + getChangedReferent();
        }
    }

    public static interface ReferentChangeListener extends EventListener {

        void referentChange(ReferentChangeEvent evt);

    }

    /**
     * Adds a listener to referent rows.
     * 
     * @param field the referent field to listen to, <code>null</code> meaning all.
     * @param l the listener.
     */
    public final void addReferentListener(SQLField field, ReferentChangeListener l) {
        if (this.referentsListener == null)
            this.referentsListener = new ListMap<SQLField, ReferentChangeListener>();
        this.referentsListener.add(field, l);
    }

    public final void removeReferentListener(SQLField field, ReferentChangeListener l) {
        if (this.referentsListener != null) {
            this.referentsListener.remove(field, l);
        }
    }

    private void fireRefChange(SQLField f, boolean put, SQLRowValues vals) {
        // only create event if needed
        if (this.referentsListener != null || this.getGraph().referentFireNeeded(put)) {
            final ReferentChangeEvent evt = new ReferentChangeEvent(f, put, vals);
            if (this.referentsListener != null) {
                for (final ReferentChangeListener l : this.referentsListener.getNonNull(f))
                    l.referentChange(evt);
                for (final ReferentChangeListener l : this.referentsListener.getNonNull(null))
                    l.referentChange(evt);
            }
            // no need to avoid creating graph, as this is called when the graph change
            assert this.graph != null;
            this.getGraph().fireModification(evt);
        }
    }

    public final void addValueListener(ValueChangeListener l) {
        this.getGraph().addValueListener(this, l);
    }

    public final void removeValueListener(ValueChangeListener l) {
        this.getGraph().removeValueListener(this, l);
    }

    @Override
    public final Collection<SQLRowValues> followLink(final Link l, final Direction direction) {
        return this.followPath(Path.get(getTable()).add(l, direction), CreateMode.CREATE_NONE, false);
    }

    /**
     * Create the necessary SQLRowValues so that the graph of this row goes along the passed path.
     * 
     * @param p the path of SQLRowValues, eg "LOCAL.ID_BATIMENT,BATIMENT.ID_SITE".
     * @return the SQLRowValues at the end of the path, eg a SQLRowValues on /SITE/.
     */
    public final SQLRowValues assurePath(final Path p) {
        return this.followPath(p, true);
    }

    /**
     * Return the row at the end of passed path.
     * 
     * @param p the path to follow, e.g. SITE,SITE.ID_CONTACT_CHEF.
     * @return the row at the end or <code>null</code> if none exists, e.g. SQLRowValues on
     *         /CONTACT/.
     */
    public final SQLRowValues followPath(final Path p) {
        return this.followPath(p, false);
    }

    private final SQLRowValues followPath(final Path p, final boolean create) {
        return followPathToOne(p, create ? CreateMode.CREATE_ONE : CreateMode.CREATE_NONE, DEFAULT_ALLOW_BACKTRACK);
    }

    /**
     * Follow path to at most one row.
     * 
     * @param p the path to follow.
     * @param create if and how to create new rows.
     * @param allowBackTrack <code>true</code> to allow encountering the same row more than once.
     * @return the destination row or <code>null</code> if none exists and <code>create</code> was
     *         {@link CreateMode#CREATE_NONE}
     * @see #followPath(Path, CreateMode, boolean, boolean)
     */
    public final SQLRowValues followPathToOne(final Path p, final CreateMode create, final boolean allowBackTrack) {
        final Collection<SQLRowValues> res = this.followPath(p, create, true, allowBackTrack);
        // since we passed onlyOne=true
        assert res.size() <= 1;
        return CollectionUtils.getSole(res);
    }

    /**
     * Return the rows at the end of the passed path.
     * 
     * @param path a path, e.g. SITE, BATIMENT, LOCAL.
     * @return the existing rows at the end of <code>path</code>, never <code>null</code>, e.g.
     *         [LOCAL[3], LOCAL[5]].
     */
    public final Collection<SQLRowValues> getDistantRows(final Path path) {
        return followPath(path, CreateMode.CREATE_NONE, false);
    }

    /**
     * Create all rows on the passed path and add them to this. There's {@link CreateMode#CREATE_ONE
     * one row} per step.
     * 
     * @param p the path.
     * @return the row at the end of the path.
     * @throws IllegalStateException if the first step is a non-empty foreign link.
     */
    public final SQLRowValues createPathToOne(final Path p) {
        final Collection<SQLRowValues> res = this.createPath(p, true);
        assert res.size() == 1;
        return res.iterator().next();
    }

    /**
     * Create all rows on the passed path and add them to this.
     * 
     * @param p the path.
     * @param createOne <code>true</code> to {@link CreateMode#CREATE_ONE create one} row per step,
     *        <code>false</code> to {@link CreateMode#CREATE_MANY create} one row per link.
     * @return the rows at the end of the path.
     * @throws IllegalStateException if the first step is a non-empty foreign link.
     */
    public final Collection<SQLRowValues> createPath(final Path p, final boolean createOne) {
        return this.followPath(p, createOne ? CreateMode.CREATE_ONE : CreateMode.CREATE_MANY, true, false, null);
    }

    public final Collection<SQLRowValues> followPath(final Path p, final CreateMode create, final boolean onlyOne) {
        return followPath(p, create, onlyOne, DEFAULT_ALLOW_BACKTRACK);
    }

    /**
     * Follow path through the graph.
     * 
     * @param p the path to follow.
     * @param create if and how to create new rows.
     * @param onlyOne <code>true</code> if this method should return at most one row.
     * @param allowBackTrack <code>true</code> to allow encountering the same row more than once.
     * @return the destination rows, can be empty.
     * @throws IllegalArgumentException if <code>p</code> doesn't start with this table.
     * @throws IllegalStateException if <code>onlyOne</code> and there's more than one row on the
     *         path.
     */
    public final Collection<SQLRowValues> followPath(final Path p, final CreateMode create, final boolean onlyOne, final boolean allowBackTrack)
            throws IllegalArgumentException, IllegalStateException {
        return followPath(p, create, false, onlyOne, allowBackTrack ? null : new LinkedIdentitySet<SQLRowValues>());
    }

    // if alwaysCreate : CREATE_NONE is invalid and existing rows are ignored (i.e. rows are always
    // created and an exception is thrown if there's a non-empty foreign link (perhaps add a force
    // mode to replace it))
    private final IdentitySet<SQLRowValues> followPath(final Path p, final CreateMode create, final boolean alwaysCreate, final boolean onlyOne, final IdentitySet<SQLRowValues> beenThere) {
        if (p.getFirst() != this.getTable())
            throw new IllegalArgumentException("path " + p + " doesn't start with us " + this);
        final boolean neverCreate = create == CreateMode.CREATE_NONE;
        if (alwaysCreate && neverCreate)
            throw new IllegalArgumentException("If alwaysCreate, don't pass " + create);
        if (alwaysCreate && beenThere != null)
            throw new IllegalArgumentException("If alwaysCreate, existing rows are ignored so the same row can never be visited more than once");
        if (p.length() > 0) {
            // fail-fast : avoid creating rows
            if (onlyOne && create == CreateMode.CREATE_MANY && !p.isSingleLink())
                throw new IllegalStateException("more than one link with " + create + " and onlyOne : " + p);

            final Step firstStep = p.getStep(0);
            final Set<Link> ffs = firstStep.getLinks();
            final SetMap<Link, SQLRowValues> existingRows = createSetMap(-1, 6);
            final Set<Link> linksToCreate = neverCreate ? Collections.<Link> emptySet() : new HashSet<Link>();
            for (final Link l : ffs) {
                final SQLField ff = l.getLabel();
                if (firstStep.isForeign(l)) {
                    final Object fkValue = this.getObject(ff.getName());
                    if (fkValue instanceof SQLRowValues && (beenThere == null || !beenThere.contains(fkValue))) {
                        if (alwaysCreate)
                            throw new IllegalStateException("alwaysCreate=true but foreign link is not empty : " + l);
                        existingRows.add(l, (SQLRowValues) fkValue);
                    } else if (!neverCreate) {
                        linksToCreate.add(l);
                    }
                } else {
                    final Set<SQLRowValues> referentRows = this.getReferentRows(ff);
                    final Set<SQLRowValues> validReferentRows;
                    if (beenThere == null || beenThere.size() == 0) {
                        validReferentRows = referentRows;
                    } else {
                        validReferentRows = new LinkedIdentitySet<SQLRowValues>(referentRows);
                        validReferentRows.removeAll(beenThere);
                    }
                    final boolean hasRef = validReferentRows.size() > 0;
                    if (hasRef) {
                        existingRows.addAll(l, validReferentRows);
                    }
                    if (alwaysCreate || !neverCreate && !hasRef) {
                        linksToCreate.add(l);
                    }
                }
            }
            assert !alwaysCreate || linksToCreate.size() > 0;
            // Set is needed when a row is multi-linked to another (to avoid calling recursively
            // followPath() on the same instance)
            // IdentitySet is needed since multiple rows can be equal, e.g. empty rows :
            // SITE -- chef -> CONTACT
            // _____-- rapport -> CONTACT
            final Set<SQLRowValues> next = new LinkedIdentitySet<SQLRowValues>();
            // by definition alwaysCreate implies ignoring existing rows
            if (!alwaysCreate)
                next.addAll(existingRows.allValues());
            final int existingCount = next.size();
            if (onlyOne && existingCount > 1)
                throw new IllegalStateException("more than one row exist and onlyOne=true : " + existingRows);

            final int newCount;
            if (create == CreateMode.CREATE_MANY) {
                newCount = existingCount + linksToCreate.size();
            } else if (create == CreateMode.CREATE_ONE) {
                // only enforce if we're creating rows, otherwise use "onlyOne"
                if (linksToCreate.size() > 0 && existingCount > 1)
                    throw new IllegalStateException("more than one row exist and " + create + ", this step won't be between two rows : " + existingRows);
                newCount = Math.max(existingCount, 1);
            } else {
                assert neverCreate;
                newCount = existingCount;
            }
            if (onlyOne && newCount > 1)
                throw new IllegalStateException("Will have more than one row and onlyOne=true : " + existingRows + " to create : " + linksToCreate);

            for (final Link l : linksToCreate) {
                final SQLField ff = l.getLabel();
                final boolean isForeign = firstStep.isForeign(l);

                final SQLRowValues nextOne;
                if (create == CreateMode.CREATE_ONE && next.size() == 1) {
                    nextOne = next.iterator().next();
                } else {
                    assert create == CreateMode.CREATE_MANY || (create == CreateMode.CREATE_ONE && next.size() == 0) : "Creating more than one, already " + next.size();
                    nextOne = new SQLRowValues(firstStep.getTo());
                    if (isForeign) {
                        // keep the id, if present
                        final Object fkValue = this.getObject(ff.getName());
                        if (fkValue instanceof Number)
                            nextOne.setID((Number) fkValue);
                    }
                    next.add(nextOne);
                }
                if (isForeign) {
                    this.put(ff.getName(), nextOne);
                } else {
                    nextOne.put(ff.getName(), this);
                }
            }
            // already checked above
            assert !(onlyOne && next.size() > 1);

            // see comment above for IdentitySet
            final IdentitySet<SQLRowValues> res = new LinkedIdentitySet<SQLRowValues>();
            for (final SQLRowValues n : next) {
                final IdentitySet<SQLRowValues> newBeenThere;
                if (beenThere == null) {
                    newBeenThere = null;
                } else {
                    newBeenThere = new LinkedIdentitySet<SQLRowValues>(beenThere);
                    final boolean added = newBeenThere.add(this);
                    assert added;
                }
                res.addAll(n.followPath(p.minusFirst(), create, alwaysCreate, onlyOne, newBeenThere));
            }
            return res;
        } else {
            return CollectionUtils.createIdentitySet(this);
        }
    }

    public final SQLRowValues changeForeigns(ForeignCopyMode mode) {
        return this.changeForeigns(null, false, mode);
    }

    static private final boolean isEmpty(final Collection<?> coll, final boolean exclude) {
        if (exclude) {
            return coll == null;
        } else {
            return coll != null && coll.isEmpty();
        }
    }

    public final SQLRowValues changeForeigns(final Collection<String> fields, final boolean exclude, final ForeignCopyMode mode) {
        if (!isEmpty(fields, exclude) && mode != ForeignCopyMode.COPY_ROW) {
            // copy otherwise ConcurrentModificationException
            for (final String ff : new ArrayList<String>(this.getForeigns().keySet())) {
                // fields == null means include all thanks to the above if
                if (fields == null || fields.contains(ff) != exclude) {
                    this.flatten(ff, mode);
                }
            }
        }
        return this;
    }

    /**
     * Flatten a foreign row values. NOTE : if there's no foreign row in <code>ff</code>, this
     * method does nothing.
     * 
     * @param ff a foreign field.
     * @param mode how to flatten.
     * @return this.
     */
    public final SQLRowValues flatten(final String ff, final ForeignCopyMode mode) {
        if (mode != ForeignCopyMode.COPY_ROW) {
            final SQLRowValues foreign = this.foreigns.get(ff);
            if (foreign != null) {
                if (mode == ForeignCopyMode.COPY_NULL) {
                    this.put(ff, null);
                } else if (mode == ForeignCopyMode.NO_COPY) {
                    this.remove(ff);
                } else if (foreign.hasID()) {
                    assert mode == ForeignCopyMode.COPY_ID_OR_ROW || mode == ForeignCopyMode.COPY_ID_OR_RM;
                    this.put(ff, foreign.getIDNumber());
                } else if (mode == ForeignCopyMode.COPY_ID_OR_RM) {
                    this.remove(ff);
                } else {
                    assert mode == ForeignCopyMode.COPY_ID_OR_ROW && !foreign.hasID();
                }
            }
        }
        return this;
    }

    // *** load

    public void loadAbsolutelyAll(SQLRow row) {
        this.setAll(row.getAbsolutelyAll());
    }

    /**
     * Load values from the passed row (and remove them if possible).
     * 
     * @param row the row to load values from.
     * @param fieldsNames what fields to load, <code>null</code> meaning all.
     */
    public void load(SQLRowAccessor row, final Collection<String> fieldsNames) {
        // make sure we only define keys that row has
        // allow load( {'A':a, 'B':b}, {'A', 'B', 'C' } ) to not define 'C' to null
        final Map<String, Object> m = new LinkedHashMap<String, Object>(row.getAbsolutelyAll());
        if (fieldsNames != null)
            m.keySet().retainAll(fieldsNames);

        // rm the added fields otherwise this and row will be linked
        // eg load LOCAL->BATIMENT into a LOCAL will result in the BATIMENT
        // being pointed to by both LOCAL
        if (row instanceof SQLRowValues)
            ((SQLRowValues) row).removeAll(m.keySet());

        // put after remove so that this graph never contains row (and thus avoids unneeded events)
        this.putAll(m);
    }

    // *** modify

    void checkValidity() {
        // this checks archived which the DB doesn't with just foreign constraints
        // it also locks foreign rows so that they don't *become* archived
        final Object[] pb = this.getInvalid();
        if (pb != null)
            throw new IllegalStateException("can't update " + this + " : the field " + pb[0] + " points to " + pb[1]);
    }

    /**
     * Renvoie le premier pb dans les valeurs. C'est à dire la première clef externe qui pointe sur
     * une ligne non valide.
     * 
     * @return <code>null</code> si pas de pb, sinon un Object[] :
     *         <ol>
     *         <li>en 0 le nom du champ posant pb, eg "ID_OBSERVATION_2"</li>
     *         <li>en 1 une SQLRow décrivant le pb, eg "(OBSERVATION[123])"</li>
     *         </ol>
     */
    public Object[] getInvalid() {
        final Map<String, Link> foreignLinks = new HashMap<String, Link>();
        for (final Link foreignLink : this.getTable().getForeignLinks()) {
            for (final String f : foreignLink.getCols()) {
                foreignLinks.put(f, foreignLink);
            }
        }

        for (final String fieldName : this.values.keySet()) {
            final Link foreignLink = foreignLinks.remove(fieldName);
            if (foreignLink != null) {
                final SQLTable foreignTable = foreignLink.getTarget();
                if (foreignTable.isRowable()) {
                    // otherwise would have to check more than field
                    assert foreignLink.getCols().size() == 1;
                    // verifie l'intégrité (a rowValues is obviously correct, as is EMPTY,
                    // DEFAULT is the responsability of the DB)
                    final Object fieldVal = this.getObject(fieldName);
                    if (fieldVal != null && fieldVal != SQL_DEFAULT && !(fieldVal instanceof SQLRowValues)) {
                        final SQLRow pb = foreignTable.checkValidity(((Number) fieldVal).intValue());
                        if (pb != null)
                            return new Object[] { fieldName, pb };
                    }
                } else {
                    // check that the foreign key is complete
                    for (final String ff : foreignLink.getCols()) {
                        if (!this.getFields().contains(ff))
                            return new Object[] { ff, null };
                    }
                    // MAYBE also check foreign row is valid
                }
            } // else not a foreign key or already checked
        }
        return null;
    }

    // * insert

    /**
     * Insert a new line (strips the primary key, it must be db generated and strips order, added at
     * the end).
     * 
     * @return the newly inserted line, or <code>null</code> if the table has not exactly one
     *         primary key.
     * @throws SQLException if an error occurs while inserting.
     * @throws IllegalStateException if the ID of the new line cannot be retrieved.
     */
    public SQLRow insert() throws SQLException {
        // remove unwanted fields, keep ARCHIVE
        return this.store(SQLRowValuesCluster.StoreMode.INSERT);
    }

    /**
     * Insert a new line verbatim. ATTN the primary key must not exist.
     * 
     * @return the newly inserted line, or <code>null</code> if the table has not exactly one
     *         primary key.
     * @throws SQLException if an error occurs while inserting.
     * @throws IllegalStateException if the ID of the new line cannot be retrieved.
     */
    public SQLRow insertVerbatim() throws SQLException {
        return this.store(SQLRowValuesCluster.StoreMode.INSERT_VERBATIM);
    }

    public SQLRow insert(final boolean insertPK, final boolean insertOrder) throws SQLException {
        return this.store(new SQLRowValuesCluster.Insert(insertPK, insertOrder));
    }

    public SQLRow store(final SQLRowValuesCluster.StoreMode mode) throws SQLException {
        return this.getGraph().store(mode).getStoredRow(this);
    }

    SQLTableEvent insertJustThis(final boolean fetchStoredRow, final Set<SQLField> autoFields) throws SQLException {
        final Map<String, Object> copy = this.clearFields(new HashMap<String, Object>(this.values), autoFields);

        try {
            final Tuple2<List<String>, Number> fieldsAndID = this.getTable().getBase().getDataSource().useConnection(new ConnectionHandlerNoSetup<Tuple2<List<String>, Number>, SQLException>() {
                @Override
                public Tuple2<List<String>, Number> handle(SQLDataSource ds) throws SQLException {
                    final Tuple2<PreparedStatement, List<String>> pStmt = createInsertStatement(getTable(), copy);
                    try {
                        final Number newID = insert(pStmt.get0(), getTable());
                        // MAYBE keep the pStmt around while values.keySet() doesn't change
                        pStmt.get0().close();
                        return Tuple2.create(pStmt.get1(), newID);
                    } catch (Exception e) {
                        throw new SQLException("Unable to insert " + pStmt.get0(), e);
                    }
                }
            });

            assert this.getTable().isRowable() == (fieldsAndID.get1() != null);
            if (this.getTable().isRowable()) {
                // pour pouvoir avoir les valeurs des champs non précisés
                return new SQLTableEvent(getEventRow(fieldsAndID.get1().intValue(), fetchStoredRow), Mode.ROW_ADDED, fieldsAndID.get0());
            } else
                return new SQLTableEvent(getTable(), SQLRow.NONEXISTANT_ID, Mode.ROW_ADDED, fieldsAndID.get0());
        } catch (SQLException e) {
            throw new SQLException("unable to insert " + this + " using " + copy, e);
        }
    }

    private SQLRow getEventRow(final int newID, final boolean fetch) {
        final SQLRow res;
        if (fetch) {
            // don't read the cache since no event has been fired yet
            // don't write to it since the transaction isn't committed yet, so other threads
            // should not see the new values.
            res = new SQLRow(getTable(), newID).fetchValues(false);
        } else {
            res = SQLRow.createEmpty(getTable(), newID);
        }
        assert res.isFilled();
        return res;
    }

    // * update

    public SQLRow update() throws SQLException {
        if (!hasID()) {
            throw new IllegalStateException("can't update : no ID specified, use update(int) or set ID for " + this);
        }
        return this.commit();
    }

    public SQLRow update(final int id) throws SQLException {
        this.put(this.getTable().getKey().getName(), id);
        return this.commit();
    }

    /**
     * Permet de mettre à jour une ligne existante avec les valeurs courantes.
     * 
     * @param fetchStoredRow <code>true</code> to fetch the just stored row.
     * @param id l'id à mettre à jour.
     * @return the updated row.
     * @throws SQLException si pb lors de la maj.
     */
    SQLTableEvent updateJustThis(boolean fetchStoredRow, final int id) throws SQLException {
        if (id == this.getTable().getUndefinedID()) {
            throw new IllegalArgumentException("can't update undefined with " + this);
        }
        // clear primary key, otherwise we might end up with :
        // UPDATE TABLE SET ID=123,DESIGNATION='aa' WHERE id=456
        // which will delete ID 456, and possibly cause a conflict with preexisting ID 123
        final Map<String, Object> updatedValues = this.clearPrimaryKeys(new HashMap<String, Object>(this.values));

        final List<String> updatedCols;
        if (updatedValues.isEmpty()) {
            updatedCols = Collections.emptyList();
        } else {
            updatedCols = this.getTable().getDBSystemRoot().getDataSource().useConnection(new ConnectionHandlerNoSetup<List<String>, SQLException>() {
                @Override
                public List<String> handle(SQLDataSource ds) throws SQLException {
                    final Tuple2<PreparedStatement, List<String>> pStmt = createUpdateStatement(getTable(), updatedValues, id);
                    final long timeMs = System.currentTimeMillis();
                    final long time = System.nanoTime();
                    final int updateCount = pStmt.get0().executeUpdate();
                    final long afterExecute = System.nanoTime();
                    // logging after closing fails to get the connection info
                    SQLRequestLog.log(pStmt.get0(), "rowValues.update()", timeMs, time, afterExecute, afterExecute, afterExecute, afterExecute, System.nanoTime());
                    pStmt.get0().close();
                    if (updateCount > 1)
                        throw new IllegalStateException(updateCount + " rows updated with ID " + id);
                    return updateCount == 0 ? null : pStmt.get1();
                }
            });
        }

        return updatedCols == null ? null : new SQLTableEvent(getEventRow(id, fetchStoredRow), Mode.ROW_UPDATED, updatedCols);
    }

    // * commit

    /**
     * S'assure que ces valeurs arrivent dans la base. Si la ligne possède un ID équivaut à update()
     * sinon insert().
     * 
     * @return the affected row.
     * @throws SQLException
     */
    public SQLRow commit() throws SQLException {
        return this.store(SQLRowValuesCluster.StoreMode.COMMIT);
    }

    SQLTableEvent commitJustThis(boolean fetchStoredRow) throws SQLException {
        if (!hasID()) {
            return this.insertJustThis(fetchStoredRow, Collections.<SQLField> emptySet());
        } else
            return this.updateJustThis(fetchStoredRow, this.getID());
    }

    /**
     * Returns a string representation of this (excluding any foreign or referent rows).
     * 
     * @return a compact representation of this.
     * @see #printGraph()
     */
    @Override
    public String toString() {
        String result = this.getClass().getSimpleName() + " on " + this.getTable() + " : {";
        result += CollectionUtils.join(this.values.entrySet(), ", ", new ITransformer<Entry<String, ?>, String>() {
            public String transformChecked(final Entry<String, ?> e) {
                final String className = e.getValue() == null ? "" : "(" + e.getValue().getClass() + ")";
                final String value;
                // avoid infinite loop (and overly verbose string)
                if (e.getValue() instanceof SQLRowValues) {
                    final SQLRowValues foreignVals = (SQLRowValues) e.getValue();
                    if (foreignVals == SQLRowValues.this) {
                        value = "this";
                    } else if (foreignVals.hasID()) {
                        value = foreignVals.getIDNumber().toString();
                    } else {
                        // so that if the same vals is referenced multiple times, we can see it
                        value = "@" + System.identityHashCode(foreignVals);
                    }
                } else
                    value = String.valueOf(e.getValue());
                return e.getKey() + "=" + value + className;
            }
        });
        result += "}";
        return result;
    }

    /**
     * Return a graphical representation (akin to the result of a query) of the tree rooted at
     * <code>this</code>.
     * 
     * @return a string representing the rows pointing to this.
     * @see SQLRowValuesCluster#printTree(SQLRowValues, int)
     */
    public final String printTree() {
        return this.getGraph().printTree(this, 16);
    }

    /**
     * Return the list of all nodes and their links.
     * 
     * @return a string representing the graph of this.
     */
    public final String printGraph() {
        return this.getGraph().printNodes();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj instanceof SQLRowValues) {
            return this.equalsGraph((SQLRowValues) obj);
        } else
            return false;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + this.getTable().hashCode();
        // don't use SQLRowValues to avoid infinite loop
        result = prime * result + this.getFields().hashCode();
        result = prime * result + this.getGraphSize();
        result = prime * result + this.foreigns.keySet().hashCode();
        result = prime * result + this.referents.keySet().hashCode();
        return result;
    }

    /**
     * Indicates whether some other graph is "equal to" this one.
     * 
     * @param other another rowValues.
     * @return <code>true</code> if both graph are equals.
     * @see #getGraphFirstDifference(SQLRowValues)
     */
    public final boolean equalsGraph(final SQLRowValues other) {
        return this.getGraphFirstDifference(other) == null;
    }

    /**
     * Return the first difference between this graph and another, ignoring order of fields.
     * 
     * @param other another instance.
     * @return the first difference, <code>null</code> if equals.
     */
    public final String getGraphFirstDifference(final SQLRowValues other) {
        return this.getGraphFirstDifference(other, false);
    }

    /**
     * Return the first difference between this graph and another. Most of the time fields orders
     * need not to be used, since when inserting they don't matter (which isn't true of the
     * referents). But they can matter if e.g. this is used to construct a query.
     * 
     * @param other another instance.
     * @param useOrder <code>true</code> to also compare the order of fields.
     * @return the first difference, <code>null</code> if equals.
     */
    public final String getGraphFirstDifference(final SQLRowValues other, final boolean useOrder) {
        if (this == other)
            return null;
        return this.getGraph().getFirstDifference(this, other, useOrder, useOrder, true).getFirstDifference();
    }

    public final boolean equalsJustThis(final SQLRowValues o) {
        // don't compare the order of fields, since inserting doesn't change with it
        return this.equalsJustThis(o, false);
    }

    /**
     * Whether this equals the passed instance without following linked rows. This method use
     * {@link ForeignCopyMode#COPY_ID_OR_RM}, so that a row having a foreign ID and a row having a
     * foreign row with the same ID are equal.
     * 
     * @param o another instance.
     * @param useFieldsOrder <code>true</code> if the order of {@link #getFields()} is to be
     *        checked.
     * @return <code>true</code> if both rows have the same {@link #getFields() fields} defined, and
     *         {@link #getAllValues(ForeignCopyMode) all values} of this are equal to all values of
     *         <code>o</code>.
     * @see #equalsGraph(SQLRowValues)
     */
    public final boolean equalsJustThis(final SQLRowValues o, final boolean useFieldsOrder) {
        return this.equalsJustThis(o, useFieldsOrder, true);
    }

    final boolean equalsJustThis(final SQLRowValues o, final boolean useFieldsOrder, final boolean useForeignID) {
        return this.equalsJustThis(o, useFieldsOrder, useForeignID, true);
    }

    final boolean equalsJustThis(final SQLRowValues o, final boolean useFieldsOrder, final boolean useForeignID, final boolean usePK) {
        if (this == o)
            return true;
        if (!this.getTable().equals(o.getTable()))
            return false;
        // first compare keySet as ForeignCopyMode can remove entries
        if (useFieldsOrder) {
            if (!CompareUtils.equalsUsingIterator(this.values.keySet(), o.values.keySet()))
                return false;
        } else {
            if (!this.values.keySet().equals(o.values.keySet()))
                return false;
        }
        // fields are already checked so if IDs are not wanted, just omit foreign rows
        final ForeignCopyMode copyMode = useForeignID ? ForeignCopyMode.COPY_ID_OR_RM : ForeignCopyMode.NO_COPY;
        final Map<String, Object> thisVals = this.getAllValues(copyMode, !usePK);
        final Map<String, Object> oVals = o.getAllValues(copyMode, !usePK);
        if (!usePK) {
            final List<String> pk = this.getTable().getPKsNames();
            thisVals.keySet().removeAll(pk);
            oVals.keySet().removeAll(pk);
        }
        // LinkedHashMap.equals() does not compare the order of entries
        return thisVals.equals(oVals);
    }

    // *** static

    static private Tuple2<PreparedStatement, List<String>> createInsertStatement(final SQLTable table, Map<String, Object> values) throws SQLException {
        final Tuple2<List<String>, List<Object>> l = CollectionUtils.mapToLists(values);
        final List<String> fieldsNames = l.get0();
        final List<Object> vals = l.get1();

        addMetadata(fieldsNames, vals, table.getCreationUserField(), getUser());
        addMetadata(fieldsNames, vals, table.getCreationDateField(), new Timestamp(System.currentTimeMillis()));

        return createStatement(table, fieldsNames, vals, true);
    }

    static private Tuple2<PreparedStatement, List<String>> createUpdateStatement(SQLTable table, Map<String, Object> values, int id) throws SQLException {
        final Tuple2<List<String>, List<Object>> l = CollectionUtils.mapToLists(values);
        final List<String> fieldsNames = l.get0();
        final List<Object> vals = l.get1();

        vals.add(new Integer(id));
        return createStatement(table, fieldsNames, vals, false);
    }

    static private void addMetadata(List<String> fieldsNames, List<Object> values, SQLField field, Object fieldValue) throws SQLException {
        if (field != null) {
            // TODO updateVerbatim to force a value
            final int index = fieldsNames.indexOf(field.getName());
            if (index < 0) {
                // ajout au dbt car le where du UPDATE a besoin de l'ID en dernier
                fieldsNames.add(0, field.getName());
                values.add(0, fieldValue);
            } else {
                values.set(index, fieldValue);
            }
        }
    }

    static private Object getUser() {
        final int userID = UserManager.getUserID();
        return userID < SQLRow.MIN_VALID_ID ? SQL_DEFAULT : userID;
    }

    /**
     * Create a prepared statement.
     * 
     * @param table the table to change.
     * @param fieldsNames the columns names of <code>table</code>.
     * @param values their values.
     * @param insert whether to insert or update.
     * @return the new statement and its columns.
     * @throws SQLException if an error occurs.
     */
    static private Tuple2<PreparedStatement, List<String>> createStatement(SQLTable table, List<String> fieldsNames, List<Object> values, boolean insert) throws SQLException {
        addMetadata(fieldsNames, values, table.getModifUserField(), getUser());
        addMetadata(fieldsNames, values, table.getModifDateField(), new Timestamp(System.currentTimeMillis()));

        final PreparedStatement pStmt;
        final String tableQuoted = table.getSQLName().quote();
        String req = (insert ? "INSERT INTO " : "UPDATE ") + tableQuoted + " ";
        if (insert) {
            assert fieldsNames.size() == values.size();
            // remove DEFAULT since they are useless and prevent us from using
            // INSERT INTO "TABLEAU_ELECTRIQUE" ("ID_OBSERVATION", ...) select DEFAULT, ?,
            // MAX("ORDRE") + 1 FROM "TABLEAU_ELECTRIQUE"
            for (int i = values.size() - 1; i >= 0; i--) {
                if (values.get(i) == SQL_DEFAULT) {
                    fieldsNames.remove(i);
                    values.remove(i);
                }
            }
            assert fieldsNames.size() == values.size();

            // ajout de l'ordre
            final SQLField order = table.getOrderField();
            final boolean selectOrder;
            if (order != null && !fieldsNames.contains(order.getName())) {
                // si l'ordre n'est pas spécifié, ajout à la fin
                fieldsNames.add(order.getName());
                selectOrder = true;
            } else {
                selectOrder = false;
            }

            if (fieldsNames.size() == 0 && table.getServer().getSQLSystem() != SQLSystem.MYSQL) {
                // "LOCAL" () VALUES () is a syntax error on PG
                req += "DEFAULT VALUES";
            } else {
                req += "(" + CollectionUtils.join(fieldsNames, ", ", new ITransformer<String, String>() {
                    public String transformChecked(String input) {
                        return SQLBase.quoteIdentifier(input);
                    }
                }) + ")";
                // no DEFAULT thus only ?
                final String questionMarks = CollectionUtils.join(Collections.nCopies(values.size(), "?"), ", ");
                if (selectOrder) {
                    // needed since VALUES ( (select MAX("ORDRE") from "LOCAL") ) on MySQL yield
                    // "You can't specify target table 'LOCAL' for update in FROM clause"
                    req += " select ";
                    req += questionMarks;
                    if (values.size() > 0)
                        req += ", ";
                    // COALESCE for empty tables, MIN_ORDER + 1 since MIN_ORDER cannot be moved
                    req += "COALESCE(MAX(" + SQLBase.quoteIdentifier(order.getName()) + "), " + ReOrder.MIN_ORDER + ") + 1 FROM " + tableQuoted;
                } else {
                    req += " VALUES (";
                    req += questionMarks;
                    req += ")";
                }
            }
            pStmt = createInsertStatement(req, table);
        } else {
            // ID at the end
            assert fieldsNames.size() == values.size() - 1;
            final List<String> fieldAndValues = new ArrayList<String>(fieldsNames.size());
            final ListIterator<String> iter = fieldsNames.listIterator();
            while (iter.hasNext()) {
                final String fieldName = iter.next();
                final SQLField field = table.getField(fieldName);
                final Object value = values.get(iter.previousIndex());
                // postgresql doesn't support prefixing fields with their tables in an update
                fieldAndValues.add(SQLBase.quoteIdentifier(field.getName()) + "= " + getFieldValue(value));
            }

            req += "SET " + CollectionUtils.join(fieldAndValues, ", ");
            req += " WHERE " + table.getKey().getFieldRef() + "= ?";
            final Connection c = table.getBase().getDataSource().getConnection();
            pStmt = c.prepareStatement(req);
        }
        // set fields values
        int i = 0;
        for (final Object value : values) {
            // nothing to set if there's no corresponding '?'
            if (value != SQL_DEFAULT) {
                final Object toIns;
                if (value instanceof SQLRowValues) {
                    // TODO if we already point to some row, archive it
                    toIns = ((SQLRowValues) value).insert().getIDNumber();
                } else
                    toIns = value;
                // sql index start at 1
                pStmt.setObject(i + 1, toIns);
                i++;
            }
        }
        return Tuple2.create(pStmt, fieldsNames);
    }

    private static String getFieldValue(final Object value) {
        return value == SQL_DEFAULT ? "DEFAULT" : "?";
    }

    @Override
    public SQLTableModifiedListener createTableListener(SQLDataListener l) {
        return new SQLTableListenerData<SQLRowValues>(this, l);
    }

    // *** static

    /**
     * Create an insert statement which can provide the inserted ID.
     * 
     * @param req the INSERT sql.
     * @param table the table where the row will be inserted.
     * @return a new <code>PreparedStatement</code> object, containing the pre-compiled SQL
     *         statement, that will have the capability of returning the primary key.
     * @throws SQLException if a database access error occurs.
     * @see #insert(PreparedStatement, SQLTable)
     */
    static public final PreparedStatement createInsertStatement(String req, final SQLTable table) throws SQLException {
        final boolean rowable = table.isRowable();
        final boolean isPG = table.getServer().getSQLSystem() == SQLSystem.POSTGRESQL;
        if (rowable && isPG)
            req += " RETURNING " + SQLBase.quoteIdentifier(table.getKey().getName());
        final Connection c = table.getDBSystemRoot().getDataSource().getConnection();
        final int returnGenK = rowable && !isPG && c.getMetaData().supportsGetGeneratedKeys() ? Statement.RETURN_GENERATED_KEYS : Statement.NO_GENERATED_KEYS;
        return c.prepareStatement(req, returnGenK);
    }

    /**
     * Execute the passed INSERT statement and return the ID of the new row.
     * 
     * @param pStmt an INSERT statement (should have been obtained using
     *        {@link #createInsertStatement(String, SQLTable)}).
     * @param table the table where the row will be inserted.
     * @return the new ID.
     * @throws SQLException if the insertion fails.
     */
    static public final Number insert(final PreparedStatement pStmt, final SQLTable table) throws SQLException {
        final long timeMs = System.currentTimeMillis();

        final long time = System.nanoTime();
        pStmt.execute();
        final long afterExecute = System.nanoTime();

        final Number newID;
        if (table.isRowable()) {
            final ResultSet rs;
            if (table.getServer().getSQLSystem() == SQLSystem.POSTGRESQL) {
                // uses RETURNING
                rs = pStmt.getResultSet();
            } else {
                rs = pStmt.getGeneratedKeys();
            }
            try {
                if (rs.next()) {
                    newID = (Number) rs.getObject(1);
                } else
                    throw new IllegalStateException("no keys have been autogenerated for the successfully executed statement :" + pStmt);
            } catch (SQLException exn) {
                throw new IllegalStateException("can't get autogenerated keys for the successfully executed statement :" + pStmt);
            }
        } else {
            newID = null;
        }
        final long afterHandle = System.nanoTime();
        SQLRequestLog.log(pStmt, "rowValues.insert()", timeMs, time, afterExecute, afterExecute, afterExecute, afterHandle, System.nanoTime());

        return newID;
    }

    /**
     * Insert rows in the passed table.
     * 
     * @param t a table, eg /LOCAL/.
     * @param sql the sql specifying the data to be inserted, eg ("DESIGNATION") VALUES('A'), ('B').
     * @return the inserted IDs, or <code>null</code> if <code>t</code> is not
     *         {@link SQLTable#isRowable() rowable}.
     * @throws SQLException if an error occurs while inserting.
     */
    @SuppressWarnings("unchecked")
    public static final List<Number> insertIDs(final SQLTable t, final String sql) throws SQLException {
        final boolean rowable = t.isRowable();
        final Insertion<?> res = insert(t, sql, rowable ? ReturnMode.FIRST_FIELD : ReturnMode.NO_FIELDS);
        if (rowable)
            return ((Insertion<Number>) res).getRows();
        else
            return null;
    }

    /**
     * Insert rows in the passed table.
     * 
     * @param t a table, eg /LOCAL/.
     * @param sql the sql specifying the data to be inserted, eg ("DESIGNATION") VALUES('A'), ('B').
     * @return an object to always know the insertion count and possibly the inserted primary keys.
     * @throws SQLException if an error occurs while inserting.
     */
    @SuppressWarnings("unchecked")
    public static final Insertion<List<Object>> insert(final SQLTable t, final String sql) throws SQLException {
        return (Insertion<List<Object>>) insert(t, sql, ReturnMode.ALL_FIELDS);
    }

    /**
     * Insert rows in the passed table. Should be faster than other insert methods since it doesn't
     * fetch primary keys.
     * 
     * @param t a table, eg /LOCAL/.
     * @param sql the sql specifying the data to be inserted, eg ("DESIGNATION") VALUES('A'), ('B').
     * @return the insertion count.
     * @throws SQLException if an error occurs while inserting.
     */
    public static final int insertCount(final SQLTable t, final String sql) throws SQLException {
        return insert(t, sql, ReturnMode.NO_FIELDS).getCount();
    }

    // if scalar is null primary keys aren't fetched
    private static final Insertion<?> insert(final SQLTable t, final String sql, final ReturnMode mode) throws SQLException {
        return new Inserter(t).insert(sql, mode, true);
    }

    /**
     * Insert rows in the passed table.
     * 
     * @param t a table, eg /LOCAL/.
     * @param sql the sql specifying the data to be inserted, eg ("DESIGNATION") VALUES('A'), ('B').
     * @return the inserted rows (with no values, ie a call to a getter will trigger a db access),
     *         or <code>null</code> if <code>t</code> is not {@link SQLTable#isRowable() rowable}.
     * @throws SQLException if an error occurs while inserting.
     */
    public static final List<SQLRow> insertRows(final SQLTable t, final String sql) throws SQLException {
        final List<Number> ids = insertIDs(t, sql);
        if (ids == null)
            return null;
        final List<SQLRow> res = new ArrayList<SQLRow>(ids.size());
        for (final Number id : ids)
            res.add(new SQLRow(t, id.intValue()));
        return res;
    }

    // MAYBE add insertFromSelect(SQLTable, SQLSelect) if aliases are kept in SQLSelect (so that we
    // can map arbitray expressions to fields in the destination table)
    public static final int insertFromTable(final SQLTable dest, final SQLTable src) throws SQLException {
        return insertFromTable(dest, src, src.getChildrenNames());
    }

    /**
     * Copy all rows from <code>src</code> to <code>dest</code>.
     * 
     * @param dest the table where rows will be inserted.
     * @param src the table where rows will be selected.
     * @param fieldsNames the fields to use.
     * @return the insertion count.
     * @throws SQLException if an error occurs while inserting.
     */
    public static final int insertFromTable(final SQLTable dest, final SQLTable src, final Set<String> fieldsNames) throws SQLException {
        if (dest.getDBSystemRoot() != src.getDBSystemRoot())
            throw new IllegalArgumentException("Tables are not on the same system root : " + dest.getSQLName() + " / " + src.getSQLName());
        if (!dest.getChildrenNames().containsAll(fieldsNames))
            throw new IllegalArgumentException("Destination table " + dest.getSQLName() + " doesn't contain all fields of the source " + src + " : " + fieldsNames);

        final List<SQLField> fields = new ArrayList<SQLField>(fieldsNames.size());
        for (final String fName : fieldsNames)
            fields.add(src.getField(fName));
        final SQLSelect sel = new SQLSelect(true);
        sel.addAllSelect(fields);
        final String colNames = "(" + CollectionUtils.join(fields, ",", new ITransformer<SQLField, String>() {
            @Override
            public String transformChecked(SQLField input) {
                return SQLBase.quoteIdentifier(input.getName());
            }
        }) + ") ";
        return insertCount(dest, colNames + sel.asString());
    }

    /**
     * Trim a collection of SQLRowValues.
     * 
     * @param graphs the rowValues to trim.
     * @return a copy of <code>graphs</code> without any linked SQLRowValues.
     */
    public static final List<SQLRowValues> trim(final Collection<SQLRowValues> graphs) {
        final List<SQLRowValues> res = new ArrayList<SQLRowValues>(graphs.size());
        for (final SQLRowValues r : graphs)
            res.add(trim(r));
        return res;
    }

    public static final SQLRowValues trim(final SQLRowValues r) {
        return new SQLRowValues(r, ForeignCopyMode.COPY_ID_OR_RM);
    }
}
