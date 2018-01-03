package org.openconcerto.modules.reports.olap.formatter;

import java.text.DateFormatSymbols;

import mondrian.olap.Member;
import mondrian.spi.MemberFormatter;

public class MonthMemberFormatter implements MemberFormatter {
    private final String[] months;

    public MonthMemberFormatter() {
        months = new DateFormatSymbols().getMonths();
    }

    @Override
    public String formatMember(Member arg0) {
        return months[Integer.parseInt(arg0.getName()) - 1];
    }

}
