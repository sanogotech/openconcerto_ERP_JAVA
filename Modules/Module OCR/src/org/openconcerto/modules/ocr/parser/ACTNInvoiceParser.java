package org.openconcerto.modules.ocr.parser;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.openconcerto.modules.ocr.InvoiceOCR;
import org.openconcerto.modules.ocr.OCRLine;
import org.openconcerto.modules.ocr.OCRPage;

public class ACTNInvoiceParser extends AbstractInvoiceParser {

    private final List<Date> dates = new ArrayList<Date>();
    private boolean nextLineIsNumber;

    @Override
    public boolean parse(OCRPage page) {
        if (!page.contains("ACTN")) {
            return false;
        }

        final InvoiceOCR invoice = this.getInvoice(); 
    	final List<OCRLine> lines = page.getLines();
    	invoice.setSupplierName("ACTN");
        try {
            for (OCRLine line : lines) {
                final String text = line.getText().toLowerCase();

                if (this.getInvoice().getInvoiceNumber() == null && text.contains("facture") && !text.trim().isEmpty()) {
                    final String cText = text.replace('!', '/').replace(']', '/');
                    final String[] split = cText.split("\\s+");
                    invoice.setInvoiceNumber(split[split.length - 1]);
                    addHighlight(page, line);
                } 
                if (invoice.getDate() == null && text.contains("page") && text.contains("date")) {
                	final String[] split = text.split("\\s+");
                	if(split.length > 2){
	                	final int pageNum = Integer.parseInt(split[2]);
	                	page.setPageNumber(pageNum);
	                	addHighlight(page, line);
                	}
                	if(split.length > 6){
                		if(split[6].length() == 10){
                			final String strDate = split[6];
                			final Date d = ParserUtils.parseDate(strDate);
                			if(d != null){
			                    invoice.setDate(d);
			                    addHighlight(page, line);
                			}
                		}
                	}
                }
                if (text.contains("frais de port")) {
                    this.nextLineIsNumber = true;
                } else if(invoice.getAmount() == null && invoice.getTax() == null && invoice.getAmountWithTax() == null &&  this.nextLineIsNumber && !text.trim().isEmpty()){ 
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
        this.dates.clear();;
        this.nextLineIsNumber = false;
    }

    @Override
    protected boolean checkInvoiceNumber(String invoiceNumber) {
        // TODO Auto-generated method stub
        return false;
    }
}
