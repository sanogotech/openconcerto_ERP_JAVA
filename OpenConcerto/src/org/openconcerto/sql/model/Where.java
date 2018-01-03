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

import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.StringUtils;
import org.openconcerto.utils.cc.ITransformer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.collections.functors.InstanceofPredicate;

import net.jcip.annotations.Immutable;

/**
 * Une clause WHERE dans une requete SQL. Une clause peut être facilement combinée avec d'autre,
 * exemple : prenomPasVide.and(pasIndéfini).and(age_sup_3.or(assez_grand)).
 * 
 * @author ILM Informatique 27 sept. 2004
 */
@Immutable
public class Where {

    static public final Where FALSE = Where.createRaw("1=0");
    static public final Where TRUE = Where.createRaw("1=1");
    static public final String NULL_IS_DATA_EQ = new String("===");
    static public final String NULL_IS_DATA_NEQ = new String("IS DISTINCT FROM");

    private static abstract class Combiner {
        public final Where combine(final Where w1, final Where w2) {
            if (w1 == null)
                return w2;
            else
                return this.combineNotNull(w1, w2);
        }

        protected abstract Where combineNotNull(Where w1, Where w2);
    }

    private static Combiner AndCombiner = new Combiner() {
        @Override
        protected Where combineNotNull(final Where where1, final Where where2) {
            return where1.and(where2);
        }
    };

    private static Combiner OrCombiner = new Combiner() {
        @Override
        protected Where combineNotNull(final Where where1, final Where where2) {
            return where1.or(where2);
        }
    };

    static private Where combine(final Collection<Where> wheres, final Combiner c) {
        Where res = null;
        for (final Where w : wheres) {
            res = c.combine(res, w);
        }
        return res;
    }

    static public Where and(final Collection<Where> wheres) {
        return combine(wheres, AndCombiner);
    }

    static public Where and(final SQLTable t, final Map<String, ?> fields) {
        final List<Where> res = new ArrayList<Where>(fields.size());
        for (final Entry<String, ?> e : fields.entrySet()) {
            res.add(new Where(t.getField(e.getKey()), "=", e.getValue()));
        }
        return and(res);
    }

    static public Where or(final Collection<Where> wheres) {
        return combine(wheres, OrCombiner);
    }

    /**
     * Permet de faire un ET entre 2 where.
     * 
     * @param where1 le 1er, peut être <code>null</code>.
     * @param where2 le 2ème, peut être <code>null</code>.
     * @return le ET, peut être <code>null</code>.
     */
    static public Where and(final Where where1, final Where where2) {
        return AndCombiner.combine(where1, where2);
    }

    static public Where or(final Where where1, final Where where2) {
        return OrCombiner.combine(where1, where2);
    }

    static public Where isNull(final FieldRef ref) {
        return new Where(ref, "is", (Object) null);
    }

    static public Where isNotNull(final FieldRef ref) {
        return new Where(ref, "is not", (Object) null);
    }

    static public Where createRaw(final String clause, final FieldRef... refs) {
        return createRaw(clause, Arrays.asList(refs));
    }

    static public Where createRaw(final String clause, final Collection<? extends FieldRef> refs) {
        if (clause == null)
            return null;
        return new Where(clause, refs);
    }

    /**
     * To create complex Where not possible with constructors.
     * 
     * @param pattern a pattern to be passed to {@link SQLSelect#quote(String, Object...)}, eg
     *        "EXTRACT(YEAR FROM %n) = 3007".
     * @param params the params to be passed to <code>quote()</code>, eg [|MISSION.DATE_DBT|].
     * @return a new Where with the result from <code>quote()</code> as its clause, and all
     *         <code>FieldRef</code> in params as its fields, eg {EXTRACT(YEAR FROM "DATE_DBT") =
     *         3007 , |MISSION.DATE_DBT|}.
     */
    @SuppressWarnings("unchecked")
    static public Where quote(final String pattern, final Object... params) {
        return new Where(SQLSelect.quote(pattern, params), org.apache.commons.collections.CollectionUtils.select(Arrays.asList(params), new InstanceofPredicate(FieldRef.class)));
    }

    static private final String normalizeOperator(final String op) {
        String res = op.trim();
        if (res.equals("!="))
            res = "<>";
        return res;
    }

    static private final String comparison(final FieldRef ref, final String op, final String y) {
        if (op == NULL_IS_DATA_EQ || op == NULL_IS_DATA_NEQ) {
            return ref.getField().getDBSystemRoot().getSyntax().getNullIsDataComparison(ref.getFieldRef(), op == NULL_IS_DATA_EQ, y);
        } else {
            return ref.getFieldRef() + " " + op + " " + y;
        }
    }

    static private final String getInClause(final FieldRef field1, final boolean in, final String inParens) {
        final String op = in ? " in (" : " not in (";
        return field1.getFieldRef() + op + inParens + ")";
    }

    private final List<FieldRef> fields;
    private final String clause;

    public Where(final FieldRef field1, final String op, final FieldRef field2) {
        this.fields = Arrays.asList(field1, field2);
        this.clause = comparison(field1, normalizeOperator(op), field2.getFieldRef());
    }

    public Where(final FieldRef field1, final String op, final int scalar) {
        this(field1, op, (Integer) scalar);
    }

    /**
     * Construct a clause like "field = 'hi'". Note: this method will try to rewrite "= null" and
     * "<> null" to "is null" and "is not null", treating null as a Java <code>null</code> (ie null
     * == null) and not as a SQL NULL (NULL != NULL), see PostgreSQL documentation section 9.2.
     * Comparison Operators. ATTN new Where(f, "=", null) will call
     * {@link #Where(FieldRef, String, FieldRef)}, you have to cast to Object.
     * 
     * @param ref a field.
     * @param op an arbitrary operator.
     * @param o the object to compare <code>ref</code> to.
     */
    public Where(final FieldRef ref, String op, final Object o) {
        this.fields = Collections.singletonList(ref);
        op = normalizeOperator(op);
        if (o == null) {
            if (op.equals("="))
                op = "is";
            else if (op.equals("<>"))
                op = "is not";
        }
        this.clause = comparison(ref, op, ref.getField().getType().toString(o));
    }

    /**
     * Crée une clause "field1 in (values)". Some databases won't accept empty values (impossible
     * where clause), so we return false.
     * 
     * @param field1 le champs à tester.
     * @param values les valeurs.
     */
    public Where(final FieldRef field1, final Collection<?> values) {
        this(field1, true, values);
    }

    /**
     * Construct a clause like "field1 not in (value, ...)".
     * 
     * @param field1 le champs à tester.
     * @param in <code>true</code> for "in", <code>false</code> for "not in".
     * @param values les valeurs.
     */
    public Where(final FieldRef field1, final boolean in, final Collection<?> values) {
        if (values.isEmpty()) {
            this.fields = Collections.emptyList();
            this.clause = in ? FALSE.getClause() : TRUE.getClause();
        } else {
            this.fields = Collections.singletonList(field1);
            this.clause = getInClause(field1, in, CollectionUtils.join(values, ",", new ITransformer<Object, String>() {
                @Override
                public String transformChecked(final Object input) {
                    return field1.getField().getType().toString(input);
                }
            }));
        }
    }

    public Where(final FieldRef field1, final boolean in, final SQLSelect subQuery) {
        this.fields = Collections.singletonList(field1);
        this.clause = getInClause(field1, in, subQuery.asString());
    }

    /**
     * Crée une clause "field BETWEEN borneInf AND borneSup".
     * 
     * @param ref le champs à tester.
     * @param borneInf la valeur minimum.
     * @param borneSup la valeur maximum.
     */
    public Where(final FieldRef ref, final Object borneInf, final Object borneSup) {
        final SQLField field1 = ref.getField();
        this.fields = Collections.singletonList(ref);
        this.clause = ref.getFieldRef() + " BETWEEN " + field1.getType().toString(borneInf) + " AND " + field1.getType().toString(borneSup);
    }

    /**
     * Crée une clause pour que <code>ref</code> soit compris entre <code>bornInf</code> et
     * <code>bornSup</code>.
     * 
     * @param ref a field, eg NAME.
     * @param borneInf the lower bound, eg "DOE".
     * @param infInclusive <code>true</code> if the lower bound should be included, eg
     *        <code>false</code> if "DOE" shouldn't match.
     * @param borneSup the upper bound, eg "SMITH".
     * @param supInclusive <code>true</code> if the upper bound should be included.
     */
    public Where(final FieldRef ref, final Object borneInf, final boolean infInclusive, final Object borneSup, final boolean supInclusive) {
        this.fields = Collections.singletonList(ref);
        final String infClause = new Where(ref, infInclusive ? ">=" : ">", borneInf).getClause();
        final String supClause = new Where(ref, supInclusive ? "<=" : "<", borneSup).getClause();
        this.clause = infClause + " AND " + supClause;
    }

    // raw ctor, see static methods
    private Where(final String clause, final Collection<? extends FieldRef> refs) {
        if (StringUtils.isEmpty(clause, true))
            throw new IllegalArgumentException("No clause");
        this.fields = Collections.unmodifiableList(new ArrayList<FieldRef>(refs));
        this.clause = clause;
    }

    public Where or(final Where w) {
        return this.combine(w, "OR");
    }

    public Where and(final Where w) {
        return this.combine(w, "AND");
    }

    public Where not() {
        return new Where("NOT (" + this.clause + ")", this.fields);
    }

    private Where combine(final Where w, final String op) {
        if (w == null)
            return this;

        final List<FieldRef> fields = new ArrayList<FieldRef>();
        fields.addAll(this.fields);
        fields.addAll(w.fields);

        final String clause = "(" + this.clause + ") " + op + " (" + w.clause + ")";
        return new Where(clause, fields);
    }

    /**
     * La clause.
     * 
     * @return la clause.
     */
    public String getClause() {
        return this.clause;
    }

    /**
     * Les champs utilisés dans cette clause.
     * 
     * @return a list of FieldRef.
     */
    public List<FieldRef> getFields() {
        return this.fields;
    }

    @Override
    public String toString() {
        return this.getClause();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof Where) {
            final Where o = ((Where) obj);
            return this.getClause().equals(o.getClause()) && this.getFields().equals(o.getFields());
        } else
            return false;
    }

    @Override
    public int hashCode() {
        return this.getClause().hashCode() + this.getFields().hashCode();
    }
}
