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
 
 package org.openconcerto.erp.core.humanresources.payroll.element;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.sql.element.BaseSQLComponent;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.ui.DefaultGridBagConstraints;

public class CoefficientPrimeSQLElement extends ComptaSQLConfElement {

    public CoefficientPrimeSQLElement() {
        super("COEFF_PRIME", "un coefficient de prime", "coefficients de prime");
    }

    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("PRIME_PERSO");
        l.add("PRIME_RECONSTRUCTION");
        l.add("PRIME_ANCIENNETE");
        l.add("PRIME_DEROULEMENT");
        return l;
    }

    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("PRIME_PERSO");
        l.add("PRIME_RECONSTRUCTION");
        l.add("PRIME_ANCIENNETE");
        l.add("PRIME_DEROULEMENT");
        return l;
    }

    @Override
    public boolean isPrivate() {
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openconcerto.devis.SQLElement#getComponent()
     */
    public SQLComponent createComponent() {
        return new BaseSQLComponent(this) {

            public void addViews() {

                this.setLayout(new GridBagLayout());
                GridBagConstraints c = new DefaultGridBagConstraints();

                // Perso
                JLabel labelPerso = new JLabel(getLabelFor("PRIME_PERSO"));
                labelPerso.setHorizontalAlignment(SwingConstants.RIGHT);
                JTextField textPerso = new JTextField();

                this.add(labelPerso, c);
                c.gridx++;
                c.weightx = 1;
                this.add(textPerso, c);

                // Reconst
                JLabel labelRecons = new JLabel(getLabelFor("PRIME_RECONSTRUCTION"));
                labelRecons.setHorizontalAlignment(SwingConstants.RIGHT);
                JTextField textRecons = new JTextField();

                c.gridx = 0;
                c.gridy++;
                c.weightx = 0;
                this.add(labelRecons, c);
                c.gridx++;
                c.weightx = 1;
                this.add(textRecons, c);

                // Ancien
                JLabel labelAncien = new JLabel(getLabelFor("PRIME_ANCIENNETE"));
                labelAncien.setHorizontalAlignment(SwingConstants.RIGHT);
                JTextField textAncien = new JTextField();

                c.gridx = 0;
                c.gridy++;
                c.weightx = 0;
                this.add(labelAncien, c);
                c.gridx++;
                c.weightx = 1;
                this.add(textAncien, c);

                // Deroulement
                JLabel labelDeroul = new JLabel(getLabelFor("PRIME_DEROULEMENT"));
                labelDeroul.setHorizontalAlignment(SwingConstants.RIGHT);
                JTextField textDeroul = new JTextField();

                c.gridx = 0;
                c.gridy++;
                c.weightx = 0;
                this.add(labelDeroul, c);
                c.gridx++;
                c.weightx = 1;
                this.add(textDeroul, c);

                this.addSQLObject(textDeroul, "PRIME_DEROULEMENT");
                this.addSQLObject(textPerso, "PRIME_PERSO");
                this.addSQLObject(textRecons, "PRIME_RECONSTRUCTION");
                this.addSQLObject(textAncien, "PRIME_ANCIENNETE");
            }
        };

    }

    @Override
    protected String createCode() {
        return createCodeFromPackage() + ".coeffPrime";
    }
}
