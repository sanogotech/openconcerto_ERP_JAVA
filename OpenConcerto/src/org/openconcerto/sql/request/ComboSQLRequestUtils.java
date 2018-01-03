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

import org.openconcerto.sql.TransfFieldExpander;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.element.SQLElementDirectory;
import org.openconcerto.sql.model.FieldPath;
import org.openconcerto.sql.model.IFieldPath;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.graph.Link.Direction;
import org.openconcerto.sql.model.graph.Path;
import org.openconcerto.utils.Tuple2;
import org.openconcerto.utils.cc.ITransformer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class ComboSQLRequestUtils {

    // return the foreign row in field and its fields
    private static Tuple2<SQLRowValues, List<FieldPath>> expandOnce(final SQLRowValues vals, final IFieldPath field) {
        assert vals.getFields().contains(field.getFieldName());

        final Object foreignObj = vals.getObject(field.getFieldName());
        if (!(foreignObj instanceof SQLRowValues))
            return Tuple2.create(null, Collections.<FieldPath> emptyList());

        final SQLRowValues foreign = (SQLRowValues) foreignObj;
        // foreign since we used getObject()
        final Path newPath = field.getPath().add(field.getField(), Direction.FOREIGN);
        final List<FieldPath> res = new ArrayList<FieldPath>();
        for (final String f : foreign.getFields())
            res.add(new FieldPath(newPath, f));

        return Tuple2.create(foreign, res);
    }

    // recursively walk vals to collect all foreign fields
    public static final List<FieldPath> expand(final SQLRowValues vals, final IFieldPath field) {
        assert vals.getTable() == field.getTable();
        final List<FieldPath> fields = new ArrayList<FieldPath>();

        if (!field.getTable().getForeignKeys().contains(field.getField())) {
            // si ce n'est pas une clef alors il n'y a pas à l'expandre
            fields.add(field.getFieldPath());
        } else {
            final Tuple2<SQLRowValues, List<FieldPath>> tmp = expandOnce(vals, field);
            for (final FieldPath f : tmp.get1()) {
                fields.addAll(expand(tmp.get0(), f));
            }
        }

        return fields;
    }

    /**
     * Group fields of the passed row by the parent foreign key. For each item of the result, the
     * path to the ancestor is also included, e.g. [ LOCAL, LOCAL.ID_BATIMENT,
     * LOCAL.ID_BATIMENT.ID_SITE, LOCAL.ID_BATIMENT.ID_SITE.ID_ETABLISSEMENT ].
     * 
     * @param vals the row to go through.
     * @param dir how to find the parents.
     * @return the complete expansion, e.g. [ [LOCAL.DESIGNATION], [BAT.DES], [SITE.DES,
     *         ADRESSE.CP], [ETABLISSEMENT.DES] ].
     */
    public static final List<Tuple2<Path, List<FieldPath>>> expandGroupBy(final SQLRowValues vals, final SQLElementDirectory dir) {
        return expandGroupBy(new Path(vals.getTable()), vals, dir);
    }

    private static final List<Tuple2<Path, List<FieldPath>>> expandGroupBy(final Path fieldsPath, final SQLRowValues vals, final SQLElementDirectory dir) {
        assert fieldsPath.getLast() == vals.getTable();
        if (vals.size() == 0)
            return Collections.emptyList();

        final SQLElement element = dir.getElement(fieldsPath.getLast());
        // treat missing element as the root
        final SQLField parentFF = element == null ? null : element.getParentForeignField();

        final List<Tuple2<Path, List<FieldPath>>> res = new ArrayList<Tuple2<Path, List<FieldPath>>>();
        final List<FieldPath> currentL = new ArrayList<FieldPath>();
        res.add(Tuple2.create(fieldsPath, currentL));
        SQLRowValues parent = null;
        for (final String f : vals.getFields()) {
            if (parentFF != null && f.equals(parentFF.getName())) {
                final Object val = vals.getObject(f);
                parent = val instanceof SQLRowValues ? (SQLRowValues) val : null;
            } else {
                currentL.addAll(expand(vals, new FieldPath(fieldsPath, f)));
            }
        }
        if (parent != null)
            res.addAll(expandGroupBy(fieldsPath.add(parentFF, Direction.FOREIGN), parent, dir));

        return res;
    }

    public static final TransfFieldExpander getShowAs(final SQLElementDirectory dir) {
        final TransfFieldExpander exp = new TransfFieldExpander(new ITransformer<SQLField, List<SQLField>>() {
            @Override
            public List<SQLField> transformChecked(SQLField fk) {
                final SQLTable foreignTable = fk.getDBSystemRoot().getGraph().getForeignTable(fk);
                return dir.getElement(foreignTable).getComboRequest().getFields();
            }
        });
        return exp;
    }
}
