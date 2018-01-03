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

import org.openconcerto.erp.core.common.ui.ScrollablePanel;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLRowValuesListFetcher;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.SwingThreadUtils;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.FileUtils;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

public class AttachmentPanel extends JPanel {

    private final SQLRowAccessor rowSource;
    private List<ListDataListener> listeners = new ArrayList<ListDataListener>();

    public AttachmentPanel(SQLRowAccessor rowSource) {
        super();
        this.rowSource = rowSource;
        this.setLayout(new GridBagLayout());
        initUI();
        setFocusable(true);
    }

    public void addListener(ListDataListener l) {
        this.listeners.add(l);
    }

    public void removeListener(ListDataListener l) {
        this.listeners.remove(l);
    }

    public void fireDataChanged() {
        for (ListDataListener listDataListener : listeners) {
            listDataListener.contentsChanged(new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 0, 0));
        }
    }

    public void initUI() {
        this.invalidate();
        this.removeAll();
        GridBagConstraints c = new DefaultGridBagConstraints();

        // Recupération de la liste des fichiers

        // TODO requete dans un SwingWorker
        final SQLTable tableAttachment = rowSource.getTable().getTable("ATTACHMENT");
        SQLRowValues rowVals = new SQLRowValues(tableAttachment);
        rowVals.putNulls(tableAttachment.getFieldsName());

        SQLRowValuesListFetcher fetcher = SQLRowValuesListFetcher.create(rowVals);
        Where where = new Where(tableAttachment.getField("SOURCE_TABLE"), "=", this.rowSource.getTable().getName());
        where = where.and(new Where(tableAttachment.getField("SOURCE_ID"), "=", this.rowSource.getID()));
        // TODO en premier les dossier, puis trier par nom
        List<SQLRowValues> attachments = fetcher.fetch(where);

        // AJout d'un fichier
        final JButton addButton = new JButton("Ajouter un fichier");
        this.add(addButton, c);

        c.gridx++;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        final JProgressBar progressBar = new JProgressBar(0, 100);
        progressBar.setValue(100);
        progressBar.setStringPainted(true);
        progressBar.setVisible(false);
        this.add(progressBar, c);
        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.gridy++;

        addButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                final Frame frame = SwingThreadUtils.getAncestorOrSelf(Frame.class, (Component) e.getSource());

                final FileDialog fd = new FileDialog(frame, "Ajouter un fichier", FileDialog.LOAD);
                fd.setVisible(true);
                final String fileName = fd.getFile();
                if (fileName != null) {
                    File inFile = new File(fd.getDirectory(), fileName);
                    AttachmentUtils utils = new AttachmentUtils();
                    utils.uploadFile(inFile, rowSource);
                    initUI();
                }
            }
        });

        ScrollablePanel files = new ScrollablePanel() {
            @Override
            public Dimension getPreferredSize() {
                int w = getSize().width;
                int nbPerRow = (w - 5) / (FilePanel.WIDTH + 5);
                if (nbPerRow < 1) {
                    nbPerRow = 1;
                }
                int nbRow = 1 + (getComponentCount() / nbPerRow);
                if (nbRow < 1) {
                    nbRow = 1;
                }
                return new Dimension(w, 5 + nbRow * (FilePanel.HEIGHT + 5));
            }

        };
        files.setOpaque(true);
        files.setBackground(Color.WHITE);
        files.setScrollableWidth(ScrollablePanel.ScrollableSizeHint.FIT);
        files.setScrollableHeight(ScrollablePanel.ScrollableSizeHint.NONE);
        files.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
        // Liste des fichiers
        for (final SQLRowValues sqlRowValues : attachments) {

            final FilePanel filePanel = new FilePanel(sqlRowValues, this);
            filePanel.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        Thread t = new Thread() {
                            @Override
                            public void run() {
                                AttachmentUtils utils = new AttachmentUtils();
                                File f = utils.getFile(sqlRowValues);
                                if (f == null) {
                                    SwingUtilities.invokeLater(new Runnable() {
                                        public void run() {
                                            JOptionPane.showMessageDialog(null, "Impossible de récupérer le fichier.", "Erreur", JOptionPane.ERROR_MESSAGE);
                                        }
                                    });
                                    System.err.println("Impossible de récupérer le fichier.");
                                } else {
                                    try {
                                        FileUtils.openFile(f);
                                    } catch (IOException e1) {
                                        ExceptionHandler.handle("Erreur lors de l'ouverture du fichier.", e1);
                                    }
                                }
                            }
                        };
                        t.start();
                    }
                }
            });
            files.add(filePanel);

        }
        c.gridwidth = 2;
        c.gridx = 0;
        c.gridy++;
        c.weightx = 1;
        c.weighty = 1;
        JScrollPane scroll = new JScrollPane(files);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scroll.setMinimumSize(new Dimension((int) (400 * 1.618), 400));
        scroll.setPreferredSize(new Dimension((int) (400 * 1.618), 400));
        scroll.setBackground(Color.WHITE);
        scroll.getViewport().setBackground(Color.WHITE);
        this.add(scroll, c);

        this.validate();
        this.repaint();

        DropTarget dt = new DropTarget() {
            @Override
            public synchronized void drop(DropTargetDropEvent dtde) {
                dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
                Transferable t = dtde.getTransferable();
                try {

                    if (t.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        @SuppressWarnings("unchecked")
                        List<File> fileList = (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);
                        // TODO faire en arriere plan, mettre une jauge à droite du bouton ajouter
                        // et mettre un bouton d'annulation
                        AttachmentUtils utils = new AttachmentUtils();
                        boolean cancelledByUser = false;
                        for (File f : fileList) {
                            if (cancelledByUser) {
                                break;
                            }
                            utils.uploadFile(f, rowSource);
                        }
                        initUI();
                    }
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        };
        files.setDropTarget(dt);
        scroll.getViewport().setDropTarget(dt);
        fireDataChanged();
    }

}
