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
 
 package org.openconcerto.erp.core.sales.credit.element;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.erp.core.finance.accounting.element.EcritureSQLElement;
import org.openconcerto.erp.core.sales.credit.component.AvoirClientSQLComponent;
import org.openconcerto.erp.generationDoc.gestcomm.AvoirClientXmlSheet;
import org.openconcerto.erp.generationEcritures.GenerationMvtSaisieVenteFacture;
import org.openconcerto.erp.model.MouseSheetXmlListeListener;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.element.SQLElementLink.LinkType;
import org.openconcerto.sql.element.SQLElementLinksSetup;
import org.openconcerto.sql.element.TreesOfSQLRows;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.request.ListSQLRequest;
import org.openconcerto.sql.view.list.SQLTableModelSource;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.ListMap;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.dbutils.handlers.ArrayListHandler;

public class AvoirClientSQLElement extends ComptaSQLConfElement {

    public AvoirClientSQLElement() {
        super("AVOIR_CLIENT", "une facture d'avoir", "factures d'avoir");
        getRowActions().addAll(new MouseSheetXmlListeListener(AvoirClientXmlSheet.class).getRowActions());
    }

    @Override
    protected void setupLinks(SQLElementLinksSetup links) {
        super.setupLinks(links);
        if (getTable().contains("ID_ADRESSE")) {
            links.get("ID_ADRESSE").setType(LinkType.ASSOCIATION);
        }
        if (getTable().contains("ID_ADRESSE_LIVRAISON")) {
            links.get("ID_ADRESSE_LIVRAISON").setType(LinkType.ASSOCIATION);
        }
    }

    public List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NUMERO");
            l.add("ID_COMMERCIAL");
        l.add("ID_CLIENT");

        l.add("NOM");


        l.add("DATE");
        l.add("MONTANT_HT");
        l.add("MONTANT_TTC");
        l.add("MONTANT_SOLDE");
        l.add("MONTANT_RESTANT");
        l.add("SOLDE");
        return l;
    }

    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("DATE");
        l.add("NOM");
        l.add("NUMERO");
        l.add("MONTANT_TTC");
        l.add("MONTANT_SOLDE");
        return l;
    }

    @Override
    protected synchronized void _initTableSource(final SQLTableModelSource table) {
        super._initTableSource(table);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openconcerto.devis.SQLElement#getComponent()
     */
    public SQLComponent createComponent() {
        return new AvoirClientSQLComponent();
    }

    @Override
    protected void _initListRequest(ListSQLRequest req) {
        super._initListRequest(req);
        req.addToGraphToFetch("A_DEDUIRE", "MOTIF");
    }

    @Override
    public ListMap<String, String> getShowAs() {
        ListMap<String, String> map = new ListMap<String, String>();
        map.putCollection(null, "NUMERO", "DATE", "ID_COMMERCIAL");
        return map;
    }

    public void annulationAvoir(SQLRowAccessor row) {
        Collection<? extends SQLRowAccessor> rows = row.getReferentRows(getTable().getTable("SAISIE_VENTE_FACTURE"));
        for (SQLRowAccessor sqlRowAccessor : rows) {
            SQLRowAccessor rowAvoir = sqlRowAccessor.getForeign("ID_AVOIR_CLIENT");

            Long montantSolde = (Long) rowAvoir.getObject("MONTANT_SOLDE");
            Long avoirTTC = (Long) sqlRowAccessor.getObject("T_AVOIR_TTC");

            long montant = montantSolde - avoirTTC;
            if (montant < 0) {
                montant = 0;
            }

            SQLRowValues rowVals = rowAvoir.createEmptyUpdateRow();

            // Soldé
            rowVals.put("SOLDE", Boolean.FALSE);
            rowVals.put("MONTANT_SOLDE", montant);
            Long restant = (Long) rowAvoir.getObject("MONTANT_TTC") - montantSolde;
            rowVals.put("MONTANT_RESTANT", restant);

            try {
                rowVals.update();

                final SQLRowValues createEmptyUpdateRow = sqlRowAccessor.createEmptyUpdateRow();
                createEmptyUpdateRow.putEmptyLink("ID_AVOIR_CLIENT");
                createEmptyUpdateRow.put("NET_A_PAYER", sqlRowAccessor.getObject("T_TTC"));
                createEmptyUpdateRow.put("T_AVOIR_TTC", 0L);
                createEmptyUpdateRow.update();
                EcritureSQLElement eltEcr = (EcritureSQLElement) Configuration.getInstance().getDirectory().getElement("ECRITURE");
                final int foreignIDmvt = sqlRowAccessor.getForeignID("ID_MOUVEMENT");
                eltEcr.archiveMouvementProfondeur(foreignIDmvt, false);

                System.err.println("Regeneration des ecritures");
                new GenerationMvtSaisieVenteFacture(sqlRowAccessor.getID(), foreignIDmvt);
                System.err.println("Fin regeneration");

            } catch (SQLException e) {
                ExceptionHandler.handle("Erreur lors de l'annulation de l'avoir", e);
            }
        }
    }

    @Override
    protected void archive(TreesOfSQLRows trees, boolean cutLinks) throws SQLException {
        // FIXME Vérifier si l'avoir est affecté sur une facture et recalculer les échéances pour la
        // facture associée
        for (SQLRow row : trees.getRows()) {

            annulationAvoir(row);

            // Mise à jour des stocks
            SQLElement eltMvtStock = Configuration.getInstance().getDirectory().getElement("MOUVEMENT_STOCK");
            SQLSelect sel = new SQLSelect(eltMvtStock.getTable().getBase());
            sel.addSelect(eltMvtStock.getTable().getField("ID"));
            Where w = new Where(eltMvtStock.getTable().getField("IDSOURCE"), "=", row.getID());
            Where w2 = new Where(eltMvtStock.getTable().getField("SOURCE"), "=", getTable().getName());
            sel.setWhere(w.and(w2));

            List l = (List) eltMvtStock.getTable().getBase().getDataSource().execute(sel.asString(), new ArrayListHandler());
            if (l != null) {
                for (int i = 0; i < l.size(); i++) {
                    Object[] tmp = (Object[]) l.get(i);
                    eltMvtStock.archive(((Number) tmp[0]).intValue());
                }
            }
        }
        // TODO Auto-generated method stub
        super.archive(trees, cutLinks);

    }

}
