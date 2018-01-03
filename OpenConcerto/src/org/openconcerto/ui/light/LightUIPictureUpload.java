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

public class LightUIPictureUpload extends LightUIFileUpload {

    private static final String FILE_PATH = "file-path";

    private String filePath = null;

    public LightUIPictureUpload(final String id, final String sendFileUrl) {
        super(id, sendFileUrl);
        this.setType(TYPE_PICTURE_UPLOAD);
    }

    public String getFilePath() {
        return this.filePath;
    }

    public void setFilePath(final String filePath) {
        this.filePath = filePath;
    }

    @Override
    public JSONObject toJSON() {
        final JSONObject json = super.toJSON();
        if (this.filePath != null) {
            json.put(FILE_PATH, this.filePath);
        }
        return json;
    }

    @Override
    public void fromJSON(JSONObject json) {
        super.fromJSON(json);
        this.filePath = JSONConverter.getParameterFromJSON(json, FILE_PATH, String.class);
    }
}
