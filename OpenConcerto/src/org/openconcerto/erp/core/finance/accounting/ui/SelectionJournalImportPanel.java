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
 
 package org.openconcerto.erp.core.finance.accounting.ui;

import org.openconcerto.erp.core.finance.accounting.model.SelectJournauxModel;
import org.openconcerto.ui.DefaultGridBagConstraints;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Map;
import java.util.concurrent.Semaphore;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

public class SelectionJournalImportPanel extends JPanel {

    public SelectionJournalImportPanel(final String journalTitle, final Map<String, Integer> m, final Semaphore sema) {
        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();

        JLabel label = new JLabel("Le journal " + journalTitle + " n'existe pas. Quel est son type?");
        this.add(label, c);

        final SelectJournauxModel model = new SelectJournauxModel();

        final JTable tableJrnl = new JTable(model);
        c.gridy++;
        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        this.add(tableJrnl, c);

        final JButton button = new JButton("Continuer");
        button.setEnabled(false);
        tableJrnl.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {

                button.setEnabled(true);
            }
        });

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridy++;
        this.add(button, c);
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int id = model.getIdForRow(tableJrnl.getSelectedRow());
                m.put(journalTitle, new Integer(id));
                // synchronized (t) {
                // System.err.println("Notify");
                // t.notify();
                // }
                //
                // sema.release();
                ((Window) SwingUtilities.getRoot(SelectionJournalImportPanel.this)).dispose();

            }
        });

    }
}
