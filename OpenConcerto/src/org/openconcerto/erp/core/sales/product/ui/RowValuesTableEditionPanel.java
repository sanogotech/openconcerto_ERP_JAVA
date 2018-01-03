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
 
 package org.openconcerto.erp.core.sales.product.ui;

import org.openconcerto.sql.view.list.RowValuesTablePanel;
import org.openconcerto.ui.DefaultGridBagConstraints;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;

public class RowValuesTableEditionPanel extends JPanel {
    final RowValuesTablePanel tablePanel;
    final JButton addButton = new JButton("Ajouter");
    final JButton removeButton = new JButton("Supprimer");

    public RowValuesTableEditionPanel(final RowValuesTablePanel tablePanel) {
        this.tablePanel = tablePanel;
        this.setLayout(new GridBagLayout());
        this.setOpaque(false);
        final GridBagConstraints c = new DefaultGridBagConstraints();
        c.weightx = 1;
        // Toolbar
        final JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        toolbar.setOpaque(false);

        addButton.setOpaque(false);
        toolbar.add(addButton);

        removeButton.setOpaque(false);
        toolbar.add(removeButton, c);

        this.add(toolbar, c);

        // Items
        c.gridy++;
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;

        tablePanel.setOpaque(false);
        this.add(tablePanel, c);

        // Listeners
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                tablePanel.getModel().addNewRow();
            }
        });
        removeButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                tablePanel.removeSelectedRow();
            }
        });

    }

    public RowValuesTablePanel getTablePanel() {
        return tablePanel;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.addButton.setEnabled(enabled);
        this.removeButton.setEnabled(enabled);
        this.tablePanel.setEnabled(enabled);
        super.setEnabled(enabled);
    }
}
