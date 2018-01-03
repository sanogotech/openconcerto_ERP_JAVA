package org.openconcerto.modules.customerrelationship.lead.visit;

import org.openconcerto.erp.modules.AbstractModule;
import org.openconcerto.modules.customerrelationship.lead.JoinServiceSQLElement;

public class CustomerVisitServiceSQLElement extends JoinServiceSQLElement {
    public CustomerVisitServiceSQLElement(AbstractModule module) {
        super(module, "CUSTOMER_VISIT");
    }
}
