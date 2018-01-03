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

import java.math.BigDecimal;
import java.util.Collection;

import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.SQLInjector;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;

public class DevisCommandeSQLInjector extends SQLInjector {
    public DevisCommandeSQLInjector(final DBRoot root) {
        super(root, "DEVIS", "COMMANDE_CLIENT", true);
        final SQLTable tableDevis = getSource();
        final SQLTable tableCommande = getDestination();
        map(tableDevis.getField("ID_CLIENT"), tableCommande.getField("ID_CLIENT"));
        if (tableDevis.contains("REMISE_HT") && tableCommande.contains("REMISE_HT")) {
            map(tableDevis.getField("REMISE_HT"), tableCommande.getField("REMISE_HT"));
        }
        if (tableDevis.contains("PORT_HT") && tableCommande.contains("PORT_HT")) {
            map(tableDevis.getField("PORT_HT"), tableCommande.getField("PORT_HT"));
        }
        mapDefaultValues(tableCommande.getField("SOURCE"), tableDevis.getName());
        map(tableDevis.getField("ID_DEVIS"), tableCommande.getField("IDSOURCE"));
        map(tableDevis.getField("ID_DEVIS"), tableCommande.getField("ID_DEVIS"));
        map(tableDevis.getField("ID_COMMERCIAL"), tableCommande.getField("ID_COMMERCIAL"));
        if (tableDevis.getTable().contains("ID_POLE_PRODUIT")) {
            map(tableDevis.getField("ID_POLE_PRODUIT"), tableCommande.getField("ID_POLE_PRODUIT"));
        }
        if (tableDevis.getTable().contains("ID_TARIF") && tableCommande.getTable().contains("ID_TARIF")) {
            map(tableDevis.getField("ID_TARIF"), tableCommande.getField("ID_TARIF"));
        }
        if (getSource().getTable().contains("ID_CONTACT")) {
            map(tableDevis.getField("ID_CONTACT"), tableCommande.getField("ID_CONTACT"));
        }
        if (getSource().getTable().contains("ID_CLIENT_DEPARTEMENT")) {
            map(getSource().getField("ID_CLIENT_DEPARTEMENT"), getDestination().getField("ID_CLIENT_DEPARTEMENT"));
        }
        if (getSource().getTable().contains("ID_ADRESSE") && getDestination().contains("ID_ADRESSE")) {
            map(tableDevis.getField("ID_ADRESSE"), getDestination().getField("ID_ADRESSE"));
        }
        if (getSource().getTable().contains("ID_ADRESSE_LIVRAISON")) {
            map(tableDevis.getField("ID_ADRESSE_LIVRAISON"), getDestination().getField("ID_ADRESSE_LIVRAISON"));
        }

        if (getSource().getTable().contains("MONTANT_REMISE") && getDestination().contains("MONTANT_REMISE")) {
            map(tableDevis.getField("MONTANT_REMISE"), getDestination().getField("MONTANT_REMISE"));
            map(tableDevis.getField("POURCENT_REMISE"), getDestination().getField("POURCENT_REMISE"));
        }
    }

    @Override
    protected void merge(SQLRowAccessor srcRow, SQLRowValues rowVals) {
        super.merge(srcRow, rowVals);

        // Merge elements
        final SQLTable tableElementSource = getSource().getTable("DEVIS_ELEMENT");
        final SQLTable tableElementDestination = getSource().getTable("COMMANDE_CLIENT_ELEMENT");
        final Collection<? extends SQLRowAccessor> myListItem = srcRow.asRow().getReferentRows(tableElementSource);
        transfertReference(srcRow, rowVals, "OBJET", "NOM");
        transfertReference(srcRow, rowVals, "INFOS", "INFOS");
        transfertNumberReference(srcRow, rowVals, tableElementDestination, "ID_COMMANDE_CLIENT");

        if (myListItem.size() != 0) {
            final SQLInjector injector = SQLInjector.getInjector(tableElementSource, tableElementDestination);
            for (SQLRowAccessor rowElt : myListItem) {
                final SQLRowValues createRowValuesFrom = injector.createRowValuesFrom(rowElt.asRow());
                if (createRowValuesFrom.getTable().getFieldsName().contains("POURCENT_ACOMPTE")) {
                    if (createRowValuesFrom.getObject("POURCENT_ACOMPTE") == null) {
                        createRowValuesFrom.put("POURCENT_ACOMPTE", new BigDecimal(100.0));
                    }
                }
                createRowValuesFrom.put("ID_COMMANDE_CLIENT", rowVals);
            }
        }
    }
}
