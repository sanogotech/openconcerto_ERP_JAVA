package org.openconcerto.modules.customerrelationship.lead.call;

import org.openconcerto.erp.modules.AbstractModule;
import org.openconcerto.modules.customerrelationship.lead.JoinServiceSQLElement;

public class LeadCallServiceSQLElement extends JoinServiceSQLElement {
    public LeadCallServiceSQLElement(AbstractModule module) {
        super(module, "LEAD_CALL");
    }
}
