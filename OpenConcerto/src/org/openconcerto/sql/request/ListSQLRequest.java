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

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.FieldExpander;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;

import java.util.List;

import net.jcip.annotations.ThreadSafe;

@ThreadSafe
public class ListSQLRequest extends FilteredFillSQLRequest {

    private static final FieldExpander getExpander(final FieldExpander showAs) {
        final FieldExpander res;
        if (showAs != null) {
            res = showAs;
        } else {
            final Configuration conf = Configuration.getInstance();
            if (conf == null) {
                res = FieldExpander.getEmpty();
            } else {
                res = conf.getShowAs();
            }
        }
        return res;
    }

    public ListSQLRequest(SQLTable table, List<String> fieldss) {
        this(table, fieldss, null);
    }

    public ListSQLRequest(SQLTable table, List<String> fieldss, Where where) {
        this(table, fieldss, where, null);
    }

    public ListSQLRequest(SQLTable table, List<String> fieldss, Where where, final FieldExpander showAs) {
        this(computeGraph(table, fieldss, getExpander(showAs)), where);
    }

    public ListSQLRequest(final SQLRowValues graph, final Where where) {
        super(graph, where);
        if (!this.getPrimaryTable().isOrdered())
            throw new IllegalArgumentException(this.getPrimaryTable() + " is not ordered.");
    }

    protected ListSQLRequest(ListSQLRequest req, final boolean freeze) {
        super(req, freeze);
    }

    // wasFrozen() : our showAs might change but our fetcher won't, MAYBE remove final modifier and
    // clone showAs

    @Override
    public ListSQLRequest toUnmodifiable() {
        return this.toUnmodifiableP(ListSQLRequest.class);
    }

    // can't implement Cloneable since the contract is to return an object of the same class. But
    // this would either prevent the use of anonymous classes if we call clone(false), or require a
    // call to super.clone() and less final fields.

    @Override
    protected ListSQLRequest clone(boolean forFreeze) {
        return new ListSQLRequest(this, forFreeze);
    }

    // MAYBE use changeGraphToFetch()
    @Override
    protected final void customizeToFetch(SQLRowValues graphToFetch) {
        super.customizeToFetch(graphToFetch);
        addField(graphToFetch, getPrimaryTable().getCreationDateField());
        addField(graphToFetch, getPrimaryTable().getCreationUserField());
        addField(graphToFetch, getPrimaryTable().getModifDateField());
        addField(graphToFetch, getPrimaryTable().getModifUserField());
        addField(graphToFetch, getPrimaryTable().getFieldRaw(SQLComponent.READ_ONLY_FIELD));
        addField(graphToFetch, getPrimaryTable().getFieldRaw(SQLComponent.READ_ONLY_USER_FIELD));
    }

    private void addField(SQLRowValues graphToFetch, final SQLField f) {
        if (f != null && !graphToFetch.getFields().contains(f.getName())) {
            if (f.isKey())
                graphToFetch.putRowValues(f.getName()).putNulls("NOM", "PRENOM");
            else
                graphToFetch.put(f.getName(), null);
        }
    }
}
