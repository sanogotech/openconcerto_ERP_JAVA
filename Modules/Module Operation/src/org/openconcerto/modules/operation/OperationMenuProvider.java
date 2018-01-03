package org.openconcerto.modules.operation;

import java.util.Collections;
import java.util.List;

import javax.swing.JMenu;
import javax.swing.JPopupMenu;

import org.jopencalendar.model.Flag;
import org.jopencalendar.model.JCalendarItemPart;
import org.jopencalendar.ui.JPopupMenuProvider;
import org.openconcerto.modules.operation.action.AssignToUserAction;
import org.openconcerto.modules.operation.action.DeleteAction;
import org.openconcerto.modules.operation.action.DuplicateAction;
import org.openconcerto.modules.operation.action.LockAction;
import org.openconcerto.modules.operation.action.ModifyAction;
import org.openconcerto.modules.operation.action.RePlanAction;
import org.openconcerto.modules.operation.action.SetStatusAction;
import org.openconcerto.modules.operation.action.UnlockAction;
import org.openconcerto.sql.users.User;
import org.openconcerto.sql.users.UserManager;

public class OperationMenuProvider implements JPopupMenuProvider {
    public OperationMenuProvider() {
    }

    @Override
    public JPopupMenu getPopup(List<JCalendarItemPart> selectedItems, List<JCalendarItemPart> currentColumnParts) {
        boolean oneIsLocked = false;
        for (JCalendarItemPart jCalendarItemPart : selectedItems) {
            if (jCalendarItemPart.getItem().hasFlag(Flag.getFlag("locked"))) {
                oneIsLocked = true;
            }
        }

        // Menu
        JPopupMenu menu = new JPopupMenu();
        if (!oneIsLocked && !selectedItems.isEmpty()) {
            menu.add(new ModifyAction(selectedItems));
        }
        if (!selectedItems.isEmpty()) {
            final JMenu menuAssign = new JMenu("Assigner à");
            final List<User> users = UserManager.getInstance().getAllUser();
            // Sort by full name
            Collections.sort(users, new UserComparator());
            for (User user : users) {
                menuAssign.add(new AssignToUserAction(user, selectedItems));
            }
            menu.add(menuAssign);

            final JMenu menuState = new JMenu("Marquer comme");
            final List<String> status = OperationStateListModel.getStatus();
            for (String s : status) {
                menuState.add(new SetStatusAction(s, selectedItems));
            }
            menu.add(menuState);
            if (oneIsLocked) {
                menuAssign.setEnabled(false);
                menuState.setEnabled(false);
            }

            if (selectedItems.size() == 1 && !oneIsLocked) {
                menu.addSeparator();
                menu.add(new RePlanAction(selectedItems));
            }
            menu.addSeparator();
            if (!oneIsLocked) {
                menu.add(new LockAction("Verrouiller", selectedItems));
            } else {
                menu.add(new UnlockAction("Déverrouiller", selectedItems));
            }
        }
        if (selectedItems.isEmpty() && !currentColumnParts.isEmpty()) {
            menu.add(new LockAction("Verrouiller la journée", currentColumnParts));
        }
        if (!selectedItems.isEmpty()) {
            final DuplicateAction dup = new DuplicateAction(selectedItems);
            if (selectedItems.size() > 1) {
                dup.setEnabled(false);
            }
            menu.add(dup);
        }

        if (!selectedItems.isEmpty()) {
            if (!oneIsLocked) {
                menu.addSeparator();
                menu.add(new DeleteAction(selectedItems));
            }
        }
        return menu;
    }

}
