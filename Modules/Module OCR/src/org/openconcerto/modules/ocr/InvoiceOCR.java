package org.openconcerto.modules.ocr;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openconcerto.erp.core.finance.tax.model.TaxeCache;
import org.openconcerto.sql.model.SQLRowAccessor;

public class InvoiceOCR {
    private String supplierName;
    private String invoiceNumber;
    private Date date;
    private BigDecimal amount;
    private BigDecimal tax;
    private BigDecimal amountWithTax;
    private int taxId = -1;
    private boolean valid = true;
    private boolean active = true;
    private boolean isNotInvoice = true;
    private boolean containCalculatedValue = false;
    private int totalPage = -1;
    private Map<OCRPage, List<OCRLine>> map = new HashMap<OCRPage, List<OCRLine>>();
    private final List<OCRPage> pages = new ArrayList<OCRPage>();

    public String getSupplierName() {
        return this.supplierName;
    }

    public String getInvoiceNumber() {
        return this.invoiceNumber;
    }

    public Date getDate() {
        return this.date;
    }

    public BigDecimal getAmount() {
        return this.amount;
    }

    /**
     * % tax (0.20, 0.55,...),
     */
    public BigDecimal getTax() {
        return this.tax;
    }

    public int getTaxId() {
        return this.taxId;
    }

    public BigDecimal getAmountWithTax() {
        return this.amountWithTax;
    }

    public boolean getValid() {
        return this.valid;
    }

    public boolean getActive() {
        return this.active;
    }
    
    public boolean getContainCalculatedValue(){
        return this.containCalculatedValue;
    }

    public int getTotalPage() {
        return this.totalPage;
    }

    public Map<OCRPage, List<OCRLine>> getMap() {
        return this.map;
    }
    
    public boolean getIsNotInvoice(){
        return this.isNotInvoice;
    }
    
    public void setIsNotInvoice(boolean isNotInvoice){
        this.isNotInvoice = isNotInvoice;
    }

    public void setMap(Map<OCRPage, List<OCRLine>> map) {
        this.map = map;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public void setTax(BigDecimal tax) {
        this.tax = tax;
    }

    public void setAmountWithTax(BigDecimal amountWithTax) {
        this.amountWithTax = amountWithTax;
    }

    public void setTaxId() {
        final BigDecimal realTaxPercent = getTaxPercent();
        Integer id = null;
        if (realTaxPercent != null) {
            Set<SQLRowAccessor> taxes = TaxeCache.getCache().getAllTaxe();
            for (SQLRowAccessor taxe : taxes) {
                final BigDecimal taxPercent = (new BigDecimal(((Float) taxe.getFloat("TAUX")).toString())).divide(new BigDecimal(100));
                final BigDecimal tempTax = this.amount.multiply(taxPercent).setScale(2, BigDecimal.ROUND_HALF_UP);
                if (this.tax.compareTo(tempTax) == 0) {
                    id = taxe.getInt("ID_TAXE");
                    break;
                }
            }
            if (id != null) {
                this.taxId = id.intValue();
            } else {
                this.taxId = -1;
            }
        } else {
            this.taxId = -1;
        }
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
    
    public void setContainCalculatedValue(boolean containCalculatedValue){
        this.containCalculatedValue = containCalculatedValue;
    }

    public void setTotalPage(int total) {
        this.totalPage = total;
    }

    public void addHighlight(OCRPage page, OCRLine line) {
        List<OCRLine> l = this.map.get(page);
        if (l == null) {
            l = new ArrayList<OCRLine>();
            this.map.put(page, l);
        }
        l.add(line);
    }

    public void setHighlight(Map<OCRPage, List<OCRLine>> m) {
        this.map = m;
    }

    public List<OCRLine> getHighlight(OCRPage page) {
        List<OCRLine> l = this.map.get(page);
        if (l == null) {
            return Collections.emptyList();
        }
        return l;
    }

    public void addPage(OCRPage page) {
        this.pages.add(page);
    }

    public void addPages(List<OCRPage> list) {
        this.pages.addAll(list);
    }

    public OCRPage getPage(int i) {
        return this.pages.get(i);
    }

    public int getPageCount() {
        return this.pages.size();
    }

    public boolean checkNullValue() {
        if (this.amount == null || this.tax == null || this.amountWithTax == null || this.invoiceNumber == null || this.date == null) {
            return false;
        }
        return true;
    }

    public boolean checkAmounts() {
        if (!(this.amount.add(this.tax).compareTo(this.amountWithTax) == 0)) {
            return false;
        } else if (this.amount.compareTo(new BigDecimal(0)) == -1 || this.amountWithTax.compareTo(new BigDecimal(0)) == -1 || this.tax.compareTo(new BigDecimal(0)) == -1) {
            return false;
        }
        return true;
    }
    
    public void setNullAmounts() {
        this.amount = null;
        this.tax = null;
        this.amountWithTax = null;
    }

    private BigDecimal getTaxPercent() {
        if (this.tax != null && this.amount != null) {
            return this.tax.divide(this.amount, 10, BigDecimal.ROUND_HALF_UP).multiply(new BigDecimal(100)).setScale(2, BigDecimal.ROUND_HALF_UP);
        }
        return null;
    }

    @Override
    public String toString() {
        return super.toString() + " : " + this.invoiceNumber + " Fournisseur:" + this.supplierName + " " + this.date + " Total: " + this.amount + " € HT TVA:" + this.tax + " TTC: "
                + this.amountWithTax;
    }
}
