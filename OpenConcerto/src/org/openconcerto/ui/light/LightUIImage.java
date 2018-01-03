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

import net.minidev.json.JSONObject;

public class LightUIImage extends LightUIElement {
    public LightUIImage(final String id, final String path) {
        super(id);

        this.setType(LightUIElement.TYPE_IMAGE);
        this.setValue(path);
    }

    public LightUIImage(final JSONObject json) {
        super(json);
    }

    public LightUIImage(final LightUIImage image) {
        super(image);
    }

    @Override
    public JSONToLightUIConvertor getConvertor() {
        return new JSONToLightUIConvertor() {
            @Override
            public LightUIElement convert(final JSONObject json) {
                return new LightUIImage(json);
            }
        };
    }

    @Override
    public LightUIElement clone() {
        return new LightUIImage(this);
    }
}
