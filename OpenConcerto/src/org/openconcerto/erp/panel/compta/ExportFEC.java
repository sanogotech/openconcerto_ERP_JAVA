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
 
 package org.openconcerto.erp.panel.compta;

import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.utils.StringUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.apache.commons.dbutils.handlers.ArrayListHandler;

public class ExportFEC extends AbstractExport {

    private static final char ZONE_SEPARATOR = '\t';
    private static final char RECORD_SEPARATOR = '\n';
    private static final char REPLACEMENT = ' ';
    static private final List<String> COLS = Arrays.asList("JournalCode", "JournalLib", "EcritureNum", "EcritureDate", "CompteNum", "CompteLib", "CompAuxNum", "CompAuxLib", "PieceRef", "PieceDate",
            "EcritureLib", "Debit", "Credit", "EcritureLet", "DateLet", "ValidDate", "Montantdevise", "Idevise");

    private final DecimalFormat format = new DecimalFormat("##0.00", DecimalFormatSymbols.getInstance(Locale.FRANCE));
    private List<Object[]> data;

    private final boolean cloture;

    public ExportFEC(DBRoot rootSociete, boolean cloture) {
        super(rootSociete, "FEC", ".csv");
        this.cloture = cloture;
    }

    @Override
    protected int fetchData(Date from, Date to, SQLRow selectedJournal, boolean onlyNew) {
        final SQLTable tableEcriture = getEcritureT();
        final SQLTable tableMouvement = tableEcriture.getForeignTable("ID_MOUVEMENT");
        final SQLTable tableCompte = tableEcriture.getForeignTable("ID_COMPTE_PCE");
        final SQLTable tableJrnl = tableEcriture.getForeignTable("ID_JOURNAL");
        final SQLTable tablePiece = tableMouvement.getForeignTable("ID_PIECE");

        final SQLSelect sel = createSelect(from, to, selectedJournal, onlyNew);
        sel.addSelect(tableJrnl.getField("CODE"));
        sel.addSelect(tableJrnl.getField("NOM"));
        sel.addSelect(tableMouvement.getField("NUMERO"));
        sel.addSelect(tableEcriture.getField("DATE"));
        sel.addSelect(tableCompte.getField("NUMERO"));
        sel.addSelect(tableCompte.getField("NOM"));
        sel.addSelect(tablePiece.getField("NOM"));
        // TODO ID_MOUVEMENT_PERE* ; SOURCE.DATE
        sel.addSelect(tableEcriture.getField("NOM"));
        sel.addSelect(tableEcriture.getField("DEBIT"));
        sel.addSelect(tableEcriture.getField("CREDIT"));
        sel.addSelect(tableEcriture.getField("DATE_LETTRAGE"));
        sel.addSelect(tableEcriture.getField("LETTRAGE"));
        sel.addSelect(tableEcriture.getField("DATE_VALIDE"));

        sel.addFieldOrder(tableEcriture.getField("DATE"));
        sel.addFieldOrder(tableMouvement.getField("NUMERO"));
        sel.setWhere(sel.getWhere().and(new Where(tableEcriture.getField("DEBIT"), "!=", tableEcriture.getField("CREDIT"))));

        @SuppressWarnings("unchecked")
        final List<Object[]> l = (List<Object[]>) this.getRootSociete().getDBSystemRoot().getDataSource().execute(sel.asString(), new ArrayListHandler());
        this.data = l;
        return l == null ? 0 : l.size();
    }

    private final void addEmptyField(final List<String> line) {
        line.add(null);
    }

    private final void addAmountField(final List<String> line, final Number cents) {
        final String formattedAmount = format.format(BigDecimal.valueOf(cents.longValue()).movePointLeft(2));
        line.add(formattedAmount);
    }

    private final void addField(final List<String> line, final String s) {
        if (s == null) {
            throw new NullPointerException("Valeur manquante pour remplir la ligne : " + line);
        }
        // TODO remove \r
        line.add(s.trim().replace(ZONE_SEPARATOR, REPLACEMENT).replace(RECORD_SEPARATOR, REPLACEMENT));
    }

    @Override
    protected void export(OutputStream out) throws IOException {
        final Writer bufOut = new OutputStreamWriter(out, StringUtils.ISO8859_15);
        final DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        final int fieldsCount = COLS.size();

        for (final String colName : COLS) {
            bufOut.write(colName);
            bufOut.write(ZONE_SEPARATOR);
        }
        bufOut.write(RECORD_SEPARATOR);

        final List<String> line = new ArrayList<String>(fieldsCount);
        for (final Object[] array : this.data) {
            line.clear();

            // JournalCode
            addField(line, (String) array[0]);
            // JournalLib
            addField(line, (String) array[1]);
            // EcritureNum
            addField(line, String.valueOf(array[2]));
            // EcritureDate
            final String ecritureDate = dateFormat.format(array[3]);
            line.add(ecritureDate);
            // CompteNum
            if (array[4] != null) {
                addField(line, (String) array[4]);
            } else {
                bufOut.close();
                JOptionPane.showMessageDialog(new JFrame(), "Une écriture n'a pas de numéro de compte :\n" + line, "Erreur FEC", JOptionPane.ERROR_MESSAGE);
                return;
            }
            // CompteLib
            if (array[5] != null) {
                addField(line, (String) array[5]);
            } else {
                bufOut.close();
                JOptionPane.showMessageDialog(new JFrame(), "Une écriture n'a pas de libellé de compte pour le compte " + array[4].toString() + " :\n" + line, "Erreur FEC", JOptionPane.ERROR_MESSAGE);
                return;
            }
            // CompAuxNum
            addEmptyField(line);
            // CompAuxLib
            addEmptyField(line);
            // PieceRef
            addField(line, (String) array[6]);
            // PieceDate TODO ID_MOUVEMENT_PERE* ; SOURCE.DATE
            line.add(ecritureDate);
            // EcritureLib
            String s = (String) array[7];
            if (s == null || s.trim().length() == 0) {
                s = "Sans libellé";
            }
            addField(line, s);
            // Debit
            addAmountField(line, (Number) array[8]);
            // Credit
            addAmountField(line, (Number) array[9]);
            // EcritureLet
            addField(line, (String) array[11]);

            // DateLet
            if (array[10] != null) {
                final String ecritureDateLettrage = dateFormat.format(array[10]);
                line.add(ecritureDateLettrage);
            } else {
                line.add("");
            }
            // ValidDate
            if (array[12] != null) {
                final String ecritureDateValid = dateFormat.format(array[12]);
                line.add(ecritureDateValid);
            } else {
                line.add("");
                if (cloture) {
                    bufOut.close();
                    JOptionPane.showMessageDialog(new JFrame(), "Une écriture n'est pas validée (pas de date):\n" + line, "Erreur FEC", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
            // Montantdevise
            addAmountField(line, ((Number) array[8]).longValue() + ((Number) array[9]).longValue());
            // Idevise
            line.add("EUR");

            assert line.size() == fieldsCount;
            for (int i = 0; i < fieldsCount; i++) {
                final String zone = line.get(i);
                // blank field
                if (zone != null)
                    bufOut.write(zone);
                bufOut.write(ZONE_SEPARATOR);
            }
            bufOut.write(RECORD_SEPARATOR);
        }
        bufOut.close();
    }
}
