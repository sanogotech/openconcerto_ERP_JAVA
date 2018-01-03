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
 
 package org.openconcerto.erp.core.sales.order.ui;

import org.openconcerto.utils.i18n.TranslationManager;

import java.util.HashMap;
import java.util.Map;

public enum EtatCommandeClient {

    A_PREPARER(1, "sales.order.state.waiting"), RUPTURE(2, "sales.order.state.noStock"), EN_PREPARATION(3, "sales.order.state.running"), A_LIVRER(4, "sales.order.state.toDeliver"), LIVREE(5,
            "sales.order.state.delivered"), BLOQUE(6, "sales.order.state.block"), ANNULEE(7, "sales.order.state.cancelled");

    private final int id;
    private final String translationID;

    private EtatCommandeClient(int id, String translationID) {
        this.id = id;
        this.translationID = translationID;
    }

    public int getId() {
        return id;
    }

    public String getTranslationID() {
        return translationID;
    }

    public String getTranslation() {
        final String translationForItem = TranslationManager.getInstance().getTranslationForItem(translationID);

        return (translationForItem == null || translationForItem.trim().length() == 0) ? translationID : translationForItem;
    }

    private static final Map<Integer, EtatCommandeClient> idToEnum = new HashMap<Integer, EtatCommandeClient>();
    static {
        for (EtatCommandeClient e : values())
            idToEnum.put(e.getId(), e);
    }

    public static EtatCommandeClient fromID(int id) {
        return idToEnum.get(id);
    }

    @Override
    public String toString() {
        return getTranslation();
    }

}
