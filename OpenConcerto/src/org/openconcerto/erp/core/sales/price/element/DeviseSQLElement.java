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
 
 package org.openconcerto.erp.core.sales.price.element;

import org.openconcerto.erp.core.finance.accounting.model.Currency;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.BaseSQLComponent;
import org.openconcerto.sql.element.ConfSQLElement;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.component.combo.ISearchableTextCombo;
import org.openconcerto.utils.ListMap;
import org.openconcerto.utils.model.DefaultIListModel;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JTextField;

public class DeviseSQLElement extends ConfSQLElement {

    public DeviseSQLElement(DBRoot root) {
        super(root.getTable("DEVISE"), "une devise", "devises");
    }

    public DeviseSQLElement() {
        this(Configuration.getInstance().getRoot());
    }

    @Override
    public boolean isShared() {
        return true;
    }

    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("CODE");
        return l;
    }

    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("CODE");
        return l;
    }

    @Override
    public ListMap<String, String> getShowAs() {
        ListMap<String, String> map = new ListMap<String, String>();
        map.putCollection(null, "CODE");
        return map;
    }

    public SQLComponent createComponent() {
        return new BaseSQLComponent(this) {

            public void addViews() {
                this.setLayout(new GridBagLayout());
                final GridBagConstraints c = new DefaultGridBagConstraints();

                // Nom
                JLabel labelCode = new JLabel(getLabelFor("CODE"));
                final ISearchableTextCombo textCode = new ISearchableTextCombo(true);
                textCode.initCache(new DefaultIListModel<String>(Currency.ISO_CODES));
                textCode.setSelectedItem("EUR");
                this.add(labelCode, c);
                c.gridx++;
                c.weightx = 1;
                this.add(textCode, c);

                c.gridy++;
                c.gridx = 0;
                JLabel labelTaux = new JLabel(getLabelFor("TAUX"));
                JTextField textTaux = new JTextField();

                c.weightx = 0;
                this.add(labelTaux, c);
                c.gridx++;
                c.weightx = 1;
                this.add(textTaux, c);

                // Nom
                JLabel labelTauxC = new JLabel(getLabelFor("TAUX_COMMERCIAL"));
                JTextField textTauxC = new JTextField();
                c.gridx++;
                c.weightx = 0;
                this.add(labelTauxC, c);
                c.gridx++;
                c.weightx = 1;
                this.add(textTauxC, c);

                this.addView(textCode, "CODE", REQ);
                this.addView(textTaux, "TAUX", REQ);
                this.addView(textTauxC, "TAUX_COMMERCIAL", REQ);

            }
        };
    }
}
