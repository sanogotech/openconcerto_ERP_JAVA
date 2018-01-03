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
import org.openconcerto.sql.element.BaseSQLComponent;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.JDate;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

public class ContratSalarieSQLElement extends ComptaSQLConfElement {

    public ContratSalarieSQLElement() {
        super("CONTRAT_SALARIE", "un contrat salarié", "contrats salariés");
    }

    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NATURE");
        return l;
    }

    @Override
    public Set<String> getInsertOnlyFields() {
        Set<String> s = new HashSet<String>();
        s.add("DATE_MODIFICATION");
        return s;
    }

    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NUMERO");
        l.add("DATE_DEBUT");
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

                // Numero
                JLabel labelNumero = new JLabel(getLabelFor("NUMERO"));
                labelNumero.setHorizontalAlignment(SwingConstants.RIGHT);
                JTextField textNumero = new JTextField();

                this.add(labelNumero, c);
                c.gridx++;
                c.weightx = 1;
                this.add(textNumero, c);
                this.addRequiredSQLObject(textNumero, "NUMERO");

                c.gridy++;
                c.gridx = 0;
                // Nature
                JLabel labelNature = new JLabel(getLabelFor("NATURE"));
                labelNature.setHorizontalAlignment(SwingConstants.RIGHT);
                JTextField textNature = new JTextField();

                this.add(labelNature, c);
                c.gridx++;
                c.weightx = 1;
                this.add(textNature, c);

                // Catégorie socioprofessionnelle
                JLabel labelCatSocio = new JLabel(getLabelFor("ID_CODE_EMPLOI"));
                labelCatSocio.setHorizontalAlignment(SwingConstants.RIGHT);
                ElementComboBox selCodeCatSocio = new ElementComboBox();
                selCodeCatSocio.setInfoIconVisible(false);
                c.gridy++;
                c.gridx = 0;
                c.weightx = 0;
                this.add(labelCatSocio, c);
                c.gridx++;
                c.weightx = 1;
                this.add(selCodeCatSocio, c);

                // Contrat de travail
                JLabel labelContratTravail = new JLabel(getLabelFor("ID_CODE_CONTRAT_TRAVAIL"));
                labelContratTravail.setHorizontalAlignment(SwingConstants.RIGHT);
                ElementComboBox selContratTravail = new ElementComboBox();
                selContratTravail.setInfoIconVisible(false);
                c.gridy++;
                c.gridx = 0;
                c.weightx = 0;
                this.add(labelContratTravail, c);
                c.gridx++;
                c.weightx = 1;
                this.add(selContratTravail, c);

                // Droit Contrat de travail
                JLabel labelDroitContrat = new JLabel(getLabelFor("ID_CODE_DROIT_CONTRAT"));
                labelDroitContrat.setHorizontalAlignment(SwingConstants.RIGHT);
                ElementComboBox selDroitContrat = new ElementComboBox();
                selDroitContrat.setInfoIconVisible(false);
                c.gridy++;
                c.gridx = 0;
                c.weightx = 0;
                this.add(labelDroitContrat, c);
                c.gridx++;
                c.weightx = 1;
                this.add(selDroitContrat, c);

                // caracteristiques activité
                JLabel labelCaractActivite = new JLabel(getLabelFor("ID_CODE_CARACT_ACTIVITE"));
                labelCaractActivite.setHorizontalAlignment(SwingConstants.RIGHT);
                ElementComboBox selCaractActivite = new ElementComboBox();
                selCaractActivite.setInfoIconVisible(false);
                c.gridy++;
                c.gridx = 0;
                c.weightx = 0;
                this.add(labelCaractActivite, c);
                c.gridx++;
                c.weightx = 1;
                this.add(selCaractActivite, c);

                // Statut profesionnel
                JLabel labelStatutProf = new JLabel(getLabelFor("ID_CODE_STATUT_PROF"));
                labelStatutProf.setHorizontalAlignment(SwingConstants.RIGHT);
                ElementComboBox selStatutProf = new ElementComboBox();
                selStatutProf.setInfoIconVisible(false);
                c.gridy++;
                c.gridx = 0;
                c.weightx = 0;
                this.add(labelStatutProf, c);
                c.gridx++;
                c.weightx = 1;
                this.add(selStatutProf, c);

                // Statut categoriel
                JLabel labelStatutCat = new JLabel(getLabelFor("ID_CODE_STATUT_CATEGORIEL"));
                labelStatutCat.setHorizontalAlignment(SwingConstants.RIGHT);
                ElementComboBox selStatutCat = new ElementComboBox();
                selStatutCat.setInfoIconVisible(false);
                c.gridy++;
                c.gridx = 0;
                c.weightx = 0;
                this.add(labelStatutCat, c);
                c.gridx++;
                c.weighty = 1;
                c.weightx = 1;
                this.add(selStatutCat, c);

                // Statut categoriel
                JLabel labelStatutCatConv = new JLabel(getLabelFor("ID_CODE_STATUT_CAT_CONV"));
                labelStatutCatConv.setHorizontalAlignment(SwingConstants.RIGHT);
                ElementComboBox selStatutCatConv = new ElementComboBox();
                selStatutCatConv.setInfoIconVisible(false);
                c.gridy++;
                c.gridx = 0;
                c.weightx = 0;
                this.add(labelStatutCatConv, c);
                c.gridx++;
                c.weighty = 1;
                c.weightx = 1;
                this.add(selStatutCatConv, c);

                List<String> dsnFF = Arrays.asList("ID_CONTRAT_MODALITE_TEMPS", "ID_CONTRAT_REGIME_MALADIE", "ID_CONTRAT_REGIME_VIEILLESSE", "ID_CONTRAT_DETACHE_EXPATRIE",
                        "ID_CONTRAT_DISPOSITIF_POLITIQUE");

                for (String ffName : dsnFF) {
                    JLabel labelFF = new JLabel(getLabelFor(ffName));
                    labelFF.setHorizontalAlignment(SwingConstants.RIGHT);
                    ElementComboBox selFF = new ElementComboBox();
                    selFF.setInfoIconVisible(false);
                    c.gridy++;
                    c.gridx = 0;
                    c.weightx = 0;
                    this.add(labelFF, c);
                    c.gridx++;
                    c.weighty = 1;
                    c.weightx = 1;
                    this.add(selFF, c);
                    this.addRequiredSQLObject(selFF, ffName);
                }
                JLabel labelFF = new JLabel(getLabelFor("ID_CONTRAT_MOTIF_RECOURS"));
                labelFF.setHorizontalAlignment(SwingConstants.RIGHT);
                ElementComboBox selFF = new ElementComboBox();
                selFF.setInfoIconVisible(false);
                c.gridy++;
                c.gridx = 0;
                c.weightx = 0;
                this.add(labelFF, c);
                c.gridx++;
                c.weighty = 1;
                c.weightx = 1;
                this.add(selFF, c);
                this.addSQLObject(selFF, "ID_CONTRAT_MOTIF_RECOURS");

                // Code UGRR
                JLabel labelCodeUGRR = new JLabel(getLabelFor("CODE_IRC_UGRR"));
                labelCodeUGRR.setHorizontalAlignment(SwingConstants.RIGHT);
                JTextField textCodeUGRR = new JTextField();
                c.gridy++;
                c.gridx = 0;
                c.weightx = 0;
                this.add(labelCodeUGRR, c);
                c.gridx++;
                c.weighty = 1;
                c.weightx = 1;
                this.add(textCodeUGRR, c);
                addView(textCodeUGRR, "CODE_IRC_UGRR");

                JLabel labelNumUGRR = new JLabel(getLabelFor("NUMERO_RATTACHEMENT_UGRR"));
                labelNumUGRR.setHorizontalAlignment(SwingConstants.RIGHT);
                JTextField textNumUGRR = new JTextField();
                c.gridy++;
                c.gridx = 0;
                c.weightx = 0;
                this.add(labelNumUGRR, c);
                c.gridx++;
                c.weighty = 1;
                c.weightx = 1;
                this.add(textNumUGRR, c);
                addView(textNumUGRR, "NUMERO_RATTACHEMENT_UGRR");

                // Code UGRC
                JLabel labelCodeUGRC = new JLabel(getLabelFor("CODE_IRC_UGRC"));
                labelCodeUGRC.setHorizontalAlignment(SwingConstants.RIGHT);
                JTextField textCodeUGRC = new JTextField();
                c.gridy++;
                c.gridx = 0;
                c.weightx = 0;
                this.add(labelCodeUGRC, c);
                c.gridx++;
                c.weighty = 1;
                c.weightx = 1;
                this.add(textCodeUGRC, c);
                addView(textCodeUGRC, "CODE_IRC_UGRC");

                JLabel labelNumUGRC = new JLabel(getLabelFor("NUMERO_RATTACHEMENT_UGRC"));
                labelNumUGRC.setHorizontalAlignment(SwingConstants.RIGHT);
                JTextField textNumUGRC = new JTextField();
                c.gridy++;
                c.gridx = 0;
                c.weightx = 0;
                this.add(labelNumUGRC, c);
                c.gridx++;
                c.weighty = 1;
                c.weightx = 1;
                this.add(textNumUGRC, c);
                addView(textNumUGRC, "NUMERO_RATTACHEMENT_UGRC");

                // Retraite
                JLabel labelCodeRetraite = new JLabel(getLabelFor("CODE_IRC_RETRAITE"));
                labelCodeRetraite.setHorizontalAlignment(SwingConstants.RIGHT);
                JTextField textCodeRetraite = new JTextField();
                c.gridy++;
                c.gridx = 0;
                c.weightx = 0;
                this.add(labelCodeRetraite, c);
                c.gridx++;
                c.weighty = 1;
                c.weightx = 1;
                this.add(textCodeRetraite, c);
                addView(textCodeRetraite, "CODE_IRC_RETRAITE");

                JLabel labelNumRetraite = new JLabel(getLabelFor("NUMERO_RATTACHEMENT_RETRAITE"));
                labelNumRetraite.setHorizontalAlignment(SwingConstants.RIGHT);
                JTextField textNumRetraite = new JTextField();
                c.gridy++;
                c.gridx = 0;
                c.weightx = 0;
                this.add(labelNumRetraite, c);
                c.gridx++;
                c.weighty = 1;
                c.weightx = 1;
                this.add(textNumRetraite, c);
                addView(textNumRetraite, "NUMERO_RATTACHEMENT_RETRAITE");

                // JLabel labelCodeRegimeRetraite = new
                // JLabel(getLabelFor("CODE_REGIME_RETRAITE_DSN"));
                // labelCodeRegimeRetraite.setHorizontalAlignment(SwingConstants.RIGHT);
                // JTextField textCodeRegimeRetraite = new JTextField();
                // c.gridy++;
                // c.gridx = 0;
                // c.weightx = 0;
                // this.add(labelCodeRegimeRetraite, c);
                // c.gridx++;
                // c.weighty = 1;
                // c.weightx = 1;
                // this.add(textCodeRegimeRetraite, c);
                // addRequiredSQLObject(textCodeRegimeRetraite, "CODE_REGIME_RETRAITE_DSN");

                JLabel labelDateModif = new JLabel(getLabelFor("DATE_MODIFICATION"));
                labelDateModif.setHorizontalAlignment(SwingConstants.RIGHT);
                JDate textDateModif = new JDate();
                c.gridy++;
                c.gridx = 0;
                c.weightx = 0;
                this.add(textDateModif, c);
                c.gridx++;
                c.weighty = 1;
                c.weightx = 1;
                this.add(textDateModif, c);
                addSQLObject(textDateModif, "DATE_MODIFICATION");

                // JLabel labelCM = new JLabel(getLabelFor("ID_INFOS_SALARIE_PAYE_MODIFIE"));
                // labelCM.setHorizontalAlignment(SwingConstants.RIGHT);
                // ElementComboBox selCM = new ElementComboBox();
                // final SQLElement elementInfosPaye =
                // getDirectory().getElement("INFOS_SALARIE_PAYE");
                // selCM.init(elementInfosPaye, elementInfosPaye.createComboRequest());
                // selCM.setInfoIconVisible(false);
                // c.gridy++;
                // c.gridx = 0;
                // c.weightx = 0;
                // this.add(labelCM, c);
                // c.gridx++;
                // c.weighty = 1;
                // c.weightx = 1;
                // this.add(selCM, c);
                // this.addSQLObject(selCM, "ID_INFOS_SALARIE_PAYE_MODIFIE");

                JLabel labelDateDebut = new JLabel(getLabelFor("DATE_DEBUT"));
                labelDateDebut.setHorizontalAlignment(SwingConstants.RIGHT);
                JDate textDateDebut = new JDate();
                c.gridy++;
                c.gridx = 0;
                c.weightx = 0;
                this.add(labelDateDebut, c);
                c.gridx++;
                c.weighty = 1;
                c.weightx = 1;
                this.add(textDateDebut, c);
                addSQLObject(textDateDebut, "DATE_DEBUT", REQ);

                JLabel labelDateFin = new JLabel(getLabelFor("DATE_PREV_FIN"));
                labelDateFin.setHorizontalAlignment(SwingConstants.RIGHT);
                JDate textDateFin = new JDate();
                c.gridy++;
                c.gridx = 0;
                c.weightx = 0;
                this.add(labelDateFin, c);
                c.gridx++;
                c.weighty = 1;
                c.weightx = 1;
                this.add(textDateFin, c);
                addSQLObject(textDateFin, "DATE_PREV_FIN");

                this.addRequiredSQLObject(selCodeCatSocio, "ID_CODE_EMPLOI");
                this.addSQLObject(selContratTravail, "ID_CODE_CONTRAT_TRAVAIL");
                this.addSQLObject(selCaractActivite, "ID_CODE_CARACT_ACTIVITE");
                this.addSQLObject(selDroitContrat, "ID_CODE_DROIT_CONTRAT");
                this.addSQLObject(selStatutProf, "ID_CODE_STATUT_PROF");
                this.addSQLObject(selStatutCat, "ID_CODE_STATUT_CATEGORIEL");
                this.addSQLObject(selStatutCatConv, "ID_CODE_STATUT_CAT_CONV");
                this.addRequiredSQLObject(textNature, "NATURE");
            }
        };
    }

    @Override
    protected String createCode() {
        return createCodeFromPackage() + ".contract.employe";
    }
}
