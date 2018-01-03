/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2011 OpenConcerto, by ILM Informatique. All rights reserved.
 * 
 * The contents of this file are subject to the terms of the GNU General Public License Version 3
 * only ("GPL"). You may not use this file except in compliance with the License. You can obtain a
 * copy of the License at http://www.gnu.org/licenses/gpl-3.0.html See the License for the specific
 * language governing permissions and limitations under the License.
 * 
 * When distributing the software, include this License Header Notice in each file.
 */
 
 package org.openconcerto.erp.core.finance.accounting.model;

import java.util.HashMap;
import java.util.Map;

public class Currency {

    private String code;

    public Currency(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static String[] ISO_CODES = { "AED", "AFN", "ALL", "AMD", "ANG", "AOA", "ARS", "AUD", "AWG", "AZN", "BAM", "BBD", "BDT", "BGN", "BHD", "BIF", "BMD", "BND", "BOB", "BRL", "BSD", "BTN",
            "BWP", "BYR", "BZD", "CAD", "CDF", "CHF", "CLP", "CNY", "COP", "CRC", "CUC", "CUP", "CVE", "CZK", "DJF", "DKK", "DOP", "DZD", "EGP", "ERN", "ETB", "EUR", "FJD", "FKP", "GBP", "GEL", "GGP",
            "GHS", "GIP", "GMD", "GNF", "GTQ", "GYD", "HKD", "HNL", "HRK", "HTG", "HUF", "IDR", "ILS", "IMP", "INR", "IQD", "IRR", "ISK", "JEP", "JMD", "JOD", "JPY", "KES", "KGS", "KHR", "KMF", "KPW",
            "KRW", "KWD", "KYD", "KZT", "LAK", "LBP", "LKR", "LRD", "LSL", "LYD", "MAD", "MDL", "MGA", "MKD", "MMK", "MNT", "MOP", "MRO", "MUR", "MVR", "MWK", "MXN", "MYR", "MZN", "NAD", "NGN", "NIO",
            "NOK", "NPR", "NZD", "OMR", "PAB", "PEN", "PGK", "PHP", "PKR", "PLN", "PYG", "QAR", "RON", "RSD", "RUB", "RWF", "SAR", "SBD", "SCR", "SDG", "SEK", "SGD", "SHP", "SLL", "SOS", "SPL", "SRD",
            "STD", "SVC", "SYP", "SZL", "THB", "TJS", "TMT", "TND", "TOP", "TRY", "TTD", "TVD", "TWD", "TZS", "UAH", "UGX", "USD", "UYU", "UZS", "VEF", "VND", "VUV", "WST", "XAF", "XCD", "XDR", "XOF",
            "XPF", "YER", "ZAR", "ZMW", "ZWD" };

    private static final Map<String, String> mapSymbol;

    static {
        mapSymbol = new HashMap<String, String>();
        mapSymbol.put("ALL", "Lek");
        mapSymbol.put("AED", "Dhs");
        mapSymbol.put("AFN", "؋");
        mapSymbol.put("ARS", "$");
        mapSymbol.put("AWG", "ƒ");
        mapSymbol.put("AUD", "$");
        mapSymbol.put("AZN", "ман");
        mapSymbol.put("BSD", "$");
        mapSymbol.put("BBD", "$");
        mapSymbol.put("BYR", "p.");
        mapSymbol.put("BZD", "BZ$");
        mapSymbol.put("BMD", "$");
        mapSymbol.put("BOB", "$b");
        mapSymbol.put("BAM", "KM");
        mapSymbol.put("BWP", "P");
        mapSymbol.put("BGN", "лв");
        mapSymbol.put("BRL", "R$");
        mapSymbol.put("BND", "$");
        mapSymbol.put("KHR", "៛");
        mapSymbol.put("CAD", "$");
        mapSymbol.put("KYD", "$");
        mapSymbol.put("CLP", "$");
        mapSymbol.put("CNY", "¥");
        mapSymbol.put("COP", "$");
        mapSymbol.put("CRC", "₡");
        mapSymbol.put("HRK", "kn");
        mapSymbol.put("CUP", "₱");
        mapSymbol.put("CZK", "Kč");
        mapSymbol.put("DZD", "D.A.");
        mapSymbol.put("DKK", "kr");
        mapSymbol.put("DOP", "RD$");
        mapSymbol.put("XCD", "$");
        mapSymbol.put("EGP", "£");
        mapSymbol.put("SVC", "$");
        mapSymbol.put("EEK", "kr");
        mapSymbol.put("EUR", "€");
        mapSymbol.put("FCFA", "F CFA");
        mapSymbol.put("FKP", "£");
        mapSymbol.put("FJD", "$");
        mapSymbol.put("GHC", "¢");
        mapSymbol.put("GIP", "£");
        mapSymbol.put("GTQ", "Q");
        mapSymbol.put("GGP", "£");
        mapSymbol.put("GYD", "$");
        mapSymbol.put("HNL", "L");
        mapSymbol.put("HKD", "$");
        mapSymbol.put("HUF", "Ft");
        mapSymbol.put("ISK", "kr");
        mapSymbol.put("INR", "");
        mapSymbol.put("IDR", "Rp");
        mapSymbol.put("IRR", "﷼");
        mapSymbol.put("IMP", "£");
        mapSymbol.put("ILS", "₪");
        mapSymbol.put("JMD", "J$");
        mapSymbol.put("JPY", "¥");
        mapSymbol.put("JEP", "£");
        mapSymbol.put("KZT", "лв");
        mapSymbol.put("KPW", "₩");
        mapSymbol.put("KRW", "₩");
        mapSymbol.put("KGS", "лв");
        mapSymbol.put("LAK", "₭");
        mapSymbol.put("LVL", "Ls");
        mapSymbol.put("LBP", "£");
        mapSymbol.put("LRD", "$");
        mapSymbol.put("LTL", "Lt");
        mapSymbol.put("MAD", "MAD");
        mapSymbol.put("MKD", "ден");
        mapSymbol.put("MYR", "RM");
        mapSymbol.put("MUR", "₨");
        mapSymbol.put("MXN", "$");
        mapSymbol.put("MNT", "₮");
        mapSymbol.put("MZN", "MT");
        mapSymbol.put("NAD", "$");
        mapSymbol.put("NPR", "₨");
        mapSymbol.put("ANG", "ƒ");
        mapSymbol.put("NZD", "$");
        mapSymbol.put("NIO", "C$");
        mapSymbol.put("NGN", "₦");
        mapSymbol.put("KPW", "₩");
        mapSymbol.put("NOK", "kr");
        mapSymbol.put("OMR", "﷼");
        mapSymbol.put("PKR", "₨");
        mapSymbol.put("PAB", "B/.");
        mapSymbol.put("PYG", "Gs");
        mapSymbol.put("PEN", "S/.");
        mapSymbol.put("PHP", "₱");
        mapSymbol.put("PLN", "zł");
        mapSymbol.put("QAR", "﷼");
        mapSymbol.put("RON", "lei");
        mapSymbol.put("RUB", "руб");
        mapSymbol.put("SHP", "£");
        mapSymbol.put("SAR", "﷼");
        mapSymbol.put("RSD", "Дин.");
        mapSymbol.put("SCR", "₨");
        mapSymbol.put("SGD", "$");
        mapSymbol.put("SBD", "$");
        mapSymbol.put("SOS", "S");
        mapSymbol.put("ZAR", "R");
        mapSymbol.put("KRW", "₩");
        mapSymbol.put("LKR", "₨");
        mapSymbol.put("SEK", "kr");
        mapSymbol.put("CHF", "CHF");
        mapSymbol.put("SRD", "$");
        mapSymbol.put("SYP", "£");
        mapSymbol.put("TWD", "NT$");
        mapSymbol.put("THB", "฿");
        mapSymbol.put("TTD", "TT$");
        mapSymbol.put("TRY", "");
        mapSymbol.put("TRL", "₤");
        mapSymbol.put("TVD", "$");
        mapSymbol.put("UAH", "₴");
        mapSymbol.put("GBP", "£");
        mapSymbol.put("USD", "$");
        mapSymbol.put("UYU", "$U");
        mapSymbol.put("XAF", "F CFA");
        mapSymbol.put("UZS", "лв");
        mapSymbol.put("VEF", "Bs");
        mapSymbol.put("VND", "₫");
        mapSymbol.put("YER", "﷼");
        mapSymbol.put("ZWD", "Z$");

    }

    public static String getSymbol(String code) {
        String symbol = mapSymbol.get(code);
        if (symbol == null) {
            symbol = "???";
        }
        return symbol;
    }

}
