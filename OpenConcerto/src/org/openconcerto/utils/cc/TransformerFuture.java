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
 
 package org.openconcerto.utils.cc;

import org.openconcerto.utils.Value;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

public class TransformerFuture<E, T, X extends Exception> implements ITransformerFuture<E, T, X> {

    private final AtomicReference<Value<E>> input;
    private final FutureTask<T> f;

    public TransformerFuture(final ITransformerExn<? super E, ? extends T, ? extends X> cl) {
        super();
        this.f = new FutureTask<T>(new Callable<T>() {
            @Override
            public T call() throws X {
                return cl.transformChecked(TransformerFuture.this.input.get().getValue());
            }
        });
        this.input = new AtomicReference<Value<E>>(Value.<E> getNone());
    }

    @Override
    public final boolean cancel(boolean mayInterruptIfRunning) {
        return this.f.cancel(mayInterruptIfRunning);
    }

    @Override
    public final boolean isCancelled() {
        return this.f.isCancelled();
    }

    @Override
    public final boolean isDone() {
        return this.f.isDone();
    }

    @Override
    public final T get() throws InterruptedException, ExecutionException {
        return this.f.get();
    }

    @Override
    public final T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return this.f.get(timeout, unit);
    }

    @Override
    public final T transformChecked(E input) throws X {
        if (!this.input.compareAndSet(Value.<E> getNone(), Value.getSome(input)))
            throw new IllegalStateException("Already run");
        this.f.run();
        assert this.f.isDone();
        try {
            return this.f.get();
        } catch (InterruptedException e) {
            // shouldn't happen since f is done
            throw new IllegalStateException(e);
        } catch (ExecutionException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof Error) {
                throw (Error) cause;
            } else if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else {
                // our Callable throws X
                @SuppressWarnings("unchecked")
                final X casted = (X) cause;
                throw casted;
            }
        }
    }
}
