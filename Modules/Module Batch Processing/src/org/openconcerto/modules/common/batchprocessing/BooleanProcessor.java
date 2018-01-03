package org.openconcerto.modules.common.batchprocessing;

import java.sql.SQLException;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.ui.VFlowLayout;

public class BooleanProcessor extends JPanel implements BatchProcessor {
    private final SQLField field;

    private JRadioButton bTrue;
    private JRadioButton bFalse;
    private JRadioButton bInvert;

    public BooleanProcessor(SQLField field) {
        this.field = field;
        this.setLayout(new VFlowLayout());
        bTrue = new JRadioButton("forcer à vrai");
        bFalse = new JRadioButton("forcer à faux");
        bInvert = new JRadioButton("inverser");
        final ButtonGroup group = new ButtonGroup();
        group.add(bTrue);
        group.add(bFalse);
        group.add(bInvert);
        this.add(bTrue);
        this.add(bFalse);
        this.add(bInvert);
        group.setSelected(bTrue.getModel(), true);

    }

    @Override
    public void process(List<SQLRowValues> r) throws SQLException {
        if (bTrue.isSelected()) {
            for (SQLRowAccessor sqlRowAccessor : r) {
                final SQLRowValues rowValues = sqlRowAccessor.createEmptyUpdateRow();
                rowValues.put(field.getName(), Boolean.TRUE);
                processBeforeUpdate(sqlRowAccessor, rowValues);
                rowValues.update();
            }
        } else if (bFalse.isSelected()) {
            for (SQLRowAccessor sqlRowAccessor : r) {
                final SQLRowValues rowValues = sqlRowAccessor.createEmptyUpdateRow();
                rowValues.put(field.getName(), Boolean.FALSE);
                processBeforeUpdate(sqlRowAccessor, rowValues);
                rowValues.update();
            }
        } else if (bInvert.isSelected()) {
            for (SQLRowAccessor sqlRowAccessor : r) {
                final SQLRowValues rowValues = sqlRowAccessor.createEmptyUpdateRow();
                final Boolean boolean1 = sqlRowAccessor.asRow().getBoolean(field.getName());
                if (boolean1 != null) {
                    rowValues.put(field.getName(), boolean1.equals(Boolean.FALSE));
                    processBeforeUpdate(sqlRowAccessor, rowValues);
                    rowValues.update();
                }
            }
        }
    }

    @Override
    public boolean checkParameters() {
        return true;
    }

    public void processBeforeUpdate(SQLRowAccessor from, SQLRowValues to) {
    }
}
