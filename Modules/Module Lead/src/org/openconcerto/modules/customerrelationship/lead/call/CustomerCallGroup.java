package org.openconcerto.modules.customerrelationship.lead.call;

import org.openconcerto.ui.group.Group;
import org.openconcerto.ui.group.LayoutHints;

public class CustomerCallGroup extends Group {

    public CustomerCallGroup() {
        super("customerrelationship.customer.call");
        final Group g = new Group("customerrelationship.customer.call.identifier");
        g.addItem("DATE");
        g.addItem("ID_CLIENT", LayoutHints.DEFAULT_LARGE_FIELD_HINTS);
        this.add(g);

        final Group gAddress = new Group("customerrelationship.customer.call.content", LayoutHints.DEFAULT_SEPARATED_GROUP_HINTS);
        gAddress.addItem("ibd.services", new LayoutHints(true, false, true, true, true, false, true, true));
        gAddress.addItem("INFORMATION", new LayoutHints(true, true, true, true, true, true));
        this.add(gAddress);

        final Group gNext = new Group("customerrelationship.customer.call.next", LayoutHints.DEFAULT_SEPARATED_GROUP_HINTS);
        gNext.addItem("NEXTCONTACT_DATE", LayoutHints.DEFAULT_VERY_LARGE_TEXT_HINTS);
        gNext.addItem("NEXTCONTACT_INFO", LayoutHints.DEFAULT_VERY_LARGE_TEXT_HINTS);
        this.add(gNext);
    }

}
