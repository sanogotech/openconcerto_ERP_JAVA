package org.openconcerto.modules.ocr.parser;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openconcerto.modules.ocr.InvoiceOCR;
import org.openconcerto.modules.ocr.OCRLine;
import org.openconcerto.modules.ocr.OCRPage;

public abstract class AbstractInvoiceParser implements InvoiceParser {

    protected boolean valid = false;
    protected boolean needModePage;
    private InvoiceOCR invoice;
    private final Map<OCRPage, List<OCRLine>> map = new HashMap<OCRPage, List<OCRLine>>();

    public AbstractInvoiceParser() {
        this.invoice = new InvoiceOCR();
    }

    public void reset() {
        this.valid = false;
        this.needModePage = true;
        this.invoice = new InvoiceOCR();
        this.map.clear();
    }

    @Override
    abstract public boolean parse(OCRPage page);
    
    abstract protected boolean checkInvoiceNumber(String invoiceNumber);

    @Override
    public boolean isValid() {
        return this.valid;
    }

    @Override
    public boolean needModePage() {
        return this.needModePage;
    }

    public void addHighlight(OCRPage page, OCRLine line) {
        List<OCRLine> l = this.map.get(page);
        if (l == null) {
            l = new ArrayList<OCRLine>();
            this.map.put(page, l);
        }
        l.add(line);
    }

    public Map<OCRPage, List<OCRLine>> getHighlight() {
        return this.map;
    }

    public InvoiceOCR getInvoice() {
        return this.invoice;
    }
    
    public void checkInvoice(boolean isValidWithCalculatedValue) {
        missValueCalcul();
        if(!this.invoice.checkNullValue()){
            this.invoice.setValid(false);
        } else if(this.invoice.getContainCalculatedValue() && !isValidWithCalculatedValue){
            this.invoice.setValid(false);
        } else if(!this.checkInvoiceNumber(this.invoice.getInvoiceNumber())){
            this.invoice.setValid(false);
        }
    }
    
    protected List<BigDecimal> getDecimalsInLine(String text) {
        final List<BigDecimal> listResult = new ArrayList<BigDecimal>();
        final StringBuilder result = new StringBuilder();
        final int textSize = text.length();
        text = text.replace(",", ".");
        for (int i = 0; i < textSize; i++) {
            if (ParserUtils.isInteger(text.substring(i, i + 1))) {
                int firstDot = -1;
                int secondDot = -1;
                int decimalDot = -1;
                String sNumber = "";
                int spaceIndex = text.indexOf(" ", i);
                int dotIndex = text.indexOf(".", i);
                
                if(dotIndex == spaceIndex + 1){
                    spaceIndex = text.indexOf(" ", dotIndex);
                }
                if (spaceIndex != -1) {
                    sNumber = text.substring(i, spaceIndex);
                } else {
                    int endNumberIndex = i;
                    while(endNumberIndex < textSize && (endNumberIndex == dotIndex || ParserUtils.isInteger(text.substring(endNumberIndex, endNumberIndex + 1)))){
                        endNumberIndex++;
                    }
                    sNumber = text.substring(i, endNumberIndex);
                }
                
                sNumber = sNumber.replace(" ", "").trim();
                
                if (ParserUtils.parseDate(sNumber) == null) {
                    if (textSize >= i + 8) {
                        if (sNumber.length() == 1 && ParserUtils.isInteger(text.substring(i + 2, i + 5))) {
                            sNumber = text.substring(i, i + 8).replace(" ", ".");
                        }
                    }
                    firstDot = sNumber.indexOf(".");
                    if (sNumber.length() > firstDot + 1) {
                        secondDot = sNumber.indexOf(".", firstDot + 1);
                    }
                    decimalDot = (secondDot == -1) ? firstDot : secondDot;
                    if (decimalDot != -1) {
                        for (int j = 0; j < sNumber.length(); j++) {
                            String character = sNumber.substring(j, j + 1);
                            if (ParserUtils.isInteger(character)) {
                                result.append(character);
                            } else if (decimalDot == j) {
                                result.append(".");
                            }
                        }
                        if (!result.equals("")) {
                            try {
                                BigDecimal dresult = new BigDecimal(result.toString());
                                if(dresult.compareTo(new BigDecimal("19.30")) == 0) {
                                    System.out.println("capasse");
                                }
                                listResult.add(dresult);
                            } catch (Exception ex) {
                                // nothing
                            }
                            result.delete(0, result.length());
                        }
                    }
                }
                i += sNumber.length() - 1;
            }
        }
        return listResult;
    }

    protected void missValueCalcul() {
        if (this.invoice.getAmount() == null) {
            if (this.invoice.getAmountWithTax() != null && this.invoice.getTax() != null) {
                final BigDecimal amount = this.invoice.getAmountWithTax().subtract(this.invoice.getTax());
                this.invoice.setAmount(amount);
                this.invoice.setContainCalculatedValue(true);
            }
        } else if (this.invoice.getAmountWithTax() == null) {
            if (this.invoice.getAmount() != null && this.invoice.getTax() != null) {
                final BigDecimal amountWithTax = this.invoice.getAmount().add(this.invoice.getTax());
                this.invoice.setAmountWithTax(amountWithTax);
                this.invoice.setContainCalculatedValue(true);
            }
        } else if (this.invoice.getTax() == null) {
            if (this.invoice.getAmount() != null && this.invoice.getAmountWithTax() != null) {
                final BigDecimal tax = this.invoice.getAmountWithTax().subtract(this.invoice.getAmount());
                this.invoice.setTax(tax);
                this.invoice.setContainCalculatedValue(true);
            }
        }
    }
}
