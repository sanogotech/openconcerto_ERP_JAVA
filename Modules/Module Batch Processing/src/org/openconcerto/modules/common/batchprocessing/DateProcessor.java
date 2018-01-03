package org.openconcerto.modules.common.batchprocessing;

import java.awt.FlowLayout;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.ui.JDate;

public class DateProcessor extends JPanel implements BatchProcessor {
    private final JDate d = new JDate(true);
    private final SQLField field;

    public DateProcessor(SQLField field) {
        this.field = field;
        this.setLayout(new FlowLayout());
        this.add(new JLabel("forcer la date au "));
        this.add(d);

    }

    @Override
    public void process(List<SQLRowValues> r) throws SQLException {
        final Date date = d.getDate();
        for (SQLRowAccessor sqlRowAccessor : r) {
            final SQLRowValues rowValues = sqlRowAccessor.createEmptyUpdateRow();
            rowValues.put(field.getName(), date);
            processBeforeUpdate(sqlRowAccessor, rowValues);
            rowValues.update();
        }

    }

    @Override
    public boolean checkParameters() {
        final Date date = d.getDate();
        if (date == null && !field.isNullable()) {
            return false;
        }
        return true;
    }

    @Override
    public void processBeforeUpdate(SQLRowAccessor from, SQLRowValues to) {
    }
}
