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

import org.openconcerto.utils.convertor.ValueConvertor;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

public abstract class ComboValueConvertor<K> implements ValueConvertor<K, String> {

    private boolean hasNotSpecifedLine = false;

    protected Map<K, String> values = new LinkedHashMap<K, String>();

    public ComboValueConvertor() {
        this.init();
    }

    public ComboValueConvertor(final boolean hasNotSpecifedLine) {
        this.hasNotSpecifedLine = hasNotSpecifedLine;
        this.init();
    }

    protected abstract void init();

    public final boolean hasNotSpecifiedLine() {
        return this.hasNotSpecifedLine;
    }

    public final void fillCombo(final LightUIComboBox combo, final K selectedValue) {
        if (this.hasNotSpecifedLine) {
            combo.addValue(LightUIComboBox.getDefaultValue());
        }
        this.fillComboWithValue(combo, selectedValue);
        if (selectedValue == null) {
            combo.setSelectedValue(null);
        }
        
        if(!combo.hasSelectedValue()){
            if(combo.hasNotSpecifedLine()){
                combo.setSelectedValue(LightUIComboBox.getDefaultValue());
            } else if(!combo.getValues().isEmpty()) {
                combo.setSelectedValue(combo.getValues().get(0));
            }
        }
        
        combo.setAlreadyFilled(true);
    }

    protected abstract void fillComboWithValue(final LightUIComboBox combo, final K selectedValue);

    @Override
    public String convert(final K key) {
        if (key == null) {
            return null;
        } else {
            return this.values.get(key);
        }
    }

    @Override
    public K unconvert(final String value) {
        if (value == null) {
            return null;
        } else {
            for (final Entry<K, String> entry : this.values.entrySet()) {
                if (entry.getValue().equals(value)) {
                    return entry.getKey();
                }
            }
            return null;
        }
    }
}
