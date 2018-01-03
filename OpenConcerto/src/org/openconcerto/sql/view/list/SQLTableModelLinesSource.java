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

import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.graph.Path;
import org.openconcerto.sql.request.ListSQLRequest;
import org.openconcerto.utils.Value;
import org.openconcerto.utils.cc.IPredicate;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

// use SQLRowValues to allow graph
public abstract class SQLTableModelLinesSource {

    private final ITableModel model;
    private final List<PropertyChangeListener> listeners;
    private IPredicate<SQLRowValues> filter;

    {
        this.listeners = new ArrayList<PropertyChangeListener>();
        this.filter = null;
    }

    protected SQLTableModelLinesSource(final ITableModel model) {
        this.model = model;
    }

    void die() {
    }

    public final ITableModel getModel() {
        return this.model;
    }

    public abstract SQLTableModelSource getParent();

    public final ListSQLRequest getUpdateQueueReq() {
        return this.getModel().getUpdateQ().getState().getReq();
    }

    public abstract List<ListSQLLine> getAll();

    /**
     * A row in the DB has been changed, fetch its current value.
     * 
     * @param id a valid ID of a database row.
     * @return if not {@link Value#hasValue()} the event should be ignored, otherwise the new value
     *         for the passed ID, <code>null</code> if it is not part of this.
     */
    public abstract Value<ListSQLLine> get(final int id);

    public abstract int compare(ListSQLLine l1, ListSQLLine l2);

    public abstract Future<?> moveBy(final List<? extends SQLRowAccessor> rows, final int inc);

    // take IDs :
    // 1. no need to protect rows from modifications, just copy IDs
    // 2. for non committed rows, i.e. without DB ID and thus without SQLRow, it's easier to handle
    // (virtual) IDs than to use IdentitySet of SQLRowValues
    public abstract Future<?> moveTo(final List<? extends Number> rows, final int rowIndex);

    public final void setFilter(final IPredicate<SQLRowValues> filter) {
        // always fire since for now there's no other way for the caller
        // (ie if the meaning of filter change, it has to do setFilter(getFilter()) )
        this.filter = filter;
        this.fireChanged(new PropertyChangeEvent(this, "filter", null, this.filter));
    }

    public final IPredicate<SQLRowValues> getFilter() {
        return this.filter;
    }

    /**
     * Adds a listener to be notified when {@link #getAll()} change value.
     * 
     * @param l the listener.
     */
    public final void addListener(PropertyChangeListener l) {
        this.listeners.add(l);
    }

    public final void rmListener(PropertyChangeListener l) {
        this.listeners.remove(l);
    }

    protected final void fireChanged(PropertyChangeEvent evt) {
        for (final PropertyChangeListener l : this.listeners)
            l.propertyChange(evt);
    }

    protected final ListSQLLine createLine(final SQLRowValues v) {
        return this.createLine(v, null);
    }

    /**
     * Create a line with the passed row.
     * 
     * @param v the values.
     * @param passedID the {@link ListSQLLine#getID() ID} of the result, <code>null</code> meaning
     *        {@link SQLRowValues#getID()}.
     * @return a new line.
     * @throws IllegalArgumentException if <code>passedID</code> is <code>null</code> and
     *         <code>v</code> {@link SQLRowValues#hasID() has no ID}.
     */
    protected final ListSQLLine createLine(final SQLRowValues v, final Number passedID) {
        if (v == null || (this.filter != null && !this.filter.evaluateChecked(v)))
            return null;
        final int id;
        if (passedID != null) {
            id = passedID.intValue();
        } else if (v.hasID()) {
            id = v.getID();
        } else {
            throw new IllegalArgumentException("No ID for " + v);
        }
        final ListSQLLine res = new ListSQLLine(this, v, id, this.getModel().getUpdateQ().getState());
        this.lineCreated(res);
        return res;
    }

    /**
     * A new line has been created. This implementation does nothing.
     * 
     * @param res the newly created line.
     */
    protected void lineCreated(ListSQLLine res) {
    }

    final void colsChanged(final SQLTableModelSourceState beforeState, final SQLTableModelSourceState afterState) {
        this.model.getUpdateQ().stateChanged(beforeState, afterState);
    }

    /**
     * Change the line <code>l</code> at the passed path with the passed values.
     * 
     * @param l the line to change, eg RECEPTEUR[12].
     * @param path the changing path, eg RECEPTEUR.ID_LIMITEUR.
     * @param vals the new values, eg LIMITEUR{ID=4, DESIGNATION="dess"}.
     * @throws SQLException if the values cannot be commited.
     */
    public abstract void commit(ListSQLLine l, Path path, SQLRowValues vals) throws SQLException;
}
