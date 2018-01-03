package org.openconcerto.modules.operation;

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