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

public class LightUITab extends LightUIPanel {
    private boolean load = false;
    private boolean forceReload = false;

    public LightUITab(final String tabId, final String title) {
        super(tabId);
        this.setTitle(title);
        this.setType(TYPE_TAB_ELEMENT);
    }

    public LightUITab(final JSONObject json) {
        super(json);
    }

    public LightUITab(final LightUITab tab) {
        super(tab);
        this.load = tab.load;
        this.forceReload = tab.forceReload;
    }

    public boolean isForceReload() {
        return this.forceReload;
    }

    public void setForceReload(final boolean forceReload) {
        this.forceReload = forceReload;
    }

    public boolean isLoad() {
        return this.load;
    }

    public void setLoad(final boolean load) {
        this.load = load;
    }

    @Override
    public JSONToLightUIConvertor getConvertor() {
        return new JSONToLightUIConvertor() {

            @Override
            public LightUIElement convert(final JSONObject json) {
                return new LightUITab(json);
            }
        };
    }

    @Override
    public LightUIElement clone() {
        return new LightUITab(this);
    }

    @Override
    public void copy(final LightUIElement element) {
        if (!(element instanceof LightUITab)) {
            throw new InvalidClassException(LightUITab.class.getName(), element.getClassName(), element.getId());
        }
        super.copy(element);
        final LightUITab tab = (LightUITab) element;
        this.forceReload = tab.forceReload;
        this.load = tab.load;
    }

    @Override
    public JSONObject toJSON() {
        final JSONObject json = super.toJSON();
        if (this.load) {
            json.put("load", true);
        }
        if (this.forceReload) {
            json.put("force-reload", true);
        }
        return json;
    }

    @Override
    public void fromJSON(JSONObject json) {
        super.fromJSON(json);
        this.load = (Boolean) JSONConverter.getParameterFromJSON(json, "load", Boolean.class, false);
        this.forceReload = (Boolean) JSONConverter.getParameterFromJSON(json, "force-reload", Boolean.class, false);
    }
}
