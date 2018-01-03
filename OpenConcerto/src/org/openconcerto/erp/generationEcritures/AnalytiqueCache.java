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
 
 package org.openconcerto.erp.generationEcritures;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowListRSH;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class AnalytiqueCache {

    static private final SQLSelect getSelPoste() {
        final DBRoot root = ((ComptaPropsConfiguration) Configuration.getInstance()).getRootSociete();
        final SQLTable table = root.getTable("POSTE_ANALYTIQUE");
        final SQLSelect sel = new SQLSelect();
        sel.addSelect(table.getKey());
        sel.addSelect(table.getField("NOM"));
        sel.addSelect(table.getField("ID_AXE_ANALYTIQUE"));
        sel.addSelect(table.getField("DEFAULT"));
        return sel;
    }

    static private final SQLSelect getSelAxe() {
        final DBRoot root = ((ComptaPropsConfiguration) Configuration.getInstance()).getRootSociete();
        final SQLTable table = root.getTable("AXE_ANALYTIQUE");
        final SQLSelect sel = new SQLSelect();
        sel.addSelect(table.getKey());
        sel.addSelect(table.getField("NOM"));
        return sel;
    }

    private transient final Map<Integer, SQLRowAccessor> mapPoste = new HashMap<Integer, SQLRowAccessor>();
    private final List<SQLRow> axes;
    private static AnalytiqueCache instance;
    private transient Map<Integer, SQLRow> defaultPoste = new HashMap<Integer, SQLRow>();

    private AnalytiqueCache() {
        {
            final SQLSelect sel = getSelPoste();

            final List<SQLRow> l = SQLRowListRSH.execute(sel);
            for (SQLRow sqlRow : l) {
                this.mapPoste.put(sqlRow.getID(), sqlRow);
                if (sqlRow.getBoolean("DEFAULT")) {
                    this.defaultPoste.put(sqlRow.getForeignID("ID_AXE_ANALYTIQUE"), sqlRow);
                }
            }
        }
        {
            this.axes = new ArrayList<SQLRow>();
            final SQLSelect sel = getSelAxe();

            final List<SQLRow> l = SQLRowListRSH.execute(sel);
            for (SQLRow sqlRow : l) {
                this.axes.add(sqlRow);
            }
        }

    }

    public Collection<SQLRow> getAxes() {
        return Collections.unmodifiableCollection(axes);
    }

    synchronized public static AnalytiqueCache getCache() {
        if (instance == null) {
            instance = new AnalytiqueCache();
        }
        return instance;
    }

    public SQLRowAccessor getPosteFromId(final int id) {
        return this.mapPoste.get(Integer.valueOf(id));
    }

    public SQLRow getDefaultPoste(int idAxe) {

        return this.defaultPoste.get(idAxe);
    }
}
