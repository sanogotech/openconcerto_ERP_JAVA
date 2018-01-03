package org.openconcerto.modules.operation.action;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.jopencalendar.model.JCalendarItemPart;
import org.openconcerto.ui.FrameUtil;

public class DeleteAction extends AbstractAction {
    private final List<JCalendarItemPart> selectedItems;

    public DeleteAction(List<JCalendarItemPart> selectedItems) {
        putValue(Action.NAME, "Effacer");
        this.selectedItems = new ArrayList<JCalendarItemPart>(selectedItems);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (selectedItems.isEmpty()) {
            JOptionPane.showMessageDialog(new JFrame(), "Aucun élément sélectionné");
            return;
        }
        final JFrame f = new JFrame("Effacement");
        final DeletePanel p = new DeletePanel(selectedItems);
        f.setContentPane(p);
        f.setLocationRelativeTo(null);
        FrameUtil.showPacked(f);
    }
}
