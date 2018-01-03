package org.openconcerto.modules.operation;

import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractListModel;
import javax.swing.SwingUtilities;

import org.openconcerto.ui.list.CheckListItem;
import org.openconcerto.utils.SwingWorker2;

public abstract class CheckListModel extends AbstractListModel {
    private List<CheckListItem> items = new ArrayList<CheckListItem>();

    @Override
    public int getSize() {
        return items.size();
    }

    @Override
    public Object getElementAt(int index) {
        return items.get(index);
    }

    public void loadContent() {
        assert SwingUtilities.isEventDispatchThread();
        SwingWorker2<List<CheckListItem>, Object> worker = new SwingWorker2<List<CheckListItem>, Object>() {

            @Override
            protected List<CheckListItem> doInBackground() throws Exception {
                return loadItems();
            }

            @Override
            protected void done() {
                try {
                    final List<CheckListItem> l = get();
                    if (items.isEmpty()) {
                        // On ajoute tout, et on sélectionne tout
                        items.clear();
                        items.addAll(l);
                        for (CheckListItem item : items) {
                            item.setSelected(true);
                        }
                    } else {
                        List<Object> previouslySelectedObject = new ArrayList<Object>();
                        for (CheckListItem item : items) {
                            if (item.isSelected()) {
                                previouslySelectedObject.add(item.getObject());
                            }
                        }
                        items.clear();
                        items.addAll(l);
                        // restaure la sélection
                        for (CheckListItem item : items) {
                            final boolean wasSelected = previouslySelectedObject.contains(item.getObject());
                            item.setSelected(wasSelected);
                        }
                    }
                    refresh();
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        };
        worker.execute();
    }

    public abstract List<CheckListItem> loadItems();

    public void refresh() {
        fireContentsChanged(this, 0, items.size());
    }
}