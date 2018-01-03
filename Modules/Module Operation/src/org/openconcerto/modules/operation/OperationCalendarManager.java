package org.openconcerto.modules.operation;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.SwingUtilities;

import org.jopencalendar.model.Flag;
import org.jopencalendar.model.JCalendarItem;
import org.jopencalendar.model.JCalendarItemGroup;
import org.jopencalendar.ui.JCalendarItemProvider;
import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowMode;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLRowValuesListFetcher;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.model.graph.Path;
import org.openconcerto.sql.model.graph.PathBuilder;
import org.openconcerto.sql.users.User;
import org.openconcerto.sql.users.UserManager;
import org.openconcerto.ui.date.DateRange;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.StringUtils;
import org.openconcerto.utils.cc.ITransformer;

public class OperationCalendarManager extends JCalendarItemProvider {
    private DBRoot root;

    public OperationCalendarManager(String name) {
        super(name);
        this.root = ComptaPropsConfiguration.getInstanceCompta().getRootSociete();
    }

    public OperationCalendarManager(String name, DBRoot root) {
        super(name);
        this.root = root;
    }

    private List<User> users;
    private List<String> states;
    private boolean hideLocked = false;
    private boolean hideUnlocked = false;

    /**
     * Set the filter to retrieve items
     * 
     * @param users if null don't limit to specific users
     * @param states if null don't limite to specific states
     */
    synchronized public void setFilter(List<User> users, List<String> states, boolean hideLocked, boolean hideUnlocked) {
        this.users = users;
        this.states = states;
        this.hideLocked = hideLocked;
        this.hideUnlocked = hideUnlocked;
    }

    @Override
    synchronized public List<JCalendarItem> getItemInWeek(int week, int year) {
        assert !SwingUtilities.isEventDispatchThread();
        return getItemInWeek(week, year, this.users, this.states);
    }

    @Override
    public List<JCalendarItem> getItemInYear(int year, int week1, int week2) {
        final Calendar c = Calendar.getInstance();
        c.clear();
        c.set(Calendar.YEAR, year);
        c.set(Calendar.WEEK_OF_YEAR, week1);
        c.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        final Date date1 = c.getTime();
        c.set(Calendar.WEEK_OF_YEAR, week2);
        final Date date2 = c.getTime();
        return getItemIn(date1, date2, this.users, this.states);
    }

    private List<JCalendarItem> getItemInWeek(final int week, final int year, final List<User> selectedUsers, final List<String> selectedStates) {
        final Calendar c = Calendar.getInstance();
        c.clear();
        c.set(Calendar.YEAR, year);
        c.set(Calendar.WEEK_OF_YEAR, week);
        c.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        final Date date1 = c.getTime();
        c.add(Calendar.WEEK_OF_YEAR, 1);
        final Date date2 = c.getTime();
        return getItemIn(date1, date2, selectedUsers, selectedStates);
    }

    public List<JCalendarItem> getItemIn(final Date date1, final Date date2, List<User> selectedUsers, final List<String> selectedStates) {
        return getItemIn(date1, date2, selectedUsers, selectedStates, null);
    }

    public List<JCalendarItem> getItemIn(final Date date1, final Date date2, List<User> selectedUsers, final List<String> selectedStates, final String uid) {
        final List<User> users = new ArrayList<User>();
        if (selectedUsers == null) {
            users.addAll(UserManager.getInstance().getAllActiveUsers());
        } else {
            users.addAll(selectedUsers);
        }
        if (users.isEmpty()) {
            return Collections.emptyList();
        }
        if (selectedStates != null && selectedStates.isEmpty()) {
            return Collections.emptyList();
        }
        final SQLRowValues valCalendarItems = new SQLRowValues(root.getTable("CALENDAR_ITEM"));
        valCalendarItems.putNulls("START", "END", "DURATION_S", "SUMMARY", "DESCRIPTION", "FLAGS", "STATUS", "SOURCE_ID", "SOURCE_TABLE", "UID", "LOCATION");
        final SQLRowValues valsCalendarItemsGroup = valCalendarItems.putRowValues("ID_CALENDAR_ITEM_GROUP");
        valsCalendarItemsGroup.put("NAME", null);

        final SQLRowValues valSite = new SQLRowValues(root.getTable(ModuleOperation.TABLE_SITE));
        valSite.putNulls("NAME", "COMMENT");

        final SQLRowValues valOperation = new SQLRowValues(root.getTable(ModuleOperation.TABLE_OPERATION));
        final SQLRowValues userVals = valOperation.putRowValues("ID_USER_COMMON").putNulls("NOM");
        valOperation.put("ID_CALENDAR_ITEM_GROUP", valsCalendarItemsGroup);
        valOperation.put("ID_SITE", valSite);
        valOperation.putNulls("STATUS", "TYPE", "PLANNER_XML", "PLANNER_UID");

        final SQLRowValuesListFetcher fetcher = SQLRowValuesListFetcher.create(valCalendarItems);
        fetcher.setFullOnly(true);
        fetcher.setSelTransf(new ITransformer<SQLSelect, SQLSelect>() {

            @Override
            public SQLSelect transformChecked(SQLSelect input) {
                if (date1 != null && date2 != null) {
                    input.andWhere(new Where(valCalendarItems.getTable().getField("START"), date1, true, date2, false));
                }
                if (uid != null) {
                    Where ww = new Where(valCalendarItems.getTable().getField("UID"), "=", uid);
                    try {
                        int i = Integer.parseInt(uid);
                        ww = ww.or(new Where(valCalendarItems.getTable().getKey(), "=", i));
                    } catch (Exception e) {

                    }

                    input.andWhere(ww);
                }
                return input;
            }
        });

        final Path item2Operation = new PathBuilder(valCalendarItems.getTable()).addForeignField("ID_CALENDAR_ITEM_GROUP").addReferentTable(ModuleOperation.TABLE_OPERATION).build();
        try {
            CollectionUtils.getSole(fetcher.getFetchers(item2Operation).allValues()).setSelTransf(new ITransformer<SQLSelect, SQLSelect>() {
                @Override
                public SQLSelect transformChecked(SQLSelect input) {
                    if (selectedStates != null) {
                        // Le status utilisé est celui de OPERATION
                        input.andWhere(new Where(input.getAlias(valOperation.getTable()).getField("STATUS"), selectedStates));
                    }
                    return input;
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        final Path p = item2Operation.addForeignField("ID_USER_COMMON");
        final Path pItemToOperation = p.minusLast();
        final Collection<SQLRowValuesListFetcher> fetchers = fetcher.getFetchers(p).allValues();
        if (fetchers.size() != 1)
            throw new IllegalStateException("Not one fetcher : " + fetchers);
        final SQLRowValuesListFetcher userFetcher = fetchers.iterator().next();
        final ITransformer<SQLSelect, SQLSelect> prevTransf = userFetcher.getSelTransf();
        userFetcher.setSelTransf(new ITransformer<SQLSelect, SQLSelect>() {
            @Override
            public SQLSelect transformChecked(SQLSelect input) {
                final List<Number> userIDs = new ArrayList<Number>();
                for (User user : users) {
                    userIDs.add(user.getId());
                }
                input.andWhere(new Where(input.getAlias(userVals.getTable()).getKey(), userIDs));
                // Because it can be the same fetcher that the previous on OPERATION
                return prevTransf == null ? input : prevTransf.transformChecked(input);
            }
        });

        final List<SQLRowValues> rows = fetcher.fetch();

        final Map<String, JCalendarItemGroup> groups = new HashMap<String, JCalendarItemGroup>();

        final List<JCalendarItem> result = new ArrayList<JCalendarItem>(rows.size());
        for (SQLRowValues r : rows) {
            final SQLRowValues user = r.followPath(p);

            if (user != null) {
                final SQLRowValues operation = r.followPath(pItemToOperation);
                final JCalendarItemDB item = new JCalendarItemDB(r.getID(), r.getString("SOURCE_TABLE"), r.getInt("SOURCE_ID"), r.getForeign("ID_CALENDAR_ITEM_GROUP").getID());

                item.setDayOnly(false);
                item.setDtStart(r.getDate("START"));
                item.setDtEnd(r.getDate("END"));
                item.setSummary(r.getString("SUMMARY"));
                item.setLocation(r.getString("LOCATION"));

                final String string = r.getForeign("ID_CALENDAR_ITEM_GROUP").getString("NAME");

                JCalendarItemGroup g = groups.get(string);
                if (g == null) {
                    g = new JCalendarItemGroup();
                    g.setName(string);
                    groups.put(string, g);
                }
                item.setGroup(g);

                if (r.getString("FLAGS") != null) {
                    List<String> str = StringUtils.fastSplit(r.getString("FLAGS"), ',');

                    for (String fId : str) {
                        Flag f = Flag.getFlag(fId);
                        if (f == null) {
                            f = new Flag(fId, null, fId, "");
                            Flag.register(f);
                        }
                        item.addFlag(f);
                    }

                }
                String desc = "";
                if (r.getString("DESCRIPTION") != null) {
                    desc += r.getString("DESCRIPTION") + "\n";
                }
                item.setDescription(desc);
                item.setColor(UserColor.getInstance().getColor(user.getID()));
                item.setCookie(user);
                item.setUserId(user.getID());
                item.setOperationStatus(operation.getString("STATUS"));
                item.setOperationType(operation.getString("TYPE"));
                item.setPlannerXML(operation.getString("PLANNER_XML"));
                item.setPlannerUID(operation.getString("PLANNER_UID"));
                if (operation.getForeign("ID_SITE") != null) {
                    item.setSiteName(operation.getForeign("ID_SITE").getString("NAME"));
                    item.setSiteId(operation.getForeign("ID_SITE").getIDNumber());
                    item.setSiteComment(operation.getForeign("ID_SITE").getString("COMMENT"));
                } else {
                    item.setSiteName("");
                    item.setSiteId(-1);
                    item.setSiteComment("");
                }

                String ruid = r.getString("UID");
                if (ruid == null || ruid.isEmpty()) {
                    ruid = String.valueOf(r.getID());
                }
                item.setUId(ruid);
                boolean isLocked = item.hasFlag(Flag.getFlag("locked"));
                if (!this.hideLocked && isLocked) {
                    result.add(item);
                } else if (!this.hideUnlocked && !isLocked) {
                    result.add(item);
                }

            }
        }
        return result;
    }

    public void addOrUpdateOperation(int idUser, String uid, Date start, Date end, String summary, String description, String location) throws SQLException {
        JCalendarItem i = this.getItemFromUid(uid);
        final SQLTable tOperation = root.getTable(ModuleOperation.TABLE_OPERATION);
        if (i == null) {
            // Create all from scratch

            SQLRowValues vOperation = new SQLRowValues(tOperation);
            vOperation.put("ID_SITE", null);
            vOperation.put("ID_USER_COMMON", idUser);
            vOperation.put("TYPE", "Inconnu");
            vOperation.put("STATUS", "Non classé");
            vOperation.put("DESCRIPTION", description);

            final SQLRow operationRow = vOperation.commit();
            SQLRow calendarGroupRow = operationRow.getForeignRow("ID_CALENDAR_ITEM_GROUP", SQLRowMode.DEFINED);

            final SQLRowValues rowItemGroup = new SQLRowValues(root.getTable("CALENDAR_ITEM_GROUP"));
            rowItemGroup.put("NAME", summary);
            rowItemGroup.put("DESCRIPTION", description);

            calendarGroupRow = rowItemGroup.commit();
            // Update Operation
            SQLRowValues operationSQLRowValues = operationRow.asRowValues();
            operationSQLRowValues.put("ID_CALENDAR_ITEM_GROUP", calendarGroupRow.getID());
            operationSQLRowValues.commit();
            // Insert Calendar Items
            DateRange dateRange = new DateRange();
            dateRange.setStart(start.getTime());
            dateRange.setStop(end.getTime());
            final SQLRowValues rowItem = new SQLRowValues(root.getTable("CALENDAR_ITEM"));
            rowItem.put("START", new Date(dateRange.getStart()));
            rowItem.put("END", new Date(dateRange.getStop()));
            rowItem.put("DURATION_S", (dateRange.getStop() - dateRange.getStart()) / 1000);
            rowItem.put("SUMMARY", summary);
            rowItem.put("DESCRIPTION", description);
            rowItem.put("LOCATION", location);
            rowItem.put("FLAGS", "");
            rowItem.put("STATUS", operationRow.getString("STATUS"));
            rowItem.put("ID_CALENDAR_ITEM_GROUP", calendarGroupRow.getID());
            rowItem.put("SOURCE_ID", operationRow.getID());
            rowItem.put("SOURCE_TABLE", ModuleOperation.TABLE_OPERATION);
            rowItem.put("UID", uid);
            rowItem.commit();
        } else {
            if (i instanceof JCalendarItemDB) {
                JCalendarItemDB item = (JCalendarItemDB) i;
                //
                final SQLRowValues vOperation = tOperation.getRow((int) item.getSourceId()).createEmptyUpdateRow();
                vOperation.put("DESCRIPTION", description);
                vOperation.commit();
                // group
                final SQLTable tGroup = root.getTable("CALENDAR_ITEM_GROUP");
                final SQLRowValues rowItemGroup = tGroup.getRow(item.getIdCalendarGroup()).createEmptyUpdateRow();
                rowItemGroup.put("NAME", summary);
                rowItemGroup.put("DESCRIPTION", description);
                rowItemGroup.commit();
                // item
                final SQLTable tItem = root.getTable("CALENDAR_ITEM");
                DateRange dateRange = new DateRange();
                dateRange.setStart(start.getTime());
                dateRange.setStop(end.getTime());
                final SQLRowValues rowItem = tItem.getRow(item.getId()).createEmptyUpdateRow();
                rowItem.put("START", new Date(dateRange.getStart()));
                rowItem.put("END", new Date(dateRange.getStop()));
                rowItem.put("DURATION_S", (dateRange.getStop() - dateRange.getStart()) / 1000);
                rowItem.put("SUMMARY", summary);
                rowItem.put("DESCRIPTION", description);
                rowItem.put("LOCATION", location);
                rowItem.commit();
            }
        }

    }

    public boolean delete(JCalendarItem item) {
        // TODO Auto-generated method stub
        return false;
    }

    public JCalendarItem getItemFromUid(String uid) {
        List<JCalendarItem> l = getItemIn(null, null, null, null, uid);
        if (l.isEmpty()) {
            System.err.println("OperationCalendarManager.getItemFromUid() nothing in db for uid: " + uid);
            return null;
        }
        return l.get(0);
    }
}
