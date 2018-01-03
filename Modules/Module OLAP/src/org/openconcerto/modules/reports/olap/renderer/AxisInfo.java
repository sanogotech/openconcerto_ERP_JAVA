package org.openconcerto.modules.reports.olap.renderer;

import java.util.ArrayList;
import java.util.List;

public class AxisInfo {

    final List<AxisOrdinalInfo> ordinalInfos;

    int width = 0;
    /**
     * Creates an AxisInfo.
     * 
     * @param ordinalCount Number of hierarchies on this axis
     */
    AxisInfo(int ordinalCount) {
        ordinalInfos = new ArrayList<AxisOrdinalInfo>(ordinalCount);
        for (int i = 0; i < ordinalCount; i++) {
            ordinalInfos.add(new AxisOrdinalInfo());
        }
        computeWidth();

    }

    public void computeWidth() {
        width = 0;
        for (AxisOrdinalInfo info : ordinalInfos) {
            width += info.getWidth();
        }
    }

    /**
     * Returns the number of matrix columns required by this axis. The sum of the width of the
     * hierarchies on this axis.
     * 
     * @return Width of axis
     */
    public int getWidth() {
        return width;
    }

}
