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
 
 /*
 * Créé le 19 nov. 2012
 */
package org.openconcerto.erp.generationDoc.gestcomm;

import org.openconcerto.erp.generationDoc.AbstractListeSheetXml;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLRowValuesListFetcher;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.utils.cc.ITransformer;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReportingEcoContributionSheetXML extends AbstractListeSheetXml {

    public static final String TEMPLATE_ID = "ReportingEcoContribution";
    public static final String TEMPLATE_PROPERTY_NAME = DEFAULT_PROPERTY_NAME;

    private Date dateD, dateF;

    public ReportingEcoContributionSheetXML(Date debut, Date fin) {
        super();

        this.dateD = debut;
        this.dateF = fin;

    }

    @Override
    public String getStoragePathP() {
        return "Autres";
    }

    @Override
    public String getDefaultTemplateId() {
        return TEMPLATE_ID;
    };

    @Override
    public String getName() {
        return "ReportingBA";
    }

    @Override
    protected void createListeValues() {

        this.mapAllSheetValues = new HashMap<Integer, Map<String, Object>>();
        this.listAllSheetValues = new HashMap<Integer, List<Map<String, Object>>>();
        this.styleAllSheetValues = new HashMap<Integer, Map<Integer, String>>();

        fillSynthese();

    }

    private static SQLTable tableVF = Configuration.getInstance().getDirectory().getElement("SAISIE_VENTE_FACTURE").getTable();

    class EcoContribRecap {
        private final String code, nom;
        private BigDecimal totalEco, qte;

        public EcoContribRecap(String code, String nom) {
            this.code = code;
            this.nom = nom;
            this.qte = BigDecimal.ZERO;
            this.totalEco = BigDecimal.ZERO;
        }

        public void cumul(BigDecimal qte, BigDecimal total) {
            this.qte = qte.add(this.qte);
            this.totalEco = total.add(this.totalEco);
        }

        public String getCode() {
            return code;
        }

        public String getNom() {
            return nom;
        }

        public BigDecimal getQte() {
            return qte;
        }

        public BigDecimal getTotalEco() {
            return totalEco;
        }
    }

    private void fillSynthese() {
        final SQLTable tableVF = Configuration.getInstance().getRoot().findTable("SAISIE_VENTE_FACTURE");
        final SQLTable tableVFElt = Configuration.getInstance().getRoot().findTable("SAISIE_VENTE_FACTURE_ELEMENT");

        SQLRowValues rowvalsVF = new SQLRowValues(tableVF);
        rowvalsVF.put("NUMERO", null);
        rowvalsVF.put("DATE", null);

        SQLRowValues rowvalsVFElt = new SQLRowValues(tableVFElt);
        rowvalsVFElt.put("ID_SAISIE_VENTE_FACTURE", rowvalsVF);
        rowvalsVFElt.put("T_ECO_CONTRIBUTION", null);
        rowvalsVFElt.put("QTE", null);
        rowvalsVFElt.put("QTE_UNITAIRE", null);
        rowvalsVFElt.putRowValues("ID_ECO_CONTRIBUTION").putNulls("CODE", "NOM");

        SQLRowValuesListFetcher fetcher = SQLRowValuesListFetcher.create(rowvalsVFElt);
        fetcher.setSelTransf(new ITransformer<SQLSelect, SQLSelect>() {

            @Override
            public SQLSelect transformChecked(SQLSelect input) {
                Where w = new Where(input.getAlias(tableVF).getField("DATE"), dateD, dateF);
                w = w.and(new Where(tableVFElt.getField("T_ECO_CONTRIBUTION"), ">", BigDecimal.ZERO));
                input.setWhere(w);
                return input;
            }
        });

        Map<Integer, EcoContribRecap> recap = new HashMap<Integer, ReportingEcoContributionSheetXML.EcoContribRecap>();
        List<SQLRowValues> results = fetcher.fetch();
        for (SQLRowValues sqlRowValues : results) {
            EcoContribRecap r;
            if (recap.containsKey(sqlRowValues.getForeignID("ID_ECO_CONTRIBUTION"))) {
                r = recap.get(sqlRowValues.getForeignID("ID_ECO_CONTRIBUTION"));
            } else {
                SQLRowAccessor rEco = sqlRowValues.getForeign("ID_ECO_CONTRIBUTION");
                r = new EcoContribRecap(rEco.getString("CODE"), rEco.getString("NOM"));
                recap.put(sqlRowValues.getForeignID("ID_ECO_CONTRIBUTION"), r);
            }
            r.cumul(sqlRowValues.getBigDecimal("QTE_UNITAIRE").multiply(new BigDecimal(sqlRowValues.getInt("QTE"))), sqlRowValues.getBigDecimal("T_ECO_CONTRIBUTION"));
        }

        List<Map<String, Object>> values = new ArrayList<Map<String, Object>>();
        this.listAllSheetValues.put(0, values);
        Map<Integer, String> style = new HashMap<Integer, String>();
        this.styleAllSheetValues.put(0, style);

        BigDecimal total = BigDecimal.ZERO;
        BigDecimal qteTotal = BigDecimal.ZERO;

        for (EcoContribRecap item : recap.values()) {
            Map<String, Object> vals = new HashMap<String, Object>();

            vals.put("CODE", item.getCode());
            vals.put("NOM", item.getNom());
            vals.put("QTE", item.getQte());
            vals.put("TOTAL_ECO", item.getTotalEco());
            style.put(values.size(), "Normal");
            total = total.add(item.getTotalEco());
            qteTotal = qteTotal.add(item.getQte());
            values.add(vals);
        }

        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy");
        Map<String, Object> vals = new HashMap<String, Object>();
        vals.put("DATE_DEB", this.dateD);
        vals.put("DATE_FIN", this.dateF);
        vals.put("PERIODE", "Période du " + format.format(this.dateD) + " au " + format.format(this.dateF));
        vals.put("TOTAL", total);
        vals.put("TOTAL_QTE", qteTotal);
        this.mapAllSheetValues.put(0, vals);
        // style.put(values.size(), "Titre 1");
    }
}
