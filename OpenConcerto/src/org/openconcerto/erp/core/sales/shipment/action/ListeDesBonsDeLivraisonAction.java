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
 
 package org.openconcerto.erp.core.sales.shipment.action;

import org.openconcerto.erp.action.CreateFrameAbstractAction;
import org.openconcerto.erp.core.common.component.TransfertBaseSQLComponent;
import org.openconcerto.erp.core.common.ui.IListFilterDatePanel;
import org.openconcerto.erp.core.common.ui.IListTotalPanel;
import org.openconcerto.erp.core.sales.shipment.report.BonLivraisonXmlSheet;
import org.openconcerto.erp.model.MouseSheetXmlListeListener;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.FieldPath;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLInjector;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.graph.Path;
import org.openconcerto.sql.model.graph.PathBuilder;
import org.openconcerto.sql.view.IListFrame;
import org.openconcerto.sql.view.ListeAddPanel;
import org.openconcerto.sql.view.list.BaseSQLTableModelColumn;
import org.openconcerto.sql.view.list.IListe;
import org.openconcerto.sql.view.list.IListeAction.IListeEvent;
import org.openconcerto.sql.view.list.RowAction;
import org.openconcerto.sql.view.list.RowAction.PredicateRowAction;
import org.openconcerto.sql.view.list.SQLTableModelSourceOnline;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.FrameUtil;
import org.openconcerto.ui.state.WindowStateManager;
import org.openconcerto.ui.table.PercentTableCellRenderer;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.DecimalUtils;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

public class ListeDesBonsDeLivraisonAction extends CreateFrameAbstractAction {

    public ListeDesBonsDeLivraisonAction() {
        super();
        this.putValue(Action.NAME, "Liste des bons de livraison");
    }

    public JFrame createFrame() {
        final JFrame frame = new JFrame("Bons de livraison");
        PredicateRowAction toInvoiceAction = new PredicateRowAction(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                transfertFactureClient(IListe.get(e).getSelectedRows());
            }
        }, false, "sales.shipment.create.invoice");
        toInvoiceAction.setPredicate(IListeEvent.getNonEmptySelectionPredicate());

        // Tabs
        final JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Toutes les livraisons", createAllDeliveryPanel(toInvoiceAction));
        tabs.addTab("Livraisons non facturées", createDeliveryWithoutInvoicePanel(toInvoiceAction));
        tabs.addTab("Livraisons facturées", createDeliveryWithInvoicePanel());
        frame.setContentPane(tabs);

        final SQLElement eltCmd = Configuration.getInstance().getDirectory().getElement("BON_DE_LIVRAISON");
        FrameUtil.setBounds(frame);
        final File file = IListFrame.getConfigFile(eltCmd, frame.getClass());
        if (file != null)
            new WindowStateManager(frame, file).loadState();
        return frame;
    }

    private ListeAddPanel getPanel(final SQLElement eltCmd, final SQLTableModelSourceOnline tableSource, final List<RowAction> allowedActions) {
        final ListeAddPanel panel = new ListeAddPanel(eltCmd, new IListe(tableSource));

        final List<SQLField> fields = new ArrayList<SQLField>(2);
        fields.add(eltCmd.getTable().getField("TOTAL_HT"));
        final IListTotalPanel totalPanel = new IListTotalPanel(panel.getListe(), fields, "Total des livraisons de la liste");

        final GridBagConstraints c = new DefaultGridBagConstraints();
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.EAST;
        c.weightx = 1;
        c.gridy = 4;

        // Date panel
        final IListFilterDatePanel datePanel = new IListFilterDatePanel(panel.getListe(), eltCmd.getTable().getField("DATE"), IListFilterDatePanel.getDefaultMap());

        panel.getListe().addIListeActions(new MouseSheetXmlListeListener(BonLivraisonXmlSheet.class) {
            @Override
            public List<RowAction> addToMenu() {
                return allowedActions;
            }
        }.getRowActions());

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

    JPanel createAllDeliveryPanel(final PredicateRowAction toInvoiceAction) {
        final SQLElement eltCmd = Configuration.getInstance().getDirectory().getElement("BON_DE_LIVRAISON");
        final SQLTableModelSourceOnline tableSource = eltCmd.getTableSource(true);
        final List<RowAction> allowedActions = new ArrayList<RowAction>();
        allowedActions.add(toInvoiceAction);
        BaseSQLTableModelColumn colAvancement = new BaseSQLTableModelColumn("Avancement facturation", BigDecimal.class) {

            @Override
            protected Object show_(SQLRowAccessor r) {

                return getAvancement(r);
            }

            @Override
            public Set<FieldPath> getPaths() {
                final Path p = new PathBuilder(eltCmd.getTable()).addTable("TR_BON_DE_LIVRAISON").addTable("SAISIE_VENTE_FACTURE").build();
                return CollectionUtils.createSet(new FieldPath(p, "T_HT"));
            }
        };
        tableSource.getColumns().add(colAvancement);
        colAvancement.setRenderer(new PercentTableCellRenderer());
        final ListeAddPanel panel = getPanel(eltCmd, tableSource, allowedActions);
        return panel;
    }

    private BigDecimal getAvancement(SQLRowAccessor r) {
        Collection<? extends SQLRowAccessor> rows = r.getReferentRows(r.getTable().getTable("TR_BON_DE_LIVRAISON"));
        long totalFact = 0;
        long total = r.getLong("TOTAL_HT");
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

    JPanel createDeliveryWithInvoicePanel() {
        final SQLElement eltCmd = Configuration.getInstance().getDirectory().getElement("BON_DE_LIVRAISON");
        final SQLTableModelSourceOnline tableSource = eltCmd.getTableSource(true);
        final List<RowAction> allowedActions = new ArrayList<RowAction>();

        // Filter on transfered
        final SQLInjector injector = SQLInjector.getInjector(eltCmd.getTable(), eltCmd.getTable().getTable("SAISIE_VENTE_FACTURE"));
        injector.setOnlyTransfered(tableSource);

        final ListeAddPanel panel = getPanel(eltCmd, tableSource, allowedActions);
        return panel;
    }

    JPanel createDeliveryWithoutInvoicePanel(PredicateRowAction toInvoiceAction) {
        final SQLElement eltCmd = Configuration.getInstance().getDirectory().getElement("BON_DE_LIVRAISON");
        final SQLTableModelSourceOnline tableSource = eltCmd.getTableSource(true);
        final List<RowAction> allowedActions = new ArrayList<RowAction>();
        allowedActions.add(toInvoiceAction);

        // Filter on not transfered
        final SQLInjector injector = SQLInjector.getInjector(eltCmd.getTable(), eltCmd.getTable().getTable("SAISIE_VENTE_FACTURE"));
        injector.setOnlyNotTransfered(tableSource);

        final ListeAddPanel panel = getPanel(eltCmd, tableSource, allowedActions);
        return panel;
    }

    /**
     * Transfert en Facture
     * 
     * @param row
     */
    private void transfertFactureClient(List<SQLRowValues> rows) {
        TransfertBaseSQLComponent.openTransfertFrame(rows, "SAISIE_VENTE_FACTURE");
    }
}
