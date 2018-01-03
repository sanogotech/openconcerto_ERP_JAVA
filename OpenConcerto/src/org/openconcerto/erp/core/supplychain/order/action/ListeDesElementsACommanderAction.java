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
 
 package org.openconcerto.erp.core.supplychain.order.action;

import org.openconcerto.erp.action.CreateFrameAbstractAction;
import org.openconcerto.erp.core.common.ui.IListFilterDatePanel;
import org.openconcerto.erp.core.common.ui.ListeViewPanel;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.FieldPath;
import org.openconcerto.sql.model.FieldRef;
import org.openconcerto.sql.model.SQLName;
import org.openconcerto.sql.model.SQLRowAccessor;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class ListeDesElementsACommanderAction extends CreateFrameAbstractAction {

    public ListeDesElementsACommanderAction() {
        super();
        this.putValue(Action.NAME, "Liste des éléments en attente de réception");
    }

    private BaseSQLTableModelColumn colAvancement;

    public JFrame createFrame() {
        final JFrame frame = new JFrame("Eléments à réceptionner");
        // Actions
        final SQLElement eltCmd = Configuration.getInstance().getDirectory().getElement("COMMANDE_ELEMENT");
        final JPanel orderPanel = createPanel();

        frame.getContentPane().add(orderPanel);
        FrameUtil.setBounds(frame);
        final File file = IListFrame.getConfigFile(eltCmd, frame.getClass());
        if (file != null)
            new WindowStateManager(frame, file).loadState();
        return frame;
    }

    JPanel createPanel() {
        final SQLElement eltCmd = Configuration.getInstance().getDirectory().getElement("COMMANDE_ELEMENT");
        final SQLTableModelSourceOnline tableSource = eltCmd.getTableSource(true);
        tableSource.getReq().setSelectTransf(new ITransformer<SQLSelect, SQLSelect>() {

            @Override
            public SQLSelect transformChecked(SQLSelect input) {
                // new SQLName(eltCmd.getTable().getDBRoot().getName(),
                // tableBLElement.getName()).quote()
                final String quoteQteL = new SQLName(input.getAlias(eltCmd.getTable()).getAlias(), "QTE_RECUE").quote();
                final String quoteQte = new SQLName(input.getAlias(eltCmd.getTable()).getAlias(), "QTE").quote();
                final String quoteQteU = new SQLName(input.getAlias(eltCmd.getTable()).getAlias(), "QTE_UNITAIRE").quote();
                Where w = Where.createRaw(quoteQteL + " < (" + quoteQte + "*" + quoteQteU + ")", eltCmd.getTable().getField("QTE_RECUE"), eltCmd.getTable().getField("QTE"),
                        eltCmd.getTable().getField("QTE_UNITAIRE"));
                input.setWhere(w);
                return input;
            }
        });

        final ListeAddPanel panel = getPanel(eltCmd, tableSource);

        return panel;
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
        final IListFilterDatePanel datePanel = new IListFilterDatePanel(panel.getListe(), eltCmd.getTable().getForeignTable("ID_COMMANDE").getField("DATE"), IListFilterDatePanel.getDefaultMap());

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
