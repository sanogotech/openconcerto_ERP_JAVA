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
 
 package org.openconcerto.utils.io;

import org.openconcerto.utils.NumberUtils;
import org.openconcerto.utils.XMLDateFormat;

import java.awt.Color;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.Format;
import java.util.Calendar;
import java.util.Date;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;

public class JSONConverter {

    static private final Format DF = new XMLDateFormat();

    static synchronized private final String format(final Date d) {
        return DF.format(d);
    }

    public synchronized static String formatCalendar(final Calendar calendar) {
        return DF.format(calendar);
    }

    static synchronized private final Date parse(final String s) throws java.text.ParseException {
        return (Date) DF.parseObject(s);
    }

    public static Object getJSON(Object param) {
        Object result = null;

        if (param != null) {
            if (param instanceof HTMLable) {
                result = ((HTMLable) param).getHTML();
            } else if (param instanceof JSONAble) {
                result = ((JSONAble) param).toJSON();
            } else if (param instanceof Date) {
                result = format((Date) param);
            } else if (param instanceof Calendar) {
                result = formatCalendar(((Calendar) param));
            } else if (param instanceof Class<?>) {
                result = ((Class<?>) param).getName();
            } else if (param instanceof Iterable) {
                final Iterable<?> tmp = (Iterable<?>) param;
                final JSONArray jsonArray = new JSONArray();
                for (Object o : tmp) {
                    jsonArray.add(getJSON(o));
                }
                result = jsonArray;
            } else if (param instanceof Color) {
                if (param != null) {
                    final Color paramColor = (Color) param;
                    final JSONObject jsonColor = new JSONObject();
                    jsonColor.put("r", paramColor.getRed());
                    jsonColor.put("g", paramColor.getGreen());
                    jsonColor.put("b", paramColor.getBlue());
                    result = jsonColor;
                }
            } else if (param instanceof BigDecimal) {
                result = ((BigDecimal) param).doubleValue();
            } else {
                result = param;
            }
        }

        return result;
    }

    public static <T> T getObjectFromJSON(final Object o, final Class<T> type) {
        final T result;
        if (o != null && !o.equals("null")) {
            if (type.isInstance(o)) {
                result = type.cast(o);
            } else if (type.equals(Integer.class)) {
                final int intVal;
                if (o instanceof BigDecimal) {
                    intVal = ((BigDecimal) o).intValueExact();
                } else if (o instanceof BigInteger) {
                    // TODO use intValueExact() in Java 8
                    final BigInteger bigInt = (BigInteger) o;
                    if (bigInt.compareTo(BigInteger.valueOf(Integer.MIN_VALUE)) < 0 || bigInt.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0)
                        throw new IllegalArgumentException("object (" + o.getClass().getName() + ") is not assignable for '" + type + "'");
                    intVal = bigInt.intValue();
                } else {
                    try {
                        intVal = NumberUtils.ensureInt((Long) o);
                    } catch (ArithmeticException ex) {
                        throw new IllegalArgumentException("object (" + o.getClass().getName() + ") is not assignable for '" + type + "'", ex);
                    }
                }
                result = type.cast(intVal);
            } else if (type.equals(Date.class)) {
                final String sparam = (String) o;
                try {
                    final Date c = parse(sparam);
                    result = type.cast(c);
                } catch (java.text.ParseException e) {
                    throw new IllegalArgumentException("object (" + o.getClass().getName() + ") is not assignable for '" + type + "', the format is not valid", e);
                }
            } else if (type.equals(Color.class)) {
                final JSONObject jsonColor = (JSONObject) o;
                final int r = JSONConverter.getParameterFromJSON(jsonColor, "r", Integer.class);
                final int g = JSONConverter.getParameterFromJSON(jsonColor, "g", Integer.class);
                final int b = JSONConverter.getParameterFromJSON(jsonColor, "b", Integer.class);
                result = type.cast(new Color(r, g, b));
            } else {
                result = type.cast(o);
            }
        } else {
            result = null;
        }

        return result;
    }

    public static <T> T getParameterFromJSON(final JSONObject json, final String key, final Class<T> type) {
        return getParameterFromJSON(json, key, type, null);
    }

    public static <T> T getParameterFromJSON(final JSONObject json, final String key, final Class<T> type, T defaultValue) {
        return json.containsKey(key) ? getObjectFromJSON(json.get(key), type) : defaultValue;
    }

    public static JSONObject convertStringToJsonObject(final String jsonString) {
        final JSONParser parser = new JSONParser(JSONParser.USE_HI_PRECISION_FLOAT);
        final JSONObject json;
        try {
            json = (JSONObject) parser.parse(jsonString);
        } catch (final ParseException ex) {
            throw new IllegalArgumentException(ex.getMessage(), ex);
        }
        return json;
    }

    public static JSONArray convertStringToJsonArray(final String jsonString) {
        final JSONParser parser = new JSONParser(JSONParser.USE_HI_PRECISION_FLOAT);
        final JSONArray json;
        try {
            json = (JSONArray) parser.parse(jsonString);
        } catch (final ParseException ex) {
            throw new IllegalArgumentException(ex.getMessage(), ex);
        }
        return json;
    }
}
