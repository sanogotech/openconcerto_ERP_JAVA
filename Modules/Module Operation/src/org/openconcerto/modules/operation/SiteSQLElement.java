package org.openconcerto.modules.operation;

import java.util.Arrays;
import java.util.List;

import org.openconcerto.erp.modules.AbstractModule;
import org.openconcerto.erp.modules.ModuleElement;
import org.openconcerto.sql.element.SQLComponent;

public class SiteSQLElement extends ModuleElement {

    public SiteSQLElement(AbstractModule module) {
        super(module, ModuleOperation.TABLE_SITE);
    }

    @Override
    protected List<String> getListFields() {
        return Arrays.asList("NAME", "ID_CLIENT", "COMMENT");
    }

    @Override
    protected SQLComponent createComponent() {
        return new SiteSQLComponent(this);
    }

}
