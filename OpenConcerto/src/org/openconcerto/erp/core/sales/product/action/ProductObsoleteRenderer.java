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
 
 package org.openconcerto.erp.core.sales.product.action;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import org.openconcerto.erp.core.finance.accounting.ui.PointageRenderer;
import org.openconcerto.erp.core.finance.accounting.ui.EcritureCheckedRenderer.EcritureUtils;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.view.list.ITableModel;
import org.openconcerto.ui.table.TableCellRendererDecorator;
import org.openconcerto.ui.table.TableCellRendererDecorator.TableCellRendererDecoratorUtils;
import org.openconcerto.ui.table.TableCellRendererUtils;

public class ProductObsoleteRenderer extends TableCellRendererDecorator {

    static public final ProductObsoleteUtils<ProductObsoleteRenderer> UTILS = new ProductObsoleteUtils<ProductObsoleteRenderer>(ProductObsoleteRenderer.class);

    // so that all subclasses replace one another, e.g. LettrageRenderer replaces
    // ListEcritureRenderer
    public final static class ProductObsoleteUtils<R extends ProductObsoleteRenderer> extends TableCellRendererDecoratorUtils<R> {

        protected ProductObsoleteUtils(Class<R> clazz) {
            super(clazz);
        }

        @Override
        protected boolean replaces(TableCellRenderer r) {
            return ProductObsoleteRenderer.class.isAssignableFrom(r.getClass());
        }
    }

    public ProductObsoleteRenderer(TableCellRenderer r) {
        super(r);
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

        final Component res = getRenderer(table, column).getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        TableCellRendererUtils.setBackgroundColor(res, table, isSelected);

        if (!isSelected) {
            final SQLRowValues ecritureRow = ITableModel.getLine(table.getModel(), row).getRow();
            if (ecritureRow.getBoolean("OBSOLETE")) {
                res.setBackground(Color.red);
            }
        }

        return res;
    }
}
