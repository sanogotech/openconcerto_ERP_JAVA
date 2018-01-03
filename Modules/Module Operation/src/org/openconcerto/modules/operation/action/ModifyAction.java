package org.openconcerto.modules.operation.action;

import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;

import org.jopencalendar.model.JCalendarItemPart;
import org.openconcerto.modules.operation.JCalendarItemDB;
import org.openconcerto.sql.view.EditFrame;
import org.openconcerto.sql.view.EditPanel.EditMode;
import org.openconcerto.ui.FrameUtil;

public class ModifyAction extends AbstractAction {
    private List<JCalendarItemPart> selectedItems;

    public ModifyAction(List<JCalendarItemPart> selectedItems) {
        putValue(Action.NAME, "Modifier");
        this.selectedItems = selectedItems;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (selectedItems.isEmpty()) {
            return;
        }

        final JCalendarItemDB item = (JCalendarItemDB) selectedItems.get(0).getItem();
        item.getSourceElement().createDefaultComponent();
        final EditFrame f = new EditFrame(item.getSourceElement(), EditMode.MODIFICATION);
        f.selectionId((int) item.getSourceId());
        FrameUtil.show(f);

    }

}
