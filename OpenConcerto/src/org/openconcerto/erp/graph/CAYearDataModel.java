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
 
 package org.openconcerto.erp.graph;

import org.openconcerto.erp.core.finance.accounting.model.SommeCompte;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElementDirectory;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;

import java.util.Date;

public class CAYearDataModel extends YearDataModel {

    public CAYearDataModel(final int year) {
        super(year);
    }

    @Override
    public double computeValue(Date d1, Date d2) {
        final SQLElementDirectory directory = Configuration.getInstance().getDirectory();
        final SQLTable tableEcr = directory.getElement("ECRITURE").getTable();
        final SQLTable tableCpt = directory.getElement("COMPTE_PCE").getTable();
        final SQLSelect sel = new SQLSelect();
        sel.addSelect(tableEcr.getField("DEBIT"), "SUM");
        sel.addSelect(tableEcr.getField("CREDIT"), "SUM");
        final Where w = new Where(tableEcr.getField("DATE"), d1, d2);
        final Where w2 = new Where(tableEcr.getField("ID_COMPTE_PCE"), "=", tableCpt.getKey());
        final Where w3 = new Where(tableCpt.getField("NUMERO"), "LIKE", "70%");
        final Where w4 = new Where(tableEcr.getField("NOM"), "LIKE", "Fermeture%");
        sel.setWhere(w.and(w2).and(w3).and(w4));
        final SommeCompte sommeCompte = new SommeCompte();
        float vCA = 0;
        final Object[] o = tableEcr.getBase().getDataSource().executeA1(sel.asString());
        if (o != null && o[0] != null && o[1] != null && (Long.valueOf(o[0].toString()) != 0 || Long.valueOf(o[1].toString()) != 0)) {
            long deb = Long.valueOf(o[0].toString());
            long cred = Long.valueOf(o[1].toString());
            long tot = deb - cred;
            vCA = sommeCompte.soldeCompteDebiteur(700, 708, true, d1, d2) - sommeCompte.soldeCompteDebiteur(709, 709, true, d1, d2);
            vCA = tot - vCA;
        } else {
            vCA = sommeCompte.soldeCompteCrediteur(700, 708, true, d1, d2) - sommeCompte.soldeCompteCrediteur(709, 709, true, d1, d2);
        }
        final float value = vCA / 100;
        return value;
    }
}
