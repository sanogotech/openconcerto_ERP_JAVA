/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2011 OpenConcerto, by ILM Informatique. All rights reserved.
 * 
 * The contents of this file are subject to the terms of the GNU General Public License Version 3
 * only ("GPL"). You may not use this file except in compliance with the License. You can obtain a
 * copy of the License at http://www.gnu.org/licenses/gpl-3.0.html See the License for the specific
 * language governing permissions and limitations under the License.
 * 
 * When distributing the software, include this License Header Notice in each file.
 */
 
 package org.openconcerto.erp.action;

import org.openconcerto.erp.config.Benchmark;
import org.openconcerto.sql.ui.InfoPanel;
import org.openconcerto.ui.JLabelBold;
import org.openconcerto.ui.ReloadPanel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

public final class AboutAction extends AbstractAction {

    static private final AboutAction instance = new AboutAction();

    public static AboutAction getInstance() {
        return instance;
    }

    private AboutAction() {
        super("Informations");

    }

    @Override
    public void actionPerformed(final ActionEvent event) {
        final JFrame frame = new JFrame((String) this.getValue(Action.NAME));
        JPanel p = new JPanel();
        p.setLayout(new BorderLayout());

        final JScrollPane contentPane = new JScrollPane(new InfoPanel());
        p.add(contentPane, BorderLayout.CENTER);
        p.add(createBenchMarkPanel(), BorderLayout.SOUTH);
        frame.setContentPane(p);
        frame.pack();

        final Dimension size = frame.getSize();

        final Dimension maxSize = new Dimension(size.width, 700);
        if (size.height > maxSize.height) {
            frame.setMinimumSize(maxSize);
            frame.setPreferredSize(maxSize);
            frame.setSize(maxSize);
        } else {
            frame.setMinimumSize(size);
            frame.setPreferredSize(size);
            frame.setSize(size);
        }
        final Dimension maximumSize = maxSize;
        frame.setMaximumSize(maximumSize);

        frame.setLocationRelativeTo(null);

        frame.setVisible(true);
    }

    private JPanel createBenchMarkPanel() {
        final JPanel p = new JPanel();
        p.setLayout(new FlowLayout(FlowLayout.LEFT));
        final JLabel lt = new JLabelBold("Test de performance : ");
        p.add(lt);
        final JLabel l = new JLabel("CLiquez sur démarrer pour lancer le test");
        p.add(l);
        final JButton b = new JButton("Démarrer");
        p.add(b);
        final ReloadPanel r = new ReloadPanel();
        p.add(r);
        b.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                l.setText("Test en cours");
                b.setEnabled(false);
                r.setMode(ReloadPanel.MODE_ROTATE);
                SwingWorker<String, String> s = new SwingWorker<String, String>() {

                    @Override
                    protected String doInBackground() throws Exception {
                        Benchmark bench = new Benchmark();
                        String s = "";
                        s += "Base de données : " + bench.testDB();
                        final String s1 = s;
                        SwingUtilities.invokeLater(new Runnable() {

                            @Override
                            public void run() {
                                l.setText(s1);

                            }
                        });
                        s += " - Processeur : " + bench.testCPU();
                        final String s2 = s;
                        SwingUtilities.invokeLater(new Runnable() {

                            @Override
                            public void run() {
                                l.setText(s2);

                            }
                        });
                        s += " - Disque dur : " + bench.testWriteHD();
                        return s;
                    }

                    protected void done() {
                        b.setEnabled(true);
                        try {
                            String result = get();
                            l.setText(result);
                            r.setMode(ReloadPanel.MODE_EMPTY);
                        } catch (Exception e) {
                            r.setMode(ReloadPanel.MODE_BLINK);
                            e.printStackTrace();
                        }
                    };
                };
                s.execute();

            }
        });
        return p;
    }
}
