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
 
 package org.openconcerto.erp.core.sales.credit.component;

import static org.openconcerto.utils.CollectionUtils.createSet;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.common.component.SocieteCommonSQLElement;
import org.openconcerto.erp.core.common.component.TransfertBaseSQLComponent;
import org.openconcerto.erp.core.common.element.BanqueSQLElement;
import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.erp.core.common.element.NumerotationAutoSQLElement;
import org.openconcerto.erp.core.common.ui.AbstractArticleItemTable;
import org.openconcerto.erp.core.common.ui.DeviseField;
import org.openconcerto.erp.core.common.ui.PanelFrame;
import org.openconcerto.erp.core.common.ui.TotalPanel;
import org.openconcerto.erp.core.customerrelationship.customer.ui.AddressChoiceUI;
import org.openconcerto.erp.core.customerrelationship.customer.ui.AdresseType;
import org.openconcerto.erp.core.finance.accounting.element.ComptePCESQLElement;
import org.openconcerto.erp.core.finance.accounting.element.EcritureSQLElement;
import org.openconcerto.erp.core.finance.payment.component.ModeDeReglementSQLComponent;
import org.openconcerto.erp.core.sales.credit.ui.AvoirItemTable;
import org.openconcerto.erp.core.supplychain.stock.element.StockItemsUpdater;
import org.openconcerto.erp.core.supplychain.stock.element.StockItemsUpdater.TypeStockUpdate;
import org.openconcerto.erp.core.supplychain.stock.element.StockLabel;
import org.openconcerto.erp.generationDoc.gestcomm.AvoirClientXmlSheet;
import org.openconcerto.erp.generationEcritures.GenerationMvtAvoirClient;
import org.openconcerto.erp.generationEcritures.GenerationMvtSaisieVenteFacture;
import org.openconcerto.erp.model.ISQLCompteSelector;
import org.openconcerto.erp.panel.PanelOOSQLComponent;
import org.openconcerto.erp.preferences.DefaultNXProps;
import org.openconcerto.erp.preferences.GestionClientPreferencePanel;
import org.openconcerto.erp.preferences.GestionCommercialeGlobalPreferencePanel;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.ElementSQLObject;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.preferences.SQLPreferences;
import org.openconcerto.sql.request.ComboSQLRequest;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.sql.sqlobject.JUniqueTextField;
import org.openconcerto.sql.sqlobject.SQLRequestComboBox;
import org.openconcerto.sql.view.EditFrame;
import org.openconcerto.sql.view.EditPanel.EditMode;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.FormLayouter;
import org.openconcerto.ui.FrameUtil;
import org.openconcerto.ui.JDate;
import org.openconcerto.ui.component.ITextArea;
import org.openconcerto.ui.component.InteractionMode;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.cc.IFactory;
import org.openconcerto.utils.checks.ValidState;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.apache.commons.dbutils.handlers.ArrayListHandler;

public class AvoirClientSQLComponent extends TransfertBaseSQLComponent implements ActionListener {

    protected PanelOOSQLComponent panelGestDoc;
    private JTextField textNom;
    private JDate date;
    private JUniqueTextField textNumero;
    private AbstractArticleItemTable table;
    private JCheckBox boxAdeduire = new JCheckBox(getLabelFor("A_DEDUIRE"));
    private ElementSQLObject eltModeRegl;
    private final SQLTable tableClient = ((ComptaPropsConfiguration) Configuration.getInstance()).getRootSociete().getTable("CLIENT");
    private final SQLElement clientElt = Configuration.getInstance().getDirectory().getElement(this.tableClient);
    private final ElementComboBox comboClient = new ElementComboBox();
    private ElementComboBox comboPole = new ElementComboBox();
    private ElementComboBox comboCommercial = new ElementComboBox();
    private ElementComboBox comboVerificateur = new ElementComboBox();
    private final ElementComboBox comboBanque = new ElementComboBox();
    private ElementComboBox selectContact;
    private JLabel labelCompteServ;
    private ISQLCompteSelector compteSelService;
    private static final SQLTable TABLE_PREFS_COMPTE = Configuration.getInstance().getBase().getTable("PREFS_COMPTE");
    private static final SQLRow ROW_PREFS_COMPTE = TABLE_PREFS_COMPTE.getRow(2);
    private final boolean displayDpt;
    private final ElementComboBox comboDpt = new ElementComboBox();

    private JCheckBox checkCompteServiceAuto;

    private PropertyChangeListener listenerModeReglDefaut = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent arg0) {
            int idCli = AvoirClientSQLComponent.this.comboClient.getWantedID();
            if (idCli > 1) {
                SQLRow rowCli = AvoirClientSQLComponent.this.tableClient.getRow(idCli);
                if (!rowCli.isForeignEmpty("ID_COMMERCIAL")) {
                    comboCommercial.setValue(rowCli.getForeignID("ID_COMMERCIAL"));
                }
                SQLElement sqleltModeRegl = Configuration.getInstance().getDirectory().getElement("MODE_REGLEMENT");
                int idModeRegl = rowCli.getInt("ID_MODE_REGLEMENT");
                if (!isFilling() && idModeRegl > 1 && AvoirClientSQLComponent.this.eltModeRegl.isCreated()) {
                    SQLRow rowModeRegl = sqleltModeRegl.getTable().getRow(idModeRegl);
                    SQLRowValues rowValsModeRegl = rowModeRegl.createUpdateRow();
                    rowValsModeRegl.clearPrimaryKeys();
                    AvoirClientSQLComponent.this.eltModeRegl.setValue(rowValsModeRegl);
                }
            }
        }
    };
    private final ElementComboBox boxTarif = new ElementComboBox();
    private PropertyChangeListener changeClientListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent evt) {
            // compteSel.removeValueListener(changeCompteListener);

            // if (AvoirClientSQLComponent.this.comboClient.getValue() != null) {
            Integer id = AvoirClientSQLComponent.this.comboClient.getWantedID();
            if (id > 1) {

                SQLRow row = AvoirClientSQLComponent.this.clientElt.getTable().getRow(id);

                final SQLField fieldForeignClient = getTable().getDBRoot().findTable("CONTACT").getField("ID_CLIENT");
                Where wC = new Where(fieldForeignClient, "=", SQLRow.NONEXISTANT_ID);
                    wC = wC.or(new Where(getTable().getDBRoot().findTable("CONTACT").getField("ID_CLIENT"), "=", id));
                selectContact.getRequest().setWhere(wC);

                if (!isFilling() && comboClient.getElement().getTable().getFieldsName().contains("ID_TARIF")) {

                    // SQLRowAccessor foreignRow = row.getForeignRow("ID_TARIF");
                    // if (foreignRow.isUndefined() &&
                    // !row.getForeignRow("ID_DEVISE").isUndefined()) {
                    // SQLRowValues rowValsD = new SQLRowValues(foreignRow.getTable());
                    // rowValsD.put("ID_DEVISE", row.getObject("ID_DEVISE"));
                    // foreignRow = rowValsD;
                    //
                    // }
                    // tableBonItem.setTarif(foreignRow, true);
                    SQLRowAccessor foreignRow = row.getForeignRow("ID_TARIF");
                    if (!foreignRow.isUndefined() && (boxTarif.getSelectedRow() == null || boxTarif.getSelectedId() != foreignRow.getID())
                            && JOptionPane.showConfirmDialog(null, "Appliquer les tarifs associés au client?") == JOptionPane.YES_OPTION) {
                        boxTarif.setValue(foreignRow.getID());
                        // SaisieVenteFactureSQLComponent.this.tableFacture.setTarif(foreignRow,
                        // true);
                    } else {
                        boxTarif.setValue(foreignRow.getID());
                    }
                }

            } else {
                selectContact.getRequest().setWhere(Where.FALSE);
            }
            // }
        }
    };


    protected org.openconcerto.sql.view.list.RowValuesTable getRowValuesTable() {
        return this.table.getRowValuesTable();

    };

    @Override
    protected SQLRowValues createDefaults() {
        SQLRowValues vals = new SQLRowValues(this.getTable());
        vals.put("A_DEDUIRE", Boolean.TRUE);
        this.eltModeRegl.setEditable(InteractionMode.DISABLED);
        this.eltModeRegl.setCreated(false);


        // Selection du compte de service
        int idCompteVenteService = ROW_PREFS_COMPTE.getInt("ID_COMPTE_PCE_VENTE_SERVICE");
        if (idCompteVenteService <= 1) {
            try {
                idCompteVenteService = ComptePCESQLElement.getIdComptePceDefault("VentesServices");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        vals.put("NUMERO", NumerotationAutoSQLElement.getNextNumero(getElement().getClass(), new Date()));
        vals.put("MONTANT_TTC", Long.valueOf(0));
        vals.put("MONTANT_SERVICE", Long.valueOf(0));
        vals.put("MONTANT_HT", Long.valueOf(0));
        vals.put("MONTANT_TVA", Long.valueOf(0));
        vals.put("ID_COMPTE_PCE_SERVICE", idCompteVenteService);

        return vals;
    }

    public AvoirClientSQLComponent() {
        super(Configuration.getInstance().getDirectory().getElement("AVOIR_CLIENT"));
        SQLPreferences prefs = SQLPreferences.getMemCached(getTable().getDBRoot());
        this.displayDpt = prefs.getBoolean(GestionClientPreferencePanel.DISPLAY_CLIENT_DPT, false);
    }

    public void addViews() {
        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();
        textNumero = new JUniqueTextField(16);
        // Champ Module
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = GridBagConstraints.REMAINDER;
        final JPanel addP = ComptaSQLConfElement.createAdditionalPanel();
        this.setAdditionalFieldsPanel(new FormLayouter(addP, 1));
        this.add(addP, c);

        c.gridy++;
        c.gridwidth = 1;

        this.textNom = new JTextField();
        this.date = new JDate(true);


            this.date.addValueListener(new PropertyChangeListener() {

                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    fireValidChange();
                    if (!isFilling() && date.getValue() != null) {
                        table.setDateDevise(date.getValue());
                    }
                }
            });


        // Ligne 1: Numero
        this.add(new JLabel(getLabelFor("NUMERO"), SwingConstants.RIGHT), c);
        c.weightx = 1;
        c.fill = GridBagConstraints.NONE;
        c.gridx++;

        DefaultGridBagConstraints.lockMinimumSize(textNumero);
        this.add(this.textNumero, c);

        // Date
        c.gridx++;
        c.weightx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.add(new JLabel("Date", SwingConstants.RIGHT), c);
        c.gridx++;
        c.weightx = 1;
        c.fill = GridBagConstraints.NONE;
        this.add(this.date, c);

        // Ligne 2: Libellé
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.add(new JLabel(getLabelFor("NOM"), SwingConstants.RIGHT), c);
        c.gridx++;
        // c.weightx = 1;
        this.add(this.textNom, c);

        // Commercial
        c.gridx++;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0;
        this.comboCommercial = new ElementComboBox();
        this.comboCommercial.setMinimumSize(this.comboCommercial.getPreferredSize());
            this.add(new JLabel(getLabelFor("ID_COMMERCIAL"), SwingConstants.RIGHT), c);
            c.gridx++;
            // c.weightx = 1;
            c.fill = GridBagConstraints.NONE;
            this.add(this.comboCommercial, c);

            this.addSQLObject(this.comboCommercial, "ID_COMMERCIAL");

        // Ligne 3: Motif
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        this.add(new JLabel(getLabelFor("MOTIF"), SwingConstants.RIGHT), c);
        c.gridx++;
        c.gridwidth = 1;
        // c.weightx = 1;

        JTextField textMotif = new JTextField();
        this.add(textMotif, c);

        if (getTable().contains("DATE_LIVRAISON")) {
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

        // Client
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        c.gridwidth = 1;
        this.add(new JLabel(getLabelFor("ID_CLIENT"), SwingConstants.RIGHT), c);
        c.gridx++;
        c.gridwidth = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.NONE;
        this.add(this.comboClient, c);
        this.addRequiredSQLObject(this.comboClient, "ID_CLIENT");

        if (this.displayDpt) {
            c.gridx++;
            c.gridwidth = 1;
            c.weightx = 0;
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

            SQLPreferences prefs = SQLPreferences.getMemCached(getTable().getDBRoot());
            if (prefs.getBoolean(GestionCommercialeGlobalPreferencePanel.ADDRESS_SPEC, true)) {

                // Adresse spe
                final SQLElement adrElement = getElement().getForeignElement("ID_ADRESSE");
                final AddressChoiceUI addressUI = new AddressChoiceUI();
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
            }

        final ComptaPropsConfiguration comptaPropsConfiguration = ((ComptaPropsConfiguration) Configuration.getInstance());

        // Contact
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 1;
        c.weightx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        // c.weightx = 0;
        final JLabel labelContact = new JLabel(getLabelFor("ID_CONTACT"), SwingConstants.RIGHT);
        this.add(labelContact, c);
        this.selectContact = new ElementComboBox() {
            @Override
            protected SQLComponent createSQLComponent(EditMode mode) {
                final SQLComponent c = super.createSQLComponent(mode);
                if (mode.equals(EditMode.CREATION)) {
                    c.setDefaultsFactory(new IFactory<SQLRowValues>() {

                        @Override
                        public SQLRowValues createChecked() {
                            final SQLRowValues defaultContactRowValues = new SQLRowValues(selectContact.getRequest().getPrimaryTable());
                            Integer id = AvoirClientSQLComponent.this.comboClient.getWantedID();
                            defaultContactRowValues.put("ID_CLIENT", id);
                            return defaultContactRowValues;
                        }
                    });
                }
                return c;
            }
        };
        c.gridx++;
        c.gridwidth = 3;
        c.weightx = 1;
        this.add(selectContact, c);
        final SQLElement contactElement = getElement().getForeignElement("ID_CONTACT");
        selectContact.init(contactElement, contactElement.getComboRequest(true));
        selectContact.getRequest().setWhere(Where.FALSE);
        this.addView(selectContact, "ID_CONTACT");



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

        this.addRequiredSQLObject(this.compteSelService, "ID_COMPTE_PCE_SERVICE");

        String valServ = DefaultNXProps.getInstance().getStringProperty("ArticleService");
        Boolean bServ = Boolean.valueOf(valServ);
        if (!bServ) {
            this.labelCompteServ.setVisible(false);
            this.compteSelService.setVisible(false);
        }

        this.checkCompteServiceAuto.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setCompteServiceVisible(!AvoirClientSQLComponent.this.checkCompteServiceAuto.isSelected());
            }
        });

        // setCompteServiceVisible(!(bServ != null && !bServ.booleanValue()));


        // Tarif
        if (this.getTable().getFieldsName().contains("ID_TARIF")) {
            // TARIF
            c.gridy++;
            c.gridx = 0;
            c.weightx = 0;
            c.weighty = 0;
            c.gridwidth = 1;
            this.add(new JLabel("Tarif à appliquer", SwingConstants.RIGHT), c);
            c.gridx++;
            c.gridwidth = GridBagConstraints.REMAINDER;

            c.weightx = 1;
            this.add(boxTarif, c);
            this.addView(boxTarif, "ID_TARIF");
            boxTarif.addValueListener(new PropertyChangeListener() {

                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    table.setTarif(boxTarif.getSelectedRow(), false);
                }
            });
        }

        // Table
            this.table = new AvoirItemTable();
        c.gridx = 0;
        c.gridy++;
        c.fill = GridBagConstraints.BOTH;
        c.gridheight = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weighty = 1;
        // c.weightx = 0;
        this.add(this.table, c);
        this.addView(this.table.getRowValuesTable(), "");

        // Panel du bas
        final JPanel panelBottom = getBottomPanel();
        c.gridy++;
        c.weighty = 0;
        this.add(panelBottom, c);

        // Infos

        c.gridheight = 1;
        c.gridx = 0;
        c.gridy++;
        this.add(new JLabel(getLabelFor("INFOS")), c);

        c.gridy++;
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 0;
        c.gridwidth = 4;
        ITextArea infos = new ITextArea(4, 4);
        infos.setBorder(null);
        JScrollPane scrollPane = new JScrollPane(infos);
        DefaultGridBagConstraints.lockMinimumSize(scrollPane);
        this.add(scrollPane, c);

        //
        // Impression
        this.panelGestDoc = new PanelOOSQLComponent(this);
        c.fill = GridBagConstraints.NONE;
        c.gridy++;
        c.anchor = GridBagConstraints.EAST;
        this.add(panelGestDoc, c);

        this.addSQLObject(this.textNom, "NOM");
        if (getTable().contains("INFOS")) {
            this.addSQLObject(infos, "INFOS");
        }
        this.addSQLObject(this.boxAdeduire, "A_DEDUIRE");
        this.addSQLObject(textMotif, "MOTIF");

        this.addRequiredSQLObject(this.textNumero, "NUMERO");
        this.addRequiredSQLObject(this.date, "DATE");
        this.boxAdeduire.addActionListener(this);

        this.comboClient.addModelListener("wantedID", this.listenerModeReglDefaut);
        this.comboClient.addModelListener("wantedID", this.changeClientListener);
        DefaultGridBagConstraints.lockMinimumSize(comboClient);
        DefaultGridBagConstraints.lockMinimumSize(this.comboBanque);
        DefaultGridBagConstraints.lockMinimumSize(comboCommercial);

    }

    private JPanel getBottomPanel() {

        // UI

        final JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        final GridBagConstraints c = new DefaultGridBagConstraints();
        c.anchor = GridBagConstraints.NORTHWEST;
        c.weightx = 1;
        // Colonne 1
        this.boxAdeduire.setOpaque(false);
        this.boxAdeduire.setMinimumSize(new Dimension(430, this.boxAdeduire.getPreferredSize().height));
        this.boxAdeduire.setPreferredSize(new Dimension(430, this.boxAdeduire.getPreferredSize().height));
        panel.add(this.boxAdeduire, c);
        this.addView("ID_MODE_REGLEMENT", DEC + ";" + SEP);
        this.eltModeRegl = (ElementSQLObject) this.getView("ID_MODE_REGLEMENT");

        c.gridy++;
        c.fill = GridBagConstraints.NONE;
        c.weighty = 1;
        this.eltModeRegl.setOpaque(false);
        panel.add(this.eltModeRegl, c);

        // Colonne 2 : port et remise

        final JPanel panelPortEtRemise = new JPanel();
        panelPortEtRemise.setOpaque(false);
        panelPortEtRemise.setLayout(new GridBagLayout());

        final GridBagConstraints cFrais = new DefaultGridBagConstraints();

        DeviseField textPortHT = new DeviseField(5);
        DeviseField textRemiseHT = new DeviseField(5);

        // Frais de port
        cFrais.gridheight = 1;
        cFrais.fill = GridBagConstraints.VERTICAL;
        cFrais.weighty = 1;
        cFrais.gridx = 1;

        // FIXME implémenter la remise et les port pour les avoirs
        JLabel labelPortHT = new JLabel(getLabelFor("PORT_HT"));
        labelPortHT.setHorizontalAlignment(SwingConstants.RIGHT);
        cFrais.gridy++;
        // panelPortEtRemise.add(labelPortHT, cFrais);
        cFrais.gridx++;
        DefaultGridBagConstraints.lockMinimumSize(textPortHT);
        // panelPortEtRemise.add(textPortHT, cFrais);

        // Remise
        JLabel labelRemiseHT = new JLabel(getLabelFor("REMISE_HT"));
        labelRemiseHT.setHorizontalAlignment(SwingConstants.RIGHT);
        cFrais.gridy++;
        cFrais.gridx = 1;
        // panelPortEtRemise.add(labelRemiseHT, cFrais);
        cFrais.gridx++;
        DefaultGridBagConstraints.lockMinimumSize(textRemiseHT);
        // panelPortEtRemise.add(textRemiseHT, cFrais);
        cFrais.gridy++;

        c.gridx++;
        c.gridy = 0;
        c.gridheight = 2;
        c.weightx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;
        panel.add(panelPortEtRemise, c);

        // Colonne 3 : totaux
        final DeviseField fieldHT = new DeviseField();
        final DeviseField fieldEco = new DeviseField();
        final DeviseField fieldTVA = new DeviseField();
        final DeviseField fieldService = new DeviseField();
        final DeviseField fieldTTC = new DeviseField();
        // SQL
        addSQLObject(textPortHT, "PORT_HT");
        final DeviseField fieldDevise = new DeviseField();
        if (getTable().getFieldsName().contains("T_DEVISE"))
            addSQLObject(fieldDevise, "T_DEVISE");
        addSQLObject(textRemiseHT, "REMISE_HT");
        addSQLObject(fieldEco, "T_ECO_CONTRIBUTION");
        addRequiredSQLObject(fieldHT, "MONTANT_HT");
        addRequiredSQLObject(fieldTVA, "MONTANT_TVA");
        addRequiredSQLObject(fieldTTC, "MONTANT_TTC");
        addRequiredSQLObject(fieldService, "MONTANT_SERVICE");
        //
        JTextField poids = new JTextField();
        if (getTable().getFieldsName().contains("T_POIDS"))
            addSQLObject(poids, "T_POIDS");
        final TotalPanel totalTTC = new TotalPanel(this.table, fieldEco, fieldHT, fieldTVA, fieldTTC, textPortHT, textRemiseHT, fieldService, null, fieldDevise, poids, null);
        totalTTC.setOpaque(false);
        c.gridx++;
        c.gridy = 0;
        c.gridheight = 2;
        c.fill = GridBagConstraints.BOTH;
        panel.add(totalTTC, c);

        // Listeners
        textPortHT.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                totalTTC.updateTotal();
            }

            public void removeUpdate(DocumentEvent e) {
                totalTTC.updateTotal();
            }

            public void insertUpdate(DocumentEvent e) {
                totalTTC.updateTotal();
            }
        });

        textRemiseHT.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                totalTTC.updateTotal();
            }

            public void removeUpdate(DocumentEvent e) {
                totalTTC.updateTotal();
            }

            public void insertUpdate(DocumentEvent e) {
                totalTTC.updateTotal();
            }
        });
        return panel;
    }

    @Override
    public synchronized ValidState getValidState() {
        final ValidState selfState;
        final Date value = this.date.getValue();
        if (value != null && value.after(SocieteCommonSQLElement.getDateDebutExercice())) {
            selfState = ValidState.getTrueInstance();
        } else {
            selfState = ValidState.createCached(false, "La date est incorrecte, cette période est cloturée.");
        }
        return super.getValidState().and(selfState);
    }

    private void setCompteServiceVisible(boolean b) {
        this.compteSelService.setVisible(b);
        this.labelCompteServ.setVisible(b);
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

    private void createCompteServiceAuto(int id) {
        SQLRow rowPole = this.comboPole.getSelectedRow();
        SQLRow rowVerif = this.comboVerificateur.getSelectedRow();
        String verifInitiale = getInitialesFromVerif(rowVerif);
        int idCpt = ComptePCESQLElement.getId("706" + rowPole.getString("CODE") + verifInitiale, "Service " + rowPole.getString("NOM") + " " + rowVerif.getString("NOM"));
        SQLRowValues rowVals = this.getTable().getRow(id).createEmptyUpdateRow();
        rowVals.put("ID_COMPTE_PCE_SERVICE", idCpt);
        try {
            rowVals.update();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int insert(SQLRow order) {

        int id = getSelectedID();
        final SQLTable tableNum = this.getTable().getBase().getTable("NUMEROTATION_AUTO");
        if (this.textNumero.checkValidation()) {

            id = super.insert(order);

            try {
                this.table.updateField("ID_AVOIR_CLIENT", id);
                final SQLRow row = getTable().getRow(id);

                // incrémentation du numéro auto
                if (NumerotationAutoSQLElement.getNextNumero(getElement().getClass(), row.getDate("DATE").getTime()).equalsIgnoreCase(this.textNumero.getText().trim())) {
                    SQLRowValues rowVals = new SQLRowValues(tableNum);
                    String label = NumerotationAutoSQLElement.getLabelNumberFor(getElement().getClass());
                    int val = tableNum.getRow(2).getInt(label);
                    val++;
                    rowVals.put(label, Integer.valueOf(val));

                    try {
                        rowVals.update(2);
                    } catch (SQLException e) {

                        e.printStackTrace();
                    }
                }

                SQLRowValues rowVals2 = row.createUpdateRow();
                Long l = rowVals2.getLong("MONTANT_SOLDE");
                Long l2 = rowVals2.getLong("MONTANT_TTC");

                rowVals2.put("MONTANT_RESTANT", l2 - l);

                rowVals2.update();

                // updateStock(id);
                final int idFinal = id;
                ComptaPropsConfiguration.getInstanceCompta().getNonInteractiveSQLExecutor().execute(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            updateStock(idFinal);
                        } catch (Exception e) {
                            ExceptionHandler.handle("Update error", e);
                        }
                    }
                });

                GenerationMvtAvoirClient gen = new GenerationMvtAvoirClient(row);
                gen.genereMouvement();

                // generation du document
                createAvoirClient(row);

                useAvoir(row);
            } catch (Exception e) {
                ExceptionHandler.handle("Erreur lors de la création de l'avoir", e);
            }
        } else {
            ExceptionHandler.handle("Impossible de modifier, numéro d'avoir existant.");
            Object root = SwingUtilities.getRoot(this);
            if (root instanceof EditFrame) {
                EditFrame frame = (EditFrame) root;
                frame.getPanel().setAlwaysVisible(true);
            }
        }
        return id;
    }

    protected void createAvoirClient(final SQLRow row) {

        final AvoirClientXmlSheet bSheet = new AvoirClientXmlSheet(row);
        try {
            bSheet.createDocumentAsynchronous();
            bSheet.showPrintAndExportAsynchronous(panelGestDoc.isVisualisationSelected(), panelGestDoc.isImpressionSelected(), true);
        } catch (Exception e) {
            ExceptionHandler.handle("Impossible de créer l'avoir", e);
        }

    }

    public void useAvoir(final SQLRowAccessor r) {
        if (r.getBoolean("A_DEDUIRE") && r.getReferentRows(getTable().getTable("SAISIE_VENTE_FACTURE")).size() == 0) {
            JPanel p = new JPanel(new GridBagLayout());
            GridBagConstraints c = new DefaultGridBagConstraints();
            c.gridwidth = GridBagConstraints.REMAINDER;
            p.add(new JLabel("Voulez appliquer cet avoir sur une facture existante?"), c);
            c.gridy++;
            c.gridwidth = 1;
            p.add(new JLabel("Appliquer l'avoir sur la facture : "), c);
            c.gridx++;
            final SQLRequestComboBox box = new SQLRequestComboBox();
            final ComboSQLRequest comboRequest = getElement().getDirectory().getElement("SAISIE_VENTE_FACTURE").getComboRequest(true);
            Where w = new Where(comboRequest.getPrimaryTable().getField("ID_AVOIR_CLIENT"), "=", getTable().getUndefinedID());
            w = w.and(new Where(comboRequest.getPrimaryTable().getField("ID_CLIENT"), "=", r.getForeignID("ID_CLIENT")));
            comboRequest.setWhere(w);
            box.uiInit(comboRequest);
            p.add(box, c);
            c.gridy++;
            c.gridx = 0;
            final JButton buttonApply = new JButton("Appliquer");
            JButton buttonAnnuler = new JButton("Fermer");
            p.add(buttonApply, c);
            buttonApply.setEnabled(false);
            box.addValueListener(new PropertyChangeListener() {

                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    buttonApply.setEnabled(box.getSelectedRow() != null);

                }
            });
            c.gridx++;
            p.add(buttonAnnuler, c);
            final PanelFrame f = new PanelFrame(p, "Affection d'un avoir client");

            buttonAnnuler.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    f.dispose();
                }
            });

            buttonApply.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    SQLRow rowFacture = box.getSelectedRow();
                    long ttc = rowFacture.getLong("T_TTC");
                    long totalAvoirTTC = r.getLong("MONTANT_TTC");
                    final long totalSolde = r.getLong("MONTANT_SOLDE");
                    long totalAvoir = totalAvoirTTC - totalSolde;

                    long totalAvoirApplique = 0;
                    long netAPayer = ttc - totalAvoir;
                    if (netAPayer < 0) {
                        netAPayer = 0;
                        totalAvoirApplique = ttc;
                    } else {
                        totalAvoirApplique = totalAvoir;
                    }

                    final SQLRowValues createEmptyUpdateRow = rowFacture.createEmptyUpdateRow();
                    createEmptyUpdateRow.put("ID_AVOIR_CLIENT", r.getID());
                    createEmptyUpdateRow.put("NET_A_PAYER", netAPayer);
                    createEmptyUpdateRow.put("T_AVOIR_TTC", totalAvoirApplique);
                    try {
                        rowFacture = createEmptyUpdateRow.commit();

                        long restant = totalAvoirTTC - totalAvoirApplique;

                        SQLRowValues rowVals = r.createEmptyUpdateRow();
                        final long l2 = ttc - restant;
                        // Soldé
                        if (l2 >= 0) {
                            rowVals.put("SOLDE", Boolean.TRUE);
                            rowVals.put("MONTANT_SOLDE", totalAvoirTTC);
                            rowVals.put("MONTANT_RESTANT", 0);
                        } else {
                            // Il reste encore de l'argent pour l'avoir
                            final long m = totalSolde + ttc;
                            rowVals.put("MONTANT_SOLDE", m);
                            rowVals.put("MONTANT_RESTANT", totalAvoirTTC - m);
                        }

                        rowVals.update();
                        EcritureSQLElement eltEcr = (EcritureSQLElement) Configuration.getInstance().getDirectory().getElement("ECRITURE");
                        final int foreignIDmvt = rowFacture.getForeignID("ID_MOUVEMENT");
                        eltEcr.archiveMouvementProfondeur(foreignIDmvt, false);

                        System.err.println("Regeneration des ecritures");
                        new GenerationMvtSaisieVenteFacture(rowFacture.getID(), foreignIDmvt);
                        System.err.println("Fin regeneration");
                    } catch (SQLException e1) {
                        ExceptionHandler.handle("Erreur lors de l'affection de l'avoir sur la facture!", e1);
                    } finally {
                        f.dispose();
                    }

                }
            });
            SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {

                    FrameUtil.showPacked(f);
                }

            });
        }
    }

    @Override
    public void select(SQLRowAccessor r) {
        if (r != null) {

            // Les contacts sont filtrés en fonction du client (ID_AFFAIRE.ID_CLIENT), donc si
            // l'ID_CONTACT est changé avant ID_AFFAIRE le contact ne sera pas présent dans la combo
            // => charge en deux fois les valeurs
            final SQLRowValues rVals = r.asRowValues().deepCopy();
            final SQLRowValues vals = new SQLRowValues(r.getTable());

            vals.load(rVals, createSet("ID_CLIENT"));
            // vals a besoin de l'ID sinon incohérence entre ID_AFFAIRE et ID (eg for
            // reloadTable())
            // ne pas supprimer l'ID de rVals pour qu'on puisse UPDATE
            vals.setID(rVals.getID());
            super.select(vals);
            rVals.remove("ID_CLIENT");
            super.select(rVals);
        } else {
            super.select(r);
        }

    }

    @Override
    public void update() {
        if (this.textNumero.checkValidation()) {
            super.update();
            try {
                this.table.updateField("ID_AVOIR_CLIENT", getSelectedID());

                // On efface les anciens mouvements de stocks
                SQLRow row = getTable().getRow(getSelectedID());
                SQLElement eltMvtStock = Configuration.getInstance().getDirectory().getElement("MOUVEMENT_STOCK");
                SQLSelect sel = new SQLSelect(eltMvtStock.getTable().getBase());
                sel.addSelect(eltMvtStock.getTable().getField("ID"));
                Where w = new Where(eltMvtStock.getTable().getField("IDSOURCE"), "=", row.getID());
                Where w2 = new Where(eltMvtStock.getTable().getField("SOURCE"), "=", getTable().getName());
                sel.setWhere(w.and(w2));

                List l = (List) eltMvtStock.getTable().getBase().getDataSource().execute(sel.asString(), new ArrayListHandler());
                if (l != null) {
                    for (int i = 0; i < l.size(); i++) {
                        Object[] tmp = (Object[]) l.get(i);
                        eltMvtStock.archive(((Number) tmp[0]).intValue());
                    }
                }

                SQLRowValues rowVals2 = getTable().getRow(getSelectedID()).createUpdateRow();
                Long l2 = rowVals2.getLong("MONTANT_SOLDE");
                Long l3 = rowVals2.getLong("MONTANT_TTC");

                rowVals2.put("MONTANT_RESTANT", l3 - l2);
                rowVals2.update();

                // On met à jour le stock
                // updateStock(getSelectedID());
                final int idFinal = getSelectedID();
                ComptaPropsConfiguration.getInstanceCompta().getNonInteractiveSQLExecutor().execute(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            // Mise à jour du stock
                            updateStock(idFinal);
                        } catch (Exception e) {
                            ExceptionHandler.handle("Update error", e);
                        }
                    }
                });

                int idMvt = row.getInt("ID_MOUVEMENT");

                // on supprime tout ce qui est lié à la facture d'avoir
                System.err.println("Archivage des fils");
                EcritureSQLElement eltEcr = (EcritureSQLElement) Configuration.getInstance().getDirectory().getElement("ECRITURE");
                eltEcr.archiveMouvementProfondeur(idMvt, false);

                GenerationMvtAvoirClient gen = new GenerationMvtAvoirClient(row, idMvt);
                gen.genereMouvement();

                createAvoirClient(row);

                useAvoir(row);
            } catch (Exception e) {
                ExceptionHandler.handle("Erreur de mise à jour de l'avoir", e);
            }
        } else {
            ExceptionHandler.handle("Impossible de modifier, numéro d'avoir existant.");
            Object root = SwingUtilities.getRoot(this);
            if (root instanceof EditFrame) {
                EditFrame frame = (EditFrame) root;
                frame.getPanel().setAlwaysVisible(true);
            }
            return;
        }
    }

    // // Su^prression de la methode, retour de marchandise à gérer avec les Mouvements de stocks
    // protected String getLibelleStock(SQLRowAccessor row, SQLRowAccessor rowElt) {
    // return "Avoir client N°" + row.getString("NUMERO");
    // }
    //
    // /**
    // * Mise à jour des stocks pour chaque article composant la facture d'avoir
    // *
    // * @throws SQLException
    // */
    // private void updateStock(int id) throws SQLException {
    //
    // MouvementStockSQLElement mvtStock = (MouvementStockSQLElement)
    // Configuration.getInstance().getDirectory().getElement("MOUVEMENT_STOCK");
    // mvtStock.createMouvement(getTable().getRow(id), getTable().getTable("AVOIR_CLIENT_ELEMENT"),
    // new StockLabel() {
    // @Override
    // public String getLabel(SQLRowAccessor rowOrigin, SQLRowAccessor rowElt) {
    // return getLibelleStock(rowOrigin, rowElt);
    // }
    // }, true, true);
    //
    // }
    protected String getLibelleStock(SQLRowAccessor row, SQLRowAccessor rowElt) {
        return "Avoir client N°" + row.getString("NUMERO");
    }

    /**
     * Mise à jour des stocks pour chaque article composant du bon
     * 
     * @throws SQLException
     */
    private void updateStock(int id) throws SQLException {

        SQLRow row = getTable().getRow(id);
        final List<SQLRow> referentRows = row.getReferentRows(getTable().getTable("AVOIR_CLIENT_ELEMENT"));
        final List<SQLRow> effectiveRows = new ArrayList<SQLRow>();
        for (SQLRow sqlRow : referentRows) {
            if (sqlRow.getBoolean("RETOUR_STOCK")) {
                effectiveRows.add(sqlRow);
            }
        }
        if (effectiveRows.size() > 0) {
            StockItemsUpdater stockUpdater = new StockItemsUpdater(new StockLabel() {

                @Override
                public String getLabel(SQLRowAccessor rowOrigin, SQLRowAccessor rowElt) {

                    return getLibelleStock(rowOrigin, rowElt);
                }
            }, row, effectiveRows, TypeStockUpdate.RETOUR_AVOIR_CLIENT);

            stockUpdater.update();
        }

    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == this.boxAdeduire) {
            if (this.eltModeRegl != null) {
                boolean b = this.boxAdeduire.isSelected();
                this.eltModeRegl.setEditable((!b) ? InteractionMode.READ_WRITE : InteractionMode.DISABLED);
                this.eltModeRegl.setCreated(!b);
            }
        }
    }

    @Override
    protected void refreshAfterSelect(SQLRowAccessor rSource) {
        table.setDateDevise(date.getValue());
    }
}
