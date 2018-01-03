package org.openconcerto.modules.reports.olap.renderer;

public class MatrixCell {
    final String value;
    final boolean right;
    final boolean sameAsPrev;

    /**
     * Creates a matrix cell.
     * 
     * @param value Value
     * @param right Whether value is right-justified
     * @param sameAsPrev Whether value is the same as the previous value. If true, some formats
     *        separators between cells
     */
    MatrixCell(String value, boolean right, boolean sameAsPrev) {
        this.value = value;
        this.right = right;
        this.sameAsPrev = sameAsPrev;
    }
}
