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
 
 package org.openconcerto.sql.sqlobject;

import java.awt.Component;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.TableCellEditor;

import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.utils.cc.ITransformer;

public class ITextArticleWithCompletionCellEditor extends AbstractCellEditor implements TableCellEditor {

    private final ITextArticleWithCompletion text;
    private boolean listenersInited = false;

    public ITextArticleWithCompletionCellEditor(SQLTable tableArticle, SQLTable tableARticleFournisseur) {
        this.text = new ITextArticleWithCompletion(tableArticle, tableARticleFournisseur);
        this.text.setBorder(BorderFactory.createEmptyBorder());
    }

    private void initListener(final JTable t) {
        if (!this.listenersInited) {
            this.listenersInited = true;
            this.text.getTextComp().addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_TAB) {
                        final int column;
                        final int row = t.getEditingRow();

                        // gestion tab ou shift+tab
                        if (e.getModifiers() == KeyEvent.SHIFT_MASK) {
                            column = t.getEditingColumn() - 1;
                        } else {
                            column = t.getEditingColumn() + 1;
                        }

                        SwingUtilities.invokeLater(new Runnable() {

                            public void run() {
                                if (t.getCellEditor() != null && t.getCellEditor().stopCellEditing()) {
                                    if (column >= 0 && column < t.getColumnCount()) {
                                        t.setColumnSelectionInterval(column, column);
                                        t.setRowSelectionInterval(row, row);
                                        // Need to postpone editCell because selection with
                                        // cancel
                                        // selection
                                        SwingUtilities.invokeLater(new Runnable() {

                                            public void run() {
                                                if (t.editCellAt(row, column)) {
                                                    t.getEditorComponent().requestFocusInWindow();
                                                }
                                            }
                                        });
                                    }
                                }
                            }
                        });

                    } else {
                        if (e.getKeyCode() == KeyEvent.VK_SPACE && e.getModifiers() == KeyEvent.SHIFT_MASK) {
                            e.setModifiers(0);
                        }
                    }
                }
            });
        }
    }

    @Override
    public Object getCellEditorValue() {
        return this.text.getText();
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {

        initListener(table);
        if (value != null) {
            this.text.setText((String) value);
        } else {
            this.text.setText("");
        }

        Runnable r = new Runnable() {

            public void run() {
                text.getTextComp().grabFocus();
            }
        };
        SwingUtilities.invokeLater(r);

        return this.text;
    }

    public void addSelectionListener(SelectionRowListener l) {
        this.text.addSelectionListener(l);
    }

    public SQLRowAccessor getComboSelectedRow() {
        return this.text.getSelectedRow();
    }

    public void setSelectTransformer(ITransformer<SQLSelect, SQLSelect> selTrans) {
        this.text.setSelectTransformer(selTrans);
    }

    public void setWhere(Where w) {
        this.text.setWhere(w);
    }
}
