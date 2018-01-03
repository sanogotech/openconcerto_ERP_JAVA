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

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.common.component.TransfertBaseSQLComponent;
import org.openconcerto.erp.core.common.component.TransfertGroupSQLComponent;
import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.erp.core.sales.account.VenteFactureSituationSQLComponent;
import org.openconcerto.erp.core.sales.account.VenteFactureSoldeSQLComponent;
import org.openconcerto.erp.core.sales.order.component.CommandeClientSQLComponent;
import org.openconcerto.erp.core.sales.order.report.CommandeClientXmlSheet;
import org.openconcerto.erp.core.sales.order.ui.EtatCommandeClient;
import org.openconcerto.erp.core.sales.product.element.ReferenceArticleSQLElement;
import org.openconcerto.erp.core.sales.shipment.component.BonDeLivraisonSQLComponent;
import org.openconcerto.erp.core.supplychain.stock.element.MouvementStockSQLElement;
import org.openconcerto.erp.model.MouseSheetXmlListeListener;
import org.openconcerto.erp.preferences.GestionCommercialeGlobalPreferencePanel;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.element.SQLElementLink.LinkType;
import org.openconcerto.sql.element.SQLElementLinksSetup;
import org.openconcerto.sql.element.TreesOfSQLRows;
import org.openconcerto.sql.model.ConnectionHandlerNoSetup;
import org.openconcerto.sql.model.SQLDataSource;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLInjector;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowListRSH;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLRowValuesListFetcher;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLSelectJoin;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.preferences.SQLPreferences;
import org.openconcerto.sql.request.UpdateBuilder;
import org.openconcerto.sql.utils.SQLUtils;
import org.openconcerto.sql.view.list.IListe;
import org.openconcerto.sql.view.list.IListeAction.IListeEvent;
import org.openconcerto.sql.view.list.RowAction;
import org.openconcerto.sql.view.list.RowAction.PredicateRowAction;
import org.openconcerto.sql.view.list.SQLTableModelColumn;
import org.openconcerto.sql.view.list.SQLTableModelSource;
import org.openconcerto.utils.CompareUtils;
import org.openconcerto.utils.DecimalUtils;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.ListMap;
import org.openconcerto.utils.NumberUtils;
import org.openconcerto.utils.cc.ITransformer;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;

import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.ArrayListHandler;

public class CommandeClientSQLElement extends ComptaSQLConfElement {

    public CommandeClientSQLElement() {
        super("COMMANDE_CLIENT", "une commande client", "commandes clients");

        SQLPreferences prefs = new SQLPreferences(getTable().getDBRoot());
        if (prefs.getBoolean(GestionCommercialeGlobalPreferencePanel.ORDER_PACKAGING_MANAGEMENT, true)) {

            for (final EtatCommandeClient etat : EtatCommandeClient.values()) {

                PredicateRowAction action = new PredicateRowAction(new AbstractAction(etat.getTranslation()) {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        changeStateOfRows(IListe.get(e).getSelectedRows(), etat);
                    }
                }, false);
                action.setPredicate(IListeEvent.getNonEmptySelectionPredicate());
                action.setPath(Arrays.asList("Etat", "Etat", "Etat"));
                getRowActions().add(action);
            }

            PredicateRowAction actionTransfertBL = new PredicateRowAction(new AbstractAction("Transfert automatique vers BL") {

                @Override
                public void actionPerformed(ActionEvent e) {
                    TransfertCommandeAutoUtils transfert = new TransfertCommandeAutoUtils(getTable());
                    transfert.transfertMultiBL(IListe.get(e).getSelectedRows());
                }
            }, false);
            actionTransfertBL.setPredicate(IListeEvent.getNonEmptySelectionPredicate());
            getRowActions().add(actionTransfertBL);

            PredicateRowAction actionStock = new PredicateRowAction(new AbstractAction("Vérification des stocks") {

                @Override
                public void actionPerformed(ActionEvent e) {
                    new Thread("Check Commande To Ship") {
                        public void run() {
                            try {
                                checkCommandeToShip();
                            } catch (Exception e) {
                                ExceptionHandler.handle("Erreur pendant la vérification du statut des commandes", e);
                            }
                        }
                    }.start();
                }

            }, false);
            actionStock.setPredicate(IListeEvent.getNonEmptySelectionPredicate());
            getRowActions().add(actionStock);

            PredicateRowAction actionFacture = new PredicateRowAction(new AbstractAction("Transfert automatique en facture") {

                @Override
                public void actionPerformed(ActionEvent e) {
                    TransfertCommandeAutoUtils transfert = new TransfertCommandeAutoUtils(getTable());
                    transfert.transfertFacture(IListe.get(e).getSelectedRows());
                }
            }, false);
            actionFacture.setPredicate(IListeEvent.getNonEmptySelectionPredicate());
            getRowActions().add(actionFacture);

        }

        final List<RowAction> allowedActions = new ArrayList<RowAction>();
        // Transfert vers facture
        PredicateRowAction bonAction = new PredicateRowAction(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                transfertBonLivraisonClient(IListe.get(e).getSelectedRows());
            }
        }, false, "sales.order.create.deliverynote");

        // Transfert vers facture
        RowAction factureAction = new RowAction(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                transfertFactureClient(IListe.get(e).getSelectedRows());
            }
        }, false, "sales.order.create.invoice") {

            @Override
            public boolean enabledFor(List<SQLRowValues> selection) {
                if (selection.isEmpty()) {
                    return false;
                } else if (selection.size() > 1) {
                    return true;
                } else {
                    BigDecimal d = getAvancement(selection.get(0));
                    return d.signum() == 0;
                }
            }
        };

        // Transfert vers facture intermédiaire
        RowAction acompteAction = new RowAction(new AbstractAction("Créer une facture intermédiaire") {
            public void actionPerformed(ActionEvent e) {
                transfertAcompteClient(IListe.get(e).getSelectedRows());
            }
        }, false, "sales.order.create.account") {
            BigDecimal cent = BigDecimal.ONE.movePointRight(2);

            @Override
            public boolean enabledFor(List<SQLRowValues> selection) {
                if (selection.isEmpty() || selection.size() > 1) {
                    return false;
                } else {
                    BigDecimal d = getAvancement(selection.get(0));
                    return NumberUtils.compare(d, cent) != 0;
                }
            }
        };

        // Transfert vers facture solde
        RowAction soldeAction = new RowAction(new AbstractAction("Facturer le solde") {
            public void actionPerformed(ActionEvent e) {
                transfertSoldeClient(IListe.get(e).getSelectedRows());
            }
        }, false, "sales.order.create.account.solde") {
            BigDecimal cent = BigDecimal.ONE.movePointRight(2);

            @Override
            public boolean enabledFor(List<SQLRowValues> selection) {
                if (selection.isEmpty() || selection.size() > 1) {
                    return false;
                } else {
                    BigDecimal d = getAvancement(selection.get(0));
                    return NumberUtils.compare(d, cent) != 0 && NumberUtils.compare(d, BigDecimal.ZERO) != 0;
                }
            }
        };

        // Transfert vers commande
        PredicateRowAction cmdAction = new PredicateRowAction(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                final int selectedId = IListe.get(e).getSelectedId();
                ComptaPropsConfiguration.getInstanceCompta().getNonInteractiveSQLExecutor().execute(new Runnable() {

                    @Override
                    public void run() {
                        final CommandeClientSQLElement elt = (CommandeClientSQLElement) Configuration.getInstance().getDirectory().getElement("COMMANDE_CLIENT");
                        elt.transfertCommande(selectedId, true);

                    }
                });

            }

        }, false, "sales.order.create.supplier.order");

        cmdAction.setPredicate(IListeEvent.getSingleSelectionPredicate());

        bonAction.setPredicate(IListeEvent.getSingleSelectionPredicate());

        MouseSheetXmlListeListener mouseSheetXmlListeListener = new MouseSheetXmlListeListener(CommandeClientXmlSheet.class);
        mouseSheetXmlListeListener.setGenerateHeader(true);
        mouseSheetXmlListeListener.setShowHeader(true);

        allowedActions.add(bonAction);
        allowedActions.add(factureAction);
        allowedActions.add(acompteAction);
        allowedActions.add(soldeAction);
        allowedActions.add(cmdAction);
        allowedActions.addAll(mouseSheetXmlListeListener.getRowActions());
        getRowActions().addAll(allowedActions);
    }

    @Override
    protected void setupLinks(SQLElementLinksSetup links) {
        super.setupLinks(links);
        if (getTable().contains("ID_ADRESSE")) {
            links.get("ID_ADRESSE").setType(LinkType.ASSOCIATION);
        }
        if (getTable().contains("ID_ADRESSE_FACT")) {
            links.get("ID_ADRESSE_FACT").setType(LinkType.ASSOCIATION);
        }
        if (getTable().contains("ID_ADRESSE_LIVRAISON")) {
            links.get("ID_ADRESSE_LIVRAISON").setType(LinkType.ASSOCIATION);
        }
    }

    public SQLRow getNextCommandeToPrepare() {
        final SQLTable tableCmd = getTable();
        SQLSelect sel = new SQLSelect();
        sel.addSelect(tableCmd.getKey());
        sel.addSelect(tableCmd.getField("NUMERO"));
        sel.addSelect(tableCmd.getField("DATE"));
        sel.addSelect(tableCmd.getField("T_HT"));
        sel.addSelect(tableCmd.getField("T_TVA"));
        sel.addSelect(tableCmd.getField("T_TTC"));
        sel.addSelect(tableCmd.getField("PORT_HT"));
        sel.addSelect(tableCmd.getField("REMISE_HT"));
        sel.addSelect(tableCmd.getField("ID_TAXE_PORT"));
        sel.addSelect(tableCmd.getField("ID_CLIENT"));
        Where w = new Where(tableCmd.getField("ETAT_COMMANDE"), "=", EtatCommandeClient.A_PREPARER.getId());
        sel.setWhere(w);
        sel.clearOrder();
        sel.addFieldOrder(sel.getAlias(tableCmd.getField("DATE")));
        sel.addFieldOrder(sel.getAlias(tableCmd.getField("T_HT")));

        List<SQLRow> result = SQLRowListRSH.execute(sel);
        if (result == null || result.size() == 0) {
            return null;
        } else {
            return result.get(0);
        }
    }

    public int getNbCommandeAPreparer() {
        final SQLTable tableCmd = getTable();
        SQLSelect sel = new SQLSelect();
        sel.addSelect(tableCmd.getKey(), "COUNT");
        Where w = new Where(tableCmd.getField("ETAT_COMMANDE"), "=", EtatCommandeClient.A_PREPARER.getId());
        sel.setWhere(w);

        Object r = getTable().getDBSystemRoot().getDataSource().executeScalar(sel.asString());
        int nb = 0;
        if (r != null) {
            nb = ((Number) r).intValue();
        }
        return nb;
    }

    public void checkCommandeToShip() throws Exception {
        assert !SwingUtilities.isEventDispatchThread();

        SQLUtils.executeAtomic(getTable().getDBSystemRoot().getDataSource(), new ConnectionHandlerNoSetup<Object, IOException>() {
            @Override
            public Object handle(final SQLDataSource ds) throws SQLException, IOException {
                final SQLTable tableCmd = getTable();

                SQLRowValues rowVals = new SQLRowValues(tableCmd);
                rowVals.put(tableCmd.getKey().getName(), null);
                rowVals.put("NUMERO", null);

                final SQLTable tableCmdElt = tableCmd.getTable("COMMANDE_CLIENT_ELEMENT");
                SQLRowValues rowValsElt = new SQLRowValues(tableCmdElt);
                rowValsElt.put("QTE", null);
                rowValsElt.put("QTE_UNITAIRE", null);
                rowValsElt.put("ID_COMMANDE_CLIENT", rowVals);

                SQLRowValues rowValsArt = new SQLRowValues(tableCmd.getTable("ARTICLE"));
                rowValsArt.putRowValues("ID_STOCK").putNulls("QTE_REEL", "QTE_TH");
                rowValsElt.put("ID_ARTICLE", rowValsArt);

                SQLRowValuesListFetcher fetcher = SQLRowValuesListFetcher.create(rowVals);
                fetcher.setSelTransf(new ITransformer<SQLSelect, SQLSelect>() {

                    @Override
                    public SQLSelect transformChecked(SQLSelect input) {
                        Where w = new Where(input.getAlias(tableCmd).getField("ETAT_COMMANDE"), "=", EtatCommandeClient.A_PREPARER.getId());
                        w = w.or(new Where(input.getAlias(tableCmd).getField("ETAT_COMMANDE"), "=", EtatCommandeClient.RUPTURE.getId()));
                        input.setWhere(w);
                        // ORDER BY ETAT COMMANDE et T_HT pour mettre en priorité les ruptures de
                        // stock et les commandes les plus chers
                        input.clearOrder();
                        input.addFieldOrder(input.getAlias(tableCmd.getField("ETAT_COMMANDE")));
                        input.addFieldOrder(input.getAlias(tableCmd.getField("T_HT")));
                        System.err.println(input.asString());
                        return input;
                    }
                });

                List<SQLRowValues> result = fetcher.fetch();
                List<Integer> cmdStockOK = new ArrayList<Integer>();
                List<Integer> cmdNoStock = new ArrayList<Integer>();

                // Stock utilisé par les commandes à préparer
                StockCommande stockGlobalUsed = new StockCommande();
                for (int i = result.size() - 1; i >= 0; i--) {
                    SQLRowValues sqlRowValues = result.get(i);
                    boolean inStock = true;

                    // Stock utilisé par la commande actuelle
                    StockCommande stockCmd = new StockCommande();
                    for (SQLRowValues item : sqlRowValues.getReferentRows(tableCmdElt)) {
                        final int foreignID = item.getForeignID("ID_ARTICLE");

                        // Stock = stock actuel dans la base - stock utilisé par les commandes
                        // déja testées -
                        // stock utilisé par la commande en cours (si 2 fois le meme article)
                        BigDecimal stock = BigDecimal.ZERO;
                        if (!item.getForeign("ID_ARTICLE").isForeignEmpty("ID_STOCK")) {
                            stock = new BigDecimal(item.getForeign("ID_ARTICLE").getForeign("ID_STOCK").getFloat("QTE_REEL"));
                        }
                        stock = stock.subtract(stockCmd.getQty(foreignID)).subtract(stockGlobalUsed.getQty(foreignID));

                        BigDecimal needQty = item.getBigDecimal("QTE_UNITAIRE").multiply(new BigDecimal(item.getInt("QTE")), DecimalUtils.HIGH_PRECISION);

                        stockCmd.addQty(foreignID, needQty);

                        inStock = CompareUtils.compare(stock, needQty) >= 0;

                        if (!inStock) {
                            break;
                        }
                    }

                    if (inStock) {
                        Map<Integer, BigDecimal> m = stockCmd.getMap();
                        for (Integer id : m.keySet()) {
                            stockGlobalUsed.addQty(id, m.get(id));
                        }

                        cmdStockOK.add(sqlRowValues.getID());
                    } else {
                        cmdNoStock.add(sqlRowValues.getID());
                    }
                }

                List<String> reqs = new ArrayList<String>(2);

                if (cmdStockOK.size() > 0) {
                    UpdateBuilder builderStockOK = new UpdateBuilder(tableCmd);
                    builderStockOK.setObject("ETAT_COMMANDE", EtatCommandeClient.A_PREPARER.getId());
                    builderStockOK.setWhere(new Where(getTable().getKey(), cmdStockOK));
                    reqs.add(builderStockOK.asString());
                }

                if (cmdNoStock.size() > 0) {
                    UpdateBuilder builderNoStock = new UpdateBuilder(tableCmd);
                    builderNoStock.setObject("ETAT_COMMANDE", EtatCommandeClient.RUPTURE.getId());
                    builderNoStock.setWhere(new Where(getTable().getKey(), cmdNoStock));
                    reqs.add(builderNoStock.asString());
                }

                if (reqs.size() > 0) {
                    List<? extends ResultSetHandler> handlers = new ArrayList<ResultSetHandler>(reqs.size());
                    for (String s : reqs) {
                        handlers.add(null);
                    }
                    SQLUtils.executeMultiple(tableCmd.getDBSystemRoot(), reqs, handlers);
                    tableCmd.fireTableModified(-1);
                }
                return null;
            }
        });

    }

    private void changeStateOfRows(List<SQLRowValues> l, EtatCommandeClient etat) {

        List<Integer> ids = new ArrayList<Integer>(l.size());
        for (SQLRowValues sqlRowValues : l) {
            ids.add(sqlRowValues.getID());
        }

        UpdateBuilder builder = new UpdateBuilder(getTable());
        builder.setObject("ETAT_COMMANDE", etat.getId());
        builder.setWhere(new Where(getTable().getKey(), ids));

        getTable().getDBSystemRoot().getDataSource().execute(builder.asString());
        getTable().fireTableModified(-1);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openconcerto.devis.BaseSQLElement#getComboFields()
     */
    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NUMERO");
        return l;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openconcerto.devis.BaseSQLElement#getListFields()
     */
    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NUMERO");
        l.add("DATE");
        if (getTable().contains("DATE_LIVRAISON_PREV")) {
            l.add("DATE_LIVRAISON_PREV");
        }
        l.add("ID_CLIENT");
        l.add("ID_COMMERCIAL");
        l.add("T_HT");
        l.add("T_TTC");
        l.add("NOM");
        l.add("INFOS");
        SQLPreferences prefs = SQLPreferences.getMemCached(getTable().getDBRoot());
        if (prefs.getBoolean(GestionCommercialeGlobalPreferencePanel.ORDER_PACKAGING_MANAGEMENT, true)) {
            l.add("NUMERO_EXPEDITION");
            l.add("ETAT_COMMANDE");
        }
        return l;
    }

    @Override
    public Set<String> getReadOnlyFields() {
        final Set<String> s = new HashSet<String>();
        s.add("ID_DEVIS");
        return s;
    }

    @Override
    protected void archive(TreesOfSQLRows trees, boolean cutLinks) throws SQLException {

        for (SQLRow row : trees.getRows()) {

            // Mise à jour des stocks
            SQLElement eltMvtStock = Configuration.getInstance().getDirectory().getElement("MOUVEMENT_STOCK");
            SQLSelect sel = new SQLSelect();
            sel.addSelect(eltMvtStock.getTable().getField("ID"));
            Where w = new Where(eltMvtStock.getTable().getField("IDSOURCE"), "=", row.getID());
            Where w2 = new Where(eltMvtStock.getTable().getField("SOURCE"), "=", getTable().getName());
            sel.setWhere(w.and(w2));

            @SuppressWarnings("rawtypes")
            List l = (List) eltMvtStock.getTable().getBase().getDataSource().execute(sel.asString(), new ArrayListHandler());
            if (l != null) {
                for (int i = 0; i < l.size(); i++) {
                    Object[] tmp = (Object[]) l.get(i);
                    eltMvtStock.archive(((Number) tmp[0]).intValue());
                }
            }
        }
        super.archive(trees, cutLinks);
    }

    @Override
    protected synchronized void _initTableSource(final SQLTableModelSource source) {
        super._initTableSource(source);
        // TODO: refaire un renderer pour les commandes transférées en BL
        // final CommandeClientRenderer rend = CommandeClientRenderer.getInstance();
        // final SQLTableModelColumn col = source.getColumn(getTable().getField("T_HT"));
        // col.setColumnInstaller(new IClosure<TableColumn>() {
        // @Override
        // public void executeChecked(TableColumn input) {
        // input.setCellRenderer(rend);
        // }
        // });
        source.init();
        SQLPreferences prefs = SQLPreferences.getMemCached(getTable().getDBRoot());
        if (prefs.getBoolean(GestionCommercialeGlobalPreferencePanel.ORDER_PACKAGING_MANAGEMENT, true)) {

            SQLTableModelColumn col = source.getColumn(getTable().getField("ETAT_COMMANDE"));
            if (col != null) {
                col.setRenderer(new DefaultTableCellRenderer() {

                    @Override
                    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

                        JLabel comp = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                        if (value != null) {
                            final EtatCommandeClient fromID = EtatCommandeClient.fromID((Integer) value);
                            if (fromID != null) {
                                comp.setText(fromID.getTranslation());
                            } else {
                                comp.setText("");
                            }
                        }
                        return comp;
                    }
                });

            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openconcerto.devis.SQLElement#getComponent()
     */
    public SQLComponent createComponent() {
        return new CommandeClientSQLComponent();
    }

    /**
     * Transfert d'une commande en commande fournisseur
     * 
     * @param commandeID
     */
    public void transfertCommande(int commandeID, boolean useCommandeEnCours) {

        SQLElement elt = Configuration.getInstance().getDirectory().getElement("COMMANDE_CLIENT_ELEMENT");
        SQLTable tableCmdElt = Configuration.getInstance().getDirectory().getElement("COMMANDE_ELEMENT").getTable();
        SQLElement eltArticle = Configuration.getInstance().getDirectory().getElement("ARTICLE");
        SQLRow rowCmd = getTable().getRow(commandeID);
        List<SQLRow> rows = rowCmd.getReferentRows(elt.getTable());
        final ListMap<SQLRow, SQLRowValues> map = new ListMap<SQLRow, SQLRowValues>();
        for (SQLRow sqlRow : rows) {
            // on récupére l'article qui lui correspond
            SQLRowValues rowArticle = new SQLRowValues(eltArticle.getTable());
            for (SQLField field : eltArticle.getTable().getFields()) {
                if (sqlRow.getTable().getFieldsName().contains(field.getName())) {
                    rowArticle.put(field.getName(), sqlRow.getObject(field.getName()));
                }
            }

            int idArticle = ReferenceArticleSQLElement.getIdForCNM(rowArticle, true);
            SQLRow rowArticleFind = eltArticle.getTable().getRow(idArticle);
            if (rowArticleFind != null && !rowArticleFind.isUndefined()) {
                SQLInjector inj = SQLInjector.getInjector(rowArticle.getTable(), tableCmdElt);
                SQLRowValues rowValsElt = new SQLRowValues(inj.createRowValuesFrom(rowArticleFind));
                rowValsElt.put("ID_STYLE", sqlRow.getObject("ID_STYLE"));
                rowValsElt.put("QTE", sqlRow.getObject("QTE"));
                rowValsElt.put("T_POIDS", rowValsElt.getLong("POIDS") * rowValsElt.getInt("QTE"));
                rowValsElt.put("T_PA_HT", ((BigDecimal) rowValsElt.getObject("PA_HT")).multiply(new BigDecimal(rowValsElt.getInt("QTE")), DecimalUtils.HIGH_PRECISION));
                rowValsElt.put("T_PA_TTC",
                        ((BigDecimal) rowValsElt.getObject("T_PA_HT")).multiply(new BigDecimal((rowValsElt.getForeign("ID_TAXE").getFloat("TAUX") / 100.0 + 1.0)), DecimalUtils.HIGH_PRECISION));
                rowValsElt.put("ID_DEVISE", rowCmd.getForeignRow("ID_TARIF").getForeignID("ID_DEVISE"));
                map.add(rowArticleFind.getForeignRow("ID_FOURNISSEUR"), rowValsElt);
            }

        }
        MouvementStockSQLElement.createCommandeF(map, rowCmd.getForeignRow("ID_TARIF").getForeignRow("ID_DEVISE"), rowCmd.getString("NUMERO") + " - " + rowCmd.getString("NOM"), useCommandeEnCours);
    }

    /**
     * Transfert en BL
     * 
     * @param row
     */
    public void transfertBonLivraisonClient(final List<SQLRowValues> rows) {
        BonDeLivraisonSQLComponent comp = (BonDeLivraisonSQLComponent) TransfertBaseSQLComponent.openTransfertFrame(rows, "BON_DE_LIVRAISON").getSQLComponent();
        final SQLTable tableElt = comp.getElement().getTable().getTable("BON_DE_LIVRAISON_ELEMENT");
        SQLRowValues rowVals = new SQLRowValues(tableElt);
        rowVals.put("QTE_UNITAIRE", null);
        rowVals.put("QTE", null);
        rowVals.put("QTE_LIVREE", null);
        rowVals.put("ID_ARTICLE", null);

        SQLRowValuesListFetcher fetcher = SQLRowValuesListFetcher.create(rowVals);
        fetcher.setSelTransf(new ITransformer<SQLSelect, SQLSelect>() {

            @Override
            public SQLSelect transformChecked(SQLSelect input) {
                List<Integer> ids = new ArrayList<Integer>(rows.size());
                for (SQLRowValues sqlRowValues : rows) {
                    ids.add(sqlRowValues.getID());
                }
                SQLSelectJoin joinBR = input.addJoin("RIGHT", tableElt.getTable("BON_DE_LIVRAISON_ELEMENT").getField("ID_BON_DE_LIVRAISON"));
                SQLSelectJoin joinTR = input.addBackwardJoin("RIGHT", tableElt.getTable("TR_COMMANDE_CLIENT").getField("ID_BON_DE_LIVRAISON"), joinBR.getJoinedTable().getAlias());
                joinTR.setWhere(new Where(joinTR.getJoinedTable().getField("ID_COMMANDE_CLIENT"), ids));
                System.err.println(input.asString());
                return input;
            }
        });
        comp.loadQuantity(fetcher.fetch());

    }

    /**
     * Transfert en Facture
     * 
     * @param row
     */
    public void transfertFactureClient(List<SQLRowValues> rows) {
        TransfertBaseSQLComponent.openTransfertFrame(rows, "SAISIE_VENTE_FACTURE");

    }

    /**
     * Transfert en Facture
     * 
     * @param row
     */
    public void transfertAcompteClient(List<SQLRowValues> rows) {
        TransfertGroupSQLComponent.openTransfertFrame(rows, "SAISIE_VENTE_FACTURE", VenteFactureSituationSQLComponent.ID);
    }

    /**
     * Transfert en Facture
     * 
     * @param row
     */
    public void transfertSoldeClient(List<SQLRowValues> rows) {
        TransfertGroupSQLComponent.openTransfertFrame(rows, "SAISIE_VENTE_FACTURE", VenteFactureSoldeSQLComponent.ID);
    }

    private BigDecimal getAvancement(SQLRowAccessor r) {
        Collection<? extends SQLRowAccessor> rows = r.getReferentRows(r.getTable().getTable("TR_COMMANDE_CLIENT"));
        long totalFact = 0;
        long total = r.getLong("T_HT");
        for (SQLRowAccessor row : rows) {
            if (!row.isForeignEmpty("ID_SAISIE_VENTE_FACTURE")) {
                SQLRowAccessor rowFact = row.getForeign("ID_SAISIE_VENTE_FACTURE");
                Long l = rowFact.getLong("T_HT");
                totalFact += l;
            }
        }
        if (total > 0) {
            return new BigDecimal(totalFact).divide(new BigDecimal(total), DecimalUtils.HIGH_PRECISION).movePointRight(2).setScale(2, RoundingMode.HALF_UP);
        } else {
            return BigDecimal.ONE.movePointRight(2);
        }
    }
}
