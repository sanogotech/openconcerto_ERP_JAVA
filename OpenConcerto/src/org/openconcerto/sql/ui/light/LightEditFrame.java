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

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.Log;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.FieldMapper;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.view.EditPanel.EditMode;
import org.openconcerto.sql.view.list.ListSQLLine;
import org.openconcerto.sql.view.list.SQLTableModelLinesSourceOffline;
import org.openconcerto.ui.group.Group;
import org.openconcerto.ui.group.Item;
import org.openconcerto.ui.light.CustomEditorProvider;
import org.openconcerto.ui.light.JSONToLightUIConvertor;
import org.openconcerto.ui.light.LightUICheckBox;
import org.openconcerto.ui.light.LightUIComboBox;
import org.openconcerto.ui.light.LightUIDate;
import org.openconcerto.ui.light.LightUIElement;
import org.openconcerto.ui.light.LightUIFrame;
import org.openconcerto.utils.io.JSONConverter;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.Future;

import net.minidev.json.JSONObject;

public class LightEditFrame extends LightUIFrame {
    private static final String EDIT_MODE_JSON_KEY = "edit-mode";

    private Group group;
    private SQLRowValues sqlRow;

    private EditMode editMode = EditMode.READONLY;

    // Init from json constructor
    public LightEditFrame(final JSONObject json) {
        super(json);
    }

    // Clone constructor
    public LightEditFrame(final LightEditFrame frame) {
        super(frame);
        this.sqlRow = frame.sqlRow;
        this.group = frame.group;
        this.editMode = frame.editMode;
    }

    public LightEditFrame(final Configuration conf, final Group group, final SQLRowValues sqlRow, final LightUIFrame parentFrame, final EditMode editMode) {
        super(group.getId() + ".edit.frame");
        this.setType(TYPE_FRAME);
        this.setParent(parentFrame);

        this.sqlRow = sqlRow;
        this.group = group;

        this.setEditMode(editMode);
    }

    public void setEditMode(final EditMode editMode) {
        this.editMode = editMode;
        if (editMode.equals(EditMode.READONLY)) {
            this.setReadOnly(true);
        } else {
            this.setReadOnly(false);
        }
    }

    /**
     * Commit the SQLRowValues attached to this frame
     * 
     * @param configuration - Current configuration
     * 
     * @return The inserted SQLRow
     * 
     * @throws SQLException When an error occur in SQLRowValues.commit()
     */
    public SQLRow commitSqlRow(final Configuration configuration) throws SQLException {
        if (this.editMode.equals(EditMode.READONLY)) {
            throw new IllegalArgumentException("Impossible to commit values when the frame is read only");
        }
        final SQLElement sqlElement = configuration.getDirectory().getElement(this.sqlRow.getTable());
        try {
            return this.sqlRow.prune(sqlElement.getPrivateGraph()).commit();
        } catch (final SQLException ex) {
            throw ex;
        }
    }

    public EditMode getEditMode() {
        return this.editMode;
    }

    public Group getGroup() {
        return this.group;
    }

    public SQLRowValues getSqlRow() {
        return this.sqlRow;
    }

    /**
     * Update the SQLRowValues attached to this frame
     * 
     * @param conf
     * @param userId
     */
    public void updateRow(final Configuration configuration, final String sessionSecurityToken, final int userId) {
        if (this.editMode.equals(EditMode.READONLY)) {
            throw new IllegalArgumentException("Impossible to update values when the frame is read only");
        }
        this.updateRow(configuration, this.group, sessionSecurityToken, userId);
    }

    private void updateRow(final Configuration configuration, final Group group, final String sessionSecurityToken, final int userId) {
        final FieldMapper fieldMapper = configuration.getFieldMapper();
        if (fieldMapper == null) {
            throw new IllegalStateException("null field mapper");
        }

        final SQLElement sqlElement = configuration.getDirectory().getElement(this.sqlRow.getTable());

        final Map<String, CustomEditorProvider> customEditors;
        if (this.editMode.equals(EditMode.CREATION)) {
            customEditors = sqlElement.getCustomEditorProviderForCreation(configuration, sessionSecurityToken);
        } else {
            customEditors = sqlElement.getCustomEditorProviderForModification(configuration, this.sqlRow, sessionSecurityToken);
        }

        this.createRowValues(configuration, sqlElement, fieldMapper, this.group, customEditors);
        this.setMetaData(userId);
    }

    final protected void createRowValues(final Configuration configuration, final SQLElement sqlElement, final FieldMapper fieldMapper, final Group group,
            final Map<String, CustomEditorProvider> customEditors) {
        final int itemCount = group.getSize();
        for (int i = 0; i < itemCount; i++) {
            final Item item = group.getItem(i);
            if (item instanceof Group) {
                this.createRowValues(configuration, sqlElement, fieldMapper, (Group) item, customEditors);
            } else {
                final SQLField field = fieldMapper.getSQLFieldForItem(item.getId());
                if (field != null) {
                    final LightUIElement uiElement = this.findChild(item.getId(), false);

                    if (uiElement == null) {
                        throw new IllegalArgumentException("Impossible to find UI Element with id: " + item.getId());
                    }

                    if (!uiElement.isNotSaved()) {
                        this.putValueFromUserControl(configuration, sqlElement, field, uiElement, customEditors);
                    }
                } else {
                    Log.get().warning("No field attached to " + item.getId());
                }
            }
        }
    }

    final protected void putValueFromUserControl(final Configuration configuration, final SQLElement sqlElement, final SQLField sqlField, final LightUIElement uiElement,
            final Map<String, CustomEditorProvider> customEditors) {
        if (!uiElement.isNotSaved()) {
            final Class<?> fieldType = sqlField.getType().getJavaType();
            if (customEditors.containsKey(uiElement.getId())) {
                final CustomEditorProvider customEditor = customEditors.get(uiElement.getId());
                if (customEditor instanceof SavableCustomEditorProvider) {
                    ((SavableCustomEditorProvider) customEditor).save(this.sqlRow, sqlField, uiElement);
                }
            } else {
                final String fieldName = sqlField.getFieldName();
                if (sqlField.isKey()) {
                    if (!(uiElement instanceof LightUIComboBox)) {
                        throw new IllegalArgumentException("Invalid UI Element for field: " + fieldName + ". When field is foreign key, UI Element must be a LightUIDate");
                    }
                    final LightUIComboBox combo = (LightUIComboBox) uiElement;

                    if (combo.hasSelectedValue()) {
                        this.sqlRow.put(fieldName, combo.getSelectedValue().getId());
                    } else {
                        this.sqlRow.put(fieldName, null);
                    }
                } else {
                    final String value = uiElement.getValue();
                    if (value == null && !sqlField.isNullable()) {
                        Log.get().warning("ignoring null value for not nullable field " + fieldName + " from table " + sqlField.getTable().getName());
                    } else {
                        if (fieldType.equals(String.class)) {
                            // FIXME check string size against field size
                            this.sqlRow.put(fieldName, value);
                        } else if (fieldType.equals(Date.class)) {
                            if (!(uiElement instanceof LightUIDate)) {
                                throw new IllegalArgumentException("Invalid UI Element for field: " + fieldName + ". When field is Date, UI Element must be a LightUIDate");
                            }
                            this.sqlRow.put(fieldName, ((LightUIDate) uiElement).getValueAsDate());
                        } else if (fieldType.equals(Boolean.class)) {
                            if (!(uiElement instanceof LightUICheckBox)) {
                                throw new IllegalArgumentException("Invalid UI Element for field: " + fieldName + ". When field is Boolean, UI Element must be a LightUICheckBox");
                            }
                            this.sqlRow.put(fieldName, ((LightUICheckBox) uiElement).isChecked());
                        } else if (fieldType.equals(Timestamp.class)) {
                            if (!(uiElement instanceof LightUIDate)) {
                                throw new IllegalArgumentException("Invalid UI Element for field: " + fieldName + ". When field is Date, UI Element must be a LightUIDate");
                            }
                            this.sqlRow.put(fieldName, ((LightUIDate) uiElement).getValueAsDate());
                        } else if (fieldType.equals(Integer.class)) {
                            if (value != null && !value.trim().isEmpty()) {
                                if (!value.matches("^-?\\d+$")) {
                                    throw new IllegalArgumentException("Invalid value for field: " + fieldName + " value: " + value);
                                }
                                this.sqlRow.put(fieldName, Integer.parseInt(value));
                            } else {
                                this.sqlRow.put(fieldName, null);
                            }
                        } else if (fieldType.equals(Double.class) || fieldType.equals(Float.class) || fieldType.equals(BigDecimal.class)) {
                            if (value != null && !value.trim().isEmpty()) {
                                try {
                                    this.sqlRow.put(fieldName, new BigDecimal(value));
                                } catch (final Exception ex) {
                                    throw new IllegalArgumentException("Invalid value for field: " + fieldName + " value: " + value);
                                }

                            } else {
                                this.sqlRow.put(fieldName, null);
                            }
                        } else {
                            Log.get().warning("unsupported type " + fieldName);
                        }
                    }
                }
            }
        }
    }

    /**
     * Save all referent rows store in LightRowValuesTable
     * 
     * @param group Element edit group
     * @param frame Element edit frame
     * @param row Element saved row
     * @param customEditors List of custom editors used in element edit frame
     */
    final public void saveReferentRows(final Configuration configuration, final SQLRow parentSqlRow, final Map<String, CustomEditorProvider> customEditors, final String sessionSecurityToken) {
        this.saveReferentRows(configuration, this.group, parentSqlRow, customEditors, sessionSecurityToken);
    }

    final private void saveReferentRows(final Configuration configuration, final Group group, final SQLRow parentSqlRow, final Map<String, CustomEditorProvider> customEditors,
            final String sessionSecurityToken) {
        for (int i = 0; i < group.getSize(); i++) {
            final Item item = group.getItem(i);
            if (item instanceof Group) {
                this.saveReferentRows(configuration, (Group) item, parentSqlRow, customEditors, sessionSecurityToken);
            } else if (customEditors.containsKey(item.getId())) {
                final LightUIElement element = this.findChild(item.getId(), false);
                if (element instanceof LightForeignRowValuesTableOffline) {
                    final LightForeignRowValuesTableOffline foreignTable = (LightForeignRowValuesTableOffline) element;
                    for (int j = 0; j < foreignTable.getRowsCount(); j++) {
                        final ListSQLLine listLine = foreignTable.getModel().getRow(j);
                        final SQLRowValues rowVals = listLine.getRow().createEmptyUpdateRow();
                        rowVals.put(foreignTable.getForeignField().getName(), parentSqlRow.getID());
                        ((SQLTableModelLinesSourceOffline) foreignTable.getModel().getLinesSource()).updateRow(listLine.getID(), rowVals);
                    }
                    final Future<?> fCommit = foreignTable.commitRows();

                    try {
                        fCommit.get();
                    } catch (final Exception ex) {
                        throw new IllegalArgumentException(ex);
                    }
                }
            }
        }
    }

    final protected void setMetaData(final int userId) {
        final SQLTable sqlTable = this.sqlRow.getTable();
        if (this.sqlRow.getObject(sqlTable.getCreationUserField().getName()) == null || this.sqlRow.getObject(sqlTable.getCreationDateField().getName()) == null) {
            this.sqlRow.put(sqlTable.getCreationUserField().getName(), userId);
            this.sqlRow.put(sqlTable.getCreationDateField().getName(), new Date());
        }
        this.sqlRow.put(sqlTable.getModifUserField().getName(), userId);
        this.sqlRow.put(sqlTable.getModifDateField().getName(), new Date());
    }

    @Override
    public String getClassName() {
        return this.getClass().getName();
    }

    @Override
    public JSONToLightUIConvertor getConvertor() {
        return new JSONToLightUIConvertor() {

            @Override
            public LightUIElement convert(JSONObject json) {
                return new LightEditFrame(json);
            }
        };
    }

    @Override
    public LightUIElement clone() {
        return new LightEditFrame(this);
    }

    // TODO: implement JSONAble on SQLRowValues and Group
    @Override
    public JSONObject toJSON() {
        final JSONObject json = super.toJSON();
        if (!this.editMode.equals(EditMode.READONLY)) {
            if (this.editMode.equals(EditMode.CREATION)) {
                json.put(EDIT_MODE_JSON_KEY, 1);
            } else if (this.editMode.equals(EditMode.MODIFICATION)) {
                json.put(EDIT_MODE_JSON_KEY, 2);
            }
        }
        return json;
    }

    @Override
    public void fromJSON(final JSONObject json) {
        super.fromJSON(json);
        final int jsonEditMode = JSONConverter.getParameterFromJSON(json, EDIT_MODE_JSON_KEY, Integer.class, 3);
        if (jsonEditMode == 1) {
            this.editMode = EditMode.CREATION;
        } else if (jsonEditMode == 2) {
            this.editMode = EditMode.MODIFICATION;
        } else if (jsonEditMode == 3) {
            this.editMode = EditMode.READONLY;
        }
    }
}
