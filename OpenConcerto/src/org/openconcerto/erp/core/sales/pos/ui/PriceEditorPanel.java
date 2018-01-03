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

import org.openconcerto.erp.core.common.ui.NumericTextField;
import org.openconcerto.erp.core.sales.pos.model.Article;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigDecimal;

import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class PriceEditorPanel extends JPanel {
    CaisseFrame frame;
    private POSLabel labelPrice;
    private Article article;
    final POSRadioButton rHT, rTTC, rDiscountPercent, rDiscount;
    private NumericTextField htTextField;
    private NumericTextField ttcTextField;
    private NumericTextField discountPercentTextField;
    private NumericTextField discountTextField;

    public PriceEditorPanel(final CaisseFrame caisseFrame, final Article article) {
        this.article = article;
        this.frame = caisseFrame;
        this.setBackground(Color.WHITE);
        this.setOpaque(true);
        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.EAST;
        c.weightx = 0;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        c.insets = new Insets(20, 20, 30, 20);
        // Line 1
        c.gridwidth = 2;
        POSLabel title = new POSLabel("Modification du prix de vente");
        this.add(title, c);
        // Line 2
        c.gridy++;
        c.gridwidth = 1;
        rHT = new POSRadioButton("prix HT");
        rHT.setSelected(true);
        c.weightx = 0;
        this.add(rHT, c);
        htTextField = new NumericTextField();
        htTextField.setValue(article.getPriceWithoutTax());
        htTextField.setFont(title.getFont());
        c.gridx++;
        c.weightx = 1;
        this.add(htTextField, c);
        // Line 3
        c.gridy++;
        rTTC = new POSRadioButton("prix TTC");
        htTextField.setValue(article.getPriceWithTax());
        c.gridx = 0;
        c.weightx = 0;
        this.add(rTTC, c);
        ttcTextField = new NumericTextField();
        ttcTextField.setFont(title.getFont());
        c.gridx++;
        c.weightx = 1;
        this.add(ttcTextField, c);
        // Line 4
        c.gridy++;
        rDiscountPercent = new POSRadioButton("remise en %");
        c.gridx = 0;
        c.weightx = 0;
        this.add(rDiscountPercent, c);
        discountPercentTextField = new NumericTextField();
        discountPercentTextField.setValue(BigDecimal.ZERO);
        discountPercentTextField.setFont(title.getFont());
        c.gridx++;
        c.weightx = 1;
        this.add(discountPercentTextField, c);
        // Line 5
        rDiscount = new POSRadioButton("remise HT");
        c.gridx = 0;
        c.weightx = 0;
        c.gridy++;
        this.add(rDiscount, c);
        discountTextField = new NumericTextField();
        discountTextField.setValue(BigDecimal.ZERO);
        discountTextField.setFont(title.getFont());
        c.gridx++;
        c.weightx = 1;
        this.add(discountTextField, c);

        final ButtonGroup group = new ButtonGroup();
        group.add(rHT);
        group.add(rTTC);
        group.add(rDiscountPercent);
        group.add(rDiscount);
        //
        //
        c.gridy++;
        c.gridx = 0;
        c.gridwidth = 2;
        final POSLabel labelPriceOld = new POSLabel("Ancien Prix : ");
        final BigDecimal ttc = Article.computePriceWithTax(this.article.getPriceWithoutTax(), this.article.getIdTaxe());
        ttcTextField.setValue(ttc);
        labelPriceOld.setText("Ancien Prix : " + TicketCellRenderer.toString(this.article.getPriceWithoutTax()) + " HT, " + TicketCellRenderer.toString(ttc));
        this.add(labelPriceOld, c);

        c.gridy++;
        c.gridx = 0;
        labelPrice = new POSLabel("Nouveau Prix : ");
        this.add(labelPrice, c);

        c.gridy++;
        c.gridx = 0;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.SOUTHEAST;

        final JPanel buttons = new JPanel();
        buttons.setOpaque(false);
        POSButton bApply = new POSButton("Appliquer");
        buttons.add(bApply, c);
        POSButton bCancel = new POSButton("Annuler");
        buttons.add(bCancel, c);
        bApply.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                caisseFrame.getControler().setArticleHT(article, getHTFromUI());
                caisseFrame.showCaisse();
            }
        });
        bCancel.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                caisseFrame.showCaisse();
            }
        });

        this.add(buttons, c);
        updatePrice(article.getPriceWithoutTax());
        updateTextFields();
        //
        final ActionListener listenerRadio = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                updatePrice(getHTFromUI());

                updateTextFields();

            }
        };
        this.rDiscount.addActionListener(listenerRadio);
        this.rDiscountPercent.addActionListener(listenerRadio);
        this.rHT.addActionListener(listenerRadio);
        this.rTTC.addActionListener(listenerRadio);

        final DocumentListener docListener = new DocumentListener() {

            @Override
            public void removeUpdate(DocumentEvent e) {
                changedUpdate(e);

            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                changedUpdate(e);

            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updatePrice(getHTFromUI());
            }
        };
        this.ttcTextField.getDocument().addDocumentListener(docListener);
        this.htTextField.getDocument().addDocumentListener(docListener);
        this.discountPercentTextField.getDocument().addDocumentListener(docListener);
        this.discountTextField.getDocument().addDocumentListener(docListener);
    }

    protected BigDecimal getHTFromUI() {
        BigDecimal r = null;
        try {
            if (this.rHT.isSelected()) {
                r = this.htTextField.getValue();
            } else if (this.rTTC.isSelected()) {
                r = Article.computePriceWithoutTax(this.ttcTextField.getValue(), this.article.getIdTaxe());
            } else if (this.rDiscountPercent.isSelected()) {
                r = this.article.getPriceWithoutTax().subtract(this.article.getPriceWithoutTax().multiply(this.discountPercentTextField.getValue().divide(new BigDecimal(100))));
            } else if (this.rDiscount.isSelected()) {
                r = this.article.getPriceWithoutTax().subtract(this.discountTextField.getValue());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (r == null) {
            // fallback if something wrong
            r = this.article.getPriceWithoutTax();
        }
        return r;
    }

    void updatePrice(BigDecimal ht) {
        BigDecimal ttc = Article.computePriceWithTax(ht, this.article.getIdTaxe());
        labelPrice.setText("Nouveau Prix : " + TicketCellRenderer.toString(ht) + " HT, " + TicketCellRenderer.toString(ttc));
    }

    public void updateTextFields() {
        this.invalidate();
        htTextField.setVisible(false);
        ttcTextField.setVisible(false);
        discountPercentTextField.setVisible(false);
        discountTextField.setVisible(false);
        if (rHT.isSelected()) {
            htTextField.setVisible(true);
        } else if (rTTC.isSelected()) {
            ttcTextField.setVisible(true);
        } else if (rDiscountPercent.isSelected()) {
            discountPercentTextField.setVisible(true);
        } else if (rDiscount.isSelected()) {
            discountTextField.setVisible(true);
        }
        this.validate();
        repaint();
    }
}
