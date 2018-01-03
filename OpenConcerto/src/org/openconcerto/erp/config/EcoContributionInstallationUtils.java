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
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSyntax;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.request.Inserter;
import org.openconcerto.sql.request.Inserter.Insertion;
import org.openconcerto.sql.utils.AlterTable;
import org.openconcerto.sql.utils.ReOrder;
import org.openconcerto.sql.utils.SQLCreateTable;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EcoContributionInstallationUtils {

    class EcoContribution {
        final private String code, libelle, famille;
        final private BigDecimal taux;

        public EcoContribution(String code, String libelle, String famille, double taux) {
            this.code = code;
            this.libelle = libelle;
            this.famille = famille;
            this.taux = new BigDecimal(taux);
        }

        public String getCode() {
            return code;
        }

        public String getFamille() {
            return famille;
        }

        public String getLibelle() {
            return libelle;
        }

        public BigDecimal getTaux() {
            return taux;
        }
    }

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

    private void insertValues(List<EcoContribution> values, SQLTable table) throws SQLException {
        for (EcoContribution eco : values) {
            SQLRowValues rowVals = new SQLRowValues(table);
            rowVals.put("CODE", eco.getCode());
            rowVals.put("NOM", eco.getLibelle());
            rowVals.put("ID_FAMILLE_ECO_CONTRIBUTION", getFamille(eco.getFamille(), table));
            rowVals.put("TAUX", eco.getTaux());
            rowVals.commit();
        }
    }

    Map<Object, SQLRowValues> familleMap = new HashMap<Object, SQLRowValues>();

    private Object getFamille(String famille, SQLTable table) {
        if (familleMap.containsKey(famille)) {
            return familleMap.get(famille);
        } else {
            SQLRowValues rowValues = new SQLRowValues(table.getTable("FAMILLE_ECO_CONTRIBUTION"));
            rowValues.put("NOM", famille);
            familleMap.put(famille, rowValues);
            return rowValues;
        }
    }

    public void updateEcoCommonTable(final DBRoot root) throws SQLException {

        if (!root.contains("ECO_CONTRIBUTION")) {

            final SQLCreateTable createTableFamille = new SQLCreateTable(root, "FAMILLE_ECO_CONTRIBUTION");
            createTableFamille.addVarCharColumn("NOM", 512);

            root.getBase().getDataSource().execute(createTableFamille.asString());
            insertUndef(createTableFamille);
            root.refetchTable("FAMILLE_ECO_CONTRIBUTION");
            root.getSchema().updateVersion();

            final SQLCreateTable createTableEco = new SQLCreateTable(root, "ECO_CONTRIBUTION");
            createTableEco.addVarCharColumn("CODE", 256);
            createTableEco.addVarCharColumn("NOM", 512);
            createTableEco.addForeignColumn("ID_FAMILLE_ECO_CONTRIBUTION", root.getTable("FAMILLE_ECO_CONTRIBUTION"));
            createTableEco.addDecimalColumn("TAUX", 16, 2, BigDecimal.ZERO, false);

            root.getBase().getDataSource().execute(createTableEco.asString());
            insertUndef(createTableEco);
            root.refetchTable("ECO_CONTRIBUTION");
            root.getSchema().updateVersion();

            // List<EcoContribution> ecos = new
            // ArrayList<EcoContributionInstallationUtils.EcoContribution>();
            // ecos.add(new EcoContribution("Ecologic E 1", "Ecrans <= 9kg", "Ecrans", 1));
            // ecos.add(new EcoContribution("Ecologic E 2", "Ecrans >= 9.01kg et <=18kg", "Ecrans",
            // 2));
            // ecos.add(new EcoContribution("Ecologic E 3", "Ecrans >= 18.01kg et <=36kg", "Ecrans",
            // 4));
            // ecos.add(new EcoContribution("Ecologic E 4", "Ecrans >= 36.01kg ", "Ecrans", 8));
            // ecos.add(new EcoContribution("Ecologic F 1", "GEM F <= 40kg", "GEM FROID", 6));
            // ecos.add(new EcoContribution("Ecologic F 2", "GEM F >= 40.01kg", "GEM FROID", 12.5));
            // ecos.add(new EcoContribution("Ecologic HF 1", "GEM HF <= 6kg", "GEM HORS FROID",
            // 0.5));
            // ecos.add(new EcoContribution("Ecologic HF 2", "GEM HF >= 6.01kg et <=12kg", "GEM HORS
            // FROID", 1));
            // ecos.add(new EcoContribution("Ecologic HF 3", "GEM HF >= 12.01kg et <=24kg", "GEM
            // HORS FROID", 2));
            // ecos.add(new EcoContribution("Ecologic HF 4", "GEM HF >= 24.01kg", "GEM HORS FROID",
            // 6));
            // ecos.add(new EcoContribution("Ecologic P 1", "PAM <= 0.20kg", "PAM", 0.01));
            // ecos.add(new EcoContribution("Ecologic P 2", "PAM >=0.21kg et <=0.50kg", "PAM",
            // 0.03));
            // ecos.add(new EcoContribution("Ecologic P 3", "PAM >=0.51kg et <=1.00kg", "PAM",
            // 0.05));
            // ecos.add(new EcoContribution("Ecologic P 4", "PAM >=1.01kg et <=2.50kg", "PAM",
            // 0.15));
            // ecos.add(new EcoContribution("Ecologic P 5", "PAM >=2.51kg et <=4.00kg", "PAM",
            // 0.25));
            // ecos.add(new EcoContribution("Ecologic P 6", "PAM >=4.01kg et <=8.00kg", "PAM",
            // 0.5));
            // ecos.add(new EcoContribution("Ecologic P 7", "PAM >=8.01kg et <=12.00kg", "PAM",
            // 0.75));
            // ecos.add(new EcoContribution("Ecologic P 8", "PAM >=12.01kg et <=20.00kg", "PAM",
            // 1.25));
            // ecos.add(new EcoContribution("Ecologic P 9", "PAM >= 20.01", "PAM", 2.25));
            // ecos.add(new EcoContribution("Ecosys 1.1", "Gros appareils ménagers 1", "Gros
            // appareils ménagers", 13));
            // ecos.add(new EcoContribution("Ecosys 1.10", "Gros appareils ménagers 10", "Gros
            // appareils ménagers", 0.1));
            // ecos.add(new EcoContribution("Ecosys 1.2", "Gros appareils ménagers 2", "Gros
            // appareils ménagers", 6));
            // ecos.add(new EcoContribution("Ecosys 1.3", "Gros appareils ménagers 3", "Gros
            // appareils ménagers", 2));
            // ecos.add(new EcoContribution("Ecosys 1.4", "Gros appareils ménagers 4", "Gros
            // appareils ménagers", 1));
            // ecos.add(new EcoContribution("Ecosys 1.5", "Gros appareils ménagers 5", "Gros
            // appareils ménagers", 0.5));
            // ecos.add(new EcoContribution("Ecosys 1.6", "Gros appareils ménagers 6", "Gros
            // appareils ménagers", 0.1));
            // ecos.add(new EcoContribution("Ecosys 1.7", "Gros appareils ménagers 7", "Gros
            // appareils ménagers", 4));
            // ecos.add(new EcoContribution("Ecosys 1.8", "Gros appareils ménagers 8", "Gros
            // appareils ménagers", 1));
            // ecos.add(new EcoContribution("Ecosys 1.9", "Gros appareils ménagers 9", "Gros
            // appareils ménagers", 0.5));
            // ecos.add(new EcoContribution("Ecosys 2.1", "Petits appareils ménagers 1", "Petits
            // appareils ménagers", 1));
            // ecos.add(new EcoContribution("Ecosys 2.2", "Petits appareils ménagers 2", "Petits
            // appareils ménagers", 0.5));
            // ecos.add(new EcoContribution("Ecosys 2.3", "Petits appareils ménagers 3", "Petits
            // appareils ménagers", 0.1));
            // ecos.add(new EcoContribution("Ecosys 3.1", "Equipements informatiques et
            // télécommunications 1", "Equipement informat. et de télécom.", 8));
            // ecos.add(new EcoContribution("Ecosys 3.2", "Equipements informatiques et
            // télécommunications 2", "Equipement informat. et de télécom.", 4));
            // ecos.add(new EcoContribution("Ecosys 3.3", "Equipements informatiques et
            // télécommunications 3", "Equipement informat. et de télécom.", 1));
            // ecos.add(new EcoContribution("Ecosys 3.4", "Equipements informatiques et
            // télécommunications 4", "Equipement informat. et de télécom.", 1));
            // ecos.add(new EcoContribution("Ecosys 3.5", "Equipements informatiques et
            // télécommunications 5", "Equipement informat. et de télécom.", 0.3));
            // ecos.add(new EcoContribution("Ecosys 3.6", "Equipements informatiques et
            // télécommunications 6", "Equipement informat. et de télécom.", 0.5));
            // ecos.add(new EcoContribution("Ecosys 3.7", "Equipements informatiques et
            // télécommunications 7", "Equipement informat. et de télécom.", 0.1));
            // ecos.add(new EcoContribution("Ecosys 3.8", "Equipements informatiques et
            // télécommunications 8", "Equipement informat. et de télécom.", 0.01));
            // ecos.add(new EcoContribution("Ecosys 4.1", "Matériel grand public 1", "Matériel grand
            // public ", 8));
            // ecos.add(new EcoContribution("Ecosys 4.2", "Matériel grand public 2", "Matériel grand
            // public ", 4));
            // ecos.add(new EcoContribution("Ecosys 4.3", "Matériel grand public 3", "Matériel grand
            // public ", 1));
            // ecos.add(new EcoContribution("Ecosys 4.4", "Matériel grand public 4", "Matériel grand
            // public ", 1));
            // ecos.add(new EcoContribution("Ecosys 4.5", "Matériel grand public 5", "Matériel grand
            // public ", 0.3));
            // ecos.add(new EcoContribution("Ecosys 4.6", "Matériel grand public 6", "Matériel grand
            // public ", 0.1));
            // ecos.add(new EcoContribution("Ecosys 6.1", "Outils électriques et électroniques 1",
            // "Outils électriques et électroniques", 0.2));
            // ecos.add(new EcoContribution("Ecosys 6.2", "Outils électriques et électroniques 2",
            // "Outils électriques et électroniques", 1.5));
            // ecos.add(new EcoContribution("Ecosys 7.1", "Jouets. équipements de loisir et de sport
            // 1", "Jouet. équip. de loisir et de sport", 0.05));
            // ecos.add(new EcoContribution("Ecosys 7.2", "Jouets. équipements de loisir et de sport
            // 2", "Jouet. équip. de loisir et de sport", 0.2));
            // ecos.add(new EcoContribution("Ecosys 7.3", "Jouets. équipements de loisir et de sport
            // 3", "Jouet. équip. de loisir et de sport", 1.5));
            // ecos.add(new EcoContribution("Ecosys 8.1", "Dispositifs médicaux 1", "Dispositifs
            // médicaux ", 1));
            // ecos.add(new EcoContribution("Ecosys 8.2", "Dispositifs médicaux 2", "Dispositifs
            // médicaux ", 0.1));
            // ecos.add(new EcoContribution("Ecosys 9.1", "Instruments de contrôle et de
            // surveillance 1", "Instrument contrôle et surveillance", 0.1));
            // ecos.add(new EcoContribution("Ecosys 9.2", "Instruments de contrôle et de
            // surveillance 2", "Instrument contrôle et surveillance", 1));
            // ecos.add(new EcoContribution("ERP E 1", "Ecrans < 9kg", "Ecrans", 1));
            // ecos.add(new EcoContribution("ERP E 2", "Ecrans >= 9kg et < 15kg", "Ecrans", 2));
            // ecos.add(new EcoContribution("ERP E 3", "Ecrans >= 15kg et < 30kg", "Ecrans", 4));
            // ecos.add(new EcoContribution("ERP E 4", "Ecrans >= 30kg ", "Ecrans", 8));
            // ecos.add(new EcoContribution("ERP GEM F 1", "Froid", "GEM FROID", 13));
            // ecos.add(new EcoContribution("ERP GEM HF 1", "GEM < 20 kg", "GEM HORS FROID", 2));
            // ecos.add(new EcoContribution("ERP GEM HF 2", "GEM >= 20 kg ", "GEM HORS FROID", 6));
            // ecos.add(new EcoContribution("ERP P 1", "PAM < 0.20kg", "PAM", 0.01));
            // ecos.add(new EcoContribution("ERP P 10", "PAM >=30 kg", "PAM", 4));
            // ecos.add(new EcoContribution("ERP P 2", "PAM >=0.2 kg et < 0.5 kg", "PAM", 0.03));
            // ecos.add(new EcoContribution("ERP P 3", "PAM >=0.5kg et < 1.00kg", "PAM", 0.05));
            // ecos.add(new EcoContribution("ERP P 4", "PAM >=1 kg et < 2 kg", "PAM", 0.15));
            // ecos.add(new EcoContribution("ERP P 5", "PAM >=2 kg et < 4 kg", "PAM", 0.25));
            // ecos.add(new EcoContribution("ERP P 6", "PAM >=4 kg et < 8 kg", "PAM", 0.5));
            // ecos.add(new EcoContribution("ERP P 7", "PAM >=8 kg et < 15 kg", "PAM", 1));
            // ecos.add(new EcoContribution("ERP P 8", "PAM >=15 kg et < 20 kg", "PAM", 1.5));
            // ecos.add(new EcoContribution("ERP P 9", "PAM >=20 kg et < 30 kg", "PAM", 2.25));
            // ecos.add(new EcoContribution("Récylum 1 ", "Lampes relevant du décret N° 2005-829 du
            // 20/07/05", "Lampes", 0.3));
            //
            // insertValues(ecos, root.getTable("ECO_CONTRIBUTION"));
        }
    }

    public void updateEco(final DBRoot root) throws SQLException {

        List<String> tables = Arrays.asList("AVOIR_CLIENT", "DEVIS", "COMMANDE_CLIENT", "BON_DE_LIVRAISON", "SAISIE_VENTE_FACTURE", "COMMANDE", "BON_RECEPTION", "FACTURE_FOURNISSEUR");

        final SQLTable tableArt = root.getTable("ARTICLE");

        if (!tableArt.contains("ID_ECO_CONTRIBUTION")) {
            AlterTable alter = new AlterTable(tableArt);
            alter.addForeignColumn("ID_ECO_CONTRIBUTION", root.findTable("ECO_CONTRIBUTION"));
            root.getBase().getDataSource().execute(alter.asString());
            root.refetchTable("ARTICLE");
            root.getSchema().updateVersion();

            for (String tableName : tables) {
                AlterTable alterTable = new AlterTable(root.getTable(tableName));
                alterTable.addLongColumn("T_ECO_CONTRIBUTION", 0L, true);
                root.getBase().getDataSource().execute(alterTable.asString());
                root.refetchTable(tableName);
                root.getSchema().updateVersion();
            }

            for (String tableName : tables) {
                AlterTable alterTable = new AlterTable(root.getTable(tableName + "_ELEMENT"));
                alterTable.addForeignColumn("ID_ECO_CONTRIBUTION", root.findTable("ECO_CONTRIBUTION"));
                alterTable.addDecimalColumn("ECO_CONTRIBUTION", 16, 2, BigDecimal.ZERO, true);
                alterTable.addDecimalColumn("T_ECO_CONTRIBUTION", 16, 2, BigDecimal.ZERO, true);
                root.getBase().getDataSource().execute(alterTable.asString());
                root.refetchTable(tableName + "_ELEMENT");
                root.getSchema().updateVersion();
            }
        }

        // Recheck --> Avoir omis dans 1.5b1
        for (String tableName : tables) {
            final SQLTable table = root.getTable(tableName);
            if (!table.contains("T_ECO_CONTRIBUTION")) {
                AlterTable alterTable = new AlterTable(table);
                alterTable.addLongColumn("T_ECO_CONTRIBUTION", 0L, true);
                root.getBase().getDataSource().execute(alterTable.asString());
                root.refetchTable(tableName);
                root.getSchema().updateVersion();
            }
        }

        for (String tableName : tables) {
            final SQLTable table = root.getTable(tableName + "_ELEMENT");
            if (!table.contains("T_ECO_CONTRIBUTION")) {
                AlterTable alterTable = new AlterTable(table);
                alterTable.addForeignColumn("ID_ECO_CONTRIBUTION", root.findTable("ECO_CONTRIBUTION"));
                alterTable.addDecimalColumn("ECO_CONTRIBUTION", 16, 2, BigDecimal.ZERO, true);
                alterTable.addDecimalColumn("T_ECO_CONTRIBUTION", 16, 2, BigDecimal.ZERO, true);
                root.getBase().getDataSource().execute(alterTable.asString());
                root.refetchTable(tableName + "_ELEMENT");
                root.getSchema().updateVersion();
            }
        }

    }

}
