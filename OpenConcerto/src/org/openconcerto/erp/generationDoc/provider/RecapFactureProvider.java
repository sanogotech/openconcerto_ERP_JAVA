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
 
 package org.openconcerto.erp.generationDoc.provider;

import org.openconcerto.erp.generationDoc.SpreadSheetCellValueContext;
import org.openconcerto.erp.generationDoc.SpreadSheetCellValueProvider;
import org.openconcerto.erp.generationDoc.SpreadSheetCellValueProviderManager;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.utils.GestionDevise;

import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class RecapFactureProvider implements SpreadSheetCellValueProvider {

    private enum TypeRecapFactureProvider {
        HT, TTC;
    };

    private final TypeRecapFactureProvider type;

    public RecapFactureProvider(TypeRecapFactureProvider t) {
        this.type = t;
    }

    public Object getValue(SpreadSheetCellValueContext context) {
        SQLRowAccessor row = context.getRow();
        Calendar c = row.getDate("DATE");

        Collection<? extends SQLRowAccessor> rows = row.getReferentRows(row.getTable().getTable("TR_COMMANDE_CLIENT"));
        StringBuffer result = new StringBuffer();
        Set<SQLRowAccessor> facture = new HashSet<SQLRowAccessor>();
        facture.add(row);
        for (SQLRowAccessor sqlRowAccessor : rows) {
            result.append(getPreviousAcompte(sqlRowAccessor.getForeign("ID_COMMANDE_CLIENT"), facture, c));
        }
        if (result.length() > 0) {
            return "Facturation précédente :" + result.toString();
        } else {
            return null;
        }
    }

    public static void register() {
        SpreadSheetCellValueProviderManager.put("sales.account.history", new RecapFactureProvider(TypeRecapFactureProvider.HT));
        SpreadSheetCellValueProviderManager.put("sales.account.history.ttc", new RecapFactureProvider(TypeRecapFactureProvider.TTC));
    }

    public String getPreviousAcompte(SQLRowAccessor sqlRowAccessor, Set<SQLRowAccessor> alreadyAdded, Calendar c) {
        if (sqlRowAccessor == null || sqlRowAccessor.isUndefined()) {
            return "";
        }
        Collection<? extends SQLRowAccessor> rows = sqlRowAccessor.getReferentRows(sqlRowAccessor.getTable().getTable("TR_COMMANDE_CLIENT"));
        StringBuffer result = new StringBuffer();
        for (SQLRowAccessor sqlRowAccessor2 : rows) {
            SQLRowAccessor rowFact = sqlRowAccessor2.getForeign("ID_SAISIE_VENTE_FACTURE");

            if (rowFact != null && !rowFact.isUndefined() && !alreadyAdded.contains(rowFact) && rowFact.getDate("DATE").before(c)) {
                alreadyAdded.add(rowFact);
                final String fieldTotal = this.type == TypeRecapFactureProvider.HT ? "T_HT" : "T_TTC";
                result.append(rowFact.getString("NUMERO") + " (" + GestionDevise.currencyToString(rowFact.getLong(fieldTotal)) + "€), ");
            }
        }

        return result.toString();
    }

}
