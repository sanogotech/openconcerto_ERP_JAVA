package org.openconcerto.modules.customerrelationship.lead.importer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openconcerto.erp.importer.DataImporter;
import org.openconcerto.openoffice.ContentType;
import org.openconcerto.openoffice.ODPackage;
import org.openconcerto.openoffice.spreadsheet.Cell;
import org.openconcerto.openoffice.spreadsheet.Sheet;
import org.openconcerto.openoffice.spreadsheet.SpreadSheet;
import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLRowValuesCluster;
import org.openconcerto.sql.model.SQLRowValuesListFetcher;
import org.openconcerto.sql.model.SQLRowValuesCluster.StoreMode;
import org.openconcerto.utils.cc.IdentityHashSet;
import org.openconcerto.utils.text.CSVReader;
import org.openconcerto.utils.text.CharsetHelper;

public class LeadImporter {

    abstract class CSVImporter<T extends LineCSV> {

        public Map<Object, T> importFrom(File csvFile) throws IOException {

            Map<Object, T> map = new HashMap<Object, T>();

            Charset cs = CharsetHelper.guessEncoding(csvFile, 4096, Charset.forName("Cp1252"));

            BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(csvFile), cs));
            String l = r.readLine();
            if (l == null) {
                r.close();
                return null;
            }
            char separator = ',';
            int cCount = 0;
            int scCount = 0;
            for (int i = 0; i < l.length(); i++) {
                char c = l.charAt(i);
                if (c == ',') {
                    cCount++;
                } else if (c == ';') {
                    scCount++;
                }
            }
            r.close();
            if (scCount > cCount) {
                separator = ';';
            }

            final CSVReader csvReader = new CSVReader(new InputStreamReader(new FileInputStream(csvFile), cs), separator);
            final List<String[]> lines = csvReader.readAll();
            final int rowCount = lines.size();
            int start = 0;

            for (int i = start; i < rowCount; i++) {
                final String[] values = lines.get(i);
                final T convertedLine = convert(values);
                map.put(convertedLine.getId(), convertedLine);
            }
            csvReader.close();
            return map;
        }

        public abstract T convert(String[] line);
    }

    public LeadCSV createLead(int i, Sheet sheet, int idAdr, int id) {
        final Cell<SpreadSheet> cell0 = sheet.getImmutableCellAt(0, i);
        final String societeName = cell0.getValue().toString().trim();
        final Cell<SpreadSheet> cell1 = sheet.getImmutableCellAt(1, i);
        final String loc = cell1.getValue().toString().trim();

        LeadCSV leadLine = new LeadCSV(id, societeName, loc);
        leadLine.setIdAdr(idAdr);

        final Cell<SpreadSheet> cell2 = sheet.getImmutableCellAt(2, i);
        final String siret = cell2.getValue().toString().trim();
        leadLine.setSiret(siret);

        final Cell<SpreadSheet> cell3 = sheet.getImmutableCellAt(3, i);
        final String ape = cell3.getValue().toString().trim();
        leadLine.setApe(ape);

        final Cell<SpreadSheet> cell4 = sheet.getImmutableCellAt(4, i);
        final String tel = cell4.getValue().toString().trim();
        leadLine.setPhone(tel);

        final Cell<SpreadSheet> cell5 = sheet.getImmutableCellAt(5, i);
        final String fax = cell5.getValue().toString().trim();
        leadLine.setFax(fax);

        final Cell<SpreadSheet> cell10 = sheet.getImmutableCellAt(10, i);
        final String secteur = cell10.getValue().toString().trim();
        leadLine.setSecteur(secteur);

        final Cell<SpreadSheet> cell11 = sheet.getImmutableCellAt(11, i);
        final String effectif = cell11.getValue().toString().trim();
        leadLine.setEffectif(effectif);

        final Cell<SpreadSheet> cell12 = sheet.getImmutableCellAt(12, i);
        final String origine = cell12.getValue().toString().trim();
        leadLine.setOrigine(origine);

        // final Cell<SpreadSheet> cell13 = sheet.getImmutableCellAt(13, i);
        // final String com = cell13.getValue().toString().trim();
        // leadLine.setCom(com);
        //
        final Cell<SpreadSheet> cell14 = sheet.getImmutableCellAt(14, i);
        final String statut = cell14.getValue().toString().trim();
        leadLine.setStatut(statut);

        final Cell<SpreadSheet> cell15 = sheet.getImmutableCellAt(15, i);
        final String prenom = cell15.getValue().toString().trim();
        leadLine.setPrenom(prenom);

        final Cell<SpreadSheet> cell16 = sheet.getImmutableCellAt(16, i);
        final String nom = cell16.getValue().toString().trim();
        leadLine.setNom(nom);

        final Cell<SpreadSheet> cell17 = sheet.getImmutableCellAt(17, i);
        final String civi = cell17.getValue().toString().trim();
        leadLine.setCivilite(civi);

        final Cell<SpreadSheet> cell18 = sheet.getImmutableCellAt(18, i);
        final String mail = cell18.getValue().toString().trim();
        leadLine.setMail(mail);

        final Cell<SpreadSheet> cell19 = sheet.getImmutableCellAt(19, i);
        final String cell = cell19.getValue().toString().trim();
        leadLine.setCell(cell);

        final Cell<SpreadSheet> cell20 = sheet.getImmutableCellAt(20, i);
        final String infos = cell20.getValue().toString().trim();
        leadLine.setInfos(infos);

        final Cell<SpreadSheet> cell21 = sheet.getImmutableCellAt(21, i);
        final String desc = cell21.getValue().toString().trim();
        leadLine.setDesc(desc);

        final Cell<SpreadSheet> cell22 = sheet.getImmutableCellAt(22, i);
        final String conv = cell22.getValue().toString().trim();
        leadLine.setConverti(conv);

        final Cell<SpreadSheet> cell23 = sheet.getImmutableCellAt(23, i);
        final String budget = cell23.getValue().toString().trim();
        leadLine.setBudget(budget);

        final Cell<SpreadSheet> cell24 = sheet.getImmutableCellAt(24, i);
        final String com = cell24.getValue().toString().trim();
        leadLine.setCom(com);

        final Cell<SpreadSheet> cell25 = sheet.getImmutableCellAt(25, i);
        final String dispo = cell25.getValue().toString().trim();
        leadLine.setDispo(dispo);

        final Cell<SpreadSheet> cell26 = sheet.getImmutableCellAt(26, i);
        final String cat = cell26.getValue().toString().trim();
        leadLine.setCategorie(cat);

        final Cell<SpreadSheet> cell27 = sheet.getImmutableCellAt(27, i);
        final String type = cell27.getValue().toString().trim();
        leadLine.setTypeT(type);

        return leadLine;
    }

    public AdresseCSV createAdresse(int i, Sheet sheet, int id) {
        final Cell<SpreadSheet> cell6 = sheet.getImmutableCellAt(6, i);
        String street = cell6.getValue().toString().trim();

        final Cell<SpreadSheet> cell7 = sheet.getImmutableCellAt(7, i);
        final String street2 = cell7.getValue().toString().trim();
        if (street2 != null && street2.trim().length() > 0) {
            street += "\n" + street2;
        }

        final Cell<SpreadSheet> cell8 = sheet.getImmutableCellAt(8, i);
        final String ville = cell8.getValue().toString().trim();
        final Cell<SpreadSheet> cell9 = sheet.getImmutableCellAt(9, i);
        final String cp = cell9.getValue().toString().trim();

        AdresseCSV adrLine = new AdresseCSV(id, street, ville, cp);

        return adrLine;
    }

    public Map<Object, LeadCSV> exportLead(DBRoot root, File dir2save, File sheetFile) throws Exception {

        List<String[]> adresse = new ArrayList<String[]>();
        List<String[]> leadList = new ArrayList<String[]>();

        Map<Object, LeadCSV> leadMap = new HashMap<Object, LeadCSV>();

        final ODPackage pkg = new ODPackage(sheetFile);
        if (pkg.getContentType().getType() != ContentType.SPREADSHEET)
            throw new IOException("Pas un tableur");
        final SpreadSheet calc = pkg.getSpreadSheet();
        final Sheet commandesSheet = calc.getSheet(0);
        int sheetCount = commandesSheet.getRowCount();
        for (int s = 1; s < sheetCount; s++) {

            final Cell<SpreadSheet> cell0 = commandesSheet.getImmutableCellAt(0, s);
            final String societeName = cell0.getValue().toString().trim();
            final Cell<SpreadSheet> cell16 = commandesSheet.getImmutableCellAt(16, s);
            final String name = cell16.getValue().toString().trim();
            if (societeName.trim().length() == 0 && name.trim().length() == 0) {
                break;
            }
            // Adresse principale
            int idAdr = adresse.size();
            AdresseCSV adr = createAdresse(s, commandesSheet, idAdr);
            adresse.add(adr.toCSVLine());
            LeadCSV lead = createLead(s, commandesSheet, idAdr, leadList.size());
            leadList.add(lead.toCSVLine());
            leadMap.put(lead.getId(), lead);
        }

        DataImporter importer = new DataImporter(root.getTable("LEAD"));
        final File csvFile = new File(dir2save, "Lead.csv");
        csvFile.createNewFile();
        importer.exportModelToCSV(csvFile, leadList);
        DataImporter importerAdr = new DataImporter(root.getTable("ADRESSE"));
        final File csvFile2 = new File(dir2save, "Address.csv");
        csvFile2.createNewFile();
        importerAdr.exportModelToCSV(csvFile2, adresse);

        return leadMap;
    }

    public void importFromFile(File csvFileDir, DBRoot root) throws Exception {

        SQLRowValues rowValsLead = new SQLRowValues(root.getTable("LEAD"));
        rowValsLead.put("COMPANY", null);
        rowValsLead.put("APE", null);
        rowValsLead.putRowValues("ID_ADRESSE").putNulls("VILLE", "RUE");
        SQLRowValuesListFetcher fecther = SQLRowValuesListFetcher.create(rowValsLead);
        List<SQLRowValues> existingLeads = fecther.fetch();
        List<String> existingHashLead = new ArrayList<String>(existingLeads.size());

        for (SQLRowValues sqlRowValues : existingLeads) {
            existingHashLead.add(sqlRowValues.getString("APE") + "----" + sqlRowValues.getString("COMPANY") + "----" + sqlRowValues.getForeign("ID_ADRESSE").getString("RUE") + "----"
                    + sqlRowValues.getForeign("ID_ADRESSE").getString("VILLE"));
        }

        // Adresse
        CSVImporter<AdresseCSV> adr = new CSVImporter<AdresseCSV>() {
            @Override
            public AdresseCSV convert(String[] line) {
                return AdresseCSV.fromCSVLine(line);
            }
        };
        final Map<Object, AdresseCSV> mapAdr = adr.importFrom(new File(csvFileDir, "Address.csv"));

        Map<Object, SQLRowValues> mapAdrRows = new HashMap<Object, SQLRowValues>();
        for (Object o : mapAdr.keySet()) {
            AdresseCSV c = mapAdr.get(o);
            mapAdrRows.put(c.getId(), c.toSQLRowValues(root));
        }

        // Client
        CSVImporter<LeadCSV> client = new CSVImporter<LeadCSV>() {
            @Override
            public LeadCSV convert(String[] line) {
                return LeadCSV.fromCSVLine(line);
            }
        };

        Map<Object, LeadCSV> mapClient = client.importFrom(new File(csvFileDir, "Lead.csv"));

        Map<Object, SQLRowValues> mapCliRows = new HashMap<Object, SQLRowValues>();
        for (Object o : mapClient.keySet()) {
            LeadCSV c = mapClient.get(o);

            String hashLead = c.getApe() + "----" + c.getName() + "----" + mapAdr.get(c.getIdAdr()).getStreet() + "----" + mapAdr.get(c.getIdAdr()).getCity();
            if (!existingHashLead.contains(hashLead)) {
                mapCliRows.put(c.getId(), c.toSQLRowValues(root, mapAdrRows));
            } else {
                System.err.println("Prospect déjà existant " + c.getName() + " --> Non importé");
            }
        }

        final Set<SQLRowValuesCluster> graphs = new IdentityHashSet<SQLRowValuesCluster>();
        for (Object c : mapCliRows.keySet()) {
            graphs.add(mapCliRows.get(c).getGraph());
        }

        for (final SQLRowValuesCluster graph : graphs) {
            graph.store(StoreMode.COMMIT);
        }
    }

}
