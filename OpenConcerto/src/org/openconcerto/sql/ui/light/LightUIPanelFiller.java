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
 
 package org.openconcerto.sql.ui.light;

import org.openconcerto.sql.Log;
import org.openconcerto.sql.PropsConfiguration;
import org.openconcerto.sql.model.Constraint;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLSyntax.ConstraintType;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.request.ComboSQLRequest;
import org.openconcerto.sql.sqlobject.IComboSelectionItem;
import org.openconcerto.ui.light.LightUIComboBox;
import org.openconcerto.ui.light.LightUIComboBoxElement;
import org.openconcerto.ui.light.LightUIElement;
import org.openconcerto.ui.light.LightUILine;
import org.openconcerto.ui.light.LightUIPanel;
import org.openconcerto.utils.io.JSONConverter;

import java.util.Calendar;
import java.util.Date;
import java.util.Set;

/**
 * Fill value from default or database
 */
public class LightUIPanelFiller {
    private final LightUIPanel panel;

    public LightUIPanelFiller(LightUIPanel panel) {
        this.panel = panel;
    }

    public void fillWithDefaultValues() {
        final int panelChildCount = this.panel.getChildrenCount();
        for (int i = 0; i < panelChildCount; i++) {
            final LightUILine panelChild = this.panel.getChild(i, LightUILine.class);
            final int lineChildCount = panelChild.getChildrenCount();
            for (int j = 0; j < lineChildCount; j++) {
                final LightUIElement element = panelChild.getChild(j);
                if (element.getType() == LightUIElement.TYPE_DATE) {
                    // Set date to current server date
                    element.setValue(JSONConverter.getJSON(new Date(System.currentTimeMillis())).toString());
                }
            }
        }
    }

    public void fillFromRow(final PropsConfiguration configuration, final SQLRowAccessor row) {
        this.fillFromRow(this.panel, configuration, row);
    }

    private void fillFromRow(final LightUIPanel panel, final PropsConfiguration configuration, SQLRowAccessor sqlRow) {
        final int panelChildCount = panel.getChildrenCount();
        // Convert as sqlrow if possible to get all values from db
        if (sqlRow.hasID()) {
            sqlRow = sqlRow.asRow();
        }
        for (int i = 0; i < panelChildCount; i++) {
            final LightUILine panelChild = panel.getChild(i, LightUILine.class);
            final int lineChildCount = panelChild.getChildrenCount();
            for (int j = 0; j < lineChildCount; j++) {
                final LightUIElement element = panelChild.getChild(j);
                final SQLField sqlField = configuration.getFieldMapper().getSQLFieldForItem(element.getId());

                SQLRowAccessor sqlRowTmp = this.getSQLRowForField(sqlRow, sqlField);
                if (sqlRowTmp == null) {
                    throw new IllegalArgumentException("Impossible to reach the field: " + sqlField.getName() + " from table " + sqlRow.getTable().getName());
                }

                int type = element.getType();
                if (type == LightUIElement.TYPE_TEXT_FIELD || type == LightUIElement.TYPE_TEXT_AREA) {

                    if (sqlField == null) {
                        Log.get().severe("No field found for text field : " + element.getId());
                        continue;
                    }
                    element.setValue(sqlRowTmp.getString(sqlField.getName()));
                } else if (sqlField != null && sqlField.isKey() && (type == LightUIElement.TYPE_COMBOBOX || type == LightUIElement.TYPE_AUTOCOMPLETE_COMBOBOX)) {
                    // send: id,value
                    final LightUIComboBox combo = (LightUIComboBox) element;
                    LightUIComboBoxElement value = null;
                    final Number foreignID = sqlRowTmp.getForeignIDNumber(sqlField.getName());
                    if (foreignID != null) {
                        final SQLTable foreignTable = sqlField.getForeignTable();
                        final ComboSQLRequest req = configuration.getDirectory().getElement(foreignTable).getComboRequest();
                        final IComboSelectionItem comboItem = req.getComboItem(foreignID.intValue());
                        if (comboItem != null) {
                            value = new LightUIComboBoxElement(comboItem.getId());
                            value.setValue1(comboItem.getLabel());
                        }
                    }
                    combo.setSelectedValue(value);
                } else if (type == LightUIElement.TYPE_CHECKBOX) {
                    if (sqlRowTmp.getObject(sqlField.getName()) != null && sqlRowTmp.getBoolean(sqlField.getName())) {
                        element.setValue("true");
                    } else {
                        element.setValue("false");
                    }
                } else if (type == LightUIElement.TYPE_DATE) {
                    Calendar date = sqlRowTmp.getDate(sqlField.getName());
                    if (date != null) {
                        element.setValue(JSONConverter.getJSON(date).toString());
                    }
                } else if (type == LightUIElement.TYPE_PANEL) {
                    this.fillFromRow((LightUIPanel) element, configuration, sqlRowTmp);
                } else if (type == LightUIElement.TYPE_SLIDER) {
                    final Integer value = sqlRowTmp.getInt(sqlField.getName());
                    if (value != null) {
                        element.setValue(value.toString());
                    }
                }
            }
        }
    }

    public SQLRowAccessor getSQLRowForField(final SQLRowAccessor sqlRow, final SQLField sqlField) {
        SQLRowAccessor sqlRowResult = sqlRow;
        if (sqlField != null && !sqlField.getTable().getName().equals(sqlRow.getTable().getName())) {
            sqlRowResult = this.findSQLRow(sqlRow, sqlField);
        }
        return sqlRowResult;
    }

    public SQLRowAccessor findSQLRow(final SQLRowAccessor sqlRow, final SQLField sqlField) {
        final Set<Constraint> constraints = sqlRow.getTable().getAllConstraints();
        for (final Constraint constraint : constraints) {
            if (constraint.getType().equals(ConstraintType.FOREIGN_KEY)) {
                // FIXME: this doesn't work when foreign key is composed of more than one field
                final String firstFkCols = constraint.getCols().get(0);
                final SQLRowAccessor fkRow = sqlRow.getForeign(firstFkCols);
                if (fkRow != null) {
                    if (fkRow.getTable().getName().equals(sqlField.getTable().getName())) {
                        return fkRow;
                    } else {
                        final SQLRowAccessor sqlRowResult = this.findSQLRow(fkRow, sqlField);
                        if (sqlRowResult != null) {
                            return sqlRowResult;
                        }
                    }
                }
            }
        }
        return null;
    }
}
