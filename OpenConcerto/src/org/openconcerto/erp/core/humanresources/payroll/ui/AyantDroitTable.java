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
 
 package org.openconcerto.erp.core.humanresources.payroll.ui;

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.view.list.RowValuesTable;
import org.openconcerto.sql.view.list.RowValuesTableControlPanel;
import org.openconcerto.sql.view.list.RowValuesTableModel;
import org.openconcerto.sql.view.list.RowValuesTablePanel;
import org.openconcerto.sql.view.list.RowValuesTableRenderer;
import org.openconcerto.sql.view.list.SQLTableElement;
import org.openconcerto.ui.DefaultGridBagConstraints;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.List;
import java.util.Vector;

import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

public class AyantDroitTable extends RowValuesTablePanel {

    public AyantDroitTable() {

        init();
        uiInit();

    }

    /**
     * 
     */
    protected void init() {

        final SQLElement e = getSQLElement();

        final List<SQLTableElement> tableElements = new Vector<SQLTableElement>();

        tableElements.add(new SQLTableElement(e.getTable().getField("ID_CONTRAT_PREVOYANCE")));

        this.defaultRowVals = new SQLRowValues(getSQLElement().getTable());
        this.model = new RowValuesTableModel(e, tableElements, e.getTable().getField("ID_CONTRAT_PREVOYANCE"), false, this.defaultRowVals);

        this.table = new RowValuesTable(this.model, null);
    }

    public SQLElement getSQLElement() {
        return Configuration.getInstance().getDirectory().getElement("CONTRAT_PREVOYANCE_AYANT_DROIT");
    }

    protected void uiInit() {
        // Ui init
        setLayout(new GridBagLayout());
        this.setOpaque(false);
        final GridBagConstraints c = new DefaultGridBagConstraints();

        c.weightx = 1;

        RowValuesTableControlPanel control = new RowValuesTableControlPanel(this.table);
        control.setOpaque(false);
        this.add(control, c);

        c.gridy++;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 1;
        final JScrollPane comp = new JScrollPane(this.table);
        comp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        this.add(comp, c);
        this.table.setDefaultRenderer(Long.class, new RowValuesTableRenderer());
    }
}
