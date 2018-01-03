package org.openconcerto.modules.common.batchprocessing;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.ui.DefaultGridBagConstraints;

public class NumberProcessor extends JPanel implements BatchProcessor {

    private final SQLField field;
    // Editors
    final JTextField tReplace = new JTextField();
    private JRadioButton bReplace;
    final JTextField tAdd = new JTextField();
    private JRadioButton bAdd;

    final JTextField tRemove = new JTextField();
    private JRadioButton bRemove;

    public NumberProcessor(SQLField field) {
        this.field = field;

        this.setLayout(new GridBagLayout());
        bReplace = new JRadioButton("remplacer par");
        bAdd = new JRadioButton("augmenter de");
        bRemove = new JRadioButton("diminuer de");

        final ButtonGroup group = new ButtonGroup();
        group.add(bReplace);
        group.add(bAdd);
        group.add(bRemove);

        GridBagConstraints c = new DefaultGridBagConstraints();
        // replace
        this.add(bReplace, c);
        c.gridx++;
        c.weightx = 1;
        this.add(tReplace, c);
        // add
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        this.add(bAdd, c);
        c.gridx++;
        c.weightx = 1;
        this.add(tAdd, c);
        // remove
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        this.add(bRemove, c);
        c.gridx++;
        c.weightx = 1;
        this.add(tRemove, c);
        //
        tAdd.setEnabled(false);
        tRemove.setEnabled(false);

        tAdd.setInputVerifier(new DecimalOrPercentVerifier());
        tRemove.setInputVerifier(new DecimalOrPercentVerifier());

        group.setSelected(bReplace.getModel(), true);

        bReplace.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                tReplace.setEnabled(true);
                tAdd.setEnabled(false);
                tRemove.setEnabled(false);
            }
        });
        bAdd.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                tReplace.setEnabled(false);
                tAdd.setEnabled(true);
                tRemove.setEnabled(false);

            }
        });
        bRemove.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                tReplace.setEnabled(false);
                tAdd.setEnabled(false);
                tRemove.setEnabled(true);

            }
        });
    }

    @Override
    public void process(List<SQLRowValues> r) throws SQLException {
        if (bReplace.isSelected()) {
            BigDecimal v = new BigDecimal(this.tReplace.getText().trim());
            for (SQLRowAccessor sqlRowAccessor : r) {
                final SQLRowValues rowValues = sqlRowAccessor.createEmptyUpdateRow();
                rowValues.put(field.getName(), decimalToFieldType(v));
                processBeforeUpdate(sqlRowAccessor, rowValues);
                rowValues.update();
            }
        } else if (bAdd.isSelected()) {

            String t = this.tAdd.getText().trim();
            boolean isPercent = false;
            if (t.endsWith("%")) {
                t = t.substring(0, t.length() - 1);
                isPercent = true;
            }

            BigDecimal v = new BigDecimal(t);

            for (SQLRowAccessor sqlRowAccessor : r) {
                final SQLRowValues rowValues = sqlRowAccessor.createEmptyUpdateRow();
                final BigDecimal value = sqlRowAccessor.asRow().getBigDecimal(field.getName());
                if (value != null) {
                    if (isPercent) {
                        rowValues.put(field.getName(), decimalToFieldType(value.multiply(v.divide(new BigDecimal(100)).add(BigDecimal.ONE))));
                    } else {
                        rowValues.put(field.getName(), decimalToFieldType(value.add(v)));
                    }
                    processBeforeUpdate(sqlRowAccessor, rowValues);
                    rowValues.update();
                }
            }
        } else if (bRemove.isSelected()) {
            String t = this.tRemove.getText().trim();
            boolean isPercent = false;
            if (t.endsWith("%")) {
                t = t.substring(0, t.length() - 1);
                isPercent = true;
            }

            BigDecimal v = new BigDecimal(t);
            for (SQLRowAccessor sqlRowAccessor : r) {
                final SQLRowValues rowValues = sqlRowAccessor.createEmptyUpdateRow();

                final BigDecimal value = sqlRowAccessor.asRow().getBigDecimal(field.getName());
                if (value != null) {
                    if (isPercent) {
                        rowValues.put(field.getName(), decimalToFieldType(value.multiply(v.divide(new BigDecimal(-100)).add(BigDecimal.ONE))));
                    } else {
                        rowValues.put(field.getName(), decimalToFieldType(value.add(v)));
                    }
                    processBeforeUpdate(sqlRowAccessor, rowValues);
                    rowValues.update();
                }
            }
        }
    }

    private Object decimalToFieldType(BigDecimal v) {
        final Class<?> javaType = field.getType().getJavaType();
        if (javaType.equals(BigDecimal.class)) {
            return v;
        } else if (javaType.equals(Float.class)) {
            return v.floatValue();
        } else if (javaType.equals(Double.class)) {
            return v.doubleValue();
        } else if (javaType.equals(Integer.class)) {
            return v.intValue();
        } else if (javaType.equals(Long.class)) {
            return v.longValue();
        }
        return v;
    }

    @Override
    public boolean checkParameters() {
        if (bReplace.isSelected()) {
            try {
                BigDecimal v = new BigDecimal(this.tReplace.getText().trim());
                return v != null;
            } catch (Exception e) {
                return false;
            }
        } else if (bAdd.isSelected()) {
            return tAdd.getInputVerifier().verify(tAdd);

        } else if (bRemove.isSelected()) {
            return tRemove.getInputVerifier().verify(tRemove);
        }
        return false;
    }

    @Override
    public void processBeforeUpdate(SQLRowAccessor from, SQLRowValues to) {
        // TODO Auto-generated method stub

    }
}
