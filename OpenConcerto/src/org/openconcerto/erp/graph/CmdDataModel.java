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

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElementDirectory;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;

import java.util.Date;

public class CmdDataModel extends MonthDataModel {

    public CmdDataModel(int year, boolean cumul) {
        super(year, cumul);
    }

    @Override
    public long computeValue(Date d1, Date d2) {
        double vCA = 0;
        final SQLElementDirectory directory = Configuration.getInstance().getDirectory();
        final SQLTable tableEcr = directory.getElement("COMMANDE_CLIENT").getTable();
        final SQLSelect sel = new SQLSelect();
        sel.addSelect(tableEcr.getField("T_HT"), "SUM");
        final Where w = new Where(tableEcr.getField("DATE"), d1, d2);
        sel.setWhere(w);

        final Object[] o = tableEcr.getBase().getDataSource().executeA1(sel.asString());
        if (o != null && o[0] != null && (Long.valueOf(o[0].toString()) != 0)) {
            long deb = Long.valueOf(o[0].toString());
            vCA = deb;
        }

        final long value = Math.round(vCA / 100.0D);
        return value;
    }

}
