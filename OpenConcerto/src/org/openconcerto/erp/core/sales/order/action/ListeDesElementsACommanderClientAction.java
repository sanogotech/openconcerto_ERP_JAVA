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
 
 package org.openconcerto.erp.core.sales.order.action;

import org.openconcerto.erp.action.CreateFrameAbstractAction;
import org.openconcerto.erp.core.common.ui.IListFilterDatePanel;
import org.openconcerto.erp.core.common.ui.ListeViewPanel;
import org.openconcerto.erp.core.sales.order.element.CommandeClientElementSQLElement;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.FieldPath;
import org.openconcerto.sql.model.FieldRef;
import org.openconcerto.sql.model.SQLName;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLSelectJoin;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.model.graph.Path;
import org.openconcerto.sql.view.IListFrame;
import org.openconcerto.sql.view.ListeAddPanel;
import org.openconcerto.sql.view.list.BaseSQLTableModelColumn;
import org.openconcerto.sql.view.list.IListe;
import org.openconcerto.sql.view.list.IListeAction.IListeEvent;
import org.openconcerto.sql.view.list.RowAction.PredicateRowAction;
import org.openconcerto.sql.view.list.SQLTableModelSourceOnline;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.FrameUtil;
import org.openconcerto.ui.state.WindowStateManager;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.DecimalUtils;
import org.openconcerto.utils.cc.ITransformer;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class ListeDesElementsACommanderClientAction extends CreateFrameAbstractAction {
    final CommandeClientElementSQLElement eltCmd = (CommandeClientElementSQLElement) Configuration.getInstance().getDirectory().getElement("COMMANDE_CLIENT_ELEMENT");

    public ListeDesElementsACommanderClientAction() {
        super();
        this.putValue(Action.NAME, "Liste des éléments en attente de livraison");
    }

    private BaseSQLTableModelColumn colAvancement;

    public JFrame createFrame() {
        final JFrame frame = new JFrame("Eléments en attente de livraison");
        // Actions

        final JPanel orderPanel = createPanel();

        frame.getContentPane().add(orderPanel);
        FrameUtil.setBounds(frame);
        final File file = IListFrame.getConfigFile(eltCmd, frame.getClass());
        if (file != null)
            new WindowStateManager(frame, file).loadState();
        return frame;
    }

    JPanel createPanel() {
        final SQLTableModelSourceOnline tableSource = eltCmd.getTableSource(true);
        tableSource.getReq().setSelectTransf(new ITransformer<SQLSelect, SQLSelect>() {

            @Override
            public SQLSelect transformChecked(SQLSelect input) {
                // new SQLName(eltCmd.getTable().getDBRoot().getName(),
                // tableBLElement.getName()).quote()
                final String quoteQteL = new SQLName(input.getAlias(eltCmd.getTable()).getAlias(), "QTE_LIVREE").quote();
                final String quoteQte = new SQLName(input.getAlias(eltCmd.getTable()).getAlias(), "QTE").quote();
                final String quoteQteU = new SQLName(input.getAlias(eltCmd.getTable()).getAlias(), "QTE_UNITAIRE").quote();
                Where w = Where.createRaw(quoteQteL + " < (" + quoteQte + "*" + quoteQteU + ")", eltCmd.getTable().getField("QTE_LIVREE"), eltCmd.getTable().getField("QTE"),
                        eltCmd.getTable().getField("QTE_UNITAIRE"));
                input.setWhere(w);
                return input;
            }
        });

        BaseSQLTableModelColumn colStockR = new BaseSQLTableModelColumn("Stock Reel", Float.class) {

            @Override
            protected Object show_(SQLRowAccessor r) {

                final SQLRowAccessor foreign = r.getForeign("ID_ARTICLE");
                if (foreign != null && !foreign.isUndefined()) {
                    final SQLRowAccessor foreign2 = foreign.getForeign("ID_STOCK");
                    if (foreign2 != null && !foreign2.isUndefined()) {
                        return foreign2.getFloat("QTE_REEL");
                    }
                }
                return 0F;
            }

            @Override
            public Set<FieldPath> getPaths() {
                Path p = new Path(eltCmd.getTable());
                p = p.add(p.getLast().getField("ID_ARTICLE"));
                p = p.add(p.getLast().getField("ID_STOCK"));
                return CollectionUtils.createSet(new FieldPath(p, "QTE_REEL"));
            }
        };
        tableSource.getColumns().add(colStockR);

        BaseSQLTableModelColumn colLiv2 = new BaseSQLTableModelColumn("Stock TH", Float.class) {

            @Override
            protected Object show_(SQLRowAccessor r) {

                final SQLRowAccessor foreign = r.getForeign("ID_ARTICLE");
                if (foreign != null && !foreign.isUndefined()) {
                    final SQLRowAccessor foreign2 = foreign.getForeign("ID_STOCK");
                    if (foreign2 != null && !foreign2.isUndefined()) {
                        return foreign2.getFloat("QTE_TH");
                    }
                }
                return 0F;
            }

            @Override
            public Set<FieldPath> getPaths() {
                Path p = new Path(eltCmd.getTable());
                p = p.add(p.getLast().getField("ID_ARTICLE"));
                p = p.add(p.getLast().getField("ID_STOCK"));
                return CollectionUtils.createSet(new FieldPath(p, "QTE_TH"));
            }
        };
        tableSource.getColumns().add(colLiv2);

        BaseSQLTableModelColumn colStockMin = new BaseSQLTableModelColumn("Stock Min", Integer.class) {

            @Override
            protected Object show_(SQLRowAccessor r) {

                final SQLRowAccessor foreign = r.getForeign("ID_ARTICLE");
                return foreign.getInt("QTE_MIN");
            }

            @Override
            public Set<FieldPath> getPaths() {
                Path p = new Path(eltCmd.getTable());
                p = p.add(p.getLast().getField("ID_ARTICLE"));

                return CollectionUtils.createSet(new FieldPath(p, "QTE_MIN"));
            }
        };
        tableSource.getColumns().add(colStockMin);

        BaseSQLTableModelColumn colSug = new BaseSQLTableModelColumn("Qtè à commander", Float.class) {

            @Override
            protected Object show_(SQLRowAccessor r) {

                // final float qteCommande = r.getBigDecimal("QTE_UNITAIRE").multiply(new
                // BigDecimal(r.getInt("QTE"))).subtract(r.getBigDecimal("QTE_LIVREE")).floatValue();
                final SQLRowAccessor foreign = r.getForeign("ID_ARTICLE");
                if (foreign != null && !foreign.isUndefined()) {
                    float qteMin = foreign.getFloat("QTE_MIN");
                    final SQLRowAccessor foreign2 = foreign.getForeign("ID_STOCK");
                    if (foreign2 != null && !foreign2.isUndefined()) {
                        float manque = foreign2.getFloat("QTE_TH") - qteMin;
                        if (manque < 0) {
                            return -manque;
                        }
                    }
                }
                return 0F;
            }

            @Override
            public Set<FieldPath> getPaths() {
                Path pA = new Path(eltCmd.getTable());

                pA = pA.add(pA.getLast().getField("ID_ARTICLE"));
                Path p = pA.add(pA.getLast().getField("ID_STOCK"));
                return CollectionUtils.createSet(new FieldPath(pA, "QTE_MIN"), new FieldPath(p, "QTE_TH"));
            }
        };
        tableSource.getColumns().add(colSug);
        // colLiv2.setRenderer(new PercentTableCellRenderer());

        final ListeAddPanel panel = getPanel(eltCmd, tableSource);
        PredicateRowAction action = new PredicateRowAction(new AbstractAction("Calcul des besoins") {

            @Override
            public void actionPerformed(ActionEvent e) {
                final SQLElement artElt = eltCmd.getForeignElement("ID_ARTICLE");
                final SQLTableModelSourceOnline createTableSource = artElt.createTableSource();
                createTableSource.getReq().setSelectTransf(new ITransformer<SQLSelect, SQLSelect>() {
                    @Override
                    public SQLSelect transformChecked(SQLSelect input) {
                        FieldRef refStock = input.getAlias(artElt.getTable().getForeignTable("ID_STOCK").getField("QTE_TH"));

                        SQLSelectJoin j = input.getJoinFromField(artElt.getTable().getTable("ARTICLE_ELEMENT").getField("ID_ARTICLE_PARENT"));
                        Where w = new Where(refStock, "<", artElt.getTable().getField("QTE_MIN"));
                        w = w.and(new Where(j.getJoinedTable().getKey(), "=", (Object) null));
                        input.setWhere(w);
                        // input.setHaving(Where.createRaw("COUNT(\"" + j.getJoinedTable().getKey()
                        // + "\")" + " = 0", Arrays.asList(j.getJoinedTable().getKey())));
                        return input;
                    }
                });

                BaseSQLTableModelColumn colSug = new BaseSQLTableModelColumn("Qtè à commander", Float.class) {

                    @Override
                    protected Object show_(SQLRowAccessor r) {

                        float qteMin = r.getFloat("QTE_MIN");
                        final SQLRowAccessor foreign2 = r.getForeign("ID_STOCK");
                        if (foreign2 != null && !foreign2.isUndefined()) {
                            float manque = foreign2.getFloat("QTE_TH") - qteMin;
                            if (manque < 0) {
                                return -manque;
                            }
                        }

                        return 0F;
                    }

                    @Override
                    public Set<FieldPath> getPaths() {
                        Path pA = new Path(artElt.getTable());

                        Path p = pA.add(pA.getLast().getField("ID_STOCK"));
                        return CollectionUtils.createSet(new FieldPath(pA, "QTE_MIN"), new FieldPath(p, "QTE_TH"));
                    }
                };
                createTableSource.getColumns().add(colSug);

                IListe listeArt = new IListe(createTableSource);
                final PredicateRowAction predicateACtion = new PredicateRowAction(new AbstractAction("Passer une commande fournisseur") {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        List<SQLRowValues> selectedRows = IListe.get(e).getSelectedRows();
                        eltCmd.createCommandeF(selectedRows);
                    }
                }, true);
                predicateACtion.setPredicate(IListeEvent.getNonEmptySelectionPredicate());
                listeArt.addIListeAction(predicateACtion);
                ListeViewPanel p = new ListeViewPanel(artElt, listeArt);
                IListFrame f = new IListFrame(p);
                FrameUtil.show(f);
            }
        }, true);
        action.setPredicate(IListeEvent.createTotalRowCountPredicate(0, Integer.MAX_VALUE));
        panel.getListe().addIListeAction(action);
        return panel;
    }

    private BigDecimal getAvancementLFromBL(SQLRowAccessor r) {
        Collection<? extends SQLRowAccessor> rows = r.getReferentRows(r.getTable().getTable("COMMANDE_CLIENT_ELEMENT"));
        BigDecimal totalQte = BigDecimal.ZERO;
        BigDecimal totalQteL = BigDecimal.ZERO;
        for (SQLRowAccessor row : rows) {
            BigDecimal qte = row.getBigDecimal("QTE_UNITAIRE").multiply(new BigDecimal(row.getInt("QTE")));
            totalQte = totalQte.add(qte);
            if (row.getBoolean("LIVRE_FORCED") || row.getBoolean("LIVRE")) {
                totalQteL = totalQteL.add(qte);
            } else {
                totalQteL = totalQteL.add(row.getBigDecimal("QTE_LIVREE"));
            }
        }
        if (totalQte.signum() != 0) {
            return totalQteL.divide(totalQte, DecimalUtils.HIGH_PRECISION).movePointRight(2).setScale(2, RoundingMode.HALF_UP);
        } else {
            return BigDecimal.ONE.movePointRight(2);
        }
    }

    private ListeAddPanel getPanel(final SQLElement eltCmd, final SQLTableModelSourceOnline tableSource) {
        final ListeAddPanel panel = new ListeAddPanel(eltCmd, new IListe(tableSource));

        // final List<Tuple2<? extends SQLTableModelColumn, IListTotalPanel.Type>> fields = new
        // ArrayList<Tuple2<? extends SQLTableModelColumn, IListTotalPanel.Type>>(2);
        // fields.add(Tuple2.create(panel.getListe().getSource().getColumn(eltCmd.getTable().getField("T_HT")),
        // IListTotalPanel.Type.SOMME));
        // fields.add(Tuple2.create(this.colAvancement, IListTotalPanel.Type.AVANCEMENT_TTC));
        // final IListTotalPanel totalPanel = new IListTotalPanel(panel.getListe(), fields, null,
        // "Total des commandes de la liste");

        final GridBagConstraints c = new DefaultGridBagConstraints();
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.EAST;
        c.weightx = 1;
        c.gridy = 4;

        // Date panel
        final IListFilterDatePanel datePanel = new IListFilterDatePanel(panel.getListe(), eltCmd.getTable().getForeignTable("ID_COMMANDE_CLIENT").getField("DATE"),
                IListFilterDatePanel.getDefaultMap());

        datePanel.setFilterOnDefault();

        final JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new GridBagLayout());
        bottomPanel.setOpaque(false);
        final GridBagConstraints c2 = new DefaultGridBagConstraints();
        c2.fill = GridBagConstraints.NONE;
        c2.weightx = 1;
        bottomPanel.add(datePanel, c2);

        // c2.gridx++;
        // c2.weightx = 0;
        // c2.anchor = GridBagConstraints.EAST;
        // bottomPanel.add(totalPanel, c2);

        panel.add(bottomPanel, c);
        return panel;
    }

}
