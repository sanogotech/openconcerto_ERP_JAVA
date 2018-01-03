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
 
 package org.openconcerto.erp.core.finance.accounting.report;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.generationDoc.AbstractListeSheetXml;
import org.openconcerto.erp.preferences.PrinterNXProps;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.FieldRef;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.TableRef;
import org.openconcerto.sql.model.Where;
import org.openconcerto.utils.GestionDevise;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RepartitionAnalytiqueSheetXML extends AbstractListeSheetXml {

    protected final static SQLTable tableAssoc = base.getTable("ASSOCIATION_ANALYTIQUE");
    protected final static SQLTable tablePoste = base.getTable("POSTE_ANALYTIQUE");
    private final static SQLTable tableEcriture = base.getTable("ECRITURE");

    private final DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM);
    private final DateFormat dateFormatEcr = DateFormat.getDateInstance(DateFormat.SHORT);
    private SQLRow rowSociete = ((ComptaPropsConfiguration) Configuration.getInstance()).getRowSociete();

    private Date dateDu, dateAu;

    public static String TEMPLATE_ID = "RepartitionAnalytique";
    public static String TEMPLATE_PROPERTY_NAME = "LocationJournaux";

    @Override
    public String getDefaultTemplateId() {
        return TEMPLATE_ID;
    }

    Date date;

    @Override
    public String getName() {
        if (this.date == null) {
            this.date = new Date();
        }
        return "RépartitionAnalytique" + date.getTime();
    }

    @Override
    protected String getStoragePathP() {
        return "Répartition Analytique";
    }

    public RepartitionAnalytiqueSheetXML(Date du, Date au) {
        super();
        Calendar cal = Calendar.getInstance();
        cal.setTime(au);
        this.printer = PrinterNXProps.getInstance().getStringProperty("JournauxPrinter");
        this.dateAu = au;
        this.dateDu = du;
    }

    private int size;

    private void makeSousTotal(Map<String, Object> line, Map<Integer, String> style, int pos, long debit, long credit) {
        style.put(pos, "Titre 1");

        line.put("DEBIT", Double.valueOf(GestionDevise.currencyToString(debit, false)));
        line.put("CREDIT", Double.valueOf(GestionDevise.currencyToString(credit, false)));
        line.put("SOLDE", Double.valueOf(GestionDevise.currencyToString(debit - credit, false)));
    }

    protected void createListeValues() {

        SQLSelect sel = new SQLSelect();

        sel.addSelect(tablePoste.getField("NOM"));
        sel.addSelect(tableAssoc.getField("MONTANT"), "SUM");

        Where w = (new Where(tableEcriture.getField("DATE"), RepartitionAnalytiqueSheetXML.this.dateDu, RepartitionAnalytiqueSheetXML.this.dateAu));

        // if (rowPoste != null && !rowPoste.isUndefined()) {

        w = w.and(new Where(tableAssoc.getField("ID_POSTE_ANALYTIQUE"), "=", tablePoste.getKey()));
        w = w.and(new Where(tableAssoc.getField("ID_ECRITURE"), "=", tableEcriture.getKey()));

        sel.setWhere(w);
        sel.addRawSelect("subString(\"" + sel.getAlias(tableEcriture).getAlias() + "\".\"COMPTE_NUMERO\",1,1)", "classe");

        sel.addRawOrder("classe");
        sel.addFieldOrder(tablePoste.getField("NOM"));
        // sel.addFieldOrder(sel.getJoinFromField(tableAssoc.getField("ID_ECRITURE")).getJoinedTable().getField("COMPTE_NUMERO"));
        // sel.addGroupBy(sel.getJoinFromField(tableAssoc.getField("ID_ECRITURE")).getJoinedTable().getField("COMPTE_NUMERO"));
        // sel.addGroupBy(sel.getJoinFromField(tableAssoc.getField("ID_ECRITURE")).getJoinedTable().getField("COMPTE_NOM"));
        // sel.addGroupBy(sel.getJoinFromField(tableAssoc.getField("ID_POSTE_ANALYTIQUE")).getJoinedTable().getField("CODE"));
        sel.addGroupBy(tablePoste.getField("NOM"));
        sel.addGroupBy(new FieldRef() {

            @Override
            public TableRef getTableRef() {

                return tableEcriture;
            }

            @Override
            public String getFieldRef() {

                return "classe";
            }

            @Override
            public SQLField getField() {

                return tableEcriture.getField("COMPTE_NUMERO");
            }

            @Override
            public String getAlias() {

                return tableEcriture.getField("COMPTE_NUMERO").getName();
            }
        });

        List<Object[]> list = tableAssoc.getDBSystemRoot().getDataSource().executeA(sel.asString());
        size = list.size();

        long totalDebit, totalCredit, sousTotalDebit, sousTotalCredit, totalCreditAntC, totalDebitAntC, totalCreditAntF, totalDebitAntF;

        totalDebit = 0;
        totalCredit = 0;
        sousTotalCredit = 0;
        sousTotalDebit = 0;
        totalCreditAntC = 0;
        totalDebitAntC = 0;
        totalCreditAntF = 0;
        totalDebitAntF = 0;

        Object[] rowFirstEcr = null;

        boolean setTitle = true;
        boolean setLine = false;

        String numClasseFirst = "";

        final String titre3 = "Titre 3";
        // int j = 0;

        // Valeur de la liste
        // listAllSheetValues ;

        // Style des lignes
        // styleAllSheetValues;

        // Valeur à l'extérieur de la liste
        // mapAllSheetValues

        List<Map<String, Object>> tableauVals = new ArrayList<Map<String, Object>>();
        this.listAllSheetValues.put(0, tableauVals);

        Map<Integer, String> style = new HashMap<Integer, String>();
        this.styleAllSheetValues.put(0, style);

        // Affiche le nom du compte
        setTitle = true;
        // ligne vide avant de mettre le setTitle
        setLine = false;
        for (int i = 0; i < size;) {
            // System.err.println(i);
            // // System.err.println("START NEW PAGE; POS : " + posLine);
            //
            // /***************************************************************************************
            // * ENTETE
            // **************************************************************************************/
            // // makeEntete(posLine);
            // // posLine += debutFill - 1;

            /***************************************************************************************
             * CONTENU
             **************************************************************************************/
            final Double doubleZero = Double.valueOf("0");

            final Object[] line = list.get(i);

            // String codePoste = line[0].toString();
            String nomPoste = line[0].toString();
            String classe = line[2].toString();

            Map<String, Object> ooLine = new HashMap<String, Object>();

            // Titre
            if (setTitle) {
                if (!setLine) {
                    style.put(tableauVals.size() - 1, "Titre 1");

                    ooLine.put("CLASSE", "");
                    ooLine.put("POSTE_CODE", "");
                    ooLine.put("POSTE_NOM", "");
                    ooLine.put("DEBIT", "");
                    ooLine.put("CREDIT", "");
                    ooLine.put("SOLDE", "");
                    setTitle = false;
                    setLine = true;

                    if (rowFirstEcr == null) {
                        rowFirstEcr = line;
                        numClasseFirst = line[2].toString();
                    }

                } else {
                    style.put(tableauVals.size() - 1, "Normal");
                    setLine = false;
                }
            } else {

                // si on change de classe alors on applique le style Titre 1
                if (rowFirstEcr != null && !classe.equals(numClasseFirst)) {
                    tableauVals.add(ooLine);
                    rowFirstEcr = line;
                    numClasseFirst = line[2].toString();
                    ooLine.put("CLASSE", classe);
                    // ooLine.put("POSTE_CODE", codePoste);
                    ooLine.put("POSTE_NOM", nomPoste);

                    makeSousTotal(ooLine, style, tableauVals.size() - 1, sousTotalDebit, sousTotalCredit);

                    sousTotalCredit = 0;
                    sousTotalDebit = 0;
                    setTitle = true;
                } else {
                    long l = ((BigDecimal) line[1]).movePointRight(2).longValue();
                    long cred = (l >= 0 ? 0 : -l);
                    long deb = (l <= 0 ? 0 : l);

                    ooLine.put("POSTE_NOM", nomPoste);
                    // ooLine.put("POSTE_CODE", codePoste);

                    totalCredit += cred;
                    totalDebit += deb;

                    sousTotalCredit += cred;
                    sousTotalDebit += deb;
                    long solde = sousTotalDebit - sousTotalCredit;

                    ooLine.put("DEBIT", (deb == 0) ? doubleZero : Double.valueOf(GestionDevise.currencyToString(deb, false)));
                    ooLine.put("CREDIT", (cred == 0) ? doubleZero : Double.valueOf(GestionDevise.currencyToString(cred, false)));
                    ooLine.put("SOLDE", (solde == 0) ? doubleZero : Double.valueOf(GestionDevise.currencyToString(solde, false)));

                    style.put(tableauVals.size() - 1, "Normal");
                    i++;
                }

            }

        }

        Map<String, Object> sheetVals = new HashMap<String, Object>();
        this.mapAllSheetValues.put(0, sheetVals);

        if (size > 0) {
            Map<String, Object> ooLine = new HashMap<String, Object>();
            tableauVals.add(ooLine);
            makeSousTotal(ooLine, style, tableauVals.size() - 1, sousTotalDebit, sousTotalCredit);

            sheetVals.put("TOTAL_DEBIT", (totalDebit == 0) ? 0 : new Double(GestionDevise.currencyToString(totalDebit, false)));
            sheetVals.put("TOTAL_CREDIT", (totalCredit == 0) ? 0 : new Double(GestionDevise.currencyToString(totalCredit, false)));
            sheetVals.put("TOTAL_SOLDE", (totalDebit - totalCredit == 0) ? 0 : new Double(GestionDevise.currencyToString(totalDebit - totalCredit, false)));
        }

        sheetVals.put("TITRE_1", "Répartition analytique " + this.rowSociete.getString("TYPE") + " " + this.rowSociete.getString("NOM"));
        sheetVals.put("DATE_EDITION", new Date());
        sheetVals.put("TITRE_2", "Période du " + dateFormatEcr.format(this.dateDu) + " au " + dateFormatEcr.format(this.dateAu) + ".");

    }

    @Override
    public String getTemplateId() {
        return TEMPLATE_ID;
    }

    public int getSize() {
        return size;
    }
}
