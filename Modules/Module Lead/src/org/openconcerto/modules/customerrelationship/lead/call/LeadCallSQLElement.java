package org.openconcerto.modules.customerrelationship.lead.call;

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

public class LeadCallSQLElement extends ModuleElement {

    public LeadCallSQLElement(AbstractModule module) {
        super(module, "LEAD_CALL");
    }

    @Override
    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("DATE");
        l.add("ID_LEAD");
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
    public SQLComponent createComponent() {
        return new GroupSQLComponentWithService(this, new LeadCallGroup()) {
            @Override
            public JComponent getLabel(String id) {
                if (id.equals("customerrelationship.lead.call.content")) {
                    return new JLabelBold("Description de l'appel téléphonique");
                }else if (id.equals("customerrelationship.lead.call.next")) {
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
                } else if (id.equals("DATE")) {
                    return new JDate(true);
                }
                return super.createEditor(id);
            }

        };
    }

}
