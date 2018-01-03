package org.openconcerto.modules.customerrelationship.lead.importer;

import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.SQLRowValues;

public class AdresseCSV extends LineCSV {

    public final static int ID = 0;
    public final static int STREET = 1;
    public final static int CITY = 2;
    public final static int CODE = 3;
    public final static int ID_LEAD = 4;

    private String code, city, street;

    public AdresseCSV(Object id, String street, String city, String code) {
        this.id = id;
        this.code = code;
        this.city = city;
        this.street = street;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String[] toCSVLine() {
        String[] values = new String[10];

        values[ID] = this.id.toString();
        values[CODE] = this.code;
        values[CITY] = this.city;
        values[STREET] = String.valueOf(this.street);

        return values;
    }

    public static AdresseCSV fromCSVLine(String[] line) {
        AdresseCSV adr = new AdresseCSV(line[ID], line[STREET], line[CITY], line[CODE]);
        return adr;
    }

    public SQLRowValues toSQLRowValues(DBRoot root) {
        SQLRowValues rowVals = new SQLRowValues(root.getTable("ADRESSE"));
        rowVals.put("RUE", getStreet());
        rowVals.put("VILLE", getCity());
        rowVals.put("CODE_POSTAL", getCode());
        rowVals.put("PAYS", "France");
        return rowVals;
    }
}
