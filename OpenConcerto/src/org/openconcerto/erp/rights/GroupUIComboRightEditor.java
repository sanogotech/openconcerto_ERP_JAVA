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
 
 package org.openconcerto.erp.rights;

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.GroupSQLComponent;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.element.SQLElementDirectory;
import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.users.rights.RightEditor;
import org.openconcerto.sql.users.rights.RightEditorManager;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.component.ComboLockedMode;
import org.openconcerto.ui.component.combo.ISearchableCombo;
import org.openconcerto.ui.group.Group;
import org.openconcerto.ui.list.DefaultMutableListModel;
import org.openconcerto.utils.model.DefaultIMutableListModel;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class GroupUIComboRightEditor implements RightEditor {

    @Override
    public void setValue(final String object, final DBRoot root, final SQLElementDirectory directory, final JComponent editorComponent) {
        SQLField f = Configuration.getInstance().getFieldMapper().getSQLFieldForItem(object);
        if (f != null) {
            ((PanelGroupUIChooser) editorComponent).getBoxTable().setValue(new SQLElementComboItem(directory.getElement(f.getTable().getName())));
            ((PanelGroupUIChooser) editorComponent).getComboMenu().setValue(new GroupItemUIComboItem(object, directory.getElement(f.getTable().getName())));
        } else {
            if (object != null && object.trim().length() > 0 && object.contains(".")) {
                String tableName = object.substring(0, object.indexOf('.'));
                if (root.findTable(tableName) != null) {
                    String fieldName = object.substring(object.indexOf('.') + 1, object.length());
                    final SQLElement element = directory.getElement(tableName);
                    ((PanelGroupUIChooser) editorComponent).getBoxTable().setValue(new SQLElementComboItem(element));
                    ((PanelGroupUIChooser) editorComponent).getComboMenu().setValue(new GroupItemUIComboItem(fieldName, element));
                }
            }
        }
    }

    @Override
    public JComponent getRightEditor(final String right, final DBRoot root, final SQLElementDirectory directory, final JTextField fieldObject) {

        return new PanelGroupUIChooser(fieldObject);
    }

    public static void register() {
        RightEditorManager.getInstance().register(GroupSQLComponent.ITEM_RIGHT_CODE, new GroupUIComboRightEditor());
    }

    private class SQLElementComboItem {
        private final SQLElement elt;

        public SQLElementComboItem(SQLElement elt) {
            this.elt = elt;
        }

        public SQLElement getSQLElement() {
            return elt;
        }

        @Override
        public String toString() {
            return elt.getPluralName();
        }

    }

    private class PanelGroupUIChooser extends JPanel {

        final ISearchableCombo<SQLElementComboItem> boxTable;
        final ISearchableCombo<GroupItemUIComboItem> comboMenu;

        public PanelGroupUIChooser(final JTextField fieldObject) {
            super(new GridBagLayout());
            GridBagConstraints c = new DefaultGridBagConstraints();
            c.gridx = GridBagConstraints.RELATIVE;
            c.weightx = 1;
            this.boxTable = new ISearchableCombo<SQLElementComboItem>(ComboLockedMode.UNLOCKED, 0, 20);

            DefaultIMutableListModel<SQLElementComboItem> comboItems = new DefaultIMutableListModel<SQLElementComboItem>();
            Collection<SQLElement> elts = Configuration.getInstance().getDirectory().getElements();

            for (SQLElement sqlElement : elts) {
                if (sqlElement.getDefaultGroup() != null) {
                    comboItems.addElement(new SQLElementComboItem(sqlElement));
                }
            }

            boxTable.initCache(comboItems);
            this.add(boxTable);

            this.comboMenu = new ISearchableCombo<GroupItemUIComboItem>(ComboLockedMode.UNLOCKED, 0, 20);
            final DefaultMutableListModel<GroupItemUIComboItem> cache = new DefaultMutableListModel<GroupItemUIComboItem>();
            comboMenu.initCache(cache);
            boxTable.addValueListener(new PropertyChangeListener() {

                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    cache.removeAllElements();
                    final SQLElementComboItem selectedItem = (SQLElementComboItem) boxTable.getSelectedItem();
                    if (selectedItem != null) {
                        final SQLElement element = selectedItem.getSQLElement();
                        if (element != null) {
                            Group g = element.getDefaultGroup();
                            if (g != null) {
                                cache.addAll(GroupItemUIComboItem.getComboMenu(g, element));
                            }
                        }
                    }
                }
            });
            comboMenu.addValueListener(new PropertyChangeListener() {

                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    final GroupItemUIComboItem selectedItem = (GroupItemUIComboItem) comboMenu.getSelectedItem();
                    if (selectedItem != null) {
                        SQLElementComboItem selectedElement = (SQLElementComboItem) boxTable.getSelectedItem();
                        if (selectedElement != null) {
                            final SQLTable table = selectedElement.getSQLElement().getTable();
                            if (table.contains(selectedItem.getId())) {
                                fieldObject.setText(table.getName() + "." + table.getField(selectedItem.getId()).getName());
                            } else {
                                fieldObject.setText(selectedItem.getId());
                            }
                        }
                    }
                }
            });
            this.add(comboMenu);
        }

        public ISearchableCombo<GroupItemUIComboItem> getComboMenu() {
            return comboMenu;
        }

        public ISearchableCombo<SQLElementComboItem> getBoxTable() {
            return boxTable;
        }

    }

}
