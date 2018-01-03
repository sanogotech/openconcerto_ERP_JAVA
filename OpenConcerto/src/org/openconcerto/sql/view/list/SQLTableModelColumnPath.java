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
 
 package org.openconcerto.sql.view.list;

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElementDirectory;
import org.openconcerto.sql.model.FieldPath;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.graph.Path;
import org.openconcerto.sql.request.RowItemDesc;
import org.openconcerto.sql.request.SQLFieldTranslator;
import org.openconcerto.utils.CollectionUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * A column displaying one and only one field.
 * 
 * @author Sylvain
 */
public class SQLTableModelColumnPath extends SQLTableModelColumn {

    public static final RowItemDesc getDescFor(final SQLField field, SQLElementDirectory dir) {
        if (dir == null && Configuration.getInstance() != null)
            dir = Configuration.getInstance().getDirectory();
        final RowItemDesc res = dir == null ? SQLFieldTranslator.NULL_DESC : dir.getTranslator().getDescFor(field.getTable(), field.getName());
        if (res.equals(SQLFieldTranslator.NULL_DESC))
            return SQLFieldTranslator.getDefaultDesc(field);
        else
            return res;
    }

    private final FieldPath p;
    private final SQLElementDirectory dir;
    private boolean editable;

    public SQLTableModelColumnPath(Path p, String fieldName, final String name) {
        this(new FieldPath(p, fieldName), name, null);
    }

    public SQLTableModelColumnPath(SQLField f) {
        this(new FieldPath(f));
    }

    public SQLTableModelColumnPath(FieldPath fp) {
        this(fp, null, null);
    }

    public SQLTableModelColumnPath(FieldPath fp, final String name, final SQLElementDirectory dir) {
        super(name == null ? getDescFor(fp.getField(), dir).getTitleLabel() : name);
        this.p = fp;
        this.dir = dir;
        this.editable = true;
    }

    @Override
    public String getIdentifier() {
        return this.p.toString();
    }

    @Override
    public String getToolTip() {
        final List<String> humanPath = new ArrayList<String>(this.p.getPath().length());
        for (final SQLField f : this.p.getPath().getSingleFields()) {
            humanPath.add(getDescFor(f, this.dir).getLabel());
        }
        humanPath.add(getDescFor(this.p.getField(), this.dir).getLabel());
        return CollectionUtils.join(humanPath, IListe.SEP);
    }

    public SQLField getField() {
        return this.p.getField();
    }

    @Override
    public Set<SQLField> getFields() {
        return Collections.singleton(this.getField());
    }

    @Override
    public Set<FieldPath> getPaths() {
        return Collections.singleton(this.p);
    }

    @Override
    protected Class<?> getValueClass_() {
        return this.getField().getType().getJavaType();
    }

    @Override
    public boolean isEditable() {
        // only edit the primary table
        return this.editable && this.p.getPath().length() == 0;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    @Override
    protected Object show_(final SQLRowAccessor r) {
        return this.p.getObject(r.asRowValues());
    }

    @Override
    protected void put_(ListSQLLine l, Object value) {
        // value == null if the user emptied the cell (see GenericEditor.stopCellEditing())
        if (value == null && this.getField().isNullable() != Boolean.TRUE)
            value = SQLRowValues.SQL_DEFAULT;
        final SQLRowValues ourVals = l.getRow().followPath(this.p.getPath());
        final SQLRowValues vals = new SQLRowValues(this.p.getPath().getLast()).put(this.p.getFieldName(), value);
        if (ourVals != null && ourVals.hasID())
            vals.setID(ourVals.getIDNumber());
        vals.getGraph().freeze();
        try {
            l.getSrc().commit(l, this.p.getPath(), vals);
        } catch (SQLException e) {
            throw new IllegalStateException("unable to set " + this + " to " + value, e);
        }
    }
}
