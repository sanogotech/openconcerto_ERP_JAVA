package org.openconcerto.modules.customerrelationship.lead;

import java.awt.Component;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.sql.SQLException;

import javax.swing.AbstractAction;
import javax.swing.JFrame;

import org.openconcerto.erp.action.CreateFrameAbstractAction;
import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.config.Gestion;
import org.openconcerto.erp.config.MainFrame;
import org.openconcerto.erp.modules.AbstractModule;
import org.openconcerto.erp.modules.ComponentsContext;
import org.openconcerto.erp.modules.DBContext;
import org.openconcerto.erp.modules.MenuContext;
import org.openconcerto.erp.modules.ModuleFactory;
import org.openconcerto.erp.modules.ModuleManager;
import org.openconcerto.erp.modules.ModulePackager;
import org.openconcerto.erp.modules.RuntimeModuleFactory;
import org.openconcerto.modules.customerrelationship.lead.call.CustomerCallSQLElement;
import org.openconcerto.modules.customerrelationship.lead.call.CustomerCallServiceSQLElement;
import org.openconcerto.modules.customerrelationship.lead.call.LeadCallSQLElement;
import org.openconcerto.modules.customerrelationship.lead.call.LeadCallServiceSQLElement;
import org.openconcerto.modules.customerrelationship.lead.importer.LeadImporter;
import org.openconcerto.modules.customerrelationship.lead.visit.CustomerVisitSQLElement;
import org.openconcerto.modules.customerrelationship.lead.visit.CustomerVisitServiceSQLElement;
import org.openconcerto.modules.customerrelationship.lead.visit.LeadVisitSQLElement;
import org.openconcerto.modules.customerrelationship.lead.visit.LeadVisitServiceSQLElement;
import org.openconcerto.openoffice.ContentTypeVersioned;
import org.openconcerto.sql.element.GlobalMapper;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.element.SQLElementDirectory;
import org.openconcerto.sql.model.ConnectionHandlerNoSetup;
import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.SQLDataSource;
import org.openconcerto.sql.model.SQLRequestLog;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.ui.ConnexionPanel;
import org.openconcerto.sql.utils.SQLCreateTable;
import org.openconcerto.sql.utils.SQLUtils;
import org.openconcerto.sql.view.EditFrame;
import org.openconcerto.sql.view.ListeAddPanel;
import org.openconcerto.sql.view.list.IListe;
import org.openconcerto.sql.view.list.IListeAction.IListeEvent;
import org.openconcerto.sql.view.list.RowAction;
import org.openconcerto.ui.FrameUtil;
import org.openconcerto.ui.SwingThreadUtils;
import org.openconcerto.utils.ExceptionHandler;

public final class Module extends AbstractModule {

    public static final String TABLE_LEAD = "LEAD";
    public static final String TABLE_LEAD_CALL = "LEAD_CALL";
    public static final String TABLE_CUSTOMER_CALL = "CUSTOMER_CALL";
    public static final String TABLE_LEAD_VISIT = "LEAD_VISIT";
    public static final String TABLE_CUSTOMER_VISIT = "CUSTOMER_VISIT";
    public static final String TABLE_SERVICE = "SERVICE";
    //
    public static final String TABLE_LEAD_CALL_SERVICE = "LEAD_CALL_SERVICE";
    public static final String TABLE_CUSTOMER_CALL_SERVICE = "CUSTOMER_CALL_SERVICE";
    public static final String TABLE_LEAD_VISIT_SERVICE = "LEAD_VISIT_SERVICE";
    public static final String TABLE_CUSTOMER_VISIT_SERVICE = "CUSTOMER_VISIT_SERVICE";

    public Module(ModuleFactory f) throws IOException {
        super(f);
    }

    @Override
    protected void install(DBContext ctxt) {
        super.install(ctxt);
        // TODO use version to upgrade
        if (ctxt.getRoot().getTable(TABLE_LEAD) == null) {
            final SQLCreateTable createTable = ctxt.getCreateTable(TABLE_LEAD);

            createTable.addVarCharColumn("NUMBER", 20);
            createTable.addDateAndTimeColumn("DATE");
            createTable.addVarCharColumn("FIRSTNAME", 64);
            createTable.addVarCharColumn("NAME", 64);
            createTable.addVarCharColumn("COMPANY", 64);
            //
            createTable.addVarCharColumn("PHONE", 16);
            createTable.addVarCharColumn("MOBILE", 16);
            createTable.addVarCharColumn("FAX", 16);
            createTable.addVarCharColumn("EMAIL", 32);
            createTable.addVarCharColumn("WEBSITE", 64);
            //
            createTable.addVarCharColumn("SOURCE", 200);
            createTable.addVarCharColumn("STATUS", 50);
            //
            createTable.addForeignColumn("ADRESSE");
            createTable.addForeignColumn("COMMERCIAL");
            //
            createTable.addVarCharColumn("INFORMATION", 512);
            //
            createTable.addVarCharColumn("INDUSTRY", 200);
            createTable.addVarCharColumn("RATING", 200);

            createTable.addIntegerColumn("REVENUE", 0);
            createTable.addIntegerColumn("EMPLOYEES", 0);

            createTable.addVarCharColumn("ROLE", 128);
            createTable.addForeignColumn("CLIENT");
            createTable.addVarCharColumn("LOCALISATION", 256);
            createTable.addVarCharColumn("INFOS", 128);
            createTable.addForeignColumn("TITRE_PERSONNEL");
            createTable.addDateAndTimeColumn("REMIND_DATE");
            createTable.addVarCharColumn("APE", 128);
            createTable.addVarCharColumn("SIRET", 256);
            createTable.addVarCharColumn("DISPO", 256);
            createTable.addBooleanColumn("MAILING", Boolean.FALSE, false);

            // V2

            // Appel à un prospect
            final SQLCreateTable createLCall = ctxt.getCreateTable(TABLE_LEAD_CALL);
            createLCall.addDateAndTimeColumn("DATE");
            createLCall.addForeignColumn(createTable);
            createLCall.addVarCharColumn("INFORMATION", 10240);
            createLCall.addDateAndTimeColumn("NEXTCONTACT_DATE");
            createLCall.addVarCharColumn("NEXTCONTACT_INFO", 1024);

            // Appel à un client
            final SQLCreateTable createCCall = ctxt.getCreateTable(TABLE_CUSTOMER_CALL);
            createCCall.addDateAndTimeColumn("DATE");
            createCCall.addForeignColumn("CLIENT");
            createCCall.addVarCharColumn("INFORMATION", 10240);
            createCCall.addDateAndTimeColumn("NEXTCONTACT_DATE");
            createCCall.addVarCharColumn("NEXTCONTACT_INFO", 1024);

            // Visites chez un prospect
            final SQLCreateTable createLV = ctxt.getCreateTable(TABLE_LEAD_VISIT);
            createLV.addDateAndTimeColumn("DATE");
            createLV.addForeignColumn(createTable);
            createLV.addVarCharColumn("INFORMATION", 10240);
            createLV.addDateAndTimeColumn("NEXTCONTACT_DATE");
            createLV.addVarCharColumn("NEXTCONTACT_INFO", 1024);
            // Visites chez un client
            final SQLCreateTable createCV = ctxt.getCreateTable(TABLE_CUSTOMER_VISIT);
            createCV.addDateAndTimeColumn("DATE");
            createCV.addForeignColumn("CLIENT");
            createCV.addVarCharColumn("INFORMATION", 10240);
            createCV.addDateAndTimeColumn("NEXTCONTACT_DATE");
            createCV.addVarCharColumn("NEXTCONTACT_INFO", 1024);

            // Services
            final SQLCreateTable createService = ctxt.getCreateTable(TABLE_SERVICE);
            createService.addVarCharColumn("NAME", 256);

            //
            if (ctxt.getRoot().getTable(TABLE_LEAD_CALL_SERVICE) == null) {
                final SQLCreateTable createTable1 = ctxt.getCreateTable(TABLE_LEAD_CALL_SERVICE);
                createTable1.addForeignColumn(createLCall);
                createTable1.addForeignColumn(createService);
            }
            if (ctxt.getRoot().getTable(TABLE_CUSTOMER_CALL_SERVICE) == null) {
                final SQLCreateTable createTable1 = ctxt.getCreateTable(TABLE_CUSTOMER_CALL_SERVICE);
                createTable1.addForeignColumn(createCCall);
                createTable1.addForeignColumn(createService);
            }
            if (ctxt.getRoot().getTable(TABLE_LEAD_VISIT_SERVICE) == null) {
                final SQLCreateTable createTable1 = ctxt.getCreateTable(TABLE_LEAD_VISIT_SERVICE);
                createTable1.addForeignColumn(createLV);
                createTable1.addForeignColumn(createService);
            }
            if (ctxt.getRoot().getTable(TABLE_CUSTOMER_VISIT_SERVICE) == null) {
                final SQLCreateTable createTable1 = ctxt.getCreateTable(TABLE_CUSTOMER_VISIT_SERVICE);
                createTable1.addForeignColumn(createCV);
                createTable1.addForeignColumn(createService);
            }
        }
    }

    @Override
    protected void setupElements(final SQLElementDirectory dir) {
        super.setupElements(dir);

        final LeadSQLElement element = new LeadSQLElement(this);
        GlobalMapper.getInstance().map(element.getCode() + ".default", new LeadGroup());
        dir.addSQLElement(element);

        dir.addSQLElement(new LeadCallSQLElement(this));
        dir.addSQLElement(new CustomerCallSQLElement(this));
        dir.addSQLElement(new LeadVisitSQLElement(this));
        dir.addSQLElement(new CustomerVisitSQLElement(this));
        dir.addSQLElement(new ServiceSQLElement(this));
        // Services
        dir.addSQLElement(new LeadCallServiceSQLElement(this));
        dir.addSQLElement(new CustomerCallServiceSQLElement(this));
        dir.addSQLElement(new LeadVisitServiceSQLElement(this));
        dir.addSQLElement(new CustomerVisitServiceSQLElement(this));

        // Call
        final RowAction.PredicateRowAction addCallAction = new RowAction.PredicateRowAction(new AbstractAction("Appeler") {

            @Override
            public void actionPerformed(ActionEvent e) {
                SQLRow sRow = IListe.get(e).getSelectedRow().asRow();
                final SQLElement eCall = dir.getElement(Module.TABLE_CUSTOMER_CALL);
                final SQLTable table = eCall.getTable();
                EditFrame editFrame = new EditFrame(eCall);
                final SQLRowValues sqlRowValues = new SQLRowValues(table);
                sqlRowValues.put("ID_CLIENT", sRow.getIDNumber());
                editFrame.getSQLComponent().select(sqlRowValues);
                FrameUtil.show(editFrame);
            }
        }, true) {
        };
        addCallAction.setPredicate(IListeEvent.getSingleSelectionPredicate());
        dir.getElement("CLIENT").getRowActions().add(addCallAction);
        // Visit
        final RowAction.PredicateRowAction addVisitAction = new RowAction.PredicateRowAction(new AbstractAction("Enregister une visite") {

            @Override
            public void actionPerformed(ActionEvent e) {
                SQLRow sRow = IListe.get(e).getSelectedRow().asRow();
                final SQLElement eCall = dir.getElement(Module.TABLE_CUSTOMER_VISIT);
                final SQLTable table = eCall.getTable();
                EditFrame editFrame = new EditFrame(eCall);
                final SQLRowValues sqlRowValues = new SQLRowValues(table);
                sqlRowValues.put("ID_CLIENT", sRow.getIDNumber());
                editFrame.getSQLComponent().select(sqlRowValues);
                FrameUtil.show(editFrame);
            }
        }, true) {
        };
        addVisitAction.setPredicate(IListeEvent.getSingleSelectionPredicate());
        dir.getElement("CLIENT").getRowActions().add(addVisitAction);

    }

    @Override
    protected void setupComponents(ComponentsContext ctxt) {

    }

    @Override
    protected void setupMenu(final MenuContext ctxt) {
        ctxt.addMenuItem(new LeadListAction(), MainFrame.LIST_MENU);

        ctxt.addMenuItem(new CreateFrameAbstractAction("Liste des services") {
            @Override
            public JFrame createFrame() {
                final JFrame frame = new JFrame("Services proposés");
                frame.setContentPane(new ListeAddPanel(ctxt.getElement(TABLE_SERVICE)));
                return frame;
            }
        }, MainFrame.STRUCTURE_MENU);
        ctxt.addMenuItem(new SuiviClientProspectListAction(), MainFrame.LIST_MENU);
        // ctxt.addMenuItem(ctxt.createListAction(TABLE_CUSTOMER_CALL), MainFrame.LIST_MENU);
        // ctxt.addMenuItem(ctxt.createListAction(TABLE_CUSTOMER_VISIT), MainFrame.LIST_MENU);
        // ctxt.addMenuItem(ctxt.createListAction(TABLE_LEAD_CALL), MainFrame.LIST_MENU);
        // ctxt.addMenuItem(ctxt.createListAction(TABLE_LEAD_VISIT), MainFrame.LIST_MENU);

        ctxt.addMenuItem(new AbstractAction("Import de prospects") {
            @Override
            public void actionPerformed(ActionEvent e) {
                final Frame frame = SwingThreadUtils.getAncestorOrSelf(Frame.class, (Component) e.getSource());
                final FileDialog fd = new FileDialog(frame, "Import de prospects", FileDialog.LOAD);
                fd.setFilenameFilter(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        return name.endsWith("." + ContentTypeVersioned.SPREADSHEET.getExtension());
                    }
                });
                fd.setVisible(true);
                if (fd.getFile() != null) {
                    final DBRoot rootSociete = ((ComptaPropsConfiguration) ComptaPropsConfiguration.getInstance()).getRootSociete();
                    try {
                        SQLUtils.executeAtomic(rootSociete.getDBSystemRoot().getDataSource(), new ConnectionHandlerNoSetup<Object, IOException>() {
                            @Override
                            public Object handle(final SQLDataSource ds) throws SQLException, IOException {
                                File f = new File(fd.getDirectory());

                                LeadImporter imp = new LeadImporter();
                                try {
                                    imp.exportLead(rootSociete, f, new File(fd.getDirectory(), fd.getFile()));
                                    imp.importFromFile(f, rootSociete);
                                } catch (Exception exn) {
                                    // TODO Bloc catch auto-généré
                                    exn.printStackTrace();
                                }
                                return null;
                            }
                        });
                    } catch (IOException exn) {
                        ExceptionHandler.handle(frame, "Erreur lors de la lecture du fichier", exn);
                    } catch (SQLException exn) {
                        ExceptionHandler.handle(frame, "Erreur lors de l'insertion dans la base", exn);
                    }
                }
            }
        }, MainFrame.FILE_MENU);
    }

    @Override
    protected void start() {
        new LeadCustomerSQLInjector();
        new LeadContactSQLInjector();
    }

    @Override
    protected void stop() {
    }

    public static void main(String[] args) throws IOException {
        System.setProperty(ConnexionPanel.QUICK_LOGIN, "true");
        final File propsFile = new File("module.properties");
        System.out.println(propsFile.getAbsolutePath());
        final ModuleFactory factory = new RuntimeModuleFactory(propsFile);
        SQLRequestLog.setEnabled(true);
        SQLRequestLog.showFrame();
        // uncomment to create and use the jar
        final ModulePackager modulePackager = new ModulePackager(propsFile, new File("bin/"));
        modulePackager.writeToDir(new File("../OpenConcerto/Modules"));
        // final ModuleFactory factory = new JarModuleFactory(jar);
        ModuleManager.getInstance().addFactories(new File("../OpenConcerto/Modules"));
        ModuleManager.getInstance().addFactoryAndStart(factory, false);
        Gestion.main(args);
    }

}
