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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import net.minidev.json.JSONObject;

public class LightUIButton extends LightUIElement {
    private List<ActionListener> clickListeners = new ArrayList<ActionListener>();

    public LightUIButton(final JSONObject json) {
        super(json);
    }

    public LightUIButton(final String id) {
        super(id);
        this.init(TYPE_BUTTON);
    }

    protected LightUIButton(final String id, final int buttonType) {
        super(id);
        this.init(buttonType);
    }

    private void init(final int buttonType) {
        this.setType(buttonType);
    }

    public LightUIButton(final LightUIButton button) {
        super(button);
    }

    public void addClickListener(final ActionListener listener) {
        this.clickListeners.add(listener);
    }

    public void removeClickListeners() {
        this.clickListeners.clear();
    }

    public void fireClick() {
        for (final ActionListener listener : this.clickListeners) {
            listener.actionPerformed(new ActionEvent(this, 1, "click"));
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        this.clickListeners.clear();
    }
}
