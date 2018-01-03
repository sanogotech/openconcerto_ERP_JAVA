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
 
 package org.openconcerto.erp.core.common.element;

import org.openconcerto.erp.config.Gestion;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.PropsConfiguration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.FieldPath;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.SQLTable.VirtualFields;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.request.ComboSQLRequest;
import org.openconcerto.sql.request.ListSQLRequest;
import org.openconcerto.sql.request.SQLFieldTranslator;
import org.openconcerto.sql.ui.light.GroupToLightUIConvertor;
import org.openconcerto.sql.view.EditPanel.EditMode;
import org.openconcerto.sql.view.list.SQLTableModelColumn;
import org.openconcerto.sql.view.list.SQLTableModelSource;
import org.openconcerto.sql.view.list.SQLTableModelSourceOnline;
import org.openconcerto.ui.AutoHideListener;
import org.openconcerto.ui.light.InformationLine;
import org.openconcerto.ui.light.LightUIElement;
import org.openconcerto.ui.light.LightUILine;
import org.openconcerto.ui.light.LightUIPanel;
import org.openconcerto.ui.light.RowSelectionSpec;
import org.openconcerto.ui.light.SimpleTextLine;
import org.openconcerto.ui.table.TableCellRendererUtils;
import org.openconcerto.utils.GestionDevise;
import org.openconcerto.utils.convertor.ValueConvertor;

import java.awt.Color;
import java.awt.Component;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import org.jdom2.Document;
import org.jdom2.input.DOMBuilder;

/**
 * SQLElement de la base société
 * 
 * @author Administrateur
 * 
 */
public abstract class SocieteSQLConfElement extends SQLElement {

    {
        this.setL18nLocation(Gestion.class);
    }

    public SocieteSQLConfElement(SQLTable table, String singular, String plural) {
        super(singular, plural, table);
    }

    public SocieteSQLConfElement(SQLTable table) {
        this(table, null);
    }

    public SocieteSQLConfElement(SQLTable table, String code) {
        super(table, null, code);
    }

    public static final TableCellRenderer CURRENCY_RENDERER = new DefaultTableCellRenderer() {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final Component res = super.getTableCellRendererComponent(table, GestionDevise.currencyToString((BigDecimal) value), isSelected, hasFocus, row, column);
            // this renderer can be decorated by e.g. ListeFactureRenderer which does a
            // setBackground(), thus always reset the colors
            // MAYBE always use ProxyComp as in AlternateTableCellRenderer to leave the decorated
            // renderer as found
            TableCellRendererUtils.setColors(res, table, isSelected);
            ((JLabel) res).setHorizontalAlignment(SwingConstants.RIGHT);
            return res;
        }
    };

    static public final JPanel createAdditionalPanel() {
        return AutoHideListener.listen(new JPanel());
    }

    @Override
    protected String createCode() {
        return createCodeFromPackage();
    }

    /**
     * Return a code that doesn't change when subclassing to allow to easily change a SQLElement
     * while keeping the same code. To achieve that, the code isn't
     * {@link #createCodeFromPackage(Class) computed} with <code>this.getClass()</code>. We iterate
     * up through our superclass chain, and as soon as we find an abstract class, we stop and use
     * the previous class (i.e. non abstract). E.g. any direct subclass of
     * {@link ComptaSQLConfElement} will still use <code>this.getClass()</code>, but so is one of
     * its subclass.
     * 
     * @return a code computed from the superclass just under the first abstract superclass.
     * @see #createCodeFromPackage(Class)
     */
    protected final String createCodeFromPackage() {
        return createCodeFromPackage(getLastNonAbstractClass());
    }

    private final Class<? extends ComptaSQLConfElement> getLastNonAbstractClass() {
        Class<?> prev = null;
        Class<?> cl = this.getClass();
        // test loop
        assert !Modifier.isAbstract(cl.getModifiers()) && ComptaSQLConfElement.class.isAssignableFrom(cl) && Modifier.isAbstract(ComptaSQLConfElement.class.getModifiers());
        while (!Modifier.isAbstract(cl.getModifiers())) {
            prev = cl;
            cl = cl.getSuperclass();
        }
        assert ComptaSQLConfElement.class.isAssignableFrom(prev);
        @SuppressWarnings("unchecked")
        final Class<? extends ComptaSQLConfElement> res = (Class<? extends ComptaSQLConfElement>) prev;
        return res;
    }

    static protected String createCodeFromPackage(final Class<? extends ComptaSQLConfElement> cl) {
        String canonicalName = cl.getName();
        if (canonicalName.contains("erp.core") && canonicalName.contains(".element")) {
            int i = canonicalName.indexOf("erp.core") + 9;
            int j = canonicalName.indexOf(".element");
            canonicalName = canonicalName.substring(i, j);
        }
        return canonicalName;
    }

    @Override
    protected void _initTableSource(SQLTableModelSource res) {
        super._initTableSource(res);
        for (final SQLTableModelColumn col : res.getColumns()) {
            // TODO getDeviseFields()
            if (col.getValueClass() == Long.class || col.getValueClass() == BigInteger.class) {
                col.setConverter(new ValueConvertor<Number, BigDecimal>() {
                    @Override
                    public BigDecimal convert(Number o) {
                        if (o == null) {
                            System.err.println("ComptaSQLConfElement._initTableSource: Warning null Number conversion (" + this + ")");
                            return BigDecimal.ZERO;
                        }
                        return new BigDecimal(o.longValue()).movePointLeft(2);
                    }

                    @Override
                    public Number unconvert(BigDecimal o) {

                        if (o == null) {
                            System.err.println("ComptaSQLConfElement._initTableSource: Warning null BigDecimal conversion (" + this + ")");
                            return 0;
                        }
                        return o.movePointRight(2);
                    }
                }, BigDecimal.class);
                col.setRenderer(CURRENCY_RENDERER);
            }
        }
    }

    /**
     * Get columns user preferences for a specific table
     * 
     * @param userId - Id of the user who want view the table
     * @param tableId - Id of table to show
     * @param sqlColumns - List of columns to be displayed
     * @return the XML which contains user preferences
     */
    protected Document getColumnsUserPerfs(final Configuration configuration, final int userId, final String tableId, final List<SQLTableModelColumn> sqlColumns) {
        Document columnsPrefs = null;
        try {
            final DOMBuilder in = new DOMBuilder();
            final org.w3c.dom.Document w3cDoc = configuration.getXMLConf(userId, tableId);
            if (w3cDoc != null) {
                columnsPrefs = in.build(w3cDoc);
            }
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to get ColumnPrefs for table " + tableId + " and for user " + userId + "\n" + ex.getMessage());
        }

        return columnsPrefs;
    }

    /**
     * Create buttons from SQLElement secondary row actions
     */
    public LightUILine createSecondaryRowActionLine(final RowSelectionSpec selection) {
        return null;
    }

    /**
     * Create information ui panel for selected lines. By default, all fields in SQLRowValues are
     * displayed
     * 
     * @param sessionToken - String use to find session on server
     * @param selection - List of SQLRowAccessor attach to selected lines
     * 
     * @return null if selection is empty or a LightUIPanel
     */
    public final LightUIPanel createDataPanel(final String sessionToken, final List<SQLRowAccessor> selection) {
        if (selection == null || selection.isEmpty()) {
            return null;
        }
        final LightUIPanel dataPanel = new LightUIPanel(this.getCode() + ".data.panel");
        dataPanel.setVerticallyScrollable(true);
        dataPanel.setWeightX(1);
        dataPanel.setMarginLeft(4);
        this.fillDataPanel(sessionToken, selection, dataPanel);

        return dataPanel;
    }

    protected void fillDataPanel(final String sessionToken, final List<SQLRowAccessor> selection, final LightUIPanel dataPanel) {
        final SQLFieldTranslator translator = this.getDirectory().getTranslator();

        for (final SQLRowAccessor listRow : selection) {
            final int rowId = listRow.getID();
            final LightUILine mainLine = new LightUILine();
            final LightUIPanel mainLinePanel = new LightUIPanel(dataPanel.getId() + ".main.line." + rowId);
            mainLinePanel.setWeightX(1);
            mainLinePanel.addChild(new SimpleTextLine("Information sur l'élément n°" + rowId, true, LightUIElement.HALIGN_CENTER));
            final LightUILine lineData = new LightUILine();
            final LightUIPanel panel = new LightUIPanel(this.getCode() + ".data.panel." + rowId);
            panel.setWeightX(1);
            
            final List<String> dataPanelFields = this.getDataPanelFields();
            for (final String fieldName : dataPanelFields) {
                this.addFieldToPanel(fieldName, panel, listRow, translator);
            }
            lineData.addChild(panel);
            mainLinePanel.addChild(lineData);
            mainLine.addChild(mainLinePanel);

            dataPanel.addChild(mainLine);
        }
    }
    
    protected List<String> getDataPanelFields(){
        return this.getListFields();
    }

    public void addFieldToPanel(final String fieldName, final LightUIPanel dataPanel, final SQLRowAccessor sqlRow, final SQLFieldTranslator translator) {
        addFieldToPanel(fieldName, dataPanel, sqlRow, translator, false, "");
    }

    static private final VirtualFields FIELDS_TO_IGNORE = VirtualFields.PRIMARY_KEY.union(VirtualFields.ARCHIVE).union(VirtualFields.ORDER);

    /**
     * Add the field name translation and it's value to the information panel
     * 
     * @param fieldName - Field to be translate
     * @param dataPanel - Information panel
     * @param sqlRow - Row which contains data
     * @param translator - Field translator
     */
    public void addFieldToPanel(final String fieldName, final LightUIPanel dataPanel, final SQLRowAccessor sqlRow, final SQLFieldTranslator translator, boolean addEmpty, String defaultValue) {
        final SQLTable sqlTable = sqlRow.getTable();
        final SQLField field = sqlTable.getField(fieldName);

        if (!sqlTable.getFields(FIELDS_TO_IGNORE).contains(field)) {
            String key = translator.getLabelFor(field);
            boolean error = false;
            if (key == null) {
                error = true;
                key = field.getFieldName();
            }

            String value = "";
            if (field.isKey()) {
                final List<FieldPath> fieldsPath = getListExpander().expand(field);
                for (FieldPath fieldPath : fieldsPath) {
                    final SQLRowValues foreignRow = sqlRow.asRowValues().followPath(fieldPath.getPath());
                    if (foreignRow != null) {
                        value += foreignRow.getString(fieldPath.getField().getName()) + " ";
                    }
                }
            } else {
                value = sqlRow.getString(fieldName);
            }
            boolean isDefault = false;
            if (value == null || value.isEmpty()) {
                isDefault = true;
                value = defaultValue;
            }
            if (!value.isEmpty() || addEmpty) {
                final InformationLine line = new InformationLine(key, value);
                if (error) {
                    line.setLabelColor(Color.RED);
                }
                line.setItalicOnValue(isDefault);
                dataPanel.addChild(line);
            }
        }
    }

    public GroupToLightUIConvertor getGroupToLightUIConvertor(final PropsConfiguration configuration, final EditMode editMode, final SQLRowValues sqlRow, final String sessionSecurityToken) {
        final GroupToLightUIConvertor convertor = new GroupToLightUIConvertor(configuration);
        if (editMode.equals(EditMode.CREATION)) {
            convertor.putAllCustomEditorProvider(this.getCustomEditorProviderForCreation(configuration, sessionSecurityToken));
        } else {
            convertor.putAllCustomEditorProvider(this.getCustomEditorProviderForModification(configuration, sqlRow, sessionSecurityToken));
        }
        return convertor;
    }

    public List<SQLRowValues> getRowValues(final String fieldName, final long id) {
        final SQLTableModelSourceOnline tableSource = this.getTableSource(true);

        final ListSQLRequest req = tableSource.getReq();
        req.setWhere(new Where(this.getTable().getField(fieldName), "=", id));
        return req.getValues();
    }

    @Override
    protected void _initComboRequest(ComboSQLRequest req) {
        super._initComboRequest(req);
        req.setSearchable(true);
    }
}
