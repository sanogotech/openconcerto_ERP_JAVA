package org.openconcerto.modules.customerrelationship.lead.call;

import org.openconcerto.erp.modules.AbstractModule;
import org.openconcerto.modules.customerrelationship.lead.JoinServiceSQLElement;

public class CustomerCallServiceSQLElement extends JoinServiceSQLElement {
    public CustomerCallServiceSQLElement(AbstractModule module) {
        super(module, "CUSTOMER_CALL");
    }
}
