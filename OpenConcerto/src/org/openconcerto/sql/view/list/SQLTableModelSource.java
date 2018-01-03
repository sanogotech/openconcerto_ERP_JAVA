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

import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.FieldPath;
import org.openconcerto.sql.model.IFieldPath;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLFieldsSet;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLRowValues.ForeignCopyMode;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.graph.Path;
import org.openconcerto.sql.request.BaseFillSQLRequest;
import org.openconcerto.sql.request.ListSQLRequest;
import org.openconcerto.utils.cc.IClosure;
import org.openconcerto.utils.change.ListChangeIndex;
import org.openconcerto.utils.change.ListChangeRecorder;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.SwingUtilities;

import net.jcip.annotations.GuardedBy;

/**
 * Define the columns and lines for ITableModel.
 * 
 * @author Sylvain
 */
public abstract class SQLTableModelSource {

    protected static final class DebugRow extends BaseSQLTableModelColumn {
        private final SQLTable t;

        protected DebugRow(final SQLTable t) {
            // don't put SQLRowAccessor: it's an interface and thus JTable.getDefaultRenderer()
            // returns null
            super("Fields", Object.class);
            this.t = t;
        }

        @Override
        protected Object show_(SQLRowAccessor r) {
            if (r instanceof SQLRow)
                return r;
            else
                return new SQLRowValues((SQLRowValues) r, ForeignCopyMode.COPY_ID_OR_RM);
        }

        @Override
        public Set<SQLField> getFields() {
            return this.t.getFields();
        }

        @Override
        public Set<FieldPath> getPaths() {
            return FieldPath.create(Path.get(this.t), this.t.getFieldsName());
        }
    }

    private final ListSQLRequest req;
    private final SQLElement elem;
    // only from EDT
    private SQLRowValues inited;
    // this.cols + debugCols, unmodifiable
    @GuardedBy("this")
    private SQLTableModelColumns allCols;
    // only from EDT
    private final ListChangeRecorder<SQLTableModelColumn> cols;
    // only from EDT
    private final List<SQLTableModelColumn> debugCols;
    // to notify of columns change, better than having one listener per line
    private final List<WeakReference<SQLTableModelLinesSource>> lines;

    private final PropertyChangeSupport supp;

    {
        this.supp = new PropertyChangeSupport(this);
        this.lines = new ArrayList<WeakReference<SQLTableModelLinesSource>>();
    }

    public SQLTableModelSource(final ListSQLRequest req, final SQLElement elem) {
        if (elem == null)
            throw new IllegalArgumentException("Missing element");
        this.req = req;
        this.elem = elem;
        if (!this.getPrimaryTable().equals(this.elem.getTable()))
            throw new IllegalArgumentException("not the same table: " + this.getPrimaryTable() + " != " + this.elem);
        this.setAllCols(SQLTableModelColumns.empty());
        this.cols = new ListChangeRecorder<SQLTableModelColumn>(new ArrayList<SQLTableModelColumn>());
        this.debugCols = new ArrayList<SQLTableModelColumn>();
        this.inited = req.getGraph();
    }

    public SQLElement getElem() {
        return this.elem;
    }

    // lazy initialization since this method calls colsChanged() which subclasses overload and
    // they need their own attribute that aren't set yet since super() must be the first statement.
    public void init() {
        assert SwingUtilities.isEventDispatchThread();
        if (this.inited == null)
            return;

        final SQLRowValues graph = this.inited;

        graph.walkFields(new IClosure<FieldPath>() {
            @Override
            public void executeChecked(final FieldPath input) {
                final SQLField f = input.getField();
                if (f.getTable().getLocalContentFields().contains(f)) {
                    final SQLTableModelColumnPath col = new SQLTableModelColumnPath(input, null, getElem().getDirectory());
                    SQLTableModelSource.this.cols.add(col);
                } else
                    SQLTableModelSource.this.debugCols.add(new SQLTableModelColumnPath(input.getPath(), f.getName(), f.toString()) {
                        // don't show the rowValues since it's very verbose (and all content fields
                        // are already displayed as normal columns) and unsortable
                        @Override
                        protected Object show_(SQLRowAccessor r) {
                            final Object res = super.show_(r);
                            return res instanceof SQLRowValues ? ((SQLRowValues) res).getID() : res;
                        }
                    });
            }
        }, true);

        this.debugCols.add(new DebugRow(getPrimaryTable()));
        final SQLField orderField = getPrimaryTable().getOrderField();
        if (orderField != null)
            this.debugCols.add(new SQLTableModelColumnPath(Path.get(getPrimaryTable()), orderField.getName(), "Order"));
        this.debugCols.add(new SQLTableModelColumnPath(Path.get(getPrimaryTable()), getPrimaryTable().getKey().getName(), "PrimaryKey"));

        // at the end so that fireColsChanged() can use it
        this.inited = null;
        listenToCols();
        updateCols(null);
    }

    public SQLTableModelSource(SQLTableModelSource src) {
        this.req = src.req;
        this.elem = src.elem;
        this.setAllCols(src.getAllColumns());
        this.cols = new ListChangeRecorder<SQLTableModelColumn>(new ArrayList<SQLTableModelColumn>(src.cols));
        this.debugCols = new ArrayList<SQLTableModelColumn>(src.debugCols);
        this.inited = null;
        listenToCols();
    }

    private void listenToCols() {
        assert SwingUtilities.isEventDispatchThread();
        // keep allCols in sync with cols, and listen to any change
        this.cols.getRecipe().addListener(new IClosure<ListChangeIndex<SQLTableModelColumn>>() {
            @Override
            public void executeChecked(ListChangeIndex<SQLTableModelColumn> change) {
                updateCols(change);
            }
        });
    }

    protected final void updateCols(ListChangeIndex<SQLTableModelColumn> change) {
        assert SwingUtilities.isEventDispatchThread();
        // do not fire while initializing
        assert this.inited == null;

        if (change != null && change.getItemsAdded().isEmpty() && change.getItemsRemoved().isEmpty())
            return;
        final SQLTableModelSourceState beforeState = this.createState();
        this.setAllCols(new SQLTableModelColumns(this.cols, this.debugCols));
        colsChanged(change == null ? new ListChangeIndex.Add<SQLTableModelColumn>(0, this.getAllColumns().getAllColumns()) : change);
        final SQLTableModelSourceState afterState = this.createState();
        fireColsChanged(beforeState, afterState);
    }

    protected final SQLTableModelSourceState createState() {
        return new SQLTableModelSourceState(this.getAllColumns(), this.getReq());
    }

    private final void colsChanged(final ListChangeIndex<SQLTableModelColumn> change) {
        final AtomicBoolean biggerGraph = new AtomicBoolean(false);
        // add needed fields for each new column
        this.getReq().changeGraphToFetch(new IClosure<SQLRowValues>() {
            @Override
            public void executeChecked(SQLRowValues g) {
                for (final SQLTableModelColumn col : change.getItemsAdded()) {
                    // DebugRow should uses all *fetched* fields, but since it cannot know them
                    // getPaths() return all fields in the table. So don't fetch all fields just for
                    // this debug column
                    if (!(col instanceof DebugRow)) {
                        for (final IFieldPath p : col.getPaths()) {
                            if (BaseFillSQLRequest.addToFetch(g, p.getPath(), Collections.singleton(p.getFieldName())))
                                biggerGraph.set(true);
                        }
                    }
                }
            }
        });
        if (biggerGraph.get() && !allowBiggerGraph())
            throw new IllegalStateException("Bigger graph not allowed");
    }

    protected abstract boolean allowBiggerGraph();

    private void fireColsChanged(final SQLTableModelSourceState beforeState, final SQLTableModelSourceState afterState) {
        // let know each of our LinesSource that the columns have changed
        for (final SQLTableModelLinesSource line : getLines()) {
            line.colsChanged(beforeState, afterState);
        }
        // before notifying our regular listeners
        this.supp.firePropertyChange("cols", null, this.cols);
    }

    private final List<SQLTableModelLinesSource> getLines() {
        final List<SQLTableModelLinesSource> res = new ArrayList<SQLTableModelLinesSource>();
        int i = 0;
        while (i < this.lines.size()) {
            final WeakReference<SQLTableModelLinesSource> l = this.lines.get(i);
            final SQLTableModelLinesSource line = l.get();
            if (line == null)
                this.lines.remove(i);
            else {
                res.add(line);
                i++;
            }
        }
        return res;
    }

    protected final int getLinesCount() {
        return this.getLines().size();
    }

    public final SQLTableModelLinesSource createLinesSource(ITableModel model) {
        this.init();
        final SQLTableModelLinesSource res = this._createLinesSource(model);
        this.lines.add(new WeakReference<SQLTableModelLinesSource>(res));
        return res;
    }

    protected abstract SQLTableModelLinesSource _createLinesSource(ITableModel model);

    /**
     * The maximum graph of the lines returned by {@link #createLinesSource(ITableModel)}.
     * 
     * @return the maximum graph of our lines.
     */
    public final SQLRowValues getMaxGraph() {
        return this.getReq().getGraphToFetch();
    }

    // * columns

    /**
     * The normal columns.
     * 
     * @return the normal columns.
     */
    public final List<SQLTableModelColumn> getColumns() {
        this.init();
        return this.cols;
    }

    private synchronized void setAllCols(SQLTableModelColumns allCols) {
        this.allCols = allCols;
    }

    /**
     * The normal columns plus some debug columns. Usually primary and foreign keys.
     * 
     * @return the debub columns.
     */
    public synchronized final SQLTableModelColumns getAllColumns() {
        return this.allCols;
    }

    public final void addDebugColumn(final SQLTableModelColumn col) {
        this.init();
        this.debugCols.add(col);
        updateCols(new ListChangeIndex.Add<SQLTableModelColumn>(this.getAllColumns().size(), Collections.singleton(col)));
    }

    public final SQLTableModelColumn getColumn(int index) {
        return this.getAllColumns().getAllColumns().get(index);
    }

    /**
     * All the columns that depends on the passed field.
     * 
     * @param f the field.
     * @return all columns needing <code>f</code>.
     */
    public final List<SQLTableModelColumn> getColumns(SQLField f) {
        return this.getAllColumns().getColumns(f);
    }

    /**
     * The column depending solely on the passed field.
     * 
     * @param f the field.
     * @return the column needing only <code>f</code>.
     * @throws IllegalArgumentException if more than one column matches.
     */
    public final SQLTableModelColumn getColumn(SQLField f) {
        return this.getAllColumns().getColumn(f);
    }

    /**
     * The column depending solely on the passed path.
     * 
     * @param fp the field path.
     * @return the column needing only <code>fp</code>.
     * @throws IllegalArgumentException if more than one column matches.
     */
    public final SQLTableModelColumn getColumn(FieldPath fp) {
        return this.getAllColumns().getColumn(fp);
    }

    public final void addColumnListener(PropertyChangeListener l) {
        this.supp.addPropertyChangeListener("cols", l);
    }

    public final void rmColumnListener(PropertyChangeListener l) {
        this.supp.removePropertyChangeListener("cols", l);
    }

    // * SQLIdentifier

    public final ListSQLRequest getReq() {
        return this.req;
    }

    public final SQLTable getPrimaryTable() {
        return this.getReq().getPrimaryTable();
    }

    /**
     * All the displayed tables, i.e. tables of {@link #getLineFields()}.
     * 
     * @return the displayed tables.
     */
    public final Set<SQLTable> getTables() {
        return new SQLFieldsSet(this.getLineFields()).getTables();
    }

    /**
     * All fields that affects a line of this source. I.e. not just the displayed fields, but also
     * the foreign keys, including intermediate ones (e.g. if this displays [BATIMENT.DES, CPI.DES]
     * LOCAL.ID_BATIMENT matters).
     * 
     * @return the fields affecting this.
     */
    public final Set<SQLField> getLineFields() {
        final Set<SQLField> res = new HashSet<SQLField>();
        for (final SQLRowValues v : getMaxGraph().getGraph().getItems()) {
            for (final String f : v.getFields())
                res.add(v.getTable().getField(f));
            if (v.getTable().isArchivable())
                res.add(v.getTable().getArchiveField());
        }
        return res;
    }

}
