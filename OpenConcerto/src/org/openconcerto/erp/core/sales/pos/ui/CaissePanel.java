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

import org.openconcerto.erp.core.sales.pos.model.Article;
import org.openconcerto.erp.core.sales.pos.model.Categorie;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowListRSH;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.utils.ExceptionHandler;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

public class CaissePanel extends JPanel implements CaisseListener {

    public static final Color LIGHT_BLUE = new Color(83, 129, 172);
    public static final Color DARK_BLUE = new Color(0, 98, 159);

    private CaisseControler controler;

    private StatusBar st;
    private ArticleSelectorPanel articleSelectorPanel;
    private ArticleSearchPanel articleSearchPanel;

    private JPanel articleSelector;

    public CaissePanel(final CaisseFrame caisseFrame) {
        this.setLayout(new GridBagLayout());
        this.setBackground(Color.WHITE);
        this.setOpaque(isOpaque());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0;
        c.weighty = 0;

        this.controler = new CaisseControler(caisseFrame);

        c.fill = GridBagConstraints.HORIZONTAL;
        this.st = createStatusBar(caisseFrame);

        this.add(this.st, c);

        TicketPanel t = new TicketPanel(this.controler);
        // fillExampleArticle();
        loadArticles();
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        c.weighty = 1;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.SOUTHWEST;
        c.fill = GridBagConstraints.NONE;
        this.add(t, c);

        c.fill = GridBagConstraints.BOTH;
        c.gridx++;
        c.weightx = 1;
        c.gridy--;
        c.gridheight = 2;
        articleSelectorPanel = new ArticleSelectorPanel(this.controler);
        articleSearchPanel = new ArticleSearchPanel(this.controler);
        articleSelector = articleSelectorPanel;
        this.add(articleSelector, c);

        c.gridx++;
        c.weightx = 0;
        this.add(new PaiementPanel(this.controler), c);
        this.controler.addCaisseListener(this);
    }

    private StatusBar createStatusBar(final CaisseFrame caisseFrame) {
        final StatusBar s = new StatusBar();
        s.setLayout(new GridBagLayout());
        final GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(0, 10, 0, 10);
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.CENTER;
        c.weightx = 0;
        final POSButton bValidate = new POSButton("Valider");
        bValidate.setForeground(Color.WHITE);
        bValidate.setBackground(DARK_BLUE);
        s.add(bValidate, c);
        c.weightx = 1;
        c.gridx++;
        final POSButton bClients = new POSButton("Clients");
        bClients.setForeground(Color.WHITE);
        bClients.setBackground(DARK_BLUE);
        s.add(bClients, c);
        c.gridx++;
        final POSButton bMenu = new POSButton("Menu");
        bMenu.setForeground(Color.WHITE);
        bMenu.setBackground(DARK_BLUE);
        s.add(bMenu, c);
        bValidate.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                // Valider
                CaissePanel.this.controler.setLCD("Impression de", "votre ticket...", 0);
                try {
                    CaissePanel.this.controler.printTicket();
                } catch (UnsatisfiedLinkError ex) {
                    JOptionPane.showMessageDialog(CaissePanel.this, "Erreur de configuration de la liaison à l'imprimante");
                } catch (Throwable ex) {
                    JOptionPane.showMessageDialog(CaissePanel.this, "Erreur d'impression du ticket");
                }
                try {
                    CaissePanel.this.controler.saveAndClearTicket();
                } catch (Throwable ex) {
                    ExceptionHandler.handle("Erreur de sauvegardes des informations du ticket", ex);
                }
                CaissePanel.this.controler.setLCDDefaultDisplay(2);

            }
        });
        bClients.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                // Clients
                try {
                    caisseFrame.showClients();
                } catch (Throwable ex) {
                    ExceptionHandler.handle("Erreur d'affichage du menu", ex);
                }

            }
        });

        bMenu.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                // Menu
                try {
                    caisseFrame.showMenu();
                } catch (Throwable ex) {
                    ExceptionHandler.handle("Erreur d'affichage du menu", ex);
                }

            }
        });

        return s;
    }

    @SuppressWarnings("unchecked")
    private void loadArticles() {

        final Map<Integer, Categorie> categoriesMap = new HashMap<Integer, Categorie>();

        SQLElement eltFam = Configuration.getInstance().getDirectory().getElement("FAMILLE_ARTICLE");
        SQLElement eltArticle = Configuration.getInstance().getDirectory().getElement("ARTICLE");

        SQLSelect selFamille = new SQLSelect(Configuration.getInstance().getBase());

        selFamille.addSelectStar(eltFam.getTable());
        selFamille.addRawOrder(eltFam.getTable().getField("CODE").getFieldRef());
        List<SQLRow> l = (List<SQLRow>) Configuration.getInstance().getBase().getDataSource().execute(selFamille.asString(), SQLRowListRSH.createFromSelect(selFamille, eltFam.getTable()));

        for (SQLRow row : l) {
            // Map id -> Category
            final Categorie cP = categoriesMap.get(row.getInt("ID_FAMILLE_ARTICLE_PERE"));
            Categorie c;
            if (cP != null) {
                c = new Categorie(row.getString("NOM"));
                cP.add(c);
            } else {
                c = new Categorie(row.getString("NOM"), true);
            }

            categoriesMap.put(row.getID(), c);
        }

        final SQLSelect selArticle = new SQLSelect();
        final SQLTable tableArticle = eltArticle.getTable();
        selArticle.addSelectStar(tableArticle);
        selArticle.setWhere(new Where(tableArticle.getField("OBSOLETE"), "=", Boolean.FALSE));
        List<SQLRow> l2 = (List<SQLRow>) Configuration.getInstance().getBase().getDataSource().execute(selArticle.asString(), SQLRowListRSH.createFromSelect(selArticle, tableArticle));

        final Categorie cUnclassified = new Categorie("Non classés", true);
        for (SQLRow row : l2) {

            Categorie s1 = categoriesMap.get(row.getInt("ID_FAMILLE_ARTICLE"));
            if (s1 == null) {
                s1 = cUnclassified;
                categoriesMap.put(row.getInt("ID_FAMILLE_ARTICLE"), cUnclassified);
            }
            final String name = row.getString("NOM").trim();
            if (name.length() > 0) {
                final Article a = new Article(s1, name, row.getID());
                final String barcode = row.getString("CODE_BARRE");
                if (barcode == null || barcode.trim().isEmpty()) {
                    a.setBarCode(row.getString("CODE"));
                } else {
                    a.setBarCode(barcode);
                }
                a.setCode(row.getString("CODE"));
                a.setIdTaxe(row.getInt("ID_TAXE"));
                a.setPriceWithoutTax((BigDecimal) row.getObject("PV_HT"));
                a.setPriceWithTax((BigDecimal) row.getObject("PV_TTC"));
            }
        }

    }

    @Override
    public void paint(Graphics g) {
        System.err.println("CaissePanel.paint()" + this.getWidth() + " x " + this.getHeight());
        super.paint(g);
        Graphics2D g2 = (Graphics2D) g;
        g.setFont(new Font("Arial", Font.PLAIN, 32));
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (this.controler.isClientDefined()) {
            g.setColor(CaissePanel.DARK_BLUE);
            g.setFont(new Font("Arial", Font.PLAIN, 28));
            g.drawString(this.controler.getClient().getFullName(), 20, 75);
            g.setColor(Color.GRAY);
            g.setFont(g.getFont().deriveFont(18f));
            g.drawString("Solde : " + new DecimalFormat("#0.00").format(this.controler.getClient().getSolde()), 20, 120);

        }

        // Prix
        int x = 300;
        int y = 110;
        String euros;
        String cents;
        Rectangle2D r;
        g.setColor(Color.BLACK);
        if (this.controler.isClientDefined()) {
            g.setFont(g.getFont().deriveFont(46f));
            y += 10;
        } else {
            g.setFont(g.getFont().deriveFont(66f));
        }
        final int total = this.controler.getTotal();
        euros = CaisseControler.getEuros(total) + ".";
        cents = CaisseControler.getCents(total);
        r = g.getFontMetrics().getStringBounds(euros, g);
        x = x - (int) r.getWidth();
        g.drawString(euros, x, y);
        g.setFont(g.getFont().deriveFont(40f));
        g.drawString(cents, x + (int) r.getWidth(), y);
        // Paiement
        y += 40;
        x = 300;
        final int paye = this.controler.getPaidTotal();
        euros = CaisseControler.getEuros(paye) + ".";
        cents = CaisseControler.getCents(paye);

        g.setFont(g.getFont().deriveFont(18f));
        Rectangle2D r2 = g.getFontMetrics().getStringBounds("Payé", g);
        if (paye >= total) {
            g.setColor(Color.DARK_GRAY);
        } else {
            g.setColor(Color.ORANGE);
        }
        g.setFont(g.getFont().deriveFont(32f));
        r = g.getFontMetrics().getStringBounds(euros, g);
        g.drawString(euros, x - (int) r.getWidth(), y);
        g.setFont(g.getFont().deriveFont(24f));
        g.drawString(cents, x, y);
        g.setFont(g.getFont().deriveFont(18f));
        g.setColor(Color.GRAY);
        g.drawString("Payé", x - (int) r2.getWidth() - (int) r.getWidth() - 10, y);
        // A rendre
        final boolean minimalHeight = this.getHeight() < 750;
        if (!minimalHeight) {
            y += 40;
            x = 300;
        } else {
            x = 140;
        }
        int aRendre = paye - total;
        if (aRendre != 0) {
            String label;
            if (aRendre > 0) {
                label = "Rendu";
            } else {
                if (!minimalHeight) {
                    label = "Reste à payer";
                } else {
                    label = "Doit";
                }
                aRendre = -aRendre;
            }

            euros = CaisseControler.getEuros(aRendre) + ".";
            cents = CaisseControler.getCents(aRendre);

            g.setFont(g.getFont().deriveFont(18f));
            Rectangle2D r3 = g.getFontMetrics().getStringBounds(label, g);

            g.setColor(Color.DARK_GRAY);
            g.setFont(g.getFont().deriveFont(32f));
            r = g.getFontMetrics().getStringBounds(euros, g);
            g.drawString(euros, x - (int) r.getWidth(), y);
            g.setFont(g.getFont().deriveFont(24f));
            g.drawString(cents, x, y);
            g.setFont(g.getFont().deriveFont(18f));
            g.setColor(Color.GRAY);
            g.drawString(label, x - (int) r3.getWidth() - (int) r.getWidth() - 10, y);

        }

    }

    @Override
    public void caisseStateChanged() {
        repaint();
    }

    public void switchListMode() {

        GridBagConstraints c = ((GridBagLayout) this.getLayout()).getConstraints(articleSelector);
        this.remove(articleSelector);

        if (articleSelector == this.articleSearchPanel) {
            articleSelector = this.articleSelectorPanel;
        } else {
            articleSelector = this.articleSearchPanel;
        }
        this.add(articleSelector, c);
        this.validate();
        this.repaint();

    }

    public CaisseControler getControler() {
        return controler;
    }
}
