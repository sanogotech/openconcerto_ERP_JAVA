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

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.view.list.CellDynamicModifier;
import org.openconcerto.sql.view.list.RowValuesTable;
import org.openconcerto.sql.view.list.RowValuesTableModel;
import org.openconcerto.sql.view.list.RowValuesTablePanel;
import org.openconcerto.sql.view.list.SQLTableElement;

import java.math.BigDecimal;
import java.util.List;
import java.util.Vector;

public class ProductItemListTable extends RowValuesTablePanel {

    public ProductItemListTable() {
        init();
        uiInit();

    }

    /**
     * 
     */
    protected void init() {

        final SQLElement eProductItem = getSQLElement();
        final SQLTable productItemTable = eProductItem.getTable();

        final List<SQLTableElement> list = new Vector<SQLTableElement>();

        final SQLTableElement product = new SQLTableElement(productItemTable.getField("ID_ARTICLE"));
        list.add(product);
        // Quantité
        final SQLTableElement q = new SQLTableElement(productItemTable.getField("QTE"), Integer.class);
        list.add(q);
        // Quantité
        final SQLTableElement quantity = new SQLTableElement(productItemTable.getField("QTE_UNITAIRE"), BigDecimal.class);
        list.add(quantity);
        // Unité de vente
        final SQLTableElement unit = new SQLTableElement(productItemTable.getField("ID_UNITE_VENTE"));

        unit.setEditable(true);
        unit.setModifier(new CellDynamicModifier() {

            @Override
            public Object computeValueFrom(SQLRowValues row, SQLTableElement source) {
                if (source.equals(product)) {

                    final Object object = row.getObject("ID_ARTICLE");
                    if (object == null) {
                        return null;
                    }
                    SQLRowAccessor r = row.getForeign("ID_ARTICLE");
                    return r.getForeignIDNumber("ID_UNITE_VENTE");
                } else {
                    return row.getObject("ID_UNITE_VENTE");
                }
            }

        });
        product.addModificationListener(unit);
        list.add(unit);

        this.defaultRowVals = new SQLRowValues(getSQLElement().getTable());
        this.defaultRowVals.put("ID_ARTICLE", null);
        this.defaultRowVals.put("QTE", Integer.valueOf(1));
        this.defaultRowVals.put("QTE_UNITAIRE", BigDecimal.valueOf(1));

        this.model = new RowValuesTableModel(eProductItem, list, productItemTable.getField("ID_ARTICLE"), false, this.defaultRowVals);

        this.table = new RowValuesTable(this.model, null);

    }

    public SQLElement getSQLElement() {
        return Configuration.getInstance().getDirectory().getElement("ARTICLE_ELEMENT");
    }

}
