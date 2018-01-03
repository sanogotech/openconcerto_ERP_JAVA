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
 
 package org.openconcerto.erp.generationDoc.compta;

import org.openconcerto.erp.generationDoc.AbstractListeSheetXml;
import org.openconcerto.erp.preferences.PrinterNXProps;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLRowValuesListFetcher;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.utils.cc.ITransformer;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReportingAnalytiqueKDXmlSheet extends AbstractListeSheetXml {

    public static final String TEMPLATE_ID = "KDAnalytique";
    public static final String TEMPLATE_PROPERTY_NAME = DEFAULT_PROPERTY_NAME;
    private Calendar c = Calendar.getInstance();
    private Date date = new Date();
    private final long MILLIS_IN_HOUR = 3600000;

    public ReportingAnalytiqueKDXmlSheet(int mois, int year) {
        super();
        this.printer = PrinterNXProps.getInstance().getStringProperty("BonPrinter");
        this.mapAllSheetValues = new HashMap<Integer, Map<String, Object>>();
        this.c.set(Calendar.DAY_OF_MONTH, 1);
        this.c.set(Calendar.YEAR, year);
        this.c.set(Calendar.MONTH, mois);
        this.c.set(Calendar.HOUR_OF_DAY, 0);
        this.c.set(Calendar.MINUTE, 0);
        this.c.set(Calendar.SECOND, 0);
        this.c.set(Calendar.MILLISECOND, 1);

    }

    @Override
    protected String getStoragePathP() {
        return "Analytique";
    }

    @Override
    public String getName() {
        if (this.date == null) {
            this.date = new Date();
        }
        return "KDAnalytique" + this.date;
    }

    @Override
    public String getDefaultTemplateId() {
        return TEMPLATE_ID;
    }

    protected void createListeValues() {
        this.listAllSheetValues = new HashMap<Integer, List<Map<String, Object>>>();
        this.styleAllSheetValues = new HashMap<Integer, Map<Integer, String>>();

        SQLTable tableAssoc = Configuration.getInstance().getDirectory().getElement("ASSOCIATION_ANALYTIQUE").getTable();
        SQLRowValues rowVals = new SQLRowValues(tableAssoc);
        rowVals.putNulls("MONTANT", "POURCENT");
        rowVals.putRowValues("ID_ECRITURE").putNulls("DEBIT", "CREDIT", "DATE", "COMPTE_NUMERO");
        rowVals.putRowValues("ID_POSTE_ANALYTIQUE").putNulls("NOM").putRowValues("ID_AXE_ANALYTIQUE").putNulls("NOM");

        SQLRowValuesListFetcher fetcher = SQLRowValuesListFetcher.create(rowVals);

        fetcher.setSelTransf(new ITransformer<SQLSelect, SQLSelect>() {

            @Override
            public SQLSelect transformChecked(SQLSelect input) {
                Calendar c = Calendar.getInstance();
                c.set(Calendar.DAY_OF_MONTH, 1);
                c.set(Calendar.MONTH, Calendar.JANUARY);
                c.set(Calendar.HOUR, 0);
                c.set(Calendar.MINUTE, 0);

                input.setWhere(new Where(input.getTable("ECRITURE").getField("DATE"), ">=", c.getTime()));

                return input;
            }
        });

        List<SQLRowValues> result = fetcher.fetch();

        List<Map<String, Object>> listValues = new ArrayList<Map<String, Object>>();
        // Map<Integer, String> styleValues = new HashMap<Integer, String>();
        for (SQLRowValues sqlRowValues : result) {

            Map<String, Object> mapSheetValue = new HashMap<String, Object>();

            final SQLRowAccessor foreignEcr = sqlRowValues.getForeign("ID_ECRITURE");
            mapSheetValue.put("DATE", foreignEcr.getDate("DATE").getTime());
            mapSheetValue.put("DEBIT", foreignEcr.getLong("DEBIT"));
            mapSheetValue.put("CREDIT", foreignEcr.getLong("CREDIT"));
            mapSheetValue.put("COMPTE", foreignEcr.getString("COMPTE_NUMERO"));

            mapSheetValue.put("POURCENT", sqlRowValues.getObject("POURCENT"));
            mapSheetValue.put("MONTANT", sqlRowValues.getObject("MONTANT"));

            final SQLRowAccessor foreignPoste = sqlRowValues.getForeign("ID_POSTE_ANALYTIQUE");
            mapSheetValue.put("POSTE", foreignPoste.getString("NOM"));

            mapSheetValue.put("AXE", foreignPoste.getForeign("ID_AXE_ANALYTIQUE").getString("NOM"));

            listValues.add(mapSheetValue);

        }

        // this.sheetNames.add(userName);

        this.listAllSheetValues.put(0, listValues);

    }

}
