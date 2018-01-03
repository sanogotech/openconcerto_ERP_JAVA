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

import org.openconcerto.utils.io.HTMLable;
import org.openconcerto.utils.io.Transferable;

import net.minidev.json.JSONObject;

public class LightUIHTMLStrippedPanel extends LightUIPanel implements Transferable, HTMLable {

    public LightUIHTMLStrippedPanel(String id) {
        super(id);
        this.setType(LightUIElement.TYPE_RAW_HTML);
    }

    public LightUIHTMLStrippedPanel(final JSONObject json) {
        super(json);
    }

    public LightUIHTMLStrippedPanel(final LightUIHTMLStrippedPanel panel) {
        super(panel);
    }

    @Override
    public JSONToLightUIConvertor getConvertor() {
        return new JSONToLightUIConvertor() {
            @Override
            public LightUIElement convert(final JSONObject json) {
                return new LightUIHTMLStrippedPanel(json);
            }
        };
    }

    @Override
    public LightUIElement clone() {
        return new LightUIHTMLStrippedPanel(this);
    }

    @Override
    public String getHTML() {
        final StringBuilder b = new StringBuilder();
        for (int i = 0; i < this.getChildrenCount(); i++) {
            final LightUILine l = this.getChild(i, LightUILine.class);
            if (l instanceof HTMLable) {
                b.append(((HTMLable) l).getHTML());
            }
        }
        return b.toString();
    }
}
