package org.openconcerto.modules.humanresources.travel.expense;

import org.openconcerto.ui.group.Group;
import org.openconcerto.ui.group.Item;
import org.openconcerto.ui.group.LayoutHints;

public class ExpenseStateGroup extends Group {

    public ExpenseStateGroup() {
        super(ExpenseStateSQLElement.ELEMENT_CODE + ".default");
        final Group g = new Group(ExpenseSQLElement.ELEMENT_CODE + ".identifier");
        g.add(new Item("NAME", LayoutHints.DEFAULT_LARGE_FIELD_HINTS));
        this.add(g);

    }

}
