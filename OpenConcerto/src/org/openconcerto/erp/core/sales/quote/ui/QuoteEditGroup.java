/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2011 OpenConcerto, by ILM Informatique. All rights reserved.
 * 
 * The contents of this file are subject to the terms of the GNU General Public License Version 3
 * only ("GPL"). You may not use this file except in compliance with the License. You can obtain a
 * copy of the License at http://www.gnu.org/licenses/gpl-3.0.html See the License for the specific
 * language governing permissions and limitations under the License.
 * 
 * When distributing the software, include this License Header Notice in each file.
 */
 
 package org.openconcerto.erp.core.sales.quote.ui;

import org.openconcerto.ui.group.Group;
import org.openconcerto.ui.group.LayoutHints;

public class QuoteEditGroup extends Group {

    public QuoteEditGroup() {
        super("sales.quote");

        final Group g = new Group("sales.quote.identifier");
        g.addItem("sales.quote.number");
        g.addItem("sales.quote.date");
        g.addItem("sales.quote.label", LayoutHints.DEFAULT_LARGE_FIELD_HINTS);
        g.addItem("sales.quote.saleman");
        g.addItem("sales.quote.validity");
        g.addItem("sales.quote.state", LayoutHints.DEFAULT_LARGE_FIELD_HINTS);
        this.add(g);

        final Group gCustomer = new Group("sales.quote.customer.info");
        gCustomer.addItem("sales.quote.customer", LayoutHints.DEFAULT_LARGE_FIELD_HINTS);
        // Service
        // gCustomer.addItem("sales.quote.customer.service", LayoutHints.DEFAULT_LARGE_FIELD_HINTS);
        // gCustomer.addItem("sales.quote.customer.contact");
        // gCustomer.addItem("sales.quote.customer.tarif");
        // gCustomer.addItem("sales.quote.customer.discount");
        add(gCustomer);

        final Group bottom = new Group("sales.quote.footer");
        final Group gElements = new Group("sales.quote.items");
        gElements.addItem("sales.quote.items.list", new LayoutHints(true, true, false, true, true, true, true, true));
        bottom.add(gElements);

        final Group gInfos = new Group("sales.quote.info");
        final LayoutHints hint = new LayoutHints(false, true, true, false, true, true, true, true);
        gInfos.addItem("sales.quote.info.general", hint);
        bottom.add(gInfos);

        final Group gTotal = new Group("sales.quote.total");
        gTotal.addItem("sales.quote.total.amount");
        bottom.add(gTotal);

        final Group gOO = new Group("sales.quote.oo");
        LayoutHints hintInfos = new LayoutHints(false, false, true, true, true, false);
        gOO.addItem("panel.oo", hintInfos);
        bottom.add(gOO);

        add(bottom);

    }

}
