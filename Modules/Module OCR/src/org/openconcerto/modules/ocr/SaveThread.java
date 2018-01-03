package org.openconcerto.modules.ocr;

import java.io.File;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.SwingUtilities;

import org.openconcerto.erp.core.finance.accounting.element.ComptePCESQLElement;
import org.openconcerto.erp.generationEcritures.GenerationMvtSaisieAchat;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLBackgroundTableCache;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowListRSH;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.ui.ReloadPanel;

public class SaveThread extends Thread {
    private InvoiceOCRTable invoiceTable;
    private JButton bSave;

    public SaveThread(InvoiceOCRTable table, JButton bSave) {
        this.invoiceTable = table;
        this.bSave = bSave;
    }

    @Override
    public void run() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                getBSave().setEnabled(false);
                getInvoiceTable().comp.setMode(ReloadPanel.MODE_ROTATE);
            }
        });
        final Map<String, SQLRow> supplierMap = new HashMap<String, SQLRow>();
        final SQLTable purchaseTable = Configuration.getInstance().getDirectory().getElement("SAISIE_ACHAT").getTable();
        final SQLTable supplierTable = purchaseTable.getForeignTable("ID_FOURNISSEUR");
        final SQLSelect sel = new SQLSelect();
        sel.addSelectStar(supplierTable);
        final List<SQLRow> result = SQLRowListRSH.execute(sel);
        for (SQLRow sqlRow : result) {
            supplierMap.put(sqlRow.getString("NOM"), sqlRow);
        }
        final int invoiceCount = this.invoiceTable.dm.getRowCount();
        for (int i = 1; i <= invoiceCount; i++) {
            final InvoiceOCR invoice = this.invoiceTable.getInvoice(i - 1);
            if (invoice.getValid()) {
                final SQLRow supplierRow = supplierMap.get(invoice.getSupplierName());
                if (supplierRow != null) {
                    final int idCompteAchat = getIdCompteAchat(supplierRow, purchaseTable);
                    // Get payment mode
                    final SQLRow r = supplierRow.getForeignRow("ID_MODE_REGLEMENT");
                    final SQLElement eltModeReglement = Configuration.getInstance().getDirectory().getElement("MODE_REGLEMENT");
                    final SQLRowValues rowValsMdr = eltModeReglement.createCopy(r.getID());
                    final SQLRowValues rowVals = new SQLRowValues(purchaseTable);
                    rowVals.put("ID_FOURNISSEUR", supplierRow.getID());
                    rowVals.put("MONTANT_TTC", invoice.getAmountWithTax().multiply(new BigDecimal(100)).longValue());
                    rowVals.put("MONTANT_HT", invoice.getAmount().multiply(new BigDecimal(100)).longValue());
                    rowVals.put("MONTANT_TVA", invoice.getTax().multiply(new BigDecimal(100)).longValue());
                    rowVals.put("ID_TAXE", invoice.getTaxId());
                    rowVals.put("ID_MODE_REGLEMENT", rowValsMdr);
                    rowVals.put("ID_COMPTE_PCE", idCompteAchat);
                    rowVals.put("NUMERO_FACTURE", invoice.getInvoiceNumber());
                    rowVals.put("DATE", invoice.getDate());
                    try {
                        final SQLRow row = rowVals.insert();
                        final GenerationMvtSaisieAchat g = new GenerationMvtSaisieAchat(row);
                        g.genereMouvement();
                        for (int j = 0; j < invoice.getPageCount(); j++) {
                            final File pngFile = invoice.getPage(j).getFileImage();
                            final File pngFileDirectory = pngFile.getParentFile();
                            final File archivePngFileDirectory = new File(pngFileDirectory, "Save");
                            final File hocrFileDirectory = new File(pngFileDirectory.getParentFile(), "HOCR");
                            final File archiveHocrFileDirectory = new File(hocrFileDirectory, "Save");
                            final File hocrFile = new File(hocrFileDirectory, TesseractUtils.getDestFileName(pngFile));

                            if (pngFile.exists()) {
                                if (pngFile.renameTo(new File(archivePngFileDirectory, pngFile.getName()))) {
                                    System.out.println("fichier png déplacé");
                                } else {
                                    System.out.println("echec png déplacé");
                                }
                            }
                            if (hocrFile.exists()) {
                                if (hocrFile.renameTo(new File(archiveHocrFileDirectory, hocrFile.getName()))) {
                                    System.out.println("fichier hocr déplacé");
                                } else {
                                    System.out.println("echec hocr déplacé");
                                }
                            }
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                } else {
                    System.out.println("fournisseur inconnue:" + invoice.getSupplierName());
                }
            }
        }
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                getInvoiceTable().comp.setMode(ReloadPanel.MODE_EMPTY);
                getBSave().setEnabled(true);
            }
        });
    }

    private InvoiceOCRTable getInvoiceTable() {
        return this.invoiceTable;
    }

    private JButton getBSave() {
        return this.bSave;
    }

    private int getIdCompteAchat(SQLRow supplierRow, SQLTable purchaseTable) {
        int idCompteAchat = -1;
        if (supplierRow.isForeignEmpty("ID_COMPTE_PCE_CHARGE")) {
            // Select Compte charge par defaut
            final SQLTable tablePrefCompte = purchaseTable.getTable("PREFS_COMPTE");
            final SQLRow rowPrefsCompte = SQLBackgroundTableCache.getInstance().getCacheForTable(tablePrefCompte).getRowFromId(2);
            // compte Achat
            idCompteAchat = rowPrefsCompte.getInt("ID_COMPTE_PCE_ACHAT");
            if (idCompteAchat <= 1) {
                try {
                    idCompteAchat = ComptePCESQLElement.getIdComptePceDefault("Achats");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        } else {
            idCompteAchat = supplierRow.getForeignID("ID_COMPTE_PCE_CHARGE");
        }
        return idCompteAchat;
    }
}
