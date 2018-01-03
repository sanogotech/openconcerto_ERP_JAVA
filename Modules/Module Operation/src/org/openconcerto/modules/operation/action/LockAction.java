package org.openconcerto.modules.operation.action;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.swing.Action;

import org.jopencalendar.model.Flag;
import org.jopencalendar.model.JCalendarItemPart;
import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.modules.operation.JCalendarItemDB;
import org.openconcerto.modules.operation.ModuleOperation;
import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.request.UpdateBuilder;

public class LockAction extends AsyncAction {
    private final ArrayList<JCalendarItemPart> selectedItems;

    public LockAction(String s, List<JCalendarItemPart> selectedItems) {
        putValue(Action.NAME, s);
        this.selectedItems = new ArrayList<JCalendarItemPart>(selectedItems);
    }

    @Override
    public Object doInBackground() {
        final Set<JCalendarItemDB> toModify = ModuleOperation.getItemDB(this.selectedItems);
        final DBRoot rootSociete = ComptaPropsConfiguration.getInstanceCompta().getRootSociete();

        for (JCalendarItemDB item : toModify) {
            final SQLTable t = rootSociete.getTable("CALENDAR_ITEM");
            final UpdateBuilder builder = new UpdateBuilder(t);
            if (!item.hasFlag(Flag.getFlag("locked"))) {
                item.addFlag(Flag.getFlag("locked"));
                builder.setObject("FLAGS", item.getFlagsString());
                builder.setWhere(new Where(t.getField("ID_CALENDAR_ITEM_GROUP"), "=", item.getIdCalendarGroup()));
                final String query = builder.asString();
                rootSociete.getDBSystemRoot().getDataSource().execute(query);
            }
        }
        return null;
    }

    @Override
    public void done(Object obj) {
        ModuleOperation.reloadCalendars();
    }
}
