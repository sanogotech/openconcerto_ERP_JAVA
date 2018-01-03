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
 
 package org.openconcerto.erp.core.supplychain.stock.element;

import org.openconcerto.erp.core.sales.product.model.ProductComponent;
import org.openconcerto.erp.core.sales.product.model.ProductHelper;
import org.openconcerto.erp.core.supplychain.stock.element.StockItem.TypeStockMouvement;
import org.openconcerto.sql.model.ConnectionHandlerNoSetup;
import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.SQLDataSource;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLRowValuesListFetcher;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.utils.SQLUtils;
import org.openconcerto.utils.DecimalUtils;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.ListMap;
import org.openconcerto.utils.RTInterruptedException;
import org.openconcerto.utils.Tuple3;
import org.openconcerto.utils.cc.ITransformer;

import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.apache.commons.dbutils.ResultSetHandler;

public class StockItemsUpdater {

    private final StockLabel label;
    private final List<? extends SQLRowAccessor> items;
    private final TypeStockUpdate type;
    private final boolean createMouvementStock;
    private final SQLRowAccessor rowSource;

    private boolean headless = false;

    public static enum TypeStockUpdate {

        VIRTUAL_RECEPT(true, TypeStockMouvement.THEORIQUE), REAL_RECEPT(true, TypeStockMouvement.REEL), VIRTUAL_DELIVER(false, TypeStockMouvement.THEORIQUE), REAL_DELIVER(false,
                TypeStockMouvement.REEL), REAL_VIRTUAL_RECEPT(true,
                        TypeStockMouvement.REEL_THEORIQUE), RETOUR_AVOIR_CLIENT(true, TypeStockMouvement.RETOUR), REAL_VIRTUAL_DELIVER(false, TypeStockMouvement.REEL_THEORIQUE);

        private final boolean entry;
        private final TypeStockMouvement type;

        /**
         * 
         * @param entry
         */
        TypeStockUpdate(boolean entry, TypeStockMouvement type) {
            this.entry = entry;
            this.type = type;
        }

        public boolean isEntry() {
            return entry;
        }

        public TypeStockMouvement getType() {
            return type;
        }
    };

    public StockItemsUpdater(StockLabel label, SQLRowAccessor rowSource, List<? extends SQLRowAccessor> items, TypeStockUpdate t) {
        this(label, rowSource, items, t, true);
    }

    public StockItemsUpdater(StockLabel label, SQLRowAccessor rowSource, List<? extends SQLRowAccessor> items, TypeStockUpdate t, boolean createMouvementStock) {
        this.label = label;
        this.items = items;
        this.type = t;
        this.createMouvementStock = createMouvementStock;
        this.rowSource = rowSource;
        this.headless = GraphicsEnvironment.isHeadless();
    }

    public void setHeadless(boolean headless) {
        this.headless = headless;
    }

    List<Tuple3<SQLRowAccessor, Integer, BigDecimal>> reliquat = new ArrayList<Tuple3<SQLRowAccessor, Integer, BigDecimal>>();

    public void addReliquat(SQLRowAccessor article, int qte, BigDecimal qteUnit) {
        reliquat.add(Tuple3.create(article, qte, qteUnit));
    }

    List<String> requests = new ArrayList<String>();

    public void update() throws SQLException {
        final SQLTable stockTable = this.rowSource.getTable().getTable("STOCK");

        if (this.createMouvementStock) {
            clearExistingMvt(this.rowSource);
        }

        // Mise à jour des stocks des articles non composés
        List<StockItem> stockItems = fetch();

        final ListMap<SQLRow, SQLRowValues> cmd = new ListMap<SQLRow, SQLRowValues>();

        for (StockItem stockItem : stockItems) {

            if (stockItem.isStockInit()) {
                requests.add(stockItem.getUpdateRequest());
            } else {
                SQLRowValues rowVals = new SQLRowValues(stockTable);
                rowVals.put("QTE_REEL", stockItem.getRealQty());
                rowVals.put("QTE_TH", stockItem.getVirtualQty());
                rowVals.put("QTE_LIV_ATTENTE", stockItem.getDeliverQty());
                rowVals.put("QTE_RECEPT_ATTENTE", stockItem.getReceiptQty());
                SQLRowValues rowValsArt = stockItem.getArticle().createEmptyUpdateRow();
                rowValsArt.put("ID_STOCK", rowVals);
                rowValsArt.commit();
            }
            if (!this.type.isEntry()) {
                stockItem.fillCommandeFournisseur(cmd);
            }
        }

        final List<? extends ResultSetHandler> handlers = new ArrayList<ResultSetHandler>(requests.size());
        for (String s : requests) {
            handlers.add(null);
        }
        // FIXME FIRE TABLE CHANGED TO UPDATE ILISTE ??
        try {
            SQLUtils.executeAtomic(stockTable.getDBSystemRoot().getDataSource(), new ConnectionHandlerNoSetup<Object, IOException>() {
                @Override
                public Object handle(SQLDataSource ds) throws SQLException, IOException {
                    SQLUtils.executeMultiple(stockTable.getDBSystemRoot(), requests, handlers);
                    return null;
                }
            });
        } catch (IOException e) {
            ExceptionHandler.handle("Erreur de la mise à jour des stocks!", e);
        }

        final DBRoot root = this.rowSource.getTable().getDBRoot();
        if (root.contains("ARTICLE_ELEMENT")) {
            // Mise à jour des stocks des nomenclatures
            ComposedItemStockUpdater comp = new ComposedItemStockUpdater(root, stockItems);
            comp.update();
        }

        // FIXME Créer une interface de saisie de commande article en dessous du seuil mini de stock
        if (!headless && cmd.size() > 0) {
            String msg = "Les articles suivants sont inférieurs au stock minimum : \n";
            for (SQLRow row : cmd.keySet()) {
                for (SQLRowValues rowVals : cmd.get(row)) {
                    msg += rowVals.getString("CODE") + " " + rowVals.getString("NOM") + "\n";
                }
            }
            final String msgFinal = msg;
            SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {
                    JOptionPane.showMessageDialog(null, msgFinal, "Alerte de stock minimum", JOptionPane.WARNING_MESSAGE);
                }
            });
        }

    }

    /**
     * Suppression des anciens mouvements
     * 
     * @param rowSource
     * @throws SQLException
     * @throws RTInterruptedException
     */
    private void clearExistingMvt(SQLRowAccessor rowSource) throws RTInterruptedException, SQLException {

        List<String> multipleRequests = new ArrayList<String>();

        final SQLTable table = this.rowSource.getTable().getTable("MOUVEMENT_STOCK");
        SQLRowValues rowVals = new SQLRowValues(table);
        rowVals.put("QTE", null);
        rowVals.put("REEL", null);
        SQLRowValues rowValsArt = new SQLRowValues(this.rowSource.getTable().getTable("ARTICLE"));
        SQLRowValues rowValsStock = new SQLRowValues(this.rowSource.getTable().getTable("STOCK"));
        rowValsStock.put("QTE_REEL", null);
        rowValsStock.put("QTE_TH", null);
        rowValsStock.put("QTE_RECEPT_ATTENTE", null);
        rowValsStock.put("QTE_LIV_ATTENTE", null);

        rowValsArt.put("ID_STOCK", rowValsStock);
        rowVals.put("ID_ARTICLE", rowValsArt);

        SQLRowValuesListFetcher fetcher = SQLRowValuesListFetcher.create(rowVals);
        fetcher.setSelTransf(new ITransformer<SQLSelect, SQLSelect>() {

            @Override
            public SQLSelect transformChecked(SQLSelect input) {
                Where w = new Where(table.getField("SOURCE"), "=", StockItemsUpdater.this.rowSource.getTable().getName());
                w = w.and(new Where(table.getField("IDSOURCE"), "=", StockItemsUpdater.this.rowSource.getID()));
                input.setWhere(w);
                return input;
            }
        });

        // On stocke les items pour le calcul total des stocks (sinon le calcul est faux si
        // l'article apparait plusieurs fois
        // ou si
        // on archive un mvt reel et theorique)
        Map<Number, StockItem> items = new HashMap<Number, StockItem>();
        List<SQLRowValues> result = fetcher.fetch();
        for (SQLRowValues sqlRowValues : result) {
            final StockItem item;
            if (!items.containsKey(sqlRowValues.getForeignIDNumber("ID_ARTICLE"))) {
                item = new StockItem(sqlRowValues.getForeign("ID_ARTICLE"));
                items.put(sqlRowValues.getForeignIDNumber("ID_ARTICLE"), item);
            } else {
                item = items.get(sqlRowValues.getForeignIDNumber("ID_ARTICLE"));
            }
            final TypeStockMouvement t;
            if (sqlRowValues.getBoolean("REEL")) {
                t = TypeStockMouvement.REEL;
            } else {
                t = TypeStockMouvement.THEORIQUE;
            }
            item.updateQty(sqlRowValues.getFloat("QTE"), t, true);
            String req = "UPDATE " + sqlRowValues.getTable().getSQLName().quote() + " SET \"ARCHIVE\"=1 WHERE \"ID\"=" + sqlRowValues.getID();
            multipleRequests.add(req);
            multipleRequests.add(item.getUpdateRequest());
        }

        List<? extends ResultSetHandler> handlers = new ArrayList<ResultSetHandler>(multipleRequests.size());
        for (String s : multipleRequests) {
            handlers.add(null);
        }
        SQLUtils.executeMultiple(table.getDBSystemRoot(), multipleRequests, handlers);
    }

    private void fillProductComponent(List<ProductComponent> productComponents, int qte, int index, int level) {
        if (level > 0) {
            for (int i = index; i < items.size(); i++) {
                SQLRowAccessor r = items.get(i);

                if (!r.getTable().contains("NIVEAU") || r.getInt("NIVEAU") >= level) {
                    // On ne calcul pas les stocks pour les éléments ayant des fils (le mouvement de
                    // stock
                    // des fils impactera les stocks automatiquement)
                    if (r.getTable().contains("NIVEAU")) {
                        if (i + 1 < items.size()) {
                            SQLRowAccessor rNext = items.get(i + 1);
                            if (rNext.getInt("NIVEAU") > r.getInt("NIVEAU")) {
                                fillProductComponent(productComponents, qte * r.getInt("QTE"), i + 1, rNext.getInt("NIVEAU"));
                                continue;
                            }
                        }
                    }
                    if ((!r.getTable().contains("NIVEAU") || r.getInt("NIVEAU") == level) && !r.isForeignEmpty("ID_ARTICLE") && r.getForeign("ID_ARTICLE") != null) {
                        productComponents.add(ProductComponent.createFrom(r, qte));
                    }
                } else if (r.getInt("NIVEAU") < level) {
                    // BREAK si on sort de l'article composé
                    break;
                }
            }
        }
    }

    /**
     * Récupére les stocks associés aux articles non composés (inclus les fils des nomenclatures) et
     * les met à jour
     * 
     * @return la liste des stocks à jour
     */
    private List<StockItem> fetch() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        List<StockItem> stockItems = new ArrayList<StockItem>(items.size());
        String mvtStockTableQuoted = rowSource.getTable().getTable("MOUVEMENT_STOCK").getSQLName().quote();

        // Liste des éléments à mettre à jour
        List<ProductComponent> productComponents = new ArrayList<ProductComponent>();
        fillProductComponent(productComponents, 1, 0, 1);
        // for (int i = 0; i < items.size(); i++) {
        // SQLRowAccessor r = items.get(i);
        //
        // // On ne calcul pas les stocks pour les éléments ayant des fils (le mouvement de stock
        // // des fils impactera les stocks automatiquement)
        // if (r.getTable().contains("NIVEAU")) {
        // if (i + 1 < items.size()) {
        // SQLRowAccessor rNext = items.get(i + 1);
        // if (rNext.getInt("NIVEAU") > r.getInt("NIVEAU")) {
        // continue;
        // }
        // }
        // }
        // if (!r.isForeignEmpty("ID_ARTICLE")) {
        // productComponents.add(ProductComponent.createFrom(r));
        // }
        // }

        // Liste des articles non composés à mettre à jour (avec les fils des nomenclatures)
        ProductHelper helper = new ProductHelper(rowSource.getTable().getDBRoot());
        List<ProductComponent> boms = helper.getChildWithQtyFrom(productComponents);

        for (ProductComponent productComp : boms) {

            if (productComp.getProduct().getBoolean("GESTION_STOCK") && productComp.getQty().signum() != 0) {
                StockItem stockItem = new StockItem(productComp.getProduct());
                double qteFinal = productComp.getQty().doubleValue();

                // reliquat
                for (Tuple3<SQLRowAccessor, Integer, BigDecimal> t : reliquat) {
                    if (stockItem.getArticle() != null && stockItem.getArticle().equalsAsRow(t.get0())) {
                        double qteFinalReliquat = t.get2().multiply(new BigDecimal(t.get1()), DecimalUtils.HIGH_PRECISION).doubleValue();
                        qteFinal -= qteFinalReliquat;
                    }
                }
                if (!this.type.isEntry()) {
                    qteFinal = -qteFinal;
                }

                stockItem.updateQty(qteFinal, this.type.getType());
                stockItems.add(stockItem);
                if (this.createMouvementStock) {
                    final Date time = this.rowSource.getDate("DATE").getTime();
                    BigDecimal prc = productComp.getPRC(time);
                    if (this.type.getType() == TypeStockMouvement.REEL || this.type.getType() == TypeStockMouvement.REEL_THEORIQUE || this.type.getType() == TypeStockMouvement.RETOUR) {
                        String mvtStockQuery = "INSERT INTO " + mvtStockTableQuoted + " (\"QTE\",\"DATE\",\"ID_ARTICLE\",\"SOURCE\",\"IDSOURCE\",\"NOM\",\"REEL\",\"ORDRE\"";

                        if (prc != null) {
                            mvtStockQuery += ",\"PRICE\"";
                        }

                        mvtStockQuery += ") VALUES(" + qteFinal + ",'" + dateFormat.format(time) + "'," + productComp.getProduct().getID() + ",'" + this.rowSource.getTable().getName() + "',"
                                + this.rowSource.getID() + ",'" + this.label.getLabel(this.rowSource, productComp.getProduct()) + "',true, (SELECT (MAX(\"ORDRE\")+1) FROM " + mvtStockTableQuoted
                                + ")";
                        if (prc != null) {
                            mvtStockQuery += "," + prc.setScale(6, RoundingMode.HALF_UP).toString();
                        }
                        mvtStockQuery += ")";
                        this.requests.add(mvtStockQuery);
                    }
                    if (this.type.getType() == TypeStockMouvement.THEORIQUE || this.type.getType() == TypeStockMouvement.REEL_THEORIQUE || this.type.getType() == TypeStockMouvement.RETOUR) {
                        String mvtStockQuery = "INSERT INTO " + mvtStockTableQuoted + " (\"QTE\",\"DATE\",\"ID_ARTICLE\",\"SOURCE\",\"IDSOURCE\",\"NOM\",\"REEL\",\"ORDRE\"";
                        if (prc != null) {
                            mvtStockQuery += ",\"PRICE\"";
                        }

                        mvtStockQuery += ") VALUES(" + qteFinal + ",'" + dateFormat.format(time) + "'," + productComp.getProduct().getID() + ",'" + this.rowSource.getTable().getName() + "',"
                                + this.rowSource.getID() + ",'" + this.label.getLabel(this.rowSource, productComp.getProduct()) + "',false, (SELECT (MAX(\"ORDRE\")+1) FROM " + mvtStockTableQuoted
                                + ")";
                        if (prc != null) {
                            mvtStockQuery += "," + prc.setScale(6, RoundingMode.HALF_UP).toString();
                        }
                        mvtStockQuery += ")";
                        this.requests.add(mvtStockQuery);
                    }
                }
            }
        }

        return stockItems;
    }
}
