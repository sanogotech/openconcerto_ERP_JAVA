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
 
 package org.openconcerto.erp.preferences;

import org.openconcerto.erp.core.edm.AttachmentSQLElement;
import org.openconcerto.erp.generationDoc.DocumentLocalStorageManager;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.preferences.DefaultPreferencePanel;
import org.openconcerto.utils.FileUtils;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class AttachmentPreferencePanel extends DefaultPreferencePanel {

    private JTextField textTemplate;
    private JFileChooser fileChooser = null;

    public AttachmentPreferencePanel() {
        this.setLayout(new GridBagLayout());
        final GridBagConstraints cPanel = new DefaultGridBagConstraints();
        cPanel.weighty = 0;
        cPanel.anchor = GridBagConstraints.WEST;
        /*******************************************************************************************
         * Emplacement
         ******************************************************************************************/
        this.add(new JLabel("Destination des documents GED"), cPanel);
        cPanel.gridx++;
        cPanel.weightx = 1;
        this.textTemplate = new JTextField();
        this.add(this.textTemplate, cPanel);

        final JButton buttonTemplate = new JButton("...");
        cPanel.gridx++;
        cPanel.weightx = 0;
        cPanel.fill = GridBagConstraints.NONE;
        this.add(buttonTemplate, cPanel);

        buttonTemplate.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                directoryChoose();
            }
        });

        setValues();
    }

    public void storeValues() {

        final File z = new File(".");
        final File f = new File(this.textTemplate.getText());
        try {
            TemplateNXProps.getInstance().setProperty(AttachmentSQLElement.DIRECTORY_PREFS, FileUtils.relative(z, f));
            DocumentLocalStorageManager.getInstance().addDocumentDirectory(AttachmentSQLElement.DIRECTORY_PREFS, new File(FileUtils.relative(z, f)));
        } catch (IOException e) {
            e.printStackTrace();
        }
        TemplateNXProps.getInstance().store();
    }

    public void restoreToDefaults() {

    }

    public String getTitleName() {
        return "Emplacement des modèles";
    }

    private void setValues() {
        try {
            final File f = DocumentLocalStorageManager.getInstance().getDocumentOutputDirectory(AttachmentSQLElement.DIRECTORY_PREFS);
            if (f.exists()) {
                this.textTemplate.setForeground(UIManager.getColor("TextField.foreground"));
            } else {
                this.textTemplate.setForeground(Color.RED);
            }
            this.textTemplate.setText(f.getCanonicalPath());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void directoryChoose() {

        if (this.fileChooser == null) {
            this.fileChooser = new JFileChooser();
            this.fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        }
        this.fileChooser.setCurrentDirectory(new File(AttachmentPreferencePanel.this.textTemplate.getText()));
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {

                if (AttachmentPreferencePanel.this.fileChooser.showDialog(AttachmentPreferencePanel.this, "Sélectionner") == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = AttachmentPreferencePanel.this.fileChooser.getSelectedFile();
                    if (selectedFile.exists()) {
                        AttachmentPreferencePanel.this.textTemplate.setForeground(UIManager.getColor("TextField.foreground"));
                    } else {
                        AttachmentPreferencePanel.this.textTemplate.setForeground(Color.RED);
                    }
                    AttachmentPreferencePanel.this.textTemplate.setText(selectedFile.getPath());
                }
            }
        });
    }

}
