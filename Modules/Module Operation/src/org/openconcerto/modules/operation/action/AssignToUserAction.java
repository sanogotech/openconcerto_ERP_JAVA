package org.openconcerto.modules.operation.action;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.swing.Action;

import org.jopencalendar.model.JCalendarItemPart;
import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.modules.operation.JCalendarItemDB;
import org.openconcerto.modules.operation.ModuleOperation;
import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.request.UpdateBuilder;
import org.openconcerto.sql.users.User;

public class AssignToUserAction extends AsyncAction {

    private final User user;
    private final ArrayList<JCalendarItemPart> selectedItems;

    public AssignToUserAction(User user, List<JCalendarItemPart> selectedItems) {
        putValue(Action.NAME, user.getFullName());
        this.user = user;
        this.selectedItems = new ArrayList<JCalendarItemPart>(selectedItems);
    }

    @Override
    public Object doInBackground() {
        final Set<JCalendarItemDB> toModify = ModuleOperation.getItemDB(this.selectedItems);
        final List<Long> ids = ModuleOperation.getOperationIdsFrom(toModify);
        final DBRoot rootSociete = ComptaPropsConfiguration.getInstanceCompta().getRootSociete();
        final SQLTable t = rootSociete.getTable(ModuleOperation.TABLE_OPERATION);
        final UpdateBuilder builder = new UpdateBuilder(t);
        builder.setObject("ID_USER_COMMON", user.getId());
        builder.setWhere(new Where(t.getKey(), true, ids));
        final String query = builder.asString();
        rootSociete.getDBSystemRoot().getDataSource().execute(query);
        return null;
    }

    @Override
    public void done(Object obj) {
        ModuleOperation.reloadCalendars();
    }
}
