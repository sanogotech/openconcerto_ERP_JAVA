package org.openconcerto.modules.finance.accounting.exchangerates;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Report {
    private final Date date;
    private final String mainCurrencyCode;
    private Map<String, BigDecimal> rates = new HashMap<String, BigDecimal>();

    public Report(Date date, String mainCurrencyCode) {
        this.date = date;
        this.mainCurrencyCode = mainCurrencyCode;
    }

    public Date getDate() {
        return date;
    }

    public String getMainCurrencyCode() {
        return mainCurrencyCode;
    }

    public void addRate(String currency, BigDecimal rate) {
        if (rates.containsValue(currency)) {
            throw new IllegalArgumentException(currency + " rate is already set");
        }
        rates.put(currency, rate);
    }

    public BigDecimal getRate(String to) {
        return rates.get(to);
    }

    public BigDecimal convert(BigDecimal amount, String from, String to) {
        return amount.multiply(getRate(from, to));
    }

    public BigDecimal getRate(String from, String to) {
        if (from.equals(mainCurrencyCode)) {
            return getRate(to);
        } else if (to.equals(mainCurrencyCode)) {
            final BigDecimal rate = getRate(from);
            return BigDecimal.ONE.divide(rate, 4, BigDecimal.ROUND_HALF_UP);
        } else {
            BigDecimal r1 = rates.get(from);
            BigDecimal r2 = rates.get(to);
            if (r1 == null) {
                throw new IllegalArgumentException("rate for " + from + " not found");
            }
            if (r2 == null) {
                throw new IllegalArgumentException("rate for " + to + " not found");
            }
            return r2.divide(r1, 4, BigDecimal.ROUND_HALF_UP);
        }

    }
}
