package org.openconcerto.modules.operation;

import org.openconcerto.ui.group.Group;
import org.openconcerto.ui.group.LayoutHints;

public class MultiOperationGroup extends Group {

    public MultiOperationGroup() {
        super("operation");
        this.addItem("operation.site", new LayoutHints(true, false, true, false, true, false));
        this.addItem("operation.type", new LayoutHints(true, false, true, false, true, false));
        this.addItem("operation.user");

        this.addItem("operation.status");
        this.addItem("operation.dates", new LayoutHints(true, false, false, false, true, false, true));
        this.addItem("operation.description", new LayoutHints(true, false, true, false, true, true, true));
    }

}
