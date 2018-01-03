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

public class EbayENInvoiceParser extends AbstractInvoiceParser {

    private final List<Date> dates = new ArrayList<Date>();

    @Override
    public boolean parse(OCRPage page) {
        if (!page.contains("ebay.com")) {
            return false;
        }
        
        final InvoiceOCR invoice = this.getInvoice();
        final List<OCRLine> lines = page.getLines();
        invoice.setSupplierName("eBay");
        try{
            for (OCRLine line : lines) {
                final String text = line.getText().toLowerCase();
                // get current page number
                if (text.contains("page") && text.contains("/")) {
                    final Pattern p = Pattern.compile(".*page.*(\\d+)/(\\d+).*");
                    final Matcher m = p.matcher(text);
                    final boolean b = m.matches();
                    
                    System.out.println(text + " : " + b + " " + m.groupCount());
                    
                    // if find
                    if (b && m.groupCount() >= 2) {
                    	final int number = Integer.valueOf(m.group(1));
                        final int total = Integer.valueOf(m.group(2));
                        System.out.println(number + ":" + total);
                        if (number <= total) {
                        	page.setPageNumber(number);
                            invoice.setTotalPage(total);
                        }
                    }
                } else if (text.startsWith("total ht")) {
                    final String cText = text.replace("euros", " ").replace("euro", " ").replace("eur", " ").replace(',', '.').trim();
                    final String[] split = cText.split("\\s+");
                    final BigDecimal ht = new BigDecimal(split[split.length - 1]);
                    this.getInvoice().setAmount(ht);
                    this.addHighlight(page, line);
                } else if(!text.trim().equals("")) {
                    final Date d = ParserUtils.parseDate(text);
                    if (d != null) {
                        this.dates.add(d);
                        this.getInvoice().setDate(d);
                        this.addHighlight(page, line);
                    }
                }
            }
            invoice.setHighlight(this.getHighlight());
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    @Override
    public void reset() {
        super.reset();
        this.needModePage = true;
        this.dates.clear();
    }

    @Override
    protected boolean checkInvoiceNumber(String invoiceNumber) {
        // TODO Auto-generated method stub
        return false;
    }
}
