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

public class LightUIButtonUnmanaged extends LightUIElement {
    public LightUIButtonUnmanaged(final JSONObject json) {
        super(json);
    }

    public LightUIButtonUnmanaged(final String id, final String label) {
        super(id);
        setType(LightUIElement.TYPE_BUTTON_UNMANAGED);

        setGridWidth(1);
        setLabel(label);
    }

    public LightUIButtonUnmanaged(final String id) {
        super(id);
        setType(LightUIElement.TYPE_BUTTON_UNMANAGED);

        setGridWidth(1);
    }

    public LightUIButtonUnmanaged(final LightUIButtonUnmanaged button) {
        super(button);
    }

    @Override
    public JSONToLightUIConvertor getConvertor() {
        return new JSONToLightUIConvertor() {
            @Override
            public LightUIElement convert(final JSONObject json) {
                return new LightUIButtonUnmanaged(json);
            }
        };
    }

    @Override
    public LightUIElement clone() {
        return new LightUIButtonUnmanaged(this);
    }
}
