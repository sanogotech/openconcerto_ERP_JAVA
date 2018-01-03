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

public class SetStatusAction extends AsyncAction {
    private final String status;
    private final ArrayList<JCalendarItemPart> selectedItems;

    public SetStatusAction(String s, List<JCalendarItemPart> selectedItems) {
        putValue(Action.NAME, s);
        this.status = s;
        this.selectedItems = new ArrayList<JCalendarItemPart>(selectedItems);
    }

    @Override
    public Object doInBackground() {
        final Set<JCalendarItemDB> toModify = ModuleOperation.getItemDB(this.selectedItems);
        final List<Long> ids = ModuleOperation.getOperationIdsFrom(toModify);
        final DBRoot rootSociete = ComptaPropsConfiguration.getInstanceCompta().getRootSociete();
        final SQLTable t = rootSociete.getTable(ModuleOperation.TABLE_OPERATION);
        final UpdateBuilder builder = new UpdateBuilder(t);
        builder.setObject("STATUS", this.status);
        builder.setWhere(new Where(t.getKey(), true, ids));
        final String query = builder.asString();
        rootSociete.getDBSystemRoot().getDataSource().execute(query);
        t.fireTableModified(-1);
        return null;
    }

    @Override
    public void done(Object obj) {
        ModuleOperation.reloadCalendars();
    }

}
