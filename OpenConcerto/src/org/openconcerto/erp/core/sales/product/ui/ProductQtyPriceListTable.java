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

import org.openconcerto.erp.core.common.ui.DeviseNumericHTConvertorCellEditor;
import org.openconcerto.erp.core.sales.product.component.ReferenceArticleSQLComponent;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.view.list.CellDynamicModifier;
import org.openconcerto.sql.view.list.RowValuesTable;
import org.openconcerto.sql.view.list.RowValuesTableModel;
import org.openconcerto.sql.view.list.RowValuesTablePanel;
import org.openconcerto.sql.view.list.SQLTableElement;
import org.openconcerto.utils.GestionDevise;

import java.awt.Component;
import java.math.BigDecimal;
import java.util.List;
import java.util.Vector;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

public class ProductQtyPriceListTable extends RowValuesTablePanel {

    private SQLTableElement tarif;
    private SQLTable article = Configuration.getInstance().getBase().getTable("ARTICLE");

    private SQLRowValues rowValuesArticleCompile = new SQLRowValues(article);
    SQLTableElement tableElementVenteHT;
    ReferenceArticleSQLComponent comp;

    public ProductQtyPriceListTable(ReferenceArticleSQLComponent comp) {

        init();
        uiInit();
        this.comp = comp;
    }

    // public void setArticleValues(SQLRowAccessor articleAccessor) {
    // rowValuesArticleCompile.put("VALEUR_METRIQUE_1",
    // articleAccessor.getObject("VALEUR_METRIQUE_1"));
    // rowValuesArticleCompile.put("VALEUR_METRIQUE_2",
    // articleAccessor.getObject("VALEUR_METRIQUE_2"));
    // rowValuesArticleCompile.put("VALEUR_METRIQUE_3",
    // articleAccessor.getObject("VALEUR_METRIQUE_2"));
    // rowValuesArticleCompile.put("PRIX_METRIQUE_VT_1",
    // articleAccessor.getObject("PRIX_METRIQUE_VT_1"));
    // rowValuesArticleCompile.put("PRIX_METRIQUE_VT_2",
    // articleAccessor.getObject("PRIX_METRIQUE_VT_2"));
    // rowValuesArticleCompile.put("PRIX_METRIQUE_VT_3",
    // articleAccessor.getObject("PRIX_METRIQUE_VT_3"));
    // rowValuesArticleCompile.put("ID_MODE_VENTE_ARTICLE",
    // articleAccessor.getObject("ID_MODE_VENTE_ARTICLE"));
    // rowValuesArticleCompile.put("ID_TAXE", articleAccessor.getObject("ID_TAXE"));
    // }
    //
    // public void fireModification() {
    //
    // rowValuesArticleCompile.putAll(comp.getDetailsRowValues().getAbsolutelyAll());
    // rowValuesArticleCompile.put("ID_TAXE", comp.getSelectedTaxe());
    //
    // int rows = getRowValuesTable().getRowCount();
    // for (int i = 0; i < rows; i++) {
    // SQLRowValues rowVals = getRowValuesTable().getRowValuesTableModel().getRowValuesAt(i);
    // rowValuesArticleCompile.put("PRIX_METRIQUE_VT_1", rowVals.getObject("PRIX_METRIQUE_VT_1"));
    // this.tableElementVenteHT.fireModification(rowVals);
    // }
    // }

    /**
     * 
     */
    protected void init() {

        final SQLElement e = getSQLElement();

        final List<SQLTableElement> list = new Vector<SQLTableElement>();

        final SQLTableElement eQuantity = new SQLTableElement(e.getTable().getField("QUANTITE")) {
            @Override
            protected Object getDefaultNullValue() {
                return BigDecimal.ONE;
            }
        };
        list.add(eQuantity);

        SQLTableElement eltPourcent = new SQLTableElement(e.getTable().getField("POURCENT_REMISE"));
        list.add(eltPourcent);

        // Prix de vente HT de la métrique 1
        final SQLField field = e.getTable().getField("PRIX_METRIQUE_VT_1");
        // final DeviseNumericHTConvertorCellEditor editorPVHT = new
        // DeviseNumericHTConvertorCellEditor(field);
        this.tableElementVenteHT = new SQLTableElement(field, BigDecimal.class);
        list.add(tableElementVenteHT);

        this.model = new RowValuesTableModel(e, list, e.getTable().getField("QUANTITE"), false, this.defaultRowVals);

        this.table = new RowValuesTable(this.model, null);

        eltPourcent.addModificationListener(tableElementVenteHT);
        tableElementVenteHT.setModifier(new CellDynamicModifier() {
            public Object computeValueFrom(SQLRowValues row, SQLTableElement source) {
                if (row.getObject("POURCENT_REMISE") != null) {
                    return null;
                } else {
                    return row.getObject("PRIX_METRIQUE_VT_1");
                }
            }
        });

        tableElementVenteHT.setRenderer(new CurrencyWithSymbolRenderer());
        tableElementVenteHT.addModificationListener(eltPourcent);
        eltPourcent.setModifier(new CellDynamicModifier() {
            @Override
            public Object computeValueFrom(SQLRowValues row, SQLTableElement source) {
                if (row.getObject("PRIX_METRIQUE_VT_1") != null) {
                    return null;
                } else {
                    return row.getObject("POURCENT_REMISE");
                }
            }
        });

        final TableCellRenderer eltPourcentTableCellRender = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                System.err.println("ProductPriceListTable. prcTableCellRender .getTableCellRendererComponent():" + value);

                final JLabel c = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setHorizontalAlignment(SwingConstants.RIGHT);
                final JTextField fieldMetriqueVT1 = (JTextField) comp.getView("PV_HT").getComp();
                if (value != null && fieldMetriqueVT1.getText().length() > 0) {

                    BigDecimal ht = new BigDecimal(fieldMetriqueVT1.getText());
                    ht = ht.multiply(BigDecimal.ONE.subtract(((BigDecimal) value).movePointLeft(2)));
                    c.setText(value + "% (" + GestionDevise.currencyToString(ht) + ")");
                } else {
                    if (value != null) {
                        c.setText(value + "%");
                    } else {
                        c.setText("");
                    }
                }
                return c;

            }
        };
        eltPourcent.setRenderer(eltPourcentTableCellRender);

    }

    public SQLElement getSQLElement() {
        return Configuration.getInstance().getDirectory().getElement("TARIF_QUANTITE");
    }

}
