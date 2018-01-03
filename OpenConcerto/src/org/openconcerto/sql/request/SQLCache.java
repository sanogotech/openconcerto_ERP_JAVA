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

import org.openconcerto.sql.model.SQLData;
import org.openconcerto.sql.model.TransactionPoint;
import org.openconcerto.sql.model.TransactionPoint.State;
import org.openconcerto.utils.cache.CacheWatcherFactory;
import org.openconcerto.utils.cache.ICache;
import org.openconcerto.utils.cache.ICacheSupport;

import java.util.ArrayList;
import java.util.List;

/**
 * To keep result related to some SQLTables in cache. The results will be automatically invalidated
 * after some period of time or when a table is modified.
 * 
 * <img src="doc-files/cache.png"/>
 * 
 * @author Sylvain CUAZ
 * @param <K> type of keys
 * @param <V> type of value
 */
public class SQLCache<K, V> extends ICache<K, V, SQLData> {

    private final TransactionPoint txPoint;
    private final List<TransactionPoint> releasedTxPoints;

    public SQLCache() {
        this(60);
    }

    public SQLCache(int delay) {
        this(delay, -1);
    }

    public SQLCache(int delay, int size) {
        this(delay, size, null);
    }

    /**
     * Creates a cache with the given parameters.
     * 
     * @param delay the delay in seconds before a key is cleared.
     * @param size the maximum size of the cache, negative means no limit.
     * @param name name of this cache and associated thread.
     * @throws IllegalArgumentException if size is 0.
     */
    public SQLCache(int delay, int size, final String name) {
        this(null, delay, size, name, null);
    }

    public SQLCache(final ICacheSupport<SQLData> supp, int delay, int size, final String name, final TransactionPoint txPoint) {
        super(supp, delay, size, name);
        this.txPoint = txPoint;
        this.releasedTxPoints = new ArrayList<TransactionPoint>();
    }

    @Override
    protected ICacheSupport<SQLData> createSupp(String name) {
        final ICacheSupport<SQLData> res = super.createSupp(name);
        res.setWatcherFactory(new CacheWatcherFactory<SQLData>() {
            @Override
            public SQLCacheWatcher createWatcher(SQLData o) {
                return new SQLCacheWatcher(o);
            }
        });
        return res;
    }

    public final TransactionPoint getTransactionPoint() {
        return this.txPoint;
    }

    private final void addReleasedSavePoint(final TransactionPoint txPoint) {
        if (txPoint.getSavePoint() == null || txPoint.getState() != State.COMMITTED)
            throw new IllegalArgumentException("Not a released savepoint : " + txPoint);
        if (txPoint.getTransaction() != this.getTransactionPoint().getTransaction())
            throw new IllegalArgumentException("Not in same transaction : " + txPoint);
        this.releasedTxPoints.add(txPoint);
    }

    public final void addReleasedSavePoints(final SQLCache<K, V> cache) {
        if (this.getSupp() != cache.getSupp())
            throw new IllegalArgumentException("Not same support : " + cache);
        for (final TransactionPoint tp : cache.releasedTxPoints)
            this.addReleasedSavePoint(tp);
        this.addReleasedSavePoint(cache.getTransactionPoint());
    }

    public final boolean changedBy(TransactionPoint evtTxPoint) {
        assert this.getTransactionPoint() != null : "See SQLCacheWatcher";
        if (evtTxPoint.isActive())
            return this.getTransactionPoint() == evtTxPoint;
        // since fires are done outside transactions (to free DB resources) the transactions are
        // already committed
        else if (evtTxPoint.wasCommitted())
            return this.releasedTxPoints.contains(evtTxPoint);
        else
            // a cache is never changed by a roll back, it is just discarded
            return false;
    }
}
