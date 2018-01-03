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
 
 package org.openconcerto.erp.core.customerrelationship.customer.ui;

import org.openconcerto.erp.utils.TM;

public enum AdresseType {

    Invoice("invoice", "address.type.invoice"), Delivery("delivery", "address.type.delivery"), External("external", "address.type.external"), Other("other", "address.type.other");

    private final String id, idLabel;

    private AdresseType(String id, String idLabel) {
        this.id = id;
        this.idLabel = idLabel;
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return this.idLabel;
    }

    @Override
    public String toString() {
        return getTranslation();
    }

    public String getTranslation() {
        return TM.tr(getLabel());
    }

}
