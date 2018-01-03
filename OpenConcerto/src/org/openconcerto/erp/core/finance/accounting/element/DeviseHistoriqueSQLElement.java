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
 
 package org.openconcerto.erp.core.finance.accounting.element;

import org.openconcerto.erp.core.finance.accounting.component.DeviseHistoriqueSQLComponent;
import org.openconcerto.sql.element.ConfSQLElement;
import org.openconcerto.sql.element.GlobalMapper;
import org.openconcerto.sql.element.SQLComponent;

import java.util.ArrayList;
import java.util.List;

public class DeviseHistoriqueSQLElement extends ConfSQLElement {

    public DeviseHistoriqueSQLElement() {
        super("DEVISE_HISTORIQUE");
        final CurrencyRateGroup group = new CurrencyRateGroup();
        GlobalMapper.getInstance().map(DeviseHistoriqueSQLComponent.ID, group);
        setDefaultGroup(group);
    }

    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("DATE");
        l.add("SRC");
        l.add("DST");
        l.add("TAUX");
        l.add("TAUX_COMMERCIAL");
        return l;
    }

    protected List<String> getComboFields() {
        return getListFields();
    }

    public SQLComponent createComponent() {
        return new DeviseHistoriqueSQLComponent(this);
    }
}
