package org.openconcerto.modules.reports.olap;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.olap4j.CellSet;
import org.olap4j.OlapConnection;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.warning.JLabelWarning;

public class OLAPMDXPanel extends JPanel {

    private JTextArea textMDX = new JTextArea();
    private OlapConnection olapConnection;
    private OLAPRenderer renderer;

    final JLabelWarning w = new JLabelWarning();
    private JButton bExecute;

    OLAPMDXPanel(OLAPParametersPanel parameters, OlapConnection olapConnection, OLAPRenderer renderer) {
        this.setOpaque(false);
        this.olapConnection = olapConnection;
        this.renderer = renderer;
        this.setLayout(new GridBagLayout());
        GridBagConstraints c = new DefaultGridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        // Colonnes
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 0;
        JLabel columnsLabel = new JLabel("Requête", SwingConstants.LEFT);
        this.add(columnsLabel, c);
        c.weighty = 1;
        c.gridy++;
        c.weightx = 1;
        textMDX.setLineWrap(true);
        JScrollPane scroll = new JScrollPane(textMDX);

        scroll.setOpaque(false);
        this.add(scroll, c);

        // Lignes
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        c.weighty = 0;
        bExecute = new JButton("Executer");
        bExecute.setOpaque(false);
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;
        this.add(bExecute, c);
        bExecute.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                configurationModified();

            }
        });
        c.gridy++;
        w.setOpaque(false);
        w.setVisible(false);
        this.add(w, c);

        this.textMDX.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void removeUpdate(DocumentEvent e) {
                w.setVisible(false);

            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                w.setVisible(false);

            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                w.setVisible(false);

            }
        });

    }

    public void setMDX(String s) {
        this.textMDX.setText(s);
        w.setVisible(false);
    }

    void configurationModified() {
        this.textMDX.setEditable(false);
        this.bExecute.setEnabled(false);
        final String mdxQuery = textMDX.getText().trim();
        SwingWorker<CellSet, Object> worker = new SwingWorker<CellSet, Object>() {

            @Override
            protected CellSet doInBackground() throws Exception {
                try {

                    CellSet results = olapConnection.prepareOlapStatement(mdxQuery).executeQuery();

                    return results;
                } catch (Exception e) {

                    e.printStackTrace();
                    w.setVisible(true);

                    w.setText(e.getCause().getCause().getMessage());
                    return null;
                }
            }

            protected void done() {
                CellSet results;
                try {
                    results = get();
                    renderer.setCellSet(results);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                textMDX.setEditable(true);
                bExecute.setEnabled(true);
            };
        };
        renderer.setWaitState(true);
        worker.execute();

    }

    public void execute(String query) {
        this.setMDX(query);
        configurationModified();

    }
}
