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

import java.awt.Color;

public class TitleLine extends LightUILine {

    final LightUILabel valueElement;

    public TitleLine(final String txt) {
        super();
        this.setGridAlignment(ALIGN_GRID);
        this.valueElement = new LightUILabel("label.value", txt, true);
        this.valueElement.setHorizontalAlignement(LightUIElement.HALIGN_LEFT);
        this.valueElement.setWeightX(1);
        this.valueElement.setGridWidth(2);
        this.addChild(this.valueElement);
        setElementPadding(5);
    }

    public void setItalicOnValue(boolean b) {
        this.valueElement.setFontItalic(b);
    }

    public void setValueolor(Color c) {
        this.valueElement.setForeColor(c);
    }
}
