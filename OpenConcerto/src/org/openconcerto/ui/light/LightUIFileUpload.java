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

public class LightUIFileUpload extends LightUserControl {
    private static final String SEND_FILE_URL_JSON_KEY = "send-file-url";

    private String sendFileUrl;

    public LightUIFileUpload(final JSONObject json) {
        super(json);
    }

    public LightUIFileUpload(final LightUIFileUpload file) {
        super(file);
    }

    public LightUIFileUpload(final String id, final String sendFileUrl) {
        super(id);
        this.setType(LightUIElement.TYPE_FILE_UPLOAD);

        this.sendFileUrl = sendFileUrl;
    }

    @Override
    public JSONToLightUIConvertor getConvertor() {
        return new JSONToLightUIConvertor() {
            @Override
            public LightUIElement convert(final JSONObject json) {
                return new LightUIFileUpload(json);
            }
        };
    }

    @Override
    public LightUIElement clone() {
        return new LightUIFileUpload(this);
    }

    @Override
    protected void copy(LightUIElement element) {
        super.copy(element);
        if (!(element instanceof LightUIFileUpload)) {
            throw new InvalidClassException(this.getClassName(), element.getClassName(), element.getId());
        }

        final LightUIFileUpload files = (LightUIFileUpload) element;
        this.sendFileUrl = files.sendFileUrl;

    }

    @Override
    public void fromJSON(final JSONObject json) {
        super.fromJSON(json);

        this.sendFileUrl = JSONConverter.getParameterFromJSON(json, SEND_FILE_URL_JSON_KEY, String.class);
    }

    @Override
    public JSONObject toJSON() {
        final JSONObject json = super.toJSON();
        json.put(SEND_FILE_URL_JSON_KEY, this.sendFileUrl);
        return json;
    }

    @Override
    public void _setValueFromContext(final Object value) {
        this.setValue((String) value);
    }

    @Override
    public Object getValueForContext() {
        return this.getValue();
    }
}
