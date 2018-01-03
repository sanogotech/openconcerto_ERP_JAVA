package org.openconcerto.modules.operation;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.dbutils.ResultSetHandler;
import org.openconcerto.erp.modules.AbstractModule;
import org.openconcerto.erp.modules.ModuleElement;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.element.TreesOfSQLRows;
import org.openconcerto.sql.model.DBSystemRoot;
import org.openconcerto.sql.model.FieldPath;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowMode;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.model.graph.Path;
import org.openconcerto.sql.utils.SQLUtils;
import org.openconcerto.sql.view.list.BaseSQLTableModelColumn;
import org.openconcerto.sql.view.list.SQLTableModelSource;
import org.openconcerto.utils.RTInterruptedException;

public class OperationSQLElement extends ModuleElement {

    public OperationSQLElement(AbstractModule module) {
        super(module, ModuleOperation.TABLE_OPERATION);
    }

    @Override
    protected List<String> getListFields() {
        return Arrays.asList("ID_SITE", "ID_USER_COMMON", "TYPE", "STATUS", "DESCRIPTION");
    }

    @Override
    protected SQLComponent createComponent() {
        return new OperationSQLComponent(this);
    }

    @Override
    protected void _initTableSource(SQLTableModelSource source) {
        super._initTableSource(source);
        source.getColumns().add(new BaseSQLTableModelColumn("Date", Timestamp.class) {

            @Override
            protected Object show_(SQLRowAccessor r) {
                SQLRowAccessor rGroup = r.getForeign("ID_CALENDAR_ITEM_GROUP");
                Collection<? extends SQLRowAccessor> l = rGroup.getReferentRows(r.getTable().getTable("CALENDAR_ITEM"));
                if (l.isEmpty()) {
                    return null;
                }
                return l.iterator().next().getObject("START");
            }

            @Override
            public Set<FieldPath> getPaths() {
                Path p = new Path(getTable()).add(getTable().getField("ID_CALENDAR_ITEM_GROUP")).add(getTable().getTable("CALENDAR_ITEM"));

                return FieldPath.create(p, Arrays.asList("START"));
            }
        });
    }

    @Override
    protected void archive(TreesOfSQLRows trees, boolean cutLinks) throws SQLException {
        List<SQLRowAccessor> rows = new ArrayList<SQLRowAccessor>();
        for (SQLRow r : trees.getRows()) {
            rows.add(r.getForeignRow("ID_CALENDAR_ITEM_GROUP", SQLRowMode.NO_CHECK));
        }
        super.archive(trees, cutLinks);

        getDirectory().getElement(getTable().getForeignTable("ID_CALENDAR_ITEM_GROUP")).archive(rows);

    }

    /**
     * Delete operation, linked calendar groups and items
     * 
     * @throws SQLException
     * @throws RTInterruptedException
     */
    @SuppressWarnings("unchecked")
    public void fastDelete(List<Long> operationsIds) throws RTInterruptedException, SQLException {
        if (operationsIds.isEmpty()) {
            return;
        }
        // Get calendar groups ids
        SQLSelect select = new SQLSelect();
        select.addSelect(this.getTable().getField("ID_CALENDAR_ITEM_GROUP"));
        select.addGroupBy(this.getTable().getField("ID_CALENDAR_ITEM_GROUP"));
        final String pkey = this.getTable().getPKsNames().get(0);
        select.setWhere(new Where(this.getTable().getField(pkey), true, operationsIds));
        String query = select.asString();
        final DBSystemRoot dbSystemRoot = this.getTable().getDBSystemRoot();
        List<Number> calendarGroupIds = (List<Number>) dbSystemRoot.getDataSource().executeCol(query);
        // Delete items
        final SQLField f1 = dbSystemRoot.findTable("CALENDAR_ITEM").getField("ID_CALENDAR_ITEM_GROUP");
        String q1 = "update " + SQLBase.quoteIdentifier("CALENDAR_ITEM");
        q1 += " set " + SQLBase.quoteIdentifier("ARCHIVE") + " = 1 ";
        q1 += " where " + new Where(f1, true, calendarGroupIds);
        // Delete groups
        final SQLField f2 = this.getTable().getDBSystemRoot().findTable("CALENDAR_ITEM_GROUP").getPrimaryKeys().iterator().next();
        String q2 = "update " + SQLBase.quoteIdentifier("CALENDAR_ITEM_GROUP");
        q2 += " set " + SQLBase.quoteIdentifier("ARCHIVE") + " = 1 ";
        q2 += " where " + new Where(f2, true, calendarGroupIds);
        // Delete operations
        final SQLField f3 = this.getTable().getField("ID_CALENDAR_ITEM_GROUP");
        String q3 = "update " + SQLBase.quoteIdentifier(ModuleOperation.TABLE_OPERATION);
        q3 += " set " + SQLBase.quoteIdentifier("ARCHIVE") + " = 1 ";
        q3 += " where " + new Where(f3, true, calendarGroupIds);

        // execute in one shot
        final List<String> queries = new ArrayList<String>(3);
        queries.add(q1);
        queries.add(q2);
        queries.add(q3);
        final List<ResultSetHandler> handlers = new ArrayList<ResultSetHandler>(3);
        final ResultSetHandler handler = new ResultSetHandler() {

            @Override
            public Object handle(ResultSet r) throws SQLException {
                // Nothing to do here
                return null;
            }
        };
        handlers.add(handler);
        handlers.add(handler);
        handlers.add(handler);
        SQLUtils.executeMultiple(dbSystemRoot, queries, handlers);

    }

    @SuppressWarnings("unchecked")
    public void fastDelete(String uid, Date startDate) throws Exception {
        SQLSelect select = new SQLSelect();
        select.addSelect(this.getTable().getField("ID_CALENDAR_ITEM_GROUP"));
        select.setWhere(new Where(this.getTable().getField("PLANNER_UID"), "=", uid));
        String query = select.asString();
        final DBSystemRoot dbSystemRoot = this.getTable().getDBSystemRoot();
        List<Number> calendarGroupIds = (List<Number>) dbSystemRoot.getDataSource().executeCol(query);

        // Recuperation des groupes correspondants et qui ont des items apres la date
        final SQLTable tableCalendarItem = dbSystemRoot.findTable("CALENDAR_ITEM");
        final SQLSelect select2 = new SQLSelect();
        select2.addSelect(tableCalendarItem.getField("ID_CALENDAR_ITEM_GROUP"));
        Where where2 = new Where(tableCalendarItem.getField("ID_CALENDAR_ITEM_GROUP"), true, calendarGroupIds);
        where2 = where2.and(new Where(tableCalendarItem.getField("START"), ">=", startDate.getTime()));
        select2.setWhere(where2);
        String query2 = select2.asString();
        List<Number> calendarGroupIdsToDelete = (List<Number>) dbSystemRoot.getDataSource().executeCol(query2);
        //
        final SQLSelect select3 = new SQLSelect();
        select3.addSelect(getTable().getKey());
        select3.setWhere(new Where(getTable().getField("ID_CALENDAR_ITEM_GROUP"), true, calendarGroupIdsToDelete));
        String query3 = select3.asString();
        List<Number> operationIdsToDelete = (List<Number>) dbSystemRoot.getDataSource().executeCol(query3);
        Set<Long> oids = new HashSet<Long>();
        for (Number n : operationIdsToDelete) {
            oids.add(n.longValue());
        }
        final List<Long> oidsToDelete = new ArrayList<Long>();
        oidsToDelete.addAll(oids);
        Collections.sort(oidsToDelete);

        fastDelete(oidsToDelete);

    }
}
