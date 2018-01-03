package org.openconcerto.modules.ocr.scan;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingWorker;

import org.openconcerto.modules.ocr.InvoiceOCRPanel;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.JLabelBold;
import org.openconcerto.ui.ReloadPanel;
import org.openconcerto.utils.ExceptionHandler;

public class ScannerPanel extends JPanel implements ActionListener {
    private final JRadioButton rbDirectory = new JRadioButton("Dossier images");
    private final JRadioButton rbPDF = new JRadioButton("Dossier PDFs");
    private final JRadioButton rbScan = new JRadioButton("Scanner");

    private final ButtonGroup bgRadio = new ButtonGroup();

    private final JButton bValid = new JButton("Valider");
    private final ReloadPanel rpLoader = new ReloadPanel();
    private final JLabel pathLabel = new JLabel();
    private String imagePath = "";

    public ScannerPanel(String path) {

        this.imagePath = path;

        final GridBagConstraints c = new DefaultGridBagConstraints();
        this.setLayout(new GridBagLayout());

        c.gridwidth = 2;
        c.gridheight = 1;
        c.weightx = 0;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;

        JLabelBold l = new JLabelBold("Dossier de travail");

        this.add(l, c);
        c.gridy++;

        updateLabelPath();
        pathLabel.setMinimumSize(new Dimension(400, 16));
        this.add(pathLabel, c);
        c.gridy++;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        final JButton bFile = new JButton("Sélectionner un dossier");
        bFile.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                final JFileChooser chooser = new JFileChooser();
                chooser.setCurrentDirectory(new File(imagePath));
                chooser.setDialogTitle("Dossier");
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

                chooser.setAcceptAllFileFilterUsed(false);
                //
                if (chooser.showOpenDialog(ScannerPanel.this) == JFileChooser.APPROVE_OPTION) {
                    final File selectedFile = chooser.getSelectedFile();
                    if (selectedFile != null) {
                        imagePath = selectedFile.getAbsolutePath();
                        updateLabelPath();
                    }
                }

            }
        });
        this.add(bFile, c);
        c.gridwidth = 2;
        c.gridy++;
        this.add(new JLabelBold("Mode de traitement"), c);

        c.weightx = 1;
        c.weighty++;
        c.gridx = 0;
        c.gridy++;
        c.anchor = GridBagConstraints.NORTHWEST;
        this.add(createCenterPanel(), c);
        c.gridwidth = 1;
        c.weightx = 0;
        c.weighty = 0;
        c.gridx = 1;
        c.gridy++;
        c.anchor = GridBagConstraints.LINE_END;

        this.add(this.rpLoader, c);
        c.gridx++;
        this.add(this.bValid, c);

        this.bValid.addActionListener(this);

    }

    private void updateLabelPath() {
        this.pathLabel.setText(new File(getImagePath()).getAbsolutePath());

    }

    public JPanel createCenterPanel() {
        final JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new GridBagLayout());
        this.bgRadio.add(this.rbDirectory);
        this.bgRadio.add(this.rbPDF);
        this.bgRadio.add(this.rbScan);

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 1;
        centerPanel.add(this.rbDirectory, c);
        centerPanel.add(this.rbPDF, c);
        centerPanel.add(this.rbScan, c);
        this.rbDirectory.setSelected(true);
        return centerPanel;
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        if (e.getSource().equals(this.bValid)) {
            this.rpLoader.setMode(ReloadPanel.MODE_ROTATE);
            this.bValid.setEnabled(false);
            SwingWorker<List<File>, Object> worker = new SwingWorker<List<File>, Object>() {

                @Override
                protected List<File> doInBackground() throws Exception {
                    final File imageScanDirectory = new File(getImagePath());
                    final File imageTIFFDirectory = new File(imageScanDirectory, "TIFF");
                    final File imagePDFDirectory = new File(imageScanDirectory, "PDF");
                    final File imagePNGDirectory = new File(imageScanDirectory, "PNG");
                    final File imageHOCRDirectory = new File(imageScanDirectory, "HOCR");
                    List<File> filesTIFF, filesPNG = null, filesPDF;
                    // Creation of necessary directories
                    if (((!imageTIFFDirectory.exists() && imageTIFFDirectory.mkdirs()) || imageTIFFDirectory.exists())
                            && ((!imagePDFDirectory.exists() && imagePDFDirectory.mkdirs()) || imagePDFDirectory.exists())
                            && ((!imagePNGDirectory.exists() && imagePNGDirectory.mkdirs()) || imagePNGDirectory.exists())
                            && ((!imageHOCRDirectory.exists() && imageHOCRDirectory.mkdirs()) || imageHOCRDirectory.exists())) {
                        // Get file for OCR
                        if (directoryIsCheck()) {
                            filesPNG = ScannerUtil.scanDirectory(imagePNGDirectory);
                        } else if (pdfIsCheck()) {
                            filesPDF = ScannerUtil.scanDirectory(imagePDFDirectory);
                            try {
                                for (File filePDF : filesPDF) {
                                    final String filePDFName = filePDF.getName();
                                    File filePNG;
                                    String filePNGName = "";

                                    if (filePDFName.length() > 3 && filePDFName.substring(filePDFName.length() - 3, filePDFName.length()).toLowerCase().equals("pdf")) {
                                        filePNGName = filePDFName.substring(0, filePDFName.length() - 3) + "png";
                                        filePNG = new File(imagePNGDirectory, filePNGName);
                                        ScannerUtil.convertFile(filePDF, filePNG);
                                    }
                                }
                            } catch (Exception ex) {
                                throw ex;
                            }
                        } else if (scanIsCheck()) {
                            try {
                                if (ScannerUtil.scanTo(imageTIFFDirectory)) {
                                    filesTIFF = ScannerUtil.scanDirectory(imageTIFFDirectory);
                                    for (File fileTIFF : filesTIFF) {
                                        File filePNG;
                                        final String fileTIFFName = fileTIFF.getName();
                                        String filePNGName = "";

                                        if (fileTIFFName.toLowerCase().endsWith(".tiff")) {
                                            filePNGName = fileTIFFName.substring(0, fileTIFFName.length() - 4) + "png";
                                            filePNG = new File(imagePNGDirectory, filePNGName);
                                            ScannerUtil.convertFile(fileTIFF, filePNG);
                                        }
                                    }
                                    filesPNG = ScannerUtil.scanDirectory(imagePNGDirectory);
                                    for (File file : imageTIFFDirectory.listFiles()) {
                                        if (!file.isDirectory()) {
                                            file.delete();
                                        }
                                    }
                                }
                            } catch (Exception ex) {
                                throw ex;
                            }
                        }

                    }
                    return filesPNG;
                }

                @Override
                protected void done() {
                    try {
                        List<File> result = get();

                        if (result != null) {
                            int choice = JOptionPane.showConfirmDialog(null, "Les fichiers sont prêts à être lus, voulez-vous continuer?", "Confirmation", JOptionPane.YES_NO_OPTION);
                            if (choice == JOptionPane.YES_OPTION) {
                                final JFrame frame = new JFrame("Lecture OCR");
                                final InvoiceOCRPanel panel = new InvoiceOCRPanel(getImagePath(), result);
                                frame.setSize(1024, 768);
                                frame.setContentPane(panel);
                                frame.setVisible(true);
                                panel.startOCR(false);
                            }
                            getRpLoader().setMode(ReloadPanel.MODE_EMPTY);
                            getBValid().setEnabled(true);
                        } else {
                            JOptionPane.showMessageDialog(ScannerPanel.this, "Aucun fichier trouvé.\nLe dossier de travail doit contenir des fichiers dans les sous-dossiers PNG, TIFF, PDF ou HOCR.");
                            getRpLoader().setMode(ReloadPanel.MODE_EMPTY);
                            getBValid().setEnabled(true);
                        }

                    } catch (Exception ex) {
                        ExceptionHandler.handle("Erreur OCR", ex);
                        getRpLoader().setMode(ReloadPanel.MODE_BLINK);
                        getBValid().setEnabled(true);
                    }
                }
            };
            worker.execute();
        }
    }

    protected synchronized JButton getBValid() {
        return this.bValid;
    }

    protected synchronized ReloadPanel getRpLoader() {
        return this.rpLoader;
    }

    protected synchronized String getImagePath() {
        return this.imagePath;
    }

    protected synchronized boolean scanIsCheck() {
        return this.rbScan.isSelected();
    }

    protected synchronized boolean pdfIsCheck() {
        return this.rbPDF.isSelected();
    }

    protected synchronized boolean directoryIsCheck() {
        return this.rbDirectory.isSelected();
    }
}
