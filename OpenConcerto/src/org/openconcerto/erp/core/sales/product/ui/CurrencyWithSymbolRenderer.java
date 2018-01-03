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
 
 package org.openconcerto.erp.core.sales.product.ui;

import org.openconcerto.erp.core.finance.accounting.model.CurrencyConverter;
import org.openconcerto.sql.model.FieldPath;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.graph.Step;
import org.openconcerto.sql.view.list.RowValuesTable;
import org.openconcerto.sql.view.list.RowValuesTableModel;
import org.openconcerto.utils.GestionDevise;

import java.awt.Component;
import java.math.BigDecimal;
import java.util.List;

import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

public class CurrencyWithSymbolRenderer extends DefaultTableCellRenderer {

    private final FieldPath fieldPath;
    private final CurrencyConverter c;

    private String getSymbol(String currencyCode) {
        // Because Java Currency return PLN as Symbol for Zl, we use our own talbe
        return org.openconcerto.erp.core.finance.accounting.model.Currency.getSymbol(currencyCode);
    }

    /**
     * Affiche une valeur monétaire en ajoutant le symbole de la devise de la société
     */
    public CurrencyWithSymbolRenderer() {
        this(null);
    }

    /**
     * Affiche une valeur monétaire en ajoutant le symbole de la devise du path
     * 
     * @param path chemin jusqu'au champ DEVISE.CODE
     */
    public CurrencyWithSymbolRenderer(FieldPath path) {
        this.fieldPath = path;
        this.c = new CurrencyConverter();
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

        if (value != null) {
            value = GestionDevise.currencyToString((BigDecimal) value);

            if (fieldPath == null) {
                value = value + " " + getSymbol(c.getCompanyCurrencyCode());
            } else {

                if (table instanceof RowValuesTable) {

                    RowValuesTableModel model = ((RowValuesTable) table).getRowValuesTableModel();

                    SQLRowAccessor rowVals = model.getRowValuesAt(row);

                    // final SQLRow asRow = rowVals.asRow();
                    // SQLRow deviseRow = asRow;
                    // Ne pas utiliser getDistant row --> fait une requete dans la base et ne
                    // reprend
                    // pas les valeurs de la SQLRow (ex : si on veut récupérer la devise du
                    // fournisseur
                    // sélectionné, getDistantRow ira chercher la valeur du fournisseur référencé en
                    // BDD
                    // et non dans la SQLRow)

                    final List<Step> steps = this.fieldPath.getPath().getSteps();
                    for (int i = 0; i < steps.size(); i++) {
                        final Step s = steps.get(i);
                        // On s'assure que la row existe
                        if (rowVals != null && !rowVals.isUndefined()) {
                            // On s'assure que la row contient le champ
                            if (!rowVals.getFields().contains(s.getSingleField().getName())) {
                                rowVals = null;
                                break;
                            }
                            final SQLRowAccessor foreign = rowVals.getForeign(s.getSingleField().getName());
                            if (i == 0 && foreign != null) {
                                rowVals = foreign.asRow();
                            } else {
                                rowVals = foreign;
                            }
                        }
                    }

                    if (rowVals != null && !rowVals.isUndefined()) {
                        String code = rowVals.getString(this.fieldPath.getFieldName());
                        value = value + " " + getSymbol(code);
                    }
                }
            }
        }
        setHorizontalAlignment(SwingConstants.RIGHT);

        return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    }
}
