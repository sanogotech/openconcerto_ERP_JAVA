package org.openconcerto.modules.operation;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JComponent;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.sql.element.GroupSQLComponent;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.SQLDataSource;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowMode;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.sqlobject.SQLTextCombo;
import org.openconcerto.ui.JLabelBold;
import org.openconcerto.ui.component.ITextArea;
import org.openconcerto.ui.date.DateRange;
import org.openconcerto.ui.date.DateRangeTable;
import org.openconcerto.utils.ExceptionHandler;

public class OperationSQLComponent extends GroupSQLComponent {

    public OperationSQLComponent(SQLElement element) {
        super(element, new OperationGroup());
    }

    @Override
    public JComponent createEditor(String id) {
        if (id.equals("operation.description")) {
            return new ITextArea(15, 3);
        } else if (id.equals("operation.dates")) {
            return new DateRangeTable(true, false, false);
        } else if (id.equals("operation.type") || id.equals("operation.status")) {
            return new SQLTextCombo();
        }
        return super.createEditor(id);
    }

    @Override
    protected Set<String> createRequiredNames() {
        Set<String> s = new HashSet<String>();
        s.add("ID_SITE");
        s.add("ID_USER_COMMON");
        s.add("TYPE");
        s.add("STATUS");
        return s;
    }

    @Override
    protected JComponent createLabel(String id) {
        if (id.equals("operation.dates")) {
            return new JLabelBold("");
        }
        return super.createLabel(id);
    }

    DateRangeTable getDateRangeTable() {
        return (DateRangeTable) getEditor("operation.dates");
    }

    @Override
    public void select(final SQLRowAccessor r) {
        super.select(r);

        if (r != null && r.getID() > this.getTable().getUndefinedID()) {
            SQLSelect select = new SQLSelect();
            int idGroup = r.getInt("ID_CALENDAR_ITEM_GROUP");
            final SQLTable calendarItemTable = r.getTable().getTable("CALENDAR_ITEM");
            select.addSelectStar(calendarItemTable);
            select.setWhere(calendarItemTable.getField("ID_CALENDAR_ITEM_GROUP"), "=", idGroup);
            final SQLDataSource ds = ComptaPropsConfiguration.getInstanceCompta().getRootSociete().getDBSystemRoot().getDataSource();
            @SuppressWarnings("unchecked")
            final List<Map<String, Object>> result = ds.execute(select.asString());
            final List<DateRange> ranges = new ArrayList<DateRange>();
            for (Map<String, Object> row : result) {
                final DateRange range = new DateRange();
                final Date dStart = (Date) row.get("START");
                final Date dEnd = (Date) row.get("END");
                range.setStart(dStart.getTime());
                range.setStop(dEnd.getTime());
                ranges.add(range);
            }

            getDateRangeTable().fillFrom(ranges);

        } else {
            getDateRangeTable().clear();
        }
    }

    @Override
    public void update() {
        super.update();
        updateCalendar(this.getSelectedID());
    }

    @Override
    public int insert(SQLRow order) {
        int id = super.insert(order);
        updateCalendar(id);
        return id;
    }

    private void updateCalendar(int idOperation) {
        final DBRoot root = ComptaPropsConfiguration.getInstanceCompta().getRootSociete();
        final SQLTable tOperation = root.getTable(ModuleOperation.TABLE_OPERATION);
        final SQLRow operationRow = tOperation.getRow(idOperation);
        SQLRow calendarGroupRow = operationRow.getForeignRow("ID_CALENDAR_ITEM_GROUP", SQLRowMode.DEFINED);
        List<String> oldUIds = new ArrayList<String>();
        if (calendarGroupRow != null) {
            oldUIds.addAll(getUUidsFromCalendarGroupId(root, calendarGroupRow.getID()));
            // Remove associated CalendarItems AND CalendarItemGroups
            final SQLTable tItems = root.getTable("CALENDAR_ITEM");
            String deleteReq = "DELETE FROM " + tItems.getSQLName().quote() + " WHERE ";
            Where where = new Where(tItems.getField("ID_CALENDAR_ITEM_GROUP"), "=", calendarGroupRow.getID());
            deleteReq += where.getClause();
            root.getDBSystemRoot().getDataSource().execute(deleteReq);
            // Archive group
            final SQLRowValues asRowValues = calendarGroupRow.asRowValues();
            asRowValues.put("ARCHIVE", 1);
            try {
                asRowValues.commit();
            } catch (SQLException e) {
                ExceptionHandler.handle("Cannot remove associated Calendar group", e);
            }
        }
        final SQLRowValues rowItemGroup = new SQLRowValues(root.getTable("CALENDAR_ITEM_GROUP"));
        rowItemGroup.put("NAME", operationRow.getForeignRow("ID_SITE").getString("NAME") + " " + operationRow.getString("TYPE"));
        rowItemGroup.put("DESCRIPTION", operationRow.getString("DESCRIPTION"));
        try {
            calendarGroupRow = rowItemGroup.commit();
            // Update Operation
            SQLRowValues operationSQLRowValues = operationRow.asRowValues();
            operationSQLRowValues.put("ID_CALENDAR_ITEM_GROUP", calendarGroupRow.getID());
            operationSQLRowValues.commit();
            // Insert Calendar Items
            final List<DateRange> ranges = this.getDateRangeTable().getRanges();
            int index = 0;
            for (DateRange dateRange : ranges) {
                final SQLRowValues rowItem = new SQLRowValues(root.getTable("CALENDAR_ITEM"));
                rowItem.put("START", new Date(dateRange.getStart()));
                rowItem.put("END", new Date(dateRange.getStop()));
                rowItem.put("DURATION_S", (dateRange.getStop() - dateRange.getStart()) / 1000);
                rowItem.put("SUMMARY", operationRow.getForeignRow("ID_SITE").getString("NAME") + "\n" + operationRow.getString("TYPE"));
                rowItem.put("DESCRIPTION", operationRow.getString("DESCRIPTION"));
                rowItem.put("FLAGS", "");
                rowItem.put("STATUS", operationRow.getString("STATUS"));
                rowItem.put("ID_CALENDAR_ITEM_GROUP", calendarGroupRow.getID());
                rowItem.put("SOURCE_ID", idOperation);
                rowItem.put("SOURCE_TABLE", ModuleOperation.TABLE_OPERATION);
                // apply old UIDs
                if (index < oldUIds.size()) {
                    rowItem.put("UID", oldUIds.get(index));
                }
                rowItem.commit();
                index++;
            }

        } catch (SQLException e) {
            ExceptionHandler.handle("Cannot create Calendar items", e);
        }

    }

    @SuppressWarnings("rawtypes")
    private List<String> getUUidsFromCalendarGroupId(DBRoot root, int id) {
        final List<String> result = new ArrayList<String>();
        final SQLTable tCalendarItem = root.getTable("CALENDAR_ITEM");
        final SQLSelect s = new SQLSelect();
        s.addSelect(tCalendarItem.getField("UID"));
        final Where where = new Where(tCalendarItem.getField("ID_CALENDAR_ITEM_GROUP"), "=", id);
        s.setWhere(where);

        final List r = root.getDBSystemRoot().getDataSource().execute(s.asString());

        for (Object line : r) {
            final String uid = (String) ((Map) line).get("UID");
            if (uid != null && !uid.trim().isEmpty()) {
                result.add(uid);
            }
        }
        return result;
    }

    public void setDate(Date date) {
        final List<DateRange> list = new ArrayList<DateRange>();
        list.add(new DateRange(date.getTime()));
        this.getDateRangeTable().fillFrom(list);
    }
}
