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

import org.openconcerto.sql.model.FieldPath;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLRowValues.CreateMode;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.SQLTable.VirtualFields;
import org.openconcerto.sql.model.graph.Path;
import org.openconcerto.sql.view.list.search.SearchQueue;
import org.openconcerto.utils.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;

/**
 * A line used by SQLTableModelSource, posessing an order and an id. Compare is done on the order.
 * 
 * @author Sylvain
 */
@ThreadSafe
public final class ListSQLLine implements Comparable<ListSQLLine> {

    public static final int indexFromID(final List<ListSQLLine> l, final int id) {
        int foundIndex = -1;
        final int size = l.size();
        for (int i = 0; i < size; i++) {
            final int currentID = l.get(i).getID();
            if (currentID == id) {
                foundIndex = i;
                break;
            }
        }
        return foundIndex;
    }

    static final ListSQLLine fromID(final List<ListSQLLine> list, final int id) {
        final int size = list.size();
        for (int i = 0; i < size; i++) {
            final ListSQLLine line = list.get(i);
            if (line.getID() == id)
                return line;
        }
        return null;
    }

    private final SQLTableModelLinesSource src;
    // unmodifiable
    @GuardedBy("this")
    private SQLRowValues row;
    // Immutable
    private final SQLTableModelSourceState state;
    private final int id;
    // allow to order by something not in the row
    @GuardedBy("this")
    private Number order;
    // lists are accessed by Swing (model.getValueAt()) and
    // by the search queue (SearchRunnable#matchFilter(ListSQLLine line))
    // immutable
    @GuardedBy("this")
    private List<Object> list;

    ListSQLLine(SQLTableModelLinesSource src, SQLRowValues row, int id, final SQLTableModelSourceState state) {
        super();
        this.src = src;
        this.setRow(row);
        this.id = id;
        this.state = state;
        this.clearCache();
    }

    // load at least columnCount values
    // (to avoid loading debug columns, which took more time than the regular columns, ie more than
    // half the time was passed on almost never displayed values)
    private void loadCache(int columnCount) {
        updateList(columnCount, Collections.<Integer> emptySet());
    }

    public final SQLTableModelSourceState getState() {
        return this.state;
    }

    public final SQLTableModelColumns getColumns() {
        return this.getState().getAllColumns();
    }

    public final SQLTableModelLinesSource getSrc() {
        return this.src;
    }

    private final void setRow(SQLRowValues v) {
        if (!v.isFrozen())
            throw new IllegalArgumentException("Not frozen : " + v);
        synchronized (this) {
            this.row = v;
        }
    }

    public synchronized final SQLRowValues getRow() {
        return this.row;
    }

    @Override
    public int compareTo(ListSQLLine o) {
        if (this.src != o.src)
            throw new IllegalArgumentException(this.src + " != " + o.src);
        return this.src.compare(this, o);
    }

    public int getID() {
        return this.id;
    }

    public synchronized final void setOrder(Number order) {
        this.order = order;
    }

    public synchronized final Number getOrder() {
        return this.order;
    }

    public synchronized List<Object> getList(int columnCount) {
        this.loadCache(columnCount);
        return this.list;
    }

    public Object getValueAt(int column) {
        return this.getList(column + 1).get(column);
    }

    public final void setValueAt(Object obj, int colIndex) {
        this.getColumns().getColumns().get(colIndex).put(this, obj);
    }

    // should update this.list at the passed indexes, and then recursively for dependent columns
    // return all updated indexes
    // for now dependent columns (i.e. getUsedCols()) aren't supported so just return our parameter
    private final Set<Integer> updateValueAt(final Set<Integer> colIndexes) {
        if (colIndexes.size() == 0)
            return colIndexes;
        updateList(-1, colIndexes);
        return colIndexes;
    }

    // @param newSize negative means use current value
    private final boolean updateList(int newSize, final Set<Integer> colsToUpdate) {
        final int minIndexToUpdate;
        if (colsToUpdate.isEmpty()) {
            minIndexToUpdate = -1;
        } else {
            minIndexToUpdate = Collections.min(colsToUpdate).intValue();
            if (minIndexToUpdate < 0)
                throw new IllegalArgumentException("Negative indexes : " + colsToUpdate);
        }
        synchronized (this) {
            final int alreadyLoaded = this.list.size();
            if (newSize < 0)
                newSize = alreadyLoaded;
            // if there's enough items and either nothing to update or the columns to update aren't
            // yet needed, return
            if (alreadyLoaded >= newSize && (minIndexToUpdate < 0 || minIndexToUpdate >= alreadyLoaded))
                return false;
            final List<Object> newList = new ArrayList<Object>(newSize);
            for (int i = 0; i < newSize; i++) {
                final Object o;
                if (i >= alreadyLoaded || colsToUpdate.contains(i))
                    o = this.getColumns().getAllColumns().get(i).show(this.getRow());
                else
                    o = this.list.get(i);
                newList.add(o);
            }
            this.list = Collections.unmodifiableList(newList);
        }
        return true;
    }

    public void clearCache() {
        synchronized (this) {
            this.list = Collections.emptyList();
        }
    }

    /**
     * Load the passed values into this row at the passed path.
     * 
     * @param id ID of vals, needed when vals is <code>null</code>.
     * @param vals values to load, eg CONTACT.NOM = "Dupont".
     * @param p where to load the values, eg "SITE.ID_CONTACT_CHEF".
     * @return the columns that were affected, <code>null</code> meaning all.
     */
    synchronized Set<Integer> loadAt(int id, SQLRowValues vals, Path p) {
        assert vals == null || vals.getID() == id;
        final String lastReferentField = SearchQueue.getLastReferentField(p);
        // null vals means id was deleted, the only way we care is if it was pointing to us
        // (otherwise the foreign key pointing to it would have changed first)
        assert vals != null || lastReferentField != null;
        // load() empties vals, so getFields() before
        final Set<Integer> indexes = lastReferentField == null ? this.pathToIndex(p, vals.getFields()) : null;
        // replace our values with the new ones
        final SQLRowValues copy = this.getRow().deepCopy();
        if (lastReferentField == null) {
            for (final SQLRowValues v : copy.followPath(p, CreateMode.CREATE_NONE, false)) {
                // check id, e.g. if p is BATIMENT <- LOCAL -> FAMILLE_LOCAL, then there's multiple
                // familles
                if (v.getID() == id)
                    v.load(vals.deepCopy(), null);
            }
        } else {
            // e.g. if p is SITE <- BATIMENT <- LOCAL, lastField is LOCAL.ID_BATIMENT
            // if p is SITE -> CLIENT <- SITE (i.e. siblings of a site), lastField is SITE.ID_CLIENT
            final SQLField lastField = p.getStep(-1).getSingleField();
            final Collection<SQLRowValues> previous;
            if (p.length() > 1 && p.getStep(-2).reverse().equals(p.getStep(-1)))
                previous = copy.followPath(p.minusLast(2), CreateMode.CREATE_NONE, false);
            else
                previous = null;
            // the rows that vals should point to, e.g. BATIMENT or CLIENT
            final Collection<SQLRowValues> targets = copy.followPath(p.minusLast(), CreateMode.CREATE_NONE, false);
            for (final SQLRowValues target : targets) {
                // remove existing referent with the updated ID
                SQLRowValues toRemove = null;
                for (final SQLRowValues toUpdate : target.getReferentRows(lastField)) {
                    // don't back track (in the example a given SITE will be at the primary location
                    // and a second time along its siblings)
                    if ((previous == null || !previous.contains(toUpdate)) && toUpdate.getID() == id) {
                        if (toRemove != null)
                            throw new IllegalStateException("Duplicate IDs " + id + " : " + System.identityHashCode(toRemove) + " and " + System.identityHashCode(toUpdate) + "\n" + copy.printGraph());
                        toRemove = toUpdate;
                    }
                }
                if (toRemove != null)
                    toRemove.remove(lastField.getName());
                // attach updated values
                if (vals != null && vals.getLong(lastField.getName()) == target.getIDNumber().longValue())
                    vals.deepCopy().put(lastField.getName(), target);
            }
        }
        copy.getGraph().freeze();
        this.setRow(copy);
        // update our cache
        if (indexes == null) {
            this.clearCache();
            return null;
        } else {
            return this.updateValueAt(indexes);
        }
    }

    /**
     * Find the columns that use the modifiedFields for their value.
     * 
     * @param p the path to the modified fields, eg "CPI.ID_LOCAL".
     * @param modifiedFields the field modified, eg "DESIGNATION".
     * @return the index of columns using "CPI.ID_LOCAL.DESIGNATION", or null for every columns.
     */
    private Set<Integer> pathToIndex(final Path p, final Collection<String> modifiedFields) {
        // TODO check if the column paths start with any of the modified foreign keys
        // since it's quite expensive, add a cache in SQLTableModelSource
        if (containsFK(p.getLast(), modifiedFields)) {
            // e.g. CPI.ID_LOCAL, easier to just refresh the whole line, than to search for each
            // column affected (that would mean expanding the FK)
            return null;
        } else {
            final Set<Integer> res = new HashSet<Integer>();
            final Set<FieldPath> modifiedPaths = FieldPath.create(p, modifiedFields);
            final List<? extends SQLTableModelColumn> cols = this.getColumns().getAllColumns();
            for (int i = 0; i < cols.size(); i++) {
                final SQLTableModelColumn col = cols.get(i);
                if (CollectionUtils.containsAny(col.getPaths(), modifiedPaths))
                    res.add(i);
            }
            return res;
        }
    }

    private static boolean containsFK(final SQLTable t, Collection<String> fields) {
        final Set<SQLField> ffs = t.getFields(VirtualFields.FOREIGN_KEYS);
        for (final String f : fields) {
            if (ffs.contains(t.getField(f)))
                return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " on " + this.getRow();
    }
}
