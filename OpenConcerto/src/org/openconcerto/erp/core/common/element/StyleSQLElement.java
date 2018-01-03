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
 
 package org.openconcerto.erp.core.common.element;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.element.UISQLComponent;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTableEvent;
import org.openconcerto.sql.model.SQLTableModifiedListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JTextField;

public class StyleSQLElement extends ComptaSQLConfElement {
    private Map<String, Integer> allStyleByName;

    public StyleSQLElement() {
        super("STYLE", "un style", "styles");
    }

    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("CODE");
        l.add("NOM");
        return l;
    }

    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NOM");
        return l;
    }

    @SuppressWarnings("unchecked")
    public static final Map<String, Map<Integer, String>> getMapAllStyle() {
        Map<String, Map<Integer, String>> m = new HashMap<String, Map<Integer, String>>();
        SQLBase base = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete();
        SQLSelect sel = new SQLSelect(base);
        sel.addSelect(base.getField("STYLE.NOM"));
        String req = sel.asString();
        List<Map<String, Object>> l = base.getDataSource().execute(req);

        for (Map<String, Object> map : l) {

            Object o = map.get("NOM");
            String s = (o == null) ? null : o.toString();
            m.put(s, null);
        }
        m.put("BlankStyle", null);
        return m;
    }

    public synchronized final Map<String, Integer> getAllStyleByName() {
        if (this.allStyleByName == null) {
            this.allStyleByName = fetchAllStyleByName();
            getTable().addTableModifiedListener(new SQLTableModifiedListener() {

                @Override
                public void tableModified(SQLTableEvent evt) {
                    synchronized (StyleSQLElement.this) {
                        allStyleByName = fetchAllStyleByName();
                    }

                }
            });
        }
        return this.allStyleByName;
    }

    @SuppressWarnings("unchecked")
    public final Map<String, Integer> fetchAllStyleByName() {
        Map<String, Integer> m = new HashMap<String, Integer>();
        SQLSelect sel = new SQLSelect();
        sel.addSelect(getTable().getField("NOM"));
        sel.addSelect(getTable().getKey());
        String req = sel.asString();
        List<Map<String, Object>> l = getTable().getDBSystemRoot().getDataSource().execute(req);
        for (Map<String, Object> map : l) {
            m.put((String) map.get("NOM"), ((Number) map.get(getTable().getKey().getName())).intValue());
        }
        return m;
    }

    @SuppressWarnings("unchecked")
    public final Map<Integer, String> getAllStyleByID() {
        Map<Integer, String> m = new HashMap<Integer, String>();
        SQLSelect sel = new SQLSelect();
        sel.addSelect(getTable().getKey());
        sel.addSelect(getTable().getField("NOM"));
        String req = sel.asString();
        List<Map<String, Object>> l = getTable().getDBSystemRoot().getDataSource().execute(req);

        for (Map<String, Object> map : l) {
            m.put(((Number) map.get(getTable().getKey().getName())).intValue(), (String) map.get("NOM"));
        }

        return m;
    }

    public SQLComponent createComponent() {
        return new UISQLComponent(this) {
            public void addViews() {
                this.addRequiredSQLObject(new JTextField(), "NOM", "left");
                this.addRequiredSQLObject(new JTextField(), "CODE", "right");
            }
        };
    }

    @Override
    protected String createCode() {
        return "style";
    }
}
