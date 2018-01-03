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

import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.io.JSONConverter;

import java.util.HashMap;
import java.util.Map;

import net.minidev.json.JSONObject;

public class LightUIButtonLink extends LightUIElement {
    private final static String OPEN_NEW_FRAME_KEY = "open-new-frame";
    private final static String URL_PARAMETER_KEY = "url-parameter";

    private Boolean openNewFrame = false;
    private Map<String, String> urlParameters = new HashMap<String, String>();

    public LightUIButtonLink(final String id, final String filePath, final boolean openNewFrame) {
        super(id);
        this.setType(LightUIElement.TYPE_BUTTON_LINK);
        this.setValue(filePath);
        this.setOpenNewFrame(openNewFrame);
    }

    public LightUIButtonLink(final JSONObject json) {
        super(json);
    }

    public LightUIButtonLink(final LightUIButtonLink button) {
        super(button);
    }

    public Boolean isOpenNewFrame() {
        return this.openNewFrame;
    }

    public void setOpenNewFrame(final Boolean openNewFrame) {
        this.openNewFrame = openNewFrame;
    }

    public void addUrlParameter(final String key, final String value) {
        this.urlParameters.put(key, value);
    }

    public void removeUrlParameter(final String key) {
        this.urlParameters.remove(key);
    }

    public void clearUrlParameter() {
        this.urlParameters.clear();
    }

    @Override
    public LightUIElement clone() {
        return new LightUIButtonLink(this);
    }

    @Override
    protected void copy(LightUIElement element) {
        super.copy(element);

        if (!(element instanceof LightUIButtonLink)) {
            throw new InvalidClassException(LightUIButtonLink.class.getName(), element.getClassName(), element.getId());
        }
        final LightUIButtonLink button = (LightUIButtonLink) element;
        this.openNewFrame = button.openNewFrame;
        this.urlParameters = button.urlParameters;
    }

    @Override
    public JSONToLightUIConvertor getConvertor() {
        return new JSONToLightUIConvertor() {
            @Override
            public LightUIElement convert(final JSONObject json) {
                return new LightUIButtonLink(json);
            }
        };
    }

    @Override
    public JSONObject toJSON() {
        final JSONObject json = super.toJSON();

        if (this.openNewFrame) {
            json.put(OPEN_NEW_FRAME_KEY, true);
        }
        if (!this.urlParameters.isEmpty()) {
            json.put(URL_PARAMETER_KEY, this.urlParameters);
        }

        return json;
    }

    @Override
    public void fromJSON(JSONObject json) {
        super.fromJSON(json);
        this.openNewFrame = JSONConverter.getParameterFromJSON(json, OPEN_NEW_FRAME_KEY, Boolean.class, false);
        this.urlParameters = CollectionUtils.castMap(JSONConverter.getParameterFromJSON(json, URL_PARAMETER_KEY, Map.class), String.class, String.class);
    }
}
