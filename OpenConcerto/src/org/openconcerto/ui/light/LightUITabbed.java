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

public abstract class LightUITabbed extends LightUserControlContainer {
    // Init from json constructor
    public LightUITabbed(final JSONObject json) {
        super(json);
    }

    // Clone constructor
    public LightUITabbed(final LightUITabbed tabbedElement) {
        super(tabbedElement);
    }

    public LightUITabbed(final String id) {
        super(id);
        this.setType(TYPE_TABBED_UI);
        this.setWeightX(1);
        this.setFillWidth(true);
    }

    /**
     * This function allow dynamic load of tab. If the loading of one tab take a while, you can load
     * others after or in other thread
     * 
     * @param tabId: id of tab which you want to load. The tab id must match with a tab contained in
     *        childs
     */
    public abstract void loadTab(final String tabId);

    @Override
    public void setValue(final String value) {
        super.setValue(value);
    }

    @Override
    public void _setValueFromContext(final Object value) {
        final JSONObject jsonContext = (JSONObject) JSONConverter.getObjectFromJSON(value, JSONObject.class);
        if (jsonContext == null) {
            System.err.println("LightUITabbed.setValueFromContext() - json is null for this panel: " + this.getId());
        } else {
            final int childCount = this.getChildrenCount();
            for (int i = 0; i < childCount; i++) {
                final LightUITab child = this.getChild(i, LightUITab.class);
                if (jsonContext.containsKey(child.getUUID())) {
                    child._setValueFromContext(jsonContext.get(child.getUUID()));
                } else {
                    System.out.println("LightUITabbed.setValueFromContext() - Context doesn't contains value for UUID: " + child.getUUID() + " ID: " + child.getId());
                }
            }
        }
    }
}
