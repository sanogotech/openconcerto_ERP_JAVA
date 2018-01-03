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
import org.openconcerto.erp.core.sales.invoice.report.MailRelanceCreator;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.preferences.SQLPreferences;
import org.openconcerto.ui.preferences.JavaPrefPreferencePanel;
import org.openconcerto.ui.preferences.PrefView;
import org.openconcerto.utils.PrefType;

public class MailRelancePreferencePanel extends JavaPrefPreferencePanel {
    public static String MAIL_RELANCE_DATE_PATTERN = "MailRelanceDatePattern";
    public static String MAIL_RELANCE = "MailRelance";
    public static String MAIL_RELANCE_OBJET = "MailRelanceObjet";

    public MailRelancePreferencePanel() {
        super("Email de relance client", null);
        setPrefs(new SQLPreferences(((ComptaPropsConfiguration) Configuration.getInstance()).getRootSociete()));
    }

    @Override
    protected void addViews() {

        PrefView<String> viewExpandDate = new PrefView<String>(PrefType.STRING_TYPE, "Format des dates", MAIL_RELANCE_DATE_PATTERN);
        PrefView<String> viewExpandObjet = new PrefView<String>(PrefType.STRING_TYPE, "Objet du mail de relance", MAIL_RELANCE_OBJET);
        PrefView<String> viewExpandNom = new PrefView<String>(PrefType.STRING_TYPE, 2048, "Contenu du mail de relance", MAIL_RELANCE);
        viewExpandDate.setDefaultValue("dd/MM/yyyy");
        MailRelanceCreator mailCreator = new MailRelanceCreator();
        viewExpandNom.setDefaultValue(mailCreator.getDefaultValue());
        viewExpandObjet.setDefaultValue(mailCreator.getDefaultObject());
        this.addView(viewExpandDate);
        this.addView(viewExpandObjet);
        this.addView(viewExpandNom);

    }

}
