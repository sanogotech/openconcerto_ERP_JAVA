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
 
 package org.openconcerto.erp.config;

import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowListRSH;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DSNUpdateRubrique {

    final DBRoot root;

    public DSNUpdateRubrique(DBRoot root) {
        this.root = root;
    }

    public void updateRubriqueCotisation() throws SQLException {

        final SQLTable tableCodeBase = this.root.getTable("CODE_BASE_ASSUJETTIE");
        SQLSelect selCodeBase = new SQLSelect();
        selCodeBase.addSelectStar(tableCodeBase);
        List<SQLRow> rowsCodeBase = SQLRowListRSH.execute(selCodeBase);
        Map<String, SQLRow> mapCodeBase = new HashMap<String, SQLRow>();
        for (SQLRow sqlRow : rowsCodeBase) {

            final String string = sqlRow.getString("CODE");
            mapCodeBase.put(string, sqlRow);
        }

        final SQLTable tableCodeTP = this.root.getTable("CODE_CAISSE_TYPE_RUBRIQUE");
        SQLSelect selCodeTP = new SQLSelect();
        selCodeTP.addSelectStar(tableCodeTP);
        List<SQLRow> rowsCodeTP = SQLRowListRSH.execute(selCodeTP);
        Map<String, SQLRow> mapCodeTP = new HashMap<String, SQLRow>();
        for (SQLRow sqlRow : rowsCodeTP) {
            final String string = sqlRow.getString("CODE");
            mapCodeTP.put(string, sqlRow);
        }

        Map<String, String> liaisonCTP = new HashMap<String, String>();
        liaisonCTP.put("COTCSA", "100");
        liaisonCTP.put("COTMALADIE", "100");
        liaisonCTP.put("COTAF", "100");
        liaisonCTP.put("COTVEUV", "100");
        liaisonCTP.put("COTVIEIL", "100");
        liaisonCTP.put("COTVIEILPLAF", "100");
        liaisonCTP.put("COTFNALPLAF", "332");
        liaisonCTP.put("COTFNAL", "230");
        liaisonCTP.put("COTAT", "100");
        liaisonCTP.put("COTVTRAN", "900");
        liaisonCTP.put("COTCH", "772");
        liaisonCTP.put("COTCHAGS", "937");
        liaisonCTP.put("COTCSGIMP", "260");
        liaisonCTP.put("COTCSGDED", "260");
        liaisonCTP.put("COTFILLON", "671");
        liaisonCTP.put("COTSYNDIC", "027");

        Map<String, String> liaisonCodeBase = new HashMap<String, String>();
        liaisonCodeBase.put("COTCSA", "03");
        liaisonCodeBase.put("COTMALADIE", "03");
        liaisonCodeBase.put("COTAF", "03");
        liaisonCodeBase.put("COTVEUV", "03");
        liaisonCodeBase.put("COTVIEIL", "03");
        liaisonCodeBase.put("COTVIEILPLAF", "02");
        liaisonCodeBase.put("COTFNALPLAF", "02");
        liaisonCodeBase.put("COTFNAL", "03");
        liaisonCodeBase.put("COTAT", "03");
        liaisonCodeBase.put("COTVTRAN", "03");
        liaisonCodeBase.put("COTCH", "07");
        liaisonCodeBase.put("COTCHAGS", "03");
        liaisonCodeBase.put("COTCSGIMP", "03");
        liaisonCodeBase.put("COTCSGDED", "03");
        liaisonCodeBase.put("COTFILLON", "02");
        liaisonCodeBase.put("COTSYNDIC", "03");

        List<String> cotPlafonne = Arrays.asList("COTVIEILPLAF", "COTFNALPLAF", "COTFILLON");

        final SQLTable table = this.root.getTable("RUBRIQUE_COTISATION");
        SQLSelect sel = new SQLSelect();
        sel.addSelectStar(table);
        List<SQLRow> rows = SQLRowListRSH.execute(sel);
        for (SQLRow sqlRow : rows) {
            String codeRub = sqlRow.getString("CODE");
            if (liaisonCTP.keySet().contains(codeRub) && sqlRow.isForeignEmpty("ID_CODE_CAISSE_TYPE_RUBRIQUE")) {
                sqlRow.createEmptyUpdateRow().put("ID_CODE_CAISSE_TYPE_RUBRIQUE", mapCodeTP.get(liaisonCTP.get(codeRub)).getID()).commit();
            }
            if (cotPlafonne.contains(codeRub)) {
                sqlRow.createEmptyUpdateRow().put("ASSIETTE_PLAFONNEE", Boolean.TRUE).commit();
            }
            if (liaisonCodeBase.keySet().contains(codeRub) && sqlRow.isForeignEmpty("ID_CODE_BASE_ASSUJETTIE")) {
                sqlRow.createEmptyUpdateRow().put("ID_CODE_BASE_ASSUJETTIE", mapCodeBase.get(liaisonCodeBase.get(codeRub)).getID()).commit();
            }
        }
    }

}
