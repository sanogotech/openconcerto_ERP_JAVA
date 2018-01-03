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

import java.util.List;
import java.util.Map.Entry;

public abstract class StringValueConvertor extends ComboValueConvertor<String> {

    protected List<String> listId;

    public StringValueConvertor() {
        super();
    }

    public StringValueConvertor(final boolean hasNotSpecifed) {
        super(hasNotSpecifed);
    }

    /**
     * @param index: index of listId.
     * 
     * @return id store at index in listId or null if index is under 1.
     * 
     * @throws IllegalArgumentException when index is out out bounds or if list of ids is not initialized
     */
    public String getIdFromIndex(final Integer index) throws IllegalArgumentException {
        int realIndex = index;
        // values start at 1.
        realIndex = index - 1;
        if (this.listId == null) {
            throw new IllegalArgumentException("listId is not initialized");
        }
        if (realIndex >= this.listId.size()) {
            throw new IllegalArgumentException("index is out of bounds");
        }
        
        if(realIndex == -1){
            return null;
        } else {
            return this.listId.get(realIndex);
        }
    }

    /**
     * @param id: id store in listId.
     * 
     * @return index of id in listId or null if not found.
     */
    public Integer getIndexFromId(final String id) {
        if (this.listId == null) {
            throw new IllegalArgumentException("listId is not initialized");
        }

        final int listIdSize = this.listId.size();
        for (int i = 0; i < listIdSize; i++) {
            if (this.listId.get(i).equals(id)) {
                // values start at 1.
                return i + 1;
            }
        }
        return null;
    }

    @Override
    public void fillComboWithValue(final LightUIComboBox combo, final String selectedValue) {
        int i = 1;
        for (final Entry<String, String> entry : this.values.entrySet()) {
            final LightUIComboBoxElement comboElement = new LightUIComboBoxElement(i);
            comboElement.setValue1(entry.getValue());
            combo.addValue(comboElement);

            if (entry.getKey().equals(selectedValue)) {
                combo.setSelectedValue(comboElement);
            }
            i++;
        }
    }
}
