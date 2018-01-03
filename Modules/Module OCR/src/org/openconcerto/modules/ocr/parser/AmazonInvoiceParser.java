package org.openconcerto.modules.ocr.parser;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.openconcerto.modules.ocr.InvoiceOCR;
import org.openconcerto.modules.ocr.OCRLine;
import org.openconcerto.modules.ocr.OCRPage;


public class AmazonInvoiceParser extends AbstractInvoiceParser {
    private boolean nextLineIsNumber;

    @Override
    public boolean parse(OCRPage page) {
        if (!page.contains("Amazon")) {
            return false;
        }

        final InvoiceOCR invoice = this.getInvoice(); 
        final List<OCRLine> lines = page.getLines();
        invoice.setSupplierName("Amazon");
        try {
            for (OCRLine line : lines) {
                final String text = line.getText().toLowerCase();
                if (this.getInvoice().getInvoiceNumber() == null && text.startsWith("votre commande")) {
                    final String[] split = text.split("\\s+");
                    if(split.length == 12){
                    	final Date d = ParserUtils.parseDate(split[3] + " " + split[4] + " " + split[5]);
                    	if(d != null){
                    		invoice.setDate(d);
                    	}
                    	invoice.setInvoiceNumber(split[7]);
                    }
                    final String n = split[split.length - 1];
                    invoice.setInvoiceNumber(n);                    
                    addHighlight(page, line);
                } 
                if (text.contains("Regu (réglé)")) {
                	invoice.setInvoiceNumber("Ceci n'est pas une facture.");
                	addHighlight(page, line);
                } 
                if (invoice.getAmount() == null && invoice.getTax() == null && invoice.getAmountWithTax() == null && text.contains("frais de port")) {
                    this.nextLineIsNumber = true;
                } else if(this.nextLineIsNumber && !text.trim().isEmpty()){ 
                	final String[] split = text.split("\\s+");
                	final List<String> tmp = new ArrayList<String>();
                	for(int i = 0; i < split.length; i++){
                		if(split[i].contains(",")){
                			tmp.add(split[i]);
                		}
                	}
                	if(tmp.size() > 0){
                        final String s = tmp.get(0).replace('.', ' ').replace(',', '.');
                        final String cleanNumberString = ParserUtils.getCleanNumberString(s);

                        final BigDecimal ht = new BigDecimal(cleanNumberString);
                		invoice.setAmount(ht);
                	}
                	if(tmp.size() > 3){
                		final String s = tmp.get(3).replace('.', ' ').replace(',', '.');
                        final String cleanNumberString = ParserUtils.getCleanNumberString(s);

                        final BigDecimal tva = new BigDecimal(cleanNumberString);
                		invoice.setTax(tva);
                	}
                	if(tmp.size() > 4){
                		final String s = tmp.get(4).replace('.', ' ').replace(',', '.');
                        final String cleanNumberString = ParserUtils.getCleanNumberString(s);

                        final BigDecimal ttc = new BigDecimal(cleanNumberString);
                		invoice.setAmountWithTax(ttc);
                	}
                	addHighlight(page, line);
                	this.nextLineIsNumber = false;
                }
            }
            invoice.setHighlight(this.getHighlight());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public void reset() {
        super.reset();
        this.needModePage = true;
        this.nextLineIsNumber = false;
    }

    @Override
    protected boolean checkInvoiceNumber(String invoiceNumber) {
        // TODO Auto-generated method stub
        return false;
    }
}
