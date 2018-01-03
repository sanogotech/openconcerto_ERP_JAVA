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
 
 package org.openconcerto.erp.core.sales.pos.ui;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.RoundingMode;
import java.util.Vector;

import javax.swing.JPanel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.openconcerto.erp.core.sales.pos.POSConfiguration;
import org.openconcerto.erp.core.sales.pos.model.Client;
import org.openconcerto.ui.DefaultListModel;
import org.openconcerto.ui.touch.ScrollableList;

public class ListeDesClientsPanel extends JPanel {

    private ScrollableList clientList;
    private DefaultListModel ticketLlistModel;

    ListeDesClientsPanel(final CaisseFrame caisseFrame) {
        this.setBackground(Color.WHITE);
        this.setOpaque(true);
        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.CENTER;
        c.weightx = 1;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;

        final StatusBar p = new StatusBar();
        p.setTitle("Liste des clients");
        p.setLayout(new FlowLayout(FlowLayout.RIGHT));
        final POSButton bBack = new POSButton("Fermer");
        p.add(bBack);
        this.add(p, c);

        // Liste des clients
        c.gridy++;
        c.gridwidth = 1;
        c.weighty = 1;
        c.gridheight = 2;

        ticketLlistModel = new DefaultListModel();
        ticketLlistModel.addAll(new Vector<Client>(POSConfiguration.getInstance().allClients()));
        final Font f = new Font("Arial", Font.PLAIN, 24);
        clientList = new ScrollableList(ticketLlistModel) {
            @Override
            public void paintCell(Graphics g, Object object, int index, boolean isSelected, int posY) {
                g.setFont(f);

                if (isSelected) {
                    g.setColor(new Color(232, 242, 254));
                } else {
                    g.setColor(Color.WHITE);
                }
                g.fillRect(0, posY, getWidth(), getCellHeight());

                //
                g.setColor(Color.GRAY);
                g.drawLine(0, posY + this.getCellHeight() - 1, this.getWidth(), posY + this.getCellHeight() - 1);

                if (isSelected) {
                    g.setColor(Color.BLACK);
                } else {
                    g.setColor(Color.GRAY);
                }
                ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Client client = (Client) object;
                String label = client.getFullName();
                final int soldeInCents = client.getSolde().movePointRight(2).setScale(0, RoundingMode.HALF_UP).intValue();
                String euro = TicketCellRenderer.centsToString(soldeInCents) + "€";

                int wEuro = (int) g.getFontMetrics().getStringBounds(euro, g).getWidth();
                g.drawString(label, 10, posY + 24);
                g.drawString(euro, getWidth() - 5 - wEuro, posY + 24);

                final String addr = client.getAddr();
                if (addr != null) {
                    g.drawString(addr, 10, posY + 48);
                }
            }
        };
        this.add(clientList, c);

        // Detail
        c.fill = GridBagConstraints.BOTH;
        c.gridx++;
        c.gridheight = 1;
        c.weighty = 1;
        c.insets = new Insets(10, 10, 10, 10);

        final DetailClientPanel detailClientPanel = new DetailClientPanel(caisseFrame);
        this.add(detailClientPanel, c);

        clientList.addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                Client selectedValue = (Client) clientList.getSelectedValue();
                detailClientPanel.setSelectedClient(selectedValue);
            }
        });

        bBack.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                caisseFrame.showCaisse();

            }
        });

    }

    public void setSelectedClient(Object selectedValue) {
        clientList.setSelectedValue(selectedValue, true);
    }
}
