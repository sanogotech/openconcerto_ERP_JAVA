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
 
 package org.openconcerto.erp.core.sales.invoice.report;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.finance.payment.element.ModeDeReglementSQLElement;
import org.openconcerto.erp.preferences.MailRelancePreferencePanel;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.preferences.SQLPreferences;
import org.openconcerto.sql.users.UserManager;
import org.openconcerto.utils.GestionDevise;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MailRelanceCreator {

    SQLRow rowEcheance;

    public MailRelanceCreator() {

    }

    public String getDefaultObject() {
        return "Relance {FactureNumero}";
    }

    public String getDefaultValue() {
        String value = "Bonjour,\n\nSauf erreur de notre part, votre compte laisse apparaître dans nos livres un montant de {FactureRestant}€ non réglé à ce jour."
                + "\nCe montant correspond à la facture {FactureNumero} datée du {FactureDate} qui a pour échéance le {FactureDateEcheance}."
                + "\nNous présumons qu'il s'agit d'un simple oubli de votre part.\n\n"
                + "Toutefois, si le paiement avait été effectué, nous vous serions très obligés de nous en communiquer la date et le mode de règlement.\n\n"
                + "Dans l'attente d’un prompt règlement,\n\n" + "Nous vous prions d\'agréer, Madame, Monsieur, l\'expression de nos sentiments distingués.";

        return value;
    }

    public Map<String, String> getMapValues(SQLRow rowEch, String datePattern) {
        final Map<String, String> map = new HashMap<String, String>();

        final SQLRow rowSoc = ((ComptaPropsConfiguration) Configuration.getInstance()).getRowSociete();
        final SQLRow rowSocAdresse = rowSoc.getForeignRow("ID_ADRESSE_COMMON");
        SQLRow rowUser = rowSoc.getTable().getDBRoot().findTable("USER_COMMON").getRow(UserManager.getUser().getId());
        map.put("UserName", rowUser.getString("NOM"));
        map.put("UserFirstName", rowUser.getString("PRENOM"));
        if (rowUser.getTable().contains("MAIL")) {
            map.put("UserMail", rowUser.getString("MAIL"));
        }
        if (rowUser.getTable().contains("TEL")) {
            map.put("UserTel", rowUser.getString("TEL"));
        }
        // Infos societe
        map.put("SocieteType", rowSoc.getString("TYPE"));
        map.put("SocieteNom", rowSoc.getString("NOM"));
        map.put("SocieteAdresse", rowSocAdresse.getString("RUE"));
        map.put("SocieteCodePostal", rowSocAdresse.getString("CODE_POSTAL"));

        String ville = rowSocAdresse.getString("VILLE");
        final Object cedex = rowSocAdresse.getObject("CEDEX");
        final boolean hasCedex = rowSocAdresse.getBoolean("HAS_CEDEX");

        if (hasCedex) {
            ville += " CEDEX";
            if (cedex != null && cedex.toString().trim().length() > 0) {
                ville += " " + cedex.toString().trim();
            }
        }

        map.put("SocieteVille", ville);

        SQLRow rowClient;
        final SQLRow clientRowNX = rowEch.getForeignRow("ID_CLIENT");
            rowClient = clientRowNX;
        SQLRow rowAdresse = rowClient.getForeignRow("ID_ADRESSE");
        if (!clientRowNX.isForeignEmpty("ID_ADRESSE_F")) {
            rowAdresse = clientRowNX.getForeign("ID_ADRESSE_F");
        }
        // Client compte
        SQLRow rowCompteClient = clientRowNX.getForeignRow("ID_COMPTE_PCE");
        String numero = rowCompteClient.getString("NUMERO");
        map.put("ClientNumeroCompte", numero);

        // Infos Client
        map.put("ClientType", rowClient.getString("FORME_JURIDIQUE"));
        map.put("ClientNom", rowClient.getString("NOM"));
        map.put("ClientAdresse", rowAdresse.getString("RUE"));
        map.put("ClientCodePostal", rowAdresse.getString("CODE_POSTAL"));
        String villeCli = rowAdresse.getString("VILLE");
        final Object cedexCli = rowAdresse.getObject("CEDEX");
        final boolean hasCedexCli = rowAdresse.getBoolean("HAS_CEDEX");

        if (hasCedexCli) {
            villeCli += " CEDEX";
            if (cedexCli != null && cedexCli.toString().trim().length() > 0) {
                villeCli += " " + cedexCli.toString().trim();
            }
        }

        map.put("ClientVille", villeCli);

        // Date relance
        Date d = new Date();
        DateFormat dateFormat = new SimpleDateFormat(datePattern);
        map.put("RelanceDate", dateFormat.format(d));

        SQLRow rowFacture = rowEch.getForeignRow("ID_SAISIE_VENTE_FACTURE");


        // Infos facture
        Long lTotal = (Long) rowFacture.getObject("T_TTC");
        Long lRestant = (Long) rowEch.getObject("MONTANT");
        Long lVerse = new Long(lTotal.longValue() - lRestant.longValue());
        map.put("FactureNumero", rowFacture.getString("NUMERO"));
        map.put("FactureReference", rowFacture.getString("NOM"));
        map.put("FactureTotal", GestionDevise.currencyToString(lTotal.longValue(), true));
        map.put("FactureRestant", GestionDevise.currencyToString(lRestant.longValue(), true));
        map.put("FactureVerse", GestionDevise.currencyToString(lVerse.longValue(), true));
        map.put("FactureDate", dateFormat.format((Date) rowFacture.getObject("DATE")));

        Date dFacture = (Date) rowFacture.getObject("DATE");
        SQLRow modeRegRow = rowFacture.getForeignRow("ID_MODE_REGLEMENT");
        Date dateEch = ModeDeReglementSQLElement.calculDate(modeRegRow.getInt("AJOURS"), modeRegRow.getInt("LENJOUR"), dFacture);
        map.put("FactureDateEcheance", dateFormat.format(dateEch));

        return map;
    }

    public String getObject(SQLRow rowEch) {
        SQLPreferences prefs = new SQLPreferences(rowEch.getTable().getDBRoot());
        String object = prefs.get(MailRelancePreferencePanel.MAIL_RELANCE_OBJET, getDefaultObject());
        String date = prefs.get(MailRelancePreferencePanel.MAIL_RELANCE_DATE_PATTERN, "dd/MM/yyyy");
        return fill(rowEch, date, object);
    }

    public String getValue(SQLRow rowEch) {
        SQLPreferences prefs = new SQLPreferences(rowEch.getTable().getDBRoot());
        String value = prefs.get(MailRelancePreferencePanel.MAIL_RELANCE, getDefaultValue());
        String date = prefs.get(MailRelancePreferencePanel.MAIL_RELANCE_DATE_PATTERN, "dd/MM/yyyy");
        return fill(rowEch, date, value);
    }

    private String fill(SQLRow rowEch, String datePattern, String string) {

        Map<String, String> map = getMapValues(rowEch, datePattern);
        String result = string;
        for (String key : map.keySet()) {
            result = result.replace("{" + key + "}", map.get(key));
        }
        return result;
    }

}
