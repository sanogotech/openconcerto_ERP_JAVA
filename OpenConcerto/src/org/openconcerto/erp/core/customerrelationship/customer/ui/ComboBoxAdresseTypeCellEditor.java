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
 
 package org.openconcerto.erp.core.customerrelationship.customer.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractCellEditor;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.border.LineBorder;
import javax.swing.table.TableCellEditor;

public class ComboBoxAdresseTypeCellEditor extends AbstractCellEditor implements TableCellEditor {

    private JComboBox comboBox;
    private AdresseType val;
    private final Map<String, AdresseType> map = new HashMap<String, AdresseType>();

    public ComboBoxAdresseTypeCellEditor() {

        super();

        this.comboBox = new JComboBox();
        for (AdresseType t : AdresseType.values()) {
            this.comboBox.addItem(t);
            this.map.put(t.getId(), t);
        }

        this.comboBox.setBorder(new LineBorder(Color.black));
    }

    public boolean isCellEditable(EventObject e) {

        if (e instanceof MouseEvent) {
            return ((MouseEvent) e).getClickCount() >= 2;
        }
        return super.isCellEditable(e);
    }

    public Object getCellEditorValue() {
        return this.val.getId();
    }

    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        if (value != null) {
            this.val = this.map.get(value);
            this.comboBox.setSelectedItem(this.val);
        }
        this.comboBox.grabFocus();

        this.comboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent arg0) {

                val = (AdresseType) comboBox.getSelectedItem();
            }
        });

        return this.comboBox;

    }

}
