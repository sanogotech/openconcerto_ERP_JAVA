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
 
 package org.openconcerto.erp.core.finance.accounting.action;

import org.openconcerto.erp.core.finance.accounting.ui.ImportEcriturePanel;
import org.openconcerto.ui.FrameUtil;
import org.openconcerto.ui.PanelFrame;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

public class ImportEcritureAction extends AbstractAction {

    public ImportEcritureAction() {
        super("Import d'écritures");

    }

    @Override
    public void actionPerformed(ActionEvent e) {

        final PanelFrame frame = new PanelFrame(new ImportEcriturePanel(), "Import d'écritures");
        frame.pack();
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        FrameUtil.show(frame);

    }
}
