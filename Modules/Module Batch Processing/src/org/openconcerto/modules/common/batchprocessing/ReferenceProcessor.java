package org.openconcerto.modules.common.batchprocessing;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.sql.SQLException;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.ui.warning.JLabelWarning;

public class ReferenceProcessor extends JPanel implements BatchProcessor {

    private final SQLField field;
    final SQLElement element;
    private ElementComboBox combo;

    public ReferenceProcessor(SQLField field) {
        this.field = field;
        this.element = ComptaPropsConfiguration.getInstanceCompta().getDirectory().getElement(field.getForeignTable());

        if (element != null) {
            this.setLayout(new BorderLayout());
            this.add(new JLabel("remplacer par "), BorderLayout.WEST);
            combo = new ElementComboBox(true, 200);
            combo.setMinimal();
            combo.setAddIconVisible(false);
            combo.init(element);
            this.add(combo, BorderLayout.CENTER);
        } else {
            this.setLayout(new FlowLayout());
            this.add(new JLabelWarning("No element for table " + field.getTable().getName()));
        }
    }

    @Override
    public void process(List<SQLRowValues> r) throws SQLException {

        for (SQLRowAccessor sqlRowAccessor : r) {
            final SQLRowValues rowValues = sqlRowAccessor.createEmptyUpdateRow();
            rowValues.put(field.getName(), combo.getSelectedId());
            processBeforeUpdate(sqlRowAccessor, rowValues);
            rowValues.update();
        }

    }

    @Override
    public boolean checkParameters() {
        return this.element != null;
    }

    @Override
    public void processBeforeUpdate(SQLRowAccessor from, SQLRowValues to) {

    }
}
