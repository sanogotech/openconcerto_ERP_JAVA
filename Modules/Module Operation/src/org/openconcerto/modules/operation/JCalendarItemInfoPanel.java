package org.openconcerto.modules.operation;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.text.DateFormat;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.jopencalendar.model.JCalendarItem;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.component.ITextArea;
import org.openconcerto.ui.date.DateRangePlannerPanel;

import com.lowagie.text.Font;

public class JCalendarItemInfoPanel extends JPanel {

    public JCalendarItemInfoPanel(JCalendarItem item) {
        JCalendarItemDB itemDb = (JCalendarItemDB) item;

        this.setLayout(new GridBagLayout());
        GridBagConstraints c = new DefaultGridBagConstraints();
        final ITextArea comp = new ITextArea(item.getSummary());
        comp.setFont(comp.getFont().deriveFont(Font.BOLD));
        comp.setEditable(false);
        comp.setOpaque(false);
        comp.setBorder(null);
        this.add(comp, c);
        c.gridy++;
        c.weightx = 1;
        DateFormat spf = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT);
        this.add(new JLabel("Debut : " + spf.format(item.getDtStart().getTime())), c);
        c.gridy++;
        this.add(new JLabel("Fin : " + spf.format(item.getDtEnd().getTime())), c);
        // Utilisateur
        c.gridy++;

        c.fill = GridBagConstraints.BOTH;
        SQLRowValues cookie = (SQLRowValues) item.getCookie();
        final JLabel c3 = new JLabel("Par : " + cookie.getString("NOM"));
        this.add(c3, c);
        c.gridy++;
        final String plannerXML = ((JCalendarItemDB) item).getPlannerXML();
        if (plannerXML != null && !plannerXML.trim().isEmpty()) {
            final String descriptionFromXML = "Plannification : " + DateRangePlannerPanel.getDescriptionFromXML(plannerXML).toLowerCase();
            final ITextArea comp1 = new ITextArea(descriptionFromXML);
            comp1.setOpaque(false);
            comp1.setBorder(null);
            comp1.setEditable(false);
            this.add(comp1, c);
        }

        //
        c.gridy++;
        c.fill = GridBagConstraints.BOTH;
        final JLabel c4 = new JLabel("Type : " + itemDb.getType());
        this.add(c4, c);
        c.gridy++;
        final JLabel c5 = new JLabel("Etat : " + itemDb.getStatus());
        this.add(c5, c);
        // Localisation
        if (item.getLocation() != null && !item.getLocation().trim().isEmpty()) {
            c.gridy++;
            c.weighty = 0;
            final ITextArea comp1 = new ITextArea(item.getLocation());
            comp1.setOpaque(false);
            comp1.setBorder(null);
            comp1.setEditable(false);
            this.add(comp1, c);
        }

        c.gridy++;
        c.weighty = 1;
        final ITextArea comp2 = new ITextArea(item.getDescription());
        comp2.setOpaque(false);
        comp2.setBorder(null);
        comp2.setEditable(false);
        this.add(comp2, c);

    }
}
