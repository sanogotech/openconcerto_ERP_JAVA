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

import org.openconcerto.erp.core.sales.pos.model.Paiement;
import org.openconcerto.ui.DefaultGridBagConstraints;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.Locale;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.text.NumberFormatter;

public class SoldePaiementPanel extends JPanel {

    private JFormattedTextField textField;
    private JRadioButton bCB;
    private JRadioButton bCheque;
    private JRadioButton bEspece;

    public SoldePaiementPanel() {
        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();

        // Label
        c.insets = new Insets(14, 5, 5, 5);
        this.add(new POSLabel("Montant"), c);

        // Textfield
        final NumberFormatter formatter = new NumberFormatter(new DecimalFormat("##0.00", new DecimalFormatSymbols(Locale.ENGLISH)));
        formatter.setAllowsInvalid(false);
        textField = new JFormattedTextField(formatter);
        textField.setHorizontalAlignment(JTextField.RIGHT);
        textField.setFont(textField.getFont().deriveFont(20f));
        textField.setMargin(new Insets(5, 5, 5, 5));
        textField.setPreferredSize(new Dimension(100, textField.getPreferredSize().height));
        c.gridx++;
        c.insets = new Insets(14, 5, 5, 0);
        this.add(textField, c);
        // Reset
        c.gridx++;
        c.insets = new Insets(14, 0, 5, 5);
        final JButton b = new JButton("C");
        b.setBackground(CaissePanel.LIGHT_BLUE);
        b.setForeground(Color.WHITE);
        b.setFont(b.getFont().deriveFont(18f));
        b.setFocusPainted(false);
        b.setPreferredSize(new Dimension(64, 40));
        b.setFocusable(false);
        b.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                textField.setValue(null);

            }
        });
        this.add(b, c);

        c.gridx++;
        //
        c.gridwidth = 3;
        c.gridx = 1;
        c.gridy++;
        c.weighty = 1;
        bCB = createRadioButton("CB");
        bCB.setFocusable(false);
        bCB.setSelected(true);
        this.add(bCB, c);
        c.gridy++;
        bCheque = createRadioButton("Chèque");
        bCheque.setFocusable(false);
        this.add(bCheque, c);
        c.gridy++;
        bEspece = createRadioButton("Espèce");
        bEspece.setFocusable(false);
        this.add(bEspece, c);
        ButtonGroup g = new ButtonGroup();
        g.add(bCB);
        g.add(bCheque);
        g.add(bEspece);
        // Calculator
        c.gridx = 4;
        c.gridy = 0;
        c.gridwidth = 1;
        c.gridheight = 4;
        c.insets = new Insets(0, 0, 0, 0);
        this.add(createCalculatorPanel(), c);
        this.setBackground(Color.WHITE);
    }

    private JRadioButton createRadioButton(String string) {
        JRadioButton b = new JRadioButton(string);
        b.setFont(b.getFont().deriveFont(20f));
        b.setOpaque(false);
        b.setFocusable(false);
        return b;
    }

    JPanel createCalculatorPanel() {
        JPanel p = new JPanel();
        p.setLayout(new GridBagLayout());
        GridBagConstraints c = new DefaultGridBagConstraints();
        JButton b7 = createButton("7");
        p.add(b7, c);
        c.gridx++;
        JButton b8 = createButton("8");
        p.add(b8, c);
        c.gridx++;
        JButton b9 = createButton("9");
        p.add(b9, c);
        //
        c.gridx = 0;
        c.gridy++;
        JButton b4 = createButton("4");
        p.add(b4, c);
        c.gridx++;
        JButton b5 = createButton("5");
        p.add(b5, c);
        c.gridx++;
        JButton b6 = createButton("6");
        p.add(b6, c);

        //
        c.gridx = 0;
        c.gridy++;
        JButton b1 = createButton("1");
        p.add(b1, c);
        c.gridx++;
        JButton b2 = createButton("2");
        p.add(b2, c);
        c.gridx++;
        JButton b3 = createButton("3");
        p.add(b3, c);
        //
        c.gridx = 0;
        c.gridy++;
        JButton b0 = createButton("0");
        c.gridwidth = 2;
        p.add(b0, c);
        c.gridx += 2;
        c.gridwidth = 1;
        JButton bPoint = createButton(".");

        p.add(bPoint, c);
        p.setBackground(CaissePanel.LIGHT_BLUE);
        p.setFocusable(false);
        return p;
    }

    private JButton createButton(final String string) {
        final JButton b = new JButton(string);
        b.setBackground(CaissePanel.DARK_BLUE);
        b.setForeground(Color.WHITE);
        b.setFont(b.getFont().deriveFont(20f));
        b.setBorderPainted(false);
        // b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setPreferredSize(new Dimension(64, 64));
        b.setFocusable(false);
        b.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                final char c = string.charAt(0);
                textField.grabFocus();
                textField.dispatchEvent(new KeyEvent(textField, KeyEvent.KEY_TYPED, System.currentTimeMillis(), 0, KeyEvent.VK_UNDEFINED, c));

            }
        });
        return b;
    }

    public BigDecimal getAmount() {
        try {
            this.textField.commitEdit();
            BigDecimal m = new BigDecimal(((Number) this.textField.getValue()).doubleValue());
            return m;
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }

    }

    public int getPaymentType() {
        if (bCB.isSelected()) {
            return Paiement.CB;
        }
        if (bCheque.isSelected()) {
            return Paiement.CHEQUE;
        }
        if (bEspece.isSelected()) {
            return Paiement.ESPECES;
        }
        throw new IllegalStateException("Unable to compute payment type");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                final JFrame jFrame = new JFrame();
                jFrame.setUndecorated(true);
                jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                jFrame.setContentPane(new SoldePaiementPanel());
                jFrame.pack();

                jFrame.setVisible(true);
            }
        });

    }
}
