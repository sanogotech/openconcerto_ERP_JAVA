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
import org.openconcerto.erp.core.finance.accounting.model.CurrencyConverter;
import org.openconcerto.erp.core.finance.tax.model.TaxeCache;
import org.openconcerto.erp.core.sales.pos.io.BarcodeReader;
import org.openconcerto.erp.core.sales.pos.ui.BarcodeListener;
import org.openconcerto.erp.core.sales.product.element.ReferenceArticleSQLElement;
import org.openconcerto.erp.core.sales.product.model.ProductComponent;
import org.openconcerto.erp.core.sales.product.model.ProductHelper;
import org.openconcerto.erp.core.sales.product.model.ProductHelper.TypePrice;
import org.openconcerto.erp.core.sales.product.ui.ArticleRowValuesRenderer;
import org.openconcerto.erp.core.sales.product.ui.CurrencyWithSymbolRenderer;
import org.openconcerto.erp.core.sales.product.ui.QteMultipleRowValuesRenderer;
import org.openconcerto.erp.core.sales.product.ui.QteUnitRowValuesRenderer;
import org.openconcerto.erp.importer.ArrayTableModel;
import org.openconcerto.erp.importer.DataImporter;
import org.openconcerto.erp.preferences.DefaultNXProps;
import org.openconcerto.erp.preferences.GestionArticleGlobalPreferencePanel;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.FieldPath;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowListRSH;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.UndefinedRowValuesCache;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.model.graph.Path;
import org.openconcerto.sql.preferences.SQLPreferences;
import org.openconcerto.sql.sqlobject.ITextArticleWithCompletionCellEditor;
import org.openconcerto.sql.sqlobject.ITextWithCompletion;
import org.openconcerto.sql.users.rights.UserRights;
import org.openconcerto.sql.users.rights.UserRightsManager;
import org.openconcerto.sql.view.list.AutoCompletionManager;
import org.openconcerto.sql.view.list.CellDynamicModifier;
import org.openconcerto.sql.view.list.RowValuesTable;
import org.openconcerto.sql.view.list.RowValuesTableModel;
import org.openconcerto.sql.view.list.SQLTableElement;
import org.openconcerto.sql.view.list.SQLTextComboTableCellEditor;
import org.openconcerto.sql.view.list.ValidStateChecker;
import org.openconcerto.utils.CompareUtils;
import org.openconcerto.utils.DecimalUtils;
import org.openconcerto.utils.StringUtils;
import org.openconcerto.utils.Tuple3;
import org.openconcerto.utils.cc.ITransformer;
import org.openconcerto.utils.i18n.TranslationManager;

import java.awt.Component;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

public abstract class AbstractVenteArticleItemTable extends AbstractArticleItemTable {

    public static final String ARTICLE_SHOW_DEVISE = "ArticleShowDevise";
    public static final String ARTICLE_SERVICE = "ArticleService";

    public static final String EDIT_PRIX_VENTE_CODE = "CORPS_EDITER_PRIX_VENTE";
    public static final String SHOW_PRIX_ACHAT_CODE = "CORPS_VOIR_PRIX_ACHAT";
    public static final String LOCK_PRIX_MIN_VENTE_CODE = "CORPS_VERROU_PRIX_MIN_VENTE";

    public AbstractVenteArticleItemTable() {
        super();
    }

    public AbstractVenteArticleItemTable(List<JButton> buttons) {
        super(buttons);
    }

    protected boolean isCellNiveauEditable(SQLRowValues vals, int rowIndex, int columnIndex) {
        RowValuesTableModel model = getModel();
        int niveau = vals.getInt("NIVEAU");
        return rowIndex + 1 == model.getRowCount() || niveau >= model.getRowValuesAt(rowIndex + 1).getInt("NIVEAU");
    }

    SQLTableElement tableElementFacturable;
    protected SQLTableElement tableElementRemise;

    public enum TypeCalcul {
        CALCUL_FACTURABLE("MONTANT_FACTURABLE", "POURCENT_FACTURABLE"), CALCUL_REMISE("MONTANT_REMISE", "POURCENT_REMISE");

        String fieldMontant, fieldPourcent;

        TypeCalcul(String fieldMontant, String fieldPourcent) {
            this.fieldMontant = fieldMontant;
            this.fieldPourcent = fieldPourcent;
        }

        public String getFieldMontant() {
            return fieldMontant;
        }

        public String getFieldPourcent() {
            return fieldPourcent;
        }
    };

    public void calculPourcentage(final Acompte a, final TypeCalcul type) {
        Runnable r = new Runnable() {

            @Override
            public void run() {
                SwingUtilities.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        SQLTableElement tableElement = (type == TypeCalcul.CALCUL_FACTURABLE ? tableElementFacturable : tableElementRemise);
                        RowValuesTableModel model = getModel();
                        if (a == null) {
                            for (int i = 0; i < model.getRowCount(); i++) {
                                model.putValue(null, i, type.getFieldMontant());
                                model.putValue(null, i, type.getFieldPourcent());
                                tableElement.fireModification(model.getRowValuesAt(i));
                            }
                        } else if (a.getPercent() != null) {
                            for (int i = 0; i < model.getRowCount(); i++) {
                                model.putValue(a.getPercent(), i, type.getFieldPourcent());
                                model.putValue(null, i, type.getFieldMontant());
                                tableElement.fireModification(model.getRowValuesAt(i));
                            }
                        } else {
                            // FIXME repartition du montant sur chaque ligne
                            BigDecimal totalHT = BigDecimal.ZERO;
                            for (SQLRowValues rowVals : getRowValuesAtLevel(1)) {
                                int qte = rowVals.getInt("QTE");
                                BigDecimal qteU = rowVals.getBigDecimal("QTE_UNITAIRE");
                                BigDecimal pU = rowVals.getBigDecimal("PV_HT");

                                BigDecimal totalLine = pU.multiply(qteU, DecimalUtils.HIGH_PRECISION).multiply(new BigDecimal(qte), DecimalUtils.HIGH_PRECISION).setScale(2, RoundingMode.HALF_UP);

                                // BigDecimal lremise = (type == TypeCalcul.CALCUL_FACTURABLE ?
                                // rowVals.getBigDecimal("POURCENT_REMISE") : BigDecimal.ZERO);
                                //
                                // if (lremise.compareTo(BigDecimal.ZERO) > 0 &&
                                // lremise.compareTo(BigDecimal.valueOf(100)) < 100) {
                                // totalLine =
                                // totalLine.multiply(BigDecimal.valueOf(100).subtract(lremise),
                                // DecimalUtils.HIGH_PRECISION).movePointLeft(2);
                                // }
                                if (type == TypeCalcul.CALCUL_FACTURABLE) {
                                    if (rowVals.getTable().getFieldsName().contains("MONTANT_REMISE")) {
                                        final BigDecimal acomptePercent = rowVals.getBigDecimal("POURCENT_REMISE");
                                        final BigDecimal acompteMontant = rowVals.getBigDecimal("MONTANT_REMISE");
                                        Remise remise = new Remise(acomptePercent, acompteMontant);
                                        totalLine = remise.getResultFrom(totalLine);
                                    }
                                }
                                totalHT = totalHT.add(totalLine);
                            }

                            // BigDecimal percent = (totalHT.signum() != 0 ?
                            // a.getMontant().divide(totalHT, DecimalUtils.HIGH_PRECISION) :
                            // BigDecimal.ZERO);

                            for (int i = 0; i < model.getRowCount(); i++) {
                                // Restrict to level 1
                                if (model.getRowValuesAt(i).getInt("NIVEAU") != 1) {
                                    continue;
                                }
                                model.putValue(null, i, type.getFieldPourcent());
                                SQLRowValues rowVals = model.getRowValuesAt(i);
                                int qte = rowVals.getInt("QTE");
                                BigDecimal qteU = rowVals.getBigDecimal("QTE_UNITAIRE");
                                BigDecimal pU = rowVals.getBigDecimal("PV_HT");

                                BigDecimal totalLine = pU.multiply(qteU, DecimalUtils.HIGH_PRECISION).multiply(new BigDecimal(qte), DecimalUtils.HIGH_PRECISION).setScale(2, RoundingMode.HALF_UP);

                                // BigDecimal lremise = (type == TypeCalcul.CALCUL_FACTURABLE ?
                                // rowVals.getBigDecimal("POURCENT_REMISE") : BigDecimal.ZERO);
                                //
                                // if (lremise.compareTo(BigDecimal.ZERO) > 0 &&
                                // lremise.compareTo(BigDecimal.valueOf(100)) < 100) {
                                // totalLine =
                                // totalLine.multiply(BigDecimal.valueOf(100).subtract(lremise),
                                // DecimalUtils.HIGH_PRECISION).movePointLeft(2);
                                // }
                                if (rowVals.getTable().getFieldsName().contains("MONTANT_REMISE")) {
                                    final BigDecimal acomptePercent = rowVals.getBigDecimal("POURCENT_REMISE");
                                    final BigDecimal acompteMontant = rowVals.getBigDecimal("MONTANT_REMISE");
                                    Remise remise = new Remise(acomptePercent, acompteMontant);
                                    totalLine = remise.getResultFrom(totalLine);
                                }

                                BigDecimal percent = (totalHT.signum() != 0 ? totalLine.divide(totalHT, DecimalUtils.HIGH_PRECISION) : BigDecimal.ZERO);

                                model.putValue(a.getMontant().multiply(percent, DecimalUtils.HIGH_PRECISION).setScale(6, RoundingMode.HALF_UP), i, type.getFieldMontant());
                                tableElement.fireModification(model.getRowValuesAt(i));
                            }
                        }
                        model.fireTableDataChanged();
                    }
                });
            }
        };
        getModel().submit(r);

    }

    private static Map<String, Boolean> visibilityMap = new HashMap<String, Boolean>();

    public static Map<String, Boolean> getVisibilityMap() {
        return visibilityMap;
    }

    private SQLTable tableArticleTarif = Configuration.getInstance().getBase().getTable("ARTICLE_TARIF");
    private SQLTable tableArticle = Configuration.getInstance().getBase().getTable("ARTICLE");

    protected void init() {

        SQLPreferences prefs = SQLPreferences.getMemCached(getSQLElement().getTable().getDBRoot());
        final boolean selectArticle = prefs.getBoolean(GestionArticleGlobalPreferencePanel.USE_CREATED_ARTICLE, false);
        final boolean filterFamilleArticle = prefs.getBoolean(GestionArticleGlobalPreferencePanel.FILTER_BY_FAMILY, false);
        final boolean createAuto = prefs.getBoolean(GestionArticleGlobalPreferencePanel.CREATE_ARTICLE_AUTO, true);
        final boolean showEco = prefs.getBoolean(AbstractVenteArticleItemTable.SHOW_ECO_CONTRIBUTION_COLUMNS, false);

        final UserRights rights = UserRightsManager.getCurrentUserRights();
        final boolean editVTPrice = rights.haveRight(EDIT_PRIX_VENTE_CODE);
        final boolean showHAPrice = rights.haveRight(SHOW_PRIX_ACHAT_CODE);
        final boolean lockVTMinPrice = rights.haveRight(LOCK_PRIX_MIN_VENTE_CODE);

        final SQLElement e = getSQLElement();

        final List<SQLTableElement> list = new Vector<SQLTableElement>();
        final SQLTableElement eNiveau = new SQLTableElement(e.getTable().getField("NIVEAU")) {
            @Override
            public void setValueFrom(SQLRowValues row, Object value) {
                super.setValueFrom(row, value);
            }
        };
        eNiveau.setRenderer(new NiveauTableCellRender());
        eNiveau.setEditor(new NiveauTableCellEditor());
        list.add(eNiveau);

        list.add(new SQLTableElement(e.getTable().getField("ID_STYLE")));

        final SQLTableElement tableFamille = new SQLTableElement(e.getTable().getField("ID_FAMILLE_ARTICLE"));
        list.add(tableFamille);

        // Article
        SQLTableElement tableElementArticle = new SQLTableElement(e.getTable().getField("ID_ARTICLE"), true, true, true);
        list.add(tableElementArticle);

        if (e.getTable().getFieldsName().contains("ID_ECO_CONTRIBUTION")) {
            this.tableElementEcoID = new SQLTableElement(e.getTable().getField("ID_ECO_CONTRIBUTION"));
            list.add(this.tableElementEcoID);
        }

        // Code article
        final SQLTableElement tableElementCode = new SQLTableElement(e.getTable().getField("CODE"), String.class,
                new ITextArticleWithCompletionCellEditor(e.getTable().getTable("ARTICLE"), e.getTable().getTable("ARTICLE_FOURNISSEUR")));
        list.add(tableElementCode);

        // Désignation de l'article
        final SQLTableElement tableElementNom = new SQLTableElement(e.getTable().getField("NOM"));
        list.add(tableElementNom);

        if (e.getTable().getFieldsName().contains("COLORIS")) {
            final SQLTableElement tableElementColoris = new SQLTableElement(e.getTable().getField("COLORIS"));
            list.add(tableElementColoris);
        }

        if (e.getTable().getFieldsName().contains("DESCRIPTIF")) {
            final SQLTableElement tableElementDesc = new SQLTableElement(e.getTable().getField("DESCRIPTIF"));
            list.add(tableElementDesc);
        }

        if (DefaultNXProps.getInstance().getBooleanValue(ARTICLE_SHOW_DEVISE, false)) {
            // Code Douanier
            final SQLTableElement tableElementCodeDouane = new SQLTableElement(e.getTable().getField("CODE_DOUANIER"));
            list.add(tableElementCodeDouane);
        }
        if (DefaultNXProps.getInstance().getBooleanValue(ARTICLE_SHOW_DEVISE, false)) {
            final SQLTableElement tableElementPays = new SQLTableElement(e.getTable().getField("ID_PAYS"));
            list.add(tableElementPays);
        }

        // Valeur des métriques
        final SQLTableElement tableElement_ValeurMetrique2 = new SQLTableElement(e.getTable().getField("VALEUR_METRIQUE_2"), Float.class) {
            @Override
            public boolean isCellEditable(SQLRowValues vals, int rowIndex, int columnIndex) {
                Number modeNumber = (Number) vals.getObject("ID_MODE_VENTE_ARTICLE");
                // int mode = vals.getInt("ID_MODE_VENTE_ARTICLE");
                if (modeNumber != null && (modeNumber.intValue() == ReferenceArticleSQLElement.A_LA_PIECE || modeNumber.intValue() == ReferenceArticleSQLElement.AU_POID_METRECARRE
                        || modeNumber.intValue() == ReferenceArticleSQLElement.AU_METRE_LONGUEUR)) {
                    return false;
                } else {
                    return super.isCellEditable(vals, rowIndex, columnIndex);
                }
            }

            @Override
            public TableCellRenderer getTableCellRenderer() {

                return new ArticleRowValuesRenderer(null);
            }
        };
        list.add(tableElement_ValeurMetrique2);
        final SQLTableElement tableElement_ValeurMetrique3 = new SQLTableElement(e.getTable().getField("VALEUR_METRIQUE_3"), Float.class) {
            @Override
            public boolean isCellEditable(SQLRowValues vals, int rowIndex, int columnIndex) {

                Number modeNumber = (Number) vals.getObject("ID_MODE_VENTE_ARTICLE");
                if (modeNumber != null && (!(modeNumber.intValue() == ReferenceArticleSQLElement.AU_POID_METRECARRE))) {
                    return false;
                } else {
                    return super.isCellEditable(vals, rowIndex, columnIndex);
                }
            }

            @Override
            public TableCellRenderer getTableCellRenderer() {

                return new ArticleRowValuesRenderer(null);
            }
        };
        list.add(tableElement_ValeurMetrique3);
        final SQLTableElement tableElement_ValeurMetrique1 = new SQLTableElement(e.getTable().getField("VALEUR_METRIQUE_1"), Float.class) {
            @Override
            public boolean isCellEditable(SQLRowValues vals, int rowIndex, int columnIndex) {

                Number modeNumber = (Number) vals.getObject("ID_MODE_VENTE_ARTICLE");
                if (modeNumber != null && (modeNumber.intValue() == ReferenceArticleSQLElement.A_LA_PIECE || modeNumber.intValue() == ReferenceArticleSQLElement.AU_POID_METRECARRE
                        || modeNumber.intValue() == ReferenceArticleSQLElement.AU_METRE_LARGEUR)) {
                    return false;
                } else {
                    return super.isCellEditable(vals, rowIndex, columnIndex);
                }
            }

            @Override
            public TableCellRenderer getTableCellRenderer() {

                return new ArticleRowValuesRenderer(null);
            }
        };
        list.add(tableElement_ValeurMetrique1);

        // Prébilan

        if (e.getTable().getFieldsName().contains("PREBILAN")) {
            prebilan = new SQLTableElement(e.getTable().getField("PREBILAN"), BigDecimal.class) {
                protected Object getDefaultNullValue() {
                    return BigDecimal.ZERO;
                }

                @Override
                public boolean isCellEditable(SQLRowValues vals, int rowIndex, int columnIndex) {
                    return isCellNiveauEditable(vals, rowIndex, columnIndex);
                }

            };
            prebilan.setRenderer(new DeviseTableCellRenderer());
            list.add(prebilan);
        }

        // Prix d'achat HT de la métrique 1
        final SQLTableElement tableElement_PrixMetrique1_AchatHT = new SQLTableElement(e.getTable().getField("PRIX_METRIQUE_HA_1"), BigDecimal.class) {
            protected Object getDefaultNullValue() {
                return BigDecimal.ZERO;
            }

            @Override
            public boolean isCellEditable(SQLRowValues vals, int rowIndex, int columnIndex) {
                return isCellNiveauEditable(vals, rowIndex, columnIndex);
            }

        };
        tableElement_PrixMetrique1_AchatHT.setRenderer(new CurrencyWithSymbolRenderer());
        list.add(tableElement_PrixMetrique1_AchatHT);

        SQLTableElement eltDevise = null;
        SQLTableElement eltUnitDevise = null;
        if (DefaultNXProps.getInstance().getBooleanValue(ARTICLE_SHOW_DEVISE, false)) {
            // Devise
            eltDevise = new SQLTableElement(e.getTable().getField("ID_DEVISE"));
            list.add(eltDevise);

            // Prix vente devise
            eltUnitDevise = new SQLTableElement(e.getTable().getField("PV_U_DEVISE"), BigDecimal.class) {
                @Override
                public boolean isCellEditable(SQLRowValues vals, int rowIndex, int columnIndex) {
                    return editVTPrice && isCellNiveauEditable(vals, rowIndex, columnIndex);
                }

                protected Object getDefaultNullValue() {
                    return BigDecimal.ZERO;
                }

            };
            Path p = new Path(getSQLElement().getTable()).addForeignField("ID_DEVISE");
            eltUnitDevise.setRenderer(new CurrencyWithSymbolRenderer(new FieldPath(p, "CODE")));
            list.add(eltUnitDevise);
        }
        // Prix de vente HT de la métrique 1

        SQLField field = e.getTable().getField("PRIX_METRIQUE_VT_1");
        final DeviseNumericHTConvertorCellEditor editorPVHT = new DeviseNumericHTConvertorCellEditor(field);

        final SQLTableElement tableElement_PrixMetrique1_VenteHT = new SQLTableElement(field, BigDecimal.class, editorPVHT) {
            @Override
            public boolean isCellEditable(SQLRowValues vals, int rowIndex, int columnIndex) {
                return editVTPrice && isCellNiveauEditable(vals, rowIndex, columnIndex);
            }
        };
        tableElement_PrixMetrique1_VenteHT.setRenderer(new CurrencyWithSymbolRenderer());
        list.add(tableElement_PrixMetrique1_VenteHT);

        if (e.getTable().getFieldsName().contains("ECO_CONTRIBUTION")) {
            this.tableElementEco = new SQLTableElement(e.getTable().getField("ECO_CONTRIBUTION"));
            list.add(this.tableElementEco);
        }

        // // Prix d'achat HT de la métrique 1

        SQLTableElement qteU = new SQLTableElement(e.getTable().getField("QTE_UNITAIRE"), BigDecimal.class) {
            @Override
            public boolean isCellEditable(SQLRowValues vals, int rowIndex, int columnIndex) {

                SQLRowAccessor row = vals.getForeign("ID_UNITE_VENTE");
                if (row != null && !row.isUndefined() && row.getBoolean("A_LA_PIECE")) {
                    return false;
                } else {
                    return super.isCellEditable(vals, rowIndex, columnIndex);
                }
            }

            @Override
            public TableCellRenderer getTableCellRenderer() {
                return new QteUnitRowValuesRenderer();
            }

            protected Object getDefaultNullValue() {
                return BigDecimal.ZERO;
            }
        };
        list.add(qteU);

        SQLTableElement uniteVente = new SQLTableElement(e.getTable().getField("ID_UNITE_VENTE"));
        list.add(uniteVente);

        // Quantité
        this.qte = new SQLTableElement(e.getTable().getField("QTE"), Integer.class, new QteCellEditor()) {
            protected Object getDefaultNullValue() {
                return Integer.valueOf(0);
            }

            public TableCellRenderer getTableCellRenderer() {
                if (getSQLElement().getTable().getFieldsName().contains("QTE_ACHAT")) {
                    return new QteMultipleRowValuesRenderer();
                } else {
                    return super.getTableCellRenderer();
                }
            }
        };
        this.qte.setPreferredSize(20);
        list.add(this.qte);

        if (e.getTable().contains("RETOUR_STOCK")) {
            list.add(new SQLTableElement(e.getTable().getField("RETOUR_STOCK")));
        }

        // Mode de vente
        final SQLTableElement tableElement_ModeVente = new SQLTableElement(e.getTable().getField("ID_MODE_VENTE_ARTICLE"));
        list.add(tableElement_ModeVente);

        // // Prix d'achat unitaire HT

        final SQLField prixAchatHTField = e.getTable().getField("PA_HT");
        final DeviseNumericCellEditor editorPAchatHT = new DeviseNumericCellEditor(prixAchatHTField);
        this.ha = new SQLTableElement(e.getTable().getField("PA_HT"), BigDecimal.class, editorPAchatHT) {
            protected Object getDefaultNullValue() {
                return BigDecimal.ZERO;
            }

            @Override
            public boolean isCellEditable(SQLRowValues vals, int rowIndex, int columnIndex) {
                return isCellNiveauEditable(vals, rowIndex, columnIndex);
            }
        };
        this.ha = new SQLTableElement(prixAchatHTField, BigDecimal.class, editorPAchatHT) {
            @Override
            public boolean isCellEditable(SQLRowValues vals, int rowIndex, int columnIndex) {
                return isCellNiveauEditable(vals, rowIndex, columnIndex);
            }
        };
        this.ha.setRenderer(new CurrencyWithSymbolRenderer());

        list.add(this.ha);

        // Prix de vente unitaire HT
        final SQLField prixVenteHTField = e.getTable().getField("PV_HT");
        final DeviseNumericCellEditor editorPVenteHT = new DeviseNumericCellEditor(prixAchatHTField);
        final SQLTableElement tableElement_PrixVente_HT = new SQLTableElement(prixVenteHTField, BigDecimal.class, editorPVenteHT) {
            @Override
            public boolean isCellEditable(SQLRowValues vals, int rowIndex, int columnIndex) {
                return editVTPrice && isCellNiveauEditable(vals, rowIndex, columnIndex);
            }
        };
        tableElement_PrixVente_HT.setRenderer(new CurrencyWithSymbolRenderer());
        list.add(tableElement_PrixVente_HT);

        // TVA
        this.tableElementTVA = new SQLTableElement(e.getTable().getField("ID_TAXE"));
        this.tableElementTVA.setPreferredSize(20);
        list.add(this.tableElementTVA);
        // Poids piece
        SQLTableElement tableElementPoids = new SQLTableElement(e.getTable().getField("POIDS"), Float.class) {
            protected Object getDefaultNullValue() {
                return 0F;
            }

            @Override
            public TableCellRenderer getTableCellRenderer() {
                return new QteUnitRowValuesRenderer();
            }

        };
        tableElementPoids.setPreferredSize(20);
        list.add(tableElementPoids);

        // Poids total
        this.tableElementPoidsTotal = new SQLTableElement(e.getTable().getField("T_POIDS"), Float.class) {
            @Override
            public TableCellRenderer getTableCellRenderer() {
                return new QteUnitRowValuesRenderer();
            }
        };
        this.tableElementPoidsTotal.setEditable(false);
        list.add(this.tableElementPoidsTotal);

        // Packaging
        if (prefs.getBoolean(GestionArticleGlobalPreferencePanel.ITEM_PACKAGING, false)) {

            SQLTableElement poidsColis = new SQLTableElement(e.getTable().getField("POIDS_COLIS_NET"), BigDecimal.class) {
                @Override
                public TableCellRenderer getTableCellRenderer() {
                    return new QteUnitRowValuesRenderer();
                }

            };
            list.add(poidsColis);

            SQLTableElement nbColis = new SQLTableElement(e.getTable().getField("NB_COLIS"), Integer.class);
            list.add(nbColis);

            final SQLTableElement totalPoidsColis = new SQLTableElement(e.getTable().getField("T_POIDS_COLIS_NET"), BigDecimal.class) {
                @Override
                public TableCellRenderer getTableCellRenderer() {
                    return new QteUnitRowValuesRenderer();
                }

            };
            list.add(totalPoidsColis);

            poidsColis.addModificationListener(totalPoidsColis);
            nbColis.addModificationListener(totalPoidsColis);
            totalPoidsColis.setModifier(new CellDynamicModifier() {
                public Object computeValueFrom(final SQLRowValues row, SQLTableElement source) {
                    final Object o2 = row.getObject("POIDS_COLIS_NET");
                    final Object o3 = row.getObject("NB_COLIS");
                    if (o2 != null && o3 != null) {
                        BigDecimal poids = (BigDecimal) o2;
                        int nb = (Integer) o3;
                        return poids.multiply(new BigDecimal(nb), DecimalUtils.HIGH_PRECISION).setScale(totalPoidsColis.getDecimalDigits(), RoundingMode.HALF_UP);
                    } else {
                        return row.getObject("T_POIDS_COLIS_NET");
                    }
                }
            });

        }

        // Service
        if (DefaultNXProps.getInstance().getBooleanValue(ARTICLE_SERVICE, false)) {
            this.service = new SQLTableElement(e.getTable().getField("SERVICE"), Boolean.class);
            list.add(this.service);
        }

        this.totalHT = new SQLTableElement(e.getTable().getField("T_PV_HT"), BigDecimal.class) {
            @Override
            public boolean isCellEditable(SQLRowValues vals, int rowIndex, int columnIndex) {
                return isCellNiveauEditable(vals, rowIndex, columnIndex);
            }
        };
        this.totalHT.setRenderer(new CurrencyWithSymbolRenderer());
        this.totalHT.setEditable(false);
        if (e.getTable().getFieldsName().contains("MONTANT_FACTURABLE")) {
            // SQLTableElement tableElementAcompte = new
            // SQLTableElement(e.getTable().getField("POURCENT_ACOMPTE"));
            // list.add(tableElementAcompte);

            this.tableElementFacturable = new SQLTableElement(e.getTable().getField("POURCENT_FACTURABLE"), Acompte.class, new AcompteCellEditor("POURCENT_FACTURABLE", "MONTANT_FACTURABLE")) {
                @Override
                public void setValueFrom(SQLRowValues row, Object value) {

                    if (value != null) {
                        Acompte a = (Acompte) value;
                        row.put("MONTANT_FACTURABLE", a.getMontant());
                        row.put("POURCENT_FACTURABLE", a.getPercent());
                    } else {
                        row.put("MONTANT_FACTURABLE", null);
                        row.put("POURCENT_FACTURABLE", BigDecimal.ONE.movePointRight(2));
                    }
                    fireModification(row);
                }
            };
            tableElementFacturable.setRenderer(new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                    SQLRowValues rowVals = ((RowValuesTable) table).getRowValuesTableModel().getRowValuesAt(row);
                    JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                    BigDecimal percent = rowVals.getBigDecimal("POURCENT_FACTURABLE");
                    BigDecimal amount = rowVals.getBigDecimal("MONTANT_FACTURABLE");
                    Acompte a = new Acompte(percent, amount);
                    label.setText(a.toPlainString(true));
                    return label;
                }
            });
            tableElementFacturable.addModificationListener(this.totalHT);
            list.add(tableElementFacturable);
        }

        final SQLField fieldRemise = e.getTable().getField("POURCENT_REMISE");

        if (e.getTable().getFieldsName().contains("MONTANT_REMISE")) {
            tableElementRemise = new SQLTableElement(e.getTable().getField("POURCENT_REMISE"), Acompte.class, new AcompteCellEditor("POURCENT_REMISE", "MONTANT_REMISE")) {
                @Override
                public void setValueFrom(SQLRowValues row, Object value) {

                    if (value != null) {
                        Acompte a = (Acompte) value;
                        row.put("MONTANT_REMISE", a.getMontant());
                        row.put("POURCENT_REMISE", a.getPercent());
                    } else {
                        row.put("MONTANT_REMISE", null);
                        row.put("POURCENT_REMISE", BigDecimal.ZERO);
                    }
                    fireModification(row);
                }
            };
            tableElementRemise.setRenderer(new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                    SQLRowValues rowVals = ((RowValuesTable) table).getRowValuesTableModel().getRowValuesAt(row);
                    JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                    BigDecimal percent = rowVals.getBigDecimal("POURCENT_REMISE");
                    BigDecimal amount = rowVals.getBigDecimal("MONTANT_REMISE");
                    Remise a = new Remise(percent, amount);
                    label.setText(a.toPlainString(true));
                    return label;
                }
            });
        } else {
            tableElementRemise = new SQLTableElement(fieldRemise) {
                protected Object getDefaultNullValue() {
                    return BigDecimal.ZERO;
                }
            };
        }
        list.add(tableElementRemise);
        SQLTableElement tableElementRG = null;
        if (e.getTable().getFieldsName().contains("POURCENT_RG")) {
            tableElementRG = new SQLTableElement(e.getTable().getField("POURCENT_RG"));
            list.add(tableElementRG);
        }

        // Total HT
        this.totalHA = new SQLTableElement(e.getTable().getField("T_PA_HT"), BigDecimal.class);
        this.totalHA.setRenderer(new CurrencyWithSymbolRenderer());
        this.totalHA.setEditable(false);
        list.add(this.totalHA);

        if (DefaultNXProps.getInstance().getBooleanValue(ARTICLE_SHOW_DEVISE, false)) {
            // Total HT
            this.tableElementTotalDevise = new SQLTableElement(e.getTable().getField("PV_T_DEVISE"), BigDecimal.class) {
                @Override
                public boolean isCellEditable(SQLRowValues vals, int rowIndex, int columnIndex) {
                    return isCellNiveauEditable(vals, rowIndex, columnIndex);
                }
            };
            Path p = new Path(getSQLElement().getTable()).addForeignField("ID_DEVISE");
            this.tableElementTotalDevise.setRenderer(new CurrencyWithSymbolRenderer(new FieldPath(p, "CODE")));
            list.add(tableElementTotalDevise);
        }

        // Marge HT
        if (e.getTable().getFieldsName().contains("MARGE_HT")) {

            final SQLTableElement marge = new SQLTableElement(e.getTable().getField("MARGE_HT"), BigDecimal.class) {
                protected Object getDefaultNullValue() {
                    return BigDecimal.ZERO;
                }

                @Override
                public boolean isCellEditable(SQLRowValues vals, int rowIndex, int columnIndex) {
                    return isCellNiveauEditable(vals, rowIndex, columnIndex);
                }

            };
            marge.setRenderer(new CurrencyWithSymbolRenderer());
            marge.setEditable(false);
            list.add(marge);
            this.totalHT.addModificationListener(marge);
            this.totalHA.addModificationListener(marge);
            marge.setModifier(new CellDynamicModifier() {
                @Override
                public Object computeValueFrom(SQLRowValues row, SQLTableElement source) {

                    BigDecimal vt = (BigDecimal) row.getObject("T_PV_HT");

                    BigDecimal ha = (BigDecimal) row.getObject("T_PA_HT");

                    final BigDecimal acomptePercent = row.getBigDecimal("POURCENT_FACTURABLE");
                    final BigDecimal acompteMontant = row.getBigDecimal("MONTANT_FACTURABLE");
                    Acompte acompte = new Acompte(acomptePercent, acompteMontant);
                    ha = acompte.getResultFrom(ha);
                    vt = acompte.getResultFrom(vt);

                    return vt.subtract(ha).setScale(marge.getDecimalDigits(), RoundingMode.HALF_UP);
                }

            });

        }

        if (e.getTable().getFieldsName().contains("MARGE_PREBILAN_HT")) {

            final SQLTableElement marge = new SQLTableElement(e.getTable().getField("MARGE_PREBILAN_HT"), BigDecimal.class) {
                protected Object getDefaultNullValue() {
                    return BigDecimal.ZERO;
                }
            };
            marge.setRenderer(new DeviseTableCellRenderer());
            marge.setEditable(false);
            list.add(marge);
            this.totalHT.addModificationListener(marge);
            prebilan.addModificationListener(marge);
            marge.setModifier(new CellDynamicModifier() {
                @Override
                public Object computeValueFrom(SQLRowValues row, SQLTableElement source) {

                    BigDecimal vt = (BigDecimal) row.getObject("T_PV_HT");

                    BigDecimal ha = row.getObject("PREBILAN") == null ? BigDecimal.ZERO : (BigDecimal) row.getObject("PREBILAN");

                    final BigDecimal acomptePercent = row.getBigDecimal("POURCENT_FACTURABLE");
                    final BigDecimal acompteMontant = row.getBigDecimal("MONTANT_FACTURABLE");
                    Acompte acompte = new Acompte(acomptePercent, acompteMontant);
                    ha = acompte.getResultFrom(ha);
                    vt = acompte.getResultFrom(vt);
                    return vt.subtract(ha).setScale(marge.getDecimalDigits(), RoundingMode.HALF_UP);
                }

            });

        }

        if (e.getTable().getFieldsName().contains("T_ECO_CONTRIBUTION")) {
            this.tableElementEcoTotal = new SQLTableElement(e.getTable().getField("T_ECO_CONTRIBUTION"));
            list.add(this.tableElementEcoTotal);
        }

        // Total HT

        this.totalHT.setEditable(false);
        list.add(this.totalHT);
        // Total TTC
        // FIXME add a modifier -> T_TTC modify P_VT_METRIQUE_1 + fix CellDynamicModifier not fire
        // if value not changed
        this.tableElementTotalTTC = new SQLTableElement(e.getTable().getField("T_PV_TTC"), BigDecimal.class) {
            @Override
            public boolean isCellEditable(SQLRowValues vals, int rowIndex, int columnIndex) {
                return isCellNiveauEditable(vals, rowIndex, columnIndex);
            }
        };
        this.tableElementTotalTTC.setRenderer(new CurrencyWithSymbolRenderer());
        this.tableElementTotalTTC.setEditable(false);
        list.add(this.tableElementTotalTTC);

        this.defaultRowVals = new SQLRowValues(UndefinedRowValuesCache.getInstance().getDefaultRowValues(e.getTable()));
        defaultRowVals.put("ID_TAXE", TaxeCache.getCache().getFirstTaxe().getID());
        defaultRowVals.put("CODE", "");
        defaultRowVals.put("NOM", "");
        final RowValuesTableModel model = new RowValuesTableModel(e, list, e.getTable().getField("NOM"), false, defaultRowVals);
        setModel(model);

        this.table = new RowValuesTable(model, getConfigurationFile());
        ToolTipManager.sharedInstance().unregisterComponent(this.table);
        ToolTipManager.sharedInstance().unregisterComponent(this.table.getTableHeader());

        if (filterFamilleArticle) {
            ((SQLTextComboTableCellEditor) tableElementArticle.getTableCellEditor(this.table)).setDynamicWhere(e.getTable().getTable("ARTICLE").getField("ID_FAMILLE_ARTICLE"));
        }

        // Autocompletion
        final SQLTable sqlTableArticle = ((ComptaPropsConfiguration) Configuration.getInstance()).getRootSociete().getTable("ARTICLE");
        List<String> completionField = new ArrayList<String>();
        if (e.getTable().getFieldsName().contains("ID_ECO_CONTRIBUTION")) {
            completionField.add("ID_ECO_CONTRIBUTION");
        }
        if (DefaultNXProps.getInstance().getBooleanValue(ARTICLE_SHOW_DEVISE, false)) {
            completionField.add("POURCENT_REMISE");

            completionField.add("CODE_DOUANIER");
        }
        if (DefaultNXProps.getInstance().getBooleanValue(ARTICLE_SHOW_DEVISE, false)) {
            completionField.add("ID_PAYS");
        }
        completionField.add("ID_UNITE_VENTE");
        completionField.add("PA_HT");
        completionField.add("PV_HT");
        completionField.add("ID_TAXE");
        completionField.add("POIDS");
        completionField.add("PRIX_METRIQUE_HA_1");
        completionField.add("PRIX_METRIQUE_HA_2");
        completionField.add("PRIX_METRIQUE_HA_3");
        completionField.add("VALEUR_METRIQUE_1");
        completionField.add("VALEUR_METRIQUE_2");
        completionField.add("VALEUR_METRIQUE_3");
        completionField.add("ID_MODE_VENTE_ARTICLE");
        completionField.add("PRIX_METRIQUE_VT_1");
        completionField.add("PRIX_METRIQUE_VT_2");
        completionField.add("PRIX_METRIQUE_VT_3");
        completionField.add("SERVICE");
        completionField.add("ID_FAMILLE_ARTICLE");
        if (getSQLElement().getTable().getFieldsName().contains("DESCRIPTIF")) {
            completionField.add("DESCRIPTIF");
        }
        if (DefaultNXProps.getInstance().getBooleanValue(ARTICLE_SHOW_DEVISE, false)) {
            completionField.add("ID_DEVISE");
        }
        if (DefaultNXProps.getInstance().getBooleanValue(ARTICLE_SHOW_DEVISE, false)) {
            completionField.add("PV_U_DEVISE");
        }
        if (getSQLElement().getTable().getFieldsName().contains("QTE_ACHAT") && sqlTableArticle.getTable().getFieldsName().contains("QTE_ACHAT")) {
            completionField.add("QTE_ACHAT");
        }

        final AutoCompletionManager m = new AutoCompletionManager(tableElementCode, sqlTableArticle.getField("CODE"), this.table, this.table.getRowValuesTableModel()) {

            @Override
            protected Object getValueFrom(SQLRow row, String field, SQLRowAccessor rowDest) {
                Object res = tarifCompletion(row, field, rowDest, true);
                if (res == null) {
                    return super.getValueFrom(row, field, rowDest);
                } else {
                    return res;
                }
            }

        };
        m.fill("NOM", "NOM");
        m.fill("ID", "ID_ARTICLE");
        for (String string : completionField) {
            m.fill(string, string);
        }

        ITransformer<SQLSelect, SQLSelect> selTrans = new ITransformer<SQLSelect, SQLSelect>() {
            @Override
            public SQLSelect transformChecked(SQLSelect input) {

                final SQLTable tableStock = sqlTableArticle.getTable("STOCK");
                input.andWhere(new Where(tableStock.getKey(), "=", sqlTableArticle.getField("ID_STOCK")));
                input.setExcludeUndefined(false, tableStock);
                Where w = new Where(sqlTableArticle.getField("OBSOLETE"), "=", Boolean.FALSE).or(new Where(input.getAlias(tableStock.getKey()), "=", tableStock.getUndefinedID()))
                        .or(new Where(input.getAlias(tableStock.getField("QTE_REEL")), ">", 0));

                if (input.getWhere() != null) {
                    input.setWhere(input.getWhere().and(w));
                } else {
                    input.setWhere(w);
                }
                input.asString();
                return input;
            }
        };

        m.setSelectTransformer(selTrans);

        this.table.setDropTarget(new DropTarget() {
            @Override
            public synchronized void drop(DropTargetDropEvent dtde) {
                dropInTable(dtde, m);
                // super.drop(dtde);
            }
        });

        if (prefs.getBoolean(GestionArticleGlobalPreferencePanel.CAN_EXPAND_NOMENCLATURE_VT, true)) {

            table.addMouseListener(new MouseAdapter() {

                @Override
                public void mousePressed(MouseEvent e) {

                    handlePopup(e);
                }

                @Override
                public void mouseReleased(MouseEvent e) {

                    handlePopup(e);
                }

                public void handlePopup(MouseEvent e) {
                    final int rowindex = table.getSelectedRow();
                    if (rowindex < 0)
                        return;
                    if (e.isPopupTrigger() && e.getComponent() instanceof JTable) {
                        JPopupMenu popup = new JPopupMenu();
                        popup.add(new AbstractAction(TranslationManager.getInstance().getTranslationForItem("product.bom.expand")) {

                            @Override
                            public void actionPerformed(ActionEvent arg0) {
                                expandNomenclature(rowindex, m, EXPAND_TYPE.EXPAND);
                            }
                        });
                        popup.add(new AbstractAction(TranslationManager.getInstance().getTranslationForItem("product.bom.expose")) {

                            @Override
                            public void actionPerformed(ActionEvent arg0) {
                                expandNomenclature(rowindex, m, EXPAND_TYPE.VIEW_ONLY);
                            }
                        });

                        for (AbstractAction action : getAdditionnalMouseAction(rowindex)) {
                            popup.add(action);
                        }

                        popup.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            });
        }
        final AutoCompletionManager m2 = new AutoCompletionManager(tableElementNom, sqlTableArticle.getField("NOM"), this.table, this.table.getRowValuesTableModel()) {
            @Override
            protected Object getValueFrom(SQLRow row, String field, SQLRowAccessor rowDest) {
                Object res = tarifCompletion(row, field, rowDest, true);
                if (res == null) {
                    return super.getValueFrom(row, field, rowDest);
                } else {
                    return res;
                }
            }

        };
        m2.fill("CODE", "CODE");
        m2.fill("ID", "ID_ARTICLE");
        for (String string : completionField) {
            m2.fill(string, string);
        }

        m2.setSelectTransformer(selTrans);

        final AutoCompletionManager m3 = new AutoCompletionManager(tableElementArticle, sqlTableArticle.getField("NOM"), this.table, this.table.getRowValuesTableModel(),
                ITextWithCompletion.MODE_CONTAINS, true, true, new ValidStateChecker()) {
            @Override
            protected Object getValueFrom(SQLRow row, String field, SQLRowAccessor rowDest) {
                Object res = tarifCompletion(row, field, rowDest, true);
                if (res == null) {
                    return super.getValueFrom(row, field, rowDest);
                } else {
                    return res;
                }
            }

        };
        m3.fill("CODE", "CODE");
        m3.fill("NOM", "NOM");
        for (String string : completionField) {
            m3.fill(string, string);
        }

        m3.setSelectTransformer(selTrans);

        // Deselection de l'article si le code est modifié
        tableFamille.addModificationListener(tableElementArticle);
        tableElementCode.addModificationListener(tableElementArticle);
        tableElementArticle.setModifier(new CellDynamicModifier() {
            @Override
            public Object computeValueFrom(SQLRowValues row, SQLTableElement source) {
                try {
                    if (filterFamilleArticle) {
                        if (row.isForeignEmpty("ID_FAMILLE_ARTICLE")) {
                            m.setWhere(null);
                            m2.setWhere(null);
                        } else {
                            m.setWhere(new Where(sqlTableArticle.getField("ID_FAMILLE_ARTICLE"), "=", row.getForeignID("ID_FAMILLE_ARTICLE")));
                            m2.setWhere(new Where(sqlTableArticle.getField("ID_FAMILLE_ARTICLE"), "=", row.getForeignID("ID_FAMILLE_ARTICLE")));
                        }
                    }
                    SQLRowAccessor foreign = row.getForeign("ID_ARTICLE");
                    if (foreign != null && !foreign.isUndefined() && foreign.getObject("CODE") != null && foreign.getString("CODE").equals(row.getString("CODE"))) {
                        return foreign.getID();
                    } else {
                        return tableArticle.getUndefinedID();
                    }
                } catch (Exception e) {
                    return tableArticle.getUndefinedID();
                }
            }
        });

        // ECO Contribution
        if (this.tableElementEco != null && this.tableElementEcoTotal != null && this.tableElementEcoID != null) {
            this.qte.addModificationListener(this.tableElementEcoTotal);
            this.tableElementEco.addModificationListener(this.tableElementEcoTotal);
            this.tableElementEcoTotal.setModifier(new CellDynamicModifier() {
                public Object computeValueFrom(final SQLRowValues row, SQLTableElement source) {

                    int qte = Integer.parseInt(row.getObject("QTE").toString());
                    BigDecimal f = (row.getObject("ECO_CONTRIBUTION") == null) ? BigDecimal.ZERO : (BigDecimal) row.getObject("ECO_CONTRIBUTION");
                    return f.multiply(new BigDecimal(qte));
                }

            });
            this.tableElementEcoID.addModificationListener(this.tableElementEco);
            this.tableElementEco.setModifier(new CellDynamicModifier() {
                public Object computeValueFrom(final SQLRowValues row, SQLTableElement source) {

                    if (source != null && source.equals(tableElementEcoID)) {
                        return row.getForeign("ID_ECO_CONTRIBUTION").getBigDecimal("TAUX");
                    } else {
                        return row.getObject("ECO_CONTRIBUTION");
                    }
                }
            });
        }

        // Calcul automatique du total HT
        this.qte.addModificationListener(tableElement_PrixMetrique1_VenteHT);
        this.qte.addModificationListener(this.totalHT);
        this.qte.addModificationListener(this.totalHA);
        qteU.addModificationListener(this.totalHT);
        qteU.addModificationListener(this.totalHA);
        if (tableElementRG != null) {
            tableElementRG.addModificationListener(this.totalHT);
        }
        tableElementRemise.addModificationListener(this.totalHT);

        tableElement_PrixVente_HT.addModificationListener(this.totalHT);
        // tableElement_PrixVente_HT.addModificationListener(tableElement_PrixMetrique1_VenteHT);
        this.ha.addModificationListener(this.totalHA);

        this.totalHT.setModifier(new CellDynamicModifier() {
            public Object computeValueFrom(final SQLRowValues row, SQLTableElement source) {

                BigDecimal lremise = BigDecimal.ZERO;

                if (row.getTable().getFieldsName().contains("POURCENT_RG")) {
                    final Object o3 = row.getObject("POURCENT_RG");
                    if (o3 != null) {
                        lremise = lremise.add(((BigDecimal) o3));
                    }
                }

                int qte = (row.getObject("QTE") == null) ? 0 : Integer.parseInt(row.getObject("QTE").toString());
                BigDecimal b = (row.getObject("QTE_UNITAIRE") == null) ? BigDecimal.ONE : (BigDecimal) row.getObject("QTE_UNITAIRE");
                BigDecimal f = (BigDecimal) row.getObject("PV_HT");
                BigDecimal r = b.multiply(f.multiply(BigDecimal.valueOf(qte), DecimalUtils.HIGH_PRECISION), DecimalUtils.HIGH_PRECISION);
                if (lremise.compareTo(BigDecimal.ZERO) > 0 && lremise.compareTo(BigDecimal.valueOf(100)) < 100) {
                    r = r.multiply(BigDecimal.valueOf(100).subtract(lremise), DecimalUtils.HIGH_PRECISION).movePointLeft(2);
                }

                if (row.getTable().getFieldsName().contains("MONTANT_REMISE")) {
                    final BigDecimal acomptePercent = row.getBigDecimal("POURCENT_REMISE");
                    final BigDecimal acompteMontant = row.getBigDecimal("MONTANT_REMISE");
                    Remise remise = new Remise(acomptePercent, acompteMontant);
                    r = remise.getResultFrom(r);
                }

                if (row.getTable().getFieldsName().contains("POURCENT_FACTURABLE")) {
                    final BigDecimal acomptePercent = row.getBigDecimal("POURCENT_FACTURABLE");
                    final BigDecimal acompteMontant = row.getBigDecimal("MONTANT_FACTURABLE");
                    Acompte acompte = new Acompte(acomptePercent, acompteMontant);
                    r = acompte.getResultFrom(r);
                }

                return r.setScale(totalHT.getDecimalDigits(), BigDecimal.ROUND_HALF_UP);
            }
        });
        this.totalHA.setModifier(new CellDynamicModifier() {
            @Override
            public Object computeValueFrom(SQLRowValues row, SQLTableElement source) {
                int qte = Integer.parseInt(row.getObject("QTE").toString());
                BigDecimal b = (row.getObject("QTE_UNITAIRE") == null) ? BigDecimal.ONE : (BigDecimal) row.getObject("QTE_UNITAIRE");
                BigDecimal f = (BigDecimal) row.getObject("PA_HT");
                BigDecimal rHA = b.multiply(new BigDecimal(qte), DecimalUtils.HIGH_PRECISION).multiply(f, DecimalUtils.HIGH_PRECISION).setScale(6, BigDecimal.ROUND_HALF_UP);
                if (row.getTable().getFieldsName().contains("POURCENT_FACTURABLE")) {
                    final BigDecimal acomptePercent = row.getBigDecimal("POURCENT_FACTURABLE");
                    final BigDecimal acompteMontant = row.getBigDecimal("MONTANT_FACTURABLE");
                    if (acomptePercent != null || acompteMontant != null) {
                        if (acomptePercent != null) {
                            rHA = rHA.multiply(acomptePercent.movePointLeft(2), DecimalUtils.HIGH_PRECISION);
                        } else {
                            // Calcul du T_HT vente origin
                            BigDecimal lremise = BigDecimal.ZERO;

                            if (row.getTable().getFieldsName().contains("POURCENT_RG")) {
                                final Object o3 = row.getObject("POURCENT_RG");
                                if (o3 != null) {
                                    lremise = lremise.add(((BigDecimal) o3));
                                }
                            }

                            BigDecimal fVT = (BigDecimal) row.getObject("PV_HT");
                            BigDecimal r = b.multiply(fVT.multiply(BigDecimal.valueOf(qte), DecimalUtils.HIGH_PRECISION), DecimalUtils.HIGH_PRECISION);
                            if (lremise.compareTo(BigDecimal.ZERO) > 0 && lremise.compareTo(BigDecimal.valueOf(100)) < 100) {
                                r = r.multiply(BigDecimal.valueOf(100).subtract(lremise), DecimalUtils.HIGH_PRECISION).movePointLeft(2);
                            }

                            if (row.getTable().getFieldsName().contains("MONTANT_REMISE")) {
                                final BigDecimal acomptePercentR = row.getBigDecimal("POURCENT_REMISE");
                                final BigDecimal acompteMontantR = row.getBigDecimal("MONTANT_REMISE");
                                Remise remise = new Remise(acomptePercentR, acompteMontantR);
                                r = remise.getResultFrom(r);
                            }
                            if (r.signum() != 0) {
                                rHA = rHA.multiply(acompteMontant.divide(r, DecimalUtils.HIGH_PRECISION), DecimalUtils.HIGH_PRECISION);
                            }
                        }
                    }

                }
                return rHA.setScale(totalHA.getDecimalDigits(), BigDecimal.ROUND_HALF_UP);
            }

            @Override
            public void setValueFrom(SQLRowValues row, Object value) {
                super.setValueFrom(row, value);
            }
        });

        if (DefaultNXProps.getInstance().getBooleanValue(ARTICLE_SHOW_DEVISE, false)) {
            this.qte.addModificationListener(tableElementTotalDevise);
            qteU.addModificationListener(tableElementTotalDevise);
            if (eltUnitDevise != null) {
                eltUnitDevise.addModificationListener(tableElementTotalDevise);
            }
            tableElementRemise.addModificationListener(this.tableElementTotalDevise);
            tableElementTotalDevise.setModifier(new CellDynamicModifier() {
                @Override
                public Object computeValueFrom(SQLRowValues row, SQLTableElement source) {
                    int qte = row.getInt("QTE");
                    BigDecimal prixDeVenteUnitaireDevise = (row.getObject("PV_U_DEVISE") == null) ? BigDecimal.ZERO : (BigDecimal) row.getObject("PV_U_DEVISE");
                    BigDecimal qUnitaire = (row.getObject("QTE_UNITAIRE") == null) ? BigDecimal.ONE : (BigDecimal) row.getObject("QTE_UNITAIRE");
                    // r = prixUnitaire x qUnitaire x qte
                    BigDecimal prixVente = qUnitaire.multiply(prixDeVenteUnitaireDevise.multiply(BigDecimal.valueOf(qte), DecimalUtils.HIGH_PRECISION), DecimalUtils.HIGH_PRECISION);

                    if (row.getTable().getFieldsName().contains("MONTANT_REMISE")) {
                        final BigDecimal acomptePercent = row.getBigDecimal("POURCENT_REMISE");
                        final BigDecimal acompteMontant = row.getBigDecimal("MONTANT_REMISE");
                        Remise remise = new Remise(acomptePercent, acompteMontant);
                        prixVente = remise.getResultFrom(prixVente);
                    }

                    // if (lremise.compareTo(BigDecimal.ZERO) > 0 &&
                    // lremise.compareTo(BigDecimal.valueOf(100)) < 100) {
                    // r = r.multiply(BigDecimal.valueOf(100).subtract(lremise),
                    // DecimalUtils.HIGH_PRECISION).movePointLeft(2);
                    // }
                    return prixVente.setScale(tableElementTotalDevise.getDecimalDigits(), BigDecimal.ROUND_HALF_UP);
                }
            });
        }
        // Calcul automatique du total TTC

        this.totalHT.addModificationListener(this.tableElementTotalTTC);
        this.tableElementTVA.addModificationListener(this.tableElementTotalTTC);
        this.tableElementTotalTTC.setModifier(new CellDynamicModifier() {
            @Override
            public Object computeValueFrom(SQLRowValues row, SQLTableElement source) {

                BigDecimal ht = (BigDecimal) row.getObject("T_PV_HT");
                int idTaux = row.getForeignID("ID_TAXE");

                Float resultTaux = TaxeCache.getCache().getTauxFromId(idTaux);

                if (resultTaux == null) {
                    SQLRow rowTax = TaxeCache.getCache().getFirstTaxe();
                    row.put("ID_TAXE", rowTax.getID());
                    resultTaux = rowTax.getFloat("TAUX");
                }

                float taux = (resultTaux == null) ? 0.0F : resultTaux.floatValue();
                editorPVHT.setTaxe(taux);
                editorPVHT.setMin(null);
                if (!row.isForeignEmpty("ID_ARTICLE") && getSQLElement().getTable().getDBRoot().contains("ARTICLE_PRIX_MIN_VENTE") && !lockVTMinPrice) {
                    List<SQLRow> minPrices = row.getForeign("ID_ARTICLE").asRow().getReferentRows(row.getTable().getTable("ARTICLE_PRIX_MIN_VENTE"));
                    if (minPrices.size() > 0) {
                        editorPVHT.setMin(minPrices.get(0).getBigDecimal("PRIX"));
                    }
                }

                BigDecimal r = ht.multiply(BigDecimal.valueOf(taux).movePointLeft(2).add(BigDecimal.ONE), DecimalUtils.HIGH_PRECISION);
                final BigDecimal resultTTC = r.setScale(tableElementTotalTTC.getDecimalDigits(), BigDecimal.ROUND_HALF_UP);

                return resultTTC;
            }

            @Override
            public void setValueFrom(SQLRowValues row, Object value) {
                super.setValueFrom(row, value);
            }

        });

        // Calcul automatique du poids unitaire
        tableElement_ValeurMetrique1.addModificationListener(tableElementPoids);
        tableElement_ValeurMetrique2.addModificationListener(tableElementPoids);
        tableElement_ValeurMetrique3.addModificationListener(tableElementPoids);
        tableElementPoids.setModifier(new CellDynamicModifier() {
            public Object computeValueFrom(SQLRowValues row, SQLTableElement source) {
                return new Float(ReferenceArticleSQLElement.getPoidsFromDetails(row));
            }

        });
        // Calcul automatique du poids total
        tableElementPoids.addModificationListener(this.tableElementPoidsTotal);
        qteU.addModificationListener(tableElementPoidsTotal);
        this.qte.addModificationListener(this.tableElementPoidsTotal);
        this.tableElementPoidsTotal.setModifier(new CellDynamicModifier() {
            public Object computeValueFrom(SQLRowValues row, SQLTableElement source) {
                Number f = (Number) row.getObject("POIDS");
                if (f == null) {
                    f = 0;
                }
                int qte = Integer.parseInt(row.getObject("QTE").toString());
                BigDecimal b = (row.getObject("QTE_UNITAIRE") == null) ? BigDecimal.ONE : (BigDecimal) row.getObject("QTE_UNITAIRE");
                // FIXME convertir en float autrement pour éviter une valeur non valeur transposable
                // avec floatValue ou passer POIDS en bigDecimal
                return b.multiply(new BigDecimal(f.floatValue() * qte)).floatValue();
            }

        });
        uniteVente.addModificationListener(qteU);
        qteU.setModifier(new CellDynamicModifier() {
            public Object computeValueFrom(SQLRowValues row, SQLTableElement source) {
                SQLRowAccessor rowUnite = row.getForeign("ID_UNITE_VENTE");
                if (rowUnite != null && !rowUnite.isUndefined() && rowUnite.getBoolean("A_LA_PIECE")) {
                    return BigDecimal.ONE;
                } else {
                    return row.getObject("QTE_UNITAIRE");
                }
            }

        });

        tableElement_PrixMetrique1_VenteHT.setModifier(new CellDynamicModifier() {
            public Object computeValueFrom(SQLRowValues row, SQLTableElement source) {
                if (source != null && source.getField().getName().equals("PRIX_METRIQUE_VT_1")) {
                    return row.getObject("PRIX_METRIQUE_VT_1");
                } else {
                    if (source != null && source.getField().getName().equals("PV_U_DEVISE")) {
                        if (!row.isForeignEmpty("ID_DEVISE")) {
                            String devCode = row.getForeign("ID_DEVISE").getString("CODE");
                            BigDecimal prixDeVenteUnitaireDevise = (row.getObject("PV_U_DEVISE") == null) ? BigDecimal.ZERO : (BigDecimal) row.getObject("PV_U_DEVISE");

                            CurrencyConverter c = new CurrencyConverter();
                            BigDecimal result = c.convert(prixDeVenteUnitaireDevise, devCode, c.getCompanyCurrencyCode(), getDateDevise(), isUsedBiasedDevise());
                            if (result == null) {
                                result = prixDeVenteUnitaireDevise;
                            }
                            return result.setScale(row.getTable().getField("PRIX_METRIQUE_VT_1").getType().getDecimalDigits(), RoundingMode.HALF_UP);
                        } else {
                            return row.getObject("PRIX_METRIQUE_VT_1");
                        }
                    }
                    return tarifCompletion(row.getForeign("ID_ARTICLE"), "PRIX_METRIQUE_VT_1", row);
                }
            }

        });

        if (DefaultNXProps.getInstance().getBooleanValue(ARTICLE_SHOW_DEVISE, false)) {
            if (eltUnitDevise != null) {
                eltUnitDevise.addModificationListener(tableElement_PrixMetrique1_VenteHT);
            }

            if (eltUnitDevise != null) {
                tableElement_PrixMetrique1_VenteHT.addModificationListener(eltUnitDevise);
                eltDevise.addModificationListener(eltUnitDevise);
                eltUnitDevise.setModifier(new CellDynamicModifier() {
                    public Object computeValueFrom(SQLRowValues row, SQLTableElement source) {
                        if (source != null && source.getField().getName().equals("PV_U_DEVISE")) {
                            BigDecimal prixDeVenteUnitaireDevise = (row.getObject("PV_U_DEVISE") == null) ? BigDecimal.ZERO : (BigDecimal) row.getObject("PV_U_DEVISE");
                            return prixDeVenteUnitaireDevise;
                        } else {
                            if (!row.isForeignEmpty("ID_DEVISE")) {
                                String devCode = row.getForeign("ID_DEVISE").getString("CODE");
                                BigDecimal bigDecimal = (BigDecimal) row.getObject("PRIX_METRIQUE_VT_1");

                                CurrencyConverter c = new CurrencyConverter();
                                BigDecimal result = c.convert(bigDecimal, c.getCompanyCurrencyCode(), devCode, getDateDevise(), isUsedBiasedDevise());
                                if (result == null) {
                                    result = bigDecimal;
                                }
                                return result.setScale(row.getTable().getField("PRIX_METRIQUE_VT_1").getType().getDecimalDigits(), RoundingMode.HALF_UP);
                            } else if (source != null && source.getField().getName().equalsIgnoreCase("PRIX_METRIQUE_VT_1")) {
                                return row.getObject("PRIX_METRIQUE_VT_1");
                            }
                            BigDecimal prixDeVenteUnitaireDevise = (row.getObject("PV_U_DEVISE") == null) ? BigDecimal.ZERO : (BigDecimal) row.getObject("PV_U_DEVISE");
                            return prixDeVenteUnitaireDevise;
                        }
                    }

                });
            }
        }

        // Calcul automatique du prix de vente unitaire HT
        tableElement_ValeurMetrique1.addModificationListener(tableElement_PrixVente_HT);
        tableElement_ValeurMetrique2.addModificationListener(tableElement_PrixVente_HT);
        tableElement_ValeurMetrique3.addModificationListener(tableElement_PrixVente_HT);
        tableElement_PrixMetrique1_VenteHT.addModificationListener(tableElement_PrixVente_HT);
        tableElement_PrixVente_HT.setModifier(new CellDynamicModifier() {
            public Object computeValueFrom(SQLRowValues row, SQLTableElement source) {
                if (row.isForeignEmpty("ID_MODE_VENTE_ARTICLE") || row.getInt("ID_MODE_VENTE_ARTICLE") == ReferenceArticleSQLElement.A_LA_PIECE) {
                    return row.getObject("PRIX_METRIQUE_VT_1");
                } else {

                    final BigDecimal prixVTFromDetails = ReferenceArticleSQLElement.getPrixVTFromDetails(row);
                    return prixVTFromDetails.setScale(tableElement_PrixVente_HT.getDecimalDigits(), RoundingMode.HALF_UP);
                }
            }
        });

        // Calcul automatique du prix d'achat unitaire HT
        tableElement_ValeurMetrique1.addModificationListener(this.ha);
        tableElement_ValeurMetrique2.addModificationListener(this.ha);
        tableElement_ValeurMetrique3.addModificationListener(this.ha);
        tableElement_PrixMetrique1_AchatHT.addModificationListener(this.ha);
        this.ha.setModifier(new CellDynamicModifier() {
            public Object computeValueFrom(SQLRowValues row, SQLTableElement source) {
                if (row.isForeignEmpty("ID_MODE_VENTE_ARTICLE") || row.getInt("ID_MODE_VENTE_ARTICLE") == ReferenceArticleSQLElement.A_LA_PIECE) {
                    return row.getObject("PRIX_METRIQUE_HA_1");
                } else {

                    final BigDecimal prixHAFromDetails = ReferenceArticleSQLElement.getPrixHAFromDetails(row);
                    return prixHAFromDetails.setScale(ha.getDecimalDigits(), RoundingMode.HALF_UP);
                }
            }

        });

        this.table.readState();

        setColumnVisible(model.getColumnForField("T_PA_HT"), true);

        // Packaging
        if (prefs.getBoolean(GestionArticleGlobalPreferencePanel.ITEM_PACKAGING, false)) {
            setColumnVisible(model.getColumnForField("T_POIDS_COLIS_NET"), false);
        }

        // Mode Gestion article avancé
        final boolean modeAvance = DefaultNXProps.getInstance().getBooleanValue("ArticleModeVenteAvance", false);
        setColumnVisible(model.getColumnForField("VALEUR_METRIQUE_1"), modeAvance);
        setColumnVisible(model.getColumnForField("VALEUR_METRIQUE_2"), modeAvance);
        setColumnVisible(model.getColumnForField("VALEUR_METRIQUE_3"), modeAvance);
        setColumnVisible(model.getColumnForField("PV_HT"), modeAvance);
        setColumnVisible(model.getColumnForField("PA_HT"), modeAvance);
        setColumnVisible(model.getColumnForField("ID_MODE_VENTE_ARTICLE"), modeAvance);

        if (this.tableElementEco != null && this.tableElementEcoTotal != null && this.tableElementEcoID != null) {
            setColumnVisible(model.getColumnForField("ID_ECO_CONTRIBUTION"), showEco);
            setColumnVisible(model.getColumnForField("ECO_CONTRIBUTION"), showEco);
            setColumnVisible(model.getColumnForField("T_ECO_CONTRIBUTION"), showEco);
        }

        // Gestion des unités de vente
        final boolean gestionUV = prefs.getBoolean(GestionArticleGlobalPreferencePanel.UNITE_VENTE, true);
        setColumnVisible(model.getColumnForField("QTE_UNITAIRE"), gestionUV);
        setColumnVisible(model.getColumnForField("ID_UNITE_VENTE"), gestionUV);

        setColumnVisible(model.getColumnForField("ID_ARTICLE"), selectArticle);
        setColumnVisible(model.getColumnForField("CODE"), !selectArticle || (selectArticle && createAuto));
        setColumnVisible(model.getColumnForField("NOM"), !selectArticle || (selectArticle && createAuto));

        // Voir le poids
        final boolean showPoids = DefaultNXProps.getInstance().getBooleanValue("ArticleShowPoids", false);
        setColumnVisible(model.getColumnForField("POIDS"), showPoids);
        setColumnVisible(model.getColumnForField("T_POIDS"), showPoids);

        // Voir le style
        setColumnVisible(model.getColumnForField("ID_STYLE"), DefaultNXProps.getInstance().getBooleanValue("ArticleShowStyle", true));
        setColumnVisible(model.getColumnForField("POURCENT_FACTURABLE"), false);

        setColumnVisible(getModel().getColumnForField("ID_FAMILLE_ARTICLE"), filterFamilleArticle);

        setColumnVisible(model.getColumnForField("PRIX_METRIQUE_HA_1"), showHAPrice);
        setColumnVisible(model.getColumnForField("T_PA_HT"), showHAPrice);


        for (String string : visibilityMap.keySet()) {
            setColumnVisible(model.getColumnForField(string), visibilityMap.get(string));
        }

        Map<String, Boolean> mapCustom = getCustomVisibilityMap();
        if (mapCustom != null) {
            for (String string : mapCustom.keySet()) {
                setColumnVisible(model.getColumnForField(string), mapCustom.get(string));
            }
        }

        // Barcode reader
        final BarcodeReader barcodeReader = ComptaPropsConfiguration.getInstanceCompta().getBarcodeReader();
        if (barcodeReader != null) {

            final BarcodeListener l = new BarcodeListener() {

                @Override
                public void keyReceived(KeyEvent ee) {
                    // TODO Auto-generated method stub
                }

                @Override
                public void barcodeRead(String code) {
                    if (((JFrame) SwingUtilities.getRoot(getRowValuesTable())).isActive()) {
                        final SQLSelect selArticle = new SQLSelect();
                        final SQLTable tableArticle = getSQLElement().getForeignElement("ID_ARTICLE").getTable();
                        selArticle.addSelectStar(tableArticle);
                        Where w = new Where(tableArticle.getField("OBSOLETE"), "=", Boolean.FALSE);
                        w = w.and(new Where(tableArticle.getField("CODE_BARRE"), "=", code));
                        selArticle.setWhere(w);
                        List<SQLRow> l2 = SQLRowListRSH.execute(selArticle);
                        if (l2.size() > 0) {
                            System.err.println("ARTICLE " + l2.get(0).getString("NOM"));
                            Tuple3<Double, String, String> art = Tuple3.create(1.0D, l2.get(0).getString("CODE"), l2.get(0).getString("NOM"));
                            List<Tuple3<Double, String, String>> l = new ArrayList<Tuple3<Double, String, String>>();
                            l.add(art);
                            insertFromDrop(l, m);
                        } else {
                            System.err.println("ARTICLE NOT FOUND !");
                        }
                    }

                }
            };
            getRowValuesTable().addHierarchyListener(new HierarchyListener() {
                public void hierarchyChanged(HierarchyEvent e) {
                    if ((e.getChangeFlags() & HierarchyEvent.DISPLAYABILITY_CHANGED) != 0)
                        if (getRowValuesTable().isDisplayable()) {
                            barcodeReader.addBarcodeListener(l);
                        } else {
                            barcodeReader.removeBarcodeListener(l);
                        }
                }
            });

        }

        // On réécrit la configuration au cas ou les preferences aurait changé (ajout ou suppression
        // du mode de vente specifique)
        this.table.writeState();
    }

    @Override
    protected void refreshDeviseAmount() {
        int count = getRowValuesTable().getRowCount();
        final int columnForField = getRowValuesTable().getRowValuesTableModel().getColumnForField("PV_U_DEVISE");
        if (columnForField >= 0) {
            SQLTableElement eltDevise = getRowValuesTable().getRowValuesTableModel().getSQLTableElementAt(columnForField);
            for (int i = 0; i < count; i++) {
                SQLRowValues rowVals = getRowValuesTable().getRowValuesTableModel().getRowValuesAt(i);
                // getRowValuesTable().getRowValuesTableModel().putValue(rowVals.getObject("PV_U_DEVISE"),
                // i, "PV_U_DEVISE", true);
                BigDecimal prixDeVenteUnitaireDevise = (rowVals.getObject("PV_U_DEVISE") == null) ? BigDecimal.ZERO : (BigDecimal) rowVals.getObject("PV_U_DEVISE");
                eltDevise.setValueFrom(rowVals, prixDeVenteUnitaireDevise);
                getRowValuesTable().getRowValuesTableModel().fireTableChanged(new TableModelEvent(getRowValuesTable().getRowValuesTableModel(), i, i, columnForField));
            }
        }
    }

    protected Map<String, Boolean> getCustomVisibilityMap() {
        return null;
    }

    protected Object tarifCompletion(SQLRowAccessor row, String field, SQLRowAccessor rowDest) {
        return tarifCompletion(row, field, rowDest, false);
    }

    protected Object tarifCompletion(SQLRowAccessor row, String field, SQLRowAccessor rowDest, boolean fromCompletion) {

        if (row != null && !row.isUndefined() && getSQLElement().getTable().getDBRoot().contains("ARTICLE_PRIX_REVIENT")
                && (field.equalsIgnoreCase("PRIX_METRIQUE_HA_1") || field.equalsIgnoreCase("PA_HT"))) {

            final BigDecimal prc;
            if (row.getBoolean("AUTO_PRIX_REVIENT_NOMENCLATURE")) {
                ProductHelper helper = new ProductHelper(row.getTable().getDBRoot());
                prc = helper.getBomPriceForQuantity(1, row.getReferentRows(row.getTable().getTable("ARTICLE_ELEMENT").getField("ID_ARTICLE_PARENT")), TypePrice.ARTICLE_PRIX_REVIENT);
            } else {
                ProductComponent productComp = new ProductComponent(row, BigDecimal.ONE);
                prc = productComp.getPRC(new Date());
            }
            if (prc == null) {
                return BigDecimal.ZERO;
            }
            return prc;
        }

        if (getTarif() != null && !getTarif().isUndefined()) {
            Collection<? extends SQLRowAccessor> rows = row.getReferentRows(tableArticleTarif);

            // Récupération du tarif associé à la quantité
            int quantite = 0;
            BigDecimal b = rowDest.getBigDecimal("QTE_UNITAIRE");
            int q = rowDest.getInt("QTE");
            BigDecimal qteTotal = b.multiply(new BigDecimal(q), DecimalUtils.HIGH_PRECISION);
            SQLRowAccessor rowTarif = null;

            for (SQLRowAccessor sqlRowAccessor : rows) {

                if (!sqlRowAccessor.getTable().contains("OBSOLETE") || !sqlRowAccessor.getBoolean("OBSOLETE")) {
                    // FIXME BigDecimal??
                    // BigDecimal bigDecimal = sqlRowAccessor.getBigDecimal("QTE");
                    int qteTarif = sqlRowAccessor.getInt("QTE");
                    if (sqlRowAccessor.getForeignID("ID_TARIF") == getTarif().getID() && CompareUtils.compare(qteTarif, qteTotal) <= 0 && CompareUtils.compare(qteTarif, quantite) > 0) {
                        quantite = qteTarif;
                        rowTarif = sqlRowAccessor;
                        // else {
                        // result = null;
                        // remise = sqlRowAccessor.getBigDecimal("POURCENT_REMISE");
                        // }
                    }
                }
            }

            if (rowTarif == null) {
                if (!getTarif().isForeignEmpty("ID_DEVISE")) {
                    if ((field.equalsIgnoreCase("ID_DEVISE"))) {
                        return getTarif().getObject("ID_DEVISE");
                    } else if ((field.equalsIgnoreCase("PV_U_DEVISE"))) {

                        return getQtyTarifPvM1(rowDest, fromCompletion);
                    }
                }
                if ((field.equalsIgnoreCase("ID_TAXE"))) {

                    if (!getTarif().isForeignEmpty("ID_TAXE")) {
                        return getTarif().getForeignID("ID_TAXE");
                    }
                }

            } else {
                if (field.equalsIgnoreCase("PRIX_METRIQUE_VT_1")) {
                    if (rowTarif.isForeignEmpty("ID_DEVISE"))
                        return rowTarif.getObject(field);
                    else {

                        String devCode = getTarif().getForeign("ID_DEVISE").getString("CODE");
                        CurrencyConverter c = new CurrencyConverter();
                        BigDecimal result = c.convert(rowTarif.getBigDecimal(field), devCode, c.getCompanyCurrencyCode(), new Date(), true);
                        return result.setScale(row.getTable().getField(field).getType().getDecimalDigits(), RoundingMode.HALF_UP);
                    }

                } else if ((field.equalsIgnoreCase("ID_DEVISE"))) {

                    return rowTarif.getObject("ID_DEVISE");
                }

                else if ((field.equalsIgnoreCase("PV_U_DEVISE"))) {

                    return rowTarif.getObject("PRIX_METRIQUE_VT_1");

                } else if ((field.equalsIgnoreCase("ID_TAXE"))) {

                    if (!rowTarif.isForeignEmpty("ID_TAXE")) {
                        return rowTarif.getObject("ID_TAXE");

                    }
                } else if ((field.equalsIgnoreCase("POURCENT_REMISE"))) {
                    Acompte remise = new Acompte(rowTarif.getBigDecimal("POURCENT_REMISE"), BigDecimal.ZERO);
                    return remise;
                }
            }
        }

        if ((field.equalsIgnoreCase("POURCENT_REMISE"))) {
            return new Acompte(BigDecimal.ZERO, BigDecimal.ZERO);
        }

        if ((field.equalsIgnoreCase("PRIX_METRIQUE_VT_1"))) {
            return getQtyTarifPvM1(rowDest, fromCompletion);
        } else {
            return null;
        }
    }

    @Override
    public void setTarif(SQLRowAccessor rowValuesTarif, boolean ask) {
        if (rowValuesTarif == null || getTarif() == null || rowValuesTarif.getID() != getTarif().getID()) {
            super.setTarif(rowValuesTarif, ask);
            if (ask && getRowValuesTable().getRowCount() > 0
                    && JOptionPane.showConfirmDialog(null, "Appliquer les tarifs associés au client sur les lignes déjà présentes?") == JOptionPane.YES_OPTION) {
                int nbRows = this.table.getRowCount();
                for (int i = 0; i < nbRows; i++) {
                    SQLRowValues rowVals = getRowValuesTable().getRowValuesTableModel().getRowValuesAt(i);

                    if (!rowVals.isForeignEmpty("ID_ARTICLE")) {
                        SQLRowAccessor rowValsArt = rowVals.getForeign("ID_ARTICLE");
                        final Object taxeValue = tarifCompletion(rowValsArt, "ID_TAXE", rowVals);
                        if (taxeValue != null) {
                            getRowValuesTable().getRowValuesTableModel().putValue(taxeValue, i, "ID_TAXE");
                        }

                        final Object deviseValue = tarifCompletion(rowValsArt, "ID_DEVISE", rowVals);
                        if (deviseValue != null) {
                            getRowValuesTable().getRowValuesTableModel().putValue(deviseValue, i, "ID_DEVISE");
                        }
                        getRowValuesTable().getRowValuesTableModel().putValue(tarifCompletion(rowValsArt, "PV_U_DEVISE", rowVals), i, "PV_U_DEVISE");
                        getRowValuesTable().getRowValuesTableModel().putValue(tarifCompletion(rowValsArt, "PRIX_METRIQUE_VT_1", rowVals), i, "PRIX_METRIQUE_VT_1");
                    }
                }
            }
        }
    }

    protected Object getQtyTarifPvM1(SQLRowAccessor row, boolean fromCompletion) {

        // Test si un tarif spécial est associé à l'article
        // Object o = tarifCompletion(row.getForeign("ID_ARTICLE"), "PRIX_METRIQUE_VT_1", row,
        // false);
        // if (o != null) {
        // return o;
        // }

        SQLRowAccessor rowA = row.getForeign("ID_ARTICLE");
        if (rowA != null && !rowA.isUndefined() && rowA.getTable().contains("AUTO_PRIX_MIN_VENTE_NOMENCLATURE") && rowA.getBoolean("AUTO_PRIX_MIN_VENTE_NOMENCLATURE")) {
            BigDecimal b = row.getBigDecimal("QTE_UNITAIRE");
            int q = row.getInt("QTE");
            BigDecimal qteTotal = b.multiply(new BigDecimal(q), DecimalUtils.HIGH_PRECISION);
            ProductHelper helper = new ProductHelper(rowA.getTable().getDBRoot());

            return helper.getBomPriceForQuantity(qteTotal.setScale(0, RoundingMode.HALF_UP).intValue(), rowA.getReferentRows(rowA.getTable().getTable("ARTICLE_ELEMENT").getField("ID_ARTICLE_PARENT")),
                    TypePrice.ARTICLE_PRIX_PUBLIC);
        }
        BigDecimal result = null;
        // BigDecimal remise = null;
        if (rowA != null && !rowA.isUndefined() && rowA.getTable().getDBRoot().contains("ARTICLE_PRIX_PUBLIC")) {
            Collection<? extends SQLRowAccessor> col = rowA.getReferentRows(rowA.getTable().getTable("ARTICLE_PRIX_PUBLIC"));
            int quantite = 0;
            BigDecimal b = row.getBigDecimal("QTE_UNITAIRE");
            int q = row.getInt("QTE");
            BigDecimal qteTotal = b.multiply(new BigDecimal(q), DecimalUtils.HIGH_PRECISION);

            for (SQLRowAccessor sqlRowAccessor : col) {

                // FIXME BigDecimal??
                // BigDecimal bigDecimal = sqlRowAccessor.getBigDecimal("QTE");
                int qtePublic = sqlRowAccessor.getInt("QTE");
                if (CompareUtils.compare(qtePublic, qteTotal) <= 0 && CompareUtils.compare(qtePublic, quantite) > 0) {
                    quantite = qtePublic;
                    if (sqlRowAccessor.getBigDecimal("PRIX") != null) {
                        result = sqlRowAccessor.getBigDecimal("PRIX");
                        // remise = null;
                    }
                    // else {
                    // result = null;
                    // remise = sqlRowAccessor.getBigDecimal("POURCENT_REMISE");
                    // }
                }
            }
        }
        if (result == null && rowA != null && !rowA.isUndefined() && rowA.getTable().getDBRoot().contains("ARTICLE_PRIX_MIN_VENTE")) {
            Collection<? extends SQLRowAccessor> col = rowA.getReferentRows(rowA.getTable().getTable("ARTICLE_PRIX_MIN_VENTE"));
            int quantite = 0;
            BigDecimal b = row.getBigDecimal("QTE_UNITAIRE");
            int q = row.getInt("QTE");
            BigDecimal qteTotal = b.multiply(new BigDecimal(q), DecimalUtils.HIGH_PRECISION);

            for (SQLRowAccessor sqlRowAccessor : col) {

                int qteMinVente = sqlRowAccessor.getInt("QTE");
                if (CompareUtils.compare(qteMinVente, qteTotal) <= 0 && CompareUtils.compare(qteMinVente, quantite) > 0) {
                    quantite = qteMinVente;
                    if (sqlRowAccessor.getBigDecimal("PRIX") != null) {
                        result = sqlRowAccessor.getBigDecimal("PRIX");
                    }
                }
            }
        }

        BigDecimal remise = null;

        if (rowA != null && !rowA.isUndefined() && rowA.getTable().getDBRoot().contains("TARIF_QUANTITE")) {
            Collection<? extends SQLRowAccessor> col = rowA.getReferentRows(rowA.getTable().getTable("TARIF_QUANTITE"));
            BigDecimal quantite = BigDecimal.ZERO;
            BigDecimal b = row.getBigDecimal("QTE_UNITAIRE");
            int q = row.getInt("QTE");
            BigDecimal qteTotal = b.multiply(new BigDecimal(q), DecimalUtils.HIGH_PRECISION);

            for (SQLRowAccessor sqlRowAccessor : col) {

                BigDecimal bigDecimal = sqlRowAccessor.getBigDecimal("QUANTITE");
                if (CompareUtils.compare(bigDecimal, qteTotal) <= 0 && CompareUtils.compare(bigDecimal, quantite) > 0) {
                    quantite = bigDecimal;
                    if (sqlRowAccessor.getBigDecimal("PRIX_METRIQUE_VT_1") != null) {
                        result = sqlRowAccessor.getBigDecimal("PRIX_METRIQUE_VT_1");
                        remise = null;
                    } else {
                        result = null;
                        remise = sqlRowAccessor.getBigDecimal("POURCENT_REMISE");
                    }
                }
            }
            if (!col.isEmpty() && result == null && remise == null) {
                result = rowA.getBigDecimal("PRIX_METRIQUE_VT_1");
            }
        }
        int index = getRowValuesTable().getRowValuesTableModel().row2index(row);
        if (result == null && remise == null) {
            // getRowValuesTable().getRowValuesTableModel().putValue(BigDecimal.ZERO, index,
            // "POURCENT_REMISE");
            return (fromCompletion ? null : row.getObject("PRIX_METRIQUE_VT_1"));
        } else {
            if (result != null) {
                // getRowValuesTable().getRowValuesTableModel().putValue(BigDecimal.ZERO, index,
                // "POURCENT_REMISE");
                return result;
            } else {
                getRowValuesTable().getRowValuesTableModel().putValue(remise, index, "POURCENT_REMISE");
                return row.getBigDecimal("PRIX_METRIQUE_VT_1");
            }
        }

    }

    @Override
    protected void setModel(RowValuesTableModel model) {
        super.setModel(model);
        model.addTableModelListener(new TableModelListener() {

            @Override
            public void tableChanged(TableModelEvent e) {

                calculTarifNomenclature();

            }
        });
    }

    private void dropInTable(DropTargetDropEvent dtde, final AutoCompletionManager autoM) {
        dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
        Transferable t = dtde.getTransferable();
        try {

            List<Tuple3<Double, String, String>> articles = new ArrayList<Tuple3<Double, String, String>>();
            if (t.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                List<File> fileList = (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);
                final DataImporter importer = new DataImporter(getSQLElement().getTable());
                final File file = fileList.get(0);
                if (file.getName().endsWith(".ods") || file.getName().endsWith(".xls")) {
                    ArrayTableModel m = importer.createModelFrom(file);
                    for (int i = 0; i < m.getRowCount(); i++) {
                        List<Object> l = m.getLineValuesAt(i);
                        if (l.size() > 1) {
                            if (l.get(0) == null || l.get(0).toString().length() == 0) {
                                break;
                            }
                            Double qte = ((Number) l.get(1)).doubleValue();
                            String code = "";
                            if (l.get(0) instanceof Number) {
                                code = String.valueOf(((Number) l.get(0)).intValue());
                            } else {
                                code = l.get(0).toString();
                            }
                            String nom = "";
                            if (l.size() > 2) {
                                nom = (String) l.get(2);
                            }
                            if (qte > 0) {
                                articles.add(Tuple3.create(qte, code, nom));
                            }
                        }
                    }
                } else {
                    JOptionPane.showMessageDialog(null, "Les formats de fichiers pris en charge sont ods et xls!");
                }
            } else if (t.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                final String transferData = (String) t.getTransferData(DataFlavor.stringFlavor);
                List<String> l = StringUtils.fastSplitTrimmed(transferData, '\n');
                for (String string : l) {
                    List<String> line = StringUtils.fastSplitTrimmed(string, '\t');
                    if (line.size() >= 2) {
                        Double qte = Double.valueOf(line.get(1));
                        String code = (line.get(0) == null ? "" : line.get(0).toString());
                        String nom = "";
                        if (line.size() > 2) {
                            nom = (String) line.get(2);
                        }
                        if (qte > 0) {
                            articles.add(Tuple3.create(qte, code, nom));
                        }
                    } else {
                        break;
                    }
                }
            }
            if (articles.size() > 0) {
                insertFromDrop(articles, autoM);
                for (Tuple3<Double, String, String> tuple3 : articles) {
                    System.err.println("ADD LINE " + tuple3);
                }
            }
        } catch (UnsupportedFlavorException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
        // Transferable t = dtde.getTransferable();
        // List fileList = (List)t.getTransferData(DataFlavor.javaFileListFlavor);
        // File f = (File)fileList.get(0);
        // table.setValueAt(f.getAbsolutePath(), row, column);
        // table.setValueAt(f.length(), row, column+1);
    }

    protected void insertFromDrop(List<Tuple3<Double, String, String>> articles, AutoCompletionManager m) {

        List<String> code = new ArrayList<String>(articles.size());
        for (int i = articles.size() - 1; i >= 0; i--) {

            Tuple3<Double, String, String> tuple = articles.get(i);
            code.add(tuple.get1());
        }
        SQLSelect sel = new SQLSelect();
        final SQLTable articleTable = getSQLElement().getTable().getForeignTable("ID_ARTICLE");
        sel.addSelectStar(articleTable);
        sel.setWhere(new Where(articleTable.getField("CODE"), code));
        List<SQLRow> matchCode = SQLRowListRSH.execute(sel);
        Map<String, SQLRow> mapCode = new HashMap<String, SQLRow>();
        for (SQLRow sqlRow : matchCode) {
            mapCode.put(sqlRow.getString("CODE"), sqlRow);
        }

        int rowCount = getRowValuesTable().getRowValuesTableModel().getRowCount();
        Map<Integer, Integer> mapRows = new HashMap<Integer, Integer>();
        for (int i = 0; i < rowCount; i++) {
            SQLRowValues rowVals = getRowValuesTable().getRowValuesTableModel().getRowValuesAt(i);
            if (rowVals.getObject("ID_ARTICLE") != null && !rowVals.isForeignEmpty("ID_ARTICLE")) {
                mapRows.put(rowVals.getForeignID("ID_ARTICLE"), i);
            }
        }

        Set<String> fieldsFrom = m.getFieldsFrom();
        fieldsFrom.remove("POURCENT_REMISE");
        for (int i = articles.size() - 1; i >= 0; i--) {

            Tuple3<Double, String, String> tuple = articles.get(i);

            SQLRow article = mapCode.get(tuple.get1());
            String fieldQte = "QTE";

            if (article != null && mapRows.containsKey(article.getID())) {
                Integer index = mapRows.get(article.getID());
                SQLRowValues rowVals = getRowValuesTable().getRowValuesTableModel().getRowValuesAt(index);
                if (rowVals.getTable().getName().equals("BON_DE_LIVRAISON_ELEMENT")) {
                    fieldQte = "QTE_LIVREE";
                }
                getRowValuesTable().getRowValuesTableModel().putValue(rowVals.getInt(fieldQte) + 1, index, fieldQte);
            } else {
                final SQLRowValues row2Insert = new SQLRowValues(getRowValuesTable().getRowValuesTableModel().getDefaultRowValues());

                // Completion depuis l'article trouvé
                if (article != null) {
                    m.fillRowValues(article, fieldsFrom, row2Insert);
                    // Fill prix total
                    row2Insert.put("ID_ARTICLE", article.getID());
                    row2Insert.put("CODE", article.getObject("CODE"));
                    row2Insert.put("NOM", article.getObject("NOM"));
                } else {
                    row2Insert.put("CODE", tuple.get1());
                    row2Insert.put("NOM", tuple.get2());
                }

                row2Insert.put(fieldQte, Math.round(tuple.get0().floatValue()));
                if (row2Insert.getTable().getName().equals("BON_DE_LIVRAISON_ELEMENT")) {
                    row2Insert.put("QTE_LIVREE", Math.round(tuple.get0().floatValue()));
                }
                row2Insert.put("POURCENT_REMISE", BigDecimal.ZERO);
                row2Insert.put("MONTANT_REMISE", BigDecimal.ZERO);

                row2Insert.put("PV_HT", row2Insert.getObject("PRIX_METRIQUE_VT_1"));
                //
                final BigDecimal resultTotalHT = row2Insert.getBigDecimal("PV_HT").multiply(new BigDecimal(row2Insert.getInt(fieldQte)));
                row2Insert.put("T_PV_HT", resultTotalHT);

                Float resultTaux = TaxeCache.getCache().getTauxFromId(row2Insert.getForeignID("ID_TAXE"));

                if (resultTaux == null) {
                    SQLRow rowTax = TaxeCache.getCache().getFirstTaxe();
                    resultTaux = rowTax.getFloat("TAUX");
                }

                float taux = (resultTaux == null) ? 0.0F : resultTaux.floatValue();

                BigDecimal r = resultTotalHT.multiply(BigDecimal.valueOf(taux).movePointLeft(2).add(BigDecimal.ONE), DecimalUtils.HIGH_PRECISION);

                row2Insert.put("T_PV_TTC", r);
                // row2Insert.put("ID_STYLE", allStyleByName.get("Composant"));
                getRowValuesTable().getRowValuesTableModel().addRowAt(0, row2Insert);
            }
        }
    }
}
