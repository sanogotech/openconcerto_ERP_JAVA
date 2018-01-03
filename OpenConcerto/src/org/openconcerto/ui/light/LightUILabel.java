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

public class LightUILabel extends LightUIElement {
    // Init from json constructor
    public LightUILabel(final JSONObject json) {
        super(json);
    }

    // Clone constructor
    public LightUILabel(final LightUILabel labelElement) {
        super(labelElement);
    }

    public LightUILabel(final String id) {
        this(id, "", false);
    }

    public LightUILabel(final String id, final boolean isBold) {
        this(id, "", isBold);
    }

    public LightUILabel(final String id, final String label) {
        this(id, label, false);
    }

    public LightUILabel(final String id, final String label, final boolean isBold) {
        super(id);
        this.setType(TYPE_LABEL);
        this.setLabel(label);
        this.setFontBold(isBold);
    }

    @Override
    public JSONToLightUIConvertor getConvertor() {
        return new JSONToLightUIConvertor() {
            @Override
            public LightUIElement convert(final JSONObject json) {
                return new LightUILabel(json);
            }
        };
    }

    @Override
    public LightUIElement clone() {
        return new LightUILabel(this);
    }
}
