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
 
 package org.openconcerto.erp.graph;

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElementDirectory;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.utils.DecimalUtils;
import org.openconcerto.utils.NumberUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;

import org.jopenchart.AxisLabel;
import org.jopenchart.barchart.VerticalBarChart;

public class MargeDataModel extends MonthDataModel {

    private VerticalBarChart chart;

    public MargeDataModel(final VerticalBarChart chart, final int year) {
        super(year, false);
        this.chart = chart;
    }

    @Override
    public long computeValue(Date d1, Date d2) {
        final SQLElementDirectory directory = Configuration.getInstance().getDirectory();
        SQLTable tableSaisieVenteF = directory.getElement("SAISIE_VENTE_FACTURE").getTable();
        SQLTable tableSaisieVenteFElt = directory.getElement("SAISIE_VENTE_FACTURE_ELEMENT").getTable();
        final SQLSelect sel = new SQLSelect(tableSaisieVenteF.getBase());
        sel.addSelect(tableSaisieVenteFElt.getField("T_PA_HT"), "SUM");
        sel.addSelect(tableSaisieVenteFElt.getField("T_PV_HT"), "SUM");
        final Where w = new Where(tableSaisieVenteF.getField("DATE"), d1, d2);
        final Where w2 = new Where(tableSaisieVenteFElt.getField("ID_SAISIE_VENTE_FACTURE"), "=", tableSaisieVenteF.getKey());
        sel.setWhere(w.and(w2));

        BigDecimal total = BigDecimal.ZERO;
        Object[] o = tableSaisieVenteF.getBase().getDataSource().executeA1(sel.asString());
        if (o != null) {
            BigDecimal pa = (BigDecimal) o[0];
            BigDecimal pv = (BigDecimal) o[1];
            if (pa != null && pv != null && (!NumberUtils.areNumericallyEqual(pa, BigDecimal.ZERO) || !NumberUtils.areNumericallyEqual(pv, BigDecimal.ZERO))) {
                total = pv.subtract(pa);
            }
        }

        final long value = total.longValue();
        if (value > chart.getHigherRange().longValue()) {
            final List<AxisLabel> labels = chart.getLeftAxis().getLabels();
            labels.get(2).setLabel(total.setScale(0, RoundingMode.HALF_UP).toString() + " €");
            labels.get(1).setLabel(total.divide(new BigDecimal(2), DecimalUtils.HIGH_PRECISION).setScale(0, RoundingMode.HALF_UP) + " €");
            chart.setHigherRange(value);
        }
        return value;
    }
}
