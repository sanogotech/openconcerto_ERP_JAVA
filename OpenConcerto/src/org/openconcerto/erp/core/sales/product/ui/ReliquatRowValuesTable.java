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
 
 package org.openconcerto.erp.core.sales.product.ui;

import java.util.List;
import java.util.Vector;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.UndefinedRowValuesCache;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.sqlobject.ITextWithCompletion;
import org.openconcerto.sql.view.list.AutoCompletionManager;
import org.openconcerto.sql.view.list.RowValuesTable;
import org.openconcerto.sql.view.list.RowValuesTableModel;
import org.openconcerto.sql.view.list.RowValuesTablePanel;
import org.openconcerto.sql.view.list.SQLTableElement;
import org.openconcerto.sql.view.list.ValidStateChecker;

public class ReliquatRowValuesTable extends RowValuesTablePanel {

    private final String tableName;

    public ReliquatRowValuesTable(String tableName) {
        this.tableName = tableName;
        init();
        uiInit();
    }

    /**
     * 
     */
    protected void init() {

        final SQLElement e = getSQLElement();

        final List<SQLTableElement> list = new Vector<SQLTableElement>();

        final SQLTableElement article = new SQLTableElement(e.getTable().getField("ID_ARTICLE"));
        list.add(article);
        // FIXME Add filter sur article BL Element source

        // Qté
        final SQLTableElement tableQte = new SQLTableElement(e.getTable().getField("QTE"));
        list.add(tableQte);

        final SQLTableElement tableQteU = new SQLTableElement(e.getTable().getField("QTE_UNITAIRE"));
        list.add(tableQteU);

        final SQLTableElement tableUnite = new SQLTableElement(e.getTable().getField("ID_UNITE_VENTE"));
        tableUnite.setEditable(false);
        list.add(tableUnite);
        this.defaultRowVals = new SQLRowValues(UndefinedRowValuesCache.getInstance().getDefaultRowValues(e.getTable()));
        this.model = new RowValuesTableModel(e, list, e.getTable().getField("QTE"), false, this.defaultRowVals);

        this.table = new RowValuesTable(this.model, null);

        // Autocompletion

        AutoCompletionManager m = new AutoCompletionManager(article, ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete().getField("ARTICLE.NOM"), this.table,
                this.table.getRowValuesTableModel(), ITextWithCompletion.MODE_CONTAINS, true, true, new ValidStateChecker());
        m.fill("ID_UNITE_VENTE", "ID_UNITE_VENTE");
        m.fill("ID", "ID_ARTICLE");
        final SQLTable sqlTableArticle = ((ComptaPropsConfiguration) Configuration.getInstance()).getRootSociete().getTable("ARTICLE");
        final Where w = new Where(sqlTableArticle.getField("OBSOLETE"), "=", Boolean.FALSE);
        m.setWhere(w);
    }

    public SQLElement getSQLElement() {
        return Configuration.getInstance().getDirectory().getElement(this.tableName);
    }

}
