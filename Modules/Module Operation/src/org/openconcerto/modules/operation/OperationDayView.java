package org.openconcerto.modules.operation;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import org.jopencalendar.model.Flag;
import org.jopencalendar.model.JCalendarItem;
import org.jopencalendar.ui.MultipleDayView;
import org.openconcerto.sql.users.User;
import org.openconcerto.sql.users.UserManager;
import org.openconcerto.utils.StringUtils;

/*
 * Planning journalier (planning des différentes personnes pour 1 jour précis)
 **/
public class OperationDayView extends MultipleDayView {

    private Date date;
    private List<User> filterUsers;
    private List<String> filterStates;
    private boolean hideLocked = false;
    private boolean hideUnlocked = false;

    @Override
    public String getColumnTitle(int index) {
        return getShownUsers().get(index).getFullName();
    }

    @Override
    public int getColumnCount() {
        return getShownUsers().size();
    }

    List<User> getShownUsers() {
        List<User> allUser = filterUsers;
        if (filterUsers == null) {
            allUser = UserManager.getInstance().getAllActiveUsers();
            Collections.sort(allUser, new UserComparator());
        }
        return allUser;
    }

    @Override
    public String getTitle() {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        int week = c.get(Calendar.WEEK_OF_YEAR);
        return StringUtils.firstUp(DateFormat.getDateInstance(DateFormat.FULL).format(date)) + ", semaine " + week;
    }

    @Override
    public void reload() {
        final List<User> selectedUsers = getShownUsers();
        final List<String> selectedStates = filterStates;
        final SwingWorker<List<List<JCalendarItem>>, Object> w = new SwingWorker<List<List<JCalendarItem>>, Object>() {

            @Override
            protected List<List<JCalendarItem>> doInBackground() throws Exception {
                final OperationCalendarManager m = new OperationCalendarManager("");
                final Calendar c = Calendar.getInstance();
                c.setTime(date);
                // Set to 00:00:00
                c.set(Calendar.MILLISECOND, 0);
                c.set(Calendar.SECOND, 0);
                c.set(Calendar.MINUTE, 0);
                c.set(Calendar.HOUR_OF_DAY, 0);
                final Date date1 = c.getTime();
                // Add 24 hours
                c.add(Calendar.HOUR_OF_DAY, 24);
                final Date date2 = c.getTime();
                final List<JCalendarItem> completeList = m.getItemIn(date1, date2, selectedUsers, selectedStates);
                //
                final List<List<JCalendarItem>> r = new ArrayList<List<JCalendarItem>>();
                for (User u : selectedUsers) {
                    final List<JCalendarItem> l = new ArrayList<JCalendarItem>();
                    r.add(l);
                    for (JCalendarItem item : completeList) {
                        boolean isLocked = item.hasFlag(Flag.getFlag("locked"));
                        if (((Number) item.getUserId()).intValue() == u.getId()) {
                            if (!hideLocked && isLocked) {
                                l.add(item);
                            } else if (!hideUnlocked && !isLocked) {
                                l.add(item);
                            }
                        }

                    }
                }
                return r;
            }

            protected void done() {
                try {
                    List<List<JCalendarItem>> items = get();
                    setItems(items);
                } catch (Exception e) {
                    throw new IllegalStateException("error while getting items", e);
                }

            };
        };
        w.execute();

    }

    public void loadDay(Date date) {
        this.date = date;
        this.reload();
    }

    public void setFilter(List<User> users, List<String> states, boolean hideLocked, boolean hideUnlocked) {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException();
        }
        this.filterUsers = users;
        this.filterStates = states;
        this.hideLocked = hideLocked;
        this.hideUnlocked = hideUnlocked;
        reload();
    }

    public Date getDate() {
        return new Date(date.getTime());
    }
}
