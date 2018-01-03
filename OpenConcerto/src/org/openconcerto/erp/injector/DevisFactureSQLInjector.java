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

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.SQLInjector;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;

import java.math.BigDecimal;
import java.util.Collection;

import com.ibm.icu.util.Calendar;

public class DevisFactureSQLInjector extends SQLInjector {
    public DevisFactureSQLInjector(final DBRoot root) {
        super(root, "DEVIS", "SAISIE_VENTE_FACTURE", true);
        final SQLTable tableDevis = getSource();
        final SQLTable tableFacture = getDestination();

        map(tableDevis.getField("PORT_HT"), tableFacture.getField("PORT_HT"));
        map(tableDevis.getField("REMISE_HT"), tableFacture.getField("REMISE_HT"));
        map(tableDevis.getField("ID_CLIENT"), tableFacture.getField("ID_CLIENT"));
        map(tableDevis.getField("ID_COMMERCIAL"), tableFacture.getField("ID_COMMERCIAL"));
        map(tableDevis.getField("ID_DEVIS"), tableFacture.getField("ID_DEVIS"));
        if (tableDevis.getTable().contains("ID_POLE_PRODUIT")) {
            map(tableDevis.getField("ID_POLE_PRODUIT"), tableFacture.getField("ID_POLE_PRODUIT"));
        }
        if (tableDevis.getTable().contains("ID_VERIFICATEUR") && tableFacture.getTable().contains("ID_VERIFICATEUR")) {
            map(tableDevis.getField("ID_VERIFICATEUR"), tableFacture.getField("ID_VERIFICATEUR"));
        }
        if (tableDevis.getTable().contains("ID_CONTACT")) {
            map(tableDevis.getField("ID_CONTACT"), tableFacture.getField("ID_CONTACT"));
        }
        if (getSource().getTable().contains("ID_CLIENT_DEPARTEMENT")) {
            map(tableDevis.getField("ID_CLIENT_DEPARTEMENT"), tableFacture.getField("ID_CLIENT_DEPARTEMENT"));
        }
        if (getSource().getTable().contains("ID_ADRESSE") && getDestination().contains("ID_ADRESSE")) {
            map(tableDevis.getField("ID_ADRESSE"), tableFacture.getField("ID_ADRESSE"));
        }
        if (getSource().getTable().contains("ID_ADRESSE_LIVRAISON") && getDestination().contains("ID_ADRESSE_LIVRAISON")) {
            map(tableDevis.getField("ID_ADRESSE_LIVRAISON"), tableFacture.getField("ID_ADRESSE_LIVRAISON"));
        }

        if (getSource().getTable().contains("MONTANT_REMISE") && tableFacture.contains("MONTANT_REMISE")) {
            map(tableDevis.getField("MONTANT_REMISE"), tableFacture.getField("MONTANT_REMISE"));
            map(tableDevis.getField("POURCENT_REMISE"), tableFacture.getField("POURCENT_REMISE"));
        }
    }

    @Override
    protected void merge(SQLRowAccessor srcRow, SQLRowValues rowVals) {
        super.merge(srcRow, rowVals);


        // Merge elements
        final SQLTable tableElementSource = getSource().getTable("DEVIS_ELEMENT");
        final SQLTable tableElementDestination = getSource().getTable("SAISIE_VENTE_FACTURE_ELEMENT");
        final Collection<? extends SQLRowAccessor> myListItem = srcRow.asRow().getReferentRows(tableElementSource);
        transfertReference(srcRow, rowVals, "OBJET", "NOM");
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
