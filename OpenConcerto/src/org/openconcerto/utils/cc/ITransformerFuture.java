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

import java.util.concurrent.Future;

/**
 * A {@link Future} that is a {@link ITransformerExn}. Successful execution of the
 * <tt>transformChecked</tt> method causes completion of the <tt>Future</tt> and allows access to
 * its results.
 * 
 * @see TransformerFuture
 * @param <E> The parameter type for the <tt>transformChecked</tt> method
 * @param <T> The result type returned by the <tt>transformChecked</tt> method and this Future's
 *        <tt>get</tt> method
 * @param <X> The type of exception thrown by the <tt>transformChecked</tt> method
 */
public interface ITransformerFuture<E, T, X extends Exception> extends Future<T>, ITransformerExn<E, T, X> {

}
