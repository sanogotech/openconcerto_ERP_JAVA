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
 
 package org.openconcerto.erp.core.sales.quote.element;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.config.Gestion;
import org.openconcerto.erp.core.common.component.TransfertBaseSQLComponent;
import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.erp.core.common.ui.AbstractVenteArticleItemTable;
import org.openconcerto.erp.core.sales.invoice.component.SaisieVenteFactureSQLComponent;
import org.openconcerto.erp.core.sales.product.element.ReferenceArticleSQLElement;
import org.openconcerto.erp.core.sales.quote.component.DevisSQLComponent;
import org.openconcerto.erp.core.sales.quote.report.DevisXmlSheet;
import org.openconcerto.erp.core.sales.quote.ui.QuoteEditGroup;
import org.openconcerto.erp.core.sales.quote.ui.QuoteSQLComponent;
import org.openconcerto.erp.core.supplychain.stock.element.MouvementStockSQLElement;
import org.openconcerto.erp.model.MouseSheetXmlListeListener;
import org.openconcerto.erp.utils.KDUtils;
import org.openconcerto.erp.utils.KDUtils.Folder;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.GlobalMapper;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.element.SQLElementLink.LinkType;
import org.openconcerto.sql.element.SQLElementLinksSetup;
import org.openconcerto.sql.model.FieldPath;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLInjector;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLRowValuesListFetcher;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.model.graph.Path;
import org.openconcerto.sql.request.ListSQLRequest;
import org.openconcerto.sql.users.UserManager;
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
import org.openconcerto.ui.light.ActivationOnSelectionControler;
import org.openconcerto.ui.light.ColumnSpec;
import org.openconcerto.ui.light.ColumnsSpec;
import org.openconcerto.ui.light.CustomEditorProvider;
import org.openconcerto.ui.light.LightControler;
import org.openconcerto.ui.light.LightUIComboBox;
import org.openconcerto.ui.light.LightUIElement;
import org.openconcerto.ui.light.LightUILine;
import org.openconcerto.ui.light.LightUIPanel;
import org.openconcerto.ui.light.LightUITable;
import org.openconcerto.ui.light.LightUITextField;
import org.openconcerto.ui.light.Row;
import org.openconcerto.ui.light.RowSelectionSpec;
import org.openconcerto.ui.light.TableContent;
import org.openconcerto.ui.light.TableSpec;
import org.openconcerto.ui.table.TimestampTableCellRenderer;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.DecimalUtils;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.FileUtils;
import org.openconcerto.utils.ListMap;
import org.openconcerto.utils.cc.ITransformer;
import org.openconcerto.utils.ui.StringWithId;

import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.DOMBuilder;

public class DevisSQLElement extends ComptaSQLConfElement {

    public static final String TABLENAME = "DEVIS";
    public static final String FACTURE_TAG_RIGHT = "TAG_FACTURE_DEVIS";

    public static enum Month {

        JANVIER("01", "Janvier"), FEVRIER("02", "Février"), MARS("03", "Mars"), AVRIL("04", "Avril"), MAI("05", "Mai"), JUIN("06", "Juin"), JUILLET("07", "Juillet"), AOUT("08",
                "Août"), SEPTEMBRE("09", "Septembre"), OCTOBRE("10", "Octobre"), NOVEMBRE("11", "Novembre"), DECEMBRE("12", "Décembre");

        private String number;
        private String name;

        Month(String number, String name) {
            this.number = number;
            this.name = name;
        }

        public String getName() {
            return this.name;
        }

        public String getNumber() {
            return this.number;
        }

        public String getPath() {
            return this.getNumber() + "-" + this.getName();
        }
    };

    public DevisSQLElement() {
        this("un devis", "devis");
    }

    public DevisSQLElement(String singular, String plural) {
        super(TABLENAME, singular, plural);

        getRowActions().addAll(getDevisRowActions());
        final QuoteEditGroup group = new QuoteEditGroup();
        GlobalMapper.getInstance().map(QuoteSQLComponent.ID, group);
        setDefaultGroup(group);

    }


    private List<RowAction> getDevisRowActions() {

        List<RowAction> rowsActions = new ArrayList<RowAction>();

        // List<RowAction> list = new ArrayList<RowAction>();
        // Transfert vers facture
        RowAction factureAction = getDevis2FactureAction();

        rowsActions.add(factureAction);

        PredicateRowAction actionClient = new PredicateRowAction(new AbstractAction("Détails client") {
            EditFrame edit;
            private SQLElement eltClient = Configuration.getInstance().getDirectory().getElement(((ComptaPropsConfiguration) Configuration.getInstance()).getRootSociete().getTable("CLIENT"));

            public void actionPerformed(ActionEvent e) {
                if (this.edit == null) {
                    this.edit = new EditFrame(this.eltClient, EditMode.MODIFICATION);
                }
                this.edit.selectionId(IListe.get(e).getSelectedRow().getForeignID("ID_CLIENT"));
                this.edit.setVisible(true);
            }
        }, false);
        actionClient.setPredicate(IListeEvent.getSingleSelectionPredicate());
        rowsActions.add(actionClient);


        // Voir le document
        RowAction actionTransfertCmd = getDevis2CmdFournAction();
        rowsActions.add(actionTransfertCmd);

        // Transfert vers commande
        RowAction commandeAction = getDevis2CmdCliAction();

        rowsActions.add(commandeAction);

        RowAction accepteEtCmdAction = getAcceptAndCmdClientAction();
        rowsActions.add(accepteEtCmdAction);

        // Marqué accepté
        RowAction accepteAction = getAcceptAction();

        rowsActions.add(accepteAction);

        // Marqué accepté
        RowAction refuseAction = getRefuseAction();

        rowsActions.add(refuseAction);

        // // Dupliquer
        RowAction cloneAction = getCloneAction();

        rowsActions.add(cloneAction);

        MouseSheetXmlListeListener mouseSheetXmlListeListener = new MouseSheetXmlListeListener(DevisXmlSheet.class);
        mouseSheetXmlListeListener.setGenerateHeader(true);
        mouseSheetXmlListeListener.setShowHeader(true);

        rowsActions.addAll(mouseSheetXmlListeListener.getRowActions());

        return rowsActions;
    }

    public static void davBrowse(String s) throws Exception {
        final boolean windows = System.getProperty("os.name").startsWith("Windows");
        if (windows) {
            Desktop.getDesktop().browse(new URI(s));
        } else {
            String[] cmdarray = new String[] { "xdg-open", s };
            final int res = Runtime.getRuntime().exec(cmdarray).waitFor();
            if (res != 0)
                throw new IOException("error (" + res + ") executing " + Arrays.asList(cmdarray));
        }
    }

    public RowAction getCloneAction() {
        return new RowAction(new AbstractAction() {

            public void actionPerformed(ActionEvent e) {
                SQLRowAccessor selectedRow = IListe.get(e).getSelectedRow();

                SQLElement eltFact = Configuration.getInstance().getDirectory().getElement("DEVIS");
                EditFrame editFrame = new EditFrame(eltFact, EditPanel.CREATION);

                ((DevisSQLComponent) editFrame.getSQLComponent()).loadDevisExistant(selectedRow.getID());
                editFrame.setVisible(true);
            }
        }, true, "sales.quote.clone") {
            public boolean enabledFor(java.util.List<org.openconcerto.sql.model.SQLRowValues> selection) {
                return (selection != null && selection.size() == 1);
            };
        };
    }

    public RowAction getRefuseAction() {
        return new RowAction(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                SQLRowValues rowVals = IListe.get(e).getSelectedRow().asRow().createEmptyUpdateRow();
                rowVals.put("ID_ETAT_DEVIS", EtatDevisSQLElement.REFUSE);
                try {
                    rowVals.update();
                } catch (SQLException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            }
        }, false, "sales.quote.refuse") {
            public boolean enabledFor(java.util.List<org.openconcerto.sql.model.SQLRowValues> selection) {
                if (selection != null && selection.size() == 1) {
                    if (selection.get(0).getForeignID("ID_ETAT_DEVIS") == EtatDevisSQLElement.EN_ATTENTE) {
                        return true;
                    }
                }
                return false;
            };
        };
    }

    public RowAction getAcceptAction() {
        return new RowAction(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                SQLRow selectedRow = IListe.get(e).getSelectedRow().asRow();
                SQLRowValues rowVals = selectedRow.createEmptyUpdateRow();
                rowVals.put("ID_ETAT_DEVIS", EtatDevisSQLElement.ACCEPTE);
                try {
                    rowVals.update();
                } catch (SQLException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            }
        }, false, "sales.quote.accept") {
            public boolean enabledFor(java.util.List<org.openconcerto.sql.model.SQLRowValues> selection) {
                if (selection != null && selection.size() == 1) {
                    final int int1 = selection.get(0).getForeignID("ID_ETAT_DEVIS");
                    if (int1 != EtatDevisSQLElement.REFUSE && int1 != EtatDevisSQLElement.ACCEPTE) {
                        return true;
                    }
                }
                return false;
            };
        };
    }

    public RowAction getDevis2FactureAction() {
        return new RowAction(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                TransfertBaseSQLComponent.openTransfertFrame(IListe.get(e).getSelectedRows(), "SAISIE_VENTE_FACTURE");

            }
        }, true, "sales.quote.create.invoice") {
            public boolean enabledFor(java.util.List<org.openconcerto.sql.model.SQLRowValues> selection) {
                boolean b = selection.size() > 0;
                for (SQLRowAccessor sqlRowAccessor : selection) {
                    b &= sqlRowAccessor.getForeignID("ID_ETAT_DEVIS") == EtatDevisSQLElement.ACCEPTE;
                }

                return b;
            };
        };
    }

    public RowAction getDevis2CmdFournAction() {
        return new RowAction(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                final SQLRow selectedRow = IListe.get(e).fetchSelectedRow();
                ComptaPropsConfiguration.getInstanceCompta().getNonInteractiveSQLExecutor().execute(new Runnable() {

                    @Override
                    public void run() {
                        transfertCommande(selectedRow);

                    }
                });

            }
        }, false, "sales.quote.create.supplier.order") {
            public boolean enabledFor(java.util.List<org.openconcerto.sql.model.SQLRowValues> selection) {
                if (selection != null && selection.size() == 1) {
                    if (selection.get(0).getForeignID("ID_ETAT_DEVIS") == EtatDevisSQLElement.ACCEPTE) {
                        return true;
                    }
                }
                return false;
            };
        };
    }

    public RowAction getDevis2CmdCliAction() {
        return new RowAction(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {

                final List<SQLRowValues> copySelectedRows = IListe.get(e).getSelectedRows();
                transfertCommandeClient(copySelectedRows);
            }

        }, true, "sales.quote.create.customer.order") {
            public boolean enabledFor(java.util.List<org.openconcerto.sql.model.SQLRowValues> selection) {
                if (selection != null && selection.size() == 1) {
                    if (selection.get(0).getForeignID("ID_ETAT_DEVIS") == EtatDevisSQLElement.ACCEPTE) {
                        return true;
                    }
                }
                return false;
            };
        };
    }

    public RowAction getAcceptAndCmdClientAction() {
        return new RowAction(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                SQLRow selectedRow = IListe.get(e).fetchSelectedRow();
                SQLRowValues rowVals = selectedRow.createEmptyUpdateRow();
                rowVals.put("ID_ETAT_DEVIS", EtatDevisSQLElement.ACCEPTE);
                try {
                    rowVals.update();
                } catch (SQLException e1) {
                    ExceptionHandler.handle("Erreur la de la mise à jour de l'état du devis!", e1);

                }
                transfertCommandeClient(IListe.get(e).getSelectedRows());
            }
        }, false, "sales.quote.accept.create.customer.order") {
            public boolean enabledFor(java.util.List<org.openconcerto.sql.model.SQLRowValues> selection) {
                if (selection != null && selection.size() == 1) {
                    if (selection.get(0).getForeignID("ID_ETAT_DEVIS") == EtatDevisSQLElement.EN_ATTENTE) {
                        return true;
                    }
                }
                return false;
            };
        };
    }

    public void transfertCommandeClient(final List<SQLRowValues> copySelectedRows) {

        SwingWorker<Boolean, Object> worker = new SwingWorker<Boolean, Object>() {
            @Override
            protected Boolean doInBackground() throws Exception {

                final SQLTable tableTransfert = getTable().getTable("TR_DEVIS");
                SQLRowValues rowVals = new SQLRowValues(tableTransfert);
                rowVals.put("ID_DEVIS", new SQLRowValues(getTable()).put("NUMERO", null));
                rowVals.put("ID_COMMANDE", null);
                rowVals.put("ID", null);

                final List<Number> lID = new ArrayList<Number>();
                for (SQLRowValues sqlRowValues : copySelectedRows) {
                    lID.add(sqlRowValues.getID());
                }

                SQLRowValuesListFetcher fetch = SQLRowValuesListFetcher.create(rowVals);
                fetch.setSelTransf(new ITransformer<SQLSelect, SQLSelect>() {

                    @Override
                    public SQLSelect transformChecked(SQLSelect input) {
                        Where w = new Where(tableTransfert.getField("ID_DEVIS"), lID);
                        w = w.and(new Where(tableTransfert.getField("ID_COMMANDE_CLIENT"), "IS NOT", (Object) null));
                        input.setWhere(w);
                        return input;
                    }
                });

                List<SQLRowValues> rows = fetch.fetch();
                if (rows != null && rows.size() > 0) {
                    String numero = "";

                    for (SQLRowValues sqlRow : rows) {
                        numero += sqlRow.getForeign("ID_DEVIS").getString("NUMERO") + " ,";
                    }

                    numero = numero.substring(0, numero.length() - 2);
                    String label = "Attention ";
                    if (rows.size() > 1) {
                        label += " les devis " + numero + " ont déjà été transféré en commande!";
                    } else {
                        label += " le devis " + numero + " a déjà été transféré en commande!";
                    }
                    label += "\n Voulez vous continuer?";

                    int ans = JOptionPane.showConfirmDialog(null, label, "Transfert devis en commande", JOptionPane.YES_NO_OPTION);
                    if (ans == JOptionPane.NO_OPTION) {
                        return Boolean.FALSE;
                    }

                }
                return Boolean.TRUE;

            }

            @Override
            protected void done() {

                try {
                    Boolean b = get();
                    if (b != null && b) {
                        TransfertBaseSQLComponent.openTransfertFrame(copySelectedRows, "COMMANDE_CLIENT");
                    }
                } catch (Exception e) {
                    ExceptionHandler.handle("Erreur lors du transfert des devis en commande!", e);
                }
                super.done();
            }
        };
        worker.execute();
    }

    protected List<String> getComboFields() {
        List<String> l = new ArrayList<String>();
        l.add("NUMERO");
        return l;
    }

    private void transfertCommande(final SQLRow row) {
        ComptaPropsConfiguration.getInstanceCompta().getNonInteractiveSQLExecutor().execute(new Runnable() {

            @Override
            public void run() {
                DevisItemSQLElement elt = (DevisItemSQLElement) Configuration.getInstance().getDirectory().getElement("DEVIS_ELEMENT");
                SQLTable tableCmdElt = Configuration.getInstance().getDirectory().getElement("COMMANDE_ELEMENT").getTable();
                SQLElement eltArticle = Configuration.getInstance().getDirectory().getElement("ARTICLE");
                List<SQLRow> rows = row.getReferentRows(elt.getTable());
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

                    // rowArticle.loadAllSafe(rowEltFact);
                    int idArticle = ReferenceArticleSQLElement.getIdForCNM(rowArticle, true);
                    SQLRow rowArticleFind = eltArticle.getTable().getRow(idArticle);
                    if (rowArticleFind != null) {
                        SQLInjector inj = SQLInjector.getInjector(rowArticle.getTable(), tableCmdElt);
                        SQLRowValues rowValsElt = new SQLRowValues(inj.createRowValuesFrom(rowArticleFind));
                        rowValsElt.put("ID_STYLE", sqlRow.getObject("ID_STYLE"));
                        rowValsElt.put("QTE", sqlRow.getObject("QTE"));
                        rowValsElt.put("T_POIDS", rowValsElt.getLong("POIDS") * rowValsElt.getInt("QTE"));
                        rowValsElt.put("T_PA_HT", ((BigDecimal) rowValsElt.getObject("PA_HT")).multiply(new BigDecimal(rowValsElt.getInt("QTE"), DecimalUtils.HIGH_PRECISION)));
                        rowValsElt.put("T_PA_TTC",
                                ((BigDecimal) rowValsElt.getObject("T_PA_HT")).multiply(new BigDecimal(rowValsElt.getForeign("ID_TAXE").getFloat("TAUX") / 100.0 + 1.0), DecimalUtils.HIGH_PRECISION));

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

                        map.add(rowArticleFind.getForeignRow("ID_FOURNISSEUR"), rowValsElt);
                    }
                }
                MouvementStockSQLElement.createCommandeF(map, rowDeviseF);
            }
        });

    }

    protected List<String> getListFields() {
        List<String> l = new ArrayList<String>();
        l.add("NUMERO");
        l.add("DATE");
        l.add("ID_CLIENT");
        l.add("OBJET");
        l.add("ID_COMMERCIAL");
        if (UserRightsManager.getCurrentUserRights().haveRight(AbstractVenteArticleItemTable.SHOW_PRIX_ACHAT_CODE)) {
            l.add("T_HA");
        }
        l.add("T_HT");
        l.add("T_TTC");
        l.add("INFOS");
        if (getTable().contains("DUNNING_DATE")) {
            l.add("DUNNING_DATE");
        }
        return l;
    }

    @Override
    protected synchronized void _initTableSource(final SQLTableModelSource table) {
        super._initTableSource(table);
        final BaseSQLTableModelColumn colAdrLiv = new BaseSQLTableModelColumn("Adresse de livraison", String.class) {

            @Override
            protected Object show_(SQLRowAccessor r) {

                SQLRowAccessor rowAd;
                if (!r.isForeignEmpty("ID_ADRESSE_LIVRAISON")) {
                    rowAd = r.getForeign("ID_ADRESSE_LIVRAISON");
                } else if (!r.getForeign("ID_CLIENT").isForeignEmpty("ID_ADRESSE_L")) {
                    rowAd = r.getForeign("ID_CLIENT").getForeign("ID_ADRESSE_L");
                } else {
                    rowAd = r.getForeign("ID_CLIENT").getForeign("ID_ADRESSE");
                }

                String lib = rowAd.getString("LIBELLE") + " " + rowAd.getString("VILLE");

                return lib;
            }

            @Override
            public Set<FieldPath> getPaths() {
                SQLTable devisTable = getTable();
                Path p = new Path(devisTable);
                p = p.add(devisTable.getField("ID_CLIENT"));
                p = p.add(p.getLast().getField("ID_ADRESSE_L"));

                Path p2 = new Path(devisTable);
                p2 = p2.add(devisTable.getField("ID_CLIENT"));
                p2 = p2.add(p2.getLast().getField("ID_ADRESSE"));

                Path p3 = new Path(devisTable);
                p3 = p3.add(devisTable.getField("ID_ADRESSE_LIVRAISON"));

                return CollectionUtils.createSet(new FieldPath(p, "LIBELLE"), new FieldPath(p, "VILLE"), new FieldPath(p2, "LIBELLE"), new FieldPath(p2, "VILLE"), new FieldPath(p3, "LIBELLE"),
                        new FieldPath(p3, "VILLE"));
            }
        };
        table.getColumns().add(3, colAdrLiv);

    }

    @Override
    public ListMap<String, String> getShowAs() {
        return ListMap.singleton(null, "NUMERO");
    }

    @Override
    protected void _initListRequest(ListSQLRequest req) {
        super._initListRequest(req);
        req.addToGraphToFetch("ID_ETAT_DEVIS");
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

    /*
     * (non-Javadoc)
     * 
     * @see org.openconcerto.devis.SQLElement#getComponent()
     */
    public SQLComponent createComponent() {
        return new DevisSQLComponent(this);
    }

    /**
     * Transfert d'un devis en facture
     * 
     * @param devisID
     */
    public void transfertFacture(final int devisID) {

        SQLElement elt = Configuration.getInstance().getDirectory().getElement("SAISIE_VENTE_FACTURE");
        EditFrame editFactureFrame = new EditFrame(elt);
        editFactureFrame.setIconImage(new ImageIcon(Gestion.class.getResource("frameicon.png")).getImage());

        SaisieVenteFactureSQLComponent comp = (SaisieVenteFactureSQLComponent) editFactureFrame.getSQLComponent();

        comp.setDefaults();
        comp.loadDevis(devisID);


        editFactureFrame.pack();
        editFactureFrame.setState(JFrame.NORMAL);
        editFactureFrame.setVisible(true);
    }

    @Override
    protected Map<String, CustomEditorProvider> _getDefaultCustomEditorProvider(final Configuration configuration, final SQLRowAccessor sqlRow, final String sessionSecurityToken)
            throws IllegalArgumentException {
        final Map<String, CustomEditorProvider> map = super._getDefaultCustomEditorProvider(configuration, sqlRow, sessionSecurityToken);
        map.put("sales.quote.items.list", new CustomEditorProvider() {

            @Override
            public LightUIElement createUIElement(final String id) {

                final ColumnSpec c1 = new ColumnSpec("sales.quote.item.style", StringWithId.class, "Style", new StringWithId(2, "Normal"), true, new LightUIComboBox("sales.quote.item.style"));
                final ColumnSpec c2 = new ColumnSpec("sales.quote.item.code", String.class, "Code", "", true, new LightUITextField("sales.quote.item.code"));
                final ColumnSpec c3 = new ColumnSpec("sales.quote.item.label", String.class, "Nom", "", true, new LightUITextField("sales.quote.item.name"));
                final ColumnSpec c4 = new ColumnSpec("sales.quote.item.description", String.class, "Descriptif", "", true, new LightUITextField("sales.quote.item.description"));
                final ColumnSpec c5 = new ColumnSpec("sales.quote.item.purchase.unit.price", BigDecimal.class, "P.U. Achat HT", new BigDecimal(0), true,
                        new LightUITextField("sales.quote.item.purchase.unit.price"));
                final ColumnSpec c6 = new ColumnSpec("sales.quote.item.sales.unit.price", BigDecimal.class, "P.U. Vente HT", new BigDecimal(0), true,
                        new LightUITextField("sales.quote.item.sales.unit.price"));
                final ColumnSpec c7 = new ColumnSpec("sales.quote.item.quantity", Integer.class, "Quantité", new BigDecimal(1), true, new LightUITextField("sales.quote.item.quantity"));

                final List<ColumnSpec> columnsSpec = new ArrayList<ColumnSpec>();
                final List<String> possibleColumnIds = new ArrayList<String>();

                columnsSpec.add(c1);
                columnsSpec.add(c2);
                columnsSpec.add(c3);
                columnsSpec.add(c4);
                columnsSpec.add(c5);
                columnsSpec.add(c6);
                columnsSpec.add(c7);

                for (ColumnSpec c : columnsSpec) {
                    possibleColumnIds.add(c.getId());
                }

                final String lId = "sales.quote.items";
                final long userId = UserManager.getUserID();
                Document columnsPrefs = null;
                try {
                    final DOMBuilder in = new DOMBuilder();
                    final org.w3c.dom.Document w3cDoc = Configuration.getInstance().getXMLConf(userId, lId);
                    if (w3cDoc != null) {
                        columnsPrefs = in.build(w3cDoc);
                    }
                    if (columnsPrefs == null) {
                        throw new IllegalStateException("Columns Prefs is null");
                    }
                } catch (Exception ex) {
                    throw new IllegalArgumentException(
                            "DevisSQLElement getItemsCustomEditorProvider - Failed to get ColumnPrefs for descriptor " + lId + " and for user " + userId + "\n" + ex.getMessage());
                }

                final Element rootElement = columnsPrefs.getRootElement();
                if (!rootElement.getName().equals("list")) {
                    throw new IllegalArgumentException("invalid xml, roots node list expected but " + rootElement.getName() + " found");
                }
                final List<Element> xmlColumns = rootElement.getChildren();
                final int columnsCount = columnsSpec.size();
                if (xmlColumns.size() != columnsCount) {
                    throw new IllegalArgumentException("incorrect columns count in xml");
                }

                for (int i = 0; i < columnsCount; i++) {
                    final ColumnSpec columnSpec = columnsSpec.get(i);
                    final String columnId = columnSpec.getId();
                    boolean find = false;

                    for (int j = 0; j < columnsCount; j++) {
                        final Element xmlColumn = xmlColumns.get(j);
                        final String xmlColumnId = xmlColumn.getAttribute("id").getValue();

                        if (xmlColumnId.equals(columnId)) {

                            if (!xmlColumn.getName().equals("column")) {
                                throw new IllegalArgumentException("ColumnSpec setPrefs - Invalid xml, element node column expected but " + xmlColumn.getName() + " found");
                            }
                            if (xmlColumn.getAttribute("width") == null || xmlColumn.getAttribute("min-width") == null || xmlColumn.getAttribute("max-width") == null) {
                                throw new IllegalArgumentException("ColumnSpec setPrefs - Invalid column node for " + columnId + ", it must have attribute width, min-width, max-width");
                            }

                            final int width = Integer.parseInt(xmlColumn.getAttribute("width").getValue());
                            final int maxWidth = Integer.parseInt(xmlColumn.getAttribute("max-width").getValue());
                            final int minWidth = Integer.parseInt(xmlColumn.getAttribute("min-width").getValue());

                            columnSpec.setPrefs(width, maxWidth, minWidth);
                            if (i != j) {
                                final ColumnSpec swap = columnsSpec.get(i);
                                columnsSpec.set(i, columnsSpec.get(j));
                                columnsSpec.set(j, swap);
                            }
                            find = true;
                            break;
                        }
                    }
                    if (!find) {
                        throw new IllegalArgumentException("xml contain unknow column: " + columnId);
                    }
                }
                final ColumnsSpec cSpec = new ColumnsSpec(lId, columnsSpec, possibleColumnIds, null);
                cSpec.setAllowMove(true);
                cSpec.setAllowResize(true);
                final RowSelectionSpec selectionSpec = new RowSelectionSpec(id);
                final TableSpec tSpec = new TableSpec(id, selectionSpec, cSpec);
                tSpec.setColumns(cSpec);

                if (sqlRow != null) {
                    // send: id,value
                    final SQLElement elem = configuration.getDirectory().getElement("DEVIS_ELEMENT");
                    final SQLTable table = elem.getTable();
                    final List<String> fieldsToFetch = new ArrayList<String>();
                    for (ColumnSpec cs : columnsSpec) {
                        String colId = cs.getId();
                        SQLField f = configuration.getFieldMapper().getSQLFieldForItem(colId);
                        if (f != null) {
                            fieldsToFetch.add(f.getName());
                        } else {
                            throw new IllegalStateException("No field in " + table + " for column id " + colId);
                        }
                    }

                    final Where where = new Where(table.getField("ID_DEVIS"), "=", sqlRow.getID());
                    final ListSQLRequest req = elem.createListRequest(fieldsToFetch, where, configuration.getShowAs());
                    List<SQLRowValues> fetchedRows = req.getValues();

                    List<Row> rows = new ArrayList<Row>();
                    for (final SQLRowValues vals : fetchedRows) {
                        Row r = new Row(vals.getID(), columnsSpec.size());
                        List<Object> values = new ArrayList<Object>();
                        for (ColumnSpec cs : columnsSpec) {
                            String colId = cs.getId();
                            SQLField f = configuration.getFieldMapper().getSQLFieldForItem(colId);
                            if (f != null) {
                                Object object = vals.getObject(f.getName());
                                System.out.println("DevisSQLElement.getItemsCustomEditorProvider(...).createUIElement()" + f.getName() + ":" + object + ":" + object.getClass().getCanonicalName());
                                if (object instanceof SQLRowValues) {
                                    SQLRowValues sqlRowValues = (SQLRowValues) object;
                                    long rowId = sqlRowValues.getIDNumber().longValue();
                                    List<SQLField> fieldsToExpand = configuration.getShowAs().getFieldExpand(sqlRowValues.getTable());
                                    String strValue = "";
                                    for (SQLField sqlField : fieldsToExpand) {
                                        strValue += sqlRowValues.getObject(sqlField.getName()).toString() + " ";
                                    }
                                    strValue = strValue.trim();
                                    StringWithId str = new StringWithId(rowId, strValue);
                                    object = str;
                                }
                                values.add(object);
                            } else {
                                throw new IllegalStateException("No field in " + table + " for column id " + colId);
                            }
                        }
                        r.setValues(values);
                        rows.add(r);
                    }

                    TableContent tableContent = new TableContent();
                    tableContent.setRows(rows);
                    // tableContent.setSpec(new RowSpec());
                    tSpec.setContent(tableContent);

                }

                final LightUITable eList = new LightUITable(id);
                eList.setTableSpec(tSpec);

                LightUIPanel panel = new LightUIPanel("sales.quote.items.list");
                panel.setGridWidth(1);
                panel.setFillWidth(true);

                LightUILine toolbarLine = new LightUILine();

                LightUIElement b1 = new LightUIElement("up");
                b1.setType(LightUIElement.TYPE_BUTTON_UNMANAGED);
                b1.setGridWidth(1);
                b1.setIcon("up.png");
                panel.addControler(new ActivationOnSelectionControler(id, b1.getId()));
                panel.addControler(new LightControler(LightControler.TYPE_UP, id, b1.getId()));
                toolbarLine.addChild(b1);

                final LightUIElement b2 = new LightUIElement("down");
                b2.setType(LightUIElement.TYPE_BUTTON_UNMANAGED);
                b2.setGridWidth(1);
                b2.setIcon("down.png");
                panel.addControler(new ActivationOnSelectionControler(id, b2.getId()));
                panel.addControler(new LightControler(LightControler.TYPE_DOWN, id, b2.getId()));
                toolbarLine.addChild(b2);
                // Add
                LightUIElement addButton = createButton("add", "Ajouter une ligne");
                panel.addControler(new LightControler(LightControler.TYPE_ADD_DEFAULT, id, addButton.getId()));
                toolbarLine.addChild(addButton);
                // Insert
                LightUIElement insertButton = createButton("insert", "Insérer une ligne");
                panel.addControler(new LightControler(LightControler.TYPE_INSERT_DEFAULT, id, insertButton.getId()));
                toolbarLine.addChild(insertButton);

                // Copy
                LightUIElement copyButton = createButton("copy", "Dupliquer");
                panel.addControler(new ActivationOnSelectionControler(id, copyButton.getId()));
                panel.addControler(new LightControler(LightControler.TYPE_COPY, id, copyButton.getId()));
                toolbarLine.addChild(copyButton);

                // Remove
                LightUIElement removeButton = createButton("remove", "Supprimer");
                panel.addControler(new ActivationOnSelectionControler(id, removeButton.getId()));
                panel.addControler(new LightControler(LightControler.TYPE_REMOVE, id, removeButton.getId()));
                toolbarLine.addChild(removeButton);

                panel.addChild(toolbarLine);
                final LightUILine listLine = new LightUILine();
                listLine.setWeightY(1);
                listLine.setFillHeight(true);
                listLine.addChild(eList);

                //
                panel.addChild(listLine);

                return panel;
            }

            LightUIElement createButton(String id, String label) {
                final LightUIElement b1 = new LightUIElement(id);
                b1.setType(LightUIElement.TYPE_BUTTON_UNMANAGED);
                b1.setGridWidth(1);
                b1.setLabel(label);
                return b1;
            }
        });
        return map;
    }
}
