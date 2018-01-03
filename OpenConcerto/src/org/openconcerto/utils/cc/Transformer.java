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

import java.util.Map;

public abstract class Transformer<E, T> implements ITransformer<E, T>, IClosure<E>, org.apache.commons.collections.Transformer {

    private static final ITransformer<Object, Object> nopTransf = new ITransformer<Object, Object>() {
        @Override
        public Object transformChecked(Object input) {
            return input;
        }
    };

    @SuppressWarnings("unchecked")
    public static final <N> ITransformer<N, N> nopTransformer() {
        return (ITransformer<N, N>) nopTransf;
    }

    public static final <K, V> ITransformer<K, V> fromMap(final Map<K, V> map) {
        return new Transformer<K, V>() {
            @Override
            public V transformChecked(K input) {
                return map.get(input);
            }
        };
    }

    @SuppressWarnings("unchecked")
    public final Object transform(Object input) {
        return this.transformChecked((E) input);
    }

    public abstract T transformChecked(E input);

    public final void executeChecked(E input) {
        this.transformChecked(input);
    }
}
