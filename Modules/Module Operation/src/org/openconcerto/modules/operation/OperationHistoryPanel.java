package org.openconcerto.modules.operation;

import java.awt.GridLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.reports.history.ui.ListeHistoriquePanel;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.request.ComboSQLRequest;
import org.openconcerto.sql.users.rights.JListSQLTablePanel;
import org.openconcerto.sql.view.IListPanel;
import org.openconcerto.sql.view.list.IListe;
import org.openconcerto.utils.SwingWorker2;
import org.openconcerto.utils.cc.ITransformer;

public class OperationHistoryPanel extends JPanel {
    OperationHistoryPanel() {
        this.setLayout(new GridLayout(1, 1));
        final SQLBase b = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete();
        final Map<String, List<String>> mapList = new HashMap<String, List<String>>();
        mapList.put("Interventions", Arrays.asList("OPERATION"));

        final Map<SQLTable, SQLField> map = new HashMap<SQLTable, SQLField>();
        final ComboSQLRequest comboRequest = JListSQLTablePanel.createComboRequest(Configuration.getInstance().getDirectory().getElement(b.getTable("SITE")), true);

        JPanel panel = new JPanel();
        panel.add(new JLabel("Année"));
        int year = Calendar.getInstance().get(Calendar.YEAR);
        final JSpinner spiner = new JSpinner(new SpinnerNumberModel(year, 1000, year + 20, 1));
        panel.add(spiner);

        final ListeHistoriquePanel listHistoriquePanel = new ListeHistoriquePanel("Interventions", comboRequest, mapList, panel, map, null);
        final IListe list = listHistoriquePanel.getListe(0);
        updateWhere(comboRequest, list, year, listHistoriquePanel);

        spiner.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                final int selectedYear = ((Number) spiner.getValue()).intValue();
                updateWhere(comboRequest, list, selectedYear, listHistoriquePanel);
            }

        });

        list.addSelectionDataListener(new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                final List<SQLRowValues> selectedRows = list.getSelectedRows();
                final IListPanel listePanel = listHistoriquePanel.getListePanel(0);
                if (selectedRows != null && !selectedRows.isEmpty()) {
                    final Set<Long> idsCalendarItemGroup = new HashSet<Long>();
                    for (SQLRowValues sqlRowValues : selectedRows) {
                        idsCalendarItemGroup.add(Long.valueOf(sqlRowValues.getForeign("ID_CALENDAR_ITEM_GROUP").getID()));
                    }
                    final SQLTable table = ((ComptaPropsConfiguration) Configuration.getInstance()).getRootSociete().getTable("CALENDAR_ITEM");
                    final SQLField flag = table.getField("FLAGS");
                    final SQLField group = table.getField("ID_CALENDAR_ITEM_GROUP");
                    final SQLSelect select = new SQLSelect();
                    select.addSelect(flag);
                    select.addSelect(group);
                    Where where = new Where(group, idsCalendarItemGroup).and(new Where(flag, "LIKE", "%locked%"));

                    select.setWhere(where);

                    final SwingWorker2<Boolean, String> w = new SwingWorker2<Boolean, String>() {

                        @Override
                        protected Boolean doInBackground() throws Exception {
                            @SuppressWarnings("rawtypes")
                            final List l = b.getDataSource().execute(select.asString());
                            return l.isEmpty();
                        }

                        protected void done() {
                            Boolean b;
                            try {
                                b = get();
                                listePanel.setModifyVisible(b);
                                listePanel.setDeleteVisible(b);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            } catch (ExecutionException e) {
                                e.printStackTrace();
                            }

                        };
                    };
                    w.execute();

                }

            }
        });

        this.add(listHistoriquePanel);

    }

    public void updateWhere(final ComboSQLRequest comboRequest, final IListe list, final int selectedYear, final ListeHistoriquePanel panel) {

        try {
            list.getRequest().setSelectTransf(new ITransformer<SQLSelect, SQLSelect>() {

                @Override
                public SQLSelect transformChecked(SQLSelect input) {
                    if (panel.getSelectedRow() == null) {
                        return input;
                    }

                    try {
                        Calendar cal = Calendar.getInstance();
                        cal.clear();

                        cal.set(Calendar.YEAR, selectedYear);
                        Date dStart = cal.getTime();
                        cal.set(Calendar.YEAR, selectedYear + 1);
                        Date dEnd = cal.getTime();

                        final SQLTable groupT = comboRequest.getPrimaryTable().getTable("CALENDAR_ITEM_GROUP");
                        final SQLTable calItemT = comboRequest.getPrimaryTable().getTable("CALENDAR_ITEM");
                        final List<?> dateGroupIDs;
                        {
                            final SQLSelect copy = new SQLSelect(input);
                            copy.clearSelect();
                            copy.addSelect(copy.getAlias(groupT.getKey()));
                            copy.setWhere(copy.getAlias(comboRequest.getPrimaryTable().getTable("OPERATION").getField("ID_SITE")), "=", panel.getSelectedRow().getID());
                            final List<?> allGroupIDs = calItemT.getDBSystemRoot().getDataSource().executeCol(copy.asString());

                            final SQLSelect selIDGroup = new SQLSelect();
                            selIDGroup.addSelect(calItemT.getField("ID_CALENDAR_ITEM_GROUP"));
                            final Where where = new Where(calItemT.getField("START"), dStart, true, dEnd, true);
                            selIDGroup.setWhere(where).andWhere(new Where(calItemT.getField("ID_CALENDAR_ITEM_GROUP"), allGroupIDs));
                            dateGroupIDs = calItemT.getDBSystemRoot().getDataSource().executeCol(selIDGroup.asString());
                        }

                        input.setWhere(new Where(input.getAlias(groupT.getKey()), dateGroupIDs));
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                    return input;
                }
            });
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }
}
