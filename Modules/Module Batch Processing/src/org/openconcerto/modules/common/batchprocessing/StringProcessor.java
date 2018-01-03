package org.openconcerto.modules.common.batchprocessing;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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

public class StringProcessor extends JPanel implements BatchProcessor {
    private final SQLField field;
    // Editors
    final JTextField tReplace = new JTextField();
    private JRadioButton bReplace;
    final JTextField tPrefix = new JTextField();
    private JRadioButton bPrefix;
    final JTextField tSuffix = new JTextField();
    private JRadioButton bSuffix;
    private JRadioButton bLower;
    private JRadioButton bUpper;

    public StringProcessor(SQLField field) {
        this.field = field;

        this.setLayout(new GridBagLayout());
        bReplace = new JRadioButton("remplacer par");
        bPrefix = new JRadioButton("préfixer par");
        bSuffix = new JRadioButton("suffixer par");
        bLower = new JRadioButton("mettre en minuscule");
        bUpper = new JRadioButton("mettre en majuscule");

        final ButtonGroup group = new ButtonGroup();
        group.add(bReplace);
        group.add(bPrefix);
        group.add(bSuffix);
        group.add(bLower);
        group.add(bUpper);

        GridBagConstraints c = new DefaultGridBagConstraints();
        // replace
        this.add(bReplace, c);
        c.gridx++;
        c.weightx = 1;
        this.add(tReplace, c);
        // prefix
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        this.add(bPrefix, c);
        c.gridx++;
        c.weightx = 1;
        this.add(tPrefix, c);
        // suffix
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        this.add(bSuffix, c);
        c.gridx++;
        c.weightx = 1;
        this.add(tSuffix, c);
        //
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        c.gridwidth = 2;
        this.add(bLower, c);
        c.gridy++;
        this.add(bUpper, c);

        tPrefix.setEnabled(false);
        tSuffix.setEnabled(false);
        group.setSelected(bReplace.getModel(), true);

        bLower.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                tPrefix.setEnabled(false);
                tReplace.setEnabled(false);
                tSuffix.setEnabled(false);

            }
        });
        bUpper.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                tPrefix.setEnabled(false);
                tReplace.setEnabled(false);
                tSuffix.setEnabled(false);
            }
        });
        bPrefix.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                tPrefix.setEnabled(true);
                tReplace.setEnabled(false);
                tSuffix.setEnabled(false);

            }
        });
        bSuffix.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                tPrefix.setEnabled(false);
                tReplace.setEnabled(false);
                tSuffix.setEnabled(true);

            }
        });
        bReplace.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                tPrefix.setEnabled(false);
                tReplace.setEnabled(true);
                tSuffix.setEnabled(false);

            }
        });

    }

    @Override
    public void process(List<SQLRowValues> r) throws SQLException {
        if (bReplace.isSelected()) {
            final String t = ensureSize(tReplace.getText());
            for (SQLRowAccessor sqlRowAccessor : r) {
                final SQLRowValues rowValues = sqlRowAccessor.createEmptyUpdateRow();
                rowValues.put(field.getName(), t);
                processBeforeUpdate(sqlRowAccessor, rowValues);
                rowValues.update();
            }
        } else if (bPrefix.isSelected()) {
            final String t = tPrefix.getText();
            for (SQLRowAccessor sqlRowAccessor : r) {
                final SQLRowValues rowValues = sqlRowAccessor.createEmptyUpdateRow();
                String str = sqlRowAccessor.asRow().getString(field.getName());
                if (str == null)
                    str = "";
                rowValues.put(field.getName(), ensureSize(t + str));
                processBeforeUpdate(sqlRowAccessor, rowValues);
                rowValues.update();
            }
        } else if (bSuffix.isSelected()) {
            final String t = tSuffix.getText();
            for (SQLRowAccessor sqlRowAccessor : r) {
                final SQLRowValues rowValues = sqlRowAccessor.createEmptyUpdateRow();
                String str = sqlRowAccessor.asRow().getString(field.getName());
                if (str == null)
                    str = "";
                rowValues.put(field.getName(), ensureSize(str + t));
                processBeforeUpdate(sqlRowAccessor, rowValues);
                rowValues.update();
            }
        } else if (bLower.isSelected()) {
            for (SQLRowAccessor sqlRowAccessor : r) {
                String str = sqlRowAccessor.asRow().getString(field.getName());
                if (str == null)
                    str = "";
                final SQLRowValues rowValues = sqlRowAccessor.createEmptyUpdateRow();
                rowValues.put(field.getName(), str.toLowerCase());
                processBeforeUpdate(sqlRowAccessor, rowValues);
                rowValues.update();
            }
        } else if (bUpper.isSelected()) {
            for (SQLRowAccessor sqlRowAccessor : r) {
                String str = sqlRowAccessor.asRow().getString(field.getName());
                if (str == null)
                    str = "";
                final SQLRowValues rowValues = sqlRowAccessor.createEmptyUpdateRow();
                rowValues.put(field.getName(), str.toUpperCase());
                processBeforeUpdate(sqlRowAccessor, rowValues);
                rowValues.update();
            }
        }
    }

    private String ensureSize(String text) {
        if (text.length() < field.getType().getSize()) {
            return text;
        }
        return text.substring(0, field.getType().getSize());
    }

    @Override
    public boolean checkParameters() {
        if (bReplace.isSelected()) {
            return true;
        } else if (bPrefix.isSelected()) {
            return !tPrefix.getText().isEmpty();
        } else if (bSuffix.isSelected()) {
            return !bPrefix.getText().isEmpty();
        }
        return true;
    }

    @Override
    public void processBeforeUpdate(SQLRowAccessor from, SQLRowValues to) {
    }
}
