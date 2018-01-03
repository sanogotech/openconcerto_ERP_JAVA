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
 
 package org.openconcerto.erp.core.edm;

import org.openconcerto.erp.core.common.ui.PanelFrame;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.view.list.IListe;
import org.openconcerto.sql.view.list.RowAction.PredicateRowAction;
import org.openconcerto.ui.FrameUtil;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

public class AttachmentAction extends PredicateRowAction {

    public AttachmentAction(final String foreignField) {
        super(new AbstractAction("Fichiers liés") {

            @Override
            public void actionPerformed(ActionEvent e) {
                SQLRowAccessor row = IListe.get(e).getSelectedRow();
                if (foreignField != null) {
                    row = row.getForeign(foreignField);
                }
                AttachmentPanel panel = new AttachmentPanel(row);
                // TODO mettre le nom du sqlelement et la ref??? ex : du devis DEV20170101-003
                PanelFrame frame = new PanelFrame(panel, "Liste des fichiers liés");
                FrameUtil.show(frame);
            }
        }, true);

    }

    public AttachmentAction() {
        this(null);
    }

}
