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
 
 package org.openconcerto.erp.core.finance.accounting.ui;

import org.openconcerto.erp.core.common.ui.DeviseCellEditor;
import org.openconcerto.erp.core.common.ui.RowValuesMultiLineEditTable;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.UndefinedRowValuesCache;
import org.openconcerto.sql.view.list.CellDynamicModifier;
import org.openconcerto.sql.view.list.RowValuesTable;
import org.openconcerto.sql.view.list.RowValuesTableControlPanel;
import org.openconcerto.sql.view.list.RowValuesTableModel;
import org.openconcerto.sql.view.list.RowValuesTableRenderer;
import org.openconcerto.sql.view.list.SQLTableElement;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.table.PercentTableCellRenderer;
import org.openconcerto.utils.DecimalUtils;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.List;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ToolTipManager;

public class AnalytiqueItemTable extends JPanel {

    private final RowValuesTable table;
    private final DeviseKmRowValuesRenderer deviseRenderer = new DeviseKmRowValuesRenderer();
    private final DeviseCellEditor deviseCellEditor = new DeviseCellEditor();
    private SQLRowAccessor rowEcr;
    private static final SQLElement elt = Configuration.getInstance().getDirectory().getElement("ASSOCIATION_ANALYTIQUE");
    private final SQLRowValues rowVals = new SQLRowValues(UndefinedRowValuesCache.getInstance().getDefaultRowValues(elt.getTable()));

    public SQLRowValues getDefaultRowValues() {
        return this.rowVals;
    }

    public AnalytiqueItemTable(boolean multilineEditor) {
        setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();
        c.weightx = 1;

        final List<SQLTableElement> list = new Vector<SQLTableElement>();
        final SQLTable tableElement = elt.getTable();

        final SQLTableElement tableElementNomCompte = new SQLTableElement(tableElement.getField("ID_POSTE_ANALYTIQUE"));
        list.add(tableElementNomCompte);

        final SQLTableElement tableElementPourcent = new SQLTableElement(tableElement.getField("POURCENT"));
        tableElementPourcent.setRenderer(new PercentTableCellRenderer());
        list.add(tableElementPourcent);

        final SQLTableElement tableElementMontant = new SQLTableElement(tableElement.getField("MONTANT"), Long.class, this.deviseCellEditor);
        list.add(tableElementMontant);

        rowVals.put("POURCENT", BigDecimal.ONE.movePointRight(2));
        final RowValuesTableModel model = new RowValuesTableModel(elt, list, tableElement.getField("ID_POSTE_ANALYTIQUE"), false, rowVals);

        if (multilineEditor) {
            this.table = new RowValuesMultiLineEditTable(model, null, "ANALYTIQUE") {
                @Override
                public String getStringValue(final SQLRowValues rowVals) {
                    return getStringAssocs(rowVals);
                }

                public void insertFrom(final SQLRowAccessor row) {
                    rowEcr = row;
                    getDefaultRowValues().put("POURCENT", BigDecimal.TEN.movePointRight(1));
                    getDefaultRowValues().put("MONTANT", rowEcr.getLong("DEBIT") - rowEcr.getLong("CREDIT"));
                    super.insertFrom(row);
                }
            };

            ToolTipManager.sharedInstance().unregisterComponent(this.table);
            ToolTipManager.sharedInstance().unregisterComponent(this.table.getTableHeader());

            final RowValuesTableControlPanel panelControl = new RowValuesTableControlPanel(this.table);
            panelControl.setVisibleButtonHaut(false);
            panelControl.setVisibleButtonBas(false);
            panelControl.setVisibleButtonClone(false);
            panelControl.setVisibleButtonInserer(false);
            this.add(panelControl, c);

            c.gridy++;
            c.fill = GridBagConstraints.BOTH;
            c.weightx = 1;
            c.weighty = 1;

            this.add(new JScrollPane(this.table), c);
            this.table.setDefaultRenderer(Long.class, new RowValuesTableRenderer());

            // Bouton valider et fermer
            final JButton buttonValider = new JButton("Valider les modifications");
            final JButton buttonFermer = new JButton("Fermer");
            c.gridx = 0;
            c.gridy++;
            c.anchor = GridBagConstraints.EAST;
            c.weightx = 1;
            c.weighty = 0;
            c.fill = GridBagConstraints.NONE;
            c.gridwidth = GridBagConstraints.REMAINDER;
            final JPanel panelButton = new JPanel();
            panelButton.add(buttonValider);
            panelButton.add(buttonFermer);
            this.add(panelButton, c);

            final RowValuesMultiLineEditTable multiTable = (RowValuesMultiLineEditTable) this.table;
            buttonValider.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent event) {
                    multiTable.updateField(multiTable.getForeignField(), multiTable.getRowValuesRoot());
                    // buttonValider.setEnabled(false);
                    multiTable.closeTable();
                }
            });
            buttonFermer.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent event) {
                    multiTable.closeTable();
                }
            });

            this.setMinimumSize(new Dimension(this.getMinimumSize().width, 200));
            this.setPreferredSize(new Dimension(this.getPreferredSize().width, 200));
        } else {
            this.table = new RowValuesTable(model, null);
            ToolTipManager.sharedInstance().unregisterComponent(this.table);
            ToolTipManager.sharedInstance().unregisterComponent(this.table.getTableHeader());
            this.add(new RowValuesTableControlPanel(this.table), c);
            c.fill = GridBagConstraints.BOTH;
            c.weightx = 1;
            c.weighty = 1;
            c.gridy++;
            this.add(new JScrollPane(this.table), c);
        }

        tableElementMontant.addModificationListener(tableElementPourcent);
        tableElementPourcent.setModifier(new CellDynamicModifier() {
            @Override
            public Object computeValueFrom(SQLRowValues row, SQLTableElement source) {
                long montant = row.getLong("MONTANT");

                SQLRowAccessor foreignEcr = row.getForeign("ID_ECRITURE");
                if (rowEcr != null) {
                    foreignEcr = rowEcr;
                }
                long total = foreignEcr.getLong("DEBIT") - foreignEcr.getLong("CREDIT");
                BigDecimal pourcent = BigDecimal.ZERO;

                if (total != 0) {
                    pourcent = new BigDecimal(montant).divide(new BigDecimal(total), DecimalUtils.HIGH_PRECISION).abs().movePointRight(2)
                            .setScale(tableElementPourcent.getDecimalDigits(), RoundingMode.HALF_UP);
                }
                return pourcent;
            }

        });

        tableElementPourcent.addModificationListener(tableElementMontant);
        tableElementMontant.setModifier(new CellDynamicModifier() {
            @Override
            public Object computeValueFrom(SQLRowValues row, SQLTableElement source) {
                BigDecimal percent = row.getBigDecimal("POURCENT");

                SQLRowAccessor foreignEcr = row.getForeign("ID_ECRITURE");
                if (rowEcr != null) {
                    foreignEcr = rowEcr;
                }
                long total = foreignEcr.getLong("DEBIT") - foreignEcr.getLong("CREDIT");

                BigDecimal montant = percent.movePointLeft(2).multiply(new BigDecimal(total)).setScale(0, RoundingMode.HALF_UP);

                return montant.longValue();
            }

        });

        c.gridy++;

        tableElementMontant.setRenderer(this.deviseRenderer);

    }

    public RowValuesTable getTable() {
        return table;
    }

    public void updateField(final String field, final int id) {
        this.table.updateField(field, id);
    }

    public void insertFrom(final SQLRowAccessor row) {
        this.rowEcr = row;
        getDefaultRowValues().putEmptyLink("ID_SAISIE_KM_ELEMENT");
        if (rowEcr != null && !rowEcr.isForeignEmpty("ID_MOUVEMENT")) {
            SQLRowAccessor r = rowEcr.getForeign("ID_MOUVEMENT");
            if (r.getString("SOURCE").equalsIgnoreCase("SAISIE_KM")) {
                Collection<? extends SQLRowAccessor> rElt = this.rowEcr.getReferentRows(this.rowEcr.getTable().getTable("SAISIE_KM_ELEMENT"));
                if (rElt.size() > 0) {
                    getDefaultRowValues().put("ID_SAISIE_KM_ELEMENT", rElt.iterator().next().getID());
                }
            }
            getDefaultRowValues().put("POURCENT", BigDecimal.TEN.movePointRight(1));
            getDefaultRowValues().put("MONTANT", rowEcr.getLong("DEBIT") - rowEcr.getLong("CREDIT"));
        }
        this.table.insertFrom(row);
    }

    public RowValuesTableModel getModel() {
        return this.table.getRowValuesTableModel();
    }

    public static String getStringAssocs(final SQLRowValues rowVals) {
        final StringBuffer buf = new StringBuffer();

        final SQLTable tableElement = elt.getTable();
        if (rowVals.getID() > 1) {
            final SQLRow row = rowVals.getTable().getRow(rowVals.getID());
            final List<SQLRow> rowSet = row.getReferentRows(tableElement);

            for (final SQLRow row2 : rowSet) {
                buf.append(getStringAssoc(row2) + ", ");
            }
        } else {
            final Collection<SQLRowValues> colRows = rowVals.getReferentRows();
            for (final SQLRowValues rowValues : colRows) {
                if (rowValues.getTable().getName().equalsIgnoreCase(tableElement.getName())) {
                    buf.append(getStringAssoc(rowValues) + ", ");
                }
            }
        }

        // return buf.append("...").toString().trim();

        String string = buf.toString();
        if (string.length() > 2) {
            string = string.substring(0, string.length() - 2);
        }
        return string.trim();
    }

    private static String getStringAssoc(final SQLRowAccessor row) {
        final StringBuffer buf = new StringBuffer();
        final SQLRowAccessor rowVerif = row.getForeign("ID_POSTE_ANALYTIQUE");
        if (rowVerif != null) {
            buf.append(rowVerif.getString("NOM"));
        }
        return buf.toString();
    }

}
