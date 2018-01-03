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

import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.sql.element.GroupSQLComponent;
import org.openconcerto.sql.element.SQLComponent;

import java.util.ArrayList;
import java.util.List;

public class CompteClientTransactionSQLELement extends ComptaSQLConfElement {

    public CompteClientTransactionSQLELement() {
        super("COMPTE_CLIENT_TRANSACTION");
    }

    @Override
    protected List<String> getListFields() {
        final List<String> fields = new ArrayList<String>();
        fields.add("ID_CLIENT");
        fields.add("DATE");
        fields.add("MONTANT");
        fields.add("ID_MODE_REGLEMENT");
        return fields;
    }

    @Override
    protected SQLComponent createComponent() {
        return new GroupSQLComponent(this, new CompteClientTransactionGroup());
    }

    @Override
    protected String createCode() {
        return super.createCodeFromPackage() + ".account.transaction";
    }
}
