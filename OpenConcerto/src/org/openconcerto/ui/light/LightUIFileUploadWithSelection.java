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

import org.openconcerto.utils.io.JSONConverter;

import net.minidev.json.JSONObject;

public class LightUIFileUploadWithSelection extends LightUIFileUpload {

    private static final String TABLE_ID_JSON_KEY = "table-id";
    private String tableId;

    public LightUIFileUploadWithSelection(final JSONObject json) {
        super(json);
    }

    public LightUIFileUploadWithSelection(final LightUIFileUploadWithSelection file) {
        super(file);
    }

    public LightUIFileUploadWithSelection(final String id, final String tableId, final String sendFileUrl) {
        super(id, sendFileUrl);
        this.setType(LightUIElement.TYPE_FILE_UPLOAD_WITH_SELECTION);
        this.setGridWidth(1);

        this.tableId = tableId;
    }

    @Override
    public JSONToLightUIConvertor getConvertor() {
        return new JSONToLightUIConvertor() {
            @Override
            public LightUIElement convert(final JSONObject json) {
                return new LightUIFileUploadWithSelection(json);
            }
        };
    }

    @Override
    public LightUIElement clone() {
        return new LightUIFileUploadWithSelection(this);
    }

    @Override
    protected void copy(LightUIElement element) {
        super.copy(element);
        if (!(element instanceof LightUIFileUploadWithSelection)) {
            throw new InvalidClassException(this.getClassName(), element.getClassName(), element.getId());
        }

        final LightUIFileUploadWithSelection files = (LightUIFileUploadWithSelection) element;
        this.tableId = files.tableId;
    }

    @Override
    public void fromJSON(final JSONObject json) {
        super.fromJSON(json);

        this.tableId = JSONConverter.getParameterFromJSON(json, TABLE_ID_JSON_KEY, String.class);
    }

    @Override
    public JSONObject toJSON() {
        final JSONObject json = super.toJSON();
        json.put(TABLE_ID_JSON_KEY, this.tableId);
        return json;
    }
}
