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
 
 package org.openconcerto.erp.core.supplychain.receipt.action;

import javax.swing.Action;
import javax.swing.JFrame;

import org.openconcerto.erp.action.CreateFrameAbstractAction;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.view.IListFrame;
import org.openconcerto.sql.view.ListeAddPanel;
import org.openconcerto.sql.view.list.IListe;
import org.openconcerto.sql.view.list.SQLTableModelSourceOnline;

public class ListeDesReliquatsBonsReceptionsAction extends CreateFrameAbstractAction {

    public ListeDesReliquatsBonsReceptionsAction() {
        super();
        this.putValue(Action.NAME, "Liste des reliquats de bons de réceptions");
    }

    public JFrame createFrame() {
        final SQLElement element = Configuration.getInstance().getDirectory().getElement("RELIQUAT_BR");
        final SQLTableModelSourceOnline tableSource = element.getTableSource(true);

        final IListFrame frame = new IListFrame(new ListeAddPanel(element, new IListe(tableSource)));

        // // Date panel
        // IListFilterDatePanel datePanel = new IListFilterDatePanel(frame.getPanel().getListe(),
        // element.getTable().getField("DATE"), IListFilterDatePanel.getDefaultMap());
        // GridBagConstraints c = new DefaultGridBagConstraints();
        // c.gridwidth = GridBagConstraints.REMAINDER;
        // c.fill = GridBagConstraints.NONE;
        // c.weightx = 0;
        // c.gridy++;
        // c.gridy++;
        // c.anchor = GridBagConstraints.CENTER;
        // datePanel.setFilterOnDefault();
        // frame.getPanel().add(datePanel, c);

        return frame;
    }

}
