package org.openconcerto.modules.reports.olap;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

import org.olap4j.metadata.MetadataElement;

public class MetadataTransferable implements Transferable {
    private MetadataElement element;

    MetadataTransferable(MetadataElement element) {
        this.element = element;
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        final DataFlavor[] result = new DataFlavor[2];
        result[0] = getDataFlavor();
        result[1] = DataFlavor.stringFlavor;
        return result;
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return getDataFlavor().equals(flavor) || DataFlavor.stringFlavor.equals(flavor);
    }

    public static DataFlavor getDataFlavor() {

        final String mimeType = DataFlavor.javaJVMLocalObjectMimeType + ";class=" + MetadataElement.class.getName();
        try {
            return new DataFlavor(mimeType, null, MetadataElement.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
        if (getDataFlavor().equals(flavor))
            return element;
        else if (DataFlavor.stringFlavor.equals(flavor)) {
            return element.getName();
        } else
            throw new UnsupportedFlavorException(flavor);
    }

    public MetadataElement getElement() {
        return element;
    }
}
