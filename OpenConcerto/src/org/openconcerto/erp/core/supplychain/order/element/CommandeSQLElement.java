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
 
 package org.openconcerto.erp.core.supplychain.order.element;

import org.openconcerto.erp.config.Gestion;
import org.openconcerto.erp.core.common.component.TransfertBaseSQLComponent;
import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.erp.core.supplychain.order.component.CommandeSQLComponent;
import org.openconcerto.erp.core.supplychain.order.component.SaisieAchatSQLComponent;
import org.openconcerto.erp.core.supplychain.receipt.component.BonReceptionSQLComponent;
import org.openconcerto.erp.generationDoc.gestcomm.CommandeXmlSheet;
import org.openconcerto.erp.model.MouseSheetXmlListeListener;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.element.TreesOfSQLRows;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLRowValuesListFetcher;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLSelectJoin;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.view.EditFrame;
import org.openconcerto.sql.view.list.IListe;
import org.openconcerto.sql.view.list.IListeAction.IListeEvent;
import org.openconcerto.sql.view.list.RowAction.PredicateRowAction;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.cc.ITransformer;

import java.awt.event.ActionEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JFrame;

import org.apache.commons.dbutils.handlers.ArrayListHandler;

public class CommandeSQLElement extends ComptaSQLConfElement {

    public CommandeSQLElement() {
        super("COMMANDE", "une commande fournisseur", "commandes fournisseur");

        getRowActions().addAll(new MouseSheetXmlListeListener(CommandeXmlSheet.class).getRowActions());

        // Transfert vers BR
        PredicateRowAction bonAction = new PredicateRowAction(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                final List<SQLRowValues> selectedRows = IListe.get(e).getSelectedRows();
                transfertBR(selectedRows);
            }

        }, false, "supplychain.order.create.receipt");

        bonAction.setPredicate(IListeEvent.getSingleSelectionPredicate());
        getRowActions().add(bonAction);

        // Transfert vers facture
        PredicateRowAction factureAction = new PredicateRowAction(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {

                CommandeSQLElement.this.transfertFacture(IListe.get(e).getSelectedRow().getID());
            }
        }, false, "supplychain.order.create.purchase");
        factureAction.setPredicate(IListeEvent.getSingleSelectionPredicate());
        getRowActions().add(factureAction);

        PredicateRowAction tagValidAction = new PredicateRowAction(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {

                final SQLRowValues asRowValues = IListe.get(e).getSelectedRow().asRow().createEmptyUpdateRow();
                asRowValues.put("EN_COURS", Boolean.FALSE);
                try {
                    asRowValues.commit();
                } catch (SQLException e1) {
                    ExceptionHandler.handle("Une erreur est survenue pour notifier la commande valider", e1);
                }
            }
        }, false, "supplychain.order.valid");
        tagValidAction.setPredicate(IListeEvent.getSingleSelectionPredicate());
        getRowActions().add(tagValidAction);
    }

    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NUMERO");
        l.add("NOM");
        l.add("DATE");
        l.add("ID_FOURNISSEUR");
        l.add("T_HT");
        l.add("T_TTC");
        l.add("EN_COURS");
        l.add("INFOS");
        return l;
    }

    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NUMERO");
        l.add("NOM");
        l.add("DATE");
        return l;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openconcerto.devis.SQLElement#getComponent()
     */
    public SQLComponent createComponent() {
        return new CommandeSQLComponent();
    }

    public void transfertBR(final List<SQLRowValues> selectedRows) {

        EditFrame f = TransfertBaseSQLComponent.openTransfertFrame(selectedRows, "BON_RECEPTION");
        BonReceptionSQLComponent comp = (BonReceptionSQLComponent) f.getSQLComponent();
        final SQLTable tableElt = comp.getElement().getTable().getTable("BON_RECEPTION_ELEMENT");
        SQLRowValues rowVals = new SQLRowValues(tableElt);
        rowVals.put("QTE_UNITAIRE", null);
        rowVals.put("QTE", null);
        rowVals.put("ID_ARTICLE", null);

        SQLRowValuesListFetcher fetcher = SQLRowValuesListFetcher.create(rowVals);
        fetcher.setSelTransf(new ITransformer<SQLSelect, SQLSelect>() {

            @Override
            public SQLSelect transformChecked(SQLSelect input) {
                List<Integer> ids = new ArrayList<Integer>(selectedRows.size());
                for (SQLRowValues sqlRowValues : selectedRows) {
                    ids.add(sqlRowValues.getID());
                }
                SQLSelectJoin joinBR = input.addJoin("RIGHT", tableElt.getTable("BON_RECEPTION_ELEMENT").getField("ID_BON_RECEPTION"));
                SQLSelectJoin joinTR = input.addBackwardJoin("RIGHT", tableElt.getTable("TR_COMMANDE").getField("ID_BON_RECEPTION"), joinBR.getJoinedTable().getAlias());
                joinTR.setWhere(new Where(joinTR.getJoinedTable().getField("ID_COMMANDE"), ids));
                System.err.println(input.asString());
                return input;
            }
        });
        comp.loadQuantity(fetcher.fetch());
    }

    /**
     * Transfert d'une commande en facture
     * 
     * @param commandeID
     */
    public void transfertFacture(int commandeID) {

        SQLElement elt = Configuration.getInstance().getDirectory().getElement("SAISIE_ACHAT");
        EditFrame editFactureFrame = new EditFrame(elt);
        editFactureFrame.setIconImage(new ImageIcon(Gestion.class.getResource("frameicon.png")).getImage());

        SaisieAchatSQLComponent comp = (SaisieAchatSQLComponent) editFactureFrame.getSQLComponent();

        // comp.setDefaults();
        comp.loadCommande(commandeID);

        editFactureFrame.pack();
        editFactureFrame.setState(JFrame.NORMAL);
        editFactureFrame.setVisible(true);
    }

    @Override
    protected void archive(TreesOfSQLRows trees, boolean cutLinks) throws SQLException {

        for (SQLRow row : trees.getRows()) {

            // Mise à jour des stocks
            SQLElement eltMvtStock = Configuration.getInstance().getDirectory().getElement("MOUVEMENT_STOCK");
            SQLSelect sel = new SQLSelect();
            sel.addSelect(eltMvtStock.getTable().getField("ID"));
            Where w = new Where(eltMvtStock.getTable().getField("IDSOURCE"), "=", row.getID());
            Where w2 = new Where(eltMvtStock.getTable().getField("SOURCE"), "=", getTable().getName());
            sel.setWhere(w.and(w2));

            @SuppressWarnings("rawtypes")
            List l = (List) eltMvtStock.getTable().getBase().getDataSource().execute(sel.asString(), new ArrayListHandler());
            if (l != null) {
                for (int i = 0; i < l.size(); i++) {
                    Object[] tmp = (Object[]) l.get(i);
                    eltMvtStock.archive(((Number) tmp[0]).intValue());
                }
            }
        }
        super.archive(trees, cutLinks);
    }

}
