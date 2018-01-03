package org.openconcerto.modules.common.batchprocessing;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JFrame;

import org.openconcerto.erp.config.Gestion;
import org.openconcerto.erp.modules.AbstractModule;
import org.openconcerto.erp.modules.ComponentsContext;
import org.openconcerto.erp.modules.ModuleFactory;
import org.openconcerto.erp.modules.ModuleManager;
import org.openconcerto.erp.modules.ModulePackager;
import org.openconcerto.erp.modules.RuntimeModuleFactory;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRequestLog;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.ui.ConnexionPanel;
import org.openconcerto.sql.view.list.IListe;
import org.openconcerto.sql.view.list.IListeAction.IListeEvent;
import org.openconcerto.sql.view.list.RowAction;
import org.openconcerto.ui.FrameUtil;

public final class Module extends AbstractModule {

    public Module(ModuleFactory f) throws IOException {
        super(f);
    }

    @Override
    protected void setupComponents(ComponentsContext ctxt) {

        super.setupComponents(ctxt);
        final SQLElement element = ctxt.getElement("ARTICLE");
        final RowAction.PredicateRowAction editAction = new RowAction.PredicateRowAction(new AbstractAction("Traitement par lot...") {

            @Override
            public void actionPerformed(ActionEvent e) {
                final List<SQLRowValues> rows = IListe.get(e).getSelectedRows();
                final int size = rows.size();
                if (size > 0) {
                    final JFrame f = new JFrame();
                    if (size > 1) {
                        f.setTitle("Traitement des " + size + " " + element.getPluralName());
                    } else {
                        f.setTitle("Traitement d'" + element.getSingularName());
                    }
                    FieldFilter filter = new FieldFilter() {

                        @Override
                        public boolean isFiltered(SQLField f) {
                            final String fieldName = f.getName();
                            return fieldName.contains("METRIQUE") || fieldName.equals("ID_STOCK");
                        }

                    };

                    f.setContentPane(new BatchEditorPanel(rows, filter));
                    f.pack();
                    f.setMinimumSize(new Dimension(400, 300));
                    f.setLocationRelativeTo(IListe.get(e));
                    FrameUtil.show(f);
                }
            }
        }, false) {
        };
        editAction.setPredicate(IListeEvent.getNonEmptySelectionPredicate());
        element.getRowActions().add(editAction);

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
