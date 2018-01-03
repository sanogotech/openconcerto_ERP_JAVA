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
 
 package org.openconcerto.erp.core.supplychain.receipt.element;

import org.openconcerto.erp.config.Gestion;
import org.openconcerto.erp.core.common.component.TransfertBaseSQLComponent;
import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.erp.core.supplychain.order.component.SaisieAchatSQLComponent;
import org.openconcerto.erp.core.supplychain.receipt.component.BonReceptionSQLComponent;
import org.openconcerto.erp.generationDoc.gestcomm.BonReceptionXmlSheet;
import org.openconcerto.erp.model.MouseSheetXmlListeListener;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.element.TreesOfSQLRows;
import org.openconcerto.sql.model.AliasedTable;
import org.openconcerto.sql.model.SQLName;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.request.UpdateBuilder;
import org.openconcerto.sql.view.EditFrame;
import org.openconcerto.sql.view.list.IListe;
import org.openconcerto.sql.view.list.IListeAction.IListeEvent;
import org.openconcerto.sql.view.list.RowAction.PredicateRowAction;
import org.openconcerto.utils.ListMap;

import java.awt.event.ActionEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JFrame;

import org.apache.commons.dbutils.handlers.ArrayListHandler;

public class BonReceptionSQLElement extends ComptaSQLConfElement {

    public BonReceptionSQLElement() {
        super("BON_RECEPTION", "un bon de réception", "Bons de réception");

        PredicateRowAction actionsTRFA = new PredicateRowAction(new AbstractAction("Transfert vers facture fournisseur") {
            public void actionPerformed(ActionEvent e) {
                TransfertBaseSQLComponent.openTransfertFrame(IListe.get(e).getSelectedRows(), "FACTURE_FOURNISSEUR");
            }
        }, true);
        actionsTRFA.setPredicate(IListeEvent.getNonEmptySelectionPredicate());

        PredicateRowAction actionTRSimple = new PredicateRowAction(new AbstractAction("Transfert vers facture simple") {
            public void actionPerformed(ActionEvent e) {
                transfertFacture(IListe.get(e).getSelectedRow().getID());
            }
        }, false);
        actionTRSimple.setPredicate(IListeEvent.getSingleSelectionPredicate());

        getRowActions().add(actionsTRFA);
        getRowActions().add(actionTRSimple);

        MouseSheetXmlListeListener mouseSheetXmlListeListener = new MouseSheetXmlListeListener(BonReceptionXmlSheet.class);
        mouseSheetXmlListeListener.setGenerateHeader(true);
        mouseSheetXmlListeListener.setShowHeader(true);
        getRowActions().addAll(mouseSheetXmlListeListener.getRowActions());
    }

    @Override
    public ListMap<String, String> getShowAs() {
        return ListMap.singleton(null, "NUMERO", "DATE");
    }

    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NUMERO");
        l.add("DATE");
        l.add("ID_FOURNISSEUR");
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

    /*
     * (non-Javadoc)
     * 
     * @see org.openconcerto.devis.SQLElement#getComponent()
     */
    public SQLComponent createComponent() {
        return new BonReceptionSQLComponent();
    }

    /**
     * Transfert d'un BR en facture
     * 
     * @param brID
     */
    public void transfertFacture(int brID) {

        SQLElement elt = Configuration.getInstance().getDirectory().getElement("SAISIE_ACHAT");
        EditFrame editFactureFrame = new EditFrame(elt);
        editFactureFrame.setIconImage(new ImageIcon(Gestion.class.getResource("frameicon.png")).getImage());

        SaisieAchatSQLComponent comp = (SaisieAchatSQLComponent) editFactureFrame.getSQLComponent();

        // comp.setDefaults();
        comp.loadBonReception(brID);
        editFactureFrame.pack();
        editFactureFrame.setState(JFrame.NORMAL);
        editFactureFrame.setVisible(true);
    }

    public List<Object> getCmdFrom(int brOrigin) {
        SQLTable tableBRElement = getTable().getTable("BON_RECEPTION_ELEMENT");
        SQLTable tableCmdElement = getTable().getTable("COMMANDE_ELEMENT");
        String up = "SELECT DISTINCT c2.\"ID_COMMANDE\" FROM " + new SQLName(tableBRElement.getDBRoot().getName(), tableBRElement.getName()).quote() + " b2, "
                + new SQLName(tableCmdElement.getDBRoot().getName(), tableCmdElement.getName()).quote() + " c2 WHERE b2.\"ID_BON_RECEPTION\"=" + brOrigin
                + " AND c2.\"ARCHIVE\"=0 AND b2.\"ARCHIVE\"=0 AND c2.\"ID\">1 AND b2.\"ID\">1 AND b2.\"ID_COMMANDE_ELEMENT\"=c2.\"ID\"";
        List<Object> cmds = getTable().getDBSystemRoot().getDataSource().executeCol(up);
        return cmds;
    }

    public void updateCmdElement(List<Object> cmds, int idbrOrigin) {
        SQLTable tableBRElement = getTable().getTable("BON_RECEPTION_ELEMENT");
        SQLTable tableCmdElement = getTable().getTable("COMMANDE_ELEMENT");
        UpdateBuilder build = new UpdateBuilder(tableCmdElement);
        build.set("QTE_RECUE", "(SELECT SUM(b.\"QTE\" * b.\"QTE_UNITAIRE\") from " + new SQLName(tableBRElement.getDBRoot().getName(), tableBRElement.getName()).quote()
                + " b where c.\"ID\"=b.\"ID_COMMANDE_ELEMENT\" AND c.\"ID\">1 AND c.\"ARCHIVE\"=0 AND b.\"ID\">1 AND b.\"ARCHIVE\"=0 )");
        AliasedTable alias = new AliasedTable(tableCmdElement, "c");
        build.setWhere(new Where(alias.getField("ID_COMMANDE"), cmds));
        getTable().getDBSystemRoot().getDataSource().execute(build.asString().replaceAll(" SET", " c SET "));
    }

    @Override
    protected void archive(TreesOfSQLRows trees, boolean cutLinks) throws SQLException {
        List<Object> cmds = null;
        List<Integer> ids = new ArrayList<Integer>();
        for (SQLRow row : trees.getRows()) {
            cmds = getCmdFrom(row.getID());
            ids.add(row.getID());

            // Mise à jour des stocks
            SQLElement eltMvtStock = Configuration.getInstance().getDirectory().getElement("MOUVEMENT_STOCK");
            SQLSelect sel = new SQLSelect(eltMvtStock.getTable().getBase());
            sel.addSelect(eltMvtStock.getTable().getField("ID"));
            Where w = new Where(eltMvtStock.getTable().getField("IDSOURCE"), "=", row.getID());
            Where w2 = new Where(eltMvtStock.getTable().getField("SOURCE"), "=", getTable().getName());
            sel.setWhere(w.and(w2));

            List l = (List) eltMvtStock.getTable().getBase().getDataSource().execute(sel.asString(), new ArrayListHandler());
            if (l != null) {
                for (int i = 0; i < l.size(); i++) {
                    Object[] tmp = (Object[]) l.get(i);
                    eltMvtStock.archive(((Number) tmp[0]).intValue());
                }
            }
        }
        super.archive(trees, cutLinks);
        for (Integer id : ids) {

            updateCmdElement(cmds, id);
        }
    }
}
