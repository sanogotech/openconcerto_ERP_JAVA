package org.openconcerto.modules.operation;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.jopencalendar.model.JCalendarItemPart;
import org.jopencalendar.ui.ItemPartHoverListener;
import org.jopencalendar.ui.MultipleDayView;
import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLTableEvent;
import org.openconcerto.sql.model.SQLTableModifiedListener;
import org.openconcerto.sql.users.User;
import org.openconcerto.sql.view.EditFrame;
import org.openconcerto.sql.view.EditPanel.EditMode;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.FrameUtil;

/**
 * Planning d'un jour, pour chaque personne
 */
public class DailyOperationCalendarPanel extends AbstractCalendarPanel {

    private MultipleDayCalendarWithToolBar calendar;

    private OperationDayView mDayView;

    public DailyOperationCalendarPanel() {

        this.setLayout(new BorderLayout());
        JPanel toolbar = new JPanel();
        toolbar.setLayout(new FlowLayout(FlowLayout.LEFT));
        toolbar.setOpaque(false);

        //
        final JButton bCreate = new JButton("Ajouter une intervention");
        bCreate.setOpaque(false);
        toolbar.add(bCreate);
        //
        final JButton bPlan = new JButton("Plannifier des interventions");
        bPlan.setOpaque(false);
        toolbar.add(bPlan);
        //
        this.add(toolbar, BorderLayout.NORTH);

        final JPanel filters = createFilterPanel();
        split.setLeftComponent(filters);
        mDayView = new OperationDayView();
        calendar = new MultipleDayCalendarWithToolBar(mDayView);
        final MultipleDayView w = calendar.getWeekView();
        w.setHourRange(HOUR_START, HOUR_STOP);
        w.addItemPartHoverListener(new ItemPartHoverListener() {

            @Override
            public void mouseOn(JCalendarItemPart newSelection) {
                if (newSelection == null) {
                    final int location = split.getDividerLocation();
                    split.setLeftComponent(filters);
                    split.setDividerLocation(location);
                } else {

                    final int location = split.getDividerLocation();
                    final JCalendarItemInfoPanel comp = new JCalendarItemInfoPanel(newSelection.getItem());
                    split.setLeftComponent(comp);
                    split.setDividerLocation(location);
                }

            }
        });

        split.setRightComponent(calendar);
        split.setDividerLocation(250);
        this.add(split, BorderLayout.CENTER);
        ComptaPropsConfiguration.getInstanceCompta().getRootSociete().getTable("CALENDAR_ITEM").addTableModifiedListener(new SQLTableModifiedListener() {

            @Override
            public void tableModified(SQLTableEvent evt) {
                calendar.reload();
            }

        });

        final SQLElement element = ComptaPropsConfiguration.getInstanceCompta().getDirectory().getElement(ModuleOperation.TABLE_OPERATION);
        // Create a new operation
        bCreate.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                final OperationSQLComponent comp = new OperationSQLComponent(element) {
                    @Override
                    public void select(SQLRowAccessor r) {
                        super.select(r);
                        setDate(DailyOperationCalendarPanel.this.mDayView.getDate());
                    }
                };
                final EditFrame f = new EditFrame(comp, EditMode.CREATION);

                FrameUtil.show(f);
            }
        });
        // Plan multiple operations
        bPlan.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                final MultiOperationSQLComponent comp = new MultiOperationSQLComponent(element);
                final EditFrame f = new EditFrame(comp, EditMode.CREATION);
                f.setTitle("Plannication d'interventions");
                FrameUtil.show(f);
            }
        });
        calendar.scrollTo(8);
        registerCalendarItemListener(calendar.getWeekView());

        calendar.getWeekView().setPopupMenuProvider(new OperationMenuProvider());
    }

    public void reload() {
        this.mDayView.reload();
    }

    protected JPanel createFilterPanel() {

        final JPanel p = new JPanel();
        p.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        p.add(new JLabel("Etats"), c);
        c.gridy++;
        c.weighty = 1;
        final OperationStateListModel statesModel = new OperationStateListModel();
        statesList = new CheckList<String>(statesModel);
        p.add(new JScrollPane(statesList), c);
        c.gridy++;
        c.weighty = 0;
        p.add(new JLabel("Employés"), c);
        c.gridy++;
        c.weighty = 1;
        usersModel = new UserOperationListModel(this);
        usesrList = new CheckList<User>(usersModel);
        p.add(new JScrollPane(usesrList), c);
        c.gridy++;
        c.weighty = 0;
        final JCheckBox check1 = new JCheckBox("masquer les verrouillés");
        p.add(check1, c);
        c.gridy++;
        final JCheckBox check2 = new JCheckBox("masquer les déverrouillés");
        p.add(check2, c);
        statesModel.loadContent();
        usersModel.loadContent();

        p.setMinimumSize(new Dimension(200, 200));
        p.setPreferredSize(new Dimension(200, 200));

        final PropertyChangeListener listener = new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                List<User> users = null;
                if (!usesrList.isAllSelected()) {
                    users = getSelectedUsers();
                }
                List<String> states = null;
                if (!statesList.isAllSelected()) {
                    states = getSelectedStates();
                }
                mDayView.setFilter(users, states, check1.isSelected(), check2.isSelected());
                calendar.reload();

            }
        };
        statesList.addPropertyChangeListener("checked", listener);
        usesrList.addPropertyChangeListener("checked", listener);
        check1.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                listener.propertyChange(null);
            }
        });
        check2.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                listener.propertyChange(null);
            }
        });
        return p;
    }
}
