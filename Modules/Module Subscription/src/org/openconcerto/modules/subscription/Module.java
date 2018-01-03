package org.openconcerto.modules.subscription;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.Calendar;

import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.openconcerto.erp.action.CreateFrameAbstractAction;
import org.openconcerto.erp.config.MainFrame;
import org.openconcerto.erp.core.common.element.NumerotationAutoSQLElement;
import org.openconcerto.erp.core.sales.invoice.element.SaisieVenteFactureSQLElement;
import org.openconcerto.erp.core.sales.invoice.element.SaisieVenteFactureSQLElement.DoWithRow;
import org.openconcerto.erp.modules.AbstractModule;
import org.openconcerto.erp.modules.AlterTableRestricted;
import org.openconcerto.erp.modules.ComponentsContext;
import org.openconcerto.erp.modules.DBContext;
import org.openconcerto.erp.modules.MenuContext;
import org.openconcerto.erp.modules.ModuleFactory;
import org.openconcerto.modules.subscription.element.SubscriptionSQLComponent;
import org.openconcerto.modules.subscription.element.SubscriptionSQLElement;
import org.openconcerto.modules.subscription.panel.BonCommandeAboPanel;
import org.openconcerto.modules.subscription.panel.DevisAboPanel;
import org.openconcerto.modules.subscription.panel.FacturesAboPanel;
import org.openconcerto.modules.subscription.panel.HistoriqueAbonnement;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.element.SQLElementDirectory;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.utils.SQLCreateTable;
import org.openconcerto.sql.view.EditFrame;
import org.openconcerto.sql.view.EditPanel.EditMode;
import org.openconcerto.sql.view.IListFrame;
import org.openconcerto.sql.view.ListeAddPanel;
import org.openconcerto.sql.view.list.IListe;
import org.openconcerto.sql.view.list.IListeAction.IListeEvent;
import org.openconcerto.sql.view.list.RowAction.PredicateRowAction;
import org.openconcerto.utils.i18n.TranslationManager;

public final class Module extends AbstractModule {

    public Module(ModuleFactory f) throws IOException {
        super(f);

    }

    @Override
    protected void install(DBContext ctxt) {
        super.install(ctxt);

        if (ctxt.getLastInstalledVersion() == null) {

            if (ctxt.getRoot().getTable("ABONNEMENT") == null) {

                // ABONNEMENT
                final SQLCreateTable createTable = ctxt.getCreateTable("ABONNEMENT");
                createTable.addVarCharColumn("NUMERO", 256);
                createTable.addVarCharColumn("DESCRIPTION", 1024);
                createTable.addVarCharColumn("NOM", 256);
                createTable.addVarCharColumn("INTITULE_FACTURE", 1024);
                createTable.addVarCharColumn("INFOS", 2048);
                createTable.addColumn("DATE", "date");
                createTable.addForeignColumn("ID_CLIENT", ctxt.getRoot().getTable("CLIENT"));
                createTable.addForeignColumn("ID_SAISIE_VENTE_FACTURE", ctxt.getRoot().getTable("SAISIE_VENTE_FACTURE"));
                createTable.addForeignColumn("ID_DEVIS", ctxt.getRoot().getTable("DEVIS"));
                createTable.addColumn("DATE_DEBUT_FACTURE", "date");
                createTable.addColumn("DATE_FIN_FACTURE", "date");
                createTable.addColumn("DATE_DEBUT_DEVIS", "date");
                createTable.addColumn("DATE_FIN_DEVIS", "date");
                createTable.addColumn("NB_MOIS_DEVIS", "int DEFAULT 3");
                createTable.addColumn("NB_MOIS_FACTURE", "int DEFAULT 3");
                createTable.addColumn("CREATE_FACTURE", "boolean DEFAULT false");
                createTable.addColumn("CREATE_DEVIS", "boolean DEFAULT false");

                createTable.addForeignColumn("ID_COMMANDE_CLIENT", ctxt.getRoot().getTable("COMMANDE_CLIENT"));
                createTable.addColumn("DATE_DEBUT_COMMANDE", "date");
                createTable.addColumn("DATE_FIN_COMMANDE", "date");
                createTable.addColumn("NB_MOIS_COMMANDE", "int DEFAULT 3");
                createTable.addColumn("CREATE_COMMANDE", "boolean DEFAULT false");

                // ctxt.manipulateData(new IClosure<DBRoot>() {
                // @Override
                // public void executeChecked(DBRoot input) {
                // // Undefined Affaire
                // SQLRowValues rowVals = new SQLRowValues(input.getTable("ABONNEMENT"));
                // try {
                // rowVals.insert();
                // } catch (SQLException exn) {
                // // TODO Bloc catch auto-généré
                // exn.printStackTrace();
                // }
                //
                // }
                // });

                AlterTableRestricted alterNumero = ctxt.getAlterTable("NUMEROTATION_AUTO");
                alterNumero.addVarCharColumn("ABONNEMENT_FORMAT", 128);
                alterNumero.addIntegerColumn("ABONNEMENT_START", 1);

                AlterTableRestricted alterFact = ctxt.getAlterTable("SAISIE_VENTE_FACTURE");
                alterFact.addForeignColumn("ID_ABONNEMENT", createTable);
                alterFact.addColumn("CREATION_AUTO_VALIDER", "boolean DEFAULT false");

                AlterTableRestricted alterDevis = ctxt.getAlterTable("COMMANDE_CLIENT");
                alterDevis.addForeignColumn("ID_ABONNEMENT", createTable);
                alterDevis.addColumn("CREATION_AUTO_VALIDER", "boolean DEFAULT false");

                AlterTableRestricted alterCmd = ctxt.getAlterTable("DEVIS");
                alterCmd.addForeignColumn("ID_ABONNEMENT", createTable);
                alterCmd.addColumn("CREATION_AUTO_VALIDER", "boolean DEFAULT false");

            }

        }
    }

    @Override
    protected void setupElements(final SQLElementDirectory dir) {
        super.setupElements(dir);
        TranslationManager.getInstance().addTranslationStreamFromClass(this.getClass());
        dir.addSQLElement(SubscriptionSQLElement.class);
        NumerotationAutoSQLElement.addClass(SubscriptionSQLElement.class, "ABONNEMENT");
        ((SaisieVenteFactureSQLElement) dir.getElement("SAISIE_VENTE_FACTURE")).putSpecialAction("subscription.autocreate", new DoWithRow() {
            @Override
            public void process(SQLRow row) {
                createAbonnement(row);
            }
        });
        PredicateRowAction action = new PredicateRowAction(new AbstractAction("Créer un abonnement") {

            @Override
            public void actionPerformed(ActionEvent e) {
                final SQLRowAccessor r = IListe.get(e).getSelectedRow();
                createAbonnement(r);

            }
        }, true);
        action.setPredicate(IListeEvent.getSingleSelectionPredicate());
        dir.getElement("SAISIE_VENTE_FACTURE").getRowActions().add(action);
        NumerotationAutoSQLElement.addClass(SubscriptionSQLElement.class, "ABONNEMENT");

    }

    private void createAbonnement(SQLRowAccessor r) {

        SQLElement aboElt = Configuration.getInstance().getDirectory().getElement("ABONNEMENT");
        if (r.getReferentRows(aboElt.getTable()).size() == 0) {
            final SubscriptionSQLComponent createComponent = (SubscriptionSQLComponent) aboElt.createComponent(SQLElement.DEFAULT_COMP_ID);
            EditFrame frame = new EditFrame(createComponent, EditMode.CREATION);
            SQLRowValues rowVals = new SQLRowValues(aboElt.getTable());
            SQLRowAccessor rowClient = r.getForeign("ID_CLIENT");
            rowVals.put("ID_CLIENT", rowClient.getID());
            rowVals.put("ID_SAISIE_VENTE_FACTURE", r.getID());
            rowVals.put("CREATE_FACTURE", Boolean.TRUE);
            rowVals.put("NOM", "Abonnement");
            rowVals.put("INTITULE_FACTURE", r.getString("NOM"));
            Calendar c = Calendar.getInstance();
            rowVals.put("DATE_DEBUT_FACTURE", c.getTime());
            c.add(Calendar.YEAR, 2);
            rowVals.put("NB_MOIS_FACTURE", 12);

            createComponent.setLightUI(false);

            createComponent.select(rowVals);
            frame.setVisible(true);
        } else {
            JOptionPane.showMessageDialog(null, "Cette facture est déjà associée à un abonnement!");
        }
    }

    @Override
    protected void setupComponents(final ComponentsContext ctxt) {

    }

    @Override
    protected void setupMenu(final MenuContext ctxt) {
        ctxt.addMenuItem(new CreateFrameAbstractAction("Liste des abonnements") {
            @Override
            public JFrame createFrame() {
                IListFrame frame = new IListFrame(new ListeAddPanel(ctxt.getElement("ABONNEMENT")));
                return frame;
            }
        }, MainFrame.LIST_MENU);

        ctxt.addMenuItem(new CreateFrameAbstractAction("Saisie abonnements") {
            @Override
            public JFrame createFrame() {
                return new EditFrame(ctxt.getElement("ABONNEMENT"));
            }
        }, MainFrame.CREATE_MENU);

        ctxt.addMenuItem(new CreateFrameAbstractAction("Historique des abonnements") {
            @Override
            public JFrame createFrame() {
                HistoriqueAbonnement histo = new HistoriqueAbonnement();

                return histo.getFrame();
            }
        }, MainFrame.LIST_MENU);
    }

    @Override
    protected void start() {
        MainFrame.getInstance().getTabbedPane().addTab("Devis d'abonnements", new DevisAboPanel());
        MainFrame.getInstance().getTabbedPane().addTab("Bon de commande d'abonnements", new BonCommandeAboPanel());
        MainFrame.getInstance().getTabbedPane().addTab("Facture d'abonnements", new FacturesAboPanel());
        SourceCellValueProvider.register();
    }

    @Override
    protected void stop() {
    }
}
