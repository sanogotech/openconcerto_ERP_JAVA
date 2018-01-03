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
 
 package org.openconcerto.utils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.text.RuleBasedNumberFormat;

/**
 * 
 * @deprecated use {@link RuleBasedNumberFormat#SPELLOUT} or {@link MessageFormat} with
 *             <code>"{1,spellout}"</code>. To write currency amount :
 *             https://www.thebalance.com/write-numbers-using-words-4083198
 *             http://icu-project.org/docs/papers/a_rule_based_approach_to_number_spellout/
 *             http://icu-project.org/apiref/icu4j/com/ibm/icu/text/RuleBasedNumberFormat.html
 */
public class Nombre {

    public static int FR = 0;
    public static int EN = 1;
    public static int ES = 2;
    public static int PL = 3;
    static private final Locale[] LOCALES = { Locale.FRENCH, Locale.ENGLISH, new Locale("es"), new Locale("pl") };
    @Deprecated
    static private final Map<Locale, String> DECIMAL_SEP;

    static {
        DECIMAL_SEP = new HashMap<Locale, String>();
        DECIMAL_SEP.put(Locale.FRENCH, "et");
        DECIMAL_SEP.put(Locale.ENGLISH, "and");
        DECIMAL_SEP.put(LOCALES[2], "y");
        DECIMAL_SEP.put(LOCALES[3], "i");
    }

    private int nb;
    private final Locale locale;
    private final RuleBasedNumberFormat format;

    public Nombre(int i) {
        this(i, FR);
    }

    public Nombre(int i, int language) {
        this(i, LOCALES[language]);
    }

    public Nombre(int i, Locale l) {
        this.nb = i;
        this.locale = l;
        this.format = new RuleBasedNumberFormat(l, RuleBasedNumberFormat.SPELLOUT);
    }

    public String getText() {
        return this.format.format(this.nb);
    }

    public final String getSeparateurLabel() {
        return DECIMAL_SEP.get(this.locale);
    }
}
