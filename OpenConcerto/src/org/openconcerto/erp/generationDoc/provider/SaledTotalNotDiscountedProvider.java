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
import org.openconcerto.erp.generationDoc.SpreadSheetCellValueProviderManager;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLTable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;

public class SaledTotalNotDiscountedProvider extends UserInitialsValueProvider {

    @Override
    public Object getValue(SpreadSheetCellValueContext context) {
        SQLRowAccessor row = context.getRow();

        final SQLTable table = row.getTable();
        Collection<? extends SQLRowAccessor> cols = row.getReferentRows(table.getTable(table.getName() + "_ELEMENT"));
        BigDecimal total = BigDecimal.ZERO;
        for (SQLRowAccessor sqlRowAccessor : cols) {

            final BigDecimal montant = sqlRowAccessor.getBigDecimal("PV_HT");
            final BigDecimal qteUnit = sqlRowAccessor.getBigDecimal("QTE_UNITAIRE");
            final int qte = sqlRowAccessor.getInt("QTE");
            if (montant != null) {
                total = total.add(montant.multiply(qteUnit).multiply(new BigDecimal(qte)).setScale(2, RoundingMode.HALF_UP));
            }
        }
        if (table.contains("PORT_HT")) {
            total = total.add(new BigDecimal(row.getLong("PORT_HT")).movePointLeft(2));
        }
        return total;
    }

    public static void register() {
        SpreadSheetCellValueProviderManager.put("sales.total.notdiscounted", new SaledTotalNotDiscountedProvider());
    }
}
