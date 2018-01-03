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
import org.openconcerto.sql.model.SQLDataListener;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.SQLTable.ListenerAndConfig;
import org.openconcerto.sql.model.SQLTableEvent;
import org.openconcerto.sql.model.TransactionPoint;
import org.openconcerto.sql.model.TransactionPoint.State;
import org.openconcerto.utils.cache.CacheItem;
import org.openconcerto.utils.cache.CacheWatcher;

/**
 * A listener to invalidate cache results when their data is modified. Currently datum can either be
 * a SQLTable or a SQLRow.
 * 
 */
public class SQLCacheWatcher extends CacheWatcher<SQLData> {

    private final ListenerAndConfig listener;

    SQLCacheWatcher(final SQLData t) {
        super(t);
        this.listener = new ListenerAndConfig(t.createTableListener(new SQLDataListener() {
            @Override
            public void dataChanged(SQLTableEvent evt) {
                SQLCacheWatcher.this.dataChanged(evt);
            }
        }), null);
    }

    @Override
    protected boolean changedBy(CacheItem<?, ?, ? extends SQLData> val, Object event) {
        final SQLCache<?, ?> sqlCache = (SQLCache<?, ?>) val.getCache();
        final TransactionPoint cacheTxPoint = sqlCache.getTransactionPoint();

        final SQLTableEvent evt = (SQLTableEvent) event;
        final TransactionPoint evtTxPoint = evt.getTransactionPoint();
        final boolean transactionCommitted = evtTxPoint == null || evtTxPoint.getTransaction().getState() == State.COMMITTED;

        if (cacheTxPoint != null) {
            // even transactions at or above TRANSACTION_REPEATABLE_READ get up to date data
            // the first time they read it, so since we don't know which data has been read
            // (initially all items of the committed cache are copied to our cache), just
            // clear any committed cached data
            return transactionCommitted || sqlCache.changedBy(evtTxPoint);
        } else {
            return transactionCommitted;
        }
    }

    private final SQLTable getTable() {
        return this.getData().getTable();
    }

    @Override
    protected void startWatching() {
        this.getTable().addPremierTableModifiedListener(this.listener);
    }

    @Override
    protected void stopWatching() {
        this.getTable().removeTableModifiedListener(this.listener);
    }

}
