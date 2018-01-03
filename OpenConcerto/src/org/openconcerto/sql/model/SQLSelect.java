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

import org.openconcerto.sql.model.Order.Direction;
import org.openconcerto.sql.model.Order.Nulls;
import org.openconcerto.sql.model.graph.Path;
import org.openconcerto.sql.model.graph.Step;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.Tuple2;
import org.openconcerto.utils.cc.ITransformer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author ILM Informatique 10 mai 2004
 */
public final class SQLSelect {

    public static enum ArchiveMode {
        UNARCHIVED, ARCHIVED, BOTH
    }

    public static enum LockStrength {
        NONE, SHARE, UPDATE
    }

    public static final ArchiveMode UNARCHIVED = ArchiveMode.UNARCHIVED;
    public static final ArchiveMode ARCHIVED = ArchiveMode.ARCHIVED;
    public static final ArchiveMode BOTH = ArchiveMode.BOTH;

    /**
     * Quote %-escaped parameters. %% : %, %s : quoteString, %i : quote, %f : quote(getFullName()),
     * %n : quote(getName()).
     * 
     * @param pattern a string with %, eg "SELECT * FROM %n where %f like '%%a%%'".
     * @param params the parameters, eg [ /TENSION/, |TENSION.LABEL| ].
     * @return pattern with % replaced, eg SELECT * FROM "TENSION" where "TENSION.LABEL" like '%a%'.
     */
    public static final String quote(final String pattern, final Object... params) {
        return SQLBase.quoteStd(pattern, params);
    }

    // [String], eg : [SITE.ID_SITE, AVG(AGE)]
    private final List<String> select;
    // names of columns (explicit aliases and field names), e.g. [ID_SITE, null]
    private final List<String> selectNames;
    // e.g. : [|SITE.ID_SITE|], known fields in this select (addRawSelect)
    private final List<FieldRef> selectFields;
    private Where where;
    private final List<FieldRef> groupBy;
    private Where having;
    // [String]
    private final List<String> order;
    private final FromClause from;
    // all the tables (and their aliases) in this select
    private final AliasedTables declaredTables;
    // {String}, aliases not to include in the FROM clause
    private final Set<String> joinAliases;
    // [String]
    private final List<SQLSelectJoin> joins;

    // la politique générale pour l'exclusion des indéfinis
    private boolean generalExcludeUndefined;
    // [SQLTable => Boolean]
    private final Map<SQLTable, Boolean> excludeUndefined;
    // null key for general
    private final Map<SQLTable, ArchiveMode> archivedPolicy;
    // DISTINCT
    private boolean distinct;
    // how to lock returned rows
    private LockStrength lockStrength;
    // which tables to wait (avoid SELECT FOR UPDATE/SHARE cannot be applied to the nullable side of
    // an outer join)
    private final List<String> waitTrxTables;
    // number of rows to return
    private Integer limit;
    // offset from the start
    private int offset;

    /**
     * Create a new SQLSelect.
     * 
     * @param base the database of the request.
     * @deprecated use {@link #SQLSelect(DBSystemRoot, boolean)}
     */
    @Deprecated
    public SQLSelect(final SQLBase base) {
        this(base, false);
    }

    /**
     * Create a new SQLSelect.
     * 
     * @param base the database of the request.
     * @param plain whether this request should automatically add a where clause for archived and
     *        undefined.
     * @deprecated use {@link #SQLSelect(DBSystemRoot, boolean)}
     */
    @Deprecated
    public SQLSelect(final SQLBase base, final boolean plain) {
        this(base.getDBSystemRoot(), plain);
    }

    public SQLSelect() {
        this(false);
    }

    public SQLSelect(final boolean plain) {
        this((DBSystemRoot) null, plain);
    }

    /**
     * Create a new SQLSelect.
     * 
     * @param sysRoot the database of the request, can be <code>null</code> (it will come from
     *        declared tables).
     * @param plain whether this request should automatically add a where clause for archived and
     *        undefined.
     */
    public SQLSelect(final DBSystemRoot sysRoot, final boolean plain) {
        this.select = new ArrayList<String>();
        this.selectNames = new ArrayList<String>();
        this.selectFields = new ArrayList<FieldRef>();
        this.where = null;
        this.groupBy = new ArrayList<FieldRef>();
        this.having = null;
        this.order = new ArrayList<String>();
        this.from = new FromClause();
        this.declaredTables = new AliasedTables(sysRoot);
        this.joinAliases = new HashSet<String>();
        this.joins = new ArrayList<SQLSelectJoin>();
        // false by default cause it slows things down
        this.distinct = false;
        this.excludeUndefined = new HashMap<SQLTable, Boolean>();
        this.archivedPolicy = new HashMap<SQLTable, ArchiveMode>();
        // none by default since it requires access rights
        this.lockStrength = LockStrength.NONE;
        this.waitTrxTables = new ArrayList<String>();
        this.limit = null;
        this.offset = 0;
        if (plain) {
            this.generalExcludeUndefined = false;
            this.setArchivedPolicy(BOTH);
        } else {
            this.generalExcludeUndefined = true;
            this.setArchivedPolicy(UNARCHIVED);
        }
        // otherwise getArchiveWhere() fails
        assert this.archivedPolicy.containsKey(null);
    }

    /**
     * Clone un SQLSelect.
     * 
     * @param orig l'instance à cloner.
     */
    public SQLSelect(final SQLSelect orig) {
        // ATTN synch les implémentations des attributs (LinkedHashSet, ...)
        this.select = new ArrayList<String>(orig.select);
        this.selectNames = new ArrayList<String>(orig.selectNames);
        this.selectFields = new ArrayList<FieldRef>(orig.selectFields);
        this.where = orig.where;
        this.groupBy = new ArrayList<FieldRef>(orig.groupBy);
        this.having = orig.having;
        this.order = new ArrayList<String>(orig.order);
        this.from = new FromClause(orig.from);
        this.declaredTables = new AliasedTables(orig.declaredTables);
        this.joinAliases = new HashSet<String>(orig.joinAliases);
        this.joins = new ArrayList<SQLSelectJoin>(orig.joins);
        this.generalExcludeUndefined = orig.generalExcludeUndefined;
        this.excludeUndefined = new HashMap<SQLTable, Boolean>(orig.excludeUndefined);
        this.archivedPolicy = new HashMap<SQLTable, ArchiveMode>(orig.archivedPolicy);
        this.distinct = orig.distinct;

        this.lockStrength = orig.lockStrength;
        this.waitTrxTables = new ArrayList<String>(orig.waitTrxTables);
        this.limit = orig.limit;
        this.offset = orig.offset;
    }

    final DBSystemRoot getSystemRoot() {
        final DBSystemRoot sysRoot = this.declaredTables.getSysRoot();
        if (sysRoot == null)
            throw new IllegalStateException("No systemRoot supplied (neither in the constructor nor by adding an item)");
        return sysRoot;
    }

    public final SQLSystem getSQLSystem() {
        return getSystemRoot().getServer().getSQLSystem();
    }

    public final SQLSyntax getSyntax() {
        return getSystemRoot().getSyntax();
    }

    public String asString() {
        final SQLSystem sys = this.getSQLSystem();

        final StringBuffer result = new StringBuffer(512);
        result.append("SELECT ");
        if (this.distinct)
            result.append("DISTINCT ");
        result.append(CollectionUtils.join(this.select, ", "));

        result.append("\n " + this.from.getSQL());

        // si c'est null, ca marche
        Where archive = this.where;
        // ne pas exclure les archivés et les indéfinis des joins : SQLSelectJoin does it
        final Collection<String> fromAliases = CollectionUtils.substract(this.declaredTables.getAliases(), this.joinAliases);
        for (final String alias : fromAliases) {
            final SQLTable fromTable = this.declaredTables.getTable(alias);
            // on ignore les lignes archivées
            archive = Where.and(getArchiveWhere(fromTable, alias), archive);
            // on ignore les lignes indéfines
            archive = Where.and(getUndefWhere(fromTable, alias), archive);
        }
        // archive == null si pas d'archive et pas d'undefined
        if (archive != null) {
            result.append("\n WHERE ");
            result.append(archive.getClause());
        }
        if (!this.groupBy.isEmpty()) {
            result.append("\n GROUP BY ");
            result.append(CollectionUtils.join(this.groupBy, ", ", new ITransformer<FieldRef, String>() {
                @Override
                public String transformChecked(final FieldRef input) {
                    return input.getFieldRef();
                }
            }));
        }
        if (this.having != null) {
            result.append("\n HAVING ");
            result.append(this.having.getClause());
        }
        if (!this.order.isEmpty()) {
            result.append("\n ORDER BY ");
            result.append(CollectionUtils.join(this.order, ", "));
        }
        // most systems need to specify both
        if (this.getLimit() != null || this.getOffset() != 0) {
            if (sys == SQLSystem.MSSQL) {
                result.append("\nOFFSET ");
                result.append(this.getOffset());
                result.append(" ROWS");
                if (this.getLimit() != null) {
                    result.append(" FETCH NEXT ");
                    result.append(this.getLimit());
                    result.append(" ROWS ONLY");
                }
            } else {
                final Object actualLimit;
                if (this.getLimit() != null) {
                    actualLimit = this.getLimit();
                } else if (sys == SQLSystem.H2) {
                    actualLimit = "NULL";
                } else if (sys == SQLSystem.POSTGRESQL) {
                    actualLimit = "ALL";
                } else {
                    // From the official MySQL manual
                    actualLimit = Integer.MAX_VALUE;
                }
                result.append("\nLIMIT ");
                result.append(actualLimit);
                result.append(" OFFSET ");
                result.append(this.getOffset());
            }
        }
        // wait for other update trx to finish before selecting
        if (this.lockStrength != LockStrength.NONE) {
            if (sys.equals(SQLSystem.POSTGRESQL)) {
                result.append(this.lockStrength == LockStrength.SHARE ? " FOR SHARE" : " FOR UPDATE");
                if (this.waitTrxTables.size() > 0)
                    result.append(" OF " + CollectionUtils.join(this.waitTrxTables, ", "));
            } else if (sys.equals(SQLSystem.MYSQL)) {
                result.append(this.lockStrength == LockStrength.SHARE ? " LOCK IN SHARE MODE" : " FOR UPDATE");
            } else if (sys.equals(SQLSystem.H2)) {
                result.append(" FOR UPDATE");
            } else {
                throw new IllegalStateException("Unsupported system : " + sys);
            }
        }

        return result.toString();
    }

    Where getArchiveWhere(final SQLTable table, final String alias) {
        final Where res;
        // null key is the default
        final ArchiveMode m = this.archivedPolicy.containsKey(table) ? this.archivedPolicy.get(table) : this.archivedPolicy.get(null);
        assert m != null : "no default policy";
        if (m == BOTH) {
            res = null;
        } else if (table.isArchivable()) {
            final Object archiveValue;
            if (table.getArchiveField().getType().getJavaType().equals(Boolean.class)) {
                archiveValue = m == ARCHIVED;
            } else {
                archiveValue = m == ARCHIVED ? 1 : 0;
            }
            res = new Where(this.createRef(alias, table.getArchiveField()), "=", archiveValue);
        } else {
            // for tables that aren't archivable, either all rows or no rows
            res = m == ARCHIVED ? Where.FALSE : null;
        }
        return res;
    }

    Where getUndefWhere(final SQLTable table, final String alias) {
        final Where res;
        final Boolean exclude = this.excludeUndefined.get(table);
        if (table.isRowable() && (exclude == Boolean.TRUE || (exclude == null && this.generalExcludeUndefined))) {
            // no need to use NULL_IS_DATA_NEQ since we're in FROM or JOIN and ID cannot be null
            res = new Where(this.createRef(alias, table.getKey()), "!=", table.getUndefinedID());
        } else
            res = null;
        return res;
    }

    @Override
    public String toString() {
        return this.asString();
    }

    /**
     * SQL expressions of the SELECT.
     * 
     * @return a list of expressions used by the SELECT, e.g. "T.*, A.f", "count(*)".
     */
    public List<String> getSelect() {
        return Collections.unmodifiableList(this.select);
    }

    /**
     * Column names of the SELECT. Should always have the same length and same indexes as the result
     * set, i.e. will contain <code>null</code> for computed columns without aliases. But the length
     * may not be equal to that of {@link #getSelect()}, e.g. when using
     * {@link #addSelectStar(TableRef)} which add one expression but all the fields.
     * 
     * @return a list of column names of the SELECT, <code>null</code> for indexes without any.
     */
    public List<String> getSelectNames() {
        return Collections.unmodifiableList(this.selectNames);
    }

    /**
     * Fields of the SELECT. Should always have the same length and same indexes as the result set,
     * i.e. will contain <code>null</code> for computed columns. But the length may not be equal to
     * that of {@link #getSelect()}, e.g. when using {@link #addSelectStar(TableRef)} which add one
     * expression but all the fields.
     * 
     * @return a list of fields used by the SELECT, <code>null</code> for indexes without any.
     */
    public final List<FieldRef> getSelectFields() {
        return Collections.unmodifiableList(this.selectFields);
    }

    public List<String> getOrder() {
        return this.order;
    }

    public Where getWhere() {
        return this.where;
    }

    public final boolean contains(final String alias) {
        return this.declaredTables.contains(alias);
    }

    /**
     * Whether this SELECT already references table (eg by a from or a join). For example, if not
     * you can't ORDER BY with a field of that table.
     * 
     * @param table the table to test.
     * @return <code>true</code> if table is already in this.
     */
    public final boolean contains(final SQLTable table) {
        return this.contains(table.getName());
    }

    private final void addIfNotExist(final TableRef t) {
        if (this.declaredTables.add(t, false))
            this.from.add(t);
    }

    // *** group by / having

    public SQLSelect addGroupBy(final FieldRef f) {
        this.groupBy.add(f);
        return this;
    }

    public SQLSelect setHaving(final Where w) {
        this.having = w;
        return this;
    }

    // *** order by

    /**
     * Ajoute un ORDER BY.
     * 
     * @param t a table alias.
     * @return this.
     * @throws IllegalArgumentException si t n'est pas ordonné.
     * @throws IllegalStateException si t n'est pas dans cette requete.
     * @see SQLTable#isOrdered()
     */
    public SQLSelect addOrder(final String t) {
        return this.addOrder(this.getTableRef(t));
    }

    public SQLSelect addOrder(final TableRef t) {
        return this.addOrder(t, true);
    }

    /**
     * Add an ORDER BY for the passed table.
     * 
     * @param t the table.
     * @param fieldMustExist if <code>true</code> then <code>t</code> must be
     *        {@link SQLTable#isOrdered() ordered} or have a {@link SQLTable#isRowable() numeric
     *        primary key}.
     * @return this.
     * @throws IllegalArgumentException if <code>t</code> has no usable order field and
     *         <code>mustExist</code> is <code>true</code>.
     */
    public SQLSelect addOrder(final TableRef t, final boolean fieldMustExist) {
        final SQLField orderField = t.getTable().getOrderField();
        if (orderField != null)
            this.addFieldOrder(t.getField(orderField.getName()));
        else if (t.getTable().isRowable())
            this.addFieldOrder(t.getKey());
        else if (fieldMustExist)
            throw new IllegalArgumentException("table is not ordered : " + t);
        return this;
    }

    public SQLSelect addFieldOrder(final FieldRef fieldRef) {
        return this.addFieldOrder(fieldRef, Order.asc());
    }

    public SQLSelect addFieldOrder(final FieldRef fieldRef, final Direction dir) {
        return this.addFieldOrder(fieldRef, dir, null);
    }

    public SQLSelect addFieldOrder(final FieldRef fieldRef, final Direction dir, final Nulls nulls) {
        // with Derby if you ORDER BY w/o mentioning the field in the select clause
        // you can't get the table names of columns in a result set.
        if (fieldRef.getField().getServer().getSQLSystem().equals(SQLSystem.DERBY))
            this.addSelect(fieldRef);

        return this.addRawOrder(fieldRef.getFieldRef() + dir.getSQL() + (nulls == null ? "" : nulls.getSQL()));
    }

    /**
     * Add an ORDER BY that is not an ORDER field.
     * 
     * @param selectItem an item that appears in the select, either a field reference or an alias.
     * @return this.
     */
    public SQLSelect addRawOrder(final String selectItem) {
        this.order.add(selectItem);
        return this;
    }

    public SQLSelect clearOrder() {
        this.order.clear();
        return this;
    }

    /**
     * Ajoute un ORDER BY. Ne fais rien si t n'est pas ordonné.
     * 
     * @param t la table.
     * @return this.
     * @throws IllegalStateException si t n'est pas dans cette requete.
     */
    public SQLSelect addOrderSilent(final String t) {
        return this.addOrder(this.getTableRef(t), false);
    }

    // *** select

    /**
     * Ajoute un champ au SELECT.
     * 
     * @param f le champ à ajouter.
     * @return this pour pouvoir chaîner.
     */
    public SQLSelect addSelect(final FieldRef f) {
        return this.addSelect(f, null);
    }

    /**
     * Permet d'ajouter plusieurs champs.
     * 
     * @param s une collection de FieldRef.
     * @return this pour pouvoir chaîner.
     */
    public SQLSelect addAllSelect(final Collection<? extends FieldRef> s) {
        for (final FieldRef element : s) {
            this.addSelect(element);
        }
        return this;
    }

    /**
     * Permet d'ajouter plusieurs champs d'une même table sans avoir à les préfixer.
     * 
     * @param t la table.
     * @param s une collection de nom de champs, eg "NOM".
     * @return this pour pouvoir chaîner.
     */
    public SQLSelect addAllSelect(final TableRef t, final Collection<String> s) {
        for (final String fieldName : s) {
            this.addSelect(t.getField(fieldName));
        }
        return this;
    }

    /**
     * Ajoute une fonction d'un champ au SELECT.
     * 
     * @param f le champ, eg "PERSON.AGE".
     * @param function la fonction, eg "AVG".
     * @return this pour pouvoir chaîner.
     */
    public SQLSelect addSelect(final FieldRef f, final String function) {
        return this.addSelect(f, function, null);
    }

    public SQLSelect addSelect(final FieldRef f, final String function, final String alias) {
        final String defaultAlias;
        String s = f.getFieldRef();
        if (function != null) {
            s = function + "(" + s + ")";
            defaultAlias = function;
        } else {
            defaultAlias = f.getField().getName();
        }
        return this.addRawSelect(f, s, alias, defaultAlias);
    }

    /**
     * To add an item that is not a field.
     * 
     * @param expr any legal exp in a SELECT statement (e.g. a constant, a complex function, etc).
     * @param alias a name for the expression, may be <code>null</code>.
     * @return this.
     */
    public SQLSelect addRawSelect(final String expr, final String alias) {
        return this.addRawSelect(null, expr, alias, null);
    }

    // private since we can't check that f is used in expr
    // defaultName only used if alias is null
    private SQLSelect addRawSelect(final FieldRef f, String expr, final String alias, final String defaultName) {
        if (alias != null) {
            expr += " as " + SQLBase.quoteIdentifier(alias);
        }
        this.select.add(expr);
        if (f != null)
            this.addIfNotExist(f.getTableRef());
        this.selectFields.add(f);
        this.selectNames.add(alias != null ? alias : defaultName);
        return this;
    }

    /**
     * Ajoute une fonction prenant * comme paramètre.
     * 
     * @param function la fonction, eg "COUNT".
     * @return this pour pouvoir chaîner.
     */
    public SQLSelect addSelectFunctionStar(final String function) {
        return this.addRawSelect(function + "(*)", null);
    }

    public SQLSelect addSelectStar(final TableRef table) {
        this.select.add(SQLBase.quoteIdentifier(table.getAlias()) + ".*");
        this.addIfNotExist(table);
        final List<SQLField> allFields = table.getTable().getOrderedFields();
        this.selectFields.addAll(allFields);
        for (final SQLField f : allFields)
            this.selectNames.add(f.getName());
        return this;
    }

    public SQLSelect clearSelect() {
        this.select.clear();
        this.selectFields.clear();
        this.selectNames.clear();
        return this;
    }

    // *** from

    public SQLSelect addFrom(final SQLTable table, final String alias) {
        return this.addFrom(AliasedTable.getTableRef(table, alias));
    }

    /**
     * Explicitely add a table to the from clause. Rarely needed since tables are auto added by
     * addSelect(), setWhere() and addJoin().
     * 
     * @param t the table to add.
     * @return this.
     */
    public SQLSelect addFrom(final TableRef t) {
        this.addIfNotExist(t);
        return this;
    }

    // *** where

    /**
     * Change la clause where de cette requete.
     * 
     * @param w la nouvelle clause, <code>null</code> pour aucune clause.
     * @return this.
     */
    public SQLSelect setWhere(final Where w) {
        this.where = w;
        // FIXME si where était non null alors on a ajouté des tables dans FROM
        // qui ne sont peut être plus utiles
        // une solution : ne calculer le from que dans asString() => marche pas car on s'en
        // sert dans addOrder
        if (w != null) {
            for (final FieldRef f : w.getFields()) {
                this.addIfNotExist(f.getTableRef());
            }
        }
        return this;
    }

    public SQLSelect setWhere(final FieldRef field, final String op, final int i) {
        return this.setWhere(new Where(field, op, i));
    }

    /**
     * Ajoute le Where passé à celui de ce select.
     * 
     * @param w le Where à ajouter.
     * @return this.
     */
    public SQLSelect andWhere(final Where w) {
        return this.setWhere(Where.and(this.getWhere(), w));
    }

    // *** join

    /**
     * Add a join to this SELECT.
     * 
     * @param joinType can be INNER, LEFT or RIGHT.
     * @param existingAlias an alias for a table already in this select, can be <code>null</code>.
     * @param s how to join a new table, i.e. the {@link Step#getTo() destination} will be added.
     * @param joinedAlias the alias of the new table, can be <code>null</code>.
     * @return the added join.
     */
    public SQLSelectJoin addJoin(final String joinType, final String existingAlias, final Step s, final String joinedAlias) {
        final TableRef existingTable = this.getTableRef(existingAlias != null ? existingAlias : s.getFrom().getName());
        final TableRef joinedTable = new AliasedTable(s.getTo(), joinedAlias);
        return this.addJoin(new SQLSelectJoin(this, joinType, existingTable, s, joinedTable));
    }

    // simple joins (with foreign field)

    /**
     * Add a join to this SELECT. Eg if <code>f</code> is |BATIMENT.ID_SITE|, then "join SITE on
     * BATIMENT.ID_SITE = SITE.ID" will be added.
     * 
     * @param joinType can be INNER, LEFT or RIGHT.
     * @param f a foreign key, eg |BATIMENT.ID_SITE|.
     * @return the added join.
     */
    public SQLSelectJoin addJoin(final String joinType, final FieldRef f) {
        return this.addJoin(joinType, f, null);
    }

    /**
     * Add a join to this SELECT. Eg if <code>f</code> is bat.ID_SITE and <code>alias</code> is "s",
     * then "join SITE s on bat.ID_SITE = s.ID" will be added.
     * 
     * @param joinType can be INNER, LEFT or RIGHT.
     * @param f a foreign key, eg obs.ID_ARTICLE_2.
     * @param alias the alias for joined table, can be <code>null</code>, eg "art2".
     * @return the added join.
     */
    public SQLSelectJoin addJoin(final String joinType, final FieldRef f, final String alias) {
        final Step s = Step.create(f.getField(), org.openconcerto.sql.model.graph.Link.Direction.FOREIGN);
        return this.addJoin(joinType, f.getAlias(), s, alias);
    }

    // arbitrary joins

    /**
     * Add a join to this SELECT, inferring the joined table from the where.
     * 
     * @param joinType can be INNER, LEFT or RIGHT.
     * @param w the where joining the new table.
     * @return the added join.
     * @throws IllegalArgumentException if <code>w</code> hasn't exactly one table not yet
     *         {@link #contains(String) contained} in this.
     */
    public SQLSelectJoin addJoin(final String joinType, final Where w) {
        final Set<AliasedTable> tables = new HashSet<AliasedTable>();
        for (final FieldRef f : w.getFields()) {
            if (!this.contains(f.getAlias())) {
                // since it's a Set, use same class (i.e. SQLTable.equals(AliasedTable) always
                // return false)
                tables.add(new AliasedTable(f.getField().getTable(), f.getAlias()));
            }
        }
        if (tables.size() == 0)
            throw new IllegalArgumentException("No tables to add in " + w);
        if (tables.size() > 1)
            throw new IllegalArgumentException("More than one table to add (" + tables + ") in " + w);
        final AliasedTable joinedTable = tables.iterator().next();
        return addJoin(joinType, joinedTable, w);
    }

    public SQLSelectJoin addJoin(final String joinType, final TableRef joinedTable, final Where w) {
        // try to parse the where to find a Step
        final Tuple2<FieldRef, TableRef> parsed = SQLSelectJoin.parse(w);
        final FieldRef foreignFieldParsed = parsed.get0();
        final Step s;
        final TableRef existingTable;
        if (foreignFieldParsed == null) {
            s = null;
            existingTable = null;
        } else {
            final TableRef srcTableParsed = foreignFieldParsed.getTableRef();
            final TableRef destTableParsed = parsed.get1();
            if (AliasedTable.equals(destTableParsed, joinedTable)) {
                existingTable = srcTableParsed;
                s = Step.create(foreignFieldParsed.getField(), org.openconcerto.sql.model.graph.Link.Direction.FOREIGN);
            } else if (AliasedTable.equals(srcTableParsed, joinedTable)) {
                existingTable = destTableParsed;
                s = Step.create(foreignFieldParsed.getField(), org.openconcerto.sql.model.graph.Link.Direction.REFERENT);
            } else {
                throw new IllegalArgumentException("Joined table " + joinedTable + " isn't referenced in " + w);
            }
        }

        return this.addJoin(new SQLSelectJoin(this, joinType, joinedTable, w, s, existingTable));
    }

    /**
     * Add a join that goes backward through a foreign key, eg LEFT JOIN "KD_2006"."BATIMENT" "bat"
     * on "s"."ID" = "bat"."ID_SITE".
     * 
     * @param joinType can be INNER, LEFT or RIGHT.
     * @param joinAlias the alias for the joined table, must not exist, eg "bat".
     * @param ff the foreign field, eg |BATIMENT.ID_SITE|.
     * @param foreignTableAlias the alias for the foreign table, must exist, eg "sit" or
     *        <code>null</code> for "SITE".
     * @return the added join.
     */
    public SQLSelectJoin addBackwardJoin(final String joinType, final String joinAlias, final SQLField ff, final String foreignTableAlias) {
        return this.addBackwardJoin(joinType, new AliasedField(ff, joinAlias), foreignTableAlias);
    }

    /**
     * Add a join that goes backward through a foreign key, eg LEFT JOIN "KD_2006"."BATIMENT" "bat"
     * on "s"."ID" = "bat"."ID_SITE".
     * 
     * @param joinType can be INNER, LEFT or RIGHT.
     * @param ff the foreign field, the alias must not exist, e.g. bat.ID_SITE.
     * @param foreignTableAlias the alias for the foreign table, must exist, e.g. "sit" or
     *        <code>null</code> for "SITE".
     * @return the added join.
     */
    public SQLSelectJoin addBackwardJoin(final String joinType, final FieldRef ff, final String foreignTableAlias) {
        final Step s = Step.create(ff.getField(), org.openconcerto.sql.model.graph.Link.Direction.REFERENT);
        return this.addJoin(joinType, foreignTableAlias, s, ff.getAlias());
    }

    private final SQLSelectJoin addJoin(final SQLSelectJoin j) {
        // first check if the joined table is not already in this from
        // avoid this (where the 2nd line already added MOUVEMENT) :
        // sel.addSelect(tableEcriture.getField("NOM"));
        // sel.addSelect(tableMouvement.getField("NUMERO"));
        // sel.addJoin("LEFT", "ECRITURE.ID_MOUVEMENT");
        final boolean added = this.declaredTables.add(j.getJoinedTable(), true);
        // since we passed mustBeNew=true
        assert added;
        this.from.add(j);
        this.joinAliases.add(j.getAlias());
        this.joins.add(j);
        return j;
    }

    // ATTN doesn't check if the join is referenced
    private final void removeJoin(final SQLSelectJoin j) {
        if (this.joins.remove(j)) {
            final boolean removed = this.declaredTables.remove(j.getJoinedTable());
            assert removed;
            this.from.remove(j);
            this.joinAliases.remove(j.getAlias());
        }
    }

    public final List<SQLSelectJoin> getJoins() {
        return Collections.unmodifiableList(this.joins);
    }

    /**
     * Get the join going through <code>ff</code>, regardless of its alias.
     * 
     * @param ff a foreign field, eg |BATIMENT.ID_SITE|.
     * @return the corresponding join or <code>null</code> if not exactly one is found, e.g. LEFT
     *         JOIN "test"."SITE" "s" on "bat"."ID_SITE"="s"."ID"
     */
    public final SQLSelectJoin getJoinFromField(final SQLField ff) {
        return CollectionUtils.getSole(getJoinsFromField(ff));
    }

    public final List<SQLSelectJoin> getJoinsFromField(final SQLField ff) {
        final List<SQLSelectJoin> res = new ArrayList<SQLSelectJoin>();
        for (final SQLSelectJoin j : this.joins) {
            final Step s = j.getStep();
            if (s != null && ff.equals(s.getSingleField())) {
                res.add(j);
            }
        }
        return res;
    }

    /**
     * The first join adding the passed table.
     * 
     * @param t the table to search for, e.g. /LOCAL/.
     * @return the first matching join or <code>null</code> if none found, eg LEFT JOIN
     *         "test"."LOCAL" "l" on "r"."ID_LOCAL"="l"."ID"
     */
    public final SQLSelectJoin findJoinAdding(final SQLTable t) {
        for (final SQLSelectJoin j : this.joins) {
            if (j.getJoinedTable().getTable().equals(t)) {
                return j;
            }
        }
        return null;
    }

    /**
     * The join adding the passed table alias.
     * 
     * @param alias a table alias, e.g. "l".
     * @return the matching join or <code>null</code> if none found, eg LEFT JOIN "test"."LOCAL" "l"
     *         on "r"."ID_LOCAL"="l"."ID"
     */
    public final SQLSelectJoin getJoinAdding(final String alias) {
        for (final SQLSelectJoin j : this.joins) {
            if (j.getAlias().equals(alias)) {
                return j;
            }
        }
        return null;
    }

    /**
     * Get the join going through <code>ff</code>, matching its alias but regardless of its
     * direction.
     * 
     * @param ff a foreign field, eg |BATIMENT.ID_SITE|.
     * @return the corresponding join or <code>null</code> if not exactly one is found, eg
     *         <code>null</code> if this only contains LEFT JOIN "test"."SITE" "s" on
     *         "bat"."ID_SITE"="s"."ID" or if it contains
     * 
     *         <pre>
     *         LEFT JOIN "test"."SITE" s1 on "BATIMENT"."ID_SITE" = s1."ID" and s1."FOO"
     *         LEFT JOIN "test"."SITE" s2 on "BATIMENT"."ID_SITE" = s2."ID" and s2."BAR"
     *         </pre>
     */
    public final SQLSelectJoin getJoin(final FieldRef ff) {
        return this.getJoin(ff, null);
    }

    public final SQLSelectJoin getJoin(final FieldRef ff, final String foreignAlias) {
        final Step s = Step.create(ff.getField(), org.openconcerto.sql.model.graph.Link.Direction.FOREIGN);
        final List<SQLSelectJoin> res = new ArrayList<SQLSelectJoin>();
        res.addAll(this.getJoins(ff.getAlias(), s, foreignAlias));
        res.addAll(this.getJoins(foreignAlias, s.reverse(), ff.getAlias()));

        // if we specify both aliases there can't be more than one join
        if (foreignAlias != null && res.size() > 1)
            throw new IllegalStateException("More than one join matched " + ff + " and " + foreignAlias + " :\n" + CollectionUtils.join(res, "\n"));
        return CollectionUtils.getSole(res);
    }

    /**
     * Get the JOIN matching the passed step.
     * 
     * @param fromAlias the alias for the source of the step, <code>null</code> match any alias.
     * @param s the {@link SQLSelectJoin#getStep() step}, cannot be <code>null</code>.
     * @param joinedAlias the alias for the destination of the step, i.e. the
     *        {@link SQLSelectJoin#getJoinedTable() added table}, <code>null</code> match any alias.
     * @return the matching joins.
     */
    public final List<SQLSelectJoin> getJoins(final String fromAlias, final Step s, final String joinedAlias) {
        if (s == null)
            throw new NullPointerException("Null step");
        final List<SQLSelectJoin> res = new ArrayList<SQLSelectJoin>();
        for (final SQLSelectJoin j : this.joins) {
            final Step joinStep = j.getStep();
            if (s.equals(joinStep) && (fromAlias == null || j.getExistingTable().getAlias().equals(fromAlias)) && (joinedAlias == null || j.getAlias().equals(joinedAlias))) {
                res.add(j);
            }
        }
        return res;
    }

    /**
     * Assure that there's a path from <code>tableAlias</code> through <code>p</code>, adding the
     * missing joins.
     * 
     * @param tableAlias the table at the start, eg "loc".
     * @param p the path that must be added, eg LOCAL-BATIMENT-SITE.
     * @return the alias of the last table of the path, "sit".
     */
    public TableRef assurePath(final String tableAlias, final Path p) {
        return this.followPath(tableAlias, p, true);
    }

    public TableRef followPath(final String tableAlias, final Path p) {
        return this.followPath(tableAlias, p, false);
    }

    /**
     * Return the alias at the end of the passed path.
     * 
     * @param tableAlias the table at the start, eg "loc".
     * @param p the path to follow, eg LOCAL-BATIMENT-SITE.
     * @param create <code>true</code> if missing joins should be created.
     * @return the alias of the last table of the path or <code>null</code>, eg "sit".
     */
    public TableRef followPath(final String tableAlias, final Path p, final boolean create) {
        final TableRef firstTableRef = this.getTableRef(tableAlias);
        final SQLTable firstTable = firstTableRef.getTable();
        if (!p.getFirst().equals(firstTable) && !p.getLast().equals(firstTable))
            throw new IllegalArgumentException("neither ends of " + p + " is " + firstTable);
        else if (!p.getFirst().equals(firstTable))
            return followPath(tableAlias, p.reverse(), create);

        TableRef current = firstTableRef;
        for (int i = 0; i < p.length(); i++) {
            final Step step = p.getStep(i);
            final List<SQLSelectJoin> joins = this.getJoins(current.getAlias(), step, null);
            if (joins.size() > 1)
                throw new IllegalStateException("More than one join from " + current + " through " + step + " : " + joins);
            if (joins.size() == 1) {
                current = joins.get(0).getJoinedTable();
            } else if (create) {
                // we must add a join
                final String uniqAlias = getUniqueAlias("assurePath_" + i);
                final SQLSelectJoin createdJoin = this.addJoin("LEFT", current.getAlias(), step, uniqAlias);
                current = createdJoin.getJoinedTable();
            } else {
                return null;
            }
        }

        return current;
    }

    public final FieldRef followFieldPath(final IFieldPath fp) {
        return this.followFieldPath(fp.getPath().getFirst().getAlias(), fp);
    }

    public final FieldRef followFieldPath(final String tableAlias, final IFieldPath fp) {
        return this.followPath(tableAlias, fp.getPath()).getField(fp.getFieldName());
    }

    public boolean isExcludeUndefined() {
        return this.generalExcludeUndefined;
    }

    public void setExcludeUndefined(final boolean excludeUndefined) {
        this.generalExcludeUndefined = excludeUndefined;
    }

    public void setExcludeUndefined(final boolean exclude, final SQLTable table) {
        this.excludeUndefined.put(table, Boolean.valueOf(exclude));
    }

    public void setArchivedPolicy(final ArchiveMode policy) {
        this.setArchivedPolicy(null, policy);
    }

    public void setArchivedPolicy(final SQLTable t, final ArchiveMode policy) {
        this.archivedPolicy.put(t, policy);
    }

    public final void setDistinct(final boolean distinct) {
        this.distinct = distinct;
    }

    /**
     * Whether this SELECT should wait until all current transactions are complete. This prevent a
     * SELECT following an UPDATE from seeing rows as they were before. NOTE that this may conflict
     * with other clauses (GROUP BY, DISTINCT, etc.).
     * 
     * @param waitTrx <code>true</code> if this select should wait.
     * @deprecated use {@link #setLockStrength(LockStrength)}
     */
    public void setWaitPreviousWriteTX(final boolean waitTrx) {
        this.setLockStrength(waitTrx ? LockStrength.SHARE : LockStrength.NONE);
    }

    /**
     * Set the lock strength for the returned rows. NOTE : this is a minimum, e.g. H2 only supports
     * {@link LockStrength#UPDATE}.
     * 
     * @param l the new lock strength.
     * @throws IllegalArgumentException if the {@link #getSQLSystem() system} doesn't support locks.
     */
    public final void setLockStrength(final LockStrength l) throws IllegalArgumentException {
        final SQLSystem sys = getSQLSystem();
        if (l != LockStrength.NONE && sys != SQLSystem.POSTGRESQL && sys != SQLSystem.MYSQL && sys != SQLSystem.H2)
            throw new IllegalArgumentException("This system doesn't support locks : " + sys);
        this.lockStrength = l;
    }

    public final LockStrength getLockStrength() {
        return this.lockStrength;
    }

    public void addLockedTable(final String table) {
        if (this.getLockStrength() == LockStrength.NONE)
            this.setLockStrength(LockStrength.SHARE);
        this.waitTrxTables.add(SQLBase.quoteIdentifier(table));
    }

    /**
     * Set the maximum number of rows to return.
     * 
     * @param limit the number of rows, <code>null</code> meaning no limit
     * @return this.
     */
    public SQLSelect setLimit(final Integer limit) {
        this.limit = limit;
        return this;
    }

    public final Integer getLimit() {
        return this.limit;
    }

    /**
     * Set the number of rows to skip. NOTE: many systems require an ORDER BY, but even if some
     * don't you should use one to get consistent results.
     * 
     * @param offset number of rows to skip, <code>0</code> meaning don't skip any.
     * @return this.
     */
    public SQLSelect setOffset(final int offset) {
        if (offset < 0)
            throw new IllegalArgumentException("Negative offset : " + offset);
        this.offset = offset;
        return this;
    }

    public int getOffset() {
        return this.offset;
    }

    @Override
    public boolean equals(final Object o) {
        if (o instanceof SQLSelect)
            // MAYBE use instance variables
            return this.asString().equals(((SQLSelect) o).asString());
        else
            return false;
    }

    @Override
    public int hashCode() {
        // don't use asString() which is more CPU intensive
        return this.select.hashCode() + this.from.getSQL().hashCode() + (this.where == null ? 0 : this.where.hashCode());
    }

    /**
     * This method will return a query for counting rows of this SELECT.
     * 
     * @return a query returning a single number, never <code>null</code>.
     */
    public final String getForRowCount() {
        if (this.getLimit() != null && this.getLimit().intValue() == 0)
            return "SELECT 0";

        final SQLSelect res = new SQLSelect(this);
        if (res.clearForRowCount(true)) {
            return "select count(*) from (" + res.asString() + ") subq";
        } else {
            return res.asString();
        }
    }

    /**
     * This method will replace the expressions in the {@link #getSelect()} by <code>count(*)</code>
     * , further it will remove any items not useful for counting rows. This includes
     * {@link #getOrder()} and left joins.
     * 
     * @throws IllegalStateException if this has GROUP BY, HAVING, OFFSET or LIMIT
     * @see #getForRowCount()
     */
    public final void clearForRowCount() throws IllegalStateException {
        this.clearForRowCount(false);
    }

    private final boolean clearForRowCount(final boolean allowSubquery) throws IllegalStateException {
        final boolean hasGroupByOrHaving = !this.groupBy.isEmpty() || this.having != null;
        if (!allowSubquery && hasGroupByOrHaving)
            throw new IllegalStateException("Group by present");
        final boolean hasOffsetOrLimit = this.getOffset() > 0 || this.getLimit() != null;
        if (!allowSubquery && hasOffsetOrLimit)
            throw new IllegalStateException("Offset or limit present");

        this.clearSelect();
        // not needed and some systems require the used fields to be in the select
        this.clearOrder();
        // some systems don't support aggregate functions (e.g. count) with this
        this.setWaitPreviousWriteTX(false);

        final Set<String> requiredAliases;
        if (this.getWhere() == null) {
            requiredAliases = Collections.emptySet();
        } else {
            requiredAliases = new HashSet<String>();
            for (final FieldRef f : this.getWhere().getFields()) {
                requiredAliases.add(f.getTableRef().getAlias());
            }
        }
        for (final SQLSelectJoin j : new ArrayList<SQLSelectJoin>(this.joins)) {
            if (j.getJoinType().equalsIgnoreCase("left") && !requiredAliases.contains(j.getAlias()))
                this.removeJoin(j);
        }

        if (hasGroupByOrHaving || hasOffsetOrLimit) {
            assert allowSubquery;
            this.addRawSelect("1", null);
            return true;
        } else {
            this.addSelectFunctionStar("count");
            return false;
        }
    }

    public final Map<String, TableRef> getTableRefs() {
        return this.declaredTables.getMap();
    }

    /**
     * Returns the table designated in this select by name.
     * 
     * @param name a table name or an alias, eg "OBSERVATION" or "art2".
     * @return the table named <code>name</code>.
     * @throws IllegalArgumentException if <code>name</code> is unknown to this select.
     */
    public final SQLTable getTable(final String name) {
        return this.getTableRef(name).getTable();
    }

    public final TableRef getTableRef(final String alias) {
        final TableRef res = this.declaredTables.getAliasedTable(alias);
        if (res == null)
            throw new IllegalArgumentException("alias not in this select : " + alias);
        return res;
    }

    /**
     * Return the alias for the passed table.
     * 
     * @param t a table.
     * @return the alias for <code>t</code>, or <code>null</code> if <code>t</code> is not exactly
     *         once in this.
     */
    public final TableRef getAlias(final SQLTable t) {
        return this.declaredTables.getAlias(t);
    }

    public final List<TableRef> getAliases(final SQLTable t) {
        return this.declaredTables.getAliases(t);
    }

    public final FieldRef getAlias(final SQLField f) {
        return this.getAlias(f.getTable()).getField(f.getName());
    }

    /**
     * See http://www.postgresql.org/docs/8.2/interactive/sql-syntax-lexical.html#SQL-SYNTAX-
     * IDENTIFIERS
     */
    static final int maxAliasLength = 63;

    /**
     * Return an unused alias in this select.
     * 
     * @param seed the wanted name, eg "tableAlias".
     * @return a unique alias with the maximum possible of <code>seed</code>, eg "tableAl_1234".
     */
    public final String getUniqueAlias(String seed) {
        if (seed.length() > maxAliasLength)
            seed = seed.substring(0, maxAliasLength);

        if (!this.contains(seed)) {
            return seed;
        } else {
            long time = 1;
            for (int i = 0; i < 50; i++) {
                final String res;
                final String cat = seed + "_" + time;
                if (cat.length() > maxAliasLength)
                    res = seed.substring(0, seed.length() - (cat.length() - maxAliasLength)) + "_" + time;
                else
                    res = cat;
                if (!this.contains(res))
                    return res;
                else
                    time += 1;
            }
            // quit
            return null;
        }
    }

    private final FieldRef createRef(final String alias, final SQLField f) {
        return createRef(alias, f, true);
    }

    /**
     * Creates a FieldRef from the passed alias and field.
     * 
     * @param alias the table alias, eg "obs".
     * @param f the field, eg |OBSERVATION.ID_TENSION|.
     * @param mustExist if the table name/alias must already exist in this select.
     * @return the corresponding FieldRef.
     * @throws IllegalArgumentException if <code>mustExist</code> is <code>true</code> and this does
     *         not contain alias.
     */
    private final FieldRef createRef(final String alias, final SQLField f, final boolean mustExist) {
        if (mustExist && !this.contains(alias))
            throw new IllegalArgumentException("unknown alias " + alias);
        return new AliasedField(f, alias);
    }

    /**
     * Return all fields known to this instance. NOTE the fields used in ORDER BY are not returned.
     * 
     * @return all fields known to this instance.
     */
    public final Set<SQLField> getFields() {
        final Set<SQLField> res = new HashSet<SQLField>(this.getSelectFields().size());
        for (final FieldRef f : this.getSelectFields()) {
            if (f != null)
                res.add(f.getField());
        }
        for (final SQLSelectJoin j : getJoins())
            res.addAll(getFields(j.getWhere()));
        res.addAll(getFields(this.getWhere()));
        for (final FieldRef gb : this.groupBy)
            res.add(gb.getField());
        res.addAll(getFields(this.having));
        // MAYBE add order

        return res;
    }

    private static final Set<SQLField> getFields(final Where w) {
        if (w != null) {
            final Set<SQLField> res = new HashSet<SQLField>();
            for (final FieldRef v : w.getFields())
                res.add(v.getField());
            return res;
        } else
            return Collections.emptySet();
    }

}
