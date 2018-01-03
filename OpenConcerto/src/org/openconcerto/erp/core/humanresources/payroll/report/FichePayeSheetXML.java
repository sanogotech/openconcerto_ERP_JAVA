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
 
 package org.openconcerto.erp.core.humanresources.payroll.report;

import org.openconcerto.erp.generationDoc.AbstractSheetXMLWithDate;
import org.openconcerto.erp.preferences.PrinterNXProps;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLRow;

import java.util.Calendar;

public class FichePayeSheetXML extends AbstractSheetXMLWithDate {

    public static final String TEMPLATE_ID = "FichePaye";
    public static final String TEMPLATE_PROPERTY_NAME = "LocationFichePaye";

    public FichePayeSheetXML(SQLRow row) {
        super(row);
        this.printer = PrinterNXProps.getInstance().getStringProperty("FichePayePrinter");
        this.elt = Configuration.getInstance().getDirectory().getElement("FICHE_PAYE");

    }

    @Override
    public String getDefaultTemplateId() {
        return TEMPLATE_ID;
    }

    @Override
    public String getName() {
        SQLRow rowSal = row.getForeign("ID_SALARIE");
        SQLRow rowMois = row.getForeign("ID_MOIS");
        Calendar du = row.getDate("DU");
        String suffix = "";
        if (du != null && du.get(Calendar.DAY_OF_MONTH) != 1) {
            suffix = "_" + du.get(Calendar.DAY_OF_MONTH);
        }
        return ("FichePaye_" + rowSal.getString("CODE") + suffix + "_" + rowMois.getString("NOM") + "_" + row.getString("ANNEE"));
    }

}
