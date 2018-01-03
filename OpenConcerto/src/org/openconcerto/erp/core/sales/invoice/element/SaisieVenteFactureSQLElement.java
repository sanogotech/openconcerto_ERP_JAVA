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
 
 package org.openconcerto.erp.core.sales.invoice.element;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.config.Gestion;
import org.openconcerto.erp.core.common.component.TransfertBaseSQLComponent;
import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.erp.core.common.ui.AbstractVenteArticleItemTable;
import org.openconcerto.erp.core.common.ui.DeviseField;
import org.openconcerto.erp.core.common.ui.PanelFrame;
import org.openconcerto.erp.core.finance.accounting.element.EcritureSQLElement;
import org.openconcerto.erp.core.sales.account.PartialInvoiceEditGroup;
import org.openconcerto.erp.core.sales.account.VenteFactureSituationSQLComponent;
import org.openconcerto.erp.core.sales.account.VenteFactureSoldeEditGroup;
import org.openconcerto.erp.core.sales.account.VenteFactureSoldeSQLComponent;
import org.openconcerto.erp.core.sales.invoice.component.SaisieVenteFactureSQLComponent;
import org.openconcerto.erp.core.sales.invoice.report.VenteFactureXmlSheet;
import org.openconcerto.erp.core.sales.product.element.ReferenceArticleSQLElement;
import org.openconcerto.erp.core.sales.shipment.component.BonDeLivraisonSQLComponent;
import org.openconcerto.erp.core.supplychain.stock.element.MouvementStockSQLElement;
import org.openconcerto.erp.generationEcritures.GenerationMvtRetourNatexis;
import org.openconcerto.erp.generationEcritures.GenerationMvtSaisieVenteFacture;
import org.openconcerto.erp.model.MouseSheetXmlListeListener;
import org.openconcerto.erp.preferences.DefaultNXProps;
import org.openconcerto.erp.preferences.GestionArticleGlobalPreferencePanel;
import org.openconcerto.erp.preferences.PrinterNXProps;
import org.openconcerto.erp.rights.NXRights;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.GlobalMapper;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.element.SQLElementLink.LinkType;
import org.openconcerto.sql.element.SQLElementLinksSetup;
import org.openconcerto.sql.element.TreesOfSQLRows;
import org.openconcerto.sql.model.FieldPath;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLInjector;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowListRSH;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.model.graph.Path;
import org.openconcerto.sql.preferences.SQLPreferences;
import org.openconcerto.sql.request.ListSQLRequest;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.sql.users.rights.UserRightsManager;
import org.openconcerto.sql.view.EditFrame;
import org.openconcerto.sql.view.EditPanel;
import org.openconcerto.sql.view.EditPanel.EditMode;
import org.openconcerto.sql.view.EditPanelListener;
import org.openconcerto.sql.view.list.BaseSQLTableModelColumn;
import org.openconcerto.sql.view.list.IListe;
import org.openconcerto.sql.view.list.IListeAction.IListeEvent;
import org.openconcerto.sql.view.list.RowAction;
import org.openconcerto.sql.view.list.RowAction.PredicateRowAction;
import org.openconcerto.sql.view.list.SQLTableModelSource;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.FrameUtil;
import org.openconcerto.ui.table.PercentTableCellRenderer;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.DecimalUtils;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.ListMap;
import org.openconcerto.utils.Tuple2;
import org.openconcerto.utils.cc.IClosure;
import org.openconcerto.utils.cc.ITransformer;
import org.openconcerto.utils.i18n.TranslationManager;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.apache.commons.dbutils.handlers.ArrayListHandler;

// Depuis le 1er juillet 2003, la règlementation fiscale impose la délivrance d'une facture pour
// tous les versements d'acomptes, même lorsqu'ils ne donnent pas lieu à exigibilité de la TVA
// (article 289 I -1-c du CGI).
// Avant la loi de finances rectificative pour 2002, il n'y avait obligation de délivrer une facture
// pour les acomptes que lorsque la TVA était exigible sur ces versements. Depuis l'entrée en
// vigueur de cette loi, initialement fixée au 1er juillet 2003, et reportée par tolérance
// administrative au 1er janvier 2004, il faut désormais délivrer une facture pour tous les acomptes
// perçus.
// L'obligation nouvelle de facturer tous les versements d'acomptes ne modifie pas les règles
// d'exigibilité de la TVA.
// La date du versement de l'acompte doit être indiquée sur la facture d'acompte si elle est
// différente de la date de délivrance de cette facture, et si elle est connue à cette date.

// La facture d'acompte peut ne pas mentionner l'ensemble des mentions obligatoires lorsque les
// informations nécessaires à son établissement ne sont pas connues au moment de son émission (par
// exemple, quantité ou prix exact du produit).
public class SaisieVenteFactureSQLElement extends ComptaSQLConfElement {

    public static final String TABLENAME = "SAISIE_VENTE_FACTURE";

    public SaisieVenteFactureSQLElement() {
        super(TABLENAME, "une facture", "factures");

        GlobalMapper.getInstance().map(VenteFactureSituationSQLComponent.ID, new PartialInvoiceEditGroup());
        addComponentFactory(VenteFactureSituationSQLComponent.ID, new ITransformer<Tuple2<SQLElement, String>, SQLComponent>() {

            @Override
            public SQLComponent transformChecked(Tuple2<SQLElement, String> input) {

                return new VenteFactureSituationSQLComponent(SaisieVenteFactureSQLElement.this);
            }
        });
        GlobalMapper.getInstance().map(VenteFactureSoldeSQLComponent.ID, new VenteFactureSoldeEditGroup());
        addComponentFactory(VenteFactureSoldeSQLComponent.ID, new ITransformer<Tuple2<SQLElement, String>, SQLComponent>() {

            @Override
            public SQLComponent transformChecked(Tuple2<SQLElement, String> input) {

                return new VenteFactureSoldeSQLComponent(SaisieVenteFactureSQLElement.this);
            }
        });

        final boolean affact = UserRightsManager.getCurrentUserRights().haveRight(NXRights.ACCES_RETOUR_AFFACTURAGE.getCode());
        List<RowAction> l = new ArrayList<RowAction>(5);
            PredicateRowAction actionBL = new PredicateRowAction(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    TransfertBaseSQLComponent.openTransfertFrame(IListe.get(e).getSelectedRows(), "BON_DE_LIVRAISON");
                }
            }, false, "sales.invoice.create.delivery");
            actionBL.setPredicate(IListeEvent.getSingleSelectionPredicate());
            l.add(actionBL);
        PredicateRowAction actionAvoir = new PredicateRowAction(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                TransfertBaseSQLComponent.openTransfertFrame(IListe.get(e).getSelectedRows(), "AVOIR_CLIENT");
            }
        }, false, "sales.invoice.create.credit");
        actionAvoir.setPredicate(IListeEvent.getSingleSelectionPredicate());
        l.add(actionAvoir);

        final String property = PrinterNXProps.getInstance().getProperty("QLPrinter");
        if (property != null && property.trim().length() > 0) {
            PredicateRowAction actionPrintLabel = new PredicateRowAction(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    // ((ClientNormalSQLElement)
                    // getForeignElement("ID_CLIENT")).printLabel(IListe.get(e).getSelectedRow().getForeign("ID_CLIENT"),
                    // property);
                }
            }, false, "customerrelationship.customer.label.print");
            actionPrintLabel.setPredicate(IListeEvent.getSingleSelectionPredicate());
           // l.add(actionPrintLabel);
        }
        RowAction actionClone = new RowAction(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {

                SQLElement eltFact = Configuration.getInstance().getDirectory().getElement("SAISIE_VENTE_FACTURE");
                EditFrame editFrame = new EditFrame(eltFact, EditPanel.CREATION);

                ((SaisieVenteFactureSQLComponent) editFrame.getSQLComponent()).loadFactureExistante(IListe.get(e).getSelectedId());
                editFrame.setVisible(true);
            }
        }, false, "sales.invoice.clone") {
            public boolean enabledFor(IListeEvent evt) {
                List<? extends SQLRowAccessor> l = evt.getSelectedRows();
                if (l != null && l.size() == 1) {
                    SQLRowAccessor r = l.get(0);
                    return !r.getBoolean("PARTIAL") && !r.getBoolean("SOLDE");
                }
                return false;
            }
        };

        l.add(actionClone);
        getRowActions().addAll(l);


        PredicateRowAction actionClient = new PredicateRowAction(new AbstractAction("Détails client") {
            EditFrame edit;
            private SQLElement eltClient = Configuration.getInstance().getDirectory().getElement(((ComptaPropsConfiguration) Configuration.getInstance()).getRootSociete().getTable("CLIENT"));

            public void actionPerformed(ActionEvent e) {
                if (edit == null) {
                    edit = new EditFrame(eltClient, EditMode.READONLY);
                }
                edit.selectionId(IListe.get(e).fetchSelectedRow().getInt("ID_CLIENT"));
                edit.setVisible(true);
            }
        }, false, "sales.invoice.info.show");
        actionClient.setPredicate(IListeEvent.getSingleSelectionPredicate());
        getRowActions().add(actionClient);

        PredicateRowAction actionCommande = new PredicateRowAction(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                SaisieVenteFactureSQLElement elt = (SaisieVenteFactureSQLElement) Configuration.getInstance().getDirectory().getElement("SAISIE_VENTE_FACTURE");
                elt.transfertCommande(IListe.get(e).getSelectedId());
            }
        }, false, "sales.invoice.create.supplier.order");
        actionCommande.setPredicate(IListeEvent.getSingleSelectionPredicate());
        getRowActions().add(actionCommande);

        RowAction actionCancelAvoir = new PredicateRowAction(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                cancelAvoir(IListe.get(e).getSelectedRow());
            }
        }, false, "sales.invoice.cancel.credit") {
            @Override
            public boolean enabledFor(IListeEvent evt) {
                final SQLRowAccessor selectedRow = evt.getSelectedRow().asRow();
                final List<SQLRowValues> rows = evt.getSelectedRows();
                return rows != null && rows.size() == 1 && selectedRow != null && !selectedRow.isForeignEmpty("ID_AVOIR_CLIENT");
            }
        };
        getRowActions().add(actionCancelAvoir);

        MouseSheetXmlListeListener mouseSheetXmlListeListener = new MouseSheetXmlListeListener(VenteFactureXmlSheet.class);
        getRowActions().addAll(mouseSheetXmlListeListener.getRowActions());
        // this.frame.getPanel().getListe().addRowActions(mouseListener.getRowActions());

    }

    @Override
    protected void setupLinks(SQLElementLinksSetup links) {

        super.setupLinks(links);
        if (getTable().contains("ID_ADRESSE")) {
            links.get("ID_ADRESSE").setType(LinkType.ASSOCIATION);
        }
        if (getTable().contains("ID_ADRESSE_LIVRAISON")) {
            links.get("ID_ADRESSE_LIVRAISON").setType(LinkType.ASSOCIATION);
        }
    }

    @Override
    public ListMap<String, String> getShowAs() {
        ListMap<String, String> map = new ListMap<String, String>();
        map.putCollection(null, "NUMERO", "DATE", "ID_COMMERCIAL");
        return map;
    }

    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();

            l.add("NUMERO");
            l.add("DATE");
            l.add("NOM");
            l.add("ID_CLIENT");
                l.add("ID_MODE_REGLEMENT");
            l.add("ID_COMMERCIAL");
            if (UserRightsManager.getCurrentUserRights().haveRight(AbstractVenteArticleItemTable.SHOW_PRIX_ACHAT_CODE)) {
                l.add("T_HA");
            }
            l.add("T_HT");
            l.add("T_TTC");
            l.add("INFOS");

                    l.add("DATE_ENVOI");
                l.add("DATE_REGLEMENT");
        return l;
    }

    @Override
    protected void _initListRequest(ListSQLRequest req) {
        super._initListRequest(req);
        req.changeGraphToFetch(new IClosure<SQLRowValues>() {
            @Override
            public void executeChecked(SQLRowValues graphToFetch) {
                graphToFetch.put("ACOMPTE", null);
                graphToFetch.put("PARTIAL", null);
                graphToFetch.put("SOLDE", null);
                graphToFetch.put("COMPLEMENT", null);

                graphToFetch.put("PREVISIONNELLE", null);
                    graphToFetch.grow("ID_MODE_REGLEMENT").put("AJOURS", null).put("LENJOUR", null);
                SQLRowValues value = new SQLRowValues(graphToFetch.getTable().getTable("MOUVEMENT"));
                value.put("ID_PIECE", null);
                graphToFetch.put("ID_MOUVEMENT", value);
                graphToFetch.put("T_AVOIR_TTC", null);
            }
        });
    }

    private BigDecimal getAvancement(SQLRowAccessor r) {
        Collection<? extends SQLRowAccessor> rows = r.getReferentRows(r.getTable().getTable("ECHEANCE_CLIENT"));
        long totalEch = 0;

        for (SQLRowAccessor row : rows) {
            if (!row.getBoolean("REGLE") && !row.getBoolean("REG_COMPTA")) {
                totalEch += row.getLong("MONTANT");
            }
        }

        SQLRowAccessor avoir = r.getForeign("ID_AVOIR_CLIENT");
        BigDecimal avoirTTC = BigDecimal.ZERO;
        if (avoir != null && !avoir.isUndefined()) {
            avoirTTC = new BigDecimal(avoir.getLong("MONTANT_TTC"));
        }

        final BigDecimal totalAregler = new BigDecimal(r.getLong("T_TTC")).subtract(avoirTTC);
        if (totalAregler.signum() > 0 && totalEch > 0) {
            return totalAregler.subtract(new BigDecimal(totalEch)).divide(totalAregler, DecimalUtils.HIGH_PRECISION).movePointRight(2).setScale(2, RoundingMode.HALF_UP);
        } else {
            return BigDecimal.ONE.movePointRight(2);
        }
    }

    @Override
    protected synchronized void _initTableSource(final SQLTableModelSource table) {
        super._initTableSource(table);

        final BaseSQLTableModelColumn colAvancement = new BaseSQLTableModelColumn("Avancement réglement", BigDecimal.class) {

            @Override
            protected Object show_(SQLRowAccessor r) {

                return getAvancement(r);
            }

            @Override
            public Set<FieldPath> getPaths() {
                Path p = new Path(SaisieVenteFactureSQLElement.this.getTable());
                p = p.add(getTable().getTable("ECHEANCE_CLIENT"));

                Path p2 = new Path(SaisieVenteFactureSQLElement.this.getTable());
                p2 = p2.add(getTable().getField("ID_AVOIR_CLIENT"));

                return CollectionUtils.createSet(new FieldPath(p, "MONTANT"), new FieldPath(p, "REG_COMPTA"), new FieldPath(p, "REGLE"), new FieldPath(p2, "MONTANT_TTC"));
            }
        };
        table.getColumns().add(colAvancement);
        colAvancement.setRenderer(new PercentTableCellRenderer());

    }

    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NUMERO");
        return l;
    }

    @Override
    public Set<String> getReadOnlyFields() {
        Set<String> s = new HashSet<String>(1);
        s.add("CONTROLE_TECHNIQUE");
        return s;
    }

    @Override
    public Set<String> getInsertOnlyFields() {
        Set<String> s = new HashSet<String>(1);
        s.add("ACOMPTE");
        return s;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openconcerto.devis.SQLElement#getComponent()
     */
    public SQLComponent createComponent() {
        return new SaisieVenteFactureSQLComponent();
    }

    @Override
    protected void archive(TreesOfSQLRows trees, boolean cutLinks) throws SQLException {
        for (SQLRow row : trees.getRows()) {

            // On retire l'avoir
            if (row.getInt("ID_AVOIR_CLIENT") > 1) {
                SQLElement eltAvoir = Configuration.getInstance().getDirectory().getElement("AVOIR_CLIENT");
                SQLRow rowAvoir = eltAvoir.getTable().getRow(row.getInt("ID_AVOIR_CLIENT"));

                Long montantSolde = (Long) rowAvoir.getObject("MONTANT_SOLDE");

                Long avoirTTC = (Long) row.getObject("T_AVOIR_TTC");

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
                try {
                    rowVals.update();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

            super.archive(new TreesOfSQLRows(this, row), cutLinks);
            SQLPreferences prefs = new SQLPreferences(getTable().getDBRoot());
            if (prefs.getBoolean(GestionArticleGlobalPreferencePanel.STOCK_FACT, true)) {

                // Mise à jour des stocks
                SQLElement eltMvtStock = Configuration.getInstance().getDirectory().getElement("MOUVEMENT_STOCK");
                SQLSelect sel = new SQLSelect();
                sel.addSelect(eltMvtStock.getTable().getField("ID"));
                Where w = new Where(eltMvtStock.getTable().getField("IDSOURCE"), "=", row.getID());
                Where w2 = new Where(eltMvtStock.getTable().getField("SOURCE"), "=", getTable().getName());
                sel.setWhere(w.and(w2));

                @SuppressWarnings("rawtypes")
                List l = (List) eltMvtStock.getTable().getBase().getDataSource().execute(sel.asString(), new ArrayListHandler());
                if (l != null) {
                    for (int i = 0; i < l.size(); i++) {
                        Object[] tmp = (Object[]) l.get(i);
                        eltMvtStock.archive(((Number) tmp[0]).intValue());
                    }
                }
            }
        }
    }

    public void transfertBL(int idFacture) {
        final SQLElement elt = Configuration.getInstance().getDirectory().getElement("BON_DE_LIVRAISON");
        final EditFrame editAvoirFrame = new EditFrame(elt);
        editAvoirFrame.setIconImage(new ImageIcon(Gestion.class.getResource("frameicon.png")).getImage());

        final BonDeLivraisonSQLComponent comp = (BonDeLivraisonSQLComponent) editAvoirFrame.getSQLComponent();
        final SQLInjector inject = SQLInjector.getInjector(this.getTable(), elt.getTable());
        SQLRowValues createRowValuesFrom = inject.createRowValuesFrom(idFacture);
        SQLRow rowFacture = getTable().getRow(idFacture);
        String string = rowFacture.getString("NOM");
        createRowValuesFrom.put("NOM", string + (string.trim().length() == 0 ? "" : ", ") + rowFacture.getString("NUMERO"));
        comp.select(createRowValuesFrom);
        // comp.loadFactureItem(idFacture);

        editAvoirFrame.pack();
        editAvoirFrame.setState(JFrame.NORMAL);
        editAvoirFrame.setVisible(true);

    }

    /**
     * Transfert en commande fournisseur
     */
    public void transfertCommande(final int idFacture) {
        ComptaPropsConfiguration.getInstanceCompta().getNonInteractiveSQLExecutor().execute(new Runnable() {

            @Override
            public void run() {
                SQLElement elt = Configuration.getInstance().getDirectory().getElement("SAISIE_VENTE_FACTURE_ELEMENT");
                SQLTable tableCmdElt = Configuration.getInstance().getDirectory().getElement("COMMANDE_ELEMENT").getTable();
                SQLElement eltArticle = Configuration.getInstance().getDirectory().getElement("ARTICLE");
                List<SQLRow> rows = getTable().getRow(idFacture).getReferentRows(elt.getTable());
                final ListMap<SQLRow, SQLRowValues> map = new ListMap<SQLRow, SQLRowValues>();
                SQLRow rowDeviseF = null;
                for (SQLRow sqlRow : rows) {
                    // on récupére l'article qui lui correspond
                    SQLRowValues rowArticle = new SQLRowValues(eltArticle.getTable());
                    for (SQLField field : eltArticle.getTable().getFields()) {
                        if (sqlRow.getTable().getFieldsName().contains(field.getName())) {
                            rowArticle.put(field.getName(), sqlRow.getObject(field.getName()));
                        }
                    }

                    int idArticle = ReferenceArticleSQLElement.getIdForCNM(rowArticle, true);
                    SQLRow rowArticleFind = eltArticle.getTable().getRow(idArticle);
                    if (rowArticleFind != null) {
                        SQLInjector inj = SQLInjector.getInjector(rowArticle.getTable(), tableCmdElt);
                        SQLRowValues rowValsElt = new SQLRowValues(inj.createRowValuesFrom(rowArticleFind));
                        rowValsElt.put("ID_STYLE", sqlRow.getObject("ID_STYLE"));
                        rowValsElt.put("QTE", sqlRow.getObject("QTE"));
                        rowValsElt.put("T_POIDS", rowValsElt.getLong("POIDS") * rowValsElt.getInt("QTE"));

                        // gestion de la devise
                        rowDeviseF = sqlRow.getForeignRow("ID_DEVISE");
                        SQLRow rowDeviseHA = rowArticleFind.getForeignRow("ID_DEVISE_HA");
                        BigDecimal qte = new BigDecimal(rowValsElt.getInt("QTE"));
                        if (rowDeviseF != null && !rowDeviseF.isUndefined()) {
                            if (rowDeviseF.getID() == rowDeviseHA.getID()) {
                                rowValsElt.put("PA_DEVISE", rowArticleFind.getObject("PA_DEVISE"));
                                rowValsElt.put("PA_DEVISE_T", ((BigDecimal) rowArticleFind.getObject("PA_DEVISE")).multiply(qte, DecimalUtils.HIGH_PRECISION));
                                rowValsElt.put("ID_DEVISE", rowDeviseF.getID());
                            } else {
                                BigDecimal taux = (BigDecimal) rowDeviseF.getObject("TAUX");
                                rowValsElt.put("PA_DEVISE", taux.multiply((BigDecimal) rowValsElt.getObject("PA_HT")));
                                rowValsElt.put("PA_DEVISE_T", ((BigDecimal) rowValsElt.getObject("PA_DEVISE")).multiply(qte, DecimalUtils.HIGH_PRECISION));
                                rowValsElt.put("ID_DEVISE", rowDeviseF.getID());
                            }
                        }

                        BigDecimal prixHA = (BigDecimal) rowValsElt.getObject("PA_HT");
                        rowValsElt.put("T_PA_HT", prixHA.multiply(qte, DecimalUtils.HIGH_PRECISION));

                        rowValsElt.put("T_PA_HT", prixHA.multiply(qte, DecimalUtils.HIGH_PRECISION));
                        rowValsElt.put("T_PA_TTC",
                                ((BigDecimal) rowValsElt.getObject("T_PA_HT")).multiply(new BigDecimal(rowValsElt.getForeign("ID_TAXE").getFloat("TAUX") / 100.0 + 1.0), DecimalUtils.HIGH_PRECISION));

                        map.add(rowArticleFind.getForeignRow("ID_FOURNISSEUR"), rowValsElt);
                    }
                }
                MouvementStockSQLElement.createCommandeF(map, rowDeviseF);

            }

        });

    }

    public interface DoWithRow {
        public void process(SQLRow row);
    }

    Map<String, DoWithRow> specialAction = new HashMap<String, DoWithRow>();

    public DoWithRow getSpecialAction(String key) {
        return specialAction.get(key);
    }

    public void putSpecialAction(String key, DoWithRow action) {
        specialAction.put(key, action);
    }

    public void cancelAvoir(final SQLRowAccessor rowFactureOrigin) {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new DefaultGridBagConstraints();
        c.gridwidth = GridBagConstraints.REMAINDER;
        p.add(new JLabel("Voulez annuler l'avoir affecté sur cette facture?"), c);
        c.gridwidth = 1;
        c.gridy++;
        c.gridx = 0;
        final JButton buttonApply = new JButton("Appliquer");
        JButton buttonAnnuler = new JButton("Fermer");
        p.add(buttonApply, c);

        c.gridx++;
        p.add(buttonAnnuler, c);
        final PanelFrame f = new PanelFrame(p, "Suppression d'un avoir client sur facture");

        buttonAnnuler.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                f.dispose();
            }
        });

        buttonApply.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                long ttc = rowFactureOrigin.getLong("T_TTC");
                // long netAPayer = rowFactureOrigin.getLong("NET_A_PAYER");
                long avoirTTC = rowFactureOrigin.getLong("T_AVOIR_TTC");
                SQLRowAccessor rowAvoir = rowFactureOrigin.getForeign("ID_AVOIR_CLIENT");

                final SQLRowValues createEmptyUpdateRow = rowFactureOrigin.createEmptyUpdateRow();
                createEmptyUpdateRow.put("ID_AVOIR_CLIENT", rowFactureOrigin.getTable().getTable("AVOIR_CLIENT").getUndefinedID());
                createEmptyUpdateRow.put("NET_A_PAYER", ttc);
                createEmptyUpdateRow.put("T_AVOIR_TTC", 0L);
                try {
                    SQLRow rowFacture = createEmptyUpdateRow.commit();

                    // long restant = totalAvoirTTC - totalAvoirApplique;

                    SQLRowValues rowVals = rowAvoir.createEmptyUpdateRow();
                    rowVals.put("SOLDE", Boolean.FALSE);
                    rowVals.put("MONTANT_SOLDE", 0L);
                    rowVals.put("MONTANT_RESTANT", avoirTTC);
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
