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

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.Log;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.graph.DatabaseGraph;
import org.openconcerto.sql.model.graph.Link;
import org.openconcerto.sql.model.graph.Link.Direction;
import org.openconcerto.sql.model.graph.Step;
import org.openconcerto.utils.NumberUtils;
import org.openconcerto.utils.Value;
import org.openconcerto.utils.cc.HashingStrategy;
import org.openconcerto.utils.convertor.StringClobConvertor;

import java.math.BigDecimal;
import java.sql.Clob;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/**
 * A class that represent a row of a table. The row might not acutally exists in the database, and
 * it might not define all the fields.
 * 
 * <table border="1">
 * <caption>Primary Key</caption> <thead>
 * <tr>
 * <th><code>ID</code> value</th>
 * <th>{@link #hasID()}</th>
 * <th>{@link #getIDNumber()}</th>
 * <th>{@link #isUndefined()}</th>
 * </tr>
 * </thead> <tbody>
 * <tr>
 * <th>∅</th>
 * <td><code>false</code></td>
 * <td><code>null</code></td>
 * <td><code>false</code></td>
 * </tr>
 * <tr>
 * <th><code>null</code></th>
 * <td><code>false</code> :<br/>
 * no row in the DB can have a <code>null</code> primary key</td>
 * <td><code>null</code></td>
 * <td><code>false</code><br/>
 * (even if getUndefinedIDNumber() is <code>null</code>, see method documentation)</td>
 * </tr>
 * <tr>
 * <th><code>instanceof Number</code></th>
 * <td><code>true</code></td>
 * <td><code>Number</code></td>
 * <td>if equals <code>getUndefinedID()</code></td>
 * </tr>
 * <tr>
 * <th><code>else</code></th>
 * <td><code>ClassCastException</code></td>
 * <td><code>ClassCastException</code></td>
 * <td><code>ClassCastException</code></td>
 * </tr>
 * </tbody>
 * </table>
 * <br/>
 * <table border="1">
 * <caption>Foreign Keys</caption> <thead>
 * <tr>
 * <th><code>ID</code> value</th>
 * <th>{@link #getForeignIDNumber(String)}</th>
 * <th>{@link #isForeignEmpty(String)}</th>
 * </tr>
 * </thead> <tbody>
 * <tr>
 * <th>∅</th>
 * <td><code>Exception</code></td>
 * <td><code>Exception</code></td>
 * </tr>
 * <tr>
 * <th><code>null</code></th>
 * <td><code>null</code></td>
 * <td>if equals <code>getUndefinedID()</code></td>
 * </tr>
 * <tr>
 * <th><code>instanceof Number</code></th>
 * <td><code>Number</code></td>
 * <td>if equals <code>getUndefinedID()</code></td>
 * </tr>
 * <tr>
 * <tr>
 * <th><code>instanceof SQLRowValues</code></th>
 * <td><code>getIDNumber()</code></td>
 * <td><code>isUndefined()</code></td>
 * </tr>
 * <th><code>else</code></th>
 * <td><code>ClassCastException</code></td>
 * <td><code>ClassCastException</code></td>
 * </tr>
 * </tbody>
 * </table>
 * 
 * @author Sylvain CUAZ
 */
public abstract class SQLRowAccessor implements SQLData {

    @Deprecated
    static public final String ACCESS_DB_IF_NEEDED_PROP = "SQLRowAccessor.accessDBIfNeeded";
    static private final boolean ACCESS_DB_IF_NEEDED = Boolean.parseBoolean(System.getProperty(ACCESS_DB_IF_NEEDED_PROP, "false"));

    public static boolean getAccessDBIfNeeded() {
        return ACCESS_DB_IF_NEEDED;
    }

    static private final HashingStrategy<SQLRowAccessor> ROW_STRATEGY = new HashingStrategy<SQLRowAccessor>() {
        @Override
        public int computeHashCode(SQLRowAccessor object) {
            return object.hashCodeAsRow();
        }

        @Override
        public boolean equals(SQLRowAccessor object1, SQLRowAccessor object2) {
            return object1.equalsAsRow(object2);
        }
    };

    /**
     * A strategy to compare instances as {@link SQLRow}, i.e. only {@link #getTable() table} and
     * {@link #getID() id}.
     * 
     * @return a strategy.
     * @see #equalsAsRow(SQLRowAccessor)
     */
    public static final HashingStrategy<SQLRowAccessor> getRowStrategy() {
        return ROW_STRATEGY;
    }

    static public final Set<Number> getIDs(final Collection<? extends SQLRowAccessor> rows) {
        return getIDs(rows, new HashSet<Number>());
    }

    static public final <C extends Collection<? super Number>> C getIDs(final Collection<? extends SQLRowAccessor> rows, final C res) {
        for (final SQLRowAccessor r : rows)
            res.add(r.getIDNumber());
        return res;
    }

    private final SQLTable table;

    protected SQLRowAccessor(SQLTable table) {
        super();
        if (table == null)
            throw new NullPointerException("null SQLTable");
        this.table = table;
    }

    public final SQLTable getTable() {
        return this.table;
    }

    /**
     * Whether this row has a Number for the primary key.
     * 
     * @return <code>true</code> if the value of the primary key is specified and is a non
     *         <code>null</code> number, <code>false</code> if the value isn't specified or if it's
     *         <code>null</code>.
     * @throws ClassCastException if value is not <code>null</code> and not a {@link Number}.
     */
    public final boolean hasID() throws ClassCastException {
        return this.getIDNumber() != null;
    }

    /**
     * Returns the ID of the represented row.
     * 
     * @return the ID, or {@link SQLRow#NONEXISTANT_ID} if this row is not linked to the DB.
     */
    public abstract int getID();

    public abstract Number getIDNumber();

    /**
     * Whether this row is the undefined row. Return <code>false</code> if both the
     * {@link #getIDNumber() ID} and {@link SQLTable#getUndefinedIDNumber()} are <code>null</code>
     * since no row can have <code>null</code> primary key in the database. IOW when
     * {@link SQLTable#getUndefinedIDNumber()} is <code>null</code> the empty
     * <strong>foreign</strong> keys are <code>null</code>.
     * 
     * @return <code>true</code> if the ID is specified, not <code>null</code> and is equal to the
     *         {@link SQLTable#getUndefinedIDNumber() undefined} ID.
     */
    public final boolean isUndefined() {
        final Number id = this.getIDNumber();
        return id != null && id.intValue() == this.getTable().getUndefinedID();
    }

    /**
     * Est ce que cette ligne est archivée.
     * 
     * @return <code>true</code> si la ligne était archivée lors de son instanciation.
     */
    public final boolean isArchived() {
        return this.isArchived(true);
    }

    protected final boolean isArchived(final boolean allowDBAccess) {
        // si il n'y a pas de champs archive, elle n'est pas archivée
        final SQLField archiveField = this.getTable().getArchiveField();
        if (archiveField == null)
            return false;
        final Object archiveVal = this.getRequiredObject(archiveField.getName(), allowDBAccess);
        if (archiveField.getType().getJavaType().equals(Boolean.class))
            return ((Boolean) archiveVal).booleanValue();
        else
            return ((Number) archiveVal).intValue() > 0;
    }

    /**
     * Creates an SQLRow from these values, without any DB access.
     * 
     * @return an SQLRow with the same values as this.
     */
    public abstract SQLRow asRow();

    /**
     * Creates an SQLRowValues from these values, without any DB access.
     * 
     * @return an SQLRowValues with the same values as this.
     */
    public abstract SQLRowValues asRowValues();

    /**
     * Creates an SQLRowValues with just this ID, and no other values.
     * 
     * @return an empty SQLRowValues.
     */
    public final SQLRowValues createEmptyUpdateRow() {
        return new SQLRowValues(this.getTable()).setID(this.getIDNumber());
    }

    /**
     * Return the fields defined by this instance.
     * 
     * @return a Set of field names.
     */
    public abstract Set<String> getFields();

    public abstract Object getObject(String fieldName);

    /**
     * Return the value for the passed field only if already present in this instance.
     * 
     * @param fieldName a field name.
     * @return the existing value for the passed field.
     * @throws IllegalArgumentException if there's no value for the passed field.
     */
    public final Object getContainedObject(String fieldName) throws IllegalArgumentException {
        return this.getObject(fieldName, true);
    }

    protected final Object getRequiredObject(String fieldName, final boolean allowDBAccess) throws IllegalArgumentException {
        // SQLRowValues cannot add a field value, so required means mustBePresent
        // SQLRow.getOject() can add and also checks whether the passed field is in its table, i.e.
        // fields are always required.
        return this.getObject(fieldName, this instanceof SQLRowValues || !allowDBAccess);
    }

    // MAYBE change paramter to enum MissingMode = THROW_EXCEPTION, ADD, RETURN_NULL
    public final Object getObject(String fieldName, final boolean mustBePresent) throws IllegalArgumentException {
        if (mustBePresent && !this.getFields().contains(fieldName))
            throw new IllegalArgumentException("Field " + fieldName + " not present in this : " + this.getFields() + " table " + this.getTable().getName());
        return this.getObject(fieldName);
    }

    /**
     * All objects in this row.
     * 
     * @return an immutable map.
     */
    public abstract Map<String, Object> getAbsolutelyAll();

    public final Map<String, Object> getValues(final SQLTable.VirtualFields vFields) {
        return this.getValues(this.getTable().getFieldsNames(vFields));
    }

    public final Map<String, Object> getValues(final Collection<String> fields) {
        return this.getValues(fields, false);
    }

    /**
     * Return the values of this row for the passed fields.
     * 
     * @param fields the keys.
     * @param includeMissingKeys <code>true</code> if a field only in the parameter should be
     *        returned with a <code>null</code> value (i.e. the result might contains fields not in
     *        {@link #getFields()}), <code>false</code> to not include it in the result (i.e. the
     *        fields of the result will be a subset of {@link #getFields()}).
     * @return the values of the passed fields.
     */
    public final Map<String, Object> getValues(final Collection<String> fields, final boolean includeMissingKeys) {
        final Map<String, Object> res = new LinkedHashMap<String, Object>();
        final Set<String> thisFields = this.getFields();
        for (final String f : fields) {
            if (includeMissingKeys || thisFields.contains(f))
                res.put(f, this.getObject(f));
        }
        return res;
    }

    /**
     * Retourne le champ nommé <code>field</code> de cette ligne. Cette méthode formate la valeur en
     * fonction de son type, par exemple une date sera localisée.
     * 
     * @param field le nom du champ que l'on veut.
     * @return la valeur du champ sous forme de chaine, ou <code>null</code> si la valeur est NULL.
     */
    public final String getString(String field) {
        String result = null;
        Object obj = this.getObject(field);
        if (obj == null) {
            result = null;
        } else if (obj instanceof Date) {
            DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault());
            result = df.format((Date) obj);
        } else if (obj instanceof Clob) {
            try {
                result = StringClobConvertor.INSTANCE.unconvert((Clob) obj);
            } catch (Exception e) {
                e.printStackTrace();
                result = obj.toString();
            }
        } else {
            result = obj.toString();
        }
        return result;
    }

    /**
     * Retourne le champ nommé <code>field</code> de cette ligne.
     * 
     * @param field le nom du champ que l'on veut.
     * @return la valeur du champ sous forme d'int, ou <code>0</code> si la valeur est NULL.
     */
    public final int getInt(String field) {
        return getObjectAs(field, Number.class).intValue();
    }

    public final long getLong(String field) {
        return getObjectAs(field, Number.class).longValue();
    }

    public final float getFloat(String field) {
        return getObjectAs(field, Number.class).floatValue();
    }

    public final Boolean getBoolean(String field) {
        return getObjectAs(field, Boolean.class);
    }

    public final BigDecimal getBigDecimal(String field) {
        return getObjectAs(field, BigDecimal.class);
    }

    public final Calendar getDate(String field) {
        final Date d = this.getObjectAs(field, Date.class);
        if (d == null)
            return null;

        final Calendar cal = Calendar.getInstance();
        cal.setTime(d);
        return cal;
    }

    public final <T> T getObjectAs(String field, Class<T> clazz) {
        T res = null;
        try {
            res = clazz.cast(this.getObject(field));
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Impossible d'accéder au champ " + field + " de la ligne " + this + " en tant que " + clazz.getSimpleName(), e);
        }
        return res;
    }

    /**
     * Returns the foreign table of <i>fieldName</i>.
     * 
     * @param fieldName the name of a foreign field, e.g. "ID_ARTICLE_2".
     * @return the table the field points to (never <code>null</code>), e.g. |ARTICLE|.
     * @throws IllegalArgumentException if <i>fieldName</i> is not a foreign field.
     */
    protected final SQLTable getForeignTable(String fieldName) throws IllegalArgumentException {
        return this.getForeignLink(Collections.singletonList(fieldName)).getTarget();
    }

    protected final Link getForeignLink(final List<String> fieldsNames) throws IllegalArgumentException {
        final DatabaseGraph graph = this.getTable().getDBSystemRoot().getGraph();
        final Link foreignLink = graph.getForeignLink(this.getTable(), fieldsNames);
        if (foreignLink == null)
            throw new IllegalArgumentException(fieldsNames + " are not a foreign key of " + this.getTable());
        return foreignLink;
    }

    /**
     * Return the foreign row, if any, for the passed field.
     * 
     * @param fieldName name of the foreign field.
     * @return <code>null</code> if the value of <code>fieldName</code> is <code>null</code>,
     *         otherwise a SQLRowAccessor with the value of <code>fieldName</code> as its ID.
     * @throws IllegalArgumentException if fieldName is not a foreign field.
     */
    public abstract SQLRowAccessor getForeign(String fieldName);

    /**
     * Return the non empty foreign row, if any, for the passed field.
     * 
     * @param fieldName name of the foreign field.
     * @return <code>null</code> if the value of <code>fieldName</code> is
     *         {@link #isForeignEmpty(String) empty}, otherwise a SQLRowAccessor with the value of
     *         <code>fieldName</code> as its ID.
     * @throws IllegalArgumentException if fieldName is not a foreign field or if the field isn't
     *         specified.
     */
    public final SQLRowAccessor getNonEmptyForeign(String fieldName) {
        if (this.isForeignEmpty(fieldName)) {
            return null;
        } else {
            final SQLRowAccessor res = this.getForeign(fieldName);
            assert res != null;
            return res;
        }
    }

    /**
     * Return the ID of a foreign row.
     * 
     * @param fieldName name of the foreign field.
     * @return the value of <code>fieldName</code>, {@link SQLRow#NONEXISTANT_ID} if
     *         <code>null</code>.
     * @throws IllegalArgumentException if fieldName is not a foreign field.
     */
    public final int getForeignID(String fieldName) throws IllegalArgumentException {
        final Number res = this.getForeignIDNumber(fieldName);
        return res == null ? SQLRow.NONEXISTANT_ID : res.intValue();
    }

    /**
     * Return the ID of a foreign row. NOTE : there's two cases when the result can be
     * <code>null</code> :
     * <ol>
     * <li><code>field</code> is defined and has the value <code>null</code></li>
     * <li><code>field</code> is defined and has an SQLRowValues value without an ID</li>
     * </ol>
     * In the second case, <code>field</code> is *not* {@link #isForeignEmpty(String) empty}, an ID
     * is just missing.
     * 
     * @param fieldName name of the foreign field.
     * @return the value of <code>fieldName</code> or {@link #getIDNumber()} if the value is a
     *         {@link SQLRowValues}, <code>null</code> if the actual value is.
     * @throws IllegalArgumentException if fieldName is not a foreign field or if the field isn't
     *         specified.
     */
    public final Number getForeignIDNumber(String fieldName) throws IllegalArgumentException {
        final Value<Number> res = getForeignIDNumberValue(fieldName);
        return res.hasValue() ? res.getValue() : null;
    }

    /**
     * Return the ID of a foreign row.
     * 
     * @param fieldName name of the foreign field.
     * @return {@link Value#getNone()} if there's a {@link SQLRowValues} without
     *         {@link SQLRowValues#hasID() ID}, otherwise the value of <code>fieldName</code> or
     *         {@link #getIDNumber()} if the value is a {@link SQLRowValues}, never
     *         <code>null</code> (the {@link Value#getValue()} is <code>null</code> when
     *         <code>fieldName</code> is).
     * @throws IllegalArgumentException if fieldName is not a foreign field or if the field isn't
     *         specified.
     */
    public final Value<Number> getForeignIDNumberValue(final String fieldName) throws IllegalArgumentException {
        fetchIfNeeded(fieldName);
        // don't use getForeign() to avoid creating a SQLRow
        final Object val = this.getContainedObject(fieldName);
        if (val instanceof SQLRowValues) {
            final SQLRowValues vals = (SQLRowValues) val;
            return vals.hasID() ? Value.getSome(vals.getIDNumber()) : Value.<Number> getNone();
        } else {
            if (!this.getTable().getField(fieldName).isForeignKey())
                throw new IllegalArgumentException(fieldName + "is not a foreign key of " + this.getTable());
            return Value.getSome((Number) val);
        }
    }

    private void fetchIfNeeded(String fieldName) {
        if (getAccessDBIfNeeded() && (this instanceof SQLRow) && !getFields().contains(fieldName)) {
            assert false : "Missing " + fieldName + " in " + this;
            Log.get().log(Level.WARNING, "Missing " + fieldName + " in " + this, new IllegalStateException());
            ((SQLRow) this).fetchValues();
        }
    }

    /**
     * Whether the passed field is empty.
     * 
     * @param fieldName name of the foreign field.
     * @return <code>true</code> if {@link #getForeignIDNumber(String)} is the
     *         {@link SQLTable#getUndefinedIDNumber()}.
     */
    public final boolean isForeignEmpty(String fieldName) {
        final Value<Number> fID = this.getForeignIDNumberValue(fieldName);
        if (!fID.hasValue()) {
            // a foreign row values without ID is *not* undefined
            return false;
        } else {
            // keep getForeignTable at the 1st line since it does the check
            final SQLTable foreignTable = this.getForeignTable(fieldName);
            final Number undefID = foreignTable.getUndefinedIDNumber();
            return NumberUtils.areNumericallyEqual(fID.getValue(), undefID);
        }
    }

    public abstract Collection<? extends SQLRowAccessor> getReferentRows();

    public abstract Collection<? extends SQLRowAccessor> getReferentRows(final SQLField refField);

    public abstract Collection<? extends SQLRowAccessor> getReferentRows(final SQLTable refTable);

    public final Collection<? extends SQLRowAccessor> followLink(final Link l) {
        return this.followLink(l, Direction.ANY);
    }

    /**
     * Return the rows linked to this one by <code>l</code>.
     * 
     * @param l the link to follow.
     * @param direction which way, one can pass {@link Direction#ANY} to infer it except for self
     *        references.
     * @return the rows linked to this one.
     * @see Step#create(SQLTable, SQLField, Direction)
     */
    public abstract Collection<? extends SQLRowAccessor> followLink(final Link l, final Direction direction);

    /**
     * Returns a java object modeling this row.
     * 
     * @return an instance modeling this row or <code>null</code> if there's no class to model this
     *         table.
     * @see org.openconcerto.sql.element.SQLElement#getModelObject(SQLRowAccessor)
     */
    public final Object getModelObject() {
        final SQLElement foreignElement = Configuration.getInstance().getDirectory().getElement(this.getTable());
        return (foreignElement == null) ? null : foreignElement.getModelObject(this);
    }

    public final BigDecimal getOrder() {
        return (BigDecimal) this.getObject(this.getTable().getOrderField().getName());
    }

    public final Calendar getCreationDate() {
        final SQLField f = getTable().getCreationDateField();
        return f == null ? null : this.getDate(f.getName());
    }

    public final Calendar getModificationDate() {
        final SQLField f = getTable().getModifDateField();
        return f == null ? null : this.getDate(f.getName());
    }

    // avoid costly asRow()
    public final boolean equalsAsRow(SQLRowAccessor o) {
        return this.getTable() == o.getTable() && this.getID() == o.getID();
    }

    // avoid costly asRow()
    public final int hashCodeAsRow() {
        return this.getTable().hashCode() + this.getID();
    }

    public SQLRowValues getMergedRowValuesFromDatabase() {
        SQLRowValues result = this.asRowValues();
        if (this.hasID()) {
            SQLRow r = result.getTable().getRow(getID());
            for (String f : r.getFields()) {
                if (!result.getFields().contains(f)) {
                    result.put(f, r.getObject(f));
                }
            }
        }
        return result;
    }

}
