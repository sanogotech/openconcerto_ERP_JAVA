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
 
 package org.openconcerto.erp.core.sales.order.ui;

import org.openconcerto.sql.request.SQLRowItemView;
import org.openconcerto.sql.sqlobject.itemview.RowItemViewComponent;
import org.openconcerto.ui.valuewrapper.ValueChangeSupport;
import org.openconcerto.ui.valuewrapper.ValueWrapper;
import org.openconcerto.utils.checks.ValidListener;
import org.openconcerto.utils.checks.ValidState;
import org.openconcerto.utils.doc.Documented;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;

import javax.swing.JComboBox;
import javax.swing.JComponent;

public class EtatCommandeClientComboBox extends JComboBox implements ValueWrapper<EtatCommandeClient>, Documented, RowItemViewComponent {
    private final ValueChangeSupport<EtatCommandeClient> supp;

    public EtatCommandeClientComboBox() {

        this.supp = new ValueChangeSupport<EtatCommandeClient>(this);
        this.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                supp.fireValueChange();
            }
        });

        for (EtatCommandeClient etat : EtatCommandeClient.values()) {
            addItem(etat);
        }
    }

    @Override
    public ValidState getValidState() {
        return ValidState.getTrueInstance();
    }

    @Override
    public void addValidListener(ValidListener l) {
        this.supp.addValidListener(l);
    }

    @Override
    public void removeValidListener(ValidListener l) {
        this.supp.removeValidListener(l);
    }

    @Override
    public void setValue(EtatCommandeClient val) {
        this.setSelectedItem(val);
    }

    @Override
    public void resetValue() {
        // TODO Auto-generated method stub

    }

    @Override
    public EtatCommandeClient getValue() {
        return (EtatCommandeClient) this.getSelectedItem();
    }

    @Override
    public void addValueListener(PropertyChangeListener l) {
        this.supp.addValueListener(l);
    }

    @Override
    public void rmValueListener(PropertyChangeListener l) {
        this.supp.addValueListener(l);
    }

    @Override
    public void init(SQLRowItemView v) {
        // TODO Auto-generated method stub

    }

    @Override
    public String getGenericDoc() {
        return "";
    }

    @Override
    public String getDocId() {

        return "EtatCommande";
    }

    @Override
    public boolean onScreen() {
        return true;
    }

    @Override
    public boolean isDocTransversable() {
        return false;
    }

    @Override
    public JComponent getComp() {
        return this;
    }

}
