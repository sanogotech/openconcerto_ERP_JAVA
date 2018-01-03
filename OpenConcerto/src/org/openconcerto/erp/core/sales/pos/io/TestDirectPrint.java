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

import java.awt.print.PrinterJob;
import java.io.ByteArrayInputStream;

import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintException;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.SimpleDoc;
import javax.print.attribute.AttributeSet;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.HashPrintServiceAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.PrinterName;
import javax.swing.JOptionPane;

public class TestDirectPrint {

    /**
     * @param args
     */
    public static void main(String[] args) {
        ESCSerialPrinter tot = new ESCSerialPrinter("COM");
        tot.addToBuffer("hello");
        print(tot.getPrintBufferBytes());

    }

    private static boolean feedPrinter(byte[] b) {
        try {

            AttributeSet attrSet = new HashPrintServiceAttributeSet(new PrinterName("EPSON TM-T20 Receipt", null)); // EPSON
                                                                                                                    // TM-U220
                                                                                                                    // ReceiptE4

            DocPrintJob job = PrintServiceLookup.lookupPrintServices(null, attrSet)[0].createPrintJob();
            // PrintServiceLookup.lookupDefaultPrintService().createPrintJob();

            DocFlavor flavor = DocFlavor.BYTE_ARRAY.AUTOSENSE;
            Doc doc = new SimpleDoc(b, flavor, null);
            // PrintJobWatcher pjDone = new PrintJobWatcher(job);

            job.print(doc, null);
            //
            System.out.println("Done !");
        } catch (javax.print.PrintException pex) {

            System.out.println("Printer Error " + pex.getMessage());
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public static void print(byte[] bytes) {
        PrintService ps = PrintServiceLookup.lookupDefaultPrintService();
        try {



          

            PrinterJob pj1 = PrinterJob.getPrinterJob();

            if (pj1.printDialog()) {
                System.err.println("TestDirectPrint.print()" + pj1.getPrintService().getName());
                ps = pj1.getPrintService();
                DocFlavor[] p = ps.getSupportedDocFlavors();
                for (int i = 0; i < p.length; i++) {
                    DocFlavor docFlavor = p[i];
                    System.out.println(docFlavor.toString());
                }
                System.err.println("TestDirectPrint.print()");
                System.out.println(java.util.Arrays.toString(ps.getSupportedAttributeCategories()));
            }
            // if (pj.printDialog()) {
            // now print!!
            if (1 > 0) {
                // PrintService ps =
                // PrintServiceLookup.lookupDefaultPrintService();
                // PrintServiceLookup.
                if (ps != null) {
                    // JOptionPane.showMessageDialog(null,"selected        printer " +ps.getName());
                    // System.out.println("selectedter " + ps.getName());

                    PrintRequestAttributeSet aset = new HashPrintRequestAttributeSet();
                    DocFlavor flavor = DocFlavor.INPUT_STREAM.AUTOSENSE;
                    // ----------DocFlavor flavor = DocFlavor.SERVICE_FORMATTED.PRINTABLE;

                    // aset.add(new MediaPrintableArea(100, 400, 210, 160, Size2DSyntax.MM));

                    DocPrintJob pj = ps.createPrintJob();
                    try {

                        ByteArrayInputStream b = new ByteArrayInputStream(bytes);

                        Doc doc = new SimpleDoc(b, flavor, null);
                        pj.print(doc, aset);

                        JOptionPane.showMessageDialog(null, "end        printing");
                    } catch (PrintException e) {
                        e.printStackTrace();
                        JOptionPane.showMessageDialog(null, e.getMessage());

                    } catch (Exception e1) {
                        e1.printStackTrace();
                        JOptionPane.showMessageDialog(null, e1.getMessage());
                    }

                } else {

                    JOptionPane.showMessageDialog(null, "no        Printer");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {

        }
    }
}
