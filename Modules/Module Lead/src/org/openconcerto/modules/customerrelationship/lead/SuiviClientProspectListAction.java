package org.openconcerto.modules.customerrelationship.lead;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Arrays;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.openconcerto.erp.action.CreateFrameAbstractAction;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.view.ListeAddPanel;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.PanelFrame;

public class SuiviClientProspectListAction extends CreateFrameAbstractAction {

    private SQLElement eltCustomerCall = Configuration.getInstance().getDirectory().getElement(Module.TABLE_CUSTOMER_CALL);
    private SQLElement eltCustomerVisit = Configuration.getInstance().getDirectory().getElement(Module.TABLE_CUSTOMER_VISIT);
    private SQLElement eltLeadCall = Configuration.getInstance().getDirectory().getElement(Module.TABLE_LEAD_CALL);
    private SQLElement eltLeadVisit = Configuration.getInstance().getDirectory().getElement(Module.TABLE_LEAD_VISIT);

    @Override
    public JFrame createFrame() {
        PanelFrame frame = new PanelFrame(getPanel(), "Suivi Prospects-Clients");
        return frame;
    }

    public SuiviClientProspectListAction() {
        super("Suivi Prospects-Clients");
    }

    public JPanel getPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();
        c.fill = GridBagConstraints.NONE;
        final JTabbedPane tabbedPane = new JTabbedPane();
        final List<SQLElement> elts = Arrays.asList(this.eltLeadCall, this.eltCustomerCall, this.eltLeadVisit, this.eltCustomerVisit);
        for (SQLElement sqlElement : elts) {
            final ListeAddPanel pane = new ListeAddPanel(sqlElement);
            pane.getListe().setOpaque(false);
            pane.setOpaque(false);
            tabbedPane.add(sqlElement.getPluralName(), pane);
        }
        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = GridBagConstraints.REMAINDER;
        panel.add(tabbedPane, c);
        return panel;
    }

}
