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
import org.openconcerto.utils.StringUtils;
import org.openconcerto.utils.StringUtils.Side;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.commons.dbutils.handlers.ArrayListHandler;

public class ExportEBP_ComptaPro extends AbstractExport {
    static private final Charset CHARSET = StringUtils.Cp1252;
    static private final char SEP = 0x0E;
    static private final int[] WIDTHS = new int[] { 6, 8, 40, 8, 15, 60, 15, 60, 14 };
    static private final String SPACES = "                                                       ";
    // Format : largeur fixe, separateur : 0x0E (Shift Out)
    // l: 6 : espaces...
    // l: 8 : code du journal, aligné à gauche
    // l: 40 : nom du journal, aligné à gauche
    // l: 8 : date de l'écriture, AAAAMMJJ
    // l: 15 : numéro du compte, aligné à gauche
    // l: 60 : nom du compte, aligné à gauche
    // l: 15 : numéro de l'écriture, aligné à gauche
    // l: 60 : nom de l'écriture, aligné à gauche
    // l: 14 : montant, aligné à droite, premier caractère : le signe
    // needs . for decimal separator
    static private final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("##0.00", DecimalFormatSymbols.getInstance(Locale.UK));

    static private String formatCents(final Number n) {
        return DECIMAL_FORMAT.format(BigDecimal.valueOf(n.longValue()).movePointLeft(2));
    }

    private List<Object[]> data;

    public ExportEBP_ComptaPro(DBRoot rootSociete) {
        super(rootSociete, "EBPPro", ".txt");
    }

    @Override
    protected int fetchData(Date from, Date to, SQLRow selectedJournal, boolean onlyNew) {
        final SQLTable tableEcriture = getEcritureT();
        final SQLTable tableMouvement = tableEcriture.getForeignTable("ID_MOUVEMENT");
        final SQLTable tableCompte = tableEcriture.getForeignTable("ID_COMPTE_PCE");
        final SQLTable tableJrnl = tableEcriture.getForeignTable("ID_JOURNAL");

        final SQLSelect sel = createSelect(from, to, selectedJournal, onlyNew);
        sel.addSelect(tableJrnl.getField("CODE"));
        sel.addSelect(tableJrnl.getField("NOM"));
        sel.addSelect(tableEcriture.getField("DATE"));
        sel.addSelect(tableCompte.getField("NUMERO"));
        sel.addSelect(tableCompte.getField("NOM"));
        sel.addSelect(tableMouvement.getField("NUMERO"));
        sel.addSelect(tableEcriture.getField("NOM"));
        sel.addSelect(tableEcriture.getField("DEBIT"));
        sel.addSelect(tableEcriture.getField("CREDIT"));
        sel.addFieldOrder(tableJrnl.getField("CODE"));
        sel.addFieldOrder(tableEcriture.getField("DATE"));
        sel.addFieldOrder(tableMouvement.getField("NUMERO"));

        @SuppressWarnings("unchecked")
        final List<Object[]> l = (List<Object[]>) this.getRootSociete().getDBSystemRoot().getDataSource().execute(sel.asString(), new ArrayListHandler());
        this.data = l;
        return l == null ? 0 : l.size();
    }

    private final String align(final Object o, final int widthIndex) {
        return align(o, widthIndex, false);
    }

    private final String align(final Object o, final int widthIndex, final boolean allowTruncate) {
        String s = String.valueOf(o).trim();
        final int width = WIDTHS[widthIndex];
        if (s.length() > width) {
            s = s.substring(0, width);
        }
        return StringUtils.getFixedWidthString(s, width, Side.LEFT, false);
    }

    @Override
    protected void export(OutputStream out) throws IOException {
        final Writer bufOut = new OutputStreamWriter(out, CHARSET);
        final DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        final String firstField = SPACES.substring(0, WIDTHS[0]);
        for (final Object[] array : this.data) {
            int fieldIndex = 0;
            // ESPACES...
            bufOut.write(align(firstField, fieldIndex++));
            bufOut.write(SEP);
            // JOURNAL.CODE
            bufOut.write(align(array[0], fieldIndex++));
            bufOut.write(SEP);
            // JOURNAL.NOM
            bufOut.write(align(array[1], fieldIndex++, true));
            bufOut.write(SEP);
            // ECRITURE.DATE
            bufOut.write(align(dateFormat.format(array[2]), fieldIndex++));
            bufOut.write(SEP);
            // COMPTE_PCE.NUMERO
            bufOut.write(align(array[3], fieldIndex++));
            bufOut.write(SEP);
            // COMPTE_PCE.NOM
            bufOut.write(align(array[4], fieldIndex++, true));
            bufOut.write(SEP);
            // MOUVEMENT.NUMERO
            bufOut.write(align(array[5], fieldIndex++));
            bufOut.write(SEP);
            // ECRITURE.NOM
            bufOut.write(align(array[6], fieldIndex++, true));
            bufOut.write(SEP);

            // Montant
            final long debit = ((Number) array[7]).longValue();
            final long credit = ((Number) array[8]).longValue();
            if (debit > 0 && credit > 0)
                throw new IllegalStateException("Both credit and debit");
            final long cents;
            final char sign;
            if (credit > 0) {
                cents = credit;
                sign = '-';
            } else {
                cents = debit;
                sign = ' ';
            }
            bufOut.write(sign);
            // -1 since we wrote the sign
            final int wAmount = WIDTHS[fieldIndex++] - 1;
            String amount = formatCents(cents);
            if (amount.length() > wAmount) {
                amount = amount.substring(0, wAmount);
            }
            bufOut.write(StringUtils.getFixedWidthString(amount, wAmount, Side.RIGHT, false));
            bufOut.write("\r\n");
        }
    }
}
