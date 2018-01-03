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
 
 package org.openconcerto.ui.light;

import org.openconcerto.utils.NumberUtils;
import org.openconcerto.utils.io.JSONConverter;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.jdom2.Document;
import org.jdom2.Element;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

public class LightUITable extends LightUserControlContainer {

    public static final int DEFAULT_LINE_HEIGHT = 40;

    private static final String LINE_PER_ROW = "line-per-row";
    private static final String TABLE_SPEC = "table-spec";
    private static final String ALLOW_SELECTION = "allow-selection";
    private static final String ALLOW_MULTI_SELECTION = "allow-multi-selection";
    private static final String DYNAMIC_LOAD = "dynamic-load";
    private static final String AUTO_SELECT_FIRST_LINE = "auto-select-first-line";
    private Boolean dynamicLoad = false;
    private Boolean allowSelection = false;
    private Boolean allowMultiSelection = false;
    private Boolean autoSelectFirstLine = true;
    private TableSpec tableSpec = null;

    private List<ActionListener> selectionListeners = new ArrayList<ActionListener>();

    // Nombre de ligne à afficher par Row
    private int linePerRow = 1;

    private int lineHeight = DEFAULT_LINE_HEIGHT;

    // Init from json constructor
    public LightUITable(final JSONObject json) {
        super(json);
    }

    // Clone constructor
    public LightUITable(final LightUITable tableElement) {
        super(tableElement);
        this.tableSpec = tableElement.tableSpec;
        this.allowSelection = tableElement.allowSelection;
    }

    public LightUITable(final String id) {
        super(id);
        this.setType(LightUIElement.TYPE_TABLE);

        this.setWeightX(1);
        this.setFillWidth(true);

        final RowSelectionSpec selection = new RowSelectionSpec(this.getId());
        final ColumnsSpec columnsSpec = new ColumnsSpec(this.getId(), new ArrayList<ColumnSpec>(), new ArrayList<String>(), new ArrayList<String>());
        final TableSpec tableSpec = new TableSpec(this.getId(), selection, columnsSpec);
        tableSpec.setContent(new TableContent(this.getId()));

        this.setTableSpec(tableSpec);
    }

    @Override
    public void setId(final String id) {
        super.setId(id);

        if (this.tableSpec != null) {
            this.tableSpec.setId(id);

            if (this.tableSpec.getSelection() != null) {
                this.tableSpec.getSelection().setTableId(id);
            }

            if (this.tableSpec.getContent() != null) {
                this.tableSpec.getContent().setTableId(id);
            }
        }
    }

    public final void setLinePerRow(int linePerRow) {
        this.linePerRow = linePerRow;
    }

    public final int getLinePerRow() {
        return this.linePerRow;
    }

    public final void setLineHeight(int lineHeight) {
        this.lineHeight = lineHeight;
    }

    public final int getLineHeight() {
        return this.lineHeight;
    }

    public final TableSpec getTableSpec() {
        return this.tableSpec;
    }

    public final void setTableSpec(final TableSpec tableSpec) {
        this.tableSpec = tableSpec;
    }

    public final Boolean isAllowSelection() {
        return this.allowSelection;
    }

    public final void setAllowSelection(final boolean allowSelection) {
        this.allowSelection = allowSelection;
    }

    public final Boolean isAllowMultiSelection() {
        return this.allowMultiSelection;
    }

    public final void setAllowMultiSelection(final boolean allowMultiSelection) {
        this.allowMultiSelection = allowMultiSelection;
    }

    public final Boolean isDynamicLoad() {
        return this.dynamicLoad;
    }

    public final void setDynamicLoad(final boolean dynamicLoad) {
        this.dynamicLoad = dynamicLoad;
    }

    public final Boolean isAutoSelectFirstLine() {
        return this.autoSelectFirstLine;
    }

    public final void setAutoSelectFirstLine(final boolean autoSelectFirstLine) {
        this.autoSelectFirstLine = autoSelectFirstLine;
    }

    public final Row removeRow(final int index) {
        return this.tableSpec.getContent().removeRow(index);
    }

    public final boolean removeRow(final Row row) {
        final TableContent content = this.getTableSpec().getContent();
        return content.removeRow(row);
    }

    public final boolean hasRow() {
        return (this.tableSpec != null && this.tableSpec.getContent() != null && this.tableSpec.getContent().getRowsCount() > 0);
    }

    public final Row getRow(final int index) {
        return this.getTableSpec().getContent().getRow(index);
    }

    public Row getRowById(final Number rowId) {
        final int size = this.getTableSpec().getContent().getRowsCount();
        for (int i = 0; i < size; i++) {
            final Row row = this.getRow(i);
            if (NumberUtils.areNumericallyEqual(row.getId(), rowId)) {
                return row;
            } else {
                System.err.println("LightUITable.getSelectedRows() - Null selectedRow");
            }
        }
        return null;
    }

    public final Row setRow(final int index, final Row row) {
        return this.getTableSpec().getContent().setRow(index, row);
    }

    public final boolean addRow(final Row row) {
        return this.getTableSpec().getContent().addRow(row);
    }

    public final int getRowsCount() {
        return this.getTableSpec().getContent().getRowsCount();
    }

    public final void clearRows() {
        this.getTableSpec().getContent().clearRows();
    }

    /**
     * Get Ids of SQLRowAccessor store in selected rows
     * 
     * @return The list of selected DB Ids
     */
    public final List<Number> getSelectedIds() {
        return this.getTableSpec().getSelection().getIds();
    }

    public final Number getFirstSelectedId() {
        final List<Number> selectedIds = this.getTableSpec().getSelection().getIds();
        if (selectedIds.isEmpty()) {
            return null;
        } else {
            return selectedIds.get(0);
        }
    }

    public final void setSelectedIds(final List<Number> selectedIds, final boolean fire) {
        this.getTableSpec().getSelection().setIds(selectedIds);
        if (fire) {
            this.fireSelectionChange();
        }
    }

    public final void clearSelection(final boolean fire) {
        this.getTableSpec().getSelection().getIds().clear();
        if (fire) {
            this.fireSelectionChange();
        }
    }

    public final List<Row> getSelectedRows() {
        final List<Row> selectedRows = new ArrayList<Row>();

        if (this.getTableSpec().getSelection() != null) {
            final List<Number> selectedIds = this.getSelectedIds();
            for (final Number selectedId : selectedIds) {
                final Row selectedRow = this.getRowById(selectedId);
                if (selectedRow != null) {
                    selectedRows.add(selectedRow);
                }
            }
        }

        return selectedRows;
    }

    public final boolean replaceChild(final LightUIElement pChild) {
        pChild.setReadOnly(this.isReadOnly());

        for (int i = 0; i < this.getRowsCount(); i++) {
            final Row tableRow = this.getTableSpec().getContent().getRow(i);
            final List<Object> tableRowValues = tableRow.getValues();
            final int tableRowValuesCount = tableRowValues.size();

            for (int j = 0; j < tableRowValuesCount; j++) {
                final Object tableRowValue = tableRowValues.get(j);
                if (tableRowValue instanceof LightUIElement) {
                    final LightUIElement child = (LightUIElement) tableRowValue;

                    if (child.getId().equals(pChild.getId())) {
                        tableRowValues.set(i, pChild);
                        child.setParent(this);
                        return true;
                    }
                    if (child instanceof LightUIContainer) {
                        if (((LightUIContainer) child).replaceChild(pChild)) {
                            return true;
                        }
                    }
                    if (child instanceof LightUITable) {
                        if (((LightUITable) child).replaceChild(pChild)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public final LightUIElement findElement(final String searchParam, final boolean byUUID) {
        return this.findElement(searchParam, byUUID, LightUIElement.class);
    }

    public final <T extends LightUIElement> T findElement(final String searchParam, final boolean byUUID, final Class<T> objectClass) {
        if (this.hasRow()) {

            for (int i = 0; i < this.getRowsCount(); i++) {
                final Row row = this.getRow(i);
                final List<Object> rowValues = row.getValues();
                for (final Object value : rowValues) {
                    if (value instanceof LightUIContainer) {
                        final LightUIContainer panel = (LightUIContainer) value;
                        final T element = panel.findChild(searchParam, byUUID, objectClass);
                        if (element != null) {
                            return element;
                        }
                    } else if (value instanceof LightUIElement) {
                        final LightUIElement element = (LightUIElement) value;
                        if (byUUID) {
                            if (element.getUUID().equals(searchParam)) {
                                if (objectClass.isAssignableFrom(element.getClass())) {
                                    return objectClass.cast(element);
                                } else {
                                    throw new IllegalArgumentException(
                                            "Element found at is not an instance of " + objectClass.getName() + ", element class: " + element.getClass().getName() + " element ID: " + element.getId());
                                }
                            }
                        } else {
                            if (element.getId().equals(searchParam)) {
                                if (objectClass.isAssignableFrom(element.getClass())) {
                                    return objectClass.cast(element);
                                } else {
                                    throw new IllegalArgumentException(
                                            "Element found at is not an instance of " + objectClass.getName() + ", element class: " + element.getClass().getName() + " element ID: " + element.getId());
                                }
                            }
                        }

                        if (element instanceof LightUITable) {
                            final T resultElement = ((LightUITable) element).findElement(searchParam, byUUID, objectClass);
                            if (resultElement != null) {
                                return resultElement;
                            }
                        }
                    }
                }
            }
        } else {
            System.out.println("LightUITable.getElementById() - No rows for table: " + this.getId());
        }
        return null;
    }

    public <T extends LightUIElement> List<T> findChildren(final Class<T> expectedClass, final boolean recursively) {
        final List<T> result = new ArrayList<T>();

        if (this.hasRow()) {
            final int size = this.getRowsCount();
            for (int i = 0; i < size; i++) {
                final Row row = this.getRow(i);
                final List<Object> rowValues = row.getValues();
                for (final Object value : rowValues) {
                    if (recursively) {
                        if (value instanceof LightUIContainer) {
                            result.addAll(((LightUIContainer) value).findChildren(expectedClass, recursively));
                        } else if (value instanceof LightUITable) {
                            result.addAll(((LightUITable) value).findChildren(expectedClass, recursively));
                        }
                    }
                    if (expectedClass.isAssignableFrom(value.getClass())) {
                        result.add(expectedClass.cast(value));
                    }
                }
            }
        } else {
            System.out.println("LightUITable.getElementById() - No rows for table: " + this.getId());
        }

        return result;
    }

    public final void addSelectionListener(final ActionListener selectionListener) {
        this.selectionListeners.add(selectionListener);
    }

    public final void removeSelectionListeners() {
        this.selectionListeners.clear();
    }

    public final void fireSelectionChange() {
        for (final ActionListener listener : this.selectionListeners) {
            listener.actionPerformed(new ActionEvent(this, 1, "selection"));
        }
    }

    // TODO: garder l'ordre des colonnes invisibles
    /**
     * Create columns preferences with the current ColumnsSpec
     * 
     * @return XML document with columns preferences
     */
    public final Document createXmlPreferences(final Document userPrefs, final ColumnsSpec columnsSpec) throws ParserConfigurationException {

        final Element rootElement = new Element("list");
        final Document xmlConf = new Document();

        final int columnSpecCount = columnsSpec.getColumnCount();
        final List<String> visibleIds = new ArrayList<String>();
        for (int i = 0; i < columnSpecCount; i++) {
            final ColumnSpec columnSpec = columnsSpec.getColumn(i);
            final Element xmlColumn = this.createXmlColumn(columnSpec.getId(), columnSpec.getMaxWidth(), columnSpec.getMinWidth(), columnSpec.getWidth());
            rootElement.addContent(xmlColumn);
            visibleIds.add(columnSpec.getId());
        }

        final Element rootUserPrefs = userPrefs.getRootElement();
        final List<Element> xmlColumns = rootUserPrefs.getChildren();
        final int columnsSize = xmlColumns.size();
        for (int i = 0; i < columnsSize; i++) {
            final Element xmlColumn = xmlColumns.get(i);
            final String columnId = xmlColumn.getAttribute("id").getValue();
            if (!visibleIds.contains(columnId)) {
                final int maxWidth = Integer.parseInt(xmlColumn.getAttribute("max-width").getValue());
                final int minWidth = Integer.parseInt(xmlColumn.getAttribute("min-width").getValue());
                final int width = Integer.parseInt(xmlColumn.getAttribute("width").getValue());
                final Element newXmlColumn = this.createXmlColumn(columnId, maxWidth, minWidth, width);
                rootElement.addContent(newXmlColumn);
            }
        }
        xmlConf.setRootElement(rootElement);
        return xmlConf;
    }

    /**
     * Create default columns preferences from the SQLTableModelLinesSourceOnline
     * 
     * @return XML document with columns preferences
     */
    public Document createDefaultXmlPreferences() {
        final Element rootElement = new Element("list");

        if (this.getTableSpec() != null && this.getTableSpec().getColumns() != null) {
            final int sqlColumnsCount = this.getTableSpec().getColumns().getColumnCount();
            for (int i = 0; i < sqlColumnsCount; i++) {
                final ColumnSpec column = this.getTableSpec().getColumns().getColumn(i);
                final String columnId = column.getId();
                final Element columnElement = this.createXmlColumn(columnId, column.getMaxWidth(), column.getMinWidth(), column.getWidth());
                rootElement.addContent(columnElement);
            }
        }
        final Document xmlConf = new Document(rootElement);

        return xmlConf;
    }

    protected final Element createXmlColumn(final String columnId, final double maxWidth, final double minWidth, final double width) {
        final Element columnElement = new Element("column");
        columnElement.setAttribute("id", columnId);
        columnElement.setAttribute("max-width", String.valueOf(maxWidth));
        columnElement.setAttribute("min-width", String.valueOf(minWidth));
        columnElement.setAttribute("width", String.valueOf(width));
        return columnElement;
    }

    @Override
    public void setReadOnly(final boolean readOnly) {
        super.setReadOnly(readOnly);

        if (this.hasRow()) {
            final int size = this.getRowsCount();
            for (int i = 0; i < size; i++) {
                final Row row = this.getRow(i);
                final List<Object> values = row.getValues();
                for (final Object value : values) {
                    if (value != null && value instanceof LightUIElement) {
                        ((LightUIElement) value).setReadOnly(readOnly);
                    }
                }
            }
        }
    }

    @Override
    public JSONToLightUIConvertor getConvertor() {
        return new JSONToLightUIConvertor() {
            @Override
            public LightUIElement convert(final JSONObject json) {
                return new LightUITable(json);
            }
        };
    }

    @Override
    public void _setValueFromContext(final Object value) {
        if (value != null) {
            final JSONArray jsonContext = (JSONArray) JSONConverter.getObjectFromJSON(value, JSONArray.class);
            final ColumnsSpec columnsSpec = this.getTableSpec().getColumns();
            final int columnsCount = columnsSpec.getColumnCount();

            final List<Integer> editorsIndex = new ArrayList<Integer>();

            for (int i = 0; i < columnsCount; i++) {
                final ColumnSpec columnSpec = columnsSpec.getColumn(i);
                if (columnSpec.getEditor() != null) {
                    editorsIndex.add(i);
                }
            }

            if (this.hasRow()) {
                final int size = this.getRowsCount();
                if (jsonContext.size() != size) {
                    System.err.println("LightUITable.setValueFromContext() - Incorrect line count in JSON");
                } else {

                    for (int i = 0; i < size; i++) {
                        final Row row = this.getRow(i);
                        final JSONObject jsonLineContext = (JSONObject) JSONConverter.getObjectFromJSON(jsonContext.get(i), JSONObject.class);
                        final Number rowId = JSONConverter.getParameterFromJSON(jsonLineContext, "row.id", Number.class);
                        final String rowExtendId = (String) JSONConverter.getParameterFromJSON(jsonLineContext, "row.extend.id", String.class);
                        if (NumberUtils.areNumericallyEqual(rowId, row.getId()) && (row.getExtendId() == null || (row.getExtendId() != null && rowExtendId.equals(row.getExtendId())))) {
                            if (row.isFillWidth()) {
                                if (!row.getValues().isEmpty() && row.getValues().get(0) instanceof LightUserControl) {
                                    final LightUIElement element = (LightUIElement) row.getValues().get(0);
                                    if (element instanceof LightUserControl) {
                                        if (jsonLineContext.containsKey(element.getUUID())) {
                                            ((LightUserControl) element)._setValueFromContext(jsonLineContext.get(element.getUUID()));
                                        } else {
                                            System.out.println("LightUITable.setValueFromContext() - Unable to find element : id - " + element.getId() + " uuid - " + element.getUUID());
                                            System.out.println("LightUITable.setValueFromContext() - In JSON                : " + jsonLineContext.toJSONString());
                                        }
                                    }
                                }
                            } else {
                                for (int k = 0; k < editorsIndex.size(); k++) {
                                    final Object objEditor = row.getValues().get(editorsIndex.get(k));
                                    if (!(objEditor instanceof LightUserControl)) {
                                        throw new IllegalArgumentException("Impossible to find editor for row: " + rowId.toString() + " at position: " + String.valueOf(k));
                                    }
                                    final LightUIElement editor = (LightUIElement) objEditor;

                                    if (editor instanceof LightUserControl && jsonLineContext.containsKey(editor.getUUID())) {
                                        ((LightUserControl) editor)._setValueFromContext(jsonLineContext.get(editor.getUUID()));
                                    } else {
                                        throw new IllegalArgumentException(
                                                "Impossible to find value for editor: " + editor.getId() + " for row: " + rowId.toString() + " at position: " + String.valueOf(k));
                                    }
                                }
                            }
                        } else {
                            throw new IllegalArgumentException("Impossible to find row: " + rowId.toString());
                        }
                    }
                }
            }
        }
    }

    @Override
    public LightUIElement clone() {
        return new LightUITable(this);
    }

    @Override
    public JSONObject toJSON() {
        final JSONObject json = super.toJSON();
        if (this.allowSelection) {
            json.put(ALLOW_SELECTION, true);
        }
        if (this.allowMultiSelection) {
            json.put(ALLOW_MULTI_SELECTION, true);
        }
        if (this.dynamicLoad) {
            json.put(DYNAMIC_LOAD, true);
        }
        if (!this.autoSelectFirstLine) {
            json.put(AUTO_SELECT_FIRST_LINE, false);
        }
        if (this.tableSpec != null) {
            json.put(TABLE_SPEC, this.tableSpec.toJSON());
        }

        json.put(LINE_PER_ROW, this.linePerRow);
        return json;
    }

    @Override
    public void fromJSON(final JSONObject json) {
        super.fromJSON(json);
        this.allowSelection = JSONConverter.getParameterFromJSON(json, ALLOW_SELECTION, Boolean.class, false);
        this.allowSelection = JSONConverter.getParameterFromJSON(json, ALLOW_MULTI_SELECTION, Boolean.class, false);
        this.dynamicLoad = JSONConverter.getParameterFromJSON(json, DYNAMIC_LOAD, Boolean.class, false);
        this.autoSelectFirstLine = JSONConverter.getParameterFromJSON(json, AUTO_SELECT_FIRST_LINE, Boolean.class, true);
        this.linePerRow = JSONConverter.getParameterFromJSON(json, LINE_PER_ROW, Integer.class);

        final JSONObject jsonRawContent = (JSONObject) JSONConverter.getParameterFromJSON(json, TABLE_SPEC, JSONObject.class);

        if (jsonRawContent != null) {
            this.tableSpec = new TableSpec(jsonRawContent);
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        this.selectionListeners.clear();
    }
}
