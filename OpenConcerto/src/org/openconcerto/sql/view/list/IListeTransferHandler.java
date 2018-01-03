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
 
 package org.openconcerto.sql.view.list;

import org.openconcerto.ui.SwingThreadUtils;
import org.openconcerto.utils.ExceptionHandler;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.TransferHandler;

public class IListeTransferHandler extends TransferHandler {
    static private final class Data {
        private final IListe list;
        private final List<Number> selection;

        private Data(IListe list, List<? extends Number> selection) {
            super();
            this.list = list;
            this.selection = new ArrayList<Number>(selection);
        }
    }

    static private final DataFlavor FLAVOR = new DataFlavor(Data.class, "List of rows");

    @Override
    public int getSourceActions(JComponent c) {
        return MOVE;
    }

    @Override
    protected Transferable createTransferable(JComponent c) {
        final IListe list = SwingThreadUtils.getAncestorOrSelf(IListe.class, c);
        // return null right away to prevent the displaying of the drag UI
        if (list.isSorted() || !list.getModel().canOrder())
            return null;
        final List<Integer> selection = list.getSelection().getSelectedIDs();
        return new Transferable() {

            @Override
            public DataFlavor[] getTransferDataFlavors() {
                return new DataFlavor[] { FLAVOR };
            }

            @Override
            public boolean isDataFlavorSupported(DataFlavor flavor) {
                return FLAVOR.equals(flavor);
            }

            @Override
            public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
                if (!this.isDataFlavorSupported(flavor))
                    throw new UnsupportedFlavorException(flavor);
                return new Data(list, selection);
            }
        };
    }

    @Override
    protected void exportDone(JComponent c, Transferable t, int action) {
    }

    @Override
    public boolean canImport(TransferSupport support) {
        final IListe targetList = SwingThreadUtils.getAncestorOrSelf(IListe.class, support.getComponent());
        return support.isDataFlavorSupported(FLAVOR) && targetList == getData(support).list;
    }

    private Data getData(TransferSupport support) {
        try {
            return (Data) support.getTransferable().getTransferData(FLAVOR);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public boolean importData(TransferSupport support) {
        if (!this.canImport(support))
            return false;
        final Data data = getData(support);
        // the user has made an action, if it cannot be carried he should be notified
        try {
            data.list.getModel().moveTo(data.selection, ((javax.swing.JTable.DropLocation) support.getDropLocation()).getRow(), false);
            return true;
        } catch (Exception e) {
            ExceptionHandler.handle(data.list, "Couldn't move", e);
            return false;
        }
    }
}
