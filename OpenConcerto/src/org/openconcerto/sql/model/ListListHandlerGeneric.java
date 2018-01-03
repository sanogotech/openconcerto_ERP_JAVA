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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.ArrayListHandler;

import net.jcip.annotations.Immutable;

/**
 * <code>ResultSetHandler</code> implementation that converts a <code>ResultSet</code> into a
 * {@link List} of lists.
 * 
 * @see ArrayListHandler
 */
@Immutable
public class ListListHandlerGeneric<A> implements ResultSetHandler {

    public static ListListHandlerGeneric<Object> create(final List<Class<?>> classes) {
        return create(Object.class, classes);
    }

    public static <A> ListListHandlerGeneric<A> create(final Class<A> arrayClass, final int colCount) {
        return create(arrayClass, Collections.<Class<? extends A>> nCopies(colCount, arrayClass));
    }

    // syntactic sugar, MAYBE cache instances
    public static <A> ListListHandlerGeneric<A> create(final Class<A> arrayClass, final List<Class<? extends A>> classes) {
        return new ListListHandlerGeneric<A>(arrayClass, classes);
    }

    private final Class<A> arrayClass;
    private final List<Class<? extends A>> classes;

    public ListListHandlerGeneric(final Class<A> arrayClass, final List<Class<? extends A>> classes) {
        if (arrayClass == null)
            throw new NullPointerException("Missing array component class");
        this.arrayClass = arrayClass;
        this.classes = classes == null ? null : new ArrayList<Class<? extends A>>(classes);
    }

    @Override
    public final List<List<A>> handle(ResultSet rs) throws SQLException {
        final int cols = this.classes == null ? rs.getMetaData().getColumnCount() : this.classes.size();
        final List<List<A>> result = new ArrayList<List<A>>();
        while (rs.next()) {
            final List<A> array = new ArrayList<A>();
            for (int i = 0; i < cols; i++) {
                if (this.classes == null)
                    array.add(this.arrayClass.cast(rs.getObject(i + 1)));
                else
                    array.add(SQLResultSet.getValue(rs, this.classes.get(i), i + 1));
            }
            result.add(array);
        }
        return result;
    }
}
