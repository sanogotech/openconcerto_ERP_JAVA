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
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.element.UISQLComponent;
import org.openconcerto.utils.ListMap;

import java.util.ArrayList;
import java.util.List;

public class CustomerCategorySQLElement extends ComptaSQLConfElement {
    public CustomerCategorySQLElement() {
        super("CATEGORIE_CLIENT");
    }

    @Override
    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();

        l.add("NOM");

        return l;
    }

    @Override
    public ListMap<String, String> getShowAs() {
        return ListMap.singleton(null, getComboFields());
    }

    @Override
    public SQLComponent createComponent() {
        // final GroupSQLComponent c = new CustomerSQLComponent(this, new CustomerGroup());
        UISQLComponent c = new UISQLComponent(this) {

            @Override
            protected void addViews() {
                addView("NOM");
            }
        };
        return c;
    }

    @Override
    protected String createCode() {
        return super.createCodeFromPackage() + ".category";
    }
}
