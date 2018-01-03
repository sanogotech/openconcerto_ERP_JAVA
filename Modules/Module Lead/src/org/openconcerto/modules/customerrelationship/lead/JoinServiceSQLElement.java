package org.openconcerto.modules.customerrelationship.lead;

import java.util.ArrayList;
import java.util.List;

import org.openconcerto.erp.modules.AbstractModule;
import org.openconcerto.erp.modules.ModuleElement;
import org.openconcerto.sql.element.SQLComponent;

public abstract class JoinServiceSQLElement extends ModuleElement {

    private final String joinField;

    protected JoinServiceSQLElement(AbstractModule module, final String joinTable) {
        super(module, joinTable + "_SERVICE");
        this.joinField = "ID_" + joinTable;
    }

    @Override
    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add(this.joinField);
        l.add("ID_SERVICE");
        return l;
    }

    @Override
    protected final void ffInited() {
        super.ffInited();
        this.setAction(this.joinField, ReferenceAction.CASCADE);
        this.setAction("ID_SERVICE", ReferenceAction.CASCADE);
    }

    @Override
    protected final SQLComponent createComponent() {
        return null;
    }
}
