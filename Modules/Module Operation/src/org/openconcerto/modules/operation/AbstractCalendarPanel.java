package org.openconcerto.modules.operation;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingWorker;

import org.jopencalendar.model.JCalendarItem;
import org.jopencalendar.ui.MultipleDayView;
import org.jopencalendar.ui.WeekView;
import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLRowValuesListFetcher;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.users.User;
import org.openconcerto.sql.users.UserManager;
import org.openconcerto.utils.ExceptionHandler;

public class AbstractCalendarPanel extends JPanel implements OperationCalendar {
    protected CheckList<String> statesList;
    protected CheckList<User> usesrList;
    final protected Map<User, String> userInfo = new HashMap<User, String>();
    protected UserOperationListModel usersModel;
    final protected JSplitPane split = new JSplitPane();
    public static final int HOUR_START = 5;
    public static final int HOUR_STOP = 21;

    public synchronized String getUserInfo(User user) {
        return userInfo.get(user);
    }

    public List<String> getSelectedStates() {
        return this.statesList.getSelectedObjects();
    }

    public List<User> getSelectedUsers() {
        return this.usesrList.getSelectedObjects();
    }

    protected String formatDuration(int durationMinute) {
        int h = durationMinute / 60;
        int m = durationMinute % 60;
        if (m != 0) {
            String mS = String.valueOf(m);
            if (m < 10) {
                mS = "0" + mS;
            }
            return h + ":" + mS;
        }
        return String.valueOf(h);
    }

    public void registerCalendarItemListener(MultipleDayView multipleDayView) {
        multipleDayView.addPropertyChangeListener(WeekView.CALENDARD_ITEMS_PROPERTY, new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                @SuppressWarnings("unchecked")
                final List<List<JCalendarItem>> items = new ArrayList<List<JCalendarItem>>((List<List<JCalendarItem>>) evt.getNewValue());
                SwingWorker<Map<User, String>, Object> worker = new SwingWorker<Map<User, String>, Object>() {

                    @Override
                    protected Map<User, String> doInBackground() throws Exception {
                        final Map<User, String> uInfo = new HashMap<User, String>();

                        final SQLTable tSalarie = ComptaPropsConfiguration.getInstanceCompta().getDirectory().getElement("SALARIE").getTable();

                        final SQLRowValues v = new SQLRowValues(tSalarie);
                        v.putNulls("NOM", "PRENOM");
                        v.putRowValues("ID_INFOS_SALARIE_PAYE").putNulls("DUREE_HEBDO");
                        final List<SQLRowValues> rows = SQLRowValuesListFetcher.create(v).fetch();
                        final List<User> users = UserManager.getInstance().getAllActiveUsers();
                        final int size = users.size();
                        for (int i = 0; i < size; i++) {

                            final User u = users.get(i);
                            final String name = u.getName().trim();
                            final String firstName = u.getFirstName();
                            for (SQLRowValues row : rows) {

                                if (row.getString("NOM").trim().equalsIgnoreCase(name) && row.getString("PRENOM").trim().equalsIgnoreCase(firstName)) {

                                    final int durationMinute = (int) row.getForeign("ID_INFOS_SALARIE_PAYE").getFloat("DUREE_HEBDO") * 60;

                                    // Durée plannifiée
                                    final int d = OperationCalendarPanel.getDuration(items, u);
                                    // Durée verrouillée
                                    int d2 = OperationCalendarPanel.getDurationLocked(items, u);
                                    uInfo.put(u, "[" + formatDuration(d2) + " / " + formatDuration(d) + " / " + formatDuration(durationMinute) + "]");
                                }
                            }
                        }
                        return uInfo;
                    }

                    @Override
                    protected void done() {
                        synchronized (AbstractCalendarPanel.this) {
                            Map<User, String> uInfo;
                            try {
                                uInfo = get();
                                userInfo.clear();
                                userInfo.putAll(uInfo);
                                usersModel.refresh();
                            } catch (Exception e) {
                                ExceptionHandler.handle("Cannot update user info", e);
                            }

                        }
                    }
                };
                worker.execute();

            }
        });
    }
}