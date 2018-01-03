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
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.graph.Path;
import org.openconcerto.sql.request.ListSQLRequest;
import org.openconcerto.utils.SleepingQueue.LethalFutureTask;
import org.openconcerto.utils.SleepingQueue.RunningState;
import org.openconcerto.utils.Value;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.swing.SwingUtilities;

/**
 * Lines are taken directly from the database.
 * 
 * @author Sylvain
 */
public class SQLTableModelLinesSourceOnline extends SQLTableModelLinesSource {

    private final SQLTableModelSourceOnline parent;
    private final PropertyChangeListener listener;
    private MoveQueue moveQ;

    public SQLTableModelLinesSourceOnline(SQLTableModelSourceOnline parent, final ITableModel model) {
        super(model);
        this.parent = parent;
        this.listener = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                fireChanged(evt);
            }
        };
        this.getParent().getReq().addWhereListener(this.listener);
        this.moveQ = null;
    }

    MoveQueue getMoveQ() {
        assert SwingUtilities.isEventDispatchThread();
        if (this.moveQ == null) {
            this.moveQ = new MoveQueue(getModel());
            this.moveQ.start();
        }
        return this.moveQ;
    }

    @Override
    protected void die() {
        this.getParent().getReq().rmWhereListener(this.listener);

        if (this.moveQ != null) {
            final RunningState threadState = this.moveQ.getRunningState();
            if (threadState == RunningState.RUNNING) {
                final LethalFutureTask<?> dieMove = this.moveQ.die();
                getModel().wait(dieMove, 15, TimeUnit.MILLISECONDS);
            } else {
                Log.get().warning("Not dying since queue is " + threadState);
            }
        }

        super.die();
    }

    @Override
    public final SQLTableModelSourceOnline getParent() {
        return this.parent;
    }

    @Override
    public List<ListSQLLine> getAll() {
        final List<SQLRowValues> values = this.getUpdateQueueReq().getValues();
        final List<ListSQLLine> res = new ArrayList<ListSQLLine>(values.size());
        for (final SQLRowValues v : values) {
            final ListSQLLine newLine = createLine(v);
            if (newLine != null)
                res.add(newLine);
        }
        return res;
    }

    @Override
    public Value<ListSQLLine> get(final int id) {
        return Value.getSome(createLine(this.getUpdateQueueReq().getValues(id)));
    }

    private BigDecimal getOrder(SQLRowAccessor r) {
        return (BigDecimal) r.getObject(r.getTable().getOrderField().getName());
    }

    @Override
    public int compare(ListSQLLine l1, ListSQLLine l2) {
        return getOrder(l1.getRow()).compareTo(getOrder(l2.getRow()));
    }

    @Override
    public Future<?> moveBy(List<? extends SQLRowAccessor> rows, int inc) {
        return this.getMoveQ().move(rows, inc);
    }

    @Override
    public Future<?> moveTo(List<? extends Number> rows, int rowIndex) {
        return this.getMoveQ().moveTo(rows, rowIndex);
    }

    @Override
    public void commit(ListSQLLine l, Path path, SQLRowValues vals) throws SQLException {
        vals.update();
    }

}
