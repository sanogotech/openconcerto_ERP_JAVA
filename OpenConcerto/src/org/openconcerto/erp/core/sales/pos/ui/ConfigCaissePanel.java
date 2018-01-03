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

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.config.ServerFinderConfig;
import org.openconcerto.erp.config.ServerFinderPanel;
import org.openconcerto.erp.core.sales.pos.POSConfiguration;
import org.openconcerto.erp.core.sales.pos.io.ESCSerialPrinter;
import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowListRSH;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.JLabelBold;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

public class ConfigCaissePanel extends JPanel {

    private final POSConfiguration configuration = POSConfiguration.getInstance();
    private int userId;
    private int societeId;
    private int caisseId;
    private final ServerFinderPanel serverFinderPanel;
    private final JComboBox comboSociete;
    private final JComboBox comboCaisse;
    private final JComboBox comboUtilisateur;
    private final TicketLineTable headerTable;
    private final TicketLineTable footerTable;
    private JTextField textTerminalCB;

    // Ticket printers
    private final TicketPrinterConfigPanel ticketPanel1;
    private final TicketPrinterConfigPanel ticketPanel2;
    // LCD
    private JComboBox comboLCDType;
    private JTextField textLCDPort;
    private JTextField textLCDLine1;
    private JTextField textLCDLine2;

    public ConfigCaissePanel(final ServerFinderPanel serverFinderPanel) {
        this.serverFinderPanel = serverFinderPanel;

        setOpaque(false);

        setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();
        c.weighty = 0;
        // Fichier
        c.weightx = 0;
        this.add(new JLabel("Fichier de configuration", SwingConstants.RIGHT), c);
        c.gridx++;
        c.gridwidth = 3;
        c.weightx = 1;
        JTextField textConfigurationFile = new JTextField("");
        if (this.configuration.getConfigFile() != null) {
            textConfigurationFile.setText(this.configuration.getConfigFile().getAbsolutePath());
        }
        textConfigurationFile.setEditable(false);
        this.add(textConfigurationFile, c);

        // Connexion
        c.gridy++;
        c.gridx = 0;
        final JLabelBold titleConnexion = new JLabelBold("Connexion");
        c.gridwidth = 2;
        this.add(titleConnexion, c);
        c.gridy++;
        c.gridwidth = 1;
        c.weightx = 0;
        this.add(new JLabel("Société", SwingConstants.RIGHT), c);
        c.gridx++;
        c.weightx = 1;
        this.comboSociete = new JComboBox();
        this.comboSociete.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList list, final Object value, final int index, final boolean isSelected, final boolean cellHasFocus) {
                final SQLRow r = (SQLRow) value;
                String label = "";
                if (r != null) {
                    label = r.getString("NOM") + " (" + r.getString("ID") + ")";
                }
                return super.getListCellRendererComponent(list, label, index, isSelected, cellHasFocus);
            }
        });
        this.add(this.comboSociete, c);
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        this.add(new JLabel("Caisse", SwingConstants.RIGHT), c);
        c.gridx++;
        this.comboCaisse = new JComboBox();
        this.comboCaisse.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList list, final Object value, final int index, final boolean isSelected, final boolean cellHasFocus) {
                final SQLRow r = (SQLRow) value;
                String label = "";
                if (r != null) {
                    label = r.getString("NOM") + " (" + r.getString("ID") + ")";
                }
                return super.getListCellRendererComponent(list, label, index, isSelected, cellHasFocus);
            }
        });
        this.add(this.comboCaisse, c);
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        this.add(new JLabel("Utilisateur", SwingConstants.RIGHT), c);
        c.gridx++;
        this.comboUtilisateur = new JComboBox();
        this.comboUtilisateur.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList list, final Object value, final int index, final boolean isSelected, final boolean cellHasFocus) {
                final SQLRow r = (SQLRow) value;
                String label = "";
                if (r != null) {
                    label = r.getString("NOM") + " " + r.getString("PRENOM") + " (" + r.getString("ID") + ")";
                }
                return super.getListCellRendererComponent(list, label, index, isSelected, cellHasFocus);
            }
        });
        this.add(this.comboUtilisateur, c);

        // Ticket
        final JLabelBold titleTicket = new JLabelBold("Ticket");
        c.gridwidth = 2;
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        this.add(titleTicket, c);

        final JTabbedPane tabsTicket = new JTabbedPane();
        c.gridy++;
        c.gridwidth = 2;
        c.weightx = 0;
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.NORTHWEST;
        this.headerTable = new TicketLineTable();
        tabsTicket.addTab("Entête", this.headerTable);
        this.footerTable = new TicketLineTable();
        tabsTicket.addTab("Pied de page", this.footerTable);
        this.add(tabsTicket, c);

        // Périphérique
        final JLabelBold titleImprimante = new JLabelBold("Périphériques");
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.gridwidth = 2;
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        this.add(titleImprimante, c);
        c.gridy++;
        //
        JTabbedPane tabs = new JTabbedPane();
        ticketPanel1 = new TicketPrinterConfigPanel();
        ticketPanel1.setOpaque(false);
        ticketPanel2 = new TicketPrinterConfigPanel();
        ticketPanel2.setOpaque(false);
        tabs.addTab("Imprimante ticket principale", ticketPanel1);
        tabs.addTab("Imprimante ticket secondaire", ticketPanel2);
        tabs.addTab("Terminal CB", createCreditCardPanel());
        tabs.addTab("Afficheur LCD", createLCDPanel());
        this.add(tabs, c);
        //

        // Spacer
        c.gridx = 0;
        c.gridy++;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        final JPanel spacer = new JPanel();
        spacer.setOpaque(false);
        spacer.setPreferredSize(new Dimension(10, 10));

        add(spacer, c);

        // Listeners
        this.comboSociete.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(final ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    ConfigCaissePanel.this.societeId = ((SQLRow) e.getItem()).getID();
                    reloadCaisses();
                }
            }
        });
        this.comboCaisse.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    ConfigCaissePanel.this.caisseId = ((SQLRow) e.getItem()).getID();
                }
            }
        });
        this.comboUtilisateur.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    ConfigCaissePanel.this.userId = ((SQLRow) e.getItem()).getID();
                }
            }
        });
    }

    private Component createLCDPanel() {
        final JPanel p = new JPanel();
        p.setOpaque(false);

        GridBagConstraints c = new DefaultGridBagConstraints();
        p.setLayout(new GridBagLayout());
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.EAST;
        c.fill = GridBagConstraints.NONE;
        p.add(new JLabel("Type", SwingConstants.RIGHT), c);
        c.gridx++;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;
        comboLCDType = new JComboBox(new String[] { "Port série", "Emulation imprimante" });

        p.add(comboLCDType, c);
        c.gridy++;
        c.gridx = 0;
        c.anchor = GridBagConstraints.EAST;
        final JLabel labelType = new JLabel("Port", SwingConstants.RIGHT);
        p.add(labelType, c);
        c.gridx++;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;

        this.textLCDPort = new JTextField(20);
        p.add(this.textLCDPort, c);
        c.weightx = 0;
        c.gridx++;
        final JButton selectPortButton = new JButton("Sélectionner");
        selectPortButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                List<String> choices = new ArrayList<String>();
                final String title;
                final String message;
                if (comboLCDType.getSelectedIndex() == 1) {
                    PrintService[] printServices = PrintServiceLookup.lookupPrintServices(null, null);
                    for (PrintService printer : printServices) {
                        choices.add(printer.getName());
                    }
                    title = "Afficheur LCD";
                    message = "Choisissez l'imprimante correspondante à l'afficheur LCD";
                } else if (comboLCDType.getSelectedIndex() == 0) {
                    choices.addAll(ESCSerialPrinter.getSerialPortNames());
                    title = "Port série";
                    message = "Choisissez le port série lié à l'afficheur LCD";
                } else {
                    return;
                }
                if (choices.isEmpty()) {
                    return;
                }
                String s = (String) JOptionPane.showInputDialog(p, message, title, JOptionPane.PLAIN_MESSAGE, null, choices.toArray(), choices.get(0));

                // If a string was returned
                if ((s != null) && (s.length() > 0)) {
                    textLCDPort.setText(s);
                }

            }
        });
        p.add(selectPortButton, c);

        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 3;
        p.add(new JLabel("Message d'accueil de l'afficheur"), c);
        //
        c.gridy++;
        c.gridwidth = 1;
        p.add(new JLabel("Ligne 1", SwingConstants.RIGHT), c);
        c.gridx++;
        this.textLCDLine1 = new JTextField(20);
        p.add(textLCDLine1, c);

        c.gridx = 0;
        c.gridy++;
        p.add(new JLabel("Ligne 2", SwingConstants.RIGHT), c);
        c.gridx++;
        this.textLCDLine2 = new JTextField(20);
        p.add(textLCDLine2, c);

        final JPanel spacer = new JPanel();
        spacer.setOpaque(false);
        c.gridy++;
        c.weighty = 1;
        p.add(spacer, c);
        return p;
    }

    private JPanel createCreditCardPanel() {
        final JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new GridBagLayout());
        GridBagConstraints c = new DefaultGridBagConstraints();

        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.EAST;
        c.fill = GridBagConstraints.NONE;
        p.add(new JLabel("Port série", SwingConstants.RIGHT), c);
        c.gridx++;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;

        this.textTerminalCB = new JTextField(20);
        p.add(this.textTerminalCB, c);
        c.weightx = 0;
        c.gridx++;
        final JButton selectPortButton = new JButton("Sélectionner");
        selectPortButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                List<String> choices = new ArrayList<String>();
                final String title;
                final String message;

                choices.addAll(ESCSerialPrinter.getSerialPortNames());
                title = "Port série";
                message = "Choisissez le port série lié au terminal CB";
                if (choices.isEmpty()) {
                    return;
                }
                String s = (String) JOptionPane.showInputDialog(p, message, title, JOptionPane.PLAIN_MESSAGE, null, choices.toArray(), choices.get(0));
                // If a string was returned
                if ((s != null) && (s.length() > 0)) {
                    textTerminalCB.setText(s);
                }
            }
        });
        p.add(selectPortButton, c);

        final JPanel spacer = new JPanel();
        spacer.setOpaque(false);
        c.gridy++;
        c.weighty = 1;
        p.add(spacer, c);

        return p;
    }

    protected void reloadCaisses() {
        this.comboCaisse.setEnabled(false);
        final int id = this.societeId;
        final ServerFinderConfig config = ConfigCaissePanel.this.serverFinderPanel.createServerFinderConfig();
        if (!config.isOnline()) {
            JOptionPane.showMessageDialog(ConfigCaissePanel.this, "Impossible de se connecter au serveur");
            return;
        }

        System.out.println("Reloading POS information from: " + config);
        ComptaPropsConfiguration conf = config.createConf();
        try {
            // Sociétés
            conf.setUpSocieteStructure(id);
            final SQLRow societe = conf.getRowSociete();
            if (societe != null) {
                final String name = societe.getString("DATABASE_NAME");
                // Caisses
                final SQLSelect sel = new SQLSelect();
                sel.addSelectStar(conf.getRootSociete().getTable("CAISSE"));
                final List<SQLRow> caisses = SQLRowListRSH.execute(sel);
                // Stock l'id de la caisse pour que la reslectionne soit correcte
                final int idCaisseToSelect = this.caisseId;
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        ConfigCaissePanel.this.caisseId = idCaisseToSelect;
                        if (caisses.isEmpty()) {
                            JOptionPane.showMessageDialog(ConfigCaissePanel.this, "Pas de caisses définies dans la société " + name);
                        }
                        ConfigCaissePanel.this.comboCaisse.setModel(new DefaultComboBoxModel(new Vector<SQLRow>(caisses)));
                        ConfigCaissePanel.this.comboUtilisateur.setEnabled(true);
                        ConfigCaissePanel.this.comboCaisse.setEnabled(true);
                        final ComboBoxModel model = ConfigCaissePanel.this.comboCaisse.getModel();
                        final int stop = model.getSize();
                        if (stop > 0) {
                            // Force la reselection si la valeur n'existe plus,
                            // nécessité de recuperer l'id
                            ConfigCaissePanel.this.caisseId = ((SQLRow) model.getElementAt(0)).getID();
                        }
                        for (int i = 0; i < stop; i++) {
                            final SQLRow r = (SQLRow) model.getElementAt(i);
                            if (r.getID() == idCaisseToSelect) {
                                ConfigCaissePanel.this.comboCaisse.setSelectedItem(r);
                                break;
                            }
                        }
                    }

                });
            } else {
                JOptionPane.showMessageDialog(this, "Impossible de trouver la société d'ID " + id);
            }
        } catch (final Exception e) {
            e.printStackTrace();
        } finally {
            conf.destroy();
        }
    }

    public void loadConfiguration() {

        final POSConfiguration configuration = this.configuration;
        // Terminal CB
        this.textTerminalCB.setText(configuration.getCreditCardPort());
        // Afficheur LCD
        final String lcdType = configuration.getLCDType();
        if (lcdType.equals("serial")) {
            this.comboLCDType.setSelectedIndex(0);
        } else {
            this.comboLCDType.setSelectedIndex(1);
        }

        this.textLCDPort.setText(configuration.getLCDPort());
        this.textLCDLine1.setText(configuration.getLCDLine1());
        this.textLCDLine2.setText(configuration.getLCDLine2());

        this.userId = configuration.getUserID();
        this.societeId = configuration.getCompanyID();
        this.caisseId = configuration.getPosID();
        this.headerTable.fillFrom(configuration.getHeaderLines());
        this.footerTable.fillFrom(configuration.getFooterLines());
        this.ticketPanel1.setConfiguration(configuration.getTicketPrinterConfiguration1());
        this.ticketPanel2.setConfiguration(configuration.getTicketPrinterConfiguration2());

        addComponentListener(new ComponentListener() {
            @Override
            public void componentHidden(final ComponentEvent event) {
            }

            @Override
            public void componentMoved(final ComponentEvent event) {
            }

            @Override
            public void componentResized(final ComponentEvent event) {
            }

            @Override
            public void componentShown(final ComponentEvent event) {
                final Thread t = new Thread(new Runnable() {

                    @Override
                    public void run() {
                        ConfigCaissePanel.this.comboSociete.setEnabled(false);
                        ConfigCaissePanel.this.comboUtilisateur.setEnabled(false);
                        ConfigCaissePanel.this.comboCaisse.setEnabled(false);
                        final ServerFinderConfig config = ConfigCaissePanel.this.serverFinderPanel.createServerFinderConfig();
                        if (!config.isOnline()) {
                            JOptionPane.showMessageDialog(ConfigCaissePanel.this, "Impossible de se connecter au serveur");
                            return;
                        }
                        final ComptaPropsConfiguration server = config.createConf();
                        try {
                            final DBRoot root = server.getRoot();

                            // Sociétés
                            SQLSelect sel = new SQLSelect();
                            sel.addSelectStar(root.findTable("SOCIETE_COMMON"));
                            final List<SQLRow> societes = SQLRowListRSH.execute(sel);

                            // Utilisateurs
                            sel = new SQLSelect();
                            sel.addSelectStar(root.findTable("USER_COMMON"));
                            final List<SQLRow> utilisateurs = SQLRowListRSH.execute(sel);

                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    ConfigCaissePanel.this.comboSociete.setModel(new DefaultComboBoxModel(new Vector<SQLRow>(societes)));
                                    ConfigCaissePanel.this.comboUtilisateur.setModel(new DefaultComboBoxModel(new Vector<SQLRow>(utilisateurs)));

                                    ConfigCaissePanel.this.comboSociete.setEnabled(true);
                                    ConfigCaissePanel.this.comboUtilisateur.setEnabled(true);

                                    // Societe
                                    ComboBoxModel model = ConfigCaissePanel.this.comboSociete.getModel();
                                    int stop = model.getSize();
                                    boolean societeFound = false;
                                    for (int i = 0; i < stop; i++) {
                                        final SQLRow r = (SQLRow) model.getElementAt(i);
                                        if (r.getID() == ConfigCaissePanel.this.societeId) {
                                            ConfigCaissePanel.this.comboSociete.setSelectedItem(r);
                                            ConfigCaissePanel.this.societeId = r.getID();
                                            societeFound = true;
                                            break;
                                        }
                                    }

                                    if (!societeFound && stop > 0) {
                                        ConfigCaissePanel.this.comboSociete.setSelectedItem(model.getElementAt(0));
                                        ConfigCaissePanel.this.societeId = ((SQLRow) model.getElementAt(0)).getID();
                                    }
                                    // Utilisateur
                                    model = ConfigCaissePanel.this.comboUtilisateur.getModel();
                                    stop = model.getSize();
                                    boolean utilisateurFound = false;
                                    for (int i = 0; i < stop; i++) {
                                        final SQLRow r = (SQLRow) model.getElementAt(i);
                                        if (r.getID() == ConfigCaissePanel.this.userId) {
                                            ConfigCaissePanel.this.comboUtilisateur.setSelectedItem(r);
                                            ConfigCaissePanel.this.userId = r.getID();
                                            utilisateurFound = true;
                                            break;
                                        }
                                    }

                                    if (!utilisateurFound && stop > 0) {
                                        ConfigCaissePanel.this.comboUtilisateur.setSelectedItem(model.getElementAt(0));
                                        ConfigCaissePanel.this.userId = ((SQLRow) model.getElementAt(0)).getID();
                                    }
                                    final Thread t = new Thread() {
                                        public void run() {
                                            reloadCaisses();
                                        };
                                    };
                                    t.start();

                                }

                            });

                        } catch (final Exception e) {
                            e.printStackTrace();
                        } finally {
                            server.destroy();
                        }
                    }
                });
                t.setDaemon(true);
                t.start();
            }
        });
    }

    public void dumpConfiguration() {
        System.out.println("Societe: id:" + this.societeId);
        System.out.println("Caisse: id:" + this.caisseId);
        System.out.println("Utilisateur:  id:" + this.userId);
    }

    public void saveConfiguration() {
        final POSConfiguration configuration = this.configuration;
        this.ticketPanel1.commitValues();
        this.ticketPanel2.commitValues();
        configuration.setUserID(this.userId);
        configuration.setCompanyID(this.societeId);
        configuration.setPosID(this.caisseId);
        configuration.setHeaderLines(this.headerTable.getLines());
        configuration.setFooterLines(this.footerTable.getLines());
        // Terminal CB
        configuration.setCreditCardPort(this.textTerminalCB.getText());
        // LCD
        final int selectedIndex = this.comboLCDType.getSelectedIndex();
        if (selectedIndex == 0) {
            configuration.setLCDType("serial");
        } else {
            configuration.setLCDType("printer");
        }
        configuration.setLCDPort(this.textLCDPort.getText());
        configuration.setLCDLine1(this.textLCDLine1.getText());
        configuration.setLCDLine2(this.textLCDLine2.getText());
        // Save
        configuration.saveConfiguration();
    }
}
