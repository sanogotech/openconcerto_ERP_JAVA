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
 
 package org.openconcerto.erp.generationDoc;

import org.openconcerto.ui.DefaultGridBagConstraints;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.print.PrinterJob;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;

public class ProgressPrintingFrame extends JFrame {
    final JProgressBar progress = new JProgressBar(0, 100);
    final JLabel label;
    final JButton cancel = new JButton("Annluer");
    private boolean cancelled;

    public ProgressPrintingFrame(Window parent, final PrinterJob printJob, String title, String message, int minWidth) {
        final JPanel p = new JPanel();
        p.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();
        label = new JLabel(message, SwingConstants.LEFT);
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        p.add(label, c);
        c.gridy++;
        p.add(progress, c);
        c.weightx = 1;
        c.fill = GridBagConstraints.NONE;
        c.gridy++;
        c.anchor = GridBagConstraints.SOUTHEAST;
        p.add(cancel, c);
        this.setContentPane(p);
        this.setTitle(title);
        this.pack();
        this.setResizable(false);
        if (p.getWidth() < minWidth) {
            this.setSize(minWidth, this.getHeight());
        }
        cancel.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                cancelled = true;
                printJob.cancel();
                dispose();
            }
        });
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setAlwaysOnTop(true);

    }

    public void setProgress(int percent) {
        this.progress.setValue(percent);
        if (percent >= 100) {
            this.dispose();
        }
    }

    public void setMessage(String m) {
        this.label.setText(m);
    }

    public boolean isCancelled() {
        return cancelled;
    }
}
