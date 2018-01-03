package org.openconcerto.modules.common.batchprocessing;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.PropsConfiguration;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.request.SQLFieldTranslator;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.JLabelBold;
import org.openconcerto.ui.ReloadPanel;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.SwingWorker2;

public class BatchEditorPanel extends JPanel {

    public BatchEditorPanel(final List<SQLRowValues> rows, FieldFilter filter) {
        Configuration conf = PropsConfiguration.getInstance();
        final SQLFieldTranslator translator = conf.getTranslator();
        Set<SQLField> fields = rows.get(0).getTable().getFields();
        List<SQLField> f = new ArrayList<SQLField>();
        for (SQLField sqlField : fields) {
            if (ForbiddenFieldName.isAllowed(sqlField.getName()) && translator.getLabelFor(sqlField) != null) {
                if (filter == null || (filter != null && !filter.isFiltered(sqlField))) {
                    f.add(sqlField);
                }
            }
        }

        Collections.sort(f, new Comparator<SQLField>() {

            @Override
            public int compare(SQLField o1, SQLField o2) {
                return translator.getLabelFor(o1).compareToIgnoreCase(translator.getLabelFor(o2));
            }
        });
        this.setLayout(new GridBagLayout());
        GridBagConstraints c = new DefaultGridBagConstraints();
        this.add(new JLabel("Champ"), c);

        final JComboBox combo = new JComboBox(f.toArray());
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                value = translator.getLabelFor(((SQLField) value));
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
        });

        combo.setSelectedIndex(0);

        c.gridx++;
        c.weightx = 1;
        c.fill = GridBagConstraints.NONE;
        this.add(combo, c);
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.add(new JLabelBold("Action à appliquer"), c);
        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridy++;
        c.anchor = GridBagConstraints.NORTHWEST;
        final BatchDetailPanel comp = new BatchDetailPanel();
        comp.setField((SQLField) combo.getSelectedItem());
        this.add(comp, c);

        JPanel actions = new JPanel();
        actions.setLayout(new FlowLayout(FlowLayout.RIGHT));
        final JButton buttonProcess = new JButton("Lancer le traitement");

        final JButton buttonCancel = new JButton("Annuler");
        final ReloadPanel reload = new ReloadPanel();
        actions.add(reload);
        actions.add(buttonProcess);

        actions.add(buttonCancel);

        c.gridy++;
        c.weighty = 0;
        this.add(actions, c);

        combo.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent e) {
                comp.setField((SQLField) combo.getSelectedItem());

            }
        });
        buttonProcess.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (!comp.getProcessor().checkParameters()) {
                    JOptionPane.showMessageDialog(BatchEditorPanel.this, "Paramètres non valides");
                    return;
                }

                buttonProcess.setEnabled(false);
                buttonCancel.setEnabled(false);
                comp.setEnabled(false);

                combo.setEnabled(false);
                reload.setMode(ReloadPanel.MODE_ROTATE);
                SwingWorker2<Object, Object> w = new SwingWorker2<Object, Object>() {

                    @Override
                    protected Object doInBackground() throws Exception {
                        try {
                            final BatchProcessor processor = comp.getProcessor();
                            processor.process(rows);
                        } catch (Exception e) {
                            ExceptionHandler.handle("Echec du traitement", e);
                        }
                        return null;
                    }

                    @Override
                    protected void done() {
                        reload.setMode(ReloadPanel.MODE_EMPTY);
                        JOptionPane.showMessageDialog(BatchEditorPanel.this, "Traitement terminé");
                        SwingUtilities.getWindowAncestor(BatchEditorPanel.this).dispose();
                    }

                };
                w.execute();
            }
        });
        buttonCancel.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                SwingUtilities.getWindowAncestor(BatchEditorPanel.this).dispose();
            }
        });
    }
}
