package org.openconcerto.modules.reports.olap;

import java.io.File;
import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.config.Gestion;
import org.openconcerto.erp.config.MainFrame;
import org.openconcerto.erp.modules.AbstractModule;
import org.openconcerto.erp.modules.ComponentsContext;
import org.openconcerto.erp.modules.DBContext;
import org.openconcerto.erp.modules.ModuleFactory;
import org.openconcerto.erp.modules.ModuleManager;
import org.openconcerto.erp.modules.ModulePackager;
import org.openconcerto.sql.element.SQLElementDirectory;
import org.openconcerto.sql.model.SQLRequestLog;
import org.openconcerto.sql.model.SQLSystem;
import org.openconcerto.sql.ui.ConnexionPanel;
import org.openconcerto.utils.FileUtils;

public final class Module extends AbstractModule {

    public Module(ModuleFactory f) throws IOException {
        super(f);

    }

    @Override
    protected void install(DBContext ctxt) {

    }

    @Override
    protected void setupElements(SQLElementDirectory dir) {

    }

    @Override
    protected void setupComponents(ComponentsContext ctxt) {

        // ctxt.addMenuItem(new SQLElementListAction(ctxt.getElement(TABLE_NAME)) {
        //
        // @Override
        // protected void initFrame(IListFrame f) {
        // super.initFrame(f);
        // f.getPanel().setAddVisible(false);
        // }
        //
        // }, MainFrame.LIST_MENU);
        // ctxt.addListAction("CLIENT", new CallActionFactory());

    }

    @Override
    protected void start() {
        try {

            Class.forName("mondrian.olap4j.MondrianOlap4jDriver");
            System.setProperty("mondrian.rolap.star.disableCaching", "true");
            System.setProperty("mondrian.expCache.enable", "false");
            System.setProperty("mondrian.rolap.RolapResult.flushAfterEachQuery", "true");
            System.setProperty("mondrian.rolap.EnableRolapCubeMemberCache", "false");
            System.setProperty("mondrian.rolap.generate.formatted.sql", "true");
            if (ComptaPropsConfiguration.getInstance().getRoot().getServer().getSQLSystem() != SQLSystem.POSTGRESQL) {
                JOptionPane.showMessageDialog(new JFrame(), "The Business Intelligence module requires a PostgreSQL database");
            } else {
                MainFrame.getInstance().getTabbedPane().addTab("Business Intelligence", new OLAPPanel());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void stop() {
    }

    public static void main(String[] args) throws IOException {
        System.setProperty(ConnexionPanel.QUICK_LOGIN, "true");
        final File propsFile = new File("module.properties");

        final File distDir = new File("dist");
        FileUtils.mkdir_p(distDir);
        final ModulePackager modulePackager = new ModulePackager(propsFile, new File("bin/"));
        modulePackager.setSkipDuplicateFiles(true);
        modulePackager.addJarsFromDir(new File("lib"));
        modulePackager.writeToDir(distDir);
        modulePackager.writeToDir(new File("../OpenConcerto/Modules"));
        SQLRequestLog.setEnabled(true);
        SQLRequestLog.showFrame();

        ModuleManager.getInstance().addFactories(new File("../OpenConcerto/Modules"));
        Gestion.main(args);
    }
}
