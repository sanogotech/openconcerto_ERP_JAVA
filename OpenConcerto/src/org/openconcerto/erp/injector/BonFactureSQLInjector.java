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
 
 package org.openconcerto.erp.injector;

import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.SQLInjector;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;

import java.math.BigDecimal;
import java.util.Collection;

public class BonFactureSQLInjector extends SQLInjector {

    public BonFactureSQLInjector(final DBRoot root) {
        super(root.getTable("BON_DE_LIVRAISON"), root.getTable("SAISIE_VENTE_FACTURE"), true);
        final SQLTable tableBon = getSource();
        final SQLTable tableFacture = getDestination();
        // map(tableDevis.getField("PORT_HT"), tableCommande.getField("PORT_HT"));
        // map(tableDevis.getField("REMISE_HT"), tableCommande.getField("REMISE_HT"));
        map(tableBon.getField("ID_CLIENT"), tableFacture.getField("ID_CLIENT"));
        if (tableBon.getTable().contains("ID_POLE_PRODUIT")) {
            map(tableBon.getField("ID_POLE_PRODUIT"), tableFacture.getField("ID_POLE_PRODUIT"));
        }
        if (getSource().getTable().contains("ID_CONTACT")) {
            map(getSource().getField("ID_CONTACT"), getDestination().getField("ID_CONTACT"));
        }
        if (getSource().getTable().contains("ID_CLIENT_DEPARTEMENT")) {
            map(getSource().getField("ID_CLIENT_DEPARTEMENT"), getDestination().getField("ID_CLIENT_DEPARTEMENT"));
        }
        if (getSource().getTable().contains("ID_ADRESSE") && getDestination().contains("ID_ADRESSE")) {
            map(getSource().getField("ID_ADRESSE"), getDestination().getField("ID_ADRESSE"));
        }
        if (getSource().getTable().contains("ID_ADRESSE_LIVRAISON")) {
            map(getSource().getField("ID_ADRESSE_LIVRAISON"), getDestination().getField("ID_ADRESSE_LIVRAISON"));
        }
        if (tableBon.contains("ID_TAXE_PORT")) {
            map(tableBon.getField("ID_TAXE_PORT"), tableFacture.getField("ID_TAXE_PORT"));
        }
        if (tableBon.contains("PORT_HT")) {
            map(tableBon.getField("PORT_HT"), tableFacture.getField("PORT_HT"));
        }
        if (tableBon.contains("REMISE_HT")) {
            map(tableBon.getField("REMISE_HT"), tableFacture.getField("REMISE_HT"));
        }
        if (getSource().getTable().contains("ID_TARIF") && getDestination().getTable().contains("ID_TARIF")) {
            map(getSource().getField("ID_TARIF"), getDestination().getField("ID_TARIF"));
        }
        if (getSource().getTable().contains("ACOMPTE_COMMANDE")) {
            map(getSource().getField("ACOMPTE_COMMANDE"), getDestination().getField("ACOMPTE_COMMANDE"));
        }
    }

    @Override
    protected void merge(SQLRowAccessor srcRow, SQLRowValues rowVals) {
        super.merge(srcRow, rowVals);

        // Merge elements
        final SQLTable tableElementSource = getSource().getTable("BON_DE_LIVRAISON_ELEMENT");
        final SQLTable tableElementDestination = getSource().getTable("SAISIE_VENTE_FACTURE_ELEMENT");
        final Collection<? extends SQLRowAccessor> myListItem = srcRow.asRow().getReferentRows(tableElementSource);
        transfertReference(srcRow, rowVals, "NOM", "NOM");
        transfertReference(srcRow, rowVals, "INFOS", "INFOS");
        transfertNumberReference(srcRow, rowVals, tableElementDestination, "ID_SAISIE_VENTE_FACTURE");

        if (myListItem.size() != 0) {
            final SQLInjector injector = SQLInjector.getInjector(tableElementSource, tableElementDestination);
            for (SQLRowAccessor rowElt : myListItem) {
                final SQLRowValues createRowValuesFrom = injector.createRowValuesFrom(rowElt.asRow());
                if (createRowValuesFrom.getTable().getFieldsName().contains("POURCENT_ACOMPTE")) {
                    if (createRowValuesFrom.getObject("POURCENT_ACOMPTE") == null) {
                        createRowValuesFrom.put("POURCENT_ACOMPTE", new BigDecimal(100.0));
                    }
                }
                createRowValuesFrom.put("ID_SAISIE_VENTE_FACTURE", rowVals);
            }
        }
    }

}
