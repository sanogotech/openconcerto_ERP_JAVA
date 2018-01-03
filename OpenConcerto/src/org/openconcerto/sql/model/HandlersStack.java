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

import org.openconcerto.sql.request.SQLCache;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

import net.jcip.annotations.GuardedBy;

class HandlersStack {
    private final SQLDataSource ds;
    private Connection conn;
    private final LinkedList<ConnectionHandler<?, ?>> stack;
    private boolean changeAllowed;
    // list of transaction points, i.e. first a transaction start and then any number of save
    // points. The list is thus empty in auto-commit mode.
    private final LinkedList<TransactionPoint> txPoints;
    // the cache for each point, items can be null if no cache should be used
    private final LinkedList<SQLCache<List<?>, Object>> caches;
    // only this thread can access this instance (except for dirtyCache)
    private final Thread thr;
    @GuardedBy("this")
    private boolean dirtyCache;

    HandlersStack(final SQLDataSource ds, final Connection conn, final ConnectionHandler<?, ?> handler) {
        super();
        if (conn == null)
            throw new NullPointerException("null connection");
        this.ds = ds;
        this.changeAllowed = false;
        this.conn = conn;
        this.stack = new LinkedList<ConnectionHandler<?, ?>>();
        this.push(handler);
        this.txPoints = new LinkedList<TransactionPoint>();
        this.caches = new LinkedList<SQLCache<List<?>, Object>>();
        this.thr = Thread.currentThread();
        this.dirtyCache = false;
    }

    final Thread getThread() {
        return this.thr;
    }

    private synchronized boolean setDirtyCache(boolean dirtyCache) {
        final boolean prev = this.dirtyCache;
        this.dirtyCache = dirtyCache;
        return prev;
    }

    private boolean checkDirtyCache() {
        final boolean dirtyCache = this.setDirtyCache(false);
        if (dirtyCache) {
            final boolean done = this.updateCache();
            assert done : "The caches were not updated even though this is the correct thread";
        }
        return dirtyCache;
    }

    public final boolean hasValidConnection() {
        return this.conn != null;
    }

    public final boolean hasTransaction() {
        return !this.txPoints.isEmpty();
    }

    public final Connection getConnection() throws IllegalStateException {
        if (this.conn == null)
            throw new IllegalStateException("connection was invalidated");
        return this.conn;
    }

    // return null if already invalid
    final Connection invalidConnection() {
        final Connection res = this.conn;
        this.conn = null;
        return res;
    }

    final HandlersStack push(final ConnectionHandler<?, ?> handler) {
        this.stack.addFirst(handler);
        return this;
    }

    /**
     * Remove the last added ConnectionHandler.
     * 
     * @return <code>true</code> if this is now empty.
     */
    final boolean pop() {
        this.stack.removeFirst();
        return this.stack.isEmpty();
    }

    private final boolean assertPointsAndCacheCoherence(final int size) {
        assert Thread.currentThread() == this.getThread() : "Wrong thread : " + Thread.currentThread() + " should be " + this.getThread();
        assert this.txPoints.size() == this.caches.size();
        if (size >= 0)
            assert this.txPoints.size() == size;
        return true;
    }

    final void addTxPoint(TransactionPoint txPoint) {
        if (txPoint.getConn() != this.conn)
            throw new IllegalArgumentException("Different connections");
        checkDirtyCache();
        // the first point should be a transaction start
        assert this.stack.size() > 0 || txPoint.getSavePoint() == null;
        txPoint.setPrevious(this.getLastTxPoint());
        this.txPoints.add(txPoint);
        this.addCache();
        assert assertPointsAndCacheCoherence(-1);
    }

    private void addCache() {
        final TransactionPoint txPoint = this.txPoints.get(this.caches.size());
        final SQLCache<List<?>, Object> previous = this.getCache();
        final SQLCache<List<?>, Object> current = this.ds.createCache(txPoint);
        this.caches.add(current);
        if (current != null) {
            // don't use parent inside a transaction (i.e. between save points) as this means doing
            // n lookups instead of just one.
            // don't use parent from a transaction to the committed cache as if a transaction update
            // a row, then a select from outside fill the cache, the transaction will get the
            // committed value not its own. MAYBE store events of a transaction so that the cache
            // only asks the committed cache if the data of the key hasn't been changed.
            current.setParent(null);
            current.putAll(previous != null ? previous : this.ds.getCommittedCache(), false);
        }
    }

    private final void removeLastCache() {
        // throws NoSuchElementException, i.e. if last is null it's because the item was null
        final SQLCache<List<?>, Object> last = this.caches.removeLast();
        if (last != null)
            last.clear();
        if (this.caches.isEmpty())
            this.setDirtyCache(false);
    }

    // this method is thread-safe
    boolean updateCache() {
        final boolean done = Thread.currentThread() == this.getThread();
        if (done) {
            final int size = this.txPoints.size();
            // cache only needed for transactions
            if (size > 0) {
                clearCache();
                for (int i = 0; i < size; i++) {
                    this.addCache();
                }
            }
            assert assertPointsAndCacheCoherence(size);
        } else {
            this.setDirtyCache(true);
        }
        return done;
    }

    private final void clearCache() {
        while (!this.caches.isEmpty()) {
            removeLastCache();
        }
    }

    final TransactionPoint getLastTxPoint() {
        return this.txPoints.peekLast();
    }

    final SQLCache<List<?>, Object> getCache() {
        checkDirtyCache();
        return this.caches.peekLast();
    }

    private final TransactionPoint removeFirstTxPoint() {
        return this.txPoints.pollFirst();
    }

    private final TransactionPoint removeLastTxPoint() {
        return this.txPoints.pollLast();
    }

    // the JDBC transaction was committed, update our state
    void commit(final TransactionPoint newTxPoint) throws SQLException {
        // * catch up with JDBC state
        if (!this.hasTransaction())
            throw new IllegalStateException("No transaction to commit");
        if (this.txPoints.size() > 1) {
            this.releaseSavepoint(this.txPoints.get(1).getSavePoint());
        }
        assert this.txPoints.size() == 1;
        final TransactionPoint txPoint = this.removeFirstTxPoint();
        txPoint.setCommitted(true);
        this.removeLastCache();
        assert assertPointsAndCacheCoherence(0);
        assert !checkDirtyCache();
        // if we're not in auto-commit, a JDBC transaction is already underway
        // (must be done before notifying listeners, so they have our state and the JDBC state
        // coherent)
        if (newTxPoint != null)
            this.addTxPoint(newTxPoint);

        // * then in a coherent state, fire
        txPoint.fire();
    }

    // the JDBC transaction was aborted, update our state
    void rollback(TransactionPoint newTxPoint) throws SQLException {
        // rollback can only be called while not in auto-commit
        if (newTxPoint == null)
            throw new NullPointerException("Missing transaction point");

        final LinkedList<TransactionPoint> toFire = new LinkedList<TransactionPoint>(this.txPoints);
        this.txPoints.clear();
        // discard the cache
        clearCache();
        assert assertPointsAndCacheCoherence(0);
        assert !checkDirtyCache();

        // as we're not in auto-commit, a JDBC transaction is already underway
        // (must be done before notifying listeners, so they have our state and the JDBC state
        // coherent)
        this.addTxPoint(newTxPoint);

        final Iterator<TransactionPoint> iter = toFire.descendingIterator();
        while (iter.hasNext()) {
            final TransactionPoint txPoint = iter.next();
            txPoint.setCommitted(false);
            txPoint.fire();
        }
    }

    void rollback(Savepoint savepoint) throws SQLException {
        final List<TransactionPoint> pointsToFire = new ArrayList<TransactionPoint>();

        TransactionPoint txPoint = this.removeLastTxPoint();
        while (txPoint.getSavePoint() != savepoint) {
            pointsToFire.add(txPoint);
            // discard the cache
            removeLastCache();
            txPoint = this.removeLastTxPoint();
        }
        pointsToFire.add(txPoint);
        // discard the cache
        removeLastCache();
        assert assertPointsAndCacheCoherence(-1);
        checkDirtyCache();

        // as we're notified after JDBC, first replicate its state then notify listeners
        for (final TransactionPoint toFire : pointsToFire) {
            toFire.setCommitted(false);
            toFire.fire();
        }
    }

    void releaseSavepoint(Savepoint savepoint) {
        if (savepoint == null)
            throw new NullPointerException("Null savepoint");
        // savepoints to release (last to first)
        final LinkedList<TransactionPoint> toRelease = new LinkedList<TransactionPoint>();
        final Iterator<TransactionPoint> iter = this.txPoints.descendingIterator();
        TransactionPoint found = null;
        while (iter.hasNext() && found == null) {
            final TransactionPoint current = iter.next();
            if (current.getSavePoint() == savepoint)
                found = current;
            toRelease.add(current);
        }
        if (found == null)
            throw new NoSuchElementException("Savepoint not found :" + savepoint);
        final int index = this.txPoints.size() - toRelease.size();
        assert index > 0 : "First point is a transaction not a savepoint";
        while (this.txPoints.size() > index) {
            // link together next and previous
            final TransactionPoint removed = this.txPoints.remove(index);
            removed.setCommitted(true);
            if (index < this.txPoints.size()) {
                this.txPoints.get(index).setPrevious(this.txPoints.get(index - 1));
            }
            // remove the cache
            final SQLCache<List<?>, Object> cache = this.caches.remove(index);
            if (cache != null) {
                // an event might have cleared an item from 'cache' (and thus not from any previous
                // caches), now that it is released, it should also be cleared from the previous
                // cache
                this.caches.get(index - 1).clear();
                this.caches.get(index - 1).putAll(cache, true);
                this.caches.get(index - 1).addReleasedSavePoints(cache);
                cache.clear();
            }
        }
        assert assertPointsAndCacheCoherence(index);

        // as we're notified after JDBC, first replicate its state then notify listeners
        for (final TransactionPoint toFire : toRelease)
            toFire.fire();
    }

    public final boolean isChangeAllowed() {
        return this.changeAllowed;
    }

    final void setChangeAllowed(boolean b) {
        this.changeAllowed = b;
    }
}
