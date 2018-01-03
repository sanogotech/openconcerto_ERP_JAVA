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
 
 package org.openconcerto.erp.core.sales.shipment.element;

import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.erp.core.sales.shipment.component.BonDeLivraisonSQLComponent;
import org.openconcerto.erp.preferences.DefaultNXProps;
import org.openconcerto.erp.preferences.GestionArticleGlobalPreferencePanel;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.element.SQLElementLink.LinkType;
import org.openconcerto.sql.element.SQLElementLinksSetup;
import org.openconcerto.sql.element.TreesOfSQLRows;
import org.openconcerto.sql.model.AliasedTable;
import org.openconcerto.sql.model.SQLName;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.preferences.SQLPreferences;
import org.openconcerto.sql.request.UpdateBuilder;
import org.openconcerto.ui.preferences.DefaultProps;
import org.openconcerto.utils.ListMap;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.dbutils.handlers.ArrayListHandler;

public class BonDeLivraisonSQLElement extends ComptaSQLConfElement {

    // TODO afficher uniquement les factures non livrees dans la combo
    // MAYBE mettre un niceCellRenderer dans les rowValuesTable

    public BonDeLivraisonSQLElement(String single, String plural) {
        super("BON_DE_LIVRAISON", single, plural);
    }

    public BonDeLivraisonSQLElement() {
        this("un bon de livraison", "Bons de livraison");
    }

    @Override
    protected void setupLinks(SQLElementLinksSetup links) {
        super.setupLinks(links);
        if (getTable().contains("ID_ADRESSE")) {
            links.get("ID_ADRESSE").setType(LinkType.ASSOCIATION);
        }
        if (getTable().contains("ID_ADRESSE_LIVRAISON")) {
            links.get("ID_ADRESSE_LIVRAISON").setType(LinkType.ASSOCIATION);
        }
    }

    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NUMERO");
        l.add("DATE");
        l.add("ID_CLIENT");
        DefaultProps props = DefaultNXProps.getInstance();
        Boolean b = props.getBooleanValue("ArticleShowPoids");
        if (b) {
            l.add("TOTAL_POIDS");
        }
        l.add("NOM");
        l.add("TOTAL_HT");
        l.add("INFOS");
        return l;
    }

    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NUMERO");
        l.add("DATE");
        return l;
    }

    @Override
    public ListMap<String, String> getShowAs() {
        return ListMap.singleton(null, "NUMERO", "DATE");
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openconcerto.devis.SQLElement#getComponent()
     */
    public SQLComponent createComponent() {
        return new BonDeLivraisonSQLComponent();
    }

    public List<Object> getCmdClientFrom(int blOrigin) {
        SQLTable tableBLElement = getTable().getTable("BON_DE_LIVRAISON_ELEMENT");
        SQLTable tableCmdElement = getTable().getTable("COMMANDE_CLIENT_ELEMENT");
        String up = "SELECT DISTINCT c2.\"ID_COMMANDE_CLIENT\" FROm " + new SQLName(tableBLElement.getDBRoot().getName(), tableBLElement.getName()).quote() + " b2, "
                + new SQLName(tableCmdElement.getDBRoot().getName(), tableCmdElement.getName()).quote() + " c2 WHERE b2.\"ID_BON_DE_LIVRAISON\"=" + blOrigin
                + " AND c2.\"ARCHIVE\"=0 AND b2.\"ARCHIVE\"=0 AND c2.\"ID\">1 AND b2.\"ID\">1 AND b2.\"ID_COMMANDE_CLIENT_ELEMENT\"=c2.\"ID\"";
        List<Object> cmds = getTable().getDBSystemRoot().getDataSource().executeCol(up);
        return cmds;
    }

    public void updateCmdClientElement(List<Object> cmds, int idblOrigin) {
        SQLTable tableBLElement = getTable().getTable("BON_DE_LIVRAISON_ELEMENT");
        SQLTable tableCmdElement = getTable().getTable("COMMANDE_CLIENT_ELEMENT");
        UpdateBuilder build = new UpdateBuilder(tableCmdElement);
        build.set("QTE_LIVREE", "(SELECT SUM(b.\"QTE_LIVREE\" * b.\"QTE_UNITAIRE\") from " + new SQLName(tableBLElement.getDBRoot().getName(), tableBLElement.getName()).quote()
                + " b where c.\"ID\"=b.\"ID_COMMANDE_CLIENT_ELEMENT\" AND c.\"ID\">1 AND c.\"ARCHIVE\"=0 AND b.\"ID\">1 AND b.\"ARCHIVE\"=0 )");
        AliasedTable alias = new AliasedTable(tableCmdElement, "c");
        build.setWhere(new Where(alias.getField("ID_COMMANDE_CLIENT"), cmds));

        getTable().getDBSystemRoot().getDataSource().execute(build.asString().replaceAll(" SET", " c SET "));
    }

    @Override
    protected void archive(TreesOfSQLRows trees, boolean cutLinks) throws SQLException {

        List<Object> cmds = null;
        List<Integer> ids = new ArrayList<Integer>();
        for (SQLRow row : trees.getRows()) {

            SQLPreferences prefs = new SQLPreferences(getTable().getDBRoot());

            cmds = getCmdClientFrom(row.getID());
            ids.add(row.getID());
            if (!prefs.getBoolean(GestionArticleGlobalPreferencePanel.STOCK_FACT, true)) {

                // Mise à jour des stocks
                SQLElement eltMvtStock = Configuration.getInstance().getDirectory().getElement("MOUVEMENT_STOCK");
                SQLSelect sel = new SQLSelect();
                sel.addSelect(eltMvtStock.getTable().getField("ID"));
                Where w = new Where(eltMvtStock.getTable().getField("IDSOURCE"), "=", row.getID());
                Where w2 = new Where(eltMvtStock.getTable().getField("SOURCE"), "=", getTable().getName());
                sel.setWhere(w.and(w2));

                @SuppressWarnings("unchecked")
                List l = (List) eltMvtStock.getTable().getBase().getDataSource().execute(sel.asString(), new ArrayListHandler());
                if (l != null) {
                    for (int i = 0; i < l.size(); i++) {
                        Object[] tmp = (Object[]) l.get(i);
                        eltMvtStock.archive(((Number) tmp[0]).intValue());
                    }
                }
            }
        }
        super.archive(trees, cutLinks);
        for (Integer id : ids) {

            updateCmdClientElement(cmds, id);
        }
    }
}
