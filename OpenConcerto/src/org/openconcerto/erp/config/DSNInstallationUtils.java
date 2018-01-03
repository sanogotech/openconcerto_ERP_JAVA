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
 
 package org.openconcerto.erp.config;

import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLName;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowListRSH;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLSyntax;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.request.Inserter;
import org.openconcerto.sql.request.Inserter.Insertion;
import org.openconcerto.sql.request.UpdateBuilder;
import org.openconcerto.sql.utils.AlterTable;
import org.openconcerto.sql.utils.ReOrder;
import org.openconcerto.sql.utils.SQLCreateTable;
import org.openconcerto.utils.Tuple2;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DSNInstallationUtils {

    private void insertUndef(final SQLCreateTable ct) throws SQLException {
        // check that we can use insertReturnFirstField()
        if (ct.getPrimaryKey().size() != 1)
            throw new IllegalStateException("Not one and only one field in the PK : " + ct.getPrimaryKey());
        final Insertion<?> insertion = new Inserter(ct).insertReturnFirstField("(" + SQLBase.quoteIdentifier(SQLSyntax.ORDER_NAME) + ") VALUES(" + ReOrder.MIN_ORDER + ")", false);
        assert insertion.getCount() == 1;
        if (insertion.getRows().size() != 1)
            throw new IllegalStateException("Missing ID " + insertion.getRows());
        SQLTable.setUndefID(ct.getRoot().getSchema(), ct.getName(), ((Number) insertion.getRows().get(0)).intValue());
    }

    private void insertValues(List<Tuple2<String, String>> values, SQLTable table) throws SQLException {
        SQLSelect sel = new SQLSelect();
        sel.addSelect(table.getField("CODE"));
        List<String> codes = (List<String>) table.getDBSystemRoot().getDataSource().executeA(sel.asString());
        for (Tuple2<String, String> tuple2 : values) {
            if (!codes.contains(tuple2.get0())) {
                SQLRowValues rowVals = new SQLRowValues(table);
                rowVals.put("CODE", tuple2.get0());
                rowVals.put("NOM", tuple2.get1());
                rowVals.commit();
            }
        }
    }

    public void updateDSNCommonTable(final DBRoot root) throws SQLException {

        SQLTable societeCommonT = root.getTable("SOCIETE_COMMON");
        if (!societeCommonT.contains("IBAN")) {
            AlterTable t = new AlterTable(societeCommonT);
            t.addVarCharColumn("IBAN", 256);
            t.addVarCharColumn("BIC", 256);
            root.getBase().getDataSource().execute(t.asString());
            root.refetchTable("SOCIETE_COMMON");
            root.getSchema().updateVersion();
        }
        if (!societeCommonT.contains("ORG_PROTECTION_SOCIAL_ID")) {
            AlterTable t = new AlterTable(societeCommonT);
            t.addVarCharColumn("ORG_PROTECTION_SOCIAL_ID", 256);
            root.getBase().getDataSource().execute(t.asString());
            root.refetchTable("SOCIETE_COMMON");
            root.getSchema().updateVersion();
        }

        SQLTable tableRubCot = root.getTable("RUBRIQUE_COTISATION");
        if (!tableRubCot.contains("ASSIETTE_PLAFONNEE")) {
            AlterTable tableRub = new AlterTable(tableRubCot);
            tableRub.addBooleanColumn("ASSIETTE_PLAFONNEE", false, false);
            root.getBase().getDataSource().execute(tableRub.asString());
            root.refetchTable("RUBRIQUE_COTISATION");
            root.getSchema().updateVersion();
        }

        if (!root.contains("CODE_CAISSE_TYPE_RUBRIQUE")) {
            final SQLCreateTable createTableCode = new SQLCreateTable(root, "CODE_CAISSE_TYPE_RUBRIQUE");
            createTableCode.addVarCharColumn("CODE", 25);
            createTableCode.addVarCharColumn("NOM", 512);
            createTableCode.addVarCharColumn("CAISSE_COTISATION", 512);

            try {
                root.getBase().getDataSource().execute(createTableCode.asString());
                insertUndef(createTableCode);
                root.refetchTable("CODE_CAISSE_TYPE_RUBRIQUE");

                final SQLTable table = root.getTable("CODE_CAISSE_TYPE_RUBRIQUE");

                DsnUrssafCode codeUrssaf = new DsnUrssafCode();
                codeUrssaf.insertCode(table);

                List<String> tableRubName = Arrays.asList("RUBRIQUE_BRUT", "RUBRIQUE_COTISATION", "RUBRIQUE_NET");
                for (String t : tableRubName) {
                    AlterTable tableRub = new AlterTable(root.getTable(t));
                    tableRub.addForeignColumn("ID_CODE_CAISSE_TYPE_RUBRIQUE", table);
                    root.getBase().getDataSource().execute(tableRub.asString());
                    root.refetchTable(t);
                }
                root.getSchema().updateVersion();

            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création de la table " + "CODE_CAISSE_TYPE_RUBRIQUE", ex);
            }
        }

        if (!root.contains("MOTIF_ARRET_TRAVAIL")) {
            final SQLCreateTable createTableMotif = new SQLCreateTable(root, "MOTIF_ARRET_TRAVAIL");
            createTableMotif.addVarCharColumn("CODE", 25);
            createTableMotif.addVarCharColumn("NOM", 512);

            try {
                root.getBase().getDataSource().execute(createTableMotif.asString());
                insertUndef(createTableMotif);
                root.refetchTable("MOTIF_ARRET_TRAVAIL");
                root.getSchema().updateVersion();

                final SQLTable table = root.getTable("MOTIF_ARRET_TRAVAIL");
                List<Tuple2<String, String>> v = new ArrayList<Tuple2<String, String>>();
                v.add(Tuple2.create("01", "maladie"));
                v.add(Tuple2.create("02", "maternité /adoption"));
                v.add(Tuple2.create("03", "paternité / accueil de l’enfant"));
                v.add(Tuple2.create("04", "congé suite à un accident de trajet"));
                v.add(Tuple2.create("05", "congé suite à maladie professionnelle"));
                v.add(Tuple2.create("06", "congé suite à accident de travail ou de service"));
                v.add(Tuple2.create("07", "femme enceinte dispensée de travail"));
                v.add(Tuple2.create("99", "annulation"));

                insertValues(v, table);
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création de la table " + "MOTIF_ARRET_TRAVAIL", ex);
            }
        }

        if (!root.contains("TYPE_PREAVIS")) {
            final SQLCreateTable createTableMotif = new SQLCreateTable(root, "TYPE_PREAVIS");
            createTableMotif.addVarCharColumn("CODE", 25);
            createTableMotif.addVarCharColumn("NOM", 512);

            try {
                root.getBase().getDataSource().execute(createTableMotif.asString());
                insertUndef(createTableMotif);
                root.refetchTable("TYPE_PREAVIS");
                root.getSchema().updateVersion();

                final SQLTable table = root.getTable("TYPE_PREAVIS");
                List<Tuple2<String, String>> v = new ArrayList<Tuple2<String, String>>();
                v.add(Tuple2.create("01", "préavis effectué et payé"));
                v.add(Tuple2.create("02", "préavis non effectué et payé"));
                v.add(Tuple2.create("03", "préavis non effectué et non payé"));
                v.add(Tuple2.create("10", "préavis non effectué non payé dans le cadre d’un contrat de sécurisation professionnelle (CSP)"));
                v.add(Tuple2.create("50", "préavis non effectué et payé dans le cadre d’un congé de reclassement"));
                v.add(Tuple2.create("51", "préavis non effectué et payé dans le cadre d’un congé de mobilité"));
                v.add(Tuple2.create("60", "Délai de prévenance"));
                v.add(Tuple2.create("90", "pas de clause de préavis applicable"));

                insertValues(v, table);
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création de la table " + "TYPE_PREAVIS", ex);
            }
        }

        if (!root.contains("CODE_BASE_ASSUJETTIE")) {
            final SQLCreateTable createTableCodeBase = new SQLCreateTable(root, "CODE_BASE_ASSUJETTIE");
            createTableCodeBase.addVarCharColumn("CODE", 25);
            createTableCodeBase.addVarCharColumn("NOM", 512);

            try {
                root.getBase().getDataSource().execute(createTableCodeBase.asString());
                insertUndef(createTableCodeBase);
                root.refetchTable("CODE_BASE_ASSUJETTIE");
                root.getSchema().updateVersion();

                final SQLTable table = root.getTable("CODE_BASE_ASSUJETTIE");
                List<Tuple2<String, String>> v = new ArrayList<Tuple2<String, String>>();

                v.add(Tuple2.create("02", "Assiette brute plafonnée"));
                v.add(Tuple2.create("03", "Assiette brute déplafonnée"));
                v.add(Tuple2.create("04", "Assiette de la contribution sociale généralisée"));
                v.add(Tuple2.create("07", "Assiette des contributions d'Assurance Chômage"));
                v.add(Tuple2.create("08", "Assiette retraite CPRP SNCF"));
                v.add(Tuple2.create("09", "Assiette de compensation bilatérale maladie CPRP SNCF"));
                v.add(Tuple2.create("10", "Base brute fiscale"));
                v.add(Tuple2.create("11", "Base forfaitaire soumise aux cotisations de Sécurité Sociale"));
                v.add(Tuple2.create("12", "Assiette du crédit d'impôt compétitivité-emploi"));
                v.add(Tuple2.create("13", "Assiette du forfait social à 8%"));
                v.add(Tuple2.create("14", "Assiette du forfait social à 20%"));

                insertValues(v, table);
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création de la table " + "CODE_BASE_ASSUJETTIE", ex);
            }
        }
        if (!tableRubCot.contains("ID_CODE_BASE_ASSUJETTIE")) {
            AlterTable alterTableCot = new AlterTable(tableRubCot);
            alterTableCot.addForeignColumn("ID_CODE_BASE_ASSUJETTIE", root.getTable("CODE_BASE_ASSUJETTIE"));
            root.getBase().getDataSource().execute(alterTableCot.asString());
            root.refetchTable("RUBRIQUE_COTISATION");
            root.getSchema().updateVersion();
        }

        if (!root.contains("CODE_TYPE_RUBRIQUE_BRUT")) {
            final SQLCreateTable createTableCodeBase = new SQLCreateTable(root, "CODE_TYPE_RUBRIQUE_BRUT");
            createTableCodeBase.addVarCharColumn("CODE", 25);
            createTableCodeBase.addVarCharColumn("NOM", 512);
            createTableCodeBase.addVarCharColumn("TYPE", 512);

            try {
                root.getBase().getDataSource().execute(createTableCodeBase.asString());
                insertUndef(createTableCodeBase);
                root.refetchTable("CODE_TYPE_RUBRIQUE_BRUT");
                root.getSchema().updateVersion();

                DsnBrutCode brutCode = new DsnBrutCode();
                brutCode.insertCode(root.getTable("CODE_TYPE_RUBRIQUE_BRUT"));
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création de la table " + "CODE_BASE_ASSUJETTIE", ex);
            }
        }

        SQLTable tableRubBrut = root.getTable("RUBRIQUE_BRUT");
        if (!tableRubBrut.contains("ID_CODE_TYPE_RUBRIQUE_BRUT")) {

            AlterTable alterTableBrut = new AlterTable(tableRubBrut);
            alterTableBrut.addForeignColumn("ID_CODE_TYPE_RUBRIQUE_BRUT", root.getTable("CODE_TYPE_RUBRIQUE_BRUT"));
            root.getBase().getDataSource().execute(alterTableBrut.asString());
            root.refetchTable("RUBRIQUE_BRUT");
            root.getSchema().updateVersion();
        }

        SQLTable tableRubNet = root.getTable("RUBRIQUE_NET");
        if (!tableRubNet.contains("ID_CODE_TYPE_RUBRIQUE_BRUT")) {

            AlterTable alterTableNet = new AlterTable(tableRubNet);
            alterTableNet.addForeignColumn("ID_CODE_TYPE_RUBRIQUE_BRUT", root.getTable("CODE_TYPE_RUBRIQUE_BRUT"));
            root.getBase().getDataSource().execute(alterTableNet.asString());
            root.refetchTable("RUBRIQUE_NET");
            root.getSchema().updateVersion();
        }

        if (!root.contains("DSN_REGIME_LOCAL")) {
            final SQLCreateTable createTableMotif = new SQLCreateTable(root, "DSN_REGIME_LOCAL");
            createTableMotif.addVarCharColumn("CODE", 25);
            createTableMotif.addVarCharColumn("NOM", 512);

            try {
                root.getBase().getDataSource().execute(createTableMotif.asString());
                insertUndef(createTableMotif);
                root.refetchTable("DSN_REGIME_LOCAL");
                root.getSchema().updateVersion();

                final SQLTable table = root.getTable("DSN_REGIME_LOCAL");
                List<Tuple2<String, String>> v = new ArrayList<Tuple2<String, String>>();
                v.add(Tuple2.create("99", "non applicable"));
                v.add(Tuple2.create("01", "régime local Alsace Moselle"));
                insertValues(v, table);
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création de la table " + "DSN_REGIME_LOCAL", ex);
            }
        }

        if (!root.contains("CONTRAT_MODALITE_TEMPS")) {
            final SQLCreateTable createTableMotif = new SQLCreateTable(root, "CONTRAT_MODALITE_TEMPS");
            createTableMotif.addVarCharColumn("CODE", 25);
            createTableMotif.addVarCharColumn("NOM", 512);

            try {
                root.getBase().getDataSource().execute(createTableMotif.asString());
                insertUndef(createTableMotif);
                root.refetchTable("CONTRAT_MODALITE_TEMPS");
                root.getSchema().updateVersion();

                final SQLTable table = root.getTable("CONTRAT_MODALITE_TEMPS");
                List<Tuple2<String, String>> v = new ArrayList<Tuple2<String, String>>();
                v.add(Tuple2.create("10", "temps plein"));
                v.add(Tuple2.create("20", "temps partiel"));
                v.add(Tuple2.create("21", "temps partiel thérapeutique"));
                v.add(Tuple2.create("99", "salarié non concerné"));
                insertValues(v, table);
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création de la table " + "CONTRAT_MODALITE_TEMPS", ex);
            }
        }

        if (!root.contains("CONTRAT_REGIME_MALADIE")) {
            final SQLCreateTable createTableMotif = new SQLCreateTable(root, "CONTRAT_REGIME_MALADIE");
            createTableMotif.addVarCharColumn("CODE", 25);
            createTableMotif.addVarCharColumn("NOM", 512);

            try {
                root.getBase().getDataSource().execute(createTableMotif.asString());
                insertUndef(createTableMotif);
                root.refetchTable("CONTRAT_REGIME_MALADIE");
                root.getSchema().updateVersion();

                final SQLTable table = root.getTable("CONTRAT_REGIME_MALADIE");
                List<Tuple2<String, String>> v = new ArrayList<Tuple2<String, String>>();
                v.add(Tuple2.create("134", "régime spécial de la SNCF"));
                v.add(Tuple2.create("135", "régime spécial de la RATP"));
                v.add(Tuple2.create("136", "établissement des invalides de la marine (ENIM)"));
                v.add(Tuple2.create("137", "mineurs ou assimilés (CANMSS)"));
                v.add(Tuple2.create("138", "militaires de carrière (CNMSS)"));
                v.add(Tuple2.create("140", "clercs et employés de notaires (CRPCEN)"));
                v.add(Tuple2.create("141", "chambre de commerce et d'industrie de Paris"));
                v.add(Tuple2.create("144", "Assemblée Nationale"));
                v.add(Tuple2.create("145", "Sénat"));
                v.add(Tuple2.create("146", "port autonome de Bordeaux"));
                v.add(Tuple2.create("147", "industries électriques et gazières (CAMIEG)"));
                v.add(Tuple2.create("149", "régimes des cultes (CAVIMAC)"));
                v.add(Tuple2.create("200", "régime général (CNAM)"));
                v.add(Tuple2.create("300", "régime agricole (MSA)"));
                v.add(Tuple2.create("400", "régime spécial Banque de France"));
                v.add(Tuple2.create("900", "autre régime (réservé Polynésie Française, Nouvelle Calédonie)"));
                v.add(Tuple2.create("999", "autre"));

                insertValues(v, table);
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création de la table " + "CONTRAT_REGIME_MALADIE", ex);
            }
        }

        if (!root.contains("CONTRAT_REGIME_VIEILLESSE")) {
            final SQLCreateTable createTableMotif = new SQLCreateTable(root, "CONTRAT_REGIME_VIEILLESSE");
            createTableMotif.addVarCharColumn("CODE", 25);
            createTableMotif.addVarCharColumn("NOM", 512);

            try {
                root.getBase().getDataSource().execute(createTableMotif.asString());
                insertUndef(createTableMotif);
                root.refetchTable("CONTRAT_REGIME_VIEILLESSE");
                root.getSchema().updateVersion();

                final SQLTable table = root.getTable("CONTRAT_REGIME_VIEILLESSE");
                List<Tuple2<String, String>> v = new ArrayList<Tuple2<String, String>>();
                v.add(Tuple2.create("120", "retraite des agents des collectivités locales (CNRACL)"));
                v.add(Tuple2.create("121", "pensions des ouvriers des établissements industriels de l'Etat (FSPOEIE)"));
                v.add(Tuple2.create("122", "pensions civiles et militaires de retraite de l'Etat (SRE)"));
                v.add(Tuple2.create("134", "régime spécial de la SNCF"));
                v.add(Tuple2.create("135", "régime spécial de la RATP"));
                v.add(Tuple2.create("136", "établissement des invalides de la marine (ENIM)"));
                v.add(Tuple2.create("137", "mineurs ou assimilés (fonds Caisse des Dépôts)"));
                v.add(Tuple2.create("139", "Banque de France"));
                v.add(Tuple2.create("140", "clercs et employés de notaires (CRPCEN)"));
                v.add(Tuple2.create("141", "chambre de commerce et d'industrie de Paris"));
                v.add(Tuple2.create("144", "Assemblée Nationale"));
                v.add(Tuple2.create("145", "Sénat"));
                v.add(Tuple2.create("147", "industries électriques et gazières (CNIEG)"));
                v.add(Tuple2.create("149", "régime des cultes (CAVIMAC)"));
                v.add(Tuple2.create("157", "régime de retraite des avocats (CNBF)"));
                v.add(Tuple2.create("158", "SEITA"));
                v.add(Tuple2.create("159", "Comédie Française"));
                v.add(Tuple2.create("160", "Opéra de Paris"));
                v.add(Tuple2.create("200", "régime général (CNAV)"));
                v.add(Tuple2.create("300", "régime agricole (MSA)"));
                v.add(Tuple2.create("900", "autre régime (réservé Polynésie Française, Nouvelle Calédonie, Principauté de Monaco)"));
                v.add(Tuple2.create("903", "salariés étrangers exemptés d'affiliation pour le risque vieillesse"));
                v.add(Tuple2.create("999", "cas particuliers d'affiliation"));

                insertValues(v, table);
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création de la table " + "CONTRAT_REGIME_VIEILLESSE", ex);
            }
        }

        if (!root.contains("CONTRAT_MOTIF_RECOURS")) {
            final SQLCreateTable createTableMotif = new SQLCreateTable(root, "CONTRAT_MOTIF_RECOURS");
            createTableMotif.addVarCharColumn("CODE", 25);
            createTableMotif.addVarCharColumn("NOM", 512);

            try {
                root.getBase().getDataSource().execute(createTableMotif.asString());
                insertUndef(createTableMotif);
                root.refetchTable("CONTRAT_MOTIF_RECOURS");
                root.getSchema().updateVersion();

                final SQLTable table = root.getTable("CONTRAT_MOTIF_RECOURS");
                List<Tuple2<String, String>> v = new ArrayList<Tuple2<String, String>>();
                v.add(Tuple2.create("01", "Remplacement d'un salarié"));
                v.add(Tuple2.create("02", "Accroissement temporaire de l'activité de l'entreprise"));
                v.add(Tuple2.create("03", "Emplois à caractère saisonnier"));
                v.add(Tuple2.create("04", "Contrat vendanges"));
                v.add(Tuple2.create("05", "Contrat à durée déterminée d’usage"));
                v.add(Tuple2.create("06", "Contrat à durée déterminée à objet défini"));
                v.add(Tuple2.create("07", "Remplacement d'un chef d'entreprise"));
                v.add(Tuple2.create("08", "Remplacement du chef d'une exploitation agricole"));
                v.add(Tuple2.create("09", "Recrutement de personnes sans emploi rencontrant des difficultés sociales et professionnelles particulières"));
                v.add(Tuple2.create("10", "Complément de formation professionnelle au salarié"));
                v.add(Tuple2.create("11",
                        "Formation professionnelle au salarié par la voie de l'apprentissage, en vue de l'obtention d'une qualification professionnelle sanctionnée par un diplôme ou un titre à finalité professionnelle enregistré au répertoire national des certifications professionnelles"));
                v.add(Tuple2.create("12", "Remplacement d’un salarié passé provisoirement à temps partiel"));
                v.add(Tuple2.create("13", "Attente de la suppression définitive du poste du salarié ayant quitté définitivement l’entreprise"));
                insertValues(v, table);
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création de la table " + "CONTRAT_MOTIF_RECOURS", ex);
            }
        }

        if (!root.contains("CONTRAT_DETACHE_EXPATRIE")) {
            final SQLCreateTable createTableMotif = new SQLCreateTable(root, "CONTRAT_DETACHE_EXPATRIE");
            createTableMotif.addVarCharColumn("CODE", 25);
            createTableMotif.addVarCharColumn("NOM", 512);

            try {
                root.getBase().getDataSource().execute(createTableMotif.asString());
                insertUndef(createTableMotif);
                root.refetchTable("CONTRAT_DETACHE_EXPATRIE");
                root.getSchema().updateVersion();

                final SQLTable table = root.getTable("CONTRAT_DETACHE_EXPATRIE");
                List<Tuple2<String, String>> v = new ArrayList<Tuple2<String, String>>();
                v.add(Tuple2.create("01", "Détaché"));
                v.add(Tuple2.create("02", "Expatrié"));
                v.add(Tuple2.create("03", "Frontalier"));
                v.add(Tuple2.create("99", "Salarié non concerné"));
                insertValues(v, table);
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création de la table " + "CONTRAT_DETACHE_EXPATRIE", ex);
            }
        }

        if (!root.contains("DSN_NATURE")) {
            final SQLCreateTable createTableMotif = new SQLCreateTable(root, "DSN_NATURE");
            createTableMotif.addVarCharColumn("CODE", 25);
            createTableMotif.addVarCharColumn("NOM", 512);

            try {
                root.getBase().getDataSource().execute(createTableMotif.asString());
                insertUndef(createTableMotif);
                root.refetchTable("DSN_NATURE");
                root.getSchema().updateVersion();

                final SQLTable table = root.getTable("DSN_NATURE");
                List<Tuple2<String, String>> v = new ArrayList<Tuple2<String, String>>();
                v.add(Tuple2.create("01", "DSN Mensuelle"));
                v.add(Tuple2.create("02", "Signalement Fin du contrat de travail"));
                v.add(Tuple2.create("04", "Signalement Arrêt de travail"));
                v.add(Tuple2.create("05", "Signalement Reprise suite à arrêt de travail"));
                v.add(Tuple2.create("06", "DSN reprise d'historique"));
                insertValues(v, table);
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création de la table " + "DSN_NATURE", ex);
            }
        }

        if (!root.contains("DSN_TYPE")) {
            final SQLCreateTable createTableMotif = new SQLCreateTable(root, "DSN_TYPE");
            createTableMotif.addVarCharColumn("CODE", 25);
            createTableMotif.addVarCharColumn("NOM", 512);

            try {
                root.getBase().getDataSource().execute(createTableMotif.asString());
                insertUndef(createTableMotif);
                root.refetchTable("DSN_TYPE");
                root.getSchema().updateVersion();

                final SQLTable table = root.getTable("DSN_TYPE");
                List<Tuple2<String, String>> v = new ArrayList<Tuple2<String, String>>();
                v.add(Tuple2.create("01", "déclaration normale"));
                v.add(Tuple2.create("02", "déclaration normale néant"));
                v.add(Tuple2.create("03", "déclaration annule et remplace intégral"));
                v.add(Tuple2.create("04", "déclaration annule"));
                v.add(Tuple2.create("05", "annule et remplace néant"));
                insertValues(v, table);
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création de la table " + "DSN_TYPE", ex);
            }
        }

        if (!root.contains("CONTRAT_DISPOSITIF_POLITIQUE")) {
            final SQLCreateTable createTableMotif = new SQLCreateTable(root, "CONTRAT_DISPOSITIF_POLITIQUE");
            createTableMotif.addVarCharColumn("CODE", 25);
            createTableMotif.addVarCharColumn("NOM", 512);

            try {
                root.getBase().getDataSource().execute(createTableMotif.asString());
                insertUndef(createTableMotif);
                root.refetchTable("CONTRAT_DISPOSITIF_POLITIQUE");
                root.getSchema().updateVersion();

                final SQLTable table = root.getTable("CONTRAT_DISPOSITIF_POLITIQUE");
                List<Tuple2<String, String>> v = new ArrayList<Tuple2<String, String>>();
                v.add(Tuple2.create("21", "CUI - Contrat Initiative Emploi"));
                v.add(Tuple2.create("41", "CUI - Contrat d'Accompagnement dans l'Emploi"));
                v.add(Tuple2.create("42", "CUI - Contrat d'accès à l'emploi - DOM"));
                v.add(Tuple2.create("50", "Emploi d'avenir secteur marchand"));
                v.add(Tuple2.create("51", "Emploi d'avenir secteur non marchand"));
                v.add(Tuple2.create("61", "Contrat de Professionnalisation"));
                v.add(Tuple2.create("64", "Contrat d'apprentissage entreprises artisanales ou de moins de 11 salariés (loi du 3 janvier 1979)"));
                v.add(Tuple2.create("65", "Contrat d’apprentissage entreprises non inscrites au répertoire des métiers d’au moins 11 salariés (loi de 1987)"));
                v.add(Tuple2.create("70", "Contrat à durée déterminée pour les séniors"));
                v.add(Tuple2.create("71", "Contrat à durée déterminée d’insertion"));
                v.add(Tuple2.create("80", "Contrat de génération"));
                v.add(Tuple2.create("81", "Contrat d'apprentissage secteur public (Loi de 1992)"));
                v.add(Tuple2.create("82", "Contrat à durée indéterminée intérimaire"));
                v.add(Tuple2.create("99", "Non concerné"));
                insertValues(v, table);
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création de la table " + "CONTRAT_DISPOSITIF_POLITIQUE", ex);
            }
        }

        if (!root.contains("MOTIF_REPRISE_ARRET_TRAVAIL")) {
            final SQLCreateTable createTableMotif = new SQLCreateTable(root, "MOTIF_REPRISE_ARRET_TRAVAIL");
            createTableMotif.addVarCharColumn("CODE", 25);
            createTableMotif.addVarCharColumn("NOM", 512);

            try {
                root.getBase().getDataSource().execute(createTableMotif.asString());
                insertUndef(createTableMotif);
                root.refetchTable("MOTIF_REPRISE_ARRET_TRAVAIL");
                root.getSchema().updateVersion();

                final SQLTable table = root.getTable("MOTIF_REPRISE_ARRET_TRAVAIL");
                List<Tuple2<String, String>> v = new ArrayList<Tuple2<String, String>>();
                v.add(Tuple2.create("01", "reprise normale"));
                v.add(Tuple2.create("02", "reprise temps partiel thérapeutique"));
                v.add(Tuple2.create("03", "reprise temps partiel raison personnelle"));
                insertValues(v, table);
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création de la table " + "MOTIF_REPRISE_ARRET_TRAVAIL", ex);
            }
        }

        if (!root.contains("MOTIF_FIN_CONTRAT")) {
            final SQLCreateTable createTableMotif = new SQLCreateTable(root, "MOTIF_FIN_CONTRAT");
            createTableMotif.addVarCharColumn("CODE", 25);
            createTableMotif.addVarCharColumn("NOM", 512);

            try {
                root.getBase().getDataSource().execute(createTableMotif.asString());
                insertUndef(createTableMotif);
                root.refetchTable("MOTIF_FIN_CONTRAT");
                root.getSchema().updateVersion();

                final SQLTable table = root.getTable("MOTIF_FIN_CONTRAT");
                List<Tuple2<String, String>> v = new ArrayList<Tuple2<String, String>>();
                v.add(Tuple2.create("011", "licenciement suite à liquidation judiciaire ou à redressement judiciaire"));
                v.add(Tuple2.create("012", "licenciement suite à fermeture définitive de l'établissement"));
                v.add(Tuple2.create("014", "licenciement pour motif économique"));
                v.add(Tuple2.create("015", "licenciement pour fin de chantier"));
                v.add(Tuple2.create("020", "licenciement pour autre motif"));
                v.add(Tuple2.create("025", "autre fin de contrat pour motif économique"));
                v.add(Tuple2.create("026", "rupture pour motif économique dans le cadre d’un contrat de sécurisation professionnelle CSP"));
                v.add(Tuple2.create("031", "fin de contrat à durée déterminée ou fin d'accueil occasionnel"));
                v.add(Tuple2.create("032", "fin de mission d'intérim"));
                v.add(Tuple2.create("033", "rupture anticipée d’un CDD ou d’un contrat de mission en cas d’inaptitude physique constatée par le médecin du travail"));
                v.add(Tuple2.create("034", "fin de période d'essai à l'initiative de l'employeur"));
                v.add(Tuple2.create("035", "fin de période d'essai à l'initiative du salarié"));
                v.add(Tuple2.create("036", " rupture anticipée d'un CDD, d'un contrat d'apprentissage ou d’un contrat de mission à l'initiative de l'employeur"));
                v.add(Tuple2.create("037", "rupture anticipée d'un CDD, d'un contrat d'apprentissage ou d’un contrat de mission à l'initiative du salarié"));
                v.add(Tuple2.create("038", "mise à la retraite par l'employeur"));
                v.add(Tuple2.create("039", "départ à la retraite à l'initiative du salarié"));
                v.add(Tuple2.create("043", "rupture conventionnelle"));
                v.add(Tuple2.create("058", "prise d'acte de la rupture de contrat de travail"));
                v.add(Tuple2.create("059", "démission"));
                v.add(Tuple2.create("065", "décès de l'employeur ou internement / conduit à un licenciement autre motif"));
                v.add(Tuple2.create("066", "décès du salarié / rupture force majeure"));
                v.add(Tuple2.create("081", "fin de contrat d'apprentissage"));
                v.add(Tuple2.create("082", "résiliation judiciaire du contrat de travail"));
                v.add(Tuple2.create("083", "rupture de contrat de travail ou d’un contrat de mission pour force majeure"));
                v.add(Tuple2.create("084", "rupture d'un commun accord du CDD, du contrat d'apprentissage ou d’un contrat de mission"));
                v.add(Tuple2.create("085", "fin de mandat"));
                v.add(Tuple2.create("086", "licenciement convention CATS"));
                v.add(Tuple2.create("087", "licenciement pour faute grave"));
                v.add(Tuple2.create("088", "licenciement pour faute lourde"));
                v.add(Tuple2.create("089", "licenciement pour force majeure"));
                v.add(Tuple2.create("091", "licenciement pour inaptitude physique d'origine non professionnelle"));
                v.add(Tuple2.create("092", "licenciement pour inaptitude physique d'origine professionnelle"));
                v.add(Tuple2.create("093", "licenciement suite à décision d'une autorité administrative"));
                v.add(Tuple2.create("094", "rupture anticipée du contrat de travail pour arrêt de tournage"));
                v.add(Tuple2.create("095", "rupture anticipée du contrat de travail ou d’un contrat de mission pour faute grave"));
                v.add(Tuple2.create("096", "rupture anticipée du contrat de travail ou d’un contrat de mission pour faute lourde"));
                v.add(Tuple2.create("097", "rupture anticipée d’un contrat de travail ou d’un contrat de mission suite à fermeture de l'établissement"));
                v.add(Tuple2.create("098", "retrait d'enfant"));
                v.add(Tuple2.create("998", "transfert du contrat de travail sans rupture du contrat vers un autre établissement n'effectuant pas encore de DSN"));
                v.add(Tuple2.create("999", "fin de relation avec l’employeur (autres que contrat de travail) pour les cas ne portant aucun impact sur l’Assurance chômage"));
                insertValues(v, table);
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création de la table " + "MOTIF_FIN_CONTRAT", ex);
            }
        }

        DSNUpdateRubrique dsnUpdateRubrique = new DSNUpdateRubrique(root);
        dsnUpdateRubrique.updateRubriqueCotisation();

        // PHASE 3
        {
            SQLTable tableCaisseCotisation = root.findTable("CAISSE_COTISATION");
            if (!tableCaisseCotisation.contains("ORG_PROTECTION_SOCIALE")) {
                final AlterTable alterCaisse = new AlterTable(tableCaisseCotisation);
                alterCaisse.addBooleanColumn("ORG_PROTECTION_SOCIALE", Boolean.FALSE, false);
                alterCaisse.addBooleanColumn("URSSAF", Boolean.FALSE, false);

                root.getBase().getDataSource().execute(alterCaisse.asString());
                root.refetchTable("CAISSE_COTISATION");
                root.getSchema().updateVersion();

                {
                    UpdateBuilder upCaisse = new UpdateBuilder(tableCaisseCotisation);
                    upCaisse.setObject("ORG_PROTECTION_SOCIALE", Boolean.TRUE);
                    upCaisse.setWhere(new Where(tableCaisseCotisation.getField("NOM"), Arrays.asList("URSSAF", "AGIRC", "ARRCO")));
                    root.getBase().getDataSource().execute(upCaisse.asString());
                }
                {
                    UpdateBuilder upCaisse = new UpdateBuilder(tableCaisseCotisation);
                    upCaisse.setObject("URSSAF", Boolean.TRUE);
                    upCaisse.setWhere(new Where(tableCaisseCotisation.getField("NOM"), Arrays.asList("URSSAF")));
                    root.getBase().getDataSource().execute(upCaisse.asString());
                }
            }
            if (!root.contains("CAISSE_MODE_PAIEMENT")) {
                final SQLCreateTable createCaisseMode = new SQLCreateTable(root, "CAISSE_MODE_PAIEMENT");
                createCaisseMode.addVarCharColumn("CODE", 25);
                createCaisseMode.addVarCharColumn("NOM", 512);

                try {
                    root.getBase().getDataSource().execute(createCaisseMode.asString());
                    insertUndef(createCaisseMode);
                    root.refetchTable("CAISSE_MODE_PAIEMENT");
                    root.getSchema().updateVersion();

                    final SQLTable table = root.getTable("CAISSE_MODE_PAIEMENT");
                    List<Tuple2<String, String>> v = new ArrayList<Tuple2<String, String>>();
                    v.add(Tuple2.create("01", "chèque"));
                    v.add(Tuple2.create("02", "virement"));
                    v.add(Tuple2.create("03", "prélèvement"));
                    v.add(Tuple2.create("04", "titre inter-bancaire de paiement"));
                    v.add(Tuple2.create("05", "prélèvement SEPA"));
                    v.add(Tuple2.create("06", "versement réalisé par un autre établissement"));

                    insertValues(v, table);
                } catch (SQLException ex) {
                    throw new IllegalStateException("Erreur lors de la création de la table " + "CAISSE_MODE_PAIEMENT", ex);
                }

                final SQLCreateTable createCaisseREnseignement = new SQLCreateTable(root, "CAISSE_COTISATION_RENSEIGNEMENT");
                createCaisseREnseignement.addVarCharColumn("IDENTIFIANT", 256);
                createCaisseREnseignement.addVarCharColumn("SIRET", 256);
                createCaisseREnseignement.addVarCharColumn("BIC", 256);
                createCaisseREnseignement.addVarCharColumn("IBAN", 256);
                createCaisseREnseignement.addVarCharColumn("ENTITE_AFFECTATION", 256);
                createCaisseREnseignement.addBooleanColumn("ORGANISME_COMPLEMENTAIRE", Boolean.FALSE, false);
                createCaisseREnseignement.addBooleanColumn("PAIEMENT_TRIMESTRIEL", Boolean.FALSE, false);
                createCaisseREnseignement.addVarCharColumn("CODE_DELEGATAIRE", 256);
                createCaisseREnseignement.addForeignColumn("ID_CAISSE_MODE_PAIEMENT", root.getTable("CAISSE_MODE_PAIEMENT"));
                createCaisseREnseignement.addForeignColumn("ID_CAISSE_COTISATION", root.getTable("CAISSE_COTISATION"));
                createCaisseREnseignement.addForeignColumn("ID_SOCIETE_COMMON", root.getTable("SOCIETE_COMMON"));
                root.getBase().getDataSource().execute(createCaisseREnseignement.asString());
                insertUndef(createCaisseREnseignement);
                root.refetchTable("CAISSE_COTISATION_RENSEIGNEMENT");
                root.getSchema().updateVersion();
            }
            SQLTable tableCR = root.getTable("CAISSE_COTISATION_RENSEIGNEMENT");
            if (!tableCR.contains("PAIEMENT_TRIMESTRIEL")) {
                AlterTable alter = new AlterTable(tableCR);
                alter.addBooleanColumn("PAIEMENT_TRIMESTRIEL", Boolean.FALSE, false);
                root.getBase().getDataSource().execute(alter.asString());
                root.refetchTable("CAISSE_COTISATION_RENSEIGNEMENT");
                root.getSchema().updateVersion();
            }
        }

        if (!root.contains("TYPE_COMPOSANT_BASE_ASSUJETTIE")) {
            final SQLCreateTable createTableTypeComposant = new SQLCreateTable(root, "TYPE_COMPOSANT_BASE_ASSUJETTIE");
            createTableTypeComposant.addVarCharColumn("CODE", 25);
            createTableTypeComposant.addVarCharColumn("NOM", 512);

            try {
                root.getBase().getDataSource().execute(createTableTypeComposant.asString());
                insertUndef(createTableTypeComposant);
                root.refetchTable("TYPE_COMPOSANT_BASE_ASSUJETTIE");
                root.getSchema().updateVersion();

                final SQLTable table = root.getTable("TYPE_COMPOSANT_BASE_ASSUJETTIE");
                List<Tuple2<String, String>> v = new ArrayList<Tuple2<String, String>>();

                v.add(Tuple2.create("01", "Montant du SMIC retenu pour le calcul de la Réduction générale des cotisations patronales de sécurité sociale"));
                v.add(Tuple2.create("02", "Montant du SMIC retenu pour le calcul du crédit d'impôt compétitivité-emploi"));
                v.add(Tuple2.create("03", "Contributions patronales à des régimes complémentaires de retraite"));
                v.add(Tuple2.create("04", "Contributions patronales destinées au financement des prestations de prévoyance complémentaire"));
                v.add(Tuple2.create("05", "Contributions patronales destinées au financement des prestations de retraite supplémentaire"));
                v.add(Tuple2.create("06", "Plafond calculé pour salarié poly-employeurs"));
                v.add(Tuple2.create("10", "Salaire brut Prévoyance"));
                v.add(Tuple2.create("11", "Tranche A Prévoyance"));
                v.add(Tuple2.create("12", "Tranche 2 Prévoyance"));
                v.add(Tuple2.create("13", "Tranche B Prévoyance"));
                v.add(Tuple2.create("14", "Tranche C Prévoyance"));
                v.add(Tuple2.create("15", "Tranche D Prévoyance"));
                v.add(Tuple2.create("16", "Tranche D1 Prévoyance"));
                v.add(Tuple2.create("17", "Base spécifique Prévoyance"));
                v.add(Tuple2.create("18", "Base forfaitaire Prévoyance"));
                v.add(Tuple2.create("19", "Base fictive Prévoyance reconstituée"));
                v.add(Tuple2.create("20", "Montant forfaitaire Prévoyance"));
                v.add(Tuple2.create("21", "Montant Prévoyance libre ou exceptionnel"));
                v.add(Tuple2.create("22", "Montant des indemnités journalières CRPCEN"));
                v.add(Tuple2.create("90", "Retenue sur salaire"));
                v.add(Tuple2.create("91", "Base de taxe sur les salaires au taux normal"));

                insertValues(v, table);

                AlterTable tableRubCotis = new AlterTable(tableRubCot);
                tableRubCotis.addForeignColumn("ID_TYPE_COMPOSANT_BASE_ASSUJETTIE", root.getTable("TYPE_COMPOSANT_BASE_ASSUJETTIE"));
                root.getBase().getDataSource().execute(tableRubCotis.asString());
                root.refetchTable(tableRubCot.getName());
                root.getSchema().updateVersion();
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création de la table " + "TYPE_COMPOSANT_BASE_ASSUJETTIE", ex);
            }
        }
        if (!root.contains("CODE_COTISATION_INDIVIDUELLE")) {
            final SQLCreateTable createTableTypeComposant = new SQLCreateTable(root, "CODE_COTISATION_INDIVIDUELLE");
            createTableTypeComposant.addVarCharColumn("CODE", 25);
            createTableTypeComposant.addVarCharColumn("NOM", 512);

            try {
                root.getBase().getDataSource().execute(createTableTypeComposant.asString());
                insertUndef(createTableTypeComposant);
                root.refetchTable("CODE_COTISATION_INDIVIDUELLE");
                root.getSchema().updateVersion();

                final SQLTable table = root.getTable("CODE_COTISATION_INDIVIDUELLE");
                List<Tuple2<String, String>> v = new ArrayList<Tuple2<String, String>>();

                v.add(Tuple2.create("001", "Exonération de cotisations au titre de l'emploi d'un apprenti (loi de 1979)"));
                v.add(Tuple2.create("002", "Exonération de cotisations au titre de l'emploi d'un apprenti (loi de 1987)"));
                v.add(Tuple2.create("003", "Exonération de cotisations au titre de l'emploi d'un apprenti (loi de 1992)"));
                v.add(Tuple2.create("004", "Exonération de cotisations au titre de l'emploi d'un salarié en contrat d'accès à l'emploi"));
                v.add(Tuple2.create("006", "Exonération de cotisations au titre de l'emploi d'un salarié en contrat d'accompagnement dans l'emploi"));
                v.add(Tuple2.create("008", "Exonération de cotisations au titre de l'emploi d'un salarié en contrat de professionnalisation"));
                v.add(Tuple2.create("009", "Exonération de cotisations applicable aux associations intermédiaires"));
                v.add(Tuple2.create("010", "Exonération de cotisations applicable aux entreprises des bassins d'emploi à redynamiser"));
                v.add(Tuple2.create("011", "Exonération de cotisations applicable au créateur ou repreneur d'entreprise"));
                v.add(Tuple2.create("012", "Exonération de cotisations applicable dans les DOM"));
                v.add(Tuple2.create("013", "Exonération de cotisations applicable aux entreprises et associations d'aide à domicile"));
                v.add(Tuple2.create("014", "Exonérations de cotisations applicable aux entreprises innovantes ou universitaires"));
                v.add(Tuple2.create("015", "Exonération de cotisations applicable aux entreprises en zones franches urbaines"));
                v.add(Tuple2.create("016", "Exonération de cotisations applicable aux organismes d'intérêt général en zones de revitalisation rurale"));
                v.add(Tuple2.create("017", "Exonération de cotisations applicable aux structures agréées de l'aide sociale"));
                v.add(Tuple2.create("018", "Réduction générale des cotisations patronales de sécurité sociale"));
                v.add(Tuple2.create("019", "Réduction de cotisations applicable aux entreprises des zones de restructuration de la défense"));
                v.add(Tuple2.create("020", "Réduction de cotisations au titre de l'embauche du 1er au 50ème salarié en zones de revitalisation rurale"));
                v.add(Tuple2.create("021", "Déduction patronale au titre des heures supplémentaires"));
                v.add(Tuple2.create("022", "Exonération de cotisations applicable à une gratification de stage"));
                v.add(Tuple2.create("023", "Exonération de cotisation des sommes provenant d'un CET et réaffectées à un PERCO ou à un régime de retraite supplémentaire"));
                v.add(Tuple2.create("025", "Exonération de cotisations au titre de l’emploi d’un salarié en chantier et atelier d'insertion"));
                v.add(Tuple2.create("027", "Exonération Personnel technique CUMA, hors ateliers"));
                v.add(Tuple2.create("028", "Réduction Travailleur Occasionnel"));
                v.add(Tuple2.create("029", "CNIEG Réduction employeurs petit pool"));
                v.add(Tuple2.create("030", "Camieg Cotisation employeurs Régime spécial Complémentaire"));
                v.add(Tuple2.create("031", "Camieg Cotisation salariés Régime spécial Complémentaire"));
                v.add(Tuple2.create("032", "Camieg Cotisation salariés Régime spécial Solidarité"));
                v.add(Tuple2.create("033", "CNIEG Cotisation employeurs complément d'invalidité"));
                v.add(Tuple2.create("034", "CNIEG Cotisation employeurs régime de droit commun (population adossée)"));
                v.add(Tuple2.create("035", "CNIEG Cotisation employeurs Régime spécial (population adossée)"));
                v.add(Tuple2.create("036", "CNIEG Cotisation employeurs régime spécial (population non adossée)"));
                v.add(Tuple2.create("037", "CNIEG Cotisation salariés régime de droit commun (population adossée)"));
                v.add(Tuple2.create("038", "CNIEG Cotisation salariés régime spécial (population non adossée)"));
                v.add(Tuple2.create("039", "CNIEG Cotisations employeurs petit pool"));
                v.add(Tuple2.create("040", "Cotisation AC : assurance chômage sur rémunérations brutes après déduction, limitées à 4 fois le plafond de la SS"));
                v.add(Tuple2.create("041", "Cotisation AC majorée 1 : application d’une majoration AC + 0,5% sur les contrats d’usage inférieurs ou égaux à 3 mois"));
                v.add(Tuple2.create("042", "Cotisation AC majorée 2 : application d’une majoration AC + 3% sur les contrats d’accroissement temporaire d’activité inférieurs ou égaux à 1 mois"));
                v.add(Tuple2.create("043",
                        "Cotisation AC majorée 3 : application d’une majoration AC + 1,5% sur les contrats d’accroissement temporaire d’activité supérieurs à 1 mois mais inférieurs ou égaux à 3 mois"));
                v.add(Tuple2.create("044", "Exonération de cotisation chômage pour les moins de 26 ans"));
                v.add(Tuple2.create("045", "Cotisation Accident du travail"));
                v.add(Tuple2.create("046", "Cotisation AEF Bourse de l'emploi"));
                v.add(Tuple2.create("047", "Cotisation AEF CESA"));
                v.add(Tuple2.create("048", "Cotisation AGS : assurance garantie des salaires sur rémunérations brutes après déduction, limitées à 4 fois le plafond de la sécurité sociale"));
                v.add(Tuple2.create("049", "Cotisation Allocation de logement (FNAL)"));
                v.add(Tuple2.create("051", "Cotisation Formation professionnelle ADEFA"));
                v.add(Tuple2.create("052", "Cotisation AFNCA, ANEFA, PROVEA, ASCPA"));
                v.add(Tuple2.create("053", "Cotisation Formation professionnelle additionnelle FAFSEA"));
                v.add(Tuple2.create("054", "Cotisation Formation professionnelle AREFA"));
                v.add(Tuple2.create("055", "Cotisation Formation professionnelle CEREFAR"));
                v.add(Tuple2.create("056", "Cotisation Formation professionnelle FAFSEA"));
                v.add(Tuple2.create("057", "Cotisation Formation professionnelle FAFSEA CDD"));
                v.add(Tuple2.create("058", "Cotisation Formation professionnelle FAFSEA des communes forestières"));
                v.add(Tuple2.create("059", "Cotisation individuelle Prévoyance-Assurance-Mutuelle pour la période et l'affiliation concernées"));
                v.add(Tuple2.create("060", "Cotisation IRCANTEC Tranche A"));
                v.add(Tuple2.create("061", "Cotisation IRCANTEC Tranche B"));
                v.add(Tuple2.create("063", "RETA Montant de cotisation Arrco"));
                v.add(Tuple2.create("064", "RETC Montant de cotisation Agirc"));
                v.add(Tuple2.create("065", "Cotisation CRPCEN"));
                v.add(Tuple2.create("066", "Cotisation caisse de congés spectacles"));
                v.add(Tuple2.create("068", "Contribution solidarité autonomie"));
                v.add(Tuple2.create("069", "Contribution sur avantage de pré-retraite entreprise à dater du 11/10/2007 (CAPE)"));
                v.add(Tuple2.create("070", "Contribution sur avantage de pré-retraite entreprise aux taux normal (CAPE)"));
                v.add(Tuple2.create("071", "Contribution forfait social"));
                v.add(Tuple2.create("072", "Contribution sociale généralisée/salaires partiellement déductibles"));
                v.add(Tuple2.create("073", "CSG/CRDS sur participation intéressement épargne salariale"));
                v.add(Tuple2.create("074", "Cotisation Allocation familiale taux normal  "));
                v.add(Tuple2.create("075", "Cotisation Assurance Maladie"));
                v.add(Tuple2.create("076", "Cotisation Assurance Vieillesse"));
                v.add(Tuple2.create("077", "Montant de la retenue à la source effectuée sur les salaires versés aux personnes domiciliées hors de France"));
                v.add(Tuple2.create("078", "Pénalité de 1% emploi sénior"));
                v.add(Tuple2.create("079", "Remboursement de la dette sociale"));
                v.add(Tuple2.create("081", "Versement transport"));
                v.add(Tuple2.create("082", "Versement transport additionnel"));
                v.add(Tuple2.create("086", "Cotisation pénibilité mono exposition"));
                v.add(Tuple2.create("087", "Cotisation pénibilité multi exposition"));
                v.add(Tuple2.create("088", "Exonération versement transport"));
                v.add(Tuple2.create("089", "Exonération Contrat Initiative Emploi"));
                v.add(Tuple2.create("090", "Exonération accueillants familiaux"));
                v.add(Tuple2.create("091", "Cotisation Service de santé au travail"));
                v.add(Tuple2.create("092", "Cotisation Association pour l'emploi des cadres ingénieurs et techniciens de l'agriculture (APECITA)"));
                v.add(Tuple2.create("093", "Contribution sur indemnités de mise à la retraite"));
                v.add(Tuple2.create("094", "Exonération cotisations Allocations familiales (SICAE)"));
                v.add(Tuple2.create("096", "Cotisation CRPNPAC au fonds de retraite"));
                v.add(Tuple2.create("097", "Cotisation CRPNPAC au fonds d'assurance"));
                v.add(Tuple2.create("098", "Cotisation CRPNPAC au fonds de majoration"));
                v.add(Tuple2.create("099", "Contribution stock options"));
                v.add(Tuple2.create("100", "Contribution pour le financement des organisations syndicales de salariés et organisations professionnelles d'employeurs"));
                v.add(Tuple2.create("101", "Association Mutualisation du Coût Inaptitude"));
                v.add(Tuple2.create("102", "Cotisation Allocation Familiale - taux réduit"));
                v.add(Tuple2.create("103", "Contribution actions gratuites"));
                v.add(Tuple2.create("226", "Assiette du Versement Transport"));
                v.add(Tuple2.create("901", "Cotisation épargne retraite"));

                insertValues(v, table);

                List<Tuple2<String, String>> vCodeBase = new ArrayList<Tuple2<String, String>>();
                vCodeBase.add(Tuple2.create("15", "CNIEG-Assiette brute du régime spécial"));
                vCodeBase.add(Tuple2.create("16", "CNIEG-Assiette brute du complément invalidité"));
                vCodeBase.add(Tuple2.create("17", "CNIEG - Assiette brute du petit pool"));
                vCodeBase.add(Tuple2.create("18", "Camieg - assiette brute plafonnée"));
                vCodeBase.add(Tuple2.create("19", "Assiette CRPCEN"));
                vCodeBase.add(Tuple2.create("20", "CIBTP - Base brute de cotisations congés payés"));
                vCodeBase.add(Tuple2.create("21", "CIBTP - Base brute de cotisations OPPBTP permanents"));
                vCodeBase.add(Tuple2.create("22", "Base brute spécifique"));
                vCodeBase.add(Tuple2.create("23", "Base exceptionnelle (Agirc Arrco)"));
                vCodeBase.add(Tuple2.create("24", "Base plafonnée spécifique"));
                vCodeBase.add(Tuple2.create("25", "Assiette de contribution libératoire"));
                vCodeBase.add(Tuple2.create("27", "Assiette Caisse de congés spectacles"));
                vCodeBase.add(Tuple2.create("28", "Base IRCANTEC cotisée"));
                vCodeBase.add(Tuple2.create("29", "Base IRCANTEC non cotisée (arrêt de travail)"));
                vCodeBase.add(Tuple2.create("31", "Eléments de cotisation Prévoyance, Santé, retraite supplémentaire"));
                vCodeBase.add(Tuple2.create("33", "Assiette Contribution sur les avantages de préretraite entreprise"));
                vCodeBase.add(Tuple2.create("34", "CIBTP -Base plafonnée de cotisations intempéries gros oeuvre travaux publics"));
                vCodeBase.add(Tuple2.create("35", "CIBTP -Base plafonnée de cotisations intempéries second oeuvre"));
                vCodeBase.add(Tuple2.create("36", "CIBTP -Base \"A\" de cotisations organisme professionnel BTP"));
                vCodeBase.add(Tuple2.create("37", "Assiette de pénibilité"));
                vCodeBase.add(Tuple2.create("38", "Rémunération pour le calcul de la réduction Travailleur Occasionnel"));
                vCodeBase.add(Tuple2.create("39", "CIBTP -Base \"B\" de cotisations organisme professionnel BTP"));
                vCodeBase.add(Tuple2.create("40", "CIBTP -Base \"C\" de cotisations organisme professionnel BTP"));
                vCodeBase.add(Tuple2.create("41", "CRPNPAC-Assiette soumise au taux normal (non-plafonnée)"));
                vCodeBase.add(Tuple2.create("42", "CRPNPAC-Assiette soumise au taux majoré (non-plafonnée)"));
                vCodeBase.add(Tuple2.create("43", "Base plafonnée exceptionnelle Agirc Arrco"));
                vCodeBase.add(Tuple2.create("44", "Assiette du forfait social à 16%"));
                vCodeBase.add(Tuple2.create("45", "Base plafonnée ICP Agirc-Arrco"));
                vCodeBase.add(Tuple2.create("90", "Autre revenu net imposable"));
                insertValues(vCodeBase, root.getTable("CODE_BASE_ASSUJETTIE"));

                AlterTable tableRubCotis = new AlterTable(tableRubCot);
                tableRubCotis.addForeignColumn("ID_CODE_COTISATION_INDIVIDUELLE", root.getTable("CODE_COTISATION_INDIVIDUELLE"));
                root.getBase().getDataSource().execute(tableRubCotis.asString());
                root.refetchTable(tableRubCot.getName());
                root.getSchema().updateVersion();
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création de la table " + "CODE_COTISATION_INDIVIDUELLE", ex);
            }
            {
                final SQLTable tableCodeBase = root.getTable("CODE_BASE_ASSUJETTIE");
                SQLSelect selCodeBase = new SQLSelect();
                selCodeBase.addSelectStar(tableCodeBase);
                List<SQLRow> rowsCodeBase = SQLRowListRSH.execute(selCodeBase);
                Map<String, SQLRow> mapCodeBase = new HashMap<String, SQLRow>();
                for (SQLRow sqlRow : rowsCodeBase) {

                    final String string = sqlRow.getString("CODE");
                    mapCodeBase.put(string, sqlRow);
                }
                final SQLTable tableCodeTypeComp = root.getTable("TYPE_COMPOSANT_BASE_ASSUJETTIE");
                SQLSelect selCodeTypeComp = new SQLSelect();
                selCodeTypeComp.addSelectStar(tableCodeTypeComp);
                List<SQLRow> rowsTypeComp = SQLRowListRSH.execute(selCodeTypeComp);
                Map<String, SQLRow> mapTypeComp = new HashMap<String, SQLRow>();
                for (SQLRow sqlRow : rowsTypeComp) {
                    final String string = sqlRow.getString("CODE");
                    mapTypeComp.put(string, sqlRow);
                }
                final SQLTable tableCodeCotInd = root.getTable("CODE_COTISATION_INDIVIDUELLE");
                SQLSelect selCodeCodeInd = new SQLSelect();
                selCodeCodeInd.addSelectStar(tableCodeCotInd);
                List<SQLRow> rowsCodeInd = SQLRowListRSH.execute(selCodeCodeInd);
                Map<String, SQLRow> mapCodeInd = new HashMap<String, SQLRow>();
                for (SQLRow sqlRow : rowsCodeInd) {
                    final String string = sqlRow.getString("CODE");
                    mapCodeInd.put(string, sqlRow);
                }
                final SQLTable tableCaisse = root.getTable("CAISSE_COTISATION");
                SQLSelect selCodeCodeCaisse = new SQLSelect();
                selCodeCodeCaisse.addSelectStar(tableCaisse);
                List<SQLRow> rowsCodeCaisse = SQLRowListRSH.execute(selCodeCodeCaisse);
                Map<String, SQLRow> mapCodeCaisse = new HashMap<String, SQLRow>();
                for (SQLRow sqlRow : rowsCodeCaisse) {
                    final String string = sqlRow.getString("NOM");
                    mapCodeCaisse.put(string, sqlRow);
                }
                if (mapCodeCaisse.containsKey("ARRCO")) {
                    UpdateBuilder updaterRubCot = new UpdateBuilder(tableRubCot);
                    updaterRubCot.setObject("ID_CODE_BASE_ASSUJETTIE", mapCodeBase.get("02").getID());
                    updaterRubCot.setObject("ID_CODE_COTISATION_INDIVIDUELLE", mapCodeInd.get("063").getID());
                    // updaterRubCot.setObject("ID_TYPE_COMPOSANT_BASE_ASSUJETTIE",
                    // mapTypeComp.get("03").getID());
                    updaterRubCot.setWhere(new Where(tableRubCot.getField("ID_CAISSE_COTISATION"), "=", mapCodeCaisse.get("ARRCO").getID()));
                    root.getBase().getDataSource().execute(updaterRubCot.asString());
                }
                if (mapCodeCaisse.containsKey("AGIRC")) {
                    UpdateBuilder updaterRubCot = new UpdateBuilder(tableRubCot);
                    updaterRubCot.setObject("ID_CODE_BASE_ASSUJETTIE", mapCodeBase.get("03").getID());
                    updaterRubCot.setObject("ID_CODE_COTISATION_INDIVIDUELLE", mapCodeInd.get("064").getID());
                    // updaterRubCot.setObject("ID_TYPE_COMPOSANT_BASE_ASSUJETTIE",
                    // mapTypeComp.get("03").getID());
                    updaterRubCot.setWhere(new Where(tableRubCot.getField("ID_CAISSE_COTISATION"), "=", mapCodeCaisse.get("AGIRC").getID()));
                    root.getBase().getDataSource().execute(updaterRubCot.asString());
                }
            }
        }

        if (!tableRubNet.contains("ID_CODE_COTISATION_INDIVIDUELLE")) {
            AlterTable alterRubNet = new AlterTable(tableRubNet);
            alterRubNet.addForeignColumn("ID_CODE_COTISATION_INDIVIDUELLE", root.getTable("CODE_COTISATION_INDIVIDUELLE"));
            root.getBase().getDataSource().execute(alterRubNet.asString());
            root.refetchTable(tableRubNet.getName());
            root.getSchema().updateVersion();
        }
        if (!tableRubNet.contains("ID_TYPE_COMPOSANT_BASE_ASSUJETTIE")) {
            AlterTable alterRubNet = new AlterTable(tableRubNet);
            alterRubNet.addForeignColumn("ID_TYPE_COMPOSANT_BASE_ASSUJETTIE", root.getTable("TYPE_COMPOSANT_BASE_ASSUJETTIE"));
            root.getBase().getDataSource().execute(alterRubNet.asString());
            root.refetchTable(tableRubNet.getName());
            root.getSchema().updateVersion();
        }
        if (!tableRubNet.contains("ID_CODE_BASE_ASSUJETTIE")) {
            AlterTable alterRubNet = new AlterTable(tableRubNet);
            alterRubNet.addForeignColumn("ID_CODE_BASE_ASSUJETTIE", root.getTable("CODE_BASE_ASSUJETTIE"));
            root.getBase().getDataSource().execute(alterRubNet.asString());
            root.refetchTable(tableRubNet.getName());
            root.getSchema().updateVersion();
        }

        if (!tableRubNet.contains("ID_CAISSE_COTISATION")) {
            AlterTable alterRubNet = new AlterTable(tableRubNet);
            alterRubNet.addForeignColumn("ID_CAISSE_COTISATION", root.getTable("CAISSE_COTISATION"));
            root.getBase().getDataSource().execute(alterRubNet.asString());
            root.refetchTable(tableRubNet.getName());
            root.getSchema().updateVersion();
        }

        if (!root.contains("CODE_COTISATION_ETABLISSEMENT")) {
            final SQLCreateTable createTableTypeComposant = new SQLCreateTable(root, "CODE_COTISATION_ETABLISSEMENT");
            createTableTypeComposant.addVarCharColumn("CODE", 25);
            createTableTypeComposant.addVarCharColumn("NOM", 512);

            try {
                root.getBase().getDataSource().execute(createTableTypeComposant.asString());
                insertUndef(createTableTypeComposant);
                root.refetchTable("CODE_COTISATION_ETABLISSEMENT");
                root.getSchema().updateVersion();

                final SQLTable table = root.getTable("CODE_COTISATION_ETABLISSEMENT");
                List<Tuple2<String, String>> v = new ArrayList<Tuple2<String, String>>();

                v.add(Tuple2.create("001", "Cotisation ADPFA (Association pour le Développement du Paritarisme des Fleuristes et Animaliers)"));
                v.add(Tuple2.create("002", "Cotisation APNAB (Association Paritaire Nationale pour le développement de la négociation collective dans l'Artisanat du Bâtiment)"));
                v.add(Tuple2.create("003 ", "Cotisation sur assiette avec congés payés CCCA-BTP (Comité de Concertation et de Coordination de l'apprentissage du Bâtiment et des Travaux Publics)"));
                v.add(Tuple2.create("004 ", "Cotisation CPPNTT (Commission Paritaire Professionnelle Nationale du Travail Temporaire)"));
                v.add(Tuple2.create("005 ", "Cotisation Développement du paritarisme"));
                v.add(Tuple2.create("006 ", "Cotisation Dialogue social"));
                v.add(Tuple2.create("007 ", "Cotisation FAF (Fonds d'Assurance formation)"));
                v.add(Tuple2.create("009 ", "Cotisation FAPS (Fonds d'action professionnelle et sociale)"));
                v.add(Tuple2.create("010 ", "Cotisation FASTT (Fonds d'Action Sociale du Travail Temporaire)"));
                v.add(Tuple2.create("011 ", "Cotisation Fonds de péréquation"));
                v.add(Tuple2.create("012 ", "Cotisation IFC (Indemnités de fin de carrière)"));
                v.add(Tuple2.create("017 ", "Cotisation ORGA (Organisations Syndicales  du Travail Temporaire)"));
                v.add(Tuple2.create("018 ", "Cotisation Promotion et recrutement"));
                v.add(Tuple2.create("019 ", "Cotisations attachées à une population de non salariés ayants"));
                v.add(Tuple2.create("020 ", "Cotisations attachées à une population de non salariés retraités"));
                v.add(Tuple2.create("021 ", "Cotisations FMSE (Fond national agricole de mutualisation des risques sanitaires et environnementaux)"));
                v.add(Tuple2.create("022 ", "Cotisations VAL'HOR (association française pour la valorisation des produits et métiers de l'horticulture et du paysage)"));
                v.add(Tuple2.create("023 ", "Chiffre d'affaire"));
                v.add(Tuple2.create("024 ", "Nombre d'heures d'intérim"));
                v.add(Tuple2.create("025 ", "Contribution aux régimes supplémentaires de retraite à prestations définies - Rente"));
                v.add(Tuple2.create("026 ", "Contribution aux régimes supplémentaires de retraite à prestations définies - Prime"));
                v.add(Tuple2.create("027 ", "Contribution aux régimes supplémentaires de retraite à prestations définies - Dotations"));
                v.add(Tuple2.create("028 ", "Contribution additionnelle sur les rentes liquidées"));
                v.add(Tuple2.create("029 ", "Contribution aux régimes supplémentaires de retraite à prestations définies. Rente à taux 7%"));
                v.add(Tuple2.create("030 ", "Contribution aux régimes supplémentaires de retraite à prestations définies. Rente à taux 14%"));
                v.add(Tuple2.create("031 ", "Contribution additionnelle de solidarité pour l'autonomie"));
                v.add(Tuple2.create("032 ", "Contribution Sociale généralisée au taux de 3,80% + RDS sur revenu de remplacement "));
                v.add(Tuple2.create("033 ", "Contribution Sociale généralisée au taux de 6,20% + RDS sur revenu de remplacement "));
                v.add(Tuple2.create("034 ", "Contribution Sociale généralisée au taux de 6,60% + RDS sur revenu de remplacement "));
                v.add(Tuple2.create("035 ", "Contribution Sociale généralisée au taux de 7,50% + RDS sur revenu de remplacement "));
                v.add(Tuple2.create("036 ", "Cotisation TTC sur assiette CDD avec congés payés pour le secteur du BTP (Constructys Organisme Paritaire Collecteur Agréé pour le BTP)"));
                v.add(Tuple2.create("037 ", "Cotisation TTC sur assiette avec congés payés pour le secteur du BTP (Constructys Organisme Paritaire Collecteur Agréé pour le BTP)"));
                v.add(Tuple2.create("038 ", "Cotisation TTC  sur assiette sans  congés payés (Constructys Organisme Paritaire Collecteur Agréé pour le BTP)"));
                v.add(Tuple2.create("039 ",
                        "Cotisation TTC  sur assiette avec congés payés pour les salariés non soumis à la cotisation CCCA-BTP (Constructys Organisme Paritaire Collecteur Agréé pour le BTP)"));
                v.add(Tuple2.create("040 ",
                        "Cotisation TTC  sur assiette hors congés payés pour les salariés non soumis à la cotisation CCCA-BTP (Constructys Organisme Paritaire Collecteur Agréé pour le BTP)"));
                v.add(Tuple2.create("041 ", "Cotisation maladie sur les avantages de préretraite"));
                v.add(Tuple2.create("042 ", "Cotisation maladie sur les avantages de retraite"));
                v.add(Tuple2.create("043 ", "Cotisation maladie Alsace-Moselle sur les avantages de retraite"));
                v.add(Tuple2.create("044 ", "Cotisation forfait social à 8%"));
                v.add(Tuple2.create("045 ", "Cotisation forfait social à 20%"));
                v.add(Tuple2.create("090 ", "Cotisation spécifique Prévoyance"));

                insertValues(v, table);

                AlterTable tableRubCotis = new AlterTable(tableRubCot);
                tableRubCotis.addForeignColumn("ID_CODE_COTISATION_ETABLISSEMENT", root.getTable("CODE_COTISATION_ETABLISSEMENT"));
                root.getBase().getDataSource().execute(tableRubCotis.asString());
                root.refetchTable(tableRubCot.getName());
                root.getSchema().updateVersion();
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création de la table " + "CODE_COTISATION_ETABLISSEMENT", ex);
            }
        }

        if (!root.contains("CODE_PENIBILITE")) {
            final SQLCreateTable createTableTypeComposant = new SQLCreateTable(root, "CODE_PENIBILITE");
            createTableTypeComposant.addVarCharColumn("CODE", 25);
            createTableTypeComposant.addVarCharColumn("NOM", 512);

            try {
                root.getBase().getDataSource().execute(createTableTypeComposant.asString());
                insertUndef(createTableTypeComposant);
                root.refetchTable("CODE_PENIBILITE");
                root.getSchema().updateVersion();

                final SQLTable table = root.getTable("CODE_PENIBILITE");
                List<Tuple2<String, String>> v = new ArrayList<Tuple2<String, String>>();

                v.add(Tuple2.create("01", "les manutentions manuelles de charges"));
                v.add(Tuple2.create("02", "les postures pénibles (positions forcées des articulations)"));
                v.add(Tuple2.create("03", "les vibrations mécaniques"));
                v.add(Tuple2.create("04", "les agents chimiques dangereux"));
                v.add(Tuple2.create("05", "les activités exercées en milieu hyperbare"));
                v.add(Tuple2.create("06", "les températures extrêmes"));
                v.add(Tuple2.create("07", "le bruit"));
                v.add(Tuple2.create("08", "le travail de nuit"));
                v.add(Tuple2.create("09", "le travail en équipes successives alternantes"));
                v.add(Tuple2.create("10", "le travail répétitif (répétition d'un même geste, à une cadence contrainte avec un temps de cycle défini)"));
                v.add(Tuple2.create("99", "annulation"));

                insertValues(v, table);

            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création de la table " + "CODE_PENIBILITE", ex);
            }
        }

        if (!root.contains("AYANT_DROIT_TYPE")) {
            final SQLCreateTable createTableTypeAyantDroit = new SQLCreateTable(root, "AYANT_DROIT_TYPE");
            createTableTypeAyantDroit.addVarCharColumn("CODE", 25);
            createTableTypeAyantDroit.addVarCharColumn("NOM", 512);

            try {
                root.getBase().getDataSource().execute(createTableTypeAyantDroit.asString());
                insertUndef(createTableTypeAyantDroit);
                root.refetchTable("AYANT_DROIT_TYPE");
                root.getSchema().updateVersion();

                final SQLTable table = root.getTable("AYANT_DROIT_TYPE");
                List<Tuple2<String, String>> v = new ArrayList<Tuple2<String, String>>();

                v.add(Tuple2.create("01", "Adultes (conjoint, concubin, pacs)"));
                v.add(Tuple2.create("02", "Enfant"));
                v.add(Tuple2.create("03", "Autre (ascendant, collatéraux, ...)"));

                insertValues(v, table);

            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création de la table " + "AYANT_DROIT_TYPE", ex);
            }
        }

    }

    public void updateDSN(final DBRoot root) throws SQLException {
        final SQLTable tableCodeStatutCat = root.getTable("CODE_STATUT_CATEGORIEL");
        SQLRow rowNonCadre = tableCodeStatutCat.getRow(4);
        if (rowNonCadre != null) {
            rowNonCadre.createEmptyUpdateRow().put("CODE", "04").commit();
        }
        SQLRow rowSansStatut = tableCodeStatutCat.getRow(4);
        if (rowSansStatut != null) {
            rowSansStatut.createEmptyUpdateRow().put("CODE", "99").commit();
        }
        // 04 non cadre
        UpdateBuilder up04 = new UpdateBuilder(tableCodeStatutCat);
        up04.setObject("NOM", "Non cadre");
        up04.setWhere(new Where(tableCodeStatutCat.getField("CODE"), "=", "04"));
        root.getBase().getDataSource().execute(up04.asString());

        // 99 - pas de retraite complémentaire
        UpdateBuilder up99 = new UpdateBuilder(tableCodeStatutCat);
        up99.setObject("NOM", "Pas de retraite complémentaire");
        up99.setWhere(new Where(tableCodeStatutCat.getField("CODE"), "=", "99"));
        root.getBase().getDataSource().execute(up99.asString());

        if (!root.contains("ARRET_TRAVAIL")) {

            final SQLCreateTable createTable = new SQLCreateTable(root, "ARRET_TRAVAIL");
            createTable.addForeignColumn("SALARIE");
            createTable.addDateAndTimeColumn("DATE_DERNIER_JOUR_TRAV");
            createTable.addDateAndTimeColumn("DATE_FIN_PREV");
            createTable.addBooleanColumn("SUBROGATION", Boolean.FALSE, false);
            createTable.addDateAndTimeColumn("DATE_DEBUT_SUBROGATION");
            createTable.addDateAndTimeColumn("DATE_FIN_SUBROGATION");
            createTable.addForeignColumn("ID_MOTIF_ARRET_TRAVAIL", root.findTable("MOTIF_ARRET_TRAVAIL"));
            createTable.addForeignColumn("ID_MOTIF_REPRISE_ARRET_TRAVAIL", root.findTable("MOTIF_REPRISE_ARRET_TRAVAIL"));
            createTable.addDateAndTimeColumn("DATE_REPRISE");
            createTable.addDateAndTimeColumn("DATE_ACCIDENT");

            try {
                root.getBase().getDataSource().execute(createTable.asString());
                insertUndef(createTable);
                root.refetchTable("ARRET_TRAVAIL");
                root.getSchema().updateVersion();
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création de la table " + "ARRET_TRAVAIL", ex);
            }
        }

        if (!root.contains("FIN_CONTRAT")) {

            final SQLCreateTable createTable = new SQLCreateTable(root, "FIN_CONTRAT");
            createTable.addForeignColumn("FICHE_PAYE");
            createTable.addForeignColumn("ID_MOTIF_FIN_CONTRAT", root.findTable("MOTIF_FIN_CONTRAT"));
            createTable.addDateAndTimeColumn("DATE_FIN");
            createTable.addDateAndTimeColumn("DATE_NOTIFICATION");
            createTable.addDateAndTimeColumn("DATE_SIGNATURE_CONVENTION");
            createTable.addDateAndTimeColumn("DATE_ENGAGEMENT_PROCEDURE");
            createTable.addDateAndTimeColumn("DERNIER_JOUR_TRAV_PAYE");
            createTable.addBooleanColumn("TRANSACTION_EN_COURS", Boolean.FALSE, false);
            createTable.addBooleanColumn("PORTABILITE_PREVOYANCE", Boolean.FALSE, false);
            createTable.addIntegerColumn("NB_DIF_RESTANT", null, true);
            createTable.addIntegerColumn("NB_MOIS_CSP", null, true);
            createTable.addDecimalColumn("SALAIRE_NET_HORAIRE", 16, 8, null, true);
            createTable.addDecimalColumn("INDEMNITE_VERSE", 16, 8, null, true);
            createTable.addForeignColumn("ID_TYPE_PREAVIS", root.findTable("TYPE_PREAVIS"));
            createTable.addDateAndTimeColumn("DATE_DEBUT_PREAVIS");
            createTable.addDateAndTimeColumn("DATE_FIN_PREAVIS");
            createTable.addVarCharColumn("INFOS", 2048);

            try {
                root.getBase().getDataSource().execute(createTable.asString());
                insertUndef(createTable);
                root.refetchTable("FIN_CONTRAT");
                root.getSchema().updateVersion();
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création de la table " + "FIN_CONTRAT", ex);
            }
        }

        if (!root.contains("REPRISE_TRAVAIL")) {

            final SQLCreateTable createTable = new SQLCreateTable(root, "REPRISE_TRAVAIL");
            createTable.addForeignColumn("SALARIE");
            createTable.addDateAndTimeColumn("DATE_DERNIER_JOUR_TRAV");
            createTable.addDateAndTimeColumn("DATE_FIN_PREV");
            createTable.addBooleanColumn("SUBROGATION", Boolean.FALSE, false);
            createTable.addDateAndTimeColumn("DATE_DEBUT_SUBROGATION");
            createTable.addDateAndTimeColumn("DATE_FIN_SUBROGATION");
            createTable.addForeignColumn("ID_MOTIF_ARRET_TRAVAIL", root.findTable("MOTIF_ARRET_TRAVAIL"));
            createTable.addForeignColumn("ID_MOTIF_REPRISE_ARRET_TRAVAIL", root.findTable("MOTIF_REPRISE_ARRET_TRAVAIL"));
            createTable.addDateAndTimeColumn("DATE_REPRISE");
            createTable.addDateAndTimeColumn("DATE_ACCIDENT");
            createTable.addDateAndTimeColumn("DATE");
            createTable.addVarCharColumn("COMMENTAIRES", 2048);

            try {
                root.getBase().getDataSource().execute(createTable.asString());
                insertUndef(createTable);
                root.refetchTable("REPRISE_TRAVAIL");
                root.getSchema().updateVersion();
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création de la table " + "REPRISE_TRAVAIL", ex);
            }
        }

        if (!root.contains("DECLARATION_DSN")) {

            final SQLCreateTable createTable = new SQLCreateTable(root, "DECLARATION_DSN");
            createTable.addForeignColumn("ID_DSN_NATURE", root.findTable("DSN_NATURE"));

            createTable.addDateAndTimeColumn("DATE");
            createTable.addDateAndTimeColumn("DATE_ENVOI");
            createTable.addBooleanColumn("TEST", Boolean.FALSE, false);
            createTable.addBooleanColumn("ENVOYE", Boolean.FALSE, false);
            createTable.addVarCharColumn("COMMENTAIRE", 1024);
            createTable.addVarCharColumn("DSN_FILE", 75000);
            createTable.addIntegerColumn("NUMERO", 1);
            createTable.addIntegerColumn("ANNEE", 2016);
            createTable.addForeignColumn("MOIS");

            try {
                root.getBase().getDataSource().execute(createTable.asString());
                insertUndef(createTable);
                root.refetchTable("DECLARATION_DSN");
                root.getSchema().updateVersion();
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création de la table " + "DECLARATION_DSN", ex);
            }
        }

        SQLTable tableArret = root.getTable("ARRET_TRAVAIL");
        if (!tableArret.contains("DATE")) {
            AlterTable alter = new AlterTable(tableArret);
            alter.addDateAndTimeColumn("DATE");
            alter.addVarCharColumn("COMMENTAIRES", 2048);
            root.getBase().getDataSource().execute(alter.asString());
            root.refetchTable("ARRET_TRAVAIL");
            root.getSchema().updateVersion();
        }

        SQLTable tableDsn = root.getTable("DECLARATION_DSN");
        if (!tableDsn.contains("ID_ARRET_TRAVAIL")) {
            AlterTable alter = new AlterTable(tableDsn);
            alter.addForeignColumn("ID_ARRET_TRAVAIL", root.findTable("ARRET_TRAVAIL"));
            root.getBase().getDataSource().execute(alter.asString());
            root.getSchema().updateVersion();
        }
        if (!tableDsn.contains("ID_FIN_CONTRAT")) {
            AlterTable alter = new AlterTable(tableDsn);
            alter.addForeignColumn("ID_FIN_CONTRAT", root.findTable("FIN_CONTRAT"));
            root.getBase().getDataSource().execute(alter.asString());
            root.getSchema().updateVersion();
        }

        if (!tableDsn.contains("ID_REPRISE_TRAVAIL")) {
            AlterTable alter = new AlterTable(tableDsn);
            alter.addForeignColumn("ID_REPRISE_TRAVAIL", root.findTable("REPRISE_TRAVAIL"));
            root.getBase().getDataSource().execute(alter.asString());
            root.getSchema().updateVersion();
        }
        if (!tableDsn.contains("ID_MOIS_REGUL")) {
            AlterTable alter = new AlterTable(tableDsn);
            alter.addForeignColumn("ID_MOIS_REGUL", root.findTable("MOIS"));
            root.getBase().getDataSource().execute(alter.asString());
            root.getSchema().updateVersion();
        }

        if (!tableDsn.contains("ANNULE_REMPLACE")) {
            AlterTable alter = new AlterTable(tableDsn);
            alter.addBooleanColumn("ANNULE_REMPLACE", Boolean.FALSE, false);
            root.getBase().getDataSource().execute(alter.asString());
            root.getSchema().updateVersion();
        }
        if (!tableDsn.contains("NUMERO_REFERENCE")) {
            AlterTable alter = new AlterTable(tableDsn);
            alter.addVarCharColumn("NUMERO_REFERENCE", 256);
            root.getBase().getDataSource().execute(alter.asString());
            root.getSchema().updateVersion();
        }
        if (!tableDsn.contains("ID_DECLARATION_DSN_ANNULATION")) {
            AlterTable alter = new AlterTable(tableDsn);
            alter.addForeignColumn("ID_DECLARATION_DSN_ANNULATION", tableDsn);
            root.getBase().getDataSource().execute(alter.asString());
            root.getSchema().updateVersion();
        }
        if (!tableDsn.contains("EFFECTIF_CVAE")) {
            AlterTable alter = new AlterTable(tableDsn);
            alter.addIntegerColumn("EFFECTIF_CVAE", null, true);
            alter.addVarCharColumn("CODE_INSEE_CVAE", 32);
            root.getBase().getDataSource().execute(alter.asString());
            root.getSchema().updateVersion();
            root.refetchTable(tableDsn.getName());
        }

        if (!tableDsn.contains("PERIODE_CVAE_DEBUT")) {
            AlterTable alter = new AlterTable(tableDsn);
            alter.addColumn("PERIODE_CVAE_DEBUT", "date");
            alter.addColumn("PERIODE_CVAE_FIN", "date");
            root.getBase().getDataSource().execute(alter.asString());
            root.getSchema().updateVersion();
        }

        // if (!root.contains("FIN_CONTRAT")) {
        //
        // final SQLCreateTable createTable = new SQLCreateTable(root, "FIN_CONTRAT");
        // createTable.addForeignColumn("SALARIE");
        // createTable.addDateAndTimeColumn("DATE_FIN");
        // createTable.addDateAndTimeColumn("DATE_FIN_PREV");
        // createTable.addBooleanColumn("SUBROGATION", Boolean.FALSE, false);
        // createTable.addDateAndTimeColumn("DATE_DEBUT_SUBROGATION");
        // createTable.addDateAndTimeColumn("DATE_FIN_SUBROGATION");
        // createTable.addForeignColumn("ID_MOTIF_ARRET_TRAVAIL",
        // root.findTable("MOTIF_ARRET_TRAVAIL"));
        // createTable.addForeignColumn("ID_MOTIF_REPRISE_ARRET_TRAVAIL",
        // root.findTable("MOTIF_REPRISE_ARRET_TRAVAIL"));
        // createTable.addDateAndTimeColumn("DATE_REPRISE");
        // createTable.addDateAndTimeColumn("DATE_ACCIDENT");
        //
        // try {
        // root.getBase().getDataSource().execute(createTable.asString());
        // insertUndef(createTable);
        // root.refetchTable("FIN_CONTRAT");
        // root.getSchema().updateVersion();
        // } catch (SQLException ex) {
        // throw new IllegalStateException("Erreur lors de la création de la table " +
        // "FIN_CONTRAT", ex);
        // }
        // }

        SQLTable tableContrat = root.getTable("CONTRAT_SALARIE");
        if (!tableContrat.contains("NUMERO")) {
            AlterTable alter = new AlterTable(tableContrat);
            alter.addColumn("NUMERO", "varchar(" + 128 + ") default '00000' NOT NULL");
            root.getBase().getDataSource().execute(alter.asString());
            root.getSchema().updateVersion();
        }

        if (!tableContrat.contains("CODE_REGIME_RETRAITE_DSN")) {
            AlterTable alter = new AlterTable(tableContrat);
            alter.addColumn("CODE_REGIME_RETRAITE_DSN", "varchar(" + 128 + ") default '00000' NOT NULL");
            root.getBase().getDataSource().execute(alter.asString());
            root.getSchema().updateVersion();
        }

        if (!tableContrat.contains("DATE_PREV_FIN")) {
            AlterTable alter = new AlterTable(tableContrat);
            alter.addDateAndTimeColumn("DATE_PREV_FIN");
            root.getBase().getDataSource().execute(alter.asString());
            root.getSchema().updateVersion();
        }

        boolean updateContrat = false;
        if (!tableContrat.contains("ID_CONTRAT_MODALITE_TEMPS")) {
            updateContrat = true;
            AlterTable alter = new AlterTable(tableContrat);
            alter.addForeignColumn("ID_CONTRAT_MODALITE_TEMPS", root.findTable("CONTRAT_MODALITE_TEMPS"));
            root.getBase().getDataSource().execute(alter.asString());
            root.getSchema().updateVersion();
        }
        if (!tableContrat.contains("ID_CONTRAT_REGIME_MALADIE")) {
            AlterTable alter = new AlterTable(tableContrat);
            alter.addForeignColumn("ID_CONTRAT_REGIME_MALADIE", root.findTable("CONTRAT_REGIME_MALADIE"));
            root.getBase().getDataSource().execute(alter.asString());
            root.getSchema().updateVersion();
        }
        if (!tableContrat.contains("ID_CONTRAT_REGIME_VIEILLESSE")) {
            AlterTable alter = new AlterTable(tableContrat);
            alter.addForeignColumn("ID_CONTRAT_REGIME_VIEILLESSE", root.findTable("CONTRAT_REGIME_VIEILLESSE"));
            root.getBase().getDataSource().execute(alter.asString());
            root.getSchema().updateVersion();
        }
        if (!tableContrat.contains("ID_CONTRAT_MOTIF_RECOURS")) {
            AlterTable alter = new AlterTable(tableContrat);
            alter.addForeignColumn("ID_CONTRAT_MOTIF_RECOURS", root.findTable("CONTRAT_MOTIF_RECOURS"));
            root.getBase().getDataSource().execute(alter.asString());
            root.getSchema().updateVersion();
        }
        if (!tableContrat.contains("ID_CONTRAT_DETACHE_EXPATRIE")) {
            AlterTable alter = new AlterTable(tableContrat);
            alter.addForeignColumn("ID_CONTRAT_DETACHE_EXPATRIE", root.findTable("CONTRAT_DETACHE_EXPATRIE"));
            root.getBase().getDataSource().execute(alter.asString());
            root.getSchema().updateVersion();
        }
        if (!tableContrat.contains("ID_CONTRAT_DISPOSITIF_POLITIQUE")) {
            AlterTable alter = new AlterTable(tableContrat);
            alter.addForeignColumn("ID_CONTRAT_DISPOSITIF_POLITIQUE", root.findTable("CONTRAT_DISPOSITIF_POLITIQUE"));
            root.getBase().getDataSource().execute(alter.asString());
            root.getSchema().updateVersion();
        }

        if (updateContrat) {
            root.refetchTable("CONTRAT_SALARIE");
            tableContrat = root.findTable("CONTRAT_SALARIE");
            SQLSelect sel = new SQLSelect();
            sel.addSelectStar(tableContrat);
            List<SQLRow> contrats = SQLRowListRSH.execute(sel);

            SQLSelect selModal = new SQLSelect();
            final SQLTable tableModaliteTemps = root.findTable("CONTRAT_MODALITE_TEMPS");
            selModal.addSelectStar(tableModaliteTemps);
            selModal.setWhere(new Where(tableModaliteTemps.getField("CODE"), "=", "10"));
            SQLRow contratModalite = SQLRowListRSH.execute(selModal).get(0);

            SQLSelect selMaladie = new SQLSelect();
            final SQLTable tableMaladie = root.findTable("CONTRAT_REGIME_MALADIE");
            selMaladie.addSelectStar(tableMaladie);
            selMaladie.setWhere(new Where(tableMaladie.getField("CODE"), "=", "200"));
            SQLRow contratMaladie = SQLRowListRSH.execute(selMaladie).get(0);

            SQLSelect selViel = new SQLSelect();
            final SQLTable tableViel = root.findTable("CONTRAT_REGIME_VIEILLESSE");
            selViel.addSelectStar(tableViel);
            selViel.setWhere(new Where(tableViel.getField("CODE"), "=", "200"));
            SQLRow contratViel = SQLRowListRSH.execute(selViel).get(0);

            SQLSelect selDetacheExp = new SQLSelect();
            final SQLTable tableDetacheExp = root.findTable("CONTRAT_DETACHE_EXPATRIE");
            selDetacheExp.addSelectStar(tableDetacheExp);
            selDetacheExp.setWhere(new Where(tableDetacheExp.getField("CODE"), "=", "99"));
            SQLRow contratDetacheExp = SQLRowListRSH.execute(selDetacheExp).get(0);

            SQLSelect selDispoPolitique = new SQLSelect();
            final SQLTable tableDispoPol = root.findTable("CONTRAT_DISPOSITIF_POLITIQUE");
            selDispoPolitique.addSelectStar(tableDispoPol);
            selDispoPolitique.setWhere(new Where(tableDispoPol.getField("CODE"), "=", "99"));
            SQLRow contratDispoPol = SQLRowListRSH.execute(selDispoPolitique).get(0);

            for (SQLRow contrat : contrats) {

                final SQLRowValues createEmptyUpdateRow = contrat.createEmptyUpdateRow();
                createEmptyUpdateRow.put("ID_CONTRAT_MODALITE_TEMPS", contratModalite.getID());
                createEmptyUpdateRow.put("ID_CONTRAT_REGIME_MALADIE", contratMaladie.getID());
                createEmptyUpdateRow.put("ID_CONTRAT_REGIME_VIEILLESSE", contratViel.getID());
                createEmptyUpdateRow.put("ID_CONTRAT_DETACHE_EXPATRIE", contratDetacheExp.getID());
                createEmptyUpdateRow.put("ID_CONTRAT_DISPOSITIF_POLITIQUE", contratDispoPol.getID());
                createEmptyUpdateRow.commit();
            }
        }
        if (!root.contains("CONTRAT_PREVOYANCE")) {

            final SQLCreateTable createTable = new SQLCreateTable(root, "CONTRAT_PREVOYANCE");
            createTable.addVarCharColumn("REFERENCE", 256);
            createTable.addDateAndTimeColumn("DATE_DEBUT");
            createTable.addDateAndTimeColumn("DATE_FIN");
            createTable.addVarCharColumn("CODE_ORGANISME", 256);
            createTable.addVarCharColumn("CODE_DELEGATAIRE", 256);
            createTable.addVarCharColumn("CODE_UNIQUE", 256);
            createTable.addVarCharColumn("NOM", 256);
            try {
                root.getBase().getDataSource().execute(createTable.asString());
                insertUndef(createTable);
                root.refetchTable("CONTRAT_PREVOYANCE");
                root.getSchema().updateVersion();
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création de la table " + "CONTRAT_PREVOYANCE", ex);
            }
        }
        if (!root.getTable("CONTRAT_PREVOYANCE").contains("COTISATION_ETABLISSEMENT")) {
            AlterTable tableContratP = new AlterTable(root.getTable("CONTRAT_PREVOYANCE"));
            tableContratP.addBooleanColumn("COTISATION_ETABLISSEMENT", Boolean.FALSE, false);
            root.getBase().getDataSource().execute(tableContratP.asString());
            root.refetchTable("CONTRAT_PREVOYANCE");
            root.getSchema().updateVersion();
        }

        if (!root.contains("CONTRAT_PREVOYANCE_CONTRAT_SALARIE")) {

            final SQLCreateTable createTableSal = new SQLCreateTable(root, "CONTRAT_PREVOYANCE_CONTRAT_SALARIE");
            final SQLTable tableInfosSalarie = root.getTable("INFOS_SALARIE_PAYE");
            createTableSal.addForeignColumn("ID_INFOS_SALARIE_PAYE", tableInfosSalarie);
            createTableSal.addForeignColumn("ID_CONTRAT_PREVOYANCE", root.getTable("CONTRAT_PREVOYANCE"));
            createTableSal.addVarCharColumn("CODE_OPTION", 256);
            createTableSal.addVarCharColumn("CODE_POPULATION", 256);
            createTableSal.addIntegerColumn("NB_ENFANT_CHARGE", null, true);
            createTableSal.addIntegerColumn("NB_ADULTE_AYANT_DROIT", null, true);
            createTableSal.addIntegerColumn("NB_AYANT_DROIT", null, true);
            createTableSal.addIntegerColumn("NB_AYANT_DROIT_AUTRE", null, true);
            createTableSal.addIntegerColumn("NB_ENFANT_AYANT_DROIT", null, true);

            try {
                root.getBase().getDataSource().execute(createTableSal.asString());
                insertUndef(createTableSal);
                root.refetchTable("CONTRAT_PREVOYANCE_CONTRAT_SALARIE");
                root.getSchema().updateVersion();

                String up = "UPDATE " + new SQLName(root.getName(), tableInfosSalarie.getName()).quote() + " i SET \"ID_SALARIE\" = (SELECT s.\"ID\" FROM "
                        + new SQLName(root.getName(), tableInfosSalarie.getForeignTable("ID_SALARIE").getName()).quote() + " s WHERE s.\"ID_INFOS_SALARIE_PAYE\"=i.\"ID\" and s.\"ARCHIVE\"=0)"
                        + " WHERE i.\"ID\" IN (SELECT i2.\"ID_INFOS_SALARIE_PAYE\" FROM " + new SQLName(root.getName(), tableInfosSalarie.getForeignTable("ID_SALARIE").getName()).quote();
                up += " i2 WHERE i2.\"ARCHIVE\"=0)";

                root.getBase().getDataSource().execute(up);

            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création de la table " + "CONTRAT_PREVOYANCE_SALARIE", ex);
            }
        }
        if (!root.contains("CONTRAT_PREVOYANCE_RUBRIQUE"))

        {

            final SQLCreateTable createTableRub = new SQLCreateTable(root, "CONTRAT_PREVOYANCE_RUBRIQUE");
            createTableRub.addForeignColumn("ID_RUBRIQUE_COTISATION", root.findTable("RUBRIQUE_COTISATION"));
            createTableRub.addForeignColumn("ID_CONTRAT_PREVOYANCE", root.getTable("CONTRAT_PREVOYANCE"));

            try {
                root.getBase().getDataSource().execute(createTableRub.asString());
                insertUndef(createTableRub);
                root.refetchTable("CONTRAT_PREVOYANCE_RUBRIQUE");
                root.getSchema().updateVersion();
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création de la table " + "CONTRAT_PREVOYANCE_RUBRIQUE", ex);
            }
        }

        if (!root.contains("CONTRAT_PREVOYANCE_RUBRIQUE_NET"))

        {

            final SQLCreateTable createTableRub = new SQLCreateTable(root, "CONTRAT_PREVOYANCE_RUBRIQUE_NET");
            createTableRub.addForeignColumn("ID_RUBRIQUE_NET", root.findTable("RUBRIQUE_NET"));
            createTableRub.addForeignColumn("ID_CONTRAT_PREVOYANCE", root.getTable("CONTRAT_PREVOYANCE"));

            try {
                root.getBase().getDataSource().execute(createTableRub.asString());
                insertUndef(createTableRub);
                root.refetchTable("CONTRAT_PREVOYANCE_RUBRIQUE_NET");
                root.getSchema().updateVersion();
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création de la table " + "CONTRAT_PREVOYANCE_RUBRIQUE_NET", ex);
            }
        }
        if (!root.contains("AYANT_DROIT")) {

            final SQLCreateTable createTableAyantDroit = new SQLCreateTable(root, "AYANT_DROIT");
            createTableAyantDroit.addVarCharColumn("NOM", 256);
            createTableAyantDroit.addForeignColumn("ID_SALARIE", root.findTable("SALARIE"));
            createTableAyantDroit.addForeignColumn("ID_AYANT_DROIT_TYPE", root.findTable("AYANT_DROIT_TYPE"));
            createTableAyantDroit.addBooleanColumn("REGIME_ALSACE", Boolean.FALSE, false);
            createTableAyantDroit.addVarCharColumn("NIR", 256);
            createTableAyantDroit.addVarCharColumn("PRENOMS", 256);
            createTableAyantDroit.addVarCharColumn("CODE_ORGANISME_AFFILIATION", 256);
            createTableAyantDroit.addVarCharColumn("CODE_OPTION", 256);
            createTableAyantDroit.addVarCharColumn("NIR_OUVRANT_DROIT", 256);
            createTableAyantDroit.addDateAndTimeColumn("DATE_DEBUT_RATTACHEMENT");
            createTableAyantDroit.addDateAndTimeColumn("DATE_FIN_RATTACHEMENT");
            createTableAyantDroit.addDateAndTimeColumn("DATE_NAISSANCE");
            try {
                root.getBase().getDataSource().execute(createTableAyantDroit.asString());
                insertUndef(createTableAyantDroit);
                root.refetchTable("AYANT_DROIT");
                root.getSchema().updateVersion();
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création de la table " + "AYANT_DROIT", ex);
            }

            final SQLCreateTable createTablePrevAyant = new SQLCreateTable(root, "CONTRAT_PREVOYANCE_AYANT_DROIT");
            createTablePrevAyant.addForeignColumn("ID_AYANT_DROIT", root.getTable("AYANT_DROIT"));
            createTablePrevAyant.addForeignColumn("ID_CONTRAT_PREVOYANCE", root.getTable("CONTRAT_PREVOYANCE"));

            try {
                root.getBase().getDataSource().execute(createTablePrevAyant.asString());
                insertUndef(createTablePrevAyant);
                root.refetchTable("CONTRAT_PREVOYANCE_AYANT_DROIT");
                root.getSchema().updateVersion();
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création de la table " + "CONTRAT_PREVOYANCE_AYANT_DROIT", ex);
            }
        }
        if (!root.contains("CODE_PENIBILITE_CONTRAT_SALARIE")) {

            final SQLCreateTable createTableSal = new SQLCreateTable(root, "CODE_PENIBILITE_CONTRAT_SALARIE");
            final SQLTable tableInfosSalarie = root.getTable("INFOS_SALARIE_PAYE");
            createTableSal.addForeignColumn("ID_INFOS_SALARIE_PAYE", tableInfosSalarie);
            createTableSal.addForeignColumn("ID_CODE_PENIBILITE", root.findTable("CODE_PENIBILITE"));

            try {
                root.getBase().getDataSource().execute(createTableSal.asString());
                insertUndef(createTableSal);
                root.refetchTable("CODE_PENIBILITE_CONTRAT_SALARIE");
                root.getSchema().updateVersion();

            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création de la table " + "CODE_PENIBILITE_CONTRAT_SALARIE", ex);
            }
        }
    }

}
