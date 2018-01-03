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
 
 package org.openconcerto.erp.core.sales.pos.ui;

import java.io.IOException;

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

public class PrinterPOSDisplay extends POSDisplay {

    private String printerName;

    public PrinterPOSDisplay(String lcdPort) {
        this.printerName = lcdPort;
    }

    @Override
    public void setMessage(String line1, String line2) throws Exception, IOException {
        if (line1 == null) {
            line1 = "";
        }
        if (line2 == null) {
            line2 = "";
        }
        // clear
        sendBytes(new byte[] { 12 });
        // send the two lines
        sendBytes((line1 + "\r\n" + line2 + "\r\n").getBytes());
    }

    private synchronized void sendBytes(byte[] b) throws PrintException {
        if (this.printerName == null || this.printerName.isEmpty()) {
            return;
        }
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

    public static void main(String[] args) throws IOException, Exception {
        PrinterPOSDisplay p = new PrinterPOSDisplay("EPSON DM-D Display");
        p.setMessage("OpenConcerto", "Caisse ouverte");
    }
}
