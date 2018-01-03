package org.openconcerto.modules.operation;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JList;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;

import org.openconcerto.ui.list.CheckListItem;
import org.openconcerto.ui.list.CheckListRenderer;

public class CheckList<T> extends JList {

    public CheckList(ListModel statesModel) {
        super(statesModel);
        setCellRenderer(new CheckListRenderer());
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent event) {
                final JList list = (JList) event.getSource();
                final int index = list.locationToIndex(event.getPoint());
                if (index >= 0 && index < list.getModel().getSize()) {
                    final CheckListItem item = (CheckListItem) list.getModel().getElementAt(index);
                    if (event.getClickCount() == 1) {
                        item.setSelected(!item.isSelected());
                        list.repaint(list.getCellBounds(index, index));
                    } else {
                        final boolean newStat = item.isSelected();
                        final ListModel model = getModel();
                        final int size = model.getSize();
                        for (int i = 0; i < size; i++) {
                            final CheckListItem cItem = (CheckListItem) model.getElementAt(i);
                            cItem.setSelected(newStat);
                        }
                        list.repaint();

                    }
                    firePropertyChange("checked", null, item);
                }
            }

        });
    }

    public boolean isAllSelected() {
        final ListModel model = getModel();
        final int size = model.getSize();
        for (int i = 0; i < size; i++) {
            final CheckListItem item = (CheckListItem) model.getElementAt(i);
            if (!item.isSelected()) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    public List<T> getSelectedObjects() {
        final List<T> result = new ArrayList<T>();
        final ListModel model = getModel();
        final int size = model.getSize();
        for (int i = 0; i < size; i++) {
            final CheckListItem item = (CheckListItem) model.getElementAt(i);
            if (item.isSelected()) {
                result.add((T) item.getObject());
            }
        }
        return result;
    }
}
