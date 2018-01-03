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
 
 package org.openconcerto.erp.core.sales.order.element;

import org.openconcerto.erp.core.common.element.NumerotationAutoSQLElement;
import org.openconcerto.erp.core.finance.payment.element.TypeReglementSQLElement;
import org.openconcerto.erp.core.finance.tax.model.TaxeCache;
import org.openconcerto.erp.core.supplychain.stock.element.StockItemsUpdater;
import org.openconcerto.erp.core.supplychain.stock.element.StockItemsUpdater.TypeStockUpdate;
import org.openconcerto.erp.core.supplychain.stock.element.StockLabel;
import org.openconcerto.erp.generationEcritures.GenerationMvtSaisieVenteFacture;
import org.openconcerto.erp.preferences.GestionArticleGlobalPreferencePanel;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.IResultSetHandler;
import org.openconcerto.sql.model.SQLDataSource;
import org.openconcerto.sql.model.SQLInjector;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.preferences.SQLPreferences;
import org.openconcerto.utils.ExceptionHandler;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public class TransfertCommandeAutoUtils {

    private final SQLTable table;

    public TransfertCommandeAutoUtils(SQLTable tableCmd) {
        this.table = tableCmd;
    }

    public SQLTable getTable() {
        return table;
    }

    public void transfertMultiBL(List<SQLRowValues> cmds) {

        Date toDay = new Date();
        for (SQLRowValues sqlRowValues : cmds) {
            try {
                transfertBL(toDay, sqlRowValues);
            } catch (Exception e) {
                ExceptionHandler.handle("Erreur lors du transfert en bl!", e);
            }
        }
    }

    public SQLRow transfertBL(Date dateTransfert, SQLRowValues sqlRowCmd) throws Exception {
        SQLInjector inj = SQLInjector.getInjector(getTable(), getTable().getTable("BON_DE_LIVRAISON"));
        SQLRowValues rowValuesBL = inj.createRowValuesFrom(sqlRowCmd.asRow());

        final SQLTable referentTable = getTable().getTable("BON_DE_LIVRAISON_ELEMENT");

        // Tout livré
        Collection<SQLRowValues> refs = rowValuesBL.getReferentRows(referentTable);
        for (SQLRowValues refRow : refs) {
            refRow.put("QTE_LIVREE", refRow.getObject("QTE"));
        }

        // Date
        rowValuesBL.put("DATE", dateTransfert);

        // N° auto
        rowValuesBL.put("NUMERO", getNextNumero(rowValuesBL.getTable(), Configuration.getInstance().getDirectory().getElement(rowValuesBL.getTable()).getClass(), dateTransfert));

        // Totaux
        rowValuesBL.put("TOTAL_HT", sqlRowCmd.getObject("T_HT"));
        rowValuesBL.put("TOTAL_TVA", sqlRowCmd.getObject("T_TVA"));
        rowValuesBL.put("TOTAL_TTC", sqlRowCmd.getObject("T_TTC"));

        SQLRow row = rowValuesBL.commit();

        inj.commitTransfert(Arrays.asList(sqlRowCmd), row.getID());

        // Mise à jour des stocks
        updateStock(row, referentTable, "Bon de livraison N°");
        return row;
    }

    public void transfertFacture(List<SQLRowValues> cmds) {

        for (SQLRowValues sqlRowValues : cmds) {
            SQLInjector inj = SQLInjector.getInjector(getTable(), getTable().getTable("SAISIE_VENTE_FACTURE"));
            SQLRowValues rowValuesfacture = inj.createRowValuesFrom(sqlRowValues.asRow());

            try {

                // Mode de reglement

                SQLRowValues rowVals = new SQLRowValues(getTable().getTable("MODE_REGLEMENT"));
                rowVals.put("ID_TYPE_REGLEMENT", TypeReglementSQLElement.INDEFINI);
                rowVals.put("DATE_FACTURE", Boolean.TRUE);
                rowVals.put("COMPTANT", Boolean.FALSE);
                rowVals.put("FIN_MOIS", Boolean.FALSE);
                rowVals.put("LENJOUR", 0);
                rowVals.put("AJOURS", 0);

                rowValuesfacture.put("ID_MODE_REGLEMENT", rowVals);

                rowValuesfacture.put("ID_COMMERCIAL", sqlRowValues.getForeignID("ID_COMMERCIAL"));

                // Date
                rowValuesfacture.put("DATE", sqlRowValues.getObject("DATE"));

                // N° auto
                rowValuesfacture.put(
                        "NUMERO",
                        getNextNumero(rowValuesfacture.getTable(), Configuration.getInstance().getDirectory().getElement(rowValuesfacture.getTable()).getClass(), sqlRowValues.getDate("DATE")
                                .getTime()));

                if (getTable().contains("ID_TAXE_PORT")) {
                    SQLRow taxeDefault = TaxeCache.getCache().getFirstTaxe();
                    rowValuesfacture.put("ID_TAXE_PORT", taxeDefault.getID());
                }

                // Totaux
                rowValuesfacture.put("T_HT", sqlRowValues.getObject("T_HT"));
                rowValuesfacture.put("T_TVA", sqlRowValues.getObject("T_TVA"));
                rowValuesfacture.put("T_TTC", sqlRowValues.getObject("T_TTC"));

                SQLRow row = rowValuesfacture.commit();

                inj.commitTransfert(Arrays.asList(sqlRowValues), row.getID());

                new GenerationMvtSaisieVenteFacture(row.getID());

                // Mise à jour des stocks
                updateStock(row, getTable().getTable("SAISIE_VENTE_FACTURE_ELEMENT"), "Facture N°");

            } catch (Exception e) {
                ExceptionHandler.handle("Erreur lors du transfert en facture!", e);
            }
        }

    }

    private String getNextNumero(SQLTable table, Class<? extends SQLElement> element, Date toDay) throws IllegalStateException {
        final SQLTable tableNum = getTable().getTable("NUMEROTATION_AUTO");

        String num = NumerotationAutoSQLElement.getNextNumero(element, toDay);

        final SQLSelect selNum = new SQLSelect();
        selNum.addSelect(table.getKey(), "COUNT");
        final Where w = new Where(table.getField("NUMERO"), "=", num);
        selNum.setWhere(w);
        final String req = selNum.asString();
        final Number l = (Number) getTable().getBase().getDataSource().execute(req, new IResultSetHandler(SQLDataSource.SCALAR_HANDLER, false));

        if (l.intValue() <= 0) {

            final SQLRow rowNum = tableNum.getRow(2);
            SQLRowValues rowValsNum = rowNum.createEmptyUpdateRow();

            String labelNumberFor = NumerotationAutoSQLElement.getLabelNumberFor(element);
            int val = rowNum.getInt(labelNumberFor);
            val++;
            rowValsNum.put(labelNumberFor, Integer.valueOf(val));
            try {
                rowValsNum.update();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            throw new IllegalStateException("La numératotation automatique n'est pas correcte! Un numéro existant est retourné!");
        }

        return num;

    }

    private void updateStock(SQLRow row, SQLTable referentTable, final String labelPrefix) throws SQLException {

        SQLPreferences prefs = new SQLPreferences(getTable().getDBRoot());
        final boolean stockOnFacture = prefs.getBoolean(GestionArticleGlobalPreferencePanel.STOCK_FACT, true);
        if ((row.getTable().getName().equals("BON_DE_LIVRAISON") && !stockOnFacture) || (row.getTable().getName().equals("SAISIE_VENTE_FACTURE") && stockOnFacture)) {
            StockItemsUpdater stockUpdater = new StockItemsUpdater(new StockLabel() {

                @Override
                public String getLabel(SQLRowAccessor rowOrigin, SQLRowAccessor rowElt) {
                    return labelPrefix + rowOrigin.getString("NUMERO");
                }
            }, row, row.getReferentRows(referentTable), TypeStockUpdate.REAL_DELIVER);

            stockUpdater.update();
        }
    }

}
