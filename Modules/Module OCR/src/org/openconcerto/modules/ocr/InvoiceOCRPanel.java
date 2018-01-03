package org.openconcerto.modules.ocr;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;

import org.openconcerto.ui.ReloadPanel;

public class InvoiceOCRPanel extends JPanel {

    private final InvoiceOCRTable table;
    private final InvoiceViewer viewer = new InvoiceViewer();
    private final JButton bCancel = new JButton("Annuler");
    private final JButton bInterrupt = new JButton("Interrompre");
    private final JButton bSave = new JButton("Valider");
    private final JButton bImprove = new JButton("Améliorer les textes");
    private final JButton bRealodFirst = new JButton("Recharger les fichiers");
    private OCRThread ocrThread;
    private SaveThread saveThread;
    private String execDirectory;
    private List<File> files;

    private class InvoiceOCREvent implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getSource().equals(getBSave())) {
                startSave();
            } else if (e.getSource().equals(getBInterrupt())) {
                interrupt();
            } else if (e.getSource().equals(getBImprove())) {
                startOCR(true);
            } else if (e.getSource().equals(getBRelaodFirst())) {
                interrupt();
            }
        }

        private void interrupt() {
            if (getOCRThread() != null && getOCRThread().isAlive()) {
                getOCRThread().interrupt();
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        getTable().comp.setMode(ReloadPanel.MODE_EMPTY);
                    }
                });
            }
        }
    }

    private void startSave() {
        this.saveThread = new SaveThread(this.table, this.bSave);
        this.saveThread.setDaemon(true);
        this.saveThread.setPriority(Thread.MIN_PRIORITY);
        getBSave().setEnabled(false);
        getBImprove().setEnabled(false);
        getBRelaodFirst().setEnabled(false);
        this.saveThread.start();

        new Thread(new Runnable() {
            public void run() {
                try {
                    getOCRThread().join();
                } catch (Exception ex) {
                    ;
                } finally {
                    getBSave().setEnabled(true);
                    getBImprove().setEnabled(true);
                    getBRelaodFirst().setEnabled(true);
                }
            }
        }).start();
    }

    public void startOCR(boolean textCleanning) {
        this.ocrThread.setTextCleanning(textCleanning);
        this.ocrThread.setDaemon(true);
        this.ocrThread.setPriority(Thread.MIN_PRIORITY);
        getBSave().setEnabled(false);
        getBImprove().setEnabled(false);
        getBRelaodFirst().setEnabled(false);
        getBInterrupt().setEnabled(true);

        final Thread t = new Thread(this.ocrThread);
        t.start();

        new Thread(new Runnable() {
            public void run() {
                try {
                    t.join();
                } catch (Exception ex) {
                    ;
                } finally {
                    getBSave().setEnabled(true);
                    getBImprove().setEnabled(true);
                    getBRelaodFirst().setEnabled(true);
                    getBInterrupt().setEnabled(false);
                    getOCRThread().interrupt();
                }
            }
        }).start();
    }

    public InvoiceOCRPanel(String execDirectory, List<File> files) {
        this.execDirectory = execDirectory;
        this.files = files;
        this.table = new InvoiceOCRTable(this.viewer);
        this.ocrThread = new OCRThread(this.files, this.table, this.viewer, this, this.execDirectory);

        final JSplitPane split = new JSplitPane();
        split.setLeftComponent(this.viewer);
        split.setRightComponent(this.table);
        split.setDividerLocation(500);

        this.bSave.addActionListener(new InvoiceOCREvent());
        this.bInterrupt.addActionListener(new InvoiceOCREvent());
        this.bImprove.addActionListener(new InvoiceOCREvent());
        this.bRealodFirst.addActionListener(new InvoiceOCREvent());

        GridBagLayout gb = new GridBagLayout();
        GridBagConstraints gbc = new GridBagConstraints();
        this.setLayout(gb);

        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.gridheight = 1;
        this.add(split, gbc);

        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0;
        gbc.weighty = 0;
        this.add(this.bInterrupt, gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        this.add(this.bSave, gbc);

        gbc.gridx = 2;
        this.add(this.bImprove, gbc);

        gbc.gridx = 3;
        this.add(this.bRealodFirst, gbc);
    }

    protected JButton getBSave() {
        return this.bSave;
    }

    protected JButton getBInterrupt() {
        return this.bInterrupt;
    }

    protected JButton getBCancel() {
        return this.bCancel;
    }

    protected JButton getBImprove() {
        return this.bImprove;
    }

    protected JButton getBRelaodFirst() {
        return this.bRealodFirst;
    }

    protected OCRThread getOCRThread() {
        return this.ocrThread;
    }

    protected InvoiceOCRTable getTable() {
        return this.table;
    }
}
