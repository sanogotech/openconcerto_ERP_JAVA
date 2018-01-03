package org.openconcerto.modules.customerrelationship.lead;

import java.awt.Dimension;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;

import org.openconcerto.erp.modules.ModuleElement;
import org.openconcerto.sql.element.GroupSQLComponent;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLRowValuesListFetcher;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.view.QuickAssignPanel;
import org.openconcerto.sql.view.list.RowValuesTableModel;
import org.openconcerto.sql.view.list.SQLTableElement;
import org.openconcerto.ui.group.Group;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.cc.ITransformer;

public class GroupSQLComponentWithService extends GroupSQLComponent {

    public GroupSQLComponentWithService(ModuleElement element, Group group) {
        super(element, group);
    }

    @Override
    public JComponent getLabel(String id) {
        if (id.equals("ibd.services")) {
            return new JLabel("Services proposés");
        }
        return super.getLabel(id);
    }

    @Override
    public JComponent createEditor(String id) {
        if (id.equals("ibd.services")) {
            final SQLElement serviceElement = this.getElement().getDirectory().getElement(Module.TABLE_SERVICE);
            final List<SQLTableElement> tableElements = new ArrayList<SQLTableElement>();
            tableElements.add(new SQLTableElement(serviceElement.getTable().getField("NAME")));
            final RowValuesTableModel model = new RowValuesTableModel(serviceElement, tableElements, serviceElement.getTable().getKey(), false);
            QuickAssignPanel table = new QuickAssignPanel(serviceElement, "ID", model);
            table.setMinimumSize(new Dimension(200, 150));
            table.setPreferredSize(new Dimension(200, 150));
            return table;
        }
        return super.createEditor(id);
    }

    QuickAssignPanel getQuickAssignPanel() {
        return (QuickAssignPanel) getEditor("ibd.services");
    }

    @Override
    public void select(final SQLRowAccessor row) {
        final QuickAssignPanel panel = getQuickAssignPanel();
        panel.getModel().clearRows();
        if (row != null) {
            final SQLTable associationTable = getServiceAssociationTable();
            SQLRowValues graph = new SQLRowValues(associationTable);
            graph.put("ID", null);
            graph.putRowValues("ID_SERVICE").put("NAME", null);
            final String tableName = this.getElement().getTable().getName();
            SQLRowValuesListFetcher f = new SQLRowValuesListFetcher(graph);
            f.setSelTransf(new ITransformer<SQLSelect, SQLSelect>() {
                @Override
                public SQLSelect transformChecked(SQLSelect input) {
                    input.setWhere(new Where(associationTable.getField("ID_" + tableName), "=", row.getID()));
                    return input;
                }
            });
            List<SQLRowValues> associations = f.fetch();
            for (SQLRowValues r : associations) {
                panel.getModel().addRow(r.getForeign("ID_SERVICE").asRowValues());
            }

        }
        super.select(row);
    }

    @Override
    public int insert(SQLRow order) {
        int id = super.insert(order);
        insertAssociatedServiceTo(id);
        SQLRow row = getTable().getRow(id);
        String field = "ID_LEAD";
        if (getTable().contains("ID_CLIENT")) {
            field = "ID_CLIENT";
        }
        SQLRowValues rowVals = row.getForeign(field).createEmptyUpdateRow();
        rowVals.put("REMIND_DATE", row.getObject("NEXTCONTACT_DATE"));
        try {
            rowVals.update();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return id;
    }

    @Override
    public void update() {
        super.update();
        final SQLTable associationTable = getServiceAssociationTable();
        String query = "DELETE FROM " + associationTable.getSQL() + " WHERE \"ID_" + this.getElement().getTable().getName() + "\" = " + getSelectedID();
        associationTable.getDBSystemRoot().getDataSource().execute(query);
        insertAssociatedServiceTo(getSelectedID());
        SQLRow row = getTable().getRow(getSelectedID());
        String field = "ID_LEAD";
        if (getTable().contains("ID_CLIENT")) {
            field = "ID_CLIENT";
        }
        SQLRowValues rowVals = row.getForeign(field).createEmptyUpdateRow();
        rowVals.put("REMIND_DATE", row.getObject("NEXTCONTACT_DATE"));
        try {
            rowVals.update();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void insertAssociatedServiceTo(int id) {
        final SQLTable associationTable = getServiceAssociationTable();
        final QuickAssignPanel panel = getQuickAssignPanel();
        final String tableName = this.getElement().getTable().getName();
        final int stop = panel.getModel().getRowCount();
        for (int i = 0; i < stop; i++) {
            SQLRowValues rService = panel.getModel().getRowValuesAt(i);
            SQLRowValues rAccess = new SQLRowValues(associationTable);
            rAccess.put("ID_" + tableName, id);
            rAccess.put("ID_SERVICE", rService.getID());
            try {
                rAccess.commit();
            } catch (SQLException e) {
                ExceptionHandler.handle("Unable to store service assocation", e);
            }
        }
    }

    SQLTable getServiceAssociationTable() {
        final SQLTable table = this.getElement().getTable();
        return table.getTable(table.getName() + "_SERVICE");
    }
}
