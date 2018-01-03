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
 
 package org.openconcerto.sql.sqlobject.itemview;

import org.openconcerto.sql.element.RIVPanel;
import org.openconcerto.sql.element.SQLComponentItem;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.request.SQLForeignRowItemView;
import org.openconcerto.sql.request.SQLRowItemView;
import org.openconcerto.ui.component.InteractionMode;
import org.openconcerto.ui.valuewrapper.ValueWrapper;
import org.openconcerto.utils.cc.IPredicate;
import org.openconcerto.utils.checks.ChainValidListener;
import org.openconcerto.utils.checks.EmptyListener;
import org.openconcerto.utils.checks.EmptyObj;
import org.openconcerto.utils.checks.EmptyObjFromVO;
import org.openconcerto.utils.checks.EmptyObjHelper;
import org.openconcerto.utils.checks.ValidListener;
import org.openconcerto.utils.checks.ValidState;

import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import javax.swing.SwingUtilities;

/**
 * A RIV delegating most of its workings to a ValueWrapper.
 * 
 * @author Sylvain CUAZ
 * @param <T> type of value
 */
public abstract class VWRowItemView<T> extends BaseRowItemView implements SQLComponentItem {

    private static final String VALUE_PROPNAME = "value";

    private final ValueWrapper<T> wrapper;
    private final PropertyChangeSupport supp;
    private final PropertyChangeListener valueL;
    private EmptyObjHelper helper;

    public VWRowItemView(ValueWrapper<T> wrapper) {
        this.wrapper = wrapper;
        this.supp = new PropertyChangeSupport(this);
        this.valueL = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                fireValueChange(evt);
            }
        };
    }

    public final ValueWrapper<T> getWrapper() {
        return this.wrapper;
    }

    @Override
    protected void init() {
        this.helper = this.createHelper();
    }

    @Override
    public void added(RIVPanel comp, SQLRowItemView v) {
        // re-use SQLComponentItem even though the second parameter is useless
        assert v == this;
        if (getWrapper() instanceof SQLComponentItem) {
            ((SQLComponentItem) getWrapper()).added(comp, v);
        }
    }

    private final EmptyObjHelper createHelper() {
        final EmptyObj eo;
        if (this.getWrapper() instanceof EmptyObj)
            eo = (EmptyObj) this.getWrapper();
        else if (this.getWrapper().getComp() instanceof EmptyObj)
            eo = (EmptyObj) this.getWrapper().getComp();
        else
            eo = new EmptyObjFromVO<T>(this.getWrapper(), this.getEmptyPredicate());

        return new EmptyObjHelper(this, eo);
    }

    /**
     * The predicate testing whether the value is empty or not. This implementation returns
     * {@link EmptyObjFromVO#getDefaultPredicate()}
     * 
     * @return the predicate testing whether the value is empty.
     */
    protected IPredicate<T> getEmptyPredicate() {
        return EmptyObjFromVO.getDefaultPredicate();
    }

    @Override
    public void resetValue() {
        this.getWrapper().resetValue();
    }

    // not final to allow subclass without exactly one field
    @Override
    public void show(SQLRowAccessor r) {
        if (r instanceof SQLRowValues && ((SQLRowValues) r).isDefault(this.getField().getName())) {
            // in update(), DEFAULT means empty, MAYBE add clear() to MutableValueObject
            this.getWrapper().setValue(null);
        } else if (r.getFields().contains(this.getField().getName())) {
            Object object = r.getObject(this.getField().getName());
            // typically views that display foreign rows are ValueWrapper<ID>, so if r contains a
            // row, the call to ValueWrapper.setValue() will fail. So check if the ValueWrapper also
            // implements SQLForeignRowItemView.
            if (object instanceof SQLRowAccessor && this.getWrapper() instanceof SQLForeignRowItemView) {
                ((SQLForeignRowItemView) this.getWrapper()).setValue((SQLRowAccessor) object);
            } else {
                try {
                    @SuppressWarnings("unchecked")
                    final T casted = (T) object;
                    this.getWrapper().setValue(casted);
                } catch (Exception e) {
                    throw new IllegalArgumentException("Cannot set value of  " + this.getWrapper() + " to " + object + " (from " + this.getField() + ")", e);
                }
            }
        }
    }

    // not final to allow subclass without exactly one field
    @Override
    public void update(SQLRowValues vals) {
        vals.put(this.getField().getName(), this.isEmpty() ? SQLRowValues.SQL_DEFAULT : this.getWrapper().getValue());
    }

    @Override
    public final void addValueListener(PropertyChangeListener l) {
        assert SwingUtilities.isEventDispatchThread();
        if (!this.supp.hasListeners(VALUE_PROPNAME)) {
            this.getWrapper().addValueListener(this.valueL);
        }
        this.supp.addPropertyChangeListener(VALUE_PROPNAME, l);
    }

    private final void fireValueChange(PropertyChangeEvent evt) {
        final PropertyChangeEvent ourEvt = new PropertyChangeEvent(this, VALUE_PROPNAME, evt.getOldValue(), evt.getNewValue());
        ourEvt.setPropagationId(evt.getPropagationId());
        this.supp.firePropertyChange(ourEvt);
    }

    @Override
    public final void removeValueListener(PropertyChangeListener l) {
        assert SwingUtilities.isEventDispatchThread();
        this.supp.removePropertyChangeListener(VALUE_PROPNAME, l);
        if (!this.supp.hasListeners(VALUE_PROPNAME)) {
            this.getWrapper().rmValueListener(this.valueL);
        }
    }

    @Override
    public String toString() {
        return super.toString() + " using " + this.getWrapper();
    }

    // *** emptyObj

    @Override
    public final boolean isEmpty() {
        return this.helper.isEmpty();
    }

    @Override
    public final void addEmptyListener(EmptyListener l) {
        this.helper.addEmptyListener(l);
    }

    @Override
    public final void removeEmptyListener(EmptyListener l) {
        this.helper.removeEmptyListener(l);
    }

    // *** validObj

    @Override
    public ValidState getValidState() {
        return this.getWrapper().getValidState();
    }

    @Override
    public final void addValidListener(ValidListener l) {
        this.getWrapper().addValidListener(new ChainValidListener(this, l));
    }

    @Override
    public final void removeValidListener(ValidListener l) {
        this.getWrapper().removeValidListener(new ChainValidListener(this, l));
    }

    @Override
    public final Component getComp() {
        return this.getWrapper().getComp();
    }

    @Override
    public void setEditable(InteractionMode mode) {
        final Component comp = this.getComp();
        if (comp != null) {
            mode.applyTo(comp);
        }
    }
}
