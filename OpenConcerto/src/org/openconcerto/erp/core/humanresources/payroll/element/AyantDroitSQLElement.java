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

import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.erp.core.humanresources.payroll.ui.AyantDroitTable;
import org.openconcerto.sql.element.BaseSQLComponent;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.sqlobject.SQLRequestComboBox;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.JDate;
import org.openconcerto.utils.ListMap;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class AyantDroitSQLElement extends ComptaSQLConfElement {

    public AyantDroitSQLElement() {
        super("AYANT_DROIT", "un ayant droit", "ayants droit");
    }

    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("ID_SALARIE");
        l.add("NOM");
        l.add("PRENOMS");
        return l;
    }

    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NOM");
        l.add("PRENOMS");
        return l;
    }

    @Override
    protected String createCode() {
        return createCodeFromPackage() + ".ayantdroit";
    }

    @Override
    public ListMap<String, String> getShowAs() {
        return ListMap.singleton(null, "NOM");
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openconcerto.devis.SQLElement#getComponent()
     */
    public SQLComponent createComponent() {
        return new BaseSQLComponent(this) {

            AyantDroitTable table = new AyantDroitTable();

            public void addViews() {

                this.setLayout(new GridBagLayout());
                final GridBagConstraints c = new DefaultGridBagConstraints();

                /***********************************************************************************
                 * Renseignements
                 **********************************************************************************/
                JPanel panelInfos = new JPanel();
                panelInfos.setBorder(BorderFactory.createTitledBorder("Renseignements (S21.G00.73)"));
                panelInfos.setLayout(new GridBagLayout());

                // Code
                JLabel labelSal = new JLabel(getLabelFor("ID_SALARIE"));
                SQLRequestComboBox boxSal = new SQLRequestComboBox();
                panelInfos.add(labelSal, c);
                c.gridx++;
                c.weightx = 1;
                panelInfos.add(boxSal, c);
                this.addRequiredSQLObject(boxSal, "ID_SALARIE");
                c.weightx = 0;

                // Nom
                JLabel labelNom = new JLabel(getLabelFor("NOM"));
                JTextField textNom = new JTextField();
                c.gridy++;
                c.gridx = 0;
                c.weightx = 0;
                panelInfos.add(labelNom, c);
                c.gridx++;
                c.weightx = 1;
                panelInfos.add(textNom, c);
                this.addRequiredSQLObject(textNom, "NOM");
                c.weightx = 0;
                // Ref
                JLabel labelPrenoms = new JLabel(getLabelFor("PRENOMS"));
                JTextField textPrenoms = new JTextField();
                c.gridx++;
                panelInfos.add(labelPrenoms, c);
                c.gridx++;
                c.weightx = 1;
                panelInfos.add(textPrenoms, c);
                this.addRequiredSQLObject(textPrenoms, "PRENOMS");

                c.weightx = 0;

                // Nom
                JLabel labelCO = new JLabel(getLabelFor("CODE_OPTION"));
                JTextField textCO = new JTextField();
                c.gridy++;
                c.gridx = 0;
                c.weightx = 0;
                panelInfos.add(labelCO, c);
                c.gridx++;
                c.weightx = 1;
                panelInfos.add(textCO, c);
                this.addSQLObject(textCO, "CODE_OPTION");

                c.weightx = 0;

                // Ref
                JLabel labelADT = new JLabel(getLabelFor("ID_AYANT_DROIT_TYPE"));
                SQLRequestComboBox boxType = new SQLRequestComboBox();
                c.gridx++;
                panelInfos.add(labelADT, c);
                c.gridx++;
                c.weightx = 1;
                panelInfos.add(boxType, c);
                this.addRequiredSQLObject(boxType, "ID_AYANT_DROIT_TYPE");

                // Nom
                JLabel labelD = new JLabel(getLabelFor("DATE_DEBUT_RATTACHEMENT"));
                JDate d = new JDate();
                c.gridy++;
                c.gridx = 0;
                c.weightx = 0;
                panelInfos.add(labelD, c);
                c.gridx++;
                c.weightx = 1;
                panelInfos.add(d, c);
                this.addRequiredSQLObject(d, "DATE_DEBUT_RATTACHEMENT");

                c.weightx = 0;

                // Ref
                JLabel labelDN = new JLabel(getLabelFor("DATE_NAISSANCE"));
                JDate dn = new JDate();
                c.gridx++;
                panelInfos.add(labelDN, c);
                c.gridx++;
                c.weightx = 1;
                panelInfos.add(dn, c);
                this.addRequiredSQLObject(dn, "DATE_NAISSANCE");

                // Nom
                JLabel labelNIR = new JLabel(getLabelFor("NIR"));
                JTextField nir = new JTextField();
                c.gridy++;
                c.gridx = 0;
                c.weightx = 0;
                panelInfos.add(labelNIR, c);
                c.gridx++;
                c.weightx = 1;
                panelInfos.add(nir, c);
                this.addSQLObject(nir, "NIR");

                c.weightx = 0;

                // // Ref
                // JLabel labelNIRO = new JLabel(getLabelFor("NIR_OUVRANT_DROIT"));
                // JTextField niro = new JTextField();
                // c.gridx++;
                // panelInfos.add(labelNIRO, c);
                // c.gridx++;
                // c.weightx = 1;
                // panelInfos.add(niro, c);
                // this.addRequiredSQLObject(niro, "NIR_OUVRANT_DROIT");

                // Nom
                JLabel labelCodeAffil = new JLabel(getLabelFor("CODE_ORGANISME_AFFILIATION"));
                JTextField codeAffil = new JTextField();
                c.gridy++;
                c.gridx = 0;
                c.weightx = 0;
                panelInfos.add(labelCodeAffil, c);
                c.gridx++;
                c.weightx = 1;
                panelInfos.add(codeAffil, c);
                this.addSQLObject(codeAffil, "CODE_ORGANISME_AFFILIATION");

                c.weightx = 0;

                // Ref
                JLabel labelDATEFIN = new JLabel(getLabelFor("DATE_FIN_RATTACHEMENT"));
                JDate dFin = new JDate();
                c.gridx++;
                panelInfos.add(labelDATEFIN, c);
                c.gridx++;
                c.weightx = 1;
                panelInfos.add(dFin, c);
                this.addRequiredSQLObject(dFin, "DATE_FIN_RATTACHEMENT");

                c.weightx = 0;

                c.gridx = 0;
                c.gridy++;
                c.fill = GridBagConstraints.BOTH;
                c.gridwidth = GridBagConstraints.REMAINDER;
                c.weighty = 1;
                panelInfos.add(table, c);

                c.gridx = 0;
                c.gridy++;
                c.fill = GridBagConstraints.BOTH;
                c.weightx = 1;
                c.weighty = 1;
                this.add(panelInfos, c);

               

            }

            @Override
            public int insert(SQLRow order) {

                int id = super.insert(order);

                table.updateField("ID_AYANT_DROIT", id);
                return id;
            }

            @Override
            public void update() {

                int id = getSelectedID();
                super.update();
                table.updateField("ID_AYANT_DROIT", id);
            }

            @Override
            public void select(SQLRowAccessor r) {

                super.select(r);
                if (r != null) {
                    table.insertFrom("ID_AYANT_DROIT", r.getID());
                }
            }
        };
    }
}
