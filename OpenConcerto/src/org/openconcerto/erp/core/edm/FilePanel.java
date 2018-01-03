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
 
 package org.openconcerto.erp.core.edm;

import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.JImage;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

public class FilePanel extends JPanel {
    JImage image = null;

    public static final int WIDTH = 128;
    public static final int HEIGHT = 80;
    JLabel label;

    public FilePanel(final SQLRowValues rowAttachment, final AttachmentPanel panelSource) {
        final String name = rowAttachment.getString("NAME");
        this.setOpaque(true);
        this.setLayout(new BorderLayout());
        try {
            String type = rowAttachment.getString("MIMETYPE");
            if (type == null || type.trim().isEmpty() || type.equals("application/octet-stream")) {
                image = new JImage(this.getClass().getResource("data-icon.png"));
            } else if (type.equals("application/msword")) {
                image = new JImage(this.getClass().getResource("doc-icon.png"));
            } else if (type.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) {
                image = new JImage(this.getClass().getResource("docx-icon.png"));
            } else if (type.equals("application/vnd.oasis.opendocument.text")) {
                image = new JImage(this.getClass().getResource("odt-icon.png"));
            } else if (type.equals("application/pdf")) {
                image = new JImage(this.getClass().getResource("pdf-icon.png"));
            } else if (type.equals("image/jpeg")) {
                image = new JImage(this.getClass().getResource("jpg-icon.png"));
            } else if (type.equals("image/png")) {
                image = new JImage(this.getClass().getResource("png-icon.png"));
            } else if (type.equals("application/vnd.oasis.opendocument.spreadsheet")) {
                image = new JImage(this.getClass().getResource("ods-icon.png"));
            } else if (type.equals("application/msexcel") || type.equals("application/vnd.ms-excel") || type.equals("application/xls")) {
                image = new JImage(this.getClass().getResource("xls-icon.png"));
            } else if (type.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) {
                image = new JImage(this.getClass().getResource("xlsx-icon.png"));
            } else {
                image = new JImage(this.getClass().getResource("data-icon.png"));
            }
            image.setOpaque(true);

            image.setCenterImage(true);
            this.add(image, BorderLayout.CENTER);
        } catch (Exception e) {
            e.printStackTrace();
        }
        setBackground(Color.WHITE);
        label = new JLabel(name, SwingConstants.CENTER);
        label.setOpaque(false);
        this.add(label, BorderLayout.SOUTH);

        this.setPreferredSize(new Dimension(WIDTH, HEIGHT));
        this.setMinimumSize(new Dimension(WIDTH, HEIGHT));
        this.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseExited(MouseEvent e) {
                setBackground(Color.WHITE);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                setBackground(new Color(230, 240, 255));
            }

        });
        final JPopupMenu menu = new JPopupMenu();
        final JMenuItem menuItemDelete = new JMenuItem("Supprimer");
        menuItemDelete.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {

                int value = JOptionPane.showConfirmDialog(FilePanel.this, "Voulez-vous vraiment supprimer ce fichier ?\n" + rowAttachment.getString("NAME") + "\nFichier orignal : "
                        + rowAttachment.getString("FILENAME") + "\nType : " + rowAttachment.getString("MIMETYPE"), "Supprimer le ficher", JOptionPane.YES_NO_OPTION);

                if (value == JOptionPane.YES_OPTION) {
                    AttachmentUtils utils = new AttachmentUtils();
                    try {
                        utils.deleteFile(rowAttachment);
                        panelSource.initUI();
                    } catch (Exception e1) {
                        ExceptionHandler.handle("Erreur lors de la suppression du fichier!", e1);
                    }
                }

            }
        });
        menu.add(menuItemDelete);
        final JMenuItem menuItemRename = new JMenuItem("Renommer");
        menuItemRename.addActionListener(new ActionListener() {

            final JTextField text = new JTextField(name);

            private void stopNameEditing() {
                FilePanel.this.invalidate();
                FilePanel.this.remove(text);
                FilePanel.this.add(label, BorderLayout.SOUTH);
                FilePanel.this.validate();
                FilePanel.this.repaint();
            }

            public void validText(final SQLRowValues rowAttachment, final String name, final JTextField text) {
                try {
                    String newName = text.getText();
                    if (newName.trim().isEmpty()) {
                        newName = name;
                    }
                    rowAttachment.put("NAME", newName).commit();
                    label.setText(newName);
                } catch (SQLException e1) {
                    ExceptionHandler.handle("Erreur lors du renommage du fichier!", e1);
                }
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                final String name = rowAttachment.getString("NAME");

                text.addKeyListener(new KeyAdapter() {
                    @Override
                    public void keyPressed(KeyEvent e) {
                        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                            validText(rowAttachment, name, text);
                            stopNameEditing();
                        } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                            stopNameEditing();
                        }
                    }

                });
                text.addFocusListener(new FocusListener() {

                    @Override
                    public void focusLost(FocusEvent e) {
                        validText(rowAttachment, name, text);
                        stopNameEditing();
                    }

                    @Override
                    public void focusGained(FocusEvent e) {
                    }

                });
                text.addMouseListener(new MouseAdapter() {

                    @Override
                    public void mouseExited(MouseEvent e) {
                        validText(rowAttachment, name, text);
                        stopNameEditing();
                    }

                });

                FilePanel.this.invalidate();
                FilePanel.this.remove(label);
                FilePanel.this.add(text, BorderLayout.SOUTH);
                FilePanel.this.validate();
                FilePanel.this.repaint();

                text.grabFocus();
                text.setSelectionStart(0);
                text.setSelectionEnd(name.length());
            }

        });
        menu.add(menuItemRename);
        menu.addSeparator();
        JMenuItem menuItemProperties = new JMenuItem("Propriétés");
        menuItemProperties.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                JFrame f = new JFrame();
                f.setTitle("Propriétés de " + rowAttachment.getString("NAME"));
                JPanel p = new JPanel();
                p.setLayout(new GridBagLayout());
                GridBagConstraints c = new DefaultGridBagConstraints();
                // Name
                c.weightx = 0;
                p.add(new JLabel("Nom : ", SwingConstants.RIGHT), c);
                c.gridx++;
                c.weightx = 1;
                p.add(new JLabel(rowAttachment.getString("NAME")), c);
                c.gridy++;
                // Type
                c.gridx = 0;
                c.weightx = 0;
                p.add(new JLabel("Type : ", SwingConstants.RIGHT), c);
                c.gridx++;
                c.weightx = 1;
                p.add(new JLabel(rowAttachment.getString("MIMETYPE")), c);
                c.gridy++;
                // FileName
                c.gridx = 0;
                c.weightx = 0;
                p.add(new JLabel("Fichier original : ", SwingConstants.RIGHT), c);
                c.gridx++;
                c.weightx = 1;
                p.add(new JLabel(rowAttachment.getString("FILENAME")), c);
                c.gridy++;
                // Size
                c.gridx = 0;
                c.weightx = 0;
                p.add(new JLabel("Taille : ", SwingConstants.RIGHT), c);
                c.gridx++;
                c.weightx = 1;
                p.add(new JLabel(rowAttachment.getInt("FILESIZE") + " octets"), c);

                // Spacer
                c.gridx = 1;
                c.gridy++;
                c.weightx = 1;
                JPanel spacer = new JPanel();
                spacer.setPreferredSize(new Dimension(300, 1));
                p.add(spacer, c);
                f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

                f.setContentPane(p);
                f.pack();
                f.setResizable(false);
                f.setLocationRelativeTo(FilePanel.this);
                f.setVisible(true);

            }
        });
        menu.add(menuItemProperties);
        setComponentPopupMenu(menu);
    }

    @Override
    public void setBackground(Color bg) {
        super.setBackground(bg);
        if (image != null) {
            image.setBackground(bg);
        }
    }
}
