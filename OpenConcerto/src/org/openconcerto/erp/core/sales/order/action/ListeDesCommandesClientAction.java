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
import org.openconcerto.erp.core.common.ui.IListTotalPanel;
import org.openconcerto.erp.core.sales.order.ui.EtatCommandeClient;
import org.openconcerto.erp.preferences.GestionCommercialeGlobalPreferencePanel;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.FieldPath;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.model.graph.Path;
import org.openconcerto.sql.model.graph.PathBuilder;
import org.openconcerto.sql.preferences.SQLPreferences;
import org.openconcerto.sql.view.IListFrame;
import org.openconcerto.sql.view.ListeAddPanel;
import org.openconcerto.sql.view.list.BaseSQLTableModelColumn;
import org.openconcerto.sql.view.list.IListe;
import org.openconcerto.sql.view.list.SQLTableModelColumn;
import org.openconcerto.sql.view.list.SQLTableModelSourceOnline;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.FrameUtil;
import org.openconcerto.ui.state.WindowStateManager;
import org.openconcerto.ui.table.PercentTableCellRenderer;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.DecimalUtils;
import org.openconcerto.utils.Tuple2;
import org.openconcerto.utils.cc.ITransformer;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

public class ListeDesCommandesClientAction extends CreateFrameAbstractAction {

    public ListeDesCommandesClientAction() {
        super();
        this.putValue(Action.NAME, "Liste des commandes clients");
    }

    private BaseSQLTableModelColumn colAvancement;

    public JFrame createFrame() {
        final JFrame frame = new JFrame("Commandes clients");
        // Actions
        final SQLElement eltCmd = Configuration.getInstance().getDirectory().getElement("COMMANDE_CLIENT");
        SQLPreferences prefs = SQLPreferences.getMemCached(eltCmd.getTable().getDBRoot());
        boolean cmdState = prefs.getBoolean(GestionCommercialeGlobalPreferencePanel.ORDER_PACKAGING_MANAGEMENT, true);

        if (cmdState && eltCmd.getTable().contains("ETAT_COMMANDE")) {
            JTabbedPane pane = new JTabbedPane();
            for (EtatCommandeClient etat : EtatCommandeClient.values()) {
                final JPanel orderPanel = createAllOrderPanel(etat);
                pane.add(etat.getTranslation(), orderPanel);
            }
            frame.getContentPane().add(pane);
        } else {
            final JPanel orderPanel = createAllOrderPanel(null);

            frame.getContentPane().add(orderPanel);
        }
        FrameUtil.setBounds(frame);
        final File file = IListFrame.getConfigFile(eltCmd, frame.getClass());
        if (file != null)
            new WindowStateManager(frame, file).loadState();
        return frame;
    }

    JPanel createAllOrderPanel(final EtatCommandeClient etat) {
        final SQLElement eltCmd = Configuration.getInstance().getDirectory().getElement("COMMANDE_CLIENT");
        final SQLTableModelSourceOnline tableSource = eltCmd.getTableSource(true);
        if (etat != null) {
            tableSource.getReq().setSelectTransf(new ITransformer<SQLSelect, SQLSelect>() {
                public SQLSelect transformChecked(SQLSelect input) {
                    input.setWhere(new Where(eltCmd.getTable().getField("ETAT_COMMANDE"), "=", etat.getId()));
                    return input;
                }
            });
        }

        BaseSQLTableModelColumn colLiv2 = new BaseSQLTableModelColumn("Avancement livraison v2", BigDecimal.class) {

            @Override
            protected Object show_(SQLRowAccessor r) {

                return getAvancementLFromBL(r);
            }

            @Override
            public Set<FieldPath> getPaths() {
                final Path p = new PathBuilder(eltCmd.getTable()).addTable("COMMANDE_CLIENT_ELEMENT").build();
                return CollectionUtils.createSet(new FieldPath(p, "QTE_LIVREE"), new FieldPath(p, "QTE"), new FieldPath(p, "QTE_UNITAIRE"), new FieldPath(p, "LIVRE_FORCED"),
                        new FieldPath(p, "LIVRE"));
            }
        };
        tableSource.getColumns().add(colLiv2);
        colLiv2.setRenderer(new PercentTableCellRenderer());

        if (eltCmd.getTable().getDBRoot().contains("TARIF_AGENCE")) {
            this.colAvancement = colLiv2;
        } else {

            this.colAvancement = new BaseSQLTableModelColumn("Avancement facturation", BigDecimal.class) {

                @Override
                protected Object show_(SQLRowAccessor r) {

                    return getAvancement(r);
                }

                @Override
                public Set<FieldPath> getPaths() {
                    final Path p = new PathBuilder(eltCmd.getTable()).addTable("TR_COMMANDE_CLIENT").addTable("SAISIE_VENTE_FACTURE").build();
                    return CollectionUtils.createSet(new FieldPath(p, "T_HT"));
                }
            };
        }

        tableSource.getColumns().add(this.colAvancement);
        this.colAvancement.setRenderer(new PercentTableCellRenderer());
        final ListeAddPanel panel = getPanel(eltCmd, tableSource);
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

    private BigDecimal getAvancementL(SQLRowAccessor r) {
        Collection<? extends SQLRowAccessor> rows = r.getReferentRows(r.getTable().getTable("TR_COMMANDE_CLIENT"));
        long totalFact = 0;
        long total = r.getLong("T_HT");
        for (SQLRowAccessor row : rows) {
            if (!row.isForeignEmpty("ID_BON_DE_LIVRAISON")) {
                SQLRowAccessor rowFact = row.getForeign("ID_BON_DE_LIVRAISON");
                Long l = rowFact.getLong("TOTAL_HT");
                totalFact += l;
            }
        }
        if (total > 0) {
            return new BigDecimal(totalFact).divide(new BigDecimal(total), DecimalUtils.HIGH_PRECISION).movePointRight(2).setScale(2, RoundingMode.HALF_UP);
        } else {
            return BigDecimal.ONE.movePointRight(2);
        }
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

    private ListeAddPanel getPanel(final SQLElement eltCmd, final SQLTableModelSourceOnline tableSource) {
        final ListeAddPanel panel = new ListeAddPanel(eltCmd, new IListe(tableSource)) {
            @Override
            protected void createUI() {
                super.createUI();
                this.btnMngr.setAdditional(this.buttonEffacer, new ITransformer<JButton, String>() {

                    @Override
                    public String transformChecked(JButton input) {

                        SQLRowAccessor row = getListe().fetchSelectedRow();

                        BigDecimal b = getAvancement(row);

                        if (row.getLong("T_HT") > 0 && b.signum() != 0) {
                            return "Vous ne pouvez pas supprimer une commande facturée !";
                        }
                        return null;
                    }
                });
                this.btnMngr.setAdditional(this.buttonModifier, new ITransformer<JButton, String>() {

                    @Override
                    public String transformChecked(JButton input) {

                        SQLRowAccessor row = getListe().fetchSelectedRow();

                        BigDecimal b = getAvancement(row);

                        if (row.getLong("T_HT") > 0 && b.signum() != 0) {
                            return "Vous ne pouvez pas modifier une commande facturée !";
                        }
                        return null;
                    }
                });
            }
        };

        final List<Tuple2<? extends SQLTableModelColumn, IListTotalPanel.Type>> fields = new ArrayList<Tuple2<? extends SQLTableModelColumn, IListTotalPanel.Type>>(2);
        // if (panel.getListe().getSource().getColumn(eltCmd.getTable().getField("T_HA")) != null) {
        // fields.add(Tuple2.create(panel.getListe().getSource().getColumn(eltCmd.getTable().getField("T_HA")),
        // IListTotalPanel.Type.SOMME));
        // }
        fields.add(Tuple2.create(panel.getListe().getSource().getColumn(eltCmd.getTable().getField("T_HT")), IListTotalPanel.Type.SOMME));
        fields.add(Tuple2.create(this.colAvancement, IListTotalPanel.Type.AVANCEMENT_TTC));
        final IListTotalPanel totalPanel = new IListTotalPanel(panel.getListe(), fields, null, "Total des commandes de la liste");

        final GridBagConstraints c = new DefaultGridBagConstraints();
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.EAST;
        c.weightx = 1;
        c.gridy = 4;

        // Date panel
        final IListFilterDatePanel datePanel = new IListFilterDatePanel(panel.getListe(), eltCmd.getTable().getField("DATE"), IListFilterDatePanel.getDefaultMap());

        datePanel.setFilterOnDefault();

        final JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new GridBagLayout());
        bottomPanel.setOpaque(false);
        final GridBagConstraints c2 = new DefaultGridBagConstraints();
        c2.fill = GridBagConstraints.NONE;
        c2.weightx = 1;
        bottomPanel.add(datePanel, c2);

        c2.gridx++;
        c2.weightx = 0;
        c2.anchor = GridBagConstraints.EAST;
        bottomPanel.add(totalPanel, c2);

        panel.add(bottomPanel, c);
        return panel;
    }

}
