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
 
 package org.openconcerto.sql.ui.light;

import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.request.ComboSQLRequest;
import org.openconcerto.sql.sqlobject.IComboSelectionItem;
import org.openconcerto.ui.light.LightUIComboBox;
import org.openconcerto.ui.light.LightUIComboBoxElement;
import org.openconcerto.utils.io.JSONConverter;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import net.minidev.json.JSONObject;

public class LightAutoCompleteComboBox extends LightUIComboBox {
    private static final String FILTER = "filter";

    private final static Pattern QUERY_SPLIT_PATTERN = Pattern.compile("\\s+");

    private String filter;

    private ComboSQLRequest request;

    public LightAutoCompleteComboBox(final JSONObject json) {
        super(json);
    }

    public LightAutoCompleteComboBox(final String id) {
        super(id);
        this.setType(TYPE_AUTOCOMPLETE_COMBOBOX);
    }

    public void setComboRequest(final ComboSQLRequest request) {
        this.request = request;
        this.setFilter("");
        this.setAlreadyFilled(true);
    }

    public ComboSQLRequest getComboRequest() {
        return this.request;
    }

    public String getFilter() {
        return this.filter;
    }

    public void setFilter(final String filter) {
        this.filter = filter;
        this.applyFilter();
    }

    private void applyFilter() {
        if (this.request != null) {
            Integer selectedId = null;
            if (this.hasSelectedValue()) {
                selectedId = this.getSelectedValue().getId();
            }

            this.clearValues();
            if (this.hasNotSpecifedLine()) {
                this.addValue(LightUIComboBox.getDefaultValue());
            }

            final Where where = this.hasSelectedValue() ? new Where(this.request.getPrimaryTable().getKey(), "=", this.getSelectedValue().getId()) : null;
            final List<IComboSelectionItem> items = this.request.getComboItems(true, Arrays.asList(QUERY_SPLIT_PATTERN.split(this.filter)), Locale.getDefault(), where);

            System.err.println("LightAutoCompleteComboBox.applyFilter() - items count: " + items.size());
            for (final IComboSelectionItem item : items) {
                this.addValue(new LightUIComboBoxElement(item.getId(), item.getLabel()));
            }

            this.setSelectedId(selectedId);
        }
    }

    @Override
    public JSONObject toJSON() {
        final JSONObject json = super.toJSON();
        json.put(FILTER, this.filter);
        return json;
    }

    @Override
    public void fromJSON(final JSONObject json) {
        super.fromJSON(json);

        this.filter = JSONConverter.getParameterFromJSON(json, FILTER, String.class);
    }
}
