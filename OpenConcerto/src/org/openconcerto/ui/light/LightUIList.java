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

import java.util.ArrayList;

import net.minidev.json.JSONObject;

public class LightUIList extends LightUIContainer {

    // Init from json constructor
    public LightUIList(final JSONObject json) {
        super(json);
    }

    // Clone constructor
    public LightUIList(final LightUIList listElement) {
        super(listElement);
    }

    public LightUIList(final String id) {
        super(id);
        this.setType(TYPE_LIST);
    }

    public LightUIList(final String id, final ArrayList<LightUIListRow> rows) {
        super(id);
        this.setType(TYPE_LIST);

        for (final LightUIListRow row : rows) {
            this.addChild(row);
        }
    }
}
