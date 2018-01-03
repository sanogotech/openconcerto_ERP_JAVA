package org.openconcerto.modules.operation;

import static org.openconcerto.sql.TM.getTM;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.jopencalendar.model.Flag;
import org.jopencalendar.model.JCalendarItem;
import org.jopencalendar.model.JCalendarItemPart;
import org.jopencalendar.ui.DayView;
import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.config.MainFrame;
import org.openconcerto.erp.generationDoc.TemplateManager;
import org.openconcerto.erp.modules.AbstractModule;
import org.openconcerto.erp.modules.ComponentsContext;
import org.openconcerto.erp.modules.DBContext;
import org.openconcerto.erp.modules.MenuContext;
import org.openconcerto.erp.modules.ModuleFactory;
import org.openconcerto.modules.operation.action.LockAction;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.element.SQLElementDirectory;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.utils.SQLCreateTable;
import org.openconcerto.sql.view.list.IListe;
import org.openconcerto.sql.view.list.RowAction;
import org.openconcerto.ui.FrameUtil;
import org.openconcerto.utils.i18n.TranslationManager;

public final class ModuleOperation extends AbstractModule {
    public static final String OPERATIONS_REPORT_TEMPLATE_ID = "Operations Report";
    public static final String OPERATIONS_REPORT_TEMPLATE2_ID = "Operations Report2";
    // -Dmodule.dir=../"Module Operation" -Dgestion.confFile=Configuration/main.properties -ea
    public static final String TABLE_SITE = "SITE";
    public static final String TABLE_OPERATION = "OPERATION";

    public ModuleOperation(ModuleFactory f) throws IOException {
        super(f);
    }

    @Override
    protected void setupElements(SQLElementDirectory dir) {
        final ComptaPropsConfiguration conf = ComptaPropsConfiguration.getInstanceCompta();
        conf.getFieldMapper().addMapperStreamFromClass(ModuleOperation.class);

        dir.addSQLElement(new SiteSQLElement(this));
        dir.addSQLElement(new OperationSQLElement(this));

        Configuration.getInstance().getShowAs().show("SITE", Arrays.asList("NAME"));
        // SQLRequestLog.setEnabled(true);
        // SQLRequestLog.showFrame();
        TemplateManager.getInstance().register(OPERATIONS_REPORT_TEMPLATE_ID);
        TemplateManager.getInstance().register(OPERATIONS_REPORT_TEMPLATE2_ID);
        // Translation loading
        TranslationManager.getInstance().addTranslationStreamFromClass(this.getClass());
    }

    @Override
    protected void setupMenu(MenuContext ctxt) {
        ctxt.addMenuItem(ctxt.createListAction(TABLE_SITE), "operation");
        final SQLElement element = Configuration.getInstance().getDirectory().getElement(TABLE_OPERATION);
        final AbstractAction aOperations = new AbstractAction(getTM().trM("listAction.name", "element", element.getName())) {

            @Override
            public void actionPerformed(ActionEvent e) {
                JFrame f = new JFrame("Gérer les interventions");
                f.setContentPane(new OperationHistoryPanel());
                f.pack();
                f.setLocationRelativeTo(null);
                FrameUtil.showPacked(f);

            }
        };
        ctxt.addMenuItem(aOperations, "operation");

        // final AbstractAction aExport = new
        // AbstractAction(TranslationManager.getInstance().getTranslationForMenu("operation.export"))
        // {
        //
        // @Override
        // public void actionPerformed(ActionEvent e) {
        // JFrame f = new JFrame("Export");
        // f.setContentPane(new OperationExportPanel(new OperationCalendarManager("all")));
        // f.pack();
        // f.setLocationRelativeTo(null);
        // FrameUtil.showPacked(f);
        //
        // }
        // };
        // ctxt.addMenuItem(aExport, "operation");

        final SQLElement elementSite = ctxt.getElement(TABLE_SITE);
        final RowAction editAction = new RowAction(new AbstractAction(TranslationManager.getInstance().getTranslationForMenu("operation.export")) {

            @Override
            public void actionPerformed(ActionEvent e) {
                final List<SQLRowValues> rows = IListe.get(e).getSelectedRows();
                JFrame f = new JFrame("Export");
                f.setContentPane(new OperationExportPanel(new OperationCalendarManager("all"), rows));
                f.pack();
                f.setLocationRelativeTo(null);
                FrameUtil.showPacked(f);
            }
        }, true) {
            @Override
            public boolean enabledFor(IListeEvent evt) {
                return true;
            }

        };
        // editAction.setPredicate(IListeEvent.getNonEmptySelectionPredicate());
        elementSite.getRowActions().add(editAction);

    }

    @Override
    protected void setupComponents(ComponentsContext ctxt) {

    }

    @Override
    protected void install(DBContext ctxt) {
        super.install(ctxt);
        if (ctxt.getRoot().getTable(TABLE_SITE) == null) {
            final SQLCreateTable createTableSite = ctxt.getCreateTable(TABLE_SITE);
            createTableSite.addVarCharColumn("NAME", 500);
            createTableSite.addForeignColumn("CLIENT");
            createTableSite.addForeignColumn("ADRESSE");
            createTableSite.addVarCharColumn("COMMENT", 10000);
            createTableSite.addVarCharColumn("INFO", 10000);
            final SQLCreateTable createTableOperation = ctxt.getCreateTable(TABLE_OPERATION);
            createTableOperation.addForeignColumn(createTableSite);
            createTableOperation.addForeignColumn("ID_USER_COMMON", ctxt.getRoot().findTable("USER_COMMON"));
            createTableOperation.addVarCharColumn("TYPE", 200);
            createTableOperation.addVarCharColumn("STATUS", 200);
            createTableOperation.addForeignColumn("CALENDAR_ITEM_GROUP");
            createTableOperation.addVarCharColumn("DESCRIPTION", 10000);
            createTableOperation.addVarCharColumn("PLANNER_UID", 2048);
            createTableOperation.addVarCharColumn("PLANNER_XML", 2048);
        }
    }

    static OperationCalendarPanel comp;
    static DailyOperationCalendarPanel comp2;

    @Override
    protected void start() {
        ImageIcon icon = new ImageIcon(DayView.class.getResource("auto.png"));
        Flag.register(new Flag("planned", icon, "Planifié", ""));
        icon = new ImageIcon(LockAction.class.getResource("locked.png"));
        Flag.register(new Flag("locked", icon, "Verrouillé", ""));

        comp = new OperationCalendarPanel();
        comp2 = new DailyOperationCalendarPanel();

        MainFrame.getInstance().getTabbedPane().addTab("Planning", comp);
        MainFrame.getInstance().getTabbedPane().addTab("Planning journalier", comp2);
        MainFrame.getInstance().getTabbedPane().addTab("Chantiers", new GanttChantierPanel());
        MainFrame.getInstance().getTabbedPane().setSelectedIndex(1);
    }

    public static void reloadCalendars() {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalAccessError("Must be called in EDT");
        }
        comp.reload();
        comp2.reload();
    }

    @Override
    protected void stop() {
    }

    public static List<Long> getOperationIdsFrom(Set<JCalendarItemDB> toDelete) {
        final List<Long> ids = new ArrayList<Long>();
        for (JCalendarItemDB jCalendarItemDB : toDelete) {
            if (!jCalendarItemDB.getTableSource().equals(ModuleOperation.TABLE_OPERATION)) {
                throw new IllegalStateException("Table is not " + ModuleOperation.TABLE_OPERATION + " but " + jCalendarItemDB.getTableSource());

            }
            final long sourceId = jCalendarItemDB.getSourceId();
            if (!ids.contains(sourceId)) {
                ids.add(sourceId);
            }
        }
        return ids;
    }

    public static Set<JCalendarItemDB> getItemDB(List<JCalendarItemPart> parts) {
        Set<JCalendarItemDB> result = new HashSet<JCalendarItemDB>();
        for (JCalendarItemPart part : parts) {
            JCalendarItem item = part.getItem();
            if (item instanceof JCalendarItemDB) {
                result.add((JCalendarItemDB) item);
            }
        }
        return result;
    }
}
