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
 
 package org.openconcerto.erp.core.common.ui;

import org.openconcerto.sql.model.SQLField;
import org.openconcerto.utils.CompareUtils;

import java.awt.Color;
import java.math.BigDecimal;

import javax.swing.border.LineBorder;

public class DeviseNumericWithRangeCellEditor extends DeviseNumericCellEditor {

    private BigDecimal min, max;

    public DeviseNumericWithRangeCellEditor(SQLField field) {
        super(field);
        this.min = null;
        this.max = null;
    }

    public void setMin(BigDecimal min) {
        this.min = min;
    }

    public void setMax(BigDecimal max) {
        this.max = max;
    }

    @Override
    public boolean stopCellEditing() {
        if (min != null && CompareUtils.compare(min, getCellEditorValue()) > 0) {
            this.textField.setBorder(new LineBorder(Color.RED));
            return false;
        }
        if (max != null && CompareUtils.compare(max, getCellEditorValue()) < 0) {
            this.textField.setBorder(new LineBorder(Color.RED));
            return false;
        }
        return super.stopCellEditing();
    }

}
