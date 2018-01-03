package org.openconcerto.modules.humanresources.travel.expense;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.sql.element.GlobalMapper;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.ui.group.Group;
import org.openconcerto.utils.ListMap;

public class ExpenseSQLElement extends ComptaSQLConfElement {
    public static final String ELEMENT_CODE = "humanresources.travel.expense";

    public ExpenseSQLElement() {
        super(Module.TABLE_EXPENSE, "une note de frais", "notes de frais");
    }

    @Override
    protected String createCode() {
        return ELEMENT_CODE;
    }

    @Override
    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("DATE");
        l.add("ID_USER_COMMON");
        l.add("DESCRIPTION");
        l.add("TRAVEL_AMOUNT");
        l.add("MISC_AMOUNT");
        l.add("ID_EXPENSE_STATE");
        return l;
    }

    @Override
    public Set<String> getReadOnlyFields() {
        return Collections.singleton("TRAVEL_AMOUNT");
    }

    @Override
    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("DATE");
        l.add("DESCRIPTION");
        return l;
    }

    @Override
    public ListMap<String, String> getShowAs() {
        return ListMap.singleton(null, getComboFields());
    }

    @Override
    public SQLComponent createComponent() {
        final String groupId = this.getCode() + ".default";
        final Group group = GlobalMapper.getInstance().getGroup(groupId);
        if (group == null) {
            throw new IllegalStateException("No group found for id " + groupId);
        }
        return new ExpenseSQLComponent(this, group);
    }

}
