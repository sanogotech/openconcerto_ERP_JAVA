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

import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.ui.component.InteractionMode;
import org.openconcerto.utils.checks.EmptyObj;
import org.openconcerto.utils.checks.ValidObject;

import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

/**
 * An element of a SQLRowView, it can be either a field (DESIGNATION) or not.
 * 
 * @author Sylvain CUAZ
 */
public interface SQLRowItemView extends EmptyObj, ValidObject {

    // eg DESIGNATION, OBSERVATIONS
    public String getSQLName();

    // null if no fields, Exception if more than one
    public SQLField getField();

    public List<SQLField> getFields();

    // TODO rename en reset()
    public void resetValue();

    public void show(SQLRowAccessor r);

    public void insert(SQLRowValues vals);

    public void update(SQLRowValues vals);

    public void setEditable(InteractionMode mode);

    public Component getComp();

    /**
     * Add a listener to be notified when this changes value. NOTE : this instance might have a
     * value expensive to compute, so it can choose to leave it out from the
     * {@link PropertyChangeEvent}.
     * 
     * @param l the listener.
     */
    public void addValueListener(PropertyChangeListener l);

    public void removeValueListener(PropertyChangeListener l);
}
