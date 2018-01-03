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
 
 /*
 * Créé le 6 mars 2012
 */
package org.openconcerto.erp.preferences;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.preferences.SQLPreferences;
import org.openconcerto.ui.preferences.JavaPrefPreferencePanel;
import org.openconcerto.ui.preferences.PrefView;
import org.openconcerto.utils.PrefType;

public class GestionClientPreferencePanel extends JavaPrefPreferencePanel {
    public static String LOAD_CITIES = "LoadCities";
    public static String DISPLAY_CLIENT_DPT = "DisplayClientDpt";
    public static String DISPLAY_CLIENT_PCE = "DisplayClientPCE";

    public GestionClientPreferencePanel() {
        super("Gestion des clients", null);
        setPrefs(new SQLPreferences(((ComptaPropsConfiguration) Configuration.getInstance()).getRootSociete()));
    }

    @Override
    protected void addViews() {
        PrefView<Boolean> viewTransfert = new PrefView<Boolean>(PrefType.BOOLEAN_TYPE, "Gérer les différents services du client", DISPLAY_CLIENT_DPT);
        viewTransfert.setDefaultValue(Boolean.FALSE);
        this.addView(viewTransfert);

        PrefView<Boolean> viewCity = new PrefView<Boolean>(PrefType.BOOLEAN_TYPE, "Charger les villes connues", LOAD_CITIES);
        viewCity.setDefaultValue(Boolean.TRUE);
        this.addView(viewCity);

        PrefView<Boolean> viewPCE = new PrefView<Boolean>(PrefType.BOOLEAN_TYPE, "Voir le compte comptable dans les sélecteurs et les listes", DISPLAY_CLIENT_PCE);
        viewPCE.setDefaultValue(Boolean.FALSE);
        this.addView(viewPCE);
    }
}
