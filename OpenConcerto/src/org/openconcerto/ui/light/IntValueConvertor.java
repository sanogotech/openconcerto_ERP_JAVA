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

import java.util.Map.Entry;

public abstract class IntValueConvertor extends ComboValueConvertor<Integer> {
    public IntValueConvertor() {
        super();
    }

    public IntValueConvertor(final boolean hasNotSpecified) {
        super(hasNotSpecified);
    }

    @Override
    public void fillComboWithValue(final LightUIComboBox combo, final Integer selectedValue) {
        for (final Entry<Integer, String> entry : this.values.entrySet()) {
            final LightUIComboBoxElement comboElement = new LightUIComboBoxElement(entry.getKey());
            comboElement.setValue1(entry.getValue());
            combo.addValue(comboElement);

            if (entry.getKey().equals(selectedValue)) {
                combo.setSelectedValue(comboElement);
            }
        }
    }
}
