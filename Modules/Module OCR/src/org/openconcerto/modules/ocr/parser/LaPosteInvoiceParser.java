package org.openconcerto.modules.ocr.parser;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openconcerto.modules.ocr.InvoiceOCR;
import org.openconcerto.modules.ocr.OCRLine;
import org.openconcerto.modules.ocr.OCRPage;

public class LaPosteInvoiceParser extends AbstractInvoiceParser {
    private boolean pageNumberNear = false;

    @Override
    public boolean parse(OCRPage page) {
        if (!page.contains("la poste") && !page.contains("la paste")) {
            return false;
        }
        
        final InvoiceOCR invoice = this.getInvoice();
        final List<OCRLine> lines = page.getLines();
        invoice.setSupplierName("La poste");
        try {
            for (OCRLine line : lines) {
                final String text = line.getText().toLowerCase();
                // Get invoice number
                if(invoice.getInvoiceNumber() == null && text.contains("lp")) {
                	final int indexStart = text.indexOf("lp");
                	final int indexStop = indexStart + 16;
                	if(text.length() >= indexStop){
                		final String n = text.substring(indexStart, indexStop);
                		if(!n.contains(" ")){
                			invoice.setInvoiceNumber(n.toUpperCase());
                		}
                	}
                
                } 
                // Get current page number
                if (invoice.getTotalPage() == -1 && text.contains("page") && text.contains("/")) {
                    final Pattern p = Pattern.compile(".*page.*(\\d+)\\((\\d+)\\).*");
                    final Matcher m = p.matcher(text);
                    final boolean b = m.matches();
                    // If find
                    if (b && m.groupCount() >= 2) {
                    	final int number = Integer.valueOf(m.group(1));
                        final int total = Integer.valueOf(m.group(2));
                        if (number <= total) {
                            page.setPageNumber(number);
                            invoice.setTotalPage(total);
                            addHighlight(page, line);
                        }
                    }
                }
                // Page informations are maybe in next lines
                if(invoice.getTotalPage() == -1 && text.contains("page")){
                	this.pageNumberNear = true;
                }
                // Get current page number
                if(this.pageNumberNear && text.contains("/")){
                    final Pattern p = Pattern.compile(".*page.*(\\d+)\\((\\d+)\\).*");
                    final Matcher m = p.matcher(text);
                    final boolean b = m.matches();
                    // If find
                    if (b && m.groupCount() >= 2) {
                        final int number = Integer.valueOf(m.group(1));
                        final int total = Integer.valueOf(m.group(2));
                        this.pageNumberNear = false;
                        if (number <= total) {
                        	page.setPageNumber(number);
                            invoice.setTotalPage(total);
                            addHighlight(page, line);
                        }
                    }
                } 
                // Get amount with tax
                if (this.getInvoice().getAmountWithTax() == null && text.contains("net") && text.contains("payer")) {
                	final List<BigDecimal> listWithTax = getDecimalsInLine(text);
                	if(listWithTax.size() > 0){
		                invoice.setAmountWithTax(listWithTax.get(0));
		                addHighlight(page, line);
                	}
                
                } 
                // Get amount
                if (this.getInvoice().getAmount() == null && (text.contains("total") || text.contains("totat")) && text.contains("net")) {
                	final List<BigDecimal> listAmount = getDecimalsInLine(text);
                	if(listAmount.size() > 0){
		                invoice.setAmount(listAmount.get(0));
		                addHighlight(page, line);
                	}
                } 
                // Get tax
                if(this.getInvoice().getTax() == null && text.trim().contains("tva")){
                	final List<BigDecimal> listTax = getDecimalsInLine(text);
                	if(listTax.size() > 0){
		                invoice.setTax(listTax.get(0));
		                addHighlight(page, line);
                	}
                } 
                // Get date
                if(invoice.getDate() == null && text.contains("le")) {
                	final String[] split = text.split("\\s+");
                	if(split.length > 1){
	                    final Date d = ParserUtils.parseDate(split[1]);
	                    if (d != null) {
	                        invoice.setDate(d);
	                        addHighlight(page, line);
	                    }
                	}
                }
            }
            checkInvoice(false);
            invoice.setHighlight(this.getHighlight());
        } catch (Exception e) {
        	return false;
        }
        return true;
    }

    @Override
    public void reset() {
        super.reset();
        this.pageNumberNear = false;
    }

    @Override
    protected boolean checkInvoiceNumber(String invoiceNumber) {
        boolean result = true; 
        final int numberLength = invoiceNumber.length();
        if(numberLength != 16){
            result = false;
        } else if(!ParserUtils.isLong(invoiceNumber.substring(2, numberLength))){
            result = false;
        } else if(!invoiceNumber.startsWith("LP")){
            result = false;
        }
        return result;
    }
}
