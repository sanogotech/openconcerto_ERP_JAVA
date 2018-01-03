package org.openconcerto.modules.customerrelationship.lead.visit;

import org.openconcerto.erp.modules.AbstractModule;
import org.openconcerto.modules.customerrelationship.lead.JoinServiceSQLElement;

public class LeadVisitServiceSQLElement extends JoinServiceSQLElement {
    public LeadVisitServiceSQLElement(AbstractModule module) {
        super(module, "LEAD_VISIT");
    }
}
