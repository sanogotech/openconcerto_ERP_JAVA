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
 
 package org.openconcerto.erp.core.supplychain.supplier.element;

import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.erp.core.edm.AttachmentAction;
import org.openconcerto.erp.core.supplychain.supplier.component.FournisseurSQLComponent;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.request.ComboSQLRequest;
import org.openconcerto.sql.request.ComboSQLRequest.KeepMode;
import org.openconcerto.sql.view.list.IListeAction.IListeEvent;
import org.openconcerto.sql.view.list.RowAction.PredicateRowAction;
import org.openconcerto.sql.request.ListSQLRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FournisseurSQLElement extends ComptaSQLConfElement {

    public FournisseurSQLElement() {
        super("FOURNISSEUR", "un fournisseur", "fournisseurs");

        PredicateRowAction actionAttachment = new PredicateRowAction(new AttachmentAction().getAction(), true);
        actionAttachment.setPredicate(IListeEvent.getSingleSelectionPredicate());
        getRowActions().add(actionAttachment);
    }

    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("CODE");
        l.add("NOM");
        l.add("TYPE");
        l.add("TEL");
        l.add("FAX");
        l.add("ID_MODE_REGLEMENT");
        l.add("ID_COMPTE_PCE");
        l.add("ID_ADRESSE");
        return l;
    }

    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NOM");
        l.add("CODE");
        return l;
    }

    @Override
    protected void _initComboRequest(ComboSQLRequest req) {
        super._initComboRequest(req);
        req.addToGraphToFetch("UE");
        req.addForeignToGraphToFetch("ID_COMPTE_PCE_CHARGE", Arrays.asList("ID", "NUMERO"));
        req.keepRows(KeepMode.GRAPH);
    }

    @Override
    protected void _initListRequest(ListSQLRequest req) {
        super._initListRequest(req);
        req.addForeignToGraphToFetch("ID_MODE_REGLEMENT", Arrays.asList("AJOURS", "LENJOUR", "DATE_FACTURE", "COMPTANT"));

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openconcerto.devis.SQLElement#getComponent()
     */
    public SQLComponent createComponent() {
        return new FournisseurSQLComponent(this);
    }
}
