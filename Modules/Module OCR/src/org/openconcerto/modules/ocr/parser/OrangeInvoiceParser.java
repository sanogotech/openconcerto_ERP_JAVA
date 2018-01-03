package org.openconcerto.modules.ocr.parser;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.openconcerto.modules.ocr.InvoiceOCR;
import org.openconcerto.modules.ocr.OCRLine;
import org.openconcerto.modules.ocr.OCRPage;

public class OrangeInvoiceParser extends AbstractInvoiceParser {
	@Override
	public boolean parse(OCRPage page) {
		if (!page.contains("orange") || page.contains("tech data")) {
			return false;
		}
		
		final InvoiceOCR invoice = this.getInvoice();
		final List<OCRLine> lines = page.getLines();
		invoice.setSupplierName("Orange");
		try {
			for (OCRLine line : lines) {
				final String text = line.getText().toLowerCase();
				// Get invoice number
				if (invoice.getInvoiceNumber() == null) {
					final String[] split = text.split("\\s+");
					final StringBuilder n = new StringBuilder();
					if (split.length > 2) {
		                final String label = split[0].replace("li", "u").replace("t", "f");
		                if(label.length() == 7 && label.startsWith("f")){
    						int splitLength = split.length;
    						for(int i = 2; i < splitLength; i++){
    							n.append(split[i]);
    						}
    						final String[] split2 = n.toString().replace(":", "-").replace("—", "-").replace(".", "-").replace("~", "-").replace(" ", "").replace("(", "").split("-");
    						if(split2.length > 1){
    						    String subs = split2[0].substring(split2[0].length() - 2, split2[0].length() - 1);
    						    if(ParserUtils.isInteger(subs)){
    						        subs = (subs.equals("6") || subs.equals("3")) ? "g" : subs;
    						        subs = (subs.equals("1")) ? "l" : subs;
    						    }
    						    split2[0] = split2[0].substring(0, split2[0].length() - 2) + subs + split2[0].substring(split2[0].length() - 1, split2[0].length());
    						    
    						}
    						
    						n.delete(0, n.length());
                            int split2Length = split2.length;
                            for(int i = 0; i < split2Length; i++){
                                n.append(split2[i]);
                            }
                            
                            if(n.length() > 9){
                                n.insert(10, " ");
                                if(n.length() > 15){
                                    String tempChar = n.substring(1, 2);
                                    if(tempChar.equals("8")){
                                        n.replace(1, 2, "3");
                                    }
                                    
                                    tempChar = n.substring(13, 14);
                                    if(ParserUtils.isInteger(tempChar)){
                                        if(tempChar.equals("9")){
                                            n.replace(13, 14, "G");
                                        }
                                    }
                                    tempChar = n.substring(14, 15);
                                    if(!ParserUtils.isInteger(tempChar)){
                                        if(tempChar.equals("o")){
                                            n.replace(14, 15, "0");
                                        }
                                    }
                                    tempChar = n.substring(15, 16);
                                    if(!ParserUtils.isInteger(tempChar)){
                                        if(tempChar.equals("i") || tempChar.equals("l")){
                                            n.replace(15, 16, "1");
                                        }
                                    }
                                    
                                    
                                    tempChar = n.substring(17, 18);
                                    if(!ParserUtils.isInteger(tempChar)){
                                        if(tempChar.equals("m")){
                                            n.replace(18, 19, "11");
                                        }
                                    }
                                    n.insert(15, " - ");
                                    if(n.length() >= 22){
                                        tempChar = n.substring(19, 20);
                                        if(!tempChar.equals("f")){
                                            n.replace(19, 20, "f");
                                        }
                                        tempChar = n.substring(21, 22);
                                        if(!ParserUtils.isInteger(tempChar)){
                                            if(tempChar.equals("o")){
                                                n.delete(21, 22);
                                                n.insert(21, "0");
                                            }
                                        }
                                        n.delete(22, n.length());
                                    }
                                }
                            }
                            
                            String finalNumber = n.toString();
                            if(checkInvoiceNumber(finalNumber)){
                                invoice.setInvoiceNumber(finalNumber.toUpperCase());
                            }
		                }
					}
				} 
				// Get amount and amount with tax
				if (invoice.getAmount() == null && text.contains("total de votre facture")) {
					List<BigDecimal> listValue = getDecimalsInLine(text);
					if (listValue.size() > 1) {
						invoice.setAmount(listValue.get(0));
						invoice.setAmountWithTax(listValue.get(1));
						addHighlight(page, line);
					}
				} 
				// Get current page number and total page
				if (invoice.getTotalPage() == -1 && text.contains("page")) {
					final String[] split = text.split("\\s+");
					final String[] split2 = split[split.length - 1].split("/");
					if (split2.length > 1) {
						if(ParserUtils.isInteger(split[0]) && ParserUtils.isInteger(split[1])){
							page.setPageNumber(Integer.parseInt(split2[0]));
							invoice.setTotalPage(Integer.parseInt(split2[1]));
						}
					}
				} 
				// Get date
				if(invoice.getDate() == null && text.contains("du")){
					final Date d = ParserUtils.parseDate(text);
					if (d != null) {
						invoice.setDate(d);
						addHighlight(page, line);
					}
				}
			}
            this.checkInvoice(true);
            invoice.setHighlight(this.getHighlight());
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	@Override
	public void reset() {
		super.reset();
	}

    @Override
    protected boolean checkInvoiceNumber(String invoiceNumber) {
        boolean result = true; 
        final String[] split = invoiceNumber.split("\\s+");
        
        if(split.length != 4){
            result = false;
        } else {
            if(split[0].length() != 10 || !ParserUtils.isLong(split[0])){
                result = false;
            } else if(split[1].length() != 4){
                result = false;
            } else if(!split[2].equals("-")){
                result = false;
            } else if(split[3].length() != 4){
                result = false;
            }
        }
        return result;
    }
}
