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
 
 package org.openconcerto.erp.core.common.ui;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.common.element.StyleSQLElement;
import org.openconcerto.erp.core.finance.tax.model.TaxeCache;
import org.openconcerto.erp.core.sales.product.element.ReferenceArticleSQLElement;
import org.openconcerto.erp.preferences.DefaultNXProps;
import org.openconcerto.erp.preferences.GestionArticleGlobalPreferencePanel;
import org.openconcerto.erp.utils.TM;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.preferences.SQLPreferences;
import org.openconcerto.sql.view.list.AutoCompletionManager;
import org.openconcerto.sql.view.list.RowValuesTable;
import org.openconcerto.sql.view.list.RowValuesTableControlPanel;
import org.openconcerto.sql.view.list.RowValuesTableModel;
import org.openconcerto.sql.view.list.RowValuesTableRenderer;
import org.openconcerto.sql.view.list.SQLTableElement;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.table.XTableColumnModel;
import org.openconcerto.utils.DecimalUtils;
import org.openconcerto.utils.ExceptionHandler;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.File;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

public abstract class AbstractArticleItemTable extends JPanel {
    protected RowValuesTable table;
    protected SQLTableElement totalHT, totalHA;
    protected SQLTableElement tableElementTVA;
    protected SQLTableElement tableElementTotalTTC;
    protected SQLTableElement tableElementTotalDevise;
    protected SQLTableElement service, qte, ha;
    protected SQLTableElement tableElementPoidsTotal;
    protected SQLTableElement tableElementEcoID, tableElementEco, tableElementEcoTotal;
    protected SQLTableElement prebilan;
    private RowValuesTableModel model;
    protected SQLRowValues defaultRowVals;
    private List<JButton> buttons = null;
    protected RowValuesTableControlPanel control = null;
    private SQLRowAccessor tarif = null;

    public static String SHOW_TOTAL_ECO_CONTRIBUTION = "SHOW_TOTAL_ECO_CONTRIBUTION";
    public static String SHOW_ECO_CONTRIBUTION_COLUMNS = "SHOW_ECO_CONTRIBUTION_COLUMNS";

    private Date dateDevise = new Date();
    private boolean usedBiasedDevise = true;

    public AbstractArticleItemTable() {
        this(null);
    }

    public AbstractArticleItemTable(List<JButton> buttons) {
        this.buttons = buttons;
        init();
        uiInit();
    }

    /**
     * 
     */
    abstract protected void init();

    protected void setModel(RowValuesTableModel model) {
        this.model = model;

    }

    public boolean isUsedBiasedDevise() {
        return usedBiasedDevise;
    }

    public void setUsedBiasedDevise(boolean usedBiasedDevise) {
        this.usedBiasedDevise = usedBiasedDevise;
    }

    public void setDateDevise(Date dateDevise) {
        if (dateDevise != null) {
            this.dateDevise = dateDevise;
            refreshDeviseAmount();
        }
    }

    public Date getDateDevise() {
        return dateDevise;
    }

    protected abstract void refreshDeviseAmount();

    protected File getConfigurationFile() {
        return new File(Configuration.getInstance().getConfDir(), "Table/" + getConfigurationFileName());
    }

    /**
     * 
     */
    protected void uiInit() {
        // Ui init
        setLayout(new GridBagLayout());
        this.setOpaque(false);
        final GridBagConstraints c = new DefaultGridBagConstraints();

        c.weightx = 1;

        control = new RowValuesTableControlPanel(this.table, this.buttons);
        control.setOpaque(false);
        this.add(control, c);

        c.gridy++;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 1;
        final JScrollPane comp = new JScrollPane(this.table);
        comp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        this.add(comp, c);
        this.table.setDefaultRenderer(Long.class, new RowValuesTableRenderer());
    }

    /**
     * @return the coniguration file to store pref
     */
    protected abstract String getConfigurationFileName();

    public abstract SQLElement getSQLElement();

    public void updateField(final String field, final int id) {
        this.table.updateField(field, id);
    }

    public RowValuesTable getRowValuesTable() {
        return this.table;
    }

    public void insertFrom(final String field, final int id) {
        this.table.insertFrom(field, id);

    }

    public RowValuesTableModel getModel() {
        return this.table.getRowValuesTableModel();
    }

    public SQLTableElement getPrebilanElement() {
        return this.prebilan;
    }

    public SQLTableElement getPrixTotalHTElement() {
        return this.totalHT;
    }

    public SQLTableElement getPoidsTotalElement() {
        return this.tableElementPoidsTotal;
    }

    public SQLTableElement getPrixTotalTTCElement() {
        return this.tableElementTotalTTC;
    }

    public SQLTableElement getPrixServiceElement() {
        return this.service;
    }

    public SQLTableElement getQteElement() {
        return this.qte;
    }

    public SQLTableElement getHaElement() {
        return this.ha;
    }

    public SQLTableElement getTotalHaElement() {
        return this.totalHA;
    }

    public SQLTableElement getTVAElement() {
        return this.tableElementTVA;
    }

    public SQLTableElement getTableElementTotalDevise() {
        return this.tableElementTotalDevise;
    }

    public SQLTableElement getTableElementTotalEco() {
        return this.tableElementEcoTotal;
    }

    public void deplacerDe(final int inc) {
        final int rowIndex = this.table.getSelectedRow();

        final int dest = this.model.moveBy(rowIndex, inc);
        this.table.getSelectionModel().setSelectionInterval(dest, dest);
    }

    /**
     * @return le poids total de tous les éléments (niveau 1) du tableau
     */
    public float getPoidsTotal() {

        float poids = 0.0F;
        final int poidsTColIndex = this.model.getColumnIndexForElement(this.tableElementPoidsTotal);
        if (poidsTColIndex >= 0) {
            for (int i = 0; i < this.table.getRowCount(); i++) {
                final Number tmp = (Number) this.model.getValueAt(i, poidsTColIndex);
                int level = 1;
                if (this.model.getRowValuesAt(i).getObject("NIVEAU") != null) {
                    level = this.model.getRowValuesAt(i).getInt("NIVEAU");
                }
                if (tmp != null && level == 1) {
                    poids += tmp.floatValue();
                }
            }
        }
        return poids;
    }

    public void refreshTable() {
        this.table.repaint();
    }

    public void createArticle(final int id, final SQLElement eltSource) {

        final SQLElement eltArticleTable = getSQLElement();

        final SQLTable tableArticle = ((ComptaPropsConfiguration) Configuration.getInstance()).getRootSociete().getTable("ARTICLE");

        final boolean modeAvance = DefaultNXProps.getInstance().getBooleanValue("ArticleModeVenteAvance", false);
        SQLPreferences prefs = SQLPreferences.getMemCached(tableArticle.getDBRoot());
        final boolean createArticle = prefs.getBoolean(GestionArticleGlobalPreferencePanel.CREATE_ARTICLE_AUTO, true);

        // On récupére les articles qui composent la table
        final List<SQLRow> listElts = eltSource.getTable().getRow(id).getReferentRows(eltArticleTable.getTable());
        final SQLRowValues rowArticle = new SQLRowValues(tableArticle);
        final Set<SQLField> fields = tableArticle.getFields();

        for (final SQLRow rowElt : listElts) {
            // final SQLRow foreignRow = rowElt.getForeignRow("ID_ARTICLE");
            // if (foreignRow == null || foreignRow.isUndefined()) {
            final Set<String> fieldsName = rowElt.getTable().getFieldsName();
            // on récupére l'article qui lui correspond

            for (final SQLField field : fields) {

                final String name = field.getName();
                if (fieldsName.contains(name) && !field.isPrimaryKey()) {
                    rowArticle.put(name, rowElt.getObject(name));
                }
            }
            // crée les articles si il n'existe pas

            int idArt = -1;
            if (modeAvance)
                idArt = ReferenceArticleSQLElement.getIdForCNM(rowArticle, createArticle);
            else {
                idArt = ReferenceArticleSQLElement.getIdForCN(rowArticle, createArticle);
            }
            if (createArticle && idArt > 1 && rowElt.isForeignEmpty("ID_ARTICLE")) {
                try {
                    rowElt.createEmptyUpdateRow().put("ID_ARTICLE", idArt).update();
                } catch (SQLException e) {
                    ExceptionHandler.handle("Erreur lors de l'affectation de l'article crée!", e);
                }
            }
            // ReferenceArticleSQLElement.getIdForCNM(rowArticle, true);
        }
        // }
    }


    public SQLRowValues getDefaultRowValues() {
        return this.defaultRowVals;
    }

    public SQLRowAccessor getTarif() {
        return tarif;
    }

    public void setTarif(SQLRowAccessor idTarif, boolean ask) {
        this.tarif = idTarif;
        // Test si ID_DEVISE est dans la table pour KD
        if (this.tarif != null && this.tarif.getTable().contains("ID_DEVISE") && !this.tarif.isForeignEmpty("ID_DEVISE") && this.defaultRowVals != null) {
            this.defaultRowVals.put("ID_DEVISE", this.tarif.getForeignID("ID_DEVISE"));
        }
    }

    protected void setColumnVisible(int col, boolean visible) {
        if (col >= 0) {
            XTableColumnModel columnModel = this.table.getColumnModel();
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(col), visible);
        }
    }

    protected void calculTarifNomenclature() {

        if (this.model.getRowCount() == 0) {
            return;
        }
        final int columnForField = this.model.getColumnForField("NIVEAU");
        if (columnForField >= 0) {
            checkNiveau();

            int rowCount = this.model.getRowCount();

            for (int niveau = 4; niveau > 1; niveau--) {
                int index = rowCount - 1;

                while (index > 0) {

                    BigDecimal prixUnitHT = BigDecimal.ZERO;
                    BigDecimal prixUnitHA = BigDecimal.ZERO;

                    boolean update = false;
                    int indexToUpdate = index;
                    // Calcul du sous total
                    for (int i = index; i >= 0; i--) {
                        indexToUpdate = i;
                        SQLRowValues rowVals = this.getRowValuesTable().getRowValuesTableModel().getRowValuesAt(i);
                        int niveauCourant = niveau;
                        if (rowVals.getObject("NIVEAU") != null) {
                            niveauCourant = rowVals.getInt("NIVEAU");
                        }
                        if (niveauCourant > 0) {
                            if (niveauCourant < niveau || niveauCourant == 1) {
                                break;
                            } else if (niveauCourant == niveau) {
                                update = true;
                                // Cumul des valeurs
                                prixUnitHT = prixUnitHT.add(rowVals.getBigDecimal("PV_HT").multiply(new BigDecimal(rowVals.getInt("QTE"))).multiply(rowVals.getBigDecimal("QTE_UNITAIRE")));
                                prixUnitHA = prixUnitHA.add(rowVals.getBigDecimal("PA_HT").multiply(new BigDecimal(rowVals.getInt("QTE"))).multiply(rowVals.getBigDecimal("QTE_UNITAIRE")));
                            }
                        }
                    }
                    if (update) {
                        final int columnForFieldHA = this.model.getColumnForField("PRIX_METRIQUE_HA_1");
                        if (columnForFieldHA >= 0) {
                            this.model.setValueAt(prixUnitHA, indexToUpdate, columnForFieldHA);
                        }

                        final int columnForFieldPVht1 = this.model.getColumnForField("PRIX_METRIQUE_VT_1");
                        if (columnForFieldPVht1 >= 0) {
                            this.model.setValueAt(prixUnitHT, indexToUpdate, columnForFieldPVht1);
                        }
                    }
                    index = indexToUpdate - 1;
                }
            }
        }
    }

    private void checkNiveau() {

        int n = this.model.getRowCount();
        final int columnForField = this.model.getColumnForField("NIVEAU");
        if (n > 0 && columnForField >= 0) {
            int start = 0;
            for (int i = 0; i < n; i++) {
                start = i;
                SQLRowValues rowVals = this.model.getRowValuesAt(i);
                if (rowVals.getObject("NIVEAU") == null || rowVals.getInt("NIVEAU") >= 1) {
                    this.model.setValueAt(1, i, columnForField);
                    break;
                }
            }

            // Dernier niveau correct autre que -1
            int lastGoodPrevious = this.model.getRowValuesAt(start).getInt("NIVEAU");
            for (int i = start + 1; i < n; i++) {
                // SQLRowValues rowValsPrev = this.model.getRowValuesAt(i - 1);
                SQLRowValues rowVals = this.model.getRowValuesAt(i);
                if (rowVals.getObject("NIVEAU") == null) {
                    this.model.setValueAt(1, i, columnForField);
                }
                // int niveauPrev = rowValsPrev.getInt("NIVEAU");
                int niveau = rowVals.getInt("NIVEAU");
                if (niveau != -1) {

                    if (niveau - lastGoodPrevious > 1) {
                        this.model.setValueAt(lastGoodPrevious, i, columnForField);
                    }
                    lastGoodPrevious = niveau;
                }
            }
        }
    }

    public List<SQLRowValues> getRowValuesAtLevel(int level) {
        final int rowCount = this.model.getRowCount();
        final List<SQLRowValues> result = new ArrayList<SQLRowValues>(rowCount);
        for (int i = 0; i < rowCount; i++) {
            final SQLRowValues row = this.model.getRowValuesAt(i);
            if (row.getObject("NIVEAU") == null || row.getInt("NIVEAU") == level) {
                result.add(row);
            }
        }
        return result;
    }

    public static enum EXPAND_TYPE {
        VIEW_ONLY, EXPAND, FLAT
    };

    public void expandNomenclature(int index, AutoCompletionManager m, final EXPAND_TYPE type) {
        SQLRowValues rowValsLineFather = this.model.getRowValuesAt(index);
        if (!rowValsLineFather.isForeignEmpty("ID_ARTICLE")) {

            if (type == EXPAND_TYPE.EXPAND) {
                int a1 = JOptionPane.showConfirmDialog(this.table, TM.tr("product.bom.expand.warning"), "Warning", JOptionPane.OK_CANCEL_OPTION);
                if (a1 != JOptionPane.YES_OPTION) {
                    return;
                }
            } else if (type == EXPAND_TYPE.FLAT) {
                int a1 = JOptionPane.showConfirmDialog(this.table, TM.tr("product.bom.flatexpand.warning"), "Warning", JOptionPane.OK_CANCEL_OPTION);
                if (a1 != JOptionPane.YES_OPTION) {
                    return;
                }
            }

            final int fatherLevel = rowValsLineFather.getInt("NIVEAU");
            // Test si il existe déjà des éléments d'un niveau inférieur dans le tableau
            if (index < table.getRowCount() - 1) {
                SQLRowValues rowValsLineNext = this.model.getRowValuesAt(index + 1);
                if (fatherLevel < rowValsLineNext.getInt("NIVEAU")) {
                    int a = JOptionPane.showConfirmDialog(this.table, "Cette ligne contient déjà des éléments d'un niveau inférieur. Êtes vous sûr de vouloir éclater la nomenclature?", "Nomenclature",
                            JOptionPane.YES_NO_OPTION);
                    if (a == JOptionPane.NO_OPTION) {
                        return;
                    }
                }
            }

            SQLRowAccessor rowValsArticleFather = rowValsLineFather.getForeign("ID_ARTICLE");
            // Elements composant la nomenclature
            Collection<? extends SQLRowAccessor> elts = rowValsArticleFather.getReferentRows(rowValsArticleFather.getTable().getTable("ARTICLE_ELEMENT").getField("ID_ARTICLE_PARENT"));

            if (elts.size() == 0) {
                JOptionPane.showMessageDialog(this.table, "Cet article ne contient aucun élément.");
            }

            List<? extends SQLRowAccessor> eltsList = new ArrayList<SQLRowAccessor>(elts);
            if (type == EXPAND_TYPE.FLAT) {
                this.model.putValue(-1, index, "NIVEAU");
                // this.model.putValue(allStyleByName.get("Composant"), index, "ID_STYLE");
            }
            Set<String> fieldsFrom = m.getFieldsFrom();
            fieldsFrom.remove("POURCENT_REMISE");
            for (int i = eltsList.size() - 1; i >= 0; i--) {
                SQLRowAccessor sqlRowArticleChildElement = eltsList.get(i);
                final SQLRowAccessor foreignArticleChild = sqlRowArticleChildElement.getForeign("ID_ARTICLE");
                final SQLRowValues row2Insert = new SQLRowValues(this.model.getDefaultRowValues());

                m.fillRowValues(foreignArticleChild, fieldsFrom, row2Insert);

                // Fill prix total
                row2Insert.put("ID_ARTICLE", foreignArticleChild.getID());

                row2Insert.put("CODE", foreignArticleChild.getObject("CODE"));
                row2Insert.put("NOM", foreignArticleChild.getObject("NOM"));

                if (type == EXPAND_TYPE.FLAT) {
                    row2Insert.put("QTE", sqlRowArticleChildElement.getInt("QTE") * rowValsLineFather.getInt("QTE"));
                } else {
                    row2Insert.put("QTE", sqlRowArticleChildElement.getInt("QTE"));
                }
                if (row2Insert.getTable().contains("POURCENT_REMISE")) {
                    row2Insert.put("POURCENT_REMISE", BigDecimal.ZERO);
                    row2Insert.put("MONTANT_REMISE", BigDecimal.ZERO);
                }

                if (type == EXPAND_TYPE.EXPAND) {
                    row2Insert.put("NIVEAU", fatherLevel + 1);
                } else if (type == EXPAND_TYPE.VIEW_ONLY) {
                    row2Insert.put("NIVEAU", -1);
                } else if (type == EXPAND_TYPE.FLAT) {
                    row2Insert.put("NIVEAU", 1);
                }

                if (type != EXPAND_TYPE.VIEW_ONLY) {

                    if (row2Insert.getTable().contains("T_PA_TTC")) {
                        row2Insert.put("PA_HT", row2Insert.getObject("PRIX_METRIQUE_HA_1"));

                        final BigDecimal resultTotalHT = row2Insert.getBigDecimal("PA_HT").multiply(new BigDecimal(row2Insert.getInt("QTE")));
                        row2Insert.put("T_PA_HT", resultTotalHT);

                        Float resultTaux = TaxeCache.getCache().getTauxFromId(row2Insert.getForeignID("ID_TAXE"));

                        if (resultTaux == null) {
                            SQLRow rowTax = TaxeCache.getCache().getFirstTaxe();
                            resultTaux = rowTax.getFloat("TAUX");
                        }

                        float taux = (resultTaux == null) ? 0.0F : resultTaux.floatValue();

                        BigDecimal r = resultTotalHT.multiply(BigDecimal.valueOf(taux).movePointLeft(2).add(BigDecimal.ONE), DecimalUtils.HIGH_PRECISION);

                        row2Insert.put("T_PA_TTC", r);
                    } else {
                        row2Insert.put("PV_HT", row2Insert.getObject("PRIX_METRIQUE_VT_1"));

                        final BigDecimal resultTotalHT = row2Insert.getBigDecimal("PV_HT").multiply(new BigDecimal(row2Insert.getInt("QTE")));
                        row2Insert.put("T_PV_HT", resultTotalHT);

                        Float resultTaux = TaxeCache.getCache().getTauxFromId(row2Insert.getForeignID("ID_TAXE"));

                        if (resultTaux == null) {
                            SQLRow rowTax = TaxeCache.getCache().getFirstTaxe();
                            resultTaux = rowTax.getFloat("TAUX");
                        }

                        float taux = (resultTaux == null) ? 0.0F : resultTaux.floatValue();

                        BigDecimal r = resultTotalHT.multiply(BigDecimal.valueOf(taux).movePointLeft(2).add(BigDecimal.ONE), DecimalUtils.HIGH_PRECISION);

                        row2Insert.put("T_PV_TTC", r);

                    }
                }
                Map<String, Integer> allStyleByName = getSQLElement().getDirectory().getElement(StyleSQLElement.class).getAllStyleByName();
                row2Insert.put("ID_STYLE", allStyleByName.get("Composant"));
                this.model.addRowAt(index + 1, row2Insert);

            }
        }
    }

    protected List<AbstractAction> getAdditionnalMouseAction(final int rowIndex) {
        return Collections.emptyList();
    }

    public void insertFromReliquat(List<SQLRowValues> reliquats) {

        for (SQLRowValues reliquat : reliquats) {

            final SQLRowValues row2Insert = new SQLRowValues(getRowValuesTable().getRowValuesTableModel().getDefaultRowValues());

            // Completion depuis l'article trouvé
            final SQLRowAccessor article = reliquat.getForeign("ID_ARTICLE").asRow();

            row2Insert.put("ID_ARTICLE", article.getID());
            row2Insert.put("CODE", article.getObject("CODE"));
            row2Insert.put("NOM", article.getObject("NOM"));

            row2Insert.put("QTE", reliquat.getObject("QTE"));
            row2Insert.put("QTE_UNITAIRE", reliquat.getObject("QTE_UNITAIRE"));
            row2Insert.put("ID_UNITE_VENTE", reliquat.getForeignID("ID_UNITE_VENTE"));
            getRowValuesTable().getRowValuesTableModel().addRowAt(0, row2Insert);

        }
    }
}
