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
 
 package org.openconcerto.erp.core.sales.invoice.component;

import static org.openconcerto.utils.CollectionUtils.createSet;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.common.component.TransfertBaseSQLComponent;
import org.openconcerto.erp.core.common.element.BanqueSQLElement;
import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.erp.core.common.element.NumerotationAutoSQLElement;
import org.openconcerto.erp.core.common.ui.AbstractArticleItemTable;
import org.openconcerto.erp.core.common.ui.DeviseField;
import org.openconcerto.erp.core.common.ui.TotalPanel;
import org.openconcerto.erp.core.customerrelationship.customer.ui.AdresseType;
import org.openconcerto.erp.core.finance.accounting.element.ComptePCESQLElement;
import org.openconcerto.erp.core.finance.accounting.element.EcritureSQLElement;
import org.openconcerto.erp.core.finance.payment.component.ModeDeReglementSQLComponent;
import org.openconcerto.erp.core.finance.tax.model.TaxeCache;
import org.openconcerto.erp.core.sales.invoice.element.SaisieVenteFactureSQLElement;
import org.openconcerto.erp.core.sales.invoice.element.SaisieVenteFactureSQLElement.DoWithRow;
import org.openconcerto.erp.core.sales.invoice.report.VenteFactureXmlSheet;
import org.openconcerto.erp.core.sales.invoice.ui.SaisieVenteFactureItemTable;
import org.openconcerto.erp.core.supplychain.stock.element.StockItemsUpdater;
import org.openconcerto.erp.core.supplychain.stock.element.StockItemsUpdater.TypeStockUpdate;
import org.openconcerto.erp.core.supplychain.stock.element.StockLabel;
import org.openconcerto.erp.generationEcritures.GenerationMvtSaisieVenteFacture;
import org.openconcerto.erp.model.BanqueModifiedListener;
import org.openconcerto.erp.model.ISQLCompteSelector;
import org.openconcerto.erp.panel.PanelOOSQLComponent;
import org.openconcerto.erp.preferences.DefaultNXProps;
import org.openconcerto.erp.preferences.GestionArticleGlobalPreferencePanel;
import org.openconcerto.erp.preferences.GestionClientPreferencePanel;
import org.openconcerto.erp.preferences.GestionCommercialeGlobalPreferencePanel;
import org.openconcerto.erp.preferences.ModeReglementDefautPrefPanel;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.ElementSQLObject;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLBackgroundTableCache;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLInjector;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.UndefinedRowValuesCache;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.preferences.SQLPreferences;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.sql.sqlobject.JUniqueTextField;
import org.openconcerto.sql.sqlobject.SQLRequestComboBox;
import org.openconcerto.sql.sqlobject.SQLTextCombo;
import org.openconcerto.sql.users.UserManager;
import org.openconcerto.sql.view.EditFrame;
import org.openconcerto.sql.view.EditPanel.EditMode;
import org.openconcerto.sql.view.list.RowValuesTable;
import org.openconcerto.sql.view.list.RowValuesTableModel;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.FormLayouter;
import org.openconcerto.ui.JDate;
import org.openconcerto.ui.TitledSeparator;
import org.openconcerto.ui.component.ITextArea;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.GestionDevise;
import org.openconcerto.utils.ProductInfo;
import org.openconcerto.utils.cc.IFactory;
import org.openconcerto.utils.text.SimpleDocumentListener;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;

public class SaisieVenteFactureSQLComponent extends TransfertBaseSQLComponent {
    private AbstractArticleItemTable tableFacture;
    private JLabel labelAffaire = new JLabel("Affaire");
    private final JDate dateSaisie = new JDate(true);
    private DeviseField textPortHT, textAvoirTTC, textRemiseHT, fieldTTC, textNetAPayer;
    private DeviseField totalTimbre, netPayer;
    private JTextField tauxTimbre;

    private SQLElement factureElt = Configuration.getInstance().getDirectory().getElement("SAISIE_VENTE_FACTURE");
    private SQLTable tableAvoir = Configuration.getInstance().getDirectory().getElement("AVOIR_CLIENT").getTable();
    public static final SQLTable TABLE_ADRESSE = Configuration.getInstance().getDirectory().getElement("ADRESSE").getTable();
    private SQLTable tableClient = ((ComptaPropsConfiguration) Configuration.getInstance()).getRootSociete().getTable("CLIENT");
    private final SQLElement client = Configuration.getInstance().getDirectory().getElement(this.tableClient);
    private JUniqueTextField textNumeroUnique;
    private ElementComboBox comboClient;
    private ISQLCompteSelector compteSel;
    private final SQLTable tableNum = this.factureElt.getTable().getBase().getTable("NUMEROTATION_AUTO");
    private JCheckBox checkCompteServiceAuto, checkPrevisionnelle, checkComplement, checkAcompte, checkCT;

    protected PanelOOSQLComponent panelOO;
    private ElementComboBox selAvoir, selAffaire;
    private ElementSQLObject eltModeRegl;
    private static final SQLBase base = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete();
    private static final SQLTable tablePrefCompte = base.getTable("PREFS_COMPTE");
    private static final SQLRow rowPrefsCompte = SQLBackgroundTableCache.getInstance().getCacheForTable(tablePrefCompte).getRowFromId(2);
    private ElementComboBox contact;
    private SQLRowAccessor rowSelected;
    private SQLElement eltContact = Configuration.getInstance().getDirectory().getElement("CONTACT");
    private JTextField refClient = new JTextField();

    protected TotalPanel totalTTC;
    private final boolean displayDpt;
    private final ElementComboBox comboDpt = new ElementComboBox();
    private final boolean gestionTimbre;

    // Type intervention
    private SQLTextCombo textTypeMission = new SQLTextCombo();

    private PropertyChangeListener listenerModeReglDefaut = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent arg0) {
            int idCli = SaisieVenteFactureSQLComponent.this.comboClient.getWantedID();
            if (idCli > 1) {
                SQLRow rowCli = SaisieVenteFactureSQLComponent.this.client.getTable().getRow(idCli);
                if (!rowCli.isForeignEmpty("ID_COMMERCIAL")) {
                    comboCommercial.setValue(rowCli.getForeignID("ID_COMMERCIAL"));
                }
                if (getMode() == SQLComponent.Mode.INSERTION || !isFilling()) {
                    SQLElement sqleltModeRegl = Configuration.getInstance().getDirectory().getElement("MODE_REGLEMENT");
                    int idModeRegl = rowCli.getInt("ID_MODE_REGLEMENT");
                    if (idModeRegl > 1) {
                        SQLRow rowModeRegl = sqleltModeRegl.getTable().getRow(idModeRegl);
                        SQLRowValues rowValsModeRegl = rowModeRegl.createUpdateRow();
                        rowValsModeRegl.clearPrimaryKeys();
                        SaisieVenteFactureSQLComponent.this.eltModeRegl.setValue(rowValsModeRegl);
                    }
                }
            }

            Where w = new Where(SaisieVenteFactureSQLComponent.this.tableAvoir.getField("SOLDE"), "=", Boolean.FALSE);
            if (SaisieVenteFactureSQLComponent.this.comboClient.isEmpty()) {
                w = w.and(new Where(getTable().getBase().getTable("AVOIR_CLIENT").getField("ID_CLIENT"), "=", -1));
            } else {
                w = w.and(new Where(getTable().getBase().getTable("AVOIR_CLIENT").getField("ID_CLIENT"), "=", idCli));
            }
            if (getSelectedID() > 1) {
                SQLRow row = getTable().getRow(getSelectedID());
                w = w.or(new Where(SaisieVenteFactureSQLComponent.this.tableAvoir.getKey(), "=", row.getInt("ID_AVOIR_CLIENT")));
            }

            SaisieVenteFactureSQLComponent.this.selAvoir.getRequest().setWhere(w);
            SaisieVenteFactureSQLComponent.this.selAvoir.fillCombo();
        }
    };

    private PropertyChangeListener changeCompteListener;
    private PropertyChangeListener changeClientListener;
    private ISQLCompteSelector compteSelService;
    private JLabel labelCompteServ;
    private ElementComboBox comboCommercial;
    private ElementComboBox comboVerificateur = new ElementComboBox();;
    private SQLTable tableBanque = getTable().getTable(BanqueSQLElement.TABLENAME);

    private final SQLRowAccessor defaultNum;

    public SaisieVenteFactureSQLComponent(int defaultNum) {
        super(Configuration.getInstance().getDirectory().getElement("SAISIE_VENTE_FACTURE"));
        this.defaultNum = this.tableNum.getRow(defaultNum);
        SQLPreferences prefs = SQLPreferences.getMemCached(getTable().getDBRoot());
        this.displayDpt = prefs.getBoolean(GestionClientPreferencePanel.DISPLAY_CLIENT_DPT, false);
        this.gestionTimbre = prefs.getBoolean(GestionCommercialeGlobalPreferencePanel.GESTION_TIMBRE_FISCAL, false);
    }

    public SaisieVenteFactureSQLComponent() {
        this(2);

    }


    private int previousClient = -1;
    private ElementComboBox comboNumAuto = null;

    public void addViews() {
        this.setLayout(new GridBagLayout());

        if (getTable().contains("CREATE_VIRTUAL_STOCK")) {
            this.addView(new JCheckBox(), "CREATE_VIRTUAL_STOCK");
        }
        final GridBagConstraints c = new DefaultGridBagConstraints();

        if (getTable().contains("ID_NUMEROTATION_AUTO")) {
            this.comboNumAuto = new ElementComboBox();
            this.addView(this.comboNumAuto, "ID_NUMEROTATION_AUTO");
        }

        this.checkPrevisionnelle = new JCheckBox();
        this.checkComplement = new JCheckBox();
        this.fieldTTC = new DeviseField();

        final ComptaPropsConfiguration comptaPropsConfiguration = ((ComptaPropsConfiguration) Configuration.getInstance());

        this.textAvoirTTC = new DeviseField();

        // Champ Module
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = GridBagConstraints.REMAINDER;
        final JPanel addP = ComptaSQLConfElement.createAdditionalPanel();
        this.setAdditionalFieldsPanel(new FormLayouter(addP, 1));
        this.add(addP, c);

        c.gridy++;
        c.gridwidth = 1;


            if (getTable().contains("ID_POLE_PRODUIT")) {
                JLabel labelPole = new JLabel(getLabelFor("ID_POLE_PRODUIT"));
                labelPole.setHorizontalAlignment(SwingConstants.RIGHT);
                this.add(labelPole, c);
                c.gridx++;
                ElementComboBox pole = new ElementComboBox();

                this.add(pole, c);
                this.addSQLObject(pole, "ID_POLE_PRODUIT");
                c.gridy++;
                c.gridwidth = 1;
                c.gridx = 0;
            }

        /*******************************************************************************************
         * * RENSEIGNEMENTS
         ******************************************************************************************/
        // Ligne 1 : Numero de facture
        JLabel labelNum = new JLabel(getLabelFor("NUMERO"));
        labelNum.setHorizontalAlignment(SwingConstants.RIGHT);
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        this.add(labelNum, c);

            this.textNumeroUnique = new JUniqueTextField(16) {
                @Override
                public String getAutoRefreshNumber() {
                    if (getMode() == Mode.INSERTION) {
                        return NumerotationAutoSQLElement.getNextNumero(getElement().getClass(), dateSaisie.getDate());
                    } else {
                        return null;
                    }
                }
            };
        c.gridx++;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        DefaultGridBagConstraints.lockMinimumSize(this.textNumeroUnique);
            this.add(textNumeroUnique, c);

        // Date
        c.gridx++;
        c.weightx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.add(new JLabel(getLabelFor("DATE"), SwingConstants.RIGHT), c);

        c.gridx++;
        c.weightx = 1;
        c.fill = GridBagConstraints.NONE;

        // listener permettant la mise à jour du numéro de facture en fonction de la date
        // sélectionnée
        dateSaisie.addValueListener(new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (!isFilling() && dateSaisie.getValue() != null) {

                    final String nextNumero = NumerotationAutoSQLElement.getNextNumero(SaisieVenteFactureSQLElement.class, dateSaisie.getValue(), defaultNum);

                    if (textNumeroUnique.getText().trim().length() > 0 && !nextNumero.equalsIgnoreCase(textNumeroUnique.getText())) {

                        int answer = JOptionPane.showConfirmDialog(SaisieVenteFactureSQLComponent.this, "Voulez vous actualiser le numéro de la facture?", "Changement du numéro de facture",
                                JOptionPane.YES_NO_OPTION);
                        if (answer == JOptionPane.NO_OPTION) {
                            return;
                        }
                    }

                    textNumeroUnique.setText(nextNumero);
                    tableFacture.setDateDevise(dateSaisie.getValue());
                }
            }
        });
        this.add(dateSaisie, c);

        // Ligne 2 : reference
        c.gridx = 0;
        c.gridwidth = 1;
        c.gridy++;
        c.weightx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;

        JLabel labelLibelle = new JLabel(getLabelFor("NOM"));
        labelLibelle.setHorizontalAlignment(SwingConstants.RIGHT);
        this.add(labelLibelle, c);

        SQLTextCombo textLibelle = new SQLTextCombo();
        c.gridx++;
        c.weightx = 1;
        c.fill = GridBagConstraints.BOTH;
        this.add(textLibelle, c);

        this.addSQLObject(textLibelle, "NOM");
        c.fill = GridBagConstraints.HORIZONTAL;
        this.comboCommercial = new ElementComboBox(false);
        // Commercial
        String field;
            field = "ID_COMMERCIAL";
        c.gridx++;
        c.weightx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.add(new JLabel(getLabelFor(field), SwingConstants.RIGHT), c);

        c.gridx++;
        c.weightx = 1;
        c.fill = GridBagConstraints.NONE;

        this.add(this.comboCommercial, c);
        this.addRequiredSQLObject(this.comboCommercial, field);
        // Client
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.add(new JLabel(getLabelFor("ID_CLIENT"), SwingConstants.RIGHT), c);

        c.gridx++;
        c.weightx = 1;
        c.fill = GridBagConstraints.NONE;
        this.comboClient = new ElementComboBox();

        this.add(this.comboClient, c);
        this.addRequiredSQLObject(this.comboClient, "ID_CLIENT");

        if (this.displayDpt) {
            c.gridx++;
            c.gridwidth = 1;
            final JLabel labelDpt = new JLabel(getLabelFor("ID_CLIENT_DEPARTEMENT"));
            labelDpt.setHorizontalAlignment(SwingConstants.RIGHT);
            c.weightx = 0;
            c.gridwidth = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            this.add(labelDpt, c);

            c.gridx++;
            c.gridwidth = 1;
            c.weightx = 0;
            c.weighty = 0;
            c.fill = GridBagConstraints.NONE;
            this.add(this.comboDpt, c);
            DefaultGridBagConstraints.lockMinimumSize(this.comboDpt);
            addSQLObject(this.comboDpt, "ID_CLIENT_DEPARTEMENT");

            comboClient.addModelListener("wantedID", new PropertyChangeListener() {

                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    int wantedID = comboClient.getWantedID();

                    if (wantedID != SQLRow.NONEXISTANT_ID && wantedID >= SQLRow.MIN_VALID_ID) {
                        final SQLRow rowClient = getTable().getForeignTable("ID_CLIENT").getRow(wantedID);
                        comboDpt.getRequest().setWhere(new Where(comboDpt.getRequest().getPrimaryTable().getField("ID_CLIENT"), "=", rowClient.getID()));
                    } else {
                        comboDpt.getRequest().setWhere(null);
                    }
                }
            });

        }

        if (

        getTable().contains("ID_ECHEANCIER_CCI")) {
            // Echeancier
            c.gridy++;
            c.gridx = 0;
            c.weightx = 0;
            c.fill = GridBagConstraints.HORIZONTAL;
            this.add(new JLabel(getLabelFor("ID_ECHEANCIER_CCI"), SwingConstants.RIGHT), c);

            c.gridx++;
            c.weightx = 1;
            c.fill = GridBagConstraints.NONE;
            final ElementComboBox echeancier = new ElementComboBox();
            final SQLElement contactElement = Configuration.getInstance().getDirectory().getElement("ECHEANCIER_CCI");
            echeancier.init(contactElement, contactElement.getComboRequest(true));
            DefaultGridBagConstraints.lockMinimumSize(echeancier);
            this.addView(echeancier, "ID_ECHEANCIER_CCI");

            selAffaire.addValueListener(new PropertyChangeListener() {

                @Override
                public void propertyChange(PropertyChangeEvent arg0) {
                    // TODO Raccord de méthode auto-généré
                    if (selAffaire.getSelectedRow() != null) {
                        echeancier.getRequest().setWhere(new Where(contactElement.getTable().getField("ID_AFFAIRE"), "=", selAffaire.getSelectedRow().getID()));

                        if (!isFilling()) {
                            SQLRow rowPole = selAffaire.getSelectedRow().getForeignRow("ID_POLE_PRODUIT");
                            comboCommercial.setValue(rowPole);
                        }
                    } else {
                        echeancier.getRequest().setWhere(null);
                    }
                }
            });
            this.add(echeancier, c);

        }

        this.comboClient.addValueListener(this.changeClientListener);

        // SQLPreferences prefs = SQLPreferences.getMemCached(getTable().getDBRoot());
        // if (prefs.getBoolean(GestionCommercialeGlobalPreferencePanel.ADDRESS_SPEC, true)) {

        final SQLElement adrElement = getElement().getForeignElement("ID_ADRESSE");
        final org.openconcerto.erp.core.customerrelationship.customer.ui.AddressChoiceUI addressUI = new org.openconcerto.erp.core.customerrelationship.customer.ui.AddressChoiceUI();

            addressUI.addToUI(this, c);

            comboClient.addModelListener("wantedID", new PropertyChangeListener() {

                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    int wantedID = comboClient.getWantedID();
                    System.err.println("SET WHERE ID_CLIENT = " + wantedID);
                    if (wantedID != SQLRow.NONEXISTANT_ID && wantedID >= SQLRow.MIN_VALID_ID) {

                        addressUI.getComboAdrF().getRequest().setWhere(
                                new Where(adrElement.getTable().getField("ID_CLIENT"), "=", wantedID).and(new Where(adrElement.getTable().getField("TYPE"), "=", AdresseType.Invoice.getId())));
                        addressUI.getComboAdrL().getRequest().setWhere(
                                new Where(adrElement.getTable().getField("ID_CLIENT"), "=", wantedID).and(new Where(adrElement.getTable().getField("TYPE"), "=", AdresseType.Delivery.getId())));
                    } else {
                        addressUI.getComboAdrF().getRequest().setWhere(Where.FALSE);
                        addressUI.getComboAdrL().getRequest().setWhere(Where.FALSE);
                    }
                }
            });
        // }
        // Contact
        this.contact = new ElementComboBox() {
            @Override
            protected SQLComponent createSQLComponent(EditMode mode) {
                final SQLComponent c = super.createSQLComponent(mode);
                if (mode.equals(EditMode.CREATION)) {
                    c.setDefaultsFactory(new IFactory<SQLRowValues>() {

                        @Override
                        public SQLRowValues createChecked() {
                            final SQLRowValues defaultContactRowValues = new SQLRowValues(eltContact.getTable());
                            final SQLRow row = SaisieVenteFactureSQLComponent.this.comboClient.getSelectedRow();
                                defaultContactRowValues.putForeignID("ID_CLIENT", row);

                            return defaultContactRowValues;
                        }
                    });
                }
                return c;
            }
        };

        JLabel labelContact = new JLabel(getLabelFor("ID_CONTACT"));
        c.gridy++;
        c.gridx = 0;
        c.gridwidth = 1;
        c.weightx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        labelContact.setHorizontalAlignment(SwingConstants.RIGHT);
        this.add(labelContact, c);

        c.gridx++;
        c.gridwidth = 1;
        c.weightx = 1;
        this.add(this.contact, c);
        SQLElement contactElement = getElement().getForeignElement("ID_CONTACT");
        this.contact.init(contactElement, contactElement.getComboRequest(true));
        this.contact.getRequest().setWhere(Where.FALSE);
        this.addSQLObject(this.contact, "ID_CONTACT");
        // }

        if (

        getTable().contains("DATE_LIVRAISON")) {
            JLabel labelDateLiv = new JLabel("Livraison le");
            c.gridx++;
            c.gridwidth = 1;
            c.weightx = 0;
            c.fill = GridBagConstraints.HORIZONTAL;
            labelDateLiv.setHorizontalAlignment(SwingConstants.RIGHT);
            this.add(labelDateLiv, c);

            c.gridx++;
            c.gridwidth = 1;
            c.weightx = 0;
            c.fill = GridBagConstraints.NONE;
            JDate dateLiv = new JDate();
            this.add(dateLiv, c);
            c.fill = GridBagConstraints.HORIZONTAL;
            this.addSQLObject(dateLiv, "DATE_LIVRAISON");
        }

        // Acompte
        this.checkAcompte = new JCheckBox(getLabelFor("ACOMPTE"));
        c.gridx++;
        c.gridwidth = GridBagConstraints.REMAINDER;
        // this.add(this.checkAcompte, c);
        c.gridwidth = 1;
        this.addView(this.checkAcompte, "ACOMPTE");

        // Compte Service
        this.checkCompteServiceAuto = new JCheckBox(getLabelFor("COMPTE_SERVICE_AUTO"));
        this.addSQLObject(this.checkCompteServiceAuto, "COMPTE_SERVICE_AUTO");
        this.compteSelService = new ISQLCompteSelector();

        this.labelCompteServ = new JLabel("Compte Service");
        c.gridy++;
        c.gridx = 0;
        c.gridwidth = 1;
        c.weightx = 0;
        this.labelCompteServ.setHorizontalAlignment(SwingConstants.RIGHT);
        this.add(this.labelCompteServ, c);

        c.gridx++;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 1;
        this.add(this.compteSelService, c);

        String valServ = DefaultNXProps.getInstance().getStringProperty("ArticleService");
        Boolean bServ = Boolean.valueOf(valServ);

        this.checkCompteServiceAuto.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setCompteServiceVisible(!SaisieVenteFactureSQLComponent.this.checkCompteServiceAuto.isSelected());
            }
        });

        // FIXME A checker si utile pour Preventec ou KD
        setCompteServiceVisible(false);
        setCompteServiceVisible(!(bServ != null && !bServ.booleanValue()));


        final JPanel pAcompte = new JPanel();
        final DeviseField textAcompteHT = new DeviseField();
        pAcompte.add(new JLabel("Acompte HT"));
        pAcompte.add(textAcompteHT);

        pAcompte.add(new JLabel("soit"));
        final JTextField textAcompte = new JTextField(5);
        pAcompte.add(textAcompte);
        pAcompte.add(new JLabel("%"));
        c.gridx = 0;
        c.gridy++;
        c.anchor = GridBagConstraints.EAST;
        c.fill = GridBagConstraints.NONE;
        this.add(pAcompte, c);
        c.anchor = GridBagConstraints.WEST;
        this.addView(textAcompte, "POURCENT_ACOMPTE");

        pAcompte.setVisible(false);

        /*******************************************************************************************
         * * DETAILS
         ******************************************************************************************/
            this.tableFacture = new SaisieVenteFactureItemTable();

        final ElementComboBox boxTarif = new ElementComboBox();
        if (this.getTable().getFieldsName().contains("ID_TARIF")) {
            // TARIF
            c.gridy++;
            c.gridx = 0;
            c.weightx = 0;
            c.weighty = 0;
            c.gridwidth = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            this.add(new JLabel(getLabelFor("ID_TARIF"), SwingConstants.RIGHT), c);
            c.gridx++;
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.fill = GridBagConstraints.NONE;
            c.weightx = 1;
            DefaultGridBagConstraints.lockMinimumSize(boxTarif);
            this.add(boxTarif, c);
            this.addView(boxTarif, "ID_TARIF");
            boxTarif.addModelListener("wantedID", new PropertyChangeListener() {

                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    SQLRow selectedRow = boxTarif.getRequest().getPrimaryTable().getRow(boxTarif.getWantedID());
                    tableFacture.setTarif(selectedRow, !isFilling());
                }
            });
        }
        if (this.getTable().getFieldsName().contains("ACOMPTE_COMMANDE")) {
            // ACOMPTE
            c.gridy += (this.getTable().getFieldsName().contains("ID_TARIF") ? 0 : 1);
            c.gridx += (this.getTable().getFieldsName().contains("ID_TARIF") ? 1 : 2);
            c.weightx = 0;
            c.weighty = 0;
            c.gridwidth = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            this.add(new JLabel(getLabelFor("ACOMPTE_COMMANDE"), SwingConstants.RIGHT), c);
            c.gridx++;
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.fill = GridBagConstraints.NONE;
            c.weightx = 1;
            JTextField acompteCmd = new JTextField(15);
            DefaultGridBagConstraints.lockMinimumSize(acompteCmd);
            this.add(acompteCmd, c);
            this.addView(acompteCmd, "ACOMPTE_COMMANDE");
        }

        c.gridy++;
        c.gridx = 0;
        c.weightx = 1;
        c.weighty = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.BOTH;

        ITextArea infos = new ITextArea(4, 4);
            this.add(this.tableFacture, c);

        // FIXME
        this.addView(this.tableFacture.getRowValuesTable(), "");

        /*******************************************************************************************
         * * MODE DE REGLEMENT
         ******************************************************************************************/
        JPanel panelBottom = new JPanel(new GridBagLayout());
        GridBagConstraints cBottom = new DefaultGridBagConstraints();
        cBottom.weightx = 1;
        cBottom.anchor = GridBagConstraints.NORTHWEST;
        // Mode de règlement
        this.addView("ID_MODE_REGLEMENT", REQ + ";" + DEC + ";" + SEP);
        this.eltModeRegl = (ElementSQLObject) this.getView("ID_MODE_REGLEMENT");
        panelBottom.add(this.eltModeRegl, cBottom);

        /*******************************************************************************************
         * * FRAIS DE PORT ET REMISE
         ******************************************************************************************/
        JPanel panelFrais = new JPanel();
        panelFrais.setLayout(new GridBagLayout());

        final GridBagConstraints cFrais = new DefaultGridBagConstraints();

        this.textPortHT = new DeviseField(5);
        DefaultGridBagConstraints.lockMinimumSize(textPortHT);
        addSQLObject(this.textPortHT, "PORT_HT");
        this.textRemiseHT = new DeviseField(5);
        DefaultGridBagConstraints.lockMinimumSize(textRemiseHT);
        addSQLObject(this.textRemiseHT, "REMISE_HT");

        // Frais de port
        cFrais.gridheight = 1;
        cFrais.gridx = 1;
        SQLRequestComboBox boxTaxePort = new SQLRequestComboBox(false, 8);
        if (getTable().contains("ID_TAXE_PORT")) {

            JLabel labelPortHT = new JLabel(getLabelFor("PORT_HT"));
            labelPortHT.setHorizontalAlignment(SwingConstants.RIGHT);
            cFrais.gridy++;
            panelFrais.add(labelPortHT, cFrais);
            cFrais.gridx++;
            panelFrais.add(this.textPortHT, cFrais);

            JLabel labelTaxeHT = new JLabel(getLabelFor("ID_TAXE_PORT"));
            labelTaxeHT.setHorizontalAlignment(SwingConstants.RIGHT);
            cFrais.gridx = 1;
            cFrais.gridy++;
            panelFrais.add(labelTaxeHT, cFrais);
            cFrais.gridx++;
            panelFrais.add(boxTaxePort, cFrais);
            this.addView(boxTaxePort, "ID_TAXE_PORT", REQ);

            boxTaxePort.addValueListener(new PropertyChangeListener() {

                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    // TODO Raccord de méthode auto-généré
                    totalTTC.updateTotal();
                }
            });
        }

        // Remise
        JLabel labelRemiseHT = new JLabel(getLabelFor("REMISE_HT"));
        labelRemiseHT.setHorizontalAlignment(SwingConstants.RIGHT);
        cFrais.gridy++;
        cFrais.gridx = 1;
        panelFrais.add(labelRemiseHT, cFrais);
        cFrais.gridx++;
        panelFrais.add(this.textRemiseHT, cFrais);
        cFrais.gridy++;

        cBottom.gridx++;
        cBottom.weightx = 1;
        cBottom.anchor = GridBagConstraints.NORTHEAST;
        cBottom.fill = GridBagConstraints.NONE;
        panelBottom.add(panelFrais, cBottom);

        /*******************************************************************************************
         * * CALCUL DES TOTAUX
         ******************************************************************************************/
        DeviseField fieldHT = new DeviseField();
        final DeviseField fieldTVA = new DeviseField();
        DeviseField fieldService = new DeviseField();
        DeviseField fieldTHA = new DeviseField();
        DeviseField fieldTEco = new DeviseField();

        DeviseField fieldDevise = null;
        if (getTable().getFieldsName().contains("T_DEVISE")) {
            fieldDevise = new DeviseField();
            addSQLObject(fieldDevise, "T_DEVISE");
        }
        // FIXME was required but not displayed for KD
        addSQLObject(fieldTHA, "T_HA");
        addSQLObject(fieldTEco, "T_ECO_CONTRIBUTION");
        addRequiredSQLObject(fieldHT, "T_HT");
        addRequiredSQLObject(fieldTVA, "T_TVA");
        addRequiredSQLObject(this.fieldTTC, "T_TTC");
        addRequiredSQLObject(fieldService, "T_SERVICE");
        JTextField poids = new JTextField();
        addSQLObject(poids, "T_POIDS");

        // Disable
        this.allowEditable("T_HA", false);
        this.allowEditable("T_ECO_CONTRIBUTION", false);
        this.allowEditable("T_HT", false);
        this.allowEditable("T_TVA", false);
        this.allowEditable("T_TTC", false);
        this.allowEditable("T_SERVICE", false);
        this.allowEditable("T_POIDS", false);

        totalTTC = new TotalPanel(this.tableFacture, fieldTEco, fieldHT, fieldTVA, this.fieldTTC, this.textPortHT, this.textRemiseHT, fieldService, fieldTHA, fieldDevise, poids, null,
                (getTable().contains("ID_TAXE_PORT") ? boxTaxePort : null), null);
        DefaultGridBagConstraints.lockMinimumSize(totalTTC);
        cBottom.gridx++;
        cBottom.weightx = 1;
        cBottom.anchor = GridBagConstraints.EAST;
        cBottom.fill = GridBagConstraints.HORIZONTAL;
        panelBottom.add(totalTTC, cBottom);

        c.anchor = GridBagConstraints.WEST;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy++;
        c.weighty = 0;
        this.add(panelBottom, c);

        // Ligne : Timbre
        c.gridy++;
        c.gridx = 0;
        c.weightx = 1;
        c.weighty = 0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.EAST;
        c.fill = GridBagConstraints.HORIZONTAL;
        final JPanel timbrePanel = createTimbrePanel();
        this.add(timbrePanel, c);

        timbrePanel.setVisible(this.gestionTimbre);

        // Ligne : Avoir
        c.gridy++;
        c.gridx = 0;
        c.weightx = 1;
        c.weighty = 0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.EAST;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.add(createPanelAvoir(), c);

        // Infos
            c.gridy++;
            c.gridx = 0;
            c.gridwidth = 4;
            c.fill = GridBagConstraints.BOTH;
            this.add(new TitledSeparator(getLabelFor("INFOS")), c);

            c.gridy++;

            final JScrollPane comp = new JScrollPane(infos);
            infos.setBorder(null);
            DefaultGridBagConstraints.lockMinimumSize(comp);
            this.add(comp, c);

        this.panelOO = new PanelOOSQLComponent(this);
        c.gridy++;
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.NORTHEAST;
        this.add(this.panelOO, c);

        this.addSQLObject(this.textAvoirTTC, "T_AVOIR_TTC");

        this.addRequiredSQLObject(dateSaisie, "DATE");
        this.addRequiredSQLObject(this.textNumeroUnique, "NUMERO");
        this.addSQLObject(infos, "INFOS");
        this.addSQLObject(this.checkPrevisionnelle, "PREVISIONNELLE");
        this.addSQLObject(this.checkComplement, "COMPLEMENT");
        this.addSQLObject(this.selAvoir, "ID_AVOIR_CLIENT");
        this.addSQLObject(this.compteSelService, "ID_COMPTE_PCE_SERVICE");
        ModeDeReglementSQLComponent modeReglComp;

        modeReglComp = (ModeDeReglementSQLComponent) this.eltModeRegl.getSQLChild();
        this.selAvoir.getRequest().setWhere(new Where(this.tableAvoir.getField("SOLDE"), "=", Boolean.FALSE));
        this.selAvoir.fillCombo();

        // Selection du compte de service
        int idCompteVenteService = rowPrefsCompte.getInt("ID_COMPTE_PCE_VENTE_SERVICE");
        if (idCompteVenteService <= 1) {
            try {
                idCompteVenteService = ComptePCESQLElement.getIdComptePceDefault("VentesServices");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        this.compteSelService.setValue(idCompteVenteService);

        // Lock

        DefaultGridBagConstraints.lockMinimumSize(this.comboClient);
        DefaultGridBagConstraints.lockMinimumSize(this.comboCommercial);

        // Listeners

        this.comboClient.addValueListener(this.listenerModeReglDefaut);
        this.fieldTTC.getDocument().addDocumentListener(new SimpleDocumentListener() {
            @Override
            public void update(DocumentEvent e) {
                calculTimbre();
                refreshText();
            }

        });

        if (this.checkTaux != null) {
            this.checkTaux.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    calculTimbre();
                    refreshText();
                }

            });
        }
        this.tauxTimbre.getDocument().addDocumentListener(new SimpleDocumentListener() {
            @Override
            public void update(DocumentEvent e) {
                calculTimbre();
                refreshText();
            }

        });

        this.totalTimbre.getDocument().addDocumentListener(new SimpleDocumentListener() {
            @Override
            public void update(DocumentEvent e) {
                refreshText();
            }

        });

        this.selAvoir.addValueListener(new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent evt) {
                refreshText();
            }

        });

        this.checkAcompte.addPropertyChangeListener(new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent evt) {

                pAcompte.setVisible(SaisieVenteFactureSQLComponent.this.checkAcompte.isSelected());
            }
        });

        this.changeClientListener = new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent evt) {

                if (SaisieVenteFactureSQLComponent.this.comboClient.getSelectedRow() != null) {
                    final SQLRow row = SaisieVenteFactureSQLComponent.this.comboClient.getSelectedRow();
                    final int id = (row == null) ? SQLRow.NONEXISTANT_ID : row.getID();
                        if (row != null) {
                            if (row.getFields().contains("ID_COMPTE_PCE_SERVICE") && compteSelService != null && !row.isForeignEmpty("ID_COMPTE_PCE_SERVICE")) {
                                compteSelService.setValue(row.getForeignID("ID_COMPTE_PCE_SERVICE"));
                            }
                            if (row.getFields().contains("ID_COMPTE_PCE_PRODUIT") && !row.isForeignEmpty("ID_COMPTE_PCE_PRODUIT")) {
                                totalTTC.setDefaultCompteProduit(row.getForeign("ID_COMPTE_PCE_PRODUIT"));
                            }
                        }
                    if (row != null) {
                        if (SaisieVenteFactureSQLComponent.this.contact != null) {
                            Where w = new Where(SaisieVenteFactureSQLComponent.this.eltContact.getTable().getField("ID_CLIENT"), "=", SQLRow.NONEXISTANT_ID);
                                w = w.or(new Where(SaisieVenteFactureSQLComponent.this.eltContact.getTable().getField("ID_CLIENT"), "=", id));
                            SaisieVenteFactureSQLComponent.this.contact.getRequest().setWhere(w);
                        }

                    } else {

                    }
                    SaisieVenteFactureSQLComponent.this.previousClient = id;
                }

            }
        };

        this.changeCompteListener = new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                SQLSelect sel = new SQLSelect(getTable().getBase());
                sel.addSelect(SaisieVenteFactureSQLComponent.this.client.getTable().getKey());
                Where where = new Where(SaisieVenteFactureSQLComponent.this.client.getTable().getField("ID_COMPTE_PCE"), "=", SaisieVenteFactureSQLComponent.this.compteSel.getValue());
                sel.setWhere(where);

                String req = sel.asString();
                List l = getTable().getBase().getDataSource().execute(req);
                if (l != null) {
                    if (l.size() == 1) {
                        Map<String, Object> m = (Map<String, Object>) l.get(0);
                        Object o = m.get(SaisieVenteFactureSQLComponent.this.client.getTable().getKey().getName());
                        System.err.println("Only one value match :: " + o);
                        if (o != null) {
                            SaisieVenteFactureSQLComponent.this.comboClient.setValue(Integer.valueOf(((Number) o).intValue()));
                        }
                    }
                }
            }
        };

        this.textPortHT.getDocument().addDocumentListener(new SimpleDocumentListener() {
            @Override
            public void update(DocumentEvent e) {
                totalTTC.updateTotal();
            }
        });

        this.textRemiseHT.getDocument().addDocumentListener(new SimpleDocumentListener() {
            @Override
            public void update(DocumentEvent e) {
                totalTTC.updateTotal();
            }
        });
        this.comboClient.addValueListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (SaisieVenteFactureSQLComponent.this.isFilling())
                    return;
                final SQLRow row = ((SQLRequestComboBox) evt.getSource()).getSelectedRow();
                if (row != null) {
                    if (SaisieVenteFactureSQLComponent.this.client.getTable().contains("ID_TARIF")) {
                        SQLRowAccessor foreignRow = row.getForeignRow("ID_TARIF");
                        if (foreignRow != null && !foreignRow.isUndefined() && (boxTarif.getSelectedRow() == null || boxTarif.getSelectedId() != foreignRow.getID())) {
                            boxTarif.setValue(foreignRow.getID());
                        } else {
                            boxTarif.setValue(foreignRow);
                        }

                    }

                    int idCpt = row.getInt("ID_COMPTE_PCE");

                    if (idCpt <= 1) {
                        // Select Compte client par defaut
                        idCpt = rowPrefsCompte.getInt("ID_COMPTE_PCE_CLIENT");
                        if (idCpt <= 1) {
                            try {
                                idCpt = ComptePCESQLElement.getIdComptePceDefault("Clients");
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    if (SaisieVenteFactureSQLComponent.this.compteSel != null) {
                        Integer i = SaisieVenteFactureSQLComponent.this.compteSel.getValue();
                        if (i == null || i.intValue() != idCpt) {
                            SaisieVenteFactureSQLComponent.this.compteSel.setValue(idCpt);
                        }
                    }
                }

            }
        });

    }

    private void calculTimbre() {
        totalTimbre.setValue(0L);
        if (gestionTimbre && this.checkTaux != null && this.checkTaux.isSelected()) {
            if (tauxTimbre.getText().trim().length() != 0) {
                BigDecimal taux = new BigDecimal(tauxTimbre.getText());
                Long ttc = fieldTTC.getValue();
                if (ttc != null) {
                    long timbreValue = taux.multiply(new BigDecimal(ttc)).movePointLeft(2).setScale(0, RoundingMode.HALF_UP).longValue();
                    totalTimbre.setValue(timbreValue);
                }
            }
        }
    }

    private JPanel createPanelAvoir() {
        JPanel panelAvoir = new JPanel(new GridBagLayout());
        panelAvoir.setOpaque(false);
        GridBagConstraints cA = new DefaultGridBagConstraints();
        JLabel labelAvoir = new JLabel(getLabelFor("ID_AVOIR_CLIENT"));
        labelAvoir.setHorizontalAlignment(SwingConstants.RIGHT);
        cA.weightx = 1;
        labelAvoir.setHorizontalAlignment(SwingConstants.RIGHT);
        panelAvoir.add(labelAvoir, cA);
        cA.weightx = 0;
        cA.gridx++;
        this.selAvoir = new ElementComboBox();
        this.selAvoir.setAddIconVisible(false);
        panelAvoir.add(this.selAvoir, cA);
        final JLabel labelTotalAvoir = new JLabel("Total à régler");
        this.textNetAPayer = new DeviseField();
        this.textNetAPayer.setEditable(false);
        cA.gridx++;
        cA.weightx = 0;
        panelAvoir.add(labelTotalAvoir, cA);
        cA.gridx++;
        cA.weightx = 0;
        panelAvoir.add(this.textNetAPayer, cA);
        addView(textNetAPayer, "NET_A_PAYER");
        this.textNetAPayer.setHorizontalAlignment(SwingConstants.RIGHT);

        return panelAvoir;
    }

    private JCheckBox checkTaux;

    private JPanel createTimbrePanel() {
        JPanel panelTimbre = new JPanel(new GridBagLayout());
        panelTimbre.setOpaque(false);
        GridBagConstraints cA = new DefaultGridBagConstraints();
        this.checkTaux = new JCheckBox(getLabelFor("SOUMIS_TIMBRE_FISCAL") + " " + getLabelFor("TAUX_TIMBRE_FISCAL"));
        checkTaux.setHorizontalAlignment(SwingConstants.RIGHT);
        cA.weightx = 1;
        checkTaux.setHorizontalAlignment(SwingConstants.RIGHT);
        panelTimbre.add(checkTaux, cA);
        cA.weightx = 0;
        cA.gridx++;
        this.tauxTimbre = new JTextField(8);
        panelTimbre.add(this.tauxTimbre, cA);
        final JLabel labelTotalTimbre = new JLabel(getLabelFor("TOTAL_TIMBRE_FISCAL"));
        this.totalTimbre = new DeviseField();
        this.totalTimbre.setEditable(false);
        cA.gridx++;
        cA.weightx = 0;
        panelTimbre.add(labelTotalTimbre, cA);
        cA.gridx++;
        cA.weightx = 0;
        panelTimbre.add(this.totalTimbre, cA);
        this.totalTimbre.setHorizontalAlignment(SwingConstants.RIGHT);

        this.addView(checkTaux, "SOUMIS_TIMBRE_FISCAL");
        this.addView(tauxTimbre, "TAUX_TIMBRE_FISCAL");
        this.addView(totalTimbre, "TOTAL_TIMBRE_FISCAL");
        return panelTimbre;
    }

    private void setCompteServiceVisible(boolean b) {
        this.compteSelService.setVisible(b);
        this.labelCompteServ.setVisible(b);
    }

    private void refreshText() {
        Number n = this.fieldTTC.getValue();
        long totalAvoirTTC = 0;
        long netAPayer = 0;
        long ttc = 0;
        if (n != null) {
            netAPayer = n.longValue();
            ttc = n.longValue();
        }

        if (this.selAvoir.getSelectedId() > 1) {
            SQLTable tableAvoir = Configuration.getInstance().getDirectory().getElement("AVOIR_CLIENT").getTable();
            if (n != null) {
                SQLRow rowAvoir = tableAvoir.getRow(this.selAvoir.getSelectedId());
                long totalAvoir = ((Number) rowAvoir.getObject("MONTANT_TTC")).longValue();
                totalAvoir -= ((Number) rowAvoir.getObject("MONTANT_SOLDE")).longValue();
                if (getSelectedID() > 1) {
                    SQLRow row = getTable().getRow(getSelectedID());
                    int idAvoirOld = row.getInt("ID_AVOIR_CLIENT");
                    if (idAvoirOld == rowAvoir.getID()) {
                        totalAvoir += Long.valueOf(row.getObject("T_AVOIR_TTC").toString());
                    }
                }

                long l = ttc - totalAvoir;
                if (l < 0) {
                    l = 0;
                    totalAvoirTTC = ttc;
                } else {
                    totalAvoirTTC = totalAvoir;
                }
                netAPayer = l;
            }
        }
        if (this.gestionTimbre) {
            Long timbre = this.totalTimbre.getValue();
            if (timbre != null) {
                netAPayer += timbre;
            }
        }
        this.textNetAPayer.setValue(netAPayer);
        this.textAvoirTTC.setValue(totalAvoirTTC);
    }

    public int insert(SQLRow order) {
        return commit(order);
    }

    private void createCompteServiceAuto(int id) throws SQLException {
        SQLRow rowPole = this.comboCommercial.getSelectedRow();
        SQLRow rowVerif = this.comboVerificateur.getSelectedRow();
        String verifInitiale = getInitialesFromVerif(rowVerif);
        int idCpt = ComptePCESQLElement.getId("706" + rowPole.getString("CODE") + verifInitiale, "Service " + rowPole.getString("NOM") + " " + rowVerif.getString("NOM"));
        SQLRowValues rowVals = this.getTable().getRow(id).createEmptyUpdateRow();
        rowVals.put("ID_COMPTE_PCE_SERVICE", idCpt);
        rowVals.update();
    }

    @Override
    public void select(SQLRowAccessor r) {

        this.panelOO.getCheckAbo().setSelected(false);

        boolean isPartial = false;
        if (r != null && r.getBoolean("PARTIAL") != null) {
            isPartial = r.getBoolean("PARTIAL").booleanValue();
        }

        boolean isSolde = false;
        if (r != null && r.getBoolean("SOLDE") != null) {
            isSolde = r.getBoolean("SOLDE").booleanValue();
        }

        if (r != null && !r.isUndefined() && (isPartial || isSolde)) {
            throw new IllegalArgumentException("Impossible de modifier une facturation intermédiaire");
        }

        if (compteSel != null) {
            this.compteSel.rmValueListener(this.changeCompteListener);
        }

        this.rowSelected = r;
        if (r != null) {
            // FIXME Mettre un droit pour autoriser la modification d'une facture lettrée ou pointée
            if (!r.isUndefined() && r.getObject("ID_MOUVEMENT") != null && !r.isForeignEmpty("ID_MOUVEMENT")) {
                SQLTable tableEcr = getTable().getTable("ECRITURE");
                SQLTable tableMvt = getTable().getTable("MOUVEMENT");
                SQLTable tablePiece = getTable().getTable("PIECE");
                {
                    SQLSelect sel = new SQLSelect();
                    sel.addSelect(tableEcr.getKey(), "COUNT");
                    int idPiece = r.getForeign("ID_MOUVEMENT").getInt("ID_PIECE");
                    Where w = new Where(tableMvt.getKey(), "=", tableEcr.getField("ID_MOUVEMENT"));
                    w = w.and(new Where(tablePiece.getKey(), "=", tableMvt.getField("ID_PIECE")));
                    w = w.and(new Where(tablePiece.getKey(), "=", idPiece));
                    w = w.and(new Where(tableEcr.getField("POINTEE"), "!=", "").or(new Where(tableEcr.getField("LETTRAGE"), "!=", "")));
                    sel.setWhere(w);
                    Object o = Configuration.getInstance().getRoot().getBase().getDataSource().executeScalar(sel.asString());
                    if (o != null && ((Number) o).longValue() > 0) {
                        SwingUtilities.invokeLater(new Runnable() {

                            @Override
                            public void run() {
                                JOptionPane.showMessageDialog(null, "Attention cette facture est pointée ou lettrée en comptabilité. \nToute modification écrasera ces informations comptables.");
                            }
                        });
                    }
                }
                {
                    SQLSelect sel = new SQLSelect();
                    SQLTable tableAssoc = getTable().getTable("ASSOCIATION_ANALYTIQUE");
                    sel.addSelect(tableAssoc.getKey(), "COUNT");
                    int idPiece = r.getForeign("ID_MOUVEMENT").getInt("ID_PIECE");
                    Where w = new Where(tableMvt.getKey(), "=", tableEcr.getField("ID_MOUVEMENT"));
                    w = w.and(new Where(tableAssoc.getField("ID_ECRITURE"), "=", tableEcr.getKey()));
                    w = w.and(new Where(tablePiece.getKey(), "=", tableMvt.getField("ID_PIECE")));
                    w = w.and(new Where(tablePiece.getKey(), "=", idPiece));
                    w = w.and(new Where(tableAssoc.getField("GESTION_AUTO"), "=", Boolean.FALSE));
                    sel.setWhere(w);
                    System.err.println(sel.asString());
                    Object o = Configuration.getInstance().getRoot().getBase().getDataSource().executeScalar(sel.asString());
                    if (o != null && ((Number) o).longValue() > 0) {
                        SwingUtilities.invokeLater(new Runnable() {

                            @Override
                            public void run() {
                                JOptionPane.showMessageDialog(null,
                                        "Attention la répartition analytique a été modifié manuellement sur cette facture. \nToute modification écrasera ces informations comptables.");
                            }
                        });
                    }
                }
            }
        }
            super.select(r);

        if (r != null)

        {
            // this.tableFacture.getModel().clearRows();
            // this.tableFacture.insertFrom("ID_SAISIE_VENTE_FACTURE", r.getID());
            Boolean b = (Boolean) r.getObject("ACOMPTE");
            if (b != null) {
                setAcompte(b);
            } else {
                setAcompte(false);
            }
        }

        if (this.comboClient != null) {

            this.comboClient.addValueListener(this.changeClientListener);
        }
        if (this.compteSel != null) {
            this.compteSel.addValueListener(this.changeCompteListener);
        } // nomClient.addValueListener(changeClientListener);
    }

    private String getInitialesFromVerif(SQLRow row) {
        String s = "";

        if (row != null) {
            String prenom = row.getString("PRENOM");
            if (prenom != null && prenom.length() > 0) {
                s += prenom.toUpperCase().charAt(0);
            }
            String nom = row.getString("NOM");
            if (nom != null && nom.length() > 0) {
                s += nom.toUpperCase().charAt(0);
            }
        }

        return s;
    }

    public int commit(SQLRow order) {

        int idSaisieVF = -1;
        long lFactureOld = 0;
        SQLRow rowFactureOld = null;
        SQLRow rowFacture = null;
        int attempt = 0;
        // on verifie qu'un devis du meme numero n'a pas été inséré entre temps
        if (!this.textNumeroUnique.checkValidation(false)) {
            while (attempt < JUniqueTextField.RETRY_COUNT) {
                String num = NumerotationAutoSQLElement.getNextNumero(getElement().getClass(), dateSaisie.getDate());
                this.textNumeroUnique.setText(num);
                attempt++;
                if (this.textNumeroUnique.checkValidation(false)) {
                    System.err.println("ATEMPT " + attempt + " SUCCESS WITH NUMERO " + num);
                    break;
                }
                try {
                    Thread.sleep(JUniqueTextField.SLEEP_WAIT_MS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        final String num = this.textNumeroUnique.getText();
        if (attempt == JUniqueTextField.RETRY_COUNT) {
            idSaisieVF = getSelectedID();
            ExceptionHandler.handle("Impossible d'ajouter, numéro de facture existant.");
            final Object root = SwingUtilities.getRoot(this);
            if (root instanceof EditFrame) {
                final EditFrame frame = (EditFrame) root;
                frame.getPanel().setAlwaysVisible(true);
            }
        } else {
            try {
                if (getMode() == Mode.INSERTION) {
                    idSaisieVF = super.insert(order);
                    rowFacture = getTable().getRow(idSaisieVF);
                    // incrémentation du numéro auto
                    final SQLRow rowNum = comboNumAuto == null ? this.tableNum.getRow(2) : comboNumAuto.getSelectedRow();
                    if (NumerotationAutoSQLElement.getNextNumero(SaisieVenteFactureSQLElement.class, rowFacture.getDate("DATE").getTime(), rowNum)
                            .equalsIgnoreCase(this.textNumeroUnique.getText().trim())) {
                        SQLRowValues rowVals = rowNum.createEmptyUpdateRow();

                        String labelNumberFor = NumerotationAutoSQLElement.getLabelNumberFor(SaisieVenteFactureSQLElement.class);
                        int val = rowNum.getInt(labelNumberFor);
                        val++;
                        rowVals.put(labelNumberFor, Integer.valueOf(val));
                        rowVals.update();
                    }
                } else {
                    if (JOptionPane.showConfirmDialog(this, "Attention en modifiant cette facture, vous supprimerez les chéques et les échéances associés. Continuer?", "Modification de facture",
                            JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                        // On recupere l'ancien total HT
                        rowFactureOld = this.getTable().getRow(getSelectedID());
                        lFactureOld = ((Number) rowFactureOld.getObject("T_HT")).longValue();

                        super.update();

                        idSaisieVF = getSelectedID();
                    } else {
                        // Annulation par l'utilisateur
                        return idSaisieVF;
                    }
                }

                rowFacture = getTable().getRow(idSaisieVF);
                final ComptaPropsConfiguration comptaPropsConfiguration = ((ComptaPropsConfiguration) Configuration.getInstance());

                // Mise à jour des tables liées
                this.tableFacture.updateField("ID_SAISIE_VENTE_FACTURE", idSaisieVF);

                this.tableFacture.createArticle(idSaisieVF, this.getElement());


                int idMvt = -1;
                if (!this.checkPrevisionnelle.isSelected()) {
                    if (getMode() == Mode.MODIFICATION) {
                        idMvt = rowFacture.getInt("ID_MOUVEMENT");
                        // on supprime tout ce qui est lié à la facture
                        System.err.println("Archivage des fils");
                        EcritureSQLElement eltEcr = (EcritureSQLElement) Configuration.getInstance().getDirectory().getElement("ECRITURE");
                        eltEcr.archiveMouvementProfondeur(idMvt, false);
                    }

                    System.err.println("Regeneration des ecritures");
                    if (idMvt > 1) {
                        new GenerationMvtSaisieVenteFacture(idSaisieVF, idMvt);
                    } else {
                        new GenerationMvtSaisieVenteFacture(idSaisieVF);
                    }
                    System.err.println("Fin regeneration");

                    // Mise à jour des stocks

                    updateStock(idSaisieVF);

                    // On retire l'avoir
                    if (rowFactureOld != null && rowFactureOld.getInt("ID_AVOIR_CLIENT") > 1) {

                        SQLRow rowAvoir = rowFactureOld.getForeignRow("ID_AVOIR_CLIENT");

                        Long montantSolde = (Long) rowAvoir.getObject("MONTANT_SOLDE");
                        Long avoirTTC = (Long) rowFactureOld.getObject("T_AVOIR_TTC");

                        long montant = montantSolde - avoirTTC;
                        if (montant < 0) {
                            montant = 0;
                        }

                        SQLRowValues rowVals = rowAvoir.createEmptyUpdateRow();

                        // Soldé
                        rowVals.put("SOLDE", Boolean.FALSE);
                        rowVals.put("MONTANT_SOLDE", montant);
                        Long restant = (Long) rowAvoir.getObject("MONTANT_TTC") - montantSolde;
                        rowVals.put("MONTANT_RESTANT", restant);

                        rowVals.update();

                    }

                    final int idAvoir = rowFacture.getInt("ID_AVOIR_CLIENT");
                    // on solde l'avoir
                    if (idAvoir > 1) {

                        SQLRow rowAvoir = rowFacture.getForeignRow("ID_AVOIR_CLIENT");

                        Long montantTTC = (Long) rowAvoir.getObject("MONTANT_TTC");
                        Long montantSolde = (Long) rowAvoir.getObject("MONTANT_SOLDE");
                        Long factTTC = (Long) rowFacture.getObject("T_TTC");

                        long restant = montantTTC - montantSolde;

                        SQLRowValues rowVals = rowAvoir.createEmptyUpdateRow();
                        final long l2 = factTTC - restant;
                        // Soldé
                        if (l2 >= 0) {
                            rowVals.put("SOLDE", Boolean.TRUE);
                            rowVals.put("MONTANT_SOLDE", montantTTC);
                            rowVals.put("MONTANT_RESTANT", 0);
                        } else {
                            // Il reste encore de l'argent pour l'avoir
                            final long m = montantSolde + factTTC;
                            rowVals.put("MONTANT_SOLDE", m);
                            rowVals.put("MONTANT_RESTANT", montantTTC - m);
                        }

                        rowVals.update();

                    }


                    if (getTable().getDBRoot().contains("ABONNEMENT") && panelOO.isCheckAboSelected()) {
                        DoWithRow doWithRow = ((SaisieVenteFactureSQLElement) getElement()).getSpecialAction("subscription.autocreate");
                        if (doWithRow != null) {
                            doWithRow.process(rowFacture);
                        }
                    }
                    createDocument(rowFacture);

                }
                if (attempt > 0) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            JOptionPane.showMessageDialog(null, "Le numéro a été actualisé en " + num);
                        }
                    });
                }
            } catch (Exception e) {
                ExceptionHandler.handle("", e);
            }
        }
        return idSaisieVF;
    }

    public void createDocument(SQLRow row) {
        // generation du document
        final VenteFactureXmlSheet sheet = new VenteFactureXmlSheet(row);

        try {
            sheet.createDocumentAsynchronous();
            sheet.showPrintAndExportAsynchronous(panelOO.isVisualisationSelected(), panelOO.isImpressionSelected(), true);
        } catch (Exception e) {
            ExceptionHandler.handle("Impossible de générer la facture", e);
        }
    }

    @Override
    public void update() {
        commit(null);
    }

    /**
     * Création d'une facture à partir d'un devis
     * 
     * @param idDevis
     * 
     */
    public void loadDevis(int idDevis) {

        SQLElement devis = Configuration.getInstance().getDirectory().getElement("DEVIS");
        SQLElement devisElt = Configuration.getInstance().getDirectory().getElement("DEVIS_ELEMENT");

        if (idDevis > 1) {
            SQLInjector injector = SQLInjector.getInjector(devis.getTable(), this.getTable());
            SQLRow rowDevis = devis.getTable().getRow(idDevis);
            SQLRowValues createRowValuesFrom = injector.createRowValuesFrom(idDevis);
            String string = rowDevis.getString("OBJET");
            createRowValuesFrom.put("NOM", string + (string.trim().length() == 0 ? "" : ",") + rowDevis.getString("NUMERO"));
            this.select(createRowValuesFrom);
        }

            loadItem(this.tableFacture, devis, idDevis, devisElt);
    }

    /**
     * Création d'une facture à partir d'une facture existante
     * 
     * @param idFacture
     * 
     */
    public void loadFactureExistante(int idFacture) {

        SQLElement fact = Configuration.getInstance().getDirectory().getElement("SAISIE_VENTE_FACTURE");
        SQLElement factElt = Configuration.getInstance().getDirectory().getElement("SAISIE_VENTE_FACTURE_ELEMENT");

        // On duplique la facture
        if (idFacture > 1) {
            SQLRow row = fact.getTable().getRow(idFacture);
            SQLRowValues rowVals = new SQLRowValues(fact.getTable());
            rowVals.put("ID_CLIENT", row.getInt("ID_CLIENT"));
            if (getTable().contains("ID_NUMEROTATION_AUTO")) {
                rowVals.put("NUMERO", NumerotationAutoSQLElement.getNextNumero(SaisieVenteFactureSQLElement.class, new Date(), row.getForeign("ID_NUMEROTATION_AUTO")));
            } else {
                rowVals.put("NUMERO", NumerotationAutoSQLElement.getNextNumero(SaisieVenteFactureSQLElement.class, new Date()));
            }
            rowVals.put("NOM", row.getObject("NOM"));
            this.select(rowVals);
        }

        // On duplique les elements de facture
        List<SQLRow> myListItem = fact.getTable().getRow(idFacture).getReferentRows(factElt.getTable());

        if (myListItem.size() != 0) {
            this.tableFacture.getModel().clearRows();

            for (SQLRow rowElt : myListItem) {

                SQLRowValues rowVals = rowElt.createUpdateRow();
                rowVals.clearPrimaryKeys();
                this.tableFacture.getModel().addRow(rowVals);
                int rowIndex = this.tableFacture.getModel().getRowCount() - 1;
                this.tableFacture.getModel().fireTableModelModified(rowIndex);
            }
        } else {
            this.tableFacture.getModel().clearRows();
        }
        this.tableFacture.getModel().fireTableDataChanged();
        this.tableFacture.repaint();
    }

    public List<SQLRowValues> createFactureAcompte(int idFacture, long acompte) {

        SQLElement fact = Configuration.getInstance().getDirectory().getElement("SAISIE_VENTE_FACTURE");
        SQLElement factElt = Configuration.getInstance().getDirectory().getElement("SAISIE_VENTE_FACTURE_ELEMENT");

        // On duplique la facture
        if (idFacture > 1) {
            SQLRow row = fact.getTable().getRow(idFacture);
            SQLRowValues rowVals = new SQLRowValues(fact.getTable());
            rowVals.put("ID_CLIENT", row.getInt("ID_CLIENT"));
            rowVals.put("ID_AFFAIRE", row.getInt("ID_AFFAIRE"));
            rowVals.put("NUMERO", NumerotationAutoSQLElement.getNextNumero(SaisieVenteFactureSQLElement.class, new Date()));
            rowVals.put("NOM", "Acompte de " + GestionDevise.currencyToString(acompte) + "€");
            this.select(rowVals);
        }

        // On duplique les elements de facture
        List<SQLRow> myListItem = fact.getTable().getRow(idFacture).getReferentRows(factElt.getTable());
        List<SQLRowValues> result = new ArrayList<SQLRowValues>(myListItem.size());
        if (myListItem.size() != 0) {
            this.tableFacture.getModel().clearRows();

            double acc = ((double) acompte / (double) myListItem.size());
            long toAdd = 0;
            SQLTable tablePourcentCCIP = Configuration.getInstance().getRoot().findTable("POURCENT_CCIP");
            for (SQLRow rowElt : myListItem) {

                SQLRowValues rowValsUp = rowElt.createUpdateRow();
                SQLRowValues rowVals = rowElt.createUpdateRow();
                rowVals.clearPrimaryKeys();

                long val = rowVals.getLong("T_PV_HT");
                double value = (acc + toAdd) / val * 100.0;
                // Si l'acompte est supérieur au montant
                if (value > 100) {
                    value = 100;
                    toAdd += (acc - val);
                } else {
                    toAdd = 0;
                }
                BigDecimal pourcentAcompte = new BigDecimal(rowValsUp.getLong("POURCENT_ACOMPTE") - value);
                rowValsUp.put("POURCENT_ACOMPTE", pourcentAcompte);
                BigDecimal pourcentCurrentAcompte = new BigDecimal(value);
                rowVals.put("POURCENT_ACOMPTE", pourcentCurrentAcompte);
                List<SQLRow> rowsCCIP = rowElt.getReferentRows(tablePourcentCCIP);
                if (rowsCCIP.size() > 0) {
                    SQLRowValues rowValsCCIP = rowsCCIP.get(0).createUpdateRow();
                    rowValsCCIP.clearPrimaryKeys();
                    rowValsCCIP.put("ID_SAISIE_VENTE_FACTURE_ELEMENT", rowVals);
                    rowValsCCIP.put("NOM", "Acompte");
                    rowValsCCIP.put("POURCENT", pourcentCurrentAcompte);
                }
                System.err.println(value);
                this.tableFacture.getModel().addRow(rowVals);
                int rowIndex = this.tableFacture.getModel().getRowCount() - 1;
                this.tableFacture.getModel().fireTableModelModified(rowIndex);
            }
            if (toAdd > 0) {
                for (int i = 0; i < this.tableFacture.getModel().getRowCount() && toAdd > 0; i++) {
                    SQLRowValues rowVals = this.tableFacture.getModel().getRowValuesAt(i);
                    if (rowVals.getFloat("POURCENT_ACOMPTE") < 100) {
                        long val = rowVals.getLong("T_PV_HT");
                        double value = (acc + toAdd) / val * 100.0;
                        // Si l'acompte est supérieur au montant
                        if (value > 100) {
                            value = 100;
                            toAdd += (acc - val);
                        } else {
                            toAdd = 0;
                        }
                        rowVals.put("POURCENT_ACOMPTE", new BigDecimal(value));
                        this.tableFacture.getModel().fireTableModelModified(i);
                    }
                }
            }

            // FIXME Check total if pb with round
        } else {
            this.tableFacture.getModel().clearRows();
        }
        this.tableFacture.getModel().fireTableDataChanged();
        this.tableFacture.repaint();
        return result;
    }

    /**
     * Création d'une facture à partir d'une commande
     * 
     * @param idCmd
     * 
     */
    public void loadCommande(int idCmd) {

        SQLElement cmd = Configuration.getInstance().getDirectory().getElement("COMMANDE_CLIENT");
        SQLElement cmdElt = Configuration.getInstance().getDirectory().getElement("COMMANDE_CLIENT_ELEMENT");

        if (idCmd > 1) {
            SQLInjector injector = SQLInjector.getInjector(cmd.getTable(), this.getTable());
            SQLRow rowCmd = cmd.getTable().getRow(idCmd);
            SQLRowValues createRowValuesFrom = injector.createRowValuesFrom(idCmd);
            String string = rowCmd.getString("NOM");
            createRowValuesFrom.put("NOM", string + (string.trim().length() == 0 ? "" : ",") + rowCmd.getString("NUMERO"));
            this.select(createRowValuesFrom);
            this.listenerModeReglDefaut.propertyChange(null);
        }
        loadItem(this.tableFacture, cmd, idCmd, cmdElt);
    }

    /**
     * Création d'une facture à partir d'un bon de livraison
     * 
     * @param idBl
     * 
     */
    public void loadBonItems(SQLRowAccessor rowBL, boolean clear) {

        SQLElement bon = Configuration.getInstance().getDirectory().getElement("BON_DE_LIVRAISON");
        SQLElement bonElt = Configuration.getInstance().getDirectory().getElement("BON_DE_LIVRAISON_ELEMENT");

        loadItem(this.tableFacture, bon, rowBL.getID(), bonElt, clear);
    }

    public void addRowItem(SQLRowValues row) {
        this.tableFacture.getModel().addRow(row);
    }

    private static final SQLElement eltModeReglement = Configuration.getInstance().getDirectory().getElement("MODE_REGLEMENT");

    @Override
    protected SQLRowValues createDefaults() {
        SQLRowValues vals = new SQLRowValues(this.getTable());
        SQLRow r;

        try {
            r = ModeReglementDefautPrefPanel.getDefaultRow(true);
            if (r.getID() > 1) {
                SQLRowValues rowVals = eltModeReglement.createCopy(r, null);
                System.err.println(rowVals.getInt("ID_TYPE_REGLEMENT"));
                vals.put("ID_MODE_REGLEMENT", rowVals);
            }
        } catch (SQLException e) {
            System.err.println("Impossible de sélectionner le mode de règlement par défaut du client.");
            e.printStackTrace();
        }
        this.tableFacture.getModel().clearRows();

        // User
        // SQLSelect sel = new SQLSelect(Configuration.getInstance().getBase());
        SQLElement eltComm = Configuration.getInstance().getDirectory().getElement("COMMERCIAL");
        int idUser = UserManager.getInstance().getCurrentUser().getId();

        // sel.addSelect(eltComm.getTable().getKey());
        // sel.setWhere(new Where(eltComm.getTable().getField("ID_USER_COMMON"), "=", idUser));
        // List<SQLRow> rowsComm = (List<SQLRow>)
        // Configuration.getInstance().getBase().getDataSource().execute(sel.asString(), new
        // SQLRowListRSH(eltComm.getTable()));

        SQLRow rowsComm = SQLBackgroundTableCache.getInstance().getCacheForTable(eltComm.getTable()).getFirstRowContains(idUser, eltComm.getTable().getField("ID_USER_COMMON"));

        if (rowsComm != null) {
            vals.put("ID_COMMERCIAL", rowsComm.getID());
        }

        SQLPreferences prefs = SQLPreferences.getMemCached(getTable().getDBRoot());
        Double d = prefs.getDouble(GestionCommercialeGlobalPreferencePanel.TAUX_TIMBRE_FISCAL, Double.valueOf(1));
        vals.put("TAUX_TIMBRE_FISCAL", new BigDecimal(d));
        vals.put("TOTAL_TIMBRE_FISCAL", 0L);
        // User
        final ComptaPropsConfiguration comptaPropsConfiguration = ((ComptaPropsConfiguration) Configuration.getInstance());
        if (getTable().contains("ID_NUMEROTATION_AUTO")) {
            vals.put("ID_NUMEROTATION_AUTO", this.defaultNum.getID());
            vals.put("NUMERO", NumerotationAutoSQLElement.getNextNumero(SaisieVenteFactureSQLElement.class, new Date(), vals.getForeign("ID_NUMEROTATION_AUTO")));
        } else {
            vals.put("NUMERO", NumerotationAutoSQLElement.getNextNumero(SaisieVenteFactureSQLElement.class, new Date()));
        }
        int idCompteVenteProduit = rowPrefsCompte.getInt("ID_COMPTE_PCE_VENTE_PRODUIT");
        if (idCompteVenteProduit <= 1) {
            try {
                idCompteVenteProduit = ComptePCESQLElement.getIdComptePceDefault("VentesProduits");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        vals.put("ID_COMPTE_PCE_VENTE", idCompteVenteProduit);
        if (this.checkCT != null) {
            vals.put("CONTROLE_TECHNIQUE", this.checkCT.isSelected());
        }
        if (getTable().contains("CREATE_VIRTUAL_STOCK")) {
            vals.put("CREATE_VIRTUAL_STOCK", Boolean.TRUE);
        }
        int idCompteVenteService = rowPrefsCompte.getInt("ID_COMPTE_PCE_VENTE_SERVICE");
        if (idCompteVenteService <= 1) {
            try {
                idCompteVenteService = ComptePCESQLElement.getIdComptePceDefault("VentesServices");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (getTable().contains("ID_TAXE_PORT")) {
            SQLRow taxeDefault = TaxeCache.getCache().getFirstTaxe();
            vals.put("ID_TAXE_PORT", taxeDefault.getID());
        }
        vals.put("ID_COMPTE_PCE_SERVICE", idCompteVenteService);
        System.err.println("Defaults " + vals);
        return vals;
    }

    public void setDefaults() {
        this.resetValue();

        this.textNumeroUnique.setText(NumerotationAutoSQLElement.getNextNumero(SaisieVenteFactureSQLElement.class, new java.util.Date(), defaultNum));
        this.tableFacture.getModel().clearRows();
    }

    public RowValuesTableModel getRowValuesTableModel() {
        return this.tableFacture.getModel();
    }

    /**
     * Définir la facture comme prévisionnelle. Pas de génération comptable, ni de mode de règlement
     * 
     * @deprecated mettre les valeurs dans une RowValues
     * @param b
     */
    @Deprecated
    public void setPrevisonnelle(boolean b) {
        this.checkPrevisionnelle.setSelected(b);
        if (!b) {
            this.textNumeroUnique.setText(NumerotationAutoSQLElement.getNextNumero(SaisieVenteFactureSQLElement.class, new Date()));
        }
    }

    /**
     * Définir la facture comme complémentaire. Règlement supérieur au montant de la facture
     * initiale
     * 
     * @param b
     */
    public void setComplement(boolean b) {
        this.checkComplement.setSelected(b);
    }

    /**
     * Définir la facture comme acompte.
     * 
     * @param b
     */
    public void setAcompte(boolean b) {
        this.checkAcompte.setSelected(b);
        this.checkAcompte.firePropertyChange("ValueChanged", !b, b);
    }

    public void setTypeInterventionText(String text) {
        this.textTypeMission.setValue(text);
    }

    public void setReferenceClientText(String text) {
        this.refClient.setText(text);
    }

    protected String getLibelleStock(SQLRowAccessor row, SQLRowAccessor rowElt) {
        return "Saisie vente facture N°" + row.getString("NUMERO");
    }

    /**
     * Mise à jour des stocks pour chaque article composant la facture
     * 
     * @throws SQLException
     */
    private void updateStock(int id) throws SQLException {

        SQLPreferences prefs = SQLPreferences.getMemCached(getTable().getDBRoot());
        if (prefs.getBoolean(GestionArticleGlobalPreferencePanel.STOCK_FACT, true)) {
            SQLRow row = getTable().getRow(id);
            StockItemsUpdater stockUpdater = new StockItemsUpdater(new StockLabel() {

                @Override
                public String getLabel(SQLRowAccessor rowOrigin, SQLRowAccessor rowElt) {
                    return getLibelleStock(rowOrigin, rowElt);
                }
            }, row, row.getReferentRows(getTable().getTable("SAISIE_VENTE_FACTURE_ELEMENT")),
                    getTable().contains("CREATE_VIRTUAL_STOCK") && row.getBoolean("CREATE_VIRTUAL_STOCK") ? TypeStockUpdate.REAL_VIRTUAL_DELIVER : TypeStockUpdate.REAL_DELIVER);

            stockUpdater.update();

        }
    }

    @Override
    protected RowValuesTable getRowValuesTable() {
        return this.tableFacture.getRowValuesTable();
    }

    @Override
    protected void refreshAfterSelect(SQLRowAccessor r) {
        this.tableFacture.setDateDevise(this.dateSaisie.getValue());
    }

}
