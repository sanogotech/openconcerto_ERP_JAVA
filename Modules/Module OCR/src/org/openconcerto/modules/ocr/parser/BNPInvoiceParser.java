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

public class BNPInvoiceParser extends AbstractInvoiceParser {

    private final List<Date> dates = new ArrayList<Date>();

    @Override
    public boolean parse(OCRPage page) {
        if (!((page.contains("bnp paribas") || page.contains("bn p pari bas")) && page.contains("domiciliation"))) {
            return false;
        }
        
        boolean result = true;
        final InvoiceOCR invoice = this.getInvoice();
        final List<OCRLine> lines = page.getLines();
        final int lineCount = lines.size();
        invoice.setSupplierName("BNP Paribas");
        try {
            for (int i = 0; i < lineCount; i++) {
                final OCRLine line = lines.get(i);
                final String text = line.getText().toLowerCase();
                // Get current page number and total page number
                if (invoice.getTotalPage() == -1 && text.contains("page") && text.contains("/")) {
                    final Pattern p = Pattern.compile(".*page.*(\\d+)/(\\d+).*");
                    final Matcher m = p.matcher(text);
                    final boolean b = m.matches();
                    // if find
                    if (b && m.groupCount() >= 2) {
                        final int number = Integer.valueOf(m.group(1));
                        final int total = Integer.valueOf(m.group(2));
                        if (number <= total) {
                            page.setPageNumber(number);
                            invoice.setTotalPage(total);
                            this.addHighlight(page, line);
                        }
                    }
                }
                // Get invoice number
                if (invoice.getInvoiceNumber() == null && text.startsWith("facture")) {
                    final String[] split = text.split("\\s+");
                    StringBuilder invoiceNumber = new StringBuilder();
                    if (split.length > 3 && split[1].contains("n")) {
                        for (int j = 2; j < split.length; j++) {
                            invoiceNumber.append(split[j]);
                        }
                        if (!invoiceNumber.equals("")) {
                            final int index = invoiceNumber.indexOf("fr");
                            if(index == 0){
                                invoiceNumber.replace(2, 3, "/");
                                invoiceNumber.insert(7, " ");
                                invoiceNumber.insert(invoiceNumber.length() - 3, " ");
                            }
                            
                            invoice.setInvoiceNumber(invoiceNumber.toString().toUpperCase());
                            this.addHighlight(page, line);
                        }
                    }
                }
                // Get amount, amount with tax and tax
                if (invoice.getAmount() == null && text.trim().startsWith("total")) {
                    final List<BigDecimal> listDecimal = getDecimalsInLine(text);
                    final int listDecimalSize = listDecimal.size();
                    if (listDecimalSize > 2) {
                        invoice.setAmount(listDecimal.get(0));
                        invoice.setTax(listDecimal.get(1));
                        invoice.setAmountWithTax(listDecimal.get(2));
                        addHighlight(page, line);
                    } else if (text.contains("000") && listDecimalSize > 1) {
                        invoice.setAmount(listDecimal.get(0));
                        invoice.setTax(new BigDecimal(0.00));
                        invoice.setAmountWithTax(listDecimal.get(1));
                    } else if (text.trim().equals("total") || (text.trim().startsWith("total") && listDecimalSize == 1)) {
                        boolean amountFind = false, taxFind = false, amountWithTaxFind = false;
                        final int limit = i + 7;
                        for (int j = i; j < limit && j < lineCount && !(amountFind && taxFind && amountWithTaxFind); j++) {
                            final String ctext = lines.get(j).getText().replace(" ", "").toLowerCase();
                            if (!ctext.trim().equals("total")) {
                                final List<BigDecimal> list = getDecimalsInLine(ctext);
                                if (!amountFind) {
                                    if (list.size() > 0) {
                                        invoice.setAmount(list.get(0));
                                    }
                                    if (ctext.trim().length() > 0) {
                                        amountFind = true;
                                    }
                                } else if (!taxFind) {
                                    if (list.size() > 0) {
                                        invoice.setTax(list.get(0));
                                    }
                                    if (ctext.trim().length() > 0) {
                                        taxFind = true;
                                    }
                                } else if (!amountWithTaxFind) {
                                    if (list.size() > 0) {
                                        invoice.setAmountWithTax(list.get(0));
                                    }
                                    if (ctext.trim().length() > 0) {
                                        amountWithTaxFind = true;
                                    }
                                }
                            }
                        }
                    }
                    if(invoice.getAmount() != null && invoice.getAmountWithTax() != null && invoice.getTax() != null && !invoice.checkAmounts()){
                        invoice.setNullAmounts();
                    }
                }
                // Get date
                if (invoice.getDate() == null && !text.trim().equals("")) {
                    final Date d = ParserUtils.parseDate(text);
                    if (d != null) {
                        this.dates.add(d);
                        if(text.startsWith("le ") || text.startsWith("ie ")){
                            invoice.setDate(d);
                        }
                    }
                }
                // Check if it's invoice or not
                if(!invoice.getIsNotInvoice() && !readValidity(text)){
                    invoice.setIsNotInvoice(true);
                }
            }
            
            if(invoice.getDate() == null){
                final int dateSize = this.dates.size();
                Date dateMax = null;
                for(int i = 0; i < dateSize; i++){
                    Date d1 = this.dates.get(i);
                    if(dateMax == null || d1.compareTo(dateMax) > 0){
                        dateMax = d1;
                    }
                }
                invoice.setDate(dateMax);
            }
            this.checkInvoice(false);
            invoice.setHighlight(this.getHighlight());
            
        } catch (Exception ex) {
            result = false;
            ex.printStackTrace();
        }
        return result;
    }

    @Override
    public void reset() {
        super.reset();
        this.dates.clear();
    }
    
    protected boolean readValidity(String text){
        final String cText = text.replace("é", "e");
        if(cText.contains("releve") && cText.contains("change")){
            return false;
        }
        return true;
    }

    @Override
    protected boolean checkInvoiceNumber(String invoiceNumber) {
        boolean result = true; 
        final int numberLength = invoiceNumber.length();
        final boolean shortNumber = (numberLength == 17);
        final boolean longNumber = (numberLength == 29);
        final String cInvoiceNumber = invoiceNumber.toLowerCase(); 
        if(!longNumber && !shortNumber){
            result = false;
        } else if(shortNumber){
            if(!ParserUtils.isLong(cInvoiceNumber)){
                result = false;
            }
        } else if(longNumber){
            if(!ParserUtils.isLong(cInvoiceNumber.substring(8, numberLength - 4))){
                result = false;
            } else if(!cInvoiceNumber.startsWith("fr/bddf") && !cInvoiceNumber.endsWith("pef")){
                result = false;
            }
        }
        return result;
    }
}
