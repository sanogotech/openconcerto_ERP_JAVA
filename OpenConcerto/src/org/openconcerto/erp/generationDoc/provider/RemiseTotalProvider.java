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

public class RemiseTotalProvider extends UserInitialsValueProvider {

    private enum TypeRemiseTotalProvider {
        TTC, HT
    };

    private final TypeRemiseTotalProvider type;

    public RemiseTotalProvider(TypeRemiseTotalProvider t) {
        this.type = t;
    }

    @Override
    public Object getValue(SpreadSheetCellValueContext context) {
        SQLRowAccessor row = context.getRow();

        final SQLTable table = row.getTable();
        Collection<? extends SQLRowAccessor> cols = row.getReferentRows(table.getTable(table.getName() + "_ELEMENT"));
        BigDecimal total = BigDecimal.ZERO;
        for (SQLRowAccessor sqlRowAccessor : cols) {

            if (!sqlRowAccessor.getTable().contains("NIVEAU") || sqlRowAccessor.getInt("NIVEAU") == 1) {

                BigDecimal lineDiscount = BigDecimal.ZERO;
                lineDiscount = sqlRowAccessor.getBigDecimal("PV_HT").multiply(sqlRowAccessor.getBigDecimal("QTE_UNITAIRE").multiply(new BigDecimal(sqlRowAccessor.getInt("QTE"))));

                if (this.type == TypeRemiseTotalProvider.TTC) {
                    if (sqlRowAccessor.getForeign("ID_TAXE") != null && !sqlRowAccessor.isForeignEmpty("ID_TAXE")) {
                        BigDecimal vat = new BigDecimal(sqlRowAccessor.getForeign("ID_TAXE").getFloat("TAUX")).movePointLeft(2).add(BigDecimal.ONE);
                        lineDiscount = lineDiscount.multiply(vat).setScale(2, RoundingMode.HALF_UP);
                    }
                }

                total = total.add(lineDiscount);

            }
        }

        String prefix = "MONTANT_";
        if (row.getTable().contains("T_HT")) {
            prefix = "T_";
        }

        if (this.type == TypeRemiseTotalProvider.TTC) {
            total = new BigDecimal(row.getLong(prefix + "TTC")).movePointLeft(2).subtract(total);
        } else {
            total = new BigDecimal(row.getLong(prefix + "HT")).movePointLeft(2).subtract(total);
        }
        return total.abs();
    }

    public static void register() {
        SpreadSheetCellValueProviderManager.put("sales.discount.total", new RemiseTotalProvider(TypeRemiseTotalProvider.HT));
        SpreadSheetCellValueProviderManager.put("sales.discount.total.ttc", new RemiseTotalProvider(TypeRemiseTotalProvider.TTC));
    }
}
