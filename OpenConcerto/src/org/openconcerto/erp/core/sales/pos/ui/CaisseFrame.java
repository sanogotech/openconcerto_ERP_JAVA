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
import org.openconcerto.erp.core.sales.pos.model.Article;
import org.openconcerto.erp.core.sales.pos.model.Client;
import org.openconcerto.erp.core.sales.pos.model.Ticket;
import org.openconcerto.sql.PropsConfiguration;
import org.openconcerto.sql.RemoteShell;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.utils.ClassPathLoader;
import org.openconcerto.utils.ExceptionHandler;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.io.File;
import java.net.MalformedURLException;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;

public class CaisseFrame extends JFrame {
    final CaissePanel mainPanel;

    CaisseFrame() {
        this.mainPanel = new CaissePanel(this);
        setContentPane(mainPanel);
        setFocusable(true);
    }

    public static void main(String[] args) {
        System.setProperty(SQLRowAccessor.ACCESS_DB_IF_NEEDED_PROP, "true");
        try {
            System.out.println("Lancement du module de caisse");
            ToolTipManager.sharedInstance().setInitialDelay(0);
            RemoteShell.startDefaultInstance(null, null);

            System.setProperty(PropsConfiguration.REDIRECT_TO_FILE, "true");
            System.setProperty(SQLBase.ALLOW_OBJECT_REMOVAL, "true");

            ExceptionHandler.setForceUI(true);
            ExceptionHandler.setForumURL("http://www.openconcerto.org/forum");

            // SpeedUp Linux
            System.setProperty("sun.java2d.pmoffscreen", "false");
            System.setProperty(SQLBase.STRUCTURE_USE_XML, "true");
            System.setProperty(PropsConfiguration.REDIRECT_TO_FILE, "true");
            if (POSConfiguration.getInstance().isUsingJPos()) {
                ClassPathLoader c = ClassPathLoader.getInstance();
                try {
                    final List<String> posDirectories = POSConfiguration.getInstance().getJPosDirectories();
                    for (String posDirectory : posDirectories) {
                        if (posDirectory != null && !posDirectory.trim().isEmpty()) {
                            c.addJarFromDirectory(new File(posDirectory.trim()));
                        }
                    }
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
                c.load();

            }
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {

                    try {
                        // UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    try {
                        System.setProperty("awt.useSystemAAFontSettings", "on");
                        System.setProperty("swing.aatext", "true");
                        System.setProperty(ElementComboBox.CAN_MODIFY, "true");

                        POSConfiguration.getInstance().createConnexion();
                        CaisseFrame f = new CaisseFrame();
                        f.setUndecorated(true);
                        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

                        f.pack();
                        f.setLocation(0, 0);
                        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                        if (POSConfiguration.getInstance().getScreenWidth() > 0 && POSConfiguration.getInstance().getScreenHeight() > 0) {
                            f.setSize(new Dimension(POSConfiguration.getInstance().getScreenWidth(), POSConfiguration.getInstance().getScreenHeight()));
                        } else {
                            f.setSize(screenSize);
                        }
                        System.out.println("Affichage de l'interface");
                        f.setVisible(true);
                        if (screenSize.getWidth() < 1280 || screenSize.getHeight() < 720) {
                            JOptionPane.showMessageDialog(f,
                                    "La résolution de votre écran est trop faible.\nLa largeur doit être au minium de 1280 pixels.\nLa hauteur doit être au minium de 720 pixels.");
                        }
                    } catch (Throwable e) {
                        // Catch throwable to be able to see NoClassDefFound and other hard issues
                        ExceptionHandler.handle("Erreur d'initialisation de la caisse (main)", e);
                    }

                }
            });
        } catch (Throwable e) {
            // Catch throwable to be able to see NoClassDefFound and other hard issues
            ExceptionHandler.handle("Erreur d'initialisation de la caisse", e);
        }
    }

    public void showMenu() {
        System.out.println("CaisseFrame.showMenu()");
        this.invalidate();
        final CaisseMenuPanel panel = new CaisseMenuPanel(this);

        final POSGlassPane glassPane2 = new POSGlassPane(panel, (getWidth() - panel.getPreferredSize().width) / 2, 100) {
            @Override
            public void mousePressed(MouseEvent e) {
                Point containerPoint = SwingUtilities.convertPoint(this, e.getPoint(), panel);
                if (containerPoint.x < 0 || containerPoint.x > panel.getWidth() || containerPoint.y < 0 || containerPoint.y > panel.getHeight()) {
                    setGlassPane(new JPanel());
                    getGlassPane().setVisible(false);
                }
                super.mousePressed(e);
            }

        };
        this.setGlassPane(glassPane2);
        this.getGlassPane().setVisible(true);
        this.validate();
        this.repaint();
        this.getControler().setLCD("OpenConcerto", "Menu", 0);

    }

    public void showPriceEditor(Article article, CaisseControler caisseControler) {
        getControler().disableBarcodeReader();
        System.out.println("CaisseFrame.showPriceEditor()");
        this.invalidate();
        final PriceEditorPanel panel = new PriceEditorPanel(this, article);

        final POSGlassPane glassPane2 = new POSGlassPane(panel, (getWidth() - panel.getPreferredSize().width) / 2, 100) {
            @Override
            public void mousePressed(MouseEvent e) {
                Point containerPoint = SwingUtilities.convertPoint(this, e.getPoint(), panel);
                if (containerPoint.x < 0 || containerPoint.x > panel.getWidth() || containerPoint.y < 0 || containerPoint.y > panel.getHeight()) {
                    setGlassPane(new JPanel());
                    getGlassPane().setVisible(false);
                    getControler().enableBarcodeReader();

                }
                super.mousePressed(e);
            }

        };
        this.setGlassPane(glassPane2);
        this.getGlassPane().setVisible(true);
        this.validate();
        this.repaint();

    }

    public void showCaisse() {
        getControler().enableBarcodeReader();
        setGlassPane(new JPanel());
        getGlassPane().setVisible(false);

        System.out.println("CaisseFrame.showCaisse()");
        this.setContentPane(this.mainPanel);
        this.getControler().setLCD("OpenConcerto", "Caisse", 0);
        this.getControler().setLCDDefaultDisplay(5);
    }

    public void showTickets(Ticket t) {
        System.out.println("CaisseFrame.showMenu()");
        final ListeDesTicketsPanel panel = new ListeDesTicketsPanel(this);
        panel.setSelectedTicket(t);
        this.setContentPane(panel);
    }

    public CaisseControler getControler() {
        return this.mainPanel.getControler();
    }

    public void showClients() {
        System.out.println("CaisseFrame.showClients()");
        final ListeDesClientsPanel panel = new ListeDesClientsPanel(this);
        this.setContentPane(panel);
    }

    public void setClient(Client client) {
        System.err.println("CaisseFrame.setClient() " + client.getFullName());
        this.getControler().setClient(client);

    }

    @Override
    public void setContentPane(Container contentPane) {
        this.invalidate();
        setGlassPane(new JPanel());
        getGlassPane().setVisible(false);
        super.setContentPane(contentPane);
        this.validate();
        this.repaint();
    }
}
