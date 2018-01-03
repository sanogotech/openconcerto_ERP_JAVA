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

import org.openconcerto.utils.CompareUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.ArrayList;
import java.util.List;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;

/**
 * The start of a transaction, or a {@link Savepoint}.
 * 
 * @author Sylvain
 * @see #addListener(TransactionListener)
 */
@ThreadSafe
public class TransactionPoint {

    static public enum State {
        ACTIVE(null), COMMITTED(true), ABORTED(false);

        private final Boolean committed;

        private State(final Boolean b) {
            this.committed = b;
        }

        public final Boolean getCommitted() {
            return this.committed;
        }

        public final boolean isActive() {
            return this.committed == null;
        }
    }

    @GuardedBy("this")
    private State state;
    @GuardedBy("this")
    private TransactionPoint previous;
    @GuardedBy("this")
    private TransactionPoint transaction;
    private final Connection conn;
    private final Savepoint savepoint;
    private final boolean namedSavePoint;
    private final String savePointID;

    private final List<TransactionListener> listeners;

    public TransactionPoint(Connection conn) throws SQLException {
        this(conn, null, false);
    }

    public TransactionPoint(Connection conn, Savepoint savepoint, boolean namedSavePoint) throws SQLException {
        super();
        this.state = State.ACTIVE;
        this.previous = null;
        this.transaction = null;
        if (conn == null)
            throw new NullPointerException("Null connection");
        this.conn = conn;
        this.savepoint = savepoint;
        this.namedSavePoint = namedSavePoint;
        if (savepoint == null)
            this.savePointID = null;
        else if (namedSavePoint)
            this.savePointID = savepoint.getSavepointName();
        else
            this.savePointID = String.valueOf(savepoint.getSavepointId());
        this.listeners = new ArrayList<TransactionListener>();
    }

    /**
     * How this transaction point ended.
     * 
     * @return <code>null</code> if it's ongoing, <code>true</code> if it was committed,
     *         <code>false</code> if aborted.
     * @deprecated use {@link #getState()}
     */
    public final Boolean getCommitted() {
        return this.getState().getCommitted();
    }

    /**
     * How this transaction point ended.
     * 
     * @return <code>true</code> if it was committed, <code>false</code> if aborted.
     * @throws IllegalStateException if still {@link State#ACTIVE}.
     * @see #getState()
     */
    public final boolean wasCommitted() throws IllegalStateException {
        final State state = this.getState();
        if (state.isActive())
            throw new IllegalStateException("Still active");
        return state.getCommitted();
    }

    public synchronized final State getState() {
        return this.state;
    }

    synchronized void setCommitted(boolean committed) {
        checkActive();
        this.state = committed ? State.COMMITTED : State.ABORTED;
        assert !this.isActive();
    }

    public boolean isActive() {
        return this.getState().isActive();
    }

    synchronized void setPrevious(TransactionPoint previous) {
        assert (previous == null) == (this.getSavePoint() == null);
        final TransactionPoint newTx = previous == null ? this : previous.getTransaction();
        if (this.transaction != null && newTx != this.transaction)
            throw new IllegalArgumentException("Previous is in a different transaction");
        this.previous = previous;
        this.transaction = newTx;
    }

    public final synchronized TransactionPoint getPrevious() {
        return this.previous;
    }

    /**
     * The transaction containing this point.
     * 
     * @return the transaction, i.e. <code>this</code> if {@link #getSavePoint()} is
     *         <code>null</code>.
     */
    public synchronized final TransactionPoint getTransaction() {
        return this.transaction;
    }

    final Connection getConn() {
        return this.conn;
    }

    /**
     * The save point.
     * 
     * @return the save point, <code>null</code> for the start of a transaction.
     */
    public final Savepoint getSavePoint() {
        return this.savepoint;
    }

    public final boolean isNamedSavePoint() {
        return this.namedSavePoint;
    }

    /**
     * The name or ID of the save point.
     * 
     * @return <code>null</code> if {@link #getSavePoint()} is, {@link Savepoint#getSavepointName()}
     *         if {@link #isNamedSavePoint()} else {@link Savepoint#getSavepointId()}.
     */
    protected final String getSavePointID() {
        return this.savePointID;
    }

    /**
     * To be notified when this point is either committed or aborted.
     * 
     * @param l a listener.
     */
    public synchronized final void addListener(TransactionListener l) {
        checkActive();
        this.listeners.add(l);
    }

    private void checkActive() {
        if (!this.isActive())
            throw new IllegalStateException("Transaction point inactive");
    }

    /**
     * Remove listener, *not* to be called in the callback. Indeed, since a transaction point is
     * either committed or aborted once and only once, the listeners are cleared automatically.
     * 
     * @param l a listener.
     */
    public synchronized final void removeListener(TransactionListener l) {
        checkActive();
        this.listeners.remove(l);
    }

    final void fire() {
        final List<TransactionListener> ls;
        synchronized (this) {
            assert !isActive() : "Fire while still active";
            ls = new ArrayList<TransactionListener>(this.listeners);
            this.listeners.clear();
        }
        for (final TransactionListener l : ls) {
            l.transactionEnded(this);
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.conn == null) ? 0 : this.conn.hashCode());
        result = prime * result + (this.namedSavePoint ? 1231 : 1237);
        result = prime * result + ((this.savePointID == null) ? 0 : this.savePointID.hashCode());
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
        final TransactionPoint other = (TransactionPoint) obj;
        return this.conn == other.conn && this.namedSavePoint == other.namedSavePoint && CompareUtils.equals(this.savePointID, other.savePointID);
    }

    @Override
    public String toString() {
        final String id = this.getSavePointID();
        final String sp = id != null ? " / savepoint " + id : "";
        return this.getClass().getSimpleName() + " " + this.getState() + " for transaction " + System.identityHashCode(this.getTransaction()) + sp;
    }
}
