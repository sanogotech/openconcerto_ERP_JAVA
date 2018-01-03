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
 
 package org.openconcerto.erp.config.update;

import org.openconcerto.erp.config.InstallationPanel;
import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.SQLField.Properties;
import org.openconcerto.sql.model.SQLName;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.request.UpdateBuilder;
import org.openconcerto.sql.utils.AlterTable;
import org.openconcerto.sql.utils.SQLCreateTable;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

public class Updater_1_5 {
    private static final String COMPTE_CLIENT_TRANSACTION = "COMPTE_CLIENT_TRANSACTION";

    public static void update(final DBRoot root) throws SQLException {
        // Champ obsolete sur compte
        SQLTable tableCompte = root.getTable("COMPTE_PCE");
        if (!tableCompte.contains("OBSOLETE")) {
            final AlterTable alter = new AlterTable(tableCompte);
            alter.addBooleanColumn("OBSOLETE", Boolean.FALSE, false);
            tableCompte.getBase().getDataSource().execute(alter.asString());
            tableCompte.getSchema().updateVersion();
            tableCompte.fetchFields();
        }

        // Transaction du solde
        if (!root.contains(COMPTE_CLIENT_TRANSACTION)) {
            final SQLCreateTable createTable = new SQLCreateTable(root, COMPTE_CLIENT_TRANSACTION);
            createTable.addForeignColumn("CLIENT");
            createTable.addDateAndTimeColumn("DATE");
            createTable.addDecimalColumn("MONTANT", 16, 6, BigDecimal.valueOf(0), false);
            createTable.addForeignColumn("MODE_REGLEMENT");
            createTable.addForeignColumn("MOUVEMENT");
            try {
                root.getBase().getDataSource().execute(createTable.asString());
                InstallationPanel.insertUndef(createTable);
                root.refetchTable(COMPTE_CLIENT_TRANSACTION);
                root.getSchema().updateVersion();
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création de la table " + COMPTE_CLIENT_TRANSACTION, ex);
            }
        }
        // Solde
        final SQLTable tClient = root.getTable("CLIENT");
        if (!tClient.contains("SOLDE_COMPTE")) {
            final AlterTable alterClient = new AlterTable(tClient);
            alterClient.addDecimalColumn("SOLDE_COMPTE", 16, 6, BigDecimal.valueOf(0), false);
            tClient.getBase().getDataSource().execute(alterClient.asString());
            tClient.getSchema().updateVersion();
            tClient.fetchFields();
        }

        final SQLTable tCompteClient = root.getTable("COMPTE_CLIENT_TRANSACTION");
        if (!tCompteClient.contains("ID_MOUVEMENT")) {
            final AlterTable alterClient = new AlterTable(tCompteClient);
            alterClient.addForeignColumn("ID_MOUVEMENT", root.getTable("MOUVEMENT"));
            tClient.getBase().getDataSource().execute(alterClient.asString());
            tClient.getSchema().updateVersion();
            tClient.fetchFields();
        }

        final SQLTable tCmdClient = root.getTable("COMMANDE_CLIENT");
        if (!tCmdClient.contains("DATE_LIVRAISON_PREV")) {
            final AlterTable alterCmdClient = new AlterTable(tCmdClient);
            alterCmdClient.addColumn("DATE_LIVRAISON_PREV", "date");
            tCmdClient.getBase().getDataSource().execute(alterCmdClient.asString());
            tCmdClient.getSchema().updateVersion();
            tCmdClient.fetchFields();
        }

        {
            // Ajout du champ SANS_VALEUR_ENCAISSEMENT pour gérer les anciens cheques sans le compte
            // 511
            List<String> tablesCheque = Arrays.asList("CHEQUE_A_ENCAISSER", "CHEQUE_FOURNISSEUR");

            for (String string : tablesCheque) {

                final SQLTable table = root.getTable(string);
                if (!table.contains("SANS_VALEUR_ENCAISSEMENT")) {
                    AlterTable alterElt = new AlterTable(table);
                    alterElt.addBooleanColumn("SANS_VALEUR_ENCAISSEMENT", Boolean.FALSE, false);
                    table.getBase().getDataSource().execute(alterElt.asString());
                    root.refetchTable(string);
                    root.getSchema().updateVersion();
                    UpdateBuilder upBuilder = new UpdateBuilder(table);
                    upBuilder.setObject("SANS_VALEUR_ENCAISSEMENT", Boolean.TRUE);
                    table.getBase().getDataSource().execute(upBuilder.asString());
                }
            }

            SQLTable tableEncElt = root.getTable("ENCAISSER_MONTANT_ELEMENT");
            if (tableEncElt.getField("DATE").getType().getType() == Types.TIMESTAMP) {
                AlterTable t = new AlterTable(tableEncElt);
                t.alterColumn("DATE", EnumSet.allOf(Properties.class), "date", null, Boolean.TRUE);
                tableEncElt.getBase().getDataSource().execute(t.asString());
                root.refetchTable(tableEncElt.getName());
                root.getSchema().updateVersion();
            }
        }

        // TVA Intra
        final SQLTable tTva = root.getTable("TAXE");
        if (!tTva.contains("ID_COMPTE_PCE_COLLECTE_INTRA")) {
            final AlterTable alterTaxe = new AlterTable(tTva);
            alterTaxe.addForeignColumn("ID_COMPTE_PCE_COLLECTE_INTRA", root.getTable("COMPTE_PCE"));
            alterTaxe.addForeignColumn("ID_COMPTE_PCE_DED_INTRA", root.getTable("COMPTE_PCE"));
            tTva.getBase().getDataSource().execute(alterTaxe.asString());
            tTva.getSchema().updateVersion();
            tTva.fetchFields();
        }

        if (!root.contains("TAXE_COMPLEMENTAIRE")) {
            final SQLCreateTable createTable = new SQLCreateTable(root, "TAXE_COMPLEMENTAIRE");
            createTable.addForeignColumn("ID_COMPTE_PCE_PRODUITS", root.getTable("COMPTE_PCE"));
            createTable.addForeignColumn("ID_COMPTE_PCE", root.getTable("COMPTE_PCE"));
            createTable.addDecimalColumn("POURCENT", 16, 6, BigDecimal.valueOf(0), false);
            createTable.addVarCharColumn("CODE", 25);
            createTable.addVarCharColumn("NOM", 256);

            try {
                root.getBase().getDataSource().execute(createTable.asString());
                InstallationPanel.insertUndef(createTable);
                root.refetchTable("TAXE_COMPLEMENTAIRE");
                root.getSchema().updateVersion();
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création de la table " + "TAXE_COMPLEMENTAIRE", ex);
            }

            SQLTable tableArt = root.getTable("ARTICLE");
            final AlterTable alterArt = new AlterTable(tableArt);
            alterArt.addForeignColumn("ID_TAXE_COMPLEMENTAIRE", root.getTable("TAXE_COMPLEMENTAIRE"));
            tableArt.getBase().getDataSource().execute(alterArt.asString());
            tableArt.getSchema().updateVersion();
            tableArt.fetchFields();
        }

        // GED
        if (!root.contains("ATTACHMENT")) {
            final SQLCreateTable createTable = new SQLCreateTable(root, "ATTACHMENT");
            createTable.addVarCharColumn("SOURCE_TABLE", 128);
            createTable.addIntegerColumn("SOURCE_ID", 0);
            createTable.addVarCharColumn("NAME", 256);
            createTable.addVarCharColumn("MIMETYPE", 256);
            createTable.addVarCharColumn("FILENAME", 256);
            createTable.addLongColumn("FILESIZE", 0L, false);
            createTable.addVarCharColumn("STORAGE_PATH", 256);
            createTable.addVarCharColumn("STORAGE_FILENAME", 256);
            createTable.addVarCharColumn("DIRECTORY", 256);
            createTable.addVarCharColumn("THUMBNAIL", 256);
            createTable.addIntegerColumn("THUMBNAIL_WIDTH", 32);
            createTable.addIntegerColumn("THUMBNAIL_HEIGHT", 32);
            createTable.addVarCharColumn("TAG", 128);
            createTable.addIntegerColumn("VERSION", 0);
            createTable.addVarCharColumn("HASH", 32);

            try {
                root.getBase().getDataSource().execute(createTable.asString());
                InstallationPanel.insertUndef(createTable);
                root.refetchTable("ATTACHMENT");
                root.getSchema().updateVersion();
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création de la table " + "ATTACHMENT", ex);
            }
        }
        SQLTable tableAttachment = root.getTable("ATTACHMENT");
        if (!tableAttachment.contains("DIRECTORY")) {
            final AlterTable alter = new AlterTable(tableAttachment);
            alter.addVarCharColumn("STORAGE_FILENAME", 256);
            alter.addVarCharColumn("DIRECTORY", 256);
            tableAttachment.getBase().getDataSource().execute(alter.asString());
            tableAttachment.getSchema().updateVersion();
            tableAttachment.fetchFields();
        }

        if (!tableAttachment.contains("VERSION")) {
            final AlterTable alter = new AlterTable(tableAttachment);
            alter.addIntegerColumn("VERSION", 0);
            alter.addVarCharColumn("HASH", 32);
            alter.addVarCharColumn("INFOS", 8000);
            tableAttachment.getBase().getDataSource().execute(alter.asString());
            tableAttachment.getSchema().updateVersion();
            tableAttachment.fetchFields();
        }

        List<String> gedTable = Arrays.asList("CLIENT", "MOUVEMENT", "FOURNISSEUR", "ARTICLE");
        for (String string : gedTable) {
            SQLTable tableGED = root.getTable(string);
            if (!tableGED.contains("ATTACHMENTS")) {
                final AlterTable alter = new AlterTable(tableGED);
                alter.addIntegerColumn("ATTACHMENTS", 0);
                tableGED.getBase().getDataSource().execute(alter.asString());
                tableGED.getSchema().updateVersion();
                tableGED.fetchFields();
            }
        }

        // gestion articles en attente
        {

            // Vente
            SQLTable tableBLElt = root.getTable("BON_DE_LIVRAISON_ELEMENT");
            if (!tableBLElt.contains("ID_COMMANDE_CLIENT_ELEMENT")) {
                AlterTable t = new AlterTable(tableBLElt);
                t.addForeignColumn("ID_COMMANDE_CLIENT_ELEMENT", root.getTable("COMMANDE_CLIENT_ELEMENT"));
                tableBLElt.getBase().getDataSource().execute(t.asString());
                root.refetchTable(tableBLElt.getName());
                root.getSchema().updateVersion();
            }

            SQLTable tableVFElt = root.getTable("SAISIE_VENTE_FACTURE_ELEMENT");
            if (!tableVFElt.contains("ID_COMMANDE_CLIENT_ELEMENT")) {
                AlterTable t = new AlterTable(tableVFElt);
                t.addForeignColumn("ID_COMMANDE_CLIENT_ELEMENT", root.getTable("COMMANDE_CLIENT_ELEMENT"));
                tableVFElt.getBase().getDataSource().execute(t.asString());
                root.refetchTable(tableVFElt.getName());
                root.getSchema().updateVersion();
            }

            SQLTable tableCmdElt = root.getTable("COMMANDE_CLIENT_ELEMENT");
            if (!tableCmdElt.contains("LIVRE")) {
                AlterTable t = new AlterTable(tableCmdElt);
                t.addBooleanColumn("LIVRE_FORCED", Boolean.FALSE, false);
                t.addBooleanColumn("LIVRE", Boolean.TRUE, false);
                t.addDecimalColumn("QTE_LIVREE", 16, 6, BigDecimal.ZERO, true);
                tableCmdElt.getBase().getDataSource().execute(t.asString());
                root.refetchTable(tableCmdElt.getName());
                root.getSchema().updateVersion();

                SQLTable tableCmdCli = root.getTable("COMMANDE_CLIENT");
                SQLTable tableTR = root.getTable("TR_COMMANDE_CLIENT");
                SQLTable tableBL = root.getTable("BON_DE_LIVRAISON");
                SQLTable tableFactC = root.getTable("SAISIE_VENTE_FACTURE");
                String sel = "SELECT t.\"ID_COMMANDE_CLIENT\" FROM " + new SQLName(root.getName(), tableTR.getName()).quote() + " t ," + new SQLName(root.getName(), tableCmdCli.getName()).quote()
                        + " c ," + new SQLName(root.getName(), tableBL.getName()).quote() + " b ," + new SQLName(root.getName(), tableFactC.getName()).quote()
                        + " f WHERE c.\"ID\"=t.\"ID_COMMANDE_CLIENT\" AND f.\"ID\"=t.\"ID_SAISIE_VENTE_FACTURE\""
                        + " AND b.\"ID\"=t.\"ID_BON_DE_LIVRAISON\" AND b.\"ARCHIVE\" = 0 AND f.\"ID\" > 1 AND t.\"ID\" > 1 AND c.\"ID\" > 1 AND f.\"ARCHIVE\" = 0 AND t.\"ARCHIVE\" = 0 AND c.\"ARCHIVE\" = 0 GROUP BY t.\"ID_COMMANDE_CLIENT\" HAVING (SUM(b.\"TOTAL_HT\")>=SUM(c.\"T_HT\") OR SUM(f.\"T_HT\")>=SUM(c.\"T_HT\")) ";
                List<Object> cmd = tableTR.getDBSystemRoot().getDataSource().executeCol(sel);
                UpdateBuilder build = new UpdateBuilder(tableCmdElt);
                build.set("QTE_LIVREE", "\"QTE\"*\"QTE_UNITAIRE\"");
                build.setObject("LIVRE_FORCED", Boolean.TRUE);
                final Where where = new Where(tableCmdElt.getField("ID_COMMANDE_CLIENT"), cmd);
                build.setWhere(where);
                // String up = "UPDATE " + new SQLName(root.getName(),
                // tableCmdElt.getName()).quote()
                // + " SET \"QTE_LIVREE\"=\"QTE\"*\"QTE_UNITAIRE\", \"LIVRE_FORCED\"=true WHERE
                // \"ID_COMMANDE_CLIENT\" IN []";
            }

            // Achat

            SQLTable tableBRElt = root.getTable("BON_RECEPTION_ELEMENT");
            if (!tableBRElt.contains("ID_COMMANDE_ELEMENT")) {
                AlterTable t = new AlterTable(tableBRElt);
                t.addForeignColumn("ID_COMMANDE_ELEMENT", root.getTable("COMMANDE_ELEMENT"));
                tableBRElt.getBase().getDataSource().execute(t.asString());
                root.refetchTable(tableBRElt.getName());
                root.getSchema().updateVersion();
            }

            SQLTable tableCmdFElt = root.getTable("COMMANDE_ELEMENT");
            if (!tableCmdFElt.contains("RECU")) {
                AlterTable t = new AlterTable(tableCmdFElt);
                t.addBooleanColumn("RECU_FORCED", Boolean.FALSE, false);
                t.addBooleanColumn("RECU", Boolean.TRUE, false);
                t.addDecimalColumn("QTE_RECUE", 16, 6, BigDecimal.ZERO, true);
                tableCmdFElt.getBase().getDataSource().execute(t.asString());
                root.refetchTable(tableCmdFElt.getName());
                root.getSchema().updateVersion();

                SQLTable tableCmdCli = root.getTable("COMMANDE");
                SQLTable tableTR = root.getTable("TR_COMMANDE");
                SQLTable tableBR = root.getTable("BON_RECEPTION");
                String sel = "SELECT t.\"ID_COMMANDE\" FROM " + new SQLName(root.getName(), tableTR.getName()).quote() + " t ," + new SQLName(root.getName(), tableCmdCli.getName()).quote() + " c ,"
                        + new SQLName(root.getName(), tableBR.getName()).quote() + " b WHERE c.\"ID\"=t.\"ID_COMMANDE\""
                        + " AND b.\"ID\"=t.\"ID_BON_RECEPTION\" AND b.\"ARCHIVE\" = 0 AND t.\"ID\" > 1 AND c.\"ID\" > 1 AND t.\"ARCHIVE\" = 0 AND c.\"ARCHIVE\" = 0 GROUP BY t.\"ID_COMMANDE\" HAVING (SUM(b.\"TOTAL_HT\")>=SUM(c.\"T_HT\")) ";
                List<Object> cmd = tableTR.getDBSystemRoot().getDataSource().executeCol(sel);
                UpdateBuilder build = new UpdateBuilder(tableCmdFElt);
                build.set("QTE_RECUE", "\"QTE\"*\"QTE_UNITAIRE\"");
                build.setObject("RECU_FORCED", Boolean.TRUE);
                final Where where = new Where(tableCmdFElt.getField("ID_COMMANDE"), cmd);
                build.setWhere(where);
                // String up = "UPDATE " + new SQLName(root.getName(),
                // tableCmdElt.getName()).quote()
                // + " SET \"QTE_LIVREE\"=\"QTE\"*\"QTE_UNITAIRE\", \"LIVRE_FORCED\"=true WHERE
                // \"ID_COMMANDE_CLIENT\" IN []";
            }

        }

    }

}
