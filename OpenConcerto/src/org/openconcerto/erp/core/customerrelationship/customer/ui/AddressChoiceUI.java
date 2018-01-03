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
 
 package org.openconcerto.erp.core.customerrelationship.customer.ui;

import org.openconcerto.sql.element.BaseSQLComponent;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.ui.DefaultGridBagConstraints;

import java.awt.GridBagConstraints;

import javax.swing.JLabel;
import javax.swing.SwingConstants;

public class AddressChoiceUI {
    private ElementComboBox comboAdrF, comboAdrL;

    public AddressChoiceUI() {
        // TODO Auto-generated constructor stub
    }

    public void addToUI(BaseSQLComponent comp, GridBagConstraints c) {
        // Adresse Facturation
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 1;
        final JLabel labelAdresseF = new JLabel(comp.getLabelFor("ID_ADRESSE"));
        labelAdresseF.setHorizontalAlignment(SwingConstants.RIGHT);
        c.weightx = 0;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        comp.add(labelAdresseF, c);

        this.comboAdrF = new ElementComboBox();
        this.comboAdrF.setButtonsVisible(false);

        c.gridx++;
        c.gridwidth = 1;
        c.weightx = 0;
        c.weighty = 0;
        c.fill = GridBagConstraints.NONE;
        comp.add(comboAdrF, c);
        final SQLElement adrElement = comp.getElement().getForeignElement("ID_ADRESSE");
        comboAdrF.init(adrElement, adrElement.getComboRequest(true));
        comboAdrF.getRequest().setWhere(Where.FALSE);
        DefaultGridBagConstraints.lockMinimumSize(comboAdrF);
        comp.addView(comboAdrF, "ID_ADRESSE");
        this.comboAdrF.setAddIconVisible(false);

        // Adresse Livraison
        c.gridx++;
        c.gridwidth = 1;
        final JLabel labelAdrL = new JLabel(comp.getLabelFor("ID_ADRESSE_LIVRAISON"));
        labelAdrL.setHorizontalAlignment(SwingConstants.RIGHT);
        c.weightx = 0;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        comp.add(labelAdrL, c);

        this.comboAdrL = new ElementComboBox();
        this.comboAdrL.setButtonsVisible(false);

        c.gridx++;
        c.gridwidth = 1;
        c.weightx = 1;
        c.weighty = 0;
        c.fill = GridBagConstraints.NONE;
        comboAdrL.init(adrElement, adrElement.getComboRequest(true));
        comboAdrL.getRequest().setWhere(Where.FALSE);
        this.comboAdrL.setAddIconVisible(false);
        DefaultGridBagConstraints.lockMinimumSize(comboAdrL);
        comp.add(this.comboAdrL, c);
        comp.addView(comboAdrL, "ID_ADRESSE_LIVRAISON");

    }

    public ElementComboBox getComboAdrF() {
        return comboAdrF;
    }

    public ElementComboBox getComboAdrL() {
        return comboAdrL;
    }
}
