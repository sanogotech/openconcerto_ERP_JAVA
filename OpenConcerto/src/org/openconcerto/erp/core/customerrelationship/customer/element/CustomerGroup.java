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

import org.openconcerto.ui.group.Group;
import org.openconcerto.ui.group.LayoutHints;

public class CustomerGroup extends Group {
    public final static String ID = "customerrelationship.customer.default";

    public CustomerGroup() {
        super(ID);
        final Group g = new Group("customerrelationship.customer.identifier");
        g.addItem("CODE");
        g.addItem("DATE");
        g.addItem("FORME_JURIDIQUE");
        g.addItem("GROUPE");
        g.addItem("NOM", LayoutHints.DEFAULT_LARGE_FIELD_HINTS);
        g.addItem("CATEGORIES");
        g.addItem("RESPONSABLE");
        g.addItem("ID_PAYS");

        g.addItem("TEL");
        g.addItem("TEL_P");
        g.addItem("MAIL", LayoutHints.DEFAULT_LARGE_FIELD_HINTS);
        g.addItem("FAX");
        g.addItem("SITE_INTERNET", LayoutHints.DEFAULT_LARGE_FIELD_HINTS);

        g.addItem("SIRET");
        g.addItem("NUMERO_TVA");

        g.addItem("BLOQUE");
        g.addItem("BLOQUE_LIVRAISON");

        this.add(g);

        // this.add(new Group("customerrelationship.customer.additionalElementFields"));

        final Group gAddress = new Group("customerrelationship.customer.address", LayoutHints.DEFAULT_SEPARATED_GROUP_HINTS);
        // gAddress.addItem("ID_ADRESSE", new LayoutHints(true, true, true, true, true, false, true,
        // true));
        // gAddress.addItem("ID_ADRESSE_F", new LayoutHints(true, true, true, true, true, false,
        // true, true));
        // gAddress.addItem("ID_ADRESSE_L");
        gAddress.addItem("customerrelationship.customer.addresses", new LayoutHints(true, true, true, true, true, true, true, true));

        this.add(gAddress);

        final Group gContact = new Group("customerrelationship.customer.contact", LayoutHints.DEFAULT_SEPARATED_GROUP_HINTS);
        gContact.addItem("customerrelationship.customer.contacts", new LayoutHints(true, true, true, true, true, true, true, true));
        this.add(gContact);

        final Group gPayment = new Group("customerrelationship.customer.payment", LayoutHints.DEFAULT_SEPARATED_GROUP_HINTS);
        gPayment.addItem("RIB", LayoutHints.DEFAULT_LARGE_FIELD_HINTS);
        gPayment.addItem("CENTRE_GESTION", LayoutHints.DEFAULT_LARGE_FIELD_HINTS);
        gPayment.addItem("ID_MODE_REGLEMENT", new LayoutHints(true, true, true, true, true, false, true, true));
        gPayment.addItem("ID_COMPTE_PCE");
        gPayment.addItem("ENCOURS_MAX");
        gPayment.addItem("ID_COMPTE_PCE_PRODUIT");
        gPayment.addItem("ID_COMPTE_PCE_SERVICE");
        gPayment.addItem("ID_DEVISE");
        gPayment.addItem("INFOS", new LayoutHints(true, true, true, true, true, true, true, true));
        gPayment.addItem("NOTE_FINANCIERE", LayoutHints.DEFAULT_VERY_LARGE_FIELD_HINTS);
        gPayment.addItem("METHODE_RELANCE");
        this.add(gPayment);

        final Group gState = new Group("customerrelationship.customer.sales", LayoutHints.DEFAULT_SEPARATED_GROUP_HINTS);
        gState.addItem("ID_COMMERCIAL");
        gState.addItem("ID_LANGUE");
        gState.addItem("ID_TARIF");

        this.add(gState);
        final Group gInfo = new Group("customerrelationship.customer.info", LayoutHints.DEFAULT_SEPARATED_GROUP_HINTS);
        gInfo.addItem("REMIND_DATE");
        gInfo.addItem("INFOS", new LayoutHints(true, true, true, true, true, true, true, true));
        gInfo.addItem("COMMENTAIRES", new LayoutHints(true, true, true, true, true, true, true, true));
        gInfo.addItem("OBSOLETE");
        this.add(gInfo);

    }
}
