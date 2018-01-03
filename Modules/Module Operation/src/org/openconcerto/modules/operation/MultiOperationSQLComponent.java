package org.openconcerto.modules.operation;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.swing.JComponent;

import org.apache.commons.dbutils.ResultSetHandler;
import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.sql.PropsConfiguration;
import org.openconcerto.sql.element.GroupSQLComponent;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSyntax;
import org.openconcerto.sql.model.SQLSystem;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.sql.sqlobject.SQLTextCombo;
import org.openconcerto.sql.utils.SQLUtils;
import org.openconcerto.ui.JLabelBold;
import org.openconcerto.ui.component.ITextArea;
import org.openconcerto.ui.date.DateRange;
import org.openconcerto.ui.date.DateRangePlannerPanel;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.checks.ValidState;

public class MultiOperationSQLComponent extends GroupSQLComponent {

    private String uid;
    private Date dateStart;

    public MultiOperationSQLComponent(SQLElement element) {
        super(element, new MultiOperationGroup());
    }

    @Override
    public JComponent createEditor(String id) {
        if (id.equals("operation.description")) {
            return new ITextArea(15, 3);
        } else if (id.equals("operation.dates")) {
            final DateRangePlannerPanel dateRangePlannerPanel = new DateRangePlannerPanel();
            final PropertyChangeListener listener = new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    fireValidChange();
                }
            };
            dateRangePlannerPanel.getJDateStart().addValueListener(listener);
            dateRangePlannerPanel.getJDateEnd().addValueListener(listener);
            return dateRangePlannerPanel;
        } else if (id.equals("operation.type") || id.equals("operation.status")) {
            return new SQLTextCombo();
        }
        return super.createEditor(id);
    }

    @Override
    protected JComponent createLabel(String id) {
        if (id.equals("operation.dates")) {
            return new JLabelBold("");
        }
        return super.createLabel(id);
    }

    DateRangePlannerPanel getDateRangeTable() {
        return (DateRangePlannerPanel) getEditor("operation.dates");
    }

    @Override
    public void select(final SQLRowAccessor r) {
        super.select(r);
        if (r != null) {
            this.uid = r.getString("PLANNER_UID");
            final String xml = r.getString("PLANNER_XML");
            try {
                if (xml != null && !xml.isEmpty()) {
                    this.getDateRangeTable().configureFromXML(xml);
                }
            } catch (Exception e) {
                ExceptionHandler.handle("Cannot configure editor from:\n" + xml, e);
            }
        } else {
            this.uid = null;
        }
    }

    @Override
    public void update() {

        if (this.uid != null && !uid.isEmpty() && this.dateStart != null) {
            final OperationSQLElement elem = (OperationSQLElement) PropsConfiguration.getInstance().getDirectory().getElement(ModuleOperation.TABLE_OPERATION);
            try {
                elem.fastDelete(this.uid, this.dateStart);
                insert(null);
            } catch (Exception e) {
                ExceptionHandler.handle("Update error", e);
            }
        }
    }

    @Override
    public int insert(SQLRow order) {

        final List<DateRange> ranges = this.getDateRangeTable().getRanges();
        if (ranges.isEmpty()) {
            // Fill with current date to avoid issue... :(
            ranges.add(new DateRange(System.currentTimeMillis()));
        }

        // Split pour ne pas bloquer l'UI
        // Synchrone
        final List<DateRange> rangesPart1 = new ArrayList<DateRange>();
        // Asynchrone
        final List<DateRange> rangesPart2 = new ArrayList<DateRange>();
        for (int i = 0; i < ranges.size(); i++) {
            if (i < 10) {
                rangesPart1.add(ranges.get(i));
            } else {
                rangesPart2.add(ranges.get(i));
            }

        }
        final String type = ((SQLTextCombo) this.getEditor("operation.type")).getCurrentValue();
        final String siteName = ((ElementComboBox) this.getEditor("operation.site")).getSelectedRow().getString("NAME");
        //
        final String summary = siteName + " " + type;
        final String description = ((ITextArea) this.getEditor("operation.description")).getText();
        final String status = ((SQLTextCombo) this.getEditor("operation.status")).getCurrentValue();

        final int idSite = ((ElementComboBox) this.getEditor("operation.site")).getSelectedId();
        final int idUser = ((ElementComboBox) this.getEditor("operation.user")).getSelectedId();

        // Create Groups
        final List<Number> calendarGroupIds = multipleInsertCalendarGroups(rangesPart1.size(), summary, description);
        // Create Operations
        final String plannerUid = UUID.randomUUID().toString();

        final String plannerXML = getDateRangeTable().getConfigXML();

        final List<Number> operationsIds = multipleInsertOperation(calendarGroupIds, idSite, status, type, description, idUser, plannerUid, plannerXML);

        multipleInsertCalendarItems(calendarGroupIds, operationsIds, rangesPart1, summary, description, status);

        if (!rangesPart2.isEmpty()) {
            final Thread t = new Thread(new Runnable() {

                @Override
                public void run() {
                    // Create Groups
                    final List<Number> calendarGroupIds = multipleInsertCalendarGroups(rangesPart2.size(), summary, description);
                    // Create Operations
                    final List<Number> operationsIds = multipleInsertOperation(calendarGroupIds, idSite, status, type, description, idUser, plannerUid, plannerXML);
                    multipleInsertCalendarItems(calendarGroupIds, operationsIds, rangesPart2, summary, description, status);
                }
            });
            t.setName(this.toString());
            t.start();
        }
        ModuleOperation.reloadCalendars();
        return operationsIds.get(0).intValue();
    }

    private List<Number> multipleInsertCalendarGroups(int numberOfRowToCreate, String name, String description) {
        final List<Number> ids = new ArrayList<Number>();
        //
        SQLTable tCalendarItemGroup = getDirectory().getElement("CALENDAR_ITEM_GROUP").getTable();
        if (tCalendarItemGroup.getServer().getSQLSystem().equals(SQLSystem.POSTGRESQL)) {
            final SQLSyntax syntax = this.getTable().getDBSystemRoot().getSyntax();
            final List<String> fields = Arrays.asList("NAME", "DESCRIPTION", "ORDRE");
            final List<List<String>> values = new ArrayList<List<String>>(numberOfRowToCreate);
            for (int i = 0; i < numberOfRowToCreate; i++) {
                final List<String> row = new ArrayList<String>(fields.size());
                row.add(syntax.quoteString(name));
                row.add(syntax.quoteString(description));
                row.add("( select COALESCE(MAX(\"ORDRE\"), 0) + " + (i + 1) + " from " + tCalendarItemGroup.getQuotedName() + " )");
                values.add(row);
            }

            try {
                ids.addAll(SQLRowValues.insertIDs(tCalendarItemGroup, "(" + SQLSyntax.quoteIdentifiers(fields) + ") " + syntax.getValues(values, fields.size())));
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            SQLRowValues v = new SQLRowValues(tCalendarItemGroup);
            v.put("NAME", name);
            v.put("DESCRIPTION", description);
            int i = 0;
            try {
                for (i = 0; i < numberOfRowToCreate; i++) {
                    ids.add(v.insert().getIDNumber());
                }
            } catch (SQLException e) {
                throw new IllegalStateException("cannot insert rowvalues " + i + " [" + numberOfRowToCreate + "]", e);
            }
        }

        return ids;
    }

    // Multiple Insert
    public List<Number> multipleInsertOperation(List<Number> calendarGroupIds, int idSite, String status, String type, String description, int idUser, String plannerUid, String plannerXML) {

        final List<Number> ids = new ArrayList<Number>();
        int size = calendarGroupIds.size();
        if (this.getTable().getServer().getSQLSystem().equals(SQLSystem.POSTGRESQL)) {
            final SQLSyntax syntax = this.getTable().getDBSystemRoot().getSyntax();
            final List<String> fields = Arrays.asList("ID_SITE", "STATUS", "TYPE", "DESCRIPTION", "PLANNER_UID", "PLANNER_XML", "ID_USER_COMMON", "ID_CALENDAR_ITEM_GROUP", "ORDRE");
            final List<List<String>> values = new ArrayList<List<String>>(size);
            for (int i = 0; i < size; i++) {
                final List<String> row = new ArrayList<String>(fields.size());
                row.add(String.valueOf(idSite));
                row.add(syntax.quoteString(status));
                row.add(syntax.quoteString(type));
                row.add(syntax.quoteString(description));
                row.add(syntax.quoteString(plannerUid));
                row.add(syntax.quoteString(plannerXML));
                row.add(String.valueOf(idUser));
                row.add(String.valueOf(calendarGroupIds.get(i).longValue()));
                row.add("( select COALESCE(MAX(\"ORDRE\"), 0) + " + (i + 1) + " from " + this.getTable().getQuotedName() + " )");
                values.add(row);
            }

            try {
                ids.addAll(SQLRowValues.insertIDs(getTable(), "(" + SQLSyntax.quoteIdentifiers(fields) + ") " + syntax.getValues(values, fields.size())));
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            final SQLRowValues v = new SQLRowValues(getTable());
            v.put("ID_SITE", idSite);
            v.put("STATUS", status);
            v.put("TYPE", type);
            v.put("DESCRIPTION", description);
            v.put("PLANNER_UID", plannerUid);
            v.put("PLANNER_XML", plannerXML);
            v.put("ID_USER_COMMON", idUser);
            int i = 0;
            try {
                for (i = 0; i < size; i++) {
                    v.put("ID_CALENDAR_ITEM_GROUP", calendarGroupIds.get(i));
                    ids.add(v.insert().getIDNumber());
                }
            } catch (SQLException e) {
                throw new IllegalStateException("cannot insert rowvalues " + i + " [" + size + "]", e);
            }
        }

        return ids;
    }

    private void multipleInsertCalendarItems(List<Number> calendarGroupIds, List<Number> operationsIds, List<DateRange> ranges, String summary, String description, String status) {
        final int size = calendarGroupIds.size();
        final DBRoot root = ComptaPropsConfiguration.getInstanceCompta().getRootSociete();
        final SQLTable tCalendarItem = root.getTable("CALENDAR_ITEM");
        //
        final List<String> queries = new ArrayList<String>(size);
        final List<ResultSetHandler> handlers = new ArrayList<ResultSetHandler>(size);
        final ResultSetHandler handler = new ResultSetHandler() {

            @Override
            public Object handle(ResultSet rs) throws SQLException {
                return null;
            }
        };

        for (int i = 0; i < size; i++) {
            final DateRange dateRange = ranges.get(i);
            String query = "INSERT INTO " + tCalendarItem.getQuotedName();
            query += " (\"START\", \"END\", \"DURATION_S\", \"SUMMARY\", \"DESCRIPTION\", \"FLAGS\", \"STATUS\", \"ID_CALENDAR_ITEM_GROUP\", \"SOURCE_ID\", \"SOURCE_TABLE\", \"ORDRE\")";
            query += " select ";
            query += tCalendarItem.getField("START").getType().toString(new Date(dateRange.getStart())) + ", ";
            query += tCalendarItem.getField("END").getType().toString(new Date(dateRange.getStop())) + ", ";
            long duration = (dateRange.getStop() - dateRange.getStart()) / 1000;
            query += tCalendarItem.getField("DURATION_S").getType().toString(duration) + ", ";
            query += SQLBase.quoteStringStd(summary) + ", ";
            query += SQLBase.quoteStringStd(description) + ", ";
            query += SQLBase.quoteStringStd("planned") + ", ";
            query += SQLBase.quoteStringStd(status) + ", ";
            query += calendarGroupIds.get(i) + ", ";
            query += operationsIds.get(i) + ", ";
            query += SQLBase.quoteStringStd(ModuleOperation.TABLE_OPERATION) + ", ";
            query += "COALESCE(MAX(\"ORDRE\"), 0) + 1 ";
            query += "FROM " + tCalendarItem.getQuotedName();
            queries.add(query);
            handlers.add(handler);

        }
        try {
            SQLUtils.executeMultiple(this.getTable().getDBSystemRoot(), queries, handlers);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setDateStart(Date dateStart) {
        this.dateStart = dateStart;
        this.getDateRangeTable().setStartDate(dateStart);
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
    public synchronized ValidState getValidState() {
        DateRangePlannerPanel t = this.getDateRangeTable();
        ValidState e = super.getValidState();
        e = e.and(ValidState.createCached(t.getJDateStart().getValue() != null, "Date de début incorrecte"));
        e = e.and(ValidState.createCached(t.getJDateEnd().getValue() != null, "Date de fin incorrecte"));
        return e;
    }
}
