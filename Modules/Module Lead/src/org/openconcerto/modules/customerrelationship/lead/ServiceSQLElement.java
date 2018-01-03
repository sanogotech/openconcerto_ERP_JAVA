package org.openconcerto.modules.customerrelationship.lead;

import java.util.ArrayList;
import java.util.List;

import org.openconcerto.erp.modules.AbstractModule;
import org.openconcerto.erp.modules.ModuleElement;
import org.openconcerto.sql.element.GroupSQLComponent;
import org.openconcerto.sql.element.SQLComponent;

public class ServiceSQLElement extends ModuleElement {

    public ServiceSQLElement(AbstractModule module) {
        super(module, "SERVICE");
    }

    @Override
    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NAME");
        return l;
    }

    @Override
    public SQLComponent createComponent() {
        return new GroupSQLComponent(this, new ServiceGroup());
    }
}
