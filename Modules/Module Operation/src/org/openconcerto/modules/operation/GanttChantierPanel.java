package org.openconcerto.modules.operation;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Calendar;

import javax.swing.JPanel;

import org.jopencalendar.ui.YearActivityPanel;
import org.openconcerto.ui.DefaultGridBagConstraints;

public class GanttChantierPanel extends JPanel {
    public GanttChantierPanel() {
        this.setOpaque(false);
        final int year = Calendar.getInstance().get(Calendar.YEAR);
        final OperationCalendarManager manager = new OperationCalendarManager("Gantt");
        final YearActivityPanel p = new YearActivityPanel(manager, year);
        p.setPrintButtonVisible(false);
        p.setOpaque(false);
        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();
        c.insets = new Insets(0, 0, 0, 0);
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 1;
        this.add(p, c);

    }
}
