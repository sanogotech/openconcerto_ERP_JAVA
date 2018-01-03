package org.openconcerto.modules.finance.accounting.exchangerates;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TimeZone;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.openconcerto.utils.StringInputStream;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ExchangeRatesDownloader {
    public static List<String> supportedCurrencyCodes = Arrays.asList("USD", "JPY", "BGN", "CZK", "DKK", "GBP", "HUF", "PLN", "RON", "SEK", "CHF", "NOK", "HRK", "RUB", "TRY", "AUD", "BRL", "CAD",
            "CNY", "HKD", "IDR", "ILS", "INR", "KRW", "MXN", "MYR", "NZD", "PHP", "SGD", "THB", "ZAR");
    private final List<Report> reports = new ArrayList<Report>();

    public ExchangeRatesDownloader() {
    }

    public static void setSupportedCurrencyCodes(List<String> codes) {
        supportedCurrencyCodes = codes;
    }

    public static void main(String[] args) throws Exception {
        ExchangeRatesDownloader d = new ExchangeRatesDownloader();
        d.downloadAndParse();
        Report r = d.getLastReport();
        System.out.println(r.getDate());
        System.out.println(r.getRate("USD"));
        System.out.println(r.convert(new BigDecimal(100), "EUR", "USD"));
        System.out.println(r.convert(new BigDecimal(100), "USD", "EUR"));
        System.out.println(r.convert(new BigDecimal(1), "EUR", "PLN"));
        System.out.println(r.convert(new BigDecimal(1), "PLN", "EUR"));
        System.out.println(r.convert(new BigDecimal(1), "USD", "PLN"));
        System.out.println(r.convert(new BigDecimal(1), "JPY", "PLN"));
    }

    private Report getLastReport() {
        if (reports.isEmpty()) {
            return null;
        }
        return reports.get(reports.size() - 1);
    }

    public void downloadAndParse() throws Exception {
        String s = downloadXML();
        parse(s);
    }

    private void parse(String s) throws Exception {
        reports.clear();
        final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        final Document doc = dBuilder.parse(new StringInputStream(s, "UTF-8"));
        doc.getDocumentElement().normalize();
        final NodeList children = doc.getFirstChild().getChildNodes();
        final int length = children.getLength();
        for (int i = 0; i < length; i++) {
            final Node n = children.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE && n.getNodeName().equals("Cube")) {
                NodeList cubes = ((Element) n).getChildNodes();
                for (int j = 0; j < cubes.getLength(); j++) {
                    final Node item = cubes.item(j);
                    if (item.getNodeType() == Node.ELEMENT_NODE) {
                        reports.add(parseReport((Element) item));
                    }
                }
            }
        }
        Collections.sort(reports, new Comparator<Report>() {
            @Override
            public int compare(Report o1, Report o2) {
                return o1.getDate().compareTo(o2.getDate());
            }
        });
    }

    private Report parseReport(Element element) {
        final String time = element.getAttribute("time");
        final int year = Integer.parseInt(time.substring(0, 4));
        final int month = Integer.parseInt(time.substring(5, 7));
        final int dayOfMonth = Integer.parseInt(time.substring(8, 10));
        final Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        c.clear();
        c.set(Calendar.YEAR, year);
        c.set(Calendar.MONTH, month - 1);
        c.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        System.out.println(c.getTime());
        final Report r = new Report(c.getTime(), "EUR");
        final NodeList children = element.getChildNodes();
        final int length = children.getLength();
        for (int i = 0; i < length; i++) {
            final Element n = (Element) children.item(i);
            final String currency = n.getAttribute("currency");
            final BigDecimal rate = new BigDecimal(n.getAttribute("rate"));
            r.addRate(currency, rate);
        }
        return r;
    }

    public List<String> getSupportedCurrencyCodes() {
        return supportedCurrencyCodes;
    }

    public static String downloadXML() {
        final String urlString = "http://www.ecb.europa.eu/stats/eurofxref/eurofxref-hist-90d.xml";
        BufferedInputStream in = null;
        ByteArrayOutputStream fout = null;
        try {
            in = new BufferedInputStream(new URL(urlString).openStream());
            fout = new ByteArrayOutputStream();

            final byte data[] = new byte[1024];
            int count;
            while ((count = in.read(data, 0, 1024)) != -1) {
                fout.write(data, 0, count);
            }
        } catch (Exception e) {
            return null;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    return null;
                }
            }
            if (fout != null) {
                try {
                    fout.close();
                    byte[] bytes = fout.toByteArray();
                    System.out.println(bytes.length);
                    return new String(bytes);
                } catch (IOException e) {
                    return null;
                }
            }
        }
        return null;
    }

    public List<Report> getReports() {
        return reports;
    }
}
