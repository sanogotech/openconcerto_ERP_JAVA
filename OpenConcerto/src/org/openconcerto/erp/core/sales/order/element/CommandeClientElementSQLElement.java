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
 
 package org.openconcerto.erp.core.sales.order.element;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.erp.core.common.ui.DeviseField;
import org.openconcerto.erp.core.sales.product.element.ReferenceArticleSQLElement;
import org.openconcerto.erp.core.supplychain.stock.element.MouvementStockSQLElement;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.element.UISQLComponent;
import org.openconcerto.sql.model.SQLInjector;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.sql.view.list.IListe;
import org.openconcerto.sql.view.list.IListeAction.IListeEvent;
import org.openconcerto.sql.view.list.RowAction.PredicateRowAction;
import org.openconcerto.utils.DecimalUtils;
import org.openconcerto.utils.ListMap;

import java.awt.event.ActionEvent;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JTextField;

public class CommandeClientElementSQLElement extends ComptaSQLConfElement {

    public CommandeClientElementSQLElement() {
        super("COMMANDE_CLIENT_ELEMENT", "un element de commande", "éléments de commande");

        PredicateRowAction rowAction = new PredicateRowAction(new AbstractAction("Transfert vers commande fournisseur") {

            @Override
            public void actionPerformed(ActionEvent e) {
                final List<SQLRowValues> selectedRows = IListe.get(e).getSelectedRows();
                final List<SQLRowAccessor> arts = new ArrayList<SQLRowAccessor>();
                final Set<Integer> s = new HashSet<Integer>();
                for (SQLRowValues sqlRowValues : selectedRows) {
                    if (sqlRowValues.getObject("ID_ARTICLE") != null && !sqlRowValues.isForeignEmpty("ID_ARTICLE")) {
                        SQLRowAccessor rowArt = sqlRowValues.getForeign("ID_ARTICLE");
                        if (!s.contains(rowArt.getID())) {
                            s.add(rowArt.getID());
                            arts.add(rowArt);
                        }
                    }
                }
                ComptaPropsConfiguration.getInstanceCompta().getNonInteractiveSQLExecutor().execute(new Runnable() {

                    @Override
                    public void run() {

                        createCommandeF(arts);
                    }
                });
            }

        }, true);
        rowAction.setPredicate(IListeEvent.getNonEmptySelectionPredicate());
        getRowActions().add(rowAction);
    }

    @Override
    protected String getParentFFName() {
        return "ID_COMMANDE_CLIENT";
    }

    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("CODE");
        l.add("NOM");
        l.add("ID_COMMANDE_CLIENT");
        l.add("ID_ARTICLE");
        l.add("PA_HT");
        l.add("PV_HT");
        l.add("QTE");
        l.add("QTE_UNITAIRE");
        l.add("QTE_LIVREE");
        return l;
    }

    /**
     * Transfert d'une commande en commande fournisseur
     * 
     * @param commandeID
     */
    public void transfertCommande(List<SQLRowValues> commandeClientEltsRows) {

        SQLTable tableCmdElt = getDirectory().getElement("COMMANDE_ELEMENT").getTable();
        SQLElement eltArticle = getDirectory().getElement("ARTICLE");

        final ListMap<SQLRow, SQLRowValues> map = new ListMap<SQLRow, SQLRowValues>();
        List<String> fields2copy = Arrays.asList("CODE", "NOM", "VALEUR_METRIQUE_1", "VALEUR_METRIQUE_2", "VALEUR_METRIQUE_3");

        Set<Integer> art = new HashSet<Integer>();
        for (SQLRowValues sqlRow : commandeClientEltsRows) {
            // on récupére l'article qui lui correspond
            SQLRowValues rowArticle = new SQLRowValues(eltArticle.getTable());
            for (String field : fields2copy) {
                // if (sqlRow.getTable().getFieldsName().contains(field.getName())) {
                rowArticle.put(field, sqlRow.asRow().getObject(field));
                // }
            }

            int idArticle = ReferenceArticleSQLElement.getIdForCNM(rowArticle, true);
            SQLRow rowArticleFind = eltArticle.getTable().getRow(idArticle);
            if (rowArticleFind != null && !rowArticleFind.isUndefined()) {
                SQLInjector inj = SQLInjector.getInjector(rowArticle.getTable(), tableCmdElt);
                SQLRowValues rowValsElt = new SQLRowValues(inj.createRowValuesFrom(rowArticleFind));
                rowValsElt.put("ID_STYLE", sqlRow.getObject("ID_STYLE"));

                // if()

                rowValsElt.put("QTE", sqlRow.getObject("QTE"));
                rowValsElt.put("T_POIDS", rowValsElt.getLong("POIDS") * rowValsElt.getInt("QTE"));
                rowValsElt.put("T_PA_HT", ((BigDecimal) rowValsElt.getObject("PA_HT")).multiply(new BigDecimal(rowValsElt.getInt("QTE")), DecimalUtils.HIGH_PRECISION));
                rowValsElt.put("T_PA_TTC",
                        ((BigDecimal) rowValsElt.getObject("T_PA_HT")).multiply(new BigDecimal((rowValsElt.getForeign("ID_TAXE").getFloat("TAUX") / 100.0 + 1.0)), DecimalUtils.HIGH_PRECISION));
                // rowValsElt.put("ID_DEVISE",
                // rowCmd.getForeignRow("ID_TARIF").getForeignID("ID_DEVISE"));
                map.add(rowArticleFind.getForeignRow("ID_FOURNISSEUR"), rowValsElt);
            }

        }
        // TODO
        MouvementStockSQLElement.createCommandeF(map, null, "", false);
    }

    public void createCommandeF(final List<? extends SQLRowAccessor> rowsArt) {

        ComptaPropsConfiguration.getInstanceCompta().getNonInteractiveSQLExecutor().execute(new Runnable() {

            @Override
            public void run() {
                final ListMap<SQLRow, SQLRowValues> map = new ListMap<SQLRow, SQLRowValues>();

                for (SQLRowAccessor rowArticleFind : rowsArt) {

                    SQLRow row = rowArticleFind.asRow();
                    final int value = -Math.round(row.getForeign("ID_STOCK").getFloat("QTE_TH") - row.getFloat("QTE_MIN"));
                    if (value > 0) {

                        SQLInjector inj = SQLInjector.getInjector(row.getTable(), row.getTable().getTable("COMMANDE_ELEMENT"));
                        SQLRowValues rowValsElt = new SQLRowValues(inj.createRowValuesFrom(row));

                        rowValsElt.put("QTE", value);
                        rowValsElt.put("T_POIDS", rowValsElt.getLong("POIDS") * rowValsElt.getInt("QTE"));
                        rowValsElt.put("T_PA_HT", ((BigDecimal) rowValsElt.getObject("PA_HT")).multiply(new BigDecimal(rowValsElt.getInt("QTE")), DecimalUtils.HIGH_PRECISION));
                        rowValsElt.put("T_PA_TTC", ((BigDecimal) rowValsElt.getObject("T_PA_HT")).multiply(new BigDecimal((rowValsElt.getForeign("ID_TAXE").getFloat("TAUX") / 100.0 + 1.0)),
                                DecimalUtils.HIGH_PRECISION));
                        // rowValsElt.put("ID_DEVISE",
                        // rowCmd.getForeignRow("ID_TARIF").getForeignID("ID_DEVISE"));
                        map.add(rowArticleFind.getForeign("ID_FOURNISSEUR").asRow(), rowValsElt);
                    }
                }
                MouvementStockSQLElement.createCommandeF(map, null, "", false);
            }
        });

    }

    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        if (getTable().contains("ID_ARTICLE")) {
            l.add("ID_ARTICLE");
        }
        l.add("NOM");
        l.add("PV_HT");
        return l;
    }

    @Override
    public ListMap<String, String> getShowAs() {
        final ListMap<String, String> res = new ListMap<String, String>();
        res.putCollection("ID_COMMANDE_CLIENT", "NUMERO", "DATE", "DATE_LIVRAISON_PREV");
        if (getTable().contains("ID_ARTICLE")) {
            res.putCollection("ID_ARTICLE", "ID_FAMILLE_ARTICLE", "ID_FOURNISSEUR");
        }
        res.putCollection(null, "NOM");
        return res;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openconcerto.devis.SQLElement#getComponent()
     */
    public SQLComponent createComponent() {
        return new UISQLComponent(this) {
            public void addViews() {
                this.addRequiredSQLObject(new JTextField(), "NOM", "left");
                this.addRequiredSQLObject(new JTextField(), "CODE", "right");

                this.addSQLObject(new ElementComboBox(), "ID_STYLE", "left");

                this.addRequiredSQLObject(new DeviseField(), "PA_HT", "left");
                this.addSQLObject(new DeviseField(), "PV_HT", "right");

                this.addSQLObject(new JTextField(), "POIDS", "left");
                this.addSQLObject(new ElementComboBox(), "ID_TAXE", "right");
            }
        };
    }

    @Override
    protected String createCode() {
        return createCodeFromPackage() + ".item";
    }
}
