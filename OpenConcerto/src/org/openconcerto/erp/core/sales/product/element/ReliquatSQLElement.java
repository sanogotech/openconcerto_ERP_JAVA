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

import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.erp.core.sales.invoice.ui.FactureAffacturerTable;
import org.openconcerto.erp.core.supplychain.receipt.component.BonReceptionSQLComponent;
import org.openconcerto.sql.element.BaseSQLComponent;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.view.EditFrame;
import org.openconcerto.sql.view.EditPanel.EditMode;
import org.openconcerto.sql.view.EditPanelListener;
import org.openconcerto.sql.view.list.IListe;
import org.openconcerto.sql.view.list.IListeAction.IListeEvent;
import org.openconcerto.sql.view.list.RowAction.PredicateRowAction;
import org.openconcerto.ui.DefaultGridBagConstraints;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JLabel;

public class ReliquatSQLElement extends ComptaSQLConfElement {

    private final String tableBonName;

    static public class ReliquatBRSQLElement extends ReliquatSQLElement {
        public ReliquatBRSQLElement() {
            super("RELIQUAT_BR", "un reliquat de BR", "reliquats de BR", "BON_RECEPTION");
        }

        @Override
        protected String createCode() {
            return createCodeFromPackage() + ".reliquatbr";
        }
    }

    public ReliquatSQLElement(final String tableName, final String singularName, final String pluralName, final String tableBonName) {
        super(tableName, singularName, pluralName);
        this.tableBonName = tableBonName;

        PredicateRowAction actionTR = new PredicateRowAction(new AbstractAction("Transfert vers Bon") {

            @Override
            public void actionPerformed(ActionEvent e) {
                final List<SQLRowValues> rows = IListe.get(e).getSelectedRows();
                EditFrame frame = new EditFrame(getForeignElement("ID_" + tableBonName), EditMode.CREATION);
                frame.addEditPanelListener(new EditPanelListener() {

                    @Override
                    public void modified() {
                    }

                    @Override
                    public void inserted(int id) {
                        for (SQLRowValues rowVals : rows) {
                            try {
                                SQLRowValues upRowVals = rowVals.createEmptyUpdateRow();
                                upRowVals.put("ID_" + tableBonName, id);
                                upRowVals.update();
                            } catch (SQLException exn) {
                                exn.printStackTrace();
                            }
                        }

                    }

                    @Override
                    public void deleted() {
                    }

                    @Override
                    public void cancelled() {
                    }
                });

                SQLComponent comp = (SQLComponent) frame.getSQLComponent();
                if (comp instanceof BonReceptionSQLComponent) {
                    ((BonReceptionSQLComponent) comp).loadFromReliquat(rows);
                } else {
                    // ((BonDeLivraisonSQLComponent) comp).loadFromReliquat(rows);

                }
                frame.setVisible(true);

            }
        }, true);
        actionTR.setPredicate(IListeEvent.getNonEmptySelectionPredicate());
        getRowActions().add(actionTR);
    }

    public ReliquatSQLElement() {
        this("RELIQUAT_BL", "un reliquat de BL", "reliquats de BL", "BON_DE_LIVRAISON");
    }

    @Override
    protected String getParentFFName() {
        return "ID_" + this.tableBonName;
    }

    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        // l.add("DATE");
        l.add("ID_" + this.tableBonName + "_ORIGINE");
        l.add("ID_ARTICLE");
        l.add("QTE_UNITAIRE");
        l.add("ID_UNITE_VENTE");
        l.add("QTE");
        l.add("ID_" + this.tableBonName);
        return l;
    }

    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("DATE");
        l.add("QTE");
        return l;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openconcerto.devis.SQLElement#getComponent()
     */
    public SQLComponent createComponent() {
        return new BaseSQLComponent(this) {
            FactureAffacturerTable table = new FactureAffacturerTable(null);

            public void addViews() {

                // FIXME CREATION DE L'UI
                this.setLayout(new GridBagLayout());

                GridBagConstraints c = new DefaultGridBagConstraints();

                this.add(new JLabel(getLabelFor("NB_FACT")), c);
                // c.gridx++;
                // c.weightx = 1;
                // JTextField fieldNbFact = new JTextField();
                // this.add(fieldNbFact, c);
                //
                // c.gridx++;
                // c.weightx = 0;
                // this.add(new JLabel(getLabelFor("MONTANT_FACT")), c);
                // c.gridx++;
                // c.weightx = 1;
                // DeviseField fieldMontantFact = new DeviseField();
                // this.add(fieldMontantFact, c);
                //
                // c.gridx++;
                // c.weightx = 0;
                // this.add(new JLabel(getLabelFor("DATE")), c);
                // c.gridx++;
                //
                // JDate date = new JDate();
                // this.add(date, c);
                //
                // c.gridwidth = GridBagConstraints.REMAINDER;
                // c.gridx = 0;
                // c.gridy++;
                // this.add(this.table, c);
                //
                // this.addView(fieldNbFact, "NB_FACT");
                // this.addSQLObject(fieldMontantFact, "MONTANT_FACT");
                // this.addView(date, "DATE");
            }

        };
    }

    @Override
    protected String createCode() {
        return createCodeFromPackage() + ".reliquatbl";
    }
}
