package org.openconcerto.modules.reports.olap.formatter;

import java.text.NumberFormat;

import mondrian.spi.CellFormatter;

public class CentsCellFormatter implements CellFormatter {

    @Override
    public String formatCell(Object value) {
        double d = ((Number) value).doubleValue();
        if (d == 0) {
            return "";
        }
        NumberFormat formatter = NumberFormat.getCurrencyInstance();

        return formatter.format(d);
    }

}
