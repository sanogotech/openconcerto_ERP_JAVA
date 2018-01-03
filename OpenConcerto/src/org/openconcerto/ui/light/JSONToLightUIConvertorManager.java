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

import java.util.HashMap;

import net.minidev.json.JSONObject;

public class JSONToLightUIConvertorManager {
    HashMap<String, JSONToLightUIConvertor> map = new HashMap<String, JSONToLightUIConvertor>();

    private static final JSONToLightUIConvertorManager instance = new JSONToLightUIConvertorManager();

    public static JSONToLightUIConvertorManager getInstance() {
        return instance;
    }

    public void put(final String className, final JSONToLightUIConvertor convertor) {
        this.map.put(className, convertor);
    }

    public LightUIElement createUIElementFromJSON(final JSONObject jsonElement) {
        final Integer elementType = (Integer) JSONConverter.getParameterFromJSON(jsonElement, "type", Integer.class, null);
        if (elementType == null) {
            throw new IllegalArgumentException("LightUIElement must contains attribute 'type'");
        }
        final String className = (String) JSONConverter.getParameterFromJSON(jsonElement, "class-name", String.class);
        if (className == null) {
            throw new IllegalArgumentException("class-name must be set");
        }
        final JSONToLightUIConvertor convertor = this.map.get(className);
        return convertor.convert(jsonElement);
    }
}
