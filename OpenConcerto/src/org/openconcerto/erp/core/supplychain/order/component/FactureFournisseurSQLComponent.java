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
 
 package org.openconcerto.erp.core.supplychain.order.component;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.common.component.TransfertBaseSQLComponent;
import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.erp.core.common.ui.AbstractVenteArticleItemTable;
import org.openconcerto.erp.core.common.ui.DeviseField;
import org.openconcerto.erp.core.common.ui.TotalPanel;
import org.openconcerto.erp.core.finance.accounting.element.EcritureSQLElement;
import org.openconcerto.erp.core.finance.accounting.model.CurrencyConverter;
import org.openconcerto.erp.core.finance.tax.model.TaxeCache;
import org.openconcerto.erp.core.supplychain.order.ui.FactureFournisseurItemTable;
import org.openconcerto.erp.generationDoc.gestcomm.FactureFournisseurXmlSheet;
import org.openconcerto.erp.generationEcritures.GenerationMvtFactureFournisseur;
import org.openconcerto.erp.panel.PanelOOSQLComponent;
import org.openconcerto.erp.preferences.DefaultNXProps;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.DefaultElementSQLObject;
import org.openconcerto.sql.element.ElementSQLObject;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLBackgroundTableCache;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.request.ComboSQLRequest;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.sql.users.UserManager;
import org.openconcerto.sql.view.list.RowValuesTable;
import org.openconcerto.ui.AutoHideListener;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.FormLayouter;
import org.openconcerto.ui.JDate;
import org.openconcerto.ui.TitledSeparator;
import org.openconcerto.ui.component.ITextArea;
import org.openconcerto.ui.preferences.DefaultProps;
import org.openconcerto.utils.text.SimpleDocumentListener;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.math.BigDecimal;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

public class FactureFournisseurSQLComponent extends TransfertBaseSQLComponent {

    private FactureFournisseurItemTable table = new FactureFournisseurItemTable();
    private final ITextArea infos = new ITextArea(3, 3);
    private ElementComboBox fourn = new ElementComboBox();
    private ElementComboBox comptePCE = new ElementComboBox();
    private DefaultElementSQLObject compAdr;
    private PanelOOSQLComponent panelOO;
    final JPanel panelAdrSpec = new JPanel(new GridBagLayout());
    private JDate dateCommande = new JDate();
    private final JTextField fieldTaux = new JTextField(15);
    private final JLabel labelTaux = new JLabel();
    private ElementSQLObject eltModeRegl;
    private PropertyChangeListener listenerModeReglDefaut = new PropertyChangeListener() {

        public void propertyChange(PropertyChangeEvent evt) {

            SQLRow rowFourn = fourn.getRequest().getPrimaryTable().getRow(fourn.getWantedID());

            if (!isFilling() && rowFourn != null && !rowFourn.isUndefined()) {

                // SQLRow rowCharge = rowFourn.getForeign("ID_COMPTE_PCE_CHARGE");
                // if (rowCharge != null && !rowCharge.isUndefined()) {
                // compteSel.setValue(rowCharge);
                // }

                int idModeRegl = rowFourn.getInt("ID_MODE_REGLEMENT");
                if (idModeRegl > 1 && FactureFournisseurSQLComponent.this.eltModeRegl != null && getMode() == Mode.INSERTION) {
                    SQLElement sqlEltModeRegl = Configuration.getInstance().getDirectory().getElement("MODE_REGLEMENT");
                    SQLRow rowModeRegl = sqlEltModeRegl.getTable().getRow(idModeRegl);
                    SQLRowValues rowVals = rowModeRegl.createUpdateRow();
                    rowVals.clearPrimaryKeys();
                    FactureFournisseurSQLComponent.this.eltModeRegl.setValue(rowVals);
                    System.err.println("Select Mode regl " + idModeRegl);
                }
                // }
            }

        }

    };

    public FactureFournisseurSQLComponent() {
        super(Configuration.getInstance().getDirectory().getElement("FACTURE_FOURNISSEUR"));
    }

    public ElementComboBox getBoxFournisseur() {
        return this.fourn;
    }

    public void addViews() {
        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();

        // Numero du commande
        c.gridx = 0;
        c.weightx = 0;
        this.add(new JLabel(getLabelFor("NUMERO"), SwingConstants.RIGHT), c);

        JTextField numero = new JTextField(25);
        c.gridx++;
        c.weightx = 1;
        c.fill = GridBagConstraints.NONE;
        DefaultGridBagConstraints.lockMinimumSize(numero);
        this.add(numero, c);

        // Date
        JLabel labelDate = new JLabel(getLabelFor("DATE"));
        labelDate.setHorizontalAlignment(SwingConstants.RIGHT);
        c.gridx = 2;
        c.weightx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.add(labelDate, c);

        c.gridx++;
        c.fill = GridBagConstraints.NONE;
        this.add(dateCommande, c);

        this.dateCommande.addValueListener(new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (!isFilling() && dateCommande.getValue() != null) {
                    table.setDateDevise(dateCommande.getValue());
                    updateLabelTauxConversion();
                }
            }
        });

        // Fournisseur
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.add(new JLabel(getLabelFor("ID_FOURNISSEUR"), SwingConstants.RIGHT), c);

        c.gridx = GridBagConstraints.RELATIVE;
        c.gridwidth = 1;
        c.weightx = 1;
        c.weighty = 0;
        c.fill = GridBagConstraints.NONE;
        this.add(this.fourn, c);
        addRequiredSQLObject(this.fourn, "ID_FOURNISSEUR");

        // Champ Module
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = GridBagConstraints.REMAINDER;
        final JPanel addP = ComptaSQLConfElement.createAdditionalPanel();
        this.setAdditionalFieldsPanel(new FormLayouter(addP, 2));
        this.add(addP, c);

        c.gridy++;
        c.gridwidth = 1;

        final ElementComboBox boxDevise = new ElementComboBox();

        if (DefaultNXProps.getInstance().getBooleanValue(AbstractVenteArticleItemTable.ARTICLE_SHOW_DEVISE, false)) {
            // Devise
            c.gridx = 0;
            c.gridy++;
            c.weightx = 0;
            c.fill = GridBagConstraints.HORIZONTAL;
            this.add(new JLabel(getLabelFor("ID_DEVISE"), SwingConstants.RIGHT), c);

            c.gridx++;
            c.gridwidth = 1;
            c.weightx = 1;
            c.weighty = 0;
            c.fill = GridBagConstraints.NONE;
            this.add(boxDevise, c);
            this.addView(boxDevise, "ID_DEVISE");

            this.fourn.addModelListener("wantedID", new PropertyChangeListener() {

                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    table.setFournisseur(fourn.getSelectedRow());
                    if (!isFilling()) {
                        SQLRow row = fourn.getSelectedRow();
                        if (row != null && !row.isUndefined() && !row.isForeignEmpty("ID_DEVISE")) {
                            boxDevise.setValue(row.getForeignID("ID_DEVISE"));
                        }
                    }
                }
            });
            this.fourn.addModelListener("wantedID", this.listenerModeReglDefaut);

            if (getTable().contains("TAUX_APPLIQUE")) {
                // Devise
                c.gridx++;
                c.weightx = 0;
                c.fill = GridBagConstraints.HORIZONTAL;
                this.add(new JLabel(getLabelFor("TAUX_APPLIQUE"), SwingConstants.RIGHT), c);

                c.gridx++;
                c.gridwidth = 1;
                c.weightx = 1;
                c.weighty = 0;
                c.fill = GridBagConstraints.NONE;

                DefaultGridBagConstraints.lockMinimumSize(this.fieldTaux);
                JPanel panelTaux = new JPanel();
                panelTaux.add(this.fieldTaux);
                panelTaux.add(this.labelTaux);
                this.add(panelTaux, c);
                this.addView(this.fieldTaux, "TAUX_APPLIQUE");

            }
        }

        // Compte pce
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.add(new JLabel(getLabelFor("ID_COMPTE_PCE"), SwingConstants.RIGHT), c);

        c.gridx = GridBagConstraints.RELATIVE;
        c.gridwidth = 1;
        c.weightx = 1;
        c.weighty = 0;
        c.fill = GridBagConstraints.NONE;

        this.add(this.comptePCE, c);

        final SQLElement foreignElement = getElement().getForeignElement("ID_COMPTE_PCE");
        final ComboSQLRequest comboRequest = foreignElement.getComboRequest(true);
        comboRequest.setWhere(new Where(foreignElement.getTable().getField("NUMERO"), "LIKE", "6%"));
        this.comptePCE.init(foreignElement, comboRequest);
        this.addView(this.comptePCE, "ID_COMPTE_PCE");

        // Reference
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 1;
        c.weightx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.EAST;
        this.add(new JLabel(getLabelFor("NOM"), SwingConstants.RIGHT), c);

        final JTextField textNom = new JTextField();
        c.gridx++;
        c.weightx = 1;
        this.add(textNom, c);

        String field;
            field = "ID_COMMERCIAL";
        // Commercial
        c.weightx = 0;
        c.gridx++;
        this.add(new JLabel(getLabelFor(field), SwingConstants.RIGHT), c);

        ElementComboBox commSel = new ElementComboBox(false, 25);
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridwidth = 1;
        c.weightx = 1;
        c.weighty = 0;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;
        this.add(commSel, c);
        addRequiredSQLObject(commSel, field);

        // Table d'élément
        c.fill = GridBagConstraints.BOTH;
        c.gridy++;
        c.gridx = 0;
        c.weightx = 0;
        c.weighty = 1;
        c.gridwidth = 4;
        this.add(this.table, c);
        if (DefaultNXProps.getInstance().getBooleanValue(AbstractVenteArticleItemTable.ARTICLE_SHOW_DEVISE, false)) {

            boxDevise.addValueListener(new PropertyChangeListener() {

                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    table.setDevise(boxDevise.getSelectedRow());
                    updateLabelTauxConversion();

                }
            });

            fieldTaux.getDocument().addDocumentListener(new SimpleDocumentListener() {

                @Override
                public void update(DocumentEvent e) {
                    BigDecimal tauxConversion = null;
                    if (fieldTaux.getText().trim().length() > 0) {
                        tauxConversion = new BigDecimal(fieldTaux.getText());
                    }
                    table.setTauxConversion(tauxConversion);
                    updateLabelTauxConversion();
                }
            });
        }

        this.fourn.addModelListener("wantedID", new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                table.setFournisseur(fourn.getSelectedRow());
                if (!isFilling()) {
                    SQLRow row = fourn.getSelectedRow();
                    row.fetchValues();
                    if (row != null && !row.isUndefined() && !row.isForeignEmpty("ID_COMPTE_PCE_CHARGE")) {
                        comptePCE.setValue(row.getForeign("ID_COMPTE_PCE_CHARGE"));
                    }
                }
            }
        });
        // Bottom
        c.gridy++;
        c.weighty = 0;
        this.add(getBottomPanel(), c);

        c.gridx = 0;
        c.gridy++;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;

        c.gridx = 0;
        c.gridy++;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.SOUTHEAST;
        c.gridwidth = GridBagConstraints.REMAINDER;

        this.panelOO = new PanelOOSQLComponent(this);
        this.add(this.panelOO, c);

        addSQLObject(textNom, "NOM");
        addRequiredSQLObject(dateCommande, "DATE");
        addRequiredSQLObject(numero, "NUMERO");
        addSQLObject(this.infos, "INFOS");

        DefaultGridBagConstraints.lockMinimumSize(this.fourn);
        DefaultGridBagConstraints.lockMinimumSize(commSel);
    }

    private void updateLabelTauxConversion() {
        String result = "";
        if (this.fieldTaux.getText().trim().length() == 0 && this.table.getDevise() != null && !this.table.getDevise().isUndefined()) {
            CurrencyConverter converter = new CurrencyConverter();
            BigDecimal taux = converter.convert(BigDecimal.ONE, converter.getCompanyCurrencyCode(), this.table.getDevise().getString("CODE"), this.table.getDateDevise(), false);
            result = "(" + taux + ")";
        } else {
            result = "(" + this.fieldTaux.getText() + ")";
        }
        this.labelTaux.setText(result);
    }

    private JPanel getBottomPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();

        // Colonne 1 : Infos
        c.gridx = 0;
        c.weightx = 1;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;

        panel.add(new TitledSeparator(getLabelFor("INFOS")), c);

        c.gridy++;
        c.weighty = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.BOTH;
        final JScrollPane scrollPane = new JScrollPane(this.infos);
        scrollPane.setBorder(null);
        panel.add(scrollPane, c);

        this.addView("ID_MODE_REGLEMENT", REQ + ";" + DEC + ";" + SEP);
        this.eltModeRegl = (ElementSQLObject) this.getView("ID_MODE_REGLEMENT");

        c.gridx++;
        c.fill = GridBagConstraints.NONE;
        c.weighty = 1;
        eltModeRegl.setOpaque(false);
        panel.add(eltModeRegl, c);

        // Colonne 2 : Poids & autres
        DefaultProps props = DefaultNXProps.getInstance();
        Boolean b = props.getBooleanValue("ArticleShowPoids");
        final JTextField textPoidsTotal = new JTextField(8);
        JTextField poids = new JTextField();
        if (b) {
            final JPanel panelPoids = new JPanel();

            panelPoids.add(new JLabel(getLabelFor("T_POIDS")), c);

            textPoidsTotal.setEnabled(false);
            textPoidsTotal.setHorizontalAlignment(JTextField.RIGHT);
            textPoidsTotal.setDisabledTextColor(Color.BLACK);

            panelPoids.add(textPoidsTotal, c);

            c.gridx++;
            c.gridy = 0;
            c.weightx = 0;
            c.weighty = 0;
            c.gridwidth = 1;
            c.gridheight = 2;
            c.fill = GridBagConstraints.NONE;
            c.anchor = GridBagConstraints.NORTHEAST;
            panel.add(panelPoids, c);
            DefaultGridBagConstraints.lockMinimumSize(panelPoids);
            addSQLObject(textPoidsTotal, "T_POIDS");
        } else {
            addSQLObject(poids, "T_POIDS");
        }

        DeviseField textPortHT = new DeviseField();
        ElementComboBox comboTaxePort = new ElementComboBox();

        if (getTable().contains("PORT_HT")) {
            addRequiredSQLObject(textPortHT, "PORT_HT");
            final JPanel panelPoids = new JPanel(new GridBagLayout());
            GridBagConstraints cPort = new DefaultGridBagConstraints();
            cPort.gridx = 0;
            cPort.weightx = 0;
            panelPoids.add(new JLabel(getLabelFor("PORT_HT")), cPort);
            textPortHT.setHorizontalAlignment(JTextField.RIGHT);
            cPort.gridx++;
            cPort.weightx = 1;
            panelPoids.add(textPortHT, cPort);

            cPort.gridy++;
            cPort.gridx = 0;
            cPort.weightx = 0;
            addRequiredSQLObject(comboTaxePort, "ID_TAXE_PORT");
            panelPoids.add(new JLabel(getLabelFor("ID_TAXE_PORT")), cPort);
            cPort.gridx++;
            cPort.weightx = 0;
            panelPoids.add(comboTaxePort, cPort);

            c.gridx++;
            c.gridy = 0;
            c.weightx = 0;
            c.weighty = 0;
            c.gridwidth = 1;
            c.gridheight = 2;
            c.fill = GridBagConstraints.NONE;
            c.anchor = GridBagConstraints.NORTHEAST;
            panel.add(panelPoids, c);
            DefaultGridBagConstraints.lockMinimumSize(panelPoids);
        }
        // Total

        DeviseField textRemiseHT = new DeviseField();
        DeviseField fieldHT = new DeviseField();
        DeviseField fieldEco = new DeviseField();
        DeviseField fieldTVA = new DeviseField();
        DeviseField fieldTTC = new DeviseField();
        DeviseField fieldDevise = new DeviseField();
        DeviseField fieldService = new DeviseField();
        fieldHT.setOpaque(false);
        fieldTVA.setOpaque(false);
        fieldTTC.setOpaque(false);
        fieldService.setOpaque(false);
        addRequiredSQLObject(fieldDevise, "T_DEVISE");
        addRequiredSQLObject(fieldEco, "T_ECO_CONTRIBUTION");
        addRequiredSQLObject(fieldHT, "T_HT");
        addRequiredSQLObject(fieldTVA, "T_TVA");

        addRequiredSQLObject(fieldTTC, "T_TTC");
        addRequiredSQLObject(fieldService, "T_SERVICE");

        // Disable
        this.allowEditable("T_ECO_CONTRIBUTION", false);
        this.allowEditable("T_HT", false);
        this.allowEditable("T_TVA", false);
        this.allowEditable("T_TTC", false);
        this.allowEditable("T_SERVICE", false);
        this.allowEditable("T_POIDS", false);

        final TotalPanel totalTTC = new TotalPanel(this.table, fieldEco, fieldHT, fieldTVA, fieldTTC, textPortHT, textRemiseHT, fieldService, null, fieldDevise, null, null,
                (getTable().contains("ID_TAXE_PORT") ? comboTaxePort : null), null);

        c.gridx++;
        c.gridy--;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridheight = 2;
        c.anchor = GridBagConstraints.NORTHEAST;
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 0;
        DefaultGridBagConstraints.lockMinimumSize(totalTTC);

        panel.add(totalTTC, c);

        c.gridy += 3;
        c.gridheight = 2;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.EAST;
        panel.add(getModuleTotalPanel(), c);

        table.getModel().addTableModelListener(new TableModelListener() {

            public void tableChanged(TableModelEvent e) {
                textPoidsTotal.setText(String.valueOf(table.getPoidsTotal()));
            }
        });

        this.fourn.addModelListener("wantedID", new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                final SQLRow selectedRow2 = fourn.getSelectedRow();
                if (selectedRow2 != null && !selectedRow2.isUndefined()) {
                    totalTTC.setIntraComm(selectedRow2.getBoolean("UE"));
                } else {
                    totalTTC.setIntraComm(false);
                }

            }
        });

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

        comboTaxePort.addValueListener(new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                // TODO Raccord de méthode auto-généré
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

    protected JPanel getModuleTotalPanel() {

        return AutoHideListener.listen(new JPanel());
    }

    public int insert(SQLRow order) {

        int idFacture = getSelectedID();
        idFacture = super.insert(order);
        this.table.updateField("ID_FACTURE_FOURNISSEUR", idFacture);

        // Création des articles
        this.table.createArticle(idFacture, this.getElement());

        new GenerationMvtFactureFournisseur(getTable().getRow(idFacture));

        final FactureFournisseurXmlSheet sheet = new FactureFournisseurXmlSheet(getTable().getRow(idFacture));
        sheet.createDocumentAsynchronous();
        sheet.showPrintAndExportAsynchronous(this.panelOO.isVisualisationSelected(), this.panelOO.isImpressionSelected(), true);

        return idFacture;
    }

    @Override
    public void update() {

        super.update();
        SQLRow row = getTable().getRow(getSelectedID());
        this.table.updateField("ID_FACTURE_FOURNISSEUR", row.getID());
        this.table.createArticle(getSelectedID(), this.getElement());

        int idMvt = (row.getObject("ID_MOUVEMENT") == null ? 1 : row.getInt("ID_MOUVEMENT"));
        System.err.println("__________***************** UPDATE" + idMvt);

        // on supprime tout ce qui est lié à la facture
        EcritureSQLElement eltEcr = (EcritureSQLElement) Configuration.getInstance().getDirectory().getElement("ECRITURE");
        eltEcr.archiveMouvementProfondeur(idMvt, false);
        if (idMvt > 1) {
            new GenerationMvtFactureFournisseur(row, idMvt);
        } else {
            new GenerationMvtFactureFournisseur(row);
        }
        final FactureFournisseurXmlSheet sheet = new FactureFournisseurXmlSheet(row);
        sheet.createDocumentAsynchronous();
        sheet.showPrintAndExportAsynchronous(this.panelOO.isVisualisationSelected(), this.panelOO.isImpressionSelected(), true);
    }

    public void setDefaults() {
        this.resetValue();
        this.table.getModel().clearRows();
    }

    @Override
    protected SQLRowValues createDefaults() {
        SQLRowValues rowVals = new SQLRowValues(getTable());
        rowVals.put("T_POIDS", 0.0F);
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
            rowVals.put("ID_COMMERCIAL", rowsComm.getID());
        }
        rowVals.put("T_HT", Long.valueOf(0));
        rowVals.put("T_SERVICE", Long.valueOf(0));
        rowVals.put("T_DEVISE", Long.valueOf(0));
        rowVals.put("T_TVA", Long.valueOf(0));
        rowVals.put("T_TTC", Long.valueOf(0));
        // rowVals.put("NUMERO",
        // NumerotationAutoSQLElement.getNextNumero(CommandeSQLElement.class));

        if (getTable().contains("ID_TAXE_PORT")) {
            rowVals.put("ID_TAXE_PORT", TaxeCache.getCache().getFirstTaxe().getID());
        }

        return rowVals;
    }

    @Override
    public RowValuesTable getRowValuesTable() {
        return this.table.getRowValuesTable();
    }

    @Override
    protected void refreshAfterSelect(SQLRowAccessor rSource) {
        if (this.dateCommande.getValue() != null) {
            this.table.setDateDevise(this.dateCommande.getValue());
            updateLabelTauxConversion();
        }

    }

}
