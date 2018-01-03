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
 
 package org.openconcerto.task.element;

import java.util.List;

import org.openconcerto.sql.element.ConfSQLElement;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.model.DBRoot;

public class FWKListPrefs extends ConfSQLElement {
    public FWKListPrefs(final DBRoot rootSociety) {
        super(rootSociety.findTable("FWK_LIST_PREFS", true));
    }

    @Override
    protected List<String> getListFields() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public SQLComponent createComponent() {
        // TODO Auto-generated method stub
        return null;
    }
}
