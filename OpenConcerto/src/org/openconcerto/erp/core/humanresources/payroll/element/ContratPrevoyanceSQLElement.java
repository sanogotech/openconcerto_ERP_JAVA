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
import org.openconcerto.erp.core.humanresources.payroll.ui.ContratPrevRubriqueTable;
import org.openconcerto.erp.core.humanresources.payroll.ui.ContratPrevSalarieTable;
import org.openconcerto.sql.element.BaseSQLComponent;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.JDate;
import org.openconcerto.ui.TitledSeparator;
import org.openconcerto.utils.ListMap;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class ContratPrevoyanceSQLElement extends ComptaSQLConfElement {

    public ContratPrevoyanceSQLElement() {
        super("CONTRAT_PREVOYANCE", "un contrat de prévoyance, mutuelle, formation", "contrats de prévoyance, mutuelle, formation");
    }

    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        // l.add("CODE_UNIQUE");
        l.add("NOM");
        l.add("REFERENCE");
        l.add("CODE_ORGANISME");
        l.add("CODE_DELEGATAIRE");
        l.add("COTISATION_ETABLISSEMENT");
        return l;
    }

    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NOM");
        l.add("REFERENCE");
        return l;
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

            ContratPrevSalarieTable tableSalarie = new ContratPrevSalarieTable();

            ContratPrevRubriqueTable tableRub = new ContratPrevRubriqueTable(false);
            ContratPrevRubriqueTable tableRubNet = new ContratPrevRubriqueTable(true);

            public void addViews() {

                this.setLayout(new GridBagLayout());
                final GridBagConstraints c = new DefaultGridBagConstraints();

                /***********************************************************************************
                 * Renseignements
                 **********************************************************************************/
                JPanel panelInfos = new JPanel();
                panelInfos.setBorder(BorderFactory.createTitledBorder("Renseignements (S21.G00.15)"));
                panelInfos.setLayout(new GridBagLayout());

                // // Code
                // JLabel labelCode = new JLabel(getLabelFor("CODE_UNIQUE"));
                // JTextField textCode = new JTextField();
                // panelInfos.add(labelCode, c);
                // c.gridx++;
                // c.weightx = 1;
                // panelInfos.add(textCode, c);
                // this.addSQLObject(textCode, "CODE_UNIQUE");
                // c.weightx = 0;

                // Nom
                JLabel labelNom = new JLabel(getLabelFor("NOM"));
                JTextField textNom = new JTextField();
                // c.gridx++;
                c.weightx = 0;
                panelInfos.add(labelNom, c);
                c.gridx++;
                c.weightx = 1;
                panelInfos.add(textNom, c);
                c.weightx = 0;
                // Ref
                JLabel labelRef = new JLabel(getLabelFor("REFERENCE"));
                JTextField textRef = new JTextField();
                c.gridx++;
                panelInfos.add(labelRef, c);
                c.gridx++;
                c.weightx = 1;
                panelInfos.add(textRef, c);

                c.weightx = 0;

                // Nom
                JLabel labelCO = new JLabel(getLabelFor("CODE_ORGANISME"));
                JTextField textCO = new JTextField();
                c.gridy++;
                c.gridx = 0;
                c.weightx = 0;
                panelInfos.add(labelCO, c);
                c.gridx++;
                c.weightx = 1;
                panelInfos.add(textCO, c);
                c.weightx = 0;

                // Ref
                JLabel labelCD = new JLabel(getLabelFor("CODE_DELEGATAIRE"));
                JTextField textCD = new JTextField();
                c.gridx++;
                panelInfos.add(labelCD, c);
                c.gridx++;
                c.weightx = 1;
                panelInfos.add(textCD, c);

                // Formation
                JCheckBox checkFO = new JCheckBox(getLabelFor("COTISATION_ETABLISSEMENT"));
                c.gridy++;
                c.gridx = 0;
                c.weightx = 0;
                c.gridwidth = 2;
                panelInfos.add(checkFO, c);

                // Deb
                JLabel labelDateDeb = new JLabel(getLabelFor("DATE_DEBUT"));
                JDate dateDeb = new JDate();
                c.gridx = 0;
                c.gridy++;
                c.weightx = 0;
                panelInfos.add(labelDateDeb, c);
                c.gridx++;
                c.weightx = 1;
                panelInfos.add(dateDeb, c);
                this.addSQLObject(dateDeb, "DATE_DEBUT");
                c.weightx = 0;
                // Fin
                JLabel labelDateFin = new JLabel(getLabelFor("DATE_FIN"));
                JDate dateFin = new JDate();
                c.gridx++;
                panelInfos.add(labelDateFin, c);
                c.gridx++;
                c.weightx = 1;
                panelInfos.add(dateFin, c);
                this.addSQLObject(dateFin, "DATE_FIN");

                c.weightx = 0;

                TitledSeparator sepRenseignement = new TitledSeparator("Renseignement par contrat salarié (S21.G00.70)");
                c.gridwidth = GridBagConstraints.REMAINDER;
                c.gridy++;
                c.gridx = 0;
                panelInfos.add(sepRenseignement, c);
                c.gridx = 0;
                c.gridy++;
                c.fill = GridBagConstraints.BOTH;
                c.gridwidth = GridBagConstraints.REMAINDER;
                c.weighty = 0.5;

                panelInfos.add(tableSalarie, c);

                TitledSeparator sepRub = new TitledSeparator("Rubriques de cotisation rattachées");
                c.gridwidth = GridBagConstraints.REMAINDER;
                c.gridy++;
                c.gridx = 0;
                panelInfos.add(sepRub, c);

                c.gridx = 0;
                c.gridy++;
                c.fill = GridBagConstraints.BOTH;
                c.weighty = 0.5;
                panelInfos.add(tableRub, c);

                TitledSeparator sepRubN = new TitledSeparator("Rubriques de net rattachées");
                c.gridwidth = GridBagConstraints.REMAINDER;
                c.gridy++;
                c.gridx = 0;
                panelInfos.add(sepRubN, c);

                c.gridx = 0;
                c.gridy++;
                c.fill = GridBagConstraints.BOTH;
                c.weighty = 0.5;
                panelInfos.add(tableRubNet, c);

                c.gridx = 0;
                c.gridy++;
                c.fill = GridBagConstraints.BOTH;
                c.weightx = 1;
                c.weighty = 1;
                this.add(panelInfos, c);

                this.addSQLObject(textNom, "NOM");

                this.addSQLObject(textCD, "CODE_DELEGATAIRE");
                this.addSQLObject(textCO, "CODE_ORGANISME");
                this.addSQLObject(textRef, "REFERENCE");
                this.addSQLObject(checkFO, "COTISATION_ETABLISSEMENT");
            }

            @Override
            public int insert(SQLRow order) {

                int id = super.insert(order);

                tableRub.updateField("ID_CONTRAT_PREVOYANCE", id);
                tableRubNet.updateField("ID_CONTRAT_PREVOYANCE", id);
                tableSalarie.updateField("ID_CONTRAT_PREVOYANCE", id);
                return id;
            }

            @Override
            public void update() {

                int id = getSelectedID();
                super.update();
                tableRub.updateField("ID_CONTRAT_PREVOYANCE", id);
                tableRubNet.updateField("ID_CONTRAT_PREVOYANCE", id);
                tableSalarie.updateField("ID_CONTRAT_PREVOYANCE", id);

            }

            @Override
            public void select(SQLRowAccessor r) {

                super.select(r);
                if (r != null) {
                    tableRub.insertFrom("ID_CONTRAT_PREVOYANCE", r.getID());
                    tableRubNet.insertFrom("ID_CONTRAT_PREVOYANCE", r.getID());
                    tableSalarie.insertFrom("ID_CONTRAT_PREVOYANCE", r.getID());
                }
            }
        };
    }
}
