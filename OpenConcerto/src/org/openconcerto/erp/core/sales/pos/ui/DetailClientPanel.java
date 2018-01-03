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

import org.openconcerto.erp.core.sales.pos.POSConfiguration;
import org.openconcerto.erp.core.sales.pos.TicketPrinterConfiguration;
import org.openconcerto.erp.core.sales.pos.io.TicketPrinter;
import org.openconcerto.erp.core.sales.pos.model.Client;
import org.openconcerto.erp.core.sales.pos.model.Transaction;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.DefaultListModel;
import org.openconcerto.ui.touch.ScrollableList;
import org.openconcerto.utils.ExceptionHandler;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.ibm.icu.text.DateFormat;

public class DetailClientPanel extends JPanel {
    private CaisseFrame caisseFrame;

    DetailClientPanel(CaisseFrame f) {
        this.caisseFrame = f;
        this.setOpaque(false);
        this.setLayout(new GridBagLayout());
        GridBagConstraints c = new DefaultGridBagConstraints();
        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        final JPanel filler = new JPanel();
        filler.setOpaque(false);
        this.add(filler, c);
    }

    public void setSelectedClient(final Client client) {

        this.removeAll();
        this.invalidate();
        final Font f = new Font("Arial", Font.PLAIN, 24);
        GridBagConstraints c = new DefaultGridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 0;
        final JLabel label = new JLabel(client.getFullName());
        label.setFont(f);
        this.add(label, c);
        c.gridy++;
        final JLabel labelTransaction = new JLabel("Transactions");
        labelTransaction.setFont(f);
        this.add(labelTransaction, c);
        c.gridy++;
        //
        c.weighty = 1;
        this.add(createTransactionList(client), c);
        //

        c.fill = GridBagConstraints.NONE;
        c.weighty = 0;
        c.gridy++;
        final JButton bCredit = new POSButton("Créditer le compte");
        this.add(bCredit, c);

        //
        c.gridy++;
        c.anchor = GridBagConstraints.EAST;
        c.insets = new Insets(20, 20, 5, 2);
        final JButton bSelect = new POSButton("Sélectionner ce client");
        this.add(bSelect, c);

        this.validate();
        this.repaint();

        // Listeners
        bCredit.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                TransparentPanel tP = new TransparentPanel(caisseFrame);
                //
                JPanel p = new JPanel();
                p.setBackground(Color.WHITE);
                p.setLayout(new GridBagLayout());
                GridBagConstraints constraints = new DefaultGridBagConstraints();
                constraints.fill = GridBagConstraints.BOTH;
                constraints.insets = new Insets(20, 20, 20, 20);
                constraints.gridwidth = 2;
                constraints.weightx = 1;
                POSLabel label = new POSLabel(client.getFullName());
                p.add(label, constraints);
                constraints.gridy++;

                final SoldePaiementPanel soldePanel = new SoldePaiementPanel();
                p.add(soldePanel, constraints);
                constraints.gridwidth = 1;
                constraints.gridy++;
                constraints.gridx = 0;
                constraints.fill = GridBagConstraints.NONE;
                constraints.anchor = GridBagConstraints.EAST;
                POSButton bC = new POSButton("Créditer");
                p.add(bC, constraints);
                bC.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        try {
                            final BigDecimal amount = soldePanel.getAmount();
                            final int paymentType = soldePanel.getPaymentType();
                            client.credit(amount, paymentType);
                            final BigDecimal nouveauSolde = client.getSolde();
                            final Thread t = new Thread(new Runnable() {

                                @Override
                                public void run() {
                                    final TicketPrinterConfiguration conf1 = POSConfiguration.getInstance().getTicketPrinterConfiguration1();
                                    if (conf1.isValid()) {
                                        final TicketPrinter prt = conf1.createTicketPrinter();
                                        final int ticketWidth = conf1.getTicketWidth();
                                        client.printCredit(prt, ticketWidth, amount, paymentType, nouveauSolde);
                                    }

                                }
                            });
                            t.setDaemon(true);
                            t.start();
                            caisseFrame.showClients();
                        } catch (Exception e1) {
                            ExceptionHandler.handle("Erreur lors du crédit", e1);
                        }

                    }
                });
                constraints.gridx++;
                constraints.weightx = 0;
                POSButton bClose = new POSButton("Fermer");
                p.add(bClose, constraints);
                bClose.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        caisseFrame.showClients();
                    }
                });
                //
                tP.setLayout(new GridBagLayout());
                GridBagConstraints c = new GridBagConstraints();
                c.gridwidth = 1;
                c.gridheight = 1;
                c.fill = GridBagConstraints.NONE;
                c.anchor = GridBagConstraints.CENTER;
                tP.add(p, c);
                caisseFrame.invalidate();
                caisseFrame.setContentPane(tP);
                caisseFrame.validate();
                caisseFrame.repaint();

            }
        });
        bSelect.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                caisseFrame.setClient(client);
                caisseFrame.showCaisse();
            }
        });
    }

    private Component createTransactionList(Client client) {
        final DefaultListModel ticketLlistModel = new DefaultListModel();
        final List<Transaction> transactions = client.getTransactions();
        final Font f = new Font("Arial", Font.PLAIN, 24);
        if (transactions.isEmpty()) {
            final JPanel p = new JPanel();
            p.setBackground(Color.WHITE);
            p.setLayout(new BorderLayout());
            final JLabel label = new JLabel("Aucune transaction pour le moment");
            label.setFont(f);
            label.setForeground(Color.GRAY);
            p.add(label, BorderLayout.NORTH);
            return p;
        }

        ticketLlistModel.addAll(transactions);

        final ScrollableList clientList = new ScrollableList(ticketLlistModel) {
            @Override
            public void paintCell(Graphics g, Object object, int index, boolean isSelected, int posY) {
                g.setFont(f);
                g.setColor(Color.WHITE);
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
                Transaction client = (Transaction) object;
                String label = DateFormat.getDateTimeInstance().format(client.getDate());
                final int soldeInCents = client.getAmount().movePointRight(2).setScale(0, RoundingMode.HALF_UP).intValue();
                String euro = TicketCellRenderer.centsToString(soldeInCents) + "€";

                int wEuro = (int) g.getFontMetrics().getStringBounds(euro, g).getWidth();
                g.drawString(label, 10, posY + 24);
                g.drawString(euro, getWidth() - 5 - wEuro, posY + 24);

            }
        };
        return clientList;
    }

}
