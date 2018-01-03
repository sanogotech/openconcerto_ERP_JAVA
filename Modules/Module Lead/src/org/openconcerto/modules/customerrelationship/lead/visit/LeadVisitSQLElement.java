package org.openconcerto.modules.customerrelationship.lead.visit;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.openconcerto.erp.modules.AbstractModule;
import org.openconcerto.erp.modules.ModuleElement;
import org.openconcerto.modules.customerrelationship.lead.GroupSQLComponentWithService;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.ui.JDate;
import org.openconcerto.ui.JLabelBold;
import org.openconcerto.utils.ListMap;

public class LeadVisitSQLElement extends ModuleElement {

    public LeadVisitSQLElement(AbstractModule module) {
        super(module, "LEAD_VISIT");
    }

    @Override
    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("DATE");
        l.add("ID_LEAD");
        l.add("NEXTCONTACT_DATE");
        return l;
    }

    @Override
    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("DATE");
        l.add("ID_LEAD");
        return l;
    }

    @Override
    public ListMap<String, String> getShowAs() {
        return ListMap.singleton(null, getComboFields());
    }

    @Override
    public SQLComponent createComponent() {
        return new GroupSQLComponentWithService(this, new LeadVisitGroup()) {

            @Override
            public JComponent getLabel(String id) {
                if (id.equals("customerrelationship.lead.visit.content")) {
                    return new JLabelBold("Description de la visite");
                } else if (id.equals("customerrelationship.lead.visit.next")) {
                    return new JLabelBold("Suite à donner");

                }
                return super.getLabel(id);
            }

            @Override
            public JComponent createEditor(String id) {
                if (id.equals("INFORMATION")) {
                    final JTextArea jTextArea = new JTextArea();
                    jTextArea.setFont(new JLabel().getFont());
                    jTextArea.setMinimumSize(new Dimension(200, 150));
                    jTextArea.setPreferredSize(new Dimension(200, 150));
                    return new JScrollPane(jTextArea);
                } else if (id.equals("NEXTCONTACT_INFO")) {
                    final JTextArea jTextArea = new JTextArea();
                    jTextArea.setFont(new JLabel().getFont());
                    jTextArea.setMinimumSize(new Dimension(200, 50));
                    jTextArea.setPreferredSize(new Dimension(200, 50));
                    return new JScrollPane(jTextArea);
                } else if (id.equals("DATE")) {
                    return new JDate(true);
                }
                return super.createEditor(id);
            }

        };
    }
}
