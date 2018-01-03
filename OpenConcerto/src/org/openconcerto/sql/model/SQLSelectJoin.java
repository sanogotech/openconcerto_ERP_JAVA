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

import org.openconcerto.sql.model.graph.Step;
import org.openconcerto.utils.Tuple2;

import java.util.List;

public class SQLSelectJoin implements SQLItem {

    private static final Tuple2<FieldRef, TableRef> NULL_TUPLE = new Tuple2<FieldRef, TableRef>(null, null);

    // tries to parse t.ID_LOCAL = l.ID
    static Tuple2<FieldRef, TableRef> parse(final Where w) {
        final List<FieldRef> fields = w.getFields();
        if (fields.size() != 2)
            return NULL_TUPLE;
        final FieldRef pk;
        final FieldRef ff;
        if (fields.get(0).getField().isPrimaryKey()) {
            pk = fields.get(0);
            ff = fields.get(1);
        } else if (fields.get(1).getField().isPrimaryKey()) {
            pk = fields.get(1);
            ff = fields.get(0);
        } else {
            return NULL_TUPLE;
        }
        if (!pk.getTableRef().getTable().equals(ff.getField().getForeignTable()))
            return NULL_TUPLE;

        return Tuple2.create(ff, pk.getTableRef());
    }

    private final SQLSelect parent;
    private final String joinType;
    /** the joined table, e.g. OBSERVATION obs */
    private final TableRef t;
    /** the where, e.g. rec.ID_LOCAL = circuit.ID_LOCAL */
    private final Where joinW;
    /** the optional step from an existing table in the select to the joined table */
    private final Step step;
    /** the existing table if step isn't null */
    private final TableRef existingTable;
    /** An additional where */
    private Where where;

    SQLSelectJoin(final SQLSelect parent, final String joinType, final TableRef existingTable, final Step s, final TableRef joinedTable) {
        this(parent, joinType, joinedTable, s.getFrom().getDBSystemRoot().getGraph().getWhereClause(existingTable, joinedTable, s), s, existingTable);
    }

    SQLSelectJoin(final SQLSelect parent, final String joinType, final TableRef joinedTable, final Where w, final Step step, final TableRef existingTable) {
        super();
        this.parent = parent;
        this.joinType = joinType;
        this.joinW = w;
        this.t = joinedTable;

        this.step = step;
        this.existingTable = existingTable;

        this.where = null;

        // checked by SQLSelect or provided by parse(Where)
        assert (step == null) == (existingTable == null);
        assert step == null || step.getFrom() == existingTable.getTable() && step.getTo() == joinedTable.getTable();
    }

    /**
     * Set an additional where for this join.
     * 
     * @param w the where to add, can be <code>null</code>, e.g. art."ID_SITE" = 123.
     */
    public final void setWhere(Where w) {
        this.where = w;
    }

    public final Where getWhere() {
        return this.where;
    }

    @Override
    public String getSQL() {
        final Where archiveW = this.parent.getArchiveWhere(getJoinedTable().getTable(), getAlias());
        final Where undefW = this.parent.getUndefWhere(getJoinedTable().getTable(), getAlias());
        return " " + this.joinType + " JOIN " + this.t.getSQL() + " on " + this.joinW.and(archiveW).and(undefW).and(getWhere());
    }

    public final String getJoinType() {
        return this.joinType;
    }

    /**
     * The step if the join is a simple t1.fk1 = t2.pk.
     * 
     * @return the step or <code>null</code>, e.g. t1.fk1.
     */
    public final Step getStep() {
        return this.step;
    }

    /**
     * The alias of the source of the step.
     * 
     * @return the source of the {@link #getStep()}, <code>null</code> if there's no step.
     */
    public final TableRef getExistingTable() {
        return this.existingTable;
    }

    public final String getAlias() {
        return this.getJoinedTable().getAlias();
    }

    public final TableRef getJoinedTable() {
        return this.t;
    }

    @Override
    public String toString() {
        return this.getSQL();
    }
}
