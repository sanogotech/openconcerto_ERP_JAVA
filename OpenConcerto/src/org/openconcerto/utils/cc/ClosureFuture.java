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

public class ClosureFuture<E, X extends Exception> extends TransformerFuture<E, Object, X> implements IClosureFuture<E, X> {

    public ClosureFuture(final IExnClosure<? super E, ? extends X> cl) {
        super(new ITransformerExn<E, Object, X>() {
            @Override
            public Object transformChecked(E input) throws X {
                cl.executeChecked(input);
                return null;
            }
        });
    }

    @Override
    public void executeChecked(E input) throws X {
        this.transformChecked(input);
    }
}
