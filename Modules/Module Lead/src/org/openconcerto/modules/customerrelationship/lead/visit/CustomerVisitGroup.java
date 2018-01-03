package org.openconcerto.modules.customerrelationship.lead.visit;

import org.openconcerto.ui.group.Group;
import org.openconcerto.ui.group.LayoutHints;

public class CustomerVisitGroup extends Group {

    public CustomerVisitGroup() {
        super("customerrelationship.customer.visit");
        final Group g = new Group("customerrelationship.customer.visit.identifier");
        g.addItem("DATE");
        g.addItem("ID_CLIENT", LayoutHints.DEFAULT_LARGE_FIELD_HINTS);
        this.add(g);

        final Group gContent = new Group("customerrelationship.customer.visit.content", LayoutHints.DEFAULT_SEPARATED_GROUP_HINTS);
        gContent.addItem("ibd.services", new LayoutHints(true, false, true, true, true, false, true, true));
        gContent.addItem("INFORMATION", new LayoutHints(true, true, true, true, true, true));
        this.add(gContent);

        final Group gNext = new Group("customerrelationship.customer.visit.next", LayoutHints.DEFAULT_SEPARATED_GROUP_HINTS);
        gNext.addItem("NEXTCONTACT_DATE", LayoutHints.DEFAULT_VERY_LARGE_TEXT_HINTS);
        gNext.addItem("NEXTCONTACT_INFO", LayoutHints.DEFAULT_VERY_LARGE_TEXT_HINTS);
        this.add(gNext);
    }

}
