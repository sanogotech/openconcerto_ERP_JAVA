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

import org.openconcerto.erp.core.sales.pos.TicketPrinterConfiguration;
import org.openconcerto.erp.core.sales.pos.io.ESCSerialPrinter;
import org.openconcerto.erp.core.sales.pos.io.TicketPrinter;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.utils.ExceptionHandler;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;

public class TicketPrinterConfigPanel extends JPanel {
    private final JComboBox comboType;
    private final JSpinner printWidth;
    private final JSpinner copyCountSpinner;
    private JLabel labelPort;
    private final JTextField textPort;
    private final JButton selectPortButton;
    private final JTextField textLibJPOS;

    private TicketPrinterConfiguration configuration;

    public TicketPrinterConfigPanel() {
        comboType = new JComboBox(new String[] { "Imprimante standard ESCP", "Imprimante série ESCP", "Imprimante JavaPOS" });
        comboType.setSelectedIndex(0);
        textPort = new JTextField(10);
        selectPortButton = new JButton("Sélectionner");
        textLibJPOS = new JTextField(24);
        textLibJPOS.setEnabled(false);
        printWidth = new JSpinner(new SpinnerNumberModel(42, 10, 320, 1));
        printWidth.setOpaque(false);
        copyCountSpinner = new JSpinner(new SpinnerNumberModel(1, 0, 10, 1));
        copyCountSpinner.setOpaque(false);
        // Layout
        this.setLayout(new GridBagLayout());
        GridBagConstraints c = new DefaultGridBagConstraints();
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.HORIZONTAL;

        this.add(new JLabel("Largeur (en caractères)", SwingConstants.RIGHT), c);
        c.gridx++;
        c.fill = GridBagConstraints.NONE;
        this.add(this.printWidth, c);
        c.gridy++;
        c.gridx = 0;
        c.weightx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.add(new JLabel("Nombre de tickets", SwingConstants.RIGHT), c);
        c.gridx++;
        c.fill = GridBagConstraints.NONE;
        this.add(this.copyCountSpinner, c);
        c.gridy++;
        c.gridx = 0;
        c.weightx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.add(new JLabel("Type", SwingConstants.RIGHT), c);
        c.gridx++;
        c.weightx = 1;
        c.fill = GridBagConstraints.NONE;

        this.add(this.comboType, c);
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        labelPort = new JLabel("Nom", SwingConstants.RIGHT);
        this.add(labelPort, c);
        c.gridx++;

        this.add(this.textPort, c);
        c.gridx++;
        selectPortButton.setOpaque(false);
        this.add(this.selectPortButton, c);
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        this.add(new JLabel("Dossier JavaPOS", SwingConstants.RIGHT), c);
        c.gridx++;

        this.add(this.textLibJPOS, c);
        c.gridx = 1;
        c.gridy++;
        c.weightx = 0;
        c.gridwidth = 3;
        c.fill = GridBagConstraints.NONE;
        final JButton buttonPrint = new JButton("Imprimer un ticket de test");
        buttonPrint.setOpaque(false);
        this.add(buttonPrint, c);
        buttonPrint.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (textPort.getText().trim().isEmpty()) {
                    JOptionPane.showMessageDialog(TicketPrinterConfigPanel.this, "Port/Imprimante manquant");
                    return;
                }
                commitValues();
                TicketPrinter prt = configuration.createTicketPrinter();
                try {
                    String str = "";
                    final int w = ((Number) printWidth.getValue()).intValue();
                    for (int i = 0; i < w; i++) {
                        str += "" + (i % 10);
                    }
                    prt.addToBuffer("Test");
                    prt.addToBuffer(configuration.getType());
                    prt.addToBuffer(configuration.getName());
                    prt.addToBuffer("" + w);
                    prt.addToBuffer(str);
                    prt.addToBuffer("OK");
                    prt.printBuffer();
                } catch (Exception e1) {
                    ExceptionHandler.handle("Erreur d'impression", e1);
                }

            }
        });
        this.selectPortButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                List<String> choices = new ArrayList<String>();
                final String title;
                final String message;
                if (comboType.getSelectedIndex() == 0) {
                    PrintService[] printServices = PrintServiceLookup.lookupPrintServices(null, null);
                    for (PrintService printer : printServices) {
                        choices.add(printer.getName());
                    }
                    title = "Imprimante ticket";
                    message = "Choisissez l'imprimante ticket à utiliser";
                } else if (comboType.getSelectedIndex() == 1) {
                    choices.addAll(ESCSerialPrinter.getSerialPortNames());
                    title = "Port série";
                    message = "Choisissez le port série lié à l'imprimante ticket";
                } else {
                    return;
                }
                if (choices.isEmpty()) {
                    return;
                }
                String s = (String) JOptionPane.showInputDialog(TicketPrinterConfigPanel.this, message, title, JOptionPane.PLAIN_MESSAGE, null, choices.toArray(), choices.get(0));

                // If a string was returned
                if ((s != null) && (s.length() > 0)) {
                    textPort.setText(s);
                }

            }
        });

        this.comboType.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                commitValues();
                setConfiguration(configuration);

            }
        });
    }

    public void setConfiguration(TicketPrinterConfiguration conf) {
        this.configuration = conf;
        this.printWidth.setValue(conf.getTicketWidth());
        this.copyCountSpinner.setValue(conf.getCopyCount());
        this.textPort.setText(conf.getName());
        this.textLibJPOS.setText(conf.getFolder());
        if (TicketPrinterConfiguration.SERIAL_PRINTER.equals(conf.getType())) {
            this.comboType.setSelectedIndex(1);
            this.labelPort.setText("Port série");
            this.textLibJPOS.setEnabled(false);
        } else if (TicketPrinterConfiguration.JPOS_PRINTER.equals(conf.getType())) {
            this.comboType.setSelectedIndex(2);
            this.labelPort.setText("Imprimante JavaPOS");
            this.textLibJPOS.setEnabled(true);
        } else {
            this.comboType.setSelectedIndex(0);
            this.labelPort.setText("Nom de l'imprimante");
            this.textLibJPOS.setEnabled(false);
        }

    }

    public void commitValues() {
        if (comboType.getSelectedIndex() == 1) {
            this.configuration.setType(TicketPrinterConfiguration.SERIAL_PRINTER);
        } else if (comboType.getSelectedIndex() == 2) {
            this.configuration.setType(TicketPrinterConfiguration.JPOS_PRINTER);
        } else {
            this.configuration.setType(TicketPrinterConfiguration.STANDARD_PRINTER);
        }
        this.configuration.setTicketWidth(((Number) this.printWidth.getValue()).intValue());
        this.configuration.setCopyCount(((Number) this.copyCountSpinner.getValue()).intValue());
        this.configuration.setName(this.textPort.getText());
        this.configuration.setFolder(this.textLibJPOS.getText());
    }
}
