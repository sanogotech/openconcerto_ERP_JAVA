package org.openconcerto.modules.humanresources.travel.expense;

import org.openconcerto.ui.group.Group;
import org.openconcerto.ui.group.Item;
import org.openconcerto.ui.group.LayoutHints;

public class ExpenseGroup extends Group {

    public ExpenseGroup() {
        super(ExpenseSQLElement.ELEMENT_CODE + ".default");
        final Group g = new Group(ExpenseSQLElement.ELEMENT_CODE + ".identifier");
        g.addItem("DATE");
        g.addItem("ID_USER_COMMON");

        this.add(g);

        final Group gDescription = new Group(ExpenseSQLElement.ELEMENT_CODE + ".description");
        gDescription.add(new Item("DESCRIPTION", new LayoutHints(true, true, true, true, true, true)));
        this.add(gDescription);

        final Group gTravel = new Group(ExpenseSQLElement.ELEMENT_CODE + ".travel", LayoutHints.DEFAULT_SEPARATED_GROUP_HINTS);
        gTravel.addItem("TRAVEL_DISTANCE");
        gTravel.addItem("TRAVEL_RATE");
        gTravel.addItem("TRAVEL_AMOUNT");
        this.add(gTravel);

        final Group gAddress = new Group(ExpenseSQLElement.ELEMENT_CODE + ".misc", LayoutHints.DEFAULT_SEPARATED_GROUP_HINTS);
        gAddress.addItem("MISC_AMOUNT");
        this.add(gAddress);

        final Group gState = new Group(ExpenseSQLElement.ELEMENT_CODE + ".state");
        gState.addItem("ID_EXPENSE_STATE");

        this.add(gState);

    }

}
