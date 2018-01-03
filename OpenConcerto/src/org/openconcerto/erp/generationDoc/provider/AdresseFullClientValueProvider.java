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
 
 package org.openconcerto.erp.generationDoc.provider;

import org.openconcerto.erp.generationDoc.SpreadSheetCellValueContext;
import org.openconcerto.erp.generationDoc.SpreadSheetCellValueProviderManager;
import org.openconcerto.sql.model.SQLRowAccessor;

public class AdresseFullClientValueProvider extends AdresseClientProvider {

    private final int type;
    private final boolean withName;

    public AdresseFullClientValueProvider(int type, boolean withName) {
        this.type = type;
        this.withName = withName;
    }

    @Override
    public Object getValue(SpreadSheetCellValueContext context) {
        final SQLRowAccessor r = getAdresse(context.getRow(), this.type);
        String result = "";
        if (this.withName) {
            result = context.getRow().getForeign("ID_CLIENT").getString("NOM") + "\n";
        }
        result = getFormattedAddress(r, result);

        return result;
    }

    public static String getFormattedAddress(final SQLRowAccessor rAddress, String prefix) {
        String result = prefix;
        if (getStringTrimmed(rAddress, "LIBELLE").length() > 0) {
            result = getStringTrimmed(rAddress, "LIBELLE") + "\n";
        }
        if (getStringTrimmed(rAddress, "DEST").length() > 0) {
            result = getStringTrimmed(rAddress, "DEST") + "\n";
        }
        if (getStringTrimmed(rAddress, "RUE").length() > 0) {
            result += getStringTrimmed(rAddress, "RUE") + "\n";
        }
        result += "\n" + getStringTrimmed(rAddress, "CODE_POSTAL");
        result += " ";
        if (rAddress.getTable().contains("DISTRICT")) {
            result += getStringTrimmed(rAddress, "DISTRICT") + " ";
        }
        result += getStringTrimmed(rAddress, "VILLE");
        if (rAddress.getBoolean("HAS_CEDEX")) {
            result += " Cedex";
            String cedex = getStringTrimmed(rAddress, "CEDEX");
            if (cedex.length() > 0) {
                result += " " + cedex;
            }
        }
        if (rAddress.getTable().contains("PROVINCE")) {
            result += "\n";
            if (getStringTrimmed(rAddress, "PROVINCE").length() > 0) {
                result += getStringTrimmed(rAddress, ("PROVINCE")) + " ";
            }

            if (rAddress.getTable().contains("DEPARTEMENT")) {
                result += getStringTrimmed(rAddress, "DEPARTEMENT");
            }
        }

        if (getStringTrimmed(rAddress, "PAYS").length() > 0) {
            result += "\n" + getStringTrimmed(rAddress, "PAYS");
        }
        return result;
    }

    public static String getStringTrimmed(SQLRowAccessor r, String field) {
        String result = r.getString(field);
        if (result == null)
            return "";
        return result.trim();
    }

    public static void register() {
        SpreadSheetCellValueProviderManager.put("address.customer.full", new AdresseFullClientValueProvider(ADRESSE_PRINCIPALE, false));
        SpreadSheetCellValueProviderManager.put("address.customer.invoice.full", new AdresseFullClientValueProvider(ADRESSE_FACTURATION, false));
        SpreadSheetCellValueProviderManager.put("address.customer.shipment.full", new AdresseFullClientValueProvider(ADRESSE_LIVRAISON, false));
        SpreadSheetCellValueProviderManager.put("address.customer.full.withname", new AdresseFullClientValueProvider(ADRESSE_PRINCIPALE, true));
        SpreadSheetCellValueProviderManager.put("address.customer.invoice.full.withname", new AdresseFullClientValueProvider(ADRESSE_FACTURATION, true));
        SpreadSheetCellValueProviderManager.put("address.customer.shipment.full.withname", new AdresseFullClientValueProvider(ADRESSE_LIVRAISON, true));
    }
}
