package org.openconcerto.modules.ocr;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.openconcerto.modules.ocr.parser.ACTNInvoiceParser;
import org.openconcerto.modules.ocr.parser.AbstractInvoiceParser;
import org.openconcerto.modules.ocr.parser.AcadiaInvoiceParser;
import org.openconcerto.modules.ocr.parser.AmazonInvoiceParser;
import org.openconcerto.modules.ocr.parser.BNPInvoiceParser;
import org.openconcerto.modules.ocr.parser.EbayENInvoiceParser;
import org.openconcerto.modules.ocr.parser.GenericInvoiceParser;
import org.openconcerto.modules.ocr.parser.LaPosteInvoiceParser;
import org.openconcerto.modules.ocr.parser.OVHInvoiceParser;
import org.openconcerto.modules.ocr.parser.OrangeInvoiceParser;
import org.openconcerto.modules.ocr.parser.ParserUtils;
import org.openconcerto.modules.ocr.parser.TechDataInvoiceParser;
import org.openconcerto.ui.ReloadPanel;

public class OCRThread extends Thread {
    private List<File> files;
    private final InvoiceOCRTable table;
    private final InvoiceViewer viewer;
    private final JPanel parent;
    private final String execDirectory;
    private boolean textCleanning = false;
    List<InvoiceOCR> invalidInvoices = new ArrayList<InvoiceOCR>();

    public OCRThread(List<File> files, InvoiceOCRTable table, InvoiceViewer viewer, JPanel parent, String execDirectory) {
        this.files = files;
        this.table = table;
        this.viewer = viewer;
        this.parent = parent;
        this.execDirectory = execDirectory;
    }

    @Override
    public void run() {
        final List<AbstractInvoiceParser> parsers = initParser();
        final List<InvoiceOCR> invoices = new ArrayList<InvoiceOCR>();
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                emptyTable();
            }
        });
        if(this.textCleanning){
            textCleanning(this.invalidInvoices);
        }
        if (this.files != null) {
            int fileCount = this.files.size();
            for (int i = 0; i < fileCount; i++) {
                System.out.println("OCRThread.run(): " + i + "/" + fileCount);
                final File file = this.files.get(i);
                try {
                    InvoiceOCR invoice = parse(file, parsers);
                    if (invoice != null) {
                        invoice.setValid(false);
                        invoices.add(invoice);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            if (invoices != null) {
                final List<InvoiceOCR> res_invoices = attachInvoices(invoices);
                commitAll(res_invoices);
            }
        }
        this.textCleanning = false;
    }

    private InvoiceOCR parse(File file, final List<AbstractInvoiceParser> parsers) throws Exception {
        InvoiceOCR invoice = null;
        final String result = TesseractUtils.DoOcr(file);
        final HOCRParser p = new HOCRParser();
        final List<OCRLine> lines = p.parse(result);
        final OCRPage page = new OCRPage(file, lines);
        for (final AbstractInvoiceParser parser : parsers) {
            final boolean b = parser.parse(page);
            if (b) {
                invoice = parser.getInvoice();
                invoice.addPage(page);
                if (invoice.getActive()) {                   
                    SwingUtilities.invokeAndWait(new Runnable() {
                        @Override
                        public void run() {
                            commit(parser.getInvoice());
                        }
                    });
                    for (AbstractInvoiceParser pr : parsers) {
                        pr.reset();
                    }
                }
                break;
            }
        }
        return invoice;
    }

    private void commitAll(final List<InvoiceOCR> invoices) {
        final int invoicesCount = invoices.size();
        getInvalidInvoices().clear();
        
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                emptyTable();
            }
        });
        
        for (int i = 0; i < invoicesCount; i++) {
            final InvoiceOCR invoice = invoices.get(i);
            if (!invoice.getValid()) {
                getInvalidInvoices().add(invoice);
            }
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    commit(invoice);
                }
            });
        }
        
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                stopLoader();
            }
        });
    }

    private List<AbstractInvoiceParser> initParser() {
        List<AbstractInvoiceParser> parsers = new ArrayList<AbstractInvoiceParser>();
        parsers.add(new BNPInvoiceParser());
        parsers.add(new TechDataInvoiceParser());
        parsers.add(new ACTNInvoiceParser());
        parsers.add(new EbayENInvoiceParser());
        parsers.add(new OrangeInvoiceParser());
        parsers.add(new AmazonInvoiceParser());
        parsers.add(new OVHInvoiceParser());
        parsers.add(new LaPosteInvoiceParser());
        parsers.add(new AcadiaInvoiceParser());
        parsers.add(new GenericInvoiceParser());
        return parsers;
    }

    private List<InvoiceOCR> attachInvoices(List<InvoiceOCR> invoices) {
        InvoiceOCR tempInvoice1;
        InvoiceOCR tempInvoice2;
        int invoiceCount = invoices.size();
        for (int i = 0; i < invoiceCount; i++) {
            tempInvoice1 = invoices.get(i);
            for (int j = i + 1; j < invoiceCount; j++) {
                tempInvoice2 = invoices.get(j);
                if (tempInvoice1.getSupplierName() != null && tempInvoice2.getSupplierName() != null && tempInvoice1.getSupplierName().equals(tempInvoice2.getSupplierName())
                        && tempInvoice1.getActive() && tempInvoice2.getActive()) {
                    boolean isSameInvoice = false;
                    if (tempInvoice1.getInvoiceNumber() != null && tempInvoice2.getInvoiceNumber() != null){
                        if(tempInvoice1.getInvoiceNumber().equals(tempInvoice2.getInvoiceNumber())) {
                            isSameInvoice = true;
                        }
                    } else if (tempInvoice1.getDate() != null && tempInvoice2.getDate() != null && ParserUtils.compareDate(tempInvoice1.getDate(), tempInvoice2.getDate())) {
                        isSameInvoice = true;
                    }

                    if (isSameInvoice) {
                        tempInvoice1 = setInvoiceFromInvoice(tempInvoice1, tempInvoice2);
                        invoices.remove(j);
                        invoiceCount = invoices.size();
                    }
                }
            }
            tempInvoice1.setTaxId();
            if (!tempInvoice1.checkNullValue() || !tempInvoice1.checkAmounts() || tempInvoice1.getTaxId() == -1) {
                tempInvoice1.setValid(false);
            }
        }
        return invoices;
    }

    private InvoiceOCR setInvoiceFromInvoice(InvoiceOCR invoice1, InvoiceOCR invoice2) {
        if (invoice2.getInvoiceNumber() != null && invoice1.getInvoiceNumber() == null) {
            invoice1.setInvoiceNumber(invoice2.getInvoiceNumber());
        }
        if (invoice2.getAmount() != null && invoice1.getAmount() == null) {
            invoice1.setAmount(invoice2.getAmount());
        }
        if (invoice2.getTax() != null && invoice1.getTax() == null) {
            invoice1.setTax(invoice2.getTax());
        }
        if (invoice2.getAmountWithTax() != null && invoice1.getAmountWithTax() == null) {
            invoice1.setAmountWithTax(invoice2.getAmountWithTax());
        }
        if (invoice2.getPageCount() > 0) {
            List<OCRLine> tempHighlight;
            OCRPage tempPage;
            invoice1.addPage(invoice2.getPage(0));
            tempHighlight = invoice2.getHighlight(invoice2.getPage(0));
            tempPage = invoice1.getPage(invoice1.getPageCount() - 1);
            final int highlightCount = tempHighlight.size();
            for (int k = 0; k < highlightCount; k++) {
                invoice1.addHighlight(tempPage, tempHighlight.get(k));
            }
        }
        return invoice1;
    }

    private void textCleanning(List<InvoiceOCR> invalidInvoices) {
        try {
            final File improveDirectory = new File(this.execDirectory, "IMPROVE");
            if(improveDirectory.exists() || improveDirectory.mkdirs()){
                final int invalidInvoicesCount = invalidInvoices.size();
                for (int i = 0; i < invalidInvoicesCount; i++) {
                    System.out.println("OCRThread.textCleanning(): " + i + "/" + invalidInvoicesCount);
                    final InvoiceOCR invoice = invalidInvoices.get(i);
                    final int pageCount = invoice.getPageCount();
                    for (int j = 0; j < pageCount; j++) {
                        final OCRPage page = invoice.getPage(j);
                        File image = page.getFileImage();
                        final File destFile = new File(improveDirectory, image.getName());
                        if(sameFileInDirectory(image, improveDirectory)){
                            image = destFile;
                        }
                        
                        final List<String> commands = new ArrayList<String>();
                        commands.add("./textcleaner");
                        commands.add("-e");
                        commands.add("normalize");
                        commands.add("-s");
                        commands.add("2");
                        commands.add(image.getAbsolutePath());
                        commands.add(destFile.getAbsolutePath());
                        try {
                            TesseractUtils.runProcess(commands, new File(this.execDirectory));
                            page.setFileImage(destFile);
                        } catch (Exception ex) {
                            throw new Exception("Le script textcleanner n'est pas accessible à l'endroit: " + new File(this.execDirectory).getAbsolutePath());
                        }
                    }
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(getParent(), ex.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private boolean sameFileInDirectory(File file, File directory){
        final File[] files = directory.listFiles();
        final int filesCount = files.length; 
        for(int i = 0; i < filesCount; i++){
            if(files[i].getName().equals(file.getName())){
                return true;
            }
        }
        return false;
    }
    
    private JPanel getParent() {
        return this.parent;
    }

    private List<InvoiceOCR> getInvalidInvoices() {
        return this.invalidInvoices;
    }
    
    public void setTextCleanning(boolean textCleanning) {
        this.textCleanning = textCleanning;
    }
    
    private void commit(InvoiceOCR invoice) {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("Must be called in EDT");
        }
        this.table.add(invoice);
        this.viewer.add(invoice);
    }

    private void emptyTable() {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("Must be called in EDT");
        }
        this.table.removeAll();
        this.viewer.removeAll();
    }

    private void stopLoader() {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("Must be called in EDT");
        }
        this.table.comp.setMode(ReloadPanel.MODE_EMPTY);
    }
}
