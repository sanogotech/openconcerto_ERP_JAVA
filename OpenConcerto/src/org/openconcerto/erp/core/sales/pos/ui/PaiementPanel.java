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
import org.openconcerto.erp.core.sales.pos.model.Paiement;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Rectangle2D;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

public class PaiementPanel extends JPanel implements CaisseListener, MouseListener, BarcodeListener {
    private static final int PAYMENT_POS_Y = 50;
    private static final int PAYMENT_LINE_HEIGHT = 60;

    private CaisseControler controller;
    private String calculatorValue = "";
    int calcHeight = 5 * 68;
    int calcWidth = 4 * 69;
    int BUTTON_SIZE = 64;
    /**
     * Mode '+' ajout d'une quantité '*' multiplication '-' soustraction ' ' remplacement
     */
    private char mode = ' ';
    private boolean init = true;

    public PaiementPanel(CaisseControler controller) {
        this.controller = controller;
        this.controller.addCaisseListener(this);
        this.controller.addBarcodeListener(this);
        this.setOpaque(false);

        this.addMouseListener(this);

        this.setLayout(null);
        StatusBar st = new StatusBar();
        st.setTitle("Règlement");
        st.setLocation(0, 0);
        st.setSize(320, (int) st.getPreferredSize().getHeight());

        this.add(st);

    }

    @Override
    public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g.setColor(new Color(250, 250, 250));
        g.fillRect(0, 0, this.getWidth(), this.getHeight());

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawLine(0, 0, 0, this.getHeight());
        int y = PAYMENT_POS_Y;
        for (Paiement p : controller.getPaiements()) {
            if (p.getType() == Paiement.CB) {
                drawMontant(g, "CB", p, 242 - 15, y, true);
            } else if (p.getType() == Paiement.CHEQUE) {
                drawMontant(g, "Chèque", p, 242 - 15, y, true);
            } else if (p.getType() == Paiement.ESPECES) {
                drawMontant(g, "Espèces", p, 300 - 45, y, false);
            } else if (p.getType() == Paiement.SOLDE) {
                drawMontant(g, "Solde", p, 300 - 45, y, false);
            }
            y += PAYMENT_LINE_HEIGHT;
        }

        drawKey(g2, "0", 0, 0, 2, 1, CaissePanel.DARK_BLUE);
        drawKey(g2, ".", 2, 0, 1, 1, CaissePanel.DARK_BLUE);

        drawKey(g2, "1", 0, 1, 1, 1, CaissePanel.DARK_BLUE);
        drawKey(g2, "2", 1, 1, 1, 1, CaissePanel.DARK_BLUE);
        drawKey(g2, "3", 2, 1, 1, 1, CaissePanel.DARK_BLUE);
        drawKey(g2, "=", 3, 1, 1, 2, CaissePanel.DARK_BLUE);

        drawKey(g2, "4", 0, 2, 1, 1, CaissePanel.DARK_BLUE);
        drawKey(g2, "5", 1, 2, 1, 1, CaissePanel.DARK_BLUE);
        drawKey(g2, "6", 2, 2, 1, 1, CaissePanel.DARK_BLUE);

        drawKey(g2, "7", 0, 3, 1, 1, CaissePanel.DARK_BLUE);
        drawKey(g2, "8", 1, 3, 1, 1, CaissePanel.DARK_BLUE);
        drawKey(g2, "9", 2, 3, 1, 1, CaissePanel.DARK_BLUE);
        drawKey(g2, "+", 3, 3, 1, 2, CaissePanel.DARK_BLUE);

        drawKey(g2, "C", 0, 4, 2, 1, CaissePanel.LIGHT_BLUE);
        drawKey(g2, "x", 2, 4, 1, 1, CaissePanel.DARK_BLUE);
        drawKey(g2, "-", 3, 4, 1, 1, CaissePanel.DARK_BLUE);

        drawCalculator(g);
        super.paint(g);
    }

    private void drawKey(Graphics2D g2, String string, int col, int row, int w, int h, Color color) {
        // background
        g2.setColor(color);
        g2.fillRect(3 + col * 69, this.getHeight() - (68 * (row + 1)) + 2, 69 * w - 5, 68 * h - 4);
        // label
        g2.setColor(Color.WHITE);
        g2.setFont(g2.getFont().deriveFont(32f));
        final int width2 = (int) g2.getFontMetrics().getStringBounds(string, g2).getWidth();
        int x = -width2 / 2 + (69) / 2 + col * 69;
        int y = this.getHeight() - (row * 68 + 20);

        g2.drawString(string, x, y);
    }

    public int getLCDY() {
        return this.getHeight() - this.calcHeight - 66;
    }

    public int getLCDHeight() {
        return 64;
    }

    private void drawCalculator(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;

        // LCD
        if (controller.getArticleSelected() != null || controller.getPaiementSelected() != null) {
            g.setColor(new Color(232, 242, 254));
        } else {
            g.setColor(new Color(240, 240, 240));
        }
        g.fillRect(3, getLCDY(), this.getWidth() - 5, getLCDHeight());
        //
        int y = this.getHeight() - this.calcHeight - 10;
        g.setFont(new Font("Arial", Font.PLAIN, 32));
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        final Article article = controller.getArticleSelected();
        g.setColor(Color.DARK_GRAY);
        if (article != null) {
            String string = calculatorValue;
            g.setFont(g.getFont().deriveFont(52f));
            Rectangle2D r1 = g.getFontMetrics().getStringBounds(string, g2);
            g.drawString(string, (int) (260 - r1.getWidth()), y);
            g.setFont(g.getFont().deriveFont(14f));
            g.drawString("Quantité", 10, 460 + y);

        } else {
            final Paiement paiement = controller.getPaiementSelected();
            if (paiement != null) {
                String string = calculatorValue;
                g.setFont(g.getFont().deriveFont(52f));
                Rectangle2D r1 = g.getFontMetrics().getStringBounds(string, g2);
                g.drawString(string, (int) (260 - r1.getWidth()), y);
                g.setFont(g.getFont().deriveFont(14f));
                String str = "Paiement ";
                if (paiement.getType() == Paiement.CB) {
                    str += " CB";
                } else if (paiement.getType() == Paiement.ESPECES) {
                    str += " en espèces";
                } else if (paiement.getType() == Paiement.CHEQUE) {
                    str += " par chèque";
                } else if (paiement.getType() == Paiement.SOLDE) {
                    str += " depuis solde";
                }
                g.drawString(str, 10, y - 40);
            }
        }
        g.setFont(g.getFont().deriveFont(14f));
        g.drawString("" + mode, 10, y - 20);
    }

    private char getToucheFrom(int x, int y) {
        int yy = (this.getHeight() - y) / 68;
        int xx = x / 69;
        switch (yy) {
        case 0:
            if (xx == 0) {
                return '0';
            } else if (xx == 1) {
                return '0';
            } else if (xx == 2) {
                return '.';
            } else if (xx == 3) {
                return '=';
            } else {
                break;
            }
        case 1:
            if (xx == 0) {
                return '1';
            } else if (xx == 1) {
                return '2';
            } else if (xx == 2) {
                return '3';
            } else if (xx == 3) {
                return '=';
            } else {
                break;
            }
        case 2:
            if (xx == 0) {
                return '4';
            } else if (xx == 1) {
                return '5';
            } else if (xx == 2) {
                return '6';
            } else if (xx == 3) {
                return '+';
            } else {
                break;
            }
        case 3:
            if (xx == 0) {
                return '7';
            } else if (xx == 1) {
                return '8';
            } else if (xx == 2) {
                return '9';
            } else if (xx == 3) {
                return '+';
            } else {
                break;
            }
        case 4:
            if (xx == 0) {
                return 'c';
            } else if (xx == 1) {
                return 'c';
            } else if (xx == 2) {
                return '*';
            } else if (xx == 3) {
                return '-';
            } else {
                break;
            }

        }
        return '?';
    }

    private void drawMontant(Graphics g, String label, Paiement p, int x, int y, boolean showAdd) {
        y = y + 36;
        Graphics2D g2 = (Graphics2D) g;

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int cents = p.getMontantInCents() % 100;
        int euros = p.getMontantInCents() / 100;
        // Background
        g.setColor(new Color(240, 240, 240));
        g.fillRect(3, y - 36, this.getWidth() - 5, 44);

        g.setColor(CaissePanel.DARK_BLUE);

        g.fillRect(3, y - 36, 95, 44);

        if (showAdd) {
            g.setColor(CaissePanel.DARK_BLUE);
            g.fillRect(this.getWidth() - 46, y - 36, 44, 44);
        }
        // Label
        g.setFont(g.getFont().deriveFont(20f));
        g.setFont(g.getFont().deriveFont(Font.BOLD));

        g.setColor(Color.WHITE);
        g.drawString(label, 10, y - 8);
        if (showAdd) {
            g.drawString("+", this.getWidth() - 32, y - 8);
        }
        // Cents
        g.setColor(Color.GRAY);
        String sCents = String.valueOf(cents);
        if (sCents.length() < 2) {
            sCents = "0" + sCents;
        }
        g.setFont(getFont().deriveFont(18f));
        Rectangle2D r1 = g.getFontMetrics().getStringBounds(sCents, g2);
        g.drawString(sCents, (int) (x - r1.getWidth()), y);
        // Euros
        g.setFont(g.getFont().deriveFont(36f));
        g.setFont(g.getFont().deriveFont(Font.BOLD));
        g.setColor(Color.BLACK);
        String sEuros = String.valueOf(euros) + ".";
        Rectangle2D r2 = g.getFontMetrics().getStringBounds(sEuros, g2);
        g.drawString(sEuros, (int) (x - r1.getWidth() - r2.getWidth()), y);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(calcWidth, 768);
    }

    public Dimension getMinimumSize() {
        return new Dimension(calcWidth, 768);
    }

    @Override
    public void caisseStateChanged() {
        if (controller.getArticleSelected() != null) {
            initCaisseArticle();
        } else if (controller.getPaiementSelected() != null) {
            initCaissePaiement();
        }
        // Add / Remove solde if needed
        boolean soldeIsShown = false;
        for (Paiement p : controller.getPaiements()) {
            if (p.getType() == Paiement.SOLDE) {
                soldeIsShown = true;
                break;
            }
        }

        if (controller.isClientDefined()) {
            if (!soldeIsShown && controller.getClient().getSolde().signum() == 1) {
                // add
                controller.addPaiement(new Paiement(Paiement.SOLDE));
            }
        } else {
            if (soldeIsShown) {
                // remove
                final List<Paiement> paiements = controller.getPaiements();
                for (int i = 0; i < paiements.size(); i++) {
                    final Paiement p = paiements.get(i);
                    if (p.getType() == Paiement.SOLDE) {
                        controller.getPaiements().remove(i);
                        break;
                    }
                }
            }
        }
        repaint();

    }

    private void initCaisseArticle() {
        calculatorValue = String.valueOf(controller.getItemCount(controller.getArticleSelected()));
        init = true;
        mode = ' ';
    }

    private void initCaissePaiement() {
        calculatorValue = TicketCellRenderer.centsToString(controller.getPaiementSelected().getMontantInCents());
        init = true;
        mode = ' ';
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        // Nothing to do here
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        // Nothing to do here
    }

    @Override
    public void mouseExited(MouseEvent e) {
        // Nothing to do here
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getY() > getLCDY() && e.getY() < (getLCDY() + getLCDHeight())) {
            lcdPressed();
            return;
        }

        char c = getToucheFrom(e.getX(), e.getY());
        if (c != '?') {
            handleCharacter(c);
        } else {
            Paiement p = getPaiementFrom(e.getY());
            if (p != null) {
                if (e.getX() > this.getWidth() - 68 && p.getType() != Paiement.ESPECES && controller.canAddPaiement(p.getType())) {
                    p = new Paiement(p.getType());
                    controller.addPaiement(p);
                }
                controller.autoFillPaiement(p);
                this.calculatorValue = TicketCellRenderer.centsToString(p.getMontantInCents());
                if (p.getType() == Paiement.ESPECES) {
                    try {
                        controller.openDrawer();
                    } catch (Throwable ex) {
                        JOptionPane.showMessageDialog(PaiementPanel.this, "Ouverture du tiroir caisse impossible");
                    }
                } else if (p.getType() == Paiement.CB) {
                    controller.sendCBRequest(p);
                }

            }
            controller.setPaiementSelected(p);
        }
    }

    public void lcdPressed() {
        System.err.println("PaiementPanel.lcdPressed()");
        final Article articleSelected = controller.getArticleSelected();
        if (articleSelected != null) {
            controller.openPriceEditor(articleSelected);
        }
        repaint();

    }

    private void handleCharacter(char c) {
        System.out.println("Handle: " + c);
        if (c == '?')
            return;
        final Article article = controller.getArticleSelected();

        if (c == '+' || c == '-' || c == '*') {
            mode = c;

            repaint();
            return;
        }

        if (article != null) {
            // Changement de quantité
            if (c == 'c' || c == '/') {
                System.out.println("Clear quantité");
                mode = ' ';
                controller.clearArticle(article);
            } else if (c == '=' || c == '\n') {
                if (!init) {
                    int v = Integer.parseInt(calculatorValue);
                    if (mode == ' ') {
                        controller.setArticleCount(article, v);
                    } else if (mode == '+') {
                        controller.setArticleCount(article, controller.getItemCount(article) + v);
                    } else if (mode == '-') {
                        controller.setArticleCount(article, controller.getItemCount(article) - v);
                    } else if (mode == '*') {
                        controller.setArticleCount(article, controller.getItemCount(article) * v);
                    }
                }
                initCaisseArticle();
            } else if (Character.isDigit(c)) {
                if (init) {
                    calculatorValue = "";
                    init = false;
                }
                if (calculatorValue.length() < 8) {
                    calculatorValue += c;
                }
            }

        } else {
            final Paiement paiement = controller.getPaiementSelected();
            if (paiement != null) {
                // Changement de paiement
                if (c == 'c' || c == '/') {
                    System.out.println("Clear paiement");
                    mode = ' ';
                    controller.clearPaiement(paiement);
                } else if (c == '.' && (calculatorValue.indexOf('.') < 0)) {
                    calculatorValue += ".";
                } else if (c == '=' || c == '\n') {
                    if (!init) {
                        int v = getCentsFrom(this.calculatorValue);
                        if (mode == ' ') {
                            controller.setPaiementValue(paiement, v);
                        } else if (mode == '+') {
                            controller.setPaiementValue(paiement, paiement.getMontantInCents() + v);
                        } else if (mode == '-') {
                            controller.setPaiementValue(paiement, paiement.getMontantInCents() - v);
                        } else if (mode == '*') {
                            controller.setPaiementValue(paiement, paiement.getMontantInCents() * v);
                        }
                    }
                    initCaissePaiement();
                    controller.setPaiementSelected(null);
                } else if (Character.isDigit(c)) {
                    if (init) {
                        calculatorValue = "";
                        init = false;
                    }
                    if (calculatorValue.length() < 9) {
                        int i = calculatorValue.indexOf('.');
                        if (i < 0 || (calculatorValue.length() - i < 3)) {
                            calculatorValue += c;
                        }
                    }
                }
            }
        }
        repaint();
    }

    private int getCentsFrom(String str) {
        int i = str.indexOf('.');
        if (i >= 0) {
            String euros = str.substring(0, i);
            String cents = str.substring(i + 1);
            if (cents.length() == 1) {
                cents += "0";
            }
            int e = 0;
            if (euros.length() > 0) {
                e = Integer.parseInt(euros);
            }
            int c = 0;
            if (cents.length() > 0) {
                c = Integer.parseInt(cents);
            }
            return e * 100 + c;
        }
        return Integer.parseInt(str) * 100;

    }

    @Override
    public void mouseReleased(MouseEvent e) {
        // Nothing to do here
    }

    @Override
    public void barcodeRead(String code) {
        // Nothing to do here
    }

    private Paiement getPaiementFrom(int y) {
        int index = (y - PAYMENT_POS_Y) / PAYMENT_LINE_HEIGHT;
        if (index < controller.getPaiements().size() && index >= 0) {
            return controller.getPaiements().get(index);
        }
        return null;
    }

    @Override
    public void keyReceived(KeyEvent e) {
        if (e.getID() == KeyEvent.KEY_TYPED) {
            System.out.println("PaiementPanel.keyPressed()" + e.getKeyChar());
            handleCharacter(e.getKeyChar());
        }

    }

}
