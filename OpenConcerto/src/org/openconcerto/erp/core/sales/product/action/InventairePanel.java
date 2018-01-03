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

import org.openconcerto.erp.core.common.ui.NumericTextField;
import org.openconcerto.erp.core.supplychain.stock.element.ComposedItemStockUpdater;
import org.openconcerto.erp.core.supplychain.stock.element.StockItem;
import org.openconcerto.erp.core.supplychain.stock.element.StockItem.TypeStockMouvement;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.utils.SQLUtils;
import org.openconcerto.sql.view.list.IListe;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.JDate;
import org.openconcerto.ui.JLabelBold;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.NumberUtils;
import org.openconcerto.utils.text.SimpleDocumentListener;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;

import org.apache.commons.dbutils.ResultSetHandler;

public class InventairePanel extends JPanel {

    private final String mvtStockTableQuoted;
    private static String defaultLabel = "Mise à jour des stocks";

    public InventairePanel(final IListe liste, final List<? extends SQLRowAccessor> articles) {
        super(new GridBagLayout());
        final SQLTable mvtStockTable = Configuration.getInstance().getRoot().findTable("MOUVEMENT_STOCK");
        this.mvtStockTableQuoted = mvtStockTable.getSQLName().quote();

        JLabel qteReel = new JLabel("Quantité réel", SwingConstants.RIGHT);
        final NumericTextField fieldReel = new NumericTextField();
        JLabel qteLivAtt = new JLabel("Quantité Livraison en attente", SwingConstants.RIGHT);
        final NumericTextField fieldLivAtt = new NumericTextField();
        JLabel qteRecpAtt = new JLabel("Quantité Réception en attente", SwingConstants.RIGHT);
        final NumericTextField fieldRecpAtt = new NumericTextField();
        JLabel qteTh = new JLabel("Quantité théorique", SwingConstants.RIGHT);
        final NumericTextField fieldTh = new NumericTextField();

        GridBagConstraints c = new DefaultGridBagConstraints();

        this.add(new JLabel("Intitulé"), c);
        final JTextField label = new JTextField();
        c.gridx++;
        c.gridwidth = 2;
        c.weightx = 1;
        this.add(label, c);
        label.setText(defaultLabel);

        c.gridy++;
        c.gridx = 0;
        c.gridwidth = 1;
        c.weightx = 0;
        this.add(new JLabel("Date", SwingConstants.RIGHT), c);
        final JDate date = new JDate(true);
        c.gridx++;
        c.weightx = 0;
        this.add(date, c);
        c.gridy++;
        c.gridx = 0;
        c.gridwidth = 3;
        this.add(new JLabelBold("Stock réel"), c);

        c.gridy++;
        c.gridx = 0;
        c.gridwidth = 2;
        c.weightx = 0;
        this.add(qteReel, c);
        c.gridx += 2;
        c.gridwidth = 1;
        c.weightx = 1;
        this.add(fieldReel, c);

        c.gridy++;
        c.gridx = 0;
        c.gridwidth = 3;
        this.add(new JLabelBold("Stock virtuel"), c);

        c.gridy++;
        c.gridx = 0;
        c.gridwidth = 2;
        c.weightx = 0;
        this.add(qteLivAtt, c);
        c.gridx += 2;
        c.gridwidth = 1;
        c.weightx = 1;
        this.add(fieldLivAtt, c);

        c.gridy++;
        c.gridx = 0;
        c.gridwidth = 2;
        c.weightx = 0;
        this.add(qteRecpAtt, c);
        c.gridx += 2;
        c.gridwidth = 1;
        c.weightx = 1;
        this.add(fieldRecpAtt, c);

        c.gridy++;
        c.gridx = 0;
        c.gridwidth = 2;
        c.weightx = 0;
        this.add(qteTh, c);
        c.gridx += 2;
        c.gridwidth = 1;
        fieldTh.setEditable(false);
        c.weightx = 1;
        this.add(fieldTh, c);
        SimpleDocumentListener l = new SimpleDocumentListener() {

            @Override
            public void update(DocumentEvent e) {
                BigDecimal qteReel = (fieldReel.getValue() == null ? BigDecimal.ZERO : fieldReel.getValue());
                BigDecimal qteLivAttente = (fieldLivAtt.getValue() == null ? BigDecimal.ZERO : fieldLivAtt.getValue());
                BigDecimal qteRecptAttente = (fieldRecpAtt.getValue() == null ? BigDecimal.ZERO : fieldRecpAtt.getValue());

                fieldTh.setValue(qteReel.subtract(qteLivAttente).add(qteRecptAttente));
            }
        };
        fieldLivAtt.getDocument().addDocumentListener(l);
        fieldReel.getDocument().addDocumentListener(l);
        fieldRecpAtt.getDocument().addDocumentListener(l);

        if (articles.size() == 1) {
            SQLRowAccessor r = articles.get(0);
            if (!r.isForeignEmpty("ID_STOCK")) {
                SQLRowAccessor stock = r.getForeign("ID_STOCK");
                fieldReel.setText(String.valueOf(stock.getFloat("QTE_REEL")));
                fieldLivAtt.setText(String.valueOf(stock.getFloat("QTE_LIV_ATTENTE")));
                fieldRecpAtt.setText(String.valueOf(stock.getFloat("QTE_RECEPT_ATTENTE")));
                fieldTh.setText(String.valueOf(stock.getFloat("QTE_TH")));
            }
        }

        c.gridy++;
        c.gridx = 0;
        final JButton buttonUpdate = new JButton("Mettre à jour");
        JButton buttonCancel = new JButton("Annuler");
        JPanel pButton = new JPanel();
        pButton.add(buttonCancel);
        pButton.add(buttonUpdate);
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.EAST;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        this.add(pButton, c);
        buttonCancel.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                ((JFrame) SwingUtilities.getRoot(InventairePanel.this)).dispose();
            }
        });
        buttonUpdate.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {

                buttonUpdate.setEnabled(false);
                defaultLabel = label.getText();
                BigDecimal qteReel = fieldReel.getValue();
                BigDecimal qteLivAttente = fieldLivAtt.getValue();
                BigDecimal qteRecptAttente = fieldRecpAtt.getValue();

                List<List<String>> multipleRequests = new ArrayList<List<String>>();
                List<String> multipleRequestsHundred = new ArrayList<String>(100);
                boolean usePrice = mvtStockTable.contains("PRICE");

                final Date dateValue = date.getValue();
                for (SQLRowAccessor sqlRowAccessor : articles) {
                    if (multipleRequestsHundred.size() > 100) {
                        multipleRequests.add(multipleRequestsHundred);
                        multipleRequestsHundred = new ArrayList<String>(100);
                    }
                    StockItem item = new StockItem(sqlRowAccessor);
                    boolean modified = false;
                    if (qteReel != null && !NumberUtils.areNumericallyEqual(qteReel, item.getRealQty())) {
                        double diff = qteReel.doubleValue() - item.getRealQty();
                        item.updateQty(diff, TypeStockMouvement.REEL);
                        multipleRequestsHundred.add(getMvtRequest(dateValue, BigDecimal.ZERO, diff, item, label.getText(), true, usePrice));
                        modified = true;
                    }

                    if (qteLivAttente != null && !NumberUtils.areNumericallyEqual(qteLivAttente, item.getDeliverQty())) {
                        double diff = item.getDeliverQty() - qteLivAttente.doubleValue();
                        item.updateQty(diff, TypeStockMouvement.THEORIQUE, diff > 0);
                        multipleRequestsHundred.add(getMvtRequest(dateValue, BigDecimal.ZERO, diff, item, label.getText(), false, usePrice));
                        modified = true;
                    }

                    if (qteRecptAttente != null && !NumberUtils.areNumericallyEqual(qteRecptAttente, item.getReceiptQty())) {
                        double diff = qteRecptAttente.doubleValue() - item.getReceiptQty();
                        item.updateQty(Math.abs(diff), TypeStockMouvement.THEORIQUE, diff < 0);
                        multipleRequestsHundred.add(getMvtRequest(dateValue, BigDecimal.ZERO, diff, item, label.getText(), false, usePrice));
                        modified = true;
                    }
                    if (modified) {
                        if (item.isStockInit()) {
                            multipleRequestsHundred.add(item.getUpdateRequest());
                        } else {
                            SQLRowValues rowVals = new SQLRowValues(sqlRowAccessor.getTable().getForeignTable("ID_STOCK"));
                            rowVals.put("QTE_REEL", item.getRealQty());
                            rowVals.put("QTE_TH", item.getVirtualQty());
                            rowVals.put("QTE_LIV_ATTENTE", item.getDeliverQty());
                            rowVals.put("QTE_RECEPT_ATTENTE", item.getReceiptQty());
                            SQLRowValues rowValsArt = item.getArticle().createEmptyUpdateRow();
                            rowValsArt.put("ID_STOCK", rowVals);
                            try {
                                rowValsArt.commit();
                            } catch (SQLException e1) {
                                e1.printStackTrace();
                            }
                        }
                    }

                }
                multipleRequests.add(multipleRequestsHundred);
                try {
                    for (List<String> requests : multipleRequests) {

                        final int size = requests.size();
                        List<? extends ResultSetHandler> handlers = new ArrayList<ResultSetHandler>(size);
                        for (int i = 0; i < size; i++) {
                            handlers.add(null);
                        }
                        SQLUtils.executeMultiple(Configuration.getInstance().getRoot().getDBSystemRoot(), requests, handlers);

                    }
                } catch (SQLException e1) {
                    ExceptionHandler.handle("Stock update error", e1);
                }

                final DBRoot root = mvtStockTable.getDBRoot();
                if (root.contains("ARTICLE_ELEMENT")) {
                    List<StockItem> stockItems = new ArrayList<StockItem>();
                    for (SQLRowAccessor sqlRowAccessor2 : articles) {
                        final SQLRow asRow = sqlRowAccessor2.asRow();
                        asRow.fetchValues();
                        stockItems.add(new StockItem(asRow));
                    }
                    // Mise à jour des stocks des nomenclatures
                    ComposedItemStockUpdater comp = new ComposedItemStockUpdater(root, stockItems);
                    try {
                        comp.update();
                    } catch (SQLException e1) {
                        e1.printStackTrace();
                    }
                }

                liste.getModel().updateAll();
                ((JFrame) SwingUtilities.getRoot(InventairePanel.this)).dispose();
            }
        });
    }

    private String getMvtRequest(Date time, BigDecimal prc, double qteFinal, StockItem item, String label, boolean reel, boolean usePrice) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String mvtStockQuery = "INSERT INTO " + mvtStockTableQuoted + " (\"QTE\",\"DATE\",\"ID_ARTICLE\",\"NOM\",\"REEL\",\"ORDRE\"";

        if (usePrice && prc != null) {
            mvtStockQuery += ",\"PRICE\"";
        }

        mvtStockQuery += ") VALUES(" + qteFinal + ",'" + dateFormat.format(time) + "'," + item.getArticle().getID() + ",'" + label + "'," + reel + ", (SELECT (MAX(\"ORDRE\")+1) FROM "
                + mvtStockTableQuoted + ")";
        if (usePrice && prc != null) {
            mvtStockQuery += "," + prc.setScale(6, RoundingMode.HALF_UP).toString();
        }
        mvtStockQuery += ")";
        return mvtStockQuery;
    }
}
