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
 
 package org.openconcerto.erp.core.sales.shipment.component;

import static org.openconcerto.utils.CollectionUtils.createSet;

import org.openconcerto.erp.core.common.component.TransfertBaseSQLComponent;
import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.erp.core.common.element.NumerotationAutoSQLElement;
import org.openconcerto.erp.core.common.ui.DeviseField;
import org.openconcerto.erp.core.common.ui.TotalPanel;
import org.openconcerto.erp.core.customerrelationship.customer.ui.AddressChoiceUI;
import org.openconcerto.erp.core.customerrelationship.customer.ui.AdresseType;
import org.openconcerto.erp.core.finance.tax.model.TaxeCache;
import org.openconcerto.erp.core.sales.invoice.element.SaisieVenteFactureItemSQLElement;
import org.openconcerto.erp.core.sales.product.ui.ReliquatRowValuesTable;
import org.openconcerto.erp.core.sales.shipment.element.BonDeLivraisonItemSQLElement;
import org.openconcerto.erp.core.sales.shipment.element.BonDeLivraisonSQLElement;
import org.openconcerto.erp.core.sales.shipment.report.BonLivraisonXmlSheet;
import org.openconcerto.erp.core.sales.shipment.ui.BonDeLivraisonItemTable;
import org.openconcerto.erp.core.supplychain.stock.element.StockItemsUpdater;
import org.openconcerto.erp.core.supplychain.stock.element.StockItemsUpdater.TypeStockUpdate;
import org.openconcerto.erp.core.supplychain.stock.element.StockLabel;
import org.openconcerto.erp.panel.PanelOOSQLComponent;
import org.openconcerto.erp.preferences.GestionArticleGlobalPreferencePanel;
import org.openconcerto.erp.preferences.GestionClientPreferencePanel;
import org.openconcerto.erp.preferences.GestionCommercialeGlobalPreferencePanel;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.preferences.SQLPreferences;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.sql.sqlobject.JUniqueTextField;
import org.openconcerto.sql.sqlobject.SQLRequestComboBox;
import org.openconcerto.sql.view.EditFrame;
import org.openconcerto.sql.view.list.RowValuesTable;
import org.openconcerto.sql.view.list.RowValuesTableModel;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.FormLayouter;
import org.openconcerto.ui.JDate;
import org.openconcerto.ui.TitledSeparator;
import org.openconcerto.ui.component.ITextArea;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.NumberUtils;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

public class BonDeLivraisonSQLComponent extends TransfertBaseSQLComponent {
    private BonDeLivraisonItemTable tableBonItem;
    private ReliquatRowValuesTable tableBonReliquatItem;
    private ElementComboBox selectCommande, comboClient;
    private PanelOOSQLComponent panelOO;
    private JUniqueTextField textNumeroUnique;
    private final SQLTable tableNum = getTable().getBase().getTable("NUMEROTATION_AUTO");
    private final DeviseField textTotalHT = new DeviseField(6);
    private final DeviseField textTotalTVA = new DeviseField(6);
    private final DeviseField textTotalTTC = new DeviseField(6);
    private final JTextField textPoidsTotal = new JTextField(6);
    private final JTextField textNom = new JTextField(25);
    private final JDate date = new JDate(true);
    private final boolean displayDpt;
    private final ElementComboBox comboDpt = new ElementComboBox();

    public BonDeLivraisonSQLComponent() {
        super(Configuration.getInstance().getDirectory().getElement("BON_DE_LIVRAISON"));
        SQLPreferences prefs = SQLPreferences.getMemCached(getTable().getDBRoot());
        this.displayDpt = prefs.getBoolean(GestionClientPreferencePanel.DISPLAY_CLIENT_DPT, false);
    }

    @Override
    protected RowValuesTable getRowValuesTable() {
        return this.tableBonItem.getRowValuesTable();
    }

    @Override
    protected SQLRowValues createDefaults() {
        this.textNumeroUnique.setText(NumerotationAutoSQLElement.getNextNumero(getElement().getClass()));
        this.tableBonItem.getModel().clearRows();
        SQLRowValues rowVals = super.createDefaults();
        if (rowVals == null) {
            rowVals = new SQLRowValues(getTable());
        }
        if (getTable().contains("CREATE_VIRTUAL_STOCK")) {
            rowVals.put("CREATE_VIRTUAL_STOCK", Boolean.TRUE);
        }
        if (getTable().contains("ID_TAXE_PORT")) {
            SQLRow taxeDefault = TaxeCache.getCache().getFirstTaxe();
            rowVals.put("ID_TAXE_PORT", taxeDefault.getID());
        }
        return rowVals;
    }

    public void addViews() {
        this.textTotalHT.setOpaque(false);
        this.textTotalTVA.setOpaque(false);
        this.textTotalTTC.setOpaque(false);
        if (getTable().contains("CREATE_VIRTUAL_STOCK")) {
            this.addView(new JCheckBox(), "CREATE_VIRTUAL_STOCK");
        }
        this.selectCommande = new ElementComboBox();

        this.setLayout(new GridBagLayout());

        final GridBagConstraints c = new DefaultGridBagConstraints();

        // Champ Module
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = GridBagConstraints.REMAINDER;
        final JPanel addP = ComptaSQLConfElement.createAdditionalPanel();
        this.setAdditionalFieldsPanel(new FormLayouter(addP, 2));
        this.add(addP, c);

        c.gridy++;
        c.gridwidth = 1;

        // Numero
        JLabel labelNum = new JLabel(getLabelFor("NUMERO"));
        labelNum.setHorizontalAlignment(SwingConstants.RIGHT);
        this.add(labelNum, c);

        this.textNumeroUnique = new JUniqueTextField(16) {
            @Override
            public String getAutoRefreshNumber() {
                if (getMode() == Mode.INSERTION) {
                    return NumerotationAutoSQLElement.getNextNumero(getElement().getClass(), date.getDate());
                } else {
                    return null;
                }
            }
        };
        c.gridx++;
        c.weightx = 1;
        c.weighty = 0;
        c.fill = GridBagConstraints.NONE;
        DefaultGridBagConstraints.lockMinimumSize(textNumeroUnique);
        this.add(this.textNumeroUnique, c);

        // Date
        c.gridx++;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0;
        this.add(new JLabel(getLabelFor("DATE"), SwingConstants.RIGHT), c);

        c.gridx++;
        c.weightx = 0;
        c.weighty = 0;
        c.fill = GridBagConstraints.NONE;
        this.add(date, c);

        this.date.addValueListener(new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (!isFilling() && date.getValue() != null) {
                    tableBonItem.setDateDevise(date.getValue());
                }
            }
        });

        // Reference
        c.gridy++;
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.add(new JLabel(getLabelFor("NOM"), SwingConstants.RIGHT), c);
        c.gridx++;
        c.weightx = 1;
        this.add(this.textNom, c);
        if (getTable().contains("DATE_LIVRAISON")) {
            // Date livraison
            c.gridx++;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weightx = 0;
            this.add(new JLabel(getLabelFor("DATE_LIVRAISON"), SwingConstants.RIGHT), c);

            JDate dateLivraison = new JDate(true);
            c.gridx++;
            c.weightx = 0;
            c.weighty = 0;
            c.fill = GridBagConstraints.NONE;
            this.add(dateLivraison, c);
            this.addView(dateLivraison, "DATE_LIVRAISON");
        }
        // Client
        JLabel labelClient = new JLabel(getLabelFor("ID_CLIENT"), SwingConstants.RIGHT);
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weighty = 0;
        this.add(labelClient, c);

        c.gridx++;
        c.weightx = 0;
        c.weighty = 0;
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
        if (getTable().contains("SPEC_LIVRAISON")) {
            // Date livraison
            c.gridx++;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weightx = 0;
            this.add(new JLabel(getLabelFor("SPEC_LIVRAISON"), SwingConstants.RIGHT), c);

            JTextField specLivraison = new JTextField();
            c.gridx++;
            c.weightx = 0;
            c.weighty = 0;
            this.add(specLivraison, c);
            this.addView(specLivraison, "SPEC_LIVRAISON");
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
                        int idClient = rowClient.getID();
                        comboContact.getRequest().setWhere(new Where(contactElement.getTable().getField("ID_CLIENT"), "=", idClient));
                    } else {
                        comboContact.getRequest().setWhere(Where.FALSE);
                        // DevisSQLComponent.this.table.setTarif(null, false);
                    }
                }
            });

        }

        final ElementComboBox boxTarif = new ElementComboBox();
        this.comboClient.addValueListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (comboClient.getElement().getTable().contains("ID_TARIF")) {
                    if (BonDeLivraisonSQLComponent.this.isFilling())
                        return;
                    final SQLRow row = ((SQLRequestComboBox) evt.getSource()).getSelectedRow();
                    if (row != null) {
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
                }
            }
        });

        // Bouton tout livrer
        JButton boutonAll = new JButton("Tout livrer");

        boutonAll.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                RowValuesTableModel m = BonDeLivraisonSQLComponent.this.tableBonItem.getModel();

                // on livre tout les éléments
                for (int i = 0; i < m.getRowCount(); i++) {
                    SQLRowValues rowVals = m.getRowValuesAt(i);
                    Object o = rowVals.getObject("QTE");
                    int qte = o == null ? 0 : ((Number) o).intValue();
                    m.putValue(qte, i, "QTE_LIVREE");
                }
            }
        });

        // Tarif
        if (this.getTable().getFieldsName().contains("ID_TARIF")) {
            // TARIF
            c.gridy++;
            c.gridx = 0;
            c.weightx = 0;
            c.weighty = 0;
            c.gridwidth = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            this.add(new JLabel(getLabelFor("ID_TARIF"), SwingUtilities.RIGHT), c);
            c.gridx++;
            c.gridwidth = 1;
            c.fill = GridBagConstraints.NONE;
            c.weightx = 1;
            this.add(boxTarif, c);
            this.addView(boxTarif, "ID_TARIF");
            boxTarif.addModelListener("wantedID", new PropertyChangeListener() {

                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    SQLRow selectedRow = boxTarif.getRequest().getPrimaryTable().getRow(boxTarif.getWantedID());
                    tableBonItem.setTarif(selectedRow, !isFilling());
                }
            });
        }

        if (getTable().contains("A_ATTENTION")) {
            // Date livraison
            c.gridx++;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weightx = 0;
            this.add(new JLabel(getLabelFor("A_ATTENTION"), SwingConstants.RIGHT), c);

            JTextField specLivraison = new JTextField();
            c.gridx++;
            c.weightx = 0;
            c.weighty = 0;
            this.add(specLivraison, c);
            this.addView(specLivraison, "A_ATTENTION");
        }

        // Element du bon
        List<JButton> l = new ArrayList<JButton>();
        l.add(boutonAll);
        this.tableBonItem = new BonDeLivraisonItemTable(l);

        c.gridx = 0;
        c.gridy++;
        c.weightx = 1;
        c.weighty = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.BOTH;
        this.add(this.tableBonItem, c);
        c.anchor = GridBagConstraints.EAST;
        // Totaux
        reconfigure(this.textTotalHT);
        reconfigure(this.textTotalTVA);
        reconfigure(this.textTotalTTC);
        DeviseField fieldEco = new DeviseField(5);
        DeviseField textPortHT = new DeviseField(5);
        DeviseField textRemiseHT = new DeviseField();

        // Total
        DeviseField fieldDevise = new DeviseField();
        DeviseField fieldService = new DeviseField();
        DeviseField fieldHA = new DeviseField();
        fieldHA.setOpaque(false);
        fieldService.setOpaque(false);
        if (getTable().contains("TOTAL_DEVISE")) {
            addSQLObject(fieldDevise, "TOTAL_DEVISE");
            addRequiredSQLObject(fieldService, "TOTAL_SERVICE");
        }
        if (getTable().contains("PREBILAN")) {
            addSQLObject(fieldHA, "PREBILAN");
        } else if (getTable().contains("T_HA")) {
            addSQLObject(fieldHA, "T_HA");
            this.allowEditable("T_HA", false);
        }
        // Disable

        SQLRequestComboBox boxTaxePort = new SQLRequestComboBox(false, 8);

        // Poids Total
        c.gridy++;
        c.gridx = 1;
        c.weightx = 0;
        c.weighty = 0;
        c.anchor = GridBagConstraints.EAST;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        this.addSQLObject(this.textPoidsTotal, "TOTAL_POIDS");
        this.addSQLObject(fieldEco, "T_ECO_CONTRIBUTION");
        this.addRequiredSQLObject(this.textTotalHT, "TOTAL_HT");
        this.addRequiredSQLObject(this.textTotalTVA, "TOTAL_TVA");
        this.addRequiredSQLObject(this.textTotalTTC, "TOTAL_TTC");
        this.allowEditable("T_ECO_CONTRIBUTION", false);
        this.allowEditable("TOTAL_HT", false);
        this.allowEditable("TOTAL_TVA", false);
        this.allowEditable("TOTAL_TTC", false);
        this.allowEditable("TOTAL_POIDS", false);
        final TotalPanel panelTotal = new TotalPanel(tableBonItem, fieldEco, textTotalHT, textTotalTVA, textTotalTTC, textPortHT, textRemiseHT, fieldService, fieldHA, fieldDevise, this.textPoidsTotal,
                null, (getTable().contains("ID_TAXE_PORT") ? boxTaxePort : null), null);

        // if (b) {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints cFrais = new DefaultGridBagConstraints();
        panel.add(new JLabel(getLabelFor("TOTAL_POIDS")), cFrais);

        this.textPoidsTotal.setEnabled(false);
        this.textPoidsTotal.setHorizontalAlignment(JTextField.RIGHT);
        this.textPoidsTotal.setDisabledTextColor(Color.BLACK);
        cFrais.gridx++;
        panel.add(this.textPoidsTotal, cFrais);

        panel.setOpaque(false);
        DefaultGridBagConstraints.lockMinimumSize(panel);

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
                    panelTotal.updateTotal();
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

        c.gridx = 1;
        c.weightx = 0;
        c.weighty = 0;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.NORTHEAST;
        this.add(panel, c);

        c.gridx = 2;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 0;
        c.weighty = 0;
        c.anchor = GridBagConstraints.EAST;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.add(panelTotal, c);

        c.anchor = GridBagConstraints.WEST;

        if (getTable().getDBRoot().contains("RELIQUAT_BL")) {

            c.gridwidth = GridBagConstraints.REMAINDER;
            c.weightx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 0;
            c.gridy++;
            TitledSeparator sep = new TitledSeparator("Reliquat de kits");
            c.insets = new Insets(10, 2, 1, 2);
            this.add(sep, c);
            c.insets = new Insets(2, 2, 1, 2);

            // Reliquat du bon
            this.tableBonReliquatItem = new ReliquatRowValuesTable("RELIQUAT_BL");
            c.gridx = 0;
            c.gridy++;
            c.weightx = 1;
            c.weighty = 1;
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.fill = GridBagConstraints.BOTH;
            this.add(this.tableBonReliquatItem, c);
            this.tableBonItem.setReliquatTable(tableBonReliquatItem);
        }

        /*******************************************************************************************
         * * INFORMATIONS COMPLEMENTAIRES
         ******************************************************************************************/
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy++;
        TitledSeparator sep = new TitledSeparator(getLabelFor("INFOS"));
        c.insets = new Insets(10, 2, 1, 2);
        this.add(sep, c);
        c.insets = new Insets(2, 2, 1, 2);

        ITextArea textInfos = new ITextArea(4, 4);

        c.gridx = 0;
        c.gridy++;
        c.gridheight = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 1;
        c.weighty = 0;
        c.fill = GridBagConstraints.BOTH;

        final JScrollPane scrollPane = new JScrollPane(textInfos);
        this.add(scrollPane, c);
        textInfos.setBorder(null);
        DefaultGridBagConstraints.lockMinimumSize(scrollPane);

        c.gridx = 0;
        c.gridy++;
        c.gridheight = 1;
        c.gridwidth = 4;
        c.weightx = 0;
        c.weighty = 0;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.EAST;

        this.panelOO = new PanelOOSQLComponent(this);
        this.add(this.panelOO, c);

        this.addRequiredSQLObject(date, "DATE");
        this.addSQLObject(textInfos, "INFOS");
        this.addSQLObject(this.textNom, "NOM");
        this.addSQLObject(this.selectCommande, "ID_COMMANDE_CLIENT");
        this.addRequiredSQLObject(this.textNumeroUnique, "NUMERO");

        // Doit etre locké a la fin
        DefaultGridBagConstraints.lockMinimumSize(comboClient);

        textPortHT.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                panelTotal.updateTotal();
            }

            public void removeUpdate(DocumentEvent e) {
                panelTotal.updateTotal();
            }

            public void insertUpdate(DocumentEvent e) {
                panelTotal.updateTotal();
            }
        });

        textRemiseHT.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                panelTotal.updateTotal();
            }

            public void removeUpdate(DocumentEvent e) {
                panelTotal.updateTotal();
            }

            public void insertUpdate(DocumentEvent e) {
                panelTotal.updateTotal();
            }
        });

    }

    public BonDeLivraisonItemTable getTableBonItem() {
        return this.tableBonItem;
    }

    private void reconfigure(JTextField field) {
        field.setEnabled(false);
        field.setHorizontalAlignment(JTextField.RIGHT);
        field.setDisabledTextColor(Color.BLACK);
        field.setBorder(null);
    }

    public int insert(SQLRow order) {

        int idBon = getSelectedID();
        int attempt = 0;
        // on verifie qu'un devis du meme numero n'a pas été inséré entre temps
        if (!this.textNumeroUnique.checkValidation(false)) {
            while (attempt < JUniqueTextField.RETRY_COUNT) {
                String num = NumerotationAutoSQLElement.getNextNumero(getElement().getClass(), date.getDate());
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
            idBon = getSelectedID();
            ExceptionHandler.handle("Impossible d'ajouter, numéro de bon existant.");
            final Object root = SwingUtilities.getRoot(this);
            if (root instanceof EditFrame) {
                final EditFrame frame = (EditFrame) root;
                frame.getPanel().setAlwaysVisible(true);
            }
        } else {
            idBon = super.insert(order);
            if (this.tableBonReliquatItem != null) {
                this.tableBonReliquatItem.updateField("ID_BON_DE_LIVRAISON_ORIGINE", idBon);
            }
            this.tableBonItem.updateField("ID_BON_DE_LIVRAISON", idBon);
            this.tableBonItem.createArticle(idBon, this.getElement());
            ((BonDeLivraisonSQLElement) getElement()).updateCmdClientElement(((BonDeLivraisonSQLElement) getElement()).getCmdClientFrom(idBon), idBon);
            // generation du document
            BonLivraisonXmlSheet bSheet = new BonLivraisonXmlSheet(getTable().getRow(idBon));
            bSheet.createDocumentAsynchronous();
            bSheet.showPrintAndExportAsynchronous(this.panelOO.isVisualisationSelected(), this.panelOO.isImpressionSelected(), true);

            // incrémentation du numéro auto
            if (NumerotationAutoSQLElement.getNextNumero(getElement().getClass()).equalsIgnoreCase(this.textNumeroUnique.getText().trim())) {
                SQLRowValues rowVals = new SQLRowValues(this.tableNum);
                int val = this.tableNum.getRow(2).getInt("BON_L_START");
                val++;
                rowVals.put("BON_L_START", new Integer(val));

                try {
                    rowVals.update(2);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

            SQLPreferences prefs = new SQLPreferences(getTable().getDBRoot());

            if (!prefs.getBoolean(GestionArticleGlobalPreferencePanel.STOCK_FACT, true)) {

                try {
                    updateStock(idBon);
                } catch (SQLException e) {
                    throw new IllegalStateException(e);
                }
            }
            // updateQte(idBon);
            if (attempt > 0) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        JOptionPane.showMessageDialog(null, "Le numéro a été actualisé en " + num);
                    }
                });
            }
        }

        return idBon;
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
        if (this.tableBonReliquatItem != null) {
            this.tableBonReliquatItem.getRowValuesTable().clear();
            if (r != null) {
                this.tableBonReliquatItem.getRowValuesTable().insertFrom("ID_BON_LIVRAISON_ORIGINE", r.asRowValues());
            }
        }
    }

    @Override
    public void update() {
        if (!this.textNumeroUnique.checkValidation()) {
            ExceptionHandler.handle("Impossible d'ajouter, numéro de bon de livraison existant.");
            Object root = SwingUtilities.getRoot(this);
            if (root instanceof EditFrame) {
                EditFrame frame = (EditFrame) root;
                frame.getPanel().setAlwaysVisible(true);
            }
            return;
        }
        super.update();
        if (tableBonReliquatItem != null) {
            this.tableBonReliquatItem.updateField("ID_BON_DE_LIVRAISON_ORIGINE", getSelectedID());
        }
        final List<Object> cmdClientFrom = ((BonDeLivraisonSQLElement) getElement()).getCmdClientFrom(getSelectedID());
        this.tableBonItem.updateField("ID_BON_DE_LIVRAISON", getSelectedID());
        this.tableBonItem.createArticle(getSelectedID(), this.getElement());
        ((BonDeLivraisonSQLElement) getElement()).updateCmdClientElement(cmdClientFrom, getSelectedID());

        // generation du document
        BonLivraisonXmlSheet bSheet = new BonLivraisonXmlSheet(getTable().getRow(getSelectedID()));
        bSheet.createDocumentAsynchronous();
        bSheet.showPrintAndExportAsynchronous(this.panelOO.isVisualisationSelected(), this.panelOO.isImpressionSelected(), true);

        SQLPreferences prefs = new SQLPreferences(getTable().getDBRoot());
        SQLElement eltMvtStock = Configuration.getInstance().getDirectory().getElement("MOUVEMENT_STOCK");
        if (!prefs.getBoolean(GestionArticleGlobalPreferencePanel.STOCK_FACT, true)) {

            try {
                updateStock(getSelectedID());
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
        }

    }

    /**
     * Chargement des qtés restantes à livrer
     * 
     * @param l
     */
    public void loadQuantity(List<SQLRowValues> l) {
        Map<Integer, SQLRowValues> map = new HashMap<Integer, SQLRowValues>();
        for (SQLRowValues sqlRowValues : l) {
            if (!sqlRowValues.isForeignEmpty("ID_ARTICLE")) {
                final int foreignID = sqlRowValues.getForeignID("ID_ARTICLE");
                if (!map.containsKey(foreignID)) {
                    map.put(foreignID, sqlRowValues);
                } else {
                    SQLRowValues vals = map.get(foreignID);
                    if (sqlRowValues.getInt("QTE_LIVREE") > 0) {
                        if (NumberUtils.areNumericallyEqual(sqlRowValues.getBigDecimal("QTE_UNITAIRE"), BigDecimal.ONE) || sqlRowValues.getInt("QTE_LIVREE") > 1) {
                            vals.put("QTE_LIVREE", vals.getInt("QTE_LIVREE") + sqlRowValues.getInt("QTE_LIVREE"));
                        } else {
                            vals.put("QTE_UNITAIRE", vals.getBigDecimal("QTE_UNITAIRE").add(sqlRowValues.getBigDecimal("QTE_UNITAIRE")));
                        }
                    }
                }
            }
        }
        int count = this.tableBonItem.getModel().getRowCount();
        for (int i = 0; i < count; i++) {
            final SQLRowValues rowValuesAt = this.tableBonItem.getModel().getRowValuesAt(i);
            rowValuesAt.put("QTE_LIVREE", rowValuesAt.getObject("QTE"));
        }

        for (int i = 0; i < count; i++) {
            SQLRowValues r = this.tableBonItem.getModel().getRowValuesAt(i);
            SQLRowValues rowTR = map.get(r.getForeignID("ID_ARTICLE"));
            if (rowTR != null && !rowTR.isUndefined()) {
                if (r.getInt("QTE_LIVREE") > 0 && rowTR.getInt("QTE_LIVREE") > 0) {
                    if (NumberUtils.areNumericallyEqual(r.getBigDecimal("QTE_UNITAIRE"), BigDecimal.ONE) || r.getInt("QTE_LIVREE") > 1) {
                        this.tableBonItem.getModel().putValue(r.getInt("QTE_LIVREE") - rowTR.getInt("QTE_LIVREE"), i, "QTE_LIVREE");
                    } else {
                        this.tableBonItem.getModel().putValue(r.getBigDecimal("QTE_UNITAIRE").subtract(rowTR.getBigDecimal("QTE_UNITAIRE")), i, "QTE_UNITAIRE");
                    }
                }
            } else {
                this.tableBonItem.getModel().putValue(r.getObject("QTE"), i, "QTE_LIVREE");
            }
        }
    }

    /***********************************************************************************************
     * Mise à jour des quantités livrées dans les élements de facture
     * 
     * @param idBon id du bon de livraison
     * @throws SQLException
     */
    public void updateQte(int idBon) throws SQLException {

        SQLTable tableFactureElem = new SaisieVenteFactureItemSQLElement().getTable();
        SQLSelect selBonItem = new SQLSelect(getTable().getBase());
        BonDeLivraisonItemSQLElement bonElt = new BonDeLivraisonItemSQLElement();
        selBonItem.addSelect(bonElt.getTable().getField("ID_SAISIE_VENTE_FACTURE_ELEMENT"));
        selBonItem.addSelect(bonElt.getTable().getField("QTE_LIVREE"));
        selBonItem.setWhere(bonElt.getTable().getField("ID_BON_DE_LIVRAISON"), "=", idBon);

        String reqBonItem = selBonItem.asString();
        Object obBonItem = getTable().getBase().getDataSource().execute(reqBonItem, new ArrayListHandler());

        final List<Object[]> myListBonItem = (List<Object[]>) obBonItem;
        final int size = myListBonItem.size();

        for (int i = 0; i < size; i++) {
            final Object[] objTmp = myListBonItem.get(i);
            final SQLRow rowFactElem = tableFactureElem.getRow(((Number) objTmp[0]).intValue());
            final SQLRowValues rowVals = new SQLRowValues(tableFactureElem);
            rowVals.put("QTE_LIVREE", Integer.valueOf(rowFactElem.getInt("QTE_LIVREE") + ((Number) objTmp[1]).intValue()));
            rowVals.update(rowFactElem.getID());
        }

    }

    /***********************************************************************************************
     * Mise à jour des quantités livrées dans les élements de facture
     * 
     * @param idBon id du bon de livraison
     * @throws SQLException
     */
    public void cancelUpdateQte(int idBon) throws SQLException {

        SQLTable tableFactureElem = new SaisieVenteFactureItemSQLElement().getTable();
        SQLSelect selBonItem = new SQLSelect(getTable().getBase());
        BonDeLivraisonItemSQLElement bonElt = new BonDeLivraisonItemSQLElement();
        selBonItem.addSelect(bonElt.getTable().getField("ID_SAISIE_VENTE_FACTURE_ELEMENT"));
        selBonItem.addSelect(bonElt.getTable().getField("QTE_LIVREE"));
        selBonItem.setWhere(bonElt.getTable().getField("ID_BON_DE_LIVRAISON"), "=", idBon);

        String reqBonItem = selBonItem.asString();
        Object obBonItem = getTable().getBase().getDataSource().execute(reqBonItem, new ArrayListHandler());

        final List<Object[]> myListBonItem = (List<Object[]>) obBonItem;
        final int size = myListBonItem.size();

        for (int i = 0; i < size; i++) {
            final Object[] objTmp = myListBonItem.get(i);
            final SQLRow rowFactElem = tableFactureElem.getRow(((Number) objTmp[0]).intValue());
            final SQLRowValues rowVals = new SQLRowValues(tableFactureElem);
            rowVals.put("QTE_LIVREE", Integer.valueOf(((Number) objTmp[1]).intValue() - rowFactElem.getInt("QTE_LIVREE")));
            rowVals.update(rowFactElem.getID());
        }

    }

    protected String getLibelleStock(SQLRowAccessor row, SQLRowAccessor rowElt) {
        return "BL N°" + row.getString("NUMERO");
    }

    /**
     * Mise à jour des stocks pour chaque article composant la facture
     * 
     * @throws SQLException
     */
    private void updateStock(int id) throws SQLException {

        SQLPreferences prefs = new SQLPreferences(getTable().getDBRoot());
        if (!prefs.getBoolean(GestionArticleGlobalPreferencePanel.STOCK_FACT, true)) {

            SQLRow row = getTable().getRow(id);
            StockItemsUpdater stockUpdater = new StockItemsUpdater(new StockLabel() {
                @Override
                public String getLabel(SQLRowAccessor rowOrigin, SQLRowAccessor rowElt) {
                    return getLibelleStock(rowOrigin, rowElt);
                }
            }, row, row.getReferentRows(getTable().getTable("BON_DE_LIVRAISON_ELEMENT")),
                    getTable().contains("CREATE_VIRTUAL_STOCK") && row.getBoolean("CREATE_VIRTUAL_STOCK") ? TypeStockUpdate.REAL_VIRTUAL_DELIVER : TypeStockUpdate.REAL_DELIVER);

            if (getTable().getDBRoot().contains("RELIQUAT_BL")) {
                List<SQLRow> l = row.getReferentRows(getTable().getTable("RELIQUAT_BL").getField("ID_BON_DE_LIVRAISON_ORIGINE"));
                for (SQLRow sqlRow : l) {
                    stockUpdater.addReliquat(sqlRow.getForeign("ID_ARTICLE"), sqlRow.getInt("QTE"), sqlRow.getBigDecimal("QTE_UNITAIRE"));
                }
            }

            stockUpdater.update();
        }
    }

    public void loadFromReliquat(List<SQLRowValues> l) {
        this.tableBonItem.insertFromReliquat(l);
        this.tableBonItem.setEnabled(false);
    }

    @Override
    protected void refreshAfterSelect(SQLRowAccessor rSource) {

        tableBonItem.setDateDevise(date.getValue());
    }

}
