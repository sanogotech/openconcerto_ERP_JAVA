package org.openconcerto.modules.operation.action;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;

import org.jopencalendar.model.JCalendarItemPart;
import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.modules.operation.JCalendarItemDB;
import org.openconcerto.modules.operation.ModuleOperation;
import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.utils.ExceptionHandler;

public class DuplicateAction extends AsyncAction {
    private final List<JCalendarItemPart> selectedItems;

    public DuplicateAction(List<JCalendarItemPart> selectedItems) {
        putValue(Action.NAME, "Dupliquer");
        this.selectedItems = new ArrayList<JCalendarItemPart>(selectedItems);
    }

    @Override
    public Object doInBackground() {
        try {
            final DBRoot root = ComptaPropsConfiguration.getInstanceCompta().getRootSociete();
            final JCalendarItemDB item = (JCalendarItemDB) selectedItems.get(0).getItem();

            // CalendarItemGroup
            final String description = item.getDescription().trim();
            final SQLTable tCalendarItemGroup = root.getTable("CALENDAR_ITEM_GROUP");
            final SQLBase base = tCalendarItemGroup.getBase();
            String query1 = " (\"NAME\", \"DESCRIPTION\", \"ORDRE\") select " + base.quoteString(item.getSummary()) + ", " + base.quoteString(description) + ", COALESCE(MAX(\"ORDRE\"), 0) + 1 ";
            query1 += "FROM " + tCalendarItemGroup.getQuotedName();
            long calendarGroupId = SQLRowValues.insertIDs(tCalendarItemGroup, query1).get(0).longValue();

            // Operation
            final String type = item.getType();
            final String status = item.getStatus();
            final long idSite = item.getSiteId().longValue();
            final String plannerUid = "";
            final String plannerXML = "";
            final int idUser = (Integer) item.getUserId();
            final SQLTable tOperation = root.getTable(ModuleOperation.TABLE_OPERATION);
            String query2 = " (\"ID_SITE\", \"STATUS\", \"TYPE\", \"DESCRIPTION\", \"PLANNER_UID\", \"PLANNER_XML\", \"ID_USER_COMMON\", \"ID_CALENDAR_ITEM_GROUP\" , \"ORDRE\") ";
            query2 += "select " + idSite + ", " + base.quoteString(status) + ", " + base.quoteString(type) + ", " + base.quoteString(description) + ", " + base.quoteString(plannerUid) + ", "
                    + base.quoteString(plannerXML) + ", " + idUser + ", " + calendarGroupId + ", COALESCE(MAX(\"ORDRE\"), 0) + 1 ";
            query2 += "FROM " + tOperation.getQuotedName();
            long operationId = SQLRowValues.insertIDs(tOperation, query2).get(0).longValue();

            // CalendarItem
            long duration = (item.getDtEnd().getTimeInMillis() - item.getDtStart().getTimeInMillis()) / 1000;
            final SQLTable tCalendarItem = root.getTable("CALENDAR_ITEM");
            String query3 = "INSERT INTO " + tCalendarItem.getQuotedName();
            query3 += " (\"START\", \"END\", \"DURATION_S\", \"SUMMARY\", \"DESCRIPTION\", \"FLAGS\", \"STATUS\", \"ID_CALENDAR_ITEM_GROUP\", \"SOURCE_ID\", \"SOURCE_TABLE\", \"ORDRE\")";
            query3 += " select ";
            query3 += tCalendarItem.getField("START").getType().toString(item.getDtStart().getTime()) + ", ";
            query3 += tCalendarItem.getField("END").getType().toString(item.getDtEnd().getTime()) + ", ";
            query3 += tCalendarItem.getField("DURATION_S").getType().toString(duration) + ", ";
            query3 += base.quoteString(item.getSummary()) + ", ";
            query3 += base.quoteString(description) + ", ";
            query3 += base.quoteString(item.getFlagsString()) + ", ";
            query3 += base.quoteString(item.getStatus()) + ", ";
            query3 += calendarGroupId + ", ";
            query3 += operationId + ", ";
            query3 += base.quoteString(ModuleOperation.TABLE_OPERATION) + ", ";
            query3 += "COALESCE(MAX(\"ORDRE\"), 0) + 1 ";
            query3 += "FROM " + tCalendarItem.getQuotedName();
            root.getDBSystemRoot().getDataSource().execute(query3);
        } catch (SQLException e) {
            e.printStackTrace();
            ExceptionHandler.handle("Duplication error", e);
        }
        return null;
    }

    @Override
    public void done(Object obj) {
        ModuleOperation.reloadCalendars();
    }
}
