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
 
 package org.openconcerto.erp.core.sales.order.component;

import static org.openconcerto.utils.CollectionUtils.createSet;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.common.component.TransfertBaseSQLComponent;
import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.erp.core.common.element.NumerotationAutoSQLElement;
import org.openconcerto.erp.core.common.ui.DeviseField;
import org.openconcerto.erp.core.common.ui.TotalPanel;
import org.openconcerto.erp.core.customerrelationship.customer.ui.AddressChoiceUI;
import org.openconcerto.erp.core.customerrelationship.customer.ui.AdresseType;
import org.openconcerto.erp.core.finance.tax.model.TaxeCache;
import org.openconcerto.erp.core.sales.order.report.CommandeClientXmlSheet;
import org.openconcerto.erp.core.sales.order.ui.CommandeClientItemTable;
import org.openconcerto.erp.core.sales.order.ui.EtatCommandeClient;
import org.openconcerto.erp.core.sales.order.ui.EtatCommandeClientComboBox;
import org.openconcerto.erp.core.sales.order.ui.EtatCommandeRowItemView;
import org.openconcerto.erp.core.supplychain.stock.element.StockItemsUpdater;
import org.openconcerto.erp.core.supplychain.stock.element.StockItemsUpdater.TypeStockUpdate;
import org.openconcerto.erp.core.supplychain.stock.element.StockLabel;
import org.openconcerto.erp.panel.PanelOOSQLComponent;
import org.openconcerto.erp.preferences.GestionClientPreferencePanel;
import org.openconcerto.erp.preferences.GestionCommercialeGlobalPreferencePanel;
import org.openconcerto.erp.utils.TM;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLBackgroundTableCache;
import org.openconcerto.sql.model.SQLInjector;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.preferences.SQLPreferences;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.sql.sqlobject.JUniqueTextField;
import org.openconcerto.sql.sqlobject.SQLRequestComboBox;
import org.openconcerto.sql.sqlobject.SQLTextCombo;
import org.openconcerto.sql.users.UserManager;
import org.openconcerto.sql.view.EditFrame;
import org.openconcerto.sql.view.list.RowValuesTable;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.FormLayouter;
import org.openconcerto.ui.JDate;
import org.openconcerto.ui.TitledSeparator;
import org.openconcerto.ui.component.ITextArea;
import org.openconcerto.utils.ExceptionHandler;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

public class CommandeClientSQLComponent extends TransfertBaseSQLComponent {

    private CommandeClientItemTable table;
    private JUniqueTextField numeroUniqueCommande;
    private final SQLTable tableNum = getTable().getBase().getTable("NUMEROTATION_AUTO");
    private final ITextArea infos = new ITextArea(3, 3);
    private ElementComboBox comboCommercial, comboDevis, comboClient;
    private PanelOOSQLComponent panelOO;
    final JDate dateCommande = new JDate(true);
    private final boolean displayDpt;
    private final ElementComboBox comboDpt = new ElementComboBox();

    private final SQLTextCombo textObjet = new SQLTextCombo();

    public CommandeClientSQLComponent() {
        super(Configuration.getInstance().getDirectory().getElement("COMMANDE_CLIENT"));
        SQLPreferences prefs = SQLPreferences.getMemCached(getTable().getDBRoot());
        this.displayDpt = prefs.getBoolean(GestionClientPreferencePanel.DISPLAY_CLIENT_DPT, false);
    }

    public RowValuesTable getRowValuesTable() {
        return this.table.getRowValuesTable();
    }

    public void addViews() {
        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();

        // Numero du commande
        c.gridx = 0;
        this.add(new JLabel(getLabelFor("NUMERO"), SwingConstants.RIGHT), c);

        this.numeroUniqueCommande = new JUniqueTextField(16) {
            @Override
            public String getAutoRefreshNumber() {
                if (getMode() == Mode.INSERTION) {
                    return NumerotationAutoSQLElement.getNextNumero(getElement().getClass(), dateCommande.getDate());
                } else {
                    return null;
                }
            }
        };
        c.fill = GridBagConstraints.NONE;
        c.gridx++;
        c.weightx = 1;
        this.add(this.numeroUniqueCommande, c);

        // Date
        JLabel labelDate = new JLabel(getLabelFor("DATE"));
        labelDate.setHorizontalAlignment(SwingConstants.RIGHT);
        c.gridx = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0;
        this.add(labelDate, c);

        c.gridx++;
        c.fill = GridBagConstraints.NONE;
        this.add(dateCommande, c);
        dateCommande.addValueListener(new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (!isFilling() && dateCommande.getValue() != null) {
                    table.setDateDevise(dateCommande.getValue());
                }
            }
        });

        // Champ Module
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = GridBagConstraints.REMAINDER;
        final JPanel addP = ComptaSQLConfElement.createAdditionalPanel();

        this.setAdditionalFieldsPanel(new FormLayouter(addP, 2));
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        this.add(addP, c);

        c.gridy++;
        c.gridwidth = 1;

        this.comboDevis = new ElementComboBox();

        // Reference
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 1;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.fill = GridBagConstraints.HORIZONTAL;

        JLabel labelObjet = new JLabel(getLabelFor("NOM"));
        labelObjet.setHorizontalAlignment(SwingConstants.RIGHT);
        c.weightx = 0;
        this.add(labelObjet, c);

        c.gridx++;
        c.weightx = 1;
        c.fill = GridBagConstraints.BOTH;
        this.add(this.textObjet, c);

        String field;
            field = "ID_COMMERCIAL";
        c.fill = GridBagConstraints.HORIZONTAL;
        // Commercial
        JLabel labelCommercial = new JLabel(getLabelFor(field));
        labelCommercial.setHorizontalAlignment(SwingConstants.RIGHT);

        c.gridx++;
        c.weightx = 0;
        this.add(labelCommercial, c);

        this.comboCommercial = new ElementComboBox(false, 25);
        this.comboCommercial.setListIconVisible(false);
        c.gridx++;
        c.fill = GridBagConstraints.NONE;
        c.weightx = 1;
        this.add(this.comboCommercial, c);
        addRequiredSQLObject(this.comboCommercial, field);

        // Ligne 3: Client
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.add(new JLabel(getLabelFor("ID_CLIENT"), SwingConstants.RIGHT), c);

        this.comboClient = new ElementComboBox();
        c.gridx++;
        c.gridwidth = 1;
        c.weightx = 1;
        c.weighty = 0;
        c.fill = GridBagConstraints.NONE;
        this.add(this.comboClient, c);
        final ElementComboBox boxTarif = new ElementComboBox();
        this.comboClient.addValueListener(new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (!isFilling() && comboClient.getValue() != null) {
                    Integer id = comboClient.getValue();

                    if (id > 1) {

                        SQLRow row = comboClient.getElement().getTable().getRow(id);
                        if (comboClient.getElement().getTable().getFieldsName().contains("ID_TARIF")) {

                            SQLRowAccessor foreignRow = row.getForeignRow("ID_TARIF");
                            if (!foreignRow.isUndefined() && (boxTarif.getSelectedRow() == null || boxTarif.getSelectedId() != foreignRow.getID())
                                    && JOptionPane.showConfirmDialog(null, TM.tr("apply.associated.pricelist.to.customer")) == JOptionPane.YES_OPTION) {
                                boxTarif.setValue(foreignRow.getID());
                                // SaisieVenteFactureSQLComponent.this.tableFacture.setTarif(foreignRow,
                                // true);
                            } else {
                                boxTarif.setValue(foreignRow.getID());
                            }
                            // SQLRowAccessor foreignRow = row.getForeignRow("ID_TARIF");
                            // if (foreignRow.isUndefined() &&
                            // !row.getForeignRow("ID_DEVISE").isUndefined()) {
                            // SQLRowValues rowValsD = new SQLRowValues(foreignRow.getTable());
                            // rowValsD.put("ID_DEVISE", row.getObject("ID_DEVISE"));
                            // foreignRow = rowValsD;
                            //
                            // }
                            // table.setTarif(foreignRow, true);
                        }
                    }
                }

            }
        });
        addRequiredSQLObject(this.comboClient, "ID_CLIENT");

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
        if (getTable().contains("ID_CONTACT")) {
            // Contact Client
            c.gridx = 0;
            c.gridy++;
            c.gridwidth = 1;
            final JLabel labelContact = new JLabel(getLabelFor("ID_CONTACT"));
            labelContact.setHorizontalAlignment(SwingConstants.RIGHT);
            c.weightx = 0;
            c.gridwidth = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            this.add(labelContact, c);

            final ElementComboBox comboContact = new ElementComboBox();
            c.gridx++;
            c.gridwidth = 1;
            c.weightx = 0;
            c.weighty = 0;
            c.fill = GridBagConstraints.NONE;
            this.add(comboContact, c);
            final SQLElement contactElement = getElement().getForeignElement("ID_CONTACT");
            comboContact.init(contactElement, contactElement.getComboRequest(true));
            comboContact.getRequest().setWhere(Where.FALSE);
            DefaultGridBagConstraints.lockMinimumSize(comboContact);
            this.addView(comboContact, "ID_CONTACT");
            comboClient.addModelListener("wantedID", new PropertyChangeListener() {

                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    int wantedID = comboClient.getWantedID();
                    System.err.println("SET WHERE ID_CLIENT = " + wantedID);
                    if (wantedID != SQLRow.NONEXISTANT_ID && wantedID >= SQLRow.MIN_VALID_ID) {

                        final SQLRow rowClient = getTable().getForeignTable("ID_CLIENT").getRow(wantedID);
                        if (!rowClient.isForeignEmpty("ID_COMMERCIAL")) {
                            comboCommercial.setValue(rowClient.getForeignID("ID_COMMERCIAL"));
                        }
                        int idClient = rowClient.getID();
                        comboContact.getRequest().setWhere(new Where(contactElement.getTable().getField("ID_CLIENT"), "=", idClient));
                    } else {
                        comboContact.getRequest().setWhere(Where.FALSE);
                        // DevisSQLComponent.this.table.setTarif(null, false);
                    }
                }
            });

            if (getTable().contains("DATE_LIVRAISON_PREV")) {
                // Date
                JLabel labelDatePrev = new JLabel(getLabelFor("DATE_LIVRAISON_PREV"));
                labelDatePrev.setHorizontalAlignment(SwingConstants.RIGHT);
                c.gridx = 2;
                c.fill = GridBagConstraints.HORIZONTAL;
                c.weightx = 0;
                this.add(labelDatePrev, c);

                c.gridx++;
                c.fill = GridBagConstraints.NONE;
                JDate datePrev = new JDate();
                this.add(datePrev, c);
                this.addView(datePrev, "DATE_LIVRAISON_PREV");
            }

        }
        SQLPreferences prefs = SQLPreferences.getMemCached(getTable().getDBRoot());
        if (prefs.getBoolean(GestionCommercialeGlobalPreferencePanel.ADDRESS_SPEC, true)) {

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

        if (prefs.getBoolean(GestionCommercialeGlobalPreferencePanel.ORDER_PACKAGING_MANAGEMENT, true)) {
            // Emballage
            c.gridy++;
            c.gridx = 0;
            c.weightx = 0;
            c.fill = GridBagConstraints.HORIZONTAL;
            this.add(new JLabel(getLabelFor("EMBALLAGE"), SwingConstants.RIGHT), c);

            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx++;
            c.weightx = 1;
            SQLTextCombo fieldEmballage = new SQLTextCombo();
            this.add(fieldEmballage, c);
            this.addView(fieldEmballage, "EMBALLAGE");

            // N° Exp
            JLabel labelNumExp = new JLabel(getLabelFor("NUMERO_EXPEDITION"));
            labelNumExp.setHorizontalAlignment(SwingConstants.RIGHT);
            c.gridx = 2;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weightx = 0;
            this.add(labelNumExp, c);

            JTextField fieldNumExp = new JTextField();
            c.gridx++;
            c.weightx = 1;
            this.add(fieldNumExp, c);
            this.addView(fieldNumExp, "NUMERO_EXPEDITION");

            // expedition

            c.gridy++;
            c.gridx = 0;
            c.weightx = 0;
            c.weighty = 0;
            c.gridwidth = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            this.add(new JLabel(getLabelFor("TYPE_EXPEDITION"), SwingConstants.RIGHT), c);
            c.gridx++;
            c.gridwidth = 1;
            c.weightx = 1;
            c.fill = GridBagConstraints.NONE;
            SQLTextCombo tTypeExpedition = new SQLTextCombo();
            this.add(tTypeExpedition, c);
            this.addView(tTypeExpedition, "TYPE_EXPEDITION");

            // Etat
            final JLabel labelEtat = new JLabel(getLabelFor("ETAT_COMMANDE"), SwingConstants.RIGHT);
            c.gridx++;
            c.weightx = 0;
            c.fill = GridBagConstraints.HORIZONTAL;
            this.add(labelEtat, c);
            final EtatCommandeClientComboBox comboEtat = new EtatCommandeClientComboBox();
            c.gridx++;
            c.fill = GridBagConstraints.NONE;
            c.weightx = 1;
            this.add(comboEtat, c);
            addView(new EtatCommandeRowItemView(comboEtat), "ETAT_COMMANDE", REQ);
        }
        // tarif
        if (this.getTable().getFieldsName().contains("ID_TARIF")) {
            // TARIF
            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridy++;
            c.gridx = 0;
            c.weightx = 0;
            c.weighty = 0;
            c.gridwidth = 1;
            this.add(new JLabel(getLabelFor("ID_TARIF"), SwingConstants.RIGHT), c);
            c.gridx++;
            c.gridwidth = 1;

            c.weightx = 1;
            this.add(boxTarif, c);
            this.addView(boxTarif, "ID_TARIF");
            boxTarif.addModelListener("wantedID", new PropertyChangeListener() {

                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    SQLRow selectedRow = boxTarif.getRequest().getPrimaryTable().getRow(boxTarif.getWantedID());
                    table.setTarif(selectedRow, false);
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

        // Table d'élément
        this.table = new CommandeClientItemTable();
        c.fill = GridBagConstraints.BOTH;
        c.gridy++;
        c.gridx = 0;
        c.weightx = 1;
        c.weighty = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;
        this.add(this.table, c);

        DeviseField textPortHT = new DeviseField(5);
        DeviseField textRemiseHT = new DeviseField();

        // Total
        DeviseField fieldHT = new DeviseField();
        DeviseField fieldTVA = new DeviseField();
        DeviseField fieldTTC = new DeviseField();
        DeviseField fieldDevise = new DeviseField();
        DeviseField fieldService = new DeviseField();
        DeviseField fieldHA = new DeviseField();
        DeviseField fieldEco = new DeviseField();
        fieldHT.setOpaque(false);
        fieldHA.setOpaque(false);
        fieldTVA.setOpaque(false);
        fieldTTC.setOpaque(false);
        fieldService.setOpaque(false);
        addSQLObject(fieldDevise, "T_DEVISE");
        addSQLObject(fieldEco, "T_ECO_CONTRIBUTION");
        addRequiredSQLObject(fieldHT, "T_HT");
        addRequiredSQLObject(fieldTVA, "T_TVA");
        addRequiredSQLObject(fieldTTC, "T_TTC");
        addRequiredSQLObject(fieldService, "T_SERVICE");
        if (getTable().contains("PREBILAN")) {
            addSQLObject(fieldHA, "PREBILAN");
        } else if (getTable().contains("T_HA")) {
            this.allowEditable("T_HA", false);
            addSQLObject(fieldHA, "T_HA");
        }
        // Disable
        this.allowEditable("T_ECO_CONTRIBUTION", false);
        this.allowEditable("T_HT", false);
        this.allowEditable("T_TVA", false);
        this.allowEditable("T_TTC", false);
        this.allowEditable("T_SERVICE", false);

        JTextField poids = new JTextField();
        SQLRequestComboBox boxTaxePort = new SQLRequestComboBox(false, 8);
        // addSQLObject(poids, "T_POIDS");
        final TotalPanel totalTTC = new TotalPanel(this.table, fieldEco, fieldHT, fieldTVA, fieldTTC, textPortHT, textRemiseHT, fieldService, fieldHA, fieldDevise, poids, null,
                (getTable().contains("ID_TAXE_PORT") ? boxTaxePort : null), null);

        // INfos
        c.gridx = 0;
        c.gridy++;
        c.gridheight = 1;
        c.weighty = 0;
        c.weightx = 1;
        c.anchor = GridBagConstraints.WEST;
        c.gridwidth = 2;
        this.add(new TitledSeparator(getLabelFor("INFOS")), c);

        c.gridy++;
        c.weightx = 1;
        c.weighty = 0;
        c.fill = GridBagConstraints.BOTH;
        final JScrollPane scrollPane = new JScrollPane(this.infos);
        scrollPane.setBorder(null);
        this.add(scrollPane, c);

        // Poids

        final JTextField textPoidsTotal = new JTextField(8);
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints cFrais = new DefaultGridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(new JLabel(getLabelFor("T_POIDS"), JTextField.RIGHT), cFrais);
        textPoidsTotal.setEnabled(false);
        textPoidsTotal.setDisabledTextColor(Color.BLACK);
        cFrais.gridx++;
        panel.add(textPoidsTotal, cFrais);

        panel.setOpaque(false);

        DefaultGridBagConstraints.lockMinimumSize(textPortHT);
        addSQLObject(textPortHT, "PORT_HT");
        DefaultGridBagConstraints.lockMinimumSize(textRemiseHT);
        addSQLObject(textRemiseHT, "REMISE_HT");

        // Frais de port

        if (getTable().contains("ID_TAXE_PORT")) {

            JLabel labelPortHT = new JLabel(getLabelFor("PORT_HT"));
            labelPortHT.setHorizontalAlignment(SwingConstants.RIGHT);
            cFrais.gridx = 0;
            cFrais.gridy++;
            panel.add(labelPortHT, cFrais);
            cFrais.gridx++;
            panel.add(textPortHT, cFrais);

            JLabel labelTaxeHT = new JLabel(getLabelFor("ID_TAXE_PORT"));
            labelTaxeHT.setHorizontalAlignment(SwingConstants.RIGHT);
            cFrais.gridx = 0;
            cFrais.gridy++;
            panel.add(labelTaxeHT, cFrais);
            cFrais.gridx++;
            panel.add(boxTaxePort, cFrais);
            this.addView(boxTaxePort, "ID_TAXE_PORT", REQ);

            boxTaxePort.addValueListener(new PropertyChangeListener() {

                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    totalTTC.updateTotal();
                }
            });
        }

        // Remise
        JLabel labelRemiseHT = new JLabel(getLabelFor("REMISE_HT"));
        labelRemiseHT.setHorizontalAlignment(SwingConstants.RIGHT);
        cFrais.gridy++;
        cFrais.gridx = 0;
        panel.add(labelRemiseHT, cFrais);
        cFrais.gridx++;
        panel.add(textRemiseHT, cFrais);

        c.gridx = 2;
        c.weightx = 0;
        c.weighty = 0;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.NORTHEAST;
        DefaultGridBagConstraints.lockMinimumSize(panel);
        this.add(panel, c);

        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy--;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridheight = 2;
        c.anchor = GridBagConstraints.NORTHEAST;
        c.fill = GridBagConstraints.NONE;
        c.weighty = 0;

        this.add(totalTTC, c);

        this.panelOO = new PanelOOSQLComponent(this);
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.EAST;
        c.gridx = 0;
        c.gridy += 3;
        c.weightx = 0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        this.add(this.panelOO, c);

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

        addSQLObject(this.textObjet, "NOM");
        addSQLObject(textPoidsTotal, "T_POIDS");
        addRequiredSQLObject(dateCommande, "DATE");
        // addRequiredSQLObject(radioEtat, "ID_ETAT_DEVIS");
        addRequiredSQLObject(this.numeroUniqueCommande, "NUMERO");
        addSQLObject(this.infos, "INFOS");
        addSQLObject(this.comboDevis, "ID_DEVIS");

        this.numeroUniqueCommande.setText(NumerotationAutoSQLElement.getNextNumero(getElement().getClass(), new Date()));

        this.table.getModel().addTableModelListener(new TableModelListener() {

            public void tableChanged(TableModelEvent e) {
                textPoidsTotal.setText(String.valueOf(CommandeClientSQLComponent.this.table.getPoidsTotal()));
            }
        });
        DefaultGridBagConstraints.lockMinimumSize(comboClient);
        DefaultGridBagConstraints.lockMinimumSize(comboCommercial);
        DefaultGridBagConstraints.lockMinimumSize(comboDevis);
        DefaultGridBagConstraints.lockMinimumSize(totalTTC);
        DefaultGridBagConstraints.lockMaximumSize(totalTTC);
        DefaultGridBagConstraints.lockMinimumSize(numeroUniqueCommande);

    }

    public int insert(SQLRow order) {
        final int idCommande;

        int attempt = 0;
        // on verifie qu'un devis du meme numero n'a pas été inséré entre temps
        if (!this.numeroUniqueCommande.checkValidation(false)) {
            while (attempt < JUniqueTextField.RETRY_COUNT) {
                String num = NumerotationAutoSQLElement.getNextNumero(getElement().getClass(), dateCommande.getDate());
                this.numeroUniqueCommande.setText(num);
                attempt++;
                if (this.numeroUniqueCommande.checkValidation(false)) {
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
        final String num = this.numeroUniqueCommande.getText();
        if (attempt == JUniqueTextField.RETRY_COUNT) {
            idCommande = getSelectedID();
            ExceptionHandler.handle("Impossible d'ajouter, numéro de commande existant.");
            final Object root = SwingUtilities.getRoot(this);
            if (root instanceof EditFrame) {
                final EditFrame frame = (EditFrame) root;
                frame.getPanel().setAlwaysVisible(true);
            }
        } else {
            idCommande = super.insert(order);
            this.table.updateField("ID_COMMANDE_CLIENT", idCommande);
            try {
                updateStock(idCommande);
            } catch (SQLException e1) {
                ExceptionHandler.handle("Erreur lors de la mise à du stock!", e1);
            }
            // Création des articles
            this.table.createArticle(idCommande, this.getElement());
            // generation du document

            try {
                CommandeClientXmlSheet sheet = new CommandeClientXmlSheet(getTable().getRow(idCommande));
                sheet.createDocumentAsynchronous();
                sheet.showPrintAndExportAsynchronous(CommandeClientSQLComponent.this.panelOO.isVisualisationSelected(), CommandeClientSQLComponent.this.panelOO.isImpressionSelected(), true);
            } catch (Exception e) {
                ExceptionHandler.handle("Impossible de créer la commande", e);
            }

            // incrémentation du numéro auto
            if (NumerotationAutoSQLElement.getNextNumero(getElement().getClass(), new Date()).equalsIgnoreCase(this.numeroUniqueCommande.getText().trim())) {
                SQLRowValues rowVals = new SQLRowValues(this.tableNum);
                int val = this.tableNum.getRow(2).getInt("COMMANDE_CLIENT_START");
                val++;
                rowVals.put("COMMANDE_CLIENT_START", new Integer(val));

                try {
                    rowVals.update(2);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (attempt > 0) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        JOptionPane.showMessageDialog(null, "Le numéro a été actualisé en " + num);
                    }
                });
            }
        }

        return idCommande;
    }

    @Override
    public Set<String> getPartialResetNames() {
        Set<String> s = new HashSet<String>();
        s.add("NOM");
        s.add("NUMERO");
        s.add("INFOS");
        s.add("ID_CLIENT");
        if (getTable().contains("ACOMPTE_COMMANDE")) {
            s.add("ACOMPTE_COMMANDE");
        }
        return s;
    }

    @Override
    public void select(SQLRowAccessor r) {
        if (r == null || r.getIDNumber() == null)
            super.select(r);
        else {
            System.err.println(r);
            final SQLRowValues rVals = r.asRowValues().deepCopy();
            final SQLRowValues vals = new SQLRowValues(r.getTable());
            vals.load(rVals, createSet("ID_CLIENT"));
            vals.setID(rVals.getID());
            System.err.println("Select CLIENT");
            super.select(vals);
            rVals.remove("ID_CLIENT");
            super.select(rVals);
        }
        if (r != null) {
            this.table.insertFrom("ID_COMMANDE_CLIENT", r.getID());
        }
        // this.radioEtat.setVisible(r.getID() > 1);
    }

    @Override
    public void update() {

        if (!this.numeroUniqueCommande.checkValidation()) {
            ExceptionHandler.handle("Impossible d'ajouter, numéro de commande client existant.");
            Object root = SwingUtilities.getRoot(this);
            if (root instanceof EditFrame) {
                EditFrame frame = (EditFrame) root;
                frame.getPanel().setAlwaysVisible(true);
            }
            return;
        }
        super.update();
        final int id = getSelectedID();
        this.table.updateField("ID_COMMANDE_CLIENT", id);
        this.table.createArticle(id, this.getElement());

        final SQLRow row = getTable().getRow(id);
        try {
            updateStock(id);
        } catch (SQLException e1) {
            ExceptionHandler.handle("Erreur lors de la mise à du stock!", e1);
        }

        // generation du document
        try {
            CommandeClientXmlSheet sheet = new CommandeClientXmlSheet(row);
            sheet.createDocumentAsynchronous();
            sheet.showPrintAndExportAsynchronous(CommandeClientSQLComponent.this.panelOO.isVisualisationSelected(), CommandeClientSQLComponent.this.panelOO.isImpressionSelected(), true);
        } catch (Exception e) {
            ExceptionHandler.handle("Impossible de créer la commande", e);
        }

    }

    protected String getLibelleStock(SQLRowAccessor row, SQLRowAccessor rowElt) {
        return "Commande client N°" + row.getString("NUMERO");
    }

    /**
     * Mise à jour des stocks pour chaque article composant la facture
     * 
     * @throws SQLException
     */
    private void updateStock(int id) throws SQLException {

        SQLRow row = getTable().getRow(id);
        StockItemsUpdater stockUpdater = new StockItemsUpdater(new StockLabel() {
            @Override
            public String getLabel(SQLRowAccessor rowOrigin, SQLRowAccessor rowElt) {
                return getLibelleStock(rowOrigin, rowElt);
            }
        }, row, row.getReferentRows(getTable().getTable("COMMANDE_CLIENT_ELEMENT")), TypeStockUpdate.VIRTUAL_DELIVER);

        stockUpdater.update();
    }

    public void setDefaults() {
        this.resetValue();
        this.numeroUniqueCommande.setText(NumerotationAutoSQLElement.getNextNumero(getElement().getClass(), new Date()));
        this.table.getModel().clearRows();
    }

    /**
     * Création d'une commande à partir d'un devis
     * 
     * @param idDevis
     * 
     */
    public void loadDevis(int idDevis) {

        SQLElement devis = Configuration.getInstance().getDirectory().getElement("DEVIS");
        SQLElement devisElt = Configuration.getInstance().getDirectory().getElement("DEVIS_ELEMENT");

        if (idDevis > 1) {
            SQLInjector injector = SQLInjector.getInjector(devis.getTable(), this.getTable());
            SQLRowValues createRowValuesFrom = injector.createRowValuesFrom(idDevis);
            SQLRow rowDevis = devis.getTable().getRow(idDevis);

            String string = rowDevis.getString("OBJET");
            createRowValuesFrom.put("NOM", string + (string.trim().length() == 0 ? "" : ",") + rowDevis.getString("NUMERO"));
            this.select(createRowValuesFrom);
        }

        loadItem(this.table, devis, idDevis, devisElt);
    }

    @Override
    protected SQLRowValues createDefaults() {
        SQLRowValues rowVals = new SQLRowValues(getTable());
        rowVals.put("T_POIDS", 0.0F);
        rowVals.put("NUMERO", NumerotationAutoSQLElement.getNextNumero(getElement().getClass(), new Date()));
        // User
        // SQLSelect sel = new SQLSelect(Configuration.getInstance().getBase());
        SQLElement eltComm = Configuration.getInstance().getDirectory().getElement("COMMERCIAL");
        int idUser = UserManager.getInstance().getCurrentUser().getId();
        //
        // sel.addSelect(eltComm.getTable().getKey());
        // sel.setWhere(new Where(eltComm.getTable().getField("ID_USER_COMMON"), "=", idUser));
        // List<SQLRow> rowsComm = (List<SQLRow>)
        // Configuration.getInstance().getBase().getDataSource().execute(sel.asString(), new
        // SQLRowListRSH(eltComm.getTable()));
        SQLRow rowsComm = SQLBackgroundTableCache.getInstance().getCacheForTable(eltComm.getTable()).getFirstRowContains(idUser, eltComm.getTable().getField("ID_USER_COMMON"));

        if (rowsComm != null) {
            rowVals.put("ID_COMMERCIAL", rowsComm.getID());
        }
        if (getTable().contains("ETAT_COMMANDE")) {
            rowVals.put("ETAT_COMMANDE", EtatCommandeClient.A_PREPARER.getId());
        }
        if (getTable().contains("ID_TAXE_PORT")) {
            SQLRow taxeDefault = TaxeCache.getCache().getFirstTaxe();
            rowVals.put("ID_TAXE_PORT", taxeDefault.getID());
        }

        return rowVals;
    }

    @Override
    protected void refreshAfterSelect(SQLRowAccessor r) {
        if (this.dateCommande.getValue() != null && r.getObject("DATE") != null) {
            this.table.setDateDevise(r.getDate("DATE").getTime());
        }
    }
}
