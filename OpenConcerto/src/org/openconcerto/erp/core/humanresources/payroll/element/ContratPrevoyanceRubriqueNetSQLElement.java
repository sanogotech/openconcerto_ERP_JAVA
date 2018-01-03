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
 
 package org.openconcerto.erp.core.humanresources.payroll.element;

import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.sql.element.BaseSQLComponent;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.utils.ListMap;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;

public class ContratPrevoyanceRubriqueNetSQLElement extends ComptaSQLConfElement {

    public ContratPrevoyanceRubriqueNetSQLElement() {
        super("CONTRAT_PREVOYANCE_RUBRIQUE_NET", "un rattachement rubrique net-contrat prévoyance", "rattachements rubrique net-contrat prévoyance");
    }

    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("ID_RUBRIQUE_NET");
        l.add("ID_CONTRAT_PREVOYANCE");
        return l;
    }

    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("ID_RUBRIQUE_NET");
        return l;
    }

    @Override
    public ListMap<String, String> getShowAs() {
        return ListMap.singleton(null, "ID_RUBRIQUE_NET");
    }

    @Override
    protected String getParentFFName() {
        return "ID_CONTRAT_PREVOYANCE";
    }

    @Override
    protected String createCode() {
        return "humanresources.payroll.prevoyance.rubriquenet";
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openconcerto.devis.SQLElement#getComponent()
     */
    public SQLComponent createComponent() {
        return new BaseSQLComponent(this) {

            public void addViews() {

                this.setLayout(new GridBagLayout());

            }
        };
    }
}
