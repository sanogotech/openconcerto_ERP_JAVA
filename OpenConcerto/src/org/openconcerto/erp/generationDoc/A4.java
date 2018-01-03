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

import java.awt.print.Paper;

public class A4 extends Paper {
    private static final double INCH_TO_MM = 25.4;
    private static final int DPI = 72;

    public A4() {
        this(24, 32);
    }

    public A4(float hMargin, float vMargin) {
        super();
        final double width = (210 * DPI) / INCH_TO_MM;
        final double height = (297 * DPI) / INCH_TO_MM;
        setSize(width, height);
        setImageableArea(hMargin, vMargin, width + 2 * hMargin, height - 2 * vMargin);
    }
}
