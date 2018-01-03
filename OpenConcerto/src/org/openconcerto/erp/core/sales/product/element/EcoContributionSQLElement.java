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
 
 package org.openconcerto.erp.core.sales.product.element;

import org.openconcerto.sql.element.BaseSQLComponent;
import org.openconcerto.sql.element.ConfSQLElement;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.sql.sqlobject.SQLRequestComboBox;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.utils.ListMap;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JTextField;

public class EcoContributionSQLElement extends ConfSQLElement {

    public EcoContributionSQLElement() {
        super("ECO_CONTRIBUTION");
    }

    protected List<String> getListFields() {
        final List<String> list = new ArrayList<String>(2);
        list.add("ID_FAMILLE_ECO_CONTRIBUTION");
        list.add("CODE");
        list.add("NOM");
        list.add("TAUX");
        return list;
    }

    protected List<String> getComboFields() {
        final List<String> list = new ArrayList<String>(2);
        list.add("CODE");
        list.add("NOM");
        list.add("TAUX");
        return list;
    }

    @Override
    public ListMap<String, String> getShowAs() {
        return ListMap.singleton(null, "CODE", "NOM");

    }

    public SQLComponent createComponent() {
        return new BaseSQLComponent(this) {

            public void addViews() {
                this.setLayout(new GridBagLayout());
                final GridBagConstraints c = new DefaultGridBagConstraints();

                // Code
                final JLabel labelCode = new JLabel(getLabelFor("CODE"));
                c.weightx = 0;
                this.add(labelCode, c);
                c.gridx++;
                c.weightx = 1;
                final JTextField textCode = new JTextField();
                this.add(textCode, c);

                // Nom
                c.gridx++;
                c.weightx = 0;
                final JLabel labelNom = new JLabel(getLabelFor("NOM"));
                this.add(labelNom, c);
                c.gridx++;
                c.weightx = 1;
                final JTextField textNom = new JTextField();
                this.add(textNom, c);

                // Famille
                c.gridy++;
                c.gridx = 0;
                c.weightx = 0;
                final JLabel labelFamille = new JLabel(getLabelFor("ID_FAMILLE_ECO_CONTRIBUTION"));
                this.add(labelFamille, c);
                c.gridx++;
                c.weightx = 1;
                ElementComboBox combo = new ElementComboBox();
                this.add(combo, c);

                // Taux
                c.gridx++;
                c.weightx = 0;
                final JLabel labelTaux = new JLabel(getLabelFor("TAUX"));
                this.add(labelTaux, c);
                c.gridx++;
                c.weightx = 1;
                final JTextField textTaux = new JTextField();
                this.add(textTaux, c);

                this.addRequiredSQLObject(textTaux, "TAUX");
                this.addSQLObject(combo, "ID_FAMILLE_ECO_CONTRIBUTION");
                this.addRequiredSQLObject(textNom, "NOM");
                this.addRequiredSQLObject(textCode, "CODE");
            }
        };
    }
}
