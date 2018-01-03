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
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Set;

import javax.swing.JPanel;

import org.openconcerto.erp.core.sales.pos.POSConfiguration;
import org.openconcerto.task.config.ComptaBasePropsConfiguration;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.JImage;

public class CaisseMenuPanel extends JPanel {

    private CaisseFrame frame;

    CaisseMenuPanel(CaisseFrame caisseFrame) {
        this.frame = caisseFrame;
        this.setBackground(Color.WHITE);
        this.setOpaque(true);
        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.CENTER;
        c.weightx = 0;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        c.insets = new Insets(20, 20, 30, 20);
        final JImage image = new JImage(ComptaBasePropsConfiguration.class.getResource("logo.png"));
        this.add(image, c);
        c.gridx++;

        final POSButton bTickets = new POSButton("Liste des tickets");
        this.add(bTickets, c);
        c.gridy++;
        final POSButton bCloture = new POSButton("Clôturer");
        this.add(bCloture, c);
        c.gridy++;
        c.insets = new Insets(20, 20, 20, 20);
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.EAST;
        final POSButton bQuit = new POSButton("Quitter");
        bQuit.setBackground(Color.decode("#AD1457"));
        this.add(bQuit, c);
        // Listeners
        bTickets.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    frame.showTickets(null);
                } catch (Exception ex) {
                    ExceptionHandler.handle("Erreur", ex);
                }
            }
        });
        bCloture.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                try {

                    frame.getControler().setLCD("Cloture", "En cours...", 0);
                    POSConfiguration.getInstance().commitAll(POSConfiguration.getInstance().allTickets());
                    frame.getControler().setLCD("Cloture", "Terminee", 0);
                } catch (Exception ex) {
                    ExceptionHandler.handle("Erreur", ex);
                }
            }
        });
        bQuit.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    // Fermeture
                    frame.getControler().setLCD("   CAISSE FERMEE    ", "", 0);
                    frame.dispose();
                    Frame[] l = Frame.getFrames();
                    for (int i = 0; i < l.length; i++) {
                        Frame f = l[i];
                        System.err.println(f.getName() + " " + f + " Displayable: " + f.isDisplayable() + " Valid: " + f.isValid() + " Active: " + f.isActive());
                    }
                    Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
                    for (Thread thread : threadSet) {
                        if (!thread.isDaemon()) {
                            System.err.println(thread.getName() + " " + thread.getId() + " not daemon");
                        }
                    }
                } catch (Exception ex) {
                    ExceptionHandler.handle("Erreur", ex);
                }
            }
        });

    }

}
