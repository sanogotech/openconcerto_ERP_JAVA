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

public class TechDataInvoiceParser extends AbstractInvoiceParser {

    private final List<Date> dates = new ArrayList<Date>();
    private boolean numberIsNear = false;
    private boolean pageNumberNear = false;

    @Override
    public boolean parse(OCRPage page) {
        if (!page.contains("tech data")) {
            return false;
        }
        
        final InvoiceOCR invoice = this.getInvoice();
        final List<OCRLine> lines = page.getLines();
        invoice.setSupplierName("Tech Data");
        try {
            for (OCRLine line : lines) {
                final String text = line.getText().toLowerCase();
                // Maybe the invoice number is in next lines
                if ((text.contains("date") || text.contains("dme")) && (text.contains("document") || text.contains("docmnmn"))) {
                    this.numberIsNear = true;
                    // Get invoice number
                } else if (invoice.getInvoiceNumber() == null && this.numberIsNear) {
                    final String cText = text.replace('!', '/').replace(']', '/');
                    final String[] split = cText.split("\\/");
                    if (split.length > 2) {
                        if (split[2].trim().length() == 8) {
                            final String n = split[2];
                            invoice.setInvoiceNumber(n.trim());
                        }
                        final String strDate = split[1].trim();
                        final Date d = ParserUtils.parseDate(strDate);
                        if (d != null) {
                            invoice.setDate(d);
                            this.numberIsNear = false;
                            addHighlight(page, line);
                        }
                    }

                }
                // Get current page number and total pages
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
                // Page number is maybe in next lines
                if (invoice.getTotalPage() == -1 && text.contains("page")) {
                    this.pageNumberNear = true;
                    // Search of current page number and total pages
                } else if (this.pageNumberNear && text.contains("(")) {
                    final Pattern p = Pattern.compile(".*page.*(\\d+)\\((\\d+)\\).*");
                    final Matcher m = p.matcher(text);
                    final boolean b = m.matches();
                    // If find
                    if (b && m.groupCount() >= 2) {
                        final int number = Integer.valueOf(m.group(1));
                        final int total = Integer.valueOf(m.group(2));
                        this.pageNumberNear = false;
                        addHighlight(page, line);
                        if (number <= total) {
                            page.setPageNumber(number);
                            invoice.setTotalPage(total);
                            addHighlight(page, line);
                        }
                    }
                }

                // FIXME : espace dans le montant
                // Get amount with tax
                if (invoice.getAmountWithTax() == null && (text.contains("net a payer") || text.contains("neta payer") || text.contains("netapayer"))) {
                    final List<BigDecimal> listWithTax = getDecimalsInLine(text);
                    if (listWithTax.size() > 0) {
                        invoice.setAmountWithTax(listWithTax.get(0));
                        addHighlight(page, line);
                    }
                }
                // Get amount
                if (text.trim().startsWith("bases") && this.getInvoice().getAmount() == null) {
                    final List<BigDecimal> listAmount = getDecimalsInLine(text);
                    if (listAmount.size() > 0) {
                        invoice.setAmount(listAmount.get(0));
                        addHighlight(page, line);
                    }
                }
                // Get tax
                if (text.trim().startsWith("total t.v.a.") && this.getInvoice().getTax() == null) {
                    final List<BigDecimal> listTax = getDecimalsInLine(text);
                    if (listTax.size() > 0) {
                        invoice.setTax(listTax.get(0));
                        addHighlight(page, line);
                    }
                }
                // Get date
                if (invoice.getDate() == null && !text.trim().equals("")) {
                    final Date d = ParserUtils.parseDate(text);
                    if (d != null) {
                        this.dates.add(d);
                        invoice.setDate(d);
                        addHighlight(page, line);
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
        this.numberIsNear = false;
        this.pageNumberNear = false;
    }

    @Override
    protected boolean checkInvoiceNumber(String invoiceNumber) {
        // TODO Auto-generated method stub
        return false;
    }
}
