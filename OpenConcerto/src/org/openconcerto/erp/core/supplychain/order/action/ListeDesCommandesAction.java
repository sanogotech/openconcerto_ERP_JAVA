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
 
 package org.openconcerto.erp.core.supplychain.order.action;

import java.awt.event.ActionEvent;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFrame;

import org.openconcerto.erp.action.CreateFrameAbstractAction;
import org.openconcerto.erp.core.common.component.TransfertBaseSQLComponent;
import org.openconcerto.erp.core.supplychain.receipt.component.BonReceptionSQLComponent;
import org.openconcerto.erp.generationDoc.gestcomm.CommandeXmlSheet;
import org.openconcerto.erp.model.MouseSheetXmlListeListener;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.FieldPath;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLRowValuesListFetcher;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLSelectJoin;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.model.graph.Path;
import org.openconcerto.sql.model.graph.PathBuilder;
import org.openconcerto.sql.view.EditFrame;
import org.openconcerto.sql.view.IListFrame;
import org.openconcerto.sql.view.ListeAddPanel;
import org.openconcerto.sql.view.list.BaseSQLTableModelColumn;
import org.openconcerto.sql.view.list.IListe;
import org.openconcerto.sql.view.list.IListeAction.IListeEvent;
import org.openconcerto.sql.view.list.RowAction;
import org.openconcerto.sql.view.list.RowAction.PredicateRowAction;
import org.openconcerto.sql.view.list.SQLTableModelSourceOnline;
import org.openconcerto.ui.table.PercentTableCellRenderer;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.DecimalUtils;
import org.openconcerto.utils.cc.ITransformer;

public class ListeDesCommandesAction extends CreateFrameAbstractAction {
    public ListeDesCommandesAction() {
        super();
        this.putValue(Action.NAME, "Liste des commandes fournisseurs");
    }

    public JFrame createFrame() {

        final SQLElement elementCmd = Configuration.getInstance().getDirectory().getElement("COMMANDE");
        final SQLTableModelSourceOnline tableSource = elementCmd.getTableSource(true);
        BaseSQLTableModelColumn colAvancement = new BaseSQLTableModelColumn("Avancement réception", BigDecimal.class) {

            @Override
            protected Object show_(SQLRowAccessor r) {

                return getAvancement(r);
            }

            @Override
            public Set<FieldPath> getPaths() {
                final Path p = new PathBuilder(elementCmd.getTable()).addTable("TR_COMMANDE").addTable("BON_RECEPTION").build();
                return CollectionUtils.createSet(new FieldPath(p, "TOTAL_HT"));
            }
        };
        tableSource.getColumns().add(colAvancement);
        colAvancement.setRenderer(new PercentTableCellRenderer());

        final IListFrame frame = new IListFrame(new ListeAddPanel(elementCmd, new IListe(tableSource)));

        return frame;
    }

    private BigDecimal getAvancement(SQLRowAccessor r) {
        Collection<? extends SQLRowAccessor> rows = r.getReferentRows(r.getTable().getTable("TR_COMMANDE"));
        long totalFact = 0;
        long total = r.getLong("T_HT");
        for (SQLRowAccessor row : rows) {
            if (!row.isForeignEmpty("ID_BON_RECEPTION")) {
                SQLRowAccessor rowFact = row.getForeign("ID_BON_RECEPTION");
                Long l = rowFact.getLong("TOTAL_HT");
                totalFact += l;
            }
        }
        if (total > 0) {
            return new BigDecimal(totalFact).divide(new BigDecimal(total), DecimalUtils.HIGH_PRECISION).movePointRight(2).setScale(2, RoundingMode.HALF_UP);
        } else {
            return BigDecimal.ONE.movePointRight(2);
        }
    }

}
