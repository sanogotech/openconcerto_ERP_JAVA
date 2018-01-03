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
 
 package org.openconcerto.sql.view.list.search;

import org.openconcerto.sql.model.graph.Link.Direction;
import org.openconcerto.sql.model.graph.Path;
import org.openconcerto.sql.model.graph.Step;
import org.openconcerto.sql.view.list.ITableModel;
import org.openconcerto.sql.view.list.ListAccess;
import org.openconcerto.sql.view.list.ListSQLLine;
import org.openconcerto.sql.view.list.UpdateQueue;
import org.openconcerto.sql.view.list.UpdateQueue.TaskType;
import org.openconcerto.sql.view.list.search.SearchOne.Mode;
import org.openconcerto.sql.view.search.SearchSpec;
import org.openconcerto.utils.IFutureTask;
import org.openconcerto.utils.SleepingQueue;
import org.openconcerto.utils.cc.IClosure;
import org.openconcerto.utils.cc.ITransformer;

import java.util.Collection;
import java.util.Deque;
import java.util.concurrent.FutureTask;

import javax.swing.SwingUtilities;

public final class SearchQueue extends SleepingQueue {

    static public interface SetStateRunnable extends Runnable {
    }

    /**
     * Whether the passed future performs a search.
     * 
     * @param f a task in this queue, can be <code>null</code>.
     * @return <code>true</code> if <code>f</code> searches.
     */
    public static boolean isSearch(final FutureTask<?> f) {
        final Runnable r = getRunnable(f);
        return r instanceof SearchRunnable && ((SearchRunnable) r).performsSearch();
    }

    public static Runnable getRunnable(final FutureTask<?> f) {
        if (f instanceof IFutureTask)
            return ((IFutureTask<?>) f).getRunnable();
        else
            return null;
    }

    /**
     * The last referent step of the passed path.
     * 
     * @param p a path.
     * @return the name of the field of the last referent step, <code>null</code> if it doesn't
     *         exist.
     */
    public static String getLastReferentField(final Path p) {
        final Step lastStep = p.length() == 0 ? null : p.getStep(-1);
        final boolean lastIsForeign = lastStep == null || lastStep.getDirection() == Direction.FOREIGN;
        return lastIsForeign ? null : lastStep.getSingleField().getName();
    }

    private final ITableModel model;
    // only accessed within this queue
    SearchSpec search;
    private final ListAccess listAccess;
    // thread-safe
    private final IClosure<Deque<FutureTask<?>>> cancelClosure;

    public SearchQueue(final ListAccess la) {
        super(SearchQueue.class.getName() + " on " + la.getModel());
        this.listAccess = la;
        this.model = la.getModel();
        this.search = null;
        this.cancelClosure = UpdateQueue.createCancelClosure(this, new ITransformer<FutureTask<?>, TaskType>() {
            @Override
            public TaskType transformChecked(FutureTask<?> input) {
                final Runnable r = getRunnable(input);
                if (r instanceof SearchRunnable)
                    return TaskType.COMPUTE;
                else if (r instanceof SetStateRunnable)
                    return TaskType.SET_STATE;
                else
                    return TaskType.USER;
            }
        });
    }

    public void orderChanged() {
        // don't search all if only order has changed
        this.put(new SearchRunnable(this) {

            @Override
            protected boolean performsSearch() {
                return false;
            }

            @Override
            public void run() {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        getAccess().setList(null, null);
                    }
                });
            }
        });
    }

    public void changeFullList(final int id, final ListSQLLine modifiedLine, final Collection<Integer> modifiedCols, final Mode mode) {
        final SearchOne oneSearchRunnable = new SearchOne(this, id, modifiedLine, modifiedCols, mode);
        this.put(oneSearchRunnable);
    }

    public void fullListChanged() {
        fullDataChange();
    }

    public void setSearch(final SearchSpec s) {
        this.setSearch(s, null);
    }

    public void setSearch(final SearchSpec s, final Runnable r) {
        // needs to be 2 different runnables, that way if the search is changed and then the table
        // is updated : the queue would naively contain setSearch, searchAll, searchAll and thus we
        // can cancel one searchAll. Whereas if the setSearch was contained in searchAll, we
        // couldn't cancel it.
        // use tasksDo() so that no other runnable can come between setSearch and searchAll.
        // Otherwise a runnable might the new search query but not the new filtered list.
        this.tasksDo(new IClosure<Deque<FutureTask<?>>>() {
            @Override
            public void executeChecked(Deque<FutureTask<?>> input) {
                put(new SetStateRunnable() {
                    @Override
                    public void run() {
                        SearchQueue.this.search = s;
                    }
                });
                fullDataChange();
                if (r != null) {
                    put(new Runnable() {
                        @Override
                        public void run() {
                            SwingUtilities.invokeLater(r);
                        }
                    });
                }
            }
        });
    }

    private void fullDataChange() {
        this.put(new SearchAll(this));
    }

    @Override
    protected void willPut(final FutureTask<?> qr) throws InterruptedException {
        if (getRunnable(qr) instanceof SearchAll) {
            // si on recherche tout, ne sert à rien de garder les recherches précédentes.
            this.tasksDo(this.cancelClosure);
        }
    }

    @Override
    public String toString() {
        return this.getClass().getName() + " for " + this.getModel();
    }

    final SearchSpec getSearch() {
        return this.search;
    }

    final ListAccess getAccess() {
        return this.listAccess;
    }

    public final ITableModel getModel() {
        return this.model;
    }
}
