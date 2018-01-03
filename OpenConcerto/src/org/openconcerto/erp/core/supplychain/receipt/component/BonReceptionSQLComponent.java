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
 
 package org.openconcerto.erp.core.supplychain.receipt.component;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.common.component.TransfertBaseSQLComponent;
import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.erp.core.common.element.NumerotationAutoSQLElement;
import org.openconcerto.erp.core.common.ui.DeviseField;
import org.openconcerto.erp.core.common.ui.TotalPanel;
import org.openconcerto.erp.core.sales.product.element.ReferenceArticleSQLElement;
import org.openconcerto.erp.core.sales.product.ui.ReliquatRowValuesTable;
import org.openconcerto.erp.core.supplychain.receipt.element.BonReceptionSQLElement;
import org.openconcerto.erp.core.supplychain.receipt.ui.BonReceptionItemTable;
import org.openconcerto.erp.core.supplychain.stock.element.StockItemsUpdater;
import org.openconcerto.erp.core.supplychain.stock.element.StockItemsUpdater.TypeStockUpdate;
import org.openconcerto.erp.core.supplychain.stock.element.StockLabel;
import org.openconcerto.erp.generationDoc.gestcomm.BonReceptionXmlSheet;
import org.openconcerto.erp.panel.PanelOOSQLComponent;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.sql.sqlobject.JUniqueTextField;
import org.openconcerto.sql.view.EditFrame;
import org.openconcerto.sql.view.list.RowValuesTable;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.FormLayouter;
import org.openconcerto.ui.JDate;
import org.openconcerto.ui.TitledSeparator;
import org.openconcerto.ui.component.ITextArea;
import org.openconcerto.utils.DecimalUtils;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.NumberUtils;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

public class BonReceptionSQLComponent extends TransfertBaseSQLComponent {
    private BonReceptionItemTable tableBonItem;
    private ReliquatRowValuesTable tableBonReliquatItem;
    private ElementComboBox selectCommande;
    private ElementComboBox fournisseur;
    private JUniqueTextField textNumeroUnique;
    private final SQLTable tableNum = getTable().getBase().getTable("NUMEROTATION_AUTO");
    private final JTextField textReference = new JTextField(25);
    private PanelOOSQLComponent panelOO;
    private JDate date = new JDate(true);

    public BonReceptionSQLComponent() {
        super(Configuration.getInstance().getDirectory().getElement("BON_RECEPTION"));
    }

    @Override
    protected SQLRowValues createDefaults() {
        this.tableBonItem.getModel().clearRows();
        this.textNumeroUnique.setText(NumerotationAutoSQLElement.getNextNumero(getElement().getClass()));
        SQLRowValues rowVals = new SQLRowValues(getTable());
        rowVals.put("T_DEVISE", 0L);
        if (getTable().contains("CREATE_VIRTUAL_STOCK")) {
            rowVals.put("CREATE_VIRTUAL_STOCK", Boolean.TRUE);
        }
        return rowVals;
    }

    public void addViews() {
        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();

        if (getTable().contains("CREATE_VIRTUAL_STOCK")) {
            this.addView(new JCheckBox(), "CREATE_VIRTUAL_STOCK");
        }

        // Champ Module
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = GridBagConstraints.REMAINDER;
        final JPanel addP = ComptaSQLConfElement.createAdditionalPanel();
        this.setAdditionalFieldsPanel(new FormLayouter(addP, 1));
        this.add(addP, c);

        c.gridy++;
        c.gridwidth = 1;

        this.selectCommande = new ElementComboBox();
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
        DefaultGridBagConstraints.lockMinimumSize(this.textNumeroUnique);
        this.add(this.textNumeroUnique, c);

        // Date
        JLabel labelDate = new JLabel(getLabelFor("DATE"));
        labelDate.setHorizontalAlignment(SwingConstants.RIGHT);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx++;
        c.weightx = 0;
        this.add(labelDate, c);

        c.gridx++;
        c.weightx = 0;
        c.weighty = 0;
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
        final JLabel labelNom = new JLabel(getLabelFor("NOM"));
        labelNom.setHorizontalAlignment(SwingConstants.RIGHT);
        this.add(labelNom, c);
        c.gridx++;
        c.fill = GridBagConstraints.HORIZONTAL;
        DefaultGridBagConstraints.lockMinimumSize(this.textReference);
        this.add(this.textReference, c);
        // Fournisseur
        JLabel labelFournisseur = new JLabel(getLabelFor("ID_FOURNISSEUR"));
        labelFournisseur.setHorizontalAlignment(SwingConstants.RIGHT);
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        c.weighty = 0;
        this.add(labelFournisseur, c);

        this.fournisseur = new ElementComboBox();
        c.gridx++;
        c.weightx = 0;
        c.weighty = 0;
        c.fill = GridBagConstraints.NONE;
        this.add(this.fournisseur, c);

        // Devise
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.add(new JLabel(getLabelFor("ID_DEVISE"), SwingConstants.RIGHT), c);

        final ElementComboBox boxDevise = new ElementComboBox();
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridwidth = 1;
        c.weightx = 1;
        c.weighty = 0;
        c.fill = GridBagConstraints.NONE;
        DefaultGridBagConstraints.lockMinimumSize(boxDevise);
        this.add(boxDevise, c);
        this.addView(boxDevise, "ID_DEVISE");
        fournisseur.addValueListener(new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent arg0) {
                if (fournisseur.getSelectedRow() != null && fournisseur.getSelectedRow().getFields().contains("ID_DEVISE")) {
                    boxDevise.setValue(fournisseur.getSelectedRow().asRow().getForeignID("ID_DEVISE"));
                } else {
                    boxDevise.setValue((SQLRowAccessor) null);
                }
            }
        });

        // Element du bon
        this.tableBonItem = new BonReceptionItemTable();
        boxDevise.addValueListener(new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                tableBonItem.setDevise(boxDevise.getSelectedRow());

            }
        });
        c.gridx = 0;
        c.gridy++;
        c.weightx = 1;
        c.weighty = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.BOTH;
        this.add(this.tableBonItem, c);
        this.fournisseur.addValueListener(new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                tableBonItem.setFournisseur(fournisseur.getSelectedRow());
            }
        });

        c.anchor = GridBagConstraints.EAST;

        // Poids Total
        c.gridy++;
        c.gridx = 1;
        c.weightx = 0;
        c.weighty = 0;
        c.anchor = GridBagConstraints.EAST;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;

        c.gridx = 2;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 0;
        c.weighty = 0;
        c.anchor = GridBagConstraints.EAST;
        c.fill = GridBagConstraints.HORIZONTAL;

        DeviseField fieldHT = new DeviseField();
        DeviseField fieldEco = new DeviseField();
        DeviseField fieldTVA = new DeviseField();
        DeviseField fieldTTC = new DeviseField();
        DeviseField fieldDevise = new DeviseField();
        JTextField fieldPoids = new JTextField();
        fieldHT.setOpaque(false);
        fieldTVA.setOpaque(false);
        fieldTTC.setOpaque(false);
        addRequiredSQLObject(fieldEco, "T_ECO_CONTRIBUTION");
        addRequiredSQLObject(fieldDevise, "T_DEVISE");
        addRequiredSQLObject(fieldHT, "TOTAL_HT");
        addRequiredSQLObject(fieldTVA, "TOTAL_TVA");
        addRequiredSQLObject(fieldPoids, "TOTAL_POIDS");

        addRequiredSQLObject(fieldTTC, "TOTAL_TTC");

        // Disable
        this.allowEditable("TOTAL_HT", false);
        this.allowEditable("TOTAL_TVA", false);
        this.allowEditable("TOTAL_TTC", false);
        this.allowEditable("T_ECO_CONTRIBUTION", false);
        this.allowEditable("TOTAL_POIDS", false);

        DeviseField textRemiseHT = new DeviseField();
        DeviseField fieldService = new DeviseField();
        DeviseField textPortHT = new DeviseField();

        final TotalPanel totalTTC = new TotalPanel(this.tableBonItem, fieldEco, fieldHT, fieldTVA, fieldTTC, textPortHT, textRemiseHT, fieldService, null, fieldDevise, fieldPoids, null, null, null);

        c.gridx++;
        c.gridy++;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridheight = 2;
        c.anchor = GridBagConstraints.NORTHEAST;
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 0;
        DefaultGridBagConstraints.lockMinimumSize(totalTTC);

        this.add(totalTTC, c);
        c.gridy++;
        c.gridheight = 1;
        c.anchor = GridBagConstraints.WEST;

        if (getTable().getDBRoot().contains("RELIQUAT_BR")) {

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
            this.tableBonReliquatItem = new ReliquatRowValuesTable("RELIQUAT_BR");
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
        TitledSeparator sep = new TitledSeparator("Informations complémentaires");
        c.insets = new Insets(10, 2, 1, 2);
        this.add(sep, c);
        c.insets = new Insets(2, 2, 1, 2);

        c.gridx = 0;
        c.gridy++;
        c.gridheight = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 1;
        c.weighty = 0;
        c.fill = GridBagConstraints.BOTH;
        final ITextArea textInfos = new ITextArea(4, 4);
        JScrollPane scrollPane = new JScrollPane(textInfos);
        DefaultGridBagConstraints.lockMinimumSize(scrollPane);

        this.add(textInfos, c);

        c.gridx = 0;
        c.gridy++;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.SOUTHEAST;
        c.gridwidth = GridBagConstraints.REMAINDER;

        this.panelOO = new PanelOOSQLComponent(this);
        this.add(this.panelOO, c);

        this.addRequiredSQLObject(date, "DATE");
        this.addSQLObject(textInfos, "INFOS");
        this.addSQLObject(this.textReference, "NOM");
        this.addSQLObject(this.selectCommande, "ID_COMMANDE");
        this.addRequiredSQLObject(this.textNumeroUnique, "NUMERO");
        this.addRequiredSQLObject(this.fournisseur, "ID_FOURNISSEUR");

        this.textNumeroUnique.setText(NumerotationAutoSQLElement.getNextNumero(getElement().getClass()));

        // Lock UI
        DefaultGridBagConstraints.lockMinimumSize(this.fournisseur);

    }

    public int insert(SQLRow order) {

        int idBon = SQLRow.NONEXISTANT_ID;
        // on verifie qu'un devis du meme numero n'a pas été inséré entre temps
        int attempt = 0;
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
            try {
                this.tableBonItem.updateField("ID_BON_RECEPTION", idBon);
                if (this.tableBonReliquatItem != null) {
                    this.tableBonReliquatItem.updateField("ID_BON_RECEPTION_ORIGINE", idBon);
                }
                this.tableBonItem.createArticle(idBon, this.getElement());

                // incrémentation du numéro auto
                if (NumerotationAutoSQLElement.getNextNumero(getElement().getClass()).equalsIgnoreCase(this.textNumeroUnique.getText().trim())) {
                    SQLRowValues rowVals = new SQLRowValues(this.tableNum);
                    int val = this.tableNum.getRow(2).getInt("BON_R_START");
                    val++;
                    rowVals.put("BON_R_START", new Integer(val));
                    rowVals.update(2);
                }
                // FIXME USE A PREF
                // calculPHaPondere(idBon);

                final int idBonFinal = idBon;
                ComptaPropsConfiguration.getInstanceCompta().getNonInteractiveSQLExecutor().execute(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            updateStock(idBonFinal);
                            ((BonReceptionSQLElement) getElement()).updateCmdElement(((BonReceptionSQLElement) getElement()).getCmdFrom(idBonFinal), idBonFinal);
                        } catch (Exception e) {
                            ExceptionHandler.handle("Update error", e);
                        }
                    }
                });

                // generation du document
                final BonReceptionXmlSheet sheet = new BonReceptionXmlSheet(getTable().getRow(idBonFinal));
                sheet.createDocumentAsynchronous();
                sheet.showPrintAndExportAsynchronous(this.panelOO.isVisualisationSelected(), this.panelOO.isImpressionSelected(), true);

            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        return idBon;
    }

    @Override
    protected RowValuesTable getRowValuesTable() {
        return this.tableBonItem.getRowValuesTable();
    }

    public void loadFromReliquat(List<SQLRowValues> l) {
        this.tableBonItem.insertFromReliquat(l);
        this.tableBonItem.setEnabled(false);
    }

    public void loadQuantity(List<SQLRowValues> l) {
        Map<Integer, SQLRowValues> map = new HashMap<Integer, SQLRowValues>();
        for (SQLRowValues sqlRowValues : l) {
            if (!sqlRowValues.isForeignEmpty("ID_ARTICLE")) {
                final int foreignID = sqlRowValues.getForeignID("ID_ARTICLE");
                if (!map.containsKey(foreignID)) {
                    map.put(foreignID, sqlRowValues);
                } else {
                    SQLRowValues vals = map.get(foreignID);
                    if (sqlRowValues.getInt("QTE") > 0) {
                        if (NumberUtils.areNumericallyEqual(sqlRowValues.getBigDecimal("QTE_UNITAIRE"), BigDecimal.ONE) || sqlRowValues.getInt("QTE") > 1) {
                            vals.put("QTE", vals.getInt("QTE") + sqlRowValues.getInt("QTE"));
                        } else {
                            vals.put("QTE_UNITAIRE", vals.getBigDecimal("QTE_UNITAIRE").add(sqlRowValues.getBigDecimal("QTE_UNITAIRE")));
                        }
                    }
                }
            }
        }
        int count = this.tableBonItem.getModel().getRowCount();
        for (int i = 0; i < count; i++) {
            SQLRowValues r = this.tableBonItem.getModel().getRowValuesAt(i);
            SQLRowValues rowTR = map.get(r.getForeignID("ID_ARTICLE"));
            if (rowTR != null && !rowTR.isUndefined()) {
                if (r.getInt("QTE") > 0) {
                    if (NumberUtils.areNumericallyEqual(r.getBigDecimal("QTE_UNITAIRE"), BigDecimal.ONE) || r.getInt("QTE") > 1) {
                        this.tableBonItem.getModel().putValue(r.getInt("QTE") - rowTR.getInt("QTE"), i, "QTE");
                    } else {
                        this.tableBonItem.getModel().putValue(r.getBigDecimal("QTE_UNITAIRE").subtract(rowTR.getBigDecimal("QTE_UNITAIRE")), i, "QTE_UNITAIRE");
                    }
                }
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
        } else {

            // Mise à jour de l'élément
            super.update();
            final List<Object> cmdFrom = ((BonReceptionSQLElement) getElement()).getCmdFrom(getSelectedID());
            this.tableBonItem.updateField("ID_BON_RECEPTION", getSelectedID());
            if (tableBonReliquatItem != null) {
                this.tableBonReliquatItem.updateField("ID_BON_RECEPTION_ORIGINE", getSelectedID());
            }
            this.tableBonItem.createArticle(getSelectedID(), this.getElement());
            final int id = getSelectedID();
            ComptaPropsConfiguration.getInstanceCompta().getNonInteractiveSQLExecutor().execute(new Runnable() {

                @Override
                public void run() {
                    try {

                        // Mise à jour du stock
                        updateStock(id);
                        ((BonReceptionSQLElement) getElement()).updateCmdElement(cmdFrom, getSelectedID());

                    } catch (Exception e) {
                        ExceptionHandler.handle("Update error", e);
                    }
                }
            });
            // generation du document
            final BonReceptionXmlSheet sheet = new BonReceptionXmlSheet(getTable().getRow(id));
            sheet.createDocumentAsynchronous();
            sheet.showPrintAndExportAsynchronous(this.panelOO.isVisualisationSelected(), this.panelOO.isImpressionSelected(), true);

        }
    }

    /**
     * Calcul du prix d'achat pondéré pour chacun des articles du bon de reception
     * 
     * @param id id du bon de reception
     * @throws SQLException
     */
    private void calculPHaPondere(int id) throws SQLException {
        // FIXME : unused !
        SQLTable sqlTableArticle = ((ComptaPropsConfiguration) Configuration.getInstance()).getRootSociete().getTable("ARTICLE");
        SQLElement eltArticle = Configuration.getInstance().getDirectory().getElement(sqlTableArticle);
        SQLElement eltStock = Configuration.getInstance().getDirectory().getElement("STOCK");
        SQLRow row = getTable().getRow(id);

        // On récupére les articles qui composent la facture
        SQLTable sqlTableBonElt = ((ComptaPropsConfiguration) Configuration.getInstance()).getRootSociete().getTable("BON_RECEPTION_ELEMENT");
        List<SQLRow> elts = row.getReferentRows(sqlTableBonElt);

        for (SQLRow rowEltBon : elts) {

            SQLRowValues rowVals = rowEltBon.createUpdateRow();

            // recupere l'ancien prix d'achat
            int idArticle = ReferenceArticleSQLElement.getIdForCNM(rowVals, false);
            if (idArticle > 1) {
                // Prix d'achat de l'article à l'origine
                SQLRow rowArticle = eltArticle.getTable().getRow(idArticle);
                if (rowArticle != null) {
                    BigDecimal prixHA = (BigDecimal) rowArticle.getObject("PRIX_METRIQUE_HA_1");

                    // Quantité en stock
                    int idStock = rowArticle.getInt("ID_STOCK");
                    SQLRow rowStock = eltStock.getTable().getRow(idStock);
                    BigDecimal qteStock = new BigDecimal(rowStock.getInt("QTE_REEL"));
                    if (prixHA != null && qteStock.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal qteRecue = new BigDecimal(rowEltBon.getInt("QTE"));
                        BigDecimal prixHACmd = (BigDecimal) rowEltBon.getObject("PRIX_METRIQUE_HA_1");
                        if (qteRecue.compareTo(BigDecimal.ZERO) > 0 && prixHACmd != null) {
                            BigDecimal totalHARecue = qteRecue.multiply(prixHACmd, DecimalUtils.HIGH_PRECISION);
                            BigDecimal totalHAStock = qteStock.multiply(prixHA, DecimalUtils.HIGH_PRECISION);
                            BigDecimal totalQte = qteRecue.add(qteStock);
                            BigDecimal prixHaPond = totalHARecue.add(totalHAStock).divide(totalQte, DecimalUtils.HIGH_PRECISION);
                            SQLRowValues rowValsArticle = rowArticle.createEmptyUpdateRow();
                            rowValsArticle.put("PRIX_METRIQUE_HA_1", prixHaPond);
                            rowValsArticle.commit();
                        }
                    }
                }
            }

        }
    }

    protected String getLibelleStock(SQLRowAccessor row, SQLRowAccessor rowElt) {
        return "Bon de réception N°" + row.getString("NUMERO");
    }

    /**
     * Mise à jour des stocks pour chaque article composant du bon
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
        }, row, row.getReferentRows(getTable().getTable("BON_RECEPTION_ELEMENT")),
                getTable().contains("CREATE_VIRTUAL_STOCK") && row.getBoolean("CREATE_VIRTUAL_STOCK") ? TypeStockUpdate.REAL_VIRTUAL_RECEPT : TypeStockUpdate.REAL_RECEPT);

        if (getTable().getDBRoot().contains("RELIQUAT_BR")) {
            List<SQLRow> l = row.getReferentRows(getTable().getTable("RELIQUAT_BR").getField("ID_BON_RECEPTION_ORIGINE"));
            for (SQLRow sqlRow : l) {
                stockUpdater.addReliquat(sqlRow.getForeign("ID_ARTICLE"), sqlRow.getInt("QTE"), sqlRow.getBigDecimal("QTE_UNITAIRE"));
            }
        }

        stockUpdater.update();

    }

    @Override
    public void select(SQLRowAccessor r) {
        super.select(r);
        if (this.tableBonReliquatItem != null) {
            this.tableBonReliquatItem.getRowValuesTable().clear();
            if (r != null) {
                this.tableBonReliquatItem.getRowValuesTable().insertFrom("ID_BON_RECEPTION_ORIGINE", r.asRowValues());
            }
        }
    }

    @Override
    protected void refreshAfterSelect(SQLRowAccessor rSource) {
        if (this.date.getValue() != null) {
            this.tableBonItem.setDateDevise(this.date.getValue());
        }

    }
}
