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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;

public class PaiementRemainedProvider implements SpreadSheetCellValueProvider {

    private enum Type {
        DONE, REMAINED
    };

    private final Type t;

    private PaiementRemainedProvider(Type t) {
        this.t = t;
    }

    public Object getValue(SpreadSheetCellValueContext context) {
        SQLRowAccessor row = context.getRow();
        return getRestant(row);
    }

    public static void register() {
        SpreadSheetCellValueProviderManager.put("invoice.payment.remained", new PaiementRemainedProvider(Type.REMAINED));
        SpreadSheetCellValueProviderManager.put("invoice.payment.done", new PaiementRemainedProvider(Type.DONE));
        SpreadSheetCellValueProviderManager.put("invoice.paiement.remained", new PaiementRemainedProvider(Type.REMAINED));
        SpreadSheetCellValueProviderManager.put("invoice.paiement.done", new PaiementRemainedProvider(Type.DONE));
    }

    private BigDecimal getRestant(SQLRowAccessor r) {
        Collection<? extends SQLRowAccessor> rows = r.getReferentRows(r.getTable().getTable("ECHEANCE_CLIENT"));
        long totalEch = 0;

        for (SQLRowAccessor row : rows) {
            if (!row.getBoolean("REGLE") && !row.getBoolean("REG_COMPTA")) {
                totalEch += row.getLong("MONTANT");
            }
        }
        if (r.getTable().contains("ACOMPTE_COMMANDE") && r.getBigDecimal("ACOMPTE_COMMANDE") != null) {
            long totalAcompte = r.getBigDecimal("ACOMPTE_COMMANDE").movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValue();
            if (totalEch > totalAcompte) {
                totalEch -= totalAcompte;
            }
        }

        long total = totalEch;
        if (t == Type.DONE) {
            total = r.getLong("T_TTC") - total;
        }
        return new BigDecimal(total).movePointLeft(2);
    }

}
