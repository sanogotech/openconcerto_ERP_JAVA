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
 
 package org.openconcerto.erp.generationDoc;

import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;

import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.Size2DSyntax;
import javax.print.attribute.standard.MediaPrintableArea;
import javax.print.attribute.standard.MediaSizeName;

import org.jopendocument.model.OpenDocument;
import org.jopendocument.print.ODTPrinterXML;

public class ODTPrinterNX extends ODTPrinterXML {

    public ODTPrinterNX(final OpenDocument doc) {
        super(doc);
        this.renderer.setPaintMaxResolution(true);
    }

    /**
     * Print the document (synchronously)
     * 
     * @throws PrinterException
     */
    public void print(PrinterJob job) throws PrinterException {
        PrinterJob printJob = job;
        if (printJob == null) {
            printJob = PrinterJob.getPrinterJob();
            // L'impression est forcée en A4, sur OpenSuse le format est en
            // Letter par défaut alors que l'imprimante est en A4 dans le système
            final PrintRequestAttributeSet attributes = new HashPrintRequestAttributeSet();
            final MediaSizeName media = MediaSizeName.ISO_A4;
            attributes.add(media);
            final Paper paper = new A4();
            final double POINTS_PER_INCH = 72.0;
            final MediaPrintableArea printableArea = new MediaPrintableArea((float) (paper.getImageableX() / POINTS_PER_INCH), (float) (paper.getImageableY() / POINTS_PER_INCH),
                    (float) (paper.getImageableWidth() / POINTS_PER_INCH), (float) (paper.getImageableHeight() / POINTS_PER_INCH), Size2DSyntax.INCH);
            attributes.add(printableArea);

            final PageFormat format = printJob.getPageFormat(null);
            format.setPaper(paper);
            printJob.setPrintable(this, format);
            printJob.print(attributes);
        } else {
            final PageFormat format = job.getPageFormat(null);
            final Paper paper = new A4();
            format.setPaper(paper);
            printJob.setPrintable(this, format);
            printJob.print();
        }
    }
}
