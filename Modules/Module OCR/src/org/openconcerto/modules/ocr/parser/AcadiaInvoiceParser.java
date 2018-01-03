package org.openconcerto.modules.ocr.parser;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openconcerto.modules.ocr.InvoiceOCR;
import org.openconcerto.modules.ocr.OCRLine;
import org.openconcerto.modules.ocr.OCRPage;

public class AcadiaInvoiceParser extends AbstractInvoiceParser {
    private final List<Date> dates = new ArrayList<Date>();
    private boolean pageNumberNear = false;

    @Override
    public boolean parse(OCRPage page) {
        if (!(page.contains("acadia") || page.contains("ccadia") || page.contains("czcadia") || page.contains("ccadic") || page.contains("0cadia") || page.contains("acadla") || page.contains("ocodio"))) {
            return false;
        }

        final InvoiceOCR invoice = this.getInvoice();
        final List<OCRLine> lines = page.getLines();
        final int lineCount = lines.size();
        invoice.setSupplierName("Acadia");
        try {            
			for (int i = 0; i < lineCount; i++) {
            	final OCRLine line = lines.get(i);
                final String text = line.getText().toLowerCase();
                if(text.contains("bon") && text.contains("livraison")){
                    invoice.setIsNotInvoice(true);
                }
                // Get invoice number
                if(invoice.getInvoiceNumber() == null && text.contains("f")) {
                	final int indexStart = text.indexOf("f");
                	final int indexNextSpace = text.indexOf(" ", indexStart);
                	final int indexStop = (indexNextSpace == -1) ? text.length() : indexNextSpace;
                	final int numberLength = indexStop - indexStart; 
                	if(numberLength == 9 && ParserUtils.isInteger(text.substring(indexStart + 1, indexStop))){
                		final String n = text.substring(indexStart, indexStop).trim();
                		if(!n.equals("")){
                			invoice.setInvoiceNumber(n.toUpperCase());
                			addHighlight(page, line);
                		}
                	}
                }
                // Get current page number
                if (invoice.getTotalPage() == -1 && text.contains("page") && text.contains("(")) {
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
                // Get current page number
                } else if(this.pageNumberNear && text.contains("(")){
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
                if (invoice.getAmountWithTax() == null && text.contains("total") && text.contains("t.t.c")) {
                	final List<BigDecimal> listDecimal = getDecimalsInLine(text);
                	final int listDecimalSize = listDecimal.size(); 
                	if(listDecimalSize > 0){
		                invoice.setAmountWithTax(listDecimal.get(0));
		                addHighlight(page, line);
                	}
                } 
                // Get amount                
                if (invoice.getAmount() == null && text.trim().startsWith("total") && (text.contains("h.t") || text.contains("h it")))  {
                	final List<BigDecimal> listDecimal = getDecimalsInLine(text);
                	final int listDecimalSize = listDecimal.size();
                    if(listDecimalSize > 0){
                	    if(text.contains("net")){
                	        invoice.setAmount(listDecimal.get(listDecimalSize - 1));
                	    } else {
                	        invoice.setAmount(listDecimal.get(0));
                	    }
		                addHighlight(page, line);
                	}
                }
                // Get tax
                if(invoice.getTax() == null && text.trim().contains("tva") && text.trim().contains("montant")){
                    int index = text.indexOf(")");
                    if(index == -1){
                        index = 0;
                    }
                    final String cText = text.substring(index, text.length()); 
                	final List<BigDecimal> listDecimal = getDecimalsInLine(cText);
                	int listDecimalSize = listDecimal.size();
                    if(listDecimalSize > 0){
		                invoice.setTax(listDecimal.get(listDecimalSize - 1));
		                addHighlight(page, line);
                	}
                }
                // Get all amounts
                if(invoice.getAmount() == null && invoice.getAmountWithTax() == null
                		&& (text.trim().equals("prix") || text.trim().equals("frix") || text.trim().equals("fiix") || text.trim().equals("prix ht"))) {
            		boolean end = false;
            		int j = i + 1;
            		List<BigDecimal> listDecimal = new ArrayList<BigDecimal>();
            		String cText;
            		while(!end && j < lineCount){
            			cText = lines.get(j).getText().toLowerCase();
            			listDecimal = getDecimalsInLine(cText);
            			if(listDecimal.size() == 0 && !ParserUtils.isInteger(cText)){
            				end = true;
            			}
            			j++;
            		}
            		final int listDecimalSize = listDecimal.size();
            		if(j - i > 4){
                		cText = lines.get(j - 2).getText().toLowerCase();
                		listDecimal = getDecimalsInLine(cText);
                		if(listDecimalSize > 0){
                			invoice.setAmountWithTax(listDecimal.get(0));
                		}
            			cText = lines.get(j - 3).getText().toLowerCase();
                		listDecimal = getDecimalsInLine(cText);
                		if(listDecimalSize > 0){
                			invoice.setTax(listDecimal.get(0));
                		}

                		cText = lines.get(j - 4).getText().toLowerCase();
                		listDecimal = getDecimalsInLine(cText);
                		if(listDecimal.size() > 0){
                			invoice.setAmount(listDecimal.get(0));
                		}
            		}
            	}
                // Get date
                if(invoice.getDate() == null && !text.trim().equals("")) {
                    Date date = ParserUtils.parseDate(text);  
                    if(date != null){
                       addHighlight(page, line);
                       invoice.setDate(date);
                    }
                }
            }
            missValueCalcul();
            invoice.setHighlight(this.getHighlight());
        } catch (Exception e) {
        	return false;
        }
        return true;
    }

    @Override
    public void reset() {
        super.reset();
        this.dates.clear();
        this.pageNumberNear = false;
    }

    @Override
    protected boolean checkInvoiceNumber(String invoiceNumber) {
        final int numberLength = invoiceNumber.length();
        boolean result = true;
        if(numberLength != 9 || !invoiceNumber.startsWith("F") || !ParserUtils.isInteger(invoiceNumber.substring(1, numberLength))){
            result = false;
        }
        return result;
    }
}
