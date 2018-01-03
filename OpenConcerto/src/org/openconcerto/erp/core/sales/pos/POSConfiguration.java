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
 
 package org.openconcerto.erp.core.sales.pos;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.config.MainFrame;
import org.openconcerto.erp.core.common.ui.TotalCalculator;
import org.openconcerto.erp.core.finance.accounting.element.ComptePCESQLElement;
import org.openconcerto.erp.core.finance.accounting.element.JournalSQLElement;
import org.openconcerto.erp.core.finance.payment.element.TypeReglementSQLElement;
import org.openconcerto.erp.core.finance.tax.model.TaxeCache;
import org.openconcerto.erp.core.sales.pos.io.TicketPrinter;
import org.openconcerto.erp.core.sales.pos.model.Article;
import org.openconcerto.erp.core.sales.pos.model.Client;
import org.openconcerto.erp.core.sales.pos.model.Paiement;
import org.openconcerto.erp.core.sales.pos.model.ReceiptCode;
import org.openconcerto.erp.core.sales.pos.model.Ticket;
import org.openconcerto.erp.core.sales.pos.model.TicketLine;
import org.openconcerto.erp.core.supplychain.stock.element.StockItemsUpdater;
import org.openconcerto.erp.core.supplychain.stock.element.StockItemsUpdater.TypeStockUpdate;
import org.openconcerto.erp.core.supplychain.stock.element.StockLabel;
import org.openconcerto.erp.generationEcritures.GenerationMvtTicketCaisse;
import org.openconcerto.erp.generationEcritures.GenerationMvtVirement;
import org.openconcerto.erp.generationEcritures.GenerationReglementVenteNG;
import org.openconcerto.erp.model.PrixTTC;
import org.openconcerto.erp.preferences.DefaultNXProps;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLRowValuesListFetcher;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLSelectHandlerBuilder;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.users.UserManager;
import org.openconcerto.sql.utils.SQLUtils;
import org.openconcerto.utils.BaseDirs;
import org.openconcerto.utils.DecimalUtils;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.Pair;
import org.openconcerto.utils.i18n.TranslationManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

public class POSConfiguration {
    private static final String POS_CONFIGURATION_FILENAME = "pos.xml";
    private static POSConfiguration instance;

    private final File confFile;
    private int screenWidth, screenHeight;
    private TicketPrinterConfiguration ticketPrinterConf1, ticketPrinterConf2;
    private int userID = 2;
    private int companyID = 42;
    private int posID = 2;
    private int scanDelay = 80;

    private List<TicketLine> headerLines = new ArrayList<TicketLine>();
    private List<TicketLine> footerLines = new ArrayList<TicketLine>();
    // Terminal CB
    private String creditCardPort = "";
    // LCD
    private String LCDType = "serial";
    private String LCDPort = "";
    private String LCDLine1 = "Bienvenue";
    private String LCDLine2 = "ILM Informatique";

    public static synchronized POSConfiguration getInstance() {
        if (instance == null) {
            instance = new POSConfiguration(getConfigFile(new File(".")));
            instance.loadConfiguration();
        }
        return instance;
    }

    private POSConfiguration(final File confFile) {
        this.confFile = confFile;
        ticketPrinterConf1 = new TicketPrinterConfiguration();
        ticketPrinterConf2 = new TicketPrinterConfiguration();
        // Desactivate second printer by default
        ticketPrinterConf2.setCopyCount(0);
    }

    public TicketPrinterConfiguration getTicketPrinterConfiguration1() {
        return ticketPrinterConf1;
    }

    public TicketPrinterConfiguration getTicketPrinterConfiguration2() {
        return ticketPrinterConf2;
    }

    public boolean isConfigurationFileCreated() {
        File file = getConfigFile();
        if (file == null) {
            return false;
        }
        return file.exists();
    }

    // Screen
    public int getScreenWidth() {
        return screenWidth;
    }

    public int getScreenHeight() {
        return screenHeight;
    }

    // Database connection
    public int getUserID() {
        return userID;
    }

    public void setUserID(int userID) {
        this.userID = userID;
    }

    public int getCompanyID() {
        return companyID;
    }

    public void setCompanyID(int companyID) {
        this.companyID = companyID;
    }

    // POS id
    public int getPosID() {
        return posID;
    }

    public void setPosID(int posID) {
        this.posID = posID;
    }

    public int getScanDelay() {
        return scanDelay;
    }

    /**
     * Set barcode scanner delay
     */
    public void setScanDelay(int ms) {
        this.scanDelay = ms;
    }

    public String getCreditCardPort() {
        return creditCardPort;
    }

    /**
     * Set the serial port of the credit card device
     */
    public void setCreditCardPort(String creditCardPort) {
        this.creditCardPort = creditCardPort;
    }

    private static File getConfigFile(final File wd) {
        final File wdFile = new File(wd + "/Configuration", POS_CONFIGURATION_FILENAME);
        final File confFile;
        if (wdFile.isFile()) {
            confFile = wdFile;
        } else {
            try {
                final File preferencesFolder = BaseDirs.create(ComptaPropsConfiguration.productInfo).getPreferencesFolderToWrite();
                confFile = new File(preferencesFolder, POS_CONFIGURATION_FILENAME);
            } catch (IOException e) {
                throw new IllegalStateException("Couldn't get folder", e);
            }
        }
        return confFile;
    }

    public final File getConfigFile() {
        return this.confFile;
    }

    public void createConnexion() {
        final ComptaPropsConfiguration conf = ComptaPropsConfiguration.create();
        TranslationManager.getInstance().addTranslationStreamFromClass(MainFrame.class);
        TranslationManager.getInstance().setLocale(Locale.getDefault());

        Configuration.setInstance(conf);
        try {
            conf.getBase();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            UserManager.getInstance().setCurrentUser(getUserID());
            final ComptaPropsConfiguration comptaPropsConfiguration = ((ComptaPropsConfiguration) Configuration.getInstance());
            comptaPropsConfiguration.setUpSocieteDataBaseConnexion(getCompanyID());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(new JFrame(), "Impossible de configurer la connexion à la base de donnée.\n ID société: " + getCompanyID() + " \n ID utilisateur: " + getUserID());
            e.printStackTrace();
            System.exit(2);
        }
    }

    public void commitAll(final List<Ticket> tickets) {
        // createConnexion();
        try {
            SQLUtils.executeAtomic(Configuration.getInstance().getSystemRoot().getDataSource(), new SQLUtils.SQLFactory<Object>() {
                @Override
                public Object create() throws SQLException {
                    final int defaultIDClient = getClientCaisse().getID();
                    SQLElement elt = Configuration.getInstance().getDirectory().getElement("TICKET_CAISSE");
                    SQLElement eltFact = Configuration.getInstance().getDirectory().getElement("SAISIE_VENTE_FACTURE_ELEMENT");
                    SQLElement eltEnc = Configuration.getInstance().getDirectory().getElement("ENCAISSER_MONTANT");
                    SQLElement eltMode = Configuration.getInstance().getDirectory().getElement("MODE_REGLEMENT");
                    SQLElement eltArticle = Configuration.getInstance().getDirectory().getElement("ARTICLE");
                    int imported = 0;
                    for (Ticket ticket : tickets) {
                        SQLSelect sel = new SQLSelect(Configuration.getInstance().getBase());
                        sel.addSelect(elt.getTable().getField("NUMERO"));
                        sel.setWhere(new Where(elt.getTable().getField("NUMERO"), "=", ticket.getCode()));
                        List<?> l = Configuration.getInstance().getBase().getDataSource().executeCol(sel.asString());
                        if (l != null && l.size() == 0) {

                            SQLRowValues rowVals = new SQLRowValues(elt.getTable());
                            rowVals.put("NUMERO", ticket.getCode());
                            rowVals.put("DATE", ticket.getCreationDate());
                            rowVals.put("ID_CAISSE", getPosID());
                            int idClient = ticket.getClient().getId();
                            if (idClient <= 0) {
                                idClient = defaultIDClient;
                            }
                            TotalCalculator calc = new TotalCalculator("T_PA_HT", "T_PV_HT", null);

                            String val = DefaultNXProps.getInstance().getStringProperty("ArticleService");
                            Boolean bServiceActive = Boolean.valueOf(val);
                            calc.setServiceActive(bServiceActive != null && bServiceActive);

                            // Articles
                            for (Pair<Article, Integer> item : ticket.getArticles()) {
                                SQLRowValues rowValsElt = new SQLRowValues(eltFact.getTable());
                                final Article article = item.getFirst();
                                final Integer nb = item.getSecond();
                                rowValsElt.put("QTE", nb);
                                rowValsElt.put("PV_HT", article.getPriceWithoutTax());
                                Float tauxFromId = TaxeCache.getCache().getTauxFromId(article.getIdTaxe());
                                BigDecimal tauxTVA = new BigDecimal(tauxFromId).movePointLeft(2).add(BigDecimal.ONE);

                                final BigDecimal valueHT = article.getPriceWithoutTax().multiply(new BigDecimal(nb), DecimalUtils.HIGH_PRECISION);

                                rowValsElt.put("T_PV_HT", valueHT);
                                rowValsElt.put("T_PV_TTC", valueHT.multiply(tauxTVA, DecimalUtils.HIGH_PRECISION));
                                rowValsElt.put("ID_TAXE", article.getIdTaxe());
                                rowValsElt.put("CODE", article.getCode());
                                rowValsElt.put("NOM", article.getName());
                                rowValsElt.put("ID_TICKET_CAISSE", rowVals);
                                rowValsElt.put("ID_ARTICLE", article.getId());
                                calc.addLine(rowValsElt, eltArticle.getTable().getRow(article.getId()), 0, false);
                            }
                            calc.checkResult();
                            long longValueTotalHT = calc.getTotalHT().movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValue();
                            rowVals.put("TOTAL_HT", longValueTotalHT);

                            long longValueTotal = calc.getTotalTTC().movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValue();
                            rowVals.put("TOTAL_TTC", longValueTotal);
                            long longValueTotalTVA = calc.getTotalTVA().movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValue();
                            rowVals.put("TOTAL_TVA", longValueTotalTVA);

                            // Paiements
                            for (Paiement paiement : ticket.getPaiements()) {
                                if (paiement.getMontantInCents() > 0 && paiement.getType() != Paiement.SOLDE) {

                                    SQLRowValues rowValsElt = new SQLRowValues(eltEnc.getTable());
                                    SQLRowValues rowValsEltMode = new SQLRowValues(eltMode.getTable());
                                    if (paiement.getType() == Paiement.CB) {
                                        rowValsEltMode.put("ID_TYPE_REGLEMENT", TypeReglementSQLElement.CB);
                                    } else if (paiement.getType() == Paiement.CHEQUE) {
                                        rowValsEltMode.put("ID_TYPE_REGLEMENT", TypeReglementSQLElement.CHEQUE);
                                    } else if (paiement.getType() == Paiement.ESPECES) {
                                        rowValsEltMode.put("ID_TYPE_REGLEMENT", TypeReglementSQLElement.ESPECE);
                                    }

                                    rowValsElt.put("ID_MODE_REGLEMENT", rowValsEltMode);
                                    rowValsElt.put("ID_CLIENT", idClient);

                                    long montant = Long.valueOf(paiement.getMontantInCents());
                                    if (ticket.getPaiements().size() == 1 && paiement.getType() == Paiement.ESPECES) {
                                        montant = longValueTotal;
                                    }
                                    rowValsElt.put("MONTANT", montant);
                                    rowValsElt.put("NOM", "Ticket " + ticket.getCode());
                                    rowValsElt.put("DATE", ticket.getCreationDate());
                                    rowValsElt.put("ID_TICKET_CAISSE", rowVals);

                                }
                            }

                            SQLRow rowFinal = rowVals.insert();
                            imported++;
                            GenerationMvtTicketCaisse mvt = new GenerationMvtTicketCaisse(rowFinal);
                            final Integer idMvt;
                            try {
                                idMvt = mvt.genereMouvement().call();

                                SQLRowValues valTicket = rowFinal.asRowValues();
                                valTicket.put("ID_MOUVEMENT", Integer.valueOf(idMvt));
                                rowFinal = valTicket.update();

                                // msie à jour du mouvement
                                List<SQLRow> rowsEnc = rowFinal.getReferentRows(eltEnc.getTable());
                                long totalEnc = 0;
                                for (SQLRow sqlRow : rowsEnc) {
                                    long montant = sqlRow.getLong("MONTANT");
                                    PrixTTC ttc = new PrixTTC(montant);
                                    totalEnc += montant;
                                    new GenerationReglementVenteNG(
                                            "Règlement " + sqlRow.getForeignRow("ID_MODE_REGLEMENT").getForeignRow("ID_TYPE_REGLEMENT").getString("NOM") + " Ticket " + rowFinal.getString("NUMERO"),
                                            sqlRow.getForeign("ID_CLIENT"), ttc, sqlRow.getDate("DATE").getTime(), sqlRow.getForeignRow("ID_MODE_REGLEMENT"), rowFinal,
                                            rowFinal.getForeignRow("ID_MOUVEMENT"), false);
                                }
                                if (totalEnc > longValueTotal) {
                                    final SQLTable table = Configuration.getInstance().getDirectory().getElement("TYPE_REGLEMENT").getTable();
                                    int idComptePceCaisse = table.getRow(TypeReglementSQLElement.ESPECE).getInt("ID_COMPTE_PCE_CLIENT");
                                    if (idComptePceCaisse == table.getUndefinedID()) {
                                        idComptePceCaisse = ComptePCESQLElement.getId(ComptePCESQLElement.getComptePceDefault("VenteEspece"));
                                    }
                                    new GenerationMvtVirement(idComptePceCaisse, rowFinal.getForeign("ID_CLIENT").getInt("ID_COMPTE_PCE"), 0, totalEnc - longValueTotal,
                                            "Rendu sur règlement " + " Ticket " + rowFinal.getString("NUMERO"), new Date(), JournalSQLElement.CAISSES, " Ticket " + rowFinal.getString("NUMERO"))
                                                    .genereMouvement();
                                }
                            } catch (Exception exn) {
                                exn.printStackTrace();
                                throw new SQLException(exn);
                            }
                            updateStock(rowFinal.getID());

                        }
                    }
                    // mark imported
                    for (Ticket ticket : tickets) {
                        final ReceiptCode code = ticket.getReceiptCode();
                        try {
                            // it's OK if some files cannot be moved, the next call will try again
                            // (the above code doesn't import duplicates)
                            code.markImported();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    // archive to avoid parsing more and more receipts
                    try {
                        // it's OK if some files cannot be moved, the next call will try again
                        ReceiptCode.archiveCompletelyImported();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    final String count = imported + "/" + tickets.size();
                    SwingUtilities.invokeLater(new Runnable() {

                        @Override
                        public void run() {
                            JOptionPane.showMessageDialog(null, count + " ticket(s) importé(s). Clôture de la caisse terminée.");
                        }
                    });
                    return null;
                }
            });
        } catch (Exception exn) {
            ExceptionHandler.handle("Une erreur est survenue pendant la clôture.", exn);
        }

    }

    private SQLRow rowClient = null;

    private SQLRow getClientCaisse() throws SQLException {
        if (rowClient == null) {
            SQLElement elt = Configuration.getInstance().getDirectory().getElement("CLIENT");
            SQLSelect sel = new SQLSelect();
            sel.addSelectStar(elt.getTable());
            sel.setWhere(new Where(elt.getTable().getField("NOM"), "=", "Caisse OpenConcerto"));
            @SuppressWarnings("unchecked")
            List<SQLRow> l = (List<SQLRow>) elt.getTable().getBase().getDataSource().execute(sel.asString(), new SQLSelectHandlerBuilder(sel).createHandler());
            if (l.size() > 0) {
                rowClient = l.get(0);
            } else {
                SQLRowValues rowValues = new SQLRowValues(elt.getTable());
                rowValues.put("NOM", "Caisse OpenConcerto");
                SQLRowValues rowValuesMode = new SQLRowValues(elt.getTable().getTable("MODE_REGLEMENT"));
                rowValuesMode.put("ID_TYPE_REGLEMENT", TypeReglementSQLElement.CB);
                rowValues.put("ID_MODE_REGLEMENT", rowValuesMode);

                // Select Compte client par defaut
                final SQLBase base = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete();
                final SQLTable tablePrefCompte = base.getTable("PREFS_COMPTE");
                final SQLRow rowPrefsCompte = tablePrefCompte.getRow(2);

                int idDefaultCompteClient = rowPrefsCompte.getInt("ID_COMPTE_PCE_CLIENT");
                if (idDefaultCompteClient <= 1) {
                    try {
                        idDefaultCompteClient = ComptePCESQLElement.getIdComptePceDefault("Clients");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                rowValues.put("ID_COMPTE_PCE", idDefaultCompteClient);
                rowClient = rowValues.insert();
            }
        }
        return rowClient;

    }

    private void updateStock(int id) throws SQLException {
        final SQLRow row = getClientCaisse().getTable().getTable("TICKET_CAISSE").getRow(id);
        StockItemsUpdater stockUpdater = new StockItemsUpdater(new StockLabel() {
            @Override
            public String getLabel(SQLRowAccessor rowOrigin, SQLRowAccessor rowElt) {
                return "Ticket N°" + rowOrigin.getString("NUMERO");
            }
        }, row, row.getReferentRows(getClientCaisse().getTable().getTable("SAISIE_VENTE_FACTURE_ELEMENT")), TypeStockUpdate.REAL_DELIVER);
        stockUpdater.update();
    }

    public List<Ticket> allTickets() {
        final List<Ticket> l = new ArrayList<Ticket>();
        for (final File f : ReceiptCode.getReceiptsToImport(getPosID())) {
            final Ticket ticket = Ticket.parseFile(f);
            if (ticket != null) {
                l.add(ticket);
            }
        }
        return l;
    }

    public List<TicketLine> getHeaderLines() {
        return headerLines;
    }

    public void setHeaderLines(List<TicketLine> headerLines) {
        this.headerLines = headerLines;
    }

    public List<TicketLine> getFooterLines() {
        return footerLines;
    }

    public void setFooterLines(List<TicketLine> footerLines) {
        this.footerLines = footerLines;
    }

    private void loadConfiguration() {
        if (!isConfigurationFileCreated()) {
            System.err.println("POSConfiguration.loadConfigurationFromXML() configuration not loaded. " + getConfigFile().getAbsolutePath() + " missing.");
            return;
        }

        final SAXBuilder builder = new SAXBuilder();
        File file = getConfigFile();

        try {
            System.out.println("POSConfiguration.loadConfigurationFromXML() loading " + file.getAbsolutePath());
            Document document = builder.build(file);
            // config
            final Element rootElement = document.getRootElement();
            setUserID(Integer.valueOf(rootElement.getAttributeValue("userID", "2")));
            setCompanyID(Integer.valueOf(rootElement.getAttributeValue("societeID", "42")));
            setPosID(Integer.valueOf(rootElement.getAttributeValue("caisseID", "2")));
            setScanDelay(Integer.valueOf(rootElement.getAttributeValue("scanDelay", "80")));
            // screen
            final List<Element> children = rootElement.getChildren("screen");
            if (children != null) {
                for (Element e : children) {
                    this.screenWidth = Integer.valueOf(e.getAttributeValue("width", "0"));
                    this.screenHeight = Integer.valueOf(e.getAttributeValue("height", "0"));
                }
            }
            // credit card
            final List<Element> childrenCreditCard = rootElement.getChildren("creditcard");
            if (childrenCreditCard != null) {
                for (Element e : childrenCreditCard) {
                    this.creditCardPort = e.getAttributeValue("port", "");
                }
            }
            // lcd
            final List<Element> childrenLCD = rootElement.getChildren("lcd");
            if (childrenLCD != null) {
                for (Element e : childrenLCD) {
                    this.LCDType = e.getAttributeValue("type", "serial");
                    this.LCDPort = e.getAttributeValue("port", "");
                    this.LCDLine1 = e.getAttributeValue("line1", "");
                    this.LCDLine2 = e.getAttributeValue("line2", "");
                }
            }

            // header
            final List<Element> headers = rootElement.getChildren("header");
            if (headers != null) {
                for (Element header : headers) {
                    this.headerLines.add(new TicketLine(header.getValue(), header.getAttributeValue("style")));
                }
            }
            // footer
            final List<Element> footers = rootElement.getChildren("footer");
            if (footers != null) {
                for (Element header : footers) {
                    this.footerLines.add(new TicketLine(header.getValue(), header.getAttributeValue("style")));
                }
            }
            // ticket printers
            final List<Element> printers = rootElement.getChildren("ticketPrinter");
            if (printers.size() > 0) {
                configureTicketPrinter(this.ticketPrinterConf1, printers.get(0));
            }
            if (printers.size() > 1) {
                configureTicketPrinter(this.ticketPrinterConf2, printers.get(1));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void configureTicketPrinter(TicketPrinterConfiguration conf, Element element) {
        conf.setType(element.getAttributeValue("type"));
        conf.setName(element.getAttributeValue("name"));
        conf.setCopyCount(Integer.parseInt(element.getAttributeValue("copyCount")));
        conf.setTicketWidth(Integer.parseInt(element.getAttributeValue("ticketWidth")));
        conf.setFolder(element.getAttributeValue("folder", ""));
    }

    private Element getElementFromConfiguration(TicketPrinterConfiguration conf) {
        final Element element = new Element("ticketPrinter");
        element.setAttribute("type", conf.getType());
        element.setAttribute("name", conf.getName());
        element.setAttribute("copyCount", String.valueOf(conf.getCopyCount()));
        element.setAttribute("ticketWidth", String.valueOf(conf.getTicketWidth()));
        element.setAttribute("folder", conf.getFolder());
        return element;
    }

    public void saveConfiguration() {
        final File file = getConfigFile();
        final XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
        try {
            System.out.println("Saving:" + file.getAbsolutePath());
            final FileOutputStream fileOutputStream = new FileOutputStream(file);
            final Document doc = new Document();
            final Element configElement = new Element("config");
            configElement.setAttribute("userID", String.valueOf(this.userID));
            configElement.setAttribute("societeID", String.valueOf(this.companyID));
            configElement.setAttribute("caisseID", String.valueOf(this.posID));
            configElement.setAttribute("scanDelay", String.valueOf(this.scanDelay));
            doc.addContent(configElement);
            // screen size
            final Element screenElement = new Element("screen");
            screenElement.setAttribute("width", String.valueOf(this.screenWidth));
            screenElement.setAttribute("height", String.valueOf(this.screenHeight));
            configElement.addContent(screenElement);
            // credit card
            final Element creditCardElement = new Element("creditcard");
            creditCardElement.setAttribute("port", this.creditCardPort);
            configElement.addContent(creditCardElement);
            // LCD
            final Element lcdElement = new Element("lcd");
            lcdElement.setAttribute("type", this.LCDType);
            lcdElement.setAttribute("port", this.LCDPort);
            lcdElement.setAttribute("line1", this.LCDLine1);
            lcdElement.setAttribute("line2", this.LCDLine2);
            configElement.addContent(lcdElement);

            // header
            for (TicketLine line : this.headerLines) {
                Element e = new Element("header");
                final String style = line.getStyle();
                if (style != null && !style.isEmpty()) {
                    e.setAttribute("style", style);
                }
                e.setText(line.getText());
                configElement.addContent(e);
            }
            // footer
            for (TicketLine line : this.footerLines) {
                Element e = new Element("footer");
                final String style = line.getStyle();
                if (style != null && !style.isEmpty()) {
                    e.setAttribute("style", style);
                }
                e.setText(line.getText());
                configElement.addContent(e);
            }
            // ticket printer
            configElement.addContent(getElementFromConfiguration(this.ticketPrinterConf1));
            configElement.addContent(getElementFromConfiguration(this.ticketPrinterConf2));
            outputter.output(doc, fileOutputStream);
            fileOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
            ExceptionHandler.handle("Erreur lors de la sauvegarde de la configuration de la caisse.\n" + file.getAbsolutePath());
        }

    }

    public void print(Ticket ticket) {
        print(ticket, this.ticketPrinterConf1);
        print(ticket, this.ticketPrinterConf2);
    }

    public void print(Ticket ticket, TicketPrinterConfiguration conf) {
        if (conf.isValid() && conf.getCopyCount() > 0) {
            final TicketPrinter prt = conf.createTicketPrinter();
            for (int i = 0; i < conf.getCopyCount(); i++) {
                ticket.print(prt, conf.getTicketWidth());
            }
        }
    }

    public boolean isUsingJPos() {
        // TODO Auto-generated method stub
        return false;
    }

    public List<String> getJPosDirectories() {// TODO Auto-generated method stub
        final ArrayList<String> result = new ArrayList<String>();
        return result;
    }

    public String getLCDPort() {
        return LCDPort;
    }

    public void setLCDPort(String port) {
        this.LCDPort = port;
    }

    public String getLCDLine1() {
        return this.LCDLine1;
    }

    public void setLCDLine1(String text) {
        this.LCDLine1 = text;
    }

    public String getLCDLine2() {
        return this.LCDLine2;
    }

    public void setLCDLine2(String text) {
        this.LCDLine2 = text;
    }

    public List<Client> allClients() {
        SQLElement elt = Configuration.getInstance().getDirectory().getElement("CLIENT");
        SQLRowValues r = new SQLRowValues(elt.getTable());
        r.putNulls("NOM", "SOLDE_COMPTE");
        SQLRowValues rAdresse = new SQLRowValues(Configuration.getInstance().getDirectory().getElement("ADRESSE").getTable());
        rAdresse.putNulls("RUE", "VILLE");
        r.put("ID_ADRESSE", rAdresse);
        SQLRowValuesListFetcher f = new SQLRowValuesListFetcher(r);
        List<SQLRowValues> result = f.fetch();
        List<Client> l = new ArrayList<Client>();

        for (SQLRowValues sqlRowValues : result) {
            Client c = new Client(sqlRowValues.getID(), sqlRowValues.getString("NOM"), sqlRowValues.getBigDecimal("SOLDE_COMPTE"));
            final SQLRowAccessor foreign = sqlRowValues.getForeign("ID_ADRESSE");
            c.setAdresse(foreign.getString("RUE") + " " + foreign.getString("VILLE"));
            l.add(c);
        }
        Collections.sort(l, new Comparator<Client>() {

            @Override
            public int compare(Client o1, Client o2) {
                return o1.getFullName().compareToIgnoreCase(o2.getFullName());
            }
        });
        l.add(0, Client.NONE);
        return l;
    }

    public void setLCDType(String type) {
        this.LCDType = type;

    }

    public String getLCDType() {
        return this.LCDType;
    }
}
