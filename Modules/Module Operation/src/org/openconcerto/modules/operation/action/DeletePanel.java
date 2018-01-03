package org.openconcerto.modules.operation.action;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import org.jopencalendar.model.JCalendarItemPart;
import org.openconcerto.modules.operation.JCalendarItemDB;
import org.openconcerto.modules.operation.ModuleOperation;
import org.openconcerto.modules.operation.OperationSQLElement;
import org.openconcerto.sql.PropsConfiguration;
import org.openconcerto.sql.model.DBSystemRoot;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.utils.ExceptionHandler;

public class DeletePanel extends JPanel {

    private List<JCalendarItemPart> selectedItems;

    public DeletePanel(List<JCalendarItemPart> selectedItems) {
        this.selectedItems = selectedItems;
        this.setLayout(new GridBagLayout());
        GridBagConstraints c = new DefaultGridBagConstraints();
        c.gridwidth = 2;
        String labelTitle = "Effacer les " + selectedItems.size() + " éléments";
        if (selectedItems.size() == 1) {
            labelTitle = "Effacer l'élément";
        }
        this.add(new JLabel(labelTitle), c);
        String choice1 = "sélectionnés uniquement";
        if (selectedItems.size() == 1) {
            choice1 = "sélectionné uniquement";
        }
        final JRadioButton radio1 = new JRadioButton(choice1);
        c.gridy++;
        this.add(radio1, c);
        String choice2 = "sélectionnés et ceux planifiés plus tard";
        if (selectedItems.size() == 1) {
            choice2 = "sélectionné et ceux planifiés plus tard";
        }
        final JRadioButton radio2 = new JRadioButton(choice2);
        c.gridy++;
        this.add(radio2, c);
        c.gridy++;

        c.anchor = GridBagConstraints.SOUTHEAST;
        c.fill = GridBagConstraints.NONE;
        c.gridwidth = 1;
        c.weighty = 1;
        c.weightx = 1;
        final JButton deleteButton = new JButton("Effacer");
        this.add(deleteButton, c);
        c.gridx++;
        c.weightx = 0;
        final JButton cancelButton = new JButton("Annuler");
        this.add(cancelButton, c);
        // Group
        final ButtonGroup group = new ButtonGroup();
        group.add(radio1);
        group.add(radio2);
        radio1.setSelected(true);
        deleteButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                deleteButton.setEnabled(false);
                if (radio1.isSelected()) {
                    deleteSelected();
                } else {
                    deleteSelectedAndFutureEvents();
                }

            }
        });
        cancelButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                SwingUtilities.getWindowAncestor(DeletePanel.this).dispose();
            }
        });
    }

    protected void deleteSelected() {
        final Set<JCalendarItemDB> toDelete = ModuleOperation.getItemDB(this.selectedItems);
        final List<Long> ids = ModuleOperation.getOperationIdsFrom(toDelete);
        deleteInBackgroundOperations(ids);
    }

    @SuppressWarnings("unchecked")
    protected void deleteSelectedAndFutureEvents() {
        Set<JCalendarItemDB> toDelete = ModuleOperation.getItemDB(this.selectedItems);
        final List<Long> ids = ModuleOperation.getOperationIdsFrom(toDelete);
        if (ids.size() != 1) {
            JOptionPane.showMessageDialog(this, "Vous devez sélectionner uniquement des interventions sur le même site d'intervention!");
            SwingUtilities.getWindowAncestor(DeletePanel.this).dispose();
            return;
        }

        final JCalendarItemDB firstItem = toDelete.iterator().next();
        int idUser = (Integer) firstItem.getUserId();

        Calendar startDate = firstItem.getDtStart();
        for (JCalendarItemDB item : toDelete) {
            if (item.getDtStart().before(startDate)) {
                startDate = item.getDtStart();
            }
        }
        final SQLTable tableOperation = firstItem.getSourceElement().getTable();
        final DBSystemRoot dbSystemRoot = tableOperation.getDBSystemRoot();

        SQLRow rowOperation = tableOperation.getRow((int) firstItem.getSourceId());
        int idSite = rowOperation.getForeignID("ID_SITE");

        // plannerUID de OPERATION
        String plannerUID = firstItem.getPlannerUID();

        final SQLSelect select = new SQLSelect();
        select.addSelect(tableOperation.getField("ID_CALENDAR_ITEM_GROUP"));
        if (plannerUID == null || plannerUID.trim().isEmpty()) {
            // Recuperation des groupes correspondants au meme site et intervenant
            select.setWhere(new Where(tableOperation.getField("ID_SITE"), "=", idSite).and(new Where(tableOperation.getField("ID_USER_COMMON"), "=", idUser)));
        } else {
            // Recuperation des groupes correspondants au meme plannerUID
            select.setWhere(new Where(tableOperation.getField("PLANNER_UID"), "LIKE", plannerUID));
        }

        String query = select.asString();
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
        select3.addSelect(tableOperation.getKey());
        select3.setWhere(new Where(tableOperation.getField("ID_CALENDAR_ITEM_GROUP"), true, calendarGroupIdsToDelete));
        String query3 = select3.asString();
        List<Number> operationIdsToDelete = (List<Number>) dbSystemRoot.getDataSource().executeCol(query3);
        Set<Long> oids = new HashSet<Long>();
        for (Number n : operationIdsToDelete) {
            oids.add(n.longValue());
        }
        final List<Long> oidsToDelete = new ArrayList<Long>();
        oidsToDelete.addAll(oids);
        Collections.sort(oidsToDelete);

        deleteInBackgroundOperations(oidsToDelete);
    }

    public void deleteInBackgroundOperations(final List<Long> ids) {
        Collections.sort(ids);
        SwingWorker<Object, Object> worker = new SwingWorker<Object, Object>() {

            @Override
            protected Object doInBackground() throws Exception {
                final OperationSQLElement elem = (OperationSQLElement) PropsConfiguration.getInstance().getDirectory().getElement(ModuleOperation.TABLE_OPERATION);
                try {
                    elem.fastDelete(ids);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return true;
            }

            protected void done() {
                try {
                    get();
                } catch (Exception e) {
                    ExceptionHandler.handle("Error while deleting", e);
                }

                ModuleOperation.reloadCalendars();
                SwingUtilities.getWindowAncestor(DeletePanel.this).dispose();
            };
        };
        worker.execute();
    }

}
