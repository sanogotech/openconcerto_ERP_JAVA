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
 
 package org.openconcerto.erp.generationEcritures;

import org.openconcerto.erp.core.finance.accounting.element.ComptePCESQLElement;
import org.openconcerto.erp.core.finance.accounting.element.JournalSQLElement;
import org.openconcerto.erp.generationEcritures.provider.AccountingRecordsProvider;
import org.openconcerto.erp.generationEcritures.provider.AccountingRecordsProviderManager;
import org.openconcerto.erp.model.PrixHT;
import org.openconcerto.erp.model.PrixTTC;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.utils.ExceptionHandler;

import java.util.Date;

public class GenerationMvtSaisieAchat extends GenerationEcritures implements Runnable {

    public static final String ID = "accounting.records.supply.order";

    private final SQLRow saisieRow;
    private static final String source = "SAISIE_ACHAT";
    private static final Integer journal = new Integer(JournalSQLElement.ACHATS);
    private static final SQLTable tablePrefCompte = base.getTable("PREFS_COMPTE");
    private static final SQLTable tableFournisseur = base.getTable("FOURNISSEUR");
    private static final SQLRow rowPrefsCompte = tablePrefCompte.getRow(2);

    public GenerationMvtSaisieAchat(SQLRow row) {
        setRowAnalytiqueSource(row);
        this.saisieRow = row;
        this.idMvt = row.getInt("ID_MOUVEMENT");
    }

    public void genereMouvement() throws Exception {
        SQLRow rowFournisseur = tableFournisseur.getRow(this.saisieRow.getInt("ID_FOURNISSEUR"));

        // iniatilisation des valeurs de la map
        this.date = (Date) saisieRow.getObject("DATE");
        this.nom = "Achat : " + rowFournisseur.getString("NOM") + " Facture : " + this.saisieRow.getObject("NUMERO_FACTURE").toString() + " " + saisieRow.getObject("NOM").toString();
        this.mEcritures.put("DATE", this.date);
        AccountingRecordsProvider provider = AccountingRecordsProviderManager.get(ID);
        provider.putLabel(saisieRow, this.mEcritures);

        this.mEcritures.put("ID_JOURNAL", GenerationMvtSaisieAchat.journal);
        this.mEcritures.put("ID_MOUVEMENT", new Integer(1));

        // Calcul des montants
        PrixTTC prixTTC = new PrixTTC(this.saisieRow.getLong("MONTANT_TTC"));
        PrixHT prixTVA = new PrixHT(this.saisieRow.getLong("MONTANT_TVA"));
        PrixHT prixHT = new PrixHT(this.saisieRow.getLong("MONTANT_HT"));

        // on calcule le nouveau numero de mouvement
        if (this.idMvt == 1) {
            SQLRowValues rowValsPiece = new SQLRowValues(pieceTable);
            provider.putPieceLabel(saisieRow, rowValsPiece);
            getNewMouvement(GenerationMvtSaisieAchat.source, this.saisieRow.getID(), 1, rowValsPiece);
        } else {
            SQLRowValues rowValsPiece = pieceTable.getTable("MOUVEMENT").getRow(idMvt).getForeign("ID_PIECE").asRowValues();
            provider.putPieceLabel(this.saisieRow, rowValsPiece);
            rowValsPiece.update();

            this.mEcritures.put("ID_MOUVEMENT", new Integer(this.idMvt));
        }

        // generation des ecritures + maj des totaux du compte associe

        // compte Achat
        int idCompteAchat = this.saisieRow.getInt("ID_COMPTE_PCE");

        if (idCompteAchat <= 1) {
            idCompteAchat = this.rowPrefsCompte.getInt("ID_COMPTE_PCE_ACHAT");
            if (idCompteAchat <= 1) {
                idCompteAchat = ComptePCESQLElement.getIdComptePceDefault("Achats");
            }
        }
        this.mEcritures.put("ID_COMPTE_PCE", new Integer(idCompteAchat));
        this.mEcritures.put("DEBIT", new Long(prixHT.getLongValue()));
        this.mEcritures.put("CREDIT", new Long(0));
        SQLRow rowEcr = ajoutEcriture();

        // addAssocAnalytiqueFromProvider(rowEcr, saisieRow);

        // compte TVA
        if (prixTVA.getLongValue() > 0) {
            int idCompteTVA;
            if (saisieRow.getBoolean("IMMO")) {
                idCompteTVA = rowPrefsCompte.getInt("ID_COMPTE_PCE_TVA_IMMO");
                if (idCompteTVA <= 1) {
                    idCompteTVA = ComptePCESQLElement.getIdComptePceDefault("TVAImmo");
                }
            } else {
                SQLRowAccessor rowCptTva = this.saisieRow.getForeign("ID_TAXE").getForeign(rowFournisseur.getBoolean("UE") ? "ID_COMPTE_PCE_DED_INTRA" : "ID_COMPTE_PCE_DED");
                if (rowCptTva == null || rowCptTva.isUndefined()) {
                    idCompteTVA = rowPrefsCompte.getInt("ID_COMPTE_PCE_TVA_ACHAT");
                    if (idCompteTVA <= 1) {
                        idCompteTVA = ComptePCESQLElement.getIdComptePceDefault("TVADeductible");
                    }
                } else {
                    idCompteTVA = rowCptTva.getID();
                }
            }
            this.mEcritures.put("ID_COMPTE_PCE", new Integer(idCompteTVA));
            this.mEcritures.put("DEBIT", new Long(prixTVA.getLongValue()));
            this.mEcritures.put("CREDIT", new Long(0));
            ajoutEcriture();

            if (rowFournisseur.getBoolean("UE")) {
                SQLRowAccessor rowCptTva = this.saisieRow.getForeign("ID_TAXE").getForeign("ID_COMPTE_PCE_COLLECTE_INTRA");
                int idCompteTVAIntra;
                if (rowCptTva == null || rowCptTva.isUndefined()) {
                    idCompteTVAIntra = rowPrefsCompte.getInt("ID_COMPTE_PCE_TVA_INTRA");

                    if (idCompteTVAIntra <= 1) {
                        idCompteTVAIntra = ComptePCESQLElement.getIdComptePceDefault("TVAIntraComm");
                    }
                } else {
                    idCompteTVAIntra = rowCptTva.getID();
                }
                this.mEcritures.put("ID_COMPTE_PCE", new Integer(idCompteTVAIntra));
                this.mEcritures.put("DEBIT", new Long(0));
                this.mEcritures.put("CREDIT", new Long(prixTVA.getLongValue()));
                ajoutEcriture();
            }
        }

        // compte Fournisseurs
        int idCompteFourn = rowFournisseur.getInt("ID_COMPTE_PCE");

        if (idCompteFourn <= 1) {
            idCompteFourn = rowPrefsCompte.getInt("ID_COMPTE_PCE_FOURNISSEUR");
            if (idCompteFourn <= 1) {
                idCompteFourn = ComptePCESQLElement.getIdComptePceDefault("Fournisseurs");
            }
        }
        this.mEcritures.put("ID_COMPTE_PCE", new Integer(idCompteFourn));
        this.mEcritures.put("DEBIT", new Long(0));
        if (rowFournisseur.getBoolean("UE")) {
            this.mEcritures.put("CREDIT", new Long(prixHT.getLongValue()));
        } else {
            this.mEcritures.put("CREDIT", new Long(prixTTC.getLongValue()));
        }
        ajoutEcriture();

        new GenerationMvtReglementAchat(this.saisieRow, this.idMvt);

        // Mise à jour de la clef etrangere mouvement sur la saisie achat
        SQLRowValues valAchat = this.saisieRow.createEmptyUpdateRow();
        valAchat.put("ID_MOUVEMENT", new Integer(this.idMvt));
        valAchat.update();
        displayMvtNumber();
    }

    public void run() {
        try {
            genereMouvement();
        } catch (Exception e) {
            ExceptionHandler.handle("Erreur pendant la générations des écritures comptables", e);
        }
    }
}
