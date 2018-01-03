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
import org.openconcerto.erp.core.sales.pos.io.BarcodeReader;
import org.openconcerto.erp.core.sales.pos.io.ConcertProtocol;
import org.openconcerto.erp.core.sales.pos.io.ESCSerialDisplay;
import org.openconcerto.erp.core.sales.pos.io.TicketPrinter;
import org.openconcerto.erp.core.sales.pos.model.Article;
import org.openconcerto.erp.core.sales.pos.model.Client;
import org.openconcerto.erp.core.sales.pos.model.Paiement;
import org.openconcerto.erp.core.sales.pos.model.Ticket;
import org.openconcerto.utils.Pair;

import java.awt.event.KeyEvent;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JOptionPane;

public class CaisseControler implements BarcodeListener {

    private Article articleSelected;
    private Paiement paiementSelected;
    private Ticket t;
    private Client client = Client.NONE;
    private List<CaisseListener> listeners = new ArrayList<CaisseListener>();

    private final BarcodeReader r;
    private Paiement p1 = new Paiement(Paiement.ESPECES);
    private Paiement p2 = new Paiement(Paiement.CB);
    private Paiement p3 = new Paiement(Paiement.CHEQUE);
    private final CaisseFrame caisseFrame;
    private final POSDisplay lcd;

    public CaisseControler(CaisseFrame caisseFrame) {
        this.caisseFrame = caisseFrame;
        this.t = new Ticket(POSConfiguration.getInstance().getPosID());

        this.t.addPaiement(this.p1);
        this.t.addPaiement(this.p2);
        this.t.addPaiement(this.p3);

        this.r = new BarcodeReader(POSConfiguration.getInstance().getScanDelay());
        this.r.start();
        this.r.addBarcodeListener(this);
        if (POSConfiguration.getInstance().getLCDType().equals("serial")) {
            lcd = new ESCSerialDisplay(POSConfiguration.getInstance().getLCDPort());
        } else {
            lcd = new PrinterPOSDisplay(POSConfiguration.getInstance().getLCDPort());
        }
        this.setLCDDefaultDisplay(0);
    }

    public Article getArticleSelected() {
        return this.articleSelected;
    }

    public Paiement getPaiementSelected() {
        return this.paiementSelected;
    }

    void setArticleSelected(Article a) {
        if (a != articleSelected) {
            this.articleSelected = a;
            this.paiementSelected = null;
            fire();
        }
    }

    void setPaiementSelected(Paiement p) {
        this.paiementSelected = p;
        this.articleSelected = null;
        fire();
    }

    // Listeners
    private void fire() {
        int stop = this.listeners.size();
        for (int i = 0; i < stop; i++) {
            this.listeners.get(i).caisseStateChanged();
        }
    }

    void addCaisseListener(CaisseListener l) {
        this.listeners.add(l);
    }

    // Customer
    public void setClient(Client client) {
        if (client == null) {
            throw new IllegalArgumentException("Customer cannot be null");
        }
        this.client = client;
        this.t.setClient(client);
        fire();
    }

    public boolean isClientDefined() {
        return this.client != Client.NONE;
    }

    public Client getClient() {
        return client;
    }

    // Articles
    void addArticle(Article a) {
        this.t.addArticle(a);
        fire();
        String price = TicketCellRenderer.centsToString(a.getPriceWithTax().movePointRight(2).setScale(0, RoundingMode.HALF_UP).intValue());
        this.setLCD(a.getName(), price, 0);
        this.setLCDDefaultDisplay(2);
    }

    void incrementArticle(Article a) {
        this.t.incrementArticle(a);
        fire();
        String price = TicketCellRenderer.centsToString(a.getPriceWithTax().movePointRight(2).setScale(0, RoundingMode.HALF_UP).intValue());
        this.setLCD(a.getName(), price, 0);
        this.setLCDDefaultDisplay(2);
    }

    void removeArticle(Article a) {
        this.t.removeArticle(a);
        fire();
    }

    // Paiements
    public List<Paiement> getPaiements() {
        return this.t.getPaiements();
    }

    public void addPaiement(Paiement p) {
        this.t.addPaiement(p);
        fire();
    }

    public void clearPaiement(Paiement paiement) {
        if (this.p1.equals(paiement) || this.p2.equals(paiement) || this.p3.equals(paiement)) {
            paiement.setMontantInCents(0);
        }
        fire();
    }

    public void setPaiementValue(Paiement p, int v) {
        if (p.getType() == Paiement.SOLDE) {
            int soldeInCents = getClient().getSolde().movePointRight(2).intValue();
            if (v > soldeInCents) {
                v = soldeInCents;
            }
        }
        p.setMontantInCents(v);
        fire();
        this.setLCD("Paiement " + p.getTypeAsString().replace('è', 'e').replace('é', 'e'), TicketCellRenderer.centsToString(p.getMontantInCents()), 0);
        this.setLCDDefaultDisplay(3);
    }

    // Totaux
    public int getTotal() {
        return this.t.getTotalInCents();
    }

    public int getPaidTotal() {
        return this.t.getPaidTotal();
    }

    //

    public List<Pair<Article, Integer>> getItems() {
        return this.t.getArticles();
    }

    public int getItemCount(Article article) {
        return this.t.getItemCount(article);
    }

    public void clearArticle(Article article) {
        this.t.clearArticle(article);
        this.setArticleSelected(null);
    }

    public void setArticleCount(Article article, int count) {
        this.t.setArticleCount(article, count);
        this.setArticleSelected(null);
    }

    @Override
    public void barcodeRead(String code) {
        System.err.println("CaisseControler.barcodeRead() " + code);
        if (code.equalsIgnoreCase("especes")) {
            autoFillPaiement(this.p1);

        } else if (code.equalsIgnoreCase("cb")) {
            autoFillPaiement(this.p2);

        } else if (code.equalsIgnoreCase("cheque")) {
            autoFillPaiement(this.p3);

        } else if (code.equalsIgnoreCase("annuler")) {
            if (this.articleSelected != null) {
                this.clearArticle(this.articleSelected);

            } else {
                if (this.paiementSelected != null) {
                    this.paiementSelected.setMontantInCents(0);
                    // setPaiementSelected(null);
                    fire();
                }

            }
        } else if (code.equalsIgnoreCase("valider")) {

        } else if (code.equalsIgnoreCase("facture")) {

        } else if (code.equalsIgnoreCase("ticket")) {

        } else {
            Article a = Article.getArticleFromBarcode(code);
            if (a != null) {
                this.incrementArticle(a);
                this.setArticleSelected(a);
            } else {
                Ticket t = Ticket.getTicketFromCode(code);
                if (t != null) {
                    caisseFrame.showTickets(t);
                }
            }
        }

    }

    void autoFillPaiement(Paiement p) {
        int nouveauMontant = getTotal() - getPaidTotal() + p.getMontantInCents();
        if (p.getType() == Paiement.SOLDE) {
            int soldeInCents = getClient().getSolde().movePointRight(2).intValue();
            if (nouveauMontant > soldeInCents) {
                nouveauMontant = soldeInCents;
            }
        }
        p.setMontantInCents(nouveauMontant);
        setPaiementSelected(p);
        this.setLCD("Paiement " + p.getTypeAsString(), TicketCellRenderer.centsToString(p.getMontantInCents()), 0);
        this.setLCDDefaultDisplay(3);
    }

    void addBarcodeListener(BarcodeListener l) {
        this.r.addBarcodeListener(l);
    }

    public boolean canAddPaiement(int type) {
        final int paiementCount = this.t.getPaiements().size();
        if (paiementCount >= 6)
            return false;
        for (int i = 0; i < paiementCount; i++) {
            Paiement p = this.t.getPaiements().get(i);
            if (p.getType() == type && p.getMontantInCents() <= 0) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void keyReceived(KeyEvent ee) {

    }

    public static String getCents(int cents) {
        String s = String.valueOf(cents % 100);
        if (s.length() < 2) {
            s = "0" + s;
        }
        return s;
    }

    public static String getEuros(int cents) {
        String s = String.valueOf(cents / 100);

        return s;
    }

    public void saveAndClearTicket() {
        if (this.t.getTotalInCents() > 0) {
            if (this.getPaidTotal() >= this.getTotal()) {
                this.t.setCreationCal(Calendar.getInstance());
                this.t.save();
                t = new Ticket(POSConfiguration.getInstance().getPosID());
                p1 = new Paiement(Paiement.ESPECES);
                p2 = new Paiement(Paiement.CB);
                p3 = new Paiement(Paiement.CHEQUE);
                this.t.addPaiement(this.p1);
                this.t.addPaiement(this.p2);
                this.t.addPaiement(this.p3);
                this.setPaiementSelected(null);
                this.setArticleSelected(null);
                client = Client.NONE;
            }
        }
    }

    public int getTicketNumber() {
        return this.t.getNumber();
    }

    public void printTicket() {
        if (this.t.getTotalInCents() > 0) {
            if (this.getPaidTotal() >= this.getTotal()) {
                POSConfiguration.getInstance().print(this.t);
            } else {
                System.err.println("Ticket not printed because not paid");
            }
        } else {
            System.err.println("Ticket not printed total <= 0");
        }
    }

    public void openDrawer() {
        try {
            final TicketPrinter prt = POSConfiguration.getInstance().getTicketPrinterConfiguration1().createTicketPrinter();
            prt.openDrawer();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void switchListMode() {
        caisseFrame.mainPanel.switchListMode();

    }

    public void setLCD(final String line1, final String line2, final int delay) {
        final TimerTask task = new TimerTask() {
            @Override
            public void run() {
                try {
                    lcd.setMessage(line1, line2);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        final Timer timer = new Timer("LCD : " + line1, true);
        timer.schedule(task, delay * 1000);

    }

    public void setLCDDefaultDisplay(int delay) {
        if (t.getTotalInCents() > 0) {
            int count = 0;
            final List<Pair<Article, Integer>> articles = t.getArticles();
            for (Pair<Article, Integer> pair : articles) {
                count += pair.getSecond();
            }
            String line1;
            if (count == 1) {
                line1 = "1 article";
            } else {
                line1 = count + " articles";
            }
            int cents = t.getTotalInCents();
            setLCD(line1, "Total : " + TicketCellRenderer.centsToString(cents), delay);
        } else {
            setLCD(POSConfiguration.getInstance().getLCDLine1(), POSConfiguration.getInstance().getLCDLine2(), delay);
        }
    }

    public void sendCBRequest(final Paiement p) {

        final String creditCardPort = POSConfiguration.getInstance().getCreditCardPort();
        if (creditCardPort != null && creditCardPort.trim().length() > 2) {
            final Thread t = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        ConcertProtocol cp = new ConcertProtocol(creditCardPort);
                        boolean ok = cp.sendCardPayment(p.getMontantInCents(), ConcertProtocol.CURRENCY_EUR);
                        if (ok) {
                            JOptionPane.showMessageDialog(null, "Paiement CB OK");
                        } else {
                            JOptionPane.showMessageDialog(null, "Erreur paiement CB");
                        }
                    } catch (Throwable ex) {
                        JOptionPane.showMessageDialog(null, "Erreur terminal CB");
                    }
                }
            });
            t.setDaemon(true);
            t.start();

        }

    }

    public void setArticleHT(Article article, BigDecimal ht) {
        Article existingArticle = null;
        final List<Pair<Article, Integer>> articles = t.getArticles();
        for (Pair<Article, Integer> pair : articles) {
            final Article a = pair.getFirst();
            if (a.getId() == article.getId()) {
                if (article.getPriceWithoutTax().doubleValue() == a.getPriceWithoutTax().doubleValue()) {
                    existingArticle = a;
                    break;
                }
            }
        }
        if (existingArticle != null) {
            existingArticle.updatePriceWithoutTax(ht);
            fire();
        }
    }

    public void openPriceEditor(Article article) {
        caisseFrame.showPriceEditor(article, this);
    }

    public void enableBarcodeReader() {
        this.r.setEnabled(true);
    }

    public void disableBarcodeReader() {
        this.r.setEnabled(false);
    }
}
