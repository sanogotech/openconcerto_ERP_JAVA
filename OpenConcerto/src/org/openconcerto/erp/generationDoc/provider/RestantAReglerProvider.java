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
import org.openconcerto.erp.generationDoc.provider.TotalAcompteProvider.TypeTotalAcompteProvider;
import org.openconcerto.erp.generationDoc.provider.TotalCommandeClientProvider.TypeTotalCommandeClientProvider;

import java.math.BigDecimal;

public class RestantAReglerProvider implements SpreadSheetCellValueProvider {

    final TotalAcompteProvider acompteProv;
    final TotalCommandeClientProvider cmdProvider;

    private enum TypeRestantAReglerProvider {
        HT, TTC;
    };

    private final TypeRestantAReglerProvider type;

    public RestantAReglerProvider(TypeRestantAReglerProvider t) {
        this.type = t;
        if (this.type == TypeRestantAReglerProvider.HT) {
            acompteProv = new TotalAcompteProvider(TypeTotalAcompteProvider.HT);
            cmdProvider = new TotalCommandeClientProvider(TypeTotalCommandeClientProvider.HT);
        } else {
            acompteProv = new TotalAcompteProvider(TypeTotalAcompteProvider.TTC);
            cmdProvider = new TotalCommandeClientProvider(TypeTotalCommandeClientProvider.TTC);
        }

    }

    public Object getValue(SpreadSheetCellValueContext context) {
        Object acompte = acompteProv.getValue(context);
        Object cmd = cmdProvider.getValue(context);
        if (acompte != null && cmd != null) {
            return ((BigDecimal) cmd).subtract((BigDecimal) acompte);
        } else {
            return null;
        }
    }

    public static void register() {
        SpreadSheetCellValueProviderManager.put("sales.account.due", new RestantAReglerProvider(TypeRestantAReglerProvider.HT));
        SpreadSheetCellValueProviderManager.put("sales.account.due.ttc", new RestantAReglerProvider(TypeRestantAReglerProvider.TTC));
    }

}
