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

import java.util.ArrayList;
import java.util.List;

import net.minidev.json.JSONObject;

public class LightUIStrippedImageLine extends LightUILine implements HTMLable {

    private static final long serialVersionUID = 4132718509484530455L;
    private List<LightUIImage> images = new ArrayList<LightUIImage>(0);

    public LightUIStrippedImageLine(final List<LightUIImage> images) {
        super();
        this.images = images;
        for (LightUIImage image : images) {
            this.addChild(image);
        }
    }

    public LightUIStrippedImageLine(final JSONObject json) {
        super(json);
    }

    public LightUIStrippedImageLine(final LightUIStrippedImageLine line) {
        super(line);
    }

    @Override
    public LightUIElement clone() {
        return new LightUIStrippedImageLine(this);
    }

    @Override
    protected void copy(LightUIElement element) {
        super.copy(element);

        if (!(element instanceof LightUIStrippedImageLine)) {
            throw new InvalidClassException(this.getClassName(), element.getClassName(), element.getId());
        }

        final LightUIStrippedImageLine line = (LightUIStrippedImageLine) element;
        this.images = line.images;
    }

    @Override
    public JSONToLightUIConvertor getConvertor() {
        return new JSONToLightUIConvertor() {
            @Override
            public LightUIElement convert(final JSONObject json) {
                return new LightUIStrippedImageLine(json);
            }
        };
    }

    @Override
    public String getHTML() {
        StringBuilder b = new StringBuilder();
        for (LightUIImage image : this.images) {
            b.append("<img src=\"");
            b.append(image.getValue());
            b.append("\" ");
            if (image.getWidth() != null) {
                b.append("width=\"");
                b.append(image.getWidth());
                b.append("\" ");
            }
            if (image.getHeight() != null) {
                b.append("height=\"");
                b.append(image.getHeight());
                b.append("\" ");
            }
            if (this.getElementMargin() != null && this.getElementMargin() > 0) {
                b.append("style=\"margin:");
                b.append(this.getElementMargin());
                b.append("px \" ");
            }
            b.append("/> ");
        }
        return b.toString();
    }
}
