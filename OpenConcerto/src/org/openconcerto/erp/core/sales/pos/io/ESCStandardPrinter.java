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
 
 package org.openconcerto.erp.core.sales.pos.io;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintException;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.SimpleDoc;
import javax.print.attribute.AttributeSet;
import javax.print.attribute.HashPrintServiceAttributeSet;
import javax.print.attribute.standard.PrinterName;

public class ESCStandardPrinter extends AbstractESCPrinter {

    private String printerName;

    public ESCStandardPrinter(String printerName) {
        if (printerName.isEmpty() || printerName.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing printer name");
        }
        this.printerName = printerName;
    }

    public String getPrinterName() {
        return printerName;
    }

    @Override
    public void printBuffer() throws Exception {
        sendBytes(getPrintBufferBytes());
    }

    @Override
    public void openDrawer() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // Pin 2, 200ms min
        out.write(0x10);// DLE
        out.write(0x14);// DC4
        out.write(0x01);
        out.write(0x00);// Pin 2
        out.write(0x02);

        sendBytes(out.toByteArray());
        try {
            // 300ms to ensure opening
            Thread.sleep(300);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        out = new ByteArrayOutputStream();
        // Pin 5, 200ms
        out.write(0x10);// DLE
        out.write(0x14);// DC4
        out.write(0x01);
        out.write(0x01);// Pin 5
        out.write(0x02);
        sendBytes(out.toByteArray());
    }

    private synchronized void sendBytes(byte[] b) throws PrintException {
        final AttributeSet attrSet = new HashPrintServiceAttributeSet(new PrinterName(this.printerName, null));
        final PrintService[] lookupPrintServices = PrintServiceLookup.lookupPrintServices(null, attrSet);
        if (lookupPrintServices.length <= 0) {
            throw new PrintException("Printer " + this.printerName + " not found");
        }
        final DocPrintJob job = lookupPrintServices[0].createPrintJob();
        final DocFlavor flavor = DocFlavor.BYTE_ARRAY.AUTOSENSE;
        final Doc doc = new SimpleDoc(b, flavor, null);
        job.print(doc, null);
    }

    public static void main(String[] args) {
        String printer = "EPSON TM-T20 Receipt";
        final ESCStandardPrinter prt = new ESCStandardPrinter(printer);
        int col = 42;
        prt.addToBuffer("ILM INFORMATIQUE", BOLD_LARGE);
        prt.addToBuffer("");
        prt.addToBuffer("22 place de la liberation");
        prt.addToBuffer("80100 ABBEVILLE");
        prt.addToBuffer("Tél: 00 00 00 00 00");
        prt.addToBuffer("Fax: 00 00 00 00 00");
        prt.addToBuffer("");
        final SimpleDateFormat df = new SimpleDateFormat("EEEE d MMMM yyyy à HH:mm");
        prt.addToBuffer(formatRight(42, "Le " + df.format(Calendar.getInstance().getTime())));
        prt.addToBuffer("");
        prt.addToBuffer(formatRight(5, "3") + " " + formatLeft(col - 6 - 9, "ILM Informatique") + " " + formatRight(8, "3.00"));
        prt.addToBuffer(formatLeft(col, "      ======================================="));
        prt.addToBuffer(formatRight(col - 8, "Total") + formatRight(8, "3.00"), BOLD);
        prt.addToBuffer("");
        prt.addToBuffer("Merci de votre visite, à bientôt.");
        prt.addToBuffer("");
        prt.addToBuffer("01 05042010 00002", BARCODE);

        try {
            prt.printBuffer();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
