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

import org.openconcerto.erp.config.DsnBrutCode;
import org.openconcerto.sql.element.BaseSQLComponent;
import org.openconcerto.sql.element.ConfSQLElement;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.component.ComboLockedMode;
import org.openconcerto.ui.component.ITextCombo;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JTextField;

public class CodeTypeRubriqueBrutSQLElement extends ConfSQLElement {

    public CodeTypeRubriqueBrutSQLElement() {
        super("CODE_TYPE_RUBRIQUE_BRUT", "un code rubrique de brut", "codes rubrique de brut");
    }

    protected List<String> getListFields() {
        final List<String> list = new ArrayList<String>(3);
        list.add("CODE");
        list.add("TYPE");
        list.add("NOM");
        return list;
    }

    protected List<String> getComboFields() {
        final List<String> list = new ArrayList<String>(3);
        list.add("CODE");
        list.add("TYPE");
        list.add("NOM");
        return list;
    }

    public SQLComponent createComponent() {
        return new BaseSQLComponent(this) {

            public void addViews() {
                this.setLayout(new GridBagLayout());
                final GridBagConstraints c = new DefaultGridBagConstraints();

                // Code
                final JLabel labelCode = new JLabel("Code");
                this.add(labelCode, c);
                c.gridx++;
                c.weightx = 1;
                final JTextField textCode = new JTextField();
                this.add(textCode, c);

                // Nom
                c.gridx++;
                c.weightx = 0;
                final JLabel labelNom = new JLabel("Libellé");
                this.add(labelNom, c);
                c.gridx++;
                c.weightx = 1;
                final JTextField textNom = new JTextField();
                this.add(textNom, c);

                // Type
                ITextCombo comboType = new ITextCombo(ComboLockedMode.LOCKED);
                comboType.initCache(Arrays.asList(DsnBrutCode.DsnTypeCodeBrut.PRIME.getName(), DsnBrutCode.DsnTypeCodeBrut.AUTRE.getName(), DsnBrutCode.DsnTypeCodeBrut.REMUNERATION.getName()));
                c.gridx++;
                c.weightx = 0;
                final JLabel labelType = new JLabel("Type");
                this.add(labelType, c);
                c.gridx++;
                c.weightx = 1;
                this.add(comboType, c);

                this.addRequiredSQLObject(comboType, "TYPE");
                this.addRequiredSQLObject(textNom, "NOM");
                this.addRequiredSQLObject(textCode, "CODE");
            }
        };
    }

    @Override
    protected String createCode() {
        return "humanresources.rubriquebrut.code";
    }
}
