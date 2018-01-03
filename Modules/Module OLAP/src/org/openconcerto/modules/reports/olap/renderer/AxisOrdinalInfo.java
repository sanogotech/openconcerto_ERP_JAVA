package org.openconcerto.modules.reports.olap.renderer;
public class AxisOrdinalInfo {
    /**
     * Description of a particular hierarchy mapped to an axis.
     */
    int minDepth = 1;
    int maxDepth = 0;

    /**
     * Returns the number of matrix columns required to display this hierarchy.
     */
    public int getWidth() {
        return maxDepth - minDepth + 1;
    }

}
