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
 
 package org.openconcerto.erp.core.sales.quote.action;

import org.openconcerto.erp.action.CreateFrameAbstractAction;
import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.common.ui.IListFilterDatePanel;
import org.openconcerto.erp.core.common.ui.IListTotalPanel;
import org.openconcerto.erp.core.common.ui.ListeViewPanel;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.view.IListFrame;
import org.openconcerto.sql.view.list.IListe;
import org.openconcerto.ui.DefaultGridBagConstraints;

import java.awt.GridBagConstraints;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;
import javax.swing.JFrame;

public class ListeDesElementsDevisAction extends CreateFrameAbstractAction {

    public ListeDesElementsDevisAction() {
        super();
        this.putValue(Action.NAME, "Liste des missions proposées");
    }

    public JFrame createFrame() {
        final SQLElement element = Configuration.getInstance().getDirectory().getElement("DEVIS_ELEMENT");

        IListe liste = new IListe(element.getTableSource(true));
        final ListeViewPanel listeAddPanel = new ListeViewPanel(element, liste);

        GridBagConstraints c = new DefaultGridBagConstraints();
        c.gridy = 2;
        c.weightx = 0;
        c.weighty = 0;
        c.anchor = GridBagConstraints.EAST;
        c.fill = GridBagConstraints.NONE;

        IListFrame frame = new IListFrame(listeAddPanel);
        frame.setTextTitle("Liste des missions proposées");
        frame.getPanel().getListe().setSQLEditable(false);
        frame.getPanel().setAddVisible(false);
        frame.getPanel().setShowReadOnlyFrameOnDoubleClick(false);
        frame.getPanel().setModifyVisible(false);
        frame.getPanel().setSearchFullMode(true);

        // Date panel
        IListFilterDatePanel datePanel = new IListFilterDatePanel(frame.getPanel().getListe(), element.getTable().getTable("DEVIS").getField("DATE"), IListFilterDatePanel.getDefaultMap());
        c.gridy++;
        c.anchor = GridBagConstraints.CENTER;
        frame.getPanel().add(datePanel, c);

        return frame;
    }
}
