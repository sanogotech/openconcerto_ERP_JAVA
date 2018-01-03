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
 
 package org.openconcerto.erp.core.customerrelationship.customer.element;

import org.openconcerto.erp.core.edm.AttachmentAction;
import org.openconcerto.sql.element.GlobalMapper;
import org.openconcerto.sql.element.GroupSQLComponent;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.view.list.IListeAction.IListeEvent;
import org.openconcerto.sql.view.list.RowAction.PredicateRowAction;

import java.util.List;

public class CustomerSQLElement extends ClientNormalSQLElement {

    public CustomerSQLElement() {
        super();
        final CustomerGroup group = new CustomerGroup();
        GlobalMapper.getInstance().map(CustomerGroup.ID, group);
        setDefaultGroup(group);
       
        PredicateRowAction actionAttachment = new PredicateRowAction(new AttachmentAction().getAction(), true);
        actionAttachment.setPredicate(IListeEvent.getSingleSelectionPredicate());
        getRowActions().add(actionAttachment);

    }

    @Override
    public SQLComponent createComponent() {
        final GroupSQLComponent c = new CustomerSQLComponent(this);
        c.startTabGroupAfter("customerrelationship.customer.identifier");
        return c;
    }

    @Override
    protected List<String> getListFields() {
        final List<String> fields = super.getListFields();
        if (getTable().contains("GROUPE")) {
            fields.add("GROUPE");
        }
        fields.add("SOLDE_COMPTE");
        fields.add("REMIND_DATE");
        fields.add("OBSOLETE");
        return fields;
    }
}
