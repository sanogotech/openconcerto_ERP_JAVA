package org.jopencalendar.ui;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.SwingWorker;
import javax.swing.table.AbstractTableModel;

import org.jopencalendar.LayoutUtils;
import org.jopencalendar.model.JCalendarItem;
import org.jopencalendar.model.JCalendarItemGroup;

public class YearActivityTableModel extends AbstractTableModel {
    private List<JCalendarItemGroup> groups = new ArrayList<JCalendarItemGroup>();
    private List<List<MonthActivityStates>> states;
    private int year;
    private JCalendarItemProvider calendarManager;

    public YearActivityTableModel(JCalendarItemProvider c, int year) {
        this.calendarManager = c;
        loadContent(year);
    }

    public void loadContent(final int year) {
        loadContent(year, 1, 53);
    }

    public void loadContent(final int year, final int week1, final int week2) {
        this.year = year;
        final SwingWorker<List<JCalendarItemGroup>, Object> w = new SwingWorker<List<JCalendarItemGroup>, Object>() {

            @Override
            protected List<JCalendarItemGroup> doInBackground() throws Exception {
                final List<JCalendarItem> item = calendarManager.getItemInYear(year, week1, week2);
                Collections.sort(item, new Comparator<JCalendarItem>() {
                    @Override
                    public int compare(JCalendarItem o1, JCalendarItem o2) {
                        return o1.getDtStart().compareTo(o2.getDtStart());
                    }

                });

                final List<JCalendarItemGroup> groups = JCalendarItemGroup.getGroups(item);
                return groups;
            }

            protected void done() {
                try {
                    final List<JCalendarItemGroup> l = get();
                    setItems(l);
                } catch (Exception e) {
                    throw new IllegalStateException("error while getting items", e);
                }

            };
        };
        w.execute();
    }

    protected void setItems(List<JCalendarItemGroup> groups) {
        this.groups = new ArrayList<JCalendarItemGroup>(groups);
        this.states = new ArrayList<List<MonthActivityStates>>(groups.size());
        for (JCalendarItemGroup group : groups) {
            final List<MonthActivityStates> s = createMonthActivityStates(group);
            this.states.add(s);
        }
        fireTableDataChanged();
    }

    private List<MonthActivityStates> createMonthActivityStates(JCalendarItemGroup group) {
        final Calendar monthStart = Calendar.getInstance();
        monthStart.clear();
        monthStart.set(Calendar.YEAR, this.year);
        monthStart.set(Calendar.DAY_OF_YEAR, 1);
        final Calendar monthEnd = Calendar.getInstance();
        monthEnd.clear();
        monthEnd.set(Calendar.YEAR, this.year);
        monthEnd.set(Calendar.DAY_OF_YEAR, 1);
        monthEnd.add(Calendar.MONTH, 1);
        final List<MonthActivityStates> result = new ArrayList<MonthActivityStates>(12);
        for (int i = 0; i < 12; i++) {
            MonthActivityStates s = new MonthActivityStates(i, year);

            int size = group.getItemCount();
            for (int j = 0; j < size; j++) {
                JCalendarItem jCalendarItem = group.getItem(j);
                Calendar start = jCalendarItem.getDtStart();
                Calendar end = jCalendarItem.getDtEnd();
                if (!(end.compareTo(monthStart) <= 0 || start.compareTo(monthEnd) >= 0)) {
                    s.set(jCalendarItem);
                }
            }
            result.add(s);

            monthStart.add(Calendar.MONTH, 1);
            monthEnd.add(Calendar.MONTH, 1);
        }

        return result;
    }

    @Override
    public String getColumnName(int columnIndex) {
        if (columnIndex == 0) {
            return "";
        }
        return LayoutUtils.firstUp(DateFormatSymbols.getInstance().getMonths()[columnIndex - 1]) + " " + this.year;
    }

    public JCalendarItemGroup getJCalendarItemGroupAt(int rowIndex) {
        return this.groups.get(rowIndex);
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (columnIndex == 0) {
            return this.groups.get(rowIndex);
        }
        return this.states.get(rowIndex).get(columnIndex - 1);
    }

    @Override
    public int getRowCount() {
        return this.groups.size();
    }

    @Override
    public int getColumnCount() {
        return 13;
    }
};