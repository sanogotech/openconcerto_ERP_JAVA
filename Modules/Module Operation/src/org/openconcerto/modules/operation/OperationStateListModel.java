package org.openconcerto.modules.operation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.SwingUtilities;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.SQLDataSource;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.SQLTableEvent;
import org.openconcerto.sql.model.SQLTableModifiedListener;
import org.openconcerto.ui.list.CheckListItem;

public class OperationStateListModel extends CheckListModel {
    private static List<String> lCache = null;

    OperationStateListModel() {
        final DBRoot r = ComptaPropsConfiguration.getInstanceCompta().getRootSociete();
        final SQLTable operationTable = r.getTable(ModuleOperation.TABLE_OPERATION);
        operationTable.addTableModifiedListener(new SQLTableModifiedListener() {

            @Override
            public void tableModified(SQLTableEvent evt) {
                SwingUtilities.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        loadContent();
                    }
                });

            }
        });
    }

    @Override
    public List<CheckListItem> loadItems() {
        final List<String> l = getStatusFromDB();
        final List<CheckListItem> items = new ArrayList<CheckListItem>();
        for (String status : l) {
            items.add(new CheckListItem(status, true));
        }
        return items;
    }

    public static synchronized List<String> getStatus() {
        if (lCache == null) {
            final List<String> l = getStatusFromDB();
            return l;
        }
        return lCache;
    }

    public static synchronized List<String> getStatusFromDB() {
        final SQLSelect select = new SQLSelect();
        final DBRoot r = ComptaPropsConfiguration.getInstanceCompta().getRootSociete();
        final SQLTable operationTable = r.getTable(ModuleOperation.TABLE_OPERATION);
        final SQLField fieldStatus = operationTable.getField("STATUS");
        select.addSelect(fieldStatus);
        select.addGroupBy(fieldStatus);
        final SQLDataSource ds = ComptaPropsConfiguration.getInstanceCompta().getRootSociete().getDBSystemRoot().getDataSource();
        @SuppressWarnings("unchecked")
        final List<String> l = ds.executeCol(select.asString());
        Collections.sort(l);
        lCache = l;
        return l;
    }
}
