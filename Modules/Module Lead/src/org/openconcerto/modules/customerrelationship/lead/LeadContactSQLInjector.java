/*
 * Créé le 5 nov. 2012
 */
package org.openconcerto.modules.customerrelationship.lead;

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLInjector;
import org.openconcerto.sql.model.SQLTable;

public class LeadContactSQLInjector extends SQLInjector {

    private final static SQLTable contactTable = Configuration.getInstance().getDirectory().getElement("CONTACT").getTable();
    private static final SQLTable leadTable = Configuration.getInstance().getDirectory().getElement(Module.TABLE_LEAD).getTable();

    public LeadContactSQLInjector() {
        super(leadTable, contactTable, false);
        createDefaultMap();
        map(leadTable.getField("NAME"), contactTable.getField("NOM"));
        map(leadTable.getField("FIRSTNAME"), contactTable.getField("PRENOM"));
        map(leadTable.getField("PHONE"), contactTable.getField("TEL_DIRECT"));
        map(leadTable.getField("MOBILE"), contactTable.getField("TEL_MOBILE"));
        map(leadTable.getField("FAX"), contactTable.getField("FAX"));
        map(leadTable.getField("ROLE"), contactTable.getField("FONCTION"));
    }
}
