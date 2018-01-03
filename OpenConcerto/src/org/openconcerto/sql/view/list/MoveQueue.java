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

import org.openconcerto.sql.model.ConnectionHandlerNoSetup;
import org.openconcerto.sql.model.SQLDataSource;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSyntax;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.request.UpdateBuilder;
import org.openconcerto.sql.utils.ReOrder;
import org.openconcerto.sql.utils.SQLUtils;
import org.openconcerto.utils.DecimalUtils;
import org.openconcerto.utils.ExceptionUtils;
import org.openconcerto.utils.SleepingQueue;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import javax.swing.SwingUtilities;

public final class MoveQueue extends SleepingQueue {

    private final ITableModel tableModel;

    MoveQueue(ITableModel model) {
        super(MoveQueue.class.getSimpleName() + " on " + model);
        this.tableModel = model;
    }

    public Future<?> move(final List<? extends SQLRowAccessor> rows, final int inc) {
        if (inc == 0 || rows.size() == 0)
            return null;

        final boolean after = inc > 0;
        SQLRowAccessor outerR = null;
        for (final SQLRowAccessor r : rows) {
            if (outerR == null) {
                outerR = r;
            } else {
                final int compare = r.getOrder().compareTo(outerR.getOrder());
                if (after && compare > 0 || !after && compare < 0) {
                    outerR = r;
                }
            }
        }
        final int id = outerR.getID();
        return this.put(new Runnable() {
            public void run() {
                final FutureTask<ListSQLLine> destID = new FutureTask<ListSQLLine>(new Callable<ListSQLLine>() {
                    @Override
                    public ListSQLLine call() {
                        return MoveQueue.this.tableModel.getDestLine(id, inc);
                    }
                });
                MoveQueue.this.tableModel.invokeLater(destID);
                try {
                    if (destID.get() != null) {
                        SQLUtils.executeAtomic(getTable().getDBSystemRoot().getDataSource(), new ConnectionHandlerNoSetup<Object, Exception>() {
                            @Override
                            public Object handle(SQLDataSource ds) throws Exception {
                                moveQuick(rows, after, destID.get().getRow().asRow());
                                return null;
                            }
                        });
                    }
                } catch (Exception e) {
                    throw ExceptionUtils.createExn(IllegalStateException.class, "move failed", e);
                }
            }
        });
    }

    // row index as returned by JTable.DropLocation.getRow()
    public Future<?> moveTo(final List<? extends Number> rows, final int rowIndex) {
        if (rows.size() == 0)
            return null;

        // the user just dropped rows to move on rowIndex, find out right away which row that is.
        // (we used to go through the UpdateQueue which could result in the destination row the user
        // saw being replaced by a different row)
        assert rowIndex >= 0;
        assert SwingUtilities.isEventDispatchThread();
        final int rowCount = this.tableModel.getRowCount();
        final boolean after = rowIndex >= rowCount;
        final int index = after ? rowCount - 1 : rowIndex;
        final SQLRowValues line = this.tableModel.getRow(index).getRow();
        assert line.isFrozen() : "row could change by the time move() is called";

        return this.put(new Runnable() {
            @Override
            public void run() {
                try {
                    SQLUtils.executeAtomic(getTable().getDBSystemRoot().getDataSource(), new ConnectionHandlerNoSetup<Object, Exception>() {
                        @Override
                        public Object handle(SQLDataSource ds) throws Exception {
                            moveQuick(rows, after, line.asRow());
                            return null;
                        }
                    });
                } catch (Exception e) {
                    throw ExceptionUtils.createExn(IllegalStateException.class, "move failed", e);
                }
            }
        });
    }

    final void moveQuick(final List<?> srcRows, final boolean after, final SQLRow destRow) throws SQLException {
        final int rowCount = srcRows.size();
        // if only some rows are moved, update one by one (avoids refreshing the whole list)
        // (getRowCount() is not thread-safe, so use getTotalRowCount())
        if (rowCount < 5 && rowCount < (this.tableModel.getTotalRowCount() / 3)) {
            final List<?> l;
            if (after) {
                // If we want to put X,Y after A in the list A,B,C
                // we need to pass first Y : A,Y,B,C then X :
                // A,X,Y,B,C
                l = new ArrayList<Object>(srcRows);
                Collections.reverse(l);
            } else {
                l = srcRows;
            }
            final SQLTable t = getTable();
            final SQLRowValues vals = new SQLRowValues(t);
            for (final Object src : l) {
                vals.setOrder(destRow, after).update(getID(src, t).intValue());
            }
        } else {
            // update all rows at once and refresh the whole list
            moveAtOnce(srcRows, rowCount, after, destRow);
        }
    }

    private static Number getID(final Object o, final SQLTable t) {
        final Number res;
        if (o instanceof SQLRowAccessor) {
            final SQLRowAccessor row = (SQLRowAccessor) o;
            if (row.getTable() != t)
                throw new IllegalArgumentException("Not from the same table : " + row + " != " + t);
            res = row.getIDNumber();
        } else {
            res = (Number) o;
        }
        return res;
    }

    private SQLTable getTable() {
        return this.tableModel.getTable();
    }

    /**
     * Move the passed rows just before or just after the destination row. The current database
     * order of the rows is not used, i.e. after this method returns the order will be that of the
     * passed list, with <code>destRow</code> coming either before or after them all.
     * 
     * @param srcRows rows to reorder.
     * @param after <code>true</code> if the rows should be placed after <code>destRow</code>,
     *        <code>false</code> otherwise.
     * @param destRow <code>srcRows</code> will be placed relative to the order of this row.
     * @throws SQLException if an error occurs.
     */
    static public void moveAtOnce(final List<? extends SQLRowAccessor> srcRows, final boolean after, final SQLRow destRow) throws SQLException {
        moveAtOnce(srcRows, srcRows.size(), after, destRow);
    }

    static private void moveAtOnce(final List<?> srcRows, final int rowCount, final boolean after, final SQLRow destRow) throws SQLException {
        if (rowCount == 0)
            return;
        final SQLTable t = destRow.getTable();

        // ULP * 10 to give a little breathing room
        final BigDecimal minDistance = t.getOrderULP().scaleByPowerOfTen(1);
        assert minDistance.signum() > 0;
        final BigDecimal places = BigDecimal.valueOf(rowCount + 1);
        // the minimum room so that we can move all rows
        final BigDecimal room = minDistance.multiply(places);

        final BigDecimal destOrder = destRow.getOrder();
        final SQLRow nextRow = destRow.getRow(true);
        final BigDecimal inc;
        final boolean destRowReordered;
        if (nextRow == null) {
            // if destRow is the last row, we can choose whatever increment we want
            inc = ReOrder.DISTANCE;
            // but we need to move destRow if we want to add before it
            destRowReordered = false;
        } else {
            final BigDecimal nextOrder = nextRow.getOrder();
            assert nextOrder.compareTo(destOrder) > 0;
            final BigDecimal diff = nextOrder.subtract(destOrder);
            assert diff.signum() > 0;
            if (diff.compareTo(room) < 0) {
                // if there's not enough room, reorder to squeeze rows upwards
                // since we keep increasing count, we will eventually reorder all rows afterwards
                int count = 100;
                final int tableRowCount = t.getRowCount();
                boolean reordered = false;
                while (!reordered) {
                    // only push destRow upwards if we want to add before
                    reordered = ReOrder.create(t, destOrder, !after, count, destOrder.add(room)).exec();
                    if (!reordered && count > tableRowCount)
                        throw new IllegalStateException("Unable to reorder " + count + " rows in " + t);
                    count *= 10;
                }
                inc = minDistance;
                destRowReordered = true;
            } else {
                // truncate
                inc = DecimalUtils.round(diff.divide(places, DecimalUtils.HIGH_PRECISION), t.getOrderDecimalDigits(), RoundingMode.DOWN);
                destRowReordered = false;
            }
        }
        // i.e. inc > 0
        assert inc.compareTo(minDistance) >= 0;

        BigDecimal newOrder = destOrder;
        // by definition if we want to add after, destOrder should remain unchanged
        if (after) {
            newOrder = newOrder.add(inc);
        }
        final List<List<String>> newOrdersAndIDs = new ArrayList<List<String>>(rowCount);
        // we go from newOrder and up, so that the passed rows are in ascending order
        for (final Object src : srcRows) {
            final Number srcID = getID(src, t);
            newOrdersAndIDs.add(Arrays.asList(srcID.toString(), newOrder.toPlainString()));
            newOrder = newOrder.add(inc);
        }
        // move out before general request as most DB systems haven't got DEFERRABLE constraints
        if (!after && !destRowReordered) {
            final UpdateBuilder updateDestRow = new UpdateBuilder(t);
            updateDestRow.setObject(t.getOrderField(), newOrder);
            updateDestRow.setWhere(destRow.getWhere());
            t.getDBSystemRoot().getDataSource().execute(updateDestRow.asString());
        }

        final SQLSyntax syntax = SQLSyntax.get(t);
        final UpdateBuilder update = new UpdateBuilder(t);
        final String constantTableAlias = "newOrdersAndIDs";
        update.addVirtualJoin(syntax.getConstantTable(newOrdersAndIDs, constantTableAlias, Arrays.asList("ID", "newOrder")), constantTableAlias, true, "ID", t.getKey().getName());
        update.setFromVirtualJoinField(t.getOrderField().getName(), constantTableAlias, "newOrder");
        t.getDBSystemRoot().getDataSource().execute(update.asString());

        t.fireTableModified(SQLRow.NONEXISTANT_ID, Collections.singletonList(t.getOrderField().getName()));
    }
}
