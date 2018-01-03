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
 
 package org.openconcerto.erp.model;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.generationDoc.A4;
import org.openconcerto.erp.generationDoc.AbstractSheetXml;
import org.openconcerto.erp.generationDoc.ProgressPrintingFrame;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.view.list.IListe;
import org.openconcerto.sql.view.list.RowAction;
import org.openconcerto.ui.EmailComposer;
import org.openconcerto.utils.ExceptionHandler;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.print.Paper;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.print.PrintService;
import javax.print.attribute.Attribute;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.Size2DSyntax;
import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.MediaPrintableArea;
import javax.print.attribute.standard.MediaSizeName;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class MouseSheetXmlListeListener {

    private Class<? extends AbstractSheetXml> clazz;
    protected IListe liste;

    private boolean previewIsVisible = true;
    private boolean showIsVisible = true;
    private boolean printIsVisible = true;
    private boolean generateIsVisible = true;
    private boolean previewHeader = false;
    private boolean showHeader = false;
    private boolean generateHeader = false;

    public MouseSheetXmlListeListener(Class<? extends AbstractSheetXml> clazz) {
        this(clazz, true, true, true, true);

    }

    public MouseSheetXmlListeListener(Class<? extends AbstractSheetXml> clazz, boolean show, boolean preview, boolean print, boolean generate) {
        this.clazz = clazz;
        this.printIsVisible = print;
        this.previewIsVisible = preview;
        this.showIsVisible = show;
        this.generateIsVisible = generate;
    }

    protected Class<? extends AbstractSheetXml> getSheetClass() {
        return this.clazz;
    }

    protected AbstractSheetXml createAbstractSheet(SQLRow row) {
        try {
            Constructor<? extends AbstractSheetXml> ctor = getSheetClass().getConstructor(SQLRow.class);
            AbstractSheetXml sheet = ctor.newInstance(row);
            return sheet;
        } catch (Exception e) {
            ExceptionHandler.handle("sheet creation error", e);
        }
        return null;
    }

    public List<AbstractSheetXml> createAbstractSheets(List<SQLRow> rows) {
        final List<AbstractSheetXml> sheets = new ArrayList<AbstractSheetXml>(rows.size());
        try {
            final Constructor<? extends AbstractSheetXml> ctor = getSheetClass().getConstructor(SQLRow.class);
            for (SQLRow row : rows) {
                AbstractSheetXml sheet = ctor.newInstance(row);
                sheets.add(sheet);
            }
        } catch (Exception e) {
            ExceptionHandler.handle("sheet creation error", e);
        }
        return sheets;
    }

    protected String getMailObject(SQLRow row) {
        return "";
    }

    public void setPreviewHeader(boolean previewHeader) {
        this.previewHeader = previewHeader;
    }

    public void setGenerateHeader(boolean generateHeader) {
        this.generateHeader = generateHeader;
    }

    public void setShowHeader(boolean showHeader) {
        this.showHeader = showHeader;
    }

    protected void sendMail(final AbstractSheetXml sheet, final boolean readOnly) {
        List<AbstractSheetXml> l = new ArrayList<AbstractSheetXml>(1);
        l.add(sheet);
        sendMail(l, readOnly);
    }

    protected void sendMail(final List<AbstractSheetXml> sheets, final boolean readOnly) {
        String mail = "";

        for (AbstractSheetXml sheet : sheets) {
            final SQLRow row = sheet.getSQLRow();
            Set<SQLField> setContact = null;
            SQLTable tableContact = Configuration.getInstance().getRoot().findTable("CONTACT");
            setContact = row.getTable().getForeignKeys(tableContact);

            Set<SQLField> setClient = null;
            SQLTable tableClient = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete().getTable("CLIENT");
            setClient = row.getTable().getForeignKeys(tableClient);

            for (SQLField field : setContact) {
                if (mail == null || mail.trim().length() == 0) {
                    mail = row.getForeignRow(field.getName()).getString("EMAIL");
                }
            }

            if (setClient != null && (mail == null || mail.trim().length() == 0)) {
                    for (SQLField field : setClient) {
                        SQLRow rowCli = row.getForeignRow(field.getName());
                        if (mail == null || mail.trim().length() == 0) {
                            mail = rowCli.getString("MAIL");
                        }
                    }
            }

            if (mail == null || mail.trim().length() == 0) {
                SQLTable tableF = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete().getTable("FOURNISSEUR");
                Set<SQLField> setF = null;
                setF = row.getTable().getForeignKeys(tableF);

                if (setF != null) {

                    for (SQLField field : setF) {
                        SQLRow rowF = row.getForeignRow(field.getName());
                        if (mail == null || mail.trim().length() == 0) {
                            mail = rowF.getString("MAIL");
                        }
                    }
                }

                if (mail == null || mail.trim().length() == 0) {
                    SQLBase base = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete();
                    if (base.containsTable("MONTEUR")) {
                        SQLTable tableM = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete().getTable("MONTEUR");
                        Set<SQLField> setM = null;
                        setM = row.getTable().getForeignKeys(tableM);
                        if (setM != null) {
                            for (SQLField field : setM) {
                                SQLRow rowM = row.getForeignRow(field.getName());
                                if (rowM.getForeignRow("ID_CONTACT_FOURNISSEUR") != null && !rowM.getForeignRow("ID_CONTACT_FOURNISSEUR").isUndefined()) {
                                    mail = rowM.getForeignRow("ID_CONTACT_FOURNISSEUR").getString("EMAIL");
                                }
                            }
                        }
                    }
                }
                if (mail == null || mail.trim().length() == 0) {
                    SQLBase base = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete();
                    if (base.containsTable("TRANSPORTEUR")) {

                        SQLTable tableM = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete().getTable("TRANSPORTEUR");
                        Set<SQLField> setM = null;
                        setM = row.getTable().getForeignKeys(tableM);

                        if (setM != null) {

                            for (SQLField field : setM) {
                                SQLRow rowM = row.getForeignRow(field.getName());
                                if (rowM.getForeignRow("ID_CONTACT_FOURNISSEUR") != null && !rowM.getForeignRow("ID_CONTACT_FOURNISSEUR").isUndefined()) {
                                    mail = rowM.getForeignRow("ID_CONTACT_FOURNISSEUR").getString("EMAIL");
                                }
                            }
                        }
                    }
                }
            }
        }
        final String adresseMail = mail;

        final String subject = sheets.get(0).getReference();

        if (readOnly) {
            final Thread t = new Thread() {
                @Override
                public void run() {
                    final List<File> files = new ArrayList<File>();
                    try {
                        for (AbstractSheetXml sheet : sheets) {
                            files.add(sheet.getOrCreatePDFDocumentFile(true).getAbsoluteFile());
                        }

                        SwingUtilities.invokeLater(new Runnable() {

                            @Override
                            public void run() {
                                try {
                                    EmailComposer.getInstance().compose(adresseMail, subject + (subject.trim().length() == 0 ? "" : ", ") + files.get(0).getName(),
                                            getMailObject(sheets.get(0).getSQLRow()), files.toArray(new File[files.size()]));
                                } catch (Exception e) {
                                    ExceptionHandler.handle("Impossible de charger le document PDF dans l'email!", e);
                                }
                            }
                        });
                    } catch (Exception e) {
                        ExceptionHandler.handle("Impossible de charger le document PDF", e);
                    }
                }
            };
            t.start();
        } else {
            try {
                final List<File> files = new ArrayList<File>();
                for (AbstractSheetXml sheet : sheets) {
                    files.add(sheet.getGeneratedFile().getAbsoluteFile());
                }
                EmailComposer.getInstance().compose(adresseMail, subject + (subject.trim().length() == 0 ? "" : ", ") + sheets.get(0).getGeneratedFile().getName(),
                        getMailObject(sheets.get(0).getSQLRow()), files.toArray(new File[files.size()]));
            } catch (Exception exn) {
                ExceptionHandler.handle(null, "Impossible de créer le courriel", exn);
            }
        }

    }

    public List<RowAction> addToMenu() {
        return null;
    }

    public List<RowAction> getRowActions() {
        List<RowAction> l = new ArrayList<RowAction>();

        if (!Boolean.getBoolean("org.openconcerto.oo.useODSViewer")) {
            if (this.showIsVisible) {
                RowAction action = new RowAction(new AbstractAction() {
                    public void actionPerformed(ActionEvent ev) {
                        System.err.println("");
                        createAbstractSheet(IListe.get(ev).fetchSelectedRow()).openDocument(false);
                    }

                }, this.previewHeader, "document.modify") {

                    @Override
                    public boolean enabledFor(IListeEvent evt) {

                        return evt.getSelectedRow() != null && (evt.getTotalRowCount() >= 1) && (createAbstractSheet(evt.getSelectedRow().asRow()).getGeneratedFile().exists());
                    }

                };
                l.add(action);

            }
        } else {
            if (this.previewIsVisible) {
                l.add(new RowAction(new AbstractAction() {
                    public void actionPerformed(ActionEvent ev) {
                        try {
                            createAbstractSheet(IListe.get(ev).fetchSelectedRow()).showPreviewDocument();
                        } catch (Exception e) {
                            ExceptionHandler.handle("Impossilbe d'ouvrir le fichier", e);
                        }
                    }

                }, this.previewHeader, "document.preview") {

                    @Override
                    public boolean enabledFor(IListeEvent evt) {
                        return evt.getSelectedRow() != null && evt.getTotalRowCount() >= 1 && createAbstractSheet(evt.getSelectedRow().asRow()).getGeneratedFile().exists();
                    }
                });

            }
        }

        // action supplémentaire
        List<RowAction> list = addToMenu();
        if (list != null) {
            for (RowAction rowAction : list) {
                l.add(rowAction);
            }
        }

        if (Boolean.getBoolean("org.openconcerto.oo.useODSViewer")) {

            if (this.showIsVisible) {
                l.add(new RowAction(new AbstractAction() {
                    public void actionPerformed(ActionEvent ev) {
                        createAbstractSheet(IListe.get(ev).fetchSelectedRow()).openDocument(false);
                    }
                }, this.showHeader, "document.modify") {

                    @Override
                    public boolean enabledFor(IListeEvent evt) {
                        return evt.getSelectedRow() != null && evt.getTotalRowCount() >= 1 && createAbstractSheet(evt.getSelectedRow().asRow()).getGeneratedFile().exists();
                    }
                });
            }
        }

        if (this.printIsVisible) {

            // Impression rapide : imprime le ou les documents sélectionnés (les génère si besoin)
            // en proposant l'interface Java d'impression
            l.add(new RowAction(new AbstractAction() {
                public void actionPerformed(final ActionEvent ev) {
                    //
                    final IListe ilist = IListe.get(ev);
                    final SQLRow asRow = IListe.get(ev).getSelectedRow().asRow();
                    AbstractSheetXml firstSheet = createAbstractSheet(asRow);
                    final String printerName = firstSheet.getPrinter();
                    // Printer configuration
                    final PrinterJob printJob = PrinterJob.getPrinterJob();

                    // Set the printer
                    PrintService myService = null;
                    if (printerName != null && printerName.trim().length() > 0) {
                        final PrintService[] services = PrinterJob.lookupPrintServices();
                        for (int i = 0; i < services.length; i++) {
                            if (services[i].getName().equals(printerName)) {
                                myService = services[i];
                                break;
                            }
                        }
                        if (myService != null) {
                            try {
                                System.err.println("MouseSheetXmlListeListener.getRowActions() set printer : " + printerName);
                                printJob.setPrintService(myService);
                            } catch (PrinterException e) {
                                e.printStackTrace();
                                JOptionPane.showMessageDialog(null, "Imprimante non compatible");
                                return;
                            }
                        }
                    }

                    final HashPrintRequestAttributeSet attributes = new HashPrintRequestAttributeSet();
                    // FIXME l'impression est forcée en A4, sur OpenSuse le format est en
                    // Letter par défaut alors que l'imprimante est en A4 dans le système
                    final MediaSizeName media = MediaSizeName.ISO_A4;
                    attributes.add(media);
                    Paper paper = new A4(0, 0);
                    double POINTS_PER_INCH = 72.0;
                    final MediaPrintableArea printableArea = new MediaPrintableArea((float) (paper.getImageableX() / POINTS_PER_INCH), (float) (paper.getImageableY() / POINTS_PER_INCH),
                            (float) (paper.getImageableWidth() / POINTS_PER_INCH), (float) (paper.getImageableHeight() / POINTS_PER_INCH), Size2DSyntax.INCH);
                    attributes.add(printableArea);
                    attributes.add(new Copies(2));
                    boolean okToPrint = true;
                    if (Boolean.getBoolean("org.openconcerto.oo.useODSViewer")) {
                        okToPrint = printJob.printDialog(attributes);
                        final Attribute attribute = attributes.get(Copies.class);
                        if (attribute != null) {
                            final Copies attributeCopies = (Copies) attribute;
                            final int value = attributeCopies.getValue();
                            printJob.setCopies(value);
                        } else {
                            printJob.setCopies(1);
                        }
                    } else {
                        printJob.setCopies(1);
                    }
                    if (okToPrint) {
                        Window w = SwingUtilities.getWindowAncestor(ilist);
                        final ProgressPrintingFrame pFrame = new ProgressPrintingFrame(w, printJob, "Impression", "Impression en cours", 300);
                        // Génération + impression
                        final List<SQLRowValues> rows = IListe.get(ev).getSelectedRows();
                        final Thread thread = new Thread() {
                            @Override
                            public void run() {
                                final int size = rows.size();
                                for (int i = 0; i < size; i++) {
                                    final int index = i;
                                    SwingUtilities.invokeLater(new Runnable() {

                                        @Override
                                        public void run() {
                                            pFrame.setMessage("Document " + (index + 1) + "/" + size);
                                            pFrame.setProgress((100 * (index + 1)) / size);
                                        }
                                    });
                                    if (!pFrame.isCancelled()) {
                                        SQLRowValues r = rows.get(i);
                                        AbstractSheetXml sheet = createAbstractSheet(r.asRow());
                                        sheet.printDocument(printJob);
                                    }
                                }
                            }
                        };
                        thread.setPriority(Thread.MIN_PRIORITY);
                        thread.setDaemon(true);
                        pFrame.setLocationRelativeTo(ilist);
                        pFrame.setVisible(true);
                        thread.start();

                    }

                }
            }, false, "document.print") {

                @Override
                public boolean enabledFor(IListeEvent evt) {
                    return evt.getSelectedRow() != null && evt.getSelectedRows().size() > 0;
                }
            });

        }

        if (this.showIsVisible) {

            l.add(new RowAction(new AbstractAction() {
                public void actionPerformed(ActionEvent ev) {

                    final List<SQLRowValues> selectedRows = IListe.get(ev).getSelectedRows();
                    final SQLTable table = IListe.get(ev).getSource().getPrimaryTable();
                    final List<SQLRow> rows = new ArrayList<SQLRow>();
                    for (SQLRowValues r : selectedRows) {
                        rows.add(table.getRow(r.getID()));
                    }
                    sendMail(createAbstractSheets(rows), true);
                }
            }, false, "document.pdf.send.email") {

                @Override
                public boolean enabledFor(IListeEvent evt) {
                    return evt.getSelectedRow() != null && evt.getTotalRowCount() >= 1 && createAbstractSheet(evt.getSelectedRow().asRow()).getGeneratedFile().exists();
                }
            });

            l.add(new RowAction(new AbstractAction() {
                public void actionPerformed(ActionEvent ev) {
                    final List<SQLRowValues> selectedRows = IListe.get(ev).getSelectedRows();
                    final SQLTable table = IListe.get(ev).getSource().getPrimaryTable();
                    final List<SQLRow> rows = new ArrayList<SQLRow>();
                    for (SQLRowValues r : selectedRows) {
                        rows.add(table.getRow(r.getID()));
                    }
                    sendMail(createAbstractSheets(rows), false);
                }
            }, false, "document.send.email") {

                @Override
                public boolean enabledFor(IListeEvent evt) {
                    return evt.getSelectedRow() != null && evt.getTotalRowCount() >= 1 && createAbstractSheet(evt.getSelectedRow().asRow()).getGeneratedFile().exists();
                }
            });

        }
        if (this.generateIsVisible) {
            l.add(new RowAction(new AbstractAction() {
                public void actionPerformed(ActionEvent ev) {
                    List<SQLRowValues> l = IListe.get(ev).getSelectedRows();

                    if (l.size() == 1) {
                        createDocument(ev);
                    } else {
                        createDocuments(l);
                    }
                }
            }, this.generateHeader, "document.create") {

                @Override
                public boolean enabledFor(List<SQLRowValues> selection) {
                    return selection != null && selection.size() > 0;
                }

            });
        }

        return l;
    }

    private void createDocuments(List<? extends SQLRowAccessor> selection) {
        int a = JOptionPane.showConfirmDialog(null, "Voulez vous recréer l'ensemble des documents sélectionnés?", "Génération de documents", JOptionPane.YES_NO_OPTION);
        if (a == JOptionPane.YES_OPTION) {
            for (SQLRowAccessor sqlRowAccessor : selection) {
                final AbstractSheetXml sheet = createAbstractSheet(sqlRowAccessor.getTable().getRow(sqlRowAccessor.getID()));
                sheet.createDocumentAsynchronous();
                sheet.showPrintAndExportAsynchronous(false, false, true);
            }
        }
    }

    private void createDocument(ActionEvent ev) {
        final AbstractSheetXml sheet = createAbstractSheet(IListe.get(ev).fetchSelectedRow());
        if (sheet.getGeneratedFile().exists()) {
            int a = JOptionPane.showConfirmDialog(null, "Voulez vous remplacer le document existant?", "Génération de documents", JOptionPane.YES_NO_OPTION);
            if (a == JOptionPane.YES_OPTION) {
                sheet.createDocumentAsynchronous();
                sheet.showPrintAndExportAsynchronous(true, false, true);
                return;
            }
        }

        try {
            sheet.getOrCreateDocumentFile();
            sheet.showPrintAndExportAsynchronous(true, false, false);
        } catch (Exception exn) {
            // TODO Bloc catch auto-généré
            exn.printStackTrace();
        }

    }

    /**
     * Action sur le double clic
     * 
     * @return
     */
    public RowAction getDefaultRowAction() {
        return new RowAction(new AbstractAction() {
            public void actionPerformed(ActionEvent ev) {
                final AbstractSheetXml sheet = createAbstractSheet(IListe.get(ev).fetchSelectedRow().asRow());
                try {
                    sheet.getOrCreateDocumentFile();
                    sheet.showPrintAndExportAsynchronous(true, false, true);
                } catch (Exception exn) {
                    ExceptionHandler.handle("Une erreur est survenue lors de la création du document.", exn);
                }
            }
        }, false, false, "document.create") {

            @Override
            public boolean enabledFor(List<SQLRowValues> selection) {
                return selection != null && selection.size() == 1;
            }

            @Override
            public Action getDefaultAction(final IListeEvent evt) {
                return this.getAction();
            }
        };
    }
}
