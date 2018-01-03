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

public class GenericInvoiceParser extends AbstractInvoiceParser {
    private final List<Date> dates = new ArrayList<Date>();

    @Override
    public boolean parse(OCRPage page) {
    	final InvoiceOCR invoice = this.getInvoice(); 
        final List<OCRLine> lines = page.getLines();
        invoice.setSupplierName("??");
        for (OCRLine line : lines) {
            try {
                final String text = line.getText().toLowerCase();
                // Get page number
                if (text.contains("page") && text.contains("/")) {
                    final Pattern p = Pattern.compile(".*page.*(\\d+)/(\\d+).*");
                    final Matcher m = p.matcher(text);
                    final boolean b = m.matches();
                    System.out.println(text + " : " + b + " " + m.groupCount());
                    // si recherche fructueuse
                    if (b && m.groupCount() >= 2) {
                        final int number = Integer.valueOf(m.group(1));
                        final int total = Integer.valueOf(m.group(2));
                        if (number <= total) {
                            page.setPageNumber(number);
                            invoice.setTotalPage(total);
                        }
                    }
                } 
                // Get amount
                if (invoice.getAmount() == null && text.startsWith("total ht")) {
                    final List<BigDecimal> listAmount = getDecimalsInLine(text);
                    if(listAmount.size() > 0){
                    	invoice.setAmount(listAmount.get(0));
                    	this.addHighlight(page, line);
                    }
                } 
                // Get amount with tax
                if (invoice.getAmountWithTax() == null && text.startsWith("total ttc")) {
                    final List<BigDecimal> listAmountWithTax = getDecimalsInLine(text);
                    if(listAmountWithTax.size() > 0){
                    	invoice.setAmountWithTax(listAmountWithTax.get(0));
                    	this.addHighlight(page, line);
                    } 
                } 
                // Get tax
                if (invoice.getTax() == null && text.contains("montant") && text.contains("tva") ) {
                    final List<BigDecimal> listTax = getDecimalsInLine(text);
                    if(listTax.size() > 0){
                    	invoice.setTax(listTax.get(0));
                    	this.addHighlight(page, line);
                    }
                }
                // Get invoice number
                if (invoice.getInvoiceNumber() == null && text.contains("facture") && text.contains("n")) {
                	final String[] split = text.split("\\s+");
                	if(split.length > 1){
                		final int splitLength = split.length;
						for(int i = 0; i < splitLength; i++){
							final String n = split[i].trim();
							final int nLength = n.length(); 
							int startIndex = -1;
							int stopIndex = -1;
							for(int j = 0; j < nLength; j++){
								final String c = n.substring(j, j + 1);
								if(ParserUtils.isInteger(c)){
									if(startIndex == -1){
										startIndex = j;
									}
								} else if(startIndex != -1){
									stopIndex = j;
								}
							}
							
							if(startIndex != -1){
								if(stopIndex == -1){
									stopIndex = nLength;
								}
			                	if(stopIndex - startIndex > 4){
				                	invoice.setInvoiceNumber(split[i]);
				                    this.addHighlight(page, line);
				                    break;
			                	}
							}
						}
                	}
                }
                // Get date
                if(invoice.getDate() == null && !text.trim().equals("")){
                    final Date d = ParserUtils.parseDate(text);
                    if (d != null) {
                        this.dates.add(d);
                        invoice.setDate(d);
                        this.addHighlight(page, line);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        invoice.setHighlight(this.getHighlight());
        return true;
    }
    
    @Override
    public void reset() {
        super.reset();
        this.dates.clear();;
    }

    @Override
    protected boolean checkInvoiceNumber(String invoiceNumber) {
        // TODO Auto-generated method stub
        return false;
    }
}
