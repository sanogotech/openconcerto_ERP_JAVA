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
 
 package org.openconcerto.sql.utils;

import org.openconcerto.sql.model.Constraint;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLField.Properties;
import org.openconcerto.sql.model.SQLName;
import org.openconcerto.sql.model.SQLSyntax;
import org.openconcerto.sql.model.SQLSystem;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.graph.Link;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.StringUtils;
import org.openconcerto.utils.Tuple2;
import org.openconcerto.utils.cc.ITransformer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Construct an ALTER TABLE statement.
 * 
 * @author Sylvain
 */
public final class AlterTable extends ChangeTable<AlterTable> {

    private final SQLTable t;

    public AlterTable(SQLTable t) {
        super(t.getDBSystemRoot().getSyntax(), t.getDBRoot().getName(), t.getName());
        this.t = t;
    }

    // for CREATE TABLE
    AlterTable(SQLSyntax s, String rootName, String tableName) {
        super(s, rootName, tableName);
        this.t = null;
    }

    public final SQLTable getTable() {
        return this.t;
    }

    @Override
    protected String getConstraintPrefix() {
        return "ADD ";
    }

    public final AlterTable addColumn(String name, String definition) {
        // column keyword is not accepted by H2
        return this.addClause("ADD " + SQLBase.quoteIdentifier(name) + " " + definition, ClauseType.ADD_COL);
    }

    public final AlterTable addPrimaryKey(final List<String> names) {
        if (names.size() == 0)
            throw new IllegalArgumentException("Empty fields");
        return this.addClause("ADD PRIMARY KEY(" + SQLSyntax.quoteIdentifiers(names) + ")", ClauseType.ADD_CONSTRAINT);
    }

    public final AlterTable dropColumn(String name) {
        if (this.getSyntax().getSystem() == SQLSystem.MSSQL)
            this.alterColumnDefault(name, null);
        return this.addClause("DROP COLUMN " + SQLBase.quoteIdentifier(name), ClauseType.DROP_COL);
    }

    public final AlterTable dropColumnCascade(String name) {
        return this.dropColumnsCascade(Collections.singleton(name));
    }

    /**
     * Drop the passed columns and all constraints that depends on them. Note: this can thus drop
     * constraints that use other fields.
     * 
     * @param columns the names of the columns to drop.
     * @return this.
     */
    public final AlterTable dropColumnsCascade(Collection<String> columns) {
        for (final Link l : this.t.getForeignLinks())
            if (CollectionUtils.containsAny(l.getCols(), columns))
                this.dropForeignConstraint(l.getName());
        for (final Constraint c : this.t.getConstraints())
            if (CollectionUtils.containsAny(c.getCols(), columns))
                this.dropConstraint(c.getName());
        for (final String name : columns)
            this.dropColumn(name);
        return thisAsT();

    }

    public final AlterTable dropForeignColumn(String name) {
        return dropForeignColumns(Collections.singletonList(name));
    }

    /**
     * Drop the passed columns and their constraint.
     * 
     * @param columns the columns of a single foreign key.
     * @return this.
     * @throws IllegalArgumentException if no foreign key with <code>columns</code> exists.
     */
    public final AlterTable dropForeignColumns(List<String> columns) throws IllegalArgumentException {
        final Link foreignLink = this.t.getDBSystemRoot().getGraph().getForeignLink(this.t, columns);
        if (foreignLink == null)
            throw new IllegalArgumentException("No foreign key in " + this.t + " with : " + columns);
        return dropForeignColumns(foreignLink);
    }

    public final AlterTable dropForeignColumns(final Link foreignLink) throws IllegalArgumentException {
        if (foreignLink.getSource() != this.t)
            throw new IllegalArgumentException("Not in " + this.t + " : " + foreignLink);
        this.dropForeignConstraint(foreignLink.getName());
        for (final String name : foreignLink.getCols())
            this.dropColumn(name);
        return thisAsT();
    }

    public final AlterTable alterColumn(String fname, SQLField from) {
        return this.alterColumn(fname, from, EnumSet.allOf(Properties.class));
    }

    /**
     * Transform <code>fname</code> into <code>from</code>.
     * 
     * @param fname the field to change.
     * @param from the field to copy.
     * @param toTake which properties of <code>from</code> to copy.
     * @return this.
     */
    public final AlterTable alterColumn(String fname, SQLField from, Set<Properties> toTake) {
        return this.addClauses(this.getSyntax().getAlterField(this.t.getField(fname), from, toTake));
    }

    private final AlterTable addClauses(final Map<ClauseType, List<String>> res) {
        for (final Entry<ClauseType, List<String>> e : res.entrySet()) {
            final ClauseType type = e.getKey();
            for (final String s : e.getValue()) {
                this.addClause(s, type);
            }
        }
        return thisAsT();
    }

    /**
     * Alter column properties. Any property value can be <code>null</code> if not used. This method
     * has many parameters since in many systems, properties cannot be changed individually.
     * 
     * @param fname the field to change.
     * @param toAlter which properties to change.
     * @param type the new type.
     * @param defaultVal the new default value.
     * @param nullable whether this field can hold NULL.
     * @return this.
     * @see #alterColumn(String, SQLField, Set)
     */
    public final AlterTable alterColumn(String fname, Set<Properties> toAlter, String type, String defaultVal, Boolean nullable) {
        return this.addClauses(this.getSyntax().getAlterField(this.t.getField(fname), toAlter, type, defaultVal, nullable));
    }

    public final AlterTable alterColumnNullable(String f, boolean b) {
        return this.alterColumn(f, EnumSet.of(Properties.NULLABLE), null, null, b);
    }

    public final AlterTable alterColumnDefault(String f, String defaultVal) {
        return this.alterColumn(f, EnumSet.of(Properties.DEFAULT), null, defaultVal, null);
    }

    public final AlterTable dropPrimaryKey() {
        return this.addClause(getSyntax().getDropPrimaryKey(this.t), ClauseType.DROP_CONSTRAINT);
    }

    /**
     * Drop a foreign constraint.
     * 
     * @param name the name of the constraint to drop.
     * @return this.
     * @see Link#getName()
     */
    public final AlterTable dropForeignConstraint(String name) {
        return this.addClause(getSyntax().getDropFK() + SQLBase.quoteIdentifier(name), ClauseType.DROP_CONSTRAINT);
    }

    public final AlterTable dropConstraint(String name) {
        return this.addClause(getSyntax().getDropConstraint() + SQLBase.quoteIdentifier(name), ClauseType.DROP_CONSTRAINT);
    }

    public final AlterTable dropIndex(final String name) {
        return this.dropIndex(name, true);
    }

    private final AlterTable dropIndex(final String name, final boolean exact) {
        return this.addOutsideClause(new OutsideClause() {
            @Override
            public ClauseType getType() {
                return ClauseType.DROP_INDEX;
            }

            @Override
            public String asString(SQLName tableName) {
                return getSyntax().getDropIndex(exact ? name : getIndexName(tableName, name), tableName);
            }
        });
    }

    public final AlterTable dropUniqueConstraint(final String name, final boolean partial) {
        final SQLSystem system = getSyntax().getSystem();
        if (system == SQLSystem.MSSQL) {
            return this.dropIndex(name, false);
        } else if (!partial) {
            return this.addClause(new DeferredClause() {
                @Override
                public String asString(ChangeTable<?> ct, SQLName tableName) {
                    return getSyntax().getDropConstraint() + getQuotedConstraintName(tableName, name);
                }

                @Override
                public ClauseType getType() {
                    return ClauseType.DROP_CONSTRAINT;
                }
            });
        } else if (system == SQLSystem.POSTGRESQL) {
            return this.dropIndex(name, false);
        } else if (system == SQLSystem.H2) {
            return this.addOutsideClause(new DropUniqueTrigger(name));
        } else if (system == SQLSystem.MYSQL) {
            for (final String event : TRIGGER_EVENTS)
                this.addOutsideClause(new DropUniqueTrigger(name, event));
            return thisAsT();
        } else {
            throw new UnsupportedOperationException("System isn't supported : " + system);
        }
    }

    @Override
    protected String asString(final NameTransformer transf, ConcatStep step) {
        return this.asString(transf, step.getTypes());
    }

    @Override
    public String asString(final NameTransformer transf) {
        // even for a single instance of AlterTable we need to order types since
        // supportMultiAlterClause() might return false, in which case we might return several SQL
        // statements
        return this.asString(transf, ChangeTable.ORDERED_TYPES);
    }

    // Contrary to CREATE TABLE, ALTER TABLE might need to interleave in and out clauses, e.g. :
    // ALTER TABLE DROP CONSTRAINT
    // DROP INDEX
    // ALTER TABLE DROP COLUMN
    // return list of clauses and their types (true : out/standalone, false : in)
    private List<Tuple2<Boolean, List<String>>> getAllClauses(final NameTransformer transf, final Set<ClauseType> types, final SQLName tableName) {
        final List<Tuple2<Boolean, String>> clauses = new ArrayList<Tuple2<Boolean, String>>();
        Boolean lastIsStandalone = null;
        for (final ClauseType type : ORDERED_TYPES) {
            if (types.contains(type)) {
                final List<Tuple2<Boolean, String>> inClauses = new ArrayList<Tuple2<Boolean, String>>();
                for (final String inClause : this.getClauses(tableName, transf, type)) {
                    inClauses.add(Tuple2.create(false, inClause));
                }

                final String outClausesS = this.getOutClauses(tableName, type);
                final List<Tuple2<Boolean, String>> outClauses;
                if (!StringUtils.isEmpty(outClausesS))
                    outClauses = Collections.singletonList(Tuple2.create(true, outClausesS));
                else
                    outClauses = Collections.emptyList();

                // within a given type there's no mandatory order between in and out clauses, so try
                // to group clauses to limit the number of SQL statements. By default
                // (lastIsStandalone == null) inClauses before outClauses, as it might create the
                // table that outClauses needs.
                if (Boolean.TRUE.equals(lastIsStandalone)) {
                    clauses.addAll(outClauses);
                    clauses.addAll(inClauses);
                } else {
                    clauses.addAll(inClauses);
                    clauses.addAll(outClauses);
                }

                lastIsStandalone = clauses.isEmpty() ? null : clauses.get(clauses.size() - 1).get0();
            }
        }

        // now group adjacent clauses
        // [true, foo], [true, bar], [false, baz] =>
        // [[true, [foo, bar]], [false, [baz]]]
        lastIsStandalone = null;
        final List<Tuple2<Boolean, List<String>>> res = new ArrayList<Tuple2<Boolean, List<String>>>();
        for (final Tuple2<Boolean, String> clause : clauses) {
            final Boolean currentIsStandAlone = clause.get0();
            assert currentIsStandAlone != null;
            if (!currentIsStandAlone.equals(lastIsStandalone)) {
                res.add(Tuple2.<Boolean, List<String>> create(currentIsStandAlone, new ArrayList<String>()));
            }
            res.get(res.size() - 1).get1().add(clause.get1());
            lastIsStandalone = currentIsStandAlone;
        }
        return res;
    }

    private final String asString(final NameTransformer transf, final Set<ClauseType> types) {
        final SQLName tableName = transf.transformTableName(new SQLName(getRootName(), this.getName()));

        final StringBuffer res = new StringBuffer(512);
        final String alterTable = "ALTER TABLE " + tableName.quote();
        for (final Tuple2<Boolean, List<String>> standaloneAndClauses : getAllClauses(transf, types, tableName)) {
            final List<String> genClauses = standaloneAndClauses.get1();
            if (standaloneAndClauses.get0()) {
                // e.g. "CREATE INDEX ;"
                res.append(CollectionUtils.join(genClauses, "\n"));
            } else if (this.getSyntax().supportMultiAlterClause()) {
                res.append(alterTable + " \n");
                res.append(CollectionUtils.join(genClauses, ",\n"));
                res.append(";");
            } else {
                res.append(CollectionUtils.join(genClauses, "\n", new ITransformer<String, String>() {
                    @Override
                    public String transformChecked(String input) {
                        return alterTable + " " + input + ";";
                    }
                }));
            }
            res.append('\n');
        }
        // remove last newline
        if (res.length() > 0)
            res.setLength(res.length() - 1);
        return res.toString();
    }
}
