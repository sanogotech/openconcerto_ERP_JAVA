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
 
 package org.openconcerto.ui;

import javax.swing.DefaultCellEditor;
import javax.swing.JTextField;

public class PositiveIntegerTableCellEditor extends DefaultCellEditor {
    private final boolean strict;

    public PositiveIntegerTableCellEditor() {
        super(new JTextField());
        this.strict = false;
    }

    public PositiveIntegerTableCellEditor(boolean strict) {
        super(new JTextField());
        this.strict = strict;
    }

    @Override
    public boolean stopCellEditing() {
        try {
            // try to get the value
            this.getCellEditorValue();
            return super.stopCellEditing();
        } catch (Exception ex) {
            return false;
        }

    }

    @Override
    public Object getCellEditorValue() {
        final String str = (String) super.getCellEditorValue();
        if (str == null || str.length() == 0) {
            if (strict)
                return 1;
            else
                return 0;
        }
        try {
            int i = Integer.parseInt(str);
            if (i < 0) {
                if (strict)
                    i = 1;
                else
                    i = 0;
            }
            return i;
        } catch (Exception ex) {
            throw new IllegalStateException();
        }

    }
}
