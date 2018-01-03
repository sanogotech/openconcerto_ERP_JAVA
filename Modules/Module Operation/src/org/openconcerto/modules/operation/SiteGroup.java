package org.openconcerto.modules.operation;

import org.openconcerto.ui.group.Group;
import org.openconcerto.ui.group.LayoutHints;

public class SiteGroup extends Group {

    public SiteGroup() {
        super("operation.site");
        final Group g = new Group("operation.site.identifier");
        g.addItem("operation.site.label");
        g.addItem("operation.site.customer", new LayoutHints(true, false, true, true, true, false));
        // g.addItem("operation.site.optionaladdress", new LayoutHints(true, false, true, false,
        // true, false));
        this.add(g);
        addItem("operation.site.comment", new LayoutHints(true, true, true, true, true, true, true));
        addItem("operation.site.info", new LayoutHints(true, true, true, true, true, true, true));
    }
}