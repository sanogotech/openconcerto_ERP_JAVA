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

public abstract class LightUIColumnCellRenderer {

    private void customizeLightUIElement(LightUIElement elt, int row, int column) {

    }

    public final LightUIElement getLightUIElement(Object value, int row, int column) {
        LightUIElement elt = createLightUIElement(value, row, column);
        customizeLightUIElement(elt, row, column);
        return elt;
    }

    protected abstract LightUIElement createLightUIElement(Object value, int row, int column);

}
