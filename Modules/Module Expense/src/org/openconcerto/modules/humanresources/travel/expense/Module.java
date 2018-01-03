package org.openconcerto.modules.humanresources.travel.expense;

import java.io.File;
import java.io.IOException;

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
import org.openconcerto.sql.element.GlobalMapper;
import org.openconcerto.sql.element.SQLElementDirectory;
import org.openconcerto.sql.model.SQLRequestLog;
import org.openconcerto.sql.ui.ConnexionPanel;
import org.openconcerto.sql.utils.SQLCreateTable;

public final class Module extends AbstractModule {

    public static final String TABLE_EXPENSE = "EXPENSE";
    public static final String TABLE_EXPENSE_STATE = "EXPENSE_STATE";

    public Module(ModuleFactory f) throws IOException {
        super(f);
    }

    @Override
    protected void install(DBContext ctxt) {
        // TODO use version to upgrade
        if (!ctxt.getTablesPreviouslyCreated().contains(TABLE_EXPENSE)) {
            final SQLCreateTable createTableState = ctxt.getCreateTable(TABLE_EXPENSE_STATE);
            createTableState.addVarCharColumn("NAME", 128);

            final SQLCreateTable createTable = ctxt.getCreateTable(TABLE_EXPENSE);
            createTable.addForeignColumn("ID_USER_COMMON", ctxt.getRoot().findTable("USER_COMMON"));
            createTable.addDateAndTimeColumn("DATE");
            createTable.addVarCharColumn("DESCRIPTION", 1024);
            createTable.addNumberColumn("TRAVEL_DISTANCE", Float.class, 0f, false);
            createTable.addNumberColumn("TRAVEL_RATE", Float.class, 0f, false);
            createTable.addLongColumn("TRAVEL_AMOUNT", 0L, false);
            createTable.addLongColumn("MISC_AMOUNT", 0L, false);
            createTable.addForeignColumn(createTableState);
        }
    }

    @Override
    protected void setupElements(SQLElementDirectory dir) {
        super.setupElements(dir);
        GlobalMapper.getInstance().map(ExpenseSQLElement.ELEMENT_CODE + ".default", new ExpenseGroup());
        GlobalMapper.getInstance().map(ExpenseStateSQLElement.ELEMENT_CODE + ".default", new ExpenseStateGroup());
        dir.addSQLElement(new ExpenseSQLElement());
        dir.addSQLElement(new ExpenseStateSQLElement());
    }

    @Override
    protected void setupComponents(ComponentsContext ctxt) {

    }

    @Override
    protected void setupMenu(MenuContext ctxt) {
        ctxt.addMenuItem(ctxt.createListAction(TABLE_EXPENSE), MainFrame.LIST_MENU);
    }

    @Override
    protected void start() {
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
