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
 
 package org.openconcerto.sql.users.rights;

import org.openconcerto.sql.TM;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.element.SQLElementDirectory;
import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.SQLName;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.ui.component.combo.ISearchableCombo;
import org.openconcerto.utils.i18n.Grammar;
import org.openconcerto.utils.model.DefaultIMutableListModel;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JTextField;

public class SQLTableRightEditor implements RightEditor {
    @SuppressWarnings("unchecked")
    @Override
    public void setValue(final String object, final DBRoot root, final SQLElementDirectory directory, final JComponent editorComponent) {
        ((ISearchableCombo<SQLTableComboItem>) editorComponent).setValue(SQLTableComboItem.createFromString(object, root, directory));
    }

    @Override
    public JComponent getRightEditor(final String right, final DBRoot root, final SQLElementDirectory directory, final JTextField fieldObject) {
        final ISearchableCombo<SQLTableComboItem> comboMenu = new ISearchableCombo<SQLTableComboItem>();
        DefaultIMutableListModel<SQLTableComboItem> comboItems = new DefaultIMutableListModel<SQLTableComboItem>();
        Set<SQLTable> set = root.getTables();

        List<SQLTableComboItem> result = new ArrayList<SQLTableComboItem>(set.size());
        result.add(SQLTableComboItem.createFromTable(null));
        for (SQLTable table : set) {
            final SQLElement elt = directory.getElement(table);
            result.add(SQLTableComboItem.create(table, elt));
        }
        comboItems.addAll(result);
        comboMenu.initCache(comboItems);
        comboMenu.addValueListener(new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                final SQLTableComboItem selectedItem = (SQLTableComboItem) comboMenu.getSelectedItem();
                if (selectedItem != null) {
                    fieldObject.setText(selectedItem.getValue());
                }
            }
        });
        return comboMenu;
    }

    public static void register() {
        final SQLTableRightEditor editor = new SQLTableRightEditor();
        for (final String code : TableAllRights.getCodes())
            RightEditorManager.getInstance().register(code, editor);
    }

    static public class SQLTableComboItem {

        static public SQLTableComboItem create(final SQLTable t, final SQLElement elt) {
            assert elt == null || elt.getTable() == t;
            return elt != null ? createFromElement(elt) : createFromTable(t);
        }

        static public SQLTableComboItem createFromElement(final SQLElement elt) {
            return new SQLTableComboItem(elt.getTable(), elt.getName().getVariant(Grammar.SINGULAR));
        }

        static public SQLTableComboItem createFromTable(final SQLTable t) {
            return new SQLTableComboItem(t, t == null ? TM.tr("rights.allTables") : t.getName());
        }

        static public SQLTableComboItem createFromString(final String s, final DBRoot r, final SQLElementDirectory dir) {
            if (s == null)
                return createFromTable(null);
            final SQLName n = SQLName.parse(s);
            if (n.getItemCount() != 1)
                throw new IllegalArgumentException("Not 1 item : " + n);
            final SQLTable t = r.findTable(n.getName());
            if (t == null)
                // allow to use unknown table (e.g. not yet created)
                return new SQLTableComboItem(s, n.getName());
            else
                return create(t, dir.getElement(t));
        }

        private final String value;
        private final String label;

        protected SQLTableComboItem(final SQLTable t, final String label) {
            this(TableAllRights.tableToString(t, false), label);
        }

        protected SQLTableComboItem(final String value, final String label) {
            this.value = value;
            this.label = label;
        }

        public String getValue() {
            return this.value;
        }

        @Override
        public String toString() {
            return this.label;
        }
    }
}
