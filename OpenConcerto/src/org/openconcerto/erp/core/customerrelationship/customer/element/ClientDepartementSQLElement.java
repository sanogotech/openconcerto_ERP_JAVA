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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ClientDepartementSQLElement extends ComptaSQLConfElement {

    public ClientDepartementSQLElement() {
        super("CLIENT_DEPARTEMENT", "un service client", "services client");
    }

    @Override
    protected List<String> getComboFields() {
        List<String> list = new ArrayList<String>();
        list.add("NOM");
        return list;
    }

    @Override
    protected List<String> getListFields() {
        List<String> list = new ArrayList<String>();
        list.add("NOM");
        list.add("ID_CLIENT");
        return list;
    }

    public SQLComponent createComponent() {
        return new UISQLComponent(this, 1) {
            public void addViews() {
                this.addView("NOM");
                this.addView("ID_CLIENT");
                this.addView("ID_ADRESSE");
                this.addView("INFOS");
            }
        };
    }

    @Override
    protected String createCode() {
        return super.createCodeFromPackage() + ".department";
    }
}
