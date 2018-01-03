package org.openconcerto.modules.operation.action;

import java.awt.event.ActionEvent;
import java.util.Date;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;

import org.jopencalendar.model.JCalendarItemPart;
import org.openconcerto.modules.operation.JCalendarItemDB;
import org.openconcerto.modules.operation.MultiOperationSQLComponent;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.view.EditFrame;
import org.openconcerto.sql.view.EditPanel.EditMode;
import org.openconcerto.ui.FrameUtil;

public class RePlanAction extends AbstractAction {
    private List<JCalendarItemPart> selectedItems;

    public RePlanAction(List<JCalendarItemPart> selectedItems) {
        putValue(Action.NAME, "Replannifier");
        this.selectedItems = selectedItems;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (selectedItems.size() != 1) {
            return;
        }
        final JCalendarItemDB item = (JCalendarItemDB) selectedItems.get(0).getItem();
        final int operationId = (int) item.getSourceId();
        final SQLRow r = item.getSourceElement().getTable().getRow(operationId);
        if (!r.getString("PLANNER_UID").isEmpty()) {
            final MultiOperationSQLComponent comp = new MultiOperationSQLComponent(item.getSourceElement());
            final EditFrame f = new EditFrame(comp, EditMode.MODIFICATION);
            f.selectionId(operationId);
            comp.setDateStart((Date) selectedItems.get(0).getItem().getDtStart().getTime().clone());
            FrameUtil.show(f);
        } else {
            JOptionPane.showMessageDialog(null, "Cette intervention ne peut pas être directement replannifiée");
        }

    }
}
