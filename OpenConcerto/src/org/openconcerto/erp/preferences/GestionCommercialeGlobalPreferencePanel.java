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
import org.openconcerto.erp.core.common.ui.AbstractArticleItemTable;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.preferences.SQLPreferences;
import org.openconcerto.ui.preferences.JavaPrefPreferencePanel;
import org.openconcerto.ui.preferences.PrefView;
import org.openconcerto.utils.PrefType;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;

public class GestionCommercialeGlobalPreferencePanel extends JavaPrefPreferencePanel {
    public static String TRANSFERT_REF = "TransfertRef";
    public static String TRANSFERT_MULTI_REF = "TransfertMultiRef";
    public static String TRANSFERT_NO_REF = "TransfertNoRef";
    public static String ORDER_PACKAGING_MANAGEMENT = "OrderPackagingManagement";
    public static String ADDRESS_SPEC = "AddressSpec";
    public static String GESTION_TIMBRE_FISCAL = "GestionTimbreFiscal";
    public static String TAUX_TIMBRE_FISCAL = "TauxTimbreFiscal";
    public static String BARCODE_INSERTION = "BarcodeInsertion";

    public GestionCommercialeGlobalPreferencePanel() {
        super("Gestion des pièces commerciales", null);
        setPrefs(new SQLPreferences(((ComptaPropsConfiguration) Configuration.getInstance()).getRootSociete()));
    }

    @Override
    protected void addViews() {
        PrefView<Boolean> viewTransfert = new PrefView<Boolean>(PrefType.BOOLEAN_TYPE, "Transférer les numéros des pièces commeriales en tant que référence", TRANSFERT_REF);
        viewTransfert.setDefaultValue(Boolean.TRUE);
        this.addView(viewTransfert);

        PrefView<Boolean> viewMultiTransfert = new PrefView<Boolean>(PrefType.BOOLEAN_TYPE, "Transférer les numéros des pièces commerciales dans le corps", TRANSFERT_MULTI_REF);
        viewMultiTransfert.setDefaultValue(Boolean.FALSE);
        this.addView(viewMultiTransfert);

        PrefView<Boolean> viewNo = new PrefView<Boolean>(PrefType.BOOLEAN_TYPE, "Ne pas transférer les numéros des pièces commerciales", TRANSFERT_NO_REF);
        viewNo.setDefaultValue(Boolean.FALSE);
        this.addView(viewNo);

        PrefView<Boolean> orderPackaging = new PrefView<Boolean>(PrefType.BOOLEAN_TYPE, "Gérer la préparation des commandes clients", ORDER_PACKAGING_MANAGEMENT);
        orderPackaging.setDefaultValue(Boolean.TRUE);
        this.addView(orderPackaging);

        PrefView<Boolean> addressSpec = new PrefView<Boolean>(PrefType.BOOLEAN_TYPE, "Afficher les sélecteurs d'adresse spécifique", ADDRESS_SPEC);
        addressSpec.setDefaultValue(Boolean.TRUE);
        this.addView(addressSpec);

        PrefView<Boolean> gestTimbreFisc = new PrefView<Boolean>(PrefType.BOOLEAN_TYPE, "Activer la gestion du timbre fiscal", GESTION_TIMBRE_FISCAL);
        gestTimbreFisc.setDefaultValue(Boolean.FALSE);
        this.addView(gestTimbreFisc);

        PrefView<Double> tauxTimbreFisc = new PrefView<Double>(PrefType.DOUBLE_TYPE, "Taux du timbre fiscal", TAUX_TIMBRE_FISCAL);
        tauxTimbreFisc.setDefaultValue(Double.valueOf(1));
        this.addView(tauxTimbreFisc);

        PrefView<Boolean> ecoColumns = new PrefView<Boolean>(PrefType.BOOLEAN_TYPE, "Afficher les colonnes éco-contributions", AbstractArticleItemTable.SHOW_ECO_CONTRIBUTION_COLUMNS);
        ecoColumns.setDefaultValue(Boolean.FALSE);
        this.addView(ecoColumns);

        PrefView<Boolean> ecoTotal = new PrefView<Boolean>(PrefType.BOOLEAN_TYPE, "Afficher le total de l'éco-contribution", AbstractArticleItemTable.SHOW_TOTAL_ECO_CONTRIBUTION);
        ecoTotal.setDefaultValue(Boolean.FALSE);
        this.addView(ecoTotal);

        PrefView<Boolean> barcodeInsert = new PrefView<Boolean>(PrefType.BOOLEAN_TYPE, "Insérer des lignes dans le corps avec la douchette", BARCODE_INSERTION);
        barcodeInsert.setDefaultValue(Boolean.FALSE);
        this.addView(barcodeInsert);

        ButtonGroup group = new ButtonGroup();
        group.add((JCheckBox) viewMultiTransfert.getVW().getComp());
        group.add((JCheckBox) viewTransfert.getVW().getComp());
        group.add((JCheckBox) viewNo.getVW().getComp());

    }
}
