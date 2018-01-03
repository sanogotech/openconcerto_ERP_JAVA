package org.openconcerto.modules.reports.olap;
import java.awt.datatransfer.Transferable;

import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.TransferHandler;

import org.olap4j.metadata.MetadataElement;

public class MetadataTransfertHandler extends TransferHandler {
    @Override
    protected Transferable createTransferable(JComponent c) {
        System.out.println("OLAParametersPanel.OLAParametersPanel(...).new TransferHandler() {...}.createTransferable():" + c + " c:" + c.getClass().getName());
        JTree tree = (JTree) c;
        MetadataElement element = (MetadataElement) tree.getSelectionPath().getLastPathComponent();

        return new MetadataTransferable(element);
    }

    @Override
    public int getSourceActions(JComponent c) {
        return TransferHandler.COPY;

    }
}
