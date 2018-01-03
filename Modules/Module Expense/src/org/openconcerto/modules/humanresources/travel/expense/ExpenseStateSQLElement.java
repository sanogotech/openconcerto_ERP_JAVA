package org.openconcerto.modules.humanresources.travel.expense;

import java.util.ArrayList;
import java.util.List;

import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.sql.element.GlobalMapper;
import org.openconcerto.sql.element.GroupSQLComponent;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.ui.group.Group;
import org.openconcerto.utils.ListMap;

public class ExpenseStateSQLElement extends ComptaSQLConfElement {
    public static final String ELEMENT_CODE = "humanresources.travel.expense.state";

    public ExpenseStateSQLElement() {
        super(Module.TABLE_EXPENSE_STATE, "un status de note de frais", "status de note de frais");
    }

    @Override
    protected String createCode() {
        return ELEMENT_CODE;
    }

    @Override
    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NAME");
        return l;
    }

    @Override
    protected List<String> getComboFields() {
        return getListFields();
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
        return new GroupSQLComponent(this, group);
    }

}
