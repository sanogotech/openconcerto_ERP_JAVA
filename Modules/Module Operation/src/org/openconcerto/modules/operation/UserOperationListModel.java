package org.openconcerto.modules.operation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openconcerto.sql.users.User;
import org.openconcerto.sql.users.UserManager;
import org.openconcerto.ui.list.CheckListItem;

public class UserOperationListModel extends CheckListModel {
    OperationCalendar p;

    public UserOperationListModel(OperationCalendar operationCalendarPanel) {
        this.p = operationCalendarPanel;
    }

    @Override
    public List<CheckListItem> loadItems() {
        final List<CheckListItem> items = new ArrayList<CheckListItem>();
        final List<User> users = UserManager.getInstance().getAllActiveUsers();
        // Sort by full name
        Collections.sort(users, new UserComparator());
        // Create list item (with check box)
        final int size = users.size();
        for (int i = 0; i < size; i++) {
            User u = users.get(i);
            final CheckListItem item = new CheckListItem(u, true) {
                @Override
                public String toString() {
                    final User user = (User) getObject();
                    final String info = p.getUserInfo(user);
                    String name = user.getFullName();
                    if (info != null) {
                        name += " " + info;
                    }
                    return name.trim();
                }
            };
            item.setColor(UserColor.getInstance().getColor(u.getId()));
            items.add(item);
        }
        return items;
    }
}