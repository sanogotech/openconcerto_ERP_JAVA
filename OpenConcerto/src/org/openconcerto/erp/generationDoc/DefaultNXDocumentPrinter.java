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

import java.awt.print.PrinterJob;
import java.util.List;

import org.jopendocument.model.OpenDocument;
import org.jopendocument.print.DocumentPrinter;

public class DefaultNXDocumentPrinter implements DocumentPrinter {

    public DefaultNXDocumentPrinter() {
    }

    @Override
    public void print(List<OpenDocument> documents) {
        print(documents, null);
    }

    public void print(List<OpenDocument> documents, PrinterJob job) {
        for (OpenDocument doc : documents) {
            ODTPrinterNX p = new ODTPrinterNX(doc);
            try {
                p.print(job);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

    }

}
