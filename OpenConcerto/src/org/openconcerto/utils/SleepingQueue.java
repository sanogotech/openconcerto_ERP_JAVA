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
 
 package org.openconcerto.utils;

import org.openconcerto.utils.cc.IClosure;
import org.openconcerto.utils.cc.IPredicate;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Collection;
import java.util.Deque;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import net.jcip.annotations.GuardedBy;

/**
 * A queue that can be put to sleep. Submitted runnables are converted to FutureTask, that can later
 * be cancelled.
 * 
 * @author Sylvain
 */
public class SleepingQueue {

    public static enum RunningState {
        NEW, RUNNING, WILL_DIE, DYING, DEAD
    }

    /**
     * A task that can kill a queue.
     * 
     * @author Sylvain
     * 
     * @param <V> The result type returned by this FutureTask's <tt>get</tt> method
     */
    public static final class LethalFutureTask<V> extends FutureTask<V> {
        private final SleepingQueue q;

        public LethalFutureTask(final SleepingQueue q, final Callable<V> c) {
            super(c);
            this.q = q;
        }

        public final SleepingQueue getQueue() {
            return this.q;
        }

        @Override
        public String toString() {
            // don't includeCurrentTask as it could be us
            return this.getClass().getSimpleName() + " for " + this.getQueue().toString(false);
        }
    }

    private static final ScheduledThreadPoolExecutor exec;

    static {
        // daemon thread to allow the VM to exit
        exec = new ScheduledThreadPoolExecutor(2, new ThreadFactory("DieMonitor", true).setPriority(Thread.MIN_PRIORITY));
        // allow threads to die
        exec.setKeepAliveTime(30, TimeUnit.SECONDS);
        exec.allowCoreThreadTimeOut(true);
        exec.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
        exec.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);

        assert exec.getPoolSize() == 0 : "Wasting resources";
    }

    public static final ScheduledFuture<?> watchDying(final LethalFutureTask<?> lethalFuture) {
        return watchDying(lethalFuture, 1, 1, TimeUnit.MINUTES);
    }

    /**
     * Watch the passed future until it's done. When
     * {@link SleepingQueue#die(boolean, Runnable, Callable) killing} a queue, the currently running
     * task must first complete then the actual killing (represented by a {@link LethalFutureTask})
     * begins. This involves running methods and passed runnables which can all hang or throw an
     * exception. Therefore this method will periodically report on the status of the killing, and
     * report any exception that was thrown.
     * 
     * @param lethalFuture the killing to watch.
     * @param initialDelay the time to delay first execution.
     * @param delay the delay between the termination of one execution and the commencement of the
     *        next.
     * @param unit the time unit of the initialDelay and delay parameters.
     * @return a future representing the watching.
     */
    public static final ScheduledFuture<?> watchDying(final LethalFutureTask<?> lethalFuture, final int initialDelay, final int delay, final TimeUnit unit) {
        return watchDying(lethalFuture, initialDelay, delay, unit, null);
    }

    static final ScheduledFuture<?> watchDying(final LethalFutureTask<?> lethalFuture, final int initialDelay, final int delay, final TimeUnit unit,
            final IClosure<? super ExecutionException> exnHandler) {
        // don't use fixed rate as it might burden our threads and if we just checked the status
        // while being late, there's no need to check sooner the next time.
        final AtomicReference<Future<?>> f = new AtomicReference<Future<?>>();
        final ScheduledFuture<?> res = exec.scheduleWithFixedDelay(new Runnable() {

            // lethalFuture won't kill the queue, i.e. willDie threw an exception and forceDie was
            // false.
            private void wontKill(final RunningState runningState, final boolean isDone) {
                Log.get().fine("Our watched future won't kill the queue, current state : " + runningState + " " + lethalFuture);
                if (isDone)
                    cancel();
            }

            private void cancel() {
                assert lethalFuture.isDone();
                try {
                    lethalFuture.get();
                } catch (InterruptedException e) {
                    // either we were cancelled or the executor is shutting down (i.e. the VM is
                    // terminating)
                    Log.get().log(Level.FINER, "Interrupted while waiting on a finished future " + lethalFuture, e);
                } catch (ExecutionException e) {
                    if (exnHandler == null)
                        Log.get().log(Level.WARNING, "Threw an exception : " + lethalFuture, e);
                    else
                        exnHandler.executeChecked(e);
                }
                f.get().cancel(true);
            }

            @Override
            public void run() {
                final boolean isDone;
                final RunningState runningState;
                final FutureTask<?> beingRun;
                final SleepingQueue q = lethalFuture.getQueue();
                synchronized (q) {
                    runningState = q.getRunningState();
                    beingRun = q.getBeingRun();
                    isDone = lethalFuture.isDone();
                }
                final Level l = Level.INFO;
                if (runningState == RunningState.RUNNING) {
                    // willDie threw an exception but lethalFuture might not be completely done
                    // in that case, wait for the next execution
                    wontKill(runningState, isDone);
                } else if (runningState == RunningState.WILL_DIE) {
                    if (isDone) {
                        wontKill(runningState, isDone);
                    } else if (beingRun == lethalFuture) {
                        // in willDie() method or Runnable
                        Log.get().log(l, "Pre-death has not yet finished " + lethalFuture);
                    } else {
                        Log.get().log(l, "Death has not yet begun for " + lethalFuture + "\ncurrently running : " + beingRun);
                    }
                } else if (runningState == RunningState.DYING) {
                    assert beingRun == null || beingRun instanceof LethalFutureTask;
                    if (beingRun == null) {
                        // should be dead real soon
                        // just wait for the next execution
                        assert isDone;
                        Log.get().log(l, "Death was carried out but the thread is not yet terminated. Watching " + lethalFuture);
                    } else if (beingRun == lethalFuture) {
                        // in dying() method or Callable
                        Log.get().log(l, "Post-death has not yet finished " + lethalFuture);
                    } else {
                        assert isDone;
                        wontKill(runningState, isDone);
                    }
                } else if (runningState == RunningState.DEAD) {
                    // OK
                    Log.get().log(l, "Death was carried out and the thread is terminated but not necessarily by " + lethalFuture);
                    cancel();
                } else {
                    Log.get().warning("Illegal state " + runningState + " for " + lethalFuture);
                }
            }
        }, initialDelay, delay, unit);
        f.set(res);
        return res;
    }

    private final String name;

    @GuardedBy("this")
    private RunningState state;

    private final PropertyChangeSupport support;
    @GuardedBy("this")
    private FutureTask<?> beingRun;

    private final SingleThreadedExecutor tasksQueue;
    @GuardedBy("this")
    private boolean canceling;
    @GuardedBy("this")
    private IPredicate<FutureTask<?>> cancelPredicate;

    public SleepingQueue() {
        this(SleepingQueue.class.getName() + System.currentTimeMillis());
    }

    public SleepingQueue(String name) {
        super();
        this.name = name;

        this.state = RunningState.NEW;

        this.canceling = false;
        this.cancelPredicate = null;
        this.support = new PropertyChangeSupport(this);
        this.setBeingRun(null);

        this.tasksQueue = new SingleThreadedExecutor();
    }

    public final void start() {
        synchronized (this) {
            this.tasksQueue.start();
            this.setState(RunningState.RUNNING);
            started();
        }
    }

    /**
     * Start this queue only if not already started.
     * 
     * @return <code>true</code> if the queue was started.
     */
    public final boolean startIfNew() {
        // don't use getRunningState() which calls isAlive()
        synchronized (this) {
            final boolean starting = this.state == RunningState.NEW;
            if (starting)
                this.start();
            assert this.state.compareTo(RunningState.NEW) > 0;
            return starting;
        }
    }

    protected void started() {
    }

    protected synchronized final void setState(final RunningState s) {
        this.state = s;
    }

    public synchronized final RunningState getRunningState() {
        // an Error could have stopped our thread so can't rely on this.state
        if (this.state == RunningState.NEW || this.tasksQueue.isAlive())
            return this.state;
        else
            return RunningState.DEAD;
    }

    public final boolean currentlyInQueue() {
        return Thread.currentThread() == this.tasksQueue;
    }

    /**
     * Customize the thread used to execute the passed runnables. This implementation sets the
     * priority to the minimum.
     * 
     * @param thr the thread used by this queue.
     */
    protected void customizeThread(Thread thr) {
        thr.setPriority(Thread.MIN_PRIORITY);
    }

    protected final <T> FutureTask<T> newTaskFor(final Runnable task) {
        return this.newTaskFor(task, null);
    }

    protected <T> FutureTask<T> newTaskFor(final Runnable task, T value) {
        return new IFutureTask<T>(task, value, " for {" + this.name + "}");
    }

    public final FutureTask<?> put(Runnable workRunnable) {
        // otherwise if passing a FutureTask, it will itself be wrapped in another FutureTask. The
        // outer FutureTask will call the inner one's run(), which just record any exception. So the
        // outer one's get() won't throw it and the exception will effectively be swallowed.
        final FutureTask<?> t;
        if (workRunnable instanceof FutureTask) {
            t = ((FutureTask<?>) workRunnable);
        } else {
            t = this.newTaskFor(workRunnable);
        }
        return this.execute(t);
    }

    public final <F extends FutureTask<?>> F execute(F t) {
        if (this.shallAdd(t)) {
            this.add(t);
            return t;
        } else
            return null;
    }

    private void add(FutureTask<?> t) {
        // no need to synchronize, if die() is called after our test, t won't be executed anyway
        if (this.dieCalled())
            throw new IllegalStateException("Already dead, cannot exec " + t);

        this.tasksQueue.put(t);
    }

    private final boolean shallAdd(FutureTask<?> runnable) {
        if (runnable == null)
            throw new NullPointerException("null runnable");
        try {
            this.willPut(runnable);
            return true;
        } catch (InterruptedException e) {
            // si on interrompt, ne pas ajouter
            return false;
        }
    }

    /**
     * Give subclass the ability to reject runnables.
     * 
     * @param r the runnable that is being added.
     * @throws InterruptedException if r should not be added to this queue.
     */
    protected void willPut(FutureTask<?> r) throws InterruptedException {
    }

    /**
     * An exception was thrown by a task. This implementation merely
     * {@link Exception#printStackTrace()}.
     * 
     * @param exn the exception thrown.
     */
    protected void exceptionThrown(final ExecutionException exn) {
        exn.printStackTrace();
    }

    /**
     * Cancel all queued tasks and the current task.
     */
    protected final void cancel() {
        this.cancel(null);
    }

    /**
     * Cancel only tasks for which pred is <code>true</code>.
     * 
     * @param pred a predicate to know which tasks to cancel.
     */
    protected final void cancel(final IPredicate<FutureTask<?>> pred) {
        this.tasksDo(new IClosure<Collection<FutureTask<?>>>() {
            @Override
            public void executeChecked(Collection<FutureTask<?>> tasks) {
                cancel(pred, tasks);
            }
        });
    }

    private final void cancel(IPredicate<FutureTask<?>> pred, Collection<FutureTask<?>> tasks) {
        try {
            synchronized (this) {
                this.canceling = true;
                this.cancelPredicate = pred;
                this.cancelCheck(this.getBeingRun());
            }

            for (final FutureTask<?> t : tasks) {
                this.cancelCheck(t);
            }
        } finally {
            synchronized (this) {
                this.canceling = false;
                // allow the predicate to be gc'd
                this.cancelPredicate = null;
            }
        }
    }

    public final void tasksDo(IClosure<? super Deque<FutureTask<?>>> c) {
        this.tasksQueue.itemsDo(c);
    }

    private void cancelCheck(FutureTask<?> t) {
        if (t != null)
            synchronized (this) {
                if (this.canceling && (this.cancelPredicate == null || this.cancelPredicate.evaluateChecked(t)))
                    t.cancel(true);
            }
    }

    private void setBeingRun(final FutureTask<?> beingRun) {
        final Future<?> old;
        synchronized (this) {
            old = this.beingRun;
            this.beingRun = beingRun;
        }
        this.support.firePropertyChange("beingRun", old, beingRun);
    }

    public final synchronized FutureTask<?> getBeingRun() {
        return this.beingRun;
    }

    public boolean isSleeping() {
        return this.tasksQueue.isSleeping();
    }

    public boolean setSleeping(boolean sleeping) {
        final boolean res = this.tasksQueue.setSleeping(sleeping);
        if (res) {
            this.support.firePropertyChange("sleeping", null, sleeping);
        }
        return res;
    }

    /**
     * Stops this queue. Once this method returns, it is guaranteed that no other task will be taken
     * from the queue to be started, and that this queue will die. But the already executing task
     * will complete unless it checks for interrupt.
     * 
     * @return the future killing.
     */
    public final LethalFutureTask<?> die() {
        return this.die(true, null, null);
    }

    /**
     * Stops this queue. All tasks in the queue, including the {@link #getBeingRun() currently
     * running}, will be {@link Future#cancel(boolean) cancelled}. The currently running task will
     * thus complete unless it checks for interrupt. Once the returned future completes successfully
     * then no task is executing ( {@link #isDead()} will happen sometimes later, the time for the
     * thread to terminate). If the returned future throws an exception because of the passed
     * runnables or of {@link #willDie()} or {@link #dying()}, one can check with
     * {@link #dieCalled()} to see if the queue is dying.
     * <p>
     * This method tries to limit the cases where the returned Future will not get executed : it
     * checks that this was {@link #start() started} and is not already {@link RunningState#DYING}
     * or {@link RunningState#DEAD}. It also doesn't allow {@link RunningState#WILL_DIE} as it could
     * cancel the previously passed runnables or never run the passed runnables. But even with these
     * restrictions a number of things can prevent the result from getting executed : the
     * {@link #getBeingRun() currently running} task hangs indefinitely, it throws an {@link Error}
     * ; the passed runnables hang indefinitely.
     * </p>
     * 
     * @param force <code>true</code> if this is guaranteed to die (even if <code>willDie</code> or
     *        {@link #willDie()} throw an exception).
     * @param willDie the last actions to take before killing this queue.
     * @param dying the last actions to take before this queue is dead.
     * @return the future killing, which will return <code>dying</code> result.
     * @throws IllegalStateException if the state isn't {@link RunningState#RUNNING}.
     * @see #dieCalled()
     */
    public final <V> LethalFutureTask<V> die(final boolean force, final Runnable willDie, final Callable<V> dying) throws IllegalStateException {
        synchronized (this) {
            final RunningState state = this.getRunningState();
            if (state == RunningState.NEW)
                throw new IllegalStateException("Not started");
            if (state.compareTo(RunningState.RUNNING) > 0)
                throw new IllegalStateException("die() already called or thread was killed by an Error : " + state);
            assert state == RunningState.RUNNING;
            this.setState(RunningState.WILL_DIE);
        }
        // reset sleeping to original value if die not effective
        final AtomicBoolean resetSleeping = new AtomicBoolean(false);
        final LethalFutureTask<V> res = new LethalFutureTask<V>(this, new Callable<V>() {
            @Override
            public V call() throws Exception {
                Exception willDieExn = null;
                try {
                    willDie();
                    if (willDie != null) {
                        willDie.run();
                        // handle Future like runnable, i.e. check right away for exception
                        if (willDie instanceof Future) {
                            final Future<?> f = (Future<?>) willDie;
                            assert f.isDone() : "Ran but not done: " + f;
                            try {
                                f.get();
                            } catch (ExecutionException e) {
                                throw (Exception) e.getCause();
                            }
                        }
                    }
                } catch (Exception e) {
                    if (!force) {
                        setState(RunningState.RUNNING);
                        throw e;
                    } else {
                        willDieExn = e;
                    }
                }
                try {
                    // don't interrupt ourselves
                    SleepingQueue.this.tasksQueue.die(false);
                    assert SleepingQueue.this.tasksQueue.isDying();
                    setState(RunningState.DYING);
                    // since there's already been an exception, throw it as soon as possible
                    // also dying() might itself throw an exception for the same reason or we now
                    // have 2 exceptions to throw
                    if (willDieExn != null)
                        throw willDieExn;
                    dying();
                    final V res;
                    if (dying != null)
                        res = dying.call();
                    else
                        res = null;

                    return res;
                } finally {
                    // if die is effective, this won't have any consequences
                    if (resetSleeping.get())
                        SleepingQueue.this.tasksQueue.setSleeping(true);
                }
            }
        });
        // die as soon as possible not after all currently queued tasks
        this.tasksQueue.itemsDo(new IClosure<Deque<FutureTask<?>>>() {
            @Override
            public void executeChecked(Deque<FutureTask<?>> input) {
                // since we cancel the current task, we might as well remove all of them since they
                // might depend on the cancelled one
                // cancel removed tasks so that callers of get() don't wait forever
                for (final FutureTask<?> ft : input) {
                    // by definition tasks in the queue aren't executing, so interrupt parameter is
                    // useless. On the other hand cancel() might return false if already cancelled.
                    ft.cancel(false);
                }
                input.clear();

                input.addFirst(res);
                // die as soon as possible, even if there's a long task already running
                final FutureTask<?> beingRun = getBeingRun();
                // since we hold the lock on items
                assert beingRun != res : "beingRun: " + beingRun + " ; res: " + res;
                if (beingRun != null)
                    beingRun.cancel(true);
            }
        });
        // force execution of our task
        resetSleeping.set(this.setSleeping(false));
        return res;
    }

    protected void willDie() {
        // nothing by default
    }

    protected void dying() throws Exception {
        // nothing by default
    }

    /**
     * Whether this will die. If this method returns <code>true</code>, it is guaranteed that no
     * other task will be taken from the queue to be started. Note: this method doesn't return
     * <code>true</code> right after {@link #die()} as the method is asynchronous and if
     * {@link #willDie()} fails it may not die at all ; as explained in its comment you may use its
     * returned future to wait for the killing.
     * 
     * @return <code>true</code> if this queue will not execute any more tasks (but it may hang
     *         indefinitely if the dying runnable blocks).
     * @see #isDead()
     */
    public final boolean dieCalled() {
        return this.tasksQueue.dieCalled();
    }

    /**
     * Whether this queue is dead, i.e. if die() has been called and all tasks have completed.
     * 
     * @return <code>true</code> if this queue will not execute any more tasks and isn't executing
     *         any.
     * @see #die()
     */
    public final boolean isDead() {
        return this.tasksQueue.isDead();
    }

    /**
     * Allow to wait for the thread to end. Once this method returns {@link #getRunningState()} will
     * always return {@link RunningState#DEAD}. Useful since the future from
     * {@link #die(boolean, Runnable, Callable)} returns when all tasks are finished but the
     * {@link #getRunningState()} is still {@link RunningState#DYING} since the Thread takes a
     * little time to die.
     * 
     * @throws InterruptedException if interrupted while waiting.
     * @see Thread#join()
     */
    public final void join() throws InterruptedException {
        this.tasksQueue.join();
    }

    public final void join(long millis, int nanos) throws InterruptedException {
        this.tasksQueue.join(millis, nanos);
    }

    public void addPropertyChangeListener(PropertyChangeListener l) {
        this.support.addPropertyChangeListener(l);
    }

    public void rmPropertyChangeListener(PropertyChangeListener l) {
        this.support.removePropertyChangeListener(l);
    }

    private final class SingleThreadedExecutor extends DropperQueue<FutureTask<?>> {
        private SingleThreadedExecutor() {
            super(SleepingQueue.this.name + System.currentTimeMillis());
            customizeThread(this);
        }

        @Override
        protected void process(FutureTask<?> task) {
            if (!task.isDone()) {
                /*
                 * From ThreadPoolExecutor : Track execution state to ensure that afterExecute is
                 * called only if task completed or threw exception. Otherwise, the caught runtime
                 * exception will have been thrown by afterExecute itself, in which case we don't
                 * want to call it again.
                 */
                boolean ran = false;
                beforeExecute(task);
                try {
                    task.run();
                    ran = true;
                    afterExecute(task, null);
                } catch (RuntimeException ex) {
                    if (!ran)
                        afterExecute(task, ex);
                    // don't throw ex, afterExecute() can do whatever needs to be done (like killing
                    // this queue)
                }
            }
        }

        protected void beforeExecute(final FutureTask<?> f) {
            cancelCheck(f);
            setBeingRun(f);
        }

        protected void afterExecute(final FutureTask<?> f, final Throwable t) {
            setBeingRun(null);

            try {
                f.get();
            } catch (CancellationException e) {
                // don't care
            } catch (InterruptedException e) {
                // f was interrupted : e.g. we're dying or f was cancelled
            } catch (ExecutionException e) {
                // f.run() raised an exception
                exceptionThrown(e);
            }
        }
    }

    @Override
    public String toString() {
        return this.toString(true);
    }

    public String toString(final boolean includeCurrentTask) {
        return super.toString() + " Queue: " + this.tasksQueue + (includeCurrentTask ? " run:" + this.getBeingRun() : "");
    }

}
