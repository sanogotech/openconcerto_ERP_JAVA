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

import java.util.Collection;

public class StockLocationProvider implements SpreadSheetCellValueProvider {

    public Object getValue(SpreadSheetCellValueContext context) {
        SQLRowAccessor row = context.getRow();
        if (!row.isForeignEmpty("ID_ARTICLE")) {
            SQLRowAccessor rowArticle = row.getForeign("ID_ARTICLE");
            Collection<? extends SQLRowAccessor> reglesStock = rowArticle.getReferentRows(rowArticle.getTable().getTable("REGLES_STOCK"));
            if (reglesStock.size() > 0) {

                final SQLRowAccessor next = reglesStock.iterator().next();
                if (!next.isForeignEmpty("ID_ENTREPOT")) {
                    final SQLRowAccessor foreign = next.getForeign("ID_ENTREPOT");
                    return foreign.getString("DESIGNATION");
                }
            }
        }
        return null;
    }

    public static void register() {
        SpreadSheetCellValueProviderManager.put("supplychain.element.stock.location", new StockLocationProvider());
    }

}
