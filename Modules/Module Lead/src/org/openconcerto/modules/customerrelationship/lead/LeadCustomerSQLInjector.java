/*
 * Créé le 5 nov. 2012
 */
package org.openconcerto.modules.customerrelationship.lead;

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLInjector;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;

public class LeadCustomerSQLInjector extends SQLInjector {

    private final static SQLTable customerTable = Configuration.getInstance().getDirectory().getElement("CLIENT").getTable();
    private static final SQLTable leadTable = Configuration.getInstance().getDirectory().getElement(Module.TABLE_LEAD).getTable();

    public LeadCustomerSQLInjector() {
        super(leadTable, customerTable, false);
        createDefaultMap();
        map(leadTable.getField("NAME"), customerTable.getField("RESPONSABLE"));
        map(leadTable.getField("COMPANY"), customerTable.getField("NOM"));
        map(leadTable.getField("PHONE"), customerTable.getField("TEL"));
        map(leadTable.getField("MOBILE"), customerTable.getField("TEL_P"));
        map(leadTable.getField("FAX"), customerTable.getField("FAX"));
        map(leadTable.getField("EMAIL"), customerTable.getField("MAIL"));
        map(leadTable.getField("MOBILE"), customerTable.getField("TEL_P"));
        // map(leadTable.getField("INFORMATION"), customerTable.getField("INFOS"));
        map(getSource().getField("INFOS"), getDestination().getField("INFOS"));
        remove(leadTable.getField("ID_ADRESSE"), customerTable.getField("ID_ADRESSE"));
    }

    @Override
    protected void merge(SQLRowAccessor srcRow, SQLRowValues rowVals) {
        // TODO Auto-generated method stub
        super.merge(srcRow, rowVals);
        if (rowVals.getString("NOM") == null || rowVals.getString("NOM").trim().length() == 0) {
            rowVals.put("NOM", srcRow.getString("NAME"));
        }
    }
}
