package org.openconcerto.modules.ocr;

import java.awt.event.ActionEvent;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.JFrame;

import org.openconcerto.erp.modules.AbstractModule;
import org.openconcerto.erp.modules.MenuContext;
import org.openconcerto.erp.modules.ModuleFactory;
import org.openconcerto.modules.ocr.scan.ScannerPanel;
import org.openconcerto.ui.FrameUtil;

public class Module extends AbstractModule {

    public Module(ModuleFactory f) throws IOException {
        super(f);
    }

    @Override
    protected void setupMenu(MenuContext menuContext) {
        super.setupMenu(menuContext);
        menuContext.addMenuItem(new AbstractAction("Factures fournisseur") {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.err.println("Module.setupMenu(...).new AbstractAction() {...}.actionPerformed()" + System.getProperty("java.version"));
                final JFrame frame = new JFrame("Source d'acquisition");
                frame.setContentPane(new ScannerPanel("."));
                FrameUtil.showPacked(frame);
                frame.setLocationRelativeTo(null);
            }
        }, "menu.ocr");
    }

    @Override
    protected void start() {
    }

    @Override
    protected void stop() {

    }

}
