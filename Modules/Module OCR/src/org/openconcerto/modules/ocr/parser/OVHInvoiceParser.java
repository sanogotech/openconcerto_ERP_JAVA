package org.openconcerto.modules.ocr.parser;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openconcerto.modules.ocr.InvoiceOCR;
import org.openconcerto.modules.ocr.OCRLine;
import org.openconcerto.modules.ocr.OCRPage;

public class OVHInvoiceParser extends AbstractInvoiceParser {
    private final List<Date> dates = new ArrayList<Date>();
    private boolean pageNumberNear = false;
    private boolean isTotalPage = false;

    @Override
    public boolean parse(OCRPage page) {
        if (!page.contains("OVH")) {
            return false;
        }

        final InvoiceOCR invoice = this.getInvoice();
        final List<OCRLine> lines = page.getLines();
        final int lineCount = lines.size();
        invoice.setSupplierName("OVH");
        try {
            for (int i = 0; i < lineCount; i++) {
                final OCRLine line = lines.get(i);
                final String text = line.getText().toLowerCase();

                // Get invoice number
                if (invoice.getInvoiceNumber() == null && text.replace(" ", "").startsWith("facture")) {
                    final String cText = text.replace('!', '/').replace(']', '/');
                    final String[] split = cText.split("\\:");
                    if (split.length > 1 && checkInvoiceNumber(split[1])) {
                        final String n = split[1].substring(0, 10).trim();
                        if (!n.equals("")) {
                            invoice.setInvoiceNumber(n);
                        }
                    }
                    final String[] split2 = cText.split("/");
                    if (split2.length > 2) {
                        String strDate = split2[1].trim().replace('-', '.');
                        final String[] partDate = strDate.split("\\.");

                        if (partDate.length > 2) {
                            System.out.println(text);
                            System.out.println(strDate);

                            try {
                                int d = Integer.parseInt(partDate[0]);
                                int m = Integer.parseInt(partDate[1]) - 1;
                                int y = Integer.parseInt(partDate[2]);
                                Calendar cal = Calendar.getInstance();
                                cal.set(y, m, d, 0, 0, 0);
                                invoice.setDate(cal.getTime());

                            } catch (Exception e) {
                                // nothing
                            }
                        }
                    }
                    addHighlight(page, line);
                }
                // Get invoice number
                if (invoice.getInvoiceNumber() == null && text.startsWith("fr") || text.startsWith("sysfr")) {
                    int indexStart = text.indexOf("sysfr");
                    if (indexStart == -1) {
                        indexStart = text.indexOf("fr");
                    }
                    final int indexNextSpace = text.indexOf(" ", indexStart);
                    final int indexStop = (indexNextSpace == -1) ? text.length() : indexNextSpace;
                    final int numberLength = indexStop - indexStart;
                    if (numberLength == 10 || numberLength == 9) {
                        final String n = text.substring(indexStart, indexStop).trim().replace("g", "9").replace("q", "9").toUpperCase();
                        if (!n.equals("")) {
                            invoice.setInvoiceNumber(n);
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
                if (invoice.getTotalPage() == -1 && text.contains("page")) {
                    this.pageNumberNear = true;
                    // Get current page number
                } else if (this.pageNumberNear && text.contains("(")) {
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
                if (invoice.getAmountWithTax() == null && text.contains("total") && !text.contains("sous")) {
                    final List<BigDecimal> listTTC = getDecimalsInLine(text);
                    if (listTTC.size() > 0) {
                        invoice.setAmountWithTax(listTTC.get(0));
                        addHighlight(page, line);
                    }
                }
                // Get amount
                if (invoice.getAmount() == null && (text.trim().startsWith("prix") || text.trim().startsWith("fiix") || text.trim().startsWith("frix"))) {
                    final List<BigDecimal> listHT = getDecimalsInLine(text);
                    if (listHT.size() > 0) {
                        invoice.setAmount(listHT.get(0));
                        addHighlight(page, line);
                    }
                }
                // Get tax
                if (invoice.getTax() == null && text.trim().contains("tva")) {
                    final List<BigDecimal> listTVA = getDecimalsInLine(text);
                    if (listTVA.size() > 0) {
                        invoice.setTax(listTVA.get(0));
                        addHighlight(page, line);
                    }
                }
                // Get all amounts
                if (invoice.getAmount() == null && invoice.getAmountWithTax() == null
                        && (text.trim().equals("prix") || text.trim().equals("frix") || text.trim().equals("fiix") || text.trim().equals("prix ht"))) {
                    boolean end = false;
                    int j = i + 1;
                    List<BigDecimal> list;
                    String cText;
                    while (!end) {
                        cText = lines.get(j).getText().toLowerCase().replace("e", "€").replace("€", "");
                        list = getDecimalsInLine(cText);
                        if (list.size() == 0 && !ParserUtils.isInteger(cText)) {
                            end = true;
                        }
                        j++;
                    }
                    if (j - i > 4) {
                        cText = lines.get(j - 2).getText().toLowerCase();
                        list = getDecimalsInLine(cText);
                        if (list.size() > 0) {
                            invoice.setAmountWithTax(list.get(0));
                        }
                        cText = lines.get(j - 3).getText().toLowerCase();
                        list = getDecimalsInLine(cText);
                        if (list.size() > 0) {
                            invoice.setTax(list.get(0));
                        }

                        cText = lines.get(j - 4).getText().toLowerCase();
                        list = getDecimalsInLine(cText);
                        if (list.size() > 0) {
                            invoice.setAmount(list.get(0));
                        }
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
            checkInvoice(false);
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
        this.dates.clear();
        this.pageNumberNear = false;
        this.isTotalPage = false;
    }

    @Override
    protected boolean checkInvoiceNumber(String invoiceNumber) {
        boolean result = true;
        final String cInvoiceNumber = invoiceNumber.trim().toLowerCase();
        final boolean bSys = cInvoiceNumber.startsWith("sysfr");
        final int numberLength = cInvoiceNumber.length();
        if (!cInvoiceNumber.startsWith("fr") && !bSys) {
            result = false;
        } else if (bSys && numberLength > 5 && !ParserUtils.isLong(cInvoiceNumber.substring(5, numberLength))) {
            result = false;
        } else if (numberLength > 2 && !ParserUtils.isLong(cInvoiceNumber.substring(2, numberLength))) {
            result = false;
        }
        return result;
    }
}
