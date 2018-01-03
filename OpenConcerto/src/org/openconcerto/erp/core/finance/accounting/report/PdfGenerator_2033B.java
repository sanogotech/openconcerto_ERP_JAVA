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
import org.openconcerto.erp.preferences.TemplateNXProps;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLRow;

public class PdfGenerator_2033B extends PdfGenerator {
    public PdfGenerator_2033B() {
        super("2033B.pdf", "result_2033B.pdf", TemplateNXProps.getInstance().getStringProperty("Location2033BPDF"));
        setTemplateOffset(0, 0);
        setOffset(0, 0);
        setMargin(32, 32);

    }

    public void generate() {
        setFontRoman(8);

        SQLRow rowSociete = ((ComptaPropsConfiguration) Configuration.getInstance()).getRowSociete();
        addText("NOM", rowSociete.getString("TYPE") + " " + rowSociete.getString("NOM"), 228, 808);
        setFontRoman(12);

        addSplittedText("CLOS1", "08202006", 410, 790, 9.7);
        // addSplittedText("CLOS2", "08202006", 502, 809, 9.7);

        // Copyright
        setFontRoman(9);
        String cc = "Document généré par le logiciel Bloc, (c) Front Software 2006";
        addText("", cc, getWidth() - 2, 460, 90);

        setFontRoman(10);
        long t = 143785123456L;
        int yy = 0;
        int y = 766;
        int cellHeight = 18;
        final int colN = 487;
        for (; y > 720; y -= cellHeight) {

            addTextRight("PRODUIT1." + yy, insertCurrencySpaces("" + t), 400, y);
            addTextRight("PRODUIT2." + yy, insertCurrencySpaces("" + t), colN, y);
            addTextRight("PRODUIT3." + yy, insertCurrencySpaces("" + t), 580, y);
            yy++;
        }
        y += 2;
        for (; y > 630; y -= cellHeight) {

            addTextRight("PRODUIT2." + yy, insertCurrencySpaces("" + t), colN, y);
            addTextRight("PRODUIT3." + yy, insertCurrencySpaces("" + t), 580, y);
            yy++;
        }

        y += 2;
        for (int i = 0; i < 5; i++) {
            addTextRight("CHARGES3." + yy, insertCurrencySpaces("" + t), colN, y);
            addTextRight("CHARGES4." + yy, insertCurrencySpaces("" + t), 608 - 28, y);
            yy++;
            y -= cellHeight;
        }

        y += cellHeight;
        t = t / 100;
        yy--;
        addTextRight("CBAIL_MO" + yy, insertCurrencySpaces("" + t), 280, y + 4);
        addTextRight("CBAIL_IMMO" + yy, insertCurrencySpaces("" + t), 396, y + 4);
        yy++;
        y -= cellHeight;
        y += 2;
        //

        addTextRight("CHARGES1." + yy, insertCurrencySpaces("" + t), 392, y);
        addTextRight("CHARGES2." + yy, insertCurrencySpaces("" + t), colN, y);
        addTextRight("CHARGES3." + yy, insertCurrencySpaces("" + t), 580, y);
        yy++;
        y -= cellHeight;
        // Remuneration du personnel
        for (int i = 0; i < 4; i++) {
            addTextRight("CHARGES3." + yy, insertCurrencySpaces("" + t), colN, y);
            addTextRight("CHARGES4." + yy, insertCurrencySpaces("" + t), 608 - 28, y);
            yy++;
            y -= cellHeight;
        }
        //
        y += 3;
        addTextRight("CHARGES1." + yy, insertCurrencySpaces("" + t), 392, y);
        addTextRight("CHARGES2." + yy, insertCurrencySpaces("" + t), colN, y);
        addTextRight("CHARGES3." + yy, insertCurrencySpaces("" + t), 580, y);
        yy++;
        y -= (cellHeight * 2);
        cellHeight = 17;
        //
        for (int i = 0; i < 9; i++) {
            addTextRight("PCHARGES3." + yy, insertCurrencySpaces("" + t), colN, y);
            addTextRight("PCHARGES4." + yy, insertCurrencySpaces("" + t), 580, y);
            yy++;
            y -= cellHeight;
        }

        for (int i = 0; i < 4; i++) {
            addTextRight("REINT3." + yy, insertCurrencySpaces("" + t), colN, y);
            yy++;
            y -= cellHeight;
        }
        //
        t = t / 100;
        addTextRight("REINT1." + yy, insertCurrencySpaces("" + t), 220, y);
        addTextRight("REINT2." + yy, insertCurrencySpaces("" + t), 401, y);
        addTextRight("REINT3." + yy, insertCurrencySpaces("" + t), colN, y);
        yy++;
        y -= cellHeight;
        for (int i = 0; i < 2; i++) {
            addTextRight("DEDUC1." + yy, insertCurrencySpaces("" + t), 148, y);
            addTextRight("DEDUC2." + yy, insertCurrencySpaces("" + t), 275, y);
            addTextRight("DEDUC3." + yy, insertCurrencySpaces("" + t), 401, y);

            yy++;
            y -= cellHeight;
        }
        yy--;
        addTextRight("DEDUC4." + yy, insertCurrencySpaces("" + t), 580, y + 25);
        yy++;
        //

        addTextRight("DEDUC2." + yy, insertCurrencySpaces("" + t), 237, y);
        addTextRight("DEDUC3." + yy, insertCurrencySpaces("" + t), 392, y);
        addTextRight("DEDUC4." + yy, insertCurrencySpaces("" + t), 580, y);

        yy++;
        y -= cellHeight;
        //
        addTextRight("RES3." + yy, insertCurrencySpaces("" + t), colN, y);
        addTextRight("RES4." + yy, insertCurrencySpaces("" + t), 580, y);
        yy++;
        y -= cellHeight;
        //
        addTextRight("DEF3." + yy, insertCurrencySpaces("" + t), colN, y);
        yy++;
        y -= cellHeight;
        //
        addTextRight("DEF4." + yy, insertCurrencySpaces("" + t), 580, y);
        yy++;
        y -= cellHeight;
        //
        addTextRight("RES3." + yy, insertCurrencySpaces("" + t), colN, y);
        addTextRight("RES4." + yy, insertCurrencySpaces("" + t), 580, y);
        yy++;
        y -= cellHeight;
        //
        addTextRight("COT1." + yy, insertCurrencySpaces("" + t), 195, y);
        addTextRight("COT2." + yy, insertCurrencySpaces("" + t), 401, y);
        addSplittedText("COT3." + yy, "876543", 514, y, 11.5);

        yy++;
        y -= cellHeight;
        y -= 12;
        //
        addTextRight("T1." + yy, insertCurrencySpaces("" + t), 226, y);
        addSplittedText("T2." + yy, "88", 333, y, 14.4);
        addSplittedText("T3." + yy, "88", 406, y, 10);
        addSplittedText("T4." + yy, "88", 464, y, 10);
        yy++;
        y -= cellHeight;
        //
        addTextRight("T1." + yy, insertCurrencySpaces("" + t), 226, y);
        addTextRight("T2." + yy, insertCurrencySpaces("" + t), 461, y);

    }

}
