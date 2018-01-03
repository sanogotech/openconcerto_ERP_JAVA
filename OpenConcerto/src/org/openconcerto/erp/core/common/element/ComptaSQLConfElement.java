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
 
 package org.openconcerto.erp.core.common.element;

import org.openconcerto.erp.config.Gestion;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.task.config.ComptaBasePropsConfiguration;

/**
 * SQLElement de la base société
 * 
 * @author Administrateur
 * 
 */
public abstract class ComptaSQLConfElement extends SocieteSQLConfElement {

    private static DBRoot baseSociete;

    private static DBRoot getBaseSociete() {
        if (baseSociete == null)
            baseSociete = ((ComptaBasePropsConfiguration) Configuration.getInstance()).getRootSociete();
        return baseSociete;
    }

    {
        this.setL18nLocation(Gestion.class);
    }

    public ComptaSQLConfElement(String tableName, String singular, String plural) {
        super(getBaseSociete().findTable(tableName, true), singular, plural);
    }

    public ComptaSQLConfElement(String tableName) {
        super(getBaseSociete().findTable(tableName, true), null);
    }

    public ComptaSQLConfElement(String tableName, String code) {
        super(getBaseSociete().findTable(tableName, true), code);
    }

}
