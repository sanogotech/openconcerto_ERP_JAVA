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
 
 package org.openconcerto.erp.core.supplychain.receipt.ui;

import java.awt.event.ActionEvent;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;

import org.openconcerto.erp.core.common.ui.AbstractAchatArticleItemTable;
import org.openconcerto.erp.core.sales.product.ui.ReliquatRowValuesTable;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;

public class BonReceptionItemTable extends AbstractAchatArticleItemTable {

    private ReliquatRowValuesTable reliquatTable;

    @Override
    protected String getConfigurationFileName() {
        return "Table_Bon_Reception.xml";

    }

    public void setReliquatTable(ReliquatRowValuesTable reliquatTable) {
        this.reliquatTable = reliquatTable;
    }

    @Override
    public SQLElement getSQLElement() {
        return Configuration.getInstance().getDirectory().getElement("BON_RECEPTION_ELEMENT");
    }

    @Override
    public boolean isUsedBiasedDevise() {
        return false;
    }

    @Override
    protected List<AbstractAction> getAdditionnalMouseAction(final int rowIndex) {
        List<AbstractAction> actions = new ArrayList<AbstractAction>();
        actions.addAll(super.getAdditionnalMouseAction(rowIndex));
        actions.add(new AbstractAction("Ajouter un reliquat") {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (reliquatTable != null) {
                    SQLRowAccessor sqlRowArticleChildElement = getRowValuesTable().getRowValuesTableModel().getRowValuesAt(rowIndex);
                    final SQLRowValues row2Insert = new SQLRowValues(reliquatTable.getDefaultRowValues());

                    row2Insert.put("ID_BON_RECEPTION_ELEMENT", sqlRowArticleChildElement);

                    row2Insert.put("QTE", 1);
                    row2Insert.put("QTE_UNITAIRE", BigDecimal.ONE);

                    reliquatTable.getRowValuesTable().getRowValuesTableModel().addRow(row2Insert);
                }
            }
        });
        return actions;
    }
}
