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
 
 package org.openconcerto.erp.core.supplychain.receipt.action;

import org.openconcerto.erp.action.CreateFrameAbstractAction;
import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.common.component.TransfertBaseSQLComponent;
import org.openconcerto.erp.core.common.ui.IListFilterDatePanel;
import org.openconcerto.erp.core.supplychain.receipt.element.BonReceptionSQLElement;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.FieldPath;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.graph.Path;
import org.openconcerto.sql.model.graph.PathBuilder;
import org.openconcerto.sql.view.IListFrame;
import org.openconcerto.sql.view.ListeAddPanel;
import org.openconcerto.sql.view.list.BaseSQLTableModelColumn;
import org.openconcerto.sql.view.list.IListe;
import org.openconcerto.sql.view.list.SQLTableModelSourceOnline;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.table.PercentTableCellRenderer;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.DecimalUtils;

import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JPopupMenu;

public class ListeDesBonsReceptionsAction extends CreateFrameAbstractAction {

    public ListeDesBonsReceptionsAction() {
        super();
        this.putValue(Action.NAME, "Liste des bons de réceptions");
    }

    public JFrame createFrame() {
        final SQLElement element = Configuration.getInstance().getDirectory().getElement("BON_RECEPTION");
        final SQLTableModelSourceOnline tableSource = element.getTableSource(true);

        BaseSQLTableModelColumn colAvancement = new BaseSQLTableModelColumn("Avancement facturation", BigDecimal.class) {

            @Override
            protected Object show_(SQLRowAccessor r) {

                return getAvancement(r);
            }

            @Override
            public Set<FieldPath> getPaths() {
                final Path p = new PathBuilder(element.getTable()).addTable("TR_BON_RECEPTION").addTable("FACTURE_FOURNISSEUR").build();
                return CollectionUtils.createSet(new FieldPath(p, "T_HT"));
            }
        };
        tableSource.getColumns().add(colAvancement);
        colAvancement.setRenderer(new PercentTableCellRenderer());

        final IListFrame frame = new IListFrame(new ListeAddPanel(element, new IListe(tableSource)));

        // Date panel
        IListFilterDatePanel datePanel = new IListFilterDatePanel(frame.getPanel().getListe(), element.getTable().getField("DATE"), IListFilterDatePanel.getDefaultMap());
        GridBagConstraints c = new DefaultGridBagConstraints();
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0;
        c.gridy++;
        c.gridy++;
        c.anchor = GridBagConstraints.CENTER;
        datePanel.setFilterOnDefault();
        frame.getPanel().add(datePanel, c);

        return frame;
    }

    private BigDecimal getAvancement(SQLRowAccessor r) {
        Collection<? extends SQLRowAccessor> rows = r.getReferentRows(r.getTable().getTable("TR_BON_RECEPTION"));
        long totalFact = 0;
        long total = (r.getObject("TOTAL_HT") == null ? 0 : r.getLong("TOTAL_HT"));
        for (SQLRowAccessor row : rows) {
            if (!row.isForeignEmpty("ID_FACTURE_FOURNISSEUR")) {
                SQLRowAccessor rowFact = row.getForeign("ID_FACTURE_FOURNISSEUR");
                Long l = rowFact.getLong("T_HT");
                totalFact += l;
            }
        }
        if (total > 0) {
            return new BigDecimal(totalFact).divide(new BigDecimal(total), DecimalUtils.HIGH_PRECISION).movePointRight(2).setScale(2, RoundingMode.HALF_UP);
        } else {
            return BigDecimal.ONE.movePointRight(2);
        }
    }

    /**
     * Transfert en Facture
     * 
     * @param row
     */
    private void transfertFactureFournisseur(SQLRow row) {
        BonReceptionSQLElement elt = (BonReceptionSQLElement) Configuration.getInstance().getDirectory().getElement("BON_RECEPTION");
        elt.transfertFacture(row.getID());
    }
}
